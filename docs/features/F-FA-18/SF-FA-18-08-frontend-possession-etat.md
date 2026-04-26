# SF-FA-18-08 — Frontend possession d'état (art. 311-1 + 311-2 + 317 Cciv)

## Objectif

Exposer côté Angular l'outil décisionnel "possession d'état" (FR uniquement)
livré par le backend SF-FA-18-07 (mergé PR #670). L'outil évalue la
recevabilité d'une possession d'état comme mode de preuve / d'établissement
de la filiation et oriente l'avocat vers le dispositif applicable
(constat par notaire art. 317 vs preuve en justice art. 311-1 + 311-2).

## Contrat API consommé (importé de SF-FA-18-07-backend)

- `POST /api/v1/case-files/{caseFileId}/possession-etat-analysis`
- `GET  /api/v1/case-files/{caseFileId}/possession-etat-analysis`

Body `PossessionEtatRequest` :

```json
{
  "dateDebutPossession": "2018-04-15",
  "dateFinPossession": "2024-04-15",
  "tractatus": true,
  "fama": true,
  "nomen": false,
  "continueCondition": true,
  "paisible": true,
  "nonEquivoque": true
}
```

Response `PossessionEtatResponse` :

```json
{
  "caseFileId": "uuid",
  "verdictRecevabilite": "ELEVEE | MOYENNE | FAIBLE",
  "dispositifApplicable": "CONSTAT_NOTAIRE | PREUVE_JUSTICE | AUCUN",
  "scoreRecevabilite": 95,
  "dureePossessionAnnees": 6,
  "delaiContestationActeAns": 5,
  "delaiContestationCessationAns": 10,
  "criteresRemplis": ["..."],
  "criteresManquants": ["..."],
  "baseJuridique": "Art. 311-1 + 311-2 + 317 Cciv",
  "formule": "Début=... → verdict ELEVEE → dispositif CONSTAT_NOTAIRE",
  "messages": ["..."],
  "country": "FRANCE"
}
```

Codes d'erreur attendus côté frontend : `400` (validation, BE non supporté,
domaine ≠ DROIT_FAMILLE), `404` (GET sans POST préalable, dossier d'un autre
workspace).

## Comportement nominal

- Bloc collapsible avec entête icône `family_restroom`, titre
  `POSSESSION D'ÉTAT (FR)`, chip verdict si analyse persistée.
- Dossier FR : `GET` au `ngOnInit`, hydrate l'écran si 200, sinon mode
  formulaire.
- Dossier BE : bannière info "Outil français uniquement" — feature jumelle
  CC art. 331-1 au backlog.
- Mode formulaire : 2 dates (début/fin de la possession) + 6 booléens
  (tractatus / fama / nomen / continue / paisible / nonEquivoque).
- Compteur live `dureePossessionAnnees` calculé côté UI à partir des dates
  saisies (cohérent avec le calcul backend `ChronoUnit.MONTHS / 12`).
- Soumission → snackbar succès, écran résultat (verdict, dispositif, score,
  durée, délais, critères remplis/manquants, messages avocat, formule, base
  juridique). Chip `delaiContestationAns` selon dispositif.
- Bouton "Modifier" pour repasser en formulaire.
- `CaseDashboardRefreshService.triggerRefresh()` après succès POST.
- Erreur backend → snackbar rouge avec message du serveur.

## Pré-fill IA + validation F-IA-03

OBLIGATOIRE (RÈGLE FONDAMENTALE coherence-audit). `aiData?: FamilleExtractedData`
expose le champ `possessionEtatConforme5AnsDetected?: boolean | null` (déjà
défini pour SF-FA-18-04 contestation). Pré-fill gracieux :

- `possessionEtatConforme5AnsDetected = true` → pré-coche les 5 critères
  cardinaux + tractatus/fama (faisceau "conforme") et signal `provenance`.
- Sources F-IA-03 :
  - `coherenceAlerts.POSSESSION_ETAT_CONFORME` : alerte multi-sources
    (IA + F96 + QUESTION_IA + PIECE_MANQUANTE) si l'avocat décoche un
    critère cardinal alors que l'IA détectait une possession conforme.

Pas d'extraction IA dédiée pour les booléens granulaires dans cette SF — le
fallback est gracieux (l'IA pré-coche en faisceau). Le pipeline IA pourra
être étendu en SF ultérieure si besoin.

## Critères d'acceptation

- `[ ]` Composant standalone Angular `app-possession-etat-section`.
- `[ ]` Imports `MatRadioModule` (dispositif preview), `MatCheckboxModule`
  (6 critères), `MatProgressSpinnerModule`, `LegalCitationsPipe`,
  `CoherencePopoverTriggerDirective`.
- `[ ]` Inputs : `caseFileId`, `workspaceCountry`, `aiData`,
  `procedureChecks`, `aiQuestions`, `piecesManquantes`.
- `[ ]` Bannière "Outil français uniquement" si `workspaceCountry !== FRANCE`.
- `[ ]` GET au `ngOnInit` (FR uniquement).
- `[ ]` Form valide ssi 2 dates renseignées et `dateFin >= dateDebut`.
- `[ ]` `dureePossessionAnnees` calculé côté UI (signal `computed`) live.
- `[ ]` Pré-fill IA (FAIL si absent — règle FONDAMENTALE).
- `[ ]` Validation F-IA-03 (FAIL si absent — règle FONDAMENTALE).
- `[ ]` Badge IA + handlers reset provenance au changement manuel.
- `[ ]` `CaseDashboardRefreshService.triggerRefresh()` après POST succès.
- `[ ]` Snackbar erreur sur failure backend.
- `[ ]` Self-check grep ≥ 5/5 (pré-fill, F-IA-03, country gate, dashboardRefresh, snackbar).

## Plan de test (Jest ≥ 12)

1. `FRANCE → isFrance() true, GET appelé au ngOnInit`.
2. `BELGIQUE → isFrance() false, aucun appel HTTP`.
3. `charge le résultat existant si GET 200`.
4. `reste en mode formulaire si GET 404`.
5. `pré-fill IA depuis possessionEtatConforme5AnsDetected=true`.
6. `pré-fill sans aiData → aucun pré-remplissage`.
7. `onTractatusChange efface le badge IA (provenance null)`.
8. `formValid false initialement`.
9. `formValid true ssi dateDebut + dateFin valides`.
10. `formValid false si dateFin < dateDebut`.
11. `dureePossessionAnnees calculé (signal computed)`.
12. `calculate() POST + résultat + snackbar + dashboardRefresh`.
13. `calculate() ignoré si form invalide`.
14. `calculate() erreur backend → snackbar rouge`.
15. `coherenceAlerts.POSSESSION_ETAT_CONFORME présent si IA diverge`.
16. `coherenceAlerts vides après calcul (showForm=false)`.
17. `toggleCollapse fonctionne`.
18. `editMode ré-affiche le form`.
19. `bannerClass mappe verdict → classe CSS`.
20. `dispositifLabel renvoie le libellé humain`.

## Tables / endpoints / composants

- Aucune migration / table — réuse 100% backend SF-FA-18-07.
- Endpoints backend déjà figés :
  - `POST /api/v1/case-files/{id}/possession-etat-analysis`
  - `GET  /api/v1/case-files/{id}/possession-etat-analysis`
- Nouveau composant Angular `frontend/src/app/case-files/possession-etat-section/`
  (4 fichiers : .ts / .html / .scss / .spec.ts).
- Nouveau modèle TypeScript `frontend/src/app/core/models/possession-etat.model.ts`.
- Nouveau service `frontend/src/app/core/services/possession-etat.service.ts`.
- Entrée TOOL_REGISTRY `'F-FA-18-possession-etat'` dans
  `decisional-tools-panel.component.ts` — symétrique aux 7 autres outils
  F-FA-18 (mêmes inputs : caseFileId, workspaceCountry, aiData,
  procedureChecks, aiQuestions, piecesManquantes).

## Hors périmètre

- Belgique (régime CC art. 331-1 — feature jumelle au backlog).
- Génération de l'acte de notoriété (relève d'une SF dédiée plus tard).
- Extension du pipeline IA pour extraire les 6 booléens granulaires (le
  fallback `possessionEtatConforme5AnsDetected` est suffisant pour la SF).

## Analyse de cohérence transversale

- **Pattern UI** : aligné `recherche-paternite-section` (PR #669 — F-FA-18-06)
  jumeau direct (palette navy/or/rouge, picker `<input type="date">`,
  CoherenceAlertBuilder, gate FR + bannière info BE).
- **Composant partagé** : pas de nouveau partagé introduit.
- **Autres outils F-FA-18** : 7 SF déjà livrées (4 backends + 3 frontends).
  Cette SF ferme le frontend pour la 4ᵉ situation métier. Pas d'overlap.
- **Pipeline IA** : pas d'extension de prompt nécessaire — fallback gracieux
  via `possessionEtatConforme5AnsDetected` déjà présent dans
  `FamilleExtractedData`.

## Impact par domaine métier

Sensible au domaine **droit de la famille** uniquement (filiation = droit de
la personne). Non applicable au droit du travail ou à l'immigration. Pour la
Belgique, régime CC art. 331-1 distinct → feature jumelle au backlog
(invariant "un outil = une situation métier").

## Parité des domaines métier

Outil de niveau 5 (scoring) :
- **Famille FR** : livré dans cette SF (frontend), backend SF-FA-18-07.
- **Famille BE** : feature jumelle au backlog (CC art. 331-1).
- **Travail / Immigration** : non pertinent (concept propre à la filiation).

## Référence

Backend : `SF-FA-18-07-backend-possession-etat.md` (PR #670 mergée).
Pattern frontend : `recherche-paternite-section` (PR #669 — F-FA-18-06).
