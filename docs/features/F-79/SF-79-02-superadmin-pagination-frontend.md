# SF-79-02 — Pagination frontend super-admin (workspaces et utilisateurs)

**Feature parente :** F-79 — Pagination super-admin
**Statut :** En cours

## Objectif

Afficher les tableaux workspaces et utilisateurs avec `MatPaginator` dans le composant `/super-admin`, en consommant les endpoints paginés backend.

## Comportement nominal

- Le tableau workspaces affiche 20 lignes max par page avec `MatPaginator`
- Le tableau utilisateurs affiche 20 lignes max par page avec `MatPaginator`
- Changer de page ou de taille → nouvel appel API avec `?page=&size=`
- `totalElements` backend affiché dans le paginator
- Page 0 / taille 20 au chargement initial

## Cas d'erreur

- Erreur API sur changement de page → snackbar erreur, tableau inchangé

## Critères d'acceptation

1. `MatPaginator` visible sous chaque tableau
2. `totalElements` reflète le compteur backend
3. Navigation page suivante/précédente déclenche un appel API
4. Changement de `pageSize` déclenche un appel API avec `size=` mis à jour
5. Aucun chargement de toutes les données en mémoire (pas de `findAll` côté frontend)

## Plan de test

- T-07 : changement de page workspaces → `listWorkspaces` appelé avec `{ page: 1, size: 20 }`
- T-08 : changement de taille utilisateurs → `listUsers` appelé avec `{ page: 0, size: 5 }`
- T-09 : `totalElements` affiché dans le paginator workspaces
- T-10 : erreur sur changement de page → snackbar + tableau inchangé

## Composants impactés

- `super-admin.component.ts` — signals `workspacePage`, `usersPage`, handlers paginator
- `super-admin.component.html` — `<mat-paginator>` sous chaque `<mat-table>`
- `super-admin.service.ts` — `listWorkspaces(page, size)` et `listUsers(page, size)` avec query params
- `super-admin.model.ts` — type `PageResponse<T>`

## Endpoints impactés

- `GET /api/v1/super-admin/workspaces?page=&size=`
- `GET /api/v1/super-admin/users?page=&size=`

## Hors périmètre

- Tri côté serveur
- Filtrage / recherche
