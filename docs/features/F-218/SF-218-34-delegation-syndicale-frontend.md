# Mini-spec — F-218 / SF-218-34 — Délégué syndical / RSS : désignation et protection — frontend

## Identifiant

`F-218 / SF-218-34`

## Feature parente

`F-218c` — IRP / négociation collective FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-31

## Branche Git

`feat/SF-218-34-delegation-syndicale-frontend`

---

## Objectif

Livrer le composant Angular `<app-delegation-syndicale-section>` pour `F-DT-69-delegation-syndicale-protection` : saisie du type de mandat (DS / RSS), de l'effectif, de la représentativité et du score, affichage de la régularité de la désignation, du statut protégé et du risque de nullité d'un licenciement non autorisé.

---

## Comportement attendu

- Formulaire : `effectif` (number, pré-rempli), `typeMandat` (select `DELEGUE_SYNDICAL`/`RSS`, pré-rempli), `syndicatRepresentatif` (checkbox), `pourcentageScorePersonnel` (number 0–100, visible si DS), `dateDesignation` (date), `licenciementEnvisage` (checkbox), `autorisationInspecteurTravail` (checkbox, visible si licenciement envisagé).
- Résultat :
  - Checklist des conditions de désignation (effectif, représentativité, score) avec coche verte / croix rouge.
  - Badge `statutDesignation` : `REGULIERE` vert / `IRREGULIERE` rouge / `A_VERIFIER` orange.
  - Badge `statutProtege` (`OUI`) + note salarié protégé.
  - Badge `risqueNulliteLicenciement` (`FAIBLE` vert / `ELEVE` rouge / `SANS_OBJET` gris) + note explicative (autorisation inspecteur du travail, réintégration).
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
- CONTEXTUAL : apparaît si flag IA `delegation_syndicale_detectee` = true. Groupement thématique cohérent avec les outils IRP / statuts protégés.
- Pré-fill : `effectif`, `typeMandat` depuis `TravailExtractedData`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `IRREGULIERE` / risque ELEVE ; orange `A_VERIFIER` ; gris `SANS_OBJET` ; navy/or info ; JetBrains Mono score/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData`, `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `effectif`/`typeMandat`, handlers `onXChange()`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (cohérence aiData / procedureChecks F-96), via `CoherenceAlertBuilder` partagé + popover (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-69-delegation-syndicale-protection` enregistré (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON)
- Niveau outil : 2 (analyseur statut + protection + verdict) → parité domaines **non applicable** (délégation syndicale = spécificité FR-only sans équivalent immigration/famille)

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript
- [ ] DS effectif 80 + représentatif + score 15 → badge `REGULIERE` vert
- [ ] DS effectif 30 → item effectif croix rouge, badge `IRREGULIERE` rouge
- [ ] DS sans score → badge `A_VERIFIER` orange
- [ ] `typeMandat=RSS` → champ score masqué, `syndicatRepresentatif=false` attendu
- [ ] `licenciementEnvisage=true` → champ autorisation visible ; sans autorisation → `risqueNulliteLicenciement=ELEVE` rouge
- [ ] `typeMandat` pré-rempli depuis `TravailExtractedData` avec badge provenance
- [ ] note salarié protégé / réintégration affichée
- [ ] Tests Jest ≥ 12 (rendu form, statutDesignation REGULIERE/IRREGULIERE/A_VERIFIER, champ score conditionnel DS, RSS, risque ELEVE/FAIBLE/SANS_OBJET, statutProtege, pré-fill, getPrefillCount 0/partiel/nominal, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `DelegationSyndicaleSectionComponent`
- **Nouveau service** `DelegationSyndicaleService`
- **Nouveau modèle** `DelegationSyndicaleAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + thème) ; DTO `TravailExtractedData` frontend (`mandatSyndicalType` + `delegationSyndicaleDetectee`)

## Dépendances

- SF-218-33 : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Statut protégé RP général (F-DT-30)
- Procédure détaillée d'autorisation / recours devant l'inspecteur du travail
- Générateur de contestation de désignation
