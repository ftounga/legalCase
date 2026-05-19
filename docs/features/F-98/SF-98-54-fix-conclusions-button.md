# Mini-spec — F-98 / SF-98-54 — Correctif : bouton « Générer le projet de conclusions » grisé à tort après navigation

> Bugfix sur la feature F-98. Exempté des étapes 0 / 0 bis (cadrage cohérence +
> cohérence écran) — aucun élément visible nouveau, aucun nouveau workflow.

## Identifiant

`F-98 / SF-98-54`

## Feature parente

`F-98` — Génération de courrier / conclusions

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-98-54-fix-conclusions-button`

---

## Objectif

Empêcher que le bouton « Générer le projet de conclusions » de la section
Conclusions soit grisé à tort (message « Lancez et terminez l'analyse du
dossier ») alors que l'analyse du dossier est terminée.

---

## Contexte du bug

Constat utilisateur 2026-05-19 (prépa démo Renversez) : depuis la section
Conclusions, l'avocat suit le lien « constituez votre corpus de style »
(`/workspace/style-learning`), puis revient via le bouton « précédent » du
navigateur. Le bouton de génération est alors grisé avec le message
« Lancez et terminez l'analyse du dossier », bien que l'analyse soit faite.

### Cause racine

1. **Binding fragile** — `case-file-detail.component.html` :
   `[hasCompletedAnalysis]="synthesis() !== null"`. L'expression confond deux
   états : « pas d'analyse » et « synthèse pas encore (re)chargée ». Le
   composant `conclusions-section` attend `undefined` (et non `false`) tant que
   l'état est inconnu — pour laisser le backend trancher (`409
   ANALYSIS_NOT_READY`). Le binding ne produit jamais `undefined` : pendant
   toute fenêtre de chargement (notamment la ré-initialisation au retour de
   navigation), il vaut `false` → bouton grisé.
2. **Échec silencieux** — `loadSynthesis()` (`case-file-detail.component.ts`)
   a un handler d'erreur vide (`error: () => {}`). Un `getAnalysis` en échec
   transitoire laisse `synthesis()` durablement à `null`, sans retry.

---

## Comportement attendu

- À l'ouverture du dossier (y compris au retour de navigation), si une analyse
  `CASE_ANALYSIS` est terminée, le bouton « Générer le projet de conclusions »
  est **actif**.
- Tant que l'état des jobs d'analyse n'est pas connu, le pré-requis transmis au
  composant `conclusions-section` vaut `undefined` (le backend tranche) — le
  bouton n'est jamais grisé à tort.
- Si aucune analyse n'est terminée, le bouton reste grisé avec le message
  guidant (comportement inchangé, légitime).

---

## Correctif

| Cause | Correctif |
|-------|-----------|
| Binding fragile | Nouveau `computed` tri-état `hasCompletedAnalysis` dans `case-file-detail` : `undefined` tant que `analysisJobsLoaded` est faux, sinon booléen réel selon la présence d'un job `CASE_ANALYSIS` `DONE`. Le template binde `[hasCompletedAnalysis]="hasCompletedAnalysis()"`. |
| Échec silencieux | `loadSynthesis()` enveloppe `getAnalysis` dans `retry({ count: 2, delay: 1000 })` — un échec transitoire est retenté avant d'abandonner. |

`analysisJobsLoaded` (nouveau signal) passe à `true` dans le `next` de
`loadAnalysisJobs` (succès) ; sur échec du chargement des jobs, il reste `false`
→ `hasCompletedAnalysis` reste `undefined` (dégradation propre, le backend
tranche).

---

## Critères d'acceptation

- [ ] `hasCompletedAnalysis()` vaut `undefined` tant que `analysisJobsLoaded` est faux.
- [ ] `hasCompletedAnalysis()` vaut `true` si les jobs sont chargés et contiennent un `CASE_ANALYSIS` `DONE`.
- [ ] `hasCompletedAnalysis()` vaut `false` si les jobs sont chargés sans `CASE_ANALYSIS` `DONE`.
- [ ] `loadAnalysisJobs` réussi met `analysisJobsLoaded` à `true`.
- [ ] La section Conclusions n'affiche plus le bouton grisé à tort sur un dossier dont l'analyse est terminée.

---

## Périmètre

### Hors scope

- Aucune modification backend (le gate `409 ANALYSIS_NOT_READY` existe déjà).
- Aucune modification du composant `conclusions-section` (son contrat
  `hasCompletedAnalysis?` accepte déjà `undefined`).
- Refonte du pattern de chargement de la synthèse.

---

## Plan de test

### Tests unitaires (`case-file-detail.component.spec.ts`)

- [x] `hasCompletedAnalysis` — `undefined` tant que les jobs ne sont pas chargés.
- [x] `hasCompletedAnalysis` — `true` si jobs chargés avec `CASE_ANALYSIS` `DONE`.
- [x] `hasCompletedAnalysis` — `false` si jobs chargés sans `CASE_ANALYSIS` `DONE`.
- [x] `loadAnalysisJobs` réussi met `analysisJobsLoaded` à `true`.

### Isolation workspace

- [x] Non applicable — dérivation d'état UI interne, aucune nouvelle surface d'accès.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune** — pas d'auth, pas de workspace context, pas de plan/quota, pas
  de route nouvelle. Dérivation d'un pré-requis UI dans un composant existant.

### Smoke tests E2E concernés

- [x] Aucun — aucune préoccupation transversale (auth / workspace / navigation
  au sens routing) touchée.

---

## Notes

- Même famille que F-227 (résilience de l'état d'analyse à la navigation).
- Source : constat utilisateur + screenshot 2026-05-19.
