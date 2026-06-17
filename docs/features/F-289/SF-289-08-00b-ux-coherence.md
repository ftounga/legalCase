# SF-289-08-00b — Cadrage cohérence écran : indicateur « dossier à ré-synthétiser »

> Étape 0 bis (skill `screen-coherence-challenger`). Verdict étape 0 = GO avec ajustements.

## Parcours écran réel de l'avocat (Vue d'ensemble, onglet index 0)
1. Ouverture du dossier → **Vue d'ensemble** par défaut.
2. **① Piloter** : bandeau d'état (phase, partie attendue, prochain couperet, santé) + bloc **« Ce qui requiert ton attention »** (≤ 5 items : échéances, pièces à demander, questions IA, **analyse obsolète**).
3. **② Parcourir** : fil chronologique.
4. **③ Approfondir** : raccourcis + export.
5. L'avocat répond à une question IA inline, marque une pièce obtenue → l'item correspondant disparaît, **mais la synthèse sous-jacente reste périmée** sans qu'il le voie.

## Cartographie des zones existantes
- **Bloc attention** : contient déjà un item `ANALYSE_OBSOLETE` (« N pièces non analysées », urgence INFO, action « Relancer l'analyse »). C'est l'emplacement naturel du signal.
- **Bandeau pilotage** : porte `analysisStale` (booléen) — un point d'accroche pour un état visuel discret si besoin.

## Challenge écran
- **Placement** : ✅ Le signal vit dans le bloc « attention » déjà dédié aux actions requises → zéro nouvel emplacement, zéro déplacement d'élément existant. On **enrichit le libellé** de l'item `ANALYSE_OBSOLETE` (ou on ajuste sa condition d'apparition) pour couvrir aussi réponses IA / nouveaux documents.
- **Lisibilité de la séquence** : ✅ L'item reste en urgence `INFO` (le moins pressant) → ne déloge pas les échéances/pièces (la sélection équilibrée par type, déjà en place, garantit qu'il a un slot si présent).
- **Charge de l'écran** : ✅ Aucun nouveau composant lourd ; au plus un libellé plus parlant (« Des éléments ont changé depuis la dernière synthèse — relance-la ») + éventuellement un compteur de raisons. Pas de surcharge.
- **État final / continuité** : ✅ Après relance de la synthèse, le signal disparaît naturellement (la dernière synthèse redevient la plus récente). Cohérent avec le cycle existant.

## Verdict : **GO avec ajustements**
1. **Réutiliser l'item `ANALYSE_OBSOLETE`** (même type, même action `RELAUNCH_ANALYSIS`) plutôt que d'ajouter un item concurrent → garder ≤ 5 items lisibles.
2. Libellé **explicite et non culpabilisant**, mentionnant la **cause dominante** (réponses IA / nouveaux documents / pièces en attente) sans détailler 4 lignes.
3. Conserver l'urgence **INFO** (signal d'amélioration, pas un couperet).
4. **Aucune modale, aucune notification** — strictement in situ.

## Invariants anti-surcharge
- 0 nouvel onglet, 0 nouvelle zone, 0 déplacement d'élément existant.
- 1 seul item d'attention pour la péremption (pas un par cause).
- Le bandeau peut refléter `analysisStale` mais **sans** ajouter de bloc visuel dédié.
