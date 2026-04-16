# Mini-spec — F-IA-03 / SF-IA-03-15b Propagation popover source (Droit du travail + Famille)

## Identifiant

`F-IA-03 / SF-IA-03-15b`

## Feature parente

`F-IA-03` — Contrôle de cohérence sur les outils décisionnels

## Statut

`draft`

## Date de création

2026-04-16

## Branche Git

`feat/SF-IA-03-15b-popover-propagation-dt-fa`

---

## Objectif

Propager le popover de source enrichi (livré par SF-IA-03-15a sur F-DT-07) aux 6 outils restants du Droit du travail et de la Famille : F-DT-08 (validité licenciement), F-DT-09 (comparateur indemnités), F-DT-10 (rupture conventionnelle), F-FA-05 (partage immobilier), F-FA-06 (calendrier garde), F-FA-07 (checklist divorce).

---

## Comportement attendu

### Cas nominal

1. Le pipeline d'analyse dossier génère déjà les phrases d'explication (livré par 15a). **Le prompt Haiku est enrichi** pour produire aussi les `sourceKey` correspondant aux critères F96 spécifiques des 6 outils (ex. `FR_CONVOCATION`, `RC_CONSENTEMENT`, `FA05_VALEUR_VENALE`, `FA06_MODE_GARDE`, `DT09_TYPE_RUPTURE`), en plus des sourcekeys métier génériques (`convention_collective`, `date_entree`, etc.).
2. Nouvelle **directive Angular** `[appCoherencePopover]` factorise le pattern `cdkConnectedOverlay + CoherencePopoverComponent + open/close hover` introduit par 15a. Elle prend en entrée : `sourceKey`, `reason`, `caseFileId`, et une `SourceExplanation | null` optionnelle. Simplifie l'intégration dans 6 composants.
3. Chaque composant des 6 outils :
   - Injecte `SourceExplanationService` + `CoherenceSourceNavigator`
   - Charge la map d'explications à `ngOnInit` via `SourceExplanationService.getForCaseFile()`
   - Définit un mapping statique champ→sourceKey local au composant (ex. pour F-DT-09 : `TYPE_RUPTURE → DT09_TYPE_RUPTURE`, `ANCIENNETE → date_entree`, `SALAIRE → salaire_brut_mensuel`)
   - Remplace chaque `[matTooltip]="alertTooltip(alert)"` par la directive avec les inputs appropriés
4. F-DT-07 est **refactoré** pour consommer la directive (déduplication du code verbose de 15a).
5. Au survol d'un badge : popover identique à 15a (phrase, lien actionnable, navigation).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Pas d'explication pour un `sourceKey` dans la base | Popover en fallback template (identique à 15a) | 200 |
| Endpoint `/source-explanations` indisponible | Popover en fallback template, pas de lien actionnable | 200 |
| Haiku n'a pas généré de sourceKey F96 pour un critère (ex. `FR_MOTIVATION` absent) | Fallback template, pas de régression visuelle | 200 |
| Composant sans `caseFileId` défini (cas test ou bug) | Directive no-op, pas d'erreur console | 200 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : 10 outils décisionnels. 15a a couvert F-DT-07 ; cette SF couvre 6 outils (F-DT-08/09/10, F-FA-05/06/07). SF-IA-03-15c couvrira F-IM-05/06/07.
- [x] **Autres pays** : FR + BE couverts nativement — chaque composant a déjà ses variantes FR/BE, le popover les consomme sans logique pays supplémentaire.
- [x] **Autres domaines** : Droit du travail + Famille couverts ici ; Immigration dans 15c.
- [x] **Autres UI patterns** : identifiés et classés dans le scan rétrospectif de 15a (F-69/F-92/F-93/F-94/F-96). Non applicable dans cette SF.
- [x] **Autres flows transversaux** : aucun (pas d'auth / workspace / plans / navigation).

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : aucun nouveau modèle. Réutilise `SourceExplanation` (15a).
- [x] **Record / DTO backend** : aucun changement de DTO. Seul le prompt Haiku est enrichi.
- [x] **Service / logique métier** : `SourceExplanationGenerator` prompt enrichi avec les sourcekeys F96 attendus. Backend fail-open préservé.
- [x] **Entité JPA + schéma DB** : aucun changement de schéma.
- [x] **Tests existants** : specs des 6 composants (Licenciement, IndemniteComparatif, RuptureConv, PartageImmobilier, CalendrierGarde, DivorceChecklist) adaptés pour mocker `SourceExplanationService` (1 call HTTP au démarrage).

### Cas spécifique : nouvelle feature d'outil décisionnel

Non applicable — la SF ne crée pas un nouvel outil, elle étend une surcouche transversale existante (F-IA-03).

### Cas spécifique : nouveau pattern UI ou service partagé

**Directive `[appCoherencePopover]`** — premier nouveau pattern introduit par cette SF.

- [x] **Où pourrait-elle être réutilisée ?** Dans toutes les zones identifiées au scan rétrospectif de 15a (F-69, F-92, F-93, F-94, F-96). Les SF dédiées à chacune (SF-IA-03-16, SF-92-03, SF-93-03, SF-94-03, SF-96-06) consommeront cette directive plutôt que de recréer le pattern.
- [x] **Patterns concurrents ?** Après cette SF, les `[matTooltip]` sur les badges F-IA-03 sont entièrement remplacés. Les tooltips restants (boutons, icônes d'aide) ne sont pas concernés — pattern design-system standard.
- [x] **Service/endpoint réutilisable ?** `SourceExplanationService` (15a) est déjà conçu transversal. Pas de nouveau service.
- [x] **Équivalent design à migrer ?** Oui : le code verbose `cdkConnectedOverlay + overlay template + openPopover/scheduleClose/cancelClose` introduit manuellement dans F-DT-07 par 15a est **refactoré pour utiliser la directive**. F-DT-07 devient le premier consommateur aligné.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-DT-07 Ancienneté (déjà livré en 15a) | Oui | **Refactoré pour utiliser la directive** dans cette SF (déduplication) |
| F-DT-08 Validité licenciement | Oui | **Intégré dans cette SF** (14 critères FR+BE) |
| F-DT-09 Comparateur indemnités | Oui | **Intégré dans cette SF** (3 champs) |
| F-DT-10 Validité rupture conv | Oui | **Intégré dans cette SF** (6 critères FR) |
| F-FA-05 Partage immobilier | Oui | **Intégré dans cette SF** (2 champs) |
| F-FA-06 Calendrier garde | Oui | **Intégré dans cette SF** (1 champ compound) |
| F-FA-07 Checklist divorce | Oui | **Intégré dans cette SF** (2 groupes : étapes + pièces) |
| F-IM-05/06/07 Immigration | Oui | **SF-IA-03-15c** (subfeature parallèle) |
| F-69/F-92/F-93/F-94/F-96 zones connexes | Oui | **Backlog** (scan rétrospectif 15a : SF-IA-03-16 / SF-92-03 / SF-93-03 / SF-94-03 / SF-96-06) |

### Décision

- [x] Étendu aux 6 outils + refactor F-DT-07 dans cette SF
- [x] Subfeature parallèle SF-IA-03-15c pour Immigration
- [x] Backlog VN pour les 5 zones connexes (déjà documenté en 15a)

---

## Critères d'acceptation

- [ ] Nouvelle directive `CoherencePopoverTriggerDirective` (`[appCoherencePopover]`) dans `frontend/src/app/shared/coherence-popover/` qui factorise le pattern `cdkConnectedOverlay + CoherencePopoverComponent + hover/click/Escape`. Inputs : `sourceKey: string`, `reason: string`, `caseFileId: string`, `sourceExplanation: SourceExplanation | null`, `blocker: boolean`.
- [ ] Prompt Haiku de `SourceExplanationGenerator` enrichi : la consigne précise que les sourcekeys peuvent être des codes critères F96 spécifiques (`FR_*`, `BE_*`, `RC_*`, `FA05_*`, `FA06_*`, `DT09_*`) en plus des sourcekeys métier génériques. Le prompt liste les codes attendus avec exemples (pas plus de 30 lignes supplémentaires).
- [ ] `AncienneteSectionComponent` (F-DT-07) refactoré pour utiliser la directive (suppression des 5 `cdkConnectedOverlay` verbeux ajoutés par 15a). Comportement identique, tests existants verts.
- [ ] `LicenciementSectionComponent` (F-DT-08) : les 14 critères FR/BE utilisent la directive. Mapping `code → sourceKey` défini côté TS (sourceKey = code F96 pour ces critères).
- [ ] `IndemniteComparatifSectionComponent` (F-DT-09) : les 3 champs (TYPE_RUPTURE, ANCIENNETE, SALAIRE) utilisent la directive.
- [ ] `RuptureConvSectionComponent` (F-DT-10) : les 6 critères (RC_*) utilisent la directive.
- [ ] `PartageImmobilierSectionComponent` (F-FA-05) : les 2 champs (VALEUR_VENALE, CAPITAL_RESTANT) utilisent la directive.
- [ ] `CalendrierGardeSectionComponent` (F-FA-06) : le champ MODE_GARDE utilise la directive (sourceKey = `FA06_MODE_GARDE`).
- [ ] `DivorceChecklistSectionComponent` (F-FA-07) : les étapes + pièces utilisent la directive. Mapping basé sur le code de l'étape/pièce.
- [ ] Chaque composant charge la map d'explications via `SourceExplanationService.getForCaseFile()` au `ngOnInit`.
- [ ] Isolation workspace : vérifiée côté endpoint (livré par 15a, aucune modification).
- [ ] Tests unitaires frontend : directive testée (hover ouvre, Escape ferme, clic extérieur ferme, lien actionnable).
- [ ] Tests frontend : chaque composant a des tests de non-régression mettant à jour les mocks HTTP (1 call `/source-explanations` supplémentaire).
- [ ] Build backend vert, build frontend vert, 860+ tests backend verts, 972+ specs frontend verts.

---

## Périmètre

### Hors scope (explicite)

- F-IM-05/06/07 Immigration (SF-IA-03-15c).
- F-69 / F-92 / F-93 / F-94 / F-96 (backlog scan rétrospectif 15a).
- Ré-enrichissement de dossiers analysés avant 15a (endpoint `/regenerate-explanations`).
- Amélioration du prompt Haiku au-delà de l'ajout des sourcekeys F96.
- Modification de l'UX du popover (design, taille, position) — identique à 15a.
- Ajout de nouveaux types d'action dans le navigator (6 types couverts en 15a, suffisant).

---

## Valeurs initiales

Aucune (pas de nouvelle entité).

---

## Contraintes de validation

Aucune (réutilise les contraintes de 15a sur `case_analysis_source_explanations`).

---

## Technique

### Endpoint(s)

Aucun nouvel endpoint. Réutilise `GET /api/v1/case-files/{id}/source-explanations` (15a).

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

### Composants Angular

- `CoherencePopoverTriggerDirective` (nouveau, `shared/coherence-popover/coherence-popover-trigger.directive.ts`).
- `AncienneteSectionComponent` — refacto pour utiliser la directive.
- `LicenciementSectionComponent` — intégration directive + mapping.
- `IndemniteComparatifSectionComponent` — idem.
- `RuptureConvSectionComponent` — idem.
- `PartageImmobilierSectionComponent` — idem.
- `CalendrierGardeSectionComponent` — idem.
- `DivorceChecklistSectionComponent` — idem.

### Composants backend

- `SourceExplanationGenerator` — prompt enrichi uniquement.

---

## Plan de test

### Tests unitaires backend

- [ ] `SourceExplanationGeneratorTest` : non-régression, les 7 tests existants passent avec le prompt étendu.

### Tests frontend

- [ ] `CoherencePopoverTriggerDirective` : hover ouvre le popover, Escape ferme, clic extérieur ferme, lien actionnable appelle le navigator, aucune erreur si `caseFileId` absent.
- [ ] `AncienneteSectionComponent` (F-DT-07) : les 43 specs existants passent après refacto directive.
- [ ] `LicenciementSectionComponent` (F-DT-08) : specs existants adaptés pour mocker `/source-explanations`.
- [ ] `IndemniteComparatifSectionComponent` (F-DT-09) : idem.
- [ ] `RuptureConvSectionComponent` (F-DT-10) : idem.
- [ ] `PartageImmobilierSectionComponent` (F-FA-05) : idem.
- [ ] `CalendrierGardeSectionComponent` (F-FA-06) : idem.
- [ ] `DivorceChecklistSectionComponent` (F-FA-07) : idem.

### Isolation workspace

- [x] Non applicable — couverte par 15a sur l'endpoint partagé.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale structurelle** — pas d'auth / workspace / plans / navigation modifiés.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `SourceExplanationGenerator` | Prompt enrichi — risque régression format Haiku | Tests unitaires existants + ajout test avec sourceKey F96 |
| 6 composants Angular | Intégration directive → changement template + un HTTP call supplémentaire | Specs existants adaptés |
| `AncienneteSectionComponent` | Refacto (suppression code cdkConnectedOverlay verbose) | 43 specs existants — non-régression stricte |

### Smoke tests E2E concernés

- [x] Aucun smoke test navigation/workspace touché.

---

## Dépendances

### Subfeatures bloquantes

- `SF-IA-03-15a Done` — infrastructure (endpoint, composant, service, entité).

### Questions ouvertes impactées

- [x] Aucune.

---

## Notes et décisions

- **Pourquoi créer la directive maintenant** (et pas en 15a) : 15a avait 5 badges F-DT-07. Le code verbeux `cdkConnectedOverlay` en template était acceptable. Avec 38 badges sur 7 outils, la directive devient indispensable.
- **Pourquoi refactorer F-DT-07 dans cette SF** : éviter d'avoir deux patterns divergents dans la base (verbeux en 15a, directive ailleurs). Une seule façon de faire.
- **Pourquoi enrichir le prompt Haiku plutôt qu'ajouter un post-processing Java** : Haiku génère déjà les explications à l'analyse. Lui demander d'identifier aussi les critères F96 spécifiques (qui sont dans la synthèse Sonnet via `points_procedure`) est naturel — elle a tout le contexte nécessaire. Post-processing Java serait un duplication.
- **Coût token** : +300-500 tokens Haiku supplémentaires par dossier (~0,1 ¢). Marginal.
- **Mapping local par composant plutôt que global** : chaque outil a son vocabulaire de critères. Le mapping `code → sourceKey` reste proche du composant qui le consomme, évite un fichier de config monolithique difficile à maintenir.
