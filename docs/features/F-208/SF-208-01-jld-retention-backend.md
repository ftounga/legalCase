# SF-208-01 — JLD rétention administrative (24-48 h) — backend

## Objectif (1 phrase)
Calculer les délais critiques (24 h notification, audience 5 j, recours 24 h) du contentieux de la rétention administrative devant le Juge des libertés et de la détention (JLD), art. L.741+ et L.743+ CESEDA, et persister l'analyse 1:1 par dossier.

## Comportement nominal
- POST `/api/v1/case-files/{caseFileId}/jld-retention-analysis`
- Body : `dateNotificationPlacement` (LocalDate, requis), `motifPlacement` (enum string), `recoursForme` (boolean), `dateRecours` (LocalDate, optionnel)
- Calculator `JldRetentionCalculator` calcule :
  - `dateExpirationSaisineJld` = notification + 48 h (délai pour saisir le JLD avant expiration intervention judiciaire)
  - `dateAudienceJld` = notification + 5 j (audience JLD ordinaire L.743-9)
  - `dateExpirationRecoursAppel` = notification décision JLD + 24 h (appel ouvert au procureur ou à l'étranger)
  - `statut` ∈ {DISPONIBLE, URGENT (≤ 12 h), EXPIRE, RECOURS_FORME}
  - `joursRestants` (en heures pour précision 24 h, exposé en jours arrondi)
- Output persisté dans `jld_retention_analyses` (1:1 case_file)
- GET `/api/v1/case-files/{caseFileId}/jld-retention-analysis` → 200 ou 404 si jamais POST

## Cas d'erreur
- 400 si dateNotificationPlacement futur, motifPlacement inconnu, recoursForme=true sans dateRecours, dateRecours antérieure à notification
- 400 si workspace.country ≠ FRANCE (outil FR-only)
- 400 si caseFile.legalDomain ≠ DROIT_IMMIGRATION
- 404 si caseFile inaccessible au workspace de l'avocat (isolation)

## Critères d'acceptation vérifiables
- [x] POST nominal retourne 200 avec dateExpirationSaisineJld, dateAudienceJld, statut
- [x] POST sur workspace BE retourne 400
- [x] POST sur dossier travail retourne 400
- [x] POST avec motif inconnu retourne 400
- [x] GET sans POST préalable retourne 404
- [x] POST upsert remplace l'analyse précédente
- [x] Isolation workspace : l'avocat A ne voit pas l'analyse du dossier B (404)
- [x] Test calculator unitaire avec Clock fixé pour stabilité temporelle

## Plan de test minimal
- **UT** `JldRetentionCalculatorTest` : 8+ tests (statuts, motifs, expiration, recours formé, dates futures rejetées, motif inconnu)
- **IT** `JldRetentionControllerIT` : 6+ tests (POST nominal, POST BE→400, POST travail→400, POST autre workspace→404, GET sans POST→404, POST upsert)
- **Intégrité** : ajout `F-IM-21-jld-retention-fr` dans `KNOWN_FRONTEND_TOOL_IDS` du `DecisionToolVisibilityIntegrityIT`

## Tables / endpoints / composants impactés
- **Nouvelle table** `jld_retention_analyses` (id UUID, case_file_id UUID UNIQUE, date_notification_placement DATE NOT NULL, motif_placement VARCHAR(40) NOT NULL, recours_forme BOOLEAN NOT NULL DEFAULT false, date_recours DATE, country VARCHAR(20) NOT NULL, result_data TEXT NOT NULL, created_at TIMESTAMP, updated_at TIMESTAMP)
- **Migration Liquibase** `218-create-jld-retention-analyses.xml` + INSERT seed `decision_tool_visibility_rules` (CONTEXTUAL, DROIT_IMMIGRATION, FRANCE, F-IM-21-jld-retention-fr, trigger_field=`mesure_eloignement_detectee`)
- **Endpoint** `JldRetentionController` (POST, GET) sous `/api/v1/case-files/{caseFileId}/jld-retention-analysis`
- **Test d'intégrité** `DecisionToolVisibilityIntegrityIT` : ajouter `F-IM-21-jld-retention-fr` dans `KNOWN_FRONTEND_TOOL_IDS` (le frontend sera livré ultérieurement, l'entrée garde-fou doit néanmoins être posée pour CI verte).

## Hors périmètre
- Composant Angular (sera livré dans une SF F-208-XX-frontend ultérieure)
- Pré-fill IA / validation F-IA-03 (couverte par SF future avec frontend)
- Génération automatique du recours (différée — requête PDF en F-IM-06 existant)

## Impact par domaine métier
**Sensible Immigration FR uniquement.** Outil purement français : la rétention administrative belge passe par un autre régime (chambre du conseil + Loi 15/12/1980 art. 71+) — couvert symétriquement par F-209 si nécessaire. Aucun impact Travail / Famille.

## Parité des domaines métier
Niveau 3 (calculateur de délais) — pas de scoring/comparateur. La parité Famille/Travail n'est pas pertinente : la rétention administrative est strictement immigration. La parité BE est tracée dans F-209 P1 Immigration BE (annexes BE).

## Analyse de cohérence transversale
- **Autres outils décisionnels Immigration FR** : OqtfAvecDelai (L.614-5), OqtfSansDelai (L.614-1), Annexe13Be (BE équivalent). JLD-rétention est complémentaire (procédure judiciaire vs administrative). Aucun chevauchement.
- **Pattern réutilisé** : copie 1:1 du pattern `Annexe13BeCalculator` + `Annexe13BeService` + `Annexe13BeController` + entity 1:1 case_file.
- **Pas de nouveau pattern UI / service partagé** : pure réutilisation.

## Audit "Impact F-166 cross-C×D"
- **FR×Travail** : non concerné.
- **FR×Immigration** : ajout 1 entrée CONTEXTUAL `F-IM-21-jld-retention-fr` (trigger `mesure_eloignement_detectee=true`). Aucun outil existant retiré.
- **FR×Famille** : non concerné.
- **BE×Immigration** : non concerné (procédure FR uniquement).
- **BE×Travail / BE×Famille** : non concernés.

## Audit "exhaustivité droit national FR"
- Source juridique : CESEDA L.741-1 à L.741-10 (placement en rétention), L.742-1 à L.742-12 (procédure JLD), L.743-1 à L.743-25 (audiences et recours), R.741-1+. Loi 26/01/2024 a porté la durée maximale à 90 jours.
- Équivalent BE : la chambre du conseil (Loi 15/12/1980 art. 71-74) — procédure analogue mais distincte. **Justifié de ne pas créer le jumeau BE dans cette SF** : F-209 P1 Immigration BE traitera annexes BE et chambres du conseil quand activé.

## Contrat API
**POST** `/api/v1/case-files/{caseFileId}/jld-retention-analysis`
Body :
```json
{
  "dateNotificationPlacement": "2026-05-08",
  "motifPlacement": "EXECUTION_OQTF",
  "recoursForme": false,
  "dateRecours": null
}
```
Réponse 200 :
```json
{
  "caseFileId": "...",
  "dateNotificationPlacement": "2026-05-08",
  "motifPlacement": "EXECUTION_OQTF",
  "recoursForme": false,
  "dateRecours": null,
  "country": "FRANCE",
  "dateExpirationSaisineJld": "2026-05-10",
  "dateAudienceJld": "2026-05-13",
  "dateExpirationRecoursAppel": null,
  "joursRestantsAvantSaisine": 2,
  "statut": "DISPONIBLE",
  "motifs_valides": ["EXECUTION_OQTF","ITF","INTERDICTION_TERRITOIRE","DUBLIN_TRANSFERT","AUTRE"],
  "formule": "Placement en rétention notifié le ...",
  "baseJuridique": "CESEDA L.741-1, L.743-9, L.743-21",
  "messages": [...]
}
```
Erreurs : 400 (validation), 404 (dossier non trouvé / non accessible).
