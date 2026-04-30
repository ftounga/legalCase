# Mini-spec — F-177 / SF-177-01 Composant partagé `<app-decision-tool-card>` + interface `DecisionToolSummary`

## Identifiant

`F-177 / SF-177-01`

## Feature parente

`F-177` — Refonte panel F-IA-04 (cards verdict synthétique + ouverture modal)

## Statut

`draft`

## Date de création

2026-04-30

## Branche Git

`feat/SF-177-01-decision-tool-card`

---

## Objectif

Livrer le composant partagé `<app-decision-tool-card>` réutilisable et son interface `DecisionToolSummary` exposée par chaque outil décisionnel — brique d'infrastructure dont SF-177-02 (modal), SF-177-03-08 (instrumentation outils), SF-177-09 (dashboard agrégé) dépendent.

---

## Comportement attendu

### Cas nominal

Le composant `<app-decision-tool-card>` reçoit en `@Input()` :

- `toolId: string` (ex : `F-IM-05-titre-sejour`)
- `theme: ThemeKey` (ex : `DIAGNOSTIC`, `DELAYS`, `INDEMNITIES`)
- `icon: string` (Material icon name)
- `title: string` (ex : `TITRE DE SÉJOUR RECOMMANDÉ`)
- `summary: DecisionToolSummary | null` (verdict synthétique, ou null si non calculé)
- `prefillCount: number | null` (nombre de champs pré-remplis IA, ou null si l'outil n'utilise pas le pré-fill)
- `coherenceAlertCount: number | null` (alertes F-IA-03 actives, ou null si l'outil n'utilise pas F-IA-03)
- `metierAlertLevel: 'OK' | 'WARNING' | 'ALERT' | null` (alerte métier dérivée du résultat)
- `disabled: boolean` (false par défaut)

Le composant émet un seul `@Output() open = new EventEmitter<void>()` quand l'utilisateur clique sur la card.

Rendu visuel :

- Card MatCard 100 % largeur (la grid parent gère 1 ou 2 colonnes via F-169) :
  - Header : icône (24px) + titre en MAJUSCULES JetBrains Mono (cohérent avec F-168)
  - Body : verdict synthétique formaté `{label} : {primaryValue} {secondaryValue?}`
    - Si `summary === null` → texte gris "Cliquer pour utiliser" (cas générateurs de document, checklists pures sans état)
    - Si `summary.alertLevel === 'ALERT'` → bordure gauche rouge 4px + valeur en rouge
    - Si `summary.alertLevel === 'WARNING'` → bordure gauche orange 4px + valeur en orange
    - Si `summary.alertLevel === 'OK'` → bordure gauche verte 4px + valeur en vert
    - Si `summary.alertLevel === undefined` ou `null` → pas de bordure colorée, valeur en navy
  - Coin haut-droit : empilement vertical de 3 badges discrets (icône seule, tooltip au hover) :
    - ✨ `auto_awesome` — visible uniquement si `prefillCount > 0` (tooltip : `{prefillCount} champ(s) pré-rempli(s) par l'IA`)
    - 🔴 `error` rouge — visible uniquement si `coherenceAlertCount > 0` (tooltip : `{coherenceAlertCount} alerte(s) de cohérence IA`)
    - ⚠️ `warning` orange — visible uniquement si `metierAlertLevel === 'WARNING'` ou `metierAlertLevel === 'ALERT'` ET la couleur primary value ne porte déjà pas l'alerte
- Curseur `pointer` sur la card entière, hover : `box-shadow` plus marqué + transition 150 ms
- Click ou Enter sur la card → `open.emit()`
- Si `disabled === true` : opacité 0.5, curseur `not-allowed`, click ne fait rien

Aucun lien direct avec le `MatDialog` dans cette SF — le composant émet l'événement seulement, c'est SF-177-02 qui consomme et ouvre le dialog.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `summary === null` et l'outil n'expose effectivement aucun verdict | Texte "Cliquer pour utiliser" gris navy 50 %, badges éventuels affichés normalement |
| `prefillCount === null` | Pas de badge ✨ |
| `coherenceAlertCount === null` | Pas de badge 🔴 |
| `metierAlertLevel === null` | Pas de badge ⚠️ |
| `summary.primaryValue` est une chaîne vide | Card traitée comme `summary === null` (texte fallback "Cliquer pour utiliser") |
| `disabled === true` | Click sans effet, opacité 0.5, badges visibles mais grisés |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : tous les ~30 composants `*-section` du panel F-IA-04 — instrumentés en SF-177-03 à 08 (voir mini-specs jumelles)
- [x] **Autres pays** : France + Belgique — couvert par les SF d'instrumentation par domaine × pays
- [x] **Autres domaines** : Travail / Famille / Immigration — couvert par les 6 SF d'instrumentation
- [x] **Autres UI patterns** : voir analyse "nouveau pattern UI" ci-dessous
- [ ] **Autres flows transversaux** : Auth/Workspace/Plans/Navigation — non concerné (composant pur d'affichage)

### Niveaux de vérification

- [x] **Modèle TypeScript** : nouveau `DecisionToolSummary` interface dans `frontend/src/app/case-files/decisional-tools-panel/decision-tool-summary.model.ts` (fichier nouveau)
- [ ] **Record / DTO backend** : non applicable (composant frontend pur, les données viennent des composants outils consommateurs)
- [ ] **Service / logique métier** : non applicable
- [ ] **Entité JPA + schéma DB** : non applicable
- [x] **Tests existants** : tests Jest des ~30 composants outils ne sont pas touchés par SF-177-01 (juste le composant card). Tests panel `decisional-tools-panel.component.spec.ts` non touchés tant que l'intégration n'est pas faite (SF-177-02)

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Où le nouveau pattern UI pourrait-il être réutilisé ?**
  - Bandeau dashboard F-IA-02 (`<app-case-dashboard>`) → consommé par SF-177-09
  - Panel F-IA-04 (`<app-decisional-tools-panel>`) → consommé par SF-177-02
  - Pas d'autre point de réutilisation identifié à ce jour
- [x] **Y a-t-il des patterns concurrents ?**
  - Aucune card "verdict synthétique" existante — pattern nouveau
  - Les 30 composants `*-section` actuels affichent leur verdict en interne après expand : pas un concurrent direct, ce sont les futurs producteurs de `DecisionToolSummary`
  - 4 composants legacy sans card-root (`immigration-recours`, `immigration-checklist`, `immigration-title-decision`, `droit-au-travail`) : leur dette visuelle disparaît automatiquement avec ce nouveau wrapper (= F-168 bis absorbée par F-177)
- [x] **Le nouveau service / endpoint peut-il servir à d'autres features ?** Non applicable (pas de service)
- [x] **Le nouveau composant a-t-il un équivalent design que ce design remplace ?**
  - Le composant remplace progressivement l'expansion inline actuelle des cards du panel F-IA-04
  - Coexistence transitoire : SF-177-01 livre le composant non intégré ; SF-177-02 fait la bascule pour le panel ; SF-177-09 fait la bascule pour le dashboard agrégé
  - Pas de coexistence durable prévue

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Panel F-IA-04 (`<app-decisional-tools-panel>`) | Oui | SF-177-02 (intégration modal + remplacement expand inline) |
| Dashboard agrégé (`<app-case-dashboard>`) | Oui | SF-177-09 (réutilisation card pour le dashboard) |
| Composants outils (~30) | Oui | SF-177-03 à 08 (instrumentation `DecisionToolSummary` par domaine × pays) |
| Composants legacy sans card-root (4) | Oui | SF-177-10 (nettoyage SCSS root) |
| Smoke tests E2E | Non | Composant pur, pas d'impact runtime tant que non consommé |

### Décision

- [x] Étendu à toutes les cibles applicables : SF-177-02 à 10 sont les SF jumelles qui consomment ou complètent SF-177-01
- [ ] Backlog VN : aucune cible reportée
- [ ] Non applicable aux autres cibles

---

## Critères d'acceptation

- [ ] Fichier `frontend/src/app/case-files/decisional-tools-panel/decision-tool-summary.model.ts` créé exposant l'interface `DecisionToolSummary`, le type `MetierAlertLevel = 'OK' | 'WARNING' | 'ALERT'`, et la fonction utilitaire `formatSummary(summary: DecisionToolSummary | null): string`
- [ ] Composant standalone `<app-decision-tool-card>` créé dans `frontend/src/app/case-files/decisional-tools-panel/decision-tool-card/`
- [ ] Composant accepte les 9 inputs listés ci-dessus (`toolId`, `theme`, `icon`, `title`, `summary`, `prefillCount`, `coherenceAlertCount`, `metierAlertLevel`, `disabled`)
- [ ] Composant émet `@Output() open = new EventEmitter<void>()` au click ou clavier Enter sur la card non-`disabled`
- [ ] Card non-`disabled` est focalisable (`tabindex="0"`, `role="button"`, `aria-label="{title}"`)
- [ ] Verdict affiché formaté `{label} : {primaryValue} {secondaryValue?}` quand `summary !== null` et `primaryValue !== ''`
- [ ] Texte fallback `Cliquer pour utiliser` (couleur navy 50 %) affiché quand `summary === null` ou `primaryValue === ''`
- [ ] Bordure gauche colorée 4px appliquée selon `summary.alertLevel` : rouge `#C62828` ALERT, orange `#E68900` WARNING, vert `#3F7B3F` OK, aucune si `null`/`undefined`
- [ ] Badge ✨ `auto_awesome` rendu uniquement si `prefillCount > 0` ; tooltip `{N} champ(s) pré-rempli(s) par l'IA` sur hover
- [ ] Badge 🔴 `error` rouge rendu uniquement si `coherenceAlertCount > 0` ; tooltip `{N} alerte(s) de cohérence IA`
- [ ] Badge ⚠️ `warning` orange rendu uniquement si `metierAlertLevel === 'WARNING'` ou `'ALERT'`
- [ ] État `disabled` : opacité 0.5, curseur `not-allowed`, click ne déclenche pas `open`
- [ ] Tests Jest couvrent : (a) rendu nominal avec summary OK/WARNING/ALERT/null ; (b) émission de `open` au click ; (c) émission de `open` à Enter ; (d) absence d'émission si `disabled === true` ; (e) badges affichés conditionnellement selon les 3 prefillCount/coherenceAlertCount/metierAlertLevel ; (f) fallback texte si `summary === null`
- [ ] Build Angular (`npm run build`) réussit
- [ ] `npm run test` reste vert (pas de régression dans les ~3700 tests existants)
- [ ] Lint vert (`npm run lint` si configuré)

---

## Périmètre

### Hors scope (explicite)

- Intégration dans le panel F-IA-04 (= SF-177-02)
- Modal `MatDialog` (= SF-177-02)
- Bouton "Enregistrer" du modal (= SF-177-02)
- Instrumentation des composants outils existants pour qu'ils exposent leur `summary` (= SF-177-03 à 08)
- Intégration dashboard agrégé `<app-case-dashboard>` (= SF-177-09)
- Nettoyage SCSS root des 4 composants legacy (= SF-177-10)
- Backend : aucun changement (les données viennent des `*Analysis` déjà persistées)
- Tests E2E

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `disabled` | `false` | Permet activation manuelle ultérieure si besoin (catalogue) |
| `prefillCount` | `null` | L'outil consommateur passera 0 ou un nombre — null = pas de pré-fill IA possible |
| `coherenceAlertCount` | `null` | Idem F-IA-03 |
| `metierAlertLevel` | `null` | Idem |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `toolId` | Oui | — | string non vide | Non (par card) | — |
| `theme` | Oui | — | `ThemeKey` enum existant F-169 | Non | — |
| `icon` | Oui | — | string Material Icon | Non | — |
| `title` | Oui | — | string non vide | Non | — |
| `summary.label` | Oui (si summary !== null) | — | string non vide | Non | — |
| `summary.primaryValue` | Oui (si summary !== null) | — | string (peut être vide → fallback) | Non | — |
| `summary.secondaryValue` | Non | — | string | Non | — |
| `summary.alertLevel` | Non | — | `'OK' \| 'WARNING' \| 'ALERT'` | Non | — |
| `prefillCount` | Non | — | number ≥ 0 ou null | Non | — |
| `coherenceAlertCount` | Non | — | number ≥ 0 ou null | Non | — |
| `metierAlertLevel` | Non | — | `'OK' \| 'WARNING' \| 'ALERT'` ou null | Non | — |
| `disabled` | Non | — | boolean (default false) | Non | — |

---

## Technique

### Endpoint(s)

Aucun (composant frontend pur).

### Tables impactées

Aucune.

### Migration Liquibase

- [ ] Non applicable

### Composants Angular

- `<app-decision-tool-card>` (nouveau, standalone) — `frontend/src/app/case-files/decisional-tools-panel/decision-tool-card/decision-tool-card.component.ts` + `.html` + `.scss` + `.spec.ts`
- Interface `DecisionToolSummary` + type `MetierAlertLevel` + util `formatSummary()` — `frontend/src/app/case-files/decisional-tools-panel/decision-tool-summary.model.ts` (nouveau)

### Interface `DecisionToolSummary`

```typescript
export type MetierAlertLevel = 'OK' | 'WARNING' | 'ALERT';

export interface DecisionToolSummary {
  /** Libellé court à gauche du verdict (ex: "Verdict", "Délai", "Indemnité"). */
  label: string;
  /** Valeur principale affichée (ex: "65 %", "3 200 €", "J-7"). Vide → fallback "Cliquer pour utiliser". */
  primaryValue: string;
  /** Précision optionnelle (ex: "MOYEN", "(brut)"). */
  secondaryValue?: string;
  /** Niveau d'alerte métier dérivé du résultat — colore la bordure gauche. */
  alertLevel?: MetierAlertLevel;
}
```

---

## Plan de test

### Tests unitaires (Jest)

- [ ] `decision-tool-card.component.spec.ts` — rendu nominal avec summary `{label: 'Verdict', primaryValue: '65 %', secondaryValue: 'MOYEN', alertLevel: 'WARNING'}` : titre, verdict formaté, bordure gauche orange visibles
- [ ] Rendu avec `summary === null` : fallback "Cliquer pour utiliser" affiché, pas de bordure colorée
- [ ] Rendu avec `summary.primaryValue === ''` : fallback "Cliquer pour utiliser" affiché
- [ ] Rendu avec chaque `alertLevel` : OK → bordure verte, WARNING → orange, ALERT → rouge, undefined → pas de bordure
- [ ] Rendu avec `prefillCount === 3` : badge ✨ visible avec tooltip `3 champ(s) pré-rempli(s) par l'IA`
- [ ] Rendu avec `prefillCount === 0` ou `null` : badge ✨ absent
- [ ] Rendu avec `coherenceAlertCount === 2` : badge 🔴 visible avec tooltip `2 alerte(s) de cohérence IA`
- [ ] Rendu avec `coherenceAlertCount === 0` ou `null` : badge 🔴 absent
- [ ] Rendu avec `metierAlertLevel === 'WARNING'` : badge ⚠️ orange visible
- [ ] Rendu avec `metierAlertLevel === 'OK'` ou `null` : badge ⚠️ absent
- [ ] Click sur la card : `open` émis 1 fois
- [ ] Keydown Enter sur la card : `open` émis 1 fois
- [ ] Keydown Space sur la card : `open` émis 1 fois (cohérent avec `role="button"`)
- [ ] Click avec `disabled === true` : `open` non émis, opacité 0.5 visible, curseur `not-allowed`
- [ ] Card a `role="button"`, `tabindex="0"`, `aria-label="{title}"`

### Tests d'intégration

Non applicable (composant frontend pur, pas de backend, pas d'endpoint).

### Isolation workspace

- [ ] Non applicable — composant pur d'affichage, ne fait aucun appel réseau, ne lit aucun contexte de sécurité

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — composant isolé, impact limité à son périmètre, pas de modification d'auth / workspace / plans / navigation

### Composants / endpoints existants potentiellement impactés

Aucun. SF-177-01 livre le composant non intégré ; les cibles d'intégration sont des SF jumelles ultérieures.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — le composant n'est pas encore consommé après cette SF (livré "à blanc" pour les SF dépendantes)

---

## Impact par domaine métier

Cette feature est **transversale par construction** : le composant `<app-decision-tool-card>` est utilisé par tous les outils des 3 domaines × 2 pays sans adaptation par domaine.

- **Travail FR / BE** : cards utilisées dans le panel F-IA-04 et le dashboard agrégé sans variation
- **Famille FR / BE** : idem
- **Immigration FR / BE** : idem

Aucune logique métier dans le composant — c'est un wrapper de présentation pur. Les variations par domaine sont entièrement portées par les SF d'instrumentation (SF-177-03 à 08) qui calculent le `DecisionToolSummary` à partir de l'`*Analysis` propre à chaque outil.

---

## Dépendances

### Subfeatures bloquantes

Aucune. SF-177-01 est le bloc d'infrastructure dont SF-177-02 à 10 dépendent.

### Subfeatures débloquées

- SF-177-02 (modal wrapper + intégration panel F-IA-04)
- SF-177-03 à 08 (instrumentation par domaine × pays)
- SF-177-09 (dashboard agrégé)
- SF-177-10 (polish SCSS legacy)

### Questions ouvertes impactées

- [x] Aucune question ouverte de `docs/OPEN_QUESTIONS.md` impactée

---

## Notes et décisions

- **Pas de logique de modal dans cette SF** : la card émet seulement un événement `open` ; le consommateur (panel ou dashboard) décide quoi faire (ouvrir un MatDialog, naviguer, etc.). Cela garde le composant pur et testable.
- **Bordure gauche colorée** plutôt que badge centralisé pour `alertLevel` : signal visuel discret mais fort, cohérent avec le DESIGN_SYSTEM.md.
- **Badges en colonne en haut-droite** plutôt qu'en bas : zone moins encombrée, lisibilité du verdict préservée.
- **Pas de `secondaryValue` requis** : laisse les SF d'instrumentation libres de le passer ou non selon ce que l'outil expose.
- **`primaryValue: ''` traité comme null** : sécurité contre les outils qui exposeraient un summary vide par bug — fallback gracieux.
