# Mini-spec — F-FA-24 / SF-FA-24-06 Frontend donation entre vifs

## Identifiant

`F-FA-24 / SF-FA-24-06`

## Feature parente

`F-FA-24` — Droit des successions. Cette SF est le frontend de l'outil "Validité d'une donation entre vifs" — backend figé dans SF-FA-24-05 (PR #671 mergée).

## Statut

`in-progress`

## Date de création

2026-04-26

## Branche Git

`feat/SF-FA-24-06-frontend-donation`

---

## Objectif

Frontend Angular de l'outil décisionnel **"Validité d'une donation entre vifs"** — section consommant l'API SF-FA-24-05 (POST + GET `/api/v1/case-files/{id}/donation-analysis`). FR uniquement (gate `workspaceCountry`), bannière info Belgique (feature jumelle backlog F-FA-24-BE-donation), pré-fill IA + validation F-IA-03 obligatoires, intégrée au panel F-IA-04 via TOOL_REGISTRY.

---

## Contrat API (importé de SF-FA-24-05 backend, mergé PR #671)

### POST `/api/v1/case-files/{caseFileId}/donation-analysis`

**Body**
```json
{
  "formeDonation": "DONATION_NOTARIEE",
  "dateDonation": "2024-03-15",
  "ageDonateurAns": 65,
  "saineDEsprit": true,
  "capaciteDonateur": true,
  "capaciteRecipiendaire": true,
  "consentementLibre": true,
  "objetDetermine": true,
  "respectFormalisme": true,
  "respectQuotiteDisponible": true,
  "acteAuthentique": true,
  "acceptationExpresse": true,
  "remiseEffective": null,
  "bienMeuble": null,
  "intentionLiberale": null,
  "actePrincipalNeutre": null,
  "apparenceOnerueuse": null,
  "prixIncoherent": null,
  "vicesConsentementDol": false,
  "erreurSubstantielle": false,
  "ingratitudeAvere": false,
  "inexecutionCharge": false
}
```

**Réponse 200**
```json
{
  "caseFileId": "...",
  "formeDonation": "DONATION_NOTARIEE",
  "verdictValidite": "VALIDE",
  "risquesRequalification": [],
  "actionEnReductionPossible": false,
  "revocationPossible": false,
  "delaiContestationAns": 5,
  "scoreEligibilite": 100,
  "baseJuridique": "Art. 893-958, 902-906, 920 et s., 931, 953-958 Cciv",
  "formule": "Forme DONATION_NOTARIEE + verdict VALIDE + 0 risque → score 100",
  "messages": ["..."],
  "country": "FRANCE"
}
```

**Codes enum**
- `formeDonation` : `DONATION_NOTARIEE` | `DONATION_MANUELLE` | `DON_INDIRECT` | `DONATION_DEGUISEE`
- `verdictValidite` : `VALIDE` | `CONTESTABLE` | `NUL`
- `risquesRequalification[*].code` : voir backend SF-FA-24-05 (17 codes).

---

## Comportement attendu

### Cas nominal

L'avocat ouvre le dossier (FR + DROIT_FAMILLE). La section "Validité donation" est affichée par le panel F-IA-04. À l'ouverture :
1. GET pour récupérer une analyse persistée → si 200, affichage hydraté.
2. Sinon (404) → mode formulaire avec **pré-fill IA** (signaux + provenance).
3. Avocat saisit forme + critères conditionnels selon la forme + capacité + consentement + quotité.
4. POST `/donation-analysis` → bandeau verdict + chips de risques + meta.

### Gate workspaceCountry
- Si `workspaceCountry !== 'FRANCE'` → bannière info bleue qui renvoie au backlog jumeau (pas de masquage silencieux). Aucun appel HTTP.

### Validation F-IA-03 (RÈGLE FONDAMENTALE)
3 champs surveillés :
- `FORME` — divergence forme de la donation détectée.
- `SAINE_ESPRIT` — divergence sur la capacité (art. 902).
- `RESPECT_QUOTITE` — divergence sur le respect de la quotité disponible (art. 913+).

Hiérarchie de sources : F-96 > QUESTION_IA > IA > PIECE_MANQUANTE. `MULTI` si convergence multiple.

### Cas d'erreur
- POST 400 / 404 → MatSnackBar rouge.
- Form invalide → bouton désactivé (pas d'appel).

---

## Critères d'acceptation

1. Composant `app-donation-section` standalone Angular, intégré au panel F-IA-04 via TOOL_REGISTRY (`'F-FA-24-donation'`).
2. Workspace BE → bannière info, aucun GET/POST.
3. Workspace FR → GET au ngOnInit ; 200 → mode résultat hydraté ; 404 → mode formulaire + prefill IA.
4. Pré-fill IA OBLIGATOIRE : `formeDonation`, `dateDonation`, `saineDEsprit`, `respectQuotiteDisponible` lus depuis `aiData` (FamilleExtractedData) → signaux + provenance IA + badge "auto_awesome Pré-rempli depuis l'analyse" + handler qui efface provenance au changement manuel.
5. Validation F-IA-03 OBLIGATOIRE : 3 champs câblés (`FORME`, `SAINE_ESPRIT`, `RESPECT_QUOTITE`) avec `CoherenceAlertBuilder` + `<app-coherence-popover-trigger>`.
6. Form : radio forme (4 options) + champs conditionnels selon forme + checkboxes vices/révocation/quotité + date + âge + capacité.
7. POST envoie le body strict du contrat ; succès → bandeau verdict + chips risques + flags ; erreur → snackbar.
8. Bandeau verdict : NUL=critical(rouge), CONTESTABLE=warn(or), VALIDE=info(navy).
9. Liste `risquesRequalification` rendue en chips (libellé humain par code).
10. Affiche **`actionEnReductionPossible`** comme flag-chip si excès quotité.
11. Affiche **`revocationPossible`** comme flag-chip si ingratitude/inexécution.
12. `CaseDashboardRefreshService.triggerRefresh()` après POST succès.
13. Tests Jest ≥ 12 passent (`donation-section.component.spec.ts`).

---

## Plan de test (≥ 12 specs)

1. FRANCE → isFrance true + GET au ngOnInit.
2. BELGIQUE → aucun appel HTTP (gate pays).
3. GET 200 → mode résultat.
4. GET 404 → mode form + prefill IA appelé.
5. Pré-fill IA forme depuis `formeDonationDetectee`.
6. Pré-fill IA dateDonation + saineDEsprit + respectQuotiteDisponible.
7. Pré-fill sans aiData → aucun pré-remplissage.
8. onFormeDonationChange efface provenance + reset champs autres formes.
9. formValid : NOTARIEE requiert acteAuthentique + acceptationExpresse + capacités.
10. formValid : MANUELLE requiert remiseEffective + bienMeuble.
11. calculate POST envoie le body attendu + succès snackbar.
12. calculate erreur → snackbar rouge.
13. coherenceAlerts.FORME présente si IA divergente.
14. coherenceAlerts.SAINE_ESPRIT multi-sources F96 + IA → MULTI.
15. coherenceAlerts vides après calcul.
16. ngOnChanges(aiData) post-saisie ne réécrase pas la saisie avocat.
17. verdictBannerClass / verdictChipClass / verdictIcon couvrent NUL/CONTESTABLE/VALIDE.
18. formeLabel + verdictLabel couvrent les enums.

---

## Tables / endpoints / composants impactés

### Composants Angular nouveaux
- `frontend/src/app/case-files/donation-section/donation-section.component.ts`
- `donation-section.component.html`
- `donation-section.component.scss`
- `donation-section.component.spec.ts`

### Modèles + service
- `frontend/src/app/core/models/donation.model.ts`
- `frontend/src/app/core/services/donation.service.ts`

### Modèle famille étendu
- `divorce-accepte.model.ts` — ajout 4 champs IA optionnels :
  - `formeDonationDetectee?: string | null`
  - `dateDonationDetectee?: string | null`
  - `saineDEspritDonateurDetected?: boolean | null`
  - `respectQuotiteDisponibleDetected?: boolean | null`

### Panel F-IA-04
- `decisional-tools-panel.component.ts` — ajout import + entrée TOOL_REGISTRY `'F-FA-24-donation'`.

### Endpoints consommés
- `POST /api/v1/case-files/{id}/donation-analysis`
- `GET /api/v1/case-files/{id}/donation-analysis`

---

## Hors périmètre

- **Belgique** : F-FA-24-BE-donation (backlog).
- **Réserve héréditaire** (SF-FA-24-07) — backend mergé, frontend dans SF-FA-24-08 future.
- **Donation entre époux art. 1096** — feature distincte.
- **Donation-partage art. 1075-1078** — feature distincte.
- Calcul fiscal — hors périmètre.

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Pattern de référence** : `testament-validite-section` (PR #665, F-FA-24 jumeau). Tous les patterns (palette, gate FR, prefill, F-IA-03, snackbar, dashboard refresh, JetBrains Mono, Inter) sont copiés tels quels.
- [x] **Autres outils décisionnels famille FR** : déjà séparés. Donation = situation distincte (entre vifs, gratuit, par opposition au testament unilatéral à effet post-mortem).
- [x] **Belgique** : backlog jumeau F-FA-24-BE-donation (mention dans la bannière info BE).
- [x] **Autres domaines** : DROIT_DU_TRAVAIL / DROIT_IMMIGRATION → non applicable.
- [x] **UI patterns / nouveau pattern** : aucun nouveau pattern transversal — réutilise `CoherenceAlertBuilder`, `CoherencePopoverTriggerDirective`, `LegalCitationsPipe`, palette navy/or DESIGN_SYSTEM.md.

### Verdict

Pattern aligné F-FA-24 SF-04 testament. Aucune duplication créée — outil isolé, single-country, single-domain.

---

## Impact par domaine métier

- **Sensibilité au domaine** : forte — feature 100% droit famille FR (donation entre vifs). Aucun impact DROIT_DU_TRAVAIL ou DROIT_IMMIGRATION.
- **Sensibilité au pays** : forte — règles propres au Code civil français. Belgique = backlog jumeau F-FA-24-BE-donation.

---

## Préoccupations transversales

- [x] **Outil décisionnel métier** : nouvelle section frontend dédiée. Scan effectué : aucun composant existant ne couvre la donation. Pattern jumeau testament copié tel quel.
- [x] **Auth / Principal** : aucun changement — pattern hérité de la section testament.
- [x] **Workspace context** : aucun changement — gate `workspaceCountry === 'FRANCE'` standard.
- [x] **Plans / limites** : aucun gate.
- [x] **Navigation / routing** : aucun changement.

Aucune préoccupation critique modifiée — pas besoin de smoke tests E2E.

---

## Self-check pré-commit (5/5)

- [x] Pré-fill IA implémenté avec provenance + handler reset (4 champs : forme, date, saine esprit, quotité).
- [x] Validation F-IA-03 implémentée avec `CoherenceAlertBuilder` (3 fields).
- [x] Gate `workspaceCountry === 'FRANCE'` + bannière info BE (pas de masquage silencieux).
- [x] `CaseDashboardRefreshService.triggerRefresh()` après POST succès.
- [x] Palette navy/or — rouge réservé verdict NUL ; JetBrains Mono pour `baseJuridique` + `formule`.
