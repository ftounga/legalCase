# F-206 — Cadrage cohérence (étape 0)

> Produit par la skill `ai-skills/feature-coherence-challenger.md`. Étape 0 du cycle de gouvernance, avant la mini-spec.
> Feature : **F-206 — P1 Travail FR — 4 outils d'urgences procédurales**.
> Date : 2026-05-19.

## Verdict : 🟢 GO avec ajustements

Les 4 outils sont des outils décisionnels qui s'insèrent proprement dans l'architecture existante (pipeline IA → détection F-IA-04 → section décisionnelle → dashboard F-167 → conclusions F-98). Toutes les briques amont et aval sont livrées. **Ajustement unique** : confirmer au cadrage-découpage que les 4 flags de détection IA sont déjà livrés par F-205 (extension Travail FR — 24 flags) ; sinon, intégrer les SF de flags au périmètre F-206.

## Intention métier (1 phrase)

Donner à l'avocat en droit du travail FR (côté salarié) quatre outils décisionnels pour les **modes de rupture / contentieux à délais courts et conséquences irréversibles** — abandon de poste / présomption de démission, congés payés acquis pendant arrêt maladie, prise d'acte de la rupture, résiliation judiciaire — situations aujourd'hui sans outil dédié et à fort risque d'erreur stratégique.

## Workflow métier réel de l'utilisateur cible

**Source** : pratique standard de l'avocat en droit du travail FR (⚠ partiellement hypothèse) + signal terrain Renversez 13/05/2026 (avocate FR droit du travail — `memory/project_renversez_post_demo_13_05.md`) + audit exhaustif `docs/features/F-191/audit-travail-fr-exhaustif.md` (§5 — top 10 outils manquants, F-DT-42 et F-DT-75 classés P1).

Workflow d'un dossier de rupture/contentieux du contrat de travail, côté salarié :

1. Le salarié consulte l'avocat en situation de crise de la relation de travail : soit l'employeur lui reproche une absence injustifiée et l'a mis en demeure de reprendre (abandon de poste) ; soit il subit des manquements graves de l'employeur (impayés de salaire, harcèlement, modification unilatérale du contrat) ; soit il sort d'un arrêt maladie long et découvre qu'aucun congé payé ne lui a été décompté.
2. L'avocat collecte les pièces : contrat de travail, bulletins de paie, courriers (mise en demeure, lettres recommandées), arrêts de travail, échanges, attestations.
3. L'avocat **qualifie la situation** : à quel régime juridique se rattache-t-elle ? (présomption de démission L. 1237-1-1 ? prise d'acte ? résiliation judiciaire ? rappel de congés payés ?).
4. L'avocat **évalue les chances de succès et les risques** : c'est l'étape décisive. Une prise d'acte qui échoue produit les effets d'une démission (perte de toute indemnité, pas d'allocation chômage) — l'avocat doit jauger la solidité des griefs **avant** de conseiller au salarié de prendre acte.
5. L'avocat **calcule les délais procéduraux** : 15 jours entre la mise en demeure et la présomption de démission ; délai de prescription de l'action en rappel de salaire/congés (3 ans) ; délais de saisine du conseil de prud'hommes.
6. L'avocat **arbitre la stratégie** et conseille le salarié : contester la présomption de démission, ou prendre acte, ou saisir le CPH en résiliation judiciaire (en restant en poste), ou réclamer le rappel de congés payés.
7. L'avocat **rédige les actes** : lettre de contestation / de prise d'acte, requête et conclusions devant le conseil de prud'hommes.
8. L'avocat **chiffre les demandes** : indemnités de rupture (barème Macron, indemnité légale, préavis, congés payés), dommages-intérêts, rappel de congés payés acquis.
9. Le dossier est plaidé ; le CPH tranche.

## Cartographie features actuelles ↔ workflow

| Étape workflow métier | Feature(s) LegalCase | Statut |
|---|---|---|
| 1-2. Réception dossier + collecte des pièces | Bloc Documents (F-1→F-12 fondations, import + upload) | ✅ Livrée |
| 3. Qualification de la situation | F-10 Analyse IA dossier · F-12 Restitution · F-IA-04 affichage conditionnel des outils par détection IA | ✅ Livrée |
| 4. **Évaluation chances / risques** | **F-206 (les 4 outils challengés)** — aujourd'hui : aucun outil pour ces 4 situations | ❌ **C'est le trou que F-206 comble** |
| 4 bis. Pré-remplissage des outils | F-IA-01 pré-remplissage IA · F-246 invariant tous-champs | ✅ Livrée / 🟡 En cours |
| 4 ter. Contrôle de cohérence des saisies | F-IA-03 validation de cohérence · F-250 complétion `critereCode` | ✅ Livrée |
| 5. Délais procéduraux | F-69 Suivi des délais légaux · F-DT-03 prescription par type de litige | ✅ Livrée |
| 6. Arbitrage stratégique | F-176 Pistes stratégiques · F-IA-02 dashboard décisionnel · F-167 dashboard agrégé | ✅ Livrée |
| 7. Rédaction des actes (courrier, conclusions) | F-98 Génération de courrier / conclusions (53/53 SF) | ✅ Livrée |
| 8. Chiffrage des demandes | F-DT-09 comparateur d'indemnités · F-DT-25 préavis · F-DT-26 congés payés · barème Macron | ✅ Livrée |
| 9. Plaidoirie / décision | Hors périmètre LegalCase | — |

## Position de la nouvelle feature

F-206 s'insère **exactement à l'étape 4** du workflow : l'évaluation décisionnelle des chances et des risques. Les 4 outils sont 4 **sections décisionnelles** de l'espace décisionnel du dossier, affichées conditionnellement par F-IA-04 sur détection IA de la situation.

Les 4 outils sont **4 situations métier distinctes** — l'invariant « un outil décisionnel = une situation métier » est respecté :

| # | Outil (tool_id audit) | Situation métier | Type | Régime juridique |
|---|---|---|---|---|
| 1 | `F-DT-42-abandon-poste-presomption-demission` | Le salarié, mis en demeure de reprendre le travail, conteste la présomption de démission (motif légitime d'absence) | Analyseur de procédure + détecteur d'irrégularité | L. 1237-1-1, D. 1237-1+ (loi marché du travail 21/12/2022) |
| 2 | `F-DT-75-conges-payes-acquisition-arrets-maladie` | Le salarié réclame les congés payés acquis pendant ses arrêts maladie non décomptés (rappel rétroactif) | Calculateur de rappel + délai d'action | Loi 22/04/2024 (post-revirement Cass. soc. 13/09/2023) |
| 3 | `F-DT-39-prise-acte-rupture` | Le salarié évalue, **avant de prendre acte**, ses chances que le CPH retienne les effets d'un licenciement sans cause | Scoring de griefs + analyseur | Cass. soc. 25/06/2003 |
| 4 | `F-DT-40-resiliation-judiciaire-cph` | Le salarié évalue l'opportunité de demander au CPH la résiliation judiciaire aux torts de l'employeur (en restant en poste) | Analyseur + appui à la rédaction de conclusions | Cass. soc. 16/03/1989, L. 1411-1 |

Le fil rouge « P1 — urgences procédurales » : (1) abandon de poste = délai de 15 jours après mise en demeure ; (3) prise d'acte = décision **irréversible** (le contrat est rompu immédiatement, l'évaluation doit précéder l'acte) ; (4) résiliation judiciaire = alternative moins risquée à arbitrer en regard de (3) ; (2) rappel de congés payés = action soumise à prescription. Erreur de qualification ou de timing = conséquence lourde et non rattrapable pour le salarié.

## Challenge amont

> *Chaque étape AVANT l'étape 4 est-elle couverte par une feature du produit ?*

| Brique amont nécessaire | Couverture | Verdict |
|---|---|---|
| Import dossier + upload documents | Bloc Documents F-1→F-12 — ✅ livré | OK |
| Analyse IA du dossier produisant une synthèse structurée | F-10 — ✅ livré | OK |
| Pré-remplissage IA des champs des outils | F-IA-01 — ✅ livré ; invariant F-246 — 🟡 en cours mais le pattern est livré | OK |
| **Détection IA de la situation pour l'affichage conditionnel** | F-IA-04 — ✅ livré (moteur). **Flags requis** : `abandon_poste_detecte`, `arret_maladie_long_detecte` (niveau 2), `prise_acte_envisagee`, `resiliation_judiciaire_envisagee` (niveau 3). L'audit F-191 (§4, lignes 325-326) les rattache au lot **F-205** « extension Travail FR — 24 flags IA » (Terminée). | ⚠ **À confirmer au découpage** |
| Référentiel des délais procéduraux | F-69 — ✅ livré | OK |

**Aucun trou amont bloquant.** Le seul point ouvert est la **provenance des 4 flags de détection** : s'ils sont déjà émis par F-205, l'amont est 100 % couvert et F-206 = ~8 SF (4 backend + 4 frontend) ; sinon, F-206 absorbe les SF de flags (~10 SF, conformément au découpage indicatif du PRODUCT_SPEC). Ce n'est pas un trou fonctionnel — le moteur F-IA-04 et le pattern de flags existent — mais un **ajustement de périmètre à trancher à l'étape de cadrage-découpage**.

## Challenge aval

> *La sortie des 4 outils est-elle exploitable par les étapes APRÈS l'étape 4 ?*

| Étape aval | Exploite la sortie de F-206 ? | Couverture |
|---|---|---|
| 5. Délais procéduraux | Oui — l'urgence (15 j mise en demeure, prescription) doit produire une échéance datée | F-69 ✅ — invariant n°5 ci-dessous |
| 6. Arbitrage stratégique | Oui — le verdict de chaque outil alimente les pistes stratégiques et le dashboard | F-176 ✅ · F-IA-02 ✅ · F-167 ✅ |
| 7. Rédaction des actes | Oui — lettre de contestation / prise d'acte, conclusions CPH | F-98 ✅ (génère courriers **et** conclusions) |
| 8. Chiffrage | Oui — prise d'acte requalifiée → barème Macron + indemnités ; rappel de congés payés → chiffré **dans** l'outil 2 lui-même | F-DT-09 ✅ · F-DT-25/26 ✅ |

**Aucun trou aval bloquant.** Toute sortie des 4 outils est consommée par une feature livrée. À noter : l'audit qualifie l'outil 4 d'« analyseur + générateur conclusions » — la génération de conclusions n'est **pas** à redévelopper dans F-206 : F-98 (Terminée, 53/53 SF) la couvre ; F-206 fournit l'analyse décisionnelle qui l'alimente.

## STOPs / pré-requis à ajouter au backlog

**Aucun STOP.** Aucun pré-requis fonctionnel manquant.

**Un point de vigilance** (≠ pré-requis backlog) : vérifier au cadrage-découpage la provenance des 4 flags de détection (F-205 vs F-206). Décision de périmètre, pas de blocage.

## Invariants anti-gadget pour la mini-spec

1. **Un outil = une situation métier** — les 4 outils restent 4 sections distinctes. Ne **pas** fusionner prise d'acte et résiliation judiciaire : l'audit les déclare explicitement « outils distincts » (rupture immédiate vs maintien en poste pendant l'instance).
2. **Pré-remplissage IA total (invariant F-246)** — tout champ saisissable de chaque outil est pré-rempli par l'IA : extension du record `TravailExtractedData` (`CaseAnalysisResponse.java`) + prompt `LegalDomainPromptBuilder` + extracteur + DTO frontend + `prefillFromAi()` réel + helper `*-prefill-rules.ts` + badges de provenance `auto_awesome`. Seule exception admise : information absente des pièces uploadées.
3. **`critereCode` F-IA-03 émis par le prompt (invariant F-250)** — tout critère modifiable d'un outil doit voir son `critereCode` émis par le prompt LLM (`CaseAnalysisService` / `AiQuestionService`) ; sinon le cross-check de cohérence naît débranché (bug d'origine de F-DT-36 → F-250). Le garde-fou d'intégrité `critereCode` doit passer.
4. **Affichage conditionnel réel (F-IA-04)** — chaque outil est affiché en mode `CONTEXTUAL` sur détection IA de sa situation, jamais `ALWAYS_ON` (sinon pollution du panel décisionnel — régression évitée par F-165).
5. **Délais procéduraux matérialisés** — l'urgence doit produire une **échéance datée exploitable** via F-69 (date butoir des 15 j post-mise en demeure ; date de prescription de l'action en rappel de congés payés), pas un simple texte. Sans cela, le « P1 urgences » est un gadget.
6. **Tile du dashboard agrégé (F-167)** — chaque outil livre son mapper `DashboardTile` ; sinon outil dormant (régression cataloguée F-180).
7. **Verdict tranché et sourcé** — chaque analyseur rend un verdict actionnable (ex. prise d'acte : `FAVORABLE` / `RISQUÉE` / `DÉFAVORABLE`) assorti d'une justification citant ses sources (F-93), pas un score nu.

## Décision finale

🟢 **GO avec ajustements.** F-206 comble un trou fonctionnel réel à l'étape 4 du workflow (évaluation décisionnelle), sur quatre situations à fort enjeu et sans outil dédié. Toutes les briques amont et aval sont livrées. Ajustement à trancher au cadrage-découpage : provenance des 4 flags de détection (F-205 ou F-206).

**Conséquence PRODUCT_SPEC** : F-206 passe de « À planifier » à « À faire ». Étape suivante : 0 bis — cadrage cohérence écran (les 4 outils ajoutent des sections visibles à l'espace décisionnel du dossier).
