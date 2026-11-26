"""Writes a short info text for every item into en_us.json as tooltip.kiyamet.<id> ('|' separates lines)."""
import json, os, re
A = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/kiyamet/lang/en_us.json')
L = json.load(open(A))
T = {}
STONE = {'fire': ('Fire resistance + Strength, your hits burn', 'Inferno: 12-block fire ring'), 'frost': ('Your hits freeze and slow', 'Blizzard: everything around frozen 10 s'),
         'storm': ('Speed II + Jump II, hits may call lightning', 'Chain storm: 6 lightning bolts'), 'nature': ('Regeneration, never starve', 'Full heal + roots enemies'),
         'blood': ('Strength, heal 25% of your damage', 'Blood drain: heavy area damage, half heals you'), 'shadow': ('Night vision, invisible while sneaking', '30 s invisible, faster, stronger'),
         'soul': ('Absorption shield every 10 s', '3 spirit golems for 3 minutes'), 'time': ('Haste II, all stone cooldowns -25%', 'Time stop: enemies frozen 12 s'),
         'void': ('Slow falling', 'Black hole: pulls and crushes for 4 s'), 'earth': ('No knockback + Resistance', 'Quake: launches and slows everything around')}
for k, (p, a) in STONE.items(): T[f'{k}_stone'] = f'Passive: {p}|Active: {a}|Socket it in a Stone Gauntlet'
T['stone_gauntlet'] = 'Holds up to 5 power stones (sockets open with Total Power)|Right-click: use the selected stone|Sneak + right-click: next stone · Left-click: gauntlet panel'
for i in range(1, 7): T[f'stone_codex_page_{i}'] = 'A page about the power stones|Collect all 6'
G = {'flashlight': 'Night vision for 1 minute', 'sonic_lure': 'Throws a loud noise 40 blocks away|Walkers go there instead of you · single use',
     'grappling_gun': 'Pulls you to the block you aim at (36 blocks)', 'vein_miner': 'Breaks up to 48 connected blocks of the same kind',
     'jetpack': 'Hold right-click to fly up', 'auto_turret': 'Place a 3D auto turret that shoots walkers,|raiders and masked ones (22 blocks) on its own|Faster on generator power · sneak + right-click picks it up',
     'flamethrower': 'Sprays fire in front of you', 'gravity_glove': 'Throws enemies away from you', 'clone_device': 'Makes a companion clone of you with your name|2 uses',
     'teleport_gun': 'Teleports you to the block you aim at (64 blocks)', 'emp_bomb': 'Throw it: slows everything in 16 blocks,|breaks robots',
     'shield_generator': 'Resistance III + big absorption for 20 s', 'time_stopper': 'Freezes enemies around you', 'black_hole_bomb': 'Throw it: pulls everything in 14 blocks|and crushes it (20 damage)',
     'energy_sword': 'Dash forward and cut through enemies', 'night_vision': 'Night vision for 5 minutes', 'healing_syringe': 'Heals you instantly',
     'structure_scanner': 'Finds the nearest structure', 'drone_controller': 'Controls your scout drone', 'laser_rifle': 'Hitscan laser, 16 damage, 64 blocks'}
T.update(G)
P = {'circuit': 'Crafting part for gadgets, guns and vehicles', 'energy_cell': 'Power part for energy gadgets', 'vaccine': 'Cures the infection after a walker bite',
     'bandage': 'Heals a little and stops bleeding', 'tech_core': 'Rare high-tech part', 'metal_plate': 'Armour plate for guns, vehicles and defenses',
     'fuel_canister': 'Refuels vehicles and generators', 'lens': 'Optics part for scopes and lasers', 'void_shard': 'Strange shard from the void', 'energy_core': 'Very rare power core',
     'medkit': 'Heals a lot · companions use it by themselves', 'repair_kit': 'Repairs a vehicle or a gun', 'ruby': 'A gem. Traders pay $30 for one',
     'walker_guts': 'Smear it on to walk among walkers (camouflage)', 'radio': 'Calls your companions (3 groups)', 'lost_note': 'A note somebody left behind - read it',
     'quest_item': 'Needed for a story mission', 'gun_tome': 'Blue tome: teaches one gun enchantment|Use it at the Enchantment Forge'}
T.update(P)
for r in ('trooper', 'drone', 'heavy'): T[f'robot_core_{r}'] = 'Right-click: build an allied robot'
for k in re.findall(r'"(car_\w+_keys|atv_keys|motorcycle_keys|armored_truck_keys)"', ' '.join(L)): pass
for v in ('car_sedan', 'car_pickup', 'car_police', 'car_ambulance', 'car_armored_suv', 'atv', 'motorcycle', 'armored_truck'):
    T[f'{v}_keys'] = 'Keys for your vehicle - keep them safe'
    T[f'{v}_kit'] = 'Place it on the ground to get the vehicle|Build kits at the Vehicle Workshop'
packs = {'canvas_satchel': 9, 'scout_pack': 14, 'hiker_pack': 18, 'survivor_pack': 24, 'ranger_pack': 30, 'medic_pack': 36, 'tactical_pack': 45, 'military_rucksack': 54, 'expedition_pack': 66, 'apocalypse_pack': 80}
for k, n in packs.items(): T[k] = f'Backpack: {n} slots|Sneak + right-click: wear it · U: open|Upgrade by crafting it with the next material'
B = {'lab_table': 'Laboratory: craft guns, gadgets, parts and ammo|Recipes open with Engineering', 'mission_board': 'Daily jobs and the story missions',
     'shelter_core': 'Claims your base: raids and safety', 'generator': 'Powers turrets, fences and lights|Stack up to 65 · needs fuel',
     'reinforced_planks': 'Walkers need longer to break it', 'steel_block': 'Very strong wall block', 'steel_grate': 'See-through steel wall', 'ruby_block': 'Nine rubies',
     'titanium_block': 'Almost unbreakable wall', 'energy_wall': 'Unbreakable glowing wall (needs power)', 'steel_gate': 'Steel door for your base',
     'barbed_wire': 'Slows and hurts walkers', 'razor_wire': 'Slows and hurts walkers more', 'electric_fence': 'Shocks walkers (needs power)', 'spike_trap': 'Hurts what walks on it',
     'ruby_ore': 'Mine it for rubies', 'deepslate_ruby_ore': 'Mine it for rubies', 'gun_bench': 'Gun bench: scopes, silencers, lights,|magazines, paint and repairs',
     'vehicle_workshop': 'Builds vehicle kits from parts|Each vehicle needs an Engineering level'}
T.update(B)
AM = {'pistol_ammo': ('9mm', 'pistols and SMGs'), 'rifle_ammo': ('7.62mm', 'AKs and rifles'), 'shotgun_shell': ('12 gauge', 'shotguns (1 shell = 5 pellets)'),
      'sniper_ammo': ('.50 BMG', 'sniper rifles'), 'rocket_ammo': ('Rocket', 'launchers, the cannon, the AK grenadier'), 'plasma_cell': ('Plasma', 'tesla, rail, void, homing, frost, toxic guns')}
for k, (n, u) in AM.items(): T[k] = f'{n} rounds for {u}|Load them into magazines'
for k, n in (('magazine_small', 32), ('magazine', 64), ('magazine_large', 128), ('magazine_drum', 256)):
    T[k] = f'Holds {n} rounds of one type|Right-click: load · sneak + right-click: empty|Guns take rounds from magazines first'
FOOD = {'tomato': 3, 'corn': 4, 'cucumber': 2, 'eggplant': 3, 'bell_pepper': 3, 'onion': 2, 'cabbage': 3, 'strawberry': 2, 'orange': 4, 'peach': 4}
for k, n in FOOD.items():
    T[k] = f'Food: restores {n} hunger' + ('|Grows on fruit trees, falls when ripe' if k in ('orange', 'peach') else '|Craft 1 into 2 seeds')
    if k not in ('orange', 'peach'): T[f'{k}_seeds'] = 'Plant on farmland · 8 growth stages|Seasons matter: winter rots open fields'
for t in ('orange', 'peach'):
    T[f'{t}_sapling'] = f'Grows into a {t} tree'; T[f'{t}_log'] = 'Fruit tree wood · 4 planks'; T[f'{t}_leaves'] = 'Blossoms, then fruit ripens|Right-click ripe leaves to pick'
FUR = {'wolf_pelt': 4, 'fox_pelt': 4.5, 'polar_pelt': 6, 'sheepskin': 3.5}
for k, w in FUR.items(): T[k] = f'Right-click: lines the armor you wear with fur|+{w:g}°C in the cold (more on the chest)|Shears + sneak: take it off'
GR = {'frag_grenade': 'Throw it: explodes (no block damage), loud', 'molotov': 'Throw it: sets the ground on fire', 'smoke_grenade': 'Throw it: thick smoke, enemies lose you',
      'flashbang': 'Throw it: blinds and stuns everything around'}
T.update(GR)
for v in (1, 5, 10, 20, 50, 100): T[f'dollar_{v}'] = f'${v} bill · the currency of the apocalypse|Traders, communities and villagers take it'
T['money_bag'] = 'Keeps your cash safe|Right-click: put your bills in · sneak: take $100 out|Counts as your money when you pay'
for k in L:
    if k.startswith('item.kiyamet.') and k.endswith('_spawn_egg'):
        T[k[13:]] = 'Use on a block to spawn it'
for k, v in T.items(): L[f'tooltip.kiyamet.{k}'] = v
json.dump(L, open(A, 'w'), indent=1, ensure_ascii=False)
ids = [k.split('.', 2)[2] for k in L if k.startswith('item.kiyamet.') or k.startswith('block.kiyamet.')]
print(len(T), 'tooltips; missing:', [i for i in ids if i not in T and not i.endswith('_crop')])
