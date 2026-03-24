# Mini-spec — F-47 / SF-47-01 Sentry backend (Spring Boot)

---

## Identifiant

`F-47 / SF-47-01`

## Feature parente

`F-47` — Monitoring & alertes applicatives

## Statut

`ready`

## Date de création

2026-03-25

## Branche Git

`feat/SF-47-01-sentry-backend`

---

## Objectif

Intégrer le SDK Sentry dans le backend Spring Boot pour capturer automatiquement toutes les exceptions non gérées en production, et envoyer manuellement un événement Sentry pour chaque job d'analyse IA terminé en statut FAILED.

---

## Comportement attendu

### Cas nominal

1. Le backend démarre avec la variable d'environnement `SENTRY_DSN` configurée.
2. Toute exception non gérée levée dans un controller ou un service est automatiquement capturée et envoyée à Sentry avec le contexte HTTP (URL, méthode, user-agent).
3. Quand un `AnalysisJob` passe au statut `FAILED` (dans `CaseAnalysisService` ou `EnrichedAnalysisService`), un événement Sentry est envoyé manuellement avec :
   - le `caseFileId`
   - le `jobType`
   - l'`errorMessage` du job
4. L'utilisateur connecté est ajouté au contexte Sentry (email, id) si disponible.
5. En profil `dev` et `local`, si `SENTRY_DSN` est absent, Sentry est désactivé (pas d'erreur au démarrage).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `SENTRY_DSN` absent ou vide | Sentry désactivé silencieusement, application démarre normalement |
| Réseau indisponible vers sentry.io | Fail-open — l'exception originale n'est pas impactée, l'envoi échoue silencieusement |
| `SentryHub` non initialisé | Pas d'exception propagée — vérification `Sentry.isEnabled()` avant tout appel manuel |

---

## Critères d'acceptation

- [ ] La dépendance `sentry-spring-boot-starter` est ajoutée dans `pom.xml`
- [ ] `SENTRY_DSN` est lu depuis une variable d'environnement (pas hardcodé)
- [ ] En profil `dev`/`local` sans `SENTRY_DSN`, le démarrage réussit sans erreur
- [ ] Une exception non gérée dans un controller est capturée par Sentry (vérifiable via `Sentry.captureException()` dans un test unitaire ou l'interface Sentry)
- [ ] Quand un `AnalysisJob` est FAILED, `Sentry.captureEvent()` est appelé avec `caseFileId`, `jobType`, `errorMessage`
- [ ] L'appel Sentry est fail-open (une exception dans l'envoi Sentry n'interrompt pas le flux applicatif)
- [ ] Le `environment` Sentry est configuré (`dev`, `local`, `staging`, `production`) via variable d'environnement ou profil Spring

---

## Périmètre

### Hors scope (explicite)

- Intégration frontend Angular (SF-47-02)
- Alertes Sentry configurées dans l'interface sentry.io (configuration manuelle post-deploy)
- Traces de performance / profiling Sentry (non requis V1)
- Breadcrumbs personnalisés autres que les jobs IA

---

## Technique

### Dépendance Maven

```xml
<dependency>
    <groupId>io.sentry</groupId>
    <artifactId>sentry-spring-boot-starter-jakarta</artifactId>
    <version>7.x.x</version>
</dependency>
```

### Configuration `application.yml`

```yaml
sentry:
  dsn: ${SENTRY_DSN:}          # vide = désactivé
  environment: ${SENTRY_ENV:dev}
  traces-sample-rate: 0        # pas de perf tracing en V1
```

### Classes impactées

| Classe | Modification |
|--------|-------------|
| `pom.xml` | Ajout dépendance `sentry-spring-boot-starter-jakarta` |
| `application.yml` | Section `sentry:` avec DSN et environment |
| `CaseAnalysisService` | `Sentry.captureEvent()` dans le bloc FAILED |
| `EnrichedAnalysisService` | `Sentry.captureEvent()` dans le bloc FAILED |

### Migration Liquibase

- [x] Non applicable — aucune table modifiée

---

## Plan de test

### Tests unitaires

- [ ] `CaseAnalysisService` — job FAILED → `Sentry.captureEvent()` appelé avec les bons tags
- [ ] `CaseAnalysisService` — job DONE → `Sentry.captureEvent()` non appelé
- [ ] `EnrichedAnalysisService` — job FAILED → `Sentry.captureEvent()` appelé avec les bons tags
- [ ] Fail-open : exception dans l'envoi Sentry → flux non interrompu

### Tests d'intégration

- [ ] L'application démarre sans erreur avec `SENTRY_DSN` vide (profil dev)
- [ ] L'application démarre sans erreur avec `SENTRY_DSN` renseigné (mock DSN invalide accepté)

### Isolation workspace

- [ ] Non applicable — Sentry est un service tiers, pas d'isolation workspace à vérifier

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — ajout de SDK tiers et d'appels fail-open dans des blocs FAILED existants

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — la modification est additive (fail-open) et ne change pas les flux existants

---

## Dépendances

### Subfeatures bloquantes

- Aucune

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- **SDK choisi** : `sentry-spring-boot-starter-jakarta` (Jakarta EE, compatible Spring Boot 3.x)
- **DSN** : stocké dans `SENTRY_DSN` env var, cohérent avec le pattern secrets du projet (cf. `STRIPE_SECRET_KEY`)
- **Fail-open** : tout appel Sentry est encapsulé dans un try/catch ou conditionné par `Sentry.isEnabled()`
- **Environment** : `SENTRY_ENV=production` dans K8s production, `SENTRY_ENV=staging` en staging — permet de filtrer dans l'interface Sentry
- **Compte Sentry** : à créer sur sentry.io (free tier) — DSN fourni par l'utilisateur avant le dev
