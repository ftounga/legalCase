# Mini-spec — F-218 / SF-218-35 — Règlement intérieur : validité (contenu, consultation, dépôt) — backend

## Identifiant

`F-218 / SF-218-35`

## Feature parente

`F-218c` — IRP / négociation collective FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-31

## Branche Git

`feat/SF-218-35-reglement-interieur-validite-backend`

---

## Objectif

Analyser la **conformité et l'opposabilité d'un règlement intérieur** (art. L.1311-1 à L.1322-4, L.1321-1 et suivants CT) : caractère obligatoire dès **50 salariés**, présence du **contenu obligatoire** (hygiène/sécurité, discipline/échelle des sanctions, droits de la défense, harcèlement, lanceur d'alerte), absence de **clauses interdites** (atteintes aux libertés non justifiées/proportionnées, dispositions moins favorables que la loi/CCN, sanctions pécuniaires), et respect de la **procédure de mise en place** (consultation du CSE, transmission à l'inspection du travail, dépôt au greffe du CPH, affichage). **Analyseur conformité**. Aucun outil existant ne couvre le règlement intérieur (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/reglement-interieur-validite-analysis`
- Body :
  - `effectif` (int, requis) — effectif (RI obligatoire dès 50 salariés, L.1311-2)
  - `reglementExiste` (boolean, requis) — un règlement intérieur existe
  - `contenuHygieneSecurite` (boolean, requis) — mesures d'hygiène et de sécurité (L.1321-1 1°)
  - `contenuDiscipline` (boolean, requis) — règles de discipline + nature/échelle des sanctions (L.1321-1 2°)
  - `contenuDroitsDefense` (boolean, requis) — dispositions sur les droits de la défense des salariés (L.1321-1 3°)
  - `contenuHarcelementAgissements` (boolean, requis) — rappel des dispositions relatives au harcèlement et aux agissements sexistes (L.1321-2)
  - `clauseAtteinteLibertesNonJustifiee` (boolean, défaut false) — clause portant atteinte aux libertés sans justification ni proportionnalité (clause interdite, L.1321-3)
  - `clauseSanctionPecuniaire` (boolean, défaut false) — clause de sanction pécuniaire (interdite, L.1331-2)
  - `consultationCseRealisee` (boolean, requis) — CSE consulté avant mise en place (L.1321-4)
  - `transmissionInspectionTravail` (boolean, requis) — transmis à l'inspection du travail
  - `depotGreffeCph` (boolean, requis) — déposé au greffe du conseil de prud'hommes
- Analyzer `ReglementInterieurValiditeAnalyzer` :
  - **Applicabilité** : si `effectif < 50` ET `reglementExiste=false` → `statut = NON_REQUIS` (RI facultatif, note).
  - **Checklist contenu obligatoire** : `{ item, conforme, type=OBLIGATOIRE, commentaire }` (4 items : hygiène/sécurité, discipline, droits de la défense, harcèlement).
  - **Checklist clauses interdites** : `{ item, conforme (true = absence de la clause interdite), type=INTERDIT }` (atteinte aux libertés non justifiée, sanction pécuniaire) — la présence d'une clause interdite rend l'item NON conforme.
  - **Checklist procédure** : `{ item, conforme, type=PROCEDURE }` (consultation CSE, transmission inspection, dépôt greffe CPH).
  - **Verdict** `statut` ∈ { `CONFORME`, `NON_CONFORME`, `INOPPOSABLE`, `NON_REQUIS` } :
    - tout obligatoire + clauses + procédure conforme → `CONFORME`.
    - contenu obligatoire manquant ou clause interdite présente → `NON_CONFORME`.
    - défaut de procédure (consultation CSE, transmission ou dépôt manquant) → `INOPPOSABLE` + note « le règlement intérieur est inopposable aux salariés faute de respect de la procédure de mise en place (L.1321-4) ».
  - `itemsObligatoiresManquants` (int) ; `clausesInterditesPresentes` (int) ; `opposabilite` ∈ { `OPPOSABLE`, `INOPPOSABLE` } (INOPPOSABLE si procédure non respectée).
  - `baseJuridique` : art. L.1311-1 à L.1322-4 CT ; L.1321-1 et suivants CT — annoté `(à vérifier par avocat)`.
- Output persisté dans `reglement_interieur_validite_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/reglement-interieur-validite-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| un des booléens requis absent (null) | 400 |
| effectif ≤ 0 | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Art. L.1311-2 CT** — règlement intérieur obligatoire dans les entreprises d'au moins 50 salariés.
- **Art. L.1321-1 CT** — contenu obligatoire : mesures d'hygiène et de sécurité ; règles générales et permanentes relatives à la discipline (nature et échelle des sanctions) ; droits de la défense des salariés.
- **Art. L.1321-2 / L.1321-2-1 CT** — rappel des dispositions relatives au harcèlement moral, sexuel et aux agissements sexistes ; protection des lanceurs d'alerte.
- **Art. L.1321-3 CT** — clauses interdites : dispositions contraires aux lois/règlements/CCN ; atteintes aux droits et libertés non justifiées par la nature de la tâche ni proportionnées ; dispositions discriminantes.
- **Art. L.1331-2 CT** — interdiction des sanctions pécuniaires.
- **Art. L.1321-4 CT** — procédure : consultation du CSE, transmission à l'inspection du travail, dépôt au greffe du conseil de prud'hommes. À défaut, inopposabilité.

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `effectif` | entier | `effectifEntreprise` (existant) | Réutiliser si présent |
| `reglementExiste` | booléen | `reglementInterieurPresent` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Flag CONTEXTUAL pivot** : `reglement_interieur_detecte` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte des signaux de règlement intérieur (mentions « règlement intérieur », « échelle des sanctions », « clause du règlement intérieur », « dépôt au greffe », « consultation CSE règlement », « sanction disciplinaire fondée sur le règlement intérieur »).

---

## Critères d'acceptation

- [ ] POST `effectif=80`, RI complet (4 contenus obligatoires) + 0 clause interdite + procédure complète → `statut=CONFORME`, `opposabilite=OPPOSABLE`
- [ ] POST `contenuHarcelementAgissements=false` → item manquant, `statut=NON_CONFORME`, `itemsObligatoiresManquants=1`
- [ ] POST `clauseSanctionPecuniaire=true` → item interdit non conforme, `statut=NON_CONFORME`, `clausesInterditesPresentes ≥ 1`
- [ ] POST `consultationCseRealisee=false` (contenu OK) → `statut=INOPPOSABLE`, `opposabilite=INOPPOSABLE`
- [ ] POST `depotGreffeCph=false` → `INOPPOSABLE`
- [ ] POST `effectif=20`, `reglementExiste=false` → `statut=NON_REQUIS`
- [ ] POST booléen requis null → 400 ; effectif=0 → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`reglement_interieur_detecte`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL
- [ ] `F-DT-100-reglement-interieur-validite` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `ReglementInterieurValiditeAnalyzerTest` : ≥ 6 cas (complet → CONFORME, contenu obligatoire manquant → NON_CONFORME, clause interdite → NON_CONFORME, défaut consultation CSE → INOPPOSABLE, défaut dépôt greffe → INOPPOSABLE, effectif < 50 sans RI → NON_REQUIS + comptage itemsObligatoiresManquants/clausesInterditesPresentes)
- **IT** `ReglementInterieurValiditeControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `reglement_interieur_validite_analyses`
- **Migrations** : `create-reglement-interieur-validite-analyses.xml` + `seed-reglement-interieur-validite-visibility.xml` (reconfirmer les numéros libres dans le worktree)
- **Endpoint** `ReglementInterieurValiditeController` (POST + GET)
- **Service** `ReglementInterieurValiditeService` + **Analyzer** `ReglementInterieurValiditeAnalyzer`
- **Extension** `TravailExtractedData` : champ `reglementInterieurPresent` + flag `reglementInterieurDetecte` + prompt `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-36)
- Validité d'une sanction disciplinaire particulière fondée sur le RI (autre situation)
- Note de service / additif au RI (régime spécifique)
- Lanceur d'alerte (F-DT-61, situation distincte)
