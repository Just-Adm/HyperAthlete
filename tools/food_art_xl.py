"""
Food pictures, part 5: picture templates for the big food list (tools/food_xl_*.py).

Every food there names a template and its colours, e.g. ("glass", "#F39C12", None) or
("pills", "#FFFFFF", "#F2C33A", "soft"). Same flat style as the hand-made pictures: rounded shapes,
three tones per object, soft ground shadow. A per-food seed varies the garnish so similar foods
don't look identical.
"""
import math
import os
import sys
import zlib

from gen_food_art import *  # noqa: F401,F403  (palette, containers, garnish, R, art)
from foodart import (blob, capsule, circle, ellipse, ellipse_rot, mound, poly, rot_pts, rrect, shade, smooth,
                     wedge)
from food_art_more import (bun, fish, grill_marks, leaf, lettuce_leaf, skewer)

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import xlkit  # noqa: E402
import food_xl_1, food_xl_2, food_xl_3, food_xl_4, food_xl_addons  # noqa: E402,F401


def seed_of(name):
    return zlib.crc32(name.encode()) % 997 + 1


# ------------------------------------------------------------------ templates

def t_plate(a, base, accent=None, s=1):
    cx, cy, rx, ry = plate(a)
    a.fill(mound(50, cy + 3, 54, 16, s, 7, 0.2), shade(base, -0.18))
    a.fill(mound(48, cy + 1, 48, 13, s + 1, 7, 0.22), base)
    dots(a, shade(base, 0.2), 48, cy - 5, 16, 4, 10, 1.2, s + 2, 0.7)
    if accent:
        g = rnd(s)
        for _ in range(7):
            x, y = 48 + g.uniform(-18, 18), cy - 4 + g.uniform(-4, 3)
            a.fill(blob(x, y, 3.4, 2.4, g.randint(1, 50), 0.2, 7), accent)
            a.fill(ellipse(x - 1, y - 1, 1.2, 0.7), shade(accent, 0.35), 0.7)
    leaves(a, PARSLEY, 48, cy - 4, 14, 4, 5, s + 3, 1.2)


def t_rice(a, base, accent=None, s=1):
    def top(a, cy):
        if accent:
            g = rnd(s + 5)
            for _ in range(6):
                x, y = 48 + g.uniform(-14, 14), cy - 12 + g.uniform(-4, 3)
                a.fill(blob(x, y, 4.2, 3.2, g.randint(1, 50), 0.2, 8), accent)
                a.fill(ellipse(x - 1, y - 1.2, 1.6, 0.9), shade(accent, 0.35), 0.7)
        leaves(a, PARSLEY, 48, cy - 10, 12, 4, 4, s + 9, 1.2)
    rice_plate(a, base, shade(base, -0.15), shade(base, 0.3), s, top)


def t_pasta(a, noodle, sauce=None, s=1):
    cx, cy, rx, ry = plate(a)
    g = rnd(s)
    a.fill(mound(50, cy + 3, 50, 13, s, 6, 0.2), shade(noodle, -0.2))
    for i in range(22):
        x, y = 50 + g.uniform(-20, 20), cy - 2 + g.uniform(-6, 4)
        a.stroke(smooth([(x - 7, y), (x - 2, y - 3), (x + 3, y + 1), (x + 8, y - 2)], closed=False),
                 shade(noodle, g.uniform(-0.1, 0.12)), 2.2)
    if sauce:
        a.fill(blob(48, cy - 5, 13, 5, s + 2, 0.25, 9), sauce)
        a.fill(blob(45, cy - 6.5, 5, 2, s + 3, 0.2, 7), shade(sauce, 0.3), 0.7)
    leaves(a, PARSLEY, 50, cy - 6, 10, 3, 4, s + 4, 1.1)


def t_stew(a, base, accent=None, s=1):
    cx, cy, rx, ry = bowl(a, rim_y=48, rx=36, ry=10.5, depth=32, surface=base)
    a.fill(ellipse(cx - rx * 0.3, cy - ry * 0.2, rx * 0.3, ry * 0.22), shade(base, 0.25), 0.6)
    if accent:
        g = rnd(s)
        for _ in range(6):
            x, y = cx + g.uniform(-rx * 0.6, rx * 0.6), cy + g.uniform(-ry * 0.4, ry * 0.35)
            a.fill(blob(x, y, 3.6, 2.2, g.randint(1, 50), 0.2, 7), accent)
    leaves(a, PARSLEY, cx, cy, rx * 0.5, ry * 0.4, 4, s + 2, 1.2)


def t_soup(a, broth, garnish=None, s=1):
    cx, cy, rx, ry = bowl(a, rim_y=48, rx=36, ry=10.5, depth=32, surface=broth)
    a.fill(ellipse(cx - rx * 0.3, cy - ry * 0.2, rx * 0.3, ry * 0.22), shade(broth, 0.25), 0.6)
    if garnish:
        dots(a, garnish, cx, cy, rx * 0.6, ry * 0.5, 9, 1.2, s)
    a.fill(ellipse(cx + 9, cy, 7, 2), WHITE, 0.35)
    leaves(a, PARSLEY, cx, cy, rx * 0.4, ry * 0.35, 4, s + 2, 1.1)


def t_bowl(a, surface, accent=None, s=1):
    cx, cy, rx, ry = bowl(a, rim_y=50, rx=34, ry=10, depth=30, surface=surface)
    a.fill(ellipse(cx - rx * 0.25, cy - ry * 0.25, rx * 0.35, ry * 0.25), shade(surface, 0.2), 0.7)
    if accent:
        g = rnd(s)
        if g.random() < 0.5:
            swirl(a, cx, cy, rx * 0.5, ry * 0.45, accent, 1.8, 0.85)
        else:
            dots(a, accent, cx, cy, rx * 0.55, ry * 0.45, 9, 1.5, s)


def t_pan(a, surface, accent=None, s=1):
    cx, cy, rx, ry = pan(a, surface)
    a.fill(blob(cx - 4, cy - 1, rx * 0.6, ry * 0.55, s, 0.2, 10), shade(surface, 0.12))
    if accent:
        g = rnd(s)
        for _ in range(6):
            x, y = cx + g.uniform(-rx * 0.6, rx * 0.6), cy + g.uniform(-ry * 0.45, ry * 0.4)
            a.fill(blob(x, y, 3.6, 2.4, g.randint(1, 50), 0.2, 7), accent)
    leaves(a, PARSLEY, cx, cy, rx * 0.5, ry * 0.4, 4, s + 2, 1.1)


def t_meat(a, color, s=1):
    cx, cy, rx, ry = plate(a)
    g = rnd(s)
    for i, (x, y) in enumerate([(38, cy - 3), (60, cy - 4), (49, cy + 3)]):
        w, h = g.uniform(14, 18), g.uniform(7, 9)
        a.fill(blob(x, y + 1.6, w, h, s + i, 0.12, 9), shade(color, -0.3))
        a.fill(blob(x, y, w, h, s + i, 0.12, 9), color)
        a.fill(blob(x - 3, y - 2, w * 0.45, h * 0.35, s + i + 7, 0.2, 7), shade(color, 0.25), 0.7)
        grill_marks(a, x, y, w * 1.4, h * 1.2, -20, shade(color, -0.55), 3, 1.3, 0.55)
    leaves(a, PARSLEY, 70, cy + 6, 5, 2, 5, s + 3, 1.2)


def t_skewer(a, color, s=1):
    cx, cy, rx, ry = plate(a)
    for k, dy in enumerate([-6, 5]):
        x0, y0, x1, y1 = 16, cy + dy + 6, 84, cy + dy - 6
        skewer(a, x0, y0, x1, y1)
        for i in range(6):
            t = 0.17 + i * 0.13
            x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            if (i + k) % 3 == 2:
                a.fill(rrect(x - 2, y - 4.2, 4, 8.4, 1.6), "#3E8E2E" if k else "#D9392B")
            else:
                a.fill(rrect(x - 4.4, y - 4, 8.8, 8.6, 2.6), shade(color, -0.2))
                a.fill(rrect(x - 4.4, y - 4.6, 8.8, 8, 2.6), color)
                a.fill(ellipse(x - 1.6, y - 2.4, 1.8, 1), shade(color, 0.35), 0.7)


def t_fish(a, body, s=1):
    cx, cy, rx, ry = plate(a)
    fish(a, 48, cy - 3, 58, body, shade(body, 0.35), -6, marks=shade(body, -0.5))
    lemon_wedge(a, 30, cy + 7, 4.6, 10)
    leaves(a, PARSLEY, 70, cy + 6, 6, 2, 6, s, 1.3)


def t_sandwich(a, bread, filling, s=1):
    ground(a, 38, 86)
    d = -8
    pts = lambda P: rot_pts(P, 50, 60, d)
    a.fill(smooth(pts([(14, 66), (20, 74), (50, 77), (80, 74), (86, 66), (50, 70)]), tension=0.7), shade(bread, -0.15))
    a.fill(smooth(pts([(14, 64), (50, 68), (86, 64), (84, 58), (50, 61), (16, 58)]), tension=0.7), filling)
    a.fill(smooth(pts([(16, 60), (30, 56), (44, 61), (58, 55), (72, 60), (84, 56), (84, 60), (16, 62)]), tension=0.6), "#7DC04B")
    for x in (32, 52, 70):
        cx_, cy_ = pts([(x, 60)])[0]
        tomato_slice(a, cx_, cy_ - 1, 6, 1.8)
    top = pts([(14, 58), (18, 42), (34, 34), (50, 32), (66, 34), (82, 42), (86, 58), (50, 54)])
    a.fill(smooth(top, tension=0.75), bread)
    a.fill(smooth(pts([(26, 46), (40, 38), (56, 37), (48, 42), (32, 48)]), tension=0.7), shade(bread, 0.3), 0.8)
    for x in (34, 50, 66):
        p = pts([(x - 4, 40), (x + 4, 44)])
        a.stroke(poly(p, closed=False), shade(bread, -0.25), 1.4, 0.7)


def t_wrap(a, wrap, filling, s=1):
    ground(a, 26, 88)
    d = -22
    cx, cy = 50, 52
    R_ = lambda P: rot_pts(P, cx, cy, d)
    a.fill(smooth(R_([(cx - 13, cy - 28), (cx, cy - 34), (cx + 13, cy - 28), (cx + 13.5, cy + 30), (cx, cy + 34),
                      (cx - 13.5, cy + 30)]), tension=0.6), wrap)
    top = R_([(cx - 11, cy - 28), (cx - 6, cy - 37), (cx + 2, cy - 39), (cx + 9, cy - 35), (cx + 11, cy - 28)])
    a.fill(smooth(top + R_([(cx, cy - 25)])), filling)
    for i, (x, y) in enumerate(R_([(cx - 5, cy - 33), (cx + 4, cy - 34), (cx, cy - 30)])):
        a.fill(blob(x, y, 3.2, 2, i + s, 0.25, 6), shade(filling, -0.15))
    for x, y in R_([(cx - 8, cy - 31), (cx + 8, cy - 30)]):
        a.fill(ellipse_rot(x, y, 2.4, 1.3, d + 30), "#7FA63A")
    a.fill(capsule(*R_([(cx - 6, cy - 6)])[0], 40, 3.4, d + 90), shade(wrap, 0.25), 0.8)
    paper = R_([(cx - 14.5, cy + 2), (cx + 14.5, cy - 4), (cx + 14.5, cy + 31), (cx, cy + 35.5), (cx - 14.5, cy + 31)])
    a.fill(smooth(paper, tension=0.35), WHITE)
    a.fill(smooth(R_([(cx + 4, cy - 1.5), (cx + 14.5, cy - 4), (cx + 14.5, cy + 31), (cx + 6, cy + 34)]), tension=0.35), "#E9EBEF")
    a.fill(poly(R_([(cx - 14.5, cy + 9), (cx + 14.5, cy + 3), (cx + 14.5, cy + 7), (cx - 14.5, cy + 13)])), "#E2483B")


def t_burger(a, bun_c, patty, s=1):
    ground(a, 34, 88)
    a.fill(rrect(22, 74, 56, 10, 5), shade(bun_c, -0.12))
    a.fill(rrect(22, 72, 56, 9, 4.5), shade(bun_c, 0.1))
    a.fill(smooth([(19, 68), (50, 64), (81, 68), (78, 74), (50, 76), (22, 74)]), shade(patty, -0.25))
    a.fill(smooth([(20, 67), (50, 63), (80, 67), (77, 72), (50, 74), (23, 72)]), patty)
    dots(a, shade(patty, -0.35), 50, 69, 26, 3, 14, 0.6, s)
    a.fill(poly([(20, 63), (80, 63), (78, 66), (64, 70), (58, 66), (44, 71), (36, 66), (22, 66)]), "#F6B92E")
    tomato_slice(a, 38, 61, 10, 2.6)
    tomato_slice(a, 62, 61, 10, 2.6)
    lettuce_leaf(a, 36, 58, 13, -4, "#8CCB55", 3)
    lettuce_leaf(a, 62, 58, 13, 4, "#7DC04B", 7)
    bun(a, 50, 28, 62, 28, bun_c)


def t_pizza(a, crust, top, s=1):
    ground(a, 34, 86)
    tip, l, r = (50, 84), (20, 30), (82, 30)
    a.fill(smooth([tip, (l[0] - 1, l[1] + 2), (50, l[1] - 6), (r[0] + 1, r[1] + 2)], tension=0.25), shade(crust, -0.2))
    a.fill(poly([tip, (l[0] + 3, l[1] + 5), (r[0] - 3, r[1] + 5)]), "#F7CD5E")
    a.fill(poly([(50, 76), (28, 38), (72, 38)]), "#FBE08A", 0.5)
    a.fill(capsule(51, 32, 64, 9, -2), crust)
    a.fill(capsule(48, 30.5, 40, 2.6, -2), shade(crust, 0.3), 0.7)
    g = rnd(s)
    for _ in range(6):
        y = g.uniform(42, 70)
        half = (84 - y) / (84 - 35) * 26
        x = 50 + g.uniform(-half * 0.7, half * 0.7)
        a.fill(ellipse(x, y + 0.5, 4.2, 3.0), shade(top, -0.25))
        a.fill(ellipse(x, y, 4.0, 2.8), top)
    leaves(a, "#3E8E2E", 50, 52, 12, 10, 4, s + 2, 1.3)


def t_pie(a, dough, filling, s=1):
    ground(a, 34, 86)
    for k, (cx, cy, d) in enumerate([(38, 62, -8), (62, 58, 10)]):
        tri = rot_pts([(cx - 17, cy + 13), (cx + 17, cy + 13), (cx, cy - 17)], cx, cy, d)
        a.fill(smooth([(x, y + 2) for x, y in tri], tension=0.35), shade(dough, -0.22))
        a.fill(smooth(tri, tension=0.35), dough)
        mid = rot_pts([(cx - 7, cy + 2), (cx + 7, cy + 2), (cx, cy - 7)], cx, cy, d)
        a.fill(smooth(mid, tension=0.5), filling)
        a.fill(ellipse_rot(*rot_pts([(cx - 6, cy + 6)], cx, cy, d)[0], 5, 1.8, d), shade(dough, 0.3), 0.7)
        sesame(a, cx, cy + 6, 9, 3, 5, s + k)


def t_bread(a, color, s=1):
    ground(a, 36, 84)
    a.fill(blob(50, 66, 36, 16, s, 0.06, 12), shade(color, -0.2))
    a.fill(blob(50, 62, 35, 16, s, 0.06, 12), color)
    a.fill(blob(44, 56, 20, 6, s + 2, 0.15, 9), shade(color, 0.3), 0.7)
    for i in range(4):
        x = 34 + i * 10
        a.stroke(smooth([(x - 3, 66), (x, 58), (x + 3, 52)], closed=False), shade(color, -0.3), 1.6, 0.7)


def t_glass(a, liquid, foam=None, ice=False, s=1):
    ly = glass_tumbler(a, liquid, 0.78, foam=foam, ice=bool(ice))
    a.fill(capsule(60, 24, 30, 2.6, 75), "#FFFFFF")
    a.fill(capsule(60, 24, 30, 1.0, 75), "#E2483B", 0.8)


def t_mug(a, liquid, foam=None, s=1):
    saucer(a, 82, 32, 7)
    cx, cy, rx, ry = mug(a, WHITE, liquid, foam=foam)
    if foam:
        swirl(a, cx, cy, rx * 0.5, ry * 0.5, shade(liquid, 0.2), 1.2, 0.8)


def t_cup(a, liquid, s=1):
    ground(a, 22, 88)
    a.fill(poly([(34, 26), (66, 26), (62, 86), (38, 86)]), WHITE)
    a.fill(poly([(54, 26), (66, 26), (62, 86), (54, 86)]), "#E9EBEF")
    a.fill(poly([(35, 46), (65, 46), (63.5, 66), (36.5, 66)]), shade(liquid, -0.05))
    a.fill(poly([(35, 46), (48, 46), (48, 66), (36.5, 66)]), shade(liquid, 0.15), 0.6)
    a.fill(rrect(30, 18, 40, 9, 3), "#F2F3F5")
    a.fill(rrect(32, 14, 36, 6, 3), shade("#F2F3F5", -0.08))
    a.fill(circle(50, 54, 4), WHITE, 0.6)


def t_can(a, color, accent=None, s=1):
    can_(a, color, accent)


def t_bottle(a, liquid, cap, s=1):
    bottle(a, liquid, cap, label=WHITE if liquid not in ("#DCEAF5",) else None)


def t_fruit(a, color, leaf_c=None, s=1):
    ground(a, 28, 86)
    pts = [(50, 36), (62, 34), (73, 44), (75, 60), (68, 76), (50, 82), (32, 76), (25, 60), (27, 44), (38, 34)]
    a.fill(smooth([(x, y + 1.6) for x, y in pts]), shade(color, -0.2))
    a.fill(smooth(pts), color)
    a.fill(smooth([(56, 40), (70, 46), (72, 62), (64, 76), (58, 70), (64, 56)]), shade(color, -0.12), 0.7)
    a.fill(ellipse_rot(37, 50, 4, 9, 25), shade(color, 0.4), 0.8)
    if leaf_c:
        a.stroke(smooth([(50, 38), (51, 30), (54, 24)], closed=False), "#6A4122", 2.2)
        a.fill(leaf(61, 28, 14, 7, -25), leaf_c)


def t_long(a, color, s=1):
    ground(a, 40, 84)
    for i, (x, y, d) in enumerate([(48, 58, -10), (54, 70, 6)]):
        a.fill(capsule(x, y + 1.6, 62, 13, d), shade(color, -0.25))
        a.fill(capsule(x, y, 62, 12, d), color)
        a.fill(capsule(x - 3, y - 2.4, 40, 3, d), shade(color, 0.35), 0.7)
        tx, ty = rot_pts([(x - 31, y)], x, y, d)[0]
        a.fill(ellipse_rot(tx - 1, ty, 3.4, 4, d), "#4E7A2B")


def t_leafy(a, color, s=1):
    ground(a, 30, 86)
    for i, (x, y, r, k) in enumerate([(30, 66, 16, -0.12), (70, 66, 16, -0.12), (50, 70, 18, 0), (38, 52, 15, 0.1),
                                      (62, 52, 15, 0.1), (50, 46, 14, 0.25), (50, 60, 14, 0.35)]):
        col = shade(color, k)
        a.fill(blob(x, y, r, r * 0.82, i + s, 0.18, 12), col)
        a.stroke(smooth([(x - r * 0.5, y + r * 0.3), (x, y - r * 0.1), (x + r * 0.4, y - r * 0.5)], closed=False), shade(col, 0.4), 1.0, 0.7)


def t_pile(a, c1, c2=None, s=1):
    cx, cy, rx, ry = small_bowl(a, shade(c1, -0.2), rim_y=60, rx=30, ry=8.5, depth=22)
    g = rnd(s)
    pts = []
    for _ in range(30):
        t = g.uniform(0, 2 * math.pi)
        k = math.sqrt(g.uniform(0, 1))
        x, y = cx + math.cos(t) * rx * 0.85 * k, cy - 4 + math.sin(t) * ry * 0.9 * k - (1 - k) * 6
        pts.append((y, x))
    for y, x in sorted(pts):
        col = c1 if g.random() < 0.6 or not c2 else c2
        a.fill(blob(x, y + 0.8, 3.6, 2.6, g.randint(1, 90), 0.2, 7), shade(col, -0.25))
        a.fill(blob(x, y, 3.4, 2.4, g.randint(1, 90), 0.2, 7), col)
        a.fill(ellipse(x - 1, y - 0.8, 1.1, 0.6), shade(col, 0.4), 0.7)


def t_bar(a, wrapper, accent=None, s=1):
    ground(a, 36, 82)
    d = -16
    R_ = lambda P: rot_pts(P, 50, 58, d)
    a.fill(poly(R_([(14, 50), (86, 50), (86, 68), (14, 68)])), shade(wrapper, -0.2))
    a.fill(poly(R_([(14, 48), (86, 48), (86, 66), (14, 66)])), wrapper)
    for x in (14, 86):
        zig = [(x, 48 + i * 3) for i in range(7)]
        zz = [(x + (2 if (i % 2) else -2) * (1 if x < 50 else -1), y) for i, (x, y) in enumerate(zig)]
        a.fill(poly(R_(zz + [(x, 66), (x, 48)])), shade(wrapper, -0.1))
    if accent:
        a.fill(poly(R_([(30, 52), (70, 52), (70, 62), (30, 62)])), accent)
    a.fill(poly(R_([(18, 50), (82, 50), (82, 52.5), (18, 52.5)])), WHITE, 0.3)
    # a piece next to it
    a.fill(rrect(60, 72, 22, 9, 2.5), "#5B3321")
    a.fill(rrect(60, 72, 22, 3, 1.5), "#7A4A2E")


def t_cake(a, sponge, frosting=None, s=1):
    ground(a, 34, 86)
    frost = frosting or shade(sponge, 0.35)
    tip, l, r = (24, 70), (70, 52), (78, 78)
    a.fill(poly([(26, 54), (72, 38), (80, 46), (80, 80), (26, 74)]), shade(sponge, -0.15))
    a.fill(poly([(26, 56), (72, 40), (72, 74), (26, 74)]), sponge)
    for y in (62, 68):
        a.fill(poly([(26, y), (72, y - 10 + (y - 62)), (72, y - 8 + (y - 62)), (26, y + 2)]), frost, 0.9)
    a.fill(poly([(24, 54), (70, 37), (82, 45), (74, 41), (26, 57)]), frost)
    a.fill(smooth([(24, 53), (48, 44), (72, 36), (82, 44), (70, 40), (48, 48), (26, 57)], tension=0.4), frost)
    a.fill(circle(66, 36, 3.6), "#E8453C")
    a.fill(circle(65, 35, 1.2), WHITE, 0.6)


def t_cookie(a, color, chips=None, s=1):
    ground(a, 36, 84)
    for k, (cx, cy, r) in enumerate([(38, 66, 18), (62, 58, 18)]):
        a.fill(blob(cx, cy + 2, r, r * 0.55, s + k, 0.06, 12), shade(color, -0.25))
        a.fill(blob(cx, cy, r, r * 0.55, s + k, 0.06, 12), color)
        a.fill(blob(cx - 4, cy - 3, r * 0.5, r * 0.2, s + k + 4, 0.15, 9), shade(color, 0.3), 0.7)
        if chips:
            dots(a, chips, cx, cy, r * 0.65, r * 0.32, 7, 1.6, s + k + 9)


def t_cup_dessert(a, color, top=None, s=1):
    ground(a, 24, 88)
    a.fill(poly([(32, 40), (68, 40), (63, 86), (37, 86)]), GLASS, 0.55)
    a.fill(poly([(33.5, 48), (66.5, 48), (62.5, 85), (37.5, 85)]), color)
    a.fill(ellipse(50, 48, 16.5, 3), shade(color, 0.2))
    if top:
        a.fill(blob(50, 46, 12, 3.4, s, 0.2, 9), top)
        dots(a, shade(top, 0.25), 50, 45, 9, 2, 6, 0.8, s + 3)
    a.fill(poly([(34, 42), (37, 42), (39, 84), (37.5, 84)]), WHITE, 0.45)
    a.fill(ellipse(50, 40, 18, 3.2), GLASS, 0.8)


def t_icecream(a, scoop, cone=False, s=1):
    ground(a, 26, 88)
    if cone:
        a.fill(poly([(36, 48), (64, 48), (50, 88)]), "#D9A14E")
        for i in range(4):
            a.stroke(poly([(38 + i * 7, 48), (50 + i * 2, 84)], closed=False), "#B8743A", 1.1, 0.8)
        for i in range(3):
            a.stroke(poly([(62 - i * 7, 48), (50 - i * 2, 84)], closed=False), "#B8743A", 1.1, 0.8)
        a.fill(blob(50, 40, 18, 15, s, 0.08, 11), shade(scoop, -0.12))
        a.fill(blob(49, 38, 17, 14, s, 0.08, 11), scoop)
        a.fill(ellipse(43, 32, 5, 3), shade(scoop, 0.35), 0.8)
    else:
        a.fill(poly([(26, 58), (74, 58), (68, 86), (32, 86)]), "#E2483B")
        a.fill(poly([(26, 58), (74, 58), (73, 62), (27, 62)]), shade("#E2483B", -0.15))
        for k, (x, y) in enumerate([(40, 50), (60, 50), (50, 40)]):
            a.fill(blob(x, y + 1, 13, 10, s + k, 0.1, 10), shade(scoop, -0.15))
            a.fill(blob(x, y, 13, 10, s + k, 0.1, 10), scoop)
            a.fill(ellipse(x - 4, y - 3, 4, 2.2), shade(scoop, 0.35), 0.8)


def t_cheese(a, color, s=1):
    cheese_slices(a, color)


def t_tub(a, color, label, s=1):
    tub(a, color, shade(color, -0.25) if color != "#FFFFFF" else "#2E2E2E", label)
    a.fill(rrect(36, 58, 28, 4, 2), WHITE, 0.6)


def t_pills(a, bottle_c, pill, kind="tab", s=1):
    ground(a, 34, 88)
    # bottle on the left
    a.fill(rrect(20, 34, 32, 50, 7), bottle_c)
    a.fill(rrect(36, 34, 16, 50, 6), shade(bottle_c, -0.12))
    a.fill(rrect(18, 26, 36, 11, 3), "#FFFFFF" if bottle_c != "#FFFFFF" else "#2E86DE")
    a.fill(rrect(18, 33, 36, 3, 1.5), "#D7DADF")
    a.fill(rrect(23, 48, 26, 18, 3), WHITE if bottle_c != "#FFFFFF" else shade(pill, -0.1))
    a.fill(rrect(27, 53, 18, 3, 1.5), shade(pill, -0.2), 0.8)
    # a few pills in front
    pos = [(64, 78, 0), (74, 72, 30), (78, 82, -20), (62, 66, 50)]
    for i, (x, y, d) in enumerate(pos):
        if kind == "cap":
            a.fill(capsule(x, y + 1, 16, 7, d), shade(pill, -0.3), 0.6)
            a.fill(capsule(x, y, 16, 7, d), pill)
            hx, hy = rot_pts([(x + 4, y)], x, y, d)[0]
            a.fill(capsule(hx, hy, 8, 7, d), shade(bottle_c, -0.05) if bottle_c != "#FFFFFF" else "#2E86DE")
        elif kind == "soft":
            a.fill(ellipse_rot(x, y + 1, 8, 5, d), shade(pill, -0.3), 0.5)
            a.fill(ellipse_rot(x, y, 8, 5, d), pill, 0.92)
            a.fill(ellipse_rot(x - 2, y - 1.4, 3, 1.4, d), WHITE, 0.6)
        elif kind == "gummy":
            a.fill(blob(x, y, 6, 5.4, i + s, 0.2, 8), pill, 0.95)
            a.fill(circle(x - 2, y - 3.6, 2.4), pill)
            a.fill(circle(x + 2, y - 3.6, 2.4), pill)
        else:
            a.fill(ellipse(x, y + 1.2, 7, 4.2), shade(pill, -0.3), 0.6)
            a.fill(ellipse(x, y, 7, 4.2), pill if pill != "#FFFFFF" else "#F6F7F9")
            a.stroke(poly([(x - 4.5, y), (x + 4.5, y)], closed=False), shade(pill if pill != "#FFFFFF" else "#C9CED6", -0.2), 0.8, 0.8)


def t_sachet(a, color, accent=None, s=1):
    ground(a, 30, 86)
    a.fill(rrect(26, 26, 48, 58, 4), shade(color, -0.15))
    a.fill(rrect(26, 26, 44, 58, 4), color)
    zig = [(26 + i * 4, 26 + (0 if i % 2 else -3)) for i in range(12)]
    a.fill(poly(zig + [(70, 30), (26, 30)]), shade(color, -0.1))
    a.fill(rrect(32, 44, 32, 22, 3), accent or WHITE)
    a.fill(rrect(36, 50, 24, 3, 1.5), color, 0.6)
    a.fill(rrect(36, 56, 16, 3, 1.5), color, 0.4)


def t_jar(a, content, lid, s=1):
    jar(a, content, lid, WHITE, label_color=shade(content, -0.1))


def t_chips(a, bag, accent=None, s=1):
    ground(a, 34, 88)
    body = [(24, 24), (76, 24), (80, 50), (76, 86), (24, 86), (20, 50)]
    a.fill(smooth(body, tension=0.45), bag)
    a.fill(smooth([(56, 24), (76, 24), (80, 50), (76, 86), (60, 86), (64, 50)], tension=0.45), shade(bag, -0.12))
    for y in (24, 86):
        zig = [(24 + i * 4.3, y + (2 if i % 2 else -2)) for i in range(13)]
        a.fill(poly(zig + [(76, y), (24, y)]), shade(bag, -0.2))
    if accent:
        a.fill(blob(48, 56, 16, 12, s, 0.15, 9), accent)
    g = rnd(s)
    for _ in range(4):
        x, y = 50 + g.uniform(-10, 10), 56 + g.uniform(-6, 6)
        a.fill(blob(x, y, 4.8, 3.6, g.randint(1, 60), 0.25, 7), "#F2C33A")
    a.fill(capsule(32, 50, 40, 4, 90), WHITE, 0.3)


def t_box(a, color, accent=None, s=1):
    ground(a, 34, 88)
    a.fill(poly([(26, 22), (66, 22), (74, 30), (74, 86), (26, 86)]), color)
    a.fill(poly([(66, 22), (74, 30), (74, 86), (66, 86)]), shade(color, -0.2))
    a.fill(rrect(30, 36, 32, 30, 4), WHITE, 0.9)
    a.fill(blob(46, 54, 11, 6, s, 0.15, 9), accent or "#F2C33A")
    dots(a, shade(accent or "#F2C33A", -0.2), 46, 52, 9, 4, 6, 1.4, s)
    a.fill(rrect(30, 26, 32, 6, 2), accent or WHITE)


TEMPLATES = {
    "plate": t_plate, "rice": t_rice, "pasta": t_pasta, "stew": t_stew, "soup": t_soup, "bowl": t_bowl, "pan": t_pan,
    "meat": t_meat, "skewer": t_skewer, "fish": t_fish, "sandwich": t_sandwich, "wrap": t_wrap, "burger": t_burger,
    "pizza": t_pizza, "pie": t_pie, "bread": t_bread, "glass": t_glass, "mug": t_mug, "cup": t_cup, "can": t_can,
    "bottle": t_bottle, "fruit": t_fruit, "long": t_long, "leafy": t_leafy, "pile": t_pile, "bar": t_bar,
    "cake": t_cake, "cookie": t_cookie, "cup_dessert": t_cup_dessert, "icecream": t_icecream, "cheese": t_cheese,
    "tub": t_tub, "pills": t_pills, "sachet": t_sachet, "jar": t_jar, "chips": t_chips, "box": t_box,
}


def _register(name, look):
    kind, args = look[0], look[1:]
    fn = TEMPLATES[kind]
    s = seed_of(name)

    def draw(a, fn=fn, args=args, s=s):
        fn(a, *args, s=s)
    R[name] = draw


for x in xlkit.XL:
    look = x["look"]
    if look and look[0] != "same":
        _register(x["id"], look)
for ad in xlkit.XL_ADDONS:
    if ad["look"]:
        _register("addon_" + ad["id"], ad["look"])
