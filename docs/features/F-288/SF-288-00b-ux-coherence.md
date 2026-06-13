# F-288 — Cadrage cohérence écran (étape 0 bis)

> Feature : **Curation amont des outils intégrés à la génération des conclusions.** Skill : `ai-skills/screen-coherence-challenger.md`. 2026-06-13.

## Verdict : **GO avec ajustements**

> **Recadrage PO 2026-06-13** : flux = **étape de composition intermédiaire déclenchée par le clic « Générer »** (pas un panneau inline permanent). Scope = 3 dimensions (outils, chefs de demande, moyens adverses), livrées en vagues. La vague 1 (cadrée ci-dessous) = **outils décisionnels**.

## Intention métier + comportement visible attendu

Quand l'avocat clique **« Générer »**, au lieu de lancer directement, on l'amène sur un **écran/modal de composition** : il voit, par **dimension** (vague 1 = outils décisionnels calculés ; vagues suivantes = chefs de demande, moyens adverses), la liste des éléments avec une **case à cocher** (toutes cochées par défaut). Il décoche ce qu'il ne plaide pas, puis **« Confirmer & générer »**. Le choix est **durable** et s'applique aux **régénérations**.

## Rappel verdict étape 0 (feature-coherence-challenger)

**GO avec ajustements** — décision PO 2026-06-13 : écran de composition intermédiaire, 3 dimensions en vagues (vague 1 = outils), persistance durable, défaut tout coché. Trous réels : outils (`detectAll` injecte tout), chefs de demande (fondation F-262 non branchée), moyens adverses (auto-extraits, pas de tri). Les 5 autres ingrédients sont déjà curés ailleurs (hors périmètre).

## Parcours écran réel de l'avocat (ouverture du dossier → état terminal)

> Source : écrans réellement codés (routes Angular `/case-files/:id/...`, `conclusions-section` autonome) + `docs/business/parcours-ecran-dossier.md` + pratique avocat. État post-F-267 (page conclusions dédiée) et F-287 (streaming).

1. Avocat ouvre le dossier → écran détail.
2. Importe les pièces, lance l'analyse (asynchrone).
3. Consulte la **synthèse** (`/case-files/:id/synthesis`).
4. Va dans l'onglet **Décision** : outils décisionnels + tableau de bord.
5. **Calcule** les outils pertinents (clic « Calculer/Comparer ») — certains pour explorer, d'autres pour fonder l'acte.
6. Ouvre la **page Conclusions** (`/case-files/:id/conclusions`, F-267) — acte en feuille + panneau d'actions.
7. État `NOT_GENERATED` : encart F-258 (« N outils non calculés ») + bouton **« Générer le projet de conclusions »**.
8. **[F-288 ici]** Avant de générer, il **compose** : il décoche les outils calculés qu'il ne plaide pas.
9. **Génère** (streaming F-287) → l'acte se construit.
10. **Édite / co-rédige** (F-264/265/277), **versionne**, **exporte** (F-266).
11. Éventuelle **régénération** (stale / récapitulatif F-271) — qui doit **respecter** la composition choisie en 8.
12. **État terminal** : projet de conclusions finalisé, exporté à en-tête cabinet (F-266) ; au-delà, cycle de vie du dossier (F-282 rounds, hors périmètre F-288).

## État terminal du processus (explicite)

Le traitement « conclusions » est terminal quand l'avocat **exporte l'acte déposable** (F-266) — la curation F-288 est un **réglage en amont de la génération (étape 8)**, pas un nouvel état terminal. Elle ne crée aucun dead-end : elle se résout toujours par le bouton « Générer » déjà présent.

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours | Écran / zone | Statut |
|---|---|---|
| 3. Synthèse | `/case-files/:id/synthesis` | ✅ |
| 4-5. Outils + calcul | onglet Décision (`decisional-tools-panel`) | ✅ |
| 6. Page conclusions | `/case-files/:id/conclusions` (F-267) | ✅ |
| 7. Encart outils non calculés + Générer | `conclusions-section` état `NOT_GENERATED` | ✅ (F-258) |
| **8. Sélecteur de composition** | **`conclusions-section`, état `NOT_GENERATED`, au-dessus de « Générer »** | ❌ **à créer (F-288)** |
| 9-10. Génération / édition / export | `conclusions-section` | ✅ |

## Position candidate de la feature (écran, zone, points d'entrée)

- **Écran** : page Conclusions (`/case-files/:id/conclusions`), composant `conclusions-section`.
- **Déclenchement** : le clic sur **« Générer le projet de conclusions »** (état `NOT_GENERATED`) **et** sur **« Régénérer »** (état stale) **ouvre d'abord l'écran de composition** au lieu de lancer directement. L'avocat compose, puis **« Confirmer & générer »** lance réellement (streaming F-287).
- **Forme** : **modal/panneau de composition** par-dessus la page conclusions (pas un sous-écran routé — on reste sur `/conclusions`). L'encart F-258 (« outils non calculés ») **reste à sa place** dans l'état `NOT_GENERATED` (alerte d'entrée, en amont), distinct de la composition (qui agit sur les calculés).
- **Point d'entrée** : aucune nouvelle route ; on **intercepte** l'action « Générer/Régénérer » existante.

## Challenge placement

✅ **Correct.** L'avocat compose **au moment exact** du « Générer » — c'est le déclencheur naturel. Le modal évite de charger l'état `NOT_GENERATED` en permanence (cf. challenge charge) : on ne montre la composition **que** quand on en a besoin.

## Challenge lisibilité de la séquence

✅ avec ajustement. La séquence devient : *(F-258, en amont) « il te manque des calculs » → clic Générer → **(F-288) écran de composition : choisis ce que tu plaides** → Confirmer & générer*. Le modal doit clairement s'annoncer comme une **étape de composition**, pas une simple confirmation (titre explicite, « Confirmer & générer » ≠ « OK »), pour que l'avocat comprenne que décocher **agit** sur l'acte.

## Challenge charge écran

✅ **Le modal résout la charge.** En sortant la composition dans une étape dédiée, l'état `NOT_GENERATED` ne s'alourdit pas. Dans le modal, par **dimension** (sections : « Outils décisionnels », plus tard « Chefs de demande », « Moyens adverses ») :
- **Liste compacte** (une ligne par élément, case à gauche, libellé court ; pas les résultats détaillés des outils — déjà dans l'onglet Décision).
- **Groupé par dimension** ; chaque dimension repliable (pattern F-268). Vague 1 = une seule section « Outils décisionnels ».
- Actions « Tout cocher / Tout décocher » par dimension pour les longues listes.

## Challenge état final / continuité

✅ Le modal se résout toujours par **« Confirmer & générer »** (continuité immédiate) ou « Annuler » (retour à la page sans générer — choix de design assumé, pas un dead-end). En **régénération**, le modal se ré-ouvre **pré-coché selon le choix durable** → l'avocat peut ajuster ou confirmer tel quel. **Invariant** : la composition durable est **le défaut** à chaque ré-ouverture, jamais un reset.

## Ajustements IA requis (à intégrer en mini-spec)

1. **Intercepter** « Générer » (et « Régénérer ») pour ouvrir d'abord le **modal de composition** ; lancer la génération seulement sur « Confirmer & générer ».
2. **Modal structuré par dimension** (vague 1 = « Outils décisionnels ») ; listes compactes, repliables, « tout cocher/décocher ».
3. **Ne pas redemander** : à la ré-ouverture (régénération), pré-cocher selon le choix durable.
4. **Distinct de F-258** : l'encart « outils non calculés » reste dans l'état `NOT_GENERATED` (amont) ; le modal agit sur les **calculés**.
5. Si **aucun élément curable** (0 outil calculé en vague 1) : **ne pas ouvrir** le modal, générer directement (pas de friction inutile).
6. Si **tout décoché** : avertir dans le modal (« aucun outil intégré — l'acte sera fondé sur les seuls faits/pièces ») sans bloquer « Confirmer & générer ».

## Invariants anti-surcharge pour la mini-spec

1. **Zéro nouvelle route** : modal par-dessus `/conclusions`, on reste dans `conclusions-section`.
2. **Composition uniquement à la demande** (au clic Générer/Régénérer) — n'alourdit jamais l'état `NOT_GENERATED`.
3. **Distinct de l'encart F-258** (alerte d'entrée ≠ composition des calculés).
4. **Continuité** : la composition s'applique à la génération ET à la régénération ; durable = défaut, jamais reset.
5. **Pas de duplication** des résultats d'outils (déjà dans l'onglet Décision) : nom + état coché.
6. **Pas de friction vide** : aucun élément curable → pas de modal.

## Décision finale

**GO avec ajustements.** Écran de composition en **modal intermédiaire déclenché par « Générer »/« Régénérer »** (forme PO), structuré par dimension (vague 1 = outils décisionnels), sans nouvelle route, charge maîtrisée, distinct de F-258, continuité assurée (durable = défaut). Vagues 2-3 (chefs de demande, moyens adverses) = nouvelles sections du même modal.

## MAJ apportée au parcours écran de référence

Ajout à `docs/business/parcours-ecran-dossier.md` : intercalage d'une étape **« composition / curation des outils »** entre « page Conclusions / état non généré » et « Générer », et précision que la **régénération respecte la composition choisie** (pas de re-saisie).
