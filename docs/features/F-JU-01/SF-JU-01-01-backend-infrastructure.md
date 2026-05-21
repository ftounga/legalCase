# Mini-spec — F-JU-01 / SF-JU-01-01 Backend infrastructure (tables + endpoint GET + service)

## Identifiant

`F-JU-01 / SF-JU-01-01`

## Feature parente

`F-JU-01` — Citations jurisprudentielles dans les outils décisionnels (FR + BE) — full auto-pilot Claude

## Statut

`draft`

## Date de création

2026-05-21

## Branche Git

`feat/SF-JU-01-01-backend-infrastructure`

---

## Objectif

Créer l'infrastructure backend (3 tables + 1 endpoint GET + 1 service) qui permet de stocker les mappings outil × branche → arrêts, de tracer les flags de veille et l'audit log, et d'exposer les citations aux composants frontend des outils décisionnels via une API REST.

---

## Comportement attendu

### Cas nominal

Quand un composant frontend d'outil décisionnel a besoin d'afficher la jurisprudence applicable à une branche de calcul active, il appelle :

```
GET /api/tools/{toolId}/jurisprudence-citations?branch={branchActive}
Authorization: Bearer <token>
```

Le backend :
1. Vérifie l'authentification (MEMBER minimum)
2. Interroge `tool_jurisprudence_mappings` (`tool_id = :toolId AND branche_calcul_id = :branchActive AND archived = false`)
3. Retourne **0 à 3 arrêts** triés par `confidence_score DESC, date_arret DESC`
4. Chaque arrêt expose : référence formatée, juridiction, date, numéro pourvoi, lien Légifrance, chapeau officiel (texte brut Cassation/Conseil d'État), date de dernière vérification, score de confiance

Si aucun mapping n'existe pour le couple `(toolId, branchActive)`, le backend retourne **liste vide `[]`** avec un statut 200 (pas une 404 — l'absence de citation est un cas nominal, pas une erreur).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `toolId` absent du `TOOL_REGISTRY` | Liste vide `[]` (l'outil existe peut-être en V2, le backend n'a pas à le savoir) | 200 |
| `branch` absent (paramètre vide) | Liste vide `[]` (tous les mappings nécessitent une branche identifiée) | 200 |
| `toolId` invalide (caractères spéciaux, > 100 caractères) | Erreur validation | 400 |
| Utilisateur non authentifié | Accès refusé | 401 |
| Endpoints d'écriture sur les 3 tables (POST/PUT/DELETE) | Accès refusé sauf `SUPER_ADMIN` | 403 |

Note : aucun cas 403 sur le GET de lecture (mappings sont globaux, lisibles par tout utilisateur authentifié du moment où il est dans un workspace actif).

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : les 131 outils du `TOOL_REGISTRY` → tous bénéficieront du même endpoint (table globale par `tool_id`)
- [x] **Autres pays** : France / Belgique → les mappings supportent les sources FR (JUDILIBRE, ArianeWeb) et BE (Juridat, Cour const. BE, Cass. BE) sans schéma distinct (colonne `juridiction` libre)
- [x] **Autres domaines** : DROIT_DU_TRAVAIL / DROIT_FAMILLE / DROIT_IMMIGRATION → table indexée par `tool_id` qui porte déjà l'information du domaine
- [x] **Autres UI patterns** : composant `<app-tool-jurisprudence-citations>` introduit en SF-JU-01-04, réutilisable par les ~80-90 outils éligibles
- [x] **Autres flows transversaux** : aucun impact auth / workspace / plans / navigation (table globale, endpoint nouveau)

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : couvert en SF-JU-01-04 (interface `JurisprudenceCitation`)
- [x] **Record / DTO backend** : `JurisprudenceCitationResponse` (record Java immutable) défini dans cette SF
- [x] **Service / logique métier** : `ToolJurisprudenceService.findByToolAndBranch(toolId, branchId)` défini dans cette SF
- [x] **Entité JPA + schéma DB** : entités `ToolJurisprudenceMapping`, `JurisprudenceWatchFlag`, `JurisprudenceAuditLog` + 3 migrations Liquibase
- [x] **Tests existants** : aucun service existant ne traite la jurisprudence d'outil. F-179 (`JurisprudenceVerificationService`) traite la vérification d'arrêts dans **documents uploadés** (use case orthogonal).

### Cas spécifique : nouvelle feature d'outil décisionnel

**Non applicable** — SF-JU-01-01 est une SF backend **infrastructure transversale**. Elle n'ajoute aucun outil décisionnel ; elle expose un endpoint nouveau consommé par les outils existants via SF-JU-01-04. Toutes les questions « Cohérence IA », « Refresh dashboard », « Pré-fill IA », « Masquage conditionnel » concernent les composants outils, pas cette SF.

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Où le nouveau pattern pourrait-il être réutilisé ?** Le service `ToolJurisprudenceService` est conçu pour être appelé **par chaque outil décisionnel** via un endpoint REST. Pas de pattern UI dans cette SF (SF frontend = SF-JU-01-04).
- [x] **Patterns concurrents** : F-179 introduit `JurisprudenceVerificationService` + `WebSearchService` pour la vérification d'arrêts cités dans documents uploadés. Use case orthogonal : F-179 lit les documents du dossier, F-JU-01 cite proactivement les arrêts mappés. **Pas de fusion** — les deux services coexistent.
- [x] **Nouveau service peut-il servir à d'autres features ?** Oui : F-98 (conclusions générées) pourra puiser dans `ToolJurisprudenceService.findByToolAndBranch()` en V2 pour citer les arrêts des outils utilisés. À documenter en SF-JU-01-04 ou ultérieure.
- [x] **Composant a-t-il un équivalent ?** Non — table `tool_jurisprudence_mappings` est nouvelle, pas de migration à faire depuis un autre stockage.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Endpoint réutilisable par les ~80 outils éligibles | Oui | Intégré dès cette SF (table globale + endpoint par `tool_id`) |
| Format unifié de citation FR + BE | Oui | Intégré dès cette SF (colonne `juridiction` libre, supporte « Cass. soc. », « CE », « Cour const. BE », « Cass. BE », « Cour trav. BE ») |
| Continuité F-98 (citations dans conclusions) | Oui | À clarifier en SF-JU-01-04 ou SF dédiée — service exposé dès cette SF, réutilisable par F-98 |
| Continuité F-241 (bouton « Ouvrir dans Doctrine » par arrêt) | V2 | Backlog ou SF-JU-01-04 V1 — service expose déjà la `reference` qui peut être réinjectée dans le pattern URL F-241 |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (endpoint réutilisable pour 131 outils, format unifié FR/BE)
- [x] Subfeature(s) parallèle(s) créée(s) : SF-JU-01-02 (cron veille mensuelle), SF-JU-01-03 (cron dérive quotidienne), SF-JU-01-04 (frontend composant + bouton signaler), SF-JU-01-05 (bootstrap auto + dashboard admin)
- [x] Backlog V2 pour : continuité F-98 (citations dans conclusions générées), continuité F-241 (bouton Doctrine par arrêt) — à creuser selon signal terrain post-livraison V1
- [x] Non applicable aux autres cibles (cas spécifique outil décisionnel — cette SF est infrastructure pure, pas un outil)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF-JU-01-01 est une SF backend pure (3 tables + 1 endpoint GET + 1 service Spring). Aucun composant Angular livré ici. Le composant frontend qui consommera l'endpoint est livré en SF-JU-01-04. La conformité F-IA-04 sera évaluée à ce moment.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : SF-JU-01-01 ne crée ni ne modifie d'outil décisionnel à champs saisissables. C'est une SF backend infrastructure transversale qui expose des mappings curatés (chargés en SF-JU-01-05 via bootstrap Claude). Aucun formulaire avocat dans cette SF.

---

## Critères d'acceptation

- [ ] **CA-01** — Migration Liquibase `300-create-tool-jurisprudence-mappings.xml` exécutée crée la table `tool_jurisprudence_mappings` avec les 11 colonnes (`id`, `tool_id`, `branche_calcul_id`, `arret_ref`, `juridiction`, `date_arret`, `numero_pourvoi`, `lien_legifrance`, `chapeau_officiel`, `last_verified_at`, `confidence_score`, `archived`), index sur `(tool_id, branche_calcul_id, archived)`, contraintes documentées dans la section « Contraintes de validation » ci-dessous.
- [ ] **CA-02** — Migration Liquibase `301-create-jurisprudence-watch-flags.xml` crée la table `jurisprudence_watch_flags` avec colonnes (`id`, `tool_id`, `branche_calcul_id`, `arret_entrant_ref`, `mapping_actuel_id`, `source` enum `CRON` / `USER_SIGNAL`, `confidence_score`, `explication`, `statut` enum `PENDING` / `REVIEWED` / `IGNORED`, `created_at`, `reviewed_at`, `reviewed_by_user_id`, `decision` enum `REPLACE` / `ADD` / `IGNORE`, `comment_user`), index sur `(statut, created_at)`.
- [ ] **CA-03** — Migration Liquibase `302-create-jurisprudence-audit-log.xml` crée la table `jurisprudence_audit_log` avec colonnes (`id`, `mapping_id`, `action` enum `AUTO_CONFIRM` / `AUTO_ADD` / `AUTO_REPLACE` / `AUTO_ARCHIVE` / `MANUAL_REPLACE` / `MANUAL_ADD` / `MANUAL_IGNORE`, `actor` enum `CRON` / `SUPER_ADMIN`, `actor_user_id` nullable, `claude_confidence` nullable, `claude_reason` nullable, `created_at`), index sur `(mapping_id, created_at)`.
- [ ] **CA-04** — Service `ToolJurisprudenceService.findByToolAndBranch(String toolId, String brancheId)` retourne `List<JurisprudenceCitationResponse>` avec **maximum 3 résultats**, triés par `confidence_score DESC, date_arret DESC`, en excluant `archived = true`.
- [ ] **CA-05** — Endpoint `GET /api/tools/{toolId}/jurisprudence-citations?branch={brancheId}` retourne 200 + JSON liste de `JurisprudenceCitationResponse` (0 à 3 éléments). Requiert authentification (rôle `MEMBER` minimum). Pas d'isolation workspace (table globale).
- [ ] **CA-06** — Si `toolId` est invalide (vide, > 100 caractères, caractères non-alphanumériques hors `-_`) → 400 avec message d'erreur explicite.
- [ ] **CA-07** — Si `branch` est absent ou vide → retour 200 + liste vide `[]` (l'outil affichera simplement aucune citation côté frontend).
- [ ] **CA-08** — Si l'utilisateur n'est pas authentifié → 401.
- [ ] **CA-09** — Aucun endpoint d'écriture (POST/PUT/DELETE) n'est exposé dans cette SF — réservé à SF-JU-01-05 (dashboard admin) avec rôle `SUPER_ADMIN`.
- [ ] **CA-10** — Tests UT couvrent : (a) retour vide si pas de mapping, (b) retour 1 arrêt, (c) retour 3 arrêts triés par confidence_score, (d) limite stricte 3 résultats même si 5 mappings actifs existent, (e) exclusion des `archived = true`, (f) tri secondaire par `date_arret DESC` à confidence égale.
- [ ] **CA-11** — Tests IT couvrent : (a) GET 200 + liste vide sur outil sans mappings, (b) GET 200 + 3 arrêts sur outil avec mappings seedés en setup, (c) GET 401 si non auth, (d) GET 400 si `toolId` invalide.
- [ ] **CA-12** — Aucune régression sur les endpoints existants (smoke tests E2E verts).

---

## Périmètre

### Hors scope (explicite)

- ❌ **Endpoints d'écriture** (POST/PUT/DELETE sur `tool_jurisprudence_mappings`, `jurisprudence_watch_flags`) — livrés en SF-JU-01-05 (dashboard admin)
- ❌ **Cron de veille mensuelle** — livré en SF-JU-01-02
- ❌ **Cron de dérive quotidienne** — livré en SF-JU-01-03
- ❌ **Bootstrap initial Claude** (population des ~3 500-4 500 mappings) — livré en SF-JU-01-05
- ❌ **Composant frontend `<app-tool-jurisprudence-citations>`** + interface `ToolJurisprudenceCitable` — livré en SF-JU-01-04
- ❌ **Bouton « Signaler un problème »** côté avocat utilisateur — livré en SF-JU-01-04
- ❌ **Dashboard admin** `/super-admin/jurisprudence-watch` — livré en SF-JU-01-05
- ❌ **Email mensuel récap** — livré en SF-JU-01-02
- ❌ **Intégration F-98 conclusions générées** (citation des arrêts dans les conclusions) — V2 selon signal terrain
- ❌ **Intégration F-241 bouton Doctrine par arrêt** — V2 selon signal terrain

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `tool_jurisprudence_mappings.archived` | `false` | Tout nouveau mapping est actif par défaut |
| `tool_jurisprudence_mappings.last_verified_at` | `NOW()` | Renseigné à l'INSERT par le service |
| `tool_jurisprudence_mappings.confidence_score` | requis, fourni par Claude (cron ou bootstrap) | Pas de défaut côté DB — null = rejet 400 lors de l'INSERT en SF-JU-01-05 |
| `jurisprudence_watch_flags.statut` | `PENDING` | Tout nouveau flag est à arbitrer |
| `jurisprudence_watch_flags.created_at` | `NOW()` | Renseigné automatiquement |
| `jurisprudence_audit_log.created_at` | `NOW()` | Renseigné automatiquement |

Comportements à la création :
- `id` est un `UUID v7` généré côté backend pour les 3 tables (cohérent avec le pattern existant des autres tables — cf. `case_files`, `decision_tool_visibility_rules`)
- Pas de `workspace_id` (tables globales)
- Pas de `created_by_user_id` sur `tool_jurisprudence_mappings` (mappings créés par cron ou par bootstrap, pas par utilisateur)
- `jurisprudence_audit_log.actor_user_id` est renseigné uniquement quand `actor = SUPER_ADMIN` (action manuelle dashboard SF-JU-01-05)

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `tool_jurisprudence_mappings.tool_id` | Oui | 100 | `^[a-zA-Z0-9_-]+$` (cohérent avec les `tool_id` du `TOOL_REGISTRY`) | Non (peut avoir plusieurs branches) | — |
| `tool_jurisprudence_mappings.branche_calcul_id` | Oui | 100 | `^[a-zA-Z0-9_-]+$` (libre, structure choisie par chaque outil) | Non | — |
| `tool_jurisprudence_mappings.arret_ref` | Oui | 200 | Format texte libre (ex : `Cass. soc. 8 janv. 2025, n° 23-12.345`) | Oui sur `(tool_id, branche_calcul_id, arret_ref)` non archivés | trim() |
| `tool_jurisprudence_mappings.juridiction` | Oui | 50 | Texte libre (ex : `Cour de cassation, chambre sociale`, `Conseil d'État, 2ème ch.`, `Cour const. BE`) | Non | trim() |
| `tool_jurisprudence_mappings.date_arret` | Oui | — | `DATE` (format ISO) | Non | — |
| `tool_jurisprudence_mappings.numero_pourvoi` | Oui | 50 | Texte libre (formats variables FR/BE) | Non | trim() |
| `tool_jurisprudence_mappings.lien_legifrance` | Oui | 500 | URL `https://www.legifrance.gouv.fr/...` ou équivalent (`https://www.const-court.be/...`, `https://juportal.be/...`) | Non | trim() |
| `tool_jurisprudence_mappings.chapeau_officiel` | Oui | 2000 | Texte brut UTF-8, pas de Markdown / HTML | Non | trim() |
| `tool_jurisprudence_mappings.last_verified_at` | Oui | — | `TIMESTAMP` | Non | — |
| `tool_jurisprudence_mappings.confidence_score` | Oui | — | `DECIMAL(3,2)` entre `0.00` et `1.00` | Non | — |
| `tool_jurisprudence_mappings.archived` | Oui | — | `BOOLEAN` (défaut `false`) | Non | — |
| `jurisprudence_watch_flags.source` | Oui | — | enum `CRON` / `USER_SIGNAL` | Non | — |
| `jurisprudence_watch_flags.statut` | Oui | — | enum `PENDING` / `REVIEWED` / `IGNORED` (défaut `PENDING`) | Non | — |
| `jurisprudence_watch_flags.decision` | Non (rempli au moment du REVIEWED) | — | enum `REPLACE` / `ADD` / `IGNORE` | Non | — |
| `jurisprudence_audit_log.action` | Oui | — | enum 7 valeurs (cf. CA-03) | Non | — |
| `jurisprudence_audit_log.actor` | Oui | — | enum `CRON` / `SUPER_ADMIN` | Non | — |

Notes :
- Pas d'isolation workspace sur les 3 tables (mappings et flags globaux)
- L'unicité sur `(tool_id, branche_calcul_id, arret_ref)` non archivés empêche les doublons actifs ; un même arrêt peut être archivé puis ré-ajouté
- `confidence_score` est requis et borné `[0.00 ; 1.00]` (côté Claude : seuls les mappings > 0.60 sont insérés en SF-JU-01-05)
- `chapeau_officiel` est limité à 2000 caractères : les chapeaux Cassation/CE font typiquement 100-800 caractères, marge confortable

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/tools/{toolId}/jurisprudence-citations?branch={brancheId}` | Oui | MEMBER |

Note : aucun endpoint POST/PUT/DELETE dans cette SF (écriture livrée en SF-JU-01-05).

### Réponse de l'endpoint

```json
[
  {
    "id": "01HJZK...",
    "arretRef": "Cass. soc. 8 janv. 2025, n° 23-12.345",
    "juridiction": "Cour de cassation, chambre sociale",
    "dateArret": "2025-01-08",
    "numeroPourvoi": "23-12.345",
    "lienLegifrance": "https://www.legifrance.gouv.fr/juri/id/JURITEXT000049XXXXXX",
    "chapeauOfficiel": "Selon l'article L. 1235-3 du code du travail, l'indemnité pour licenciement sans cause réelle et sérieuse est plafonnée par le barème...",
    "lastVerifiedAt": "2026-05-01T03:00:00Z",
    "confidenceScore": 0.92
  },
  { "... 2ᵉ arrêt ..." },
  { "... 3ᵉ arrêt ..." }
]
```

Liste vide `[]` si aucun mapping.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `tool_jurisprudence_mappings` | CREATE TABLE + SELECT | Nouvelle table — 11 colonnes, index `(tool_id, branche_calcul_id, archived)` |
| `jurisprudence_watch_flags` | CREATE TABLE | Nouvelle table — table créée mais pas alimentée dans cette SF (alimentation en SF-JU-01-02 et SF-JU-01-04 via bouton signaler) |
| `jurisprudence_audit_log` | CREATE TABLE | Nouvelle table — table créée mais pas alimentée dans cette SF (alimentation en SF-JU-01-02/05) |

### Migration Liquibase

- [x] Oui — 3 migrations dans `backend/src/main/resources/db/changelog/migrations/` :
  - `300-create-tool-jurisprudence-mappings.xml`
  - `301-create-jurisprudence-watch-flags.xml`
  - `302-create-jurisprudence-audit-log.xml`

Note : numérotation 300+ pour ne pas entrer en collision avec les migrations 200+ en cours (F-217 et autres bundles). Numéro à confirmer au moment du PR selon l'état du repo (incrémenter si conflit).

### Composants Angular (si applicable)

Aucun — SF backend pure.

### Classes Java introduites

**Entités JPA** (package `fr.ailegalcase.jurisprudence`) :
- `ToolJurisprudenceMapping` — entité principale, mappée sur `tool_jurisprudence_mappings`
- `JurisprudenceWatchFlag` — entité flag, mappée sur `jurisprudence_watch_flags`
- `JurisprudenceAuditLog` — entité audit, mappée sur `jurisprudence_audit_log`

**Repositories Spring Data JPA** :
- `ToolJurisprudenceMappingRepository` — méthode `findTop3ByToolIdAndBrancheCalculIdAndArchivedFalseOrderByConfidenceScoreDescDateArretDesc(String toolId, String brancheCalculId)`
- `JurisprudenceWatchFlagRepository` — vide dans cette SF (créé pour SF-JU-01-02/04)
- `JurisprudenceAuditLogRepository` — vide dans cette SF (créé pour SF-JU-01-02/05)

**Services Spring** :
- `ToolJurisprudenceService` — méthode publique `findByToolAndBranch(String toolId, String brancheCalculId): List<JurisprudenceCitationResponse>` qui délègue au repository et mappe vers le record DTO

**Records DTO** :
- `JurisprudenceCitationResponse` — record immutable exposé par l'endpoint (ne contient pas `archived`, ne contient pas `branche_calcul_id` qui est dans l'URL)

**Contrôleurs Spring** :
- `ToolJurisprudenceController` — endpoint `@GetMapping("/api/tools/{toolId}/jurisprudence-citations")`, sécurisé via `SecurityConfig` existant (auth required, pas de rôle spécifique car `MEMBER` = défaut)

---

## Plan de test

### Tests unitaires

- [ ] `ToolJurisprudenceServiceTest.findByToolAndBranch_returnsEmpty_whenNoMappingExists()`
- [ ] `ToolJurisprudenceServiceTest.findByToolAndBranch_returns1Arret_whenSingleMapping()`
- [ ] `ToolJurisprudenceServiceTest.findByToolAndBranch_returns3ArretsSortedByConfidence_whenMultipleMappings()`
- [ ] `ToolJurisprudenceServiceTest.findByToolAndBranch_returnsMaxThreeArrets_evenIfFiveExist()`
- [ ] `ToolJurisprudenceServiceTest.findByToolAndBranch_excludesArchivedMappings()`
- [ ] `ToolJurisprudenceServiceTest.findByToolAndBranch_sortsByDateArretDesc_whenConfidenceEqual()`
- [ ] `ToolJurisprudenceServiceTest.findByToolAndBranch_returnsImmutableDtos_notEntities()` (vérifie que le service ne fuit pas l'entité JPA)

### Tests d'intégration

- [ ] `ToolJurisprudenceControllerIT.getCitations_returns200WithEmptyList_whenNoMapping()`
- [ ] `ToolJurisprudenceControllerIT.getCitations_returns200With3Arrets_whenMappingsSeeded()`
- [ ] `ToolJurisprudenceControllerIT.getCitations_returns401_whenNotAuthenticated()`
- [ ] `ToolJurisprudenceControllerIT.getCitations_returns400_whenToolIdInvalidFormat()`
- [ ] `ToolJurisprudenceControllerIT.getCitations_returns200WithEmptyList_whenBranchParamEmpty()`
- [ ] `ToolJurisprudenceControllerIT.getCitations_returns200_evenIfToolIdNotInToolRegistry()` (l'endpoint ne vérifie pas l'existence dans `TOOL_REGISTRY` — c'est l'absence de mapping qui produit une liste vide)

### Tests Liquibase

- [ ] Migration `300-create-tool-jurisprudence-mappings.xml` rollback testé en H2 (ajout + rollback + rejoue) — pattern standard projet
- [ ] Migration `301-create-jurisprudence-watch-flags.xml` idem
- [ ] Migration `302-create-jurisprudence-audit-log.xml` idem

### Isolation workspace

- [x] **Non applicable** — raison : les 3 tables sont **globales** (pas de colonne `workspace_id`). Les mappings de jurisprudence sont les mêmes pour tous les workspaces (la jurisprudence française est identique pour tous les avocats). Le contrôle d'accès est limité à l'authentification (utilisateur connecté = OK pour lire). Les endpoints d'écriture (SF-JU-01-05) seront protégés par rôle `SUPER_ADMIN`.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité à son périmètre. Aucune modification de l'auth, du workspace context, des plans/limites, ou de la navigation. Endpoint nouveau, tables nouvelles, service nouveau.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `SecurityConfig` | Endpoint nouveau à inclure dans la liste des routes authentifiées | Test IT existant `SecurityConfigIT` (si présent) — sinon vérification manuelle que l'endpoint hérite du défaut « auth required » |
| `Spring Boot Actuator` | Métriques par défaut sur le nouvel endpoint (latence, taux d'erreur) | Aucun (additif, pas de modification de la config Actuator) |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné (justification : SF backend pure, endpoint nouveau lu uniquement par SF-JU-01-04 — pas de consommateur frontend en V1 de cette SF). Les smoke tests E2E `e2e/smoke/` couvrant auth / workspace / dashboard continuent à passer sans modification.

---

## Dépendances

### Subfeatures bloquantes

Aucune. SF-JU-01-01 est la première SF de F-JU-01, démarrable immédiatement.

### Subfeatures débloquées par cette SF

- **SF-JU-01-02** (cron veille mensuelle) — a besoin du repository `ToolJurisprudenceMappingRepository` + `JurisprudenceWatchFlagRepository` + `JurisprudenceAuditLogRepository`
- **SF-JU-01-03** (cron dérive quotidienne) — a besoin du repository `ToolJurisprudenceMappingRepository`
- **SF-JU-01-04** (frontend composant + bouton signaler) — a besoin de l'endpoint `GET /api/tools/{toolId}/jurisprudence-citations`
- **SF-JU-01-05** (bootstrap auto + dashboard admin) — a besoin des 3 tables + service de base

### Questions ouvertes impactées

- [x] Aucune — `docs/OPEN_QUESTIONS.md` n'a pas d'entrée relative à la jurisprudence ou aux outils décisionnels qui bloquerait cette SF (vérifié 2026-05-21).

---

## Notes et décisions

### Décisions techniques prises lors de la spécification

1. **Tables globales (pas d'isolation workspace)** — la jurisprudence est identique pour tous les avocats. Une isolation workspace serait sur-engineering et compliquerait la veille (chaque workspace devrait être bootstrappé séparément).

2. **`confidence_score` requis et borné `[0.00 ; 1.00]`** — Claude fournit un score à chaque mapping, le seuil de 0.60 (filtrage en SF-JU-01-05 bootstrap) garantit qu'aucun mapping faible ne pollue la base. Le seuil sera ajustable via constante de service plutôt que paramètre runtime (pas de cas d'usage qui justifie un override en V1).

3. **Pas d'enum Java pour `juridiction`** — la diversité des formulations (chambres Cassation, formations CE, Cour const. BE, juridictions BE diverses) rend un enum fragile. Texte libre normalisé via `trim()` côté service, et liste blanche optionnelle dans la doc (pas en contrainte DB).

4. **Pas de FK vers une table `arrets`** — la jurisprudence référencée est externe (Légifrance, JUDILIBRE), il n'y a pas de table interne d'arrêts à référencer. Les champs (`arret_ref`, `juridiction`, `date_arret`, etc.) sont dénormalisés pour éviter une table secondaire qui ne servirait à rien.

5. **Endpoint GET pas paginé** — limite à 3 résultats max (CA-04). Pas besoin de pagination.

6. **Limite stricte 3 arrêts via `findTop3...` Spring Data JPA** — la méthode du repository inclut la limite dans son nom (`Top3`). Pas de risque qu'un consommateur fasse un `findAll()` qui ramène 1000 lignes.

7. **DTO immutable record Java** — `JurisprudenceCitationResponse` est un `record` Java (pas une classe POJO). Pattern miroir des autres DTOs récents du projet (`CaseFileResponse`, etc.).

8. **Numérotation migrations 300+** — pour éviter la collision avec les migrations 200+ en cours (F-217 et bundles). À reconfirmer au moment du PR si d'autres SF ont consommé 280-299 entre-temps.

9. **Pas d'endpoint d'écriture dans cette SF** — toute écriture est différée à SF-JU-01-02 (cron) ou SF-JU-01-05 (dashboard admin). Cela permet de figer le contrat de lecture en SF-JU-01-01 et de paralléliser les SF backend cron (02, 03) + SF frontend (04) sans risque de course condition sur l'écriture.

10. **Continuité F-98 conclusions générées** — non incluse dans cette SF. Sera décidée en V2 selon signal terrain : faut-il que `app-conclusions-section` (F-98) puise dans `tool_jurisprudence_mappings` pour citer les arrêts des outils utilisés ? Probablement oui, mais on attend de voir comment les avocats utilisent les citations en pratique avant d'engager le dev.

### Coût estimé

- ~1,5 j dev backend (3 migrations + 3 entités JPA + 3 repositories + 1 service + 1 contrôleur + DTOs + tests UT/IT)

### Critère de fin de cette SF

L'endpoint `GET /api/tools/{toolId}/jurisprudence-citations?branch={brancheId}` est accessible sur l'environnement staging, retourne 200 + liste vide pour tout `(toolId, branch)` non encore mappé, et passe tous les tests UT + IT verts. Les tables sont créées, les migrations rollbackables, les repositories utilisables par les SF suivantes.
