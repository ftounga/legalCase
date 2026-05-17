# F-217 — Cadrage cohérence écran (étape 0 bis)

**Date** : 2026-05-17
**Skill appliquée** : `ai-skills/screen-coherence-challenger.md`
**Feature parente** : `F-217` — P2 Famille BE — ~10 outils décisionnels

---

## Verdict : GO

F-217 livre 10 **outils décisionnels** : ils suivent le pattern écran ultra-standardisé des ~35 outils décisionnels existants (dont les 4 outils Famille BE de F-211). Placement, séquence, charge et continuité sont déjà cadrés par l'architecture du panel décisionnel — aucun ajustement écran requis.

## Intention métier + comportement visible attendu

Sur un dossier de droit de la famille belge, l'avocat dispose, dans le panel des outils décisionnels (`app-decisional-tools-panel`), de 10 sections supplémentaires (régime matrimonial, liquidation-partage, autorité parentale, contributions, successions, protection du majeur, reconnaissance mariage étranger, contestation de filiation). Chacune est un formulaire de saisie rendant un verdict décisionnel après calcul.

## Rappel verdict étape 0

GO avec ajustements (`SF-217-00-coherence.md`) — ajustements = mode de visibilité explicite par outil + pré-fill IA = 0, sans impact écran.

## Parcours écran réel de l'avocat

Source : `docs/business/parcours-ecran-dossier.md`. Position de F-217 = **étape « outils décisionnels »** du parcours (`app-decisional-tools-panel`). Zone existante, déjà cartographiée — F-217 y ajoute des sections, comme F-211 et F-DT-36 avant lui.

## Cartographie écrans / zones ↔ parcours

| Étape parcours | Zone | Statut |
|---|---|---|
| Outils décisionnels | `app-decisional-tools-panel` | ✅ existant — F-217 y ajoute 10 sections |

## Challenges

- **Placement** : ✅ standard. Tous les outils décisionnels Famille (FR et BE) vivent dans ce panel. F-217 rejoint ses voisins naturels, dont les 4 outils F-211.
- **Lisibilité de la séquence** : ✅ le panel décisionnel vient après l'analyse IA — séquence portée par `app-case-dashboard-stepper`. Aucune rupture.
- **Charge écran** : ✅ le moteur de visibilité F-IA-04 n'affiche que les outils pertinents pour le dossier courant (gate `legalDomain` + `country` + mode). Sur un workspace FR, **aucun** outil F-217 n'apparaît. Les outils `CATALOG` ne s'affichent que sur demande via le catalogue F-238. Pas de surcharge.
- **État final / continuité** : ✅ les verdicts alimentent la synthèse décisionnelle + F-176 — chaînage existant. Pas de dead-end.

## Invariants anti-surcharge pour les mini-specs

1. Gate `workspaceCountry` strict : aucun outil F-217 visible sur un workspace FR (bannière info si le composant est néanmoins monté).
2. Sections conformes au pattern des outils décisionnels existants — pas de mise en page ad hoc (réutiliser le pattern F-211).
3. Mode de visibilité explicite et seedé par outil (`ALWAYS_ON` / `CATALOG`) — ne jamais laisser un outil orphelin du moteur F-IA-04.

## Décision finale

**GO.** F-217 suit le pattern écran standard des outils décisionnels. Aucun ajustement de placement. Aucune nouvelle zone, aucun nouveau parcours.

## MAJ du parcours écran de référence

Aucune — F-217 s'inscrit dans une zone (`app-decisional-tools-panel`) déjà cartographiée.

---

## Liens
- `docs/features/F-217/SF-217-00-coherence.md` — cadrage fonctionnel (étape 0)
- `docs/business/parcours-ecran-dossier.md` — référentiel parcours écran
