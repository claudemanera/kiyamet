# Kıyamet – design notes
- All in-game text is English. Mod id: kiyamet. Display name: Kıyamet.
- Zombies are drawn bloody.
- Bosses are giants: render/hitbox scale ×2.0 (Hive Mother and Deep Horror ×2.5).
  storm_lord 2.0, swamp_witch 2.0, assembly_mother 2.0,
  frost_giant 2.0, lava_golem 2.0, shadow_stalker 2.0, the_butcher 2.0, time_keeper 2.0,
  patient_zero 2.0, the_general 2.0, forgotten_king 2.0, core_warden 2.0,
  hive_mother 2.5, experiment_47 2.0, deep_horror 2.5
- Power stones: vanilla amethyst shard recoloured at build time (tools/recolor_stones.py).
- Robots: advanced look (segmented limbs, panel lines, glowing seams). Tech cities get robot soldiers (regular enemies).

- Gadgets (20), Engineering level to craft: flashlight 25, sonic_lure 50, grappling_gun 75, night_vision 100, vein_miner 120, jetpack 150, healing_syringe 175, auto_turret 200, flamethrower 250, gravity_glove 300, structure_scanner 350, drone_controller 400, clone_device 450, teleport_gun 550, emp_bomb 600, laser_rifle 650, shield_generator 700, time_stopper 800, black_hole_bomb 900, energy_sword 999
- XP curve (per skill): xp_to_next(n) = round((120 + 55*n^0.42) * 1.018^max(0, n-880)); total ~1.14M XP per skill.
  Target pace: 2h/day, 999 in all skills ≈ 364h (~182 days). Avg 3,140 XP/hour per skill.
  Milestones: 100@12h, 250@41h, 500@103h, 750@178h, 900@229h, 990@340h, 999@364h (last 99 levels = 37% of time).
  Rested XP: first 2h of each real day +50%. Blood Moon x2. Actions far below skill level give 10% XP. Bosses big XP on first kill only.
- Arrow weapons (18): no draw, fire full-power arrows, cooldown per weapon; damage x(1+0.001*Archery level); use normal arrows; Archery XP; loud except silenced.
  arrow_pistol: archery 50, dmg 5, dps 10
  arrow_rifle: archery 100, dmg 10, dps 10
  double_arrow_rifle: archery 150, dmg 9 ×2, dps 15
  arrow_shotgun: archery 200, dmg 4 ×5, dps 13
  silenced_arrow_pistol: archery 250, dmg 8, dps 13
  arrow_repeater: archery 300, dmg 4, dps 16
  toxic_arrow_rifle: archery 350, dmg 10 + zehir 6, dps 13
  frost_arrow_gun: archery 400, dmg 12 + yavaşlatma, dps 12
  arrow_sniper: archery 450, dmg 30, delici, dps 12
  incendiary_repeater: archery 500, dmg 7 ×3 + ateş, dps 15
  tesla_arrow_gun: archery 550, dmg 14 + 3×8 sıçrama, dps 25
  arrow_gatling: archery 600, dmg 5, dps 40
  harpoon_gun: archery 650, dmg 18 + çekme, dps 12
  arrow_railgun: archery 700, dmg 60, hepsini deler, dps 20
  arrow_launcher: archery 750, dmg 25 alan, dps 8
  void_arrow_launcher: archery 850, dmg 20 + kara delik, dps —
  homing_arrow_gun: archery 900, dmg 22, güdümlü, dps 18
  apocalypse_cannon: archery 999, dmg 12 ×12 + 40 patlama, dps 30
- Healing: bandage 2 hearts + stops bleeding; medkit 8 hearts, 2s channel; healing_syringe instant + cures infection.
- Structures: 66 total = 59 single structures + 7 giant cities (jigsaw-assembled from city building pieces):
  1 Dead Metropolis 256x256, 2 Port City 224x192, 3 Industrial City 208x208, 4 Suburbs 224x224,
  5 Quarantine City 192x192, 6 Desert Oasis City 176x176, 7 Frozen Mountain City 192x192.
  Chests can use room-specific loot tables (kiyamet:chests/<structure>_<room>).

## Giant cities — BUILT (7/7)
Built with `zombi/city_lib.py` + `zombi/sehir_build.py`. Exported as 64x64 tiles in `data/kiyamet/structure/cities/<id>/t_i_j.nbt`,
manifest `data/kiyamet/city_layouts/<id>.json` (tiles + markers), structure type `kiyamet:tiled_city` (Java TODO, phase 5).
| id | size (XxYxZ) | tiles | boss | spacing/sep | loot tables |
|---|---|---|---|---|---|
| dead_metropolis | 256x90x256 | 16 | The General (HQ) | 300/120 | city_dead_metropolis, _hq (+ stamped mall/hotel/police/fire/school/pharmacy/gas/city center) |
| port | 224x60x192 | 12 | Deep Horror (under ship) | 250/100 | city_port, city_port_ship |
| industrial | 208x70x208 | 16 | Assembly Mother (robot factory) | 250/100 | city_industrial, _robot, _vaccine, _power |
| suburbs | 224x40x224 | 16 | — (3 survivors, barricaded house) | 200/80 | city_suburbs, city_suburbs_safe (+ mall/school/gas/pharmacy) |
| quarantine | 192x50x192 | 9 | Patient Zero (hospital lab) | 300/120 | city_quarantine, _hospital, _lab, _military |
| oasis | 176x40x176 | 9 | — (traders in caravanserai) | 220/90 | city_oasis, city_oasis_trader |
| frozen_mountain | 192x90x192 | 9 | Frost Giant (ice cave under avalanche) | 220/90 | city_mountain, _summit, _observatory, _cave |
Markers: zombie / loot / boss / survivor / guard -> spawned by the tiled_city Java piece on placement.
Structures progress: 11 single + 7 cities = 18/66. Next: singles 12–21 (medical set).

## Structure plan change: 70 total = 63 singles + 7 cities (user asked for mosque + church, small and large)
Pack 12–21 BUILT (zombi/yapilar_04.py):
12 small_mosque (sanctuary, 3 survivors) · 13 grand_mosque (sanctuary, 7 survivors + 2 guards, rooms: library, storeroom)
14 small_church (sanctuary, 2 survivors) · 15 cathedral (sanctuary, 6 survivors + guard, room: vestry)
Sanctuaries: no zombie spawns inside, interiors clean (no blood/decay) out of respect; survivors give rescue missions + trader.
16 abandoned_hospital (rooms pharmacy, surgery) · 17 quarantine_zone (military, medical) · 18 outbreak_lab (underground, sink 13, boss Patient Zero; samples, servers, containment)
19 morgue (office) · 20 field_hospital (supplies; wounded medic -> companion) · 21 asylum (boss The Butcher; office, kitchen)
Progress: 21 singles + 7 cities = 28/70. Next pack 22–31: Askeri (military).

Pack 22–31 BUILT (zombi/yapilar_05.py) — military:
22 military_checkpoint (office) · 23 army_base (armory, office, fuel, rations) · 24 bunker (underground sink 10; 2 guards; armory, comms, fuel, rations)
25 armory_depot (vault) · 26 crashed_helicopter (cargo) · 27 tank_graveyard · 28 radar_station (control, fuel)
29 missile_silo (underground sink 26, boss Experiment 47; control, vault) · 30 barracks (lockers, kitchen) · 31 border_outpost (tower)
Progress: 31 singles + 7 cities = 38/70. Next pack 32–41: underground.

## PLANNED (user request 2026-11-25): base defense blocks + barbed wire + 5 cars
Defense blocks (blast resistance / zombie break time): reinforced_planks, steel_block, steel_wall (pane-like), ruby_block, titanium_block, energy_wall (powered), steel_gate (2x3, opens with lever),
 barbed_wire (slows 70%, 2 dmg/s to zombies, wears out), razor_wire (stronger), spike_trap, electric_fence (needs generator).
Zombies break blocks by "durability points"; Tank/Alpha zombies damage walls faster; walls repairable with Repair Kit.
Cars: drivable entities (GeckoLib), fuel_canister fuel, health, trunk inventory, headlights (LambDynamicLights), horn lures zombies, ram damage by speed.
 5 cars: sedan, pickup (trunk), police car (siren), ambulance (heals passengers), armored SUV.

## BUILT 2026-11-25: base defense + cars (zombi/savunma.py, zombi/arabalar.py)
Blocks (assets done: textures, models, blockstates, items, lang): reinforced_planks(60) steel_block(150) steel_grate(120, cutout) ruby_block(260, fireproof, light 4)
 titanium_block(400, blast-proof) energy_wall(animated, needs power, knockback) steel_gate(door, 220) barbed_wire(slow 70%, 2/s, 60 uses) razor_wire(slow 80%, 3/s+bleed, 90 uses)
 electric_fence(6 dmg + 2s stun, needs power) spike_trap(10 dmg, 20 uses, hidden from zombies). Item: repair_kit (+50%, 100% at Engineering 100).
 Ruby: item ruby, ruby_ore + deepslate_ruby_ore, worldgen configured/placed feature ore_ruby (y -64..16, 4/chunk) -> Java BiomeModifications.addFeature.
 Generator (existing block model) = power source: burns fuel_canister, powers energy_wall/electric_fence within 8 blocks.
 Zombie wall damage: block "durability" map per position (SavedData), Tank/Alpha x3, Exploder x5 (titanium immune).
Cars (geo + textures + lang done): car_sedan(18 b/s, hp60, 9 slots) car_pickup(15, hp90, 27 slots, 2 passengers) car_police(17, hp75, siren lure/scare)
 car_ambulance(14, hp100, 18 slots, heals 0.5 heart/s) car_armored_suv(12, hp220, plow). Fuel: fuel_canister. Ram damage by speed. Repair with metal_plate.
 Keys: H horn (lures), L lights (LambDynamicLights), trunk = sneak+use. 5% chance a working car spawns in cities.

Pack 32–41 BUILT (zombi/yapilar_06.py) — underground. Exported with skip_air (untouched cells keep natural terrain; carved space = cave_air), step underground_structures.
32 ancient_underground_city (y-36, boss Forgotten King; treasury, throne: soul_stone 3%) · 33 abandoned_mine (sink 24, surface headframe; cart, foreman)
34 sewer_network (sink 12, manholes; maintenance, nest) · 35 cave_camp (sink 12, sanctuary 4 survivors + guard; trade)
36 underground_prison (y-24; armory, warden) · 37 crystal_cavern (y-30; shrine: earth/frost/time stone 2%)
38 hive_nest (y-28, boss Hive Mother; pod, core: nature_stone 3%) · 39 lava_forge (y-40, boss Lava Golem; forge, anvil: fire_stone 3%)
40 lost_library (y-24, boss Time Keeper; archive, sanctum: time_stone 3%) · 41 core_chamber (y-58, boss Core Warden; core: void/storm stone 3%)
Progress: 41 singles + 7 cities = 48/70. Next pack 42–51: technology.

Pack 42–51 BUILT (zombi/yapilar_07.py) — technology (new marker type "robot" = robot_trooper/robot_heavy/scout_drone spawn):
42 tech_outpost (console; 4 live turrets, EMP disables) · 43 research_tower (60 high; servers, director: teleport_gun/clone_device 2%)
44 data_center (servers, admin; terminal marks nearby structures on map) · 45 solar_farm (control, batteries; repair with 5 circuits -> wireless power to base)
46 drone_hangar (control, parts; drones wake + call robots) · 47 teleport_lab (console; portal: 1 energy_core -> teleport to random structure)
48 robot_plant (control, storage) · 49 weather_station (boss Storm Lord; storm on approach; console: storm_stone 3%)
50 cryo_vault (frozen survivors -> wake with healing_syringe = companion; medical, control) · 51 dome_settlement (64x64 glass dome; capture = zombie-free base with free power; block, core)
Progress: 51 singles + 7 cities = 58/70. Next pack 52–63 (12, final): nature / Terralith.

Pack 52–63 BUILT (zombi/yapilar_08.py) — nature / Terralith (final):
52 swamp_witch_hut (boss Swamp Witch; witch: nature_stone 3%) · 53 shadow_ruins (boss Shadow Stalker, invisible in light-less areas; altar: shadow_stone 3%)
54 treehouse_village (sanctuary, 5 survivors + 2 guards; store) · 55 fishing_village (sanctuary, 3 survivors + guard; nets)
56 buried_pyramid (bury; tomb x4, touching sarcophagus wakes 5 mummies; earth_stone 2%) · 57 canyon_camp (sink 22; camp, cache)
58 fire_lookout (ranger -> companion; spyglass reveals structures in 64 blocks) · 59 abandoned_farm (barn, loft, house; seeds)
60 jungle_ruins (idol: nature_stone 2%) · 61 frozen_ship (hold, captain: frost_stone 2%) · 62 mushroom_camp (sanctuary, 4 survivors; camp, trade)
63 volcano_station (lab: fire_stone 2%)
ALL STRUCTURES DONE: 63 singles + 7 cities = 70/70. Next: mod code phases (Java), then GitHub Actions build.

## CODE PHASE 1 (written 2026-11-25) — foundation
Gradle: loom 1.14-SNAPSHOT (net.fabricmc.fabric-loom-remap), Mojang mappings, loader 0.18.4, Fabric API 0.141.6+1.21.11, Java 21, Gradle 9.2.1 (CI).
CI: .github/workflows/build.yml downloads the 1.21.11 client jar, runs tools/recolor_stones.py, builds, uploads jar artifact.
Java: Kiyamet (main), registry/ModBlocks (13 blocks), ModItems (all items + block items + creative tab), ModWorldgen (ruby ore),
 skill/Skill, XpCurve, SkillData (record + Codec), ModAttachments (persistent, copyOnDeath), SkillManager (XP, rested +50% first 2h/day,
 level ups, perk messages, Health -> max hp +1 per 33 lvls), SkillEvents (block break, kills, damage, movement ticks), SkillCommand (/skills).
 client/KiyametClient: cutout/translucent render layers.
City structures moved to pending/ until kiyamet:tiled_city exists (phase 5) — otherwise datapack load fails.
Stone Codex: 6 pages (stone_codex_page_1..6) in research_tower_director, outbreak_lab_containment, data_center_servers,
 teleport_lab_console, weather_station_console, cryo_vault_control (35%); shapeless recipe -> written book "Stone Codex" (all stone info).
