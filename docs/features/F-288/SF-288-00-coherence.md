# F-288 — Cadrage cohérence (étape 0)

> Feature : **Écran de composition des conclusions avant génération** — au clic « Générer », l'avocat choisit ce qu'il verse dans l'acte, parmi les ingrédients réellement non contrôlés.
> Skill : `ai-skills/feature-coherence-challenger.md`. 2026-06-13. Origine : signal PO « rendre la génération des conclusions plus intelligente / dynamique ».
> **Recadrage PO 2026-06-13** (après audit des ingrédients) : ce n'est pas « curation des outils » seuls — c'est un **écran de composition** (étape intermédiaire déclenchée par « Générer ») couvrant les **3 ingrédients non curés** : outils décisionnels, chefs de demande, moyens adverses.

## Verdict : **GO avec ajustements** — profil de risque F-262/F-263 assumé, scope restreint aux trous réels

---

## Intention métier (1 phrase)

Au clic « Générer », présenter à l'avocat un **écran de composition** où il **sélectionne/désélectionne les ingrédients non encore contrôlés** (outils décisionnels calculés, chefs de demande, moyens adverses) avant de lancer la génération, de façon **durable** (le choix survit aux régénérations).

---

## Audit des ingrédients de l'acte (vérifié dans `CaseConclusionService.prepare`, 2026-06-13)

Une conclusion est composée de **7 ingrédients**. **5 sont déjà curables en amont** → les re-piloter = doublon. **Seuls 3 sont des trous** :

| # | Ingrédient | Source code | Déjà piloté ? |
|---|---|---|---|
| 1 | Synthèse / analyse (faits, points, risques) | `loadLatestAnalysisResult` | C'est le dossier — non curable (on ne retire pas les faits) |
| 2 | Pièces / bordereau | `loadNumberedPieces` | ✅ F-260 (ordre, numéro, suppression) |
| 3 | **Outils décisionnels calculés** | `assembleDecisionToolTiles` | ❌ **trou — injectés en bloc** |
| 4 | Pistes stratégiques | `loadRetainedStrategies` | ✅ seules les `RETAINED` (marquées) entrent |
| 5 | Jurisprudence d'appui (F-242) | `loadJurisprudenceCitations` | ✅ saisie manuelle par point |
| 6 | Jurisprudence adverse à réfuter (SF-98-56) | `loadAdverseJurisprudenceChecks` | ✅ seules les **marquées** entrent |
| 7 | **Moyens adverses (F-261)** | `loadAdverseMoyens` | 🟡 **trou partiel — doc marqué mais moyens auto-extraits, pas de tri** |

**+ chefs de demande** : la fondation F-262 (`GET …/heads-of-claim`) existe mais **n'alimente aucune sélection** dans la génération → **3ᵉ trou** si l'avocat doit choisir les chefs qu'il plaide.

➡️ **Scope F-288 = les 3 trous** : outils décisionnels (#3), chefs de demande, moyens adverses individuels (#7). Les 5 ingrédients déjà curés sont **hors périmètre** (anti-doublon).

---

## Constat central — le pipeline est un « tout-ou-rien » sur les outils calculés

Vérifié dans le code (2026-06-13) :

- `ToolUsageAggregator.detectAll(caseFileId)` parcourt **tous** les contributeurs et renvoie **chaque** outil ayant un résultat **calculé** (persisté en table `*Analysis`). Aucun filtre, aucun choix de l'avocat.
- La génération (`CaseConclusionService.prepare`) consomme cet agrégat **en bloc** : tout ce qui est calculé entre dans le prompt.
- **F-258** alerte sur les outils **applicables non calculés** (entrée manquante). **F-288 traite l'inverse** : parmi les outils **calculés**, lesquels verser dans l'acte.
- **F-265** permet de régénérer/supprimer une **section** après coup — mais ce n'est **pas persistant** comme choix de composition : la source de génération reste `detectAll`. Avec **F-271** (conclusions récapitulatives, qui « repartent ») et **F-278** (garde anti-écrasement), une régénération **ré-injecte** l'outil que l'avocat avait écarté à la main.

➡️ **Trou réel** : aujourd'hui l'avocat **ne peut pas façonner durablement ce que le générateur utilise**. C'est le seul endroit du pipeline conclusions où son intention stratégique n'a aucune prise.

---

## Workflow métier réel de l'avocat

> Source : pratique standard du contentieux + acte réel DURAND (référentiel conclusions, validé 10/06). ⚠️ Le caractère « durable du choix » est une hypothèse de pratique à confirmer en démo.

1. L'avocat analyse le dossier (faits, pièces).
2. Il **calcule plusieurs outils** — certains pour **explorer/comparer** (analyse interne), d'autres pour **fonder son acte**.
3. Il arrête sa **ligne de plaidoirie** : *tous les arguments calculés ne vont pas dans l'acte*. On n'avance pas un chiffre défavorable ; on n'empile pas un moyen faible qui affaiblit la thèse principale ; on choisit principal vs subsidiaire.
4. Il **rédige / génère** les conclusions à partir des seuls arguments retenus.
5. Il **ajuste** l'acte (édition, co-rédaction) et l'exporte.

**F-288 couvre l'étape 3 : le choix de la composition, en amont de la génération.**

## Cartographie features actuelles ↔ workflow

| Étape métier | Feature(s) LegalCase | Statut |
|---|---|---|
| 1. Analyse des faits / pièces | F-3 / F-4 / F-5 | ✅ Livrée |
| 2. Calcul des outils décisionnels | F-DT / F-IM / F-FA, pré-remplissage F-IA-01 | ✅ Livrée |
| 2b. Alerte « outils applicables **non calculés** » | **F-258** | ✅ Livrée |
| 2c. Catalogue des chefs de demande (applicabilité) | F-262 (`GET …/heads-of-claim`, socle) | ✅ Fondation livrée (enrichissement clos) |
| **3. Choix des outils/chefs à VERSER dans l'acte (ligne de plaidoirie)** | **F-288 (feature challengée)** | ❌ **Manquante — `detectAll` injecte tout** |
| 4. Génération des conclusions | F-98 / F-287 (streaming) | ✅ Livrée |
| 5. Édition / co-rédaction / export | F-264 / F-265 / F-266 / F-277 | ✅ Livrée |

## Position de la nouvelle feature

F-288 s'insère **entre l'étape 2 (calcul) et l'étape 4 (génération)** : un **filtre de composition** appliqué à l'entrée du prompt. Elle ne calcule rien (≠ outils), ne génère rien (≠ F-98), n'édite rien après coup (≠ F-265) : elle **décide ce qui entre**.

## Challenge amont

- Étapes 1, 2, 2b, 2c : ✅ toutes couvertes. La brique « outils calculés » et le « catalogue chefs de demande » existent déjà — F-288 a de quoi s'appuyer.
- **Aucun trou amont.** La feature ne suppose aucune brique manquante.

## Challenge aval

- ✅ La sortie (un acte composé uniquement des arguments retenus) s'édite/exporte via F-264/265/266/277 — aval entièrement couvert.
- ✅ La sélection durable **renforce** la cohérence de F-271 (récapitulatif) et F-278 (anti-écrasement) : la régénération respecte le choix au lieu de le piétiner.

## Le vrai débat anti-gadget — est-ce un doublon de F-265, comme F-262/263 l'étaient de F-258 ?

**Argument STOP (à prendre au sérieux)** : F-265 permet déjà de supprimer une section après génération ; F-288 ne ferait qu'« économiser un aller-retour » → marginal, pas de signal terrain → clore comme F-262/263.

**Réfutation (pourquoi GO)** :
1. F-262/263 ajoutaient une **sortie redondante** (alerte/traçabilité déjà couvertes). F-288 ajoute un **contrôle d'entrée qui n'existe nulle part**.
2. La suppression F-265 **n'est pas durable** : la source reste `detectAll`, donc une régénération (F-271/F-278, *workflow dominant à venir*) **réintroduit** l'outil écarté. F-288 est le **seul** point où le choix tient. Ce n'est pas un raccourci, c'est une **cohérence de pipeline manquante**.
3. C'est **conforme à une vraie pratique** (choix de la ligne de plaidoirie : principal/subsidiaire, écarter un angle faible/défavorable) — pas un filet théorique.

➡️ **Distinction nette des précédents clos** ⇒ verdict **GO avec ajustements**, sous réserve de la confirmation PO ci-dessous.

## STOPs / pré-requis à ajouter au backlog

- **Aucun pré-requis amont manquant.** (Le catalogue chefs de demande F-262 existe si l'on étend la curation aux chefs.)

## Décision PO requise — périmètre & persistance

| Axe | Option A | Option B | Avis |
|---|---|---|---|
| **Périmètre** | **Outils décisionnels calculés seulement** (case à cocher par outil) | **Outils + chefs de demande** (réutilise `heads-of-claim` F-262) | A = MVP net et sans ambiguïté ; B = plus complet mais ré-ouvre F-262 |
| **Persistance** | **Durable** (table de préférences de composition par dossier — survit aux régénérations) | **Volatile** (choix valable pour la génération courante seulement) | Durable = la vraie valeur (sinon doublon F-265) ; volatile = léger mais affaibli |
| **Défaut** | **Tout coché** (comportement actuel préservé, opt-out) | Rien coché (opt-in) | Tout coché = non-régressif, recommandé |

**Recommandation de cadrage** : **Périmètre A (outils calculés)** + **Persistance durable** + **défaut tout coché**. C'est le MVP qui comble le trou réel sans ré-ouvrir F-262 ni régresser l'existant. L'extension aux chefs de demande (B) = vague ultérieure si signal.

## Invariants anti-gadget pour la mini-spec

1. **Effet réel sur le prompt** : un outil désélectionné **n'est pas injecté** dans `prepare()` — vérifiable par test (l'agrégat passé au builder exclut le toolId écarté). Sans cet effet, la feature est un gadget.
2. **Persistance durable** (si Option durable retenue) : le choix **survit à une régénération** (F-271/F-278) — c'est la différence de fond avec F-265.
3. **Non-régressif** : défaut = tout sélectionné → comportement identique à aujourd'hui pour qui ne touche à rien.
4. **Pas de doublon F-258** : F-288 agit sur les outils **calculés** ; F-258 continue d'alerter sur les **non calculés**. Les deux coexistent sans se recouvrir.
5. **Pas de blocage** : la curation est facultative ; ne jamais empêcher de générer.
6. **Isolation workspace** : la préférence de composition est rattachée au dossier dans son workspace (invariant transverse).
7. **Étape 0 bis obligatoire** : ajout d'un sélecteur avant « Générer » = impact écran sur `conclusions-section` / page conclusions (F-267) → cadrage anti-surcharge requis.

## Décision finale

**GO avec ajustements.** La feature comble un trou de pipeline réel (aucun contrôle de la composition de l'acte en entrée ; `detectAll` = tout-ou-rien) et se distingue nettement de F-258 (entrée manquante) et F-265 (édition non durable). Elle échappe au sort de F-262/F-263 **à condition** que le choix ait un **effet réel et durable** sur la génération (invariants 1-2).

**Décisions PO à acter avant la mini-spec :**
1. Périmètre (A outils seuls / B + chefs de demande) — *reco A*.
2. Persistance (durable / volatile) — *reco durable*.
3. Confirmer GO malgré le précédent F-262/263 (profil de risque assumé).

### ✅ DÉCISION PO (2026-06-13) : **GO** confirmé — recadré « écran de composition ».
- **Forme** = **écran/étape de composition intermédiaire déclenchée par le clic « Générer »** (l'écran ne se charge que quand on génère), puis « Confirmer & générer ».
- **Périmètre = 3 dimensions = les 3 trous réels**, livrées en **vagues** :
  - **Vague 1 (SF-288-01) — Outils décisionnels calculés** (le seul trou franc). Ancre du framework.
  - **Vague 2 — Chefs de demande** (réutilise la fondation F-262 `heads-of-claim`).
  - **Vague 3 — Moyens adverses individuels** (tri moyen par moyen des écritures adverses F-261).
- **Persistance = durable** (le choix de composition est rattaché au dossier et **survit aux régénérations** — valeur de fond vs F-265).
- **Défaut = tout coché** (non-régressif : qui ne touche à rien obtient le comportement actuel).
- **Hors périmètre (anti-doublon)** : pièces (F-260), pistes stratégiques (RETAINED), jurisprudence d'appui (F-242), jurisprudence adverse (SF-98-56), synthèse — déjà curés ailleurs.

**Cadre vs multi-feature** : F-288 = **un** écran, **un** flux, **une** génération ; les 3 dimensions sont des **sous-features/vagues** (framework commun + dimension par vague, comme F-98/F-261), pas des features séparées.

F-288 `Backlog` → `À faire`. Prochaine étape : **0 bis** (cohérence écran de l'étape intermédiaire), puis mini-spec **SF-288-01** (vague 1, outils).
