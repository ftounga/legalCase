# Mini-spec — F-213 / SF-213-08-backend Outil protection délégué syndical BE — indemnité 2-4 ans

## Identifiant

`F-213 / SF-213-08-backend`

## Feature parente

`F-213` — P2 Travail BE — ~10 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-213-08-backend-licenciement-be-protection-deleguee`

---

## Objectif

Analyseur de la validité d'un licenciement d'un délégué syndical ou candidat aux élections sociales, et calculateur de l'**indemnité forfaitaire** (2 ans rémunération minimum, jusqu'à 4 ans en cas de récidive ou circonstances aggravantes). **Loi du 19 mars 1991** et **CCT n° 5**. **BELGIQUE UNIQUEMENT** — mécanisme distinct du statut protégé FR.

---

## Source juridique BE

- **Loi du 19 mars 1991** relative à la protection contre le licenciement des représentants des travailleurs dans les entreprises avec CPPT et CE :
  - Délégués syndicaux élus + **candidats non élus** protégés pendant 2 ans après les élections.
  - Licenciement interdit sauf pour motif grave (art. 35 Loi 03/07/1978) OU raisons d'ordre économique ou technique reconnues par la juridiction compétente.
  - **Procédure de réintégration** : le délégué licencié peut demander la réintégration dans les 30 jours.
  - Si l'employeur refuse : **indemnité forfaitaire = 2 ans de rémunération brute** (délégué élu, ≥ 2 ans ancienneté).
  - **Récidive** ou circonstances aggravantes : 4 ans.
- **CCT n° 5 du 24 mai 1971** relative au statut des délégations syndicales : complète la loi de 1991 pour les délégués syndicaux non liés au CPPT/CE (représentants syndicaux non élus — leur protection varie selon la CCT sectorielle).
- **Candidats non élus** : protégés 2 ans après les élections sociales (cycle 4 ans : 2024, 2028…).
- **Délai de demande de réintégration** : 30 jours après le licenciement.
- **À vérifier par avocat BE** : barème exact selon ancienneté et niveau de mandat (titulaire vs suppléant, CE vs CPPT).

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-protection-deleguee`

Inputs (body) :
- `statutProtege` (enum) — `DELEGUE_SYNDICAL_ELU` | `CANDIDAT_NON_ELU` | `DELEGUE_CCT5` | `CONSEILLER_CPPT`.
- `ancienneteAnnees` (int) — ancienneté totale, obligatoire.
- `remunerationAnnuelleBrute` (BigDecimal, €) — obligatoire pour calcul indemnité.
- `dateLicenciement` (ISO date) — obligatoire.
- `dateElectionOuMandat` (ISO date) — date d'élection ou début de mandat, obligatoire.
- `demandeReintegrationDansTrente` (boolean, défaut false) — le délégué a-t-il demandé la réintégration dans les 30 jours ?
- `employeurRefuseReintegration` (boolean, défaut false) — requis si `demandeReintegrationDansTrente=true`.
- `circonstancesAggravantes` (boolean, défaut false) — récidive ou licenciement collectif de représentants.

Logique (`LicenciementBeProtectionDelegueeValidator`) :

**Période de protection :**
- Délégués élus : pendant le mandat + durée restante.
- Candidats non élus : 2 ans après l'élection.
- `dateFinProtection = max(dateElectionOuMandat + durée mandat, dateElectionOuMandat + 2 ans)`.
- `licenciementDansProtection = dateLicenciement ≤ dateFinProtection`.

**Verdict validité :**

| Condition | Verdict |
|---|---|
| `!licenciementDansProtection` | `HORS_PERIODE_PROTECTION` |
| `licenciementDansProtection` | `LICENCIEMENT_INTERDIT_SANS_PROCEDURE` |

**Calcul indemnité (si employeur refuse réintégration ou n'y est pas contraint) :**
- `annees = circonstancesAggravantes ? 4 : 2`
- `indemniteForfaitaire = remunerationAnnuelleBrute × annees`

**Délai réintégration :**
- `dateLimiteDemandeReintegration = dateLicenciement + 30 jours`
- `joursRestantsReintegration = dateLimiteDemandeReintegration - today`
- Si `joursRestantsReintegration ≤ 0` → `delaiReintegrationDepasse = true`

Output :
```json
{
  "verdict": "LICENCIEMENT_INTERDIT_SANS_PROCEDURE" | "HORS_PERIODE_PROTECTION",
  "licenciementDansProtection": true,
  "indemniteForfaitaire": 80000.00,
  "anneesForfait": 2,
  "dateLimiteDemandeReintegration": "2026-06-19",
  "joursRestantsReintegration": 30,
  "delaiReintegrationDepasse": false,
  "baseJuridique": "Loi 19/03/1991 ; CCT n°5 du 24/05/1971",
  "avertissement": null
}
```

Persistance : `licenciement_be_protection_deleguee_analyses` — unique sur `case_file_id`.

`GET` → dernière analyse ou 404.

### Cas d'erreur

| Situation | Code | Comportement |
|---|---|---|
| `workspaceCountry !== 'BELGIQUE'` | 404 | Isolation |
| `remunerationAnnuelleBrute` ≤ 0 | 400 | Invalide |
| `employeurRefuseReintegration=true` sans `demandeReintegrationDansTrente=true` | 400 | Incohérence |

---

## Champs IA à extraire — BELGIQUE UNIQUEMENT

| Champ | Type | Champ `TravailExtractedData` BE | Notes |
|---|---|---|---|
| `statutProtege` | enum | `positionProtegee` — **BELGIQUE UNIQUEMENT** | Extrait mandat syndical dans le dossier |
| `dateElectionOuMandat` | date | `dateMandatProtege` — **BELGIQUE UNIQUEMENT** | |
| `remunerationAnnuelleBrute` | BigDecimal | `salaireBrutAnnuel` (existant) | |
| `dateLicenciement` | date | `dateRuptureContrat` (SF-207-01) | |

`critereCode` : `BE_DELEGUE_STATUT`, `BE_DELEGUE_DATE_MANDAT`, `BE_DELEGUE_REMUNERATION`.

---

## Critères d'acceptation

- [ ] Licenciement dans période protection → `LICENCIEMENT_INTERDIT_SANS_PROCEDURE`.
- [ ] Indemnité 2 ans calculée (sans aggravantes), 4 ans (avec aggravantes).
- [ ] Délai réintégration 30 j calculé depuis dateLicenciement.
- [ ] Workspace France → 404.
- [ ] `CritereCodeIntegrityIT` vert.

---

## Périmètre

### Hors scope

- Frontend — SF-213-08b.
- **Membres du CE et CPPT** dans leur intégralité — focus délégués syndicaux V1 ; membres organes paritaires = P3.
- Procédure judiciaire de réintégration forcée — hors scope (orientation vers tribunal du travail mentionnée dans `avertissement`).

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-protection-deleguee` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-protection-deleguee` | OIDC | MEMBER |

### Tables

`licenciement_be_protection_deleguee_analyses` — unique `case_file_id`.

### Composants backend

- `LicenciementBeProtectionDeleguee{Analysis,Repository,Request,Result,Response,Service,Validator,Controller}.java`
- `LicenciementBeStatutProtegeEnum.java`
- Extension `TravailExtractedData` + `LegalDomainPromptBuilder` BE
- Migration `XXX-create-licenciement-be-protection-deleguee-analyses.xml`

---

## Plan de test

### Unitaires

- [ ] Délégué élu licencié dans mandat → `LICENCIEMENT_INTERDIT_SANS_PROCEDURE`.
- [ ] Indemnité 2 ans = 2 × rémunération.
- [ ] Circonstances aggravantes → 4 ans.
- [ ] Délai réintégration 30 j expiré → `delaiReintegrationDepasse=true`.
- [ ] Candidat non élu hors 2 ans → `HORS_PERIODE_PROTECTION`.

### Intégration

- [ ] `POST` BE → 200, `POST` FR → 404.

---

## Dépendances

- `dateRuptureContrat` depuis SF-207-01.
