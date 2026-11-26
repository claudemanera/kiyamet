"""Boots a real Fabric dedicated server with the mod, drives it through console commands and reports problems.
Run in CI from a folder containing fabric-server-launch.jar and mods/."""
import subprocess, threading, time, sys, re, os, queue

ENTS = open(sys.argv[1]).read().split()
STRUCTS = open(sys.argv[2]).read().split()
log = open('smoke.log', 'w')
lines = queue.Queue()
problems = []

def start():
    p = subprocess.Popen(['java', '-Xmx4G', '-jar', 'fabric-server-launch.jar', 'nogui'], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
    def pump():
        for l in p.stdout:
            log.write(l); log.flush()
            lines.put(l)
    threading.Thread(target=pump, daemon=True).start()
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
    for i, s in enumerate(STRUCTS):
        x, z = 600 + (i % 10) * 450, 600 + (i // 10) * 450
        cmd(p, f'place structure kiyamet:{s} {x} 0 {z}', 4)
        log.write(f'### placed {s}\n')
    for i, e in enumerate(ENTS):
        cmd(p, f'summon kiyamet:{e} {(i % 8) * 4 - 16} 150 {(i // 8) * 4 - 16}', 0.3)
    cmd(p, 'time set 12400', 1)
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
    cmd(p, 'forceload add -48 -48 48 48', 5)
    cmd(p, 'tick sprint 600', 10)
    cmd(p, 'execute as @e[type=!minecraft:player] run data get entity @s Health', 3)
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
    if re.search(r'Exception|ERROR\]|/ERROR|Error:|Caused by|Failed to|Unknown|Incorrect argument|Couldn\'t|could not', l, re.I) and 'Can\'t keep up' not in l:
        problems.append('\n'.join(ls[i:i + 4]))
summon_ok = len(re.findall(r'Summoned new', txt))
placed_ok = len(re.findall(r'Generated structure', txt))
with open('smoke_report.txt', 'w') as f:
    f.write(f'summoned OK: {summon_ok}/{len(ENTS)}\nstructures placed OK: {placed_ok}/{len(STRUCTS)}\nproblems: {len(problems)}\n\n')
    seen = set()
    for pr in problems:
        k = pr[:160]
        if k in seen: continue
        seen.add(k)
        f.write(pr + '\n---\n')
print(open('smoke_report.txt').read()[:3000])
