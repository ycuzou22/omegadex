"""Banque de défis par famille (import_banque_defis) : générée sur la base jeu embarquée."""

import json
import pathlib
import re
import sqlite3
import sys
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

import import_banque_defis as banque  # noqa: E402


class BanqueDefisTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        con = sqlite3.connect(f"file:{banque.BASE}?mode=ro", uri=True)
        cls.donnees = banque.lire_base(con)
        con.close()
        cls.objets = banque.lire_objets(cls.donnees["lieux"])
        cls.regles = banque.Regles(cls.donnees["etapes"], cls.objets, cls.donnees["lieux"])
        cls.obtention = banque.etapes_d_obtention(cls.donnees, cls.regles)
        cls.defis = banque.generer(cls.donnees, cls.objets)
        cls.par_famille = {}
        for d in cls.defis:
            cls.par_famille.setdefault(d["parametres"]["famille"], []).append(d)
        cls.noms = {i: e["nom"] for i, e in cls.donnees["especes"].items()}
        cls.par_nom = {e["nom"]: i for i, e in cls.donnees["especes"].items()}

    def test_chaque_famille_capturable_a_sa_banque_d_environ_200_defis(self):
        familles = banque.familles(self.donnees)
        capturables = {r for r, membres in familles.items() if any(m in self.obtention for m in membres)}
        self.assertEqual(capturables, set(self.par_famille))
        self.assertGreater(len(capturables), 200)
        for racine, defis in self.par_famille.items():
            self.assertLessEqual(len(defis), banque.DEFIS_PAR_FAMILLE, self.noms[racine])
            self.assertGreaterEqual(len(defis), 100, self.noms[racine])
        pleines = sum(len(d) == banque.DEFIS_PAR_FAMILLE for d in self.par_famille.values())
        self.assertGreater(pleines, 0.9 * len(self.par_famille))

    def test_les_defis_passent_la_validation_des_modeles_et_restent_dans_la_plage_de_leur_famille(self):
        banque.valider(self.defis)
        ids = [d["id"] for d in self.defis]
        self.assertEqual(len(ids), len(set(ids)))
        for d in self.defis:
            self.assertEqual((d["id"] - banque.ID_BANQUE) // banque.TAILLE_PLAGE, d["parametres"]["famille"])

    def test_un_defi_vise_un_membre_de_sa_famille_qu_on_peut_deja_obtenir(self):
        familles = banque.familles(self.donnees)
        for d in self.defis:
            cible = d["parametres"]["cible"]
            self.assertIn(cible, familles[d["parametres"]["famille"]])
            self.assertGreaterEqual(d["etapeMin"], self.obtention[cible], d["gabarit"])
            self.assertIn(self.noms[cible], d["gabarit"])

    def test_aucun_enonce_en_double_dans_une_famille_et_seuls_les_marqueurs_du_moteur_restent(self):
        for defis in self.par_famille.values():
            enonces = [d["gabarit"] for d in defis]
            self.assertEqual(len(enonces), len(set(enonces)))
        for d in self.defis:
            self.assertLessEqual(set(re.findall(r"\{(\w+)\}", d["gabarit"])), {"adversaire", "niveau"}, d["gabarit"])

    def test_un_niveau_cite_ne_depasse_jamais_le_plafond_des_etapes_ou_le_defi_est_propose(self):
        plafonds = [e["plafond"] for e in self.donnees["etapes"]]
        for d in self.defis:
            for niveau in re.findall(r"niveau (\d+)", d["gabarit"]):
                self.assertLessEqual(int(niveau), min(plafonds[d["etapeMin"]:]), d["gabarit"])

    def test_les_especes_qu_on_n_obtient_que_par_echange_n_ont_pas_de_defis(self):
        alakazam = self.par_nom["Alakazam"]  # Kadabra n'évolue que par échange
        self.assertNotIn(alakazam, self.obtention)
        self.assertFalse(any(d["parametres"]["cible"] == alakazam for d in self.defis))
        self.assertFalse(any("en Alakazam" in d["gabarit"] for d in self.defis))

    def test_une_exclusivite_de_saphir_alpha_ne_s_attrape_pas(self):
        rencontre = {"methode": "HERBE", "condition": "Saphir Alpha uniquement", "lieuEtape": 3}
        self.assertIsNone(banque.etape_de_la_rencontre(rencontre))
        surf = {"methode": "SURF", "condition": None, "lieuEtape": 2}
        self.assertEqual(banque.SURF, banque.etape_de_la_rencontre(surf))
        groudon = {"methode": "NAVI_DEX", "condition": "après avoir vaincu Groudon ou Kyogre", "lieuEtape": 4}
        self.assertEqual(banque.APRES_GROUDON, banque.etape_de_la_rencontre(groudon))

    def test_la_famille_d_arcko_couvre_ses_trois_membres_des_le_depart(self):
        arcko = self.par_famille[self.par_nom["Arcko"]]
        self.assertEqual(banque.DEFIS_PAR_FAMILLE, len(arcko))
        cibles = {self.noms[d["parametres"]["cible"]] for d in arcko}
        self.assertEqual({"Arcko", "Massko", "Jungko"}, cibles)
        self.assertTrue(any(d["etapeMin"] == 0 for d in arcko))
        massko = [d for d in arcko if d["gabarit"].startswith("Faire évoluer Massko en Jungko au niveau 36")]
        for d in massko:
            self.assertGreaterEqual(self.donnees["etapes"][d["etapeMin"]]["plafond"], 36)
        self.assertEqual({"NORMAL", "DIFFICILE", "HARDCORE"}, {d["difficulteMin"] for d in arcko})

    def test_la_generation_est_reproductible(self):
        self.assertEqual(self.defis, banque.generer(self.donnees, self.objets))

    def test_la_base_embarquee_contient_la_banque_a_jour(self):
        con = sqlite3.connect(f"file:{banque.BASE}?mode=ro", uri=True)
        lignes = con.execute("SELECT id, gabarit, parametres FROM modele_defi WHERE id >= ? ORDER BY id", (banque.ID_BANQUE,))
        embarquee = [(i, g, json.loads(p)) for i, g, p in lignes]
        con.close()
        self.assertEqual([(d["id"], d["gabarit"], d["parametres"]) for d in self.defis], embarquee)

    def test_un_defi_qui_vise_un_type_attend_qu_on_puisse_en_croiser_un_sauvage(self):
        sauvages = banque.etapes_des_types_sauvages(self.donnees)
        noms_types = {nom: t for t, nom in banque.NOMS_TYPES.items()}
        motif = re.compile(r"(?:Pokémon de type|Pokémon sauvage de type) ([A-Za-zÀ-ÿ]+)")
        for d in self.defis:
            if d["parametres"]["categorie"] not in ("type", "capture"):
                continue
            for nom in motif.findall(d["gabarit"]):
                self.assertGreaterEqual(d["etapeMin"], sauvages[noms_types[nom]], d["gabarit"])

    def test_pas_de_type_glace_ni_spectre_a_croiser_avant_l_etape_7(self):
        sauvages = banque.etapes_des_types_sauvages(self.donnees)
        self.assertEqual(7, sauvages["glace"])
        self.assertEqual(7, sauvages["spectre"])

    def test_la_base_embarquee_a_les_etapes_de_deblocage_a_jour(self):
        con = sqlite3.connect(f"file:{banque.BASE}?mode=ro", uri=True)
        embarquee = dict(con.execute("SELECT id, etapeMin FROM modele_defi WHERE id >= ?", (banque.ID_BANQUE,)))
        con.close()
        self.assertEqual({d["id"]: d["etapeMin"] for d in self.defis}, embarquee)

    def test_elision_des_noms(self):
        self.assertEqual("d'Arcko", banque.de("Arcko"))
        self.assertEqual("de Massko", banque.de("Massko"))
        self.assertEqual("qu'Écrapince", banque.que("Écrapince"))


if __name__ == "__main__":
    unittest.main()
