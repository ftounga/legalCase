# Mini-spec — F-218 / SF-218-16 — Journaliste professionnel : statut et clauses spécifiques — frontend

## Identifiant

`F-218 / SF-218-16`

## Feature parente

`F-218b` — Régimes catégoriels FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-16-journaliste-statut-frontend`

---

## Objectif

Livrer le composant Angular `<app-journaliste-statut-section>` pour `F-DT-105-journaliste-statut` : saisie de la rupture du journaliste, affichage de la qualification du statut, de la validité de la clause de cession / conscience invoquée, de l'indemnité de congédiement et du renvoi éventuel à la commission arbitrale.

---

## Comportement attendu

- Formulaire : `dateEntree` (date, pré-rempli), `dateRupture` (date, pré-rempli), `typeRupture` (select 5 valeurs), `salaireMensuelMoyen` (number), `carteIdentiteProfessionnelle` (checkbox, pré-rempli), `cessionTitreConstatee` (checkbox), `changementOrientationConstate` (checkbox).
- Résultat :
  - Badge `statutJournaliste` (`CONFIRME` / `A_QUALIFIER`).
  - Badge `clauseValide` : `VALIDE` vert / `NON_VALIDE` rouge / `SANS_OBJET` neutre (+ `motif` affiché).
  - `indemniteCongediement` en JetBrains Mono.
  - Encart **commission arbitrale** affiché si `commissionArbitraleRequise=true` (note plafond 15 mois + compétence exclusive).
  - Badge `verdictGlobal`.
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
- CONTEXTUAL : apparaît si flag IA `statut_journaliste_detecte` = true. Groupement thématique cohérent avec les outils de statut / qualification (réutiliser le thème existant des analyseurs de statut).
- Pré-fill : `dateEntree`, `dateRupture`, `carteIdentiteProfessionnelle` depuis `TravailExtractedData`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `NON_VALIDE` ; navy/or info ; `<input type="date">` ; JetBrains Mono montants/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData`, `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `dateEntree`/`dateRupture`/`carteIdentiteProfessionnelle`, handlers `onXChange()`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (dates croisées aiData / procedureChecks F-96), via `CoherenceAlertBuilder` partagé + popover (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-105-journaliste-statut` enregistré (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON)
- Niveau outil : 3 (analyseur statut + validité clause + calculateur) → parité domaines **non applicable** (statut journaliste = spécificité FR-only sans équivalent immigration/famille)

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript
- [ ] `typeRupture=CLAUSE_CESSION` + `cessionTitreConstatee` → badge `VALIDE` vert
- [ ] `typeRupture=CLAUSE_CONSCIENCE` sans constat → badge `NON_VALIDE` rouge + motif
- [ ] ancienneté > 15 ans → encart commission arbitrale affiché
- [ ] `carteIdentiteProfessionnelle` pré-rempli depuis `TravailExtractedData` avec badge provenance
- [ ] `indemniteCongediement` affichée en JetBrains Mono
- [ ] Tests Jest ≥ 12 (rendu form, clause cession/conscience valide/non valide, statut confirmé/à qualifier, commission arbitrale, indemnité, pré-fill, getPrefillCount 0/partiel/nominal, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `JournalisteStatutSectionComponent`
- **Nouveau service** `JournalisteStatutService`
- **Nouveau modèle** `JournalisteStatutAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + thème) ; DTO `TravailExtractedData` frontend (`journalisteCartePresse` + `statutJournalisteDetecte`)

## Dépendances

- SF-218-15 : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Génération d'un courrier d'activation de la clause de conscience (générateur futur)
- Estimation du montant fixé par la commission arbitrale
