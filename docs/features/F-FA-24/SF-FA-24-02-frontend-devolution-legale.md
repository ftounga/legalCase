# Mini-spec — F-FA-24 / SF-FA-24-02 Frontend dévolution légale successorale

## Identifiant

`F-FA-24 / SF-FA-24-02`

## Feature parente

`F-FA-24` — Droit des successions (chantier ~8-10 SF — futures : testament 967+, donation 893+, réserve 913+, action en réduction, partage successoral, indivision successorale, rapport à succession)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-FA-24-02-frontend-devolution-legale`

## Contrat API

Importé de `SF-FA-24-01-backend-devolution-legale.md` (PR #651, mergée).

- `POST /api/v1/case-files/{caseFileId}/devolution-legale-analysis` — body `DevolutionLegaleRequest`, réponse `DevolutionLegaleResponse`.
- `GET /api/v1/case-files/{caseFileId}/devolution-legale-analysis` — réponse `DevolutionLegaleResponse`.

Enums identiques au backend :
- `OrdreActif` : `DESCENDANTS` | `PRIVILEGIES` | `ASCENDANTS_ORDINAIRES` | `COLLATERAUX_ORDINAIRES` | `CONJOINT_SEUL` | `DESHERENCE`
- `OptionConjoint` : `USUFRUIT` | `QUART`
- `QualiteHeritier` : `CONJOINT` | `DESCENDANT` | `PERE` | `MERE` | `FRERE_SOEUR` | `ASCENDANT_ORDINAIRE_PATERNEL` | `ASCENDANT_ORDINAIRE_MATERNEL` | `COLLATERAL_ORDINAIRE` | `REPRESENTANT`
- `ModaliteHeritier` : `PLEINE_PROPRIETE` | `USUFRUIT` | `NUE_PROPRIETE`

---

## Objectif

Composant Angular `<app-devolution-legale-section>` — formulaire de composition familiale + tableau des héritiers désignés + indicateurs (ordre actif, représentation, fente) + chips risques contentieux. Intégré au panneau F-IA-04 via `TOOL_REGISTRY` `'F-FA-24-devolution-legale'`. FR uniquement (gate pays — bannière info pour BE).

---

## Comportement attendu

### Cas nominal

L'avocat saisit la composition familiale du défunt (conjoint survivant, descendants, parents, frères/sœurs, ascendants ordinaires, collatéraux ordinaires, option conjoint) → POST → affichage du **tableau des héritiers désignés** avec ordre + quote-part % + modalité + indicateur graphique de l'ordre actif (1, 2, 3 ou 4) + chips info `representationActive` / `fenteApplicable` + chips alerte `risquesContentieux` + base juridique + formule (JetBrains Mono).

GET au montage : si une analyse persiste → mode résultat ; sinon mode formulaire.

### Pré-fill IA + F-IA-03

Le composant lit `aiData: FamilleExtractedData` (étendu par la SF avec 4 nouveaux champs optionnels) :
- `conjointSurvivantDetected` → `conjointSurvivant`
- `nbDescendantsDetecte` → `nbDescendants`
- `tousDescendantsCommunsAvecConjointDetected` → `tousDescendantsCommunsAvecConjoint`
- `nbFreresSoeursDetecte` → `nbFreresSoeurs`

Provenance signal par champ + badge `auto_awesome` "Pré-rempli depuis l'analyse" + handler manuel qui efface le badge.

`coherenceAlerts` (computed) construit des alertes via `CoherenceAlertBuilder` pour 2 fields critiques :
- `CONJOINT` — divergence si IA dit conjoint vivant et avocat dit non (ou inversement). Sources : F96 (`DEVOLUTION_LEGALE_CONJOINT`), QUESTION_IA, IA, PIECE_MANQUANTE.
- `DESCENDANTS_COMMUNS` — divergence sur le caractère commun des descendants (impacte option ¼ vs option usufruit). Sources : F96, QUESTION_IA, IA, PIECE_MANQUANTE.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `workspaceCountry !== 'FRANCE'` | Bannière info "Outil français uniquement" + aucun appel HTTP |
| GET 404 | Mode formulaire (pas d'analyse persistée) |
| GET 200 | Mode résultat hydraté |
| POST avec form invalide | Pas d'appel HTTP (formValid retourne false) |
| POST 400/500 | snackbar rouge avec message backend |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Pattern de référence** : `partage-judiciaire-section` (PR #638, SF-FA-17-02). Pattern jumeau quasi-identique : tool décisionnel familial FR uniquement, scoring niveau 5, visibility `ALWAYS_ON`, gate country, pré-fill IA + F-IA-03.
- [x] **Helper partagé** : `CoherenceAlertBuilder` (SF-155-05) réutilisé tel quel.
- [x] **TOOL_REGISTRY** : entrée symétrique avec `aiData/procedureChecks/aiQuestions/piecesManquantes` passés au composant.
- [x] **Autres pays** : Belgique (CC BE art. 731+) → backlog jumeau F-FA-24-BE — bannière info renvoie vers cette feature.
- [x] **Autres domaines** : DROIT_DU_TRAVAIL / DROIT_IMMIGRATION → non applicable (succession = strictement DROIT_FAMILLE).

### Verdict

Aucune duplication créée — composant isolé, single-country, single-domain, réutilise `CoherenceAlertBuilder`. Pattern frontend-coherence-audit complet.

---

## Impact par domaine métier

- **Sensibilité au domaine** : forte — composant 100% droit famille FR. Aucun impact DROIT_DU_TRAVAIL ou DROIT_IMMIGRATION.
- **Sensibilité au pays** : forte — gate `workspaceCountry === 'FRANCE'` ; bannière info pour BE qui renvoie vers la feature jumelle backlog.

---

## Parité des domaines métier (outil de niveau 5 — scoring)

L'outil affiche un **scoring** (heritiers + ordre actif + score). Application des règles de parité héritée de SF-FA-24-01 :

| Domaine | Équivalent | Décision |
|---------|------------|----------|
| DROIT_DU_TRAVAIL | N/A | Non applicable, justifié |
| DROIT_IMMIGRATION | N/A | Non applicable, justifié |
| DROIT_FAMILLE FRANCE | **Cette SF** | En cours |
| DROIT_FAMILLE BELGIQUE | CC BE art. 731+ | Backlog jumeau F-FA-24-BE |

---

## Critères d'acceptation

1. Composant `app-devolution-legale-section` standalone (Angular 19) créé sous `frontend/src/app/case-files/devolution-legale-section/`.
2. 4 fichiers : `.ts` / `.html` / `.scss` / `.spec.ts`.
3. Modèle `frontend/src/app/core/models/devolution-legale.model.ts` (Request/Response/Enums).
4. Service `frontend/src/app/core/services/devolution-legale.service.ts` (HttpClient POST + GET).
5. Entrée `'F-FA-24-devolution-legale'` ajoutée au `TOOL_REGISTRY` du panneau F-IA-04.
6. Form composition famille : conjointSurvivant (toggle radio), descendants (count + tousCommunsAvecConjoint), pereVivant/mereVivant (toggles), freresSoeurs (count + prédécédés), ascendantsOrdinaires (toggle), collateralOrdinaires (toggle), optionConjoint (radio QUART/USUFRUIT — visible seulement si conjoint + descendants tous communs).
7. Affichage du résultat : tableau `heritiersDesignes` avec **ordre + quote-part % + modalité** (PLEINE_PROPRIETE / USUFRUIT / NUE_PROPRIETE).
8. Indicateur graphique de l'ordre actif via chip stylisé (1 / 2 / 3 / 4 / Conjoint seul / Déshérence).
9. Chips info `representationActive` (badge or) + `fenteApplicable` (badge or).
10. Liste `risquesContentieux` en chips alerte (badge or).
11. `baseJuridique` + `formule` rendus en JetBrains Mono.
12. Pré-fill IA fonctionnel pour les 4 champs détectés (conjoint, nbDescendants, tousCommuns, nbFreresSoeurs) avec badge `auto_awesome` + handlers reset.
13. `coherenceAlerts` computed avec 2 fields (CONJOINT, DESCENDANTS_COMMUNS) via `CoherenceAlertBuilder`.
14. Bannière "Outil français uniquement" si `workspaceCountry !== 'FRANCE'`.
15. `CaseDashboardRefreshService.triggerRefresh()` appelé après POST réussi.
16. `MatSnackBar` pour les erreurs backend (panel rouge).
17. Tests Jest ≥ 15.

---

## Plan de test

### Tests unitaires Jest (`devolution-legale-section.component.spec.ts`) — ≥ 15

1. FRANCE → isFrance() true, GET appelé au ngOnInit.
2. BELGIQUE → isFrance() false, aucun appel HTTP (gate pays).
3. Mode résultat hydraté si GET 200.
4. Mode formulaire si GET 404.
5. Pré-fill IA : conjointSurvivant ← `aiData.conjointSurvivantDetected` + provenance IA.
6. Pré-fill IA : nbDescendants + tousCommuns + nbFreresSoeurs.
7. onConjointSurvivantChange efface le badge IA.
8. formValid initial false ; true quand tous les champs requis sont présents.
9. formValid : option conjoint requise si conjointSurvivant + descendants + tousCommuns.
10. POST : envoie le body attendu, met à jour `result()`, snackbar succès, triggerRefresh.
11. POST : ignoré si form invalide.
12. POST : erreur backend → snackbar rouge avec message.
13. coherenceAlerts.CONJOINT présent si IA divergente.
14. coherenceAlerts.DESCENDANTS_COMMUNS indépendant de CONJOINT.
15. ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide.
16. ngOnChanges(aiData) post-saisie ne réécrase pas la saisie avocat.
17. toggleCollapse, editMode, ordreLabel pour les 6 valeurs de OrdreActif, modaliteLabel pour les 3 modalités.

### Isolation workspace

Pas de logique workspace côté frontend — c'est le backend qui filtre sur le workspace. Test côté frontend : vérifie que `caseFileId` est bien transmis dans l'URL (déjà couvert par le test POST).

---

## Tables / endpoints / composants impactés

### Endpoints consommés
- `POST /api/v1/case-files/{caseFileId}/devolution-legale-analysis`
- `GET /api/v1/case-files/{caseFileId}/devolution-legale-analysis`

### Composants Angular nouveaux
- `frontend/src/app/case-files/devolution-legale-section/devolution-legale-section.component.{ts,html,scss,spec.ts}`
- `frontend/src/app/core/models/devolution-legale.model.ts`
- `frontend/src/app/core/services/devolution-legale.service.ts`

### Composants Angular modifiés
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` — ajout entrée `'F-FA-24-devolution-legale'` dans `TOOL_REGISTRY`.
- `frontend/src/app/core/models/divorce-accepte.model.ts` — ajout 4 champs optionnels dans `FamilleExtractedData` (conjointSurvivantDetected, nbDescendantsDetecte, tousDescendantsCommunsAvecConjointDetected, nbFreresSoeursDetecte) — purement additif, no-op gracieux.

---

## Hors périmètre

- **Backend** : SF-FA-24-01 (mergée PR #651).
- **Belgique** : F-FA-24-BE backlog (CC BE art. 731+ avec quotités différentes).
- **Autres SF de F-FA-24** : testament, donation, réduction, partage, indivision, rapport à succession.
- **Détail valorisation usufruit** : la SF-01 indique "USUFRUIT" sans valoriser (pas de table 669 CGI côté UI).
- **Représentation profonde** : on s'appuie uniquement sur les inputs `nbDescendantsPredecedes` + `nbPetitsEnfantsParRepresentation` exposés par le backend (1 niveau seulement — backlog si besoin de plus).

---

## Préoccupations transversales

- [x] **Outil décisionnel métier** : nouvel outil dédié dévolution successorale FR. Scan effectué via mini-spec SF-FA-24-01 — aucun outil existant ne le couvre, et le pattern "1 outil = 1 situation métier" est respecté.
- [x] **Auth / Principal** : le composant ne fait pas d'auth — délégation HttpClient + intercepteurs existants. Aucun changement.
- [x] **Workspace context** : le `workspaceCountry` est passé en `@Input` par le panel parent. Aucun changement de résolution workspace.
- [x] **Plans / limites** : non touché.
- [x] **Navigation / routing** : non touché — composant intégré au panneau F-IA-04 existant, pas de nouvelle route.

Aucune préoccupation critique modifiée — pas besoin de smoke tests E2E.

---

## Self-check pré-commit (5 items obligatoires audit cohérence frontend)

1. Pattern canonique référencé (`partage-judiciaire-section` PR #638) — OUI.
2. Pré-fill IA implémenté (4 champs avec provenance + badge + handlers) — OUI.
3. Validation F-IA-03 implémentée (2 fields via `CoherenceAlertBuilder`) — OUI.
4. Gate pays via bannière info (pas de masquage silencieux) — OUI.
5. `CaseDashboardRefreshService.triggerRefresh()` après POST succès + `MatSnackBar` pour erreurs + JetBrains Mono pour `baseJuridique` et `formule` — OUI.
