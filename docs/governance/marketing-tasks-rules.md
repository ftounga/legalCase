# Tâches marketing — règles de gouvernance

Les règles de blocage automatique correspondantes sont définies dans `CLAUDE.md` section "Tâches marketing". Ce fichier détaille les règles complètes.

## Règle 1 — Complétion

Toute tâche du `docs/MARKETING_BACKLOG.md` suit cette règle :

**Une tâche marketing n'est marquée `Terminé` que si elle est entièrement opérationnelle en production.**

- Un email rédigé mais non branché dans le code → statut `Rédigé`, pas `Terminé`
- Une page web rédigée mais non déployée → statut `Rédigé`, pas `Terminé`
- Un document produit mais non publié/transmis → statut `Rédigé`, pas `Terminé`

Quand une tâche marketing implique du code (email automatique, tracking, intégration), elle doit passer par la séquence de dev standard (mini-spec → dev → review → push) avant d'être marquée `Terminé`.

## Règle 2 — Contrôle de cohérence avant tout ajout au backlog marketing

Avant d'ajouter une nouvelle tâche dans `docs/MARKETING_BACKLOG.md`, exécuter et **afficher dans la conversation** le contrôle suivant en 4 points :

1. **Cohérence budgétaire** — la tâche entre-t-elle dans l'enveloppe marketing en vigueur (cadrage actif `docs/marketing/m71-budget-cadrage-2026h2.md` ou successeur) ? Si l'enveloppe doit être dépassée, l'ajout est conditionné à une décision d'arbitrage budgétaire explicite, et la tâche prérequise correspondante (cadrage / révision d'enveloppe) doit être créée d'abord.
2. **Doublon avec une feature produit** — la capacité technique demandée n'est-elle pas déjà couverte par une feature livrée du `PRODUCT_SPEC.md` ? Exemples : ne pas demander Google Analytics si F-77 est `Terminée`, ne pas demander un tracking conversion Google Ads si F-119 est `Terminée`.
3. **Doublon backlog (overlap > 30 %)** — scanner les sections M-XX existantes par thème (Site, Vidéo, Email, LinkedIn, Vente, Belgique, Stratégie acquisition, Mesure, Cadrage stratégique) pour vérifier qu'aucune tâche existante ne couvre déjà l'intention. Si oui, étendre la tâche existante plutôt qu'en créer une nouvelle (cf. règle "feedback_backlog_overlap_analysis").
4. **Séquence stratégique** — la tâche met-elle la charrue avant les bœufs (par ex. engager un stand événementiel à 5 k € avant d'avoir tranché l'enveloppe globale et l'arbitrage entre canaux SEO/SEA/SDR/événements) ? Si oui, créer d'abord la tâche prérequise (cadrage budget, validation traction, etc.).

Le contrôle doit produire un tableau visible dans la réponse (tâche proposée → verdict 4 points → action). Les tâches qui sortent du contrôle peuvent être marquées `Bloqué` si elles dépendent d'un arbitrage non encore tranché.
