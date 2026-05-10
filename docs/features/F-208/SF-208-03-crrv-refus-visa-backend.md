# SF-208-03 — CRRV recours refus de visa (2 mois) — backend

## Objectif (1 phrase)
Calculer le délai de recours hiérarchique de 2 mois devant la Commission de recours contre les refus de visa (CRRV — préalable obligatoire avant TA Nantes), L.312-1+ CESEDA, et persister l'analyse 1:1 par dossier.

## Comportement nominal
- POST `/api/v1/case-files/{caseFileId}/crrv-refus-visa-analysis`
- Body : `dateNotificationRefus` (LocalDate, requis), `typeVisa` (enum string : COURT_SEJOUR, LONG_SEJOUR, REGROUPEMENT_FAMILIAL, ETUDIANT, AUTRE), `motifRefus` (string libre 200 c.), `recoursForme` (boolean), `dateRecours` (LocalDate, optionnel)
- Calculator `CrrvRefusVisaCalculator` :
  - `dateExpirationRecoursCrrv` = notification + 2 mois (CESEDA D.312-3)
  - `dateExpirationRecoursTaNantes` = decisionCrrv + 2 mois (recours juridictionnel après CRRV) — non calculé tant que dateDecisionCrrv non saisie (champ optionnel)
  - `joursRestants`, `statut` ∈ {DISPONIBLE, URGENT (≤ 7 j), EXPIRE, RECOURS_FORME}
  - `messages` : préalable obligatoire CRRV avant TA Nantes (compétence exclusive R.312-3 CJA), motivation d'usage selon type visa
- GET `/api/v1/case-files/{caseFileId}/crrv-refus-visa-analysis`

## Cas d'erreur
- 400 si dateNotificationRefus futur, typeVisa inconnu
- 400 si workspace.country ≠ FRANCE
- 400 si caseFile.legalDomain ≠ DROIT_IMMIGRATION
- 404 isolation

## Critères d'acceptation vérifiables
- [x] POST nominal retourne 200 avec dateExpirationRecoursCrrv = notif + 2 mois et statut
- [x] Statut URGENT si ≤ 7 jours
- [x] Statut EXPIRE si délai dépassé
- [x] POST workspace BE → 400, dossier travail → 400
- [x] Calculator UT, IT couvre nominal + erreurs

## Plan de test minimal
- **UT** `CrrvRefusVisaCalculatorTest` : 8+ tests
- **IT** `CrrvRefusVisaControllerIT` : 6+ tests
- **Intégrité** : ajout `F-IM-23-crrv-refus-visa-fr` dans `KNOWN_FRONTEND_TOOL_IDS`

## Tables / endpoints / composants impactés
- **Nouvelle table** `crrv_refus_visa_analyses` (id UUID, case_file_id UUID UNIQUE, date_notification_refus DATE NOT NULL, type_visa VARCHAR(40) NOT NULL, motif_refus VARCHAR(255), recours_forme BOOLEAN NOT NULL DEFAULT false, date_recours DATE, country VARCHAR(20) NOT NULL, result_data TEXT NOT NULL, created_at TIMESTAMP, updated_at TIMESTAMP)
- **Migration Liquibase** `220-create-crrv-refus-visa-analyses.xml` + seed `decision_tool_visibility_rules` (ALWAYS_ON, DROIT_IMMIGRATION, FRANCE, F-IM-23-crrv-refus-visa-fr — l'avocat doit pouvoir saisir proactivement un refus de visa). Choix ALWAYS_ON justifié : **pas de flag IA dédié `refus_visa_detecte` actuellement** (à ajouter dans F-205 extension Travail FR ou un futur F-205-bis Immigration). Visible donc en attendant pour ne pas masquer un outil P1.
- **Endpoint** `CrrvRefusVisaController`
- **Test d'intégrité** : ajout `F-IM-23-crrv-refus-visa-fr`

## Hors périmètre
- Frontend Angular (SF future)
- Génération automatique du courrier CRRV (différé)
- Calcul automatique du délai TA Nantes après décision CRRV (champ optionnel pour V1)

## Impact par domaine métier
**Sensible Immigration FR uniquement** : CRRV est une institution française (Nantes), pas d'équivalent en BE. Aucun impact Travail / Famille.

## Parité des domaines métier
Niveau 3 (calculateur). Parité BE = sans objet (CRRV inexistant en BE).

## Analyse de cohérence transversale
- **Autres outils Immigration FR** : aucun chevauchement (CRRV ≠ recours OQTF, ≠ recours préfecture).
- **Pattern réutilisé** : copie pattern Annexe13Be.

## Audit "Impact F-166 cross-C×D"
- **FR×Immigration** : ajout 1 entrée ALWAYS_ON `F-IM-23-crrv-refus-visa-fr`. Pas de retrait. ALWAYS_ON justifié : absence de flag IA dédié.
- Autres cellules : non concernées.

## Audit "exhaustivité droit national FR"
- Source : CESEDA L.312-1 à L.312-3 + D.312-3 + arrêté du 10 mars 2011.
- Équivalent BE : sans objet — les refus de visa BE relèvent du Conseil du contentieux des étrangers directement, pas d'instance hiérarchique consulaire spécialisée. **Justifié de ne pas créer d'équivalent BE**.

## Contrat API
**POST** `/api/v1/case-files/{caseFileId}/crrv-refus-visa-analysis`
```json
{
  "dateNotificationRefus": "2026-04-15",
  "typeVisa": "LONG_SEJOUR",
  "motifRefus": "Ressources insuffisantes",
  "recoursForme": false,
  "dateRecours": null
}
```
Erreurs : 400 / 404.
