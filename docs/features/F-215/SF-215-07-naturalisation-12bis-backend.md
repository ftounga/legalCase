# SF-215-07 — Naturalisation déclaration art. 12bis Code nationalité belge — backend

## Identifiant
`F-215 / SF-215-07`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-07-naturalisation-12bis-be-backend`

---

## Objectif
Livrer le Calculator + Service + Entity + Endpoint backend pour `F-IM-28-naturalisation-12bis-be` : analyse d'éligibilité à la déclaration de nationalité belge (art. 12bis CNB 1984) — voie 5 ans séjour illimité + intégration (emploi/langue/intégration) ou voie 10 ans séjour illimité, BELGIQUE UNIQUEMENT. Cet outil est le jumeau BE de `F-IM-13-naturalisation` (FR).

---

## Comportement attendu

### Cas nominal
- POST `/api/v1/case-files/{caseFileId}/naturalisation-12bis-be-analysis`
- Body :
  - `dureeSejour` (Integer — mois de séjour légal ininterrompu en BE, requis)
  - `typeSejour` (enum : LIMITE / ILLIMITE, requis) — séjour illimité requis (carte B/C/K)
  - `niveauLangue` (enum : INFERIEUR_A2 / A2 / SUPERIEUR_A2, requis)
  - `preuveIntegration` (Boolean — cours d'intégration régional validé ou équivalent, requis)
  - `preuveEmploi` (Boolean — activité pro ≥ 468h dans 5 ans ou pension/revenus stables, requis)
  - `menaceOrdrePublic` (Boolean, requis)
  - `condamnationPenale` (Boolean — condamnation à ≥ 3 mois ferme dans 5 ans, requis)
- `Naturalisation12bisBeCalculator` calcule :
  - `voie5Ans` : dureeSejour ≥ 60 mois + typeSejour ILLIMITE + niveauLangue ∈ {A2, SUPERIEUR_A2} + (preuveIntegration OU preuveEmploi) + !menaceOrdrePublic + !condamnationPenale
  - `voie10Ans` : dureeSejour ≥ 120 mois + typeSejour ILLIMITE + niveauLangue ∈ {A2, SUPERIEUR_A2} + !menaceOrdrePublic + !condamnationPenale (sans condition emploi/intégration)
  - `voieEligible` ∈ { VOIE_5_ANS, VOIE_10_ANS, AUCUNE }
  - `dureeManquante` = max(60, 120) − dureeSejour si négatif (mois à attendre)
  - `criteresNonRemplis` : liste humaine des conditions non remplies
  - `procedurerReference` : « Chambre des représentants — Commission des naturalisations — art. 12bis CNB 1984 »

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| workspace.country ≠ BELGIQUE | 400 | 400 |
| legalDomain ≠ DROIT_IMMIGRATION | 400 | 400 |
| dureeSejour < 0 ou > 600 | 400 | 400 |

---

## Analyse de cohérence transversale

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `F-IM-13-naturalisation` (FR) | Oui | Jumeau BE. `F-IM-13` couvre 6 voies FR (DECRET, MARIAGE, ASCENDANT, MINEUR, REINTEGRATION, OPPOSITION). `F-IM-28` couvre les voies BE art. 12bis. Deux outils distincts — un outil = une situation = un pays. |
| SF-215-09 (`naturalisation-conjoint-belge`) | Oui | Art. 16 CNB = naturalisation conjoint Belge. Voie distincte d'art. 12bis. 2 outils séparés — invariant respecté. |
| Flags IA `naturalisation_be_envisagee` (F-203) | Oui | Déjà seedé. Déclencheur commun pour `F-IM-28` (12bis) et `F-IM-29` (art. 16). Les deux outils partagent ce flag CONTEXTUAL. |

---

## Conformité F-IA-04
- [ ] **Non applicable** — SF backend pure. Composant : SF-215-08.

---

## Champs IA à extraire (pré-remplissage)

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|-------|------|-----------------------------------------|-----------|
| `dureeSejour` | entier | `naturalisationBeDureeSejour` | Nouveau |
| `typeSejour` | enum | `naturalisationBeTypeSejour` | Nouveau — LIMITE/ILLIMITE |
| `niveauLangue` | enum | `naturalisationBeNiveauLangue` | Nouveau — 3 valeurs |
| `preuveIntegration` | booléen | aspirationnel (évaluation juridique) | PREFILL_COUNT_ALWAYS_ZERO pour ce champ |
| `preuveEmploi` | booléen | aspirationnel | PREFILL_COUNT_ALWAYS_ZERO pour ce champ |

3 champs réels dans `ImmigrationExtractedData` + prompt. 2 champs booléens aspirationnels documentés.

---

## Critères d'acceptation

- [ ] POST → voie5Ans=true si dureeSejour≥60 + ILLIMITE + langue A2 + intégration/emploi + pas menace
- [ ] POST → voie10Ans=true si dureeSejour≥120 + ILLIMITE + langue A2 + pas menace (sans intégration/emploi)
- [ ] POST → voieEligible=AUCUNE si ni l'une ni l'autre satisfaite
- [ ] `dureeManquante` = 0 si voie éligible
- [ ] POST workspace FR → 400
- [ ] Flag `naturalisation_be_envisagee` déjà présent dans visibility — vérifier la règle CONTEXTUAL existante couvre `F-IM-28`
- [ ] UT : ≥ 8 cas
- [ ] IT : ≥ 6 tests
- [ ] `F-IM-28-naturalisation-12bis-be` dans `KNOWN_FRONTEND_TOOL_IDS`

---

## Hors périmètre
- Naturalisation par décret (art. 19-21 CNB) — P3, très rare (F-221)
- Naturalisation conjoint Belge art. 16 (SF-215-09)
- Composant Angular (SF-215-08)

---

## Plan de test
- `Naturalisation12bisBeCalculatorTest` : voie 5 ans OK, voie 10 ans OK, aucune voie, langue insuffisante, durée insuffisante, ordre public, condamnation
- `Naturalisation12bisBeControllerIT`

## Notes et décisions
- Source : Code de la nationalité belge (loi 28/06/1984, consolidé) art. 12bis ; AR 14/01/2013 (preuve langue) ; AR 27/04/2018 (preuve intégration).
- Cours d'intégration régionalisés : Parcours d'intégration (Wallonie), Inburgering (Flandre), Bruxelles-Inburgering (Bruxelles) — le Calculator les accepte tous comme `preuveIntegration = true` sans distinction régionale en V1.
- Niveau A2 : diplôme A2 DELF, certificat NT2 (Pays-Bas), diplôme Belgique francophone 6e secondaire ou équivalent.
