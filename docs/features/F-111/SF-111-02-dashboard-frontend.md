# Mini-spec — F-111 / SF-111-02 : DashboardComponent Angular

## Identifiant
`F-111 / SF-111-02`

## Feature parente
`F-111` — Tableau de bord opérationnel workspace

## Statut
`in-progress`

## Date de création
`2026-04-04`

## Branche Git
`feat/SF-111-02-dashboard-frontend`

---

## Objectif
Créer le composant Angular `/dashboard` qui affiche les données agrégées du workspace (dossiers ouverts, délais urgents, alertes checklist, activité récente) en appelant `GET /api/v1/dashboard`, et le rendre accessible depuis le sidenav comme page d'accueil après connexion.

---

## Comportement attendu

### Cas nominal
1. L'utilisateur se connecte → redirigé vers `/dashboard` (au lieu de `/case-files`).
2. Le composant appelle `GET /api/v1/dashboard` à l'init.
3. Pendant le chargement : spinner centré.
4. Affichage de 4 sections :
   - **Dossiers ouverts** : liste de cartes (titre, domaine, statut) + compteur total. Bouton "Voir tous les dossiers" → `/case-files`.
   - **Délais urgents** : liste des délais (libellé, dossier, date échéance, badge J-X coloré). Vide → "Aucun délai urgent."
   - **Alertes checklist** : dossiers avec NON_COMPLIANT en retard (titre, nombre de points). Vide → "Aucune alerte."
   - **Activité récente** : 5 dernières analyses DONE (dossier, type, date). Vide → "Aucune activité."
5. Lien "Tableau de bord" dans le sidenav (icône `dashboard`), en première position.

### Cas d'erreur
| Situation | Comportement |
|-----------|-------------|
| Erreur 5xx backend | Message d'erreur affiché à la place du contenu |

---

## Critères d'acceptation

- [x] Route `/dashboard` fonctionnelle, accessible depuis le sidenav
- [x] Redirection post-connexion vers `/dashboard`
- [x] Spinner pendant le chargement
- [x] Section "Dossiers ouverts" : cartes + compteur + lien `/case-files`
- [x] Section "Délais urgents" : badge J-X coloré (rouge ≤ 3j, orange ≤ 7j)
- [x] Section "Alertes checklist" : groupées par dossier avec compteur
- [x] Section "Activité récente" : type et date des 5 dernières analyses
- [x] Lien sidenav "Tableau de bord" icône `dashboard`, en première position
- [x] Message vide pour chaque section si liste vide

---

## Périmètre

### Hors scope
- Uploads dans l'activité récente
- Filtres / pagination
- Polling SSE
- Graphiques / métriques

---

## Technique

### Nouveaux composants / services
- `DashboardComponent` (`/dashboard`)
- `DashboardService` Angular
- `dashboard.model.ts` (interfaces)

### Routes modifiées
- `app.routes.ts` : ajout route `/dashboard`
- Redirection post-login → `/dashboard`
- `ShellComponent` : lien sidenav "Tableau de bord"

---

## Plan de test

### Frontend — unitaires (DASH-UI)
- [x] DASH-UI-01 : spinner visible pendant chargement
- [x] DASH-UI-02 : dossiers ouverts affichés correctement
- [x] DASH-UI-03 : délai urgent rouge si ≤ 3j, orange sinon
- [x] DASH-UI-04 : message "Aucun délai urgent" si liste vide
- [x] DASH-UI-05 : activité récente affichée

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Navigation / routing** — nouvelle route `/dashboard`, modification redirection post-login

| Composant | Impact potentiel | Non-régression |
|-----------|-----------------|----------------|
| `app.routes.ts` | nouvelle route | Routes existantes inchangées |
| `ShellComponent` | nouveau lien sidenav | Tests existants à vérifier |
| Redirection post-login | `AuthCallbackComponent` | Smoke test navigation |

---

## Dépendances
- SF-111-01 — Done ✓
