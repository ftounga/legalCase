# Mini-spec — F-218 / SF-218-53 — Droit à la déconnexion : conformité — backend

## Identifiant

`F-218 / SF-218-53`

## Feature parente

`F-218d` — Temps de travail / congés FR-only (P3 Travail FR — différé signal terrain, réactivé)

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-218-53-droit-deconnexion-conformite-backend`

---

## Objectif

Analyser la **conformité de l'employeur à l'obligation relative au droit à la déconnexion** (art. L.2242-17 7° CT) : pour les entreprises d'au moins 50 salariés dotées d'au moins un délégué syndical, le droit à la déconnexion doit être négocié dans le cadre de la négociation annuelle obligatoire (NAO) sur l'égalité professionnelle et la qualité de vie et des conditions de travail (QVCT) ; à défaut d'accord, l'employeur élabore une charte, après avis du CSE, définissant les modalités d'exercice du droit à la déconnexion et prévoyant des actions de formation et de sensibilisation. Produit une **checklist de conformité**. **Analyseur conformité**. Aucun outil existant ne couvre le droit à la déconnexion (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/droit-deconnexion-conformite-analysis`
- Body :
  - `effectif` (int, requis, > 0) — effectif de l'entreprise
  - `delegueSyndicalPresent` (boolean, requis) — au moins un délégué syndical est désigné
  - `accordOuChartePresent` (boolean, requis) — un accord ou une charte sur le droit à la déconnexion existe
  - `plagesDeconnexionDefinies` (boolean, requis) — des plages / modalités de déconnexion sont définies
  - `actionsSensibilisation` (boolean, requis) — des actions de formation / sensibilisation sont prévues
  - `avisCseRecueilliPourCharte` (boolean, requis) — l'avis du CSE a été recueilli avant l'élaboration de la charte (le cas échéant)
- Analyzer `DroitDeconnexionConformiteAnalyzer` :
  - **Applicabilité** : l'obligation de négocier s'applique si `effectif >= 50` ET `delegueSyndicalPresent = true`. Si `effectif < 50` OU `delegueSyndicalPresent = false` → `statut = NON_REQUIS` + note « obligation de négociation non applicable (entreprise < 50 salariés ou absence de délégué syndical) ; l'employeur reste libre d'adopter une charte (L.2242-17 7°) ».
  - **Checklist conformité** (si applicable) : `{ item, conforme, type, commentaire }` :
    - `{ negociationDroitDeconnexion / charte, conforme = accordOuChartePresent, type=OBLIGATION }`
    - `{ plagesDeconnexionDefinies, conforme = plagesDeconnexionDefinies, type=PROCEDURE }`
    - `{ actionsSensibilisation, conforme = actionsSensibilisation, type=PROCEDURE }`
    - `{ avisCsePourCharte, conforme = avisCseRecueilliPourCharte, type=PROCEDURE }` — pertinent surtout en cas de charte unilatérale ; si un accord existe, l'item est conforme par construction (note).
  - **Verdict** `statut` ∈ { `CONFORME`, `NON_CONFORME`, `NON_REQUIS` } :
    - non applicable → `NON_REQUIS`.
    - tous les items applicables conformes → `CONFORME`.
    - au moins un item obligatoire/procédure non conforme → `NON_CONFORME`.
  - `itemsNonConformes` (int).
  - `baseJuridique` : art. L.2242-17 7° CT — annoté `(à vérifier par avocat)`.
- Output persisté dans `droit_deconnexion_conformite_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/droit-deconnexion-conformite-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| un des booléens requis absent (null) | 400 |
| `effectif` ≤ 0 | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Art. L.2242-17 7° CT** — la négociation annuelle sur l'égalité professionnelle entre les femmes et les hommes et la qualité de vie et des conditions de travail (QVCT) porte notamment sur les modalités du plein exercice par le salarié de son droit à la déconnexion et la mise en place par l'entreprise de dispositifs de régulation de l'utilisation des outils numériques, en vue d'assurer le respect des temps de repos et de congé ainsi que de la vie personnelle et familiale.
- À défaut d'accord, l'employeur élabore une charte, après avis du CSE, définissant les modalités de l'exercice du droit à la déconnexion et prévoyant la mise en œuvre, à destination des salariés et du personnel d'encadrement, d'actions de formation et de sensibilisation à un usage raisonnable des outils numériques.

(à vérifier par avocat)

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `effectif` | entier | `effectifEntreprise` (existant) | Réutiliser si présent |
| `accordOuChartePresent` | booléen | `chartteDeconnexionPresente` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Consolidation IA critique** : les nouveaux champs IA de cet outil sont ajoutés au **sous-record consolidé `Sf218dDetail`** (un seul sous-record `@JsonUnwrapped` partagé par les 9 outils de la vague F-218d, dans `TravailExtractedData` du record `CaseAnalysisResponse.java`) — **PAS** un sous-record dédié, afin de ne pas dépasser la limite JVM de 255 paramètres du constructeur canonical. Clés JSON HTTP inchangées (plates).

**Flag CONTEXTUAL pivot** : `droit_deconnexion_detecte` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte des signaux de droit à la déconnexion (mentions « droit à la déconnexion », « charte de déconnexion », « plages de déconnexion », « usage des outils numériques », « sollicitations hors temps de travail », « emails en dehors des heures de travail »).

---

## Critères d'acceptation

- [ ] POST `effectif=120`, `delegueSyndicalPresent=true`, accord + plages + sensibilisation + avis CSE tous true → `statut=CONFORME`, `itemsNonConformes=0`
- [ ] POST `effectif=120`, DS présent, `accordOuChartePresent=false` → `statut=NON_CONFORME`, `itemsNonConformes ≥ 1`
- [ ] POST `effectif=120`, DS présent, charte présente mais `avisCseRecueilliPourCharte=false` → `statut=NON_CONFORME`
- [ ] POST `effectif=30` (sans DS) → `statut=NON_REQUIS`
- [ ] POST `effectif=120`, `delegueSyndicalPresent=false` → `statut=NON_REQUIS`
- [ ] POST booléen requis null → 400 ; `effectif=0` → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`droit_deconnexion_detecte`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL, priority 99
- [ ] `F-DT-83-droit-deconnexion-conformite` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `DroitDeconnexionConformiteAnalyzerTest` : ≥ 6 cas (tout conforme → CONFORME, accord/charte absent → NON_CONFORME, avis CSE manquant → NON_CONFORME, effectif < 50 → NON_REQUIS, ≥50 sans DS → NON_REQUIS, comptage itemsNonConformes)
- **IT** `DroitDeconnexionConformiteControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `droit_deconnexion_conformite_analyses`
- **Migrations** : `542-create-droit-deconnexion-conformite-analyses.xml` (create) + `543-seed-droit-deconnexion-conformite-visibility.xml` (seed visibility, priority 99)
- **Endpoint** `DroitDeconnexionConformiteController` (POST + GET)
- **Service** `DroitDeconnexionConformiteService` + **Analyzer** `DroitDeconnexionConformiteAnalyzer`
- **Extension** `TravailExtractedData` : champ `chartteDeconnexionPresente` ajouté au sous-record consolidé `Sf218dDetail` + flag `droitDeconnexionDetecte` + instruction `TRAVAIL_INSTRUCTION_PART44` dans `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-54)
- Négociation annuelle obligatoire dans son ensemble (NAO — F-DT-66, situation distincte)
- Désignation du délégué syndical (F-DT-69, situation distincte)
- Forfait jours / charge de travail des cadres autonomes (situation distincte)
- Contentieux d'un manquement individuel au droit à la déconnexion (réparation, situation distincte)
