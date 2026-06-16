# Mini-spec — F-96 / SF-96-07 — Bannir les qualifications de fond et constats de cadrage de `points_procedure`

> Étape 1. Prolongement de SF-96-06 (qui bannit déjà options/recommandations/opportunités). Backend pur (prompt), aucun écran, aucune migration. Étape 0 couverte par F-96 + SF-96-06 (même flux, même garde-fou anti-fourre-tout).

## Identifiant

`F-96 / SF-96-07`

## Feature parente

`F-96` — Checklist procédurale interactive

## Statut

`draft`

## Date de création

2026-06-16

## Branche Git

`feat/SF-96-07-bannir-fond-checklist`

---

## Objectif

Durcir le prompt produisant `points_procedure` pour bannir, quand `critere_code = null`, **deux nouvelles catégories** d'intrus observées au test (dossier Dupont-3) : (1) les **qualifications / appréciations juridiques de fond** et (2) les **constats de cadrage / données extraites** — et les rediriger vers `risques` (si aléa/argument) ou la synthèse (si simple donnée). La checklist procédurale ne doit contenir que des **vérifications de formalisme et de délais** dont les 3 statuts ✅/❌/⚠️ ont un sens.

---

## Comportement attendu

### Cas nominal

Dans `CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` **et** `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE`, dans le bloc de règles `points_procedure` (à la suite des 3 catégories déjà interdites par SF-96-06), ajouter :

**Catégorie interdite n°4 — Qualifications / appréciations de fond** (quand `critere_code = null`) :
tout libellé qui **qualifie ou apprécie le motif au fond** plutôt que de vérifier une étape :
- « faute grave / faute lourde caractérisée (ou non) », « intention de nuire (non) caractérisée »
- « cause réelle et sérieuse », « motif réel et sérieux contestable », « licenciement justifié / abusif »
→ **rediriger vers `risques`** (c'est un aléa / un argument), jamais dans `points_procedure`.

**Catégorie interdite n°5 — Constats de cadrage / données extraites** (quand `critere_code = null`) :
tout libellé qui **constate une donnée du dossier sans vérification de conformité** :
- « Type de rupture identifié : … », « Qualification retenue par l'employeur : … »
- « Date de l'entretien identifiée dans les pièces : … », « X identifié dans les pièces »
→ ces données relèvent de la **synthèse / des métadonnées** du dossier (ex. `type_rupture`), pas de la checklist. Ne pas les produire dans `points_procedure`.

**Test d'inclusion explicite (à formuler dans le prompt)** :
> Un point n'entre dans `points_procedure` que si les trois statuts **Vérifié / Non conforme / À vérifier** ont un sens concret — c'est-à-dire **uniquement** une **étape de formalisme** (convocation, entretien, notification, lettre motivée) ou un **délai légal** (préavis, prescription, délai de dépôt). Si le libellé décrit une qualification, une appréciation, un risque, une option, une donnée constatée ou une pièce → il va ailleurs.

### Cas d'erreur

| Situation | Comportement | HTTP |
|---|---|---|
| Claude ignore la règle (variance LLM ~5-10 %) | **Fail-open** — le point passe quand même ; couverture par tests IT. Pas de blocage technique. | 200 |
| JSON invalide | Extraction fail-open existante inchangée | 200 |

---

## Analyse de cohérence transversale

| Cible | Applicable ? | Traitement |
|---|---|---|
| Prompt `CaseAnalysisService` + `EnrichedAnalysisService` | Oui | Cible directe — 2 templates, même bloc `points_procedure`. |
| Codes `critere_code` énumérés (FR_*, BE_*, IM21_*, FA06_*…) | Non touché | **Strictement inchangés** (pas de régression F-DT-08 / F-IM-21 / F-FA-06). La règle ne vise que `critere_code = null`. |
| Domaines : Travail / Famille / Immigration | Oui (transversal) | Règle générique. **Effet majeur en Travail** (où les intrus de fond ont été observés — Dupont-3) et en Immigration. |
| `ProcedureCheckService` / extraction | Non | Inchangé (le sens est sémantique → reste au LLM, comme tranché en SF-96-06 note). |
| Frontend `synthesis` / checklist | Non | Aucun changement d'affichage. |
| Bug doublons / limite 8 (SF-96-08) | Distinct | **Hors scope** — traité par une SF séparée (cumul de versions à l'affichage). SF-96-07 réduit le bruit *de fond*, pas les doublons. |
| Auth / Workspace / Plans / Navigation | Non | Aucun. |

### Décision

- [x] Étendu à toutes les cibles applicables (transversal par construction, comme SF-96-06).
- [x] Doublons traités séparément (SF-96-08, après investigation).

---

## Conformité F-IA-04 / Pré-fill IA

- [x] **Non applicable** — modification de prompt LLM, pas d'outil décisionnel ni de formulaire.

---

## Critères d'acceptation

- [ ] **CA1** : `CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` contient une section explicite bannissant de `points_procedure` (quand `critere_code = null`) les **qualifications de fond** (faute grave/lourde caractérisée, cause réelle et sérieuse, intention de nuire) → redirection `risques`.
- [ ] **CA2** : même prompt bannit les **constats de cadrage / données extraites** (« type de rupture identifié », « date identifiée dans les pièces ») → synthèse/métadonnées.
- [ ] **CA3** : le **test d'inclusion** (« les 3 statuts ont-ils un sens = formalisme/délai ? ») est formulé dans le prompt.
- [ ] **CA4** : idem CA1+CA2+CA3 dans `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE`.
- [ ] **CA5 (non-régression)** : les règles d'usage des `critere_code` énumérés et les 3 catégories SF-96-06 restent **intactes** (pas de suppression).
- [ ] **CA6** : tests unitaires de présence des nouvelles règles dans les 2 prompts ; IT pipeline (mock Anthropic) sur dossier Travail FR licenciement faute grave où une **qualification de fond** du fixture atterrit en `risques` et **pas** dans `points_procedure` (et un point de formalisme reste bien dans `points_procedure`).
- [ ] **CA7** : aucune migration, aucun changement frontend, format JSON de sortie inchangé.

---

## Périmètre

### Hors scope

- **Bug doublons / dépassement limite 8** → SF-96-08 (investigation en cours).
- Filtrage post-extraction heuristique (le sens reste au LLM, cf. note SF-96-06).
- Toute modification du frontend, du format JSON, de `ProcedureCheckService`.
- Re-traitement automatique des dossiers existants (ré-analyse manuelle par l'avocat).

---

## Technique

### Fichiers backend modifiés

| Fichier | Modification |
|---|---|
| `backend/.../analysis/CaseAnalysisService.java` | Extension `SYSTEM_PROMPT_TEMPLATE` — 2 catégories interdites (fond + constats) + test d'inclusion, à la suite des règles SF-96-06 du bloc `points_procedure`. |
| `backend/.../analysis/EnrichedAnalysisService.java` | Idem (même bloc). |
| `backend/.../analysis/CaseAnalysisServiceTest.java` | Assertions présence nouvelles règles + non-régression SF-96-06. |
| `backend/.../analysis/EnrichedAnalysisServiceTest.java` | Idem. |
| `backend/.../analysis/ProcedureCheckPromptHardeningIT.java` (existant SF-96-06) | Ajouter un cas Travail FR faute grave (qualif de fond → risques, formalisme → points_procedure). |

### Migration Liquibase

- [x] Non applicable.

---

## Plan de test

### Unitaires
- [ ] `CaseAnalysisServiceTest` : prompt contient les mots-clés des 2 nouvelles catégories + test d'inclusion.
- [ ] `CaseAnalysisServiceTest` : non-régression — règles SF-96-06 et codes énumérés toujours présents.
- [ ] `EnrichedAnalysisServiceTest` : idem (×2).

### Intégration
- [ ] `ProcedureCheckPromptHardeningIT` : cas Travail FR faute grave — fixture mock où `points_procedure` ne contient que du formalisme/délais ; une qualif de fond du fixture est en `risques`. Fail-open documenté si Claude dévie.

### Isolation workspace
- [x] Non applicable (prompt seulement).

---

## Analyse d'impact

- [x] **Aucune préoccupation transversale** (Auth/Workspace/Plans/Navigation).
- [x] **Aucun smoke E2E** — prompt en amont de la persistance, contrat API et écrans inchangés.

---

## Dépendances

- SF-96-06 (3 catégories interdites) — Done (base sur laquelle on étend).
- Indépendant de SF-96-08 (doublons) — zones de code disjointes.

---

## Notes et décisions

- **Pourquoi une SF de F-96 et pas une feature** : même raison que SF-96-06 — la cohérence du prompt `points_procedure` appartient à F-96.
- **Pourquoi au niveau prompt et pas extraction** : distinguer une qualification de fond d'une vérification est **sémantique** → relève du LLM, pas d'un filtre regex (cf. note SF-96-06). Fail-open assumé.
- **Origine** : test Dupont-3 (2026-06-16) — 5 intrus sur 14 (points 4, 6, 7, 10, 12) = qualifications de fond + constats de cadrage. Les 4 doublons (8, 9, 11, 13) relèvent de SF-96-08.
- **Cohérent avec la règle d'or** : VÉRIFIER dans `points_procedure` · PROPOSER dans pistes · ALERTER dans `risques` · QUESTIONNER dans `questions_ouvertes` · LISTER les pièces dans `pieces_manquantes`.
