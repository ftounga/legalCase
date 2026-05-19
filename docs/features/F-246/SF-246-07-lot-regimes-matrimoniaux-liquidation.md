# Mini-spec — [F-246 / SF-246-07] Pré-remplissage IA — Lot Régimes matrimoniaux & liquidation FR (Famille FR)

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Découpage de référence : `docs/features/F-246/cadrage-decoupage.md` (§3.1 ligne SF-246-07, vague 2).
> **SF de lot** : 3 outils — `communaute-universelle` (F-FA-16), `recompenses` (F-FA-15),
> `partage-judiciaire` (F-FA-17) — adossés au **même record** `FamilleExtractedData` et au
> **même prompt** `FAMILLE_INSTRUCTION`.

---

## Identifiant

`F-246 / SF-246-07`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-246-07-lot-regimes-matrimoniaux-prefill`

---

## Objectif

> En une phrase : que fait cette subfeature ?

Compléter le pré-remplissage IA des 3 outils décisionnels régimes matrimoniaux / liquidation (`communaute-universelle`, `recompenses`, `partage-judiciaire` — F-FA-15/16/17) en faisant extraire par le pipeline IA la valeur de la communauté, le régime matrimonial et la valeur des biens en indivision aujourd'hui absents de `FamilleExtractedData`.

---

## Comportement attendu

### Cas nominal

1. L'avocat lance l'analyse IA d'un dossier de droit de la famille FR comportant un contrat de mariage, une liquidation de régime ou une indivision.
2. Le pipeline IA (prompt `LegalDomainPromptBuilder.FAMILLE_INSTRUCTION`) extrait, dans `famille_extracted_data`, un sous-objet `regime_matrimonial_detection` regroupant le régime, la valeur de la communauté et les données d'indivision.
3. L'extracteur `extractFamilleData()` parse ce sous-objet en champs typés du record `FamilleExtractedData`.
4. Le DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`) expose les champs ; les 3 entrées `TOOL_REGISTRY` passent déjà `aiData: ctx.synthesis?.familleExtractedData`.
5. À l'ouverture de l'un des 3 outils, `prefillFromAi()` renseigne les champs détectables ; un badge `auto_awesome` s'affiche par champ pré-rempli.
6. L'avocat peut modifier toute valeur : `onXxxChange()` remet `provenance<Field>` à `null` et déclenche la revérification F-IA-03.
7. Le badge « Pré-rempli par l'IA (N champs) » du panel F-IA-04 reflète `getPrefillCount()` de chaque outil.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Le LLM ne détecte pas de régime matrimonial | Sous-objet `regime_matrimonial_detection` à `null` ; `prefillFromAi()` no-op gracieux ; `getPrefillCount()` = 0 ; aucun badge | n/a |
| Régime hors énumération | `null` (fail-open) ; pas de pré-fill du champ régime | n/a |
| Valeur de communauté / biens en indivision ≤ 0 ou aberrante | `positiveDoubleOrNull()` → `null` (jamais `0` — invariant cadrage §5.2) | n/a |
| Nombre de coïndivisaires négatif ou aberrant (> 50) | `boundedIntOrNull()` garde de plage `[0, 50]` → `null` | n/a |
| Dossier de famille belge | Champs FR restent `null` (le prompt impose null hors FR) ; outils non affichés pour la BE | n/a |
| `famille_extracted_data` absent du JSON IA | `extractFamilleData()` retourne `null` ; outils affichés formulaire vierge | n/a |
| `aiData` arrive après le premier rendu | `prefillFromAi()` réinvoqué dans `ngOnChanges()` | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : `regimeMatrimonialDetecte` est lue par `recompenses` (F-FA-15) — et est potentiellement utile à d'autres outils Famille (révisions, liquidation). Champ ajouté **une fois** au record ; les autres outils qui en bénéficieront sont traités par leurs SF respectives sans re-modification du record (champ déjà présent). Le flag `regime_communaute_universelle_detecte` (F-200) existe et pilote la **visibilité** de `communaute-universelle` — finalité distincte du pré-fill, pas de doublon.
- [x] **Autres pays** : France uniquement. Les régimes matrimoniaux BE (communauté légale BE, liquidation-partage BE) relèvent des outils BE F-217 — hors périmètre. Champs `null` pour la BE.
- [x] **Autres domaines** : non applicable — concept propre au droit patrimonial de la famille FR.
- [x] **Autres UI patterns** : pré-remplissage IA (pattern canonique), badges `auto_awesome`, alertes F-IA-03.
- [x] **Autres flows transversaux** : aucun.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : `FamilleExtractedData` dans `divorce-accepte.model.ts` — extension + **réalignement** (retrait des champs aspirationnels `valeurCommunauteEurDetectee`, `regimeMatrimonialDetecte`, `valeurBiensIndivisionEur` déclarés sans source backend).
- [x] **Record / DTO backend** : `CaseAnalysisResponse.FamilleExtractedData` + builder F-234.
- [x] **Service / logique métier** : `extractFamilleData()`.
- [x] **Entité JPA + schéma DB** : non applicable — `familleExtractedData` sérialisé dans la synthèse IA. Inputs des 3 outils persistés par leurs endpoints existants (inchangés).
- [x] **Tests existants** : helpers `communaute-universelle-section-prefill-rules.spec.ts`, `recompenses-section-prefill-rules.spec.ts`, `partage-judiciaire-section-prefill-rules.spec.ts`, tests `extractFamilleData()`. Tous mis à jour.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : oui — régime, valeur de communauté et valeur d'indivision croisables. `coherenceAlerts` étendu sur les 3 outils.
- [x] **Refresh dashboard (F-IA-02)** : inchangé — `triggerRefresh()` déjà câblé.
- [x] **Pré-remplissage IA** : objet de la SF.
- [x] **Persistance des inputs** : inchangée — endpoints F-FA-15/16/17 existants.
- [x] **Masquage conditionnel selon type** : inchangé — visibilité F-IA-04 déjà gérée (FR + flags F-200).
- [x] **Alertes actives après calcul** : gate `coherenceAlerts` = `!this.showForm()` uniquement — vérifié sur les 3 composants.

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — aucun nouveau composant partagé, service ou endpoint.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `communaute-universelle` (F-FA-16), `recompenses` (F-FA-15), `partage-judiciaire` (F-FA-17) | Oui | Intégrés dans cette SF de lot |
| Flag `regime_communaute_universelle_detecte` (F-200) | Non | Flag de visibilité — finalité distincte du pré-fill |
| Outils Régimes BE (F-217) | Non | Régime BE distinct — champs `null` en BE |
| Outils successions F-FA-24 | Non | Sous-domaine distinct — traités par SF-246-06 |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s) créée(s)
- [ ] Backlog VN
- [x] Non applicable aux autres cibles (justification explicite ci-dessus)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [ ] **Non applicable** — la SF livre 3 parties frontend décisionnelles.

### 1. Cohérence visuelle

- [x] **Palette statut** : navy/or info, vert OK, rouge réservé aux alertes critiques — conservé.
- [x] **Datepicker** : `<input type="date">` natif — pas de `MatDatepicker` (cette SF n'ajoute pas de date).
- [x] **Typographie** : `JetBrains Mono` pour `baseJuridique` / `formule`, `Inter` pour le reste — conservé.
- [x] **Gate `workspaceCountry`** : outils FR-only — bannière info en cas de mismatch BE (existant).
- [x] **Erreurs** : `MatSnackBar` — pas d'`alert()` / `confirm()`.
- [x] **Refresh dashboard** : `triggerRefresh()` dans le `next:` du POST — existant, inchangé.

### 2. Pré-fill IA (OBLIGATOIRE)

- [x] `@Input() aiData?: FamilleExtractedData | null` — déjà typé strictement sur les 3 composants.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` **ET** `ngOnChanges()`.
- [x] Signaux `provenance<Field>` : `provenanceValeurCommunaute`, `provenanceRegimeMatrimonial`, `provenanceValeurBiensIndivision`, `provenanceNombreCoindivisaires`.
- [x] Badge `auto_awesome` « Pré-rempli depuis l'analyse » par champ pré-rempli.
- [x] Handler `onXxxChange()` par champ qui remet `provenance<Field>` à `null`.

### 3. Validation F-IA-03 (OBLIGATOIRE)

- [x] `coherenceAlerts` étendu sur les 3 outils pour les champs valeur / régime pré-remplis.
- [x] Hiérarchie F-96 > Question IA > IA détection > Pièce manquante ; convergence → `'MULTI'`.
- [x] `<app-coherence-popover-trigger>` câblé sur les champs concernés.
- [x] Helper partagé `CoherenceAlertBuilder`.

### 4. TOOL_REGISTRY symétrique + `getPrefillCount(input)`

- [x] Les 3 entrées F-FA-15/16/17 déjà présentes dans `TOOL_REGISTRY` ; `inputs(ctx)` passe déjà `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`.
- [x] Static `getPrefillCount(input)` de chaque composant : refactorisé pour appeler `computePrefillCount()` enrichi.
- [x] Parité stricte `getPrefillCount()` ↔ `prefillFromAi()` sur les 3 outils.
- [x] Tests Jest par outil : (a) 0 champ, (b) partiel, (c) nominal.
- [x] Les 3 `tool_id` déjà présents dans `KNOWN_FRONTEND_TOOL_IDS` du test `DecisionToolVisibilityIntegrityIT` — pas de migration.

### 5. Parité des domaines métier (niveau ≥ 5)

- [x] Niveau des outils : `communaute-universelle` et `recompenses` niveau **5** (analyse de régime / récompenses) ; `partage-judiciaire` niveau **5** (analyse de recevabilité du partage judiciaire).

| Domaine | Équivalent existe ? | Si non → action |
|---------|---------------------|-----------------|
| Droit du travail | Non | Les régimes matrimoniaux n'ont pas de transposition en droit du travail — concept non pertinent |
| Immigration | Non | Concept non pertinent en droit des étrangers |
| Famille | Oui (F-FA-15/16/17) | Ce sont les outils de cette SF |

> La SF complète le pré-fill d'outils existants — la parité de domaine a été tranchée à leur création.

---

## Champs IA à extraire (pré-remplissage)

- [ ] **Aucun pré-remplissage** — non, la SF crée le pré-remplissage des 3 outils.

| Champ du formulaire | Outil(s) consommateur(s) | Type | Champ source du record `FamilleExtractedData` | Extension requise |
|---------------------|--------------------------|------|------------------------------------------------|-------------------|
| valeur de la communauté (€) | `communaute-universelle` | nombre (€) | `valeurCommunauteEurDetectee` (`Double`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| régime matrimonial | `recompenses` | texte (énum) | `regimeMatrimonialDetecte` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| valeur des biens en indivision (€) | `partage-judiciaire` | nombre (€) | `valeurBiensIndivisionEur` (`Double`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| nombre de coïndivisaires | `partage-judiciaire` | nombre (entier `[0, 50]`) | `nombreCoindivisairesDetecte` (`Integer`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

- [x] Pour chaque champ valeur / texte à pré-remplir non encore présent, l'extension du record `FamilleExtractedData` **et** du prompt `LegalDomainPromptBuilder` (`FAMILLE_INSTRUCTION`) est explicitement dans le périmètre de cette SF.

> **Note couplage record partagé** : voir « Notes et décisions » — SF-246-07 séquentielle après SF-246-06 sur `FamilleExtractedData` / `FAMILLE_INSTRUCTION` / `extractFamilleData()`.

> **Note de design IA** : `regimeMatrimonialDetecte` reprend l'énumération déjà utilisée par `liquidation_communaute_data.regime_matrimonial` (`COMMUNAUTE_LEGALE` / `SEPARATION_BIENS` / `PARTICIPATION_ACQUETS`) + `COMMUNAUTE_UNIVERSELLE`. Les booléens du formulaire (clause d'attribution intégrale, clause de retranchement, présence de récompenses) ne sont pas factualisables de façon fiable → restent en saisie manuelle (documenté, cadrage §5.6). Les libellés et valeurs individuelles de chaque bien en indivision restent en saisie manuelle (concept déjà couvert pour `partage-immobilier` par SF-155-20 sur d'autres champs — pas d'extension ici).

---

## Critères d'acceptation

- [ ] Le record `FamilleExtractedData` contient les 4 nouveaux champs (`valeurCommunauteEurDetectee`, `regimeMatrimonialDetecte`, `valeurBiensIndivisionEur`, `nombreCoindivisairesDetecte`), tous nullables, propagés par le builder F-234.
- [ ] Le prompt `FAMILLE_INSTRUCTION` décrit un sous-objet `regime_matrimonial_detection` avec une définition juridique sans ambiguïté par champ + l'énumération exacte du régime + l'instruction `null` hors FR / hors certitude.
- [ ] `extractFamilleData()` parse `regime_matrimonial_detection` : valeurs via `positiveDoubleOrNull()`, dénombrement via `boundedIntOrNull(_, _, 0, 50)`, régime via `stringOrNull()` + whitelist.
- [ ] Le DTO frontend `FamilleExtractedData` expose les 4 champs avec les bons types TS et **ne déclare plus** de champ aspirationnel équivalent sans source backend.
- [ ] Les 3 helpers lisent des champs réels ; chaque `computePrefillCount()` retourne le nombre exact de champs pré-remplissables.
- [ ] Les 3 `prefillFromAi()` renseignent les champs de leur tableau respectif quand `workspaceCountry === 'FRANCE'`, et restent no-op si BE.
- [ ] Chaque champ pré-rempli affiche un badge `auto_awesome` ; la modification manuelle remet `provenance<Field>` à `null`.
- [ ] Sur chaque outil, `getPrefillCount()` statique et `prefillFromAi()` runtime en parité stricte (test Jest cas 0 / partiel / nominal).
- [ ] Une fixture IA avec un contrat de mariage en communauté universelle (valeur 350000 €) → `regimeMatrimonialDetecte` = `COMMUNAUTE_UNIVERSELLE`, `valeurCommunauteEurDetectee` = 350000 (test backend).
- [ ] `coherenceAlerts` lève une alerte F-IA-03 si une valeur / un régime pré-rempli diverge de la saisie de l'avocat.
- [ ] Isolation workspace : non applicable côté pré-fill — voir section dédiée.

---

## Périmètre

### Hors scope (explicite)

- Toute modification du scoring, des verdicts, des formules ou des bases juridiques des 3 outils (logique métier inchangée).
- Le pré-remplissage des clauses du contrat de mariage, de la présence de récompenses, des libellés / valeurs individuelles des biens en indivision — non factualisables de façon fiable par le LLM en V1 ; restent en saisie manuelle (documenté).
- Tout outil Régimes BE (F-217) — hors périmètre vague 2 FR.
- Toute migration Liquibase — aucun nouvel outil, aucune table.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `valeurCommunauteEurDetectee` | `null` | montant `> 0` ou `null` |
| `regimeMatrimonialDetecte` | `null` | `COMMUNAUTE_LEGALE` / `COMMUNAUTE_UNIVERSELLE` / `SEPARATION_BIENS` / `PARTICIPATION_ACQUETS` ou `null` |
| `valeurBiensIndivisionEur` | `null` | montant `> 0` ou `null` |
| `nombreCoindivisairesDetecte` | `null` | entier `[0, 50]` ou `null` |

Comportements à la création : aucun — la SF étend un record de réponse IA.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `valeurCommunauteEurDetectee` | Non | — | nombre `> 0` ; `≤ 0` ou aberrant → `null` | Non | `positiveDoubleOrNull()` |
| `regimeMatrimonialDetecte` | Non | — | énumération 4 valeurs ; sinon → `null` | Non | `stringOrNull()` + whitelist |
| `valeurBiensIndivisionEur` | Non | — | nombre `> 0` ; `≤ 0` → `null` | Non | `positiveDoubleOrNull()` |
| `nombreCoindivisairesDetecte` | Non | — | entier `[0, 50]` ; hors plage → `null` | Non | `boundedIntOrNull(_, _, 0, 50)` |

Notes :
- Tous les champs nullables — invariant cadrage §5.1.2 / §5.2.
- Montants en euros — invariant cadrage §5.2.

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/{tool}` (3 outils F-FA-15/16/17) | Oui | LAWYER |
| GET | `/api/v1/case-files/{caseFileId}/{tool}` (3 outils F-FA-15/16/17) | Oui | MEMBER |

> Endpoints **inchangés** (existants F-FA-15/16/17). La SF n'ajoute aucun endpoint.

### Contrat API figé (parallélisation back / front)

**Bloc JSON produit par le pipeline IA** (sous `analysis_result.famille_extracted_data`) :

```json
"regime_matrimonial_detection": {
  "regime_matrimonial": "COMMUNAUTE_UNIVERSELLE",
  "valeur_communaute_eur": 350000.0,
  "valeur_biens_indivision_eur": 180000.0,
  "nombre_coindivisaires": 2
}
```

**Record backend `FamilleExtractedData`** — 4 champs ajoutés (en fin de record, après les champs SF-246-06) :

```java
// SF-246-07 : 4 champs IA régimes matrimoniaux / liquidation pour pré-fill F-FA-15/16/17
// (Famille FR uniquement, nullables).
Double valeurCommunauteEurDetectee,
String regimeMatrimonialDetecte,
Double valeurBiensIndivisionEur,
Integer nombreCoindivisairesDetecte
```

**DTO frontend `FamilleExtractedData`** (`divorce-accepte.model.ts`) — 4 champs ajoutés (types TS `number | null` / `string | null`).

**Helpers `*PrefillInput`** — chaque helper expose un `Pick<FamilleExtractedData, ...>` restreint + `workspaceCountry`. `computePrefillCount(input)` retourne 0 si `workspaceCountry !== 'FRANCE'`.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| — | — | Aucune table impactée. `familleExtractedData` sérialisé dans le JSON de synthèse |

### Migration Liquibase

- [ ] Oui
- [x] Non applicable — aucun nouvel outil, aucune table, aucune règle `decision_tool_visibility_rules`.

### Composants Angular (si applicable)

- `CommunauteUniverselleSectionComponent`, `RecompensesSectionComponent`, `PartageJudiciaireSectionComponent` — `prefillFromAi()` rendu effectif, signaux `provenance<Field>`, handlers `onXxxChange()`, badges `auto_awesome`, extension `coherenceAlerts`.
- Les 3 helpers `*-section-prefill-rules.ts` — lecture de champs réels, `computePrefillCount()` recalculé.

---

## Plan de test

### Tests unitaires

- [ ] `extractFamilleData()` — cas nominal : `regime_matrimonial_detection` complet → 4 champs renseignés.
- [ ] `extractFamilleData()` — sous-objet absent → 4 champs `null`, pas d'exception.
- [ ] `extractFamilleData()` — `regime_matrimonial` hors whitelist → `null`.
- [ ] `extractFamilleData()` — valeur ≤ 0 → `null` ; `nombre_coindivisaires` hors `[0, 50]` → `null`.
- [ ] `LegalDomainPromptBuilderTest` — `FAMILLE_INSTRUCTION` contient les 4 clés `regime_matrimonial_detection` + l'énumération du régime.
- [ ] Par outil : `computePrefillCount()` cas (a) `aiData` vide → 0 ; (b) partiel ; (c) nominal.
- [ ] Par outil : `computePrefillCount()` `workspaceCountry = 'BELGIQUE'` → 0.
- [ ] Par outil : `prefillFromAi()` cas nominal → champs renseignés, badges présents.
- [ ] Par outil : `prefillFromAi()` parité stricte avec `getPrefillCount()`.
- [ ] Par outil : `onXxxChange()` — modification manuelle remet `provenance<Field>` à `null`.
- [ ] Par outil : `coherenceAlerts` — alerte levée si valeur saisie diverge de la détection IA.

### Tests d'intégration

- [ ] Analyse IA d'un dossier famille FR fixture avec contrat de mariage + indivision → la synthèse expose `regime_matrimonial_detection` peuplé.
- [ ] **Fixture multi-valeurs** : dossier mentionnant à la fois la valeur de la communauté et la valeur des biens en indivision distinctes → chaque montant dans le bon champ.
- [ ] Dossier famille BE → les 4 champs FR restent `null`.
- [ ] `GET /api/v1/case-files/{caseFileId}/{tool}` → 403 si workspace différent (non-régression sur les 3 endpoints).

### Isolation workspace

- [x] Applicable — vérifiée au niveau des 3 endpoints F-FA-15/16/17 existants (tests de non-régression conservés). Les champs IA n'introduisent aucun nouvel accès.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale structurelle** — la SF coche le déclencheur **« Outil décisionnel métier »**. Composants impactés ci-dessous.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| Les 3 `*SectionComponent` F-FA-15/16/17 | `prefillFromAi()` devient effectif — risque de pré-remplir un champ à tort | Tests Jest pré-fill cas 0 / partiel / nominal |
| `extractFamilleData()` | Tout consommateur de `FamilleExtractedData` reçoit 4 champs supplémentaires (additif, nullable) | Tests d'extraction existants conservés verts |
| `decisional-tools-panel` | Badge « Pré-rempli par l'IA (N) » des 3 outils passe de 0 à N | Tests Jest `getPrefillCount` |
| Autres outils Famille FR consommant `familleExtractedData` | Aucun — champs additifs ignorés | Compilation TS + tests existants |

### Smoke tests E2E concernés

- [x] `e2e/smoke/case-analysis-flow.spec.ts` — flux d'analyse de dossier reste vert.
- [x] `e2e/smoke/happy-path.spec.ts` — parcours nominal inchangé.
- [x] `cd e2e && npm test` avant push (préoccupation transversale « outil décisionnel »).

---

## Dépendances

### Subfeatures bloquantes

- **SF-246-06** — couplage de fichier (record `FamilleExtractedData`, prompt `FAMILLE_INSTRUCTION`, `extractFamilleData()`, DTO `divorce-accepte.model.ts`). SF-246-07 doit être développée **après** le merge de SF-246-06 et rebasée sur master à jour — voir « Notes et décisions ».

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` touchée.

---

## Notes et décisions

### Couplage de fichiers partagés — ordre de dev de la série Famille FR

SF-246-07 modifie les **mêmes fichiers backend partagés** que les autres SF Famille : record `FamilleExtractedData` (+ builder F-234), prompt `FAMILLE_INSTRUCTION`, méthode `extractFamilleData()`, DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`).

**Décision** : ordre de dev imposé sur la série Famille :

```
SF-246-06 → SF-246-07 → SF-246-08 → SF-246-09 → SF-246-10 → SF-246-11 → SF-246-12
```

SF-246-07 ajoute ses 4 champs **après** ceux de SF-246-06 (en fin de record / prompt / builder), branchée après le merge de SF-246-06 et rebasée sur master à jour. Ne **jamais** développer deux SF Famille sur deux branches simultanées modifiant `FamilleExtractedData` — conflit de rebase systématique. La parallélisation backend / frontend **intra-SF** reste autorisée (contrat API figé ci-dessus).

### Décision de mutualisation du régime matrimonial

`regimeMatrimonialDetecte` est ajouté une seule fois et pourra bénéficier à d'autres outils Famille (révisions post-divorce, liquidation). Le champ existant `liquidation_communaute_data.regime_matrimonial` reste **distinct** : il alimente le calculateur de liquidation (F-FA déjà couvert), pas le pré-fill des formulaires d'outils. Pas de mutualisation forcée — deux sources, deux finalités (cohérent avec l'invariant « un champ = une définition »).
