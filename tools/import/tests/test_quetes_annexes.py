"""Quêtes annexes et exclusivités de version : python3 -m unittest discover -s tools/import/tests"""

import pathlib
import sqlite3
import sys
import tempfile
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))

import creer_base_jeu  # noqa: E402
import import_etapes  # noqa: E402
import import_lieux  # noqa: E402
import verifier  # noqa: E402

SAPHIR = "Saphir Alpha uniquement"
RUBIS = "Rubis Oméga uniquement"
HORS_HOENN = 99

# id, nom ; Nénupiot (270) → Lombre (271) → Ludicolo (272) n'existent à l'état sauvage que dans Saphir Alpha.
ESPECES = [
    (263, "Zigzaton"),
    (270, "Nénupiot"),
    (271, "Lombre"),
    (272, "Ludicolo"),
    (273, "Grainipiot"),
    (302, "Ténéfix"),
]
LIEUX = [
    (1, "Route 101", "ROUTE", 0),
    (2, "Route 102", "ROUTE", 0),
    (3, "Grotte Granite", "GROTTE", 2),
    (4, "Route 114", "ROUTE", 4),
    (5, "Grotte Origine", "GROTTE", 8),
    (HORS_HOENN, import_lieux.NOM_HORS_HOENN, "HORS_HOENN", import_lieux.ETAPE_POKEDEX_NATIONAL),
]
# (lieuId, especeId, methode, niveauMin, niveauMax, taux, condition)
RENCONTRES = [
    (1, 263, "HERBE", 2, 3, 30, None),
    (2, 270, "HERBE", 2, 3, 15, SAPHIR),
    (2, 270, "HORDE", 2, 2, 35, SAPHIR),
    (4, 271, "HORDE", 9, 9, 35, SAPHIR),
    (2, 273, "HERBE", 2, 3, 15, RUBIS),
    # Ténéfix : Saphir Alpha à la Grotte Granite, mais aussi capturable plus tard dans Rubis Oméga.
    (3, 302, "HERBE", 10, 12, 10, SAPHIR),
    (5, 302, "HORDE", 30, 30, 35, None),
]


class QuetesAnnexesTest(unittest.TestCase):
    def setUp(self):
        self.dossier = tempfile.TemporaryDirectory()
        base = pathlib.Path(self.dossier.name) / "jeu.db"
        creer_base_jeu.creer(base, 1, "test")
        self.con = sqlite3.connect(base)
        for eid, nom in ESPECES:
            self.con.execute(
                "INSERT INTO espece VALUES (?,?,?,'eau',NULL,'',0,0,1,1,1,1,1,1,'',45,0.5,'',0)",
                (eid, eid - 251, nom),
            )
        self.con.executemany("INSERT INTO evolution VALUES (?,?,?,'NIVEAU','14')", [(1, 270, 271), (2, 271, 272)])
        self.con.executemany("INSERT INTO lieu VALUES (?,?,?,?)", LIEUX)
        self.con.executemany(
            "INSERT INTO rencontre (lieuId, especeId, methode, niveauMin, niveauMax, taux, condition) "
            "VALUES (?,?,?,?,?,?,?)",
            RENCONTRES,
        )
        self.echange_requis = import_lieux.especes_echange_requis(self.con, RENCONTRES, [e for e, _ in ESPECES])
        self.con.executemany(
            "INSERT INTO rencontre (lieuId, especeId, methode) VALUES (?,?,'ECHANGE_REQUIS')",
            [(HORS_HOENN, e) for e in self.echange_requis],
        )
        for etape in range(13):
            self.con.execute("INSERT INTO etape VALUES (?,?,?,NULL,10,'')", (etape, f"Étape {etape}", "Champion"))

    def tearDown(self):
        self.con.close()
        self.dossier.cleanup()

    def quetes(self):
        return self.con.execute(
            "SELECT etapeId, ordre, lieuId, description FROM quete WHERE type = 'ANNEXE' ORDER BY etapeId, ordre"
        ).fetchall()

    def test_une_rencontre_de_saphir_alpha_n_existe_pas_dans_rubis_omega(self):
        self.assertTrue(import_lieux.dans_rubis_omega(None))
        self.assertTrue(import_lieux.dans_rubis_omega(RUBIS))
        self.assertTrue(import_lieux.dans_rubis_omega("Hordes"))
        self.assertFalse(import_lieux.dans_rubis_omega(SAPHIR))
        self.assertFalse(import_lieux.dans_rubis_omega(f"forme Été; {SAPHIR}"))

    def test_les_exclusivites_de_saphir_alpha_et_leur_famille_demandent_un_echange(self):
        self.assertEqual([270, 271, 272], self.echange_requis)

    def test_les_quetes_d_echange_remplacent_les_captures_impossibles(self):
        echanges = import_etapes.generer_quetes_annexes(self.con, 1)

        self.assertEqual(2, echanges)
        # Zigzaton, Grainipiot et Ténéfix se capturent dans Rubis Oméga : pas de quête, le bouton « Attraper » suffit.
        self.assertEqual(
            [
                (0, 3, None, "Obtenir Nénupiot par échange (exclusif à Saphir Alpha)"),
                (4, 1, None, "Obtenir Lombre par échange (exclusif à Saphir Alpha)"),
            ],
            self.quetes(),
        )

    def test_les_captures_gardent_leur_numero_reserve(self):
        import_etapes.generer_quetes_annexes(self.con, 1)
        # Numéros 1 et 2 : captures de l'étape 0 ; 3 : l'échange de Nénupiot ; 4 : l'échange de Lombre.
        self.assertEqual([3, 4], [i for (i,) in self.con.execute("SELECT id FROM quete ORDER BY id")])

    def test_le_controle_refuse_une_quete_de_capture_et_un_echange_injustifie(self):
        import_etapes.generer_quetes_annexes(self.con, 1)
        controle = verifier.Controle()
        verifier.controler_quetes_annexes(self.con, controle)
        self.assertEqual([], controle.erreurs)

        self.con.execute("INSERT INTO quete VALUES (100, 0, 2, 'ANNEXE', 9, 'Capturer Nénupiot (Route 102)', NULL)")
        self.con.execute("INSERT INTO quete VALUES (101, 0, NULL, 'ANNEXE', 10, 'Obtenir Zigzaton par échange', NULL)")
        controle = verifier.Controle()
        verifier.controler_quetes_annexes(self.con, controle)
        self.assertEqual(2, len(controle.erreurs))
        self.assertIn("Capturer Nénupiot (Route 102)", controle.erreurs[0])
        self.assertIn("Obtenir Zigzaton par échange", controle.erreurs[1])


if __name__ == "__main__":
    unittest.main()
