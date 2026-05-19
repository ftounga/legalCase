# F-156 — Cadrage cohérence (étape 0)

> Produit par la skill `ai-skills/feature-coherence-challenger.md`.
> Date : 2026-05-19.

## Verdict : **GO**

Aucun trou fonctionnel amont ni aval. La feature s'insère sur un workflow métier réel (avocat opérant plusieurs entités), tous ses pré-requis sont livrés (workspace switcher F-154, pricing F-123, intégration Stripe), et sa sortie est exploitable immédiatement (le nouveau workspace est utilisable comme tout autre, via le switcher existant).

## Intention métier

Restreindre la création d'un workspace supplémentaire aux plans **TEAM** et **PRO** — empêcher la multiplication gratuite de workspaces tout en restant cohérent avec le modèle SOLO = mono-workspace par design.

## Workflow métier réel de l'utilisateur cible (avocat) — multi-workspace

> Source : pratique standard du barreau français + belge (⚠ hypothèse à valider auprès d'un avocat associé). Aucun référentiel `docs/business/workflow-multi-workspace.md` existant — à créer si la feature est livrée.

Cas d'usage couverts par le **besoin d'un workspace additionnel** :

1. L'avocat opère sur son workspace principal (cabinet A / structure principale).
2. Il prend en charge une **seconde entité** (cabinet B, activité de conseil indépendante, double inscription FR+BE, holding + filiale, SCP secondaire).
3. Il a besoin de **séparer les données** entre les deux entités : dossiers, clients, équipe, facturation, statistiques — la séparation est juridique (entités distinctes, secret professionnel cloisonné) et opérationnelle (équipes, rôles, accès).
4. Il crée un workspace additionnel pour la seconde entité.
5. Il invite des membres distincts (les associés de B ne sont pas ceux de A).
6. Il bascule entre A et B via le **workspace switcher** existant.
7. Il gère les dossiers et clients de chaque entité **indépendamment** — un dossier du cabinet B n'apparaît jamais dans le contexte du cabinet A.
8. Il facture / suit la consommation de chaque workspace **séparément** (chaque workspace a son propre abonnement Stripe).

État terminal du workflow : l'avocat utilise plusieurs workspaces actifs et les traverse via le switcher, avec une isolation totale entre eux.

## Cartographie features actuelles ↔ workflow

| # | Étape workflow métier | Feature LegalCase | Statut |
|---|---|---|---|
| 1 | Workspace principal existant | F-08 onboarding workspace initial | ✅ Livrée |
| 2-3 | Besoin de seconde entité (contexte métier — pas feature produit) | — | n/a |
| 4 | **Création workspace additionnel** | **F-156 (la feature challengée)** | 🟡 À faire |
| 4 bis | Sélection plan + paiement | F-123 (pricing SOLO/TEAM/PRO déployé prod) + intégration Stripe existante (`StripeCustomerService` + webhooks `customer.subscription.*`) | ✅ Livrée |
| 5 | Invitation des membres | F-WS workspace members / invitations | ✅ Livrée |
| 6 | Bascule entre workspaces | F-154 workspace switcher | ✅ Livrée |
| 7 | Gestion dossiers / clients par workspace | Toutes les features dossier (F-3/4/5 import + analyse, F-98 conclusions, outils décisionnels, etc.) — isolation `workspace_id` au niveau service / repository | ✅ Livrées (modèle multi-tenant canonique) |
| 8 | Facturation / consommation par workspace | F-billing — table `subscriptions` indexée par `workspace_id` | ✅ Livrée |

## Position de la nouvelle feature

F-156 s'insère **à l'étape 4** du workflow — la création d'un workspace supplémentaire. Toutes les étapes amont (workspace existant, plan model, pricing, Stripe) et aval (switcher, invitations, dossiers, facturation par workspace) sont **livrées**.

La feature ajoute **deux contraintes** sur cette étape 4 :

1. **Gate de visibilité** sur l'OWNER courant : option "Créer un workspace" visible **uniquement si OWNER ∈ {TEAM, PRO}**. FREE et SOLO ne la voient pas.
2. **Gate de validation** sur le plan choisi pour le nouveau workspace : **uniquement TEAM ou PRO** (rejet FREE, SOLO, null).

## Challenge amont

> *Chaque étape AVANT F-156 dans le workflow est-elle couverte par une feature livrée ou au backlog ?*

| Pré-requis amont | Couverture | Verdict |
|---|---|---|
| Le concept de **workspace** existe comme unité de tenancy | F-08 + modèle multi-tenant `workspace_id` partout (architecture canonique) | ✅ |
| Le **switcher** existe pour avoir plus d'un workspace par utilisateur | F-154 Terminée | ✅ |
| Les **plans** FREE / SOLO / TEAM / PRO existent | F-123 déployé prod 2026-04-XX | ✅ |
| **Stripe Checkout** est intégré pour le paiement initial d'un abonnement | `StripeCustomerService` + webhooks `customer.subscription.created` déjà branchés sur l'onboarding initial du workspace | ✅ |
| Le **modèle `Workspace`** supporte un état transitoire pré-paiement | ⚠️ **À vérifier** — la mini-spec backend devra confirmer si l'état `pending_payment` existe déjà ou doit être ajouté (`Workspace.status` ENUM `PENDING_PAYMENT`) + migration Liquibase associée |
| La **politique d'isolation workspace** s'applique automatiquement à tout nouveau workspace | `workspace_id` propagé via tous les services / repositories | ✅ |

**Conclusion amont** : aucun trou bloquant. Un seul point d'attention pour la mini-spec — l'état `pending_payment` est-il déjà supporté par le modèle ? À trancher dans SF-156-01.

## Challenge aval

> *La sortie de F-156 est-elle exploitable par les étapes AVAL du workflow ?*

| Sortie F-156 | Exploitée par | Verdict |
|---|---|---|
| Nouveau workspace créé en état `pending_payment` | UI doit afficher un état clair (banderole) + désactiver les actions d'écriture jusqu'à webhook (cf. invariant anti-gadget §1) | ✅ (avec invariant) |
| Workspace passé à `ACTIVE` après webhook `customer.subscription.created` | Switcher F-154 le liste, toutes les features dossier l'acceptent comme tout autre workspace | ✅ |
| Abonnement Stripe créé et lié à `workspace_id` | F-billing — table `subscriptions` indexée par `workspace_id`, dashboard de consommation par workspace | ✅ |
| Échec de paiement Stripe (webhook `*.failed` ou timeout 24 h) | ⚠️ **Politique de nettoyage** à définir dans la mini-spec — supprimer le workspace `pending_payment`, le geler, ou relancer ? (cf. invariant anti-gadget §2) |

**Conclusion aval** : exploitable, sous réserve de fixer la politique d'échec/nettoyage dans la mini-spec.

## STOPs / pré-requis à ajouter au backlog

**Aucun.** Toutes les briques amont sont livrées. La feature peut démarrer SF-156-01 / SF-156-02 immédiatement.

## Invariants anti-gadget pour la mini-spec

1. **L'état `pending_payment` n'est PAS un workspace utilisable.** L'UI doit afficher une banderole « Paiement en cours — accès limité » et **désactiver toute action d'écriture** (création de dossier, invitation, upload) sur ce workspace jusqu'à réception du webhook `customer.subscription.created`. Sinon, l'avocat crée du contenu qui sera perdu si le paiement échoue.

2. **Politique d'échec Stripe explicitement définie.** Si le webhook `customer.subscription.created` n'est pas reçu sous **24 h** (timeout) OU si `customer.subscription.deleted` / `invoice.payment_failed` arrive : le workspace `pending_payment` est **automatiquement supprimé** (ou marqué `CANCELLED` pour audit). Pas de workspace orphelin laissé en zombie.

3. **Idempotence du webhook.** Le handler `customer.subscription.created` doit être idempotent — un même `subscription.id` reçu deux fois n'active pas deux fois le workspace ni ne crée d'effet de bord.

4. **Messaging clair pour FREE et SOLO.** Quand un OWNER FREE ou SOLO tente de créer un workspace (s'ils trouvent l'endpoint par un autre chemin), le 403 backend doit s'accompagner d'un message frontend explicite — **« La création d'un workspace supplémentaire nécessite un abonnement TEAM ou PRO. [Découvrir les plans]→ »** avec lien vers la page pricing. Pas de 403 silencieux côté UI.

5. **Le nom F-156 « clone » ne désigne PAS une copie de données.** Aucune duplication de dossiers, membres, contenu d'un workspace existant vers le nouveau. Le nouveau workspace est créé **vide**. Toute évolution vers de la vraie duplication doit faire l'objet d'une feature distincte (le mot « clone » est conservé dans le titre PRODUCT_SPEC pour traçabilité historique, mais la description l'explicite).

6. **Isolation workspace renforcée.** Le test d'isolation existant doit être étendu pour vérifier qu'un OWNER avec workspace A actif **ne peut pas voir** les dossiers du workspace B nouvellement créé sans avoir explicitement basculé via le switcher. (Test de non-régression sur `workspace_id` cross-check — devrait passer trivialement vu l'architecture, mais à confirmer.)

## Décision finale

**GO** — la feature F-156 (version TEAM/PRO uniquement) est cohérente avec le workflow métier réel, ses pré-requis sont tous livrés, et sa sortie est immédiatement exploitable.

Pré-requis pour passer à l'étape 1 (mini-spec) :

- ✅ étape 0 bis cadrage écran à produire (impact écran sur le workspace switcher + dialog de création).
- Mini-specs SF-156-01 (backend) et SF-156-02 (frontend) à rédiger en respectant les 6 invariants anti-gadget ci-dessus.

**Statut PRODUCT_SPEC** : F-156 passe de `Backlog` à `À faire`.
