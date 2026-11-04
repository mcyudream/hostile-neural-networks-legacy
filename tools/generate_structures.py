#!/usr/bin/env python3
"""Write vanilla 1.12 structure NBT for the two Ponder demonstrations."""
import gzip
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/hostilenetworks/ponder'
ROOT.mkdir(parents=True, exist_ok=True)
def string(s):
    b = s.encode(); return struct.pack('>H', len(b)) + b
def payload(t, v):
    if t == 3: return struct.pack('>i', v)
    if t == 8: return string(v)
    if t == 9:
        child, values = v
        return bytes([child]) + struct.pack('>i', len(values)) + b''.join(payload(child, x) for x in values)
    if t == 10: return b''.join(bytes([k]) + string(name) + payload(k, val) for name, (k, val) in v.items()) + b'\0'
    raise ValueError(t)
def write(name, size, blocks):
    palette = []; entries = []
    for pos, (block, props) in blocks.items():
        definition = {'Name': (8, block)}
        if props: definition['Properties'] = (10, {k: (8, v) for k, v in props.items()})
        if definition not in palette: palette.append(definition)
        entries.append({'pos': (9, (3, list(pos))), 'state': (3, palette.index(definition))})
    data = {'DataVersion': (3, 1343), 'size': (9, (3, size)), 'palette': (9, (10, palette)),
            'blocks': (9, (10, entries)), 'entities': (9, (10, [])), 'author': (8, 'HNN Cleanroom')}
    (ROOT / (name + '.nbt')).write_bytes(gzip.compress(b'\x0a\0\0' + payload(10, data), mtime=0))
workflow = {(x, 0, z): ('minecraft:stone', {}) for x in range(5) for z in range(5)}
workflow[(1, 1, 2)] = ('hostilenetworks:sim_chamber', {'facing': 'north'})
workflow[(3, 1, 2)] = ('hostilenetworks:loot_fabricator', {'facing': 'north'})
workflow[(4, 1, 2)] = ('minecraft:hopper', {'facing': 'down', 'enabled': 'true'})
workflow[(4, 1, 3)] = ('minecraft:chest', {'facing': 'north'})
write('workflow', [5, 3, 5], workflow)
center = {(x, 0, z): ('minecraft:stone', {}) for x in range(9) for z in range(9)}
for x in range(1, 8):
    for y in range(1, 8):
        for z in range(1, 8):
            if y == 1: center[(x, y, z)] = ('minecraft:obsidian', {})
            elif x in (1, 7) or y == 7 or z in (1, 7): center[(x, y, z)] = ('minecraft:stained_glass', {'color': 'black'})
center[(4, 2, 1)] = ('hostilenetworks:data_center', {'facing': 'north'})
center[(6, 2, 1)] = ('hostilenetworks:data_center_io_port', {'mode': 'energy'})
write('data_center', [9, 9, 9], center)
expansion = {(x, 0, z): ('minecraft:stone', {}) for x in range(7) for z in range(7)}
for x, block in ((1, 'simulator_modeling'), (2, 'merger_camera'), (4, 'ultimate_sim_chamber_v1'), (5, 'ultimate_loot_fabricator_v1')):
    expansion[(x, 1, 3)] = ('hostilenetworks:' + block, {'facing': 'north'})
expansion[(5, 1, 4)] = ('minecraft:chest', {'facing': 'north'})
write('expansion', [7, 3, 7], expansion)
agonizer = {(x, 0, z): ('minecraft:stone', {}) for x in range(5) for z in range(5)}
agonizer[(1, 1, 2)] = ('bloodmagic:altar', {})
agonizer[(3, 1, 2)] = ('hostilenetworks:digital_agonizer', {'facing': 'north'})
agonizer[(3, 2, 2)] = ('minecraft:hopper', {'facing': 'down', 'enabled': 'true'})
write('agonizer', [5, 4, 5], agonizer)
