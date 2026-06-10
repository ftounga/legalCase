# F-263 — Cadrage cohérence (étape 0)

> Feature : **Conclusions V2 ③ — Chiffrage 100 % auditable** — chaque € du dispositif tracé à un calcul vérifiable, jamais inventé par le LLM.
> Programme « Conclusions V2 », levier fonctionnel n°3. Skill : `ai-skills/feature-coherence-challenger.md`. 2026-06-10.
> ⚠️ Directive PO ce run : « FORT risque de DOUBLON — si l'étape 0 conclut au doublon/gadget (comme F-262), CLORE à la fondation et tracer le constat. NE PAS builder un gadget. »

## Verdict : **STOP — clôture à la fondation (doublon avéré)**

Le « chiffrage source-of-truth » que F-263 prétend introduire **existe déjà et est déjà câblé** dans la génération des conclusions. La feature, telle que cadrée, **reconstruirait un moteur de calcul redondant des 60+ outils décisionnels** et ajouterait une couche de traçabilité que **les gardes F-98 + le flux outils → prompt couvrent déjà fonctionnellement**. Construire F-263 produirait du redondant (moteur) ou du gadget (traçabilité cosmétique non fiable). **Recommandation : clore F-263 à la fondation, tracer le constat, et reporter la seule valeur résiduelle (affichage de la traçabilité montant→outil) sur F-266 SI un signal terrain l'exige.**

---

## Intention métier (1 phrase)

Garantir que chaque montant du dispositif des conclusions (« 29 100 € », « 13 641 € »…) **provienne d'un calcul vérifiable** (barème Macron × ancienneté × convention) et **ne soit jamais dérivé/inventé par le LLM**, avec une traçabilité montant → calcul affichable.

---

## Constat central — le chiffrage source-of-truth EXISTE DÉJÀ

La hantise visée par F-263 (« le LLM peut dériver un montant ») est **déjà neutralisée par l'architecture en place**, à trois niveaux :

### 1. Les montants ne sont PAS produits par le LLM — ils sont injectés depuis les outils

Le pipeline de génération (`CaseConclusionPromptBuilder.buildUserMessage`) injecte une section **`=== VERDICTS DES OUTILS DÉCISIONNELS REMPLIS ===`** assemblée à partir des `DashboardTile` (champ `toolTiles`) :

```
- <label outil> : <primaryValue>  — <secondaryValue>
```

Le `primaryValue` / `secondaryValue` d'une tuile = **la sortie déterministe du calculateur** (barème Macron, indemnités, congés payés afférents…), calculée hors-LLM par les 60+ outils décisionnels (modèle « outil décisionnel = simulateur »). **Le LLM reçoit le montant déjà calculé** ; sa tâche est de le **plaider**, pas de le **produire**.

### 2. Le prompt système INTERDIT déjà au LLM d'inventer un chiffre

`REDACTION_QUALITY_GUARD` (SF-98-55), appliqué aux 45 cellules :
- **Règle 1** : « Les verdicts des outils décisionnels … te sont fournis comme matière première INTERNE. »
- **Règle 3** : « Dans le PAR CES MOTIFS, **reprends les chefs chiffrés fournis** … »
- **Règle 5** : « **N'invente AUCUN chef non étayé** par les faits, les pièces ou **les verdicts fournis**. »

Le contrat est déjà « le LLM consomme les calculs, ne les produit pas » — c'est **exactement** la promesse de F-263, **déjà tenue**.

### 3. F-258 garantit déjà la complétude amont (montant manquant = outil non calculé)

Le seul trou résiduel — un montant **absent** parce que l'outil n'a pas été calculé — est **déjà couvert par F-258** (alerte non bloquante « outils à calculer » dans `conclusions-section`). Si un montant manque dans le dispositif, ce n'est pas un défaut de chiffrage : c'est un outil que l'avocat n'a pas rempli, et F-258 le signale.

---

## Cartographie features actuelles ↔ promesse F-263

| Promesse F-263 | Déjà couvert par | Statut |
|---|---|---|
| Montant = calcul vérifiable (pas une dérivation LLM) | Outils décisionnels (60+ calculateurs déterministes) → `toolTiles` → prompt | ✅ existe |
| LLM consomme les calculs, ne les produit pas | `REDACTION_QUALITY_GUARD` règles 1/3/5 (SF-98-55) | ✅ existe |
| Dispositif reprend les chefs chiffrés fournis | `REDACTION_QUALITY_GUARD` règle 3 + dispositif complet | ✅ existe |
| Montant manquant signalé | F-258 (alerte « outils à calculer ») | ✅ existe |
| **Moteur de chiffrage source-of-truth (barèmes par domaine)** | **= les 60+ outils décisionnels eux-mêmes** | ❌ **reconstruire = doublon massif** |
| **Affichage de la traçabilité montant → outil au survol** | — (n'existe pas) | 🟡 **seule valeur résiduelle, relève de F-266 (UX)** |

## Challenge amont

- ✅ Pré-requis présents et **suffisants** : moteur de calcul (outils), injection au prompt (`toolTiles`), gardes anti-invention (SF-98-55), complétude (F-258).
- ❌ Le « moteur de chiffrage source-of-truth + barèmes PAR DOMAINE » que F-263 veut bâtir **dupliquerait les calculateurs existants** (barème Macron = déjà un outil ; indemnités = déjà un outil ; pension/prestation famille = déjà des outils). Invariant produit violé : **un outil décisionnel = une situation métier** ; un second moteur de chiffrage créerait deux sources de vérité divergentes pour le même montant (anti-pattern déjà tranché : « divergence entre outils ≠ bug, pas d'override »).

## Challenge aval

- La seule sortie **nouvelle** que F-263 pourrait livrer = une **traçabilité affichée** (survoler « 29 100 € » → « barème Macron, ancienneté 7 ans, plafond 8 mois — outil Indemnité licenciement sans cause »). Or :
  1. Cette traçabilité est un **rendu UX** (survol), pas un moteur fonctionnel → elle relève de **F-266** (« survoler … un montant → le calcul »), pas d'un chantier fonctionnel séparé.
  2. Elle suppose d'**ancrer chaque montant du texte LLM à l'outil source**. Le texte étant du **markdown libre généré**, il n'existe **aucun ancrage déterministe** montant-texte → outil : retrouver « 29 100 € » dans la prose et le relier au bon calcul = heuristique fragile (mêmes montants pour des chefs différents, montants reformulés, arrondis). Sans ancrage fiable, l'affichage serait **trompeur** = gadget (même conclusion que F-262 sur les chefs « non outillés sans flag »).

## Invariants (si une suite était un jour décidée)

Aucun moteur de chiffrage parallèle. Toute traçabilité montant→calcul doit :
1. **Réutiliser les outils** comme unique source de montant (jamais recalculer).
2. Reposer sur un **ancrage déterministe** (le LLM devrait émettre un marqueur structuré, ex. `[montant:outilId]`, sinon pas de lien fiable) — non trivial, à ne tenter que sur signal terrain fort.
3. Rester **markdown-safe** (non-régression export Word/PDF).

## Arbitrage (réversible — décidé par défaut, tracé)

**Décision : STOP à la fondation.** Conformément à la directive PO (« si doublon/gadget, CLORE à la fondation et tracer ») et au précédent F-262 (clos à la fondation pour doublon avec F-258 + outils), F-263 **n'ouvre aucune SF de dev**. Le constat est tracé ici et reporté au `PRODUCT_SPEC.md` (par l'orchestrateur, hors commit docs de cette run).

**Conséquence sur F-266** (dépendance directive) : F-263 étant clos **sans moteur de chiffrage tracé**, **F-266 se limite à la traçabilité fait → pièce + l'export à en-tête cabinet** — **pas** la traçabilité montant → calcul (qui resterait un gadget sans ancrage déterministe, cf. challenge aval).

**Réversibilité** : si un signal terrain avocat exige explicitement la traçabilité montant→calcul affichée, rouvrir F-263 (ou l'absorber dans F-266) avec l'ancrage structuré `[montant:outilId]` en pré-requis. Aucune dette de code (rien n'est écrit).

## Décision finale

**STOP — F-263 clos à la fondation (doublon avéré avec les outils décisionnels + SF-98-55 + F-258).** Statut PRODUCT_SPEC : reste **Backlog** → passe **Bloqué (doublon — clos à la fondation 2026-06-10)**. Zéro SF, zéro code. F-266 réduit en conséquence (fait→pièce + export cabinet, sans montant→calcul).
