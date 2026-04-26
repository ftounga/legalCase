# Mini-spec — F-FA-24 / SF-FA-24-09 Backend partage successoral

## Identifiant

`F-FA-24 / SF-FA-24-09`

## Feature parente

`F-FA-24` — Droit des successions (chantier 9 SF backend + frontend déjà livrées : SF-01/02 dévolution, SF-03/04 testament, SF-05/06 donation, SF-07/08 réserve héréditaire).

## Statut

`in-progress`

## Date de création

2026-04-26

## Branche Git

`feat/SF-FA-24-09-backend-partage-successoral`

---

## Objectif

Calculator + endpoint d'analyse de la **modalité de partage successoral** (FR — art. 815-840 Cciv + 1364 CPC) après dévolution : amiable, judiciaire ou partiel — avec verdict de recevabilité, mode recommandé pouvant basculer (amiable → judiciaire si désaccord), délai d'instruction, frais estimés et risque de licitation si bien indivisible.

---

## Comportement attendu

### Cas nominal

L'avocat saisit le mode de partage demandé (`PARTAGE_AMIABLE` / `PARTAGE_JUDICIAIRE` / `PARTAGE_PARTIEL`), le nombre de cohéritiers, la présence d'immeubles, les consentements de tous les héritiers, l'accord sur les évaluations, l'éventuel désaccord persistant et la date du décès. L'outil retourne :
- `verdictRecevabilite` : `ELEVEE` / `MOYENNE` / `FAIBLE`,
- `modeRecommande` (peut basculer amiable → judiciaire),
- `delaiInstructionMois` (3 amiable / 6-18 judiciaire),
- `fraisEstimesPct` (1-3 % des biens),
- `risqueLicitation` (true si bien indivisible et désaccord persistant),
- `baseJuridique` (« Art. 815-840 Cciv + 1364 CPC »),
- `formule` lisible et `messages` pédagogiques.

### Trois modalités (art. 815, 835, 838, 840 Cciv)

| Mode | Conditions | Forme |
|------|-----------|-------|
| `PARTAGE_AMIABLE` | Tous les cohéritiers consentent (art. 835), pas de désaccord persistant | Acte sous seing privé sauf si immeubles → notaire obligatoire |
| `PARTAGE_JUDICIAIRE` | Désaccord persistant ou l'un des cohéritiers est protégé/absent (art. 840) | TJ ordonne — expertise notariale (art. 1364 CPC) |
| `PARTAGE_PARTIEL` | Sur seulement certains biens (art. 838), le reste demeure en indivision | Acte sous seing privé / notarié selon biens partagés |

### Bascule amiable → judiciaire

Si `modePartageDemande = PARTAGE_AMIABLE` mais `desaccordPersistant = true` ou `consentementsTous = false` → `modeRecommande = PARTAGE_JUDICIAIRE` + verdict `MOYENNE` + message pédagogique.

### Cas d'erreur

- Champs obligatoires manquants → 400.
- `nombreCoheritiers < 2` → 400 (partage sans objet).
- `dateDeces` future → 400.
- Pays workspace ≠ FRANCE → 400 (outil single-country, l'équivalent BE relève d'une feature jumelle).
- Domaine workspace ≠ DROIT_FAMILLE → 400.
- Dossier hors workspace → 404.

---

## Critères d'acceptation vérifiables

1. Endpoint `POST /api/v1/case-files/{caseFileId}/partage-successoral-analysis` accepte le body documenté et retourne 200 + JSON conforme.
2. Endpoint `GET /api/v1/case-files/{caseFileId}/partage-successoral-analysis` retourne la dernière analyse persistée (404 sinon).
3. La table `partage_successoral_analyses` contient une ligne unique par `case_file_id` (UNIQUE).
4. Verdict `ELEVEE` si amiable + tous consentements + pas de désaccord (immeubles → notaire signalé).
5. Verdict `MOYENNE` si bascule amiable → judiciaire ou partiel.
6. Verdict `FAIBLE` si critères obligatoires manquants (cohéritiers < 2 → 400 mais autres défauts comme désaccord total + judiciaire mal préparé → FAIBLE).
7. `risqueLicitation = true` si `presenceImmeubles && desaccordPersistant`.
8. `fraisEstimesPct` ∈ [0.01 ; 0.03].
9. Pays BE → 400 ; domaine ≠ DROIT_FAMILLE → 400.
10. Migration `188-create-partage-successoral-analyses.xml` insère la règle `decision_tool_visibility_rules` (`F-FA-24-partage-successoral`, ALWAYS_ON, DROIT_FAMILLE, FRANCE, priority 97).

---

## Plan de test

### Unitaires (Calculator) — 17 tests

1. amiable + tous consentements + pas désaccord → ELEVEE
2. amiable + immeubles + consentements → ELEVEE + message notaire
3. amiable mais consentements partiels → bascule judiciaire + MOYENNE
4. amiable mais désaccord persistant → bascule judiciaire + MOYENNE
5. judiciaire + désaccord motivé → ELEVEE
6. judiciaire + désaccord + immeuble → ELEVEE + risqueLicitation true
7. partiel → MOYENNE + message indivision résiduelle
8. délai instruction 3 mois si amiable
9. délai instruction 6-18 mois si judiciaire
10. frais 1 % si amiable simple
11. frais 3 % si judiciaire avec immeubles
12. base juridique contient « 815 », « 840 », « 1364 »
13. country FRANCE normalized
14. country BELGIQUE → IllegalArgumentException
15. validation modePartageDemande null → IAE
16. validation nombreCoheritiers < 2 → IAE
17. validation dateDeces future → IAE

### Intégration (Controller) — 9 tests

1. POST FR DROIT_FAMILLE nominal → 200 ELEVEE
2. POST FR amiable + désaccord → 200 MOYENNE + bascule
3. POST FR judiciaire + immeuble + désaccord → 200 risqueLicitation true
4. POST workspace BE → 400
5. POST DROIT_DU_TRAVAIL → 400
6. POST autre workspace → 404
7. POST nombreCoheritiers = 1 → 400
8. POST upsert (replace)
9. GET après POST → 200 ; GET sans POST → 404

### Isolation workspace

Test cross-workspace 404 inclus.

---

## Tables / endpoints / composants impactés

### Backend
- Nouvelle table `partage_successoral_analyses` (UUID, FK case_files, UNIQUE case_file_id).
- Nouveaux fichiers : `PartageSuccessoralRequest`, `Response`, `Result`, `Analysis`, `Repository`, `Calculator`, `Service`, `Controller`.
- Migration Liquibase `188-create-partage-successoral-analyses.xml`.
- Nouvelle règle `decision_tool_visibility_rules` (UUID `f1a04001-0000-0000-0000-ee0000000188`, tool_id `F-FA-24-partage-successoral`, ALWAYS_ON, DROIT_FAMILLE, FRANCE, priority 97).

### Endpoints
- `POST /api/v1/case-files/{caseFileId}/partage-successoral-analysis`
- `GET  /api/v1/case-files/{caseFileId}/partage-successoral-analysis`

### Frontend
Hors périmètre (sera SF-FA-24-10 frontend).

---

## Hors périmètre

- Composant Angular (sera SF-FA-24-10 frontend).
- Calcul des soultes (calcul détaillé des compensations entre lots) — possible feature future.
- Régime BE (CJ art. 1207 et s.) — feature jumelle au backlog.
- Action en partage forcée par un seul indivisaire (art. 815 al. 1) — incluse implicitement dans le cas judiciaire.
- Procédure d'attribution préférentielle (art. 831) — déjà couverte par F-FA-17 partage judiciaire.

---

## Contrat API (figé pour SF parallèle frontend)

### Request body (POST)

```json
{
  "modePartageDemande": "PARTAGE_AMIABLE | PARTAGE_JUDICIAIRE | PARTAGE_PARTIEL",
  "nombreCoheritiers": 3,
  "consentementsTous": true,
  "presenceImmeubles": false,
  "accordsValuation": true,
  "desaccordPersistant": false,
  "dateDeces": "2025-06-15"
}
```

### Response (200)

```json
{
  "caseFileId": "uuid",
  "verdictRecevabilite": "ELEVEE | MOYENNE | FAIBLE",
  "modeRecommande": "PARTAGE_AMIABLE | PARTAGE_JUDICIAIRE | PARTAGE_PARTIEL",
  "delaiInstructionMois": 3,
  "fraisEstimesPct": 0.015,
  "fraisEstimesEur": 0,
  "risqueLicitation": false,
  "scoreEligibilite": 85,
  "country": "FRANCE",
  "baseJuridique": "Art. 815-840 Cciv + 1364 CPC",
  "formule": "Mode demandé ... → mode recommandé ...",
  "messages": ["..."]
}
```

### Codes d'erreur

- 400 : champs manquants, nombreCoheritiers < 2, dateDeces future, country ≠ FRANCE, domaine ≠ DROIT_FAMILLE.
- 404 : case file inconnu ou hors workspace, GET sans analyse préalable.

---

## Analyse de cohérence transversale

### Outils décisionnels du même domaine (DROIT_FAMILLE FR)
- F-FA-17 partage judiciaire — outil distinct (post-procédure judiciaire avec PV de difficultés). **Pas de fusion** : F-FA-17 traite la procédure judiciaire stricto sensu, F-FA-24-09 traite le **choix de la modalité** entre amiable / judiciaire / partiel en amont.
- F-FA-24-01 dévolution — pré-requis (savoir qui hérite avant de partager).
- F-FA-24-07 réserve héréditaire — distinct (calcul de la réserve / quotité disponible avant action en réduction).

### Pays
- FRANCE seul — équivalent BE (CJ art. 1207, partage judiciaire devant juge de paix) à ouvrir au backlog comme feature jumelle.

### UI patterns
- Hors périmètre (frontend différent).

### Nouveau pattern UI ou service partagé
- Aucun composant partagé créé. Suit le pattern existant des autres outils décisionnels DROIT_FAMILLE.

---

## Impact par domaine métier

- **Droit du travail** : non applicable (succession est exclusivement famille).
- **Immigration** : non applicable.
- **Famille FR** : pertinent (cette SF).
- **Famille BE** : feature jumelle au backlog (équivalent CJ art. 1207).

## Parité des domaines métier

Cet outil relève du **niveau 5 (scoring)** et **niveau 6 (comparateur de modalités)**. Vu qu'il est domaine-spécifique (succession = famille uniquement), pas d'équivalent pertinent en travail/immigration. Parité concerne uniquement la dimension **pays** (FR vs BE) — feature jumelle BE au backlog.

---

## Préoccupations transversales

- Auth / Principal : pattern OidcUser+Principal standard (cf. PartageJudiciaireService).
- Workspace context : gate FRANCE + DROIT_FAMILLE.
- Plans / limites : non applicable (outil décisionnel sans coût IA).
- Navigation / routing : aucune.
- Outil décisionnel métier : oui — scan effectué (cf. ci-dessus).
