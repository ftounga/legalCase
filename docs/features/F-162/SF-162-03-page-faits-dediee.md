# SF-162-03 — Page Faits dédiée

## Objectif

Sortir le bloc **Faits** de la page synthèse pour le présenter sur une **page dédiée** sous forme de table groupée par thème (heuristique simple côté client : préfixes courants), et faire pointer le badge "Faits" de la grille SF-162-01 vers cette page.

## Contexte

Suite immédiate de SF-162-02 (Timeline dédiée). Réutilise le patron canonique `.detail-page` / `.detail-header` posé en SF-162-02.

## Comportement nominal

1. Nouvelle route `/case-files/:id/synthesis/faits` (lazy-loaded).
2. Composant `SynthesisFaitsComponent` standalone réutilisant le même header que la page Timeline (titre, retour, version, dossier).
3. Body : table groupée par thème :
   - Heuristique de groupement client : extraction du premier mot capital (1ʳᵉ majuscule + suffixe alphanumérique jusqu'au premier `:`) ou `Autres` si aucun préfixe détecté.
   - Sections collapsables (groupe avec compteur, liste numérotée des faits).
   - Composant `app-source-ref` réutilisé pour les références aux documents.
4. Badge "Faits" de la grille SF-162-01 → `routerLink` vers cette route.
5. État vide explicite si aucun fait.

## Cas d'erreur / edge cases

- Aucun fait → état vide explicite.
- Aucun préfixe détecté sur tous les faits → tous regroupés sous "Autres".
- Source orpheline → `app-source-ref` gère le fallback texte.

## Critères d'acceptation

- [ ] Route `/case-files/:id/synthesis/faits` enregistrée.
- [ ] Composant standalone créé avec le markup `.detail-page` réutilisé.
- [ ] Badge "Faits" navigue via `routerLink` (champ `route` du `SynthesisBadge`).
- [ ] Faits groupés client-side par thème (préfixe ou "Autres").
- [ ] Composant `app-source-ref` rend les sources comme dans la pile actuelle.
- [ ] État vide rendu si liste vide ou analyse inexistante.
- [ ] Tests Jest U1-U5 verts (chargement, groupement, états vides).
- [ ] DESIGN_SYSTEM.md respecté.

## Plan de test minimal

- **Jest** :
  - U1 : chargement de la dernière version.
  - U2 : groupement par thème — `groupedFaits` retourne 2 groupes pour `["Procédure: x", "Procédure: y", "Salaire: z"]`.
  - U3 : faits sans préfixe → groupe "Autres".
  - U4 : état vide rendu si timeline vide.
  - U5 : `version` query-param respecté.
- **Smoke E2E** : non requis.

## Tables / endpoints / composants impactés

- **Route** : `app.routes.ts`
- **Composant** : `synthesis-faits/synthesis-faits.component.{ts,html,scss,spec.ts}`
- **Synthesis** : badge "Faits" → `route` ajouté (champ déjà présent depuis SF-162-02).

## Hors périmètre

- Pages Points juridiques / Risques → SF-162-04 / SF-162-05.
- Édition / annotation des faits → backlog.

## Analyse de cohérence transversale

- **Préoccupations transversales** : ajout d'une route enfant — guards parents couvrent. Aucune autre.
- **Nouveau pattern UI** : ré-utilisation du patron `.detail-page` + `.detail-header` posé en SF-162-02 (pas de divergence). Heuristique de groupement par thème = nouveau pattern interne, propre à cette page (pas de duplication ailleurs aujourd'hui).
- **Impact par domaine métier** : transversal, identique 3 domaines × 2 pays.

## Contrat API

Aucun nouvel endpoint. Réutilise `getByVersion(caseFileId, version)` existant.
