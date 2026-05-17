# Cadrage cohérence — F-217 — P2 Famille BE (étape 0)

**Date** : 2026-05-17
**Skill appliquée** : `ai-skills/feature-coherence-challenger.md`
**Feature parente** : `F-217` — P2 Famille BE — ~10 outils décisionnels fréquence haute
**Sources** : `docs/features/F-191/audit-famille-be-exhaustif.md` (audit exhaustif 93 situations, 2026-05-06) ; audit de complétude 2026-05-17.

---

## Contexte et déclencheur

Le droit de la famille belge n'est couvert que par **5 outils décisionnels réellement BE-fonctionnels** (F-FA-05/06/07 transversaux, F-FA-11 partiel, F-FA-23 bipays) + **4 outils BE-only livrés par F-211** (`divorce-dc-be`, `divorce-ddi-3voies-be`, `tribunal-famille-be-mesures-provisoires`, `pacte-successoral-be-2018`). Face à ~30 outils côté FR, c'est le déséquilibre dénoncé par la mémoire `feedback_belgique_never_forget`.

F-211 (P1) a traité les **urgences**. F-217 (P2) traite la **fréquence haute** : les situations qu'un avocat belge rencontre en routine de cabinet. F-223 (P3) traitera la spécificité longue traîne.

---

## Workflow métier réel — avocat belge en droit de la famille

L'avocat belge instruit un dossier famille selon 4 piliers, sous la compétence unique du **Tribunal de la famille** (TF — CJ art. 572bis, guichet unique depuis la loi du 30/07/2013) :

1. **Couple — dissolution et patrimoine** : qualifier le régime matrimonial (communauté légale par défaut depuis la loi du 22/07/2018), puis, à la dissolution, conduire la **liquidation-partage** (procédure du notaire commis, CJ art. 1207+ — très différente de la France).
2. **Enfants** : fixer l'**autorité parentale** (conjointe par défaut, exclusive sur décision du TF), l'hébergement, et la **contribution alimentaire** (méthode Renard, CC art. 203/203bis).
3. **Patrimoine transmis** : à un décès, établir la **dévolution** et la **réserve héréditaire** (1/2 fixe depuis la réforme du 31/07/2017), arbitrer l'**acceptation / renonciation** (délais impératifs).
4. **Protection et situations internationales** : protéger le **majeur incapable** (loi du 17/03/2013), et — population binationale forte en BE — statuer sur la **reconnaissance des mariages et divorces étrangers** et la **filiation** contestée.

Séquence type d'un dossier : consultation → qualification de la situation → choix de la voie → mesures provisoires (si urgence — couvert F-211) → procédure au fond TF → liquidation → exécution / appel.

---

## Cartographie features existantes ↔ workflow

| Étape du workflow | Outil existant | Couverture BE |
|-------------------|----------------|---------------|
| Dissolution du couple (DC / DDI) | F-211 `divorce-dc-be`, `divorce-ddi-3voies-be` | ✅ livré |
| Mesures provisoires (urgence) | F-211 `tribunal-famille-be-mesures-provisoires` | ✅ livré |
| Anticipation successorale | F-211 `pacte-successoral-be-2018` | ✅ livré |
| Calendrier d'hébergement | F-FA-06 (transversal, supporte modes BE) | ✅ partiel |
| Partage immobilier (calcul) | F-FA-05 (transversal, calcul neutre) | ✅ fonctionnel |
| Régime matrimonial, liquidation-partage | — | ❌ trou |
| Autorité parentale, contributions | F-FA-02/19 **FR-only** (masqués en BE) | ❌ trou |
| Successions (dévolution, réserve, acceptation) | F-FA-24 **FR-only** (8 outils, masqués en BE) | ❌ trou |
| Protection des majeurs | F-FA-25 **FR-only** | ❌ trou |
| Mariage / divorce étranger, filiation | F-FA-18 **FR-only** | ❌ trou |

Les outils FR existants ne sont **pas réutilisables** : ils sont bâtis sur le Code civil français (régime de communauté réduite aux acquêts, réserve 1/2-2/3-3/4, prestation compensatoire) — mécanismes juridiquement distincts du droit belge. Conformément à `feedback_belgique_never_forget`, F-217 part des sources de droit belge, pas d'un miroir FR.

---

## Challenge de cohérence amont — les pré-requis existent-ils ?

| Pré-requis | État | Verdict |
|------------|------|---------|
| Référentiels procéduraux BE | `TRIBUNAL_FAMILLE_BE`, `COUR_APPEL_FAMILLE_BE`, `CASSATION_FAMILLE_BE` seedés (migration 162) | ✅ disponible |
| Pattern outil décisionnel (Calculator + section + F-IA-04 + F-IA-03) | Éprouvé sur ~35 outils, dont 4 Famille BE (F-211) | ✅ disponible |
| Wrappers Angular + seeds visibilité Famille BE | Pattern posé par F-211 SF-211-05 | ✅ disponible |
| Extraction IA de flags pivots famille BE | F-211 a seedé des flags (`divorce_dc_envisage`…). Les nouveaux outils F-217 n'ont **pas** tous un flag pivot dédié extrait par le pipeline V1 | ⚠️ ajustement |

**Ajustement amont** : plusieurs outils F-217 (régime matrimonial, successions) sont des situations *toujours pertinentes* sur un dossier famille BE plutôt que des situations à détecter. Ils relèvent du mode **`ALWAYS_ON`** (pas de flag IA requis) — comme `F-FA-06`. Les outils contextuels (reconnaissance mariage étranger) seront `CATALOG` ou `CONTEXTUAL` selon qu'un flag pivot est extractible ; à défaut, `CATALOG` (accessible via le catalogue F-238, pré-fill IA = 0 documenté). Aucun nouvel investissement pipeline IA n'est requis en V1.

---

## Challenge de cohérence aval — la sortie est-elle exploitable ?

Le verdict de chaque outil décisionnel alimente la synthèse décisionnelle du dossier (`app-case-dashboard`) et les pistes stratégiques (F-176) — chaînage déjà établi et utilisé par les 4 outils de F-211. F-217 s'y branche à l'identique. Pas de dead-end. ✅

---

## Périmètre cadré — les 10 outils de F-217

Sélection des 10 situations de **fréquence haute** structurant le workflow ci-dessus, par pilier, à partir du Top-10 priorité de l'audit F-191 (§ 4.2) en excluant ce que F-211 a déjà livré :

| # | tool_id | Situation | Source juridique | Pilier |
|---|---------|-----------|------------------|--------|
| 1 | `regime-mat-be-communaute-legale` | Régime de communauté légale BE — composition, gestion, dettes | Livre 3 CC (loi 22/07/2018) | Couple/patrimoine |
| 2 | `liquidation-partage-be` | Liquidation-partage post-divorce — notaire commis, projet, contredits | CJ art. 1207+ / 1218 | Couple/patrimoine |
| 3 | `autorite-parentale-be` | Autorité parentale conjointe (défaut) vs exclusive (décision TF) | CC art. 374-375 | Enfants |
| 4 | `contribution-alimentaire-enfants-be` | Contribution alimentaire des enfants — méthode Renard | CC art. 203 / 203bis | Enfants |
| 5 | `contribution-conjoint-be` | Pension alimentaire entre ex-époux post-divorce | CC art. 301 | Enfants/couple |
| 6 | `succession-be-devolution-reserve` | Dévolution légale + réserve héréditaire (1/2 fixe) | Livre 4 CC (loi 31/07/2017), art. 913+ | Successions |
| 7 | `succession-be-acceptation-renonciation` | Acceptation pure / sous bénéfice d'inventaire / renonciation — délais | CC art. 774+ | Successions |
| 8 | `protection-majeur-be` | Administration de la personne / des biens du majeur incapable | Loi 17/03/2013 | Protection |
| 9 | `mariage-etranger-be-reconnaissance` | Reconnaissance d'un mariage / divorce étranger (dont talaq) — ordre public | CDIP art. 21+ / 27 | International |
| 10 | `contestation-filiation-be` | Contestation de filiation (paternité) — qualité à agir, délais | CC art. 318 | Filiation |

**Divergence assumée avec l'actuel one-liner F-217 du PRODUCT_SPEC** : le one-liner mentionnait « kafala recueil » et « Code DIP belge » comme outils autonomes — l'audit F-191 les classe en P3 (spécialisés, longue traîne) ; ils sont **reportés à F-223**. Le one-liner F-217 sera mis à jour pour refléter ce périmètre cadré (étape 6).

**Niveau des outils** : 5 (scoring / analyse de validité / arbre décisionnel — verdicts décisionnels), comme les outils de F-211.

---

## Verdict : **GO avec ajustements**

F-217 est cohérent : il comble des trous réels du workflow de l'avocat belge, suit le pattern écran et technique éprouvé par F-211, et tous ses pré-requis existent. Ajustements à porter par les mini-specs :

1. **Mode de visibilité explicite par outil** : `ALWAYS_ON` pour les situations toujours pertinentes (régime matrimonial, contributions, successions, autorité parentale) ; `CATALOG` pour les situations contextuelles sans flag IA extractible en V1 (reconnaissance mariage étranger, contestation filiation). Aucun investissement pipeline IA.
2. **Pré-fill IA = 0 en V1** documenté (`PREFILL_COUNT_ALWAYS_ZERO`) pour les outils sans flag pivot — factuel, pas une dette masquée.
3. **Aucune réutilisation des Calculators FR** : chaque outil part du droit belge (un outil = une situation, `feedback_decision_tools_one_per_situation`).
4. **Sources juridiques à valider par un avocat belge** avant mise en production (plusieurs articles tagués « à vérifier » dans l'audit F-191) — centralisées dans chaque Calculator.

## Invariants anti-gadget pour les mini-specs

- Chaque outil = **une situation métier belge distincte**, modélisée depuis le Code civil / judiciaire belge — jamais une transposition d'un outil FR.
- Verdict toujours rattaché à un **fondement légal belge** affiché (article CC / CJ).
- Visibilité : ne jamais afficher un outil Famille BE sur un workspace FR (gate `workspaceCountry`).
- Le contenu juridique (délais, barèmes, articles) est centralisé dans le Calculator et signalé pour validation juridique.

## Découpage proposé — livraison par 3 vagues

F-217 = 10 outils ≈ **~20 SF** (1 SF backend + 1 SF frontend par outil, frontend mutualisable). Livraison par vagues cohérentes par pilier, back/front parallélisés (contrats API figés) :

- **Vague 1 — Patrimoine du couple** : outils 1, 2 (régime légal, liquidation-partage).
- **Vague 2 — Enfants** : outils 3, 4, 5 (autorité parentale, contributions enfants + conjoint).
- **Vague 3 — Successions, protection, international** : outils 6, 7, 8, 9, 10.

Chaque vague suit le cycle complet (mini-spec → readiness → dev → review → release → merge).

## Impact sur PRODUCT_SPEC

- Statut F-217 : `À planifier` → `À faire` (verdict GO).
- One-liner F-217 mis à jour avec le périmètre cadré des 10 outils (étape 6 — après validation).

---

## Liens

- `docs/features/F-191/audit-famille-be-exhaustif.md` — audit source
- `docs/features/F-211/` — précédent P1 Famille BE (pattern de référence)
- `ai-skills/feature-coherence-challenger.md` — skill appliquée
