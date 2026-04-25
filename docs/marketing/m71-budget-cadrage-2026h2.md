# M-71 — Cadrage budget marketing 2026 H2

**Statut** : `Validé` — déployable
**Date de cadrage** : 2026-04-25
**Horizon couvert** : mai 2026 → octobre 2026 (6 mois)
**Prochain audit** : T+3 mois (≈ 2026-07-25) — voir §7
**Référence pricing** : F-123 en production (SOLO 99 € / TEAM 219 € / PRO 429 €)

---

## 1. Objectif

Définir l'enveloppe marketing 6 mois, l'arbitrage entre canaux, et la règle de réallocation dynamique de la réserve. Sert de **référence opposable** à toute tâche M-XX du `MARKETING_BACKLOG.md` (cf. CLAUDE.md règle 2 du contrôle de cohérence marketing).

## 2. Hypothèses de calcul

| Variable | Valeur | Justification |
|----------|--------|---------------|
| ARR moyen / nouveau client | **2 300 €/an** | Mix 50 % SOLO / 35 % TEAM / 15 % PRO au pricing F-123 |
| Rétention annuelle | 80 % | Standard SaaS B2B legal sticky une fois intégré |
| Durée client moyenne | 5 ans | 1 / (1 – 0,80) |
| LTV brute / client | 11 500 € | Non actualisée |
| LTV 3 ans actualisée | **6 000 €** | Horizon de référence pour le calcul ROI |
| Seuil PMF visé | **10+ cabinets payants** | Nécessaire pour défendre M-71 v2 (révision T+3) et lever / repricer ultérieurement |

Toute évolution du pricing, du mix, ou de la rétention impose une révision de ce document.

## 3. Enveloppe globale

| Tranche | Montant | Disponibilité | Destination |
|---------|---------|---------------|-------------|
| **Tranche 1** | **13 000 €** | Immédiate | Plan B-dynamique (§4) |
| **Tranche 2 — dry powder** | **9 000 €** | Activable à T+2 mois (cash mobilisable confirmé) | Doublement du canal gagnant à T+3 mois (§7) |
| **Total mobilisable** | **22 000 €** | — | — |

**Décision rule** : la Tranche 2 n'est **pas** déployée par défaut. Elle est activable **uniquement si** l'audit signal T+3 (§7) identifie un canal au ROI Y1 ≥ 2,5× ou un signal PMF crédible. Sinon, elle est conservée en réserve pour T+6 ou re-cadrage.

## 4. Plan B-dynamique — répartition Tranche 1 (13 000 €)

| Poste | Montant | Backlog | Justification |
|-------|---------|---------|---------------|
| Atelier sponsorisé Transfodroit 30 min (sans stand) | 3 500 € | M-72 / nouveau M-XX post-sondage | Audience captive 50–200 avocats > stand passif pour marque inconnue |
| Déplacements + supports impression Transfodroit | 500 € | — | — |
| Vidéo motion design (4 vidéos + voix off) | 1 200 € | M-11 (engagé) | Multiplicateur +25 % sur tous les autres canaux |
| Village de la Justice — article + relais newsletter 60 K | 600 € | M-53 (engagé) | Effet 12 mois, intent éditorial élevé |
| Google Ads FR (campagne 4–6 sem) | 1 500 € | M-56 | F-119 conversion tracking opérationnel |
| SDR freelance 6 semaines | 2 400 € | M-57 | 800–1 500 messages personnalisés, 1–3 paid attendus |
| Belgique partenariat éditorial | 500 € | M-65 | Jubel.be / Droitbelge.be / Justice-en-ligne.be |
| Barreaux locaux gratuits + déplacements | 1 000 € | M-58 | 2–3 événements gratuits, 1–2 paid attendus |
| **Réserve activable mois 0–3** | **1 800 €** | — | Mobilisable sans nouveau cadrage si opportunité court terme (re-pitch atelier, partenariat opportuniste, voix off premium) |
| **Total Tranche 1** | **13 000 €** | — | — |

**Test à coût zéro avant engagement Transfodroit** : passer M-72 (sondage Village de la Justice) pour tester l'éligibilité **Technodroit 2026**. Si retenu : pitch scène + jury + relais média = équivalent atelier sponsorisé pour 0 €. Réallocation alors : 3 500 € vers SDR cycle 2 ou SEA dopé. Asymétrie pure — à privilégier systématiquement.

## 5. Rendement attendu

Plan B-dynamique au pricing F-123, médiane des fourchettes par canal :

| Métrique | Valeur attendue |
|----------|----------------|
| Clients payants 6 mois | **16** (low 10, high 25) |
| ARR Year 1 | **37 K€** |
| ROI Y1 | **2,8×** |
| LTV 3 ans | **96 K€** |
| LTV / € investi | **7,4×** |
| Probabilité PMF (≥ 10 payants à 6 mois) | **80 %** |

Espérance ajustée du retour : **77 K€ LTV** sur 13 K€ investis.

## 6. Canaux écartés et raisons (anti-charrue-avant-bœufs)

| Canal | Coût | Raison du rejet |
|-------|------|-----------------|
| 2ᵉ événement physique (CNB Convention Avocats / Lexpo Amsterdam) | 4–5 K€ | Légitimité institutionnelle déjà acquise par Transfodroit. ROI marginal 1,4× — sous le seuil de réinvestissement |
| 2ᵉ cycle SDR en bloc dès mois 0 | 2 400 € | Doit être conditionné au signal cycle 1 (bon ICP confirmé) — sinon dépense sèche |
| SEA dopé > 1 500 € dès mois 0 | +1 500 € | Volume linéaire sans saut qualitatif — à reconsidérer en T+3 si conversion landing solide |
| Stand physique grand format Transfodroit | 5–8 K€ | Rendement médiocre pour marque inconnue vs atelier captif |

Ces canaux peuvent être **réintroduits via la Tranche 2** au mois 3 si l'audit signal le justifie.

## 7. Audit signal — décision Tranche 2 (T+3 mois)

À mener au mois 3 (≈ 2026-07-25) sur la base des données F-76 (dashboard super-admin) + F-119 (tracking conversion) + retours qualitatifs cabinets.

### Tableau de décision Tranche 2

| Signal observé à T+3 | Décision Tranche 2 |
|----------------------|--------------------|
| Un canal a converti ≥ 5 paid customers, ROI Y1 ≥ 2,5× | **Doubler** ce canal avec 4–6 K€ de la Tranche 2 |
| Atelier Transfodroit a généré ≥ 3 paid + multiplicateur outreach mesuré | **Activer 2ᵉ événement** (CNB Convention Avocats novembre) jusqu'à 5 K€ |
| SDR cycle 1 a trouvé l'ICP (≥ 8 trials, ≥ 2 paid) | **Lancer 2ᵉ cycle SDR** 2 400 € |
| Pas de canal à ROI ≥ 2× | **Ne pas activer la Tranche 2** — conserver pour T+6 ou pivoter |
| Signal mixte | Activation partielle (4 K€) ciblée sur le moins risqué |

### Métriques à figer pour l'audit

- Nb de leads qualifiés / canal
- Trial conversion par canal
- Trial → paid par canal
- ARR cumulé Y1
- CAC observé / canal
- Retour qualitatif (objection-clé, segments converts vs non-converts)

## 8. Règles de gouvernance attachées

1. **Toute nouvelle tâche événementielle ou canal payant > 1 000 €** ajouté au `MARKETING_BACKLOG.md` doit être justifié contre ce document (cf. CLAUDE.md règle 2). Si le coût sort de la Tranche 1, la décision passe par la Tranche 2 (audit T+3).
2. **Toute évolution majeure du pricing** (nouveau plan, hausse > 15 %, nouveau gate) impose une révision de §2 et §5.
3. **Toute évolution du mix client observé** (si le mix réel s'écarte de 50/35/15 de plus de 10 points) impose recalcul de l'ARR moyen.
4. **Audit T+3 obligatoire** : sans audit, pas d'activation Tranche 2.
5. **Re-cadrage T+6** : à T+6 mois, ce document devient obsolète et doit être remplacé par `m71-budget-cadrage-2026h2-v2.md` (ou cadrage 2027 H1).

## 9. Lien avec le backlog marketing

Ce document **remplace de fait** la section "Stratégie acquisition 7 000 €" du `MARKETING_BACKLOG.md`. Le backlog reste la source des tâches opérationnelles M-XX ; ce document est la grille budgétaire et stratégique qui les arbitre.

Tâches directement pilotées par ce cadrage :
- **M-11** vidéo (engagée, dans Tranche 1)
- **M-53** Village de la Justice (engagée, dans Tranche 1)
- **M-56** Google Ads FR (à lancer)
- **M-57** SDR freelance (à recruter Malt)
- **M-58** Barreaux locaux (à activer)
- **M-65** Belgique partenariat éditorial (à activer)
- **M-72** Sondage Village de la Justice (à débloquer immédiatement — porte d'entrée Transfodroit / Technodroit)
- **M-73** One-pager traction (à produire — réutilisable partout)
- **M-75** Veille AMI France Legaltech 2027 (à lancer en parallèle)

Tâches conditionnées à la Tranche 2 (à introduire au backlog seulement si audit T+3 le justifie) :
- 2ᵉ événement physique (CNB Convention Avocats ou équivalent)
- 2ᵉ cycle SDR
- Doublement Google Ads
- Candidature Technodroit (si non gratuite via M-72)

## 10. Historique des révisions

| Date | Version | Auteur | Changement |
|------|---------|--------|-----------|
| 2026-04-25 | v1 | Franck Tounga | Cadrage initial — Plan B-dynamique 13 K€ + 9 K€ dry powder |
