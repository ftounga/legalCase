# Mini-spec — F-231 / SF-231-03 — Backend : quotas mensuels minutes vidéo par plan

## Identifiant

`F-231 / SF-231-03`

## Feature parente

`F-231` — Ingestion et analyse de pièces vidéo (MP4/MOV) — extraction frames clés + Claude Vision multi-frames

## Statut

`ready` (séquentielle après SF-231-01)

## Date de création

2026-05-09

## Branche Git

`feat/SF-231-03-quotas-video`

---

## Objectif

Ajouter un quota mensuel `video_minutes_monthly` au `PlanLimitService` pour limiter la consommation vidéo par workspace selon le plan tarifaire (SOLO 5 min/mois, TEAM 30 min/mois, PRO 120 min/mois). Code 402 dédié `VIDEO_QUOTA_EXCEEDED`.

---

## Comportement attendu

### Cas nominal

1. À chaque upload vidéo (SF-231-01), `DocumentController` appelle `PlanLimitService.checkAndConsumeVideoMinutes(workspaceId, durationSeconds)` AVANT la persistance Document.
2. `PlanLimitService` :
   - Récupère le plan actif du workspace (SOLO/TEAM/PRO/TRIAL).
   - Récupère la consommation cumulée du mois calendaire courant depuis `workspace_video_usage` (table créée par migration).
   - Si (consommation + ceil(durationSeconds/60)) > quota plan → throw `PaymentRequiredException(VIDEO_QUOTA_EXCEEDED, message)`.
   - Sinon → INSERT INTO `workspace_video_usage` (workspace_id, year_month, minutes_consumed, document_id).
3. L'upload procède normalement.
4. Côté frontend, le bandeau quota existant `<app-quota-error-banner>` (F-171) gère l'affichage de l'erreur (extension du switch sur `code`).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Quota mensuel dépassé | `PaymentRequiredException(VIDEO_QUOTA_EXCEEDED)` | 402 |
| Plan inconnu / workspace orphelin | Comportement existant `PlanLimitService` (TRIAL par défaut ?) | — |
| Race condition (2 uploads simultanés frôlant la limite) | Transaction REPEATABLE_READ + retry idempotent | 402 si effectif dépassement |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Quotas existants** : pattern réutilisé de `pages_ocr_monthly` (F-122) et `tokens_monthly` (F-34). Architecture identique.
- [x] **Codes 402** : étend l'enum `PaymentRequiredCode` (F-171) avec une nouvelle valeur `VIDEO_QUOTA_EXCEEDED`.
- [x] **Frontend quota banner** : `<app-quota-error-banner>` (F-171) doit afficher correctement le message vidéo. Étendre le mapping si besoin.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `PlanLimitService` | Oui | Méthode `checkAndConsumeVideoMinutes` ajoutée (pattern OCR) |
| `PaymentRequiredCode` enum | Oui | Nouvelle valeur `VIDEO_QUOTA_EXCEEDED` |
| Frontend `paymentRequiredInterceptor` | Oui | Pas de modification — déjà générique sur le `code` field |
| `<app-quota-error-banner>` | Vérifier | Si le composant a un mapping label par `code`, ajouter le mapping vidéo |
| Page billing | Non V1 | Pas d'achat de minutes vidéo à la carte V1 |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (sauf page billing — V2).

---

## Critères d'acceptation

- [ ] Migration Liquibase crée la table `workspace_video_usage` (workspace_id, year_month, minutes_consumed, document_id, created_at).
- [ ] `PlanLimitService.checkAndConsumeVideoMinutes(workspaceId, durationSeconds)` implémentée.
- [ ] Plans : SOLO 5 min/mois, TEAM 30 min/mois, PRO 120 min/mois, TRIAL 1 min (essai), ENTERPRISE illimité.
- [ ] Calcul minutes consommées = `ceil(durationSeconds/60)` arrondi au **dessus** (1s consommée = 1 minute facturée).
- [ ] `PaymentRequiredCode.VIDEO_QUOTA_EXCEEDED` ajouté à l'enum.
- [ ] `DocumentController` appelle `checkAndConsumeVideoMinutes` AVANT persistance pour les contentTypes vidéo.
- [ ] Tests UT : quota OK, quota dépassé, quota partiellement consommé, race condition.
- [ ] Test IT : upload vidéo qui dépasse le quota → 402.
- [ ] Frontend `<app-quota-error-banner>` mapping étendu pour `VIDEO_QUOTA_EXCEEDED`.

---

## Périmètre

### Hors scope (explicite)

- Achat de minutes vidéo à la carte (V2).
- Email proactif "vous avez atteint 80% du quota vidéo" (V2).
- Reset manuel du quota par admin (V2).
- Quotas par utilisateur (uniquement par workspace V1).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `minutes_consumed` | 0 | À chaque INSERT, valeur réelle calculée |
| `year_month` | YYYY-MM courant | Format ISO mois (ex: "2026-05") |

---

## Contraintes de validation

| Champ | Obligatoire | Format |
|-------|-------------|--------|
| `workspace_id` | Oui | UUID |
| `year_month` | Oui | VARCHAR(7), pattern `^\d{4}-\d{2}$` |
| `minutes_consumed` | Oui | INT >= 0 |
| `document_id` | Oui | UUID, FK → documents |

---

## Technique

### Endpoint(s)

Aucun nouvel endpoint exposé — la logique est interne à `DocumentController` qui orchestre quota + upload.

### Tables impactées

| Table | Opération |
|-------|-----------|
| `workspace_video_usage` | CREATE (nouvelle table) |

### Migration Liquibase

- [x] Oui — `db/changelog/migrations/{XXX}-create-workspace-video-usage.xml`

```sql
CREATE TABLE workspace_video_usage (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id UUID NOT NULL REFERENCES workspaces(id),
  year_month VARCHAR(7) NOT NULL,
  minutes_consumed INTEGER NOT NULL CHECK (minutes_consumed >= 0),
  document_id UUID NOT NULL REFERENCES documents(id),
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_wvu_workspace_month ON workspace_video_usage(workspace_id, year_month);
CREATE INDEX idx_wvu_document ON workspace_video_usage(document_id);
```

### Composants Java créés

- `WorkspaceVideoUsage` entité JPA
- `WorkspaceVideoUsageRepository` Spring Data
- `PlanLimitService.checkAndConsumeVideoMinutes(UUID workspaceId, int durationSeconds)` méthode

### Composants Java modifiés

- `PaymentRequiredCode` enum (F-171) — ajout `VIDEO_QUOTA_EXCEEDED`
- `DocumentController` — appel `checkAndConsumeVideoMinutes` pour vidéos (avant persistance)

### Composants frontend modifiés (mineur)

- `quota-error-banner.component.ts` (F-171) — ajout du label utilisateur pour `VIDEO_QUOTA_EXCEEDED` ("Quota vidéo mensuel atteint — passez au plan supérieur")

---

## Plan de test

### Tests unitaires

- [ ] `PlanLimitServiceTest` — quota OK : `checkAndConsumeVideoMinutes` insert sans throw
- [ ] `PlanLimitServiceTest` — quota dépassé : throw `PaymentRequiredException(VIDEO_QUOTA_EXCEEDED)`
- [ ] `PlanLimitServiceTest` — calcul `ceil(durationSeconds/60)` correct (61s → 2 min)
- [ ] `PlanLimitServiceTest` — quotas par plan : SOLO=5, TEAM=30, PRO=120, ENTERPRISE=∞
- [ ] `WorkspaceVideoUsageRepositoryTest` — sum minutes par workspace_id et year_month

### Tests d'intégration

- [ ] `POST /case-files/{id}/documents` vidéo MP4 SOLO 5 min/mois, déjà 4 min consommées → upload 90 s OK (consume 2, total 6) → SUR LA LIMITE.
   - **Précision** : 4 + 2 = 6 > 5 → 402.
- [ ] Idem mais 4 min déjà + upload 30 s → 4 + 1 = 5 ≤ 5 → OK.
- [ ] Plan ENTERPRISE → quota illimité → toujours OK même 1000 min.

### Isolation workspace

- [x] Applicable — la query `WHERE workspace_id = ?` garantit l'isolation.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Plans / limites** — extension directe de `PlanLimitService`
- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Navigation / routing frontend

### Composants impactés

| Composant | Impact |
|-----------|--------|
| `PlanLimitService` | Méthode ajoutée — pas de changement de signature des méthodes existantes |
| `PaymentRequiredCode` enum | Nouvelle valeur (rétrocompat) |
| `DocumentController` | Appel quota pour vidéos (pour PDF/images, pas de changement) |
| `quota-error-banner` | Mapping label étendu (pas de breaking) |

### Smoke tests E2E

- [ ] `e2e/smoke/upload-video-quota.spec.ts` (à créer) — upload répété pour atteindre le quota → bandeau erreur affiché

---

## Dépendances

### Subfeatures bloquantes

- **SF-231-01 backend ingestion vidéo** — DOIT être mergée d'abord pour avoir le hook upload vidéo où brancher le quota check. Cette SF est explicitement séquentielle.

### Sous-jacents

- Schema `workspaces` existant
- F-171 `PaymentRequiredCode` enum existant

---

## Notes et décisions

- **Choix des limites** : SOLO 5 min, TEAM 30 min, PRO 120 min — basé sur l'estimation que :
  - Une vidéo type caméra de surveillance fait 12-30 s
  - SOLO = ~10 vidéos/mois
  - TEAM = ~60 vidéos/mois
  - PRO = ~240 vidéos/mois
- **Arrondi au-dessus** : 1 s = 1 min facturée, choix anti-abus + simplicité comptable.
- **Année-mois calendaire** : reset au 1er du mois, pas d'année glissante. Cohérent avec le pattern OCR existant.
- **Pas de mécanisme de "report"** des minutes non consommées sur le mois suivant.
- **Migration** : la valeur exacte du numéro de migration sera à choisir au moment du dev (la dernière migration en date est à vérifier dans `db/changelog/`).
