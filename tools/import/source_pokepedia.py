"""Accès aux pages de Poképédia (https://www.pokepedia.fr), source des lieux et rencontres de Rubis Oméga.

Le wikitexte des pages est téléchargé par l'API MediaWiki (une requête par seconde au plus,
avec un User-Agent identifiant le projet) puis mis en cache dans tools/import/.cache/pokepedia.
Contenu sous licence CC BY-NC-SA 3.0 : seuls des faits (espèces, niveaux, méthodes) en sont extraits,
la source est citée dans docs/sources-donnees.md.
"""

from __future__ import annotations

import json
import pathlib
import time
import urllib.parse
import urllib.request

API = "https://www.pokepedia.fr/api.php"
CACHE = pathlib.Path(__file__).resolve().parent / ".cache" / "pokepedia"
AGENT = "Omegadex-import/1.0 (https://github.com/ycuzou22/omegadex ; projet personnel non commercial)"
PAUSE_S = 1.0
# Données de localisation de chaque espèce dans Rubis Oméga, utilisées pour la vérification croisée (T04.5).
MODULE_LOCALISATIONS = "Module:Localisations/Données/Pokémon Rubis Oméga et Saphir Alpha"


def _fichier(titre: str) -> pathlib.Path:
    return CACHE / (titre.replace("/", "_").replace(" ", "_") + ".wiki")


def wikitexte(titre: str, rafraichir: bool = False) -> str:
    """Wikitexte de la page (après redirection), depuis le cache ou l'API."""
    fichier = _fichier(titre)
    if fichier.exists() and not rafraichir:
        return fichier.read_text(encoding="utf-8")
    parametres = {
        "action": "parse",
        "page": titre,
        "prop": "wikitext",
        "redirects": "1",
        "format": "json",
        "formatversion": "2",
    }
    requete = urllib.request.Request(f"{API}?{urllib.parse.urlencode(parametres)}", headers={"User-Agent": AGENT})
    with urllib.request.urlopen(requete, timeout=30) as reponse:
        donnees = json.load(reponse)
    time.sleep(PAUSE_S)
    if "error" in donnees:
        raise LookupError(f"Poképédia : page « {titre} » introuvable ({donnees['error'].get('info')})")
    texte = donnees["parse"]["wikitext"]
    fichier.parent.mkdir(parents=True, exist_ok=True)
    fichier.write_text(texte, encoding="utf-8")
    return texte


def telecharger_tout() -> list[str]:
    """Télécharge le module de localisations et la page du champion de chaque étape ; renvoie les échecs."""
    pages = [MODULE_LOCALISATIONS]
    etapes = json.loads((pathlib.Path(__file__).resolve().parent / "donnees" / "etapes_rosa.json").read_text(encoding="utf-8"))
    pages += [e["page"] for e in etapes["etapes"]]
    echecs = []
    for page in dict.fromkeys(pages):
        try:
            wikitexte(page)
        except (LookupError, OSError) as erreur:
            echecs.append(f"{page} : {erreur}")
    return echecs


if __name__ == "__main__":
    for ligne in telecharger_tout():
        print("INTROUVABLE", ligne)
