#!/usr/bin/env python3
"""Collect only the installable jar, sources, release notes and checksums."""
import argparse
import hashlib
import json
import shutil
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--tag', help='Require this tag to match the packaged version')
    args = parser.parse_args()
    identity = json.loads((ROOT/'legacy-identity.json').read_text())
    tag = identity['release_tag']
    if args.tag and args.tag != tag:
        raise SystemExit('Tag must match packaged version: ' + tag)
    version = tag.removeprefix('v')
    names = [f"{identity['modid']}-{version}{suffix}.jar" for suffix in ('', '-sources')]
    with ZipFile(ROOT/'build/libs'/names[0]) as archive:
        metadata = json.loads(archive.read('mcmod.info'))[0]
        if metadata['version'] != version:
            raise SystemExit('Package version differs from release tag')
    notes = ROOT/'docs/releases'/f'{version}.md'
    if not notes.is_file():
        raise SystemExit('Release notes are missing: ' + str(notes))
    dist = ROOT/'build/release'
    if dist.exists():
        shutil.rmtree(dist)
    dist.mkdir(parents=True)
    for name in names:
        shutil.copyfile(ROOT/'build/libs'/name, dist/name)
    checksums = ''.join(hashlib.sha256((dist/name).read_bytes()).hexdigest()
                        + '  ' + name + '\n' for name in names)
    (dist/'SHA256SUMS').write_text(checksums)
    shutil.copyfile(notes, dist/'RELEASE-NOTES.md')
    print(checksums, end='')


if __name__ == '__main__':
    main()
