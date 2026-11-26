"""Bakes zombie spawn markers into the structure templates as entities (placed with the structure)."""
import json, glob, os, random, sys
sys.path.insert(0, os.path.dirname(__file__))
import nbtlib as N
D = 'src/main/resources/data/kiyamet'
THEMES = {
    'medical': (['abandoned_hospital', 'quarantine_zone', 'outbreak_lab', 'morgue', 'field_hospital', 'asylum', 'pharmacy'], ['doctor_zombie'] * 4 + ['nurse_zombie'] * 3 + ['toxic_zombie', 'runner_zombie', 'screamer_zombie']),
    'military': (['military_checkpoint', 'army_base', 'bunker', 'armory_depot', 'crashed_helicopter', 'tank_graveyard', 'radar_station', 'missile_silo', 'barracks', 'border_outpost', 'police_station'],
                 ['armored_zombie'] * 3 + ['police_zombie', 'runner_zombie', 'tank_zombie']),
    'farm': (['abandoned_farm', 'safe_town', 'treehouse_village', 'fishing_village', 'mushroom_camp'], ['farmer_zombie'] * 3 + ['woman_zombie'] * 3 + ['runner_zombie']),
    'dark': (['ancient_underground_city', 'abandoned_mine', 'sewer_network', 'underground_prison', 'crystal_cavern', 'hive_nest', 'lava_forge', 'lost_library', 'core_chamber', 'shadow_ruins'],
             ['listener_zombie'] * 2 + ['crawler_zombie', 'runner_zombie', 'toxic_zombie']),
}
DEFAULT = ['woman_zombie'] * 4 + ['runner_zombie'] * 2 + ['farmer_zombie', 'police_zombie', 'screamer_zombie', 'exploder_zombie', 'toxic_zombie', 'tank_zombie']

def pool_for(sid):
    for ids, pool in THEMES.values():
        if sid in ids: return pool
    return DEFAULT

SANCTUARY = {'small_mosque','grand_mosque','small_church','cathedral','cave_camp','treehouse_village','fishing_village','mushroom_camp'}
BOSS = {'outbreak_lab':'patient_zero','asylum':'the_butcher','missile_silo':'experiment_47','ancient_underground_city':'forgotten_king',
        'hive_nest':'hive_mother','lava_forge':'lava_golem','lost_library':'time_keeper','core_chamber':'core_warden',
        'weather_station':'storm_lord','swamp_witch_hut':'swamp_witch','shadow_ruins':'shadow_stalker'}
ROBOTS = ['robot_trooper'] * 3 + ['robot_heavy', 'scout_drone']
TWD = {'twd_prison': ('COMMUNITY', 'prison'), 'twd_farm': ('COMMUNITY', 'farm'), 'twd_walled_town': ('COMMUNITY', 'walled_town'),
       'twd_hilltop': ('COMMUNITY', 'hilltop'), 'twd_kingdom': ('COMMUNITY', 'kingdom'), 'twd_terminal': ('COMMUNITY', 'terminal'),
       'twd_raider_factory': ('RAIDER', 'sanctuary'), 'twd_masked_camp': ('WHISPERER', 'camp')}
LEVELS = {'member': (5, 120), 'guard': (40, 180), 'trader': (20, 80), 'leader': (150, 230), 'commander': (260, 300)}
def human_nbt(sid, role, r):
    faction, ctype = TWD.get(sid, ('SURVIVOR', ''))
    if faction == 'RAIDER' and role == 'leader': role = 'commander'
    if faction == 'RAIDER' and role == 'trader': role = 'member'
    lo, hi = LEVELS[role]
    if faction == 'WHISPERER' and role == 'leader': lo, hi = 180, 240
    return {'Faction': N.T(N.STR, faction), 'Role': N.T(N.STR, role.upper()), 'Level': N.T(N.INT, r.randint(lo, hi)),
            'CommunityType': N.T(N.STR, ctype), 'Ready': N.T(N.BYTE, 0)}
n_total = 0
for mf in sorted(glob.glob(D + '/structure_markers/*.json')):
    sid = os.path.basename(mf)[:-5]
    nbt = D + f'/structure/{sid}.nbt'
    if not os.path.exists(nbt): continue
    marks = json.load(open(mf))['markers']
    root = N.load(nbt)
    r = random.Random(sid)
    _pal = [q['Name'].v for q in root['palette'].v[1]]
    _at = {tuple(b['pos'].v[1]): _pal[b['state'].v] for b in root['blocks'].v[1]}
    def free(x, y, z):
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (0, 0)):
            p = (x + dx, y, z + dz)
            if _at.get(p, 'minecraft:air') in ('minecraft:air',) and _at.get((p[0], y + 1, p[2]), 'minecraft:air') == 'minecraft:air' and _at.get((p[0], y - 1, p[2]), 'minecraft:air') != 'minecraft:air':
                return p
        return None
    ents = []
    for m in marks:
        if m['type'] == 'zombie' and sid not in SANCTUARY:
            x, y, z = m['pos']; ents.append(N.T(N.COMP, N.entity(x, y, z, 'kiyamet:' + r.choice(pool_for(sid)))))
        elif m['type'] == 'boss' and sid in BOSS:
            x, y, z = m['pos']; ents.append(N.T(N.COMP, N.entity(x, y, z, 'kiyamet:' + BOSS[sid])))
        elif m['type'].startswith('human:'):
            x, y, z = m['pos']; ents.append(N.T(N.COMP, N.entity(x, y, z, 'kiyamet:human', human_nbt(sid, m['type'][6:], r))))
        elif m['type'] in ('survivor', 'guard'):          # 1.7: lone survivors and their guards actually show up (two per spot)
            x, y, z = m['pos']
            role = 'guard' if m['type'] == 'guard' else ('trader' if r.random() < 0.15 else 'member')
            ents.append(N.T(N.COMP, N.entity(x, y, z, 'kiyamet:human', human_nbt(sid, role, r))))
            q = free(x, y, z) if m['type'] == 'survivor' and r.random() < 0.7 else None
            if q and q != (x, y, z): ents.append(N.T(N.COMP, N.entity(*q, 'kiyamet:human', human_nbt(sid, 'member', r))))
        elif m['type'] == 'loot' and sid not in BOSS and sid not in TWD and r.random() < 0.35:   # someone else got here first
            q = free(*m['pos'])
            if q: ents.append(N.T(N.COMP, N.entity(*q, 'kiyamet:human', human_nbt(sid, 'member', r))))
        elif m['type'] == 'robot':
            x, y, z = m['pos']; ents.append(N.T(N.COMP, N.entity(x, y, z, 'kiyamet:' + r.choice(ROBOTS))))
    root['entities'] = N.T(N.LIST, (N.COMP, [e.v for e in ents]) if ents else (N.END, []))
    N.save(nbt, root); n_total += len(ents)
print('entities baked:', n_total)
