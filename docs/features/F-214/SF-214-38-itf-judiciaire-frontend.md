# Mini-spec — F-214 / SF-214-38 — ITF judiciaire — frontend

## Identifiant

`F-214 / SF-214-38`

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer `<app-itf-judiciaire-section>` pour `F-IM-43-itf-judiciaire-fr`, avec encadré explicatif ITF vs IRTF.

---

## Comportement attendu

- Formulaire : `dateCondamnation` (date), `dureeITFAnnees` (number), `infractionPrincipale` (text), `condamnationDefinitive` (checkbox).
- Résultat : statut chip, voiesRecours liste (délais JetBrains Mono), `requisReleve` liste, encadré bleu distinctionItfVsIrtf.
- CONTEXTUAL : `mesureEloignementDetectee`.
- Bridge F-69 : deadline recours si APPEL_POSSIBLE.

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 5 (analyseur validité).

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] Encadré distinctionItfVsIrtf affiché
- [x] Bridge F-69 délai recours
- [x] Tests Jest ≥ 12

## Dépendances

- SF-214-37 : statut `done`
