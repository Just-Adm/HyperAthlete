#!/usr/bin/env python3
"""
Exercise pictograms for Hyper Athlete.

Figures are posed with inverse/forward kinematics on fixed limb lengths (so frame A and frame B of an
animation always keep the same proportions) and exported as absolute points to
app/src/main/assets/program/art.json. The app draws them on a Compose Canvas in theme colours and
loops between frame A and B. Preview sheet:

    python3 tools/gen_art.py --preview /path/to/sheet.png [NAME ...]

Units: a 100 × 100 box, y grows downwards, the floor sits at y = 92.
Limb angles are absolute and measured from "straight down": 0 = down, 90 = forward (right),
180 = up, -90 = backward (left). The torso angle is measured from "straight up" (+ = leaning forward).
"""
import json, math, os, sys

FLOOR = 92
L = dict(torso=26, neck=10.5, upper=14, fore=13, thigh=18.5, shin=18.5, foot=6.5)


def v(a):
    r = math.radians(a)
    return math.sin(r), math.cos(r)


def add(p, a, length):
    dx, dy = v(a)
    return (p[0] + dx * length, p[1] + dy * length)


def ik(base, target, l1, l2, hint):
    """Middle joint of a two-bone chain reaching from base towards target."""
    dx, dy = target[0] - base[0], target[1] - base[1]
    d = math.hypot(dx, dy)
    d = max(abs(l1 - l2) + 0.01, min(l1 + l2 - 0.01, d))
    ang = math.atan2(dy, dx)
    off = math.acos(max(-1, min(1, (l1 * l1 + d * d - l2 * l2) / (2 * l1 * d))))
    c1 = (base[0] + math.cos(ang + off) * l1, base[1] + math.sin(ang + off) * l1)
    c2 = (base[0] + math.cos(ang - off) * l1, base[1] + math.sin(ang - off) * l1)
    if isinstance(hint, tuple):
        key = lambda c: -math.hypot(c[0] - hint[0], c[1] - hint[1])
    else:
        key = {"f": lambda c: c[0], "b": lambda c: -c[0], "u": lambda c: -c[1], "d": lambda c: c[1]}[hint]
    mid = max((c1, c2), key=key)
    # the end point lies on the chain direction from the middle joint
    ex, ey = target[0] - mid[0], target[1] - mid[1]
    el = math.hypot(ex, ey) or 1
    end = (mid[0] + ex / el * l2, mid[1] + ey / el * l2)
    return mid, end


class T:
    """IK target for a hand / ankle. hint = where the elbow / knee bends: f b u d or a point."""
    def __init__(self, x, y, hint="f", toe=90):
        self.p, self.hint, self.toe = (x, y), hint, toe


class A:
    """Absolute angles (upper, lower[, toe])."""
    def __init__(self, u, f, toe=90):
        self.u, self.f, self.toe = u, f, toe


class R(A):
    """Angles relative to the torso: an arm posed for an upright body follows when it leans."""


def fig(P=None, t=0, head=0, arm=None, arm2=None, leg=None, leg2=None, front=False, S=None, ground=False, lift=0):
    if S is not None and P is not None:
        t = math.degrees(math.atan2(S[0] - P[0], -(S[1] - P[1])))
    elif S is not None:
        P = (S[0] - math.sin(math.radians(t)) * L["torso"], S[1] + math.cos(math.radians(t)) * L["torso"])
    tr = math.radians(t)
    S = (P[0] + math.sin(tr) * L["torso"], P[1] - math.cos(tr) * L["torso"])
    hr = math.radians(t + head)
    j = {"h": (S[0] + math.sin(hr) * L["neck"], S[1] - math.cos(hr) * L["neck"]), "s": S, "p": P}
    if front:
        px, py = math.cos(tr), math.sin(tr)
        j["s1"] = (S[0] - px * 8, S[1] - py * 8); j["s2"] = (S[0] + px * 8, S[1] + py * 8)
        j["p1"] = (P[0] - px * 5, P[1] - py * 5); j["p2"] = (P[0] + px * 5, P[1] + py * 5)
    arm = arm or R(0, 0)
    if arm2 is None:
        arm2 = shade(arm, front)
    leg = leg or A(0, 0)
    if leg2 is None:
        leg2 = shade(leg, front)
    for i, spec in (("1", arm), ("2", arm2)):
        base = j.get("s" + i, S) if front else S
        e, w = chain(base, spec, L["upper"], L["fore"], t)
        j["e" + i], j["w" + i] = e, w
    for i, spec in (("1", leg), ("2", leg2)):
        base = j.get("p" + i, P) if front else P
        k, a = chain(base, spec, L["thigh"], L["shin"], t, is_leg=True)
        j["k" + i], j["a" + i] = k, a
        j["t" + i] = add(a, spec.toe if not isinstance(spec, R) else spec.toe - t, L["foot"])
    if ground:
        low = max(j["a1"][1], j["a2"][1], j["t1"][1], j["t2"][1])
        dy = (FLOOR - lift) - low
        j = {k: (x, y + dy) for k, (x, y) in j.items()}
    return j


def shade(spec, front):
    """Far limb: same pose nudged so it peeks out behind the near one."""
    if front:
        return spec
    if isinstance(spec, T):
        return T(spec.p[0] - 2.5, spec.p[1], spec.hint, spec.toe)
    cls = type(spec)
    return cls(spec.u - 7, spec.f - 7, spec.toe)


def chain(base, spec, l1, l2, t, is_leg=False):
    if isinstance(spec, T):
        return ik(base, spec.p, l1, l2, spec.hint)
    u, f = (spec.u - t, spec.f - t) if isinstance(spec, R) else (spec.u, spec.f)
    m = add(base, u, l1)
    return m, add(m, f, l2)


def mirror(j):
    return {k: (100 - x, y) for k, (x, y) in j.items()}


def move(j, dx=0, dy=0):
    return {k: (x + dx, y + dy) for k, (x, y) in j.items()}


ART = {}


def art(name, frames, gear=(), front=False, floor=True):
    ART[name] = {"frames": frames, "gear": [g for g in flat(gear)], "front": front, "floor": floor}


def flat(items):
    for g in items:
        if isinstance(g, list):
            yield from flat(g)
        else:
            yield g


# ---- gear ------------------------------------------------------------------------------------
def line(a, b, w=3.5, c="gear", z="back"):
    return {"t": "line", "a": a, "b": b, "w": w, "c": c, "z": z}


def disc(at, r, c="gear", z="front"):
    return {"t": "disc", "at": at, "r": r, "c": c, "z": z}


def ring(at, r, w=2.5, c="gear", z="front"):
    return {"t": "ring", "at": at, "r": r, "w": w, "c": c, "z": z}


def rect(x, y, w, h, r=2, c="gear2", z="back"):
    return {"t": "rect", "x": x, "y": y, "w": w, "h": h, "r": r, "c": c, "z": z}


def pill(at, w, h, c="gear", z="front"):
    return {"t": "pill", "at": at, "w": w, "h": h, "c": c, "z": z}


def poly(points, c="gear2", z="back", points2=None):
    g = {"t": "poly", "pts": points, "c": c, "z": z}
    if points2:
        g["pts2"] = points2
    return g


def kb(at, r=5.2, z="front"):
    return {"t": "kb", "at": at, "r": r, "c": "gear", "z": z}


def plate(at, r=9.5, z="front"):
    return [disc(at, r, "gear", z), disc(at, r * 0.3, "gear2", z)]


def dumbbell(at="w1", z="front"):
    return [disc(at, 4.6, "gear", z), disc(at, 1.5, "gear2", z)]


def bench(x0, x1, top, legs=True):
    g = [rect(x0, top, x1 - x0, 5, 2.5)]
    if legs:
        g += [line([x0 + 5, top + 5], [x0 + 5, FLOOR], 2.6, "gear2"), line([x1 - 5, top + 5], [x1 - 5, FLOOR], 2.6, "gear2")]
    return g


SEAT_Y = 75          # seat top for seated machines
SIT_P = SEAT_Y - 4   # pelvis height when seated


def seat(x0=34, x1=60, back=True):
    g = [rect(x0, SEAT_Y, x1 - x0, 5, 2.5), line([(x0 + x1) / 2, SEAT_Y + 5], [(x0 + x1) / 2, FLOOR], 2.8, "gear2")]
    if back:
        g.append(line([x0 + 1, SEAT_Y], [x0 + 1, SEAT_Y - 32], 4.5, "gear2"))
    return g


def sit(x=46, t=0, arm=None, arm2=None, leg=None, **kw):
    return fig((x, SIT_P), t, arm=arm, arm2=arm2, leg=leg or A(90, 0), **kw)


def rot(points, center, deg):
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    return [[round(center[0] + (x - center[0]) * c - (y - center[1]) * s, 1), round(center[1] + (x - center[0]) * s + (y - center[1]) * c, 1)] for x, y in points]


# ---- Chest + Biceps ----------------------------------------------------------------------------
art("BENCH", [
    fig((58, 58), -90, arm=T(37, 32, "d"), leg=T(75, 90, "u")),
    fig((58, 58), -90, arm=T(38, 47, "d"), leg=T(75, 90, "u")),
], [bench(14, 70, 63), plate("w1")])

art("INCLINE", [
    fig((56, 61), -56, arm=T(44, 21, "d"), leg=T(74, 90, "u")),
    fig((56, 61), -56, arm=T(39, 37, "d"), leg=T(74, 90, "u")),
], [poly([[50, 66], [64, 66], [64, 70], [50, 70]]), line([52, 65], [31, 47], 5, "gear2"),
    line([57, 70], [57, FLOOR], 2.6, "gear2"), line([38, 54], [44, FLOOR], 2.6, "gear2"), dumbbell()])

art("FLY", [
    fig((50, 55), front=True, arm=T(14, 34, "u"), arm2=T(86, 34, "u"), leg=A(-4, -2), leg2=A(4, 2)),
    fig((50, 55), front=True, arm=T(47, 42, "b"), arm2=T(53, 42, "f"), leg=A(-4, -2), leg2=A(4, 2)),
], [line([5, 4], [5, FLOOR], 3, "gear2"), line([95, 4], [95, FLOOR], 3, "gear2"),
    line([7, 14], "w1", 1.5, "gear", "back"), line([93, 14], "w2", 1.5, "gear", "back"),
    disc([7, 14], 2.2, "gear", "back"), disc([93, 14], 2.2, "gear", "back")], front=True)

art("CURL", [sit(arm=R(3, 6)), sit(arm=R(3, 152))], [seat(), dumbbell()])
art("HAMMER", [sit(arm=R(3, 4)), sit(arm=R(3, 140))],
    [seat(), {"t": "line", "a": ["w1", -1, -4.5], "b": ["w1", 1, 4.5], "w": 4.4, "c": "gear", "z": "front"}])

# ---- Back + Triceps ----------------------------------------------------------------------------
art("PULLDOWN", [
    sit(50, -8, arm=T(55, 19, "f")),
    sit(50, -12, arm=T(57, 44, "d")),
], [line([30, 4], [78, 4], 3, "gear2"), line([74, 4], [74, FLOOR], 3.2, "gear2"), disc([56, 4], 2.4, "gear", "back"),
    line([56, 4], ["w1", 0, 0], 1.4, "gear", "back"), line(["w1", -10, 0], ["w1", 10, 0], 3.2, "gear", "front"),
    seat(38, 62, back=False), rect(60, 62, 10, 6, 3)])

art("ROW", [
    fig((38, 54), 54, head=-12, arm=T(64, 74, "f"), leg=T(35, 90, "f"), ground=True, lift=4),
    fig((38, 54), 54, head=-12, arm=T(54, 56, "b"), leg=T(35, 90, "f"), ground=True, lift=4),
], [line([50, 69], [70, 52], 5.5, "gear2"), line([60, 62], [60, FLOOR], 2.8, "gear2"), rect(26, 88, 20, 4, 1.5), plate("w1", 7.5)])

art("BACK_EXT", [
    fig((52, 60), 45, head=-5, arm=R(28, 168), leg=T(26, 86, "u", toe=0)),
    fig((52, 60), 150, head=10, arm=R(28, 168), leg=T(26, 86, "u", toe=0)),
], [line([47, 69], [62, 57], 5.5, "gear2"), line([53, 66], [40, FLOOR], 3, "gear2"), line([18, 91], [34, 84], 3.4, "gear2")])

art("DIPS", [
    fig(S=(48, 23), t=6, arm=T(50, 50, "b"), leg=A(14, -95, -10)),
    fig(S=(54, 36), t=16, arm=T(50, 50, "b"), leg=A(22, -88, -10)),
], [line([28, 50], [72, 50], 3.4, "gear2"), line([32, 50], [32, FLOOR], 3, "gear2"), line([68, 50], [68, FLOOR], 3, "gear2")])

art("OVERHEAD_EXT", [
    fig((52, 55), 20, head=-8, arm=R(172, -25), leg=T(62, FLOOR, "f"), leg2=T(38, FLOOR, "f", toe=120), ground=True),
    fig((52, 55), 20, head=-8, arm=R(165, 165), leg=T(62, FLOOR, "f"), leg2=T(38, FLOOR, "f", toe=120), ground=True),
], [line([4, 4], [4, FLOOR], 3, "gear2"), disc([6, 72], 2.2, "gear", "back"), line([6, 72], "w1", 1.5, "gear", "back")])

# ---- Shoulders + Traps -------------------------------------------------------------------------
art("SHOULDER_PRESS", [sit(48, arm=R(6, 178)), sit(48, arm=R(166, 172))], [seat(36, 60), dumbbell()])

art("LATERAL", [
    fig((50, 55), front=True, arm=A(-8, -5), arm2=A(8, 5), leg=A(-4, -2), leg2=A(4, 2)),
    fig((50, 55), front=True, arm=A(-86, -82), arm2=A(86, 82), leg=A(-4, -2), leg2=A(4, 2)),
], [dumbbell("w1"), dumbbell("w2")], front=True)

art("FACE_PULL", [
    fig((48, 55), -4, arm=T(74, 27, "d"), leg=T(58, FLOOR, "f"), leg2=T(38, FLOOR, "f", toe=120), ground=True),
    fig((48, 55), -8, arm=T(58, 21, "b"), leg=T(58, FLOOR, "f"), leg2=T(38, FLOOR, "f", toe=120), ground=True),
], [line([97, 2], [97, FLOOR], 3, "gear2"), disc([95, 24], 2.2, "gear", "back"), line([95, 24], "w1", 1.5, "gear", "back")])


def shrug(up):
    j = fig((50, 55), front=True, arm=A(-4, -2), arm2=A(4, 2), leg=A(-4, -2), leg2=A(4, 2))
    if up:
        for k in ("s", "s1", "s2", "e1", "w1", "e2", "w2"):
            j[k] = (j[k][0], j[k][1] - 4.5)
        j["h"] = (j["h"][0], j["h"][1] - 1.2)
    return j


art("SHRUG", [shrug(False), shrug(True)], [dumbbell("w1"), dumbbell("w2")], front=True)


def walk(step, arm=None, arm2=None, t=3):
    near, far = (T(62, FLOOR, "f"), T(39, FLOOR - 2, "f", toe=125)) if step else (T(39, FLOOR - 2, "f", toe=125), T(62, FLOOR, "f"))
    return fig((50, 57), t, arm=arm or A(3, 0), arm2=arm2, leg=near, leg2=far, ground=True)


art("FARMER", [walk(True), walk(False)], [dumbbell()])
art("SUITCASE", [walk(True, arm=A(0, 0)), walk(False, arm=A(0, 0))], [kb(["w1", 0, 4])])
art("SANDBAG", [walk(True, arm=T(56, 47, "f"), t=-4), walk(False, arm=T(56, 47, "f"), t=-4)],
    [pill(["s", 10, 17], 15, 23, "gear", "mid")])

# ---- Outdoor -----------------------------------------------------------------------------------
art("SNAP_DOWN", [
    fig((50, 52), 0, arm=R(150, 158), leg=T(51, FLOOR - 4, "f", toe=140), ground=True),
    fig((44, 70), 34, arm=R(-45, -35), leg=T(53, FLOOR, "f"), ground=True),
])

art("BOX_JUMP", [
    fig((28, 70), 38, arm=R(-50, -40), leg=T(36, FLOOR, "f"), ground=True),
    fig((68, 52), 12, arm=R(60, 75), leg=T(72, 80, "f"), ground=True, lift=12),
], [rect(56, 80, 28, 12, 3)])

art("BROAD_JUMP", [
    fig((26, 70), 40, arm=R(-55, -45), leg=T(34, FLOOR, "f"), ground=True),
    move(fig((62, 52), 30, arm=R(118, 128), leg=A(30, -30, 60)), 0, 4),
])

SLED = [poly([[68, 62], [90, 62], [93, 88], [65, 88]], "gear"), line([62, 89.5], [97, 89.5], 3, "gear"),
        line([69, 44], [69, 75], 3.4, "gear")]


def sled_push(step):
    near, far = (T(52, FLOOR, "f"), T(22, FLOOR, "f", toe=130)) if step else (T(22, FLOOR, "f", toe=130), T(52, FLOOR, "f"))
    return fig((40, 60), 62, head=-32, arm=T(68, 48, "d"), arm2=T(68, 51, "d"), leg=near, leg2=far)


art("SLED_PUSH", [sled_push(True), sled_push(False)], SLED)


def sled_drag(step):
    near, far = (T(44, FLOOR, "f"), T(24, FLOOR - 2, "f", toe=125)) if step else (T(24, FLOOR - 2, "f", toe=125), T(44, FLOOR, "f"))
    return fig((34, 57), -16, arm=T(56, 58, "d"), leg=near, leg2=far, ground=True)


art("SLED_DRAG", [sled_drag(True), sled_drag(False)],
    [poly([[74, 66], [92, 66], [95, FLOOR - 4], [71, FLOOR - 4]], "gear"), line([68, 89.5], [97, 89.5], 3, "gear"),
     line(["w1", 0, 0], [74, 74], 1.6, "gear", "back")])

art("KB_DEADLIFT", [
    fig((40, 66), 66, head=-25, arm=A(0, 0), leg=T(50, FLOOR, "f"), ground=True),
    fig((48, 55), 0, arm=A(0, 0), leg=T(49, FLOOR, "f"), ground=True),
], [kb(["w1", 0, 4])])

art("RDL", [
    fig((48, 55), 2, arm=A(2, 0), leg=T(49, FLOOR, "f"), ground=True),
    fig((40, 57), 76, head=-30, arm=A(4, 2), leg=T(50, FLOOR, "f"), ground=True),
], [plate("w1", 9)])

art("KB_SWING", [
    fig((40, 64), 60, head=-25, arm=A(-28, -25), leg=T(50, FLOOR, "f"), ground=True),
    fig((48, 55), -4, arm=A(90, 92), leg=T(49, FLOOR, "f"), ground=True),
], [kb(["w1", 2, 3])])

TIRE = [[56, 78], [90, 78], [90, 92], [56, 92]]
art("TIRE_FLIP", [
    fig((36, 70), 62, head=-30, arm=T(56, 88, "d"), leg=T(42, FLOOR, "f"), ground=True),
    fig((50, 58), 22, head=-10, arm=T(70, 60, "d"), leg=T(56, FLOOR, "f"), leg2=T(32, FLOOR, "f", toe=125), ground=True),
], [poly(TIRE, "gear", "mid", rot(TIRE, (90, 92), 36)), rect(62, 83.5, 22, 3, 1.5, "gear2", "front")])

art("WALL_BALL", [
    fig((44, 74), 30, arm=R(20, 160), leg=T(53, FLOOR, "f"), ground=True),
    fig((50, 55), -6, arm=R(146, 152), leg=T(51, FLOOR - 3, "f", toe=130), ground=True),
], [disc(["w1", 4, -3], 6.4, "gear", "front"), line([93, 2], [93, FLOOR], 3, "gear2"), ring([89, 12], 5, 2.2, "acc", "back")])


def hang_bar(j, bar_y=8):
    return move(j, 0, bar_y - min(j["w1"][1], j["w2"][1]))


art("KNEE_RAISE", [
    hang_bar(fig((50, 46), 0, head=-12, arm=A(170, 172), leg=A(4, 0, 60))),
    hang_bar(fig((50, 46), -6, head=-12, arm=A(170, 172), leg=A(98, 4, 70))),
], [line([20, 8], [84, 8], 3.2, "gear2")], floor=False)

art("PULL_UP", [
    hang_bar(fig((50, 58), 0, front=True, arm=T(34, 8, "b"), arm2=T(66, 8, "f"), leg=A(-2, -14, 20), leg2=A(2, -12, -20))),
    hang_bar(fig((50, 40), 0, front=True, arm=T(34, 8, "b"), arm2=T(66, 8, "f"), leg=A(-2, -14, 20), leg2=A(2, -12, -20))),
], [line([14, 8], [86, 8], 3.2, "gear2")], front=True, floor=False)


def monkey(step):
    a1, a2 = (A(205, 182), A(152, 178)) if step else (A(152, 178), A(205, 182))
    l1, l2 = (A(14, -12, 60), A(-10, -30, 60)) if step else (A(-10, -30, 60), A(14, -12, 60))
    return fig((50, 46), 0, head=-5, arm=a1, arm2=a2, leg=l1, leg2=l2)


art("MONKEY_BARS", [hang_bar(monkey(True)), hang_bar(monkey(False))],
    [line([4, 8], [96, 8], 3, "gear2")] + [disc([x, 8], 2.2, "gear2", "back") for x in range(10, 96, 13)], floor=False)

# ---- Core --------------------------------------------------------------------------------------
art("DEAD_BUG", [
    fig((54, 85), -90, arm=A(180, 180), arm2=A(176, 176), leg=A(180, 90, 180), leg2=A(176, 86, 176)),
    fig((54, 85), -90, arm=A(262, 266), arm2=A(176, 176), leg=A(180, 90, 180), leg2=A(98, 94, 180)),
], [disc(["k1", -3, -8], 4.4, "gear", "front")])


def copenhagen(up):
    S = (30, 75)
    P = (54, 72) if up else (54, 79)
    j = fig(P, S=S, arm=A(0, 90), arm2=A(160, 175), leg=T(91, 68, "u", toe=180), leg2=T(72, 88, "d", toe=90))
    return j


art("COPENHAGEN", [copenhagen(False), copenhagen(True)], [rect(82, 72, 16, 20, 2.5)])

# ---- Legs --------------------------------------------------------------------------------------
HEEL = [pill(["a1", -1.5, 4.2], 13, 4.6, "gear", "back")]
art("GOBLET", [
    fig((50, 51), 0, arm=R(22, 162), leg=T(52, 87, "f", toe=118), ground=True, lift=1),
    fig((44, 74), 32, head=-12, arm=R(22, 162), leg=T(54, 87, "f", toe=118), ground=True, lift=1),
], [HEEL, disc(["w1", 3, -1], 5, "gear", "front")])

art("BACK_SQUAT", [
    fig((50, 51), 4, arm=R(-38, 155), leg=T(52, 87, "f", toe=118), ground=True, lift=1),
    fig((43, 73), 40, head=-20, arm=R(-38, 155), leg=T(54, 87, "f", toe=118), ground=True, lift=1),
], [HEEL, plate(["s", -4, 0], 8, "mid")])

art("BULGARIAN", [
    fig((46, 56), 6, arm=A(1, 0), leg=T(58, FLOOR, "f"), leg2=T(20, 64, "d", toe=-75)),
    fig((42, 70), 10, arm=A(1, 0), leg=T(58, FLOOR, "f"), leg2=T(20, 64, "d", toe=-75)),
], [bench(4, 30, 68), dumbbell()])

art("ADDUCTION", [
    fig((50, SIT_P), front=True, arm=A(-25, -5), arm2=A(25, 5), leg=T(16, 86, "u", toe=-90), leg2=T(84, 86, "u", toe=90)),
    fig((50, SIT_P), front=True, arm=A(-25, -5), arm2=A(25, 5), leg=T(40, FLOOR, "b", toe=-90), leg2=T(60, FLOOR, "f", toe=90)),
], [rect(28, SEAT_Y, 44, 6, 3), line([50, SEAT_Y + 6], [50, FLOOR], 3, "gear2"),
    disc(["k1", 3, 0], 3.4, "gear", "front"), disc(["k2", -3, 0], 3.4, "gear", "front")], front=True)

art("LEG_CURL", [
    sit(44, -6, arm=R(20, 60), leg=A(90, 90, 180)),
    sit(44, -6, arm=R(20, 60), leg=A(90, -12, 75)),
], [seat(30, 56), disc(["a1", 0, 3], 3.6, "gear", "front")])

# ---- Mobility ----------------------------------------------------------------------------------
WALL_R = [line([88, 2], [88, FLOOR], 4, "gear2")]
art("KNEE_TO_WALL", [
    fig((58, 58), 4, arm=T(85, 44, "d"), leg=T(72, FLOOR, "f"), leg2=T(36, FLOOR - 2, "f", toe=125), ground=True),
    fig((63, 61), 6, arm=T(85, 44, "d"), leg=T(72, FLOOR, "f"), leg2=T(36, FLOOR - 2, "f", toe=125), ground=True),
], WALL_R)

art("TIB_RAISE", [
    fig((38, 56), -8, arm=A(-6, -4), leg=T(48, FLOOR, "f", toe=92), ground=True),
    fig((38, 56), -8, arm=A(-6, -4), leg=T(48, FLOOR, "f", toe=148), ground=True),
], [line([28, 2], [28, FLOOR], 4, "gear2")])

art("CALF_STRETCH", [
    fig((50, 56), 22, arm=T(86, 34, "d"), leg=T(64, FLOOR, "f"), leg2=T(30, FLOOR, "u"), ground=True),
    fig((50, 60), 18, arm=T(86, 34, "d"), leg=T(64, FLOOR, "f"), leg2=T(32, FLOOR, "f"), ground=True),
], WALL_R)

art("COUCH", [
    fig((46, 70), 14, arm=R(40, 70), leg=T(30, 64, "d", toe=180), leg2=T(68, FLOOR, "u")),
    fig((44, 68), -4, arm=R(10, 0), leg=T(30, 64, "d", toe=180), leg2=T(68, FLOOR, "u")),
], [line([26, 34], [26, FLOOR], 4, "gear2"), rect(28, 88, 18, 4, 1.5)])


def ham(up):
    j = fig((52, 86), -90, arm=A(160, 120), leg=A(150 if not up else 168, 150 if not up else 168, 240 if not up else 258),
            leg2=A(90, 90, 180))
    k, a = j["k1"], j["a1"]
    target = ((k[0] + a[0]) / 2, (k[1] + a[1]) / 2)
    e, w = ik(j["s"], target, L["upper"], L["fore"], "d")
    j["e1"], j["w1"] = e, w
    j["e2"], j["w2"] = (e[0] - 1.5, e[1]), (w[0] - 1.5, w[1])
    return j


art("HAM_STRETCH", [ham(False), ham(True)], [line(["w1", 0, 0], ["t1", 0, 0], 1.4, "gear", "front")])


def cossack(left):
    if left:
        j = fig((38, 74), 6, front=True, arm=T(46, 50, "b"), arm2=T(46, 50, "f"),
                leg=T(30, FLOOR, "b", toe=-90), leg2=T(80, FLOOR - 4, "d", toe=180))
    else:
        j = mirror(fig((38, 74), 6, front=True, arm=T(46, 50, "b"), arm2=T(46, 50, "f"),
                       leg=T(30, FLOOR, "b", toe=-90), leg2=T(80, FLOOR - 4, "d", toe=180)))
        j = {**j, "s1": j["s2"], "s2": j["s1"], "p1": j["p2"], "p2": j["p1"], "e1": j["e2"], "e2": j["e1"],
             "w1": j["w2"], "w2": j["w1"], "k1": j["k2"], "k2": j["k1"], "a1": j["a2"], "a2": j["a1"], "t1": j["t2"], "t2": j["t1"]}
    return j


art("COSSACK", [cossack(True), cossack(False)], [], front=True)

art("DEEP_SQUAT", [
    fig((42, 74), 22, arm=T(80, 54, "d"), leg=T(52, FLOOR, "f"), ground=True),
    fig((44, 80), 16, arm=T(80, 58, "d"), leg=T(52, FLOOR, "f"), ground=True),
], [line([82, 2], [82, FLOOR], 3.4, "gear2")])

art("WALL_SLIDE", [
    fig((50, 55), front=True, arm=A(-92, -178), arm2=A(92, 178), leg=A(-4, -2), leg2=A(4, 2)),
    fig((50, 55), front=True, arm=A(-150, -172), arm2=A(150, 172), leg=A(-4, -2), leg2=A(4, 2)),
], [rect(14, 2, 72, FLOOR - 2, 4, "gear2", "back")], front=True)

art("BALANCE", [
    fig((50, 55), front=True, arm=A(-62, -74), arm2=A(62, 74), leg=A(-2, 0), leg2=A(60, -12, 60)),
    fig((50, 55), front=True, arm=A(-48, -60), arm2=A(48, 60), leg=A(-2, 0), leg2=A(66, -8, 60)),
], [], front=True)

art("STRETCH", [
    fig((50, 55), 0, front=True, arm=A(-168, -176), arm2=A(168, 176), leg=A(-10, -10), leg2=A(10, 10), ground=True),
    fig((50, 55), -22, head=-6, front=True, arm=A(-158, -140), arm2=A(-178, -150), leg=A(-10, -10), leg2=A(10, 10), ground=True),
], [], front=True)


# ---- export ----------------------------------------------------------------------------------
def rnd(p):
    return [round(p[0], 1), round(p[1], 1)]


def export():
    return {name: {"frames": [{k: rnd(p) for k, p in f.items()} for f in a["frames"]], "gear": a["gear"],
                   "front": a["front"], "floor": a["floor"]} for name, a in ART.items()}


def resolve(p, f):
    if isinstance(p, str):
        return f[p]
    if isinstance(p, list) and isinstance(p[0], str):
        x, y = f[p[0]]
        return (x + p[1], y + p[2])
    return tuple(p)


def render(data, path, names=None, scale=4, cols=8, bg=(255, 255, 255)):
    from PIL import Image, ImageDraw
    names = names or list(data)
    cell = 100 * scale
    rows = (len(names) * 2 + cols - 1) // cols
    img = Image.new("RGB", (cols * cell, rows * (cell + 30)), bg)
    d = ImageDraw.Draw(img)
    colors = {"fig": (255, 107, 0), "fig2": (255, 194, 156), "gear": (60, 64, 72), "gear2": (200, 204, 212), "acc": (255, 107, 0)}
    i = 0
    for name in names:
        a = data[name]
        for fi, f in enumerate(a["frames"]):
            ox, oy = (i % cols) * cell, (i // cols) * (cell + 30)
            i += 1
            P = lambda p: (ox + p[0] * scale, oy + p[1] * scale)

            def seg(a_, b_, w, c):
                d.line([P(a_), P(b_)], fill=c, width=int(w * scale))
                for q in (a_, b_):
                    r = w * scale / 2
                    x, y = P(q)
                    d.ellipse([x - r, y - r, x + r, y + r], fill=c)

            def gear(z):
                for g in a["gear"]:
                    if g.get("z", "back") != z:
                        continue
                    c = colors[g["c"]]
                    t = g["t"]
                    if t == "line":
                        seg(resolve(g["a"], f), resolve(g["b"], f), g["w"], c)
                    elif t in ("disc", "ring", "kb"):
                        x, y = P(resolve(g["at"], f)); r = g["r"] * scale
                        if t == "ring":
                            d.ellipse([x - r, y - r, x + r, y + r], outline=c, width=int(g["w"] * scale))
                        else:
                            d.ellipse([x - r, y - r, x + r, y + r], fill=c)
                            if t == "kb":
                                d.ellipse([x - r * 0.62, y - r * 1.75, x + r * 0.62, y - r * 0.55], outline=c, width=int(1.8 * scale))
                    elif t == "rect":
                        d.rounded_rectangle([P((g["x"], g["y"])), P((g["x"] + g["w"], g["y"] + g["h"]))], radius=g["r"] * scale, fill=c)
                    elif t == "pill":
                        x, y = P(resolve(g["at"], f)); w, h = g["w"] * scale / 2, g["h"] * scale / 2
                        d.rounded_rectangle([x - w, y - h, x + w, y + h], radius=min(w, h), fill=c)
                    elif t == "poly":
                        pts = g["pts2"] if (fi == 1 and "pts2" in g) else g["pts"]
                        d.polygon([P(q) for q in pts], fill=c)

            def limb(pts, w, c, halo):
                if halo:
                    for q1, q2 in zip(pts, pts[1:]):
                        seg(q1, q2, w + 2.6, bg)
                for (q1, q2), ww in zip(zip(pts, pts[1:]), [w, w, 5][:len(pts) - 1]):
                    seg(q1, q2, ww if ww != 5 else 5, c)

            if a["floor"]:
                seg((4, FLOOR + 3), (96, FLOOR + 3), 1.2, (228, 230, 235))
            gear("back")
            far = colors["fig"] if a["front"] else colors["fig2"]
            limb([f.get("s2", f["s"]), f["e2"], f["w2"]], 6.4, far, False)
            limb([f.get("p2", f["p"]), f["k2"], f["a2"], f["t2"]], 6.8, far, False)
            gear("mid")
            fig_c = colors["fig"]
            if "s1" in f:
                d.polygon([P(f["s1"]), P(f["s2"]), P(f["p2"]), P(f["p1"])], fill=fig_c)
                seg(f["s1"], f["s2"], 6, fig_c); seg(f["p1"], f["p2"], 6, fig_c)
            else:
                seg(f["s"], f["p"], 9.5, fig_c)
            hx, hy = P(f["h"]); r = 6.2 * scale
            d.ellipse([hx - r, hy - r, hx + r, hy + r], fill=fig_c)
            halo = not a["front"]
            limb([f.get("p1", f["p"]), f["k1"], f["a1"], f["t1"]], 6.8, fig_c, halo)
            limb([f.get("s1", f["s"]), f["e1"], f["w1"]], 6.4, fig_c, halo)
            gear("front")
            d.text((ox + 8, oy + cell + 6), f"{name} {'AB'[fi]}", fill=(0, 0, 0))
    img.save(path)


if __name__ == "__main__":
    data = export()
    path = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "program", "art.json")
    with open(path, "w") as fh:
        json.dump(data, fh, separators=(",", ":"))
    print(len(data), "pictograms →", os.path.normpath(path), os.path.getsize(path), "bytes")
    if "--preview" in sys.argv:
        i = sys.argv.index("--preview")
        render(data, sys.argv[i + 1], sys.argv[i + 2:] or None)
