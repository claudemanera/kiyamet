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
