# Mini-spec — F-294 / SF-294-01 — Référentiel de pièces attendues : mécanisme + jointure canonique + contenu Droit du travail FR

> Étape 1 du cycle de gouvernance. Validée AVANT dev. Étape 0 : `SF-294-00-coherence.md` (verdict GO, 2026-06-15).
> **Révision 2026-06-15** : stockage via la table existante `legal_referentials` (type `EXPECTED_PIECES`) sur le modèle `DIVORCE_PIECES` — **pas** de table dédiée (décision d'archi, cf. Notes).

---

## Identifiant

`F-294 / SF-294-01`

## Feature parente

`F-294` — Référentiel de pièces attendues par situation procédurale

## Statut

`draft`

## Date de création

2026-06-15

## Branche Git

`feat/SF-294-01-referentiel-pieces-travail-fr`

---

## Objectif

> En une phrase.

Doter le produit d'un **référentiel de pièces attendues** par `(domaine × pays × stade procédural)`, stocké via le pattern existant `legal_referentials` (type `EXPECTED_PIECES`, fallback Java), l'injecter en amont de la génération des pièces manquantes comme **socle minimum additif** (le LLM complétant au cas d'espèce), et **canoniser les libellés** afin que la jointure de statut F-194 cesse de perdre l'appariement entre deux analyses — première vague de contenu : **droit du travail FR**.

---

## Comportement attendu

### Cas nominal

1. Un dossier porte `legalDomain` (ex. `DROIT_DU_TRAVAIL`), un `country` (workspace, ex. `FR`) et, si renseigné, un `procedureStage` (F-243, ex. `CPH_LICENCIEMENT`).
2. Au run de **Synthèse enrichie** (`EnrichedAnalysisService`), `LegalReferentialService.getExpectedPieces(legalDomain, country, procedureStage)` résout la liste des pièces attendues : **DB-first** (`legal_referentials` type `EXPECTED_PIECES`) puis **fallback Java** (`TravailPieceReferentiel`) si la DB est vide, exactement comme `getDivorcePieces()`.
3. Cette liste (libellés **canoniques**) est injectée dans le contexte du prompt sous une section dédiée : « **Pièces standards attendues pour ce type de procédure — à inclure AU MINIMUM, en réutilisant EXACTEMENT ces libellés ; AJOUTE librement toute autre pièce pertinente au cas d'espèce** ».
4. Le LLM produit `analysis_result.pieces_manquantes` : il **réutilise les libellés canoniques** pour toute pièce du socle, et **forge un libellé libre** pour toute pièce **hors socle** (cas d'espèce) — il n'est jamais bridé.
5. À la matérialisation (`PieceManquanteAlignmentService.materializeForAnalysis`), chaque pièce produite est **canonisée** : si sa forme normalisée (`trim().toLowerCase()`) correspond **exactement** à un libellé du socle (pour la clé du dossier), elle est remplacée par le **libellé canonique** du référentiel avant la jointure F-194. Les pièces sans correspondance gardent leur libellé LLM (comportement F-194 inchangé).
6. La jointure de l'overlay statut (`piece_manquante_status`, clé `piece_libelle_normalise`) apparie alors de façon **déterministe** les pièces du socle d'une analyse à l'autre → une pièce marquée `OBTENUE` ne réapparaît plus `A_DEMANDER` sous un libellé voisin.
7. Le reste de la chaîne est **inchangé** : délais auto `case_deadlines` (F-194), affichage F-289, bouton « Marquer obtenue ».

### Résolution & fallback de maille (`getExpectedPieces`)

- Charge les entrées `EXPECTED_PIECES` pour `(legalDomain × country)` (système + override workspace, via le pattern `findActiveByDomainAndType`).
- Parse `value_json` ; pour chaque pièce, lit le champ `stages` (liste de codes de stade F-243) :
  - `stages` **renseigné** → pièce incluse **si** `procedureStage ∈ stages` ;
  - `stages` **absent / null** → pièce **générique**, incluse **quel que soit** le stade (et même si `procedureStage` est nul) ;
- si `procedureStage` est **nul** (champ F-243 nullable) → ne sont incluses que les pièces **génériques** `(domaine × pays)` ;
- si le couple `(domaine × pays)` n'a **aucune** entrée (DB + fallback Java vides) → **liste vide** → comportement **identique à aujourd'hui** (100 % LLM), jamais d'erreur.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `legalDomain` / `country` non couverts (DB + Java vides) | Socle vide, le LLM génère seul (statu quo), aucune dégradation | n/a (interne) |
| `procedureStage` nul | Socle générique `(domaine × pays)`, jamais d'échec | n/a |
| `value_json` mal formé / exception de parsing | **Fail-open** : log warn, fallback Java puis, à défaut, socle vide ; le run de synthèse aboutit | n/a |
| Exception lors de la canonisation | **Fail-open** : alignement non canonisé (comportement F-194 actuel), run abouti | n/a |

> Pas de nouvel endpoint exposé (cf. Technique) → pas de cas d'erreur HTTP propre à cette SF.

---

## Analyse de cohérence transversale

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| **Référentiel métier `legal_referentials`** | Oui | **Réutilisé** : nouveau `referential_type = EXPECTED_PIECES` sur le modèle `DIVORCE_PIECES`. Pattern DB-first + fallback Java + override workspace conservé. **Pas de nouvelle table** (anti-duplication). |
| **Classe `*Referentiel.java` (fallback)** | Oui | Nouvelle classe `TravailPieceReferentiel` sur le modèle `DivorceChecklistReferentiel` / `ImmigrationPieceReferentiel`. |
| **Autres domaines** (FAMILLE, IMMIGRATION) | Oui (mécanisme), Non (contenu) | **Mécanisme transverse** dès cette SF (type `EXPECTED_PIECES` + `getExpectedPieces` + injection + canonisation valables pour tous domaines/pays). **Contenu** : SF parallèles — `SF-294-02` Travail BE, `SF-294-03` Famille FR/BE, `SF-294-04` Immigration FR/BE. ⚠ Famille a déjà `DIVORCE_PIECES` : la vague Famille devra arbitrer fusion/cohabitation `EXPECTED_PIECES` ↔ `DIVORCE_PIECES` (noté pour SF-294-03). |
| **Autres pays** (BE) | Oui (mécanisme), Non (contenu) | La clé inclut `country` ; contenu BE = pièces réellement attendues en procédure belge (règle « Belgique : pas de miroir FR »), vague ultérieure. |
| **Pipeline IA — prompt pièces manquantes** (F-92) | Oui | Intégré : injection du socle dans le contexte du prompt (pattern d'injection existant F-194 `[Pièces déjà obtenues…]`, F-146 `PiecesPromptContext`). |
| **Overlay statut F-194** (`piece_manquante_status`) | Oui | Intégré : canonisation AVANT la jointure existante ; contrat de statut et endpoint `PUT .../pieces-manquantes/{id}` **inchangés** ; statuts existants non orphelinés. |
| **Affichage F-289** (Vue d'ensemble, bloc attention) | Oui | Lecture seule, **aucun changement de contrat** : F-294 stabilise la source, n'ajoute aucun élément d'écran (→ étape 0 bis non applicable). |
| **Délais F-194 / F-284** (`case_deadlines` `PIECE_A_DEMANDER`) | Oui | Effet de bord **positif** : socle plus stable ⇒ moins de création/suppression erratique de délais. Aucun code à modifier. |
| **Outils décisionnels** (F-IA-04, calculators…) | Non | F-294 n'est pas un outil décisionnel, n'écrit sur aucune table `*_analysis` ni `decision_tool_visibility_rules`. Invariant « 1 outil = 1 situation » intact. |
| **Auth / Workspace / Plans / Navigation** | Non | `legal_referentials` système est non-tenant ; l'override workspace existe déjà ; aucune route ni guard modifiés. |

### Cas spécifique : service partagé / référentiel réutilisable

- **`getExpectedPieces` est transverse** : utilisable par les 3 domaines (le type `EXPECTED_PIECES` n'est pas spécifique au travail). Réutilisation prévue par les SF de contenu 02/03/04.
- **Patterns concurrents** : `DIVORCE_PIECES` (Famille) et `ImmigrationPieceReferentiel` (pièces titre de séjour) couvrent déjà des pièces, mais selon une logique « pièces du dossier » et non « pièces attendues indexées par stade procédural ». Cohabitation assumée en V1 ; convergence Famille à arbitrer en SF-294-03 (classée backlog, pas immédiate).

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature : **mécanisme** transverse (`EXPECTED_PIECES` + service + injection + canonisation) + **contenu** Droit du travail FR.
- [x] Subfeature(s) parallèle(s) pour les cibles restantes (contenu) : `SF-294-02` (Travail BE), `SF-294-03` (Famille FR/BE + arbitrage convergence `DIVORCE_PIECES`), `SF-294-04` (Immigration FR/BE) — à inscrire au backlog F-294.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF **backend pure** (entrées référentiel + résolution + injection prompt + canonisation à la matérialisation). Aucun composant frontend décisionnel, aucune entrée `TOOL_REGISTRY`, aucun nouvel écran. L'affichage des pièces existe déjà (F-289) et n'est pas modifié.

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : pas d'outil décisionnel à champs saisissables. F-294 enrichit le **contexte du prompt** de génération des pièces ; elle ne crée pas de formulaire.

---

## Critères d'acceptation

- [ ] **CA1 (cœur — corrige le défaut (a))** : pour un dossier Travail FR, deux runs de Synthèse enrichie consécutifs produisent, pour une pièce du socle, le **même libellé canonique** ; une pièce marquée `OBTENUE` entre les deux runs **reste `OBTENUE`** et ne réapparaît pas dans le bloc « ce qui requiert ton attention » (F-289).
- [ ] **CA2 (socle)** : pour un dossier Travail FR au stade `CPH_LICENCIEMENT`, la liste injectée contient **au minimum** les pièces standard seedées (cf. Valeurs initiales), même si le LLM ne les avait pas spontanément listées.
- [ ] **CA3 (complétion non bridée)** : le LLM peut toujours **ajouter** des pièces hors socle (cas d'espèce) ; elles apparaissent normalement avec leur libellé libre.
- [ ] **CA4 (non normatif)** : une pièce du socle peut être marquée `NON_APPLICABLE` par l'avocat et ne réapparaît pas comme `A_DEMANDER` au run suivant.
- [ ] **CA5 (fallback)** : un dossier Travail FR **sans `procedureStage`** reçoit le socle **générique** `(DROIT_DU_TRAVAIL × FR)` ; aucun échec.
- [ ] **CA6 (zéro régression hors périmètre)** : un dossier Famille/Immigration ou BE (non encore seedé en `EXPECTED_PIECES`) se comporte **exactement comme aujourd'hui** (socle vide, génération 100 % LLM).
- [ ] **CA7 (fail-open)** : toute exception dans la résolution/canonisation laisse le run de synthèse aboutir (comportement F-194 préservé).
- [ ] **CA8 (statuts existants)** : les `piece_manquante_status` posés avant cette SF ne sont **pas orphelinés** ; au pire ils restent appariés par leur libellé historique.
- [ ] **CA9 (anti-loupé — additif strict, garde-fou central de la crainte PO)** : la liste finale de pièces manquantes est l'**UNION** `(pièces produites par le LLM ∪ socle du référentiel)`. Le référentiel **ne retire jamais** une pièce listée par le LLM ni n'en réduit le nombre. Test : pour un dossier où le LLM liste une pièce spécifique au cas d'espèce **absente du socle**, cette pièce **reste présente** après application du référentiel (`socle ⊆ résultat`, jamais `résultat = socle`). Conséquence : F-294 ne peut, par construction, qu'**égaler ou augmenter** la couverture de pièces vs aujourd'hui — jamais la diminuer.
- [ ] **CA10 (canonisation sans fusion abusive)** : la canonisation remplace un libellé LLM par un libellé canonique **uniquement** sur correspondance **normalisée exacte** (`trim().toLowerCase()`), jamais par rapprochement approximatif/sémantique. Deux pièces de libellés distincts (ex. « Bulletins de paie des 3 derniers mois » vs « Bulletins de paie des 12 derniers mois ») ne sont **jamais fusionnées** → aucune perte de pièce par canonisation.
- [ ] **CA11 (DB source de vérité)** : une entrée `EXPECTED_PIECES` en DB prime sur le fallback Java ; un override workspace (`is_system=false`) prime sur l'entrée système (cohérence F-139, pattern `getDivorcePieces`).

---

## Périmètre

### Hors scope (explicite)

- **Contenu** des domaines Famille et Immigration, et du pays BE → SF-294-02/03/04.
- **Convergence** `EXPECTED_PIECES` ↔ `DIVORCE_PIECES` existant (Famille) → arbitrée en SF-294-03, pas ici.
- **Appariement sémantique / fuzzy** des libellés (au-delà de la normalisation `trim().toLowerCase()`) → durcissement futur si signal.
- **Interface d'administration** du référentiel (CRUD super-admin) → non requis en V1, seedé par migration (l'override workspace reste possible via le mécanisme `legal_referentials` existant).
- **Migration/réécriture** des `piece_manquante_status` historiques.
- Toute injection de la **stratégie F-286** dans les conclusions (sujet distinct, décision : Niveau 0 / statu quo).

---

## Valeurs initiales

> Seed du référentiel — vague 1 : Droit du travail FR. Entrées dans `legal_referentials`, type `EXPECTED_PIECES`. `entry_key` = code stable, `label` = libellé canonique, `value_json` = `{"stages":[...]|absent, "obligatoire":bool, "ordre":int}`.

Exemple de contenu seedé pour `(DROIT_DU_TRAVAIL × FR)` (liste indicative, à figer au dev) :

| `entry_key` (code) | `label` (canonique) | `value_json` |
|--------------------|---------------------|--------------|
| `CONTRAT_TRAVAIL` | Contrat de travail | `{"stages":["CPH_LICENCIEMENT"],"obligatoire":true,"ordre":1}` |
| `BULLETINS_PAIE_12M` | Bulletins de paie des 12 derniers mois | `{"stages":["CPH_LICENCIEMENT"],"obligatoire":true,"ordre":2}` |
| `LETTRE_LICENCIEMENT` | Lettre de licenciement | `{"stages":["CPH_LICENCIEMENT"],"obligatoire":true,"ordre":3}` |
| `CONVOCATION_ENTRETIEN_PREALABLE` | Convocation à l'entretien préalable | `{"stages":["CPH_LICENCIEMENT"],"obligatoire":true,"ordre":4}` |
| `SOLDE_TOUT_COMPTE` | Reçu pour solde de tout compte | `{"stages":["CPH_LICENCIEMENT"],"obligatoire":true,"ordre":5}` |
| `CERTIFICAT_TRAVAIL` | Certificat de travail | `{"stages":["CPH_LICENCIEMENT"],"obligatoire":true,"ordre":6}` |
| `ATTESTATION_FRANCE_TRAVAIL` | Attestation France Travail | `{"stages":["CPH_LICENCIEMENT"],"obligatoire":true,"ordre":7}` |
| `CONVENTION_COLLECTIVE` | Convention collective applicable | `{"obligatoire":true,"ordre":8}` (générique — `stages` absent) |

Comportements à la création :
- Entrées **système** : `is_system=true`, `workspace_id=NULL`, `is_active=true`, `legal_domain='DROIT_DU_TRAVAIL'`, `country='FRANCE'` (valeur conforme aux entrées existantes type DIVORCE).
- `description` (langage avocat) remplie pour chaque INSERT système (exigence readiness `legal_referentials`).
- `stages` **absent** dans `value_json` = pièce **générique** (socle de fallback, incluse à tout stade).

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `referential_type` | Oui | 100 | `EXPECTED_PIECES` | — | — |
| `entry_key` (code) | Oui | 200 | UPPER_SNAKE_CASE, stable | Oui sur `(legal_domain, referential_type, country, workspace_id, entry_key)` | — |
| `label` | Oui | 500 | non vide | — | normalisation à la jointure (`trim().toLowerCase()`, réutilise `PieceManquanteAlignmentService.normalize`) |
| `value_json.stages[]` | Non | — | codes de stade `ProcedureStageCatalog` ; absent = générique | — | — |
| `value_json.obligatoire` | Non | — | booléen (défaut true) | — | — |
| `legal_domain` | Oui | 50 | enum `LegalDomain` | — | — |
| `country` | Oui | 20 | `FRANCE` / `BELGIQUE` (convention table) | — | — |

---

## Technique

### Endpoint(s)

> **Aucun nouvel endpoint REST.** Le référentiel est lu par le service interne de pipeline IA. (L'override workspace passe par le mécanisme `legal_referentials` existant ; aucune API d'admin nouvelle.)

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `legal_referentials` (existante) | INSERT (seed) + SELECT | Nouveau `referential_type = EXPECTED_PIECES`. **Aucun changement de schéma** : colonnes existantes (`legal_domain`, `country`, `entry_key`, `label`, `value_json`, `description`, `is_system`…) suffisent. |
| `piece_manquante_status` (F-194) | SELECT (inchangé) | Aucune modification de schéma ; la canonisation agit **en amont** de la jointure existante. |
| `case_analyses.analysis_result` | lecture (inchangé) | `pieces_manquantes` désormais alimenté avec libellés canoniques pour les pièces du socle. |

### Migration Liquibase

- [x] Oui — `XXX-f294-expected-pieces-travail-fr.xml` : **INSERT seul** dans `legal_referentials` (pas de `createTable`). Vérifier les noms **exacts** des colonnes vs `048-create-legal-referentials.xml` (règle migration INSERT). Plage d'UUID dédiée sans collision. Réversible (DELETE par `referential_type='EXPECTED_PIECES'` et plage d'UUID).

### Backend — éléments à créer / modifier

- **Record** `ExpectedPiece(code, label, country, List<String> stages, boolean obligatoire, int ordre)` (sur le modèle `DivorcePiece`).
- **Classe fallback** `TravailPieceReferentiel` (sur le modèle `DivorceChecklistReferentiel`) — pièces Travail FR en dur.
- **`LegalReferentialService.getExpectedPieces(legalDomain, country, procedureStage)`** — DB-first + fallback Java + filtrage par stade (cf. Résolution).
- **`EnrichedAnalysisService`** (ou son prompt builder) — injection de la section « Pièces standards attendues… » dans le contexte.
- **`PieceManquanteAlignmentService.materializeForAnalysis`** — étape de **canonisation** avant la jointure (correspondance normalisée exacte → libellé canonique).

### Composants Angular (si applicable)

- Aucun. Affichage inchangé (F-289).

---

## Plan de test

### Tests unitaires

- [ ] `LegalReferentialService.getExpectedPieces` — DB renseignée : retourne les pièces du `(domaine × pays)`, filtrées par `procedureStage`.
- [ ] `getExpectedPieces` — pièce générique (`stages` absent) incluse quel que soit le stade, et quand `procedureStage` est nul.
- [ ] `getExpectedPieces` — DB vide → **fallback Java** `TravailPieceReferentiel` (CA11).
- [ ] `getExpectedPieces` — override workspace (`is_system=false`) prime sur système (CA11).
- [ ] `getExpectedPieces` — domaine/pays non couvert → liste vide, pas d'exception.
- [ ] `getExpectedPieces` — `value_json` mal formé → fail-open (fallback puis vide), pas d'exception propagée.
- [ ] Injection prompt — le contexte construit contient les libellés canoniques du socle (section dédiée).
- [ ] `PieceManquanteAlignmentService` — canonisation : pièce LLM dont la normalisation matche le socle → remplacée par libellé canonique ; pièce hors socle → libellé conservé (CA10).
- [ ] `PieceManquanteAlignmentService` — jointure : statut `OBTENUE` sur libellé canonique préservé sur un 2ᵉ alignement (CA1).

### Tests d'intégration

- [ ] Dossier Travail FR `CPH_LICENCIEMENT` : `analysis_result.pieces_manquantes` matérialisé contient au minimum les pièces seedées (CA2).
- [ ] Dossier Famille FR (pas d'`EXPECTED_PIECES` seedé) : comportement identique à avant F-294 (CA6).
- [ ] Pièce marquée `NON_APPLICABLE` non réaffichée au run suivant (CA4).
- [ ] Pièce hors socle listée par le LLM toujours présente après canonisation (CA9).

### Isolation workspace

- [x] Applicable (indirect) — les entrées système `EXPECTED_PIECES` sont non-tenant (`workspace_id=NULL`) et ne contiennent aucune donnée client ; l'override workspace suit le mécanisme `legal_referentials` déjà testé (F-139/F-140). L'isolation des statuts reste celle de F-194, **inchangée**.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** (au sens Auth / Workspace context / Plans / Navigation). SF backend isolée touchant le pipeline IA (génération des pièces), le référentiel `legal_referentials` (nouveau type) et la matérialisation F-194.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — pas de changement auth / workspace / navigation. (Validation fonctionnelle via tests d'intégration backend + test manuel staging sur dossier Travail FR.)

---

## Dépendances

### Subfeatures bloquantes

- Aucune. Étape 0 `SF-294-00` GO ; briques amont (F-243, `ProcedureStageCatalog`, `CaseFile.legalDomain`, `legal_referentials`/`LegalReferentialService`) et aval (F-92, F-194, F-289) livrées.

### Questions ouvertes impactées

- [ ] Aucune entrée de `docs/OPEN_QUESTIONS.md` concernée.

---

## Notes et décisions

- **Décision d'archi (2026-06-15) — stockage via `legal_referentials`, PAS de table dédiée.** Le produit modélise déjà des pièces par situation via `legal_referentials` (`DIVORCE_PIECES`, `ImmigrationPieceReferentiel`) avec le pattern DB-source-de-vérité + fallback Java + override workspace (`LegalReferentialService`, F-139/F-140). Réutiliser ce pattern (nouveau `referential_type=EXPECTED_PIECES`) respecte la règle CLAUDE.md « ne pas réinventer un mécanisme existant », évite une 2ᵉ table de référentiel de pièces, et fournit gratuitement : code stable (`entry_key`), libellé canonique (`label`), `description` avocat, override workspace, fail-open. Le manque d'index natif par stade est couvert par `value_json.stages` + filtrage en mémoire (volume faible), sur le modèle de `DivorceEtape.ordre`. (Une exploration avait suggéré une table dédiée `expected_piece_catalog` ; écartée au profit de la cohérence de pattern.)
- **Clé canonique** = `(legalDomain × country × procedureStage)` via `entry_key` + filtrage `stages`, fallback `(legalDomain × country)` — adossée à F-243 / `ProcedureStageCatalog`, **pas de 2ᵉ taxonomie** (invariant #5 étape 0).
- **Correction du défaut (a)** = deux leviers combinés : (1) prompt « réutiliser EXACTEMENT les libellés canoniques », (2) canonisation défensive par correspondance normalisée exacte. L'appariement sémantique au-delà de la normalisation est hors scope.
- **Crainte « loupé » (PO 2026-06-15)** : le référentiel est **ADDITIF (union)**, jamais substitutif (CA9). Il ne peut que réduire les oublis du LLM (défaut b), pas en créer. Le risque résiduel n'est pas l'oubli mais le **surplus** (pièce du socle non pertinente au cas) → traité par `NON_APPLICABLE` (non normatif). La canonisation exacte (CA10) ne fusionne jamais deux pièces distinctes → aucune perte.
- **Transition** : aucune migration des statuts existants ; le socle améliore les runs futurs sans orpheliner l'historique (CA8).
- Respect des 7 invariants anti-gadget de `SF-294-00-coherence.md`.
