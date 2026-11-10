"""
Food pictures, part 2: main dishes, grill, fast food, soups and salads.
Imported by gen_food_art.py (which registers itself as `gen_food_art` first, so R is shared).
"""
import math

from gen_food_art import *  # noqa: F401,F403  (palette, containers, garnish, R, art)
from foodart import (blob, capsule, circle, ellipse, ellipse_rot, mound, poly, rot_pts, rrect, shade, smooth,
                     wedge)


# ------------------------------------------------------------------ extra building blocks

def leaf(cx, cy, length, width, deg=0, curve=0.0):
    L, W = length / 2, width / 2
    pts = [(-L, 0), (-L * 0.45, -W * 0.95), (L * 0.1, -W), (L * 0.6, -W * 0.6), (L, 0),
           (L * 0.6, W * 0.55), (L * 0.1, W * 0.9), (-L * 0.45, W * 0.85)]
    pts = [(cx + x, cy + y + curve * (x / L) ** 2 * W) for x, y in pts]
    return smooth(rot_pts(pts, cx, cy, deg), tension=0.8)


def grill_marks(a, cx, cy, w, h, deg=0, color="#4A2414", n=3, width=1.6, alpha=0.7):
    for i in range(n):
        off = (i - (n - 1) / 2) * h / n
        p = rot_pts([(cx - w / 2, cy + off - w * 0.12), (cx + w / 2, cy + off + w * 0.12)], cx, cy, deg)
        a.stroke(poly(p, closed=False), color, width, alpha)


def skewer(a, x0, y0, x1, y1):
    a.stroke(poly([(x0, y0), (x1, y1)], closed=False), "#A3A9B1", 1.8)
    a.stroke(ellipse(x1, y1, 2.6, 2.6), "#7D838C", 1.4)


def onion_curls(a, cx, cy, rx, ry, n, seed, color="#8A4B1E", width=1.2):
    g = rnd(seed)
    for _ in range(n):
        t = g.uniform(0, 2 * math.pi)
        k = math.sqrt(g.uniform(0, 1))
        x, y = cx + math.cos(t) * rx * k, cy + math.sin(t) * ry * k
        r = g.uniform(1.4, 2.6)
        a0 = g.uniform(0, 6.28)
        pts = [(x + math.cos(a0 + u) * r, y + math.sin(a0 + u) * r * 0.7) for u in (0, 0.8, 1.6, 2.4)]
        a.stroke(smooth(pts, closed=False), g.choice([color, shade(color, 0.25)]), width, 0.95)


def fish(a, cx, cy, length, body, belly, deg=0, marks=None):
    L, H = length / 2, length * 0.2
    shape = [(-L, 0.5), (-L * 0.75, -H * 0.75), (-L * 0.2, -H), (L * 0.35, -H * 0.8), (L * 0.62, -H * 0.3),
             (L * 0.68, 0), (L * 0.62, H * 0.3), (L * 0.3, H * 0.8), (-L * 0.2, H * 0.95), (-L * 0.75, H * 0.7)]
    tail = [(L * 0.55, 0), (L * 1.02, -H * 0.95), (L * 0.88, 0), (L * 1.02, H * 0.95)]
    fin = [(-L * 0.15, -H * 0.9), (L * 0.05, -H * 1.45), (L * 0.35, -H * 0.75)]
    R_ = lambda pts: rot_pts([(cx + x, cy + y) for x, y in pts], cx, cy, deg)
    a.fill(smooth(R_(fin), tension=0.4), shade(body, -0.15))
    a.fill(smooth(R_(tail), tension=0.35), shade(body, -0.12))
    a.fill(smooth(R_([(x, y + 1.6) for x, y in shape])), shade(body, -0.25))
    a.fill(smooth(R_(shape)), body)
    a.fill(smooth(R_([(-L * 0.7, H * 0.35), (-L * 0.2, H * 0.55), (L * 0.4, H * 0.45), (-L * 0.1, H * 0.8)])), belly, 0.75)
    a.stroke(smooth(R_([(-L * 0.48, -H * 0.62), (-L * 0.42, 0), (-L * 0.5, H * 0.6)]), closed=False), shade(body, -0.3), 1.1, 0.7)
    ex, ey = R_([(-L * 0.72, -H * 0.15)])[0]
    a.fill(circle(ex, ey, 1.9), WHITE)
    a.fill(circle(ex, ey, 1.05), "#222222")
    if marks:
        for i in range(4):
            x = -L * 0.25 + i * L * 0.22
            p = R_([(x - 3, -H * 0.7), (x + 3, H * 0.7)])
            a.stroke(poly(p, closed=False), marks, 1.7, 0.65)


def roast_half(a, cx, cy, s=1.0, color="#D38A3A"):
    """Half a roast chicken, leg forward."""
    body = [(cx - 16 * s, cy + 2 * s), (cx - 12 * s, cy - 9 * s), (cx, cy - 12 * s), (cx + 13 * s, cy - 8 * s),
            (cx + 17 * s, cy + 1 * s), (cx + 10 * s, cy + 8 * s), (cx - 6 * s, cy + 9 * s)]
    a.fill(smooth([(x, y + 2.4 * s) for x, y in body]), shade(color, -0.3))
    a.fill(smooth(body), color)
    a.fill(smooth([(cx - 10 * s, cy - 5 * s), (cx, cy - 9 * s), (cx + 9 * s, cy - 6 * s), (cx, cy - 4 * s)]), shade(color, 0.3), 0.8)
    chicken_piece(a, cx + 6 * s, cy + 4 * s, 0.75 * s, 20, shade(color, -0.08))
    dots(a, shade(color, -0.4), cx, cy - 2 * s, 12 * s, 6 * s, 9, 0.6 * s, int(cx), 0.5)


def potato_chunk(a, x, y, r, seed):
    a.fill(blob(x, y + 0.8, r, r * 0.8, seed, 0.18, 6), "#C88A2E")
    a.fill(blob(x, y, r, r * 0.78, seed, 0.18, 6), "#F1C55C")
    a.fill(ellipse(x - r * 0.25, y - r * 0.3, r * 0.4, r * 0.22), "#FBE3A0", 0.8)


def carrot_piece(a, x, y, deg, length=8):
    a.fill(capsule(x, y, length, 3.4, deg), "#E9822C")
    a.fill(capsule(x - 0.5, y - 0.6, length * 0.7, 1.2, deg), "#F8B061", 0.8)


def pea_dots(a, cx, cy, rx, ry, n, seed):
    dots(a, "#4E9B2C", cx, cy, rx, ry, n, 1.5, seed)
    dots(a, "#86C653", cx - 0.4, cy - 0.4, rx, ry, n, 0.6, seed)


def sauce_pool(a, cx, cy, rx, ry, color, seed=2):
    a.fill(blob(cx, cy, rx, ry, seed, 0.08, 12), color)
    a.fill(blob(cx - rx * 0.2, cy - ry * 0.25, rx * 0.5, ry * 0.35, seed + 1, 0.15, 8), shade(color, 0.2), 0.6)


def lettuce_leaf(a, cx, cy, size, deg=0, color="#7DC04B", seed=1):
    g = rnd(seed)
    pts = []
    for i in range(14):
        ang = 2 * math.pi * i / 14
        k = 1 + (0.18 if i % 2 else -0.05) + g.uniform(-0.05, 0.05)
        pts.append((cx + math.cos(ang) * size * k, cy + math.sin(ang) * size * 0.62 * k))
    a.fill(smooth(rot_pts(pts, cx, cy, deg)), color)
    a.fill(smooth(rot_pts([(cx - size * 0.6, cy), (cx, cy - size * 0.32), (cx + size * 0.5, cy - size * 0.1), (cx, cy + size * 0.2)], cx, cy, deg)),
           shade(color, 0.3), 0.7)
    a.stroke(poly(rot_pts([(cx - size * 0.8, cy + 1), (cx + size * 0.7, cy - 1)], cx, cy, deg), closed=False), shade(color, 0.45), 0.9, 0.8)


def tomato_cubes(a, cx, cy, rx, ry, n, seed, size=2.6):
    g = rnd(seed)
    for _ in range(n):
        t = g.uniform(0, 2 * math.pi)
        k = math.sqrt(g.uniform(0, 1))
        x, y = cx + math.cos(t) * rx * k, cy + math.sin(t) * ry * k
        a.fill(rrect(x - size / 2, y - size / 2, size, size * 0.9, 0.8), TOMATO)
        a.fill(rrect(x - size / 2 + 0.4, y - size / 2 + 0.3, size * 0.5, size * 0.35, 0.4), "#F7897C", 0.8)


def cucumber_cubes(a, cx, cy, rx, ry, n, seed, size=2.6):
    g = rnd(seed)
    for _ in range(n):
        t = g.uniform(0, 2 * math.pi)
        k = math.sqrt(g.uniform(0, 1))
        x, y = cx + math.cos(t) * rx * k, cy + math.sin(t) * ry * k
        a.fill(rrect(x - size / 2, y - size / 2, size, size * 0.9, 0.8), "#3E8E2E")
        a.fill(rrect(x - size / 2 + 0.5, y - size / 2 + 0.2, size - 0.8, size * 0.6, 0.6), "#CDEB9E")


def bun(a, cx, top, w, h, color="#E0A04A", seeds=True):
    a.fill(smooth([(cx - w / 2, top + h), (cx - w * 0.47, top + h * 0.4), (cx - w * 0.3, top + h * 0.08), (cx, top),
                   (cx + w * 0.3, top + h * 0.08), (cx + w * 0.47, top + h * 0.4), (cx + w / 2, top + h)], tension=0.9), color)
    a.fill(ellipse(cx - w * 0.15, top + h * 0.35, w * 0.22, h * 0.18), shade(color, 0.3), 0.7)
    if seeds:
        sesame(a, cx, top + h * 0.45, w * 0.36, h * 0.3, 14, int(w))


# ================================================================== main dishes

@art("mansaf")
def _mansaf(a):
    cx, cy, rx, ry = tray(a, cy=64, rx=46, ry=21)
    a.fill(blob(50, cy - 1, rx * 1.0, ry * 0.95, 3, 0.05, 14), "#EED7A6")
    dots(a, "#C9A46A", 50, cy, rx * 0.9, ry * 0.85, 14, 0.9, 4, 0.6)
    a.fill(mound(50, cy + 3, 66, 21, 7), "#D99A30")
    a.fill(mound(49, cy + 1, 60, 19, 8), RICE_YELLOW)
    grains(a, "#FFE7A6", 49, cy - 8, 24, 6, 55, 9)
    for i, (x, y) in enumerate([(37, cy - 11), (50, cy - 17), (63, cy - 11), (44, cy - 6), (57, cy - 5)]):
        a.fill(blob(x, y + 1.6, 7.4, 5.6, i + 3, 0.16, 8), "#5E2E18")
        a.fill(blob(x, y, 7.2, 5.4, i + 3, 0.16, 8), "#8E4B2E")
        a.fill(blob(x - 1.6, y - 1.8, 3.8, 2.2, i + 9, 0.2, 6), "#B4714A", 0.85)
    nuts_topping(a, 50, cy - 9, 18, 6, 5)
    leaves(a, PARSLEY, 50, cy - 9, 18, 5, 7, 9, 1.3)


@art("maqluba")
def _maqluba(a):
    cx, cy, rx, ry = plate(a)
    dome = [(24, cy + 3), (25, cy - 10), (34, cy - 22), (50, cy - 26), (66, cy - 22), (75, cy - 10), (76, cy + 3), (50, cy + 6)]
    a.fill(smooth([(x, y + 2) for x, y in dome]), "#B97A2B")
    a.fill(smooth(dome), "#E3AE55")
    grains(a, "#F7D896", 50, cy - 4, 22, 6, 46, 4)
    for x, y, d in [(31, cy - 12, -62), (40, cy - 20, -32), (50, cy - 23, 0), (60, cy - 20, 32), (69, cy - 12, 62)]:
        a.fill(ellipse_rot(x, y, 6.6, 4.4, d), "#4A2A45")
        a.fill(ellipse_rot(x, y + 0.4, 5.0, 3.1, d), "#C2914F")
        a.fill(ellipse_rot(x - 0.8, y - 0.4, 2.2, 1.2, d), "#DDB074", 0.8)
    for x, y in [(36, cy - 4), (63, cy - 5)]:
        a.fill(blob(x, y, 4.5, 3.4, int(x), 0.25, 8), "#F4E7C6")
        dots(a, "#E2CF9C", x, y, 3, 2, 4, 0.8, int(y))
    chicken_piece(a, 50, cy - 28, 0.75, -18)
    nuts_topping(a, 50, cy - 10, 13, 5, 3)
    leaves(a, PARSLEY, 50, cy - 8, 16, 6, 6, 8, 1.2)


@art("musakhan")
def _musakhan(a):
    ground(a, 36, 86)
    bread_round(a, 50, 65, 41, 18, "#D9A15A", 3, 6)
    a.fill(blob(50, 63, 33, 13, 4, 0.1, 12), "#6E2533")
    g = rnd(5)
    for _ in range(34):
        x, y = 50 + g.uniform(-28, 28), 63 + g.uniform(-10, 10)
        if ((x - 50) / 30) ** 2 + ((y - 63) / 11) ** 2 > 1:
            continue
        a0 = g.uniform(0, 3.14)
        pts = [(x + math.cos(a0 + u) * 3, y + math.sin(a0 + u) * 2) for u in (0, 0.9, 1.8)]
        a.stroke(smooth(pts, closed=False), g.choice(["#B44B60", "#E9B7C2", "#8E3346"]), 1.1, 0.95)
    chicken_piece(a, 41, 57, 1.05, -24, "#C27235")
    chicken_piece(a, 61, 60, 0.95, 200, "#C9783A")
    dots(a, "#F7E7B8", 50, 64, 26, 9, 14, 0.9, 7)
    leaves(a, PARSLEY, 50, 64, 20, 7, 5, 9, 1.2)


@art("kabsa")
def _kabsa(a):
    def top(a, cy):
        chicken_piece(a, 44, cy - 15, 1.0, -25, "#C46A2E")
        chicken_piece(a, 59, cy - 13, 0.9, 205, "#C46A2E")
        dots(a, "#3B2418", 50, cy - 7, 20, 5, 9, 0.9, 6)
        nuts_topping(a, 50, cy - 8, 18, 5, 7, pine=False)
        leaves(a, PARSLEY, 50, cy - 6, 18, 5, 5, 3, 1.2)
    rice_plate(a, "#E58B3A", "#B9622A", "#F7BD7A", 11, top, height=19, width=60)


@art("mandi")
def _mandi(a):
    def top(a, cy):
        roast_half(a, 50, cy - 12, 0.95, "#C77B2F")
        nuts_topping(a, 50, cy - 3, 22, 4, 4)
    cy = rice_plate(a, "#F6EBCB", "#D9C089", "#F2C35A", 21, top, height=16, width=62)


@art("zarb")
def _zarb(a):
    cx, cy, rx, ry = tray(a, cy=64, rx=46, ry=20)
    a.fill(blob(50, cy - 1, rx * 0.98, ry * 0.9, 6, 0.05, 12), "#E7C27F")
    roast_half(a, 40, cy - 6, 0.75, "#C9772F")
    for i, (x, y) in enumerate([(62, cy - 7), (70, cy - 1)]):
        meat_chunk(a, x, y, 1.2, i + 9, "#8A4A2A")
    for i, (x, y) in enumerate([(28, cy + 3), (52, cy + 6), (62, cy + 6), (75, cy - 8)]):
        potato_chunk(a, x, y, 4.2, i + 2)
    for x, y, d in [(46, cy + 7, 20), (72, cy + 4, -30), (32, cy - 8, 60)]:
        carrot_piece(a, x, y, d, 9)
    a.fill(ellipse(57, cy - 12, 4, 2.6), "#F2E0C2")
    a.fill(ellipse(57, cy - 12.5, 2.4, 1.4), "#FFF5E2")
    leaves(a, PARSLEY, 50, cy, 26, 8, 5, 3, 1.2)


@art("qidreh")
def _qidreh(a):
    cx, cy, rx, ry = bowl(a, rim_y=48, rx=34, ry=10, depth=34, body=CLAY, surface="#EED8A2", band="#A65B2A")
    grains(a, "#FFF0C8", cx, cy, rx * 0.8, ry * 0.6, 40, 3)
    g = rnd(4)
    for _ in range(9):
        x, y = cx + g.uniform(-rx * 0.75, rx * 0.75), cy + g.uniform(-ry * 0.5, ry * 0.4)
        a.fill(circle(x, y, 1.8), "#D9A85A")
        a.fill(circle(x - 0.5, y - 0.5, 0.8), "#F2D08E")
    for i, (x, y) in enumerate([(cx - 9, cy - 3), (cx + 6, cy - 4), (cx + 14, cy)]):
        meat_chunk(a, x, y, 0.85, i + 5)
    dots(a, "#F7E7B8", cx, cy - 2, rx * 0.6, ry * 0.4, 8, 0.8, 9)


def dawali(a, color, accents):
    cx, cy, rx, ry = plate(a)
    rows = [(cy - 8, [-7, 7]), (cy - 3.5, [-14, 0, 14]), (cy + 1, [-21, -7, 7, 21]), (cy + 5.5, [-14, 0, 14])]
    for y, xs in rows:
        for x in xs:
            x0 = 50 + x
            a.fill(capsule(x0, y + 1.3, 14.4, 6.4), shade(color, -0.35))
            a.fill(capsule(x0, y, 14, 6), color)
            a.fill(capsule(x0 - 0.6, y - 1.4, 9.5, 1.8), shade(color, 0.28), 0.8)
            for k in (-3.4, 0.4, 4.2):
                a.stroke(poly([(x0 + k - 1.4, y - 2.6), (x0 + k + 1.2, y + 2.6)], closed=False), shade(color, -0.22), 0.7, 0.8)
    accents(a, cy)


@art("dawali_meat")
def _dawali_meat(a):
    def acc(a, cy):
        lemon_wedge(a, 78, cy - 3, 6, 20)
    dawali(a, "#52702A", acc)


@art("dawali_veg")
def _dawali_veg(a):
    def acc(a, cy):
        lemon_wedge(a, 78, cy - 3, 6, 20)
        tomato_slice(a, 22, cy - 4, 5, 3.4)
    dawali(a, "#6E8E34", acc)


def stuffed_plate(a, sauce, draw):
    cx, cy, rx, ry = plate(a)
    if sauce:
        sauce_pool(a, cx, cy, rx * 0.9, ry * 0.85, sauce)
    draw(a, cy)
    leaves(a, "#3E7F2E", 50, cy, 24, 6, 6, 4, 1.2)


@art("kousa_mahshi")
def _kousa(a):
    def d(a, cy):
        for i, (x, y, dg) in enumerate([(36, cy - 2, -12), (52, cy - 5, 8), (60, cy + 3, -6)]):
            a.fill(capsule(x, y + 1.2, 26, 9, dg), "#6E8F38")
            a.fill(capsule(x, y, 26, 9, dg), "#A9C96A")
            for k in (-2, 1.5):
                p = rot_pts([(x - 10, y + k), (x + 10, y + k)], x, y, dg)
                a.stroke(poly(p, closed=False), "#86A84E", 1.0, 0.7)
            ex, ey = rot_pts([(x + 12, y)], x, y, dg)[0]
            a.fill(ellipse_rot(ex, ey, 1.8, 3.6, dg), "#F3E4BE")
    stuffed_plate(a, "#C44A30", d)


@art("batinjan_mahshi")
def _batinjan(a):
    def d(a, cy):
        for i, (x, y, dg) in enumerate([(36, cy - 2, -18), (54, cy - 5, 10), (58, cy + 4, -4)]):
            a.fill(ellipse_rot(x, y, 11, 7, dg), "#3E2340")
            a.fill(ellipse_rot(x - 2, y - 2.4, 5, 2.2, dg), "#7A4D7E", 0.8)
            cx_, cy_ = rot_pts([(x - 11, y)], x, y, dg)[0]
            a.fill(ellipse_rot(cx_, cy_, 3.4, 4.6, dg), "#5D8B33")
            a.fill(capsule(cx_ - 3, cy_ - 1, 5, 1.6, dg + 10), "#4F7A2B")
            ox, oy = rot_pts([(x + 10.5, y)], x, y, dg)[0]
            a.fill(ellipse_rot(ox, oy, 1.6, 3.4, dg), "#E9D6A8")
    stuffed_plate(a, "#C44A30", d)


@art("filfil_mahshi")
def _filfil(a):
    def d(a, cy):
        for i, (x, y, col) in enumerate([(38, cy - 3, "#4E9A34"), (60, cy - 4, "#E2462F"), (49, cy + 4, "#F0B530")]):
            a.fill(blob(x, y + 1.5, 10.5, 7.5, i + 3, 0.1, 8), shade(col, -0.3))
            a.fill(blob(x, y, 10.5, 7.5, i + 3, 0.1, 8), col)
            a.fill(ellipse(x, y - 1, 6.8, 4.4), shade(col, -0.25))
            a.fill(ellipse(x, y - 1.4, 6, 3.8), "#EED7A0")
            grains(a, "#FFF0C8", x, y - 1.4, 4.5, 2.6, 8, i + 5, 1.6, 0.8)
            a.fill(ellipse(x - 5, y + 2, 2.2, 1.4), shade(col, 0.35), 0.7)
    stuffed_plate(a, "#C44A30", d)


@art("malfouf_mahshi")
def _malfouf(a):
    cx, cy, rx, ry = plate(a)
    for y, xs in [(cy - 6, [-14, -4, 6, 16]), (cy + 1, [-19, -9, 1, 11, 21]), (cy + 7, [-10, 0, 10])]:
        for x in xs:
            roll(a, 50 + x, y, 12, 5.6, "#C9D58F", 90, lines=False)
            a.stroke(poly([(50 + x - 2, y - 3), (50 + x + 1.5, y + 3)], closed=False), "#E9EEC4", 0.9, 0.8)
    dots(a, "#F5F2E8", 50, cy - 1, 20, 6, 12, 0.7, 3, 0.9)
    lemon_wedge(a, 77, cy - 5, 5.5, 25)


def stew_bowl(a, surface, garnish, band=None, body=CERAMIC):
    cx, cy, rx, ry = bowl(a, rim_y=50, rx=35, ry=10, depth=32, body=body, surface=surface, band=band)
    a.fill(ellipse(cx - rx * 0.3, cy - ry * 0.25, rx * 0.35, ry * 0.25), shade(surface, 0.22), 0.6)
    garnish(a, cx, cy, rx, ry)


@art("mulukhiyah")
def _mulukhiyah(a):
    def gnsh(a, cx, cy, rx, ry):
        leaves(a, "#2C4416", cx, cy, rx * 0.8, ry * 0.6, 22, 4, 1.4)
        swirl(a, cx + 3, cy, rx * 0.4, ry * 0.4, "#6F8F3A", 1.2, 0.6)
        for i, (x, y) in enumerate([(cx - 14, cy - 1), (cx + 12, cy + 1)]):
            a.fill(blob(x, y, 4.5, 2.6, i + 3, 0.2, 7), "#E8B979")
    stew_bowl(a, "#3F5C1E", gnsh)
    lemon_wedge(a, 76, 78, 7, -15)


@art("bamia")
def _bamia(a):
    def gnsh(a, cx, cy, rx, ry):
        g = rnd(4)
        for _ in range(10):
            x, y = cx + g.uniform(-rx * 0.75, rx * 0.75), cy + g.uniform(-ry * 0.5, ry * 0.4)
            a.fill(ellipse(x, y, 2.6, 2.0), "#5F8F2E")
            a.fill(ellipse(x, y, 1.5, 1.1), "#C9DD8E")
            a.fill(circle(x, y, 0.4), "#5F8F2E")
        for x, y, d in [(cx - 10, cy - 2, 20), (cx + 8, cy + 1, -30)]:
            a.fill(capsule(x, y, 10, 3.4, d), "#5F8F2E")
        for i, (x, y) in enumerate([(cx + 15, cy - 2)]):
            meat_chunk(a, x, y, 0.7, 3)
    stew_bowl(a, "#B7382A", gnsh)


@art("fasolia")
def _fasolia(a):
    def gnsh(a, cx, cy, rx, ry):
        g = rnd(6)
        for _ in range(12):
            x, y = cx + g.uniform(-rx * 0.75, rx * 0.75), cy + g.uniform(-ry * 0.5, ry * 0.4)
            d = g.uniform(-60, 60)
            a.fill(capsule(x, y, 8, 2.6, d), "#4F8F2B")
            a.fill(capsule(x - 0.3, y - 0.5, 5.5, 0.9, d), "#8CC45A", 0.9)
        meat_chunk(a, cx + 12, cy - 1, 0.7, 4)
    stew_bowl(a, "#C2432C", gnsh)


@art("bazella")
def _bazella(a):
    def gnsh(a, cx, cy, rx, ry):
        pea_dots(a, cx, cy, rx * 0.8, ry * 0.55, 22, 3)
        g = rnd(8)
        for _ in range(8):
            x, y = cx + g.uniform(-rx * 0.7, rx * 0.7), cy + g.uniform(-ry * 0.5, ry * 0.4)
            a.fill(rrect(x - 1.6, y - 1.4, 3.2, 2.8, 0.6), "#F0892E")
        meat_chunk(a, cx - 12, cy, 0.7, 6)
    stew_bowl(a, "#D2552F", gnsh)


@art("mujaddara")
def _mujaddara(a):
    def top(a, cy):
        dots(a, "#5A3A26", 49, cy - 8, 20, 6, 30, 0.8, 4, 0.8)
        onion_curls(a, 50, cy - 13, 15, 4, 26, 6)
    rice_plate(a, "#9C7650", "#6F4E33", "#C7A27A", 31, top, height=18, width=58)


@art("sayadieh")
def _sayadieh(a):
    def top(a, cy):
        fish_fillet = [(30, cy - 13), (44, cy - 19), (62, cy - 18), (72, cy - 13), (60, cy - 10), (40, cy - 9)]
        a.fill(smooth([(x, y + 1.6) for x, y in fish_fillet]), "#B97C3A")
        a.fill(smooth(fish_fillet), "#E8B66A")
        for i in range(4):
            x = 40 + i * 7
            a.stroke(poly([(x, cy - 17), (x + 3, cy - 11)], closed=False), "#C98D45", 1.1, 0.7)
        nuts_topping(a, 50, cy - 5, 20, 3, 8)
        lemon_wedge(a, 74, cy - 4, 5, 15)
    rice_plate(a, "#A0662E", "#73471E", "#CC9558", 41, top, height=16, width=60)


@art("fatteh_chicken")
def _fatteh_chicken(a):
    cx, cy, rx, ry = bowl(a, surface="#F5F0E2", body="#E9E2D5")
    g = rnd(3)
    for _ in range(5):
        x, y = cx + g.uniform(-rx * 0.8, rx * 0.8), cy + g.uniform(-ry * 0.4, ry * 0.3)
        a.fill(poly([(x - 3, y + 1), (x, y - 3), (x + 3.4, y + 1.2)]), "#E2A84E")
    for i, (x, y) in enumerate([(cx - 8, cy - 1), (cx + 7, cy)]):
        a.fill(blob(x, y, 5, 2.8, i + 4, 0.2, 7), "#E7B26B")
    dots(a, "#F7E7B8", cx, cy - 1, rx * 0.5, ry * 0.4, 12, 0.9, 6)
    dots(a, "#C9452D", cx, cy - 1, rx * 0.5, ry * 0.4, 8, 0.45, 9)
    leaves(a, PARSLEY, cx, cy - 1, rx * 0.5, ry * 0.4, 5, 2, 1.2)


def kibbeh_shape(a, x, y, length, width, deg, seed):
    pts = [(x - length / 2, y), (x - length * 0.25, y - width / 2), (x + length * 0.25, y - width / 2),
           (x + length / 2, y), (x + length * 0.25, y + width / 2), (x - length * 0.25, y + width / 2)]
    pts = rot_pts(pts, x, y, deg)
    a.fill(smooth([(px, py + 1.6) for px, py in pts], tension=0.9), "#5E3016")
    a.fill(smooth(pts, tension=0.9), "#93511F")
    a.fill(capsule(x - 1, y - width * 0.22, length * 0.45, width * 0.25, deg), "#B9732F", 0.8)
    dots(a, "#6E3A18", x, y, length * 0.3, width * 0.3, 5, 0.5, seed, 0.6)


@art("kibbeh_fried")
def _kibbeh_fried(a):
    cx, cy, rx, ry = plate(a)
    for i, (x, y, d) in enumerate([(36, cy - 2, -18), (52, cy - 6, 6), (58, cy + 4, -10)]):
        kibbeh_shape(a, x, y, 24, 11, d, i + 1)
    lemon_wedge(a, 76, cy - 3, 5.5, 25)
    leaves(a, PARSLEY, 26, cy + 3, 4, 2, 3, 4, 1.4)


def clip_line(p0, p1, cx, cy, A, B):
    """Part of segment p0-p1 inside the ellipse, or None."""
    dx, dy = p1[0] - p0[0], p1[1] - p0[1]
    ox, oy = (p0[0] - cx) / A, (p0[1] - cy) / B
    ux, uy = dx / A, dy / B
    qa, qb, qc = ux * ux + uy * uy, 2 * (ox * ux + oy * uy), ox * ox + oy * oy - 1
    disc = qb * qb - 4 * qa * qc
    if disc <= 0:
        return None
    r = math.sqrt(disc)
    t0, t1 = max(0, (-qb - r) / (2 * qa)), min(1, (-qb + r) / (2 * qa))
    if t1 <= t0:
        return None
    return [(p0[0] + dx * t0, p0[1] + dy * t0), (p0[0] + dx * t1, p0[1] + dy * t1)]


@art("kibbeh_tray")
def _kibbeh_tray(a):
    cx, cy, rx, ry = tray(a, cy=62, rx=44, ry=20, metal="#C3A27A")
    a.fill(ellipse(cx, cy - 1, rx * 0.98, ry * 0.95), "#7E4219")
    A, B = rx * 0.95, ry * 0.9
    a.fill(ellipse(cx, cy - 2, A, B), "#A35E28")
    a.fill(ellipse(cx - 8, cy - 6, A * 0.5, B * 0.4), "#B86E33", 0.7)
    for k in range(-6, 7):
        for sgn in (1, -1):
            seg = clip_line((cx + k * 9 - sgn * 16, cy - 24), (cx + k * 9 + sgn * 16, cy + 22), cx, cy - 2, A - 1.5, B - 1.2)
            if seg:
                a.stroke(poly(seg, closed=False), "#6E3714", 1.1, 0.8)
    g = rnd(4)
    for k in range(-5, 6):
        for j in range(-2, 3):
            x, y = cx + k * 9 + j * 0.0, cy - 2 + j * 7.5
            if ((x - cx) / (A - 4)) ** 2 + ((y - cy + 2) / (B - 3)) ** 2 < 1 and (k + j) % 2 == 0:
                a.fill(ellipse_rot(x, y, 1.3, 0.75, g.uniform(0, 180)), "#F7E7B8")


@art("kibbeh_labaniyeh")
def _kibbeh_laban(a):
    cx, cy, rx, ry = bowl(a, surface="#F5F1E6", body="#E8EEF4")
    for i, (x, y) in enumerate([(cx - 13, cy), (cx, cy - 2), (cx + 13, cy + 0.5), (cx - 5, cy + 3), (cx + 7, cy + 3)]):
        a.fill(blob(x, y, 5.6, 3.6, i + 2, 0.08, 8), "#8E4E22")
        a.fill(ellipse(x - 1.4, y - 1.4, 2.4, 1.2), "#B9763A", 0.8)
    dots(a, "#4F7A2B", cx, cy, rx * 0.7, ry * 0.5, 14, 0.5, 8)
    swirl(a, cx, cy, rx * 0.4, ry * 0.3, "#E2DCCB", 1.0, 0.7)


@art("kofta_tray")
def _kofta_tray(a):
    cx, cy, rx, ry = tray(a, cy=62, rx=44, ry=20, metal="#B9BEC6")
    a.fill(ellipse(cx, cy - 1, rx * 0.95, ry * 0.9), "#B2372A")
    a.fill(ellipse(cx - 6, cy - 4, rx * 0.5, ry * 0.4), "#C9503A", 0.7)
    for i, (x, y) in enumerate([(30, cy - 6), (48, cy - 9), (66, cy - 6), (38, cy + 4), (58, cy + 4)]):
        a.fill(ellipse(x, y + 1, 6.6, 3.6), "#C88A2E")
        a.fill(ellipse(x, y, 6.6, 3.4), "#F1C55C")
    for i, (x, y) in enumerate([(39, cy - 6), (57, cy - 7), (48, cy + 1), (29, cy + 1), (68, cy + 1)]):
        a.fill(ellipse(x, y + 1.2, 6, 3.6), "#5E2A14")
        a.fill(ellipse(x, y, 6, 3.4), "#8F4824")
        a.fill(ellipse(x - 1.4, y - 1, 2.4, 1.1), "#B26A3A", 0.8)
    for x, y in [(44, cy - 13), (62, cy - 12), (33, cy + 8)]:
        tomato_slice(a, x, y, 3.6, 2.4)
    leaves(a, PARSLEY, cx, cy - 2, rx * 0.6, ry * 0.5, 6, 3, 1.2)


# ================================================================== grill

@art("shish_tawook")
def _shish_tawook(a):
    cx, cy, rx, ry = plate(a)
    for k, (dy, seed) in enumerate([(-7, 2), (4, 5)]):
        x0, y0, x1, y1 = 16, cy + dy + 6, 84, cy + dy - 6
        skewer(a, x0, y0, x1, y1)
        pieces = ["c", "p", "c", "o", "c", "r", "c"]
        for i, p in enumerate(pieces):
            t = 0.16 + i * 0.115
            x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            if p == "c":
                a.fill(rrect(x - 4.4, y - 4, 8.8, 8.6, 2.6), "#C9772F")
                a.fill(rrect(x - 4.4, y - 4.6, 8.8, 8, 2.6), "#ECA650")
                a.fill(poly([(x - 3, y - 3), (x + 1, y + 3)], closed=False), WHITE, 0)
                a.stroke(poly([(x - 2.6, y - 2), (x + 0.5, y + 2.4)], closed=False), "#7A3F18", 1.1, 0.6)
            elif p == "p":
                a.fill(rrect(x - 2, y - 4.4, 4, 8.6, 1.6), "#3E8E2E")
            elif p == "r":
                a.fill(rrect(x - 2, y - 4.4, 4, 8.6, 1.6), "#D9392B")
            else:
                a.fill(rrect(x - 2, y - 4.2, 4, 8.2, 1.6), "#EADCF0")


@art("kebab")
def _kebab(a):
    ground(a, 36, 84)
    bread_round(a, 50, 68, 40, 14, "#EBC27A", 6, 5)
    for dy in (-6, 4):
        skewer(a, 14, 62 + dy + 4, 86, 62 + dy - 4)
        a.fill(capsule(48, 62 + dy, 50, 7.6, -6.4), "#5E2A14")
        a.fill(capsule(48, 61 + dy, 50, 7, -6.4), "#8F4824")
        for i in range(6):
            x = 28 + i * 8
            y = 62 + dy - (x - 48) * 0.112
            a.fill(ellipse_rot(x, y, 0.9, 3.2, -6), "#4E2210", 0.7)
        a.fill(capsule(46, 59.5 + dy, 36, 1.8, -6.4), "#B26A3A", 0.7)
    a.fill(circle(78, 72, 6), "#C93A2B")
    a.fill(circle(76.5, 70.5, 2.2), "#F06A5A")
    a.fill(ellipse(78, 66.5, 2.2, 1.2), "#3E8E2E")
    leaves(a, PARSLEY, 26, 72, 6, 2, 5, 3, 1.3)


@art("kofta_grilled")
def _kofta_grilled(a):
    cx, cy, rx, ry = plate(a)
    for i, (x, y, d) in enumerate([(40, cy - 4, -14), (54, cy - 6, -14), (47, cy + 4, -14)]):
        kofta_finger(a, x, y, 26, 8.6, d)
        grill_marks(a, x, y, 6, 20, d + 90, "#3B1A0C", 3, 1.2, 0.6)
    onion_curls(a, 66, cy + 2, 6, 3, 8, 4, "#C7A2D4", 1.1)
    leaves(a, PARSLEY, 68, cy, 6, 3, 6, 5, 1.3)
    tomato_slice(a, 27, cy + 3, 5, 3.4)


@art("chicken_grilled")
def _chicken_grilled(a):
    cx, cy, rx, ry = plate(a)
    roast_half(a, 48, cy - 4, 1.25, "#C8732C")
    grill_marks(a, 44, cy - 8, 18, 10, 30, "#5A2A10", 3, 1.6, 0.55)
    lemon_wedge(a, 77, cy + 1, 5.5, 25)
    leaves(a, PARSLEY, 24, cy + 2, 5, 2, 5, 3, 1.3)


@art("chicken_breast")
def _chicken_breast(a):
    cx, cy, rx, ry = plate(a)
    for i, (x, y, d) in enumerate([(42, cy - 2, -10), (58, cy + 1, 12)]):
        pts = rot_pts([(x - 14, y + 1), (x - 8, y - 6), (x + 4, y - 7), (x + 13, y - 2), (x + 10, y + 5), (x - 4, y + 6)], x, y, d)
        a.fill(smooth([(px, py + 1.8) for px, py in pts]), "#B57434")
        a.fill(smooth(pts), "#E8B271")
        grill_marks(a, x, y - 1, 5, 18, d + 70, "#6B3714", 4, 1.5, 0.65)
    lemon_wedge(a, 74, cy - 6, 5, 15)
    leaves(a, PARSLEY, 28, cy + 4, 5, 2, 5, 6, 1.3)


@art("chicken_thigh")
def _chicken_thigh(a):
    cx, cy, rx, ry = plate(a)
    for i, (x, y) in enumerate([(40, cy - 2), (60, cy - 1)]):
        a.fill(blob(x, y + 1.8, 12, 8.4, i + 4, 0.12, 9), "#8E4A1C")
        a.fill(blob(x, y, 12, 8.2, i + 4, 0.12, 9), "#C8732C")
        a.fill(blob(x - 3, y - 3, 6, 3, i + 9, 0.2, 7), "#E59A4C", 0.85)
        dots(a, "#7A3B14", x, y, 8, 5, 6, 0.6, i + 2, 0.5)
    leaves(a, PARSLEY, 50, cy + 6, 14, 2, 5, 3, 1.3)


@art("steak")
def _steak(a):
    cx, cy, rx, ry = plate(a)
    pts = [(28, cy - 2), (34, cy - 11), (52, cy - 13), (68, cy - 9), (73, cy), (62, cy + 7), (40, cy + 7)]
    a.fill(smooth([(x, y + 2.4) for x, y in pts]), "#4A2414")
    a.fill(smooth(pts), "#7B3E22")
    a.fill(smooth([(30, cy - 3), (35, cy - 10.5), (50, cy - 12.6), (49, cy - 10), (36, cy - 7.5)]), "#EBD3B5")
    a.fill(blob(54, cy - 3, 12, 5, 4, 0.15, 8), "#95502E", 0.8)
    for i in range(4):
        x = 40 + i * 8
        a.stroke(poly([(x - 4, cy + 4), (x + 4, cy - 9)], closed=False), "#2B1209", 2.0, 0.7)
    a.stroke(smooth([(66, cy + 5), (72, cy + 2), (79, cy - 3)], closed=False), "#4E7A2B", 1.3)
    for k in range(5):
        x, y = 67 + k * 2.6, cy + 4 - k * 1.6
        a.fill(ellipse_rot(x, y - 1.6, 0.7, 2.0, 30), "#5E9A3A")
        a.fill(ellipse_rot(x + 0.6, y + 1.3, 0.7, 2.0, -30), "#5E9A3A")


@art("beef_mince")
def _beef_mince(a):
    cx, cy, rx, ry = pan(a, "#6A3418")
    g = rnd(5)
    for _ in range(60):
        t = g.uniform(0, 2 * math.pi)
        k = math.sqrt(g.uniform(0, 1))
        x, y = cx + math.cos(t) * rx * 0.85 * k, cy + math.sin(t) * ry * 0.8 * k
        a.fill(blob(x, y, g.uniform(1.4, 2.4), g.uniform(1.0, 1.7), g.randint(0, 99), 0.3, 6),
               g.choice(["#8A4A26", "#A0582E", "#73391C", "#B66B3C"]))
    leaves(a, PARSLEY, cx, cy, rx * 0.6, ry * 0.5, 6, 3, 1.2)


@art("lamb")
def _lamb(a):
    cx, cy, rx, ry = plate(a)
    for i, (x, y, d) in enumerate([(34, cy, -30), (50, cy - 3, -12), (66, cy, 8)]):
        bx, by = rot_pts([(x, y - 16)], x, y, d)[0]
        a.stroke(poly([(x, y - 4), (bx, by)], closed=False), "#F3EEE3", 3.4)
        a.fill(circle(bx, by, 2.2), "#F3EEE3")
        pts = rot_pts([(x - 8, y), (x - 6, y - 6), (x, y - 8), (x + 6, y - 6), (x + 8, y), (x + 4, y + 5), (x - 4, y + 5)], x, y, d)
        a.fill(smooth([(px, py + 1.8) for px, py in pts]), "#5A2814")
        a.fill(smooth(pts), "#8C4526")
        a.fill(ellipse_rot(x - 1, y - 1.5, 4, 2.6, d), "#B9653F", 0.8)
        grill_marks(a, x, y, 4, 8, d + 70, "#2E1408", 2, 1.3, 0.6)
    leaves(a, "#4E7A2B", 50, cy + 7, 18, 2, 6, 5, 1.3)


@art("fish_grilled")
def _fish_grilled(a):
    cx, cy, rx, ry = plate(a)
    fish(a, 48, cy - 3, 58, "#A08A6A", "#D9CBB0", -6, marks="#3A2A1A")
    lemon_wedge(a, 30, cy + 7, 4.6, 10)
    lemon_wedge(a, 42, cy + 8, 4.6, -10)
    leaves(a, PARSLEY, 70, cy + 6, 6, 2, 6, 3, 1.3)


@art("fish_fried")
def _fish_fried(a):
    cx, cy, rx, ry = plate(a)
    fish(a, 48, cy - 3, 58, "#C9873A", "#E7B66A", -6)
    dots(a, "#9A5A22", 46, cy - 4, 18, 5, 18, 0.55, 7, 0.6)
    lemon_wedge(a, 74, cy + 6, 5, 15)
    a.fill(ellipse(28, cy + 7, 6, 2.6), "#F5F1E6")


@art("shrimp")
def _shrimp(a):
    cx, cy, rx, ry = plate(a)
    for i, (x, y, d) in enumerate([(36, cy - 2, -10), (52, cy - 4, 10), (64, cy + 2, -20), (44, cy + 5, 30)]):
        pts = []
        for k in range(9):
            t = math.pi * 0.15 + k / 8 * math.pi * 1.1
            pts.append((x + math.cos(t) * 7.4, y - math.sin(t) * 5.6))
        pts = rot_pts(pts, x, y, d)
        a.stroke(smooth(pts, closed=False), "#E3683C", 5.2)
        a.stroke(smooth(pts, closed=False), "#F59A6A", 3.0, 0.9)
        for k in (2, 4, 6):
            px, py = pts[k]
            a.fill(circle(px, py, 0.6), "#C9502A", 0.8)
        tx, ty = pts[-1]
        a.fill(blob(tx, ty, 2.2, 2.6, i + 3, 0.2, 6), "#D9532C")
    lemon_wedge(a, 76, cy - 6, 4.6, 20)
    leaves(a, PARSLEY, 50, cy, 18, 6, 5, 9, 1.1)


def tin(a, label, label2, contents):
    ground(a, 34, 82)
    a.fill(ellipse(50, 70, 34, 13), "#9EA5AF")
    a.fill(rrect(16, 56, 68, 14, 2), "#9EA5AF")
    a.fill(rrect(16, 58, 68, 10, 1), label)
    a.fill(rrect(16, 62, 68, 2.6, 1), label2)
    a.fill(ellipse(50, 56, 34, 13), "#C8CED6")
    a.fill(ellipse(50, 56, 31, 11.4), "#AEB4BE")
    contents(a)
    # peeled lid
    lid = [(70, 50), (86, 30), (92, 36), (78, 54)]
    a.fill(ellipse_rot(84, 38, 10, 14, 35), "#D2D7DE")
    a.fill(ellipse_rot(84, 38, 7.4, 11, 35), "#E4E8ED")
    a.stroke(ellipse_rot(84, 38, 3, 4.4, 35), "#AEB4BE", 1.6)


def tuna_flakes(a, oil):
    a.fill(ellipse(50, 56, 30, 10.6), "#E6CDB2" if not oil else "#D9B98F")
    g = rnd(5)
    for _ in range(28):
        x, y = 50 + g.uniform(-24, 24), 56 + g.uniform(-7, 7)
        if ((x - 50) / 27) ** 2 + ((y - 56) / 9) ** 2 > 1:
            continue
        a.fill(blob(x, y, g.uniform(1.6, 3.0), g.uniform(1.0, 1.8), g.randint(0, 99), 0.3, 6),
               g.choice(["#D9B596", "#C99F7E", "#F0DCC4", "#E3C3A3"]))
    if oil:
        a.fill(ellipse(42, 55, 10, 3.4), OIL, 0.5)


@art("tuna_water")
def _tuna_water(a):
    tin(a, "#2F7BC9", "#9FD0F5", lambda a: tuna_flakes(a, False))


@art("tuna_oil")
def _tuna_oil(a):
    tin(a, "#E2B32E", "#5D7A2A", lambda a: tuna_flakes(a, True))


@art("sardines")
def _sardines(a):
    ground(a, 36, 84)
    a.fill(rrect(12, 50, 76, 32, 12), "#8E959F")
    a.fill(rrect(12, 47, 76, 32, 12), "#C8CED6")
    a.fill(rrect(16, 50, 68, 26, 9), "#B98A4A")
    for i in range(4):
        y = 54.5 + i * 6
        a.fill(capsule(50, y, 60, 5.4), "#8A96A3")
        a.fill(capsule(50, y - 0.8, 56, 2.6), "#C9D3DE")
        a.fill(poly([(78, y), (83, y - 2.8), (83, y + 2.8)]), "#6F7A86")
    a.fill(rrect(80, 26, 14, 26, 5), "#D2D7DE")
    a.stroke(ellipse(87, 30, 3, 2.4), "#AEB4BE", 1.4)


# ================================================================== fast food

def wrap_sandwich(a, filling, filling2, sauce):
    ground(a, 26, 88)
    d = -22
    cx, cy = 50, 52
    body = rot_pts([(cx - 13, cy - 32), (cx + 13, cy - 32), (cx + 13, cy + 32), (cx - 13, cy + 32)], cx, cy, d)
    a.fill(smooth(rot_pts([(cx - 13, cy - 28), (cx, cy - 34), (cx + 13, cy - 28), (cx + 13.5, cy + 30), (cx, cy + 34),
                           (cx - 13.5, cy + 30)], cx, cy, d), tension=0.6), "#E7C28A")
    # filling at the open end
    top = rot_pts([(cx - 11, cy - 28), (cx - 6, cy - 37), (cx + 2, cy - 39), (cx + 9, cy - 35), (cx + 11, cy - 28)], cx, cy, d)
    a.fill(smooth(top + rot_pts([(cx, cy - 25)], cx, cy, d)), filling)
    for i, (x, y) in enumerate(rot_pts([(cx - 5, cy - 33), (cx + 4, cy - 34), (cx, cy - 30)], cx, cy, d)):
        a.fill(blob(x, y, 3.2, 2, i + 2, 0.25, 6), filling2)
    for x, y in rot_pts([(cx - 8, cy - 31), (cx + 8, cy - 30)], cx, cy, d):
        a.fill(ellipse_rot(x, y, 2.4, 1.3, d + 30), "#7FA63A")
    sx, sy = rot_pts([(cx + 1, cy - 29)], cx, cy, d)[0]
    a.fill(ellipse_rot(sx, sy, 6, 1.8, d), sauce, 0.95)
    # bread fold highlight
    a.fill(capsule(*rot_pts([(cx - 6, cy - 6)], cx, cy, d)[0], 40, 3.4, d + 90), "#F3D9AC", 0.8)
    # paper wrapping the lower half
    paper = rot_pts([(cx - 14.5, cy + 2), (cx + 14.5, cy - 4), (cx + 14.5, cy + 31), (cx, cy + 35.5), (cx - 14.5, cy + 31)], cx, cy, d)
    a.fill(smooth(paper, tension=0.35), "#FFFFFF")
    a.fill(smooth(rot_pts([(cx + 4, cy - 1.5), (cx + 14.5, cy - 4), (cx + 14.5, cy + 31), (cx + 6, cy + 34)], cx, cy, d), tension=0.35), "#E9EBEF")
    stripe = rot_pts([(cx - 14.5, cy + 9), (cx + 14.5, cy + 3), (cx + 14.5, cy + 7), (cx - 14.5, cy + 13)], cx, cy, d)
    a.fill(poly(stripe), "#E2483B")


@art("shawarma_chicken")
def _shawarma_chicken(a):
    wrap_sandwich(a, "#E8A852", "#D98838", "#FBF8F0")


@art("shawarma_meat")
def _shawarma_meat(a):
    wrap_sandwich(a, "#8A4524", "#6B321A", "#E9D6A6")


def shawarma_plate(a, color, color2, sauce):
    cx, cy, rx, ry = plate(a)
    g = rnd(7)
    pieces = []
    a.fill(mound(46, cy + 2, 50, 13, 3, 8, 0.25), shade(color2, -0.2))
    for _ in range(46):
        x, y = 46 + g.gauss(0, 9.5), cy - 4 + g.gauss(0, 3.8)
        pieces.append((y, x, g.uniform(-45, 45), g.choice([color, color2, shade(color, 0.15)])))
    for y, x, d, col in sorted(pieces):
        a.fill(capsule(x, y, g.uniform(9, 13), 3.4, d), col)
        a.fill(capsule(x - 0.5, y - 0.8, 5, 1.0, d), shade(col, 0.3), 0.7)
    a.fill(ellipse(76, cy - 2, 8.4, 4.6), "#DADDE3")
    a.fill(ellipse(76, cy - 3, 8, 4.2), WHITE)
    a.fill(ellipse(76, cy - 3, 6.4, 3.2), sauce)
    for x, y in [(30, cy + 6), (62, cy + 7)]:
        a.fill(capsule(x, y, 9, 3, 20), "#7FA63A")
        a.fill(capsule(x, y - 0.4, 7, 1.2, 20), "#B9D47A", 0.8)


@art("shawarma_chicken_meat")
def _shawarma_chicken_meat(a):
    shawarma_plate(a, "#E3A04E", "#C47A2E", "#FBF8F0")


@art("shawarma_meat_only")
def _shawarma_meat_only(a):
    shawarma_plate(a, "#8A4524", "#6B321A", "#E9D6A6")


@art("falafel_sandwich")
def _falafel_sandwich(a):
    ground(a, 30, 86)
    # pita pocket standing, half-moon
    a.fill(smooth([(20, 82), (18, 60), (30, 42), (50, 36), (70, 42), (82, 60), (80, 82)], closed=True, tension=0.8), "#D9A55A")
    a.fill(smooth([(23, 80), (22, 61), (32, 46), (50, 41), (68, 46), (78, 61), (77, 80)], closed=True, tension=0.8), "#EBC27A")
    # filling poking out of the top
    lettuce_leaf(a, 38, 42, 9, -20, seed=3)
    lettuce_leaf(a, 64, 41, 9, 25, seed=5)
    for i, (x, y) in enumerate([(40, 38), (54, 34), (64, 39)]):
        ball(a, x, y, 6, "#7A4A20", i + 2, texture="#5A3412")
    tomato_slice(a, 48, 43, 5, 2.6)
    a.fill(ellipse(52, 45, 14, 2.4), TAHINI, 0.9)
    # front of pocket
    a.fill(smooth([(20, 82), (21, 56), (50, 50), (79, 56), (80, 82), (50, 86)], tension=0.7), "#E3B46A")
    dots(a, "#C48A3E", 50, 70, 24, 10, 8, 0.9, 3, 0.6)
    a.fill(capsule(36, 58, 18, 3, -8), "#F2D398", 0.8)


@art("zinger")
def _zinger(a):
    ground(a, 34, 88)
    a.fill(rrect(22, 72, 56, 12, 6), "#D9963E")
    a.fill(rrect(22, 70, 56, 10, 5), "#EBB25A")
    # crispy fillet sticking out
    g = rnd(4)
    pts = []
    for i in range(22):
        ang = 2 * math.pi * i / 22
        k = 1 + (0.09 if i % 2 else -0.04) + g.uniform(-0.04, 0.04)
        pts.append((50 + math.cos(ang) * 34 * k, 64 + math.sin(ang) * 7.5 * k))
    a.fill(smooth(pts), "#B8641E")
    a.fill(smooth([(x, y - 1.2) for x, y in pts]), "#E08E33")
    dots(a, "#F2B866", 50, 62, 28, 4, 22, 0.8, 5)
    lettuce_leaf(a, 38, 58, 11, -6, "#8CCB55", 2)
    lettuce_leaf(a, 60, 58, 11, 8, "#7DC04B", 4)
    a.fill(ellipse(50, 56, 22, 2.4), "#FBF8F0")
    bun(a, 50, 30, 58, 26, "#E09C48")


@art("broasted")
def _broasted(a):
    ground(a, 36, 88)
    # paper boat
    a.fill(poly([(16, 62), (84, 62), (76, 86), (24, 86)]), "#C9392C")
    a.fill(poly([(16, 62), (84, 62), (82, 67), (18, 67)]), "#E2483B")
    def crispy(x, y, s, d, seed):
        g = rnd(seed)
        pts = []
        for i in range(18):
            ang = 2 * math.pi * i / 18
            k = 1 + (0.1 if i % 2 else -0.03) + g.uniform(-0.05, 0.05)
            pts.append((x + math.cos(ang) * 12 * s * k, y + math.sin(ang) * 9 * s * k))
        pts = rot_pts(pts, x, y, d)
        a.fill(smooth([(px, py + 1.8) for px, py in pts]), "#9A4E16")
        a.fill(smooth(pts), "#D9862E")
        dots(a, "#F2B866", x, y - 2 * s, 8 * s, 5 * s, 10, 0.8, seed + 3)
        dots(a, "#B0601C", x, y + 1, 8 * s, 5 * s, 8, 0.7, seed + 5)
    for x in (24, 30, 70, 76):
        a.fill(capsule(x, 54, 22, 4, 90 + (x - 50) * 0.4), "#F2BF4A")
    crispy(38, 54, 1.0, -20, 2)
    crispy(62, 55, 1.0, 25, 6)
    crispy(50, 46, 0.9, 5, 9)
    a.fill(poly([(18, 67), (82, 67), (76, 86), (24, 86)]), "#C9392C")
    a.fill(poly([(46, 67), (54, 67), (52, 86), (48, 86)]), WHITE, 0.85)


@art("burger")
def _burger(a):
    ground(a, 34, 88)
    a.fill(rrect(22, 74, 56, 10, 5), "#D9963E")
    a.fill(rrect(22, 72, 56, 9, 4.5), "#EBB25A")
    a.fill(smooth([(19, 68), (50, 64), (81, 68), (78, 74), (50, 76), (22, 74)]), "#5A2B16")
    a.fill(smooth([(20, 67), (50, 63), (80, 67), (77, 72), (50, 74), (23, 72)]), "#7A3E22")
    dots(a, "#4A2010", 50, 69, 26, 3, 14, 0.6, 3)
    a.fill(poly([(20, 63), (80, 63), (78, 66), (64, 70), (58, 66), (44, 71), (36, 66), (22, 66)]), "#F6B92E")
    tomato_slice(a, 38, 61, 10, 2.6)
    tomato_slice(a, 62, 61, 10, 2.6)
    lettuce_leaf(a, 36, 58, 13, -4, "#8CCB55", 3)
    lettuce_leaf(a, 62, 58, 13, 4, "#7DC04B", 7)
    bun(a, 50, 28, 62, 28, "#E09C48")


@art("pizza")
def _pizza(a):
    ground(a, 38, 84)
    cx, cy, rx, ry = 50, 62, 40, 19
    a.fill(ellipse(cx, cy + 2.4, rx, ry), "#B9772F")
    a.fill(ellipse(cx, cy, rx, ry), "#E3A04E")
    a.fill(ellipse(cx, cy, rx * 0.88, ry * 0.86), "#D9452E")
    a.fill(blob(cx, cy - 0.5, rx * 0.84, ry * 0.8, 3, 0.06, 14), "#F7CD5E")
    a.fill(blob(cx - 6, cy - 4, rx * 0.4, ry * 0.3, 5, 0.15, 9), "#FBE08A", 0.8)
    g = rnd(4)
    for _ in range(9):
        t = g.uniform(0, 2 * math.pi)
        k = math.sqrt(g.uniform(0.05, 1))
        x, y = cx + math.cos(t) * rx * 0.68 * k, cy + math.sin(t) * ry * 0.65 * k
        a.fill(ellipse(x, y + 0.5, 4.2, 2.3), "#A82E22")
        a.fill(ellipse(x, y, 4.2, 2.2), "#CE3B2C")
        a.fill(ellipse(x - 1, y - 0.6, 1.4, 0.7), "#E86A55", 0.8)
    for _ in range(6):
        x, y = cx + g.uniform(-28, 28), cy + g.uniform(-10, 10)
        if ((x - cx) / 30) ** 2 + ((y - cy) / 14) ** 2 < 1:
            a.stroke(ellipse(x, y, 1.6, 1.0), "#2A2A2A", 1.0)
    leaves(a, "#3E8E2E", cx, cy, rx * 0.6, ry * 0.55, 6, 9, 1.4)
    for ang in (0, 60, 120):
        p0 = (cx + math.cos(math.radians(ang)) * rx * 0.88, cy + math.sin(math.radians(ang)) * ry * 0.86)
        p1 = (cx - math.cos(math.radians(ang)) * rx * 0.88, cy - math.sin(math.radians(ang)) * ry * 0.86)
        a.stroke(poly([p0, p1], closed=False), "#C98A3A", 0.9, 0.7)


# ================================================================== soups

def soup(a, broth, garnish):
    cx, cy, rx, ry = bowl(a, rim_y=48, rx=36, ry=10.5, depth=32, surface=broth)
    a.fill(ellipse(cx - rx * 0.3, cy - ry * 0.2, rx * 0.3, ry * 0.22), shade(broth, 0.25), 0.6)
    garnish(a, cx, cy, rx, ry)


@art("soup_lentil")
def _soup_lentil(a):
    def gsh(a, cx, cy, rx, ry):
        a.fill(ellipse(cx + 8, cy, 8, 2.2), "#E4A63A", 0.8)
        dots(a, "#C9452D", cx, cy, rx * 0.6, ry * 0.5, 10, 0.45, 3)
        leaves(a, PARSLEY, cx, cy, rx * 0.5, ry * 0.4, 5, 5, 1.2)
        for x, y in [(cx - 14, cy - 1), (cx - 8, cy + 2)]:
            a.fill(rrect(x - 2.2, y - 1.8, 4.4, 3.6, 0.8), "#D9A04E")
    soup(a, "#E89A3C", gsh)
    lemon_wedge(a, 76, 76, 7, -15)


@art("soup_veg")
def _soup_veg(a):
    def gsh(a, cx, cy, rx, ry):
        g = rnd(4)
        for _ in range(8):
            x, y = cx + g.uniform(-rx * 0.7, rx * 0.7), cy + g.uniform(-ry * 0.45, ry * 0.4)
            a.fill(rrect(x - 1.8, y - 1.5, 3.6, 3, 0.7), "#F0892E")
        pea_dots(a, cx, cy, rx * 0.7, ry * 0.45, 10, 6)
        for _ in range(4):
            x, y = cx + g.uniform(-rx * 0.6, rx * 0.6), cy + g.uniform(-ry * 0.4, ry * 0.3)
            a.fill(rrect(x - 1.8, y - 1.5, 3.6, 3, 0.7), "#F3E3A6")
        leaves(a, PARSLEY, cx, cy, rx * 0.5, ry * 0.4, 4, 8, 1.2)
    soup(a, "#D86A35", gsh)


@art("soup_freekeh")
def _soup_freekeh(a):
    def gsh(a, cx, cy, rx, ry):
        grains(a, "#6E6A2E", cx, cy, rx * 0.8, ry * 0.55, 40, 3, 2.4, 1.1)
        grains(a, "#B9A55A", cx, cy, rx * 0.8, ry * 0.55, 20, 5, 2.4, 1.1)
        for i, (x, y) in enumerate([(cx - 10, cy), (cx + 11, cy - 1)]):
            a.fill(blob(x, y, 3.6, 2.2, i + 4, 0.2, 7), "#E3B57A")
        leaves(a, PARSLEY, cx, cy, rx * 0.5, ry * 0.4, 4, 9, 1.2)
    soup(a, "#B48A4A", gsh)


@art("soup_chicken")
def _soup_chicken(a):
    def gsh(a, cx, cy, rx, ry):
        g = rnd(6)
        for _ in range(9):
            x, y = cx + g.uniform(-rx * 0.7, rx * 0.7), cy + g.uniform(-ry * 0.45, ry * 0.4)
            a.stroke(smooth([(x - 4, y), (x - 1.5, y - 1.2), (x + 1.5, y + 1), (x + 4, y)], closed=False), "#F6E3A8", 1.2)
        for i, (x, y) in enumerate([(cx - 10, cy), (cx + 9, cy - 1), (cx + 1, cy + 2)]):
            a.fill(blob(x, y, 3.4, 2.2, i + 4, 0.2, 7), "#F1D3A0")
        for x, y in [(cx - 3, cy - 2), (cx + 15, cy + 1)]:
            a.fill(circle(x, y, 1.8), "#F0892E")
        leaves(a, PARSLEY, cx, cy, rx * 0.5, ry * 0.4, 5, 5, 1.2)
    soup(a, "#EBC25A", gsh)


# ================================================================== salads

def salad_bowl(a, draw, body="#E8F1EA", band=None):
    cx, cy, rx, ry = bowl(a, rim_y=50, rx=36, ry=11, depth=30, body=body, surface="#7DB84A", band=band)
    draw(a, cx, cy, rx, ry)


def heap_leaves(a, cx, cy, rx, ry, n, seed, colors=("#7DC04B", "#5FA83A", "#9AD266")):
    g = rnd(seed)
    for _ in range(n):
        x, y = cx + g.uniform(-rx, rx), cy - 2 + g.uniform(-ry, ry * 0.4)
        lettuce_leaf(a, x, y, g.uniform(5, 8), g.uniform(-40, 40), g.choice(colors), g.randint(0, 99))


@art("fattoush")
def _fattoush(a):
    def d(a, cx, cy, rx, ry):
        heap_leaves(a, cx, cy - 2, rx * 0.75, ry * 0.6, 10, 3)
        g = rnd(5)
        for _ in range(6):
            x, y = cx + g.uniform(-rx * 0.7, rx * 0.7), cy - 4 + g.uniform(-ry * 0.6, ry * 0.3)
            pts = rot_pts([(x - 3.4, y - 2.6), (x + 3.4, y - 2.2), (x + 3, y + 2.6), (x - 3.2, y + 2.2)], x, y, g.uniform(0, 90))
            a.fill(poly(pts), "#D9A04E")
            a.fill(poly([(px, py - 0.6) for px, py in pts]), "#EFC27A")
        tomato_cubes(a, cx, cy - 4, rx * 0.6, ry * 0.5, 7, 4)
        cucumber_cubes(a, cx, cy - 4, rx * 0.6, ry * 0.5, 6, 6)
        dots(a, "#7A2232", cx, cy - 4, rx * 0.6, ry * 0.5, 14, 0.5, 8)
        for x, y in [(cx - 12, cy - 9), (cx + 10, cy - 8)]:
            a.fill(blob(x, y, 2.6, 1.6, int(x), 0.2, 6), "#E9474F")
    salad_bowl(a, d)


@art("tabbouleh")
def _tabbouleh(a):
    cx, cy, rx, ry = plate(a)
    for x, y, d in [(24, cy - 2, -60), (76, cy - 3, 60)]:
        a.fill(leaf(x, y, 22, 10, d), "#8CCB55")
        a.stroke(poly(rot_pts([(x - 9, y), (x + 9, y)], x, y, d), closed=False), "#C2E59A", 0.9, 0.8)
    a.fill(mound(50, cy + 2, 52, 14, 5), "#2F7A26")
    a.fill(mound(50, cy, 48, 12.5, 6), "#4A9A34")
    g = rnd(4)
    for _ in range(140):
        t = g.uniform(0, 2 * math.pi)
        k = math.sqrt(g.uniform(0, 1))
        x, y = 50 + math.cos(t) * 21 * k, cy - 6 + math.sin(t) * 6.5 * k
        a.fill(ellipse_rot(x, y, 0.9, 0.5, g.uniform(0, 180)), g.choice(["#2E6E22", "#6DBE45", "#3E8E2E", "#86C653"]))
    tomato_cubes(a, 50, cy - 6, 18, 5, 10, 6, 2.2)
    dots(a, "#F3E3B0", 50, cy - 6, 18, 5, 10, 0.5, 8)


@art("salad_arabic")
def _salad_arabic(a):
    def d(a, cx, cy, rx, ry):
        a.fill(ellipse(cx, cy, rx * 0.95, ry * 0.95), "#E9C9A6")
        tomato_cubes(a, cx, cy - 1, rx * 0.8, ry * 0.65, 22, 3, 3)
        cucumber_cubes(a, cx, cy - 1, rx * 0.8, ry * 0.65, 18, 5, 3)
        leaves(a, PARSLEY, cx, cy - 1, rx * 0.7, ry * 0.5, 8, 7, 1.1)
        dots(a, "#F3EAD2", cx, cy - 1, rx * 0.7, ry * 0.5, 8, 0.6, 9)
    salad_bowl(a, d)


@art("salad_green")
def _salad_green(a):
    def d(a, cx, cy, rx, ry):
        heap_leaves(a, cx, cy - 3, rx * 0.8, ry * 0.7, 16, 5, ("#7DC04B", "#5FA83A", "#9AD266", "#B7E07D"))
        for x, y in [(cx - 6, cy - 7), (cx + 9, cy - 5)]:
            cucumber_slice(a, x, y, 3.6, 2.6)
    salad_bowl(a, d)


@art("salad_caesar")
def _salad_caesar(a):
    def d(a, cx, cy, rx, ry):
        heap_leaves(a, cx, cy - 3, rx * 0.8, ry * 0.7, 14, 9, ("#A6D46E", "#8CCB55", "#C9E89A"))
        g = rnd(4)
        for _ in range(6):
            x, y = cx + g.uniform(-rx * 0.6, rx * 0.6), cy - 5 + g.uniform(-ry * 0.6, ry * 0.3)
            a.fill(rrect(x - 2.4, y - 2.4, 4.8, 4.6, 1.2), "#C98A3A")
            a.fill(rrect(x - 2.4, y - 2.8, 4.8, 4, 1.2), "#E7B05A")
        for _ in range(10):
            x, y = cx + g.uniform(-rx * 0.6, rx * 0.6), cy - 5 + g.uniform(-ry * 0.6, ry * 0.3)
            a.fill(poly([(x - 1.6, y + 0.8), (x, y - 1.2), (x + 1.8, y + 0.6)]), "#FBF2D2")
        a.stroke(smooth([(cx - 16, cy - 6), (cx - 5, cy - 9), (cx + 6, cy - 5), (cx + 15, cy - 8)], closed=False), "#F6EFD8", 1.6, 0.9)
        chicken_strips = [(cx - 8, cy - 9, 20), (cx + 4, cy - 10, -10)]
        for x, y, d in chicken_strips:
            a.fill(capsule(x, y, 11, 4, d), "#E3A65A")
            grill_marks(a, x, y, 3, 8, d + 80, "#8E4E1C", 2, 1, 0.6)
    salad_bowl(a, d)


@art("salad_greek")
def _salad_greek(a):
    def d(a, cx, cy, rx, ry):
        heap_leaves(a, cx, cy - 1, rx * 0.8, ry * 0.5, 8, 2)
        for i, (x, y) in enumerate([(cx - 12, cy - 5), (cx + 10, cy - 6), (cx - 2, cy - 2)]):
            tomato_slice(a, x, y, 4.6, 3.2)
        for x, y in [(cx + 2, cy - 8), (cx - 16, cy)]:
            cucumber_slice(a, x, y, 3.6, 2.6)
        for x, y in [(cx - 4, cy - 9), (cx + 8, cy - 2)]:
            a.fill(rrect(x - 3.4, y - 3.2, 6.8, 6.2, 1), "#E6E0D2")
            a.fill(rrect(x - 3.4, y - 3.6, 6.8, 5.4, 1), "#FBF8F0")
        for x, y in [(cx + 15, cy - 3), (cx - 9, cy - 9), (cx + 4, cy + 1)]:
            a.fill(ellipse(x, y, 2.4, 1.9), "#2E2A24")
            a.fill(ellipse(x - 0.6, y - 0.6, 0.8, 0.5), "#6E6A5E")
        onion_curls(a, cx, cy - 4, rx * 0.5, ry * 0.3, 5, 9, "#A0559E", 1.0)
    salad_bowl(a, d)


def dip_plate(a, color, shade_c, swirl_c, extras):
    cx, cy, rx, ry = plate(a)
    a.fill(ellipse(cx, cy, rx * 0.88, ry * 0.85), shade_c)
    a.fill(ellipse(cx, cy - 1, rx * 0.84, ry * 0.78), color)
    swirl(a, cx, cy - 1, rx * 0.6, ry * 0.55, swirl_c, 1.8, 0.9)
    extras(a, cx, cy - 1, rx, ry)


@art("baba_ghanoush")
def _baba(a):
    def ex(a, cx, cy, rx, ry):
        dots(a, "#7E5A36", cx, cy, rx * 0.6, ry * 0.5, 14, 0.7, 3, 0.7)
        tomato_cubes(a, cx, cy, rx * 0.45, ry * 0.4, 6, 4, 2.2)
        dots(a, "#C2283E", cx, cy, rx * 0.5, ry * 0.45, 8, 0.9, 6)
        leaves(a, PARSLEY, cx, cy, rx * 0.5, ry * 0.4, 6, 8, 1.2)
        a.fill(ellipse(cx + 2, cy, 8, 2.6), OIL, 0.5)
    dip_plate(a, "#C9A475", "#A88454", "#9C7A4E", ex)


@art("mutabbal")
def _mutabbal(a):
    def ex(a, cx, cy, rx, ry):
        a.fill(ellipse(cx + 1, cy, 9, 3), OIL, 0.55)
        dots(a, "#C9452D", cx, cy, rx * 0.5, ry * 0.45, 9, 0.5, 4)
        dots(a, "#C2283E", cx, cy, rx * 0.5, ry * 0.45, 5, 0.9, 7)
        leaves(a, PARSLEY, cx, cy, rx * 0.4, ry * 0.35, 4, 8, 1.2)
    dip_plate(a, "#E3D0AE", "#C9B48F", "#C2AA80", ex)


@art("salad_tahini")
def _salad_tahini(a):
    cx, cy, rx, ry = small_bowl(a, TAHINI, rim_y=54, rx=30, ry=9, depth=24)
    swirl(a, cx, cy, rx * 0.5, ry * 0.5, "#CDB384", 1.2, 0.8)
    leaves(a, PARSLEY, cx, cy, rx * 0.6, ry * 0.5, 9, 3, 1.2)
    dots(a, "#C9452D", cx, cy, rx * 0.4, ry * 0.3, 4, 0.5, 6)
    lemon_wedge(a, 74, 76, 6.5, -15)


@art("pickles")
def _pickles(a):
    ground(a, 28, 88)
    a.fill(rrect(26, 32, 48, 54, 10), GLASS, 0.6)
    a.fill(rrect(28, 40, 44, 44, 9), "#C9C46A", 0.85)
    for i, (x, y, d, col) in enumerate([(38, 58, 80, "#5D8A2E"), (50, 56, 95, "#6E9A35"), (62, 60, 85, "#5D8A2E"),
                                         (44, 72, 100, "#E26A8E"), (58, 74, 80, "#D9557E")]):
        if col.startswith("#E") or col.startswith("#D9"):
            a.fill(rrect(x - 5, y - 6, 10, 12, 2), col)
            a.fill(rrect(x - 3.6, y - 4.6, 4, 9, 1.2), shade(col, 0.3), 0.7)
        else:
            a.fill(capsule(x, y, 24, 8, d), col)
            dots(a, shade(col, 0.35), x, y, 2, 8, 5, 0.6, i + 3)
    a.fill(ellipse(38, 82, 5, 4), "#F6D33C", 0.85)
    a.fill(rrect(30, 42, 4, 36, 2), WHITE, 0.35)
    a.fill(rrect(24, 24, 52, 11, 3), "#D33B2F")
    a.fill(rrect(24, 31, 52, 4, 2), "#A82E24")
