# F-DT-38 — Cadrage cohérence écran (étape 0 bis)

**Date** : 2026-05-20
**Skill appliquée** : `ai-skills/screen-coherence-challenger.md`

---

## Verdict : GO

F-DT-38 est un **outil décisionnel** : il suit le pattern écran ultra-standardisé des ~35 outils F-DT existants. Placement, séquence, charge et continuité sont déjà cadrés par l'architecture du panel décisionnel — aucun ajustement requis.

## Intention métier + comportement visible attendu

L'avocat dispose, dans le panel des outils décisionnels du dossier, d'une section « Rupture de période d'essai (FR) » : un formulaire pré-rempli depuis l'analyse IA (dates de début et fin de contrat, durée d'essai contractuelle, motif, état de santé, présence d'un renouvellement, convention collective…), qui rend un verdict 4 niveaux après calcul : `REGULIERE` / `RISQUE_ABUSIVE` / `NULLE` / `ILLEGALE_REQUALIF_LICENCIEMENT`.

## Rappel verdict feature-coherence-challenger (étape 0)

GO sans ajustements (`SF-DT-38-00-coherence.md`) — situation métier strictement distincte, pré-fill IA exhaustif obligatoire (F-246).

## Parcours écran réel de l'avocat

Source : `docs/business/parcours-ecran-dossier.md` (référentiel établi et affiné). Position de F-DT-38 = **étape 8 du parcours : outils décisionnels** (`app-decisional-tools-panel`, colonne gauche, bas de l'écran détail dossier).

## État terminal du processus

Inchangé / hérité — la consultation des verdicts décisionnels reste l'état terminal de fait (le trou « état terminal explicite » sera traité au cadrage écran de F-98). F-DT-38 ne le modifie pas.

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours | Zone | Statut |
|---|---|---|
| Outils décisionnels | `app-decisional-tools-panel` | ✅ existant — F-DT-38 y ajoute une section |
| Tuile dashboard outil décisionnel | `app-case-dashboard` (tile) | ✅ existant — F-DT-38 y ajoute une tuile via TOOL_REGISTRY |

## Position candidate de la feature

Section `rupture-periode-essai-section` dans `app-decisional-tools-panel`, enregistrée dans `TOOL_REGISTRY` (`F-DT-38-rupture-periode-essai`), visibilité **contextuelle** : affichée uniquement si une rupture pendant la période d'essai est détectée dans le dossier (flag IA `rupture_periode_essai_detectee` ou indicateur `type_rupture` + ancienneté < durée essai).

## Challenge placement

✅ Standard. Tous les outils décisionnels de rupture/licenciement (F-DT-08, F-DT-10, F-DT-16, F-DT-36…) vivent dans ce panel. L'avocat y est déjà à l'étape « identification des moyens en droit ». F-DT-38 rejoint ses voisins naturels.

## Challenge lisibilité de la séquence

✅ Le panel décisionnel vient après l'analyse IA — séquence portée par le `app-case-dashboard-stepper`. F-DT-38 n'introduit aucune rupture.

## Challenge charge écran

✅ Le panel contient ~35 outils, mais le moteur de visibilité conditionnelle (F-IA-04) n'affiche que les outils pertinents pour le dossier courant. F-DT-38 étant **contextuel** (visible si rupture d'essai détectée), il n'alourdit pas les dossiers sans période d'essai. Aucun nouveau bloc primaire — F-DT-38 densifie un bloc existant déjà conçu pour accueillir N outils filtrés.

⚠️ Précision : F-DT-38 cohabite avec F-DT-08 (validité licenciement) — mais sur un dossier de rupture d'essai, F-DT-08 ne sera **pas** affiché (gating IA `type_rupture` = LICENCIEMENT). Si le dossier comporte une **requalification** (essai irrégulier → licenciement), les deux outils peuvent s'afficher : F-DT-38 rend le verdict `ILLEGALE_REQUALIF_LICENCIEMENT` qui ouvre la porte à F-DT-08. Pas de surcharge — l'avocat lit séquentiellement.

## Challenge état final / continuité

✅ Le verdict de F-DT-38 alimente la synthèse décisionnelle (`app-case-dashboard`) et les pistes stratégiques (F-176) — chaînage existant des outils décisionnels. Pas de dead-end.

## Ajustements IA requis

Aucun — placement standard.

## Invariants anti-surcharge pour la mini-spec

1. Visibilité **contextuelle** obligatoire (flag IA `rupture_periode_essai_detectee` ou équivalent) — ne jamais afficher F-DT-38 sur un dossier sans période d'essai.
2. Section conforme au pattern des outils décisionnels existants (pas de mise en page ad hoc).
3. Tuile dashboard via TOOL_REGISTRY — pas d'écran dédié.
4. Verdict 4 niveaux avec banner color-coded (RÉGULIÈRE = navy / RISQUE_ABUSIVE = or / NULLE = rouge avec mention réintégration / ILLEGALE = rouge).

## Décision finale

**GO.** F-DT-38 suit le pattern écran standard des outils décisionnels. Mini-spec : section `rupture-periode-essai-section` dans le panel, tuile dashboard via TOOL_REGISTRY, visibilité contextuelle. Aucun ajustement de placement.

## MAJ apportée au parcours écran de référence

Aucune — F-DT-38 s'inscrit dans une zone (`app-decisional-tools-panel`) déjà cartographiée dans `docs/business/parcours-ecran-dossier.md`. Pas de nouvelle zone, pas de nouveau parcours.

---

## Liens
- `docs/features/F-DT-38/SF-DT-38-00-coherence.md` — cadrage fonctionnel
- `docs/business/parcours-ecran-dossier.md` — référentiel parcours écran
- `ai-skills/screen-coherence-challenger.md` — skill appliquée
