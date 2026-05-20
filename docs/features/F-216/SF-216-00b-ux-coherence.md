# F-216 — Cadrage cohérence écran (étape 0 bis)

**Date** : 2026-05-20
**Skill appliquée** : `ai-skills/screen-coherence-challenger.md`
**Feature parente** : `F-216` — P2 Famille FR — ~20 outils décisionnels fréquence haute

---

## Verdict : GO

F-216 livre 14 **outils décisionnels** suivant exactement le même pattern écran que les 35 outils Famille FR existants et les 4 outils F-210. Le placement, la séquence, la charge et la continuité sont déjà cadrés par l'architecture du panel décisionnel. Aucun ajustement de layout requis.

---

## Intention métier + comportement visible attendu

Sur un dossier de droit de la famille française, l'avocat dispose, dans le panel des outils décisionnels (`app-decisional-tools-panel`), de **14 nouvelles sections** permettant d'analyser :
- le montant de la prestation compensatoire et de la pension alimentaire ;
- la liquidation de la communauté légale ;
- le recouvrement ARIPA pour pension impayée ;
- la délégation, le retrait de l'autorité parentale, l'audition du mineur ;
- les variantes d'adoption (intra-familiale, internationale) ;
- l'indignité successorale, le recel de succession, la donation-partage, le partage notarial ;
- la donation entre époux, la présomption de paternité et son désaveu.

Chaque section = un formulaire rendant un verdict décisionnel après calcul + base juridique + recommandation.

---

## Rappel verdict étape 0

GO avec ajustements (`SF-216-00-coherence.md`) — ajustements techniques (nouveaux flags IA + vérification DELETE-191) sans aucun impact écran.

---

## Parcours écran réel de l'avocat

Source : `docs/business/parcours-ecran-dossier.md`. Position de F-216 = **étape « outils décisionnels »** du parcours (`app-decisional-tools-panel`). Zone existante, déjà cartographiée. F-216 y ajoute 14 sections comme F-210 et F-DT-36 avant lui.

| Étape parcours | Zone | Statut |
|---|---|---|
| Outils décisionnels | `app-decisional-tools-panel` | ✅ existant — F-216 y ajoute 14 sections |

---

## Challenges

- **Placement** : ✅ standard. Tous les outils décisionnels Famille FR vivent dans ce panel. F-216 rejoint ses voisins naturels (35 outils existants + 2 outils F-210).
- **Lisibilité de la séquence** : ✅ le panel décisionnel vient après l'analyse IA — séquence portée par `app-case-dashboard-stepper`. Aucune rupture.
- **Charge écran** : ✅ le moteur de visibilité F-IA-04 n'affiche que les outils pertinents pour le dossier courant. Chacun des 14 outils F-216 sera en mode CONTEXTUAL (affichage uniquement si flag IA pivot détecté) sauf outils deletion-191 restaurés (à maintenir ALWAYS_ON pour retrouver la couverture antérieure). Pas de surcharge — au pire 3 outils supplémentaires visibles sur un dossier typique.
- **État final / continuité** : ✅ les verdicts alimentent la synthèse décisionnelle + F-176. Chaînage établi. Pas de dead-end.

---

## Spécificités des outils CONTEXTUAL F-216

Chaque outil F-216 possède un flag IA pivot (listé dans `SF-216-00-coherence.md` §Tableau récapitulatif) qui conditionne son affichage. L'avocat peut également forcer l'affichage via le catalogue F-238. Les sections sont conformes au template d'outil décisionnel existant :
- En-tête : titre + badge statut (`CONTEXTUAL`) + bouton « Analyser »
- Corps : formulaire (champs pré-remplis via `prefillFromAi()`)
- Pied : verdict décisionnel + base juridique + recommandation

---

## Invariants anti-surcharge pour les mini-specs

1. Gate `workspaceCountry='FRANCE'` strict : aucun outil F-216 visible sur un workspace BE (bannière info si le composant est monté par erreur).
2. Sections conformes au pattern des outils décisionnels existants — pas de mise en page ad hoc (réutiliser le pattern F-210 / F-FA-12).
3. Mode de visibilité explicite et seedé par outil (`CONTEXTUAL` + flag IA pivot, ou `ALWAYS_ON` pour les 3 outils DELETE-191 restaurés) — ne jamais laisser un outil orphelin du moteur F-IA-04.
4. Pas de décision d'interface non portée par une SF — toute variation de template doit être documentée dans la mini-spec frontend correspondante.

---

## Décision finale

**GO.** F-216 suit le pattern écran standard des outils décisionnels. Aucun ajustement de placement. Aucune nouvelle zone, aucun nouveau parcours.

---

## MAJ du parcours écran de référence

Aucune — F-216 s'inscrit dans une zone (`app-decisional-tools-panel`) déjà cartographiée.

---

## Liens

- `docs/features/F-216/SF-216-00-coherence.md` — cadrage fonctionnel (étape 0)
- `docs/business/parcours-ecran-dossier.md` — référentiel parcours écran
- `docs/features/F-217/SF-217-00b-ux-coherence.md` — modèle pour feature Famille jumelle
