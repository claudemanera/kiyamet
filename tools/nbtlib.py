"""Minimal NBT read/write (gzip, big-endian) for structure templates."""
import gzip, struct, io
END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BARR, STR, LIST, COMP, IARR, LARR = range(13)

class T:  # typed value
    def __init__(s, t, v): s.t, s.v = t, v
    def __repr__(s): return f'T({s.t},{s.v!r})'

def _r(f, t):
    if t == BYTE: return struct.unpack('>b', f.read(1))[0]
    if t == SHORT: return struct.unpack('>h', f.read(2))[0]
    if t == INT: return struct.unpack('>i', f.read(4))[0]
    if t == LONG: return struct.unpack('>q', f.read(8))[0]
    if t == FLOAT: return struct.unpack('>f', f.read(4))[0]
    if t == DOUBLE: return struct.unpack('>d', f.read(8))[0]
    if t == BARR: n = struct.unpack('>i', f.read(4))[0]; return f.read(n)
    if t == STR: n = struct.unpack('>H', f.read(2))[0]; return f.read(n).decode('utf-8')
    if t == LIST:
        et = f.read(1)[0]; n = struct.unpack('>i', f.read(4))[0]
        return (et, [_r(f, et) for _ in range(n)])
    if t == COMP:
        d = {}
        while True:
            tt = f.read(1)[0]
            if tt == END: return d
            n = struct.unpack('>H', f.read(2))[0]; k = f.read(n).decode('utf-8')
            d[k] = T(tt, _r(f, tt))
    if t == IARR: n = struct.unpack('>i', f.read(4))[0]; return list(struct.unpack(f'>{n}i', f.read(4 * n)))
    if t == LARR: n = struct.unpack('>i', f.read(4))[0]; return list(struct.unpack(f'>{n}q', f.read(8 * n)))
    raise ValueError(t)

def _w(f, t, v):
    if t == BYTE: f.write(struct.pack('>b', v))
    elif t == SHORT: f.write(struct.pack('>h', v))
    elif t == INT: f.write(struct.pack('>i', v))
    elif t == LONG: f.write(struct.pack('>q', v))
    elif t == FLOAT: f.write(struct.pack('>f', v))
    elif t == DOUBLE: f.write(struct.pack('>d', v))
    elif t == BARR: f.write(struct.pack('>i', len(v))); f.write(v)
    elif t == STR: b = v.encode('utf-8'); f.write(struct.pack('>H', len(b))); f.write(b)
    elif t == LIST:
        et, items = v; f.write(bytes([et])); f.write(struct.pack('>i', len(items)))
        for i in items: _w(f, et, i)
    elif t == COMP:
        for k, tv in v.items():
            f.write(bytes([tv.t])); b = k.encode('utf-8'); f.write(struct.pack('>H', len(b))); f.write(b); _w(f, tv.t, tv.v)
        f.write(b'\x00')
    elif t == IARR: f.write(struct.pack('>i', len(v))); f.write(struct.pack(f'>{len(v)}i', *v))
    elif t == LARR: f.write(struct.pack('>i', len(v))); f.write(struct.pack(f'>{len(v)}q', *v))

def load(path):
    with gzip.open(path, 'rb') as g: f = io.BytesIO(g.read())
    assert f.read(1)[0] == COMP; n = struct.unpack('>H', f.read(2))[0]; f.read(n)
    return _r(f, COMP)

def save(path, root):
    f = io.BytesIO(); f.write(bytes([COMP])); f.write(struct.pack('>H', 0)); _w(f, COMP, root)
    with gzip.open(path, 'wb') as g: g.write(f.getvalue())

def entity(x, y, z, eid, extra=None):
    nbt = {'id': T(STR, eid), 'PersistenceRequired': T(BYTE, 1)}
    if extra: nbt.update(extra)
    return {'pos': T(LIST, (DOUBLE, [x + .5, float(y), z + .5])), 'blockPos': T(LIST, (INT, [x, y, z])), 'nbt': T(COMP, nbt)}
