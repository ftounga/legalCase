# Mini-spec — F-243 / SF-243-01 — Backend : stade procédural du dossier

## Identifiant
`F-243 / SF-243-01`

## Feature parente
`F-243` — Stade procédural du dossier (juridiction + stade + position)

## Statut
`ready`

## Date de création
2026-05-15

## Branche Git
`feat/SF-243-01-backend`

---

## Objectif

Ajouter au dossier (`case_file`) la possibilité de stocker et exposer un stade procédural — juridiction + stade + position juridique — et fournir le référentiel des valeurs valides selon le domaine du dossier et le pays du workspace.

---

## Comportement attendu

### Cas nominal

1. **Lire le référentiel** : le client demande les valeurs possibles pour un domaine + pays → le backend retourne la liste des juridictions, stades (avec leurs juridictions parentes) et positions (avec leurs stades parents).
2. **Lire le stade d'un dossier** : le client demande le stade procédural d'un dossier → retourne les 3 codes + leurs libellés, ou `null` si non renseigné.
3. **Mettre à jour** : le client envoie une combinaison (juridiction, stade, position) → le backend valide la cohérence (combinaison existante pour le domaine du dossier + pays du workspace) puis persiste sur `case_files`. Les 3 champs sont optionnels et peuvent être remis à `null`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `domain` ou `country` invalide (endpoint référentiel) | Message explicite | 400 |
| Combinaison (juridiction/stade/position) incohérente avec le domaine du dossier | Message explicite indiquant le champ fautif | 422 |
| `stage` non rattaché à la `jurisdiction` fournie | Message explicite | 422 |
| `position` non valide pour le `stage` fourni | Message explicite | 422 |
| Dossier inexistant | — | 404 |
| Dossier d'un autre workspace | Accès refusé | 403 |
| Non authentifié | — | 401 |

---

## Contrat API (FIGÉ — importé par SF-243-02)

### Endpoint A — Référentiel des valeurs

`GET /api/v1/procedure-stage/options?domain={domain}&country={country}`

- `domain` : `DROIT_DU_TRAVAIL` | `DROIT_IMMIGRATION` | `DROIT_FAMILLE`
- `country` : `FRANCE` | `BELGIQUE`

Réponse `200` :
```json
{
  "domain": "DROIT_DU_TRAVAIL",
  "country": "FRANCE",
  "jurisdictions": [
    { "code": "CPH", "label": "Conseil de prud'hommes" }
  ],
  "stages": [
    { "code": "FOND", "label": "Bureau de jugement (fond)", "jurisdictionCode": "CPH" }
  ],
  "positions": [
    { "code": "DEMANDEUR", "label": "Demandeur (salarié)", "stageCodes": ["FOND","REFERE","DEPARTAGE","BCO"] }
  ]
}
```
- `400` si `domain`/`country` hors valeurs autorisées.

### Endpoint B — Lecture du stade procédural d'un dossier

`GET /api/v1/case-files/{id}/procedure-stage`

Réponse `200` :
```json
{
  "caseFileId": "uuid",
  "jurisdiction": "CPH",
  "jurisdictionLabel": "Conseil de prud'hommes",
  "stage": "FOND",
  "stageLabel": "Bureau de jugement (fond)",
  "position": "DEMANDEUR",
  "positionLabel": "Demandeur (salarié)"
}
```
- Champs `jurisdiction`/`stage`/`position` et leurs `*Label` valent `null` si non renseignés.
- `404` si dossier inexistant ; `403` si autre workspace ; `401` si non authentifié.

### Endpoint C — Mise à jour du stade procédural

`PATCH /api/v1/case-files/{id}/procedure-stage`

Body :
```json
{ "jurisdiction": "CPH", "stage": "FOND", "position": "DEMANDEUR" }
```
- Les 3 champs sont **nullable**. Envoyer `null` sur un champ l'efface. Effacer `jurisdiction` force `stage` et `position` à `null` (cohérence en cascade) ; effacer `stage` force `position` à `null`.
- Réponse `200` : même structure que l'endpoint B.
- `422` si combinaison incohérente (détail du champ fautif dans le message) ; `404` ; `403` ; `401`.

---

## Référentiel des valeurs (V1 — exhaustif 6 combinaisons)

> Codes stables, libellés humains. Stocké en constantes backend (`ProcedureStageCatalog`), pas en table DB. **Ce n'est pas un référentiel métier `legal_referentials`** : c'est une nomenclature procédurale technique stable (juridictions/stades/positions), non paramétrable par workspace, non éditable par un administrateur métier. D'où le nom `Catalog` et non `Referential`.

### DROIT_DU_TRAVAIL × FRANCE
- Juridictions : `CPH` (Conseil de prud'hommes), `CA_SOC` (Cour d'appel — chambre sociale), `CASS_SOC` (Cour de cassation — chambre sociale)
- Stades : `BCO` (Bureau de conciliation et d'orientation, →CPH), `FOND` (Bureau de jugement, →CPH), `REFERE` (Référé prud'homal, →CPH), `DEPARTAGE` (Audience de départage, →CPH), `APPEL` (Appel, →CA_SOC), `POURVOI` (Pourvoi en cassation, →CASS_SOC)
- Positions : `DEMANDEUR` (Demandeur — salarié), `DEFENDEUR` (Défendeur — employeur) [→BCO/FOND/REFERE/DEPARTAGE] ; `APPELANT`, `INTIME` [→APPEL] ; `DEMANDEUR_POURVOI`, `DEFENDEUR_POURVOI` [→POURVOI]

### DROIT_DU_TRAVAIL × BELGIQUE
- Juridictions : `TT` (Tribunal du travail), `CT` (Cour du travail), `CASS_BE` (Cour de cassation)
- Stades : `FOND` (→TT), `REFERE` (Référé — président, →TT), `APPEL` (→CT), `POURVOI` (→CASS_BE)
- Positions : `DEMANDEUR`, `DEFENDEUR` [→FOND/REFERE] ; `APPELANT`, `INTIME` [→APPEL] ; `DEMANDEUR_POURVOI`, `DEFENDEUR_POURVOI` [→POURVOI]

### DROIT_IMMIGRATION × FRANCE
- Juridictions : `TA` (Tribunal administratif), `CAA` (Cour administrative d'appel), `CE` (Conseil d'État), `CNDA` (Cour nationale du droit d'asile), `PREF` (Préfecture / OFII)
- Stades : `RECOURS_OQTF` (→TA), `REFERE_LIBERTE` (→TA), `REFERE_SUSPENSION` (→TA), `RECOURS_TITRE` (Recours refus titre / regroupement, →TA), `APPEL` (→CAA), `CASSATION` (→CE), `RECOURS_ASILE` (→CNDA), `DEMANDE_TITRE` (Demande admission au séjour — hors contentieux, →PREF)
- Positions : `REQUERANT` [→tous stades contentieux] ; `DEMANDEUR_TITRE` [→DEMANDE_TITRE]

### DROIT_IMMIGRATION × BELGIQUE
- Juridictions : `CCE` (Conseil du contentieux des étrangers), `CE_BE` (Conseil d'État), `OE` (Office des étrangers)
- Stades : `RECOURS_PLEIN_CONTENTIEUX` (→CCE), `REFERE_EXTREME_URGENCE` (→CCE), `CASSATION` (→CE_BE), `DEMANDE_TITRE` (→OE)
- Positions : `REQUERANT` [→stades contentieux] ; `DEMANDEUR_TITRE` [→DEMANDE_TITRE]

### DROIT_FAMILLE × FRANCE
- Juridictions : `JAF` (Juge aux affaires familiales — TJ), `CA_FAM` (Cour d'appel — chambre de la famille), `CASS_CIV1` (Cour de cassation — 1ʳᵉ chambre civile), `TJ` (Tribunal judiciaire)
- Stades : `DIVORCE_FOND` (→JAF), `MESURES_PROVISOIRES` (→JAF), `REFERE` (→JAF), `ORDONNANCE_PROTECTION` (→JAF), `APPEL` (→CA_FAM), `POURVOI` (→CASS_CIV1), `FILIATION` (→TJ), `SUCCESSION` (→TJ)
- Positions : `DEMANDEUR`, `DEFENDEUR` [→DIVORCE_FOND/MESURES_PROVISOIRES/REFERE/FILIATION/SUCCESSION] ; `REQUERANT` [→ORDONNANCE_PROTECTION] ; `APPELANT`, `INTIME` [→APPEL] ; `DEMANDEUR_POURVOI`, `DEFENDEUR_POURVOI` [→POURVOI]

### DROIT_FAMILLE × BELGIQUE
- Juridictions : `TF` (Tribunal de la famille), `CA_FAM_BE` (Cour d'appel — chambre de la famille), `CASS_BE` (Cour de cassation)
- Stades : `FOND` (→TF), `REFERE` (Référé — mesures urgentes, →TF), `APPEL` (→CA_FAM_BE), `POURVOI` (→CASS_BE)
- Positions : `DEMANDEUR`, `DEFENDEUR` [→FOND/REFERE] ; `APPELANT`, `INTIME` [→APPEL] ; `DEMANDEUR_POURVOI`, `DEFENDEUR_POURVOI` [→POURVOI]

---

## Conformité F-IA-04

- [x] **Non applicable** — justification : SF backend pure. F-243 n'est pas un outil décisionnel (pas de scoring, pas de calcul, pas de verdict) — c'est une métadonnée de saisie sur le dossier. Pas de `TOOL_REGISTRY`, pas de gate F-IA-03, pas de pré-fill IA.

---

## Critères d'acceptation

- [ ] `GET /api/v1/procedure-stage/options` retourne le bon référentiel pour les 6 combinaisons domaine × pays.
- [ ] `GET .../options` retourne `400` pour un `domain` ou `country` invalide.
- [ ] `GET /api/v1/case-files/{id}/procedure-stage` retourne les 3 champs + labels, ou `null` si non renseigné.
- [ ] `PATCH .../procedure-stage` persiste une combinaison valide et retourne la réponse à jour.
- [ ] `PATCH` retourne `422` si le stade n'appartient pas à la juridiction, si la position n'est pas valide pour le stade, ou si une valeur n'existe pas pour le domaine du dossier.
- [ ] `PATCH` avec `jurisdiction: null` remet `stage` et `position` à `null` (cascade).
- [ ] `GET`/`PATCH` retournent `403` pour un dossier d'un autre workspace, `404` si inexistant, `401` si non authentifié.
- [ ] Isolation workspace vérifiée par test d'intégration.

---

## Périmètre

### Hors scope (explicite)
- UI (couverte par SF-243-02).
- Consommation du stade procédural par la génération de conclusions (F-98, ultérieur).
- Édition du référentiel par un administrateur (valeurs en constantes, pas en DB).
- Historisation des changements de stade procédural (V2 si besoin).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `procedure_jurisdiction` | `null` | Optionnel — renseigné par l'avocat |
| `procedure_stage` | `null` | Optionnel |
| `procedure_position` | `null` | Optionnel |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Valeurs autorisées | Normalisation |
|-------|-------------|-------------|--------------------|---------------|
| `jurisdiction` | Non | 50 | Code du référentiel pour le domaine du dossier | trim, uppercase |
| `stage` | Non | 50 | Code rattaché à la `jurisdiction` | trim, uppercase |
| `position` | Non | 50 | Code valide pour le `stage` | trim, uppercase |

Notes :
- Combinaison validée côté service à chaque `PATCH`.
- Le domaine du dossier (`CaseFile.legalDomain`) et le pays du workspace (`Workspace.country`) déterminent le sous-référentiel applicable.

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle |
|---------|-----|------|------|
| GET | `/api/v1/procedure-stage/options` | Oui | MEMBER |
| GET | `/api/v1/case-files/{id}/procedure-stage` | Oui | MEMBER |
| PATCH | `/api/v1/case-files/{id}/procedure-stage` | Oui | LAWYER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_files` | ALTER (3 colonnes) + UPDATE/SELECT | `procedure_jurisdiction`, `procedure_stage`, `procedure_position` — VARCHAR(50) nullable |

### Migration Liquibase
- [x] Oui — `229-add-procedural-stage-to-case-files.xml` (changeSet `229-add-procedural-stage-to-case-files`, `addColumn` × 3 sur `case_files`)

### Classes backend
- `ProcedureStageCatalog` — constantes + lookup des valeurs valides par domaine/pays (nomenclature technique, pas `legal_referentials`).
- `ProcedureStageController` — endpoints A, B, C.
- `ProcedureStageService` — validation cohérence + persistance, isolation workspace (pattern `WorkspaceMemberRepository.findByUserAndPrimaryTrue()`).
- DTOs : `ProcedureStageOptionsResponse`, `ProcedureStageResponse`, `ProcedureStageUpdateRequest`.
- `CaseFile` — 3 nouveaux champs.

---

## Plan de test

### Tests unitaires
- [ ] `ProcedureStageCatalog` — les 6 combinaisons retournent un catalogue non vide et cohérent (chaque stage référence une juridiction existante, chaque position référence un stage existant).
- [ ] `ProcedureStageService` — combinaison valide persistée.
- [ ] `ProcedureStageService` — stade hors juridiction → exception 422.
- [ ] `ProcedureStageService` — position hors stade → exception 422.
- [ ] `ProcedureStageService` — valeur hors domaine du dossier → exception 422.
- [ ] `ProcedureStageService` — cascade : effacer `jurisdiction` efface `stage` + `position`.

### Tests d'intégration
- [ ] `GET .../options` → 200 pour chaque domaine/pays ; 400 si invalide.
- [ ] `GET .../{id}/procedure-stage` → 200 (renseigné et non renseigné).
- [ ] `PATCH .../{id}/procedure-stage` → 200 combinaison valide ; 422 combinaison invalide.
- [ ] `GET`/`PATCH` → 403 workspace différent, 404 inexistant, 401 non authentifié.

### Isolation workspace
- [x] Applicable — test : un utilisateur du workspace A ne peut ni lire ni modifier le stade procédural d'un dossier du workspace B.

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Aucune préoccupation transversale** — endpoints additifs, pas de modification de l'auth/workspace/plans/routing. Réutilise le pattern d'isolation workspace existant.

### Smoke tests E2E concernés
- [x] Aucun smoke test concerné — feature additive, pas de modification des flows auth/workspace/navigation existants.

---

## Dépendances

### Subfeatures bloquantes
- Aucune.

### Questions ouvertes impactées
- [ ] Aucune.

---

## Notes et décisions

- Référentiel en constantes Java plutôt qu'en table DB : les juridictions/stades sont stables, pas d'édition métier prévue. Si un besoin d'édition émerge → V2 avec table dédiée.
- Endpoints B et C dédiés (`/case-files/{id}/procedure-stage`) plutôt qu'extension de `CaseFileResponse` partagé : isole la feature, évite de toucher un DTO consommé partout.
- `422 Unprocessable Entity` pour les combinaisons incohérentes (la requête est bien formée mais sémantiquement invalide), `400` réservé aux paramètres malformés.
