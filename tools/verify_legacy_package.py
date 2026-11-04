#!/usr/bin/env python3
"""Check release metadata; this does not replace runtime compatibility acceptance."""
import argparse
import json
import hashlib
from pathlib import Path
from zipfile import ZipFile
p = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--require-verified-hash', action='store_true',
                    help='Require the exact jar accepted on official Cleanroom 0.4.0')
parser.add_argument('--tag', help='Require this release tag to match the packaged version')
args = parser.parse_args()
identity = json.loads((p/'legacy-identity.json').read_text())
jars = [f for f in (p/'build/libs').glob(identity['modid']+'-*.jar') if not f.stem.endswith(('-sources','-api','-mcp','-javadoc'))]
if len(jars) != 1:
    raise SystemExit('Expected exactly one production jar: '+repr(jars))
with ZipFile(jars[0]) as archive:
    metadata = json.loads(archive.read('mcmod.info'))[0]
    assert metadata['modid'] == identity['modid'], metadata
    assert metadata['name'] == identity['name'], metadata
    assert metadata['authorList'] == ['yudream'], metadata
    logo = metadata['logoFile']
    assert logo in archive.namelist(), 'Missing project icon: ' + logo
    assert archive.read(logo) == (p/'branding/icon.png').read_bytes(), 'Packaged icon differs from project artwork'
    assert '${' not in archive.read('mcmod.info').decode(), 'Unexpanded metadata'
    assert not any('/dev/' in name or 'Smoke.class' in name for name in archive.namelist() if name.endswith('.class')), 'Development harness in release'
    assert not any(name.startswith(('net/createmod/', 'crafttweaker/', 'mezz/jei/'))
                   for name in archive.namelist() if name.endswith('.class')), 'Optional dependency bundled in release'
    expected_tag = 'v' + metadata['version']
    assert identity['release_tag'] == expected_tag, 'Release tag differs from packaged version'
    if args.tag and args.tag != expected_tag:
        raise SystemExit('Tag must match packaged version: ' + expected_tag)
sha256 = hashlib.sha256(jars[0].read_bytes()).hexdigest()
if args.require_verified_hash:
    if sha256 != identity['verified_jar_sha256']:
        raise SystemExit('Runtime acceptance required for jar SHA-256: ' + sha256
                         + '\nAccepted: ' + identity['verified_jar_sha256'])
    assert identity['cleanroom_baseline'] == '0.4.0', 'Expected the official 0.4.0 release baseline'
    assert identity['status'] == 'verified-cleanroom-0.4.0', 'Runtime acceptance is incomplete'
    for variant in ('client-without-ponder', 'client-with-ponder-jei',
                    'server-without-ponder', 'server-with-ponder'):
        evidence = p/'docs/verification/legacy-identity/cleanroom-0.4.0-final'/variant
        report = json.loads((evidence/'report.json').read_text())
        runtime = json.loads((evidence/'runtime.json').read_text())
        assert report['result'] == 'PASS', 'Failed runtime acceptance: ' + variant
        assert runtime['cleanroom'] == '0.4.0', 'Wrong runtime baseline: ' + variant
        assert runtime['jars'].get(jars[0].name) == sha256, 'Runtime package mismatch: ' + variant
print('PASS:', jars[0].name, metadata['modid'], 'SHA-256:', sha256)
