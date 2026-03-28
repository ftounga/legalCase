# SF-67-01 — Wizard d'onboarding guidé

**Feature parente :** F-67 — Wizard d'onboarding guidé
**Branche :** feat/SF-67-01-onboarding-wizard
**Statut :** ready
**Date de création :** 2026-03-29

---

## Objectif

Afficher un wizard 4 étapes en dialog overlay au premier accès à `/case-files` après création du workspace, pour guider le nouvel utilisateur à travers les actions clés du produit. Skippable à tout moment. État persisté en localStorage.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur crée son workspace (onboarding existant) → redirigé vers `/case-files`.
2. `CaseFilesListComponent` vérifie le localStorage : clé `onboarding_wizard_done_<workspaceId>` absente.
3. Un `MatDialog` s'ouvre automatiquement avec le wizard 4 étapes :
   - **Étape 1 — Bienvenue** : "Votre workspace est prêt ! Découvrez comment utiliser AI LegalCase."
   - **Étape 2 — Créer un dossier** : "Commencez par créer un dossier pour votre affaire."
   - **Étape 3 — Ajouter des documents** : "Uploadez vos pièces (contrats, emails, décisions)."
   - **Étape 4 — Lancer une analyse** : "L'IA génère une synthèse juridique complète de vos documents."
4. Chaque étape affiche : icône, titre, description, bouton "Suivant" (ou "Commencer" sur la dernière), bouton "Passer" (skip tout).
5. À la fin ou au skip → clé localStorage posée → dialog fermé → utilisateur sur `/case-files`.
6. Les visites suivantes : clé présente → wizard non affiché.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Workspace ID absent (non résolu) | Wizard non affiché — pas de crash |
| localStorage indisponible (rare) | Wizard non affiché — fail silencieux |
| Utilisateur invité (pas créateur du workspace) | Wizard non affiché — clé vérifiée par workspaceId |

---

## Critères d'acceptation

- [ ] Dialog s'ouvre automatiquement au premier accès à `/case-files` (workspace créé)
- [ ] 4 étapes affichées avec icône + titre + description
- [ ] Bouton "Suivant" avance à l'étape suivante
- [ ] Bouton "Passer" ferme le dialog à tout moment
- [ ] Dernière étape : bouton "Commencer" ferme le dialog
- [ ] Après completion ou skip : localStorage marqué → dialog ne réapparaît plus
- [ ] Utilisateur invité (workspace existant) : wizard non affiché
- [ ] Dialog non bloquant : clic en dehors → ferme (= équivalent à "Passer")

---

## Périmètre

### Hors scope

- Barre de progression persistante dans la sidebar après fermeture du wizard
- Relance du wizard depuis les settings
- Personnalisation des étapes par domaine juridique
- Persistance côté backend (localStorage suffit pour V2)

---

## Technique

### Composants Angular

- **`OnboardingWizardDialogComponent`** (nouveau) — dialog 4 étapes, signal `currentStep`
- **`OnboardingWizardService`** (nouveau) — gère localStorage : `shouldShow(workspaceId)`, `markDone(workspaceId)`
- **`CaseFilesListComponent`** (modifié) — injecte `OnboardingWizardService` + `MatDialog`, ouvre le wizard au `ngOnInit` si applicable

### localStorage

- Clé : `onboarding_wizard_done_<workspaceId>` (ex: `onboarding_wizard_done_44e9b1f8-...`)
- Valeur : `"1"`
- Jamais de données sensibles

### Endpoints

Aucun — feature 100% frontend.

### Migration Liquibase

Non applicable.

---

## Plan de test

### Tests unitaires

- [ ] `OnboardingWizardService.shouldShow` — clé absente → true
- [ ] `OnboardingWizardService.shouldShow` — clé présente → false
- [ ] `OnboardingWizardService.markDone` — pose la clé localStorage
- [ ] `OnboardingWizardService.shouldShow` — workspaceId null → false

### Tests du composant

- [ ] `OnboardingWizardDialogComponent` — étape 1 affichée à l'ouverture
- [ ] `OnboardingWizardDialogComponent` — "Suivant" avance à l'étape 2
- [ ] `OnboardingWizardDialogComponent` — "Passer" ferme le dialog (appelle dialogRef.close)
- [ ] `OnboardingWizardDialogComponent` — étape 4 affiche "Commencer"
- [ ] `CaseFilesListComponent` — `shouldShow` true → `MatDialog.open` appelé
- [ ] `CaseFilesListComponent` — `shouldShow` false → `MatDialog.open` non appelé

### Isolation workspace

- Non applicable — pas d'accès données backend.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing frontend** — `CaseFilesListComponent` modifié (ngOnInit)

### Composants existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `CaseFilesListComponent` | Ajout d'un `MatDialog.open` au ngOnInit — risque d'ouvrir le wizard pour des users existants | `shouldShow` retourne false si clé présente → non-régression via test |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/navigation.spec.ts` — vérifier que la navigation vers /case-files ne bloque pas si le dialog s'ouvre

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Le wizard s'affiche uniquement sur `/case-files`, pas sur d'autres pages.
- `disableClose: false` sur le dialog → clic en dehors = skip (appelle `markDone`).
- Le workspaceId est lu depuis `WorkspaceService.getCurrentWorkspace()` — déjà disponible dans `CaseFilesListComponent`.
- Les utilisateurs existants (workspace déjà créé avant le déploiement) ne verront pas le wizard — clé absente mais workspace existant. Solution : pas de guard supplémentaire, la clé étant simplement absente, ils verraient le wizard une fois. Acceptable pour V2.
