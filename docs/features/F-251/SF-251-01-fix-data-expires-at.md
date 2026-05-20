# Mini-spec — F-251 / SF-251-01 — Fix data rétroactif `subscriptions.expires_at` NULL

## Identifiant

`F-251 / SF-251-01`

## Feature parente

`F-251` — Fiabilisation de la période d'évaluation pour les comptes provisionnés en bypass IHM

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-251-01-fix-data-expires-at`

---

## Objectif

Corriger en base les souscriptions FREE existantes qui ont `expires_at IS NULL` en leur affectant `COALESCE(started_at, NOW()) + 14 days`, pour rétablir l'affichage de la date de fin d'évaluation et armer le mécanisme de bascule lecture seule (`PlanLimitService.isExpiredFree`).

---

## Comportement attendu

### Cas nominal

Migration Liquibase exécutée au démarrage backend (staging puis prod). Pour chaque row de `subscriptions` où `plan_code = 'FREE'` ET `expires_at IS NULL` :

- si `started_at` non NULL : `expires_at = started_at + INTERVAL '14 days'`
- sinon (started_at NULL aussi) : `expires_at = NOW() + INTERVAL '14 days'`

Effet immédiat (sans redéploiement frontend) :

- Carte « Période d'évaluation » du `workspace-admin` affiche la date.
- Bandeau `trial-banner` apparaît avec compte-à-rebours.
- `PlanLimitService.isExpiredFree` renvoie `true` une fois la date passée → bascule en lecture seule (quotas 0).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|----------------------|
| Migration ré-exécutée (idempotence) | 0 rows touchées (filtre `expires_at IS NULL`) |
| `started_at` également NULL | Fallback `NOW() + 14 days` |
| Plan autre que FREE avec `expires_at IS NULL` | **Hors scope** — pas touché (les plans payants n'utilisent pas `expires_at` de la même manière, géré côté Stripe webhook) |
| Échec exécution Liquibase | Boot fail standard, rollback no-op (impossible de restaurer NULL sans état antérieur) |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : non applicable (correctif data backend, ne touche pas les outils décisionnels)
- [x] **Autres pays** : non applicable (FR et BE traités identiquement par le modèle `Subscription`)
- [x] **Autres domaines** : non applicable (transverse à tous les domaines)
- [x] **Autres UI patterns** : non applicable (pas de modification UI, le frontend consomme déjà `expiresAt`)
- [x] **Autres flows transversaux** : **Plans / limites** scanné — 4 gates (`getMaxOpenCaseFilesForWorkspace`, `getMaxDocumentsPerCaseFileForWorkspace`, `isCaseAnalysisLimitReached`, `isReAnalysisLimitReached`) reposent sur `isExpiredFree` ; la correction data les arme automatiquement.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Frontend `workspace-admin` / `trial-banner` / `workspace-billing` | Oui | Aucune modification — les 3 composants consomment déjà `expiresAt` |
| Backend `PlanLimitService.isExpiredFree` + 4 gates | Oui | Aucune modification — garde `expiresAt != null` reste pertinente (défense en profondeur post-fix) |
| SF-251-02 (garde-fou `@PrePersist`) | Oui | Subfeature séquentielle qui prévient toute récidive |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (correction data seule)
- [x] Subfeature parallèle SF-251-02 créée pour le garde-fou backend (prévention)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — SF data backend pure, aucun composant frontend décisionnel modifié.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — SF data backend pure, sans outil décisionnel ni champ saisissable.

---

## Critères d'acceptation

- [x] Migration Liquibase `255-fix-subscriptions-free-expires-at-null.xml` créée avec changeset id unique et incrémenté.
- [x] Pour chaque row `subscriptions` où `plan_code = 'FREE'` ET `expires_at IS NULL`, `expires_at` est mis à `COALESCE(started_at, NOW()) + INTERVAL '14 days'`.
- [x] La migration est **idempotente** : ré-exécutée, le nombre de rows touchées est 0 (cf. filtre `WHERE expires_at IS NULL`).
- [x] Logging du nombre de rows touchées via Liquibase `<sql>` retournant le count (ou via le rapport d'exécution Liquibase standard).
- [x] Test d'intégration `SubscriptionsBackfillExpiresAtIT` qui :
  - insère 1 row FREE avec `expires_at = NULL` et `started_at` non NULL (date `2026-05-01`)
  - insère 1 row FREE avec `expires_at = NULL` et `started_at = NULL`
  - insère 1 row FREE avec `expires_at = '2027-01-01'` (déjà fixée, ne doit pas être modifiée)
  - insère 1 row SOLO avec `expires_at = NULL` (plan payant, ne doit pas être modifiée)
  - exécute la migration via Liquibase context test
  - vérifie : row 1 → `expires_at = 2026-05-15`, row 2 → `expires_at ≈ now+14d`, row 3 → inchangée, row 4 → inchangée
- [x] Rollback Liquibase explicitement **no-op** documenté (impossible de restaurer NULL aveuglément, on ne sait pas distinguer les rows qu'on a touchées de celles qui auraient un `expires_at` fixé pour d'autres raisons après la migration).
- [x] Smoke build : `./mvnw verify` passe vert.

---

## Périmètre

### Hors scope (explicite)

- Plans payants (SOLO/TEAM/PRO) avec `expires_at IS NULL` — laissés tel quel (gestion Stripe).
- Garde-fou `@PrePersist` Java — SF-251-02 séquentielle.
- Audit / dashboard super-admin listant les comptes FREE expirés — hors scope F-251.
- Skill `prospect-account-bootstrap` — SF-251-03 optionnelle, traitée si signal.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `expires_at` (après migration) | `COALESCE(started_at, NOW()) + INTERVAL '14 days'` | Appliqué uniquement aux rows FREE avec `expires_at IS NULL` au moment de l'exécution |

Comportements à la création de nouvelles rows :
- Inchangés par cette SF — les nouvelles créations passent toujours par `WorkspaceService.createWorkspace` qui fixe `expires_at = now + 14d` ; SF-251-02 ajoutera un garde-fou `@PrePersist`.

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs | Notes |
|-------|-------------|------------------|-------|
| `expires_at` | Non au niveau colonne (reste nullable — non touché par SF-251-01) | `TIMESTAMP` PostgreSQL | Pas de contrainte NOT NULL ajoutée — risque de bloquer des plans payants legacy ; le garde-fou applicatif SF-251-02 cible spécifiquement FREE |

---

## Technique

### Endpoint(s)

Aucun — migration data seule.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `subscriptions` | UPDATE | Filtre `plan_code='FREE' AND expires_at IS NULL` |

### Migration Liquibase

- [x] Oui — `255-fix-subscriptions-free-expires-at-null.xml`

Contenu (résumé) :

```xml
<changeSet id="255-fix-subscriptions-free-expires-at-null" author="ailegalcase">
  <comment>
    F-251 SF-251-01 — corrige les souscriptions FREE provisionnées hors chaîne
    WorkspaceService.createWorkspace (canal bypass IHM super-admin) qui avaient
    expires_at NULL. Applique COALESCE(started_at, NOW()) + 14 jours pour
    rétablir l'affichage trial côté avocat et armer PlanLimitService.isExpiredFree.
  </comment>
  <sql dbms="postgresql">
    UPDATE subscriptions
    SET expires_at = COALESCE(started_at, NOW()) + INTERVAL '14 days'
    WHERE plan_code = 'FREE'
      AND expires_at IS NULL;
  </sql>
  <sql dbms="h2">
    UPDATE subscriptions
    SET expires_at = DATEADD('DAY', 14, COALESCE(started_at, NOW()))
    WHERE plan_code = 'FREE'
      AND expires_at IS NULL;
  </sql>
  <rollback>
    <!-- No-op : impossible de restaurer NULL aveuglément sans perdre les rows
         qui auraient été légitimement complétées après la migration. -->
  </rollback>
</changeSet>
```

### Composants Angular (si applicable)

Aucun.

---

## Plan de test

### Tests unitaires

- [x] N/A — pas de logique Java métier ajoutée par SF-251-01. Le code applicatif est inchangé.

### Tests d'intégration

- [x] `SubscriptionsBackfillExpiresAtIT.shouldBackfillFreeNullExpiresAt` — 4 fixtures (FREE+started, FREE+NULL, FREE déjà fixée, SOLO NULL) vérifient le périmètre exact de l'UPDATE.

### Isolation workspace

- [x] Non applicable — migration globale.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Plans / limites** — touche indirectement `PlanLimitService.isExpiredFree` en armant les rows existantes. Pas de changement de logique applicative.
- [x] **Workspace context** — touche la subscription liée au workspace.
- [ ] Auth / Principal
- [ ] Navigation / routing

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `PlanLimitService.isExpiredFree` | Bascule en `true` pour les comptes dont `started_at` est ancien (> 14j) | Test IT vérifie post-migration que `expires_at` est cohérent ; les unit tests `PlanLimitServiceTest` existants restent verts |
| `WorkspaceService.getCurrentWorkspace` | Renvoie `expiresAt` non NULL au frontend | Pas de changement de contrat |
| Frontend `workspace-admin` / `trial-banner` | Affichage active pour les rows précédemment NULL | Validation visuelle staging |

### Smoke tests E2E concernés

- [ ] `e2e/smoke` — aucun smoke test ne couvre la trial pour l'instant.
- [x] Aucun smoke test concerné (justification : pas de modification de flow utilisateur, fix data backend pur).

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **Choix de la date de bascule** : `COALESCE(started_at, NOW()) + 14 days` plutôt que `NOW() + 14 days` aveugle pour ne pas offrir une « rallonge trial » indue aux comptes provisionnés depuis longtemps. Cas Marjolaine RENVERSEZ : `started_at` daté du provisionnement réel → calcul d'expiration cohérent avec la date où la démo a eu lieu (~18/05). Si `started_at` est aussi NULL (cas très improbable), fallback `NOW()+14d`.
- **Rollback no-op assumé** : la migration n'est pas réversible par construction. Documenté dans le changeset.
- **Pas de contrainte NOT NULL** : ajoutée plus tard si SF-251-02 démontre que toutes les chaînes garantissent désormais `expires_at`. Pour V1 fix, on évite tout risque de bloquer un row Stripe payant ; le garde-fou `@PrePersist` ciblera spécifiquement FREE.
- **Numéro de migration 255** : dernier numéro existant `254-create-c4-onem-checklist-analyses.xml`. Pas de collision attendue (autres sessions concurrentes pourraient prendre 255 — vérifier au push).
