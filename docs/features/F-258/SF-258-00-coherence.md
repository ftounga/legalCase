# F-258 — Cadrage cohérence (étape 0)

## Verdict : **GO**

## Intention métier (1 phrase)
Avertir l'avocat, **avant** de générer le projet de conclusions, que des outils décisionnels **proposés et pré-remplis** par l'IA n'ont pas encore été **calculés** — donc ne seront pas pris en compte — sans pour autant l'empêcher de générer.

## Source du workflow
Pratique observée au **test manuel 2026-06-06** (dossier licenciement Lemaire) + diagnostic code : `AncienneteService.calculate()` n'est appelé que par le `@PostMapping` du contrôleur ; le pré-remplissage IA ne persiste aucun résultat. Les conclusions ne s'appuient que sur les outils **calculés** (cf. [[project_coherence_conclusions_outils_non_calcules]]).

## Workflow métier réel de l'avocat
1. Dépôt des pièces du dossier.
2. Lancement de l'analyse → faits, points juridiques, risques.
3. L'app **propose** les outils décisionnels pertinents (pré-remplis par l'IA) dans le panneau de l'onglet « Décision ».
4. Pour chaque outil pertinent, l'avocat l'ouvre, **vérifie/ajuste** les valeurs pré-remplies, et **calcule** (valide) → résultat persisté (chiffrage + jurisprudence).
5. Une fois les outils clés calculés, il **génère** le projet de conclusions.
6. Les conclusions intègrent chiffrages + jurisprudence (F-JU-02) des outils **calculés**.
7. Il relit, ajuste, dépose au greffe.

**Le trou (entre 4 et 5)** : rien ne rappelle à l'avocat que des outils **proposés** (étape 3) n'ont pas été **calculés** (étape 4). Il peut passer directement de 3 à 5 → conclusions appauvries, sans signal.

## Cartographie features actuelles ↔ workflow
| Étape métier | Feature(s) | Statut |
|---|---|---|
| 1-2. Dépôt + analyse | F-3/F-4/F-5 | ✅ Livrée |
| 3. Outils proposés + pré-remplis | F-IA-04 (visibilité, `decision-tools-visibility`) + F-IA-01 (pré-remplissage) | ✅ |
| 4. Calcul des outils | outils décisionnels (`POST …/calculate`) + F-IA-03 (cohérence des valeurs) | ✅ |
| 5. Génération des conclusions | F-98 / F-243 (`POST …/conclusions/generate`) | ✅ |
| 6. Jurisprudence dans conclusions | F-JU-02 | ✅ |
| 7. Relecture | note à l'avocat (dans le projet) | ✅ |
| **Garde-fou de complétude** (entre 4 et 5) | *aucun* | ❌ **Manquant → F-258** |

## Position de la nouvelle feature
F-258 s'insère **juste avant l'étape 5** (génération), comme **garde-fou non bloquant** : à l'instant où l'avocat s'apprête à générer, on lui signale les outils proposés non calculés.

## Challenge amont
Tout ce que F-258 suppose existe déjà :
- liste des outils **proposés** → `GET …/decision-tools-visibility` (`alwaysOn` + `contextual`) ✅
- liste des outils **calculés** → `GET …/dashboard` (`tiles`, un tile = un résultat persisté) ✅
- bouton de génération → `conclusions-section` (F-98) ✅

→ **Aucun trou amont. GO amont.** (Pas de nouveau backend strictement requis : `N = (alwaysOn + contextual) − tiles.toolId`.)

## Challenge aval
La sortie de F-258 = un avocat informé qui peut (a) aller calculer les outils manquants → conclusions plus complètes (F-98 + F-JU-02 ✅), ou (b) générer en connaissance de cause. Sortie pleinement exploitable.

→ **Aucun trou aval. GO aval.**

## STOPs / pré-requis à ajouter au backlog
Aucun. Toutes les briques amont/aval sont livrées.

## Invariants anti-gadget pour la mini-spec
1. **Non bloquante** (décision PO 2026-06-08) : le bouton « Générer quand même » reste toujours disponible.
2. **Décompte exact** : `N = (alwaysOn + contextual) − {toolId des tiles}`. **Ne jamais inclure le `catalog`** (outils non proposés) → sinon faux positifs massifs.
3. **`N = 0` → aucune alerte** (ne pas polluer quand tout le pertinent est calculé).
4. **Actionnable** : l'alerte offre un moyen d'aller voir/calculer les outils manquants (focus/scroll vers le panneau d'outils, ou liste).
5. **Dynamique** : recalculer N après qu'un outil a été calculé (l'alerte décrémente / disparaît).
6. **Pas d'heuristique floue** : se baser uniquement sur les données réelles (visibilité + tiles), pas sur une estimation.

## Décision finale
**GO** — feature de cohérence sans trou fonctionnel ; toutes les briques existent ; approche = **alerte non bloquante**. Statut PRODUCT_SPEC : `À faire`. Prochaine étape : **0 bis (cadrage écran)** car l'alerte ajoute un élément visible à l'onglet « Décision », puis mini-spec.
