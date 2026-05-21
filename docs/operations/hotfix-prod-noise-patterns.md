# Patterns d'erreur ignorés par `prod-health-check`

Liste des signatures d'erreur considérées comme **bruit acceptable** — la skill `prod-health-check` ne les re-listera pas dans `hotfix-prod.md`.

> Format : 1 pattern par ligne, signature exacte (substring matché dans le message log) suivi d'un commentaire bref justifiant l'exclusion.
>
> Pour exclure un nouveau pattern : ajoute-le ici manuellement après avoir examiné un item `IGNORÉ` dans `hotfix-prod.md`. La skill lit ce fichier à chaque run.

---

## Patterns exclus

_(aucun pour le moment — ce fichier se remplira au fil de tes décisions de tri)_

<!-- Exemples futurs :
- `OAuth2AuthenticationException: invalid_grant` — token avocat expiré, comportement normal, pas une régression
- `Webhook signature verification failed.*Stripe` — Stripe retry pattern au démarrage pods, transitoire
- `HikariPool.*Connection is not available` UNIQUEMENT pendant les 60s post-deploy — saturation transitoire normale
-->
