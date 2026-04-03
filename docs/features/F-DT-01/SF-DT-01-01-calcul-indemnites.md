# Mini-spec — F-DT-01 / SF-DT-01-01 Calcul automatique des indemnités de rupture

---

## Identifiant

`F-DT-01 / SF-DT-01-01`

## Feature parente

`F-DT-01` — Calcul d'indemnités automatique (droit du travail)

## Statut

`ready`

## Date de création

2026-04-04

## Branche Git

`feat/SF-DT-01-01-calcul-indemnites`

---

## Objectif

À partir des données extraites par l'IA (ancienneté, salaire de référence, type de rupture), calculer les indemnités légales de rupture et le plafond prud'homal (barème Macron), et afficher le résultat dans la synthèse et dans l'export PDF.

---

## Comportement attendu

### Cas nominal

1. Le prompt enrichi demande à l'IA de renseigner un objet `compensation_data` dans le JSON de réponse.
2. `CaseAnalysisResponse.from()` parse `compensation_data` et appelle `CompensationCalculator.calculate()`.
3. Le résultat (`CompensationEstimate`) est inclus dans `CaseAnalysisResponse` comme champ `compensationEstimate`.
4. Le frontend affiche un panneau "Indemnités estimées" dans la synthèse (après les pièces manquantes, avant la checklist).
5. L'export PDF inclut la section indemnités dans `buildSections()`.

### Formule légale

**Indemnité légale (Art. R1234-2 Code du travail) :**
- 1/4 de mois de salaire de référence par année de présence pour les 10 premières années
- 1/3 de mois au-delà de 10 ans
- Les mois sont inclus au prorata : `anciennetéTotale = annees + mois / 12`
- Valeur 0 si ancienneté < 1 an

**Types de rupture couverts :**

| `type_rupture` IA | Formule | Article |
|---|---|---|
| `LICENCIEMENT` | 1/4 puis 1/3 | Art. R1234-2 |
| `LICENCIEMENT_ECONOMIQUE` | Même base | Art. R1234-2 |
| `RUPTURE_CONVENTIONNELLE` | Même base | Art. L1237-19 |

**Barème Macron (plafond D&I, entreprises ≥ 11 salariés) :**

| Ancienneté (années entières) | Min (mois) | Max (mois) |
|---|---|---|
| < 1 | 0 | 1 |
| 1 | 1 | 2 |
| 2 | 3 | 3.5 |
| 3-4 | 3 | 4-5 |
| 5-9 | 3 | 6-9 |
| 10-29 | 3 | 10-19 |
| ≥ 30 | 3 | 20 |

### Contenu de l'objet `compensation_data` (JSON enrichi)

```json
{
  "compensation_data": {
    "type_rupture": "LICENCIEMENT" | "LICENCIEMENT_ECONOMIQUE" | "RUPTURE_CONVENTIONNELLE" | null,
    "anciennete_annees": 6,
    "anciennete_mois": 4,
    "salaire_reference_mensuel": 2800.00
  }
}
```

### Affichage frontend

```
┌─ Indemnités estimées ─────────────────────────────────────────┐
│  Indemnité légale de licenciement          8 050 €             │
│  Ancienneté : 6 ans 4 mois                                     │
│  Salaire de référence : 2 800 €/mois                           │
│                                                                 │
│  Plafond prud'homal (barème Macron)        3 — 7 mois          │
│  (soit 8 400 € — 19 600 €)                                     │
│                                                                 │
│  ⚠ Estimation indicative — données extraites par l'IA          │
└─────────────────────────────────────────────────────────────────┘
```

Si `donnees_partielles = true` (données incomplètes) → avertissement visible.
Si `compensationEstimate` est null → panneau non affiché.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `compensation_data` absent du JSON | `compensationEstimate = null` — panneau non affiché |
| `type_rupture` null ou non reconnu | `compensationEstimate = null` — pas de calcul |
| `salaire_reference_mensuel` absent ou ≤ 0 | `donnees_partielles = true`, calcul quand même si ancienneté disponible, sinon null |
| `anciennete_annees` absent | `donnees_partielles = true` si salaire présent, sinon null |
| Workspace non DROIT_DU_TRAVAIL | Prompt n'inclut pas `compensation_data` — null naturellement |

---

## Critères d'acceptation

- [ ] `CompensationCalculator` applique correctement la formule 1/4 + 1/3 (10 ans pivot)
- [ ] Le barème Macron retourne min/max corrects pour chaque tranche d'ancienneté
- [ ] Le panneau est affiché uniquement si `compensationEstimate` est non null
- [ ] `donnees_partielles = true` déclenche un avertissement visible dans le panneau
- [ ] L'export PDF inclut la section indemnités si `compensationEstimate` non null
- [ ] Aucune migration DB — la donnée est calculée depuis le JSON de l'analyse existant
- [ ] Isolation workspace garantie — l'estimate est lié à un `CaseAnalysis` déjà isolé

---

## Périmètre

### Hors scope

- Calcul pour d'autres domaines (immigration, famille)
- Prise en compte de l'ancienneté dans plusieurs entreprises
- Calcul des indemnités compensatrices de préavis et congés payés
- Indemnité spécifique harcèlement/discrimination (F-DT-02 abandonné)
- Saisie manuelle des paramètres de calcul par l'avocat (V2)
- Barème pour entreprises < 11 salariés (V2)

---

## Technique

### Endpoint(s)

Aucun nouveau — le champ `compensationEstimate` est ajouté à la réponse `GET /api/v1/case-files/{id}/analyses/{version}` existante.

### Nouveaux composants

| Composant | Rôle |
|---|---|
| `CompensationCalculator` (backend) | Calcule indemnité légale + barème Macron à partir de type, ancienneté, salaire |
| `CompensationEstimate` (backend record) | `indemnite`, `salaireReference`, `ancienneteAnnees`, `ancienneteMois`, `typeRupture`, `plafondMinMois`, `plafondMaxMois`, `donneesPartielles` |

### Composants modifiés

| Composant | Modification |
|---|---|
| `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE` | Ajout instruction + champ `compensation_data` dans le format JSON attendu |
| `CaseAnalysisResponse` | Ajout champ `compensationEstimate` + parsing + appel `CompensationCalculator` |
| `case-analysis.model.ts` | Ajout interface `CompensationEstimate` + champ optionnel dans `CaseAnalysisResult` |
| `synthesis.component.html` | Nouveau panneau `mat-expansion-panel` "Indemnités estimées" |
| `pdf-export.service.ts` | Nouvelle section dans `buildSections()` si `compensationEstimate` non null |

### Tables impactées

Aucune — pas de nouvelle table.

### Migration Liquibase

- [x] Non applicable

---

## Plan de test

### Tests unitaires

- [ ] `CompensationCalculator` — licenciement < 10 ans : formule 1/4 correcte
- [ ] `CompensationCalculator` — licenciement > 10 ans : formule 1/4 + 1/3 correcte
- [ ] `CompensationCalculator` — ancienneté < 1 an → indemnité 0
- [ ] `CompensationCalculator` — barème Macron : chaque tranche retourne min/max correct
- [ ] `CompensationCalculator` — `type_rupture` non reconnu → retourne null
- [ ] `CaseAnalysisResponse` — JSON avec `compensation_data` valide → `compensationEstimate` non null
- [ ] `CaseAnalysisResponse` — JSON sans `compensation_data` → `compensationEstimate` null
- [ ] `PdfExportService` — `buildSections()` inclut section indemnités si non null
- [ ] `PdfExportService` — `buildSections()` n'inclut pas section si null

### Tests d'intégration

- [ ] Non applicable — endpoint existant, pas de nouveau

### Isolation workspace

- [x] Non applicable — `compensationEstimate` dérivé d'un `CaseAnalysis` déjà isolé par workspace

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — ajout d'un champ dans la réponse existante + nouveau panneau UI, aucune modification auth/routing/workspace/plans.

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné

---

## Dépendances

### Subfeatures bloquantes

- F-97 (pipeline IA enrichi) — statut : **done**
- SF-DT-03-01 (prompt enrichi avec `type_litige_detecte`) — statut : **done** (prompt déjà étendu, même pattern)

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Pas de persistance DB : le calcul est déterministe depuis le JSON → recalculé à chaque appel. Pas de risque de données périmées.
- `compensation_data` est ajouté au prompt enrichi uniquement (pas initial) — l'estimation n'a de sens qu'avec une synthèse complète.
- L'avertissement "Estimation indicative" est toujours affiché — même si données complètes, il s'agit d'un calcul IA non certifié.
- `donnees_partielles` est `true` si l'une des données clés (ancienneté ou salaire) est manquante ou invalide.
