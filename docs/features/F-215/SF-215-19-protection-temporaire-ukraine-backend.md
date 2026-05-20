# SF-215-19 — Protection temporaire Ukraine BE — backend

## Identifiant
`F-215 / SF-215-19`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-19-protection-temporaire-ukraine-be-backend`

---

## Objectif
Livrer le Calculator + Service + Entity + Endpoint backend pour `F-IM-34-protection-temporaire-ukraine-be` : outil d'information + checklist + calculateur de durée pour la protection temporaire Ukraine (directive 2001/55/CE activée décision UE 2022/382 le 04/03/2022, prolongée annuellement — toujours active 2026), BELGIQUE UNIQUEMENT.

---

## Comportement attendu

### Cas nominal
- POST `/api/v1/case-files/{caseFileId}/protection-temporaire-ukraine-be-analysis`
- Body :
  - `dateArrivee` (LocalDate, requis — date d'entrée en Belgique ou de première demande PT)
  - `nationaliteUkrainienne` (Boolean, requis)
  - `residenceUkraineAvant24Fev2022` (Boolean, requis — résidence habituelle en Ukraine avant le 24/02/2022)
  - `apatridesUkraine` (Boolean — apatride ayant résidence habituelle Ukraine, optionnel)
  - `membreFamilleProtege` (Boolean — membre famille d'un bénéficiaire PT, optionnel)
  - `titreSejourBE` (enum : AUCUN / ATTESTATION_IMMATRICULATION / TITRE_A / TITRE_B / TITRE_AUTRE, requis)
- `ProtectionTemporaireUkraineBeCalculator` calcule :
  - `eligible` = (nationaliteUkrainienne || apatridesUkraine || membreFamilleProtege) && residenceUkraineAvant24Fev2022
  - `dureeProtectionRestante` = dateFin PT − today (dateFin = 04/03/2026 → prolongée annuellement ; valeur paramétrable `protection.temporaire.ukraine.date-fin`)
  - `droitsTravail` = « Droit au travail immédiat dès enregistrement OE (art. 57/29 Loi 15/12/1980) — pas de single permit requis »
  - `droitsAides` : CPAS + logement temporaire, accès enseignement
  - `prochainRenouvellement` : si `dureeProtectionRestante < 90` → alerte renouvellement imminent
  - `cheminProcedure` : liste d'étapes (présentation OE → annexe 35 → inscription commune → droits activés)

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| workspace.country ≠ BELGIQUE | 400 | 400 |
| legalDomain ≠ DROIT_IMMIGRATION | 400 | 400 |
| dateArrivee avant 24/02/2022 | Arrivée avant activation PT | 400 |

---

## Analyse de cohérence transversale

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `asile-protection-subsidiaire-be` (F-221) | Oui | PT Ukraine = régime distinct de la protection subsidiaire (art. 48/4 Loi). Voies non cumulables mais l'avocat peut orienter vers l'une ou l'autre. Documenter dans l'outil la distinction + recommandation si éligibilité protection subsidiaire also applicable. |
| `F-IM-07-droit-au-travail` (transversal) | Oui | F-IM-07 couvre le droit au travail en général. F-IM-34 précise que le droit au travail est immédiat pour PT Ukraine sans single permit. Pas de duplication — précision contextuelle. |

---

## Conformité F-IA-04
- [ ] **Non applicable** — SF backend pure. Composant : SF-215-20.

---

## Champs IA à extraire (pré-remplissage)

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|-------|------|-----------------------------------------|-----------|
| `dateArrivee` | date | `ptUkraineDateArrivee` | Nouveau |
| `nationaliteUkrainienne` | booléen | `ptUkraineNationalite` | Nouveau — isoBool |

2 champs réels. Les autres (`residenceAvant24fev`, `apatride`, `membreFamille`) sont des appréciations juridiques / aspirationnels.

---

## Critères d'acceptation

- [ ] `eligible=true` si nationalité ukrainienne + résidence avant 24/02/2022
- [ ] `dureeProtectionRestante` calculée depuis `protection.temporaire.ukraine.date-fin`
- [ ] `prochainRenouvellement` alerte si restant < 90 jours
- [ ] `droitsTravail` renseigné avec mention « pas de single permit »
- [ ] POST workspace FR → 400
- [ ] POST dateArrivee avant 24/02/2022 → 400
- [ ] UT Calculator : ≥ 6 cas (éligible, non éligible, renouvellement imminent, apatride)
- [ ] IT Controller : ≥ 5 tests
- [ ] `F-IM-34-protection-temporaire-ukraine-be` dans `KNOWN_FRONTEND_TOOL_IDS`
- [ ] Migration : table `protection_temporaire_ukraine_be_analyses` + visibility CONTEXTUAL (`protection_temporaire_ukraine_detectee=true`)

---

## Hors périmètre
- Composant Angular (SF-215-20)
- Protection subsidiaire art. 48/4 (F-221)
- Protection temporaire autres pays (F-221)
- Demande d'asile combinée PT + CGRA (P3)

---

## Plan de test
- `ProtectionTemporaireUkraineBeCalculatorTest`
- `ProtectionTemporaireUkraineBeControllerIT`

## Notes et décisions
- Source : Directive 2001/55/CE ; Décision UE 2022/382 du 04/03/2022 activant la directive ; Loi 15/12/1980 art. 57/29+ ; prolongations annuelles (dernière connue : prolongation jusqu'au 04/03/2026 — paramètre `protection.temporaire.ukraine.date-fin=2026-03-04` en application.properties, révisable).
- Régime particulier : droit au travail immédiat (≠ single permit) est un avantage clé à mettre en évidence dans l'outil.
- Annotation : `// date-fin PT Ukraine à mettre à jour annuellement via application.properties`.
