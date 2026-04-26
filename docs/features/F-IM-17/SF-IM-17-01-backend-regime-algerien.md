# Mini-spec — F-IM-17 / SF-IM-17-01 Backend régime algérien (accord franco-algérien 1968)

## Identifiant

`F-IM-17 / SF-IM-17-01`

## Feature parente

`F-IM-17` — Régime algérien (accord franco-algérien du 27/12/1968 + avenants 1985 / 1994 / 2001)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-IM-17-01-backend-regime-algerien`

---

## Objectif

Évaluer la recevabilité d'une demande de **certificat de résidence algérien (CRA)** ou de **regroupement familial** régie par l'accord bilatéral franco-algérien du 27/12/1968 (modifié par avenants 1985, 1994, 2001) — **régime parallèle au CESEDA**, applicable uniquement aux ressortissants algériens.

---

## Contexte juridique

Les ressortissants algériens **ne sont pas régis par le CESEDA** mais par un accord bilatéral. Cet outil couvre 5 voies distinctes :

| Code voie | Article | Description |
|-----------|---------|-------------|
| `CRA_1_AN` | art. 5 accord 1968 | 1ère demande, équivalent VPF étudiant / visiteur |
| `CRA_10_ANS_LIEN_FRANCE` | art. 6 al. 1, 2, 3 | conjoint français OU parent enfant français OU 10 ans de présence régulière |
| `CRA_10_ANS_RESIDENT_ANCIEN` | art. 7bis | ressortissant algérien né en France OU arrivé < 13 ans |
| `CHANGEMENT_VERS_TRAVAILLEUR` | art. 7 | passage CRA 1 an → CRA Travailleur |
| `REGROUPEMENT_FAMILIAL_ACCORD_1968` | art. 4 | conjoint et enfants mineurs, conditions ressources réduites vs CESEDA |

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/regime-algerien-analysis` avec un body décrivant la voie + critères.

Le service :
1. Vérifie que le dossier appartient au workspace du user (filtre `workspace_id`).
2. Vérifie que le dossier est `DROIT_IMMIGRATION` et que le workspace est `FRANCE` → sinon 400.
3. **Gate nationalité backend** : si `nationaliteAlgerienne` est explicitement `false`, rejette avec 400 (`Régime applicable uniquement aux ressortissants algériens`).
4. Délègue au `RegimeAlgerienCalculator` qui produit un `Result` (verdict ELEVEE / MOYENNE / FAIBLE).
5. Persiste l'analyse 1:1 par dossier (upsert) dans `regime_algerien_analyses`.
6. Retourne un `Response` (caseFileId, country, voie, verdict, criteresNonRemplis, documentsRequis, delaiInstructionMois, baseJuridique, formule, messages).

`GET` retourne la dernière analyse persistée, 404 sinon.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Body absent | "Corps de requête requis" | 400 |
| `voieDemande` null / blank / inconnue | "Voie non supportée" | 400 |
| `nationaliteAlgerienne == false` | "Régime applicable uniquement aux ressortissants algériens" | 400 |
| Workspace BELGIQUE | "Accord franco-algérien applicable uniquement en France" | 400 |
| Dossier hors DROIT_IMMIGRATION | "Ce dossier n'est pas un dossier de droit de l'immigration" | 400 |
| Dossier d'un autre workspace | "Case file not found" | 404 |
| GET sans POST préalable | "Aucune analyse trouvée" | 404 |
| Durées négatives | "doit être ≥ 0" | 400 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : Naturalisation (F-IM-13), Asile (F-IM-12), Mesures éloignement (F-IM-20), Mineurs immigration (F-IM-19), Changement statut (F-IM-11), AES (F-IM-09), Title décision (F-IM-05), Recours (F-IM-06). Le régime algérien est un outil **standalone et exclusif** : un dossier dont le ressortissant est algérien doit utiliser cet outil au lieu des outils CESEDA pour les CRA. Pas d'intégration avec eux ; le panel F-IA-04 décidera de l'affichage selon `aiData.nationalite === 'ALG'` (frontend, SF-IM-17-02).
- [x] **Autres pays** : Belgique → non applicable (l'accord est bilatéral FR-DZ). Visibility F-IA-04 = ALWAYS_ON DROIT_IMMIGRATION FRANCE.
- [x] **Autres domaines** : DROIT_FAMILLE / DROIT_DU_TRAVAIL → non applicable (régime de séjour pur).
- [x] **Autres UI patterns** : pré-remplissage IA + validation F-IA-03 sur `nationalite` et `voieDemande` côté frontend SF-IM-17-02.
- [x] **Autres flows transversaux** : aucun impact auth / workspace / plans (gate workspace standard).

### Classement

| Cible | Classement | Justification |
|-------|-----------|---------------|
| Naturalisation FR | non applicable | Régime de nationalité, pas de séjour. Cohabitation possible (un algérien naturalisé peut faire les 2). |
| Recours immigration FR | non applicable | Recours générique, peut s'appliquer aux décisions algériennes mais reste neutre. |
| AES / Changement statut FR | non applicable | Hors champ accord bilatéral pour les algériens (l'accord couvre tous les motifs CRA). |
| Belgique | non applicable | Accord bilatéral FR-DZ, sans équivalent BE. |
| Frontend section dédiée | **SF parallèle SF-IM-17-02** | Feature jumelle dans la même feature parente F-IM-17. |

---

## Impact par domaine métier

Cette feature est **sensible au domaine** : elle ne concerne que `DROIT_IMMIGRATION`. Elle ne s'applique pas à `DROIT_DU_TRAVAIL` ni à `DROIT_FAMILLE`. Elle n'est pas non plus disponible en Belgique (accord bilatéral FR-DZ).

---

## Parité des domaines métier

Cet outil est de **niveau 5 (scoring / arbre décisionnel)**. Pas d'équivalent attendu en droit du travail ou en famille (notion strictement de droit des étrangers / nationalité d'origine). Pour l'immigration belge, aucun accord bilatéral équivalent n'existe.

---

## Critères d'acceptation

- [x] `RegimeAlgerienCalculator.compute(...)` retourne un verdict ELEVEE / MOYENNE / FAIBLE selon la voie + critères.
- [x] Les 5 voies sont supportées : `CRA_1_AN`, `CRA_10_ANS_LIEN_FRANCE`, `CRA_10_ANS_RESIDENT_ANCIEN`, `CHANGEMENT_VERS_TRAVAILLEUR`, `REGROUPEMENT_FAMILIAL_ACCORD_1968`.
- [x] Le `baseJuridique` cite explicitement `"Accord franco-algérien 27/12/1968"` et l'article applicable.
- [x] `documentsRequis` n'est jamais vide.
- [x] `delaiInstructionMois` ∈ {3, 6} selon la voie.
- [x] Le service rejette en 400 si `nationaliteAlgerienne == false`.
- [x] Le service rejette en 400 si workspace BELGIQUE.
- [x] L'isolation workspace est respectée (test IT cross-workspace → 404).
- [x] Visibility F-IA-04 : règle `ALWAYS_ON DROIT_IMMIGRATION FRANCE` priority 77 inscrite via migration 176.

---

## Plan de test

### Unitaires (`RegimeAlgerienCalculatorTest`) — ≥ 18 tests

1. `cra1An_premiereDemande_complete_ELEVEE`
2. `cra1An_sansCasier_FAIBLE`
3. `cra1An_sansVisaLongSejour_FAIBLE`
4. `cra10AnsLienFrance_conjointFr_ELEVEE`
5. `cra10AnsLienFrance_parentEnfantFr_ELEVEE`
6. `cra10AnsLienFrance_10ansPresence_ELEVEE`
7. `cra10AnsLienFrance_aucunLien_FAIBLE`
8. `cra10AnsResidentAncien_neEnFrance_ELEVEE`
9. `cra10AnsResidentAncien_arriveAvant13_ELEVEE`
10. `cra10AnsResidentAncien_arriveApres13_FAIBLE`
11. `changementTravailleur_avecContrat_ELEVEE`
12. `changementTravailleur_sansContrat_FAIBLE`
13. `regroupement_ressourcesOk_logementOk_ELEVEE`
14. `regroupement_logementInsuffisant_MOYENNE`
15. `regroupement_ressourcesInsuffisantes_FAIBLE`
16. `voieInconnue_throws`
17. `voieNull_throws`
18. `presenceFranceNegative_throws`
19. `casInsensitive_voie`
20. `formule_containsVoieAndVerdict`
21. `delais_perVoieAreCorrect`
22. `documentEtatCivilOriginalManquant_FAIBLE`

### Intégration (`RegimeAlgerienControllerIT`) — ≥ 7 tests

1. `POST_fr_cra1AnNominal_returnsELEVEE`
2. `POST_fr_cra10ansLienFrance_returnsELEVEE`
3. `POST_fr_regroupement_returnsELEVEE`
4. `POST_workspaceBe_returns400`
5. `POST_droitDuTravail_returns400`
6. `POST_otherWorkspace_returns404` (isolation workspace)
7. `POST_nationaliteNonAlgerienne_returns400`
8. `POST_voieInconnue_returns400`
9. `POST_upsert_replacesAnalysis`
10. `GET_afterPost_returnsPersisted`
11. `GET_withoutPost_returns404`

---

## Tables / endpoints / composants impactés

### Backend

- **Nouvelle table `regime_algerien_analyses`** (1:1 par dossier).
- **Migration `176-create-regime-algerien-analyses.xml`** : table + UNIQUE `case_file_id` + visibility rule UUID `f1a04001-0000-0000-0000-ee0000000176` priority 77 tool_id `'F-IM-17-regime-algerien'`.
- **Nouveau endpoint** `POST /api/v1/case-files/{caseFileId}/regime-algerien-analysis` (calculate + upsert).
- **Nouveau endpoint** `GET /api/v1/case-files/{caseFileId}/regime-algerien-analysis` (lecture).
- **Nouveaux fichiers** :
  - `RegimeAlgerienRequest`
  - `RegimeAlgerienResponse`
  - `RegimeAlgerienResult`
  - `RegimeAlgerienAnalysis` (entité JPA)
  - `RegimeAlgerienRepository`
  - `RegimeAlgerienCalculator`
  - `RegimeAlgerienService`
  - `RegimeAlgerienController`

### Contrat API (figé pour SF-IM-17-02 frontend)

**`POST /api/v1/case-files/{caseFileId}/regime-algerien-analysis`**

Request body :
```json
{
  "voieDemande": "CRA_1_AN | CRA_10_ANS_LIEN_FRANCE | CRA_10_ANS_RESIDENT_ANCIEN | CHANGEMENT_VERS_TRAVAILLEUR | REGROUPEMENT_FAMILIAL_ACCORD_1968",
  "nationaliteAlgerienne": true,
  "documentEtatCivilOriginal": true,
  "presenceReguliereFranceMois": 0,
  "casierJudiciaireVierge": true,
  "visaLongSejourValide": true,
  "conjointFrancais": false,
  "parentEnfantFrancais": false,
  "neEnFrance": false,
  "arriveeAvant13Ans": false,
  "contratTravailValide": false,
  "ressourcesSuffisantes": null,
  "logementDecent": null,
  "nombrePersonnesFoyer": null
}
```

Response :
```json
{
  "caseFileId": "uuid",
  "country": "FRANCE",
  "voieDemande": "CRA_1_AN",
  "voieRecommandee": "Certificat de résidence algérien 1 an (art. 5)",
  "verdictRecevabilite": "ELEVEE | MOYENNE | FAIBLE",
  "titreApplicable": "CRA_1_AN",
  "dureeTitreAnnees": 1,
  "criteresNonRemplis": ["..."],
  "documentsRequis": ["..."],
  "delaiInstructionMois": 3,
  "baseJuridique": "Accord franco-algérien 27/12/1968 modifié + art. 5",
  "formule": "Régime algérien — CRA 1 an : verdict ELEVEE.",
  "messages": ["..."]
}
```

---

## Hors périmètre

- **Frontend** : géré par SF-IM-17-02 (sequentielle, après merge de cette SF).
- **Recours contre refus de CRA** : couvert par F-IM-06 (Recours immigration).
- **Belgique** : régime non applicable.
- **Acquisition nationalité française par un algérien** : couvert par F-IM-13 (Naturalisation).
- **Pré-remplissage IA / validation F-IA-03** : géré par SF frontend.
- **Visibility logique conditionnelle sur `nationalite === 'ALG'`** : à câbler frontend dans le panel F-IA-04 lors de SF-IM-17-02.

---

## Préoccupations transversales

| Préoccupation | Impact | Action |
|---------------|--------|--------|
| Auth / Principal | Aucun (réutilise `OidcUser` + `Principal`) | — |
| Workspace context | Standard (`workspaceMemberRepository.findByUserAndPrimaryTrue`) | — |
| Plans / limites | Aucun gate quota | — |
| Navigation / routing | Aucun (backend pur) | — |
| Outil décisionnel métier | **Outil neuf, situation dédiée** : un outil = une situation métier (régime franco-algérien). Pas de switch interne sur pays / domaine. | Conforme |
