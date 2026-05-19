# F-247 — Cadrage cohérence écran (étape 0 bis)

Skill : `ai-skills/screen-coherence-challenger.md` · Date : 2026-05-19.

## Verdict : **GO**

## Intention métier + comportement visible attendu

Permettre à l'OWNER de résilier son abonnement payant lui-même. **Visible** : dans l'écran Abonnement (`/workspace/billing`), une section permet de résilier ; quand une résiliation est programmée, un bandeau en haut de l'écran le signale avec une action « Réactiver ».

## Rappel verdict feature-coherence-challenger (étape 0)

**GO** — `docs/features/F-247/SF-247-00-coherence.md`. La feature comble le seul trou du cycle de vie de l'abonnement ; amont et aval entièrement couverts.

## Parcours écran réel de l'avocat (écran Abonnement)

> Source : écran réellement codé `workspace-billing.component.html` + `docs/business/parcours-ecran-cabinet.md` + pratique standard SaaS.

1. L'avocat ouvre la rubrique GESTION → Abonnement (`/workspace/billing`) depuis le menu latéral.
2. Il voit l'en-tête « Abonnement » + sous-titre.
3. (conditionnel) bandeau « Votre essai gratuit a expiré ».
4. Il voit la grille des 3 plans (SOLO / TEAM / PRO), son plan courant marqué « Plan actuel ».
5. Section « Acheter des tokens supplémentaires ».
6. (conditionnel) section « Utilisateurs actifs » (seats).
7. Section « Acheter des pages OCR supplémentaires ».
8. **Aujourd'hui : aucun moyen de résilier** — l'écran ne propose que de l'upgrade et de l'achat.

## État terminal du processus

L'écran Abonnement est un écran de **réglage persistant**, sans état terminal unique. Pour la résiliation spécifiquement, deux états successifs : (a) « résiliation programmée » → bandeau « Abonnement actif jusqu'au JJ/MM · Réactiver » ; (b) après l'échéance → workspace en plan FREE, la grille de plans réaffiche FREE comme plan courant + CTA upgrade.

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours écran | Écran / zone LegalCase | Statut |
|---|---|---|
| Accès à l'abonnement | rubrique GESTION → Abonnement | ✅ existant |
| Écran Abonnement (plans, achats, seats) | `/workspace/billing` | ✅ existant |
| **Section résiliation** | **F-247** | ❌ manquant |
| **Bandeau « résiliation programmée »** | **F-247** | ❌ manquant |

## Position candidate de la feature

- **Section résiliation** : écran `/workspace/billing`, **bas de page**, après les sections d'achat. Visible uniquement si plan payant actif ET rôle OWNER.
- **Bandeau « résiliation programmée »** : **haut** de l'écran, même pattern que le `expired-banner` existant.
- **Point d'entrée** : rubrique GESTION → Abonnement, déjà présente dans le menu — aucun nouveau point d'entrée à créer.

## Challenge placement

✅ **Correct.** L'abonnement se gère sur l'écran Abonnement — c'est là que l'avocat se rend quand il veut résilier. Aucun autre écran candidat. La résiliation est l'opposé fonctionnel de l'upgrade ; les deux cohabitent naturellement sur le même écran, comme dans tout SaaS.

## Challenge lisibilité de la séquence

L'écran billing n'est pas un parcours séquentiel multi-étapes — c'est une page de réglage. La résiliation est une action ponctuelle, pas une étape. **Ajustement mineur** : séparer visuellement l'action destructive (résilier) des CTA commerciaux (upgrade, acheter) — la placer en bas, dé-emphasée. L'état « résiliation programmée » doit en revanche être visible **dès l'ouverture sans scroll** → bandeau en haut.

## Challenge charge écran

Après ajout, l'écran porte ~5 blocs primaires permanents (en-tête, plans, tokens, OCR, **+ section résiliation**) et jusqu'à 3 blocs conditionnels (expired-banner, seats, **+ bandeau résiliation programmée**). La section résiliation est **compacte** (un libellé + un bouton ; un bandeau d'état). **Pas de surcharge significative** — aucun besoin d'onglet, de drawer ou d'écran dédié.

## Challenge état final / continuité

Clic « Résilier » → dialog de confirmation → résiliation programmée → bandeau « Abonnement actif jusqu'au JJ/MM · Réactiver ». L'avocat dispose d'une **sortie explicite** (réactiver) tant que la période court. Après l'échéance, l'écran billing affiche FREE comme plan courant + CTA upgrade. **Continuité assurée, pas de dead-end.** ✅

## Ajustements IA requis

- **A1** — Section résiliation en **bas** de l'écran billing, visuellement séparée des CTA commerciaux, action **dé-emphasée** (bouton stroked/texte, pas flat primary).
- **A2** — L'état « résiliation programmée » remonte en **bandeau haut de page** (réutiliser le pattern `expired-banner`), visible sans scroll, avec action « Réactiver ».
- **A3** — La section résiliation n'apparaît que si (plan payant actif) ET (rôle OWNER) — pas de bouton mort pour un membre simple ou un workspace FREE.

## Invariants anti-surcharge pour la mini-spec

1. La section résiliation reste un **bloc compact** (libellé + bouton), pas une carte riche.
2. Tout état de résiliation (programmée / réactivable) a un **point de sortie explicite** à l'écran.
3. Hiérarchie visuelle stricte : upgrade = action primaire, résilier = action tertiaire — la résiliation ne concurrence pas les CTA commerciaux.

## Décision finale

**GO.** Placement, séquence, charge et continuité tous cohérents. Ajustements A1-A3 (mineurs) à intégrer dans la mini-spec. Enchaîner l'étape 1.

## MAJ apportée au parcours écran de référence

`docs/business/parcours-ecran-cabinet.md` enrichi : l'écran Abonnement (`/workspace/billing`) porte désormais la résiliation self-service en plus de l'upgrade et de l'achat.
