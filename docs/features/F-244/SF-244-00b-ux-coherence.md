# F-244 — Cadrage cohérence écran (étape 0 bis)

> Produit via la skill `ai-skills/screen-coherence-challenger.md`.
> **Dérivé de l'audit** `docs/audits/AUDIT-2026-05-15-ux-coherence-detail-dossier.md` (audit rétrospectif `screen-coherence-challenger` du 2026-05-15, qui couvre déjà l'intégralité de l'analyse écran). Ce document est le cadrage 0 bis **formel de F-244** : il rattache l'audit au périmètre de la feature. Pour le détail des 4 challenges, du parcours écran et de la cartographie, se référer à l'audit.

## Verdict : GO avec ajustements

## Intention métier + comportement visible attendu

Réorganiser l'écran `case-file-detail` pour que l'avocat enchaîne sans friction analyse → simulation décisionnelle → arbitrage. Comportement visible : un détail dossier structuré en onglets par phase, une activité décisionnelle (outils + verdicts) cohérente, une séquence lisible, un écran non saturé.

## Rappel verdict feature-coherence-challenger (étape 0)

**GO** — voir `docs/features/F-244/SF-244-00-coherence.md`. Aucun trou fonctionnel amont ; dépendance aval (F-98) au backlog. Pré-requis pour cette étape 0 bis : satisfait.

## Parcours écran réel de l'avocat + état terminal

Reconstruit dans l'audit du 2026-05-15 et persisté dans le référentiel `docs/business/parcours-ecran-dossier.md` (source : ⚠ hypothèse à valider auprès d'un avocat + signaux terrain Renversez 13/05 / Mengue 11/05).

**État terminal** : ⚠ non matérialisé à ce jour. Sa matérialisation (action « générer les conclusions » / clôture) relève de l'ajustement 4 de l'audit — **hors périmètre F-244**, rattaché à **F-98**. F-244 réserve seulement l'emplacement.

## Cartographie écrans / zones ↔ parcours

Voir l'audit, section « Cartographie écrans / zones ↔ parcours ». Écran cible : `case-file-detail` (`/case-files/:id`), 2 colonnes, ~16 régions UI.

## Position de la feature

F-244 porte les **ajustements 1, 2, 3 et 5** de l'audit. Il n'introduit aucune zone nouvelle — il restructure l'agencement des zones existantes (étapes 5-6-7 du parcours + lien vers l'étape 3).

## Challenges (synthèse — détail dans l'audit)

| Challenge | Constat | Couvert par F-244 |
|---|---|---|
| Placement | Couplage saisie → verdict fragile (split col-left / col-right sans alignement ni découvrabilité garantis) | ✅ ajustement 1 |
| Lisibilité de la séquence | Le `case-dashboard-stepper` omet synthèse / outils / tableau de bord | ✅ ajustement 2 |
| Charge écran | ~16 régions sur une route — écran saturé | ✅ ajustement 5 |
| État final / continuité | Tableau de bord en cul-de-sac, pas d'étape suivante | ⚠️ ajustement 4 — **hors périmètre F-244** → F-98 (F-244 réserve l'emplacement) |

## Ajustements IA requis (périmètre F-244)

1. **Fiabiliser le couplage saisie → verdict** — modèle colonne d'entrée / colonne de sortie conservé ; garantir alignement vertical + signal de découvrabilité.
2. **Rendre la séquence lisible** — `case-dashboard-stepper` étendu à Synthèse → Outils → Tableau de bord.
3. **Reconnecter synthèse et outils** — point d'entrée bidirectionnel.
5. **Plafonner la charge + structurer en onglets** (Dossier / Analyse / Décision / Suivi), avec sous-règle pré-remplissage IA : badge `auto_awesome` agrégé au niveau de l'onglet « Décision ».

*Hors périmètre* : ajustement 4 (état terminal) → F-98.

## Invariants anti-surcharge pour la mini-spec

Repris de l'audit (section « Invariants anti-surcharge ») + des 5 invariants anti-gadget de l'étape 0 (`SF-244-00-coherence.md`). Points durs :
- Aucun nouveau bloc primaire autonome sur `/case-files/:id` sans en retirer ou regrouper un autre.
- Saisie d'un outil et son verdict visibles ensemble (pas de couplage hors champ de vision).
- Un onglet / une section repliable ne masque jamais un signal de pré-remplissage IA — le compteur remonte au conteneur.
- Tout bloc du parcours déclare sa position dans la séquence ; tout bloc terminal expose l'étape suivante.

## Décision finale

**GO avec ajustements.** Périmètre retenu : ajustements 1, 2, 3, 5. Découpage pressenti 4 SF (cf. ligne F-244 du `PRODUCT_SPEC.md`). Étape suivante : mini-spec SF-244-01 (structure en onglets — socle).

## MAJ apportée au parcours écran de référence

Aucune nouvelle MAJ par ce document : le référentiel `docs/business/parcours-ecran-dossier.md` a déjà été enrichi par l'audit du 2026-05-15 (ligne d'historique « Audit outils décisionnels »).
