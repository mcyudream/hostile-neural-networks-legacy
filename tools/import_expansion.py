#!/usr/bin/env python3
"""Import the pinned MIT Extra HNN assets and adapt its recipes for 1.12.2."""
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UPSTREAM = ROOT.parents[1] / 'upstream/Extra-Hostile-Neural-Networks'
SOURCE = UPSTREAM / 'src/main/resources'
ASSETS = ROOT / 'src/main/resources/assets'
REPLACEMENTS = {
    'minecraft:netherite_scrap': 'minecraft:quartz',
    'minecraft:netherite_ingot': 'minecraft:nether_star',
    'minecraft:netherite_block': 'minecraft:diamond_block',
    'minecraft:netherite_sword': 'minecraft:diamond_sword',
    'minecraft:copper_ingot': 'minecraft:iron_ingot',
}
def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n')

def main():
    src = SOURCE / 'assets/extrahnn'
    for folder in ('textures', 'models'):
        shutil.copytree(src / folder, ASSETS / 'extrahnn' / folder, dirs_exist_ok=True)
    for file in (ASSETS / 'extrahnn/models').rglob('*.json'):
        data = json.loads(file.read_text())
        for key, value in data.get('textures', {}).items():
            if value.startswith('minecraft:block/') or value.startswith('block/'):
                data['textures'][key] = value.replace('block/', 'blocks/', 1)
        write(file, data)
    # Registry IDs belong to HNN; asset references remain in Extra HNN's namespace.
    for folder in ('models/item', 'blockstates'):
        for file in (src / folder).glob('*.json'):
            data = json.loads(file.read_text())
            if folder == 'blockstates':
                for variant in data['variants'].values():
                    variant['model'] = variant['model'].replace(':block/', ':')
            write(ASSETS / 'hostilenetworks' / folder / file.name, data)
    for file in (SOURCE / 'data/extrahnn/recipe').glob('*.json'):
        data = json.loads(file.read_text())
        for value in data['key'].values():
            item = value['item'].replace('extrahnn:', 'hostilenetworks:')
            value['item'] = REPLACEMENTS.get(item, item)
        data['result']['item'] = data['result'].pop('id').replace('extrahnn:', 'hostilenetworks:')
        write(ASSETS / 'hostilenetworks/recipes' / ('extra_' + file.name), data)
    (ROOT / 'licenses').mkdir(exist_ok=True)
    shutil.copyfile(UPSTREAM / 'LICENSE', ROOT / 'licenses/Extra-HNN-MIT.txt')
    write(ROOT / 'expansion-upstream-lock.json', {
        'repository': 'https://github.com/lentel27/Extra-Hostile-Neural-Networks',
        'commit': '950bc0327ed3ace3911eebf2eab5d0aca3b44949', 'version': '2.2.5',
        'license': 'MIT', 'resource_namespace': 'extrahnn', 'registry_namespace': 'hostilenetworks',
        'recipe_replacements': REPLACEMENTS,
        'digital_agonizer': {'implementation': 'independent', 'reference': 'https://github.com/xt9/DeepMobLearning-BloodMagic',
                             'copied_code_or_assets': False}
    })

if __name__ == '__main__':
    main()
