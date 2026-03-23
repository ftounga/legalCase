# Mini-spec — F-36 / SF-36-03 Indicateur synthèse périmée

---

## Identifiant

`F-36 / SF-36-03`

## Feature parente

`F-36` — Déclenchement manuel de l'analyse dossier

## Statut

`in-progress`

## Date de création

2026-03-23

## Branche Git

`feat/SF-36-03-synthese-perimee`

---

## Objectif

Signaler visuellement à l'avocat que la synthèse affichée ne couvre pas tous les documents du dossier lorsque de nouveaux documents ont été uploadés après la dernière analyse dossier.

---

## Comportement attendu

### Cas nominal

1. L'avocat a un dossier avec N documents analysés et une synthèse existante.
2. Il uploade un ou plusieurs nouveaux documents → DOCUMENT_ANALYSIS se lance automatiquement.
3. Une fois les nouveaux documents analysés (DOCUMENT_ANALYSIS DONE), le bouton "Analyser le dossier" réapparaît.
4. **À ce moment** (et jusqu'au prochain clic sur "Analyser le dossier") :
   - La section Synthèse affiche un badge warning : "Cette synthèse ne prend pas en compte X document(s) récent(s)"
   - Dans le tableau des documents, chaque document uploadé APRÈS la date de création de la synthèse affiche un badge "Non inclus"
5. Dès que l'avocat clique sur "Analyser le dossier" et que la nouvelle synthèse est disponible, les indicateurs disparaissent.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `synthesis()` est null (pas encore de synthèse) | Aucun indicateur affiché — état normal pré-analyse |
| Tous les documents sont antérieurs à la synthèse | Aucun indicateur — synthèse à jour |
| `synthesis.createdAt` absent du modèle frontend | Indicateurs non affichés (dégradé silencieux) |

---

## Critères d'acceptation

- [ ] Badge warning visible sur la section Synthèse si au moins 1 document a un `createdAt` > `synthesis.createdAt`
- [ ] Badge indique le nombre exact de documents non inclus : "X document(s) récent(s)"
- [ ] Badge "Non inclus" visible sur chaque ligne de document concernée dans le tableau
- [ ] Aucun indicateur si `synthesis()` est null
- [ ] Aucun indicateur si tous les documents sont antérieurs à la synthèse
- [ ] Les indicateurs disparaissent dès qu'une nouvelle synthèse est chargée (après re-analyse)
- [ ] Implémentation 100% frontend — comparaison `document.createdAt > synthesis.createdAt`

---

## Périmètre

### Hors scope

- Modification du backend (aucune)
- Tracking précis des documents inclus dans chaque synthèse (déféré — timestamp suffit pour V1)
- Indicateur sur les questions IA
- Notification push ou email

---

## Technique

### Endpoint(s)

Aucun nouvel endpoint. Données déjà disponibles :
- `synthesis.createdAt` via `CaseAnalysisResult`
- `document.createdAt` via `Document`

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable

### Composants Angular

- `CaseFileDetailComponent` — computed `outdatedDocuments`, badge warning synthèse, badge "Non inclus" dans le tableau

---

## Plan de test

### Tests unitaires (Karma)

- [ ] `outdatedDocuments()` retourne [] si synthesis null
- [ ] `outdatedDocuments()` retourne [] si tous les docs sont antérieurs à la synthèse
- [ ] `outdatedDocuments()` retourne les docs postérieurs à la synthèse
- [ ] Badge warning visible si `outdatedDocuments().length > 0`
- [ ] Badge "Non inclus" visible sur les lignes concernées

### Tests d'intégration

- [ ] Non applicable (pure logique frontend)

### Isolation workspace

- [ ] Non applicable — pas de nouvel accès aux données

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité à `CaseFileDetailComponent`

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — pas de navigation ni routing modifié

---

## Dépendances

### Subfeatures bloquantes

- SF-36-01 — statut : done
- SF-36-02 — statut : done

### Questions ouvertes impactées

- [ ] Aucune

---

## Notes et décisions

- Comparaison par timestamp : simple, sans changement backend. Légère imprécision théorique (doc uploadé avant la synthèse mais dont l'analyse a échoué) acceptée en V1.
- `synthesis.createdAt` doit être exposé par le backend — à vérifier dans `CaseAnalysisResponse`.
