# Mini-spec — F-118 / SF-118-01 Shell (nav groupée, avatar) + Header dossier

---

## Identifiant

`F-118 / SF-118-01`

## Feature parente

`F-118` — Refonte visuelle des écrans principaux

## Statut

`draft`

## Date de création

2026-04-07

## Branche Git

`feat/SF-118-01-shell-header-refonte`

---

## Objectif

Améliorer la navigation latérale (groupes, dividers, avatar initiales) et restructurer le header de la page dossier (hiérarchie visuelle, moins de surcharge).

---

## Comportement attendu

### Shell / Sidenav

1. Les liens de navigation sont regroupés par section avec des labels (mat-subheader) : "DOSSIERS", "OUTILS", "GESTION"
2. Des dividers (1px #E0E4EA) séparent les groupes
3. L'icône account_circle dans le header est remplacée par un avatar circulaire avec les initiales de l'utilisateur (fond navy, texte blanc)

### Header dossier (case-file-detail)

1. Restructuration sur 2 lignes :
   - Ligne 1 : bouton retour + titre + badge statut
   - Ligne 2 : actions (Modifier, Exporter ZIP, Partager, Clôturer/Réouvrir, Supprimer) groupées
2. Les actions destructives (Supprimer) sont séparées visuellement (à droite, couleur warn)

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Utilisateur sans nom/prénom | Avatar affiche "?" |
| Aucune action disponible (dossier clôturé) | Boutons grisés comme actuellement |

---

## Critères d'acceptation

- [ ] Sidenav a des labels de section (DOSSIERS, OUTILS, GESTION)
- [ ] Dividers entre les groupes de navigation
- [ ] Avatar initiales dans le header au lieu de l'icône générique
- [ ] Header dossier restructuré sur 2 lignes
- [ ] Actions destructives visuellement séparées
- [ ] Conforme au design system (couleurs, polices, espacements)
- [ ] Tous les tests existants restent verts

---

## Périmètre

### Hors scope

- Changement de couleurs du design system
- Refonte de la liste dossiers (SF-118-02)
- Refonte de la synthèse (SF-118-03)

---

## Technique

### Fichiers modifiés

| Fichier | Modification |
|---------|-------------|
| `shell.component.html` | Groupes nav, dividers, avatar initiales |
| `shell.component.scss` | Styles groupes, avatar |
| `shell.component.ts` | Computed initiales utilisateur |
| `case-file-detail.component.html` | Restructuration header 2 lignes |
| `case-file-detail.component.scss` | Styles header restructuré |

---

## Plan de test

- [ ] Tests existants shell restent verts
- [ ] Tests existants case-file-detail restent verts
- [ ] Tous les tests frontend restent verts

## Analyse d'impact

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [x] **Navigation / routing frontend** — modification du template shell (pas des routes)
- [ ] Aucune préoccupation transversale

### Composants impactés

| Composant | Impact | Test |
|-----------|--------|------|
| ShellComponent | Template + styles modifiés | Tests existants |
| CaseFileDetailComponent | Header restructuré | Tests existants |
