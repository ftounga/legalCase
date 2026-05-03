---
feature: F-184
subfeature: SF-184-02
title: Combler le trou visuel sous col-left en remontant bottom-sections dans col-left
domain: Frontend (transversal 3 domaines × 2 pays)
estimation: 0.5-1 h
status: Ready to dev
---

# SF-184-02 — Combler le trou sous col-left (suite SF-184-01)

## Objectif

Supprimer le grand espace blanc qui est apparu sous `col-left` (et qui s'aggrave quand la liste DOCUMENTS est repliée) en remontant les 3 blocs de `bottom-sections` (Délais + Notes + Outils décisionnels) directement dans `col-left` sous la section DOCUMENTS, et en supprimant le conteneur `bottom-sections` devenu inutile.

## Contexte (origine du bug)

SF-184-01 a déplacé `<app-case-dashboard>` de `bottom-sections` vers `col-right`. Conséquence non anticipée : `col-right` est devenue significativement plus haute que `col-left`. Comme `.detail-grid` utilise `align-items: start`, `col-left` reste collée en haut et un large espace blanc apparaît en dessous, jusqu'au début de `bottom-sections`. Quand l'utilisateur replie la liste DOCUMENTS (F-170 SF-170-01), `col-left` devient encore plus courte et le trou s'agrandit.

Capture utilisateur : `/screenshoot/Screenshot from 2026-05-03 01-40-17.png` (vide à la lecture mais bug confirmé verbalement en staging le 2026-05-03).

## Comportement nominal

1. **Avant SF-184-02** :
   ```
   .detail-grid
     .col-left   → infos + stats + DOCUMENTS
     .col-right  → bandeau questions + decisional-summary-panel (dashboard)
   .bottom-sections → Délais + Notes + Outils décisionnels
   ```

2. **Après SF-184-02** :
   ```
   .detail-grid
     .col-left   → infos + stats + DOCUMENTS + Délais + Notes + Outils décisionnels
     .col-right  → bandeau questions + decisional-summary-panel (dashboard)
   ```
   Le conteneur `.bottom-sections` est supprimé du DOM ainsi que sa règle SCSS (`max-width: 720px; margin-top: var(--spacing-lg);`).

3. **Ordre dans col-left** (de haut en bas) :
   1. `<mat-card class="detail-card">` (infos dossier)
   2. `<mat-card class="stats-card">` (stats)
   3. `<section class="td-section td-section--documents">` (DOCUMENTS pliable)
   4. `<app-case-deadlines-section>` (Délais)
   5. `<app-case-notes-section>` (Notes)
   6. `<app-decisional-tools-panel>` (Outils décisionnels)

4. **Espacement** : conserver l'espacement vertical actuel entre blocs grâce à `gap` ou `margin-top` cohérent avec le reste de col-left (les `mat-card` ont leurs propres marges, les `app-*` sections ont leur SCSS interne — vérifier visuellement qu'aucun bloc ne colle l'autre).

5. **`col-right` reste inchangée** — bandeau questions IA + `decisional-summary-panel` (SF-184-01).

## Cas d'erreur

- **Largeur insuffisante** (col-left < 720px sur écran 1024-1280px) — les composants `<app-decisional-tools-panel>` (grille 2 colonnes SF-169-01) et `<app-case-deadlines-section>` doivent rester lisibles. Si le rendu est cassé sur breakpoint moyen, prévoir media query d'ajustement (mais on attend un fonctionnement nominal car les composants sont déjà responsive).
- **Mobile (< 1024px)** : `.detail-grid` passe en `1fr` simple (déjà fait : SCSS ligne 63-65). Tous les blocs de col-left s'enchaînent verticalement, comme actuellement → aucun changement visuel attendu sur mobile.
- **Dashboard vide (verdictsCount = 0)** : aucun impact, col-right reste haute uniquement à cause du bandeau questions ou de la card dashboard vide. Le trou diminue mais ne disparaît pas si col-right > col-left après remontée. Critère d'acceptation : sur un dossier avec dashboard non vide (cas réel staging), `col-left` doit atteindre au moins 90 % de la hauteur de `col-right`.

## Critères d'acceptation

- [ ] `<app-case-deadlines-section>` apparaît dans `col-left` après la section DOCUMENTS, plus dans `bottom-sections`
- [ ] `<app-case-notes-section>` apparaît dans `col-left` après `<app-case-deadlines-section>`, plus dans `bottom-sections`
- [ ] `<app-decisional-tools-panel>` apparaît dans `col-left` après `<app-case-notes-section>`, plus dans `bottom-sections`
- [ ] Le `<div class="bottom-sections">` est supprimé du template
- [ ] La règle SCSS `.bottom-sections` est supprimée du fichier `case-file-detail.component.scss`
- [ ] Sur un dossier réel en staging (E-36 ou équivalent avec dashboard non vide), aucun trou blanc visible sous DOCUMENTS quand la liste docs est dépliée
- [ ] Sur le même dossier, aucun trou blanc visible sous DOCUMENTS quand la liste docs est repliée (le trou résiduel doit être < hauteur d'un bloc, idéalement nul)
- [ ] `<app-case-dashboard>` reste dans `col-right` (aucune régression SF-184-01)
- [ ] Suite Jest verte : tests existants `case-file-detail.component.spec.ts` adaptés + 1 nouveau test SF-184-02

## Plan de test minimal

### Tests Jest SF-184-02 (1 nouveau, 1 adapté)

| ID | Cas | Vérification |
|----|-----|--------------|
| T-01 | Position des 3 blocs ex-bottom | `<app-case-deadlines-section>`, `<app-case-notes-section>`, `<app-decisional-tools-panel>` sont dans `.col-left`, dans cet ordre, et **pas** dans `.bottom-sections`. `.bottom-sections` n'existe plus dans le DOM rendu. |
| T-02 (adapté) | Dashboard reste dans col-right (non-régression SF-184-01) | `<app-case-dashboard>` reste dans `.col-right`, wrappé dans `.decisional-summary-panel`. Le test SF-184-01 T-04 garde son assertion "pas dans bottom-sections" qui devient triviale (le sélecteur ne match plus rien) — l'assertion principale (présence dans col-right + wrapper) reste valide. |

### Non-régression (existants)

- 35 tests `case-dashboard.component.spec.ts` doivent rester verts (zéro modification du composant).
- Tests `case-file-detail.component.spec.ts` (4 nouveaux SF-184-01 + existants) doivent rester verts.

### Validation visuelle staging

- [ ] Tester sur dossier réel en staging avec :
  - liste DOCUMENTS dépliée + dashboard non vide → pas de trou
  - liste DOCUMENTS repliée + dashboard non vide → pas de trou (ou trou résiduel < hauteur d'un bloc)
- [ ] Tester sur écrans 1280px / 1440px / 1920px → pas de cassure layout
- [ ] Tester sur écran < 1024px (single column) → ordre de stacking conservé : infos / stats / docs / délais / notes / outils décisionnels / questions IA / dashboard

## Tables / endpoints / composants impactés

- **Aucune table impactée** (frontend pur).
- **Aucun endpoint impacté**.
- **Composants modifiés** :
  - `frontend/src/app/case-files/case-file-detail/case-file-detail.component.html` (déplacement des 3 balises depuis `bottom-sections` vers fin de `col-left`, suppression du `<div class="bottom-sections">`).
  - `frontend/src/app/case-files/case-file-detail/case-file-detail.component.scss` (suppression de la règle `.bottom-sections`).
  - `frontend/src/app/case-files/case-file-detail/case-file-detail.component.spec.ts` (1 nouveau test T-01 + adaptation T-04 SF-184-01 si nécessaire).

## Hors périmètre (volontaire)

- **Refonte interne d'un des 3 composants déplacés** (Délais, Notes, Outils) — ils sont déplacés tels quels.
- **Refactor `detail-grid`** — la grille reste `1fr 1fr` avec `align-items: start`.
- **Sticky col-left** — explicitement écarté (option 1 discutée puis abandonnée — ne remplit pas le trou).
- **Ajout d'un 3ᵉ ordre de tri ou groupement** — purement structurel.
- **Backend** — zéro impact.
- **Adaptation par domaine ou pays** — transversal, aucune adaptation.
- **Persistence du collapse / expand** — pas de toggle dans cette SF.

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : aucun impact — les 3 blocs déplacés sont déjà composants Angular packagés, leur fonctionnement interne est inchangé.
- [x] **Autres pays** : aucun impact — pas de différence FR/BE sur la position dans le DOM.
- [x] **Autres domaines** : aucun impact — pas de différence Travail/Immigration/Famille sur la position dans le DOM.
- [x] **Autres UI patterns** : la disparition de `.bottom-sections` peut concerner d'autres pages utilisant le même pattern. Scan : `grep -r "bottom-sections" frontend/src` → seul `case-file-detail` utilise cette classe (vérifié ci-dessous).
- [x] **Autres flows transversaux** : aucun impact (pas auth / workspace / plans / routing).

### Niveaux de vérification

- [x] **Modèle TypeScript** : non concerné (pas de changement signal/computed).
- [x] **Record/DTO backend** : non concerné.
- [x] **Service / logique métier** : non concerné.
- [x] **Entité JPA + schéma DB** : non concerné.
- [x] **Tests existants** : `case-file-detail.component.spec.ts` SF-184-01 T-04 vérifie la position du dashboard — assertion compatible avec SF-184-02 (le dashboard reste dans col-right).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Autre page utilisant `.bottom-sections` | À vérifier | grep pré-implémentation — si trouvé, élargir scope ou créer SF jumelle ; sinon non applicable |
| Autres outils décisionnels | Non | Pas de modification interne |
| F-IA-04 panel décisionnel | Non | `<app-decisional-tools-panel>` est juste déplacé, son interne est inchangé |
| Pré-fill IA / F-IA-03 | Non | Pas de modification du binding `synthesis` / `aiQuestions` / `procedureChecks` |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (zéro autre cible identifiée).
- [ ] Subfeature(s) parallèle(s) créée(s) — non applicable.
- [ ] Backlog VN — non applicable.
- [ ] Non applicable aux autres cibles (justification : SF purement structurelle locale à `case-file-detail`, pas de pattern UI/service partagé introduit).

## Impact par domaine métier

Cette SF est **transversale** et ne touche aucune logique métier :
- pas de différence Travail / Immigration / Famille,
- pas de différence FR / BE,
- aucune adaptation par domaine.

## Nouveau pattern UI ou service partagé

- **Pas de nouveau composant partagé** — pure réorganisation DOM locale.
- **Pas de nouveau service** — aucun ajout TypeScript.
- **Pattern supprimé** : la classe `.bottom-sections` disparaît. Vérification grep avant implémentation que cette classe n'est utilisée nulle part ailleurs (sinon élargir le scope).

## Préoccupations transversales

| Préoccupation | Concerné ? |
|---------------|-----------|
| Auth / Principal | Non |
| Workspace context | Non |
| Plans / limites | Non |
| Navigation / routing | Non |
| Outil décisionnel métier | Non (pas de changement métier — pure réorganisation) |

## Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — la SF ne touche ni à l'auth, ni au workspace context, ni au routing. Les tests `e2e/smoke/auth.spec.ts`, `workspace.spec.ts`, `navigation.spec.ts` ne sont pas impactés.

## Notes d'implémentation

- **Self-check pré-commit** :
  - `grep -n "bottom-sections" frontend/src/app/case-files/case-file-detail/case-file-detail.component.html` → doit retourner 0 résultat.
  - `grep -n "bottom-sections" frontend/src/app/case-files/case-file-detail/case-file-detail.component.scss` → doit retourner 0 résultat.
  - `grep -rn "bottom-sections" frontend/src` → doit retourner 0 résultat global (vérifier qu'aucun autre fichier ne référence la classe).
  - `grep -c "app-case-dashboard" frontend/src/app/case-files/case-file-detail/case-file-detail.component.html` → exactement 1 (non-régression SF-184-01).
- **Test visuel staging obligatoire** avant marquer Done : la SF est purement visuelle, les tests Jest seuls ne valident pas l'absence de trou.

## Estimation

0.5-1 h dev + tests + review.

## Référence backlog

- `docs/PRODUCT_SPEC.md` — F-184 (à rouvrir : 1/2 SF Terminée → En cours, ré-Terminée après merge SF-184-02).
- Origine : SF-184-01 mergée le 2026-05-03 (PR #764), trou détecté par utilisateur en staging le même jour.
