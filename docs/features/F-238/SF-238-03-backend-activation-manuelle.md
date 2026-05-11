# Mini-spec — F-238 / SF-238-03 Backend activation manuelle

## Identifiant

`F-238 / SF-238-03`

## Feature parente

`F-238` — Catalogue d'outils décisionnels cliquable + labels humains

## Statut

`ready`

## Date de création

2026-05-11

## Branche Git

`feat/F-238-catalogue-cliquable-labels`

---

## Objectif

Backend persistant pour l'activation manuelle d'un outil décisionnel par l'avocat : table `manual_tool_activations` + 2 endpoints + injection dans `DecisionToolVisibilityService.resolveVisibleTools` (les activations actives migrent dans `contextual`, sortent de `catalog`).

---

## Comportement attendu

### Cas nominal

1. POST `/api/v1/case-files/{id}/decision-tools-visibility/manual-activations` body `{"toolId": "F-DT-08-licenciement-validity"}`
   - Vérifie ownership du case_file par le workspace de l'utilisateur (`CurrentUserResolver`).
   - Vérifie que `toolId` non null et non vide.
   - Insère une ligne `manual_tool_activations` (id UUID, case_file_id, tool_id, activated_by=user.id, activated_at=now, workspace_id=cf.workspace.id, deactivated_at=NULL).
   - Renvoie 200 `{"id": "<uuid>", "toolId": "<tool_id>", "activatedAt": "<ISO-8601>"}`.
2. DELETE `/api/v1/case-files/{id}/decision-tools-visibility/manual-activations/{toolId}`
   - Vérifie ownership.
   - Trouve la ligne active (deactivated_at IS NULL), met `deactivated_at = now`.
   - Renvoie 204 No Content. Idempotent : 204 si aucune ligne active (no-op).
3. `DecisionToolVisibilityService.resolveVisibleTools` :
   - Après calcul standard alwaysOn/contextual/catalog, charge les `manual_tool_activations` actives (`deactivated_at IS NULL`) du case_file.
   - Chaque `tool_id` activé manuellement :
     - retiré de `catalog`,
     - ajouté à `contextual` s'il n'y est pas déjà.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| toolId null/blank | 400 « toolId requis » | 400 |
| case_file inexistant ou autre workspace | 404 « Case file not found » | 404 |
| toolId hors TOOL_REGISTRY backend (placeholder validation simple : longueur ≤ 128) | 400 (optionnel ; on accepte la valeur tant qu'elle respecte la contrainte de longueur ; la validation `tool_id` ∈ TOOL_REGISTRY est garantie côté frontend) | 400 |
| Déjà actif (deactivated_at IS NULL existante) | 409 « Outil déjà activé » | 409 |
| User non authentifié | 401 (gérée par Spring Security) | 401 |

---

## Contrat API (figé pour SF-238-02)

### POST `/api/v1/case-files/{caseFileId}/decision-tools-visibility/manual-activations`

- Body :
  ```json
  { "toolId": "F-DT-08-licenciement-validity" }
  ```
- Réponse 200 :
  ```json
  {
    "id": "8a7f4a3b-...",
    "toolId": "F-DT-08-licenciement-validity",
    "activatedAt": "2026-05-11T14:32:18Z"
  }
  ```
- Réponses d'erreur : 400 / 401 / 404 / 409.

### DELETE `/api/v1/case-files/{caseFileId}/decision-tools-visibility/manual-activations/{toolId}`

- Réponse 204 (toujours, idempotent).
- Réponses d'erreur : 401 / 404 (case_file).

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] Auth / Principal : utilise `@AuthenticationPrincipal OidcUser oidcUser` + `Principal` + `CurrentUserResolver` (pattern standard, identique à `DecisionToolVisibilityController`).
- [x] Workspace context : ownership vérifié via `WorkspaceMemberRepository.findByUserAndPrimaryTrue(user)` (pattern miroir `DecisionToolVisibilityService.resolveCaseFile`).
- [x] Plans / limites : pas de gate plan (l'activation manuelle est gratuite, ne consomme aucune ressource IA).
- [x] Outil décisionnel métier : nouveau mécanisme d'activation, ne modifie pas les outils eux-mêmes — pas de switch sur situation métier (transversal).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `DecisionToolVisibilityService` | Oui | Étendu avec injection des activations manuelles |
| `DecisionToolVisibilityController` | Oui (mêmes paths) | Endpoint POST/DELETE ajoutés (peut être un controller séparé pour clarté) |
| Tous les outils 103 | Indirect | Tout outil peut être activé manuellement (pas de whitelist) |

### Décision

- [x] Étendu à toutes les cibles applicables.

---

## Audit "Impact F-166 cross-C×D"

L'activation manuelle est **agnostique au cross C×D** : un outil activé manuellement migre `catalog → contextual` quelle que soit la combinaison (FR/BE × Travail/Immigration/Famille). Aucune entry seedée dans `decision_tool_visibility_rules` — c'est une couche orthogonale (table dédiée `manual_tool_activations`). Pas d'impact sur les 6 cellules C×D des règles standards.

## Audit "exhaustivité droit national FR/BE"

N/A — cette SF est **transversale infrastructure** (table d'activations manuelles, pas de seed national). Pas de source juridique nationale à référencer ; pas de jumeau FR↔BE à valider.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — SF backend pure.

---

## Impact par domaine métier

**Transversale infrastructure, aucune adaptation par domaine.** Tous les outils des 3 domaines × 2 pays peuvent être activés manuellement via le même endpoint avec le même comportement.

---

## Parité des domaines métier (niveau ≥ 5)

- [x] Niveau du tool décisionnel livré : **non applicable** — SF backend infrastructure, ne livre pas d'outil décisionnel.

---

## Critères d'acceptation

- [ ] Migration Liquibase 224 (numéro libre vérifié) crée la table `manual_tool_activations` avec colonnes id/case_file_id/tool_id/activated_by/activated_at/deactivated_at/workspace_id + FK + 2 index + contrainte d'unicité partielle sur (case_file_id, tool_id) where deactivated_at IS NULL (Postgres) / via index unique conditionnel.
- [ ] Entity `ManualToolActivation` + repository Spring Data JPA.
- [ ] Endpoint POST renvoie 200 avec body conforme au contrat ; 409 si déjà activé ; 404 si case_file hors workspace ; 400 si toolId blank.
- [ ] Endpoint DELETE renvoie 204, idempotent.
- [ ] `DecisionToolVisibilityService.resolveVisibleTools` injecte les activations manuelles dans `contextual` (retire de `catalog`).
- [ ] Tests UT : repository charge les activations actives ; service injecte correctement dans contextual.
- [ ] Test IT minimum : POST → GET visibility → tool présent dans contextual + absent de catalog.
- [ ] Isolation workspace vérifiée (test 404 pour case_file d'un autre workspace).
- [ ] `./mvnw test` cible verte.

---

## Périmètre

### Hors scope

- UI bouton désactiver → DELETE exposé en backend mais pas câblé UI dans F-238 (peut être ajouté en SF future).
- Historique « qui a activé / quand » exposé à l'écran → données présentes en DB, pas exposées côté API (out of scope V1).
- Notification équipe « X a activé l'outil Y sur le dossier Z » → out of scope.

---

## Plan de test

### Unitaires backend (JUnit)

- `ManualToolActivationRepositoryTest` : findActiveByCaseFile retourne uniquement les lignes avec `deactivated_at IS NULL`.
- Service unitaire : `resolveVisibleTools` injecte les activations dans contextual.

### Intégration backend (Spring Boot)

- `ManualToolActivationControllerIT` :
  - POST nouveau → 200 + activation visible via GET visibility (contextual contient toolId).
  - POST 2× même toolId → 2e renvoie 409.
  - DELETE actif → 204 + GET visibility (toolId redescendu en catalog).
  - DELETE inactif (jamais activé) → 204 (idempotent).
  - POST case_file autre workspace → 404.
  - POST sans body → 400.

### Isolation workspace

- Test explicite : utilisateur du workspace W1 tente d'activer un outil sur le case_file du workspace W2 → 404.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `activated_at` | `CURRENT_TIMESTAMP` | Renseigné automatiquement par defaut DB |
| `deactivated_at` | `NULL` | Indique « actif » |

---

## Tables / endpoints / composants impactés

- **Tables nouvelles** : `manual_tool_activations`.
- **Endpoints** : POST + DELETE `/api/v1/case-files/{id}/decision-tools-visibility/manual-activations[/{toolId}]`.
- **Services** : `DecisionToolVisibilityService.resolveVisibleTools` étendu.
- **Entities** : `ManualToolActivation` + `ManualToolActivationRepository`.
- **Controllers** : `ManualToolActivationController` (ou réuse `DecisionToolVisibilityController` — décidé en implémentation, un controller séparé pour clarté).
