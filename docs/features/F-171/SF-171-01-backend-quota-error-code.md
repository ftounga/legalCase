# Mini-spec — F-171 / SF-171-01 — Backend : code machine-readable dans les réponses 402

## Identifiant

`F-171 / SF-171-01`

## Feature parente

`F-171` — Visibilité erreur quota / budget tokens — feedback différencié + CTA upgrade

## Statut

`draft`

## Date de création

2026-04-29

## Branche Git

`feat/SF-171-01-backend-quota-error-code`

---

## Objectif

Ajouter un champ `code` machine-readable dans le body JSON des réponses HTTP **402 PAYMENT_REQUIRED** afin que le frontend puisse router le rendu (bandeau, message, CTA) selon le motif réel sans parser une chaîne libre.

---

## Comportement attendu

### Cas nominal

Quand un service backend lève une exception parce qu'un quota est atteint, la réponse HTTP 402 contient désormais un body :

```json
{
  "error": "402 PAYMENT_REQUIRED",
  "message": "Budget tokens mensuel dépassé.",
  "code": "TOKEN_BUDGET_EXCEEDED"
}
```

Le `code` est l'une des 7 valeurs de l'enum `PaymentRequiredCode` (voir Contrat API). Le `message` reste celui que le service a fourni (français, lisible). Le code permet au frontend de mapper sans ambiguïté motif → message UI + CTA.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Quota tokens dépassé | Body avec `code = TOKEN_BUDGET_EXCEEDED` | 402 |
| Limite analyses/dossier atteinte | Body avec `code = CASE_ANALYSIS_LIMIT_EXCEEDED` | 402 |
| Limite messages chat | Body avec `code = CHAT_MESSAGE_LIMIT_EXCEEDED` | 402 |
| Limite documents | Body avec `code = DOCUMENT_LIMIT_EXCEEDED` | 402 |
| Limite dossiers actifs | Body avec `code = CASE_FILE_OPEN_LIMIT_EXCEEDED` | 402 |
| Pack OCR épuisé | Body avec `code = OCR_QUOTA_EXCEEDED` | 402 |
| Limite sièges (TEAM/PRO) | Body avec `code = SEAT_LIMIT_EXCEEDED` | 402 |
| Exception 402 sans code (legacy) | Body **sans champ `code`** (frontend tombe sur fallback générique) | 402 |
| Autres status (4xx/5xx) | Body inchangé (rétro-compat) | inchangé |

---

## Contrat API (figé pour SF-171-02 frontend)

### Body 402 enrichi

```json
{
  "error": "402 PAYMENT_REQUIRED",
  "message": "<message lisible français>",
  "code": "<PaymentRequiredCode | absent>"
}
```

### Enum `PaymentRequiredCode`

| Code | Service source | Message canonique |
|---|---|---|
| `TOKEN_BUDGET_EXCEEDED` | `CaseAnalysisCommandService`, `ReAnalysisCommandService`, `ChatService` | "Budget tokens mensuel dépassé." |
| `CASE_ANALYSIS_LIMIT_EXCEEDED` | `CaseAnalysisCommandService` | "Limite d'analyses atteinte pour ce dossier." |
| `CHAT_MESSAGE_LIMIT_EXCEEDED` | `ChatService` | "Limite de messages chat atteinte." |
| `DOCUMENT_LIMIT_EXCEEDED` | `DocumentService` | "Limite de documents atteinte pour votre plan." |
| `CASE_FILE_OPEN_LIMIT_EXCEEDED` | `CaseFileStatusService` | "Limite de dossiers actifs atteinte. Passez à un plan supérieur." |
| `OCR_QUOTA_EXCEEDED` | `OcrService` | "Quota OCR mensuel épuisé." (à confirmer en lecture du service) |
| `SEAT_LIMIT_EXCEEDED` | `WorkspaceInvitationService`, `StripeSeatService` | "Limite de sièges atteinte pour votre plan." |

---

## Analyse de cohérence transversale

### Périmètres scannés

- **Autres pays** : non applicable — les gates `PlanLimitService` sont indifférents au pays.
- **Autres domaines** : non applicable — infrastructure transversale.
- **Autres flows** : tout endpoint protégé par `PlanLimitService` est dans le périmètre. Scan exhaustif effectué (8 sources `HttpStatus.PAYMENT_REQUIRED` + 1 site OCR à confirmer).

### Cas spécifique : nouveau pattern UI ou service partagé

Cette SF introduit une convention `code` dans les bodies 402. Pattern réutilisable au-delà du quota :
- **Pourrait s'étendre aux 401/403/422** plus tard (codes machine-readable génériques pour tous les statuts d'erreur métier). Hors scope de cette SF, **noté pour futur backlog éventuel** si le pattern fait ses preuves.
- **Pas de pattern concurrent** existant — `GlobalExceptionHandler` est aujourd'hui le point unique pour les `ResponseStatusException` ; on enrichit, on ne duplique pas.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `CaseAnalysisCommandService` | Oui | Intégré dans cette SF (2 sites : tokens + analyses/dossier) |
| `ReAnalysisCommandService` | Oui (à vérifier en lecture) | Intégré dans cette SF |
| `ChatService` | Oui | Intégré dans cette SF |
| `DocumentService` | Oui | Intégré dans cette SF |
| `CaseFileStatusService` | Oui | Intégré dans cette SF |
| `OcrService` | Oui | Intégré dans cette SF |
| `WorkspaceInvitationService` | Oui | Intégré dans cette SF |
| `StripeSeatService` | Oui | Intégré dans cette SF |
| Codes 401/403/422 | Non (hors scope) | Backlog futur si ROI |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (8 sites de throw 402)
- [ ] Subfeature(s) parallèle(s) créée(s) pour les cibles restantes
- [x] Backlog futur : extension du pattern aux 401/403/422 si le ROI est démontré sur cette SF

---

## Impact par domaine métier

**Transversale, infrastructure — aucune adaptation par domaine ni par pays.** Les gates `PlanLimitService` sont structurels (tokens, analyses, documents, sièges, OCR), indépendants du droit du travail / immigration / famille et de la France / Belgique.

---

## Critères d'acceptation

- [ ] **C1** — Body 402 contient le champ `code` quand l'exception est levée via le nouveau mécanisme `PaymentRequiredException(code, message)` ou équivalent
- [ ] **C2** — Les 8 sites de throw 402 listés ci-dessus utilisent le nouveau mécanisme et associent le bon code
- [ ] **C3** — Le `message` français reste inchangé pour chaque site (rétrocompat — les handlers frontend qui lisent encore `error.error.message` continuent de fonctionner)
- [ ] **C4** — Une exception `ResponseStatusException` 402 levée sans code (cas legacy) renvoie un body **sans** le champ `code` (pas de null explicite, juste absent) — le frontend tombe sur fallback
- [ ] **C5** — Aucun changement sur les autres statuts (200, 400, 403, 404, 409, 422, 500) — vérifié par tests existants
- [ ] **C6** — `GlobalExceptionHandler` ne casse pas la rétrocompat des consommateurs externes (ex : tests E2E qui parsent le body)
- [ ] **C7** — Tests d'intégration : 1 IT par site qui vérifie le `code` dans le body 402

---

## Périmètre

### Hors scope

- Modification du frontend (couvert par SF-171-02)
- Extension de la convention `code` aux 401/403/422 (backlog futur)
- Modification des messages français existants (les services gardent leurs reasons actuels)
- Notifications email proactives "vous avez atteint 80 % du quota"

---

## Technique

### Approche d'implémentation

**Option retenue** : créer une exception métier dédiée `PaymentRequiredException` qui porte le `code` + `message`, et l'attraper dans `GlobalExceptionHandler` AVANT le handler générique `ResponseStatusException`. Avantages : (a) code typé via enum, (b) refactor progressif possible (un site à la fois — ceux qui restent en `ResponseStatusException(402, ...)` continuent de fonctionner sans `code`), (c) pas de breaking change sur l'existant.

```java
public class PaymentRequiredException extends RuntimeException {
    private final PaymentRequiredCode code;
    public PaymentRequiredException(PaymentRequiredCode code, String message) { ... }
}

public enum PaymentRequiredCode {
    TOKEN_BUDGET_EXCEEDED,
    CASE_ANALYSIS_LIMIT_EXCEEDED,
    CHAT_MESSAGE_LIMIT_EXCEEDED,
    DOCUMENT_LIMIT_EXCEEDED,
    CASE_FILE_OPEN_LIMIT_EXCEEDED,
    OCR_QUOTA_EXCEEDED,
    SEAT_LIMIT_EXCEEDED
}
```

`GlobalExceptionHandler` ajoute :
```java
@ExceptionHandler(PaymentRequiredException.class)
public ResponseEntity<Map<String, String>> handlePaymentRequired(PaymentRequiredException ex, HttpServletRequest req) {
    log.warn("...");
    return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
        .body(Map.of(
            "error", "402 PAYMENT_REQUIRED",
            "message", ex.getMessage(),
            "code", ex.getCode().name()
        ));
}
```

### Endpoints impactés

Tous les endpoints qui passent par les 8 services listés. Aucun nouvel endpoint, aucun changement de signature.

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

---

## Plan de test

### Tests unitaires

- [ ] `PaymentRequiredException` — getter `code` retourne l'enum fourni
- [ ] `GlobalExceptionHandlerTest` — handler `PaymentRequiredException` retourne 402 avec `code` dans le body
- [ ] `GlobalExceptionHandlerTest` — handler `ResponseStatusException` 402 (legacy, sans code) retourne 402 **sans** champ `code`

### Tests d'intégration

- [ ] `CaseAnalysisCommandServiceIT` — 402 sur tokens dépassés → body contient `"code": "TOKEN_BUDGET_EXCEEDED"`
- [ ] `CaseAnalysisCommandServiceIT` — 402 sur limite analyses → body contient `"code": "CASE_ANALYSIS_LIMIT_EXCEEDED"`
- [ ] `ChatServiceIT` — 402 → body contient `"code": "CHAT_MESSAGE_LIMIT_EXCEEDED"` (ou `TOKEN_BUDGET_EXCEEDED` selon le path)
- [ ] `DocumentServiceIT` — 402 → body contient `"code": "DOCUMENT_LIMIT_EXCEEDED"`
- [ ] `CaseFileStatusServiceIT` — 402 → body contient `"code": "CASE_FILE_OPEN_LIMIT_EXCEEDED"`
- [ ] `WorkspaceInvitationServiceIT` — 402 → body contient `"code": "SEAT_LIMIT_EXCEEDED"`
- [ ] (si pertinent) `OcrServiceIT` — 402 → body contient `"code": "OCR_QUOTA_EXCEEDED"`

### Isolation workspace

Non applicable — la SF ne touche pas l'accès aux données, seulement le format du body de réponse.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Plans / limites** — touche les sites qui lèvent 402 via `PlanLimitService`
- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Navigation / routing frontend

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|---|---|---|
| Tous les endpoints derrière `PlanLimitService` (8 services) | Body 402 enrichi d'un champ `code` ; rétrocompat préservée si `code` absent | IT par service |
| Frontend `payment-required.interceptor.ts` | Continuera de fonctionner sur `error.error.message` ; SF-171-02 le refactorisera pour lire `code` | Test Jest existant + nouveau test |
| Tests E2E qui parsent les bodies 402 | Doivent continuer à matcher sur `message` ; éventuel ajout d'assertion sur `code` | Smoke tests E2E |

### Smoke tests E2E

- [x] `e2e/smoke/auth.spec.ts` — non concerné (200/302) — vérification de non-régression
- [x] Aucun smoke test ne fait actuellement assertion sur le body 402 (à confirmer en lecture)

---

## Dépendances

### Subfeatures bloquantes

Aucune — SF-171-01 démarrable immédiatement.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **Décision** : utiliser une exception dédiée plutôt qu'enrichir `ResponseStatusException`. Raison : permet le refactor progressif site par site, et l'enum garantit la cohérence des codes.
- **Décision** : ne PAS modifier les messages français existants. Ils sont rétrocompat-friendly et la SF-171-02 frontend pourra les overrider via mapping `code → message UI` si besoin.
- **Note** : la convention `code` ne sera étendue aux 401/403/422 qu'après validation du ROI sur cette SF.
