# Mini-spec — F-133 / SF-133-01 Extraction de RecoursGenerator en 3 générateurs dédiés

## Identifiant
`F-133 / SF-133-01`

## Feature parente
`F-133` — Refonte F-IM-06 : générateurs de recours décisionnels dédiés

## Statut `draft`  · Date `2026-04-20`  · Branche `feat/SF-133-01-recours-generators-split`

---

## Objectif

Extraire les 6 types de recours fondamentalement distincts du monolithe `RecoursGenerator` en 3 générateurs spécialisés (France administratif, France CNDA asile, Belgique), pour appliquer l'invariant "un outil décisionnel = une situation métier" (pattern F-DT-08/F-DT-10, réplication de F-132). `RecoursGenerator` devient un **routeur** minimal qui délègue — la signature publique reste inchangée pour ne casser ni les consumers ni le frontend.

---

## Comportement attendu

### Cas nominal

`RecoursGenerator.generate(recoursTypeCode, ...)` est inchangé côté signature :

- `RECOURS_GRACIEUX_PREFET`, `RECOURS_CONTENTIEUX_TA` → délégué à `RecoursGeneratorFrance`
- `RECOURS_CNDA` → délégué à `RecoursGeneratorCnda`
- `RECOURS_CGRA`, `RECOURS_CCE`, `RECOURS_CE_BELGIQUE` → délégué à `RecoursGeneratorBelgique`

Chaque classe spécialisée porte **sa propre logique d'entête, moyens de droit et conclusions**, sans switch interne sur le type/pays. Les méthodes communes (objet, visa des textes) restent dans un helper `RecoursGeneratorCommon` (package-private).

### Cas d'erreur

| Situation | Comportement |
|---|---|
| Code inconnu | `IllegalArgumentException` : "Type de recours inconnu : {code}" — inchangé |
| `type == null` retourné par le référentiel | `IllegalArgumentException` — inchangé |
| Date notification `null` | NPE (comportement actuel conservé — pas dans le scope de cette SF) |

---

## Analyse de cohérence transversale

### Périmètres scannés

| Cible | Applicable ? | Traitement |
|---|---|---|
| Autres outils décisionnels monolithiques | Scan F-132 fait : F-132 déjà terminée (F-DT-09), aucun autre outil problématique. F-IM-05 (titre séjour) est un cas limite acceptable | N/A |
| Autres pays / domaines | Cette SF couvre FR + BE pour l'immigration. Pas d'impact sur droit du travail / famille. | N/A |
| Cohérence IA (F-IA-03) | Non — recours est un générateur de document, pas un outil à champs saisis librement croisables | N/A |
| Refresh dashboard (F-IA-02) | Non — la card dashboard recours utilise `ImmigrationRecoursAnalysis` entity, inchangée | N/A |
| Pré-remplissage IA | Non — pas de champs de formulaire impactés, le pré-remplissage existant continue | N/A |
| Masquage conditionnel | Non — un seul composant frontend pour tous les types, chaque avocat choisit le type via le dropdown | N/A |
| Nouveau pattern partagé | Non — classes backend isolées au package `casefile` | N/A |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette SF (les 6 types couverts)
- [x] Non applicable aux autres outils (scan F-132 déjà fait)

---

## Critères d'acceptation

### Backend

- [ ] Nouvelle classe `RecoursGeneratorFrance` gérant `RECOURS_GRACIEUX_PREFET` + `RECOURS_CONTENTIEUX_TA` (entête FR + moyens spécifiques + conclusions FR)
- [ ] Nouvelle classe `RecoursGeneratorCnda` gérant `RECOURS_CNDA` (entête FR + moyens asile + conclusions FR — variante distincte car juridiction différente)
- [ ] Nouvelle classe `RecoursGeneratorBelgique` gérant `RECOURS_CGRA` + `RECOURS_CCE` + `RECOURS_CE_BELGIQUE` (entête BE + moyens selon le code + conclusions BE)
- [ ] `RecoursGeneratorCommon` (package-private) porte les helpers partagés : `buildObjet`, `buildVisaTextes`, calcul `dateLimite` + `avertissement`
- [ ] `RecoursGenerator.generate(...)` devient un dispatcher minimal : lookup du référentiel + switch sur `type.code()` → délégation à la bonne classe
- [ ] **Zéro** switch conditionnel sur `type.country()` ou `type.code()` à l'intérieur d'une classe spécialisée (test lecture code)
- [ ] Tous les 10 tests existants `RecoursGeneratorTest` restent verts sans modification (la signature publique est strictement préservée)
- [ ] Nouveau test : `RecoursGeneratorTest.unknownCode_throws` confirme le comportement dispatcher (déjà présent, à préserver)
- [ ] `ImmigrationRecoursService` et `ImmigrationRecoursController` **inchangés**
- [ ] Tous les tests backend restent verts (981 → 981)

### Cohérence

- [ ] Aucune régression de rendu : le document généré pour chacun des 6 codes est **identique caractère pour caractère** à l'ancien. Les tests existants couvrent ce point.

---

## Périmètre

### Hors scope

- Ajout de nouveaux types de recours
- Modification des textes juridiques (visa, moyens, conclusions)
- Frontend (pas d'impact — le composant Angular ne voit que `GeneratedRecours` en sortie, inchangé)
- Séparation de l'entity `ImmigrationRecoursAnalysis` ou de son endpoint (pas pertinent : l'entity persistée contient juste le document généré + métadonnées, pas de différenciation selon le type)

---

## Technique

### Endpoints

Aucun changement (`ImmigrationRecoursController` inchangé).

### Tables impactées

Aucune.

### Migration Liquibase

- [ ] Aucune

### Fichiers backend impactés

- **Nouveaux** : `RecoursGeneratorFrance.java`, `RecoursGeneratorCnda.java`, `RecoursGeneratorBelgique.java`, `RecoursGeneratorCommon.java`
- **Modifiés** : `RecoursGenerator.java` (devient routeur minimal)
- **Inchangés** : `ImmigrationRecoursService`, `ImmigrationRecoursController`, `RecoursType`, `GeneratedRecours`, `ImmigrationRecoursReferentiel`, tests existants

---

## Plan de test

### Tests unitaires

- `RecoursGeneratorTest` (10 tests existants) : inchangés, doivent rester verts
  - Si utile, ajouter 2 tests ciblant explicitement le dispatch (sanity) :
    - Test que `RECOURS_CNDA` est bien traité par `RecoursGeneratorCnda` (via assertion de contenu spécifique asile)
    - Test que `RECOURS_CGRA` est bien traité par `RecoursGeneratorBelgique`

### Tests IT

- `ImmigrationRecoursControllerIT` existant : doit rester vert

### Isolation workspace

- N/A (inchangé)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal : non
- [ ] Workspace context : non
- [ ] Plans / limites : non
- [ ] Navigation / routing : non (backend pur)
- [x] **Outil décisionnel métier** : scan effectué, F-132 terminée, seul F-IM-06 reste, cette SF le traite

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| `RecoursGenerator` | Signature publique préservée, implémentation déléguée | Tests existants verts |
| `ImmigrationRecoursService` | Aucun — appelle toujours `RecoursGenerator.generate(...)` | Tests existants verts |
| Frontend `immigration-recours-section` | Aucun — consomme toujours le même format `GeneratedRecours` | Tests existants verts |

### Smoke tests E2E

- Aucun — backend pur, pas de flow UI touché

---

## Dépendances

### Subfeatures bloquantes

- F-132 terminée ✅ (pattern de référence : routeur + classes spécialisées)

### Questions ouvertes

- Aucune

---

## Notes et décisions

- **Pourquoi un routeur `RecoursGenerator` au lieu de faire switcher `ImmigrationRecoursService` directement** : préserver la signature publique évite un diff inutile côté service et garde les tests existants intacts. Le routeur n'a pas de logique métier — juste un lookup + délégation. C'est acceptable au regard de l'invariant "un outil = une situation métier" car chaque **classe métier** (France/Cnda/Belgique) est homogène. Le routeur est une infrastructure, pas un outil.
- **Pourquoi 3 classes et pas 6** : les 6 types partagent du code significatif par regroupement géographique/juridictionnel (entête FR commun aux 2 types administratifs FR, entête BE commun aux 3 types BE, CNDA a son propre format). 6 classes seraient de la fragmentation excessive. 3 classes respectent le principe "une situation métier = un outil" : recours administratif FR (préfet ou TA = même logique), recours asile FR CNDA, recours BE (CGRA/CCE/CE partagent cadre BE avec moyens spécifiques par code).
- **Pourquoi `RecoursGeneratorCommon` package-private** : les helpers `buildObjet`/`buildVisaTextes`/calcul date limite sont génériques, pas de logique divergente selon le type. Les dupliquer dans chaque classe serait du copier-coller.
