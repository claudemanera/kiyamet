"""Generates GeckoLib animation files (idle / walk / attack / drive) from each .geo.json's bone names."""
import json, os, glob, re
ROOT = 'src/main/resources/assets/kiyamet'
GEO = ROOT + '/geckolib/models/entity'
OUT = ROOT + '/geckolib/animations/entity'
os.makedirs(OUT, exist_ok=True)

def kf(pairs): return {f'{t:.2f}': v for t, v in pairs}
def swing(a, length=1.0, phase=0):
    s = 1 if phase == 0 else -1
    return kf([(0, [s * a, 0, 0]), (length / 2, [-s * a, 0, 0]), (length, [s * a, 0, 0])])

for path in glob.glob(GEO + '/*.geo.json'):
    name = os.path.basename(path)[:-9]
    geo = json.load(open(path))['minecraft:geometry'][0]
    bones = [b['name'] for b in geo['bones']]
    has = lambda b: b in bones
    zombie = 'zombie' in name or name in ('patient_zero', 'the_butcher', 'experiment_47')
    vehicle = name.startswith('car_') or name in ('atv', 'motorcycle', 'armored_truck')
    anims = {}
    idle = {}
    if has('body'): idle['body'] = {'rotation': kf([(0, [0, 0, 0]), (1.5, [2, 0, 0]), (3.0, [0, 0, 0])])}
    if has('head'): idle['head'] = {'rotation': kf([(0, [0, -6, 0]), (1.5, [3, 6, 0]), (3.0, [0, -6, 0])])}
    for side, ph in (('arm_r', 0), ('arm_l', 1)):
        if has(side): idle[side] = {'rotation': kf([(0, [-75 if zombie else 0, 0, 0]), (1.5, [-70 if zombie else 4 * (1 if ph else -1), 0, 0]), (3.0, [-75 if zombie else 0, 0, 0])])}
    anims['misc.idle'] = {'loop': True, 'animation_length': 3.0, 'bones': idle}
    walk = {}
    for side, ph in (('leg_r', 0), ('leg_l', 1)):
        if has(side): walk[side] = {'rotation': swing(32, 1.0, ph)}
    for side, ph in (('arm_r', 1), ('arm_l', 0)):
        if has(side):
            if zombie: walk[side] = {'rotation': kf([(0, [-80, 0, 0]), (0.5, [-70, 0, 0]), (1.0, [-80, 0, 0])])}
            else: walk[side] = {'rotation': swing(28, 1.0, ph)}
    if has('body'): walk['body'] = {'position': kf([(0, [0, 0, 0]), (0.25, [0, 0.4, 0]), (0.5, [0, 0, 0]), (0.75, [0, 0.4, 0]), (1.0, [0, 0, 0])])}
    for b in bones:
        if re.match(r'^(leg|l\d|r\d|fl|fr|bl|br)', b) and b not in walk: walk[b] = {'rotation': swing(22, 1.0, len(walk) % 2)}
    anims['move.walk'] = {'loop': True, 'animation_length': 1.0, 'bones': walk}
    atk = {}
    for side in ('arm_r', 'arm_l'):
        if has(side): atk[side] = {'rotation': kf([(0, [-80, 0, 0]), (0.15, [-150, 0, 0]), (0.35, [-20, 0, 0]), (0.5, [-80, 0, 0])])}
    if not atk and has('body'): atk['body'] = {'rotation': kf([(0, [0, 0, 0]), (0.2, [-15, 0, 0]), (0.5, [0, 0, 0])])}
    anims['attack.swing'] = {'loop': False, 'animation_length': 0.5, 'bones': atk}
    if vehicle:
        drive = {b: {'rotation': kf([(0, [0, 0, 0]), (0.5, [180, 0, 0]), (1.0, [360, 0, 0])])} for b in bones if re.match(r'^w', b)}
        anims['move.drive'] = {'loop': True, 'animation_length': 1.0, 'bones': drive}
    json.dump({'format_version': '1.8.0', 'animations': anims}, open(f'{OUT}/{name}.animation.json', 'w'), indent=1)
print('animations:', len(glob.glob(OUT + '/*.json')))
