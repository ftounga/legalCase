# Mini-spec — F-DT-12 / SF-DT-12-01 Discrimination — dommages-intérêts (backend)

## Identifiant
`F-DT-12 / SF-DT-12-01`

## Feature parente
`F-DT-12` — Discrimination — outil décisionnel FR + BE (critique 🔴)

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-DT-12-01-discrimination-dommages-interets`

---

## Objectif

Fournir un outil dédié à l'évaluation des dommages-intérêts pour acte discriminatoire (licenciement, sanction, embauche refusée, promotion refusée, différence salariale), distinct de F-DT-11 qui calcule l'indemnité minimum 6 mois en cas de nullité du licenciement. F-DT-12 calcule la **réparation du préjudice** spécifique à la discrimination (préjudice moral + préjudice de carrière), **cumulable** avec l'indemnité nullité de F-DT-11.

---

## Comportement attendu

### Cas nominal

**Fourchette indicative** par motif × contexte d'acte (jurisprudence constante chambre sociale Cour de cassation) :

Formule : `DI = salaireMensuelReference × facteur` où `facteur ∈ [3, 12]` selon motif/contexte.

**Motifs FR (8 groupes issus des 25 motifs prohibés L.1132-1) :**
| Code | Description |
|---|---|
| `ORIGINE_ETHNIQUE` | Origine, ethnie, race, nationalité — facteur 6-12 |
| `SEXE_GROSSESSE` | Sexe, grossesse, maternité, paternité — facteur 6-12 |
| `ORIENTATION_SEXUELLE` | Orientation sexuelle, identité de genre — facteur 6-10 |
| `AGE` | Âge — facteur 3-8 |
| `ETAT_SANTE_HANDICAP` | État de santé, handicap, apparence physique — facteur 6-12 |
| `OPINIONS_POLITIQUES` | Opinions politiques, convictions religieuses, activités syndicales — facteur 6-12 |
| `ORIGINE_SOCIALE` | Origine sociale, patronyme, lieu de résidence — facteur 3-8 |
| `SITUATION_FAMILIALE` | Situation familiale, charges de famille — facteur 3-8 |

**Motifs BE (5 issus de la loi 10/5/2007 + loi genre 10/5/2007) :**
| Code | Description |
|---|---|
| `DISCRIMINATION_RACIALE_BE` | Loi 30/7/1981 — origine, race, couleur, ascendance |
| `DISCRIMINATION_GENRE_BE` | Loi genre 10/5/2007 — sexe, grossesse, changement de sexe |
| `DISCRIMINATION_HANDICAP_BE` | Loi 10/5/2007 — handicap, état de santé |
| `DISCRIMINATION_AGE_BE` | Loi 10/5/2007 — âge |
| `DISCRIMINATION_ORIENTATION_BE` | Loi 10/5/2007 — orientation sexuelle |

**Contextes d'acte (facteur multiplicateur modulé) :**
| Code | Description | Modulation |
|---|---|---|
| `LICENCIEMENT` | Licenciement discriminatoire | facteur maximum (cas le plus grave) |
| `SANCTION` | Sanction disciplinaire discriminatoire | facteur moyen |
| `EMBAUCHE_REFUSEE` | Refus d'embauche discriminatoire | facteur moyen |
| `PROMOTION_REFUSEE` | Refus de promotion / stagnation | facteur moyen-haut |
| `DIFFERENCE_SALARIALE` | Écart salarial injustifié | facteur minimum (préjudice souvent matériel remboursable indépendamment) |
| `HARCELEMENT_LIE_DISCRIMINATION` | Harcèlement motivé par discrimination | facteur haut |

**Sortie :**
- `fourchetteMin` = salaire × facteur_min
- `fourchetteMediane` = salaire × (facteur_min + facteur_max) / 2
- `fourchetteMax` = salaire × facteur_max
- `formule` = texte
- `baseJuridique` = "Art. L.1134-5 Code du travail" (FR) ou "Loi 10 mai 2007" (BE)
- `messages` : rappel régime probatoire L.1134-1 (charge renversée), prescription 5 ans, cumul avec nullité F-DT-11.

### Cas d'erreur
- Salaire ≤ 0 → 400
- Motif FR sur BE (inverse) → 400
- Motif/contexte inconnu → 400
- Dossier autre domaine → 400
- Workspace étranger → 404

---

## Contrat API (figé)

POST/GET `/api/v1/case-files/{caseFileId}/discrimination-dommages-interets`

**Request :**
```json
{
  "salaireMensuelReference": 3000.00,
  "motifDiscrimination": "SEXE_GROSSESSE",
  "contexteActe": "LICENCIEMENT"
}
```

**Response :**
```json
{
  "caseFileId": "uuid",
  "salaireMensuelReference": 3000.00,
  "motifDiscrimination": "SEXE_GROSSESSE",
  "contexteActe": "LICENCIEMENT",
  "country": "FRANCE",
  "fourchetteMin": 18000.00,
  "fourchetteMediane": 27000.00,
  "fourchetteMax": 36000.00,
  "formule": "Salaire × 6 à 12 mois selon préjudice = 18 000 € à 36 000 €",
  "baseJuridique": "Art. L.1134-5 Code du travail",
  "messages": ["Régime probatoire L.1134-1...", "Cumulable...", "Prescription 5 ans..."]
}
```

---

## Périmètre

### Hors scope
- Frontend → SF-DT-12-02 (parallèle).
- Détection IA auto du motif → hors scope.
- Calcul du préjudice matériel exact (rappel de salaire, perte de chance carrière) → laissé à l'avocat.

---

## Architecture

Pattern strict F-DT-11 backend. Table `discrimination_dommages_interets_analyses`. Migration 112. Tool_id `F-DT-12-discrimination-dommages-interets`, règle ALWAYS_ON × 2 pays.

## Critères d'acceptation

- [ ] Calculator retourne fourchette min/médiane/max selon motif × contexte.
- [ ] 8 motifs FR + 5 motifs BE testés.
- [ ] 6 contextes d'acte testés.
- [ ] Cross-country (motif FR + workspace BE) → IllegalArgumentException.
- [ ] Salaire invalide → IllegalArgumentException.
- [ ] Migration 112 crée table + 2 règles visibility.
- [ ] Endpoint POST persiste, GET idempotent, upsert OK.
- [ ] Isolation workspace testée.
- [ ] Gate domaine testé (dossier immigration → 400).

## Impact par domaine

DROIT_DU_TRAVAIL FR + BE. Parité ≥5 non applicable (niveau 3/4 hybride — calculateur avec fourchette).

## Analyse de cohérence transversale
- [x] Complémentaire F-DT-11 (motif nullité) — situations métier distinctes (nullité acte vs réparation préjudice discrimination), cumulables.
- [x] Pattern F-DT-11 backend copié verbatim.
