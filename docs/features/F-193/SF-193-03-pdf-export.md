# Mini-spec — F-193 / SF-193-03 Section "Conformité procédurale validée par votre avocat" en début d'export PDF synthèse

## Identifiant

`F-193 / SF-193-03`

## Feature parente

`F-193` — Matérialisation des points procéduraux F-96 vers outils décisionnels + dashboard + pieces/délais + PDF

## Statut

`draft`

## Date de création

2026-05-06

## Branche Git

`feat/SF-193-03-pdf-export-procedure-checks`

## Pattern de référence

**SF-192-03-pdf-export-strategies-retenues.md** (mergée 2026-05-06, PR #859) — cette SF en est le **jumeau procédural**. Lire SF-192-03 pour le pattern complet (insertion en page 2, pdfmake, fail-open).

---

## Objectif

Ajouter une section dédiée « Conformité procédurale validée par votre avocat » au début de l'export PDF synthèse, listant les checks F-96 avec leur statut (✅ vérifié / ❌ non conforme / ⏳ à vérifier) et leur outil cible.

La section est **insérée immédiatement APRÈS** la section « Stratégies retenues » de F-192 SF-192-03 (donc en page 2-3, avant Timeline / Faits / etc.).

---

## Comportement attendu

### Cas nominal

1. L'avocat clique « Exporter en PDF ». `PdfExportService.export(caseFile, synthesis, retainedPistes?, procedureChecksAlignment?)` est appelé avec un nouveau 4ᵉ paramètre `procedureChecksAlignment?: ProcedureCheckAlignment[]`.
2. `SynthesisComponent.exportPdf()` charge avant export :
   - `RetainedPisteAlignmentService.getForCaseFile(id)` (déjà fait par SF-192-03)
   - **Nouveau** : `ProcedureCheckAlignmentService.getForCaseFile(id)` (introduit par SF-193-02)
   - Timeout 5 s sur chacun (fail-open silencieux → `[]` si erreur)
3. Si ≥ 1 check matérialisé, le PDF inclut une nouvelle section « 🔍 Conformité procédurale validée par votre avocat » insérée APRÈS la section Stratégies retenues, avec le contenu suivant :
   - **Titre de section** navy 16/bold
   - **3 sous-blocs distincts** (selon ce qui s'applique) :
     - **✅ Vérifications confirmées** — liste des checks ALIGNED (statut VERIFIED + matchStatus = ALIGNED), titre or, badge ✅ par item, libellé Inter regular 11 + raison Inter italique 9 grise
     - **❌ Points non conformes** — liste des checks NON_COMPLIANT (matchStatus = NON_COMPLIANT_FLAG), titre rouge subtil, badge ❌ par item, libellé + raison
     - **⏳ Points à vérifier** — liste des checks TO_CHECK (matchStatus = TO_VERIFY_FLAG), titre gris, badge ⏳ par item
   - Pour chaque check, affichage de l'outil cible si `toolIdCible` non null : `→ <label outil>` JetBrains Mono italique 9 (lookup via `TOOL_REGISTRY` du panel F-IA-04, fallback `toolId` brut)
   - Séparateur navy fine entre sous-blocs
   - Page break après la section
4. Si aucun check matérialisé → section omise (comportement actuel inchangé).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Endpoint `/procedure-checks-alignment` 404/500/timeout | Section omise du PDF, log warn console, le reste du PDF est généré normalement (fail-open) |
| Aucun check matérialisé | Section omise (cas nominal) |
| Tous les checks en `NO_TARGET_TOOL` | Section incluse, listant les checks sans suffixe `→ <label outil>` |
| `RetainedPisteAlignmentService` succès mais `ProcedureCheckAlignmentService` échoue | Section pistes (SF-192-03) incluse, section checks omise (les 2 sont indépendantes) |

---

## Critères d'acceptation

- [ ] **CA-01** : sur un dossier avec ≥ 1 check matérialisé, l'export PDF inclut la section « 🔍 Conformité procédurale validée par votre avocat » insérée APRÈS la section Stratégies retenues
- [ ] **CA-02** : sur un dossier sans check matérialisé, le PDF est identique au comportement actuel (aucune section ajoutée)
- [ ] **CA-03 sous-bloc ALIGNED** : check VERIFIED `IM05_MOTIF` → sous-bloc « ✅ Vérifications confirmées » avec libellé + suffixe `→ TITRE DE SÉJOUR RECOMMANDÉ`
- [ ] **CA-04 sous-bloc NON_COMPLIANT** : check NON_COMPLIANT `LICENCIEMENT_NOTIFICATION` → sous-bloc « ❌ Points non conformes » avec libellé + raison (si présente) + suffixe outil
- [ ] **CA-05 sous-bloc TO_VERIFY** : check TO_CHECK → sous-bloc « ⏳ Points à vérifier »
- [ ] **CA-06 NO_TARGET_TOOL** : check sans toolIdCible → libellé sans suffixe outil
- [ ] **CA-07 mix** : combinaison de plusieurs statuts → 2 ou 3 sous-blocs affichés
- [ ] **CA-08 ordre des sections** : « Stratégies retenues » F-192 puis « Conformité procédurale » F-193 puis Timeline / Faits / etc.
- [ ] **CA-09 fail-open** : endpoint timeout 5 s → section omise, le reste du PDF se génère
- [ ] **CA-10 fail-open indépendant** : section pistes succès + section checks échec → PDF avec pistes seulement (et inverse)
- [ ] **CA-11 visuel charte** : palette navy/or DESIGN_SYSTEM.md, rouge subtil pour ❌, JetBrains Mono italique pour le suffixe outil
- [ ] **CA-12 nom de fichier inchangé** : pas de suffixe spécifique

---

## Périmètre

### Hors scope (explicite)

- (a) Export PDF dédié aux seuls procedure_checks
- (b) Personnalisation par l'avocat de l'inclusion par check
- (c) Section générée en cas où **seuls** les checks NON_COMPLIANT existent (toujours afficher les 3 sous-blocs si applicables)
- (d) Hyperliens cliquables vers le bloc Checklist de la synthèse en ligne
- (e) Inclusion du `expectedValue` (champ technique, pas pertinent pour le rendu PDF)

---

## Technique

### Composants Angular impactés

- `PdfExportService.export(caseFile, synthesis, retainedPistes?, procedureChecksAlignment?)` (signature étendue, 4ᵉ paramètre optionnel)
- `PdfExportService.buildProcedureChecksSection(procedureChecksAlignment): Content[]` (nouvelle méthode privée)
- `SynthesisComponent.exportPdf()` (étendu) — appel `ProcedureCheckAlignmentService.getForCaseFile(id)` avant `pdfExportService.export(...)`, passe le résultat en 4ᵉ argument
- `ProcedureCheckAlignmentService` (réutilisé de SF-193-02)
- Lookup label outil : réutilise le helper `resolveToolLabel()` introduit par SF-192-03 (extension du helper si besoin)

### Migration

- [x] Aucune

---

## Plan de test

### Tests Jest (8-10 tests)

- `PdfExportServiceTest` :
  - `export(caseFile, synthesis, [], [])` → PDF sans section procedure_checks
  - `export(caseFile, synthesis, [], [checkAligned])` → section + sous-bloc ✅ avec suffixe outil
  - `export(caseFile, synthesis, [], [checkNonCompliant])` → section + sous-bloc ❌
  - `export(caseFile, synthesis, [], [checkToVerify])` → section + sous-bloc ⏳
  - `export(caseFile, synthesis, [], [checkNoTargetTool])` → section + libellé sans suffixe
  - `export(caseFile, synthesis, [], [aligned, nonCompliant, toVerify])` → 3 sous-blocs simultanés
  - section insérée APRÈS « Stratégies retenues » et AVANT Timeline (vérifier index dans `content[]`)
  - palette : titre ✅ or, ❌ rouge subtil, ⏳ gris ; suffixe outil JetBrains Mono italique
- `SynthesisComponentTest` :
  - clic export → `ProcedureCheckAlignmentService.getForCaseFile` appelé en parallèle de `RetainedPisteAlignmentService.getForCaseFile`
  - timeout `procedureChecksAlignment` → export quand même appelé avec `[]` en 4ᵉ argument

### Isolation workspace

- [x] Non applicable côté frontend pur

---

## Dépendances

- F-96 ✅ Terminée
- F-192 SF-192-03 ✅ Terminée (`PdfExportService.export` signature étendue à 3 args ; cette SF passe à 4 args)
- **SF-193-01 backend** — endpoint `/procedure-checks-alignment` requis
- **SF-193-02 frontend** — `ProcedureCheckAlignmentService` + `ProcedureCheckAlignment` modèle requis

---

## Notes et décisions

- **Décision 2026-05-06** : section insérée APRÈS « Stratégies retenues » F-192 et AVANT Timeline — l'ordre reflète la priorité de lecture pour le client (stratégie → conformité → faits du dossier).
- **Décision 2026-05-06** : 3 sous-blocs séparés (✅ / ❌ / ⏳) avec couleurs distinctes plutôt qu'une liste mixte — meilleure lisibilité PDF.
- **Décision 2026-05-06** : V1 affiche les checks NO_TARGET_TOOL sans suffixe outil plutôt que de les omettre — privilégie la visibilité des choix avocat sur la cohérence d'alignement.
- **Décision 2026-05-06** : fail-open indépendant entre les 2 sections (pistes F-192 + checks F-193) — l'échec d'une n'empêche pas l'autre.
