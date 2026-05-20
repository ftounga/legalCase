# Mini-spec — F-207 / SF-207-03-backend Outil contestation C4 ONEM (calculateur délais double)

## Identifiant

`F-207 / SF-207-03-backend` · Statut : `ready` · Date : 2026-05-20 · Branche : `feat/SF-207-03-backend-contestation-c4-onem`

## Cadrages amont

Étape 0 / 0 bis F-207 livrées en #1119. Pas re-cadrés. Pattern à mirrorer : `ContestationAre*` (F-DT-35) — substance adaptée BE.

## Objectif

Calculateur des **deux délais successifs** de contestation d'une décision ONEM (exclusion / sanction) :
1. **Recours administratif** auprès du Directeur du Bureau du chômage — **1 mois** à compter de la notification (AR 25/11/1991 **art. 144**).
2. **Recours juridictionnel** devant le tribunal du travail — **3 mois** à compter de la décision du Directeur (ou de l'expiration du délai de réponse) (CJ **art. 580**, 2°).

Outil BE-only (`workspaceCountry=BELGIQUE`). Jumeau BE structurel de F-DT-35 `contestation-are` (allocations d'aide au retour à l'emploi FR) — patterns équivalents, substance juridique distincte.

## Contrat API (figé)

`POST /api/v1/case-files/{caseFileId}/decision-tools/contestation-c4-onem`

Inputs (`ContestationC4OnemRequest`) :
```json
{
  "dateNotificationDecisionOnem": "2026-04-15",       // requis (date notification décision)
  "dateActionEnvisagee": "2026-05-20",                // optionnel (default today, Europe/Brussels)
  "recoursAdminDejaForme": false,                      // booléen — si true, calcul du délai tribunal seulement
  "dateDecisionDirecteur": null                        // requis si recoursAdminDejaForme=true (notification décision recours admin)
}
```

Réponse 200 :
```json
{
  "verdict": "RECOURS_ADMIN_OUVERT" | "RECOURS_ADMIN_IMMINENT" | "RECOURS_ADMIN_PRESCRIT" | "RECOURS_TRIBUNAL_OUVERT" | "RECOURS_TRIBUNAL_IMMINENT" | "RECOURS_TRIBUNAL_PRESCRIT",
  "paliers": [
    { "type": "ADMIN", "dateLimite": "2026-05-15", "joursRestants": -5, "baseJuridique": "AR du 25/11/1991 art. 144" },
    { "type": "TRIBUNAL", "dateLimite": null, "joursRestants": null, "baseJuridique": "CJ art. 580, 2°" }
  ],
  "etapeSuivante": "RECOURS_ADMIN_DIRECTEUR" | "RECOURS_TRIBUNAL_TRAVAIL" | "FORCLUSION_TOTALE" | "AUCUNE",
  "baseJuridique": "AR du 25 novembre 1991 art. 144 ; CJ art. 580, 2° ; Loi du 3 juillet 1978",
  "formuleCalcul": "Notification ONEM (2026-04-15) + 1 mois = dateLimiteAdmin (2026-05-15) ; joursRestants = -5 → admin prescrit ; tribunal indéterminé tant que décision directeur non notifiée."
}
```

`GET` du même path : dernière analyse persistée ou 404.

## Logique de calcul (`ContestationC4OnemCalculator`)

Cas A — `recoursAdminDejaForme=false` (pas encore de recours administratif) :
- `dateLimiteAdmin = dateNotificationDecisionOnem + 1 mois` (calendrier Europe/Brussels).
- `joursRestantsAdmin = dateLimiteAdmin - dateActionEnvisagee`.
- Verdict palier ADMIN :
  - `RECOURS_ADMIN_OUVERT` si `joursRestantsAdmin > 7` ;
  - `RECOURS_ADMIN_IMMINENT` si `0 < joursRestantsAdmin ≤ 7` ;
  - `RECOURS_ADMIN_PRESCRIT` si `joursRestantsAdmin ≤ 0`.
- Palier TRIBUNAL : `dateLimite=null, joursRestants=null` (indéterminé tant que décision Directeur non notifiée).
- `etapeSuivante` :
  - `RECOURS_ADMIN_DIRECTEUR` si admin OUVERT ou IMMINENT ;
  - `RECOURS_TRIBUNAL_TRAVAIL` si admin PRESCRIT (saut palier admin — possible directement vers tribunal selon CJ 580, à valider par l'avocat).
- `verdict` global = verdict palier ADMIN.

Cas B — `recoursAdminDejaForme=true` + `dateDecisionDirecteur` non null :
- `dateLimiteTribunal = dateDecisionDirecteur + 3 mois`.
- `joursRestantsTribunal = dateLimiteTribunal - dateActionEnvisagee`.
- Verdict palier TRIBUNAL : seuils 14 j (IMMINENT) / 0 (PRESCRIT).
- Palier ADMIN : prescrit (renvoyé en informatif).
- `etapeSuivante` :
  - `RECOURS_TRIBUNAL_TRAVAIL` si tribunal OUVERT ou IMMINENT ;
  - `FORCLUSION_TOTALE` si tribunal PRESCRIT.
- `verdict` global = verdict palier TRIBUNAL.

Calculs en fuseau Europe/Brussels, ajustement « 1 mois calendaire » via `LocalDate.plusMonths(1)` (gestion automatique fins de mois).

## Cas d'erreur

| Situation | Code |
|---|---|
| `workspaceCountry !== BELGIQUE` | 404 (isolation BE-only) |
| `caseFileId` autre workspace | 404 |
| `dateNotificationDecisionOnem` manquant ou futur | 400 |
| `recoursAdminDejaForme=true` sans `dateDecisionDirecteur` | 400 |
| `dateDecisionDirecteur < dateNotificationDecisionOnem` | 400 |

## Composants à créer (pattern `ContestationAre*` à mirrorer)

Sous `backend/src/main/java/fr/ailegalcase/casefile/` :
- `ContestationC4OnemAnalysis.java` (entité JPA, unique sur `case_file_id`)
- `ContestationC4OnemRepository.java`
- `ContestationC4OnemRequest.java` (Bean Validation : `@NotNull dateNotificationDecisionOnem`, validation conditionnelle `dateDecisionDirecteur` selon `recoursAdminDejaForme`)
- `ContestationC4OnemResult.java` (record avec enum `Verdict`, enum `EtapeSuivante`, record `Palier(type ADMIN|TRIBUNAL, dateLimite, joursRestants, baseJuridique)`)
- `ContestationC4OnemResponse.java`
- `ContestationC4OnemCalculator.java` (fonction pure, Cas A + Cas B)
- `ContestationC4OnemService.java` (gate workspace BELGIQUE, gate caseFile, persistance)
- `ContestationC4OnemController.java` (POST + GET)

Migration `XXX-create-contestation-c4-onem-analyses.xml` (prochain numéro disponible, probablement 256 après 255 SF-207-02b visibility). Table : `id`, `case_file_id` (FK CASCADE unique), `result_data` TEXT JSON, `created_at`/`updated_at`. Rollback `dropTable`.

Extensions :
- `LegalDomainPromptBuilder` (branche BE Travail) : ajout des champs `dateNotificationDecisionOnem`, `dateDecisionDirecteur`, `recoursAdminEnvisage` (boolean dérivé `motifRupture` ∋ "faute grave" + `contestationAreEnvisagee`-like). Émission `critereCode` `BE_CONTESTATION_C4_DATE_NOTIFICATION_ONEM`, `BE_CONTESTATION_C4_DATE_DECISION_DIRECTEUR`, `BE_CONTESTATION_C4_RECOURS_ADMIN_DEJA_FORME`.
- `CaseAnalysisResponse.TravailExtractedData` : ajout champs `dateNotificationDecisionOnem` (String), `dateDecisionDirecteur` (String), `recoursAdminDejaForme` (Boolean). Rétrocompat constructeurs.

## Critères d'acceptation

- [ ] Cas A — palier ADMIN calculé correctement ; verdict OUVERT/IMMINENT/PRESCRIT selon `joursRestantsAdmin` (seuil 7 j).
- [ ] Cas B — palier TRIBUNAL calculé ; verdict OUVERT/IMMINENT/PRESCRIT (seuil 14 j) ; étape suivante FORCLUSION_TOTALE si tribunal prescrit.
- [ ] `recoursAdminDejaForme=true` sans `dateDecisionDirecteur` → 400.
- [ ] Workspace FR → 404.
- [ ] Workspace BE → 200 + persistance.
- [ ] `GET` après `POST` → 200 ; sans POST → 404.
- [ ] `critereCode` BE_CONTESTATION_C4_* émis (`CritereCodeIntegrityIT` reste vert).
- [ ] `TravailExtractedData` BE étendu — rétrocompat OK.
- [ ] Calculs fuseau Europe/Brussels, `plusMonths(1)` / `plusMonths(3)` corrects en fin de mois.

## Périmètre / Hors scope

- Frontend (SF-207-03b).
- Génération du recours rédigé (lettre type ONEM Directeur, requête tribunal du travail) — peut être enrichi en V2.
- Procédure de pourvoi en cassation (`pourvoi-cassation-social`, autre outil backlog).

## Plan de test

`ContestationC4OnemCalculatorTest` (10+ tests) : Cas A OUVERT / IMMINENT / PRESCRIT ; Cas B OUVERT / IMMINENT / PRESCRIT (FORCLUSION) ; bornes seuils ; gestion fin de mois (`plusMonths(1)` 31 janvier → 28/29 février) ; `dateActionEnvisagee` après `dateLimite`.

`ContestationC4OnemControllerIT` (5+ tests) : BE OK, FR 404, autre workspace 404, Bean Validation 400 (date manquante, `recoursAdminDejaForme=true` sans `dateDecisionDirecteur`), GET sans POST 404.

## Dépendances

- SF-207-02 (#1123) — pattern `TravailExtractedData` BE déjà étendu.
- F-DT-35 `ContestationAre*` — pattern source.
