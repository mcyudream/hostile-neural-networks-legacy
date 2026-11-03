#!/usr/bin/env python3
"""Import assets and model data from the pinned upstream checkout, reproducibly."""
import copy
import json
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UPSTREAM = ROOT.parents[1] / 'upstream/Hostile-Neural-Networks'
LOCK = json.loads((ROOT / 'upstream-lock.json').read_text())
if subprocess.check_output(['git', '-C', str(UPSTREAM), 'rev-parse', 'HEAD'], text=True).strip() != LOCK['commit']:
    raise SystemExit('Upstream checkout does not match upstream-lock.json')
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/hostilenetworks'
SOURCE = UPSTREAM / 'src/main/resources'
ASSETS.mkdir(parents=True, exist_ok=True)
for folder in ['textures', 'models', 'blockstates']:
    shutil.copytree(SOURCE / 'assets/hostilenetworks' / folder, ASSETS / folder, dirs_exist_ok=True)
# 1.12 blockstate model locations are relative to models/block.
for path in (ASSETS / 'blockstates').glob('*.json'):
    path.write_text(path.read_text().replace('hostilenetworks:block/', 'hostilenetworks:'))

ENTITY_IDS = {'minecraft:zombified_piglin': 'minecraft:zombie_pigman',
              'minecraft:evoker': 'minecraft:evocation_illager',
              'minecraft:vindicator': 'minecraft:vindication_illager',
              'minecraft:iron_golem': 'minecraft:villager_golem',
              'minecraft:snow_golem': 'minecraft:snowman'}
STACK_IDS = {
    'minecraft:short_grass': ('minecraft:tallgrass', 1),
    'minecraft:smooth_stone': ('minecraft:stone', 0),
    'minecraft:black_stained_glass': ('minecraft:stained_glass', 15),
    'minecraft:cobweb': ('minecraft:web', 0),
    'minecraft:slime_block': ('minecraft:slime', 0),
    'minecraft:skeleton_skull': ('minecraft:skull', 0),
    'minecraft:wither_skeleton_skull': ('minecraft:skull', 1),
    'minecraft:zombie_head': ('minecraft:skull', 2),
    'minecraft:creeper_head': ('minecraft:skull', 4),
    'minecraft:dragon_head': ('minecraft:skull', 5),
    'minecraft:bone_meal': ('minecraft:dye', 15),
    'minecraft:ink_sac': ('minecraft:dye', 0),
    'minecraft:lapis_lazuli': ('minecraft:dye', 4),
    'minecraft:cod': ('minecraft:fish', 0),
    'minecraft:salmon': ('minecraft:fish', 1),
    'minecraft:tropical_fish': ('minecraft:fish', 2),
    'minecraft:pufferfish': ('minecraft:fish', 3),
    'minecraft:wet_sponge': ('minecraft:sponge', 1),
    'minecraft:white_wool': ('minecraft:wool', 0),
    'minecraft:red_wool': ('minecraft:wool', 14),
    'minecraft:poppy': ('minecraft:red_flower', 0),
    'minecraft:snowball': ('minecraft:snowball', 0),
    'minecraft:nether_quartz_ore': ('minecraft:quartz_ore', 0),
}
for wool_meta, wool_color in enumerate(['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']):
    STACK_IDS['minecraft:' + wool_color + '_wool'] = ('minecraft:wool', wool_meta)
# These vanilla entities/blocks do not exist in 1.12.2. They are not substituted.
MISSING = {'minecraft:cod', 'minecraft:salmon', 'minecraft:pufferfish', 'minecraft:tropical_fish',
           'minecraft:drowned', 'minecraft:phantom', 'minecraft:breeze', 'minecraft:warden',
           'minecraft:hoglin', 'minecraft:zoglin', 'minecraft:piglin', 'minecraft:copper_ore', 'minecraft:glow_squid'}
# Ore blocks themselves are the 1.12 raw materials; preserving counts requires smelting.
ORE_IDS = {'minecraft:raw_iron': 'minecraft:iron_ore', 'minecraft:raw_gold': 'minecraft:gold_ore'}

report = {'source_commit': LOCK['commit'], 'models': [], 'excluded': [],
          'entity_renames': ENTITY_IDS, 'stack_mappings': STACK_IDS,
          'ore_adaptations': ORE_IDS,
          'recipe_adaptations': {'loot_fabricator': 'diamond replaces unavailable netherite ingot',
                                 'data_center': 'dragon breath replaces unavailable echo shards',
                                 'framework': 'stone replaces smooth stone item'}}

def adapt_stack(stack):
    if not isinstance(stack, dict):
        return stack
    out = copy.deepcopy(stack)
    key = out.get('id', out.get('item'))
    if key in STACK_IDS:
        out['id'], out['data'] = STACK_IDS[key]
        out.pop('item', None)
    elif key in ORE_IDS:
        out['id'] = ORE_IDS[key]
    return out

data_dir = ASSETS / 'data_models'
if data_dir.exists():
    shutil.rmtree(data_dir)
data_dir.mkdir()
for path in sorted((SOURCE / 'data/hostilenetworks/data_models').rglob('*.json')):
    relative = path.relative_to(SOURCE / 'data/hostilenetworks/data_models')
    model = json.loads(path.read_text())
    target = model.get('entity', model.get('block'))
    if target in MISSING:
        report['excluded'].append({'id': str(relative), 'reason': f'{target} absent in 1.12.2'})
        continue
    # Modern third-party registries differ. Keep vanilla models and let pack authors add verified compat.
    if target and not target.startswith('minecraft:'):
        report['excluded'].append({'id': str(relative), 'reason': 'modern third-party model requires separate 1.12 registry verification'})
        continue
    if 'entity' in model:
        model['entity'] = ENTITY_IDS.get(model['entity'], model['entity'])
    if 'block' in model:
        model['block'] = STACK_IDS.get(model['block'], (model['block'], 0))[0]
    model['variants'] = [ENTITY_IDS.get(v, v) for v in model.get('variants', [])
                         if v not in MISSING and not v.startswith('minecraft:deepslate_') and v != 'minecraft:nether_gold_ore']
    if model.get('block') == 'minecraft:redstone_ore':
        model['variants'].append('minecraft:lit_redstone_ore')
    model['base_drop'] = adapt_stack(model['base_drop'])
    model['fabricator_drops'] = [adapt_stack(s) for s in model['fabricator_drops']]
    dest = data_dir / relative
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(model, ensure_ascii=False, indent=2) + '\n')
    report['models'].append(str(relative))

shutil.copytree(SOURCE / 'data/hostilenetworks/model_tiers', ASSETS / 'model_tiers', dirs_exist_ok=True)
index = {'data_models': report['models'],
         'model_tiers': [p.name for p in sorted((ASSETS / 'model_tiers').glob('*.json'))]}
(ASSETS / 'data-index.json').write_text(json.dumps(index, indent=2) + '\n')
(ROOT / 'docs').mkdir(exist_ok=True)
(ROOT / 'docs/import-report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
for path in (SOURCE / 'assets/hostilenetworks/lang').glob('*.json'):
    dest = ASSETS / 'lang' / (path.stem + '.lang')
    dest.parent.mkdir(exist_ok=True)
    try:
        lang = json.loads(path.read_text())
    except ValueError as exc:
        print(f'Skipped invalid upstream translation {path.name}: {exc}')
        continue
    extra = ROOT / 'localization' / (path.stem + '.json')
    if extra.exists():
        lang.update(json.loads(extra.read_text()))
    for key, value in list(lang.items()):
        if (key.startswith('item.hostilenetworks.') or key.startswith('block.hostilenetworks.')) and not key.endswith('.name'):
            lang[key + '.name'] = value
        if key.startswith('block.hostilenetworks.') and not key.endswith('.name'):
            lang[key.replace('block.', 'tile.', 1) + '.name'] = value
    # Vanilla Item#getTranslationKeyInefficiently translates the bare key before
    # appending .name. Keeping modern bare item keys would produce "Deep Learner.name".
    lang = {k: v for k, v in lang.items() if not (k.startswith('item.hostilenetworks.') and not k.endswith('.name') and '.' not in k[len('item.hostilenetworks.'):])}
    dest.write_text('\n'.join(k + '=' + v.replace('\n', '\\n') for k, v in lang.items()) + '\n')

TAG_ORES = {'c:obsidians': 'obsidian', 'c:glass_panes': 'paneGlass', 'c:dusts/redstone': 'dustRedstone',
            'c:ingots/iron': 'ingotIron', 'c:ingots/gold': 'ingotGold', 'c:gems/lapis': 'gemLapis',
            'c:gems/diamond': 'gemDiamond', 'c:ingots/netherite': 'gemDiamond', 'minecraft:leaves': 'treeLeaves'}
def ingredient(value):
    if 'tag' in value:
        if value['tag'] == 'hostilenetworks:generalized_predictions':
            return [{'item': 'hostilenetworks:' + name + '_prediction'} for name in ['overworld', 'nether', 'end', 'twilight']]
        return {'type': 'forge:ore_dict', 'ore': TAG_ORES[value['tag']]}
    if value.get('item') == 'minecraft:echo_shard': value = {'item': 'minecraft:dragon_breath'}
    result = adapt_stack(value)
    if 'id' in result: result['item'] = result.pop('id')
    # Forge 1.12 requires explicit metadata for subtype items in JSON ingredients.
    if 'item' in result: result.setdefault('data', 0)
    return result
for path in sorted((UPSTREAM / 'src/generated/resources/data/hostilenetworks/recipe').rglob('*.json')):
    recipe = json.loads(path.read_text())
    recipe.pop('category', None)
    recipe['result'] = ingredient(recipe['result'])
    if 'key' in recipe: recipe['key'] = {k: ingredient(v) for k, v in recipe['key'].items()}
    if 'ingredients' in recipe: recipe['ingredients'] = [ingredient(v) for v in recipe['ingredients']]
    relative = path.relative_to(UPSTREAM / 'src/generated/resources/data/hostilenetworks/recipe')
    dest = ASSETS / 'recipes' / relative
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(recipe, indent=2) + '\n')
print(f"Imported {len(report['models'])} model definitions, {len(index['model_tiers'])} tiers; {len(report['excluded'])} exclusions recorded.")
