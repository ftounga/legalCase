# Prérequis avant SF-120-03 — Garde-fou coût (1) à activer

> **Statut : ⏳ EN ATTENTE** (pause 2026-04-17 — en attente réception nouvelle carte bancaire).
> Ce document liste les configurations **provider** à activer **avant** tout développement de SF-120-03
> (générateur d'article Claude Sonnet 4.6 — premier consommateur d'IA du blog).

---

## Pourquoi ce garde-fou

F-120 (Blog SEO automatisé) enchaîne génération texte (Claude) + image (DALL-E) + self-review sur un rythme automatique.
Sans plafond strict côté provider, un bug dans le scheduler (SF-120-05) ou une boucle de retry pourrait
brûler plusieurs centaines d'euros en quelques minutes.

Coût nominal estimé : ~0,25 €/article → ~2 €/mois phase 1 (2/semaine) → ~1 €/mois phase 2 (1/semaine).
**Plafonds cibles** : 20 €/mois OpenAI, 30 €/mois Anthropic.

---

## Action 1 — OpenAI (plafond 20 € ≈ 22 USD)

### Option A (recommandée) — Crédits prépayés

1. Se connecter sur platform.openai.com
2. Barre latérale **Settings** → **Billing** (ou bouton compte en haut → **Manage account**)
3. Section **Credit balance** → bouton **Add to credit balance**
4. Déposer **22 USD** (≈ 20 €)
5. **DÉSACTIVER l'auto-recharge** (toggle sur OFF) — essentiel pour que ce soit un vrai hard limit

Quand le solde est à 0 → toutes les requêtes retournent `insufficient_quota` → coupe naturelle, zéro code à ajouter.

### Option B (si compte en monthly billing)

1. **Settings** → **Limits** (ou **Usage limits**)
2. **Hard limit** = 22 USD (bloque les appels au-delà)
3. **Soft limit** = 15 USD (email d'alerte)

### Vérification

- [ ] Solde visible sur le dashboard
- [ ] Screenshot pris et stocké dans ce dossier (`docs/features/F-120/openai-limit-setup-YYYY-MM-DD.png`) pour la PR SF-120-03
- [ ] Date d'activation : ____

---

## Action 2 — Anthropic (alerte 30 € ≈ 33 USD + hard 50 USD si disponible)

1. Se connecter sur console.anthropic.com
2. **Plans & billing** → **Workspace settings** → chercher **Spend limits** / **Spend alerts**
3. Configurer :
   - **Spend alert** : 33 USD (email d'alerte)
   - **Hard spend limit** (si option disponible) : 50 USD (safety net au-dessus de l'alerte)

### Fallback app-side si Anthropic n'expose pas de hard limit

À implémenter dans **SF-120-05** (scheduler + circuit breaker) :

- Lire le coût cumulé du mois via `UsageEventRepository` (table `usage_events` existante)
- Gate : bloquer tout nouvel appel Sonnet/Haiku si `monthlyCost >= 30 €`
- Alerte email à 15 € de consommation au prorata (garde-fou F-120 n° 6)

### Vérification

- [ ] Alerte configurée à 33 USD
- [ ] (Bonus) hard limit 50 USD configuré si l'UI le permet
- [ ] Screenshot pris et stocké dans ce dossier (`docs/features/F-120/anthropic-limit-setup-YYYY-MM-DD.png`)
- [ ] Date d'activation : ____

---

## À faire au moment de reprendre F-120

1. Récupérer nouvelle carte bancaire
2. Exécuter Actions 1 et 2 ci-dessus
3. Cocher les vérifications
4. Mettre à jour le **Statut** en haut de ce fichier : `⏳ EN ATTENTE` → `✅ ACTIVÉ`
5. Démarrer SF-120-03 (mini-spec + dev)
6. Mentionner dans la PR SF-120-03 : "Garde-fou coût F-120 (1) activé le YYYY-MM-DD — voir docs/features/F-120/PREREQUIS-AVANT-SF-120-03.md"

---

## Autres garde-fous coût (rappel — couverts dans SF ultérieures)

| # | Garde-fou | Couvert par |
|---|---|---|
| 1 | Hard limit OpenAI + alerte Anthropic | **Ce document** (prérequis SF-120-03) |
| 2 | Idempotence stricte (pas de re-traitement) | ✅ SF-120-02 (compareAndSwap statut) |
| 3 | Circuit breaker max 3 articles/jour + 15/sem | SF-120-05 |
| 4 | Rate-limit admin "Générer maintenant" 5/jour | SF-120-08 |
| 5 | Auth super-admin stricte endpoints d'écriture | SF-120-08 |
| 6 | Alerte email si coût projeté mois > 15 € au prorata | SF-120-05 |
| 7 | DALL-E uniquement après validation garde-fous IA | SF-120-04 |
