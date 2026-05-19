# Mini-spec — F-180 / SF-180-01 — Backend audit dashboard tiles

> Étape 1 du cycle. Validée AVANT le dev.

---

## Identifiant

`F-180 / SF-180-01`

## Feature parente

`F-180` — Audit dashboard tiles F-167 (mappers en erreur + tiles dormantes vs actives)

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-180-01-backend-audit`

---

## Objectif

Persister chaque crash de mapper `DashboardTile` en base et exposer un endpoint super-admin qui retourne le dernier rapport d'audit (mappers en erreur 168h + tiles dormantes/actives), alimenté par un `@Scheduled` hebdomadaire.

---

## Comportement attendu

### Cas nominal

1. **Instrumentation crash** : `CaseFileDashboardService.addSafely(...)` reçoit désormais le `toolId` du mapper. Quand un mapper jette une exception, en plus du WARN existant, le service persiste une row dans `dashboard_tile_crashes` (`tool_id`, `case_file_id`, `exception_class`, `exception_message`, `occurred_at`). Si cette persistance échoue elle-même, elle est catchée (fail-open du fail-open) — le dashboard de l'avocat n'est jamais dégradé.
2. **Run d'audit** : `DashboardAuditService.runAudit()` :
   - groupe `dashboard_tile_crashes` par `tool_id` sur les **168 dernières heures** → liste 🔴 (toolId, crashCount, lastExceptionClass, lastExceptionMessage, lastOccurredAt) ;
   - purge les crashes de plus de **30 jours** (rétention) ;
   - énumère via `information_schema.tables` toutes les tables de résultat décisionnel (suffixe `_analyses` hors tables pipeline `chunk_analyses` / `document_analyses` / `case_analyses`, + set explicite `immigration_recours`, `immigration_title_decisions`, `immigration_work_rights`), exécute un `SELECT count(*)` sur chacune ;
   - 0 row → liste 🟡 dormantes ; ≥ 1 row → liste 🟢 actives triée par count desc ;
   - persiste une row dans `dashboard_audit_runs` (`ran_at`, `crashed_json`, `dormant_json`, `active_json`).
3. **Cron** : `@Scheduled(cron = "0 0 8 * * MON", zone = "UTC")` appelle `runAudit()` chaque lundi 8h UTC.
4. **Endpoint GET** : `GET /api/v1/super-admin/dashboard-audit/latest` → retourne le dernier `dashboard_audit_runs` désérialisé. Si aucun run n'existe encore → déclenche un `runAudit()` à la volée et retourne son résultat.
5. **Endpoint POST** : `POST /api/v1/super-admin/dashboard-audit/run` → force un `runAudit()` immédiat et retourne le rapport (bouton « Relancer maintenant »).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Appelant non authentifié | Rejet sécurité Spring | 401 |
| Appelant authentifié mais non super-admin | `assertSuperAdmin` lève `ResponseStatusException` | 403 |
| Persistance d'un crash échoue (DB indisponible le temps d'un INSERT) | Catchée silencieusement, WARN loggé, tile non dégradée | — (pas d'API) |
| Une table `_analyses` introuvable au count (drop concurrent) | Count de cette table = exception catchée → table ignorée du rapport, WARN loggé | — |
| `runAudit()` échoue intégralement | Exception loggée, le cron ne propage pas (pattern `BacklogSyncScheduler`) | — |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : F-180 ne crée pas un outil décisionnel — c'est un outil de pilotage interne. Il *observe* les 85 mappers existants sans les modifier (sauf la signature de `addSafely`, voir ci-dessous). Aucun outil métier n'est altéré dans sa logique.
- [x] **Autres pays** : non applicable — F-180 est transversal, aucune adaptation FR/BE (confirmé spec PRODUCT_SPEC « transversal, aucune adaptation par domaine ou pays »).
- [x] **Autres domaines** : non applicable — idem.
- [x] **Autres UI patterns** : non applicable côté backend.
- [x] **Autres flows transversaux** : pas d'impact auth / workspace context / plans / navigation (voir Analyse d'impact).

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Nouveau service `DashboardAuditService`** : service applicatif super-admin transversal. Zones de réutilisation scannées : aucun autre consommateur prévu — c'est un service de pilotage spécifique à F-167. Pas de pattern concurrent (aucun audit runtime des tiles n'existe). `BacklogSyncService` (F-178) est le pattern de référence sib­ling (service + scheduler + table de runs) — F-180 le réplique sans le dupliquer.
- [x] **Modification de la signature de `addSafely`** : `addSafely(List, Supplier)` → `addSafely(List, String toolId, Supplier)`. C'est une méthode **privée** de `CaseFileDashboardService` — aucun consommateur externe. Les 108 appels internes sont mis à jour dans le même commit. Le `toolId` passé est l'identifiant TOOL_REGISTRY canonique déjà émis par chaque mapper.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| 85+ mappers `tileFromXxx` | Oui (observés) | Aucune modification de leur logique — seul `addSafely` est instrumenté |
| `addSafely` privée | Oui | Signature étendue, 108 appels mis à jour dans cette SF |
| `DashboardTileToolIdIntegrityIT` (SF-DT-36-03) | Oui | Non impacté — il scanne `new DashboardTile("...")`, pas `addSafely`. Régression vérifiée : le test continue de passer |
| Tables `*_analyses` | Oui (comptées) | Lecture seule via `information_schema` — aucune migration sur elles |
| `BacklogSyncService` / scheduler | Oui (pattern de réf.) | Pattern répliqué, pas partagé — services distincts |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (instrumentation de `addSafely` couvre les 108 appels en une fois).
- [x] Non applicable aux autres cibles (pays / domaines) — justification : F-180 est un outil de pilotage transversal sans dimension métier.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF-180-01 est une SF backend pure (instrumentation + service + endpoints + cron). Elle ne livre aucun composant frontend décisionnel, n'a pas d'entrée `TOOL_REGISTRY`, ne touche pas au panel F-IA-04. F-180 n'est pas un outil décisionnel métier mais un outil de pilotage interne.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : SF-180-01 ne crée aucun outil décisionnel à champs saisissables. Les tables créées (`dashboard_tile_crashes`, `dashboard_audit_runs`) sont alimentées automatiquement par l'instrumentation et le cron, jamais par un formulaire avocat. Aucune analyse IA n'est impliquée.

---

## Contrat API (figé — importé par SF-180-02)

### `GET /api/v1/super-admin/dashboard-audit/latest`

- Auth : Oui — super-admin uniquement (`SuperAdminService.assertSuperAdmin`)
- Réponse `200` : `DashboardAuditReport`

```jsonc
{
  "ranAt": "2026-05-19T08:00:00Z",          // Instant ISO-8601, jamais null
  "crashedMappers": [                        // 🔴 — peut être [] (état sain)
    {
      "toolId": "F-DT-08-licenciement-validity",
      "crashCount": 3,
      "lastExceptionClass": "com.fasterxml.jackson.databind.JsonMappingException",
      "lastExceptionMessage": "Cannot deserialize ... (truncated 500 chars)",
      "lastOccurredAt": "2026-05-18T14:22:10Z"
    }
  ],
  "dormantTiles": [                          // 🟡 — peut être []
    { "tableName": "regime_algerien_analyses", "rowCount": 0 }
  ],
  "activeTiles": [                           // 🟢 — trié par rowCount desc, peut être []
    { "tableName": "licenciement_analyses", "rowCount": 142 }
  ]
}
```

### `POST /api/v1/super-admin/dashboard-audit/run`

- Auth : Oui — super-admin uniquement
- Body : aucun
- Réponse `200` : `DashboardAuditReport` (même schema que ci-dessus — le run fraîchement exécuté)

### Codes d'erreur (les deux endpoints)

| Code | Cas |
|------|-----|
| 401 | Non authentifié |
| 403 | Authentifié mais non super-admin |

---

## Critères d'acceptation

- [ ] La table `dashboard_tile_crashes` est créée par migration Liquibase `251-create-dashboard-audit-tables.xml` avec colonnes `id`, `tool_id`, `case_file_id`, `exception_class`, `exception_message`, `occurred_at`.
- [ ] La table `dashboard_audit_runs` est créée par la même migration avec colonnes `id`, `ran_at`, `crashed_json`, `dormant_json`, `active_json`, `created_at`.
- [ ] `addSafely` reçoit un `toolId` ; quand un mapper crashe, une row est insérée dans `dashboard_tile_crashes` avec le bon `tool_id` et `exception_class` / `exception_message`.
- [ ] Si l'INSERT du crash échoue, `addSafely` reste fail-open : la liste de tiles est retournée sans la tile crashée, aucune exception propagée.
- [ ] `DashboardAuditService.runAudit()` groupe les crashes par `tool_id` sur 168h, purge ceux > 30j, compte les rows des tables `_analyses` décisionnelles, et persiste une row `dashboard_audit_runs`.
- [ ] `GET /api/v1/super-admin/dashboard-audit/latest` retourne le dernier run ; si aucun run n'existe, en déclenche un.
- [ ] `POST /api/v1/super-admin/dashboard-audit/run` force un run et retourne le rapport.
- [ ] Les deux endpoints répondent `403` pour un utilisateur authentifié non super-admin.
- [ ] Le `@Scheduled(cron = "0 0 8 * * MON", zone = "UTC")` est configuré sur un composant scheduler dédié.
- [ ] `DashboardTileToolIdIntegrityIT` (SF-DT-36-03) passe toujours — aucune régression.

---

## Périmètre

### Hors scope (explicite)

- Alerting Slack / email quand un crash est détecté (V2 selon volume — spec).
- Drill-down crash → caseFile exposé par l'API (PII — la colonne `case_file_id` est stockée pour usage interne futur mais jamais retournée par l'endpoint).
- Audit cross-feature au-delà de F-167 (cards F-IA-04, calculator services hors dashboard) — V2.
- Export CSV/JSON, graphique d'évolution dans le temps — V2.
- Frontend (tab « Audit dashboard ») — c'est SF-180-02.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `dashboard_tile_crashes.occurred_at` | `now()` | renseigné à l'instant du crash par `@PrePersist` |
| `dashboard_audit_runs.ran_at` | `now()` | renseigné au début du run |
| `dashboard_audit_runs.created_at` | `now()` | `@PrePersist` |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs |
|-------|-------------|-------------|------------------|
| `tool_id` | Oui | 100 | identifiant TOOL_REGISTRY |
| `exception_class` | Oui | 255 | nom de classe Java |
| `exception_message` | Non | 2000 | tronqué à 2000 chars avant INSERT pour éviter un payload géant |
| `case_file_id` | Non | — | UUID, nullable (un crash peut survenir hors contexte caseFile résolu) |
| `crashed_json` / `dormant_json` / `active_json` | Oui | — | TEXT — JSON sérialisé (compat H2 + PostgreSQL) |

Notes :
- `exception_message` tronqué à 2000 caractères côté service avant persistance.
- Les colonnes JSON sont en `TEXT` (pas `jsonb`) pour compatibilité H2 profil dev — pattern `prudhome_fiches` / `regime_communaute_legale_be_analyses`.

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/super-admin/dashboard-audit/latest` | Oui | super-admin |
| POST | `/api/v1/super-admin/dashboard-audit/run` | Oui | super-admin |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `dashboard_tile_crashes` | CREATE / INSERT / SELECT / DELETE | nouvelle — INSERT par `addSafely`, SELECT+DELETE par `runAudit` |
| `dashboard_audit_runs` | CREATE / INSERT / SELECT | nouvelle — INSERT par `runAudit`, SELECT par endpoint |
| `*_analyses` (≈95 tables) | SELECT count(*) | lecture seule, aucune modification |
| `information_schema.tables` | SELECT | énumération des tables `_analyses` |

### Migration Liquibase

- [x] Oui — `251-create-dashboard-audit-tables.xml`
- Réversible : `<rollback><dropTable .../></rollback>` sur les 2 tables.

### Aucun composant Angular (SF backend pure)

---

## Plan de test

### Tests unitaires (`DashboardAuditServiceTest` — Mockito)

- [ ] `runAudit` — cas nominal : crashes groupés par toolId sur 168h, rapport sérialisé, row `dashboard_audit_runs` persistée.
- [ ] `runAudit` — purge : les crashes > 30j sont supprimés.
- [ ] `runAudit` — un crash de 169h n'apparaît pas dans `crashedMappers` (fenêtre 168h).
- [ ] `runAudit` — tables 0 row → dormantTiles ; tables ≥ 1 row → activeTiles triées desc.
- [ ] `runAudit` — count d'une table en erreur → table ignorée, pas d'exception propagée.
- [ ] `getLatest` — aucun run → déclenche un run.

### Tests d'intégration (`DashboardAuditIT` — `@SpringBootTest`)

- [ ] `GET /dashboard-audit/latest` → 200, structure JSON conforme au contrat.
- [ ] `GET /dashboard-audit/latest` → 403 si utilisateur non super-admin.
- [ ] `POST /dashboard-audit/run` → 200, nouveau run persisté.
- [ ] `POST /dashboard-audit/run` → 403 si utilisateur non super-admin.
- [ ] Instrumentation : un crash de mapper persiste une row `dashboard_tile_crashes` (via un mapper dont le repository mocké jette une exception, ou test ciblé de `addSafely`).
- [ ] `DashboardTileToolIdIntegrityIT` passe toujours (non-régression du garde-fou SF-DT-36-03).

### Isolation workspace

- [x] **Non applicable** — F-180 est un outil de pilotage super-admin transversal. `dashboard_tile_crashes` et `dashboard_audit_runs` n'ont pas de `workspace_id` (données globales d'observabilité produit, pattern identique aux tables `backlog_*` de F-178). L'accès est gardé par `assertSuperAdmin`, pas par isolation workspace. La colonne `case_file_id` de `dashboard_tile_crashes` n'est jamais exposée.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — SF-180-01 n'ajoute pas de type d'auth, ne modifie pas le Principal ni la résolution de workspace, ne touche pas `PlanLimitService`, n'ajoute aucune route Angular. Les nouveaux endpoints réutilisent le gating super-admin existant (`assertSuperAdmin`) sans le modifier.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — justification : pas d'impact auth / workspace / navigation. Les endpoints sont sous `/api/v1/super-admin/*` déjà couvert par le gating existant.

---

## Dépendances

### Subfeatures bloquantes

- F-167 — Done (mappers + `addSafely`).
- F-178 — Done (infra super-admin, pattern scheduler).
- SF-DT-36-03 — Done (garde-fou statique, non impacté).

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

- **Signature de `addSafely`** : passe de `(List, Supplier)` à `(List, String toolId, Supplier)`. Méthode privée → aucun impact externe. Le `toolId` passé est le même littéral que le mapper émet dans `new DashboardTile("...", ...)`. Décision documentée dans la PR.
- **Découverte des tables `_analyses` via `information_schema`** : choisi plutôt qu'un map `toolId → table` codé en dur (85 entrées, dérive garantie au prochain outil) ou qu'un couplage aux 85 repositories. `INFORMATION_SCHEMA.TABLES` est SQL-standard (H2 + PostgreSQL). Exclusion explicite des 3 tables pipeline (`chunk_analyses`, `document_analyses`, `case_analyses`) qui ne sont pas des résultats d'outil décisionnel. Set explicite supplémentaire pour les 3 tables décisionnelles ne suivant pas le suffixe (`immigration_recours`, `immigration_title_decisions`, `immigration_work_rights`).
- **`getLatest` déclenche un run si vide** : évite un endpoint qui retourne `404`/`null` au premier accès avant le premier lundi 8h. Pattern « lazy first run ».
- **Robustesse JVM** : la persistance en DB (vs grep logs) est l'invariant central de F-180 (cf. SF-180-00-coherence invariant 2).
