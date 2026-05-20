# Mini-spec — F-213 / SF-213-03-backend Outil préavis statut unique BE — calculateur délais post-2014

## Identifiant

`F-213 / SF-213-03-backend`

## Feature parente

`F-213` — P2 Travail BE — ~10 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-213-03-backend-licenciement-be-statut-unique-preavis`

---

## Objectif

Calculateur de la durée du délai de préavis selon le **statut unique belge** (Loi du 26 décembre 2013) pour les **contrats signés ou les périodes d'ancienneté à compter du 01/01/2014**, avec calcul de l'indemnité compensatoire de préavis. **BELGIQUE UNIQUEMENT** — outil autonome distinct de `F-DT-09-comparateur-indemnites` (F-DT-09 compare plusieurs régimes ; cet outil produit une vue dédiée préavis statut unique avec date de fin de préavis et formule lisible).

---

## Source juridique BE

- **Loi du 26 décembre 2013** concernant l'introduction d'un statut unique entre ouvriers et employés : barème de préavis en semaines selon ancienneté (art. 37/1 et seq.).
- **Barème officiel statut unique** (tranches) :

| Ancienneté (années complètes) | Préavis (semaines) côté EMPLOYEUR |
|---|---|
| 0 (< 3 mois) | 2 semaines |
| 0-1 an | 4 semaines |
| 1-2 ans | 6 semaines |
| 2-3 ans | 7 semaines |
| 3-4 ans | 9 semaines |
| 4-5 ans | 12 semaines |
| 5-6 ans | 15 semaines |
| 6-7 ans | 18 semaines |
| 7-8 ans | 21 semaines |
| 8-9 ans | 24 semaines |
| 9-10 ans | 27 semaines |
| 10-11 ans | 30 semaines |
| 11-12 ans | 33 semaines |
| 12-13 ans | 36 semaines |
| 13-14 ans | 39 semaines |
| 14-15 ans | 42 semaines |
| 15-16 ans | 45 semaines |
| 16-17 ans | 48 semaines |
| 17-18 ans | 51 semaines |
| 18-19 ans | 54 semaines |
| 19-20 ans | 57 semaines |
| ≥ 20 ans | 62 semaines |

Préavis côté SALARIÉ (démission) : plafonné à 13 semaines maximum (tranches réduites — voir mini-spec `demission-be-validite` P2+, hors F-213).

- **Indemnité compensatoire de préavis** = rémunération en cours × nombre de semaines préavis non presté (Loi 03/07/1978 art. 39, adapté statut unique).
- **Clause d'ancienneté double (clause Claeys + statut unique)** : pour les contrats antérieurs au 01/01/2014, l'ancienneté est scindée — partie pré-2014 calculée selon Claeys (voir SF-213-04), partie post-2014 selon statut unique. F-213-03 couvre **uniquement la partie post-2014** ou les contrats 100 % post-2014.
- **À vérifier par avocat BE** : le barème ci-dessus est issu des connaissances du modèle — confirmer les chiffres exacts avec la loi du 26/12/2013 version consolidée.

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-statut-unique-preavis`

Inputs (body) :
- `ancienneteAnnees` (int) — ancienneté totale en années complètes post-2014 (ou totale si contrat 100 % post-2014), obligatoire.
- `ancienneteMoisSupplementaires` (int 0-11) — mois supplémentaires au-delà des années complètes, optionnel (défaut 0).
- `salaireHebdomadaireBrut` (BigDecimal, €) — rémunération hebdomadaire brute, obligatoire (pour calcul indemnité).
- `dateNotificationLicenciement` (ISO date) — obligatoire pour calculer la date de fin de préavis.
- `partieStatutUniqueSeulement` (boolean, défaut true) — si false, signale un contrat pré-2014 partiellement (avertissement affiché).

Logique (`LicenciementBeStatutUniquePreavisCalculator`) :
1. Résoudre `ancienneteAnnees` + mois → tranche barème → `dureePreavisEnSemaines`.
2. `dateFinPreavis = dateNotificationLicenciement + dureePreavisEnSemaines * 7 jours` (arrondi au lundi suivant si besoin — à confirmer avec avocat BE pour règle exacte).
3. `indemnitéCompensatoire = salaireHebdomadaireBrut * dureePreavisEnSemaines`.
4. Si `!partieStatutUniqueSeulement` : émettre `avertissement = "Contrat antérieur au 01/01/2014 : préavis pré-2014 à calculer séparément via l'outil Formule Claeys"`.

Output (`LicenciementBeStatutUniquePreavisResponse`) :
```json
{
  "dureePreavisEnSemaines": 27,
  "dateFinPreavis": "2025-11-19",
  "indemnitéCompensatoire": 13500.00,
  "formuleCalcul": "9 ans ancienneté → 27 semaines (loi 26/12/2013) ; ICP = 500 € × 27 = 13 500 €",
  "baseJuridique": "Loi 26/12/2013 — statut unique — barème art. 37/1",
  "avertissement": null
}
```

Persistance : table `licenciement_be_statut_unique_preavis_analyses` — unique sur `case_file_id`.

`GET` du même path → dernière analyse ou 404.

### Cas d'erreur

| Situation | Code | Comportement |
|---|---|---|
| `workspaceCountry !== 'BELGIQUE'` | 404 | Isolation BE-only |
| `ancienneteAnnees` < 0 | 400 | « Ancienneté invalide » |
| `salaireHebdomadaireBrut` ≤ 0 | 400 | « Salaire invalide » |
| `dateNotificationLicenciement` dans le futur lointain (> 1 an) | 400 | Warning ou rejet selon paramétrage |

---

## Champs IA à extraire — BELGIQUE UNIQUEMENT

| Champ | Type | Champ `TravailExtractedData` BE | Notes |
|---|---|---|---|
| `ancienneteAnnees` | int | `ancienneteAnnees` (existant ou à ajouter) | Extrait contrat ou bulletins paie |
| `salaireHebdomadaireBrut` | BigDecimal | `salaireHebdomadaireBrut` — **BELGIQUE UNIQUEMENT** | = `salaireBrutMensuel / (365.25/7/12)` si mensuel |
| `dateNotificationLicenciement` | date | `dateRuptureContrat` (SF-207-01) | Approx : date rupture |
| `partieStatutUniqueSeulement` | boolean | dérivé : `dateContrat >= 2014-01-01` → true | |

`critereCode` : `BE_PREAVIS_ANCIENNETE`, `BE_PREAVIS_SALAIRE_HEBDO`, `BE_PREAVIS_DATE_NOTIFICATION`.

---

## Critères d'acceptation

- [ ] 9 ans ancienneté → 27 semaines.
- [ ] ≥ 20 ans → 62 semaines.
- [ ] Indemnité compensatoire = salaire × semaines.
- [ ] Date fin préavis calculée correctement.
- [ ] Avertissement si contrat pré-2014.
- [ ] Workspace France → 404.
- [ ] `CritereCodeIntegrityIT` vert.

---

## Périmètre

### Hors scope

- Frontend — SF-213-03b.
- Préavis **salarié** (démission) — outil distinct (`demission-be-validite`, P2+, hors F-213).
- **Formule Claeys** (pré-2014) — SF-213-04.
- Cas particuliers : secteurs avec CCT sectorielle plus favorable que la loi (P4).

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-statut-unique-preavis` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-statut-unique-preavis` | OIDC | MEMBER |

### Tables

| Table | Opération | Notes |
|---|---|---|
| `licenciement_be_statut_unique_preavis_analyses` | INSERT/UPDATE/SELECT | Unique `case_file_id` |

### Composants backend

- `LicenciementBeStatutUniquePreavisAnalysis.java`, `…Repository.java`
- `LicenciementBeStatutUniquePreavisRequest.java`, `…Result.java`, `…Response.java`
- `LicenciementBeStatutUniquePreavisService.java`, `…Calculator.java`, `…Controller.java`
- Constante `BAREME_STATUT_UNIQUE` (table des tranches en Java) dans le Calculator
- Extension `TravailExtractedData` : `salaireHebdomadaireBrut`, `ancienneteAnnees` si absents
- Migration `XXX-create-licenciement-be-statut-unique-preavis-analyses.xml`

---

## Plan de test

### Unitaires (`LicenciementBeStatutUniquePreavisCalculatorTest`)

- [ ] Tranches limites : 0 an (2 sem), 1 an (4 sem), 10 ans (30 sem), 19 ans (57 sem), 20 ans (62 sem).
- [ ] Ancienneté 9 ans 6 mois → tranche 9-10 ans = 27 sem.
- [ ] Indemnité 500 € × 27 sem = 13 500 €.
- [ ] Date fin préavis correcte pour une date de notification donnée.

### Intégration

- [ ] `POST` BE → 200, `POST` FR → 404.
- [ ] `GET` après POST → 200, sans → 404.

---

## Dépendances

- `dateRuptureContrat` disponible depuis SF-207-01.
