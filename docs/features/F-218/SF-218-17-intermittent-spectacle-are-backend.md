# Mini-spec — F-218 / SF-218-17 — Intermittent du spectacle : ouverture des droits ARE (annexes 8/10) — backend

## Identifiant

`F-218 / SF-218-17`

## Feature parente

`F-218b` — Régimes catégoriels FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-17-intermittent-spectacle-are-backend`

---

## Objectif

Outiller l'**ouverture des droits à l'allocation d'aide au retour à l'emploi (ARE)** d'un **intermittent du spectacle** relevant des **annexes 8 (techniciens) et 10 (artistes)** du règlement Unedic : vérifier le seuil de **507 heures sur 12 mois** (période de référence affiliation glissante), calculer le déficit / l'excédent d'heures et déterminer l'ouverture ou non des droits. Aucun outil existant ne couvre le régime intermittent (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/intermittent-spectacle-are-analysis`
- Body :
  - `annexe` (enum `ANNEXE_8_TECHNICIENS` | `ANNEXE_10_ARTISTES`, requis)
  - `dateFinContrat` (LocalDate, requis) — fin du dernier contrat (point de départ de la recherche de droits)
  - `heuresTravaillees12Mois` (int, requis) — total des heures (ou cachets convertis) déclarées sur la période de référence de 12 mois
  - `nombreCachets` (int, défaut 0) — cachets artistiques annexe 10 (1 cachet = 12 h ; cachets isolés/groupés à convertir)
  - `heuresFormationDispensees` (int, défaut 0) — heures d'enseignement assimilables (plafonnées, ex. 70 h / 90 h selon âge)
- Analyzer `IntermittentSpectacleAreAnalyzer` :
  - **Conversion cachets** (annexe 10) : `heuresCachets = nombreCachets × 12`. `heuresFormationRetenues = min(heuresFormationDispensees, PLAFOND_FORMATION)`.
  - **Total d'heures retenues** : `heuresTotalesRetenues = heuresTravaillees12Mois + heuresCachets + heuresFormationRetenues`.
  - **Seuil d'affiliation** : 507 heures sur 12 mois (constante `SEUIL_HEURES = 507`, `PERIODE_REFERENCE_MOIS = 12`). Annoter `// seuil et plafonds Unedic — à actualiser à chaque renégociation de la convention d'assurance chômage`.
  - **Verdict ouverture** : `heuresTotalesRetenues >= 507` → `ouvertureDroits = OUVERTS` ; sinon `NON_OUVERTS`. Champs `heuresManquantes` (= max(0, 507 − total)) ou `heuresExcedentaires`.
  - **Date anniversaire / réexamen** : `dateProchainExamen = dateFinContrat + 12 mois` (date d'anniversaire indicative du réexamen annuel des droits).
  - **Statut** : `DROITS_OUVERTS` / `DROITS_NON_OUVERTS` / `A_VERIFIER` (si `heuresTotalesRetenues` dans une marge de ± 10 h du seuil → recommander vérification France Travail).
  - `baseJuridique` : règlement d'assurance chômage Unedic — annexes 8 et 10 ; condition d'affiliation 507 h / 12 mois — annoté `(à vérifier par avocat / France Travail)`.
- Output persisté dans `intermittent_spectacle_are_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/intermittent-spectacle-are-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| annexe absente / inconnue | 400 |
| dateFinContrat absente / future | 400 |
| heuresTravaillees12Mois négatif | 400 |
| nombreCachets / heuresFormationDispensees négatif | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Règlement d'assurance chômage Unedic — annexe 8** — ouvriers et techniciens de l'édition d'enregistrement sonore, de la production cinématographique et audiovisuelle, du spectacle.
- **Règlement d'assurance chômage Unedic — annexe 10** — artistes du spectacle.
- Condition d'affiliation : **507 heures sur les 12 mois** précédant la fin du contrat.
- Conversion des cachets (annexe 10) : 1 cachet = 12 heures (cachets isolés / groupés).
- Plafonds d'heures de formation / enseignement assimilables — à actualiser à chaque convention.

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `dateFinContrat` | date | `dateFinContrat` / `dateRupture` (existant) | Réutiliser si présent |
| `annexe` | enum | `intermittentAnnexe` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Flag CONTEXTUAL pivot** : `statut_intermittent_detecte` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte un statut intermittent (mentions « intermittent du spectacle », « annexe 8 », « annexe 10 », « cachet », « 507 heures », « artiste du spectacle », « technicien audiovisuel », « France Travail spectacle »).

---

## Critères d'acceptation

- [ ] POST `annexe=ANNEXE_8_TECHNICIENS`, `heuresTravaillees12Mois=520` → `ouvertureDroits=OUVERTS`, `heuresExcedentaires=13`
- [ ] POST `annexe=ANNEXE_10_ARTISTES`, `heuresTravaillees12Mois=300`, `nombreCachets=15` → total 480, `NON_OUVERTS`, `heuresManquantes=27`
- [ ] POST `heuresFormationDispensees` au-delà du plafond → écrêtage au plafond
- [ ] POST total dans la marge ± 10 h du seuil → statut `A_VERIFIER`
- [ ] POST → `dateProchainExamen = dateFinContrat + 12 mois`
- [ ] POST `dateFinContrat` future → 400 ; heures négatives → 400
- [ ] POST annexe absente → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`statut_intermittent_detecte`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL
- [ ] `F-DT-106-intermittent-spectacle-are` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `IntermittentSpectacleAreAnalyzerTest` : ≥ 6 cas (ouverture annexe 8, conversion cachets annexe 10, non-ouverture + heures manquantes, écrêtage formation, statut A_VERIFIER en marge, date prochain examen)
- **IT** `IntermittentSpectacleAreControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `intermittent_spectacle_are_analyses`
- **Migrations** : `create-intermittent-spectacle-are-analyses.xml` + `seed-intermittent-spectacle-are-visibility.xml` (reconfirmer les numéros libres dans le worktree)
- **Endpoint** `IntermittentSpectacleAreController` (POST + GET)
- **Service** `IntermittentSpectacleAreService` + **Analyzer** `IntermittentSpectacleAreAnalyzer`
- **Extension** `TravailExtractedData` : champ `intermittentAnnexe` + flag `statutIntermittentDetecte` + prompt `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-18)
- Calcul du montant de l'ARE journalière (formule complexe SJR — V2)
- Allocation de professionnalisation et de solidarité (APS) — autre dispositif
- Requalification d'un CDD d'usage en CDI (autre situation, outil dédié futur)
