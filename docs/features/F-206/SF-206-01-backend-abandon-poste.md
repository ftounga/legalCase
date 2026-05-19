# SF-206-01 — Backend : outil décisionnel « abandon de poste / présomption de démission »

> Feature F-206 — P1 Travail FR — 4 outils d'urgences procédurales. Cadrages : `SF-206-00-coherence.md` (GO), `SF-206-00b-ux-coherence.md` (GO).
> Outil : `F-DT-42-abandon-poste-presomption-demission`. Fondement : L. 1237-1-1 CT (loi 21/12/2022), D. 1237-2-1 s. (décret 17/04/2023).

## Objectif

Fournir le moteur backend qui évalue, du point de vue de l'avocat du salarié, la **solidité de la contestation** d'une présomption de démission par abandon de poste — irrégularités de la mise en demeure et motif légitime d'absence.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/abandon-poste-presomption-demission` reçoit la saisie de l'avocat, calcule via un `Calculator` stateless, persiste un snapshot et renvoie le verdict. `GET` renvoie le dernier snapshot.

L'analyseur détecte les **irrégularités côté employeur** :
- délai de mise en demeure < 15 jours calendaires (D. 1237-2-1) ;
- mise en demeure ne mentionnant pas le délai imparti, ou pas les conséquences (présomption de démission) ;
- mise en demeure non notifiée par LRAR ni remise en main propre contre décharge ;
- absence justifiée par un **motif légitime** (médical, droit de retrait, droit de grève, modification du contrat refusée, défaut de paiement du salaire) — fait tomber la présomption ;
- reprise du poste ou justification de l'absence intervenue dans le délai imparti.

Verdict (`Verdict`) — solidité de la contestation : `CONTESTATION_SOLIDE` / `CONTESTATION_INCERTAINE` / `CONTESTATION_DIFFICILE`. Score 0-100 (solidité). Chaque irrégularité détectée = un `MotifContestation(code, libelle, fondement, poids, explication)`.

L'outil expose une **date d'échéance** : `dateMiseEnDemeurePresentee + delaiAccordeJours` (expiration du délai) — destinée à F-69 (échéance « Expiration du délai de mise en demeure (abandon de poste) »).

## Cas d'erreur

- `caseFileId` inexistant ou hors workspace de l'utilisateur → 404.
- Domaine du dossier ≠ `DROIT_DU_TRAVAIL` → 422 (outil hors domaine).
- `country` du workspace ≠ `FRANCE` → 422 (outil FR-only, art. franco-français).
- Corps de requête invalide (date future incohérente, délai négatif) → 400.

## Contrat API (figé — référence pour SF-206-02)

```
POST /api/v1/case-files/{caseFileId}/abandon-poste-presomption-demission
Request {
  dateMiseEnDemeurePresentee: LocalDate,            // date de présentation de la MED
  modeNotificationMiseEnDemeure: enum LRAR|REMISE_MAIN_PROPRE|AUTRE,
  delaiAccordeJours: int,                            // délai imparti par l'employeur
  miseEnDemeureMentionneDelai: boolean,
  miseEnDemeureMentionneConsequences: boolean,
  motifAbsenceInvoque: enum AUCUN|MEDICAL|DROIT_RETRAIT|DROIT_GREVE|MODIFICATION_CONTRAT_REFUSEE|DEFAUT_PAIEMENT_SALAIRE|AUTRE,
  motifAbsenceCommentaire: String|null,
  repriseOuJustificationDansDelai: boolean,
  dateRepriseOuJustification: LocalDate|null
}
Response 200 {
  ...12 champs input (snapshot),
  verdict: CONTESTATION_SOLIDE|CONTESTATION_INCERTAINE|CONTESTATION_DIFFICILE,
  scoreContestation: int,                            // 0-100
  motifsContestation: [{code, libelle, fondement, poids, explication}],
  dateExpirationDelai: LocalDate,                    // échéance dérivée
  basesJuridiques: [String],
  messages: [String],
  country: "FRANCE",
  calculatedAt: Instant
}
GET …/abandon-poste-presomption-demission → 200 dernier snapshot | 204 si absent
```

`critereCode` F-IA-03 émis par le prompt pour le cross-check de cohérence : `DT42_DATE_MISE_EN_DEMEURE`, `DT42_DELAI_ACCORDE`, `DT42_MENTIONS_MED`, `DT42_MOTIF_LEGITIME`.

## Pré-remplissage IA (invariant F-246 — tous les champs)

Extension du record `TravailExtractedData` (`CaseAnalysisResponse.java`) — objet `abandon_poste_detail` :
`abandonPosteDateMiseEnDemeure`, `abandonPosteModeNotification`, `abandonPosteDelaiAccordeJours`, `abandonPosteMotifAbsence`, `abandonPosteDateReprise`, `abandonPosteMedMentionneDelai`, `abandonPosteMedMentionneConsequences`, `abandonPosteRepriseDansDelai`.
Extension de `LegalDomainPromptBuilder` (bloc DROIT_DU_TRAVAIL) pour faire extraire ces champs par l'IA. Aucun champ n'est laissé non pré-rempli sauf information absente des pièces.

## Critères d'acceptation

1. Délai < 15 j → irrégularité `DT42_DELAI_INSUFFISANT` détectée, poids fort.
2. Motif d'absence ≠ AUCUN → irrégularité `DT42_MOTIF_LEGITIME`, poids fort, verdict tiré vers `CONTESTATION_SOLIDE`.
3. MED sans mention du délai OU des conséquences → irrégularité de forme.
4. Mode de notification = AUTRE → irrégularité de forme.
5. `repriseOuJustificationDansDelai = true` → irrégularité majeure (présomption inopérante).
6. Aucune irrégularité → `CONTESTATION_DIFFICILE`, score bas.
7. `dateExpirationDelai` = `dateMiseEnDemeurePresentee + delaiAccordeJours`.
8. Outil renvoyé en 422 hors `DROIT_DU_TRAVAIL` / hors `FRANCE`.
9. Isolation workspace : un utilisateur d'un autre workspace reçoit 404.
10. Pré-remplissage : les 8 champs `abandon_poste_detail` sont extraits par le prompt.

## Plan de test

- **UT `AbandonPostePresomptionDemissionCalculatorTest`** : délai 14 j vs 15 j vs 20 j ; chaque motif légitime ; absence de mentions ; reprise dans le délai ; cumul d'irrégularités ; bornes de score.
- **IT `AbandonPostePresomptionDemissionControllerIT`** : POST + GET, droits, domaine, pays, isolation workspace (404 cross-workspace), validation 400.
- **IT visibilité** : `DecisionToolVisibilityIntegrityIT` reste vert (seed ↔ TOOL_REGISTRY — coordination avec SF-206-02).

## Tables / endpoints / composants impactés

- **Nouvelle table** `abandon_poste_presomption_demission_analyses` (id, case_file_id, country, snapshot_data JSONB, calculated_at) — migration Liquibase (prochain numéro libre).
- **Seed** `decision_tool_visibility_rules` : `tool_id=F-DT-42-abandon-poste-presomption-demission`, `legal_domain=DROIT_DU_TRAVAIL`, `country=FRANCE`, `layer=CONTEXTUAL`, `trigger_field=abandon_poste_detecte`, `trigger_value=true` — migration Liquibase.
- **Nouveaux fichiers** `fr.ailegalcase.casefile` : `AbandonPostePresomptionDemissionCalculator`, `…Request`, `…Response`, `…Input`, `…Result`, `…Analysis`, `…Repository`, `…Service`, `…Controller`.
- **Modifiés** : `CaseAnalysisResponse.java` (record + builder + toBuilder), `LegalDomainPromptBuilder.java` (prompt + critereCodes), `CaseFileDashboardService.java` (mapper `DashboardTile`, thème `DIAGNOSTIC`).

## Préoccupations transversales

**Outil décisionnel métier** — création d'un analyseur. Invariant « un outil = une situation » respecté (situation = contestation d'une présomption de démission). **Plans / limites** : non. **Auth / workspace** : réutilise le pattern existant (résolution workspace via `CaseFile`). Smoke E2E non impacté (pas de route publique ni de modification d'auth).

## Hors périmètre

- Frontend (→ SF-206-02).
- Génération de la lettre de contestation (couvert par F-98).
- Création effective de l'échéance dans F-69 : l'outil **expose** `dateExpirationDelai` ; le branchement dans `case_deadlines` est hors périmètre de cette SF (la date est rendue exploitable, son intégration calendaire suit le pattern F-69 et fera l'objet d'un suivi si besoin émerge).
