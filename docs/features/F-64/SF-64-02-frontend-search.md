# SF-64-02 — Frontend recherche full-text dans les synthèses

## Objectif
Fournir un écran `/search` permettant à l'avocat de saisir un mot-clé et d'afficher les dossiers dont les synthèses contiennent ce terme, avec des extraits contextuels et un lien vers chaque dossier.

---

## Comportement nominal

1. L'utilisateur clique sur "Recherche" dans la sidenav → navigue vers `/search`
2. Il saisit au moins 2 caractères dans le champ de recherche
3. Après 400 ms de debounce, `GET /api/v1/search?q=<terme>` est appelé
4. Les résultats s'affichent sous forme de cartes : une par dossier correspondant
   - Titre du dossier + lien vers `/case-files/:id`
   - Domaine juridique (badge)
   - Type d'analyse
   - Extraits correspondants (max 3, en italique avec le terme en gras)
   - Nombre de correspondances
5. Si 0 résultats : message "Aucun résultat pour « terme »"
6. Pendant le chargement : spinner centré
7. Si erreur HTTP : snackbar d'erreur

---

## Cas d'erreur

| Cas | Comportement attendu |
|-----|---------------------|
| Moins de 2 caractères | Aucun appel HTTP, résultats précédents effacés |
| Champ vide | Retour à l'état initial (pas d'appel) |
| Erreur 400 (q trop court côté backend) | Snackbar "Requête invalide" |
| Erreur 5xx | Snackbar "Erreur serveur, réessayez" |
| Timeout / réseau | Snackbar "Erreur réseau" |

---

## Critères d'acceptation

- [ ] Route `/search` accessible depuis la sidenav (icône `search`)
- [ ] Debounce 400 ms — pas d'appel pour chaque frappe
- [ ] Moins de 2 chars → aucun appel, zone résultats vide
- [ ] Résultats affichés en cartes Material avec lien vers le dossier
- [ ] Le terme recherché est mis en gras dans les extraits
- [ ] Spinner visible pendant le chargement
- [ ] "Aucun résultat" affiché si la liste est vide
- [ ] Snackbar d'erreur si l'appel échoue
- [ ] Conformité design system : couleurs, typo, layout shell

---

## Plan de test

### Unitaires (SearchComponent)
- U-01 : moins de 2 chars → `search()` non appelé
- U-02 : 2+ chars après debounce → `search()` appelé avec le terme
- U-03 : résultats reçus → `results` signal alimenté
- U-04 : erreur HTTP → snackbar affiché
- U-05 : nouveau terme avant fin du premier call → `switchMap` annule le précédent

### Composant (SearchResultCardComponent)
- U-06 : terme en gras dans l'extrait (pipe `highlightTerm`)
- U-07 : lien vers `/case-files/:id` correct

---

## Composants / fichiers impactés

| Fichier | Action |
|---------|--------|
| `frontend/src/app/search/search.component.ts` | Nouveau |
| `frontend/src/app/search/search.component.html` | Nouveau |
| `frontend/src/app/search/search.component.scss` | Nouveau |
| `frontend/src/app/search/search.component.spec.ts` | Nouveau |
| `frontend/src/app/core/services/synthesis-search.service.ts` | Nouveau |
| `frontend/src/app/core/services/synthesis-search.service.spec.ts` | Nouveau |
| `frontend/src/app/core/models/search.model.ts` | Nouveau |
| `frontend/src/app/shared/pipes/highlight-term.pipe.ts` | Nouveau |
| `frontend/src/app/app.routes.ts` | Ajout route `/search` |
| `frontend/src/app/layout/shell/shell.component.html` | Ajout lien sidenav "Recherche" |

---

## Hors périmètre

- Pagination des résultats (max 50 côté backend, pas de contrôle côté frontend en V2)
- Filtres par domaine juridique ou période
- Sauvegarde de l'historique des recherches
- Recherche full-text PostgreSQL avec indexation (pg_trgm) — reporté en V3
