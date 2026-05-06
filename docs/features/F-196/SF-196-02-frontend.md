# Mini-spec — F-196 / SF-196-02 Frontend — Tile dashboard questions complémentaires + visibility outils

## Identifiant

`F-196 / SF-196-02`

## Statut

`draft` — 2026-05-06

## Branche Git

`feat/SF-196-02-frontend-questions-tile`

## Pattern de référence

**SF-194-02 + SF-195-02**. Plus simple car pas de nouvelle UI markable (F-94 existe déjà).

## Contrat API

- `GET /api/v1/case-files/{id}/ai-questions-alignment` → `AiQuestionAlignment[]`
- Tile `{ toolId: 'F-196-questions-summary', theme: 'DOCUMENTS', label, primaryValue, secondaryValue?, alertLevel? }`

---

## Objectif

Pas de modification du `SynthesisComponent` côté UI réponses (F-94 reste). Ajouts : (1) tile dashboard `F-196-questions-summary` ; (2) signal optionnel `aiQuestionsAlignment` propagé via TOOL_REGISTRY pour les outils décisionnels qui pourraient bénéficier de l'info.

---

## Comportement attendu

1. `AiQuestionAlignmentService.getForCaseFile(id)` au montage, signal cache.
2. Tile `F-196-questions-summary` rendue, clic → scroll vers bloc Questions de la synthèse.
3. **Optionnel** : extension TOOL_REGISTRY pour outils qui consomment déjà F-IA-03 (la majorité — ne rien faire, F-IA-03 suffit).
4. Refresh uniquement au SSE `ENRICHED_ANALYSIS DONE`.

---

## Critères d'acceptation

- [ ] **CA-01** : tile thème DOCUMENTS rendue avec primary/secondary/alertLevel corrects
- [ ] **CA-02** : clic tile → navigation vers `/case-files/{id}/synthesis#section-questions`
- [ ] **CA-03 fail-open** : timeout endpoint → tile absente, pas d'erreur UI
- [ ] **CA-04 OnPush + markForCheck**

---

## Hors scope V1

- (a) UI markable supplémentaire (F-94 reste tel quel)
- (b) Badge sur card panel F-IA-04 (F-IA-03 alertes existantes suffisent)
- (c) Modification du flux F-94

---

## Composants impactés

- `AiQuestionAlignmentService` (nouveau) + modèle `ai-question-alignment.model.ts`
- `<app-case-dashboard>` mapping toolId `F-196-questions-summary`

---

## Tests Jest (~5)

- Service (3 : success / 404 / 500 fail-open)
- CaseDashboard tile + clic (2)

---

## Dépendances

- F-94 ✅
- F-IA-03 ✅
- **SF-196-01 backend**

---

## Notes 2026-05-06

- Charge minimale (~0.5 j) — gap réduit vs autres F-19X
- Pas de modification UI bloc Questions (F-94 reste)
