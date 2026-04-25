# Mini-spec — F-DT-24 / SF-DT-24-01 Backend Clause de non-concurrence (FR)

## Identifiant

`F-DT-24 / SF-DT-24-01`

## Feature parente

`F-DT-24` — Clause de non-concurrence

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-DT-24-01-non-concurrence-backend`

---

## Objectif

Outil décisionnel backend qui évalue la validité d'une clause de non-concurrence en droit
du travail français selon les 4 critères cumulatifs posés par la Cour de cassation
(Cass. soc. 10/07/2002 + L.1221-1 Code du travail) : limitation territoriale, limitation
temporelle, limitation à un objet déterminé, contrepartie financière effective et non
dérisoire. Fournit un score 0-100, un verdict (VALIDE / RISQUE_NULLITE_PARTIELLE / NULLE)
et calcule l'indemnité de contrepartie due ainsi que l'indemnité potentielle en cas de
nullité (clause exécutée mais nulle).

---

## Comportement attendu

### Cas nominal

Entrée :
- Présence de la clause au contrat, description du territoire et durée (mois), description
  de l'objet (secteur d'activité limité), montant mensuel de la contrepartie financière,
  salaire mensuel brut, secteur d'activité, date de prise d'effet.

Le calculateur :

1. **Critère 1 — Territoire** : OK si `clausePresenteContrat=true` ET
   `limiteTerritoireDefini=true` ET `territoireDescription` ne contient pas
   « monde », « tout pays », « illimité », « universel ».
2. **Critère 2 — Durée** : OK si `clausePresenteContrat=true` ET
   `limiteDureeDefinie=true` ET `dureeMois` est strictement positif et
   ≤ 36 mois (3 ans, durée raisonnable jurisprudence constante).
3. **Critère 3 — Objet** : OK si `clausePresenteContrat=true` ET
   `limiteObjetDefini=true` ET `objetDescription` ne contient pas
   « toute activité », « toute fonction », « tout poste ».
4. **Critère 4 — Contrepartie financière** : OK si
   `contrepartieFinancierePresente=true` ET `contrepartieMontantMensuelEur > 0`
   ET `ratio = contrepartie / salaire >= 25 %` (seuil non dérisoire — jurisprudence).
5. `ratioContrepartiePct = contrepartieMontantMensuelEur / salaireMensuelBrutEur × 100`
   (arrondi 1 décimale).
6. `scoreValidite = 25 × nombre de critères respectés` (0, 25, 50, 75 ou 100).
7. **Verdict** :
   - `VALIDE` si 4/4 critères ET ratio ≥ 25 %.
   - `RISQUE_NULLITE_PARTIELLE` si 3/4 critères OU (4/4 mais ratio < 25 %).
   - `NULLE` si ≤ 2 critères OK ou clause absente.
8. **Indemnité contrepartie due** :
   - Si VALIDE : `contrepartieMontantMensuelEur × dureeMois`.
   - Sinon : 0,00 €.
9. **Indemnité potentielle en cas de nullité** :
   - Plancher 1 mois × salaire (jurisprudence Cass. soc. 12/05/2010 — minimum
     symbolique pour exécution illicite).
   - Calculée seulement si verdict ≠ VALIDE et clause présente au contrat ;
     sinon 0,00 €.

Persistance : snapshot JSON complet (inputs + outputs) en entité 1:1 par dossier (upsert
côté service).

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| `salaireMensuelBrutEur` ≤ 0 | Erreur explicite | 400 |
| `salaireMensuelBrutEur` absent | Erreur explicite | 400 |
| `dureeMois` négatif | Erreur explicite | 400 |
| `contrepartieMontantMensuelEur` négatif | Erreur explicite | 400 |
| Workspace pays = BELGIQUE | Refus — outil FR uniquement | 400 |
| Dossier ≠ DROIT_DU_TRAVAIL | Refus | 400 |
| `caseFileId` autre workspace | 404 | 404 |
| GET sans POST préalable | 404 | 404 |

---

## Contrat API (figé pour SF-DT-24-02)

### Endpoint

`POST /api/v1/case-files/{caseFileId}/non-concurrence`
`GET  /api/v1/case-files/{caseFileId}/non-concurrence`

### Request

```json
{
  "clausePresenteContrat": true,
  "limiteTerritoireDefini": true,
  "territoireDescription": "France métropolitaine",
  "limiteDureeDefinie": true,
  "dureeMois": 24,
  "limiteObjetDefini": true,
  "objetDescription": "Édition logiciels SaaS B2B",
  "contrepartieFinancierePresente": true,
  "contrepartieMontantMensuelEur": 1500.00,
  "salaireMensuelBrutEur": 5000.00,
  "secteurActivite": "INFORMATIQUE",
  "datePriseEffet": "2026-04-15"
}
```

`secteurActivite` enum : `INFORMATIQUE`, `COMMERCE`, `INDUSTRIE`, `SERVICES`, `AUTRE`.

### Response

```json
{
  "caseFileId": "uuid",
  "clausePresenteContrat": true,
  "limiteTerritoireDefini": true,
  "territoireDescription": "France métropolitaine",
  "limiteDureeDefinie": true,
  "dureeMois": 24,
  "limiteObjetDefini": true,
  "objetDescription": "Édition logiciels SaaS B2B",
  "contrepartieFinancierePresente": true,
  "contrepartieMontantMensuelEur": 1500.00,
  "salaireMensuelBrutEur": 5000.00,
  "secteurActivite": "INFORMATIQUE",
  "datePriseEffet": "2026-04-15",
  "critere1TerritoireOk": true,
  "critere2DureeOk": true,
  "critere3ObjetOk": true,
  "critere4ContrepartieOk": true,
  "ratioContrepartiePct": 30.0,
  "scoreValidite": 100,
  "verdictValidite": "VALIDE",
  "indemniteContrepartieDueEur": 36000.00,
  "indemnitePotentielleNulliteEur": 0.00,
  "baseJuridique": "Cass. soc. 10/07/2002 + L.1221-1",
  "formule": "...",
  "messages": ["..."],
  "country": "FRANCE"
}
```

`verdictValidite` enum : `VALIDE`, `RISQUE_NULLITE_PARTIELLE`, `NULLE`.

---

## Critères d'acceptation

- [x] POST 4 critères respectés + ratio ≥ 25 % → score 100, VALIDE, indemnité = montant × durée.
- [x] POST 3 critères + ratio ≥ 25 % → score 75, RISQUE_NULLITE_PARTIELLE.
- [x] POST 4 critères mais ratio < 25 % → score 100 mais verdict RISQUE_NULLITE_PARTIELLE.
- [x] POST clause absente → score 0, NULLE, indemnité due = 0, indemnité nullité = 0.
- [x] POST 2 critères → NULLE, indemnité nullité = salaire × 1.
- [x] POST territoire « monde entier » détecté comme non limité → critère 1 KO.
- [x] POST objet « toute activité » détecté → critère 3 KO.
- [x] POST durée > 36 mois → critère 2 KO.
- [x] POST contrepartie 0 € → critère 4 KO.
- [x] GET après POST renvoie le snapshot.
- [x] GET sans POST → 404.
- [x] Workspace BE → 400.
- [x] Dossier IMMIGRATION → 400.
- [x] Autre workspace → 404.
- [x] Salaire ≤ 0 → 400.

## Plan de test

### Tests unitaires (≥ 14)

Calculator — chaque branche couverte :
1. 4 critères OK + ratio 30 % → VALIDE 100, indemnité = 1500 × 24 = 36 000.
2. 3 critères OK (territoire KO) → RISQUE_NULLITE_PARTIELLE 75.
3. 4 critères OK mais ratio 20 % → RISQUE_NULLITE_PARTIELLE.
4. 2 critères OK → NULLE 50.
5. Clause absente → NULLE 0, indemnité nullité 0.
6. Verdict NULLE avec clause présente → indemnité nullité = salaire × 1.
7. Territoire « monde entier » → critère 1 KO.
8. Territoire « tout pays » → critère 1 KO.
9. Objet « toute activité » → critère 3 KO.
10. Durée 48 mois → critère 2 KO.
11. Durée 0 mois → critère 2 KO.
12. Contrepartie 0 € → critère 4 KO + ratio 0.
13. Salaire null ou ≤ 0 → IllegalArgumentException.
14. Pays BELGIQUE → IllegalArgumentException.
15. Critère ratio borderline 25.0 % → VALIDE.

### Tests d'intégration (≥ 8)

Controller IT (MockMvc + H2) :
1. POST nominal FR 4/4 → 200 + score 100 + VALIDE.
2. POST 3/4 → 200 + RISQUE_NULLITE_PARTIELLE.
3. POST clause absente → 200 + NULLE.
4. POST salaire = 0 → 400.
5. POST workspace BE → 400.
6. POST dossier IMMIGRATION → 400.
7. POST autre workspace → 404.
8. GET sans POST → 404.
9. GET après POST → snapshot complet.
10. POST upsert remplace l'analyse existante.

---

## Tables / endpoints / composants impactés

- Nouvelle table : `non_concurrence_analyses` (1:1 par dossier).
- Migration Liquibase 149.
- Visibility rule F-IA-04 : `f1a04001-0000-0000-0000-ee0000000241`,
  ALWAYS_ON, FRANCE, DROIT_DU_TRAVAIL, priority 60.
- Endpoints : `POST/GET /api/v1/case-files/{caseFileId}/non-concurrence`.
- Pas d'impact UI dans cette SF (frontend = SF-DT-24-02 vague suivante).

---

## Hors périmètre

- Frontend Angular (SF-DT-24-02).
- Variante BELGIQUE (CCT 1bis CCT 1.04.1996 + Loi 03/07/1978 art. 65) — feature jumelle backlog.
- Pré-fill IA (SF-DT-24-02 ou ultérieur).
- Génération de document de levée/renonciation à la clause (F-DT-31 transaction).

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Outils similaires** : F-DT-08 validité licenciement, F-DT-10 validité rupture
  conv., F-DT-32 documents fin contrat — tous trois utilisent score 0-100 + verdict
  catégoriel + snapshot JSON 1:1 par dossier. Pattern **réutilisé tel quel** ici (4 critères
  binaires × 25 pts = score 0/25/50/75/100, verdict VALIDE/RISQUE/NULLE, indemnité plancher
  pour cas non valides).
- [x] **FR vs BE** : la Belgique a son propre régime (CCT 1bis du 04/03/1981 + CCT 14
  du 27/05/1976 + Loi 03/07/1978 art. 65 — durée max 12 mois pour ouvriers, 12 mois
  pour employés sauf clause spéciale, indemnité forfaitaire ≥ 50 % brut sur la durée).
  → SF jumelle F-DT-24 BE à scoper séparément (backlog).
- [x] **Domaines** : strictement DROIT_DU_TRAVAIL FR — clause non-concurrence n'a pas
  d'équivalent dans Famille / Immigration.
- [x] **UI patterns** : pas concerné côté backend ; frontend SF-DT-24-02 réutilisera le
  template canonique des sections décisionnelles (cf. `ai-skills/frontend-coherence-audit.md`).
- [x] **Pré-remplissage IA** : possible depuis synthèse — frontend SF-DT-24-02 le câblera.

### Nouveau pattern UI ou service partagé

Aucun composant partagé / DTO réutilisable / endpoint transversal introduit. Tout est
local au domaine F-DT-24.

---

## Impact par domaine métier

Cette feature **est sensible au domaine** : strictement Droit du travail, FR uniquement
dans cette SF.

- Droit du travail FR : couvert (cette SF).
- Droit du travail BE : SF jumelle au backlog (régime CCT distinct).
- Famille FR/BE : non applicable (concept absent).
- Immigration FR/BE : non applicable (concept absent).

---

## Parité des domaines métier

Outil de **niveau 5** (scoring + analyse validité) : la parité avec les 2 autres domaines
n'est pas pertinente — la clause de non-concurrence n'existe que dans le contrat de
travail. Asymétrie justifiée.

---

## Préoccupations transversales

| Préoccupation | Concerné | Action |
|---------------|----------|--------|
| Auth / Principal | Non | RAS — utilise `CurrentUserResolver` standard. |
| Workspace context | Oui | Filtre `workspace_id` via `WorkspaceMemberRepository.findByUserAndPrimaryTrue` (pattern jumelles). |
| Plans / limites | Non | RAS pour cette SF (gating au niveau panel F-IA-04). |
| Navigation / routing | Non | RAS. |
| Outil décisionnel métier | Oui | Une situation métier unique (clause de non-concurrence). Aucun mélange. |
