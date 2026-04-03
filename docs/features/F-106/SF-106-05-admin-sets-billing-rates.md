# Mini-spec — F-106 / SF-106-05 — Taux horaires gérés par l'admin

---

## Identifiant

`F-106 / SF-106-05`

## Feature parente

`F-106` — Suivi du temps facturable par dossier

## Statut

`ready`

## Date de création

2026-04-03

## Branche Git

`feat/SF-106-05-admin-sets-billing-rates`

---

## Objectif

Transférer la gestion des taux horaires aux OWNER/ADMIN : l'admin fixe le taux de chaque membre depuis la page Administration ; les membres voient leur taux en lecture seule.

---

## Comportement attendu

### Cas nominal

**Admin (OWNER/ADMIN) :**
1. Ouvre Administration → section Membres & taux horaires
2. Pour chaque membre, voit son taux actuel (ou "—" si non défini)
3. Saisit un nouveau taux dans le champ inline et clique "Enregistrer"
4. Le taux est sauvegardé pour cet utilisateur ; un AuditLog `BILLING_RATE_UPDATED` est créé

**Membre (LAWYER/MEMBER) :**
1. Ouvre "Mon temps facturable" → voit son taux actuel en lecture seule (`200 €/h` ou "Non configuré")
2. Aucun champ de saisie — message explicatif : "Votre taux est défini par votre administrateur"
3. Le timer widget : si aucun taux → message "Votre taux n'a pas encore été configuré. Contactez votre administrateur."

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| MEMBER tente PUT /members/{userId}/billing-rate | Accès refusé | 403 |
| userId n'appartient pas au workspace de l'admin | Accès refusé | 403 |
| ratePerHour ≤ 0 ou > 9999.99 | Validation échouée | 400 |
| userId inexistant | Ressource non trouvée | 404 |

---

## Critères d'acceptation

- [ ] Un OWNER ou ADMIN peut définir le taux d'un membre via PUT /api/v1/workspace/members/{userId}/billing-rate
- [ ] Un MEMBER ne peut pas accéder à PUT /api/v1/workspace/members/{userId}/billing-rate (403)
- [ ] L'admin ne peut pas modifier le taux d'un membre d'un autre workspace (403)
- [ ] La page Admin affiche le taux de chaque membre avec édition inline
- [ ] La page "Mon temps facturable" affiche le taux en lecture seule uniquement
- [ ] Le timer widget affiche le bon message quand aucun taux n'est défini par l'admin
- [ ] Un AuditLog BILLING_RATE_UPDATED est créé à chaque modification par l'admin
- [ ] L'endpoint existant PUT /api/v1/workspace/billing-rate (self-service) est supprimé ou restreint à OWNER/ADMIN uniquement

---

## Périmètre

### Hors scope

- Workflow d'approbation (PENDING/APPROVED) — décision explicite de ne pas implémenter
- Historique des taux par membre visible dans l'UI (la table user_billing_rates le stocke déjà, pas d'écran dédié)
- Notification au membre quand son taux est modifié

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| PUT | `/api/v1/workspace/members/{userId}/billing-rate` | Oui | ADMIN |
| GET | `/api/v1/workspace/billing-rate` | Oui | MEMBER (lit son propre taux — inchangé) |
| ~~PUT~~ | ~~`/api/v1/workspace/billing-rate`~~ | — | Supprimé (self-service) |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| user_billing_rates | INSERT | Même logique qu'aujourd'hui, userId cible au lieu de l'utilisateur connecté |
| audit_logs | INSERT | BILLING_RATE_UPDATED avec metadata userId cible + rate |

### Migration Liquibase

- [x] Non applicable — pas de changement de schéma

### Composants Angular

- `WorkspaceAdminComponent` — remplacer la section "Facturation" (taux personnel) par une section "Taux horaires membres" avec liste et édition inline par membre
- `TimeReportComponent` — remplacer l'input taux par un affichage lecture seule
- `TimerWidgetComponent` — mettre à jour le message "no rate" pour mentionner l'administrateur

---

## Plan de test

### Tests unitaires

- [ ] `BillingRateService.setRateForMember()` — nominal : taux sauvegardé + audit log
- [ ] `BillingRateService.setRateForMember()` — userId hors workspace → 403
- [ ] `BillingRateService.setRateForMember()` — appelant non OWNER/ADMIN → 403

### Tests d'intégration

- [ ] `PUT /members/{userId}/billing-rate` en tant que OWNER → 200
- [ ] `PUT /members/{userId}/billing-rate` en tant que MEMBER → 403
- [ ] `PUT /members/{userId}/billing-rate` avec userId hors workspace → 403
- [ ] `GET /billing-rate` par le membre cible après modification → retourne le nouveau taux
- [ ] Audit log créé après modification admin

### Isolation workspace

- [ ] Applicable — un admin du workspace A ne peut pas modifier le taux d'un membre du workspace B

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Workspace context** — résolution du workspace de l'utilisateur cible (pas l'appelant)
- [ ] Auth / Principal
- [ ] Plans / limites
- [ ] Navigation / routing frontend

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `PUT /api/v1/workspace/billing-rate` | Supprimé — les tests IT existants I-BR-01/02 doivent être mis à jour | Remplacer par le nouvel endpoint dans les tests |
| `WorkspaceAdminComponent` | Section Facturation remplacée | Spec mis à jour |
| `TimeReportComponent` | Input taux supprimé | Spec mis à jour |
| `TimerWidgetComponent` | Message "no rate" mis à jour | Spec mis à jour |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné (la gestion des taux n'est pas dans les chemins critiques auth/workspace/navigation)

---

## Dépendances

### Subfeatures bloquantes

- SF-106-01 à SF-106-04 — statut : done

---

## Notes et décisions

- Le self-service `PUT /api/v1/workspace/billing-rate` est supprimé : seul l'admin fixe les taux
- `BillingRateService.setRate()` est remplacé par `setRateForMember(targetUserId, request, caller)`
- La table `user_billing_rates` ne change pas — même schéma, même logique d'historique
- L'endpoint GET reste inchangé — chaque membre lit son propre taux
