# SF-286-00 — Cadrage de cohérence fonctionnelle (étape 0)

**Feature** : F-286 — Stratégie de dossier unifiée
**Skill** : `ai-skills/feature-coherence-challenger.md`
**Date** : 2026-06-12
**Décision PO de mécanisme (2026-06-12)** : couche LLM de synthèse (cf. ci-dessous).

---

## 1 — Workflow métier réel de l'avocat cible

Source : `docs/business/audit-workflow-decisionnel.md` (audit PO 2026-06-12), parcours-écran existants
`docs/business/parcours-ecran-*.md`, signaux terrain Renversez / Mengue (mémoire).

L'avocat instruit un dossier prud'homal / famille / immigration de la façon suivante :

1. **Intake / qualification** (F-285) — il cadre le litige.
2. **Analyse de dossier** (F-3/F-4/F-5, Terminées) — pipeline IA chunk→document→dossier produit une **synthèse**
   (faits, points juridiques, risques, pistes stratégiques).
3. **Outils décisionnels** (F-DT / F-IM / F-FA, ~90 outils) — il **calcule** des verdicts ciblés, **une situation
   métier par outil** (validité du licenciement, comparateur d'indemnités, droit au travail, checklist divorce…).
   Invariant produit : *1 outil = 1 situation*.
4. **Jurisprudence** (F-JU) — il appuie / réfute.
5. **Conclusions** (F-98 / F-271…281) — il rédige l'acte.
6. **Cycle de vie** (F-282 rounds, F-283 phases, F-284 échéancier) — il pilote le dossier dans le temps.

**Le trou identifié par l'audit** : entre l'étape 3 (verdicts silotés, un par outil) et l'étape 5 (rédaction),
l'avocat **raisonne stratégie de dossier** — *référé ou fond ? concilier/transiger ou plaider ? quels chefs de
demande prioriser ? dans quel ordre agir ?*. Aujourd'hui cette synthèse stratégique n'existe nulle part :
les verdicts restent éclatés (par construction, invariant 1 outil = 1 situation) et les « pistes stratégiques »
sont éparses dans la synthèse d'analyse. L'avocat fait **mentalement** la consolidation. F-286 outille cette
consolidation **en lecture**, sans toucher aux outils.

---

## 2 — Cartographie des features existantes sur le workflow

| Étape workflow | Feature(s) | Statut | Rôle vis-à-vis de F-286 |
|---|---|---|---|
| Analyse → synthèse | F-3/F-4/F-5 | Terminées | **Amont** : fournit la synthèse `DONE` (faits/risques/pistes) lue par F-286 |
| Outils décisionnels | F-DT/F-IM/F-FA | Terminées | **Amont** : fournissent les **verdicts CALCULÉS** (tiles) lus par F-286 |
| Agrégation verdicts | F-167 / F-98 SF-98-01 (`assembleDecisionToolTiles`) | Terminée | **Amont** : agrégation générique déjà exposée — F-286 la réutilise (zéro chemin parallèle) |
| Pistes stratégiques | synthèse `strategies` RETAINED | Terminée | **Amont** : `loadRetainedStrategies` déjà existant |
| Conclusions | F-98 / F-271…281 | Terminées | **Aval** : la reco stratégique peut éclairer la rédaction (déjà nourrie par les mêmes tiles) |
| Alerte outils non calculés | F-258 | Terminée | **Précédent de référence** : compteur N = (alwaysOn+contextual) − calculés, encart non bloquant |

---

## 3 — Mécanisme retenu (décision PO 2026-06-12) : couche LLM de synthèse

Un **appel LLM dédié** (prompt nouveau, `SYSTEM_CASE_STRATEGY`) lit :
- les **verdicts des outils décisionnels CALCULÉS** du dossier (`assembleDecisionToolTiles`, exactement comme F-98),
- la **synthèse d'analyse** `DONE` la plus récente (faits / points juridiques / risques / pistes stratégiques retenues),

et produit une **recommandation stratégique lisible** structurée : voie procédurale (référé vs fond),
posture (concilier/transiger vs plaider), priorisation des chefs de demande, séquencement.

C'est une **couche de LECTURE par-dessus**. Elle **n'altère, ne réordonne, ne modifie AUCUN outil décisionnel
individuel** ni leur positionnement / visibilité. Le résultat est persisté dans une table **additive** dédiée
(`case_strategy`), sans aucune écriture sur les tables `*_analysis` des outils ni sur `decision_tool_visibility_rules`.

---

## 4 — Challenge AMONT (les pré-requis existent-ils ?)

| Pré-requis fonctionnel | Existe ? | Preuve |
|---|---|---|
| Une synthèse de dossier exploitable | ✅ | F-3/F-4/F-5 Terminées ; `loadLatestAnalysisResult` lit la synthèse `DONE` |
| Des verdicts d'outils calculés agrégés | ✅ | `CaseFileDashboardService.assembleDecisionToolTiles` (F-98 SF-98-01) |
| Filtre « calculés uniquement » (anti-invention) | ✅ | `assembleDecisionToolTiles` ne retourne **que** les outils dont une `*Analysis` est persistée (= calculés) ; les tuiles pré-remplies non cliquées n'y figurent pas — même semantique que F-258 |
| Gate Anthropic + AiCallContext | ✅ | F-257 — nouveau `JobType.SYSTEM_CASE_STRATEGY` (system-level, record obligatoire userId=null) |
| Déterminisme | ✅ | `temperature=0` câblé en dur dans `AnthropicService.doAnalyze` |
| Isolation workspace | ✅ | `resolveCaseFileForUser` / `workspaceMemberRepository.findByUserAndPrimaryTrue` (pattern Contradictoire/CaseNote) |

**Aucun trou amont.** Tous les pré-requis sont livrés.

---

## 5 — Challenge AVAL (la sortie est-elle exploitable ?)

La sortie de F-286 est une **recommandation lisible affichée** à l'avocat dans l'onglet Décision (cf. étape 0 bis).
Elle est **terminale en lecture** : l'avocat la consulte pour décider de sa stratégie. Elle peut, sans dette,
éclairer la rédaction des conclusions (déjà nourrie par les mêmes verdicts). Aucun export ni brique aval manquante
n'est bloquant à ce stade — la reco est consultable et copiable.

**Aucun trou aval bloquant.**

---

## 6 — Garde anti-gadget : INVARIANT « 1 outil = 1 situation »

Le risque de gadgétisation de F-286 est précis : **dériver vers un méta-outil** qui prétendrait remplacer ou
réordonner les outils décisionnels, ou inventer un verdict là où l'avocat n'a rien calculé. Invariants que la
mini-spec DOIT respecter :

1. **Lecture seule sur les outils** : F-286 ne fait AUCUN INSERT/UPDATE sur les tables `*_analysis` ni sur
   `decision_tool_visibility_rules`. Preuve = revue migration (table `case_strategy` additive only) + revue service
   (aucun repository d'outil injecté en écriture ; seul `assembleDecisionToolTiles` en lecture).
2. **Calculés uniquement** : l'entrée du prompt = `assembleDecisionToolTiles` (outils calculés) + synthèse. Les
   tuiles pré-remplies non cliquées ne sont JAMAIS comptées comme des verdicts (semantique F-258).
3. **Honnêteté du vide** : si peu/pas d'outils calculés, la reco le **signale** (encart « outils à calculer »,
   reprise du précédent F-258) au lieu d'inventer une stratégie sur des données absentes.
4. **Pas de mutation de positionnement** : la reco est une **vue de synthèse** placée au-dessus du tableau de bord,
   elle ne déplace ni ne masque aucun outil.

---

## 7 — Verdict

## ✅ GO

Tous les pré-requis amont sont livrés, la sortie est exploitable en lecture, le mécanisme PO (couche LLM de
synthèse) est cohérent avec l'architecture (réutilise `assembleDecisionToolTiles`, gate F-257, temperature 0,
isolation workspace). L'invariant « 1 outil = 1 situation » est protégé par 4 gardes anti-gadget explicites
ci-dessus, à reporter dans la mini-spec.

→ Statut PRODUCT_SPEC F-286 : `Backlog` → `À faire` (mise à jour par l'orchestrateur).
→ Étape suivante : 0 bis (cohérence écran), feature à impact écran.
