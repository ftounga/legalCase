# Mini-spec — F-218 / SF-218-26 — Licenciement CDI de chantier / d'opération — frontend

## Identifiant

`F-218 / SF-218-26`

## Feature parente

`F-218b` — Régimes catégoriels FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-26-cdi-chantier-frontend`

---

## Objectif

Livrer le composant Angular `<app-cdi-chantier-section>` pour `F-DT-37-licenciement-cdi-chantier` : saisie des conditions du recours et de la fin de chantier, affichage de la validité du recours, de la qualification du motif (fin de chantier = cause réelle et sérieuse) et de l'indemnité de licenciement.

---

## Comportement attendu

- Formulaire : `dateEntree` (date, pré-rempli), `dateRupture` (date, pré-rempli), `fondementRecours` (select ACCORD_BRANCHE_ETENDU / USAGE_CONSTANT_SECTEUR / AUCUN), `secteur` (select BTP / INGENIERIE / AUTRE, pré-rempli), `chantierAcheve` (checkbox), `salaireMensuelMoyen` (number), `reclassementAutreChantierPropose` (checkbox).
- Résultat :
  - Badge `recoursValide` (oui vert / non rouge) + `motifRecours`.
  - Badge `motifLicenciement` (`FIN_CHANTIER_CRS` vert / `MOTIF_NON_FONDE` rouge).
  - `indemniteLicenciement` en JetBrains Mono.
  - Badge `verdictGlobal` (`LICENCIEMENT_FONDE` / `LICENCIEMENT_A_SECURISER` orange / `RECOURS_INVALIDE` rouge).
  - Rappel `procedureRequise` (procédure de licenciement de droit commun).
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
  - Mention « barème R.1234-2 — vérifier CCN BTP/ingénierie plus favorable ».
- CONTEXTUAL : apparaît si flag IA `cdi_chantier_detecte` = true. Groupement thématique cohérent avec les outils de validité de licenciement / indemnités (réutiliser le thème existant).
- Pré-fill : `dateEntree`, `dateRupture`, `secteur` depuis `TravailExtractedData`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `RECOURS_INVALIDE` / `MOTIF_NON_FONDE` ; orange `A_SECURISER` ; navy/or info ; `<input type="date">` ; JetBrains Mono montants/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData`, `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `dateEntree`/`dateRupture`/`secteur`, handlers `onXChange()`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (dates croisées aiData / procedureChecks F-96), via `CoherenceAlertBuilder` partagé + popover (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-37-licenciement-cdi-chantier` enregistré (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON)
- Niveau outil : 3 (analyseur validité recours + qualification motif + calculateur indemnité) → parité domaines **non applicable** (CDI de chantier = spécificité FR-only sans équivalent immigration/famille)

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript
- [ ] `fondementRecours=ACCORD_BRANCHE_ETENDU` + `chantierAcheve=true` → `recoursValide` oui + `FIN_CHANTIER_CRS` vert + `LICENCIEMENT_FONDE`
- [ ] `fondementRecours=AUCUN` → `recoursValide` non rouge + `RECOURS_INVALIDE` + note requalification
- [ ] `chantierAcheve=false` → `MOTIF_NON_FONDE` rouge
- [ ] `indemniteLicenciement` affichée en JetBrains Mono
- [ ] `secteur` pré-rempli depuis `TravailExtractedData` avec badge provenance
- [ ] Mention « vérifier CCN BTP/ingénierie plus favorable » présente
- [ ] Tests Jest ≥ 12 (rendu form, recours valide/invalide, motif fin chantier/non fondé, indemnité, verdict A_SECURISER, pré-fill, getPrefillCount 0/partiel/nominal, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `CdiChantierSectionComponent`
- **Nouveau service** `CdiChantierService`
- **Nouveau modèle** `CdiChantierAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + thème) ; DTO `TravailExtractedData` frontend (`cdiChantierSecteur` + `cdiChantierDetecte`)

## Dépendances

- SF-218-25 : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Chiffrage de l'indemnité pour licenciement sans cause réelle et sérieuse en cas de requalification
- Génération d'un courrier de licenciement pour fin de chantier (générateur futur)
