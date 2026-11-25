"""Bakes zombie markers of each giant city into its 64x64 tile templates."""
import json, glob, os, random, sys
sys.path.insert(0, os.path.dirname(__file__))
import nbtlib as N
D = 'src/main/resources/data/kiyamet'
POOLS = {
    'dead_metropolis': ['runner_zombie'] * 3 + ['police_zombie', 'screamer_zombie', 'tank_zombie', 'exploder_zombie', 'hunter_zombie', 'alpha_zombie'],
    'frozen_mountain': ['runner_zombie'] * 2 + ['armored_zombie', 'listener_zombie', 'tank_zombie'],
    'industrial': ['runner_zombie'] * 2 + ['toxic_zombie', 'toxic_zombie', 'exploder_zombie', 'tank_zombie', 'mutant_zombie'],
    'oasis': ['runner_zombie'] * 3 + ['farmer_zombie', 'screamer_zombie', 'crawler_zombie'],
    'port': ['runner_zombie'] * 3 + ['farmer_zombie', 'police_zombie', 'toxic_zombie', 'tank_zombie'],
    'quarantine': ['doctor_zombie'] * 3 + ['armored_zombie', 'toxic_zombie', 'screamer_zombie', 'mutant_zombie'],
    'suburbs': ['runner_zombie'] * 3 + ['farmer_zombie', 'police_zombie', 'screamer_zombie', 'exploder_zombie'],
}
BOSS = {'dead_metropolis':'the_general','port':'deep_horror','industrial':'assembly_mother','frozen_mountain':'frost_giant'}
total = 0
for lf in sorted(glob.glob(D + '/city_layouts/*.json')):
    cid = os.path.basename(lf)[:-5]
    lay = json.load(open(lf))
    r = random.Random(cid)
    per = {}
    for m in lay.get('markers', []):
        x, y, z = m['pos']
        if m['type'] == 'zombie': eid = r.choice(POOLS[cid])
        elif m['type'] == 'boss' and cid in BOSS: eid = BOSS[cid]
        else: continue
        per.setdefault((x // 64, z // 64), []).append((x % 64, y, z % 64, eid))
    for t in glob.glob(D + f'/structure/cities/{cid}/t_*.nbt'):
        i, j = map(int, os.path.basename(t)[2:-4].split('_'))
        root = N.load(t)
        ents = [N.entity(x, y, z, 'kiyamet:' + e) for x, y, z, e in per.get((i, j), [])]
        root['entities'] = N.T(N.LIST, (N.COMP, ents) if ents else (N.END, []))
        N.save(t, root); total += len(ents)
print('city zombies baked:', total)
