# Mini-spec — F-IM-05 / SF-IM-05-03 Interface questionnaire et fiche récapitulative (frontend)

---

## Identifiant

`F-IM-05 / SF-IM-05-03`

## Feature parente

`F-IM-05` — Arbre décisionnel type de titre

## Statut

`draft`

## Date de création

2026-04-07

## Branche Git

`feat/SF-IM-05-03-frontend-title-decision`

---

## Objectif

Créer un composant Angular qui affiche un questionnaire par étapes (pays, nationalité UE, motif, durée, situation familiale), appelle l'endpoint POST title-decision, et affiche la fiche récapitulative des titres recommandés. Section visible uniquement pour les dossiers DROIT_IMMIGRATION.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre un dossier DROIT_IMMIGRATION
2. Une nouvelle section "Titre de séjour recommandé" apparaît (collapsible, comme la checklist immigration)
3. Si aucune décision n'existe → affiche le questionnaire (5 champs : pays, nationalité UE, motif, durée, situation familiale)
4. L'avocat remplit le questionnaire et clique "Analyser"
5. POST vers l'API → affichage de la fiche récapitulative :
   - Pour chaque titre recommandé : code, libellé, conditions, pièces à fournir, délai moyen
   - Bouton "Refaire l'analyse" pour modifier les critères
6. Si une décision existe déjà → GET au chargement → affiche directement la fiche récapitulative
7. L'avocat peut modifier les critères et relancer l'analyse (upsert)

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Erreur API (400/500) | MatSnackBar avec message d'erreur |
| Aucun titre recommandé | Message "Aucun titre standard identifié pour ces critères" |
| Chargement en cours | Spinner sur le bouton "Analyser" |

---

## Critères d'acceptation

- [ ] La section n'apparaît que pour les dossiers DROIT_IMMIGRATION
- [ ] Le questionnaire contient les 5 champs avec les bonnes valeurs (mat-select appearance outline)
- [ ] Le POST est envoyé au clic sur "Analyser" et la fiche s'affiche
- [ ] Le GET charge la décision existante au ngOnInit
- [ ] La fiche affiche code, libellé, conditions, pièces, délai pour chaque titre
- [ ] Le bouton "Refaire l'analyse" affiche le questionnaire pré-rempli
- [ ] Les couleurs, polices et espacements respectent le design system
- [ ] Les données sont configurées pour FRANCE et BELGIQUE

---

## Périmètre

### Hors scope (explicite)

- Export PDF de la fiche
- Modification du pipeline IA
- Intégration avec la checklist F-IM-01

---

## Technique

### Composants Angular

| Composant | Rôle |
|-----------|------|
| `ImmigrationTitleDecisionSectionComponent` | Section collapsible avec questionnaire + fiche récapitulative |

### Service Angular

| Service | Rôle |
|---------|------|
| `ImmigrationTitleDecisionService` | Appels HTTP POST + GET vers `/api/v1/case-files/{id}/immigration/title-decision` |

### Modèle TypeScript

| Interface | Champs |
|-----------|--------|
| `TitleDecisionRequest` | country, nationaliteUe, motif, duree, situationFamiliale |
| `TitleDecisionResponse` | caseFileId, country, nationaliteUe, motif, duree, situationFamiliale, recommendations[] |
| `TitleRecommendation` | code, label, country, motif, conditions, pieces[], delaiMoyenJours |

### Intégration dans case-file-detail

Ajout d'un bloc `@if (caseFile()!.legalDomain === 'DROIT_IMMIGRATION')` dans `case-file-detail.component.html`, après la checklist immigration existante.

---

## Plan de test

### Tests unitaires

- [ ] Composant créé sans erreur
- [ ] Section cachée si domaine != DROIT_IMMIGRATION (testé via case-file-detail)
- [ ] POST appelé au clic sur "Analyser"
- [ ] GET appelé au ngOnInit
- [ ] Fiche récapitulative affichée après résolution

### Isolation workspace

- [x] Non applicable côté frontend — l'isolation est garantie par le backend (SF-IM-05-02)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] **Navigation / routing frontend** — pas de nouvelle route, section ajoutée dans un composant existant
- [x] **Aucune préoccupation transversale** — ajout d'une section conditionnelle dans case-file-detail

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné (ajout conditionnel, pas de modification de route/guard)

---

## Dépendances

### Subfeatures bloquantes

- SF-IM-05-01 — done
- SF-IM-05-02 — done

---

## Notes et décisions

- Le composant suit exactement le pattern de `ImmigrationChecklistSectionComponent` : standalone, signals, collapsible, mat-form-field outline
- La situation familiale est optionnelle — affichée uniquement quand motif = FAMILLE
- Les titres recommandés sont affichés en cards avec icône check_circle dorée
