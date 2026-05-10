# Mini-spec — F-234 / SF-234-02 — Supprimer les constructeurs rétrocompat + garde-fou intégrité

## Identifiant

`F-234 / SF-234-02`

## Feature parente

`F-234` — Refactor records ExtractedData en Builder pattern

## Statut

`in-progress`

## Date de création

2026-05-10

## Branche Git

`feat/F-234-builder-extracted-data` (mutualisée avec SF-234-01 — 1 PR groupée).

---

## Objectif

Supprimer les 9 constructeurs rétrocompat des 3 records `*ExtractedData` (4 sur Travail, 0 sur Famille, 5 sur Immigration), migrer les 4 call-sites de tests qui les invoquaient explicitement vers le builder, et ajouter un garde-fou (`BuilderPatternEnforcementIT`) qui empêche leur réapparition silencieuse.

---

## Comportement attendu

### Cas nominal

- Les 4 constructeurs rétrocompat de `TravailExtractedData` sont supprimés (9-args, 17-args, 18-args, 23-args, 36-args).
- Les 4 constructeurs rétrocompat d'`ImmigrationExtractedData` sont supprimés (4-args, 6-args, 8-args, 9-args, 14-args).
- `FamilleExtractedData` n'a aucun constructeur rétrocompat à supprimer (un seul constructeur canonique généré par le record).
- Seul le constructeur canonique généré par chaque record subsiste.
- Les tests qui invoquaient explicitement un constructeur rétrocompat (`CaseAnalysisResponseTest` lignes 1641, 1658, 1790, 1817) sont migrés pour utiliser le builder. La sémantique des assertions est strictement préservée.
- Un nouveau test d'intégrité `BuilderPatternEnforcementIT` lit le source de `CaseAnalysisResponse.java` et compte le nombre de constructeurs publics par record. Il échoue si > 1 constructeur est trouvé.

### Cas d'erreur

- Si une PR future réintroduit un constructeur rétrocompat, le garde-fou échoue avec un message explicite : « Pas de constructeur rétrocompat — utiliser Builder pattern (F-234) ».

---

## Analyse de cohérence transversale

- Pas de pattern UI/service partagé introduit.
- Le garde-fou suit le même principe que les autres tests d'intégrité du projet (`DecisionToolVisibilityIntegrityIT`, `LegalReferentialDescriptionIntegrityIT`).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Autres records `*ExtractedData` susceptibles d'être ajoutés à l'avenir | Oui | Le test parcourt tout `record *ExtractedData` détecté dans le source — couverture automatique des futurs records sans modification du test. |
| Autres records IA hors `*ExtractedData` | Non | Pas applicable — pas d'autre record exposant > 4 args dans le projet. Pattern réservé aux `*ExtractedData`. |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature.

---

## Conformité F-IA-04

- [x] **Non applicable** — justification : SF backend pure (refactor + test d'intégrité), aucun composant frontend décisionnel.

---

## Critères d'acceptation

- [x] `TravailExtractedData` n'a plus que son constructeur canonique (généré par le record).
- [x] `ImmigrationExtractedData` n'a plus que son constructeur canonique.
- [x] `FamilleExtractedData` reste avec son constructeur canonique unique.
- [x] Les 4 tests `CaseAnalysisResponseTest` qui invoquaient un constructeur rétrocompat sont migrés vers le builder, sémantique préservée.
- [x] `BuilderPatternEnforcementIT` existe et passe (un seul constructeur public par record).
- [x] Le test `BuilderPatternEnforcementIT` échoue si on rajoute un constructeur public manuel (test de non-régression validé localement par un essai conscient).
- [x] `./mvnw test -Dtest='CaseAnalysisResponseTest,LegalDomainPromptBuilderTest,BuilderPatternEnforcementIT'` vert.
- [x] Build complet `./mvnw clean package -DskipTests` vert.

---

## Périmètre

### Hors scope (explicite)

- Refactor des autres records non `*ExtractedData` (ex. `LiquidationCommunauteResult`, `DivorceConsentementScoring`) — leur signature reste inférieure au seuil de douleur.

---

## Impact par domaine métier

Transversal — infrastructure IA (parsing JSON IA en records Java). Aucune adaptation par domaine. Le refactor préserve à 100% le comportement runtime des 3 parsers et des 4 sites de test migrés.

---

## Technique

### Composants Java impactés

| Fichier | Opération |
|---------|-----------|
| `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisResponse.java` | Suppression des constructeurs rétrocompat |
| `backend/src/test/java/fr/ailegalcase/analysis/CaseAnalysisResponseTest.java` | Migration des 4 call-sites legacy vers `.builder()` |
| `backend/src/test/java/fr/ailegalcase/analysis/BuilderPatternEnforcementIT.java` | **Nouveau** — test d'intégrité regex |

### Migration Liquibase

- [x] Non applicable.

---

## Plan de test

### Tests unitaires

- [x] Tests existants `CaseAnalysisResponseTest` passent inchangés à l'exception des 4 sites migrés (sémantique préservée).
- [x] Nouveau test `BuilderPatternEnforcementIT` : compte les constructeurs `public *ExtractedData(...)` via regex et assert <= 1 par record.

### Isolation workspace

- [x] Non applicable.

---

## Analyse d'impact

- [x] **Aucune préoccupation transversale** — subfeature isolée.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné.

---

## Dépendances

### Subfeatures bloquantes

- SF-234-01 doit être intégrée dans la même PR (les call-sites migrés dépendent de l'existence des builders).

---

## Notes et décisions

- Le test d'intégrité ne se base **pas** sur la réflexion (qui ne distingue pas un constructeur déclaré d'un constructeur canonique généré). Il lit le source au format texte via `Files.readString()` et compte les motifs `public TravailExtractedData(`, etc.
- Le test ignore le constructeur "canonique compact" record-style (`public TravailExtractedData {` sans parenthèses ouvertes immédiatement après le nom — récap : la regex matche un `(` après le nom).
- Le test décompte les paramètres pour s'assurer que le constructeur canonique compact ou full est bien le seul (le record canonique full constructor a la même signature que le compact, ils comptent comme 1 logique).
