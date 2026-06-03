# F-223 — Cadrage cohérence écran (étape 0 bis)

> Skill appliquée : `ai-skills/screen-coherence-challenger.md`. Modèle de structure : `docs/features/F-222/SF-222-00b-ux-coherence.md`. Suppose le verdict étape 0 (`SF-223-00-coherence.md`) = **GO avec ajustements** (périmètre trimé 9 outils + 1 extension).

## Verdict : **GO** — insertion dans le parcours décisionnel F-IA-04 déjà établi (zéro nouvel écran, zéro surcharge structurelle)

## Périmètre écran (après trim étape 0)
- **9 nouveaux outils décisionnels Famille BE** : `cohabitation-legale-be`, `adoption-be`, `kafala-be-recueil-legal`, `gpa-be-situation-contentieuse`, `regime-algerien-be`, `regime-be-separation-biens`, `dip-be-loi-applicable-famille`, `dip-be-reconnaissance-decision-etrangere`, `etat-civil-be-modification`.
- **1 extension** d'un outil existant : branche **mandat extra-judiciaire + déclaration anticipée** dans `protection-majeur-be` (F-217).

## Parcours écran réel de l'avocat famille BE (réf. `docs/business/parcours-ecran-*.md` + pattern F-IA-04 / F-217 établi)
1. L'avocat ouvre un dossier de droit de la famille belge (`case-file-detail`).
2. Le pipeline IA produit la synthèse + extrait les flags Famille BE (niveau 2/3) → **panel décisionnel** (`decisional-tools-panel`, F-IA-04).
3. Les outils dont le flag pivot est détecté basculent en **cards CONTEXTUAL** (zone `contextual`) ; les autres restent accessibles via le **catalogue** filtrable (F-238).
4. L'avocat clique une card → ouverture de la **section outil** (`*-section.component`) en modal/expansion : formulaire pré-rempli IA (F-246) → calcul → verdict + actes à produire + bases juridiques. Pas de bloc « Jurisprudence applicable » côté BE tant que **F-JU-04 est parké** (silence > erreur).

## Cartographie des zones écran existantes ↔ nouveaux éléments
| Élément F-223 | Zone écran cible | Pattern existant réutilisé |
|---|---|---|
| 9 nouvelles cards d'outils Famille BE | `decisional-tools-panel` (CONTEXTUAL si flag détecté, sinon catalogue) | identique aux 15 outils Famille BE F-211/F-217 déjà présents |
| 9 nouvelles sections outil | modal/expansion `*-section.component` | template canonique F-155 (cf. `divorce-dc-be-section`, `protection-majeur-be-section`) |
| Branche mandat extra-judiciaire / déclaration anticipée | **dans** la section `protection-majeur-be` existante | nouvelle vue/champs du même formulaire (comme l'administration déjà présente) |

## Challenge placement
Les 9 cards se placent dans le panel décisionnel **exactement comme les 15 outils Famille BE déjà livrés** par F-211/F-217 (flag IA détecté → CONTEXTUAL, sinon catalogue F-238). Aucun nouvel emplacement, aucune réorganisation. L'extension mandat = des champs/une vue supplémentaire dans une section déjà ouverte par l'avocat (pas de nouveau point d'entrée, pas de nouvelle card). ✅

## Challenge lisibilité de la séquence
La séquence card → formulaire pré-rempli IA → verdict → actes à produire est **inchangée** et déjà connue de l'avocat sur tous les outils Famille BE. Les 9 nouveaux outils ne créent aucune rupture de parcours. La longue traîne BE-only (cohabitation légale, kafala, GPA, régime algérien, DIP, état civil) s'instruit avec la même grammaire d'écran. ✅

## Challenge charge de l'écran cible
Le panel gère déjà des dizaines d'outils Famille (FR + 15 BE) via catalogue filtrable (F-238) + détection contextuelle. **Seules les cards dont le flag pivot est détecté s'affichent par défaut** ; le reste reste replié dans le catalogue. +9 cards n'augmente donc pas la charge perçue par défaut (un dossier réel ne déclenche en CONTEXTUAL que les 1-2 outils pertinents à sa situation). ✅ Pas de surcharge — c'est précisément la raison du choix CONTEXTUAL (jamais ALWAYS_ON) imposé par le cadrage étape 0.

## Challenge état final / continuité
Chaque outil termine sur un verdict exploitable (validité / recevabilité / loi applicable / régime adapté / éligibilité de modification d'état civil / reconnaissance) + actes à produire + bases juridiques — état terminal identique aux autres outils Famille BE. L'extension : le verdict de `protection-majeur-be` intègre la qualification de la voie conventionnelle (mandat valable → pas de saisine judiciaire) sans changer l'état terminal de la section. ✅

## Invariants anti-surcharge pour les mini-specs
- **Réutiliser le template canonique** F-155 (`*-section.component`) — pas de layout custom.
- **Détection contextuelle bridée** : chaque outil déclare son `trigger_field` (flag pivot) + `layer = CONTEXTUAL` de visibilité — **jamais `ALWAYS_ON`** (sinon pollution du panel sur tout dossier famille BE). Si aucun flag extractible en V1 pour un outil, fallback `CATALOG` (pattern SF-217-17), jamais `ALWAYS_ON`.
- **Extension mandat = une branche dans `protection-majeur-be`**, pas une card ni un écran séparé (cohérent invariant « 1 outil = 1 situation » : la protection du majeur vulnérable est une seule situation).
- Respecter `DESIGN_SYSTEM.md` (cards navy/or, JetBrains Mono pour les IDs, badge pré-rempli `auto_awesome`).
- Garde-fou intégrité : chaque nouvel outil → entrée `KNOWN_NO_DASHBOARD_TILE_IDS` (pas de dashboard tile) + `TOOL_REGISTRY` + `decision_tool_visibility_rules` cohérents ([[feedback_pre_merge_visibility_seed_check]]).
- **Pas de bloc jurisprudence BE** dans les sections tant que F-JU-04 est parké (silence > erreur).

## Enrichissement référentiel `docs/business/parcours-ecran-*.md`
La longue traîne Famille BE (cohabitation légale, adoption, kafala, GPA, régime algérien, séparation de biens, DIP familial, reconnaissance de décision étrangère, état civil) s'inscrit dans le parcours écran « dossier Famille BE → panel décisionnel F-IA-04 → section outil » déjà documenté pour F-211/F-217. Aucun nouveau parcours écran à créer ; mention à ajouter au référentiel Famille BE comme extension de longue traîne (9 outils + 1 branche), même grammaire d'écran.

## Décision finale
**GO.** Aucun nouvel écran, insertion dans le parcours décisionnel F-IA-04 établi (identique à F-211/F-217). Enchaîner les mini-specs (étape 1).
