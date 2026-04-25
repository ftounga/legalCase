# SF-DT-13-01 — Backend Licenciement économique (FR)

> **Feature parente** : F-DT-13 Licenciement économique détaillé (V7).
> **Pays** : FRANCE uniquement (pas d'équivalent direct BE — le licenciement collectif BE relève du PSE Renault, traité dans F-DT-14 PSE / régime BE distinct).
> **Statut** : `In progress` — backend-first, frontend SF-DT-13-02 en vague suivante.
> **Branche** : `feat/SF-DT-13-01-licenciement-economique-backend`

## Objectif (1 phrase)

Fournir un calculateur backend qui évalue le risque de requalification d'un licenciement pour motif économique selon 3 axes : causalité du motif (L.1233-3), respect des critères d'ordre (L.1233-5), et obligation de reclassement + priorité de réembauche (L.1233-4 + L.1233-45).

## Comportement nominal

L'utilisateur saisit dans un dossier de droit du travail FR :
- Le motif économique invoqué (`DIFFICULTES_ECONOMIQUES`, `MUTATIONS_TECHNOLOGIQUES`, `REORGANISATION_COMPETITIVITE`, `CESSATION_ACTIVITE`, `AUTRE`)
- Les preuves matérielles du motif (multi-select)
- Les critères d'ordre appliqués (multi-select, 4 obligatoires L.1233-5)
- Les caractéristiques du salarié (âge, ancienneté, charges famille, qualités professionnelles)
- Les tentatives de reclassement (multi-select)
- Si la priorité de réembauche L.1233-45 a été proposée
- Si un congé de reclassement a été proposé
- Date de notification du licenciement

Le service calcule **3 sous-scores 0-100** (`scoreCausalite`, `scoreCriteresOrdre`, `scoreReclassement`) puis un **scoreGlobal = moyenne** et un **verdict de risque de requalification** (`FAIBLE` / `MOYENNE` / `ELEVEE`). L'analyse est persistée 1:1 par dossier (upsert) et exposée via GET.

## Règles de calcul

### Score causalité (0-100)
- Motif `AUTRE` ou aucun motif : 0
- Motif reconnu (≠ AUTRE) : +20 base
- ≥ 2 preuvesMotif : +30 (total = +50)
- ≥ 4 preuvesMotif : +30 supplémentaires (total = 80, capé)
- Bonus +20 si `RAPPORT_EXPERT` parmi preuves (cap 100)

### Score critères d'ordre (0-100)
- 4 critères obligatoires L.1233-5 attendus : `AGE`, `ANCIENNETE`, `CHARGES_FAMILLE`, `QUALITES_PROFESSIONNELLES`
- Tous présents : 100
- Chaque critère obligatoire manquant : -25 (plancher 0)
- `SITUATION_HANDICAP` est facultatif (pas pénalisé)

### Score reclassement (0-100)
- 0 tentative : 0
- 1 tentative : 40
- ≥ 2 tentatives : 70
- + 30 si `prioriteReembauchePropose` true (cap 100)
- Si `tentativesReclassement` contient uniquement `AUCUNE` : 0

### Score global et verdict
- `scoreGlobal = round((scoreCausalite + scoreCriteresOrdre + scoreReclassement) / 3)`
- `FAIBLE` si scoreGlobal ≥ 70
- `MOYENNE` si 40 ≤ scoreGlobal < 70
- `ELEVEE` si scoreGlobal < 40

### Listes dérivées
- `criteresOrdreManquants` : critères obligatoires absents de `criteresOrdreAppliques`
- `criteresOrdreObligatoiresOk` : true si les 4 obligatoires sont présents
- `obligationReclassementRespectee` : true si ≥ 2 tentatives ET `AUCUNE` n'est pas seul

### Messages contextuels
- Confirmation priorité réembauche (L.1233-45) si proposée
- Alerte si critères obligatoires manquants
- Alerte si reclassement insuffisant
- Recommandation congé reclassement (L.1233-71) si non proposé et entreprise > 1000 salariés (note informative)

## Cas d'erreur

- Body manquant → 400
- `motifEconomiqueInvoque` null → 400
- `criteresOrdreAppliques` null → 400
- `tentativesReclassement` null → 400
- `dateNotification` null → 400
- Dossier non droit du travail → 400
- Dossier non FR (workspace BELGIQUE) → 400 (pays non couvert par la SF)
- Dossier inexistant ou hors workspace → 404

## Critères d'acceptation

1. POST avec body valide retourne 200 + score causalité, critères ordre, reclassement, global et verdict cohérents
2. POST avec motif `AUTRE` + 0 preuve retourne `scoreCausalite` = 0 et verdict `ELEVEE`
3. POST avec 4 critères ordre + ≥ 2 reclassement + priorité réembauche retourne `scoreCriteresOrdre` = 100 + `scoreReclassement` ≥ 70 + verdict `FAIBLE` ou `MOYENNE`
4. POST sur dossier BE → 400 (single-country FR)
5. POST sur dossier immigration → 400
6. POST sur dossier d'un autre workspace → 404
7. GET après POST retourne les données persistées
8. GET sans POST préalable → 404
9. POST 2 fois sur le même dossier upsert l'analyse (1:1)
10. La règle de visibilité ALWAYS_ON DROIT_DU_TRAVAIL FRANCE priority 55 est ajoutée (F-IA-04)

## Plan de test

### Unit tests (≥ 16 — `LicenciementEconomiqueCalculatorTest`)
1. Causalité — motif AUTRE + 0 preuve → score 0
2. Causalité — motif reconnu + 1 preuve → score 20
3. Causalité — motif reconnu + 2 preuves → score 50
4. Causalité — motif reconnu + 4 preuves → score 80
5. Causalité — motif reconnu + 4 preuves + RAPPORT_EXPERT → score 100
6. Critères ordre — 4 obligatoires → score 100
7. Critères ordre — 3 obligatoires → score 75
8. Critères ordre — 1 obligatoire → score 25
9. Critères ordre — 0 → score 0
10. Critères ordre — 4 obligatoires + handicap → score 100 (handicap = bonus optionnel non pénalisé)
11. Reclassement — 0 tentative → score 0
12. Reclassement — 1 tentative → score 40
13. Reclassement — 2 tentatives sans priorité → score 70
14. Reclassement — 2 tentatives + priorité réembauche → score 100
15. Reclassement — uniquement AUCUNE → score 0
16. Score global — moyenne arithmétique des 3
17. Verdict — global ≥ 70 → FAIBLE
18. Verdict — global 40-69 → MOYENNE
19. Verdict — global < 40 → ELEVEE
20. Validation — motif null → IllegalArgumentException
21. Validation — country BELGIQUE → IllegalArgumentException

### Integration tests (≥ 8 — `LicenciementEconomiqueControllerIT`)
1. POST nominal FR → 200 + scores corrects
2. POST motif AUTRE + 0 preuve → 200 + verdict ELEVEE
3. POST critères incomplets → criteresOrdreManquants non vide
4. POST workspace BE → 400
5. POST workspace immigration → 400
6. POST autre workspace → 404
7. POST upsert → mise à jour
8. GET après POST → 200 données persistées
9. GET sans POST → 404
10. POST body invalide (motif null) → 400

### Isolation workspace
- Test cross-workspace : un user d'un workspace différent reçoit 404 sur l'endpoint d'un dossier qui ne lui appartient pas (vérifié par les patterns `WorkspaceMemberRepository.findByUserAndPrimaryTrue`).

## Tables / endpoints / composants impactés

### Tables
- **NEW** `licenciement_economique_analyses` — entité 1:1 par dossier (UNIQUE constraint `case_file_id`)
  - Champs : id, case_file_id, motif_economique_invoque, preuves_motif_json, criteres_ordre_json, salarie_age, salarie_anciennete_mois, salarie_charges_famille, salarie_qualites_prof, tentatives_reclassement_json, priorite_reembauche_propose, conge_reclassement_propose, date_notification, country, result_data, created_at, updated_at

### Endpoints
- **POST** `/api/v1/case-files/{caseFileId}/licenciement-economique` — calcul + upsert
- **GET** `/api/v1/case-files/{caseFileId}/licenciement-economique` — retour de l'analyse persistée

### Migration
- **141**-create-licenciement-economique-analyses.xml
- + 1 INSERT `decision_tool_visibility_rules` ALWAYS_ON DROIT_DU_TRAVAIL FRANCE priority 55 UUID `f1a04001-0000-0000-0000-ee0000000131`

### Composants Java
- `LicenciementEconomiqueCalculator` (logique pure, statique)
- `LicenciementEconomiqueRequest`, `LicenciementEconomiqueResponse`, `LicenciementEconomiqueResult` (records)
- `LicenciementEconomiqueAnalysis` (Entity)
- `LicenciementEconomiqueRepository` (JpaRepository)
- `LicenciementEconomiqueService` (orchestration + serialization)
- `LicenciementEconomiqueController` (REST)

## Ce qui est hors périmètre

- **Frontend** : SF-DT-13-02 (vague suivante, contrat API figé ci-dessous)
- **PSE / DREETS / CSE** : F-DT-14 (séparée)
- **Belgique** : pas d'équivalent direct retenu pour cette SF (régime BE = licenciement collectif Loi Renault, traité dans F-DT-14 ou feature dédiée)
- **Indemnités chiffrées** : ce calculator est un évaluateur de **risque de requalification** (validité/contestation), pas un calculator d'indemnité (cf. F-DT-08 indemnité légale).

## Contrat API (importé par SF-DT-13-02 frontend)

### POST `/api/v1/case-files/{caseFileId}/licenciement-economique`

Request body :
```json
{
  "motifEconomiqueInvoque": "DIFFICULTES_ECONOMIQUES",
  "preuvesMotif": ["BAISSE_CHIFFRE_AFFAIRES", "PERTES_EXPLOITATION"],
  "criteresOrdreAppliques": ["AGE", "ANCIENNETE", "CHARGES_FAMILLE", "QUALITES_PROFESSIONNELLES"],
  "salarieAge": 52,
  "salarieAncienneteMois": 180,
  "salarieChargesFamille": 2,
  "salarieQualitesProf": "EXCELLENT",
  "tentativesReclassement": ["FORMATION_INTERNE", "MUTATION_GROUPE"],
  "prioriteReembauchePropose": true,
  "congeReclassementPropose": false,
  "dateNotification": "2026-04-01"
}
```

Enums :
- `motifEconomiqueInvoque` : `DIFFICULTES_ECONOMIQUES`, `MUTATIONS_TECHNOLOGIQUES`, `REORGANISATION_COMPETITIVITE`, `CESSATION_ACTIVITE`, `AUTRE`
- `preuvesMotif[]` : `BAISSE_CHIFFRE_AFFAIRES`, `PERTES_EXPLOITATION`, `BAISSE_COMMANDES`, `BAISSE_TRESORERIE`, `BAISSE_ENE`, `MUTATION_TECHNOLOGIQUE_PROUVEE`, `RAPPORT_EXPERT`, `AUTRE`
- `criteresOrdreAppliques[]` : `AGE`, `ANCIENNETE`, `CHARGES_FAMILLE`, `QUALITES_PROFESSIONNELLES`, `SITUATION_HANDICAP`
- `salarieQualitesProf` : `EXCELLENT`, `BON`, `MOYEN`, `INSUFFISANT`
- `tentativesReclassement[]` : `FORMATION_INTERNE`, `MUTATION_GROUPE`, `OFFRE_POSTE_GROUPE`, `OFFRE_POSTE_EXTERIEUR`, `AUCUNE`

Response 200 :
```json
{
  "caseFileId": "uuid",
  "motifEconomiqueInvoque": "DIFFICULTES_ECONOMIQUES",
  "preuvesMotif": [...],
  "criteresOrdreAppliques": [...],
  "salarieAge": 52,
  "salarieAncienneteMois": 180,
  "salarieChargesFamille": 2,
  "salarieQualitesProf": "EXCELLENT",
  "tentativesReclassement": [...],
  "prioriteReembauchePropose": true,
  "congeReclassementPropose": false,
  "dateNotification": "2026-04-01",
  "scoreCausalite": 50,
  "scoreCriteresOrdre": 100,
  "scoreReclassement": 100,
  "scoreGlobal": 83,
  "verdictRisqueRequalification": "FAIBLE",
  "criteresOrdreManquants": ["SITUATION_HANDICAP"],
  "criteresOrdreObligatoiresOk": true,
  "obligationReclassementRespectee": true,
  "baseJuridique": "Art. L.1233-3 + L.1233-4 + L.1233-5 + L.1233-45 Code du travail",
  "formule": "Causalité 50 + Critères ordre 100 + Reclassement 100 = global 83 → risque requalification FAIBLE",
  "messages": ["Priorité réembauche 1 an proposée (L.1233-45) ✓", "..."],
  "country": "FRANCE"
}
```

Codes d'erreur :
- 400 : body manquant, motif null, criteres null, tentatives null, dateNotification null, dossier hors droit du travail, workspace BE
- 404 : dossier inexistant, dossier d'un autre workspace, GET sans POST préalable

## Analyse de cohérence transversale

| Cible | Statut | Justification |
|-------|--------|---------------|
| Autres outils décisionnels DT FR | Intégrée — pattern miroir Inaptitude (F-DT-15) + LicenciementCalculator (F-DT-08) | Calculator pur statique + service upsert + controller standard |
| BE | Backlog | Pas d'équivalent direct ; régime collectif BE = Loi Renault (F-DT-14 PSE) |
| Immigration FR/BE | Non applicable | Domaine différent |
| Famille FR/BE | Non applicable | Domaine différent |
| Frontend | SF parallèle (SF-DT-13-02) | Contrat API figé ci-dessus, frontend démarre quand backend mergé |
| F-IA-04 visibility | Intégrée | 1 règle ALWAYS_ON DROIT_DU_TRAVAIL FRANCE priority 55 |

## Nouveau pattern UI ou service partagé

Aucun nouveau pattern UI (frontend hors SF). Aucun service partagé nouveau côté backend — réutilise `CurrentUserResolver`, `WorkspaceMemberRepository`, `CaseFileRepository`, `ObjectMapper` existants.

## Impact par domaine métier

- **Droit du travail** : feature **principale** — outil dédié au licenciement économique FR
- **Droit immigration** : non applicable
- **Droit famille** : non applicable
- **Pays** : FRANCE uniquement. La Belgique n'a pas d'équivalent direct mono-salarié pour le licenciement économique (le régime BE est collectif via Loi Renault — couvert par F-DT-14 ou feature dédiée). Décision documentée et tracée dans le backlog.

## Parité des domaines métier (niveau 5 — scoring)

L'outil est un **scoring de validité** (niveau 5 sur l'échelle des 7 niveaux de profondeur).

| Domaine | Équivalent existe ? | Statut |
|---------|---------------------|--------|
| Droit du travail FR | OUI (cette SF) | En cours |
| Droit du travail BE | NON (régime collectif Loi Renault — différent) | À traiter dans F-DT-14 PSE / Loi Renault |
| Immigration | NON pertinent | Concept inapplicable |
| Famille | NON pertinent | Concept inapplicable |

Pas de feature jumelle à ouvrir au backlog — la couverture BE du licenciement collectif est déjà planifiée via F-DT-14 (PSE / Loi Renault).
