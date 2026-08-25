"""Authors the Basalt launcher icon and emits both SVG and Android drawables.

The mark: a brass bezel with rivets and etched ticks, housing a dot-matrix
dial, with real tapered hands riding on top. The dots are the dial's texture —
the same unlit grid that sits behind every readout in the app — and the hands
are metal, because a hand made of dots is not legible at 48px.

**The icon tells the time.** The adaptive foreground is a layer-list whose
hour and minute hands are separate layers, and the launcher activity declares
`com.android.launcher3.{HOUR,MINUTE,SECOND}_LAYER_INDEX` meta-data. Launcher3
(and the launchers derived from it, which is most of them) reads that metadata,
finds those layers inside the AdaptiveIconDrawable, and rotates them about the
drawable's centre as the clock advances. The hands are therefore authored
pointing at twelve, and `DEFAULT_HOUR` / `DEFAULT_MINUTE` are 0 so the
launcher's rotation is absolute rather than a delta from some drawn time.

Two consequences worth knowing:

* The hands must be their own layers in the *foreground* of an adaptive icon.
  A flattened PNG cannot move, so the legacy `mipmap-*/ic_launcher.png` files
  are baked at 10:10 and stay there. Pre-26 launchers get a static icon.
* The second hand is deliberately not animated (`SECOND_LAYER_INDEX = -1`).
  A ticking second hand on the home screen wakes the launcher every second;
  the battery cost is real and the payoff is not.

Geometry is generated rather than hand-drawn so the dial stays in sync with the
renderer's cell maths and a palette change is a one-line edit.

Requires `resvg-py` for the legacy raster pass:

    python -m pip install resvg-py
    python tools/generate_icon.py
"""

import math
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ICON_DIR = os.path.join(ROOT, "icon")
RES = os.path.join(ROOT, "app", "src", "main", "res")

INK = "#0B0A09"
IRON_OXIDE = "#171310"
BRONZE_DEEP = "#3E2C1C"
BRONZE = "#7A5230"
COPPER = "#B87333"
COPPER_HOT = "#E0925A"
SILVER = "#C9CDD2"
SILVER_BRIGHT = "#F2F4F6"
PEWTER = "#6E7479"

# Everything is authored in a 108x108 space: that is the adaptive-icon
# viewport, so the foreground layers need no transform of their own.
VIEWPORT = 108.0
CX = CY = VIEWPORT / 2

# The adaptive-icon safe zone is the central 66 of 108.
SAFE_RADIUS = 66 / 2

GRID = 9  # dial is a 9x9 dot matrix


# --------------------------------------------------------------------- shapes
# Shapes are emitted as dicts so one geometry pass can feed both the SVG
# writer and the VectorDrawable writer.

def circle(cx, cy, r, fill, opacity=1.0):
    return {"kind": "circle", "cx": cx, "cy": cy, "r": r, "fill": fill, "opacity": opacity}


def line(x1, y1, x2, y2, stroke, width, cap="round"):
    return {"kind": "line", "x1": x1, "y1": y1, "x2": x2, "y2": y2,
            "stroke": stroke, "width": width, "cap": cap}


def path(d, fill, opacity=1.0):
    return {"kind": "path", "d": d, "fill": fill, "opacity": opacity}


def _circle_d(cx, cy, r):
    """A circle as a closed path — VectorDrawable has no <circle>."""
    return (f"M{cx - r:.3f},{cy:.3f} "
            f"a{r:.3f},{r:.3f} 0 1,0 {2 * r:.3f},0 "
            f"a{r:.3f},{r:.3f} 0 1,0 {-2 * r:.3f},0 Z")


# ----------------------------------------------------------------------- dial

def dial_shapes(radius, include_plate=True, include_dots=True):
    """Bezel, ticks and dot matrix — everything that does not move."""
    out = []
    ring_inner = radius * 0.86

    if include_plate:
        # A banded metal ramp rather than a gradient: each band is nudged
        # upward, leaving a bright specular crescent along the top edge and a
        # dark one beneath. Banding survives a 48px raster; a smooth ramp
        # turns to mud.
        out.append(circle(CX, CY, radius, BRONZE_DEEP))
        for shift, scale, fill in (
            (0.015, 0.985, BRONZE),
            (0.040, 0.955, COPPER),
            (0.070, 0.920, COPPER_HOT),
        ):
            out.append(circle(CX, CY - radius * shift, radius * scale, fill))
        out.append(circle(CX, CY, ring_inner, BRONZE_DEEP))
        out.append(circle(CX, CY, ring_inner * 0.96, INK))

    # Etched ticks on the bezel; quarters longer and brighter.
    for i in range(12):
        a = math.radians(i * 30)
        quarter = i % 3 == 0
        r1 = ring_inner * 0.99
        r2 = ring_inner * (0.88 if quarter else 0.93)
        out.append(line(
            CX + math.sin(a) * r1, CY - math.cos(a) * r1,
            CX + math.sin(a) * r2, CY - math.cos(a) * r2,
            SILVER_BRIGHT if quarter else PEWTER,
            radius * (0.038 if quarter else 0.020),
        ))

    if include_dots:
        span = ring_inner * 1.30
        cell = span / GRID
        dot_r = cell * 0.30
        origin = CX - span / 2 + cell / 2
        quarters = {(4, 0), (8, 4), (4, 8), (0, 4)}
        for gy in range(GRID):
            for gx in range(GRID):
                x = origin + gx * cell
                y = CY - span / 2 + cell / 2 + gy * cell
                if (gx, gy) in quarters:
                    out.append(circle(x, y, dot_r * 0.95, SILVER))
                elif (gx, gy) == (4, 4):
                    continue  # the hub sits here
                else:
                    out.append(circle(x, y, dot_r * 0.70, PEWTER, opacity=0.22))

    if include_plate:
        # Rivets on the diagonals.
        for i in range(4):
            a = math.radians(45 + i * 90)
            rx = CX + math.sin(a) * radius * 0.93
            ry = CY - math.cos(a) * radius * 0.93
            out.append(circle(rx, ry, radius * 0.045, BRONZE_DEEP))
            out.append(circle(rx, ry - radius * 0.012, radius * 0.030, SILVER))

    return out


# ---------------------------------------------------------------------- hands

def _hand_path(length, base_half, tip_half, tail=0.0):
    """A tapered hand pointing at twelve, pivoting on the drawable's centre.

    Authored straight up because that is what the launcher expects: it applies
    an absolute rotation, so any drawn angle would be added to the real time.
    """
    top = CY - length
    bottom = CY + tail
    return (
        f"M{CX - base_half:.3f},{bottom:.3f} "
        f"L{CX - tip_half:.3f},{top:.3f} "
        f"Q{CX:.3f},{top - tip_half * 1.6:.3f} {CX + tip_half:.3f},{top:.3f} "
        f"L{CX + base_half:.3f},{bottom:.3f} Z"
    )


def hour_hand_shapes(radius):
    return [
        path(_hand_path(length=radius * 0.46, base_half=radius * 0.070,
                        tip_half=radius * 0.042, tail=radius * 0.075),
             SILVER_BRIGHT),
    ]


def minute_hand_shapes(radius):
    return [
        path(_hand_path(length=radius * 0.72, base_half=radius * 0.052,
                        tip_half=radius * 0.030, tail=radius * 0.095),
             SILVER),
    ]


def second_hand_shapes(radius):
    return [
        path(_hand_path(length=radius * 0.78, base_half=radius * 0.020,
                        tip_half=radius * 0.014, tail=radius * 0.170),
             COPPER_HOT),
    ]


def hub_shapes(radius):
    """Drawn above the hands so the pivot reads as a single pinned joint."""
    return [
        circle(CX, CY, radius * 0.085, BRONZE_DEEP),
        circle(CX, CY, radius * 0.060, COPPER_HOT),
        circle(CX, CY, radius * 0.022, INK),
    ]


# ------------------------------------------------------------------ SVG output

def to_svg(shapes, size, background=None, viewport=VIEWPORT):
    body = []
    if background is not None:
        body.append(background)
    for s in shapes:
        op = "" if s.get("opacity", 1.0) >= 1.0 else f' fill-opacity="{s["opacity"]}"'
        if s["kind"] == "circle":
            body.append(f'<circle cx="{s["cx"]:.3f}" cy="{s["cy"]:.3f}" r="{s["r"]:.3f}" '
                        f'fill="{s["fill"]}"{op}/>')
        elif s["kind"] == "line":
            body.append(f'<line x1="{s["x1"]:.3f}" y1="{s["y1"]:.3f}" '
                        f'x2="{s["x2"]:.3f}" y2="{s["y2"]:.3f}" stroke="{s["stroke"]}" '
                        f'stroke-width="{s["width"]:.3f}" stroke-linecap="{s["cap"]}"/>')
        else:
            body.append(f'<path d="{s["d"]}" fill="{s["fill"]}"{op}/>')
    inner = "\n  ".join(body)
    return (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" '
        f'viewBox="0 0 {viewport:g} {viewport:g}">\n  {inner}\n</svg>\n'
    )


# --------------------------------------------------- VectorDrawable output

def to_vector(shapes, tint=None):
    body = []
    for s in shapes:
        alpha = s.get("opacity", 1.0)
        alpha_attr = "" if alpha >= 1.0 else f'\n        android:fillAlpha="{alpha}"'
        if s["kind"] == "circle":
            fill = tint or s["fill"]
            body.append(
                '    <path\n'
                f'        android:pathData="{_circle_d(s["cx"], s["cy"], s["r"])}"\n'
                f'        android:fillColor="{fill}"{alpha_attr} />'
            )
        elif s["kind"] == "line":
            stroke = tint or s["stroke"]
            body.append(
                '    <path\n'
                f'        android:pathData="M{s["x1"]:.3f},{s["y1"]:.3f} L{s["x2"]:.3f},{s["y2"]:.3f}"\n'
                f'        android:strokeColor="{stroke}"\n'
                f'        android:strokeWidth="{s["width"]:.3f}"\n'
                '        android:strokeLineCap="round" />'
            )
        else:
            fill = tint or s["fill"]
            body.append(
                '    <path\n'
                f'        android:pathData="{s["d"]}"\n'
                f'        android:fillColor="{fill}"{alpha_attr} />'
            )
    inner = "\n".join(body)
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<!-- Generated by tools/generate_icon.py — do not edit by hand. -->\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{VIEWPORT:g}dp"\n'
        f'    android:height="{VIEWPORT:g}dp"\n'
        f'    android:viewportWidth="{VIEWPORT:g}"\n'
        f'    android:viewportHeight="{VIEWPORT:g}">\n'
        f'{inner}\n'
        '</vector>\n'
    )


# ------------------------------------------------------------------- file I/O

def write(path_, text):
    os.makedirs(os.path.dirname(path_), exist_ok=True)
    with open(path_, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
    print("wrote ", os.path.relpath(path_, ROOT))


def rasterize(svg_path, png_path, size):
    import resvg_py

    png = resvg_py.svg_to_bytes(
        svg_path=svg_path,
        width=size,
        height=size,
        shape_rendering="geometric_precision",
    )
    os.makedirs(os.path.dirname(png_path), exist_ok=True)
    with open(png_path, "wb") as f:
        f.write(bytes(png))
    print("raster", os.path.relpath(png_path, ROOT), f"{size}x{size}")


def _svg_rotated_group(shapes, degrees):
    """SVG fragment for hand shapes rotated about the dial centre."""
    inner = "\n    ".join(
        f'<path d="{s["d"]}" fill="{s["fill"]}"/>' for s in shapes
    )
    return f'<g transform="rotate({degrees:.3f} {CX:g} {CY:g})">\n    {inner}\n  </g>'


DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

# The time the static, non-animating assets depict. 10:10 is symmetric, which
# matters at 48px, and it leaves the hub clear.
BAKED_HOUR, BAKED_MINUTE = 10, 10


def main():
    # ---- Adaptive icon layers (these are the ones the launcher animates).
    r = SAFE_RADIUS * 0.98
    write(os.path.join(RES, "drawable", "ic_launcher_dial.xml"),
          to_vector(dial_shapes(r)))
    write(os.path.join(RES, "drawable", "ic_launcher_hand_hour.xml"),
          to_vector(hour_hand_shapes(r)))
    write(os.path.join(RES, "drawable", "ic_launcher_hand_minute.xml"),
          to_vector(minute_hand_shapes(r) + hub_shapes(r)))
    write(os.path.join(RES, "drawable", "ic_launcher_hand_second.xml"),
          to_vector(second_hand_shapes(r)))

    # Themed (monochrome) icon: alpha is all the launcher keeps, so the whole
    # mark collapses to one colour and the plate is dropped.
    write(os.path.join(RES, "drawable", "ic_launcher_monochrome.xml"),
          to_vector(
              dial_shapes(r, include_plate=False, include_dots=True)
              + hour_hand_shapes(r) + minute_hand_shapes(r) + hub_shapes(r),
              tint="#FFFFFFFF",
          ))

    # ---- SVG sources, for reference and for the legacy raster pass.
    hour_angle = (BAKED_HOUR % 12) * 30 + BAKED_MINUTE * 0.5
    minute_angle = BAKED_MINUTE * 6

    def mark_svg(size, radius, background):
        shapes = dial_shapes(radius)
        svg = to_svg(shapes, size, background=background)
        hands = (
            "  " + _svg_rotated_group(hour_hand_shapes(radius), hour_angle) + "\n"
            "  " + _svg_rotated_group(minute_hand_shapes(radius), minute_angle) + "\n"
            "  " + "\n  ".join(
                f'<circle cx="{s["cx"]:.3f}" cy="{s["cy"]:.3f}" r="{s["r"]:.3f}" fill="{s["fill"]}"/>'
                for s in hub_shapes(radius)
            ) + "\n"
        )
        return svg.replace("</svg>", hands + "</svg>")

    corner = VIEWPORT * 0.22
    square_bg = (
        f'<rect x="0" y="0" width="{VIEWPORT:g}" height="{VIEWPORT:g}" '
        f'rx="{corner:.3f}" ry="{corner:.3f}" fill="{INK}"/>\n'
        f'  <rect x="0" y="0" width="{VIEWPORT:g}" height="{VIEWPORT * 0.55:.3f}" '
        f'rx="{corner:.3f}" ry="{corner:.3f}" fill="{IRON_OXIDE}" fill-opacity="0.85"/>'
    )
    round_bg = f'<circle cx="{CX:g}" cy="{CY:g}" r="{CX:g}" fill="{INK}"/>'

    write(os.path.join(ICON_DIR, "ic_launcher.svg"),
          mark_svg(512, VIEWPORT * 0.40, square_bg))
    write(os.path.join(ICON_DIR, "ic_launcher_round.svg"),
          mark_svg(512, VIEWPORT * 0.44, round_bg))
    write(os.path.join(ICON_DIR, "ic_launcher_foreground.svg"),
          mark_svg(432, r, None))

    # ---- Legacy rasters. Flat, and therefore frozen at 10:10.
    for density, factor in DENSITIES.items():
        out = os.path.join(RES, f"mipmap-{density}")
        rasterize(os.path.join(ICON_DIR, "ic_launcher.svg"),
                  os.path.join(out, "ic_launcher.png"), round(48 * factor))
        rasterize(os.path.join(ICON_DIR, "ic_launcher_round.svg"),
                  os.path.join(out, "ic_launcher_round.png"), round(48 * factor))

    rasterize(os.path.join(ICON_DIR, "ic_launcher.svg"),
              os.path.join(ICON_DIR, "ic_launcher_512.png"), 512)


if __name__ == "__main__":
    main()
