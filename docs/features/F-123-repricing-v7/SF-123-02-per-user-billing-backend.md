# Mini-spec — F-123 / SF-123-02 Per-user billing backend

## Identifiant · `F-123 / SF-123-02`
## Date · `2026-04-20` · Branche (à créer) · `feat/SF-123-02-per-user-billing-backend`

## Objectif
Facturer les seats au-delà du quota inclus via `Stripe subscriptions.update(quantity)` et verrouiller l'invitation d'un nouveau membre par une gate plan × `seat_count`. Le workspace paie l'abonnement de base + X seats supplémentaires automatiquement.

## Modèle de facturation ciblé
| Plan | Seats inclus | Cap total | Prix extra seat |
|------|--------------|-----------|-----------------|
| FREE | 1            | 1         | —               |
| SOLO | 1            | 1         | — (upgrade vers TEAM pour 2e user) |
| TEAM | 3            | 6         | +59 €/user/mois |
| PRO  | 5            | illimité  | +79 €/user/mois |

La quantité Stripe du line item V2 correspond au nombre total de seats (inclus + supp). Le prix de base + incréments est configuré côté Stripe via `tiered` pricing (graduated : première tranche = seats inclus à 0 €, deuxième tranche = incréments à 59/79 € per-unit). Aucune logique de tarification côté backend — Stripe fait tout le calcul. Le backend pousse juste le `quantity`.

## Comportement nominal
1. **Invitation** (`WorkspaceInvitationService.createInvitation`) : avant de persister `WorkspaceInvitation`, calculer `currentSeats = members + pendingInvitations` et refuser en 402 PAYMENT_REQUIRED si `currentSeats + 1 > maxSeats(plan)`.
2. **Acceptation** (`acceptInvitation`) : après `workspaceMemberRepository.save(newMember)`, appeler `StripeSeatService.syncSeatCount(workspaceId)` qui :
   - recalcule `seatCount = members.count` (authoritative post-save)
   - si `subscription.stripeSubscriptionId != null` et plan ∈ {TEAM, PRO} : `Subscription.update(stripeSubId, params{quantity=seatCount, proration_behavior=CREATE_PRORATIONS})`
   - met à jour `Subscription.seatCount` localement
3. **Retrait membre** (`WorkspaceMemberService.removeMember`) : même appel `syncSeatCount` après `delete(targetMember)`.
4. **Webhook `customer.subscription.updated`** : si `items.data[0].quantity` diffère de `Subscription.seatCount`, mettre à jour localement (cas où l'admin modifie la quantité depuis le Dashboard Stripe).

## Cas d'erreur
- Stripe 402 / card_declined lors de l'update quantity → rollback : ne pas persister l'acceptation. L'invitation reste PENDING (pas ACCEPTED). L'avocat reçoit un message "Paiement refusé — mettez à jour votre carte avant d'ajouter un membre".
- Subscription sans `stripeSubscriptionId` (FREE ou PENDING) → `syncSeatCount` met à jour seatCount local sans appel Stripe et gate invitation reste effective via seatCount.
- Plan SOLO avec 2e invitation → 402 avec message "Passez à TEAM pour inviter un collaborateur".

## Critères d'acceptation
- [ ] Migration Liquibase 095 `subscriptions.seat_count INTEGER NOT NULL DEFAULT 1`
- [ ] `Subscription` entity + `PlanLimitService.getMaxSeats(planCode)` : FREE 1 / SOLO 1 / TEAM 6 / PRO Integer.MAX_VALUE (pas de cap PRO)
- [ ] `PlanLimitService.getIncludedSeats(planCode)` : FREE 1 / SOLO 1 / TEAM 3 / PRO 5 (exposé pour UI)
- [ ] Gate `createInvitation` : 402 si dépassement (avec messages par plan)
- [ ] `StripeSeatService.syncSeatCount(UUID)` idempotent, no-op si stripeSubId null ou plan FREE/SOLO
- [ ] Webhook `customer.subscription.updated` met à jour seatCount local si divergence
- [ ] Rollback transactionnel si Stripe update échoue (acceptation non persistée, pas de membre fantôme)

## Plan de test minimal
### Unitaires
- U-01 `PlanLimitService.getMaxSeats` : 4 cas plans
- U-02 `PlanLimitService.getIncludedSeats` : 4 cas plans
- U-03 `WorkspaceInvitationService.createInvitation` SOLO + 2e user → 402
- U-04 `WorkspaceInvitationService.createInvitation` TEAM avec 6 seats utilisés → 402
- U-05 `WorkspaceInvitationService.createInvitation` PRO avec 20 seats → OK (pas de cap)
- U-06 `StripeSeatService.syncSeatCount` TEAM 4 seats → Stripe.update appelé avec quantity=4
- U-07 `StripeSeatService.syncSeatCount` FREE / SOLO → no-op Stripe
- U-08 `StripeSeatService.syncSeatCount` stripeSubId null → no-op
- U-09 Webhook `customer.subscription.updated` quantity=3 → seatCount local = 3

### Intégration
- IT-01 Acceptation invitation TEAM → membre créé + seatCount=2 (Stripe mocké)
- IT-02 Retrait membre TEAM → seatCount décrémenté
- IT-03 Rollback transactionnel : Stripe jette RuntimeException → membre non persisté
- IT-04 Isolation workspace : invitation workspace A n'affecte pas seatCount workspace B

## Tables / endpoints / composants impactés
### Backend
- `Subscription.java` : +`seatCount` Integer
- `PlanLimitService.java` : +`getMaxSeats(String)`, `getIncludedSeats(String)`, `getSeatCountForWorkspace(UUID)`
- `StripeSeatService.java` (nouveau)
- `WorkspaceInvitationService.java` : gate + sync après acceptation
- `WorkspaceMemberService.removeMember` : sync après delete
- `StripeWebhookService.java` : handler `customer.subscription.updated` (si non présent, ajouter)
- Migration 095

### Pas impacté
- `StripeCheckoutService` : la quantité initiale reste 1 lors du checkout (un nouvel abonnement démarre toujours à 1 seat — le owner). Les seats supplémentaires se font via `subscription.update(quantity)` après acceptation d'invitation.

## Analyse de cohérence transversale
| Cible | Évaluation | Classement |
|-------|-----------|------------|
| F-16 plans / quotas | `getIncludedSeats` + `getMaxSeats` alignés avec la nouvelle grille V2 | Intégré |
| F-11 invitations | gate ajoutée `createInvitation` | Intégré |
| F-106 membres (liste) | `removeMember` émet sync seat | Intégré |
| Isolation workspace | seatCount stocké côté Subscription, filtré par workspaceId | Intégré (test IT-04) |
| Webhook Stripe | `customer.subscription.updated` nouveau handler | Intégré |
| Frontend billing | hors scope ici, traité SF-123-03 | SF parallèle |

Pas de composant partagé / directive / DTO réutilisable nouveau — `StripeSeatService` n'est consommé que par 2 endroits internes.

## Préoccupations transversales
- **Plans / limites** : oui — nouveau gate. Composants impactés listés ci-dessus. Aucun autre endroit n'appelle `createInvitation` ou `removeMember`. Pas de smoke test touchant la séquence invitation → facturation.
- **Auth / Principal** : inchangé.
- **Workspace context** : inchangé (résolution via `@AuthenticationPrincipal OidcUser`).

## Hors scope
- UI frontend pour afficher coût d'un membre (SF-123-03)
- Upgrade automatique de plan au dépassement (reste : message "Passez à TEAM")
- Gestion des seats non-utilisés (on facture le total = inclus + supp, même si quelques invitations restent PENDING — aligné avec la pratique SaaS)
- Refund au retrait de membre → Stripe proration créée automatiquement en crédit sur la prochaine facture (comportement natif `CREATE_PRORATIONS`)
- Création côté Dashboard Stripe des tiered prices V2 — opération manuelle documentée dans la PR
