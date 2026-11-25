"""Bakes zombie spawn markers into the structure templates as entities (placed with the structure)."""
import json, glob, os, random, sys
sys.path.insert(0, os.path.dirname(__file__))
import nbtlib as N
D = 'src/main/resources/data/kiyamet'
THEMES = {
    'medical': (['abandoned_hospital', 'quarantine_zone', 'outbreak_lab', 'morgue', 'field_hospital', 'asylum', 'pharmacy'], ['doctor_zombie'] * 4 + ['toxic_zombie', 'runner_zombie', 'screamer_zombie']),
    'military': (['military_checkpoint', 'army_base', 'bunker', 'armory_depot', 'crashed_helicopter', 'tank_graveyard', 'radar_station', 'missile_silo', 'barracks', 'border_outpost', 'police_station'],
                 ['armored_zombie'] * 3 + ['police_zombie', 'runner_zombie', 'tank_zombie']),
    'farm': (['abandoned_farm', 'safe_town', 'treehouse_village', 'fishing_village', 'mushroom_camp'], ['farmer_zombie'] * 3 + ['runner_zombie']),
    'dark': (['ancient_underground_city', 'abandoned_mine', 'sewer_network', 'underground_prison', 'crystal_cavern', 'hive_nest', 'lava_forge', 'lost_library', 'core_chamber', 'shadow_ruins'],
             ['listener_zombie'] * 2 + ['crawler_zombie', 'runner_zombie', 'toxic_zombie']),
}
DEFAULT = ['runner_zombie'] * 4 + ['farmer_zombie', 'police_zombie', 'screamer_zombie', 'exploder_zombie', 'toxic_zombie', 'tank_zombie']

def pool_for(sid):
    for ids, pool in THEMES.values():
        if sid in ids: return pool
    return DEFAULT

SANCTUARY = {'small_mosque','grand_mosque','small_church','cathedral','cave_camp','treehouse_village','fishing_village','mushroom_camp'}
BOSS = {'outbreak_lab':'patient_zero','asylum':'the_butcher','missile_silo':'experiment_47','ancient_underground_city':'forgotten_king',
        'hive_nest':'hive_mother','lava_forge':'lava_golem','lost_library':'time_keeper','core_chamber':'core_warden',
        'weather_station':'storm_lord','swamp_witch_hut':'swamp_witch','shadow_ruins':'shadow_stalker'}
ROBOTS = ['robot_trooper'] * 3 + ['robot_heavy', 'scout_drone']
n_total = 0
for mf in sorted(glob.glob(D + '/structure_markers/*.json')):
    sid = os.path.basename(mf)[:-5]
    nbt = D + f'/structure/{sid}.nbt'
    if not os.path.exists(nbt): continue
    marks = json.load(open(mf))['markers']
    root = N.load(nbt)
    r = random.Random(sid)
    ents = []
    for m in marks:
        if m['type'] == 'zombie' and sid not in SANCTUARY:
            x, y, z = m['pos']; ents.append(N.T(N.COMP, N.entity(x, y, z, 'kiyamet:' + r.choice(pool_for(sid)))))
        elif m['type'] == 'boss' and sid in BOSS:
            x, y, z = m['pos']; ents.append(N.T(N.COMP, N.entity(x, y, z, 'kiyamet:' + BOSS[sid])))
        elif m['type'] == 'robot':
            x, y, z = m['pos']; ents.append(N.T(N.COMP, N.entity(x, y, z, 'kiyamet:' + r.choice(ROBOTS))))
    root['entities'] = N.T(N.LIST, (N.COMP, [e.v for e in ents]) if ents else (N.END, []))
    N.save(nbt, root); n_total += len(ents)
print('entities baked:', n_total)
