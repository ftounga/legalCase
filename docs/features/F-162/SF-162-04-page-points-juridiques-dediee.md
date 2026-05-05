# SF-162-04 — Page Points juridiques dédiée

## Objectif

Sortir le bloc **Points juridiques** de la page synthèse vers une **page dédiée** avec des cards expandables (extrait court par défaut, intégralité au clic), et faire pointer le badge "Points juridiques" de la grille SF-162-01 vers cette page.

## Contexte

3ᵉ page dédiée de F-162 (après Timeline et Faits). Réutilise le patron canonique `.detail-page` posé en SF-162-02.

## Comportement nominal

1. Nouvelle route `/case-files/:id/synthesis/points-juridiques` (lazy-loaded).
2. Composant `SynthesisPointsJuridiquesComponent` standalone.
3. Header identique aux pages Timeline / Faits (titre, retour, version, dossier).
4. Body : cards expandables — préview (200 caractères max + ellipse) par défaut, extension complète au clic. État géré par signal local par card (`expandedIds: Set<number>`).
5. Composant `app-source-ref` réutilisé pour les sources documents.
6. Badge "Points juridiques" → `routerLink` vers cette route.
7. État vide explicite si liste vide.

## Cas d'erreur / edge cases

- Texte plus court que 200 caractères → pas d'ellipse, pas d'expand button.
- Aucun point juridique → état vide explicite.
- Dossier inexistant → état vide standard.

## Critères d'acceptation

- [ ] Route `/case-files/:id/synthesis/points-juridiques` enregistrée.
- [ ] Composant standalone réutilise patron `.detail-page`.
- [ ] Badge "Points juridiques" navigue via `routerLink`.
- [ ] Cards : preview tronqué + bouton "Voir plus" / "Voir moins" si texte > 200 chars.
- [ ] Sources documents rendues via `app-source-ref`.
- [ ] État vide explicite.
- [ ] Tests Jest U1-U5 verts.

## Plan de test minimal

- **Jest** :
  - U1 : chargement de la dernière version.
  - U2 : `isLong(text)` retourne `true` si > 200 chars, `false` sinon.
  - U3 : `toggle(index)` ajoute/retire l'index dans `expandedIds`.
  - U4 : état vide rendu si liste vide.
  - U5 : `version` query-param respecté.
- **Smoke E2E** : non requis.

## Tables / endpoints / composants impactés

- **Route** : `app.routes.ts`
- **Composant** : `synthesis-points-juridiques/synthesis-points-juridiques.component.{ts,html,scss,spec.ts}`
- **Synthesis** : badge Points juridiques → `route` ajouté.

## Hors périmètre

- Page Risques → SF-162-05.
- Annotation / favoris sur les points → backlog.

## Analyse de cohérence transversale

- **Préoccupations transversales** : ajout route enfant — guards parents couvrent.
- **Nouveau pattern UI** : preview tronqué + expand. Pas réutilisé ailleurs aujourd'hui — pattern propre à cette page. Si SF-162-05 (Risques) en a besoin, on factorisera à ce moment-là.
- **Impact par domaine métier** : transversal, identique 3 domaines × 2 pays.

## Contrat API

Aucun. Réutilise `getByVersion` existant.
