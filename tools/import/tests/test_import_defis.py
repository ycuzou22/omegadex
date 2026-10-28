"""Tests de la validation des modèles de défis : python3 -m unittest discover tools/import/tests"""

import copy
import json
import pathlib
import sys
import tempfile
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))

import import_defis  # noqa: E402

VALIDE = {
    "id": 7,
    "type": "MONOTYPE",
    "etape": None,
    "titre": "Monotype",
    "gabarit": "Battre {adversaire} uniquement avec des Pokémon de type {type}",
    "parametres": {"type": "eau"},
    "difficulteMin": "NORMAL",
    "deblocage": {"etapeMin": 1, "conditions": []},
}


def avec(**changements):
    m = copy.deepcopy(VALIDE)
    m.update(changements)
    return m


class ValidationDefisTest(unittest.TestCase):
    def rejet(self, modele, extrait):
        with self.assertRaises(import_defis.ModeleInvalide) as e:
            import_defis.valider(modele, 3)
        self.assertIn(extrait, str(e.exception))
        self.assertIn("modèle n° 3 (id 7)", str(e.exception))

    def test_modele_valide(self):
        import_defis.valider(VALIDE, 1)

    def test_fichier_du_depot_valide(self):
        self.assertGreater(len(import_defis.charger()), 0)

    def test_type_inconnu(self):
        self.rejet(avec(type="ESPEC"), "type « ESPEC » inconnu")

    def test_champ_manquant(self):
        m = avec()
        del m["gabarit"]
        self.rejet(m, "champ obligatoire « gabarit » manquant")

    def test_champ_inconnu(self):
        self.rejet(avec(couleur="rouge"), "champ(s) inconnu(s) : couleur")

    def test_marqueur_sans_valeur(self):
        self.rejet(avec(parametres={}), "marqueur(s) sans valeur dans « parametres » : {type}")

    def test_type_pokemon_inconnu(self):
        self.rejet(avec(parametres={"type": "lumiere"}), "type Pokémon « lumiere » inconnu")

    def test_defi_d_etape_sans_etape(self):
        self.rejet(avec(type="ETAPE", etape=None), "doit indiquer « etape » entre 0 et 12")

    def test_ecart_au_dessus_du_plafond(self):
        self.rejet(avec(parametres={"type": "eau", "ecart": -3}), "ne vise jamais au-dessus du plafond")

    def test_condition_inconnue(self):
        self.rejet(avec(deblocage={"etapeMin": 1, "conditions": ["TOUJOURS"]}), "condition inconnue")

    def test_difficulte_inconnue(self):
        self.rejet(avec(difficulteMin="FACILE"), "« difficulteMin » doit valoir")

    def test_id_en_double(self):
        with tempfile.TemporaryDirectory() as d:
            fichier = pathlib.Path(d) / "defis.json"
            fichier.write_text(json.dumps({"modeles": [VALIDE, VALIDE]}), encoding="utf-8")
            with self.assertRaises(import_defis.ModeleInvalide) as e:
                import_defis.charger(fichier)
            self.assertIn("id 7 déjà utilisé", str(e.exception))

    def test_json_illisible(self):
        with tempfile.TemporaryDirectory() as d:
            fichier = pathlib.Path(d) / "defis.json"
            fichier.write_text('{"modeles": [', encoding="utf-8")
            with self.assertRaises(import_defis.ModeleInvalide) as e:
                import_defis.charger(fichier)
            self.assertIn("n'est pas du JSON valide (ligne 1", str(e.exception))


if __name__ == "__main__":
    unittest.main()
