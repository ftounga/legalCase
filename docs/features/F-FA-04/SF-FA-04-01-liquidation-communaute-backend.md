# Mini-spec — F-FA-04 / SF-FA-04-01 Liquidation de communauté — backend extraction + prompt

## Identifiant

`F-FA-04 / SF-FA-04-01`

## Feature parente

`F-FA-04` — Synthèse liquidation de communauté

## Statut

`in-progress`

## Date de création

2026-04-04

## Branche Git

`feat/SF-FA-04-01-liquidation-communaute-backend`

---

## Objectif

Enrichir le prompt DROIT_FAMILLE avec les données de liquidation de communauté, extraire un `LiquidationCommunauteResult` structuré depuis le JSON IA, et l'exposer dans `CaseAnalysisResponse`.

---

## Comportement attendu

Pas de calculateur : l'IA inventorie les biens depuis les documents. Le backend structure et transmet.

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| `liquidation_communaute_data` absent | `null` dans la réponse |
| Listes vides | Retournées vides (fail-open) |
| `valeur_estimee` null | `BienItem(libelle, null)` |

---

## Critères d'acceptation

- [ ] Prompt DROIT_FAMILLE enrichi avec `liquidation_communaute_data` (4 listes + régime)
- [ ] `LiquidationCommunauteResult` record avec `BienItem` imbriqué
- [ ] `extractLiquidationCommunaute()` fail-open dans `CaseAnalysisResponse`
- [ ] Champ `liquidationCommunaute` dans `CaseAnalysisResponse`
- [ ] 8 tests unitaires

---

## Périmètre

### Hors scope

- Calcul de la part de chaque époux (dépend du régime et de la valorisation)
- Export Word
- Affichage frontend (SF-FA-04-02)

---

## Technique

### Pas de migration Liquibase

### Record

```java
public record LiquidationCommunauteResult(
    String regimeMatrimonial,
    List<BienItem> actifCommun,
    List<BienItem> biensPropresEpouxA,
    List<BienItem> biensPropresEpouxB,
    List<BienItem> passifCommun
) {
    public record BienItem(String libelle, Double valeur) {}
}
```

---

## Plan de test

- [ ] JSON complet → result non null, toutes listes peuplées
- [ ] `valeur_estimee` null → `BienItem(libelle, null)`
- [ ] Champ absent → null
- [ ] Prompt DROIT_FAMILLE → contient `liquidation_communaute_data`
- [ ] Listes vides → retournées vides
- [ ] `regimeMatrimonial` null → conservé null
- [ ] Item malformé ignoré (fail-open)
- [ ] `CaseAnalysisResponse.from()` — JSON avec données → champ non null

---

## Analyse d'impact

- [x] **Aucune préoccupation transversale**

---

## Dépendances

- SF-FA-04-02 dépend de SF-FA-04-01
