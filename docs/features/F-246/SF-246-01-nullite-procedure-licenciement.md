# Mini-spec — [F-246 / SF-246-01] Pré-remplissage IA — Nullité de procédure de licenciement (FR)

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Découpage de référence : `docs/features/F-246/cadrage-decoupage.md` (§3.1 ligne SF-246-01, vague 1).

---

## Identifiant

`F-246 / SF-246-01`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`draft`

## Date de création

2026-05-18

## Branche Git

`feat/SF-246-01-nullite-procedure-prefill`

---

## Objectif

> En une phrase : que fait cette subfeature ?

Brancher le pré-remplissage IA de l'outil `procedure-nullite-licenciement` (F-DT-36, Travail FR) en faisant extraire par le pipeline IA les flags procéduraux du licenciement (délai de convocation, entretien préalable, motivation de la lettre) aujourd'hui absents de la chaîne backend, et en remplaçant le `PREFILL_COUNT_ALWAYS_ZERO = true` par un pré-fill réel.

---

## Comportement attendu

### Cas nominal

1. L'avocat lance l'analyse IA d'un dossier de droit du travail FR contenant les pièces du licenciement (convocation à entretien préalable, lettre de licenciement, éventuelle convention collective).
2. Le pipeline IA (prompt `LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION`) extrait, dans le bloc `travail_extracted_data`, un sous-objet `procedure_licenciement_detection` regroupant les indices procéduraux factuels.
3. L'extracteur `extractTravailData()` parse ce sous-objet en champs typés du record `TravailExtractedData`.
4. Le DTO frontend `TravailExtractedData` (`case-analysis.model.ts`) expose les champs ; le `TOOL_REGISTRY` les passe déjà via `aiData: ctx.synthesis?.travailExtractedData`.
5. À l'ouverture de l'outil `procedure-nullite-licenciement`, `prefillFromAi()` renseigne les champs détectables du formulaire ; un badge `auto_awesome` « Pré-rempli depuis l'analyse » s'affiche à côté de chaque champ pré-rempli.
6. L'avocat peut modifier toute valeur : le handler `onXxxChange()` correspondant remet `provenance<Field>` à `null` (le badge disparaît) et déclenche la revérification F-IA-03.
7. Le badge « Pré-rempli par l'IA (N champs) » du panel F-IA-04 reflète `getPrefillCount()`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Le LLM ne détecte aucun indice procédural fiable | Tous les champs `procedure_licenciement_detection` à `null` / `INCONNU` ; `prefillFromAi()` no-op gracieux ; `getPrefillCount()` = 0 ; aucun badge | n/a (pipeline async) |
| Le LLM renvoie un code énumération hors whitelist | `normalizeEnumCode()` rejette → champ à `null` (fail-open) ; pas de pré-fill du champ concerné | n/a |
| Le LLM renvoie une `reponse` hors {OUI, NON, INCONNU} | `extractDetectedAnswer()` normalise ; valeur non reconnue → `null` | n/a |
| Dossier de droit du travail belge | Tous les champs FR procéduraux restent `null` (le prompt impose null hors FR) ; outil non affiché pour la BE | n/a |
| `travail_extracted_data` absent du JSON IA | `extractTravailData()` retourne `null` ; outil affiché formulaire vierge | n/a |
| `aiData` arrive après le premier rendu du composant | `prefillFromAi()` réinvoqué dans `ngOnChanges()` | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : seul `procedure-nullite-licenciement` (F-DT-36) consomme les flags procéduraux du licenciement. F-DT-08 (validité licenciement) consomme `licenciement_validity_detection` — bloc IA **distinct** (scoring de validité au fond, pas vices de forme procéduraux) : pas de doublon, pas de mutualisation des champs.
- [x] **Autres pays** : France uniquement. La nullité de procédure FR (entretien préalable, délai de 5 jours ouvrables, lettre motivée) n'a pas d'équivalent direct côté BE — la régularité de licenciement BE est couverte par d'autres outils (F-DT-27 motif grave BE). Les champs restent `null` pour la BE.
- [x] **Autres domaines** : non applicable — concept propre au droit du travail FR.
- [x] **Autres UI patterns** : pré-remplissage IA (pattern canonique `immigration-title-decision-section`), badges de provenance `auto_awesome`, alertes F-IA-03.
- [x] **Autres flows transversaux** : aucun (pas d'auth, pas de workspace context modifié, pas de plan).

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : `TravailExtractedData` dans `case-analysis.model.ts` — extension de l'interface.
- [x] **Record / DTO backend** : `CaseAnalysisResponse.TravailExtractedData` — extension du record + builder.
- [x] **Service / logique métier** : `extractTravailData()` — extension du parsing JSON.
- [x] **Entité JPA + schéma DB** : non applicable — `travailExtractedData` est sérialisé dans la synthèse IA (`analysis_result` JSON), pas de colonne dédiée. Les inputs de l'outil F-DT-36 restent persistés par l'endpoint `procedure-nullite-licenciement` existant (inchangé).
- [x] **Tests existants** : `procedure-nullite-licenciement-section-prefill-rules.spec.ts` (assertions `PREFILL_COUNT_ALWAYS_ZERO`), tests d'extraction `extractTravailData()`. Les deux sont mis à jour par cette SF.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : oui — 3 champs croisables (`DATE_ENTRETIEN_PREALABLE`, `MOTIVATION_SUFFISANTE`, `ENTRETIEN_TENU`) déjà câblés F-IA-03 sur l'outil. La SF étend `coherenceAlerts` pour intégrer les nouvelles sources IA, sans nouvelle SF jumelle.
- [x] **Refresh dashboard (F-IA-02)** : non modifié — l'outil déclenche déjà `triggerRefresh()` dans le `next:` du POST de validation ; pas de changement.
- [x] **Pré-remplissage IA** : c'est l'objet de la SF — `prefillFromAi()` réel remplace le no-op.
- [x] **Persistance des inputs** : inchangée — les inputs validés de F-DT-36 sont déjà persistés via l'endpoint existant ; le pré-fill ne pré-remplit que le formulaire avant soumission.
- [x] **Masquage conditionnel selon type** : inchangé — la visibilité F-IA-04 de F-DT-36 est déjà gérée (FR + type litige licenciement).
- [x] **Alertes actives après calcul** : le gate `coherenceAlerts` ne doit gater que `!this.showForm()` — vérifié, pas de `|| this.result()`.

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — aucun nouveau composant partagé, service ou endpoint. La SF réutilise le pattern de pré-fill canonique existant et étend un record / un prompt existants.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `procedure-nullite-licenciement` (F-DT-36) | Oui | Intégré dans cette SF |
| F-DT-08 validité licenciement | Non | Bloc IA `licenciement_validity_detection` distinct (validité au fond) — pas de doublon |
| Outils Travail BE | Non | Pas d'équivalent procédural FR ; champs `null` en BE — justifié |
| Outils Famille / Immigration | Non | Concept propre au droit du travail FR |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s) créée(s)
- [ ] Backlog VN
- [x] Non applicable aux autres cibles (justification explicite ci-dessus)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [ ] **Non applicable** — la SF livre une partie frontend décisionnelle (`procedure-nullite-licenciement-section`).

### 1. Cohérence visuelle

- [x] **Palette statut** : navy/or pour info, vert OK, rouge réservé aux alertes critiques — conservé tel quel.
- [x] **Datepicker** : `<input type="date">` natif pour `dateConvocationPresentee`, `dateEntretienPrealable`, `dateNotificationLicenciement` — pas de `MatDatepicker`.
- [x] **Typographie** : `JetBrains Mono` pour `basesJuridiques` / `formule`, `Inter` pour le reste — conservé.
- [x] **Gate `workspaceCountry`** : outil FR-only — bannière info en cas de mismatch BE (existant, inchangé).
- [x] **Erreurs** : `MatSnackBar` — pas d'`alert()` / `confirm()`.
- [x] **Refresh dashboard** : `CaseDashboardRefreshService.triggerRefresh()` dans le `next:` du POST — existant, inchangé.

### 2. Pré-fill IA (OBLIGATOIRE)

- [x] `@Input() aiData?: TravailExtractedData | null` — déjà typé strictement.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` **ET** `ngOnChanges()` — la SF rend la méthode effective (aujourd'hui no-op via `PREFILL_COUNT_ALWAYS_ZERO`).
- [x] Un signal `provenance<Field>` par champ pré-rempli : `provenanceDateConvocation`, `provenanceDateEntretien`, `provenanceEntretienTenu`, `provenanceDateNotification`, `provenanceLettreMotivee`, `provenanceMotivationSuffisante`.
- [x] Badge `auto_awesome` « Pré-rempli depuis l'analyse » à côté de chaque champ pré-rempli.
- [x] Handler `onXxxChange()` par champ qui remet `provenance<Field>` à `null`.

### 3. Validation F-IA-03 (OBLIGATOIRE)

- [x] `coherenceAlerts` étendu pour intégrer les nouvelles sources IA pré-remplies (`DATE_ENTRETIEN_PREALABLE`, `MOTIVATION_SUFFISANTE`, `ENTRETIEN_TENU`).
- [x] Hiérarchie F-96 > Question IA > IA détection > Pièce manquante respectée ; convergence → source `'MULTI'`.
- [x] `<app-coherence-popover-trigger>` câblé sur les champs concernés.
- [x] Helper partagé `CoherenceAlertBuilder` (`frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts`).

### 4. TOOL_REGISTRY symétrique + `getPrefillCount(input)`

- [x] Entrée `F-DT-36-procedure-nullite-licenciement` déjà présente dans `TOOL_REGISTRY` ; `inputs(ctx)` passe déjà `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes` — aucune modification du binding requise.
- [x] Static `getPrefillCount(input)` du composant : refactorisé pour appeler `ProcedureNulliteLicenciementSectionPrefillRules.computePrefillCount()` qui ne retourne plus systématiquement 0.
- [x] Parité stricte `getPrefillCount()` ↔ `prefillFromAi()` : mêmes guards `typeof === 'string'`, mêmes mappings, même condition `workspaceCountry === 'FRANCE'`.
- [x] Tests Jest : (a) 0 champ (aiData vide / BE), (b) M champs partiels, (c) N champs cas nominal.
- [x] `tool_id` `F-DT-36-procedure-nullite-licenciement` déjà présent dans `KNOWN_FRONTEND_TOOL_IDS` du test `DecisionToolVisibilityIntegrityIT` — pas de migration `decision_tool_visibility_rules` (aucun nouvel outil).

### 5. Parité des domaines métier (niveau ≥ 5)

- [x] Niveau du tool : **5** (scoring / analyse de validité — verdict de nullité 0-100).

| Domaine | Équivalent existe ? | Si non → action |
|---------|---------------------|-----------------|
| Droit du travail | Oui (F-DT-36) | C'est l'outil de cette SF |
| Immigration | Non | La nullité de procédure de licenciement n'a pas de transposition en droit des étrangers — concept non pertinent |
| Famille | Non | Concept non pertinent en droit de la famille |

> La SF ne crée pas un nouvel outil de niveau ≥ 5 — elle complète le pré-fill d'un outil existant. La parité de domaine de F-DT-36 a déjà été tranchée à sa création.

---

## Champs IA à extraire (pré-remplissage)

- [ ] **Aucun pré-remplissage** — non, la SF crée précisément le pré-remplissage.

> L'outil F-DT-36 a un formulaire mixte booléens + dates. Champs pré-remplissables retenus (les autres champs — `licenciementCollectif`, `conventionCollectiveApplicable`, `procedureCseRespectee` — restent en saisie manuelle car non factualisables de façon fiable par le LLM en V1).

| Champ du formulaire | Type | Champ source du record `TravailExtractedData` | Extension requise |
|---------------------|------|------------------------------------------------|-------------------|
| `convocationEnvoyee` | booléen | `TravailExtractedData.convocationEntretienDetectee` (`Boolean`, nullable) | [x] record + [x] prompt `LegalDomainPromptBuilder` + [x] extracteur + [x] DTO frontend |
| `dateConvocationPresentee` | date (ISO YYYY-MM-DD) | `TravailExtractedData.dateConvocationEntretienDetectee` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `dateEntretienPrealable` | date (ISO YYYY-MM-DD) | `TravailExtractedData.dateEntretienPrealableDetectee` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `entretienTenu` | booléen | `TravailExtractedData.entretienPrealableTenuDetected` (`DetectedAnswer`, nullable — OUI/NON/INCONNU) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `dateNotificationLicenciement` | date (ISO YYYY-MM-DD) | `TravailExtractedData.dateLicenciement` (`String`, **déjà présent**) | « déjà présent » — mappé directement dans `prefillFromAi()` |
| `lettreLicenciementEcrite` | booléen | `TravailExtractedData.lettreLicenciementEcriteDetectee` (`Boolean`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `lettreMotivee` | booléen | `TravailExtractedData.lettreLicenciementMotiveeDetected` (`DetectedAnswer`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `motivationSuffisante` | booléen | `TravailExtractedData.motivationLettreSuffisanteDetected` (`DetectedAnswer`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

- [x] Pour chaque champ date / valeur / booléen pré-rempli non encore présent, l'extension du record `TravailExtractedData` **et** du prompt `LegalDomainPromptBuilder` est explicitement dans le périmètre de cette SF.

> **Note couplage record partagé** : voir section « Notes et décisions » — ordre de dev imposé sur `TravailExtractedData` / `LegalDomainPromptBuilder` / `extractTravailData()`.

> **Note de design IA** : les booléens factualisables sont scindés en deux familles. `convocationEnvoyee`, `lettreLicenciementEcrite` = faits binaires nettement observables → `Boolean` nullable simple. `entretienTenu`, `lettreMotivee`, `motivationSuffisante` = appréciations nécessitant une justification citée → `DetectedAnswer` (objet `{reponse, justification}`), cohérent avec `reclassementRespecteDetected` (SF-155-04). Le mapping `prefillFromAi()` traduit `DetectedAnswer.reponse` : `OUI`→true, `NON`→false, `INCONNU`/`null`→non pré-rempli.

---

## Critères d'acceptation

- [ ] Le record `TravailExtractedData` contient les 6 nouveaux champs (`convocationEntretienDetectee`, `dateConvocationEntretienDetectee`, `dateEntretienPrealableDetectee`, `entretienPrealableTenuDetected`, `lettreLicenciementEcriteDetectee`, `lettreLicenciementMotiveeDetected`, `motivationLettreSuffisanteDetected`), tous nullables, propagés par le builder F-234.
- [ ] Le prompt `TRAVAIL_INSTRUCTION` décrit un sous-objet `procedure_licenciement_detection` avec une définition juridique sans ambiguïté pour chaque champ + l'instruction `null` hors FR / hors certitude.
- [ ] `extractTravailData()` parse `procedure_licenciement_detection` : booléens via `booleanOrNull()`, dates via `textOrNull()`, `DetectedAnswer` via `extractDetectedAnswer()`.
- [ ] Le DTO frontend `TravailExtractedData` (`case-analysis.model.ts`) expose les 6 champs avec les bons types TS.
- [ ] `ProcedureNulliteLicenciementSectionPrefillRules.computePrefillCount()` retourne le nombre exact de champs pré-remplissables ; la constante `PREFILL_COUNT_ALWAYS_ZERO` est supprimée.
- [ ] `prefillFromAi()` du composant renseigne les 8 champs du tableau ci-dessus (incl. `dateNotificationLicenciement` via `dateLicenciement`) quand `workspaceCountry === 'FRANCE'`, et reste no-op si BE.
- [ ] Chaque champ pré-rempli affiche un badge `auto_awesome` ; la modification manuelle remet `provenance<Field>` à `null` et masque le badge.
- [ ] `getPrefillCount()` statique et `prefillFromAi()` runtime sont en parité stricte (vérifié par test Jest cas 0 / partiel / nominal).
- [ ] Une fixture IA contenant deux dates concurrentes (date de convocation ≠ date d'entretien préalable) remplit le bon champ et laisse l'autre correct (test backend).
- [ ] `coherenceAlerts` lève une alerte F-IA-03 si une date pré-remplie est incohérente (ex. `dateEntretienPrealable` antérieure à `dateConvocationPresentee`).
- [ ] Isolation workspace : non applicable côté pré-fill (donnée portée par la synthèse du dossier, déjà isolée par `caseFileId`) — voir section dédiée.

---

## Périmètre

### Hors scope (explicite)

- Toute modification du scoring de nullité, des verdicts ou des bases juridiques de F-DT-36 (logique métier inchangée).
- Le pré-remplissage des champs `licenciementCollectif`, `conventionCollectiveApplicable`, `conventionCollectiveRespectee`, `procedureCseRespectee` — non factualisables de façon fiable par le LLM en V1 ; restent en saisie manuelle (documenté, pas une dette masquée — cf. §5.6 cadrage).
- Tout outil Travail BE — hors périmètre vague 1 FR.
- Toute migration Liquibase — aucun nouvel outil, aucune table.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `convocationEntretienDetectee` | `null` | Renseigné par l'IA seulement si indice factuel ; absent sinon |
| `dateConvocationEntretienDetectee` | `null` | Format ISO `YYYY-MM-DD` strict ou `null` |
| `dateEntretienPrealableDetectee` | `null` | Format ISO `YYYY-MM-DD` strict ou `null` |
| `entretienPrealableTenuDetected` | `null` | `DetectedAnswer` (OUI/NON/INCONNU) ou `null` |
| `lettreLicenciementEcriteDetectee` | `null` | Booléen ou `null` |
| `lettreLicenciementMotiveeDetected` | `null` | `DetectedAnswer` ou `null` |
| `motivationLettreSuffisanteDetected` | `null` | `DetectedAnswer` ou `null` |

Comportements à la création : aucun — la SF n'crée pas d'entité, elle étend un record de réponse IA.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `dateConvocationEntretienDetectee` | Non | — | ISO `YYYY-MM-DD` ; rejet du parsing sinon → `null` | Non | `textOrNull()` |
| `dateEntretienPrealableDetectee` | Non | — | ISO `YYYY-MM-DD` ; rejet sinon → `null` | Non | `textOrNull()` |
| `entretienPrealableTenuDetected.reponse` | Non | — | `OUI` / `NON` / `INCONNU` | Non | `normalizeReponse()` |
| `lettreLicenciementMotiveeDetected.reponse` | Non | — | `OUI` / `NON` / `INCONNU` | Non | `normalizeReponse()` |
| `motivationLettreSuffisanteDetected.reponse` | Non | — | `OUI` / `NON` / `INCONNU` | Non | `normalizeReponse()` |
| `*.justification` | Non | 500 | texte libre tronqué à 500 car. | Non | troncature |

Notes :
- Tous les champs sont nullables — invariant cadrage §5.1.2 : un champ non identifié de façon fiable reste `null`.
- Format ISO strict cohérent avec `dateAcceptationPV` (F-239) et `avisMedecinTravailDate` (SF-155-04).

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/procedure-nullite-licenciement` | Oui | LAWYER |
| GET | `/api/v1/case-files/{caseFileId}/procedure-nullite-licenciement` | Oui | MEMBER |

> Endpoints **inchangés** (existants SF-DT-36-01). La SF n'ajoute aucun endpoint : les champs IA transitent par la synthèse d'analyse (`travailExtractedData`) déjà servie via le GET de synthèse du dossier.

### Contrat API figé (parallélisation back / front)

**Bloc JSON produit par le pipeline IA** (sous `analysis_result.travail_extracted_data`) :

```json
"procedure_licenciement_detection": {
  "convocation_entretien_detectee": true,
  "date_convocation_entretien": "2026-02-10",
  "date_entretien_prealable": "2026-02-18",
  "entretien_prealable_tenu": { "reponse": "OUI", "justification": "PV d'entretien daté du 18/02 produit aux pièces" },
  "lettre_licenciement_ecrite": true,
  "lettre_licenciement_motivee": { "reponse": "NON", "justification": "Lettre du 25/02 mentionne uniquement 'motif personnel'" },
  "motivation_lettre_suffisante": { "reponse": "NON", "justification": "Aucun fait précis daté n'est articulé" }
}
```

**Record backend `TravailExtractedData`** — 6 champs ajoutés (ordre d'ajout en fin de record, après les flags F-205, avant `)`) :

```java
// SF-246-01 : 6 champs IA procéduraux pour pré-fill F-DT-36 (Travail FR uniquement, nullables).
Boolean convocationEntretienDetectee,
String dateConvocationEntretienDetectee,
String dateEntretienPrealableDetectee,
DetectedAnswer entretienPrealableTenuDetected,
Boolean lettreLicenciementEcriteDetectee,
DetectedAnswer lettreLicenciementMotiveeDetected,
DetectedAnswer motivationLettreSuffisanteDetected
```

**DTO frontend `TravailExtractedData`** (`case-analysis.model.ts`) — 6 champs ajoutés :

```ts
/** SF-246-01 : flags procéduraux du licenciement pour pré-fill F-DT-36 (FR uniquement). */
convocationEntretienDetectee?: boolean | null;
dateConvocationEntretienDetectee?: string | null;
dateEntretienPrealableDetectee?: string | null;
entretienPrealableTenuDetected?: DetectedAnswer | null;
lettreLicenciementEcriteDetectee?: boolean | null;
lettreLicenciementMotiveeDetected?: DetectedAnswer | null;
motivationLettreSuffisanteDetected?: DetectedAnswer | null;
```

**Helper `ProcedureNulliteLicenciementPrefillInput`** — contrat figé :

```ts
export interface ProcedureNulliteLicenciementPrefillInput {
  aiData?: Pick<TravailExtractedData,
    | 'convocationEntretienDetectee' | 'dateConvocationEntretienDetectee'
    | 'dateEntretienPrealableDetectee' | 'entretienPrealableTenuDetected'
    | 'dateLicenciement' | 'lettreLicenciementEcriteDetectee'
    | 'lettreLicenciementMotiveeDetected' | 'motivationLettreSuffisanteDetected'> | null;
  workspaceCountry?: string;
}
```

`computePrefillCount(input)` retourne le nombre de champs non-`null` / non-`INCONNU` (et 0 si `workspaceCountry !== 'FRANCE'`).

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| — | — | Aucune table impactée. `travailExtractedData` est sérialisé dans le JSON de synthèse d'analyse |

### Migration Liquibase

- [ ] Oui
- [x] Non applicable — aucun nouvel outil, aucune table, aucune règle `decision_tool_visibility_rules`.

### Composants Angular (si applicable)

- `ProcedureNulliteLicenciementSectionComponent` — `prefillFromAi()` rendu effectif, ajout des signaux `provenance<Field>`, handlers `onXxxChange()`, badges `auto_awesome`, extension `coherenceAlerts`.
- `procedure-nullite-licenciement-section-prefill-rules.ts` — suppression de `PREFILL_COUNT_ALWAYS_ZERO`, implémentation réelle de `computePrefillCount()` + fonctions de calcul par champ.

---

## Plan de test

### Tests unitaires

- [ ] `extractTravailData()` — cas nominal : `procedure_licenciement_detection` complet → 6 champs renseignés.
- [ ] `extractTravailData()` — sous-objet absent → 6 champs `null`, pas d'exception.
- [ ] `extractTravailData()` — `entretien_prealable_tenu.reponse` hors énumération → `DetectedAnswer` avec `reponse` `null`.
- [ ] `extractTravailData()` — date non ISO → champ `null` (fail-open).
- [ ] `LegalDomainPromptBuilderTest` — `TRAVAIL_INSTRUCTION` contient les 7 clés `procedure_licenciement_detection`.
- [ ] `computePrefillCount()` — cas (a) `aiData` vide → 0 ; cas (b) 3 champs renseignés → 3 ; cas (c) 8 champs → 8.
- [ ] `computePrefillCount()` — `workspaceCountry = 'BELGIQUE'` → 0.
- [ ] `prefillFromAi()` — 8 champs renseignés, badges présents.
- [ ] `prefillFromAi()` — parité stricte avec `getPrefillCount()` (même cardinalité sur les mêmes inputs).
- [ ] `onXxxChange()` — modification manuelle remet `provenance<Field>` à `null`.
- [ ] `coherenceAlerts` — alerte levée si `dateEntretienPrealable` < `dateConvocationPresentee`.

### Tests d'intégration

- [ ] Analyse IA d'un dossier travail FR fixture avec pièces de licenciement → la synthèse expose `procedure_licenciement_detection` peuplé.
- [ ] **Fixture multi-dates** (invariant cadrage §5.1.6) : dossier contenant date de convocation `2026-02-10` ET date d'entretien `2026-02-18` ET date de notification `2026-02-25` → `dateConvocationEntretienDetectee` = 10/02, `dateEntretienPrealableDetectee` = 18/02, `dateLicenciement` = 25/02, aucune confusion.
- [ ] Dossier travail BE → les 6 champs FR restent `null`.
- [ ] `GET /api/v1/case-files/{caseFileId}/procedure-nullite-licenciement` → 403 si workspace différent (non-régression endpoint existant).

### Isolation workspace

- [x] Applicable — vérifié au niveau de l'endpoint F-DT-36 existant (un utilisateur du workspace A ne peut pas lire l'outil d'un dossier du workspace B) : test de non-régression conservé. Les champs IA n'introduisent aucun nouvel accès — ils transitent par la synthèse du dossier, déjà isolée par `caseFileId` + workspace.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale structurelle** — mais la SF coche le déclencheur **« Outil décisionnel métier »** (modification d'un outil décisionnel existant). Composants impactés listés ci-dessous.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `ProcedureNulliteLicenciementSectionComponent` | `prefillFromAi()` devient effectif — risque de pré-remplir un champ à tort | Tests Jest pré-fill cas 0 / partiel / nominal |
| `extractTravailData()` | Tout consommateur de `TravailExtractedData` reçoit 6 champs supplémentaires (additif, nullable — non cassant via builder F-234) | Tests d'extraction existants conservés verts |
| `decisional-tools-panel` | Badge « Pré-rempli par l'IA (N) » de F-DT-36 passe de 0 à N | Test Jest `getPrefillCount` |
| Autres outils Travail FR consommant `travailExtractedData` (F-DT-08, F-DT-12, F-DT-24…) | Aucun — champs additifs ignorés par les autres outils | Compilation TS + tests existants |

### Smoke tests E2E concernés

- [x] `e2e/smoke/case-analysis-flow.spec.ts` — le flux d'analyse de dossier doit rester vert (la synthèse est enrichie, le contrat de réponse reste rétrocompatible).
- [x] `e2e/smoke/happy-path.spec.ts` — parcours nominal inchangé.
- [x] `cd e2e && npm test` avant push (préoccupation transversale « outil décisionnel »).

---

## Dépendances

### Subfeatures bloquantes

- Aucune SF bloquante. **Couplage de fichier** avec SF-246-02 et SF-246-04 — voir « Notes et décisions ».

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` touchée.

---

## Notes et décisions

### Couplage de fichiers partagés — ordre de dev de la vague 1

Les 3 SF de la vague 1 modifient potentiellement les **mêmes fichiers backend partagés** :

| Fichier | SF-246-01 | SF-246-02 | SF-246-04 |
|---------|-----------|-----------|-----------|
| `CaseAnalysisResponse.TravailExtractedData` (record + builder) | OUI | OUI | NON (`ImmigrationExtractedData`) |
| `LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION` | OUI | OUI | NON (`IMMIGRATION_INSTRUCTION`) |
| `extractTravailData()` | OUI | OUI | NON (`extractImmigrationData()`) |
| `CaseAnalysisResponse.ImmigrationExtractedData` / `IMMIGRATION_INSTRUCTION` / `extractImmigrationData()` | NON | NON | OUI |
| `case-analysis.model.ts` (`TravailExtractedData`) | OUI | OUI | NON (`ImmigrationExtractedData`) |

**Conséquence** :
- **SF-246-01 et SF-246-02 sont séquentielles** sur le record `TravailExtractedData`, le prompt `TRAVAIL_INSTRUCTION` et `extractTravailData()`. Elles **ne doivent pas** être développées sur deux branches simultanées modifiant ces 3 fichiers — risque de conflit de rebase systématique.
- **Ordre recommandé** : `SF-246-01` → `SF-246-02` (SF-246-01 d'abord car elle ajoute le plus de champs et fige la structure ; SF-246-02 rebase ensuite sur master à jour).
- **SF-246-04 est indépendante** : elle touche `ImmigrationExtractedData` / `IMMIGRATION_INSTRUCTION` / `extractImmigrationData()` — aucun fichier partagé avec 01/02. Elle peut être développée **en parallèle** de 01 et 02.
- **Parallélisation backend / frontend intra-SF** : autorisée (contrat API figé ci-dessus). Le frontend SF-246-01 (composant + helper + DTO) peut être développé en parallèle du backend sur deux branches isolées, le DTO `case-analysis.model.ts` étant la seule zone de contact — figé par le contrat ci-dessus.

### Décision de typage `DetectedAnswer` vs `Boolean`

Conserver `DetectedAnswer` (objet `{reponse, justification}`) pour les 3 champs d'appréciation (`entretienPrealableTenuDetected`, `lettreLicenciementMotiveeDetected`, `motivationLettreSuffisanteDetected`) plutôt qu'un simple `Boolean` : la justification citée est nécessaire au popover F-IA-03 et au respect de l'invariant « no-op gracieux » (l'avocat voit *pourquoi* l'IA a tranché). Cohérent avec `reclassementRespecteDetected` (SF-155-04).
