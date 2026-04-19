# Mini-spec — F-128 / SF-128-01 Robustesse classification IA — immigration + famille

## Identifiant
`F-128 / SF-128-01`

## Feature parente
`F-128` — Robustesse classification IA cross-domain

## Statut `draft`  · Date `2026-04-20`  · Branche `feat/SF-128-01-classification-robustness-im-fa`

---

## Objectif

Généraliser aux domaines **immigration** et **famille** les fixes de robustesse de classification appliqués au droit du travail (PR #398-401) :
1. Règle critique en tête du prompt : distinguer **mécanisme factuel** (ce qui s'est passé, attesté par une pièce) vs **qualification demandée** (argumentation juridique).
2. Préservation de la baseline dans le prompt enrichi : les champs de classification factuels ne changent pas sur la base des Q&A / chat / checks — uniquement si un nouveau document signé/notifié contredit.

---

## Comportement

### Domaines & champs couverts

**Immigration** (`IMMIGRATION_INSTRUCTION`) :
- `type_titre_sejour_code` (16 codes)
- `type_procedure_detectee` (RENOUVELLEMENT_TITRE_SEJOUR / DEMANDE_ASILE_OFPRA / RECOURS_CNDA)
- `type_recours_code` (6 codes)
- `nationalite_ue`

**Famille** (`FAMILLE_INSTRUCTION`) :
- `mode_garde_detaille` (6 codes ALTERNEE_FR, DVH_*, ALTERNEE_BE, etc.)
- `regime_matrimonial` (COMMUNAUTE_LEGALE / SEPARATION_BIENS / PARTICIPATION_ACQUETS)
- `pays_applicable`

### Règle critique en tête (nouvelle)

Ajouter un bloc encadré visuellement au début de `IMMIGRATION_INSTRUCTION` et `FAMILLE_INSTRUCTION`, même structure que celui introduit par PR #399 dans `TRAVAIL_INSTRUCTION` :

```
========== RÈGLE CRITIQUE DE CLASSIFICATION — À APPLIQUER EN PREMIER ==========
Identifier le MÉCANISME FACTUEL (pièce signée / décision notifiée),
JAMAIS la qualification demandée dans les arguments d'une partie.

Exemples critiques :
- [IM] Refus OFPRA reçu + arguments pour recours CNDA : type_procedure = 
  DEMANDE_ASILE_OFPRA (état actuel), pas RECOURS_CNDA tant que le recours
  n'est pas formellement déposé.
- [FA] Régime conventionnel de séparation signé + arguments pour 
  requalifier en communauté de fait : regime_matrimonial = SEPARATION_BIENS
  (la convention existe factuellement).
================================================================================
```

### Préservation baseline enrichie (généralisation PR #401)

Ajouter au `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE` une règle qui s'applique à tous les champs de classification factuels (pas uniquement `type_rupture`) :

> "RÈGLE CRITIQUE DE PRÉSERVATION (enrichie) : les champs de classification factuels extraits par la synthèse précédente (`type_rupture`, `type_titre_sejour_code`, `type_procedure_detectee`, `type_recours_code`, `nationalite_ue`, `mode_garde_detaille`, `regime_matrimonial`, `pays_applicable`) sont la BASELINE à préserver. Tu ne les changes QUE si un nouveau document signé/notifié dans le dossier contredit la classification précédente. Les réponses Q&A de l'avocat, le chat et les checks procéduraux N'AUTORISENT PAS à retourner ces classifications."

### Cas d'erreur

- Domaine non reconnu → pas d'injection (comportement actuel préservé)
- Valeur baseline nulle → comportement nominal (Claude peut choisir librement)

---

## Critères d'acceptation

- [ ] `IMMIGRATION_INSTRUCTION` commence par la règle critique encadrée, avec exemple immigration concret
- [ ] `FAMILLE_INSTRUCTION` commence par la règle critique encadrée, avec exemple famille concret
- [ ] `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE` contient la règle de préservation généralisée listant les 8 champs
- [ ] Règle `type_rupture` précédemment spécifique (PR #401) est fusionnée/élargie — pas de doublon
- [ ] Aucune régression sur les tests existants (14 tests `LegalDomainPromptBuilder/CaseAnalysis` + 20 tests `EnrichedAnalysis`)
- [ ] Les exemples cités dans la règle critique sont juridiquement corrects (pour éviter de fausser Claude)

---

## Plan de test

### Unitaires backend
- `LegalDomainPromptBuilderTest` : 2 nouveaux tests
  - `domainSpecificInstruction("DROIT_IMMIGRATION")` contient "RÈGLE CRITIQUE DE CLASSIFICATION"
  - `domainSpecificInstruction("DROIT_FAMILLE")` contient "RÈGLE CRITIQUE DE CLASSIFICATION"
- `EnrichedAnalysisServiceTest` : 1 nouveau test
  - `buildSystemPrompt` contient "RÈGLE CRITIQUE DE PRÉSERVATION" avec les 8 noms de champs

### Intégration manuelle staging
- Dossier immigration type "refus titre + arguments recours" → vérifier que `type_procedure_detectee` reste RENOUVELLEMENT, pas RECOURS
- Dossier famille avec contrat séparation contesté → vérifier que `regime_matrimonial` reste SEPARATION_BIENS

### Isolation workspace
- Non applicable (modification prompt uniquement)

---

## Tables / endpoints / composants impactés

### Backend
- `LegalDomainPromptBuilder.java` — IMMIGRATION_INSTRUCTION + FAMILLE_INSTRUCTION : règle critique en tête
- `EnrichedAnalysisService.java` — SYSTEM_PROMPT_TEMPLATE : généralisation règle préservation baseline
- `LegalDomainPromptBuilderTest.java` + `EnrichedAnalysisServiceTest.java` — 3 tests ajoutés

### Frontend / DB
- Aucun changement

---

## Hors périmètre

- Modification du prompt chunk-level (`ChunkAnalysisService`) ou document-level (`DocumentAnalysisService`) — ces niveaux ne font pas de classification
- Ajout de nouveaux champs de classification — seulement généralisation des existants
- Fix du pattern dans `LegalDomainPromptBuilder` pour mutualiser les règles critiques — refactor prématuré

---

## Analyse de cohérence transversale

| Cible | Applicable | Classement |
|---|---|---|
| Autres pays (Belgique) | Oui | **Intégrée** — règles et champs couvrent FR + BE dans les 2 domaines |
| Autres domaines (travail) | Non applicable | Déjà fait par PR #398-401 (cette SF est justement la généralisation) |
| Autres outils | Non applicable | La règle s'applique au niveau analyse IA, pas à des outils spécifiques |

**Analyse d'impact cross-cutting** :
- [ ] Auth / Principal — non touché
- [ ] Workspace context — non touché
- [x] **Plans / limites** — non touché (coût prompt marginal : +50 tokens par analyse)
- [ ] Navigation / routing — non touché

Aucun smoke E2E concerné.

---

## Nouveau pattern UI ou service partagé

- [x] Pas de nouveau pattern — extension homogène du pattern existant sur les 3 domaines
- [x] Pas de service partagé extrait — refactor mutualisation prématuré, les 3 domaines ont des exemples juridiquement distincts qui méritent d'être explicites
