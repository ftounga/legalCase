# Mini-spec — F-122 / SF-122-12 Tracking OCR dans workspace-admin

## Identifiant
`F-122 / SF-122-12`

## Feature parente
`F-122` — OCR pour PDF scannés

## Statut `draft`  · Date `2026-04-19`  · Branche `feat/SF-122-12-admin-ocr-tracking`

---

## Objectif

Afficher dans la page d'administration du workspace (`/workspace/admin`) une section "Consommation OCR ce mois" parallèle à l'existante "Consommation tokens ce mois" : progress bar, pourcentage consommé, alerte à 80 %, avec mention du reliquat de packs OCR achetés.

---

## Comportement

### Backend

`WorkspaceUsageSummaryResponse` étendu avec 3 champs :
- `long ocrPagesUsed` — pages consommées ce mois (`PlanLimitService.effectiveMonthlyUsage(ws, today)`)
- `long ocrMonthlyBudget` — quota mensuel du plan (`getMonthlyOcrPages(planCode)`)
- `long ocrPacksRemaining` — pages restantes depuis packs achetés (`computeOcrPacksRemaining(...)`)

`AdminUsageService.getWorkspaceSummary` : récupère `Subscription.planCode`, calcule les 3 valeurs, les ajoute à la réponse.

Le calcul du pourcentage se fait côté frontend (`used / (budget + packs) * 100`).

### Frontend

Modèle `WorkspaceUsageSummary` étendu avec `ocrPagesUsed, ocrMonthlyBudget, ocrPacksRemaining`.

`workspace-admin.component.ts` : getter `monthlyOcrProgressPercent` qui calcule `used / (budget + packs) × 100`, clampé à 0-100. Getter `monthlyOcrProgressColor` qui renvoie `warn` si ≥80 %, `accent` si ≥60 %, `primary` sinon (même règle que tokens).

`workspace-admin.component.html` : nouvelle `mat-card` après la section tokens avec :
- Titre "Consommation OCR ce mois" + icône `document_scanner`
- Ligne "X / Y pages" avec mention "+ Z pages depuis packs" si `ocrPacksRemaining > 0`
- Progress bar même style que tokens
- Alerte à 80 % "Quota OCR proche ou atteint — les analyses peuvent basculer en OCR payant"

### Cas d'erreur

- Workspace sans subscription → planCode fallback "FREE" (même logique que `OcrRetryService`)
- `ocrMonthlyBudget = 0` → affichage "Aucun quota OCR" (ne devrait pas arriver, tous les plans ont un quota)

---

## Critères d'acceptation

- [ ] `GET /api/admin/workspace-usage` renvoie `ocrPagesUsed, ocrMonthlyBudget, ocrPacksRemaining` avec les bonnes valeurs
- [ ] La page `/workspace/admin` affiche la section "Consommation OCR ce mois" en dessous de la section tokens
- [ ] Progress bar OCR fonctionne : couleur primary < 60 %, accent 60-80 %, warn ≥80 %
- [ ] Alerte warning à ≥80 %
- [ ] Si `ocrPacksRemaining > 0` : mention "+ N pages depuis packs"
- [ ] Aucune régression sur la section tokens

---

## Plan de test

### Unitaires backend
- `AdminUsageServiceTest` : nouveau test — workspace SOLO avec 200 pages consommées → response contient `ocrPagesUsed=200, ocrMonthlyBudget=800, ocrPacksRemaining=0`
- Nouveau test : workspace avec pack OCR_500 acheté et 0 page consommée → `ocrPacksRemaining` ≈ 500

### Unitaires frontend
- `workspace-admin.component.spec.ts` : nouveau test — `monthlyOcrProgressPercent` = 25 quand used=200, budget=800, packs=0
- Nouveau test : couleur warn quand percent ≥80

### Isolation workspace
- Vérifiée par l'endpoint existant (`workspaceMemberRepository.findByUserAndPrimaryTrue`)

---

## Tables / endpoints / composants impactés

### Backend
- `WorkspaceUsageSummaryResponse.java` — 3 nouveaux champs
- `AdminUsageService.java` — calcul et injection des 3 champs
- `AdminUsageServiceTest.java` — 2 nouveaux tests

### Frontend
- `workspace-usage-summary.model.ts` — 3 nouveaux champs
- `workspace-admin.component.ts` — 2 nouveaux getters
- `workspace-admin.component.html` — nouvelle `mat-card`
- `workspace-admin.component.spec.ts` — 2 nouveaux tests

### Config / Migration
- Aucune — tous les champs workspace existent déjà (`ocr_pages_used_current_month`, `ocr_pages_used_all_time`)

---

## Hors périmètre

- Bouton "Acheter pack OCR" depuis la section tracking (redirection vers billing suffit via lien existant)
- Graphique d'évolution OCR sur les 30 derniers jours (V2)
- Notification email quand quota OCR proche (V2, même logique que tokens)

---

## Analyse de cohérence transversale

| Cible | Applicable | Classement |
|---|---|---|
| Autres pays (Belgique) | Oui | **Intégrée** — même endpoint, même logique workspace-scope |
| Autres domaines | Oui | **Intégrée** — workspace-scope |
| Super-admin usage | **Backlog** — le super-admin a son propre endpoint usage qui pourrait aussi afficher l'OCR par workspace |
| Autres pages consommation | Non applicable | workspace-admin est l'unique page consommation workspace-scope |

**Analyse d'impact cross-cutting** :
- [ ] Auth / Principal — non touché (endpoint existant réutilisé)
- [ ] Workspace context — non touché
- [ ] Plans / limites — lecture uniquement, pas de gate modifiée
- [ ] Navigation / routing — non touché

Aucun smoke E2E concerné.

---

## Nouveau pattern UI ou service partagé

- [x] Pas de nouveau pattern — duplication consciente de la section tokens pour OCR (3-4 lignes HTML + 2 getters). Pas de composant partagé car la répétition est locale et limitée. Si une 3e ressource était ajoutée (ex. documents OCR historisés), refactor en composant "ResourceMeter" à considérer (→ backlog).
