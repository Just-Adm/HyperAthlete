"""
Food pictures, part 3: dairy, fruit, vegetables, nuts, sweets, drinks, supplements and pantry basics.
"""
import math

from gen_food_art import *  # noqa: F401,F403
from foodart import (blob, capsule, circle, ellipse, ellipse_rot, mound, poly, rot_pts, rrect, shade, smooth,
                     wedge)
from food_art_more import clip_line, leaf, lettuce_leaf, onion_curls, sauce_pool


# ------------------------------------------------------------------ helpers

def glass(a, cx, liquid, top=24, bottom=86, wt=34, wb=26, level=0.78, foam=None, alpha=1.0, shadow=True):
    if shadow:
        ground(a, wb * 0.75, bottom + 2)
    lt, rt, lb, rb = cx - wt / 2, cx + wt / 2, cx - wb / 2, cx + wb / 2
    a.fill(poly([(lt, top), (rt, top), (rb, bottom), (lb, bottom)]), GLASS, 0.55)
    ly = bottom - (bottom - top) * level
    k = (ly - top) / (bottom - top)
    ll, lr = lt + (lb - lt) * k, rt + (rb - rt) * k
    a.fill(poly([(ll + 1, ly), (lr - 1, ly), (rb - 1, bottom - 1.2), (lb + 1, bottom - 1.2)]), liquid, alpha)
    a.fill(poly([(lr - 7, ly), (lr - 1, ly), (rb - 1, bottom - 1.2), (rb - 5, bottom - 1.2)]), shade(liquid, -0.12), 0.6 * alpha)
    a.fill(ellipse(cx, ly, (lr - ll) / 2 - 1, 2.8), foam or shade(liquid, 0.2), alpha if not foam else 1)
    a.fill(poly([(lt + 3, top + 4), (lt + 7, top + 4), (lb + 6, bottom - 5), (lb + 3, bottom - 5)]), WHITE, 0.45)
    a.fill(ellipse(cx, bottom - 0.6, wb / 2, 2.2), shade(GLASS, -0.15), 0.8)
    a.stroke(ellipse(cx, top, wt / 2, 3), shade(GLASS, -0.22), 1)
    return ly, ll, lr


def saucer_at(a, cx, cy, rx, ry=None, color=WHITE):
    ry = ry or rx * 0.26
    ground(a, rx * 0.9, cy + ry + 2)
    a.fill(ellipse(cx, cy + 1.4, rx, ry), shade(color, -0.14))
    a.fill(ellipse(cx, cy, rx, ry), color)
    a.fill(ellipse(cx, cy, rx * 0.5, ry * 0.45), shade(color, -0.05))


def cup(a, cx, top, w, h, body, liquid, handle=True, band=None):
    if handle:
        a.stroke(ellipse(cx + w / 2 + 1.5, top + h * 0.42, w * 0.16, h * 0.24), shade(body, -0.1), max(2.2, w * 0.08))
    d = (f"M{cx - w / 2} {top}C{cx - w / 2} {top + h * 0.8} {cx - w * 0.3} {top + h} {cx} {top + h}"
         f"C{cx + w * 0.3} {top + h} {cx + w / 2} {top + h * 0.8} {cx + w / 2} {top}Z")
    a.fill(d, body)
    a.fill(f"M{cx + w * 0.12} {top + h * 0.98}C{cx + w * 0.36} {top + h * 0.9} {cx + w / 2} {top + h * 0.6} {cx + w / 2} {top}"
           f"L{cx + w * 0.36} {top}C{cx + w * 0.36} {top + h * 0.6} {cx + w * 0.3} {top + h * 0.85} {cx + w * 0.12} {top + h * 0.98}Z", shade(body, -0.1))
    if band:
        a.fill(rrect(cx - w / 2 + 0.6, top + h * 0.2, w - 1.2, h * 0.1, 0.5), band)
    a.fill(ellipse(cx, top, w / 2, w * 0.13), shade(body, -0.18))
    a.fill(ellipse(cx, top + 0.6, w / 2 - 1.6, w * 0.13 - 1.2), liquid)
    return (cx, top + 0.6, w / 2 - 1.6, w * 0.13 - 1.2)


def mint_sprig(a, x, y, s=1.0, deg=0):
    for i, (dx, dy, d) in enumerate([(-3, -2, -40), (3, -3, 40), (0, -7, 0), (-2, -10, -30), (2, -11, 30)]):
        px, py = rot_pts([(x + dx * s, y + dy * s)], x, y, deg)[0]
        a.fill(leaf(px, py, 7 * s, 4 * s, d + deg - 90), "#3E9A3A" if i % 2 else "#58B04A")
        a.stroke(poly(rot_pts([(px - 2 * s, py), (px + 2 * s, py)], px, py, d + deg - 90), closed=False), "#8ED07A", 0.6, 0.8)


def heap(a, cx, cy, rx, ry, n, seed, draw):
    """Pile of small items, back to front."""
    g = rnd(seed)
    pts = []
    for i in range(n):
        t = g.uniform(0, 2 * math.pi)
        k = math.sqrt(g.uniform(0, 1))
        x, y = cx + math.cos(t) * rx * k, cy + math.sin(t) * ry * k - (1 - k) * ry * 0.6
        pts.append((y, x, g.uniform(0, 360), g.randint(0, 999)))
    for y, x, d, s in sorted(pts):
        draw(a, x, y, d, s)


def nut_bowl(a, draw, n=26, surface="#E9D3A8"):
    cx, cy, rx, ry = small_bowl(a, surface, rim_y=56, rx=30, ry=9, depth=24)
    heap(a, cx, cy - 2, rx * 0.8, ry * 0.9, n, 3, draw)


def stripe_jar(a, content, lid, label, mark):
    jar(a, content, lid, label=label, top=26, bottom=86, w=44)
    mark(a, 50, 65)


# ================================================================== dairy

def carton(a, color, cap):
    x, top, w, h, side = 22, 34, 30, 50, 12
    ground(a, 34, top + h + 3)
    a.fill(poly([(x + w, top), (x + w + side, top - 5), (x + w + side, top + h - 5), (x + w, top + h)]), "#DDE2E8")
    a.fill(poly([(x + w, top + h * 0.38), (x + w + side, top + h * 0.38 - 5), (x + w + side, top + h * 0.78 - 5), (x + w, top + h * 0.78)]), shade(color, -0.2))
    a.fill(poly([(x, top), (x + w, top), (x + w, top + h), (x, top + h)]), WHITE)
    a.fill(poly([(x, top + h * 0.38), (x + w, top + h * 0.38), (x + w, top + h * 0.78), (x, top + h * 0.78)]), color)
    a.fill(circle(x + w / 2, top + h * 0.58, 5.5), WHITE, 0.9)
    a.fill(ellipse(x + w / 2, top + h * 0.6, 3, 2), color, 0.5)
    a.fill(poly([(x, top), (x + w, top), (x + w + side / 2, top - 12), (x + side / 2, top - 12)]), "#F2F4F7")
    a.fill(poly([(x + w, top), (x + w + side, top - 5), (x + w + side / 2, top - 12)]), "#C9CFD7")
    a.fill(poly([(x + side / 2, top - 15), (x + w + side / 2, top - 15), (x + w + side / 2, top - 12), (x + side / 2, top - 12)]), "#E3E7EC")
    a.fill(ellipse(x + w * 0.7, top - 6, 3.6, 1.8), cap)
    a.fill(rrect(x + 2, top + 3, 3, h - 8, 1.5), "#EEF1F5")


def milk(a, color, cap):
    carton(a, color, cap)
    glass(a, 70, "#FBFBF8", top=52, bottom=88, wt=24, wb=19, level=0.8)


@art("milk_full")
def _milk_full(a):
    milk(a, "#2F7BC9", "#2F7BC9")


@art("milk_low")
def _milk_low(a):
    milk(a, "#4FB0A0", "#4FB0A0")


@art("laban")
def _laban(a):
    tub(a, "#FFFFFF", "#2F6FC2", "#E8F1FB", top=34, bottom=86, w=50)
    a.fill(circle(50, 66, 7), "#2F6FC2", 0.9)
    a.fill(ellipse(50, 67, 4, 2.6), WHITE)


@art("ayran")
def _ayran(a):
    ly, ll, lr = glass(a, 48, "#F8F7F1", top=22, bottom=86, wt=34, wb=27, level=0.8, foam="#FFFFFF")
    dots(a, "#EDEBE2", 48, ly - 1, 12, 2, 8, 0.8, 3)
    mint_sprig(a, 58, ly - 1, 0.9, 20)


@art("greek_yogurt")
def _greek_yogurt(a):
    ground(a, 26, 86)
    a.fill(poly([(26, 46), (74, 46), (68, 86), (32, 86)]), "#FFFFFF")
    a.fill(poly([(58, 46), (74, 46), (68, 86), (60, 86)]), "#E8EBF0")
    a.fill(poly([(27, 58), (73, 58), (71.5, 70), (28.5, 70)]), "#2F7BC9")
    a.fill(ellipse(50, 46, 24, 7), "#E3E7EC")
    a.fill(ellipse(50, 46.6, 22, 6), "#F8F7F2")
    a.fill(blob(48, 44, 12, 4.5, 3, 0.2, 8), WHITE)
    swirl(a, 48, 45, 8, 2.4, "#E6E3DA", 1.1, 0.9)
    # peeled lid
    a.fill(ellipse_rot(70, 34, 16, 6, -35), "#C9CFD7")
    a.fill(ellipse_rot(70, 33.4, 15, 5.4, -35), "#E3E7EC")
    a.stroke(poly([(40, 50), (30, 26)], closed=False), "#B9BEC6", 2.4)
    a.fill(ellipse_rot(41, 48, 3.4, 2.2, -65), "#C9CED6")


@art("cheddar")
def _cheddar(a):
    ground(a, 34, 84)
    top = [(16, 58), (66, 40), (86, 50), (38, 70)]
    front = [(16, 58), (38, 70), (38, 84), (16, 72)]
    side = [(38, 70), (86, 50), (86, 64), (38, 84)]
    a.fill(poly(side), "#D9821E")
    a.fill(poly(front), "#E99630")
    a.fill(poly(top), "#F6B23E")
    for x, y, r in [(52, 66, 2.2), (68, 60, 1.6), (62, 70, 1.8), (28, 66, 1.5), (78, 58, 1.3)]:
        a.fill(ellipse(x, y, r * 0.8, r), "#C46F12", 0.8)
    a.fill(poly([(22, 58), (60, 44), (66, 46), (28, 61)]), "#FBCB6A", 0.7)


@art("mozzarella")
def _mozzarella(a):
    cx, cy, rx, ry = small_bowl(a, "#EDF2F6", rim_y=58, rx=32, ry=9.5, depth=22, body="#DCE6EE")
    for i, (x, y, r) in enumerate([(cx - 11, cy - 3, 9), (cx + 10, cy - 4, 8.5), (cx, cy - 8, 8)]):
        a.fill(circle(x, y + 1, r), "#E6E1D3")
        a.fill(circle(x, y, r), "#FFFDF6")
        a.fill(ellipse(x - r * 0.3, y - r * 0.35, r * 0.4, r * 0.25), WHITE)
    for x, y, d in [(cx + 4, cy - 15, -20), (cx + 10, cy - 13, 30)]:
        a.fill(leaf(x, y, 9, 5, d), "#3E8E2E")


# ================================================================== fruit

def banana(a, cx, cy, L, W, deg, color="#F7D23E"):
    up, low = [], []
    for i in range(13):
        t = i / 12
        x = cx + (t - 0.5) * L
        yc = cy + ((t - 0.5) ** 2) * -L * 0.55
        w = W * (math.sin(math.pi * t) ** 0.55) * 0.5 + 0.6
        up.append((x, yc - w))
        low.append((x, yc + w))
    pts = rot_pts(up + low[::-1], cx, cy, deg)
    a.fill(smooth(pts, tension=0.5), color)
    hl = rot_pts([(x, y + W * 0.3) for x, y in low[3:10]] + [(x, y + W * 0.05) for x, y in low[3:10]][::-1], cx, cy, deg)
    a.fill(smooth(hl, tension=0.5), shade(color, -0.18), 0.7)
    a.fill(smooth(rot_pts([(x, y + W * 0.18) for x, y in up[3:9]] + [(x, y + W * 0.35) for x, y in up[3:9]][::-1], cx, cy, deg), tension=0.5),
           shade(color, 0.35), 0.8)
    e0 = rot_pts([up[0], low[0]], cx, cy, deg)
    a.fill(circle((e0[0][0] + e0[1][0]) / 2, (e0[0][1] + e0[1][1]) / 2, 1.6), "#5A4020")
    return rot_pts([up[-1], low[-1]], cx, cy, deg)


@art("banana", "addon_banana")
def _banana(a):
    ground(a, 34, 82)
    for i, (dy, d, col) in enumerate([(-8, -12, "#F2C83A"), (0, -4, "#F7D23E"), (8, 4, "#FADC55")]):
        end = banana(a, 48, 60 + dy, 66, 13, d, col)
    a.fill(rrect(78, 44, 7, 18, 3), "#8A6A2E")
    a.fill(rrect(78, 40, 7, 6, 2.5), "#5E7A2A")


def round_fruit(a, cx, cy, r, color, seed=1, dip=True):
    a.fill(blob(cx, cy, r, r * 0.95, seed, 0.04, 10), color)
    if dip:
        a.fill(ellipse(cx, cy - r * 0.86, r * 0.32, r * 0.14), shade(color, -0.3), 0.8)
    a.fill(blob(cx + r * 0.25, cy + r * 0.35, r * 0.7, r * 0.5, seed + 2, 0.1, 8), shade(color, -0.14), 0.6)
    a.fill(ellipse_rot(cx - r * 0.38, cy - r * 0.32, r * 0.24, r * 0.38, 35), shade(color, 0.4), 0.7)


@art("apple")
def _apple(a):
    ground(a, 26, 86)
    pts = [(50, 38), (60, 32), (72, 38), (76, 56), (68, 78), (58, 84), (50, 82), (42, 84), (32, 78), (24, 56), (28, 38), (40, 32)]
    a.fill(smooth(pts), "#D7322B")
    a.fill(smooth([(56, 40), (70, 42), (74, 58), (66, 76), (56, 80), (64, 60)]), "#B2241F", 0.7)
    a.fill(ellipse_rot(36, 50, 4, 9, 25), "#F27A6E", 0.8)
    a.stroke(smooth([(50, 40), (51, 32), (54, 24)], closed=False), "#6A4122", 2.4)
    a.fill(leaf(62, 28, 16, 8, -25), "#5DAA3A")
    a.stroke(poly(rot_pts([(56, 28), (68, 28)], 62, 28, -25), closed=False), "#8ED07A", 0.8)


@art("orange")
def _orange(a):
    ground(a, 34, 86)
    round_fruit(a, 40, 60, 22, "#F29A1F", 3)
    dots(a, "#D9800F", 40, 60, 16, 16, 18, 0.5, 4, 0.6)
    a.fill(leaf(44, 36, 12, 6, -30), "#4E9A34")
    # half
    a.fill(ellipse(68, 76, 18, 9), "#E4880F")
    a.fill(ellipse(68, 74, 18, 9), "#F6A62A")
    a.fill(ellipse(68, 74, 15.6, 7.6), "#FBE3A4")
    a.fill(ellipse(68, 74, 14.6, 7), "#F8B33E")
    for i in range(10):
        a0, a1 = i * 36 + 4, (i + 1) * 36 - 4
        a.fill(wedge(68, 74, 13, a0, a1, 6.2), "#FBC458")
    a.fill(ellipse(68, 74, 2, 1), "#FBE3A4")


def strawberry(a, x, y, s=1.0, deg=0, seed=1):
    pts = rot_pts([(x - 9 * s, y - 6 * s), (x - 4 * s, y - 9 * s), (x + 4 * s, y - 9 * s), (x + 9 * s, y - 6 * s),
                   (x + 7 * s, y + 3 * s), (x, y + 12 * s), (x - 7 * s, y + 3 * s)], x, y, deg)
    a.fill(smooth(pts), "#E0262F")
    a.fill(smooth(rot_pts([(x + 2 * s, y - 7 * s), (x + 8 * s, y - 5 * s), (x + 6 * s, y + 3 * s), (x + 1 * s, y + 10 * s)], x, y, deg)), "#B91B24", 0.6)
    g = rnd(seed)
    for _ in range(9):
        px, py = x + g.uniform(-6, 6) * s, y + g.uniform(-6, 6) * s
        if abs(px - x) < (8 - (py - y + 6) * 0.45) * s:
            qx, qy = rot_pts([(px, py)], x, y, deg)[0]
            a.fill(ellipse_rot(qx, qy, 0.5 * s, 0.8 * s, deg), "#FBE36A")
    for d in (-60, -20, 20, 60):
        lx, ly = rot_pts([(x + math.sin(math.radians(d)) * 5 * s, y - 9 * s - math.cos(math.radians(d)) * 1.5 * s)], x, y, deg)[0]
        a.fill(leaf(lx, ly, 7 * s, 3.4 * s, d - 90 + deg), "#3E9A3A")
    sx, sy = rot_pts([(x, y - 11 * s)], x, y, deg)[0]
    a.fill(capsule(sx, sy - 1.5 * s, 4 * s, 1.6 * s, 90 + deg), "#4E7A2B")


@art("strawberry", "addon_strawberry")
def _strawberry(a):
    ground(a, 30, 84)
    strawberry(a, 36, 64, 1.45, -18, 2)
    strawberry(a, 62, 68, 1.35, 15, 5)
    strawberry(a, 50, 50, 1.15, 0, 8)


@art("grapes")
def _grapes(a):
    ground(a, 24, 88)
    a.stroke(smooth([(50, 30), (52, 22), (58, 16)], closed=False), "#6A4A22", 2.4)
    a.fill(leaf(36, 24, 18, 11, 15), "#5DAA3A")
    rows = [(34, [-14, -4, 6, 16]), (43, [-10, 0, 10, 19]), (52, [-13, -3, 7, 16]), (61, [-8, 2, 12]), (70, [-3, 7]), (78, [2])]
    g = rnd(4)
    for y, xs in rows:
        for x in xs:
            cx_ = 48 + x + g.uniform(-1, 1)
            a.fill(circle(cx_, y, 6), "#6E2E78")
            a.fill(circle(cx_ + 1, y + 1, 4.6), "#5A2364", 0.7)
            a.fill(ellipse(cx_ - 2, y - 2, 1.8, 1.2), "#B887C2", 0.9)


@art("watermelon")
def _watermelon(a):
    ground(a, 38, 84)
    cx, cy = 50, 46
    outer = [(cx + math.cos(math.radians(t)) * 40, cy + math.sin(math.radians(t)) * 34) for t in range(10, 171, 10)]
    a.fill(poly([(cx + 40, cy + 6)] + outer + [(cx - 40, cy + 6)]), "#2E7A2A")
    inner = [(cx + math.cos(math.radians(t)) * 35, cy + math.sin(math.radians(t)) * 29) for t in range(10, 171, 10)]
    a.fill(poly([(cx + 37, cy + 5)] + [(cx + math.cos(math.radians(t)) * 37.6, cy + math.sin(math.radians(t)) * 31.6) for t in range(10, 171, 10)] + [(cx - 37, cy + 5)]), "#DDF0B5")
    a.fill(poly([(cx + 35, cy + 4)] + inner + [(cx - 35, cy + 4)]), "#EA3F4B")
    a.fill(poly([(cx - 35, cy + 4), (cx + 35, cy + 4), (cx + 34, cy + 9), (cx - 34, cy + 9)]), "#F2626C")
    for x, y in [(cx - 18, cy + 14), (cx - 6, cy + 20), (cx + 8, cy + 18), (cx + 20, cy + 12), (cx, cy + 10), (cx - 12, cy + 26), (cx + 12, cy + 27)]:
        a.fill(ellipse_rot(x, y, 1.1, 1.9, (x - cx) * 0.8), "#2B1A16")
    a.fill(poly([(cx - 35, cy + 4), (cx + 35, cy + 4), (cx + 40, cy + 6), (cx - 40, cy + 6)]), "#C92F3B", 0.0)


@art("melon")
def _melon(a):
    ground(a, 34, 84)
    a.fill(ellipse(50, 64, 36, 20), "#C9B27A")
    for i in range(-7, 8):
        for sgn in (1, -1):
            seg = clip_line((50 + i * 7 - sgn * 10, 44), (50 + i * 7 + sgn * 10, 86), 50, 64, 34.5, 18.6)
            if seg:
                a.stroke(poly(seg, closed=False), "#A8915A", 0.7, 0.6)
    a.fill(ellipse(50, 58, 33, 14), "#9EBF5A")
    a.fill(ellipse(50, 58, 31, 12.6), "#F6A04A")
    a.fill(ellipse(50, 58, 25, 9.4), "#F9B76A")
    a.fill(ellipse(50, 58, 15, 5.6), "#E68A2E")
    for i in range(14):
        t = i / 14 * 2 * math.pi
        a.fill(ellipse_rot(50 + math.cos(t) * 10, 58 + math.sin(t) * 3.4, 1.6, 0.8, math.degrees(t)), "#FCEBC2")


@art("mango")
def _mango(a):
    ground(a, 30, 86)
    pts = [(28, 54), (36, 38), (52, 32), (68, 38), (76, 54), (72, 72), (58, 82), (40, 80), (30, 70)]
    a.fill(smooth(pts), "#F2A03A")
    a.fill(smooth([(28, 54), (36, 38), (52, 32), (60, 34), (48, 48), (36, 64), (30, 66)]), "#E8503A", 0.85)
    a.fill(smooth([(54, 70), (70, 60), (74, 66), (64, 80), (56, 80)]), "#8DB03A", 0.75)
    a.fill(ellipse_rot(40, 46, 5, 9, 40), "#F9C77A", 0.6)
    a.fill(leaf(62, 28, 18, 7, -20), "#4E9A34")
    a.stroke(smooth([(52, 33), (54, 28), (58, 26)], closed=False), "#6A4A22", 1.8)


@art("dates")
def _dates(a):
    cx, cy, rx, ry = plate(a)
    for i, (x, y, d) in enumerate([(34, cy - 4, -20), (48, cy - 7, 10), (62, cy - 4, -10), (42, cy + 3, 15), (57, cy + 4, -25)]):
        a.fill(ellipse_rot(x, y + 1, 9.6, 5.4, d), "#3E1A0E")
        a.fill(ellipse_rot(x, y, 9.4, 5.2, d), "#6E2E14")
        a.fill(ellipse_rot(x - 1.6, y - 1.6, 4.2, 1.6, d), "#A04A24", 0.8)
        cx_, cy_ = rot_pts([(x - 9, y)], x, y, d)[0]
        a.fill(circle(cx_, cy_, 1.4), "#C9A26A")


@art("figs")
def _figs(a):
    ground(a, 34, 84)
    pts = [(30, 30), (34, 40), (44, 52), (46, 66), (36, 78), (20, 76), (14, 62), (20, 48), (26, 40)]
    a.fill(smooth(pts), "#5E2B57")
    a.fill(smooth([(30, 34), (36, 46), (40, 60), (34, 70), (30, 56), (28, 44)]), "#8A4A80", 0.7)
    a.fill(capsule(30, 28, 6, 2.6, 80), "#6E8A3A")
    # half
    a.fill(blob(64, 66, 20, 15, 3, 0.05, 10), "#5E2B57")
    a.fill(blob(64, 66, 17.6, 12.8, 3, 0.05, 10), "#F2E3C2")
    a.fill(blob(64, 67, 13.6, 9.6, 3, 0.08, 10), "#D8384E")
    a.fill(blob(64, 67, 9, 6, 5, 0.12, 9), "#B4223A")
    dots(a, "#F7D9A6", 64, 67, 10, 7, 22, 0.55, 6)


@art("pomegranate")
def _pomegranate(a):
    ground(a, 34, 86)
    round_fruit(a, 38, 58, 22, "#C9252F", 5, dip=False)
    for i, dx in enumerate((-5, 0, 5)):
        a.fill(poly([(38 + dx - 3, 38), (38 + dx, 31 - (i % 2) * 2), (38 + dx + 3, 38)]), "#9E1B26")
    a.fill(rrect(32, 36, 12, 5, 2), "#A51D28")
    a.fill(ellipse(68, 76, 19, 10), "#B51F2B")
    a.fill(ellipse(68, 74, 19, 10), "#F0DCC0")
    g = rnd(5)
    for _ in range(36):
        t = g.uniform(0, 2 * math.pi)
        k = math.sqrt(g.uniform(0, 1))
        x, y = 68 + math.cos(t) * 15 * k, 74 + math.sin(t) * 7.4 * k
        a.fill(ellipse(x, y, 1.6, 1.3), g.choice(["#D8243A", "#E8394C", "#B51A30"]))
        a.fill(circle(x - 0.5, y - 0.4, 0.45), "#FFB3BE", 0.9)


@art("pineapple")
def _pineapple(a):
    ground(a, 22, 90)
    for d, l, col in [(-40, 22, "#4E9A34"), (40, 22, "#4E9A34"), (-15, 28, "#5DAA3A"), (15, 28, "#5DAA3A"), (0, 30, "#6DBE45")]:
        x, y = rot_pts([(50, 40 - l / 2)], 50, 42, d)[0]
        a.fill(leaf(x, y, l, 6, d - 90), col)
    a.fill(ellipse(50, 66, 19, 24), "#E6A12A")
    a.fill(ellipse(56, 68, 12, 20), "#C9821C", 0.6)
    for i in range(-6, 7):
        p = []
        for u in range(-1, 2):
            pass
        a.stroke(poly([(50 + i * 5 - 14, 44), (50 + i * 5 + 14, 90)], closed=False), "#B87318", 0.0)
    for r in range(6):
        for c in range(5):
            x = 38 + c * 6 + (3 if r % 2 else 0)
            y = 49 + r * 7
            if ((x - 50) / 18) ** 2 + ((y - 66) / 23) ** 2 < 0.85:
                a.fill(poly([(x, y - 3), (x + 3, y), (x, y + 3), (x - 3, y)]), "#F6C14A")
                a.fill(circle(x, y, 0.7), "#9A5E12")


@art("peach")
def _peach(a):
    ground(a, 30, 86)
    round_fruit(a, 46, 60, 24, "#F6A35A", 7)
    a.fill(blob(36, 54, 14, 16, 3, 0.1, 9), "#EE6E5A", 0.75)
    a.stroke(smooth([(46, 38), (52, 52), (50, 70), (44, 82)], closed=False), "#D9734A", 1.6, 0.7)
    a.fill(leaf(58, 34, 18, 8, -25), "#5DAA3A")
    a.stroke(poly(rot_pts([(52, 34), (64, 34)], 58, 34, -25), closed=False), "#8ED07A", 0.8)


@art("kiwi")
def _kiwi(a):
    ground(a, 34, 84)
    a.fill(ellipse(36, 58, 20, 17), "#8A6534")
    dots(a, "#6E4E26", 36, 58, 15, 12, 18, 0.5, 3, 0.7)
    a.fill(ellipse_rot(30, 52, 6, 3, -30), "#A98252", 0.7)
    a.fill(ellipse(64, 70, 20, 14), "#8A6534")
    a.fill(ellipse(64, 69, 18.6, 12.6), "#7CB82F")
    a.fill(ellipse(64, 69, 14, 9.4), "#9ACD4A")
    a.fill(ellipse(64, 69, 7, 4.6), "#EEF4D2")
    for i in range(16):
        t = i / 16 * 2 * math.pi
        a.fill(ellipse_rot(64 + math.cos(t) * 9, 69 + math.sin(t) * 6, 1.2, 0.6, math.degrees(t)), "#2A2A1A")


@art("apricot")
def _apricot(a):
    ground(a, 34, 84)
    round_fruit(a, 36, 60, 17, "#F5A031", 9)
    a.fill(blob(30, 56, 9, 10, 2, 0.1, 9), "#EE7A3A", 0.6)
    a.stroke(smooth([(36, 44), (40, 56), (37, 74)], closed=False), "#D07E22", 1.3, 0.7)
    a.fill(ellipse(64, 70, 17, 13), "#E38A22")
    a.fill(ellipse(64, 69, 15.6, 11.6), "#F8B54A")
    a.fill(ellipse(65, 69, 6.4, 5), "#9A5A2A")
    a.fill(ellipse(64, 68, 3, 2), "#B9773F", 0.8)
    a.fill(leaf(46, 40, 13, 6, -30), "#5DAA3A")


@art("avocado")
def _avocado(a):
    ground(a, 34, 86)
    def pear(cx, cy, s):
        return [(cx, cy - 22 * s), (cx + 9 * s, cy - 16 * s), (cx + 15 * s, cy + 2 * s), (cx + 12 * s, cy + 16 * s),
                (cx, cy + 22 * s), (cx - 12 * s, cy + 16 * s), (cx - 15 * s, cy + 2 * s), (cx - 9 * s, cy - 16 * s)]
    a.fill(smooth(rot_pts(pear(34, 58, 1.0), 34, 58, -18)), "#3E5A22")
    a.fill(smooth(rot_pts(pear(32, 54, 0.6), 32, 54, -18)), "#5E7A34", 0.5)
    a.fill(smooth(rot_pts(pear(64, 62, 1.05), 64, 62, 14)), "#2F4A1A")
    a.fill(smooth(rot_pts(pear(64, 62, 0.95), 64, 62, 14)), "#C9DD7A")
    a.fill(smooth(rot_pts(pear(64, 62, 0.8), 64, 62, 14)), "#E6EBA0")
    a.fill(circle(65.5, 67, 8), "#8A5228")
    a.fill(ellipse(63, 64.5, 3, 2), "#B9773F", 0.9)


# ================================================================== vegetables

@art("cucumber")
def _cucumber(a):
    ground(a, 36, 82)
    a.fill(capsule(44, 58, 64, 15, -18), "#2E6E22")
    a.fill(capsule(42, 55, 52, 4, -18), "#5E9A3A", 0.8)
    dots(a, "#7FB85A", 44, 58, 26, 6, 14, 0.6, 5, 0.8)
    for x, y in [(66, 76), (80, 72)]:
        cucumber_slice(a, x, y, 8, 5.6)


@art("tomato")
def _tomato(a):
    ground(a, 34, 86)
    pts = [(38, 34), (52, 32), (60, 38), (64, 54), (58, 72), (40, 78), (22, 72), (14, 54), (20, 38)]
    a.fill(smooth(pts), TOMATO)
    a.fill(smooth([(50, 36), (60, 42), (62, 56), (56, 70), (46, 74), (54, 56)]), "#C9302A", 0.7)
    a.fill(ellipse_rot(26, 46, 4, 7, 30), "#F7897C", 0.8)
    for d in range(0, 360, 72):
        x, y = 38 + math.cos(math.radians(d)) * 5, 35 + math.sin(math.radians(d)) * 2.4
        a.fill(leaf(x, y, 8, 3, d), "#3E8E2E")
    a.fill(capsule(38, 31, 5, 2, 80), "#4E7A2B")
    tomato_slice(a, 70, 74, 14, 9)


@art("lettuce")
def _lettuce(a):
    ground(a, 30, 86)
    for i, (x, y, s, col) in enumerate([(30, 66, 16, "#5FA83A"), (70, 66, 16, "#5FA83A"), (50, 70, 18, "#6DB542"),
                                        (38, 52, 15, "#7DC04B"), (62, 52, 15, "#7DC04B"), (50, 46, 14, "#9AD266"), (50, 60, 14, "#B7E07D")]):
        a.fill(blob(x, y, s, s * 0.82, i + 3, 0.18, 12), col)
        a.stroke(smooth([(x - s * 0.5, y + s * 0.3), (x, y - s * 0.1), (x + s * 0.4, y - s * 0.5)], closed=False), shade(col, 0.4), 1.0, 0.7)


@art("carrot")
def _carrot(a):
    ground(a, 34, 84)
    for i, (x, y, d) in enumerate([(40, 60, 62), (60, 62, 112)]):
        root = rot_pts([(x - 6, y - 22), (x + 6, y - 22), (x + 4, y + 2), (x, y + 24), (x - 4, y + 2)], x, y, d - 90)
        a.fill(smooth(root, tension=0.6), "#F07A1E")
        hl = rot_pts([(x - 3, y - 20), (x, y - 20), (x - 1, y + 10)], x, y, d - 90)
        a.fill(smooth(hl, tension=0.6), "#F9A65A", 0.8)
        for k in (-12, -4, 4):
            p = rot_pts([(x - 4, y + k), (x - 1, y + k + 1)], x, y, d - 90)
            a.stroke(poly(p, closed=False), "#C95A12", 1.0, 0.7)
        tx, ty = rot_pts([(x, y - 23)], x, y, d - 90)[0]
        for dd in (-25, 0, 25):
            lx, ly = rot_pts([(tx, ty - 7)], tx, ty, dd + d - 90)[0]
            a.fill(leaf(lx, ly, 15, 4.4, dd + d - 180), "#4E9A34")


@art("broccoli")
def _broccoli(a):
    ground(a, 30, 88)
    a.fill(smooth([(42, 60), (58, 60), (56, 74), (60, 86), (40, 86), (44, 74)], tension=0.6), "#8CC45A")
    a.fill(smooth([(50, 62), (56, 62), (54, 76), (57, 86), (50, 86)], tension=0.6), "#6FA637", 0.7)
    for i, (x, y, r) in enumerate([(30, 52, 12), (70, 52, 12), (40, 40, 13), (60, 40, 13), (50, 50, 14), (50, 30, 12)]):
        a.fill(blob(x, y, r, r * 0.9, i + 2, 0.15, 11), "#2E7A26")
        dots(a, "#4E9A34", x, y - 1, r * 0.7, r * 0.6, 7, r * 0.18, i + 9)
        dots(a, "#6DBE45", x - 1, y - 2, r * 0.5, r * 0.4, 4, r * 0.1, i + 19)


@art("spinach")
def _spinach(a):
    ground(a, 34, 86)
    for i, (x, y, d, col) in enumerate([(34, 54, -50, "#2E6E22"), (66, 54, 50, "#2E6E22"), (42, 46, -20, "#3E8E2E"),
                                        (58, 46, 20, "#3E8E2E"), (50, 52, 0, "#4E9A34")]):
        a.stroke(poly(rot_pts([(x, y + 12), (x, y + 30)], x, y + 12, d * 0.3), closed=False), "#8CC45A", 2.0)
        a.fill(leaf(x, y, 34, 22, d - 90, 0.15), col)
        a.stroke(poly(rot_pts([(x, y + 14), (x, y - 14)], x, y, d), closed=False), shade(col, 0.4), 1.0, 0.8)


# ================================================================== nuts & spreads

@art("almonds")
def _almonds(a):
    def d(a, x, y, deg, s):
        pts = rot_pts([(x - 5, y), (x - 2, y - 3), (x + 3, y - 2.6), (x + 6, y), (x + 3, y + 2.4), (x - 2, y + 2.6)], x, y, deg)
        a.fill(smooth(pts, tension=0.8), "#A86A38")
        a.fill(ellipse_rot(x - 0.5, y - 0.8, 2.6, 1, deg), "#C98A54", 0.8)
    nut_bowl(a, d, 26)


@art("walnuts")
def _walnuts(a):
    def d(a, x, y, deg, s):
        a.fill(blob(x, y, 5.6, 4.4, s, 0.15, 9), "#B98A52")
        a.stroke(smooth([(x - 4, y - 1), (x - 2, y + 1.5), (x, y - 1), (x + 2, y + 1.5), (x + 4, y - 1)], closed=False), "#7E5528", 1.0, 0.85)
        a.stroke(poly([(x, y - 3.6), (x, y + 3.6)], closed=False), "#7E5528", 0.8, 0.7)
    nut_bowl(a, d, 16)


@art("cashews")
def _cashews(a):
    def d(a, x, y, deg, s):
        pts = []
        for i in range(9):
            t = math.radians(200 + i * 18)
            pts.append((x + math.cos(t) * 5, y + math.sin(t) * 4.4))
        for i in range(9):
            t = math.radians(360 - i * 18)
            pts.append((x + math.cos(t) * 2, y + math.sin(t) * 1.6 - 0.2))
        pts = rot_pts(pts, x, y, deg)
        a.fill(smooth(pts, tension=0.7), "#EED9A8")
        a.fill(ellipse_rot(x, y - 2.4, 2.2, 0.8, deg), "#FFF0CC", 0.8)
    nut_bowl(a, d, 22)


@art("pistachio")
def _pistachio(a):
    def d(a, x, y, deg, s):
        a.fill(ellipse_rot(x, y, 4.6, 3.2, deg), "#E3CFA2")
        a.fill(ellipse_rot(x + 0.4, y - 0.3, 3, 1.8, deg), "#8DB255")
        a.fill(ellipse_rot(x + 0.8, y - 0.2, 1.8, 0.9, deg), "#B9873E", 0.6)
    nut_bowl(a, d, 26)


@art("peanuts")
def _peanuts(a):
    def d(a, x, y, deg, s):
        if s % 3:
            a.fill(capsule(x, y, 11, 5, deg), "#D9B275")
            cx_, cy_ = rot_pts([(x, y)], x, y, deg)[0]
            a.fill(ellipse_rot(cx_, cy_, 1.2, 2.6, deg), "#C29A5E")
            dots(a, "#B88E50", x, y, 4, 1.6, 4, 0.4, s, 0.8)
        else:
            a.fill(ellipse_rot(x, y, 3, 2.2, deg), "#C9864A")
            a.fill(ellipse_rot(x - 0.6, y - 0.6, 1.4, 0.8, deg), "#E7B07A")
    nut_bowl(a, d, 20)


@art("seeds")
def _seeds(a):
    def d(a, x, y, deg, s):
        pts = rot_pts([(x - 3.4, y), (x - 1, y - 1.8), (x + 3, y - 1.2), (x + 3.6, y), (x + 3, y + 1.2), (x - 1, y + 1.8)], x, y, deg)
        a.fill(smooth(pts, tension=0.8), "#2B2B2B" if s % 3 else "#EEE3C8")
        p = rot_pts([(x - 2, y), (x + 2.6, y)], x, y, deg)
        a.stroke(poly(p, closed=False), "#C9C3B5" if s % 3 else "#C9B98F", 0.7, 0.9)
    nut_bowl(a, d, 40)


def spread_jar(a, content, lid, mark, label=WHITE):
    jar(a, content, lid, label=label, top=28, bottom=86, w=46)
    mark(a, 50, 67)


@art("peanut_butter", "addon_peanut_butter")
def _peanut_butter(a):
    def m(a, x, y):
        a.fill(capsule(x, y, 13, 6.4, -20), "#D9B275")
        a.fill(ellipse_rot(x, y, 1.2, 2.8, -20), "#C29A5E")
    spread_jar(a, "#B9773A", "#D33B2F", m)


@art("tahini")
def _tahini(a):
    def m(a, x, y):
        dots(a, "#E3CFA2", x, y, 5, 3, 10, 0.9, 4)
    spread_jar(a, "#D9BF8A", "#2E4E8C", m, label="#F7F1E3")


# ================================================================== sweets

@art("knafeh")
def _knafeh(a):
    cx, cy, rx, ry = plate(a)
    top = [(26, cy - 4), (58, cy - 14), (76, cy - 6), (44, cy + 4)]
    front = [(26, cy - 4), (44, cy + 4), (44, cy + 14), (26, cy + 6)]
    side = [(44, cy + 4), (76, cy - 6), (76, cy + 4), (44, cy + 14)]
    a.fill(poly(side), "#F4EEDC")
    a.fill(poly([(44, cy + 9), (76, cy - 1), (76, cy + 4), (44, cy + 14)]), "#D9541E")
    a.fill(poly(front), "#FBF7EA")
    a.fill(poly([(26, cy + 1), (44, cy + 9), (44, cy + 14), (26, cy + 6)]), "#C94A18")
    for k in range(5):
        x = 46 + k * 6
        a.stroke(smooth([(x, cy + 4 - k * 2), (x + 1, cy + 7 - k * 2), (x - 0.5, cy + 9 - k * 2)], closed=False), "#FFFFFF", 1.0, 0.8)
    a.fill(poly(top), "#E9642A")
    a.fill(poly([(30, cy - 4), (56, cy - 12), (62, cy - 10), (36, cy - 2)]), "#F2874A", 0.7)
    grains(a, "#B9451A", 51, cy - 5, 14, 4, 22, 3, 1.6, 0.7)
    dots(a, PISTACHIO, 51, cy - 5, 14, 4, 16, 0.9, 5)


@art("qatayef")
def _qatayef(a):
    cx, cy, rx, ry = plate(a)
    for i, (x, y, d) in enumerate([(36, cy - 2, -10), (56, cy - 6, 8), (60, cy + 4, -4)]):
        pts = []
        for k in range(11):
            t = math.pi + k / 10 * math.pi
            pts.append((x + math.cos(t) * 12, y + math.sin(t) * 9))
        pts = rot_pts(pts + [(x + 10, y + 1.4), (x, y + 2.4), (x - 10, y + 1.4)], x, y, d)
        a.fill(smooth([(px, py + 1.6) for px, py in pts], tension=0.8), "#C98A3A")
        a.fill(smooth(pts, tension=0.8), "#E3B06A")
        ex = rot_pts([(x - 9, y + 0.6), (x, y - 0.4), (x + 9, y + 0.6), (x, y + 2.2)], x, y, d)
        a.fill(smooth(ex), "#F5ECD8")
        dots(a, PISTACHIO, x, y + 0.6, 6, 0.8, 5, 0.8, i + 3)
        dots(a, "#C99A5A", x, y - 4, 7, 3, 6, 0.5, i + 7, 0.7)
    a.fill(ellipse(48, cy + 1, 22, 6), SYRUP, 0.25)


@art("baklava")
def _baklava(a):
    cx, cy, rx, ry = plate(a)
    for i, (x, y) in enumerate([(36, cy - 4), (52, cy - 7), (66, cy - 2), (44, cy + 4), (60, cy + 6)]):
        top = [(x - 10, y), (x, y - 5), (x + 10, y), (x, y + 5)]
        a.fill(poly([(x - 10, y), (x, y + 5), (x + 10, y), (x + 10, y + 4.5), (x, y + 9.5), (x - 10, y + 4.5)]), "#B8711E")
        for k in (6, 7.8):
            a.stroke(poly([(x - 10, y + k - 3), (x, y + k + 2), (x + 10, y + k - 3)], closed=False), "#F3C46A", 0.8, 0.9)
        a.fill(poly(top), "#E3A33E")
        a.fill(poly([(x - 6, y), (x, y - 3), (x + 2, y - 2), (x - 4, y + 1)]), "#F3C46A", 0.8)
        a.fill(ellipse(x, y, 2.6, 1.4), PISTACHIO)


@art("harissa")
def _harissa(a):
    cx, cy, rx, ry = tray(a, cy=64, rx=44, ry=20)
    for r in range(3):
        for c in range(4):
            x = 50 + (c - 1.5) * 15 + (r - 1) * 2
            y = cy - 7 + r * 7
            if ((x - 50) / 40) ** 2 + ((y - cy) / 17) ** 2 > 1:
                continue
            a.fill(poly([(x - 7, y - 3), (x + 7, y - 3), (x + 8, y + 3), (x - 6, y + 3)]), "#E0A44A")
            a.fill(poly([(x - 6, y + 3), (x + 8, y + 3), (x + 8, y + 5), (x - 6, y + 5)]), "#B9782E")
            a.fill(ellipse(x + 0.5, y, 2.6, 1.4), "#FFF2D6")


@art("awameh")
def _awameh(a):
    cx, cy, rx, ry = plate(a)
    for i, (x, y) in enumerate([(30, cy), (42, cy + 2), (54, cy + 2), (66, cy), (36, cy - 7), (48, cy - 6), (60, cy - 7),
                                (42, cy - 13), (54, cy - 13), (48, cy - 19)]):
        a.fill(circle(x, y + 0.8, 6.4), "#A8641E")
        a.fill(circle(x, y, 6.2), "#E09A34")
        a.fill(ellipse(x - 1.8, y - 2, 2.4, 1.6), "#F9D27A")
        dots(a, "#C47A22", x + 1, y + 1, 3, 3, 3, 0.5, i + 3, 0.6)


@art("maamoul")
def _maamoul(a):
    cx, cy, rx, ry = plate(a)
    for i, (x, y) in enumerate([(36, cy - 2), (52, cy - 6), (64, cy + 1), (46, cy + 5)]):
        a.fill(ellipse(x, y + 1.6, 9.6, 5.4), "#C99A5A")
        a.fill(smooth([(x - 9.6, y), (x - 6, y - 6), (x, y - 7.6), (x + 6, y - 6), (x + 9.6, y), (x, y + 4.6)]), "#EBC98E")
        for k in range(-3, 4):
            a.stroke(poly([(x + k * 2.4, y - 6.6 + abs(k) * 0.6), (x + k * 2.8, y + 1)], closed=False), "#C9A26A", 0.8, 0.8)
        dots(a, WHITE, x, y - 3, 7, 3, 12, 0.5, i + 3, 0.9)


@art("um_ali")
def _um_ali(a):
    ground(a, 36, 86)
    a.fill(ellipse(50, 66, 42, 20), "#D9D3C8")
    a.fill(ellipse(50, 63, 42, 20), "#FFFFFF")
    a.fill(ellipse(50, 63, 37, 16.5), "#E8E2D6")
    a.fill(blob(50, 62, 35, 15, 3, 0.04, 14), "#E3B06A")
    a.fill(blob(44, 58, 16, 6, 5, 0.2, 9), "#F2CF92", 0.8)
    g = rnd(4)
    for _ in range(9):
        x, y = 50 + g.uniform(-28, 28), 62 + g.uniform(-10, 10)
        if ((x - 50) / 32) ** 2 + ((y - 62) / 13) ** 2 < 1:
            a.fill(blob(x, y, 3.4, 2.2, g.randint(0, 99), 0.3, 6), "#B9782E", 0.85)
    nuts_topping(a, 50, 62, 26, 9, 6)
    dots(a, "#6E2E3A", 50, 62, 26, 9, 8, 0.8, 9)
    grains(a, "#FFFFFF", 50, 62, 26, 9, 18, 11, 2.2, 0.7)


@art("muhallabia")
def _muhallabia(a):
    ground(a, 22, 88)
    a.fill(poly([(42, 84), (58, 84), (54, 78), (46, 78)]), GLASS, 0.9)
    a.fill(rrect(47, 66, 6, 13, 2), GLASS, 0.9)
    a.fill(smooth([(22, 38), (78, 38), (74, 54), (62, 66), (50, 68), (38, 66), (26, 54)], tension=0.8), GLASS, 0.55)
    a.fill(smooth([(24, 42), (76, 42), (72, 55), (61, 64), (50, 66), (39, 64), (28, 55)], tension=0.8), "#FBF8EF")
    a.fill(ellipse(50, 41, 26, 6), "#FFFDF7")
    a.fill(ellipse(50, 41, 18, 3.6), "#F2B8C6", 0.55)
    dots(a, PISTACHIO, 50, 41, 17, 3.6, 18, 0.9, 4)
    a.stroke(ellipse(50, 38, 28, 6), shade(GLASS, -0.2), 1)
    a.fill(poly([(26, 42), (30, 42), (34, 58), (31, 58)]), WHITE, 0.5)


@art("rice_pudding")
def _rice_pudding(a):
    cx, cy, rx, ry = small_bowl(a, "#FBF6EA", rim_y=54, rx=30, ry=9, depth=26, body="#C9773F")
    grains(a, "#FFFFFF", cx, cy, rx * 0.7, ry * 0.6, 22, 3, 2.4, 1.0)
    swirl(a, cx, cy, rx * 0.5, ry * 0.5, "#B9783A", 1.4, 0.7)
    dots(a, "#A86A2E", cx, cy, rx * 0.6, ry * 0.5, 18, 0.45, 6, 0.8)


@art("cake")
def _cake(a):
    ground(a, 34, 86)
    tip_t, out_t = (16, 54), (76, 62)
    h = 22
    def band(d1, d2, col):
        a.fill(poly([(tip_t[0], tip_t[1] + d1), (out_t[0], out_t[1] + d1), (out_t[0], out_t[1] + d2), (tip_t[0], tip_t[1] + d2)]), col)
    band(0, h, "#E9B865")
    band(6, 9, "#FBF1E0")
    band(14, 17, "#FBF1E0")
    band(9, 10.2, "#D9465A")
    a.fill(poly([(out_t[0], out_t[1]), (86, 42), (86, 42 + h), (out_t[0], out_t[1] + h)]), "#F2DFC2")
    a.fill(poly([tip_t, (64, 30), (86, 42), out_t]), "#FBF4E6")
    a.fill(poly([(64, 30), (86, 42), (84, 44), (63, 32.5)]), "#FFFFFF")
    a.fill(poly([tip_t, out_t, (out_t[0], out_t[1] + 2.4), (tip_t[0], tip_t[1] + 2.4)]), "#FFFAF0")
    for i, (x, y) in enumerate([(56, 42), (68, 46), (78, 46)]):
        a.fill(blob(x, y + 1, 4.4, 3, i + 3, 0.2, 8), "#E9DCC6")
        a.fill(blob(x, y, 4.4, 3, i + 3, 0.2, 8), "#FFFFFF")
    strawberry(a, 66, 36, 0.62, 10, 4)


@art("pancakes")
def _pancakes(a):
    cx, cy, rx, ry = plate(a)
    base = cy + 2
    for i in range(4):
        y = base - i * 6
        a.fill(ellipse(50, y + 2.5, 26, 9), "#B8711E")
        a.fill(rrect(24, y - 2, 52, 4.5, 2), "#C98A3A")
        a.fill(ellipse(50, y - 2, 26, 9), "#E6A54E")
    top = base - 3 * 6 - 2
    a.fill(blob(50, top, 22, 7.4, 3, 0.1, 12), SYRUP)
    for x, l in [(30, 8), (44, 14), (62, 10), (72, 6)]:
        a.fill(capsule(x, top + l / 2 + 4, l, 3, 90), SYRUP)
    a.fill(blob(48, top - 0.6, 12, 3.6, 4, 0.15, 9), "#F2BE5A", 0.8)
    a.fill(rrect(44, top - 6, 10, 5, 1.4), "#FBE6A0")
    a.fill(rrect(44, top - 6, 10, 2, 1), "#FFF4CF")


@art("crepe")
def _crepe(a):
    cx, cy, rx, ry = plate(a)
    pts = [(24, cy + 2), (42, cy - 14), (72, cy - 10), (76, cy + 4), (50, cy + 8)]
    a.fill(smooth([(x, y + 1.8) for x, y in pts], tension=0.6), "#C98A3A")
    a.fill(smooth(pts, tension=0.6), "#EAC07A")
    a.fill(smooth([(26, cy + 1), (40, cy - 10), (52, cy - 6), (38, cy + 4)], tension=0.6), "#F5D9A0", 0.8)
    dots(a, "#C08A42", 52, cy - 3, 18, 6, 10, 0.8, 3, 0.6)
    a.stroke(smooth([(30, cy - 2), (40, cy - 8), (48, cy - 2), (58, cy - 8), (66, cy - 3), (72, cy - 6)], closed=False), CHOC, 1.6)
    for x, y in [(42, cy - 4), (58, cy - 2)]:
        a.fill(ellipse(x, y, 3.6, 2.4), "#FBF1C8")
        a.fill(ellipse(x, y, 1.2, 0.8), "#D9C08A")
    strawberry(a, 74, cy - 8, 0.5, 20, 3)


@art("waffle")
def _waffle(a):
    ground(a, 34, 84)
    a.fill(rrect(18, 44, 64, 36, 9), "#B8711E")
    a.fill(rrect(18, 41, 64, 36, 9), "#E3A33E")
    for r in range(4):
        for c in range(7):
            a.fill(rrect(22 + c * 8.4, 45 + r * 7.6, 6.2, 5.4, 1.2), "#C47F22")
            a.fill(rrect(22 + c * 8.4, 45 + r * 7.6, 6.2, 2, 1), "#A8661A", 0.7)
    a.fill(blob(44, 54, 16, 7, 3, 0.18, 10), SYRUP, 0.8)
    strawberry(a, 64, 46, 0.6, 15, 2)
    strawberry(a, 72, 52, 0.55, -10, 6)


@art("donut")
def _donut(a):
    ground(a, 34, 82)
    a.fill(ellipse(50, 62, 36, 18) + ellipse(50, 60, 10, 4.6), "#C98A3A", evenodd=True)
    a.fill(ellipse(50, 59, 36, 18) + ellipse(50, 59, 10, 4.6), "#E3A855", evenodd=True)
    g = rnd(3)
    pts = []
    for i in range(24):
        t = 2 * math.pi * i / 24
        k = 0.9 + (0.06 if i % 3 == 0 else 0) + g.uniform(-0.02, 0.02)
        pts.append((50 + math.cos(t) * 34 * k, 57.5 + math.sin(t) * 16.5 * k + (2 if i % 4 == 1 and math.sin(t) > 0 else 0)))
    a.fill(smooth(pts) + ellipse(50, 58, 12.6, 6), "#F2A3C0", evenodd=True)
    a.fill(ellipse(38, 52, 10, 3.6), "#F8C9DA", 0.8)
    for _ in range(26):
        t = g.uniform(0, 2 * math.pi)
        k = g.uniform(0.5, 0.85)
        x, y = 50 + math.cos(t) * 33 * k, 57.5 + math.sin(t) * 16 * k
        a.fill(capsule(x, y, 3.2, 1.2, g.uniform(0, 180)), g.choice(["#FFFFFF", "#5BC0EB", "#FDE74C", "#9BC53D", "#E55934"]))


@art("chocolate")
def _chocolate(a):
    ground(a, 30, 88)
    a.fill(rrect(26, 20, 44, 66, 4), "#4A2414")
    for r in range(3):
        for c in range(3):
            x, y = 29 + c * 13.6, 23 + r * 13
            a.fill(rrect(x, y, 11.6, 11, 1.6), "#6B3A22")
            a.fill(rrect(x + 1.6, y + 1.4, 8.4, 7.8, 1.2), "#7E4A2E")
            a.fill(rrect(x + 1.6, y + 1.4, 8.4, 2, 1), "#9A6040", 0.8)
    a.fill(poly([(22, 58), (74, 52), (74, 88), (22, 88)]), "#C0392B")
    a.fill(poly([(22, 58), (74, 52), (74, 56), (22, 62)]), "#D9D9DE")
    a.fill(poly([(56, 54), (74, 52), (74, 88), (60, 88)]), "#A02E22")
    a.fill(rrect(32, 68, 30, 8, 3), "#F6D33C")


@art("ice_cream")
def _ice_cream(a):
    ground(a, 16, 92)
    L = lambda t: (36 + 14 * t, 54 + 36 * t)
    Rr = lambda t: (64 - 14 * t, 54 + 36 * t)
    a.fill(poly([L(0), Rr(0), (50, 90)]), "#E3A855")
    a.fill(poly([(50, 54), Rr(0), (50, 90)]), "#C98A3A", 0.5)
    for i in range(6):
        t = i / 6
        a.stroke(poly([L(t), Rr(min(1, t + 0.32))], closed=False), "#B8711E", 0.9, 0.8)
        a.stroke(poly([Rr(t), L(min(1, t + 0.32))], closed=False), "#B8711E", 0.9, 0.8)
    a.fill(smooth([(32, 54), (36, 46), (46, 42), (58, 42), (66, 46), (68, 54), (62, 58), (56, 55), (50, 59), (42, 55), (36, 58)]), "#F4A6B8")
    a.fill(smooth([(36, 40), (42, 28), (52, 24), (62, 30), (64, 42), (56, 44), (48, 41), (40, 44)]), "#7A4A2E")
    a.fill(ellipse_rot(46, 32, 4, 2.4, -30), "#9C6A4A", 0.8)
    a.fill(ellipse_rot(40, 48, 4, 2.2, -20), "#F9CAD6", 0.9)
    a.fill(circle(52, 20, 4), "#D9262F")
    a.fill(ellipse(51, 18.6, 1.2, 0.8), "#F27A6E")


@art("biscuits")
def _biscuits(a):
    ground(a, 34, 84)
    for i in range(3):
        y = 72 - i * 5
        a.fill(ellipse(40, y + 2, 22, 8), "#B9782E")
        a.fill(ellipse(40, y, 22, 8), "#E3B064")
    dots(a, "#C08A42", 40, 62, 14, 4.6, 8, 0.8, 3)
    a.fill(ellipse(66, 70, 18, 15), "#B9782E")
    a.fill(ellipse(66, 68, 18, 15), "#EBC07A")
    a.stroke(ellipse(66, 68, 14, 11.6), "#D9A458", 1.2)
    for k in range(8):
        t = k / 8 * 2 * math.pi
        a.fill(circle(66 + math.cos(t) * 8, 68 + math.sin(t) * 6.4, 0.9), "#C08A42")
    a.fill(circle(66, 68, 0.9), "#C08A42")


# ================================================================== drinks

@art("tea")
def _tea(a):
    saucer_at(a, 50, 82, 26, 6)
    body = [(36, 36), (64, 36), (60, 50), (58, 58), (62, 72), (60, 82), (40, 82), (38, 72), (42, 58), (40, 50)]
    a.fill(smooth(body, tension=0.7), GLASS, 0.5)
    liq = [(37.5, 42), (62.5, 42), (59.5, 50), (57.6, 58), (61.4, 72), (59.4, 80.6), (40.6, 80.6), (38.6, 72), (42.4, 58), (40.5, 50)]
    a.fill(smooth(liq, tension=0.7), "#B3401C", 0.92)
    a.fill(smooth([(52, 44), (61, 44), (58, 56), (60, 72), (58, 79), (53, 79)], tension=0.7), "#8E2E12", 0.5)
    a.fill(ellipse(50, 42, 12.4, 2.6), "#C9582A")
    a.fill(poly([(39, 40), (42, 40), (42, 54), (40, 54)]), WHITE, 0.45)
    a.stroke(ellipse(50, 36, 14, 2.8), shade(GLASS, -0.25), 1)
    a.fill(rrect(36, 60, 28, 2, 1), "#D4A443", 0.9)
    mint_sprig(a, 58, 40, 0.8, 25)


@art("coffee_arabic")
def _coffee_arabic(a):
    ground(a, 34, 88)
    gold, dark = "#D4A443", "#A87A22"
    a.stroke(smooth([(42, 52), (30, 46), (22, 36), (14, 30)], closed=False), dark, 5)
    a.stroke(smooth([(42, 52), (30, 46), (22, 36), (14, 30)], closed=False), gold, 3)
    a.fill(poly([(10, 26), (18, 30), (14, 33)]), gold)
    a.stroke(smooth([(56, 40), (68, 46), (68, 64), (58, 74)], closed=False), dark, 3)
    body = [(40, 86), (36, 74), (38, 62), (44, 54), (46, 44), (54, 44), (56, 54), (62, 62), (64, 74), (60, 86)]
    a.fill(smooth(body, tension=0.7), gold)
    a.fill(smooth([(52, 46), (56, 54), (62, 62), (63, 74), (59, 85), (54, 85), (58, 72), (55, 58)], tension=0.7), dark, 0.6)
    a.fill(rrect(38, 84, 24, 4, 1.5), dark)
    a.fill(ellipse(50, 44, 7, 2), dark)
    a.fill(smooth([(43, 44), (44, 36), (50, 32), (56, 36), (57, 44)]), gold)
    a.fill(circle(50, 28, 2.2), gold)
    a.fill(poly([(48.6, 28), (50, 20), (51.4, 28)]), dark)
    a.fill(ellipse_rot(42, 66, 2.4, 7, 15), "#F2D58A", 0.7)
    # finjan
    cx_ = 76
    ground(a, 10, 89)
    a.fill(smooth([(68, 76), (84, 76), (82, 86), (70, 86)], tension=0.4), WHITE)
    a.fill(rrect(68.4, 79, 15.2, 1.8, 0.8), gold)
    a.fill(ellipse(76, 76, 8, 2.2), "#E3E5EA")
    a.fill(ellipse(76, 76.4, 6.8, 1.6), "#B9853A")


def espresso_cup(a, cx, top, w, h, surface, crema=None, art_heart=False, saucer_rx=None):
    saucer_at(a, cx, top + h + 1, saucer_rx or w * 0.85)
    ex = cup(a, cx, top, w, h, WHITE, surface)
    if crema:
        a.fill(ellipse(ex[0], ex[1], ex[2] * 0.7, ex[3] * 0.6), crema, 0.8)
    if art_heart:
        x, y = ex[0], ex[1]
        a.fill(smooth([(x, y + 2.6), (x - 6, y - 0.6), (x - 3.6, y - 2.6), (x, y - 1), (x + 3.6, y - 2.6), (x + 6, y - 0.6)], tension=0.8), WHITE, 0.95)


@art("coffee_turkish")
def _coffee_turkish(a):
    espresso_cup(a, 46, 48, 30, 30, "#7A4A2A", "#A06A3E")
    dots(a, "#C99A6A", 46, 48.6, 9, 2, 8, 0.6, 3)


@art("espresso")
def _espresso(a):
    espresso_cup(a, 46, 52, 26, 26, "#5A2E16", "#C6844A")


@art("cappuccino")
def _cappuccino(a):
    espresso_cup(a, 46, 46, 42, 32, "#B9844E", None, True, 32)
    a.fill(ellipse(46, 46.6, 16, 4.2), "#E9D6BC", 0.0)


@art("latte")
def _latte(a):
    ground(a, 18, 90)
    top, bottom, wt, wb = 18, 88, 30, 24
    a.fill(poly([(50 - wt / 2, top), (50 + wt / 2, top), (50 + wb / 2, bottom), (50 - wb / 2, bottom)]), GLASS, 0.5)
    def band(y0, y1, col):
        k0, k1 = (y0 - top) / (bottom - top), (y1 - top) / (bottom - top)
        w0, w1 = wt + (wb - wt) * k0, wt + (wb - wt) * k1
        a.fill(poly([(50 - w0 / 2 + 1, y0), (50 + w0 / 2 - 1, y0), (50 + w1 / 2 - 1, y1), (50 - w1 / 2 + 1, y1)]), col)
    band(60, 87, "#FBF6EC")
    band(42, 60, "#B47A44")
    band(56, 62, "#D9B48A")
    band(28, 42, "#FFFFFF")
    a.fill(ellipse(50, 28, 13.6, 2.6), "#FFFFFF")
    dots(a, "#B47A44", 50, 28, 8, 1.6, 6, 0.6, 3)
    a.fill(poly([(37.5, 22), (40.5, 22), (42, 82), (39.6, 82)]), WHITE, 0.45)
    a.stroke(ellipse(50, top, wt / 2, 2.8), shade(GLASS, -0.22), 1)
    a.stroke(poly([(56, 30), (64, 8)], closed=False), "#3A7BD5", 3)


@art("orange_juice")
def _orange_juice(a):
    ly, ll, lr = glass(a, 48, "#F6A21E", level=0.82)
    a.stroke(poly([(54, ly + 6), (62, 12)], closed=False), "#E2483B", 2.6)
    a.fill(wedge(64, 26, 11, 180, 360, 11), "#E4880F")
    a.fill(wedge(64, 26, 9.6, 180, 360, 9.6), "#FBE3A4")
    for i in range(5):
        a.fill(wedge(64, 25.4, 8.4, 182 + i * 36, 214 + i * 36, 8.4), "#F8B33E")


@art("lemon_mint")
def _lemon_mint(a):
    ly, ll, lr = glass(a, 48, "#B9DB6A", level=0.84, alpha=0.95)
    leaves_pos = [(42, ly + 12), (54, ly + 22), (46, ly + 30), (52, ly + 8)]
    for i, (x, y) in enumerate(leaves_pos):
        a.fill(leaf(x, y, 8, 4, i * 50), "#4E9A34", 0.9)
    for x, y in [(44, ly + 4), (54, ly + 16)]:
        a.fill(rrect(x - 4, y - 3.6, 8, 7.2, 1.6), WHITE, 0.45)
    mint_sprig(a, 44, ly - 1, 0.9, -15)
    a.fill(wedge(66, 30, 10, 180, 360, 10), "#E8C51E")
    a.fill(wedge(66, 30, 8.6, 180, 360, 8.6), "#FBF2B0")
    for i in range(5):
        a.fill(wedge(66, 29.4, 7.4, 182 + i * 36, 214 + i * 36, 7.4), "#F6E15A")


def drink_can(a, color, accent, mark):
    can_(a, color, None, top=16, bottom=88, w=36)
    mark(a)


@art("soda")
def _soda(a):
    def m(a):
        a.fill(smooth([(32, 54), (42, 46), (56, 56), (68, 48), (68, 58), (56, 66), (42, 56), (32, 64)], tension=0.6), WHITE, 0.95)
        dots(a, "#FFFFFF", 44, 32, 6, 6, 5, 0.9, 3, 0.6)
    drink_can(a, "#D52B2B", None, m)


@art("energy_drink")
def _energy_drink(a):
    def m(a):
        a.fill(poly([(53, 30), (40, 56), (49, 56), (45, 76), (60, 46), (51, 46), (56, 30)]), "#F6D33C")
        a.fill(rrect(32, 80, 36, 3, 1), "#5BC0EB")
    drink_can(a, "#1E2A4A", None, m)


@art("sahlab")
def _sahlab(a):
    ex = mug(a, "#E9E2D5", "#FBF7EE", cx=46, top=36, w=42, h=48)
    swirl(a, ex[0], ex[1], ex[2] * 0.6, ex[3] * 0.6, "#B9783A", 1.2, 0.8)
    dots(a, "#A86A2E", ex[0], ex[1], ex[2] * 0.7, ex[3] * 0.6, 14, 0.4, 3)
    dots(a, PISTACHIO, ex[0], ex[1], ex[2] * 0.6, ex[3] * 0.5, 6, 0.8, 5)
    grains(a, "#FFFFFF", ex[0], ex[1], ex[2] * 0.6, ex[3] * 0.5, 6, 7, 2.2, 0.7)
    for k in range(3):
        a.stroke(smooth([(40 + k * 6, 30), (38 + k * 6, 24), (41 + k * 6, 18)], closed=False), "#C9CED6", 1.6, 0.6)


# ================================================================== supplements

def supp_tub(a, body, lid, label, accent, top=24, w=52, scoop_powder=None):
    tub(a, body, lid, label, top=top, bottom=86, w=w)
    a.fill(rrect(50 - w / 2 + 4, top + 22, w - 8, 10, 3), accent)
    a.fill(rrect(50 - w / 2 + 8, top + 36, w - 16, 3, 1.5), accent, 0.6)
    a.fill(rrect(50 - w / 2 + 8, top + 41, w * 0.4, 3, 1.5), accent, 0.4)
    if scoop_powder:
        ground(a, 12, 90, 0.1)
        a.fill(capsule(76, 86, 18, 4, -20), "#E9EBF0")
        a.fill(ellipse(70, 84, 9, 4.4), "#D9DDE3")
        a.fill(ellipse(70, 82, 8, 3.6), scoop_powder)
        a.fill(mound(70, 82, 15, 5, 3, 5, 0.1, 1), scoop_powder)


@art("whey", "addon_whey")
def _whey(a):
    supp_tub(a, "#22262E", "#22262E", "#FFFFFF", "#E2483B", scoop_powder="#C9A27A")


@art("creatine")
def _creatine(a):
    supp_tub(a, "#F4F6F9", "#2F7BC9", "#2F7BC9", "#FFFFFF", top=36, w=44, scoop_powder="#FFFFFF")


@art("mass_gainer")
def _mass_gainer(a):
    ground(a, 32, 88)
    bag = [(20, 30), (80, 30), (82, 60), (78, 86), (22, 86), (18, 60)]
    a.fill(smooth(bag, tension=0.3), "#3A3D44")
    a.fill(smooth([(56, 32), (80, 30), (82, 60), (78, 86), (60, 86), (64, 58)], tension=0.3), "#2A2D33", 0.8)
    a.fill(rrect(20, 24, 60, 8, 2), "#2A2D33")
    for x in range(22, 80, 4):
        a.stroke(poly([(x, 25), (x, 31)], closed=False), "#4A4E56", 0.7)
    a.fill(rrect(26, 44, 48, 20, 3), "#F6B92E")
    a.fill(rrect(30, 48, 30, 4, 1.5), "#22262E")
    a.fill(rrect(30, 55, 20, 3, 1.5), "#22262E", 0.6)
    a.fill(rrect(26, 70, 48, 6, 2), "#E2483B")


@art("protein_bar")
def _protein_bar(a):
    ground(a, 36, 82)
    pts = rot_pts([(16, 52), (84, 52), (84, 70), (16, 70)], 50, 61, -12)
    a.fill(poly(pts), "#2F7BC9")
    for x in (16, 84):
        e = rot_pts([(x, 52), (x + (3 if x > 50 else -3), 51), (x + (3 if x > 50 else -3), 71), (x, 70)], 50, 61, -12)
        a.fill(poly(e), "#245FA0")
    a.fill(poly(rot_pts([(30, 56), (70, 56), (70, 66), (30, 66)], 50, 61, -12)), "#FFFFFF")
    a.fill(poly(rot_pts([(34, 59), (56, 59), (56, 63), (34, 63)], 50, 61, -12)), "#2F7BC9", 0.8)
    a.fill(rrect(30, 72, 30, 12, 3), "#5A3320")
    a.fill(rrect(30, 72, 30, 4, 2), "#7A4A30")
    dots(a, "#E9D3A8", 45, 79, 12, 3, 8, 0.7, 4)


# ================================================================== basics

@art("olive_oil", "addon_olive_oil")
def _olive_oil(a):
    bottle(a, "#B9A21E", "#2E5E22", top=10, bottom=88, w=30, neck=9, label="#F7F1E3", alpha=0.95)
    a.fill(ellipse(50, 66, 7, 5), "#5E7A2A")
    a.fill(leaf(56, 62, 8, 3.4, -30), "#4E8A2E")
    for x, y in [(74, 84), (82, 82)]:
        a.fill(ellipse(x, y, 4, 3), "#6B7A2A")
        a.fill(ellipse(x - 1, y - 1, 1.2, 0.8), "#A6B15A")


@art("ghee")
def _ghee(a):
    jar(a, "#F2C53A", "#C9302A", label="#FFF4D0", top=30, bottom=86, w=48, label_color="#E2A32E")


@art("honey", "addon_honey")
def _honey(a):
    jar(a, "#E9A126", "#C47A22", label="#FFF4D0", top=32, bottom=86, w=44)
    a.fill(poly([(42, 70), (50, 64), (58, 70), (58, 78), (50, 82), (42, 78)]), "#E9A126", 0.9)
    a.stroke(poly([(56, 34), (72, 14)], closed=False), "#B9782E", 2.6)
    for k in range(3):
        a.fill(ellipse_rot(74 + k * 1.6, 12 - k * 2, 4.4, 2.6, -50), "#C98A3A" if k % 2 else "#E0A44A")
    a.fill(capsule(56, 40, 4, 3.4, 90), "#E9A126")


@art("sugar", "addon_sugar")
def _sugar(a):
    cx, cy, rx, ry = small_bowl(a, "#FFFFFF", rim_y=58, rx=28, ry=8.4, depth=22, body="#8DB8E0")
    a.fill(mound(cx, cy + 1, rx * 1.6, 7, 3, 7, 0.1, 1), "#FBFBFB")
    dots(a, "#E3E6EB", cx, cy - 2, rx * 0.6, ry * 0.5, 22, 0.4, 3)
    for i, (x, y) in enumerate([(70, 80), (80, 76)]):
        a.fill(poly([(x - 5, y - 3), (x + 1, y - 6), (x + 7, y - 3), (x + 7, y + 3), (x + 1, y + 6), (x - 5, y + 3)]), "#E9ECF0")
        a.fill(poly([(x - 5, y - 3), (x + 1, y - 6), (x + 7, y - 3), (x + 1, y)]), WHITE)


@art("jam", "addon_jam")
def _jam(a):
    jar(a, "#C2182E", "#F4F4F4", label="#FFFFFF", top=32, bottom=86, w=44)
    for r in range(2):
        for c in range(5):
            if (r + c) % 2 == 0:
                a.fill(rrect(30 + c * 8, 32 + r * 5.4, 8, 5.4, 0.6), "#D33B2F")
    strawberry(a, 50, 70, 0.55, 0, 3)


def squeeze(a, body, cap, nozzle):
    ground(a, 22, 88)
    a.fill(poly([(46, 12), (54, 12), (52, 4), (48, 4)]), nozzle)
    a.fill(rrect(38, 12, 24, 12, 4), cap)
    a.fill(smooth([(36, 24), (64, 24), (68, 40), (68, 80), (62, 88), (38, 88), (32, 80), (32, 40)], tension=0.4), body)
    a.fill(smooth([(54, 24), (64, 24), (68, 40), (68, 80), (62, 88), (56, 88), (60, 60)], tension=0.4), shade(body, -0.12))
    a.fill(rrect(36, 30, 4, 44, 2), WHITE, 0.4)
    a.fill(rrect(36, 50, 28, 18, 3), WHITE, 0.9)


@art("mayo", "addon_mayo")
def _mayo(a):
    squeeze(a, "#F3E6B4", "#2F6FC2", "#2F6FC2")
    a.fill(ellipse(50, 59, 7, 4.4), "#F6E7A6")


@art("ketchup", "addon_ketchup")
def _ketchup(a):
    squeeze(a, "#C9201E", "#F4F4F4", "#F4F4F4")
    tomato_slice(a, 50, 59, 6, 4.4)


@art("egg_white")
def _egg_white(a):
    cx, cy, rx, ry = small_bowl(a, "#F6F0DA", rim_y=54, rx=30, ry=9, depth=26, body="#8DB8E0")
    a.fill(blob(cx, cy, rx * 0.8, ry * 0.75, 3, 0.1, 10), "#FFFFFF", 0.8)
    a.fill(ellipse(cx - 6, cy - 1.6, 6, 1.6), WHITE)
    for x, y, d in [(74, 80, -20), (84, 76, 160)]:
        pts = []
        for k in range(9):
            t = math.radians(k * 22.5)
            pts.append((x + math.cos(t) * 7, y + math.sin(t) * 6))
        pts += [(x - 7, y), (x - 5, y - 1.6), (x - 3, y + 0.6), (x - 1, y - 1.4), (x + 1, y + 0.4), (x + 3, y - 1.6), (x + 5, y + 0.4), (x + 7, y)]
        a.fill(smooth(rot_pts(pts, x, y, d), tension=0.4), "#F1DEC2")


def legume_bowl(a, draw, n, surface):
    cx, cy, rx, ry = bowl(a, rim_y=52, rx=33, ry=10, depth=28, surface=surface)
    heap(a, cx, cy - 1, rx * 0.8, ry * 0.7, n, 4, draw)
    leaves(a, PARSLEY, cx, cy - 2, rx * 0.4, ry * 0.3, 4, 6, 1.2)


@art("lentils")
def _lentils(a):
    def d(a, x, y, deg, s):
        a.fill(ellipse_rot(x, y, 1.9, 1.3, deg), "#8A5A2E" if s % 2 else "#A06C3A")
        a.fill(ellipse_rot(x - 0.4, y - 0.4, 0.8, 0.4, deg), "#C99A6A", 0.8)
    legume_bowl(a, d, 110, "#7A4E28")


@art("chickpeas")
def _chickpeas(a):
    def d(a, x, y, deg, s):
        a.fill(blob(x, y, 3, 2.7, s, 0.08, 7), "#E3B66A")
        a.fill(ellipse(x - 0.8, y - 0.9, 1.2, 0.8), "#F7DCA0")
        a.fill(circle(x + 1.4, y - 1.6, 0.5), "#B9873E", 0.8)
    legume_bowl(a, d, 46, "#D9A85A")


@art("white_beans")
def _white_beans(a):
    def d(a, x, y, deg, s):
        a.fill(capsule(x, y, 6.4, 3.4, deg), "#F2EAD8")
        a.fill(capsule(x - 0.4, y - 0.5, 4, 1.2, deg), "#FFFFFF", 0.8)
    legume_bowl(a, d, 44, "#E3D6BC")
