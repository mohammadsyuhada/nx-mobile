#!/usr/bin/env python3
"""Pre-step for make_pads.py: the Android console's controller (a transparent PNG) to a clean 1000 px PNG.

The background removal left a hole in the right stick's cap; the left stick's cap (the same part, lit the same way)
is pasted over it, a disc with a soft edge, aligned by the ring around it. The controller is then cropped, centred on
a 1000 px canvas and fitted to 960 px, so make_pads.py needs no MARGIN for it. It stops if a hole is left (a
transparent region inside the body, not connected to the border).
Needs Pillow. Usage: prep_controller.py <controller.png> <out.png>   (e.g. ~/Downloads/controllers/android-controller.png)
"""
import sys
from collections import deque
from PIL import Image, ImageDraw, ImageFilter

LEFT_CAP = (325, 245)    # the stick caps' centres in the 1280 x 853 source
RIGHT_CAP = (795, 425)   # aligned to the left one by its ring (least colour difference)
CAP_R = 60               # covers the cap and its rim, where the hole reaches
FEATHER = 6
OPAQUE = 128             # alpha at or above this is body; below it, background (or a hole)


def holes(im):
    """The transparent pixels not reachable from the border through transparent pixels."""
    w, h = im.size
    a = im.getchannel("A").load()
    clear = lambda x, y: a[x, y] < OPAQUE
    outside = bytearray(w * h)
    q = deque((x, y) for x in range(w) for y in (0, h - 1) if clear(x, y))
    q.extend((x, y) for y in range(h) for x in (0, w - 1) if clear(x, y))
    for x, y in q: outside[y * w + x] = 1
    while q:
        x, y = q.popleft()
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < w and 0 <= ny < h and not outside[ny * w + nx] and clear(nx, ny):
                outside[ny * w + nx] = 1
                q.append((nx, ny))
    return sum(1 for y in range(h) for x in range(w) if clear(x, y) and not outside[y * w + x])


def main(src, out):
    im = Image.open(src).convert("RGBA")
    if im.size != (1280, 853): sys.exit(f"expected the 1280 x 853 controller.png, got {im.size}")
    r, f = CAP_R, FEATHER
    lx, ly = LEFT_CAP
    patch = im.crop((lx - r - f, ly - r - f, lx + r + f, ly + r + f))
    mask = Image.new("L", patch.size, 0)
    ImageDraw.Draw(mask).ellipse((f, f, f + 2 * r, f + 2 * r), fill=255)
    im.paste(patch, (RIGHT_CAP[0] - r - f, RIGHT_CAP[1] - r - f), mask.filter(ImageFilter.GaussianBlur(f / 2)))
    if (left := holes(im)): sys.exit(f"{left} hole pixels left")
    im = im.crop(im.getchannel("A").point(lambda v: 255 if v > 10 else 0).getbbox())
    im.thumbnail((960, 960), Image.LANCZOS)
    canvas = Image.new("RGBA", (1000, 1000), (0, 0, 0, 0))
    canvas.paste(im, ((1000 - im.width) // 2, (1000 - im.height) // 2))
    canvas.save(out)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
