# F-248 — Cadrage cohérence (étape 0)

Skill : `ai-skills/feature-coherence-challenger.md` · Date : 2026-05-19 · Source : question utilisateur 2026-05-19 + audit codebase + obligation RGPD art. 21 / LCEN art. L34-5.

## Verdict : **GO**

## Intention métier (1 phrase)

Permettre à l'avocat de ne plus recevoir les emails non-transactionnels de LegalCase (séquence d'onboarding, newsletter) via un lien de désinscription conforme, tout en continuant de recevoir les emails transactionnels indispensables.

## Workflow métier réel de l'utilisateur cible (cycle de réception des emails)

> Source : pratique standard de la communication SaaS + obligations légales RGPD/LCEN — ⚠ hypothèse de bon sens. Ce n'est pas un acte juridique métier mais la relation de communication éditeur ↔ avocat.

1. L'avocat crée son compte → reçoit les premiers emails (vérification d'adresse, bienvenue).
2. Dans les jours suivants, il reçoit la **séquence d'onboarding** (tips J+2, J+5, relance avant expiration J+12, post-expiration J+15) — F-73.
3. Chaque mois, il reçoit la **newsletter** récapitulative — M-26.
4. En parallèle, il reçoit des **emails transactionnels** liés à son activité (analyse terminée, échec d'extraction, alerte de délai, invitation de membre…).
5. À un moment, il ne souhaite plus recevoir les emails d'information (volume jugé excessif, contenu non pertinent, simple préférence).
6. Il cherche un **lien de désinscription** dans le pied de l'email — réflexe universel, et mention légalement obligatoire.
7. Il clique → se désinscrit en un geste, sans avoir à se connecter.
8. Il ne reçoit plus que les emails transactionnels indispensables au suivi de ses dossiers.

## Cartographie features actuelles ↔ workflow

| Étape workflow métier | Feature(s) LegalCase | Statut |
|---|---|---|
| 1. Création compte + emails initiaux | F-02 onboarding, vérification email | ✅ Livrée |
| 2. Séquence d'onboarding (5 emails) | F-73 séquence email onboarding | ✅ Livrée |
| 3. Newsletter mensuelle | M-26 / `MonthlyNewsletterScheduler` | ✅ Livrée |
| 4. Emails transactionnels | F-11/F-39 notifications analyse, alertes délai, invitations | ✅ Livrée |
| 5. Décision de ne plus recevoir | — (préférence du client, hors produit) | n/a |
| 6. **Lien de désinscription dans l'email** | **F-248 (feature challengée)** | ❌ Manquante |
| 7. **Acte de désinscription (page publique)** | F-248 | ❌ Manquante |
| 8. Réception limitée aux transactionnels | F-248 (garde dans `EmailService`) | ❌ Manquante |

## Position de la nouvelle feature

F-248 s'insère aux **étapes 6, 7 et 8** — le trou complet de fin de chaîne. L'amont (des emails non-transactionnels sont effectivement envoyés) est entièrement couvert par F-73 et M-26, livrés et actifs en production.

## Challenge amont

**Question : chaque étape AVANT la désinscription est-elle couverte par une feature du produit ?**

- Étapes 1 à 4 : ✅ livrées. La brique amont strictement nécessaire est **« des emails non-transactionnels sont envoyés »** — assurée par F-73 (séquence onboarding) et M-26 (newsletter), tous deux en production. Sans eux, la feature de désinscription n'aurait pas d'objet ; ils existent.
- Aucun trou amont.

## Challenge aval

**Question : la sortie de la désinscription est-elle exploitable / cohérente avec l'aval ?**

- Étape 8 — après désinscription, l'avocat doit **continuer de recevoir les emails transactionnels** (analyse terminée, alerte de délai). Ces emails existent (F-11/F-39) et la feature doit explicitement les exclure de l'opt-out. ✅ exploitable, sous réserve de l'invariant 4 ci-dessous.
- Réactivation : un avocat désinscrit par erreur doit pouvoir revenir — couvert par le toggle de préférence côté connecté prévu en SF-248-02. ✅
- Aucun trou aval bloquant.

## STOPs / pré-requis à ajouter au backlog

Aucun. La feature est développable immédiatement. C'est par ailleurs une **mise en conformité légale** (RGPD art. 21 droit d'opposition, LCEN art. L34-5 opt-out obligatoire) : l'absence de la feature est une non-conformité active en production, pas un simple manque de confort.

## Invariants anti-gadget pour la mini-spec

1. **Lien de désinscription dans CHAQUE email non-transactionnel** — les 5 emails de la séquence F-73 + la newsletter. Un lien présent sur un seul type d'email laisse la non-conformité active sur les autres.
2. **Opt-out réellement respecté** — `EmailService.sendOnboarding*` et `sendMonthlyNewsletter` doivent court-circuiter l'envoi quand l'utilisateur est désinscrit. Un lien cosmétique qui n'arrête pas les envois maintient l'infraction.
3. **Désinscription sans authentification (one-click)** — la page `/unsubscribe?token=…` ne doit demander aucun login. Un avocat ne se souvient pas forcément de ses identifiants, et la conformité LCEN exige un opt-out simple et immédiat. Le token EST l'autorisation.
4. **Les emails transactionnels ne sont JAMAIS coupés** — vérification d'email, reset mot de passe, analyse terminée, échec d'extraction, alertes délai, invitations, confirmation de contact restent envoyés quel que soit l'opt-out. Couper une notification « analyse terminée » casserait le produit.
5. **Pas de lien de désinscription dans les emails transactionnels** — le désabonnement ne s'y applique pas ; y mettre le lien induirait l'avocat en erreur.
6. **Token non devinable et stable par utilisateur** — un UUID aléatoire, non énumérable, pour qu'un tiers ne puisse pas désinscrire un autre utilisateur.

## Décision finale

**GO.** La feature comble exactement la fin du cycle de communication email, l'amont est entièrement couvert, et il s'agit d'une mise en conformité légale prioritaire. Aucun pré-requis backlog. Statut PRODUCT_SPEC : `Backlog` → `À faire`. Enchaîner l'étape 0 bis (cohérence écran — la feature ajoute une page publique `/unsubscribe` et un toggle dans l'écran préférences) puis la mini-spec.
