# Mini-spec — F-IM-08 / SF-IM-08-07 Référés administratifs combinés FR — BACKEND

## Identifiant
`F-IM-08 / SF-IM-08-07`

## Feature parente
`F-IM-08` — OQTF et contentieux éloignement (FR + BE)

## Statut `draft` · Date `2026-04-25` · Branche `feat/SF-IM-08-07-referes-admin-backend`

---

## Objectif

Outil unique combinant le **référé suspension (L.521-1 CJA)** et le **référé liberté (L.521-2 CJA)** pour l'avocat en contentieux administratif d'éloignement / refus de titre. Un seul écran, une seule analyse, un verdict de recommandation entre les deux voies. **Ferme F-IM-08 (8/8 SF)**.

**Justification du combiné** : les deux référés s'appuient sur des conditions partiellement croisées (urgence) et l'avocat doit comparer les chances de succès en parallèle pour choisir la voie la plus pertinente. Combiner réduit la friction décisionnelle. **Pas de double outil** — un seul calculateur applique deux scoring distincts (L.521-1 et L.521-2) puis un verdict consolidé.

---

## Comportement

### Règles juridiques

- **L.521-1 CJA (référé suspension)** : urgence + doutes sérieux sur la légalité = suspension de la décision. Audience sous **30 jours** (R.522-1 CJA).
- **L.521-2 CJA (référé liberté)** : atteinte grave et manifestement illégale à une liberté fondamentale + urgence = mesures conservatoires. Statué dans les **48h** (L.522-1 CJA).

### Scoring

#### L.521-1 (suspension)
- Base : `urgenceCaracterisee` + `doutesSerieuxLegalite`
  - Les 2 = **100**
  - 1 seul = **50**
  - Aucun = **0**
- Bonus : `preuvesUrgence` → 5 pts par preuve, max 30 (cap)
- Score final = min(100, base + bonus)

#### L.521-2 (liberté)
- Base : `atteinteLiberteFondamentale` + `urgenceCaracterisee`
  - Les 2 = **100**
  - 1 seul = **50**
  - Aucun = **0**
- Bonus : `preuvesUrgence` → 5 pts par preuve, max 30 (cap)
- Score final = min(100, base + bonus)

### Verdict

Logique :
1. Si `typeRefere == LES_DEUX` ET les deux conditions cumulatives OK → `LES_DEUX_CUMULES`
2. Sinon, si `scoreLiberte >= scoreSuspension + 15` → `REFERE_LIBERTE_PRIORITAIRE` (48h plus rapide)
3. Sinon, si `scoreSuspension > scoreLiberte` OU score équivalent → `REFERE_SUSPENSION_PRIORITAIRE`
4. Sinon, si `max(scoreSuspension, scoreLiberte) < 40` → `AUCUN_PROBABLE`

### Inputs

- `typeRefere` : enum `SUSPENSION` (L.521-1), `LIBERTE` (L.521-2), `LES_DEUX` (cumul)
- `decisionContestee` : enum `OQTF`, `RETRAIT_TITRE`, `REFUS_TITRE`, `IRTF`, `AUTRE_MESURE_ADMIN`
- `dateNotificationDecision` : LocalDate
- `urgenceCaracterisee` : boolean
- `atteinteLiberteFondamentale` : boolean
- `doutesSerieuxLegalite` : boolean
- `preuvesUrgence` : List<String> enum
  - `MENACE_VIE_PRIVEE`, `TRANSFERT_IMMINENT`, `IMPACT_SCOLARITE_ENFANTS`, `RUPTURE_SOINS_MEDICAUX`, `RISQUE_VIOLENCES_PAYS_ORIGINE`, `AUTRE`
- `demandeurDejaPrived` : boolean (déjà privé de liberté → centre de rétention)

### Outputs

- `scoreSuccessProbabiliteSuspension` : int (0-100)
- `scoreSuccessProbabiliteLiberte` : int (0-100)
- `verdictRecommandation` : enum
- `delaiJugeTaJoursL521_1` : int = 30
- `delaiJugeTaHeuresL521_2` : int = 48
- `conditionsCumulativesL521_1Ok` : boolean (urgence + doutes sérieux ?)
- `conditionsCumulativesL521_2Ok` : boolean (urgence + atteinte liberté fondamentale ?)
- `baseJuridique` : "Art. L.521-1 + L.521-2 CJA + jurisprudence CE"
- `formule` : texte synthétique
- `messages` : List<String>
- `country` : "FRANCE"

### Cas d'erreur

| Situation | HTTP |
|---|---|
| typeRefere null/inconnu | 400 |
| decisionContestee null/inconnu | 400 |
| dateNotificationDecision null ou future | 400 |
| preuveUrgence inconnue | 400 |
| Workspace BELGIQUE | 400 "Référés administratifs FR — procédure CJA non applicable en Belgique" |
| Dossier non DROIT_IMMIGRATION | 400 |
| Workspace étranger | 404 |

---

## Contrat API

### POST `/api/v1/case-files/{caseFileId}/referes-admin`

**Request :**
```json
{
  "typeRefere": "SUSPENSION",
  "decisionContestee": "OQTF",
  "dateNotificationDecision": "2026-04-15",
  "urgenceCaracterisee": true,
  "atteinteLiberteFondamentale": true,
  "doutesSerieuxLegalite": true,
  "preuvesUrgence": ["MENACE_VIE_PRIVEE", "TRANSFERT_IMMINENT"],
  "demandeurDejaPrived": false
}
```

**Response :**
```json
{
  "caseFileId": "uuid",
  "typeRefere": "SUSPENSION",
  "decisionContestee": "OQTF",
  "dateNotificationDecision": "2026-04-15",
  "urgenceCaracterisee": true,
  "atteinteLiberteFondamentale": true,
  "doutesSerieuxLegalite": true,
  "preuvesUrgence": ["MENACE_VIE_PRIVEE", "TRANSFERT_IMMINENT"],
  "demandeurDejaPrived": false,
  "scoreSuccessProbabiliteSuspension": 100,
  "scoreSuccessProbabiliteLiberte": 100,
  "verdictRecommandation": "REFERE_LIBERTE_PRIORITAIRE",
  "delaiJugeTaJoursL521_1": 30,
  "delaiJugeTaHeuresL521_2": 48,
  "conditionsCumulativesL521_1Ok": true,
  "conditionsCumulativesL521_2Ok": true,
  "baseJuridique": "Art. L.521-1 + L.521-2 CJA + jurisprudence CE",
  "formule": "...",
  "messages": ["L.521-2 statué dans les 48h (art. L.522-1 CJA)", "..."],
  "country": "FRANCE"
}
```

### GET `/api/v1/case-files/{caseFileId}/referes-admin`

Idem response, ou 404 si pas d'analyse persistée.

---

## Architecture

Pattern miroir SF-IM-08-01. Table `referes_admin_analyses` (migration **146**). Tool_id `F-IM-08-referes-admin-fr`. **1 règle visibility CONTEXTUAL** FR sur trigger `type_procedure_detectee = OQTF_AVEC_DELAI` (cohérent F-IA-04) — UUID `f1a04001-0000-0000-0000-ee0000000087`, priority 68.

### Composants
- `ReferesAdminCalculator.java`
- `ReferesAdminAnalysis.java` (entity)
- `ReferesAdminRepository.java`
- `ReferesAdminRequest/Response/Result.java`
- `ReferesAdminService.java` (gate DROIT_IMMIGRATION + country==FRANCE)
- `ReferesAdminController.java`
- Migration `146-create-referes-admin-analyses.xml`

### Entity
- id uuid PK
- case_file_id uuid UNIQUE FK
- type_refere varchar(20) NOT NULL
- decision_contestee varchar(40) NOT NULL
- date_notification_decision date NOT NULL
- urgence_caracterisee boolean NOT NULL
- atteinte_liberte_fondamentale boolean NOT NULL
- doutes_serieux_legalite boolean NOT NULL
- demandeur_deja_prived boolean NOT NULL
- preuves_urgence text NULL (JSON)
- country varchar(20) NOT NULL
- result_data text NOT NULL
- timestamps

### Tests
- `ReferesAdminCalculatorTest` (UT) — ≥14
- `ReferesAdminControllerIT` (IT) — ≥8

---

## Plan de test

### UT
- typeRefere SUSPENSION : score selon combinaisons (0/0/0=0, 1/0/1=50+bonus, 1/0/0=50, 1/1/1=100)
- typeRefere LIBERTE : score selon combinaisons
- typeRefere LES_DEUX : verdict LES_DEUX_CUMULES si conditions OK
- Verdict REFERE_LIBERTE_PRIORITAIRE quand scoreLiberte >= scoreSuspension + 15
- Verdict REFERE_SUSPENSION_PRIORITAIRE quand suspension dominante
- Verdict AUCUN_PROBABLE quand max < 40
- preuvesUrgence : bonus capped à 30
- Formule contient L.521-1 + L.521-2 et délais
- Validation : typeRefere null, decision null, date future, preuve inconnue

### IT
- POST FR nominal SUSPENSION → 200
- POST FR LES_DEUX → 200 avec verdict
- POST workspace BE → 400
- POST dossier DROIT_DU_TRAVAIL → 400
- POST workspace étranger → 404
- POST upsert (2x)
- GET après POST
- GET sans POST → 404

---

## Impact par domaine

DROIT_IMMIGRATION FR uniquement. Procédure CJA française. Belgique : équivalent suspension d'extrême urgence devant CCE = procédure juridiquement distincte (peut être ouverte plus tard si pertinent — non prévu V1).

## Parité niveau ≥5

Niveau 5 (scoring de probabilité de succès). **Parité immigration** : SF-IM-08-05 annexe 13 BE traite le contentieux éloignement BE. La suspension d'extrême urgence belge devant CCE n'est pas couverte V1 — à backlog si demande utilisateur.

## Analyse de cohérence transversale

- [x] **F-IM-08 (autres SF)** : SF-01 OQTF avec délai mentionne `referedDisponibles=[REFERE_SUSPENSION_L521_1, REFERE_LIBERTE_L521_2]` — cet outil est leur compagnon. Pas de duplication, OQTF calcule délais, SF-07 calcule chances de succès.
- [x] **F-IA-04** : règle visibility CONTEXTUAL ajoutée sur trigger OQTF_AVEC_DELAI — l'outil apparaît quand l'IA détecte cette procédure.

## Hors scope

- Référé mesure utile (L.521-3) → pas pertinent V1.
- Suspension extrême urgence BE → backlog si demandé.
- Frontend → SF-IM-08-08.

---

## Critères d'acceptation

- [ ] Calculator scoring suspension + liberté + verdict
- [ ] 5 verdicts couverts (SUSPENSION_PRIO, LIBERTE_PRIO, LES_DEUX_CUMULES, AUCUN_PROBABLE) (4 valeurs)
- [ ] Bonus preuves capped à 30
- [ ] Migration 146 + visibility rule UUID `f1a04001-0000-0000-0000-ee0000000087`, priority 68
- [ ] Gate country FRANCE strict
- [ ] Gate DROIT_IMMIGRATION
- [ ] Isolation workspace
- [ ] ≥14 UT + ≥8 IT verts
