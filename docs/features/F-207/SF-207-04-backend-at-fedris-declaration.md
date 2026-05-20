# Mini-spec — F-207 / SF-207-04-backend Outil déclaration AT Fedris

## Identifiant

`F-207 / SF-207-04-backend` · Statut : `ready` · Date : 2026-05-20 · Branche : `feat/SF-207-04-backend-at-fedris-declaration`

## Cadrages amont

Étape 0 / 0 bis F-207 livrées #1119. Pattern source : `AtMp*` (F-DT-33 FR, jumeau structurel) + pattern BE workspace gate de `PrescriptionBeLitigeTravail*` (SF-207-01).

## Objectif

Calculateur du **délai de déclaration à Fedris** d'un accident du travail par l'employeur belge — **8 jours** à compter du moment où l'employeur a eu connaissance de l'accident (Loi du 10/04/1971 sur les accidents du travail, art. 62). Sanction si dépassement : préjudice indemnisation salarié + intérêts moratoires + responsabilité civile employeur. Outil BE-only.

## Contrat API

`POST /api/v1/case-files/{caseFileId}/decision-tools/at-fedris-declaration`

Inputs :
```json
{
  "dateAccident": "2026-05-10",                  // requis (date AT)
  "dateConnaissanceEmployeur": "2026-05-12",     // optionnel ; default = dateAccident
  "dateActionEnvisagee": "2026-05-20",           // optionnel ; default today
  "dateDeclarationEffectuee": null                // optionnel ; si renseigné, calcule a posteriori si délai respecté
}
```

Réponse 200 :
```json
{
  "verdict": "DELAI_OUVERT" | "DELAI_IMMINENT" | "DELAI_DEPASSE" | "DECLARATION_HORS_DELAI" | "DECLARATION_DANS_LES_TEMPS",
  "dateLimiteDeclaration": "2026-05-20",
  "joursRestants": 0,                            // négatif si dépassé
  "regleAppliquee": "8_JOURS_LOI_1971_ART_62",
  "baseJuridique": "Loi du 10 avril 1971 sur les accidents du travail, art. 62 ; AR 21/12/1971 art. 25",
  "formuleCalcul": "dateConnaissanceEmployeur (2026-05-12) + 8 jours = dateLimite (2026-05-20) ; aujourd'hui (2026-05-20) → joursRestants 0 → IMMINENT",
  "consequencesNonRespect": "Préjudice salarié (perte indemnisation) ; intérêts moratoires sur les indemnités dues ; responsabilité civile de l'employeur (art. 62 loi 1971)."
}
```

Logique :
- `dateLimite = (dateConnaissanceEmployeur ?? dateAccident) + 8 jours` (calendrier, Europe/Brussels).
- Mode **prospectif** (`dateDeclarationEffectuee=null`) :
  - `joursRestants = dateLimite - dateActionEnvisagee`.
  - `DELAI_OUVERT` si > 2 ; `DELAI_IMMINENT` si 0 ≤ joursRestants ≤ 2 ; `DELAI_DEPASSE` si < 0.
- Mode **rétrospectif** (`dateDeclarationEffectuee` renseigné) :
  - Si `dateDeclarationEffectuee ≤ dateLimite` → `DECLARATION_DANS_LES_TEMPS`.
  - Sinon → `DECLARATION_HORS_DELAI` (avec calcul de jours de retard).

`GET` même path : dernière analyse persistée ou 404.

## Cas d'erreur

| Situation | Code |
|---|---|
| `workspaceCountry !== BELGIQUE` | 404 |
| `caseFileId` autre workspace | 404 |
| `dateAccident` manquant ou futur | 400 |
| `dateConnaissanceEmployeur < dateAccident` | 400 |
| `dateDeclarationEffectuee < dateAccident` | 400 |

## Composants à créer (pattern `AtMp*` + pattern BE de SF-207-01)

Sous `backend/src/main/java/fr/ailegalcase/casefile/` :
- `AtFedrisDeclarationAnalysis.java` (entité JPA, unique sur `case_file_id`)
- `AtFedrisDeclarationRepository.java`
- `AtFedrisDeclarationRequest.java` (Bean Validation : `@NotNull dateAccident`)
- `AtFedrisDeclarationResult.java` (record avec enum `Verdict` 5 valeurs ; `dateLimite`, `joursRestants`, `regleAppliquee`, `baseJuridique`, `formuleCalcul`, `consequencesNonRespect`)
- `AtFedrisDeclarationResponse.java`
- `AtFedrisDeclarationCalculator.java` (modes prospectif + rétrospectif)
- `AtFedrisDeclarationService.java` (gate `workspaceCountry=BELGIQUE`, isolation workspace, validation, persistance JSON)
- `AtFedrisDeclarationController.java` (POST + GET)

Migration `XXX-create-at-fedris-declaration-analyses.xml` (prochain numéro après 257). Table standard `id`/`case_file_id` (FK CASCADE unique)/`result_data`/`created_at`/`updated_at`. Rollback `dropTable`.

Extensions :
- `LegalDomainPromptBuilder.java` — branche BE Travail : ajout `dateAccident`, `dateConnaissanceEmployeur` (Strings ISO) + émission `critereCode` `BE_AT_FEDRIS_DATE_ACCIDENT`, `BE_AT_FEDRIS_DATE_CONNAISSANCE_EMPLOYEUR`.
- `CaseAnalysisResponse.java` — `TravailExtractedData` : ajout 2 fields `dateAccident` (String), `dateConnaissanceAccidentEmployeur` (String). **Rétrocompat via Builder uniquement** (BuilderPatternEnforcementIT).

## Critères d'acceptation

- [ ] Mode prospectif — verdict OUVERT/IMMINENT/DEPASSE selon `joursRestants` (seuil 2 j).
- [ ] Mode rétrospectif — verdict `DECLARATION_DANS_LES_TEMPS` ou `DECLARATION_HORS_DELAI` selon `dateDeclarationEffectuee` vs `dateLimite`.
- [ ] `dateLimite = dateConnaissance (ou dateAccident) + 8 jours` calendaires Europe/Brussels.
- [ ] Workspace FR → 404 ; autre workspace → 404.
- [ ] `dateAccident` futur → 400 ; `dateConnaissance < dateAccident` → 400 ; `dateDeclarationEffectuee < dateAccident` → 400.
- [ ] `GET` après `POST` → 200 ; sans POST → 404.
- [ ] `critereCode` BE_AT_FEDRIS_* émis ; `CritereCodeIntegrityIT` vert.
- [ ] `TravailExtractedData` BE étendu sans casse Builder.

## Plan de test

`AtFedrisDeclarationCalculatorTest` (10+ tests) : 5 verdicts × 2 modes + bornes seuils (2 j inclus → IMMINENT) + edge cases (`dateConnaissance==dateAccident`, dépassement de 3 jours).

`AtFedrisDeclarationControllerIT` (5+ tests) : BE OK, FR 404, autre workspace 404, validation Bean 400, GET sans POST 404.

## Hors scope

- Frontend (SF-207-04b).
- Calcul des intérêts moratoires (autre outil potentiel).
- Reconnaissance maladie professionnelle Fedris (autre régime, `mp-fedris-reconnaissance`).

## Dépendances

- Pattern AtMp* (F-DT-33 FR) + pattern BE workspace gate (SF-207-01).
- Aucune dépendance directe sur les autres SF F-207.
