# F-294 — Cadrage cohérence (étape 0)

> Produit par la skill `ai-skills/feature-coherence-challenger.md` (étape 0 du cycle de gouvernance). Ne mesure que l'**existence fonctionnelle** des briques (livrée OU au backlog), jamais l'usage prod.
> Date : 2026-06-15 · Source feature : `docs/PRODUCT_SPEC.md:346` (`| F-294 |`).

## Verdict : **GO**

La brique amont indispensable (clé de référentiel = domaine × situation procédurale) **existe fonctionnellement et techniquement** dans le produit, et la sortie est consommable par F-194 / F-289 sans casser l'overlay statut. Aucun trou bloquant amont ni aval. Le GO est **conditionné** au respect strict des invariants anti-gadget ci-dessous (notamment : la jointure F-194 sur libellés canoniques et la non-altération du contrat de statut). Pas de pré-requis backlog à créer.

---

## Intention métier (1 phrase)

Fiabiliser la liste des « pièces manquantes / attendues » d'un dossier en la fondant sur un **référentiel canonique stable** par (domaine juridique × situation procédurale), le LLM venant **compléter** au cas d'espèce et non plus la produire intégralement à chaque Synthèse enrichie.

---

## Workflow métier réel de l'utilisateur cible (avocat)

> Source : `docs/business/parcours-ecran-dossier.md` (parcours réel de l'avocat, étapes 1-12, sourcé par les cadrages F-243 / F-98 / F-194 / F-289) + libellé F-194 `docs/PRODUCT_SPEC.md:284`. Les étapes purement « métier cabinet » non directement attestées par une feature sont marquées **⚠ hypothèse à valider** (piège 3 de la skill).

1. L'avocat reçoit un nouveau dossier d'un client (mandat, premières pièces). ⚠ hypothèse à valider (hors app).
2. Il identifie **la nature de la procédure** : domaine (travail / immigration / famille), pays, juridiction, stade, position (demandeur/défendeur). → couvert produit par F-243 (stade procédural).
3. Connaissant le type de procédure, l'avocat sait **quelles pièces sont standards / obligatoires** pour ce type de litige (réflexe métier : un licenciement CPH ⇒ contrat, bulletins, lettre de licenciement, etc.). ⚠ hypothèse à valider (réflexe métier hors outil).
4. Il importe les pièces déjà en sa possession dans le dossier.
5. Il lance l'analyse / Synthèse enrichie → le produit lui restitue une **liste de pièces manquantes** (`analysis_result.pieces_manquantes`).
6. L'avocat compare cette liste à son attendu métier : repère les pièces encore à réclamer et écarte celles non applicables à son espèce.
7. Il **marque le statut** de chaque pièce : à demander / obtenue / non applicable (overlay avocat).
8. Une **échéance de relance** est posée pour chaque pièce à demander (butoir).
9. L'avocat **relance le client** pour les pièces manquantes. ⚠ hypothèse à valider (geste hors app : mail/téléphone client).
10. Le client envoie des pièces ; l'avocat les importe → nouvelle vague de pièces (constitution incrémentale).
11. Il **relance une analyse** → la liste de pièces doit refléter ce qui a déjà été obtenu (ne pas re-réclamer une pièce reçue) et **conserver les statuts** posés en (7).
12. Il **pilote l'avancement** de la collecte depuis une vue de synthèse (« ce qui requiert ton attention » : pièces à obtenir).
13. Une fois le dossier suffisamment constitué, il passe aux **outils décisionnels** puis à la **génération des conclusions** (état terminal métier).

---

## Cartographie features actuelles ↔ workflow

| # | Étape workflow métier | Feature(s) LegalCase | Statut |
|---|---|---|---|
| 1 | Réception dossier client | hors app | — ⚠ hyp. |
| 2 | Qualification de la procédure (domaine × juridiction × stade × position) | **F-243** stade procédural (`CaseFile.procedureJuridiction/procedureStage/procedurePosition`, `ProcedureStageCatalog` indexé `(domain,country)`) + `CaseFile.legalDomain` | ✅ Livrée |
| 3 | Connaissance des pièces standards de la procédure | **F-294 (la feature challengée)** — n'existe pas encore comme socle structuré | 🟡 Backlog |
| 4 | Import des pièces détenues | F-43 import dossier / pièces | ✅ Livrée |
| 5 | Restitution liste pièces manquantes | **F-92** détection pièces manquantes (`analysis_result.pieces_manquantes` via `EnrichedAnalysisService`) | ✅ Livrée |
| 6 | Tri attendu / non applicable | F-194 (statuts) | ✅ Livrée |
| 7 | Marquage statut pièce (à demander / obtenue / non applicable) | **F-194** overlay statut (`piece_manquante_status`, `PieceManquanteAlignmentService.materializeForAnalysis`) | ✅ Livrée |
| 8 | Échéance de relance auto | F-194 (délais `case_deadlines` source `PIECE_A_DEMANDER`, J+14) + **F-69 / F-284** | ✅ Livrée |
| 9 | Relance client | hors app | — ⚠ hyp. |
| 10 | Nouvelle vague de pièces | **F-283** dossier vivant / vagues de pièces | ✅ Livrée |
| 11 | Ré-analyse préservant les statuts | F-194 (matérialisation par jointure libellé normalisé) | ✅ Livrée (point faible = jointure, cf. challenge aval) |
| 12 | Pilotage de la collecte | **F-289** Vue d'ensemble, bloc « ce qui requiert ton attention » (`OverviewService`, `CaseOverviewComponent`) | ✅ Livrée |
| 13 | Outils décisionnels → conclusions | F-177 / F-98 | ✅ Livrées |

---

## Position de la nouvelle feature

F-294 s'insère à l'**étape 3** du workflow (« connaissance des pièces standards de la procédure »), comme **brique amont** entre la qualification de la procédure (étape 2, F-243) et la restitution de la liste (étape 5, F-92). Elle ne crée pas une nouvelle étape métier : elle **fiabilise le socle** de l'étape 5 et **stabilise** la jointure de l'étape 11.

Dit autrement : aujourd'hui l'étape 3 est entièrement portée par le réflexe de l'avocat + une génération LLM volatile à l'étape 5 ; F-294 matérialise ce socle dans le produit, sous la qualification F-243.

---

## Challenge amont

**Question : chaque étape AVANT F-294 (étapes 1-2) est-elle couverte par une feature existante ?**

L'enjeu amont central, explicitement demandé : **existe-t-il dans le produit une notion de « situation procédurale » / « type de procédure » exploitable comme clé du référentiel ?** Réponse **OUI** — vérifié au PRODUCT_SPEC et dans le code :

| Candidat clé | Existence | Source |
|---|---|---|
| Domaine juridique | ✅ `CaseFile.legalDomain` | `backend/.../casefile/CaseFile.java:34` |
| Pays (workspace) | ✅ utilisé comme clé secondaire du catalogue | `ProcedureStageCatalog.java:18,59` (`key(domain,country)`) |
| Juridiction | ✅ `CaseFile.procedureJuridiction` (F-243) | `CaseFile.java:54` |
| Stade procédural | ✅ `CaseFile.procedureStage` (F-243) | `CaseFile.java:59-60` |
| Position juridique | ✅ `CaseFile.procedurePosition` (F-243) | `CaseFile.java:62-64` |
| Type de rupture (sous-cas Travail) | ✅ `compensation_data.type_rupture` | `DecisionToolVisibilityService.java:153-154` |
| Référentiel `(domaine × pays)` outillé | ✅ `ProcedureStageCatalog` (6 combinaisons exhaustives) | `ProcedureStageCatalog.java:18,38` |

**Conclusion amont : aucun trou bloquant.** La clé du référentiel canonique de F-294 existe déjà sous forme exploitable. La granularité recommandée pour la clé est **(legalDomain × country × procedureStage)** — exactement la maille de F-243 / `ProcedureStageCatalog`, ce qui garantit la cohérence et évite d'inventer une 2ᵉ taxonomie de procédure (anti-doublon). Un raffinement par `type_rupture` est possible comme sous-clé Travail mais non obligatoire au socle.

Étapes 1 et 9 (réception / relance client) sont hors app et n'ont jamais été des pré-requis produit (cf. cas F-999 de la skill).

⚠ Point d'attention amont (non bloquant, à trancher en mini-spec) : F-243 est **statique et facultatif** — `procedureStage` est **nullable** (`CaseFile.java:59`). Si l'avocat n'a pas renseigné le stade, le référentiel doit dégrader proprement vers la maille `(legalDomain × country)` seule (toujours disponible) plutôt que d'échouer. C'est un comportement de fallback, pas un pré-requis manquant.

---

## Challenge aval

**Question : la sortie de F-294 (liste de pièces canoniques) est-elle exploitable par les étapes AVAL (F-92 → F-194 → F-289) sans casser l'existant ?**

1. **Vers F-92 / `EnrichedAnalysisService` (étape 5)** : le socle canonique doit être **injecté dans le contexte du prompt** comme liste de référence (« pièces standards attendues — au minimum »), le LLM venant compléter au cas d'espèce. C'est le pattern d'injection déjà utilisé par F-194 (`[Pièces déjà obtenues — ne pas réclamer]`, etc.) et par F-146 (`PiecesPromptContext`). Sortie exploitable. ✅

2. **Vers F-194 / `PieceManquanteAlignmentService` (étapes 7 & 11)** — point névralgique, demandé explicitement : la jointure de l'overlay statut se fait aujourd'hui par **libellé normalisé** = `s.trim().toLowerCase()` (`PieceManquanteAlignmentService.java:237-239`), via `piece_libelle_normalise`. C'est précisément cette jointure qui rate quand le LLM **reformule** un libellé (défaut (a) du test 2026-06-15). Pour que F-294 corrige réellement le défaut sans casser F-194 :
   - les pièces du socle doivent porter un **libellé canonique stable** (idéalement un **code/identifiant stable**, pas seulement un texte), réutilisé tel quel dans `analysis_result.pieces_manquantes` ;
   - le LLM doit être contraint à **réutiliser le libellé canonique** quand une pièce correspond au socle, et ne forger un libellé libre que pour les pièces hors socle (cas d'espèce) ;
   - la jointure F-194 (`piece_libelle_normalise`) doit **continuer de fonctionner** : libellé canonique ⇒ normalisation stable ⇒ appariement fiable. Les statuts existants (`A_DEMANDER` / `OBTENUE` / `NON_APPLICABLE`) sont **préservés** (contrat F-176 inchangé).
   - ⚠ Migration de l'existant : les `piece_manquante_status` déjà posés sont indexés sur des libellés LLM historiques ; F-294 ne doit pas les orpheliner silencieusement. À traiter en mini-spec (au pire, ils restent appariés à l'ancienne, le socle améliore les futurs runs).

   Sortie exploitable **sous réserve** que la mini-spec impose le libellé canonique comme clé de jointure. ✅ (avec invariant dur ci-dessous)

3. **Vers F-194 délais auto + F-69/F-284 (étape 8)** : les `case_deadlines` source `PIECE_A_DEMANDER` sont dérivées du statut, pas du texte — un socle plus stable **réduit** la création/suppression erratique de délais (effet de bord positif). ✅

4. **Vers F-289 Vue d'ensemble (étape 12)** : le bloc « ce qui requiert ton attention » lit les pièces via `OverviewService` (lecture seule, fail-open, aucune table). F-294 n'ajoute pas de source nouvelle à afficher : elle stabilise la source existante. Le bouton « Marquer obtenue » continue d'opérer sur `piece_manquante_status`. Aucun changement de contrat d'affichage requis. ✅

**Conclusion aval : aucun trou bloquant.** La sortie est consommable par les 3 consommateurs (F-92, F-194, F-289) à condition de figer la clé de jointure canonique. Aucune feature aval à créer.

---

## STOPs / pré-requis à ajouter au backlog

**Aucun.** Toutes les briques amont (clé de référentiel) et aval (consommateurs) existent et sont livrées. F-294 passe `Backlog → À faire`.

Décisions à porter par la **mini-spec** (ne sont PAS des pré-requis backlog) :
- Maille exacte de la clé canonique (recommandé : `legalDomain × country × procedureStage`, sous-clé `type_rupture` optionnelle) + comportement de fallback quand `procedureStage` est nul.
- Forme du libellé canonique : **code stable + label affiché** (recommandé) vs label seul.
- Stratégie de transition des `piece_manquante_status` existants (pas de réapparition de pièces déjà obtenues lors du premier run post-F-294).

---

## Invariants anti-gadget pour la mini-spec

1. **Jointure canonique obligatoire** — toute pièce du socle réutilise son **libellé canonique** (idéalement adossé à un code stable) dans `analysis_result.pieces_manquantes`, de sorte que la jointure `piece_libelle_normalise` de F-194 (`PieceManquanteAlignmentService.normalize`) apparie de façon déterministe. Une pièce marquée `OBTENUE` ne doit jamais réapparaître `A_DEMANDER` sous un libellé voisin (défaut (a) corrigé = critère d'acceptation).
2. **Ne casse pas l'overlay statut F-194** — les statuts `A_DEMANDER` / `OBTENUE` / `NON_APPLICABLE` et le contrat acte-pur F-176 (`PUT .../pieces-manquantes/{id}`) sont préservés ; les statuts déjà posés ne sont pas orphelinés silencieusement.
3. **Socle = suggestion, non normatif** — le référentiel garantit un **minimum** de pièces standards, pas un plafond. L'avocat reste maître : il peut marquer une pièce du socle `NON_APPLICABLE`.
4. **Le LLM complète, ne plafonne pas** — le LLM peut toujours **ajouter** des pièces hors socle (cas d'espèce) ; F-294 le **socle**, ne le bride pas.
5. **Clé adossée à F-243, pas une nouvelle taxonomie** — la clé réutilise `legalDomain` + le référentiel `ProcedureStageCatalog (domain × country × stade)`. Interdiction d'introduire une 2ᵉ taxonomie de « situation procédurale » concurrente (anti-doublon).
6. **Fallback de maille** — si `procedureStage` est nul (champ F-243 nullable), dégrader vers `(legalDomain × country)` ; ne jamais échouer ni vider la liste.
7. **Lecture / suggestion seule, n'altère aucun outil décisionnel** — F-294 alimente le contexte de génération des pièces ; elle n'écrit sur aucune table `*_analysis` ni `decision_tool_visibility_rules`, ne déplace/masque aucun outil. Invariant produit « 1 outil = 1 situation » intact.

---

## Décision finale

**GO** (conditionné aux 7 invariants ci-dessus). F-294 passe `Backlog → À faire`. La clé amont existe (F-243 + `legalDomain` + `ProcedureStageCatalog`), la sortie est consommable par F-92 / F-194 / F-289 sans rupture de contrat, et la feature corrige des défauts réels constatés au test 2026-06-15 (instabilité des libellés, absence de socle exhaustif). Aucun pré-requis backlog à créer. Enchaîner l'étape 0 bis (cohérence écran — F-294 a un impact écran indirect via F-92/F-289, à confirmer) puis la mini-spec SF-294-01.
