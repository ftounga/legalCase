# F-215 — Cadrage cohérence écran (étape 0 bis)

**Date** : 2026-05-20
**Skill appliquée** : `ai-skills/screen-coherence-challenger.md`
**Feature parente** : `F-215` — P2 Immigration BE — ~10 outils décisionnels fréquence haute

---

## Verdict : GO

F-215 livre 10 **outils décisionnels** Immigration BE. Ils suivent le pattern écran ultra-standardisé des ~40+ outils décisionnels existants (dont 9 outils Immigration BE de F-209/F-203). Placement, séquence, charge et continuité sont déjà cadrés par l'architecture du panel décisionnel. Aucun ajustement écran requis.

---

## Intention métier + comportement visible attendu

Sur un dossier de droit des étrangers belge, l'avocat dispose, dans le panel des outils décisionnels (`app-decisional-tools-panel`), de 10 sections supplémentaires (single permit, regroupements 10bis/10ter, naturalisation art. 12bis et art. 16, AESM/MENA, recours CCE annulation/extrême urgence, Annexe 13quinquies + IE, protection temporaire Ukraine). Chacune est un formulaire de saisie rendant un verdict décisionnel (délais calculés, scoring d'éligibilité, checklist procédurale) après calcul.

---

## Rappel verdict étape 0

GO (`SF-215-00-coherence.md`) — ajustements = 2 nouveaux flags IA pour 10bis/10ter (SF-215-03/05), mode CONTEXTUAL pour tous les outils, sources juridiques à valider par avocat BE. Aucun impact sur le parcours écran.

---

## Parcours écran réel de l'avocat

Source : `docs/business/parcours-ecran-dossier.md`. Position de F-215 = **étape « outils décisionnels »** du parcours (`app-decisional-tools-panel`). Zone existante, déjà cartographiée — F-215 y ajoute 10 sections, comme F-209/F-203/F-208 avant lui.

---

## Cartographie écrans / zones ↔ parcours

| Étape parcours | Zone | Statut |
|---|---|---|
| Outils décisionnels | `app-decisional-tools-panel` | ✅ existant — F-215 y ajoute 10 sections |

---

## Challenges

- **Placement** : ✅ standard. Tous les outils décisionnels Immigration (FR et BE) vivent dans ce panel. F-215 rejoint ses voisins naturels, dont les 9 outils Immigration BE existants.
- **Lisibilité de la séquence** : ✅ le panel décisionnel vient après l'analyse IA — séquence portée par `app-case-dashboard-stepper`. Aucune rupture.
- **Charge écran** : ✅ les 10 outils F-215 sont tous `CONTEXTUAL` — ils ne s'affichent que si le flag IA correspondant est `true`. Sur un dossier immigration BE sans single permit ni recours CCE, le panel reste léger. Aucune surcharge. Sur workspace FR, **aucun** outil F-215 n'apparaît (gate `workspaceCountry === 'BELGIQUE'`).
- **Outils à fort formulaire** (single permit, regroupement 10bis/10ter) : structure en 2 colonnes existante dans le design system. Pas de nouvelle mise en page.
- **Calculateur délais CCE** (F-IM-31, F-IM-32) : affichage du délai calculé identique au pattern `F-IM-08-annexe13-be` — label date + badge statut urgence/disponible/expiré. Pattern éprouvé.
- **État final / continuité** : ✅ les verdicts alimentent la synthèse décisionnelle + F-176 — chaînage existant. Pas de dead-end.

---

## Invariants anti-surcharge pour les mini-specs

1. Gate `workspaceCountry === 'BELGIQUE'` strict : aucun outil F-215 visible sur un workspace FR (bannière info explicite si le composant est néanmoins monté — pas de masquage silencieux).
2. Sections conformes au pattern décisionnel existant — réutiliser les composants canoniques Immigration BE (F-209).
3. Mode de visibilité `CONTEXTUAL` seedé par outil dans `decision_tool_visibility_rules` — pas d'outil orphelin du moteur F-IA-04.
4. Badge pré-fill IA uniquement si champ réellement extractible (`getPrefillCount` parité stricte avec `prefillFromAi()` runtime).
5. Pour les outils sans champ extractible en V1 (`PREFILL_COUNT_ALWAYS_ZERO`), documenter explicitement dans la mini-spec — pas de dette masquée.

---

## Décision finale

**GO.** F-215 suit le pattern écran standard des outils décisionnels Immigration BE. Aucun ajustement de placement. Aucune nouvelle zone, aucun nouveau parcours. Le moteur de visibilité F-IA-04 assure la non-surcharge via les 10 triggers CONTEXTUAL.

---

## MAJ du parcours écran de référence

Aucune — F-215 s'inscrit dans une zone (`app-decisional-tools-panel`) déjà cartographiée.

---

## Liens
- `docs/features/F-215/SF-215-00-coherence.md` — cadrage fonctionnel (étape 0)
- `docs/business/parcours-ecran-dossier.md` — référentiel parcours écran
