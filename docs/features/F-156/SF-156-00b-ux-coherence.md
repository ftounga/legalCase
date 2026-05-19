# F-156 — Cadrage cohérence écran (étape 0 bis)

> Produit par la skill `ai-skills/screen-coherence-challenger.md`.
> Date : 2026-05-19.

## Verdict : **GO**

Placement, séquence, charge et continuité sont tous cohérents. La feature s'insère sur des écrans existants (workspace switcher dropdown + dialog de création) **sans ajouter de nouvel écran**, et l'état terminal (avocat dans le nouveau workspace) est explicite. Aucun ajustement écran requis hors invariants anti-surcharge ci-dessous.

## Intention métier + comportement visible attendu

**Intention** : restreindre la création d'un workspace supplémentaire aux plans TEAM / PRO (cf. SF-156-00-coherence.md).

**Comportement visible** :
- Pour un OWNER TEAM/PRO : l'option « + Créer un workspace » apparaît dans le dropdown du switcher ; au clic, un dialog s'ouvre avec un sélecteur de plan obligatoire (cards TEAM / PRO) puis redirige vers Stripe Checkout.
- Pour un OWNER FREE/SOLO : l'option « + Créer un workspace » est **invisible** dans le dropdown. Si l'endpoint est appelé par un autre chemin (URL bricolée), une page d'erreur dédiée invite à upgrader.

## Rappel verdict feature-coherence-challenger

`docs/features/F-156/SF-156-00-coherence.md` — verdict **GO**.

## Parcours écran réel de l'avocat (création d'un workspace additionnel)

> Source : composants Angular réellement codés (`WorkspaceSwitcherComponent` F-154, `WorkspaceCreateDialogComponent` existant) + pratique standard d'apps SaaS multi-tenant (Slack, Notion, Linear utilisent tous le pattern « switcher dropdown + dialog »). ⚠ hypothèse à valider auprès d'un avocat opérant déjà plusieurs entités.

1. L'avocat est sur **n'importe quel écran de l'app** authentifié (dashboard, détail dossier, etc.) — le header / top-bar est visible avec le **workspace switcher** à droite (composant F-154).
2. Le switcher affiche le **nom du workspace courant** + une chevron-down qui invite au clic.
3. Clic → **dropdown** qui liste tous les workspaces de l'OWNER (icône + nom + checkmark sur le courant), suivi d'un séparateur, suivi d'une option **« + Créer un workspace »** (visible **seulement si OWNER TEAM ou PRO**).
4. Clic sur « + Créer un workspace » → **`WorkspaceCreateDialogComponent`** s'ouvre en modal (Material `MatDialog`, taille moyenne, centré).
5. Le dialog affiche : (a) un champ texte **« Nom du workspace »** obligatoire, (b) un **sélecteur de plan** sous forme de deux cards cliquables et mutuellement exclusives — **TEAM** (libellé, prix, sièges max) et **PRO** (libellé, prix, sièges max) — réutilisant les données pricing F-123, (c) un bouton **« Créer + payer »** désactivé tant que nom + plan ne sont pas tous deux renseignés, (d) un bouton « Annuler ».
6. L'avocat saisit le nom, clique TEAM ou PRO, clique « Créer + payer ».
7. Frontend → `POST /api/v1/workspaces` avec `{ name, plan }` → backend crée le workspace en état **`PENDING_PAYMENT`** + crée la session Stripe Checkout + retourne `{ workspaceId, stripeCheckoutUrl }`.
8. Frontend **redirige le navigateur vers `stripeCheckoutUrl`** (page Stripe hors app — l'avocat saisit ses moyens de paiement chez Stripe).
9. Stripe traite le paiement → redirige vers `https://legalcase.fr/workspaces?workspace_created=success&workspace_id=<id>` (`success_url` configuré côté backend).
10. Le frontend (route handler `/workspaces`) détecte le query param → **bascule automatiquement** sur le nouveau workspace via le switcher (équivalent d'un clic sur ce workspace).
11. L'avocat atterrit sur le **dashboard du nouveau workspace** (écran vide — pas encore de dossier ni de membre). Une banderole d'état affiche :
    - **« Paiement en cours d'activation… »** si le webhook `customer.subscription.created` n'est pas encore reçu (workspace toujours `PENDING_PAYMENT`) — actions d'écriture désactivées (cf. invariant SF-156-00 §1).
    - **« Bienvenue dans <nom workspace> — invitez vos premiers membres »** dès que le webhook a bascule le workspace à `ACTIVE`.
12. **État terminal** : l'avocat travaille dans le nouveau workspace, peut inviter des membres, importer ses premiers dossiers, etc. — exactement comme son workspace principal.

## État terminal du processus

**L'avocat est sur le dashboard du nouveau workspace, banderole de bienvenue affichée, prêt à inviter ses membres ou créer son premier dossier.** L'ancien workspace reste accessible à tout moment via le switcher (re-clic + sélection).

Le processus est **fermé** au moment où la banderole de bienvenue s'affiche (workspace `ACTIVE`). Avant ce point, l'avocat est dans un état transitoire `PENDING_PAYMENT` qui doit être visible et lever toute ambiguïté (cf. invariants ci-dessous).

## Cartographie écrans / zones existants ↔ parcours

| # | Étape parcours | Écran / zone LegalCase | Statut |
|---|---|---|---|
| 1-2 | Header avec workspace switcher | `AppHeaderComponent` + `WorkspaceSwitcherComponent` (F-154) | ✅ existant |
| 3 | Dropdown switcher avec liste workspaces + option Créer | `WorkspaceSwitcherComponent` template (`mat-menu` ou similaire) | ✅ existant — **F-156 ajoute la condition de visibilité de l'option « + Créer »** |
| 4-6 | Dialog de création | `WorkspaceCreateDialogComponent` | ✅ existant — **F-156 ajoute le sélecteur de plan obligatoire (cards TEAM/PRO) + désactivation FREE/SOLO** |
| 7 | POST /workspaces backend + état `PENDING_PAYMENT` | `WorkspaceController.createWorkspace` | ✅ existant — **F-156 ajoute la gate plan + l'état `PENDING_PAYMENT`** (à confirmer en mini-spec backend si l'état existe déjà) |
| 8-9 | Stripe Checkout externe + redirect retour | Intégration Stripe (`StripeCustomerService` + `StripeCheckoutSession`) | ✅ existant (réutilisé de l'onboarding initial) |
| 10 | Bascule auto sur nouveau workspace via query param | Handler route `/workspaces` ou guard | ⚠️ **à vérifier en mini-spec frontend** — si existant via l'onboarding initial, réutiliser ; sinon ajouter |
| 11 | Banderole d'état `PENDING_PAYMENT` / banderole de bienvenue | À ajouter (composant `WorkspaceStatusBanner` ou équivalent) | 🟡 **nouveau** — petite composante d'état réactif sur `workspace.status` |
| 12 | Dashboard nouveau workspace, invitations, dossiers | Toutes les features existantes | ✅ existant |

## Position candidate de la feature

- **Écran** : le workspace switcher (header) + le dialog modal `WorkspaceCreateDialogComponent`.
- **Zone** : dropdown du switcher (visibilité de l'option « + Créer ») + cards plan dans le dialog.
- **Points d'entrée** : un seul — le switcher du header. Aucune autre route ni autre écran ne propose la création de workspace (et c'est bien : un seul point d'entrée naturel évite la dispersion).

## Challenge placement

**Question** : *l'écran / zone candidate correspond-il à l'étape du parcours où l'avocat a réellement besoin de la feature ?*

**Réponse** : OUI. Le switcher est le seul point logique pour gérer les workspaces — c'est l'écran où l'avocat consulte la liste de SES workspaces et bascule entre eux. Ajouter / créer en fait naturellement partie. Le dialog modal est le pattern Material standard pour une action de création courte (vs un écran dédié `/workspaces/new` qui serait du surinvestissement pour un formulaire à 2 champs).

**Aucun ajustement de placement requis.**

## Challenge lisibilité de la séquence

**Question** : *l'UI rend-elle visible l'ordre des étapes ?*

**Réponse** : OUI, séquence linéaire évidente :

```
switcher → dropdown → clic "Créer" → dialog (nom + plan) → Stripe (externe) → retour → switch auto → dashboard nouveau workspace
```

L'avocat ne peut pas se perdre — chaque étape mène à la suivante sans branchement subi.

**Point d'attention** : le passage 8→9→10 (sortie Stripe, redirect, switch auto) doit être **fluide et transparent** — pas de flash d'écran vide, pas de demande d'authentification re-jouée. Le `success_url` Stripe doit emmener directement sur l'app avec le workspace pré-bascule.

**Aucun ajustement de lisibilité requis** — la séquence est déjà claire.

## Challenge charge écran

**Question** : *quelle est la densité TOTALE de l'écran cible APRÈS ajout ?*

**Switcher dropdown (zone 1)** :

| Bloc | Avant F-156 | Après F-156 |
|---|---|---|
| Liste workspaces OWNER | N items | N items (inchangé) |
| Séparateur | 1 | 1 (inchangé) |
| Option « + Créer un workspace » | absente / présente sans gate | présente conditionnellement (TEAM/PRO) |

Charge : **inchangée** pour TEAM/PRO (juste un libellé qui apparaît) ; **allégée** pour FREE/SOLO (l'option ne s'affiche plus).

**Dialog `WorkspaceCreateDialogComponent` (zone 2)** :

| Bloc | Avant F-156 | Après F-156 |
|---|---|---|
| Titre dialog | 1 | 1 |
| Champ « Nom du workspace » | 1 | 1 |
| **Sélecteur de plan (cards TEAM / PRO)** | absent | **+1 bloc** (2 cards côte à côte) |
| Boutons « Créer + payer » / « Annuler » | 2 | 2 |

Charge : passe de 4 à 5 blocs primaires. Reste **largement sous le seuil de surcharge** (le seuil empirique est ~7-8 blocs primaires pour un dialog Material centré).

**Cas mobile** : les 2 cards plan doivent **s'empiler verticalement** sur écran < 600 px (responsive). Sinon le dialog déborde.

**Aucune surcharge** — la charge est très en-deçà du seuil.

## Challenge état final / continuité

**Question** : *après l'output de la feature, que fait l'avocat ?*

**Réponse** : l'état terminal est **explicite et nommé** (cf. section « État terminal » plus haut) — l'avocat est sur le dashboard du nouveau workspace, prêt à inviter des membres ou créer son premier dossier. Continuité **fluide** vers les actions naturelles suivantes (inviter, importer) qui sont déjà gérées par les features existantes.

**Cas d'échec** : si Stripe échoue (paiement refusé, abandon), Stripe redirige vers `cancel_url` configuré — le frontend doit traiter ce cas : retour sur l'écran d'origine + snackbar « Création annulée — aucun frais prélevé ». Le workspace `PENDING_PAYMENT` créé en step 7 doit être nettoyé (cf. invariant SF-156-00 §2 — politique d'échec 24 h ou explicite). Pas de dead-end.

**Cas FREE/SOLO** (tentative d'accès par URL bricolée) : le 403 backend déclenche une **page d'erreur dédiée** (route `/workspaces/new` ou équivalent) avec :
- Titre : « Création d'un workspace supplémentaire »
- Sous-titre : « Cette fonctionnalité nécessite un abonnement TEAM ou PRO »
- CTA : « Découvrir les plans » → ouvre **`/pricing`** en nouvelle page (pas de modal qui couvrirait le contexte).

Pas de retour silencieux ni de 403 brut.

## Ajustements IA requis

**Aucun ajustement de placement / séquence / charge** — la feature s'insère naturellement.

**Précisions à porter dans les mini-specs** :

1. **Mini-spec frontend (SF-156-02)** doit spécifier : (a) la condition de visibilité de l'option « + Créer » dans le switcher dropdown, (b) le composant `WorkspaceStatusBanner` pour les états `PENDING_PAYMENT` / `ACTIVE` post-création, (c) la page d'erreur dédiée FREE/SOLO, (d) le handler du query param `workspace_created=success` pour le switch auto, (e) le comportement responsive du dialog (empilement vertical < 600 px).
2. **Mini-spec backend (SF-156-01)** doit spécifier : (a) la gate dans `WorkspaceController.createWorkspace`, (b) l'éventuelle migration Liquibase si l'état `PENDING_PAYMENT` est nouveau, (c) la politique de timeout 24 h et le cron de nettoyage des `PENDING_PAYMENT` orphelins, (d) le `success_url` et `cancel_url` Stripe.

## Invariants anti-surcharge pour la mini-spec

1. **Dropdown switcher** : le dropdown ne doit pas dépasser **~7 workspaces affichés sans scroll** — au-delà, scroll vertical interne (cas rare mais à prévoir pour les PRO multi-cabinets).
2. **Dialog responsive** : les 2 cards plan TEAM/PRO s'empilent verticalement sous **600 px** de largeur, restent côte à côte au-dessus.
3. **Banderole d'état** : `WorkspaceStatusBanner` est une bande fine (~40-48 px) en haut du contenu — **pas de modal**, **pas de toast**. Persistante tant que `PENDING_PAYMENT`, auto-dismissée après 5 s en mode `ACTIVE` (« Bienvenue »).
4. **Pas de snackbar** pour l'état de paiement — le snackbar disparaît, la banderole reste. L'avocat doit voir d'un coup d'œil si son workspace est `PENDING_PAYMENT` ou `ACTIVE`.
5. **Page d'erreur FREE/SOLO** : CTA « Découvrir les plans » ouvre `/pricing` **en nouvelle page** (pas un modal qui couvrirait le contexte de l'app).
6. **Aucun nouvel écran** introduit par la feature — toute l'UX vit dans le switcher dropdown + le dialog existant + la banderole d'état. Si la mini-spec frontend introduit un nouvel écran dédié, c'est un signal de surcharge à challenger.

## Décision finale

**GO** — l'insertion écran de F-156 est cohérente. La mini-spec frontend (SF-156-02) intègre les 6 invariants anti-surcharge ci-dessus.

## MAJ apportée au parcours écran de référence

Le parcours écran « création d'un workspace additionnel » reconstruit ci-dessus est ajouté à **`docs/business/parcours-ecran-cabinet.md`** sous une nouvelle section « Création d'un workspace supplémentaire (multi-entité) » — voir le commit associé pour le diff exact du référentiel.
