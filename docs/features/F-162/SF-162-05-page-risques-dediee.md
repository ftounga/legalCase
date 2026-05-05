# SF-162-05 — Page Risques dédiée

## Objectif

Sortir le bloc **Risques** de la page synthèse vers une **page dédiée** avec des cards portant un badge de gravité (FAIBLE / MOYEN / ELEVE), et faire pointer le badge "Risques" de la grille SF-162-01 vers cette page.

## Contexte

4ᵉ et dernière page dédiée riche de F-162. Le `riskLevel` global de l'analyse est utilisé pour estimer la gravité des risques individuels (faute d'info dédiée par item dans le modèle actuel — voir hors périmètre).

## Comportement nominal

1. Nouvelle route `/case-files/:id/synthesis/risques` (lazy-loaded).
2. Composant `SynthesisRisquesComponent` standalone.
3. Header identique aux pages précédentes (titre, retour, version, dossier).
4. Bandeau de tête : niveau de risque global (`riskLevel` + `riskScore`) si présent — réutilise les classes `risk-badge--*` existantes via SCSS local mais sans dépendre du composant Synthesis.
5. Body : cards de risques numérotés, chaque card reçoit la classe `--gravite-{level}` calquée sur le `riskLevel` global. (Dans une SF future, l'IA pourra fournir une gravité par item ; ici on hérite du niveau global pour donner une indication visuelle uniforme.)
6. Composant `app-source-ref` réutilisé.
7. Badge "Risques" → `routerLink` vers cette route.
8. État vide explicite si liste vide.

## Cas d'erreur / edge cases

- `riskLevel` absent → cards rendues sans classe gravité (style neutre).
- Aucun risque → état vide explicite.

## Critères d'acceptation

- [ ] Route `/case-files/:id/synthesis/risques` enregistrée.
- [ ] Composant standalone réutilise patron `.detail-page`.
- [ ] Badge "Risques" navigue via `routerLink`.
- [ ] Bandeau global `riskLevel`/`riskScore` rendu si présent.
- [ ] Cards stylées selon `riskLevel` global (3 variantes : faible/moyen/elevé).
- [ ] Composant `app-source-ref` rend les sources documents.
- [ ] État vide rendu si liste vide.
- [ ] Tests Jest U1-U5 verts.

## Plan de test minimal

- **Jest** :
  - U1 : chargement de la dernière version.
  - U2 : `gravityClass(level)` retourne la classe correcte pour FAIBLE / MOYEN / ELEVE.
  - U3 : état vide rendu si liste de risques vide.
  - U4 : `version` query-param respecté.
  - U5 : bandeau global rendu si `riskLevel` présent.

## Tables / endpoints / composants impactés

- **Route** : `app.routes.ts`
- **Composant** : `synthesis-risques/synthesis-risques.component.{ts,html,scss,spec.ts}`
- **Synthesis** : badge Risques → `route` ajouté.

## Hors périmètre

- Gravité par item de risque (le modèle actuel ne l'expose pas) — backlog si demande client.
- Filtrage / tri par gravité — backlog.

## Analyse de cohérence transversale

- **Préoccupations transversales** : route enfant — guards parents couvrent.
- **Nouveau pattern UI** : badge gravité par card, calqué sur `risk-badge--*` existant. Pas de duplication de logique : la classe est dérivée d'une simple méthode pure.
- **Impact par domaine métier** : transversal, identique 3 domaines × 2 pays.

## Contrat API

Aucun. Réutilise `getByVersion`.
