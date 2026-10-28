"""Contrôle la couverture des tests JVM à partir du rapport JaCoCo de la CI.

Affiche la couverture des lignes de chaque paquet du domaine, puis échoue si un paquet surveillé
descend sous son seuil. Le rapport est produit par ``./gradlew createDebugUnitTestCoverageReport``.

Usage : python3 tools/couverture/verifier_couverture.py [chemin/vers/report.xml]
"""

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

RAPPORT_PAR_DEFAUT = Path("app/build/reports/coverage/test/debug/report.xml")
PREFIXE_DOMAINE = "com/omegadex/app/domain"

# Paquet surveillé -> couverture minimale des lignes (en %).
SEUILS = {
    "com/omegadex/app/domain/defi": 70.0,  # T11.4 : moteur de défis
}


def lignes(element):
    """Lignes (couvertes, manquées) d'un élément JaCoCo."""
    for compteur in element.findall("counter"):
        if compteur.get("type") == "LINE":
            return int(compteur.get("covered")), int(compteur.get("missed"))
    return 0, 0


def pourcentage(couvertes, manquees):
    total = couvertes + manquees
    return 100.0 if total == 0 else 100.0 * couvertes / total


def trouver_rapport(argument):
    if argument:
        return Path(argument)
    if RAPPORT_PAR_DEFAUT.exists():
        return RAPPORT_PAR_DEFAUT
    trouves = sorted(Path("app/build/reports/coverage").rglob("report.xml"))
    return trouves[0] if trouves else RAPPORT_PAR_DEFAUT


def main():
    rapport = trouver_rapport(sys.argv[1] if len(sys.argv) > 1 else None)
    if not rapport.exists():
        print(f"::error::Rapport de couverture introuvable : {rapport}")
        return 1

    racine = ET.parse(rapport).getroot()
    paquets = {p.get("name"): lignes(p) for p in racine.iter("package")}
    domaine = {nom: compte for nom, compte in paquets.items() if nom.startswith(PREFIXE_DOMAINE)}

    print("Couverture des lignes du domaine :")
    for nom, (couvertes, manquees) in sorted(domaine.items()):
        print(f"  {pourcentage(couvertes, manquees):6.1f} %  {nom.replace('/', '.')}")
    total_couvertes = sum(c for c, _ in domaine.values())
    total_manquees = sum(m for _, m in domaine.values())
    print(f"  {pourcentage(total_couvertes, total_manquees):6.1f} %  domaine (total)")

    echec = False
    for nom, seuil in SEUILS.items():
        if nom not in paquets:
            print(f"::error::Paquet {nom} absent du rapport de couverture.")
            echec = True
            continue
        taux = pourcentage(*paquets[nom])
        detail = {}
        for paquet in racine.iter("package"):
            if paquet.get("name") == nom:
                for classe in paquet.findall("class"):
                    fichier = classe.get("sourcefilename") or classe.get("name")
                    couvertes, manquees = lignes(classe)
                    avant = detail.get(fichier, (0, 0))
                    detail[fichier] = (avant[0] + couvertes, avant[1] + manquees)
        resume = ", ".join(
            f"{fichier} {pourcentage(*compte):.0f} %" for fichier, compte in sorted(detail.items())
        )
        print(f"::notice::{nom.replace('/', '.')} {taux:.1f} % : {resume}")
        if taux < seuil:
            print(f"::error::Couverture de {nom.replace('/', '.')} : {taux:.1f} % < {seuil:.0f} %.")
            echec = True
    return 1 if echec else 0


if __name__ == "__main__":
    sys.exit(main())
