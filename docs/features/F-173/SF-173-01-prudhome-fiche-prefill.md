# Mini-spec — F-173 / SF-173-01 — Frontend : pré-fill IA sur fiche prud'homale (F-DT-04, FR)

## Identifiant

`F-173 / SF-173-01`

## Feature parente

`F-173` — Pré-fill IA sur les 2 fiches procédurales legacy (F-DT-04 prud'homale FR + F-DT-06 tribunal du travail BE)

## Statut

`draft`

## Date de création

2026-04-30

## Branche Git

`feat/SF-173-bundle-fiches-procedurales-prefill` (bundlée avec SF-173-02 — voir Notes)

---

## Objectif

Brancher le **pré-remplissage IA** sur le composant `prudhome-fiche-section` (outil ALWAYS_ON Travail FR priorité 20, visible sur tous les dossiers travail FR) qui aujourd'hui présente un formulaire vide alors que `TravailExtractedData` contient déjà nom, dates, employeur, salaire, motif extraits par l'IA.

---

## Comportement attendu

### Cas nominal

1. L'avocat a uploadé un dossier travail FR avec contrat + bulletins + lettre de licenciement.
2. Le pipeline IA extrait `TravailExtractedData` (nomSalarie, dateEmbauche, dateRupture, salaireBrutMensuel, motifLicenciement, conventionCollective, etc.)
3. L'avocat ouvre la fiche prud'homale (`<app-prudhome-fiche-section>`).
4. **Le formulaire est désormais pré-rempli** avec les valeurs extraites par l'IA. Chaque champ pré-rempli affiche un badge `auto_awesome` "Pré-rempli depuis l'analyse" à côté.
5. L'avocat peut éditer un champ ; le badge disparaît pour ce champ uniquement (signal `provenance<Field>` revient à null).
6. Si une nouvelle analyse arrive (re-analyse), `ngOnChanges` déclenche `prefillFromAi()` à nouveau pour les champs encore en `provenance = 'IA'` (ne pas écraser les saisies manuelles de l'avocat).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `aiData` null (dossier sans analyse) | Pas de pré-fill, formulaire vide (comportement actuel) |
| Champ extrait null (l'IA n'a pas trouvé) | Champ formulaire reste vide, pas de badge |
| Champ avec valeur invalide (date mal formattée) | Skip silencieux + console.warn (fail-open) |
| Avocat saisit puis re-analyse arrive | Saisies préservées (pas écrasées) |

---

## Contrat (existant côté backend)

`TravailExtractedData` est livré dans `synthesis.travailExtractedData` (record 23 args post-PR #518). Champs pertinents pour la fiche prud'homale (à confirmer en lecture du composant) : `nomSalarie`, `prenomSalarie`, `dateEmbauche`, `dateRupture`, `salaireBrutMensuel`, `motifLicenciement`, `conventionCollective`, `nomEmployeur`, `adresseEmployeur`, etc.

Aucun changement backend nécessaire.

---

## Analyse de cohérence transversale

### Périmètres scannés

- **Composant frère** : `tribunal-travail-fiche-section` (équivalent BE F-DT-06) — même problème, même fix. Couvert par SF-173-02 dans la même PR (bundlées car les deux modifient `decisional-tools-panel.component.ts` TOOL_REGISTRY).
- **Pattern de référence** : `immigration-title-decision-section` (template canonique F-155) — `prefillFromAi()`, signals `provenance<Field>`, badges `auto_awesome`, handler `onXxxChange()`.
- **Autres composants legacy sans pré-fill** : audit 2026-04-29 a identifié 9 composants sans `prefillFromAi`, dont 7 sont display-only (légitimes). Les 2 vrais oublis = F-DT-04 + F-DT-06 traités par cette feature.
- **F-IA-03 cohérence** : applicable sur les fields critiques (dateEmbauche, salaireBrutMensuel, motifLicenciement). À ajouter dans cette SF avec le helper partagé `CoherenceAlertBuilder`.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `prudhome-fiche-section` | Oui | Modifié dans cette SF |
| `tribunal-travail-fiche-section` (BE) | Oui | Couvert par SF-173-02 (bundlée) |
| TOOL_REGISTRY entrée `F-DT-04-fiche-prudhomale` | Oui | Mise à jour dans cette SF |
| Validation F-IA-03 | Oui | Intégrée dans cette SF |
| Composant partagé `<app-future-event-badge>` ou similaire | Non | Pattern badge `auto_awesome` déjà standard dans le projet |

### Décision

- [x] Étendu à la cible directe + SF-02 bundlée pour le BE
- [x] Validation F-IA-03 intégrée
- [x] Pattern canonique F-155 appliqué strict

---

## Impact par domaine métier

**Travail France uniquement.** L'équivalent Belgique (F-DT-06 requête tribunal du travail) est traité par SF-173-02 (bundlée). Immigration / Famille : non concernés (leurs fiches procédurales sont déjà pré-remplies via F-IA-01 ou n'existent pas).

---

## Critères d'acceptation

- [ ] **C1** — `prudhome-fiche-section.component.ts` ajoute `@Input() aiData?: TravailExtractedData | null`
- [ ] **C2** — Méthode privée `prefillFromAi()` invoquée dans `ngOnInit()` ET `ngOnChanges()` (pour re-analyse)
- [ ] **C3** — Pour chaque field pré-rempli (nom, prénom, dates, salaire, motif, convention, employeur), un signal `provenance<Field> = signal<'IA' | null>(null)` est posé
- [ ] **C4** — Badge `<mat-icon>auto_awesome</mat-icon>` "Pré-rempli depuis l'analyse" affiché à côté de chaque champ avec `provenance<Field>() === 'IA'`
- [ ] **C5** — Handler `onXxxChange()` (ou équivalent via formControl `valueChanges`) qui remet `provenance<Field>` à null au changement manuel de l'avocat
- [ ] **C6** — Validation F-IA-03 : `coherenceAlerts = computed<...>()` utilisant le helper partagé `CoherenceAlertBuilder` sur les fields critiques (dateEmbauche, salaireBrutMensuel, motifLicenciement). Source `MULTI` si convergence aiData + aiQuestions + procedureChecks
- [ ] **C7** — Directive `<app-coherence-popover-trigger>` câblée sur les fields concernés
- [ ] **C8** — TOOL_REGISTRY entrée `F-DT-04-fiche-prudhomale` mise à jour : ajouter `aiData: ctx.synthesis?.travailExtractedData`, `procedureChecks: ctx.procedureChecks`, `aiQuestions: ctx.aiQuestions`, `piecesManquantes: ctx.synthesis?.piecesManquantes` dans les inputs
- [ ] **C9** — Tests Jest : prefill sans aiData (formulaire vide), prefill avec aiData complet (tous champs remplis + badges), prefill partiel (certains champs null), changement manuel (badge disparaît), re-analyse (nouveaux IA non écrasent saisies manuelles), validation F-IA-03 (alerte sur divergence)
- [ ] **C10** — Aucune régression sur les autres tests Jest

---

## Périmètre

### Hors scope

- Refonte du FormGroup ou des validators (gardés tels quels)
- Backend (champs déjà présents dans `TravailExtractedData`)
- Pré-fill sur les 7 autres composants display-only
- Composant `tribunal-travail-fiche-section` — couvert par SF-173-02

---

## Technique

### Fichiers modifiés

- `frontend/src/app/case-files/prudhome-fiche-section/prudhome-fiche-section.component.ts`
- `frontend/src/app/case-files/prudhome-fiche-section/prudhome-fiche-section.component.html`
- `frontend/src/app/case-files/prudhome-fiche-section/prudhome-fiche-section.component.spec.ts`
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` (TOOL_REGISTRY entrée F-DT-04)

### Pattern de référence

`frontend/src/app/case-files/immigration-title-decision-section/immigration-title-decision-section.component.ts` — `prefillFromAi()` + signals `provenance<Field>` + handlers + `coherenceAlerts` computed + helper `CoherenceAlertBuilder`.

### Helper partagé

`frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts` (existant — F-IA-03).

---

## Plan de test

### Tests unitaires Jest

Voir C9 — 6+ scénarios couvrant prefill sans/avec/partiel aiData, changement manuel, re-analyse, validation F-IA-03.

### Tests d'intégration

Non applicable — pure presentation logic.

### Isolation workspace

Non applicable — frontend.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non
- [ ] Workspace context — non
- [ ] Plans / limites — non
- [ ] Navigation / routing — non
- [x] Aucune préoccupation transversale

### Smoke tests E2E

Aucun smoke test ne teste la fiche prud'homale. Pas de régression attendue.

---

## Dépendances

### Subfeatures bloquantes

Aucune. Bundlée avec SF-173-02 dans la même PR (mêmes fichiers `TOOL_REGISTRY`).

---

## Notes et décisions

- **Décision** : SF-173-01 et SF-173-02 sont **bundlées dans 1 seule PR** car les deux modifient `decisional-tools-panel.component.ts` (TOOL_REGISTRY). Branche unique `feat/SF-173-bundle-fiches-procedurales-prefill`. Permet d'éviter un conflit immédiat sur ce fichier.
- **Décision** : on garde le FormGroup et validators existants. La SF ne touche pas la logique métier, seulement le pré-fill et la validation F-IA-03 visuelle.
- **Note** : pattern canonique F-155 appliqué strict — c'est l'occasion de remettre ce composant legacy aux standards du projet.
