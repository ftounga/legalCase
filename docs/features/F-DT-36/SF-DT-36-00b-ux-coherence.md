# F-DT-36 — Cadrage cohérence écran (étape 0 bis)

**Date** : 2026-05-17
**Skill appliquée** : `ai-skills/screen-coherence-challenger.md`

---

## Verdict : GO

F-DT-36 est un **outil décisionnel** : il suit le pattern écran ultra-standardisé des ~35 outils F-DT existants. Placement, séquence, charge et continuité sont déjà cadrés par l'architecture du panel décisionnel — aucun ajustement requis.

## Intention métier + comportement visible attendu

L'avocat dispose, dans le panel des outils décisionnels du dossier, d'une section « Nullités de procédure de licenciement » : un formulaire pré-rempli depuis l'analyse IA (dates de convocation, d'entretien, de notification…), qui rend un verdict de nullité après calcul.

## Rappel verdict feature-coherence-challenger (étape 0)

GO avec ajustements (`SF-DT-36-00-coherence.md`) — ajustement = frontière à clarifier avec F-DT-08, sans impact écran.

## Parcours écran réel de l'avocat

Source : `docs/business/parcours-ecran-dossier.md` (référentiel établi et affiné). Position de F-DT-36 = **étape 8 du parcours : outils décisionnels** (`app-decisional-tools-panel`, colonne gauche, bas de l'écran détail dossier).

## État terminal du processus

Inchangé / hérité — la consultation des verdicts décisionnels reste l'état terminal de fait (le trou « état terminal explicite » sera traité au cadrage écran de F-98). F-DT-36 ne le modifie pas.

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours | Zone | Statut |
|---|---|---|
| Outils décisionnels | `app-decisional-tools-panel` | ✅ existant — F-DT-36 y ajoute une section |

## Position candidate de la feature

Section `nullite-procedure-section` dans `app-decisional-tools-panel`, enregistrée dans `TOOL_REGISTRY`, visibilité **contextuelle** : affichée uniquement si un licenciement est détecté dans le dossier (gérée par le moteur F-IA-04, pattern identique à F-DT-08/16).

## Challenge placement

✅ Standard. Tous les outils décisionnels licenciement (F-DT-08, F-DT-16…) vivent dans ce panel. L'avocat y est déjà à l'étape « identification des moyens en droit ». F-DT-36 rejoint ses voisins naturels.

## Challenge lisibilité de la séquence

✅ Le panel décisionnel vient après l'analyse IA — séquence portée par le `app-case-dashboard-stepper`. F-DT-36 n'introduit aucune rupture.

## Challenge charge écran

✅ Le panel contient ~35 outils, mais le moteur de visibilité conditionnelle (F-IA-04) n'affiche que les outils pertinents pour le dossier courant. F-DT-36 étant **contextuel** (visible si licenciement détecté), il n'alourdit pas les dossiers non concernés. Aucun nouveau bloc primaire — F-DT-36 densifie un bloc existant déjà conçu pour accueillir N outils filtrés.

## Challenge état final / continuité

✅ Le verdict de F-DT-36 alimente la synthèse décisionnelle (`app-case-dashboard`) et les pistes stratégiques (F-176) — chaînage existant des outils décisionnels. Pas de dead-end.

## Ajustements IA requis

Aucun — placement standard.

## Invariants anti-surcharge pour la mini-spec

1. Visibilité **contextuelle** obligatoire (licenciement détecté) — ne jamais afficher F-DT-36 sur un dossier sans licenciement.
2. Section conforme au pattern des outils décisionnels existants (pas de mise en page ad hoc).

## Décision finale

**GO.** F-DT-36 suit le pattern écran standard des outils décisionnels. Mini-spec : section `nullite-procedure-section` dans le panel, visibilité contextuelle. Aucun ajustement de placement.

## MAJ apportée au parcours écran de référence

Aucune — F-DT-36 s'inscrit dans une zone (`app-decisional-tools-panel`) déjà cartographiée dans `docs/business/parcours-ecran-dossier.md`. Pas de nouvelle zone, pas de nouveau parcours.

---

## Liens
- `docs/features/F-DT-36/SF-DT-36-00-coherence.md` — cadrage fonctionnel
- `docs/business/parcours-ecran-dossier.md` — référentiel parcours écran
- `ai-skills/screen-coherence-challenger.md` — skill appliquée
