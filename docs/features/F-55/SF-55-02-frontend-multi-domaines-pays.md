# Mini-spec — F-55 / SF-55-02 Frontend multi-domaines et pays

> Template : copier ce fichier, renommer en `SF-XX-YY-nom.md`, placer dans `docs/features/FEAT-XX/`
> Ce document doit être validé AVANT de démarrer le dev.

---

## Identifiant

`F-55 / SF-55-02`

## Feature parente

`F-55` — Multi-domaines et pays workspace

## Statut

`in-progress`

## Date de création

2026-03-26

## Branche Git

`feat/SF-55-02-frontend-multi-domaines-pays`

---

## Objectif

Mettre à jour le frontend pour exposer les 3 domaines juridiques (DROIT_DU_TRAVAIL, DROIT_IMMIGRATION, DROIT_FAMILLE) avec une couleur distinctive par domaine, et ajouter le sélecteur de pays (FRANCE / BELGIQUE) dans le dialog d'onboarding.

---

## Comportement attendu

### Cas nominal

1. **Modèle** : `Workspace` Angular enrichi avec `legalDomain` et `country`.
2. **Service** : `WorkspaceService.createWorkspace(name, legalDomain, country)` passe le pays au backend.
3. **Dialog onboarding** (`DomainPickerDialogComponent`) :
   - Affiche 3 tuiles : DROIT_DU_TRAVAIL (vert `#27AE60`), DROIT_IMMIGRATION (bleu `#1A3A5C`), DROIT_FAMILLE (or `#C9973A`) — toutes actives.
   - Affiche un sélecteur de pays (FRANCE / BELGIQUE) en dessous de la sélection domaine.
   - DROIT_IMMOBILIER retiré.
   - "Confirmer" activé seulement si domaine **et** pays sont sélectionnés.
   - Retourne `{ legalDomain: string, country: string }`.
4. **OnboardingComponent** : consomme le `{ legalDomain, country }` retourné par le dialog.
5. **Header** (`ShellComponent`) : l'icône du workspace actif affiche la couleur du domaine (dot coloré ou icône colorée) en utilisant les 3 couleurs de la palette.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Dialog fermé sans sélection domaine/pays | `confirm()` bloqué (bouton désactivé) |
| API 400 (validation backend) | snackBar d'erreur existant — pas de changement |

---

## Critères d'acceptation

- [ ] `Workspace` model contient `legalDomain: string` et `country: string`
- [ ] `WorkspaceService.createWorkspace()` passe `{ name, legalDomain, country }` au POST
- [ ] DomainPickerDialog affiche 3 tuiles colorées toutes actives (pas de tuile "bientôt disponible")
- [ ] Couleurs des tuiles : travail → `#27AE60`, immigration → `#1A3A5C`, famille → `#C9973A`
- [ ] Un sélecteur de pays FRANCE / BELGIQUE est présent dans le dialog
- [ ] Le bouton "Confirmer" est désactivé tant que domaine et pays ne sont pas sélectionnés
- [ ] Le dialog retourne `{ legalDomain, country }` et l'onboarding crée le workspace avec les deux champs
- [ ] L'icône de workspace dans le header reflète la couleur du domaine du workspace actif
- [ ] DROIT_IMMOBILIER n'apparaît plus nulle part dans l'UI
- [ ] Tests unitaires passent (≥ 10 tests : dialog spec + onboarding spec + workspace service spec)

---

## Périmètre

### Hors scope (explicite)

- Affichage du pays dans le header (nom du workspace uniquement, pas de flag)
- Modification du domaine/pays d'un workspace existant (aucun écran de settings workspace prévu)
- Traduction FR/BE des libellés juridiques (pas de i18n)
- Couleur de domaine dans les listes de dossiers (hors scope SF-55-02)

---

## Technique

### Endpoints

Aucun nouvel endpoint. Modification de l'appel existant :
- `POST /api/v1/workspaces` : ajout de `country` dans le body

### Tables impactées

Aucune (backend déjà fait en SF-55-01).

### Migration Liquibase

- [x] Non applicable

### Composants Angular impactés

| Composant | Modification |
|-----------|-------------|
| `workspace.model.ts` | Ajout `legalDomain` et `country` |
| `WorkspaceService` | `createWorkspace(name, legalDomain, country)` |
| `DomainPickerDialogComponent` | 3 tuiles colorées actives + sélecteur pays + retour `{legalDomain, country}` |
| `OnboardingComponent` | Consomme `{legalDomain, country}` |
| `ShellComponent` | Icône workspace colorée selon domaine |

---

## Plan de test

### Tests unitaires

- [ ] `DomainPickerDialogComponent` — 3 tuiles rendues et toutes actives
- [ ] `DomainPickerDialogComponent` — bouton Confirmer désactivé sans sélection pays
- [ ] `DomainPickerDialogComponent` — bouton Confirmer actif avec domaine + pays sélectionnés
- [ ] `DomainPickerDialogComponent` — confirm() retourne `{ legalDomain, country }`
- [ ] `DomainPickerDialogComponent` — DROIT_IMMOBILIER absent
- [ ] `OnboardingComponent` — appelle createWorkspace avec legalDomain et country
- [ ] `WorkspaceService` — createWorkspace envoie body `{ name, legalDomain, country }`
- [ ] `ShellComponent` — couleur domaine DROIT_DU_TRAVAIL = `#27AE60`
- [ ] `ShellComponent` — couleur domaine DROIT_IMMIGRATION = `#1A3A5C`
- [ ] `ShellComponent` — couleur domaine DROIT_FAMILLE = `#C9973A`

### Tests d'intégration

Non applicable (couvert par WorkspaceControllerIT en SF-55-01).

### Isolation workspace

- [x] Non applicable — modification purement UI.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Workspace context** — `Workspace` model enrichi avec `legalDomain`/`country`, consommé par `ShellComponent`

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|----------------|------------------------------|
| `ShellComponent` | Lecture de `workspace().legalDomain` — `null` si ancienne session | Guard null → couleur par défaut `#1A3A5C` |
| `OnboardingComponent` | Change la signature de retour du dialog | Test spec onboarding mis à jour |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/workspace.spec.ts` — workspace switch → rechargement des dossiers

---

## Dépendances

### Subfeatures bloquantes

- SF-55-01 — statut : done (mergée 2026-03-26)

### Questions ouvertes impactées

- [ ] Aucune question ouverte impactée

---

## Notes et décisions

- **Couleurs domaine** issues de la palette officielle `DESIGN_SYSTEM.md` — aucune couleur hors charte :
  - DROIT_DU_TRAVAIL → Success `#27AE60`
  - DROIT_IMMIGRATION → Primary `#1A3A5C`
  - DROIT_FAMILLE → Accent `#C9973A`
- **Sélecteur pays** : deux boutons `mat-button` toggle (pas de `mat-select`) pour garder la cohérence visuelle des tuiles
- **Dialog retour** : interface `DomainPickerResult { legalDomain: string; country: string }` typée
- **Header** : `domainColor(workspace)` computed depuis `legalDomain` → couleur CSS appliquée sur l'icône `domain`
