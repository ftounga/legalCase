# Mini-spec — F-96 / SF-96-02 : Checklist procédurale — Frontend

> Ce document doit être validé AVANT de démarrer le dev.

---

## Identifiant

`F-96 / SF-96-02`

## Feature parente

`F-96` — Checklist procédurale interactive

## Statut

`draft`

## Date de création

`2026-04-01`

## Branche Git

`feat/SF-96-02-procedure-checks-frontend`

---

## Objectif

Afficher dans `SynthesisComponent` un panneau interactif de checklist procédurale : les `procedure_checks` chargés depuis le backend sont présentés avec des boutons de statut cliquables (TO_CHECK / VERIFIED / NON_COMPLIANT). Chaque clic appelle `PATCH /procedure-checks/{checkId}` et met à jour l'affichage en temps réel.

---

## Comportement attendu

### Cas nominal

1. Au chargement d'une version de synthèse → appel `GET /case-files/{id}/analyses/{analysisId}/procedure-checks`
2. Si des checks existent → panneau `mat-expansion-panel` affiché après "Pièces manquantes"
3. Chaque ligne affiche : description + 3 boutons statut (✅ Vérifié / ❌ Non conforme / ⚠️ À vérifier)
4. Le bouton actif est mis en évidence (couleur pleine vs outlined)
5. Clic sur un bouton inactif → appel PATCH → statut mis à jour localement, spinner sur la ligne
6. Changement de version → rechargement des checks (comme les questions)

### Cas dégradés

| Situation | Comportement attendu |
|-----------|---------------------|
| Aucun check pour cette analyse | Panneau absent (pas de section vide) |
| Erreur PATCH (réseau) | Snackbar erreur, statut local non modifié |
| Clic pendant un PATCH en cours | Boutons désactivés pour cette ligne |
| Analyse sans pointsProcedure (ancienne) | Appel GET retourne [] → panneau absent |

---

## Critères d'acceptation

- [ ] `ProcedureCheckService` Angular créé : `list(caseFileId, analysisId)` + `updateStatus(checkId, statut)`
- [ ] `ProcedureCheck` model TypeScript : `{ id, ordre, description, statut }`
- [ ] Signal `procedureChecks` dans `SynthesisComponent` chargé à chaque version
- [ ] Panneau conditionnel affiché si `procedureChecks().length > 0`
- [ ] 3 boutons par ligne : TO_CHECK / VERIFIED / NON_COMPLIANT avec icône + libellé
- [ ] Bouton actif visuellement distinct (mat-flat-button vs mat-stroked-button)
- [ ] Spinner par ligne pendant le PATCH (`updatingCheckId` signal)
- [ ] Snackbar erreur si PATCH échoue
- [ ] Changement de version → `procedureChecks.set([])` puis rechargement
- [ ] Rétrocompatibilité : synthèses sans checks → pas d'erreur

---

## Périmètre

### Hors scope (explicite)

- Injection des NON_COMPLIANT dans le prompt enrichi — SF-96-03
- Modification du PDF export
- Statistiques / compteurs de conformité

---

## Technique

### Nouveau fichier

| Fichier | Rôle |
|---------|------|
| `procedure-check.service.ts` | `list()` + `updateStatus()` |
| `procedure-check.model.ts` | Interface `ProcedureCheck` |

### Fichiers modifiés

| Fichier | Modification |
|---------|-------------|
| `synthesis.component.ts` | Signal `procedureChecks`, `updatingCheckId`, `loadChecksForVersion()`, `updateCheckStatus()` |
| `synthesis.component.html` | Panneau checklist après "Pièces manquantes" |
| `synthesis.component.scss` | Styles `.checklist-item`, `.status-btn`, `.status-btn--active` |

### Statuts et couleurs (Design System)

| Statut | Icône | Couleur active | Libellé |
|--------|-------|----------------|---------|
| TO_CHECK | `help_outline` | `#6B7A8D` (text secondaire) | À vérifier |
| VERIFIED | `check_circle` | `#27AE60` (success) | Vérifié |
| NON_COMPLIANT | `cancel` | `#C0392B` (error) | Non conforme |

---

## Plan de test

### Tests unitaires

- [ ] `SynthesisComponent` — `loadChecksForVersion()` appelé au chargement de la version la plus récente
- [ ] `SynthesisComponent` — `onVersionChange()` réinitialise `procedureChecks`
- [ ] `SynthesisComponent` — checks vides → panneau absent
- [ ] `SynthesisComponent` — `updateCheckStatus()` met à jour le statut localement en cas de succès
- [ ] `SynthesisComponent` — `updateCheckStatus()` affiche snackbar et ne modifie pas le statut en cas d'erreur

### Isolation workspace

- [ ] Non applicable (isolation garantie par le backend SF-96-01)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — nouveau composant de données dans une page existante, aucun changement auth/routing/plans

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné

---

## Dépendances

### Subfeatures bloquantes

- SF-96-01 mergée ✅

### Questions ouvertes

- Aucune

---

## Notes et décisions

- Les checks sont chargés via un appel GET distinct (pas depuis `CaseAnalysisResult`) pour rester indépendants du cycle de vie de l'analyse
- `updatingCheckId` est un signal `string | null` — une seule ligne peut être en cours de mise à jour à la fois (UX suffisante, simplifie les tests)
- Les boutons utilisent `mat-flat-button` pour le statut actif et `mat-stroked-button` pour les inactifs
