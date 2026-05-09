# Mini-spec — F-199 / SF-199-01 Extension du template subfeature — Conformité F-IA-04

> Pure SF de gouvernance documentaire. Aucun code applicatif, aucun test runtime.
> Doit être livrée avant F-200/201/203/204 qui dépendent du template enrichi.

---

## Identifiant

`F-199 / SF-199-01`

## Feature parente

`F-199` — Gouvernance F-IA-04 (template SF + 2 garde-fous CLAUDE.md)

## Statut

`ready`

## Date de création

2026-05-09

## Branche Git

`feat/F-199-gouvernance-fia04`

---

## Objectif

Étendre `project-governance/templates/subfeature-template.md` avec une section unique « Conformité F-IA-04 » composée de 5 blocs obligatoires, à remplir explicitement par toute SF frontend décisionnelle (composant `<app-XXX-section>` consommant un endpoint POST/GET décisionnel et intégré au panel F-IA-04 via `TOOL_REGISTRY`).

---

## Comportement attendu

### Cas nominal

1. Toute nouvelle mini-spec frontend décisionnelle est créée à partir du template enrichi.
2. L'auteur de la SF remplit les 5 blocs « Conformité F-IA-04 » avec les preuves attendues (références au composant canonique, captures de signal, tests Jest pré-fill, etc.).
3. La readiness checklist (cf. `project-governance/checklists/readiness-checklist.md`) ne passe pas tant que l'un des 5 blocs reste vide ou mal renseigné.

### Cas d'erreur (gouvernance)

| Situation | Comportement attendu |
|-----------|---------------------|
| SF frontend décisionnelle dont la section « Conformité F-IA-04 » est absente | REFUS — la mini-spec doit reprendre le template à jour |
| Bloc 2 (pré-fill IA) ou bloc 3 (validation F-IA-03) vide | REFUS — règle CLAUDE.md « SF frontend décisionnelle mergée sans pré-remplissage IA fonctionnel OU sans validation F-IA-03 » |
| Bloc 5 (parité domaines) vide pour un outil de niveau ≥ 5 | REFUS — règle CLAUDE.md « SF livre un outil décisionnel de niveau ≥ 5… sans section "Parité des domaines métier" » |
| SF non décisionnelle (purement transversale) | Cocher « non applicable » avec justification explicite — la section reste présente dans la mini-spec |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : non applicable — cette SF outille la gouvernance pour TOUS les futurs outils décisionnels (F-DT/F-IM/F-FA et descendants).
- [x] **Autres pays** : non applicable — la section couvre explicitement la gate `workspaceCountry` (FR + BE) dans le bloc 1.
- [x] **Autres domaines** : non applicable — la section impose la mention `TravailExtractedData` / `ImmigrationExtractedData` / `FamilleExtractedData` dans le bloc 2 et la parité domaines au bloc 5.
- [x] **Autres UI patterns** : la section référence le pattern canonique `immigration-title-decision-section` et l'helper partagé `CoherenceAlertBuilder`.
- [x] **Autres flows transversaux** : la section impose `CaseDashboardRefreshService.triggerRefresh()` (F-IA-02), `MatSnackBar` pour erreurs, et la directive `<app-coherence-popover-trigger>` (F-IA-03).

### Niveaux de vérification à couvrir

- [x] Modèle TypeScript / API exposée — bloc 4 impose `TOOL_REGISTRY.inputs(ctx)` symétrique.
- [x] Record / DTO backend — non applicable (cette SF est purement docs).
- [x] Service / logique métier — non applicable (cette SF est purement docs).
- [x] Entité JPA + schéma DB — non applicable (cette SF est purement docs).
- [x] Tests existants — bloc 4 impose tests Jest `getPrefillCount()` 0/M/N champs.

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] La section « Conformité F-IA-04 » est elle-même un pattern de gouvernance partagé : elle s'applique à TOUTES les futures SF frontend décisionnelles. Sa diffusion est immédiate via le template, pas de duplication ad hoc.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Toutes futures SF frontend décisionnelles | Oui | Bénéficient automatiquement du template enrichi |
| SF backend décisionnelles | Non | Le périmètre F-IA-04 frontend (panel, pré-fill, alerts) est frontend par nature |
| SF infrastructure / docs / marketing | Non | Cocher « non applicable » avec justification |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (template = source unique).
- [ ] Subfeature(s) parallèle(s) — non applicable.
- [ ] Backlog — non applicable.
- [ ] Non applicable aux autres cibles (justification explicite) — voir tableau ci-dessus.

---

## Critères d'acceptation

- [ ] Le fichier `project-governance/templates/subfeature-template.md` contient une section unique titrée `## Conformité F-IA-04 (SF frontend décisionnelle)` avec 5 sous-sections numérotées 1 à 5.
- [ ] Sous-section 1 (Cohérence visuelle) : palette statut (navy/or/rouge alerte critique uniquement), datepicker (`<input type="date">` ou `datetime-local` — pas MatDatepicker), JetBrains Mono (baseJuridique/formule), Inter (reste), gate `workspaceCountry` (bannière info, pas masquage silencieux), `MatSnackBar` (pas alert/confirm).
- [ ] Sous-section 2 (Pré-fill IA) : `@Input() aiData?` typé + `prefillFromAi()` invoquée dans `ngOnInit()` ET `ngOnChanges()` + signal `provenance<Field> = signal<'IA'|null>(null)` + badge UI `auto_awesome` + handler `onXxxChange()` qui reset provenance. Pattern de référence : `immigration-title-decision-section`.
- [ ] Sous-section 3 (Validation F-IA-03) : `coherenceAlerts = computed<Partial<Record<FieldName, CoherenceAlert>>>()` + directive `<app-coherence-popover-trigger>` + helper partagé `CoherenceAlertBuilder` (chemin `frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts`). Hiérarchie sources F-96 > Question IA > IA détection > Pièce manquante.
- [ ] Sous-section 4 (TOOL_REGISTRY symétrique) : entrée `inputs: (ctx) => ({ caseFileId, workspaceCountry, aiData, procedureChecks, aiQuestions, piecesManquantes })` + static `getPrefillCount(input)` parité stricte avec `prefillFromAi()` runtime. Tests Jest 0/M/N champs.
- [ ] Sous-section 5 (Parité domaines métier — niveau ≥ 5) : pour scoring/comparateur/détection événement, lister explicitement si Travail/Immigration/Famille ont l'équivalent — sinon ouvrir une feature jumelle au backlog OU justifier le non-applicable.
- [ ] La section référence explicitement le composant canonique `immigration-title-decision-section` et le skill `ai-skills/frontend-coherence-audit.md` pour l'audit périodique.
- [ ] La section autorise « non applicable » avec justification explicite pour les SF non décisionnelles.

---

## Périmètre

### Hors scope (explicite)

- Aucune modification de code applicatif (frontend/backend).
- Aucune migration Liquibase.
- Aucun test runtime à lancer.
- Aucune modification de `docs/PRODUCT_SPEC.md` ni de `MEMORY.md`.
- Aucune modification du skill `ai-skills/frontend-coherence-audit.md` (la section template y renvoie pour l'audit, sans le redéfinir).

---

## Valeurs initiales

Non applicable — pas d'entité créée.

---

## Contraintes de validation

Non applicable — pas de champ formulaire.

---

## Technique

### Endpoint(s)

Non applicable.

### Tables impactées

Non applicable.

### Migration Liquibase

- [ ] Oui
- [x] Non applicable

### Composants Angular (si applicable)

Non applicable — pure modification documentaire.

### Fichier impacté

| Fichier | Opération | Notes |
|---------|-----------|-------|
| `project-governance/templates/subfeature-template.md` | INSERT (ajout d'une nouvelle section) | Insertion à la suite de la section « Analyse de cohérence transversale » et avant « Critères d'acceptation » |

---

## Plan de test

### Tests unitaires

Non applicable (pas de code).

### Tests d'intégration

Non applicable (pas de code).

### Validation manuelle (relecture)

- [ ] Le template, après modification, est lu de bout en bout par un mainteneur.
- [ ] Les 5 sous-sections sont présentes, numérotées, et chaque bloc est rédigé sans abréviation.
- [ ] Les références (`immigration-title-decision-section`, `CoherenceAlertBuilder`, `ai-skills/frontend-coherence-audit.md`) sont correctes.
- [ ] L'orthographe et les accents sont vérifiés (relecture).

### Isolation workspace

- [x] Non applicable — pure modification documentaire.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale runtime** — modification documentaire pure.

### Composants / endpoints existants potentiellement impactés

Non applicable.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné (justification : modification documentaire, aucun chemin runtime impacté).

---

## Impact par domaine métier

Cette SF est **transversale** — elle s'applique uniformément aux 3 domaines (droit du travail, immigration, famille) et aux 2 pays (FR, BE). Aucune adaptation par domaine.

Le bloc 5 « Parité des domaines métier » que la SF introduit dans le template est précisément le mécanisme qui force chaque future SF outil décisionnel à expliciter son comportement par domaine.

---

## Parité des domaines métier

Non applicable — la SF ne livre pas un outil décisionnel de niveau ≥ 5. Elle livre une règle de gouvernance qui sera elle-même un véhicule de parité pour toutes les SF futures.

---

## Dépendances

### Subfeatures bloquantes

Aucune — F-199 est elle-même bloquante pour F-200/201/203/204.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- La règle longue déjà présente dans `CLAUDE.md` (entrée table « Nouveau composant Angular décisionnel frontend… ») reste la **source de vérité juridique**. Le template ne fait que **réfléchir** cette règle dans un format pratique pour l'auteur de SF, sans la dupliquer dans son intégralité ni la diluer.
- Le bloc 5 (parité domaines) est conditionnel : il s'applique uniquement aux SF outil décisionnel de niveau ≥ 5 (scoring / comparateur / détection événement). Pour les niveaux 1–4, cocher « non applicable ».
- Le pattern de référence cité (`immigration-title-decision-section` post-SF-177-12) est le composant canonique consensus issu des audits successifs de F-155, F-IA-04, F-177.
