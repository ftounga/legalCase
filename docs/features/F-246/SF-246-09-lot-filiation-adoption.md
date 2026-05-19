# Mini-spec — [F-246 / SF-246-09] Pré-remplissage IA — Lot Filiation / adoption (Famille FR)

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Découpage de référence : `docs/features/F-246/cadrage-decoupage.md` (§3.1 ligne SF-246-09, vague 3).
> **SF de lot** : 4 outils — `contestation-paternite`, `recherche-paternite`,
> `reconnaissance-paternelle`, `adoption` (F-FA-18) — adossés au **même record**
> `FamilleExtractedData` et au **même prompt** `FAMILLE_INSTRUCTION`.

---

## Identifiant

`F-246 / SF-246-09`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-246-09-lot-filiation-prefill`

---

## Objectif

> En une phrase : que fait cette subfeature ?

Compléter le pré-remplissage IA des 4 outils décisionnels filiation / adoption (`contestation-paternite`, `recherche-paternite`, `reconnaissance-paternelle`, `adoption` — F-FA-18) en faisant extraire par le pipeline IA les dates de filiation et les âges aujourd'hui absents de `FamilleExtractedData`.

---

## Comportement attendu

### Cas nominal

1. L'avocat lance l'analyse IA d'un dossier de droit de la famille FR comportant une question de filiation ou d'adoption.
2. Le pipeline IA (prompt `LegalDomainPromptBuilder.FAMILLE_INSTRUCTION`) extrait, dans `famille_extracted_data`, un sous-objet `filiation_detection` regroupant les dates et âges factuels.
3. L'extracteur `extractFamilleData()` parse ce sous-objet en champs typés du record `FamilleExtractedData`.
4. Le DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`) expose les champs ; les 4 entrées `TOOL_REGISTRY` passent déjà `aiData: ctx.synthesis?.familleExtractedData`.
5. À l'ouverture de l'un des 4 outils, `prefillFromAi()` renseigne les champs détectables ; un badge `auto_awesome` s'affiche par champ pré-rempli.
6. L'avocat peut modifier toute valeur : `onXxxChange()` remet `provenance<Field>` à `null` et déclenche la revérification F-IA-03.
7. Le badge « Pré-rempli par l'IA (N champs) » du panel F-IA-04 reflète `getPrefillCount()` de chaque outil.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Le LLM ne détecte aucune question de filiation | Sous-objet `filiation_detection` à `null` ; `prefillFromAi()` no-op gracieux ; `getPrefillCount()` = 0 ; aucun badge | n/a |
| Date présente mais ambiguë | Le prompt impose `null` plutôt qu'une date approximative | n/a |
| Date hors ISO `YYYY-MM-DD` | `isoDateOrNull()` côté extracteur rejette → `null` | n/a |
| Âge négatif ou aberrant (> 120) | `boundedIntOrNull()` garde de plage `[0, 120]` → `null` | n/a |
| Dossier de famille belge | Champs FR restent `null` (le prompt impose null hors FR) ; outils non affichés pour la BE | n/a |
| `famille_extracted_data` absent du JSON IA | `extractFamilleData()` retourne `null` ; outils affichés formulaire vierge | n/a |
| `aiData` arrive après le premier rendu | `prefillFromAi()` réinvoqué dans `ngOnChanges()` | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : les 4 outils F-FA-18 partagent le sous-domaine filiation. `dateNaissanceEnfantDetectee` (reconnaissance) et `dateNaissanceEnfantRechercheDetectee` (recherche de paternité) sont **deux champs distincts** : le prompt nomme deux concepts juridiques différents (enfant dont la filiation paternelle est reconnue / enfant dont la paternité est recherchée) — un champ = une définition (cadrage §5.1.1). Les flags F-200 (`contestation_paternite_envisagee`, `adoption_envisagee`, etc.) pilotent la **visibilité** — finalité distincte du pré-fill.
- [x] **Autres pays** : France uniquement. Les actions de filiation BE relèvent d'autres outils — champs `null` pour la BE.
- [x] **Autres domaines** : non applicable — concept propre au droit de la filiation FR. (`mineurs-immigration` F-IM porte une notion d'âge — outil et record distincts, hors périmètre §2.3 cadrage.)
- [x] **Autres UI patterns** : pré-remplissage IA (pattern canonique), badges `auto_awesome`, alertes F-IA-03.
- [x] **Autres flows transversaux** : aucun.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : `FamilleExtractedData` dans `divorce-accepte.model.ts` — extension + **réalignement** (retrait des champs aspirationnels `ageEnfants`, `dateEtablissementFiliationDetectee`, etc. déclarés sans source backend).
- [x] **Record / DTO backend** : `CaseAnalysisResponse.FamilleExtractedData` + builder F-234.
- [x] **Service / logique métier** : `extractFamilleData()`.
- [x] **Entité JPA + schéma DB** : non applicable — `familleExtractedData` sérialisé dans la synthèse IA. Inputs des 4 outils persistés par leurs endpoints existants (inchangés).
- [x] **Tests existants** : helpers `*-section-prefill-rules.spec.ts` des 4 outils, tests `extractFamilleData()`. Tous mis à jour.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : oui — dates / âges croisables. `coherenceAlerts` étendu sur les 4 outils.
- [x] **Refresh dashboard (F-IA-02)** : inchangé — `triggerRefresh()` déjà câblé.
- [x] **Pré-remplissage IA** : objet de la SF.
- [x] **Persistance des inputs** : inchangée — endpoints F-FA-18 existants.
- [x] **Masquage conditionnel selon type** : inchangé — visibilité F-IA-04 déjà gérée (FR + flags F-200).
- [x] **Alertes actives après calcul** : gate `coherenceAlerts` = `!this.showForm()` uniquement — vérifié sur les 4 composants.

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — aucun nouveau composant partagé, service ou endpoint.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `contestation-paternite`, `recherche-paternite`, `reconnaissance-paternelle`, `adoption` (F-FA-18) | Oui | Intégrés dans cette SF de lot |
| `possession-etat` (F-FA-18-possession-etat) | Non | Hors périmètre §2.3 cadrage (formulaire sans champ date/valeur saisissable) |
| Outils Famille BE | Non | Régime BE distinct — champs `null` en BE |
| Outils Travail / Immigration FR | Non | Concept propre au droit de la filiation |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s) créée(s)
- [ ] Backlog VN
- [x] Non applicable aux autres cibles (justification explicite ci-dessus)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [ ] **Non applicable** — la SF livre 4 parties frontend décisionnelles.

### 1. Cohérence visuelle

- [x] **Palette statut** : navy/or info, vert OK, rouge réservé aux alertes critiques — conservé.
- [x] **Datepicker** : `<input type="date">` natif pour toutes les dates (établissement filiation, naissance enfant, etc.) — pas de `MatDatepicker`.
- [x] **Typographie** : `JetBrains Mono` pour `baseJuridique` / `formule`, `Inter` pour le reste — conservé.
- [x] **Gate `workspaceCountry`** : outils FR-only — bannière info en cas de mismatch BE (existant).
- [x] **Erreurs** : `MatSnackBar` — pas d'`alert()` / `confirm()`.
- [x] **Refresh dashboard** : `triggerRefresh()` dans le `next:` du POST — existant, inchangé.

### 2. Pré-fill IA (OBLIGATOIRE)

- [x] `@Input() aiData?: FamilleExtractedData | null` — déjà typé strictement sur les 4 composants.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` **ET** `ngOnChanges()`.
- [x] Signaux `provenance<Field>` : `provenanceDateEtablissementFiliation`, `provenanceDateConnaissanceVerite`, `provenanceDateMajoriteEnfant`, `provenanceDateNaissanceEnfantRecherche`, `provenanceDateNaissanceEnfant`, `provenanceAgeAdoptant`, `provenanceAgeAdopte`.
- [x] Badge `auto_awesome` « Pré-rempli depuis l'analyse » par champ pré-rempli.
- [x] Handler `onXxxChange()` par champ qui remet `provenance<Field>` à `null`.

### 3. Validation F-IA-03 (OBLIGATOIRE)

- [x] `coherenceAlerts` étendu sur les 4 outils pour les champs date / âge pré-remplis.
- [x] Hiérarchie F-96 > Question IA > IA détection > Pièce manquante ; convergence → `'MULTI'`.
- [x] `<app-coherence-popover-trigger>` câblé sur les champs concernés.
- [x] Helper partagé `CoherenceAlertBuilder`.

### 4. TOOL_REGISTRY symétrique + `getPrefillCount(input)`

- [x] Les 4 entrées F-FA-18 déjà présentes dans `TOOL_REGISTRY` ; `inputs(ctx)` passe déjà `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`.
- [x] Static `getPrefillCount(input)` de chaque composant : refactorisé pour appeler `computePrefillCount()` enrichi.
- [x] Parité stricte `getPrefillCount()` ↔ `prefillFromAi()` sur les 4 outils.
- [x] Tests Jest par outil : (a) 0 champ, (b) partiel, (c) nominal.
- [x] Les 4 `tool_id` déjà présents dans `KNOWN_FRONTEND_TOOL_IDS` du test `DecisionToolVisibilityIntegrityIT` — pas de migration.

### 5. Parité des domaines métier (niveau ≥ 5)

- [x] Niveau des outils : `contestation-paternite`, `recherche-paternite`, `adoption` niveau **5** (analyse de recevabilité / délais) ; `reconnaissance-paternelle` niveau **3-4**.

| Domaine | Équivalent existe ? | Si non → action |
|---------|---------------------|-----------------|
| Droit du travail | Non | La filiation n'a pas de transposition en droit du travail — concept non pertinent |
| Immigration | Non | Concept non pertinent en droit des étrangers |
| Famille | Oui (F-FA-18) | C'est le sous-domaine de cette SF |

> La SF complète le pré-fill d'outils existants — la parité de domaine de F-FA-18 a été tranchée à leur création.

---

## Champs IA à extraire (pré-remplissage)

- [ ] **Aucun pré-remplissage** — non, la SF crée le pré-remplissage des 4 outils.

| Champ du formulaire | Outil(s) consommateur(s) | Type | Champ source du record `FamilleExtractedData` | Extension requise |
|---------------------|--------------------------|------|------------------------------------------------|-------------------|
| date d'établissement de la filiation | `contestation-paternite` | date (ISO YYYY-MM-DD) | `dateEtablissementFiliationDetectee` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| date de connaissance de la vérité | `contestation-paternite` | date (ISO YYYY-MM-DD) | `dateConnaissanceVeriteDetectee` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| date de majorité de l'enfant | `contestation-paternite` | date (ISO YYYY-MM-DD) | `dateMajoriteEnfantDetectee` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| date de naissance de l'enfant (recherche de paternité) | `recherche-paternite` | date (ISO YYYY-MM-DD) | `dateNaissanceEnfantRechercheDetectee` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| date de naissance de l'enfant (reconnaissance) | `reconnaissance-paternelle` | date (ISO YYYY-MM-DD) | `dateNaissanceEnfantDetectee` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| âge de l'adoptant | `adoption` | nombre (entier `[0, 120]`) | `ageAdoptantDetecte` (`Integer`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| âge de l'adopté | `adoption` | nombre (entier `[0, 120]`) | `ageAdopteDetecte` (`Integer`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

- [x] Pour chaque champ date / valeur à pré-remplir non encore présent, l'extension du record `FamilleExtractedData` **et** du prompt `LegalDomainPromptBuilder` (`FAMILLE_INSTRUCTION`) est explicitement dans le périmètre de cette SF.

> **Note couplage record partagé** : voir « Notes et décisions » — SF-246-09 séquentielle après SF-246-08.

> **Note de design IA** : 7 champs source ajoutés au record. `dateNaissanceEnfantDetectee` (reconnaissance) ≠ `dateNaissanceEnfantRechercheDetectee` (recherche de paternité) : deux contextes juridiques distincts (la recherche de paternité a un délai d'action lié à la naissance — art. 327 Cciv ; la reconnaissance non). Le prompt nomme explicitement chaque concept. Les âges (`ageAdoptantDetecte`, `ageAdopteDetecte`) sont extraits directement et non dérivés d'une date de naissance — l'âge à la date de la requête est l'unité pertinente du formulaire F-FA-18 adoption ; l'instruction de prompt précise « âge en années à la date de la requête d'adoption ». Les autres champs des formulaires (motif de la contestation, type d'adoption plénière/simple, existence de l'expertise génétique) restent en saisie manuelle (documenté, cadrage §5.6).

---

## Critères d'acceptation

- [ ] Le record `FamilleExtractedData` contient les 7 nouveaux champs (`dateEtablissementFiliationDetectee`, `dateConnaissanceVeriteDetectee`, `dateMajoriteEnfantDetectee`, `dateNaissanceEnfantRechercheDetectee`, `dateNaissanceEnfantDetectee`, `ageAdoptantDetecte`, `ageAdopteDetecte`), tous nullables, propagés par le builder F-234.
- [ ] Le prompt `FAMILLE_INSTRUCTION` décrit un sous-objet `filiation_detection` avec une définition juridique sans ambiguïté par champ + l'instruction `null` hors FR / hors certitude + la distinction explicite entre les deux dates de naissance d'enfant (recherche vs reconnaissance).
- [ ] `extractFamilleData()` parse `filiation_detection` : dates via `isoDateOrNull()`, âges via `boundedIntOrNull(_, _, 0, 120)`.
- [ ] Le DTO frontend `FamilleExtractedData` expose les 7 champs avec les bons types TS et **ne déclare plus** de champ aspirationnel équivalent sans source backend.
- [ ] Les 4 helpers lisent des champs réels ; chaque `computePrefillCount()` retourne le nombre exact de champs pré-remplissables.
- [ ] Les 4 `prefillFromAi()` renseignent les champs de leur tableau respectif quand `workspaceCountry === 'FRANCE'`, et restent no-op si BE.
- [ ] Chaque champ pré-rempli affiche un badge `auto_awesome` ; la modification manuelle remet `provenance<Field>` à `null`.
- [ ] Sur chaque outil, `getPrefillCount()` statique et `prefillFromAi()` runtime en parité stricte (test Jest cas 0 / partiel / nominal).
- [ ] Une fixture IA multi-dates pour `contestation-paternite` (date d'établissement de filiation ≠ date de connaissance de la vérité ≠ date de majorité) remplit chaque champ avec la bonne date (test backend — invariant cadrage §5.1.6).
- [ ] `coherenceAlerts` lève une alerte F-IA-03 si une date / un âge pré-rempli diverge de la saisie de l'avocat.
- [ ] Isolation workspace : non applicable côté pré-fill — voir section dédiée.

---

## Périmètre

### Hors scope (explicite)

- Toute modification du scoring, des verdicts, des formules ou des bases juridiques des 4 outils (logique métier inchangée).
- Le pré-remplissage des champs non factualisables de façon fiable par le LLM en V1 (motif de la contestation, type d'adoption plénière/simple, présence de l'expertise génétique) — restent en saisie manuelle (documenté).
- L'outil `possession-etat` (F-FA-18-possession-etat) — hors périmètre §2.3 cadrage.
- Tout outil Famille BE — hors périmètre vague 3 FR.
- Toute migration Liquibase — aucun nouvel outil, aucune table.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `dateEtablissementFiliationDetectee` | `null` | ISO `YYYY-MM-DD` strict ou `null` |
| `dateConnaissanceVeriteDetectee` | `null` | ISO `YYYY-MM-DD` strict ou `null` |
| `dateMajoriteEnfantDetectee` | `null` | ISO `YYYY-MM-DD` strict ou `null` |
| `dateNaissanceEnfantRechercheDetectee` | `null` | ISO `YYYY-MM-DD` strict ou `null` |
| `dateNaissanceEnfantDetectee` | `null` | ISO `YYYY-MM-DD` strict ou `null` |
| `ageAdoptantDetecte` | `null` | entier `[0, 120]` ou `null` |
| `ageAdopteDetecte` | `null` | entier `[0, 120]` ou `null` |

Comportements à la création : aucun — la SF étend un record de réponse IA.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| dates `date*Detectee` | Non | — | ISO `YYYY-MM-DD` ; rejet sinon → `null` | Non | `isoDateOrNull()` |
| `ageAdoptantDetecte`, `ageAdopteDetecte` | Non | — | entier `[0, 120]` ; hors plage → `null` | Non | `boundedIntOrNull(_, _, 0, 120)` |

Notes :
- Tous les champs nullables — invariant cadrage §5.1.2.
- Format ISO strict cohérent avec `dateAcceptationPV` (F-239).

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/{tool}` (4 outils F-FA-18) | Oui | LAWYER |
| GET | `/api/v1/case-files/{caseFileId}/{tool}` (4 outils F-FA-18) | Oui | MEMBER |

> Endpoints **inchangés** (existants F-FA-18). La SF n'ajoute aucun endpoint.

### Contrat API figé (parallélisation back / front)

**Bloc JSON produit par le pipeline IA** (sous `analysis_result.famille_extracted_data`) :

```json
"filiation_detection": {
  "date_etablissement_filiation": "2010-05-12",
  "date_connaissance_verite": "2024-02-01",
  "date_majorite_enfant": "2028-05-12",
  "date_naissance_enfant_recherche": "2020-07-03",
  "date_naissance_enfant": "2015-11-20",
  "age_adoptant": 42,
  "age_adopte": 7
}
```

**Record backend `FamilleExtractedData`** — 7 champs ajoutés (en fin de record, après les champs SF-246-08) :

```java
// SF-246-09 : 7 champs IA filiation / adoption pour pré-fill F-FA-18
// (Famille FR uniquement, nullables).
String dateEtablissementFiliationDetectee,
String dateConnaissanceVeriteDetectee,
String dateMajoriteEnfantDetectee,
String dateNaissanceEnfantRechercheDetectee,
String dateNaissanceEnfantDetectee,
Integer ageAdoptantDetecte,
Integer ageAdopteDetecte
```

**DTO frontend `FamilleExtractedData`** (`divorce-accepte.model.ts`) — 7 champs ajoutés (types TS `string | null` / `number | null`).

**Helpers `*PrefillInput`** — chaque helper expose un `Pick<FamilleExtractedData, ...>` restreint + `workspaceCountry`. `computePrefillCount(input)` retourne 0 si `workspaceCountry !== 'FRANCE'`.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| — | — | Aucune table impactée. `familleExtractedData` sérialisé dans le JSON de synthèse |

### Migration Liquibase

- [ ] Oui
- [x] Non applicable — aucun nouvel outil, aucune table, aucune règle `decision_tool_visibility_rules`.

### Composants Angular (si applicable)

- `ContestationPaterniteSectionComponent`, `RecherchePaterniteSectionComponent`, `ReconnaissancePaternelleSectionComponent`, `AdoptionSectionComponent` — `prefillFromAi()` rendu effectif, signaux `provenance<Field>`, handlers `onXxxChange()`, badges `auto_awesome`, extension `coherenceAlerts`.
- Les 4 helpers `*-section-prefill-rules.ts` correspondants — lecture de champs réels, `computePrefillCount()` recalculé.

---

## Plan de test

### Tests unitaires

- [ ] `extractFamilleData()` — cas nominal : `filiation_detection` complet → 7 champs renseignés.
- [ ] `extractFamilleData()` — sous-objet absent → 7 champs `null`, pas d'exception.
- [ ] `extractFamilleData()` — date non ISO → champ `null` (fail-open).
- [ ] `extractFamilleData()` — âge hors `[0, 120]` → `null`.
- [ ] `LegalDomainPromptBuilderTest` — `FAMILLE_INSTRUCTION` contient les 7 clés `filiation_detection` + la distinction des deux dates de naissance d'enfant.
- [ ] Par outil : `computePrefillCount()` cas (a) `aiData` vide → 0 ; (b) partiel ; (c) nominal.
- [ ] Par outil : `computePrefillCount()` `workspaceCountry = 'BELGIQUE'` → 0.
- [ ] Par outil : `prefillFromAi()` cas nominal → champs renseignés, badges présents.
- [ ] Par outil : `prefillFromAi()` parité stricte avec `getPrefillCount()`.
- [ ] Par outil : `onXxxChange()` — modification manuelle remet `provenance<Field>` à `null`.
- [ ] Par outil : `coherenceAlerts` — alerte levée si valeur saisie diverge de la détection IA.

### Tests d'intégration

- [ ] Analyse IA d'un dossier famille FR fixture avec contestation de paternité → la synthèse expose `filiation_detection` peuplé.
- [ ] **Fixture multi-dates** (invariant cadrage §5.1.6) : dossier contenant date d'établissement de filiation, date de connaissance de la vérité et date de majorité distinctes → chaque champ rempli avec la bonne date, aucune confusion.
- [ ] **Fixture distinction naissance** : dossier mentionnant à la fois une recherche de paternité et une reconnaissance pour deux enfants différents → `dateNaissanceEnfantRechercheDetectee` ≠ `dateNaissanceEnfantDetectee`, aucune confusion.
- [ ] Dossier famille BE → les 7 champs FR restent `null`.
- [ ] `GET /api/v1/case-files/{caseFileId}/{tool}` → 403 si workspace différent (non-régression sur les 4 endpoints).

### Isolation workspace

- [x] Applicable — vérifiée au niveau des 4 endpoints F-FA-18 existants (tests de non-régression conservés). Les champs IA n'introduisent aucun nouvel accès.

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
| Les 4 `*SectionComponent` F-FA-18 | `prefillFromAi()` devient effectif — risque de pré-remplir un champ à tort | Tests Jest pré-fill cas 0 / partiel / nominal |
| `extractFamilleData()` | Tout consommateur de `FamilleExtractedData` reçoit 7 champs supplémentaires (additif, nullable) | Tests d'extraction existants conservés verts |
| `decisional-tools-panel` | Badge « Pré-rempli par l'IA (N) » des 4 outils passe de 0 à N | Tests Jest `getPrefillCount` |
| Autres outils Famille FR consommant `familleExtractedData` | Aucun — champs additifs ignorés | Compilation TS + tests existants |

### Smoke tests E2E concernés

- [x] `e2e/smoke/case-analysis-flow.spec.ts` — flux d'analyse de dossier reste vert.
- [x] `e2e/smoke/happy-path.spec.ts` — parcours nominal inchangé.
- [x] `cd e2e && npm test` avant push (préoccupation transversale « outil décisionnel »).

---

## Dépendances

### Subfeatures bloquantes

- **SF-246-08** — couplage de fichier (record `FamilleExtractedData`, prompt `FAMILLE_INSTRUCTION`, `extractFamilleData()`, DTO `divorce-accepte.model.ts`). SF-246-09 doit être développée **après** le merge de SF-246-08 et rebasée sur master à jour — voir « Notes et décisions ».

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` touchée.

---

## Notes et décisions

### Couplage de fichiers partagés — ordre de dev de la série Famille FR

SF-246-09 modifie les **mêmes fichiers backend partagés** que les autres SF Famille : record `FamilleExtractedData` (+ builder F-234), prompt `FAMILLE_INSTRUCTION`, méthode `extractFamilleData()`, DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`).

**Décision** : ordre de dev imposé sur la série Famille :

```
SF-246-06 → SF-246-07 → SF-246-08 → SF-246-09 → SF-246-10 → SF-246-11 → SF-246-12
```

SF-246-09 ajoute ses 7 champs **après** ceux de SF-246-08, branchée après le merge de SF-246-08 et rebasée sur master à jour. Ne **jamais** développer deux SF Famille sur deux branches simultanées modifiant `FamilleExtractedData` — conflit de rebase systématique. La parallélisation backend / frontend **intra-SF** reste autorisée (contrat API figé ci-dessus).

### Décision âge extrait vs dérivé d'une date de naissance

Pour `adoption`, les âges (`ageAdoptantDetecte`, `ageAdopteDetecte`) sont extraits directement et non calculés à partir d'une date de naissance : le formulaire F-FA-18 adoption raisonne en âge à la date de la requête (conditions d'âge minimum de l'adoptant, écart d'âge — art. 343+ Cciv). Extraire un âge déjà contextualisé évite à `prefillFromAi()` de devoir choisir une date de référence (date de requête souvent absente des pièces). Le prompt impose « âge en années à la date de la requête d'adoption, null si non déterminable ».
