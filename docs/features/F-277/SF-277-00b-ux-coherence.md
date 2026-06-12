# F-277 — Cadrage cohérence écran (étape 0 bis)

> Suite de l'étape 0 (`SF-277-00-coherence.md`, verdict GO avec ajustements). Feature à impact écran : remplace la **surface d'édition** des conclulsions.

## Verdict : **GO avec ajustements** (le WYSIWYG SIMPLIFIE l'écran d'édition — moins de charge, pas plus)

## Parcours écran réel (édition d'un acte)
Page `/case-files/:id/conclusions` (F-267) → bouton **Modifier** → mode édition (SF-267-02, pleine largeur). Aujourd'hui ce mode = **split 2 colonnes** :
- **Gauche (`cs-edit-pane--editor`)** : barre co-rédaction IA (F-265) + barre d'outils markdown (F-264) + **textarea markdown brut** (monospace).
- **Droite (`cs-edit-pane--preview`)** : **aperçu live** de l'acte rendu (`conclusion-document`).

Hors édition : lecture de l'acte + sommaire (F-276) + actions (versions, diff F-280, export F-281, cycle de vie).

## Écrans / zones existants cartographiés
| Zone | Rôle | Sort avec F-277 |
|---|---|---|
| Textarea markdown (gauche) | saisie | **remplacée** par la surface WYSIWYG |
| Aperçu live (droite) | voir le rendu | **devient redondant** (on édite déjà le rendu) → **supprimé en mode WYSIWYG** |
| Barre d'outils markdown | gras/titres/listes | **conservée**, réétiquetée actions WYSIWYG (mêmes gestes sur la sélection) |
| Barre co-rédaction IA (F-265) | régénérer une section sur instruction | **conservée** (au-dessus de la surface) |
| Sommaire (F-276) | navigation sections | **conservé** |
| Versions / diff (F-280) / export (F-281) / cycle de vie | autour | **inchangés** |

## Challenges écran

### 1. Placement & lisibilité de la séquence
Le WYSIWYG **fusionne** « saisie » et « rendu » en **une seule colonne** mise en forme. La séquence devient : co-rédaction IA → **surface WYSIWYG (= l'acte)** → actions. Plus naturelle (on lit/écrit le même objet), pas de va-et-vient œil gauche↔droite.

### 2. Charge de l'écran cible — **baisse**
On passe de **2 colonnes** (textarea + aperçu) à **1 surface**. Désencombrement net, surtout sur écran moyen. La largeur élargie de SF-267-02 (`min(1640px,100%)`) reste utile pour confort de lecture/édition d'un acte, mais le besoin « 2 panneaux côte à côte » disparaît.

### 3. État final / continuité
- L'avocat reste sur la même page ; bascule lecture↔édition fluide (la surface WYSIWYG peut même rendre la frontière lecture/édition plus douce).
- **Toggle « source markdown »** (invariant 6 de l'étape 0) : un mode replié, non intrusif, pour les power-users / cas limites — ne charge pas l'écran par défaut.
- **Responsive** : sur étroit, plus de bascule éditeur/aperçu nécessaire (une seule surface) → simplification du responsive existant.

## Invariants anti-surcharge (pour la mini-spec)
1. **Une seule surface en mode WYSIWYG** : supprimer l'aperçu live redondant (ne pas empiler WYSIWYG + aperçu).
2. **Barre d'outils = mêmes actions** qu'aujourd'hui (gras, italique, titres ##/###, listes, citation) — pas d'inflation de boutons ; icônes sobres, design system.
3. **Co-rédaction IA (F-265) conservée telle quelle**, au-dessus de la surface.
4. **Sommaire (F-276) conservé** comme moyen de navigation.
5. **Toggle « source markdown » discret** (replié par défaut), jamais un 2ᵉ éditeur permanent à l'écran.
6. **Aucune nouvelle zone** ailleurs sur la page ; F-277 remplace, n'ajoute pas.
7. **Cohérence visuelle** : la surface WYSIWYG rend l'acte avec le **même style** que `conclusion-document` (Merriweather titres, justifié, filet or) — l'avocat édite « la feuille ».

## Référentiel parcours-écran
Enrichit `docs/business/parcours-ecran-conclusions.md` (si présent) : mode édition = **1 surface WYSIWYG = l'acte**, co-rédaction au-dessus, sommaire à gauche, actions en bas ; fin du split textarea/aperçu.

## Verdict final
**GO avec ajustements.** F-277 **réduit** la charge de l'écran d'édition (1 surface vs 2 colonnes) tout en préservant co-rédaction, sommaire, versions, diff, export. Sous réserve du **spike round-trip markdown** (étape 0) : sans round-trip fidèle, l'aperçu « WYSIWYG = acte » serait mensonger. Prochaine étape : **spike**, puis mini-spec.
