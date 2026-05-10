# SF-208-02 — Dublin recours 7 jours suspensif — backend

## Objectif (1 phrase)
Calculer le délai suspensif de recours de 7 jours (jours calendaires) contre une décision de transfert Dublin (Règlement UE 604/2013 art. 27 + CESEDA L.572+) et persister l'analyse 1:1 par dossier.

## Comportement nominal
- POST `/api/v1/case-files/{caseFileId}/dublin-recours-analysis`
- Body : `dateNotificationDecisionTransfert` (LocalDate, requis), `etatMembreResponsable` (string, code ISO 2 ou nom court), `motifTransfert` (enum string), `recoursForme` (boolean), `dateRecours` (LocalDate, optionnel)
- Calculator `DublinRecoursCalculator` :
  - `dateExpirationRecours` = notification + 7 jours calendaires (CESEDA L.572-4)
  - `joursRestants`, `statut` ∈ {DISPONIBLE, URGENT (≤ 2 j), EXPIRE, RECOURS_FORME}
  - `dateLimiteTransfertEffectif` = notification + 6 mois (Règlement Dublin III art. 29 — délai de transfert max ; au-delà, retour à charge de la France)
  - `effetSuspensif` = "automatique" (recours suspend l'exécution — L.572-4)
  - `messages` : motivation Dublin (Etat membre, motif), accès au recours juridictionnel effectif (CJUE Mengesteab C-670/16)
- GET `/api/v1/case-files/{caseFileId}/dublin-recours-analysis`

## Cas d'erreur
- 400 si dateNotificationDecisionTransfert futur, motifTransfert inconnu, recoursForme=true sans dateRecours
- 400 si workspace.country ≠ FRANCE
- 400 si caseFile.legalDomain ≠ DROIT_IMMIGRATION
- 404 isolation workspace

## Critères d'acceptation vérifiables
- [x] POST nominal retourne 200 avec dateExpirationRecours = notif + 7 j et statut
- [x] Statut URGENT si ≤ 2 jours restants
- [x] Statut EXPIRE si délai dépassé
- [x] POST sur workspace BE → 400
- [x] POST sur dossier travail → 400
- [x] Calculator UT couvre tous les statuts + validation
- [x] IT couvre nominal + erreurs gates + isolation

## Plan de test minimal
- **UT** `DublinRecoursCalculatorTest` : 8+ tests (statuts, motifs, recours formé, futur rejeté, motif inconnu, dateLimiteTransfertEffectif)
- **IT** `DublinRecoursControllerIT` : 6+ tests (nominal, BE→400, travail→400, autre workspace→404, GET sans POST→404, upsert)
- **Intégrité** : ajout `F-IM-22-dublin-recours-fr` dans `KNOWN_FRONTEND_TOOL_IDS`

## Tables / endpoints / composants impactés
- **Nouvelle table** `dublin_recours_analyses` (id UUID, case_file_id UUID UNIQUE, date_notification_decision_transfert DATE NOT NULL, etat_membre_responsable VARCHAR(60), motif_transfert VARCHAR(40) NOT NULL, recours_forme BOOLEAN NOT NULL DEFAULT false, date_recours DATE, country VARCHAR(20) NOT NULL, result_data TEXT NOT NULL, created_at TIMESTAMP, updated_at TIMESTAMP)
- **Migration Liquibase** `219-create-dublin-recours-analyses.xml` + seed `decision_tool_visibility_rules` (CONTEXTUAL, DROIT_IMMIGRATION, FRANCE, F-IM-22-dublin-recours-fr, trigger_field=`procedure_asile_detectee`)
- **Endpoint** `DublinRecoursController`
- **Test d'intégrité** : ajout `F-IM-22-dublin-recours-fr` dans `KNOWN_FRONTEND_TOOL_IDS`

## Hors périmètre
- Composant Angular (SF future)
- Pré-fill IA / validation F-IA-03 (avec frontend)
- Génération recours PDF (F-IM-06 existant gère le générique recours)

## Impact par domaine métier
**Sensible Immigration FR uniquement.** Le mécanisme Dublin existe également côté belge (Office des étrangers + recours CCE), mais procédurallement distinct (15 jours BE vs 7 jours FR) — sera traité par F-209 si nécessaire. Aucun impact Travail / Famille.

## Parité des domaines métier
Niveau 3 (calculateur). Pas de scoring/comparateur. Parité BE différée à F-209.

## Analyse de cohérence transversale
- **Autres outils décisionnels Immigration FR** : aucun chevauchement (Dublin est un recours unique, séparé d'OQTF et de l'asile).
- **Pattern réutilisé** : copie 1:1 du pattern `Annexe13BeCalculator`/Service.
- **Pas de nouveau pattern UI / service partagé**.

## Audit "Impact F-166 cross-C×D"
- **FR×Immigration** : ajout 1 entrée CONTEXTUAL `F-IM-22-dublin-recours-fr` (trigger `procedure_asile_detectee=true` — Dublin = phase préliminaire de la procédure d'asile).
- Autres cellules : non concernées.

## Audit "exhaustivité droit national FR"
- Source : CESEDA L.572-1 à L.572-9 (recours transfert Dublin) + Règlement (UE) 604/2013 art. 27 (recours juridictionnel effectif) + 29 (délai transfert 6 mois).
- Équivalent BE : Loi 15/12/1980 art. 51/5 (procédure Dublin) + recours CCE — délais et procédure distincts. **Justifié de différer le jumeau BE** : F-209 P1 Immigration BE planifié.

## Contrat API
**POST** `/api/v1/case-files/{caseFileId}/dublin-recours-analysis`
Body :
```json
{
  "dateNotificationDecisionTransfert": "2026-05-04",
  "etatMembreResponsable": "ITALIE",
  "motifTransfert": "DEMANDE_ASILE_AUTRE_ETAT",
  "recoursForme": false,
  "dateRecours": null
}
```
Erreurs : 400 / 404.
