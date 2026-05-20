# SF-215-02 — Single permit BE — frontend

## Identifiant
`F-215 / SF-215-02`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-02-single-permit-be-frontend`

---

## Objectif
Livrer le composant Angular standalone OnPush `<app-single-permit-be-section>` conforme au pattern canonique F-IA-04, consommant le backend SF-215-01, avec pré-remplissage IA des 5 champs et validation F-IA-03, BELGIQUE UNIQUEMENT.

---

## Comportement attendu

### Cas nominal
- L'outil s'affiche dans `app-decisional-tools-panel` uniquement sur workspace BELGIQUE × DROIT_IMMIGRATION, mode CONTEXTUAL (flag `single_permit_envisage = true`).
- Formulaire : dateDebutPermit, dateFinPermit, regionInstruction (dropdown), typeActivite (dropdown), motifDemande (radio Nouveau / Renouvellement).
- À la soumission, POST puis affichage du verdict : `statutRenouvellement` (badge coloré), `dateLimiteDemande` (date formatée JJ/MM/YYYY), `joursAvantExpiration` (nombre), `regionCompetente` (texte), `etapesProchaines` (liste bullets).
- Sur workspace FR : bannière info « Outil Belgique uniquement ».
- `CaseDashboardRefreshService.triggerRefresh()` invoqué dans le `next:` du POST.

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| POST 400 | MatSnackBar erreur métier |
| GET 404 | Section vide, formulaire vierge |
| workspace.country ≠ BELGIQUE | Bannière info, formulaire masqué |

---

## Analyse de cohérence transversale

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern canonique `F-IM-14-9bis-humanitaire-be` | Oui | Template de référence — réutiliser structure, palette, signals |
| `TOOL_REGISTRY` (decisional-tools-panel.component.ts) | Oui | Entrée à ajouter avec `tool_id = 'F-IM-25-single-permit-be'` |
| `getPrefillCount` / `prefillFromAi()` parité stricte | Oui | 5 champs : dates + région + type + motif |
| F-IA-03 alertes cohérence | Oui | Alert sur dateFinPermit si diverge de `singlePermitDateFin` extrait |

### Décision
- Étendu à toutes les cibles applicables dans cette SF.
- Non applicable aux autres domaines.

---

## Conformité F-IA-04

### 1. Cohérence visuelle
- [x] Palette statut : URGENT = rouge, DANS_DELAI = orange, DEPOSE_EN_TEMPS = vert, EXPIRE = rouge sombre.
- [x] Dates : `<input type="date">` (pas MatDatepicker).
- [x] Typographie : `JetBrains Mono` pour dates de délai ; `Inter` pour le reste.
- [x] Gate `workspaceCountry` : bannière info si FR.
- [x] Erreurs : `MatSnackBar`.
- [x] Refresh dashboard : `CaseDashboardRefreshService.triggerRefresh()` dans `next:`.

### 2. Pré-fill IA
- [x] `@Input() aiData?: ImmigrationExtractedData`
- [x] `prefillFromAi()` dans `ngOnInit()` ET `ngOnChanges()`
- [x] Signals provenance : `provenanceDateDebut`, `provenanceDateFin`, `provenanceRegion`, `provenanceTypeActivite`, `provenanceMotif`
- [x] Badge `auto_awesome` par champ pré-rempli
- [x] Handlers `onDateDebutChange()` etc. → reset provenance

### 3. Validation F-IA-03
- [x] `coherenceAlerts = computed<Partial<Record<...>>>()` — alert si `dateFinPermit` saisie diverge de `singlePermitDateFin` IA
- [x] `CoherenceAlertBuilder` partagé (chemin obligatoire)
- [x] `<app-coherence-popover-trigger>` sur chaque champ concerné

### 4. TOOL_REGISTRY symétrique + `getPrefillCount`
- [x] Entrée TOOL_REGISTRY : `tool_id = 'F-IM-25-single-permit-be'`, inputs ctx complets (caseFileId, workspaceCountry, aiData, procedureChecks, aiQuestions, piecesManquantes)
- [x] `static getPrefillCount(input)` : parité stricte avec `prefillFromAi()` — 5 champs max
- [x] Tests Jest : 0 champs, partiel (2/5), nominal (5/5)
- [x] `F-IM-25-single-permit-be` présent dans `KNOWN_FRONTEND_TOOL_IDS` (vérifié avant commit via self-check grep)

### 5. Parité des domaines métier
- [ ] Niveau 4 (checklist procédurale + calculateur délais) → **non applicable, justifier**.
- Single permit = procédure Immigration BE-only. Pas d'équivalent Travail (le droit au travail est couvert par F-IM-07 transversal) ni Famille.

---

## Champs IA à extraire (pré-remplissage)

| Champ formulaire | Type | Champ source `ImmigrationExtractedData` | Extension |
|-----------------|------|-----------------------------------------|-----------|
| `dateDebutPermit` | date | `singlePermitDateDebut` | Livré SF-215-01 |
| `dateFinPermit` | date | `singlePermitDateFin` | Livré SF-215-01 |
| `regionInstruction` | enum | `singlePermitRegion` | Livré SF-215-01 |
| `typeActivite` | enum | `singlePermitTypeActivite` | Livré SF-215-01 |
| `motifDemande` | enum | `singlePermitMotif` | Livré SF-215-01 |

---

## Critères d'acceptation

- [ ] Composant affiché uniquement sur workspace BELGIQUE × DROIT_IMMIGRATION × flag `single_permit_envisage=true`
- [ ] Formulaire pré-rempli depuis aiData (5 champs) avec badges provenance
- [ ] POST nominal affiche verdict + badge statut
- [ ] MatSnackBar sur erreur POST 400
- [ ] Bannière info sur workspace FR
- [ ] `getPrefillCount` retourne 5 si tous les champs IA présents, 0 si aucun
- [ ] TOOL_REGISTRY entrée symétrique avec ctx complet
- [ ] `npm run build` BUILD SUCCESS, aucune régression
- [ ] `npm test` : ≥ 15 tests Jest verts (composant + service + prefill-rules)
- [ ] Self-check grep `F-IM-25-single-permit-be` dans TOOL_REGISTRY + KNOWN_FRONTEND_TOOL_IDS avant commit

---

## Hors périmètre
- Export PDF (disponible via F-IM-06 générique)
- Génération de courrier OE (P3)

---

## Tables / endpoints / composants impactés
- `SinglePermitBeSectionComponent` (nouveau, standalone OnPush)
- `SinglePermitBeService` (HTTP wrapper POST/GET)
- `SinglePermitBeModel` (DTO TypeScript)
- `single-permit-be-prefill-rules.ts`
- Entrée TOOL_REGISTRY dans `decisional-tools-panel.component.ts`
- DTO frontend `ImmigrationExtractedData.ts` : 5 nouveaux champs (alignés sur backend SF-215-01)

---

## Plan de test

### Tests Jest
- `SinglePermitBeSectionComponent.spec.ts` : rendu, pré-fill IA, reset provenance, submission POST, bannière FR, badge statut URGENT/DANS_DELAI
- `SinglePermitBeService.spec.ts` : POST/GET wrapping
- `single-permit-be-prefill-rules.spec.ts` : 3 cas getPrefillCount (0, partiel, complet)

### Isolation workspace
- Applicable — POST sur dossier autre workspace → 404 (vérifié côté backend SF-215-01 ; côté frontend : caseFileId dans l'URL suffit)

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Outil décisionnel métier** — `F-IM-25-single-permit-be` dans TOOL_REGISTRY

### Smoke tests E2E concernés
- [x] `cd e2e && npm test` avant push (obligation décisionnelle frontend)

---

## Dépendances
- SF-215-01 — statut : ready (doit être mergée avant SF-215-02)

---

## Notes et décisions
- Self-check grep OBLIGATOIRE pré-commit : `grep -r "F-IM-25-single-permit-be" frontend/src/` doit retourner au moins 2 occurrences (TOOL_REGISTRY + KNOWN_FRONTEND_TOOL_IDS).
- Annotation BELGIQUE UNIQUEMENT dans le helper `prefillFromAi()` : `if (this.workspaceCountry !== 'BELGIQUE') return;`
