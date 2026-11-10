"""
Add-on pictures for the "في إضافات؟" sheet (brief §11). Where the add-on is the same thing as a food
(honey, jam, banana …) the food's own picture is registered under the add-on id in the earlier modules.
"""
import math

from gen_food_art import *  # noqa: F401,F403
from foodart import blob, capsule, circle, ellipse, ellipse_rot, mound, poly, rot_pts, rrect, shade, smooth
from food_art_more import grill_marks, lettuce_leaf, onion_curls
from food_art_more2 import glass, heap, strawberry

SLATE = "#4F5B6B"


def ramekin(a, sauce, dark=False, garnish=None):
    body = SLATE if dark else CERAMIC
    cx, cy, rx, ry = small_bowl(a, sauce, rim_y=56, rx=30, ry=9.5, depth=24, body=body)
    a.fill(blob(cx - 2, cy - 1.2, rx * 0.6, ry * 0.65, 4, 0.18, 9), shade(sauce, 0.12))
    a.fill(blob(cx - 6, cy - 2.4, rx * 0.25, ry * 0.3, 6, 0.2, 7), shade(sauce, 0.35), 0.8)
    if garnish:
        garnish(a, cx, cy, rx, ry)


def side_plate(a):
    return plate(a, cy=64, rx=40, ry=18)


@art("addon_toum")
def _toum(a):
    ramekin(a, "#FFFFFF", True, lambda a, cx, cy, rx, ry: dots(a, "#E6E2D6", cx, cy, rx * 0.5, ry * 0.4, 6, 0.6, 3))


@art("addon_tahini_sauce")
def _tahini_sauce(a):
    ramekin(a, TAHINI, False, lambda a, cx, cy, rx, ry: leaves(a, PARSLEY, cx, cy, rx * 0.5, ry * 0.4, 6, 3, 1.1))


@art("addon_red_sauce")
def _red_sauce(a):
    ramekin(a, "#D2402E", False, lambda a, cx, cy, rx, ry: dots(a, "#7A1A12", cx, cy, rx * 0.6, ry * 0.5, 12, 0.5, 5))


@art("addon_white_sauce")
def _white_sauce(a):
    ramekin(a, "#F7F1E2", True, lambda a, cx, cy, rx, ry: leaves(a, "#5E9A3A", cx, cy, rx * 0.5, ry * 0.4, 7, 6, 0.9))


@art("addon_burger_sauce")
def _burger_sauce(a):
    def g(a, cx, cy, rx, ry):
        dots(a, "#6E9A35", cx, cy, rx * 0.5, ry * 0.4, 8, 0.7, 4)
    ramekin(a, "#F08A4B", False, g)


@art("addon_caesar_dressing")
def _caesar(a):
    ramekin(a, "#F2E6C2", True, lambda a, cx, cy, rx, ry: dots(a, "#2B2B2B", cx, cy, rx * 0.6, ry * 0.5, 14, 0.35, 7))


@art("addon_tomato_sauce")
def _tomato_sauce(a):
    def g(a, cx, cy, rx, ry):
        leaves(a, "#3E8E2E", cx, cy, rx * 0.5, ry * 0.4, 5, 3, 1.2)
        dots(a, "#F26B5B", cx, cy, rx * 0.5, ry * 0.4, 6, 0.9, 8, 0.7)
    ramekin(a, "#C9302A", False, g)


@art("addon_laban")
def _laban_addon(a):
    ramekin(a, "#FBF8F0", True, lambda a, cx, cy, rx, ry: dots(a, "#4F7A2B", cx, cy, rx * 0.5, ry * 0.4, 10, 0.45, 2))


@art("addon_jameed")
def _jameed(a):
    def g(a, cx, cy, rx, ry):
        swirl(a, cx, cy, rx * 0.5, ry * 0.5, "#E3D3A6", 1.2, 0.9)
        a.fill(ellipse(cx + 4, cy, 6, 1.6), "#F6D86A", 0.5)
    ramekin(a, "#F3E8C6", True, g)
    a.stroke(smooth([(66, 50), (78, 34), (84, 22)], closed=False), "#B9BEC6", 2.6)


@art("addon_chicken")
def _addon_chicken(a):
    cx, cy, rx, ry = side_plate(a)
    g = rnd(5)
    for i in range(9):
        x, y = cx + g.uniform(-rx * 0.6, rx * 0.6), cy - 2 + g.uniform(-ry * 0.5, ry * 0.4)
        d = g.uniform(-40, 40)
        a.fill(rrect(x - 4.4, y - 3.4, 8.8, 7.2, 2.4), "#C9772F")
        a.fill(rrect(x - 4.4, y - 4, 8.8, 6.6, 2.4), "#ECA650")
        a.stroke(poly([(x - 2.8, y - 2.4), (x + 1.4, y + 1.4)], closed=False), "#7A3F18", 1.0, 0.55)
    leaves(a, PARSLEY, cx, cy - 2, rx * 0.5, ry * 0.4, 5, 3, 1.2)


@art("addon_meat")
def _addon_meat(a):
    cx, cy, rx, ry = side_plate(a)
    for i, (x, y) in enumerate([(36, cy - 2), (48, cy - 6), (60, cy - 3), (42, cy + 4), (56, cy + 4)]):
        a.fill(blob(x, y + 1.4, 6.6, 5, i + 3, 0.16, 8), "#5E2E18")
        a.fill(blob(x, y, 6.4, 4.8, i + 3, 0.16, 8), "#8E4B2E")
        a.fill(blob(x - 1.4, y - 1.6, 3.2, 1.8, i + 9, 0.2, 6), "#B4714A", 0.85)
    leaves(a, PARSLEY, cx, cy - 2, rx * 0.5, ry * 0.4, 5, 3, 1.2)


@art("addon_kofta")
def _addon_kofta(a):
    cx, cy, rx, ry = side_plate(a)
    for x, y in [(42, cy - 3), (56, cy - 1)]:
        kofta_finger(a, x, y, 22, 8, -14)
        grill_marks(a, x, y, 5, 16, 76, "#3B1A0C", 3, 1.1, 0.55)
    leaves(a, PARSLEY, 64, cy + 4, 5, 2, 5, 4, 1.2)


@art("addon_meat_topping")
def _meat_topping(a):
    cx, cy, rx, ry = side_plate(a)
    g = rnd(8)
    a.fill(mound(50, cy + 1, 44, 8, 3, 7, 0.2, 2), "#6A3418")
    for _ in range(40):
        t = g.uniform(0, 2 * math.pi)
        k = math.sqrt(g.uniform(0, 1))
        x, y = 50 + math.cos(t) * 20 * k, cy - 3 + math.sin(t) * 5 * k
        a.fill(blob(x, y, g.uniform(1.3, 2.1), g.uniform(1.0, 1.5), g.randint(0, 99), 0.3, 6),
               g.choice(["#8A4A26", "#A0582E", "#73391C", "#B66B3C"]))
    dots(a, "#F7E7B8", 50, cy - 4, 16, 4, 12, 0.9, 4)


@art("addon_beans")
def _addon_beans(a):
    def g(a, cx, cy, rx, ry):
        heap(a, cx, cy, rx * 0.75, ry * 0.6, 18, 4, lambda a, x, y, d, s: (
            a.fill(capsule(x, y, 6, 3.2, d), "#F2EAD8"), a.fill(capsule(x - 0.4, y - 0.5, 3.6, 1.1, d), "#FFFFFF", 0.8)))
    ramekin(a, "#C2432C", False, g)


@art("addon_peas")
def _addon_peas(a):
    def g(a, cx, cy, rx, ry):
        heap(a, cx, cy - 1, rx * 0.8, ry * 0.7, 40, 6, lambda a, x, y, d, s: (
            a.fill(circle(x, y, 2.2), "#4E9B2C"), a.fill(circle(x - 0.6, y - 0.6, 0.9), "#9AD266")))
    ramekin(a, "#6FAE3E", False, g)


def pine_nut(a, x, y, d, s):
    a.fill(ellipse_rot(x, y, 2.2, 1.2, d), "#F2DCA0")
    a.fill(ellipse_rot(x - 0.4, y - 0.3, 1.0, 0.5, d), "#FFF4D6", 0.9)


def almond_flake(a, x, y, d, s):
    a.fill(ellipse_rot(x, y, 3.4, 1.8, d), "#E3BC80")
    a.fill(ellipse_rot(x - 0.6, y - 0.4, 1.8, 0.7, d), "#F6DCAA", 0.9)


@art("addon_pine_nuts")
def _pine_nuts(a):
    cx, cy, rx, ry = small_bowl(a, "#EADFC6", rim_y=58, rx=28, ry=8.4, depth=18, body=SLATE)
    heap(a, cx, cy - 1, rx * 0.8, ry * 0.8, 40, 3, pine_nut)


@art("addon_nuts_pine")
def _nuts_pine(a):
    cx, cy, rx, ry = small_bowl(a, "#EADFC6", rim_y=58, rx=28, ry=8.4, depth=18, body=SLATE)
    heap(a, cx, cy - 1, rx * 0.8, ry * 0.8, 30, 4, lambda a, x, y, d, s: (almond_flake if s % 2 else pine_nut)(a, x, y, d, s))


@art("addon_nutella")
def _choc_spread(a):
    jar(a, "#4A2414", "#F4F4F4", label="#FFFFFF", top=28, bottom=86, w=46)
    a.fill(blob(50, 67, 6, 5.4, 3, 0.1, 9), "#8A5A30")
    a.fill(ellipse(48.6, 65.6, 2, 1.4), "#B9875A")
    a.fill(capsule(50, 61.4, 4, 1.4, 90), "#5A3A1A")


@art("addon_milk")
def _addon_milk(a):
    glass(a, 50, "#FBFBF8", top=24, bottom=86, wt=34, wb=27, level=0.8)


@art("addon_extra_cheese")
def _extra_cheese(a):
    cx, cy, rx, ry = side_plate(a)
    g = rnd(4)
    shreds = []
    for _ in range(60):
        x, y = 50 + g.gauss(0, 9), cy - 4 + g.gauss(0, 3)
        shreds.append((y, x, g.uniform(-70, 70), g.choice(["#F6C14A", "#F9D46E", "#EFB23A"])))
    for y, x, d, col in sorted(shreds):
        a.fill(capsule(x, y, g.uniform(5, 8), 1.6, d), col)


@art("addon_cheese")
def _addon_cheese(a):
    ground(a, 30, 84)
    for i, (dx, dy, d) in enumerate([(-6, 4, -8), (6, -4, 6)]):
        x, y = 50 + dx, 60 + dy
        pts = rot_pts([(x - 18, y - 12), (x + 18, y - 12), (x + 18, y + 12), (x - 18, y + 12)], x, y, d)
        a.fill(poly([(px, py + 2.6) for px, py in pts]), "#D9921E")
        a.fill(poly(pts), "#F6C14A")
        for hx, hy, r in [(x - 8, y - 4, 2.6), (x + 6, y + 4, 2), (x + 10, y - 6, 1.6)]:
            a.fill(ellipse(hx, hy, r, r * 0.8), "#E8A82E")


@art("addon_feta")
def _feta(a):
    cx, cy, rx, ry = side_plate(a)
    for i, (x, y) in enumerate([(38, cy - 3), (50, cy - 6), (62, cy - 3), (44, cy + 3), (57, cy + 3)]):
        a.fill(poly([(x - 5, y - 2), (x + 3, y - 4.4), (x + 7, y - 1), (x + 7, y + 4), (x - 1, y + 6.4), (x - 5, y + 3)]), "#E6E0D2")
        a.fill(poly([(x - 5, y - 2), (x + 3, y - 4.4), (x + 7, y - 1), (x - 1, y + 1.4)]), "#FFFDF6")
    a.fill(ellipse(50, cy, 16, 4), OIL, 0.3)
    dots(a, "#4E7A2B", 50, cy - 2, 16, 5, 18, 0.45, 4)


@art("addon_vegetables")
def _addon_veg(a):
    cx, cy, rx, ry = side_plate(a)
    lettuce_leaf(a, 38, cy - 3, 13, -10, "#7DC04B", 3)
    tomato_slice(a, 60, cy - 4, 9, 5.6)
    cucumber_slice(a, 46, cy + 4, 6, 3.6)
    cucumber_slice(a, 60, cy + 5, 6, 3.6)
    a.stroke(ellipse(70, cy + 1, 6, 3.4), "#B98AC9", 1.6)
    a.stroke(ellipse(70, cy + 1, 4, 2.2), "#E3CDEB", 1.0)


@art("addon_pickles")
def _addon_pickles(a):
    cx, cy, rx, ry = side_plate(a)
    for i, (x, y) in enumerate([(36, cy - 2), (46, cy - 5), (56, cy - 2), (44, cy + 4)]):
        a.fill(ellipse(x, y + 0.8, 6, 3.6), "#4E7A22")
        a.fill(ellipse(x, y, 6, 3.6), "#8DAE4A")
        a.fill(ellipse(x, y, 4, 2.3), "#B9CF7A")
        dots(a, "#E3EBC2", x, y, 2, 1.2, 3, 0.4, i + 3)
    for x, y, d in [(64, cy - 3, 70), (68, cy + 3, 100)]:
        a.fill(capsule(x, y, 14, 4, d), "#D9557E")
        a.fill(capsule(x - 0.5, y, 10, 1.4, d), "#F08AAE", 0.8)


@art("addon_granola")
def _granola(a):
    def d(a, x, y, deg, s):
        g = rnd(s)
        col = g.choice(["#C98A3A", "#B8711E", "#DDA055", "#8A4A26"])
        a.fill(blob(x, y, g.uniform(2, 3.4), g.uniform(1.6, 2.6), s, 0.3, 6), col)
        if s % 4 == 0:
            a.fill(ellipse_rot(x, y, 1.8, 1, deg), "#6E2E3A")
    cx, cy, rx, ry = small_bowl(a, "#C99A5A", rim_y=58, rx=28, ry=8.4, depth=18, body=CERAMIC)
    heap(a, cx, cy - 1, rx * 0.8, ry * 0.8, 34, 5, d)


@art("addon_fruits")
def _addon_fruits(a):
    cx, cy, rx, ry = small_bowl(a, "#FBF6EA", rim_y=58, rx=30, ry=9, depth=20, body="#8DB8E0")
    for i, (x, y) in enumerate([(cx - 12, cy - 2), (cx + 2, cy + 1)]):
        a.fill(ellipse(x, y, 4, 2.6), "#FBF1C8")
        a.fill(ellipse(x, y, 1.4, 0.9), "#D9C08A")
    a.fill(ellipse(cx + 12, cy - 2, 4.6, 3), "#7CB82F")
    a.fill(ellipse(cx + 12, cy - 2, 2, 1.3), "#EEF4D2")
    strawberry(a, cx - 4, cy - 5, 0.5, -20, 3)
    strawberry(a, cx + 8, cy - 7, 0.45, 25, 5)
    for x, y in [(cx - 18, cy), (cx + 18, cy + 1), (cx - 2, cy + 3)]:
        a.fill(circle(x, y, 1.9), "#3A3F8A")
        a.fill(circle(x - 0.5, y - 0.5, 0.6), "#8A90D2")


R["addon_ground_meat"] = R["beef_mince"]
