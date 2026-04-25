# SF-IM-11-02 — Frontend changement de statut CESEDA (FR)

> **Feature parente** : F-IM-11 Changement de statut (passage d'un titre de séjour à un autre).
> **Pays** : FRANCE uniquement (équivalent BE = procédure 9bis OE — feature jumelle au backlog F-IM-11-BE).
> **Statut** : `In progress` — clôt F-IM-11 (backend SF-IM-11-01 mergé via PR #635).
> **Branche** : `feat/SF-IM-11-02-frontend-changement-statut`
> **Pattern de référence canonique** : `protection-rp-section` (SF-DT-30-02 PR #634 — pattern récent : form multi-sections FR + bandeau verdict 3 niveaux + chips informatifs + alert builder) ; gate FR avec bannière info importé de `aes-metiers-tension-section` (SF-IM-09-05).
> **Contrat importé de SF-IM-11-01** (mergé PR #635).

## Objectif (1 phrase)

Exposer côté frontend un composant Angular `<app-changement-statut-section>` qui consomme `POST/GET /api/v1/case-files/{caseFileId}/changement-statut-analysis`, affiche un formulaire à 6 champs (titre actuel/envisagé, durée restante, justificatif, rémunération conditionnelle, casier judiciaire) et un bandeau verdict ELEVEE/MOYENNE/FAIBLE avec pré-remplissage IA et validation F-IA-03 sur le titre actuel.

## Comportement nominal

L'avocat ouvre la fiche dossier (droit immigration, FRANCE). Le panel décisionnel F-IA-04 affiche la section ALWAYS_ON `Changement de statut (FR)`. Au déploiement :
1. GET `/api/v1/case-files/{caseFileId}/changement-statut-analysis` est appelé. 200 → résultat hydraté (mode résultat). 404 → mode formulaire.
2. **Pré-fill IA** depuis `aiData: ImmigrationExtractedData` :
   - `titreActuel` ← mappé depuis `aiData.typeTitreSejourCode` (ETUDIANT / VISITEUR / VPF / SALARIE / PASSEPORT_TALENT_*) avec normalisation insensible à la casse.
   - Provenance signal + badge `auto_awesome` "Pré-rempli depuis l'analyse" + handler `onTitreActuelChange()` qui efface la provenance au changement manuel.
3. L'avocat complète :
   - **Titre actuel** (radio, 7 valeurs) — obligatoire, pré-fill possible.
   - **Titre envisagé** (radio, 7 valeurs) — obligatoire.
   - **Durée restante sur titre actuel (mois)** (number ≥ 0) — obligatoire.
   - **Document justificatif fourni** (toggle) — facultatif.
   - **Rémunération contrat (€/mois)** (number ≥ 0) — visible **uniquement si** `titreEnvisage = SALARIE`, requis si visible.
   - **Casier judiciaire vierge** (toggle, default true) — facultatif.
4. Bouton "Analyser" → POST → bandeau verdict.
   - **ELEVEE** → navy/info, icône `verified` — transition admise + tous critères réunis.
   - **MOYENNE** → or/warning, icône `warning` — transition possible mais conditions limites.
   - **FAIBLE** → rouge alerte, icône `gpp_bad` — critère bloquant (durée < 2 mois, casier non vierge, justificatif absent pour transitions exigeantes).
5. Liste `documentsRequis` en chips informatifs navy.
6. Liste `risqueRefus` en chips alerte (or si modéré, rouge si bloquant).
7. `delaiInstructionMois` affiché en JetBrains Mono.
8. `CaseDashboardRefreshService.triggerRefresh()` après POST succès.
9. **Validation F-IA-03** sur le field `TITRE_ACTUEL` — alerte de cohérence si la valeur saisie diverge de `aiData.typeTitreSejourCode` mappé. Multi-sources `IA / F96 / QUESTION_IA / PIECE_MANQUANTE` via `CoherenceAlertBuilder` partagé.

## Cas d'erreur (UI)

- 400 backend (titre null, transition non supportée, durée négative, rémunération négative, pays BE détecté, domaine ≠ DROIT_IMMIGRATION) → `MatSnackBar` rouge avec message backend.
- 404 GET → mode formulaire (no-op silencieux).
- 404 POST → snack `Dossier introuvable`.
- Réseau / 5xx → snack rouge `Erreur lors de l'analyse`.
- Mismatch pays (`workspaceCountry === 'BELGIQUE'`) → bannière info "Régime CESEDA français uniquement — équivalent BE traité dans une feature jumelle (procédure 9bis OE)". Pas d'appel HTTP, pas de masquage silencieux.
- Form invalide → bouton désactivé, pas d'appel HTTP.

## Critères d'acceptation

1. Composant standalone `ChangementStatutSectionComponent` exporté et intégré au `TOOL_REGISTRY` (`'F-IM-11-changement-statut'`) avec signature symétrique aux autres FR-only IM.
2. Gate `workspaceCountry === 'FRANCE'` → si BELGIQUE : bannière info, aucun appel HTTP.
3. GET 200 hydrate le résultat persisté (mode résultat, sans afficher le form).
4. GET 404 reste en mode formulaire et déclenche `prefillFromAi()`.
5. Pré-fill IA `titreActuel` mappé correctement depuis `aiData.typeTitreSejourCode` (cas exact ou normalisé).
6. Provenance signal + badge auto_awesome présent quand pré-fill actif. Handler `onTitreActuelChange()` efface la provenance.
7. Champ `remunerationContratEur` masqué si `titreEnvisage !== 'SALARIE'`. Visible et requis si SALARIE.
8. POST verdict ELEVEE → bandeau navy.
9. POST verdict MOYENNE → bandeau or.
10. POST verdict FAIBLE → bandeau rouge.
11. `documentsRequis` rendus en chips navy. `risqueRefus` en chips or/rouge.
12. `CaseDashboardRefreshService.triggerRefresh()` invoqué après POST succès.
13. `MatSnackBar` rouge sur erreur 400 / 5xx (pas d'alert/confirm natif).
14. Validation F-IA-03 active : `coherenceAlerts().TITRE_ACTUEL` non-null si `aiData.typeTitreSejourCode` mappé diverge de la saisie avocat. Source `MULTI` quand convergence IA + F96.
15. `coherenceAlerts` vide en mode résultat (`showForm = false`).
16. `baseJuridique` et `formule` rendus en JetBrains Mono. Le reste en Inter.

## Plan de test

### Jest unit tests (≥ 13 — `changement-statut-section.component.spec.ts`)

1. `FRANCE → isFrance() true, GET appelé au ngOnInit`.
2. `BELGIQUE → bannière info, aucun appel HTTP`.
3. `GET 200 hydrate le résultat (mode résultat)`.
4. `GET 404 reste en mode formulaire`.
5. `pré-fill IA : titreActuel ← aiData.typeTitreSejourCode` (cas exact `ETUDIANT`).
6. `pré-fill IA : normalisation insensible casse` (`'etudiant'` → `ETUDIANT`).
7. `pré-fill sans aiData → aucun pré-remplissage, aucun badge`.
8. `onTitreActuelChange efface le badge IA`.
9. `champ remunerationContratEur masqué si titreEnvisage != SALARIE, visible si SALARIE`.
10. `formValid false initialement, true seulement quand tous les champs requis présents`.
11. `formValid false si remunerationContratEur manquante quand titreEnvisage = SALARIE`.
12. `calculate() POST + résultat + snackbar succès + triggerRefresh`.
13. `calculate() ignoré si form invalide`.
14. `calculate() erreur backend → snackbar rouge`.
15. `coherenceAlerts.TITRE_ACTUEL présent si IA diverge de saisie`.
16. `coherenceAlerts.TITRE_ACTUEL absent si IA convergent`.
17. `coherenceAlerts vides après calcul (showForm=false)`.
18. `bannerClass mappe verdict → classe CSS attendue`.

### Régression

- `decisional-tools-panel.component.spec.ts` : doit toujours passer (entrée TOOL_REGISTRY ajoutée). Lancer `npx jest decisional-tools-panel`.

### Self-check grep pré-commit (5 patterns canoniques)

```
grep -rn "alert(\|confirm(" frontend/src/app/case-files/changement-statut-section/  # → 0
grep -rn "MatDatepicker" frontend/src/app/case-files/changement-statut-section/      # → 0
grep -rn "auto_awesome" frontend/src/app/case-files/changement-statut-section/       # → ≥1
grep -rn "CoherenceAlertBuilder" frontend/src/app/case-files/changement-statut-section/ # → ≥1
grep -rn "triggerRefresh()" frontend/src/app/case-files/changement-statut-section/   # → ≥1
```

## Tables / endpoints / composants impactés

### Endpoints consommés (figés par SF-IM-11-01 PR #635)

- `POST /api/v1/case-files/{caseFileId}/changement-statut-analysis` — calcul + upsert
- `GET /api/v1/case-files/{caseFileId}/changement-statut-analysis` — retour persisté

### Composants nouveaux (frontend)

- `frontend/src/app/case-files/changement-statut-section/changement-statut-section.component.ts/html/scss/spec.ts`
- `frontend/src/app/core/models/changement-statut.model.ts`
- `frontend/src/app/core/services/changement-statut.service.ts`

### Composants modifiés

- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` — ajout entrée TOOL_REGISTRY `'F-IM-11-changement-statut'` (tool_id aligné migration 170).

### Pas de modification backend.

## Contrat API (importé de SF-IM-11-01)

### Request `ChangementStatutRequest`

```ts
{
  titreActuel: 'ETUDIANT' | 'VISITEUR' | 'VPF' | 'SALARIE' | 'PASSEPORT_TALENT_SALARIE_QUALIFIE' | 'PASSEPORT_TALENT_INNOVANT' | 'APS', // requis
  titreEnvisage: same enum, // requis
  dureeRestanteSurTitreActuelMois: number, // ≥ 0 requis
  documentJustificatifFourni: boolean,
  remunerationContratEur?: number | null, // requis si titreEnvisage = SALARIE
  casierJudiciaireVierge: boolean
}
```

### Response `ChangementStatutResponse`

```ts
{
  caseFileId: string,
  country: 'FRANCE',
  titreActuel: string,
  titreEnvisage: string,
  nouveauTitreEnvisage: string,
  dureeRestanteMois: number,
  documentJustificatifFourni: boolean,
  remunerationContratEur: number | null,
  casierJudiciaireVierge: boolean,
  verdictTransition: 'ELEVEE' | 'MOYENNE' | 'FAIBLE',
  documentsRequis: string[],
  risqueRefus: string[],
  delaiInstructionMois: number, // 2-4
  baseJuridique: string,
  formule: string,
  messages: string[]
}
```

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils décisionnels frontend** : protection-rp-section (template canonique récent V8), aes-metiers-tension-section (gate FR avec bannière info), immigration-title-decision-section (pattern de pré-fill IA depuis ImmigrationExtractedData).
- [x] **Pays** : FR uniquement — BE = procédure 9bis OE distincte, backlog jumeau F-IM-11-BE déjà identifié dans la mini-spec backend.
- [x] **Domaine** : DROIT_IMMIGRATION uniquement.
- [x] **UI patterns** : alert builder partagé `CoherenceAlertBuilder`, JetBrains Mono pour `formule` + `baseJuridique`, palette navy/or/rouge canonique, `<input type="date">` natif (pas MatDatepicker), MatSnackBar pour erreurs.

### Décision

- [x] Étendu à toutes les cibles applicables (FR uniquement par scope CESEDA)
- [x] Subfeature jumelle BE déjà au backlog (F-IM-11-BE)

## Impact par domaine métier

Cette feature est sensible au domaine — DROIT_IMMIGRATION uniquement (régime CESEDA). Les autres domaines (droit du travail, famille) ne sont pas concernés.

## Préoccupations transversales

- [x] **Outil décisionnel métier** — frontend d'un outil existant (backend mergé). Tool_id `F-IM-11-changement-statut` aligné migration 170.
- [ ] Auth / Principal — non impacté.
- [ ] Workspace context — pas de nouveau resolver, gate standard.
- [ ] Plans / limites — non impacté.
- [ ] Navigation / routing — pas de nouvelle route, intégration via TOOL_REGISTRY.

## Hors périmètre

- Génération automatique du dossier de demande préfectorale.
- Suivi de l'instruction (CaseDeadline traité en F-IM-16).
- Belgique (F-IM-11-BE backlog).
