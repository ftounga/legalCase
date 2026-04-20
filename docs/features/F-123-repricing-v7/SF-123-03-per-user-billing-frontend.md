# Mini-spec — F-123 / SF-123-03 Per-user billing frontend

## Identifiant · `F-123 / SF-123-03`
## Date · `2026-04-20` · Branche (à créer) · `feat/SF-123-03-per-user-billing-frontend`

## Objectif
Rendre visible à l'avocat le coût qu'il s'apprête à engager quand il invite un membre, et lui rappeler où il en est (seats utilisés / inclus / cap) sur sa page billing. Sans ça, SF-123-02 est une gate silencieuse : le backend bloque ou facture, l'avocat ne comprend pas pourquoi.

## Comportement nominal

### A — Endpoint backend léger `GET /api/v1/billing/seats-summary`
Retourne pour le workspace courant :
```
{
  "planCode": "TEAM",
  "seatCount": 4,
  "includedSeats": 3,
  "maxSeats": 6,
  "extraSeatPriceCents": 5900,
  "baseMonthlyCostCents": 21900,
  "totalMonthlyCostCents": 27800  // 219 + 1×59
}
```
Autorisation : OWNER ou ADMIN (comme les invitations).

### B — Header liste membres (`workspace-members`)
Au-dessus du tableau, carte compacte :
> **Plan TEAM · 4 / 6 utilisateurs · 278 €/mois**
> 3 inclus · 1 seat supplémentaire (+59 €)

### C — Modale "Inviter un membre" (MatDialog)
Remplace le form inline actuel. Champs email + rôle. **Section coût prévisionnel** :
- Cas "encore dans les seats inclus" : *"Invitation gratuite (inclus dans votre plan)"*
- Cas "au-delà du quota inclus" : *"Ajoutera 59 € TTC/mois à votre abonnement (proratisé au jour)"*
- Cas "cap atteint" : bouton disabled + lien "Passer à TEAM/PRO" (selon plan courant)

Sur 402 retour backend : snackbar affichant le message venu du backend ("Passez à TEAM …" / "Passez à PRO …").

### D — Confirm dialog retrait membre (MatDialog)
Remplace l'appel direct actuel. Affiche :
> *Retirer **Jean Dupont** de l'espace ?*
> *Il perdra accès aux dossiers. Le coût mensuel passera de 278 € à 219 €.*

### E — Section "Utilisateurs actifs" dans `workspace-billing`
Sous la grille des plans, bloc dédié :
- "Plan TEAM — 4 / 6 utilisateurs"
- Détail tarif : 3 inclus dans le plan de base + 1 seat supplémentaire (+59 €/mois)
- "Coût total : 278 €/mois"
- CTA "Gérer les membres" → lien vers `/workspace/members`

## Cas d'erreur
- Endpoint `/seats-summary` retourne 403 → composant reste silencieux (panneau non affiché), loggue en console
- Appel `createInvitation` retourne 402 → snackbar avec le message backend
- Appel `removeMember` échoue → snackbar "Erreur lors du retrait" (comportement existant conservé)
- Endpoint `/seats-summary` retourne 5xx → fallback : on affiche juste "Plan X · N utilisateurs" sans le coût

## Critères d'acceptation
- [ ] Backend `GET /api/v1/billing/seats-summary` retourne DTO ci-dessus, 403 pour LAWYER/MEMBER
- [ ] Frontend `BillingService.getSeatsSummary()` typé + intégré
- [ ] Header `workspace-members` affiche "X/Y utilisateurs · N €/mois" via signal
- [ ] Modale invite (MatDialog) remplace le form inline, coût prévisionnel affiché dynamiquement selon le cas
- [ ] Modale invite sur 402 : snackbar avec message backend
- [ ] Confirm dialog retrait avec preview coût avant/après
- [ ] Section "Utilisateurs actifs" dans `workspace-billing` avec CTA "Gérer les membres"
- [ ] 3 tests backend (GetSeatsSummary : nominal TEAM / SOLO / FREE + 403 pour LAWYER)
- [ ] 4 tests frontend (`workspace-members` : modale ouvre + coût / 402 snackbar / confirm dialog bloque la suppression avant OK / `workspace-billing` seats section affichée)

## Tables / endpoints / composants impactés
### Backend
- **Nouveau** : `BillingSeatsController` + `SeatsSummaryResponse` DTO + `BillingSeatsService`
- `PlanLimitService` : `getExtraSeatPriceCents(planCode)` (FREE/SOLO 0, TEAM 5900, PRO 7900) + `getBaseMonthlyCostCents(planCode)` (FREE 0, SOLO 9900, TEAM 21900, PRO 42900)

### Frontend
- `BillingService.getSeatsSummary()` (nouveau)
- `WorkspaceMembersComponent` : header carte + modale invite (nouveau composant `invite-member-dialog`) + confirm dialog retrait (nouveau composant `confirm-remove-member-dialog`)
- `WorkspaceBillingComponent` : nouvelle section "Utilisateurs actifs"

## Analyse de cohérence transversale
| Cible | Évaluation | Classement |
|-------|-----------|------------|
| F-11 invitations | UX modale remplace form inline, service HTTP inchangé | Intégré |
| F-16 plans / grille | Section seats vient s'empiler à la grille plans existante | Intégré |
| F-106 membres liste | Confirm dialog ajouté au flow remove existant | Intégré |
| Autres outils avec confirm → retrait ? | Scanné : pas d'autre flow de retrait membre V1 | N/A |
| Design System : MatDialog (pas `window.confirm`) | Conforme règle review-checklist | Intégré |

### Cas spécifique : nouveaux composants partagés
- `invite-member-dialog` et `confirm-remove-member-dialog` sont spécifiques à cette feature (pas de pattern de réutilisation immédiate identifié). Pas de promotion en composant partagé — si un 2e usage apparaît, on refactorera alors.

## Préoccupations transversales
- **Plans / limites** : oui — consommateur supplémentaire de l'info plan. Aucun impact sur les gates existantes (qui restent côté backend).
- **Auth / Principal** : nouvel endpoint lit le Principal pour récupérer le workspace courant — reste cohérent.
- **Workspace context** : endpoint filtre par workspace courant, mêmes guards qu'invitations.
- **Navigation** : CTA "Gérer les membres" = route existante `/workspace/members`, pas de nouveau route.

## Hors scope
- Mise à jour automatique du plan quand cap atteint (reste : CTA "Passer à TEAM/PRO" qui ouvre le checkout existant via `workspace-billing`)
- Animation de transition du montant quand on ajoute un seat
- Historique des changements de seat_count
- Alerte email "Votre facture du mois prochain passera à X €" — reste hors scope (Stripe envoie déjà ses emails de proration)
