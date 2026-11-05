"""brut.txt (Poképédia) -> app/src/main/assets/donnees/dresseurs.json, champions existants conservés."""
import json, os, re, sqlite3, sys, unicodedata
from collections import Counter

REPO = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../../app/src/main/assets')
BRUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), 'brut.txt')
DB = sqlite3.connect(f'{REPO}/database/omegadex_jeu.db')
ESPECES = {n: (i, t1, t2) for i, n, t1, t2 in DB.execute('select id,nom,type1,type2 from espece')}
LIEUX = {n: e for n, e in DB.execute('select nom,etapeDeblocageId from lieu')}
PLAFONDS = dict(DB.execute('select id,plafond from etape'))
STARTERS = {'Arcko': 252, 'Poussifeu': 255, 'Gobou': 258}
ARENES = {
    'Arène de Mérouville': 1, 'Arène de Myokara': 2, 'Arène de Lavandia': 3, 'Arène de Vermilava': 4,
    'Arène de Clémenti-Ville': 5, 'Arène de Cimetronelle': 6, "Arène d'Algatia": 7, "Arène d'Atalanopolis": 9,
}
NOMMES = {'Flora', 'Max', 'Kelvin', 'Lilie', 'Timmy', 'Amaryllis', 'Damien', 'Spectra', 'Glacia', 'Aragon',
          'Pierre Rochard', 'Marc'}
FEMININS = {'Flora', 'Lilie', 'Amaryllis', 'Spectra', 'Glacia'}
CLASSES_F = {'Fillette', 'Aromathérapeute', 'Topdresseuse', 'Nageuse', 'Pêcheuse de Perles', 'Combattante', 'Canon',
             'Mademoiselle', 'Sœur Parasol', 'Fillette de Conte', 'Experte', 'Éleveuse', 'Fille à la Bouée',
             'Gardienne du Savoir'}
CLASSES_P = {'Jumelles', 'Journalistes', 'Jeune Couple', 'Vieux Couple', 'Frère et Sœur', 'Senior et Junior',
             'Topduo', 'Cerveau et Muscles', 'Sœurs Étranges'}
QUETES = {('Route Victoire', 'Timmy'): 35, ('Ligue Pokémon', 'Pierre Rochard'): 37, ('Caverne Fondmer', 'Max'): 32}
DELTA, LIGUE, SURF = 11, 10, 6


def slug(t):
    t = unicodedata.normalize('NFD', t.lower().replace('&', 'et').replace('œ', 'oe'))
    t = ''.join(c for c in t if unicodedata.category(c) != 'Mn')
    return re.sub(r'[^a-z0-9]+', '-', t).strip('-')


def equipe(texte):
    out = []
    for pk in texte.split(','):
        nom, niv = re.match(r'\s*(.+?)\s+(\d+)$', pk).groups()
        out.append({'especeId': ESPECES[nom][0], 'niveau': int(niv)})
    return out


def type_dominant(eq):
    c = Counter()
    for p in eq:
        _, t1, t2 = next(v for v in ESPECES.values() if v[0] == p['especeId'])
        c[t1] += 1
        if t2:
            c[t2] += 1
    premier = next(v for v in ESPECES.values() if v[0] == eq[0]['especeId'])[1]
    best = max(c.values())
    return premier if c[premier] == best else c.most_common(1)[0][0]


def type_as(eq):
    """Type du Pokémon le plus fort : celui qui donne le ton d'un personnage nommé (Flora, Max…)."""
    plus_fort = max(eq, key=lambda p: p['niveau'])
    return next(v for v in ESPECES.values() if v[0] == plus_fort['especeId'])[1]


def etape_lieu(lieu):
    if lieu in ARENES:
        return ARENES[lieu]
    return LIEUX[re.sub(r' \(.*\)$', '', lieu)]


def etape_niveau(depart, niveau_max):
    e = depart
    while e < LIGUE and PLAFONDS[e] < niveau_max:
        e += 1
    return e


def genre(classe, nom):
    if classe in CLASSES_P or '&' in nom:
        return 'p'
    if nom in FEMININS or (nom not in NOMMES and classe in CLASSES_F):
        return 'f'
    return None


dresseurs, cles = [], Counter()
for ordre, ligne in enumerate(open(BRUT, encoding='utf-8')):
    ligne = ligne.strip()
    if not ligne or ligne.startswith('#'):
        continue
    tag = None
    if ligne.startswith('@'):
        tag, ligne = ligne.split(' ', 1)
    parts = ligne.split('|')
    if tag == '@FLORA':
        lieu, starter, eq = parts
        classe, nom, starter = 'Rivale', 'Flora', STARTERS[starter]
    else:
        lieu, classe, nom, eq = parts
        starter = None
    eq = equipe(eq)
    niveau_max = max(p['niveau'] for p in eq)
    if tag == '@DELTA':
        etape = DELTA
    elif tag == '@LIGUE':
        etape = LIGUE
    else:
        depart = etape_lieu(lieu)
        # Plus fort que le plafond du lieu, hors arène : la partie du lieu qui demande Surf (après Norman).
        if tag is None and lieu not in ARENES and depart < SURF and niveau_max > PLAFONDS[depart]:
            depart = SURF
        etape = etape_niveau(depart, niveau_max)
    nom_affiche = nom or classe
    nomme = nom in NOMMES
    base = slug(nom) + '-' + slug(lieu) if nomme else '-'.join(filter(None, [slug(lieu), slug(classe), slug(nom)]))
    cles[base] += 1
    # Les trois variantes de Flora partagent la même clé : une seule est montrée par run.
    cle = base if cles[base] == 1 or tag == '@FLORA' else f'{base}-{cles[base]}'
    d = {'cle': cle, 'etapeId': etape, 'nom': nom_affiche, 'titre': classe, 'lieu': lieu,
         'type': type_as(eq) if nomme else type_dominant(eq), 'image': slug(nom) if nomme else slug(classe)}
    q = QUETES.get((lieu, nom))
    if q:
        d['queteId'] = q
    if starter:
        d['starter'] = starter
    g = genre(classe, nom)
    if g:
        d['genre'] = g
    d['equipe'] = [{**p, 'attaques': []} for p in eq]
    d['_ordre'] = ordre
    dresseurs.append(d)

existant = json.load(open(f'{REPO}/donnees/dresseurs.json', encoding='utf-8'))
CHAMPIONS = {'roxanne', 'bastien', 'voltere', 'adriane', 'norman', 'alizee', 'levy-et-tatia', 'marc'}
champions = [c for c in existant['dresseurs'] if c['cle'] in CHAMPIONS]
GENRES_CHAMPIONS = {'roxanne': 'f', 'adriane': 'f', 'alizee': 'f', 'levy-et-tatia': 'p'}
for c in champions:
    c.pop('genre', None)
    c['image'] = c['cle']
    if c['cle'] in GENRES_CHAMPIONS:
        c['genre'] = GENRES_CHAMPIONS[c['cle']]
    c['_ordre'] = 10_000  # le champion ferme son étape

# L'adversaire de l'étape la ferme, comme un champion : Flora au départ, Amaryllis pour l'Épisode Delta.
for d in dresseurs:
    if d['cle'] in ('flora-route-103', 'amaryllis-pilier-celeste'):
        d['_ordre'] = 10_000
tous = sorted(dresseurs + champions, key=lambda d: (d['etapeId'], d['_ordre']))
for d in tous:
    d.pop('_ordre')
sortie = {
    'source': 'Poképédia, équipes du premier combat dans Pokémon Rubis Oméga / Saphir Alpha',
    'dresseurs': tous,
}
with open(f'{REPO}/donnees/dresseurs.json', 'w', encoding='utf-8') as f:
    # Un dresseur par ligne : le fichier reste lisible et ses diffs courts.
    lignes = ',\n    '.join(json.dumps(d, ensure_ascii=False) for d in tous)
    f.write('{\n  "source": ' + json.dumps(sortie['source'], ensure_ascii=False)
            + ',\n  "dresseurs": [\n    ' + lignes + '\n  ]\n}\n')
print(len(tous), Counter(d['etapeId'] for d in tous))
assert len({(d['cle'], d.get('starter')) for d in tous}) == len(tous)
