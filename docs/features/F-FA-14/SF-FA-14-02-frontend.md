# Mini-spec — F-FA-14 / SF-FA-14-02 Ordonnance de protection FR — FRONTEND

## Identifiant

`F-FA-14 / SF-FA-14-02`

## Feature parente

`F-FA-14` — Ordonnance de protection (violences conjugales, art. 515-9 Cciv) — 🔴 critique

## Statut `draft` · Date `2026-04-25` · Branche `feat/SF-FA-14-02-frontend-ordonnance-protection`

## Pattern de référence

- Canonique : `harcelement-licenciement-nul-section` (`ai-skills/frontend-coherence-audit.md` §5).
- Miroir le plus proche (multi-select enum + scoring famille FR) : `divorce-faute-section` (F-FA-09 SF-FA-09-02).
- Pré-fill IA + F-IA-03 multi-sources : `divorce-accepte-section` (F-FA-10 SF-FA-10-02) et `immigration-title-decision-section` (F-IM-05).

---

## Objectif

Intégrer dans le panel décisionnel F-IA-04 le composant Angular `<app-ordonnance-protection-section>` consommant l'API SF-FA-14-01 (POST/GET `/api/v1/case-files/{caseFileId}/ordonnance-protection`), pour permettre à un avocat **droit de la famille FR** d'estimer en quelques secondes la **vraisemblance** d'une demande d'ordonnance de protection (art. 515-9 à 515-13 Cciv) et la **liste des mesures recommandées** (TGD, BAR, éviction, etc.).

---

## Contrat API

> Contrat importé de SF-FA-14-01 (PR #568 backend mergée). Aucune modification autorisée côté frontend.

### POST `/api/v1/case-files/{caseFileId}/ordonnance-protection`

Body (champs hors `dateRequete`/`ageEnfants` requis) :

```json
{
  "dateRequete": "2026-04-20",
  "violencesAlleguees": ["PHYSIQUES", "PSYCHOLOGIQUES", "MENACES_MORT"],
  "preuvesViolences": ["CONSTAT_HUISSIER", "CERTIFICAT_MEDICAL"],
  "dangerImmediat": true,
  "presenceEnfants": true,
  "ageEnfants": [5, 8],
  "logementCommun": true,
  "victimeFinanciairementDependante": false,
  "demandeurDejaProtege": false,
  "demandeMesures": ["EVICTION_CONJOINT", "INTERDICTION_APPROCHER", "TGD", "BAR"]
}
```

Réponse 200 :

```json
{
  "caseFileId": "uuid",
  "dateRequete": "2026-04-20",
  "violencesAlleguees": [...],
  "preuvesViolences": [...],
  "dangerImmediat": true,
  "presenceEnfants": true,
  "ageEnfants": [5, 8],
  "logementCommun": true,
  "victimeFinanciairementDependante": false,
  "demandeurDejaProtege": false,
  "demandeMesures": [...],
  "scoreVraisemblance": 95,
  "verdictProbabiliteOctroi": "ELEVEE",
  "mesuresRecommandees": ["EVICTION_CONJOINT", "INTERDICTION_APPROCHER", "TGD", "BAR"],
  "delaiTraitementJoursPrevisionnel": 6,
  "baseJuridique": "Art. 515-9 à 515-13 Cciv + Loi 30/07/2020 (BAR)",
  "formule": "Score 95 = vraisemblance des faits élevée (≥ 75)…",
  "messages": [
    "Audience à demander en urgence — délai indicatif 6 jours (art. 515-11 Cciv)",
    "TGD (Téléphone Grave Danger) attribué au plus tard 24h après ordonnance",
    "BAR autorisé depuis Loi 30/07/2020 si danger immédiat caractérisé"
  ],
  "country": "FRANCE"
}
```

Codes d'erreur :

| Situation | HTTP | Côté frontend |
|---|---|---|
| `violencesAlleguees` vide | 400 | snackbar erreur (form ré-éditable) |
| Workspace BELGIQUE | 400 | bannière info (gate) — POST jamais émis |
| Dossier hors DROIT_FAMILLE | 400 | snackbar erreur |
| Aucune analyse persistée | 404 sur GET | mode formulaire (pas une erreur) |

### GET `/api/v1/case-files/{caseFileId}/ordonnance-protection`

Retourne la dernière analyse (404 si jamais calculée — comportement nominal, le composant reste en mode formulaire).

---

## Comportement attendu

### Cas nominal (FRANCE)

1. Le composant est monté par `decisional-tools-panel` (visibility ALWAYS_ON FR DROIT_FAMILLE).
2. `ngOnInit()` :
   - `load()` : GET `/ordonnance-protection`. Si 200 → résultat affiché en mode résumé. Si 404 → form vierge.
   - `loadSourceExplanations()` (F-IA-03-15c — fail-open).
3. `prefillFromAi()` (depuis `aiData?: FamilleExtractedData`) ré-applique les valeurs `dateRequete`, `dangerImmediat`, `presenceEnfants`, `logementCommun`, `victimeFinanciairementDependante`, `demandeurDejaProtege`, `violencesAlleguees`, `preuvesViolences` si l'IA les a extraites — no-op gracieux si absent.
4. L'avocat coche/saisit les champs. Pour chaque champ pré-rempli IA, badge `auto_awesome` "Pré-rempli depuis l'analyse" affiché ; le badge disparaît au premier `onXxxChange()` manuel.
5. POST `/ordonnance-protection`. Réponse 200 → `result.set(r)`, `showForm.set(false)`, snackbar succès, `dashboardRefresh.triggerRefresh()`.
6. Affichage résultat : bannière verdict (palette navy/or/rouge classique — voir §Palette), score vraisemblance, liste `mesuresRecommandees` en `mat-chip-set`, délai 6 jours en JetBrains Mono, `messages` en liste.
7. Bouton "Modifier" → `editMode()` → re-bascule `showForm.set(true)`.

### Cas d'erreur

| Situation | UI |
|---|---|
| `workspaceCountry !== 'FRANCE'` | Bannière info "Outil FR uniquement — équivalent BE = art. 1253ter CJ (à venir, F-FA-14-BE backlog)". Form non rendu. GET non émis. |
| Form invalide (violences vide) | Bouton submit disabled |
| POST 400 / 500 | Snackbar `panelClass: snack-error`, form re-éditable |
| GET 404 | Form vierge, pas d'erreur |
| `aiData` change après mount | `ngOnChanges()` ré-invoque `prefillFromAi()` si form encore vierge |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils décisionnels famille FR** :
  - `divorce-faute-section` (F-FA-09) → pattern multi-select enum + scoring + verdict, importé tel quel.
  - `divorce-accepte-section` (F-FA-10) → pré-fill IA + F-IA-03 multi-sources, importé tel quel.
  - `partage-immobilier-section` (F-FA-05) → palette navy/or, importée.
- [x] **Autres pays** : équivalent BE (`art. 1253ter CJ`) **hors scope** — feature backlog `F-FA-14-BE` (référencée dans la mini-spec backend SF-FA-14-01 §"Parité"). Bannière info gate FRANCE.
- [x] **Autres domaines** : DROIT_DU_TRAVAIL / DROIT_IMMIGRATION non pertinents (non-applicabilité juridique). Visibility ALWAYS_ON sur DROIT_FAMILLE FR uniquement, géré côté backend.
- [x] **TOOL_REGISTRY** : entrée à ajouter avec `inputs: (ctx) => ({ caseFileId, workspaceCountry, aiData: ctx.synthesis?.familleExtractedData, procedureChecks, aiQuestions, piecesManquantes })` symétrique à F-FA-09/10.
- [x] **F-IA-03** : alertes de cohérence multi-sources (`IA` / `F96` / `QUESTION_IA` / `PIECE_MANQUANTE`) sur 3 fields clés candidats : `DATE_REQUETE` (cohérence date IA), `VIOLENCES_ALLEGUEES` (cohérence ensembliste IA-extraite), `LOGEMENT_COMMUN` (cohérence boolean IA). Réutilisation stricte du `CoherenceAlertBuilder` partagé (chemin `frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts`) — pas d'interface locale `CoherenceAlert`.
- [x] **F-IA-02 dashboard refresh** : `CaseDashboardRefreshService.triggerRefresh()` post-POST succès, comme F-FA-09/10.
- [x] **Datepicker** : `<input type="date">` natif (convention projet — pas MatDatepicker).
- [x] **Palette** : palette navy/or/rouge **classique** (verdict ELEVEE/MOYENNE/FAIBLE → strong/medium/weak). Le rouge ELEVEE est volontairement **inversé sémantiquement** par rapport à F-FA-09 (probabilité élevée d'octroi = bon pour la victime), donc on garde la palette classique navy strong, sans bascule en gradation rouge dominante. Justification documentée en SCSS (référence SF-155-07/DIV-8).

### Pattern UI réutilisé

Aucune nouveauté — réutilisation stricte de `divorce-faute-section`. Pas de nouveau composant partagé.

---

## Composants impactés

### Créés

- `frontend/src/app/core/models/ordonnance-protection.model.ts` (interfaces request/response + enum types + helpers labels).
- `frontend/src/app/core/services/ordonnance-protection.service.ts` (HttpClient wrapper).
- `frontend/src/app/case-files/ordonnance-protection-section/ordonnance-protection-section.component.ts`
- `.html`, `.scss`, `.spec.ts`

### Modifiés

- `frontend/src/app/core/models/divorce-accepte.model.ts` : extension `FamilleExtractedData` avec champs ordonnance protection (`dateRequeteOP`, `dangerImmediatDetected`, `presenceEnfantsDetected`, `logementCommunDetected`, `violencesAllegueesDetectees`, `preuvesViolencesDetectees`, `victimeFinanciairementDependanteDetected`, `demandeurDejaProtegeDetected`) — tous optionnels, no-op gracieux.
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` : import + entrée `TOOL_REGISTRY['F-FA-14-ordonnance-protection']`.

### Tests modifiés

Aucun (zéro régression sur composants existants).

---

## Plan de test

### Spec Jest (≥ 14 cas)

- mount sans erreur (FRANCE) — fautesOptions = 5, mesuresOptions = 7, preuvesOptions = 8.
- formValid faux si `violencesAlleguees` vide.
- formValid faux si `demandeMesures` vide.
- formValid vrai si ≥ 1 violence + ≥ 1 mesure + tous les booléens définis.
- GET 200 → form masqué, valeurs persistées, badge IA absent.
- GET 404 → mode formulaire ; pré-fill IA appliqué (provenance IA).
- calculate() POST → résultat affiché + snackbar succès + dashboardRefresh appelé.
- calculate() erreur 400 → snackbar rouge.
- calculate() ignoré si form invalide (pas d'appel HTTP).
- onViolencesChange efface badge IA.
- onLogementCommunChange efface badge IA.
- coherenceAlerts.VIOLENCES_ALLEGUEES présent si IA détecte un set ≠ saisie avocat.
- coherenceAlerts.LOGEMENT_COMMUN présent si IA dit `true` et avocat saisit `false`.
- alertes masquées après showForm=false.
- gate BELGIQUE → form non rendu, GET non appelé.
- gate FRANCE → load() appelé.
- ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide.
- toggleCollapse, editMode, verdictBannerClass, alertBadgeLabel.
- explanationFor renvoie [] fail-open.

### Tsc strict

`npx tsc --noEmit -p tsconfig.app.json` doit passer.

### Self-check pre-commit

Tous seuils du brief ≥ 1 (CoherenceAlertBuilder ≥ 2, popover ≥ 3, prefillFromAi ≥ 2, auto_awesome ≥ 2, provenance ≥ 6, coherenceAlerts ≥ 1, handlers ≥ 2, interface locale `CoherenceAlert` = 0).

---

## Préoccupations transversales

- **Outil décisionnel métier** : nouveau composant. Composants impactés listés ci-dessus. Aucun outil existant modifié. Pattern réutilisé strictement.
- **Auth / Principal** : pas de changement.
- **Workspace context** : gate FRANCE inchangé (déjà géré par F-IA-04).
- **Plans / limites** : pas de gate plan supplémentaire.
- **Navigation / routing** : aucun nouveau route.

Aucun smoke test E2E impacté (composant secondaire dans le panel).

---

## Impact par domaine métier

DROIT_FAMILLE FR uniquement. Sensible au domaine et au pays — équivalent BE séparé (backlog F-FA-14-BE / SF-FA-14-04). Bannière info BE en frontend pour expliciter le gate. Non pertinent pour DROIT_DU_TRAVAIL et DROIT_IMMIGRATION.

## Parité des domaines métier (niveau ≥ 5)

Outil de **niveau 5 (scoring + verdict)**. Parité reprise de SF-FA-14-01 :
- DROIT_FAMILLE FR : couvert par cette SF.
- DROIT_FAMILLE BE : équivalent backlog `F-FA-14-BE` (cf. SF-FA-14-01).
- DROIT_DU_TRAVAIL : non pertinent.
- DROIT_IMMIGRATION : non pertinent.

---

## Hors scope

- Backend (livré en SF-FA-14-01 PR #568).
- Équivalent Tribunal de la famille BE (F-FA-14-BE backlog).
- Suivi du renouvellement de l'OP au-delà de 6 mois (SF future).
- Articulation avec procédure pénale (plainte, ITT) — feature distincte.
- Détection automatique des violences par pipeline IA (champs `*Detected` ajoutés à `FamilleExtractedData` mais pipeline backend non branché — comportement no-op gracieux). Branchement IA pipeline = SF future.

---

## Critères d'acceptation

- [ ] Composant `<app-ordonnance-protection-section>` standalone, monté via TOOL_REGISTRY.
- [ ] Form complet : datepicker `dateRequete`, mat-select multiple `violencesAlleguees` (5 options), mat-select multiple `preuvesViolences` (8 options), 5 slide-toggles (`dangerImmediat`, `presenceEnfants`, `logementCommun`, `victimeFinanciairementDependante`, `demandeurDejaProtege`), mat-select multiple `demandeMesures` (7 options).
- [ ] Bannière verdict (palette navy/or classique) avec score 0-100, `mesuresRecommandees` en chips, délai `6 j` en JetBrains Mono, `messages` listés, base juridique citée.
- [ ] Pré-fill IA depuis `FamilleExtractedData` (champs *Detected) — badge auto_awesome par champ pré-rempli, effacement au changement manuel.
- [ ] Validation F-IA-03 multi-sources (≥ 3 fields, `CoherenceAlertBuilder` partagé) — alerte popover par field.
- [ ] Gate `workspaceCountry === 'FRANCE'` strict avec bannière info BE.
- [ ] `CaseDashboardRefreshService.triggerRefresh()` post-POST succès.
- [ ] `MatSnackBar` pour erreurs HTTP.
- [ ] Datepicker `<input type="date">` natif.
- [ ] JetBrains Mono pour `baseJuridique`, `formule`, `delaiTraitement`.
- [ ] ≥ 14 specs Jest verts.
- [ ] tsc strict passe.
- [ ] Self-check pre-commit : tous seuils respectés.
- [ ] Pas d'interface locale `CoherenceAlert` (réutiliser le partagé).

---

## Notes

- Le rouge sémantique (verdict FAIBLE / risque) reste réservé à `--weak` (probabilité d'octroi faible = échec procédural). La probabilité ELEVEE est navy strong (succès procédural pour la victime). Cohérent avec F-FA-09.
- Les champs `*Detected` ajoutés à `FamilleExtractedData` sont déclaratifs — le pipeline IA backend ne les remplit pas encore. Comportement no-op gracieux garanti par les `typeof` checks dans `prefillFromAi()`.
- Sévérité IA-03 : `WARNING` par défaut. Pas de `CRITICAL` (l'urgence métier 6 jours JAF est déjà signalée par les `messages` côté résultat).
