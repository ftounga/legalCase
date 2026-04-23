# Mini-spec — F-154 / SF-154-01 Création workspace depuis le switcher

## Identifiant · `F-154 / SF-154-01`
## Date · `2026-04-23` · Branche · `feat/SF-154-01-workspace-create-from-switcher`

## Objectif
Permettre à tout utilisateur de créer un nouveau workspace (nom, pays, domaine métier) directement depuis le switcher de la toolbar, avec switch automatique vers le nouveau workspace après création.

## Contexte
Backend déjà en place :
- `POST /api/v1/workspaces` (WorkspaceController) avec `{name, legalDomain, country}`
- `POST /api/v1/workspaces/:id/switch` pour basculer
- `GET /api/v1/workspaces` pour la liste
- `WorkspaceService.createWorkspace/switchWorkspace` côté frontend

Aujourd'hui la création est possible uniquement **via le parcours d'onboarding** au premier login. Un utilisateur qui a déjà un workspace ne peut plus en créer d'autre via l'UI — alors que la fonction backend existe et supporte les workspaces multiples (cf. switcher dans ShellComponent).

## Comportement nominal

### A — Menu switcher toujours visible
Aujourd'hui : condition `@if (workspaces().length > 1)` → bouton caché si 1 seul workspace.
Demain : bouton visible **dès qu'il y a ≥ 1 workspace** pour exposer l'action Créer.

### B — Option "Créer un nouveau workspace"
En bas du menu déroulant existant, avec séparateur visuel :
```
────────────────
➕ Créer un nouveau workspace
```

### C — Dialog `WorkspaceCreateDialogComponent`
Nouveau composant standalone :
- Champ **Nom** (mat-form-field outline, requis)
- Sélecteur **Domaine métier** (3 cards : Droit du travail / Immigration / Famille) — même pattern UX que `DomainPickerDialog` de l'onboarding, à extraire en composant partagé ou dupliquer proprement
- Sélecteur **Pays** (mat-select FRANCE / BELGIQUE)
- Boutons `Annuler` / `Créer` (disabled tant que formulaire invalide)
- Spinner pendant la requête, erreur → MatSnackBar

### D — Flux après création
1. `WorkspaceService.createWorkspace(name, legalDomain, country)` → POST
2. À succès : `switchWorkspace(newWs.id)` pour rendre le nouveau actif
3. `notifyWorkspaceSwitched()` pour déclencher le reload des données dépendantes
4. `router.navigate(['/case-files'])`
5. Snackbar succès

## Critères d'acceptation
- [ ] `ShellComponent` : menu switcher affiché dès 1 workspace (au lieu de ≥ 2)
- [ ] Menu contient en bas l'option `Créer un nouveau workspace` avec séparateur
- [ ] Clic ouvre le nouveau `WorkspaceCreateDialogComponent`
- [ ] Dialog : 3 champs (nom, domaine, pays), validation, bouton Créer disabled tant qu'incomplet
- [ ] Succès : dialog ferme, switch vers nouveau workspace, redirect `/case-files`, snackbar
- [ ] Erreur 400/500 : reste sur dialog, affiche erreur
- [ ] Tests unitaires : dialog (formulaire, validation, submit succès, submit erreur) + ShellComponent (menu visible dès 1 ws, clic création appelle dialog + switch)
- [ ] 1135+ tests frontend verts

## Plan de test minimal
- U-01 (dialog) : formulaire vide → bouton Créer désactivé
- U-02 (dialog) : formulaire complet → bouton Créer activé
- U-03 (dialog) : submit succès → `WorkspaceService.createWorkspace` + `switchWorkspace` appelés, dialog fermé avec le nouveau workspace
- U-04 (dialog) : submit erreur 500 → snackbar, dialog reste ouvert
- U-05 (shell) : 1 seul workspace → menu switcher visible, option Créer présente
- U-06 (shell) : 2 workspaces → menu affiche les 2 + option Créer
- U-07 (shell) : clic Créer → ouvre dialog, après fermeture succès → recharge liste + navigate `/case-files`

## Tables / endpoints / composants impactés
### Frontend
- Nouveau : `layout/workspace-create-dialog/workspace-create-dialog.component.(ts|html|scss|spec.ts)`
- Modifié : `layout/shell/shell.component.(ts|html|spec.ts)`

### Pas impacté
- Backend : endpoints déjà en place
- DB : aucune migration
- Onboarding : pas touché (parcours premier login inchangé)

## Impact par domaine métier (FR + BE × 3 domaines)
| Domaine | Impact |
|---|---|
| Transversal | Feature d'infrastructure — permet de **créer** un workspace dans n'importe lequel des 3 domaines × 2 pays. Aucune adaptation spécifique. |

## Parité des domaines métier
N/A — feature d'infrastructure, pas un outil décisionnel métier (pas de niveau 5/6/7).

## Analyse de cohérence transversale
| Cible | Évaluation | Classement |
|---|---|---|
| `DomainPickerDialog` onboarding | Contient déjà le UX pattern cards-domaine. V1 : duplication délibérée pour ne pas coupler onboarding et switcher. Si feedback utilisateur, extraire un composant partagé `<app-domain-picker>` en SF-154-02. | SF parallèle possible (V1 : dupliquer) |
| `WorkspaceService` (createWorkspace, switchWorkspace, listWorkspaces, notifyWorkspaceSwitched) | Réutilisé tel quel | Intégré |
| Workspace switcher UI actuel | Étendu (toujours visible + option Créer) | Intégré |
| Plans / limites (F-16) | Aucun gate sur la création actuellement. Si plus tard on veut limiter à N workspaces par utilisateur, ce sera un ajout backend séparé. | Non applicable V1 |

## Préoccupations transversales
- **Auth / Principal** : aucun impact (réutilise `@AuthenticationPrincipal` existant)
- **Workspace context** : après création + switch, `WorkspaceService.notifyWorkspaceSwitched()` déclenche le reload automatique (pattern déjà en place dans le switcher)
- **Plans / limites** : aucun gate V1
- **Navigation / routing** : redirect vers `/case-files` après création (le guard auth est déjà satisfait puisque l'utilisateur est connecté)

## Hors scope
- Suppression d'un workspace (pas demandé, cas sensible en termes de données)
- Renommage / modification d'un workspace depuis le switcher (passe par workspace-admin existant)
- Invitation simultanée de membres au nouveau workspace (reste à faire depuis workspace-members)
- Limite du nombre de workspaces par utilisateur (à évaluer dans F-16 plus tard)
- Extraction d'un composant `<app-domain-picker>` partagé entre onboarding et switcher (SF parallèle si besoin futur)
