"""Quêtes d'objets : python3 -m unittest discover -s tools/import/tests"""

import json
import pathlib
import sqlite3
import sys
import tempfile
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))

import creer_base_jeu  # noqa: E402
import import_etapes  # noqa: E402

LIEUX = [
    (1, "Mérouville", "VILLE", 1),
    (2, "Route 104", "ROUTE", 1),
    (3, "Grotte Granite", "GROTTE", 2),
    (4, "Atalanopolis", "VILLE", 8),
]
OBJETS = [
    {"nom": "CT39 Tomberoche", "lieu": "Mérouville", "methode": "RECOMPENSE", "cs": None, "condition": None},
    # Route 104 est atteinte à l'étape 1, mais l'objet est derrière un arbre : Coupe, utilisable à l'étape 2.
    {"nom": "Pierre Feuille", "lieu": "Route 104", "methode": "SOL", "cs": "COUPE", "condition": None},
    {"nom": "CT70 Flash", "lieu": "Grotte Granite", "methode": "CADEAU", "cs": None, "condition": None},
    # Plusieurs capacités : la dernière devenue utilisable compte (Vélo Cross à l'étape 3, Surf à l'étape 6).
    {"nom": "Pierre Eau", "lieu": "Route 104", "methode": "SOL", "cs": ["VELO_CROSS", "SURF"], "condition": None},
    # Lieu de l'étape 2, mais la condition d'histoire le repousse après la Ligue.
    {"nom": "Méga-Gemme X", "lieu": "Grotte Granite", "methode": "SOL", "cs": None, "condition": "après la Ligue"},
    {"nom": "Objet sans lieu", "lieu": None, "methode": "SOL", "cs": None, "condition": None},
    # Exceptions : étape imposée malgré un lieu débloqué plus tard, et lieu mirage absent de la base.
    {"nom": "Méga-Bracelet", "lieu": "Atalanopolis", "methode": "CADEAU", "cs": None, "condition": None, "etape": 6},
    {"nom": "CT90 Clonage", "lieu": None, "lieuLibre": "Île Mirage", "methode": "SOL", "cs": "ECLATE_ROC", "etape": 12},
]


class QuetesObjetsTest(unittest.TestCase):
    def setUp(self):
        self.dossier = tempfile.TemporaryDirectory()
        dossier = pathlib.Path(self.dossier.name)
        creer_base_jeu.creer(dossier / "jeu.db", 1, "test")
        self.con = sqlite3.connect(dossier / "jeu.db")
        self.con.executemany("INSERT INTO lieu VALUES (?,?,?,?)", LIEUX)
        for etape in range(13):
            self.con.execute("INSERT INTO etape VALUES (?,?,?,NULL,10,'')", (etape, f"Étape {etape}", "Champion"))
        # Une quête de capture existe déjà à l'étape 2 : les objets passent après elle.
        self.con.execute("INSERT INTO quete VALUES (1, 2, 3, 'ANNEXE', 1, 'Capturer Ténéfix (Grotte Granite)', NULL)")
        self.fichier = dossier / "objets.json"
        self.fichier.write_text(json.dumps({"objets": OBJETS}, ensure_ascii=False), encoding="utf-8")

    def tearDown(self):
        self.con.close()
        self.dossier.cleanup()

    def quetes(self):
        return self.con.execute(
            "SELECT etapeId, ordre, description, recompense FROM quete WHERE recompense IS NOT NULL ORDER BY etapeId, ordre"
        ).fetchall()

    def test_chaque_objet_arrive_quand_il_devient_accessible(self):
        self.assertEqual(7, import_etapes.generer_quetes_objets(self.con, self.fichier))
        self.assertEqual(
            [
                (1, 1, "Gagner CT39 Tomberoche (Mérouville)", "CT39 Tomberoche"),
                (2, 2, "Récupérer Pierre Feuille (Route 104) · Coupe", "Pierre Feuille"),
                (2, 3, "Recevoir CT70 Flash (Grotte Granite)", "CT70 Flash"),
                (6, 1, "Récupérer Pierre Eau (Route 104) · Vélo Cross + Surf", "Pierre Eau"),
                (6, 2, "Recevoir Méga-Bracelet (Atalanopolis)", "Méga-Bracelet"),
                (10, 1, "Récupérer Méga-Gemme X (Grotte Granite)", "Méga-Gemme X"),
                (12, 1, "Récupérer CT90 Clonage (Île Mirage) · Éclate-Roc", "CT90 Clonage"),
            ],
            self.quetes(),
        )

    def test_rejouer_ne_duplique_pas_les_quetes(self):
        import_etapes.generer_quetes_objets(self.con, self.fichier)
        import_etapes.generer_quetes_objets(self.con, self.fichier)
        self.assertEqual(7, len(self.quetes()))
        self.assertEqual(1, self.con.execute("SELECT COUNT(*) FROM quete WHERE recompense IS NULL").fetchone()[0])

    def test_un_lieu_ou_une_cs_inconnus_sont_refuses(self):
        for objet in (
            {"nom": "X", "lieu": "Nulle Part", "cs": None},
            {"nom": "X", "lieu": "Route 104", "cs": "TELEPORT"},
            {"nom": "X", "lieu": "Route 104", "cs": ["SURF", "TELEPORT"]},
        ):
            self.fichier.write_text(json.dumps({"objets": [objet]}), encoding="utf-8")
            with self.assertRaises(ValueError):
                import_etapes.generer_quetes_objets(self.con, self.fichier)

    def test_le_fichier_du_jeu_ne_reference_que_des_lieux_connus(self):
        fichier = import_etapes.DOSSIER / "donnees" / "objets_rosa.json"
        if not fichier.exists():
            self.skipTest("objets_rosa.json absent")
        lieux = {lieu["nom"] for lieu in json.loads((import_etapes.DOSSIER / "donnees" / "lieux_rosa.json").read_text("utf-8"))["lieux"]}
        for objet in json.loads(fichier.read_text("utf-8"))["objets"]:
            self.assertTrue(objet.get("lieu") is None or objet["lieu"] in lieux, objet)
            for cs in import_etapes.capacites_objet(objet):
                self.assertIn(cs, import_etapes.CS_ETAPE, objet)


if __name__ == "__main__":
    unittest.main()
