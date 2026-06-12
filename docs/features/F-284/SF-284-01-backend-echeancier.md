# SF-284-01 — Backend : endpoint d'agrégation échéancier (lecture seule)

> Feature parente : **F-284 — Échéancier procédural proactif & alertes**

## Objectif (une phrase)
Exposer un endpoint **lecture seule** qui agrège, pour un dossier, les délais `case_deadlines`
(MANUAL + AI ACCEPTED + STATUTORY, hors AI PENDING) et les échéances de réponse des rounds
contradictoires (`contradictoire_rounds.response_due_at` non échues), priorisés par urgence,
afin d'alimenter l'échéancier proactif de l'onglet Suivi.

## Comportement nominal
- `GET /api/v1/case-files/{caseFileId}/echeancier` → 200 avec un `EcheancierResponse` :
  - `items` : liste triée par `dueDate` ascendant, chaque item =
    `{ id, label, dueDate, daysUntil, urgency, kind, source }`
    - `daysUntil` : entier (négatif si dépassé), calculé serveur (`LocalDate.now()`).
    - `urgency` : `OVERDUE` (<0) · `CRITICAL` (0–7) · `SOON` (8–15) · `UPCOMING` (>15).
    - `kind` : `DEADLINE` (issu de `case_deadlines`) · `CONTRADICTOIRE` (issu d'un round).
    - `source` : pour `DEADLINE` = MANUAL/AI/STATUTORY ; pour `CONTRADICTOIRE` = `CONTRADICTOIRE`.
  - `nextItem` : l'item le plus urgent (premier de la liste) ou `null` si vide.
  - `counts` : `{ overdue, critical, soon, upcoming, total }`.
- Délais inclus : `source=MANUAL` OU `source=STATUTORY` OU (`source=AI` ET `aiStatus=ACCEPTED`).
  Les AI `PENDING` sont **exclus** (pas encore confirmés par l'avocat).
- Rounds inclus : tout round dont `responseDueAt` est non nul. Label = `responseLabel(round)`
  (« Réponse — <label round> » ou « Réponse au round N »).

## Cas d'erreur
- Dossier inexistant ou hors workspace → **404** (réutilise `resolveCaseFileForUser`, même
  comportement que F-69).
- Aucune donnée → 200 avec `items=[]`, `nextItem=null`, `counts` tous à 0.
- Round avec `responseDueAt` null → ignoré (fail-open, pas d'exception).

## Critères d'acceptation vérifiables
1. AC1 : un dossier avec 1 délai MANUAL J-3 + 1 round réponse J-20 → `items` de taille 2, trié
   (J-3 puis J-20), `nextItem.daysUntil=3`, `urgency=CRITICAL`, `counts.critical=1`, `counts.upcoming=1`.
2. AC2 : un délai AI PENDING n'apparaît **pas** dans `items`.
3. AC3 : un délai dépassé (J+2) → `urgency=OVERDUE`, `daysUntil=-2`, `counts.overdue=1`.
4. AC4 : appel par un user d'un autre workspace → 404 (isolation workspace).
5. AC5 : dossier sans délai ni round → 200, `items=[]`, `nextItem=null`.

## Plan de test minimal
- **Unitaire** (`EcheancierServiceTest`) : tri/urgence/filtre AI PENDING/inclusion rounds, mapping
  `daysUntil`/`urgency`/`counts` (dates relatives à `LocalDate.now()`).
- **Intégration** (`EcheancierControllerIT`) : 200 nominal, 404 cross-workspace (AC4), 200 vide (AC5),
  exclusion AI PENDING (AC2). Réutilise le harnais d'auth OIDC des IT F-69.
- **Isolation workspace** : AC4 couvre explicitement le scope (même garde que `CaseDeadlineService`).

## Tables / endpoints / composants impactés
- **Tables** : aucune nouvelle table. Lecture seule sur `case_deadlines` + `contradictoire_rounds`.
  **Pas de migration Liquibase.**
- **Endpoint NOUVEAU** : `GET /api/v1/case-files/{caseFileId}/echeancier`.
- **Classes** : `EcheancierController`, `EcheancierService`, `EcheancierResponse` (record),
  `EcheancierItem` (record). Réutilise `CaseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc`,
  `ContradictoireRoundRepository.findByCaseFileIdOrderByRoundNumberAsc`, et le pattern
  `resolveUser`/`resolveCaseFileForUser` (copié de `CaseDeadlineService`).

## Contrat API figé (consommé par SF-284-02)
```
GET /api/v1/case-files/{caseFileId}/echeancier
200 →
{
  "items": [
    { "id": "uuid", "label": "string", "dueDate": "YYYY-MM-DD",
      "daysUntil": 3, "urgency": "OVERDUE|CRITICAL|SOON|UPCOMING",
      "kind": "DEADLINE|CONTRADICTOIRE",
      "source": "MANUAL|AI|STATUTORY|CONTRADICTOIRE" }
  ],
  "nextItem": { ...item } | null,
  "counts": { "overdue": 0, "critical": 1, "soon": 0, "upcoming": 1, "total": 2 }
}
404 → dossier introuvable / hors workspace
```

## Hors périmètre
- Aucune mutation (création/édition/suppression reste dans F-69 / F-282).
- Aucune nouvelle alerte mail (F-69 SF-69-03 inchangé).
- Aucune agrégation multi-dossiers (le dashboard couvre le niveau cabinet).

## Préoccupations transversales
- **Auth / Principal** : réutilise `CurrentUserResolver` + `OAuthProviderResolver` (aucun nouveau
  type d'auth). Composant impacté : `EcheancierService` (copie du pattern F-69).
- **Workspace context** : isolation via `resolveCaseFileForUser` (AC4). Composant : `EcheancierService`.
- **Outil décisionnel** : non — vue de lecture, pas un simulateur. Pas d'impact `decision_tool_*`.
