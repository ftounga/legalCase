# SF-DT-30-02 — Frontend protection des représentants du personnel (FR)

> **Feature parente** : F-DT-30 Protection des représentants du personnel (V8).
> **Pays** : FRANCE uniquement (équivalent BE = statut protégé belge, loi 19 mars 1991 — feature jumelle au backlog).
> **Statut** : `In progress` — clôt F-DT-30 (backend SF-DT-30-01 mergé via PR #631).
> **Branche** : `feat/SF-DT-30-02-frontend-protection-rp`
> **Pattern de référence canonique** : `pse-section` (SF-DT-14-02 PR #627 — pattern récent identique : form multi-axes FR + bandeau verdict NUL/CONTESTABLE/VALIDE colorisé + alert builder).
> **Contrat importé de SF-DT-30-01** (mergé PR #631).

## Objectif (1 phrase)

Exposer côté frontend un composant Angular `<app-protection-rp-section>` qui consomme les endpoints `POST/GET /api/v1/case-files/{caseFileId}/protection-rp-analysis`, affiche un formulaire à 6 axes (statut, dates mandat/rupture, procédure, motif, salaire optionnel) et un bandeau verdict colorisé (NUL rouge / CONTESTABLE or / VALIDE navy) avec pré-remplissage IA et validation F-IA-03 sur le motif de licenciement.

## Comportement nominal

L'avocat ouvre la fiche dossier (droit du travail, FRANCE). Le panel décisionnel F-IA-04 affiche la section ALWAYS_ON `Protection des représentants du personnel (FR)`. Au déploiement :
1. GET `/api/v1/case-files/{caseFileId}/protection-rp-analysis` est appelé. 200 → résultat hydraté (mode résultat). 404 → mode formulaire.
2. **Pré-fill IA** depuis `aiData: TravailExtractedData` :
   - `dateExpirationMandat` ← (champ non encore extrait — no-op gracieux, branchement IA ultérieur)
   - `motifLicenciement` ← `aiData.motifLicenciement` mappé vers l'enum backend (FAUTE_GRAVE / INSUFFISANCE_PRO / ECONOMIQUE / INAPTITUDE / AUTRE) avec normalisation insensible à la casse / tirets.
   - Provenance signal + badge `auto_awesome` "Pré-rempli depuis l'analyse" + handler `onMotifChange()` qui efface la provenance au changement manuel.
3. L'avocat complète :
   - **Statut protégé** (radio, 9 valeurs) — obligatoire.
   - **Date d'expiration du mandat** (`<input type="date">`) — obligatoire.
   - **Date présumée de rupture** (`<input type="date">`) — obligatoire.
   - **Procédure suivie** (radio, 4 valeurs) — obligatoire.
   - **Motif de licenciement** (radio, 5 valeurs, pré-fill possible) — obligatoire.
   - **Salaire mensuel brut** (number, optionnel) — pour estimer indemnités si verdict NUL.
4. Bouton "Analyser" → POST → bandeau verdict.
   - **NUL** → rouge alerte, icône `gpp_bad`, indemnité forfaitaire en JetBrains Mono (`≥ 6 × salaire`) + salaires éviction + délai contestation.
   - **CONTESTABLE** → or, icône `warning`.
   - **VALIDE** → navy/info, icône `verified`.
5. `CaseDashboardRefreshService.triggerRefresh()` après POST succès.
6. **Validation F-IA-03** sur le field `MOTIF_LICENCIEMENT` — alerte de cohérence si la valeur saisie diverge de `aiData.motifLicenciement` mappé. Multi-sources `IA / F96 / QUESTION_IA / PIECE_MANQUANTE` via `CoherenceAlertBuilder` partagé.

## Cas d'erreur (UI)

- 400 backend (statut/date/procédure/motif null, ou pays BE détecté côté backend) → `MatSnackBar` rouge avec message backend.
- 404 GET → mode formulaire (no-op silencieux).
- 404 POST → snack `Dossier introuvable`.
- Réseau / 5xx → snack rouge `Erreur lors de l'analyse`.
- Mismatch pays (`workspaceCountry === 'BELGIQUE'`) → bannière info "Outil français uniquement — équivalent BE traité dans une feature jumelle". Pas d'appel HTTP, pas de masquage silencieux.
- Form invalide → bouton désactivé, pas d'appel HTTP.

## Critères d'acceptation

1. Composant standalone `ProtectionRpSectionComponent` exporté et intégré au `TOOL_REGISTRY` (`'F-DT-30-protection-rp'`) avec signature symétrique aux autres FR-only.
2. Gate `workspaceCountry === 'FRANCE'` → si BELGIQUE : bannière info, aucun appel HTTP.
3. GET 200 hydrate le résultat persisté (mode résultat, sans afficher le form).
4. GET 404 reste en mode formulaire et déclenche `prefillFromAi()`.
5. Pré-fill IA `motifLicenciement` mappé correctement depuis `aiData.motifLicenciement` (cas exact ou normalisé).
6. Provenance signal + badge auto_awesome présent quand pré-fill actif. Handler `onMotifChange()` efface la provenance.
7. POST avec `procedureSuivie === AUCUNE_DEMANDE` → bandeau rouge NUL + indemnité affichée si salaire fourni.
8. POST avec `AUTORISATION_OBTENUE` → bandeau navy VALIDE.
9. POST avec `EN_COURS_INSTRUCTION` → bandeau or CONTESTABLE.
10. `CaseDashboardRefreshService.triggerRefresh()` invoqué après POST succès.
11. `MatSnackBar` rouge sur erreur 400 / 5xx (pas d'alert/confirm natif).
12. Validation F-IA-03 active : `coherenceAlerts().MOTIF_LICENCIEMENT` non-null si `aiData.motifLicenciement` diverge de la saisie avocat. Source `MULTI` quand convergence IA + F96.
13. `coherenceAlerts` vide en mode résultat (`showForm = false`).
14. `baseJuridique` et `formule` rendus en JetBrains Mono. Le reste en Inter.
15. Indemnité forfaitaire `indemniteForfaitaireMinEur` rendue en JetBrains Mono dans le bandeau NUL.

## Plan de test

### Jest unit tests (≥ 13 — `protection-rp-section.component.spec.ts`)

1. `FRANCE → isFrance() true, GET appelé au ngOnInit`.
2. `BELGIQUE → bannière info, aucun appel HTTP`.
3. `GET 200 hydrate le résultat (mode résultat)`.
4. `GET 404 reste en mode formulaire`.
5. `pré-fill IA : motifLicenciement ← aiData.motifLicenciement` (cas exact `FAUTE_GRAVE`).
6. `pré-fill IA : motifLicenciement normalisation` (`'inaptitude'` → `INAPTITUDE`).
7. `pré-fill sans aiData → aucun pré-remplissage, aucun badge`.
8. `onMotifChange efface le badge IA`.
9. `formValid false initialement, true seulement quand tous les champs requis sont présents`.
10. `calculate() POST + résultat + snackbar succès + triggerRefresh`.
11. `calculate() ignoré si form invalide`.
12. `calculate() erreur backend → snackbar rouge`.
13. `coherenceAlerts.MOTIF_LICENCIEMENT présent si IA diverge de saisie`.
14. `coherenceAlerts.MOTIF_LICENCIEMENT absent si IA convergent`.
15. `coherenceAlerts vides après calcul (showForm=false)`.
16. `coherenceAlerts multi-sources F96 + IA convergents → MULTI`.
17. `bannerClass mappe verdict → classe CSS attendue`.
18. `editMode ré-affiche le form`.

### Régression

- `decisional-tools-panel.component.spec.ts` : doit toujours passer (entrée TOOL_REGISTRY ajoutée). Lancer `npx jest decisional-tools-panel`.

### Self-check grep pré-commit (5 patterns canoniques)

```
grep -rn "alert(\|confirm(" frontend/src/app/case-files/protection-rp-section/  # → 0
grep -rn "MatDatepicker" frontend/src/app/case-files/protection-rp-section/      # → 0
grep -rn "auto_awesome" frontend/src/app/case-files/protection-rp-section/       # → ≥1
grep -rn "CoherenceAlertBuilder" frontend/src/app/case-files/protection-rp-section/ # → ≥1
grep -rn "triggerRefresh()" frontend/src/app/case-files/protection-rp-section/   # → ≥1
```

## Tables / endpoints / composants impactés

### Endpoints consommés (figés par SF-DT-30-01 PR #631)

- `POST /api/v1/case-files/{caseFileId}/protection-rp-analysis` — calcul + upsert
- `GET /api/v1/case-files/{caseFileId}/protection-rp-analysis` — retour persisté

### Composants nouveaux (frontend)

- `frontend/src/app/case-files/protection-rp-section/protection-rp-section.component.ts/html/scss/spec.ts`
- `frontend/src/app/core/models/protection-rp.model.ts`
- `frontend/src/app/core/services/protection-rp.service.ts`

### Composants modifiés

- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` — ajout entrée TOOL_REGISTRY `'F-DT-30-protection-rp'` (tool_id aligné migration 166).

### Pas de modification backend.

## Contrat API (importé de SF-DT-30-01)

### POST `/api/v1/case-files/{caseFileId}/protection-rp-analysis`

Request body :
```json
{
  "statutProtege": "MEMBRE_CSE_TITULAIRE",
  "dateExpirationMandat": "2026-09-30",
  "datePresumeeRupture": "2026-04-15",
  "procedureSuivie": "AUTORISATION_OBTENUE",
  "motifLicenciement": "FAUTE_GRAVE",
  "salaireMensuelBrutEur": 3500
}
```

Enums :
- `statutProtege` : `MEMBRE_CSE_TITULAIRE`, `MEMBRE_CSE_SUPPLEANT`, `DELEGUE_SYNDICAL`, `REPRESENTANT_SECTION_SYNDICALE`, `CONSEILLER_PRUDHOMMES`, `CONSEILLER_SALARIE`, `DEFENSEUR_SYNDICAL`, `MEDECIN_TRAVAIL`, `MEMBRE_CHSCT_HISTORIQUE`
- `procedureSuivie` : `AUTORISATION_OBTENUE`, `AUTORISATION_REFUSEE`, `EN_COURS_INSTRUCTION`, `AUCUNE_DEMANDE`
- `motifLicenciement` : `FAUTE_GRAVE`, `INSUFFISANCE_PRO`, `ECONOMIQUE`, `INAPTITUDE`, `AUTRE`

Response 200 :
```json
{
  "caseFileId": "uuid",
  "salarieEncoreProtege": true,
  "scoreConformite": 100,
  "verdictLegalite": "VALIDE",
  "criteresRemplis": ["STATUT_PROTEGE_VALIDE", "PROCEDURE_AUTORISATION_DEMANDEE", "AUTORISATION_OBTENUE", "LICENCIEMENT_HORS_PROCEDURE_EN_COURS"],
  "criteresManquants": [],
  "indemniteForfaitaireMinEur": 0.0,
  "salaireEvictionPotentielEur": 0.0,
  "delaiContestationJours": 60,
  "baseJuridique": "Art. L.2411-1 + L.2411-3 + L.2411-22 + L.2422-1 + R.2422-1 Code du travail",
  "formule": "Salarié protégé (MEMBRE_CSE_TITULAIRE) + procédure AUTORISATION_OBTENUE + motif FAUTE_GRAVE → 4 critères remplis / 0 manquants = score 100 → verdict VALIDE",
  "messages": ["..."],
  "country": "FRANCE"
}
```

## Ce qui est hors périmètre

- Backend : déjà mergé (PR #631).
- Belgique : feature jumelle au backlog.
- Génération de la requête à l'inspection du travail : SF future (générateur niveau 2).
- Calcul détaillé de l'indemnité de préavis : couvert par F-DT-25.
- Workflow inspection du travail dynamique : pas de mise à jour temps réel.

## Analyse de cohérence transversale

| Cible | Statut | Justification |
|-------|--------|---------------|
| Autres outils décisionnels FR-only | Intégrée — pattern miroir `pse-section` (SF-DT-14-02) | Composant standalone + service HttpClient + entrée TOOL_REGISTRY symétrique. |
| BE | Backlog — feature jumelle | Statut protégé belge (loi 19 mars 1991). À ouvrir au backlog. |
| Immigration FR/BE | Non applicable | Domaine différent. |
| Famille FR/BE | Non applicable | Domaine différent. |
| F-IA-04 visibility | Intégrée | Règle ALWAYS_ON DROIT_DU_TRAVAIL FRANCE priority 58 ajoutée par migration 166. |
| F-IA-03 alertes de cohérence | Intégrée | `MOTIF_LICENCIEMENT` field via `CoherenceAlertBuilder` partagé. |
| Pré-fill IA | Intégrée | `motifLicenciement` ← `aiData.motifLicenciement` mappé. |

## Nouveau pattern UI ou service partagé

Aucun. Le composant réutilise les patterns existants :
- `CoherenceAlertBuilder` (SF-155-05) — helper partagé pour les alertes F-IA-03.
- `CoherencePopoverTriggerDirective` — directive partagée pour les popovers.
- `LegalCitationsPipe` — pipe partagé pour rendre les articles de loi en JetBrains Mono.
- `CaseDashboardRefreshService` — service partagé pour rafraîchir le dashboard.
- `MatSnackBar` — pattern d'erreur canonique.

## Impact par domaine métier

- **Droit du travail** : feature **principale** — outil dédié à la protection des représentants du personnel FR.
- **Droit immigration** : non applicable.
- **Droit famille** : non applicable.
- **Pays** : FRANCE uniquement. Belgique = feature jumelle au backlog.

## Parité des domaines métier (niveau 5 — scoring)

L'outil est un **scoring de validité procédurale** (niveau 5 sur l'échelle des 7 niveaux).

| Domaine | Équivalent existe ? | Statut |
|---------|---------------------|--------|
| Droit du travail FR | OUI (cette SF + backend SF-DT-30-01) | En cours de finalisation |
| Droit du travail BE | NON (statut protégé belge — loi 19 mars 1991) | À ouvrir au backlog |
| Immigration | NON pertinent | Concept inapplicable |
| Famille | NON pertinent | Concept inapplicable |

Action requise : confirmer l'ouverture au backlog d'une feature jumelle BE (déjà signalée dans SF-DT-30-01).

## Préoccupations transversales

- **Auth / Principal** : non — réutilise le pattern existant.
- **Workspace context** : non — gate `workspaceCountry` Input string standard.
- **Plans / limites** : non — pas de quota.
- **Navigation / routing** : non — section dans le panel existant.
- **Outil décisionnel métier** : OUI — nouveau scoring niveau 5 ajouté. Scan effectué :
  - `pse-section` (F-DT-14) — distinct (collectif PSE).
  - `licenciement-section` (F-DT-08) — distinct (validité générale).
  - `licenciement-economique-section` (F-DT-13) — distinct (motif économique).
  - `inaptitude-section` (F-DT-15) — distinct (motif inaptitude).
  - `licenciement-nul-detection-section` (F-DT-16) — distinct (causes générales de nullité).
  - `harcelement-licenciement-nul-section` (F-DT-11) — distinct (harcèlement).
  - Protection RP = situation **statutaire** (procédure spéciale autorisation IT). **Invariant respecté** : un outil = une situation métier.
- **Smoke tests E2E** : non concernés (pas de changement d'auth/routing).
