# Mini-spec — F-195 / SF-195-03 Section "Risques retenus" en PDF synthèse

## Identifiant

`F-195 / SF-195-03`

## Statut

`draft` — 2026-05-06

## Branche Git

`feat/SF-195-03-pdf-export-risques`

## Pattern de référence

**SF-192-03 + SF-193-03 + SF-194-03**.

## Ordre des sections en début de PDF synthèse (post F-195)

1. Page de garde
2. **🎯 Stratégies retenues** (F-192)
3. **🔍 Conformité procédurale** (F-193)
4. **📎 Pièces à demander au client** (F-194 — proéminent, livrable client)
5. **⚠️ Risques retenus par votre avocat** (F-195 — nouveau)
6. Timeline / Faits / Risques (lecture seule, complémentaire) / etc.

---

## Objectif

Section dédiée « ⚠️ Risques retenus par votre avocat » avec liste des risques VALIDÉ + badge outil pré-flaggé. Sous-bloc `❌ Risques écartés (N)` en compteur (sans liste détaillée — cohérent F-194 pieces).

---

## Comportement attendu

### Cas nominal

1. `PdfExportService.export(caseFile, synthesis, retainedPistes?, procedureChecksAlignment?, piecesAlignment?, risquesAlignment?)` 6ᵉ paramètre.
2. `SynthesisComponent.exportPdf` charge `RisqueAlignmentService.getForCaseFile(id)` en parallèle (timeout 5 s, fail-open).
3. Si ≥ 1 risque VALIDÉ → section insérée APRÈS « Pièces à demander » :
   - Titre : « ⚠️ Risques retenus par votre avocat » (taille 16 bold, navy)
   - Sous-titre : « Score validé : Y / 100 » (si `scoreAvocat` disponible — JetBrains Mono 11)
   - Liste des risques VALIDÉ : libellé Inter 11 + badge outil cible si applicable (`→ <label outil>` JetBrains Mono italique 9, lookup helper `resolveToolLabel()`)
   - Sous-compteur `❌ Risques écartés : N` (Inter italique 9 gris, sans liste — cohérent F-194)
4. Si **aucun risque VALIDÉ** → section omise (cas nominal).

### Cas d'erreur

Identiques F-194 SF-194-03 (fail-open silencieux endpoint timeout).

---

## Critères d'acceptation

- [ ] **CA-01** : ≥ 1 risque VALIDÉ → section incluse APRÈS « Pièces à demander »
- [ ] **CA-02** : aucun VALIDÉ → section omise
- [ ] **CA-03 score validé** : `scoreAvocat` affiché si présent
- [ ] **CA-04 badge outil** : risque VALIDÉ avec `toolIdsCibles` → suffixe `→ <label outil>` JetBrains Mono italique
- [ ] **CA-05 compteur écartés** : sous-bloc `❌ Risques écartés : N` rendu si écartés ≥ 1
- [ ] **CA-06 ordre sections** : Pieces (F-194) → Risques retenus (F-195) → Timeline
- [ ] **CA-07 fail-open** : endpoint timeout → section omise, le reste du PDF se génère
- [ ] **CA-08 visuel charte** : palette navy/or, ⚠️ icône modérée (pas dramatisée — risques sont sérieux mais informatifs ici)

---

## Tests Jest (~6)

- `PdfExportServiceTest` :
  - `export(caseFile, synthesis, [], [], [], [])` → PDF sans section risques
  - `export(..., [risqueValide])` → section + libellé + badge outil
  - `export(..., [risqueEcarte])` → section omise (pas de VALIDÉ) MAIS compteur `❌ Risques écartés : 1` si autres VALIDÉ
  - `export(..., [risqueValideHarcelement, risqueEcarteNonConcurrence])` → liste + compteur
  - section insérée APRÈS « Pièces à demander » et AVANT Timeline
  - score validé affiché
- `SynthesisComponentTest` : `RisqueAlignmentService.getForCaseFile` appelé en parallèle

---

## Dépendances

- F-192 SF-192-03 ✅, F-193 SF-193-03, F-194 SF-194-03 (en cours)
- **SF-195-01 backend** + **SF-195-02 frontend**

---

## Notes 2026-05-06

- ⚠️ icône modérée (cohérent DESIGN_SYSTEM.md — rouge réservé alertes critiques)
- Compteur risques écartés sans liste détaillée (cohérent F-194 — éviter d'allonger PDF)
- Score validé clairement labellisé `Score validé : Y / 100` (vs `Score IA brut : X / 100` qui reste sur la grille de badges F-162)
