# Prérequis avant SF-120-03 — Garde-fou coût F-120

> **Statut : ✅ ACTIVÉ — 2026-05-03**
> Billing OpenAI (org `ng-itconsulting`) et Anthropic confirmés actifs par appels test (HTTP 200).
> Garde-fou principal côté code via `IaCostTracker` app-side (cf. décision 2026-05-03 ci-dessous).
> Alertes email providers à finaliser de manière best-effort (non bloquant).

---

## Décision 2026-05-03 — Bascule du garde-fou vers le code app-side

OpenAI ne permet plus d'activer un hard limit sur les comptes monthly billing standard, et l'option "crédits prépayés + auto-recharge OFF" n'est pas accessible non plus dans la configuration actuelle du compte. Conclusion : **le garde-fou coût ne peut plus reposer sur les providers**.

Bascule actée :

1. Le garde-fou strict est implémenté **côté application** via un service `IaCostTracker` introduit dans SF-120-03 et étendu dans SF-120-04.
2. Côté providers, on garde uniquement les **alertes email** comme filet best-effort (visibilité, pas blocage).
3. SF-120-04 ajoute une stratégie **retry borné + fallback "article sans hero"** spécifique à DALL-E pour absorber les échecs sans replanifier.

---

## Action 1 — Activer les alertes email (best-effort, plus de hard limit côté provider)

### OpenAI

1. Se connecter sur platform.openai.com
2. **Settings** → **Limits** (ou **Usage limits**)
3. **Soft limit** = 15 USD (email d'alerte) — toujours configurable même sans hard limit
4. Vérifier que l'email du compte est correct (alerte y arrive)

### Anthropic

1. Se connecter sur console.anthropic.com
2. **Plans & billing** → **Workspace settings** → **Spend alerts**
3. **Spend alert** = 33 USD (≈ 30 €)
4. Si l'UI expose un **Hard spend limit** : configurer 50 USD comme safety net

### Vérification

- [ ] Alerte OpenAI 15 USD configurée
- [ ] Alerte Anthropic 33 USD configurée
- [ ] (Bonus) Hard limit Anthropic 50 USD si UI disponible
- [ ] Date d'activation : ____

---

## Action 2 — Garde-fou strict app-side (implémenté en SF-120-03)

Le garde-fou réel est dans le code, sur la table `usage_events` (existante).

### Service `IaCostTracker` (SF-120-03)

- Avant tout appel Claude/DALL-E, lit le coût cumulé du mois civil en cours via `UsageEventRepository`.
- Si `monthlyCost(provider) >= cap(provider)` → l'appel est **bloqué** (exception métier dédiée), un événement super-admin est loggé, l'article est laissé en `PENDING` (sera retenté le mois suivant).
- Caps configurables via `application.yml` (nominal + alerte) :

| Provider | Cap blocage | Alerte email super-admin |
|----------|-------------|--------------------------|
| Anthropic (Claude Sonnet + Haiku) | 30 € / mois | 15 € / mois |
| OpenAI (DALL-E 3) | 20 € / mois | 10 € / mois |

- Atomicité : enregistrement du `UsageEvent` dans la même transaction que le check (évite course entre 2 articles parallèles qui passent ensemble au-dessus du cap).

### Justification du choix d'architecture

- **Source de vérité unique** : la table `usage_events` est déjà utilisée pour les quotas plan (F-16). Pas de nouveau mécanisme à maintenir.
- **Cap par provider** : permet de plafonner indépendamment le risque Anthropic et OpenAI (un bug DALL-E ne consomme pas le budget Claude).
- **Stop sans replan** : un article bloqué par le cap reste `PENDING` (pas de boucle de retry).

---

## Action 3 — Retry + fallback DALL-E (implémenté en SF-120-04)

Voir mini-spec [`SF-120-04-image-hero-dall-e.md`](SF-120-04-image-hero-dall-e.md).

Résumé :

- 2 retries max sur erreurs transitoires uniquement (`429`, `5xx`, network).
- Aucun retry sur erreurs définitives (`400`, `content_policy_violation`, `insufficient_quota`).
- Si l'image échoue après retries → l'article est **publié sans hero**, événement super-admin `BLOG_HERO_IMAGE_FAILED` loggé, l'avocat peut uploader manuellement *a posteriori*.

Coût plafonné par échec : 3 × 0,04 USD = **0,12 USD max**, négligeable.

---

## À faire au moment de reprendre F-120

1. Récupérer nouvelle carte bancaire et la rattacher au compte OpenAI + Anthropic.
2. Exécuter Action 1 (alertes email — sans blocage hard limit puisqu'indisponible).
3. Vérifier que les caps `IA_BUDGET_*` (`application.yml`) sont prêts à être chargés (ils seront introduits par SF-120-03).
4. Mettre à jour le **Statut** en haut de ce fichier : `⏳ EN ATTENTE` → `✅ ACTIVÉ`.
5. Démarrer SF-120-03 (mini-spec + dev) — qui inclut le `IaCostTracker`.
6. Mentionner dans la PR SF-120-03 : "Garde-fou coût F-120 activé via IaCostTracker app-side (PREREQUIS-AVANT-SF-120-03 v2)".

---

## Récapitulatif des garde-fous coût F-120

| # | Garde-fou | Mécanisme | Couvert par |
|---|---|---|---|
| 1 | Hard cap mensuel par provider | `IaCostTracker` app-side, table `usage_events` | **SF-120-03** (Claude) + **SF-120-04** (DALL-E) |
| 2 | Idempotence stricte (pas de re-traitement) | `compareAndSwap` statut topic | ✅ SF-120-02 |
| 3 | Circuit breaker max 3 articles/jour + 15/sem | `@Scheduled` quota | SF-120-05 |
| 4 | Rate-limit admin "Générer maintenant" 5/jour | Rate-limiter endpoint | SF-120-08 |
| 5 | Auth super-admin stricte sur endpoints d'écriture | Spring Security `@PreAuthorize` | SF-120-08 |
| 6 | Alerte email si coût projeté mois > 15 € au prorata | Cron `@Scheduled` quotidien | SF-120-05 |
| 7 | DALL-E uniquement si garde-fou IA actif | Vérification `IaCostTracker` au démarrage de SF-120-04 | SF-120-04 |
| 8 | Retry borné + fallback "no hero" sur DALL-E | `RetryableImageGenerator` | **SF-120-04** |

> **Note** : les garde-fous 1 et 8 sont la nouvelle ligne de défense après la décision du 2026-05-03 (perte du hard limit côté providers).
