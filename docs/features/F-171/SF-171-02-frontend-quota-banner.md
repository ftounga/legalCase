# Mini-spec — F-171 / SF-171-02 — Frontend : bandeau quota persistant + état disabled-quota

## Identifiant

`F-171 / SF-171-02`

## Feature parente

`F-171` — Visibilité erreur quota / budget tokens — feedback différencié + CTA upgrade

## Statut

`draft`

## Date de création

2026-04-30

## Branche Git

`feat/SF-171-02-frontend-quota-banner`

---

## Objectif

Remplacer les **snackbars qui se chassent** (intercepteur global 8 s + handler local 5 s) par un **bandeau inline persistant** sur les zones d'action concernées (analyse, re-analyse, chat, upload), affichant le motif exact issu du backend (`code` SF-171-01) avec CTA "Upgrader le plan", et passer les boutons concernés en état `disabled-quota` tant que le quota n'est pas réinitialisé.

---

## Comportement attendu

### Cas nominal

1. L'avocat clique "Analyser" sur un dossier dont le workspace a atteint son quota tokens mensuel.
2. Le backend renvoie `402 { code: "TOKEN_BUDGET_EXCEEDED", message: "Budget tokens mensuel dépassé." }`.
3. **L'intercepteur global ne déclenche plus de snackbar**. Il pousse `{ code, message, source: req.url }` dans un service partagé `QuotaErrorState`.
4. Le composant `<app-quota-error-banner>` (présent en haut du panel analyse via `case-file-detail`) écoute le state et **affiche un bandeau inline persistant** :
   - Icône d'alerte navy/or aligné DESIGN_SYSTEM.md
   - Titre : `"Quota mensuel atteint"` (mappé depuis `code`)
   - Message : message exact du backend (`error.error.message`)
   - Bouton primaire : `"Upgrader le plan"` → `/workspace/billing`
   - Bouton secondaire (optionnel) : `"Acheter des tokens"` (si `code = TOKEN_BUDGET_EXCEEDED` et flag F-148-tokens existe — gated)
5. **Le bouton "Analyser" passe en état `[disabled-quota]`** avec tooltip : `"Quota mensuel atteint — passez au plan supérieur"`. Tant que le quota state n'est pas effacé, l'avocat ne peut plus relancer.
6. Le state se vide automatiquement : (a) au refresh manuel de la page, (b) après navigation vers `/workspace/billing` puis retour avec succès Stripe (paramètre URL `?upgraded=success`), (c) au switch de workspace.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| 402 reçu sans champ `code` (legacy backend) | Bandeau générique : titre `"Limite atteinte"` + message brut du backend + CTA upgrade |
| 402 reçu avec `code` inconnu (nouveau code non encore mappé côté front) | Idem générique + `console.warn("Unknown quota code: X")` (fail-open) |
| Plusieurs 402 successifs | Le bandeau garde le **dernier** `code` reçu ; pas d'empilement |
| 402 sur un endpoint chat alors que la page analyse est ouverte | Le state est partagé : le bandeau s'affiche aussi sur la zone chat (composant intégré aux 4 zones) |
| Upgrade Stripe réussi → retour | Au load avec `?upgraded=success`, le state est vidé et le bandeau disparaît |

---

## Contrat API (importé de SF-171-01-backend)

Body 402 attendu :
```json
{
  "error": "402 PAYMENT_REQUIRED",
  "message": "Budget tokens mensuel dépassé.",
  "code": "TOKEN_BUDGET_EXCEEDED"
}
```

Codes possibles : `TOKEN_BUDGET_EXCEEDED`, `CASE_ANALYSIS_LIMIT_EXCEEDED`, `CHAT_MESSAGE_LIMIT_EXCEEDED`, `DOCUMENT_LIMIT_EXCEEDED`, `CASE_FILE_OPEN_LIMIT_EXCEEDED`, `OCR_QUOTA_EXCEEDED`, `SEAT_LIMIT_EXCEEDED`, ou absent (fallback générique).

Le contrat est **figé dans la mini-spec SF-171-01-backend**. Cette SF-171-02 développable en parallèle avec mock côté tests Jest.

---

## Mapping code → titre UI + CTA (frontend)

| Code | Titre bandeau | Action primaire | Action secondaire |
|---|---|---|---|
| `TOKEN_BUDGET_EXCEEDED` | "Quota tokens mensuel atteint" | Upgrader le plan → `/workspace/billing` | (V8+) Acheter des tokens |
| `CASE_ANALYSIS_LIMIT_EXCEEDED` | "Limite d'analyses atteinte pour ce dossier" | Upgrader le plan → `/workspace/billing` | — |
| `CHAT_MESSAGE_LIMIT_EXCEEDED` | "Limite de messages chat atteinte" | Upgrader le plan → `/workspace/billing` | — |
| `DOCUMENT_LIMIT_EXCEEDED` | "Limite de documents atteinte" | Upgrader le plan → `/workspace/billing` | — |
| `CASE_FILE_OPEN_LIMIT_EXCEEDED` | "Limite de dossiers actifs atteinte" | Upgrader le plan → `/workspace/billing` | — |
| `OCR_QUOTA_EXCEEDED` | "Quota OCR mensuel atteint" | Upgrader le plan → `/workspace/billing` | (V8+) Pack OCR ponctuel |
| `SEAT_LIMIT_EXCEEDED` | "Limite de sièges atteinte" | Voir les plans → `/workspace/billing` | — |
| (absent / inconnu) | "Limite atteinte" | Voir les plans → `/workspace/billing` | — |

---

## Analyse de cohérence transversale

### Périmètres scannés

- **Pages frontend touchées** : 4 zones d'intégration du bandeau identifiées :
  - `case-file-detail` (analyse + re-analyse) — handler 402 actuel ligne 808 + 882
  - `synthesis` (chat panel) — 3 handlers 402 lignes 442 / 473 / 522
  - `case-file-create-dialog` ligne 44 — 402 swallowed silently aujourd'hui
  - Upload de documents — quota OCR + DOCUMENT_LIMIT
  - `workspace-members` ligne 106 — message custom 402 (sieges)
- **Patterns concurrents** identifiés à harmoniser ou à laisser :
  - `MatSnackBar` direct dans handlers locaux : **à supprimer** sur les zones intégrées au bandeau (rétrocompat possible via fallback)
  - Snackbar `paymentRequiredInterceptor` global : **refactorisé** en publication d'event, plus de snackbar direct
  - Composant `<app-feedback-banner>` ou similaire ? À scanner avant création — si existant, étendre plutôt que créer

### Cas spécifique : nouveau pattern UI ou service partagé

Cette SF crée :
1. Un composant partagé `<app-quota-error-banner>` dans `frontend/src/app/shared/quota-error-banner/`
2. Un service partagé `QuotaErrorState` dans `frontend/src/app/core/services/`

**Questions du template appliquées** :

- [x] **Où le pattern bandeau pourrait-il être réutilisé ?** Pour tous les blocages métier transverses (ex. sessions Stripe expirées, maintenances). Le composant prend `code` + `message` + `actions[]` → générique. **Décision** : on le nomme `<app-quota-error-banner>` mais l'API le rend extensible (pas de couplage dur au quota).
- [x] **Patterns concurrents** :
  - `MatSnackBar` direct = pattern legacy, à phaser (commencer par les 4 zones de cette SF)
  - `<app-quota-error-banner>` ne vise pas à remplacer toutes les notifications, juste les blocages **persistants** liés à un quota
- [x] **Le service `QuotaErrorState` peut-il servir à autre chose ?** Pas dans l'immédiat. Pattern signal Angular standard (signal `error: WritableSignal<QuotaError | null>`).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `case-file-detail` (analyse + re-analyse) | Oui | Intégré dans cette SF |
| `synthesis` (chat × 3) | Oui | Intégré dans cette SF |
| `case-file-create-dialog` | Partiel | Intégré dans cette SF (afficher bandeau au lieu de swallow) |
| Upload documents (`document-upload` ?) | Oui | Intégré dans cette SF |
| `workspace-members` | Partiel | Cette SF garde le message custom existant + propose le bandeau pour cohérence ; harmonisation non bloquante |
| Autres handlers 402 isolés | À scanner | Si trouvés, soit intégrés cette SF soit notés backlog |
| Migration vers code 401/403/422 banner | Non | Backlog futur si ROI |

### Décision

- [x] Étendu aux 4 zones principales (analyse, chat, upload, workspace-members)
- [x] `case-file-create-dialog` swallow → bandeau (intégré)
- [ ] Migration globale `MatSnackBar` → bandeau pour autres motifs : backlog si ROI
- [x] Pattern documenté pour réutilisation future (`<app-quota-error-banner>` pris extensible)

---

## Impact par domaine métier

**Transversale, infrastructure UI — aucune adaptation par domaine ni par pays.** Les zones intégrées (analyse, chat, upload, sièges) sont indépendantes du droit du travail / immigration / famille et de la France / Belgique.

---

## Critères d'acceptation

- [ ] **C1** — Composant `<app-quota-error-banner>` créé dans `shared/` avec inputs `code: string | null`, `message: string`, et template aligné DESIGN_SYSTEM.md (palette navy/or, icône alerte)
- [ ] **C2** — Service `QuotaErrorState` créé dans `core/services/` avec signal `error: WritableSignal<QuotaError | null>` et méthodes `set(err)`, `clear()`
- [ ] **C3** — `paymentRequiredInterceptor` refactorisé : ne déclenche **plus** de snackbar, pousse l'erreur dans `QuotaErrorState.set()` à la place
- [ ] **C4** — `case-file-detail.component.html` intègre `<app-quota-error-banner>` en haut du panel analyse ; les 2 handlers 402 (analyze ligne 808 + re-analyze ligne 882) sont **supprimés**
- [ ] **C5** — Bouton "Analyser" / "Re-analyser" passe en `[disabled]="quotaErrorState.error()" ` quand un quota error matche le contexte (TOKEN_BUDGET_EXCEEDED ou CASE_ANALYSIS_LIMIT_EXCEEDED)
- [ ] **C6** — `synthesis.component` intègre `<app-quota-error-banner>` ; les 3 handlers 402 (chat lignes 442/473/522) sont **simplifiés** (suppression du snackbar local)
- [ ] **C7** — Upload documents : intégration du bandeau dans la zone d'upload pour `DOCUMENT_LIMIT_EXCEEDED` et `OCR_QUOTA_EXCEEDED`
- [ ] **C8** — `case-file-create-dialog` ligne 44 : remplace le `return` silencieux par `quotaErrorState.set(err)`, le bandeau s'affiche dans le dialog
- [ ] **C9** — Le state se vide au switch de workspace (intégration avec `WorkspaceContextService` existant)
- [ ] **C10** — Le state se vide au paramètre URL `?upgraded=success` (à activer plus tard côté Stripe success URL)
- [ ] **C11** — Tests Jest : composant + service + intercepteur + intégration `case-file-detail`
- [ ] **C12** — Aucune régression sur les autres motifs d'erreur (5xx, 4xx ≠ 402) — les snackbars correspondants restent en place

---

## Périmètre

### Hors scope

- Backend (couvert par SF-171-01)
- Refonte des messages français côté backend (le frontend mappe `code → titre UI` mais lit toujours `message` brut comme fallback)
- Achats one-shot tokens / packs OCR — le bouton "Acheter des tokens" reste désactivé tant que F-148-tokens n'existe pas, mais la place visuelle est réservée
- Notifications email proactives "vous avez atteint 80 % du quota"
- Migration globale `MatSnackBar` → bandeau pour les motifs non-quota
- Analytics event `quota_blocked` — couvert par SF-171-03 si activée

### Reporté en SF-171-03 (optionnelle)

- Analytics event `quota_blocked` (signal commercial → relance upgrade) — peut être ajouté plus tard sans casser cette SF

---

## Technique

### Composants Angular

- **`<app-quota-error-banner>`** dans `frontend/src/app/shared/quota-error-banner/quota-error-banner.component.ts`
  - Inputs : `code: string | null`, `message: string`, optionnel `caseFileId?: string`
  - Outputs : `dismissed: EventEmitter<void>` (futur — pour bouton fermeture optionnel)
  - Template : bandeau aligné DESIGN_SYSTEM.md (navy/or, icône `error_outline` ou `lock`)

### Services

- **`QuotaErrorState`** dans `frontend/src/app/core/services/quota-error-state.service.ts`
  - Provider `providedIn: 'root'`
  - State : `error: WritableSignal<QuotaError | null>`
  - Méthodes : `set(err: QuotaError): void`, `clear(): void`, `matches(code: string): Signal<boolean>` (helper pour les boutons)
  - Type : `interface QuotaError { code: string | null; message: string; sourceUrl: string; receivedAt: number; }`

### Pattern de référence

- Pattern signal Angular standard (similaire à `CaseDashboardRefreshService` — `refresh$` Subject avec wrapper)
- Visuellement, s'inspirer du **bandeau "OQTF urgence 48h"** de F-IM-08 (composant `oqtf-sans-delai-section`) pour la palette critique mais en moins agressif (jaune/or pour quota vs rouge pour urgence)

### Endpoints

Aucun nouveau — consomme le contrat 402 figé par SF-171-01.

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

---

## Plan de test

### Tests unitaires (Jest)

- [ ] `quota-error-state.service.spec.ts` — `set`/`clear`/`matches` cas nominaux
- [ ] `quota-error-banner.component.spec.ts` — affichage selon `code` (mappings titre + CTA)
- [ ] `quota-error-banner.component.spec.ts` — fallback générique sur `code` absent ou inconnu
- [ ] `payment-required.interceptor.spec.ts` — pousse `QuotaErrorState.set()` au lieu d'ouvrir snackbar
- [ ] `case-file-detail.component.spec.ts` — bouton "Analyser" `disabled` quand `QuotaErrorState.error()` matche `TOKEN_BUDGET_EXCEEDED`
- [ ] `case-file-detail.component.spec.ts` — bandeau affiché en haut du panel analyse au déclenchement

### Tests d'intégration (mock backend)

- [ ] Mock 402 avec `code: TOKEN_BUDGET_EXCEEDED` → bandeau "Quota tokens mensuel atteint" + bouton désactivé
- [ ] Mock 402 sans `code` (legacy) → bandeau générique
- [ ] Switch workspace → state nettoyé, bandeau disparaît

### Isolation workspace

Non applicable — la SF ne touche pas l'accès aux données.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Plans / limites** — frontend des erreurs 402 (côté backend = SF-171-01)
- [x] **Workspace context** — le state se vide au switch workspace
- [x] **Navigation / routing frontend** — CTA route vers `/workspace/billing` (existant)
- [ ] Auth / Principal — non touché

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|---|---|---|
| `paymentRequiredInterceptor` | Refactor majeur (plus de snackbar direct) | Test Jest existant adapté |
| `case-file-detail.component` (handlers 402 ligne 808 + 882) | Suppression des snackbars locaux | Spec existant adapté |
| `synthesis.component` (3 handlers 402) | Simplification | Spec existant adapté |
| `case-file-create-dialog` | Affichage bandeau au lieu de swallow | Spec à compléter |
| `workspace-members.component` ligne 106 | Conserver le message custom existant ; bandeau optionnel | Pas de régression |

### Smoke tests E2E concernés

- [x] `e2e/smoke/auth.spec.ts` — non concerné (200/302) — non-régression
- [x] `e2e/smoke/navigation.spec.ts` — non concerné — non-régression
- [x] `e2e/smoke/workspace.spec.ts` — vérifier que le switch workspace ne crashe pas avec un quota error en state

---

## Dépendances

### Subfeatures bloquantes

- **SF-171-01 backend** — fournit le contrat `code` dans le body 402. La SF-171-02 frontend est développable en parallèle avec mock Jest. Intégration réelle après merge backend.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **Décision** : ne pas implémenter de bouton "Fermer" sur le bandeau dans cette SF. Le state se vide via switch workspace ou retour Stripe `?upgraded=success`. Si retour terrain demande une fermeture manuelle, ajouter en SF suivante.
- **Décision** : garder les snackbars existants pour les motifs ≠ 402 (5xx, 409, 422, etc.). On ne touche QUE les 402 pour limiter le blast radius.
- **Décision** : l'analytics event `quota_blocked` est sorti de cette SF (SF-171-03 optionnelle) pour rester focalisé sur l'UX.
- **Note** : la SF-171-02 doit être **mergée APRÈS SF-171-01** pour bénéficier du `code` réel. Tests Jest mock indépendants OK.
