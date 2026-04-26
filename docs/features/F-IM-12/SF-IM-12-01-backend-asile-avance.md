# Mini-spec — F-IM-12 / SF-IM-12-01 Backend asile avancé (Dublin III, accélérée, réexamen, apatride, protection subsidiaire)

## Identifiant

`F-IM-12 / SF-IM-12-01`

## Feature parente

`F-IM-12` — Asile avancé (Dublin III, accélérée, réexamen, apatride)

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-IM-12-01-backend-asile-avance`

---

## Objectif

Fournir un outil décisionnel backend qui évalue, sur 5 dispositifs distincts du droit d'asile français
(Dublin III, procédure accélérée, réexamen, apatridie, protection subsidiaire), la recevabilité d'une
demande, les délais d'instruction, les recours possibles, les documents requis et les risques de refus.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre un dossier `DROIT_IMMIGRATION` rattaché à un workspace `FRANCE`.
2. Il appelle `POST /api/v1/case-files/{id}/asile-avance-analysis` avec un `dispositifAsile` parmi
   `DUBLIN_III`, `PROCEDURE_ACCELEREE`, `REEXAMEN`, `APATRIDIE`, `PROTECTION_SUBSIDIAIRE` + critères
   propres au dispositif.
3. Le service applique la logique du dispositif :
   - **DUBLIN_III** : pivot `empreintesEurodacAutresEm`. Si vrai, transfert probable (verdict
     `RECEVABLE_TRANSFERT`), délai 6 mois (ou 18 si en fuite). Sinon, France compétente.
   - **PROCEDURE_ACCELEREE** : pivot `paysOrigineDansListeSurs` (ou réexamen / fraude / refus
     empreintes). Verdict `ACCELEREE_APPLICABLE` avec délai 1.5 mois.
   - **REEXAMEN** : pivot `elementsNouveaux` + `dateDecisionAnterieure` postérieure. Si oui,
     `RECEVABLE_REEXAMEN` (délai 0.3 mois = 8 jours), sinon `IRRECEVABLE`.
   - **APATRIDIE** : pivot `motifsExclusion`. Si vrai → `IRRECEVABLE`. Sinon `RECEVABLE_APATRIDIE`,
     délai 12 mois.
   - **PROTECTION_SUBSIDIAIRE** : pivot `traitementsGravesEtablis` + `motifsExclusion`. Si traitements
     établis et pas d'exclusion → `RECEVABLE_PROTECTION_SUBSIDIAIRE`, délai 18 mois.
4. La réponse contient `verdictRecevabilite`, `delaiInstructionMois`, `recoursPossible`,
   `documentsRequis`, `risqueRefus`, `baseJuridique`, `formule`, `messages`.
5. L'analyse est persistée 1:1 par dossier (UNIQUE `case_file_id`), upsert au prochain POST.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `dispositifAsile` absent ou vide | Message "dispositifAsile est requis" | 400 |
| `dispositifAsile` inconnu | Message "Dispositif d'asile non supporté : X" | 400 |
| Workspace `BELGIQUE` | Message "Régime d'asile propre à la France (CESEDA Livre V)" | 400 |
| Dossier non `DROIT_IMMIGRATION` | Message "Ce dossier n'est pas un dossier de droit de l'immigration" | 400 |
| Dossier d'un autre workspace | Message "Case file not found" | 404 |
| GET sans POST préalable | Message "Aucune analyse d'asile avancé trouvée pour ce dossier" | 404 |
| Body absent sur POST | Message "Corps de requête requis" | 400 |
| `REEXAMEN` sans `dateDecisionAnterieure` | Message critère manquant + verdict IRRECEVABLE | 200 |

---

## Analyse de cohérence transversale

### Périmètres scannés

- **Autres outils métier** : `NaturalisationCalculator` (FR-only single-domain), `MineursImmigrationCalculator`,
  `OqtfAvecDelai`/`OqtfSansDelai` — tous des outils d'immigration FR. Pattern aligné : 1 dispositif = 1 voie.
- **Autres pays** : Belgique exclue — la procédure d'asile en BE relève du CGRA + Loi du 15/12/1980,
  régime distinct (sera traité dans une feature jumelle backlog F-IM-12-BE si besoin métier avéré).
- **Autres domaines** : non applicable (asile = pure immigration).
- **Autres UI patterns** : la SF backend ne touche pas la UI, mais la SF-IM-12-02 frontend devra suivre
  le pattern canonique (pré-fill IA + F-IA-03).

### Niveaux de vérification couverts

- [x] DTO Request / Response / Result (records)
- [x] Entity JPA + colonnes dédiées (5 dispositifs + flags pivots)
- [x] Service avec gates `DROIT_IMMIGRATION` + `FRANCE` + isolation workspace
- [x] Tests unitaires Calculator (≥ 18) + IT Controller (≥ 7)
- [x] Migration Liquibase 173 avec UNIQUE case_file_id + visibility ALWAYS_ON

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : oui — sera consommée par la SF frontend (signaux comparant
      `dispositifAsile` aux questions IA et au type de procédure détectée).
- [x] **Refresh dashboard (F-IA-02)** : oui — la SF frontend appellera `triggerRefresh()`.
- [x] **Pré-remplissage IA** : oui — la SF frontend implémentera `prefillFromAi()` à partir des
      données extraites (ex. `nationalite_origine`, `procedure_detectee=ASILE`).
- [x] **Persistance des inputs** : oui — colonnes dédiées pour tous les flags pivots + `result_data` JSON.
- [x] **Masquage conditionnel** : pris en charge par F-IA-04 via la règle visibility ALWAYS_ON
      (DROIT_IMMIGRATION + FRANCE + priorité 75).
- [x] **Alertes actives après calcul** : la SF frontend respectera le gate `!showForm()`.

### Cas spécifique : nouveau pattern UI ou service partagé

Aucun nouveau pattern partagé introduit côté backend (réutilise le pattern Naturalisation /
MineursImmigration : record DTO + Entity 1:1 + Service gates + Controller path-versioned + migration
Liquibase + visibility rule).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern Calculator FR-only | Oui | Réutilisé tel quel — délégation au service NaturalisationService |
| Belgique (CGRA / Loi 1980) | Non (cette SF) | Backlog F-IM-12-BE si besoin métier (régime distinct) |
| Autres domaines (travail, famille) | Non | Asile = pure immigration |
| F-IA-04 visibility | Oui | Règle ALWAYS_ON injectée dans la migration |
| F-IA-03 cohérence | Oui — SF frontend | Pas de couplage dans cette SF backend |
| F-IA-02 refresh | Oui — SF frontend | Pas de couplage dans cette SF backend |

### Décision

- [x] Étendu à toutes les cibles applicables côté backend dans cette subfeature
- [x] Subfeature parallèle frontend SF-IM-12-02 à créer après merge de cette SF
- [x] Backlog F-IM-12-BE si besoin métier futur

---

## Impact par domaine métier

Cette feature est **spécifique au domaine droit de l'immigration**. Elle ne s'applique ni au droit du
travail ni au droit de la famille (régimes séparés). Elle est **single-country FRANCE** : le régime
belge d'asile (CGRA + Loi 15/12/1980) sera couvert par une feature jumelle au backlog (F-IM-12-BE) si
le besoin métier est confirmé. Pas d'impact transversal.

## Parité des domaines métier

Outil de niveau **5 (scoring / analyse de validité)** — verdict `RECEVABLE_*` / `IRRECEVABLE` selon
critères pivots. Asile n'a **pas d'équivalent** en droit du travail ou en droit de la famille (concept
juridique strictement immigration). Pas de feature jumelle nécessaire dans les autres domaines.

---

## Critères d'acceptation

- [x] `POST /api/v1/case-files/{id}/asile-avance-analysis` retourne 200 + verdict pour les 5 dispositifs.
- [x] Le verdict suit la table métier (Dublin III / PA / réexamen / apatridie / protection subsidiaire).
- [x] `delaiInstructionMois` correspond au dispositif (Dublin 6 ou 18, PA 1.5, réexamen 0.3, apatridie
      12, protection subsidiaire 18).
- [x] `baseJuridique` cite `Règlement UE 604/2013` pour Dublin III ou `CESEDA L.512+/L.531+` selon dispositif.
- [x] `recoursPossible` indique recours suspensif 15 j devant le TA pour Dublin III, sinon CNDA.
- [x] Workspace `BELGIQUE` → 400 avec message clair.
- [x] Dossier `DROIT_DU_TRAVAIL` → 400.
- [x] Dossier d'un autre workspace → 404 (isolation stricte).
- [x] `dispositifAsile` inconnu → 400.
- [x] `GET` après `POST` retourne la même analyse persistée.
- [x] `POST` est idempotent (upsert sur `case_file_id`).
- [x] La règle visibility `f1a04001-0000-0000-0000-ee0000000173` est insérée par la migration 173.

---

## Périmètre

### Hors scope (explicite)

- Frontend Angular (SF-IM-12-02 séparée).
- Régime belge (backlog F-IM-12-BE).
- Recours juridictionnels concrets (génération de mémoires) — relève d'une autre feature.
- Mises à jour automatiques de la liste OFPRA pays sûrs (référentiel statique pour cette SF).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `result_data` | `"{}"` | sérialisation du résultat à la sauvegarde |
| `country` | `"FRANCE"` | imposé par le gate workspace |
| `created_at` / `updated_at` | now() | géré par `@PrePersist` / `@PreUpdate` |

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs |
|-------|-------------|-----------------|
| `dispositifAsile` | Oui | enum DUBLIN_III, PROCEDURE_ACCELEREE, REEXAMEN, APATRIDIE, PROTECTION_SUBSIDIAIRE — case-insensitive |
| `dateDecisionAnterieure` | Conditionnel (REEXAMEN) | LocalDate ISO |
| `elementsNouveaux` | Conditionnel (REEXAMEN) | Boolean tristate |
| `paysOrigineDansListeSurs` | Conditionnel (PROCEDURE_ACCELEREE) | Boolean tristate |
| `empreintesEurodacAutresEm` | Conditionnel (DUBLIN_III) | Boolean tristate |
| `motifsExclusion` | Conditionnel | Boolean tristate |
| `traitementsGravesEtablis` | Conditionnel (PROTECTION_SUBSIDIAIRE) | Boolean tristate |

---

## Technique

### Endpoints

| Méthode | URL | Auth |
|---------|-----|------|
| POST | `/api/v1/case-files/{caseFileId}/asile-avance-analysis` | Oui (OAuth2) |
| GET  | `/api/v1/case-files/{caseFileId}/asile-avance-analysis` | Oui (OAuth2) |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `asile_avance_analyses` | CREATE | nouvelle table 1:1 par dossier |
| `decision_tool_visibility_rules` | INSERT | règle ALWAYS_ON DROIT_IMMIGRATION FRANCE priorité 75 |

### Migration Liquibase

`backend/src/main/resources/db/changelog/migrations/173-create-asile-avance-analyses.xml` :
- table avec colonnes pour chaque flag pivot + `dispositif_asile` + `country` + `result_data`
- UNIQUE constraint sur `case_file_id`
- INSERT visibility rule UUID `f1a04001-0000-0000-0000-ee0000000173`, tool_id `'F-IM-12-asile-avance'`.

---

## Plan de test

### Tests unitaires (≥ 18)

- DUBLIN_III : empreintes EURODAC autres EM → RECEVABLE_TRANSFERT, délai 6.
- DUBLIN_III : pas d'empreintes ailleurs → FRANCE_COMPETENTE.
- DUBLIN_III : en fuite → délai 18.
- PROCEDURE_ACCELEREE : pays sûr → ACCELEREE_APPLICABLE, délai 1.5 (1 ou 2 selon arrondi).
- PROCEDURE_ACCELEREE : pas un pays sûr → ACCELEREE_NON_APPLICABLE.
- REEXAMEN : éléments nouveaux + date postérieure → RECEVABLE_REEXAMEN.
- REEXAMEN : pas d'éléments nouveaux → IRRECEVABLE.
- REEXAMEN : date manquante → critère non rempli + IRRECEVABLE.
- APATRIDIE : sans motifs d'exclusion → RECEVABLE_APATRIDIE, délai 12.
- APATRIDIE : motifs d'exclusion → IRRECEVABLE.
- PROTECTION_SUBSIDIAIRE : traitements graves OK + pas d'exclusion → RECEVABLE_PROTECTION_SUBSIDIAIRE, délai 18.
- PROTECTION_SUBSIDIAIRE : pas de traitements graves → IRRECEVABLE.
- PROTECTION_SUBSIDIAIRE : motifs d'exclusion → IRRECEVABLE.
- Dispositif inconnu → IllegalArgumentException.
- Dispositif null → IllegalArgumentException.
- Dispositif blank → IllegalArgumentException.
- Case-insensitive (`dublin_iii` accepté).
- Formule contient le dispositif et le verdict.
- Documents requis non vides pour chaque dispositif.
- Risque de refus est une List non null.
- baseJuridique cite Règlement 604/2013 pour Dublin et CESEDA pour les autres.

### Tests d'intégration (≥ 7)

- POST FR DUBLIN_III nominal → 200 RECEVABLE_TRANSFERT.
- POST FR PROCEDURE_ACCELEREE pays sûr → 200 ACCELEREE_APPLICABLE.
- POST FR REEXAMEN sans éléments nouveaux → 200 IRRECEVABLE.
- POST FR APATRIDIE → 200 RECEVABLE_APATRIDIE.
- POST FR PROTECTION_SUBSIDIAIRE → 200 RECEVABLE_PROTECTION_SUBSIDIAIRE.
- POST workspace BELGIQUE → 400.
- POST dossier DROIT_DU_TRAVAIL → 400.
- POST dossier autre workspace (cross) → 404.
- POST dispositif inconnu → 400.
- POST upsert (2x) → seconde analyse remplace.
- GET après POST → renvoie analyse persistée.
- GET sans POST → 404.

### Isolation workspace

- [x] Applicable — un user FR ne peut pas POST sur un dossier d'un autre workspace (404).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — nouvel endpoint isolé sous un nouveau path
      `asile-avance-analysis`. Pas de modification d'auth, workspace context, plans, navigation.

### Smoke tests E2E concernés

- [x] Aucun (la SF backend n'introduit pas de route frontend ni de modification d'un flow critique).

---

## Dépendances

### Subfeatures bloquantes

- F-IA-04 (mergée) — la migration ajoute une règle dans `decision_tool_visibility_rules`.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- Pattern de référence : `NaturalisationCalculator` (PR #639) et `MineursImmigrationCalculator` (PR #642).
- Single-country FR cohérent avec naturalisation (régime BE différent et complexe).
- `delaiInstructionMois` modélisé en `double` interne mais exposé en `int` arrondi (sauf si on garde `double`).
  Décision : exposer `double` pour préserver `1.5` et `0.3` (REEXAMEN = 8 jours = 0.3 mois).
- UUID visibility `f1a04001-0000-0000-0000-ee0000000173` (suite immédiate de F-IM-13 = 171 et F-IM-19 = 172).
