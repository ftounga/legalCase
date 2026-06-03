# Mini-spec — F-218 / SF-218-43 — Congés pour évènements familiaux — backend

## Identifiant

`F-218 / SF-218-43`

## Feature parente

`F-218d` — Temps de travail / congés FR-only (P3 Travail FR — différé signal terrain, réactivé)

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-218-43-conges-evenements-familiaux-backend`

---

## Objectif

Déterminer la **durée du congé pour évènement familial** applicable (art. L.3142-1 à L.3142-5 CT) selon la nature de l'évènement (mariage/PACS, naissance/adoption, décès d'un enfant, d'un conjoint/partenaire, d'un parent, annonce d'un handicap/cancer/pathologie chronique chez un enfant), retenir la **durée la plus favorable** entre la durée légale et la durée conventionnelle (CCN), et confirmer le **maintien intégral du salaire** (ces congés sont assimilés à du temps de travail effectif). **Calculateur d'indemnité / droit à congé**. Aucun outil existant ne couvre les congés pour évènements familiaux (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/conges-evenements-familiaux-analysis`
- Body :
  - `typeEvenement` (enum, requis) ∈ { `MARIAGE_PACS`, `NAISSANCE`, `DECES_ENFANT`, `DECES_CONJOINT_PARTENAIRE`, `DECES_PERE_MERE`, `ANNONCE_HANDICAP_ENFANT`, `DEMENAGEMENT_NON_LEGAL` }
  - `conventionPlusFavorable` (boolean, requis) — la CCN prévoit une durée plus favorable que la loi
  - `dureeConventionnelleJours` (Integer, optionnel/nullable) — durée prévue par la CCN (jours), requise si `conventionPlusFavorable = true`
- Analyzer `CongesEvenementsFamiliauxAnalyzer` :
  - **Durée légale** (L.3142-4) :
    - `MARIAGE_PACS` du salarié → 4 jours
    - `NAISSANCE` ou adoption → 3 jours
    - `DECES_ENFANT` → 5 jours (porté à 7 jours ouvrés si l'enfant a moins de 25 ans, ou enfant à charge effective et permanente, ou si le salarié est parent d'un enfant de moins de 25 ans)
    - `DECES_CONJOINT_PARTENAIRE` (conjoint, partenaire PACS, concubin) → 3 jours
    - `DECES_PERE_MERE` (père, mère, beau-père, belle-mère, frère, sœur) → 3 jours
    - `ANNONCE_HANDICAP_ENFANT` (annonce d'un handicap, d'un cancer ou d'une pathologie chronique chez un enfant) → 2 jours
    - `DEMENAGEMENT_NON_LEGAL` → 0 jour légal + note « pas de congé légal pour évènement familial, le déménagement n'ouvre droit qu'à une éventuelle disposition conventionnelle »
  - **Comparaison** : si `conventionPlusFavorable = true` ET `dureeConventionnelleJours != null` ET `dureeConventionnelleJours > dureeLegaleJours` → `dureeApplicableJours = dureeConventionnelleJours`, `base = CONVENTIONNELLE` ; sinon `dureeApplicableJours = dureeLegaleJours`, `base = LEGALE`.
  - **Maintien salaire** : `maintienSalaire = true` (congé assimilé à du temps de travail effectif, pas de retenue) ; `assimileTempsTravailEffectif = true`.
  - **Verdict** : `dureeApplicableJours` (int) ; `dureeLegaleJours` (int) ; `base` ∈ { `LEGALE`, `CONVENTIONNELLE` } ; `maintienSalaire` (boolean).
  - `baseJuridique` : art. L.3142-1 à L.3142-5 CT — annoté `(à vérifier par avocat)`.
- Output persisté dans `conges_evenements_familiaux_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/conges-evenements-familiaux-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| `typeEvenement` absent ou valeur inconnue | 400 |
| `conventionPlusFavorable` absent (null) | 400 |
| `conventionPlusFavorable=true` sans `dureeConventionnelleJours` | 400 |
| `dureeConventionnelleJours` ≤ 0 (si fourni) | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Art. L.3142-1 CT** — congés pour évènements familiaux (principe).
- **Art. L.3142-4 CT** — durées légales minimales : mariage/PACS du salarié 4 jours ; naissance/adoption 3 jours ; décès d'un enfant 5 jours (7 jours ouvrés dans les cas renforcés) ; décès du conjoint, partenaire PACS, concubin, père, mère, beau-père, belle-mère, frère, sœur 3 jours ; annonce d'un handicap, d'un cancer ou d'une pathologie chronique chez un enfant 2 jours.
- **Art. L.3142-2 / L.3142-3 CT** — assimilation à du temps de travail effectif (pas de réduction de la rémunération ni des droits à congés payés).
- **Art. L.3142-5 CT** — possibilité de durées conventionnelles plus favorables (la plus favorable au salarié l'emporte).

(à vérifier par avocat)

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `typeEvenement` | enum (String) | `typeEvenementFamilial` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `dureeConventionnelleJours` | entier (nullable) | `dureeConventionnelleEvtFamilial` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Consolidation IA critique** : les nouveaux champs IA de cet outil sont ajoutés au **sous-record consolidé `Sf218dDetail`** (un seul sous-record `@JsonUnwrapped` partagé par les 9 outils de la vague F-218d, dans `TravailExtractedData` du record `CaseAnalysisResponse.java`) — **PAS** un sous-record dédié, afin de ne pas dépasser la limite JVM de 255 paramètres du constructeur canonical. Clés JSON HTTP inchangées (plates).

**Flag CONTEXTUAL pivot** : `conge_evt_familial_detecte` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte des signaux de congé pour évènement familial (mentions « congé mariage », « congé naissance », « congé décès », « congé pour évènement familial », « jours pour décès d'un proche », « annonce du handicap de l'enfant »).

---

## Critères d'acceptation

- [ ] POST `typeEvenement=MARIAGE_PACS`, `conventionPlusFavorable=false` → `dureeApplicableJours=4`, `base=LEGALE`, `maintienSalaire=true`
- [ ] POST `typeEvenement=DECES_ENFANT`, `conventionPlusFavorable=false` → `dureeApplicableJours=5` (cas de base)
- [ ] POST `typeEvenement=ANNONCE_HANDICAP_ENFANT` → `dureeApplicableJours=2`
- [ ] POST `typeEvenement=NAISSANCE`, `conventionPlusFavorable=true`, `dureeConventionnelleJours=5` → `dureeApplicableJours=5`, `base=CONVENTIONNELLE`
- [ ] POST `typeEvenement=MARIAGE_PACS`, `conventionPlusFavorable=true`, `dureeConventionnelleJours=3` (< légale 4) → `dureeApplicableJours=4`, `base=LEGALE`
- [ ] POST `typeEvenement=DEMENAGEMENT_NON_LEGAL` → `dureeLegaleJours=0` + note
- [ ] POST `typeEvenement` inconnu → 400 ; `conventionPlusFavorable=true` sans durée → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`conge_evt_familial_detecte`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL, priority 94
- [ ] `F-DT-76-conges-evenements-familiaux` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `CongesEvenementsFamiliauxAnalyzerTest` : ≥ 6 cas (mariage 4j, naissance 3j, décès enfant 5j, annonce handicap 2j, CCN plus favorable retenue, CCN moins favorable → légale, déménagement 0j)
- **IT** `CongesEvenementsFamiliauxControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `conges_evenements_familiaux_analyses`
- **Migrations** : `532-create-conges-evenements-familiaux-analyses.xml` (create) + `533-seed-conges-evenements-familiaux-visibility.xml` (seed visibility, priority 94)
- **Endpoint** `CongesEvenementsFamiliauxController` (POST + GET)
- **Service** `CongesEvenementsFamiliauxService` + **Analyzer** `CongesEvenementsFamiliauxAnalyzer`
- **Extension** `TravailExtractedData` : champs `typeEvenementFamilial` + `dureeConventionnelleEvtFamilial` ajoutés au sous-record consolidé `Sf218dDetail` + flag `congeEvtFamilialDetecte` + instruction `TRAVAIL_INSTRUCTION_PART39` dans `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-44)
- Congé de paternité / maternité (couvert par F-212, situation distincte)
- Congé parental d'éducation (F-DT-78, SF-218-45 — situation distincte)
- Congé de deuil (régime CAF spécifique au décès d'enfant — non recalculé ici)
- Détermination fine de l'âge de l'enfant pour le décès (cas renforcé signalé en note, pas paramétré)
