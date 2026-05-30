# Mini-spec — F-218 / SF-218-11 — VRP : statut, préavis et indemnité de clientèle — backend

## Identifiant

`F-218 / SF-218-11`

## Feature parente

`F-218b` — Régimes catégoriels FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-11-vrp-indemnite-clientele-backend`

---

## Objectif

Outiller la rupture du contrat d'un **VRP statutaire** (voyageur représentant placier, art. L.7311-1 et s. CT) : déterminer l'éligibilité à l'**indemnité de clientèle** (art. L.7313-13), calculer le **préavis VRP spécifique** (art. L.7313-9), et comparer l'indemnité de clientèle à l'indemnité légale de licenciement pour appliquer la règle de **non-cumul / option la plus favorable**. Aucun outil existant ne couvre le régime VRP (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/vrp-indemnite-clientele-analysis`
- Body :
  - `dateEntree` (LocalDate, requis) — début du contrat VRP
  - `dateRupture` (LocalDate, requis) — date de notification de la rupture
  - `causeRupture` (enum `LICENCIEMENT_CAUSE_REELLE` | `FAUTE_GRAVE` | `FAUTE_LOURDE` | `DEMISSION` | `DEPART_RETRAITE` | `RUPTURE_CONVENTIONNELLE`, requis)
  - `typeVrp` (enum `EXCLUSIF` | `MULTICARTES`, défaut `EXCLUSIF`)
  - `commissionsAnnuellesMoyennes` (BigDecimal, requis) — moyenne annuelle des commissions des 3 dernières années (assiette de l'indemnité de clientèle)
  - `salaireMensuelMoyen` (BigDecimal, requis) — pour l'indemnité légale de licenciement comparée
  - `clienteleDeveloppee` (boolean, défaut true) — le VRP a créé/développé/accru la clientèle (condition de fond L.7313-13)
- Analyzer `VrpIndemniteClienteleAnalyzer` :
  - **Préavis** (art. L.7313-9, selon ancienneté à la rupture) : < 1 an → 1 mois ; 1 à 2 ans → 2 mois ; > 2 ans → 3 mois. Champ `dureePreavisMois`.
  - **Éligibilité indemnité de clientèle** : due si `clienteleDeveloppee=true` ET `causeRupture` ∉ { `FAUTE_GRAVE`, `FAUTE_LOURDE`, `DEMISSION` }. (Le départ en retraite et la rupture conventionnelle n'excluent pas par principe ; faute grave/lourde et démission excluent.) Verdict `eligibiliteClientele` ∈ { `DUE`, `NON_DUE` } + `motifNonDue` le cas échéant.
  - **Estimation indemnité de clientèle** (fourchette indicative, jurisprudence — l'évaluation finale relève du juge) : borne basse = 1 × `commissionsAnnuellesMoyennes`, borne haute = 2 × `commissionsAnnuellesMoyennes` (usage le plus répandu : 1 à 2 années de commissions sur la part de clientèle développée). Champs `indemniteClienteleMin`, `indemniteClienteleMax`. Annoter `// estimation indicative — évaluation souveraine du juge (préjudice réel)`.
  - **Indemnité légale de licenciement comparée** (art. R.1234-2, base de comparaison) : ancienneté en années × (1/4 salaire mensuel pour les 10 premières années + 1/3 au-delà). Champ `indemniteLegaleLicenciement`.
  - **Règle de non-cumul / option** : l'indemnité de clientèle ne se cumule pas avec l'indemnité légale ; le VRP perçoit la plus élevée. `optionRecommandee` ∈ { `INDEMNITE_CLIENTELE`, `INDEMNITE_LEGALE` } en comparant `indemniteClienteleMax` à `indemniteLegaleLicenciement` (recommandation, à confirmer par l'avocat).
  - `baseJuridique` : L.7311-1 et s. ; L.7313-13 (indemnité de clientèle) ; L.7313-9 (préavis) ; L.7313-11 (commissions de retour) — annoté `(à vérifier par avocat)`.
- Output persisté dans `vrp_indemnite_clientele_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/vrp-indemnite-clientele-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| dateEntree ou dateRupture absente | 400 |
| dateRupture < dateEntree | 400 |
| commissionsAnnuellesMoyennes ou salaireMensuelMoyen négatif/absent | 400 |
| causeRupture inconnue | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Art. L.7311-1 à L.7313-18 CT** — statut légal du VRP (exclusif / multicartes).
- **Art. L.7313-13 CT** — indemnité de clientèle en cas de rupture non imputable à une faute grave du VRP.
- **Art. L.7313-9 CT** — préavis spécifique VRP (1 / 2 / 3 mois selon ancienneté).
- **Art. L.7313-11 CT** — commissions de retour sur échantillonnages (hors périmètre calcul V1, mentionné en checklist).
- Non-cumul indemnité de clientèle / indemnité légale de licenciement : jurisprudence constante (Cass. soc.), le VRP perçoit la plus favorable.

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `dateEntree` | date | `dateEntree` (existant) | Réutiliser |
| `dateRupture` | date | `dateRupture` / `dateLicenciement` (existant) | Réutiliser si présent |
| `commissionsAnnuellesMoyennes` | montant | `vrpCommissionsAnnuelles` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Flag CONTEXTUAL pivot** : `vrp_statut_detecte` (FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte un statut VRP (mentions « VRP », « voyageur représentant placier », « représentant de commerce », « indemnité de clientèle », « carte de représentant », commissions).

---

## Critères d'acceptation

- [ ] POST ancienneté 3 ans, `LICENCIEMENT_CAUSE_REELLE`, `clienteleDeveloppee=true` → `dureePreavisMois=3`, `eligibiliteClientele=DUE`, fourchette `[1×, 2×]` commissions
- [ ] POST `causeRupture=FAUTE_GRAVE` → `eligibiliteClientele=NON_DUE` + `motifNonDue`
- [ ] POST `causeRupture=DEMISSION` → `NON_DUE`
- [ ] POST `clienteleDeveloppee=false` → `NON_DUE`
- [ ] POST ancienneté 6 mois → `dureePreavisMois=1` ; 18 mois → `dureePreavisMois=2`
- [ ] `optionRecommandee` = la plus favorable entre `indemniteClienteleMax` et `indemniteLegaleLicenciement`
- [ ] POST dateRupture < dateEntree → 400 ; montants négatifs → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`vrp_statut_detecte`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL
- [ ] `F-DT-104-vrp-indemnite-clientele` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `VrpIndemniteClienteleAnalyzerTest` : ≥ 8 cas (préavis 1/2/3 mois, éligibilité DUE, NON_DUE faute grave / démission / clientèle non développée, fourchette clientèle, option la plus favorable, comparaison indemnité légale)
- **IT** `VrpIndemniteClienteleControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `vrp_indemnite_clientele_analyses`
- **Migrations** : `492-create-vrp-indemnite-clientele-analyses.xml` + `493-seed-vrp-indemnite-clientele-visibility.xml` (reconfirmer les numéros libres dans le worktree)
- **Endpoint** `VrpIndemniteClienteleController` (POST + GET)
- **Service** `VrpIndemniteClienteleService` + **Analyzer** `VrpIndemniteClienteleAnalyzer`
- **Extension** `TravailExtractedData` : champ `vrpCommissionsAnnuelles` + flag `vrpStatutDetecte` + prompt `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-12)
- Calcul détaillé des commissions de retour sur échantillonnages (L.7313-11) — mentionné en checklist seulement
- VRP multicartes : répartition entre employeurs (V2)
- Requalification du statut VRP (litige sur la qualification — autre situation)
