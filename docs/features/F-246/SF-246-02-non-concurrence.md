# Mini-spec — [F-246 / SF-246-02] Pré-remplissage IA — Clause de non-concurrence (FR)

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Découpage de référence : `docs/features/F-246/cadrage-decoupage.md` (§3.1 ligne SF-246-02, vague 1).
> **Traçabilité** : SF-246-02 **est** la SF anciennement nommée `SF-DT-24-03` (clause de
> non-concurrence), renumérotée dans la série F-246. Origine : prépa démo Renversez 2026-05-18
> (dossier Dupont — clause de non-concurrence).

---

## Identifiant

`F-246 / SF-246-02`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`draft`

## Date de création

2026-05-18

## Branche Git

`feat/SF-246-02-non-concurrence-prefill`

---

## Objectif

> En une phrase : que fait cette subfeature ?

Compléter le pré-remplissage IA de l'outil `non-concurrence` (F-DT-24, Travail FR) en faisant extraire par le pipeline IA la durée de la clause, sa zone géographique et sa contrepartie financière — champs aujourd'hui absents de la chaîne backend — afin que `prefillFromAi()` ne pré-remplisse plus le seul `salaireMensuelBrutEur`.

---

## Comportement attendu

### Cas nominal

1. L'avocat lance l'analyse IA d'un dossier de droit du travail FR contenant le contrat de travail produisant la clause de non-concurrence.
2. Le pipeline IA (prompt `LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION`) extrait, dans `travail_extracted_data`, un sous-objet `clause_non_concurrence_detail` regroupant la durée, la zone et la contrepartie de la clause.
3. L'extracteur `extractTravailData()` parse ce sous-objet en champs typés du record `TravailExtractedData`.
4. Le DTO frontend `TravailExtractedData` expose les champs ; le `TOOL_REGISTRY` les passe déjà via `aiData: ctx.synthesis?.travailExtractedData`.
5. À l'ouverture de l'outil `non-concurrence`, `prefillFromAi()` renseigne `dureeMois`, `territoireDescription`, `limiteTerritoireDefini`, `limiteDureeDefinie`, `contrepartieMontantMensuelEur`, `contrepartieFinancierePresente`, `clausePresenteContrat` et `salaireMensuelBrutEur` (déjà branché) ; un badge `auto_awesome` s'affiche par champ pré-rempli.
6. L'avocat peut modifier toute valeur : le handler `onXxxChange()` remet `provenance<Field>` à `null` et déclenche la revérification F-IA-03.
7. Le badge « Pré-rempli par l'IA (N champs) » du panel F-IA-04 reflète `getPrefillCount()`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Le LLM ne détecte pas de clause de non-concurrence | `clause_non_concurrence_detail` à `null` ; `prefillFromAi()` no-op gracieux (sauf `salaireMensuelBrutEur` toujours tenté) ; `getPrefillCount()` peut valoir 0 ou 1 | n/a |
| Clause présente mais durée non chiffrée | `nonConcurrenceDureeMois` à `null` ; les autres champs détectables restent pré-remplis | n/a |
| Contrepartie exprimée en % du salaire et non en € | Le prompt impose la conversion en € mensuel via `salaire_brut_mensuel` ; si conversion impossible → `nonConcurrenceContrepartieMontantEur` à `null` | n/a |
| Le LLM renvoie une durée négative ou aberrante (> 600 mois) | `intOrNull()` + garde de plage côté extracteur → `null` | n/a |
| Dossier de droit du travail belge | Champs FR de la clause restent `null` (le prompt impose null hors FR) ; outil non affiché pour la BE | n/a |
| `aiData` arrive après le premier rendu | `prefillFromAi()` réinvoqué dans `ngOnChanges()` | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : seul `non-concurrence` (F-DT-24) a un formulaire avec durée / zone / contrepartie de clause. Le flag de visibilité `clauseNonConcurrenceDetectee` (F-166) existe déjà — il pilote l'affichage de l'outil, **pas** son pré-fill : pas de doublon, finalité distincte (cadrage §1 et §"HORS périmètre").
- [x] **Autres pays** : France uniquement. La clause de non-concurrence BE (CCT 1bis, conditions de validité distinctes) relève d'un outil BE dédié non concerné par cette vague — champs FR restent `null` pour la BE.
- [x] **Autres domaines** : non applicable — concept propre au droit du travail.
- [x] **Autres UI patterns** : pré-remplissage IA (pattern canonique), badges `auto_awesome`, alertes F-IA-03.
- [x] **Autres flows transversaux** : aucun.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : `TravailExtractedData` dans `case-analysis.model.ts`.
- [x] **Record / DTO backend** : `CaseAnalysisResponse.TravailExtractedData` + builder.
- [x] **Service / logique métier** : `extractTravailData()`.
- [x] **Entité JPA + schéma DB** : non applicable — `travailExtractedData` est sérialisé dans la synthèse IA. Les inputs validés de F-DT-24 restent persistés par l'endpoint `non-concurrence` existant (inchangé).
- [x] **Tests existants** : `non-concurrence-section-prefill-rules.spec.ts` (helper actuel : `computeSalaireMensuelBrutEur` uniquement), tests `extractTravailData()`. Les deux étendus par la SF.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : oui — l'outil a déjà une alerte F-IA-03 sur `SALAIRE_MENSUEL`. La SF ajoute des alertes sur `DUREE_CLAUSE`, `CONTREPARTIE` et `ZONE_GEOGRAPHIQUE` quand la valeur saisie diverge de la détection IA.
- [x] **Refresh dashboard (F-IA-02)** : inchangé — `triggerRefresh()` déjà dans le `next:` du POST.
- [x] **Pré-remplissage IA** : objet de la SF — pré-fill étendu de 1 à 8 champs.
- [x] **Persistance des inputs** : inchangée — inputs persistés via l'endpoint F-DT-24 existant.
- [x] **Masquage conditionnel selon type** : inchangé — visibilité F-IA-04 de F-DT-24 déjà gérée (FR + flag `clauseNonConcurrenceDetectee` F-166).
- [x] **Alertes actives après calcul** : gate `coherenceAlerts` = `!this.showForm()` uniquement — vérifié.

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — aucun nouveau composant partagé, service ou endpoint.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `non-concurrence` (F-DT-24) | Oui | Intégré dans cette SF |
| Flag `clauseNonConcurrenceDetectee` (F-166) | Non | Flag de visibilité — finalité distincte du pré-fill, conservé tel quel |
| Clause non-concurrence BE | Non | Régime BE distinct (CCT 1bis) — hors vague 1, champs `null` en BE |
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

- [x] **Palette statut** : navy/or info, vert OK, rouge réservé aux alertes critiques — conservé.
- [x] **Datepicker** : `<input type="date">` natif pour `datePriseEffet` — pas de `MatDatepicker`.
- [x] **Typographie** : `JetBrains Mono` pour `baseJuridique` / `formule`, `Inter` pour le reste — conservé.
- [x] **Gate `workspaceCountry`** : outil FR-only — bannière info en cas de mismatch BE (existant).
- [x] **Erreurs** : `MatSnackBar` — pas d'`alert()` / `confirm()`.
- [x] **Refresh dashboard** : `triggerRefresh()` dans le `next:` du POST — existant, inchangé.

### 2. Pré-fill IA (OBLIGATOIRE)

- [x] `@Input() aiData?: TravailExtractedData | null` — déjà typé strictement.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` **ET** `ngOnChanges()` — déjà le cas (la SF en étend le corps).
- [x] Signaux `provenance<Field>` : `provenanceSalaire` (existant) + ajout de `provenanceDureeMois`, `provenanceTerritoire`, `provenanceLimiteTerritoire`, `provenanceLimiteDuree`, `provenanceContrepartieMontant`, `provenanceContrepartiePresente`, `provenanceClausePresente`.
- [x] Badge `auto_awesome` « Pré-rempli depuis l'analyse » par champ pré-rempli.
- [x] Handler `onXxxChange()` par champ qui remet `provenance<Field>` à `null`.

### 3. Validation F-IA-03 (OBLIGATOIRE)

- [x] `coherenceAlerts` étendu : alertes `DUREE_CLAUSE`, `CONTREPARTIE`, `ZONE_GEOGRAPHIQUE` en plus de `SALAIRE_MENSUEL`.
- [x] Hiérarchie F-96 > Question IA > IA détection > Pièce manquante ; convergence → `'MULTI'`.
- [x] `<app-coherence-popover-trigger>` câblé sur les champs concernés.
- [x] Helper partagé `CoherenceAlertBuilder` (`frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts`).

### 4. TOOL_REGISTRY symétrique + `getPrefillCount(input)`

- [x] Entrée `F-DT-24-non-concurrence` déjà présente dans `TOOL_REGISTRY` ; `inputs(ctx)` passe déjà `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes` — aucune modification du binding requise.
- [x] Static `getPrefillCount(input)` du composant : étendu pour appeler `NonConcurrenceSectionPrefillRules.computePrefillCount()` enrichi (8 champs au lieu de 1).
- [x] Parité stricte `getPrefillCount()` ↔ `prefillFromAi()` : mêmes guards, mêmes mappings, même condition `workspaceCountry === 'FRANCE'`.
- [x] Tests Jest : (a) 0 champ (aiData vide / BE), (b) M champs partiels, (c) N champs cas nominal.
- [x] `tool_id` `F-DT-24-non-concurrence` déjà présent dans `KNOWN_FRONTEND_TOOL_IDS` du test `DecisionToolVisibilityIntegrityIT` — pas de migration `decision_tool_visibility_rules`.

### 5. Parité des domaines métier (niveau ≥ 5)

- [x] Niveau du tool : **5** (scoring de validité — verdict `VALIDE` / `RISQUE_NULLITE_PARTIELLE` / `NULLE` sur 4 critères Cass. soc. 10/07/2002).

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
| `salaireMensuelBrutEur` | nombre (€) | `TravailExtractedData.salaireBrutMensuel` (`Double`, **déjà présent**) | « déjà présent » — pré-fill existant conservé |
| `clausePresenteContrat` | booléen | `TravailExtractedData.clauseNonConcurrenceDetectee` (`boolean`, **déjà présent** — flag F-166) | « déjà présent » — mappé dans `prefillFromAi()` (le flag de visibilité sert aussi de pré-fill du booléen de présence) |
| `dureeMois` | nombre (mois) | `TravailExtractedData.nonConcurrenceDureeMois` (`Integer`, nullable) | [x] record + [x] prompt `LegalDomainPromptBuilder` + [x] extracteur + [x] DTO frontend |
| `limiteDureeDefinie` | booléen | dérivé : `true` si `nonConcurrenceDureeMois != null` | « dérivé » — calculé dans `prefillFromAi()` à partir du champ ci-dessus |
| `territoireDescription` | texte | `TravailExtractedData.nonConcurrenceZoneGeographique` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `limiteTerritoireDefini` | booléen | dérivé : `true` si `nonConcurrenceZoneGeographique` non vide | « dérivé » — calculé dans `prefillFromAi()` |
| `contrepartieMontantMensuelEur` | nombre (€) | `TravailExtractedData.nonConcurrenceContrepartieMontantEur` (`Double`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `contrepartieFinancierePresente` | booléen | dérivé : `true` si `nonConcurrenceContrepartieMontantEur != null` | « dérivé » — calculé dans `prefillFromAi()` |

- [x] Pour chaque champ valeur à pré-remplir non encore présent (`nonConcurrenceDureeMois`, `nonConcurrenceZoneGeographique`, `nonConcurrenceContrepartieMontantEur`), l'extension du record `TravailExtractedData` **et** du prompt `LegalDomainPromptBuilder` est explicitement dans le périmètre de cette SF.

> **Note couplage record partagé** : voir « Notes et décisions » — SF-246-02 séquentielle après SF-246-01 sur `TravailExtractedData` / `TRAVAIL_INSTRUCTION` / `extractTravailData()`.

> **Note de design IA** : 3 champs source seulement sont ajoutés au record ; les 4 booléens dérivés (`limiteDureeDefinie`, `limiteTerritoireDefini`, `contrepartieFinancierePresente`, et le mapping de `clausePresenteContrat`) sont **calculés côté `prefillFromAi()`** à partir des champs source — pas de champ booléen redondant dans le record (cohérence avec l'invariant « un champ = une définition »). Les champs `limiteObjetDefini` / `objetDescription` et `secteurActivite` / `datePriseEffet` restent en saisie manuelle (objet de la clause et secteur non factualisables de façon fiable — documenté, pas une dette masquée, cf. §5.6 cadrage).

> **Conversion contrepartie** : le prompt impose au LLM d'exprimer la contrepartie en **euros mensuels**. Si la clause stipule un pourcentage du salaire, le LLM applique `montant = pct × salaire_brut_mensuel` quand le salaire est lisible ; sinon `null`. La contrepartie est toujours **brute** (cohérent avec `salaire_brut_mensuel`).

---

## Critères d'acceptation

- [ ] Le record `TravailExtractedData` contient les 3 nouveaux champs (`nonConcurrenceDureeMois`, `nonConcurrenceZoneGeographique`, `nonConcurrenceContrepartieMontantEur`), tous nullables, propagés par le builder F-234.
- [ ] Le prompt `TRAVAIL_INSTRUCTION` décrit un sous-objet `clause_non_concurrence_detail` avec une définition juridique sans ambiguïté par champ + l'instruction de conversion en € mensuels + `null` hors FR / hors certitude.
- [ ] `extractTravailData()` parse `clause_non_concurrence_detail` : durée via `intOrNull()` + garde de plage `[0, 600]`, zone via `textOrNull()`, contrepartie via `doubleOrNull()`.
- [ ] Le DTO frontend `TravailExtractedData` (`case-analysis.model.ts`) expose les 3 champs avec les bons types TS.
- [ ] `NonConcurrenceSectionPrefillRules` expose `computeDureeMois`, `computeTerritoireDescription`, `computeContrepartieMontantEur` (en plus de `computeSalaireMensuelBrutEur`) et un `computePrefillCount()` qui compte les 8 champs effectivement pré-remplissables.
- [ ] `prefillFromAi()` du composant renseigne les 8 champs du tableau ci-dessus quand `workspaceCountry === 'FRANCE'`, et reste no-op si BE.
- [ ] Chaque champ pré-rempli affiche un badge `auto_awesome` ; la modification manuelle remet `provenance<Field>` à `null` et masque le badge.
- [ ] `getPrefillCount()` statique et `prefillFromAi()` runtime en parité stricte (test Jest cas 0 / partiel / nominal).
- [ ] `coherenceAlerts` lève une alerte F-IA-03 si `dureeMois` saisi diverge de `nonConcurrenceDureeMois` détecté, idem `contrepartie` et `zone`.
- [ ] Une fixture IA avec une clause détaillée (durée 24 mois, zone "France métropolitaine", contrepartie 30 % du salaire) → durée=24, zone renseignée, contrepartie convertie en € (test backend).
- [ ] Isolation workspace : non applicable côté pré-fill (donnée portée par la synthèse du dossier) — voir section dédiée.

---

## Périmètre

### Hors scope (explicite)

- Toute modification du scoring de validité, des verdicts, du ratio contrepartie/salaire ou des bases juridiques de F-DT-24 (logique métier inchangée).
- Le pré-remplissage de `limiteObjetDefini`, `objetDescription`, `secteurActivite`, `datePriseEffet` — non factualisables de façon fiable par le LLM en V1 ; restent en saisie manuelle (documenté).
- La clause de non-concurrence BE — hors vague 1 FR.
- Toute migration Liquibase — aucun nouvel outil, aucune table.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `nonConcurrenceDureeMois` | `null` | Entier `[0, 600]` ou `null` |
| `nonConcurrenceZoneGeographique` | `null` | Texte libre ≤ 500 car. ou `null` |
| `nonConcurrenceContrepartieMontantEur` | `null` | Montant brut mensuel `> 0` ou `null` |

Comportements à la création : aucun — la SF étend un record de réponse IA.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `nonConcurrenceDureeMois` | Non | — | entier `[0, 600]` ; hors plage → `null` | Non | `intOrNull()` + garde de plage |
| `nonConcurrenceZoneGeographique` | Non | 500 | texte libre tronqué à 500 car. | Non | `textOrNull()` + troncature |
| `nonConcurrenceContrepartieMontantEur` | Non | — | nombre `> 0` ; `≤ 0` ou aberrant → `null` | Non | `doubleOrNull()` |

Notes :
- Tous les champs nullables — invariant cadrage §5.1.2 / §5.2 : un montant non identifié de façon fiable reste `null`, jamais `0`.
- Contrepartie en euros **bruts mensuels** — invariant cadrage §5.2.

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/non-concurrence` | Oui | LAWYER |
| GET | `/api/v1/case-files/{caseFileId}/non-concurrence` | Oui | MEMBER |

> Endpoints **inchangés** (existants SF-DT-24-01). La SF n'ajoute aucun endpoint : les champs IA transitent par la synthèse d'analyse (`travailExtractedData`).

### Contrat API figé (parallélisation back / front)

**Bloc JSON produit par le pipeline IA** (sous `analysis_result.travail_extracted_data`) :

```json
"clause_non_concurrence_detail": {
  "duree_mois": 24,
  "zone_geographique": "France métropolitaine",
  "contrepartie_montant_mensuel_eur": 900.0
}
```

**Record backend `TravailExtractedData`** — 3 champs ajoutés (en fin de record, après les champs SF-246-01) :

```java
// SF-246-02 : 3 champs IA pour pré-fill F-DT-24 clause de non-concurrence (Travail FR uniquement, nullables).
Integer nonConcurrenceDureeMois,
String nonConcurrenceZoneGeographique,
Double nonConcurrenceContrepartieMontantEur
```

**DTO frontend `TravailExtractedData`** (`case-analysis.model.ts`) — 3 champs ajoutés :

```ts
/** SF-246-02 : détail de la clause de non-concurrence pour pré-fill F-DT-24 (FR uniquement). */
nonConcurrenceDureeMois?: number | null;
nonConcurrenceZoneGeographique?: string | null;
nonConcurrenceContrepartieMontantEur?: number | null;
```

**Helper `NonConcurrencePrefillInput`** — contrat figé (remplace l'interface actuelle à 1 champ) :

```ts
export interface NonConcurrencePrefillInput {
  aiData?: Pick<TravailExtractedData,
    | 'salaireBrutMensuel' | 'clauseNonConcurrenceDetectee'
    | 'nonConcurrenceDureeMois' | 'nonConcurrenceZoneGeographique'
    | 'nonConcurrenceContrepartieMontantEur'> | null;
  workspaceCountry?: string;
}
```

`computePrefillCount(input)` retourne le nombre de champs effectivement pré-remplissables (0 si `workspaceCountry !== 'FRANCE'`).

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| — | — | Aucune table impactée. `travailExtractedData` sérialisé dans le JSON de synthèse |

### Migration Liquibase

- [ ] Oui
- [x] Non applicable — aucun nouvel outil, aucune table, aucune règle `decision_tool_visibility_rules`.

### Composants Angular (si applicable)

- `NonConcurrenceSectionComponent` — `prefillFromAi()` étendu de 1 à 8 champs, ajout des signaux `provenance<Field>`, handlers `onXxxChange()`, badges `auto_awesome`, extension `coherenceAlerts`.
- `non-concurrence-section-prefill-rules.ts` — ajout des fonctions `computeDureeMois`, `computeTerritoireDescription`, `computeContrepartieMontantEur` ; `computePrefillCount()` recalculé sur 8 champs.

---

## Plan de test

### Tests unitaires

- [ ] `extractTravailData()` — cas nominal : `clause_non_concurrence_detail` complet → 3 champs renseignés.
- [ ] `extractTravailData()` — sous-objet absent → 3 champs `null`, pas d'exception.
- [ ] `extractTravailData()` — `duree_mois` négative ou > 600 → `null` (garde de plage).
- [ ] `extractTravailData()` — `contrepartie_montant_mensuel_eur` ≤ 0 → `null`.
- [ ] `LegalDomainPromptBuilderTest` — `TRAVAIL_INSTRUCTION` contient les 3 clés `clause_non_concurrence_detail` + l'instruction de conversion en € mensuels.
- [ ] `computePrefillCount()` — cas (a) `aiData` vide → 0 ; cas (b) durée + salaire seuls → 3 (durée + limiteDuree dérivé + salaire) ; cas (c) clause complète → 8.
- [ ] `computePrefillCount()` — `workspaceCountry = 'BELGIQUE'` → 0.
- [ ] `prefillFromAi()` — clause complète → 8 champs renseignés, badges présents, booléens dérivés corrects.
- [ ] `prefillFromAi()` — parité stricte avec `getPrefillCount()`.
- [ ] `onXxxChange()` — modification manuelle remet `provenance<Field>` à `null`.
- [ ] `coherenceAlerts` — alerte levée si `dureeMois` saisi diverge de `nonConcurrenceDureeMois` détecté.

### Tests d'intégration

- [ ] Analyse IA d'un dossier travail FR fixture avec contrat produisant une clause de non-concurrence → la synthèse expose `clause_non_concurrence_detail` peuplé.
- [ ] **Fixture conversion** (invariant cadrage §5.2) : clause stipulant « contrepartie = 30 % du salaire », `salaire_brut_mensuel` = 3000 → `nonConcurrenceContrepartieMontantEur` = 900.
- [ ] **Fixture multi-valeurs** : dossier mentionnant à la fois une durée d'engagement contractuel et une durée de clause de non-concurrence → seule la durée de la **clause de non-concurrence** est extraite dans `nonConcurrenceDureeMois`.
- [ ] Dossier travail BE → les 3 champs FR restent `null`.
- [ ] `GET /api/v1/case-files/{caseFileId}/non-concurrence` → 403 si workspace différent (non-régression endpoint existant).

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
| `extractTravailData()` | Tout consommateur de `TravailExtractedData` reçoit 3 champs supplémentaires (additif, nullable — non cassant via builder F-234) | Tests d'extraction existants conservés verts |
| `decisional-tools-panel` | Badge « Pré-rempli par l'IA (N) » de F-DT-24 passe de ≤1 à N | Test Jest `getPrefillCount` |
| Autres outils Travail FR consommant `travailExtractedData` | Aucun — champs additifs ignorés | Compilation TS + tests existants |

### Smoke tests E2E concernés

- [x] `e2e/smoke/case-analysis-flow.spec.ts` — flux d'analyse de dossier reste vert.
- [x] `e2e/smoke/happy-path.spec.ts` — parcours nominal inchangé.
- [x] `cd e2e && npm test` avant push (préoccupation transversale « outil décisionnel »).

---

## Dépendances

### Subfeatures bloquantes

- **SF-246-01** — couplage de fichier (record `TravailExtractedData`, prompt `TRAVAIL_INSTRUCTION`, `extractTravailData()`, DTO `case-analysis.model.ts`). SF-246-02 doit être développée **après** le merge de SF-246-01 et rebasée sur master à jour — voir « Notes et décisions ».

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` touchée.

---

## Notes et décisions

### Couplage de fichiers partagés — ordre de dev de la vague 1

SF-246-01 et SF-246-02 modifient les **mêmes fichiers backend partagés** : record `TravailExtractedData` (+ builder F-234), prompt `TRAVAIL_INSTRUCTION`, méthode `extractTravailData()`, DTO frontend `TravailExtractedData` (`case-analysis.model.ts`).

**Décision** : **SF-246-02 est séquentielle après SF-246-01**. Ordre de dev de la vague 1 :

1. **SF-246-01** (nullité de procédure) — ajoute 6 champs au record, fige la structure.
2. **SF-246-02** (non-concurrence) — branchée après le merge de SF-246-01, ajoute ses 3 champs **après** ceux de SF-246-01 (positionnement en fin de record + builder pour minimiser les conflits).
3. **SF-246-04** (victime de violences L.425-6) — **indépendante**, touche `ImmigrationExtractedData` / `IMMIGRATION_INSTRUCTION` / `extractImmigrationData()` : **parallélisable** avec 01 et 02 sans risque de conflit.

Ne **jamais** développer SF-246-01 et SF-246-02 sur deux branches simultanées modifiant le record `TravailExtractedData` — conflit de rebase systématique. La parallélisation backend / frontend **intra-SF** reste autorisée (contrat API figé ci-dessus, seul `case-analysis.model.ts` est zone de contact, figé par le contrat).

### Décision booléens dérivés vs champs source

Seuls 3 champs source sont ajoutés au record (`nonConcurrenceDureeMois`, `nonConcurrenceZoneGeographique`, `nonConcurrenceContrepartieMontantEur`). Les 4 booléens du formulaire (`limiteDureeDefinie`, `limiteTerritoireDefini`, `contrepartieFinancierePresente`, `clausePresenteContrat`) sont **dérivés** au moment du `prefillFromAi()` — pas de champ booléen redondant dans le record IA. `clausePresenteContrat` est mappé depuis le flag de visibilité existant `clauseNonConcurrenceDetectee` (F-166), qui constate textuellement la présence de la clause au contrat — sémantiquement identique au booléen du formulaire.
