# Mini-spec — F-FA-24 / SF-FA-24-04 Frontend validité testament

## Identifiant

`F-FA-24 / SF-FA-24-04`

## Feature parente

`F-FA-24` — Droit des successions (chantier ~9-11 SF). SF-01 + SF-02 (dévolution
légale) déjà mergées. SF-03 backend testament mergée PR #661.

## Statut

`in-progress`

## Date de création

2026-04-26

## Branche Git

`feat/SF-FA-24-04-frontend-testament`

---

## Objectif

4ème SF de F-FA-24 — composant Angular décisionnel "Validité testament" qui
consomme l'API SF-FA-24-03 (PR #661 mergée) : formulaire à 4 formes
(OLOGRAPHE / AUTHENTIQUE / MYSTIQUE / INTERNATIONAL) avec champs conditionnels,
verdict tri-couleur (NUL=rouge, CONTESTABLE=or, VALIDE=navy), liste de vices,
chip d'action en réduction (art. 920+) et délai de contestation (5 ans, art. 1304).

---

## Comportement attendu

### Cas nominal

L'avocat ouvre la section "Validité testament" du panel décisionnel
(F-IA-04, tool_id `F-FA-24-testament-validite`).
- Le composant **se gate sur `workspaceCountry === 'FRANCE'`** ; en BE, une
  bannière info renvoie au backlog jumeau `F-FA-24-BE-testament` (pas de
  masquage silencieux).
- Au mount : GET `/api/v1/case-files/{id}/testament-validite-analysis` ;
  - 200 → mode résultat hydraté.
  - 404 → mode formulaire vierge ; pré-fill IA si `aiData` est fourni.
- L'avocat choisit la `formeTestament` (radio 4 valeurs) — les blocs de
  champs conditionnels apparaissent.
- POST → 200 ; le résultat est affiché (verdict + vices + actions complémentaires).

#### Champs conditionnels par forme

| Forme | Champs requis (booléens) |
|-------|-------------------------|
| `TESTAMENT_OLOGRAPHE` | `ecritureManuscritIntegrale`, `dateComplete`, `signatureTestateur` |
| `TESTAMENT_AUTHENTIQUE` | `presenceNotaireEtTemoinsConforme`, `dicteEnPresence`, `lectureFinaleAuTestateur`, `signaturesCompletes` |
| `TESTAMENT_MYSTIQUE` | `remiseSousPliCache`, `declarationDevant2Temoins`, `acteSuscriptionNotaire` |
| `TESTAMENT_INTERNATIONAL` | `respecteFormeWashington`, `signaturesCompletes` |

#### Champs communs

- `dateRedaction` (`<input type="date">` — convention canonique).
- `ageTestateurAnsRedaction` (number).
- `saineDEsprit` (radio Oui/Non, requis).
- `majeurProtegeAvecAssistance` (radio Oui/Non/N/A — null = non protégé).
- `vicesConsentementDol`, `erreurSubstantielle` (booléens, défaut false).
- `testamentPosterieurContradictoire`, `dechirureVolontaireOriginal`
  (booléens, défaut false).
- `legsExcedeQuotiteDisponible` (booléen, défaut false).

#### Affichage du résultat

- **Bandeau verdict tri-couleur** :
  - `NUL` → rouge (border-left + icône `gpp_bad`).
  - `CONTESTABLE` → or (icône `warning`).
  - `VALIDE` → navy (icône `verified`).
- **Liste `vicesIdentifies`** affichée en chips alerte (or) — un chip par vice.
- **Chip info `actionEnReductionPossible`** affiché si `true` (art. 920+,
  délai 5 ans à compter de l'ouverture, art. 921).
- **Chip info `delaiContestationAns = 5`** (art. 1304 Cciv) toujours affiché
  en mode résultat (sauf si NUL avec vice rédhibitoire absolu).
- **Score** affiché dans le chip header (Score X/100).
- **Formule** + **base juridique** affichées en JetBrains Mono (DESIGN_SYSTEM).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `workspaceCountry === 'BELGIQUE'` | Bannière info "Outil français uniquement" → renvoi backlog jumeau `F-FA-24-BE-testament`, pas d'appel HTTP |
| 404 GET au mount | Mode formulaire (no-op) |
| 400 POST (validation backend) | MatSnackBar erreur + reste sur le formulaire |
| 404 POST (workspace mismatch) | MatSnackBar erreur |
| Form invalide (champs requis manquants) | Bouton "Analyser" disabled |
| Forme non choisie | Champs conditionnels masqués |

---

## Pré-fill IA + validation F-IA-03 (RÈGLE FONDAMENTALE)

### Pré-fill IA (`@Input() aiData?: FamilleExtractedData | null`)

Champs pré-remplis depuis `aiData` (ajout de 4 nouveaux champs optionnels) :
- `formeTestament` ← `aiData.formeTestamentDetectee`
  (`'OLOGRAPHE'` / `'AUTHENTIQUE'` / `'MYSTIQUE'` / `'INTERNATIONAL'` —
  toute autre valeur ignorée).
- `dateRedaction` ← `aiData.dateRedactionTestamentDetectee` (ISO YYYY-MM-DD).
- `saineDEsprit` ← `aiData.saineDEspritTestateurDetected` (boolean).
- `legsExcedeQuotiteDisponible` ← `aiData.legsExcedeQuotiteDisponibleDetected`
  (boolean).

Chacun expose un `provenance<Field>` signal et un badge `auto_awesome`. Le
handler `onXxxChange()` remet le badge à `null` au changement manuel.

### Validation F-IA-03 (`coherenceAlerts` computed)

3 fields audités :
- `FORME` (forme du testament) — IA `aiData.formeTestamentDetectee` +
  F-96 critereCode `TESTAMENT_FORME` + QUESTION_IA + PIECE_MANQUANTE.
- `SAINE_ESPRIT` (capacité art. 901) — IA + F-96 + QUESTION_IA + PIECE_MANQUANTE.
- `LEGS_EXCEDE_QUOTITE` (action en réduction art. 920+) — IA + F-96 + QUESTION_IA.

Helper partagé `CoherenceAlertBuilder.forField<TestamentValiditeAlertField>()`
utilisé strictement (pas d'interface locale).

---

## Critères d'acceptation

1. Composant `<app-testament-validite-section>` standalone.
2. Mount → GET ; 200 → résultat hydraté ; 404 → formulaire vierge.
3. Gate FR strict : si `workspaceCountry === 'BELGIQUE'`, bannière info
   "Outil français uniquement", **aucun** appel HTTP.
4. Sélection forme `TESTAMENT_OLOGRAPHE` → 3 champs spécifiques affichés.
5. Sélection forme `TESTAMENT_AUTHENTIQUE` → 4 champs spécifiques.
6. Sélection forme `TESTAMENT_MYSTIQUE` → 3 champs spécifiques.
7. Sélection forme `TESTAMENT_INTERNATIONAL` → 2 champs spécifiques.
8. POST → corps conforme au contrat SF-FA-24-03.
9. Verdict NUL = bandeau rouge ; CONTESTABLE = or ; VALIDE = navy.
10. `vicesIdentifies` rendus en chips alerte (or, avec icône warning).
11. Chip info `actionEnReductionPossible` si `true` mentionnant art. 920+.
12. Chip info `Délai de contestation : 5 ans (art. 1304)` en mode résultat.
13. Pré-fill IA fonctionnel : `formeTestament`, `dateRedaction`, `saineDEsprit`,
    `legsExcedeQuotiteDisponible` (4 champs avec badge `auto_awesome`).
14. `coherenceAlerts` produit `FORME`, `SAINE_ESPRIT`, `LEGS_EXCEDE_QUOTITE`
    quand les sources IA divergent de la saisie.
15. `CaseDashboardRefreshService.triggerRefresh()` appelé après POST succès.
16. MatSnackBar pour erreurs (pas alert/confirm).
17. Entrée TOOL_REGISTRY `'F-FA-24-testament-validite'` symétrique au pattern.
18. ≥ 12 tests Jest passent.
19. Self-check 5/5 (palette, datepicker, gate FR, refresh, snackbar).

---

## Plan de test

### Tests unitaires Jest (`testament-validite-section.component.spec.ts`) — ≥ 12

1. FRANCE → `isFrance()` true, GET appelé au ngOnInit.
2. BELGIQUE → `isFrance()` false, **aucun** appel HTTP.
3. GET 200 → mode résultat hydraté, showForm false.
4. GET 404 → mode formulaire vierge, showForm true.
5. Pré-fill IA `formeTestament` ← `aiData.formeTestamentDetectee` + provenance IA.
6. Pré-fill IA `dateRedaction` + `saineDEsprit` + `legsExcedeQuotite`.
7. Pré-fill sans `aiData` → aucun pré-remplissage.
8. `onFormeTestamentChange` : provenance reset au changement manuel + reset
   des champs conditionnels d'autres formes.
9. `formValid` : OLOGRAPHE requiert ses 3 champs.
10. `formValid` : AUTHENTIQUE requiert ses 4 champs.
11. `calculate()` POST envoie le body correct + résultat hydraté + snackbar succès.
12. `calculate()` form invalide → pas d'appel HTTP.
13. `calculate()` erreur backend → snackbar rouge.
14. `coherenceAlerts.FORME` présente si IA `aiData.formeTestamentDetectee`
    diverge de la saisie.
15. `coherenceAlerts.LEGS_EXCEDE_QUOTITE` indépendant des autres alertes.
16. `coherenceAlerts` vides après calcul (showForm=false).
17. `verdictBadgeClass` : NUL → critical, CONTESTABLE → warn, VALIDE → info.
18. `viceLabel` : couvre quelques codes vices.

### Plan d'intégration

Validation post-merge backend SF-03 (déjà mergé). Le composant est testé en
unitaire avec mock HTTP. Intégration end-to-end testée manuellement après merge.

### Isolation workspace

Pas d'isolation côté frontend — gate FR + gate côté backend (workspace).

---

## Tables / endpoints / composants impactés

### Endpoints consommés

- `POST /api/v1/case-files/{caseFileId}/testament-validite-analysis` (SF-03 ✓)
- `GET /api/v1/case-files/{caseFileId}/testament-validite-analysis` (SF-03 ✓)

### Composants Angular créés

- `frontend/src/app/case-files/testament-validite-section/`
  - `testament-validite-section.component.ts`
  - `testament-validite-section.component.html`
  - `testament-validite-section.component.scss`
  - `testament-validite-section.component.spec.ts`

### Modèles / services

- `frontend/src/app/core/models/testament-validite.model.ts` (nouveaux types).
- `frontend/src/app/core/services/testament-validite.service.ts` (HttpClient
  wrapper).

### Modifications

- `frontend/src/app/core/models/divorce-accepte.model.ts` :
  ajout 4 champs optionnels dans `FamilleExtractedData`
  (`formeTestamentDetectee`, `dateRedactionTestamentDetectee`,
  `saineDEspritTestateurDetected`, `legsExcedeQuotiteDisponibleDetected`).
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` :
  import + entrée `TOOL_REGISTRY` `'F-FA-24-testament-validite'`.

---

## Hors périmètre

- **Belgique** : F-FA-24-BE-testament (backlog jumeau).
- **Backend** : déjà livré (PR #661).
- **Pipeline IA** : extraction réelle des 4 nouveaux champs IA (le composant est
  no-op gracieux si absents — l'extraction sera faite par une SF backend
  ultérieure dédiée pipeline IA succession).
- **Autres SF de F-FA-24** : donation, réduction (action), partage successoral,
  indivision successorale, rapport à succession.

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Pattern UI canonique** : `devolution-legale-section` (PR #658, F-FA-24
      jumeau) — palette navy/or, datepicker `<input type="date">`, gate
      `workspaceCountry`, refresh service, snackbar, JetBrains Mono pour
      `baseJuridique` et `formule`. Strictement réutilisé.
- [x] **Pré-fill IA** : pattern `immigration-title-decision-section` + helper
      `CoherenceAlertBuilder` (SF-155-05). Strictement réutilisé.
- [x] **Validation F-IA-03** : 4 sources (IA, F96, QUESTION_IA,
      PIECE_MANQUANTE), `CoherenceAlert<F>`, popover trigger directive.
- [x] **Autres pays** : Belgique → backlog jumeau (bannière info, pas masquage).
- [x] **Autres outils succession** : `devolution-legale-section` (différent —
      dévolution légale = absence de testament). Aucune duplication —
      situations métier distinctes (cf. invariant un outil = une situation).

### Verdict

Pattern cohérent F-FA-24 SF-02 dévolution légale, ré-utilisation maximale du
helper `CoherenceAlertBuilder`. Aucune nouvelle dette de convergence créée.

---

## Impact par domaine métier

- **Sensibilité au domaine** : forte — feature 100% droit famille FR.
- **Sensibilité au pays** : forte — Cciv FR uniquement. Belgique = backlog
  jumeau (CC BE art. 895+ avec différences de forme).

---

## Préoccupations transversales

- [x] **Outil décisionnel métier** : F-FA-24 SF-04 = composant frontend dédié
      (1 outil = 1 situation : validité d'un testament), aligné sur le
      pattern F-FA-24 SF-02 dévolution légale.
- [x] **Auth / Principal** : aucun changement — consomme un endpoint déjà
      sécurisé.
- [x] **Workspace context** : aucun changement — gate `workspaceCountry`
      côté composant + gate backend.
- [x] **Navigation / routing** : aucune nouvelle route — composant intégré
      au panel F-IA-04 existant.

Aucune préoccupation critique modifiée — pas besoin de smoke tests E2E.

---

## Self-check pré-commit (5 items)

| # | Item | Statut |
|---|------|--------|
| 1 | Palette statut respecte navy/or/rouge (rouge réservé NUL critique) | OK |
| 2 | Datepicker `<input type="date">` (pas MatDatepicker) | OK |
| 3 | Gate `workspaceCountry === 'FRANCE'` avec bannière info BE (pas masquage) | OK |
| 4 | `CaseDashboardRefreshService.triggerRefresh()` après POST succès | OK |
| 5 | `MatSnackBar` pour erreurs (pas alert/confirm) | OK |

---

## Contrat API (importé de SF-FA-24-03)

POST/GET `/api/v1/case-files/{caseFileId}/testament-validite-analysis`
- Body POST : 21 champs (1 enum forme + dates/age + saineDEsprit +
  4 booléens olographe/authentique/mystique/international + vices/révocation +
  legsExcedeQuotite). Voir SF-FA-24-03 §Contrat API.
- Réponse : `caseFileId`, `formeTestament`, `verdictValidite`,
  `vicesIdentifies[]`, `actionEnReductionPossible`, `delaiContestationAns`,
  `scoreEligibilite`, `baseJuridique`, `formule`, `messages[]`, `country`.
- 18 codes `vicesIdentifies` enum (cf. SF-FA-24-03).
