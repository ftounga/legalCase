# F-222 — Cadrage cohérence écran (étape 0 bis)

## Verdict : **GO** — insertion dans un parcours écran déjà établi (zéro nouvel écran, zéro surcharge structurelle)

## Périmètre écran (après trim étape 0)
- 4 nouveaux outils décisionnels : `F-FA-HABILITATION-FAMILIALE`, `F-FA-TGD`, `F-FA-ASF-CAF`, `F-FA-ASSISTANCE-EDUCATIVE`.
- 1 extension d'un outil existant : branche **DEC** dans `F-FA-14-ordonnance-protection`.

## Parcours écran réel de l'avocat (réf. `docs/business/parcours-ecran-*.md` + pattern F-IA-04 établi)
1. L'avocat ouvre un dossier (`case-file-detail`).
2. Le pipeline IA produit la synthèse + détecte les outils décisionnels pertinents → **panel décisionnel** (`decisional-tools-panel`, F-IA-04).
3. Les outils détectés s'affichent en **cards** (zone `contextual`) ; les autres sont accessibles via le **catalogue** filtrable (F-238).
4. L'avocat clique une card → ouverture de la **section outil** (`*-section.component`) en modal/expansion : formulaire pré-rempli IA → calcul → verdict + bloc « Jurisprudence applicable » (F-JU-01).

## Cartographie des zones écran existantes ↔ nouveaux éléments
| Élément F-222 | Zone écran cible | Pattern existant réutilisé |
|---|---|---|
| 4 nouvelles cards d'outils | `decisional-tools-panel` (contextual + catalogue) | identique aux ~50 outils Famille FR déjà présents |
| 4 nouvelles sections outil | modal/expansion `*-section.component` | template canonique F-155 |
| Branche DEC | **dans** la section `F-FA-14-ordonnance-protection` existante | nouvelle option du même formulaire (comme BAR) |

## Challenge placement
Les 4 cards se placent dans le panel décisionnel **exactement comme les autres outils Famille FR** (détection IA → contextual, sinon catalogue). Aucun nouvel emplacement, aucune réorganisation. DEC = une option supplémentaire dans un formulaire déjà ouvert par l'avocat (pas de nouveau point d'entrée). ✅

## Challenge lisibilité de la séquence
La séquence card → formulaire pré-rempli → verdict → jurisprudence est **inchangée** et déjà connue de l'avocat sur tous les outils. Les 4 nouveaux outils ne créent aucune rupture de parcours. ✅

## Challenge charge de l'écran cible
Le panel gère déjà ~50 outils Famille FR via catalogue filtrable (F-238) + détection contextuelle → +4 cards n'augmente pas la charge perçue (seules les cards **détectées** s'affichent par défaut ; le reste est replié dans le catalogue). ✅ Pas de surcharge.

## Challenge état final / continuité
Chaque outil termine sur un verdict exploitable (conditions remplies / mesure orientée / montant / éligibilité) + jurisprudence si disponible — état terminal identique aux autres outils. DEC : le verdict de `F-FA-14` intègre simplement l'option retenue. ✅

## Invariants anti-surcharge pour les mini-specs
- **Réutiliser le template canonique** F-155 (`*-section.component`) — pas de layout custom.
- **Détection contextuelle** : chaque outil déclare son `trigger_field`/`layer` de visibilité (sinon il reste au catalogue, pas affiché d'office → pas de pollution).
- **DEC = une branche dans F-FA-14**, pas une card ni un écran séparé.
- Respecter `DESIGN_SYSTEM.md` (cards navy/or, JetBrains Mono pour les IDs, badge pré-rempli `auto_awesome`).
- Garde-fou intégrité : chaque nouvel outil → entrée `KNOWN_NO_DASHBOARD_TILE_IDS` (pas de dashboard tile) + `TOOL_REGISTRY` + `decision_tool_visibility_rules` cohérents ([[feedback_pre_merge_visibility_seed_check]]).

## Décision finale
**GO.** Aucun nouvel écran, insertion dans le parcours décisionnel F-IA-04 établi. Enchaîner les mini-specs.
