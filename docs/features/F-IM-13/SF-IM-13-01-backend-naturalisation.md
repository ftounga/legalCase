# Mini-spec — F-IM-13 / SF-IM-13-01 Backend naturalisation (Code civil 21+)

## Identifiant

`F-IM-13 / SF-IM-13-01`

## Feature parente

`F-IM-13` — Naturalisation (6 voies distinctes du Code civil)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-IM-13-01-backend-naturalisation`

---

## Objectif

Évaluer la recevabilité d'une demande de naturalisation française au regard de l'une des 6 voies du Code civil (décret 21-15, déclaration mariage 21-2, déclaration ascendant 21-13-1, mineur 22-1, réintégration 24-1, opposition gouvernementale), avec verdict ELEVEE/MOYENNE/FAIBLE et liste des critères non remplis et documents à fournir.

---

## Comportement attendu

### Cas nominal

L'avocat saisit la voie envisagée, les critères clés (résidence régulière, casier vierge, ressources, assimilation, communauté de vie, âge, lien de filiation, etc.) et le calculateur applique le switch sur la `voieNaturalisation` :

1. **NATURALISATION_PAR_DECRET** (art. 21-15+ Cciv) — voie classique : 5 ans de résidence régulière (réduit à 2 ans si études supérieures FR ou services exceptionnels) + assimilation (langue B1, valeurs républicaines) + moralité (casier vierge) + ressources stables. Décision discrétionnaire.
2. **DECLARATION_MARIAGE** (art. 21-2 Cciv) — conjoint étranger marié à un français depuis ≥ 4 ans (5 ans si pas de cohabitation continue ou si pas de résidence régulière FR ≥ 3 ans), assimilation, communauté de vie matérielle et affective.
3. **DECLARATION_ASCENDANT_FRANCAIS** (art. 21-13-1 Cciv) — étranger ≥ 65 ans ascendant direct d'un français + ≥ 25 ans de résidence régulière en France.
4. **NATURALISATION_MINEUR** (art. 22-1 Cciv) — mineur étranger dont un parent acquiert la nationalité française, vit avec ce parent à titre habituel.
5. **REINTEGRATION** (art. 24-1+ Cciv) — ancien français réintégrant la nationalité après l'avoir perdue.
6. **OPPOSITION_GOUVERNEMENTALE** (art. 21-4 / 27-2 Cciv) — voie d'évaluation du risque d'opposition gouvernementale dans le délai d'1 an.

Verdict :
- **ELEVEE** si tous les critères de la voie + critères communs (présence régulière, casier vierge, pas d'opposition active) sont remplis.
- **MOYENNE** si la voie est ouverte mais avec conditions limites (ressources juste suffisantes, durée juste atteinte, langue à valider).
- **FAIBLE** si critère bloquant (durée résidence insuffisante, casier non vierge, pas de communauté de vie pour mariage, mineur ne vit pas avec parent acquéreur).

Sortie : `verdictRecevabilite`, `voieRecommandee`, `criteresNonRemplis` (List<String>), `delaiInstructionMois` (12-24 selon voie : décret ≈18, déclaration ≈12, mineur ≈6), `documentsAFournir` (List<String>), `baseJuridique` (Cciv art. 21-15/21-2/21-13-1/22-1/24-1+), `formule`, `messages`.

Persistance 1:1 par `case_file_id` (upsert).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Body absent | Message "Corps de requête requis" | 400 |
| `voieNaturalisation` null/vide | Message "voieNaturalisation est requise" | 400 |
| `voieNaturalisation` valeur non supportée | Message "Voie non supportée" | 400 |
| `dureeResidenceReguliereAnnees` négatif | Message "doit être ≥ 0" | 400 |
| `dureeMariageAnnees` négatif | Message "doit être ≥ 0" | 400 |
| `ageDemandeur` négatif | Message "doit être ≥ 0" | 400 |
| Workspace BELGIQUE | Message "régime Code civil propre à la France" | 400 |
| Domaine ≠ DROIT_IMMIGRATION | Message "ce dossier n'est pas droit immigration" | 400 |
| Case file d'un autre workspace | Message "Case file not found" | 404 |
| GET sans POST préalable | Message "Aucune analyse trouvée" | 404 |

---

## Impact par domaine métier

- **Droit du travail (FR + BE)** : non applicable — naturalisation n'est pas une matière du droit du travail.
- **Immigration FR** : c'est exactement la cible de cette feature (Code civil français).
- **Immigration BE** : non couvert ici. La naturalisation belge relève du Code de la nationalité belge (CNB), procédure et conditions distinctes (déclaration de nationalité art. 12bis CNB ; naturalisation par procédure parlementaire — quasi-disparue ; nationalité par mariage — pas le même délai). **Feature jumelle backlog : `F-IM-13-BE` Naturalisation belge** (CNB art. 12bis et s.). Ne pas mélanger les deux régimes dans le même outil — invariant "un outil = une situation".
- **Famille FR + BE** : non applicable — bien que la naturalisation par mariage touche au mariage, c'est un acte d'état civil/nationalité, pas un acte de droit de la famille.

---

## Parité des domaines métier (niveau 5 — scoring)

Cet outil est un **scoring de recevabilité** (niveau 5). À comparer aux autres scorings livrés :

| Domaine | Scoring équivalent existant | Pays |
|---------|-----------------------------|------|
| Droit du travail | F-DT-08 Validité licenciement, F-DT-09 Validité rupture conv. | FR + BE |
| Immigration | F-IM-12 Régularisation/admission exceptionnelle, F-IM-09 AES, F-IM-10 Passeport talent | FR |
| Famille | F-FA-13 Validité mesure protection, F-FA-15 etc. | FR + BE |

**Décision parité** : la naturalisation française est un scoring spécifique au Code civil français. L'équivalent belge (CNB) sera couvert par **F-IM-13-BE** ouvert au backlog. Aucun équivalent en droit du travail ou famille (régime juridique distinct, pas de transposition).

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier immigration FR** : F-IM-08 OQTF (éloignement, distinct), F-IM-09 AES (régularisation, distinct), F-IM-10 Passeport talent (titre de séjour, distinct), F-IM-11 Changement de statut (transition titre, distinct), F-IM-12 Régularisation (autre admission exceptionnelle, distinct), F-IM-16 procedures detection.
- [x] **Pays** : Couvre uniquement la **France** (Code civil français). Belgique : équivalent CNB (Code de la nationalité belge) → backlog F-IM-13-BE. JUSTIFICATION : régime juridique radicalement différent (CNB art. 12bis = déclaration directe sans décret).
- [x] **Domaine** : DROIT_IMMIGRATION uniquement.
- [x] **UI patterns** : N/A — backend seul, le frontend SF-IM-13-02 sera traité dans une SF dédiée.
- [x] **Flows transversaux** : Pas de touche auth / workspace / plans / nav. Endpoint suit le pattern AES/ChangementStatut (POST + GET sur `/api/v1/case-files/{id}/naturalisation-analysis`).

### Niveaux de vérification

- [x] **Record / DTO backend** : Request, Response, Result.
- [x] **Service / logique métier** : NaturalisationService gates + délégation.
- [x] **Entité JPA + schéma DB** : `naturalisation_analyses` avec UNIQUE case_file_id + result_data JSON.
- [x] **Tests existants** : pattern ChangementStatut (≥ 18 unit + ≥ 7 IT).

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : sera intégrée dans la SF frontend (les champs `voieNaturalisation`, `dureeResidence` sont sources IA).
- [x] **Refresh dashboard (F-IA-02)** : SF frontend.
- [x] **Pré-remplissage IA** : SF frontend (ImmigrationExtractedData).
- [x] **Persistance des inputs** : colonnes dédiées + result_data JSON.
- [x] **Masquage conditionnel** : visibility rule ALWAYS_ON pour DROIT_IMMIGRATION + FRANCE (priority 73).
- [x] **Alertes actives** : SF frontend.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-IM-11 changement statut | Oui | Pattern de référence |
| F-IM-12 régularisation | Oui | Pattern complémentaire (autre voie d'admission) |
| Belgique équivalent | Non immédiat | Backlog F-IM-13-BE (CNB art. 12bis) |
| F-IM-16 detection IA | Oui | Code procédure `NATURALISATION` à ajouter dans une SF future |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (FR uniquement par scope Code civil)
- [x] Subfeature(s) parallèle(s) créée(s) pour les cibles restantes : SF-IM-13-02 (frontend)
- [x] Backlog : F-IM-13-BE (naturalisation belge CNB) — distinct car procédure radicalement différente

---

## Critères d'acceptation

- [ ] POST `/api/v1/case-files/{id}/naturalisation-analysis` calcule et persiste l'analyse avec verdict + criteresNonRemplis + documentsAFournir.
- [ ] Switch sur `voieNaturalisation` couvre les 6 voies (DECRET, MARIAGE, ASCENDANT, MINEUR, REINTEGRATION, OPPOSITION).
- [ ] Verdict ELEVEE quand tous critères de la voie + critères communs OK.
- [ ] Verdict MOYENNE quand voie ouverte mais conditions limites (durée juste atteinte).
- [ ] Verdict FAIBLE quand critère bloquant (durée insuffisante / casier / pas de communauté de vie / mineur sans parent).
- [ ] Workspace BELGIQUE → 400.
- [ ] Workspace DROIT_DU_TRAVAIL → 400.
- [ ] Case file d'un autre workspace (cross-isolation) → 404.
- [ ] GET retourne l'analyse persistée.
- [ ] Migration 171 crée la table + UNIQUE + visibility rule.
- [ ] Tests : ≥ 18 unitaires + ≥ 7 IT.

---

## Périmètre

### Hors scope

- Frontend (SF-IM-13-02 dédiée).
- Belgique (F-IM-13-BE backlog).
- Génération automatique du dossier de demande.
- Suivi de l'instruction préfectorale (CaseDeadline traité en F-IM-16).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| country | "FRANCE" | Imposé par gate workspace |
| result_data | "{}" puis JSON sérialisé | rempli au compute |
| created_at / updated_at | now() | @PrePersist |

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs | Notes |
|-------|-------------|------------------|-------|
| `voieNaturalisation` | Oui | enum string : DECRET / MARIAGE / ASCENDANT / MINEUR / REINTEGRATION / OPPOSITION | |
| `dureeResidenceReguliereAnnees` | Non | int ≥ 0 | requis pour DECRET / ASCENDANT / MARIAGE |
| `dureeMariageAnnees` | Non | int ≥ 0 | requis pour MARIAGE |
| `cohabitationContinue` | Non | Boolean | impacte le délai 4/5 ans pour MARIAGE |
| `ageDemandeur` | Non | int ≥ 0 | requis pour ASCENDANT (≥ 65) |
| `ascendantDirectFrancais` | Non | Boolean | requis pour ASCENDANT |
| `parentAcquiertNationalite` | Non | Boolean | requis pour MINEUR |
| `vitAvecParentAcquereur` | Non | Boolean | requis pour MINEUR |
| `ancienFrancais` | Non | Boolean | requis pour REINTEGRATION |
| `casierJudiciaireVierge` | Non | Boolean (default true) | critère commun moralité |
| `assimilationLangueB1` | Non | Boolean | critère DECRET / MARIAGE |
| `ressourcesStables` | Non | Boolean | critère DECRET |
| `oppositionGouvernementaleActive` | Non | Boolean (default false) | bloque tout |
| `etudesSuperieuresFrance` | Non | Boolean | réduit DECRET à 2 ans |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/naturalisation-analysis` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/naturalisation-analysis` | Oui | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `naturalisation_analyses` | createTable + UNIQUE case_file_id | nouvelle |
| `decision_tool_visibility_rules` | INSERT | F-IM-13-naturalisation, ALWAYS_ON, DROIT_IMMIGRATION, FRANCE, priority 73 |

### Migration Liquibase

- [x] Oui — `171-create-naturalisation-analyses.xml`

### Pas de modification `legal_referentials`

- Cette SF n'introduit aucune nouvelle entrée dans la table `legal_referentials` ni dans une classe `*Referentiel.java`. Pas de risque sur `LegalReferentialDescriptionIntegrityIT`.

---

## Plan de test

### Tests unitaires (≥ 18 — `NaturalisationCalculatorTest`)

1. DECRET 5 ans + langue B1 + casier OK + ressources OK → ELEVEE
2. DECRET 2 ans + études supérieures FR → ELEVEE (durée réduite)
3. DECRET 4 ans (sous seuil 5 ans, sans études) → FAIBLE
4. DECRET sans assimilation langue B1 → MOYENNE (avec critère non rempli)
5. DECRET sans ressources stables → MOYENNE
6. MARIAGE 4 ans + cohabitation continue + assimilation OK → ELEVEE
7. MARIAGE 4 ans sans cohabitation continue → FAIBLE (5 ans requis)
8. MARIAGE 5 ans sans cohabitation continue + assimilation OK → ELEVEE
9. MARIAGE 3 ans (sous seuil) → FAIBLE
10. ASCENDANT 65 ans + 25 ans résidence + ascendant direct → ELEVEE
11. ASCENDANT 64 ans (sous seuil âge) → FAIBLE
12. ASCENDANT 65 ans + 20 ans résidence (sous seuil) → FAIBLE
13. MINEUR parent acquiert + vit avec lui → ELEVEE
14. MINEUR parent acquiert mais ne vit pas avec lui → FAIBLE
15. MINEUR sans parent acquéreur → FAIBLE
16. REINTEGRATION ancien français → ELEVEE
17. REINTEGRATION jamais français → FAIBLE
18. OPPOSITION gouvernementale active → FAIBLE pour toute voie
19. Casier non vierge → FAIBLE pour toute voie
20. Voie inconnue → IllegalArgumentException
21. dureeResidence négative → IllegalArgumentException
22. ageDemandeur négatif → IllegalArgumentException
23. baseJuridique contient l'article correspondant à la voie

### Tests d'intégration (≥ 7 — `NaturalisationControllerIT`)

1. POST FR DECRET nominal → 200 verdict ELEVEE
2. POST FR DECRET durée insuffisante → 200 verdict FAIBLE
3. POST FR MARIAGE 4 ans cohabitation → 200 verdict ELEVEE
4. POST FR ASCENDANT âge 65 + 25 ans → 200 verdict ELEVEE
5. POST FR MINEUR sans parent → 200 verdict FAIBLE
6. POST FR REINTEGRATION → 200 verdict ELEVEE
7. POST workspace BELGIQUE → 400
8. POST domaine DROIT_DU_TRAVAIL → 400
9. POST cross-workspace → 404
10. POST upsert (2 fois) → remplace
11. GET après POST → 200
12. GET sans POST → 404
13. POST voie inconnue → 400

### Isolation workspace

- [x] Applicable — un user FR ne peut accéder à un dossier BE → 404.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Outil décisionnel métier** — création nouvel outil

### Liste des outils décisionnels scannés

| Outil | Pattern | Décision |
|-------|---------|----------|
| F-IM-09 AES | single-FR + verdict | déjà séparé |
| F-IM-10 Passeport talent | référentiel pièces | indépendant |
| F-IM-11 Changement de statut | single-FR + switch transition | pattern de référence |
| F-IM-12 Régularisation | single-FR + switch motif | pattern complémentaire |

Verdict : F-IM-13 = **une situation = un outil** (naturalisation française au regard du Code civil). Pas de mélange. L'équivalent BE ouvre une feature jumelle au backlog. Conforme à l'invariant F-DT-08/F-DT-10.

### Smoke tests E2E

- [x] Aucun (pas de modif auth/workspace/nav)

---

## Dépendances

### Subfeatures bloquantes

- Aucune.

### Questions ouvertes

- Aucune impactée.

---

## Notes et décisions

- **Délai d'instruction** : valeurs constantes par voie (DECRET 18 mois, MARIAGE 12 mois, ASCENDANT 12 mois, MINEUR 6 mois, REINTEGRATION 12 mois, OPPOSITION 12 mois).
- **Critères communs** : casier vierge + pas d'opposition gouvernementale active = critères transversaux qui forcent FAIBLE quel que soit la voie.
- **Discrétion gouvernementale** : pour DECRET, la décision finale est discrétionnaire — l'outil donne un score de recevabilité technique, pas une garantie. À mentionner dans `messages`.
- **Visibility rule UUID** : `f1a04001-0000-0000-0000-ee0000000171` (numérotation alignée sur la migration).
- **Tool ID** : `F-IM-13-naturalisation`.
- **Pas de seed `legal_referentials`** : l'outil opère sur des entrées textuelles validées par le calculateur, pas sur un référentiel externe.
