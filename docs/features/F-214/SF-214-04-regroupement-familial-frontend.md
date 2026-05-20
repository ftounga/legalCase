# Mini-spec — F-214 / SF-214-04 — Regroupement familial — frontend

## Identifiant

`F-214 / SF-214-04`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-214-04-regroupement-familial-frontend`

---

## Objectif

Livrer le composant Angular `<app-regroupement-familial-section>` pour l'outil `F-IM-26-regroupement-familial-fr`, conforme au pattern canonique F-IA-04, avec calculateur ressources SMIC et surface habitable interactif.

---

## Comportement attendu

### Cas nominal

- Formulaire : `dureeSejourRegulierMois` (number), `ressourcesMensuellesNettes` (number), `tailleLogementM2` (number), `nombrePersonnesFoyer` (number), `typeRegroupement` (select), `membresFamilleARegrouper` (number 1-6).
- Affichage résultat : verdict chip, `ressourcesRequises` (JetBrains Mono, en euros), `surfaceRequise` (m²), `chipsCriteresNonRemplis`.
- `prefillFromAi()` depuis `ImmigrationExtractedData` (`aesDureePresenceMois` → dureeSejourRegulierMois ; `regroupementRessourcesMensuelles` → ressourcesMensuellesNettes).
- Gate country FRANCE + bannière info si BE.
- `CaseDashboardRefreshService.triggerRefresh()` dans next:.

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| POST échoue | MatSnackBar |
| aiData absent | Formulaire vide |

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques (visuelle, pré-fill, F-IA-03, TOOL_REGISTRY, getPrefillCount, parité domaine)
- Niveau outil : 4 (calculateur) — parité domaine non applicable (regroupement familial FR = spécifique).

---

## Critères d'acceptation

- [x] Composant BUILD SUCCESS 0 erreur TypeScript
- [x] POST nominal → verdict + ressourcesRequises affiché
- [x] prefillFromAi() pré-remplit dureeSejourRegulierMois depuis aesDureePresenceMois
- [x] Tests Jest ≥ 15
- [x] TOOL_REGISTRY + KNOWN_FRONTEND_TOOL_IDS

## Plan de test minimal

- Jest : composant spec (≥ 10), service spec (≥ 5), prefill-rules spec (≥ 3 getPrefillCount)

## Tables / endpoints / composants impactés

- **Nouveau composant** `RegroupementFamilialSectionComponent`
- **Nouveau service** `RegroupementFamilialService`
- **Nouveau modèle** `RegroupementFamilialAnalysis`
- **Nouveau fichier** `regroupement-familial-prefill-rules.ts`
- **Modification** `decisional-tools-panel.component.ts` : ajout entrée TOOL_REGISTRY

## Hors périmètre

- Backend (SF-214-03)

## Dépendances

- SF-214-03 : statut `done`
