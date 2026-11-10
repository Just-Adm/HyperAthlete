#!/usr/bin/env python3
"""
Front / back body map for the progress page (brief §12.6: muscles coloured by weekly sets).

    python tools/gen_body.py                    # writes ui/progress/BodyArt.kt
    python tools/gen_body.py --preview x.html   # both views with sample colours, open in a browser

Each view is 100 × 210 (classic 8-head proportions: head 26, chin 25, shoulders 37, navel 80,
crotch 108, knees 155, soles 208). Every shape is drawn once for the figure's side at x > 50 and
mirrored, so left and right always match. A muscle may have several shapes (e.g. three quad heads)
and may appear on both views (side delts, forearms, obliques, neck).
"""
import math, os, sys

sys.path.insert(0, os.path.dirname(__file__))
from foodart import smooth, ellipse

W = 100


def mirror(pts):
    return [(W - x, y) for x, y in pts]


def both(*shapes, tension=0.8):
    out = []
    for pts in shapes:
        out.append(smooth(pts, tension=tension))
        out.append(smooth(mirror(pts), tension=tension))
    return out


def spindle(a, b, w, peak=0.5, off=0.0, n=7):
    """Muscle belly along the limb axis a→b: widest (w) at [peak], tapering to the tendons; [off] shifts it sideways."""
    ax, ay = a
    bx, by = b
    length = math.hypot(bx - ax, by - ay)
    ux, uy = (bx - ax) / length, (by - ay) / length
    nx, ny = uy, -ux  # perpendicular, pointing to the outside of a right-side limb
    side1, side2 = [], []
    for i in range(n):
        t = i / (n - 1)
        warped = t / peak * 0.5 if t <= peak else 0.5 + (t - peak) / (1 - peak) * 0.5
        hw = w / 2 * max(0.12, math.sin(math.pi * warped)) ** 0.7
        cx, cy = ax + (bx - ax) * t + nx * off, ay + (by - ay) * t + ny * off
        side1.append((cx + nx * hw, cy + ny * hw))
        side2.append((cx - nx * hw, cy - ny * hw))
    return side1 + list(reversed(side2))


# Limb axes (figure's side at x > 50), matching the silhouette below.
UPPER_ARM = ((77.6, 53.6), (81.0, 76.6))
FOREARM = ((81.8, 80.2), (85.6, 99.0))


def outline():
    """One closed silhouette (head separate): right half down the outside, back up the inside leg, then mirrored."""
    right = [
        (54.4, 23.5), (55.4, 30.0), (60.0, 32.4), (68.0, 34.8), (74.2, 37.2), (78.6, 41.4), (80.6, 47.8),
        (81.2, 55.0), (82.2, 64.0), (83.6, 73.0), (85.0, 80.0), (87.0, 90.0), (88.6, 100.0), (90.0, 106.0),
        (90.6, 114.0), (88.6, 121.0), (85.6, 120.6), (84.6, 113.0), (84.0, 106.0), (82.6, 100.4), (80.2, 90.0),
        (78.4, 81.0), (76.6, 70.0), (75.0, 60.0), (73.4, 52.6),
        (71.0, 58.0), (69.0, 68.0), (67.2, 78.0), (67.6, 88.0), (69.6, 97.0), (70.6, 106.0), (70.2, 118.0),
        (68.6, 132.0), (66.6, 146.0), (65.6, 154.0), (65.8, 162.0), (66.6, 172.0), (65.2, 184.0), (62.6, 196.0),
        (63.6, 202.0), (62.2, 207.0), (55.2, 207.6), (54.6, 203.0), (55.6, 196.0), (54.8, 184.0), (54.2, 172.0),
        (54.4, 162.0), (53.6, 152.0), (53.0, 140.0), (52.0, 124.0), (51.0, 112.0),
    ]
    left = [(W - x, y) for x, y in reversed(right)]
    body = smooth(right + [(50, 108.6)] + left + [(50, 23.2)], tension=0.6)
    head = ellipse(50, 13.5, 8.6, 11.2)
    return body, head


# ---------------------------------------------------------------- front view
FRONT = {
    "NECK": both([(54.6, 22.8), (55.6, 25.6), (53.6, 30.0), (51.6, 32.6), (50.7, 31.6), (52.6, 27.4)]),
    "TRAPS": both([(55.8, 27.4), (60.2, 31.6), (68.0, 34.6), (64.0, 35.6), (57.2, 33.6), (55.4, 31.0)]),
    "CHEST": both([(51.0, 37.6), (58.0, 36.4), (66.4, 37.2), (70.0, 42.6), (71.4, 49.6), (68.0, 55.0),
                   (61.0, 57.0), (54.0, 56.6), (51.0, 54.0)]),
    "FRONT_DELTS": both([(66.8, 36.0), (72.0, 37.0), (75.4, 40.2), (75.8, 46.0), (74.2, 52.0), (71.6, 49.0), (69.2, 43.6)]),
    "SIDE_DELTS": both([(74.6, 37.8), (78.4, 41.4), (80.4, 47.6), (79.6, 53.4), (76.8, 54.6), (76.6, 47.0), (76.2, 41.0)]),
    "SERRATUS": both([(67.6, 55.8), (70.8, 55.6), (71.0, 58.6), (67.6, 59.2)],
                     [(67.2, 60.0), (70.4, 59.8), (70.2, 63.2), (67.0, 63.6)],
                     [(66.8, 64.4), (69.8, 64.2), (69.2, 67.8), (66.8, 67.8)], tension=0.5),
    "ABS": both([(51.0, 58.0), (57.0, 57.4), (58.2, 63.6), (51.0, 64.4)],
                [(51.0, 65.4), (58.3, 64.8), (58.5, 72.4), (51.0, 73.0)],
                [(51.0, 74.0), (58.4, 73.6), (58.0, 81.4), (51.0, 82.0)],
                [(51.0, 83.0), (57.8, 82.6), (56.8, 92.0), (54.0, 99.0), (51.0, 101.4)], tension=0.45),
    "OBLIQUES": both([(59.6, 63.6), (66.6, 60.4), (67.4, 69.0), (66.6, 79.0), (67.6, 88.0), (65.8, 93.6),
                      (60.2, 92.4), (59.4, 80.0)]),
    "HIP_FLEXORS": both([(58.0, 96.6), (62.8, 97.0), (64.6, 101.0), (61.8, 106.2), (58.8, 108.2), (57.0, 103.0)]),
    "ADDUCTORS": both([(51.6, 111.0), (55.0, 108.4), (57.0, 113.0), (56.6, 123.0), (54.6, 128.0), (52.8, 124.6)]),
    "QUADS": both(
        # rectus femoris, vastus lateralis, vastus medialis
        [(58.6, 112.0), (62.6, 110.6), (64.4, 122.0), (63.4, 136.0), (61.0, 146.0), (58.6, 145.0), (57.6, 132.0), (57.6, 121.0)],
        [(64.8, 109.6), (69.6, 112.2), (69.0, 126.0), (67.0, 140.0), (65.0, 148.6), (62.6, 148.0), (64.8, 136.0), (65.6, 122.0)],
        [(55.0, 128.4), (57.2, 131.2), (58.4, 142.0), (58.6, 149.6), (55.8, 150.6), (53.8, 144.0), (53.6, 136.0)],
    ),
    "TIBIALIS": both([(59.6, 160.0), (63.2, 158.6), (64.4, 166.0), (63.2, 180.0), (61.2, 192.0), (59.6, 191.0),
                      (59.8, 178.0), (59.2, 168.0)]),
    "GASTROCNEMIUS": both([(55.2, 160.0), (57.6, 160.4), (58.2, 170.0), (57.4, 180.0), (55.8, 183.0), (54.8, 172.0)]),
    "BICEPS": both(spindle(*UPPER_ARM, w=5.4, peak=0.55, off=-0.2)),
    "FOREARM_FLEXORS": both(spindle(*FOREARM, w=2.9, peak=0.32, off=-1.6)),
    "FOREARM_EXTENSORS": both(spindle(*FOREARM, w=2.9, peak=0.32, off=1.6)),
}

# ---------------------------------------------------------------- back view
BACK = {
    "NECK": both([(50.9, 22.0), (54.4, 23.0), (54.8, 27.0), (51.0, 27.6)], tension=0.6),
    "TRAPS": both([(50.7, 25.6), (54.8, 26.6), (58.6, 31.0), (66.0, 34.4), (70.0, 36.2), (62.0, 38.4), (56.4, 41.6),
                   (54.2, 48.0), (53.4, 57.0), (50.7, 64.6)], tension=0.7),
    "REAR_DELTS": both([(67.6, 37.8), (73.4, 38.4), (76.4, 42.4), (76.4, 49.4), (73.6, 52.0), (70.2, 46.0)]),
    "SIDE_DELTS": both([(74.8, 37.9), (78.4, 41.4), (80.4, 47.6), (79.6, 53.4), (77.6, 54.0), (77.6, 46.4), (76.6, 40.6)]),
    "ROTATOR_CUFF": both([(61.4, 40.2), (67.0, 39.8), (70.2, 44.0), (68.6, 49.2), (63.6, 50.2), (61.0, 46.0)]),
    "MID_BACK": both([(55.8, 43.6), (60.0, 40.8), (59.6, 46.4), (60.4, 51.6), (58.4, 56.4), (54.8, 58.2), (55.2, 50.0)]),
    "LATS": both([(56.8, 59.6), (61.0, 53.8), (66.0, 51.4), (71.2, 55.2), (70.2, 62.0), (68.2, 70.0), (64.4, 79.0),
                  (59.0, 86.0), (55.8, 84.0), (56.4, 72.0)]),
    "LOWER_BACK": both([(51.2, 65.4), (54.0, 63.0), (56.2, 72.0), (56.8, 84.0), (56.0, 93.4), (51.2, 95.0)], tension=0.6),
    "OBLIQUES": both([(67.0, 73.2), (67.4, 80.0), (68.6, 88.6), (66.0, 93.2), (60.6, 91.2), (64.2, 84.4)]),
    "TRICEPS": both(spindle(*UPPER_ARM, w=5.8, peak=0.4, off=0.1)),
    "FOREARM_EXTENSORS": both(spindle(*FOREARM, w=3.6, peak=0.3, off=0.9)),
    "FOREARM_FLEXORS": both(spindle(*FOREARM, w=1.8, peak=0.35, off=-2.1)),
    "GLUTES": both(
        [(51.4, 96.6), (57.0, 94.4), (64.4, 95.8), (69.6, 101.0), (70.2, 110.0), (67.6, 118.6), (60.4, 121.6),
         (53.4, 120.6), (51.4, 114.0)],
        [(63.6, 92.2), (67.6, 93.2), (69.6, 97.6), (66.2, 95.6)],  # gluteus medius
    ),
    "HAMSTRINGS": both(
        [(61.0, 123.6), (67.6, 121.0), (68.6, 130.0), (67.2, 142.0), (64.6, 152.0), (62.2, 151.4), (62.8, 140.0), (61.8, 131.0)],
        [(52.8, 123.2), (59.8, 123.8), (60.6, 132.0), (61.2, 142.0), (60.8, 151.6), (57.6, 150.0), (54.6, 140.0), (53.2, 131.0)],
    ),
    "ADDUCTORS": both([(51.6, 121.8), (52.4, 121.6), (53.2, 130.0), (52.4, 136.0), (51.5, 130.0)], tension=0.6),
    "GASTROCNEMIUS": both(
        [(59.8, 158.6), (64.6, 158.0), (66.2, 166.0), (65.2, 174.0), (62.0, 177.8), (60.2, 170.0)],
        [(53.8, 158.2), (58.8, 158.8), (59.4, 168.0), (58.8, 178.8), (56.0, 180.6), (54.2, 172.0)],
    ),
    "SOLEUS": both([(55.2, 182.4), (58.6, 181.8), (60.6, 179.6), (63.8, 178.8), (63.2, 186.0), (61.4, 192.4),
                    (57.4, 193.0), (55.4, 188.0)]),
}


def kt_list(parts):
    rows = []
    for muscle, paths in parts.items():
        for d in paths:
            rows.append(f'        Muscle.{muscle} to "{d}",')
    return "\n".join(rows)


def write_kotlin():
    fb, fh = outline()
    out = f'''package com.hyperathlete.app.ui.progress

import com.hyperathlete.app.domain.progress.Muscle

/** Body map paths (viewport 100 × 210). Generated by tools/gen_body.py, do not edit. */
object BodyArt {{
    const val WIDTH = 100f
    const val HEIGHT = 210f
    const val BODY = "{fb}"
    const val HEAD = "{fh}"

    val front: List<Pair<Muscle, String>> = listOf(
{kt_list(FRONT)}
    )

    val back: List<Pair<Muscle, String>> = listOf(
{kt_list(BACK)}
    )
}}
'''
    path = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "java", "com", "hyperathlete", "app", "ui", "progress", "BodyArt.kt")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as fh_:
        fh_.write(out)
    print("wrote", path)


def preview(html):
    fb, fh = outline()
    colors = ["#FFD9BF", "#FFB27F", "#FF8A3D", "#FF6B00", "#C24F00"]

    def view(parts, dx):
        s = [f'<g transform="translate({dx} 0)"><path d="{fb}" fill="#EEF0F3" stroke="#D5D9E0" stroke-width="0.6"/>'
             f'<path d="{fh}" fill="#EEF0F3" stroke="#D5D9E0" stroke-width="0.6"/>']
        for i, (m, paths) in enumerate(parts.items()):
            for d in paths:
                s.append(f'<path d="{d}" fill="{colors[i % len(colors)]}" stroke="#FFFFFF" stroke-width="0.7"><title>{m}</title></path>')
        s.append("</g>")
        return "".join(s)

    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 210 210" style="width:100vw;height:100vh">'
           f'<rect width="210" height="210" fill="white"/>{view(FRONT, 0)}{view(BACK, 110)}</svg>')
    with open(html, "w", encoding="utf-8") as f:
        f.write(f"<!doctype html><meta charset=utf-8><body style='margin:0;background:#fff'>{svg}</body>")
    print("preview", html)


if __name__ == "__main__":
    write_kotlin()
    if "--preview" in sys.argv:
        preview(sys.argv[sys.argv.index("--preview") + 1])
