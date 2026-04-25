# SF-DT-30-01 — Backend protection des représentants du personnel (FR)

> **Feature parente** : F-DT-30 Protection des représentants du personnel (V8).
> **Pays** : FRANCE uniquement (le statut protégé belge a des règles analogues mais différentes — feature jumelle BE à backloger).
> **Statut** : `In progress` — backend-first, frontend SF-DT-30-02 en vague suivante.
> **Branche** : `feat/SF-DT-30-01-backend-protection-rp`
> **Pattern de référence** : SF-DT-14-01 (PSE) — calculator pur statique + service upsert + controller standard.

## Objectif (1 phrase)

Fournir un calculateur backend qui évalue la régularité de la procédure de licenciement d'un salarié protégé selon les 4 critères structurants du Code du travail (art. L.2411-1 et s.) : statut protégé, période de protection (mandat + 6 mois), procédure d'autorisation de l'inspection du travail suivie, motif de licenciement.

## Comportement nominal

L'utilisateur saisit dans un dossier de droit du travail FR :
- Statut protégé (`statutProtege` enum) — `MEMBRE_CSE_TITULAIRE`, `MEMBRE_CSE_SUPPLEANT`, `DELEGUE_SYNDICAL`, `REPRESENTANT_SECTION_SYNDICALE`, `CONSEILLER_PRUDHOMMES`, `CONSEILLER_SALARIE`, `DEFENSEUR_SYNDICAL`, `MEDECIN_TRAVAIL`, `MEMBRE_CHSCT_HISTORIQUE`
- Date d'expiration du mandat (`dateExpirationMandat`)
- Date présumée de rupture (`datePresumeeRupture`)
- Procédure suivie (`procedureSuivie` enum) — `AUTORISATION_OBTENUE`, `AUTORISATION_REFUSEE`, `EN_COURS_INSTRUCTION`, `AUCUNE_DEMANDE`
- Motif de licenciement (`motifLicenciement` enum) — `FAUTE_GRAVE`, `INSUFFISANCE_PRO`, `ECONOMIQUE`, `INAPTITUDE`, `AUTRE`
- Salaire mensuel brut (`salaireMensuelBrutEur`, optionnel — pour estimer indemnités)

Le service calcule :
- `salarieEncoreProtege` (boolean) — basé sur `dateExpirationMandat + 6 mois ≥ datePresumeeRupture`
- `verdictLegalite` (`VALIDE`, `CONTESTABLE`, `NUL`)
- `scoreConformite` (0-100)
- `criteresRemplis` / `criteresManquants` (listes)
- `indemniteForfaitaireMinEur` (≥ 6 × salaire mensuel si verdict NUL)
- `salaireEvictionPotentielEur` (estimation si verdict NUL)
- Messages contextuels

L'analyse est persistée 1:1 par dossier (upsert) et exposée via GET.

## Règles de calcul

### 1. Période de protection
- Salarié protégé pendant le mandat **+ 6 mois après expiration** (L.2411-1 al. 2)
- `salarieEncoreProtege = (datePresumeeRupture ≤ dateExpirationMandat + 6 mois)`
- Si `salarieEncoreProtege = false` → procédure de droit commun applicable, l'outil retourne :
  - `verdictLegalite = VALIDE`
  - `scoreConformite = 100`
  - Message : "Salarié hors période de protection — procédure de droit commun applicable"

### 2. Si salarié encore protégé — règles de validité

**Verdict** :
- `NUL` (réintégration de droit + indemnités) si :
  - `procedureSuivie == AUCUNE_DEMANDE` ET salarié encore protégé
  - OU `procedureSuivie == AUTORISATION_REFUSEE` ET licenciement prononcé quand-même
- `CONTESTABLE` si `procedureSuivie == EN_COURS_INSTRUCTION` (licenciement prononcé avant la fin de l'instruction = irrégulier)
- `VALIDE` si `procedureSuivie == AUTORISATION_OBTENUE`

**Score de conformité** (0-100) :
- 100 si `AUTORISATION_OBTENUE`
- 30 si `EN_COURS_INSTRUCTION`
- 0 si `AUCUNE_DEMANDE` ou `AUTORISATION_REFUSEE`

**Indemnités si NUL** :
- `indemniteForfaitaireMinEur = 6 × salaireMensuelBrutEur` (L.2422-1) — réintégration de droit
- `salaireEvictionPotentielEur` = estimation des salaires correspondant à la période d'éviction (depuis `datePresumeeRupture`)
- Dommages-intérêts complémentaires possibles (signalés en messages)

### 3. Critères évalués
- `STATUT_PROTEGE_VALIDE` : statut listé par L.2411-1 et s. (toujours rempli si statut non null)
- `PROCEDURE_AUTORISATION_DEMANDEE` : `procedureSuivie ≠ AUCUNE_DEMANDE`
- `AUTORISATION_OBTENUE` : `procedureSuivie == AUTORISATION_OBTENUE`
- `LICENCIEMENT_HORS_PROCEDURE_EN_COURS` : `procedureSuivie ≠ EN_COURS_INSTRUCTION`

### 4. Délai de contestation
- 2 mois pour contester un refus d'autorisation devant le tribunal administratif (R.2422-1)
- Constante : `DELAI_CONTESTATION_JOURS = 60`

### Messages
- Confirmation chaque critère rempli avec article de loi
- Alerte chaque critère manquant
- Message critique si verdict NUL (réintégration de droit, indemnités)

## Cas d'erreur

- Body manquant → 400
- `statutProtege` null → 400
- `dateExpirationMandat` null → 400
- `datePresumeeRupture` null → 400
- `procedureSuivie` null → 400
- `motifLicenciement` null → 400
- Dossier non droit du travail → 400
- Dossier non FR (workspace BELGIQUE) → 400 (pays non couvert par la SF)
- Dossier inexistant ou hors workspace → 404

## Critères d'acceptation

1. POST avec `AUTORISATION_OBTENUE` + salarié protégé → `verdictLegalite = VALIDE`, score 100
2. POST avec `AUCUNE_DEMANDE` + salarié protégé → `verdictLegalite = NUL`, indemnité ≥ 6 × salaire
3. POST avec `AUTORISATION_REFUSEE` → `verdictLegalite = NUL`
4. POST avec `EN_COURS_INSTRUCTION` → `verdictLegalite = CONTESTABLE`, score 30
5. POST avec mandat expiré + 7 mois → message "hors période de protection", `verdictLegalite = VALIDE` par défaut
6. POST avec mandat actif → `salarieEncoreProtege = true`
7. POST avec date d'expiration = aujourd'hui - 6 mois → encore protégé (limite haute incluse)
8. POST sur dossier BE → 400
9. POST sur dossier immigration → 400
10. POST sur dossier d'un autre workspace → 404
11. GET après POST retourne les données persistées
12. GET sans POST préalable → 404
13. POST 2 fois sur le même dossier upsert l'analyse (1:1)
14. La règle de visibilité ALWAYS_ON DROIT_DU_TRAVAIL FRANCE priority 58 est ajoutée (F-IA-04)

## Plan de test

### Unit tests (≥ 18 — `ProtectionRpCalculatorTest`)
1. `autorisation_obtenue_motif_faute_grave` → VALIDE score 100
2. `aucune_demande_pendant_protection` → NUL
3. `autorisation_refusee_licenciement_prononce` → NUL
4. `en_cours_instruction` → CONTESTABLE
5. `salarie_hors_periode_protection` (mandat expiré + 7 mois) → message "hors protection" + verdict VALIDE par défaut
6. `periode_6_mois_post_mandat` — calcul exact depuis `dateExpirationMandat`
7. `statut_cse_titulaire_vs_suppleant` — même protection
8. `delegue_syndical_protege`
9. `conseiller_prudhommes_protege`
10. `medecin_travail_protege_specifique`
11. score 100 si VALIDE
12. score 30 si CONTESTABLE
13. score 0 si NUL
14. `indemniteForfaitaire_min_6_mois_salaire`
15. `salaire_eviction_calcul`
16. `criteresNonRemplis_explicite_si_AUCUNE_DEMANDE`
17. `baseJuridique_contient_L2411`
18. `messages_alerte_si_NUL`
19. validation IllegalArgumentException si `statutProtege` null
20. validation IllegalArgumentException si `dateExpirationMandat` null

### Integration tests (≥ 7 — `ProtectionRpControllerIT`)
1. POST tout OK FR → 200 + verdict VALIDE
2. POST aucune demande → 200 + verdict NUL
3. POST autorisation refusée → 200 + NUL
4. POST en cours instruction → 200 + CONTESTABLE
5. POST workspace BE → 400 (single-country FR)
6. POST workspace immigration → 400
7. POST autre workspace → 404
8. POST upsert → mise à jour
9. GET après POST → 200 données persistées
10. GET sans POST → 404
11. POST body invalide (`statutProtege` null) → 400

### Isolation workspace
- Test cross-workspace : un user d'un workspace différent reçoit 404 sur l'endpoint d'un dossier qui ne lui appartient pas (vérifié par les patterns `WorkspaceMemberRepository.findByUserAndPrimaryTrue`).

## Tables / endpoints / composants impactés

### Tables
- **NEW** `protection_rp_analyses` — entité 1:1 par dossier (UNIQUE constraint `case_file_id`)
  - Champs : id, case_file_id, statut_protege, date_expiration_mandat, date_presumee_rupture, procedure_suivie, motif_licenciement, salaire_mensuel_brut_eur, country, result_data, created_at, updated_at

### Endpoints
- **POST** `/api/v1/case-files/{caseFileId}/protection-rp-analysis` — calcul + upsert
- **GET** `/api/v1/case-files/{caseFileId}/protection-rp-analysis` — retour de l'analyse persistée

### Migration
- **166**-create-protection-rp-analyses.xml
- + 1 INSERT `decision_tool_visibility_rules` ALWAYS_ON DROIT_DU_TRAVAIL FRANCE priority 58 UUID `f1a04001-0000-0000-0000-ee0000000166` tool_id `'F-DT-30-protection-rp'`

### Composants Java
- `ProtectionRpCalculator` (logique pure, statique)
- `ProtectionRpRequest`, `ProtectionRpResponse`, `ProtectionRpResult` (records)
- `ProtectionRpAnalysis` (Entity)
- `ProtectionRpRepository` (JpaRepository)
- `ProtectionRpService` (orchestration + serialization)
- `ProtectionRpController` (REST)

## Ce qui est hors périmètre

- **Frontend** : SF-DT-30-02 (vague suivante, contrat API figé ci-dessous)
- **Belgique** : statut protégé belge (loi du 19 mars 1991 sur les délégués du personnel) — règles analogues mais différentes (procédure CPP/Tribunal du travail). Feature jumelle à backloger.
- **Génération automatique de la requête à l'inspection du travail** : SF future (générateur niveau 2)
- **Calcul détaillé indemnité de préavis** : couvert par F-DT-25 (déjà existant)
- **Workflow inspection du travail dynamique** : pas de mise à jour temps réel — saisie manuelle par l'avocat

## Contrat API (importé par SF-DT-30-02 frontend)

### POST `/api/v1/case-files/{caseFileId}/protection-rp-analysis`

Request body :
```json
{
  "statutProtege": "MEMBRE_CSE_TITULAIRE",
  "dateExpirationMandat": "2026-09-30",
  "datePresumeeRupture": "2026-04-15",
  "procedureSuivie": "AUTORISATION_OBTENUE",
  "motifLicenciement": "FAUTE_GRAVE",
  "salaireMensuelBrutEur": 3500
}
```

Enums :
- `statutProtege` : `MEMBRE_CSE_TITULAIRE`, `MEMBRE_CSE_SUPPLEANT`, `DELEGUE_SYNDICAL`, `REPRESENTANT_SECTION_SYNDICALE`, `CONSEILLER_PRUDHOMMES`, `CONSEILLER_SALARIE`, `DEFENSEUR_SYNDICAL`, `MEDECIN_TRAVAIL`, `MEMBRE_CHSCT_HISTORIQUE`
- `procedureSuivie` : `AUTORISATION_OBTENUE`, `AUTORISATION_REFUSEE`, `EN_COURS_INSTRUCTION`, `AUCUNE_DEMANDE`
- `motifLicenciement` : `FAUTE_GRAVE`, `INSUFFISANCE_PRO`, `ECONOMIQUE`, `INAPTITUDE`, `AUTRE`

Response 200 :
```json
{
  "caseFileId": "uuid",
  "salarieEncoreProtege": true,
  "scoreConformite": 100,
  "verdictLegalite": "VALIDE",
  "criteresRemplis": ["STATUT_PROTEGE_VALIDE", "PROCEDURE_AUTORISATION_DEMANDEE", "AUTORISATION_OBTENUE", "LICENCIEMENT_HORS_PROCEDURE_EN_COURS"],
  "criteresManquants": [],
  "indemniteForfaitaireMinEur": 0,
  "salaireEvictionPotentielEur": 0,
  "delaiContestationJours": 60,
  "baseJuridique": "Art. L.2411-1 + L.2411-3 + L.2411-22 + L.2422-1 + R.2422-1 Code du travail",
  "formule": "Salarié protégé + autorisation IT obtenue → verdict VALIDE",
  "messages": ["Statut protégé reconnu (L.2411-1) ✓", "Autorisation IT obtenue ✓", "..."],
  "country": "FRANCE"
}
```

Codes d'erreur :
- 400 : body manquant, champ obligatoire null, dossier hors droit du travail, workspace BE
- 404 : dossier inexistant, dossier d'un autre workspace, GET sans POST préalable

## Analyse de cohérence transversale

| Cible | Statut | Justification |
|-------|--------|---------------|
| Autres outils décisionnels DT FR | Intégrée — pattern miroir SF-DT-14-01 (PSE) | Calculator pur statique + service upsert + controller standard |
| BE | Backlog — feature jumelle | Statut protégé belge (loi 19 mars 1991 sur les délégués du personnel) — procédure CPP / Tribunal du travail. À ajouter au backlog comme feature jumelle après merge. |
| Immigration FR/BE | Non applicable | Domaine différent |
| Famille FR/BE | Non applicable | Domaine différent |
| Frontend | SF parallèle (SF-DT-30-02) | Contrat API figé ci-dessus, frontend démarre quand backend mergé |
| F-IA-04 visibility | Intégrée | 1 règle ALWAYS_ON DROIT_DU_TRAVAIL FRANCE priority 58 |
| Référentiels métier | Non applicable | Pas de nouveau type ajouté à `legal_referentials` (les enums sont gérées en code calculator) |

## Nouveau pattern UI ou service partagé

Aucun nouveau pattern UI (frontend hors SF). Aucun service partagé nouveau côté backend — réutilise `CurrentUserResolver`, `WorkspaceMemberRepository`, `CaseFileRepository`, `ObjectMapper` existants. Le calculator pur statique suit le pattern établi par F-DT-13 / F-DT-14 / F-DT-15 / F-DT-16.

## Impact par domaine métier

- **Droit du travail** : feature **principale** — outil dédié à la protection des représentants du personnel FR
- **Droit immigration** : non applicable
- **Droit famille** : non applicable
- **Pays** : FRANCE uniquement. La Belgique a un régime analogue (loi 19 mars 1991) mais différent en seuils, autorités et procédure. Une feature jumelle BE doit être ajoutée au backlog (idéalement F-DT-30-BE ou similaire).

## Parité des domaines métier (niveau 5 — scoring)

L'outil est un **scoring de validité procédurale** (niveau 5 sur l'échelle des 7 niveaux de profondeur).

| Domaine | Équivalent existe ? | Statut |
|---------|---------------------|--------|
| Droit du travail FR | OUI (cette SF) | En cours |
| Droit du travail BE | NON (statut protégé belge — loi 19 mars 1991) | **À ouvrir au backlog comme feature jumelle** |
| Immigration | NON pertinent | Concept inapplicable |
| Famille | NON pertinent | Concept inapplicable |

Action requise : ouvrir au backlog une feature jumelle BE après merge de cette SF.

## Préoccupations transversales

- **Auth / Principal** : non — réutilise le pattern `@AuthenticationPrincipal OidcUser` + `Principal` standard, identique aux F-DT-13/14
- **Workspace context** : non — réutilise `WorkspaceMemberRepository.findByUserAndPrimaryTrue` standard
- **Plans / limites** : non — pas de quota appliqué à cet outil
- **Navigation / routing** : non (backend uniquement)
- **Outil décisionnel métier** : OUI — un nouveau scoring niveau 5 ajouté. Scan effectué :
  - F-DT-13 (Licenciement économique) — distinct (motif économique, pas statut)
  - F-DT-14 (PSE) — distinct (collectif ≥10)
  - F-DT-15 (Inaptitude) — distinct (motif inaptitude)
  - F-DT-16 (Licenciement nul) — distinct (causes générales)
  - Protection RP = situation **statutaire** (procédure spéciale autorisation IT) → **invariant respecté : un outil = une situation métier**
- **Smoke tests E2E** : non concernés (backend uniquement, pas de changement d'auth/routing)
