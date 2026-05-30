# Mini-spec — F-218 / SF-218-22 — Stagiaire : gratification minimale et requalification en CDI — frontend

## Identifiant

`F-218 / SF-218-22`

## Feature parente

`F-218b` — Régimes catégoriels FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-22-stagiaire-gratification-frontend`

---

## Objectif

Livrer le composant Angular `<app-stagiaire-gratification-section>` pour `F-DT-109-stagiaire-gratification-requalification` : saisie de la période de stage et des indices d'irrégularité, affichage du caractère obligatoire de la gratification, du rappel dû et du risque de requalification du stage en CDI.

---

## Comportement attendu

- Formulaire : `dateDebutStage` (date, pré-rempli), `dateFinStage` (date, pré-rempli), `nombreJoursPresence` (number), `gratificationMensuelleVersee` (number), `tauxHoraireConventionnel` (number, optionnel), `missionsHorsProjetPedagogique` (checkbox), `posteTravailPermanent` (checkbox).
- Résultat :
  - Badge `gratificationObligatoire` (oui/non) + `seuilAtteint`.
  - `gratificationMinimaleDue` et `rappelGratification` en JetBrains Mono (rappel en rouge si > 0).
  - Badge `risqueRequalification` (`FAIBLE` vert / `MODERE` orange / `ELEVE` rouge) + liste `motifs`.
  - Badge `verdictGlobal` (`STAGE_CONFORME` / `RAPPEL_GRATIFICATION` / `REQUALIFICATION_PROBABLE`).
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
  - Mention « taux de gratification / plafond SS à actualiser annuellement ».
- CONTEXTUAL : apparaît si flag IA `stage_detecte` = true. Groupement thématique cohérent avec les outils de qualification / rappel de salaire (réutiliser le thème existant).
- Pré-fill : `dateDebutStage`, `dateFinStage` depuis `TravailExtractedData`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé rappel > 0 / risque ELEVE ; orange `MODERE` ; navy/or info ; `<input type="date">` ; JetBrains Mono montants/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData`, `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `dateDebutStage`/`dateFinStage`, handlers `onXChange()`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (dates croisées aiData / procedureChecks F-96), via `CoherenceAlertBuilder` partagé + popover (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-109-stagiaire-gratification-requalification` enregistré (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON)
- Niveau outil : 3 (analyseur requalification + calculateur gratification + verdict) → parité domaines **non applicable** (régime stagiaire = spécificité FR-only sans équivalent immigration/famille)

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript
- [ ] stage > 2 mois, gratification 0 → `gratificationObligatoire` oui + `rappelGratification` rouge > 0
- [ ] stage < seuil → `gratificationObligatoire` non
- [ ] `posteTravailPermanent` + `missionsHorsProjetPedagogique` → badge `ELEVE` rouge + motifs listés
- [ ] durée > 6 mois → risque `ELEVE`
- [ ] `dateDebutStage`/`dateFinStage` pré-remplis depuis `TravailExtractedData` avec badge provenance
- [ ] Mention « taux à actualiser annuellement » présente
- [ ] Tests Jest ≥ 12 (rendu form, gratification obligatoire/non, rappel, taux conventionnel, requalification ELEVE/FAIBLE, motifs, pré-fill, getPrefillCount 0/partiel/nominal, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `StagiaireGratificationSectionComponent`
- **Nouveau service** `StagiaireGratificationService`
- **Nouveau modèle** `StagiaireGratificationAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + thème) ; DTO `TravailExtractedData` frontend (`dateDebutStage`, `dateFinStage` + `stageDetecte`)

## Dépendances

- SF-218-21 : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Génération d'une mise en demeure de versement de la gratification (générateur futur)
- Chiffrage complet des indemnités de rupture en cas de requalification
