# Mini-spec — F-155 / SF-155-07 Cleanup divergences résiduelles audit 2026-04-24

## Identifiant

`F-155 / SF-155-07`

## Feature parente

`F-155` — Harmonisation frontend des 6 composants décisionnels (audit post-2026-04-24)

## Statut

`ready`

## Date de création

2026-04-24

## Branche Git

`feat/SF-155-07-cleanup-divergences-residuelles`

---

## Objectif

> Fermer les 4 dernières divergences structurelles (DIV-6 / DIV-7 / DIV-9 / DIV-11) de l'audit F-155 sur les 6 composants décisionnels, et documenter la convention palette rouge (DIV-8) dans le skill.

---

## Comportement attendu

### Cas nominal

- **DIV-6** — `oqtf-avec-delai-section` adopte le composant partagé `DecisionalHeaderFlagComponent` (pattern déjà établi par `oqtf-sans-delai-section` + `annexe13-be-section`). Un flag "URGENT" (variant `warning`) ou "EXPIRÉ" (variant `danger`) s'affiche dans le header dès qu'un `result()` existe avec le statut correspondant.
- **DIV-7** — les 4 composants actuellement sans `SourceExplanationService` (`inaptitude-section`, `heures-sup-section`, `oqtf-avec-delai-section`, `oqtf-sans-delai-section`) injectent le service **`@Optional()`**, déclenchent `loadSourceExplanations()` dans `ngOnInit()`, exposent une méthode `explanationFor(field)` et branchent les popovers `[appCoherencePopover]="explanationFor(...)"` sur chaque badge de cohérence. Fail-open strict (erreur HTTP silencieuse, map vide).
- **DIV-9** — vérification des 6 composants : toutes les dates ISO affichées dans le résultat sont formatées `| date:'dd/MM/yyyy'` (ou `'dd/MM/yyyy HH:mm'` pour `oqtf-sans-delai` — urgence 48h). État actuel : déjà conforme sur tous les composants. Pas de changement de code ; vérification documentée.
- **DIV-11** — `immigration-title-decision-section` : ajoute `@Optional()` devant `private sourceExplanationService: SourceExplanationService` pour cohérence stricte avec le canonique `harcelement-licenciement-nul-section`. Fail-open préservé dans `loadSourceExplanations()` par garde explicite `if (!this.sourceExplanationService) return;`.
- **DIV-8** (doc) — `ai-skills/frontend-coherence-audit.md` §5 Palette statut : documente explicitement la convention "palette rouge = `--danger` unique par défaut, `--danger-medium/-strong/-dark` réservés aux urgences < 72h, commentaire SCSS obligatoire".

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| GET `/source-explanations` → 404 | Map vide, pas de log, pas de popover, pas de crash | 404 (fail-open) |
| GET `/source-explanations` → 500 | Map vide (idem) | 500 (fail-open) |
| `caseFileId` absent au mount | `loadSourceExplanations()` ne fait rien | — |
| `SourceExplanationService` non fourni (contexte de test) | Le composant continue, map vide (DIV-11 : idem sur immigration-title-decision) | — |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : scan terminé — 6 composants F-155 scannés (harcelement, inaptitude, heures-sup, immigration-title-decision, oqtf-avec-delai, oqtf-sans-delai, annexe13-be). Les futurs composants F-DT-12/13/14/16/18/20 + F-IM-09/10 + F-FA seront audités via règle périodique CLAUDE.md "tous les 5 composants".
- [x] **Autres pays** : BE uniquement concerné via `annexe13-be` (déjà conforme) et `oqtf-avec-delai` (FR uniquement gated).
- [x] **Autres domaines** : transversal — le service `SourceExplanationService` sert potentiellement à tous les outils décisionnels, pas de spécificité domaine.
- [x] **Autres UI patterns** : la migration `DecisionalHeaderFlagComponent` sur `oqtf-avec-delai` est symétrique aux 2 autres adoptants. Pas de nouveau pattern introduit.
- [x] **Autres flows transversaux** : aucun — cleanup pur, pas d'auth/workspace/plans/navigation touchés.

### Niveaux de vérification

- [x] **Modèle TypeScript / API exposée** : pas de changement d'API publique.
- [x] **Service** : `SourceExplanationService.getForCaseFile()` inchangé, consommé par 4 nouveaux composants.
- [x] **Template canonique** : `harcelement-licenciement-nul-section` (Optional + explanationFor) respecté.

### Cas spécifique : nouveau pattern UI ou service partagé

**Non applicable** — SF-155-07 ne crée pas de nouveau composant partagé / service / endpoint ; elle harmonise l'usage des composants partagés existants (`DecisionalHeaderFlagComponent`, `SourceExplanationService`, `CoherencePopoverTriggerDirective`) livrés par SF-155-03/04.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `oqtf-avec-delai-section` | Oui (DIV-6 + DIV-7 + DIV-9) | Intégré dans cette SF |
| `oqtf-sans-delai-section` | Oui (DIV-7) | Intégré dans cette SF |
| `inaptitude-section` | Oui (DIV-7) | Intégré dans cette SF |
| `heures-sup-section` | Oui (DIV-7) | Intégré dans cette SF |
| `immigration-title-decision-section` | Oui (DIV-11) | Intégré dans cette SF |
| `harcelement-licenciement-nul-section` | Déjà conforme | Non applicable |
| `annexe13-be-section` | Déjà conforme | Non applicable |
| `ai-skills/frontend-coherence-audit.md` | Oui (DIV-8 doc) | Intégré dans cette SF (non-bloquant) |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (5 composants + 1 doc)

---

## Critères d'acceptation

- [ ] **DIV-6** : `oqtf-avec-delai-section.component.html` importe `<app-decisional-header-flag>` et affiche un flag `URGENT` (variant `warning`) quand `result().statutDelaiRecours === 'URGENT'`, un flag `EXPIRÉ` (variant `danger`) quand `EXPIRE`.
- [ ] **DIV-6** : test spec vérifie le rendu du flag URGENT (au moins 1 test).
- [ ] **DIV-7 × 4 composants** : `SourceExplanationService` injecté `@Optional()`, `sourceExplanations` signal, `loadSourceExplanations()` invoqué dans `ngOnInit()` (fail-open), `explanationFor(field)` mappe chaque field vers une sourceKey documentée, templates HTML branchent `[appCoherencePopover]="explanationFor(...)"` sur chaque badge de cohérence.
- [ ] **DIV-7** : chacun des 4 composants a 2 tests spec : `explanationFor` retourne les explications quand la map est peuplée, retourne `[]` sinon (fail-open).
- [ ] **DIV-9** : scan documentaire — toutes les dates ISO affichées dans les résultats des 6 composants F-155 utilisent `| date:'dd/MM/yyyy'` ou `| date:'dd/MM/yyyy HH:mm'`. Vérification tracée dans la PR.
- [ ] **DIV-11** : `immigration-title-decision-section` décore l'injection `SourceExplanationService` avec `@Optional()` ; la méthode `loadSourceExplanations()` gère le cas `this.sourceExplanationService == null`.
- [ ] **DIV-8** (doc) : `ai-skills/frontend-coherence-audit.md` §5 a un paragraphe "Convention palette rouge" explicitant `--danger` par défaut vs `--danger-medium/-strong/-dark` urgence < 72h.
- [ ] **Non-régression** : tous les tests existants des 5 composants touchés + `immigration-title-decision-section` passent.
- [ ] **Compilation** : `tsc --noEmit -p frontend/tsconfig.app.json` vert.

---

## Périmètre

### Hors scope

- Modification backend (aucune).
- Modification du `decisional-tools-panel.component.ts` et des entrées `TOOL_REGISTRY` (intouchable — 4 F-DT agents en parallèle).
- Création de nouveaux composants décisionnels.
- Ajout de nouveaux champs dans `sourceExplanations` côté backend (les sourceKeys consommés par les 4 composants peuvent rester non-persistés — le fail-open garantit un rendu vide silencieux).
- Migration DB / Liquibase (aucune).
- Refonte SCSS / palette (hors DIV-8 qui est pure doc).

---

## Valeurs initiales

Non applicable — pas d'entité créée.

---

## Contraintes de validation

Non applicable — pas de nouveau champ persistable.

---

## Technique

### Endpoint(s)

Aucun nouvel endpoint. Les 4 composants appellent `GET /api/v1/case-files/{caseFileId}/source-explanations` déjà livré par SF-IA-03-15a.

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable

### Composants Angular

- `oqtf-avec-delai-section.component.{ts,html,spec.ts}` — DIV-6 (flag), DIV-7 (SE), DIV-9 (vérif).
- `oqtf-sans-delai-section.component.{ts,html,spec.ts}` — DIV-7.
- `inaptitude-section.component.{ts,html,spec.ts}` — DIV-7.
- `heures-sup-section.component.{ts,html,spec.ts}` — DIV-7.
- `immigration-title-decision-section.component.ts` — DIV-11 (`@Optional()`).
- `ai-skills/frontend-coherence-audit.md` — DIV-8 (doc).

---

## Plan de test

### Tests unitaires (Jest)

- [ ] **DIV-6 (oqtf-avec-delai)** : fixture avec `result().statutDelaiRecours === 'URGENT'` → flag rendu avec `label="URGENT"`, `variant="warning"`.
- [ ] **DIV-7 × 4 composants** : pour chaque composant, 2 tests :
  - `explanationFor(field)` retourne un tableau non-vide quand `sourceExplanations` est peuplé pour la bonne clé.
  - `explanationFor(field)` retourne `[]` quand la clé n'existe pas (fail-open).
- [ ] **DIV-11** : test vérifiant que `immigration-title-decision-section` ne crash pas quand `SourceExplanationService` est absent (instantiation minimale).

### Tests d'intégration

Non applicable — refactor pur frontend sans nouvelle route.

### Isolation workspace

Non applicable — pas d'accès DB.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — cleanup frontend isolé.

### Composants / endpoints existants potentiellement impactés

Aucun — modifications localisées aux 5 composants listés ; signatures publiques préservées.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné (refactor cosmetic/convention sans impact fonctionnel ou routing).

---

## Dépendances

### Subfeatures bloquantes

- SF-155-03 (`DecisionalHeaderFlagComponent` livré) — **done**.
- SF-155-04 / 05 / 06 (mergées) — **done**.
- SF-IA-03-15a (endpoint `source-explanations`) — **done**.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **DIV-9 — découverte** : après lecture, toutes les dates ISO affichées dans les 6 composants sont déjà pipe-formatées. L'audit périodique citait `{{ result()!.dateExpirationDdv }}` sur oqtf-avec-delai comme brut, mais inspection HTML L153 confirme `| date:'dd/MM/yyyy'` en place. La SF trace ce constat pour archive ; aucun code n'est modifié pour DIV-9.
- **Pas de `[appCoherencePopoverBlocker]` sur oqtf-avec-delai / inaptitude / heures-sup** : ces composants n'avaient pas de popoverBlocker avant, on n'en ajoute pas.
- **`explanationFor` sourceKeys choisies** (convention `<PREFIX>_<FIELD>` upper_case) :
  - `inaptitude-section` : `INAPT_SALAIRE`, `INAPT_ORIGINE`, `INAPT_AVIS_MEDECIN`, `INAPT_RECLASSEMENT`.
  - `heures-sup-section` : `HS_TAUX_HORAIRE`, `HS_HEURES_SUP`, `HS_SALAIRE_DEDUIT`.
  - `oqtf-avec-delai-section` : `IM08_DATE_NOTIFICATION`, `IM08_MOTIF_OQTF`, `IM08_RECOURS_FORME`.
  - `oqtf-sans-delai-section` : `IM08_DATE_HEURE_NOTIFICATION`, `IM08_PLACEMENT_CRA`, `IM08_RECOURS_FORME`, `IM08_MOTIF_SANS_DELAI`.
  Ces clés peuvent ne pas encore être persistées backend — le fail-open garantit un rendu vide sans erreur.
- **DIV-11 ngOnInit** : `loadSourceExplanations()` est déjà gardé par `if (!this.caseFileId) return;` ; on ajoute `if (!this.sourceExplanationService) return;` pour rendre la méthode null-safe (idem pattern canonique).
