# Mini-spec — F-DT-31 / SF-DT-31-02 Transaction art. 2044 Cciv — FRONTEND

## Identifiant

`F-DT-31 / SF-DT-31-02`

## Feature parente

`F-DT-31` — Transaction / protocole transactionnel (FR uniquement, art. 2044-2058 Cciv)

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-DT-31-02-frontend-transaction`

## Subfeature jumelle (parallélisée)

`SF-DT-31-01` — backend (contrat API ci-dessous, importé tel quel).

---

## Objectif

Composant Angular standalone `transaction-section` (FR uniquement, art. 2044
Cciv) qui consomme l'API `POST + GET /api/v1/case-files/{caseFileId}/transaction`
exposée par `SF-DT-31-01`. Affiche un verdict de validité de la transaction
(VALIDE / CONTESTABLE / NULLE), un score, une carte concessions réciproques,
le ratio des concessions, la comparaison avec le barème Macron et les messages.
Pré-fill IA + alertes F-IA-03 obligatoires.

---

## Contrat API (importé de SF-DT-31-01)

- **POST** `/api/v1/case-files/{caseFileId}/transaction`
- **GET** `/api/v1/case-files/{caseFileId}/transaction`

### Enums

```typescript
export type ConcessionEmployeur =
  | 'INDEMNITE_TRANSACTIONNELLE'
  | 'MAINTIEN_AVANTAGES_SOCIAUX'
  | 'LETTRE_RECOMMANDATION'
  | 'LEVEE_NON_CONCURRENCE'
  | 'DELAI_PRESCRIPTION_ANTICIPE'
  | 'AUTRE';

export type ConcessionSalarie =
  | 'RENONCIATION_ACTION_PRUDHOMALE'
  | 'RENONCIATION_PARTAGE_BENEFICES'
  | 'AUTRE';

export type RupturePrealable =
  | 'LICENCIEMENT_PERSONNEL'
  | 'LICENCIEMENT_ECONOMIQUE'
  | 'RUPTURE_CONVENTIONNELLE'
  | 'DEMISSION'
  | 'FIN_CDD'
  | 'AUTRE';

export type VerdictValiditeContrat = 'VALIDE' | 'CONTESTABLE' | 'NULLE';
```

### Request body

```json
{
  "dateSignature": "2026-04-25",
  "concessionsEmployeur": ["INDEMNITE_TRANSACTIONNELLE"],
  "concessionsSalarie": ["RENONCIATION_ACTION_PRUDHOMALE"],
  "indemniteTransactionnelleEur": 12000,
  "salaireMensuelBrutEur": 3000,
  "ancienneteAnnees": 6,
  "renonciationActionExpresse": true,
  "delaiReflexion15jOk": true,
  "rupturePrealable": "LICENCIEMENT_PERSONNEL",
  "presenceAvocatAssistance": false,
  "viceConsentementAllégué": false
}
```

### Response

```json
{
  "caseFileId": "uuid",
  "dateSignature": "2026-04-25",
  "concessionsEmployeur": ["INDEMNITE_TRANSACTIONNELLE"],
  "concessionsSalarie": ["RENONCIATION_ACTION_PRUDHOMALE"],
  "indemniteTransactionnelleEur": 12000,
  "salaireMensuelBrutEur": 3000,
  "ancienneteAnnees": 6,
  "renonciationActionExpresse": true,
  "delaiReflexion15jOk": true,
  "rupturePrealable": "LICENCIEMENT_PERSONNEL",
  "presenceAvocatAssistance": false,
  "viceConsentementAllégué": false,
  "concessionsReciproquesCaracterisees": true,
  "ratioConcessionsEmployeurPct": 0.42,
  "indemniteTransactionnelleSuperieureMacron": true,
  "scoreValidite": 78,
  "verdictValiditeContrat": "VALIDE",
  "risqueNulliteRetenu": false,
  "baseJuridique": "Art. 2044-2058 Cciv + jurisprudence Cass. Soc.",
  "formule": "score = 78/100",
  "messages": ["Concessions réciproques caractérisées."],
  "country": "FRANCE"
}
```

### Codes erreur

- 400 si champs requis manquants ou pays BELGIQUE
- 404 GET sans POST → form mode (pré-fill IA)
- 409 si dossier non FR / DROIT_DU_TRAVAIL (gate backend)

---

## Form (FR uniquement)

| Champ | UI | Required |
|---|---|---|
| `dateSignature` | `<input type="date">` (convention canonique — pas MatDatepicker) | Oui |
| `concessionsEmployeur` | `mat-select multiple` (6 options) | Oui (≥ 1) |
| `concessionsSalarie` | `mat-select multiple` (3 options) | Oui (≥ 1) |
| `indemniteTransactionnelleEur` | `mat-input` type number, min=0, step=100 | Oui (≥ 0) |
| `salaireMensuelBrutEur` | `mat-input` type number, min=0, step=10 | Oui (> 0) |
| `ancienneteAnnees` | `mat-input` type number, min=0, step=0.5 | Oui (≥ 0) |
| `renonciationActionExpresse` | `mat-slide-toggle` | Oui (boolean default false) |
| `delaiReflexion15jOk` | `mat-slide-toggle` | Oui (boolean default false) |
| `rupturePrealable` | `mat-select` (6 options, single) | Oui |
| `presenceAvocatAssistance` | `mat-slide-toggle` | Oui (boolean default false) |
| `viceConsentementAllégué` | `mat-slide-toggle` | Oui (boolean default false) |

`formValid` = `dateSignature` non null + `concessionsEmployeur.length ≥ 1` +
`concessionsSalarie.length ≥ 1` + `indemniteTransactionnelleEur ≥ 0` +
`salaireMensuelBrutEur > 0` + `ancienneteAnnees ≥ 0` + `rupturePrealable !== null`.

### Libellés FR

- ConcessionEmployeur :
  - `INDEMNITE_TRANSACTIONNELLE` → "Indemnité transactionnelle"
  - `MAINTIEN_AVANTAGES_SOCIAUX` → "Maintien des avantages sociaux"
  - `LETTRE_RECOMMANDATION` → "Lettre de recommandation"
  - `LEVEE_NON_CONCURRENCE` → "Levée de la clause de non-concurrence"
  - `DELAI_PRESCRIPTION_ANTICIPE` → "Délai de prescription anticipé"
  - `AUTRE` → "Autre"
- ConcessionSalarie :
  - `RENONCIATION_ACTION_PRUDHOMALE` → "Renonciation à l'action prud'homale"
  - `RENONCIATION_PARTAGE_BENEFICES` → "Renonciation au partage des bénéfices"
  - `AUTRE` → "Autre"
- RupturePrealable :
  - `LICENCIEMENT_PERSONNEL` → "Licenciement (motif personnel)"
  - `LICENCIEMENT_ECONOMIQUE` → "Licenciement (motif économique)"
  - `RUPTURE_CONVENTIONNELLE` → "Rupture conventionnelle"
  - `DEMISSION` → "Démission"
  - `FIN_CDD` → "Fin de CDD"
  - `AUTRE` → "Autre"

---

## Affichage résultat

1. **Bannière verdict** (`verdictValiditeContrat`) :
   - `VALIDE` → palette navy + icône `verified`
   - `CONTESTABLE` → palette or (accent gold) + icône `gavel`
   - `NULLE` → palette rouge classique (alerte critique) + icône `error`
2. **Carte "Score de validité"** : `scoreValidite / 100`.
3. **Carte "Concessions réciproques"** : ✓ ou ✗ selon `concessionsReciproquesCaracterisees`,
   sous-libellé "art. 2044 Cciv".
4. **Carte "Ratio concessions employeur"** : pourcentage (`ratioConcessionsEmployeurPct × 100`),
   formaté 1 décimale.
5. **Carte "Comparaison barème Macron"** : `indemniteTransactionnelleSuperieureMacron`
   ✓ "supérieure" / ✗ "inférieure ou égale" — sous-libellé "art. L.1235-3".
6. **Carte "Risque de nullité retenu"** : ✓ ou ✗.
7. **Liste `<ul>` `messages`** rendue via `LegalCitationsPipe`.
8. **`baseJuridique`** + **`formule`** en `JetBrains Mono`.

Bouton "Modifier" pour rouvrir le form.

---

## Pré-fill IA (OBLIGATOIRE)

Via `@Input() aiData?: TravailExtractedData | null` — null-safe.

Champs pré-remplis depuis `aiData` :
- `salaireMensuelBrutEur` ← `aiData.salaireBrutMensuel` (si > 0)
- `ancienneteAnnees` ← calculé depuis `aiData.dateEntree` et `aiData.dateLicenciement`
  (si les 2 sont des dates ISO valides), sinon depuis seul `dateEntree` vs aujourd'hui
- `rupturePrealable` ← mapping depuis `aiData.motifLicenciement` :
  - `LICENCIEMENT_PERSONNEL` ou label contenant "personnel" → `LICENCIEMENT_PERSONNEL`
  - `LICENCIEMENT_ECONOMIQUE` ou label contenant "économique" → `LICENCIEMENT_ECONOMIQUE`
  - `RUPTURE_CONVENTIONNELLE` ou label contenant "conventionnelle" → `RUPTURE_CONVENTIONNELLE`
  - `DEMISSION` ou label contenant "démission" → `DEMISSION`
  - `FIN_CDD` ou label contenant "cdd" → `FIN_CDD`
  - sinon → pas de pré-fill (graceful)

Pas de pré-fill pour `dateSignature`, `concessionsEmployeur`, `concessionsSalarie`,
`indemniteTransactionnelleEur`, `renonciationActionExpresse`, `delaiReflexion15jOk`,
`presenceAvocatAssistance`, `viceConsentementAllégué` — données qui n'ont pas
d'équivalent direct dans `TravailExtractedData`.

`provenance<Field>` signals + badges `<mat-icon>auto_awesome</mat-icon> Pré-rempli
depuis l'analyse`. Toute modif manuelle (`onXxxChange`) efface le badge IA.

---

## F-IA-03 — Alertes de cohérence

`coherenceAlerts` (computed, gate strict `showForm()`) — multi-sources via
`CoherenceAlertBuilder` partagé :

- `SALAIRE_MENSUEL` : divergence > 10 % entre `aiData.salaireBrutMensuel` et la
  saisie avocat. Multi-sources : IA + F96 (`DT31_SALAIRE_MENSUEL`) + QUESTION_IA
  + PIECE_MANQUANTE (`DT31_SALAIRE_MENSUEL`, `FICHE_PAIE`).
- `ANCIENNETE` : divergence > 1 an absolue entre l'ancienneté calculée IA et
  la saisie avocat. Multi-sources : IA + F96 + PIECE_MANQUANTE
  (`DT31_ANCIENNETE`, `CONTRAT_TRAVAIL`).
- `RUPTURE_PREALABLE` : divergence si IA mappe à un autre code que la saisie.
  Source IA uniquement (mapping interne).

Affichage badge or (palette F-IA-03 standard, pas rouge — règle `frontend-coherence-audit`)
+ popover via `CoherencePopoverTriggerDirective` (clé `source-explanations`).

---

## Gate workspaceCountry

- `FRANCE` → form actif.
- `BELGIQUE` → bannière info "Transaction art. 2044 Cciv — droit français
  uniquement. Le droit belge est régi par les art. 1043 et suivants du Code civil."
  — **pas masquage silencieux** (règle CLAUDE.md, leçon F-155).

---

## Composants impactés

### Nouveaux fichiers

- `frontend/src/app/core/models/transaction.model.ts`
- `frontend/src/app/core/services/transaction.service.ts`
- `frontend/src/app/case-files/transaction-section/transaction-section.component.ts`
- `frontend/src/app/case-files/transaction-section/transaction-section.component.html`
- `frontend/src/app/case-files/transaction-section/transaction-section.component.scss`
- `frontend/src/app/case-files/transaction-section/transaction-section.component.spec.ts`
- `docs/features/F-DT-31/SF-DT-31-02-frontend-transaction.md` (ce fichier)

### Modifications

- Aucune modification existante : `TravailExtractedData` est consommé sans
  ajout de champ. Pas de migration sur `decisional-tools-panel.component.ts`
  (entrée TOOL_REGISTRY documentée ci-dessous, intégration future).

### Hors scope (NE PAS modifier)

- `decisional-tools-panel.component.ts` (TOOL_REGISTRY entry documentée
  ci-dessous mais non intégrée dans cette SF)
- `docs/PRODUCT_SPEC.md` (mise à jour post-merge uniquement)

### TOOL_REGISTRY entry (à intégrer ultérieurement)

```typescript
import { TransactionSectionComponent } from '../transaction-section/transaction-section.component';

['F-DT-31-transaction', {
  component: TransactionSectionComponent,
  inputs: (ctx) => ({
    caseFileId: ctx.caseFileId,
    workspaceCountry: ctx.workspaceCountry,
    aiData: ctx.synthesis?.travailExtractedData,
    procedureChecks: ctx.procedureChecks,
    aiQuestions: ctx.aiQuestions,
    piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
  }),
}],
```

---

## Plan de test (≥ 12 UT)

1. Mount sans erreur (FRANCE).
2. `formValid` faux si `concessionsEmployeur` vide.
3. `formValid` faux si `concessionsSalarie` vide.
4. `formValid` faux si `salaireMensuelBrutEur ≤ 0`.
5. `formValid` vrai si tous les champs requis renseignés.
6. GET 200 → form masqué + valeurs persistées + pas de badge IA.
7. GET 404 → reste en form mode + pré-fill IA appliqué.
8. `calculate()` POST nominal → résultat affiché + snackbar + `dashboardRefresh.triggerRefresh()`.
9. `calculate()` erreur 400 → snackbar `panelClass: 'snack-error'`.
10. `calculate()` ignoré si form invalide (pas de POST).
11. Pré-fill IA `salaireBrutMensuel` → champ rempli + provenance IA.
12. Pré-fill IA `motifLicenciement = LICENCIEMENT_PERSONNEL` → `rupturePrealable` mappé.
13. `onSalaireChange` efface le badge IA.
14. `coherenceAlerts.SALAIRE_MENSUEL` présent si divergence > 10 %.
15. `coherenceAlerts.SALAIRE_MENSUEL` absent si écart ≤ 10 %.
16. `coherenceAlerts.SALAIRE_MENSUEL` enrichi par F96 → contributors `IA`+`F96` → `MULTI`.
17. `coherenceAlerts.SALAIRE_MENSUEL` enrichi par PIECE_MANQUANTE.
18. Alertes masquées après `showForm.set(false)`.
19. Gate BELGIQUE → form non rendu, GET non appelé.
20. Gate FRANCE → load() appelé.
21. `ngOnChanges(aiData)` post-mount rafraîchit le pré-fill si form vide.
22. `toggleCollapse` ouvre/ferme la section.
23. `editMode` ré-affiche le form.
24. `verdictBannerClass` renvoie strong/medium/weak selon verdict.

---

## Design system

- Standalone component, palette navy/or — rouge **classique uniquement** pour
  verdict `NULLE` (alerte critique).
- `Inter` pour le corps du texte, `JetBrains Mono` pour `formule` + `baseJuridique`.
- Datepicker : `<input type="date">` (convention canonique).
- Citations juridiques rendues via `LegalCitationsPipe`.
- `MatSnackBar` pour erreurs (pas d'`alert()` ni `confirm()`).
- `CaseDashboardRefreshService.triggerRefresh()` après POST succès.

---

## Pattern de référence

- **Canonical** : `harcelement-licenciement-nul-section` (HLN, F-DT-11) —
  template canonique IA-compliant (cf. `ai-skills/frontend-coherence-audit.md` §5).
- **Multi-select** : `divorce-faute-section` (F-FA-09) — mat-select multiple
  + multi-sources F-IA-03 (DUREE_MARIAGE, REVENUS, FAUTES_INVOQUEES) via
  `CoherenceAlertBuilder`.
- **Gate FR-only avec bannière info** : `divorce-faute-section` (F-FA-09)
  et `oqtf-sans-delai-section` (F-IM-08-04).

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] Outils similaires : F-DT-08 (validité licenciement) et F-DT-10 (validité
  rupture conv.) reposent sur le même pattern « scoring 0-100 + verdict 3
  niveaux ». Pattern réutilisé.
- [x] FR vs BE : la BE a sa propre transaction (art. 1043 et suivants Code
  civil belge) → SF jumelle backlog (notée dans la bannière info).
- [x] Domaines : strict DROIT_DU_TRAVAIL (transaction post-rupture).
- [x] UI patterns : strict template HLN canonique + multi-select divorce-faute.
- [x] Pré-remplissage IA : extension naturelle depuis salaireBrutMensuel +
  motifLicenciement + dateEntree (déjà extraits par le pipeline IA).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern score+verdict (F-DT-08/10/16) | Oui | Réutilisé (3 niveaux : VALIDE/CONTESTABLE/NULLE) |
| F-DT-31 BE | Oui | Backlog feature jumelle (transaction Code civil belge) |
| Refresh dashboard F-IA-02 | Oui | `dashboardRefresh.triggerRefresh()` après POST |
| F-IA-03 cohérence | Oui | 3 alertes (salaire, ancienneté, rupturePrealable) |
| F-IA-04 visibility rule | Oui | À gérer dans SF-DT-31-01 backend (priority alignée) |

### Décision

- [x] Étendu à toutes les cibles applicables côté frontend dans cette SF
- [x] BE = feature jumelle backlog (F-DT-31-BE potentiellement)
- [x] Intégration `decisional-tools-panel.ts` reportée (entrée documentée)

---

## Parité des domaines métier (niveau 5 — scoring)

- DROIT_TRAVAIL FR : F-DT-31 (cette SF) — transaction art. 2044 Cciv. **Couvert.**
- DROIT_TRAVAIL BE : transaction Code civil belge → backlog jumeau (à scoper).
- IMMIGRATION : non applicable (pas de transaction post-rupture en immigration).
- FAMILLE : transaction patrimoniale ≠ transaction prud'homale — non applicable.

---

## Impact par domaine métier

- DROIT_TRAVAIL : OUI, FR uniquement (art. 2044 Cciv). BE → bannière info.
- IMMIGRATION : non applicable.
- FAMILLE : non applicable.

---

## Nouveau pattern UI ou service partagé

Aucun nouveau pattern partagé introduit — ce composant suit strictement le
template HLN canonique + le pattern multi-select / multi-sources F-IA-03 du
divorce-faute (F-FA-09). `CoherenceAlertBuilder` partagé réutilisé tel quel.

---

## Préoccupations transversales

- **Outil décisionnel métier** : oui (création).
  - Composants impactés (validation invariant 1 outil = 1 situation) :
    F-DT-08 (validité licenciement), F-DT-10 (rupture conv. validité), F-DT-16
    (licenciement nul détection) — situations distinctes, pas de fusion.
  - Test de non-régression : aucune modification de composant existant,
    périmètre nouveau isolé.
- **Workspace context** : non — l'endpoint utilise `caseFileId` qui résout déjà
  le workspace côté backend.
- **Auth / Principal** : non.
- **Plans / limites** : non.
- **Navigation / routing** : non.

Smoke tests E2E : non concernés (composant nouveau isolé).

---

## Hors scope

- Génération PDF du protocole transactionnel (autre SF F-DT-31-03 backlog).
- Détection IA des concessions employeur/salarié (extraction LLM dédiée → backlog).
- Mise à jour `decisional-tools-panel.component.ts` (intégration ultérieure).
- Mise à jour `docs/PRODUCT_SPEC.md` (post-merge).
- Implémentation BE (feature jumelle backlog).
- Détection automatique du vice du consentement (saisie manuelle uniquement).
