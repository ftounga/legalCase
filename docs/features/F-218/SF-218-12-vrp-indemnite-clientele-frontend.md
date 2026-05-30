# Mini-spec — F-218 / SF-218-12 — VRP : statut, préavis et indemnité de clientèle — frontend

## Identifiant

`F-218 / SF-218-12`

## Feature parente

`F-218b` — Régimes catégoriels FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-12-vrp-indemnite-clientele-frontend`

---

## Objectif

Livrer le composant Angular `<app-vrp-indemnite-clientele-section>` pour `F-DT-104-vrp-indemnite-clientele` : saisie de la rupture VRP, affichage du préavis spécifique, du verdict d'éligibilité à l'indemnité de clientèle, de sa fourchette estimée, et de l'option la plus favorable vs indemnité légale.

---

## Comportement attendu

- Formulaire : `dateEntree` (date, pré-rempli), `dateRupture` (date, pré-rempli), `causeRupture` (select 6 valeurs), `typeVrp` (select EXCLUSIF/MULTICARTES), `commissionsAnnuellesMoyennes` (number, pré-rempli), `salaireMensuelMoyen` (number), `clienteleDeveloppee` (checkbox, défaut coché).
- Résultat :
  - `dureePreavisMois` (badge, JetBrains Mono).
  - Badge `eligibiliteClientele` : `DUE` vert / `NON_DUE` rouge (+ `motifNonDue` affiché).
  - Fourchette `indemniteClienteleMin`–`indemniteClienteleMax` en JetBrains Mono, avec mention « estimation indicative — évaluation souveraine du juge ».
  - `indemniteLegaleLicenciement` en JetBrains Mono.
  - Encart **option recommandée** (`INDEMNITE_CLIENTELE` / `INDEMNITE_LEGALE`) avec rappel du non-cumul.
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
- CONTEXTUAL : apparaît si flag IA `vrp_statut_detecte` = true. Groupement thématique cohérent avec les outils d'indemnités (réutiliser le thème existant des calculateurs d'indemnités, p. ex. `INDEMNITES`).
- Pré-fill : `dateEntree`, `dateRupture`, `commissionsAnnuellesMoyennes` depuis `TravailExtractedData`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `NON_DUE` ; navy/or info ; `<input type="date">` ; JetBrains Mono montants/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData`, `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `dateEntree`/`dateRupture`/`commissionsAnnuellesMoyennes`, handlers `onXChange()`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (dates croisées aiData / procedureChecks F-96), via `CoherenceAlertBuilder` partagé + popover (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-104-vrp-indemnite-clientele` enregistré (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON)
- Niveau outil : 4 (calculateur + verdict + recommandation) → parité domaines **non applicable** (régime VRP = spécificité FR-only sans équivalent immigration/famille)

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript
- [ ] `causeRupture=FAUTE_GRAVE` → badge `NON_DUE` rouge + motif
- [ ] ancienneté > 2 ans → `dureePreavisMois=3`
- [ ] `commissionsAnnuellesMoyennes` pré-rempli depuis `TravailExtractedData` avec badge provenance
- [ ] Encart option recommandée affiche la plus favorable + mention non-cumul
- [ ] Fourchette indemnité de clientèle affichée avec disclaimer « évaluation souveraine du juge »
- [ ] Tests Jest ≥ 12 (rendu form, éligibilité DUE/NON_DUE, préavis, pré-fill, getPrefillCount 0/partiel/nominal, gate FR, option recommandée)

## Tables / endpoints / composants impactés

- **Nouveau composant** `VrpIndemniteClienteleSectionComponent`
- **Nouveau service** `VrpIndemniteClienteleService`
- **Nouveau modèle** `VrpIndemniteClienteleAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + thème) ; DTO `TravailExtractedData` frontend (`vrpCommissionsAnnuelles` + `vrpStatutDetecte`)

## Dépendances

- SF-218-11 : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Génération d'un courrier de réclamation d'indemnité de clientèle (générateur futur)
- VRP multicartes : ventilation par employeur
