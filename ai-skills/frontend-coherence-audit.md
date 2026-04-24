# Skill : frontend-coherence-audit

---

## 1. Nom

`frontend-coherence-audit`

---

## 2. Mission

Auditer la cohérence des composants Angular décisionnels (sections `<app-XXX-section>` intégrées au panel F-IA-04) contre un **template canonique**, identifier les divergences, et produire un rapport actionnable de harmonisation. Prévenir la **dette de convergence** entre composants similaires livrés en parallèle par agents autonomes.

---

## 3. Quand utiliser ce skill

- **Préventif** : à chaque nouveau composant décisionnel frontend, appeler le scan pré-implémentation (check template + checklist).
- **Périodique** : tous les 5 nouveaux composants décisionnels, exécuter l'audit complet (règle gouvernance CLAUDE.md).
- **Correctif** : lors d'une SF d'harmonisation rétroactive (ex. F-155).
- **Signal terrain** : quand un utilisateur (avocat, PM) signale une incohérence visuelle entre 2 composants similaires.

---

## 4. Quand ne pas utiliser ce skill

- Pour un composant non-décisionnel (shell, navigation, listes génériques) — ce skill cible spécifiquement les outils décisionnels avec pattern `section consommant un endpoint POST/GET + intégration TOOL_REGISTRY`.
- Pour valider la logique métier d'un composant — utiliser `test-case-generator` + review code standard.
- Pour créer un nouveau composant from scratch — utiliser le skill `document-skills:frontend-design` global ou copier directement le template canonique.

---

## 5. Template canonique (référence 2026-04-24)

Le composant de référence canonique actuel est **`harcelement-licenciement-nul-section`** (F-DT-11-02, premier livré du batch parallélisé). Il définit la structure attendue :

### Fichiers

```
frontend/src/app/core/models/XXX.model.ts
frontend/src/app/core/services/XXX.service.ts
frontend/src/app/case-files/XXX-section/
  ├── XXX-section.component.ts
  ├── XXX-section.component.html
  ├── XXX-section.component.scss
  └── XXX-section.component.spec.ts
```

### Composant (XXX-section.component.ts)

- **Standalone** : `standalone: true`
- **Inputs** standards :
  - `@Input() caseFileId: string` (required)
  - `@Input() workspaceCountry: 'FRANCE' | 'BELGIQUE'` (default 'FRANCE' si applicable)
- **Signals** pour l'état : `collapsed`, `loading`, `resolving`, `result`, form fields
- **Service injecté** : `XXXService` + `CaseDashboardRefreshService` (optional)
- **MatSnackBar** injecté pour erreurs
- **Refresh dashboard** obligatoire : `this.refreshService?.triggerRefresh()` dans `next:` du POST
- **Imports modules** : `FormsModule`, `MatButtonModule`, `MatIconModule`, `MatFormFieldModule`, `MatInputModule`, `MatProgressSpinnerModule`, `MatSelectModule` (si enum), `MatSlideToggleModule` (si booléens), `CommonModule`, `DecimalPipe` (si montants)

### Gate `workspaceCountry` (si outil country-specific)

Convention : **afficher une bannière d'information** si `workspaceCountry !== paysAttendu`, **ne jamais masquer silencieusement le form**. Exemple :

```html
<div *ngIf="workspaceCountry !== 'FRANCE'" class="country-banner">
  Cet outil s'applique à la France uniquement. 
  Pour la Belgique, voir l'outil XXX-BE (SF-YY-ZZ).
</div>
<form *ngIf="workspaceCountry === 'FRANCE'" ...>
```

### Datepicker

Convention projet : **`<input type="date">`** natif pour les dates simples, **`<input type="datetime-local">`** pour les précisions horaires (ex. F-IM-08 OQTF sans délai 48h). **Pas de MatDatepicker** (non utilisé dans le codebase — pattern anciennete-section établi).

### Palette statut (bannières colorées)

**Palette standard** (ordre croissant urgence) :
- `DISPONIBLE` / succès → **navy clair** (fond primary-50) + icône `info_outline`
- `URGENT` (≤ 7 jours typ.) → **or soutenu** (accent-500) + icône `warning`
- `EXPIRE` / alerte critique → **rouge** (error-500) + icône `error`
- `RECOURS_FORME` / action prise → **vert** (success-500) + icône `check_circle`

**Exception rouge dominant** (palette rouge pour DISPONIBLE et URGENT) : **réservée aux urgences absolues < 72h** (ex. F-IM-08 SF-04 OQTF sans délai 48h JLD). Documenter l'exception dans le composant + message explicitant pourquoi.

**Convention nommage palette rouge** (SF-155-07 / DIV-8) :
- **Par défaut** : un seul modificateur `--danger` (ex. `oqtf-banner--danger`, `annexe13-banner--danger`). Utilisé pour `EXPIRE` uniquement dans les outils à urgence standard.
- **Exception urgence < 72h** : gradation `--danger-medium` / `--danger-strong` / `--danger-dark` autorisée pour représenter l'escalade d'urgence quand tous les statuts sont déjà en zone critique (DISPONIBLE inclus). Seul cas légitime à ce jour : `oqtf-sans-delai-section` (F-IM-08 SF-04, 48h JLD).
- **Règle d'usage** : tout composant adoptant la gradation `--danger-medium/-strong/-dark` DOIT commenter en SCSS la justification (ex. `// Urgence absolue 48h — palette rouge dominante autorisée par DESIGN_SYSTEM.md`) ET inclure une mention dans le commentaire de classe `@Component` (ou dans `bannerClass()`).
- **Revue** : tout nouveau composant décisionnel qui introduit une gradation rouge sans justification documentée est marqué FAIL dans l'audit.

### Typographie

- **Inter** pour tout le texte courant (inclus labels, messages)
- **JetBrains Mono** obligatoirement pour :
  - `baseJuridique` (ex. "Art. L.1235-3-1 Code du travail")
  - `formule` (ex. "6 mois × 2 500,00 € = 15 000,00 €")
  - Références d'articles, codes enum, dates ISO dans les aides

### Messages

- Liste `<ul>` avec puces
- Chaque message cite la référence juridique (article CESEDA/Code travail/Loi) en `<code>` JetBrains Mono
- Pas d'emoji dans les messages (sauf badges spécifiques type "⚠ TRANSFERT IMMINENT")

### Pré-remplissage IA + validation IA au changement (RÈGLE FONDAMENTALE)

Tout outil décisionnel frontend **DOIT** implémenter 2 mécanismes :

#### A. Pré-remplissage IA après analyse de dossier

Dès qu'une **analyse de dossier** ou une **synthèse enrichie** est produite (contenu `aiData`, `synthesis`, `caseAnalysisResult`), le composant doit :
- Accepter un `@Input() aiData?: CaseAnalysisResult | ImmigrationExtractedData | ... | null` (type spécifique au domaine)
- Méthode privée `prefillFromAi()` invoquée dans `ngOnInit()` ET `ngOnChanges()` (si `aiData` change avant première résolution)
- Signal `provenance<Champ> = signal<'IA' | null>(null)` par champ pré-rempli (un par field clé)
- Badge UI "Pré-rempli depuis l'analyse" à côté de chaque champ avec provenance IA (icône `auto_awesome` ou équivalent)
- Au changement manuel du champ par l'avocat (`onXxxChange()`), le signal provenance est remis à `null` (effacement badge IA)

**Pattern de référence** : `frontend/src/app/case-files/immigration-title-decision-section/immigration-title-decision-section.component.ts` (méthode `prefillFromAi()` + signals `provenanceMotif`, `provenanceSituationFamiliale`, `provenanceNationaliteUe` + handlers `onXxxChange()`).

#### B. Validation IA / alertes cohérence F-IA-03 au changement

Pour chaque champ clé du form, si l'IA a produit :
- une valeur dans l'analyse du dossier (`aiData`),
- un résultat de checklist procédurale F-96 (`@Input() procedureChecks: ProcedureCheck[]`),
- une réponse à une question complémentaire (`@Input() aiQuestions: AiQuestion[]`),
- une pièce manquante (`@Input() piecesManquantes: PieceManquanteEntry[]`),

Et que la valeur saisie/affichée par l'avocat **diverge** de cette valeur IA, le composant doit afficher une **alerte de cohérence** inline (pattern F-IA-03 — `CoherencePopoverTriggerDirective` + popover expliquant la divergence avec source).

**Computed signal** `coherenceAlerts = computed<Partial<Record<FieldName, CoherenceAlert>>>()` produit les alertes dynamiquement.

**Pattern de référence** : `immigration-title-decision-section.component.ts` — méthodes `buildMotifAlert()`, `buildNationaliteAlert()` + `coherenceAlerts` computed + `alertsSummary` computed + `<app-coherence-popover-trigger>` dans le template.

#### Rationale

Cette règle est **fondamentale** car :
1. L'avocat perd du temps à ressaisir ce que l'IA a déjà extrait
2. L'avocat peut saisir une valeur contradictoire avec l'analyse IA sans le voir
3. Sans pré-fill IA, l'outil décisionnel devient "encore un formulaire" au lieu d'un assistant
4. Sans alertes cohérence F-IA-03, l'IA ne "parle pas" à l'outil — chaque outil vit en silo

**Sans ces 2 mécanismes, l'outil est marqué FAIL dans l'audit** (pas juste WARN).

### Intégration TOOL_REGISTRY

Entrée symétrique dans `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` :

```typescript
import { XXXSectionComponent } from '../XXX-section/XXX-section.component';

// dans TOOL_REGISTRY:
['F-XX-YY-tool-id', {
  component: XXXSectionComponent,
  inputs: (ctx) => ({
    caseFileId: ctx.caseFileId,
    workspaceCountry: ctx.workspaceCountry,  // si country-gated
    // autres inputs contextuels si besoin
  }),
}],
```

Ordre : aligner avec le `priority` de la règle `decision_tool_visibility_rules` backend.

---

## 6. Checklist cohérence (à scanner à chaque nouveau composant)

| Check | OK / KO | Notes |
|---|---|---|
| Composant standalone, pas de NgModule | | |
| `@Input() caseFileId: string` déclaré required | | |
| `@Input() workspaceCountry` déclaré si country-gated | | |
| Imports Material complets (pas d'imports manquants) | | |
| `CaseDashboardRefreshService` injecté `@Optional()` | | |
| `triggerRefresh()` appelé après POST succès | | |
| `MatSnackBar` pour erreurs HTTP (pas window.alert/confirm) | | |
| Datepicker convention : `<input type="date">` ou `datetime-local` | | |
| Gate `workspaceCountry` : bannière info si mismatch (pas masquage silencieux) | | |
| Palette statut : navy/or/rouge/vert selon sémantique | | |
| Palette rouge dominant UNIQUEMENT si urgence < 72h documentée | | |
| JetBrains Mono pour `baseJuridique` + `formule` | | |
| Inter pour le reste | | |
| Messages citent articles juridiques (code) en `<code>` | | |
| Entrée `TOOL_REGISTRY` alignée (inputs, priority cohérent) | | |
| Spec test : au moins 10 tests couvrant mount + form valid + POST + erreur | | |
| Pas de couleur hors palette `DESIGN_SYSTEM.md` | | |
| Pas d'emoji hors badges spécifiques | | |
| **Pré-fill IA** : `@Input() aiData`/`synthesis`, méthode `prefillFromAi()`, signals `provenance<Field>`, badges UI "Pré-rempli depuis l'analyse" (FAIL si absent) | | |
| **Validation IA F-IA-03** : `coherenceAlerts` computed + `CoherencePopoverTriggerDirective` sur chaque field clé (FAIL si absent) | | |
| `ngOnChanges()` re-invoque `prefillFromAi()` quand `aiData` change (avant première résolution) | | |
| Effacement badge provenance IA au `onXxxChange()` manuel avocat | | |

---

## 7. Inputs attendus

```
- Liste des composants à auditer (chemins absolus ou pattern glob)
- Template canonique de référence (par défaut : harcelement-licenciement-nul-section)
- Mode : PREVENTIF (1 composant pré-merge) / PERIODIQUE (tous les composants existants) / CORRECTIF (après F-155)
```

---

## 8. Outputs attendus

### Format du rapport

```markdown
# Audit cohérence frontend — {DATE}

## Composants audités
- `XXX-section` (F-DT-11-02) — template canonique
- `YYY-section` (F-DT-15-02)
- ...

## Checklist par composant

| Composant | Standalone | Inputs | Refresh | Datepicker | Gate country | Palette | Typo | TOOL_REGISTRY | Verdict |
|---|---|---|---|---|---|---|---|---|---|
| XXX | ✅ | ✅ | ✅ | date | ✅ | standard | ✅ | ✅ | PASS |
| YYY | ✅ | ✅ | ❓ | date | ⚠️ masque | standard | ✅ | ✅ | MINEUR |
| ZZZ | ✅ | ✅ | ✅ | datetime-local | ✅ | rouge dominant (justifié 48h) | ✅ | ✅ | PASS |

## Divergences identifiées

### D1 — [Catégorie] : [Description]
**Composants concernés** : ..., ..., ...
**Impact** : [visuel / comportemental / accessibilité]
**Reco harmonisation** : ...
**Estimation migration** : ~X heures

### D2 — ...

## Plan de harmonisation

1. **Priorité haute** : divergences comportementales (ex. refresh manquant)
2. **Priorité moyenne** : divergences visuelles (palette, typo)
3. **Priorité basse** : cosmétique (ordre imports, commentaires)

## PR consolidées suggérées

- PR harmonisation 1 : [composants] - [change type]
- PR harmonisation 2 : ...

## Template canonique à jour

Recommandation : mise à jour du template `harcelement-licenciement-nul-section` à NNN ligne(s) pour refléter conventions actuelles.
```

---

## 9. Méthodologie étape par étape

1. **Identifier le template canonique** — par défaut `harcelement-licenciement-nul-section`, à actualiser si un nouveau composant pose une convention meilleure
2. **Lister les composants à auditer** — glob `frontend/src/app/case-files/*-section/`
3. **Parser chaque composant** (`.ts`, `.html`, `.scss`) pour extraire :
   - Imports modules
   - Inputs déclarés
   - Services injectés
   - Template fields (datepicker type, bannière country, couleurs bannière statut)
   - Appels `triggerRefresh()`, `MatSnackBar`
4. **Comparer à la checklist** (§6)
5. **Produire le rapport** format §8
6. **Proposer PR harmonisées** regroupées par type de divergence (pas 1 PR par composant = noise)
7. **Si CORRECTIF** : créer les SFs techniques dans `docs/features/F-155/` avec contrat de refactor

---

## 10. Limites

- **Pas un validateur métier** : ne vérifie pas que les formules de calcul sont correctes, seulement la forme.
- **Pas un testeur runtime** : ne lance pas les composants dans le navigateur.
- **Pas une migration auto** : produit un plan, pas un commit.
- **Template canonique évolue** : à mettre à jour quand un nouveau composant innovant établit une meilleure convention.

---

## 11. Interactions avec la gouvernance

- **Règle CLAUDE.md** "Nouveau composant Angular décisionnel frontend sans scan de cohérence → REFUS" invoque ce skill.
- **Règle périodique** : tous les 5 nouveaux composants, lancer ce skill en mode PERIODIQUE et produire un rapport dans `docs/features/F-155/audit-YYYY-MM-DD.md`.
- **Prérequis pré-merge** : chaque SF frontend décisionnelle doit référencer le template canonique dans sa mini-spec (section "Pattern de référence").

---

## 12. Exemples d'invocation

### Mode préventif (avant merge SF-XX-02 frontend)
```
Invoke frontend-coherence-audit en mode PREVENTIF sur
frontend/src/app/case-files/XXX-section/
```
→ Retourne un checklist PASS/WARN/FAIL + recommandations avant push.

### Mode périodique (audit trimestriel ou après batch 5 composants)
```
Invoke frontend-coherence-audit en mode PERIODIQUE sur
frontend/src/app/case-files/*-section/
```
→ Produit un rapport complet + plan de harmonisation + PRs suggérées.

### Mode correctif (dans le cadre de F-155)
```
Invoke frontend-coherence-audit en mode CORRECTIF
cible = ensemble des 6 composants livrés 2026-04-24
```
→ Produit le rapport + les mini-specs SF-155-XX de harmonisation.
