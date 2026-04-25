# Mini-spec — F-DT-28 / SF-DT-28-01 Avantages conventionnels BE — BACKEND

## Identifiant
`F-DT-28 / SF-DT-28-01`

## Feature parente
`F-DT-28` — Avantages conventionnels belges (pécule de vacances + prime de fin d'année + éco-chèques + chèques-repas)

## Statut `draft` · Date `2026-04-25` · Branche `feat/SF-DT-28-01-avantages-conventionnels-be-backend`

---

## Objectif

Outil décisionnel dédié au calcul agrégé des **avantages conventionnels obligatoires en Belgique** pour un employé (CP 200 et assimilés) :

1. **Pécule de vacances** simple + double (Loi 28/06/1971 sur les vacances annuelles)
2. **Prime de fin d'année** (CCT sectorielle, ex. CP 200 / CP 124 / CP 209)
3. **Éco-chèques** (CCT 98 du Conseil National du Travail)
4. **Chèques-repas** (CCT 19octies)

**Spécificité** : outil **single-country** (BE uniquement). L'équivalent FR (13e mois, prime d'ancienneté CCN) est traité par convention individuelle / CCN française et n'a pas de plafond légal national équivalent — couvert le cas échéant par F-DT-04 (fiche prud'homale).

---

## Comportement

### Inputs

- `salaireMensuelBrutEur` : BigDecimal > 0 (rémunération brute mensuelle de référence)
- `joursTravaillesAnneePrecedente` : int ≥ 0 (jours assimilés pour le pécule)
- `anciennetteMois` : int ≥ 0
- `commissionParitaire` : String (`CP_200`, `CP_124`, `CP_209`, `AUTRE`)
- `annee` : int (année de calcul, ex. 2026)
- `doublePeculeVacancesPercu` : boolean (l'employé a-t-il déjà perçu son double pécule cette année ?)
- `primeFinAnneePrevueCcCp` : boolean (la CCT sectorielle prévoit-elle une prime ?)
- `ecoChequesPrevuCcCp` : boolean (la CCT prévoit-elle des éco-chèques ?)
- `ecoChequesUtilisationDansAn` : boolean (info de validité dans les délais — facultatif, message info si false)
- `chequesRepasPrevu` : boolean
- `joursPrestesEffectifs` : int ≥ 0 (jours réellement prestés pour le calcul des chèques-repas)

### Calculs

1. **Pécule de vacances simple** = `salaireMensuelBrut × 12 × 7,67 %` (employés CCT)
   - Formule de référence : 7,67 % de la rémunération annuelle (Loi 28/06/1971)
2. **Double pécule de vacances** ≈ `0,50 × salaireMensuelBrut × 12 × 0,92` (simplification)
   - Si `doublePeculeVacancesPercu = true` → 0 (déjà payé)
3. **Prime de fin d'année** = `salaireMensuelBrut × 1` (1 mois) si `primeFinAnneePrevueCcCp = true` ; 0 sinon
4. **Éco-chèques** = 250 € (plafond CP 200 et plafond ONSS exonération) si `ecoChequesPrevuCcCp = true` ; 0 sinon
5. **Chèques-repas** = `joursPrestesEffectifs × 8 €` (valeur faciale standard 8 €/jour) si `chequesRepasPrevu = true` ; 0 sinon
6. **Total annuel** = somme des 5 lignes (pécule simple + double pécule + prime + éco-chèques + chèques-repas)

### Outputs

- `caseFileId` (rappel)
- input echo (12 champs)
- `peculeVacancesSimpleEur` : BigDecimal (2 décimales)
- `doublePeculeVacancesEur`
- `primeFinAnneeEur`
- `ecoChequesValeurAnnuelleEur`
- `chequesRepasValeurAnnuelleEur`
- `totalAvantagesAnnuelsEur`
- `formule` : texte récapitulatif
- `baseJuridique` : "Loi 28/06/1971 (pécule) + CCT 98 (éco-chèques) + CCT 19octies (chèques-repas)"
- `messages` : liste pédagogique (≥ 4 messages)
- `country` : "BELGIQUE"

### Cas d'erreur
- salaire ≤ 0 → 400
- jours travaillés < 0 → 400
- ancienneté < 0 → 400
- jours prestés < 0 → 400
- annee < 1971 → 400
- commissionParitaire null → 400
- Workspace FRANCE → 400 "Avantages conventionnels BE uniquement"
- Dossier autre domaine → 400
- Workspace étranger → 404

---

## Contrat API (FIGÉ pour SF-DT-28-02)

### POST + GET `/api/v1/case-files/{caseFileId}/avantages-conventionnels-be`

**Request :**
```json
{
  "salaireMensuelBrutEur": 3000.00,
  "joursTravaillesAnneePrecedente": 220,
  "anciennetteMois": 24,
  "commissionParitaire": "CP_200",
  "annee": 2026,
  "doublePeculeVacancesPercu": false,
  "primeFinAnneePrevueCcCp": true,
  "ecoChequesPrevuCcCp": true,
  "ecoChequesUtilisationDansAn": true,
  "chequesRepasPrevu": true,
  "joursPrestesEffectifs": 220
}
```

**Response :**
```json
{
  "caseFileId": "uuid",
  "salaireMensuelBrutEur": 3000.00,
  "joursTravaillesAnneePrecedente": 220,
  "anciennetteMois": 24,
  "commissionParitaire": "CP_200",
  "annee": 2026,
  "doublePeculeVacancesPercu": false,
  "primeFinAnneePrevueCcCp": true,
  "ecoChequesPrevuCcCp": true,
  "ecoChequesUtilisationDansAn": true,
  "chequesRepasPrevu": true,
  "joursPrestesEffectifs": 220,
  "peculeVacancesSimpleEur": 2761.20,
  "doublePeculeVacancesEur": 16560.00,
  "primeFinAnneeEur": 3000.00,
  "ecoChequesValeurAnnuelleEur": 250.00,
  "chequesRepasValeurAnnuelleEur": 1760.00,
  "totalAvantagesAnnuelsEur": 24331.20,
  "formule": "Pécule simple 7,67 % × 36 000 € = 2 761,20 € | Double pécule 0,50 × 36 000 × 0,92 = 16 560 € | Prime 1 mois = 3 000 € | Éco-chèques 250 € | Chèques-repas 220 × 8 = 1 760 €",
  "baseJuridique": "Loi 28/06/1971 (pécule) + CCT 98 (éco-chèques) + CCT 19octies (chèques-repas)",
  "messages": ["..."],
  "country": "BELGIQUE"
}
```

GET retourne la dernière analyse persistée (404 sinon).

---

## Architecture

Pattern standard MotifGraveBe / IndemnitePrecariteCdd. Table `avantages_conventionnels_be_analyses` (migration **156**). Tool_id `F-DT-28-avantages-conventionnels-be`. **1 règle visibility ALWAYS_ON BELGIQUE / DROIT_DU_TRAVAIL** uniquement. UUID `f1a04001-0000-0000-0000-ee0000000281`, priority 62.

### Composants à créer
- `AvantagesConventionnelsBeCalculator.java`
- `AvantagesConventionnelsBeAnalysis.java`
- `AvantagesConventionnelsBeRepository.java`
- `AvantagesConventionnelsBeRequest/Response/Result.java`
- `AvantagesConventionnelsBeService.java`
- `AvantagesConventionnelsBeController.java`
- Migration `156-create-avantages-conventionnels-be-analyses.xml`

## Plan de test

### UT (`AvantagesConventionnelsBeCalculatorTest`, ≥ 12)
1. Cas nominal CP 200 complet → 5 lignes positives, total ≈ 24 331 €
2. Pécule simple uniquement (toutes les autres opts à false) → seul le pécule est calculé
3. `doublePeculeVacancesPercu=true` → double pécule = 0
4. `primeFinAnneePrevueCcCp=false` → prime = 0
5. `ecoChequesPrevuCcCp=false` → éco-chèques = 0
6. `chequesRepasPrevu=false` → chèques-repas = 0
7. `joursPrestesEffectifs=0` → chèques-repas = 0 même si prévu
8. salaire = 1 → tous les montants minimaux > 0 (sauf double pécule)
9. salaire ≤ 0 → IllegalArgumentException
10. anciennetteMois < 0 → IllegalArgumentException
11. joursPrestesEffectifs < 0 → IllegalArgumentException
12. annee < 1971 → IllegalArgumentException
13. commissionParitaire null → IllegalArgumentException
14. Total = somme exacte des 5 lignes (vérification arithmétique)
15. Messages contiennent les bases juridiques (Loi 28/06/1971, CCT 98, CCT 19octies)

### IT (`AvantagesConventionnelsBeControllerIT`, ≥ 6)
1. POST BE workspace nominal → 200 + total > 0
2. POST workspace FRANCE → 400
3. POST dossier IMMIGRATION BE → 400
4. POST workspace étranger → 404
5. POST salaire 0 → 400
6. Upsert : 2× POST écrase
7. GET après POST → 200 retourne valeurs persistées
8. GET sans POST → 404

---

## Impact domaine

DROIT_DU_TRAVAIL BE uniquement. Pas d'équivalent FR (cf. note ci-dessus).

## Parité niveau ≥5

Niveau 3 (calculateur multi-postes). Parité N/A.

## Préoccupations transversales

- [ ] Auth / Principal : pattern OAuth standard, aucun nouveau type — N/A.
- [ ] Workspace context : gate `country=BELGIQUE` + `legalDomain=DROIT_DU_TRAVAIL` (idem MotifGraveBe).
- [ ] Plans / limites : aucun gate plan spécifique.
- [ ] Navigation : N/A backend pur.
- [ ] Outil décisionnel : 1 outil, 1 situation (cumul agrégé). Pattern simulateur indépendant. Pas de concurrence avec un outil existant.

## Critères d'acceptation

- [ ] Pécule simple = 7,67 % × salaireBrutAnnuel (rounding HALF_UP scale 2).
- [ ] Double pécule = 0 si `doublePeculeVacancesPercu = true`, sinon 0.50 × annuel × 0.92.
- [ ] Prime fin d'année = `salaireMensuelBrut` si prévue par la CCT sectorielle, sinon 0.
- [ ] Éco-chèques = 250 € (plafond CP 200) si prévus, sinon 0.
- [ ] Chèques-repas = `joursPrestesEffectifs × 8` si prévus, sinon 0.
- [ ] Total = somme exacte des 5 lignes.
- [ ] Gate country BELGIQUE strict, gate legalDomain strict, isolation workspace.
- [ ] Migration 156 + 1 règle visibility BELGIQUE only priority 62 UUID `f1a04001-0000-0000-0000-ee0000000281`.
- [ ] ≥ 12 UT + ≥ 6 IT verts.

## Hors scope

- Frontend (SF-DT-28-02 ultérieure).
- Calcul de la table fiscale exacte (taxation distincte du pécule, retenues ONSS) — hors scope (outil informatif).
- Différences sectorielles fines entre CP — approximation CP 200 standard pour MVP.

## Notes

- Plafond éco-chèques CCT 98 : 250 € / an (plafond légal d'exonération ONSS).
- Valeur faciale chèques-repas : standard 8 €/jour (max 7,18 € exonéré, valeur médiane retenue 8 €).
- Source officielle : SPF Emploi, Travail et Concertation sociale + ONSS.
