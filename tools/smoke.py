"""Boots a real Fabric dedicated server with the mod, drives it through console commands and reports problems.
Run in CI from a folder containing fabric-server-launch.jar and mods/."""
import subprocess, threading, time, sys, re, os, queue

ENTS = open(sys.argv[1]).read().split()
STRUCTS = open(sys.argv[2]).read().split()
log = open('smoke.log', 'w')
lines = queue.Queue()
problems = []

heap = {}
phase = ['boot']

def monitor(p):
    while p.poll() is None:
        try:
            out = subprocess.run(['jcmd', str(p.pid), 'GC.heap_info'], capture_output=True, text=True, timeout=20).stdout
            m = re.search(r'used (\d+)K', out)
            if m:
                mb = int(m.group(1)) // 1024
                heap[phase[0]] = max(heap.get(phase[0], 0), mb)
                log.write(f'### HEAP {phase[0]} {mb} MB\n')
        except Exception:
            pass
        time.sleep(15)

def start():
    p = subprocess.Popen(['java', '-Xmx6G', '-jar', 'fabric-server-launch.jar', 'nogui'], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
    def pump():
        for l in p.stdout:
            log.write(l); log.flush()
            lines.put(l)
    threading.Thread(target=pump, daemon=True).start()
    threading.Thread(target=monitor, args=(p,), daemon=True).start()
    return p

def wait_for(pat, timeout):
    end = time.time() + timeout
    while time.time() < end:
        try: l = lines.get(timeout=1)
        except queue.Empty: continue
        if re.search(pat, l): return l
    return None

def drain(sec):
    end = time.time() + sec
    while time.time() < end:
        try: lines.get(timeout=0.5)
        except queue.Empty: pass

def cmd(p, c, sec=1.0):
    p.stdin.write(c + '\n'); p.stdin.flush()
    drain(sec)

def boot(tag):
    p = start()
    if not wait_for(r'Done \(', 900):
        problems.append(f'[{tag}] server did not finish starting'); p.kill(); return None
    return p

# ---------------------------------------------------------------- run 1: world gen, every structure, every entity, time passes
p = boot('run1')
if p:
    cmd(p, 'gamerule spawnRadius 0'); cmd(p, 'forceload add -48 -48 48 48', 5)
    phase[0] = 'structures'
    for i, s in enumerate(STRUCTS):
        x, z = 600 + (i % 10) * 450, 600 + (i // 10) * 450
        if s.startswith('city_'):
            cmd(p, f'locate structure kiyamet:{s}', 12)
            continue
        cmd(p, f'forceload add {x-48} {z-48} {x+48} {z+48}', 6)
        cmd(p, f'place structure kiyamet:{s} {x} 0 {z}', 6)
        cmd(p, f'forceload remove {x-48} {z-48} {x+48} {z+48}', 0.5)
        log.write(f'### placed {s}\n')
    phase[0] = 'structures-save'
    cmd(p, 'save-all flush', 20)
    cmd(p, 'stop', 1)
    try: p.wait(300)
    except subprocess.TimeoutExpired: problems.append('[structures] server did not stop'); p.kill()

# ---------------------------------------------------------------- run 1b: every entity, enchantments, time passing
p = boot('run1b')
if p:
    phase[0] = 'entities'
    cmd(p, 'forceload add -48 -48 48 48', 5)
    cmd(p, 'fill -24 100 -24 24 100 24 minecraft:stone', 1)
    cmd(p, 'fill -24 101 -24 24 110 24 minecraft:air', 1)
    for i, e in enumerate(ENTS):
        cmd(p, f'summon kiyamet:{e} {(i % 8) * 5 - 18} 101 {(i // 8) * 5 - 18}', 0.3)
    # 1.4: our enchantments must parse and apply
    cmd(p, 'item replace entity @e[type=kiyamet:farmer_zombie,limit=1] weapon.mainhand with kiyamet:arrow_pistol[enchantments={"kiyamet:rapid_fire":5,"kiyamet:explosive_tips":2}]', 1)
    cmd(p, 'item replace entity @e[type=kiyamet:human,limit=1] weapon.mainhand with minecraft:diamond_sword[enchantments={"minecraft:sharpness":10,"minecraft:unbreaking":10}]', 1)
    cmd(p, 'summon item 0 101 0 {Item:{id:"kiyamet:gun_tome",count:1}}', 1)
    # 1.5: gunsmith bench and a fully kitted gun
    cmd(p, 'setblock 2 101 2 kiyamet:gun_bench', 1)
    cmd(p, 'summon item 1 101 1 {Item:{id:"kiyamet:arrow_rifle",count:1,components:{"minecraft:custom_model_data":{strings:["acog","long","laser","drum"],colors:[9282650]},"minecraft:custom_data":{ModSCOPE:"acog",Paint:"olive"}}}}', 1)
    # 1.6 farming: fruit trees, crops, fur-lined armor
    cmd(p, 'setblock 6 100 6 minecraft:grass_block', 0.3); cmd(p, 'setblock 6 100 10 minecraft:grass_block', 0.3)
    cmd(p, 'place feature kiyamet:orange_tree 6 101 6', 1); cmd(p, 'place feature kiyamet:peach_tree 6 101 10', 1)
    cmd(p, 'setblock 9 100 6 minecraft:farmland', 0.3); cmd(p, 'setblock 9 101 6 kiyamet:tomato_crop[age=7]', 0.3)
    cmd(p, 'setblock 9 100 8 minecraft:farmland', 0.3); cmd(p, 'setblock 9 101 8 kiyamet:corn_crop[age=3]', 0.3)
    cmd(p, 'setblock 11 101 6 kiyamet:orange_leaves[fruit=2]', 0.3)
    for a in ('orange', 'peach', 'strawberry', 'cabbage_seeds', 'wolf_pelt', 'polar_pelt', 'orange_sapling'):
        cmd(p, 'summon item 4 101 4 {Item:{id:"kiyamet:%s",count:1}}' % a, 0.3)
    cmd(p, 'summon item 4 101 4 {Item:{id:"minecraft:iron_chestplate",count:1,components:{"minecraft:equippable":{slot:"chest",asset_id:"kiyamet:fur_wolf_iron"},"minecraft:custom_data":{Fur:"WOLF",FurBase:"minecraft:iron"}}}}', 1)
    # 1.7: furniture, bags, fridge, new cars, meals
    fx = -12
    for f in ('double_bed', 'royal_bed', 'wardrobe', 'fridge', 'kitchen_cabinet', 'kitchen_sink', 'stove', 'dining_table', 'wooden_chair', 'armchair', 'sofa', 'coffee_table',
              'bookcase', 'office_desk', 'floor_lamp', 'television', 'bathtub', 'toilet', 'gun_rack', 'nightstand', 'furniture_table'):
        cmd(p, 'setblock %d 101 -12 kiyamet:%s' % (fx, f), 0.2); fx += 2
    cmd(p, 'setblock 14 101 -12 kiyamet:placed_bag[lower=3,upper=11]', 0.3)
    cmd(p, 'item replace block -6 101 -12 container.0 with kiyamet:tomato 4', 0.3)
    for v in ('sports_gt', 'sports_rs', 'sports_turbo', 'taxi', 'muscle_car', 'jeep', 'van', 'school_bus', 'fire_truck', 'superbike_vortex', 'superbike_blaze'):
        cmd(p, 'summon kiyamet:%s %d 101 18' % (v, -30 + 6 * ('sports_gt', 'sports_rs', 'sports_turbo', 'taxi', 'muscle_car', 'jeep', 'van', 'school_bus', 'fire_truck', 'superbike_vortex', 'superbike_blaze').index(v)), 0.4)
    for a in ('ratatouille', 'survivor_ration', 'gold_money_bag', 'spoiled_food'):
        cmd(p, 'summon item 3 101 -3 {Item:{id:"kiyamet:%s",count:1}}' % a, 0.2)
    cmd(p, 'twd date', 0.3)
    # 1.9: stations, power, settlement, meals, guns, family
    for i, b in enumerate(('main_table', 'cooking_table', 'settlement_bell', 'solar_panel', 'wind_turbine', 'chest')):
        cmd(p, 'setblock %d 101 -16 %s' % (-6 + 2 * i, b if b == 'chest' else 'kiyamet:' + b), 0.3)
    cmd(p, 'item replace block 4 101 -16 container.0 with minecraft:beef 8', 0.3)
    cmd(p, 'item replace block 4 101 -16 container.1 with minecraft:iron_ingot 4', 0.3)
    for a in ('venison', 'cooked_bear_meat', 'jerky', 'moose_roast', 'electric_drill', 'chainsaw', 'mission_board', 'lion_pelt', 'ice_bear_pelt', 'waterskin', 'hydration_pack', 'pitbull_spawn_egg'):
        cmd(p, 'summon item 3 101 -3 {Item:{id:"kiyamet:%s",count:1}}' % a, 0.2)
    cmd(p, 'summon kiyamet:human 0 101 -14 {Faction:"COMPANION",Work:"FARMER",SettleAt:%dL}' % (lambda v: v - (1 << 64) if v >= 1 << 63 else v)(((-2 & 0x3FFFFFF) << 38) | ((-16 & 0x3FFFFFF) << 12) | (101 & 0xFFF)), 0.5)
    cmd(p, 'summon kiyamet:human 1 101 -14 {Faction:"COMPANION",Partner:1b,PregnantDay:-400L}', 0.5)
    cmd(p, 'summon kiyamet:human 2 101 -14 {Faction:"COMPANION",BornDay:0L}', 0.5)
    # 2.0: element weapons, cores, a forged weapon, sage + guardian + lord, and one temple built for real
    for a in ('flame_sword', 'ice_nunchaku', 'metal_claws', 'water_shuriken', 'earth_core', 'ancient_rod', 'sage_map'):
        cmd(p, 'summon item 3 101 -3 {Item:{id:"kiyamet:%s",count:1}}' % a, 0.2)
    cmd(p, 'summon kiyamet:flame_sage 6 101 -6', 0.4)
    cmd(p, 'summon kiyamet:ice_guardian 8 101 -8', 0.4)
    cmd(p, 'summon kiyamet:shadow_lord 10 101 -10', 0.4)
    cmd(p, 'forceload add 160 160 260 260', 6)
    cmd(p, 'execute positioned 210 100 210 run twd buildtemple flame', 8)
    cmd(p, 'execute if entity @e[type=kiyamet:flame_lord] run say TEMPLE_LORD_OK', 1)
    cmd(p, 'execute if entity @e[type=kiyamet:flame_guardian] run say TEMPLE_GUARDS_OK', 1)
    # 2.2: creation items, the new stones, the zombie enderman, the five End bosses, the five End structures and the Temple of Wisdom
    for a in ('creation_axe', 'creation_rod', 'creation_core', 'star_stone', 'dragon_stone', 'creation_altar', 'wisdom_stone'):
        cmd(p, 'summon item 3 101 -3 {Item:{id:"kiyamet:%s",count:1}}' % a, 0.2)
    cmd(p, 'setblock 5 101 5 kiyamet:creation_table', 0.3)
    cmd(p, 'setblock 7 101 5 kiyamet:creation_altar', 0.3)
    cmd(p, 'setblock 9 101 5 kiyamet:plate', 0.3)
    cmd(p, 'summon kiyamet:human 3 101 -14 {Faction:"COMPANION",BornDay:-1L}', 0.5)
    cmd(p, 'summon kiyamet:zombie_enderman 6 101 8', 0.4)
    for i, bs in enumerate(('void_tyrant', 'sky_herald', 'dead_captain', 'hive_matriarch', 'star_warden')):
        cmd(p, 'summon kiyamet:%s %d 101 12' % (bs, -8 + 4 * i), 0.4)
    for i, st in enumerate(('void_citadel', 'sky_spire', 'fallen_skyship', 'ender_hive', 'starfall_observatory')):
        x = -700 + 350 * i
        cmd(p, 'forceload add %d -460 %d -340' % (x - 60, x + 60), 4)
        cmd(p, 'execute positioned %d 120 -400 run twd endbuild %s' % (x, st), 10)
    cmd(p, 'forceload add -40 360 40 520', 4)
    cmd(p, 'execute positioned 0 140 400 run twd buildwisdom', 10)
    for bs in ('void_tyrant', 'sky_herald', 'dead_captain', 'hive_matriarch', 'star_warden'):
        cmd(p, 'execute if entity @e[type=kiyamet:%s,x=-900,y=0,z=-600,dx=2000,dy=400,dz=400] run say END_%s_OK' % (bs, bs.upper()), 1)
    cmd(p, 'execute if block 0 141 400 kiyamet:creation_table run say WISDOM_TABLE_OK', 1)
    # 1.6 more: auto turret, thrown bombs, cars, eggs, cash
    cmd(p, 'summon kiyamet:auto_turret 2 101 -4', 0.5)
    cmd(p, 'summon kiyamet:thrown_bomb 2 104 -2 {Item:{id:"kiyamet:frag_grenade",count:1}}', 0.5)
    cmd(p, 'summon kiyamet:thrown_bomb 4 104 -2 {Item:{id:"kiyamet:molotov",count:1}}', 0.5)
    cmd(p, 'summon kiyamet:car_sedan -6 101 -6', 0.5)
    cmd(p, 'setblock -3 101 3 kiyamet:vehicle_workshop', 0.3)
    for a in ('dollar_100', 'money_bag', 'runner_zombie_spawn_egg', 'car_sedan_kit', 'flashbang'):
        cmd(p, 'summon item 3 101 -3 {Item:{id:"kiyamet:%s",count:1}}' % a, 0.3)
    # 1.5 bullets: ammo, a loaded drum magazine, the giant shotgun
    for a in ('pistol_ammo', 'rifle_ammo', 'shotgun_shell', 'sniper_ammo', 'rocket_ammo', 'plasma_cell', 'behemoth_shotgun', 'ak_shotgun', 'ak47', 'bolt_sniper'):
        cmd(p, 'summon item 3 101 3 {Item:{id:"kiyamet:%s",count:1}}' % a, 0.3)
    cmd(p, 'summon item 3 101 3 {Item:{id:"kiyamet:magazine_drum",count:1,components:{"minecraft:custom_data":{Ammo:"SHELL",Rounds:200}}}}', 1)
    cmd(p, 'time add 168000', 1)
    cmd(p, 'time set 12400', 1)
    phase[0] = 'sprint'
    cmd(p, 'tick sprint 3000', 25)
    cmd(p, 'weather rain', 1)
    cmd(p, 'tick sprint 3000', 25)
    cmd(p, 'time add 24000', 1)
    cmd(p, 'tick sprint 1200', 15)
    cmd(p, 'save-all flush', 10)
    cmd(p, 'stop', 1)
    try: p.wait(240)
    except subprocess.TimeoutExpired: problems.append('[run1] server did not stop'); p.kill()

# ---------------------------------------------------------------- run 2: reload the saved world (save/load code), then kill everything (death hooks)
p = boot('run2')
if p:
    phase[0] = 'reload'
    cmd(p, 'forceload add -48 -48 48 48', 5)
    cmd(p, 'tick sprint 600', 10)
    cmd(p, 'execute as @e[type=!minecraft:player,type=!minecraft:item] run data get entity @s Health', 3)
    cmd(p, 'kill @e[type=!minecraft:player]', 3)
    cmd(p, 'tick sprint 600', 10)
    cmd(p, 'save-all flush', 10)
    cmd(p, 'stop', 1)
    try: p.wait(240)
    except subprocess.TimeoutExpired: problems.append('[run2] server did not stop'); p.kill()

log.close()
txt = open('smoke.log').read()
# collect exceptions / errors with a little context
ls = txt.splitlines()
for i, l in enumerate(ls):
    if re.search(r'Exception|ERROR\]|/ERROR|Error:|Caused by|Failed to|Unknown|Incorrect argument|Couldn\'t|could not|Could not find|That position is not loaded|Failed to place', l, re.I) and 'Can\'t keep up' not in l:
        problems.append('\n'.join(ls[i:i + 4]))
summon_ok = len(re.findall(r'Summoned new', txt))
placed_ok = len(re.findall(r'Generated structure', txt)) + len(re.findall(r'The nearest kiyamet:city', txt))
with open('smoke_report.txt', 'w') as f:
    f.write(f'summoned OK: {summon_ok}/{len(ENTS)}\nstructures placed OK: {placed_ok}/{len(STRUCTS)}\nproblems: {len(problems)}\nmax heap per phase (MB): {heap}\n\n')
    seen = set()
    for pr in problems:
        k = pr[:160]
        if k in seen: continue
        seen.add(k)
        f.write(pr + '\n---\n')
print(open('smoke_report.txt').read()[:3000])
