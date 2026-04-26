# Mini-spec — F-FA-24 / SF-FA-24-07 Backend réserve héréditaire + action en réduction

## Identifiant

`F-FA-24 / SF-FA-24-07`

## Feature parente

`F-FA-24` — Droit des successions (chantier ~9-11 SF — déjà livrées : SF-01/02 dévolution, SF-03/04 testament, SF-05 donation).

## Statut

`in-progress`

## Date de création

2026-04-26

## Branche Git

`feat/SF-FA-24-07-backend-reserve-heriditaire`

---

## Objectif

Calculator + endpoint d'analyse de la **réserve héréditaire** (FR — art. 913 + 914-1 Cciv) et de la **recevabilité de l'action en réduction** des libéralités excédant la quotité disponible (art. 920-928 Cciv).

---

## Comportement attendu

### Cas nominal

L'avocat saisit la composition successorale (nombre d'enfants, présence du conjoint), le montant total de la succession, le total cumulé des libéralités (donations + legs), la date d'ouverture de la succession et la qualité du demandeur → l'outil calcule :
- la quotité disponible et la réserve héréditaire selon le barème art. 913,
- les montants en euros (réserve, quotité disponible, excédent réductible),
- la prescription restante (art. 921 — 5 ans à compter de l'ouverture),
- la recevabilité de l'action en réduction.

### Barème (art. 913 Cciv)

| Configuration | Quotité disponible | Réserve |
|---------------|--------------------|---------|
| 0 enfant + conjoint (art. 914-1) | 75 % | 25 % (conjoint) |
| 0 enfant sans conjoint | 100 % | 0 % |
| 1 enfant | 50 % | 50 % |
| 2 enfants | 33,33 % | 66,67 % (1/3 chacun) |
| 3+ enfants | 25 % | 75 % (75/N chacun) |

### Action en réduction (art. 920-928)

- **Recevable si** : `excedentReductible > 0` ET prescription pas écoulée (5 ans depuis ouverture, art. 921).
- **Délai prescription** = 5 ans à compter de l'ouverture de la succession (art. 921 al. 1).
- **Ordre de réduction** (art. 923-924) : legs réduits d'abord proportionnellement, puis donations dans l'ordre inverse de leur date.
- **Qualité du demandeur** : doit être héritier réservataire (descendant ou conjoint en l'absence de descendants).

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| `nombreEnfants` < 0 | rejet | 400 |
| `montantSuccession` ≤ 0 | rejet | 400 |
| `montantLibsTotal` < 0 | rejet | 400 |
| `dateOuvertureSuccession` future | rejet | 400 |
| `qualiteDuDemandeur` null | rejet | 400 |
| Demandeur = CONJOINT_SURVIVANT mais descendants présents | non recevable (conjoint évincé par descendants) | 200 |
| Pays workspace ≠ FRANCE | rejet (outil single-country) | 400 |
| Domaine ≠ DROIT_FAMILLE | rejet | 400 |
| Case file autre workspace | 404 | 404 |

---

## Critères d'acceptation

- [x] POST `/api/v1/case-files/{id}/reserve-heriditaire-analysis` calcule et persiste l'analyse (upsert 1:1).
- [x] GET du même endpoint renvoie l'analyse persistée (404 sinon).
- [x] Calcul correct sur les 5 configurations du barème (0/1/2/3+ enfants ± conjoint).
- [x] `excedentReductibleEur = max(0, libs - quotitéDispo)` arrondi à 2 décimales.
- [x] `actionReductionRecevable` = excédent > 0 ET prescription non écoulée.
- [x] `delaiPrescriptionRestantMois` ≥ 0, 0 si prescrit.
- [x] Réponse contient `baseJuridique`, `formule`, `messages`.
- [x] Outil exposé via `decision_tool_visibility_rules` ALWAYS_ON DROIT_FAMILLE FRANCE priority 95 tool_id `F-FA-24-reserve-heriditaire`.
- [x] Migration UUID `f1a04001-0000-0000-0000-ee0000000186` (pas de collision).

---

## Plan de test

### Unitaires (≥ 18)
1. 0 enfant + conjoint → QD=75 %, réserve=25 %.
2. 0 enfant sans conjoint → QD=100 %, réserve=0 %.
3. 1 enfant → QD=50 %, réserve=50 %.
4. 2 enfants → QD=33,33 %, réserve=66,67 %.
5. 3 enfants → QD=25 %, réserve=75 %.
6. 4 enfants → QD=25 %, réserve=75 % (75/4 chacun).
7. Libs ≤ QD → excédent = 0 → action NON recevable.
8. Libs > QD → excédent = libs − QD → action recevable.
9. Prescription écoulée (date > 5 ans) → action NON recevable même si excédent > 0.
10. Prescription juste atteinte (5 ans − 1 jour) → recevable.
11. Conjoint survivant demandeur sans descendants → recevable (réservataire art. 914-1).
12. Conjoint survivant demandeur avec descendants → non recevable (évincé par descendants).
13. Calcul `montantReserveEur` 200 000 × 50 % → 100 000.
14. Calcul `delaiPrescriptionRestantMois` ouverture il y a 24 mois → 36.
15. Validation `nombreEnfants` négatif → IAE.
16. Validation `montantSuccession` ≤ 0 → IAE.
17. Validation `montantLibsTotal` négatif → IAE.
18. Validation pays ≠ FRANCE → IAE.
19. Validation date future → IAE.
20. Score d'éligibilité (qualité demandeur + excédent + non prescrit) ≤ 100.

### Intégration (≥ 7)
1. POST nominal (1 enfant + libs > QD) → 200 + persistance.
2. GET après POST → 200, même valeurs.
3. POST workspace BE → 400.
4. POST workspace DROIT_DU_TRAVAIL → 400.
5. POST sur case file d'un autre workspace → 404.
6. POST `nombreEnfants` négatif → 400.
7. POST upsert (2 fois POST) → remplace l'analyse.
8. GET sans POST préalable → 404.

---

## Tables / endpoints / composants impactés

- **Table** : `reserve_heriditaire_analyses` (1:1 `case_files`).
- **Endpoint** : `/api/v1/case-files/{caseFileId}/reserve-heriditaire-analysis` (POST, GET).
- **Visibility** : `decision_tool_visibility_rules` ALWAYS_ON DROIT_FAMILLE FRANCE priority 95 — UUID `f1a04001-0000-0000-0000-ee0000000186`, tool_id `F-FA-24-reserve-heriditaire`.

## Hors périmètre

- Frontend (SF-FA-24-08 jumelle suivant la livraison backend).
- Calcul par soche (descendants prédécédés représentés) — V1 simplifiée : on prend le total des enfants.
- Réserve du conjoint en présence de descendants (le conjoint n'est pas réservataire dans ce cas).
- Régime BE (réserve différente — feature jumelle backlog F-FA-24-BE).
- Calcul détaillé par libéralité (ordre chronologique des donations) — V1 retourne juste l'excédent global ; ordre de réduction documenté en `messages`.
- Imputation de la donation entre époux ou des avantages matrimoniaux (renvoi vers SF action en retranchement, hors V1).

---

## Impact par domaine métier

Cette SF est **strictement Droit de la famille FR** — outil dédié successions FR, single-country.
- **Droit du travail** : non applicable (retour 400).
- **Immigration** : non applicable.
- **Famille FR** : ce qu'on livre.
- **Famille BE** : non couvert ici, feature jumelle dédiée au backlog (F-FA-24-BE), barème CC BE différent.

## Parité des domaines métier

Outil de niveau 5 (scoring de recevabilité). Pas d'équivalent dans Travail/Immigration (concept successoral propre au droit civil patrimonial). Pas de feature jumelle requise hors du chantier successions.

## Analyse de cohérence transversale

| Cible | Statut |
|-------|--------|
| Outils décisionnels FR famille (dévolution F-FA-24-01, testament F-FA-24-03, donation F-FA-24-05) | Pattern cohérent (BigDecimal, ALWAYS_ON, DROIT_FAMILLE FRANCE, single-country) — **intégré** |
| Outils BE famille | Hors scope SF — backlog F-FA-24-BE dédié |
| Outils Travail/Immigration | Non applicable |
| Préoccupation transversale "Outil décisionnel métier" | Nouveau, isolé : un outil = une situation (réserve + action en réduction = même flux décisionnel art. 913 + 920-928) |
