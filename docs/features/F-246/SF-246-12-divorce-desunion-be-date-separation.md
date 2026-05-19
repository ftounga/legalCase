# Mini-spec — [F-246 / SF-246-12] Pré-remplissage IA — Divorce pour désunion irrémédiable BE (date de séparation) (Famille BE)

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Découpage de référence : `docs/features/F-246/cadrage-decoupage.md` (§3.1 ligne SF-246-12, vague 4).
> **Outil mono** : `divorce-desunion-be` (`F-FA-11-desunion-irremediable-be`) — adossé au record
> `FamilleExtractedData` et au prompt `FAMILLE_INSTRUCTION`.

---

## Identifiant

`F-246 / SF-246-12`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-246-12-divorce-desunion-be-prefill`

---

## Objectif

> En une phrase : que fait cette subfeature ?

Brancher le pré-remplissage IA de l'outil `divorce-desunion-be` (Famille BE) en faisant extraire par le pipeline IA la date de séparation effective des époux — aujourd'hui absente de `FamilleExtractedData` — afin que `prefillFromAi()` renseigne le champ `dateSeparation` du formulaire, point d'entrée du calcul du délai de désunion irrémédiable (CJ art. 1255).

---

## Comportement attendu

### Cas nominal

1. L'avocat lance l'analyse IA d'un dossier de droit de la famille belge évoquant un divorce pour désunion irrémédiable (DDI — CC art. 229 §1/§3, CJ art. 1255 §1/§2).
2. Le pipeline IA (prompt `LegalDomainPromptBuilder.FAMILLE_INSTRUCTION`) extrait, dans `famille_extracted_data`, le champ `date_separation_be` au format ISO `YYYY-MM-DD`.
3. L'extracteur `extractFamilleData()` parse le champ en `FamilleExtractedData.dateSeparationBe`.
4. Le DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`) expose le champ ; l'entrée `TOOL_REGISTRY` de `divorce-desunion-be` passe déjà `aiData: ctx.synthesis?.familleExtractedData`.
5. À l'ouverture de l'outil `divorce-desunion-be`, `prefillFromAi()` renseigne `dateSeparation` ; un badge `auto_awesome` « Pré-rempli depuis l'analyse » s'affiche.
6. L'avocat peut modifier la valeur : le handler `onDateSeparationChange()` remet `provenanceDateSeparation` à `null` et déclenche la revérification F-IA-03.
7. Le badge « Pré-rempli par l'IA (N champs) » du panel F-IA-04 reflète `getPrefillCount()` (passe de 0 à 1 quand la date est détectée).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Aucune date de séparation détectable dans les pièces | `date_separation_be` à `null` ; `prefillFromAi()` no-op gracieux ; `getPrefillCount()` = 0 ; aucun badge | n/a |
| Date présente mais ambiguë / non lisible | Le prompt impose `null` plutôt qu'une date approximative | n/a |
| Date hors ISO `YYYY-MM-DD` | `isoDateOrNull()` côté extracteur rejette → `null` ; pas de pré-fill | n/a |
| Confusion avec une autre date du dossier (date du mariage, date de la requête, date de l'accord initial DC) | Le prompt nomme explicitement « date de la cessation effective de la vie commune » — distincte de la date du mariage, de la requête et de `date_accord_initial_divorce` | n/a |
| Dossier de droit de la famille français | Champ BE reste `null` (le prompt impose null hors BE) ; outil non affiché pour la FR | n/a |
| `famille_extracted_data` absent du JSON IA | `extractFamilleData()` retourne `null` ; outil affiché formulaire vierge | n/a |
| `aiData` arrive après le premier rendu | `prefillFromAi()` réinvoqué dans `ngOnChanges()` | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : seul `divorce-desunion-be` consomme la date de séparation BE. L'outil Famille FR `separation-corps` / `indivision` (F-FA-21/22) porte un champ `dateSeparation` **FR** (SF-246-08) — c'est un **champ distinct** : la séparation effective et son régime juridique (délai de 6 mois / 1 an du DDI belge vs séparation de corps FR) diffèrent ; un champ = une définition juridique sans ambiguïté (cadrage §5.1.1). Champs séparés (`dateSeparation` FR de SF-246-08 ≠ `dateSeparationBe` de cette SF). Le flag `divorce_ddi_envisage` (F-202) pilote la **visibilité** — finalité distincte du pré-fill.
- [x] **Autres pays** : Belgique uniquement. Le DDI est un dispositif belge ; le champ FR équivalent (`dateSeparation`) relève de SF-246-08. Champ `null` pour la FR.
- [x] **Autres domaines** : non applicable — concept propre au droit du divorce belge.
- [x] **Autres UI patterns** : pré-remplissage IA (pattern canonique), badge `auto_awesome`, alertes F-IA-03.
- [x] **Autres flows transversaux** : aucun.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : `FamilleExtractedData` dans `divorce-accepte.model.ts` — extension. Le modèle frontend peut déclarer un champ aspirationnel `dateSeparation` non sourcé (cadrage §5.3) : la SF clarifie que `dateSeparationBe` (BE) et `dateSeparation` (FR, SF-246-08) sont deux champs distincts et **réaligne** le DTO.
- [x] **Record / DTO backend** : `CaseAnalysisResponse.FamilleExtractedData` + builder F-234.
- [x] **Service / logique métier** : `extractFamilleData()`.
- [x] **Entité JPA + schéma DB** : non applicable — `familleExtractedData` sérialisé dans la synthèse IA. Inputs de l'outil persistés par son endpoint existant (inchangé).
- [x] **Tests existants** : `divorce-desunion-be-section-prefill-rules.spec.ts`, tests `extractFamilleData()`. Mis à jour par la SF.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : oui — la date saisie est croisable avec la détection IA et avec le délai de séparation requis. `coherenceAlerts` étendu sur `DATE_SEPARATION`.
- [x] **Refresh dashboard (F-IA-02)** : inchangé — `triggerRefresh()` déjà câblé.
- [x] **Pré-remplissage IA** : objet de la SF.
- [x] **Persistance des inputs** : inchangée — endpoint `divorce-desunion-be` existant.
- [x] **Masquage conditionnel selon type** : inchangé — visibilité F-IA-04 déjà gérée (BE + flag F-202 `divorce_ddi_envisage`).
- [x] **Alertes actives après calcul** : gate `coherenceAlerts` = `!this.showForm()` uniquement — vérifié.

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — aucun nouveau composant partagé, service ou endpoint.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `divorce-desunion-be` (`F-FA-11-desunion-irremediable-be`) | Oui | Intégré dans cette SF |
| `separation-corps` / `indivision` (F-FA-21/22, Famille FR) | Non | Champ `dateSeparation` FR distinct — traité par SF-246-08 |
| Flag `divorce_ddi_envisage` (F-202) | Non | Flag de visibilité — finalité distincte du pré-fill |
| Outils Travail / Immigration | Non | Concept propre au droit du divorce |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s) créée(s)
- [ ] Backlog VN
- [x] Non applicable aux autres cibles (justification explicite ci-dessus)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [ ] **Non applicable** — la SF livre une partie frontend décisionnelle (`divorce-desunion-be-section`).

### 1. Cohérence visuelle

- [x] **Palette statut** : navy/or info, vert OK, rouge réservé aux alertes critiques — conservé.
- [x] **Datepicker** : `<input type="date">` natif pour `dateSeparation` — pas de `MatDatepicker`.
- [x] **Typographie** : `JetBrains Mono` pour `baseJuridique` / `formule`, `Inter` pour le reste — conservé.
- [x] **Gate `workspaceCountry`** : outil BE-only — bannière info en cas de mismatch FR (existant).
- [x] **Erreurs** : `MatSnackBar` — pas d'`alert()` / `confirm()`.
- [x] **Refresh dashboard** : `triggerRefresh()` dans le `next:` du POST — existant, inchangé.

### 2. Pré-fill IA (OBLIGATOIRE)

- [x] `@Input() aiData?: FamilleExtractedData | null` — déjà typé strictement.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` **ET** `ngOnChanges()` — la SF rend le corps effectif.
- [x] Signal `provenanceDateSeparation = signal<'IA' | null>(null)`.
- [x] Badge `auto_awesome` « Pré-rempli depuis l'analyse » à côté de `dateSeparation`.
- [x] Handler `onDateSeparationChange()` qui remet `provenanceDateSeparation` à `null`.

### 3. Validation F-IA-03 (OBLIGATOIRE)

- [x] `coherenceAlerts` : alerte `DATE_SEPARATION` quand la date saisie diverge de la date détectée par l'IA.
- [x] Hiérarchie F-96 > Question IA > IA détection > Pièce manquante ; convergence → `'MULTI'`.
- [x] `<app-coherence-popover-trigger>` câblé sur `dateSeparation`.
- [x] Helper partagé `CoherenceAlertBuilder`.

### 4. TOOL_REGISTRY symétrique + `getPrefillCount(input)`

- [x] Entrée `divorce-desunion-be` (`F-FA-11-desunion-irremediable-be`) déjà présente dans `TOOL_REGISTRY` ; `inputs(ctx)` passe déjà `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`.
- [x] Static `getPrefillCount(input)` du composant : appelle `DivorceDesunionBePrefillRules.computePrefillCount()` enrichi.
- [x] Parité stricte `getPrefillCount()` ↔ `prefillFromAi()` : même guard `ISO_DATE_RE`, même condition `workspaceCountry === 'BELGIQUE'`.
- [x] Tests Jest : (a) 0 champ (aiData vide / FR / date non ISO), (b) — pas de cas « partiel » (1 seul champ), (c) 1 champ cas nominal.
- [x] `tool_id` `F-FA-11-desunion-irremediable-be` déjà présent dans `KNOWN_FRONTEND_TOOL_IDS` du test `DecisionToolVisibilityIntegrityIT` — pas de migration.

### 5. Parité des domaines métier (niveau ≥ 5)

- [x] Niveau du tool : **5** (analyse de recevabilité / éligibilité du divorce pour désunion irrémédiable — délais de séparation des 3 voies).

| Domaine | Équivalent existe ? | Si non → action |
|---------|---------------------|-----------------|
| Droit du travail | Non | Le divorce pour désunion irrémédiable n'a pas de transposition en droit du travail — concept non pertinent |
| Immigration | Non | Concept non pertinent en droit des étrangers |
| Famille | Oui (`divorce-desunion-be` BE ; voies FR équivalentes : F-FA-08 altération du lien) | C'est l'outil de cette SF (Famille BE) |

> La SF complète le pré-fill d'un outil existant — la parité de domaine a été tranchée à sa création.

---

## Champs IA à extraire (pré-remplissage)

- [ ] **Aucun pré-remplissage** — non, la SF crée le pré-remplissage.

| Champ du formulaire | Type | Champ source du record `FamilleExtractedData` | Extension requise |
|---------------------|------|------------------------------------------------|-------------------|
| `dateSeparation` | date (ISO YYYY-MM-DD) | `dateSeparationBe` (`String`, nullable) | [x] record + [x] prompt `LegalDomainPromptBuilder` (`FAMILLE_INSTRUCTION`) + [x] extracteur (`extractFamilleData()`) + [x] DTO frontend |

- [x] Pour le champ date à pré-remplir non encore présent dans la chaîne backend (`dateSeparationBe`), l'extension du record `FamilleExtractedData` **et** du prompt `LegalDomainPromptBuilder` (`FAMILLE_INSTRUCTION`) est explicitement dans le périmètre de cette SF.

> **Note couplage record partagé** : voir « Notes et décisions » — SF-246-12 séquentielle dans la série Famille (vague 4), après SF-246-11.

> **Note de design IA** : 1 champ source ajouté au record, nommé `dateSeparationBe` pour le **distinguer explicitement** de `dateSeparation` (FR, SF-246-08). La séparation effective belge au sens du DDI (point de départ du délai de 6 mois ou 1 an — CJ art. 1255 §1) n'a pas le même régime juridique que la séparation de fait FR : champs séparés, le prompt nomme « date de la cessation effective de la vie commune entre les époux » et impose `null` hors BE. La distinction d'avec la date du mariage, la date de la requête et `date_accord_initial_divorce` (champ DC, F-241) est inscrite au prompt. Les autres champs du formulaire (voie de DDI choisie, faits constitutifs) restent en saisie manuelle (non factualisables de façon fiable — documenté, cadrage §5.6).

---

## Critères d'acceptation

- [ ] Le record `FamilleExtractedData` contient le nouveau champ `dateSeparationBe` (`String`, nullable), propagé par le builder F-234.
- [ ] Le prompt `FAMILLE_INSTRUCTION` décrit la clé `date_separation_be` avec une définition juridique sans ambiguïté (« date de la cessation effective de la vie commune entre les époux », CJ art. 1255) + l'instruction `null` hors BE / hors certitude + la distinction explicite d'avec la date du mariage, la date de requête et `date_accord_initial_divorce`.
- [ ] `extractFamilleData()` parse `date_separation_be` via `isoDateOrNull()`.
- [ ] Le DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`) expose `dateSeparationBe?: string | null` et le distingue de `dateSeparation` (FR, SF-246-08).
- [ ] `DivorceDesunionBePrefillRules` lit `aiData.dateSeparationBe`, valide via `ISO_DATE_RE` ; `computePrefillCount()` retourne 1 si la date est valide et BE, 0 sinon.
- [ ] `prefillFromAi()` du composant renseigne `dateSeparation` quand `workspaceCountry === 'BELGIQUE'` et que la date est valide, reste no-op sinon.
- [ ] Le champ pré-rempli affiche un badge `auto_awesome` ; la modification manuelle remet `provenanceDateSeparation` à `null` et masque le badge.
- [ ] `getPrefillCount()` statique et `prefillFromAi()` runtime en parité stricte (test Jest cas 0 / nominal).
- [ ] Une fixture IA contenant plusieurs dates concurrentes (date du mariage ≠ date de séparation effective ≠ date de la requête) remplit `dateSeparationBe` avec la date de séparation (test backend — invariant cadrage §5.1.6).
- [ ] `coherenceAlerts` lève une alerte F-IA-03 si la date saisie diverge de la date détectée par l'IA.
- [ ] Isolation workspace : non applicable côté pré-fill — voir section dédiée.

---

## Périmètre

### Hors scope (explicite)

- Toute modification du scoring, des verdicts, des délais ou des bases juridiques de l'outil (logique métier inchangée).
- Le pré-remplissage de la voie de DDI choisie et des faits constitutifs — non factualisables de façon fiable par le LLM en V1 ; restent en saisie manuelle (documenté).
- Le champ `dateSeparation` FR de `separation-corps` / `indivision` — traité par SF-246-08.
- Tout outil Famille FR — hors périmètre de cette SF BE.
- Toute migration Liquibase — aucun nouvel outil, aucune table.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `dateSeparationBe` | `null` | ISO `YYYY-MM-DD` strict ou `null` |

Comportements à la création : aucun — la SF étend un record de réponse IA.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `dateSeparationBe` | Non | — | ISO `YYYY-MM-DD` ; rejet sinon → `null` | Non | `isoDateOrNull()` |

Notes :
- Champ nullable — invariant cadrage §5.1.2 : une date non identifiée de façon fiable reste `null`.
- Format ISO strict cohérent avec `dateAcceptationPV` (F-239).
- Champ **distinct** de `dateSeparation` (FR, SF-246-08) — invariant cadrage §5.1.1.

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/divorce-desunion-be` | Oui | LAWYER |
| GET | `/api/v1/case-files/{caseFileId}/divorce-desunion-be` | Oui | MEMBER |

> Endpoints **inchangés** (existants F-FA-11). La SF n'ajoute aucun endpoint.

### Contrat API figé (parallélisation back / front)

**Champ JSON produit par le pipeline IA** (sous `analysis_result.famille_extracted_data`) :

```json
"date_separation_be": "2024-11-15"
```

**Record backend `FamilleExtractedData`** — 1 champ ajouté (en fin de record, après le champ SF-246-11) :

```java
// SF-246-12 : date de séparation effective pour pré-fill divorce-desunion-be
// (Famille BELGIQUE uniquement, nullable). Distinct de dateSeparation FR (SF-246-08).
String dateSeparationBe
```

**DTO frontend `FamilleExtractedData`** (`divorce-accepte.model.ts`) — 1 champ ajouté :

```ts
/** SF-246-12 : date de séparation effective pour pré-fill divorce-desunion-be (BE uniquement).
 *  Distinct de dateSeparation FR (SF-246-08). */
dateSeparationBe?: string | null;
```

**Helper `DivorceDesunionBePrefillInput`** — contrat figé :

```ts
export interface DivorceDesunionBePrefillInput {
  aiData?: Pick<FamilleExtractedData, 'dateSeparationBe'> | null;
  workspaceCountry?: string;
}
```

`computePrefillCount(input)` retourne 1 si `workspaceCountry === 'BELGIQUE'` et la date valide, 0 sinon.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| — | — | Aucune table impactée. `familleExtractedData` sérialisé dans le JSON de synthèse |

### Migration Liquibase

- [ ] Oui
- [x] Non applicable — aucun nouvel outil, aucune table, aucune règle `decision_tool_visibility_rules`.

### Composants Angular (si applicable)

- `DivorceDesunionBeSectionComponent` — `prefillFromAi()` rendu effectif, signal `provenanceDateSeparation`, handler `onDateSeparationChange()`, badge `auto_awesome`, extension `coherenceAlerts`.
- `divorce-desunion-be-section-prefill-rules.ts` — lecture de `aiData.dateSeparationBe` réel, `computePrefillCount()` recalculé.

---

## Plan de test

### Tests unitaires

- [ ] `extractFamilleData()` — cas nominal : `date_separation_be` présent → champ renseigné.
- [ ] `extractFamilleData()` — champ absent → `dateSeparationBe` `null`, pas d'exception.
- [ ] `extractFamilleData()` — date non ISO → `null` (fail-open).
- [ ] `LegalDomainPromptBuilderTest` — `FAMILLE_INSTRUCTION` contient la clé `date_separation_be` + la distinction d'avec date du mariage / requête / `date_accord_initial_divorce`.
- [ ] `computePrefillCount()` — cas (a) `aiData` vide / date non ISO → 0 ; cas (c) date valide + BE → 1.
- [ ] `computePrefillCount()` — `workspaceCountry = 'FRANCE'` → 0.
- [ ] `prefillFromAi()` — date valide → `dateSeparation` renseigné, badge présent, `provenanceDateSeparation = 'IA'`.
- [ ] `prefillFromAi()` — parité stricte avec `getPrefillCount()`.
- [ ] `onDateSeparationChange()` — modification manuelle remet `provenanceDateSeparation` à `null`.
- [ ] `coherenceAlerts` — alerte levée si la date saisie diverge de la date détectée.

### Tests d'intégration

- [ ] Analyse IA d'un dossier famille BE fixture avec divorce pour désunion irrémédiable → la synthèse expose `date_separation_be` peuplé.
- [ ] **Fixture multi-dates** (invariant cadrage §5.1.6) : dossier contenant date du mariage, date de séparation effective et date de la requête distinctes → `dateSeparationBe` = date de séparation, aucune confusion.
- [ ] Dossier famille FR → `dateSeparationBe` reste `null`.
- [ ] `GET /api/v1/case-files/{caseFileId}/divorce-desunion-be` → 403 si workspace différent (non-régression endpoint existant).

### Isolation workspace

- [x] Applicable — vérifiée au niveau de l'endpoint F-FA-11 existant (test de non-régression conservé). Le champ IA n'introduit aucun nouvel accès.

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
| `DivorceDesunionBeSectionComponent` | `prefillFromAi()` devient effectif sur `dateSeparation` | Tests Jest pré-fill cas 0 / nominal |
| `extractFamilleData()` | Tout consommateur de `FamilleExtractedData` reçoit 1 champ supplémentaire (additif, nullable) | Tests d'extraction existants conservés verts |
| `decisional-tools-panel` | Badge « Pré-rempli par l'IA (N) » de l'outil passe de 0 à 1 | Test Jest `getPrefillCount` |
| Autres outils Famille BE consommant `familleExtractedData` | Aucun — champ additif ignoré | Compilation TS + tests existants |

### Smoke tests E2E concernés

- [x] `e2e/smoke/case-analysis-flow.spec.ts` — flux d'analyse de dossier reste vert.
- [x] `e2e/smoke/happy-path.spec.ts` — parcours nominal inchangé.
- [x] `cd e2e && npm test` avant push (préoccupation transversale « outil décisionnel »).

---

## Dépendances

### Subfeatures bloquantes

- **SF-246-11** — couplage de fichier (record `FamilleExtractedData`, prompt `FAMILLE_INSTRUCTION`, `extractFamilleData()`, DTO `divorce-accepte.model.ts`). SF-246-12 doit être développée **après** le merge de SF-246-11 et rebasée sur master à jour — voir « Notes et décisions ».

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` touchée.

---

## Notes et décisions

### Couplage de fichiers partagés — ordre de dev de la série Famille

SF-246-12 modifie les **mêmes fichiers backend partagés** que les autres SF Famille : record `FamilleExtractedData` (+ builder F-234), prompt `FAMILLE_INSTRUCTION`, méthode `extractFamilleData()`, DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`).

**Décision — ordre de dev de toute la série Famille (vagues 2 → 4)** :

```
SF-246-06 → SF-246-07 → SF-246-08 → SF-246-09 → SF-246-10 → SF-246-11 → SF-246-12 → SF-246-03
```

SF-246-12 ajoute son champ **après** celui de SF-246-11, branchée après le merge de SF-246-11 et rebasée sur master à jour. Ne **jamais** développer deux SF Famille sur deux branches simultanées modifiant `FamilleExtractedData` — conflit de rebase systématique. La parallélisation backend / frontend **intra-SF** reste autorisée (contrat API figé ci-dessus).

### Décision séparation FR / BE — deux champs distincts

`dateSeparationBe` (cette SF) est un champ **distinct** de `dateSeparation` (FR, SF-246-08). La cessation effective de la vie commune au sens du DDI belge (point de départ des délais de 6 mois / 1 an — CJ art. 1255 §1) et la séparation de fait FR (`separation-corps`, `indivision`) sont deux notions juridiques au régime différent. Conformément à l'invariant « un champ = une définition juridique sans ambiguïté » (cadrage §5.1.1), on ne mutualise pas : le prompt remplit `date_separation_be` **uniquement** pour les dossiers BE et `date_separation` **uniquement** pour les dossiers FR, jamais les deux.
