# Mini-spec — F-DT-19 / SF-DT-19-01 Calculateur heures supplémentaires — BACKEND

## Identifiant
`F-DT-19 / SF-DT-19-01`

## Feature parente
`F-DT-19` — Calculateur heures supplémentaires (critique 🔴)

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-DT-19-01-heures-sup-backend`

---

## Objectif

Outil décisionnel calculant le rappel de salaire dû au salarié pour heures supplémentaires effectuées mais non payées, FR + BE. Chef de demande très fréquent en prud'hommes / tribunal du travail.

---

## Comportement

### Formule FR (art. L.3121-28 à L.3121-39, D.3121-24)
- **Taux majoration** : 25 % pour les 8 premières HS par semaine (35h→43h), 50 % au-delà de la 43e heure — sauf CCN plus favorable (min 10 %).
- **Contingent annuel** : 220h par défaut (D.3121-24), au-delà → **contrepartie obligatoire en repos** (COR) = 100 % de chaque HS.
- Les CCN peuvent modifier le contingent et les taux.

### Formule BE (art. 29 Loi 16/03/1971)
- **Sursalaire 50 %** pour HS en semaine (> 40h/semaine ou 8h/jour).
- **Sursalaire 100 %** pour HS le dimanche ou un jour férié.
- **Repos compensatoire** obligatoire en sus.

### Inputs communs

- `tauxHoraireBrut` : BigDecimal > 0 (€/h)
- `country` : dérivé du workspace

### Inputs FR

- `heuresSupDeclarees25pct` : int ≥ 0 (HS à 25 % — les 8 premières/semaine)
- `heuresSupDeclarees50pct` : int ≥ 0 (HS à 50 % — au-delà)
- `heuresHorsContingent` : int ≥ 0 (HS au-delà du contingent annuel de 220h, déclenchent COR)
- `tauxMajoration25` : BigDecimal default 25 (range 10-50 — CCN peut adapter)
- `tauxMajoration50` : BigDecimal default 50

### Inputs BE

- `heuresSupSemaine` : int ≥ 0 (HS hebdo à 50 %)
- `heuresDimancheJoursFeries` : int ≥ 0 (HS à 100 %)

### Outputs

```json
{
  "rappelMajoration25pct": 0.00,
  "rappelMajoration50pct": 0.00,
  "rappelMajoration100pct": 0.00,
  "rappelTotal": 0.00,
  "reposCompensateurHeuresDues": 0.00,
  "formule": "…",
  "baseJuridique": "…",
  "messages": […]
}
```

Pour FR : `rappelTotal = h25 × taux × (tauxMajo25/100) + h50 × taux × (tauxMajo50/100)`. `reposCompensateurHeuresDues = heuresHorsContingent × 1.0` (100 % du temps HS au-delà du contingent).

Pour BE : `rappelTotal = heuresSemaine × taux × 0.5 + heuresDimanche × taux × 1.0`. `reposCompensateurHeuresDues = heuresSemaine + heuresDimanche` (repos compensatoire obligatoire).

### Cas d'erreur
- tauxHoraireBrut ≤ 0 → 400
- Toutes heures = 0 → 400 "Au moins une heure supplémentaire requise"
- heures négatives → 400
- tauxMajoration FR hors [10, 50] → 400
- Country invalide → 400
- Dossier autre domaine → 400
- Workspace étranger → 404

---

## Contrat API

### POST `/api/v1/case-files/{caseFileId}/heures-sup`

**Request FR :**
```json
{
  "tauxHoraireBrut": 15.00,
  "heuresSupDeclarees25pct": 40,
  "heuresSupDeclarees50pct": 10,
  "heuresHorsContingent": 0,
  "tauxMajoration25": 25,
  "tauxMajoration50": 50
}
```

**Request BE :**
```json
{
  "tauxHoraireBrut": 15.00,
  "heuresSupSemaine": 30,
  "heuresDimancheJoursFeries": 5
}
```

**Response :** voir outputs ci-dessus + champs echoes du request + country.

Le service distingue FR/BE en regardant country et choisit la validation + le calcul adaptés.

---

## Architecture

Pattern standard. Table `heures_sup_analyses` (migration 114). Tool_id `F-DT-19-heures-sup`. 2 règles visibility ALWAYS_ON FR + BE (UUIDs `f1a04001-...-ee0000000191` + `f1a04001-...-ee0000000192`, priority 55).

Entity colonnes : id, case_file_id UNIQUE, taux_horaire_brut numeric(8,2), heures_sup_25pct int, heures_sup_50pct int, heures_hors_contingent int, taux_majoration_25 numeric(5,2), taux_majoration_50 numeric(5,2), heures_sup_semaine_be int, heures_dimanche_jf_be int, country varchar(20), result_data text, timestamps. Valeurs par défaut 0 pour les int nullable.

### Composants à créer
`HeuresSupCalculator.java`, `HeuresSupAnalysis.java`, `HeuresSupRepository.java`, `HeuresSupRequest/Response/Result.java`, `HeuresSupService.java`, `HeuresSupController.java`, migration `114-create-heures-sup-analyses.xml`.

---

## Plan de test

### UT (`HeuresSupCalculatorTest`)
- FR : 40h@25% × 15€ = 150€
- FR : 10h@50% × 15€ = 75€
- FR : total 2 tranches
- FR : heuresHorsContingent → repos compensateur = heures × 1.0
- FR : tauxMajoration25 custom (ex 10%) → calcul adapté
- BE : 30h@50% × 15€ = 225€
- BE : 5h@100% × 15€ = 75€
- BE : repos compensatoire = somme heures
- Validation : tauxHoraireBrut ≤ 0 throws
- Validation : toutes heures 0 throws
- Validation : heures négatives throws
- Validation : tauxMajoration < 10 ou > 50 throws (FR)
- Validation : country incorrect throws

### IT (`HeuresSupControllerIT`)
- POST FR nominal
- POST BE nominal
- POST upsert
- POST dossier immigration → 400
- POST workspace étranger → 404
- GET après POST
- GET sans POST → 404

---

## Impact domaine métier

DROIT_DU_TRAVAIL FR + BE. Immigration/Famille N/A.

## Parité niveau ≥5

Niveau 3 (calculateur). Parité N/A.

## Critères

- [ ] FR tranches 25/50 % calculées séparément.
- [ ] FR COR si heuresHorsContingent > 0.
- [ ] BE sursalaire 50 % + 100 %.
- [ ] Validation inputs.
- [ ] Migration 114.
- [ ] 2 règles visibility.
- [ ] Endpoint POST + GET.
- [ ] Isolation workspace.
- [ ] +12 UT + 8 IT verts.

## Hors scope
- Détection IA des heures sup non payées depuis bulletins → SF ultérieure.
- Calcul prescription 3 ans L.3245-1 → affiché en message seulement.
- Taux d'imposition brut→net → hors scope.
