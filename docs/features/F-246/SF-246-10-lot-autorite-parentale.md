# Mini-spec — [F-246 / SF-246-10] Pré-remplissage IA — Lot Autorité parentale (âge des enfants) (Famille FR)

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Découpage de référence : `docs/features/F-246/cadrage-decoupage.md` (§3.1 ligne SF-246-10, vague 3).
> **SF de lot** : 4 outils — `autorite-parentale`, `changement-residence`,
> `desaccords-parentaux`, `calendrier-garde` (F-FA-19) — adossés au **même record**
> `FamilleExtractedData` et au **même prompt** `FAMILLE_INSTRUCTION`, partageant le champ
> `ageEnfants`.

---

## Identifiant

`F-246 / SF-246-10`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-246-10-lot-autorite-parentale-prefill`

---

## Objectif

> En une phrase : que fait cette subfeature ?

Compléter le pré-remplissage IA des 4 outils décisionnels autorité parentale (`autorite-parentale`, `changement-residence`, `desaccords-parentaux`, `calendrier-garde` — F-FA-19) en faisant extraire par le pipeline IA l'âge des enfants concernés — champ partagé aujourd'hui absent de `FamilleExtractedData`.

---

## Comportement attendu

### Cas nominal

1. L'avocat lance l'analyse IA d'un dossier de droit de la famille FR comportant une question d'autorité parentale, de résidence ou de garde d'enfant(s).
2. Le pipeline IA (prompt `LegalDomainPromptBuilder.FAMILLE_INSTRUCTION`) extrait, dans `famille_extracted_data`, un sous-objet `autorite_parentale_detection` contenant l'âge des enfants concernés.
3. L'extracteur `extractFamilleData()` parse ce sous-objet en champ typé du record `FamilleExtractedData` (`agesEnfantsDetectes`, liste d'entiers).
4. Le DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`) expose le champ ; les 4 entrées `TOOL_REGISTRY` passent déjà `aiData: ctx.synthesis?.familleExtractedData`.
5. À l'ouverture de l'un des 4 outils, `prefillFromAi()` renseigne l'âge des enfants (et les dates de référence du calendrier pour `calendrier-garde`) ; un badge `auto_awesome` s'affiche.
6. L'avocat peut modifier toute valeur : `onXxxChange()` remet `provenance<Field>` à `null` et déclenche la revérification F-IA-03.
7. Le badge « Pré-rempli par l'IA (N champs) » du panel F-IA-04 reflète `getPrefillCount()` de chaque outil.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Le LLM ne détecte pas l'âge des enfants | Sous-objet `autorite_parentale_detection` à `null` ou liste vide ; `prefillFromAi()` no-op gracieux ; `getPrefillCount()` = 0 ; aucun badge | n/a |
| Un âge négatif ou aberrant dans la liste (> 25) | L'âge concerné est exclu de la liste (`boundedIntOrNull()` garde de plage `[0, 25]`) ; les autres âges conservés | n/a |
| Liste vide après filtrage | Champ `agesEnfantsDetectes` à `null` (jamais liste vide — invariant cadrage §5.1.2 transposé aux listes) | n/a |
| Date de référence du calendrier hors ISO `YYYY-MM-DD` | `isoDateOrNull()` rejette → `null` | n/a |
| Dossier de famille belge | Champ FR reste `null` (le prompt impose null hors FR) ; outils non affichés pour la BE | n/a |
| `famille_extracted_data` absent du JSON IA | `extractFamilleData()` retourne `null` ; outils affichés formulaire vierge | n/a |
| `aiData` arrive après le premier rendu | `prefillFromAi()` réinvoqué dans `ngOnChanges()` | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : les 4 outils F-FA-19 partagent le champ `ageEnfants` — d'où la SF de lot (un seul champ source, un seul ajout au record/prompt). `calendrier-garde` consomme en plus des dates de référence du calendrier. Les flags F-200 (`changement_residence_envisage`, `desaccord_parental_detecte`) pilotent la **visibilité** — finalité distincte du pré-fill. Aucun autre outil Famille ne consomme l'âge des enfants comme tel (la pension alimentaire utilise `nb_enfants` — champ distinct déjà géré).
- [x] **Autres pays** : France uniquement. L'autorité parentale BE relève de `autorite-parentale-be` (outil sans champ date/valeur — hors périmètre §2.3 cadrage). Champ `null` pour la BE.
- [x] **Autres domaines** : non applicable — concept propre au droit de la famille FR.
- [x] **Autres UI patterns** : pré-remplissage IA (pattern canonique), badges `auto_awesome`, alertes F-IA-03.
- [x] **Autres flows transversaux** : aucun.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : `FamilleExtractedData` dans `divorce-accepte.model.ts` — extension + **réalignement** (retrait du champ aspirationnel `ageEnfants` déclaré sans source backend, remplacé par `agesEnfantsDetectes`).
- [x] **Record / DTO backend** : `CaseAnalysisResponse.FamilleExtractedData` + builder F-234.
- [x] **Service / logique métier** : `extractFamilleData()`.
- [x] **Entité JPA + schéma DB** : non applicable — `familleExtractedData` sérialisé dans la synthèse IA. Inputs des 4 outils persistés par leurs endpoints existants (inchangés).
- [x] **Tests existants** : helpers `*-section-prefill-rules.spec.ts` des 4 outils, tests `extractFamilleData()`. Tous mis à jour. **NB calendrier-garde** : le composant ne lit aujourd'hui aucun `aiData` (cadrage §2.1 ligne #25) — la SF y branche le pré-fill.
- [x] **Réalignement frontend** : le modèle `FamilleExtractedData` déclare actuellement `ageEnfants` (cf. cadrage §5.3) — la SF le remplace par `agesEnfantsDetectes` (source backend réelle) et adapte les helpers.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : oui — âges croisables. `coherenceAlerts` étendu sur les 4 outils.
- [x] **Refresh dashboard (F-IA-02)** : inchangé — `triggerRefresh()` déjà câblé.
- [x] **Pré-remplissage IA** : objet de la SF.
- [x] **Persistance des inputs** : inchangée — endpoints F-FA-19 existants.
- [x] **Masquage conditionnel selon type** : inchangé — visibilité F-IA-04 déjà gérée (FR + flags F-200).
- [x] **Alertes actives après calcul** : gate `coherenceAlerts` = `!this.showForm()` uniquement — vérifié sur les 4 composants.

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — aucun nouveau composant partagé, service ou endpoint. La représentation « liste d'âges » réutilise le type `number[]` standard ; aucun nouveau pattern.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `autorite-parentale`, `changement-residence`, `desaccords-parentaux`, `calendrier-garde` (F-FA-19) | Oui | Intégrés dans cette SF de lot |
| `autorite-parentale-be` | Non | Outil BE sans champ date/valeur — hors périmètre §2.3 cadrage |
| Calculateur de pension alimentaire | Non | Consomme `nb_enfants` (compte), pas l'âge — champ distinct |
| Outils Travail / Immigration FR | Non | Concept propre au droit de la famille |

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
- [x] **Datepicker** : `<input type="date">` natif pour les dates de référence du calendrier (`calendrier-garde`) — pas de `MatDatepicker`.
- [x] **Typographie** : `JetBrains Mono` pour `baseJuridique` / `formule`, `Inter` pour le reste — conservé.
- [x] **Gate `workspaceCountry`** : outils FR-only — bannière info en cas de mismatch BE (existant).
- [x] **Erreurs** : `MatSnackBar` — pas d'`alert()` / `confirm()`.
- [x] **Refresh dashboard** : `triggerRefresh()` dans le `next:` du POST — existant, inchangé.

### 2. Pré-fill IA (OBLIGATOIRE)

- [x] `@Input() aiData?: FamilleExtractedData | null` — déjà typé strictement sur les 4 composants ; ajouté à `calendrier-garde` si absent.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` **ET** `ngOnChanges()`.
- [x] Signaux `provenance<Field>` : `provenanceAgesEnfants` (4 outils) + `provenanceDateDebutCalendrier`, `provenanceDateFinCalendrier` (`calendrier-garde`).
- [x] Badge `auto_awesome` « Pré-rempli depuis l'analyse » par champ pré-rempli.
- [x] Handler `onXxxChange()` par champ qui remet `provenance<Field>` à `null`.

### 3. Validation F-IA-03 (OBLIGATOIRE)

- [x] `coherenceAlerts` étendu sur les 4 outils pour le champ `agesEnfants` (et les dates calendrier).
- [x] Hiérarchie F-96 > Question IA > IA détection > Pièce manquante ; convergence → `'MULTI'`.
- [x] `<app-coherence-popover-trigger>` câblé sur les champs concernés.
- [x] Helper partagé `CoherenceAlertBuilder`.

### 4. TOOL_REGISTRY symétrique + `getPrefillCount(input)`

- [x] Les 4 entrées F-FA-19 déjà présentes dans `TOOL_REGISTRY` ; `inputs(ctx)` passe déjà `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes` — vérifier que `calendrier-garde` passe bien `aiData` (le composant ne le lisait pas — cadrage §2.1).
- [x] Static `getPrefillCount(input)` de chaque composant : refactorisé pour appeler `computePrefillCount()` enrichi.
- [x] Parité stricte `getPrefillCount()` ↔ `prefillFromAi()` sur les 4 outils.
- [x] Tests Jest par outil : (a) 0 champ, (b) partiel, (c) nominal.
- [x] Les 4 `tool_id` déjà présents dans `KNOWN_FRONTEND_TOOL_IDS` du test `DecisionToolVisibilityIntegrityIT` — pas de migration.

### 5. Parité des domaines métier (niveau ≥ 5)

- [x] Niveau des outils : `changement-residence`, `desaccords-parentaux` niveau **5** (analyse de l'intérêt de l'enfant / recevabilité) ; `autorite-parentale` niveau **3-4** ; `calendrier-garde` niveau **3** (générateur de calendrier).

| Domaine | Équivalent existe ? | Si non → action |
|---------|---------------------|-----------------|
| Droit du travail | Non | L'autorité parentale n'a pas de transposition en droit du travail — concept non pertinent |
| Immigration | Non | Concept non pertinent en droit des étrangers |
| Famille | Oui (F-FA-19) | C'est le sous-domaine de cette SF |

> La SF complète le pré-fill d'outils existants — la parité de domaine de F-FA-19 a été tranchée à leur création.

---

## Champs IA à extraire (pré-remplissage)

- [ ] **Aucun pré-remplissage** — non, la SF crée le pré-remplissage des 4 outils.

| Champ du formulaire | Outil(s) consommateur(s) | Type | Champ source du record `FamilleExtractedData` | Extension requise |
|---------------------|--------------------------|------|------------------------------------------------|-------------------|
| âge des enfants concernés | `autorite-parentale`, `changement-residence`, `desaccords-parentaux`, `calendrier-garde` | liste de nombres (entiers `[0, 25]`) | `agesEnfantsDetectes` (`List<Integer>`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| date de début de la période du calendrier | `calendrier-garde` | date (ISO YYYY-MM-DD) | `dateDebutCalendrierDetectee` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| date de fin de la période du calendrier | `calendrier-garde` | date (ISO YYYY-MM-DD) | `dateFinCalendrierDetectee` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

- [x] Pour chaque champ valeur à pré-remplir non encore présent, l'extension du record `FamilleExtractedData` **et** du prompt `LegalDomainPromptBuilder` (`FAMILLE_INSTRUCTION`) est explicitement dans le périmètre de cette SF.

> **Note couplage record partagé** : voir « Notes et décisions » — SF-246-10 séquentielle après SF-246-09.

> **Note de design IA** : 3 champs source ajoutés au record. `agesEnfantsDetectes` est une **liste d'entiers** (`List<Integer>` backend, `number[] | null` frontend) — un dossier comporte couramment plusieurs enfants. Le prompt impose « âges en années à la date de l'analyse, dans l'ordre de naissance ; null si aucun âge déterminable » ; un âge non fiable est exclu de la liste plutôt que deviné. Liste vide → `null` (jamais `[]` — invariant cadrage §5.1.2 transposé). `prefillFromAi()` mappe la liste sur le contrôle de chaque outil selon son ergonomie propre (champ multi, contrôles répétés). Le mode de garde (`mode_garde_detaille`) reste extrait par le bloc `pension_alimentaire_data` existant — pas de duplication ici. Les autres champs des formulaires (modalités demandées, motif du désaccord) restent en saisie manuelle (documenté, cadrage §5.6).

---

## Critères d'acceptation

- [ ] Le record `FamilleExtractedData` contient les 3 nouveaux champs (`agesEnfantsDetectes` `List<Integer>`, `dateDebutCalendrierDetectee` `String`, `dateFinCalendrierDetectee` `String`), tous nullables, propagés par le builder F-234.
- [ ] Le prompt `FAMILLE_INSTRUCTION` décrit un sous-objet `autorite_parentale_detection` avec une définition juridique sans ambiguïté par champ + l'instruction `null` hors FR / hors certitude + l'instruction « exclure un âge non fiable de la liste ».
- [ ] `extractFamilleData()` parse `autorite_parentale_detection` : la liste d'âges via une boucle `boundedIntOrNull(_, _, 0, 25)` (chaque élément invalide exclu ; liste vide → `null`), dates via `isoDateOrNull()`.
- [ ] Le DTO frontend `FamilleExtractedData` expose les 3 champs avec les bons types TS et **ne déclare plus** le champ aspirationnel `ageEnfants`.
- [ ] Les 4 helpers lisent des champs réels ; chaque `computePrefillCount()` retourne le nombre exact de champs pré-remplissables.
- [ ] Les 4 `prefillFromAi()` renseignent les champs de leur tableau respectif quand `workspaceCountry === 'FRANCE'`, et restent no-op si BE. `calendrier-garde` voit son `prefillFromAi()` (aujourd'hui no-op total) rendu effectif.
- [ ] Chaque champ pré-rempli affiche un badge `auto_awesome` ; la modification manuelle remet `provenance<Field>` à `null`.
- [ ] Sur chaque outil, `getPrefillCount()` statique et `prefillFromAi()` runtime en parité stricte (test Jest cas 0 / partiel / nominal).
- [ ] Une fixture IA avec un dossier comportant trois enfants d'âges distincts → `agesEnfantsDetectes` = `[12, 9, 4]` dans l'ordre de naissance (test backend).
- [ ] Une fixture IA avec un âge aberrant (`[10, 200, 6]`) → `agesEnfantsDetectes` = `[10, 6]` (élément hors plage exclu).
- [ ] `coherenceAlerts` lève une alerte F-IA-03 si l'âge / la date pré-remplie diverge de la saisie de l'avocat.
- [ ] Isolation workspace : non applicable côté pré-fill — voir section dédiée.

---

## Périmètre

### Hors scope (explicite)

- Toute modification du scoring, des verdicts, des formules ou des bases juridiques des 4 outils (logique métier inchangée).
- Le pré-remplissage des champs non factualisables de façon fiable par le LLM en V1 (modalités demandées, motif du désaccord, choix du mode de garde au-delà de `mode_garde_detaille` déjà géré) — restent en saisie manuelle (documenté).
- L'outil `autorite-parentale-be` — hors périmètre §2.3 cadrage.
- Tout outil Famille BE — hors périmètre vague 3 FR.
- Toute migration Liquibase — aucun nouvel outil, aucune table.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `agesEnfantsDetectes` | `null` | `List<Integer>` d'entiers `[0, 25]` non vide, ou `null` |
| `dateDebutCalendrierDetectee` | `null` | ISO `YYYY-MM-DD` strict ou `null` |
| `dateFinCalendrierDetectee` | `null` | ISO `YYYY-MM-DD` strict ou `null` |

Comportements à la création : aucun — la SF étend un record de réponse IA.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `agesEnfantsDetectes` | Non | — | liste d'entiers `[0, 25]` ; éléments hors plage exclus ; liste vide → `null` | Non | boucle `boundedIntOrNull(_, _, 0, 25)` |
| `dateDebutCalendrierDetectee`, `dateFinCalendrierDetectee` | Non | — | ISO `YYYY-MM-DD` ; rejet sinon → `null` | Non | `isoDateOrNull()` |

Notes :
- Champs nullables — invariant cadrage §5.1.2, transposé aux listes (liste vide → `null`).
- Format ISO strict cohérent avec `dateAcceptationPV` (F-239).

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/{tool}` (4 outils F-FA-19) | Oui | LAWYER |
| GET | `/api/v1/case-files/{caseFileId}/{tool}` (4 outils F-FA-19) | Oui | MEMBER |

> Endpoints **inchangés** (existants F-FA-19). La SF n'ajoute aucun endpoint.

### Contrat API figé (parallélisation back / front)

**Bloc JSON produit par le pipeline IA** (sous `analysis_result.famille_extracted_data`) :

```json
"autorite_parentale_detection": {
  "ages_enfants": [12, 9, 4],
  "date_debut_calendrier": "2026-09-01",
  "date_fin_calendrier": "2027-08-31"
}
```

**Record backend `FamilleExtractedData`** — 3 champs ajoutés (en fin de record, après les champs SF-246-09) :

```java
// SF-246-10 : 3 champs IA autorité parentale pour pré-fill F-FA-19
// (Famille FR uniquement, nullables).
java.util.List<Integer> agesEnfantsDetectes,
String dateDebutCalendrierDetectee,
String dateFinCalendrierDetectee
```

**DTO frontend `FamilleExtractedData`** (`divorce-accepte.model.ts`) — 3 champs ajoutés :

```ts
/** SF-246-10 : âge des enfants + période du calendrier pour pré-fill F-FA-19 (FR uniquement). */
agesEnfantsDetectes?: number[] | null;
dateDebutCalendrierDetectee?: string | null;
dateFinCalendrierDetectee?: string | null;
```

**Helpers `*PrefillInput`** — chaque helper expose un `Pick<FamilleExtractedData, ...>` restreint + `workspaceCountry`. `computePrefillCount(input)` retourne 0 si `workspaceCountry !== 'FRANCE'` ; `agesEnfantsDetectes` non vide compte pour 1 champ pré-rempli.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| — | — | Aucune table impactée. `familleExtractedData` sérialisé dans le JSON de synthèse |

### Migration Liquibase

- [ ] Oui
- [x] Non applicable — aucun nouvel outil, aucune table, aucune règle `decision_tool_visibility_rules`.

### Composants Angular (si applicable)

- `AutoriteParentaleSectionComponent`, `ChangementResidenceSectionComponent`, `DesaccordsParentauxSectionComponent`, `CalendrierGardeSectionComponent` — `prefillFromAi()` rendu effectif (créé pour `calendrier-garde`), signaux `provenance<Field>`, handlers `onXxxChange()`, badges `auto_awesome`, extension `coherenceAlerts`.
- Les 4 helpers `*-section-prefill-rules.ts` correspondants — lecture de champs réels, `computePrefillCount()` recalculé.

---

## Plan de test

### Tests unitaires

- [ ] `extractFamilleData()` — cas nominal : `autorite_parentale_detection` complet → liste d'âges + 2 dates renseignées.
- [ ] `extractFamilleData()` — sous-objet absent → 3 champs `null`, pas d'exception.
- [ ] `extractFamilleData()` — liste contenant un âge aberrant → élément exclu, liste filtrée.
- [ ] `extractFamilleData()` — liste vide après filtrage → `agesEnfantsDetectes` `null`.
- [ ] `extractFamilleData()` — date non ISO → champ `null` (fail-open).
- [ ] `LegalDomainPromptBuilderTest` — `FAMILLE_INSTRUCTION` contient les 3 clés `autorite_parentale_detection` + l'instruction d'exclusion d'un âge non fiable.
- [ ] Par outil : `computePrefillCount()` cas (a) `aiData` vide → 0 ; (b) partiel ; (c) nominal.
- [ ] Par outil : `computePrefillCount()` `workspaceCountry = 'BELGIQUE'` → 0.
- [ ] `calendrier-garde` : `prefillFromAi()` rendu effectif (no-op total avant la SF).
- [ ] Par outil : `prefillFromAi()` cas nominal → champs renseignés, badges présents.
- [ ] Par outil : `prefillFromAi()` parité stricte avec `getPrefillCount()`.
- [ ] Par outil : `onXxxChange()` — modification manuelle remet `provenance<Field>` à `null`.
- [ ] Par outil : `coherenceAlerts` — alerte levée si valeur saisie diverge de la détection IA.

### Tests d'intégration

- [ ] Analyse IA d'un dossier famille FR fixture avec autorité parentale (3 enfants) → la synthèse expose `autorite_parentale_detection.ages_enfants` = `[12, 9, 4]`.
- [ ] **Fixture multi-valeurs** : dossier mentionnant des âges et des dates de calendrier → chaque champ rempli correctement.
- [ ] Dossier famille BE → les 3 champs FR restent `null`.
- [ ] `GET /api/v1/case-files/{caseFileId}/{tool}` → 403 si workspace différent (non-régression sur les 4 endpoints).

### Isolation workspace

- [x] Applicable — vérifiée au niveau des 4 endpoints F-FA-19 existants (tests de non-régression conservés). Les champs IA n'introduisent aucun nouvel accès.

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
| Les 4 `*SectionComponent` F-FA-19 | `prefillFromAi()` devient effectif (créé pour `calendrier-garde`) — risque de pré-remplir un champ à tort | Tests Jest pré-fill cas 0 / partiel / nominal |
| `extractFamilleData()` | Tout consommateur de `FamilleExtractedData` reçoit 3 champs supplémentaires (additif, nullable) | Tests d'extraction existants conservés verts |
| `decisional-tools-panel` | Badge « Pré-rempli par l'IA (N) » des 4 outils passe de 0 à N ; le binding `inputs(ctx)` de `calendrier-garde` doit passer `aiData` | Tests Jest `getPrefillCount` + vérification du binding |
| Autres outils Famille FR consommant `familleExtractedData` | Aucun — champs additifs ignorés | Compilation TS + tests existants |

### Smoke tests E2E concernés

- [x] `e2e/smoke/case-analysis-flow.spec.ts` — flux d'analyse de dossier reste vert.
- [x] `e2e/smoke/happy-path.spec.ts` — parcours nominal inchangé.
- [x] `cd e2e && npm test` avant push (préoccupation transversale « outil décisionnel »).

---

## Dépendances

### Subfeatures bloquantes

- **SF-246-09** — couplage de fichier (record `FamilleExtractedData`, prompt `FAMILLE_INSTRUCTION`, `extractFamilleData()`, DTO `divorce-accepte.model.ts`). SF-246-10 doit être développée **après** le merge de SF-246-09 et rebasée sur master à jour — voir « Notes et décisions ».

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` touchée.

---

## Notes et décisions

### Couplage de fichiers partagés — ordre de dev de la série Famille FR

SF-246-10 modifie les **mêmes fichiers backend partagés** que les autres SF Famille : record `FamilleExtractedData` (+ builder F-234), prompt `FAMILLE_INSTRUCTION`, méthode `extractFamilleData()`, DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`).

**Décision** : ordre de dev imposé sur la série Famille :

```
SF-246-06 → SF-246-07 → SF-246-08 → SF-246-09 → SF-246-10 → SF-246-11 → SF-246-12
```

SF-246-10 ajoute ses 3 champs **après** ceux de SF-246-09, branchée après le merge de SF-246-09 et rebasée sur master à jour. Ne **jamais** développer deux SF Famille sur deux branches simultanées modifiant `FamilleExtractedData` — conflit de rebase systématique. La parallélisation backend / frontend **intra-SF** reste autorisée (contrat API figé ci-dessus).

### Décision liste d'âges vs champ unique

L'âge des enfants est extrait sous forme de **liste** (`List<Integer>`) et non d'un champ unique : un dossier de droit de la famille comporte couramment plusieurs enfants, et chaque outil F-FA-19 raisonne sur l'ensemble de la fratrie (âge de discernement art. 388-1 Cciv, calendrier de garde collectif). Une liste vide est normalisée à `null` (jamais `[]`) — transposition de l'invariant « pas de valeur vide trompeuse » du cadrage §5.1.2 aux champs liste.

### Cas `calendrier-garde` — pré-fill créé de zéro

Le composant `calendrier-garde` ne consomme aujourd'hui aucun `aiData` (cadrage §2.1 ligne #25). La SF y ajoute le `@Input() aiData`, le `prefillFromAi()`, les signaux de provenance et vérifie que son entrée `TOOL_REGISTRY` passe bien `aiData` dans `inputs(ctx)` — sinon le binding est corrigé dans la même SF.
