# Mini-spec — F-218 / SF-218-52 — Temps de trajet / de déplacement professionnel — frontend

## Identifiant

`F-218 / SF-218-52`

## Feature parente

`F-218d` — Temps de travail / épargne salariale / congés FR-only (P4 Travail FR)

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-218-52-temps-trajet-deplacement-frontend`

---

## Objectif

Livrer le composant Angular standalone OnPush `<app-temps-trajet-deplacement-section>` pour `F-DT-81-temps-trajet-deplacement` : saisie du type de trajet et de sa durée, affichage de la qualification (temps de travail effectif ou non) et du verdict de contrepartie due.

---

## Comportement attendu

- Composant **standalone, `ChangeDetection.OnPush`**, sélecteur `<app-temps-trajet-deplacement-section>` ; injection `ChangeDetectorRef` et appel `markForCheck()` dans chaque `subscribe()` (next + error) — invariant OnPush.
- Endpoint consommé : `GET /api/v1/case-files/{caseFileId}/temps-trajet-deplacement-analysis`.
- Formulaire (champs = ceux du POST backend — SF paire) : `typeTrajet` (select : domicile-travail habituel, déplacement professionnel, intervention sur site, etc. — pré-rempli), `tempsTrajetQuotidienMinutes` (number, pré-rempli).
- Résultat :
  - Badge `qualification` : temps de travail effectif (vert favorable au salarié) / hors temps de travail (gris neutre).
  - Badge `contrepartieDue` : `DUE` vert / `NON_DUE` rouge (+ motif).
  - Mention du dépassement éventuel du temps normal de trajet (contrepartie en repos ou financière).
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
- CONTEXTUAL : apparaît si flag IA `temps_trajet_detecte` = true. Thème **INDEMNITES**.
- Pré-fill : `typeTrajet`, `tempsTrajetQuotidienMinutes` depuis `Sf218dDetail` (sous-record consolidé de `TravailExtractedData`). Tout champ saisissable est pré-rempli par l'IA (F-246) sauf information non factualisable.
- Règles de pré-remplissage isolées dans `temps-trajet-deplacement-section-prefill-rules.ts`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `contrepartieDue=NON_DUE` ; vert qualification effective / contrepartie DUE ; gris hors temps de travail neutre ; navy/or info ; JetBrains Mono baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData` (via `Sf218dDetail`), `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `typeTrajet`/`tempsTrajetQuotidienMinutes`, handlers `onXChange()` ; règles dans `temps-trajet-deplacement-section-prefill-rules.ts`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (cohérence aiData / saisie), via `CoherenceAlertBuilder` partagé + popover de divergence (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-81-temps-trajet-deplacement` enregistré dans `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON) + `THEME_BY_TOOL_ID` (`INDEMNITES`)
- Niveau outil : 3 (qualification + verdict contrepartie) → parité domaines **non applicable** (temps de trajet FR = spécificité FR-only sans équivalent immigration/famille)

---

## Self-check grep pré-commit (OBLIGATOIRE)

Avant tout commit, exécuter et vérifier la cohérence `TOOL_REGISTRY` ↔ test d'intégrité :

```
grep -n "F-DT-81-temps-trajet-deplacement" frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts
grep -n "F-DT-81-temps-trajet-deplacement" frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.spec.ts
grep -rn "F-DT-81-temps-trajet-deplacement" frontend/src/app/case-files/decisional-tools-panel/prefill-count-integrity.spec.ts
```

L'ID doit apparaître dans `TOOL_REGISTRY`, `THEME_BY_TOOL_ID` et la liste des IDs connus du test d'intégrité (KNOWN_FRONTEND_TOOL_IDS / test `THEME_BY_TOOL_ID`). Tout orphelin = BLOCAGE pré-commit.

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript (`ng build`)
- [ ] `typeTrajet=DEPLACEMENT_PROFESSIONNEL` dépassant le temps normal → `contrepartieDue=DUE` vert
- [ ] `typeTrajet=DOMICILE_TRAVAIL_HABITUEL` → qualification hors temps de travail gris, `contrepartieDue=NON_DUE` rouge + motif
- [ ] `typeTrajet` / `tempsTrajetQuotidienMinutes` pré-remplis depuis `Sf218dDetail` avec badge provenance `auto_awesome`
- [ ] badge qualification cohérent avec le type de trajet
- [ ] divergence aiData ↔ saisie → alerte F-IA-03 (CoherenceAlertBuilder) + popover
- [ ] Tests Jest ≥ 12 (rendu form, qualification, contrepartie DUE/NON_DUE, pré-fill, getPrefillCount 0/partiel/nominal, coherenceAlerts, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `TempsTrajetDeplacementSectionComponent` (+ `temps-trajet-deplacement-section-prefill-rules.ts`)
- **Nouveau service** `TempsTrajetDeplacementService`
- **Nouveau modèle** `TempsTrajetDeplacementAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + `THEME_BY_TOOL_ID`) ; DTO `TravailExtractedData` frontend (sous-record `Sf218dDetail` : `typeTrajet`, `tempsTrajetQuotidienMinutes`, `tempsTrajetDetecte`)

## Dépendances

- SF-218-51 (backend temps de trajet / déplacement) : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Backend (= SF paire SF-218-51)
- Calcul des frais de déplacement / remboursement kilométrique
- Décompte des heures supplémentaires liées au déplacement
