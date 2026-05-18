# Mini-spec — F-244 / SF-244-02 — Onglet « Décision » : regroupement, couplage saisie → verdict, badge prefill agrégé

## Identifiant
`F-244 / SF-244-02`

## Feature parente
`F-244` — Refonte de l'architecture de l'information de l'espace décisionnel du détail dossier

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
`feat/SF-244-02-onglet-decision`

---

## Objectif

Faire de l'onglet « Décision » un **espace décisionnel cohérent** : outils de simulation et tableau de bord agencés en colonne d'entrée / colonne de verdict alignées verticalement, couplage saisie → verdict rendu découvrable, et badge `auto_awesome` de pré-remplissage IA agrégé au niveau de l'onglet.

---

## Comportement attendu

### Cas nominal

Dans l'onglet « Décision » de `/case-files/:id` :

- L'espace décisionnel est présenté en **2 colonnes alignées verticalement** sur viewport large (≥ 1024 px) :
  - **Colonne de saisie** (gauche) : `decisional-tools-panel` — l'avocat y renseigne les outils.
  - **Colonne de verdict** (droite) : carte « Tableau de bord décisionnel » (`case-dashboard`), en position `sticky` afin de rester dans le champ de vision pendant la saisie d'un outil.
  - Sous le tableau de bord, dans la même colonne de verdict : `conclusions-section` (F-98).
- Sur viewport étroit (< 1024 px), les 2 colonnes s'**empilent** (saisie au-dessus, verdict en dessous) — le `sticky` est neutralisé.
- Un **bandeau de couplage** discret coiffe l'espace décisionnel : il explicite que renseigner un outil à gauche met à jour le tableau de bord à droite (signal de découvrabilité de l'ajustement 1 de l'audit). Le bandeau porte l'icône `sync_alt`.
- L'**onglet « Décision »** (barre `mat-tab-group`) porte un **badge `auto_awesome`** affichant le nombre total de champs pré-remplis par l'IA = somme des `getPrefillCount()` de tous les outils visibles du panel. Le badge est masqué quand le total vaut 0.
- Quand le total de prefill passe de 0 à > 0 (retour d'analyse), l'onglet « Décision » est **mis en avant** : le badge devient visible ; aucune pré-sélection forcée de l'onglet (l'avocat reste sur son onglet courant — le badge suffit à signaler le travail de l'IA, conformément à la sous-règle « un onglet fermé ne masque pas le travail de l'IA »).
- Le badge se recalcule à chaque émission de `CaseDashboardRefreshService.refresh$` (déjà déclenchée en fin de run d'analyse) et à chaque changement de `synthesis`.

### Cas d'erreur / cas limites

| Situation | Comportement attendu |
|-----------|---------------------|
| Aucun outil visible (panel vide) | espace décisionnel rendu, colonne de saisie affiche l'empty state du panel ; badge onglet masqué (total 0) |
| Aucun champ pré-remplissable | badge `auto_awesome` de l'onglet masqué |
| `synthesis` nulle (analyse pas encore lancée) | outils sans pré-fill ; badge masqué ; bandeau de couplage affiché (statique) |
| Viewport mobile (< 1024 px) | colonnes empilées, `sticky` neutralisé, bandeau de couplage conservé |
| Un composant outil n'expose pas `getPrefillCount` | il compte 0 dans l'agrégat (forward-compat, déjà géré par `getToolPrefillCount`) |

---

## Analyse de cohérence transversale

### Périmètres scannés

- **Autres outils métier** : non concerné — SF-244-02 ne touche **aucune logique** d'outil décisionnel. Le panel `decisional-tools-panel` est repositionné et instrumenté d'un `@Output` agrégé ; les composants outils eux-mêmes (`*-section`) ne sont pas modifiés.
- **Autres pays / domaines** : l'agencement 2 colonnes et le badge agrégé sont transversaux (FR/BE × 3 domaines), `case-file-detail` étant un composant unique.
- **Autres UI patterns** : le badge `auto_awesome` sur l'onglet réutilise la convention prefill existante (`decision-tool-card` affiche déjà `auto_awesome` par carte ; `prefillCountFor()` existe déjà dans le panel). Aucun nouveau pattern partagé : on agrège un signal déjà calculé. Le bandeau de couplage est un bloc local de la page dossier (pas de composant `shared/`).
- **Autres flows transversaux** : navigation interne de l'écran modifiée (la structure d'onglets reçoit un badge). Auth / workspace / plans : non touchés.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Logique des outils décisionnels (`*-section`) | Non | repositionnement uniquement, zéro modification de code interne |
| `decisional-tools-panel` | Oui | ajout d'un `@Output() prefillTotalChange` (agrégat des `prefillCountFor`) + `triggerRefresh` re-câblé pour le recalcul |
| `case-dashboard` | Non | déplacé dans la colonne de verdict, inchangé |
| `case-dashboard-stepper` | Non | son extension = SF-244-03 |
| Synthèse ↔ outils | Non | SF-244-04 |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (l'unique composant adjacent instrumenté — `decisional-tools-panel` — est traité ici ; le badge agrégé remonte au conteneur onglet du `case-file-detail`).

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF-244-02 ne crée ni ne modifie aucun composant décisionnel `<app-XXX-section>` et ne consomme aucun endpoint décisionnel. Elle réorganise l'agencement de l'onglet « Décision » et agrège un compteur (`getPrefillCount()`) déjà exposé par les outils existants. Le pré-fill IA par outil et la validation F-IA-03 restent portés par les composants outils inchangés ; SF-244-02 se contente d'en **remonter la somme** au niveau de l'onglet (sous-règle anti-surcharge de l'audit, invariant 4 de l'étape 0).

---

## Critères d'acceptation

- [ ] Dans l'onglet « Décision », outils et tableau de bord sont en 2 colonnes sur viewport ≥ 1024 px ; la colonne de verdict (tableau de bord) est en position `sticky`.
- [ ] Sur viewport < 1024 px, les 2 colonnes s'empilent (saisie puis verdict) et le `sticky` est neutralisé.
- [ ] Un bandeau de couplage `sync_alt` coiffe l'espace décisionnel et explicite le lien saisie → verdict.
- [ ] L'onglet « Décision » du `mat-tab-group` affiche un badge `auto_awesome` avec le total de champs pré-remplis IA dès que ce total > 0.
- [ ] Le badge est masqué quand le total vaut 0 (aucun outil pré-remplissable / pas d'analyse).
- [ ] Le total du badge = somme des `getPrefillCount()` de tous les outils visibles (always-on + contextual).
- [ ] Le badge se recalcule à la fin d'un run d'analyse (`CaseDashboardRefreshService.refresh$`) et au changement de `synthesis`.
- [ ] `conclusions-section` (F-98) reste présent dans l'onglet « Décision », dans la colonne de verdict, sous le tableau de bord.
- [ ] Aucune régression de comportement des outils ni du tableau de bord (repositionnement pur).
- [ ] Tests Jest `case-file-detail` + `decisional-tools-panel` adaptés et verts ; `npm run build` prod OK.
- [ ] Smoke tests E2E verts (parcours dossier inchangé).

---

## Périmètre

### Hors scope (explicite)

- Extension du `case-dashboard-stepper` → **SF-244-03**.
- Reconnexion synthèse ↔ outils (point d'entrée bidirectionnel) → **SF-244-04**.
- Toute modification de la logique métier d'un outil décisionnel ou du tableau de bord.
- Restyle visuel global (DESIGN_SYSTEM) — on réutilise les tokens existants.
- Onglets routés / deeplinkables — inchangé depuis SF-244-01.
- Tout changement backend, endpoint, migration.

---

## Technique

### Endpoint(s)
Aucun.

### Tables impactées
Aucune.

### Migration Liquibase
- [x] Non applicable

### Composants Angular

- `case-file-detail.component.html` — onglet « Décision » : wrapping de `decisional-tools-panel` et de la carte tableau de bord + `conclusions-section` dans une grille 2 colonnes `.decision-space` ; ajout du bandeau de couplage ; le `<mat-tab label="Décision">` passe à un template `<ng-template mat-tab-label>` portant le badge `auto_awesome`.
- `case-file-detail.component.ts` — signal `decisionPrefillTotal`, handler `onDecisionPrefillTotalChange()` ; pas de mutation dans un `subscribe()` (le panel émet via `@Output`, géré par Angular CD ; `markForCheck` non requis — composant non OnPush).
- `case-file-detail.component.scss` — grille `.decision-space` 2 colonnes + `sticky` colonne verdict + media query < 1024 px ; styles du bandeau de couplage ; styles du badge d'onglet.
- `decisional-tools-panel.component.ts` — `@Output() prefillTotalChange = new EventEmitter<number>()` ; émission après `loadVisibility()` (succès) et à chaque `refresh$`. Le total = `Σ prefillCountFor(toolId)` sur `resolvedAlwaysOn() ∪ resolvedContextual()`.

### Décision technique

- **Réintroduction d'une grille 2 colonnes locale à l'onglet** : SF-244-01 a supprimé la grille `.detail-grid` *globale* ; SF-244-02 réintroduit une grille **strictement bornée à l'onglet « Décision »** (`.decision-space`), conforme à l'ajustement 1 de l'audit (« modèle colonne d'entrée / colonne de sortie conservé »). Ce n'est pas un retour en arrière : la grille ne porte plus tout l'écran, seulement l'espace décisionnel.
- **`sticky` sur la colonne de verdict** : garantit que le tableau de bord reste dans le champ de vision pendant la saisie (défaut 1a de l'audit). Neutralisé < 1024 px (empilement).
- **Badge agrégé via `@Output`** : le panel connaît déjà `prefillCountFor()` ; il expose la somme au parent qui la porte sur l'onglet. Pas de service partagé — couplage parent/enfant direct, suffisant et testable.
- **Pas de pré-sélection forcée de l'onglet** : la sous-règle de l'audit autorise « pastille OU pré-sélection » ; on retient la pastille (badge) seule pour ne pas voler le focus de l'avocat. Décision documentée — modifiable si retour terrain.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `decisionPrefillTotal` | 0 | recalculé à chaque émission `prefillTotalChange` du panel |

---

## Plan de test

### Tests unitaires (Jest)

**`case-file-detail.component.spec.ts`**
- [ ] L'onglet « Décision » contient une grille `.decision-space` avec `decisional-tools-panel` et `case-dashboard`.
- [ ] Le bandeau de couplage `sync_alt` est rendu dans l'onglet « Décision ».
- [ ] Le badge `auto_awesome` de l'onglet est masqué quand `decisionPrefillTotal` vaut 0.
- [ ] Le badge `auto_awesome` est rendu avec le total quand `decisionPrefillTotal` > 0.
- [ ] `onDecisionPrefillTotalChange(n)` met `decisionPrefillTotal` à `n`.
- [ ] `conclusions-section` reste présent dans l'onglet « Décision ».

**`decisional-tools-panel.component.spec.ts`**
- [ ] `prefillTotalChange` émet 0 quand aucun outil visible.
- [ ] `prefillTotalChange` émet la somme des `prefillCountFor()` des outils résolus.
- [ ] `prefillTotalChange` ré-émet après un `refresh$`.

### Tests d'intégration
Non applicable — SF purement frontend, aucun endpoint.

### Isolation workspace
- [x] Non applicable — SF de mise en page + agrégation d'un compteur ; aucun accès données nouveau.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing frontend** — la barre d'onglets (SF-244-01) reçoit un badge sur l'onglet « Décision ». ⚠️ Précision : **aucune route Angular, aucun guard, aucune redirection** n'est ajouté/modifié. La case est cochée car la navigation perçue (signalétique d'onglet) change → impose l'analyse d'impact + smoke tests E2E.
- Auth / Principal, Workspace context, Plans / limites : non touchés.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `decisional-tools-panel` | nouvel `@Output` — purement additif, ne change pas le rendu | tests Jest panel (émission `prefillTotalChange`) ; suites existantes inchangées |
| `case-dashboard` | déplacé dans la colonne de verdict — piloté par `@Input`, rendu identique | tests Jest présence dans l'onglet ; suites existantes inchangées |
| `conclusions-section` (F-98) | déplacé dans la colonne de verdict — piloté par `@Input` | test Jest présence dans l'onglet |
| `case-dashboard-stepper` | non touché ici (extension = SF-244-03) | — |
| tests E2E `/case-files/:id` | sélecteurs de l'onglet Décision inchangés (`data-tab-panel="decision"` conservé) | `cd e2e && npm test` avant push |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/*` — tout parcours touchant `/case-files/:id` (onglet Décision). `cd e2e && npm test` **obligatoire avant push** (préoccupation transversale navigation cochée).

---

## Dépendances

### Subfeatures bloquantes

- `SF-244-01` — statut : `done` (mergée PR #980). SF-244-02 s'appuie sur la structure en onglets.

### Questions ouvertes impactées
- [ ] Aucune — vérifié : aucun sujet `docs/OPEN_QUESTIONS.md` relatif à l'espace décisionnel du dossier.

---

## Notes et décisions

- **Grille 2 colonnes locale à l'onglet** — réintroduite délibérément (ajustement 1 de l'audit), bornée à l'onglet « Décision », pas un retour à la grille globale supprimée par SF-244-01.
- **Badge sans pré-sélection d'onglet** — choix de ne pas voler le focus ; la pastille suffit (sous-règle audit « pastille OU pré-sélection »).
- **`markForCheck` non requis** — `case-file-detail` n'est pas en `ChangeDetectionStrategy.OnPush` ; le `@Output` du panel est traité par la CD Angular standard. Si le composant passait OnPush ultérieurement, injecter `ChangeDetectorRef` + `markForCheck()` dans le handler.
