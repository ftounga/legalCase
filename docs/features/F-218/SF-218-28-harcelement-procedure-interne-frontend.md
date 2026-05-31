# Mini-spec — F-218 / SF-218-28 — Harcèlement : procédure interne de traitement d'un signalement — frontend

## Identifiant

`F-218 / SF-218-28`

## Feature parente

`F-218c` — IRP / négociation collective FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-31

## Branche Git

`feat/SF-218-28-harcelement-procedure-interne-frontend`

---

## Objectif

Livrer le composant Angular `<app-harcelement-procedure-interne-section>` pour `F-DT-59-harcelement-procedure-interne` : saisie des éléments de la procédure interne (référents, information, enquête, mesures conservatoires, dates), affichage de la checklist de conformité employeur, du verdict de conformité, du délai de réaction et du risque de responsabilité.

---

## Comportement attendu

- Formulaire : `effectif` (number, pré-rempli), `referentCseDesigne` (checkbox), `referentEmployeurDesigne` (checkbox), `informationAffichageRealisee` (checkbox), `signalementRecu` (checkbox, pré-rempli), `dateSignalement` (date), `dateOuvertureEnquete` (date), `enqueteContradictoire` (checkbox), `mesuresConservatoiresPrises` (checkbox).
- Résultat :
  - Checklist des items avec coche verte / croix rouge + badge `obligatoire` ; `itemsObligatoiresManquants` (badge, JetBrains Mono).
  - Badge `statut` : `CONFORME` vert / `NON_CONFORME` orange / `CARENCE_GRAVE` rouge.
  - Bloc délai : `delaiReactionJours` (JetBrains Mono) + badge `delaiRaisonnable` (`OUI` vert / `LIMITE` orange / `NON` rouge).
  - Badge `risqueResponsabiliteEmployeur` (`FAIBLE` vert / `MODERE` orange / `ELEVE` rouge) + note explicative (obligation de sécurité L.4121-1).
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
- CONTEXTUAL : apparaît si flag IA `harcelement_procedure_interne_detectee` = true. Groupement thématique cohérent avec les outils de conformité employeur / IRP (réutiliser le thème existant des analyseurs de conformité).
- Pré-fill : `effectif`, `signalementRecu` depuis `TravailExtractedData`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `CARENCE_GRAVE` / délai NON / risque ELEVE ; orange `NON_CONFORME`/`LIMITE`/`MODERE` ; navy/or info ; JetBrains Mono délai/itemsManquants/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData`, `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `effectif`/`signalementRecu`, handlers `onXChange()`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (cohérence aiData / procedureChecks F-96), via `CoherenceAlertBuilder` partagé + popover (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-59-harcelement-procedure-interne` enregistré (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON)
- Niveau outil : 2 (checklist conformité employeur + verdict + délai) → parité domaines **non applicable** (procédure interne harcèlement = spécificité FR-only sans équivalent immigration/famille)

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript
- [ ] tous items obligatoires conformes + délai ≤ 15 j → badge `CONFORME` vert, risque `FAIBLE`
- [ ] référent CSE non coché (effectif ≥ 11) → item croix rouge, badge `NON_CONFORME` orange
- [ ] signalement reçu + enquête non contradictoire → badge `CARENCE_GRAVE` rouge, risque `ELEVE`
- [ ] délai 90 j → badge `delaiRaisonnable=NON` rouge
- [ ] `itemsObligatoiresManquants` affiché
- [ ] `signalementRecu` pré-rempli depuis `TravailExtractedData` avec badge provenance
- [ ] note obligation de sécurité affichée
- [ ] Tests Jest ≥ 12 (rendu form, statut CONFORME/NON_CONFORME/CARENCE_GRAVE, item référent CSE selon effectif, mapping délai OUI/LIMITE/NON, mapping risque, pré-fill, getPrefillCount 0/partiel/nominal, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `HarcelementProcedureInterneSectionComponent`
- **Nouveau service** `HarcelementProcedureInterneService`
- **Nouveau modèle** `HarcelementProcedureInterneAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + thème) ; DTO `TravailExtractedData` frontend (`harcelementSignalementInterne` + `harcelementProcedureInterneDetectee`)

## Dépendances

- SF-218-27 : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Nullité du licenciement consécutif (F-DT-11)
- Chiffrage des dommages-intérêts de harcèlement
- Générateur de réponse à un signalement
