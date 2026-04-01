# Mini-spec — F-99 / SF-99-01 — Migration tests frontend Karma → Jest

## Identifiant
`F-99 / SF-99-01`

## Feature parente
`F-99` — Migration tests frontend Karma → Jest

## Statut
`ready`

## Date de création
2026-04-01

## Branche Git
`feat/SF-99-01-karma-to-jest`

---

## Objectif

Remplacer Karma + Chrome Headless par Jest dans le projet Angular frontend pour réduire le temps d'exécution des ~422 tests de ~2min à ~20s.

---

## Comportement attendu

### Cas nominal

1. `npm test` exécute Jest (pas Karma, pas Chrome).
2. Les 422 tests existants passent tous sans modification de leur logique métier.
3. Les APIs Jasmine (`jasmine.createSpyObj`, `jasmine.SpyObj`, `spyOn`) sont remplacées par leurs équivalents Jest (`jest.fn()`, type manuel, `jest.spyOn`).
4. Le temps d'exécution est inférieur à 30 secondes en local.
5. Le mode watch fonctionne via `npm run test:watch`.
6. La CI GitHub Actions utilise `npm test -- --ci` (pas de `--watch`, pas de navigateur).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Test utilisant une API Jasmine non migrée | Erreur explicite à la compilation ou à l'exécution Jest |
| Import `RouterTestingModule` (déprécié Angular 16+) | Conservé tel quel si déjà fonctionnel sous Jest |
| Test Angular Material avec animation | `NoopAnimationsModule` conservé, aucun changement requis |

---

## Critères d'acceptation

- [ ] `karma`, `karma-chrome-launcher`, `karma-coverage`, `karma-jasmine`, `karma-jasmine-html-reporter` supprimés des devDependencies
- [ ] `jest`, `jest-preset-angular`, `@types/jest` installés
- [ ] `jest.config.ts` créé à la racine de `frontend/`
- [ ] `tsconfig.spec.json` mis à jour : `types: ["jest"]` au lieu de `["jasmine"]`
- [ ] `angular.json` builder `test` mis à jour vers `@angular-devkit/build-angular:jest`
- [ ] `package.json` script `test` mis à jour ; script `test:watch` ajouté
- [ ] Tous les `jasmine.createSpyObj` remplacés par équivalents Jest
- [ ] Tous les `jasmine.SpyObj<T>` remplacés par le type Jest approprié
- [ ] `jasmine.Spy` remplacé par `jest.SpyInstance`
- [ ] `jasmine.stringContaining` remplacé par `expect.stringContaining`
- [ ] `jasmine.any` remplacé par `expect.any`
- [ ] `throwError(() => ...)` RxJS conservé (non Jasmine — inchangé)
- [ ] **422/422 tests passent sous Jest**
- [ ] CI `.github/workflows/frontend.yml` adaptée si nécessaire (suppression flag `--browsers=ChromeHeadless`)

---

## Périmètre

### Hors scope
- Réécriture des tests existants (logique inchangée)
- Migration vers Testing Library (`@testing-library/angular`)
- Couverture de code (coverage config Jest optionnelle, non bloquante)
- Tests E2E (Playwright — non concernés)

---

## Technique

### Fichiers créés
- `frontend/jest.config.ts`

### Fichiers modifiés
| Fichier | Modification |
|---------|-------------|
| `frontend/package.json` | Suppression deps Karma, ajout deps Jest, mise à jour scripts |
| `frontend/tsconfig.spec.json` | `types: ["jest"]` |
| `frontend/angular.json` | Builder `@angular-devkit/build-angular:jest` |
| `frontend/.github/workflows/frontend.yml` (si applicable) | Supprimer `--browsers=ChromeHeadless` |
| 58 fichiers `*.spec.ts` | Migration APIs Jasmine → Jest |

### Config Jest cible (`jest.config.ts`)
```typescript
import type { Config } from 'jest';

const config: Config = {
  preset: 'jest-preset-angular',
  setupFilesAfterFramework: ['<rootDir>/setup-jest.ts'],
  testPathPattern: ['src/.*\\.spec\\.ts$'],
  collectCoverageFrom: ['src/**/*.ts', '!src/**/*.spec.ts'],
};

export default config;
```

### Équivalences API Jasmine → Jest

| Jasmine | Jest |
|---------|------|
| `jasmine.createSpyObj('Svc', ['m1','m2'])` | `{ m1: jest.fn(), m2: jest.fn() }` |
| `jasmine.SpyObj<T>` | `jest.Mocked<T>` |
| `jasmine.Spy` | `jest.SpyInstance` |
| `spy.and.returnValue(x)` | `spy.mockReturnValue(x)` |
| `spy.and.returnValues(a, b)` | `spy.mockReturnValueOnce(a).mockReturnValueOnce(b)` |
| `spy.calls.count()` | `spy.mock.calls.length` |
| `spy.calls.mostRecent().args` | `spy.mock.calls.at(-1)` |
| `jasmine.stringContaining('x')` | `expect.stringContaining('x')` |
| `jasmine.any(Type)` | `expect.any(Type)` |
| `jasmine.objectContaining({})` | `expect.objectContaining({})` |
| `spyOn(obj, 'method')` | `jest.spyOn(obj, 'method')` |

---

## Plan de test

### Validation
- [ ] `npm test -- --ci` → 422/422 SUCCESS, exit code 0
- [ ] Temps d'exécution < 30s
- [ ] `npm run test:watch` démarre sans erreur

### Isolation workspace
- Non applicable — migration technique pure

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Aucune préoccupation transversale** — migration d'infrastructure de test uniquement, aucun code applicatif modifié

### Smoke tests E2E
- Aucun smoke test concerné (Playwright non impacté)

---

## Dépendances

### Subfeatures bloquantes
- Aucune

### Notes
- Angular 19 supporte nativement le builder Jest via `@angular-devkit/build-angular:jest` (stable depuis v19).
- `jest-preset-angular` v14+ requis pour compatibilité Angular 19 + Jest 29.
- `setup-jest.ts` à créer avec `import 'jest-preset-angular/setup-jest'`.
