"""1.6: puts doors into doorways and furnishes the insides of every structure template (barrels with loot, benches,
shelves, lanterns, beds of rubble, cobwebs...). Only fills blocks that are explicit air in the template."""
import glob, os, random, sys
sys.path.insert(0, os.path.dirname(__file__))
import nbtlib as N
D = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/data/kiyamet')
LOOT = {os.path.basename(f)[:-5] for f in glob.glob(D + '/loot_table/chests/*.json')}
SOFT = ('air', 'structure_void', 'water', 'lava', 'torch', 'lantern', 'carpet', 'door', 'sign', 'banner', 'flower', 'grass', 'fern', 'vine', 'sapling', 'rail',
        'button', 'pressure_plate', 'cobweb', 'snow', 'pane', 'fence', 'bars', 'leaves', 'candle', 'chain', 'dead_bush', 'skull', 'head', 'pot', 'wire', 'trapdoor', 'ladder')
THEME = {'medical': ['abandoned_hospital', 'quarantine_zone', 'outbreak_lab', 'morgue', 'field_hospital', 'asylum', 'pharmacy'],
         'military': ['military_checkpoint', 'army_base', 'bunker', 'armory_depot', 'radar_station', 'missile_silo', 'barracks', 'border_outpost', 'police_station', 'tank_graveyard'],
         'tech': ['data_center', 'research_tower', 'robot_plant', 'teleport_lab', 'tech_outpost', 'drone_hangar', 'solar_farm', 'weather_station', 'cryo_vault', 'volcano_station'],
         'holy': ['small_mosque', 'grand_mosque', 'small_church', 'cathedral']}
DECOR = {'medical': ['barrel', 'cauldron', 'brewing_stand', 'white_wool', 'iron_trapdoor', 'smoker'],
         'military': ['barrel', 'barrel', 'smithing_table', 'anvil', 'blast_furnace', 'target', 'iron_block'],
         'tech': ['barrel', 'observer', 'redstone_lamp', 'lectern', 'smithing_table', 'iron_block'],
         'holy': ['bookshelf', 'lectern', 'candle', 'chiseled_bookshelf'],
         'home': ['barrel', 'crafting_table', 'furnace', 'bookshelf', 'composter', 'loom', 'barrel', 'cauldron', 'chest_barrel']}
DOOR = {'medical': 'birch_door', 'military': 'spruce_door', 'tech': 'mangrove_door', 'holy': 'dark_oak_door', 'home': 'oak_door'}

def theme(sid):
    for t, ids in THEME.items():
        if sid in ids: return t
    return 'home'

def soft(name): return any(s in name for s in SOFT)

total_d = total_f = 0
for f in sorted(glob.glob(D + '/structure/*.nbt')):
    sid = os.path.basename(f)[:-4]
    root = N.load(f)
    pal = root['palette'].v[1]
    blocks = root['blocks'].v[1]
    names = [p['Name'].v for p in pal]
    at = {}
    for i, b in enumerate(blocks):
        x, y, z = b['pos'].v[1]; at[(x, y, z)] = i
    def name(p):
        i = at.get(p)
        return names[blocks[i]['state'].v] if i is not None else None
    def is_air(p): return name(p) == 'minecraft:air'
    def solid(p):
        n = name(p)
        return n is not None and not soft(n)
    def state(n, props=None):
        key = (n, tuple(sorted((props or {}).items())))
        for j, p in enumerate(pal):
            pk = tuple(sorted((k, v.v) for k, v in p.get('Properties', N.T(N.COMP, {})).v.items())) if 'Properties' in p else ()
            if p['Name'].v == n and pk == key[1]: return j
        e = {'Name': N.T(N.STR, n)}
        if props: e['Properties'] = N.T(N.COMP, {k: N.T(N.STR, v) for k, v in props.items()})
        pal.append(e); names.append(n)
        return len(pal) - 1
    def put(p, n, props=None, nbt=None):
        i = at[p]
        blocks[i]['state'] = N.T(N.INT, state(n, props))
        if nbt: blocks[i]['nbt'] = N.T(N.COMP, nbt)
        elif 'nbt' in blocks[i]: del blocks[i]['nbt']
    def roofed(p):
        x, y, z = p
        return any(solid((x, y + k, z)) for k in range(2, 9))
    r = random.Random(sid)
    th = theme(sid)
    # ---------------------------------------------------------------- doors
    size = root['size'].v[1]
    def exits(p, d):
        x, y, z = p
        for k in range(1, 40):
            q = (x + d[0] * k, y, z + d[1] * k)
            if not (0 <= q[0] < size[0] and 0 <= q[2] < size[2]): return True
            if solid(q) or solid((q[0], y + 1, q[2])): return False
        return True
    def opening(p): return is_air(p) and is_air((p[0], p[1] + 1, p[2])) and solid((p[0], p[1] - 1, p[2])) and (solid((p[0], p[1] + 2, p[2])) or is_air((p[0], p[1] + 2, p[2])) and solid((p[0], p[1] + 3, p[2])))
    doors = []
    seen = set()
    for p0 in list(at):
        if p0 in seen or not opening(p0): continue
        x, y, z = p0
        for axis in ('z', 'x'):
            along = (1, 0) if axis == 'z' else (0, 1)        # direction the wall runs
            pas = (0, 1) if axis == 'z' else (1, 0)          # direction you walk through
            prev = (x - along[0], y, z - along[1])
            if not (solid(prev) and (solid((prev[0], y + 1, prev[2])) or 'pane' in (name((prev[0], y + 1, prev[2])) or ''))): continue
            cells = [p0]
            nxt = (x + along[0], y, z + along[1])
            if opening(nxt): cells.append(nxt); nxt = (nxt[0] + along[0], y, nxt[2] + along[1])
            if not (solid(nxt) and (solid((nxt[0], y + 1, nxt[2])) or 'pane' in (name((nxt[0], y + 1, nxt[2])) or ''))): continue
            ok = True
            for c in cells:
                fr, bk = (c[0] - pas[0], y, c[2] - pas[1]), (c[0] + pas[0], y, c[2] + pas[1])
                if solid(fr) or solid(bk): ok = False
            if not ok: continue
            fr, bk = (x - pas[0], y, z - pas[1]), (x + pas[0], y, z + pas[1])
            if not (roofed(fr) != roofed(bk) or exits(fr, (-pas[0], -pas[1])) or exits(bk, pas)): continue
            facing = 'south' if axis == 'z' else 'east'
            doors.append((cells, facing))
            seen.update(cells)
    doors.sort(key=lambda d: d[0][0][1])
    placed = []
    for (cells, facing) in doors:
        p = cells[0]
        if len(placed) >= 10 or any(abs(p[0] - q[0]) + abs(p[2] - q[2]) < 3 and abs(p[1] - q[1]) < 3 for q in placed): continue
        dn = 'minecraft:' + DOOR[th]
        for i, (x, y, z) in enumerate(cells):
            hinge = 'left' if i == 0 else 'right'
            put((x, y, z), dn, {'facing': facing, 'half': 'lower', 'hinge': hinge, 'open': 'false', 'powered': 'false'})
            put((x, y + 1, z), dn, {'facing': facing, 'half': 'upper', 'hinge': hinge, 'open': 'false', 'powered': 'false'})
            placed.append((x, y, z))
    # ---------------------------------------------------------------- furniture
    cands = []
    for (x, y, z) in list(at):
        p = (x, y, z)
        if not (is_air(p) and is_air((x, y + 1, z)) and solid((x, y - 1, z)) and roofed(p)): continue
        if any(abs(x - q[0]) + abs(z - q[2]) <= 2 and abs(y - q[1]) <= 1 for q in placed): continue
        wall = [d for d in ((1, 0), (-1, 0), (0, 1), (0, -1)) if solid((x + d[0], y, z + d[1]))]
        cands.append((p, wall))
    r.shuffle(cands)
    budget = min(60, max(4, len(cands) // 14))
    used = set(); nf = 0
    FACE = {(1, 0): 'west', (-1, 0): 'east', (0, 1): 'north', (0, -1): 'south'}
    for p, wall in cands:
        if nf >= budget: break
        x, y, z = p
        if any((x + a, y, z + b) in used for a in (-1, 0, 1) for b in (-1, 0, 1)): continue
        if wall:
            kind = r.choice(DECOR[th])
            face = FACE[wall[0]]
            if kind in ('barrel', 'chest_barrel'):
                nbt = {'LootTable': N.T(N.STR, f'kiyamet:chests/{sid}')} if sid in LOOT and r.random() < 0.5 else None
                put(p, 'minecraft:barrel', {'facing': 'up', 'open': 'false'}, nbt)
            elif kind in ('furnace', 'smoker', 'blast_furnace'): put(p, 'minecraft:' + kind, {'facing': face, 'lit': 'false'})
            elif kind == 'anvil': put(p, 'minecraft:chipped_anvil', {'facing': face})
            elif kind == 'lectern': put(p, 'minecraft:lectern', {'facing': face, 'has_book': 'false', 'powered': 'false'})
            elif kind == 'chiseled_bookshelf': put(p, 'minecraft:bookshelf')
            elif kind == 'candle': put(p, 'minecraft:candle', {'candles': str(r.randint(1, 4)), 'lit': 'false', 'waterlogged': 'false'})
            elif kind == 'iron_trapdoor': put(p, 'minecraft:iron_trapdoor', {'facing': face, 'half': 'top', 'open': 'false', 'powered': 'false', 'waterlogged': 'false'})
            elif kind == 'redstone_lamp': put(p, 'minecraft:redstone_lamp', {'lit': 'false'})
            elif kind == 'observer': put(p, 'minecraft:observer', {'facing': face, 'powered': 'false'})
            elif kind == 'composter': put(p, 'minecraft:composter', {'level': '0'})
            elif kind == 'loom': put(p, 'minecraft:loom', {'facing': face})
            elif kind == 'brewing_stand': put(p, 'minecraft:brewing_stand', {'has_bottle_0': 'false', 'has_bottle_1': 'false', 'has_bottle_2': 'false'})
            elif kind == 'cauldron': put(p, 'minecraft:cauldron')
            else: put(p, 'minecraft:' + kind)
            # sometimes a lantern or candle on top
            if r.random() < 0.3 and is_air((x, y + 1, z)) and kind not in ('candle',): put((x, y + 1, z), 'minecraft:lantern', {'hanging': 'false', 'waterlogged': 'false'})
        else:
            v = r.random()
            if v < 0.45: put(p, 'minecraft:' + r.choice(['gray_carpet', 'brown_carpet', 'red_carpet', 'light_gray_carpet']))
            elif v < 0.6: put(p, 'minecraft:skeleton_skull', {'rotation': str(r.randint(0, 15))})
            elif v < 0.8: put(p, 'minecraft:cobweb')
            else: put(p, 'minecraft:potted_dead_bush')
        used.add(p); nf += 1
    # cobwebs in ceiling corners
    webs = 0
    for (x, y, z) in list(at):
        if webs > 12: break
        if is_air((x, y, z)) and solid((x, y + 1, z)) and sum(solid((x + a, y, z + b)) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))) >= 2 and r.random() < 0.08:
            put((x, y, z), 'minecraft:cobweb'); webs += 1
    root['palette'] = N.T(N.LIST, (N.COMP, pal))
    N.save(f, root)
    total_d += len(placed); total_f += nf + webs
    print(f'{sid:28s} doors {len(placed):2d}  furniture {nf:3d}  webs {webs}')
print('TOTAL doors', total_d, 'furniture', total_f)
