# Mini-spec — [F-246 / SF-246-22] Résorption anomalie pré-fill IA `travail-procedure` (Travail FR+BE)

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Découpage de référence : `docs/features/F-246/cadrage-decoupage.md` (§3.3 + §10 audit SF-246-14).
> **Outil** : `travail-procedure` (F-136 — Calendrier procédural travail FR + BE).

---

## Identifiant

`F-246 / SF-246-22`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-246-22-travail-procedure-anomalie`

---

## Objectif

Résorber l'anomalie de pré-remplissage de l'outil `travail-procedure` (F-136) en branchant
les deux champs `procedureTravailDetectee` et `dateDeclencheurProcedure` sur des sources
backend réelles — aujourd'hui ces deux champs sont aspirationnels (type d'intersection
frontend, cast permissif, jamais alimentés par le pipeline IA) — rendant le pré-fill
structurellement no-op.

---

## Comportement attendu

### Cas nominal

1. L'avocat lance l'analyse IA d'un dossier de droit du travail FR ou BE.
2. Le pipeline IA (prompt `LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION`) extrait, dans
   `travail_extracted_data`, un sous-objet `procedure_travail_detection` contenant :
   - `procedure_detectee` : code parmi l'énumération fermée des 6 codes FR+BE
     (`PRUDHOMMES_FR`, `APPEL_CA_SOCIALE_FR`, `CASSATION_SOCIALE_FR`,
     `TRIBUNAL_TRAVAIL_BE`, `COUR_TRAVAIL_BE`, `CASSATION_BE`)
   - `date_declencheur` : date ISO `YYYY-MM-DD` ou null
3. L'extracteur `extractTravailData()` parse ces deux champs en champs typés de
   `TravailExtractedData` : `procedureTravailDetectee` (`String`, code whitelisté) et
   `dateDeclencheurProcedure` (`String`, format ISO validé).
4. Le DTO frontend `TravailExtractedData` (`case-analysis.model.ts`) expose les deux
   champs réels (les champs aspirationnels du type d'intersection sont supprimés).
5. Le helper `TravailProcedurePrefillRules` lit `procedureTravailDetectee` et
   `dateDeclencheurProcedure` directement sur `TravailExtractedData` (suppression du
   type d'intersection `TravailProcedureAiData` et des casts permissifs).
6. À l'ouverture de l'outil `travail-procedure`, `prefillFromAi()` pré-sélectionne le
   type de procédure (avec gating pays via suffixe `_FR`/`_BE`) et pré-remplit la date
   déclencheur. Un badge `auto_awesome` s'affiche sur le champ type de procédure.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `procedure_travail_detection` absent du JSON IA | `procedureTravailDetectee` et `dateDeclencheurProcedure` null ; pré-fill no-op gracieux |
| Code hors whitelist des 6 codes | Code exclu (whitelist côté extracteur) ; `procedureTravailDetectee` null |
| Dossier FR + code BE (ex. `TRIBUNAL_TRAVAIL_BE`) | Gating pays : code non pré-sélectionné (`computeTypeProcedure` retourne null) |
| Date non ISO YYYY-MM-DD | `dateDeclencheurProcedure` null (validation `isoDateOrNull`) |
| `procedure_detectee` présent mais `date_declencheur` absent | `dateDeclencheurProcedure` null ; `procedureTravailDetectee` rempli si valide |
| Modification manuelle du type procédure | `provenanceTypeProcedure` remis à null ; badge masqué |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : `travail-procedure` est le seul outil F-136. Les fiches
  `prudhome-fiche` (FR) et `tribunal-travail-fiche` (BE) partagent `TravailExtractedData`
  mais n'utilisent pas `procedureTravailDetectee` / `dateDeclencheurProcedure`.
- [x] **Autres pays** : les 6 codes couvrent FR et BE — gating pays déjà présent dans le helper.
- [x] **Autres domaines** : non applicable — concept propre au Travail.
- [x] **Autres UI patterns** : pré-remplissage IA (pattern canonique), badge `auto_awesome`,
  suppression du type d'intersection aspirationnel.

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [x] Non applicable aux autres cibles (justification explicite ci-dessus)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

### 2. Pré-fill IA (OBLIGATOIRE)

- [x] `@Input() aiData?: TravailExtractedData | null` — déjà présent.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` **ET** `ngOnChanges()` — déjà câblé.
- [x] Signal `provenanceTypeProcedure = signal<'IA' | null>(null)` — déjà présent.
- [x] Badge `auto_awesome` sur le champ type de procédure — déjà présent.
- [x] Handler `onTypeProcedureChange()` qui remet `provenanceTypeProcedure` à `null` — déjà présent.

### 4. TOOL_REGISTRY symétrique + `getPrefillCount(input)`

- [x] `tool_id` `F-136-travail-procedure` présent dans `TOOL_REGISTRY` (mapping `aiData:
  ctx.synthesis?.travailExtractedData`) — inchangé.
- [x] Static `getPrefillCount` délègue à `TravailProcedurePrefillRules.computePrefillCount()`.

---

## Champs IA à extraire (pré-remplissage)

| Champ du formulaire | Type | Champ source `TravailExtractedData` | Extension requise |
|---------------------|------|--------------------------------------|-------------------|
| typeProcedure (select) | code parmi les 6 `TravailProcedureCode` | `procedureTravailDetectee` (`String`, nullable) | [x] record backend + [x] prompt `TRAVAIL_INSTRUCTION` + [x] extracteur + [x] DTO frontend (suppression type d'intersection) + [x] helper (suppression cast) |
| dateDeclencheur (date) | `String` ISO YYYY-MM-DD, nullable | `dateDeclencheurProcedure` (`String`, nullable) | idem |

---

## Critères d'acceptation

- [ ] Le record `TravailExtractedData` contient les deux champs `procedureTravailDetectee`
  (`String`, nullable) et `dateDeclencheurProcedure` (`String`, nullable), propagés par le
  builder F-234.
- [ ] Le prompt `TRAVAIL_INSTRUCTION` décrit le sous-objet `procedure_travail_detection`
  avec `procedure_detectee` (whitelist 6 codes) et `date_declencheur` (ISO strict).
- [ ] `extractTravailData()` parse `procedure_travail_detection` : code validé contre la
  whitelist des 6 codes, format ISO strict pour la date ; absent → deux champs null.
- [ ] Le DTO frontend `TravailExtractedData` expose `procedureTravailDetectee?: string | null`
  et `dateDeclencheurProcedure?: string | null` — le type d'intersection `TravailProcedureAiData`
  est supprimé.
- [ ] Le helper `TravailProcedurePrefillRules` lit les champs réels de `TravailExtractedData`
  sans cast permissif.
- [ ] Le composant `TravailProcedureSectionComponent.prefillFromAi()` supprime les casts inline.
- [ ] `computePrefillCount()` retourne 1 (type seul) ou 2 (type + date) selon les données IA.
- [ ] Une fixture IA avec `PRUDHOMMES_FR` + date → pré-fill chez un workspace FR.
- [ ] Code BE sur workspace FR → gating pays : non pré-sélectionné.
- [ ] Code hors whitelist → exclu.
- [ ] Nettoyage : le champ `fautesDetectees` de `TravailExtractedData` (vestige de l'ancienne
  anomalie, désormais dans `FamilleExtractedData` via SF-246-03) est retiré du DTO frontend
  `TravailExtractedData` — le composant `divorce-faute-section` utilise `FamilleExtractedData`
  et n'est pas impacté.

---

## Périmètre

### Hors scope (explicite)

- Toute modification de la logique de calcul des jalons (F-136 — endpoint `getJalons` inchangé).
- L'ajout d'autres champs à l'outil `travail-procedure` hors les deux champs ci-dessus.
- Les outils `prudhome-fiche` et `tribunal-travail-fiche` (leurs champs sont couverts par SF-246-15).

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{caseFileId}/travail-procedure-jalons` | Oui | MEMBER |

> Endpoint **inchangé** (existant F-136). La SF n'ajoute aucun endpoint.

### Contrat API (sous-objet ajouté dans `travail_extracted_data`)

```json
"procedure_travail_detection": {
  "procedure_detectee": "PRUDHOMMES_FR",
  "date_declencheur": "2026-03-01"
}
```

**Record backend `TravailExtractedData`** — 2 champs ajoutés :

```java
// SF-246-22 : type de procédure et date déclencheur pour pré-fill F-136 (FR+BE).
String procedureTravailDetectee
String dateDeclencheurProcedure
```

**DTO frontend `TravailExtractedData`** — 2 champs ajoutés, type d'intersection supprimé :

```ts
/** SF-246-22 : type de procédure travail pour pré-fill F-136. */
procedureTravailDetectee?: string | null;
/** SF-246-22 : date déclencheur de la procédure (ISO YYYY-MM-DD). */
dateDeclencheurProcedure?: string | null;
```

### Tables impactées

Aucune — `travailExtractedData` sérialisé dans le JSON de synthèse.

### Migration Liquibase

Non applicable.

### Composants Angular modifiés

- `travail-procedure-section-prefill-rules.ts` — suppression de `TravailProcedureAiData`, lecture directe.
- `travail-procedure-section.component.ts` — suppression des casts inline.

---

## Plan de test

### Tests unitaires backend

- [ ] `extractTravailData()` — `procedure_travail_detection` nominal → 2 champs remplis.
- [ ] `extractTravailData()` — sous-objet absent → 2 champs null.
- [ ] `extractTravailData()` — code hors whitelist → `procedureTravailDetectee` null.
- [ ] `extractTravailData()` — date non ISO → `dateDeclencheurProcedure` null.
- [ ] `LegalDomainPromptBuilderTest` — `TRAVAIL_INSTRUCTION` contient `procedure_travail_detection`.

### Tests Jest frontend

- [ ] `computeTypeProcedure` — cas nominal FR.
- [ ] `computeTypeProcedure` — code BE sur workspace FR → null.
- [ ] `computeTypeProcedure` — code hors whitelist → null.
- [ ] `computeDateDeclencheur` — cas nominal.
- [ ] `computeDateDeclencheur` — format non ISO → null.
- [ ] `computePrefillCount` — 0, 1, 2 cas.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Outil décisionnel métier** — composants impactés : `TravailProcedureSectionComponent`,
  `travail-procedure-section-prefill-rules.ts`, `TravailExtractedData`.

### Smoke tests E2E

- [x] `e2e/smoke/case-analysis-flow.spec.ts`
- [x] `cd e2e && npm test` avant push.

---

## Dépendances

- SF-246-03 mergée (nettoyage `fautesDetectees` de `TravailExtractedData`).

---

## Notes et décisions

### Suppression de `fautesDetectees` de `TravailExtractedData`

Le champ `fautesDetectees?: string[] | null` était dans `TravailExtractedData` à titre de
stub aspirationnel (commentaire « no-op gracieux — pipeline IA branché ultérieurement »).
SF-246-03 a branché ce champ sur `FamilleExtractedData`. La SF-246-22 supprime le vestige
de `TravailExtractedData` pour éliminer la confusion. Le composant `divorce-faute-section`
utilise `FamilleExtractedData` — aucun impact.
