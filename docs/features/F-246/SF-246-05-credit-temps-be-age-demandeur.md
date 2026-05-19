# Mini-spec — [F-246 / SF-246-05] Pré-remplissage IA — Crédit-temps fin de carrière (âge du demandeur) (Travail BE)

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Découpage de référence : `docs/features/F-246/cadrage-decoupage.md` (§3.1 ligne SF-246-05, vague 4).
> **Outil mono** : `credit-temps-be` (F-DT-29) — adossé au record `TravailExtractedData` et au
> prompt `TRAVAIL_INSTRUCTION`.

---

## Identifiant

`F-246 / SF-246-05`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-246-05-credit-temps-be-prefill`

---

## Objectif

> En une phrase : que fait cette subfeature ?

Brancher le pré-remplissage IA de l'outil `credit-temps-be` (F-DT-29, Travail BE) en faisant extraire par le pipeline IA l'âge du demandeur — champ aujourd'hui stubé côté frontend (`ageDemandeurAnnees`) sans source backend — afin que `prefillFromAi()` renseigne l'âge du formulaire.

---

## Comportement attendu

### Cas nominal

1. L'avocat lance l'analyse IA d'un dossier de droit du travail belge évoquant une demande de crédit-temps fin de carrière (CCT 103, ONEM).
2. Le pipeline IA (prompt `LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION`) extrait, dans `travail_extracted_data`, le champ `age_demandeur_annees` (entier, âge du travailleur demandeur).
3. L'extracteur `extractTravailData()` parse le champ en `TravailExtractedData.ageDemandeurAnnees`.
4. Le DTO frontend `TravailExtractedData` (`case-analysis.model.ts`) expose le champ ; le `TOOL_REGISTRY` le passe déjà via `aiData: ctx.synthesis?.travailExtractedData`.
5. À l'ouverture de l'outil `credit-temps-be`, `prefillFromAi()` renseigne `ageDemandeur` ; un badge `auto_awesome` « Pré-rempli depuis l'analyse » s'affiche.
6. L'avocat peut modifier la valeur : le handler `onAgeDemandeurChange()` remet `provenanceAgeDemandeur` à `null` et déclenche la revérification F-IA-03.
7. Le badge « Pré-rempli par l'IA (N champs) » du panel F-IA-04 reflète `getPrefillCount()` (passe de 0 à 1 quand l'âge est détecté).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Le LLM ne détecte pas l'âge du demandeur | `age_demandeur_annees` à `null` ; `prefillFromAi()` no-op gracieux ; `getPrefillCount()` = 0 ; aucun badge | n/a |
| Âge négatif ou aberrant (> 100) | `boundedIntOrNull()` garde de plage `[0, 100]` → `null` | n/a |
| Dossier de droit du travail français | Champ BE reste `null` (le prompt impose null hors BE) ; outil non affiché pour la FR | n/a |
| Confusion avec un autre âge ou une durée du dossier (ancienneté, durée de contrat) | Le prompt nomme explicitement « âge en années révolues du travailleur qui demande le crédit-temps » — distinct de l'ancienneté et de toute durée | n/a |
| `travail_extracted_data` absent du JSON IA | `extractTravailData()` retourne `null` ; outil affiché formulaire vierge | n/a |
| `aiData` arrive après le premier rendu | `prefillFromAi()` réinvoqué dans `ngOnChanges()` | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : seul `credit-temps-be` (F-DT-29) consomme l'âge du demandeur. Aucun autre outil Travail BE ne raisonne sur l'âge du travailleur comme tel. Le concept « âge » existe en Famille (`adoption` SF-246-09, `autorite-parentale` SF-246-10) — mais sur un **record distinct** (`FamilleExtractedData`) et un **acteur distinct** (adoptant/enfant, pas travailleur) : champs séparés assumés (un champ = une définition juridique, cadrage §5.1.1).
- [x] **Autres pays** : Belgique uniquement. Le crédit-temps fin de carrière est un dispositif belge (CCT 103) ; pas d'équivalent FR dans cet outil. Champ `null` pour la FR.
- [x] **Autres domaines** : non applicable — concept propre au droit du travail belge.
- [x] **Autres UI patterns** : pré-remplissage IA (pattern canonique), badge `auto_awesome`, alertes F-IA-03.
- [x] **Autres flows transversaux** : aucun.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : `TravailExtractedData` dans `case-analysis.model.ts` — le champ `ageDemandeurAnnees` est aujourd'hui un **stub frontend** sans source backend ; la SF le branche.
- [x] **Record / DTO backend** : `CaseAnalysisResponse.TravailExtractedData` + builder F-234.
- [x] **Service / logique métier** : `extractTravailData()`.
- [x] **Entité JPA + schéma DB** : non applicable — `travailExtractedData` sérialisé dans la synthèse IA. Inputs de F-DT-29 persistés par son endpoint existant (inchangé).
- [x] **Tests existants** : `credit-temps-be-section-prefill-rules.spec.ts` (helper avec stub `ageDemandeurAnnees`), tests `extractTravailData()`. Mis à jour par la SF.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : oui — l'âge saisi est croisable avec la détection IA. `coherenceAlerts` étendu sur `AGE_DEMANDEUR`.
- [x] **Refresh dashboard (F-IA-02)** : inchangé — `triggerRefresh()` déjà câblé.
- [x] **Pré-remplissage IA** : objet de la SF.
- [x] **Persistance des inputs** : inchangée — endpoint F-DT-29 existant.
- [x] **Masquage conditionnel selon type** : inchangé — visibilité F-IA-04 déjà gérée (BE + Travail).
- [x] **Alertes actives après calcul** : gate `coherenceAlerts` = `!this.showForm()` uniquement — vérifié.

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — aucun nouveau composant partagé, service ou endpoint.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `credit-temps-be` (F-DT-29) | Oui | Intégré dans cette SF |
| Outils Travail FR | Non | Le crédit-temps fin de carrière est un dispositif belge — champ `null` en FR |
| Outils Famille consommant un âge | Non | Record + acteur distincts (`FamilleExtractedData`, adoptant/enfant) — champs séparés |
| Outils Immigration | Non | Concept propre au droit du travail |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s) créée(s)
- [ ] Backlog VN
- [x] Non applicable aux autres cibles (justification explicite ci-dessus)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [ ] **Non applicable** — la SF livre une partie frontend décisionnelle (`credit-temps-be-section`).

### 1. Cohérence visuelle

- [x] **Palette statut** : navy/or info, vert OK, rouge réservé aux alertes critiques — conservé.
- [x] **Datepicker** : non applicable — la SF n'ajoute pas de champ date.
- [x] **Typographie** : `JetBrains Mono` pour `baseJuridique` / `formule`, `Inter` pour le reste — conservé.
- [x] **Gate `workspaceCountry`** : outil BE-only — bannière info en cas de mismatch FR (existant).
- [x] **Erreurs** : `MatSnackBar` — pas d'`alert()` / `confirm()`.
- [x] **Refresh dashboard** : `triggerRefresh()` dans le `next:` du POST — existant, inchangé.

### 2. Pré-fill IA (OBLIGATOIRE)

- [x] `@Input() aiData?: TravailExtractedData | null` — déjà typé strictement.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` **ET** `ngOnChanges()` — la SF rend le corps effectif.
- [x] Signal `provenanceAgeDemandeur = signal<'IA' | null>(null)`.
- [x] Badge `auto_awesome` « Pré-rempli depuis l'analyse » à côté de `ageDemandeur`.
- [x] Handler `onAgeDemandeurChange()` qui remet `provenanceAgeDemandeur` à `null`.

### 3. Validation F-IA-03 (OBLIGATOIRE)

- [x] `coherenceAlerts` : alerte `AGE_DEMANDEUR` quand l'âge saisi diverge de l'âge détecté par l'IA.
- [x] Hiérarchie F-96 > Question IA > IA détection > Pièce manquante ; convergence → `'MULTI'`.
- [x] `<app-coherence-popover-trigger>` câblé sur `ageDemandeur`.
- [x] Helper partagé `CoherenceAlertBuilder`.

### 4. TOOL_REGISTRY symétrique + `getPrefillCount(input)`

- [x] Entrée F-DT-29 (`credit-temps-be`) déjà présente dans `TOOL_REGISTRY` ; `inputs(ctx)` passe déjà `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`.
- [x] Static `getPrefillCount(input)` du composant : appelle `CreditTempsBePrefillRules.computePrefillCount()` enrichi.
- [x] Parité stricte `getPrefillCount()` ↔ `prefillFromAi()` : mêmes guards, même condition `workspaceCountry === 'BELGIQUE'`.
- [x] Tests Jest : (a) 0 champ (aiData vide / FR), (b) — pas de cas « partiel » (1 seul champ), (c) 1 champ cas nominal.
- [x] `tool_id` `credit-temps-be` (F-DT-29) déjà présent dans `KNOWN_FRONTEND_TOOL_IDS` du test `DecisionToolVisibilityIntegrityIT` — pas de migration.

### 5. Parité des domaines métier (niveau ≥ 5)

- [x] Niveau du tool : **5** (analyse d'éligibilité au crédit-temps fin de carrière — conditions d'âge et d'ancienneté).

| Domaine | Équivalent existe ? | Si non → action |
|---------|---------------------|-----------------|
| Droit du travail | Oui (F-DT-29) | C'est l'outil de cette SF (Travail BE) |
| Immigration | Non | Le crédit-temps n'a pas de transposition en droit des étrangers — concept non pertinent |
| Famille | Non | Concept non pertinent en droit de la famille |

> La SF complète le pré-fill d'un outil existant — la parité de domaine de F-DT-29 a été tranchée à sa création.

---

## Champs IA à extraire (pré-remplissage)

- [ ] **Aucun pré-remplissage** — non, la SF crée le pré-remplissage.

| Champ du formulaire | Type | Champ source du record `TravailExtractedData` | Extension requise |
|---------------------|------|------------------------------------------------|-------------------|
| `ageDemandeur` | nombre (entier, années) | `TravailExtractedData.ageDemandeurAnnees` (`Integer`, nullable) | [x] record + [x] prompt `LegalDomainPromptBuilder` (`TRAVAIL_INSTRUCTION`) + [x] extracteur (`extractTravailData()`) + [x] DTO frontend (remplace le stub) |

- [x] Pour le champ valeur à pré-remplir non encore présent dans la chaîne backend (`ageDemandeurAnnees`), l'extension du record `TravailExtractedData` **et** du prompt `LegalDomainPromptBuilder` (`TRAVAIL_INSTRUCTION`) est explicitement dans le périmètre de cette SF.

> **Note couplage** : voir « Notes et décisions » — SF-246-05 touche `TravailExtractedData` / `TRAVAIL_INSTRUCTION` / `extractTravailData()` ; **indépendante** de la série Famille (record distinct) → parallélisable.

> **Note de design IA** : `ageDemandeurAnnees` est extrait directement (entier) et non dérivé d'une date de naissance — le formulaire F-DT-29 raisonne en âge à la date de la demande de crédit-temps (la condition CCT 103 est un âge minimum). Le prompt impose « âge en années révolues du travailleur qui demande le crédit-temps fin de carrière, à la date de sa demande ; null si non déterminable ». La distinction explicite d'avec l'ancienneté et toute durée de contrat est inscrite au prompt (invariant cadrage §5.1.1). Les autres champs du formulaire (ancienneté, régime de travail) restent en saisie manuelle ou couverts par les champs existants (`anciennete` déjà branché — cf. cadrage §2.2).

---

## Critères d'acceptation

- [ ] Le record `TravailExtractedData` contient le nouveau champ `ageDemandeurAnnees` (`Integer`, nullable), propagé par le builder F-234.
- [ ] Le prompt `TRAVAIL_INSTRUCTION` décrit la clé `age_demandeur_annees` avec une définition juridique sans ambiguïté (« âge en années révolues du travailleur demandeur, à la date de la demande de crédit-temps ») + l'instruction `null` hors BE / hors certitude + la distinction explicite d'avec l'ancienneté.
- [ ] `extractTravailData()` parse `age_demandeur_annees` via `boundedIntOrNull(_, _, 0, 100)`.
- [ ] Le DTO frontend `TravailExtractedData` (`case-analysis.model.ts`) expose `ageDemandeurAnnees?: number | null` — le champ cesse d'être un stub.
- [ ] `CreditTempsBePrefillRules` lit `aiData.ageDemandeurAnnees` réel ; `computePrefillCount()` retourne 1 si l'âge est valide et BE, 0 sinon.
- [ ] `prefillFromAi()` du composant renseigne `ageDemandeur` quand `workspaceCountry === 'BELGIQUE'`, et reste no-op si FR.
- [ ] Le champ pré-rempli affiche un badge `auto_awesome` ; la modification manuelle remet `provenanceAgeDemandeur` à `null` et masque le badge.
- [ ] `getPrefillCount()` statique et `prefillFromAi()` runtime en parité stricte (test Jest cas 0 / nominal).
- [ ] Une fixture IA mentionnant à la fois l'âge du travailleur (58 ans) et son ancienneté (25 ans) → `ageDemandeurAnnees` = 58, aucune confusion (test backend — invariant cadrage §5.1.1).
- [ ] `coherenceAlerts` lève une alerte F-IA-03 si l'âge saisi diverge de l'âge détecté par l'IA.
- [ ] Isolation workspace : non applicable côté pré-fill — voir section dédiée.

---

## Périmètre

### Hors scope (explicite)

- Toute modification du scoring, des verdicts ou des bases juridiques de F-DT-29 (logique métier inchangée).
- Le pré-remplissage des autres champs du formulaire (régime de travail, type de crédit-temps) — non factualisables de façon fiable par le LLM en V1 ; restent en saisie manuelle (documenté). L'ancienneté reste couverte par le champ `anciennete` existant.
- Tout outil Travail FR — le crédit-temps est un dispositif belge.
- Toute migration Liquibase — aucun nouvel outil, aucune table.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `ageDemandeurAnnees` | `null` | entier `[0, 100]` ou `null` |

Comportements à la création : aucun — la SF étend un record de réponse IA.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `ageDemandeurAnnees` | Non | — | entier `[0, 100]` ; hors plage → `null` | Non | `boundedIntOrNull(_, _, 0, 100)` |

Notes :
- Champ nullable — invariant cadrage §5.1.2 : un âge non identifié de façon fiable reste `null`, jamais `0`.

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/credit-temps-be` | Oui | LAWYER |
| GET | `/api/v1/case-files/{caseFileId}/credit-temps-be` | Oui | MEMBER |

> Endpoints **inchangés** (existants F-DT-29). La SF n'ajoute aucun endpoint.

### Contrat API figé (parallélisation back / front)

**Champ JSON produit par le pipeline IA** (sous `analysis_result.travail_extracted_data`) :

```json
"age_demandeur_annees": 58
```

**Record backend `TravailExtractedData`** — 1 champ ajouté (en fin de record, après les champs SF-246-02) :

```java
// SF-246-05 : âge du demandeur pour pré-fill F-DT-29 crédit-temps fin de carrière
// (Travail BELGIQUE uniquement, nullable).
Integer ageDemandeurAnnees
```

**DTO frontend `TravailExtractedData`** (`case-analysis.model.ts`) — champ branché (remplace le stub) :

```ts
/** SF-246-05 : âge du demandeur pour pré-fill F-DT-29 crédit-temps (BE uniquement). */
ageDemandeurAnnees?: number | null;
```

**Helper `CreditTempsBePrefillInput`** — contrat figé :

```ts
export interface CreditTempsBePrefillInput {
  aiData?: Pick<TravailExtractedData, 'ageDemandeurAnnees'> | null;
  workspaceCountry?: string;
}
```

`computePrefillCount(input)` retourne 1 si `workspaceCountry === 'BELGIQUE'` et `ageDemandeurAnnees` valide, 0 sinon.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| — | — | Aucune table impactée. `travailExtractedData` sérialisé dans le JSON de synthèse |

### Migration Liquibase

- [ ] Oui
- [x] Non applicable — aucun nouvel outil, aucune table, aucune règle `decision_tool_visibility_rules`.

### Composants Angular (si applicable)

- `CreditTempsBeSectionComponent` — `prefillFromAi()` rendu effectif, signal `provenanceAgeDemandeur`, handler `onAgeDemandeurChange()`, badge `auto_awesome`, extension `coherenceAlerts`.
- `credit-temps-be-section-prefill-rules.ts` — lecture de `aiData.ageDemandeurAnnees` réel (cesse d'être un stub), `computePrefillCount()` recalculé.

---

## Plan de test

### Tests unitaires

- [ ] `extractTravailData()` — cas nominal : `age_demandeur_annees` présent → champ renseigné.
- [ ] `extractTravailData()` — champ absent → `ageDemandeurAnnees` `null`, pas d'exception.
- [ ] `extractTravailData()` — âge hors `[0, 100]` → `null` (garde de plage).
- [ ] `LegalDomainPromptBuilderTest` — `TRAVAIL_INSTRUCTION` contient la clé `age_demandeur_annees` + la distinction d'avec l'ancienneté.
- [ ] `computePrefillCount()` — cas (a) `aiData` vide → 0 ; cas (c) âge valide + BE → 1.
- [ ] `computePrefillCount()` — `workspaceCountry = 'FRANCE'` → 0.
- [ ] `prefillFromAi()` — âge valide → `ageDemandeur` renseigné, badge présent, `provenanceAgeDemandeur = 'IA'`.
- [ ] `prefillFromAi()` — parité stricte avec `getPrefillCount()`.
- [ ] `onAgeDemandeurChange()` — modification manuelle remet `provenanceAgeDemandeur` à `null`.
- [ ] `coherenceAlerts` — alerte levée si l'âge saisi diverge de l'âge détecté.

### Tests d'intégration

- [ ] Analyse IA d'un dossier travail BE fixture avec demande de crédit-temps → la synthèse expose `age_demandeur_annees` peuplé.
- [ ] **Fixture multi-valeurs** (invariant cadrage §5.1.1) : dossier mentionnant l'âge du travailleur ET son ancienneté → `ageDemandeurAnnees` = âge, aucune confusion avec l'ancienneté.
- [ ] Dossier travail FR → `ageDemandeurAnnees` reste `null`.
- [ ] `GET /api/v1/case-files/{caseFileId}/credit-temps-be` → 403 si workspace différent (non-régression endpoint existant).

### Isolation workspace

- [x] Applicable — vérifiée au niveau de l'endpoint F-DT-29 existant (test de non-régression conservé). Le champ IA n'introduit aucun nouvel accès.

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
| `CreditTempsBeSectionComponent` | `prefillFromAi()` devient effectif sur `ageDemandeur` | Tests Jest pré-fill cas 0 / nominal |
| `extractTravailData()` | Tout consommateur de `TravailExtractedData` reçoit 1 champ supplémentaire (additif, nullable) | Tests d'extraction existants conservés verts |
| `decisional-tools-panel` | Badge « Pré-rempli par l'IA (N) » de F-DT-29 passe de 0 à 1 | Test Jest `getPrefillCount` |
| Autres outils Travail consommant `travailExtractedData` | Aucun — champ additif ignoré | Compilation TS + tests existants |

### Smoke tests E2E concernés

- [x] `e2e/smoke/case-analysis-flow.spec.ts` — flux d'analyse de dossier reste vert.
- [x] `e2e/smoke/happy-path.spec.ts` — parcours nominal inchangé.
- [x] `cd e2e && npm test` avant push (préoccupation transversale « outil décisionnel »).

---

## Dépendances

### Subfeatures bloquantes

- Aucune SF bloquante. SF-246-05 touche `TravailExtractedData` / `TRAVAIL_INSTRUCTION` / `extractTravailData()` — **aucun fichier partagé avec la série Famille** (SF-246-03/06→12). Couplage de fichier potentiel avec SF-246-01 / SF-246-02 (vague 1, déjà mergées) — la SF ajoute son champ **après** ceux de SF-246-02 dans le record. SF-246-05 est **parallélisable** avec toute SF Famille de la vague 4.

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` touchée.

---

## Notes et décisions

### Couplage de fichiers — SF-246-05 indépendante de la série Famille

SF-246-05 touche `TravailExtractedData` / `TRAVAIL_INSTRUCTION` / `extractTravailData()` — les **mêmes** fichiers que les SF de la vague 1 (SF-246-01 / 02, déjà mergées), mais **aucun fichier partagé** avec la série Famille (SF-246-03 / 06 → 12, qui touchent `FamilleExtractedData`).

**Conséquence** : SF-246-05 peut être développée **en parallèle** de n'importe quelle SF Famille de la vague 4 (SF-246-03, 11, 12) sans risque de conflit de rebase. Elle ajoute son unique champ `ageDemandeurAnnees` en fin de record `TravailExtractedData`, après les champs SF-246-02. La parallélisation backend / frontend **intra-SF** reste autorisée (contrat API figé ci-dessus).

### Décision âge extrait vs dérivé d'une date de naissance

`ageDemandeurAnnees` est extrait directement en années entières et non calculé depuis une date de naissance : le formulaire F-DT-29 raisonne en âge à la date de la demande (condition d'âge minimum CCT 103). Extraire un âge déjà contextualisé évite à `prefillFromAi()` de devoir choisir une date de référence (date de demande souvent absente ou ambiguë dans les pièces). Le prompt impose « âge en années révolues à la date de la demande, null si non déterminable ».
