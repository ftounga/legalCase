# Mini-spec — F-229 / SF-229-03 Audit transversal toolIds dashboard + handler F-193 + garde-fou intégrité

## Identifiant

`F-229 / SF-229-03`

## Statut

`draft` — 2026-05-09

## Branche Git

`feat/SF-229-03-audit-transversal-dashboard-toolids`

## Pattern de référence

- `DecisionToolVisibilityIntegrityIT` (test d'intégrité frontend KNOWN_FRONTEND_TOOL_IDS, garde-fou F-164 SF-164-01)
- `decision_tool_visibility_rules` Liquibase = source de vérité des toolIds (CLAUDE.md règle "Migration Liquibase qui INSERT/UPDATE dans `decision_tool_visibility_rules` un `tool_id` absent de `TOOL_REGISTRY`")

---

## Objectif

Aligner tous les toolIds émis par `CaseFileDashboardService` + `RisqueToolMatcher` sur la convention canonique des migrations Liquibase (`decision_tool_visibility_rules`). Ajouter le handler frontend manquant pour la tile orpheline F-193. Poser un garde-fou d'intégrité backend pour prévenir le retour du bug.

---

## Comportement attendu

### Audit complet (2026-05-09) — 5 mismatches détectés en plus de F-192 (déjà fixé SF-229-02)

| # | Backend dashboard service | Liquibase + TOOL_REGISTRY frontend | Sévérité |
|---|---|---|---|
| A | `CaseFileDashboardService.java:2342` `F-IM-08-oqtf-avec-delai` | `F-IM-08-oqtf-avec-delai-fr` | Code mort (tile cassée silencieusement) |
| B | `CaseFileDashboardService.java:2366` `F-IM-08-oqtf-sans-delai` | `F-IM-08-oqtf-sans-delai-fr` | Code mort |
| C | `CaseFileDashboardService.java:2386` `F-IM-08-referes-admin` | `F-IM-08-referes-admin-fr` | Code mort |
| D | `CaseFileDashboardService.java:2563` `F-IM-19-mineurs-immigration` | `F-IM-19-mineurs` | Code mort |
| E | `RisqueToolMatcher.java:41` `TOOL_OQTF_AVEC_DELAI = "F-IM-08-oqtf-avec-delai"` | `F-IM-08-oqtf-avec-delai-fr` | Mapping risque → tool incohérent (le toolIdCible posé en pieces_manquantes / case_deadlines pour les outils OQTF reste invalide aussi) |
| F | `case-dashboard.openGenericTool` : aucun handler pour `F-193-procedure-checks-summary` | Backend émet la tile → fallback `console.warn + return` | Orpheline (F-193 perdu, alors que c'est un délivrable de F-193 SF-193-01) |

### Cas nominal (après fix)

1. Backend `CaseFileDashboardService` émet une tile `F-IM-08-oqtf-avec-delai-fr` (corrigé)
2. Frontend `case-dashboard.openGenericTool('F-IM-08-oqtf-avec-delai-fr')` consulte `TOOL_REGISTRY` → entrée trouvée → modal ouvert avec le composant outil OQTF
3. `RisqueToolMatcher` mappe un risque "OQTF" sur `F-IM-08-oqtf-avec-delai-fr` (corrigé) → quand cet outil est cible, le toolIdCible pointé en `pieces_manquantes.toolIdCible` / `case_deadlines.toolIdCible` est valide
4. Backend émet une tile `F-193-procedure-checks-summary` → frontend appelle `BadgeNavigationService.go('checklist', caseFileId)` → navigation `/synthesis#section-checklist` + scroll fluide

### Garde-fou (CA-G1) — nouveau test d'intégrité backend

`DashboardTileToolIdIntegrityIT` — assert que tout toolId hardcodé dans `CaseFileDashboardService` (extrait par grep automatique via `Files.readAllLines`) existe dans `decision_tool_visibility_rules` OU dans une liste de tile résumé dérogatoires (`F-192-retained-pistes-summary`, `F-193-procedure-checks-summary`, `F-194-pieces-summary`, `F-195-risques-summary`, `F-196-questions-summary` — ces tiles ne sont pas instanciées comme outils, elles sont des "résumés" qui passent par BadgeNavigationService côté frontend, donc absentes de visibility_rules).

---

## Critères d'acceptation

### Backend

- [ ] **CA-01** : `CaseFileDashboardService.java:2342` `F-IM-08-oqtf-avec-delai` → `F-IM-08-oqtf-avec-delai-fr`
- [ ] **CA-02** : `CaseFileDashboardService.java:2366` `F-IM-08-oqtf-sans-delai` → `F-IM-08-oqtf-sans-delai-fr`
- [ ] **CA-03** : `CaseFileDashboardService.java:2386` `F-IM-08-referes-admin` → `F-IM-08-referes-admin-fr`
- [ ] **CA-04** : `CaseFileDashboardService.java:2563` `F-IM-19-mineurs-immigration` → `F-IM-19-mineurs`
- [ ] **CA-05** : `RisqueToolMatcher.java:41` `TOOL_OQTF_AVEC_DELAI = "F-IM-08-oqtf-avec-delai"` → `"F-IM-08-oqtf-avec-delai-fr"`
- [ ] **CA-06** : tests backend qui mockent ou assertent ces 5 toolIds adaptés (chercher tous les fichiers `*Test.java` / `*IT.java` qui contiennent l'ancienne valeur, les renommer)
- [ ] **CA-07** : nouveau `DashboardTileToolIdIntegrityIT` (`backend/src/test/java/fr/ailegalcase/casefile/DashboardTileToolIdIntegrityIT.java`) — extrait par regex tous les `new DashboardTile("F-XX-...", ...)` de `CaseFileDashboardService.java` ; pour chaque toolId extrait, assert que **soit** il existe dans `decision_tool_visibility_rules` (`SELECT 1 FROM decision_tool_visibility_rules WHERE tool_id = ?`), **soit** il est dans la liste hardcodée de tiles résumé (`KNOWN_SUMMARY_TILE_IDS = ['F-192-retained-pistes-summary', 'F-193-procedure-checks-summary', 'F-194-pieces-summary', 'F-195-risques-summary', 'F-196-questions-summary']`). Échec si un toolId n'est ni l'un ni l'autre. **Garde-fou symétrique** au `DecisionToolVisibilityIntegrityIT` frontend (F-164 SF-164-01).

### Frontend

- [ ] **CA-08** : `case-dashboard.component.ts:openGenericTool` ajouter handler `if (toolId === 'F-193-procedure-checks-summary') { this.badgeNavigation.go('checklist', this.caseFileId); return; }` AVANT le check `TOOL_REGISTRY.get`
- [ ] **CA-09** : `BadgeNavigationService` étendu avec key `'checklist'` qui invoque `router.navigate(['/case-files', caseFileId, 'synthesis'], { fragment: 'section-checklist' })` (anchor cohérent avec badge F-162 `synthesis.component.ts:361`)
- [ ] **CA-10** : tests Jest `case-dashboard.component.spec.ts` — nouveau test : clic tile `F-193-procedure-checks-summary` → `BadgeNavigationService.go('checklist', 'case-1')` invoqué
- [ ] **CA-11** : tests Jest `badge-navigation.service.spec.ts` — nouveau test pour key `'checklist'`
- [ ] **CA-12** : aucune régression sur les ~150 tests synthesis + ~58 case-dashboard + 8 badge-navigation existants

### Tests manuels staging

- [ ] **CA-13** (post-deploy) : sur dossier Immigration FR avec OQTF avec délai, vérifier que la tile dashboard OQTF s'instancie correctement au clic (modal ouvert, plus de console.warn)
- [ ] **CA-14** (post-deploy) : sur dossier Immigration FR avec mineur, vérifier la tile mineurs idem
- [ ] **CA-15** (post-deploy) : sur n'importe quel dossier avec checks F-96, vérifier que la tile F-193 ouvre `/synthesis#section-checklist` avec scroll fluide

---

## Périmètre

### Hors scope V1

- (a) Refactor pour partager les constantes toolId entre backend et frontend (DTO TypeScript généré) — V2
- (b) Audit des autres composants backend qui pourraient référencer ces toolIds (services métier, repositories) — vérifier seulement `CaseFileDashboardService` + `RisqueToolMatcher` (sources confirmées par grep). Si un autre fichier émerge, traité dans la même SF.
- (c) Remontée des tests Jest existants F-194/195/196 qui mockent les bons toolIds (déjà OK, pas de changement)

---

## Technique

### Fichiers à modifier — backend

1. `backend/src/main/java/fr/ailegalcase/casefile/CaseFileDashboardService.java` — 4 string littéraux corrigés (lignes 2342/2366/2386/2563)
2. `backend/src/main/java/fr/ailegalcase/analysis/RisqueToolMatcher.java` — ligne 41 string littéral
3. **Tests** — chercher toutes les occurrences avec `grep -rn "F-IM-08-oqtf-avec-delai\b\|F-IM-08-oqtf-sans-delai\b\|F-IM-08-referes-admin\b\|F-IM-19-mineurs-immigration\b" backend/src/test` et adapter chacune

### Fichiers à créer — backend

4. `backend/src/test/java/fr/ailegalcase/casefile/DashboardTileToolIdIntegrityIT.java` — IT qui extrait par regex les `new DashboardTile("X-...",` du source `CaseFileDashboardService.java` et vérifie chaque toolId présent dans `decision_tool_visibility_rules` OU dans `KNOWN_SUMMARY_TILE_IDS`. Échec → la SF est en faute. Pattern miroir `DecisionToolVisibilityIntegrityIT`.

### Fichiers à modifier — frontend

5. `frontend/src/app/case-files/case-dashboard/case-dashboard.component.ts` — ajouter handler F-193 (1 bloc `if`)
6. `frontend/src/app/case-files/synthesis-badges/badge-navigation.service.ts` — ajouter key `'checklist'` au type `BadgeKey` + branche dans `go()`
7. `frontend/src/app/case-files/case-dashboard/case-dashboard.component.spec.ts` — 1 nouveau test
8. `frontend/src/app/case-files/synthesis-badges/badge-navigation.service.spec.ts` — 1 nouveau test

### Aucune migration DB

Tout est code applicatif. Les visibility rules existantes en DB sont la source de vérité — on s'aligne dessus.

---

## Plan de test

### Tests backend

- Tests unitaires `CaseFileDashboardServiceTest` adaptés (renommages)
- Nouveau IT `DashboardTileToolIdIntegrityIT` (utilise `@SpringBootTest` + JdbcTemplate sur Testcontainers PostgreSQL ; charge les migrations Liquibase ; assert tous les toolIds extraits par regex)

### Tests frontend

- Jest `badge-navigation.service.spec.ts` : 1 nouveau test (key `'checklist'`)
- Jest `case-dashboard.component.spec.ts` : 1 nouveau test (clic F-193 → BadgeNavigationService.go('checklist'))
- Aucune régression sur les ~150 tests synthesis ni les 8 badge-navigation existants

### Tests manuels staging

Voir CA-13/14/15 ci-dessus.

---

## Dépendances

- F-229 SF-229-01 ✅ (BadgeNavigationService existant — étendu d'1 key)
- F-229 SF-229-02 ✅ (toolId F-192 corrigé)
- F-164 SF-164-01 ✅ (pattern garde-fou d'intégrité visibility rules — symétrique côté backend)
- Migrations Liquibase F-IM-08 (116/117/146) + F-IM-19 (172) ✅ (source de vérité)

---

## Impact par domaine métier

Transversal — corrige des cas métier Immigration FR (F-IM-08 OQTF avec/sans délai, référés admin, F-IM-19 mineurs) + F-193 procédure (3 domaines × 2 pays). Aucune adaptation par domaine ni par pays.

---

## Analyse de cohérence transversale

- **Auth/Principal** : N/A.
- **Workspace context** : N/A.
- **Plans/limites** : N/A.
- **Navigation/routing** : ✅ concerné (frontend ajoute key `checklist` au `BadgeNavigationService`). Smoke E2E navigation à valider post-deploy staging.
- **Outil décisionnel métier** : ✅ concerné — 4 outils Immigration FR (F-IM-08 ×3, F-IM-19) auparavant cassés silencieusement au clic depuis le dashboard sont rendus fonctionnels.
- **Pattern partagé** : `BadgeNavigationService` (déjà introduit SF-229-01) étendu d'1 key. Pas de nouveau service. Test d'intégrité backend `DashboardTileToolIdIntegrityIT` est un **nouveau pattern de garde-fou** symétrique au frontend `DecisionToolVisibilityIntegrityIT` — justifié par la divergence détectée 2026-05-09.

### Nouveau pattern UI ou service partagé — analyse d'impact

- **DashboardTileToolIdIntegrityIT** (nouveau IT backend) :
  - Cibles à harmoniser : `CaseFileDashboardService` (le seul à émettre des `DashboardTile` aujourd'hui)
  - Évolutions V2 : si un autre service émet des `DashboardTile`, l'IT doit être étendu pour scanner aussi ce fichier
  - Pas de pattern concurrent — c'est l'introduction d'un garde-fou jusqu'ici absent

---

## Risques

- **Régression métier sur F-IM-08/F-IM-19** : les 4 toolIds étaient cassés silencieusement, leur fix peut révéler des bugs cachés en aval (pieces_manquantes / case_deadlines avec toolIdCible invalide → maintenant valide → outil instancié → potentiel bug runtime). Mitigation : tests d'intégration backend doivent passer post-rename.
- **Tests Liquibase legacy** : si un test backend ancien hardcode l'ancienne valeur du toolId, il faudra l'adapter sans casser sa sémantique. Atteint par grep exhaustif (CA-06).
- **Smoke E2E** : à lancer post-deploy si possible.

---

## Notes

- **Décision 2026-05-09** : source de vérité = migration Liquibase (`decision_tool_visibility_rules`) + TOOL_REGISTRY frontend qui s'aligne dessus (cohérent avec règle CLAUDE.md F-164). Backend `CaseFileDashboardService` doit s'aligner sur la DB.
- **Décision 2026-05-09** : garde-fou backend `DashboardTileToolIdIntegrityIT` posé en même temps que le fix pour empêcher la régression future. Symétrique du frontend `DecisionToolVisibilityIntegrityIT`. Si un dev ajoute un nouveau `new DashboardTile("F-XXX-yyy",...)` sans correspondance Liquibase, la CI échoue.
- **Origine** : audit transversal demandé par utilisateur 2026-05-09 suite au bug F-192 SF-229-02. La consigne initiale F-229 SF-229-01 ("vérifie partout dans l'app") n'avait été appliquée qu'aux 4 mismatches visibles (F-194/195/196 + RETAINED_PISTES_SUMMARY) ; l'audit complet aurait dû être fait dès F-229 SF-229-01. Mémoire `feedback_audit_transversal_si_demande` créée pour éviter la rechute.
