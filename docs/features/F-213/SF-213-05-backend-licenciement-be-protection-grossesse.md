# Mini-spec — F-213 / SF-213-05-backend Outil protection grossesse BE — validité + indemnité 6 mois

## Identifiant

`F-213 / SF-213-05-backend`

## Feature parente

`F-213` — P2 Travail BE — ~10 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-213-05-backend-licenciement-be-protection-grossesse`

---

## Objectif

Analyseur de la validité d'un licenciement pendant la grossesse ou la maternité (**Loi 16/03/1971 art. 40**) et calculateur de l'indemnité forfaitaire **6 mois de rémunération brute** + dommages supplémentaires prouvés. **BELGIQUE UNIQUEMENT** — pas d'équivalent direct FR (FR a une protection similaire mais l'indemnité et la durée de protection diffèrent).

---

## Source juridique BE

- **Loi du 16 mars 1971** sur le travail, **art. 40** :
  - Protection contre le licenciement : **du début de la grossesse** jusqu'à **1 mois après la fin du congé de maternité** (soit 3 mois après l'accouchement env.).
  - Interdiction absolue de licenciement sauf en cas de **motif étranger à la grossesse** — la preuve incombe à l'employeur.
  - Si le motif est lié à la grossesse : licenciement **nul**.
  - **Indemnité forfaitaire = 6 mois de rémunération brute** + réparation des dommages prouvés (cumulable).
- **Délai d'action** : prescription de droit commun (1 an post-rupture pour la créance ex-contrat, art. 15 Loi 03/07/1978).
- **Charge de la preuve inversée** : dès que la travailleuse a notifié sa grossesse par écrit et est licenciée dans les 10 semaines, il est présumé que le licenciement est lié à la grossesse — l'employeur doit prouver le contraire.

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-protection-grossesse`

Inputs (body) :
- `dateDebutGrossesse` (ISO date) — date de début de grossesse (ou de notification à l'employeur), obligatoire.
- `dateAccouchement` (ISO date, optionnel) — si accouchement déjà eu lieu.
- `dateCongeMaterniteDebut` (ISO date, optionnel).
- `dateCongeMaterniteFinale` (ISO date, optionnel).
- `dateLicenciement` (ISO date) — obligatoire.
- `grossesseNotifieeParEcrit` (boolean, défaut false) — si la travailleuse a notifié par écrit.
- `remunerationMensuelleBrute` (BigDecimal, €) — obligatoire pour calcul indemnité.
- `motifInvoqueParEmployeur` (String, optionnel) — motif allégué par l'employeur.

Logique (`LicenciementBeProtectionGrossesseValidator`) :

**Phase de protection :**
1. `dateDebutProtection = dateDebutGrossesse`.
2. `dateFinProtection` :
   - Si `dateCongeMaterniteFinale` renseignée : `dateCongeMaterniteFinale + 1 mois`.
   - Sinon si `dateAccouchement` renseignée : `dateAccouchement + 3 mois` (approximatif — congé maternité standard BE = 15 semaines).
   - Sinon : `NULL` → avertissement « Date fin protection indéterminée ».
3. `licenciementDansLaPeriodeProtegee = dateLicenciement ∈ [dateDebutProtection, dateFinProtection]`.

**Verdict :**

| Condition | Verdict |
|---|---|
| `!licenciementDansLaPeriodeProtegee` | `HORS_PERIODE_PROTECTION` |
| `licenciementDansLaPeriodeProtegee` && `grossesseNotifieeParEcrit` && `dateLicenciement ≤ dateDebutGrossesse + 10 sem` | `PROTECTION_PRESUMEE` (charge inversée) |
| `licenciementDansLaPeriodeProtegee` && !grossesse notifiée | `PROTECTION_APPLICABLE_NON_NOTIFIEE` |
| Default période protégée | `PROTECTION_APPLICABLE` |

**Calcul indemnité :**
- Si verdict ≠ `HORS_PERIODE_PROTECTION` :
  - `indemniteForfaitaire = remunerationMensuelleBrute × 6`.
  - `baseJuridique = "Loi 16/03/1971 art. 40"`.

Output (`LicenciementBeProtectionGrossesseResponse`) :
```json
{
  "verdict": "PROTECTION_APPLICABLE" | "PROTECTION_PRESUMEE" | "HORS_PERIODE_PROTECTION" | "PROTECTION_APPLICABLE_NON_NOTIFIEE",
  "licenciementDansLaPeriodeProtegee": true,
  "dateDebutProtection": "2026-01-15",
  "dateFinProtection": "2026-09-01",
  "indemniteForfaitaire": 18000.00,
  "chargePreuveEmployeur": true,
  "baseJuridique": "Loi 16/03/1971 art. 40",
  "avertissement": null
}
```

Persistance : `licenciement_be_protection_grossesse_analyses` — unique sur `case_file_id`.

`GET` → dernière analyse ou 404.

### Cas d'erreur

| Situation | Code | Comportement |
|---|---|---|
| `workspaceCountry !== 'BELGIQUE'` | 404 | Isolation |
| `dateLicenciement` avant `dateDebutGrossesse` | 400 | « Date incohérente » |
| `remunerationMensuelleBrute` ≤ 0 | 400 | Invalide |

---

## Champs IA à extraire — BELGIQUE UNIQUEMENT

| Champ | Type | Champ `TravailExtractedData` BE | Notes |
|---|---|---|---|
| `dateDebutGrossesse` | date | `dateDebutGrossesse` — **BELGIQUE UNIQUEMENT** | Extrait certificats médicaux |
| `dateAccouchement` | date | `dateAccouchement` — **BELGIQUE UNIQUEMENT** | |
| `dateLicenciement` | date | `dateRuptureContrat` (SF-207-01) | Réutilisation |
| `grossesseNotifieeParEcrit` | boolean | `grossesseNotifieeParEcrit` — **BELGIQUE UNIQUEMENT** | Extrait courrier de notification |
| `remunerationMensuelleBrute` | BigDecimal | dérivé `salaireBrutAnnuel / 12` | |

`critereCode` : `BE_GROSSESSE_DATE_DEBUT`, `BE_GROSSESSE_NOTIFICATION_ECRITE`, `BE_GROSSESSE_DATE_LICENCIEMENT`.

---

## Critères d'acceptation

- [ ] Licenciement dans période protégée → `PROTECTION_APPLICABLE` + indemnité 6 mois.
- [ ] Présomption inversée si notification écrite + licenciement ≤ 10 semaines après début.
- [ ] Licenciement hors période → `HORS_PERIODE_PROTECTION` (pas d'indemnité forfaitaire calculée).
- [ ] Workspace France → 404.
- [ ] `CritereCodeIntegrityIT` vert.

---

## Périmètre

### Hors scope

- Frontend — SF-213-05b.
- **Maternité adoptante** — régime distinct, hors scope V1.
- Congé de paternité (20 jours) — outil distinct P3 (`conge-paternite-naissance-be`).
- Dommages supplémentaires prouvés — indiqués dans `avertissement` mais non calculés automatiquement (à la charge de l'avocat).

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-protection-grossesse` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-protection-grossesse` | OIDC | MEMBER |

### Tables

`licenciement_be_protection_grossesse_analyses` — unique `case_file_id`.

### Composants backend

- `LicenciementBeProtectionGrossesse{Analysis,Repository,Request,Result,Response,Service,Validator,Controller}.java`
- Extension `TravailExtractedData` + `LegalDomainPromptBuilder` BE
- Migration `XXX-create-licenciement-be-protection-grossesse-analyses.xml`

---

## Plan de test

### Unitaires

- [ ] Licenciement à J+2 debut grossesse → `PROTECTION_APPLICABLE`.
- [ ] Licenciement 1 mois après fin congé maternité exactement → `HORS_PERIODE_PROTECTION`.
- [ ] Licenciement à J+9 semaines début grossesse, notification écrite → `PROTECTION_PRESUMEE`.
- [ ] Indemnité 6 mois = 3 000 € × 6 = 18 000 €.

### Intégration

- [ ] `POST` BE → 200, `POST` FR → 404.
- [ ] `GET` après POST → 200.

---

## Dépendances

- `dateRuptureContrat` depuis SF-207-01.
