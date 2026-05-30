# Mini-spec — F-218 / SF-218-18 — Intermittent du spectacle : ouverture des droits ARE (annexes 8/10) — frontend

## Identifiant

`F-218 / SF-218-18`

## Feature parente

`F-218b` — Régimes catégoriels FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-18-intermittent-spectacle-are-frontend`

---

## Objectif

Livrer le composant Angular `<app-intermittent-spectacle-are-section>` pour `F-DT-106-intermittent-spectacle-are` : saisie de l'annexe et des heures/cachets, affichage du total d'heures retenues vs seuil 507 h, du verdict d'ouverture des droits ARE et du déficit/excédent d'heures.

---

## Comportement attendu

- Formulaire : `annexe` (select ANNEXE_8_TECHNICIENS / ANNEXE_10_ARTISTES, pré-rempli), `dateFinContrat` (date, pré-rempli), `heuresTravaillees12Mois` (number), `nombreCachets` (number, visible si annexe 10), `heuresFormationDispensees` (number).
- Résultat :
  - `heuresTotalesRetenues` vs seuil 507 h (badge JetBrains Mono + barre de progression vers 507).
  - Badge `ouvertureDroits` : `OUVERTS` vert / `NON_OUVERTS` rouge.
  - `heuresManquantes` (rouge) ou `heuresExcedentaires` (vert) en JetBrains Mono.
  - Badge `statut` (`DROITS_OUVERTS` / `DROITS_NON_OUVERTS` / `A_VERIFIER`).
  - `dateProchainExamen` affichée.
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
  - Mention « seuil et plafonds Unedic à actualiser à chaque convention — vérification finale France Travail ».
- CONTEXTUAL : apparaît si flag IA `statut_intermittent_detecte` = true. Groupement thématique cohérent avec les outils d'ouverture de droits / chômage (réutiliser le thème existant des analyseurs de droits).
- Pré-fill : `dateFinContrat`, `annexe` depuis `TravailExtractedData`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `NON_OUVERTS` / heures manquantes ; navy/or info ; `<input type="date">` ; JetBrains Mono heures/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData`, `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `dateFinContrat`/`annexe`, handlers `onXChange()`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (dates croisées aiData / procedureChecks F-96), via `CoherenceAlertBuilder` partagé + popover (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-106-intermittent-spectacle-are` enregistré (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON)
- Niveau outil : 2 (analyseur ouverture droits + calculateur seuil) → parité domaines **non applicable** (régime intermittent = spécificité FR-only sans équivalent immigration/famille)

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript
- [ ] `heuresTravaillees12Mois >= 507` → badge `OUVERTS` vert + `heuresExcedentaires`
- [ ] `annexe=ANNEXE_10_ARTISTES` → champ `nombreCachets` visible ; conversion 12 h reflétée dans le total
- [ ] total < 507 → badge `NON_OUVERTS` rouge + `heuresManquantes`
- [ ] total en marge ± 10 h → badge `A_VERIFIER`
- [ ] `dateFinContrat` pré-rempli depuis `TravailExtractedData` avec badge provenance
- [ ] Barre de progression vers 507 h affichée
- [ ] Tests Jest ≥ 12 (rendu form, conditionnement cachets annexe 10, ouverture/non-ouverture, A_VERIFIER, heures manquantes/excédentaires, date examen, pré-fill, getPrefillCount 0/partiel/nominal, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `IntermittentSpectacleAreSectionComponent`
- **Nouveau service** `IntermittentSpectacleAreService`
- **Nouveau modèle** `IntermittentSpectacleAreAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + thème) ; DTO `TravailExtractedData` frontend (`intermittentAnnexe` + `statutIntermittentDetecte`)

## Dépendances

- SF-218-17 : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Estimation du montant journalier de l'ARE (SJR)
- Simulation de la date d'anniversaire et du nombre de jours indemnisables
