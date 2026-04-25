# SF-DT-16-01 — Backend Licenciement nul (détection multi-protections + indemnité plancher 6 mois)

## Objectif (1 phrase)

Exposer un endpoint REST qui **détecte automatiquement** les protections légales (maternité, AT, harcèlement, discrimination, lanceur d'alerte, mandat, action en justice) entourant un licenciement et calcule l'indemnité plancher 6 mois × salaire prévue par l'art. L.1235-3-1 al. 2 du Code du travail français.

## Périmètre

- **Pays** : FRANCE uniquement (le pendant BE — Loi 1978 / CCT 109 — reste à scoper séparément ; voir section "Parité des domaines métier").
- **Domaine** : Droit du travail.
- **Couche F-IA-04** : `ALWAYS_ON` — outil affiché systématiquement sur tout dossier travail FR (l'avocat saisit les booléens, le moteur détecte les protections actives).

## Différence avec F-DT-11

- **F-DT-11** : l'avocat **choisit un motif** unique de nullité (parmi 8) → calcule l'indemnité 6 mois.
- **F-DT-16** : le moteur **détecte simultanément** plusieurs protections actives à partir des faits (booléens + dates) → score de probabilité de nullité, plancher 6 mois, ouverture réintégration.

Les deux outils coexistent : F-DT-11 = "j'ai déjà un motif clair" ; F-DT-16 = "vérifie si je suis dans un cas de nullité".

## Comportement nominal

### Endpoint
- `POST /api/v1/case-files/{caseFileId}/licenciement-nul-detection` — calcul + persistance (upsert 1:1 par dossier)
- `GET /api/v1/case-files/{caseFileId}/licenciement-nul-detection` — récupération du dernier calcul

### Request body
```json
{
  "dateNotificationLicenciement": "2026-04-15",
  "salarieEnceinte": false,
  "dateAccouchement": null,
  "salarieAccidentTravail": true,
  "dateConsolidationAT": "2024-01-15",
  "salarieHarceleAvere": false,
  "salarieDiscriminationAlleguee": true,
  "salarieMotifLanceurAlerte": false,
  "salarieMandatRepresentant": false,
  "salarieActionJustice": false,
  "salaireMensuelBrutEur": 2500.00,
  "ancienneteAnnees": 8
}
```

### Response
```json
{
  "caseFileId": "uuid",
  "dateNotificationLicenciement": "2026-04-15",
  "salarieEnceinte": false,
  "dateAccouchement": null,
  "salarieAccidentTravail": true,
  "dateConsolidationAT": "2024-01-15",
  "salarieHarceleAvere": false,
  "salarieDiscriminationAlleguee": true,
  "salarieMotifLanceurAlerte": false,
  "salarieMandatRepresentant": false,
  "salarieActionJustice": false,
  "salaireMensuelBrutEur": 2500.00,
  "ancienneteAnnees": 8,
  "protectionsDetectees": ["ACCIDENT_TRAVAIL", "DISCRIMINATION"],
  "nombreProtectionsActives": 2,
  "nulliteProbable": true,
  "scoreNullite": 70,
  "verdictProbabiliteNullite": "ELEVEE",
  "indemniteMinimumNuliteEur": 15000.00,
  "indemniteMinimumMois": 6,
  "reintegrationOuverte": true,
  "baseJuridique": "Art. L.1235-3-1 al. 2 + L.1226-9 (AT) + L.1132-4 (discrimination)",
  "formule": "Plancher 6 mois × 2500,00 € = 15 000,00 € | 2 protections détectées (ACCIDENT_TRAVAIL + DISCRIMINATION) → nullité ELEVEE",
  "messages": [
    "Plancher 6 mois (art. L.1235-3-1 al. 2) — le juge peut allouer davantage selon le préjudice.",
    "Le salarié peut demander la réintégration dans l'entreprise OU une indemnité minimum 6 mois.",
    "..."
  ],
  "country": "FRANCE"
}
```

### Logique de détection (8 protections FR)

| Booléen / date d'entrée | Protection détectée | Code | Base juridique |
|-------------------------|---------------------|------|----------------|
| `salarieEnceinte = true` OU `dateAccouchement` < 10 semaines avant notif | `MATERNITE` | L.1225-4 | Code du travail |
| `salarieAccidentTravail = true` OU `dateConsolidationAT` à moins de 6 mois de la notif | `ACCIDENT_TRAVAIL` | L.1226-9 | Code du travail |
| `salarieHarceleAvere = true` | `HARCELEMENT` | L.1152-3 | Code du travail |
| `salarieDiscriminationAlleguee = true` | `DISCRIMINATION` | L.1132-4 | Code du travail |
| `salarieMotifLanceurAlerte = true` | `LANCEUR_ALERTE` | L.1132-3-3 | Code du travail |
| `salarieMandatRepresentant = true` | `MANDAT_SYNDICAL` | L.2411-1 | Code du travail |
| `salarieActionJustice = true` | `ACTION_JUSTICE` | L.1132-1 al. dernier | Code du travail |

(8e cas réservé `LIBERTE_FONDAMENTALE` — non détecté automatiquement, hors périmètre SF actuelle.)

### Scoring + verdict

- `nombreProtectionsActives` = `protectionsDetectees.size()`
- `nulliteProbable` = `nombreProtectionsActives ≥ 1`
- `scoreNullite = min(100, nombreProtectionsActives × 35)` → 1 protection = 35, 2 = 70, 3+ = 100
- `verdictProbabiliteNullite` :
  - `0` → `FAIBLE`
  - `35–69` → `MOYENNE`
  - `≥ 70` → `ELEVEE`

### Indemnité minimum

- `indemniteMinimumMois = 6` (constant, art. L.1235-3-1 al. 2)
- `indemniteMinimumNuliteEur = 6 × salaireMensuelBrutEur` (BigDecimal scale 2 HALF_UP)
- `reintegrationOuverte = nulliteProbable`

### Base juridique dynamique

Construite par concaténation des codes des protections détectées avec leurs articles ; si aucune protection : `"Art. L.1235-3-1 al. 2 (aucune protection détectée — vérifier manuellement)"`.

## Cas d'erreur

| Cas | Statut | Comportement |
|-----|--------|--------------|
| `salaireMensuelBrutEur ≤ 0` ou null | 400 | "Salaire mensuel brut requis et strictement positif" |
| `dateNotificationLicenciement` null | 400 | "Date de notification du licenciement requise" |
| `ancienneteAnnees < 0` | 400 | "Ancienneté ne peut être négative" |
| Workspace pays != FRANCE | 400 | "Outil disponible uniquement en FRANCE — pendant BE en cours de scoping" |
| Dossier hors `DROIT_DU_TRAVAIL` | 400 | "Ce dossier n'est pas un dossier de droit du travail" |
| Dossier non accessible | 404 | "Case file not found" |

## Critères d'acceptation

1. POST sur dossier travail FR avec `salarieAccidentTravail=true` + `salarieDiscriminationAlleguee=true` retourne `protectionsDetectees=["ACCIDENT_TRAVAIL","DISCRIMINATION"]`, `scoreNullite=70`, `verdictProbabiliteNullite="ELEVEE"`, `indemniteMinimumNuliteEur=15000.00` (avec salaire 2500).
2. POST avec aucun booléen actif retourne `nulliteProbable=false`, `scoreNullite=0`, `verdictProbabiliteNullite="FAIBLE"`, `protectionsDetectees=[]`.
3. POST avec 3+ protections retourne `scoreNullite=100`.
4. `dateAccouchement` à moins de 10 semaines de la notif déclenche `MATERNITE` même sans `salarieEnceinte=true`.
5. `dateConsolidationAT` à moins de 6 mois de la notif déclenche `ACCIDENT_TRAVAIL` même sans `salarieAccidentTravail=true`.
6. POST sur workspace BE retourne 400.
7. POST sur dossier immigration retourne 400.
8. GET après POST retourne le snapshot persisté.
9. Upsert 1:1 — second POST écrase le premier.
10. Migration 142 crée la table + 1 règle visibility ALWAYS_ON FRANCE.

## Plan de test

### Unitaires (≥ 16) — `LicenciementNulDetectionCalculatorTest`
- 8 protections détectées individuellement (`MATERNITE`, `ACCIDENT_TRAVAIL`, `HARCELEMENT`, `DISCRIMINATION`, `LANCEUR_ALERTE`, `MANDAT_SYNDICAL`, `ACTION_JUSTICE` + dérivé date)
- Aucune protection → score 0, verdict FAIBLE
- 2 protections → score 70 ELEVEE
- 3 protections → score 100 ELEVEE
- 4+ protections → score plafonné 100
- `dateAccouchement` < 10 semaines déclenche MATERNITE
- `dateConsolidationAT` < 6 mois déclenche ACCIDENT_TRAVAIL
- Salaire 0 → IllegalArgumentException
- Date notif null → IllegalArgumentException
- Pays autre que FRANCE → IllegalArgumentException
- Indemnité = 6 × salaire arbitraire
- Base juridique cumule les articles des protections

### Intégration (≥ 8) — `LicenciementNulDetectionControllerIT`
- POST nominal FR, 1 protection → 200 + score 35 MOYENNE
- POST 2 protections → 200 + score 70 ELEVEE
- POST 0 protection → 200 + score 0 FAIBLE
- POST workspace BE → 400
- POST dossier immigration → 400
- POST autre workspace → 404
- POST salaire 0 → 400
- POST date notif manquante → 400
- GET après POST → 200 snapshot
- GET sans POST → 404

### Isolation workspace
- Test "POST autre workspace → 404" couvre l'isolation.

## Tables / endpoints / composants impactés

- **Nouvelle table** `licenciement_nul_detection_analyses` (1 ligne par dossier — case_file_id unique).
- **Nouveau endpoint** `POST + GET /api/v1/case-files/{caseFileId}/licenciement-nul-detection`.
- **Migration Liquibase 142** + 1 règle visibility ALWAYS_ON FRANCE DROIT_DU_TRAVAIL priority 56 UUID `f1a04001-0000-0000-0000-ee0000000161`.
- **Aucune modification** des composants frontend (SF-DT-16-02 prévue).

## Hors périmètre

- Frontend Angular (SF-DT-16-02 — vague suivante).
- Pendant BE Loi 1978 / CCT 109 (scoping ultérieur — feature jumelle au backlog si non couvert par F-DT-11 BE).
- Détection IA automatique des booléens (extraction depuis documents) — couvert par chaîne `TravailExtractedData` ; ce backend n'embarque pas l'extraction, juste la décision.
- Calcul indemnité maxi (juge) — fourchette indicative non requise par la spec.

## Contrat API (figé pour SF-DT-16-02)

- **Méthode** : `POST` + `GET`
- **URL** : `/api/v1/case-files/{caseFileId}/licenciement-nul-detection`
- **Schemas** : voir sections Request/Response ci-dessus.
- **Codes erreur** : 200 nominal, 400 validation, 404 dossier inaccessible.
- **Codes enum** : `protectionsDetectees` ∈ {`MATERNITE`, `ACCIDENT_TRAVAIL`, `HARCELEMENT`, `DISCRIMINATION`, `LANCEUR_ALERTE`, `MANDAT_SYNDICAL`, `ACTION_JUSTICE`}, `verdictProbabiliteNullite` ∈ {`FAIBLE`, `MOYENNE`, `ELEVEE`}.

## Analyse de cohérence transversale

- **Outils décisionnels existants scannés** : F-DT-07/08/09/10/11/12/15/17/19/21/27. Aucun ne fait de détection multi-protections — F-DT-11 est l'outil le plus proche (calcul indemnité 6 mois, motif unique). Pas de divergence — F-DT-16 est complémentaire.
- **Pays FR seul** dans cette SF — assumé. Pendant BE = feature jumelle backlog (Loi 3 juillet 1978 art. 39 + CCT 109).
- **Pattern de pré-fill IA + validation F-IA-03** : ne s'applique qu'au frontend (SF-DT-16-02). Backend n'expose pas de logique IA particulière.
- **Pas de nouveau pattern partagé** introduit (calculator + entity 1:1 par dossier — pattern standard F-DT).

## Impact par domaine métier

- **Droit du travail FRANCE** : oui, c'est le cœur de la SF. 8 protections art. L.1225-4, L.1226-9, L.1152-3, L.1132-4, L.1132-3-3, L.2411-1, L.1132-1.
- **Droit du travail BELGIQUE** : reporté — pendant non implémenté ici (motivation : la nullité belge repose sur Loi 1978 art. 63 + CCT 109 + Loi 1996 art. 32tredecies, mécanique différente, mérite une feature jumelle dédiée). Garde-fou : 400 si workspace BE.
- **Immigration / Famille** : non applicable (concept spécifique au droit du travail). Garde-fou : 400 si domaine != DROIT_DU_TRAVAIL.

## Parité des domaines métier (niveau 5 — scoring)

Cette SF livre un **scoring** (verdict probabilité + score 0-100) → règle CLAUDE.md déclenchée.

- **Travail FR** : couvert par cette SF.
- **Travail BE** : non couvert ici — backlog jumeau requis (mécanique Loi 1978 + CCT 109 différente, ne se transpose pas par switch). À ouvrir comme `F-DT-16-BE` ou intégrer à F-DT-11 BE en extension.
- **Immigration** : non pertinent (concept de nullité de licenciement absent).
- **Famille** : non pertinent.

→ **Action** : note ajoutée à PRODUCT_SPEC F-DT-16 indiquant la non-couverture BE et sera consignée dans l'historique post-merge.
