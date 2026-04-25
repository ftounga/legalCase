# SF-DT-29-01 — Backend crédit-temps / interruption de carrière BE

> **Feature parente** : F-DT-29 Crédit-temps belge (CCT 103 + AR 29/10/1997).
> **Pays** : BELGIQUE uniquement.
> **Statut** : `In progress` — backend rétroactif (le frontend SF-DT-29-02 a été mergé en avant ; cette SF rétablit la cohérence du contrat API).
> **Branche** : `feat/SF-DT-29-01-backend-credit-temps-be`
> **Contrat API figé** : importé de `SF-DT-29-02-frontend-credit-temps-be.md` (déjà mergé).

## Objectif (1 phrase)

Fournir l'endpoint `POST + GET /api/v1/case-files/{id}/credit-temps-be-analysis` qui calcule l'éligibilité au crédit-temps belge selon les 3 régimes (AVEC_MOTIF / SANS_MOTIF / FIN_CARRIERE) prévus par la **CCT 103** (CNT, en vigueur depuis 2012) et l'**AR 29/10/1997**, en exposant l'indemnité ONEM mensuelle estimée + la durée maximale + verdict d'éligibilité + critères non remplis.

## Comportement nominal

L'avocat saisit un dossier de droit du travail BE. Le composant frontend `CreditTempsBeSectionComponent` (déjà mergé) consomme l'endpoint avec :

- `regime` : `AVEC_MOTIF` | `SANS_MOTIF` | `FIN_CARRIERE`
- `motif` (si `AVEC_MOTIF`) : `SOINS_ENFANT_LT_8_ANS` | `SOINS_PARENT_MALADE` | `FORMATION` | `AUTRE`
- `ancienneteEntrepriseMois` (entier ≥ 0)
- `tailleEntrepriseEtp` (entier ≥ 0)
- `dureeReductionType` : `CINQUIEME` | `MOITIE` | `TEMPS_PLEIN`
- `ageDemandeurAnnees` (entier ≥ 0, ≤ 75)
- `dateDemande` (`YYYY-MM-DD`)

Le service calcule :

- `eligible` (boolean dérivé : `criteresNonRemplis.isEmpty()`)
- `scoreGlobal` (0..100)
- `verdictEligibilite` : `ELEVEE` (≥ 80) | `MOYENNE` (60..79) | `FAIBLE` (< 60)
- `criteresNonRemplis` (List<String>)
- `indemniteOnemMensuelle` (EUR — estimation forfaitaire selon régime + durée réduction)
- `dureeMaximaleMois` (entier — plafond légal selon régime + motif)
- `baseJuridique`, `formule`, `messages`

L'analyse est persistée 1:1 par dossier (upsert).

## Règles de calcul

### 1. Régime `AVEC_MOTIF` (CCT 103 art. 4)

**Critères** :
- `ancienneteEntrepriseMois ≥ 24` (24 mois sur les 15 derniers mois civils)
- Si `motif = SOINS_ENFANT_LT_8_ANS` : pas de seuil ETP supplémentaire ; durée max **51 mois**
- Si `motif = SOINS_PARENT_MALADE` : durée max **12 mois**
- Si `motif = FORMATION` : durée max **36 mois**
- Si `motif = AUTRE` : durée max **36 mois** (générique)
- `motif` requis si régime `AVEC_MOTIF` (sinon critère "Motif requis pour régime AVEC_MOTIF")

**Indemnité ONEM mensuelle estimée** (forfaitaire 2026, selon `dureeReductionType`) :
- `CINQUIEME` (réduction 1/5) : ~150 €
- `MOITIE` (réduction 1/2) : ~400 €
- `TEMPS_PLEIN` (interruption complète) : ~700 €

### 2. Régime `SANS_MOTIF` (CCT 103 art. 3)

**Critères** :
- `ancienneteEntrepriseMois ≥ 24`
- `tailleEntrepriseEtp ≥ 10` (les < 10 ETP excluent SANS_MOTIF dans la pratique)
- Durée max **12 mois équivalent temps plein** (24 mois si `MOITIE`, 60 mois si `CINQUIEME` — équivalence)
- `motif` ignoré (pas requis)

**Indemnité ONEM mensuelle** : ~70 % du barème AVEC_MOTIF (réduit pour le sans motif).

### 3. Régime `FIN_CARRIERE` (CCT 103 art. 8)

**Critères** :
- `ageDemandeurAnnees ≥ 55` (60 ans est la règle générale, 55 ans pour métiers lourds — on prend 55 comme plancher)
- `ancienneteEntrepriseMois ≥ 24`
- `dureeReductionType ∈ {CINQUIEME, MOITIE}` (TEMPS_PLEIN non autorisé en fin de carrière)
- Durée jusqu'à la pension légale (plafond 120 mois = 10 ans)

**Indemnité ONEM mensuelle** :
- `CINQUIEME` : ~200 €
- `MOITIE` : ~500 €

### 4. Score global

Base 100, déductions par critère manquant :
- Ancienneté < 24 mois : -40 (bloquant)
- ETP < 10 (régime SANS_MOTIF) : -30
- Âge < 55 (régime FIN_CARRIERE) : -50 (bloquant)
- Motif requis et absent : -40 (bloquant)
- TEMPS_PLEIN sur FIN_CARRIERE : -30
- Plancher 0, plafond 100.

### 5. Verdict
- `score ≥ 80` → `ELEVEE`
- `60 ≤ score < 80` → `MOYENNE`
- `score < 60` → `FAIBLE`

## Cas d'erreur

- Body manquant → 400
- `regime` null → 400
- `motif` null si `regime = AVEC_MOTIF` → 400 (validation côté service ; côté score on déduit aussi -40)
- `ancienneteEntrepriseMois` null ou < 0 → 400
- `tailleEntrepriseEtp` null ou < 0 → 400
- `dureeReductionType` null → 400
- `ageDemandeurAnnees` null, < 0 ou > 75 → 400
- `dateDemande` null → 400
- Dossier non droit du travail → 400
- Dossier FRANCE → 400 (single-country BE)
- Dossier inexistant ou hors workspace → 404

## Critères d'acceptation

1. POST AVEC_MOTIF SOINS_ENFANT ancienneté 30 mois → `eligible=true`, `dureeMaximaleMois=51`
2. POST AVEC_MOTIF sans motif → `eligible=false`, criteresNonRemplis contient "Motif requis"
3. POST SANS_MOTIF ETP=5 → `eligible=false`, criteresNonRemplis contient "Taille entreprise ≥ 10 ETP"
4. POST FIN_CARRIERE âge 50 → `eligible=false`, criteresNonRemplis contient "Âge ≥ 55 ans"
5. POST FIN_CARRIERE TEMPS_PLEIN → `eligible=false`, criteresNonRemplis contient TEMPS_PLEIN
6. POST tout OK régime AVEC_MOTIF MOITIE → `indemniteOnemMensuelle ≈ 400`
7. POST workspace FRANCE → 400
8. POST dossier immigration → 400
9. POST autre workspace → 404
10. GET après POST → 200 + données persistées
11. GET sans POST préalable → 404
12. POST 2 fois sur le même dossier → upsert (1:1)
13. Migration 165 crée la table + 1 règle visibility F-IA-04 ALWAYS_ON DROIT_DU_TRAVAIL BELGIQUE priority 57
14. Verdict ELEVEE/MOYENNE/FAIBLE cohérent avec score
15. `delaiContestationJours` : N/A (pas de contestation forfaitaire — contestation contre la décision ONEM passe par le tribunal du travail, hors scope)

## Plan de test

### Unit tests (≥ 15 — `CreditTempsBeCalculatorTest`)

1. AVEC_MOTIF SOINS_ENFANT ancienneté 30 → eligible + duree 51
2. AVEC_MOTIF SOINS_PARENT → duree 12
3. AVEC_MOTIF FORMATION → duree 36
4. AVEC_MOTIF AUTRE → duree 36
5. AVEC_MOTIF sans motif → criteresNonRemplis "Motif requis"
6. AVEC_MOTIF ancienneté 12 mois → criteresNonRemplis "Ancienneté ≥ 24 mois"
7. SANS_MOTIF ETP=5 → criteresNonRemplis ETP
8. SANS_MOTIF ETP=20 OK → eligible
9. FIN_CARRIERE âge 50 → critère âge manquant
10. FIN_CARRIERE âge 60 OK → eligible
11. FIN_CARRIERE TEMPS_PLEIN → critère type réduction
12. FIN_CARRIERE âge < 55 → score plancher 0 ou faible
13. Indemnité CINQUIEME ≈ 150
14. Indemnité MOITIE ≈ 400
15. Indemnité TEMPS_PLEIN ≈ 700
16. Score 100 verdict ELEVEE
17. Score 60 verdict MOYENNE
18. Score 20 verdict FAIBLE

### Integration tests (≥ 7 — `CreditTempsBeControllerIT`)

1. POST tout OK BE → 200 + verdict ELEVEE
2. POST workspace FRANCE → 400
3. POST workspace immigration → 400
4. POST autre workspace → 404
5. POST regime AVEC_MOTIF sans motif → 400
6. POST upsert → 200
7. GET après POST → 200
8. GET sans POST → 404
9. POST body invalide (regime null) → 400

## Tables / endpoints / composants impactés

### Tables

- **NEW** `credit_temps_be_analyses` — entité 1:1 par dossier (UNIQUE `case_file_id`)
  - id, case_file_id, regime, motif, anciennete_entreprise_mois, taille_entreprise_etp, duree_reduction_type, age_demandeur_annees, date_demande, country, result_data (jsonb), created_at, updated_at

### Endpoints

- **POST** `/api/v1/case-files/{caseFileId}/credit-temps-be-analysis`
- **GET** `/api/v1/case-files/{caseFileId}/credit-temps-be-analysis`

### Migration

- **165**-create-credit-temps-be-analyses.xml
- + 1 INSERT `decision_tool_visibility_rules` ALWAYS_ON DROIT_DU_TRAVAIL BELGIQUE priority 57 UUID `f1a04001-0000-0000-0000-ee0000000165`

### Composants Java

- `CreditTempsBeCalculator` (logique pure statique)
- `CreditTempsBeRequest`, `CreditTempsBeResponse`, `CreditTempsBeResult` (records)
- `CreditTempsBeAnalysis` (Entity)
- `CreditTempsBeRepository` (JpaRepository)
- `CreditTempsBeService` (orchestration + serialization)
- `CreditTempsBeController` (REST)

## Hors scope

- Frontend (déjà mergé SF-DT-29-02 PR #624)
- Calcul exact de l'indemnité ONEM (les valeurs forfaitaires 2026 sont des estimations — affinement possible via lecture du barème ONEM dans une SF future)
- Workflow ONEM dynamique (saisie manuelle par l'avocat)
- Régime fin de carrière 50 ans pour métiers lourds (RCC) — hors scope, traité par feature dédiée si pertinent

## Analyse de cohérence transversale

| Cible | Statut | Justification |
|-------|--------|---------------|
| Pattern miroir AvantagesConventionnelsBe (F-DT-28) | Intégré | Calculator pur statique + service upsert + controller standard |
| FR équivalent (Compte Épargne Temps) | Non applicable | Mécanisme français différent (CET, art. L.3151-1 et s.) — hors scope V8 |
| Frontend | **Déjà mergé indépendamment** (SF-DT-29-02 PR #624) | Contrat API figé respecté |
| F-IA-04 visibility | Intégré | 1 règle ALWAYS_ON DROIT_DU_TRAVAIL BELGIQUE priority 57 |
| Référentiels métier | Non applicable | Enums gérées en code calculator |

## Impact par domaine métier

- **Droit du travail BE** : feature **principale**
- **Droit du travail FR** : non applicable (CET = mécanisme différent)
- **Famille / Immigration** : non applicable
- Pays : BE uniquement

## Parité des domaines métier (niveau 5 — scoring)

| Domaine | Équivalent existe ? | Statut |
|---------|---------------------|--------|
| Droit du travail BE | OUI (cette SF) | En cours |
| Droit du travail FR | NON (mécanisme CET différent, pas dans backlog V8) | Non bloquant |
| Immigration | Non pertinent | Concept inapplicable |
| Famille | Non pertinent | Concept inapplicable |

## Préoccupations transversales

- **Auth / Principal** : pattern `@AuthenticationPrincipal OidcUser` + `Principal` standard
- **Workspace context** : `WorkspaceMemberRepository.findByUserAndPrimaryTrue` standard
- **Plans / limites** : N/A
- **Outil décisionnel métier** : OUI — nouveau scoring niveau 5 BE. Scan effectué : F-DT-27 (motif grave BE), F-DT-28 (avantages BE) — distincts. Invariant respecté : un outil = une situation métier.
- **Smoke tests E2E** : non concernés (backend uniquement)
