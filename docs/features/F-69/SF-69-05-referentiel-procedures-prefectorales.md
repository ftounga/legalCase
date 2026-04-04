# Mini-spec — F-69 / SF-69-05 Référentiel procédures préfectorales

## Identifiant

`F-69 / SF-69-05`

## Feature parente

`F-69` — Suivi des délais légaux (extension DROIT_IMMIGRATION / F-IM-03)

## Statut

`in-progress`

## Date de création

2026-04-04

## Branche Git

`feat/SF-69-05-referentiel-procedures-prefectorales`

---

## Objectif

Pour les dossiers DROIT_IMMIGRATION, l'IA détecte le type de procédure préfectorale en cours (OFPRA, CNDA, renouvellement de titre) et génère automatiquement les jalons procéduraux standards sous forme de `CaseDeadline`, calculés depuis la date de dépôt du dossier.

---

## Comportement attendu

### Cas nominal

1. L'analyse IA d'un dossier DROIT_IMMIGRATION termine (DONE).
2. `StatutoryDeadlineService.createStatutoryDeadlines()` est appelé avec le JSON enrichi.
3. Le JSON contient `type_procedure_detectee` (ex: `"RENOUVELLEMENT_TITRE_SEJOUR"`) et `date_depot_procedure` (ex: `"2026-01-15"`).
4. `ImmigrationProcedureReferentiel` (map statique Java) résout le type → liste de jalons `{label, offsetDays}`.
5. Pour chaque jalon : une `CaseDeadline` est créée avec `dueDate = date_depot + offsetDays`, `source = "STATUTORY"`, `documentType = type_procedure_detectee`.
6. Si une `CaseDeadline` STATUTORY avec le même label existe déjà pour ce dossier → suppression avant insertion (upsert, même pattern que SF-69-04).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `type_procedure_detectee` absent ou null | Aucune deadline créée, log debug, fail-open |
| `date_depot_procedure` absente ou invalide | Aucune deadline créée, log debug, fail-open |
| Type de procédure inconnu dans le référentiel | Aucune deadline créée, log debug ("type inconnu — skipped") |
| Exception inattendue | Log warn, swallowed (fail-open existant) |
| Dossier non DROIT_IMMIGRATION | Non appelé (dispatch dans `createStatutoryDeadlines`) |

---

## Critères d'acceptation

- [ ] Pour `RENOUVELLEMENT_TITRE_SEJOUR` : 2 jalons créés (délai instruction 4 mois, silence vaut rejet 2 mois)
- [ ] Pour `DEMANDE_ASILE_OFPRA` : 2 jalons créés (convocation OFPRA 21 jours, décision OFPRA 6 mois)
- [ ] Pour `RECOURS_CNDA` : 2 jalons créés (audience CNDA 5 mois, décision CNDA 9 mois)
- [ ] Type inconnu dans le référentiel → aucune deadline créée, pas d'exception
- [ ] `date_depot_procedure` invalide → aucune deadline créée, pas d'exception
- [ ] Upsert : deadline STATUTORY avec même label supprimée avant ré-insertion
- [ ] Scoping : uniquement déclenché pour `legalDomain = "DROIT_IMMIGRATION"`
- [ ] Les délais `date_expiration_titre` (SF-69-04) restent inchangés

---

## Périmètre

### Hors scope

- UI dédiée (les deadlines sont affichées via `CaseDeadlinesSectionComponent` existant)
- Modification du référentiel par l'admin (map statique Java)
- Support de la Belgique (France uniquement en V1 immigration)
- Alertes multi-seuils sur ces jalons (pas d'`alertThresholds` — délais procéduraux, pas d'expiration)
- Procédures hors OFPRA/CNDA/renouvellement (ex : naturalisation, regroupement familial) — V5

---

## Technique

### Endpoints

Aucun nouveau endpoint. Déclenché via `StatutoryDeadlineService.createStatutoryDeadlines()` appelé depuis le pipeline IA existant.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| case_deadlines | INSERT + DELETE (upsert) | Colonnes existantes — aucune migration requise |

### Migration Liquibase

- [x] Non applicable — toutes les colonnes nécessaires existent (migration 046)

### Composants Angular

Aucun — les deadlines s'affichent via `CaseDeadlinesSectionComponent` (SF-69-02).

### Nouveau composant Java

`ImmigrationProcedureReferentiel` — classe utilitaire (méthode statique) : `List<ProcedureJalon> resolve(String typeProcedure)` où `ProcedureJalon` = record `{String label, int offsetDays}`.

Extension de `StatutoryDeadlineService.createImmigrationTitreDeadline()` → ajout d'une méthode `createImmigrationProcedureDeadlines()` appelée en parallèle dans la branche DROIT_IMMIGRATION.

Extension du prompt DROIT_IMMIGRATION dans `LegalDomainPromptBuilder` ou `CaseAnalysisService` : ajouter l'extraction de `type_procedure_detectee` + `date_depot_procedure`.

---

## Plan de test

### Tests unitaires

- [ ] `ImmigrationProcedureReferentielTest` — `RENOUVELLEMENT_TITRE_SEJOUR` → 2 jalons avec labels et offsets corrects
- [ ] `ImmigrationProcedureReferentielTest` — `DEMANDE_ASILE_OFPRA` → 2 jalons
- [ ] `ImmigrationProcedureReferentielTest` — `RECOURS_CNDA` → 2 jalons
- [ ] `ImmigrationProcedureReferentielTest` — type inconnu → liste vide
- [ ] `StatutoryDeadlineServiceTest` — DROIT_IMMIGRATION + type connu + date valide → N deadlines créées avec bons labels et dates
- [ ] `StatutoryDeadlineServiceTest` — DROIT_IMMIGRATION + type inconnu → aucune deadline créée
- [ ] `StatutoryDeadlineServiceTest` — DROIT_IMMIGRATION + date_depot absente → aucune deadline créée
- [ ] `StatutoryDeadlineServiceTest` — DROIT_IMMIGRATION + date_depot invalide → aucune deadline, pas d'exception
- [ ] `StatutoryDeadlineServiceTest` — DROIT_IMMIGRATION + les deux paths (expiration titre + procédure) coexistent

### Tests d'intégration

Non applicable — scheduler interne, aucun endpoint exposé.

### Isolation workspace

Non applicable — aucun accès direct aux données du workspace dans ce composant.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — extension isolée de `StatutoryDeadlineService`, branche DROIT_IMMIGRATION uniquement

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — backend only, pas de route ni de guard modifié

---

## Dépendances

### Subfeatures bloquantes

- SF-69-04 — statut : done (alert_thresholds, documentType, dispatch legalDomain en place)

### Questions ouvertes

- Aucune

---

## Notes et décisions

- Le référentiel est une map Java statique (`Map<String, List<ProcedureJalon>>`), pas de table DB. Évolutivité future via fichier YAML si nécessaire.
- Les jalons procéduraux n'ont pas d'`alertThresholds` : ce sont des échéances réglementaires connues d'avance, pas des expirations à surveiller.
- Les deux paths DROIT_IMMIGRATION (expiration titre SF-69-04 + procédure SF-69-05) sont indépendants et coexistent dans `createStatutoryDeadlines()`.
- Procédures couvertes en V1 : `RENOUVELLEMENT_TITRE_SEJOUR`, `DEMANDE_ASILE_OFPRA`, `RECOURS_CNDA`.
