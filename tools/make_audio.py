#!/usr/bin/env python3
"""
Rat-Man chiptune audio generator.

Synthesises all of the game's music and sound effects from scratch (square/triangle waves + noise
drums) and writes them to src/main/resources/sounds/ as 22 kHz mono WAV files. Everything here is
original, so there are no licensing worries.

    python3 tools/make_audio.py

Edit a melody string below and re-run to change a tune. Melody format: "NOTE:BEATS ..." where NOTE
is like C5, F#4, Bb3, or R for a rest, and BEATS is a length in beats (0.5 = an eighth note).
Requires numpy.
"""
import os
import wave

import numpy as np

SR = 22050
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "sounds")
rng = np.random.default_rng(42)

# --------------------------------------------------------------------------- synthesis helpers

NOTE_INDEX = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}


def freq(name):
    """'C#5' -> Hz."""
    letter, rest = name[0], name[1:]
    shift = 0
    while rest and rest[0] in "#b":
        shift += 1 if rest[0] == "#" else -1
        rest = rest[1:]
    midi = 12 * (int(rest) + 1) + NOTE_INDEX[letter] + shift
    return 440.0 * 2 ** ((midi - 69) / 12)


def square(f, t, duty=0.5):
    # subtract the average so narrow pulses don't carry a DC offset (wastes headroom, muddies the mix)
    return np.where((t * f) % 1.0 < duty, 1.0, -1.0) - (2 * duty - 1)


def triangle(f, t):
    return 2.0 * np.abs(2.0 * ((t * f) % 1.0) - 1.0) - 1.0


def envelope(n, attack=0.004, release=0.03, decay=0.0, sustain=1.0):
    """Linear attack, optional exponential decay toward `sustain`, linear release at the end."""
    env = np.ones(n)
    a = min(n, int(attack * SR))
    r = min(n - a, int(release * SR))
    if a:
        env[:a] = np.linspace(0, 1, a)
    if decay > 0:
        k = np.arange(n - a)
        env[a:] = sustain + (1 - sustain) * np.exp(-k / (decay * SR))
    if r:
        env[n - r:] *= np.linspace(1, 0, r)
    return env


def tone(f, dur, shape="square", duty=0.5, vib=0.0, **env):
    n = max(1, int(dur * SR))
    t = np.arange(n) / SR
    if vib:  # gentle vibrato that fades in after 0.12s
        depth = vib * np.clip((t - 0.12) / 0.2, 0, 1)
        phase = np.cumsum(f * (1 + depth * np.sin(2 * np.pi * 5.5 * t))) / SR
        t_eff = phase / f
    else:
        t_eff = t
    wave_ = square(f, t_eff, duty) if shape == "square" else triangle(f, t_eff)
    return wave_ * envelope(n, **env)


def sweep(f0, f1, dur, shape="square", duty=0.5, curve=1.0, **env):
    n = int(dur * SR)
    k = np.linspace(0, 1, n) ** curve
    f = f0 + (f1 - f0) * k
    phase = np.cumsum(f) / SR
    if shape == "square":
        wave_ = np.where(phase % 1.0 < duty, 1.0, -1.0) - (2 * duty - 1)
    else:
        wave_ = 2 * np.abs(2 * (phase % 1.0) - 1) - 1
    return wave_ * envelope(n, **env)


def noise(dur, decay=0.05, highpass=False):
    n = int(dur * SR)
    x = rng.uniform(-1, 1, n)
    if highpass:
        x = np.diff(x, prepend=0) * 0.7
    return x * np.exp(-np.arange(n) / (decay * SR))


def kick():
    n = int(0.16 * SR)
    t = np.arange(n) / SR
    f = 45 + 110 * np.exp(-t / 0.03)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.07)


def snare():
    n = int(0.14 * SR)
    t = np.arange(n) / SR
    body = np.sin(2 * np.pi * 185 * t) * np.exp(-t / 0.03) * 0.5
    return body + noise(0.14, decay=0.045) * 0.8


def hat():
    return noise(0.05, decay=0.012, highpass=True)


def add(buf, sig, start):
    start = int(start)
    end = min(len(buf), start + len(sig))
    if end > start:
        buf[start:end] += sig[: end - start]


def parse(melody):
    out = []
    for tok in melody.split():
        name, beats = tok.split(":")
        out.append((name, float(beats)))
    return out


def render_line(buf, melody, bpm, start_beat=0.0, octave_shift=0, gate=0.9, **tone_args):
    beat = 60.0 / bpm
    pos = start_beat
    for name, beats in parse(melody):
        if name != "R":
            f = freq(name) * 2 ** octave_shift
            add(buf, tone(f, beats * beat * gate, **tone_args), pos * beat * SR)
        pos += beats
    return pos


CHORDS = {
    "C": ["C", "E", "G"], "Am": ["A", "C", "E"], "F": ["F", "A", "C"], "G": ["G", "B", "D"],
    "Em": ["E", "G", "B"], "E": ["E", "G#", "B"], "Dm": ["D", "F", "A"], "Bb": ["Bb", "D", "F"],
}


def check_bars(melody, bars, name):
    total = sum(b for _, b in parse(melody))
    assert abs(total - bars * 4) < 1e-6, f"{name}: melody is {total} beats, expected {bars * 4}"


def song(bpm, chords, lead, *, lead_duty=0.5, lead_vol=0.24, bass_style="eighths", arp=True,
         arp_vol=0.06, drums=("k...s...k.k.s...", "x.x.x.x.x.x.x.x."), drum_vol=0.3, harmony=None,
         lead_shape="square", vib=0.006):
    """Renders a seamless loop: anything ringing past the end is wrapped back to the start."""
    bars = len(chords)
    check_bars(lead, bars, "lead")
    beat = 60.0 / bpm
    loop = int(round(bars * 4 * beat * SR))
    buf = np.zeros(loop + SR)  # one second of tail, folded back below

    render_line(buf, lead, bpm, shape=lead_shape, duty=lead_duty, vib=vib, release=0.04)
    buf *= lead_vol  # buf holds only the lead at this point
    if harmony:
        h = np.zeros_like(buf)
        render_line(h, harmony, bpm, shape="square", duty=0.25, release=0.04)
        buf += h * lead_vol * 0.45

    sixteenth = beat / 4
    for bar, chord in enumerate(chords):
        notes = CHORDS[chord]
        root = notes[0]
        t0 = bar * 4 * beat
        # bass
        if bass_style == "eighths":
            for i in range(8):
                octv = 3 if i % 2 == 0 else 4
                add(buf, tone(freq(root + str(octv)), beat * 0.45, shape="triangle", release=0.02) * 0.22,
                    (t0 + i * beat / 2) * SR)
        else:  # driving sixteenths
            for i in range(16):
                add(buf, tone(freq(root + "3"), sixteenth * 0.7, duty=0.5, release=0.01) * 0.10,
                    (t0 + i * sixteenth) * SR)
                if i % 4 == 0:
                    add(buf, tone(freq(root + "3"), beat * 0.9, shape="triangle", release=0.03) * 0.2,
                        (t0 + i * sixteenth) * SR)
        # arpeggio
        if arp:
            seq = [n + "5" for n in notes] + [notes[0] + "6"]
            for i in range(16):
                add(buf, tone(freq(seq[i % 4]), sixteenth * 0.8, duty=0.25, release=0.015) * arp_vol,
                    (t0 + i * sixteenth) * SR)
        # drums
        kick_snare, hats = drums
        for i in range(16):
            at = (t0 + i * sixteenth) * SR
            if kick_snare[i] == "k":
                add(buf, kick() * drum_vol * 0.8, at)
            elif kick_snare[i] == "s":
                add(buf, snare() * drum_vol * 0.8, at)
            if hats[i] == "x":
                add(buf, hat() * drum_vol * 0.5, at)

    out = buf[:loop].copy()
    tail = buf[loop:]
    out[: len(tail)] += tail  # seamless wrap
    return out


def save(name, x, peak=0.85):
    x = x / max(1e-9, np.max(np.abs(x))) * peak
    data = (np.clip(x, -1, 1) * 32767).astype("<i2")
    os.makedirs(OUT, exist_ok=True)
    with wave.open(os.path.join(OUT, name), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data.tobytes())
    print(f"  {name:18s} {len(x) / SR:5.2f}s")


def seq(parts):
    return np.concatenate(parts)


# --------------------------------------------------------------------------- music

def level_theme():
    chords = ["C", "Am", "F", "G", "C", "Am", "F", "G", "F", "G", "Em", "Am", "F", "G", "C", "G"]
    lead = (
        "E5:.5 G5:.5 C6:1 B5:.5 G5:.5 E5:1 "
        "A5:.5 G5:.5 E5:.5 C5:.5 E5:1 R:1 "
        "F5:.5 A5:.5 C6:1 A5:.5 F5:.5 A5:1 "
        "G5:1.5 F5:.5 D5:1 B4:1 "
        "E5:.5 G5:.5 C6:1 D6:.5 C6:.5 G5:1 "
        "A5:1 C6:.5 B5:.5 A5:1 E5:1 "
        "F5:.5 G5:.5 A5:.5 C6:.5 D6:1 C6:1 "
        "B5:1 G5:1 D6:1.5 R:.5 "
        "A5:1.5 G5:.5 F5:1 C5:1 "
        "D5:.5 E5:.5 F5:.5 G5:.5 B5:1 G5:1 "
        "E5:1.5 G5:.5 B5:1 G5:1 "
        "A5:.5 B5:.5 C6:.5 B5:.5 A5:1 E5:1 "
        "F5:.5 A5:.5 C6:.5 F6:.5 E6:1 C6:1 "
        "D6:.5 C6:.5 B5:.5 A5:.5 G5:1 B5:1 "
        "C6:1.5 G5:.5 E5:.5 G5:.5 C6:1 "
        "B5:.5 A5:.5 G5:.5 F5:.5 E5:.5 D5:.5 B4:1"
    )
    return song(140, chords, lead, lead_duty=0.25)


def boss_theme():
    chords = ["Am", "Am", "F", "E", "Am", "Am", "Dm", "E"] * 2
    a = (
        "A4:.5 C5:.5 E5:.5 A5:.5 G#5:.5 A5:.5 E5:1 "
        "A4:.5 C5:.5 E5:.5 A5:.5 B5:.5 C6:.5 B5:1 "
        "A5:.5 F5:.5 C5:.5 F5:.5 A5:1 C6:1 "
        "B5:.5 G#5:.5 E5:.5 G#5:.5 B5:1 E6:1 "
        "E6:.5 D6:.5 C6:.5 B5:.5 A5:1 E5:1 "
        "A5:.5 B5:.5 C6:.5 D6:.5 E6:1 C6:1 "
        "D6:.5 C6:.5 A5:.5 F5:.5 D5:1 F5:1 "
        "E5:.5 G#5:.5 B5:.5 D6:.5 E6:1.5 R:.5 "
    )
    return song(165, chords, a * 2, lead_duty=0.125, bass_style="sixteenths",
                drums=("k...s..kk.k.s...", "x.xxx.xxx.xxx.xx"), drum_vol=0.24, arp_vol=0.05, lead_vol=0.34)


def menu_theme():
    chords = ["F", "C", "Dm", "Bb", "F", "C", "Bb", "C"]
    lead = (
        "C5:.5 F5:.5 A5:.5 C6:.5 A5:1 F5:1 "
        "G5:.5 E5:.5 C5:.5 E5:.5 G5:2 "
        "F5:.5 A5:.5 D6:.5 A5:.5 F5:1 D5:1 "
        "D5:.5 F5:.5 Bb5:1 A5:.5 G5:.5 F5:1 "
        "A5:.5 C6:.5 F6:1 E6:.5 D6:.5 C6:1 "
        "G5:1 E5:.5 G5:.5 C6:2 "
        "D6:.5 C6:.5 Bb5:.5 A5:.5 G5:1 F5:1 "
        "E5:.5 F5:.5 G5:.5 A5:.5 Bb5:.5 G5:.5 E5:1"
    )
    return song(112, chords, lead, lead_shape="triangle", lead_vol=0.42, arp_vol=0.05,
                drums=("k.......s...k...", "x...x...x...x..."), drum_vol=0.22)


# --------------------------------------------------------------------------- jingles

def jingle(bpm, lead, chords_at=(), lead_duty=0.5, tail=0.6):
    beat = 60.0 / bpm
    total = sum(b for _, b in parse(lead))
    buf = np.zeros(int((total * beat + tail) * SR))
    render_line(buf, lead, bpm, duty=lead_duty, release=0.06, vib=0.008, gate=0.95)
    for beat_pos, notes, length in chords_at:
        for n in notes:
            add(buf, tone(freq(n), length * beat, duty=0.25, release=0.1) * 0.35, beat_pos * beat * SR)
    return buf


def start_jingle():  # plays during READY! (about 2 seconds)
    lead = "C5:.25 E5:.25 G5:.25 C6:.25 E5:.25 G5:.25 C6:.25 E6:.25 G6:.5 R:.25 G6:.25 C7:1.5"
    j = jingle(150, lead, chords_at=[(2.5, ["C4", "E4", "G4"], 1.5)], tail=0.15)  # must fit in 2s
    add(j, kick() * 0.6, 0)
    add(j, snare() * 0.4, int(2.5 * 60 / 150 * SR))
    return j


def level_clear_jingle():
    lead = "C5:.25 E5:.25 G5:.25 C6:.25 E6:.75 D6:.25 E6:.5 G6:2"
    return jingle(160, lead, chords_at=[(1.75, ["C4", "G4", "E5"], 2.5)])


def game_over_jingle():
    lead = "G5:.75 F#5:.75 F5:.75 E5:2"
    return jingle(110, lead, chords_at=[(2.25, ["A3", "C4", "E4"], 2)], lead_duty=0.25, tail=0.8)


# --------------------------------------------------------------------------- sound effects

def munch(low):
    base = 260 if low else 330
    return sweep(base, base * 1.9, 0.07, duty=0.5, release=0.02) * 0.8


def cheese_power():
    notes = ["C5", "E5", "G5", "C6", "E6", "G6", "C7"]
    return seq([tone(freq(n), 0.045, duty=0.25, release=0.01) for n in notes] +
               [tone(freq("C7"), 0.18, shape="triangle", release=0.12)])


def eat_cat():
    up = sweep(220, 1400, 0.16, duty=0.5, curve=0.6, release=0.01)
    ding = tone(freq("E6"), 0.12, duty=0.25, release=0.08) + tone(freq("B6"), 0.12, duty=0.125, release=0.08) * 0.6
    return seq([up, ding])


def death():
    parts = []
    f = 900.0
    for i in range(10):
        parts.append(sweep(f, f * 0.75, 0.12, duty=0.5, release=0.01))
        f *= 0.82
    parts.append(np.zeros(int(0.08 * SR)))
    parts.append(tone(110, 0.09, release=0.03))
    parts.append(np.zeros(int(0.05 * SR)))
    parts.append(tone(82, 0.16, release=0.08))
    return seq(parts) * 0.8


def pause_blip():
    return seq([tone(freq("E6"), 0.05, duty=0.25, release=0.01), tone(freq("B5"), 0.08, duty=0.25, release=0.04)]) * 0.6


def menu_click():
    return sweep(600, 1100, 0.05, duty=0.25, release=0.02) * 0.6


def cat_free():  # a cat leaves the cage
    return sweep(500, 260, 0.12, shape="triangle", release=0.04) * 0.6


if __name__ == "__main__":
    print("Writing to", os.path.abspath(OUT))
    save("music_menu.wav", menu_theme(), peak=0.8)
    save("music_level.wav", level_theme(), peak=0.8)
    save("music_boss.wav", boss_theme(), peak=0.8)
    save("jingle_start.wav", start_jingle())
    save("jingle_level_clear.wav", level_clear_jingle())
    save("jingle_game_over.wav", game_over_jingle())
    save("sfx_munch_a.wav", munch(True), peak=0.6)
    save("sfx_munch_b.wav", munch(False), peak=0.6)
    save("sfx_cheese.wav", cheese_power())
    save("sfx_eat_cat.wav", eat_cat())
    save("sfx_death.wav", death())
    save("sfx_pause.wav", pause_blip(), peak=0.6)
    save("sfx_click.wav", menu_click(), peak=0.6)
    save("sfx_cat_free.wav", cat_free(), peak=0.5)
