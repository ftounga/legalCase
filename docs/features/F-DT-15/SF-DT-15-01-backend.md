# Mini-spec — F-DT-15 / SF-DT-15-01 Licenciement pour inaptitude — BACKEND

## Identifiant
`F-DT-15 / SF-DT-15-01`

## Feature parente
`F-DT-15` — Licenciement pour inaptitude (critique 🔴)

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-DT-15-01-inaptitude-backend`

---

## Objectif

Outil décisionnel calculant les indemnités dues au salarié licencié pour inaptitude après avis du médecin du travail. Distinction essentielle **origine professionnelle vs non-professionnelle** (L.1226-10 vs L.1226-2) — doublement de l'indemnité légale + indemnité compensatrice de préavis si origine pro. Extension BE : force majeure médicale art. 34 loi 03/07/1978.

---

## Comportement attendu

### Cas nominal FR (art. L.1226-10 ou L.1226-2)

**Inputs :**
- `salaireMensuelReference` : BigDecimal > 0
- `ancienneteAnnees` : int ≥ 0
- `origineInaptitude` : `PROFESSIONNELLE` ou `NON_PROFESSIONNELLE`
- `reclassementRespecte` : boolean (employeur a-t-il respecté l'obligation de reclassement)
- `avisMedecinTravailDate` : LocalDate (optionnel, pour rappel délai 1 mois avant licenciement)

**Formule FR origine PROFESSIONNELLE (L.1226-10) :**
- `indemniteLegale = (¼ + ⅓ après 10 ans) × salaire × ancienneté` puis × 2 (doublement L.1226-14)
- `indemniteCompensatricePreavis = salaire × préavis_mois` (préavis non exécuté mais indemnisé)
- `damagesReclassement = 12 × salaire` (minimum L.1226-15 si reclassement non respecté, peut être plus élevé selon préjudice)

**Formule FR origine NON_PROFESSIONNELLE (L.1226-2) :**
- `indemniteLegale = (¼ + ⅓ après 10 ans) × salaire × ancienneté` (SANS doublement)
- Pas d'indemnité de préavis (préavis non dû ni indemnisé)
- Obligation de reclassement existe mais pas sanctionnée par L.1226-15

**Messages :**
- Rappel délai : l'employeur ne peut licencier qu'après 1 mois sans reclassement (art. L.1226-11) — plus de salaire dû après ce mois.
- Obligation consultation CSE si pro (L.1226-10).
- Si reclassement non respecté → ouverture droit dommages-intérêts min 12 mois + indemnité nullité possible.

### Cas nominal BE (art. 34 Loi 03/07/1978)

**Inputs :** mêmes + `origineInaptitude` adapté (PROFESSIONNELLE_BE / NON_PROFESSIONNELLE_BE)

**Formule BE :**
- `indemniteLegale = 0` (force majeure médicale = rupture sans indemnité de rupture en principe)
- `damagesReclassement = 3 × salaire` indicatif si l'employeur n'a pas proposé de reclassement raisonnable (art. 31§1 Loi bien-être 04/08/1996)
- `indemniteCompensatricePreavis = 0`

**Messages BE :**
- Force majeure médicale ne donne pas droit à indemnité forfaitaire de rupture.
- Procédure de reclassement préalable (AR 28/05/2003, trajet de réintégration) — si non respectée : dommages-intérêts possibles.
- Prise en compte du Fonds des accidents du travail si origine professionnelle.

### Cas d'erreur
| Situation | Code HTTP |
|---|---|
| salaire ≤ 0 | 400 |
| ancienneté < 0 | 400 |
| origineInaptitude FR sur workspace BE (inverse) | 400 |
| origineInaptitude inconnue | 400 |
| Dossier autre que DROIT_DU_TRAVAIL | 400 |
| Workspace étranger | 404 |

---

## Contrat API (figé pour parallélisation)

### POST `/api/v1/case-files/{caseFileId}/inaptitude`

**Request :**
```json
{
  "salaireMensuelReference": 2500.00,
  "ancienneteAnnees": 8,
  "origineInaptitude": "PROFESSIONNELLE",
  "reclassementRespecte": true,
  "avisMedecinTravailDate": "2026-03-15"
}
```

**Response :**
```json
{
  "caseFileId": "uuid",
  "salaireMensuelReference": 2500.00,
  "ancienneteAnnees": 8,
  "origineInaptitude": "PROFESSIONNELLE",
  "reclassementRespecte": true,
  "avisMedecinTravailDate": "2026-03-15",
  "country": "FRANCE",
  "indemniteLegale": 10000.00,
  "indemniteCompensatricePreavis": 5000.00,
  "damagesReclassement": 0.00,
  "total": 15000.00,
  "formule": "…",
  "baseJuridique": "Art. L.1226-10 Code du travail",
  "messages": ["…"]
}
```

---

## Enum

**FR :**
- `PROFESSIONNELLE` (L.1226-10)
- `NON_PROFESSIONNELLE` (L.1226-2)

**BE :**
- `PROFESSIONNELLE_BE` (art. 34 Loi 03/07/1978 + force majeure médicale AT/MP)
- `NON_PROFESSIONNELLE_BE` (art. 34 Loi 03/07/1978 + maladie ordinaire prolongée)

---

## Architecture

Pattern F-DT-11 (calculator + entity + repository + service + controller + migration + 2 règles visibility ALWAYS_ON FR+BE).

### Composants créés
- `InaptitudeCalculator.java`
- `InaptitudeAnalysis.java` (entity avec colonnes ancienneteAnnees, salaireMensuelReference, origineInaptitude, reclassementRespecte, avisMedecinTravailDate nullable, country, result_data)
- `InaptitudeRepository.java`
- `InaptitudeRequest/Response/Result.java`
- `InaptitudeService.java`
- `InaptitudeController.java`
- Migration `113-create-inaptitude-analyses.xml` + UUIDs visibility `f1a04001-0000-0000-0000-ee0000000151` (FR) + `f1a04001-0000-0000-0000-ee0000000152` (BE), tool_id `F-DT-15-inaptitude`, priority 54.

## Plan de test

### Tests unitaires
- 4 origines acceptées (2 FR + 2 BE)
- Formule FR pro : doublement vérifié, préavis indemnisé
- Formule FR non-pro : pas de doublement, pas de préavis
- Formule BE pro : pas d'indemnité principale, reclassement insuffisant → dommages
- Cross-country : origineFR sur BE et inverse → IllegalArgumentException
- Salaire ≤ 0, ancienneté < 0, origine inconnue → IllegalArgumentException
- Reclassement non respecté (FR pro) → damages 12 mois
- Indemnité légale calcul selon ancienneté (pivot 10 ans)

### Tests intégration
- POST nominal FR + BE
- POST cross-country → 400
- POST dossier immigration → 400
- Workspace isolation → 404
- Upsert
- GET idempotent

---

## Impact par domaine

DROIT_DU_TRAVAIL FR + BE. Immigration / Famille N/A.

## Parité niveau ≥5

Niveau 3 (calculateur). Parité N/A.

## Critères d'acceptation

- [ ] Migration 113 table + 2 règles visibility.
- [ ] Calculator 4 origines × tests complets.
- [ ] Doublement FR pro vérifié (L.1226-14).
- [ ] Préavis FR pro vérifié (indemnisé).
- [ ] Damages reclassement FR pro si non respect (12 mois min).
- [ ] BE : flux séparé, pas d'indemnité principale mais damages reclassement possibles.
- [ ] Isolation workspace + gate domaine + cross-country rejet.
- [ ] +14 UT + 10 IT verts.

---

## Notes

- **Préavis FR** : simplifié à `2 mois` par défaut (régime standard CDI, art. L.1234-1). En toute rigueur, dépend de la CCN. Le calculator accepte un override `preavisMois` non-obligatoire (default 2).
- **Calcul indemnité légale** : utiliser la formule R.1234-2 (¼ + ⅓ après 10 ans) identique au calculator existant `RuptureConvIndemniteCalculator` (peut être extrait en util commun dans une SF ultérieure — pour cette SF, dupliquer le code).
- **BE** : la logique est simplifiée — il n'y a pas d'indemnité forfaitaire. Les messages orientent l'avocat vers les autres recours (AT/MP si origine pro, trajet de réintégration si manquement employeur).
