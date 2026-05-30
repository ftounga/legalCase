# Mini-spec — F-218 / SF-218-25 — Licenciement CDI de chantier / d'opération — backend

## Identifiant

`F-218 / SF-218-25`

## Feature parente

`F-218b` — Régimes catégoriels FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-25-cdi-chantier-backend`

---

## Objectif

Analyser le **licenciement à la fin d'un CDI de chantier ou d'opération** (art. L.1223-8 et L.1223-9 CT) : vérifier les conditions de validité du recours (accord de branche étendu OU usage constant — BTP / ingénierie), qualifier le motif (fin de chantier = **cause de licenciement reposant sur une raison réelle et sérieuse, motif spécifique**), et calculer l'**indemnité de licenciement** due. Aucun outil existant ne couvre le CDI de chantier (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/cdi-chantier-analysis`
- Body :
  - `dateEntree` (LocalDate, requis) — début du contrat
  - `dateRupture` (LocalDate, requis) — notification du licenciement
  - `fondementRecours` (enum `ACCORD_BRANCHE_ETENDU` | `USAGE_CONSTANT_SECTEUR` | `AUCUN`, requis) — base légale du recours au CDI de chantier
  - `secteur` (enum `BTP` | `INGENIERIE` | `AUTRE`, requis)
  - `chantierAcheve` (boolean, requis) — le chantier/l'opération pour lequel le salarié a été engagé est réalisé
  - `salaireMensuelMoyen` (BigDecimal, requis) — base de l'indemnité de licenciement
  - `reclassementAutreChantierPropose` (boolean, défaut false) — proposition de poursuite sur un autre chantier (le cas échéant)
- Analyzer `CdiChantierAnalyzer` :
  - **Validité du recours** : valable si `fondementRecours` ∈ { `ACCORD_BRANCHE_ETENDU`, `USAGE_CONSTANT_SECTEUR` }. Si `AUCUN` → `recoursValide=false` + note « risque de requalification en CDI de droit commun ». Champ `recoursValide` (boolean) + `motifRecours`.
  - **Qualification du motif** : si `chantierAcheve=true` ET `recoursValide=true` → le licenciement pour fin de chantier repose sur une **cause réelle et sérieuse (motif spécifique L.1236-8)**. Champ `motifLicenciement` ∈ { `FIN_CHANTIER_CRS`, `MOTIF_NON_FONDE` }. Si `chantierAcheve=false` → `MOTIF_NON_FONDE` (le chantier n'est pas achevé, motif non caractérisé).
  - **Indemnité de licenciement** (art. R.1234-2, sauf disposition conventionnelle plus favorable) : ancienneté en années × (1/4 salaire mensuel pour les 10 premières années + 1/3 au-delà). Le CDI de chantier ouvre droit à l'indemnité de licenciement (et non à l'indemnité de précarité du CDD). Champ `indemniteLicenciement`. Annoter `// barème légal R.1234-2 — vérifier CCN BTP/ingénierie plus favorable, à actualiser`.
  - **Procédure** : licenciement soumis à la procédure de droit commun (entretien préalable, notification) — champ `procedureRequise=true`.
  - **Verdict global** : `LICENCIEMENT_FONDE` / `LICENCIEMENT_A_SECURISER` (reclassement proposé non tracé, ou motif fragile) / `RECOURS_INVALIDE` (requalification probable en CDI classique).
  - `baseJuridique` : art. L.1223-8 et L.1223-9 CT (CDI de chantier / opération) ; art. L.1236-8 CT (rupture à la fin du chantier = motif spécifique) ; art. R.1234-2 (indemnité) ; CCN BTP / ingénierie — annoté `(à vérifier par avocat)`.
- Output persisté dans `cdi_chantier_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/cdi-chantier-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| dateEntree ou dateRupture absente | 400 |
| dateRupture < dateEntree | 400 |
| salaireMensuelMoyen négatif/absent | 400 |
| fondementRecours / secteur inconnu | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Art. L.1223-8 et L.1223-9 CT** — contrat de chantier ou d'opération (CDI de chantier) : recours subordonné à un accord de branche étendu ou, à défaut, à un usage constant dans certains secteurs (BTP, ingénierie).
- **Art. L.1236-8 CT** — la rupture du CDI de chantier à la fin du chantier repose sur une cause réelle et sérieuse (motif spécifique) ; procédure de licenciement de droit commun.
- **Art. R.1234-2 CT** — indemnité légale de licenciement.
- **CCN BTP / ingénierie (Syntec)** — modalités et indemnités spécifiques.
- Barèmes d'indemnité — constantes à actualiser annuellement (et CCN plus favorable à contrôler).

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `dateEntree` | date | `dateEntree` (existant) | Réutiliser |
| `dateRupture` | date | `dateRupture` / `dateLicenciement` (existant) | Réutiliser si présent |
| `secteur` | enum | `cdiChantierSecteur` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Flag CONTEXTUAL pivot** : `cdi_chantier_detecte` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte un CDI de chantier (mentions « CDI de chantier », « contrat de chantier », « contrat d'opération », « fin de chantier », « BTP », « ingénierie », « licenciement pour fin de chantier »).

---

## Critères d'acceptation

- [ ] POST `fondementRecours=ACCORD_BRANCHE_ETENDU`, `secteur=BTP`, `chantierAcheve=true`, ancienneté 3 ans → `recoursValide=true`, `motifLicenciement=FIN_CHANTIER_CRS`, `indemniteLicenciement` > 0, `verdictGlobal=LICENCIEMENT_FONDE`
- [ ] POST `fondementRecours=USAGE_CONSTANT_SECTEUR`, `secteur=INGENIERIE`, `chantierAcheve=true` → `recoursValide=true`
- [ ] POST `fondementRecours=AUCUN` → `recoursValide=false`, `verdictGlobal=RECOURS_INVALIDE` + note requalification
- [ ] POST `chantierAcheve=false` → `motifLicenciement=MOTIF_NON_FONDE`
- [ ] POST `reclassementAutreChantierPropose=false` sur motif fragile → `LICENCIEMENT_A_SECURISER`
- [ ] Indemnité = 1/4 + 1/3 selon R.1234-2 (vérif barème ancienneté > 10 ans)
- [ ] POST dateRupture < dateEntree → 400 ; salaire négatif → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`cdi_chantier_detecte`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL
- [ ] `F-DT-37-licenciement-cdi-chantier` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `CdiChantierAnalyzerTest` : ≥ 6 cas (recours valide accord branche, recours valide usage secteur, recours invalide AUCUN, chantier non achevé → motif non fondé, indemnité R.1234-2 < 10 ans / > 10 ans, verdict A_SECURISER)
- **IT** `CdiChantierControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `cdi_chantier_analyses`
- **Migrations** : `create-cdi-chantier-analyses.xml` + `seed-cdi-chantier-visibility.xml` (reconfirmer les numéros libres dans le worktree)
- **Endpoint** `CdiChantierController` (POST + GET)
- **Service** `CdiChantierService` + **Analyzer** `CdiChantierAnalyzer`
- **Extension** `TravailExtractedData` : champ `cdiChantierSecteur` + flag `cdiChantierDetecte` + prompt `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-26)
- Chiffrage de l'indemnité pour licenciement sans cause réelle et sérieuse en cas de requalification (renvoi barème Macron F-DT existant)
- Régime du CDD de chantier (situation distincte)
- Détail des obligations de reclassement conventionnelles BTP (checklist seulement)
