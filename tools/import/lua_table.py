"""Lecture d'une table Lua littérale (module de données Poképédia) en objets Python."""

from __future__ import annotations

import re

_JETON = re.compile(r'\s*(\{|\}|,|=|\[|\]|"(?:\\.|[^"\\])*"|-?\d+(?:\.\d+)?|true|false|nil|[A-Za-z_]\w*)', re.S)


def _jetons(texte: str) -> list[str]:
    texte = re.sub(r"--[^\n]*", "", texte)
    position, jetons = 0, []
    while position < len(texte):
        m = _JETON.match(texte, position)
        if not m:
            if texte[position:].strip() == "":
                break
            raise ValueError(f"Lua illisible près de : {texte[position:position + 40]!r}")
        jetons.append(m.group(1))
        position = m.end()
    return jetons


def lire(texte: str):
    """Évalue `return { … }` : tables à clés → dict, tables séquentielles → list."""
    jetons = _jetons(texte.strip().removeprefix("return"))
    valeur, fin = _valeur(jetons, 0)
    return valeur


def _valeur(j: list[str], i: int):
    t = j[i]
    if t == "{":
        return _table(j, i + 1)
    if t.startswith('"'):
        return bytes(t[1:-1], "utf-8").decode("unicode_escape").encode("latin-1").decode("utf-8"), i + 1
    if t in ("true", "false"):
        return t == "true", i + 1
    if t == "nil":
        return None, i + 1
    return (float(t) if "." in t else int(t)), i + 1


def _table(j: list[str], i: int):
    cles: dict = {}
    sequence: list = []
    while j[i] != "}":
        if j[i] == "[":
            cle, i = _valeur(j, i + 1)
            assert j[i] == "]" and j[i + 1] == "=", j[i : i + 3]
            cles[cle], i = _valeur(j, i + 2)
        elif j[i + 1] == "=" and re.fullmatch(r"[A-Za-z_]\w*", j[i]):
            cles[j[i]], i = _valeur(j, i + 2)
        else:
            v, i = _valeur(j, i)
            sequence.append(v)
        if j[i] == ",":
            i += 1
    return (cles if cles else sequence), i + 1
