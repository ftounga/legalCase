# Mini-spec — [F-246 / SF-246-08] Pré-remplissage IA — Lot Séparation / indivision / PACS / protection (Famille FR)

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Découpage de référence : `docs/features/F-246/cadrage-decoupage.md` (§3.1 ligne SF-246-08, vague 3).
> **SF de lot** : 6 outils — `pacs-dissolution` (F-FA-20), `separation-corps` (F-FA-21),
> `indivision` (F-FA-22), `ordonnance-protection` (F-FA-14), `mesures-provisoires` (F-FA-12),
> `revisions-post-divorce` (F-FA-13) — adossés au **même record** `FamilleExtractedData` et au
> **même prompt** `FAMILLE_INSTRUCTION`.

---

## Identifiant

`F-246 / SF-246-08`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-246-08-lot-vie-commune-prefill`

---

## Objectif

> En une phrase : que fait cette subfeature ?

Compléter le pré-remplissage IA des 6 outils décisionnels vie commune & protection (`pacs-dissolution`, `separation-corps`, `indivision`, `ordonnance-protection`, `mesures-provisoires`, `revisions-post-divorce` — F-FA-12/13/14/20/21/22) en faisant extraire par le pipeline IA les dates de séparation / requête / audience, le patrimoine commun, les revenus et le nombre d'enfants à charge aujourd'hui absents de `FamilleExtractedData`.

---

## Comportement attendu

### Cas nominal

1. L'avocat lance l'analyse IA d'un dossier de droit de la famille FR comportant une séparation, une dissolution de PACS, une demande d'ordonnance de protection ou une révision post-divorce.
2. Le pipeline IA (prompt `LegalDomainPromptBuilder.FAMILLE_INSTRUCTION`) extrait, dans `famille_extracted_data`, un sous-objet `vie_commune_detection` regroupant les dates, montants et dénombrements factuels.
3. L'extracteur `extractFamilleData()` parse ce sous-objet en champs typés du record `FamilleExtractedData`.
4. Le DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`) expose les champs ; les 6 entrées `TOOL_REGISTRY` passent déjà `aiData: ctx.synthesis?.familleExtractedData`.
5. À l'ouverture de l'un des 6 outils, `prefillFromAi()` renseigne les champs détectables ; un badge `auto_awesome` s'affiche par champ pré-rempli.
6. L'avocat peut modifier toute valeur : `onXxxChange()` remet `provenance<Field>` à `null` et déclenche la revérification F-IA-03.
7. Le badge « Pré-rempli par l'IA (N champs) » du panel F-IA-04 reflète `getPrefillCount()` de chaque outil.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Le LLM ne détecte aucune des situations | Sous-objet `vie_commune_detection` à `null` ; `prefillFromAi()` no-op gracieux ; `getPrefillCount()` = 0 ; aucun badge | n/a |
| Date présente mais ambiguë | Le prompt impose `null` plutôt qu'une date approximative | n/a |
| Date hors ISO `YYYY-MM-DD` | `isoDateOrNull()` côté extracteur rejette → `null` | n/a |
| Patrimoine commun / revenus ≤ 0 ou aberrant | `positiveDoubleOrNull()` → `null` (jamais `0` — invariant cadrage §5.2) | n/a |
| Nombre d'enfants à charge négatif ou aberrant (> 30) | `boundedIntOrNull()` garde de plage `[0, 30]` → `null` | n/a |
| Dossier de famille belge | Champs FR restent `null` (le prompt impose null hors FR) ; outils non affichés pour la BE | n/a |
| `famille_extracted_data` absent du JSON IA | `extractFamilleData()` retourne `null` ; outils affichés formulaire vierge | n/a |
| `aiData` arrive après le premier rendu | `prefillFromAi()` réinvoqué dans `ngOnChanges()` | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : `dateSeparation` est lue par `separation-corps` ET `indivision` (concept partagé) — d'où la SF de lot. Le concept « date de séparation » est aussi lu par SF-246-09 (filiation) ? Non — la filiation ne consomme pas la date de séparation. SF-246-12 (`divorce-desunion-be`) consomme une `dateSeparation` BE distincte : champ séparé (un champ = une définition juridique, FR ≠ BE — cadrage §5.1.1). Le flag `ordonnance_protection_envisagee` (F-200) pilote la visibilité de `ordonnance-protection` — finalité distincte du pré-fill.
- [x] **Autres pays** : France uniquement. Les concepts BE équivalents relèvent des outils Famille BE — champs `null` pour la BE.
- [x] **Autres domaines** : `victime-violences-l4256` (F-IM-24, Immigration) porte le concept voisin « date de l'ordonnance de protection JAF » — **outil et record distincts** (`ImmigrationExtractedData`), traité par SF-246-04 (vague 1). `dateRequeteOP` (Famille, `ordonnance-protection`) ≠ `dateOrdonnanceProtectionJaf` (Immigration) : la première est la date de **dépôt de la requête** par le justiciable, la seconde la date de **l'ordonnance rendue** par le JAF — deux faits juridiques distincts, deux champs séparés (cadrage §5.1.1).
- [x] **Autres UI patterns** : pré-remplissage IA (pattern canonique), badges `auto_awesome`, alertes F-IA-03.
- [x] **Autres flows transversaux** : aucun.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : `FamilleExtractedData` dans `divorce-accepte.model.ts` — extension + **réalignement** (retrait des champs aspirationnels `dateSeparation`, `patrimoineCommun`, `dateRequeteOP`, `dateAudienceAOMP`, `nbEnfantsACharge`, `revenusAnnuelsEpoux` déclarés sans source backend).
- [x] **Record / DTO backend** : `CaseAnalysisResponse.FamilleExtractedData` + builder F-234.
- [x] **Service / logique métier** : `extractFamilleData()`.
- [x] **Entité JPA + schéma DB** : non applicable — `familleExtractedData` sérialisé dans la synthèse IA. Inputs des 6 outils persistés par leurs endpoints existants (inchangés).
- [x] **Tests existants** : helpers `*-section-prefill-rules.spec.ts` des 6 outils, tests `extractFamilleData()`. Tous mis à jour.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : oui — dates / montants croisables. `coherenceAlerts` étendu sur les 6 outils.
- [x] **Refresh dashboard (F-IA-02)** : inchangé — `triggerRefresh()` déjà câblé.
- [x] **Pré-remplissage IA** : objet de la SF.
- [x] **Persistance des inputs** : inchangée — endpoints F-FA-12/13/14/20/21/22 existants.
- [x] **Masquage conditionnel selon type** : inchangé — visibilité F-IA-04 déjà gérée (FR + flags F-200).
- [x] **Alertes actives après calcul** : gate `coherenceAlerts` = `!this.showForm()` uniquement — vérifié sur les 6 composants.

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — aucun nouveau composant partagé, service ou endpoint.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `pacs-dissolution`, `separation-corps`, `indivision`, `ordonnance-protection`, `mesures-provisoires`, `revisions-post-divorce` (F-FA-12/13/14/20/21/22) | Oui | Intégrés dans cette SF de lot |
| `victime-violences-l4256` (F-IM-24, Immigration) | Non | Domaine + record distincts — traité par SF-246-04 (vague 1) ; champ `dateOrdonnanceProtectionJaf` ≠ `dateRequeteOP` |
| `divorce-desunion-be` (Famille BE) | Non | `dateSeparation` BE distincte — traité par SF-246-12 |
| Outils Travail / Immigration FR | Non | Concept propre au droit de la famille |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s) créée(s)
- [ ] Backlog VN
- [x] Non applicable aux autres cibles (justification explicite ci-dessus)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [ ] **Non applicable** — la SF livre 6 parties frontend décisionnelles.

### 1. Cohérence visuelle

- [x] **Palette statut** : navy/or info, vert OK, rouge réservé aux alertes critiques — conservé.
- [x] **Datepicker** : `<input type="date">` natif pour toutes les dates (séparation, requête OP, audience AOMP) — pas de `MatDatepicker`.
- [x] **Typographie** : `JetBrains Mono` pour `baseJuridique` / `formule`, `Inter` pour le reste — conservé.
- [x] **Gate `workspaceCountry`** : outils FR-only — bannière info en cas de mismatch BE (existant).
- [x] **Erreurs** : `MatSnackBar` — pas d'`alert()` / `confirm()`.
- [x] **Refresh dashboard** : `triggerRefresh()` dans le `next:` du POST — existant, inchangé.

### 2. Pré-fill IA (OBLIGATOIRE)

- [x] `@Input() aiData?: FamilleExtractedData | null` — déjà typé strictement sur les 6 composants.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` **ET** `ngOnChanges()`.
- [x] Signaux `provenance<Field>` : `provenanceDateSeparation`, `provenancePatrimoineCommun`, `provenanceDateRequeteOP`, `provenanceDateAudienceAOMP`, `provenancePatrimoineCommunSignificatif`, `provenanceNbEnfantsACharge`, `provenanceRevenusAnnuelsEpoux`, `provenanceDateConclusionPacs`.
- [x] Badge `auto_awesome` « Pré-rempli depuis l'analyse » par champ pré-rempli.
- [x] Handler `onXxxChange()` par champ qui remet `provenance<Field>` à `null`.

### 3. Validation F-IA-03 (OBLIGATOIRE)

- [x] `coherenceAlerts` étendu sur les 6 outils pour les champs date / montant pré-remplis.
- [x] Hiérarchie F-96 > Question IA > IA détection > Pièce manquante ; convergence → `'MULTI'`.
- [x] `<app-coherence-popover-trigger>` câblé sur les champs concernés.
- [x] Helper partagé `CoherenceAlertBuilder`.

### 4. TOOL_REGISTRY symétrique + `getPrefillCount(input)`

- [x] Les 6 entrées F-FA-12/13/14/20/21/22 déjà présentes dans `TOOL_REGISTRY` ; `inputs(ctx)` passe déjà `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`.
- [x] Static `getPrefillCount(input)` de chaque composant : refactorisé pour appeler `computePrefillCount()` enrichi.
- [x] Parité stricte `getPrefillCount()` ↔ `prefillFromAi()` sur les 6 outils.
- [x] Tests Jest par outil : (a) 0 champ, (b) partiel, (c) nominal.
- [x] Les 6 `tool_id` déjà présents dans `KNOWN_FRONTEND_TOOL_IDS` du test `DecisionToolVisibilityIntegrityIT` — pas de migration.

### 5. Parité des domaines métier (niveau ≥ 5)

- [x] Niveau des outils : `ordonnance-protection`, `mesures-provisoires`, `revisions-post-divorce`, `pacs-dissolution`, `separation-corps` niveau **5** (analyse de recevabilité / éligibilité) ; `indivision` niveau **3-4**.

| Domaine | Équivalent existe ? | Si non → action |
|---------|---------------------|-----------------|
| Droit du travail | Non | Concept non pertinent en droit du travail |
| Immigration | Voisin (F-IM-24 ordonnance de protection) | Concept voisin, outil distinct (titre de séjour vs mesure de protection familiale) — pré-fill traité par SF-246-04 |
| Famille | Oui (F-FA-12/13/14/20/21/22) | Ce sont les outils de cette SF |

> La SF complète le pré-fill d'outils existants — la parité de domaine a été tranchée à leur création.

---

## Champs IA à extraire (pré-remplissage)

- [ ] **Aucun pré-remplissage** — non, la SF crée le pré-remplissage des 6 outils.

| Champ du formulaire | Outil(s) consommateur(s) | Type | Champ source du record `FamilleExtractedData` | Extension requise |
|---------------------|--------------------------|------|------------------------------------------------|-------------------|
| date de la séparation effective | `separation-corps`, `indivision` | date (ISO YYYY-MM-DD) | `dateSeparation` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| patrimoine commun (€) | `separation-corps` | nombre (€) | `patrimoineCommun` (`Double`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| date de conclusion du PACS | `pacs-dissolution` | date (ISO YYYY-MM-DD) | `dateConclusionPacs` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| date de la requête en ordonnance de protection | `ordonnance-protection` | date (ISO YYYY-MM-DD) | `dateRequeteOP` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| date d'audience AOMP | `mesures-provisoires` | date (ISO YYYY-MM-DD) | `dateAudienceAOMP` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| patrimoine commun significatif | `mesures-provisoires` | booléen | dérivé : `true` si `patrimoineCommun != null` | « dérivé » — calculé dans `prefillFromAi()` |
| nombre d'enfants à charge | `revisions-post-divorce` | nombre (entier `[0, 30]`) | `nbEnfantsACharge` (`Integer`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| revenus annuels de l'époux (€) | `revisions-post-divorce` | nombre (€) | `revenusAnnuelsEpoux` (`Double`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

- [x] Pour chaque champ date / valeur à pré-remplir non encore présent, l'extension du record `FamilleExtractedData` **et** du prompt `LegalDomainPromptBuilder` (`FAMILLE_INSTRUCTION`) est explicitement dans le périmètre de cette SF.

> **Note couplage record partagé** : voir « Notes et décisions » — SF-246-08 séquentielle après SF-246-07.

> **Note de design IA** : 6 champs source ajoutés au record (5 si l'on ne compte pas le booléen dérivé). `patrimoineCommunSignificatif` est **dérivé** de `patrimoineCommun != null` dans `prefillFromAi()` — pas de champ booléen redondant. `dateSeparation` (FR) ≠ `dateSeparation` BE de SF-246-12 : champ séparé. `dateRequeteOP` (date de dépôt de la requête) ≠ `dateOrdonnanceProtectionJaf` (date de l'ordonnance rendue, Immigration SF-246-04) : le prompt nomme explicitement les deux concepts. `revenusAnnuelsEpoux` : le prompt précise « revenus annuels nets » et l'époux concerné (débiteur de la pension à réviser). Les autres champs des formulaires (motifs, modalités demandées) restent en saisie manuelle (documenté, cadrage §5.6).

---

## Critères d'acceptation

- [ ] Le record `FamilleExtractedData` contient les 6 nouveaux champs (`dateSeparation`, `patrimoineCommun`, `dateConclusionPacs`, `dateRequeteOP`, `dateAudienceAOMP`, `nbEnfantsACharge`, `revenusAnnuelsEpoux`), tous nullables, propagés par le builder F-234.
- [ ] Le prompt `FAMILLE_INSTRUCTION` décrit un sous-objet `vie_commune_detection` avec une définition juridique sans ambiguïté par champ + l'instruction `null` hors FR / hors certitude + la distinction explicite `date de requête OP` / `date d'audience AOMP` / `date de séparation` / `date de conclusion du PACS`.
- [ ] `extractFamilleData()` parse `vie_commune_detection` : dates via `isoDateOrNull()`, montants via `positiveDoubleOrNull()`, `nbEnfantsACharge` via `boundedIntOrNull(_, _, 0, 30)`.
- [ ] Le DTO frontend `FamilleExtractedData` expose les 6 champs avec les bons types TS et **ne déclare plus** de champ aspirationnel équivalent sans source backend.
- [ ] Les 6 helpers lisent des champs réels ; chaque `computePrefillCount()` retourne le nombre exact de champs pré-remplissables (incluant le booléen dérivé `patrimoineCommunSignificatif` pour `mesures-provisoires`).
- [ ] Les 6 `prefillFromAi()` renseignent les champs de leur tableau respectif quand `workspaceCountry === 'FRANCE'`, et restent no-op si BE.
- [ ] Chaque champ pré-rempli affiche un badge `auto_awesome` ; la modification manuelle remet `provenance<Field>` à `null`.
- [ ] Sur chaque outil, `getPrefillCount()` statique et `prefillFromAi()` runtime en parité stricte (test Jest cas 0 / partiel / nominal).
- [ ] Une fixture IA multi-dates (date de séparation `2024-01-10` ≠ date de requête OP `2024-03-05` ≠ date d'audience AOMP `2024-04-20`) remplit chaque champ avec la bonne date (test backend — invariant cadrage §5.1.6).
- [ ] `coherenceAlerts` lève une alerte F-IA-03 si une date / un montant pré-rempli diverge de la saisie (ex. date de requête OP postérieure à la date d'audience).
- [ ] Isolation workspace : non applicable côté pré-fill — voir section dédiée.

---

## Périmètre

### Hors scope (explicite)

- Toute modification du scoring, des verdicts, des formules ou des bases juridiques des 6 outils (logique métier inchangée).
- Le pré-remplissage des champs non factualisables de façon fiable par le LLM en V1 (motifs, modalités demandées, qualification des faits) — restent en saisie manuelle (documenté).
- Le pré-fill de l'outil Immigration `victime-violences-l4256` (F-IM-24) — traité par SF-246-04.
- L'outil `divorce-desunion-be` — traité par SF-246-12.
- Toute migration Liquibase — aucun nouvel outil, aucune table.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `dateSeparation` | `null` | ISO `YYYY-MM-DD` strict ou `null` |
| `patrimoineCommun` | `null` | montant `> 0` ou `null` |
| `dateConclusionPacs` | `null` | ISO `YYYY-MM-DD` strict ou `null` |
| `dateRequeteOP` | `null` | ISO `YYYY-MM-DD` strict ou `null` |
| `dateAudienceAOMP` | `null` | ISO `YYYY-MM-DD` strict ou `null` |
| `nbEnfantsACharge` | `null` | entier `[0, 30]` ou `null` |
| `revenusAnnuelsEpoux` | `null` | montant `> 0` ou `null` |

Comportements à la création : aucun — la SF étend un record de réponse IA.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| dates `date*` | Non | — | ISO `YYYY-MM-DD` ; rejet sinon → `null` | Non | `isoDateOrNull()` |
| `patrimoineCommun`, `revenusAnnuelsEpoux` | Non | — | nombre `> 0` ; `≤ 0` → `null` | Non | `positiveDoubleOrNull()` |
| `nbEnfantsACharge` | Non | — | entier `[0, 30]` ; hors plage → `null` | Non | `boundedIntOrNull(_, _, 0, 30)` |

Notes :
- Tous les champs nullables — invariant cadrage §5.1.2 / §5.2.
- Format ISO strict cohérent avec `dateAcceptationPV` (F-239).

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/{tool}` (6 outils) | Oui | LAWYER |
| GET | `/api/v1/case-files/{caseFileId}/{tool}` (6 outils) | Oui | MEMBER |

> Endpoints **inchangés** (existants F-FA-12/13/14/20/21/22). La SF n'ajoute aucun endpoint.

### Contrat API figé (parallélisation back / front)

**Bloc JSON produit par le pipeline IA** (sous `analysis_result.famille_extracted_data`) :

```json
"vie_commune_detection": {
  "date_separation": "2024-01-10",
  "patrimoine_commun_eur": 220000.0,
  "date_conclusion_pacs": "2015-09-01",
  "date_requete_op": "2024-03-05",
  "date_audience_aomp": "2024-04-20",
  "nb_enfants_a_charge": 2,
  "revenus_annuels_epoux_eur": 48000.0
}
```

**Record backend `FamilleExtractedData`** — 7 champs ajoutés (en fin de record, après les champs SF-246-07) :

```java
// SF-246-08 : 7 champs IA vie commune & protection pour pré-fill F-FA-12/13/14/20/21/22
// (Famille FR uniquement, nullables).
String dateSeparation,
Double patrimoineCommun,
String dateConclusionPacs,
String dateRequeteOP,
String dateAudienceAOMP,
Integer nbEnfantsACharge,
Double revenusAnnuelsEpoux
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

- `PacsDissolutionSectionComponent`, `SeparationCorpsSectionComponent`, `IndivisionSectionComponent`, `OrdonnanceProtectionSectionComponent`, `MesuresProvisoiresSectionComponent`, `RevisionsPostDivorceSectionComponent` — `prefillFromAi()` rendu effectif, signaux `provenance<Field>`, handlers `onXxxChange()`, badges `auto_awesome`, extension `coherenceAlerts`.
- Les 6 helpers `*-section-prefill-rules.ts` correspondants — lecture de champs réels, `computePrefillCount()` recalculé.

---

## Plan de test

### Tests unitaires

- [ ] `extractFamilleData()` — cas nominal : `vie_commune_detection` complet → 7 champs renseignés.
- [ ] `extractFamilleData()` — sous-objet absent → 7 champs `null`, pas d'exception.
- [ ] `extractFamilleData()` — date non ISO → champ `null` (fail-open).
- [ ] `extractFamilleData()` — montant ≤ 0 → `null` ; `nb_enfants_a_charge` hors `[0, 30]` → `null`.
- [ ] `LegalDomainPromptBuilderTest` — `FAMILLE_INSTRUCTION` contient les 7 clés `vie_commune_detection` + la distinction date requête OP / date audience AOMP / date séparation.
- [ ] Par outil : `computePrefillCount()` cas (a) `aiData` vide → 0 ; (b) partiel ; (c) nominal.
- [ ] Par outil : `computePrefillCount()` `workspaceCountry = 'BELGIQUE'` → 0.
- [ ] `mesures-provisoires` : `computePrefillCount()` compte le booléen dérivé `patrimoineCommunSignificatif` quand `patrimoineCommun != null`.
- [ ] Par outil : `prefillFromAi()` cas nominal → champs renseignés, badges présents.
- [ ] Par outil : `prefillFromAi()` parité stricte avec `getPrefillCount()`.
- [ ] Par outil : `onXxxChange()` — modification manuelle remet `provenance<Field>` à `null`.
- [ ] Par outil : `coherenceAlerts` — alerte levée si valeur saisie diverge de la détection IA.

### Tests d'intégration

- [ ] Analyse IA d'un dossier famille FR fixture avec séparation + requête OP → la synthèse expose `vie_commune_detection` peuplé.
- [ ] **Fixture multi-dates** (invariant cadrage §5.1.6) : dossier contenant date de séparation, date de requête OP, date d'audience AOMP, date de conclusion du PACS distinctes → chaque champ rempli avec la bonne date, aucune confusion.
- [ ] Dossier famille BE → les 7 champs FR restent `null`.
- [ ] `GET /api/v1/case-files/{caseFileId}/{tool}` → 403 si workspace différent (non-régression sur les 6 endpoints).

### Isolation workspace

- [x] Applicable — vérifiée au niveau des 6 endpoints existants (tests de non-régression conservés). Les champs IA n'introduisent aucun nouvel accès.

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
| Les 6 `*SectionComponent` F-FA-12/13/14/20/21/22 | `prefillFromAi()` devient effectif — risque de pré-remplir un champ à tort | Tests Jest pré-fill cas 0 / partiel / nominal |
| `extractFamilleData()` | Tout consommateur de `FamilleExtractedData` reçoit 7 champs supplémentaires (additif, nullable) | Tests d'extraction existants conservés verts |
| `decisional-tools-panel` | Badge « Pré-rempli par l'IA (N) » des 6 outils passe de 0 à N | Tests Jest `getPrefillCount` |
| Autres outils Famille FR consommant `familleExtractedData` | Aucun — champs additifs ignorés | Compilation TS + tests existants |

### Smoke tests E2E concernés

- [x] `e2e/smoke/case-analysis-flow.spec.ts` — flux d'analyse de dossier reste vert.
- [x] `e2e/smoke/happy-path.spec.ts` — parcours nominal inchangé.
- [x] `cd e2e && npm test` avant push (préoccupation transversale « outil décisionnel »).

---

## Dépendances

### Subfeatures bloquantes

- **SF-246-07** — couplage de fichier (record `FamilleExtractedData`, prompt `FAMILLE_INSTRUCTION`, `extractFamilleData()`, DTO `divorce-accepte.model.ts`). SF-246-08 doit être développée **après** le merge de SF-246-07 et rebasée sur master à jour — voir « Notes et décisions ».

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` touchée.

---

## Notes et décisions

### Couplage de fichiers partagés — ordre de dev de la série Famille FR

SF-246-08 modifie les **mêmes fichiers backend partagés** que les autres SF Famille : record `FamilleExtractedData` (+ builder F-234), prompt `FAMILLE_INSTRUCTION`, méthode `extractFamilleData()`, DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`).

**Décision** : ordre de dev imposé sur la série Famille :

```
SF-246-06 → SF-246-07 → SF-246-08 → SF-246-09 → SF-246-10 → SF-246-11 → SF-246-12
```

SF-246-08 ajoute ses 7 champs **après** ceux de SF-246-07, branchée après le merge de SF-246-07 et rebasée sur master à jour. Ne **jamais** développer deux SF Famille sur deux branches simultanées modifiant `FamilleExtractedData` — conflit de rebase systématique. La parallélisation backend / frontend **intra-SF** reste autorisée (contrat API figé ci-dessus).

### Décision de séparation FR / BE et requête OP / ordonnance JAF

`dateSeparation` (cette SF, Famille FR) est un champ distinct de la `dateSeparation` BE de SF-246-12 — le concept de séparation effective et son traitement juridique diffèrent FR/BE (cadrage §5.1.1). De même, `dateRequeteOP` (date de **dépôt de la requête** en ordonnance de protection) est distincte de `dateOrdonnanceProtectionJaf` (date de **l'ordonnance rendue** par le JAF, SF-246-04, Immigration) : deux faits juridiques différents, deux champs séparés — pas de mutualisation forcée.
