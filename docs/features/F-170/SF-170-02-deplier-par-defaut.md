# SF-170-02 — Section "Documents" toujours dépliée par défaut, persistance retirée

**Feature parente** : F-170 — Section Documents en accordéon
**Type** : frontend
**Branche** : `feat/SF-170-02-deplier-par-defaut`
**Effort estimé** : ~20-30 min
**Date de création** : 2026-05-01
**Statut** : draft

---

## Objectif (1 phrase)

Corriger la régression UX introduite par SF-170-01 où, lorsque l'avocat avait replié manuellement la section "Documents" sur un dossier, la persistance `sessionStorage` masquait au rechargement la liste des `pendingFiles` ET le bouton "Uploader les documents", rendant le flux d'upload bloquant — en retirant la persistance, en gardant le repli manuel intra-session, et en forçant le dépliage automatique dès qu'au moins un fichier est en attente d'upload.

---

## Contexte / Bug observé

`frontend/src/app/case-files/case-file-detail/case-file-detail.component.html` (issu de SF-170-01) :

- Ligne 96 : `<header class="td-header" (click)="toggleDocsCollapsed()" ...>`
- Ligne 116 : `@if (!docsCollapsed()) { <div id="section-documents-body">`
- Ligne 144-206 : la liste `pendingFiles` + le bouton **"Uploader les documents (n)"** sont à l'intérieur du bloc `@if (!docsCollapsed())`.

`case-file-detail.component.ts` :

- Ligne 98 : `readonly docsCollapsed = signal(false);`
- Ligne 347 : `restoreDocsCollapsedFromSession(id)` au `ngOnInit`
- Ligne 425-447 : clé `case-file-{id}-docs-collapsed` lue/écrite dans `sessionStorage`.

**Scénario du bug** (reproductible) :

1. Avocat ouvre un dossier riche, replie la section "Documents" pour gagner de l'espace vertical.
2. `sessionStorage['case-file-{id}-docs-collapsed'] = 'true'`.
3. À une session ultérieure (même onglet, après navigation ou reload) il revient sur le même dossier — `restoreDocsCollapsedFromSession` lit `'true'`, la section est repliée.
4. Il clique "Ajouter des documents" (bouton dans le header, donc visible). Le file picker s'ouvre, il sélectionne 3 fichiers.
5. Les 3 fichiers sont ajoutés à `pendingFiles()`. Mais comme `docsCollapsed() === true`, **ni la liste, ni les options OCR, ni le bouton "Uploader les documents (3)" ne sont rendus**.
6. L'avocat est bloqué : il ne sait pas où trouver le bouton de validation, il pense que l'upload n'a pas marché.

**Décision (Option B validée par le PO le 2026-05-01)** : retirer la persistance `sessionStorage`, garder le toggle pour replier en cours de session, et **auto-déplier** dès que `pendingFiles().length > 0` pour que le flux d'upload reste visible quoi qu'il arrive.

---

## Comportement attendu

### Cas nominal

1. Au chargement d'un dossier (premier accès ou rechargement) : la section "Documents" est **toujours dépliée** (`docsCollapsed() === false`).
2. L'avocat peut replier manuellement la section en cliquant sur le header (toggle in-session conservé pour permettre l'économie de scroll sur dossiers riches).
3. **Aucune lecture ni écriture `sessionStorage`** n'est faite — l'état n'est pas persisté entre rechargements ni entre navigations vers d'autres dossiers.
4. **Auto-dépliage défensif** : un `effect()` Angular surveille `pendingFiles()`. Dès que `pendingFiles().length > 0`, si la section est repliée, elle est dépliée automatiquement (`docsCollapsed.set(false)`). Ainsi, même si l'avocat replie puis clique "Ajouter des documents", la liste et le bouton submit redeviennent visibles dès la sélection des fichiers.
5. Le bouton "Ajouter des documents" reste dans le header avec `(click)="triggerUpload(); $event.stopPropagation()"` (inchangé).
6. Le toggle clavier (`Entrée` au focus du header) reste fonctionnel (inchangé).
7. L'`id="section-documents"` et tous les attributs ARIA (`aria-expanded`, `aria-controls`, `tabindex`, `role`) sont conservés.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `pendingFiles()` passe de N≥1 à 0 (upload terminé ou tous retirés) | La section reste **dépliée** (l'auto-expand n'inverse pas — l'avocat peut ensuite replier manuellement s'il le souhaite). |
| `pendingFiles()` est non vide au tout premier `ngOnInit` (improbable mais défensif) | La section démarre dépliée (cas nominal couvert puisque la valeur initiale est déjà `false`). |
| `sessionStorage` indisponible (navigateur en mode incognito strict, etc.) | Aucun impact — le code de persistance est entièrement supprimé. |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : F-DT-XX / F-FA-XX / F-IM-XX — Non applicable (la persistance `sessionStorage` du repli est spécifique à la section "Documents" de `case-file-detail`, pas répliquée ailleurs).
- [x] **Autres pays** : France / Belgique — Non applicable (pas de logique pays).
- [x] **Autres domaines** : DROIT_DU_TRAVAIL / DROIT_FAMILLE / DROIT_IMMIGRATION — Non applicable (UI transversale, indépendante du domaine).
- [x] **Autres UI patterns** : autres accordéons `td-section` / `td-header` / `td-body` (F-168 panel décisionnel, F-169 grid thèmes, sections fiche prudhommale, etc.) — **À scanner** : aucune autre section de la page dossier ne persiste son état replié en `sessionStorage` aujourd'hui (vérifié par `grep -n "sessionStorage" frontend/src/app/case-files/case-file-detail/`). La SF-170-01 était un cas isolé. Donc retirer ce mécanisme n'introduit pas d'incohérence — au contraire, ramène le comportement aux autres sections (toujours dépliées par défaut, repli intra-session uniquement).
- [x] **Autres flows transversaux** : auth / workspace context / plans / navigation — Non applicable.

### Niveaux de vérification

- [x] **Modèle TypeScript** : signal `docsCollapsed` + méthodes `toggleDocsCollapsed` + `restoreDocsCollapsedFromSession` + `docsCollapsedKey` — concernés.
- [x] **Record / DTO backend** : aucun (UI pure).
- [x] **Service / logique métier** : aucun.
- [x] **Entité JPA + schéma DB** : aucun.
- [x] **Tests existants** : `case-file-detail.component.spec.ts` T-01..T-04 (SF-170-01) — à mettre à jour : T-02 et T-04 deviennent obsolètes (suppression de la persistance), T-01 reste valide (`docsCollapsed === false` par défaut), T-03 doit être ajusté (toggle ne doit plus écrire en sessionStorage). Ajouter T-05 nouveau : auto-expand quand pendingFiles devient non vide.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Persistance `sessionStorage` du repli sur d'autres sections (Analyse, Synthèse, Outils décisionnels) | Non | Aucune autre section ne persiste son état (vérifié). Pas de cible parallèle. |
| Toggle clavier des autres `td-header` | Non | Conservé, inchangé. |
| Tests Jest existants `case-file-detail.component.spec.ts` | Oui | Mise à jour des tests T-02/T-03/T-04 dans cette SF, ajout T-05. |

### Décision

- [x] Modification limitée à la section Documents de `case-file-detail` — ramène au comportement standard des autres sections (pas de persistance).
- [x] Mise à jour des 4 tests T-01..T-04 dans cette SF (suppression T-02/T-04 devenus obsolètes, ajustement T-03, ajout T-05 auto-expand).
- [x] Pas de SF parallèle nécessaire.

---

## Critères d'acceptation vérifiables

1. La méthode `restoreDocsCollapsedFromSession(caseFileId)` est **supprimée** du composant `case-file-detail.component.ts`.
2. La méthode privée `docsCollapsedKey(caseFileId?)` est **supprimée**.
3. Au `ngOnInit`, plus aucun appel à `restoreDocsCollapsedFromSession` (la ligne 347 actuelle est retirée).
4. La méthode `toggleDocsCollapsed()` ne fait plus aucun appel à `sessionStorage.setItem` — elle se limite à `this.docsCollapsed.set(!this.docsCollapsed())`.
5. Le signal `docsCollapsed` conserve sa valeur initiale `signal(false)` (déplié par défaut).
6. Un `effect()` Angular est ajouté dans le constructeur ou `ngOnInit` : si `pendingFiles().length > 0` ET `docsCollapsed() === true`, alors `docsCollapsed.set(false)`. Ce `effect` ne s'exécute que dans le sens "auto-expand", jamais "auto-collapse".
7. Le HTML du header (ligne 95-114 de `case-file-detail.component.html`) reste **inchangé** : header toujours cliquable, chevron toujours affiché, bouton "Ajouter des documents" inchangé, attributs ARIA inchangés.
8. Le bloc `@if (!docsCollapsed()) { ... }` (ligne 116) reste inchangé — le rendu conditionnel du body est conservé.
9. **Test T-01 (conservé)** : `docsCollapsed` est `false` par défaut au `ngOnInit`.
10. **Test T-02 (supprimé)** : l'ancien test "lit `'true'` depuis sessionStorage et applique l'état replié" est retiré.
11. **Test T-03 (ajusté)** : `toggleDocsCollapsed()` bascule la valeur, **sans appel à `sessionStorage.setItem`** (vérifier que `setItem` n'est jamais appelé dans le test).
12. **Test T-04 (supprimé)** : l'ancien test "fail-silent si sessionStorage indisponible" devient obsolète puisque le code n'utilise plus `sessionStorage`.
13. **Test T-05 (nouveau)** : un avocat replie la section (`docsCollapsed.set(true)`), puis ajoute des fichiers via `onFileSelected` qui peuple `pendingFiles` ; après détection de changement, `docsCollapsed()` est `false`.
14. **Test T-06 (nouveau, défensif)** : si l'avocat n'a pas replié la section et que `pendingFiles` devient non vide, `docsCollapsed()` reste `false` (l'effect n'est pas perturbant).
15. **Test T-07 (nouveau)** : après upload terminé (`pendingFiles.set([])`), `docsCollapsed()` reste à sa valeur courante (l'effect ne re-replie pas).
16. Aucune référence à `sessionStorage` ne subsiste dans `case-file-detail.component.ts` (vérifié par grep `grep -n "sessionStorage" case-file-detail.component.ts` → 0 résultat).
17. Tous les tests Jest existants de `case-file-detail.component.spec.ts` restent verts (hors T-02 et T-04 supprimés).
18. Aucune régression visuelle sur les autres sections de la page dossier.
19. Self-check avant commit : `cd frontend && npm test -- --testPathPattern=case-file-detail` doit être vert.

---

## Périmètre

### Hors scope (explicite)

- Suppression du toggle (header reste cliquable, le repli intra-session reste possible).
- Modification du HTML (header / bouton / chevron / ARIA inchangés).
- Modification du SCSS (les styles `td-section` / `td-header` / `td-body` restent intacts).
- Migration de l'état vers un autre store (NgRx, service partagé, etc.) — overkill pour un toggle UI in-session.
- Modification du flux d'upload lui-même (`onFileSelected`, `uploadPendingFiles`) — inchangés.
- Backend : aucune modification (frontend pur).

---

## Technique

### Endpoint(s)

Aucun (frontend pur).

### Composants Angular

- `case-file-detail.component.ts` — modification : suppression méthodes `restoreDocsCollapsedFromSession`, `docsCollapsedKey`, simplification `toggleDocsCollapsed`, ajout `effect` auto-expand.
- `case-file-detail.component.html` — inchangé.
- `case-file-detail.component.spec.ts` — mise à jour : suppression T-02/T-04, ajustement T-03, ajout T-05/T-06/T-07.

### Migration Liquibase

- [x] Non applicable

---

## Plan de test

### Tests Jest unitaires (`case-file-detail.component.spec.ts`)

- [x] T-01 (existant, conservé) — `docsCollapsed` est `false` par défaut au `ngOnInit`.
- [x] T-02 (existant, **supprimé**) — devenu obsolète.
- [x] T-03 (existant, **ajusté**) — `toggleDocsCollapsed()` bascule la valeur sans toucher à `sessionStorage`. Vérifie que `sessionStorage.setItem` n'est jamais appelé (spy).
- [x] T-04 (existant, **supprimé**) — devenu obsolète.
- [x] T-05 (nouveau) — `docsCollapsed.set(true)` puis `pendingFiles.set([file1, file2])` → après détection (`fixture.detectChanges()` ou `flushEffects()`), `docsCollapsed()` est `false`.
- [x] T-06 (nouveau, défensif) — `docsCollapsed === false`, `pendingFiles.set([file1])` → `docsCollapsed()` reste `false`.
- [x] T-07 (nouveau) — `docsCollapsed.set(true)`, puis `pendingFiles.set([file1])` (auto-expand → false), puis `pendingFiles.set([])` → `docsCollapsed()` reste `false` (l'effect ne re-collapse jamais).

### Tests d'intégration

- [x] Non applicable (frontend pur, comportement UI couvert par Jest).

### Isolation workspace

- [x] Non applicable — cette SF ne touche aucune donnée multi-tenant.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — modification limitée au composant `case-file-detail` (UI in-session, sans impact sur auth, workspace, plans, navigation).

### Composants / endpoints existants potentiellement impactés

Aucun. La méthode `toggleDocsCollapsed()` reste publique (utilisée depuis le HTML). Les méthodes supprimées (`restoreDocsCollapsedFromSession`, `docsCollapsedKey`) sont privées et n'ont aucun consommateur externe.

### Smoke tests E2E concernés

- [x] Aucun smoke test E2E n'est concerné — le flux d'upload n'est pas couvert par les smoke tests `e2e/smoke/` (auth, workspace switch, navigation), et la régression visée est in-session UI.

---

## Impact par domaine métier

Cette feature est **transversale (infrastructure UI)** : aucune adaptation par domaine (droit du travail / immigration / famille) ni par pays (France / Belgique). Elle touche uniquement la section "Documents" du composant `case-file-detail`, commune à tous les dossiers quel que soit leur domaine ou pays. Aucune logique conditionnelle métier n'est ajoutée ni modifiée.

---

## Dépendances

### Subfeatures bloquantes

- SF-170-01 (mergée 2026-04-27, PR #701) — fournit l'accordéon dont on supprime ici la persistance.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **Décision : auto-expand sur `pendingFiles` non vide** — choix défensif retenu pour garantir que le flux d'upload reste visible même si l'avocat a replié manuellement la section dans la même session. Sans ce mécanisme, retirer la persistance corrige le cas du reload mais pas le cas du repli manuel suivi d'un `Ajouter des documents`.
- **Décision : conserver le toggle manuel** — le bénéfice originel de F-170 (gain d'espace vertical sur dossiers riches) est conservé. Seule la persistance est retirée.
- **Décision : pas de migration sessionStorage cleanup** — les valeurs `case-file-{id}-docs-collapsed` qui resteraient dans le sessionStorage des avocats actuels ne sont plus lues, donc inertes. `sessionStorage` est purgé à la fermeture de l'onglet, donc le squat est temporaire.
