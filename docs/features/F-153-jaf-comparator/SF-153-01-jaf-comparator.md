# Mini-spec — F-153 / SF-153-01 Comparateur fourchettes JAF famille

## Identifiant · `F-153 / SF-153-01`
## Date · `2026-04-23` · Branche · `feat/SF-153-01-jaf-comparator`

## Objectif
Ajouter des **fourchettes jurisprudentielles JAF** (p25/p50/p75) aux calculateurs existants F-FA-01 (prestation compensatoire), F-FA-02 (pension alimentaire), F-FA-05 (soulte partage). Permet à l'avocat de situer la valeur calculée vs ce qui est habituellement accordé par les JAF pour un profil similaire.

## Contexte
Niveau 6 de la hiérarchie — équivalent F-DT-09 (comparateur indemnités Macron + fourchette CCT 109) pour le domaine famille. Livré en dernière feature du bloc de rattrapage de parité (F-150 → F-153).

## Comportement nominal

### A — Enrichissement des 3 calculateurs existants
Les records `PensionAlimentaireEstimate`, `PrestationCompensatoireEstimate` et — **nouveau** — `PartageImmobilierResult` reçoivent un champ optionnel `jurisprudenceRange: JurisprudenceRange`.

```java
public record JurisprudenceRange(
    Integer p25,
    Integer p50,
    Integer p75,
    String label,      // "Fourchette observée JAF (France)"
    String sourceRef   // ex. "Statistiques ministère Justice 2023 — divorces contentieux JAF"
) {}
```

### B — Tables de référence statiques
`JafReferenceTable` fournit les percentiles selon le profil, en Java (pas de migration DB — données figées de la V1).

Pour **pension alimentaire** : fonction des revenus débiteur, nb enfants, mode de garde, pays FR/BE. Fourchettes dérivées des barèmes UNAF/CGKR + dispersion observée.

Pour **prestation compensatoire** : fonction de l'écart de revenus mensuel × durée mariage. Matrice simplifiée (5 paliers revenus × 4 paliers durée) × pays.

Pour **soulte partage** : fonction de la valeur vénale, non applicable (la soulte est un calcul arithmétique, pas une zone de variation jurisprudentielle) → **pas de fourchette pour ce calculateur**. On garde la portée aux 2 calculateurs où ça fait sens.

### C — Intégration backend
Les 3 services (`PensionAlimentaireCalculator`, `PrestationCompensatoireCalculator`) enrichissent leur résultat via `JafReferenceTable.rangeFor(...)`. Les calculateurs existants ne sont pas cassés (nouveau champ nullable).

### D — Frontend
Les panneaux existants `SynthesisComponent` pour pension et prestation compensatoire reçoivent une **ligne "Fourchette JAF"** sous la valeur calculée :

```
Fourchette observée JAF  |  200 € (p25) — 350 € (médiane) — 520 € (p75)
```

Si `jurisprudenceRange == null` → ligne cachée (rétrocompat dossiers pré-F-153).

## Critères d'acceptation
- [ ] Record `JurisprudenceRange`
- [ ] `JafReferenceTable` statique (FR + BE) pour pension + prestation
- [ ] `PensionAlimentaireEstimate` / `PrestationCompensatoireEstimate` : +champ `jurisprudenceRange` nullable + constructeurs rétrocompat
- [ ] Calculateurs peuplent le champ
- [ ] Interface TS + ligne de rendu dans synthesis
- [ ] Tests backend (table lookup + intégration calculateurs)
- [ ] Tests frontend rendu conditionnel
- [ ] Full suites vertes

## Plan de test minimal
**Backend :**
- U-01 : pension alim — 3 000 € revenus / 2 enfants / garde exclusive / FR → range p25/p50/p75 cohérent (p25 < p50 < p75)
- U-02 : prestation compensatoire — écart 2000 €/mois × 15 ans mariage / FR → range cohérent
- U-03 : pays BE → table différente (ordres de grandeur distincts)
- U-04 : profil hors table (ex. 50 000 € revenus) → range null (pas d'extrapolation hasardeuse)
- U-05 : récompat constructeur sans range

**Frontend :**
- U-06 : panneau pension avec range → ligne "Fourchette JAF" visible
- U-07 : panneau sans range → ligne cachée
- U-08 : format `p25 — p50 — p75` avec euros

## Tables / endpoints / composants impactés
### Backend
- `JurisprudenceRange.java` (nouveau record)
- `JafReferenceTable.java` (nouveau service statique)
- `PensionAlimentaireCalculator.java` (+ enrichissement résultat)
- `PrestationCompensatoireCalculator.java` (+ enrichissement résultat)
- Records `PensionAlimentaireEstimate` / `PrestationCompensatoireEstimate` (+ champ nullable + constructeur rétrocompat)

### Frontend
- `core/models/case-analysis.model.ts` (+ interface JurisprudenceRange + champ sur les 2 types)
- `case-files/synthesis/synthesis.component.html` (+ lignes "Fourchette JAF")
- `core/services/pdf-export.service.ts` (optionnel : inclure dans les exports, à évaluer)

## Impact par domaine métier
| Domaine | Impact |
|---|---|
| **Famille FR** | Barèmes UNAF dérivés + stats ministère Justice |
| **Famille BE** | CGKR barème + dispersion estimée (à raffiner avec feedback cabinet BE) |
| **Travail / Immigration** | Non applicable |

## Parité des domaines métier
**Niveau 6 — Comparateur / fourchettes** :
- ✅ Travail : F-DT-09
- ✅ Immigration : F-151
- 🚧 Famille : F-153 (cette SF)

Après F-153 : **tous les 3 domaines au même niveau 6**. Parité atteinte pour les niveaux 5, 6 et 7.

## Analyse de cohérence transversale
- F-DT-09 pattern : récup avec fourchette p25/p50/p75. Pattern appliqué fidèlement.
- `JafReferenceTable` : isolée, non couplée au prompt IA (contrairement à F-DT-09 qui vient de barèmes réels Stripe). Version V1 : tables Java. Si feedback cabinet juge indispensable, basculer en migration DB plus tard.

## Préoccupations transversales
Aucune.

## Hors scope
- Raffinement des fourchettes belges (cabinets BE à solliciter)
- Export PDF des fourchettes — si feedback utilisateur positif après V1
- Fourchettes pour soulte (pas de valeur — calcul arithmétique)
