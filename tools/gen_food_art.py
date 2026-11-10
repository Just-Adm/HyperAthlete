#!/usr/bin/env python3
"""
Food and add-on pictures for Hyper Athlete (brief §11: "صورة توضيحية بستايل التطبيق").

    python3 tools/gen_food_art.py                 # writes app/src/main/res/drawable/food_*.xml
    python3 tools/gen_food_art.py --preview sheet.png [prefix]

Each picture is composed from small building blocks (plate, bowl, glass, garnish …) in foodart.py.
"""
import json, math, os, random, sys

sys.path.insert(0, os.path.dirname(__file__))
from foodart import (Art, blob, capsule, circle, ellipse, ellipse_rot, mound, poly, rot_pts, rrect, shade, smooth,
                     to_svg, to_vector, wedge)

# ------------------------------------------------------------------ palette
WHITE = "#FFFFFF"
PLATE_RIM = "#E4E6EB"
PLATE_WELL = "#F4F5F8"
SHADOW = "#1A1A1A"
CERAMIC = "#F2ECE2"
CERAMIC_SHADE = "#DED3C2"
CLAY = "#C9773F"
RICE = "#FFF4DC"
RICE_YELLOW = "#F3C566"
CHICKEN = "#D98A3D"
MEAT = "#8E4B2E"
TOMATO = "#E8453C"
CUCUMBER = "#6DBE45"
PARSLEY = "#3E9A3A"
OIL = "#C9B23A"
BREAD = "#E6B565"
YOLK = "#FDB813"
YOGURT = "#FAF7EF"
HUMMUS = "#EBCB8F"
TAHINI = "#E7D2A6"
CHOC = "#5B3321"
SYRUP = "#E9A23B"
PISTACHIO = "#8DB255"
GLASS = "#DCEAF5"


def rnd(seed):
    return random.Random(seed)


# ------------------------------------------------------------------ containers & bases

def ground(a, rx=36, cy=89, alpha=0.09):
    a.fill(ellipse(50, cy, rx, 4.2), SHADOW, alpha)


def plate(a, cy=64, rx=45, ry=21, color=WHITE):
    ground(a, rx - 4, cy + ry + 3)
    a.fill(ellipse(50, cy + 2.2, rx, ry), shade(PLATE_RIM, -0.06))
    a.fill(ellipse(50, cy, rx, ry), color)
    a.fill(ellipse(50, cy + 0.6, rx * 0.76, ry * 0.7), PLATE_RIM if color == WHITE else shade(color, -0.08), 0.65)
    a.fill(ellipse(50, cy + 1.6, rx * 0.74, ry * 0.66), PLATE_WELL if color == WHITE else shade(color, 0.05))
    return (50, cy, rx * 0.72, ry * 0.64)


def tray(a, cy=64, rx=44, ry=20, metal="#B9BEC6"):
    ground(a, rx - 3, cy + ry + 3)
    a.fill(ellipse(50, cy + 3, rx, ry), shade(metal, -0.25))
    a.fill(ellipse(50, cy, rx, ry), metal)
    a.fill(ellipse(50, cy + 1.2, rx * 0.9, ry * 0.86), shade(metal, -0.12))
    return (50, cy + 1.2, rx * 0.88, ry * 0.84)


def bowl(a, rim_y=50, rx=35, ry=10, depth=34, body=CERAMIC, surface="#F0D9A8", band=None):
    """Side-view bowl. Returns the visible food surface ellipse (cx, cy, rx, ry)."""
    ground(a, rx * 0.8, rim_y + depth + 3)
    body_d = (f"M{50 - rx} {rim_y}C{50 - rx} {rim_y + depth * 0.7} {50 - rx * 0.55} {rim_y + depth} 50 {rim_y + depth}"
              f"C{50 + rx * 0.55} {rim_y + depth} {50 + rx} {rim_y + depth * 0.7} {50 + rx} {rim_y}Z")
    a.fill(body_d, body)
    a.fill(f"M{50 + rx * 0.2} {rim_y + depth * 0.98}C{50 + rx * 0.7} {rim_y + depth * 0.9} {50 + rx} {rim_y + depth * 0.6} {50 + rx} {rim_y}"
           f"L{50 + rx * 0.82} {rim_y}C{50 + rx * 0.8} {rim_y + depth * 0.55} {50 + rx * 0.55} {rim_y + depth * 0.85} {50 + rx * 0.2} {rim_y + depth * 0.98}Z",
           shade(body, -0.12))
    if band:
        a.fill(f"M{50 - rx * 0.97} {rim_y + 6}C{50 - rx * 0.9} {rim_y + 11} {50 + rx * 0.9} {rim_y + 11} {50 + rx * 0.97} {rim_y + 6}"
               f"L{50 + rx * 0.93} {rim_y + 10}C{50 + rx * 0.8} {rim_y + 15} {50 - rx * 0.8} {rim_y + 15} {50 - rx * 0.93} {rim_y + 10}Z", band)
    a.fill(ellipse(50, rim_y, rx, ry), shade(body, 0.35))
    a.fill(ellipse(50, rim_y + 0.8, rx * 0.9, ry * 0.8), surface)
    return (50, rim_y + 0.8, rx * 0.9, ry * 0.8)


def small_bowl(a, surface, cx=50, rim_y=58, rx=24, ry=7, depth=20, body=CERAMIC):
    ground(a, rx * 0.8, rim_y + depth + 3)
    body_d = (f"M{cx - rx} {rim_y}C{cx - rx} {rim_y + depth * 0.7} {cx - rx * 0.55} {rim_y + depth} {cx} {rim_y + depth}"
              f"C{cx + rx * 0.55} {rim_y + depth} {cx + rx} {rim_y + depth * 0.7} {cx + rx} {rim_y}Z")
    a.fill(body_d, body)
    a.fill(ellipse(cx, rim_y, rx, ry), shade(body, 0.35))
    a.fill(ellipse(cx, rim_y + 0.6, rx * 0.88, ry * 0.76), surface)
    return (cx, rim_y + 0.6, rx * 0.88, ry * 0.76)


def pan(a, surface, cy=60, rx=38, ry=16, handle=True, metal="#3A3D44"):
    ground(a, rx, cy + ry + 6)
    if handle:
        a.fill(capsule(88, cy - 2, 22, 6, -12), shade(metal, 0.1))
    a.fill(ellipse(50, cy + 4, rx, ry), shade(metal, -0.2))
    a.fill(ellipse(50, cy, rx, ry), metal)
    a.fill(ellipse(50, cy + 0.8, rx * 0.88, ry * 0.82), surface)
    return (50, cy + 0.8, rx * 0.88, ry * 0.82)


def glass_tumbler(a, liquid, level=0.75, top=22, bottom=86, w_top=36, w_bot=28, foam=None, ice=False):
    ground(a, w_bot * 0.7, bottom + 2)
    lt, rt = 50 - w_top / 2, 50 + w_top / 2
    lb, rb = 50 - w_bot / 2, 50 + w_bot / 2
    a.fill(poly([(lt, top), (rt, top), (rb, bottom), (lb, bottom)]), GLASS, 0.55)
    ly = bottom - (bottom - top) * level
    k = (ly - top) / (bottom - top)
    ll, lr = lt + (lb - lt) * k, rt + (rb - rt) * k
    a.fill(poly([(ll + 1, ly), (lr - 1, ly), (rb - 1, bottom - 1), (lb + 1, bottom - 1)]), liquid)
    a.fill(ellipse(50, ly, (lr - ll) / 2 - 1, 3), shade(liquid, 0.18))
    if foam:
        a.fill(ellipse(50, ly - 1.5, (lr - ll) / 2 - 1, 4), foam)
    if ice:
        for i, (x, y) in enumerate([(44, ly + 4), (55, ly + 6)]):
            a.fill(rrect(x - 4, y - 4, 8, 8, 2), WHITE, 0.55)
    a.fill(poly([(lt + 3, top + 3), (lt + 7, top + 3), (lb + 6, bottom - 4), (lb + 3, bottom - 4)]), WHITE, 0.45)
    a.fill(ellipse(50, top, w_top / 2, 3.2), GLASS, 0.8)
    a.stroke(ellipse(50, top, w_top / 2, 3.2), shade(GLASS, -0.2), 1)
    return ly


def mug(a, body, liquid, cx=46, top=34, w=40, h=46, foam=None):
    ground(a, w * 0.6, top + h + 3, 0.1)
    a.stroke(ellipse(cx + w / 2 + 3, top + h * 0.45, 8, 11), shade(body, -0.08), 5)
    a.fill(rrect(cx - w / 2, top, w, h, 9), body)
    a.fill(rrect(cx + w * 0.2, top, w * 0.3, h, 9), shade(body, -0.07))
    a.fill(ellipse(cx, top, w / 2, 5.5), shade(body, -0.15))
    a.fill(ellipse(cx, top + 0.8, w / 2 - 2.5, 4.2), foam or liquid)
    return (cx, top + 0.8, w / 2 - 2.5, 4.2)


def saucer(a, cy=80, rx=30, ry=7, color=WHITE):
    ground(a, rx * 0.9, cy + ry + 2)
    a.fill(ellipse(50, cy + 1.5, rx, ry), shade(color, -0.12))
    a.fill(ellipse(50, cy, rx, ry), color)


def can_(a, color, accent, top=20, bottom=86, w=34, label=None):
    ground(a, w * 0.7, bottom + 2)
    a.fill(rrect(50 - w / 2, top, w, bottom - top, 5), color)
    a.fill(rrect(50 + w * 0.15, top, w * 0.3, bottom - top, 4), shade(color, -0.12))
    a.fill(rrect(50 - w / 2 + 2, top + 2, 5, bottom - top - 4, 2.5), WHITE, 0.25)
    a.fill(ellipse(50, top + 1, w / 2, 4), "#C9CED6")
    a.fill(ellipse(50, top + 1, w / 2 - 3, 2.6), "#AEB4BE")
    a.fill(ellipse(50, bottom - 1, w / 2, 3.5), shade(color, -0.25))
    if accent:
        a.fill(rrect(50 - w / 2, top + (bottom - top) * 0.42, w, 12, 1), accent)
    return (top, bottom, w)


def tub(a, color, lid, label, top=30, bottom=86, w=48, scoop=False):
    ground(a, w * 0.6, bottom + 2)
    a.fill(rrect(50 - w / 2, top + 6, w, bottom - top - 6, 7), color)
    a.fill(rrect(50 + w * 0.18, top + 6, w * 0.3, bottom - top - 6, 6), shade(color, -0.12))
    a.fill(rrect(50 - w / 2 + 4, top + 20, w - 8, 26, 4), label)
    a.fill(rrect(50 - w / 2 - 2, top, w + 4, 10, 4), lid)
    a.fill(rrect(50 - w / 2 - 2, top + 6, w + 4, 4, 2), shade(lid, -0.18))
    return top


def jar(a, content, lid, label=WHITE, top=30, bottom=86, w=42, label_color=None):
    ground(a, w * 0.6, bottom + 2)
    a.fill(rrect(50 - w / 2, top + 8, w, bottom - top - 8, 9), content)
    a.fill(rrect(50 + w * 0.16, top + 8, w * 0.3, bottom - top - 8, 8), shade(content, -0.14))
    a.fill(rrect(50 - w / 2 + 4, top + 12, 4, bottom - top - 22, 2), WHITE, 0.35)
    a.fill(rrect(50 - w / 2 + 3, top + 28, w - 6, 22, 4), label)
    if label_color:
        a.fill(circle(50, top + 39, 6), label_color)
    a.fill(rrect(50 - w / 2 + 2, top, w - 4, 11, 3), lid)
    a.fill(rrect(50 - w / 2 + 2, top + 7, w - 4, 4, 2), shade(lid, -0.2))


def bottle(a, liquid, cap, top=14, bottom=86, w=30, neck=10, label=None, alpha=1.0):
    ground(a, w * 0.65, bottom + 2)
    a.fill(rrect(50 - neck / 2, top, neck, 16, 3), cap)
    body = smooth([(50 - neck / 2, top + 14), (50 - w / 2, top + 30), (50 - w / 2, bottom - 4), (50, bottom),
                   (50 + w / 2, bottom - 4), (50 + w / 2, top + 30), (50 + neck / 2, top + 14)], closed=True, tension=0.5)
    a.fill(body, liquid, alpha)
    a.fill(rrect(50 + w * 0.15, top + 30, w * 0.28, bottom - top - 36, 5), shade(liquid, -0.15), alpha)
    a.fill(rrect(50 - w / 2 + 3, top + 30, 4, bottom - top - 40, 2), WHITE, 0.4)
    if label:
        a.fill(rrect(50 - w / 2, top + 44, w, 20, 2), label)


# ------------------------------------------------------------------ garnish & pieces

def dots(a, color, cx, cy, rx, ry, n, r, seed, alpha=1.0, jitter=0.25):
    g = rnd(seed)
    for _ in range(n):
        t = g.uniform(0, 2 * math.pi)
        k = math.sqrt(g.uniform(0, 1))
        x, y = cx + math.cos(t) * rx * k, cy + math.sin(t) * ry * k
        rr = r * g.uniform(1 - jitter, 1 + jitter)
        a.fill(circle(x, y, rr), color, alpha)


def grains(a, color, cx, cy, rx, ry, n, seed, length=2.6, width=1.1, alpha=0.9):
    g = rnd(seed)
    for _ in range(n):
        t = g.uniform(0, 2 * math.pi)
        k = math.sqrt(g.uniform(0, 1))
        x, y = cx + math.cos(t) * rx * k, cy + math.sin(t) * ry * k
        a.fill(ellipse_rot(x, y, length / 2, width / 2, g.uniform(0, 180)), color, alpha)


def leaves(a, color, cx, cy, rx, ry, n, seed, size=2.4):
    g = rnd(seed)
    for _ in range(n):
        t = g.uniform(0, 2 * math.pi)
        k = math.sqrt(g.uniform(0, 1))
        x, y = cx + math.cos(t) * rx * k, cy + math.sin(t) * ry * k
        a.fill(blob(x, y, size * g.uniform(0.8, 1.3), size * g.uniform(0.6, 1.0), g.randint(0, 999), 0.25, 6), color)


def drizzle(a, cx, cy, rx, ry, color=OIL, width=2.2, alpha=0.75, seed=3):
    g = rnd(seed)
    pts = []
    for i in range(7):
        t = i / 6
        pts.append((cx - rx + 2 * rx * t, cy + math.sin(t * 6.2 + g.uniform(0, 1)) * ry))
    a.stroke(smooth(pts, closed=False), color, width, alpha)


def swirl(a, cx, cy, rx, ry, color, width=1.6, alpha=0.6):
    pts = []
    for i in range(22):
        t = i / 21
        ang = t * 4.2 * math.pi
        r = 0.25 + 0.75 * t
        pts.append((cx + math.cos(ang) * rx * r, cy + math.sin(ang) * ry * r))
    a.stroke(smooth(pts, closed=False), color, width, alpha)


def chicken_piece(a, cx, cy, s=1.0, deg=0, color=CHICKEN):
    """Roast drumstick."""
    body = [(cx - 9 * s, cy), (cx - 7 * s, cy - 6 * s), (cx, cy - 7.5 * s), (cx + 7 * s, cy - 4 * s), (cx + 10 * s, cy),
            (cx + 7 * s, cy + 5 * s), (cx, cy + 6 * s), (cx - 7 * s, cy + 5 * s)]
    body = rot_pts(body, cx, cy, deg)
    bone_end = rot_pts([(cx + 15 * s, cy - 1 * s)], cx, cy, deg)[0]
    bx, by = rot_pts([(cx + 9 * s, cy)], cx, cy, deg)[0]
    a.stroke(poly([(bx, by), bone_end], closed=False), "#F3EEE3", 3.4 * s)
    a.fill(circle(bone_end[0], bone_end[1] - 1.2 * s, 2 * s), "#F3EEE3")
    a.fill(circle(bone_end[0], bone_end[1] + 1.2 * s, 2 * s), "#F3EEE3")
    a.fill(smooth(body), color)
    hl = rot_pts([(cx - 5 * s, cy - 3.5 * s), (cx + 1 * s, cy - 5 * s), (cx + 5 * s, cy - 3 * s), (cx, cy - 2 * s)], cx, cy, deg)
    a.fill(smooth(hl), shade(color, 0.3), 0.8)
    a.fill(smooth(rot_pts([(cx - 6 * s, cy + 3 * s), (cx + 4 * s, cy + 4.5 * s), (cx + 7 * s, cy + 3 * s), (cx, cy + 5.5 * s)], cx, cy, deg)),
           shade(color, -0.2), 0.7)


def meat_chunk(a, cx, cy, s=1.0, seed=1, color=MEAT):
    a.fill(blob(cx, cy, 6 * s, 4.6 * s, seed, 0.18, 7), color)
    a.fill(blob(cx - 1.2 * s, cy - 1.4 * s, 3.4 * s, 2 * s, seed + 7, 0.2, 6), shade(color, 0.25), 0.8)


def kofta_finger(a, cx, cy, length=18, width=7, deg=0, color="#9A5232"):
    a.fill(capsule(cx, cy, length, width, deg), color)
    a.fill(capsule(cx - 1, cy - 1.4, length * 0.7, width * 0.35, deg), shade(color, 0.25), 0.7)
    for i in (-1, 0, 1):
        x, y = rot_pts([(cx + i * length * 0.25, cy)], cx, cy, deg)[0]
        a.fill(ellipse_rot(x, y, 0.8, width * 0.42, deg), shade(color, -0.35), 0.6)


def ball(a, cx, cy, r, color, seed=1, texture=None):
    a.fill(blob(cx, cy, r, r * 0.95, seed, 0.06, 8), color)
    a.fill(ellipse(cx - r * 0.3, cy - r * 0.35, r * 0.45, r * 0.3), shade(color, 0.3), 0.8)
    a.fill(blob(cx + r * 0.2, cy + r * 0.45, r * 0.7, r * 0.35, seed + 3, 0.1, 6), shade(color, -0.2), 0.6)
    if texture:
        dots(a, texture, cx, cy, r * 0.7, r * 0.7, 6, 0.6, seed + 11, 0.7)


def roll(a, cx, cy, length, width, color, deg=0, lines=True):
    a.fill(capsule(cx, cy, length, width, deg), color)
    a.fill(capsule(cx, cy - width * 0.22, length * 0.82, width * 0.3, deg), shade(color, 0.22), 0.7)
    if lines:
        for i in (-0.25, 0.05, 0.32):
            x, y = rot_pts([(cx + i * length, cy)], cx, cy, deg)[0]
            a.fill(ellipse_rot(x, y, 0.7, width * 0.45, deg + 18), shade(color, -0.25), 0.55)


def tomato_slice(a, cx, cy, r, ry=None):
    ry = ry or r * 0.7
    a.fill(ellipse(cx, cy, r, ry), TOMATO)
    a.fill(ellipse(cx, cy, r * 0.78, ry * 0.76), "#F26B5B")
    for i in range(3):
        ang = i * 2.1 + 0.4
        a.fill(ellipse(cx + math.cos(ang) * r * 0.45, cy + math.sin(ang) * ry * 0.45, r * 0.12, ry * 0.12), "#FBD6A0")


def cucumber_slice(a, cx, cy, r, ry=None):
    ry = ry or r * 0.7
    a.fill(ellipse(cx, cy, r, ry), "#3E8E2E")
    a.fill(ellipse(cx, cy, r * 0.84, ry * 0.82), "#CDEB9E")
    dots(a, "#9BC968", cx, cy, r * 0.35, ry * 0.35, 4, r * 0.08, int(cx * 7 + cy))


def lemon_wedge(a, cx, cy, r, deg=0):
    pts = [(cx - r, cy), (cx - r * 0.7, cy - r * 0.7), (cx, cy - r), (cx + r * 0.7, cy - r * 0.7), (cx + r, cy)]
    a.fill(smooth(rot_pts(pts, cx, cy, deg)), "#F6D33C")
    pts2 = [(cx - r * 0.8, cy), (cx - r * 0.55, cy - r * 0.55), (cx, cy - r * 0.8), (cx + r * 0.55, cy - r * 0.55), (cx + r * 0.8, cy)]
    a.fill(smooth(rot_pts(pts2, cx, cy, deg)), "#FBEA8A")


def bread_round(a, cx, cy, rx, ry, color=BREAD, seed=1, spots=7, puff=True):
    a.fill(ellipse(cx, cy + 2, rx, ry), shade(color, -0.22))
    a.fill(blob(cx, cy, rx, ry, seed, 0.03, 10), color)
    if puff:
        a.fill(ellipse(cx - rx * 0.15, cy - ry * 0.2, rx * 0.6, ry * 0.45), shade(color, 0.25), 0.7)
    dots(a, shade(color, -0.35), cx, cy, rx * 0.75, ry * 0.7, spots, rx * 0.06, seed + 5, 0.55)


def sesame(a, cx, cy, rx, ry, n, seed, color="#FFF4D6"):
    grains(a, color, cx, cy, rx, ry, n, seed, 1.8, 1.0, 1.0)


def nuts_topping(a, cx, cy, rx, ry, seed, almonds=True, pine=True):
    g = rnd(seed)
    if almonds:
        for _ in range(5):
            x, y = cx + g.uniform(-rx, rx), cy + g.uniform(-ry, ry)
            a.fill(ellipse_rot(x, y, 2.2, 1.2, g.uniform(0, 180)), "#E9C98F")
    if pine:
        for _ in range(7):
            x, y = cx + g.uniform(-rx, rx), cy + g.uniform(-ry, ry)
            a.fill(ellipse_rot(x, y, 1.3, 0.75, g.uniform(0, 180)), "#F7E7B8")


# ------------------------------------------------------------------ recipes
R = {}


def art(*names):
    def deco(fn):
        for n in names:
            R[n] = fn
        return fn
    return deco


def rice_plate(a, base, shade_c, grains_c, seed, toppings=None, height=20, width=56):
    cx, cy, rx, ry = plate(a)
    a.fill(mound(50, cy + 2, width, height, seed), shade_c)
    a.fill(mound(48, cy, width - 6, height - 3, seed + 1), base)
    grains(a, grains_c, 49, cy - height * 0.45, width * 0.38, height * 0.38, 40, seed + 2)
    if toppings:
        toppings(a, cy)
    return cy


# --- breakfast ---------------------------------------------------------------------------

@art("hummus")
def _hummus(a):
    cx, cy, rx, ry = plate(a)
    a.fill(ellipse(cx, cy, rx * 0.95, ry * 0.95), shade(HUMMUS, -0.12))
    a.fill(ellipse(cx, cy - 1, rx * 0.9, ry * 0.86), HUMMUS)
    swirl(a, cx, cy - 1, rx * 0.7, ry * 0.65, shade(HUMMUS, -0.25), 1.8, 0.7)
    a.fill(ellipse(cx, cy - 1, rx * 0.28, ry * 0.3), OIL, 0.75)
    for x, y in [(cx - 6, cy - 3), (cx + 3, cy - 4), (cx + 6, cy + 1), (cx - 2, cy + 2)]:
        a.fill(circle(x, y, 2.2), "#E8C276")
        a.fill(circle(x - 0.6, y - 0.7, 0.9), "#F7E2B0")
    dots(a, "#C9452D", cx, cy - 1, rx * 0.75, ry * 0.7, 10, 0.7, 4)
    leaves(a, PARSLEY, cx + 9, cy - 4, 4, 2, 4, 9, 1.6)


@art("ful")
def _ful(a):
    cx, cy, rx, ry = bowl(a, surface="#8E5E34", body=CLAY)
    dots(a, "#B07A47", cx, cy, rx * 0.8, ry * 0.7, 18, 2.0, 7)
    dots(a, "#6E4523", cx, cy, rx * 0.8, ry * 0.7, 10, 1.2, 8)
    a.fill(ellipse(cx + 4, cy - 1, 7, 2.6), OIL, 0.7)
    leaves(a, PARSLEY, cx - 6, cy, 6, 2.5, 5, 3, 1.5)
    tomato_slice(a, cx + 12, cy + 1, 3.5)


@art("falafel")
def _falafel(a):
    cx, cy, rx, ry = plate(a)
    for i, (x, y) in enumerate([(36, cy - 4), (50, cy - 7), (64, cy - 4), (43, cy + 3), (57, cy + 3)]):
        ball(a, x, y, 7.2, "#9A6A2E", i + 1, "#6E8F2C")
    leaves(a, PARSLEY, 70, cy + 6, 3, 2, 3, 2, 1.8)


@art("msabbaha")
def _msabbaha(a):
    cx, cy, rx, ry = plate(a)
    a.fill(ellipse(cx, cy, rx * 0.9, ry * 0.9), TAHINI)
    for i in range(16):
        g = rnd(i)
        x, y = cx + g.uniform(-rx * 0.6, rx * 0.6), cy + g.uniform(-ry * 0.55, ry * 0.55)
        a.fill(circle(x, y, 2.4), "#E3B86C")
        a.fill(circle(x - 0.7, y - 0.7, 0.9), "#F6DFA8")
    a.fill(ellipse(cx + 2, cy - 1, 8, 3), OIL, 0.6)
    dots(a, "#C9452D", cx, cy, rx * 0.7, ry * 0.6, 8, 0.7, 5)
    leaves(a, PARSLEY, cx - 8, cy - 2, 4, 2, 4, 4, 1.5)


@art("fatteh_hummus")
def _fatteh_hummus(a):
    cx, cy, rx, ry = bowl(a, surface=YOGURT, body="#E9E1D3")
    a.fill(ellipse(cx, cy, rx * 0.9, ry * 0.8), "#F3E6C8")
    swirl(a, cx, cy, rx * 0.6, ry * 0.55, "#E2CFA2", 1.5, 0.8)
    nuts_topping(a, cx, cy, rx * 0.6, ry * 0.45, 4)
    a.fill(ellipse(cx - 6, cy - 1, 5, 2), "#D9A441", 0.6)
    dots(a, "#C9452D", cx, cy, rx * 0.6, ry * 0.5, 7, 0.7, 6)
    leaves(a, PARSLEY, cx + 8, cy, 4, 2, 4, 1, 1.4)


@art("shakshuka")
def _shakshuka(a):
    cx, cy, rx, ry = pan(a, "#D6402E")
    dots(a, "#B9301F", cx, cy, rx * 0.8, ry * 0.7, 14, 1.8, 3)
    for x, y in [(cx - 11, cy - 1), (cx + 10, cy + 1)]:
        a.fill(blob(x, y, 8.5, 5, int(x), 0.12, 8), WHITE)
        a.fill(circle(x + 0.5, y - 0.5, 3.2), YOLK)
        a.fill(circle(x - 0.4, y - 1.3, 1.1), "#FFE08A")
    leaves(a, PARSLEY, cx, cy - 2, 10, 4, 6, 5, 1.5)


@art("egg_boiled")
def _egg_boiled(a):
    cx, cy, rx, ry = plate(a)
    # one whole egg behind, two halves in front
    a.fill(ellipse_rot(64, cy - 9, 9.5, 12.5, 18), "#D9C3A5")
    a.fill(ellipse_rot(64, cy - 10, 9, 12, 18), "#F1DEC2")
    a.fill(ellipse_rot(61, cy - 14, 3, 4.6, 18), "#FBF0E0", 0.9)
    for x, y, d in [(36, cy - 1, -10), (52, cy + 3, 8)]:
        a.fill(ellipse_rot(x, y + 1.8, 11.5, 7.6, d), "#D8D2C6")
        a.fill(ellipse_rot(x, y, 11.5, 7.6, d), WHITE)
        a.fill(ellipse_rot(x + 0.6, y - 0.2, 5.8, 4.4, d), "#F4A40E")
        a.fill(ellipse_rot(x + 0.6, y - 0.6, 5.2, 3.8, d), YOLK)
        a.fill(ellipse_rot(x - 1.2, y - 1.8, 2, 1.1, d), "#FFE08A")
    dots(a, "#2B2B2B", 44, cy + 1, 14, 4, 6, 0.4, 2, 0.6)
    dots(a, "#C2402A", 44, cy + 1, 14, 4, 5, 0.45, 8, 0.7)


@art("egg_fried", "addon_egg")
def _egg_fried(a):
    cx, cy, rx, ry = plate(a)
    a.fill(blob(50, cy - 1, 26, 13, 4, 0.14, 10), "#F3EBDD")
    a.fill(blob(50, cy - 2, 24, 11.5, 4, 0.14, 10), WHITE)
    a.fill(circle(52, cy - 4, 7), "#F4A40E")
    a.fill(circle(52, cy - 4.5, 6.2), YOLK)
    a.fill(ellipse(49.5, cy - 7, 2.6, 1.6), "#FFE7A0")


@art("ijjeh")
def _ijjeh(a):
    cx, cy, rx, ry = plate(a)
    for i, (x, y) in enumerate([(38, cy - 2), (60, cy - 5), (55, cy + 4)]):
        a.fill(blob(x, y + 1.5, 13, 7, i + 2, 0.12, 9), "#C98E2E")
        a.fill(blob(x, y, 12.5, 6.5, i + 2, 0.12, 9), "#EBB842")
        leaves(a, PARSLEY, x, y, 8, 3.5, 6, i + 10, 1.1)
        dots(a, "#F6D77A", x, y, 7, 3, 4, 1.2, i + 20)


@art("labneh")
def _labneh(a):
    cx, cy, rx, ry = plate(a)
    a.fill(ellipse(cx, cy, rx * 0.88, ry * 0.85), shade(YOGURT, -0.06))
    a.fill(ellipse(cx, cy - 1, rx * 0.84, ry * 0.78), YOGURT)
    swirl(a, cx, cy - 1, rx * 0.65, ry * 0.6, "#E4DED0", 1.6, 0.9)
    a.fill(ellipse(cx + 1, cy - 1, 9, 3.4), OIL, 0.55)
    for x, y in [(cx - 14, cy - 3), (cx + 15, cy + 1), (cx + 9, cy - 6)]:
        a.fill(ellipse(x, y, 3.2, 2.4), "#3E3A2A")
        a.fill(ellipse(x - 0.8, y - 0.8, 1, 0.7), "#7A7562")
    leaves(a, "#5F8F3E", cx - 4, cy - 2, 3, 1.5, 3, 6, 1.4)


def cheese_slices(a, color, marks=None, holes=False):
    cx, cy, rx, ry = plate(a)
    for i, (x, y) in enumerate([(38, cy - 3), (50, cy - 1), (62, cy - 4)]):
        pts = rot_pts([(x - 9, y - 6), (x + 9, y - 6), (x + 9, y + 6), (x - 9, y + 6)], x, y, -12 + i * 10)
        a.fill(poly([(px, py + 2.4) for px, py in pts]), shade(color, -0.12))
        a.fill(poly(pts), color)
        if marks:
            for k in (-4, 0, 4):
                m = rot_pts([(x - 7, y + k - 1), (x + 7, y + k + 1)], x, y, -12 + i * 10)
                a.stroke(poly(m, closed=False), marks, 1.6, 0.8)
        if holes:
            dots(a, shade(color, -0.1), x, y, 5, 3, 3, 0.9, i + 3)


@art("white_cheese")
def _white_cheese(a):
    cheese_slices(a, "#F6EED8", holes=True)


@art("halloumi")
def _halloumi(a):
    cheese_slices(a, "#F7E6C2", marks="#B98340")


@art("olives")
def _olives(a):
    cx, cy, rx, ry = small_bowl(a, "#5D6B2E", rim_y=56, rx=30, ry=9, depth=24)
    g = rnd(5)
    for i in range(11):
        x, y = cx + g.uniform(-rx * 0.7, rx * 0.7), cy + g.uniform(-ry * 0.5, ry * 0.3) - 1
        col = "#6B7A2A" if i % 3 else "#3B3324"
        a.fill(ellipse_rot(x, y, 4.2, 3, g.uniform(-30, 30)), col)
        a.fill(ellipse(x - 1.2, y - 1, 1.2, 0.7), shade(col, 0.4))


@art("zaatar_oil")
def _zaatar_oil(a):
    ground(a, 38, 80)
    for cx, surf in [(32, "#6E7633"), (68, "#C9AE2E")]:
        small_bowl(a, surf, cx=cx, rim_y=56, rx=17, ry=5.5, depth=16)
    dots(a, "#4E5523", 32, 56.6, 12, 3.2, 14, 0.6, 3)
    dots(a, "#E9DDB8", 32, 56.6, 12, 3.2, 6, 0.5, 4)
    a.fill(ellipse(68, 56.4, 11, 3), "#E2CB4D", 0.6)
    bread_round(a, 50, 72, 14, 5, BREAD, 3, 4)


def manakish(a, topping, accents, seed):
    ground(a, 34, 87)
    bread_round(a, 50, 62, 40, 20, "#E5B565", seed, 0, puff=False)
    a.fill(blob(50, 61, 33, 15.5, seed + 1, 0.06, 10), topping)
    accents(a)


@art("manakish_zaatar")
def _man_zaatar(a):
    def acc(a):
        dots(a, "#4B5221", 50, 61, 28, 12, 40, 0.8, 4)
        a.fill(ellipse(44, 58, 10, 3.5), "#D9C64A", 0.45)
        sesame(a, 50, 61, 26, 11, 14, 5)
    manakish(a, "#77812F", acc, 1)


@art("manakish_cheese")
def _man_cheese(a):
    def acc(a):
        a.fill(blob(50, 60, 28, 12, 9, 0.12, 9), "#FFF0C2")
        dots(a, "#F2C46B", 50, 60, 24, 10, 10, 2.2, 6, 0.8)
        sesame(a, 50, 61, 26, 11, 10, 2, "#FFFFFF")
    manakish(a, "#FBE7B0", acc, 2)


@art("manakish_muhammara")
def _man_muhammara(a):
    def acc(a):
        dots(a, "#9E2A1E", 50, 61, 28, 12, 30, 0.8, 4)
        a.fill(ellipse(44, 58, 10, 3.5), "#F09A55", 0.45)
        sesame(a, 50, 61, 26, 11, 10, 8)
    manakish(a, "#C9452D", acc, 3)


@art("fatayer_spinach")
def _fatayer(a):
    cx, cy, rx, ry = plate(a)
    for i, (x, y, d) in enumerate([(38, cy - 4, -8), (62, cy - 4, 10), (50, cy + 3, 0)]):
        pts = rot_pts([(x - 11, y + 5), (x, y - 9), (x + 11, y + 5)], x, y, d)
        a.fill(smooth([(px, py + 2) for px, py in pts], tension=0.4), "#C98E3E")
        a.fill(smooth(pts, tension=0.4), "#E9BC6B")
        a.fill(ellipse(x, y + 1, 2.2, 1.6), "#4D7A2C")
        a.fill(ellipse(x - 2, y - 1, 3, 1.2), "#F6D99A", 0.7)


@art("sfiha")
def _sfiha(a):
    cx, cy, rx, ry = plate(a)
    for i, (x, y) in enumerate([(36, cy - 3), (54, cy - 6), (64, cy + 2), (44, cy + 4)]):
        a.fill(rrect(x - 9, y - 6 + 1.6, 18, 12, 3), "#C98E3E")
        a.fill(rrect(x - 9, y - 6, 18, 12, 3), "#EDC27A")
        a.fill(rrect(x - 7, y - 4.5, 14, 9, 2), "#9C4A2C")
        dots(a, "#C9452D", x, y, 5, 3, 5, 1, i + 4)
        dots(a, "#F7E7B8", x, y, 5, 3, 3, 0.7, i + 9)


@art("kaak")
def _kaak(a):
    ground(a, 34, 86)
    outer, inner = ellipse(50, 62, 38, 21), ellipse(50, 62, 17, 8)
    a.fill(outer + inner, "#B9772F", evenodd=True)
    outer, inner = ellipse(50, 60, 38, 21), ellipse(50, 61, 18, 8.4)
    a.fill(outer + inner, "#D9994A", evenodd=True)
    a.fill(ellipse(44, 50, 22, 6), "#E9B66A", 0.7)
    g = rnd(6)
    for _ in range(70):
        t = g.uniform(0, 2 * math.pi)
        k = g.uniform(0.62, 0.92)
        x, y = 50 + math.cos(t) * 36 * k, 60 + math.sin(t) * 19.5 * k
        a.fill(ellipse_rot(x, y, 0.95, 0.55, g.uniform(0, 180)), "#FFF4D6")


@art("halawa")
def _halawa(a):
    cx, cy, rx, ry = plate(a)
    pts = [(28, cy - 2), (56, cy - 10), (72, cy - 4), (44, cy + 4)]
    a.fill(poly([(x, y + 10) for x, y in [(28, cy - 2), (44, cy + 4), (72, cy - 4)]] + [(72, cy - 4), (44, cy + 4), (28, cy - 2)]), "#D9C08F")
    a.fill(poly([(28, cy - 2), (44, cy + 4), (44, cy + 14), (28, cy + 8)]), "#D8BE8A")
    a.fill(poly([(44, cy + 4), (72, cy - 4), (72, cy + 6), (44, cy + 14)]), "#CDB27C")
    a.fill(poly(pts), "#EDDBB4")
    dots(a, PISTACHIO, 50, cy - 3, 14, 4, 8, 1.2, 4)
    grains(a, "#D9C49A", 50, cy - 3, 16, 5, 14, 5, 3, 0.6)


@art("qashta", "addon_qashta")
def _qashta(a):
    cx, cy, rx, ry = small_bowl(a, "#FFFBEF", rim_y=54, rx=28, ry=8.5, depth=24)
    a.fill(blob(cx, cy - 2.5, rx * 0.75, ry * 1.2, 5, 0.18, 8), "#FFFDF6")
    a.fill(blob(cx - 2, cy - 4, rx * 0.4, ry * 0.6, 6, 0.2, 7), WHITE)
    dots(a, PISTACHIO, cx, cy - 2, rx * 0.4, ry * 0.4, 8, 1.1, 3)
    dots(a, "#F2A7B2", cx, cy - 2, rx * 0.4, ry * 0.4, 4, 0.8, 9)


def cereal_bowl(a, piece, milk=True, seed=1):
    cx, cy, rx, ry = bowl(a, surface="#F8F4EA" if milk else "#E6C27D", body="#EAF1F8", band="#F28C38")
    piece(a, cx, cy, rx, ry, seed)


@art("oats")
def _oats(a):
    def p(a, cx, cy, rx, ry, seed):
        a.fill(ellipse(cx, cy, rx, ry), "#E5D0A0")
        grains(a, "#C9A96B", cx, cy, rx * 0.8, ry * 0.7, 40, seed, 3.2, 1.6)
        grains(a, "#F2E2BE", cx, cy, rx * 0.8, ry * 0.7, 20, seed + 3, 3, 1.4)
    cereal_bowl(a, p)


@art("corn_flakes")
def _corn_flakes(a):
    def p(a, cx, cy, rx, ry, seed):
        g = rnd(seed)
        for _ in range(22):
            x, y = cx + g.uniform(-rx * 0.8, rx * 0.8), cy + g.uniform(-ry * 0.6, ry * 0.5) - 2
            a.fill(blob(x, y, 3.4, 2.4, g.randint(0, 99), 0.25, 6), g.choice(["#E7A63C", "#F1BE5C", "#D9902A"]))
    cereal_bowl(a, p)


def croissant(a, color="#E3A64B", choc=False):
    ground(a, 36, 82)
    segs = [(-26, 6, 7), (-14, 2, 10), (0, 0, 12), (14, 2, 10), (26, 6, 7)]
    for x, y, r in segs:
        a.fill(ellipse(50 + x, 60 + y + 2, r + 1, r * 0.85), shade(color, -0.25))
    for x, y, r in segs:
        a.fill(ellipse(50 + x, 60 + y, r + 1, r * 0.85), color)
        a.fill(ellipse(50 + x - 1, 60 + y - r * 0.35, r * 0.6, r * 0.3), shade(color, 0.35), 0.8)
    if choc:
        for x in (-8, 6):
            a.stroke(poly([(50 + x - 6, 52), (50 + x + 6, 56)], closed=False), CHOC, 2, 0.9)


@art("croissant")
def _croissant(a):
    croissant(a)


@art("croissant_choc")
def _croissant_choc(a):
    croissant(a, "#D99843", True)


@art("qalayet_bandora")
def _qalayet(a):
    cx, cy, rx, ry = pan(a, "#C9452D")
    g = rnd(4)
    for _ in range(10):
        x, y = cx + g.uniform(-rx * 0.7, rx * 0.7), cy + g.uniform(-ry * 0.6, ry * 0.5)
        a.fill(blob(x, y, 4.2, 3, g.randint(0, 99), 0.2, 6), "#E8574A")
        a.fill(ellipse(x - 1, y - 1, 1.6, 1), "#F6907F")
    leaves(a, "#4E9A3A", cx, cy, rx * 0.6, ry * 0.5, 6, 2, 1.6)
    dots(a, "#F3E6B0", cx, cy, rx * 0.6, ry * 0.5, 6, 0.9, 7)


@art("cheese_triangles")
def _cheese_tri(a):
    ground(a, 34, 86)
    cx, cy, r, ry = 50, 64, 34, 16
    # round box seen from above at an angle: foil wedges, one unwrapped portion lifted out
    a.fill(ellipse(cx, cy + 4, r, ry), "#C43A2F")
    a.fill(ellipse(cx, cy, r, ry), "#E5483B")
    a.fill(ellipse(cx, cy, r - 3, ry - 2), "#F6F1E4")
    n = 8
    for i in range(n):
        if i == 6:
            continue
        a0, a1 = i * 360 / n + 2, (i + 1) * 360 / n - 2
        a.fill(wedge(cx, cy, r - 4, a0, a1, ry - 2.8), "#D5DAE1" if i % 2 else "#E8ECF1")
        mid = math.radians((a0 + a1) / 2)
        a.fill(circle(cx + math.cos(mid) * (r - 12), cy + math.sin(mid) * (ry - 6), 1.6), "#E5483B", 0.8)
    a.fill(circle(cx, cy, 3), "#E5483B")
    # unwrapped portion
    px, py = 70, 54
    tri = [(px - 12, py + 7), (px + 2, py - 12), (px + 12, py + 6)]
    a.fill(poly([(x, y + 3) for x, y in tri]), "#E3CF9D")
    a.fill(smooth(tri, tension=0.25), "#FBEFCB")
    a.fill(poly([(px - 6, py + 3), (px + 1, py - 6), (px + 1, py + 3)]), WHITE, 0.45)


@art("mortadella")
def _mortadella(a):
    cx, cy, rx, ry = plate(a)
    for i, (x, y) in enumerate([(42, cy - 2), (56, cy - 4), (50, cy + 2)]):
        a.fill(ellipse(x, y + 1.6, 13, 7), "#D88E8E")
        a.fill(ellipse(x, y, 13, 7), "#F2B6B0")
        dots(a, WHITE, x, y, 9, 4.5, 6, 0.9, i + 2)
        dots(a, "#7EAA4A", x, y, 9, 4.5, 3, 0.6, i + 7)


# --- breads & starches ---------------------------------------------------------------------

@art("bread_arabic")
def _bread_arabic(a):
    ground(a, 36, 84)
    bread_round(a, 50, 66, 38, 15, "#EBC27A", 2, 8)
    bread_round(a, 50, 58, 38, 15, "#F0CB86", 3, 9)


@art("bread_taboon")
def _taboon(a):
    ground(a, 38, 84)
    a.fill(blob(50, 64, 42, 18, 6, 0.08, 12), "#B9772F")
    a.fill(blob(50, 61, 42, 18, 6, 0.08, 12), "#DCA45A")
    dots(a, "#A9692A", 50, 61, 34, 14, 22, 1.8, 3, 0.8)
    dots(a, "#EFC98A", 50, 61, 34, 14, 12, 1.4, 4, 0.7)


@art("bread_shrak")
def _shrak(a):
    ground(a, 40, 82)
    a.fill(smooth([(10, 70), (30, 50), (60, 44), (90, 58), (80, 74), (50, 78), (22, 78)]), "#EED8A8")
    a.fill(smooth([(16, 64), (34, 52), (62, 48), (84, 58), (60, 62), (30, 66)]), "#F5E4BD")
    dots(a, "#C99A55", 50, 62, 32, 10, 14, 1.4, 5, 0.6)
    a.stroke(smooth([(14, 68), (40, 62), (70, 60), (88, 62)], closed=False), "#D9BB80", 1.4, 0.8)


@art("bread_brown")
def _bread_brown(a):
    ground(a, 36, 84)
    bread_round(a, 50, 62, 38, 16, "#B07A45", 4, 6)
    dots(a, "#8A5A2E", 50, 62, 30, 12, 20, 0.7, 7, 0.8)


def toast(a, color, crust):
    ground(a, 26, 86)
    for i, (x, y, d) in enumerate([(44, 58, -8), (56, 60, 8)]):
        pts = [(x - 15, y + 20), (x - 15, y - 6), (x - 18, y - 12), (x - 10, y - 20), (x, y - 21), (x + 10, y - 20),
               (x + 18, y - 12), (x + 15, y - 6), (x + 15, y + 20)]
        pts = rot_pts(pts, x, y, d)
        a.fill(smooth(pts, tension=0.5), crust)
        inner = rot_pts([(x - 12, y + 17), (x - 12, y - 6), (x - 14, y - 10), (x - 8, y - 17), (x, y - 18), (x + 8, y - 17),
                         (x + 14, y - 10), (x + 12, y - 6), (x + 12, y + 17)], x, y, d)
        a.fill(smooth(inner, tension=0.5), color)
        dots(a, shade(color, -0.12), x, y, 9, 13, 8, 0.8, i + 3, 0.7)


@art("toast_white")
def _toast_white(a):
    toast(a, "#FBF0D8", "#D9A050")


@art("toast_brown")
def _toast_brown(a):
    toast(a, "#D9B788", "#9C6633")


@art("rice_white", "addon_rice_side")
def _rice_white(a):
    rice_plate(a, RICE, "#E8D9B4", WHITE, 1, lambda a, cy: leaves(a, PARSLEY, 52, cy - 16, 3, 1.5, 3, 4, 1.4))


@art("rice_basmati")
def _rice_basmati(a):
    def top(a, cy):
        grains(a, "#FFFFFF", 50, cy - 9, 22, 8, 30, 9, 3.6, 0.9)
    rice_plate(a, "#FFF7E6", "#EAD9B2", "#FFFFFF", 2, top)


@art("rice_vermicelli")
def _rice_verm(a):
    def top(a, cy):
        grains(a, "#B9772F", 50, cy - 9, 22, 8, 22, 5, 4.2, 1.0)
    rice_plate(a, RICE, "#E8D9B4", WHITE, 3, top)


@art("bulgur")
def _bulgur(a):
    rice_plate(a, "#D7B477", "#B99456", "#EAD2A0", 4, lambda a, cy: dots(a, "#C9452D", 50, cy - 9, 18, 6, 6, 1.2, 3))


@art("freekeh")
def _freekeh(a):
    rice_plate(a, "#9E8B58", "#7E6D40", "#BBA874", 5, lambda a, cy: nuts_topping(a, 50, cy - 12, 14, 4, 2))


@art("pasta")
def _pasta(a):
    cx, cy, rx, ry = plate(a)
    a.fill(mound(50, cy + 1, 52, 16, 6), "#E7BE68")
    g = rnd(8)
    for i in range(14):
        x0 = g.uniform(30, 66)
        y0 = cy - g.uniform(2, 14)
        pts = [(x0 + k * 3, y0 + math.sin(k * 1.4 + i) * 2.5) for k in range(6)]
        a.stroke(smooth(pts, closed=False), g.choice(["#F6D58A", "#E9B657", "#F9E3A6"]), 1.6)
    leaves(a, PARSLEY, 50, cy - 14, 5, 2, 4, 2, 1.6)


@art("potato_boiled")
def _potato(a):
    cx, cy, rx, ry = plate(a)
    for i, (x, y, r) in enumerate([(38, cy - 3, 10), (60, cy - 4, 11), (50, cy + 3, 9)]):
        a.fill(blob(x, y + 1.6, r, r * 0.72, i + 4, 0.08, 9), "#B98B4E")
        a.fill(blob(x, y, r, r * 0.72, i + 4, 0.08, 9), "#E9C98A")
        a.fill(ellipse(x - r * 0.3, y - r * 0.3, r * 0.45, r * 0.25), "#F7E4B6", 0.8)
        dots(a, "#A9824E", x, y, r * 0.6, r * 0.4, 3, 0.6, i + 9)
    leaves(a, PARSLEY, 50, cy - 6, 14, 5, 6, 3, 1.3)


@art("fries", "addon_fries")
def _fries(a):
    ground(a, 26, 88)
    g = rnd(3)
    for i in range(11):
        x = 32 + i * 3.6 + g.uniform(-1, 1)
        top = 20 + g.uniform(0, 14)
        a.fill(rrect(x, top, 5, 40, 1.5), g.choice(["#F7C64E", "#F2B83A", "#FAD566"]))
    a.fill(poly([(26, 46), (74, 46), (68, 88), (32, 88)]), "#E8453C")
    a.fill(poly([(50, 46), (74, 46), (68, 88), (50, 88)]), "#C9352D")
    a.fill(ellipse(50, 66, 8, 8), "#FFFFFF", 0.9)
    a.fill(ellipse(50, 66, 5, 5), "#E8453C")


@art("sweet_potato")
def _sweet_potato(a):
    cx, cy, rx, ry = plate(a)
    a.fill(ellipse_rot(46, cy - 3, 20, 9, -10), "#9C4F3A")
    a.fill(ellipse_rot(58, cy - 1, 13, 8, 15), "#F08A3C")
    a.fill(ellipse_rot(58, cy - 1.5, 10, 6, 15), "#F7A960")
    a.fill(ellipse_rot(42, cy - 6, 8, 2.5, -10), "#B86A50", 0.8)


@art("corn")
def _corn(a):
    ground(a, 30, 84)
    a.fill(smooth([(16, 70), (30, 40), (44, 34), (40, 56), (28, 76)]), "#8CBF4A")
    a.fill(capsule(52, 56, 58, 22, -28), "#F2C33A")
    g = rnd(2)
    for i in range(8):
        for j in range(3):
            x, y = rot_pts([(52 - 22 + i * 6.2, 56 - 6 + j * 6)], 52, 56, -28)[0]
            a.fill(ellipse_rot(x, y, 2.6, 2.4, -28), g.choice(["#F7D352", "#FBE07A", "#EDB92C"]))
    a.fill(smooth([(60, 82), (76, 66), (90, 44), (74, 74)]), "#6FA637")


# ------------------------------------------------------------------ registry export

def all_ids():
    root = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "food", "foods.json")
    db = json.load(open(root, encoding="utf-8"))
    return [f["art"] for f in db["foods"]] + [x["art"] for x in db["addons"]]


def build(name):
    a = Art()
    R[name](a)
    return a


if __name__ == "__main__":
    import importlib
    sys.modules["gen_food_art"] = sys.modules["__main__"]   # extra recipe modules register into this R
    for mod in ("food_art_more", "food_art_more2", "food_art_addons", "food_art_xl"):
        try:
            importlib.import_module(mod)
        except ModuleNotFoundError:
            pass
    ids = all_ids()
    missing = [i for i in ids if i not in R]
    out_dir = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "drawable")
    done = 0
    for name in ids:
        if name in R:
            with open(os.path.join(out_dir, f"food_{name}.xml"), "w") as fh:
                fh.write(to_vector(build(name), name))
            done += 1
    print(f"{done} pictures written, {len(missing)} missing")
    # Typed lookup table: resource shrinking keeps only drawables referenced from code.
    kt = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "java", "com", "hyperathlete", "app", "ui", "nutrition", "FoodArt.kt")
    os.makedirs(os.path.dirname(kt), exist_ok=True)
    names = sorted(n for n in set(ids) if n in R)
    with open(kt, "w") as fh:
        fh.write("package com.hyperathlete.app.ui.nutrition\n\nimport com.hyperathlete.app.R\n\n")
        fh.write("/** Food and add-on pictures by art id. Generated by tools/gen_food_art.py, do not edit. */\n")
        fh.write("object FoodArt {\n    val ids: Map<String, Int> = mapOf(\n")
        for n in names:
            fh.write(f'        "{n}" to R.drawable.food_{n},\n')
        fh.write("    )\n\n    fun of(art: String?): Int? = art?.let { ids[it] }\n}\n")
    if missing:
        print("missing:", " ".join(missing))
    if "--preview" in sys.argv:
        import cairosvg
        from PIL import Image
        i = sys.argv.index("--preview")
        path = sys.argv[i + 1]
        prefix = sys.argv[i + 2] if len(sys.argv) > i + 2 else ""
        names = [n for n in ids if n in R and n.startswith(prefix)] if prefix != "*" else [n for n in ids if n in R]
        if prefix.startswith("@"):
            names = [n for n in ids if n in R and n in prefix[1:].split(",")]
        cols, cell = int(os.environ.get("COLS", 8)), int(os.environ.get("CELL", 160))
        rows = (len(names) + cols - 1) // cols
        sheet = Image.new("RGB", (cols * cell, rows * (cell + 18)), (255, 241, 230))
        from PIL import ImageDraw
        d = ImageDraw.Draw(sheet)
        for k, n in enumerate(names):
            png = cairosvg.svg2png(bytestring=to_svg(build(n), cell).encode())
            im = Image.open(__import__("io").BytesIO(png)).convert("RGBA")
            x, y = (k % cols) * cell, (k // cols) * (cell + 18)
            sheet.paste(im, (x, y), im)
            d.text((x + 4, y + cell + 2), n, fill=(0, 0, 0))
        sheet.save(path)
        print("preview", path, len(names))
