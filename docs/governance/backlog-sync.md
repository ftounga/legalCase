# Sync backlog DB (Étape 7 de la séquence obligatoire)

La règle de blocage automatique correspondante est définie dans `CLAUDE.md` section "Séquence obligatoire — Étape 7". Ce fichier détaille les modes de fonctionnement.

## Principe

Toute modification de `docs/PRODUCT_SPEC.md` ou `docs/MARKETING_BACKLOG.md` doit aboutir à une synchronisation des tables `backlog_features`, `backlog_subfeatures`, `backlog_marketing_tasks` consommées par l'écran super-admin `/super-admin/backlog` (F-178).

**Source de vérité** : les fichiers MD. La DB est un cache de lecture (Option A retenue F-178).

## Mode normal — automatique

Tâche `@Scheduled` cron 5 min qui parse les 2 fichiers et upsert les tables. Audit dans `backlog_sync_runs` (timestamp, durée, count, success/error).

**Aucun artefact obligatoire côté contributeur** — le merge sur master suffit.

## Mode resync manuelle — opérationnel

Si une modification doit être visible immédiatement (démo, présentation, debug), cliquer **"Resync now"** dans l'écran `/super-admin/backlog` (super-admin only). L'écran affiche un indicateur de fraîcheur ("Synchronisé il y a X minutes") visible en haut.

## En cas d'échec répété

Si la sync échoue répétitivement (visible dans `backlog_sync_runs.success = false` sur plusieurs runs consécutifs) : ouvrir un ticket — ne pas éditer la DB à la main. Les MD restent la source de vérité, la DB sera ré-écrasée au prochain cron réussi.
