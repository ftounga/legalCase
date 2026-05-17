# F-244 — Cadrage cohérence fonctionnelle (étape 0)

> Produit via la skill `ai-skills/feature-coherence-challenger.md`.
> **Nature** : F-244 est une refonte d'**architecture de l'information** — pas une capacité métier nouvelle. L'étape 0 (cohérence fonctionnelle) est donc légère ; la substance du cadrage est portée par l'**étape 0 bis** (audit `screen-coherence-challenger` du 2026-05-15 — `docs/audits/AUDIT-2026-05-15-ux-coherence-detail-dossier.md`).

## Verdict : GO

## Intention métier (1 phrase)

Réorganiser l'écran de détail du dossier pour que l'avocat enchaîne sans friction analyse → simulation décisionnelle → arbitrage, sur un écran lisible et non saturé.

## Workflow métier réel de l'avocat (8-15 étapes + source)

Source : audit `screen-coherence-challenger` 2026-05-15 (parcours reconstruit, ⚠ **hypothèse à valider auprès d'un avocat**) + signaux terrain démos Renversez 13/05 et Mengue 11/05.

1. Reçoit un dossier client (litige travail / titre de séjour / divorce…) + premières pièces.
2. Prend connaissance des pièces, les classe.
3. Analyse faits / chronologie / risques / points juridiques.
4. Identifie la situation juridique précise (type de litige, juridiction, stade, position).
5. Évalue et chiffre les enjeux — indemnités, délais, validité d'un acte, recevabilité : il « simule » des décisions.
6. Consolide une vue d'ensemble des verdicts et du risque du dossier.
7. Vérifie la cohérence de ses évaluations avec les preuves du dossier.
8. Arbitre la stratégie (transiger / saisir / contester / renoncer).
9. Produit le livrable de procédure (conclusions, requête, recours).
10. Dépose / transmet, puis suit le dossier jusqu'à clôture.

## Cartographie features actuelles ↔ workflow

| Étape métier | Feature(s) LegalCase | Statut |
|---|---|---|
| 1-2 import des pièces | F-04 dossier, F-05 upload | ✅ Livrée |
| 3 analyse / synthèse | F-08/09/10 pipeline, F-12 / F-31 synthèse | ✅ Livrée |
| 4 situation juridique | F-243 stade procédural, F-IA-04 détection | ✅ Livrée |
| 5 simulation décisionnelle | outils F-DT-* / F-IM-* / F-FA-*, F-IA-01 pré-fill | ✅ Livrée |
| 6 vue d'ensemble des verdicts | F-IA-02 tableau de bord décisionnel | ✅ Livrée |
| 7 cohérence saisie ↔ preuves | F-IA-03 contrôle de cohérence | ✅ Livrée |
| 8 arbitrage stratégie | — (acte intellectuel de l'avocat, hors outil) | — |
| 9 livrable de procédure | F-DT-04/06, F-IM-06 (exports) ; F-98 conclusions | 🟡 partiel / 🔵 Backlog |
| 10 clôture | F-04 statut OPEN/CLOSED | ✅ Livrée |

## Position de la nouvelle feature

F-244 n'introduit **aucune étape métier nouvelle**. C'est une refonte de l'**écran** `case-file-detail` qui héberge les étapes 5, 6 et 7 et porte le lien vers l'étape 3. Elle agit sur l'agencement (architecture de l'information), pas sur la capacité fonctionnelle.

## Challenge amont

*Chaque brique fonctionnelle que F-244 réorganise existe-t-elle dans le produit ?*

→ Oui. Synthèse ✅, outils décisionnels des 3 domaines ✅, tableau de bord F-IA-02 ✅, contrôle F-IA-03 ✅, `case-dashboard-stepper` ✅, stade procédural F-243 ✅. F-244 ne crée aucune capacité — il réarrange des features livrées. **Aucun trou amont.**

## Challenge aval

*La sortie de F-244 (un écran restructuré, doté d'un emplacement terminal) est-elle exploitable par l'aval ?*

→ L'aval du parcours est l'étape 9 (livrable de procédure). F-244 prévoit explicitement un emplacement terminal où se branchera l'action « générer les conclusions ». Cette capacité = **F-98** (🔵 Backlog, réactivée 2026-05-15). F-244 ne construit pas F-98 — il lui réserve la place. Trou aval **acceptable** : F-98 est inscrite au backlog, et F-244 reste pleinement utile sans elle (les exports F-DT-04/06 et F-IM-06 existent déjà comme livrables terminaux partiels).

## STOPs / pré-requis à ajouter au backlog

Aucun. Toutes les briques amont sont livrées ; l'unique dépendance aval (F-98) est déjà au backlog.

## Invariants anti-gadget pour la mini-spec

Le risque « gadget » d'une refonte = déplacer des boîtes sans rien résoudre. La mini-spec devra respecter :

1. La refonte doit **mesurablement** réduire la charge perçue (structure en onglets effective, pas un simple re-tri vertical).
2. Le couplage saisie → verdict doit être **vérifiable** (alignement outils ↔ tableau de bord testé, pas supposé).
3. Les onglets doivent **épouser le parcours réel** (Dossier / Analyse / Décision / Suivi) — pas un découpage arbitraire.
4. Le badge de pré-remplissage IA agrégé au niveau de l'onglet « Décision » est **non négociable** : sans lui, passer en onglets *régresse* la visibilité du travail de l'IA.
5. Aucune capacité métier supprimée ni dégradée : F-244 ne touche pas la logique des outils décisionnels (repositionnement uniquement).

(Ces invariants complètent les invariants anti-surcharge de l'étape 0 bis — audit 2026-05-15.)

## Décision finale

**GO.** F-244 réorganise des features fonctionnellement cohérentes et toutes livrées ; aucun trou amont ; l'unique dépendance aval (F-98 conclusions) est déjà inscrite au backlog. La feature passe `Backlog` → `À faire`.

Étape suivante : **étape 0 bis** — formaliser `SF-244-00b-ux-coherence.md` à partir de l'audit du 2026-05-15 (déjà ~90 % fait), puis mini-spec SF-244-01.
