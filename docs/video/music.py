"""Original soundtrack for the FindPal promo: synthesized here, so no licensing issues.

usage: python3 music.py cues.json out.wav
cues.json: {"duration": seconds, "cues": [[time, "tick"|"pop"|"chime"|"whoosh"|"thud"], ...]}

0 - 10.6 s: a hesitant minor groove under the "lost something?" problem scenes.
10.6 s on: bright major loop (C G Am F) with plucked arpeggios and a soft beat.
"""
import json
import sys
import wave

import numpy as np

SR = 44100
rng = np.random.default_rng(7)


def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12)


def env(n, attack, release):
    e = np.ones(n)
    a = min(n, int(attack * SR))
    r = min(n - a, int(release * SR))
    if a:
        e[:a] = np.linspace(0, 1, a)
    if r:
        e[n - r:] = np.linspace(1, 0, r)
    return e


def tone(freq, dur, partials=((1, 1.0),), decay=None):
    t = np.arange(int(dur * SR)) / SR
    y = sum(a * np.sin(2 * np.pi * freq * k * t) for k, a in partials)
    if decay:
        y = y * np.exp(-t / decay)
    return y


def add(buf, start, y, gain=1.0, pan=0.0):
    i = int(start * SR)
    if i >= buf.shape[0]:
        return
    y = y[: buf.shape[0] - i] * gain
    buf[i:i + len(y), 0] += y * (1 - pan) / 2 * 2 ** 0.5
    buf[i:i + len(y), 1] += y * (1 + pan) / 2 * 2 ** 0.5


def pad(notes, dur):
    y = sum(tone(midi(n), dur, ((1, 1), (2, 0.25), (3, 0.08))) + tone(midi(n) * 1.004, dur, ((1, 0.6),)) for n in notes)
    return y * env(len(y), 0.35, 0.6) / len(notes)


def pluck(n, dur=0.6):
    y = tone(midi(n), dur, ((1, 1), (2, 0.35), (4, 0.12)), decay=0.22)
    return y * env(len(y), 0.004, 0.05)


def kick():
    t = np.arange(int(0.35 * SR)) / SR
    f = 45 + 85 * np.exp(-t / 0.04)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.13)


def hat():
    n = int(0.06 * SR)
    y = np.diff(rng.standard_normal(n + 1)) * np.exp(-np.arange(n) / SR / 0.012)
    return y * 0.5


def sfx(kind):
    t = np.arange(int(1.4 * SR)) / SR
    if kind == "tick":
        y = tone(2600, 0.03, decay=0.006) + 0.4 * np.diff(rng.standard_normal(int(0.03 * SR) + 1)) * np.exp(-np.arange(int(0.03 * SR)) / SR / 0.004)
        return y * 0.35
    if kind == "pop":
        tt = t[: int(0.18 * SR)]
        f = 380 + 700 * (1 - np.exp(-tt / 0.03))
        return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt / 0.05) * 0.9
    if kind == "chime":
        y = sum(a * np.sin(2 * np.pi * f * t) * np.exp(-t / d) for f, a, d in ((midi(88), 1, 0.9), (midi(95), 0.5, 0.6), (midi(100), 0.3, 0.4), (midi(88) * 2.76, 0.15, 0.2)))
        return y * 0.5
    if kind == "whoosh":
        n = int(0.6 * SR)
        noise = rng.standard_normal(n)
        k = 40
        noise = np.convolve(noise, np.ones(k) / k, mode="same")
        shape = np.sin(np.pi * np.linspace(0, 1, n)) ** 2
        return noise * shape * 1.6
    if kind == "thud":
        tt = t[: int(0.4 * SR)]
        return (np.sin(2 * np.pi * 70 * tt) * np.exp(-tt / 0.12) + 0.2 * rng.standard_normal(len(tt)) * np.exp(-tt / 0.02)) * 0.9
    raise ValueError(kind)


def main(cues_path, out_path):
    spec = json.load(open(cues_path))
    duration = spec["duration"]
    music = np.zeros((int((duration + 1) * SR), 2))
    fx = np.zeros_like(music)

    # Section A: hesitant minor (Am, F, Dm, E) at 92 bpm, sparse plucks.
    beat = 60 / 92
    prog_a = [(57, [57, 60, 64]), (53, [53, 57, 60]), (50, [50, 53, 57]), (52, [52, 56, 59])]
    t = 0.0
    i = 0
    while t < 10.6:
        root, chord = prog_a[i % 4]
        add(music, t, pad([n + 12 for n in chord], beat * 4 + 0.6), 0.18)
        add(music, t, pluck(root - 12, 1.2), 0.35)
        for k, n in enumerate([chord[0] + 12, chord[2] + 12, chord[1] + 12]):
            add(music, t + beat * (1 + k), pluck(n), 0.16, pan=-0.4 + 0.4 * k)
        t += beat * 4
        i += 1

    # Section B and C: bright C G Am F at 104 bpm from the reveal to the end.
    beat = 60 / 104
    start = 10.6
    prog = [(48, [60, 64, 67]), (43, [59, 62, 67]), (45, [57, 60, 64]), (41, [57, 60, 65])]
    t = start
    bar = 0
    end = duration - 1.2
    while t < end:
        root, chord = prog[bar % 4]
        add(music, t, pad(chord, beat * 4 + 0.6), 0.2)
        add(music, t, pluck(root, 1.0), 0.45)
        arp = [chord[0], chord[1], chord[2], chord[1] + 12, chord[2] + 12, chord[1] + 12, chord[2], chord[1]]
        for k, n in enumerate(arp):
            add(music, t + k * beat / 2, pluck(n + 12, 0.5), 0.13, pan=-0.5 if k % 2 else 0.5)
        for b in range(4):
            if b in (0, 2):
                add(music, t + b * beat, kick(), 0.55)
            add(music, t + b * beat + beat / 2, hat(), 0.05)
        t += beat * 4
        bar += 1
    # Final chord rings out.
    add(music, end, pad([60, 64, 67, 72], 2.2), 0.24)
    add(music, end, pluck(48, 2.0), 0.5)

    for when, kind in spec["cues"]:
        add(fx, when, sfx(kind), {"tick": 0.25, "pop": 0.55, "chime": 0.45, "whoosh": 0.3, "thud": 0.5}[kind])

    mix = music * 0.8 + fx
    n = int(duration * SR)
    mix = mix[:n]
    fade = int(1.0 * SR)
    mix[-fade:] *= np.linspace(1, 0, fade)[:, None]
    mix[: int(0.05 * SR)] *= np.linspace(0, 1, int(0.05 * SR))[:, None]
    mix = mix / np.max(np.abs(mix)) * 0.89  # about -1 dBFS
    data = (mix * 32767).astype(np.int16)
    with wave.open(out_path, "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data.tobytes())


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
