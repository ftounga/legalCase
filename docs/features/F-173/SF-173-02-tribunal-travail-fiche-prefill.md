# Mini-spec — F-173 / SF-173-02 — Frontend : pré-fill IA sur fiche tribunal du travail (F-DT-06, BE)

## Identifiant

`F-173 / SF-173-02`

## Feature parente

`F-173` — Pré-fill IA sur les 2 fiches procédurales legacy (F-DT-04 prud'homale FR + F-DT-06 tribunal du travail BE)

## Statut

`draft`

## Date de création

2026-04-30

## Branche Git

`feat/SF-173-bundle-fiches-procedurales-prefill` (bundlée avec SF-173-01 — voir Notes)

---

## Objectif

Brancher le **pré-remplissage IA** sur le composant `tribunal-travail-fiche-section` (outil ALWAYS_ON Travail BE priorité 20, équivalent BE de F-DT-04) qui aujourd'hui présente un formulaire vide alors que `TravailExtractedData` contient les valeurs extraites par l'IA. Symétrique de SF-173-01 sur le côté Belgique.

---

## Comportement attendu

### Cas nominal

Identique à SF-173-01 (FR) — l'avocat ouvre la fiche tribunal du travail BE, le formulaire est pré-rempli depuis `TravailExtractedData`, badges `auto_awesome` à côté de chaque champ pré-rempli, validation F-IA-03 sur les fields critiques.

### Cas d'erreur

Identique à SF-173-01 (workspace mismatch FR vs BE est géré au niveau de l'affichage du composant via la couche de visibilité `decision_tool_visibility_rules`).

---

## Contrat

Identique à SF-173-01. `TravailExtractedData` couvre FR + BE (champs `salaireBrutMensuel`, `dateEmbauche`, etc. identiques sémantiquement).

---

## Analyse de cohérence transversale

### Périmètres scannés

- **Composant frère** : `prudhome-fiche-section` (FR) — même problème, fix symétrique. Couvert par SF-173-01.
- **Pattern de référence** : `immigration-title-decision-section` (template canonique F-155) — appliqué strict identique.
- **Spécificité BE** : la convention collective FR (`conventionCollective`) devient `commissionParitaire` côté BE — utiliser le bon champ. Sinon les champs sont équivalents (nom, dates, salaire, motif).
- **Gate workspaceCountry** : déjà géré par la couche visibilité — F-DT-06 n'apparaît que pour les workspaces BE. Pas de bannière info nécessaire dans cette SF.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `tribunal-travail-fiche-section` | Oui | Modifié dans cette SF |
| TOOL_REGISTRY entrée `F-DT-06-requete-tribunal-travail` | Oui | Mise à jour dans cette SF |
| Validation F-IA-03 | Oui | Intégrée dans cette SF |

### Décision

- [x] Étendu à la cible directe + SF-01 bundlée pour le FR
- [x] Validation F-IA-03 intégrée

---

## Impact par domaine métier

**Travail Belgique uniquement.** L'équivalent FR (F-DT-04 fiche prud'homale) est traité par SF-173-01 (bundlée).

---

## Critères d'acceptation

- [ ] **C1** — `tribunal-travail-fiche-section.component.ts` ajoute `@Input() aiData?: TravailExtractedData | null`
- [ ] **C2** — Méthode privée `prefillFromAi()` invoquée dans `ngOnInit()` ET `ngOnChanges()`
- [ ] **C3** — Signals `provenance<Field>` par champ pré-rempli
- [ ] **C4** — Badge `auto_awesome` "Pré-rempli depuis l'analyse" affiché
- [ ] **C5** — Handler de changement remet `provenance<Field>` à null
- [ ] **C6** — Validation F-IA-03 sur fields critiques (dateEmbauche, salaireBrutMensuel, motifLicenciement) avec helper `CoherenceAlertBuilder`
- [ ] **C7** — Directive `<app-coherence-popover-trigger>` câblée
- [ ] **C8** — TOOL_REGISTRY entrée `F-DT-06-requete-tribunal-travail` mise à jour avec `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`
- [ ] **C9** — Tests Jest : symétriques de SF-173-01 adaptés au composant BE
- [ ] **C10** — Aucune régression

---

## Périmètre

### Hors scope

- Refonte du FormGroup
- Backend (champs déjà présents)
- Composant `prudhome-fiche-section` — couvert par SF-173-01

---

## Technique

### Fichiers modifiés

- `frontend/src/app/case-files/tribunal-travail-fiche-section/tribunal-travail-fiche-section.component.ts`
- `frontend/src/app/case-files/tribunal-travail-fiche-section/tribunal-travail-fiche-section.component.html`
- `frontend/src/app/case-files/tribunal-travail-fiche-section/tribunal-travail-fiche-section.component.spec.ts`
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` (TOOL_REGISTRY entrée F-DT-06)

### Pattern de référence

Identique à SF-173-01 — `immigration-title-decision-section`.

---

## Plan de test

### Tests unitaires Jest

Symétriques de SF-173-01 adaptés au composant BE. 6+ scénarios.

### Tests d'intégration

Non applicable.

### Isolation workspace

Non applicable.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] Aucune préoccupation transversale

### Smoke tests E2E

Aucun.

---

## Dépendances

### Subfeatures bloquantes

Aucune. Bundlée avec SF-173-01 dans la même PR.

---

## Notes et décisions

- **Décision** : bundle avec SF-173-01 dans une PR unique pour éviter le conflit immédiat sur `decisional-tools-panel.component.ts` (TOOL_REGISTRY).
- **Note** : la spécificité BE (commissionParitaire vs conventionCollective) sera vérifiée en lecture du composant existant pour utiliser les bons champs `TravailExtractedData`.
