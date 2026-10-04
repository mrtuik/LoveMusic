#!/usr/bin/env python3
"""
LoveMusic SVG -> Android VectorDrawable converter (stdlib only).

Usage:
    python3 tools/svg2vd.py                 # converts svg-icons/*.svg -> app/src/main/res/drawable/*.xml
    python3 tools/svg2vd.py in.svg out.xml  # single file

Rules:
  * File name  ->  drawable name  (play-circle.svg -> play_circle.xml)
  * Supports <path>, <circle>, <ellipse>, <rect> (rx/ry), <line>, <polyline>, <polygon>, <g>
  * fill / stroke / stroke-width / stroke-linecap / stroke-linejoin / opacity attributes
  * currentColor, black, white or any colour on an icon that should be tinted -> white
    (so Compose `Icon(tint = ...)` can recolour it). Use real colours + `data-keep-color="1"`
    on <svg> for multi-colour artwork (logos).
  * Anything unsupported (gradients, filters, text) is skipped with a warning.
"""
import math
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "svg-icons")
DST = os.path.join(ROOT, "app", "src", "main", "res", "drawable")
NS = "{http://www.w3.org/2000/svg}"


def strip(tag):
    return tag.replace(NS, "")


def num(v, default=0.0):
    if v is None:
        return default
    m = re.match(r"\s*(-?\d*\.?\d+(?:[eE][-+]?\d+)?)", str(v))
    return float(m.group(1)) if m else default


def f(n):
    s = ("%.3f" % n).rstrip("0").rstrip(".")
    return s if s not in ("", "-0") else "0"


def circle_path(cx, cy, r):
    return (f"M{f(cx - r)},{f(cy)}a{f(r)},{f(r)} 0 1,0 {f(2 * r)},0"
            f"a{f(r)},{f(r)} 0 1,0 {f(-2 * r)},0z")


def ellipse_path(cx, cy, rx, ry):
    return (f"M{f(cx - rx)},{f(cy)}a{f(rx)},{f(ry)} 0 1,0 {f(2 * rx)},0"
            f"a{f(rx)},{f(ry)} 0 1,0 {f(-2 * rx)},0z")


def rect_path(x, y, w, h, rx, ry):
    rx = min(rx, w / 2)
    ry = min(ry, h / 2)
    if rx <= 0 and ry <= 0:
        return f"M{f(x)},{f(y)}h{f(w)}v{f(h)}h{f(-w)}z"
    return (f"M{f(x + rx)},{f(y)}h{f(w - 2 * rx)}a{f(rx)},{f(ry)} 0 0,1 {f(rx)},{f(ry)}"
            f"v{f(h - 2 * ry)}a{f(rx)},{f(ry)} 0 0,1 {f(-rx)},{f(ry)}"
            f"h{f(-(w - 2 * rx))}a{f(rx)},{f(ry)} 0 0,1 {f(-rx)},{f(-ry)}"
            f"v{f(-(h - 2 * ry))}a{f(rx)},{f(ry)} 0 0,1 {f(rx)},{f(-ry)}z")


def points_path(points, close):
    nums = [float(x) for x in re.findall(r"-?\d*\.?\d+(?:[eE][-+]?\d+)?", points)]
    pts = list(zip(nums[0::2], nums[1::2]))
    if not pts:
        return ""
    d = "M" + " L".join(f"{f(x)},{f(y)}" for x, y in pts)
    return d + ("z" if close else "")


def parse_style(el):
    attrs = dict(el.attrib)
    style = attrs.pop("style", "")
    for part in style.split(";"):
        if ":" in part:
            k, v = part.split(":", 1)
            attrs[k.strip()] = v.strip()
    return attrs


def color(v, keep, tintable):
    """Return (#AARRGGBB-ish string or None)."""
    if v is None:
        return None
    v = v.strip()
    if v in ("none", "transparent"):
        return None
    if not keep:
        return "#FFFFFFFF"
    named = {"black": "#000000", "white": "#FFFFFF", "red": "#FF0000"}
    if v == "currentColor":
        return "#FFFFFFFF"
    v = named.get(v, v)
    m = re.fullmatch(r"#([0-9a-fA-F]{3})", v)
    if m:
        h = m.group(1)
        return "#FF" + "".join(c * 2 for c in h).upper()
    m = re.fullmatch(r"#([0-9a-fA-F]{6})", v)
    if m:
        return "#FF" + m.group(1).upper()
    m = re.fullmatch(r"#([0-9a-fA-F]{8})", v)
    if m:  # CSS #RRGGBBAA -> Android #AARRGGBB
        h = m.group(1)
        return "#" + (h[6:8] + h[0:6]).upper()
    m = re.fullmatch(r"rgb\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)", v)
    if m:
        return "#FF%02X%02X%02X" % tuple(int(x) for x in m.groups())
    return "#FFFFFFFF"



def group_for(transform):
    """Compose translate()/scale() (no rotate/skew) into one <group>. Returns (open, close) or None."""
    if not transform:
        return None
    sx = sy = 1.0
    tx = ty = 0.0
    for name, args in re.findall(r"(translate|scale)\(([^)]*)\)", transform):
        v = [float(x) for x in re.findall(r"-?\d*\.?\d+(?:[eE][-+]?\d+)?", args)]
        if not v:
            continue
        if name == "translate":
            dx, dy = v[0], (v[1] if len(v) > 1 else 0.0)
            tx, ty = tx + sx * dx, ty + sy * dy
        else:
            kx, ky = v[0], (v[1] if len(v) > 1 else v[0])
            sx, sy = sx * kx, sy * ky
    if "rotate" in transform or "matrix" in transform or "skew" in transform:
        print("warning: rotate/matrix/skew transforms are not supported", file=sys.stderr)
    return (f'<group android:scaleX="{f(sx)}" android:scaleY="{f(sy)}" '
            f'android:translateX="{f(tx)}" android:translateY="{f(ty)}">', "</group>")

def convert(svg_text, name="icon"):
    root = ET.fromstring(svg_text)
    keep = root.attrib.get("data-keep-color") == "1"
    vb = root.attrib.get("viewBox")
    if vb:
        vx, vy, vw, vh = [float(x) for x in re.split(r"[ ,]+", vb.strip())]
    else:
        vw, vh = num(root.attrib.get("width"), 24), num(root.attrib.get("height"), 24)
        vx = vy = 0.0
    size = int(root.attrib.get("data-size", 24 if max(vw, vh) <= 64 else 96))
    out = []
    warnings = []

    def walk(el, inherited):
        tag = strip(el.tag)
        a = dict(inherited)
        a.update(parse_style(el))
        if tag in ("defs", "title", "desc", "metadata", "linearGradient", "radialGradient", "clipPath", "mask", "style"):
            return
        if tag in ("g", "svg", "a"):
            grp = group_for(el.attrib.get("transform")) if tag == "g" else None
            if grp:
                out.append(grp[0])
            for ch in el:
                walk(ch, {k: v for k, v in a.items() if k in
                          ("fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin",
                           "opacity", "fill-opacity", "stroke-opacity", "fill-rule")})
            if grp:
                out.append(grp[1])
            return
        d = None
        if tag == "path":
            d = a.get("d")
        elif tag == "circle":
            d = circle_path(num(a.get("cx")), num(a.get("cy")), num(a.get("r")))
        elif tag == "ellipse":
            d = ellipse_path(num(a.get("cx")), num(a.get("cy")), num(a.get("rx")), num(a.get("ry")))
        elif tag == "rect":
            rx = num(a.get("rx"), num(a.get("ry")))
            ry = num(a.get("ry"), rx)
            d = rect_path(num(a.get("x")), num(a.get("y")), num(a.get("width")), num(a.get("height")), rx, ry)
        elif tag == "line":
            d = f"M{f(num(a.get('x1')))},{f(num(a.get('y1')))}L{f(num(a.get('x2')))},{f(num(a.get('y2')))}"
        elif tag == "polyline":
            d = points_path(a.get("points", ""), False)
        elif tag == "polygon":
            d = points_path(a.get("points", ""), True)
        else:
            warnings.append(f"skipped <{tag}>")
            return
        if not d:
            return
        d = re.sub(r"\s+", " ", d.strip())
        fill_v = a.get("fill", "black")  # SVG default is black
        stroke_v = a.get("stroke")
        fill = color(fill_v, keep, True)
        stroke = color(stroke_v, keep, True)
        if fill is None and stroke is None:
            return
        attrs = [f'android:pathData="{d}"']
        op = num(a.get("opacity"), 1.0)
        if fill:
            attrs.append(f'android:fillColor="{fill}"')
            fa = num(a.get("fill-opacity"), 1.0) * op
            if fa < 1:
                attrs.append(f'android:fillAlpha="{f(fa)}"')
            if a.get("fill-rule") == "evenodd":
                attrs.append('android:fillType="evenOdd"')
        if stroke:
            attrs.append(f'android:strokeColor="{stroke}"')
            attrs.append(f'android:strokeWidth="{f(num(a.get("stroke-width"), 1.0))}"')
            attrs.append(f'android:strokeLineCap="{a.get("stroke-linecap", "butt")}"')
            attrs.append(f'android:strokeLineJoin="{a.get("stroke-linejoin", "miter")}"')
            sa = num(a.get("stroke-opacity"), 1.0) * op
            if sa < 1:
                attrs.append(f'android:strokeAlpha="{f(sa)}"')
        out.append("    <path\n        " + "\n        ".join(attrs) + " />")

    for ch in root:
        walk(ch, {})

    # honour a non-zero viewBox origin
    group_open = group_close = ""
    if vx or vy:
        group_open = f'    <group android:translateX="{f(-vx)}" android:translateY="{f(-vy)}">\n'
        group_close = "\n    </group>"

    xml = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Generated by tools/svg2vd.py from svg-icons/" + name + ".svg - edit the SVG, not this file -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{size}dp"\n    android:height="{size}dp"\n'
        f'    android:viewportWidth="{f(vw)}"\n    android:viewportHeight="{f(vh)}">\n'
        + group_open + "\n".join(out) + group_close + "\n</vector>\n"
    )
    return xml, warnings


def main(argv):
    if len(argv) == 3:
        pairs = [(argv[1], argv[2])]
    else:
        os.makedirs(DST, exist_ok=True)
        pairs = []
        for fn in sorted(os.listdir(SRC)):
            if fn.lower().endswith(".svg"):
                stem = re.sub(r"[^a-z0-9_]", "_", os.path.splitext(fn)[0].lower())
                if stem and stem[0].isdigit():
                    stem = "ic_" + stem
                pairs.append((os.path.join(SRC, fn), os.path.join(DST, stem + ".xml")))
    for src, dst in pairs:
        with open(src, encoding="utf-8") as fh:
            xml, warns = convert(fh.read(), os.path.splitext(os.path.basename(src))[0])
        with open(dst, "w", encoding="utf-8") as fh:
            fh.write(xml)
        print("ok  ", os.path.relpath(src, ROOT), "->", os.path.relpath(dst, ROOT), *("(" + w + ")" for w in warns))
    print(f"{len(pairs)} icon(s) converted")


if __name__ == "__main__":
    main(sys.argv)
