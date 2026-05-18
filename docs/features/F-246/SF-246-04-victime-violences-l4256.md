# Mini-spec — [F-246 / SF-246-04] Pré-remplissage IA — Victime de violences L.425-6 (date de l'ordonnance de protection JAF)

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Découpage de référence : `docs/features/F-246/cadrage-decoupage.md` (§3.1 ligne SF-246-04, vague 1).

---

## ⚠️ Écart de cadrage signalé (à valider avant readiness)

Le tableau §2.1 du cadrage (ligne #32) classe `victime-violences-l4256` en **« Travail FR »** avec un
champ source `TravailExtractedData.dateOrdonnanceProtectionJaf`. La vérification du code contredit ce
classement :

- L'outil est enregistré dans `TOOL_REGISTRY` sous l'ID **`F-IM-24-victime-violences-l4256-fr`** —
  domaine **Immigration FR** (et non Travail).
- Son fondement juridique est le **CESEDA L.425-6** (titre de séjour délivré à la victime de
  violences conjugales bénéficiant d'une ordonnance de protection) — droit des étrangers, pas droit
  du travail.
- Son binding `inputs(ctx)` passe **`aiData: ctx.synthesis?.immigrationExtractedData`** — le composant
  `VictimeViolencesL4256SectionComponent` a `@Input() aiData?: ImmigrationExtractedData | null`.
- Le helper `victime-violences-l4256-section-prefill-rules.ts` documente explicitement le gap dans
  `ImmigrationExtractedData` (pas dans `TravailExtractedData`).

**Décision retenue pour cette mini-spec** : le champ source est créé dans le record
**`ImmigrationExtractedData`** (et non `TravailExtractedData`), le prompt étendu est
**`IMMIGRATION_INSTRUCTION`** (et non `TRAVAIL_INSTRUCTION`), l'extracteur est
**`extractImmigrationData()`**. La ligne #32 du cadrage `cadrage-decoupage.md` doit être corrigée en
post-merge (domaine « Immigration FR », record `ImmigrationExtractedData`,
champ `dateOrdonnanceProtectionJaf`). Cet écart est **sans impact sur l'ordonnancement** : SF-246-04
ne touche aucun fichier partagé avec SF-246-01 / SF-246-02 → elle reste pleinement parallélisable.

---

## Identifiant

`F-246 / SF-246-04`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-18

## Branche Git

`feat/SF-246-04-victime-violences-prefill`

---

## Objectif

> En une phrase : que fait cette subfeature ?

Brancher le pré-remplissage IA de l'outil `victime-violences-l4256` (F-IM-24, Immigration FR) en faisant extraire par le pipeline IA la date de l'ordonnance de protection rendue par le juge aux affaires familiales — aujourd'hui absente de `ImmigrationExtractedData` — afin que `prefillFromAi()` renseigne le champ `dateOrdonnanceProtection` du formulaire.

---

## Comportement attendu

### Cas nominal

1. L'avocat lance l'analyse IA d'un dossier d'immigration FR contenant l'ordonnance de protection délivrée par le JAF (Cciv art. 515-9 à 515-13).
2. Le pipeline IA (prompt `LegalDomainPromptBuilder.IMMIGRATION_INSTRUCTION`) extrait, dans `immigration_extracted_data`, le champ `date_ordonnance_protection_jaf` au format ISO `YYYY-MM-DD`.
3. L'extracteur `extractImmigrationData()` parse le champ en `ImmigrationExtractedData.dateOrdonnanceProtectionJaf`.
4. Le DTO frontend `ImmigrationExtractedData` (`case-analysis.model.ts`) expose le champ ; le `TOOL_REGISTRY` le passe déjà via `aiData: ctx.synthesis?.immigrationExtractedData`.
5. À l'ouverture de l'outil `victime-violences-l4256`, `prefillFromAi()` renseigne `dateOrdonnanceProtection` ; un badge `auto_awesome` « Pré-rempli depuis l'analyse » s'affiche.
6. L'avocat peut modifier la valeur : le handler `onDateOrdonnanceChange()` remet `provenanceDateOrdonnance` à `null` et déclenche la revérification F-IA-03.
7. Le badge « Pré-rempli par l'IA (N champs) » du panel F-IA-04 reflète `getPrefillCount()` (passe de 0 à 1 quand la date est détectée).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Aucune ordonnance de protection dans les pièces | `date_ordonnance_protection_jaf` à `null` ; `prefillFromAi()` no-op gracieux ; `getPrefillCount()` = 0 ; aucun badge | n/a |
| Date présente mais non lisible / format ambigu | Le prompt impose `null` plutôt qu'une date approximative ; champ `null` | n/a |
| Le LLM renvoie une date hors ISO `YYYY-MM-DD` | `textOrNull()` côté extracteur conserve le texte brut ; `prefillFromAi()` rejette via la regex `ISO_DATE_RE` du helper → pas de pré-fill | n/a |
| Dossier d'immigration belge | Champ FR reste `null` (le prompt impose null hors FR) ; outil non affiché pour la BE | n/a |
| Confusion avec une autre date du dossier (date d'expiration du titre, date de dépôt de procédure) | Le prompt nomme explicitement « date de l'ordonnance de protection rendue par le JAF » — distincte de `date_expiration_titre` et `date_depot_procedure` | n/a |
| `aiData` arrive après le premier rendu | `prefillFromAi()` réinvoqué dans `ngOnChanges()` (déjà câblé, l. 174-179 du composant) | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : seul `victime-violences-l4256` (F-IM-24) consomme la date d'ordonnance de protection. L'outil Famille `ordonnance-protection` (F-FA-14) porte aussi le concept « date de la requête OP » — mais c'est un **outil distinct** d'un **domaine distinct** (Famille FR) avec son **propre record** `FamilleExtractedData`, traité par **SF-246-08** (vague 3). Pas de mutualisation entre `ImmigrationExtractedData` et `FamilleExtractedData` ; champs séparés assumés (un champ = une définition juridique, cadrage §5.1.1).
- [x] **Autres pays** : France uniquement. Le titre de séjour « victime de violences » L.425-6 est un dispositif CESEDA français — pas d'équivalent BE dans cet outil. Champ `null` pour la BE.
- [x] **Autres domaines** : `ordonnance-protection` F-FA-14 (Famille) porte un concept voisin — articulation documentée, traité indépendamment par SF-246-08.
- [x] **Autres UI patterns** : pré-remplissage IA (pattern canonique), badge `auto_awesome`, alertes F-IA-03.
- [x] **Autres flows transversaux** : aucun.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : `ImmigrationExtractedData` dans `case-analysis.model.ts`.
- [x] **Record / DTO backend** : `CaseAnalysisResponse.ImmigrationExtractedData` + builder F-234.
- [x] **Service / logique métier** : `extractImmigrationData()`.
- [x] **Entité JPA + schéma DB** : non applicable — `immigrationExtractedData` est sérialisé dans la synthèse IA. Les inputs validés de F-IM-24 restent persistés par l'endpoint `victime-violences-l4256` existant (inchangé).
- [x] **Tests existants** : `victime-violences-l4256-section-prefill-rules.spec.ts` (`computeDateOrdonnanceProtection` retournant `null`), `victime-violences-l4256.service.spec.ts`, tests `extractImmigrationData()`. Mis à jour par la SF.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : oui — la SF câble une alerte F-IA-03 sur `DATE_ORDONNANCE_PROTECTION` (croisement IA détection / question IA / pièce manquante). Le composant a déjà la structure F-IA-03.
- [x] **Refresh dashboard (F-IA-02)** : inchangé — `triggerRefresh()` déjà dans le `next:` du POST.
- [x] **Pré-remplissage IA** : objet de la SF — `prefillFromAi()` rendu effectif sur 1 champ.
- [x] **Persistance des inputs** : inchangée — inputs persistés via l'endpoint F-IM-24 existant.
- [x] **Masquage conditionnel selon type** : inchangé — visibilité F-IA-04 de F-IM-24 déjà gérée (FR + immigration).
- [x] **Alertes actives après calcul** : gate `coherenceAlerts` = `!this.showForm()` uniquement — vérifié.

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — aucun nouveau composant partagé, service ou endpoint.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `victime-violences-l4256` (F-IM-24) | Oui | Intégré dans cette SF |
| `ordonnance-protection` F-FA-14 (Famille) | Non | Domaine + record distincts (`FamilleExtractedData`) — traité par SF-246-08 (vague 3) |
| Autres outils Immigration FR | Non | Aucun autre outil ne porte le concept « ordonnance de protection JAF » |
| Outils Immigration BE | Non | Dispositif CESEDA L.425-6 propre à la France — champ `null` en BE |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s) créée(s)
- [x] Backlog VN — le concept Famille voisin (`ordonnance-protection` F-FA-14, « date de la requête OP ») est **déjà couvert par SF-246-08** du même découpage F-246 (vague 3) — pas de nouvelle entrée backlog requise.
- [x] Non applicable aux autres cibles (justification explicite ci-dessus)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [ ] **Non applicable** — la SF livre une partie frontend décisionnelle (`victime-violences-l4256-section`).

### 1. Cohérence visuelle

- [x] **Palette statut** : navy/or info, vert OK, rouge réservé aux alertes critiques — conservé.
- [x] **Datepicker** : `<input type="date">` natif pour `dateOrdonnanceProtection`, `dateExpirationProtection` — pas de `MatDatepicker`.
- [x] **Typographie** : `JetBrains Mono` pour `baseJuridique` / `formule`, `Inter` pour le reste — conservé.
- [x] **Gate `workspaceCountry`** : outil FR-only — bannière info en cas de mismatch BE (existant).
- [x] **Erreurs** : `MatSnackBar` — pas d'`alert()` / `confirm()`.
- [x] **Refresh dashboard** : `triggerRefresh()` dans le `next:` du POST — existant, inchangé.

### 2. Pré-fill IA (OBLIGATOIRE)

- [x] `@Input() aiData?: ImmigrationExtractedData | null` — déjà typé strictement.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` **ET** `ngOnChanges()` — déjà câblé (la SF rend le corps effectif : `computeDateOrdonnanceProtection()` retourne aujourd'hui `null` inconditionnellement).
- [x] Signal `provenanceDateOrdonnance = signal<'IA' | null>(null)` — **déjà présent** (l. 119 du composant).
- [x] Badge `auto_awesome` « Pré-rempli depuis l'analyse » à côté de `dateOrdonnanceProtection` — déjà prévu, activé par la SF.
- [x] Handler `onDateOrdonnanceChange()` qui remet `provenanceDateOrdonnance` à `null` — déjà présent (l. 202), conservé.

### 3. Validation F-IA-03 (OBLIGATOIRE)

- [x] `coherenceAlerts` : alerte `DATE_ORDONNANCE_PROTECTION` quand la date saisie diverge de la date détectée par l'IA, ou quand la pièce « ordonnance de protection » est listée manquante alors qu'une date est saisie.
- [x] Hiérarchie F-96 > Question IA > IA détection > Pièce manquante respectée ; convergence → `'MULTI'`.
- [x] `<app-coherence-popover-trigger>` câblé sur `dateOrdonnanceProtection`.
- [x] Helper partagé `CoherenceAlertBuilder` (`frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts`).

### 4. TOOL_REGISTRY symétrique + `getPrefillCount(input)`

- [x] Entrée `F-IM-24-victime-violences-l4256-fr` déjà présente dans `TOOL_REGISTRY` ; `inputs(ctx)` passe déjà `aiData` (`immigrationExtractedData`), `procedureChecks`, `aiQuestions`, `piecesManquantes` — aucune modification du binding requise.
- [x] Static `getPrefillCount(input)` du composant : appelle `VictimeViolencesL4256PrefillRules.computePrefillCount()` qui ne retourne plus systématiquement 0 quand la date est détectée.
- [x] Parité stricte `getPrefillCount()` ↔ `prefillFromAi()` : même guard `ISO_DATE_RE`, même condition `workspaceCountry === 'FRANCE'`.
- [x] Tests Jest : (a) 0 champ (aiData vide / BE / date non ISO), (b) — pas de cas « partiel » (1 seul champ), (c) 1 champ cas nominal.
- [x] `tool_id` `F-IM-24-victime-violences-l4256-fr` déjà présent dans `KNOWN_FRONTEND_TOOL_IDS` du test `DecisionToolVisibilityIntegrityIT` — pas de migration `decision_tool_visibility_rules`.

### 5. Parité des domaines métier (niveau ≥ 5)

- [x] Niveau du tool : **5** (scoring d'éligibilité — `ELIGIBLE_PLEIN_DROIT` / `ELIGIBLE_SOUS_RESERVE` / `NON_ELIGIBLE`).

| Domaine | Équivalent existe ? | Si non → action |
|---------|---------------------|-----------------|
| Droit du travail | Non | Le titre de séjour « victime de violences » L.425-6 n'a pas de transposition en droit du travail — concept non pertinent |
| Immigration | Oui (F-IM-24) | C'est l'outil de cette SF |
| Famille | Voisin (F-FA-14 ordonnance de protection) | Concept voisin, outil distinct (mesure de protection familiale, pas titre de séjour) — pré-fill traité par SF-246-08 |

> La SF complète le pré-fill d'un outil existant — la parité de domaine de F-IM-24 a été tranchée à sa création.

---

## Champs IA à extraire (pré-remplissage)

- [ ] **Aucun pré-remplissage** — non, la SF crée le pré-remplissage.

> Le formulaire F-IM-24 (`VictimeViolencesL4256Request`) a 6 champs : `dateOrdonnanceProtection` (date),
> `juridiction` (texte), `dureeProtectionMois` (nombre), `dateExpirationProtection` (date),
> `enfantsAcharge` (nombre), `nationalite` (texte). Seule la **date de l'ordonnance** est extractible
> de façon fiable et sans ambiguïté en V1 (point d'entrée du calcul). Les autres champs restent en
> saisie manuelle — documenté, pas une dette masquée (cf. §5.6 cadrage) :
> `nationalite` n'est pas systématiquement reliée au contexte « violences » ; `juridiction`,
> `dureeProtectionMois`, `dateExpirationProtection`, `enfantsAcharge` ne sont pas factualisables de
> façon fiable sans risque de confusion.

| Champ du formulaire | Type | Champ source du record `ImmigrationExtractedData` | Extension requise |
|---------------------|------|---------------------------------------------------|-------------------|
| `dateOrdonnanceProtection` | date (ISO YYYY-MM-DD) | `ImmigrationExtractedData.dateOrdonnanceProtectionJaf` (`String`, nullable) | [x] record + [x] prompt `LegalDomainPromptBuilder` (`IMMIGRATION_INSTRUCTION`) + [x] extracteur (`extractImmigrationData()`) + [x] DTO frontend |

- [x] Pour le champ date à pré-remplir non encore présent (`dateOrdonnanceProtectionJaf`), l'extension du record `ImmigrationExtractedData` **et** du prompt `LegalDomainPromptBuilder` (`IMMIGRATION_INSTRUCTION`) est explicitement dans le périmètre de cette SF.

> **Note couplage** : voir « Notes et décisions » — SF-246-04 ne touche **aucun** fichier partagé avec SF-246-01 / SF-246-02 (record et prompt **Immigration**, pas Travail) → pleinement parallélisable.

---

## Critères d'acceptation

- [ ] Le record `ImmigrationExtractedData` contient le champ `dateOrdonnanceProtectionJaf` (`String`, nullable), propagé par le builder F-234 (constructeur + `toBuilder()` + `Builder`).
- [ ] Le prompt `IMMIGRATION_INSTRUCTION` décrit le champ `date_ordonnance_protection_jaf` avec une définition juridique sans ambiguïté (« date de l'ordonnance de protection rendue par le juge aux affaires familiales, Cciv 515-9 ») + l'instruction `null` hors FR / hors certitude + la distinction explicite d'avec `date_expiration_titre` et `date_depot_procedure`.
- [ ] `extractImmigrationData()` parse `date_ordonnance_protection_jaf` via `textOrNull()`.
- [ ] Le DTO frontend `ImmigrationExtractedData` (`case-analysis.model.ts`) expose `dateOrdonnanceProtectionJaf?: string | null`.
- [ ] `VictimeViolencesL4256PrefillRules.computeDateOrdonnanceProtection()` lit `aiData.dateOrdonnanceProtectionJaf`, valide via `ISO_DATE_RE`, retourne la date ou `null` ; `computePrefillCount()` retourne 1 quand la date est valide et FR, 0 sinon.
- [ ] `prefillFromAi()` du composant renseigne `dateOrdonnanceProtection` quand `workspaceCountry === 'FRANCE'` et que la date est valide, reste no-op sinon.
- [ ] Le champ pré-rempli affiche un badge `auto_awesome` ; la modification manuelle remet `provenanceDateOrdonnance` à `null` et masque le badge.
- [ ] `getPrefillCount()` statique et `prefillFromAi()` runtime en parité stricte (test Jest cas 0 / nominal).
- [ ] Une fixture IA contenant **deux dates concurrentes** (date d'ordonnance de protection ≠ date d'expiration du titre) remplit `dateOrdonnanceProtectionJaf` avec la bonne date (test backend — invariant cadrage §5.1.6).
- [ ] `coherenceAlerts` lève une alerte F-IA-03 si la date saisie diverge de la date détectée par l'IA.
- [ ] Isolation workspace : non applicable côté pré-fill (donnée portée par la synthèse du dossier) — voir section dédiée.

---

## Périmètre

### Hors scope (explicite)

- Toute modification du scoring d'éligibilité, des verdicts ou des bases juridiques de F-IM-24 (logique métier inchangée).
- Le pré-remplissage de `juridiction`, `dureeProtectionMois`, `dateExpirationProtection`, `enfantsAcharge`, `nationalite` — non factualisables de façon fiable par le LLM en V1 ; restent en saisie manuelle (documenté).
- Le pré-fill de l'outil Famille `ordonnance-protection` F-FA-14 — domaine et record distincts, traité par SF-246-08 (vague 3).
- Tout outil Immigration BE — hors périmètre vague 1 FR.
- Toute migration Liquibase — aucun nouvel outil, aucune table.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `dateOrdonnanceProtectionJaf` | `null` | Format ISO `YYYY-MM-DD` ou texte brut si l'IA renvoie un format non conforme ; le pré-fill rejette tout ce qui n'est pas ISO |

Comportements à la création : aucun — la SF étend un record de réponse IA.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `dateOrdonnanceProtectionJaf` (backend) | Non | — | texte ; idéalement ISO `YYYY-MM-DD` | Non | `textOrNull()` |
| `dateOrdonnanceProtection` (frontend, pré-fill) | Non | — | ISO `YYYY-MM-DD` strict ; sinon pas de pré-fill | Non | regex `ISO_DATE_RE` du helper |

Notes :
- Champ nullable — invariant cadrage §5.1.2 : une date non identifiée de façon fiable reste `null`.
- Le helper applique la regex `ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/` avant tout pré-fill — cohérent avec le traitement de `dateAcceptationPV` (F-239) et `avisMedecinTravailDate` (SF-155-04).

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/victime-violences-l4256` | Oui | LAWYER |
| GET | `/api/v1/case-files/{caseFileId}/victime-violences-l4256` | Oui | MEMBER |

> Endpoints **inchangés** (existants SF-208-04). La SF n'ajoute aucun endpoint : le champ IA transite par la synthèse d'analyse (`immigrationExtractedData`).

### Contrat API figé (parallélisation back / front)

**Champ JSON produit par le pipeline IA** (sous `analysis_result.immigration_extracted_data`) :

```json
"date_ordonnance_protection_jaf": "2026-01-15"
```

**Record backend `ImmigrationExtractedData`** — 1 champ ajouté (en fin de record, après `nationalite`) :

```java
// SF-246-04 : date de l'ordonnance de protection JAF (Cciv 515-9) pour pré-fill F-IM-24 victime de
// violences L.425-6. Immigration FRANCE uniquement, nullable — dossier BE : null.
String dateOrdonnanceProtectionJaf
```

**DTO frontend `ImmigrationExtractedData`** (`case-analysis.model.ts`) — 1 champ ajouté :

```ts
/** SF-246-04 : date de l'ordonnance de protection JAF pour pré-fill F-IM-24 (FR uniquement). */
dateOrdonnanceProtectionJaf?: string | null;
```

**Helper `VictimeViolencesL4256PrefillRules`** — contrat figé (le helper utilise déjà `PrefillCountInput` de `decision-tool.contract.ts` ; `PrefillCountInput.aiData` doit exposer le nouveau champ) :

```ts
// computeDateOrdonnanceProtection lit input.aiData?.dateOrdonnanceProtectionJaf,
// valide via ISO_DATE_RE, retourne string | null.
// computePrefillCount retourne 1 si FR + date valide, 0 sinon.
```

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| — | — | Aucune table impactée. `immigrationExtractedData` sérialisé dans le JSON de synthèse |

### Migration Liquibase

- [ ] Oui
- [x] Non applicable — aucun nouvel outil, aucune table, aucune règle `decision_tool_visibility_rules`.

### Composants Angular (si applicable)

- `VictimeViolencesL4256SectionComponent` — `computeDateOrdonnanceProtection()` rendu effectif via le helper, badge `auto_awesome` activé, alerte `coherenceAlerts` `DATE_ORDONNANCE_PROTECTION` ajoutée. Les signaux `provenanceDateOrdonnance` et le handler `onDateOrdonnanceChange()` existent déjà — conservés.
- `victime-violences-l4256-section-prefill-rules.ts` — `computeDateOrdonnanceProtection()` lit `aiData.dateOrdonnanceProtectionJaf` au lieu de retourner `null` ; `computePrefillCount()` recalculé.

---

## Plan de test

### Tests unitaires

- [ ] `extractImmigrationData()` — cas nominal : `date_ordonnance_protection_jaf` présent → champ renseigné.
- [ ] `extractImmigrationData()` — champ absent → `dateOrdonnanceProtectionJaf` `null`, pas d'exception.
- [ ] `LegalDomainPromptBuilderTest` — `IMMIGRATION_INSTRUCTION` contient la clé `date_ordonnance_protection_jaf` + la mention de distinction d'avec `date_expiration_titre` / `date_depot_procedure`.
- [ ] `computeDateOrdonnanceProtection()` — date ISO valide → retourne la date.
- [ ] `computeDateOrdonnanceProtection()` — date non ISO → retourne `null`.
- [ ] `computeDateOrdonnanceProtection()` — `aiData` absent → `null`.
- [ ] `computePrefillCount()` — cas (a) `aiData` vide ou date non ISO → 0 ; cas (c) date valide + FR → 1.
- [ ] `computePrefillCount()` — `workspaceCountry = 'BELGIQUE'` → 0.
- [ ] `prefillFromAi()` — date valide → `dateOrdonnanceProtection` renseigné, badge présent, `provenanceDateOrdonnance = 'IA'`.
- [ ] `prefillFromAi()` — parité stricte avec `getPrefillCount()`.
- [ ] `onDateOrdonnanceChange()` — modification manuelle remet `provenanceDateOrdonnance` à `null`.
- [ ] `coherenceAlerts` — alerte levée si la date saisie diverge de la date détectée.

### Tests d'intégration

- [ ] Analyse IA d'un dossier immigration FR fixture avec ordonnance de protection JAF → la synthèse expose `date_ordonnance_protection_jaf` peuplé.
- [ ] **Fixture multi-dates** (invariant cadrage §5.1.6) : dossier contenant date d'ordonnance de protection `2026-01-15` ET date d'expiration du titre `2026-06-30` ET date de dépôt de procédure `2026-02-01` → `dateOrdonnanceProtectionJaf` = 15/01, `dateExpirationTitre` = 30/06, `dateDepotProcedure` = 01/02, aucune confusion.
- [ ] Dossier immigration BE → `dateOrdonnanceProtectionJaf` reste `null`.
- [ ] `GET /api/v1/case-files/{caseFileId}/victime-violences-l4256` → 403 si workspace différent (non-régression endpoint existant).

### Isolation workspace

- [x] Applicable — vérifiée au niveau de l'endpoint F-IM-24 existant (test de non-régression conservé). Le champ IA n'introduit aucun nouvel accès : il transite par la synthèse du dossier, déjà isolée par `caseFileId` + workspace.

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
| `VictimeViolencesL4256SectionComponent` | `prefillFromAi()` devient effectif sur `dateOrdonnanceProtection` | Tests Jest pré-fill cas 0 / nominal |
| `extractImmigrationData()` | Tout consommateur de `ImmigrationExtractedData` reçoit 1 champ supplémentaire (additif, nullable — non cassant via builder F-234) | Tests d'extraction existants conservés verts |
| `decisional-tools-panel` | Badge « Pré-rempli par l'IA (N) » de F-IM-24 passe de 0 à 1 | Test Jest `getPrefillCount` |
| Autres outils Immigration FR consommant `immigrationExtractedData` (OQTF, titre de séjour…) | Aucun — champ additif ignoré | Compilation TS + tests existants |

### Smoke tests E2E concernés

- [x] `e2e/smoke/case-analysis-flow.spec.ts` — flux d'analyse de dossier reste vert.
- [x] `e2e/smoke/happy-path.spec.ts` — parcours nominal inchangé.
- [x] `cd e2e && npm test` avant push (préoccupation transversale « outil décisionnel »).

---

## Dépendances

### Subfeatures bloquantes

- Aucune. SF-246-04 touche `ImmigrationExtractedData` / `IMMIGRATION_INSTRUCTION` / `extractImmigrationData()` — **aucun fichier partagé** avec SF-246-01 / SF-246-02 (qui touchent le Travail). SF-246-04 est **parallélisable** avec les deux autres SF de la vague 1.

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` touchée.

---

## Notes et décisions

### Couplage de fichiers partagés — ordre de dev de la vague 1

| Fichier | SF-246-01 | SF-246-02 | SF-246-04 |
|---------|-----------|-----------|-----------|
| `TravailExtractedData` + `TRAVAIL_INSTRUCTION` + `extractTravailData()` + DTO front `TravailExtractedData` | OUI | OUI | **NON** |
| `ImmigrationExtractedData` + `IMMIGRATION_INSTRUCTION` + `extractImmigrationData()` + DTO front `ImmigrationExtractedData` | NON | NON | **OUI** |

**Conséquence** : SF-246-04 ne partage **aucun** fichier de modèle / prompt / extracteur avec SF-246-01 et SF-246-02. Elle peut être développée **entièrement en parallèle** des deux autres SF de la vague 1, sans risque de conflit de rebase. SF-246-01 et SF-246-02 restent, elles, séquentielles entre elles (voir leurs mini-specs respectives).

**Ordre de dev recommandé vague 1** : `SF-246-01` → `SF-246-02` (séquentiel, fichiers Travail partagés) ; `SF-246-04` **en parallèle** de l'un ou l'autre. La parallélisation backend / frontend **intra-SF** est autorisée (contrat API figé ci-dessus, `case-analysis.model.ts` zone de contact figée).

### Écart de cadrage à corriger en post-merge

La ligne #32 du tableau §2.1 de `docs/features/F-246/cadrage-decoupage.md` indique « Travail FR » et
`TravailExtractedData`. Le code montre qu'il s'agit d'un outil **Immigration FR** (`F-IM-24`,
CESEDA L.425-6, `ImmigrationExtractedData`). À la mise à jour documentaire post-merge (étape 6),
corriger cette ligne : domaine « Immigration FR », champ source `ImmigrationExtractedData.dateOrdonnanceProtectionJaf`.
L'écart est sans impact sur l'ordonnancement (SF-246-04 reste indépendante et parallélisable).
