# Mini-spec — F-214 / SF-214-09 — OQTF catégories L. 611-1 — backend

## Identifiant

`F-214 / SF-214-09`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-214-09-oqtf-categories-l6111-backend`

---

## Objectif

Déterminer la catégorie de l'OQTF parmi les 7 catégories L. 611-1 CESEDA (1° à 7°), car chaque catégorie ouvre des moyens de défense distincts que F-IM-08 (OQTF avec/sans délai) ne distingue pas.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/oqtf-categories-analysis`
- Body : `categorieL611` (enum `CAT_1` à `CAT_7`), `dateNotificationOqtf` (LocalDate, requis), `motifOqtf` (string ≤ 300)
- Analyzer `OqtfCategoriesAnalyzer` :
  - Identifie la catégorie (1° entrée irrégulière, 2° séjour expiré, 3° fraude au titre, 4° refus de titre, 5° retrait de titre, 6° menace ordre public, 7° OQTF prise dans le cadre d'une procédure Dublin)
  - Produit les `moyensDefense` spécifiques à la catégorie : liste de moyens (ex. CAT_6 : examen proportionnalité art. 8 CEDH ; CAT_1 : vérification régularité notification)
  - `baseJuridique` : référence précise L. 611-1 1° à 7°
  - `delaiRecours` : calcul selon type OQTF (avec délai 30 j L. 614-5 ; sans délai 48 h L. 614-1)
  - `procedureParallele` : si CAT_7 → renvoi vers F-IM-22 Dublin recours ; si CAT_6 → IRTF possible L. 612-6
- Output persisté dans `oqtf_categories_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/oqtf-categories-analysis` → 200 ou 404

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_IMMIGRATION | 400 |
| categorieL611 inconnue | 400 |
| dateNotificationOqtf future | 400 |
| motifOqtf > 300 chars | 400 |
| caseFile inaccessible | 404 |

---

## Source juridique

- **L. 611-1 1° à 7° CESEDA** — 7 catégories d'OQTF. Recodification 2021 (anciens L. 511-1 I 1° à 7°).
- **L. 614-1 CESEDA** — OQTF sans délai, recours 48 h.
- **L. 614-5 CESEDA** — OQTF avec délai 30 j, recours TA.
- **CE 5 décembre 2014, n° 373520** — proportionnalité OQTF catégorie 6 (menace ordre public).
- **CE 10 octobre 2013, n° 366528** — OQTF catégorie 1 (entrée irrégulière), vices de procédure.
- **Loi 26/01/2024** (Darmanin) : modifications délais recours et catégories.

**Relation avec F-IM-08** : F-IM-08 analyse les délais de recours et les motifs de contestation GÉNÉRIQUES d'une OQTF (avec/sans délai). F-IM-29 (cet outil) analyse la CATÉGORIE L. 611-1 et les moyens de défense SPÉCIFIQUES à chaque catégorie. Les deux outils sont complémentaires et non redondants.

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `categorieL611` | enum | `motifOqtfCode` (proxy) | Dériver + extension mapping (`oqtfCategorieL611`) |
| `dateNotificationOqtf` | date | `dateNotificationOqtf` | Déjà présent — réutiliser |
| `motifOqtf` | texte | `motifOqtfCode` | Réutiliser |

**Trigger CONTEXTUAL** : `typeProcedureDetectee` = OQTF_AVEC_DELAI ou OQTF_SANS_DELAI (existant F-IM-08). L'outil apparaît dans le même contexte que F-IM-08, visible côte à côte. Pas de nouveau flag IA requis.

---

## Critères d'acceptation

- [x] POST CAT_1 retourne moyensDefense liste (vérification notification, défaut de base légale)
- [x] POST CAT_6 retourne moyensDefense + procedureParallele IRTF
- [x] POST CAT_7 retourne procedureParallele DUBLIN + renvoi F-IM-22
- [x] POST dateNotificationOqtf future → 400
- [x] POST workspace BE → 400
- [x] GET sans POST → 404
- [x] POST upsert → remplacement
- [x] Isolation workspace
- [x] `F-IM-29-oqtf-categories-l6111-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed `decision_tool_visibility_rules` : CONTEXTUAL, trigger_field=`type_procedure_detectee`, trigger_value=`OQTF_AVEC_DELAI` (identique F-IM-08)

## Plan de test minimal

- **UT** `OqtfCategoriesAnalyzerTest` : 7+ cas (une par catégorie)
- **IT** `OqtfCategoriesControllerIT` : 6+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `oqtf_categories_analyses`
- **Migration Liquibase** + seed visibility rules
- **Endpoint** `OqtfCategoriesController`
- Pas d'extension `ImmigrationExtractedData` (mapping existant `motifOqtfCode` + `dateNotificationOqtf` suffisants pour V1)

## Hors périmètre

- Composant Angular (SF-214-10)
- Génération de requête TA (F-IM-06 existant)
