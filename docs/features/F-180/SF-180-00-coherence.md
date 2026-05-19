# F-180 — Cadrage cohérence (étape 0)

## Verdict : GO

## Intention métier (1 phrase)

Donner au pilote produit (super-admin) un moyen fiable de savoir, sans grep des logs JVM, quels mappers `DashboardTile` de F-167 crashent silencieusement en production et quels outils décisionnels sont réellement consommés ou dormants.

## Nature de la feature

**Outil de pilotage interne super-admin** — F-180 n'est pas une feature de l'avocat. L'« utilisateur cible » n'est pas l'avocat mais l'équipe produit/dev qui maintient les 85+ mappers décisionnels livrés par F-167. La skill `feature-coherence-challenger` reste applicable : son objet est l'**existence fonctionnelle** des briques amont/aval, indépendamment du persona. Le workflow reconstruit ci-dessous est donc le workflow de maintenance produit, pas le workflow d'un dossier.

> Précision importante (encadré vocabulaire de la skill) : F-180 **mesure l'usage prod** par conception — c'est son objet métier (panels dormantes/actives). Cela ne contredit pas la skill : la skill interdit de **fonder un verdict de cohérence sur l'usage prod**. Ici on ne juge pas si F-180 doit exister à la lumière de compteurs prod ; on vérifie que ses briques amont (F-167 instrumentable, F-178 écran) existent fonctionnellement. C'est le cas.

## Workflow métier réel de l'utilisateur cible (pilotage produit)

Source : pratique de gouvernance du projet (CLAUDE.md cycle de feature, `docs/governance/`), constat documenté ligne F-180 de `PRODUCT_SPEC.md` (discussion produit 2026-05-02), cas historiques F-164 / F-192 / F-229 / SF-DT-36-03 (régressions silencieuses de tiles).

1. F-167 a livré ~85 mappers `tileFromXxxAnalysis()` dans `CaseFileDashboardService.assembleTiles()`, chacun wrappé par `addSafely()`.
2. Un refactor backend (changement de record `*Result`, enum renommée, colonne migrée, JSON malformé) casse un mapper.
3. `addSafely()` catche l'exception, log un WARN, la tile disparaît du dashboard de l'avocat.
4. Côté avocat : aucune alerte — la tile est juste absente. Il ne sait pas qu'un outil aurait dû s'afficher.
5. Côté équipe produit : le WARN existe dans les logs ELK 7 jours, mais personne ne le surveille proactivement. Un redémarrage JVM entre deux audits perd les WARN antérieurs.
6. La régression n'est découverte que fortuitement (audit transversal manuel — cas F-229, ou signal terrain — cas F-DT-36).
7. En parallèle, l'équipe veut prioriser le polish UX : quels outils sont vraiment consommés en prod, lesquels n'ont jamais produit la moindre analyse (backlog mort) ?
8. Aujourd'hui cette information n'existe nulle part — il faut écrire des requêtes SQL ad hoc table par table.
9. État terminal du workflow de maintenance : l'équipe dispose d'un tableau de bord unique listant (a) les mappers en erreur à corriger, (b) les outils dormants, (c) les outils actifs triés par usage.

## Cartographie features actuelles ↔ workflow

| Étape workflow maintenance | Feature(s) LegalCase | Statut |
|---|---|---|
| 1. 85 mappers `DashboardTile` instrumentables | F-167 — dashboard décisionnel agrégé | ✅ Livrée |
| 1bis. Garde-fou statique toolId↔seed (CI) | SF-DT-36-03 `DashboardTileToolIdIntegrityIT` | ✅ Livrée (PR #1067, 2026-05-19) |
| 2-3. `addSafely()` catch silencieux | F-167 `CaseFileDashboardService` | ✅ Livrée |
| 5. Logs JVM / ELK (volatils, 7j) | Observabilité existante | ✅ Existe mais insuffisant |
| 7-8. Compteurs d'usage par outil | — | ❌ **Aucune feature** |
| 9. Écran super-admin de pilotage | F-178 `/super-admin/backlog` (tabs réutilisables) | ✅ Livrée |
| 9. **Tab « Audit dashboard » + tables crashes/runs** | **F-180 (la feature challengée)** | — |

## Position de la nouvelle feature

F-180 s'insère aux **étapes 5 → 9** : elle remplace le grep ELK volatil (étape 5) par une persistance robuste (`dashboard_tile_crashes`), comble le trou d'information de l'étape 7-8 (compteurs d'usage), et matérialise l'état terminal du workflow de maintenance à l'étape 9 (tab « Audit dashboard »).

## Challenge amont

> *Chaque étape AVANT F-180 est-elle couverte par une feature du produit ?*

- **F-167 livrée** : les 85 mappers + `addSafely()` existent. F-180 instrumente directement `addSafely()` — la brique amont d'instrumentation existe. ✅
- **F-178 livrée** : `/super-admin/backlog` fournit l'infrastructure d'écran super-admin (composant standalone à onglets, gating `SuperAdminService.assertSuperAdmin`, pattern freshness + bouton resync). F-180 réutilise ce composant. ✅
- **`@Scheduled` déjà en place dans le projet** : `BacklogSyncScheduler` (F-178) prouve que le pattern cron Spring fonctionne. ✅
- **Tables `*_analyses`** : les ~85 tables de résultat décisionnel existent (105 migrations `create-*-analys*`). F-180 compte leurs rows. ✅

**Aucun trou amont.** Tous les pré-requis sont livrés.

## Challenge aval

> *La sortie de F-180 est-elle exploitable par les étapes aval ?*

- Sortie de F-180 = un rapport d'audit lisible (3 panels) consommé par l'équipe produit pour décider : corriger un mapper / archiver un outil dormant / prioriser le polish d'un outil actif.
- L'étape aval « corriger un mapper » est couverte par le cycle de dev standard CLAUDE.md (bugfix → mini-spec allégée → PR).
- L'étape aval « alerting proactif (Slack/email) » est explicitement **hors scope V1** dans la spec — déférée V2 selon volume. Ce n'est pas un trou bloquant : le rapport reste consultable manuellement, exactement comme le tab Produit de F-178.
- L'état terminal du workflow de maintenance (étape 9) **est** la consultation du tab — F-180 le matérialise. Pas de dead-end.

**Aucun trou aval bloquant.**

## Non-redondance avec SF-DT-36-03 (`DashboardTileToolIdIntegrityIT`)

Point explicitement demandé dans le brief. Vérification :

| Axe | SF-DT-36-03 `DashboardTileToolIdIntegrityIT` | F-180 `DashboardAuditService` |
|---|---|---|
| Moment | **Statique** — exécuté en CI, build time | **Runtime** — exécuté en production, sur données réelles |
| Détecte | Désynchro *structurelle* : toolId hardcodé sans seed `decision_tool_visibility_rules` (et l'inverse) | *Crash d'exécution* : NPE, mauvais cast JSON, enum inconnue dans un mapper qui tourne en prod |
| Source de données | Code source `CaseFileDashboardService.java` + table `decision_tool_visibility_rules` | Table `dashboard_tile_crashes` (peuplée par `addSafely()` en prod) + count rows `*_analyses` |
| Ce qu'il ne voit pas | Un mapper qui compile et a son seed mais **jette une exception au runtime** (JSON corrompu, donnée inattendue) | Une désynchro structurelle qui casse le build avant tout déploiement |

**Conclusion** : zéro redondance. SF-DT-36-03 est un garde-fou *préventif build-time* (empêche de merger une désynchro). F-180 est un détecteur *curatif runtime* (révèle les crashes qui surviennent malgré un build vert, ex. donnée prod inattendue). Les deux sont complémentaires et tous deux nécessaires. F-180 ne réimplémente aucune logique de SF-DT-36-03.

## STOPs / pré-requis à ajouter au backlog

Aucun. Tous les pré-requis (F-167, F-178) sont livrés.

## Invariants anti-gadget pour la mini-spec

1. **Instrumentation réelle, pas mock** : `addSafely()` doit effectivement persister chaque crash dans `dashboard_tile_crashes` avec le `toolId` du mapper concerné — la signature de `addSafely()` doit donc recevoir le `toolId` (aujourd'hui le lambda est opaque). Sans cela le panel 🔴 est toujours vide et la feature est un gadget.
2. **Robustesse au redémarrage JVM** : la persistance DB est l'invariant central justifiant F-180 vs grep logs. La table `dashboard_tile_crashes` doit survivre à un redémarrage et porter une rétention 30j (vs 7j ELK).
3. **Le crash de persistance ne doit jamais re-casser la tile** : si l'INSERT dans `dashboard_tile_crashes` échoue, `addSafely()` doit rester fail-open (catch du catch) — l'instrumentation ne doit pas dégrader le dashboard de l'avocat.
4. **Endpoint super-admin only** : `GET /api/v1/super-admin/dashboard-audit/latest` gated par `SuperAdminService.assertSuperAdmin` — sinon fuite d'information de pilotage interne.
5. **Pas de PII** : l'affichage est **agrégé par toolId** (compte de crashes, dernier message). Aucun drill-down crash → caseFile en V1 (la colonne `case_file_id` est stockée pour usage interne futur mais jamais exposée par l'endpoint).
6. **Le cron doit produire une row historisée** : `@Scheduled` lundi 8h UTC pousse une row dans `dashboard_audit_runs` — l'endpoint `latest` lit la dernière row, il ne recalcule pas à chaque GET (sinon coût de 85 counts par appel).

## Décision finale

**GO.** F-180 est fonctionnellement cohérente : briques amont (F-167 instrumentable, F-178 écran) livrées, sortie exploitable (rapport de pilotage consultable), aucune redondance avec le garde-fou statique SF-DT-36-03 (statique/build-time vs runtime/prod). Statut PRODUCT_SPEC : `Backlog` → `À faire`. Enchaîner étape 0 bis (impact écran : nouvelle tab super-admin).
