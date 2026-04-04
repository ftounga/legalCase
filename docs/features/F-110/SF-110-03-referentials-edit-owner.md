# Mini-spec — F-110 / SF-110-03 : Modification OWNER/ADMIN + validation IA

## Identifiant
`F-110 / SF-110-03`

## Feature parente
`F-110` — Guides & barèmes métier par domaine

## Statut
`ready`

## Date de création
`2026-04-04`

## Branche Git
`feat/SF-110-03-referentials-edit-owner`

---

## Objectif
Permettre à l'OWNER/ADMIN de modifier une valeur du référentiel depuis l'écran "Guides & barèmes". L'IA valide la modification avant sauvegarde et affiche un avertissement si divergence — l'OWNER confirme ou annule.

---

## Comportement attendu

### Cas nominal
1. L'OWNER voit un bouton "Modifier" sur chaque entrée.
2. Il clique → dialog `mat-dialog` avec `label` + `valueJson` éditables, pré-remplis.
3. Il soumet → `PUT /api/v1/referentials/{id}` body `{ label, valueJson, force: false }`.
4. Le backend appelle Claude Haiku (fail-open, 512 tokens) pour valider.
5. Si "VALID" → save + return `{ saved: true, entry: {...} }` → panel mis à jour.
6. Si "WARNING: ..." → return `{ saved: false, warning: "..." }` → dialog confirmation.
7. L'OWNER confirme → re-soumis avec `force: true` → save.
8. L'OWNER annule → rien sauvegardé.

### Logique upsert
- Entrée système (`workspaceId=NULL`) → crée/met à jour une workspace override (`workspaceId` du caller, `is_system=false`) avec même `(legalDomain, referentialType, entryKey, country)`.
- Entrée workspace → vérifie `workspaceId` = workspace du caller → update in-place.
- `getReferentials` déduplique : workspace override prime sur entrée système pour même `(type, key, country)`.

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| Appelant non OWNER/ADMIN | Accès refusé | 403 |
| Entry appartenant à un autre workspace | Accès refusé | 403 |
| valueJson non-parsable JSON | Erreur de validation | 400 |
| Claude indisponible / timeout | fail-open → save sans validation | 200 |

---

## Critères d'acceptation

- [ ] Bouton "Modifier" visible uniquement si `memberRole` ∈ {OWNER, ADMIN}
- [ ] Dialog : champs `label` + `valueJson` pré-remplis, validation JSON côté client
- [ ] `PUT /api/v1/referentials/{id}` accepte `{ label, valueJson, force: boolean }`
- [ ] Validation IA : Claude Haiku, prompt droit FR/BE, fail-open si erreur
- [ ] Warning → dialog confirmation avec message IA + bouton "Sauvegarder quand même"
- [ ] Entrée système → upsert workspace override
- [ ] `getReferentials` déduplique (workspace override masque système pour même clé)
- [ ] MEMBER : bouton "Modifier" absent, aucune régression

---

## Périmètre

### Hors scope
- Suppression d'entrée
- Reset vers valeur système
- Ajout d'une nouvelle entrée
- Cron + badge (SF-110-04)
- Signalement anomalie (SF-110-05)

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| PUT | `/api/v1/referentials/{id}` | Oui | OWNER ou ADMIN |

### Composant Angular
- `ReferentialEditDialogComponent` (`referentials/referential-edit-dialog/`)

### Nouvelles classes backend
- `ReferentialUpdateRequest` (record)
- `ReferentialUpdateResponse` (record)
- `ReferentialValidationService` (Claude Haiku, fail-open)
- Query repo : `findWorkspaceEntry(workspaceId, domain, type, key, country)`

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| legal_referentials | SELECT + INSERT ou UPDATE | Upsert workspace override |

### Migration Liquibase
Non applicable.

---

## Plan de test

### Backend — unitaires
- [ ] `ReferentialValidationService` — réponse "VALID" → `valid=true`
- [ ] `ReferentialValidationService` — réponse "WARNING: ..." → `valid=false, warning=...`
- [ ] `ReferentialValidationService` — exception Anthropic → fail-open (`valid=true`)
- [ ] `LegalReferentialService.updateReferential()` — entrée système → crée workspace override
- [ ] `LegalReferentialService.updateReferential()` — entrée workspace → update in-place
- [ ] `LegalReferentialService.getReferentials()` — workspace override masque entrée système

### Backend — intégration
- [ ] `PUT /api/v1/referentials/{id}` → 403 si MEMBER
- [ ] `PUT /api/v1/referentials/{id}` force=false + Claude mock → 200 saved=false avec warning
- [ ] `PUT /api/v1/referentials/{id}` force=true → 200 saved=true, pas d'appel Claude

### Frontend — unitaires
- [ ] Bouton "Modifier" absent pour MEMBER
- [ ] Bouton "Modifier" présent pour OWNER
- [ ] Dialog de confirmation affiché si `saved=false`

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Auth / Principal** — PUT vérifie le rôle (WorkspaceMember)
- [x] **Workspace context** — upsert utilise workspaceId du caller

| Composant | Impact potentiel | Non-régression |
|-----------|-----------------|----------------|
| `ReferentialController` | nouveau PUT | GET non impacté |
| `LegalReferentialService.getReferentials` | déduplication ajoutée | test unitaire |
| `ReferentialsComponent` | bouton conditionnel + dialog | tests MEMBER inchangés |

### Smoke tests
Aucun — pas de changement de route ni de guard.

---

## Dépendances
- SF-110-01 — Done
- SF-110-02 — Done
