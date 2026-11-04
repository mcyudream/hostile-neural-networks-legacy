#!/usr/bin/env python3
"""Verify production jars on official Cleanroom 0.4.0, without a development resource pack (Linux x64)."""
import argparse
import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import urllib.request
from zipfile import ZipFile

PROJECT = Path(__file__).resolve().parent.parent
WORKSPACE = PROJECT.parent.parent
VERSION = "0.4.0"
CACHE = WORKSPACE / ".dev" / f"cleanroom-{VERSION}-runtime"
GRADLE = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
PACK = WORKSPACE / ".dev" / f"cleanroom-{VERSION}-installer.jar"
PACK_SHA256 = "882435d851a51e14b802e0a9384e4a05cfbb3890fca435c40c494af75e5a166f"


def digest(path, algorithm="sha1"):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, algorithm).hexdigest()


def download(url, destination, expected, algorithm="sha1", candidates=()):
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.is_file() and digest(destination, algorithm) == expected:
        return destination
    for candidate in candidates:
        if candidate.is_file() and digest(candidate, algorithm) == expected:
            shutil.copyfile(candidate, destination)
            return destination
    urls = [url]
    if url.startswith("https://repo.cleanroommc.com/releases/"):
        urls.insert(0, url.replace("https://repo.cleanroommc.com/releases/", "https://maven.cleanroommc.com/"))
    last_error = None
    for attempt in range(2):
        for source in urls:
            try:
                request = urllib.request.Request(source, headers={"User-Agent": "HNN-Cleanroom-verification"})
                partial = destination.with_name(destination.name + ".part")
                with urllib.request.urlopen(request, timeout=30) as response, partial.open("wb") as output:
                    shutil.copyfileobj(response, output, 128 * 1024)
                if digest(partial, algorithm) != expected:
                    raise ValueError(f"Hash mismatch: {destination.name}")
                partial.replace(destination)
                print("Downloaded", destination.name, flush=True)
                return destination
            except (OSError, ValueError) as error:
                last_error = error
    raise RuntimeError(f"Could not download {url}: {last_error}")


def allowed(library):
    rules = library.get("rules", [])
    result = not rules
    for rule in rules:
        os_rule = rule.get("os", {})
        if os_rule.get("name", "linux") == "linux" and os_rule.get("arch", "x86_64") in ("x86_64", "amd64"):
            result = rule["action"] == "allow"
    # The historical MMC recipe also lists ARM natives with a plain Linux rule.
    classifier = library["name"].split(":")[3:]
    return result and (not classifier or not classifier[0].startswith("natives-") or classifier == ["natives-linux"])


def artifact(library):
    entry = library["downloads"]["artifact"]
    parts = library["name"].split(":")
    group, module, version = parts[:3]
    path = entry.get("path") or f"{group.replace('.', '/')}/{module}/{version}/{entry['url'].rsplit('/', 1)[1]}"
    candidates = (GRADLE / "caches/modules-2/files-2.1" / group / module / version).glob(f"*/{Path(path).name}")
    candidates = list(candidates)
    if module == "cleanroom":
        candidates.append(WORKSPACE / ".dev" / f"cleanroom-{VERSION}-universal.jar")
    # Reuse byte-identical libraries from the old verification cache only after SHA-1 validation.
    candidates += list((WORKSPACE / ".dev/cleanroom-0.4.0-alpha-runtime/libraries").glob("**/" + Path(path).name))
    if module == "cleanroom":
        entry = dict(entry, url=f"https://maven.cleanroommc.com/com/cleanroommc/cleanroom/{VERSION}/cleanroom-{VERSION}-universal.jar")
    return download(entry["url"], CACHE / "libraries" / path, entry["sha1"], candidates=candidates)


def prepare(side):
    download(f"https://maven.cleanroommc.com/com/cleanroommc/cleanroom/{VERSION}/{PACK.name}", PACK, PACK_SHA256, "sha256")
    with ZipFile(PACK) as archive:
        recipe = json.loads(archive.read("version.json"))
    if recipe["id"] != f"cleanroom-{VERSION}":
        raise ValueError("Unexpected Cleanroom version in the official installer")
    metadata = json.loads((GRADLE / "caches/cleanroom_gradle/versions/1.12.2/meta.json").read_text())
    # Cleanroom replaces vanilla's obsolete LWJGL, Netty, ICU, JNA, logging and utility libraries.
    # Retain only the vanilla audio, authentication and platform services used by the official recipe.
    retained = {"com.paulscode", "com.mojang:authlib", "com.mojang:realms", "com.mojang:text2speech", "ca.weblite:java-objc-bridge"}
    vanilla_libraries = [library for library in metadata["libraries"]
                        if library["name"].split(":")[0] in retained or ":".join(library["name"].split(":")[:2]) in retained]
    libraries = {}
    for group in (recipe["libraries"], vanilla_libraries):
        for library in group:
            if allowed(library):
                libraries.setdefault(library["name"], library)
                native = library.get("natives", {}).get("linux")
                if native:
                    libraries.setdefault(library["name"] + ":" + native, {
                        "name": library["name"] + ":" + native,
                        "downloads": {"artifact": library["downloads"]["classifiers"][native]},
                    })
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        classpath = list(pool.map(artifact, libraries.values()))
    main = metadata["downloads"][side]
    vanilla = GRADLE / f"caches/cleanroom_gradle/versions/1.12.2/{side}.jar"
    classpath.append(download(main["url"], CACHE / f"minecraft-{side}-1.12.2.jar", main["sha1"], candidates=[vanilla]))
    # Loaded ELF relocation pages must never be truncated by another project's
    # verifier extracting its natives into the shared download cache.
    natives = PROJECT / "build/verification-natives" / str(os.getpid())
    natives.mkdir(parents=True, exist_ok=True)
    for jar in classpath:
        if "natives-linux" in jar.name:
            with ZipFile(jar) as archive:
                for name in archive.namelist():
                    if name.endswith(".so"):
                        target = natives / Path(name).name
                        data = archive.read(name)
                        if not target.is_file() or target.read_bytes() != data:
                            partial = target.with_name(target.name + f".{os.getpid()}.part")
                            partial.write_bytes(data)
                            partial.replace(target)
    return classpath, natives


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('side', choices=['client', 'server'])
    parser.add_argument('--with-ponder', action='store_true')
    parser.add_argument('--with-jei', action='store_true')
    parser.add_argument('--with-integrations', action='store_true', help='Install the pinned Draconic/Thermal/Tinkers/Blood Magic test set')
    parser.add_argument('--prepare-only', action='store_true')
    args = parser.parse_args()
    if platform.system() != 'Linux' or platform.machine() not in ('x86_64', 'amd64'):
        parser.error('This verifier supports Linux x64 only')
    classpath, natives = prepare(args.side)
    if args.prepare_only:
        return
    identity = json.loads((PROJECT / 'legacy-identity.json').read_text())
    release_version = identity['release_tag'].removeprefix('v')
    variant = args.side + ('-with-ponder' if args.with_ponder else '-without-ponder') + ('-jei' if args.with_jei else '') + ('-integrations' if args.with_integrations else '')
    run = PROJECT / 'run' / release_version / f'cleanroom-{VERSION}' / variant
    mods = run / 'mods'
    mods.mkdir(parents=True, exist_ok=True)
    jars = [PROJECT / 'build/libs' / f'{identity["modid"]}-{release_version}.jar',
            PROJECT / 'build/dev/hnn-verification-srg.jar']
    if args.with_integrations:
        lock = json.loads((PROJECT / 'integration-runtime-lock.json').read_text())
        for entry in lock['jars']:
            destination = WORKSPACE / '.dev/hnn-compat/jars' / entry['file']
            jars.append(download(entry['url'], destination, entry['sha256'], 'sha256'))
    if args.with_ponder:
        jars.append(Path(os.environ.get('LEGACY_PONDER_JAR', PROJECT.parent / 'ponder/build/libs/ponder-legacy-1.4.0-cleanroom-alpha.1.jar')))
        candidates = list((GRADLE / 'caches/modules-2/files-2.1/maven.modrinth/crafttweaker/4.1.20.698').glob('*/*.jar'))
        if len(candidates) != 1:
            raise RuntimeError('Resolve CraftTweaker with the withPonder development configuration first')
        jars += candidates
    if args.with_jei:
        candidates = list((GRADLE / 'caches/modules-2/files-2.1/mezz.jei/jei_1.12.2/4.16.1.302').glob('*/jei_1.12.2-4.16.1.302.jar'))
        if len(candidates) != 1:
            raise RuntimeError('Resolve JEI with the withJei development configuration first')
        jars += candidates
    wanted = {p.name for p in jars}
    for old in mods.glob('*.jar'):
        if old.name not in wanted:
            raise RuntimeError(f'Unexpected mod in isolated verification instance: {old}')
    for jar in jars:
        if not jar.is_file():
            raise FileNotFoundError(f'Build production jars and reobfDevHarnessJar first: {jar}')
        shutil.copyfile(jar, mods / jar.name)
    config = run / 'config'
    config.mkdir(exist_ok=True)
    (config / 'splash.properties').write_text('enabled=false\n')
    jdk = Path(os.environ.get('HNN_JAVA_HOME', WORKSPACE / '.dev/jdk-25.0.5+8'))
    command = [str(jdk / 'bin/java'), '-Xmx3G', '-Dfile.encoding=UTF-8', f'-Djava.library.path={natives}',
               '-Dhttp.proxyHost=127.0.0.1', '-Dhttp.proxyPort=9', '-Dhttps.proxyHost=127.0.0.1', '-Dhttps.proxyPort=9',
               f'-Dhnn.dev.cleanroomTarget={VERSION}', '-cp', os.pathsep.join(map(str, classpath))]
    report = run / 'hnn-smoke/report.json'
    if args.side == 'client':
        (run / 'options.txt').write_text('resourcePacks:[]\npauseOnLostFocus:false\n')
        command += ['-Dhnn.dev.smoke=true', 'top.outlands.foundation.boot.Foundation', '--tweakClass', 'net.minecraftforge.fml.common.launcher.FMLTweaker',
                    '--username', 'HNNDeveloper', '--uuid', '9dd8bda6-1594-3dc2-8f90-7121f2c01b74', '--accessToken', '0',
                    '--version', f'Cleanroom-{VERSION}', '--gameDir', str(run), '--assetsDir', str(GRADLE / 'caches/cleanroom_gradle/assets'),
                    '--assetIndex', '1.12', '--width', '1280', '--height', '800']
    else:
        # The user accepted the Minecraft EULA for this local verification instance.
        (run / 'eula.txt').write_text('eula=true\n')
        (run / 'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25579\nonline-mode=false\nlevel-name=hnn-server-smoke\nlevel-type=FLAT\nview-distance=3\ngenerate-structures=false\n')
        command += ['-Dhnn.dev.serverSmoke=true', 'net.minecraftforge.fml.relauncher.ServerLaunchWrapper', 'nogui']
    report.unlink(missing_ok=True)
    runtime = {'cleanroom': VERSION, 'side': args.side, 'ponder': args.with_ponder, 'jei': args.with_jei,
               'integrations': args.with_integrations,
               'audio_backend': os.environ.get('ALSOFT_DRIVERS'),
               'isolated_natives': {p.name: digest(p, 'sha256') for p in natives.glob('*.so')},
               'installerSha256': digest(PACK, 'sha256'), 'jars': {p.name: digest(p, 'sha256') for p in jars},
               'libraries': {str(p): digest(p) for p in classpath}}
    (run / 'runtime.json').write_text(json.dumps(runtime, indent=2) + '\n')
    print(f'Starting official Cleanroom {VERSION}: {variant}', flush=True)
    with (run / 'startup.log').open('w') as log:
        result = subprocess.run(command, cwd=run, stdout=log, stderr=subprocess.STDOUT, timeout=600)
    if result.returncode or not report.exists() or json.loads(report.read_text()).get('result') != 'PASS':
        raise SystemExit(f'Verification failed (exit={result.returncode}); inspect {run}')
    archive = PROJECT / identity['verification_root'] / variant
    archive.mkdir(parents=True, exist_ok=True)
    for file in ('runtime.json', 'startup.log'):
        shutil.copyfile(run / file, archive / file)
    shutil.copyfile(report, archive / 'report.json')
    screenshots = run / 'hnn-smoke/screenshots'
    if screenshots.exists():
        shutil.copytree(screenshots, archive / 'screenshots', dirs_exist_ok=True)
    print('PASS:', report, flush=True)


if __name__ == '__main__':
    main()
