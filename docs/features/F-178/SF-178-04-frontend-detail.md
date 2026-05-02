# Mini-spec — F-178 / SF-178-04 Frontend détail (drawer/modal)

## Identifiant

`F-178 / SF-178-04`

## Feature parente

`F-178` — Visualiseur de backlog dans super-admin

## Statut

`ready`

## Date de création

2026-05-02

## Branche Git

`feat/SF-178-04-frontend-detail`

---

## Objectif

Permettre à l'utilisateur super-admin d'ouvrir le détail d'une feature (description complète + métadonnées + découpage SF) sans quitter l'écran liste — via un `MatDialog` ouvert au clic sur une ligne du tableau Produit.

---

## Comportement attendu

### Cas nominal — Tab Produit

1. L'utilisateur clique sur une ligne du tableau Produit. La ligne entière a `cursor: pointer`.
2. Un `MatDialog` s'ouvre, largeur 880px max (responsive). Spinner pendant le chargement.
3. Le dialog appelle `BacklogAdminService.getFeatureDetail(code)` → `BacklogFeatureDetail`.
4. Il affiche : header (code + titre), badges (statut + domaine + priorité + cible), description dans un `<pre>` (max-height 360px overflow), ligne source (`docs/PRODUCT_SPEC.md (ligne X) — Mis à jour le …`), liste des subfeatures (badge + description tronquée 240 chars + path attendu mini-spec).
5. Fermeture via bouton "Fermer", touche Escape ou backdrop (Material par défaut).

### Cas d'erreur

| Situation | Comportement |
|-----------|--------------|
| 404 sur `getFeatureDetail` | dialog affiche "Feature introuvable. Le code existe peut-être uniquement en MD non encore parsé." + bouton fermer |
| 403 | redirect `/case-files` (handler global) |
| 500 / network | dialog ferme automatiquement, snackbar erreur |

### Hors scope V1

- Tab Marketing : pas de drawer V1 (pas d'endpoint `getMarketingTaskDetail` côté backend SF-178-01).
- Rendu Markdown formaté → V1 affiche brut dans `<pre>`.
- Édition depuis le dialog → interdite par règle gouvernance Étape 7 CLAUDE.md.
- Lien GitHub blame → V1 affiche juste le path.

---

## Analyse de cohérence transversale

- [x] **Autres outils / pays / domaines** : N/A — feature transversale super-admin interne.
- [x] **Pattern UI MatDialog** : déjà utilisé (`SuperAdminConfirmDialogComponent`, `DecisionToolModalService` F-177). Pas de nouveau pattern.
- [x] **Pattern badge** : réutilise `<app-backlog-status-badge>` créé en SF-178-03.
- [x] **Pattern row click** : convention `cursor: pointer` + `(click)` sur `mat-row`. Déjà utilisé sur `case-files-list`.

### Décision

- [x] Étendu à toutes les cibles applicables (feature isolée, badge réutilisé, pas de nouveau pattern).

---

## Critères d'acceptation

- [ ] Composant `BacklogFeatureDetailDialogComponent` créé (standalone, dans `super-admin/backlog/feature-detail/`)
- [ ] Click sur ligne du tableau Produit ouvre le dialog avec `code` en data
- [ ] Dialog charge `getFeatureDetail(code)` au open et affiche header / badges / description / source / subfeatures
- [ ] Tableau Marketing reste non-cliquable (hors scope V1)
- [ ] 404 → message "Feature introuvable" + bouton fermer
- [ ] 500 / network → dialog ferme + snackbar erreur
- [ ] Tests Jest (≥ 5) sur le dialog + ≥ 1 sur composant principal + ≥ 2 sur path helper
- [ ] Suite Jest complète verte
- [ ] Build frontend vert

---

## Plan de test

Tests Jest dialog (≥ 6) : init/render/subfeatures/404/500/truncate. Tests Jest composant principal (≥ 1) : open dialog. Tests path helper (≥ 3). Isolation workspace : N/A.

---

## Analyse d'impact

- [x] Aucune préoccupation transversale touchée.
