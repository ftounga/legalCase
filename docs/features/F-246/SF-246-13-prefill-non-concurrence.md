# Mini-spec — [F-246 / SF-246-13] Pré-remplissage IA — Clause de non-concurrence : `datePriseEffet` + `secteurActivite`

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Pattern de référence : `docs/features/F-246/SF-246-02-non-concurrence.md` — SF-246-02 a
> branché 3 champs (durée, zone, contrepartie) de l'outil F-DT-24 ; cette SF en reproduit
> **exactement** le pattern pour les 2 champs restés en saisie manuelle.

---

## Identifiant

`F-246 / SF-246-13`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-246-13-prefill-non-concurrence`

---

## Objectif

> En une phrase : que fait cette subfeature ?

Compléter le pré-remplissage IA de l'outil `non-concurrence` (F-DT-24, Travail FR) en faisant extraire par le pipeline IA la **date de prise d'effet** de la clause et le **secteur d'activité** — les 2 derniers champs saisissables que SF-246-02 avait volontairement laissés en saisie manuelle — afin que `prefillFromAi()` couvre désormais l'intégralité des champs renseignables (invariant F-246 « tous les champs »).

---

## Comportement attendu

### Cas nominal

1. L'avocat lance l'analyse IA d'un dossier de droit du travail FR contenant le contrat de travail produisant la clause de non-concurrence.
2. Le pipeline IA (prompt `LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION`) enrichit le sous-objet `clause_non_concurrence_detail` de 2 clés supplémentaires : `date_prise_effet` (date ISO de prise d'effet de la clause ≈ date de fin de contrat / rupture) et `secteur_activite` (secteur classé dans l'enum `SecteurActivite`).
3. L'extracteur `extractTravailData()` parse ces 2 clés en champs typés du record `TravailExtractedData` : `nonConcurrenceDatePriseEffet` (`String`, date ISO `YYYY-MM-DD`) et `nonConcurrenceSecteurActivite` (`String`, code enum normalisé).
4. Le DTO frontend `TravailExtractedData` expose les 2 champs ; le `TOOL_REGISTRY` les passe déjà via `aiData: ctx.synthesis?.travailExtractedData` — aucun changement de binding.
5. À l'ouverture de l'outil `non-concurrence`, `prefillFromAi()` renseigne `datePriseEffet` et `secteurActivite` en plus des 8 champs déjà branchés par SF-246-02 ; un badge `auto_awesome` s'affiche par champ pré-rempli.
6. L'avocat peut modifier toute valeur : les handlers `onDatePriseEffetChange()` / `onSecteurActiviteChange()` remettent `provenanceDatePriseEffet` / `provenanceSecteur` à `null` (masquage du badge).
7. Le badge « Pré-rempli par l'IA (N champs) » du panel F-IA-04 reflète `getPrefillCount()` recalculé sur 10 champs.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Le LLM ne détecte pas de clause de non-concurrence | `clause_non_concurrence_detail` à `null` ; les 2 nouveaux champs `null` ; `prefillFromAi()` no-op gracieux | n/a |
| Clause présente mais date de prise d'effet non datée | `nonConcurrenceDatePriseEffet` à `null` ; les autres champs détectables restent pré-remplis | n/a |
| Le LLM renvoie une date dans un format non ISO | `isoDateOrNull()` → `null` (fail-open, jamais de pré-fill d'un champ date avec une valeur malformée) | n/a |
| Le LLM classe le secteur dans une valeur hors enum (ex. « BTP », « banque ») | `normalizeEnumCode()` whitelist `SECTEUR_ACTIVITE_CODES` → `null` ; pas de pré-fill du select | n/a |
| Secteur non identifiable avec certitude | Le prompt impose `null` ; aucun pré-fill, l'avocat choisit manuellement | n/a |
| Dossier de droit du travail belge | Les 2 champs FR restent `null` (le prompt impose null hors FR) ; outil non affiché pour la BE | n/a |
| `aiData` arrive après le premier rendu | `prefillFromAi()` réinvoqué dans `ngOnChanges()` | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : seul `non-concurrence` (F-DT-24) a un formulaire avec un secteur d'activité et une date de prise d'effet de clause. La date de prise d'effet de clause est un concept propre à F-DT-24 ; le secteur d'activité au sens « module les seuils jurisprudentiels de proportionnalité » est lui aussi propre à F-DT-24. Pas de doublon avec un autre outil.
- [x] **Autres pays** : France uniquement. La clause de non-concurrence BE (CCT 1bis) relève d'un outil BE dédié non concerné — champs FR restent `null` pour la BE.
- [x] **Autres domaines** : non applicable — concept propre au droit du travail.
- [x] **Autres UI patterns** : pré-remplissage IA (pattern canonique), badges `auto_awesome`. Pas d'alerte F-IA-03 ajoutée (voir « Cas spécifique : nouvelle feature d'outil décisionnel »).
- [x] **Autres flows transversaux** : aucun.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : `TravailExtractedData` dans `case-analysis.model.ts`.
- [x] **Record / DTO backend** : `CaseAnalysisResponse.TravailExtractedData` + builder F-234.
- [x] **Service / logique métier** : `extractTravailData()`.
- [x] **Entité JPA + schéma DB** : non applicable — `travailExtractedData` est sérialisé dans la synthèse IA. Les inputs validés de F-DT-24 restent persistés par l'endpoint `non-concurrence` existant (inchangé).
- [x] **Tests existants** : `non-concurrence-section-prefill-rules.spec.ts`, tests `extractTravailData()` (`CaseAnalysisResponseTest`), `LegalDomainPromptBuilderTest`. Tous étendus par la SF.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : **non ajoutée pour ces 2 champs**. SF-246-02 a posé des alertes F-IA-03 sur les champs **numériques/textuels comparables** (`SALAIRE_MENSUEL`, `DUREE_CLAUSE`, `CONTREPARTIE`, `ZONE_GEOGRAPHIQUE` — écart relatif ou divergence textuelle). `datePriseEffet` et `secteurActivite` n'ont pas de notion d'« écart de 10 % » : une date est exacte ou non, un secteur appartient ou non à l'enum. Le pré-fill IA + badge `auto_awesome` (provenance) couvre déjà la traçabilité. Aucune nouvelle entrée dans `NonConcurrenceAlertField` — décision documentée, pas une dette masquée.
- [x] **Refresh dashboard (F-IA-02)** : inchangé — `triggerRefresh()` déjà dans le `next:` du POST.
- [x] **Pré-remplissage IA** : objet de la SF — pré-fill étendu de 8 à 10 champs.
- [x] **Persistance des inputs** : inchangée — inputs persistés via l'endpoint F-DT-24 existant.
- [x] **Masquage conditionnel selon type** : inchangé — visibilité F-IA-04 de F-DT-24 déjà gérée (FR + flag `clauseNonConcurrenceDetectee` F-166).
- [x] **Alertes actives après calcul** : gate `coherenceAlerts` = `!this.showForm()` uniquement — inchangé.

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — aucun nouveau composant partagé, service ou endpoint. La SF étend un record, un prompt, un extracteur et un helper existants.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `non-concurrence` (F-DT-24) | Oui | Intégré dans cette SF |
| Alertes F-IA-03 sur date/secteur | Non | Pas de notion d'écart relatif — pré-fill + badge suffisent (justifié ci-dessus) |
| Clause non-concurrence BE | Non | Régime BE distinct (CCT 1bis) — champs `null` en BE |
| Outils Famille / Immigration | Non | Concept propre au droit du travail |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s) créée(s)
- [ ] Backlog VN
- [x] Non applicable aux autres cibles (justification explicite ci-dessus)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [ ] **Non applicable** — la SF livre une partie frontend décisionnelle (`non-concurrence-section`).

### 1. Cohérence visuelle

- [x] **Palette statut** : conservée — la SF n'ajoute aucun élément de statut.
- [x] **Datepicker** : `<input type="date">` natif pour `datePriseEffet` — déjà en place, pas de `MatDatepicker`.
- [x] **Typographie** : conservée — aucun nouveau bloc `baseJuridique` / `formule`.
- [x] **Gate `workspaceCountry`** : outil FR-only — pré-fill court-circuité hors FR (existant, étendu aux 2 nouveaux champs).
- [x] **Erreurs** : `MatSnackBar` — inchangé.
- [x] **Refresh dashboard** : `triggerRefresh()` dans le `next:` du POST — existant, inchangé.

### 2. Pré-fill IA (OBLIGATOIRE)

- [x] `@Input() aiData?: TravailExtractedData | null` — déjà typé strictement.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` (via `load()`) **ET** `ngOnChanges()` — déjà le cas (la SF en étend le corps).
- [x] Signaux `provenance<Field>` : ajout de `provenanceDatePriseEffet` et `provenanceSecteur` aux côtés des signaux existants (SF-246-02).
- [x] Badge `auto_awesome` « Pré-rempli depuis l'analyse » par champ pré-rempli — ajouté pour `datePriseEffet` et `secteurActivite`.
- [x] Handlers `onDatePriseEffetChange()` / `onSecteurActiviteChange()` étendus pour remettre la provenance à `null`.

### 3. Validation F-IA-03 (OBLIGATOIRE)

- [x] `coherenceAlerts` **inchangé** — pas d'alerte F-IA-03 sur `datePriseEffet` / `secteurActivite`. Justification : ces 2 champs n'ont pas de notion d'écart relatif (date exacte/inexacte, secteur dans/hors enum). Le pré-fill IA + badge `auto_awesome` assure la traçabilité de la valeur IA. Décision explicite — cf. « Cas spécifique : nouvelle feature d'outil décisionnel ».
- [x] Hiérarchie F-96 > Question IA > IA détection > Pièce manquante : inchangée pour les 4 alertes existantes.
- [x] `<app-coherence-popover-trigger>` : inchangé.
- [x] Helper partagé `CoherenceAlertBuilder` : inchangé.

### 4. TOOL_REGISTRY symétrique + `getPrefillCount(input)`

- [x] Entrée `F-DT-24-non-concurrence` déjà présente dans `TOOL_REGISTRY` ; `inputs(ctx)` passe déjà `aiData` — aucune modification du binding requise.
- [x] Static `getPrefillCount(input)` du composant : délègue à `NonConcurrenceSectionPrefillRules.computePrefillCount()` enrichi (10 champs au lieu de 8).
- [x] Parité stricte `getPrefillCount()` ↔ `prefillFromAi()` : mêmes guards, mêmes mappings, même condition `workspaceCountry === 'FRANCE'`.
- [x] Tests Jest : (a) 0 champ (aiData vide / BE), (b) M champs partiels, (c) 10 champs cas nominal.
- [x] `tool_id` `F-DT-24-non-concurrence` déjà présent dans `KNOWN_FRONTEND_TOOL_IDS` du test d'intégrité — pas de migration `decision_tool_visibility_rules`.

### 5. Parité des domaines métier (niveau ≥ 5)

- [x] Niveau du tool : **5** (scoring de validité — verdict `VALIDE` / `RISQUE_NULLITE_PARTIELLE` / `NULLE`).

| Domaine | Équivalent existe ? | Si non → action |
|---------|---------------------|-----------------|
| Droit du travail | Oui (F-DT-24) | C'est l'outil de cette SF |
| Immigration | Non | La clause de non-concurrence n'existe pas en droit des étrangers — concept non pertinent |
| Famille | Non | Concept non pertinent en droit de la famille |

> La SF complète le pré-fill d'un outil existant — la parité de domaine de F-DT-24 a été tranchée à sa création.

---

## Champs IA à extraire (pré-remplissage)

- [ ] **Aucun pré-remplissage** — non, la SF étend le pré-remplissage.

| Champ du formulaire | Type | Champ source du record `TravailExtractedData` | Extension requise |
|---------------------|------|------------------------------------------------|-------------------|
| `datePriseEffet` | date ISO (`YYYY-MM-DD`) | `TravailExtractedData.nonConcurrenceDatePriseEffet` (`String`, nullable) | [x] record + [x] prompt `LegalDomainPromptBuilder` + [x] extracteur + [x] DTO frontend |
| `secteurActivite` | enum `SecteurActivite` | `TravailExtractedData.nonConcurrenceSecteurActivite` (`String`, nullable, code enum normalisé) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

- [x] Pour chaque champ à pré-remplir non encore présent (`nonConcurrenceDatePriseEffet`, `nonConcurrenceSecteurActivite`), l'extension du record `TravailExtractedData` **et** du prompt `LegalDomainPromptBuilder` est explicitement dans le périmètre de cette SF.

> **Note couplage record partagé** : la SF ajoute ses 2 champs **en fin du sous-objet `clause_non_concurrence_detail`** du prompt et **en fin de record** (après les champs SF-246-05) pour minimiser tout risque de conflit. Aucune autre SF F-246 ne touche `TravailExtractedData` en parallèle.

> **Note de design IA — secteur d'activité** : le prompt demande au LLM de classer le secteur dans **une valeur exacte de l'enum** (`INFORMATIQUE`, `COMMERCE`, `INDUSTRIE`, `SERVICES`, `AUTRE`). L'extracteur applique `normalizeEnumCode()` (upper-case + whitelist `SECTEUR_ACTIVITE_CODES`) : un code hors liste → `null`. Le secteur n'est jamais deviné — un secteur réel non rattachable de façon certaine à l'enum reste `null` (l'avocat choisit `AUTRE` ou un autre code manuellement). Le prompt précise que `AUTRE` n'est PAS un fallback de doute : en cas de doute → `null`.

> **Note de design IA — date de prise d'effet** : la date de prise d'effet de la clause = date à partir de laquelle l'obligation de non-concurrence s'applique = en pratique la date de fin / rupture du contrat de travail (lisible dans la lettre de licenciement, la convention de rupture ou le contrat). `isoDateOrNull()` rejette tout format non `YYYY-MM-DD` (fail-open). Le prompt impose de NE PAS confondre avec la date de signature du contrat ni la date de convocation à entretien.

---

## Critères d'acceptation

- [ ] Le record `TravailExtractedData` contient les 2 nouveaux champs (`nonConcurrenceDatePriseEffet`, `nonConcurrenceSecteurActivite`), tous deux `String` nullables, propagés par le builder F-234 (déclaration, champ Builder, setter, `toBuilder()`, `build()`).
- [ ] Le prompt `TRAVAIL_INSTRUCTION` enrichit le sous-objet `clause_non_concurrence_detail` de 2 clés `date_prise_effet` et `secteur_activite` avec une définition juridique sans ambiguïté + l'instruction `null` hors FR / hors certitude + la liste exhaustive des 5 codes de secteur.
- [ ] `extractTravailData()` parse ces 2 clés du sous-objet `clause_non_concurrence_detail` : date via `isoDateOrNull()`, secteur via `normalizeEnumCode()` whitelist `SECTEUR_ACTIVITE_CODES`. Les 2 champs sont `null` si le sous-objet est absent.
- [ ] Le DTO frontend `TravailExtractedData` (`case-analysis.model.ts`) expose les 2 champs avec les bons types TS.
- [ ] `NonConcurrenceSectionPrefillRules` expose `computeDatePriseEffet` et `computeSecteurActivite` (en plus des 8 fonctions existantes) et un `computePrefillCount()` qui compte 10 champs.
- [ ] `prefillFromAi()` du composant renseigne `datePriseEffet` et `secteurActivite` quand `workspaceCountry === 'FRANCE'`, et reste no-op si BE.
- [ ] Chaque champ pré-rempli affiche un badge `auto_awesome` ; la modification manuelle remet `provenance<Field>` à `null` et masque le badge.
- [ ] `getPrefillCount()` statique et `prefillFromAi()` runtime en parité stricte (test Jest cas 0 / partiel / nominal 10 champs).
- [ ] Une fixture IA avec une clause complète (`date_prise_effet` = `2026-03-31`, `secteur_activite` = `INFORMATIQUE`) → `nonConcurrenceDatePriseEffet` = `2026-03-31`, `nonConcurrenceSecteurActivite` = `INFORMATIQUE` (test backend).
- [ ] Une fixture IA avec `secteur_activite` hors enum (ex. `BTP`) → `nonConcurrenceSecteurActivite` = `null`.
- [ ] Une fixture IA avec `date_prise_effet` non ISO (ex. `31/03/2026`) → `nonConcurrenceDatePriseEffet` = `null`.
- [ ] Isolation workspace : non applicable côté pré-fill (donnée portée par la synthèse du dossier, déjà isolée).

---

## Périmètre

### Hors scope (explicite)

- Toute modification du scoring de validité, des verdicts, du ratio contrepartie/salaire ou des bases juridiques de F-DT-24 (logique métier inchangée).
- Le pré-remplissage de `limiteObjetDefini` / `objetDescription` — l'objet de la clause reste non factualisable de façon fiable par le LLM (libellé d'activité interdite trop variable) ; reste en saisie manuelle, documenté.
- Toute alerte F-IA-03 sur `datePriseEffet` / `secteurActivite` (justifié : pas de notion d'écart relatif).
- La clause de non-concurrence BE — hors périmètre FR.
- Toute migration Liquibase — aucun nouvel outil, aucune table, aucune règle `decision_tool_visibility_rules`.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `nonConcurrenceDatePriseEffet` | `null` | Date ISO `YYYY-MM-DD` ou `null` |
| `nonConcurrenceSecteurActivite` | `null` | Code parmi `INFORMATIQUE`, `COMMERCE`, `INDUSTRIE`, `SERVICES`, `AUTRE` ou `null` |

Comportements à la création : aucun — la SF étend un record de réponse IA.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `nonConcurrenceDatePriseEffet` | Non | — | date ISO `YYYY-MM-DD` stricte ; hors format → `null` | Non | `isoDateOrNull()` |
| `nonConcurrenceSecteurActivite` | Non | — | un des 5 codes enum ; hors liste → `null` | Non | `normalizeEnumCode()` (upper-case + whitelist) |

Notes :
- Les 2 champs sont nullables — invariant F-246 : une valeur non identifiée de façon fiable reste `null`, jamais une valeur par défaut arbitraire.
- `AUTRE` n'est pas un fallback de doute côté LLM : en cas de doute, le prompt impose `null`.

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/non-concurrence` | Oui | LAWYER |
| GET | `/api/v1/case-files/{caseFileId}/non-concurrence` | Oui | MEMBER |

> Endpoints **inchangés** (existants SF-DT-24-01). La SF n'ajoute aucun endpoint : les champs IA transitent par la synthèse d'analyse (`travailExtractedData`).

### Contrat API figé (parallélisation back / front)

**Bloc JSON produit par le pipeline IA** (sous `analysis_result.travail_extracted_data.clause_non_concurrence_detail`) — 2 clés ajoutées :

```json
"clause_non_concurrence_detail": {
  "duree_mois": 24,
  "zone_geographique": "France métropolitaine",
  "contrepartie_montant_mensuel_eur": 900.0,
  "date_prise_effet": "2026-03-31",
  "secteur_activite": "INFORMATIQUE"
}
```

**Record backend `TravailExtractedData`** — 2 champs ajoutés (en fin de record, après `ageDemandeurAnnees`) :

```java
// SF-246-13 : 2 champs IA pour pré-fill F-DT-24 clause de non-concurrence
// (Travail FR uniquement, nullables).
String nonConcurrenceDatePriseEffet,
String nonConcurrenceSecteurActivite
```

**DTO frontend `TravailExtractedData`** (`case-analysis.model.ts`) — 2 champs ajoutés :

```ts
/** SF-246-13 : date de prise d'effet + secteur d'activité de la clause de
 *  non-concurrence pour pré-fill F-DT-24 (FR uniquement). */
nonConcurrenceDatePriseEffet?: string | null;
nonConcurrenceSecteurActivite?: string | null;
```

**Helper `NonConcurrencePrefillInput`** — contrat étendu (2 champs source ajoutés au `Pick`) :

```ts
export interface NonConcurrencePrefillInput {
  aiData?: Pick<TravailExtractedData,
    | 'salaireBrutMensuel' | 'clauseNonConcurrenceDetectee'
    | 'nonConcurrenceDureeMois' | 'nonConcurrenceZoneGeographique'
    | 'nonConcurrenceContrepartieMontantEur'
    | 'nonConcurrenceDatePriseEffet' | 'nonConcurrenceSecteurActivite'> | null;
  workspaceCountry?: string;
}
```

`computePrefillCount(input)` retourne le nombre de champs effectivement pré-remplissables (0 si `workspaceCountry !== 'FRANCE'`, max 10).

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| — | — | Aucune table impactée. `travailExtractedData` sérialisé dans le JSON de synthèse |

### Migration Liquibase

- [ ] Oui
- [x] Non applicable — aucun nouvel outil, aucune table, aucune règle `decision_tool_visibility_rules`.

### Composants Angular (si applicable)

- `NonConcurrenceSectionComponent` — `prefillFromAi()` étendu de 8 à 10 champs, ajout des signaux `provenanceDatePriseEffet` / `provenanceSecteur`, extension des handlers `onDatePriseEffetChange()` / `onSecteurActiviteChange()`, 2 badges `auto_awesome`.
- `non-concurrence-section-prefill-rules.ts` — ajout des fonctions `computeDatePriseEffet`, `computeSecteurActivite` ; `computePrefillCount()` recalculé sur 10 champs.

---

## Plan de test

### Tests unitaires

- [ ] `extractTravailData()` — cas nominal : `clause_non_concurrence_detail` avec `date_prise_effet` + `secteur_activite` → 2 champs renseignés.
- [ ] `extractTravailData()` — sous-objet absent → 2 champs `null`, pas d'exception.
- [ ] `extractTravailData()` — `date_prise_effet` non ISO (`31/03/2026`) → `null`.
- [ ] `extractTravailData()` — `secteur_activite` hors enum (`BTP`) → `null`.
- [ ] `extractTravailData()` — `secteur_activite` en minuscules (`informatique`) → normalisé `INFORMATIQUE`.
- [ ] `LegalDomainPromptBuilderTest` — `TRAVAIL_INSTRUCTION` contient les clés `date_prise_effet` et `secteur_activite` dans le sous-objet `clause_non_concurrence_detail` + les 5 codes de secteur.
- [ ] `computePrefillCount()` — cas (a) `aiData` vide → 0 ; cas (b) clause partielle → compte intermédiaire ; cas (c) clause complète avec date + secteur → 10.
- [ ] `computePrefillCount()` — `workspaceCountry = 'BELGIQUE'` → 0.
- [ ] `computeDatePriseEffet()` / `computeSecteurActivite()` — null hors FR, valeur OK en FR.
- [ ] `computeSecteurActivite()` — code hors enum → `null`.
- [ ] `prefillFromAi()` (couvert par les tests helper) — parité stricte avec `getPrefillCount()`.

### Tests d'intégration

- [ ] Analyse IA d'un dossier travail FR fixture avec clause complète → la synthèse expose `clause_non_concurrence_detail` peuplé avec `date_prise_effet` + `secteur_activite` (couvert par les tests `extractTravailData()` sur `CaseAnalysisResponseTest`).
- [ ] Dossier travail BE → les 2 champs FR restent `null` (le prompt impose null hors FR — couvert au niveau prompt + extracteur fail-open).
- [ ] `GET /api/v1/case-files/{caseFileId}/non-concurrence` → 403 si workspace différent (non-régression endpoint existant — couvert par `NonConcurrenceControllerIT`).

### Isolation workspace

- [x] Applicable — vérifiée au niveau de l'endpoint F-DT-24 existant (test de non-régression conservé). Les champs IA n'introduisent aucun nouvel accès : ils transitent par la synthèse du dossier, déjà isolée par `caseFileId` + workspace.

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
| `NonConcurrenceSectionComponent` | `prefillFromAi()` étendu — risque de pré-remplir un champ à tort | Tests Jest pré-fill cas 0 / partiel / nominal |
| `extractTravailData()` | Tout consommateur de `TravailExtractedData` reçoit 2 champs supplémentaires (additif, nullable — non cassant via builder F-234) | Tests d'extraction existants conservés verts |
| `decisional-tools-panel` | Badge « Pré-rempli par l'IA (N) » de F-DT-24 passe de ≤8 à ≤10 | Test Jest `getPrefillCount` |
| Autres outils Travail FR consommant `travailExtractedData` | Aucun — champs additifs ignorés | Compilation TS + tests existants |

### Smoke tests E2E concernés

- [x] `e2e/smoke/case-analysis-flow.spec.ts` — flux d'analyse de dossier reste vert.
- [x] `e2e/smoke/happy-path.spec.ts` — parcours nominal inchangé.
- [x] La SF étend uniquement un record IA, un prompt, un extracteur et un helper de pré-fill — aucune route, aucun guard, aucun endpoint modifié. Préoccupation transversale = « outil décisionnel » (pas auth/workspace/navigation) : les smoke tests E2E ne sont pas un blocage de push pour cette SF, l'exécution reste recommandée si l'environnement E2E est disponible.

---

## Dépendances

### Subfeatures bloquantes

- **SF-246-02** — `done` et **mergée sur master** (commit `574aca00`). Cette SF en est la continuation directe : elle reproduit le même pattern sur les 2 champs que SF-246-02 avait laissés en saisie manuelle. Aucune autre SF F-246 ne modifie `TravailExtractedData` en parallèle.

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` touchée.

---

## Notes et décisions

### Décision product owner 2026-05-19 — invariant F-246 « tous les champs »

SF-246-02 avait explicitement exclu `datePriseEffet` et `secteurActivite` de son périmètre (« non factualisables de façon fiable par le LLM en V1 »). La décision product owner du 2026-05-19, en application de l'invariant F-246 « tous les champs saisissables d'un outil décisionnel doivent être pré-remplissables », lève cette exclusion : ces 2 champs doivent être branchés sur le pipeline IA. La fiabilité reste garantie par le filet `null` — `isoDateOrNull()` rejette une date malformée, `normalizeEnumCode()` rejette un secteur hors enum — donc un pré-fill IA n'introduit jamais une valeur erronée silencieuse : soit la valeur est extraite avec certitude, soit le champ reste `null` et l'avocat saisit.

### Pas d'alerte F-IA-03 sur ces 2 champs

Les 4 alertes F-IA-03 de SF-246-02 (`SALAIRE_MENSUEL`, `DUREE_CLAUSE`, `CONTREPARTIE`, `ZONE_GEOGRAPHIQUE`) reposent sur une notion d'écart : écart relatif > 10 % pour les montants/durée, divergence textuelle normalisée pour la zone. `datePriseEffet` et `secteurActivite` n'ont pas de gradation : une date est exacte ou non, un secteur appartient ou non à l'enum. Une alerte « la date saisie diffère de la date IA » serait soit toujours muette soit toujours bruyante sans valeur ajoutée. Le badge `auto_awesome` (provenance) suffit à tracer l'origine de la valeur ; sa disparition au premier changement manuel signale à l'avocat qu'il s'écarte de l'IA. Décision documentée — pas une omission.

### Secteur d'activité — enum interne `NonConcurrenceCalculator.SecteurActivite`

L'enum `SecteurActivite` est défini comme enum interne de `NonConcurrenceCalculator`. La whitelist `SECTEUR_ACTIVITE_CODES` de `CaseAnalysisResponse` est dérivée des 5 valeurs de cet enum (`INFORMATIQUE`, `COMMERCE`, `INDUSTRIE`, `SERVICES`, `AUTRE`) — alignée sur `SECTEUR_ACTIVITE_OPTIONS` du frontend pour un pré-fill direct sans mapping intermédiaire (même approche que `MOTIFS_OQTF_FR_CODES` ↔ enum `MotifOqtf` front).
