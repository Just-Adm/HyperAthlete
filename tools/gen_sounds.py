#!/usr/bin/env python3
"""Synthesises the workout timer sounds (original, no samples) into app/src/main/res/raw/*.wav."""
import math, os, struct, wave

RATE = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")


def bell(freq, dur, decay=6.0, partials=((1, 1.0), (2.0, 0.35), (3.01, 0.12), (4.2, 0.05))):
    n = int(RATE * dur)
    out = []
    for i in range(n):
        t = i / RATE
        attack = min(1.0, t / 0.004)
        env = attack * math.exp(-decay * t)
        out.append(env * sum(a * math.sin(2 * math.pi * freq * m * t) for m, a in partials))
    return out


def mix(tracks, length):
    buf = [0.0] * int(RATE * length)
    for start, samples in tracks:
        o = int(RATE * start)
        for i, s in enumerate(samples):
            if o + i < len(buf):
                buf[o + i] += s
    return buf


def save(name, buf, peak=0.7):  # written as .wav, then converted to .ogg with ffmpeg (see README)
    m = max(abs(x) for x in buf) or 1
    with wave.open(os.path.join(OUT, name), "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(x / m * peak * 32767)) for x in buf))


def note(n):  # MIDI → Hz
    return 440 * 2 ** ((n - 69) / 12)


save("tick.wav", bell(note(93), 0.09, decay=45, partials=((1, 1.0), (2.0, 0.2))), peak=0.45)
save("ready.wav", mix([(0, bell(note(88), 0.5, 7)), (0.11, bell(note(93), 0.6, 6))], 0.75), peak=0.55)
end = [(0.0, bell(note(84), 0.9, 4.5)), (0.14, bell(note(88), 0.9, 4.5)), (0.28, bell(note(91), 1.1, 4)),
       (0.75, bell(note(84), 0.9, 4.5)), (0.89, bell(note(88), 0.9, 4.5)), (1.03, bell(note(96), 1.3, 3.5))]
save("rest_end.wav", mix(end, 2.4), peak=0.8)
done = [(0.0, bell(note(72), 1.0, 3.5)), (0.12, bell(note(76), 1.0, 3.5)), (0.24, bell(note(79), 1.0, 3.5)), (0.36, bell(note(84), 1.6, 2.8)),
        (0.36, bell(note(88), 1.6, 3.0))]
save("session_done.wav", mix(done, 2.2), peak=0.75)
print("sounds →", os.path.normpath(OUT))
