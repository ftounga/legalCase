# Mini-spec — F-DT-36 / SF-DT-36-03 — Correctif câblage des 16 tuiles dashboard orphelines

> Correctif (bugfix) sur la feature F-DT-36. Exempté des étapes 0 / 0 bis
> (cadrage cohérence + cohérence écran) — aucun élément visible nouveau, aucun
> nouveau workflow : la tuile dashboard et le panel F-IA-04 existent déjà.

---

## Identifiant

`F-DT-36 / SF-DT-36-03`

## Feature parente

`F-DT-36` — Analyse des nullités de procédure de licenciement (droit du travail FR)

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-DT-36-03-fix-dashboard-tile`

---

## Objectif

> En une phrase : que fait cette subfeature ?

Câbler dans `CaseFileDashboardService` les 16 outils décisionnels dont le
résultat calculé était persisté mais n'émettait aucune tuile dashboard
(F-DT-36 — bug déclencheur — + 15 outils orphelins révélés par le garde-fou
inverse), et ajouter ce garde-fou de test empêchant qu'un futur outil seedé en
visibilité soit oublié dans l'assemblage des tuiles.

---

## Contexte du bug

Bug constaté en staging (dossier « Licenciement Dupon ») : l'avocat calcule la
nullité de procédure, le résultat est bien persisté (`POST` → table
`procedure_nullite_licenciement_analyses`), mais **rien ne s'affiche dans le
dashboard décisionnel**.

Cause : `CaseFileDashboardService.assembleTiles()` itère sur une liste **codée
en dur** d'appels `addSafely(tiles, () -> tileFromXxxAnalysis(caseFileId))`, un
par outil. F-DT-36 a été livré en deux lots (SF-DT-36-01 backend calcul +
migrations, SF-DT-36-02 frontend `TOOL_REGISTRY` + composant + seed visibilité)
sans que la couche intermédiaire — la tuile dashboard — soit ajoutée. Le
repository `ProcedureNulliteLicenciementRepository` n'est ni injecté ni appelé.

Le garde-fou existant `DashboardTileToolIdIntegrityIT` (F-229 SF-229-03) ne l'a
pas détecté : il vérifie un seul sens (« toute tuile émise → seed DB »), pas
l'inverse (« tout outil seedé → tuile émise »). Cette SF ferme l'angle mort.

### Audit transversal — autres outils

⚠️ **Correction de périmètre (2026-05-19)** : la rédaction initiale de cette
mini-spec concluait « F-DT-36 est le seul orphelin réel ». **C'était faux.**
Le garde-fou inverse ajouté par cette SF (`DashboardTileToolIdIntegrityIT`,
sens « tout outil seedé → tuile émise ») a révélé **16 outils orphelins**, pas
1 : F-DT-36 + F-IM-21/22/23/24 (urgences immigration FR, livrées par F-208) +
11 outils Famille BE (acceptation/renonciation succession, autorité parentale
BE, contributions alimentaires BE, divorce DC/DDI BE, liquidation-partage BE,
médiation familiale pré-saisine, pacte successoral BE 2018, régime communauté
légale BE, tribunal de la famille BE mesures provisoires) — tous dotés d'une
table de résultat persistée et d'un endpoint de calcul, donc tous légitimement
censés émettre une tuile. **SF-DT-36-03 câble les 16.**

Les outils seedés en visibilité qui restent **volontairement** sans tuile
(outils formule/PDF sans persistance de résultat, wrappers frontend
auto-suffisants F-198, checklists référentielles) sont listés et justifiés
dans `KNOWN_NO_DASHBOARD_TILE_IDS` (10 entrées — cf.
`DashboardTileToolIdIntegrityIT`).

---

## Comportement attendu

### Cas nominal

Sur `GET /api/v1/case-files/{id}/dashboard`, `assembleTiles()` interroge
`procedureNulliteLicenciementRepo.findByCaseFileId(caseFileId)`. Si une analyse
F-DT-36 existe pour le dossier :

1. désérialisation du snapshot JSON (`ProcedureNulliteLicenciementResponse`) ;
2. émission d'une `DashboardTile` :
   - `toolId` = `F-DT-36-procedure-nullite-licenciement`
   - `theme` = `VALIDITE` (même thème que F-DT-16, sa SF jumelle)
   - `label` = `Nullité de procédure`
   - `primaryValue` = nom du verdict (`NULLITE_AVEREE` / `NULLITE_PROBABLE` /
     `PROCEDURE_REGULIERE`)
   - `secondaryValue` = `N vice(s) — score S/100`
   - `alertLevel` selon le verdict (mapping ci-dessous).

Si aucune analyse n'existe pour le dossier → aucune tuile (comportement
inchangé, `orElse(null)`).

### Mapping verdict → `alertLevel`

Conforme à la convention couleur **déjà tranchée dans SF-DT-36-02** (« rouge
réservé au verdict `NULLITE_AVEREE` ») — donc **distinct** du helper partagé
`mapVerdictRisque` (dont la sémantique « probabilité » est inverse). Mapping
dédié `mapVerdictNullite` :

| Verdict | `alertLevel` | Couleur UI |
|---------|--------------|-----------|
| `NULLITE_AVEREE` | `ALERT` | rouge |
| `NULLITE_PROBABLE` | `WARNING` | navy/or |
| `PROCEDURE_REGULIERE` | `OK` | vert |
| `null` / inconnu | `null` | neutre |

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Snapshot JSON corrompu / illisible | `catch (Exception)` → tuile absente, autres tuiles préservées (fail-open par tuile, pattern existant) | 200 (dashboard partiel) |
| Aucune analyse F-DT-36 pour le dossier | Aucune tuile F-DT-36 | 200 |
| Repository en erreur | `addSafely` isole l'échec, autres tuiles préservées | 200 |

---

## Analyse de cohérence transversale

### Périmètres scannés

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Autres outils décisionnels (table + endpoint + seed mais pas de tuile) | Oui | Audit transversal réalisé → **16 outils orphelins** (F-DT-36 + F-IM-21/22/23/24 + 11 Famille BE), **tous câblés dans cette SF**. Les 10 outils volontairement sans tuile sont tracés et justifiés dans `KNOWN_NO_DASHBOARD_TILE_IDS` |
| Garde-fou anti-récidive | Oui | **Intégré dans cette SF** : test inverse dans `DashboardTileToolIdIntegrityIT` |
| Autres pays (BE) | Non | F-DT-36 est FRANCE uniquement ; la nullité de procédure BE est une feature jumelle distincte non encore livrée |
| Autres domaines (Famille / Immigration) | Non | Le bug est spécifique au câblage F-DT-36 ; aucun autre domaine concerné (audit) |
| Frontend (`TOOL_REGISTRY`, composant, seed visibilité) | Non | Déjà livrés et corrects (SF-DT-36-02) — aucune modification |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (correctif
  F-DT-36 + garde-fou de test couvrant tous les outils)
- [x] Non applicable aux autres cibles (justification explicite ci-dessus)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF **backend pure**. Aucun composant
  frontend décisionnel livré ni modifié. Le composant
  `ProcedureNulliteLicenciementSectionComponent` et l'entrée `TOOL_REGISTRY`
  existent déjà (SF-DT-36-02) et restent inchangés.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : SF backend pure ne créant ni
  ne modifiant aucun champ saisissable. Le formulaire F-DT-36 (SF-DT-36-02) et
  son pré-remplissage IA (SF-246-01) ne sont pas touchés.

---

## Critères d'acceptation

- [ ] Une analyse F-DT-36 persistée pour un dossier produit une `DashboardTile`
  de `toolId` `F-DT-36-procedure-nullite-licenciement` dans le retour de
  `assembleTiles()`.
- [ ] `theme` = `VALIDITE`, `label` = `Nullité de procédure`, `primaryValue` =
  nom du verdict, `secondaryValue` au format `N vice(s) — score S/100`.
- [ ] `alertLevel` : `NULLITE_AVEREE` → `ALERT`, `NULLITE_PROBABLE` →
  `WARNING`, `PROCEDURE_REGULIERE` → `OK`.
- [ ] Les 15 autres outils orphelins (F-IM-21/22/23/24 + 11 Famille BE)
  émettent chacun leur `DashboardTile` quand une analyse est persistée pour le
  dossier, avec `theme` / `alertLevel` cohérents — couverts par les tests
  paramétrés `assembleTiles_cableLesOutilsImmigrationOrphelins` et
  `assembleTiles_cableLesOutilsFamilleBeOrphelins`.
- [ ] Dossier sans analyse F-DT-36 → aucune tuile F-DT-36 (pas de régression).
- [ ] Snapshot corrompu → fail-open : tuile F-DT-36 absente, les autres tuiles
  du dashboard restent présentes.
- [ ] Le test inverse de `DashboardTileToolIdIntegrityIT` passe : tout `tool_id`
  de `decision_tool_visibility_rules` a soit une tuile émise dans
  `CaseFileDashboardService`, soit une entrée justifiée dans
  `KNOWN_NO_DASHBOARD_TILE_IDS`.
- [ ] Le test existant `DashboardTileToolIdIntegrityIT` (sens direct) reste vert.
- [ ] `DecisionToolVisibilityIntegrityIT` (F-164) reste vert.

---

## Périmètre

### Hors scope (explicite)

- Aucune modification frontend (composant, `TOOL_REGISTRY`, service Angular).
- Aucune migration Liquibase (le seed `decision_tool_visibility_rules` de
  F-DT-36 existe déjà — migration 231).
- Aucune modification du moteur de calcul, du controller ou de l'entité F-DT-36.
- Nullité de procédure belge (feature jumelle distincte, non livrée).
- Refonte de l'assemblage codé en dur de `assembleTiles()` (dette connue,
  hors périmètre de ce correctif).

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{id}/dashboard` | Oui | MEMBER (inchangé) |

> Endpoint existant, non modifié. Seul le contenu agrégé (`tiles`) gagne la
> tuile F-DT-36.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `procedure_nullite_licenciement_analyses` | SELECT | lecture via `findByCaseFileId` — table existante |
| `decision_tool_visibility_rules` | SELECT (test) | lue par le test d'intégrité — non modifiée |

### Migration Liquibase

- [x] Non applicable

### Fichiers impactés

- `backend/.../casefile/CaseFileDashboardService.java` — injection des 16
  repositories (champs + constructeur + assignations), 16 méthodes
  `tileFromXxxAnalysis()` (dont `tileFromProcedureNulliteLicenciementAnalysis()`),
  helper `mapVerdictNullite()`, 16 appels `addSafely(...)` dans `assembleTiles()`.
- `backend/.../casefile/CaseFileDashboardServiceTest.java` — mocks des 16
  repositories, stubs `Optional.empty()` par défaut, arguments constructeur,
  helpers de seed par outil, test dédié du mapping des 3 verdicts F-DT-36 +
  tests paramétrés sur les outils immigration et Famille BE orphelins.
- `backend/.../casefile/CaseFileDashboardServiceProcedureChecksTest.java`,
  `CaseFileDashboardServiceRetainedPistesTest.java` — mise à jour des appels
  constructeur (16 arguments repo supplémentaires).
- `backend/.../casefile/DashboardTileToolIdIntegrityIT.java` — test inverse +
  liste `KNOWN_NO_DASHBOARD_TILE_IDS`.

---

## Plan de test

### Tests unitaires (`CaseFileDashboardServiceTest`)

- [ ] `assembleTiles` — analyse F-DT-36 persistée → tuile présente, `theme` =
  `VALIDITE`, `label` / `primaryValue` / `secondaryValue` non nuls (cas ajouté
  à la liste paramétrée `travailMappersData`).
- [ ] `assembleTiles` — verdict `NULLITE_AVEREE` → `alertLevel` = `ALERT`.
- [ ] `assembleTiles` — verdict `NULLITE_PROBABLE` → `alertLevel` = `WARNING`.
- [ ] `assembleTiles` — verdict `PROCEDURE_REGULIERE` → `alertLevel` = `OK`.
- [ ] `assembleTiles_cableLesOutilsImmigrationOrphelins` — test paramétré :
  F-IM-21/22/23/24 → tuile présente, `theme` / `alertLevel` attendus.
- [ ] `assembleTiles_cableLesOutilsFamilleBeOrphelins` — test paramétré :
  les 11 outils Famille BE → tuile présente.
- [ ] `assembleTiles` — aucune analyse F-DT-36 → aucune tuile F-DT-36
  (couvert par `assembleTiles_returnsEmptyListWhenNoAnalysis` existant).
- [ ] `assembleTiles` — repository en exception → fail-open (couvert par
  `assembleTiles_failsOpenPerTileOnRepoException` existant).

### Tests d'intégration (`DashboardTileToolIdIntegrityIT`)

- [ ] Test direct existant (`aucun_toolId_hardcode..._n_est_orphelin`) → reste
  vert.
- [ ] Nouveau test inverse : tout `tool_id` de `decision_tool_visibility_rules`
  est soit émis comme tuile, soit dans `KNOWN_NO_DASHBOARD_TILE_IDS`.

### Isolation workspace

- [x] Non applicable — raison : `assembleTiles()` est une méthode interne
  d'agrégation ; le contrôle d'accès workspace est assuré en amont par
  `getDashboard()` (inchangé). Aucune nouvelle surface d'accès.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée : ajout d'une
  tuile dans un service d'agrégation interne. Pas d'auth, pas de workspace
  context, pas de plan/quota, pas de routing.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — aucune préoccupation transversale (auth /
  workspace / navigation) touchée. Le dashboard reste servi par le même
  endpoint, avec le même contrôle d'accès.

---

## Dépendances

### Subfeatures bloquantes

- `SF-DT-36-01` — statut : done (backend calcul + persistance + migrations 230)
- `SF-DT-36-02` — statut : done (frontend + seed visibilité migration 231)

### Questions ouvertes impactées

- [ ] Aucune — la convention couleur du verdict est déjà tranchée dans
  SF-DT-36-02 ; cette SF s'y conforme.

---

## Notes et décisions

- **Élargissement de périmètre assumé (1 → 16 outils)** : la mini-spec a été
  écrite en supposant F-DT-36 seul orphelin. Le garde-fou inverse, une fois
  écrit, a fait échouer le test sur 15 autres `tool_id` seedés sans tuile et
  dotés d'une table de résultat. Plutôt que de masquer le constat en ajoutant
  ces 15 outils à `KNOWN_NO_DASHBOARD_TILE_IDS` (ce qui aurait pérennisé le
  bug — résultats calculés jamais affichés), SF-DT-36-03 les câble tous. La
  mini-spec a été corrigée le 2026-05-19 pour refléter ce périmètre réel.
- **Mapping dédié `mapVerdictNullite`** plutôt que réutilisation de
  `mapVerdictRisque` : la sémantique diffère. `mapVerdictRisque` traite un
  verdict de *probabilité* favorable (ex. F-DT-16 : `ELEVEE` → `OK`) ; F-DT-36
  rend un verdict de *gravité* où `NULLITE_AVEREE` est le résultat critique
  (rouge). Réutiliser `mapVerdictRisque` produirait `null` (valeurs non
  reconnues) ou une couleur inversée. Le helper dédié garde la convention de
  SF-DT-36-02 explicite et locale.
- **Garde-fou inverse** : `KNOWN_NO_DASHBOARD_TILE_IDS` matérialise la décision
  « cet outil n'a volontairement pas de tuile dashboard ». Tout nouvel outil
  seedé en visibilité devra désormais soit émettre une tuile, soit être ajouté
  explicitement à cette liste — sinon le test échoue. C'est la traduction
  exécutable de la règle mémoire `feedback_pre_merge_visibility_seed_check`.
- Branche créée depuis `origin/master` (db39d892) — bug reconfirmé sur cette
  base : aucune occurrence de `ProcedureNullite` dans `CaseFileDashboardService`.
