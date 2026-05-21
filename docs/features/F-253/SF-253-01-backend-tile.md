# SF-253-01 — Backend : tile dashboard `F-253-risques-a-creuser`

## Objectif

Donner un consommateur backend au statut `À_CREUSER` des risques (F-195) en ajoutant une tile dashboard dédiée, visible uniquement quand au moins un risque reste à arbitrer, dans le dossier en cours.

## Comportement nominal

### Tile `F-253-risques-a-creuser`

- **toolId** : `F-253-risques-a-creuser`
- **theme** : `DIAGNOSTIC`
- **title** : `Risques à arbitrer`
- **primaryValue** : `N à creuser` (N = nombre de risques au statut `A_CREUSER` dans la dernière analyse `DONE` du dossier)
- **secondaryValue** : `Curation à compléter`
- **alertLevel** : `WARNING` quand visible (la tile n'apparaît jamais avec alertLevel `OK` — anti-bruit, cf. invariant étape 0 bis)

### Visibilité

La tile retourne `null` (donc disparaît du payload `assembleTiles`) si :
- Aucune `CaseAnalysis` `DONE` pour le dossier.
- L'alignement matérialisé est vide (analyse legacy pré-F-195 ou matérialisation fail-open ratée).
- Compteur `À_CREUSER` = 0.

### Source des données

- Lecture seule sur `case_analyses.risques_alignment_json` (colonne déjà créée par F-195 migration 208).
- Réutilisation de `RisqueAlignmentService.deserializeAlignment(latest.getRisquesAlignmentJson())`.
- Aucune nouvelle requête JPA, aucun cache, aucune migration.

### Cohabitation avec F-195

- La tile `F-195-risques-summary` (livrée F-195) reste **inchangée** — elle continue à afficher le récap V/É/À_C en sous-titre. Pas de régression sur les 5 tests `f195Tile_*` existants.
- F-253 met l'**accent** sur le À_CREUSER : sur un dossier avec mix (1 V + 1 É + 1 À_C), les 2 tiles s'affichent simultanément (F-195 = vue d'ensemble, F-253 = rappel d'action).
- Sur un dossier 100 % arbitré (0 À_C), seule F-195 reste visible.

## Cas d'erreur

| Cas | Comportement |
|---|---|
| Pas d'analyse DONE | Tile null (silence) |
| `risques_alignment_json` corrompu | `deserializeAlignment` fail-open → liste vide → tile null |
| Exception non-anticipée dans la méthode | `addSafely` log + tile absente |

Fail-open systématique : aucune exception ne remonte au payload `assembleTiles`.

## Critères d'acceptation

- **CA-01** : tile `F-253-risques-a-creuser` présente quand ≥ 1 alignement avec `statut = A_CREUSER` et au moins 1 analyse `DONE`.
- **CA-02** : tile absente quand tous les alignements sont `VALIDE` / `ECARTE` (compteur À_C = 0).
- **CA-03** : tile absente quand aucune analyse `DONE` ou alignement vide.
- **CA-04** : `primaryValue` = `"N à creuser"` avec pluralisation (ex. `"3 à creuser"`, mais `"1 à creuser"` au singulier).
- **CA-05** : `secondaryValue` = `"Curation à compléter"` (constant).
- **CA-06** : `alertLevel` = `WARNING` sur toutes les apparitions.
- **CA-07** : `theme` = `DIAGNOSTIC`, `title` = `"Risques à arbitrer"`.
- **CA-08** : isolation workspace stricte (héritée de `RisqueAlignmentService.getForLatestAnalysis`, mais ici on passe par `assembleTiles` qui repose sur `analysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc` — le filtre workspace est déjà géré par les couches amont).
- **CA-09** : pas de régression sur les 5 tests existants `f195Tile_*` (la tile F-195 garde son secondary `X validés · Y écartés · Z à creuser`).
- **CA-10** : test d'intégrité `DashboardTileToolIdIntegrityIT` reste vert avec l'ajout de `"F-253-risques-a-creuser"` dans `KNOWN_SUMMARY_TILE_IDS`.

## Plan de test minimal

### Tests unitaires (`CaseFileDashboardServiceTest.java`)

1. `f253Tile_aCreuserPresent_warning` — 2 alignements À_C + 1 VALIDE → tile présente, primary `2 à creuser`, alertLevel WARNING.
2. `f253Tile_singularPrimary` — 1 alignement À_C → primary `1 à creuser` (singulier).
3. `f253Tile_zeroACreuser_noTile` — 1 VALIDE + 1 ECARTE → tile absente.
4. `f253Tile_emptyAlignment_noTile` — alignement vide → tile absente.
5. `f253Tile_noAnalysis_noTile` — aucune analyse DONE → tile absente.
6. `f253Tile_cohabitsWithF195` — mix V/É/À_C → les 2 tiles présentes simultanément.

### Tests d'intégrité

- `DashboardTileToolIdIntegrityIT.aucun_toolId_hardcode_dans_CaseFileDashboardService_n_est_orphelin` — vert après ajout dans `KNOWN_SUMMARY_TILE_IDS`.

### Tests de régression

Les 5 tests `f195Tile_*` existants doivent rester verts sans modification.

## Tables / endpoints / composants impactés

| Élément | Modification |
|---|---|
| `CaseFileDashboardService.java` | + `addSafely` dans `buildDashboardTiles` (juste après F-195) + nouvelle méthode privée `tileFromRisquesACreuserAlignment(UUID caseFileId)` |
| `DashboardTileToolIdIntegrityIT.java` | + `"F-253-risques-a-creuser"` dans `KNOWN_SUMMARY_TILE_IDS` |
| `CaseFileDashboardServiceTest.java` | + 6 tests dédiés F-253 |

**Aucune** modification de :
- Migration Liquibase (réutilise infra F-195 — table `risque_status`, colonne `case_analyses.risques_alignment_json`)
- Entité JPA
- Repository
- Endpoint REST (`assembleTiles` est déjà exposé via `CaseFileDashboardController` existant)
- `RisqueAlignmentService` (utilisation lecture seule)
- `RisqueStatusService` (PUT statut reste un acte pur — invariant F-176 préservé)

## Hors périmètre SF-253-01

- **Frontend** : tile + pill cards outils + libellés → SF-253-02.
- **Export PDF** : section « Risques à creuser » → SF-253-03.
- **Modification tile F-195** : non — risque de régression sur tests existants. Le double affichage (F-195 montre 3 buckets, F-253 met l'accent sur À_C) est volontaire et acceptable (cf. décision technique étape 0 bis assouplie).
- **Endpoint dédié** : non — `assembleTiles` suffit, la tile sera consommée comme les autres par le frontend.
- **Notification / email** : non — invariant n°5 étape 0 strict.
- **Mapping pays/domaine** : non — `RisqueToolMatcher` existant couvre déjà tous les cas, F-253 ne discrimine pas.

## Notes et décisions

- **Décision de cohabitation F-195 ↔ F-253** : ne pas modifier la tile F-195 pour ne pas casser les 5 tests existants `f195Tile_*` (notamment `f195Tile_mixStatuses_correctSecondary` qui vérifie `secondary.contains("1 à creuser")`). L'étape 0 bis avait proposé d'harmoniser les libellés ; on garde l'harmonisation pour SF-253-02 frontend qui peut piloter l'affichage final (titres, sous-titres) sans casser le backend.
- **Pourquoi la tile masquée si À_C = 0** : invariant n°2 étape 0 bis — aucune apparition « tous arbitrés ✅ » (F-195 couvre déjà l'état post-arbitrage). Anti-pollution dashboard.
- **Pourquoi `alertLevel = WARNING` constant** : un risque à creuser est un travail à faire, pas une alerte critique. `ALERT` serait réservé à un risque VALIDÉ critique (F-195). `OK` n'est pas possible puisque la tile n'apparaît que si compteur > 0.

## Estimation

~30-45 min (lecture du code F-195 existante + ajout 1 méthode + 6 UT + 1 ajout liste intégrité).
