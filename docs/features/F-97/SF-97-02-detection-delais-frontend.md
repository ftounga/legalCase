# Mini-spec — F-97 / SF-97-02 Détection automatique des délais — Frontend

## Identifiant

`F-97 / SF-97-02`

## Feature parente

`F-97` — Détection automatique des délais légaux

## Statut

`draft`

## Date de création

2026-04-01

## Branche Git

`feat/SF-97-02-detection-delais-frontend`

---

## Objectif

Afficher les délais proposés par l'IA (`source=AI`, `aiStatus=PENDING`) dans `CaseDeadlinesSectionComponent` avec des boutons Accepter / Rejeter, et appeler le nouvel endpoint PATCH validate.

---

## Comportement attendu

### Cas nominal

1. La page dossier charge `GET /api/v1/case-files/{id}/deadlines`.
2. La réponse contient un mélange de délais MANUAL et AI (PENDING/ACCEPTED).
3. `CaseDeadlinesSectionComponent` sépare les délais en deux groupes :
   - **Délais confirmés** : `source=MANUAL` ou (`source=AI` et `aiStatus=ACCEPTED`) → affichage actuel inchangé
   - **Propositions IA** : `source=AI` et `aiStatus=PENDING` → nouvelle sous-section distincte
4. Les propositions IA s'affichent avec :
   - Badge "IA" (couleur `#7C3AED`, violet — hors palette → utiliser `$color-ai` défini dans ce ticket si validé, sinon `#1A3A5C` avec label "IA" suffisant)
   - Label + date proposée
   - Bouton **Accepter** (icône check, couleur `$success`) → PATCH validate ACCEPT → déplace le délai dans "Délais confirmés"
   - Bouton **Rejeter** (icône close, couleur `$error`) → PATCH validate REJECT → retire le délai de la liste
5. Pendant l'appel PATCH, les boutons sont désactivés (spinner sur le délai concerné).
6. Après ACCEPT : snackbar "Délai accepté.", délai déplacé dans la liste confirmée.
7. Après REJECT : snackbar "Délai rejeté.", délai retiré de la liste.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Erreur PATCH validate | Snackbar "Erreur lors de la validation du délai." Boutons réactivés. |
| Liste vide de propositions | Sous-section masquée (aucun titre affiché) |

---

## Critères d'acceptation

- [ ] Les délais PENDING (`source=AI`, `aiStatus=PENDING`) s'affichent dans une sous-section "Propositions IA" distincte
- [ ] Les délais ACCEPTED et MANUAL s'affichent dans la section existante "Délais légaux" sans changement visuel
- [ ] Chaque proposition IA a un bouton Accepter et un bouton Rejeter
- [ ] Clic Accepter → PATCH validate ACCEPT → délai passe dans "Délais confirmés"
- [ ] Clic Rejeter → PATCH validate REJECT → délai disparaît de la liste
- [ ] Pendant l'appel : boutons désactivés sur le délai concerné
- [ ] Snackbar de confirmation après Accepter / Rejeter
- [ ] Snackbar d'erreur si le PATCH échoue
- [ ] Si aucune proposition IA : sous-section masquée
- [ ] `CaseDeadlineService` Angular exposé `validateDeadline(caseFileId, deadlineId, action)` → Observable
- [ ] `CaseDeadlineResponse` Angular étendu avec `source` et `aiStatus`
- [ ] Au moins 5 tests : affichage propositions, clic accepter, clic rejeter, erreur PATCH, sous-section masquée si vide

---

## Périmètre

### Hors scope (explicite)

- Modification de l'affichage des délais confirmés (section existante inchangée)
- Édition des propositions IA avant acceptation
- Historique des délais rejetés

---

## Technique

### Endpoint(s) consommés

| Méthode | URL |
|---------|-----|
| GET | `/api/v1/case-files/{id}/deadlines` (existant, enrichi) |
| PATCH | `/api/v1/case-files/{id}/deadlines/{deadlineId}/validate` (nouveau — SF-97-01) |

### Tables impactées

Aucune (lecture/écriture via API).

### Migration Liquibase

- [x] Non applicable

### Composants Angular

- `CaseDeadlinesSectionComponent` — ajout sous-section "Propositions IA", boutons Accepter/Rejeter, signal `validating(deadlineId)` pour état de chargement par délai
- `CaseDeadlineService` (Angular) — nouvelle méthode `validateDeadline(caseFileId, deadlineId, action)`
- `CaseDeadlineResponse` (model Angular) — extension avec `source: string` et `aiStatus: string | null`

---

## Plan de test

### Tests unitaires (composant)

- [ ] Proposition AI PENDING → visible dans sous-section "Propositions IA"
- [ ] Délai MANUAL → visible dans section "Délais légaux", absent de "Propositions IA"
- [ ] Délai AI ACCEPTED → visible dans section "Délais légaux", absent de "Propositions IA"
- [ ] Clic Accepter → `deadlineService.validateDeadline(caseFileId, id, 'ACCEPT')` appelé
- [ ] Après ACCEPT réussi → délai absent de "Propositions IA", présent dans "Délais légaux"
- [ ] Clic Rejeter → `deadlineService.validateDeadline(caseFileId, id, 'REJECT')` appelé
- [ ] Après REJECT réussi → délai absent des deux sections
- [ ] Erreur PATCH → snackbar erreur, boutons réactivés
- [ ] Aucune proposition IA → sous-section "Propositions IA" absente du DOM

### Tests d'intégration

Non applicable (frontend uniquement).

### Isolation workspace

- [x] Non applicable — l'isolation est garantie côté backend (SF-97-01).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — modification d'un composant existant sans toucher routing, auth ou workspace context.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression |
|----------------------|-----------------|----------------------|
| `CaseDeadlinesSectionComponent` | Section existante enrichie — affichage des délais MANUAL et ACCEPTED inchangé | Tests existants conservés |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — pas de route ni guard modifié.

---

## Dépendances

### Subfeatures bloquantes

- SF-97-01 — statut : `draft` (doit être mergée avant SF-97-02)

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Couleur badge "IA" : utiliser `#1A3A5C` (primaire du design system) avec le label texte "IA" — on n'introduit pas de nouvelle couleur hors palette sans validation du design system.
- Le signal `validating` est un `signal<string | null>` contenant l'id du délai en cours de validation (null = aucun). Cela désactive les boutons du délai concerné uniquement.
- SF-97-02 est bloquée par SF-97-01 : elle nécessite le nouveau endpoint PATCH validate.
