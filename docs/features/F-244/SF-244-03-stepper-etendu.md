# Mini-spec — F-244 / SF-244-03 — `case-dashboard-stepper` étendu : lisibilité de la séquence du parcours

## Identifiant
`F-244 / SF-244-03`

## Feature parente
`F-244` — Refonte de l'architecture de l'information de l'espace décisionnel du détail dossier

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
`feat/SF-244-03-stepper-etendu`

---

## Objectif

Étendre le `case-dashboard-stepper` du détail dossier aux étapes **Synthèse → Outils décisionnels → Tableau de bord décisionnel** pour que la séquence du parcours métier soit lisible de bout en bout (l'audit pointe que le stepper omet précisément ces 3 étapes).

---

## Comportement attendu

### Cas nominal

Le `case-dashboard-stepper` (en-tête de `/case-files/:id`, hors onglets) affiche désormais **8 étapes** dans l'ordre du parcours réel :

1. **Documents** (existant) — onglet Dossier
2. **Analyse du dossier** (existant) — onglet Analyse
3. **Synthèse** *(nouveau)* — navigue vers la route synthèse `/case-files/:id/synthesis`
4. **Questions complémentaires** (existant) — route synthèse
5. **Outils décisionnels** *(nouveau)* — onglet Décision ; porte un badge `auto_awesome` avec le total de champs pré-remplis IA (= `decisionPrefillTotal`, le même compteur que le badge d'onglet de SF-244-02)
6. **Tableau de bord décisionnel** *(nouveau)* — onglet Décision, scroll vers l'ancre du tableau de bord
7. **Délais légaux** (existant) — onglet Suivi
8. **Pièces manquantes** (existant) — route synthèse

- Statuts des 3 nouvelles étapes :
  - **Synthèse** : `done` si une synthèse existe (`synthesis !== null`), `in_progress` pendant un run d'analyse, sinon `pending`.
  - **Outils décisionnels** : `pending` tant qu'aucune synthèse n'existe (les outils ne sont pertinents qu'après analyse) ; sinon **point d'entrée navigable** — l'étape reste cliquable (statut non-`done`), elle n'a pas d'état « terminé » strict (l'avocat décide quels outils renseigner). Son `detail` indique le nombre de champs pré-remplis quand > 0.
  - **Tableau de bord décisionnel** : `pending` tant qu'aucune synthèse n'existe ; sinon point d'entrée navigable vers la carte tableau de bord (statut non-`done`).
- Un clic sur une nouvelle étape :
  - **Synthèse** → navigation route synthèse (étape sans `tabIndex` ni `anchorId` → navigation, comportement `case-dashboard-stepper` existant).
  - **Outils décisionnels** → `stepActivated` avec `tabIndex = TAB_DECISION` (2) et `anchorId` = ancre du panel d'outils → bascule sur l'onglet Décision et scrolle vers les outils.
  - **Tableau de bord** → `stepActivated` avec `tabIndex = TAB_DECISION` (2) et `anchorId` = ancre du tableau de bord → bascule sur l'onglet Décision et scrolle vers le tableau de bord.
- Le `case-file-detail` ajoute les ancres DOM correspondantes : `id="section-outils-decisionnels"` sur le wrapper du panel d'outils, `id="section-tableau-bord"` sur la carte tableau de bord.
- Le badge `auto_awesome` de l'étape « Outils décisionnels » est masqué quand le total vaut 0 (cohérent avec le badge d'onglet SF-244-02 : un onglet/une étape fermé·e ne masque pas le travail de l'IA).

### Cas d'erreur / cas limites

| Situation | Comportement attendu |
|-----------|---------------------|
| Aucune synthèse (analyse pas lancée) | étapes Synthèse / Outils / Tableau de bord en `pending`, cliquables ; badge prefill masqué |
| Analyse en cours | étape Synthèse en `in_progress` ; Outils / Tableau de bord restent `pending` |
| Total prefill = 0 | étape « Outils décisionnels » sans badge ni `detail` prefill |
| Stepper sur viewport étroit (< 600 px) | empilement vertical (média query existante du stepper) — 8 étapes empilées, utilisable |
| Clic sur l'étape Synthèse | navigation route, pas de `stepActivated` émis |
| Clic Outils / Tableau de bord | bascule onglet Décision + scroll ancre (via `onStepActivated` existant) |

---

## Analyse de cohérence transversale

### Périmètres scannés

- **Autres outils métier** : non concerné — SF-244-03 ne touche aucune logique d'outil. Le stepper liste des étapes de parcours, il n'exécute aucun calcul décisionnel.
- **Autres pays / domaines** : le stepper est transversal (FR/BE × 3 domaines), `case-dashboard-stepper` et `case-file-detail` étant des composants uniques.
- **Autres UI patterns** : le badge `auto_awesome` de l'étape « Outils » réutilise le compteur agrégé déjà calculé par SF-244-02 (`decisionPrefillTotal`). Aucun nouveau pattern partagé ; le `DashboardStep` reçoit un champ optionnel `prefillCount` — interface locale du stepper, pas un DTO réutilisable.
- **Autres flows transversaux** : navigation interne de l'écran modifiée (3 étapes de stepper en plus, 2 émettent `stepActivated`). Auth / workspace / plans : non touchés.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Logique des outils décisionnels | Non | le stepper n'exécute aucun calcul |
| `case-dashboard-stepper` | Oui | étendu dans cette SF (3 étapes + champ `prefillCount`) |
| `case-file-detail` | Oui | `dashboardSteps()` étendu + 2 ancres DOM ajoutées |
| `case-dashboard` (synthèse) | Non | l'étape Synthèse navigue vers la route existante, inchangée |
| Badge prefill agrégé (SF-244-02) | Oui | réutilisé tel quel (`decisionPrefillTotal`), pas de recalcul |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (`case-dashboard-stepper` + `case-file-detail` traités ici ; aucune cible résiduelle).

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF-244-03 ne crée ni ne modifie aucun composant décisionnel `<app-XXX-section>` et ne consomme aucun endpoint décisionnel. Elle étend un composant de navigation (`case-dashboard-stepper`) avec 3 étapes de parcours et y remonte un compteur de pré-fill déjà agrégé par SF-244-02. Aucun pré-fill IA ni validation F-IA-03 n'est porté par cette SF.

---

## Critères d'acceptation

- [ ] Le `case-dashboard-stepper` affiche 8 étapes dans l'ordre : Documents, Analyse, Synthèse, Questions, Outils décisionnels, Tableau de bord, Délais, Pièces manquantes.
- [ ] L'étape « Synthèse » a le statut `done` si une synthèse existe, `in_progress` pendant l'analyse, `pending` sinon ; un clic navigue vers `/case-files/:id/synthesis`.
- [ ] L'étape « Outils décisionnels » émet `stepActivated` avec `tabIndex = 2` et une `anchorId` au clic ; elle est `pending` sans synthèse.
- [ ] L'étape « Tableau de bord décisionnel » émet `stepActivated` avec `tabIndex = 2` et une `anchorId` au clic ; elle est `pending` sans synthèse.
- [ ] L'étape « Outils décisionnels » affiche un badge `auto_awesome` avec le total de prefill quand `decisionPrefillTotal > 0`, masqué sinon.
- [ ] `case-file-detail` expose les ancres `section-outils-decisionnels` (panel d'outils) et `section-tableau-bord` (carte tableau de bord) ; un clic d'étape bascule sur l'onglet Décision et scrolle vers la cible.
- [ ] Aucune régression des 5 étapes existantes ni de leur navigation.
- [ ] Tests Jest `case-dashboard-stepper` + `case-file-detail` adaptés et verts ; `npm run build` prod OK.
- [ ] Smoke tests E2E verts (parcours dossier inchangé).

---

## Périmètre

### Hors scope (explicite)

- Reconnexion synthèse ↔ outils (point d'entrée bidirectionnel depuis l'écran synthèse) → **SF-244-04**.
- Toute modification de la logique métier d'un outil ou du tableau de bord.
- État terminal / « générer les conclusions » sur le stepper → relève de F-98 (ajustement 4 hors périmètre F-244).
- Restyle visuel du stepper hors ajout du badge prefill.
- Onglets routés / deeplinkables — inchangé.
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

- `case-dashboard-stepper.component.ts` — interface `DashboardStep` enrichie d'un champ optionnel `prefillCount?: number | null`.
- `case-dashboard-stepper.component.html` — rendu d'un badge `auto_awesome` sur une étape quand `prefillCount > 0`.
- `case-dashboard-stepper.component.scss` — style du badge `auto_awesome` du stepper (cohérent avec le badge d'onglet SF-244-02).
- `case-file-detail.component.ts` — `dashboardSteps()` étendu de 5 à 8 étapes ; l'étape « Outils décisionnels » porte `prefillCount: this.decisionPrefillTotal()`.
- `case-file-detail.component.html` — ancres `id="section-outils-decisionnels"` (wrapper colonne de saisie) et `id="section-tableau-bord"` (carte `.decisional-summary-panel`).

### Décision technique

- **Étapes Outils / Tableau de bord sans état `done` strict** : ces 2 étapes sont des **points d'entrée navigables**, pas des jalons à compléter (l'avocat décide quels outils renseigner — il n'y a pas de critère objectif « outils terminés »). Elles restent donc `pending` une fois la synthèse disponible, ce qui les garde cliquables (le stepper ne rend cliquable que les étapes non-`done`). Décision documentée — un état `done` les rendrait non navigables, contraire à l'objectif de lisibilité.
- **Badge prefill réutilisé, pas recalculé** : le compteur `decisionPrefillTotal` est déjà alimenté par l'`@Output` du panel (SF-244-02) ; le stepper le reçoit via `dashboardSteps()`. Une seule source de vérité, pas de divergence.
- **Ancres DOM** : ajoutées sur le wrapper de la colonne de saisie et sur la carte tableau de bord ; `onStepActivated` (SF-244-01) gère déjà la bascule d'onglet + scroll d'ancre.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `prefillCount` (étape Outils) | `0` | = `decisionPrefillTotal()`, recalculé à chaque changement du compteur agrégé |

---

## Plan de test

### Tests unitaires (Jest)

**`case-dashboard-stepper.component.spec.ts`**
- [ ] Un badge `auto_awesome` est rendu sur une étape dont `prefillCount > 0`.
- [ ] Aucun badge `auto_awesome` quand `prefillCount` vaut 0 ou `null`.

**`case-file-detail.component.spec.ts`**
- [ ] `dashboardSteps()` retourne 8 étapes avec les ids attendus dans l'ordre.
- [ ] L'étape « Synthèse » est `done` quand `synthesis()` est non nulle, `pending` sinon.
- [ ] Les étapes « Outils décisionnels » et « Tableau de bord décisionnel » portent `tabIndex = TAB_DECISION`.
- [ ] L'étape « Outils décisionnels » porte `prefillCount = decisionPrefillTotal()`.
- [ ] Les ancres `section-outils-decisionnels` et `section-tableau-bord` sont présentes dans l'onglet Décision.

### Tests d'intégration
Non applicable — SF purement frontend, aucun endpoint.

### Isolation workspace
- [x] Non applicable — SF de navigation/affichage ; aucun accès données nouveau.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing frontend** — le `case-dashboard-stepper` reçoit 3 étapes de parcours dont 2 émettent `stepActivated` (bascule d'onglet) et 1 navigue vers la route synthèse. ⚠️ Précision : **aucune route Angular, aucun guard, aucune redirection** n'est ajouté/modifié — l'étape Synthèse réutilise la route synthèse existante. La case est cochée car la navigation perçue change → impose l'analyse d'impact + smoke tests E2E.
- Auth / Principal, Workspace context, Plans / limites : non touchés.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `case-dashboard-stepper` | 3 étapes en plus, champ `prefillCount` additif optionnel | tests Jest stepper (badge) ; suites existantes inchangées |
| `case-file-detail` | `dashboardSteps()` passe de 5 à 8 entrées ; 2 ancres DOM ajoutées | tests Jest detail (8 étapes, ancres) |
| `onStepActivated` (SF-244-01) | réutilisé tel quel pour Outils / Tableau de bord | tests Jest existants SF-244-01 inchangés |
| route `/case-files/:id/synthesis` | l'étape Synthèse y navigue — route existante, inchangée | smoke E2E navigation |
| tests E2E `/case-files/:id` | le stepper a 3 `.step` en plus — aucun smoke ne compte les étapes | `cd e2e && npm test` avant push |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/navigation.spec.ts` + tout parcours touchant `/case-files/:id`. `cd e2e && npm test` **obligatoire avant push** (préoccupation transversale navigation cochée).

---

## Dépendances

### Subfeatures bloquantes

- `SF-244-01` — statut : `done` (PR #980) — structure en onglets + `StepActivation`.
- `SF-244-02` — statut : `done` (PR #992) — fournit `decisionPrefillTotal` réutilisé par le badge de l'étape « Outils ».

### Questions ouvertes impactées
- [ ] Aucune — vérifié : aucun sujet `docs/OPEN_QUESTIONS.md` relatif au stepper du dossier.

---

## Notes et décisions

- **Étapes Outils / Tableau de bord = points d'entrée, pas jalons** — restent `pending` (donc cliquables) une fois la synthèse disponible ; pas d'état `done` faute de critère objectif de complétion.
- **Badge prefill = `decisionPrefillTotal` réutilisé** — source unique partagée avec le badge d'onglet SF-244-02.
- **8 étapes sur le stepper** — densité acceptable : le stepper est `overflow-x: auto` sur large écran et empilé < 600 px (média query existante).
