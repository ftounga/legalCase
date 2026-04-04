# Mini-spec — F-FA-01 / SF-FA-01-01 Prestation compensatoire — backend calculator + prompt

## Identifiant

`F-FA-01 / SF-FA-01-01`

## Feature parente

`F-FA-01` — Calcul de la prestation compensatoire

## Statut

`in-progress`

## Date de création

2026-04-04

## Branche Git

`feat/SF-FA-01-01-prestation-compensatoire-backend`

---

## Objectif

Ajouter un `PrestationCompensatoireCalculator` basé sur les critères de l'art. 271 Cciv (écart de revenus, durée du mariage), enrichir le prompt DROIT_FAMILLE avec les champs `prestation_compensatoire_data`, et exposer le résultat dans `CaseAnalysisResponse.prestationCompensatoireEstimate`.

---

## Formule appliquée

Base mensuelle = (revenus_A - revenus_B) × coeff
Montant capitalisé = base × 12 × durée_référence (8 ans)

Coefficients : France 0.30 / Belgique 0.25
Durée de référence : 8 ans (convention indicative)
Fourchette ±15 % (montantMin / montantMax)
Si revenus identiques → montants à 0, donneesPartielles = false

---

## Comportement attendu

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Revenus A et B null ET durée null | `Optional.empty()` |
| Un champ manquant | `donneesPartielles = true` |
| `pays_applicable` non reconnu | FRANCE (fail-open) |

---

## Critères d'acceptation

- [ ] `PrestationCompensatoireCalculator.calculate()` retourne les bons montants France et Belgique
- [ ] `Optional.empty()` si données totalement absentes
- [ ] `donneesPartielles = true` si champ manquant
- [ ] Prompt DROIT_FAMILLE enrichi avec `prestation_compensatoire_data` (5 champs)
- [ ] `CaseAnalysisResponse` expose `prestationCompensatoireEstimate`
- [ ] `extractPrestationCompensatoireEstimate()` fail-open

---

## Périmètre

### Hors scope

- Affichage frontend (SF-FA-01-02)
- Prise en compte du patrimoine (trop complexe sans données structurées)
- Prestation en rente (uniquement capital)

---

## Technique

### Pas de migration Liquibase

Résultat calculé à la volée, comme `compensationEstimate` et `pensionAlimentaireEstimate`.

### Record

```java
public record PrestationCompensatoireEstimate(
    double montantMin,
    double montantMax,
    double ecartRevenus,
    int dureeMarriage,
    String pays,
    boolean donneesPartielles
) {}
```

---

## Plan de test

- [ ] France données complètes → montants corrects
- [ ] Belgique coeff 0.25 → différent de France
- [ ] Revenus identiques → montants à 0
- [ ] Données partielles → `donneesPartielles = true`
- [ ] Revenus + durée null → `Optional.empty()`
- [ ] Pays inconnu → FRANCE (fail-open)
- [ ] Durée null → `donneesPartielles = true`
- [ ] Prompt DROIT_FAMILLE → contient `prestation_compensatoire_data`
- [ ] JSON complet → estimate non null
- [ ] Champ absent → null

---

## Analyse d'impact

- [x] **Aucune préoccupation transversale**

---

## Dépendances

- SF-FA-01-02 dépend de SF-FA-01-01
