# Mini-spec — F-218 / SF-218-13 — Particulier employeur (CESU) : préavis et indemnité de licenciement — backend

## Identifiant

`F-218 / SF-218-13`

## Feature parente

`F-218b` — Régimes catégoriels FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-13-particulier-employeur-cesu-backend`

---

## Objectif

Outiller la rupture du contrat d'un salarié du **particulier employeur** (CESU : garde d'enfants, employé de maison, assistant maternel) en appliquant le régime conventionnel propre — **préavis** et **indemnité de licenciement** selon la **CCN des salariés du particulier employeur (2021)** ou la **CCN des assistants maternels**, distinct du droit commun (Code du travail partiellement applicable). Aucun outil existant ne couvre ce régime (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/particulier-employeur-cesu-analysis`
- Body :
  - `dateEntree` (LocalDate, requis) — début du contrat
  - `dateRupture` (LocalDate, requis) — date de notification du licenciement
  - `categorieEmploye` (enum `SALARIE_PARTICULIER_EMPLOYEUR` | `ASSISTANT_MATERNEL`, requis) — pilote la CCN applicable
  - `causeRupture` (enum `LICENCIEMENT_MOTIF_PERSONNEL` | `RETRAIT_ENFANT` | `FAUTE_GRAVE` | `FORCE_MAJEURE` | `DEPART_RETRAITE`, requis) — `RETRAIT_ENFANT` réservé à l'assistant maternel
  - `salaireMensuelMoyen` (BigDecimal, requis) — moyenne des 12 (ou 3) derniers mois, base IL
- Analyzer `ParticulierEmployeurCesuAnalyzer` :
  - **Préavis** (selon ancienneté à la rupture, barème conventionnel) : < 6 mois → 1 semaine ; 6 mois à < 2 ans → 1 mois ; ≥ 2 ans → 2 mois. Champ `dureePreavisJours` (converti) + `dureePreavisLibelle`.
  - **Éligibilité indemnité de licenciement** : due si ancienneté ≥ 8 mois (assistant maternel) / ≥ 8 mois CCN PE ET `causeRupture` ∉ { `FAUTE_GRAVE` }. Le `RETRAIT_ENFANT` (assistant maternel) ouvre droit à l'indemnité de rupture spécifique. Verdict `eligibiliteIndemnite` ∈ { `DUE`, `NON_DUE` } + `motifNonDue`.
  - **Calcul indemnité de licenciement conventionnelle** :
    - CCN salariés du particulier employeur : 1/4 de mois par année pour les 10 premières années + 1/3 au-delà (aligné R.1234-2, base salaire moyen).
    - CCN assistants maternels (indemnité de rupture) : 1/80 du total des salaires nets perçus depuis le 1er jour du contrat (formule conventionnelle spécifique). Champ `indemniteLicenciement` + `methodeCalcul` (enum `CONVENTIONNEL_PE` | `INDEMNITE_RUPTURE_ASSMAT`). Annoter `// barème CCN à actualiser annuellement (avenant salaires)`.
  - **Verdict global** : `RUPTURE_REGULIERE` / `RUPTURE_A_SECURISER` (préavis ou IL à formaliser) / `INDEMNITE_NON_DUE`.
  - `baseJuridique` : CCN salariés du particulier employeur (2021) ; CCN assistants maternels du particulier employeur ; art. L.7221-1 et s. CT (employés de maison) ; art. L.423-1 et s. CASF (assistant maternel) — annoté `(à vérifier par avocat)`.
- Output persisté dans `particulier_employeur_cesu_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/particulier-employeur-cesu-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| dateEntree ou dateRupture absente | 400 |
| dateRupture < dateEntree | 400 |
| salaireMensuelMoyen négatif/absent | 400 |
| `causeRupture=RETRAIT_ENFANT` avec `categorieEmploye=SALARIE_PARTICULIER_EMPLOYEUR` | 400 |
| categorieEmploye / causeRupture inconnue | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **CCN des salariés du particulier employeur (IDCC 3239, 2021)** — préavis et indemnité de licenciement.
- **CCN des assistants maternels du particulier employeur** — indemnité de rupture (formule 1/80).
- **Art. L.7221-1 et s. CT** — employés de maison (gens de maison).
- **Art. L.423-1 et s. CASF** — statut de l'assistant maternel, retrait de l'enfant.
- Barèmes de salaires conventionnels révisés par avenant — constantes à actualiser annuellement.

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `dateEntree` | date | `dateEntree` (existant) | Réutiliser |
| `dateRupture` | date | `dateRupture` / `dateLicenciement` (existant) | Réutiliser si présent |
| `categorieEmploye` | enum | `cesuCategorieEmploye` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Flag CONTEXTUAL pivot** : `particulier_employeur_detecte` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte un employeur particulier (mentions « CESU », « garde d'enfants », « assistant maternel », « employé de maison », « PAJEMPLOI », « particulier employeur »).

---

## Critères d'acceptation

- [ ] POST `categorieEmploye=SALARIE_PARTICULIER_EMPLOYEUR`, ancienneté 3 ans, `LICENCIEMENT_MOTIF_PERSONNEL` → `dureePreavisLibelle="2 mois"`, `eligibiliteIndemnite=DUE`, `methodeCalcul=CONVENTIONNEL_PE`
- [ ] POST `categorieEmploye=ASSISTANT_MATERNEL`, `causeRupture=RETRAIT_ENFANT` → `eligibiliteIndemnite=DUE`, `methodeCalcul=INDEMNITE_RUPTURE_ASSMAT` (1/80)
- [ ] POST `causeRupture=FAUTE_GRAVE` → `eligibiliteIndemnite=NON_DUE` + `motifNonDue`
- [ ] POST ancienneté 4 mois → `dureePreavisLibelle="1 semaine"`, `eligibiliteIndemnite=NON_DUE` (seuil 8 mois)
- [ ] POST ancienneté 1 an → préavis "1 mois"
- [ ] POST `RETRAIT_ENFANT` + `SALARIE_PARTICULIER_EMPLOYEUR` → 400
- [ ] POST dateRupture < dateEntree → 400 ; salaire négatif → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`particulier_employeur_detecte`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL
- [ ] `F-DT-108-particulier-employeur-cesu` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `ParticulierEmployeurCesuAnalyzerTest` : ≥ 6 cas (préavis 1 semaine / 1 mois / 2 mois, IL conventionnelle PE, indemnité rupture assmat 1/80, NON_DUE faute grave, NON_DUE ancienneté < 8 mois, retrait enfant assmat DUE)
- **IT** `ParticulierEmployeurCesuControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `particulier_employeur_cesu_analyses`
- **Migrations** : `create-particulier-employeur-cesu-analyses.xml` + `seed-particulier-employeur-cesu-visibility.xml` (reconfirmer les numéros libres dans le worktree)
- **Endpoint** `ParticulierEmployeurCesuController` (POST + GET)
- **Service** `ParticulierEmployeurCesuService` + **Analyzer** `ParticulierEmployeurCesuAnalyzer`
- **Extension** `TravailExtractedData` : champ `cesuCategorieEmploye` + flag `particulierEmployeurDetecte` + prompt `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-14)
- Régularisation CESU / déclaration PAJEMPLOI (hors outil décisionnel)
- Calcul des congés payés du particulier employeur (autre situation, calculateur CP existant)
- Litige sur les heures de présence responsable / présence de nuit (régime horaire spécifique — V2)
