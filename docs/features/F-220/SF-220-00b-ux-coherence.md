# F-220 — Cadrage cohérence écran (étape 0 bis)

> Feature : **F-220 — P3 Immigration FR (longue traîne : accords bilatéraux + Outre-mer + niches FR forte)**
> Skill appliquée : `ai-skills/screen-coherence-challenger.md`. Modèle : `docs/features/F-222/SF-222-00b-ux-coherence.md`.
> Suppose un verdict étape 0 GO (cf. `SF-220-00-coherence.md`, périmètre trimé = 6 outils 🟢).

## Verdict : **GO** — insertion dans le parcours décisionnel Immigration FR déjà établi (zéro nouvel écran, zéro nouvelle route, zéro surcharge structurelle)

## Périmètre écran (après trim étape 0)
- 6 nouveaux outils décisionnels, tous en **cards CONTEXTUAL** du panel décisionnel F-IA-04 — exactement comme les 22 outils Immigration FR livrés par F-214 :
  - `F-IM-47-regime-tunisien-fr`
  - `F-IM-48-regime-mayotte-fr`
  - `F-IM-49-vpf-jeune-majeur-l42322-fr`
  - `F-IM-50-pacs-vpf-fr`
  - `F-IM-51-decheance-nationalite-fr`
  - `F-IM-52-signalement-sis-fr`
- Aucun nouvel écran, aucune nouvelle route, aucune réorganisation de zone.

## Parcours écran réel de l'avocat (réf. `docs/business/parcours-ecran-*.md` + pattern F-IA-04 / F-214 établi)
1. L'avocat ouvre un dossier (`case-file-detail`).
2. Le pipeline IA produit la synthèse + détecte les outils décisionnels pertinents → **panel décisionnel** (`decisional-tools-panel`, F-IA-04).
3. Les outils détectés s'affichent en **cards** (zone `contextual`) ; les autres sont accessibles via le **catalogue** filtrable (F-238).
4. L'avocat clique une card → ouverture de la **section outil** (`*-section.component`) en modal/expansion : formulaire pré-rempli IA (F-246) → calcul → verdict + bloc « Jurisprudence applicable » (F-JU-01).

Les 6 outils F-220 s'insèrent à l'**étape 3** : ce sont des cards qui n'apparaissent que lorsqu'un **pivot dérogatoire** est détecté dans le dossier (nationalité tunisienne, territoire Mayotte, PACS, jeune majeur ex-mineur, déchéance de nationalité, signalement SIS). Hors détection, elles restent repliées dans le catalogue.

## Cartographie des zones écran existantes ↔ nouveaux éléments
| Élément F-220 | Zone écran cible | Pattern existant réutilisé |
|---|---|---|
| 6 nouvelles cards d'outils | `decisional-tools-panel` (contextual + catalogue) | identique aux 39 outils Immigration FR déjà présents |
| 6 nouvelles sections outil | modal/expansion `*-section.component` | template canonique F-155 (comme F-214) |

## Challenge placement
Les 6 cards se placent dans le panel décisionnel **exactement comme les outils Immigration FR de F-214** : détection IA d'un pivot → affichage en zone `contextual` ; sinon repli dans le catalogue filtrable. Aucun nouvel emplacement, aucun nouveau point d'entrée, aucune réorganisation. ✅

## Challenge lisibilité de la séquence
La séquence card → formulaire pré-rempli → verdict → jurisprudence est **inchangée** et déjà connue de l'avocat sur tous les outils Immigration FR. Les 6 nouveaux outils ne créent aucune rupture de parcours ni nouvelle convention d'interaction. ✅

## Challenge charge de l'écran cible
Le panel gère déjà 39 outils Immigration FR via catalogue filtrable (F-238) + détection contextuelle. Comme les 6 outils sont **strictement CONTEXTUAL** (pivots dérogatoires rares : < 30 % des dossiers), ils **ne s'affichent jamais d'office** : sur un dossier standard FR, aucune card F-220 n'apparaît. La charge perçue par défaut est donc **inchangée** ; seuls les dossiers atypiques (tunisien, Mayotte, PACS, jeune majeur, déchéance, SIS) voient surgir la card pertinente. ✅ Pas de surcharge — c'est précisément l'argument anti-surcharge majeur de F-220 : aucun de ces outils ne doit jamais être ALWAYS_ON.

> Point d'attention (hérité de l'étape 0) : Mayotte repose idéalement sur un **trigger territoire**. Le moteur `extractDetectedSituations` n'expose pas encore de champ territoire ; en V1, Mayotte est rattaché au pivot texte `mayotte_detecte` (F-235 text-trigger sur territoire/`nationalite`/lieu de demande). À défaut de détection, la card reste au catalogue (jamais ALWAYS_ON) — aucune pollution de l'écran standard.

## Challenge état final / continuité
Chaque outil termine sur un verdict exploitable (régime applicable, portée territoriale du titre, éligibilité de la voie, validité de la mesure + voies de recours) + jurisprudence si disponible — état terminal identique aux autres outils Immigration FR. La sortie alimente directement le conseil et le générateur de recours (F-IM-06) en aval. ✅

## Invariants anti-surcharge pour les mini-specs
- **CONTEXTUAL strict, jamais ALWAYS_ON** : chacun des 6 outils déclare un `layer=CONTEXTUAL` + un `trigger_field` (pivot). Aucun ne doit s'afficher d'office sur un dossier FR standard.
- **Réutiliser le template canonique** F-155 (`*-section.component`) — pas de layout custom (comme F-214).
- **Détection contextuelle = condition d'affichage** : hors pivot, la card reste dans le catalogue filtrable (pas de pollution du `contextual`).
- Respecter `DESIGN_SYSTEM.md` (cards navy/or, JetBrains Mono pour les IDs, badge pré-rempli `auto_awesome` F-246).
- Garde-fou intégrité : chaque nouvel outil → entrée `KNOWN_NO_DASHBOARD_TILE_IDS` (pas de dashboard tile) + `TOOL_REGISTRY` + `KNOWN_FRONTEND_TOOL_IDS` + `decision_tool_visibility_rules` cohérents ([[feedback_pre_merge_visibility_seed_check]]).

## Décision finale
**GO.** Aucun nouvel écran, aucune route, insertion dans le parcours décisionnel F-IA-04 / F-214 établi, charge écran par défaut inchangée (6 cards CONTEXTUAL). Enchaîner les mini-specs.
