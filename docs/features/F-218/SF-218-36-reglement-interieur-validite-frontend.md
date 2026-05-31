# Mini-spec — F-218 / SF-218-36 — Règlement intérieur : validité (contenu, consultation, dépôt) — frontend

## Identifiant

`F-218 / SF-218-36`

## Feature parente

`F-218c` — IRP / négociation collective FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-31

## Branche Git

`feat/SF-218-36-reglement-interieur-validite-frontend`

---

## Objectif

Livrer le composant Angular `<app-reglement-interieur-validite-section>` pour `F-DT-100-reglement-interieur-validite` : saisie du contenu (obligatoire / interdit) et de la procédure de mise en place du règlement intérieur, affichage des checklists, du verdict de conformité et de l'opposabilité aux salariés.

---

## Comportement attendu

- Formulaire : `effectif` (number, pré-rempli), `reglementExiste` (checkbox, pré-rempli), `contenuHygieneSecurite` (checkbox), `contenuDiscipline` (checkbox), `contenuDroitsDefense` (checkbox), `contenuHarcelementAgissements` (checkbox), `clauseAtteinteLibertesNonJustifiee` (checkbox), `clauseSanctionPecuniaire` (checkbox), `consultationCseRealisee` (checkbox), `transmissionInspectionTravail` (checkbox), `depotGreffeCph` (checkbox).
- Résultat :
  - Si `NON_REQUIS` : encart d'information (effectif < 50, RI facultatif).
  - Trois checklists groupées : contenu obligatoire (4 items), clauses interdites (2 items, conforme = absence), procédure (3 items) — coche verte / croix rouge + badges `OBLIGATOIRE` / `INTERDIT` / `PROCEDURE`.
  - Compteurs `itemsObligatoiresManquants` et `clausesInterditesPresentes` (JetBrains Mono).
  - Badge `statut` : `CONFORME` vert / `NON_CONFORME` orange / `INOPPOSABLE` rouge / `NON_REQUIS` gris.
  - Badge `opposabilite` (`OPPOSABLE` vert / `INOPPOSABLE` rouge) + note explicative (procédure de mise en place).
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
- CONTEXTUAL : apparaît si flag IA `reglement_interieur_detecte` = true. Groupement thématique cohérent avec les outils de conformité employeur / IRP.
- Pré-fill : `effectif`, `reglementExiste` depuis `TravailExtractedData`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `INOPPOSABLE` / opposabilité INOPPOSABLE ; orange `NON_CONFORME` ; gris `NON_REQUIS` ; navy/or info ; JetBrains Mono compteurs/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData`, `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `effectif`/`reglementExiste`, handlers `onXChange()`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (cohérence aiData / procedureChecks F-96), via `CoherenceAlertBuilder` partagé + popover (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-100-reglement-interieur-validite` enregistré (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON)
- Niveau outil : 2 (analyseur conformité multi-checklists + verdict + opposabilité) → parité domaines **non applicable** (règlement intérieur = spécificité FR-only sans équivalent immigration/famille)

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript
- [ ] effectif 80 + 4 contenus + 0 clause interdite + procédure complète → badge `CONFORME` vert, `OPPOSABLE`
- [ ] `contenuHarcelementAgissements` non coché → item croix rouge, badge `NON_CONFORME` orange, `itemsObligatoiresManquants=1`
- [ ] `clauseSanctionPecuniaire` coché → item interdit croix rouge, `NON_CONFORME`, `clausesInterditesPresentes ≥ 1`
- [ ] `consultationCseRealisee` non coché → badge `INOPPOSABLE` rouge, `opposabilite=INOPPOSABLE`
- [ ] effectif 20 + `reglementExiste=false` → encart `NON_REQUIS` gris
- [ ] `reglementExiste` pré-rempli depuis `TravailExtractedData` avec badge provenance
- [ ] note opposabilité / procédure de mise en place affichée
- [ ] Tests Jest ≥ 12 (rendu form, NON_REQUIS, statut CONFORME/NON_CONFORME/INOPPOSABLE, checklist contenu manquant, clause interdite, défaut procédure, compteurs, pré-fill, getPrefillCount 0/partiel/nominal, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `ReglementInterieurValiditeSectionComponent`
- **Nouveau service** `ReglementInterieurValiditeService`
- **Nouveau modèle** `ReglementInterieurValiditeAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + thème) ; DTO `TravailExtractedData` frontend (`reglementInterieurPresent` + `reglementInterieurDetecte`)

## Dépendances

- SF-218-35 : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Validité d'une sanction disciplinaire particulière fondée sur le RI
- Note de service / additif au RI
- Lanceur d'alerte (F-DT-61)
