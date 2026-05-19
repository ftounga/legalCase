# Mini-spec — [F-246 / SF-246-11] Pré-remplissage IA — Changement d'état civil (date de naissance) (Famille FR)

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Découpage de référence : `docs/features/F-246/cadrage-decoupage.md` (§3.1 ligne SF-246-11, vague 4).
> **Outil mono** : `changement-etat-civil` (F-FA-26) — adossé au record `FamilleExtractedData`
> et au prompt `FAMILLE_INSTRUCTION`.

---

## Identifiant

`F-246 / SF-246-11`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-246-11-changement-etat-civil-prefill`

---

## Objectif

> En une phrase : que fait cette subfeature ?

Brancher le pré-remplissage IA de l'outil `changement-etat-civil` (F-FA-26, Famille FR) en faisant extraire par le pipeline IA la date de naissance du demandeur — aujourd'hui absente de `FamilleExtractedData` — afin que `prefillFromAi()` renseigne le champ `dateNaissanceDemandeur` du formulaire.

---

## Comportement attendu

### Cas nominal

1. L'avocat lance l'analyse IA d'un dossier de droit de la famille FR évoquant un changement de nom, de prénom ou de mention de sexe à l'état civil (art. 60+, 61-1+ Cciv).
2. Le pipeline IA (prompt `LegalDomainPromptBuilder.FAMILLE_INSTRUCTION`) extrait, dans `famille_extracted_data`, le champ `date_naissance_demandeur` au format ISO `YYYY-MM-DD`.
3. L'extracteur `extractFamilleData()` parse le champ en `FamilleExtractedData.dateNaissanceDemandeurDetectee`.
4. Le DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`) expose le champ ; l'entrée `TOOL_REGISTRY` de `changement-etat-civil` passe déjà `aiData: ctx.synthesis?.familleExtractedData`.
5. À l'ouverture de l'outil `changement-etat-civil`, `prefillFromAi()` renseigne `dateNaissanceDemandeur` ; un badge `auto_awesome` « Pré-rempli depuis l'analyse » s'affiche.
6. L'avocat peut modifier la valeur : le handler `onDateNaissanceChange()` remet `provenanceDateNaissance` à `null` et déclenche la revérification F-IA-03.
7. Le badge « Pré-rempli par l'IA (N champs) » du panel F-IA-04 reflète `getPrefillCount()` (passe de 0 à 1 quand la date est détectée).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Aucune date de naissance détectable dans les pièces | `date_naissance_demandeur` à `null` ; `prefillFromAi()` no-op gracieux ; `getPrefillCount()` = 0 ; aucun badge | n/a |
| Date présente mais non lisible / ambiguë | Le prompt impose `null` plutôt qu'une date approximative | n/a |
| Date hors ISO `YYYY-MM-DD` | `isoDateOrNull()` côté extracteur rejette → `null` ; pas de pré-fill | n/a |
| Confusion avec une autre date du dossier (date de l'acte de naissance, date de la requête) | Le prompt nomme explicitement « date de naissance du demandeur du changement d'état civil » — distincte de la date de l'acte et de la date de requête | n/a |
| Dossier de famille belge | Champ FR reste `null` (le prompt impose null hors FR) ; outil non affiché pour la BE | n/a |
| `famille_extracted_data` absent du JSON IA | `extractFamilleData()` retourne `null` ; outil affiché formulaire vierge | n/a |
| `aiData` arrive après le premier rendu | `prefillFromAi()` réinvoqué dans `ngOnChanges()` | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : seul `changement-etat-civil` (F-FA-26) consomme la date de naissance du demandeur du changement d'état civil. D'autres outils Famille raisonnent sur des dates de naissance d'enfant (`recherche-paternite`, `reconnaissance-paternelle` — SF-246-09) ou des âges (`adoption` — SF-246-09) : ce sont des **acteurs distincts** (enfant ≠ demandeur du changement d'état civil) → champs séparés assumés (un champ = une définition juridique, cadrage §5.1.1). Le flag `changement_etat_civil_envisage` (F-200) pilote la **visibilité** — finalité distincte du pré-fill.
- [x] **Autres pays** : France uniquement. Le changement d'état civil BE relève de procédures distinctes (CC belge, tribunal de la famille) — pas d'outil concerné dans cette vague. Champ `null` pour la BE.
- [x] **Autres domaines** : non applicable — concept propre au droit de l'état des personnes FR.
- [x] **Autres UI patterns** : pré-remplissage IA (pattern canonique), badge `auto_awesome`, alertes F-IA-03.
- [x] **Autres flows transversaux** : aucun.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : `FamilleExtractedData` dans `divorce-accepte.model.ts` — extension + **réalignement** (retrait du champ aspirationnel `dateNaissanceDemandeurDetectee` s'il est déjà déclaré sans source — cf. cadrage §5.3).
- [x] **Record / DTO backend** : `CaseAnalysisResponse.FamilleExtractedData` + builder F-234.
- [x] **Service / logique métier** : `extractFamilleData()`.
- [x] **Entité JPA + schéma DB** : non applicable — `familleExtractedData` sérialisé dans la synthèse IA. Inputs de F-FA-26 persistés par son endpoint existant (inchangé).
- [x] **Tests existants** : `changement-etat-civil-section-prefill-rules.spec.ts`, tests `extractFamilleData()`. Mis à jour par la SF.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : oui — la date saisie est croisable avec la détection IA. `coherenceAlerts` étendu sur `DATE_NAISSANCE_DEMANDEUR`.
- [x] **Refresh dashboard (F-IA-02)** : inchangé — `triggerRefresh()` déjà câblé.
- [x] **Pré-remplissage IA** : objet de la SF.
- [x] **Persistance des inputs** : inchangée — endpoint F-FA-26 existant.
- [x] **Masquage conditionnel selon type** : inchangé — visibilité F-IA-04 déjà gérée (FR + flag F-200 `changement_etat_civil_envisage`).
- [x] **Alertes actives après calcul** : gate `coherenceAlerts` = `!this.showForm()` uniquement — vérifié.

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — aucun nouveau composant partagé, service ou endpoint.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `changement-etat-civil` (F-FA-26) | Oui | Intégré dans cette SF |
| Flag `changement_etat_civil_envisage` (F-200) | Non | Flag de visibilité — finalité distincte du pré-fill |
| Outils Famille FR consommant une date de naissance d'enfant (SF-246-09) | Non | Acteur distinct (enfant ≠ demandeur) — champs séparés |
| Outils Travail / Immigration | Non | Concept propre au droit de l'état des personnes |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s) créée(s)
- [ ] Backlog VN
- [x] Non applicable aux autres cibles (justification explicite ci-dessus)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [ ] **Non applicable** — la SF livre une partie frontend décisionnelle (`changement-etat-civil-section`).

### 1. Cohérence visuelle

- [x] **Palette statut** : navy/or info, vert OK, rouge réservé aux alertes critiques — conservé.
- [x] **Datepicker** : `<input type="date">` natif pour `dateNaissanceDemandeur` — pas de `MatDatepicker`.
- [x] **Typographie** : `JetBrains Mono` pour `baseJuridique` / `formule`, `Inter` pour le reste — conservé.
- [x] **Gate `workspaceCountry`** : outil FR-only — bannière info en cas de mismatch BE (existant).
- [x] **Erreurs** : `MatSnackBar` — pas d'`alert()` / `confirm()`.
- [x] **Refresh dashboard** : `triggerRefresh()` dans le `next:` du POST — existant, inchangé.

### 2. Pré-fill IA (OBLIGATOIRE)

- [x] `@Input() aiData?: FamilleExtractedData | null` — déjà typé strictement.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` **ET** `ngOnChanges()` — la SF rend le corps effectif.
- [x] Signal `provenanceDateNaissance = signal<'IA' | null>(null)`.
- [x] Badge `auto_awesome` « Pré-rempli depuis l'analyse » à côté de `dateNaissanceDemandeur`.
- [x] Handler `onDateNaissanceChange()` qui remet `provenanceDateNaissance` à `null`.

### 3. Validation F-IA-03 (OBLIGATOIRE)

- [x] `coherenceAlerts` : alerte `DATE_NAISSANCE_DEMANDEUR` quand la date saisie diverge de la date détectée par l'IA.
- [x] Hiérarchie F-96 > Question IA > IA détection > Pièce manquante ; convergence → `'MULTI'`.
- [x] `<app-coherence-popover-trigger>` câblé sur `dateNaissanceDemandeur`.
- [x] Helper partagé `CoherenceAlertBuilder`.

### 4. TOOL_REGISTRY symétrique + `getPrefillCount(input)`

- [x] Entrée F-FA-26 (`changement-etat-civil`) déjà présente dans `TOOL_REGISTRY` ; `inputs(ctx)` passe déjà `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`.
- [x] Static `getPrefillCount(input)` du composant : appelle `ChangementEtatCivilPrefillRules.computePrefillCount()` enrichi.
- [x] Parité stricte `getPrefillCount()` ↔ `prefillFromAi()` : même guard `ISO_DATE_RE`, même condition `workspaceCountry === 'FRANCE'`.
- [x] Tests Jest : (a) 0 champ (aiData vide / BE / date non ISO), (b) — pas de cas « partiel » (1 seul champ), (c) 1 champ cas nominal.
- [x] `tool_id` `changement-etat-civil` (F-FA-26) déjà présent dans `KNOWN_FRONTEND_TOOL_IDS` du test `DecisionToolVisibilityIntegrityIT` — pas de migration.

### 5. Parité des domaines métier (niveau ≥ 5)

- [x] Niveau du tool : **3-4** (arbre décisionnel / checklist de la procédure de changement d'état civil — non applicable au bloc 5).

> Bloc 5 **non applicable** : `changement-etat-civil` est un outil de niveau 3-4 (orientation procédurale par type de changement), pas un scoring / comparateur / détecteur d'événement de niveau ≥ 5. La SF complète le pré-fill d'un outil existant — pas de création d'outil de niveau ≥ 5.

---

## Champs IA à extraire (pré-remplissage)

- [ ] **Aucun pré-remplissage** — non, la SF crée le pré-remplissage.

| Champ du formulaire | Type | Champ source du record `FamilleExtractedData` | Extension requise |
|---------------------|------|------------------------------------------------|-------------------|
| `dateNaissanceDemandeur` | date (ISO YYYY-MM-DD) | `dateNaissanceDemandeurDetectee` (`String`, nullable) | [x] record + [x] prompt `LegalDomainPromptBuilder` (`FAMILLE_INSTRUCTION`) + [x] extracteur (`extractFamilleData()`) + [x] DTO frontend |

- [x] Pour le champ date à pré-remplir non encore présent dans la chaîne backend (`dateNaissanceDemandeurDetectee`), l'extension du record `FamilleExtractedData` **et** du prompt `LegalDomainPromptBuilder` (`FAMILLE_INSTRUCTION`) est explicitement dans le périmètre de cette SF.

> **Note couplage record partagé** : voir « Notes et décisions » — SF-246-11 séquentielle dans la série Famille (vague 4).

> **Note de design IA** : 1 champ source ajouté au record. Le prompt nomme explicitement « date de naissance du **demandeur du changement d'état civil** » — distincte des dates de naissance d'enfant de SF-246-09 (acteur différent) et de la date de l'acte de naissance / date de la requête (faits différents). Les autres champs du formulaire (type de changement, motif) restent en saisie manuelle (non factualisables de façon fiable — documenté, cadrage §5.6).

---

## Critères d'acceptation

- [ ] Le record `FamilleExtractedData` contient le nouveau champ `dateNaissanceDemandeurDetectee` (`String`, nullable), propagé par le builder F-234.
- [ ] Le prompt `FAMILLE_INSTRUCTION` décrit la clé `date_naissance_demandeur` avec une définition juridique sans ambiguïté + l'instruction `null` hors FR / hors certitude + la distinction explicite d'avec les dates de naissance d'enfant et la date de requête.
- [ ] `extractFamilleData()` parse `date_naissance_demandeur` via `isoDateOrNull()`.
- [ ] Le DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`) expose `dateNaissanceDemandeurDetectee?: string | null`.
- [ ] `ChangementEtatCivilPrefillRules` lit `aiData.dateNaissanceDemandeurDetectee`, valide via `ISO_DATE_RE` ; `computePrefillCount()` retourne 1 si la date est valide et FR, 0 sinon.
- [ ] `prefillFromAi()` du composant renseigne `dateNaissanceDemandeur` quand `workspaceCountry === 'FRANCE'` et que la date est valide, reste no-op sinon.
- [ ] Le champ pré-rempli affiche un badge `auto_awesome` ; la modification manuelle remet `provenanceDateNaissance` à `null` et masque le badge.
- [ ] `getPrefillCount()` statique et `prefillFromAi()` runtime en parité stricte (test Jest cas 0 / nominal).
- [ ] Une fixture IA contenant deux dates concurrentes (date de naissance du demandeur ≠ date de la requête) remplit `dateNaissanceDemandeurDetectee` avec la bonne date (test backend — invariant cadrage §5.1.6).
- [ ] `coherenceAlerts` lève une alerte F-IA-03 si la date saisie diverge de la date détectée par l'IA.
- [ ] Isolation workspace : non applicable côté pré-fill — voir section dédiée.

---

## Périmètre

### Hors scope (explicite)

- Toute modification de l'arbre décisionnel, des verdicts ou des bases juridiques de F-FA-26 (logique métier inchangée).
- Le pré-remplissage du type de changement et du motif — non factualisables de façon fiable par le LLM en V1 ; restent en saisie manuelle (documenté).
- Tout outil Famille BE — hors périmètre vague 4.
- Toute migration Liquibase — aucun nouvel outil, aucune table.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `dateNaissanceDemandeurDetectee` | `null` | ISO `YYYY-MM-DD` strict ou `null` |

Comportements à la création : aucun — la SF étend un record de réponse IA.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `dateNaissanceDemandeurDetectee` | Non | — | ISO `YYYY-MM-DD` ; rejet sinon → `null` | Non | `isoDateOrNull()` |

Notes :
- Champ nullable — invariant cadrage §5.1.2 : une date non identifiée de façon fiable reste `null`.
- Format ISO strict cohérent avec `dateAcceptationPV` (F-239).

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/changement-etat-civil` | Oui | LAWYER |
| GET | `/api/v1/case-files/{caseFileId}/changement-etat-civil` | Oui | MEMBER |

> Endpoints **inchangés** (existants F-FA-26). La SF n'ajoute aucun endpoint.

### Contrat API figé (parallélisation back / front)

**Champ JSON produit par le pipeline IA** (sous `analysis_result.famille_extracted_data`) :

```json
"date_naissance_demandeur": "1985-03-22"
```

**Record backend `FamilleExtractedData`** — 1 champ ajouté (en fin de record, à la position de la série Famille vague 4) :

```java
// SF-246-11 : date de naissance du demandeur pour pré-fill F-FA-26 changement d'état civil
// (Famille FR uniquement, nullable).
String dateNaissanceDemandeurDetectee
```

**DTO frontend `FamilleExtractedData`** (`divorce-accepte.model.ts`) — 1 champ ajouté :

```ts
/** SF-246-11 : date de naissance du demandeur pour pré-fill F-FA-26 (FR uniquement). */
dateNaissanceDemandeurDetectee?: string | null;
```

**Helper `ChangementEtatCivilPrefillInput`** — contrat figé :

```ts
export interface ChangementEtatCivilPrefillInput {
  aiData?: Pick<FamilleExtractedData, 'dateNaissanceDemandeurDetectee'> | null;
  workspaceCountry?: string;
}
```

`computePrefillCount(input)` retourne 1 si `workspaceCountry === 'FRANCE'` et la date valide, 0 sinon.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| — | — | Aucune table impactée. `familleExtractedData` sérialisé dans le JSON de synthèse |

### Migration Liquibase

- [ ] Oui
- [x] Non applicable — aucun nouvel outil, aucune table, aucune règle `decision_tool_visibility_rules`.

### Composants Angular (si applicable)

- `ChangementEtatCivilSectionComponent` — `prefillFromAi()` rendu effectif, signal `provenanceDateNaissance`, handler `onDateNaissanceChange()`, badge `auto_awesome`, extension `coherenceAlerts`.
- `changement-etat-civil-section-prefill-rules.ts` — lecture de `aiData.dateNaissanceDemandeurDetectee` réel, `computePrefillCount()` recalculé.

---

## Plan de test

### Tests unitaires

- [ ] `extractFamilleData()` — cas nominal : `date_naissance_demandeur` présent → champ renseigné.
- [ ] `extractFamilleData()` — champ absent → `dateNaissanceDemandeurDetectee` `null`, pas d'exception.
- [ ] `extractFamilleData()` — date non ISO → `null` (fail-open).
- [ ] `LegalDomainPromptBuilderTest` — `FAMILLE_INSTRUCTION` contient la clé `date_naissance_demandeur` + la distinction d'avec les dates de naissance d'enfant et la date de requête.
- [ ] `computePrefillCount()` — cas (a) `aiData` vide / date non ISO → 0 ; cas (c) date valide + FR → 1.
- [ ] `computePrefillCount()` — `workspaceCountry = 'BELGIQUE'` → 0.
- [ ] `prefillFromAi()` — date valide → `dateNaissanceDemandeur` renseigné, badge présent, `provenanceDateNaissance = 'IA'`.
- [ ] `prefillFromAi()` — parité stricte avec `getPrefillCount()`.
- [ ] `onDateNaissanceChange()` — modification manuelle remet `provenanceDateNaissance` à `null`.
- [ ] `coherenceAlerts` — alerte levée si la date saisie diverge de la date détectée.

### Tests d'intégration

- [ ] Analyse IA d'un dossier famille FR fixture avec changement d'état civil → la synthèse expose `date_naissance_demandeur` peuplé.
- [ ] **Fixture multi-dates** (invariant cadrage §5.1.6) : dossier contenant la date de naissance du demandeur ET la date de la requête → `dateNaissanceDemandeurDetectee` = date de naissance, aucune confusion.
- [ ] Dossier famille BE → `dateNaissanceDemandeurDetectee` reste `null`.
- [ ] `GET /api/v1/case-files/{caseFileId}/changement-etat-civil` → 403 si workspace différent (non-régression endpoint existant).

### Isolation workspace

- [x] Applicable — vérifiée au niveau de l'endpoint F-FA-26 existant (test de non-régression conservé). Le champ IA n'introduit aucun nouvel accès.

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
| `ChangementEtatCivilSectionComponent` | `prefillFromAi()` devient effectif sur `dateNaissanceDemandeur` | Tests Jest pré-fill cas 0 / nominal |
| `extractFamilleData()` | Tout consommateur de `FamilleExtractedData` reçoit 1 champ supplémentaire (additif, nullable) | Tests d'extraction existants conservés verts |
| `decisional-tools-panel` | Badge « Pré-rempli par l'IA (N) » de F-FA-26 passe de 0 à 1 | Test Jest `getPrefillCount` |
| Autres outils Famille FR consommant `familleExtractedData` | Aucun — champ additif ignoré | Compilation TS + tests existants |

### Smoke tests E2E concernés

- [x] `e2e/smoke/case-analysis-flow.spec.ts` — flux d'analyse de dossier reste vert.
- [x] `e2e/smoke/happy-path.spec.ts` — parcours nominal inchangé.
- [x] `cd e2e && npm test` avant push (préoccupation transversale « outil décisionnel »).

---

## Dépendances

### Subfeatures bloquantes

- **SF-246-10** — couplage de fichier (record `FamilleExtractedData`, prompt `FAMILLE_INSTRUCTION`, `extractFamilleData()`, DTO `divorce-accepte.model.ts`). SF-246-11 est la **première SF Famille de la vague 4** : elle doit être développée **après** le merge de SF-246-10 (dernière SF Famille de la vague 3) et rebasée sur master à jour — voir « Notes et décisions ».

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` touchée.

---

## Notes et décisions

### Couplage de fichiers partagés — ordre de dev de la série Famille

SF-246-11 modifie les **mêmes fichiers backend partagés** que les autres SF Famille : record `FamilleExtractedData` (+ builder F-234), prompt `FAMILLE_INSTRUCTION`, méthode `extractFamilleData()`, DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`).

**Décision — ordre de dev de toute la série Famille (vagues 2 → 4)** :

```
SF-246-06 → SF-246-07 → SF-246-08 → SF-246-09 → SF-246-10 → SF-246-11 → SF-246-12 → SF-246-03
```

SF-246-11 ouvre la vague 4 côté Famille : elle ajoute son champ **après** ceux de SF-246-10, branchée après le merge de SF-246-10 et rebasée sur master à jour. Ne **jamais** développer deux SF Famille sur deux branches simultanées modifiant `FamilleExtractedData` — conflit de rebase systématique. SF-246-05 (`credit-temps-be`, Travail BE) touche `TravailExtractedData` — **indépendante** de la série Famille, parallélisable avec SF-246-11. La parallélisation backend / frontend **intra-SF** reste autorisée (contrat API figé ci-dessus).
