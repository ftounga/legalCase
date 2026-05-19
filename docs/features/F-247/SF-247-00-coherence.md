# F-247 — Cadrage cohérence (étape 0)

Skill : `ai-skills/feature-coherence-challenger.md` · Date : 2026-05-19 · Source : question utilisateur 2026-05-19 + audit codebase.

## Verdict : **GO**

## Intention métier (1 phrase)

Permettre à l'avocat OWNER d'un workspace de mettre fin lui-même à son abonnement payant, sans passer par le support ni par une suppression destructive de son workspace.

## Workflow métier réel de l'utilisateur cible (cycle de vie d'un abonnement SaaS)

> Source : pratique standard SaaS B2B — ⚠ hypothèse de bon sens (pas de `docs/business/workflow-billing.md` existant). Le workflow décrit la relation contractuelle avocat ↔ éditeur, pas un acte métier juridique.

1. L'avocat découvre LegalCase (landing, démo, bouche-à-oreille).
2. Il crée un compte et un workspace — entre en plan FREE (trial 14 j).
3. Il teste l'outil pendant la période d'essai.
4. Il souscrit un plan payant (SOLO / TEAM / PRO) via checkout Stripe.
5. Il utilise l'outil ; il est facturé mensuellement de façon récurrente.
6. À un moment, il décide d'arrêter (fin de mission, contrainte budgétaire, changement d'outil, insatisfaction, cabinet en sommeil).
7. Il cherche à **résilier son abonnement** — il veut couper la facturation récurrente.
8. La résiliation prend effet ; il n'est plus prélevé.
9. Son workspace bascule en plan FREE (ou compte dormant) — ses données restent accessibles.
10. Éventuellement, il revient plus tard et re-souscrit.

## Cartographie features actuelles ↔ workflow

| Étape workflow métier | Feature(s) LegalCase | Statut |
|---|---|---|
| 1. Découverte | F-158 landing V3 | ✅ Livrée |
| 2. Création compte + workspace + trial | F-02 onboarding, F-19 trial 14 j | ✅ Livrée |
| 3. Essai | F-19 plan FREE trial | ✅ Livrée |
| 4. Souscription plan payant | F-19 checkout Stripe, F-123 repricing | ✅ Livrée |
| 5. Usage + facturation récurrente | F-16 gestion des abonnements, F-19, F-123 per-user | ✅ Livrée |
| 6. Décision d'arrêter | — (décision du client, hors produit) | n/a |
| 7. **Résiliation self-service** | **F-247 (feature challengée)** — aujourd'hui : seul F-25 (annulation Stripe super-admin, destructive) | ❌ Manquante |
| 8. Effet de la résiliation (fin de prélèvement) | F-247 | ❌ Manquante |
| 9. Retour en plan FREE, données conservées | F-16 (le plan FREE existe), F-38 (données du workspace) | ✅ Livrée |
| 10. Re-souscription ultérieure | F-19 checkout | ✅ Livrée |

## Position de la nouvelle feature

F-247 s'insère aux **étapes 7 et 8** du workflow — le seul trou ❌ de la chaîne. Tout l'amont (souscription, facturation) et tout l'aval (plan FREE de repli, conservation des données, re-checkout) sont déjà couverts par des features livrées.

## Challenge amont

**Question : chaque étape AVANT la résiliation est-elle couverte par une feature du produit ?**

- Étapes 1 à 5 : ✅ toutes livrées. La brique amont strictement nécessaire est **« avoir un abonnement payant actif »** — assurée par F-16 + F-19 + F-123, toutes Terminées. La table `subscriptions` et l'entité `Subscription` (avec `stripeSubscriptionId`) existent déjà.
- Aucun trou amont. La feature s'appuie sur une base réelle.

## Challenge aval

**Question : la sortie de la résiliation est-elle exploitable par les étapes aval ?**

- Étape 9 — après résiliation, le workspace doit retomber proprement sur un état valide. Le **plan FREE existe** (F-16) et `PlanLimitService` sait appliquer ses quotas. Les **données du workspace restent** (aucune suppression — F-247 ≠ F-25). ✅
- Étape 10 — re-souscription : le checkout Stripe (F-19) reste disponible depuis l'écran billing. ✅
- Aucun trou aval. La résiliation produit un état (« workspace FREE, données conservées ») que le produit sait déjà gérer.

## STOPs / pré-requis à ajouter au backlog

Aucun. Tous les pré-requis amont et aval sont livrés. F-247 est développable immédiatement.

## Invariants anti-gadget pour la mini-spec

1. **Résiliation réellement effective côté Stripe** — la mini-spec doit appeler `Subscription.update(cancel_at_period_end=true)` sur l'API Stripe, pas seulement positionner un flag en base. Une résiliation qui ne coupe pas le prélèvement réel est pire que pas de feature (le client croit avoir résilié et continue d'être facturé).
2. **Webhook `customer.subscription.deleted` branché et testé** — à l'échéance, le workspace doit repasser en FREE. Sans ce webhook, le client garderait l'accès payant gratuitement (perte de revenu) ou resterait facturé sans le savoir.
3. **Vrai self-service** — le bouton doit être atteignable par l'OWNER depuis l'écran billing sans aucune action support. Un parcours qui renvoie vers un email de contact n'est pas du self-service.
4. **Réactivation possible tant que la période court** — l'avocat qui résilie par erreur doit pouvoir annuler la résiliation programmée jusqu'au terme. Sinon une résiliation = un client perdu sur un faux clic.
5. **Résilier ≠ supprimer** — la résiliation ne supprime ni le workspace, ni les dossiers, ni les membres. Elle coupe la facturation et rétrograde le plan. La suppression destructive reste l'apanage de F-25 (super-admin).
6. **Gate OWNER strict** — un membre simple (rôle ≠ OWNER) ne doit pas pouvoir résilier l'abonnement du workspace.

## Décision finale

**GO.** La feature s'insère exactement sur le seul trou du cycle de vie de l'abonnement ; amont et aval sont entièrement couverts. Aucun pré-requis backlog. Statut PRODUCT_SPEC : `Backlog` → `À faire`. Enchaîner l'étape 0 bis (cohérence écran — la feature ajoute un élément visible dans `workspace-billing`) puis la mini-spec.
