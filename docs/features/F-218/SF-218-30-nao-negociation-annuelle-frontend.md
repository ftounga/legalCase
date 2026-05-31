# Mini-spec — F-218 / SF-218-30 — NAO : négociation annuelle obligatoire — frontend

## Identifiant

`F-218 / SF-218-30`

## Feature parente

`F-218c` — IRP / négociation collective FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-31

## Branche Git

`feat/SF-218-30-nao-negociation-annuelle-frontend`

---

## Objectif

Livrer le composant Angular `<app-nao-negociation-annuelle-section>` pour `F-DT-66-nao-negociation-annuelle` : saisie des blocs négociés, de la périodicité et des dates, affichage de la checklist de conformité NAO, du calcul d'échéance de la prochaine négociation, du verdict de conformité et du risque d'entrave.

---

## Comportement attendu

- Formulaire : `effectif` (number, pré-rempli), `delegueSyndicalPresent` (checkbox, pré-rempli), `blocRemunerationNegocie` (checkbox), `blocEgaliteQvtNegocie` (checkbox), `accordMethodePeriodicite` (checkbox), `dateDerniereNegociation` (date), `periodiciteMois` (number, défaut 12), `pvDesaccordEtabli` (checkbox), `negociationAboutie` (checkbox).
- Résultat :
  - Si `NON_APPLICABLE` : encart d'information (pas de DS → pas de NAO), formulaire de checklist masqué.
  - Checklist des items (4 items) avec coche verte / croix rouge + badge `obligatoire`.
  - Bloc échéance : `dateProchaineEcheance`, `joursAvantEcheance` (JetBrains Mono) + badge `statutEcheance` (`A_JOUR` vert / `ECHEANCE_PROCHE` orange / `DEPASSEE` rouge).
  - Badge `statut` : `CONFORME` vert / `NON_CONFORME` rouge / `NON_APPLICABLE` gris.
  - Badge `risqueEntrave` (`FAIBLE` vert / `MODERE` orange / `ELEVE` rouge) + note explicative (délit d'entrave, pénalité égalité F/H).
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
- CONTEXTUAL : apparaît si flag IA `nao_detectee` = true. Groupement thématique cohérent avec les outils IRP / négociation collective.
- Pré-fill : `effectif`, `delegueSyndicalPresent` depuis `TravailExtractedData`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `NON_CONFORME` / échéance DEPASSEE / risque ELEVE ; orange `ECHEANCE_PROCHE`/`MODERE` ; gris `NON_APPLICABLE` ; navy/or info ; JetBrains Mono échéance/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData`, `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `effectif`/`delegueSyndicalPresent`, handlers `onXChange()`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (cohérence aiData / procedureChecks F-96), via `CoherenceAlertBuilder` partagé + popover (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-66-nao-negociation-annuelle` enregistré (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON)
- Niveau outil : 2 (checklist conformité + calculateur d'échéance + verdict) → parité domaines **non applicable** (NAO = spécificité FR-only sans équivalent immigration/famille)

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript
- [ ] `delegueSyndicalPresent=false` → encart `NON_APPLICABLE` gris, checklist masquée
- [ ] DS présent + 2 blocs + échéance non dépassée → badge `CONFORME` vert, risque `FAIBLE`
- [ ] DS présent + blocs non cochés → badge `NON_CONFORME` rouge, risque `ELEVE`
- [ ] `dateDerniereNegociation` -13 mois, périodicité 12 → badge `statutEcheance=DEPASSEE` rouge
- [ ] `periodiciteMois=24` sans accord de méthode → item périodicité croix rouge
- [ ] `delegueSyndicalPresent` pré-rempli depuis `TravailExtractedData` avec badge provenance
- [ ] note entrave / pénalité égalité F/H affichée
- [ ] Tests Jest ≥ 12 (rendu form, NON_APPLICABLE, statut CONFORME/NON_CONFORME, mapping échéance A_JOUR/PROCHE/DEPASSEE, item périodicité selon accord, mapping risque, pré-fill, getPrefillCount 0/partiel/nominal, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `NaoNegociationAnnuelleSectionComponent`
- **Nouveau service** `NaoNegociationAnnuelleService`
- **Nouveau modèle** `NaoNegociationAnnuelleAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + thème) ; DTO `TravailExtractedData` frontend (`delegueSyndicalPresent` + `naoDetectee`)

## Dépendances

- SF-218-29 : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Validité de l'accord issu de la NAO (F-DT-67)
- Index égalité professionnelle F/H (F-DT-101)
- Générateur de PV de désaccord
