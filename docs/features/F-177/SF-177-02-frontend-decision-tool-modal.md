# Mini-spec — F-177 / SF-177-02 Composant `<app-decision-tool-modal>` + service `DecisionToolModalService`

## Identifiant

`F-177 / SF-177-02`

## Feature parente

`F-177` — Refonte panel F-IA-04 (cards verdict synthétique + ouverture modal)

## Statut

`draft`

## Date de création

2026-04-30

## Branche Git

`feat/SF-177-02-decision-tool-modal`

---

## Objectif

Livrer le composant `<app-decision-tool-modal>` (wrapper MatDialog 90vw/90vh hébergeant n'importe quel composant outil dans son `mat-dialog-content`) et le service `DecisionToolModalService.open(toolId, component, inputs, title, icon)` qui instancie ce dialog — sans intégrer encore dans le panel F-IA-04 ou le dashboard agrégé (intégration globale = SF-177-11 après instrumentation des outils SF-177-03 à 08).

---

## Scope révisé vs PRODUCT_SPEC.md

> Le découpage initial du backlog (PRODUCT_SPEC.md ligne F-177) prévoyait "modal wrapper + intégration panel F-IA-04" en SF-177-02. **Scope ajusté ici** : l'intégration panel nécessite que chaque composant outil expose deux capacités nouvelles — `forceExpanded` (s'ouvrir automatiquement dans le modal) et `onSave()` (callback déclenché par le bouton Enregistrer du footer). Ces deux capacités touchent les ~30 composants outils → c'est le scope des SF d'instrumentation 03 à 08. Une **SF-177-11** finale câblera le panel sur le composant card + modal une fois l'instrumentation achevée. SF-177-02 livre donc le wrapper modal isolé, testable, mais non encore consommé en runtime — symétrique de SF-177-01.

---

## Comportement attendu

### Cas nominal

Le service `DecisionToolModalService.open(args)` reçoit :

```typescript
interface DecisionToolModalArgs {
  toolId: string;
  title: string;
  icon: string;
  component: Type<unknown>;
  inputs: Record<string, unknown>;
  /** Callback invoqué quand l'utilisateur clique "Enregistrer" — true si l'outil a accepté la demande, false sinon. */
  onSave?: () => boolean | Promise<boolean>;
}
```

Il ouvre un `MatDialog` configuré :
- `width: '90vw'`, `maxWidth: '1200px'`, `height: '90vh'`, `maxHeight: '900px'`
- `panelClass: 'decision-tool-modal-panel'`
- `autoFocus: 'first-tabbable'`
- `restoreFocus: true`
- `disableClose: false` (l'utilisateur peut fermer via Esc / clic backdrop)
- `data: args` (passé au composant modal)

Le composant `<app-decision-tool-modal>` reçoit `data` via `MAT_DIALOG_DATA` et rend :

- **Header** : icône (24 px) + titre en MAJUSCULES JetBrains Mono (cohérent F-168) + bouton fermer (`close` icon en haut-droite)
- **Content** (`mat-dialog-content`) : le composant outil instancié via `*ngComponentOutlet` avec ses inputs
- **Footer** (`mat-dialog-actions align="end"`) :
  - Bouton secondaire "Annuler" (`mat-stroked-button`) → ferme le dialog sans appeler `onSave`
  - Bouton primaire "Enregistrer" (`mat-flat-button color="primary"`) → si `args.onSave` défini, l'appelle ; selon retour true/false ferme ou laisse ouvert ; si `args.onSave` non défini, le bouton n'est pas affiché (cas outil display-only)

Comportement footer "Enregistrer" :
- Sans `onSave` : bouton absent (outil display-only)
- Avec `onSave` retournant `true` synchrone ou `Promise<true>` : ferme le dialog (`dialogRef.close('saved')`)
- Avec `onSave` retournant `false` ou `Promise<false>` : dialog reste ouvert (l'outil affiche son erreur via MatSnackBar)
- Pendant l'attente d'une promise : bouton désactivé, spinner inline 16 px

Le service retourne le `MatDialogRef<DecisionToolModalComponent, 'saved' | undefined>` pour permettre au consommateur de réagir au close si besoin.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `onSave` non défini | Bouton "Enregistrer" absent du footer, seulement "Fermer" |
| `onSave` retourne `false` | Dialog reste ouvert, bouton ré-activé, focus rendu au composant outil |
| `onSave` lance une exception | MatSnackBar "Erreur lors de l'enregistrement", dialog reste ouvert |
| `onSave` est une `Promise` qui timeout > 30 s | Pas de timeout côté modal — c'est à l'outil d'avoir son propre HTTP timeout. Le bouton reste désactivé tant que la promise ne résout pas |
| `inputs` contient une référence à un composant non monté | Erreur Angular bubble naturellement — pas de wrap spécifique (c'est un bug d'intégration) |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : tous les ~30 composants `*-section` du panel F-IA-04 — consommeront le service en SF-177-11 (intégration globale)
- [x] **Autres pays** : France + Belgique — wrapper agnostique
- [x] **Autres domaines** : Travail / Famille / Immigration — wrapper agnostique
- [x] **Autres UI patterns** : voir analyse "nouveau pattern UI" ci-dessous
- [ ] **Autres flows transversaux** : Auth/Workspace/Plans/Navigation — non concerné

### Niveaux de vérification

- [x] **Modèle TypeScript** : nouvelle interface `DecisionToolModalArgs` colocalisée dans `decision-tool-modal.service.ts`
- [ ] **Record / DTO backend** : non applicable
- [x] **Service / logique métier** : nouveau `DecisionToolModalService` (frontend uniquement, wraps `MatDialog`)
- [ ] **Entité JPA + schéma DB** : non applicable
- [x] **Tests existants** : aucun composant ne consomme encore le service ; tests panel `decisional-tools-panel.component.spec.ts` non touchés

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Où le nouveau pattern UI pourrait-il être réutilisé ?**
  - Panel F-IA-04 (`<app-decisional-tools-panel>`) → consommé par SF-177-11
  - Dashboard agrégé (`<app-case-dashboard>`) → consommé par SF-177-09
  - Aucun autre point d'usage prévu
- [x] **Y a-t-il des patterns concurrents ?**
  - `MatDialog` est déjà utilisé dans l'app pour confirmations (pattern existant du DESIGN_SYSTEM.md) — le wrapper est une spécialisation qui standardise un usage récurrent (modal hébergeant un composant arbitraire avec footer Save/Cancel)
  - Pas de wrapper modal générique existant à remplacer
- [x] **Le nouveau service / endpoint peut-il servir à d'autres features ?**
  - Potentiellement : tout composant Angular qui voudrait héberger un autre composant en modal pourrait réutiliser ce wrapper. Mais le périmètre actuel se limite aux outils décisionnels (verrouillé par convention, pas par typage)
- [x] **Le nouveau composant a-t-il un équivalent design que ce design remplace ?**
  - Pas d'équivalent direct — c'est un nouveau pattern dans l'app (la majorité des MatDialog actuels sont des dialogs ad-hoc avec leur propre composant)

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Panel F-IA-04 | Oui | SF-177-11 (intégration globale après instrumentation 03-08) |
| Dashboard agrégé | Oui | SF-177-09 (intégration via le service) |
| Composants outils (~30) | Oui | SF-177-03 à 08 (exposent `forceExpanded` + `onSave` consommables par le modal) |
| Smoke tests E2E | Non | Service livré "à blanc", pas d'impact runtime tant que non consommé |

### Décision

- [x] Étendu à toutes les cibles applicables : SF-177-03 à 11 sont les SF dépendantes
- [ ] Backlog VN : aucune cible reportée
- [ ] Non applicable aux autres cibles

---

## Critères d'acceptation

- [ ] Service `DecisionToolModalService` créé dans `frontend/src/app/case-files/decisional-tools-panel/decision-tool-modal/decision-tool-modal.service.ts`
- [ ] Service expose `open(args: DecisionToolModalArgs): MatDialogRef<DecisionToolModalComponent, 'saved' | undefined>`
- [ ] Composant standalone `<app-decision-tool-modal>` créé dans le même dossier
- [ ] Modal a la config MatDialog : `width: 90vw`, `maxWidth: 1200px`, `height: 90vh`, `maxHeight: 900px`, `panelClass: 'decision-tool-modal-panel'`
- [ ] Header rend : `mat-icon` (de l'arg), titre en MAJUSCULES JetBrains Mono, bouton fermer (`close` icon)
- [ ] Content rend : composant outil instancié via `*ngComponentOutlet` avec `inputs`
- [ ] Footer rend : "Annuler" (mat-stroked-button) + "Enregistrer" (mat-flat-button color="primary") **uniquement si `onSave` défini**
- [ ] Click "Annuler" : ferme dialog sans appeler `onSave`
- [ ] Click "Enregistrer" sans `onSave` défini : bouton inexistant
- [ ] Click "Enregistrer" avec `onSave` retournant `true` : ferme dialog (`'saved'`)
- [ ] Click "Enregistrer" avec `onSave` retournant `false` : dialog reste ouvert, bouton réactivé
- [ ] Click "Enregistrer" avec `onSave` retournant Promise true : bouton spinner pendant attente, ferme à la résolution
- [ ] Click "Enregistrer" avec `onSave` lançant exception : MatSnackBar erreur, dialog reste ouvert
- [ ] Click bouton fermer header ou Esc : ferme dialog (équivalent Annuler)
- [ ] Tests Jest couvrent : (a) ouverture du modal avec inputs ; (b) rendu header titre + icône ; (c) rendu content via componentOutlet ; (d) bouton Enregistrer caché si `onSave` undefined ; (e) Annuler ferme sans onSave ; (f) Enregistrer true ferme avec 'saved' ; (g) Enregistrer false reste ouvert ; (h) Promise true ferme ; (i) Promise false reste ouvert ; (j) exception affiche snackbar
- [ ] Build Angular réussit
- [ ] `npm run test` reste vert (pas de régression)

---

## Périmètre

### Hors scope (explicite)

- Intégration dans le panel F-IA-04 (= SF-177-11, après instrumentation des outils)
- Intégration dans le dashboard agrégé (= SF-177-09)
- Instrumentation des composants outils pour exposer `forceExpanded` et `onSave` (= SF-177-03 à 08)
- Backend : aucun changement
- Animations custom (transition simple MatDialog par défaut)
- Tests E2E (couverts plus tard quand intégration faite)

---

## Valeurs initiales

Aucune entité créée.

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs autorisées | Notes |
|-------|-------------|----------------------------|-------|
| `args.toolId` | Oui | string non vide | Sert à `data-tool-id` attribut + tracking |
| `args.title` | Oui | string non vide | Affiché en header MAJUSCULES |
| `args.icon` | Oui | string Material Icon | Affiché en header |
| `args.component` | Oui | `Type<unknown>` | Composant Angular standalone à instancier |
| `args.inputs` | Oui | `Record<string, unknown>` | Inputs passés au composant via `*ngComponentOutlet` |
| `args.onSave` | Non | `() => boolean \| Promise<boolean>` | Si absent → bouton Enregistrer caché |

---

## Technique

### Endpoint(s)

Aucun (frontend pur).

### Tables impactées

Aucune.

### Migration Liquibase

- [ ] Non applicable

### Composants Angular

- `DecisionToolModalComponent` (nouveau, standalone) — `decision-tool-modal/decision-tool-modal.component.{ts,html,scss,spec.ts}`
- `DecisionToolModalService` (nouveau, `providedIn: 'root'`) — `decision-tool-modal/decision-tool-modal.service.ts` + `.spec.ts`
- Interface `DecisionToolModalArgs` colocalisée dans le service

### Structure

```
frontend/src/app/case-files/decisional-tools-panel/decision-tool-modal/
  ├── decision-tool-modal.component.ts
  ├── decision-tool-modal.component.html
  ├── decision-tool-modal.component.scss
  ├── decision-tool-modal.component.spec.ts
  ├── decision-tool-modal.service.ts
  └── decision-tool-modal.service.spec.ts
```

---

## Plan de test

### Tests unitaires (Jest)

#### `DecisionToolModalComponent`
- [ ] Rendu header avec titre + icône passés via MAT_DIALOG_DATA
- [ ] Rendu content via `*ngComponentOutlet` avec un composant stub + ses inputs (vérifier que le stub reçoit ses inputs)
- [ ] Bouton Enregistrer absent quand `onSave` undefined
- [ ] Bouton Enregistrer présent quand `onSave` défini
- [ ] Click Annuler : `dialogRef.close()` appelé sans argument
- [ ] Click Enregistrer + `onSave` retourne `true` : `dialogRef.close('saved')` appelé
- [ ] Click Enregistrer + `onSave` retourne `false` : `dialogRef.close` non appelé
- [ ] Click Enregistrer + `onSave` retourne `Promise<true>` : bouton désactivé pendant attente, close('saved') après résolution
- [ ] Click Enregistrer + `onSave` retourne `Promise<false>` : dialog reste ouvert, bouton réactivé
- [ ] Click Enregistrer + `onSave` lance exception : MatSnackBar appelé, dialog reste ouvert

#### `DecisionToolModalService`
- [ ] `open(args)` appelle `MatDialog.open` avec la bonne config (width 90vw, maxWidth 1200px, etc.)
- [ ] `open(args)` passe `args` dans `data`
- [ ] `open(args)` retourne le `MatDialogRef`

### Tests d'intégration

Non applicable (pas de backend).

### Isolation workspace

Non applicable (composant pur d'affichage).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — service isolé, pas d'auth/workspace/plans/navigation, livré non-intégré

### Composants / endpoints existants potentiellement impactés

Aucun (pas d'intégration dans cette SF).

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — service livré "à blanc"

---

## Impact par domaine métier

Wrapper transversal par construction — pas de logique métier. Les variations par domaine sont entièrement portées par les composants outils consommateurs (SF-177-03 à 08).

---

## Dépendances

### Subfeatures bloquantes

Aucune. SF-177-02 est indépendante de SF-177-01 (le service modal n'utilise pas la card — c'est le panel en SF-177-11 qui orchestrera card + service).

### Subfeatures débloquées

- SF-177-09 (dashboard agrégé qui ouvre le modal sur clic card)
- SF-177-11 (intégration globale panel après instrumentation)

### Questions ouvertes impactées

- [x] Aucune question ouverte impactée

---

## Notes et décisions

- **Service plutôt que composant exposé directement** : le service `DecisionToolModalService.open()` masque la complexité de `MatDialog.open` au consommateur, et permet de centraliser la config (width/height/panelClass) à un seul endroit.
- **`onSave` retourne `boolean | Promise<boolean>`** : laisse au composant outil le choix d'être synchrone (validation locale OK → save direct) ou asynchrone (HTTP). Le retour booléen permet au modal de rester ouvert si la validation échoue côté outil.
- **Bouton Enregistrer absent si pas de `onSave`** : décision produit — outil display-only n'a pas besoin de confirmation, l'utilisateur ferme via Annuler ou Esc.
- **Pas de timeout côté modal** : c'est à l'outil de gérer ses HTTP timeouts. Le modal n'est pas responsable de la robustesse réseau.
- **Pas d'intégration dans cette SF** : décision actée car l'intégration panel/dashboard nécessite que les composants outils exposent `forceExpanded` et un mécanisme de save callback — instrumentation portée par SF-177-03 à 08. Tenter d'intégrer maintenant serait soit (a) une bascule cassée (les outils ne savent pas s'auto-expand), soit (b) un hack temporaire (simulate click sur header) à supprimer plus tard.
