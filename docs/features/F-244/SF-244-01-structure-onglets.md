# Mini-spec — F-244 / SF-244-01 — Structure en onglets du détail dossier (socle)

## Identifiant
`F-244 / SF-244-01`

## Feature parente
`F-244` — Refonte de l'architecture de l'information de l'espace décisionnel du détail dossier

## Statut
`draft`

## Date de création
2026-05-17

## Branche Git
`feat/SF-244-01-structure-onglets`

---

## Objectif

Remplacer la page à scroll unique en 2 colonnes de l'écran détail dossier par une structure en 4 onglets par phase du parcours (Dossier / Analyse / Décision / Suivi), **sans modifier le comportement des blocs déplacés**.

---

## Comportement attendu

### Cas nominal

À l'ouverture d'un dossier (`/case-files/:id`), l'écran affiche :

- un **en-tête fixe** (titre, actions Export ZIP / Clôturer / Supprimer, `timer-widget`, `case-dashboard-stepper`) — toujours visible, hors onglets ;
- un **`mat-tab-group`** à 4 onglets :
  1. **Dossier** — `detail-card` (métadonnées), `stats-card`, section Documents (repliable).
  2. **Analyse** — section Analyse + bouton d'analyse, `analysis-pipeline`, section Synthèse (lien), bandeau questions, spinner analyse globale.
  3. **Décision** — `decisional-tools-panel` + `decisional-summary-panel` (`case-dashboard`), affichés tels quels (leur regroupement fin = SF-244-02).
  4. **Suivi** — `case-deadlines-section`, `case-notes-section`.
- L'onglet **Dossier** est sélectionné par défaut.
- Chaque bloc déplacé **conserve son comportement actuel** (collapsible Documents, upload, lancement d'analyse, lien synthèse, etc.) — aucune régression fonctionnelle.
- Un clic sur une étape du `case-dashboard-stepper` **bascule sur l'onglet** contenant la cible (le scroll d'ancre seul ne fonctionne plus une fois les blocs répartis en onglets).

### Cas d'erreur / cas limites

| Situation | Comportement attendu |
|-----------|---------------------|
| Dossier en chargement | écran de chargement, pas d'onglets tant que `caseFile()` est nul |
| Dossier introuvable | message « Dossier introuvable » (inchangé) |
| Dossier sans document | onglet Dossier : empty state Documents ; onglet Analyse : placeholder ; aucun onglet vide/cassé |
| Viewport mobile (<1024px) | `mat-tab-group` reste utilisable (onglets Material scrollables) |
| Étape du stepper pointant un bloc d'un autre onglet | bascule d'onglet (+ scroll ancre si applicable) |

---

## Analyse de cohérence transversale

Périmètres scannés :
- **Autres outils métier** : non concerné — SF-244-01 ne touche aucun outil décisionnel (déplacement de conteneur uniquement).
- **Autres pays / domaines** : la structure en onglets est transversale, identique FR/BE × 3 domaines (`case-file-detail` est un composant unique).
- **Autres UI patterns** : `mat-tab-group` est le composant Angular Material standard — **aucun pattern partagé custom introduit**, aucune dette de convergence. Pas de composant `shared/`, pas de service, pas d'endpoint nouveau.
- **Autres flows transversaux** : navigation interne de l'écran modifiée (cf. Analyse d'impact). Auth / workspace / plans : non touchés.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Outils décisionnels | Non | déplacés tels quels, zéro modification de code interne |
| Autres écrans tabbables (synthèse, simulators) | Non | hors périmètre F-244 (spécifique `case-file-detail`) |
| `case-dashboard-stepper` | Oui | adapté dans cette SF (bascule d'onglet) |

### Décision

- [x] Non applicable aux autres cibles (SF purement structurelle sur un composant unique) ; le seul composant adjacent impacté (`case-dashboard-stepper`) est traité dans cette SF.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF-244-01 est une SF structurelle ; elle ne crée ni ne modifie aucun composant décisionnel `<app-XXX-section>`. `decisional-tools-panel` et `case-dashboard` sont déplacés dans l'onglet Décision **sans modification de leur code interne**. Leur regroupement fin (alignement, badge prefill agrégé) relève de SF-244-02.

---

## Critères d'acceptation

- [ ] L'écran `/case-files/:id` affiche un `mat-tab-group` à 4 onglets : Dossier, Analyse, Décision, Suivi.
- [ ] L'en-tête (titre, actions, `timer-widget`, `case-dashboard-stepper`) reste affiché au-dessus des onglets quel que soit l'onglet actif.
- [ ] Chaque bloc existant est présent dans l'onglet attendu (cf. cas nominal) ; aucun bloc perdu.
- [ ] Le comportement de chaque bloc déplacé est inchangé (collapsible Documents, upload, lancement d'analyse, lien synthèse, bandeau questions, etc.).
- [ ] L'onglet Dossier est sélectionné par défaut à l'ouverture.
- [ ] Un clic sur une étape du `case-dashboard-stepper` bascule sur le bon onglet.
- [ ] Le rendu est utilisable sur viewport mobile (<1024px).
- [ ] `.detail-grid` (2 colonnes), `.col-left`, `.col-right` et tout résidu `.bottom-sections` sont retirés du DOM et du SCSS.
- [ ] Les suites de tests `case-file-detail` existantes sont adaptées et vertes ; `npm run build` prod OK.
- [ ] Smoke tests E2E verts (aucune régression du parcours dossier).

---

## Périmètre

### Hors scope (explicite)

- Regroupement fin / alignement outils ↔ tableau de bord dans l'onglet Décision, badge prefill agrégé → **SF-244-02**.
- Extension du `case-dashboard-stepper` (Synthèse → Outils → Tableau de bord) → **SF-244-03**. SF-244-01 se limite à **ne pas casser** le stepper actuel.
- Reconnexion synthèse ↔ outils → **SF-244-04**.
- Onglets routés / deeplinkables (`/case-files/:id/decision`) → hors scope (cf. Notes et décisions).
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

- `case-file-detail.component.html` — remplacement de `.detail-grid` (2 colonnes) par `<mat-tab-group>` à 4 `<mat-tab>` ; en-tête conservé au-dessus du groupe d'onglets.
- `case-file-detail.component.ts` — signal `selectedTabIndex`, méthode publique de bascule d'onglet appelée par le handler du stepper.
- `case-file-detail.component.scss` — suppression `.detail-grid` / `.col-left` / `.col-right` / `.bottom-sections` ; styles du conteneur d'onglets.
- `case-dashboard-stepper` — le clic d'étape demande une bascule d'onglet (en plus / à la place du scroll d'ancre).

### Décision technique

Onglets en **état UI** (`mat-tab-group` + signal `selectedIndex`), **non routés**. Justification : socle minimal, pas de complexité de routing ; le deeplink par onglet n'est pas un besoin exprimé. Conséquence assumée : ouverture toujours sur l'onglet Dossier, le bouton « précédent » du navigateur ne parcourt pas les onglets.

Rendu **non-lazy** des `mat-tab` (pas de `<ng-template matTabContent>`) : le contenu des 4 onglets est monté dès l'ouverture du dossier, afin de ne pas casser les composants qui s'initialisent à l'ouverture (pipeline d'analyse, SSE, polling). La lazy-load éventuelle = optimisation ultérieure, hors socle.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `selectedTabIndex` | 0 (onglet Dossier) | à chaque ouverture de dossier |

---

## Plan de test

### Tests unitaires (Jest — `case-file-detail.component.spec.ts`)

- [ ] Les 4 onglets sont rendus avec les bons libellés.
- [ ] Un sélecteur représentatif de chaque bloc clé est présent dans l'onglet attendu (un test par onglet).
- [ ] L'en-tête (titre + `case-dashboard-stepper`) est rendu **hors** du `mat-tab-group`.
- [ ] `selectedTabIndex` vaut 0 à l'initialisation.
- [ ] Un clic d'étape du `case-dashboard-stepper` met à jour `selectedTabIndex` vers l'onglet attendu.
- [ ] `.detail-grid` / `.bottom-sections` absents du DOM.
- [ ] Cas dossier sans document : onglets rendus, empty states affichés, aucune erreur.
- [ ] Adaptation des tests `case-file-detail` existants qui s'appuyaient sur la structure 2 colonnes.

### Tests d'intégration
Non applicable — SF purement frontend, aucun endpoint.

### Isolation workspace
- [x] Non applicable — SF de mise en page pure, aucun accès données nouveau ; les blocs déplacés conservent leur logique d'isolation existante.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing frontend** — la structure passe de scroll unique à onglets et le `case-dashboard-stepper` doit être adapté. ⚠️ Précision : **aucune route Angular, aucun guard, aucune redirection** n'est ajouté/modifié (onglets en état UI). La case est cochée car la navigation perçue par l'avocat change et un composant adjacent (stepper) est impacté → impose l'analyse d'impact et les smoke tests E2E.
- Auth / Principal, Workspace context, Plans / limites : non touchés.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `case-dashboard-stepper` | son clic d'étape doit basculer d'onglet (le scroll d'ancre seul ne marche plus) | test Jest clic étape → `selectedTabIndex` ; smoke E2E |
| child components (documents, deadlines, notes, `decisional-tools-panel`, `case-dashboard`, `analysis-pipeline`, synthèse) | déplacés dans des onglets — pilotés par `@Input`, doivent rendre identiquement | tests Jest présence par onglet ; suites existantes de chaque composant inchangées |
| tests E2E interagissant avec le détail dossier | sélection d'éléments désormais dans un onglet | `cd e2e && npm test` avant push |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/*` — tout parcours touchant `/case-files/:id` (upload, lancement d'analyse, accès synthèse). `cd e2e && npm test` **obligatoire avant push** (préoccupation transversale navigation cochée).

---

## Dépendances

### Subfeatures bloquantes
Aucune. SF-244-01 est le socle ; SF-244-02 / 03 / 04 en dépendent.

### Questions ouvertes impactées
- [ ] Aucune — vérifié : pas de sujet `docs/OPEN_QUESTIONS.md` relatif à l'IA de l'écran dossier.

---

## Notes et décisions

- **Onglets en état UI, non routés** — choix de socle minimal ; deeplink par onglet éventuellement V2.
- **Rendu non-lazy des `mat-tab`** — préserve l'initialisation des composants à l'ouverture du dossier (SSE analyse, polling) ; un rendu lazy casserait le suivi d'analyse si l'avocat n'est pas sur l'onglet Analyse.
- **Stepper non encore étendu** (SF-244-03) — SF-244-01 se limite à le faire basculer d'onglet pour qu'il ne soit pas cassé par la restructuration.
