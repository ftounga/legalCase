# Mini-spec — F-FA-02 / SF-FA-02-01 Pension alimentaire — backend calculator + prompt

## Identifiant

`F-FA-02 / SF-FA-02-01`

## Feature parente

`F-FA-02` — Grille pension alimentaire

## Statut

`in-progress`

## Date de création

2026-04-04

## Branche Git

`feat/SF-FA-02-01-pension-alimentaire-backend`

---

## Objectif

Ajouter un `PensionAlimentaireCalculator` basé sur le barème UNAF simplifié (France) et un barème de référence belge (Belgique), enrichir le prompt DROIT_FAMILLE pour extraire les données nécessaires depuis les documents, et exposer le résultat dans `CaseAnalysisResponse` via le champ `pensionAlimentaireEstimate`.

---

## Comportement attendu

### Cas nominal

1. L'IA analyse un dossier DROIT_FAMILLE contenant des éléments relatifs à une pension alimentaire.
2. Le prompt enrichi DROIT_FAMILLE demande à l'IA d'extraire : `revenus_net_mensuel_debiteur`, `revenus_net_mensuel_creancier`, `nb_enfants`, `mode_garde` (`EXCLUSIVE` | `ALTERNEE`), `pays_applicable` (`FRANCE` | `BELGIQUE`).
3. `CaseAnalysisService` et `EnrichedAnalysisService` appellent `PensionAlimentaireCalculator.calculate(revenus, nbEnfants, modeGarde, pays)`.
4. Le résultat `PensionAlimentaireEstimate` est inclus dans `CaseAnalysisResponse.pensionAlimentaireEstimate`.
5. Si les données sont partielles (revenus nuls ou nb_enfants null), `donneesPartielles = true`, le calcul reste retourné avec les données disponibles.

### Barème appliqué

**France (UNAF simplifié — pourcentage du revenu net mensuel du débiteur) :**

| Nb enfants | Garde exclusive | Garde alternée |
|-----------|----------------|----------------|
| 1         | 18 %           | 11 %           |
| 2         | 26 %           | 16 %           |
| 3         | 30 %           | 19 %           |
| 4         | 33 %           | 21 %           |
| 5+        | 35 %           | 22 %           |

**Belgique (table de référence CGKR simplifiée) :**

| Nb enfants | Garde exclusive | Garde alternée |
|-----------|----------------|----------------|
| 1         | 15 %           | 9 %            |
| 2         | 22 %           | 14 %           |
| 3         | 27 %           | 17 %           |
| 4         | 31 %           | 19 %           |
| 5+        | 33 %           | 21 %           |

Le montant calculé est une fourchette indicative ± 10 % (`montantMin` / `montantMax`).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `nb_enfants` null ET `revenus_net_mensuel_debiteur` null | `Optional.empty()` — pas de champ dans la réponse |
| `nb_enfants` présent mais `revenus` null | Retourné avec `donneesPartielles = true`, montants à 0 |
| `mode_garde` non reconnu | Traité comme `EXCLUSIVE` (fail-open) |
| `pays_applicable` non reconnu | Traité comme `FRANCE` (fail-open) |
| Dossier non DROIT_FAMILLE | Prompt non enrichi, champ absent de la réponse |

---

## Critères d'acceptation

- [ ] `PensionAlimentaireCalculator.calculate()` retourne les bons montants pour France et Belgique, garde exclusive et alternée
- [ ] `PensionAlimentaireCalculator.calculate()` retourne `Optional.empty()` si données totalement absentes
- [ ] Prompt DROIT_FAMILLE étendu dans `LegalDomainPromptBuilder` (5 champs)
- [ ] `CaseAnalysisResponse` expose `pensionAlimentaireEstimate` (null si absent)
- [ ] `CaseAnalysisService` appelle le calculator et remplit le champ
- [ ] `EnrichedAnalysisService` appelle le calculator et remplit le champ
- [ ] `donneesPartielles = true` si au moins une donnée manque

---

## Périmètre

### Hors scope

- Affichage frontend (SF-FA-02-02)
- Export PDF (SF-FA-02-02)
- Calcul basé sur les revenus du créancier (pris en compte dans une V5 si besoin)
- Barème complet UNAF (tables détaillées par tranches de revenu) — on utilise le barème simplifié en pourcentage

---

## Technique

### Pas de migration Liquibase

Aucune nouvelle table — le résultat est calculé à la volée et retourné dans la réponse JSON, comme `compensationEstimate`.

### Classes créées / modifiées

| Classe | Opération | Package |
|--------|-----------|---------|
| `PensionAlimentaireCalculator` | CREATE | `fr.ailegalcase.analysis` |
| `LegalDomainPromptBuilder` | MODIFY — ajout instruction DROIT_FAMILLE | `fr.ailegalcase.analysis` |
| `CaseAnalysisResponse` | MODIFY — champ `pensionAlimentaireEstimate` | `fr.ailegalcase.analysis` |
| `CaseAnalysisService` | MODIFY — appel calculator + affectation | `fr.ailegalcase.analysis` |
| `EnrichedAnalysisService` | MODIFY — appel calculator + affectation | `fr.ailegalcase.analysis` |

### Record `PensionAlimentaireEstimate`

```java
public record PensionAlimentaireEstimate(
    double montantMin,
    double montantMax,
    double revenus,
    int nbEnfants,
    String modeGarde,   // "EXCLUSIVE" | "ALTERNEE"
    String pays,        // "FRANCE" | "BELGIQUE"
    boolean donneesPartielles
) {}
```

---

## Plan de test

### Tests unitaires

- [ ] `PensionAlimentaireCalculator` — France, garde exclusive, 1/2/3/4/5 enfants → montants corrects
- [ ] `PensionAlimentaireCalculator` — France, garde alternée, 2 enfants → montants corrects
- [ ] `PensionAlimentaireCalculator` — Belgique, garde exclusive, 2 enfants → montants corrects
- [ ] `PensionAlimentaireCalculator` — nb_enfants null ET revenus null → `Optional.empty()`
- [ ] `PensionAlimentaireCalculator` — nb_enfants présent, revenus null → `donneesPartielles = true`
- [ ] `PensionAlimentaireCalculator` — mode_garde inconnu → traité comme EXCLUSIVE
- [ ] `PensionAlimentaireCalculator` — pays inconnu → traité comme FRANCE
- [ ] `LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE")` → contient les 5 champs
- [ ] `CaseAnalysisService` — JSON DROIT_FAMILLE avec données complètes → `pensionAlimentaireEstimate` non null
- [ ] `CaseAnalysisService` — JSON non DROIT_FAMILLE → `pensionAlimentaireEstimate` null

### Isolation workspace

- [ ] Non applicable — aucune table touchée

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — extension isolée du calculator et du prompt, pas de route ni de table modifiée

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné

---

## Dépendances

- F-FA-02 (feature parente) — à spécifier ✅ (cette mini-spec suffit)
- SF-FA-02-02 — dépend de SF-FA-02-01

---

## Notes et décisions

- Pattern identique à `CompensationCalculator` (F-DT-01-01) : classe statique pure, `Optional.empty()` si données absentes, `donneesPartielles` flag.
- Barème UNAF simplifié (pourcentage fixe par enfant) : défendable pour un outil indicatif à destination des avocats. Pas une décision juridique — afficher un avertissement côté frontend.
- Le champ `pays_applicable` est séparé de `country` du workspace pour couvrir les dossiers transfrontaliers (ex: ressortissant belge en France).
