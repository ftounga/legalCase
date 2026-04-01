# Mini-spec — F-93 / SF-93-01 : Traçabilité des sources IA — Backend

## Identifiant
`F-93 / SF-93-01`

## Feature parente
`F-93` — Traçabilité des sources IA

## Statut
`done`

## Date de création
`2026-04-01`

## Branche Git
`feat/SF-93-01-source-tracing-backend`

---

## Objectif

Modifier le pipeline IA pour que chaque fait, risque et point juridique cite le document source et un extrait justificatif. Le prompt inclut les noms de fichiers, et le JSON retourné contient des objets `{texte, source, extrait}`.

---

## Comportement attendu

### Cas nominal

1. `buildAggregatedPrompt()` : `Document N (nom.pdf) : <analyse>`
2. System prompt : format `{"texte": "...", "source": "Document N", "extrait": "..."}` pour faits/risques/points_juridiques
3. `CaseAnalysisResponse` : parse fail-open — objet → AnalysisItem complet ; string → `AnalysisItem(string, null, null)`
4. API retourne `faits: [{texte, source, extrait}]`

### Cas dégradés

| Situation | Comportement attendu |
|-----------|---------------------|
| Item string (ancienne analyse) | `AnalysisItem(texte=string, source=null, extrait=null)` |
| Item objet sans source | `source = null` |
| Item objet malformé | Fallback string |

---

## Critères d'acceptation

- [x] `buildAggregatedPrompt()` inclut `originalFilename`
- [x] System prompts CaseAnalysisService + EnrichedAnalysisService mis à jour
- [x] `AnalysisItem` record créé
- [x] `CaseAnalysisResponse` : faits/risques/pointsJuridiques → `List<AnalysisItem>`, parse fail-open
- [x] `AnalysisJsonTruncator` : tronque les arrays (pas de changement de logique)

---

## Plan de test

- [x] Parse item string → AnalysisItem(texte, null, null)
- [x] Parse item objet complet → AnalysisItem(texte, source, extrait)
- [x] Parse item objet partiel (source absente) → source null
- [x] buildAggregatedPrompt inclut le filename
- [x] AnalysisJsonTruncator — items objets tronqués correctement

---

## Périmètre hors scope

- Affichage frontend — SF-93-02
- PDF export — SF-93-02
- timeline/questionsOuvertes/piecesManquantes/pointsProcedure restent strings
