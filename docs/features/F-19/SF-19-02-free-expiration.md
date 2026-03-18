# Mini-spec — SF-19-02 — Plan FREE trial : expiration et gates lecture seule

## Identifiant
`SF-19-02`

## Feature parente
`F-19` — Intégration paiement Stripe

## Statut
`done`

## Date de création
2026-03-18

## Branche Git
`feat/SF-19-02-free-expiration`

---

## Objectif
Quand un workspace FREE a dépassé son `expires_at`, toute action d'écriture retourne 402. Les données existantes restent accessibles en lecture.

---

## Critères d'acceptation
- [x] FREE expiré → POST /api/v1/case-files → 402
- [x] FREE expiré → upload document → 402
- [x] FREE expiré → re-analyse → 402
- [x] FREE actif → actions autorisées dans quota
- [x] STARTER/PRO → expires_at sans effet
- [x] GET non bloqué même si FREE expiré

---

## Technique

### Classes modifiées
- `PlanLimitService` — ajout `isExpiredFree(Subscription)`, intégré dans les 3 méthodes ForWorkspace
  - Expired FREE → maxOpenCaseFiles = 0, maxDocuments = 0, enrichedAllowed = false

### Tests (108/108 verts)
- `PlanLimitServiceTest` — 14 tests dont 6 nouveaux sur expiration FREE
