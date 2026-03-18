# Mini-spec — F-10 / SF-10-03 — Ajout timeline au prompt CaseAnalysis

## Identifiant
`F-10 / SF-10-03`

## Feature parente
`F-10` — Analyse IA — dossier

## Statut
`in-progress`

## Date de création
`2026-03-18`

## Branche Git
`feat/SF-10-03-timeline-prompt`

---

## Objectif

Ajouter un champ `timeline` au format JSON produit par le LLM lors de la synthèse globale du dossier, afin que F-12 (Restitution de l'analyse) puisse afficher une frise chronologique des événements clés.

---

## Comportement attendu

### Format JSON attendu du LLM (après modification)

```json
{
  "timeline": [
    {"date": "2024-01-15", "evenement": "Signature du contrat de travail"},
    {"date": "2024-06-01", "evenement": "Premier avertissement écrit"},
    {"date": "2024-09-15", "evenement": "Notification de licenciement"}
  ],
  "faits": ["..."],
  "points_juridiques": ["..."],
  "risques": ["..."],
  "questions_ouvertes": ["..."]
}
```

- `timeline` : tableau d'objets `{"date": "...", "evenement": "..."}`, triés chronologiquement
- `date` : format ISO 8601 (`YYYY-MM-DD`) ou mention approximative si la date exacte est inconnue (ex : `"2024-Q1"`)
- `evenement` : description courte de l'événement (max ~100 caractères)
- Si aucun événement datable n'est identifiable → `"timeline": []`

### Backward compatibility

La table `case_analyses.analysis_result` stocke du JSON texte libre — pas de migration nécessaire. Les anciennes analyses (sans `timeline`) restent lisibles ; F-12 gérera l'absence du champ côté frontend.

---

## Critères d'acceptation

- [x] `SYSTEM_PROMPT` de `CaseAnalysisService` inclut `timeline` dans le format attendu
- [x] Le prompt précise le format `{"date": "...", "evenement": "..."}` et l'ordre chronologique
- [x] Tests unitaires mis à jour pour vérifier que le prompt contient `timeline`
- [x] Aucune migration Liquibase nécessaire

---

## Périmètre

### Hors scope (explicite)

- Modification des prompts `ChunkAnalysisService` ou `DocumentAnalysisService`
- Parsing ou affichage du champ `timeline` (F-12)
- Migration des anciennes `case_analyses`

---

## Technique

### Fichiers modifiés

| Fichier | Modification |
|---------|-------------|
| `CaseAnalysisService.java` | Mise à jour de `SYSTEM_PROMPT` |
| `CaseAnalysisServiceTest.java` | Mise à jour des assertions sur le prompt |

### Migration Liquibase
Non applicable.

---

## Plan de test

### Tests unitaires

- [x] `SYSTEM_PROMPT` contient `"timeline"`
- [x] `SYSTEM_PROMPT` contient `"date"` et `"evenement"`
- [x] Tests existants (U-01 à U-04) toujours verts

---

## Dépendances

### Subfeatures bloquantes
Aucune.

### Débloque
- F-12 / SF-12-01 (API case-analysis)
- F-12 / SF-12-02 (frontend synthèse)

---

## Notes et décisions

- Le LLM est instruit de produire `timeline: []` si aucune date n'est identifiable — évite les `null` côté frontend
- Format ISO 8601 recommandé mais non obligatoire (les documents juridiques ont souvent des dates approximatives)
