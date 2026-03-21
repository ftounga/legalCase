# Mini-spec — F-27 / SF-27-02 — Domaine juridique workspace (frontend)

## Identifiant

`F-27 / SF-27-02`

## Feature parente

`F-27` — Domaine juridique du workspace

## Statut

`ready`

## Date de création

2026-03-21

## Branche Git

`feat/SF-27-02-legal-domain-frontend`

---

## Objectif

Ajouter une modale de sélection du domaine juridique dans le flow d'onboarding (après saisie du nom du workspace), affichant 3 grandes catégories dont 2 marquées "bientôt disponible".

---

## Comportement attendu

### Cas nominal

1. L'utilisateur saisit le nom de son workspace dans l'écran d'onboarding.
2. Il clique "Créer mon workspace".
3. Une modale s'ouvre avec 3 tuiles :
   - **Droit du travail** — active, sélectionnable, sélectionnée par défaut
   - **Droit de l'immigration** — désactivée, badge "Bientôt disponible"
   - **Droit immobilier** — désactivée, badge "Bientôt disponible"
4. L'utilisateur ne peut cliquer que sur "Droit du travail".
5. Il clique "Confirmer" → l'API `POST /api/v1/workspaces` est appelée avec `{ name, legalDomain: 'DROIT_DU_TRAVAIL' }`.
6. Redirection vers `/case-files`.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Fermeture de la modale sans sélection | Modale non fermable (pas de bouton Annuler / pas de clic outside) |
| Erreur API création workspace | Snackbar erreur, retour à l'onboarding |

---

## Critères d'acceptation

- [ ] La modale s'ouvre automatiquement après soumission du nom du workspace
- [ ] Les 3 tuiles sont affichées : Droit du travail, Droit de l'immigration, Droit immobilier
- [ ] Les tuiles "Droit de l'immigration" et "Droit immobilier" sont visuellement désactivées (opacité réduite, badge "Bientôt disponible", non cliquables)
- [ ] "Droit du travail" est sélectionnée par défaut (état actif visible)
- [ ] Le bouton "Confirmer" est actif dès l'ouverture (seul choix possible)
- [ ] La modale n'est pas fermable sans confirmer
- [ ] L'appel API inclut `legalDomain: 'DROIT_DU_TRAVAIL'`
- [ ] La constante `'EMPLOYMENT_LAW'` est supprimée du frontend (remplacée dans `case-file-create-dialog`)
- [ ] Le formulaire de création de dossier ne demande plus le domaine (hérité du workspace)

---

## Périmètre

### Hors scope (explicite)

- Affichage du domaine dans le header ou sur les dossiers (hors scope V1)
- Modification du domaine workspace après création
- Implémentation réelle de DROIT_IMMIGRATION ou DROIT_IMMOBILIER

---

## Technique

### Composants Angular

- `DomainPickerDialogComponent` (nouveau) — modale standalone, 3 tuiles, no-close
- `OnboardingComponent` (modifié) — ouvre la modale après submit du nom, passe le nom en paramètre
- `CaseFileCreateDialogComponent` (modifié) — supprime `legalDomain: 'EMPLOYMENT_LAW'` du body API
- `WorkspaceService` (modifié) — `createWorkspace(name, legalDomain)` → updated signature

### Endpoint appelé

| Méthode | URL | Body |
|---------|-----|------|
| POST | `/api/v1/workspaces` | `{ name: string, legalDomain: 'DROIT_DU_TRAVAIL' }` |

---

## Design (conforme DESIGN_SYSTEM.md)

- 3 tuiles en grille `1fr 1fr 1fr`, gap 16px
- Tuile active : bordure `4px solid #C9973A`, fond blanc, icône + titre + sous-titre
- Tuiles désactivées : opacité 0.45, badge "Bientôt disponible" fond `#F5F5F5`, texte `#6B7A8D`
- Icônes Material : `gavel` (droit du travail), `flight` (immigration), `home` (immobilier)
- Titre modale : "Votre domaine principal" (Merriweather)
- Largeur modale : 600px

---

## Plan de test

### Tests unitaires

- [ ] `DomainPickerDialogComponent` — seule la tuile DROIT_DU_TRAVAIL est sélectionnable

### Tests d'intégration

- [ ] `OnboardingComponent` spec — après submit, modale ouverte + appel API avec legalDomain
- [ ] `CaseFileCreateDialogComponent` spec — legalDomain absent du body envoyé

### Isolation workspace

- [ ] Non applicable — flow d'onboarding, pas d'accès multi-workspace

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] **Auth / Principal** — non
- [x] **Workspace context** — l'onboarding crée le workspace, changement de signature API
- [ ] **Plans / limites** — non
- [ ] **Navigation / routing frontend** — non (pas de nouvelle route)

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `CaseFileCreateDialogComponent` | Suppression de `legalDomain` dans le body | Spec mis à jour |
| `OnboardingComponent` | Flow modifié — modale intermédiaire | Spec mis à jour |

### Smoke tests E2E concernés

- [x] `e2e/smoke/happy-path.spec.ts` — `login → créer dossier` — pas d'impact direct (onboarding déjà passé pour le compte e2e), mais vérifier que la création de dossier fonctionne sans legalDomain dans le body

---

## Dépendances

### Subfeatures bloquantes

- `SF-27-01` — doit être mergée avant le dev frontend (l'API doit accepter `legalDomain`)

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- La modale est non-fermable pour forcer la sélection : c'est un choix UX volontaire. L'utilisateur DOIT choisir un domaine avant d'accéder au dashboard.
- En V1, "Droit du travail" est le seul choix réel. Les deux autres tuiles sont là pour montrer la roadmap produit et créer la sensation d'un produit évolutif.
- Le compte e2e existant a déjà un workspace — le smoke test happy-path ne passera pas par l'onboarding et ne sera pas impacté.
