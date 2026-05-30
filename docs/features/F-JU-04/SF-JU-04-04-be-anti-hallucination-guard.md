# Mini-spec — F-JU-04 / SF-JU-04-04 — Garde-fou anti-hallucination des arrêts BE

## Identifiant
`F-JU-04 / SF-JU-04-04`

## Date
2026-05-30

## Branche Git
`feat/SF-JU-04-04-be-anti-hallucination-guard`

## Type
Durcissement qualité — bloquant pour le passage à l'échelle des ~80 outils BE.

---

## Objectif (1 phrase)
Rejeter, à la source (`JurisprudenceBeWebSearchClient.parseOne`), les arrêts belges renvoyés par web_search qui présentent un signe d'hallucination, pour ne laisser passer que des citations crédibles.

## Contexte
Pilote BE du 2026-05-30 (après SF-JU-04-03) : 7/10 outils couverts, mais **~20-30 % de citations douteuses** :
- `divorce-ddi-3voies-be` : `lien` = page de **recherche** juricaf (`/recherche/%22Code+civil%22…`), pas un arrêt → inexploitable ;
- `autorite-parentale-be` : « 2e chambre » (pénal) pour de l'autorité parentale + `numero_pourvoi` = fragment ECLI `ARR.20210520.2F.1` (pas un n° de rôle).

Décision PO « soyons prudent » : ces mappings ont été archivés ; aucun scaling sans ce garde-fou.

## Comportement attendu

### Nominal
`parseOne` n'accepte un arrêt BE que si **les deux** conditions sont remplies :
1. **Lien crédible** : `lien` non vide ET ne contient AUCUN marqueur de page de recherche (`/recherche/`, `query=`, `q=`, `/search`).
2. **Numéro crédible** : `numero_pourvoi` contient soit un **n° de rôle belge** (`[A-Z]\.\d{2}\.\d{3,4}\.[FN]` — ex. `S.10.0044.F`, `C.18.0294.F`, `S.20.0019.N`), soit un **numéro de Cour constitutionnelle** (`\d{1,3}/\d{4}` — ex. `121/2013`).

Sinon → l'arrêt est **rejeté** (`return null`), logué en WARN avec le motif. Les arrêts crédibles restants suivent le flux normal (évaluateur Claude → INSERT).

### Cas d'erreur / rejets attendus (cas réels du pilote)
| Arrêt | Motif de rejet |
|---|---|
| `divorce-ddi` lien `/recherche/…` | lien = page de recherche |
| `autorite-parentale` n° `ARR.20210520.2F.1` | numéro ni rôle ni C.const |

## Critères d'acceptation
- [ ] `parseOne` rejette un arrêt dont le `lien` contient un marqueur de recherche.
- [ ] `parseOne` rejette un arrêt dont le `numero_pourvoi` n'est ni un n° de rôle belge ni un n° de Cour constitutionnelle.
- [ ] `parseOne` accepte les arrêts valides (rôle `S./C./P./F.YY.NNNN.F/N`, joint inclus, ou `NN/AAAA`).
- [ ] Rejet logué en WARN avec le motif + la query.
- [ ] Anti-régression : les tests existants `JurisprudenceBeWebSearchClientTest` restent verts (les fixtures valides passent).

## Plan de test
- **UT** `JurisprudenceBeWebSearchClientTest` (extension) :
  - rejet lien = page de recherche → arrêt exclu ;
  - rejet numéro = fragment ECLI / texte libre → arrêt exclu ;
  - acceptation rôle `S.10.0044.F`, rôle joint `C.18.0294.F-C.18.0611.F`, C.const `121/2013` ;
  - un JSON mêlant 1 valide + 1 hallucination → 1 seul arrêt retourné.

## Composants impactés
- `backend/.../JurisprudenceBeWebSearchClient.java` (helper de validation + appel dans `parseOne`).
- `backend/.../JurisprudenceBeWebSearchClientTest.java` (UT).
- **Aucune migration, aucun frontend.**

## Hors périmètre
- Vérification d'existence réelle de l'arrêt (HEAD HTTP du lien / 2ᵉ passe Claude) → SF-JU-04-05 si le re-test montre des faux positifs format-valides résiduels.
- Mode « validation humaine » des mappings BE (alternative non retenue ici — on filtre à la source).
- Scaling aux ~80 outils (opérationnel, après re-test du pilote validant le garde-fou).

## Préoccupations transversales
- **Outil décisionnel métier** : fiabilise une source de citations, aucun outil ajouté. Additif.
- Auth/workspace/plans/navigation : non concernés.
