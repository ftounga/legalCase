# Mini-spec — F-47 / SF-47-02 Sentry frontend (Angular)

## Identifiant
`F-47 / SF-47-02`

## Statut
`ready`

## Date de création
2026-03-25

## Objectif
Intégrer le SDK `@sentry/angular` dans le frontend Angular pour capturer automatiquement toutes les erreurs JavaScript non gérées en production.

## Comportement attendu
- `Sentry.init()` appelé dans `main.ts` avant `bootstrapApplication`, avec DSN via variable d'environnement Angular (`environment.ts`)
- `ErrorHandler` Angular remplacé par `Sentry.createErrorHandler()` dans `app.config.ts`
- Toute exception JS non gérée est capturée et envoyée à Sentry
- En `environment.ts` (dev local), `dsn: ''` → Sentry désactivé silencieusement
- En `environment.prod.ts`, `dsn` est renseigné

## Critères d'acceptation
- [ ] `@sentry/angular` installé dans `package.json`
- [ ] `Sentry.init()` dans `main.ts` avec DSN depuis `environment.sentryDsn`
- [ ] `ErrorHandler` remplacé par `Sentry.createErrorHandler()` dans `app.config.ts`
- [ ] `environment.ts` a `sentryDsn: ''` (dev désactivé)
- [ ] `environment.prod.ts` a `sentryDsn` renseigné via variable de build CI/CD

## Hors scope
- TraceService / performance tracing (non requis V1)
- Source maps upload (post-deploy manuel)

## Plan de test
- Test manuel : vérification dans l'interface Sentry que les erreurs remontent (pas de test automatisé pour ErrorHandler Angular)
