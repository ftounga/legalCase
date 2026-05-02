---
feature: F-184
subfeature: SF-184-01
title: Repositionner + redesigner le tableau de bord décisionnel sur la page dossier
domain: Frontend (transversal 3 domaines × 2 pays)
estimation: 1-2 h
status: Ready to dev
---

# SF-184-01 — Repositionner + redesigner le tableau de bord décisionnel

## Objectif

Déplacer `<app-case-dashboard>` de `bottom-sections` vers `col-right` (juste sous le bandeau "X question(s) complémentaire(s) en attente de réponse") et le redesigner en **carte premium** différenciée (Option B) pour qu'il devienne immédiatement identifiable comme la **synthèse des verdicts** des outils décisionnels — distincte du panel `<app-decisional-tools-panel>` qui reste un sélecteur d'outils.

## Comportement nominal

1. **Position** : sur la page `/case-files/{id}`, le tableau de bord est rendu dans la `col-right`, **après** le bandeau "Questions complémentaires" (`@if (questionGenerationRunning() || questions().length > 0) { … }`) et **avant** la fin de la `col-right`.
2. **Cadrage visuel** :
   - Wrapper `<section class="decisional-summary-panel">` avec :
     - bordure 2 px or `#C9973A` (canonique DESIGN_SYSTEM.md),
     - shadow `0 4px 16px rgba(201,151,58,0.15)`,
     - `border-radius: 10px`,
     - background `#FFFFFF`.
   - **Header sticky** intra-panel (sticky vis-à-vis du body de la card, pas du viewport — comportement normal d'un panel) :
     - background navy `#1A3A5C`,
     - barre fine accent or 2 px en `border-bottom`,
     - titre "Tableau de bord décisionnel" Merriweather 16 px blanc,
     - **count badge** "N verdicts disponibles" à droite (chip or sur navy 8 %).
3. **Count badge** : N = `riskScoreTile() ? 1 : 0` + somme des tailles des `themeSections().tiles`. Calculé dans le composant `case-dashboard` en computed signal `verdictsCount`, exposé publiquement et lu par le parent via template reference variable (`#dash`) → `{{ dash.verdictsCount() }}`. Si N = 0 → badge masqué.
4. **`<app-decisional-tools-panel>` reste dans `bottom-sections`** (sélecteur d'outils par thème) — sa fonction est différenciée et c'est volontaire.

## Cas d'erreur

- Aucun nouveau cas d'erreur introduit. Le composant `case-dashboard` continue de gérer son propre état `loading` / `isEmpty`. Le wrapper `decisional-summary-panel` se contente d'envelopper.
- Si `verdictsCount() === 0` → header reste affiché mais sans count badge (état vide géré par le composant interne avec son message "Aucun outil exécuté").

## Critères d'acceptation

- [x] `<app-case-dashboard>` n'apparaît plus dans `bottom-sections` du template.
- [x] `<app-case-dashboard>` apparaît dans `col-right` après le bandeau questions et avant la fin de `col-right`.
- [x] `<app-case-dashboard>` est enveloppé dans un `<section class="decisional-summary-panel">` avec header sticky navy/or et count badge.
- [x] Count badge affiche le nombre exact de verdicts (riskScore + tiles génériques).
- [x] `<app-decisional-tools-panel>` reste dans `bottom-sections` (pas de modification).
- [x] Aucun changement visuel sur le composant card interne SF-177-01 ni sur les composants outils.
- [x] Suite Jest verte : tests existants `case-dashboard` (32) + tests nouveaux SF-184-01 (≥ 3).

## Plan de test minimal

### Tests Jest SF-184-01 (3 nouveaux)

| ID | Cas | Vérification |
|----|-----|--------------|
| T-01 | Count badge — riskScore seul | `dashboard.set({riskScore: 75, riskLevel:'ALERT', tiles:[]})` → `verdictsCount()` = 1 |
| T-02 | Count badge — riskScore + 5 tiles | `dashboard.set({riskScore: 50, ... , tiles:[...5]})` → `verdictsCount()` = 6 |
| T-03 | Count badge — pas de riskScore + 0 tiles | `dashboard.set({riskScore: null, tiles:[]})` → `verdictsCount()` = 0 |

### Tests parent `case-file-detail` (1 nouveau)

| ID | Cas | Vérification |
|----|-----|--------------|
| T-04 | Position dashboard | Snapshot DOM : `<app-case-dashboard>` est dans `.col-right` (pas dans `.bottom-sections`), wrappé dans `.decisional-summary-panel`. |

### Non-régression (existants)

- 32 tests `case-dashboard.component.spec.ts` doivent rester verts.
- Tests `case-file-detail.component.spec.ts` doivent rester verts (vérifier qu'aucune assertion sur la position du dashboard n'existe ailleurs).

## Tables / endpoints / composants impactés

- **Aucune table impactée** (frontend pur).
- **Aucun endpoint impacté**.
- **Composants modifiés** :
  - `frontend/src/app/case-files/case-file-detail/case-file-detail.component.html` (déplacement balise + wrapper).
  - `frontend/src/app/case-files/case-file-detail/case-file-detail.component.scss` (styles `.decisional-summary-panel`).
  - `frontend/src/app/case-files/case-dashboard/case-dashboard.component.ts` (ajout `verdictsCount` computed public).
  - `frontend/src/app/case-files/case-dashboard/case-dashboard.component.spec.ts` (3 nouveaux tests).
  - `frontend/src/app/case-files/case-file-detail/case-file-detail.component.spec.ts` (1 nouveau test).

## Hors périmètre (volontaire)

- **Refonte de la card SF-177-01** (palette, typo, métadonnées) — déjà canonique post F-177.
- **Filtres / tri sur le dashboard** — déjà groupé par thème post SF-167-05.
- **Sticky scroll long** sur le panel — la `col-right` est sticky via CSS existant ailleurs.
- **Refactor structurel `detail-grid`** — la grille reste 2 colonnes 1fr/1fr.
- **Backend** — zéro impact.
- **Adaptation par domaine ou pays** — transversal, aucune adaptation.
- **Persistence du collapse / expand** — pas de toggle dans cette SF.

## Analyse de cohérence transversale

- **Outils décisionnels métier (Travail FR/BE, Immigration FR/BE, Famille FR/BE)** : aucun impact — la SF ne touche pas aux composants outils ni aux cards.
- **Pré-fill IA / F-IA-03** : aucun impact — la SF ne change pas le binding `synthesis` / `aiQuestions` / `procedureChecks` ni leur propagation depuis le dashboard vers les modals (déjà câblé par SF-177-09).
- **F-IA-04 panel décisionnel** : aucun impact direct — `<app-decisional-tools-panel>` n'est pas modifié et reste dans `bottom-sections`.
- **Auth / Principal / Workspace context / Plans / Routing** : aucun impact.

## Impact par domaine métier

Cette SF est **transversale** et ne touche aucune logique métier :
- pas de différence Travail / Immigration / Famille,
- pas de différence FR / BE,
- pas de différence par pays sur le styling (le tableau de bord agrège déjà tous les domaines via les 85 mappers post F-167).

## Nouveau pattern UI ou service partagé

- **Pas de nouveau composant partagé** — le wrapper `.decisional-summary-panel` est local à `case-file-detail` (pas de réutilisation prévue ailleurs ; si un besoin émerge, extraction en composant dédié possible plus tard).
- **Pas de nouveau service** — `verdictsCount` est un computed sur `CaseDashboardComponent` (logique locale).

## Préoccupations transversales

| Préoccupation | Concerné ? |
|---------------|-----------|
| Auth / Principal | Non |
| Workspace context | Non |
| Plans / limites | Non |
| Navigation / routing | Non |
| Outil décisionnel métier | Non (pas de changement métier — pure réorganisation) |

## Notes d'implémentation

- **Sticky du header intra-card** : `position: sticky; top: 0;` à l'intérieur du panel. Le panel lui-même n'est pas sticky vis-à-vis du viewport (comportement standard et non régressif).
- **Wrapper SCSS** dans `case-file-detail.component.scss` (encapsulé par Angular) — pas de fuite globale.
- **Self-check pré-commit** : grep que `app-case-dashboard` apparaît exactement 1 fois dans `case-file-detail.component.html` (dans `col-right`), 0 fois dans `bottom-sections`.

## Estimation

1-2 h dev + tests + review.

## Référence backlog

`docs/PRODUCT_SPEC.md` — F-184 ligne 416 (post commit `c68f537c` 2026-05-02).
