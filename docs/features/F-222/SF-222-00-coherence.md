# F-222 — Cadrage cohérence (étape 0)

## Verdict : **GO avec ajustements** — périmètre ramené de 7 à **4 nouveaux outils + 1 extension**

## Intention métier (1 phrase)
Compléter le catalogue décisionnel Famille FR avec les situations FR-only restantes (protection des majeurs alternative, protection des victimes de violences, recouvrement de pension via allocation, enfant en danger).

## Workflow métier réel de l'avocat famille (source : pratique standard cabinet — ⚠ hypothèse à valider avec un avocat famille)
1. Le client consulte sur une situation familiale (séparation, violences, proche vulnérable, enfant en danger…).
2. L'avocat **qualifie la situation juridique** et la juridiction compétente (JAF / juge des tutelles / juge des enfants / parquet).
3. L'avocat **évalue les conditions, droits, options et montants** applicables ← **c'est ici que vivent les outils décisionnels**.
4. L'avocat conseille la mesure / la procédure adaptée.
5. L'avocat rédige les actes et constitue le dossier (pièces).
6. Saisine de la juridiction → décision.

## Cartographie features actuelles ↔ situations visées par F-222
| Situation métier (F-222) | Outil LegalCase | Statut |
|---|---|---|
| Protéger un majeur vulnérable sans tutelle lourde | **Habilitation familiale** (art. 494-1+) | ❌ Manquant (distinct de F-FA-25 sélecteur de régime) |
| Évaluer l'éligibilité d'une victime au téléphone grave danger | **TGD** (art. 41-3-1 CPP) | ❌ Manquant |
| Obtenir une allocation quand la pension n'est pas payée | **ASF CAF** (art. L.523-1 CSS) | ❌ Manquant (≠ ARIPA = recouvrement) |
| Enfant en danger → mesure de protection | **Assistance éducative** (art. 375+ : AED/AEMO/OPP/placement) | ❌ Manquant |
| Suivi électronique du contact d'un conjoint violent | **DEC** | 🟡 variante du **BAR déjà intégré à F-FA-14** |

## Position de la nouvelle feature
Étape 3 du workflow (évaluation conditions/droits/mesure) pour 4 situations, + enrichissement de l'outil existant F-FA-14 (violences) pour le DEC.

## Challenge amont
Chaque outil suppose seulement que l'avocat ait **qualifié la situation** (étape 2) — aucune brique produit amont nécessaire (pas de dépendance à une analyse de dossier ou à un autre outil). Les 4 outils sont **autoportants** : l'avocat saisit la situation, l'outil évalue. ✅ Aucun trou amont bloquant.

## Challenge aval
La sortie (conditions remplies / mesure adaptée / montant / éligibilité) alimente le conseil et la rédaction d'actes — exploitable directement par l'avocat. Citation jurisprudentielle (F-JU-01) déjà disponible pour les outils Famille FR Cassation. ✅ Pas de trou aval bloquant.

## Ajustements anti-gadget (invariant « 1 outil décisionnel = 1 situation » — [[feedback_decision_tools_one_per_situation]])
Deux dérives détectées dans la cible initiale de 7 outils :

1. **Sur-découpage AED / AEMO / OPP** : ce sont **3 issues d'UNE SEULE situation** (« enfant en danger — mesure de protection par le juge des enfants », art. 375+ Cciv). Les fabriquer en 3 outils violerait l'invariant. → **1 seul outil `F-FA-ASSISTANCE-EDUCATIVE`** qui évalue la situation de danger et oriente vers la mesure adaptée (AED administrative / AEMO / OPP urgence / placement) selon les critères. **7 → 5.**

2. **Doublon DEC ↔ BAR** : le DEC est un dispositif de suivi électronique du contact, **même situation que le BAR** (bracelet anti-rapprochement) déjà **intégré à `F-FA-14-ordonnance-protection`**. → **DEC = extension de F-FA-14**, pas un nouvel outil. **5 → 4 nouveaux outils + 1 extension.**

3. **Anti-doublon ASF ↔ ARIPA** : `F-FA-ARIPA-RECOUVREMENT` (recouvrement forcé de la pension) existe déjà. L'ASF (allocation versée par la CAF en cas d'impayé) est une **situation distincte** (droit à une prestation sociale, pas un recouvrement). Invariant respecté **à condition** que la mini-spec ASF cadre nettement le périmètre (droit + montant ASF) sans empiéter sur le recouvrement ARIPA.

## Invariants anti-gadget pour les mini-specs
- `F-FA-ASSISTANCE-EDUCATIVE` = **un seul** outil couvrant AED/AEMO/OPP/placement (pas 3) — sortie = mesure orientée + critères de l'art. 375.
- `F-FA-ASF-CAF` = droit + montant de l'ASF uniquement ; renvoyer vers `F-FA-ARIPA-RECOUVREMENT` pour le recouvrement (pas de chevauchement).
- `F-FA-TGD` = analyzer d'**éligibilité** (danger grave + interdiction de contact + non-cohabitation) ; préciser que l'attribution relève du parquet (l'outil conseille, ne décide pas).
- `F-FA-HABILITATION-FAMILIALE` = distinct de `F-FA-25-majeurs-proteges` ; cadrer les conditions propres (lien familial, consentement, étendue des actes) — pas un re-sélecteur de régime.
- **DEC** = nouvelle branche conditionnelle dans `F-FA-14-ordonnance-protection` (comme BAR), **pas** de nouvel `*_analyses`/composant.
- Chaque outil décisionnel : tous les champs saisissables **pré-remplis par l'IA** ([[feedback_decision_tools_all_fields_prefilled]]).

## Décision finale
**GO avec ajustements.** Périmètre F-222 confirmé = **4 nouveaux outils décisionnels** (`F-FA-HABILITATION-FAMILIALE`, `F-FA-TGD`, `F-FA-ASF-CAF`, `F-FA-ASSISTANCE-EDUCATIVE`) **+ 1 extension** (DEC dans `F-FA-14`). Passage `À planifier` → `À faire`. Enchaîner étape 0 bis (cohérence écran) puis les mini-specs.
