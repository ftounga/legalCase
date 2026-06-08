# F-258 — Cadrage cohérence écran (étape 0 bis)

## Verdict : **GO**

## Parcours écran réel de l'avocat (onglet « Décision »)
De haut en bas, l'onglet « Décision » enchaîne :
1. **Panneau d'outils décisionnels** (`decisional-tools-panel`) — tuiles des outils **proposés** (pré-remplis), « Cliquez pour ouvrir / Calculer ».
2. **Tableau de bord décisionnel** (`case-dashboard`) — tuiles des outils **calculés** (verdict / score).
3. **Section « Projet de conclusions »** (`conclusions-section`) — bouton **« Générer le projet de conclusions »**.

L'avocat scanne donc : *outils proposés → résultats calculés → génération*. L'alerte doit se trouver au **point de décision** : juste avant le bouton de génération.

## Cartographie écrans / zones
| Zone | Composant | Rôle |
|---|---|---|
| Outils proposés | `decisional-tools-panel` | ouvrir + calculer |
| Outils calculés | `case-dashboard` | voir les résultats |
| **Génération** | `conclusions-section` | **point d'insertion de l'alerte** |

## Challenge — placement, lisibilité, charge, état final
- **Placement** : l'alerte s'insère dans `conclusions-section`, **au-dessus du bouton** « Générer le projet de conclusions ». C'est l'endroit exact où l'avocat prend la décision de générer. ✅
- **Lisibilité de la séquence** : cohérent avec le scan haut→bas (proposés → calculés → génération + alerte). ✅
- **Charge de l'écran cible** : l'onglet est déjà dense (3 zones). Mais l'alerte est **conditionnelle** (`N > 0` seulement) → **zéro charge ajoutée** quand tout le pertinent est calculé. Encart discret, pas une modale. ✅
- **État final / continuité** : après génération, l'alerte **reste pertinente** tant que `N > 0` (rappel que les conclusions sont partielles). Elle disparaît dès que `N = 0`. Pas d'état bloqué.

## Invariants anti-surcharge pour la mini-spec
1. **Conditionnelle** : l'alerte n'existe dans le DOM que si `N > 0` (jamais d'encart vide / « 0 outil »).
2. **Discrète, non modale** : un **encart d'information** (style avertissement) au-dessus du bouton — pas une pop-up bloquante.
3. **Une seule** : pas de répétition par outil ; un encart agrégé « N outils … ».
4. **Action claire** : un lien/bouton « Voir les outils à calculer » qui ramène au panneau d'outils (scroll/focus), distinct de « Générer quand même ».
5. **Pas de déplacement d'éléments existants** : l'alerte s'ajoute, ne réorganise pas l'onglet.

## Décision finale
**GO** — placement au point de décision, charge nulle hors cas pertinent, état final propre. Prête pour la mini-spec.

## Référentiel parcours-écran
Onglet « Décision » : ajout d'un **encart d'avertissement conditionnel** dans `conclusions-section`, au-dessus du CTA de génération (zone « Projet de conclusions »).
