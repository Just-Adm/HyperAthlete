"""
Tiny vector illustration kit for the food pictures (used by gen_food_art.py).

Every picture is a list of filled/stroked paths on a 100 × 100 canvas, written out twice from the
same data: as SVG (preview sheets) and as an Android VectorDrawable (what the app draws).
Style: flat, rounded, three tones per object (base, shade, highlight), soft ground shadow, no outlines.
"""
import math, random, re

# ------------------------------------------------------------------ path building

def f(v):
    s = f"{v:.1f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def ellipse(cx, cy, rx, ry):
    k = 0.5523
    return (f"M{f(cx - rx)} {f(cy)}C{f(cx - rx)} {f(cy - ry * k)} {f(cx - rx * k)} {f(cy - ry)} {f(cx)} {f(cy - ry)}"
            f"C{f(cx + rx * k)} {f(cy - ry)} {f(cx + rx)} {f(cy - ry * k)} {f(cx + rx)} {f(cy)}"
            f"C{f(cx + rx)} {f(cy + ry * k)} {f(cx + rx * k)} {f(cy + ry)} {f(cx)} {f(cy + ry)}"
            f"C{f(cx - rx * k)} {f(cy + ry)} {f(cx - rx)} {f(cy + ry * k)} {f(cx - rx)} {f(cy)}Z")


def circle(cx, cy, r):
    return ellipse(cx, cy, r, r)


def rrect(x, y, w, h, r):
    r = min(r, w / 2, h / 2)
    k = 0.5523 * r
    return (f"M{f(x + r)} {f(y)}L{f(x + w - r)} {f(y)}C{f(x + w - r + k)} {f(y)} {f(x + w)} {f(y + r - k)} {f(x + w)} {f(y + r)}"
            f"L{f(x + w)} {f(y + h - r)}C{f(x + w)} {f(y + h - r + k)} {f(x + w - r + k)} {f(y + h)} {f(x + w - r)} {f(y + h)}"
            f"L{f(x + r)} {f(y + h)}C{f(x + r - k)} {f(y + h)} {f(x)} {f(y + h - r + k)} {f(x)} {f(y + h - r)}"
            f"L{f(x)} {f(y + r)}C{f(x)} {f(y + r - k)} {f(x + r - k)} {f(y)} {f(x + r)} {f(y)}Z")


def poly(pts, closed=True):
    d = "M" + "L".join(f"{f(x)} {f(y)}" for x, y in pts)
    return d + ("Z" if closed else "")


def smooth(pts, closed=True, tension=1.0):
    """Catmull-Rom through the points, as cubic Béziers."""
    n = len(pts)
    if n < 3:
        return poly(pts, closed)
    out = [f"M{f(pts[0][0])} {f(pts[0][1])}"]
    rng = range(n) if closed else range(n - 1)
    for i in rng:
        p0 = pts[(i - 1) % n] if closed or i > 0 else pts[0]
        p1 = pts[i]
        p2 = pts[(i + 1) % n]
        p3 = pts[(i + 2) % n] if closed or i + 2 < n else pts[-1]
        c1 = (p1[0] + (p2[0] - p0[0]) / 6 * tension, p1[1] + (p2[1] - p0[1]) / 6 * tension)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6 * tension, p2[1] - (p3[1] - p1[1]) / 6 * tension)
        out.append(f"C{f(c1[0])} {f(c1[1])} {f(c2[0])} {f(c2[1])} {f(p2[0])} {f(p2[1])}")
    return "".join(out) + ("Z" if closed else "")


def blob(cx, cy, rx, ry, seed=1, wobble=0.1, n=9, rot=0):
    r = random.Random(seed)
    pts = []
    for i in range(n):
        a = 2 * math.pi * i / n + rot
        k = 1 + r.uniform(-wobble, wobble)
        pts.append((cx + math.cos(a) * rx * k, cy + math.sin(a) * ry * k))
    return smooth(pts)


def mound(cx, base, w, h, seed=1, bumps=6, wobble=0.12, flat=4):
    """Dome of food sitting on a plate: soft bumpy top, slightly rounded base."""
    r = random.Random(seed)
    pts = []
    for i in range(bumps + 1):
        t = i / bumps
        a = math.pi * (1 - t)
        x = cx + math.cos(a) * w / 2
        y = base - math.sin(a) * h * (1 + r.uniform(-wobble, wobble) * (0.3 + math.sin(a)))
        pts.append((x, y))
    # base curve (front edge bulges down a little)
    pts += [(cx + w * 0.38, base + flat * 0.8), (cx, base + flat), (cx - w * 0.38, base + flat * 0.8)]
    return smooth(pts)


def wedge(cx, cy, r, a0, a1, ry=None):
    ry = ry or r
    x0, y0 = cx + math.cos(math.radians(a0)) * r, cy + math.sin(math.radians(a0)) * ry
    pts = [(cx, cy)]
    steps = 10
    for i in range(steps + 1):
        a = math.radians(a0 + (a1 - a0) * i / steps)
        pts.append((cx + math.cos(a) * r, cy + math.sin(a) * ry))
    return poly(pts)


def rot_pts(pts, cx, cy, deg):
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    return [(cx + (x - cx) * c - (y - cy) * s, cy + (x - cx) * s + (y - cy) * c) for x, y in pts]


def capsule(cx, cy, length, width, deg=0):
    """Rounded stick (fries, kofta fingers, rolls)."""
    hl, r = length / 2, width / 2
    pts = []
    for i in range(9):
        a = math.pi / 2 + math.pi * i / 8
        pts.append((cx - hl + r + math.cos(a) * r, cy + math.sin(a) * r))
    for i in range(9):
        a = -math.pi / 2 + math.pi * i / 8
        pts.append((cx + hl - r + math.cos(a) * r, cy + math.sin(a) * r))
    return smooth(rot_pts(pts, cx, cy, deg), tension=0.6)


def ellipse_rot(cx, cy, rx, ry, deg):
    pts = [(cx + math.cos(2 * math.pi * i / 12) * rx, cy + math.sin(2 * math.pi * i / 12) * ry) for i in range(12)]
    return smooth(rot_pts(pts, cx, cy, deg))


# ------------------------------------------------------------------ drawing list

class Art:
    def __init__(self):
        self.items = []

    def fill(self, d, color, alpha=1.0, evenodd=False):
        self.items.append({"d": d, "fill": color, "alpha": alpha, "eo": evenodd})
        return self

    def stroke(self, d, color, width, alpha=1.0):
        self.items.append({"d": d, "stroke": color, "sw": width, "alpha": alpha})
        return self

    def extend(self, other):
        self.items += other.items
        return self


def shade(hex_color, k):
    """k < 0 darker, k > 0 lighter."""
    h = hex_color.lstrip("#")
    r, g, b = int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)
    if k >= 0:
        r, g, b = (int(v + (255 - v) * k) for v in (r, g, b))
    else:
        r, g, b = (int(v * (1 + k)) for v in (r, g, b))
    return f"#{r:02X}{g:02X}{b:02X}"


_NUM = re.compile(r"-?\d+(?:\.\d+)?")


def bbox(art):
    xs, ys = [], []
    for it in art.items:
        if it.get("alpha", 1) <= 0.001:
            continue
        nums = [float(v) for v in _NUM.findall(it["d"])]
        pad = it.get("sw", 0) / 2
        for i in range(0, len(nums) - 1, 2):
            xs += [nums[i] - pad, nums[i] + pad]
            ys += [nums[i + 1] - pad, nums[i + 1] + pad]
    return min(xs), min(ys), max(xs), max(ys)


def fit(art, margin=4.0, max_scale=1.45):
    """Scale + offset that centres the picture in the 100 × 100 box, so every food fills its tile alike."""
    x0, y0, x1, y1 = bbox(art)
    w, h = x1 - x0, y1 - y0
    s = min((100 - 2 * margin) / w, (100 - 2 * margin) / h, max_scale)
    tx = 50 - (x0 + w / 2) * s
    ty = 50 - (y0 + h / 2) * s
    return s, tx, ty


def to_svg(art, size=100):
    s, tx, ty = fit(art)
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="{size}" height="{size}">',
           f'<g transform="translate({tx:.2f} {ty:.2f}) scale({s:.4f})">']
    for it in art.items:
        if "fill" in it:
            rule = ' fill-rule="evenodd"' if it.get("eo") else ""
            out.append(f'<path d="{it["d"]}" fill="{it["fill"]}" fill-opacity="{it["alpha"]:.2f}"{rule}/>')
        else:
            out.append(f'<path d="{it["d"]}" fill="none" stroke="{it["stroke"]}" stroke-width="{it["sw"]}" '
                       f'stroke-linecap="round" stroke-linejoin="round" stroke-opacity="{it["alpha"]:.2f}"/>')
    out.append("</g></svg>")
    return "".join(out)


def argb(hex_color, alpha):
    return f"#{int(round(alpha * 255)):02X}{hex_color.lstrip('#').upper()}"


def to_vector(art, name):
    out = ['<?xml version="1.0" encoding="utf-8"?>',
           f'<!-- {name}: generated by tools/gen_food_art.py, do not edit -->',
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
           '    android:width="96dp" android:height="96dp"',
           '    android:viewportWidth="100" android:viewportHeight="100">']
    s, tx, ty = fit(art)
    out.append(f'  <group android:scaleX="{s:.4f}" android:scaleY="{s:.4f}" android:translateX="{tx:.2f}" android:translateY="{ty:.2f}">')
    for it in art.items:
        if it.get("alpha", 1) <= 0.001:
            continue
        if "fill" in it:
            rule = ' android:fillType="evenOdd"' if it.get("eo") else ""
            out.append(f'    <path android:fillColor="{argb(it["fill"], it["alpha"])}"{rule} android:pathData="{it["d"]}"/>')
        else:
            out.append(f'    <path android:strokeColor="{argb(it["stroke"], it["alpha"])}" android:strokeWidth="{it["sw"]}" '
                       f'android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{it["d"]}"/>')
    out.append("  </group>")
    out.append("</vector>")
    return "\n".join(out) + "\n"
