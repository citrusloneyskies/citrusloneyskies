"""Downsample 4x icon renders to crisp 32x32 item sprites with a dark outline (Minecraft item style)."""
import sys, os, glob
from PIL import Image

def make(src, dst, size=32, ss=4):
    im = Image.open(src).convert('RGBA')
    W = size
    px = im.load()
    out = Image.new('RGBA', (W, W), (0, 0, 0, 0))
    op = out.load()
    for y in range(W):
        for x in range(W):
            r = g = b = a = n = 0
            for dy in range(ss):
                for dx in range(ss):
                    c = px[x * ss + dx, y * ss + dy]
                    if c[3] > 0:
                        r += c[0]; g += c[1]; b += c[2]; n += 1
            if n >= ss * ss * 0.4:
                op[x, y] = (r // n, g // n, b // n, 255)
    # outline: transparent pixels next to opaque ones become a dark shade of their neighbour
    res = out.copy()
    rp = res.load()
    for y in range(W):
        for x in range(W):
            if op[x, y][3]:
                continue
            nb = [op[x + dx, y + dy] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= x + dx < W and 0 <= y + dy < W and op[x + dx, y + dy][3]]
            if nb:
                c = nb[0]
                rp[x, y] = (int(c[0] * 0.28), int(c[1] * 0.28), int(c[2] * 0.3), 255)
    res.save(dst)

if __name__ == '__main__':
    src_dir, dst_dir = sys.argv[1], sys.argv[2]
    os.makedirs(dst_dir, exist_ok=True)
    for f in sorted(glob.glob(os.path.join(src_dir, '*_big.png'))):
        name = os.path.basename(f)[:-8]
        make(f, os.path.join(dst_dir, name + '.png'))
        print('icon', name)
