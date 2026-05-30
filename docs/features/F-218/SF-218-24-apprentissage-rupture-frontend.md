# Mini-spec — F-218 / SF-218-24 — Apprentissage : validité de la rupture du contrat — frontend

## Identifiant

`F-218 / SF-218-24`

## Feature parente

`F-218b` — Régimes catégoriels FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-24-apprentissage-rupture-frontend`

---

## Objectif

Livrer le composant Angular `<app-apprentissage-rupture-section>` pour `F-DT-110-apprentissage-rupture` : saisie de la rupture du contrat d'apprentissage (période, auteur, motif), affichage de la qualification de la période (45 jours), de la validité du motif et des conséquences procédurales.

---

## Comportement attendu

- Formulaire : `dateDebutContrat` (date, pré-rempli), `dateRupture` (date, pré-rempli), `auteurRupture` (select EMPLOYEUR / APPRENTI), `motifRupture` (select 6 valeurs, pré-rempli), `apprentiMajeur` (checkbox, défaut coché).
- Résultat :
  - Badge `periode` (`DANS_45_PREMIERS_JOURS` / `APRES_45_JOURS`) + jours depuis le début.
  - Badge `validite` : `VALIDE` vert / `NON_VALIDE` rouge / `A_SECURISER` orange (+ `motif` affiché).
  - Liste `consequences` (saisine CPH, procédure de licenciement, indemnités...).
  - Badge `verdictGlobal` (`RUPTURE_REGULIERE` / `RUPTURE_IRREGULIERE` / `RUPTURE_A_SECURISER`).
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
- CONTEXTUAL : apparaît si flag IA `apprentissage_rupture_detectee` = true. Groupement thématique cohérent avec les outils de validité de rupture (réutiliser le thème existant des analyseurs de rupture).
- Pré-fill : `dateDebutContrat`, `dateRupture`, `motifRupture` depuis `TravailExtractedData`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `NON_VALIDE` ; orange `A_SECURISER` ; navy/or info ; `<input type="date">` ; JetBrains Mono baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData`, `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `dateDebutContrat`/`dateRupture`/`motifRupture`, handlers `onXChange()`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (dates croisées aiData / procedureChecks F-96), via `CoherenceAlertBuilder` partagé + popover (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-110-apprentissage-rupture` enregistré (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON)
- Niveau outil : 3 (analyseur validité + période + conséquences) → parité domaines **non applicable** (rupture d'apprentissage = spécificité FR-only sans équivalent immigration/famille)

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript
- [ ] rupture J+20 + `SANS_MOTIF` → badge période `DANS_45_PREMIERS_JOURS` + `VALIDE` vert
- [ ] rupture J+90 + `SANS_MOTIF` → badge `NON_VALIDE` rouge + conséquences listées
- [ ] J+90 + `FAUTE_GRAVE` → badge `A_SECURISER` orange (procédure de licenciement)
- [ ] J+90 + `ACCORD_PARTIES` → badge `VALIDE` vert
- [ ] `motifRupture` pré-rempli depuis `TravailExtractedData` avec badge provenance
- [ ] Liste `consequences` affichée
- [ ] Tests Jest ≥ 12 (rendu form, période 45 j, validité VALIDE/NON_VALIDE/A_SECURISER, conséquences, motifs, pré-fill, getPrefillCount 0/partiel/nominal, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `ApprentissageRuptureSectionComponent`
- **Nouveau service** `ApprentissageRuptureService`
- **Nouveau modèle** `ApprentissageRuptureAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + thème) ; DTO `TravailExtractedData` frontend (`apprentissageMotifRupture` + `apprentissageRuptureDetectee`)

## Dépendances

- SF-218-23 : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Chiffrage des dommages-intérêts en cas de rupture irrégulière
- Génération d'un courrier de rupture d'apprentissage (générateur futur)
