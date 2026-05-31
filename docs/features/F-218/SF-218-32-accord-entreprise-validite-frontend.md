# Mini-spec — F-218 / SF-218-32 — Accord d'entreprise : validité (conditions de majorité) — frontend

## Identifiant

`F-218 / SF-218-32`

## Feature parente

`F-218c` — IRP / négociation collective FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-31

## Branche Git

`feat/SF-218-32-accord-entreprise-validite-frontend`

---

## Objectif

Livrer le composant Angular `<app-accord-entreprise-validite-section>` pour `F-DT-67-accord-entreprise-validite` : saisie du % de suffrages des signataires, du référendum éventuel, du type d'opération (conclusion / révision / dénonciation), affichage de la condition de majorité retenue, du verdict de validité et, en dénonciation, de la date de fin de survie de l'accord.

---

## Comportement attendu

- Formulaire : `pourcentageSuffragesSignataires` (number 0–100, pré-rempli), `referendumOrganise` (checkbox), `referendumApprouve` (checkbox), `typeOperation` (select `CONCLUSION`/`REVISION`/`DENONCIATION`, pré-rempli), `signePartiesHabilitees` (checkbox, visible si REVISION), `preavisDenonciationRespecte` (checkbox, visible si DENONCIATION), `dateDenonciation` (date, visible si DENONCIATION).
- Résultat :
  - Badge `conditionMajorite` : `MAJORITE_50` vert / `REFERENDUM_30` orange / `INSUFFISANTE` rouge + `pourcentageSuffragesSignataires` (JetBrains Mono).
  - Checklist des items (majorité, référendum, parties habilitées, préavis selon le cas) avec coche verte / croix rouge.
  - Badge `statut` : `VALIDE` vert / `VALIDE_SOUS_RESERVE` orange / `NON_VALIDE` rouge.
  - En dénonciation : `dateFinSurvie` (JetBrains Mono) + note survie 12 mois.
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
- CONTEXTUAL : apparaît si flag IA `accord_entreprise_detecte` = true. Groupement thématique cohérent avec les outils IRP / négociation collective.
- Pré-fill : `pourcentageSuffragesSignataires`, `typeOperation` depuis `TravailExtractedData`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `INSUFFISANTE`/`NON_VALIDE` ; orange `REFERENDUM_30`/`VALIDE_SOUS_RESERVE` ; navy/or info ; JetBrains Mono pourcentage/dateFinSurvie/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData`, `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `pourcentageSuffragesSignataires`/`typeOperation`, handlers `onXChange()`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (cohérence aiData / procedureChecks F-96), via `CoherenceAlertBuilder` partagé + popover (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-67-accord-entreprise-validite` enregistré (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON)
- Niveau outil : 3 (analyseur validité multi-conditions + verdict + calcul survie) → parité domaines **non applicable** (accord d'entreprise = spécificité FR-only sans équivalent immigration/famille)

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript
- [ ] `pourcentageSuffragesSignataires=55`, CONCLUSION → badge `MAJORITE_50` vert, `VALIDE`
- [ ] `35` + référendum approuvé → badge `REFERENDUM_30` orange, `VALIDE_SOUS_RESERVE`
- [ ] `35` sans référendum → badge `INSUFFISANTE` rouge, `NON_VALIDE`
- [ ] `typeOperation=REVISION` → champ `signePartiesHabilitees` visible ; false → item croix rouge, `NON_VALIDE`
- [ ] `typeOperation=DENONCIATION` → champs préavis + date visibles, `dateFinSurvie` affichée
- [ ] `typeOperation` pré-rempli depuis `TravailExtractedData` avec badge provenance
- [ ] Tests Jest ≥ 12 (rendu form, conditionMajorite MAJORITE_50/REFERENDUM_30/INSUFFISANTE, statut VALIDE/VALIDE_SOUS_RESERVE/NON_VALIDE, champs conditionnels révision/dénonciation, dateFinSurvie, pré-fill, getPrefillCount 0/partiel/nominal, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `AccordEntrepriseValiditeSectionComponent`
- **Nouveau service** `AccordEntrepriseValiditeService`
- **Nouveau modèle** `AccordEntrepriseValiditeAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + thème) ; DTO `TravailExtractedData` frontend (`accordPourcentageSignataires` + `accordTypeOperation` + `accordEntrepriseDetecte`)

## Dépendances

- SF-218-31 : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Contenu / opposabilité d'une clause particulière de l'accord
- NAO (F-DT-66)
- Générateur d'avenant de révision
