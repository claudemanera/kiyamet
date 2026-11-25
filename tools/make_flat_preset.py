"""Builds the 'Kiyamet: Flatter World' world preset from the vanilla overworld noise (run in CI with the client jar).
Terrain height variation (offset), squash factor and jaggedness are rescaled -> wider plains, lower mountains."""
import json, sys, zipfile, re
jar = zipfile.ZipFile(sys.argv[1])
OUT = 'src/main/resources/data/kiyamet/worldgen/'
DF = 'data/minecraft/worldgen/density_function/'
names = {n[len(DF):-5]: n for n in jar.namelist() if n.startswith(DF) and n.endswith('.json')}
load = lambda n: json.loads(jar.read(n))

WRAP = {  # replaced root functions
    'overworld/offset': {"type": "minecraft:add", "argument1": 0.0, "argument2": {"type": "minecraft:mul", "argument1": 0.55, "argument2": "minecraft:overworld/offset"}},
    'overworld/factor': {"type": "minecraft:mul", "argument1": 1.7, "argument2": "minecraft:overworld/factor"},
    'overworld/jaggedness': {"type": "minecraft:mul", "argument1": 0.2, "argument2": "minecraft:overworld/jaggedness"},
}
refs = lambda txt: set(re.findall(r'"minecraft:(overworld/[a-z0-9_/]+)"', txt))
# every vanilla overworld function that (transitively) uses a wrapped one gets a kiyamet copy
dirty = set(WRAP)
changed = True
while changed:
    changed = False
    for n, path in names.items():
        if n.startswith('overworld/') and n not in dirty and refs(jar.read(path).decode()) & dirty:
            dirty.add(n); changed = True

def rewrite(txt):
    for d in sorted(dirty, key=len, reverse=True):
        txt = txt.replace(f'"minecraft:{d}"', f'"kiyamet:flat/{d}"')
    return txt

for d in dirty:
    p = OUT + f'density_function/flat/{d}.json'
    import os; os.makedirs(os.path.dirname(p), exist_ok=True)
    body = json.dumps(WRAP[d]) if d in WRAP else rewrite(jar.read(names[d]).decode())
    open(p, 'w').write(body)
ns = jar.read('data/minecraft/worldgen/noise_settings/overworld.json').decode()
import os; os.makedirs(OUT + 'noise_settings', exist_ok=True); os.makedirs(OUT + 'world_preset', exist_ok=True)
open(OUT + 'noise_settings/flatter_overworld.json', 'w').write(rewrite(ns))
preset = load('data/minecraft/worldgen/world_preset/normal.json')
preset['dimensions']['minecraft:overworld']['generator']['settings'] = 'kiyamet:flatter_overworld'
json.dump(preset, open(OUT + 'world_preset/flatter.json', 'w'), indent=1)
print('flat preset: rewrote', sorted(dirty))
