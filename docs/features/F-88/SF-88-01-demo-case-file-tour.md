# Mini-spec — F-88 / SF-88-01 Tour d'onboarding avec dossier de démonstration

---

## Identifiant

`F-88 / SF-88-01`

## Feature parente

`F-88` — Tour d'onboarding avec dossier de démonstration

## Statut

`ready`

## Date de création

2026-03-31

## Branche Git

`feat/SF-88-01-demo-case-file-tour`

---

## Objectif

Lorsque le tour guidé démarre sur un workspace sans aucun dossier, créer automatiquement un dossier réel "Dossier de démonstration", naviguer dedans pour les étapes 2-4, puis le supprimer silencieusement à la fin ou à la fermeture du tour.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur arrive sur `/case-files` avec un workspace vide (0 dossiers)
2. Le tour démarre automatiquement (step 0 — bienvenue)
3. L'utilisateur clique "Suivant" pour passer à l'étape 1 (step 1 — "Créez votre premier dossier")
4. **À la transition step 1 → step 2** : `TourService` appelle `CaseFileService.create()` avec :
   - `title: "Dossier de démonstration"`
   - `legalDomain` : domaine du workspace courant (récupéré via `WorkspaceService.getCurrentWorkspace()`)
   - `description: null`
5. L'ID du dossier créé est stocké dans `TourService._demoCaseFileId` (signal en mémoire)
6. `TourService` navigue vers `/case-files/{demoCaseFileId}`
7. Le tour continue normalement (étapes 2, 3, 4 pointent leurs cibles sur la page dossier)
8. À la fin du tour (step 4 → "Terminer") **ou** si l'utilisateur clique "Passer" à n'importe quelle étape :
   - `CaseFileStatusService.delete(demoCaseFileId)` est appelé silencieusement
   - `TourService._demoCaseFileId` est remis à null
   - Navigation vers `/case-files`
9. Le dossier de démonstration n'apparaît plus dans la liste

### Cas : workspace avec au moins un dossier existant

- Aucun dossier de démonstration n'est créé
- Le tour fonctionne exactement comme aujourd'hui (étapes 1-4 sur les vrais boutons)

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `create()` échoue (réseau, quota) | Tour continue sans dossier demo — étapes 2-4 s'affichent en bas à droite (comportement actuel) |
| `delete()` échoue silencieusement | Pas d'erreur affichée — le dossier orphelin reste (soft-deleted manuellement plus tard) |
| Utilisateur ferme le navigateur mid-tour | Le dossier reste en base — orphelin, visible dans la liste au prochain login — acceptable en V1 |
| `demoCaseFileId` présent en mémoire mais dossier déjà supprimé | `delete()` retourne 404 → ignoré silencieusement |

---

## Critères d'acceptation

- [ ] Si 0 dossiers : passage step 1→2 crée un dossier "Dossier de démonstration" et navigue dedans
- [ ] Si ≥1 dossiers : aucun dossier de démonstration créé, tour inchangé
- [ ] Le dossier de démonstration utilise le `legalDomain` du workspace courant
- [ ] Fin du tour (step 4 "Terminer") → dossier supprimé + navigation `/case-files`
- [ ] "Passer" à n'importe quelle étape → dossier supprimé (si créé) + navigation `/case-files`
- [ ] Échec création → tour continue sans bloquer (pas d'erreur utilisateur)
- [ ] Échec suppression → silencieux (pas d'erreur utilisateur)
- [ ] Le dossier de démonstration n'est pas visible dans la liste après la fin du tour

---

## Effet spotlight

Quand le tour est actif et qu'une cible existe :

1. Un `div.tour-backdrop` couvre tout l'écran (`position: fixed; inset: 0; z-index: 9000; background: rgba(0,0,0,0.65); pointer-events: none`)
2. L'élément cible reçoit `position: relative; z-index: 9001` pour passer au-dessus du backdrop
3. La carte tour passe en `z-index: 9002`
4. L'effet "trou" est obtenu par `box-shadow: 0 0 0 9999px rgba(0,0,0,0.65)` sur l'élément cible — remplace le `tour-highlight` actuel (outline doré)
5. Un léger `border-radius: 8px` sur le box-shadow pour arrondir le spotlight
6. Quand aucune cible (step 0) : backdrop absent

Le backdrop est un élément Angular dans `TourOverlayComponent` (conditionnellement rendu via `@if (step.target)`).

---

## Périmètre

### Hors scope

- Cleanup automatique des dossiers orphelins (fermeture navigateur mid-tour) — V1 acceptable
- Persistance du `demoCaseFileId` en localStorage (trop complexe pour le gain)
- Contenu pré-rempli dans le dossier de démonstration (documents mockés, analyse simulée)
- Modification du libellé des étapes du tour
- Animation de transition du spotlight entre deux cibles

---

## Technique

### Modifications `TourService`

```typescript
private _demoCaseFileId = signal<string | null>(null);
demoCaseFileId = this._demoCaseFileId.asReadonly();

// Injecter CaseFileService, CaseFileStatusService, WorkspaceService, Router

advanceToStep2(): void {
  // Appelé à la place de next() lorsque step courant = 1
  // Si dataSource vide → créer le dossier demo, stocker l'id, naviguer, passer au step 2
  // Sinon → next() normal
}

private cleanup(): void {
  const id = this._demoCaseFileId();
  if (id) {
    this.caseFileStatusService.delete(id).subscribe({ error: () => {} });
    this._demoCaseFileId.set(null);
  }
}
```

`stop()` et `skip()` appellent `cleanup()` avant de marquer le tour terminé et de naviguer vers `/case-files`.

### Modifications `TourOverlayComponent`

- La méthode `next()` du composant délègue à `tourService.advanceToStep2()` si step courant = 1
- Sinon délègue à `tourService.next()` comme avant
- `skip()` inchangé (délègue à `tourService.skip()` qui appelle `cleanup()`)

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| case_files | INSERT (create) | Dossier demo |
| case_files | UPDATE (soft-delete) | deletedAt renseigné à la fin du tour |

### Migration Liquibase

- [x] Non applicable — tables existantes

### Composants Angular impactés

- `TourService` — ajout `_demoCaseFileId`, `advanceToStep2()`, `cleanup()`
- `TourOverlayComponent` — délégation à `advanceToStep2()` au step 1, ajout `div.tour-backdrop`, remplacement `tour-highlight` par spotlight `box-shadow`
- `styles.scss` — suppression de l'ancien `.tour-highlight` outline doré, ajout `.tour-spotlight` avec `box-shadow: 0 0 0 9999px rgba(0,0,0,0.65); border-radius: 8px; position: relative; z-index: 9001`

---

## Plan de test

### Tests unitaires `TourService`

- [ ] 0 dossiers, step 1→2 : `CaseFileService.create()` appelé avec titre "Dossier de démonstration"
- [ ] 0 dossiers, step 1→2 : navigation vers `/case-files/{id}` déclenchée
- [ ] 0 dossiers, `stop()` : `CaseFileStatusService.delete()` appelé avec l'id stocké
- [ ] 0 dossiers, `skip()` : `CaseFileStatusService.delete()` appelé
- [ ] ≥1 dossiers, step 1→2 : `CaseFileService.create()` NON appelé
- [ ] Échec create : tour passe au step 2 sans bloquer, `_demoCaseFileId` reste null

### Tests `TourOverlayComponent`

- [ ] Clic "Suivant" à step 1 → `tourService.advanceToStep2()` appelé
- [ ] Clic "Passer" → `tourService.skip()` appelé

### Isolation workspace

- [x] Applicable — le dossier demo est créé dans le workspace courant, isolation garantie par l'API existante

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] **Workspace context** — `advanceToStep2()` lit le workspace courant pour le `legalDomain`

| Composant / Endpoint | Impact potentiel | Test prévu |
|---------------------|-----------------|------------|
| `CaseFilesListComponent` | Rafraîchit la liste au `workspaceSwitched$` — pas impacté par la création demo | Aucun |
| `WorkspaceService.getCurrentWorkspace()` | Appelé une fois pour le legalDomain — pattern existant | Test unitaire |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — pas de changement de routing ni d'auth

---

## Dépendances

### Subfeatures bloquantes

Aucune.

---

## Notes et décisions

- Le dossier demo est un vrai dossier avec un vrai `id` — aucun flag spécial en base
- L'identification se fait uniquement par `_demoCaseFileId` en mémoire dans `TourService`
- Si l'utilisateur navigue manuellement hors du dossier demo mid-tour, le tour continue (les étapes 2-4 s'affichent en bas à droite) — comportement déjà existant
- On ne crée le dossier demo que si `dataSource.length === 0` au moment du passage step 1→2. `TourService` reçoit cette information via un paramètre de `advanceToStep2(hasExistingFiles: boolean)`
