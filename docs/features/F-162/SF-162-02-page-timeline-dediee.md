# SF-162-02 — Page Timeline dédiée

## Objectif

Sortir le bloc **Chronologie** de la page synthèse pour le présenter sur une **page dédiée** avec une visualisation horizontale (rail des dates) plutôt que la liste verticale actuelle, et faire pointer le badge "Timeline" de la grille SF-162-01 vers cette page.

## Contexte

SF-162-01 a posé la grille de badges en tête de synthèse, avec un comportement temporaire de scroll-to-block. SF-162-02 livre la première **page riche dédiée** — la timeline — qui sert de patron visuel pour SF-162-03 à SF-162-05 (Faits / Points juridiques / Risques).

## Comportement nominal

1. Nouvelle route `/case-files/:id/synthesis/timeline` (lazy-loaded comme la synthèse).
2. La page charge la dernière analyse `DONE` du dossier (ou un `version` query-param si fourni) via `caseAnalysisService.getByVersion`.
3. Header : titre "Chronologie", lien retour "← Retour à la synthèse" (vers `/case-files/:id/synthesis`), nom du dossier en sous-titre, libellé de la version.
4. Body : rail horizontal scrollable des événements timeline, ordre chronologique tel que reçu :
   - Carte par événement : date en haut (JetBrains Mono), événement en dessous (Inter).
   - Connecteurs visuels (ligne horizontale, points repères) — design system navy + or.
5. Si la timeline est vide → état vide explicite "Aucun événement chronologique détecté pour cette analyse."
6. Le badge "Timeline" de la grille SF-162-01 navigue vers cette route au lieu de scroller.

## Cas d'erreur / edge cases

- Dossier inexistant → état vide standard, retour aux dossiers.
- Aucune analyse `DONE` → état vide explicite.
- Mobile : rail horizontal reste scrollable (overflow-x), cartes mini 240px.
- ID query param `version=N` invalide → fallback sur la dernière version.

## Critères d'acceptation

- [ ] Route `/case-files/:id/synthesis/timeline` enregistrée dans `app.routes.ts`.
- [ ] Composant `SynthesisTimelineComponent` standalone créé.
- [ ] Le badge Timeline de la grille SF-162-01 utilise désormais `routerLink` vers cette route (et non `scrollToBlock`).
- [ ] La timeline s'affiche en mode horizontal (overflow-x scrollable), ordre chronologique préservé.
- [ ] Lien retour vers `/case-files/:id/synthesis` fonctionnel.
- [ ] État vide rendu si timeline absent ou analyse inexistante.
- [ ] Tests Jest U1-U5 verts (chargement, ordre, état vide, retour).
- [ ] DESIGN_SYSTEM.md respecté (navy/or, JetBrains Mono pour dates, Inter pour libellés).

## Plan de test minimal

- **Jest** :
  - U1 : chargement de la dernière version (`getByVersion` appelé avec la version max).
  - U2 : événements rendus dans l'ordre reçu (pas de re-tri côté client).
  - U3 : état vide affiché si `timeline` vide ou `synthesis` null.
  - U4 : `version` query-param respecté (si fourni).
  - U5 : badge Timeline de la synthèse rend `[routerLink]="['/case-files', cfId, 'synthesis', 'timeline']"`.
- **Smoke E2E** : non requis (route lazy-loaded, pas d'auth/guard nouveau).

## Tables / endpoints / composants impactés

- **Route** : `app.routes.ts`
- **Composant** : `frontend/src/app/case-files/synthesis-timeline/synthesis-timeline.component.{ts,html,scss,spec.ts}`
- **Composant existant modifié** : `synthesis.component.{ts,html}` — badge Timeline = routerLink.
- **Aucun nouvel endpoint, aucune migration**.

## Hors périmètre

- Pages dédiées Faits / Points juridiques / Risques → SF-162-03 / 04 / 05.
- Popups blocs courts → SF-162-06.
- Pas de filtre / recherche dans la timeline (futur backlog si besoin).

## Analyse de cohérence transversale

- **Préoccupations transversales** : navigation/routing — ajout d'une route enfant. Les guards existants (`AuthGuard` au niveau parent dans `app.routes.ts`) couvrent automatiquement la nouvelle route. Pas de redirection nouvelle, pas de garde modifiée.
- **Nouveau pattern UI** : page dédiée riche avec header (titre + retour + meta version) + body. Sera repris par SF-162-03/04/05 — donc ce premier composant sert de **patron canonique**. Pour éviter la dette de convergence, je documente le patron dans le commit/PR de SF-162-02 et les SFs suivantes le ré-implémentent à l'identique (header markup + tokens SCSS).
- **Impact par domaine métier** : transversal, identique 3 domaines × 2 pays.

## Contrat API

Aucun nouvel endpoint. Réutilise `getByVersion(caseFileId, version)` existant.
