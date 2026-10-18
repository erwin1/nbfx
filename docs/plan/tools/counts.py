#!/usr/bin/env python3
"""Regenerate docs/plan/figures.md from the source tree.

The plan must state the tree, so its numbers must be generated, not hand-typed.
Run:   python3 docs/plan/tools/counts.py            # write figures.md
       python3 docs/plan/tools/counts.py --check    # fail if figures.md is stale

Stdlib only; run from the repository root.
"""
from __future__ import annotations

import glob
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
OUT = os.path.join(ROOT, "docs", "plan", "figures.md")

FX_DECL = re.compile(r"public (?:final )?(?:class|interface|@interface|enum|record) (Fx\w+)")
JAVA_SOURCE = "org.netbeans.api.java.source"
CONTROL = re.compile(r"new (TreeTableView|TreeView|TableView|ListView)<")


def read(path: str) -> str:
    try:
        with open(path, encoding="utf-8", errors="ignore") as fh:
            return fh.read()
    except OSError:
        return ""


def modules() -> list[str]:
    out = []
    for manifest in glob.glob(os.path.join(ROOT, "nbfx-*", "*", "manifest.mf")):
        out.append(os.path.dirname(manifest))
    return sorted(out)


def sources(mod: str) -> list[str]:
    return sorted(glob.glob(os.path.join(mod, "src", "**", "*.java"), recursive=True))


def tests() -> list[str]:
    return sorted(glob.glob(os.path.join(ROOT, "nbfx-*", "*", "test", "**", "*.java"), recursive=True))


def collect() -> list[tuple[str, str]]:
    mods = modules()
    exporting = [m for m in mods if "<public-packages>" in read(os.path.join(m, "nbproject", "project.xml"))]
    subpackages = [m for m in exporting if "<subpackages>" in read(os.path.join(m, "nbproject", "project.xml"))]
    arch = [m for m in mods if os.path.exists(os.path.join(m, "arch.xml"))]
    apichanges = [m for m in mods if os.path.exists(os.path.join(m, "apichanges.xml"))]

    src = [f for m in mods for f in sources(m)]
    specs = set()
    for m in mods:
        with open(os.path.join(m, "manifest.mf"), encoding="utf-8", errors="ignore") as fh:
            for line in fh:
                if "OpenIDE-Module-Specification-Version" in line:
                    specs.add(line.split(":", 1)[1].strip())

    fx_all: set[str] = set()
    fx_api: set[str] = set()
    for m in mods:
        for f in sources(m):
            names = set(FX_DECL.findall(read(f)))
            fx_all |= names
            if m in exporting:
                fx_api |= names

    control_files = [f for m in mods for f in sources(m) if CONTROL.search(read(f))]
    view_files = [f for m in mods for f in sources(m)
                  if re.search(r"TreeView|TreeTableView|TableView|ListView", read(f))]
    subscribe = sum(read(f).count(".subscribe(") for f in src)
    dispose_files = [f for f in src if "dispose()" in read(f)]
    js_importers = {os.path.basename(m) for m in mods
                    for f in sources(m) if JAVA_SOURCE in read(f)}
    js_files = [f for m in mods for f in sources(m) if JAVA_SOURCE in read(f)]
    semantic = [f for m in mods for f in sources(m)
                if JAVA_SOURCE in read(f) or "org.netbeans.api.java." in read(f)
                or "com.sun.source" in read(f) or "libs.javacapi" in read(f)]
    nbbundle = [f for f in src if "NbBundle" in read(f)]
    bundles = sorted(glob.glob(os.path.join(ROOT, "nbfx-*", "*", "src", "**", "Bundle.properties"), recursive=True))

    test_files = tests()
    tested = {m for m in mods if glob.glob(os.path.join(m, "test", "**", "*.java"), recursive=True)}

    rows: list[tuple[str, str]] = [
        ("nbfx modules", f"{len(mods)}"),
        ("nbfx source files", f"{len(src)}"),
        ("modules exporting packages (`<public-packages>`)", f"{len(exporting)}"),
        ("of those, subtree exports (`<subpackages>`)", f"{len(subpackages)}"),
        ("modules with `arch.xml`", f"{len(arch)}"),
        ("modules with `apichanges.xml`", f"{len(apichanges)}"),
        ("distinct `OpenIDE-Module-Specification-Version` values",
         f"{len(specs)}: {', '.join(sorted(specs))}"),
        ("public `Fx*` types (total)", f"{len(fx_all)}"),
        ("public `Fx*` types in exporting modules", f"{len(fx_api)}"),
        ("files constructing TreeView/TreeTableView/TableView/ListView", f"{len(control_files)}"),
        ("files mentioning a TreeView/TreeTableView/TableView/ListView", f"{len(view_files)}"),
        ("`.subscribe(` call sites", f"{subscribe}"),
        ("files mentioning `dispose()`", f"{len(dispose_files)}"),
        ("modules importing `org.netbeans.api.java.source`", f"{len(js_importers)}: {', '.join(sorted(js_importers))}"),
        ("files importing `org.netbeans.api.java.source`", f"{len(js_files)}"),
        ("files touching the Java semantic surface (`java.source`, `api.java.*`, `com.sun.source`, `libs.javacapi`)",
         f"{len(semantic)}"),
        ("files using `NbBundle`", f"{len(nbbundle)}"),
        ("`Bundle.properties` files", f"{len(bundles)}"),
        ("test files", f"{len(test_files)}"),
        ("modules with tests", f"{len(tested)} of {len(mods)}"),
    ]

    nb = os.path.join(ROOT, "netbeans")
    if os.path.isdir(nb):
        ups = []
        for group in ("ide/editor*", "ide/spi.editor*", "java/java.editor*",
                      "java/java.source*", "java/java.hints*"):
            for d in glob.glob(os.path.join(nb, group)):
                if os.path.isdir(os.path.join(d, "src")):
                    ups.append(d)
        swing = 0
        total = 0
        for d in ups:
            for f in glob.glob(os.path.join(d, "src", "**", "*.java"), recursive=True):
                total += 1
                t = read(f)
                if "import javax.swing" in t or "import java.awt" in t:
                    swing += 1
        rows.append(("upstream editor/Java modules (ide/editor*, java/java.source*, java.java.hints*)", f"{len(ups)}"))
        rows.append(("of their source files, Swing/AWT-touching", f"{swing} of {total}"))

    return rows


def render(rows: list[tuple[str, str]]) -> str:
    out = ["<!-- generated by docs/plan/tools/counts.py; do not edit by hand -->",
           "# Figures (generated)",
           "",
           "Every number the plan states is derived here from the tree. Regenerate with",
           "`python3 docs/plan/tools/counts.py`; `--check` fails when this file is stale.",
           "",
           "| Figure | Value |",
           "|---|---|"]
    for name, value in rows:
        out.append(f"| {name} | {value} |")
    out.append("")
    return "\n".join(out)


def main() -> int:
    text = render(collect())
    if "--check" in sys.argv:
        current = read(OUT)
        if current != text:
            print("figures.md is stale; run: python3 docs/plan/tools/counts.py", file=sys.stderr)
            return 1
        print("figures.md is up to date")
        return 0
    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write(text)
    print(f"wrote {os.path.relpath(OUT, ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
