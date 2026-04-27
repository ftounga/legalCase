# SF-168-01 — Harmonisation typographique cards décisionnelles legacy

**Feature parente** : F-168 — Harmonisation typographique cards décisionnelles
**Type** : frontend
**Branche** : `feat/SF-168-01-harmonisation-typo-cards`
**Effort estimé** : ~30 min

---

## Objectif (1 phrase)

Migrer les 4 composants décisionnels legacy (`case-deadlines-section`, `prudhome-fiche-section`, `anciennete-section`, `rupture-conv-section`) du pattern HTML/SCSS legacy `xxx-section / section-header / section-title (font-weight: 600 + text-transform: uppercase)` vers le **template canonique** `td-section / td-header / td-title (font-weight: 700, titre en MAJUSCULES dans le HTML)` afin que toutes les cards du panel F-IA-04 partagent la même signature visuelle.

---

## Contexte

Le panel F-IA-04 (`<app-decisional-tools-panel>`) affiche aujourd'hui des cards qui suivent **deux conventions HTML/SCSS distinctes** :

| Famille | Structure HTML | Titre CSS | Rendu |
|---|---|---|---|
| **Canonique** (référence : `travail-dissimule-section`, `rappel-salaire-section`, `transaction-section`, `non-concurrence-section`, etc.) | `<section class="td-section"><header class="td-header"><span class="td-title">INDEMNITÉ TRAVAIL DISSIMULÉ</span></header>` | `font-weight: 700; font-size: 14px; letter-spacing: 0.04em;` (texte directement en MAJUSCULES dans le HTML) | **GRAS ÉPAIS** |
| **Legacy** (les 4 cibles de cette SF) | `<div class="xxx-section"><button class="section-header"><span class="section-title">Validité de la rupture conventionnelle</span></button>` | `font-weight: 600; letter-spacing: 0.5px; text-transform: uppercase;` | semi-gras |

L'écart visuel est frappant côte-à-côte sur le même panel — détecté par le PO le 2026-04-27.

Cette SF supprime la dette de cohérence avant **F-169** (grid 2 colonnes par thème) qui s'appuiera sur des cards visuellement homogènes.

---

## Comportement nominal

Pour chaque composant migré :
1. La card affiche un header au pattern canonique : `<mat-icon class="td-icon">` + `<span class="td-title">TITRE EN MAJUSCULES</span>` + (badge optionnel `<span class="td-chip">`) + `<mat-icon class="td-toggle">`.
2. Le clic sur le header (ou Entrée au focus) bascule l'état `collapsed()` (signal préservé).
3. Le corps `<div class="td-body">` s'affiche conditionnellement avec `@if (!collapsed())`.
4. **Aucun changement de comportement métier** : signaux, formulaires, calculs, badges de verdict — tout reste à l'identique.
5. Les classes SCSS `.td-section`, `.td-header`, `.td-icon`, `.td-title`, `.td-chip`, `.td-toggle`, `.td-body` sont **dupliquées** dans chaque SCSS (pattern existant — pas de mixin partagé livré dans cette SF, ce serait une autre SF d'extraction si décidée plus tard).

### Mapping titres

| Composant | Titre actuel (casse mixte) | Titre cible (MAJUSCULES dans HTML) | Icône conservée |
|---|---|---|---|
| `case-deadlines-section` | "Délais légaux" | **"DÉLAIS LÉGAUX"** | `schedule` (à confirmer à la lecture) |
| `prudhome-fiche-section` | "Fiche prud'homale" | **"FICHE PRUD'HOMALE"** | `gavel` |
| `anciennete-section` | "Ancienneté et congés" | **"ANCIENNETÉ ET CONGÉS"** | `calendar_month` |
| `rupture-conv-section` | "Validité de la rupture conventionnelle" | **"VALIDITÉ DE LA RUPTURE CONVENTIONNELLE"** | `gavel` |

### Cas d'erreur

- **Aucun cas d'erreur métier nouveau** : la SF ne touche pas la logique métier, ne modifie aucun service, n'appelle aucun endpoint, ne change aucun input/output de composant.
- Risque résiduel : régression visuelle sur ces 4 cards (mauvaise migration HTML qui casse l'affichage du body). Mitigation = tests Jest existants + smoke visuel staging.

---

## Critères d'acceptation vérifiables

1. Les 4 fichiers `*.component.html` utilisent **`<section class="td-section">`** comme racine (et plus `<div class="xxx-section">`).
2. Les 4 fichiers `*.component.html` utilisent **`<header class="td-header">`** comme zone cliquable (et plus `<button class="section-header">`).
3. Les 4 fichiers `*.component.html` ont leur titre en **MAJUSCULES dans le HTML** (pas via CSS `text-transform: uppercase`) à l'intérieur d'un `<span class="td-title">`.
4. Les 4 fichiers `*.component.scss` définissent `.td-title { font-weight: 700; font-size: 14px; letter-spacing: 0.04em; }` (les valeurs alignées sur `travail-dissimule-section.component.scss` lignes 27-34) sans `text-transform: uppercase`.
5. Le clic sur `.td-header` continue de basculer `collapsed()` (test Jest existant doit rester vert).
6. Le clavier `Entrée` au focus du header bascule aussi `collapsed()` (`(keydown.enter)="toggleCollapse()"` ajouté).
7. Le header est focusable au clavier : `tabindex="0"`, `role="button"`, `[attr.aria-expanded]="!collapsed()"`, `aria-controls="td-body"`.
8. Tous les tests Jest existants des 4 composants restent verts (suite frontend complète passée).
9. Aucune régression visuelle sur les cards canoniques déjà conformes (`travail-dissimule-section`, etc.) — vérifié à l'œil sur staging post-merge.
10. La taille du SCSS de chaque composant ne dépasse pas le budget Angular `anyComponentStyle: 8kB warning / 24kB error` (déjà respecté avant migration).

---

## Plan de test minimal

### Tests unitaires (Jest)

Pour chaque composant migré, vérifier que les tests existants restent verts. Si un test interroge `By.css('.section-header')` ou `By.css('.section-title')`, l'adapter à `By.css('.td-header')` et `By.css('.td-title')` respectivement (ce sont les seules adaptations attendues).

**Liste des assertions à conserver** (pré-existantes) :
- `case-deadlines-section.component.spec.ts` : test de toggle `collapsed()`, test d'affichage du badge délai dépassé
- `prudhome-fiche-section.component.spec.ts` : test de soumission formulaire, test de validation
- `anciennete-section.component.spec.ts` : test de calcul ancienneté + badge années/mois
- `rupture-conv-section.component.spec.ts` : test affichage verdict + badge couleur

### Tests d'intégration

Aucun test d'intégration spécifique requis — la SF est purement visuelle et le contrat des composants ne change pas.

### Tests d'isolation workspace

Non applicable (composants frontend purs, pas d'accès données).

### Smoke test E2E

Non requis (aucune préoccupation transversale cochée). Vérification visuelle staging suffit.

### Vérification manuelle staging

Sur dossier travail FR avec analyse complète :
- [ ] Les 4 cards "Délais légaux", "Fiche prud'homale", "Ancienneté et congés", "Validité RC" affichent leur titre **en MAJUSCULES en gras épais**, identique aux autres cards.
- [ ] Le toggle expand/collapse fonctionne (clic + clavier Entrée).
- [ ] Les badges secondaires (verdict couleur sur rupture-conv, badge années/mois sur anciennete) restent visibles et conservent leur style.
- [ ] La landing page (`/`) reste impeccable (vérification non-régression du fix SF-165-08 : header fixed, sections 96px).

---

## Tables / endpoints / composants impactés

### Backend
**Aucun.** La SF est 100 % frontend.

### Frontend — composants modifiés

| Fichier | Type de modification |
|---|---|
| `frontend/src/app/case-files/case-deadlines-section/case-deadlines-section.component.html` | Refactor structure : `xxx-section / section-header / section-title` → `td-section / td-header / td-title` + titre MAJUSCULES |
| `frontend/src/app/case-files/case-deadlines-section/case-deadlines-section.component.scss` | Remplacement classes legacy par `.td-*` alignées sur `travail-dissimule-section.component.scss` |
| `frontend/src/app/case-files/case-deadlines-section/case-deadlines-section.component.spec.ts` | Adapter sélecteurs `.section-*` → `.td-*` si présents |
| `frontend/src/app/case-files/prudhome-fiche-section/prudhome-fiche-section.component.{html,scss,spec.ts}` | Idem |
| `frontend/src/app/case-files/anciennete-section/anciennete-section.component.{html,scss,spec.ts}` | Idem |
| `frontend/src/app/case-files/rupture-conv-section/rupture-conv-section.component.{html,scss,spec.ts}` | Idem |

### Composants **non** impactés
- Tous les autres composants décisionnels déjà conformes au pattern canonique (`travail-dissimule-section`, `rappel-salaire-section`, `non-concurrence-section`, `transaction-section`, `protection-rp-section`, `at-mp-section`, `refere-prudhomal-section`, `contestation-are-section`, `indemnite-conges-section`, `indemnite-preavis-section`, `documents-fin-contrat-section`, `rupture-conv-indemnite-section`, etc.).
- `decisional-tools-panel.component` (panel parent) — sa structure reste inchangée, la refonte panel = F-169 SF distincte.

### Endpoints / API
**Aucun.**

### Migrations / DB
**Aucune.**

---

## Hors périmètre

- ❌ Refonte du `decisional-tools-panel.component` (grid, groupement par thème) → **F-169 SF-169-01**.
- ❌ Bandeau dashboard synthétique avec verdicts agrégés → **F-167**.
- ❌ Section "Documents" en accordéon → **F-170 SF-170-01**.
- ❌ Extraction du pattern `td-*` en mixin SCSS partagé ou directive Angular → potentielle SF de refactor ultérieure si la duplication devient gênante (>20 composants).
- ❌ Refonte du comportement métier des composants (validation, calculs, formulaires).
- ❌ Migration des autres composants ne faisant pas partie du panel décisionnel (header app, sidebar, etc.).
- ❌ Changement d'icône, de couleur, de palette.

---

## Analyse de cohérence transversale

| Cible | Statut | Justification |
|---|---|---|
| **Autres outils décisionnels Travail FR** déjà conformes | ✅ aucune action | `travail-dissimule-section`, `rappel-salaire-section`, `non-concurrence-section`, `transaction-section`, `protection-rp-section`, `at-mp-section`, `refere-prudhomal-section`, `contestation-are-section`, `indemnite-conges-section`, `indemnite-preavis-section`, `documents-fin-contrat-section`, `rupture-conv-indemnite-section`, `prescription-action-section` — tous déjà au pattern canonique. Vérifié par grep `td-section` dans les `*.component.html`. |
| **Outils décisionnels Famille FR + BE** | ✅ aucune action | Composants F-FA-* livrés via les vagues 2026-04-24 sont au pattern canonique (références : `obligation-alimentaire-section`, `desunion-section`, `partage-successoral-be-section`, etc.). |
| **Outils décisionnels Immigration FR + BE** | ✅ aucune action | Composants F-IM-* idem (références : `immigration-title-decision-section`, `recours-OQTF-section`, `naturalisation-section`, etc.). |
| **Section "Documents" sur `case-file-detail`** | 🔵 SF parallèle | Pattern legacy `<h2 class="section-title">Documents</h2>` + `<mat-card class="docs-card">` à migrer aussi → **F-170 SF-170-01**. Pas inclus dans cette SF pour rester courte et atomique. |
| **Section "Synthèse"** sur `case-file-detail` | 🟢 backlog | Le bloc synthèse a son propre pattern d'affichage (page longue). Refonte prévue dans **F-162** (V8+, "Refonte écran synthèse"). |
| **Pays Belgique** | ✅ aucune action | Les 4 composants ciblés sont strictement FR (cf. `[disabled]` ou bannière info pays). Pas de variante BE. |
| **Domaine Famille / Immigration** | ✅ aucune action | Les 4 composants ciblés sont strictement DROIT_DU_TRAVAIL. |

**Conclusion** : la dette legacy est cantonnée à ces 4 composants. Aucun autre composant ne suit le pattern obsolète `xxx-section / section-header / section-title font-weight: 600`.

---

## Nouveau pattern UI ou service partagé

❌ **Non**. Cette SF **rejoint** un pattern existant (`td-section / td-header / td-title`), elle n'en introduit aucun. Aucun composant partagé, directive, service, DTO ou endpoint nouveau.

---

## Impact par domaine métier

Cette SF est **transversale** : elle ne porte aucune logique spécifique au domaine. Cependant les 4 composants ciblés sont tous des outils du domaine **DROIT_DU_TRAVAIL FRANCE** (le pattern legacy date d'avant la formalisation du template canonique mi-avril 2026). Il n'existe pas de composant equivalent legacy en Famille ou Immigration : ces domaines ont été développés après la formalisation du template canonique et utilisent déjà `td-section`.

**Symétrie pays** : pas applicable (composants strictement FR).

---

## Préoccupations transversales (anti-régression)

| Préoccupation | Impacté ? | Action |
|---|---|---|
| Auth / Principal | Non | — |
| Workspace context | Non | — |
| Plans / limites | Non | — |
| Navigation / routing | Non | — |
| Outil décisionnel métier | **Oui (visuel uniquement)** | Les 4 composants sont des outils décisionnels mais aucune logique métier touchée. Pas de switch conditionnel, pas de mélange de situations, pas de paramètre métier modifié. **Vérifié** : les 4 composants restent **un outil = une situation métier** (Délais légaux = procédure prud'homale, Fiche prud'homale = saisine, Ancienneté = calcul, Validité RC = analyse) — invariant conservé. |

---

## Notes de mise en œuvre

1. Lire `frontend/src/app/case-files/travail-dissimule-section/travail-dissimule-section.component.html` (lignes 1-25) et `.component.scss` (lignes 1-55) comme **référence canonique** avant chaque migration.
2. Pour chaque composant cible, **lire le HTML actuel en entier** avant d'éditer pour préserver tous les attributs spécifiques (badges, formulaires, ARIA).
3. Conserver la signal-based architecture (`collapsed = signal<boolean>(false)`, `toggleCollapse()`) si déjà en place.
4. Si un composant utilise `<button class="section-header">` au lieu de `<header class="section-header">`, le passer à `<header class="td-header">` avec `tabindex="0"`, `role="button"`, `(keydown.enter)` — même comportement clavier, structure plus sémantique.
5. Build de validation : `cd frontend && npx ng build --configuration=staging` doit passer sans warning.
