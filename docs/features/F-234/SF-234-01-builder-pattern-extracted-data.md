# Mini-spec — F-234 / SF-234-01 — Introduire un Builder pattern par record `*ExtractedData`

## Identifiant

`F-234 / SF-234-01`

## Feature parente

`F-234` — Refactor records ExtractedData en Builder pattern

## Statut

`in-progress`

## Date de création

2026-05-10

## Branche Git

`feat/F-234-builder-extracted-data` (mutualisée pour SF-234-01 + SF-234-02 — dette technique pure, 1 PR groupée).

---

## Objectif

Introduire un Builder pattern par record (`TravailExtractedData`, `ImmigrationExtractedData`, `FamilleExtractedData`) afin de permettre la construction d'instances avec des champs nommés et des valeurs par défaut sûres, sans casser la compilation existante (les constructeurs rétrocompat restent en place dans cette SF — leur suppression vient en SF-234-02).

---

## Comportement attendu

### Cas nominal

- Pour chaque record `*ExtractedData`, une classe interne `Builder` est ajoutée :
  - `static Builder builder()` retourne une nouvelle instance.
  - Setters fluents par champ (`builder.conventionCollective("SYNTEC").salaireBrutMensuel(3200.0)`).
  - `build()` construit l'instance via le constructeur canonique du record.
- Valeurs par défaut :
  - `null` pour Object/String/Integer/Double/Boolean.
  - `false` pour `boolean` primitif.
- Tous les constructeurs rétrocompat existants restent inchangés (compilation garantie).
- Les 3 parsers `extractTravailData`, `extractImmigrationData`, `extractFamilleData` sont migrés vers le builder pour remplacer leurs `new ...(arg, arg, ...)` longs par un `builder.X(...).Y(...).build()` lisible.
- Le call-site de reconstruction `ImmigrationExtractedData` dans `from()` (lignes 768-806) est également migré vers le builder.

### Cas d'erreur

Aucun nouveau cas d'erreur introduit — refactor pur.

---

## Analyse de cohérence transversale

- Refactor isolé à `CaseAnalysisResponse.java` (1 fichier de production, 1 fichier de tests).
- Aucune autre classe Java ne construit ces records (vérifié via `grep -rn "new TravailExtractedData\|new ImmigrationExtractedData\|new FamilleExtractedData" backend/src/`).
- Le frontend ne construit jamais ces objets (interfaces TypeScript déclarées mais pas instanciées dans le code Java).
- Pas de pattern UI/service partagé introduit — pure refactor backend.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Autres records IA backend | Non | Aucun autre record IA n'a > 4 args dans le projet. Pattern réservé aux `*ExtractedData` post-F-234. |
| Frontend | Non | Le frontend ne construit jamais ces objets — il les consomme uniquement via JSON API. |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (les 3 records).
- [x] Non applicable aux autres cibles (frontend ne construit pas ces records).

---

## Conformité F-IA-04

- [x] **Non applicable** — justification : SF backend pure (refactor), aucun composant frontend décisionnel ni endpoint exposé.

---

## Critères d'acceptation

- [x] `TravailExtractedData.Builder`, `ImmigrationExtractedData.Builder`, `FamilleExtractedData.Builder` existent et exposent `static builder()` + `build()`.
- [x] Chaque builder expose un setter fluent par champ du record (nommage camelCase Java).
- [x] Les 3 parsers (`extractTravailData`, `extractImmigrationData`, `extractFamilleData`) utilisent le builder en lieu et place des `new ...(...)` longs.
- [x] Le call-site `from()` qui reconstruit `ImmigrationExtractedData` après inférence checklist (lignes ~768) utilise le builder avec un setter de copie.
- [x] Les constructeurs rétrocompat sont **conservés** (suppression différée en SF-234-02).
- [x] Tous les tests existants passent sans modification : `./mvnw test -Dtest='CaseAnalysisResponseTest,LegalDomainPromptBuilderTest'` vert.
- [x] Build complet `./mvnw clean package -DskipTests` vert.

---

## Périmètre

### Hors scope (explicite)

- Suppression des constructeurs rétrocompat → SF-234-02.
- Garde-fou regex contre la réintroduction d'un constructeur rétrocompat → SF-234-02.

---

## Impact par domaine métier

Transversal — infrastructure IA (parsing JSON IA en records Java). Aucune adaptation par domaine. Le refactor préserve à 100% le comportement runtime des 3 parsers.

---

## Technique

### Composants Java impactés

| Fichier | Opération |
|---------|-----------|
| `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisResponse.java` | Ajout de 3 classes Builder internes + migration des 4 call-sites (3 parsers + 1 reconstruction) |

### Migration Liquibase

- [x] Non applicable.

---

## Plan de test

### Tests unitaires

- [x] Tous les tests existants `CaseAnalysisResponseTest` passent inchangés (couvre les 3 parsers + les constructeurs rétrocompat encore présents).
- [x] Tests `LegalDomainPromptBuilderTest` passent inchangés (consomme la sortie des records).

### Tests d'intégration

- [x] Build complet vert.

### Isolation workspace

- [x] Non applicable — refactor sans accès données.

---

## Analyse d'impact

- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité à son périmètre.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné (refactor backend pur, JSON parser interne, aucune route HTTP modifiée).

---

## Dépendances

### Subfeatures bloquantes

- Aucune.

### Questions ouvertes impactées

- Aucune.

---

## Notes et décisions

- Pattern simple sans Lombok (cohérent avec l'absence de Lombok dans `CaseAnalysisResponse`).
- Setter fluent : retourne `Builder` (pas `this` typé) pour lisibilité.
- Le builder ne fait **pas** de validation (le record canonique reste responsable des invariants).
- Méthode utilitaire `Builder from(record)` pour copier un record existant et n'ajuster qu'un sous-ensemble de champs (utilisée notamment par `from()` après inférence checklist immigration).
