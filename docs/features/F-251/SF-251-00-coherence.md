# Cohérence fonctionnelle — F-251 (étape 0)

Date : 2026-05-20
Verdict : **GO sans ajustement**

## Workflow métier impacté

Période d'évaluation 14 jours d'un compte avocat (plan FREE). Deux acteurs concernés :

1. **Avocat / propriétaire de workspace** — consulte la date d'expiration dans `workspace-admin` (carte « Période d'évaluation ») et reçoit un bandeau `trial-banner` dès que la trial est active. À expiration, le compte bascule en lecture seule (quotas plan = 0 sur ouverture dossier, upload doc, analyse, ré-analyse).
2. **Super-admin / équipe LegalCase** — provisionne occasionnellement des comptes démo « bypass IHM » via SQL direct (cas Marjolaine RENVERSEZ, workspace `5d07e421-3e3c-4076-91a1-9ff8e8aaf7b8`) — chaîne hors `WorkspaceService.createWorkspace`.

## Cartographie features existantes

| Feature parente | Rôle dans le workflow |
|-----------------|----------------------|
| F-16 Gestion abonnements | Modèle `Subscription` + colonne `expires_at` nullable |
| F-19 + F-19-02 FREE trial | Crée subscription avec `expires_at = now+14d` via `WorkspaceService` |
| F-25 Super-admin | Provisionnement bypass IHM (source de la dérive) |
| F-247 Résiliation self-service | Mutation côté sortie, disjoint de l'enjeu NULL création |

## Challenge amont

- **Pré-requis fonctionnels présents ?** Oui — la chaîne nominale (`WorkspaceService.createWorkspace`) fixe systématiquement `expires_at` ; le bug ne survient que sur le canal bypass.
- **Surface code stable ?** Oui — `Subscription` entité, `PlanLimitService.isExpiredFree`, `WorkspaceResponse.expiresAt` existent et sont consommés côté frontend.

## Challenge aval

- **La sortie est-elle exploitable ?** Oui — fixer `expires_at` dans la DB rend immédiatement opérationnels :
  - `isTrial(workspace)` côté `workspace-admin` (carte trial s'affiche)
  - `trial-banner` (bandeau visible jusqu'à expiration)
  - `PlanLimitService.isExpiredFree` (bascule quota 0 à expiration)
  - `workspace-billing` (`new Date(ws.expiresAt) < new Date()` pour bandeau « trial expirée »)

Aucun code consommateur n'a besoin d'être modifié — les 4 callsites supposent déjà que `expiresAt` peut être présent.

## Invariants anti-gadget

1. La migration data **doit être idempotente** : ré-exécutée, elle ne touche aucune row déjà fixée (filtre `expires_at IS NULL` obligatoire).
2. La migration **doit logger le nombre de rows touchées** (traçabilité post-déploiement) — sinon impossible d'auditer si Marjolaine a bien été corrigée vs un quelconque autre compte démo.
3. Le **garde-fou backend** (SF-251-02) doit s'appliquer même au canal bypass — c'est sa raison d'être. Une validation côté `WorkspaceService` uniquement ne couvrirait pas le cas INSERT SQL direct → préférer `@PrePersist` (intercepte tous les flux JPA).
4. Le `@PrePersist` ne doit **pas écraser** `expires_at` quand il est déjà fourni — comportement défensif uniquement (fallback NULL → `started_at + 14 days`).

## Verdict

**GO** — F-251 = bug fix transverse au workflow trial, pas de gadget, surface restreinte.
