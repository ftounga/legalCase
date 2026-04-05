# Mini-spec — F-111 / SF-111-03 : Refonte visuelle dashboard

## Identifiant
`F-111 / SF-111-03`

## Feature parente
`F-111` — Tableau de bord opérationnel workspace

## Statut
`in-progress`

## Date de création
`2026-04-05`

## Branche Git
`feat/SF-111-03-dashboard-redesign`

---

## Objectif
Remplacer la grille 2×2 plate par un dashboard "command center" lisible d'un coup d'œil : barre KPI en haut, layout 2 colonnes asymétrique, cartes riches avec couleurs d'urgence et de domaine. Aucun changement backend.

---

## Comportement attendu

### Structure visuelle

**Barre KPI (header)**
- 4 cartes horizontales : Dossiers ouverts / Délais urgents / Alertes checklist / Analyses récentes
- Couleur du compteur : rouge si urgentDeadlines > 0, orange si staleChecks > 0, bleu sinon
- Cliquable : scroll vers la section correspondante

**Colonne gauche (40%)**
- Section "Délais urgents" en premier — cartes avec fond rouge/orange selon criticité (≤3j = rouge, 4-7j = orange), icône horloge, badge J-X en gras, titre dossier en sous-titre
- Section "Alertes checklist" en dessous — cartes avec fond orange pâle, nombre de points NON_COMPLIANT, lien vers dossier

**Colonne droite (60%)**
- Section "Dossiers ouverts" — cards avec `border-left` coloré par domaine (vert Travail, doré Famille, navy Immigration), badge domaine, titre, lien vers dossier
- Section "Activité récente" en dessous — liste compacte avec icône, type analyse, dossier, date relative

**Empty states**
- Chaque section vide affiche une icône + message court + couleur neutre (pas de texte gris basique)

### Cas nominal
Les données sont identiques à SF-111-02 — seul le rendu HTML/CSS change.

### Cas d'erreur
Identique à SF-111-02 — message d'erreur centré avec bouton "Réessayer" (remplace le texte seul actuel).

---

## Critères d'acceptation

- [ ] Barre KPI visible en haut avec 4 compteurs
- [ ] Compteur délais urgents rouge si > 0
- [ ] Compteur alertes checklist orange si > 0
- [ ] Layout 2 colonnes (40/60) sur desktop, 1 colonne sur mobile
- [ ] Cartes délais : fond rouge si J≤3, fond orange si J≤7, badge J-X en gras
- [ ] Cartes dossiers : border-left coloré par domaine juridique
- [ ] Hover effect sur toutes les cartes cliquables
- [ ] Empty states stylisés (icône + message) pour chaque section
- [ ] État erreur avec bouton "Réessayer"
- [ ] Aucun changement backend (données identiques)
- [ ] Responsive mobile : 1 colonne, KPI scrollable horizontalement

---

## Périmètre

### Dans le scope
- `dashboard.component.html` — refonte complète
- `dashboard.component.scss` — refonte complète
- `dashboard.component.ts` — ajout `retry()` et `scrollTo()` helpers

### Hors scope
- Aucun changement backend
- Aucun changement de données (même `DashboardSummary`)
- Pagination des sections
- Filtres par domaine

---

## Technique

### Composants impactés
| Fichier | Changement |
|---------|-----------|
| `dashboard.component.html` | Refonte structure : KPI bar + 2 colonnes |
| `dashboard.component.scss` | Refonte complète styles |
| `dashboard.component.ts` | Ajout `retry()`, `scrollToSection()` |
| `dashboard.component.spec.ts` | Mise à jour sélecteurs de test cassés par refonte HTML |

### Couleurs (design system)
| Usage | Variable | Valeur |
|-------|----------|--------|
| Urgent critique (J≤3) | — | `#C53030` bg `#FFF5F5` |
| Urgent warning (J≤7) | — | `#B7791F` bg `#FFFBEB` |
| Domaine Travail | — | `#27AE60` |
| Domaine Famille | — | `#C9973A` |
| Domaine Immigration | — | `#1A3A5C` |
| KPI neutre | `--navy` | `#1A3A5C` |

---

## Plan de test

### Unitaires (DASH-UI)
- DASH-UI-06 : barre KPI affiche 4 compteurs
- DASH-UI-07 : compteur urgentDeadlines rouge si > 0
- DASH-UI-08 : carte délai a classe `urgent-critical` si J≤3
- DASH-UI-09 : carte dossier a `border-left` coloré selon domaine
- DASH-UI-10 : retry() re-déclenche l'appel HTTP
- DASH-UI-11 : empty state affiché si section vide

---

## Analyse d'impact

### Préoccupations transversales
- [ ] **Navigation / routing** — aucune nouvelle route, aucun guard modifié

### Composants impactés par la refonte HTML
| Composant | Impact |
|-----------|--------|
| `dashboard.component.spec.ts` | Sélecteurs CSS/texte à mettre à jour |
| Shell sidenav | Aucun impact |
| Routes | Aucun impact |

---

## Dépendances
- SF-111-01 (backend dashboard) — Done
- SF-111-02 (frontend dashboard v1) — Done
