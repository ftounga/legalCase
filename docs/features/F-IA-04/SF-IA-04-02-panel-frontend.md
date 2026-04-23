# Mini-spec — F-IA-04 / SF-IA-04-02 Composant frontend `<app-decisional-tools-panel>`

## Identifiant
`F-IA-04 / SF-IA-04-02`

## Feature parente
`F-IA-04` — Moteur d'affichage conditionnel des outils décisionnels

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-IA-04-02-decisional-tools-panel`

---

## Objectif

Créer un composant Angular `<app-decisional-tools-panel>` qui consomme l'endpoint `GET /api/v1/case-files/{id}/decision-tools-visibility` (SF-IA-04-01) et rend dynamiquement les outils décisionnels d'un dossier en trois couches (always-on / contextual / catalogue). **Pas d'intégration dans `case-file-detail` dans cette SF** — c'est le job de SF-IA-04-03.

---

## Comportement attendu

### Cas nominal
Le composant reçoit `[caseFileId]` + `[synthesis]` en entrée. Au `ngOnInit` :
1. Appelle `CaseFileService.getDecisionToolsVisibility(caseFileId)` → `VisibleToolSet { alwaysOn, contextual, catalog }`
2. Pour chaque `tool_id` de `alwaysOn` et `contextual`, résout via un **registre statique** `Map<string, ComponentType>` le composant Angular correspondant
3. Utilise `NgComponentOutlet` pour rendre dynamiquement chaque composant avec les inputs `[caseFileId]` + `[synthesis]` passés en binding
4. Affiche `catalog` sous forme de chips/boutons (pas de rendu du composant — juste un label cliquable "Ajouter cet outil") — en V1, le clic fait **rien** (bouton désactivé avec tooltip "Activation manuelle bientôt disponible"). Le mécanisme complet d'activation manuelle est hors scope (V2).
5. Si une `tool_id` n'est pas dans le registre → warning log + skip (forward-compat : permet de livrer le backend d'une nouvelle règle avant le frontend correspondant)

### Cas d'erreur
| Situation | Comportement |
|---|---|
| API 404 (caseFile introuvable) | snackbar erreur + panel vide (3 listes) |
| API 500 / réseau down | snackbar erreur + panel vide |
| Registre ne contient pas le `tool_id` | log warning `[decisional-tools-panel] Unknown toolId: {id}` + skip ce tool |
| `alwaysOn` / `contextual` tous deux vides | panel affiche un empty state "Aucun outil disponible pour ce dossier" |

---

## Analyse de cohérence transversale

### Périmètres scannés
| Cible | Applicable ? | Traitement |
|---|---|---|
| Composants décisionnels existants (13 identifiés) | Oui — le panel doit **tous** les référencer dans le registre | **Intégré** : registre de 13 entrées minimum. `F-132-rupture-conv-indemnite` absent du registre si SF-132-02 pas encore mergée → flag |
| Autres pays / domaines | N/A côté panel — c'est le backend qui filtre selon `(legalDomain, country)` | Non applicable |
| Cohérence IA (F-IA-03) | N/A — le panel n'affiche pas de réponse avocat, il oriente | — |
| Refresh dashboard F-IA-02 | À brancher en **SF-IA-04-04** | — |
| Masquage conditionnel `@if` existants | **Cohabitation courte** — cette SF ne remplace rien dans `case-file-detail`. L'intégration (remplacement des 13 `@if`) est **SF-IA-04-03**. | SF parallèle |
| Nouveau pattern UI | Oui — **conteneur dynamique via `NgComponentOutlet`** | Voir section ci-dessous |

### Nouveau pattern UI
- Composant `<app-decisional-tools-panel>` = nouveau **conteneur dynamique**. Pattern unique dans l'app jusqu'ici (aucun autre endroit n'utilise `NgComponentOutlet` côté `case-file-detail`).
- Remplace à terme (SF-IA-04-03) les ~13 blocs `@if` hardcodés dans `case-file-detail.component.html`. Pas de coexistence durable.
- **Registre** centralisé dans le composant lui-même (pas de service séparé en V1) — si un nouvel outil arrive, on ajoute 1 ligne dans le registre. Documenté dans le code.

### Décision
- [x] Étendu aux cibles applicables : les 13 composants existants dans le registre
- [x] SF parallèle créée : SF-IA-04-03 (intégration), SF-IA-04-04 (dashboard)
- [x] Non applicable ailleurs

---

## Critères d'acceptation

- [ ] Nouveau composant Angular `DecisionToolsPanelComponent` dans `frontend/src/app/case-files/decisional-tools-panel/` (4 fichiers : `.ts`, `.html`, `.scss`, `.spec.ts`)
- [ ] Selector `app-decisional-tools-panel`, standalone, signal-based (signals Angular 19)
- [ ] Inputs : `caseFileId: InputSignal<string>` (required), `synthesis: InputSignal<any | null>` (optional, forward au registre)
- [ ] Au `ngOnInit` (ou effect selon l'approche) : fetch via `CaseFileService.getDecisionToolsVisibility(caseFileId)` → signal `visibility`
- [ ] Nouvelle méthode `CaseFileService.getDecisionToolsVisibility(id: string): Observable<VisibleToolSet>` + interface TypeScript `VisibleToolSet { alwaysOn: string[]; contextual: string[]; catalog: string[] }`
- [ ] Registre statique `TOOL_REGISTRY: Map<string, Type<unknown>>` dans le composant, couvrant **au minimum** : `F-DT-04-fiche-prudhomale`, `F-DT-07-anciennete-conges-prime`, `F-DT-08-licenciement-validity`, `F-DT-09-comparateur-indemnites`, `F-DT-10-rupture-conv-validity`, `F-132-rupture-conv-indemnite`, `F-FA-05-partage-immobilier`, `F-FA-06-calendrier-garde`, `F-FA-07-checklist-divorce`, `F-IM-01-checklist-pieces`, `F-IM-05-arbre-decisionnel-titre`, `F-IM-06-recours`, `F-IM-07-droit-au-travail`. Tool IDs non mappés → log warning et skip
- [ ] Template : 3 sections `mat-card` avec titres "Outils principaux" / "Outils contextuels" / "Catalogue", `NgComponentOutlet` pour le rendu dynamique avec `inputs` binding (Angular 16.2+)
- [ ] Empty state si `alwaysOn.length === 0 && contextual.length === 0`
- [ ] Catalog rendu comme chips désactivés avec tooltip "Activation manuelle bientôt disponible"
- [ ] Gestion d'erreur HTTP (snackbar via `MatSnackBar`) + panel vide en fallback
- [ ] Tests unitaires Jest : au moins 5 tests couvrant (1) rendu 3 couches avec mocks, (2) empty state, (3) tool_id inconnu skippé avec warning, (4) erreur HTTP → snackbar, (5) inputs forwarded au composant rendu
- [ ] Design system : `mat-card` avec `border-radius 8px`, `padding 24px`, `margin 16px` entre cartes, h2 Merriweather 24px couleur `#1C2B3A`, **pas** de couleur de fond sur la card (fond blanc)

---

## Périmètre

### Hors scope
- Intégration dans `case-file-detail.component.html` (remplacement des 13 `@if`) → **SF-IA-04-03**
- Intégration dashboard F-IA-02 → **SF-IA-04-04**
- Activation manuelle réelle d'un outil du catalogue (juste un bouton désactivé en V1)
- Refresh automatique sur nouvelle analyse IA (F-IA-02 pattern `CaseDashboardRefreshService.triggerRefresh()`) → **SF-IA-04-04**
- Filtrage/recherche dans le catalogue → **V2**
- Administration UI des règles → **hors V1**

---

## Contraintes de validation

| Champ | Obligatoire | Format |
|---|---|---|
| `caseFileId` (input) | Oui | UUID string |
| `synthesis` (input) | Non | objet ou null — forwardé aux sous-composants |

---

## Technique

### Nouveaux fichiers
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts`
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.html`
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.scss`
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.spec.ts`

### Modification
- `frontend/src/app/core/services/case-file.service.ts` : nouvelle méthode `getDecisionToolsVisibility(id)`

### Aucune migration DB ni endpoint backend
Endpoint déjà livré par SF-IA-04-01.

---

## Plan de test

### Tests unitaires Jest
- `renders 3 layers when visibility returns all groups filled`
- `renders empty state when alwaysOn and contextual are both empty`
- `skips unknown tool_id with warning log`
- `shows snackbar on HTTP error`
- `forwards caseFileId and synthesis to rendered sub-components`

### Tests d'intégration backend
Non applicable — cette SF est frontend only, le backend a déjà été testé en SF-IA-04-01.

### Isolation workspace
Non applicable — le backend garantit déjà l'isolation. Le composant reçoit l'ID du dossier de son parent, qui est lui-même protégé par l'auth.

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Aucune préoccupation transversale** — le composant est nouveau, isolé, et non intégré nulle part dans cette SF. Aucun impact sur l'existant.

### Smoke tests E2E
- [x] Aucun smoke test concerné — le composant n'est utilisé par personne en fin de SF.

---

## Dépendances
- **SF-IA-04-01** done (endpoint backend disponible) ✓
- **SF-132-02** pas obligatoire — si le composant `RuptureConvIndemniteSectionComponent` n'existe pas encore côté frontend, le registre mappe `F-132-rupture-conv-indemnite` vers `null` et le panel skippe avec warning log. Forward-compat assurée.

---

## Notes et décisions

### Pourquoi un registre centralisé dans le composant et pas un service ?
En V1, le registre est court (~13 entrées), évolue à la même cadence que le composant (un nouvel outil = 1 ligne), et n'est consommé nulle part ailleurs. Le factoriser en service ajouterait un niveau d'indirection sans bénéfice. Si un 2ᵉ consommateur émerge (dashboard F-IA-02 en SF-IA-04-04 par exemple), on extraira le registre dans un service à ce moment-là.

### Pourquoi `NgComponentOutlet` plutôt que `switch` de templates ?
- `switch` avec 13 branches devient illisible et nécessite une mise à jour du template à chaque nouvel outil
- `NgComponentOutlet` prend le composant comme donnée → ajouter un outil = ajouter une ligne dans le registre, rien dans le template
- Support des inputs dynamiques depuis Angular 16.2 (on est sur Angular 19)

### Gestion des inputs hétérogènes entre sous-composants
V1 : panel forward 2 inputs uniformes (`[caseFileId]`, `[synthesis]`). Si un sous-composant en demande plus (ex. `[workspaceCountry]`), on étend le registre avec un transformateur `(context) => inputs`. Non nécessaire en V1 d'après le scan des composants existants.
