# Mini-spec — F-JU-01 / SF-JU-01-09 Transactions par-entrée + reprise sur erreur

## Identifiant

`F-JU-01 / SF-JU-01-09`

## Feature parente

`F-JU-01` — Citations jurisprudentielles dans les outils décisionnels (FR + BE) — full auto-pilot Claude

## Statut

`draft`

## Date de création

2026-05-27

## Branche Git

`feat/SF-JU-01-09-tx-par-entree`

---

## Objectif

Découper le `@Transactional` global de `JurisprudenceBootstrapService.runBootstrap` en transactions **par-entrée** pour garantir que :
- (a) un kill de pod ou une erreur DB sur l'entrée N ne déclenche pas un rollback des N-1 mappings déjà créés,
- (b) une erreur sur une entrée individuelle (contrainte unique, FK, timeout) est journalisée et l'opération continue sur l'entrée N+1,
- (c) le compteur `created` retourné dans la `JurisprudenceBootstrapResponse` reflète **réellement** ce qui est en DB après l'appel.

---

## Comportement attendu

### Cas nominal

- `runBootstrap` ne porte plus `@Transactional` au niveau méthode.
- Pour chaque entrée du CSV : si Claude renvoie une action persistable (`ADD`/`REPLACE`/`CONFIRM`/`ARCHIVE`) avec un `arretChoisi` valide, la persistance (`tool_jurisprudence_mappings` + `jurisprudence_audit_log`) est exécutée dans une **transaction dédiée** via `TransactionTemplate`.
- Chaque transaction réussie est immédiatement commitée → visible en DB / pg_stat_activity.
- En cas d'erreur DB (timeout, contrainte, FK…) sur une entrée : la transaction est rollback **localement**, l'entrée est comptée comme `skipped`, l'erreur est journalisée en `WARN`, et la boucle continue sur l'entrée suivante.
- À la fin : `JurisprudenceBootstrapResponse(processed, created, skipped, durationMs)` reflète l'état réel.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Erreur DB sur une entrée (contrainte unique, FK, timeout, deadlock) | Log WARN, rollback local de cette entrée, skipped++, boucle continue | n/a (interne) |
| Claude indisponible / 0 candidat / `NONE` | Comportement inchangé (skipped++, pas de persistance) | n/a |
| Erreur fatale hors transaction (OOM, NPE non gérée) | L'exception remonte au caller du controller, les entrées déjà commitées sont conservées | 500 |
| Kill du pod en cours de bootstrap | Les entrées déjà commitées sont conservées. Les entrées en cours ou non encore atteintes sont perdues — le SUPER_ADMIN doit relancer le bootstrap **uniquement sur les entrées manquantes** (gestion manuelle V1, idempotence du bootstrap traitée hors scope cf. notes) | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — non applicable, refactor interne F-JU-01
- [x] **Autres pays** — non applicable
- [x] **Autres domaines** — non applicable
- [x] **Autres UI patterns** — non applicable, backend pur (le retour HTTP reste identique : `JurisprudenceBootstrapResponse`)
- [x] **Autres flows transversaux** — non applicable

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `JurisprudenceVeilleCronService` (SF-JU-01-02) | Oui — cron mensuel utilise `@Transactional` global similaire ? | À auditer dans cette SF — si même pattern, étendre le découpage. Sinon, **noter en hors scope**. |
| `JurisprudenceDriftService` (SF-JU-01-03) | Oui — cron quotidien idem ? | Idem |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature **après audit des 2 crons** ci-dessus (lecture du code source au moment du dev — si le pattern `@Transactional` global existe, on étend ; sinon non applicable, mentionné en notes)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — SF backend pure, aucun composant frontend touché. Contrat HTTP `JurisprudenceBootstrapResponse` inchangé.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — refactor pur de la gestion transactionnelle.

---

## Critères d'acceptation

- [ ] `runBootstrap` ne porte plus l'annotation `@Transactional` au niveau méthode.
- [ ] La persistance (`mappingRepository.save` + `auditLogRepository.save`) est exécutée dans une transaction dédiée par entrée via `TransactionTemplate` ou via un bean dédié `@Transactional(propagation = REQUIRES_NEW)`.
- [ ] Un test unitaire `JurisprudenceBootstrapServiceTest.shouldContinueOnSingleEntryFailure` simule une erreur DB sur l'entrée 2 (parmi 3) et vérifie que les entrées 1 et 3 sont persistées + comptées en `created`, l'entrée 2 en `skipped`.
- [ ] Un test unitaire vérifie que `TransactionTemplate.execute(...)` est invoqué **exactement N fois** pour N entrées persistables (pas de transaction superflue, pas de transaction manquante).
- [ ] Le contrat HTTP `JurisprudenceBootstrapResponse` reste **identique** (mêmes champs, mêmes types) → backward compatible avec le frontend SF-JU-01-06/07.
- [ ] Le log INFO final `F-JU-01 — Bootstrap done: X processed, Y created, Z skipped, T ms` reste émis, mais les valeurs reflètent le comportement par-entrée.
- [ ] Anti-régression : les 68 tests existants du module `jurisprudencemapping` continuent de passer.

---

## Périmètre

### Hors scope (explicite)

- **Idempotence du bootstrap** (reprise après kill) : il n'y a pas de table `bootstrap_progress` pour pister quelles entrées ont déjà été traitées. Si l'admin relance le bootstrap avec le même CSV après un kill, des doublons peuvent apparaître selon les contraintes DB existantes. Géré côté admin manuellement V1.
- **Endpoint async + polling** : le côté synchrone (toast 50x après timeout NGINX 120s) reste tel quel. Traité par **SF-JU-01-10**.
- **Refactor des crons** SF-JU-01-02 et SF-JU-01-03 : si à l'audit on découvre qu'ils utilisent le même pattern problématique, on étend le découpage dans cette SF ; sinon, **hors scope**.

---

## Valeurs initiales

Non applicable — aucune nouvelle entité.

---

## Contraintes de validation

Non applicable — pas de nouveau champ saisissable.

---

## Technique

### Endpoint(s)

Inchangé : `POST /api/admin/jurisprudence/bootstrap` (`JurisprudenceWatchAdminController`).

### Tables impactées

Aucune nouvelle table. Pas de migration.

### Migration Liquibase

- [ ] Oui
- [x] Non applicable

### Composants Angular (si applicable)

Aucun. Frontend non touché.

### Détail des modifications backend

| Fichier | Modification |
|---------|--------------|
| `JurisprudenceBootstrapService.java` | (1) Retirer `@Transactional` de `runBootstrap`. (2) Injecter `PlatformTransactionManager` + instancier `TransactionTemplate`. (3) Wrapper l'appel à `persistTopCandidates` dans `txTemplate.executeWithoutResult(...)` avec `try/catch` autour pour ne pas faire échouer la boucle. (4) Logger en WARN avec `entry.toolId()` + `entry.brancheCalculId()` + message d'exception en cas d'échec d'une entrée. |
| `JurisprudenceBootstrapServiceTest.java` (nouveau ou étendu) | Tests unitaires avec mocks Mockito : (a) chemin nominal 3 entrées → 3 transactions exécutées ; (b) erreur DB sur entrée 2 → entrées 1 + 3 commitées, compteurs cohérents. |

---

## Plan de test

### Tests unitaires

- [ ] `JurisprudenceBootstrapServiceTest.shouldRunOneTransactionPerPersistableEntry` — 3 entrées dont Claude renvoie `ADD` 3× → `transactionTemplate.executeWithoutResult` appelé 3 fois.
- [ ] `JurisprudenceBootstrapServiceTest.shouldContinueOnSingleEntryFailure` — entrée 2 simule une `DataAccessException` lors du save → vérifie : entrée 1 et 3 sont sauvegardées normalement, entrée 2 compte en skipped, `created == 2`, `skipped == 1`.
- [ ] `JurisprudenceBootstrapServiceTest.shouldSkipNonPersistableWithoutTransaction` — entrée Claude renvoie `NONE` → `transactionTemplate.executeWithoutResult` **non appelé**, skipped++.

### Tests d'intégration

Non requis V1 — la logique transactionnelle est suffisamment couverte par les unitaires avec mocks. Si un IT staging révèle un problème post-déploiement, on ajoutera un IT dédié.

### Isolation workspace

- [x] Non applicable — mappings globaux.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — refactor interne au service. Endpoint inchangé, contrat HTTP inchangé.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| Frontend SF-JU-01-06/07 (panel super-admin) | Aucun — `JurisprudenceBootstrapResponse` inchangée | N/A |
| Crons SF-JU-01-02/03 | À auditer — si même pattern de tx globale, on étend ; sinon, hors scope | Décision documentée en notes |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — pas de parcours utilisateur visible touché.

---

## Dépendances

### Subfeatures bloquantes

- `SF-JU-01-08` — statut : done (mergée 2026-05-27).

### Questions ouvertes impactées

- [x] Aucune.

---

## Notes et décisions

- **Choix `TransactionTemplate` vs nouveau bean `@Transactional(REQUIRES_NEW)`** : on retient `TransactionTemplate` car (a) plus simple (pas de nouveau bean dédié), (b) pas de problème de self-injection / proxy Spring, (c) permet un `try/catch` granulaire autour de chaque entrée sans avoir à wrapper toute la méthode.
- **Idempotence** : volontairement hors scope V1. Le bootstrap est un geste admin maîtrisé, l'admin sait quel batch il relance. Si on observe en pratique des doublons gênants, on ouvrira une SF dédiée.
- **Audit des crons** : à faire au moment du dev. Si les crons SF-JU-01-02/03 utilisent eux aussi un `@Transactional` global problématique, on étend l'approche dans cette SF (puisque c'est la même cible architecturale). Sinon, justification explicite dans le commit/PR.
