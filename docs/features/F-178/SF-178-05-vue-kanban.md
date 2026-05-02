# Mini-spec — F-178 / SF-178-05 Vue kanban (read-only)

## Identifiant

`F-178 / SF-178-05`

## Feature parente

`F-178` — Visualiseur de backlog dans super-admin

## Statut

`ready`

## Date de création

2026-05-02

## Branche Git

`feat/SF-178-05-kanban-iso` (push vers `feat/SF-178-05-kanban`)

---

## Objectif

Ajouter une vue **kanban en lecture seule** sur l'écran `/super-admin/backlog` — colonnes par statut, cards compactes, toggle Liste / Kanban en haut. Permet une vision agrégée par état pour répondre rapidement à "combien de features par statut ?" et "qu'est-ce qui est bloqué ?".

---

## Comportement attendu

### Cas nominal

1. Sur la tab **Produit** uniquement (le kanban n'a pas de sens sur Marketing — moins de tâches, statuts différents et déjà visibles dans la liste). La tab Marketing reste en mode liste.
2. En haut de la tab Produit, un toggle `<mat-button-toggle-group>` **Liste | Kanban**.
3. Mode Liste = mode actuel (SF-178-03), comportement inchangé.
4. Mode Kanban : 5 colonnes correspondant aux statuts produit principaux : `READY` (Ready to dev) | `IN_PROGRESS` (En cours) | `BLOCKED` (Bloqué) | `DONE` (Terminée) | `PLANNED` (À planifier). Les statuts secondaires (`PARTIAL`, `ABSORBED`, `UNKNOWN`) sont regroupés dans une 6e colonne "Autres" affichée seulement si elle contient des cards (sinon masquée).
5. Chaque colonne affiche : header (label + count), liste de cards verticales scrollables.
6. Chaque card : code (JetBrains Mono) + title (1 ligne tronquée), badge domaine compact, badge priorité compact. Pas de description (kanban = vue d'ensemble, le détail est dans le drawer SF-178-04).
7. Au clic sur une card → ouvre le `BacklogFeatureDetailDialogComponent` (réutilisé SF-178-04, pas de duplication).
8. Les filtres status/domain/priority/search **restent applicables** au-dessus du kanban — ils filtrent ce qui descend dans les colonnes. Note : si l'utilisateur sélectionne un filtre status, seules les cards de ce statut sont retournées par l'API ; les autres colonnes apparaissent vides.
9. Les données affichées sont **toutes les pages cumulées** disponibles côté backend — V1 : on charge `size=200` (max paginator) pour avoir un grand échantillon dans le kanban. Plus tard : pagination kanban (hors scope V1).
10. La pagination MatPaginator est **masquée en mode kanban** (la pagination "page X / Y" n'a pas de sens dans cette vue ; on charge un grand batch).

### Cas d'erreur

| Situation | Comportement |
|-----------|--------------|
| 500 sur features | snackbar erreur, kanban vide |
| Bascule pendant chargement | la requête en cours s'applique à la nouvelle vue (pas de double appel) |
| Filtre + bascule mode | reload features avec filtres en mode kanban (size 200) |

### Hors scope V1

- Drag-and-drop entre colonnes (gouvernance Étape 7 : édition UI interdite, MD = source de vérité)
- Pagination dans les colonnes (V2)
- Animations transition Liste ↔ Kanban
- Sauvegarde de la préférence du mode (toujours Liste au refresh)
- Multi-tri ou drag des colonnes
- Vue kanban sur la tab Marketing

---

## Analyse de cohérence transversale

- [x] **Autres outils / pays / domaines** : N/A — feature transversale super-admin.
- [x] **Pattern UI mat-button-toggle-group** : déjà utilisé ailleurs (vérification : `MatButtonToggleModule` standard Material). Pas de nouveau pattern.
- [x] **Pattern card** : composant local au scope kanban, simple template inline (le badge composant SF-178-03 est déjà réutilisé pour le statut). Pas besoin d'extraire un nouveau composant partagé V1 (cards kanban = présentation interne au composant principal).
- [x] **Pattern dialog détail** : réutilise `BacklogFeatureDetailDialogComponent` créé SF-178-04 — aucune duplication.

### Cas spécifique : nouveau pattern UI ou service partagé

- Aucun nouveau pattern réutilisable. Le toggle Liste/Kanban est local au composant principal.

### Décision

- [x] Étendu à toutes les cibles applicables (réutilise badge SF-178-03 + dialog SF-178-04).

---

## Critères d'acceptation

- [ ] Toggle `<mat-button-toggle-group>` Liste | Kanban sur tab Produit (Liste par défaut)
- [ ] Mode Liste : tableau actuel inchangé, MatPaginator visible
- [ ] Mode Kanban : 5 colonnes principales (READY / IN_PROGRESS / BLOCKED / DONE / PLANNED) + colonne "Autres" si non vide
- [ ] Chaque card kanban : code + title + badge domaine + badge priorité + badge statut
- [ ] Click sur card kanban ouvre `BacklogFeatureDetailDialogComponent` (réutilisé)
- [ ] Filtres applicables en mode kanban
- [ ] MatPaginator masquée en mode kanban
- [ ] Mode kanban charge `size=200` (vs 50 en mode liste) pour avoir un échantillon large
- [ ] Tab Marketing inchangée (reste en mode liste — toggle absent)
- [ ] Tests Jest (≥ 5) : toggle change mode, kanban groupe par statut, click ouvre dialog, filtre fonctionne en kanban, paginator masqué en kanban
- [ ] Suite Jest complète verte
- [ ] Build frontend vert

---

## Plan de test

### Tests Jest ≥ 5

- T-01 : `viewMode` = 'list' par défaut. Toggle → bascule à 'kanban'.
- T-02 : Bascule en mode kanban → reload features avec `size=200`.
- T-03 : `groupedFeatures()` retourne un map status → array (READY/IN_PROGRESS/BLOCKED/DONE/PLANNED + "OTHER" pour PARTIAL/ABSORBED/UNKNOWN).
- T-04 : Click sur card kanban appelle `openFeatureDetail(code)` → ouvre dialog (mêmes spies que SF-178-04).
- T-05 : `showOtherColumn()` true seulement si la colonne OTHER contient des cards.
- T-06 : Filtre status appliqué en kanban → reload avec filtre.

### Isolation workspace

N/A — feature super-admin transversale.

---

## Analyse d'impact

- [x] Aucune préoccupation transversale touchée.
- Modifications limitées au composant principal `super-admin-backlog.component.ts/html/scss(/spec.ts)`. Pas d'impact sur les autres SF.
