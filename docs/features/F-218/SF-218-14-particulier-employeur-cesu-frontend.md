# Mini-spec — F-218 / SF-218-14 — Particulier employeur (CESU) : préavis et indemnité de licenciement — frontend

## Identifiant

`F-218 / SF-218-14`

## Feature parente

`F-218b` — Régimes catégoriels FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-14-particulier-employeur-cesu-frontend`

---

## Objectif

Livrer le composant Angular `<app-particulier-employeur-cesu-section>` pour `F-DT-108-particulier-employeur-cesu` : saisie de la rupture (catégorie d'employé, cause), affichage du préavis conventionnel, du verdict d'éligibilité et du montant de l'indemnité de licenciement / rupture selon la CCN applicable.

---

## Comportement attendu

- Formulaire : `dateEntree` (date, pré-rempli), `dateRupture` (date, pré-rempli), `categorieEmploye` (select SALARIE_PARTICULIER_EMPLOYEUR / ASSISTANT_MATERNEL, pré-rempli), `causeRupture` (select 5 valeurs ; `RETRAIT_ENFANT` désactivé si catégorie ≠ assistant maternel), `salaireMensuelMoyen` (number).
- Résultat :
  - `dureePreavisLibelle` (badge, JetBrains Mono).
  - Badge `eligibiliteIndemnite` : `DUE` vert / `NON_DUE` rouge (+ `motifNonDue` affiché).
  - `indemniteLicenciement` en JetBrains Mono + libellé `methodeCalcul` (CCN particulier employeur / indemnité de rupture assistant maternel 1/80).
  - Badge `verdictGlobal` (`RUPTURE_REGULIERE` / `RUPTURE_A_SECURISER` / `INDEMNITE_NON_DUE`).
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
  - Mention « barème CCN à actualiser annuellement (avenant salaires) ».
- CONTEXTUAL : apparaît si flag IA `particulier_employeur_detecte` = true. Groupement thématique cohérent avec les outils d'indemnités (réutiliser le thème `INDEMNITES`).
- Pré-fill : `dateEntree`, `dateRupture`, `categorieEmploye` depuis `TravailExtractedData`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `NON_DUE` ; navy/or info ; `<input type="date">` ; JetBrains Mono montants/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData`, `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `dateEntree`/`dateRupture`/`categorieEmploye`, handlers `onXChange()`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (dates croisées aiData / procedureChecks F-96), via `CoherenceAlertBuilder` partagé + popover (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-108-particulier-employeur-cesu` enregistré (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON)
- Niveau outil : 2 (calculateur + verdict) → parité domaines **non applicable** (régime particulier employeur = spécificité FR-only sans équivalent immigration/famille)

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript
- [ ] `categorieEmploye=SALARIE_PARTICULIER_EMPLOYEUR`, ancienneté > 2 ans → préavis "2 mois"
- [ ] `categorieEmploye=ASSISTANT_MATERNEL` → option `RETRAIT_ENFANT` activée ; sinon désactivée
- [ ] `causeRupture=FAUTE_GRAVE` → badge `NON_DUE` rouge + motif
- [ ] `categorieEmploye` pré-rempli depuis `TravailExtractedData` avec badge provenance
- [ ] Montant indemnité affiché avec libellé de la méthode de calcul (CCN PE / 1/80 assmat)
- [ ] Mention « barème CCN à actualiser annuellement » présente
- [ ] Tests Jest ≥ 12 (rendu form, préavis 1 semaine/1 mois/2 mois, éligibilité DUE/NON_DUE, conditionnement RETRAIT_ENFANT, pré-fill, getPrefillCount 0/partiel/nominal, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `ParticulierEmployeurCesuSectionComponent`
- **Nouveau service** `ParticulierEmployeurCesuService`
- **Nouveau modèle** `ParticulierEmployeurCesuAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + thème) ; DTO `TravailExtractedData` frontend (`cesuCategorieEmploye` + `particulierEmployeurDetecte`)

## Dépendances

- SF-218-13 : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Génération d'un courrier de licenciement particulier employeur (générateur futur)
- Aide à la déclaration CESU / PAJEMPLOI
