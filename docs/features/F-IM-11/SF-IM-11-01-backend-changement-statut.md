# Mini-spec — F-IM-11 / SF-IM-11-01 Backend changement de statut (CESEDA)

## Identifiant

`F-IM-11 / SF-IM-11-01`

## Feature parente

`F-IM-11` — Changement de statut (passage d'un titre de séjour à un autre)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-IM-11-01-backend-changement-statut`

---

## Objectif

Évaluer la viabilité d'un changement de statut entre deux titres de séjour CESEDA (typiquement étudiant → salarié, visiteur → salarié, transition entre sous-catégories passeport talent), avec verdict ELEVEE/MOYENNE/FAIBLE et liste des documents/risques.

---

## Comportement attendu

### Cas nominal

L'avocat saisit le titre actuel, le titre envisagé, la durée restante sur le titre actuel, la rémunération du contrat (si applicable), la fourniture du justificatif, le casier judiciaire vierge. Le calculateur applique le switch sur le couple `(titreActuel, titreEnvisage)` :

1. **ETUDIANT → SALARIE** (L.421-1, R.5221-3) : exige rémunération ≥ 1,5 × SMIC + métier en lien + justificatif (contrat).
2. **ETUDIANT → APS** (L.422-10) : 1 an renouvelable 1 fois, recherche emploi en lien avec formation.
3. **VISITEUR → SALARIE** (L.421-1) : exige contrat + autorisation préalable.
4. **VPF → SALARIE** : VPF autorise déjà le travail — pas de procédure formelle, signalé en informationnel.
5. **PASSEPORT_TALENT_SALARIE_QUALIFIE → PASSEPORT_TALENT_INNOVANT** (et autres transitions intra-talent) : changement de sous-catégorie, conditions documentaires propres.
6. **VPF → ETUDIANT** : retour formation, conditions de ressources.

Verdict :
- **ELEVEE** si tous critères + transition admise par CESEDA.
- **MOYENNE** si transition possible mais conditions limites (ex : rémunération entre 1,0 et 1,5 SMIC pour passage salarié sortie études).
- **FAIBLE** si critère bloquant (durée restante < 2 mois → renvoi vers renouvellement classique, casier non vierge, justificatif absent).

Sortie : `verdictTransition`, `nouveauTitreEnvisage`, `documentsRequis`, `delaiInstructionMois`, `risqueRefus`, `baseJuridique`, `formule`, `messages`.

Persistance 1:1 par `case_file_id` (upsert).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Body absent | Message "Corps de requête requis" | 400 |
| `titreActuel` ou `titreEnvisage` null/vide | Message "titre actuel/envisagé requis" | 400 |
| Transition non encadrée (ex : ETUDIANT → ETUDIANT) | Message "Transition non supportée" | 400 |
| `dureeRestanteSurTitreActuelMois` négatif | Message "doit être ≥ 0" | 400 |
| `remunerationContratEur` négatif | Message "doit être ≥ 0" | 400 |
| Workspace BELGIQUE | Message "régime CESEDA propre à la France" | 400 |
| Domaine ≠ DROIT_IMMIGRATION | Message "ce dossier n'est pas droit immigration" | 400 |
| Case file d'un autre workspace | Message "Case file not found" | 404 |
| GET sans POST préalable | Message "Aucune analyse trouvée" | 404 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier immigration** : F-IM-05 Titre séjour (référentiel pièces — réutilisé via codes), F-IM-08 OQTF (différent — éloignement vs transition), F-IM-09 AES (transition spéciale L.435-4 → contexte différent), F-IM-10 Passeport talent (sous-catégories — réutilisées dans les transitions intra-talent), F-IM-16 procedures detection.
- [x] **Pays** : Couvre uniquement la **France** (CESEDA est code français). Belgique : équivalent OE/CGRA — feature jumelle au backlog (F-IM-11-BE), JUSTIFICATION : changement de motif de séjour BE relève de la procédure 9bis/9ter/9.1 distincte, pas de mapping 1:1.
- [x] **Domaine** : DROIT_IMMIGRATION uniquement.
- [x] **UI patterns** : N/A — backend seul, le frontend SF-IM-11-02 sera traité dans une SF dédiée.
- [x] **Flows transversaux** : Pas de touche auth / workspace / plans / nav. Endpoint suit le pattern AES existant (POST + GET sur `/api/v1/case-files/{id}/changement-statut-analysis`).

### Niveaux de vérification

- [x] **Record / DTO backend** : Request, Response, Result.
- [x] **Service / logique métier** : ChangementStatutService gates + délégation.
- [x] **Entité JPA + schéma DB** : `changement_statut_analyses` avec UNIQUE case_file_id + result_data JSON.
- [x] **Tests existants** : pattern AesMetiersTension (≥ 18 unit + ≥ 7 IT).

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : sera intégrée dans la SF frontend (les champs `titreActuel`, `nationalite` sont sources IA).
- [x] **Refresh dashboard (F-IA-02)** : SF frontend.
- [x] **Pré-remplissage IA** : SF frontend (TravailExtractedData/ImmigrationExtractedData).
- [x] **Persistance des inputs** : colonnes dédiées + result_data JSON.
- [x] **Masquage conditionnel** : visibility rule ALWAYS_ON pour DROIT_IMMIGRATION + FRANCE (priority 72).
- [x] **Alertes actives** : SF frontend.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-IM-05 référentiel pièces | Oui | Codes alignés avec ImmigrationPieceReferentiel (ETUDIANT, VISITEUR, VPF, SALARIE, TALENT_*) |
| F-IM-10 sous-catégories talent | Oui | Codes TALENT_* utilisés pour transitions intra-talent |
| F-IM-16 detection IA | Oui | Code procédure `CHANGEMENT_STATUT` déjà ajouté (migration 108) |
| Belgique équivalent | Non immédiat | Backlog F-IM-11-BE (procédure 9bis modification) |
| Autres outils Immigration FR (AES, OQTF) | Non applicable | Régimes distincts |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (FR uniquement par scope CESEDA)
- [x] Subfeature(s) parallèle(s) créée(s) pour les cibles restantes : SF-IM-11-02 (frontend)
- [x] Backlog : F-IM-11-BE (changement motif séjour Belgique) — distinct car procédure différente
- [ ] Non applicable aux autres cibles (justification explicite)

---

## Critères d'acceptation

- [ ] POST `/api/v1/case-files/{id}/changement-statut-analysis` calcule et persiste l'analyse avec verdict + documentsRequis + risques.
- [ ] Switch sur `(titreActuel, titreEnvisage)` couvre les 6 transitions principales.
- [ ] Verdict ELEVEE quand tous critères + transition admise.
- [ ] Verdict MOYENNE quand rémunération entre 1,0 et 1,5 SMIC (ETUDIANT → SALARIE).
- [ ] Verdict FAIBLE quand `dureeRestanteSurTitreActuelMois < 2`.
- [ ] Verdict FAIBLE quand `casierJudiciaireVierge = false`.
- [ ] Verdict FAIBLE quand `documentJustificatifFourni = false` pour transitions exigeantes.
- [ ] Workspace BELGIQUE → 400.
- [ ] Workspace DROIT_DU_TRAVAIL → 400.
- [ ] Case file d'un autre workspace (cross-isolation) → 404.
- [ ] GET retourne l'analyse persistée.
- [ ] Migration 170 crée la table + UNIQUE + visibility rule.
- [ ] Tests : ≥ 18 unitaires + ≥ 7 IT.

---

## Périmètre

### Hors scope

- Frontend (SF-IM-11-02 dédiée).
- Belgique (F-IM-11-BE backlog).
- Génération automatique du dossier de demande (cf. F-IM-XX backlog).
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
| `titreActuel` | Oui | non vide | enum logique : ETUDIANT, VISITEUR, VPF, SALARIE, TALENT_* |
| `titreEnvisage` | Oui | non vide | idem |
| `dureeRestanteSurTitreActuelMois` | Oui | int ≥ 0 | |
| `documentJustificatifFourni` | Non | Boolean (default false) | |
| `remunerationContratEur` | Non | BigDecimal ≥ 0 | requis pour cible SALARIE |
| `casierJudiciaireVierge` | Non | Boolean (default true sauf info contraire) | |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/changement-statut-analysis` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/changement-statut-analysis` | Oui | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `changement_statut_analyses` | createTable + UNIQUE case_file_id | nouvelle |
| `decision_tool_visibility_rules` | INSERT | F-IM-11-changement-statut, ALWAYS_ON, DROIT_IMMIGRATION, FRANCE, priority 72 |

### Migration Liquibase

- [x] Oui — `170-create-changement-statut-analyses.xml`

---

## Plan de test

### Tests unitaires (≥ 18 — `ChangementStatutCalculatorTest`)

1. ETUDIANT → SALARIE rémunération ≥ 1,5 SMIC + justificatif → ELEVEE
2. ETUDIANT → SALARIE rémunération entre 1,0 et 1,5 SMIC → MOYENNE
3. ETUDIANT → SALARIE rémunération < SMIC → FAIBLE
4. ETUDIANT → SALARIE sans justificatif → FAIBLE
5. ETUDIANT → APS → ELEVEE (conditions documentaires)
6. ETUDIANT → APS sans justificatif → FAIBLE
7. VISITEUR → SALARIE avec contrat → MOYENNE (autorisation préalable)
8. VISITEUR → SALARIE sans contrat → FAIBLE
9. VPF → SALARIE → ELEVEE avec message informationnel
10. VPF → ETUDIANT → ELEVEE
11. PASSEPORT_TALENT_SALARIE_QUALIFIE → PASSEPORT_TALENT_INNOVANT → MOYENNE
12. PASSEPORT_TALENT_SALARIE_QUALIFIE → SALARIE classique → MOYENNE
13. dureeRestanteMois < 2 → FAIBLE (renvoi renouvellement)
14. casierJudiciaireVierge = false → FAIBLE (risque refus ordre public)
15. Transition non supportée (ETUDIANT → ETUDIANT) → IllegalArgumentException
16. titreActuel null → IllegalArgumentException
17. titreEnvisage null → IllegalArgumentException
18. dureeRestante négatif → IllegalArgumentException
19. remuneration négative → IllegalArgumentException
20. baseJuridique contient L.421 et R.5221

### Tests d'intégration (≥ 7 — `ChangementStatutControllerIT`)

1. POST FR nominal ETUDIANT → SALARIE → 200 verdict ELEVEE
2. POST FR ETUDIANT → SALARIE rémunération limite → 200 verdict MOYENNE
3. POST FR durée < 2 → 200 verdict FAIBLE
4. POST workspace BELGIQUE → 400
5. POST domaine DROIT_DU_TRAVAIL → 400
6. POST cross-workspace → 404
7. POST upsert (2 fois) → remplace
8. GET après POST → 200
9. GET sans POST → 404

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
| F-IM-09 AES Métiers tension | single-FR + verdict booléen | déjà séparé, pas de mélange |
| F-IM-08 OQTF | sous-types FR + BE | déjà découpé |
| F-IM-10 Passeport talent | référentiel pièces | réutilisé via codes TALENT_* |
| F-DT-30 Protection RP | single-FR | pattern de référence |

Verdict : F-IM-11 = **une situation = un outil**. Pas de mélange. Conforme à l'invariant F-DT-08/F-DT-10.

### Smoke tests E2E

- [x] Aucun (pas de modif auth/workspace/nav)

---

## Dépendances

### Subfeatures bloquantes

- F-IM-10 SF-01 (Passeport talent sous-catégories) : Done — codes utilisés
- F-IM-16 SF-01 (extension procedures) : Done — code `CHANGEMENT_STATUT` ajouté

### Questions ouvertes

- Aucune impactée.

---

## Notes et décisions

- **SMIC 2026** : la valeur SMIC mensuelle utilisée comme référence (1,5 × SMIC) est paramétrée en constante dans le calculateur (`SMIC_MENSUEL_BRUT_EUR_2026 = 1801.80`). Si le SMIC évolue, mettre à jour la constante (idéalement à terme via `legal_referentials`).
- **Délai d'instruction** : 2-4 mois selon transition (préfecture). Constante `DELAI_INSTRUCTION_MOIS_DEFAULT = 3`.
- **APS** : 1 an renouvelable 1 fois (L.422-10) → délai instruction réduit (2 mois).
- **Champ `remunerationContratEur`** : optionnel, BigDecimal pour précision.
- **Pattern transition** : `record TransitionKey(titreActuel, titreEnvisage)` interne au calculateur pour clarté du switch.
- **Visibility rule UUID** : `f1a04001-0000-0000-0000-ee0000000170` (numérotation alignée sur la migration).
- **Tool ID** : `F-IM-11-changement-statut`.
