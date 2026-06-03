# F-221 — Cadrage cohérence écran (étape 0 bis)

## Verdict : **GO** — insertion dans un parcours écran déjà établi (zéro nouvel écran, zéro surcharge structurelle)

## Périmètre écran (après trim étape 0)
- 6 nouveaux outils décisionnels BE-only : `F-IM-47-carte-a-prorogation-be`, `F-IM-48-carte-b-sejour-illimite-be`, `F-IM-49-residence-longue-duree-ue-be`, `F-IM-50-detention-centre-ferme-be`, `F-IM-51-cce-suspension-be`, `F-IM-52-victime-traite-be`.
- Aucune extension d'outil existant, aucun nouvel écran.

## Parcours écran réel de l'avocat (réf. `docs/business/parcours-ecran-*.md` + pattern F-IA-04 établi)
1. L'avocat ouvre un dossier (`case-file-detail`) d'un workspace BE en `DROIT_IMMIGRATION`.
2. Le pipeline IA produit la synthèse + détecte les outils décisionnels pertinents → **panel décisionnel** (`decisional-tools-panel`, F-IA-04).
3. Les outils détectés (flag pivot IA `true`) s'affichent en **cards** (zone `contextual`) ; les autres restent accessibles via le **catalogue** filtrable (F-238).
4. L'avocat clique une card → ouverture de la **section outil** (`*-section.component`) en modal/expansion : formulaire pré-rempli IA → calcul → verdict + bloc « Jurisprudence applicable » (F-JU-01 ; sourcing BE parké, le bloc reste vide sans casser le parcours).

## Cartographie des zones écran existantes ↔ nouveaux éléments
| Élément F-221 | Zone écran cible | Pattern existant réutilisé |
|---|---|---|
| 6 nouvelles cards d'outils BE | `decisional-tools-panel` (contextual + catalogue) | identique aux 10 outils Immigration BE livrés en P2 (F-215, `F-IM-25→34`) |
| 6 nouvelles sections outil | modal/expansion `*-section.component` | template canonique F-155 |

## Challenge placement
Les 6 cards se placent dans le panel décisionnel **exactement comme les 10 outils Immigration BE de F-215** (détection IA → contextual, sinon catalogue). Aucun nouvel emplacement, aucune réorganisation, aucun nouveau point d'entrée. ✅

## Challenge lisibilité de la séquence
La séquence card → formulaire pré-rempli → verdict → jurisprudence est **inchangée** et déjà connue de l'avocat sur tous les outils Immigration BE. Les 6 nouveaux outils ne créent aucune rupture de parcours. Cas particulier `F-IM-50` (détention centre fermé) : l'outil reste sur le même gabarit, son verdict orienté urgence (délais courts) s'affiche dans le même bloc verdict, sans alerte modale supplémentaire. ✅

## Challenge charge de l'écran cible
Le panel gère déjà ~250 outils tous domaines via catalogue filtrable (F-238) + détection contextuelle. Chaque nouvel outil arrive en **CONTEXTUAL** (jamais ALWAYS_ON) : il ne s'affiche en card que si son flag pivot IA `<slug>_detecte` est `true` pour le dossier ouvert. Sinon il est replié dans le catalogue. +6 cards potentielles n'augmente pas la charge perçue par défaut. ✅ Pas de surcharge — conforme au garde-fou Tableau C de l'audit Immigration BE (panel BE à ne pas saturer).

## Challenge état final / continuité
Chaque outil termine sur un verdict exploitable :
- `F-IM-47` → délai de prorogation + conditions persistantes ;
- `F-IM-48` → éligibilité séjour illimité (carte B) ;
- `F-IM-49` → éligibilité résident longue durée UE ;
- `F-IM-50` → durée/prolongation de détention + base et délai de la requête de mise en liberté ;
- `F-IM-51` → conditions du recours CCE en suspension + délai ;
- `F-IM-52` → éligibilité titre victime de traite + étapes de la procédure.

État terminal identique aux autres outils. Le bloc jurisprudence reste présent (vide tant que le sourcing BE est parké) sans rupture. ✅

## Invariants anti-surcharge pour les mini-specs
- **Réutiliser le template canonique** F-155 (`*-section.component`) — pas de layout custom.
- **Détection contextuelle obligatoire** : chaque outil déclare son flag pivot IA `<slug>_detecte` (niveau 2 BE-only) + son entrée `decision_tool_visibility_rules` en `layer = CONTEXTUAL` — jamais `ALWAYS_ON` (sinon il pollue le panel par défaut).
- **BE-only** : chaque outil n'est visible que sur un workspace `country = BELGIQUE` en `DROIT_IMMIGRATION` ; gate backend doublé de la visibility.
- Respecter `DESIGN_SYSTEM.md` (cards navy/or, JetBrains Mono pour les IDs, badge pré-rempli `auto_awesome`).
- Garde-fou intégrité : chaque nouvel outil → entrée `KNOWN_NO_DASHBOARD_TILE_IDS` (pas de dashboard tile) + `TOOL_REGISTRY` + `KNOWN_FRONTEND_TOOL_IDS` + `decision_tool_visibility_rules` cohérents ([[feedback_pre_merge_visibility_seed_check]]).

## Décision finale
**GO.** Aucun nouvel écran, insertion dans le parcours décisionnel F-IA-04 établi (mêmes gabarits que les 10 outils Immigration BE de F-215). Enchaîner les mini-specs.
