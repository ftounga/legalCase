# Mini-spec — F-207 / SF-207-05-backend Outil référé tribunal du travail BE

## Identifiant

`F-207 / SF-207-05-backend` · Statut : `ready` · Date : 2026-05-20 · Branche : `feat/SF-207-05-backend-refere-tribunal-travail-be`

## Cadrages amont

Étape 0 / 0 bis F-207 livrées #1119. Pattern source : `ReferePrudhomal*` (F-DT-34 FR, jumeau structurel) + pattern BE workspace gate (SF-207-01).

## Objectif

Analyseur d'**éligibilité au référé** devant le président du tribunal du travail BE (Code Judiciaire **art. 584**) — vérifie les conditions d'urgence + génère le squelette d'une requête en référé. Cas type : harcèlement persistant, salaire impayé, modification unilatérale du contrat. Outil BE-only.

## Contrat API

`POST /api/v1/case-files/{caseFileId}/decision-tools/refere-tribunal-travail-be`

Inputs (`RefereTribunalTravailBeRequest`) :
```json
{
  "motifUrgence": "HARCELEMENT" | "SALAIRE_IMPAYE" | "MODIFICATION_UNILATERALE" | "AUTRE",
  "motifUrgenceDescription": "texte libre décrivant l'urgence",
  "dateFaitGenerateur": "2026-05-15",
  "dateDemarcheAmiable": "2026-05-16",                 // optionnel : démarche amiable préalable
  "preuveUrgenceJointe": true,
  "mesureProvisoireDemandee": "Cessation immédiate des actes de harcèlement",
  "perilEnDemeure": true,                              // boolean
  "competenceTerritorialeIdentifiee": true             // boolean — juridiction du domicile salarié
}
```

Réponse 200 :
```json
{
  "verdict": "REFERE_ELIGIBLE" | "REFERE_INCERTAIN" | "REFERE_NON_ELIGIBLE",
  "conditionsNonRemplies": ["PEUVE_URGENCE_MANQUANTE", "COMPETENCE_NON_IDENTIFIEE"],
  "scoreConditions": 4,                                 // sur 5
  "requeteSquelette": "REQUÊTE EN RÉFÉRÉ\n\nÀ MONSIEUR/MADAME LE PRÉSIDENT DU TRIBUNAL DU TRAVAIL DE … …",
  "baseJuridique": "Code Judiciaire art. 584 (référé) ; CJ art. 627 (compétence territoriale) ; Loi 03/07/1978",
  "etapeSuivante": "DEPOSER_REQUETE" | "RENFORCER_DOSSIER" | "ALTERNATIVE_PROCEDURE_FOND"
}
```

Logique :
- 5 conditions évaluées (booléens dérivés) :
  1. `URGENCE_QUALIFIABLE` — `motifUrgence != AUTRE` OU `motifUrgenceDescription.length > 30` (urgence circonstanciée).
  2. `PEUVE_URGENCE_JOINTE` — `preuveUrgenceJointe == true`.
  3. `PERIL_EN_DEMEURE` — `perilEnDemeure == true`.
  4. `MESURE_PROVISOIRE_PRECISE` — `mesureProvisoireDemandee.length > 10`.
  5. `COMPETENCE_IDENTIFIEE` — `competenceTerritorialeIdentifiee == true`.
- `scoreConditions` = nb de booléens true (0-5).
- Verdict :
  - `REFERE_ELIGIBLE` si score = 5.
  - `REFERE_INCERTAIN` si 3 ≤ score ≤ 4.
  - `REFERE_NON_ELIGIBLE` si score ≤ 2.
- `etapeSuivante` :
  - `DEPOSER_REQUETE` si ELIGIBLE.
  - `RENFORCER_DOSSIER` si INCERTAIN.
  - `ALTERNATIVE_PROCEDURE_FOND` si NON_ELIGIBLE.
- `requeteSquelette` : template texte (en-tête tribunal du travail, exposé des faits, mesures provisoires demandées, dispositif). Généré pour ELIGIBLE et INCERTAIN uniquement (pas pour NON_ELIGIBLE).

`GET` même path : dernière analyse ou 404.

## Cas d'erreur

| Situation | Code |
|---|---|
| `workspaceCountry !== BELGIQUE` | 404 |
| `caseFileId` autre workspace | 404 |
| `motifUrgence` manquant ou invalide | 400 |
| `dateFaitGenerateur` futur | 400 |
| `dateDemarcheAmiable < dateFaitGenerateur` | 400 |

## Composants à créer (pattern checklist BE — `C4OnemChecklist*` + `ReferePrudhomal*` FR)

Sous `backend/src/main/java/fr/ailegalcase/casefile/` :
- `RefereTribunalTravailBeAnalysis.java`
- `RefereTribunalTravailBeRepository.java`
- `RefereTribunalTravailBeMotifUrgence.java` — enum (4 valeurs).
- `RefereTribunalTravailBeCondition.java` — enum 5 conditions vérifiées.
- `RefereTribunalTravailBeRequest.java` (Bean Validation : `@NotNull motifUrgence`, `@NotBlank motifUrgenceDescription`, `@NotNull dateFaitGenerateur`, `@NotNull perilEnDemeure`, `@NotNull preuveUrgenceJointe`, `@NotNull competenceTerritorialeIdentifiee`, `@NotBlank mesureProvisoireDemandee`)
- `RefereTribunalTravailBeResult.java` (record + enums `Verdict` 3 valeurs, `EtapeSuivante` 3 valeurs, `scoreConditions`, `conditionsNonRemplies` List).
- `RefereTribunalTravailBeResponse.java`
- `RefereTribunalTravailBeCalculator.java` (5 vérifications + verdict prioritaire + génération squelette texte).
- `RefereTribunalTravailBeService.java` (gate `BELGIQUE`, validation, persistance).
- `RefereTribunalTravailBeController.java` (POST + GET).

Migration `XXX-create-refere-tribunal-travail-be-analyses.xml` (prochain après 259). Table standard. Rollback.

Extensions :
- `LegalDomainPromptBuilder` branche BE Travail : ajout 4 champs IA (`motifUrgenceDetecte` String, `dateFaitGenerateurUrgence` String, `urgenceProcedurale` boolean — peut déjà exister via F-166, `perilImmediatPresume` boolean dérivé). Émission `critereCode` `BE_REFERE_BE_MOTIF_URGENCE`, `BE_REFERE_BE_DATE_FAIT_GENERATEUR`, `BE_REFERE_BE_PERIL`.
- `CaseAnalysisResponse.TravailExtractedData` : ajout ces champs (vérifier qu'`urgenceProcedurale` n'est pas déjà présent — F-166 SF-166-01 a livré certains flags BE).

## Critères d'acceptation

- [ ] Score 5 → `REFERE_ELIGIBLE` + squelette présent.
- [ ] Score 3-4 → `REFERE_INCERTAIN` + squelette présent + `conditionsNonRemplies` listée.
- [ ] Score ≤ 2 → `REFERE_NON_ELIGIBLE` + squelette null + `etapeSuivante = ALTERNATIVE_PROCEDURE_FOND`.
- [ ] `motifUrgence == AUTRE` + description ≤ 30 caractères → condition `URGENCE_QUALIFIABLE` non remplie.
- [ ] `mesureProvisoireDemandee` ≤ 10 caractères → condition `MESURE_PROVISOIRE_PRECISE` non remplie.
- [ ] Workspace FR → 404 ; autre workspace → 404.
- [ ] Bean Validation manquante → 400.
- [ ] `GET` après `POST` → 200.
- [ ] `critereCode` BE_REFERE_BE_* émis ; `CritereCodeIntegrityIT` vert.

## Plan de test

`RefereTribunalTravailBeCalculatorTest` (10+ tests : 3 verdicts × bornes seuil, conditions individuelles cassées, squelette présence/absence).
`RefereTribunalTravailBeControllerIT` (5+ tests : BE OK, FR 404, autre workspace 404, validation 400, GET 404).

## Hors scope

- Frontend (SF-207-05b).
- Action en cessation (loi 27/05/1899) — autre procédure d'urgence, hors scope.
- Génération PDF/Word de la requête.

## Dépendances

- Pattern `C4OnemChecklist*` (SF-207-02) + `ReferePrudhomal*` FR.
- SF-207-04 backend (#1142) — pattern le plus récent BE workspace gate.
