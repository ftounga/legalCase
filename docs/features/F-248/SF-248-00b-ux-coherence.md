# F-248 — Cadrage cohérence écran (étape 0 bis)

Skill : `ai-skills/screen-coherence-challenger.md` · Date : 2026-05-19.

## Verdict : **GO avec ajustements**

## Intention métier + comportement visible attendu

Permettre à l'avocat de ne plus recevoir les emails non-transactionnels. **Visible** : un lien « Se désinscrire » dans le pied des emails non-transactionnels ; un clic ouvre une page publique `/unsubscribe?token=…` qui confirme l'action et permet aussi de se réabonner.

## Rappel verdict feature-coherence-challenger (étape 0)

**GO** — `docs/features/F-248/SF-248-00-coherence.md`. La feature comble la fin du cycle de communication email ; mise en conformité légale RGPD/LCEN.

## Parcours écran réel de l'avocat

> Source : routes publiques réellement codées (`app.routes.ts` : `verify-email`, `reset-password`, `share/:token`) + templates `EmailService` + pratique standard.

F-248 n'appartient ni au parcours dossier ni au parcours cabinet : c'est un **parcours déclenché depuis la boîte mail**, hors application.

1. L'avocat reçoit un email non-transactionnel (tip d'onboarding F-73 ou newsletter) dans sa messagerie — hors LegalCase.
2. Il lit l'email ; en pied de page figure un lien « Se désinscrire ».
3. Il clique → le navigateur ouvre `/unsubscribe?token=…`.
4. La page publique (hors shell applicatif, sans login) traite le token et affiche une confirmation : « Vous êtes désinscrit des emails d'information ».
5. La page précise que les emails liés à ses dossiers (transactionnels) continuent d'être envoyés.
6. (bidirectionnel) Si l'utilisateur était déjà désinscrit, la même page propose « Me réabonner ».
7. L'avocat ferme l'onglet.

## État terminal du processus

La **page de confirmation `/unsubscribe`** est l'état terminal du parcours. Le parcours est volontairement court et fermé — l'avocat n'a pas vocation à entrer dans l'application depuis ce lien. Un CTA secondaire discret « Retour à LegalCase » est acceptable, non obligatoire.

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours écran | Écran / zone LegalCase | Statut |
|---|---|---|
| Email reçu | template `EmailService` (F-73 / newsletter) | ✅ existant — ❌ le lien de désinscription |
| Page `/unsubscribe` | nouvelle route publique top-level | ❌ manquant |
| Réactivation in-app (toggle préférences) | écran de préférences utilisateur | ❌ **n'existe pas** |

## Position candidate de la feature

- **Lien de désinscription** : pied de page des templates d'emails **non-transactionnels** (séquence onboarding F-73 + newsletter).
- **Page de confirmation** : nouvelle route publique top-level `/unsubscribe`, sans `authGuard`, hors `ShellComponent` — pattern **strictement identique** aux routes publiques token-based déjà présentes dans `app.routes.ts` (`verify-email`, `reset-password`, `share/:token`).
- **Toggle réactivation in-app** : aucun écran hôte — les écrans `/workspace/*` sont de niveau workspace, **il n'existe aucune page profil / préférences de niveau utilisateur**.

## Challenge placement

- **Lien email + page `/unsubscribe`** : ✅ placement canonique, identique aux 3 routes publiques token-based existantes. Aucun écran applicatif impacté.
- **Toggle réactivation in-app** : ❌ pas d'écran hôte. La note PRODUCT_SPEC F-248 prévoyait un toggle « dans l'écran préférences » — cet écran n'existe pas. **Ajustement requis** (cf. A2).

## Challenge lisibilité de la séquence

Le parcours email → page est linéaire et trivial (une seule étape réelle). Aucune séquence multi-étapes à rendre lisible. ✅

## Challenge charge écran

La page `/unsubscribe` est une **page autonome mono-message** (comme `verify-email`) — zéro surcharge d'un écran existant. **Aucun écran applicatif n'est densifié** par F-248. ✅

## Challenge état final / continuité

La page de confirmation est l'état terminal assumé. Le seul point demandant une continuité in-app était la **réactivation**. **Ajustement** : rendre la page `/unsubscribe` **bidirectionnelle** — selon l'état `marketing_emails_opted_out` lu à partir du token, elle affiche soit « Se désinscrire » soit « Me réabonner ». Le token étant stable par utilisateur, le même lien (dans un email conservé) fonctionne dans les deux sens. La réactivation est ainsi couverte **sans créer d'écran de préférences**.

## Ajustements IA requis

- **A1** — La page `/unsubscribe` est **bidirectionnelle** : désinscription ou réabonnement selon l'état courant du token. Couvre la réactivation.
- **A2** — **Retirer du périmètre V1 le toggle in-app « Emails d'information »** annoncé dans la note PRODUCT_SPEC F-248 : aucun écran de préférences utilisateur n'existe ; en créer un pour un seul toggle serait disproportionné. ⚠️ **Substitution explicite, pas réduction de scope** — le besoin « pouvoir se réabonner » reste intégralement couvert par A1 (page publique bidirectionnelle). Si un véritable écran de préférences utilisateur émerge (V2), le toggle pourra y être ajouté — à backloguer si signal. La note PRODUCT_SPEC F-248 est mise à jour en conséquence.
- **A3** — La page publique reste hors `ShellComponent`, sans `authGuard` — ne jamais router le désabonnement derrière le login (invariant 3 de l'étape 0).

## Invariants anti-surcharge pour la mini-spec

1. La page `/unsubscribe` reste une **page autonome mono-message** (pattern `verify-email`) — pas de blocs multiples.
2. Aucun écran applicatif existant n'est densifié par F-248.
3. Le parcours email → page → confirmation est fermé en ≤ 2 écrans — pas de ping-pong vers l'application.
4. Réactivation = même page, même token, sens inverse — **pas de nouvel écran**.

## Décision finale

**GO avec ajustements.** Le placement de la page publique est canonique et sans surcharge. Ajustements A1-A3 à intégrer dans la mini-spec — notamment A2 qui substitue la page bidirectionnelle au toggle in-app initialement prévu. Enchaîner l'étape 1.

## MAJ apportée au parcours écran de référence

`docs/business/parcours-ecran-cabinet.md` enrichi : ajout d'une note sur le **parcours périphérique de désinscription email** (email → page publique `/unsubscribe`), distinct du parcours cabinet applicatif.
