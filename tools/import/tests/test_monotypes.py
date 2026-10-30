"""Défis Monotype : proposés seulement quand le type réunit assez de familles obtenables."""

import pathlib
import sqlite3
import sys
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))

import import_banque_defis as banque  # noqa: E402
import import_defis  # noqa: E402
import monotypes  # noqa: E402


class MonotypesTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        con = sqlite3.connect(f"file:{banque.BASE}?mode=ro", uri=True)
        cls.donnees = banque.lire_base(con)
        cls.embarques = dict(con.execute("SELECT id, etapeMin FROM modele_defi WHERE type = 'MONOTYPE'"))
        con.close()
        regles = banque.Regles(cls.donnees["etapes"], banque.lire_objets(cls.donnees["lieux"]), cls.donnees["lieux"])
        cls.familles = monotypes.familles_par_type(cls.donnees, regles)
        cls.etapes = monotypes.etapes_monotype(cls.donnees, regles)
        cls.modeles = [m for m in import_defis.charger() if m["type"] == "MONOTYPE"]

    def test_chaque_monotype_attend_deux_familles_de_son_type_hors_starters(self):
        for m in self.modeles:
            t = m["parametres"]["type"]
            etape = m["deblocage"]["etapeMin"]
            self.assertEqual(self.etapes[t], etape, m["titre"])
            self.assertGreaterEqual(len(self.familles[t][etape]), monotypes.FAMILLES_MIN, m["titre"])

    def test_aucun_monotype_impossible_contre_bastien(self):
        # Bastien est l'adversaire de l'étape 2 : ni Feu, ni Glace, ni Spectre, ni Dragon à ce moment-là.
        proposes = {m["parametres"]["type"] for m in self.modeles if m["deblocage"]["etapeMin"] <= 2}
        self.assertFalse({"feu", "glace", "spectre", "dragon", "electrik"} & proposes)

    def test_les_starters_ne_comptent_pas(self):
        racines = set().union(*(familles for par_etape in self.familles.values() for familles in par_etape))
        self.assertFalse(monotypes.STARTERS & racines)

    def test_la_base_embarquee_a_les_etapes_du_fichier(self):
        self.assertEqual({m["id"]: m["deblocage"]["etapeMin"] for m in self.modeles}, self.embarques)


if __name__ == "__main__":
    unittest.main()
