# Mini-spec — F-218 / SF-218-27 — Harcèlement : procédure interne de traitement d'un signalement — backend

## Identifiant

`F-218 / SF-218-27`

## Feature parente

`F-218c` — IRP / négociation collective FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-31

## Branche Git

`feat/SF-218-27-harcelement-procedure-interne-backend`

---

## Objectif

Évaluer la **conformité de la procédure interne de traitement d'un signalement de harcèlement** côté employeur (art. L.1153-5-1, L.2314-1, L.1152-4 CT) : désignation d'un référent harcèlement sexuel au CSE, information/affichage des salariés, déclenchement d'une enquête interne contradictoire dans un délai raisonnable, mesures conservatoires et prévention. Outil **conformité employeur**, **distinct de `F-DT-11`** (qui traite la nullité du licenciement consécutif au harcèlement). Aucun outil existant ne couvre la procédure interne de signalement (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/harcelement-procedure-interne-analysis`
- Body :
  - `effectif` (int, requis) — effectif de l'entreprise (≥ 11 → CSE obligatoire avec référent harcèlement sexuel L.2314-1)
  - `referentCseDesigne` (boolean, requis) — référent harcèlement sexuel désigné parmi les membres du CSE
  - `referentEmployeurDesigne` (boolean, défaut false) — référent employeur (entreprises ≥ 250 salariés, art. L.1153-5-1)
  - `informationAffichageRealisee` (boolean, requis) — information des salariés sur les sanctions / voies de recours (L.1152-4, L.1153-5)
  - `signalementRecu` (boolean, requis) — un signalement a été reçu
  - `dateSignalement` (LocalDate, optionnel) — date de réception du signalement
  - `dateOuvertureEnquete` (LocalDate, optionnel) — date d'ouverture de l'enquête interne
  - `enqueteContradictoire` (boolean, défaut false) — enquête menée de façon contradictoire et impartiale
  - `mesuresConservatoiresPrises` (boolean, défaut false) — mesures conservatoires (éloignement, suspension) durant l'enquête
- Analyzer `HarcelementProcedureInterneAnalyzer` :
  - **Checklist de conformité** : chaque item produit `{ item, conforme (boolean), obligatoire (boolean), commentaire }`. Items :
    1. Référent CSE harcèlement sexuel désigné (obligatoire si `effectif ≥ 11`).
    2. Référent employeur désigné (obligatoire si `effectif ≥ 250`).
    3. Information / affichage des salariés réalisé (toujours obligatoire).
    4. Enquête interne déclenchée à réception d'un signalement (obligatoire si `signalementRecu=true`).
    5. Enquête contradictoire et impartiale (obligatoire si enquête ouverte).
    6. Mesures conservatoires envisagées (recommandé si `signalementRecu=true`).
  - **Délai de réaction** : si `dateSignalement` et `dateOuvertureEnquete` présents → `delaiReactionJours` = nombre de jours. `delaiRaisonnable` ∈ { `OUI`, `LIMITE`, `NON` } (OUI ≤ 15 j, LIMITE 16–60 j, NON > 60 j ou enquête non ouverte alors que signalement reçu).
  - **Verdict de conformité** :
    - tous les items obligatoires conformes → `statut = CONFORME`.
    - au moins un item obligatoire non conforme, hors enquête → `statut = NON_CONFORME`.
    - signalement reçu et enquête non déclenchée / non contradictoire / délai NON → `statut = CARENCE_GRAVE` + note « manquement à l'obligation de sécurité (L.4121-1) susceptible d'engager la responsabilité de l'employeur ».
  - `itemsObligatoiresManquants` (int) ; `risqueResponsabiliteEmployeur` ∈ { `ELEVE`, `MODERE`, `FAIBLE` } (ELEVE si CARENCE_GRAVE, MODERE si NON_CONFORME, FAIBLE si CONFORME).
  - `baseJuridique` : art. L.1153-5-1, L.2314-1, L.1152-4 CT — annoté `(à vérifier par avocat)`.
- Output persisté dans `harcelement_procedure_interne_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/harcelement-procedure-interne-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| un des booléens requis absent (null) | 400 |
| effectif ≤ 0 | 400 |
| dateOuvertureEnquete antérieure à dateSignalement | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Art. L.1153-5-1 CT** — référent harcèlement sexuel : un référent désigné par l'employeur (entreprises ≥ 250 salariés) chargé d'orienter, d'informer et d'accompagner les salariés.
- **Art. L.2314-1 CT** — désignation d'un référent en matière de lutte contre le harcèlement sexuel et les agissements sexistes parmi les membres du CSE (toute entreprise dotée d'un CSE, dès 11 salariés).
- **Art. L.1152-4 CT** — obligation de l'employeur de prévenir les agissements de harcèlement moral, d'informer et de faire cesser. L'employeur tenu à une obligation de sécurité (L.4121-1) doit diligenter une enquête à réception d'un signalement.

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `effectif` | entier | `effectifEntreprise` (existant) | Réutiliser si présent |
| `signalementRecu` | booléen | `harcelementSignalementInterne` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Flag CONTEXTUAL pivot** : `harcelement_procedure_interne_detectee` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte des signaux de procédure interne de signalement (mentions « signalement de harcèlement », « référent CSE harcèlement », « enquête interne », « alerte agissements sexistes », « obligation de prévention employeur »), distincts d'un litige de nullité de licenciement (F-DT-11).

---

## Critères d'acceptation

- [ ] POST tous items obligatoires conformes (référent CSE, affichage, enquête contradictoire, délai ≤ 15 j) → `statut=CONFORME`, `risqueResponsabiliteEmployeur=FAIBLE`
- [ ] POST `effectif=12`, `referentCseDesigne=false` → item référent CSE non conforme, `statut=NON_CONFORME`
- [ ] POST `signalementRecu=true` + `enqueteContradictoire=false` → `statut=CARENCE_GRAVE`, `risqueResponsabiliteEmployeur=ELEVE`
- [ ] POST `dateSignalement`/`dateOuvertureEnquete` à 10 j → `delaiRaisonnable=OUI` ; à 90 j → `delaiRaisonnable=NON`
- [ ] `effectif=300`, `referentEmployeurDesigne=false` → item référent employeur obligatoire non conforme
- [ ] `itemsObligatoiresManquants` compté correctement
- [ ] POST booléen requis null → 400 ; effectif=0 → 400 ; dateOuvertureEnquete < dateSignalement → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`harcelement_procedure_interne_detectee`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL
- [ ] `F-DT-59-harcelement-procedure-interne` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `HarcelementProcedureInterneAnalyzerTest` : ≥ 6 cas (tout conforme → CONFORME, référent CSE manquant → NON_CONFORME, enquête non contradictoire → CARENCE_GRAVE, délai OUI/NON, référent employeur ≥ 250 obligatoire, comptage itemsObligatoiresManquants + mapping risque)
- **IT** `HarcelementProcedureInterneControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `harcelement_procedure_interne_analyses`
- **Migrations** : `create-harcelement-procedure-interne-analyses.xml` + `seed-harcelement-procedure-interne-visibility.xml` (reconfirmer les numéros libres dans le worktree)
- **Endpoint** `HarcelementProcedureInterneController` (POST + GET)
- **Service** `HarcelementProcedureInterneService` + **Analyzer** `HarcelementProcedureInterneAnalyzer`
- **Extension** `TravailExtractedData` : champ `harcelementSignalementInterne` + flag `harcelementProcedureInterneDetectee` + prompt `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-28)
- Nullité du licenciement consécutif au harcèlement (F-DT-11, situation distincte)
- Chiffrage des dommages-intérêts pour préjudice de harcèlement
- Élections CSE / conformité des IRP (F-DT-65, situation distincte)
