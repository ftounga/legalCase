# Mini-spec — [F-246 / SF-246-03] Pré-remplissage IA — Divorce pour faute (codes faute) (Famille FR)

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Découpage de référence : `docs/features/F-246/cadrage-decoupage.md` (§3.1 ligne SF-246-03, vague 4).
> **Outil mono** : `divorce-faute` (F-FA-09) — adossé au record `FamilleExtractedData` et au
> prompt `FAMILLE_INSTRUCTION`.

---

## Identifiant

`F-246 / SF-246-03`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-246-03-divorce-faute-prefill`

---

## Objectif

> En une phrase : que fait cette subfeature ?

Brancher le pré-remplissage IA de l'outil `divorce-faute` (F-FA-09, Famille FR) en faisant extraire par le pipeline IA les codes de faute détectés dans les pièces — champ aujourd'hui stubé côté frontend (`fautesDetectees`) sans source backend — afin que `prefillFromAi()` pré-coche les fautes constatées.

---

## Comportement attendu

### Cas nominal

1. L'avocat lance l'analyse IA d'un dossier de droit de la famille FR évoquant un divorce pour faute (violences, adultère, abandon du domicile, manquement grave — art. 242 Cciv).
2. Le pipeline IA (prompt `LegalDomainPromptBuilder.FAMILLE_INSTRUCTION`) extrait, dans `famille_extracted_data`, une liste `fautes_detectees` de codes de faute parmi une énumération fermée.
3. L'extracteur `extractFamilleData()` parse cette liste en champ typé `FamilleExtractedData.fautesDetectees` (`List<String>`, codes whitelistés).
4. Le DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`) expose le champ ; l'entrée `TOOL_REGISTRY` de `divorce-faute` passe déjà `aiData: ctx.synthesis?.familleExtractedData`.
5. À l'ouverture de l'outil `divorce-faute`, `prefillFromAi()` pré-coche les fautes détectées ; un badge `auto_awesome` « Pré-rempli depuis l'analyse » s'affiche à côté de chaque faute pré-cochée.
6. L'avocat peut décocher / cocher une faute : le handler `onFauteChange()` remet `provenanceFautes` à `null` et déclenche la revérification F-IA-03.
7. Le badge « Pré-rempli par l'IA (N champs) » du panel F-IA-04 reflète `getPrefillCount()`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Le LLM ne détecte aucune faute | `fautes_detectees` absent ou liste vide → `fautesDetectees` `null` ; `prefillFromAi()` no-op gracieux ; `getPrefillCount()` = 0 ; aucun badge | n/a |
| Le LLM renvoie un code hors énumération | Le code est exclu de la liste (whitelist côté extracteur) ; les codes valides conservés | n/a |
| Liste vide après filtrage | `fautesDetectees` `null` (jamais `[]` — invariant cadrage §5.1.2 transposé aux listes) | n/a |
| Dossier de famille belge | Champ FR reste `null` (le prompt impose null hors FR) ; outil non affiché pour la BE | n/a |
| `famille_extracted_data` absent du JSON IA | `extractFamilleData()` retourne `null` ; outil affiché formulaire vierge | n/a |
| `aiData` arrive après le premier rendu | `prefillFromAi()` réinvoqué dans `ngOnChanges()` | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : seul `divorce-faute` (F-FA-09) consomme les codes de faute. Le flag `divorce_faute_envisage` (F-200) pilote la **visibilité** de l'outil — finalité distincte du pré-fill : pas de doublon. Aucun autre outil Famille ne raisonne sur une typologie de fautes du divorce.
- [x] **Autres pays** : France uniquement. Le divorce pour faute (art. 242 Cciv) est un dispositif FR ; la BE raisonne par désunion irrémédiable (DDI) sur faits constitutifs — typologie distincte, outil distinct (`divorce-desunion-be`, SF-246-12). Champ `null` pour la BE.
- [x] **Autres domaines** : non applicable — concept propre au droit du divorce FR.
- [x] **Autres UI patterns** : pré-remplissage IA (pattern canonique), badges `auto_awesome`, alertes F-IA-03.
- [x] **Autres flows transversaux** : aucun.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : `FamilleExtractedData` dans `divorce-accepte.model.ts` — le champ `fautesDetectees` est aujourd'hui un **stub frontend** (`String[]`) sans source backend ; la SF le branche.
- [x] **Record / DTO backend** : `CaseAnalysisResponse.FamilleExtractedData` + builder F-234.
- [x] **Service / logique métier** : `extractFamilleData()`.
- [x] **Entité JPA + schéma DB** : non applicable — `familleExtractedData` sérialisé dans la synthèse IA. Inputs de F-FA-09 persistés par son endpoint existant (inchangé).
- [x] **Tests existants** : `divorce-faute-section-prefill-rules.spec.ts` (helper avec stub `fautesDetectees`), tests `extractFamilleData()`. Mis à jour par la SF.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : oui — les fautes cochées sont croisables avec la détection IA. `coherenceAlerts` étendu.
- [x] **Refresh dashboard (F-IA-02)** : inchangé — `triggerRefresh()` déjà câblé.
- [x] **Pré-remplissage IA** : objet de la SF.
- [x] **Persistance des inputs** : inchangée — endpoint F-FA-09 existant.
- [x] **Masquage conditionnel selon type** : inchangé — visibilité F-IA-04 déjà gérée (FR + flag F-200 `divorce_faute_envisage`).
- [x] **Alertes actives après calcul** : gate `coherenceAlerts` = `!this.showForm()` uniquement — vérifié.

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — aucun nouveau composant partagé, service ou endpoint.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `divorce-faute` (F-FA-09) | Oui | Intégré dans cette SF |
| Flag `divorce_faute_envisage` (F-200) | Non | Flag de visibilité — finalité distincte du pré-fill |
| `divorce-desunion-be` (Famille BE) | Non | Typologie DDI distincte — traité par SF-246-12 |
| Outils Travail / Immigration | Non | Concept propre au droit du divorce |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s) créée(s)
- [ ] Backlog VN
- [x] Non applicable aux autres cibles (justification explicite ci-dessus)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [ ] **Non applicable** — la SF livre une partie frontend décisionnelle (`divorce-faute-section`).

### 1. Cohérence visuelle

- [x] **Palette statut** : navy/or info, vert OK, rouge réservé aux alertes critiques — conservé.
- [x] **Datepicker** : non applicable — la SF n'ajoute pas de champ date.
- [x] **Typographie** : `JetBrains Mono` pour `baseJuridique` / `formule`, `Inter` pour le reste — conservé.
- [x] **Gate `workspaceCountry`** : outil FR-only — bannière info en cas de mismatch BE (existant).
- [x] **Erreurs** : `MatSnackBar` — pas d'`alert()` / `confirm()`.
- [x] **Refresh dashboard** : `triggerRefresh()` dans le `next:` du POST — existant, inchangé.

### 2. Pré-fill IA (OBLIGATOIRE)

- [x] `@Input() aiData?: FamilleExtractedData | null` — déjà typé strictement.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` **ET** `ngOnChanges()` — la SF rend le corps effectif.
- [x] Signal `provenanceFautes = signal<'IA' | null>(null)`.
- [x] Badge `auto_awesome` « Pré-rempli depuis l'analyse » à côté de chaque faute pré-cochée.
- [x] Handler `onFauteChange()` qui remet `provenanceFautes` à `null`.

### 3. Validation F-IA-03 (OBLIGATOIRE)

- [x] `coherenceAlerts` : alerte `FAUTES` quand les fautes cochées divergent de la détection IA (ex. l'avocat retire une faute détectée).
- [x] Hiérarchie F-96 > Question IA > IA détection > Pièce manquante ; convergence → `'MULTI'`.
- [x] `<app-coherence-popover-trigger>` câblé sur le contrôle des fautes.
- [x] Helper partagé `CoherenceAlertBuilder`.

### 4. TOOL_REGISTRY symétrique + `getPrefillCount(input)`

- [x] Entrée `F-FA-09-divorce-faute` déjà présente dans `TOOL_REGISTRY` ; `inputs(ctx)` passe déjà `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`.
- [x] Static `getPrefillCount(input)` du composant : appelle `DivorceFautePrefillRules.computePrefillCount()` enrichi (1 champ liste).
- [x] Parité stricte `getPrefillCount()` ↔ `prefillFromAi()` : mêmes guards, même condition `workspaceCountry === 'FRANCE'`.
- [x] Tests Jest : (a) 0 champ (aiData vide / BE / liste vide), (b) — pas de cas « partiel » (1 seul champ), (c) 1 champ cas nominal.
- [x] `tool_id` `F-FA-09-divorce-faute` déjà présent dans `KNOWN_FRONTEND_TOOL_IDS` du test `DecisionToolVisibilityIntegrityIT` — pas de migration.

### 5. Parité des domaines métier (niveau ≥ 5)

- [x] Niveau du tool : **5** (analyse de la matérialité des fautes — verdict d'opportunité de la voie « faute »).

| Domaine | Équivalent existe ? | Si non → action |
|---------|---------------------|-----------------|
| Droit du travail | Non | Le divorce pour faute n'a pas de transposition en droit du travail — concept non pertinent |
| Immigration | Non | Concept non pertinent en droit des étrangers |
| Famille | Oui (F-FA-09) | C'est l'outil de cette SF |

> La SF complète le pré-fill d'un outil existant — la parité de domaine de F-FA-09 a été tranchée à sa création.

---

## Champs IA à extraire (pré-remplissage)

- [ ] **Aucun pré-remplissage** — non, la SF crée le pré-remplissage.

| Champ du formulaire | Type | Champ source du record `FamilleExtractedData` | Extension requise |
|---------------------|------|------------------------------------------------|-------------------|
| fautes constatées (cases à cocher) | liste de codes (énumération fermée) | `fautesDetectees` (`List<String>`, nullable) | [x] record + [x] prompt `LegalDomainPromptBuilder` (`FAMILLE_INSTRUCTION`) + [x] extracteur (`extractFamilleData()`) + [x] DTO frontend (remplace le stub) |

- [x] Pour le champ liste à pré-remplir non encore présent dans la chaîne backend (`fautesDetectees`), l'extension du record `FamilleExtractedData` **et** du prompt `LegalDomainPromptBuilder` (`FAMILLE_INSTRUCTION`) est explicitement dans le périmètre de cette SF.

> **Note couplage record partagé** : voir « Notes et décisions » — SF-246-03 séquentielle dans la série Famille (vague 4).

> **Note de design IA** : `fautesDetectees` est une **liste de codes whitelistés** (`List<String>`) — l'énumération exacte est alignée sur les cases à cocher du formulaire F-FA-09 : `VIOLENCES`, `ADULTERE`, `ABANDON_DOMICILE`, `MANQUEMENT_DEVOIR_ASSISTANCE`, `INJURES_GRAVES`, `MANQUEMENT_DEVOIR_FIDELITE`, `MANQUEMENT_DEVOIR_RESPECT` (liste fermée — figer dans la mini-spec et le prompt). Un code hors whitelist est exclu côté extracteur (fail-open). Liste vide → `null` (jamais `[]`). Le prompt impose « ne lister une faute que si un indice factuel concret la documente — main courante, plainte, attestation, constat — sinon ne pas l'inclure ». L'intensité / la gravité de chaque faute reste en saisie manuelle (non factualisable de façon fiable — documenté, cadrage §5.6).

---

## Critères d'acceptation

- [ ] Le record `FamilleExtractedData` contient le nouveau champ `fautesDetectees` (`List<String>`, nullable), propagé par le builder F-234.
- [ ] Le prompt `FAMILLE_INSTRUCTION` décrit la clé `fautes_detectees` avec l'énumération fermée des 7 codes + l'instruction « indice factuel concret obligatoire » + `null` hors FR / hors certitude.
- [ ] `extractFamilleData()` parse `fautes_detectees` : chaque code validé contre la whitelist, codes invalides exclus, liste vide → `null`.
- [ ] Le DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`) expose `fautesDetectees?: string[] | null` — le champ cesse d'être un stub.
- [ ] `DivorceFautePrefillRules` lit `aiData.fautesDetectees` réel ; `computePrefillCount()` retourne 1 si la liste est non vide et FR, 0 sinon.
- [ ] `prefillFromAi()` du composant pré-coche les fautes détectées quand `workspaceCountry === 'FRANCE'`, et reste no-op si BE.
- [ ] Chaque faute pré-cochée affiche un badge `auto_awesome` ; la modification manuelle remet `provenanceFautes` à `null` et masque le badge.
- [ ] `getPrefillCount()` statique et `prefillFromAi()` runtime en parité stricte (test Jest cas 0 / nominal).
- [ ] Une fixture IA évoquant des violences et un abandon du domicile → `fautesDetectees` = `["VIOLENCES", "ABANDON_DOMICILE"]` (test backend).
- [ ] Une fixture IA renvoyant un code inconnu → ce code est exclu, les autres conservés.
- [ ] `coherenceAlerts` lève une alerte F-IA-03 si les fautes cochées divergent de la détection IA.
- [ ] Isolation workspace : non applicable côté pré-fill — voir section dédiée.

---

## Périmètre

### Hors scope (explicite)

- Toute modification du scoring, des verdicts ou des bases juridiques de F-FA-09 (logique métier inchangée).
- Le pré-remplissage de l'intensité / la gravité de chaque faute — non factualisable de façon fiable par le LLM en V1 ; reste en saisie manuelle (documenté).
- L'outil `divorce-desunion-be` — traité par SF-246-12.
- Toute migration Liquibase — aucun nouvel outil, aucune table.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `fautesDetectees` | `null` | `List<String>` de codes whitelistés non vide, ou `null` |

Comportements à la création : aucun — la SF étend un record de réponse IA.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `fautesDetectees` | Non | — | liste de codes ∈ {`VIOLENCES`, `ADULTERE`, `ABANDON_DOMICILE`, `MANQUEMENT_DEVOIR_ASSISTANCE`, `INJURES_GRAVES`, `MANQUEMENT_DEVOIR_FIDELITE`, `MANQUEMENT_DEVOIR_RESPECT`} ; codes hors whitelist exclus ; liste vide → `null` | Codes dédupliqués | whitelist + déduplication |

Notes :
- Champ nullable — invariant cadrage §5.1.2 transposé aux listes (liste vide → `null`).

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/divorce-faute` | Oui | LAWYER |
| GET | `/api/v1/case-files/{caseFileId}/divorce-faute` | Oui | MEMBER |

> Endpoints **inchangés** (existants F-FA-09). La SF n'ajoute aucun endpoint.

### Contrat API figé (parallélisation back / front)

**Champ JSON produit par le pipeline IA** (sous `analysis_result.famille_extracted_data`) :

```json
"fautes_detectees": ["VIOLENCES", "ABANDON_DOMICILE"]
```

**Record backend `FamilleExtractedData`** — 1 champ ajouté (en fin de record, à la position de la série Famille vague 4) :

```java
// SF-246-03 : codes de faute détectés pour pré-fill F-FA-09 divorce pour faute
// (Famille FR uniquement, nullable).
java.util.List<String> fautesDetectees
```

**DTO frontend `FamilleExtractedData`** (`divorce-accepte.model.ts`) — champ branché (remplace le stub) :

```ts
/** SF-246-03 : codes de faute détectés pour pré-fill F-FA-09 (FR uniquement). */
fautesDetectees?: string[] | null;
```

**Helper `DivorceFautePrefillInput`** — contrat figé :

```ts
export interface DivorceFautePrefillInput {
  aiData?: Pick<FamilleExtractedData, 'fautesDetectees'> | null;
  workspaceCountry?: string;
}
```

`computePrefillCount(input)` retourne 1 si `workspaceCountry === 'FRANCE'` et `fautesDetectees` non vide, 0 sinon.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| — | — | Aucune table impactée. `familleExtractedData` sérialisé dans le JSON de synthèse |

### Migration Liquibase

- [ ] Oui
- [x] Non applicable — aucun nouvel outil, aucune table, aucune règle `decision_tool_visibility_rules`.

### Composants Angular (si applicable)

- `DivorceFauteSectionComponent` — `prefillFromAi()` rendu effectif, signal `provenanceFautes`, handler `onFauteChange()`, badges `auto_awesome`, extension `coherenceAlerts`.
- `divorce-faute-section-prefill-rules.ts` — lecture de `aiData.fautesDetectees` réel (cesse d'être un stub), `computePrefillCount()` recalculé.

---

## Plan de test

### Tests unitaires

- [ ] `extractFamilleData()` — cas nominal : `fautes_detectees` valide → champ renseigné.
- [ ] `extractFamilleData()` — clé absente ou liste vide → `fautesDetectees` `null`, pas d'exception.
- [ ] `extractFamilleData()` — code hors whitelist → exclu, autres conservés.
- [ ] `extractFamilleData()` — codes en double → dédupliqués.
- [ ] `LegalDomainPromptBuilderTest` — `FAMILLE_INSTRUCTION` contient la clé `fautes_detectees` + l'énumération des 7 codes.
- [ ] `computePrefillCount()` — cas (a) `aiData` vide / liste vide → 0 ; cas (c) liste non vide + FR → 1.
- [ ] `computePrefillCount()` — `workspaceCountry = 'BELGIQUE'` → 0.
- [ ] `prefillFromAi()` — liste valide → fautes pré-cochées, badges présents, `provenanceFautes = 'IA'`.
- [ ] `prefillFromAi()` — parité stricte avec `getPrefillCount()`.
- [ ] `onFauteChange()` — modification manuelle remet `provenanceFautes` à `null`.
- [ ] `coherenceAlerts` — alerte levée si les fautes cochées divergent de la détection IA.

### Tests d'intégration

- [ ] Analyse IA d'un dossier famille FR fixture évoquant un divorce pour faute → la synthèse expose `fautes_detectees` peuplé.
- [ ] **Fixture multi-fautes** : dossier documentant violences + abandon du domicile → `fautesDetectees` = les deux codes.
- [ ] Dossier famille BE → `fautesDetectees` reste `null`.
- [ ] `GET /api/v1/case-files/{caseFileId}/divorce-faute` → 403 si workspace différent (non-régression endpoint existant).

### Isolation workspace

- [x] Applicable — vérifiée au niveau de l'endpoint F-FA-09 existant (test de non-régression conservé). Le champ IA n'introduit aucun nouvel accès.

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
| `DivorceFauteSectionComponent` | `prefillFromAi()` devient effectif — risque de pré-cocher une faute à tort | Tests Jest pré-fill cas 0 / nominal |
| `extractFamilleData()` | Tout consommateur de `FamilleExtractedData` reçoit 1 champ supplémentaire (additif, nullable) | Tests d'extraction existants conservés verts |
| `decisional-tools-panel` | Badge « Pré-rempli par l'IA (N) » de F-FA-09 passe de 0 à 1 | Test Jest `getPrefillCount` |
| Autres outils Famille FR consommant `familleExtractedData` | Aucun — champ additif ignoré | Compilation TS + tests existants |

### Smoke tests E2E concernés

- [x] `e2e/smoke/case-analysis-flow.spec.ts` — flux d'analyse de dossier reste vert.
- [x] `e2e/smoke/happy-path.spec.ts` — parcours nominal inchangé.
- [x] `cd e2e && npm test` avant push (préoccupation transversale « outil décisionnel »).

---

## Dépendances

### Subfeatures bloquantes

- **SF-246-12** (ou la SF Famille immédiatement précédente dans l'ordre interne vague 4) — couplage de fichier (record `FamilleExtractedData`, prompt `FAMILLE_INSTRUCTION`, `extractFamilleData()`, DTO `divorce-accepte.model.ts`). Voir « Notes et décisions » pour l'ordre interne de la vague 4.

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` touchée.

---

## Notes et décisions

### Couplage de fichiers partagés — ordre de dev de la série Famille

SF-246-03 modifie les **mêmes fichiers backend partagés** que les autres SF Famille : record `FamilleExtractedData` (+ builder F-234), prompt `FAMILLE_INSTRUCTION`, méthode `extractFamilleData()`, DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`).

**Décision — ordre de dev de toute la série Famille (vagues 2 → 4)** :

```
SF-246-06 → SF-246-07 → SF-246-08 → SF-246-09 → SF-246-10 → SF-246-11 → SF-246-12 → SF-246-03
```

SF-246-03 et SF-246-11 sont des SF Famille mono-outil de la vague 4. **Ordre interne de la vague 4** : SF-246-11 (changement d'état civil, Famille FR) puis SF-246-12 (divorce désunion BE, Famille BE) puis SF-246-03 (divorce-faute, Famille FR) — chaque SF Famille rebase sur master à jour après le merge de la précédente. SF-246-05 (`credit-temps-be`, Travail BE) touche `TravailExtractedData` — **indépendante** de la série Famille, parallélisable.

Ne **jamais** développer deux SF Famille sur deux branches simultanées modifiant `FamilleExtractedData` — conflit de rebase systématique. La parallélisation backend / frontend **intra-SF** reste autorisée (contrat API figé ci-dessus).

### Décision énumération fermée des codes de faute

L'énumération est **fermée** et alignée 1:1 sur les cases à cocher du formulaire F-FA-09 existant. Tout code émis par le LLM hors whitelist est silencieusement exclu (fail-open) — l'avocat reste libre de cocher manuellement une faute non détectée. Le prompt exige un indice factuel concret par faute (main courante, plainte, attestation) pour éviter le pré-cochage hasardeux d'un divorce conflictuel.
