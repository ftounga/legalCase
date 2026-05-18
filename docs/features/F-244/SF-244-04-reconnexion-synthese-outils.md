# Mini-spec — F-244 / SF-244-04 — Reconnexion synthèse ↔ outils : point d'entrée bidirectionnel

## Identifiant
`F-244 / SF-244-04`

## Feature parente
`F-244` — Refonte de l'architecture de l'information de l'espace décisionnel du détail dossier

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
`feat/SF-244-04-reconnexion-synthese-outils`

---

## Objectif

Établir un point d'entrée explicite **dans les deux sens** entre l'écran de synthèse (`/case-files/:id/synthesis`) et l'espace décisionnel du détail dossier (onglet « Décision »), pour que le contrôle de cohérence F-IA-03 ne force plus un aller-retour aveugle.

---

## Comportement attendu

### Cas nominal

**Sens synthèse → outils** (le manquant) :
- L'en-tête de l'écran de synthèse (`/case-files/:id/synthesis`) affiche, à côté des actions existantes (« Retour au dossier », « Exporter PDF », « Exporter (.docx) »), un bouton **« Outils décisionnels »** (icône `build`).
- Un clic navigue vers `/case-files/:id?section=decision` — le détail dossier s'ouvre alors **directement sur l'onglet « Décision »**, scrollé sur le panel d'outils (`section-outils-decisionnels`, ancre posée par SF-244-03).

**Sens outils → synthèse** (renforcement) :
- L'onglet « Décision » du détail dossier affiche, dans le bandeau de couplage existant (SF-244-02), un lien **« Voir la synthèse »** (icône `summarize`) vers `/case-files/:id/synthesis`.
- Ce lien n'est affiché que si une synthèse existe (`synthesis() !== null`) — sinon il n'y a rien à consulter.

**Routing `?section=decision`** :
- `case-file-detail` étend la résolution du query param `?section=` : `decision` → bascule sur `TAB_DECISION` (2) + scroll vers l'ancre `section-outils-decisionnels`.
- Le mécanisme réutilise l'infrastructure `?section=` existante (SF-IA-03-19 / SF-244-01) — aucune route Angular nouvelle.

### Cas d'erreur / cas limites

| Situation | Comportement attendu |
|-----------|---------------------|
| Synthèse absente, sur l'onglet Décision | le lien « Voir la synthèse » du bandeau de couplage est masqué |
| `?section=decision` sur un dossier sans outils | onglet Décision affiché, panel d'outils en empty state ; aucune erreur |
| `?section=decision` + dossier en chargement | bascule d'onglet appliquée dès que `caseFile()` est résolu (scroll en retry, infra `scrollAndHighlight` existante) |
| Valeur `?section=` inconnue | comportement inchangé (aucune bascule — `target` reste `null`) |
| Synthèse en cours de génération (route synthèse) | le bouton « Outils décisionnels » reste actif — les outils sont consultables indépendamment |

---

## Analyse de cohérence transversale

### Périmètres scannés

- **Autres outils métier** : non concerné — SF-244-04 ne touche aucune logique d'outil décisionnel. Elle ajoute deux liens de navigation et étend une résolution de query param.
- **Autres pays / domaines** : transversal (FR/BE × 3 domaines) — `synthesis` et `case-file-detail` sont des composants uniques.
- **Autres UI patterns** : le bouton « Outils décisionnels » de l'en-tête synthèse réutilise `mat-stroked-button` + `mat-icon`, à l'identique des boutons d'export voisins. Le lien « Voir la synthèse » du bandeau de couplage réutilise le pattern `<a routerLink>` déjà présent dans l'onglet Analyse (« Voir la synthèse »). Aucun nouveau pattern partagé, aucun composant `shared/`.
- **Autres flows transversaux** : navigation interne modifiée (2 liens + extension `?section=`). Auth / workspace / plans : non touchés.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Logique des outils décisionnels | Non | aucun calcul touché |
| `synthesis` (écran synthèse) | Oui | bouton « Outils décisionnels » ajouté dans l'en-tête |
| `case-file-detail` | Oui | extension `?section=decision` + lien « Voir la synthèse » dans le bandeau de couplage |
| `case-dashboard-stepper` | Non | l'étape « Synthèse » (SF-244-03) couvre déjà le sens dossier → synthèse via le stepper ; SF-244-04 ajoute le sens inverse + un point d'entrée depuis l'écran synthèse lui-même |
| Route Angular | Non | aucune route nouvelle — réutilisation de `?section=` |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (`synthesis` + `case-file-detail` traités ici ; aucune cible résiduelle).

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF-244-04 ne crée ni ne modifie aucun composant décisionnel `<app-XXX-section>` et ne consomme aucun endpoint décisionnel. Elle ajoute deux liens de navigation bidirectionnels et étend la résolution du query param `?section=`. Aucun pré-fill IA ni validation F-IA-03 n'est porté par cette SF — elle facilite seulement le parcours de l'avocat qui *effectue* le contrôle F-IA-03 entre les deux écrans.

---

## Critères d'acceptation

- [ ] L'en-tête de `/case-files/:id/synthesis` affiche un bouton « Outils décisionnels » (icône `build`).
- [ ] Un clic sur ce bouton navigue vers `/case-files/:id?section=decision`.
- [ ] `/case-files/:id?section=decision` ouvre le détail dossier sur l'onglet « Décision » (`selectedTabIndex` = 2) et scrolle vers `section-outils-decisionnels`.
- [ ] Le bandeau de couplage de l'onglet « Décision » affiche un lien « Voir la synthèse » vers `/case-files/:id/synthesis` quand une synthèse existe.
- [ ] Le lien « Voir la synthèse » est masqué quand `synthesis()` est nulle.
- [ ] Une valeur `?section=` inconnue ne déclenche aucune bascule (comportement inchangé).
- [ ] Aucune régression de l'en-tête synthèse ni du bandeau de couplage SF-244-02.
- [ ] Tests Jest `synthesis` + `case-file-detail` adaptés et verts ; `npm run build` prod OK.
- [ ] Smoke tests E2E verts (parcours dossier / synthèse inchangés).

---

## Périmètre

### Hors scope (explicite)

- État terminal / « générer les conclusions » → relève de F-98 (ajustement 4 hors périmètre F-244).
- Toute modification de la logique métier d'un outil, du tableau de bord ou de la synthèse.
- Onglets routés / deeplinkables par URL propre (`/case-files/:id/decision`) — on réutilise `?section=`, pas de route nouvelle.
- Synchronisation d'état temps réel entre les deux écrans (ils restent deux routes distinctes).
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

- `synthesis.component.html` — bouton « Outils décisionnels » ajouté dans `.header-top-row`, à côté des boutons d'export.
- `synthesis.component.ts` — aucune logique nouvelle : le bouton utilise `[routerLink]` + `[queryParams]` (navigation déclarative).
- `case-file-detail.component.ts` — résolution `?section=` étendue avec la branche `decision` → `{ tab: TAB_DECISION, anchor: 'section-outils-decisionnels' }`.
- `case-file-detail.component.html` — lien « Voir la synthèse » ajouté dans le bandeau de couplage `.decision-coupling-hint` de l'onglet « Décision », gardé par `@if (synthesis())`.
- `case-file-detail.component.scss` — style du lien dans le bandeau de couplage (le bandeau existe déjà — SF-244-02).

### Décision technique

- **Réutilisation de `?section=`** plutôt qu'une route Angular dédiée : l'infrastructure `?section=` existe déjà (SF-IA-03-19, étendue par SF-244-01) et gère la bascule d'onglet + scroll d'ancre. Une route propre `/case-files/:id/decision` introduirait du routing d'onglet, hors scope F-244 (les onglets sont en état UI depuis SF-244-01). Conséquence assumée : le deep-link passe par un query param, pas une URL « propre ».
- **Lien « Voir la synthèse » dans le bandeau de couplage existant** : pas de nouveau bloc — on enrichit le bandeau SF-244-02, ce qui respecte l'invariant anti-surcharge (aucun nouveau bloc primaire autonome).
- **Bouton synthèse 100 % déclaratif** : `[routerLink]` + `[queryParams]`, aucune méthode TS — pas de risque CD/`subscribe`.

---

## Valeurs initiales

Aucune entité créée — SF de navigation pure.

---

## Plan de test

### Tests unitaires (Jest)

**`synthesis.component.spec.ts`**
- [ ] L'en-tête affiche un bouton « Outils décisionnels » avec `routerLink` vers `/case-files/:id` et `queryParams { section: 'decision' }`.

**`case-file-detail.component.spec.ts`**
- [ ] `?section=decision` → `selectedTabIndex` vaut `TAB_DECISION` (2).
- [ ] Le bandeau de couplage affiche le lien « Voir la synthèse » quand `synthesis()` est non nulle.
- [ ] Le lien « Voir la synthèse » est absent quand `synthesis()` est nulle.
- [ ] `?section=` avec une valeur inconnue ne change pas l'onglet (reste `TAB_DOSSIER`).

### Tests d'intégration
Non applicable — SF purement frontend, aucun endpoint.

### Isolation workspace
- [x] Non applicable — SF de navigation ; aucun accès données nouveau.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing frontend** — un bouton de navigation ajouté sur l'écran synthèse, un lien ajouté sur l'onglet Décision, et la résolution du query param `?section=` étendue. ⚠️ Précision : **aucune route Angular, aucun guard, aucune redirection** n'est ajouté/modifié — réutilisation du query param `?section=` existant. La case est cochée car la navigation perçue change → impose l'analyse d'impact + smoke tests E2E.
- Auth / Principal, Workspace context, Plans / limites : non touchés.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `synthesis` | un bouton additif dans l'en-tête — n'altère pas les boutons existants | tests Jest synthesis (présence + routerLink) ; suites existantes inchangées |
| `case-file-detail` | branche `?section=decision` additive ; lien additif dans le bandeau de couplage | tests Jest detail (`?section=decision`, lien conditionnel) |
| résolution `?section=` (SF-IA-03-19 / SF-244-01) | branche ajoutée — `documents` / `analyse` / `deadlines` inchangées | tests Jest existants `?section=` inchangés |
| route `/case-files/:id/synthesis` et `/case-files/:id` | inchangées — réutilisées | smoke E2E navigation |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/navigation.spec.ts` + tout parcours touchant `/case-files/:id` et `/case-files/:id/synthesis`. `cd e2e && npm test` **obligatoire avant push** (préoccupation transversale navigation cochée).

---

## Dépendances

### Subfeatures bloquantes

- `SF-244-01` — `done` (PR #980) — structure en onglets + résolution `?section=`.
- `SF-244-02` — `done` (PR #992) — bandeau de couplage de l'onglet Décision (le lien « Voir la synthèse » s'y greffe).
- `SF-244-03` — `done` (PR #996) — ancre `section-outils-decisionnels` (cible de `?section=decision`).

### Questions ouvertes impactées
- [ ] Aucune — vérifié : aucun sujet `docs/OPEN_QUESTIONS.md` relatif à la navigation synthèse ↔ dossier.

---

## Notes et décisions

- **`?section=decision` plutôt qu'une route propre** — réutilise l'infra `?section=` existante ; pas de routing d'onglet (hors scope F-244).
- **Lien « Voir la synthèse » greffé sur le bandeau de couplage SF-244-02** — pas de nouveau bloc, respect de l'invariant anti-surcharge.
- **Sens dossier → synthèse déjà partiellement couvert** — l'onglet Analyse a « Voir la synthèse » et le stepper a l'étape « Synthèse » (SF-244-03) ; SF-244-04 ajoute le point d'entrée manquant *depuis l'écran synthèse* et *depuis l'onglet Décision*, fermant la boucle bidirectionnelle.
