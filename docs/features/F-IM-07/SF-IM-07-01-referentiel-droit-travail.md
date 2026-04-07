# Mini-spec — F-IM-07 / SF-IM-07-01 Référentiel droits au travail et moteur de résolution

---

## Identifiant

`F-IM-07 / SF-IM-07-01`

## Feature parente

`F-IM-07` — Analyse droit au travail du demandeur

## Statut

`draft`

## Date de création

2026-04-07

## Branche Git

`feat/SF-IM-07-01-referentiel-droit-travail`

---

## Objectif

Créer le référentiel statique des droits au travail par type de titre de séjour (France + Belgique) et un moteur de résolution qui détermine le droit au travail, les conditions et les obligations employeur. Persistance 1:1 par dossier.

---

## Comportement attendu

### Cas nominal

1. Le référentiel associe chaque type de titre/statut à :
   - `droitTravail` : `OUI`, `NON`, `CONDITIONNEL`
   - `conditions` : texte décrivant les restrictions (secteur, durée, autorisation préalable)
   - `obligationsEmployeur` : liste des obligations (vérification, déclaration, sanctions)
   - `baseJuridique` : textes de référence

2. Le moteur reçoit `titreType` + `country` et retourne le résultat structuré.

3. Persistance en table `immigration_work_rights` (1:1 avec case_file).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Titre inconnu | Message d'erreur | 400 |
| Pays non supporté | Message d'erreur | 400 |
| Dossier autre workspace | "Case file not found" | 404 |
| Domaine != DROIT_IMMIGRATION | Message d'erreur | 400 |

---

## Critères d'acceptation

- [ ] Au minimum 8 titres FR et 8 titres BE couverts (alignés sur ImmigrationTitleReferentiel)
- [ ] Les données sont configurées pour FRANCE et BELGIQUE
- [ ] Le moteur retourne le droit (OUI/NON/CONDITIONNEL), conditions et obligations pour chaque titre
- [ ] Persistance 1:1, upsert, isolation workspace
- [ ] Pattern existant suivi

---

## Technique

### Migration : `060-create-immigration-work-rights.xml`

| Colonne | Type |
|---------|------|
| `id` | UUID PK |
| `case_file_id` | UUID FK UNIQUE |
| `titre_type` | VARCHAR(50) NOT NULL |
| `country` | VARCHAR(20) NOT NULL |
| `result_data` | TEXT NOT NULL — JSON |
| `created_at` / `updated_at` | TIMESTAMP |

### Classes Java

- `WorkRightResult` — record (titreType, country, droitTravail, conditions, obligationsEmployeur, baseJuridique)
- `ImmigrationWorkRightReferentiel` — référentiel statique
- `ImmigrationWorkRight` — entity JPA
- `ImmigrationWorkRightRepository` — repository

---

## Plan de test

- [ ] Référentiel : min 8 titres FR + 8 titres BE
- [ ] Chaque titre a un droit (OUI/NON/CONDITIONNEL) non null
- [ ] Chaque titre a des obligations employeur non vides
- [ ] Titre inconnu → null
- [ ] Résultat structuré complet pour chaque titre

## Analyse d'impact

- [x] Aucune préoccupation transversale

## Dépendances

- Aucune
