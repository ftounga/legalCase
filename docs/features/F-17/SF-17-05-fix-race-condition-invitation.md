# Mini-spec — F-17 / SF-17-05 Fix race condition acceptation invitation

## Identifiant

`F-17 / SF-17-05`

## Feature parente

`F-17` — Gestion des membres workspace

## Statut

`ready`

## Date de création

2026-03-19

## Branche Git

`feat/SF-17-05-fix-invitation-race`

---

## Objectif

Corriger la race condition frontend qui provoque l'affichage des dossiers de l'ancien workspace lors de l'acceptation d'une invitation par un utilisateur déjà connu du système.

---

## Comportement attendu

### Cas nominal

**Flux : utilisateur existant, non connecté, clique sur un lien d'invitation**

1. `/invite?token=...` → utilisateur non authentifié
2. Token stocké dans `localStorage` (`pendingInvitationToken`)
3. Redirect OAuth2 → connexion Google/Microsoft
4. Après OAuth2 → `defaultSuccessUrl` → `/case-files` → `ShellComponent` charge
5. `ShellComponent` détecte le `pendingInvitationToken`
6. **BLOQUE l'affichage du router-outlet** (signal `ready = false`)
7. Appelle `acceptInvitation(token)` → backend met à jour `is_primary`
8. Après succès : recharge `getCurrentWorkspace()` → met à jour le signal workspace
9. Passe `ready = true` → router-outlet s'affiche
10. `CaseFilesComponent` charge → appelle `/api/v1/case-files` → retourne les dossiers du **nouveau** workspace (is_primary correct)

**Flux : utilisateur existant, déjà connecté, clique sur un lien d'invitation**

1. `/invite?token=...` → `InviteAcceptComponent`
2. `loadCurrentUser()` → user existant
3. `acceptInvitation(token)` → backend met à jour `is_primary`
4. Après succès : navigate vers `/case-files` avec reload forcé du workspace
5. `ShellComponent` charge avec le workspace à jour → dossiers corrects

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Token invalide/expiré lors de l'acceptation dans ShellComponent | Snackbar erreur + `ready = true` (ne pas bloquer l'app) |
| Erreur réseau lors de `acceptInvitation` | Snackbar erreur + `ready = true` |
| Pas de `pendingInvitationToken` | Comportement inchangé, `ready = true` immédiatement |

---

## Critères d'acceptation

- [ ] Un utilisateur existant invité dans un nouveau workspace voit les dossiers du **nouveau** workspace immédiatement après connexion, sans avoir à rafraîchir
- [ ] L'app ne se bloque pas si l'acceptation d'invitation échoue (token expiré, erreur réseau) — `ready` passe à `true` dans tous les cas
- [ ] Le comportement sans invitation pendante est inchangé (pas de régression)
- [ ] Le workspace affiché dans le header correspond aux dossiers affichés dès le premier chargement

---

## Périmètre

### Hors scope (explicite)

- Modification du backend (aucune)
- Refactoring global de la gestion du state workspace
- Autres composants que `ShellComponent` et `InviteAcceptComponent`

---

## Technique

### Composants Angular impactés

- `ShellComponent` — ajout d'un signal `ready`, séquencement acceptation → reload workspace → affichage router-outlet
- `InviteAcceptComponent` — pas de modification nécessaire (le cas "déjà connecté" est géré correctement dans le flow actuel via `ShellComponent`)

### Changement clé dans ShellComponent

Actuellement : `getCurrentWorkspace()` et `acceptInvitation()` lancés en parallèle, `<router-outlet>` toujours visible.

Après fix :
```
ngOnInit():
  if pendingToken:
    ready = false
    acceptInvitation(token)
      .then: reload workspace → ready = true
      .catch: ready = true  (fail-open)
  else:
    getCurrentWorkspace() → ready = true

template: <router-outlet *ngIf="ready()" />
```

### Tables impactées

Aucune — correctif purement frontend.

### Migration Liquibase

- [x] Non applicable

---

## Plan de test

### Tests unitaires (Karma)

- [ ] `ShellComponent` — sans token pending : `ready` passe à `true` après `getCurrentWorkspace()`
- [ ] `ShellComponent` — avec token pending : `ready` reste `false` pendant l'acceptation, puis passe à `true` après reload workspace
- [ ] `ShellComponent` — token pending + erreur acceptation : `ready` passe à `true` malgré l'erreur (fail-open)
- [ ] `ShellComponent` — token pending + succès : workspace rechargé avec la nouvelle valeur

### Isolation workspace

- [ ] Non applicable — correctif de séquencement, pas de données

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Le fix est **fail-open** : si l'acceptation échoue, l'app s'affiche quand même (l'utilisateur verra le snackbar d'erreur mais ne sera pas bloqué)
- Aucune modification backend — le bug est entièrement côté frontend
- `InviteAcceptComponent` (flux "déjà connecté") n'est pas impacté par ce bug car dans ce cas la navigation vers `/case-files` se fait 2 secondes après l'acceptation, et le `ShellComponent` est instancié **après** l'acceptation. Cependant, le workspace n'est pas rechargé dans ce flux — on ajoutera aussi un reload workspace dans `InviteAcceptComponent` post-acceptation pour cohérence.
