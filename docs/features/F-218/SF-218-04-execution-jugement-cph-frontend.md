# Mini-spec — F-218 / SF-218-04 — Exécution du jugement CPH (AGS) — frontend

## Identifiant

`F-218 / SF-218-04`

## Feature parente

`F-218a` — Procédure CPH avancée (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-04-execution-jugement-cph-frontend`

---

## Objectif

Livrer le composant Angular `<app-execution-jugement-cph-section>` pour `F-DT-88-execution-jugement-cph`, affichant la checklist d'exécution forcée, le verdict d'orientation (exécution directe vs relais AGS) et les plafonds AGS quand l'employeur est en procédure collective.

---

## Comportement attendu

- Formulaire : `dateJugement` (`<input type="date">`), `montantCondamnation` (number, pré-rempli), `executionProvisoireOrdonnee` (checkbox), `situationEmployeur` (select IN_BONIS/REDRESSEMENT/LIQUIDATION), `dateOuvertureProcedureCollective` (`<input type="date">`, affiché si REDRESSEMENT/LIQUIDATION), `creancesSuperPrivilegiees` (number optionnel).
- Résultat : badge verdict (`EXECUTION_DIRECTE` vert / `RELAIS_AGS` navy / `BLOQUE_INFO_MANQUANTE` or), bloc AGS (plafonds calculés en JetBrains Mono, mention « barème à actualiser annuellement »), checklist exécution (items obligatoires/bloquants), `baseJuridique` (JetBrains Mono).
- CONTEXTUAL : apparaît si flag IA `execution_jugement_cph_envisagee` = true. Groupement thématique `CONTENTIEUX`.
- Pré-fill : `montantCondamnation`, `situationEmployeur`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge non utilisé hors alerte critique ; navy pour `RELAIS_AGS` ; `<input type="date">` ; JetBrains Mono plafonds/baseJuridique ; bannière gate FR ; MatSnackBar)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData`, `prefillFromAi()` ngOnInit+ngOnChanges, signals `provenanceMontant` / `provenanceSituationEmployeur` + badges `auto_awesome` + handlers
- [x] Validation F-IA-03 : `coherenceAlerts` computed sur `situationEmployeur` / `montantCondamnation` croisé aiData + procedureChecks ; `CoherenceAlertBuilder` partagé
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-88-execution-jugement-cph` dans `KNOWN_FRONTEND_TOOL_IDS`
- Niveau outil : 1–4 (checklist + détecteur AGS) → parité domaines **non applicable** (AGS = mécanisme FR Code travail, sans équivalent immigration/famille)

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript
- [ ] `situationEmployeur=LIQUIDATION` → bloc AGS visible avec plafonds + mention barème annuel
- [ ] `situationEmployeur=IN_BONIS` → verdict `EXECUTION_DIRECTE`, bloc AGS masqué
- [ ] `dateOuvertureProcedureCollective` requis affiché dynamiquement quand procédure collective sélectionnée
- [ ] `montantCondamnation` pré-rempli depuis `TravailExtractedData` avec badge provenance
- [ ] Tests Jest ≥ 12 (verdicts, affichage conditionnel AGS, pré-fill, getPrefillCount 0/partiel/nominal, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `ExecutionJugementCphSectionComponent`
- **Nouveau service** `ExecutionJugementCphService`
- **Nouveau modèle** `ExecutionJugementCphAnalysis`
- **Modification** `decisional-tools-panel.component.ts`

## Dépendances

- SF-218-03 : statut `done`

## Hors périmètre

- Génération de la déclaration de créance AGS / CGEA
