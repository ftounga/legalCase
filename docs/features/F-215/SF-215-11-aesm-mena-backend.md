# SF-215-11 — AESM + tutelle DGDE (MENA) — backend

## Identifiant
`F-215 / SF-215-11`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-11-aesm-mena-be-backend`

---

## Objectif
Livrer le Calculator + Service + Entity + Endpoint backend pour `F-IM-30-aesm-mena-be` : outil composite couvrant deux volets distincts mais liés du droit belge des mineurs non accompagnés —
1. **Volet MENA / tutelle DGDE** (loi 04/05/2007 — Service des Tutelles SPF Justice) : checklist procédurale désignation tuteur, projet de vie ;
2. **Volet AESM** (Admission Exceptionnelle Séjour Mineur — art. 9bis adapté MENA, circulaire OE 15/09/2005) : analyse éligibilité basée sur intégration scolaire + projet de vie + perspective autonomie.
BELGIQUE UNIQUEMENT — aucun équivalent FR direct (MNA FR = ordonnance JE, mécanisme différent).

---

## Comportement attendu

### Cas nominal
- POST `/api/v1/case-files/{caseFileId}/aesm-mena-be-analysis`
- Body :
  - `ageActuel` (Integer — âge du mineur en années, requis)
  - `dateArrivéeBelgique` (LocalDate, requis)
  - `tuteurDesigné` (Boolean — tuteur DGDE désigné, requis)
  - `integrationScolaire` (Boolean — scolarité régulière, requis)
  - `dureeScolaire` (Integer — mois de scolarité continue, requis si integrationScolaire=true)
  - `projetVieElabore` (Boolean — projet de vie formalisé OE, requis)
  - `perspectiveAutonomie` (Boolean — jeune démontre perspective d'autonomie — emploi/formation, requis)
  - `menaceOrdrePublic` (Boolean, requis)
- `AesmMenaBeCalculator` calcule :
  - **Volet tutelle** :
    - `etapeTutelle` : si !tuteurDesigné → « Signaler au Service des Tutelles SPF Justice — art. 7 loi 04/05/2007 »
    - `delaiDesignationTuteur` = dateArrivéeBelgique + 30 jours (délai théorique signalement, à vérifier)
  - **Volet AESM** :
    - `scoreIntegration` : integrationScolaire pondéré (0-40), projetVieElabore (0-30), perspectiveAutonomie (0-20), !menaceOrdrePublic (0-10)
    - `bonus` = dureeScolaire ≥ 24 mois → +5 pts
    - `verdictAESM` ∈ { FAVORABLE (score ≥ 70), SOUS_RESERVE (score 50-69), DEFAVORABLE (score < 50) }
    - `prioriteUrgence` = ageActuel ≥ 17 → vrai (approche majorité = urgence procédurale)
  - `criteresNonRemplis` : liste humaine
  - `prochainActe` : « Désignation tuteur DGDE → Entretien OE → Décision AESM (3-12 mois) »

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| workspace.country ≠ BELGIQUE | 400 | 400 |
| legalDomain ≠ DROIT_IMMIGRATION | 400 | 400 |
| ageActuel ≥ 18 | Outil réservé aux mineurs | 400 |
| ageActuel < 0 | 400 | 400 |

---

## Analyse de cohérence transversale

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `F-IM-14-9bis-humanitaire-be` | Oui | AESM est basé sur art. 9bis mais avec critères adaptés MENA. **Distinction documentée** : 9bis adulte = faisceau présence/enracinement ; AESM = scolarité/projet de vie/tutelle. Un outil = une situation — 2 outils distincts. |
| `F-IM-19-mineurs` (FR) | Non | F-IM-19 = MNA FR (ordonnance JE + L.435-3). Procédure entièrement différente — pas de réutilisation possible. |
| Flags IA `mineur_non_accompagne_be_detecte` (F-203) | Oui | Déjà seedé — déclenche CONTEXTUAL pour `F-IM-30`. |

---

## Conformité F-IA-04
- [ ] **Non applicable** — SF backend pure. Composant : SF-215-12.

---

## Champs IA à extraire (pré-remplissage)

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|-------|------|-----------------------------------------|-----------|
| `ageActuel` | entier | `menaAge` | Nouveau |
| `dateArrivéeBelgique` | date | `menaDateArrivee` | Nouveau |
| `integrationScolaire` | booléen | aspirationnel (contextuel — constat dossier) | PREFILL_COUNT_ALWAYS_ZERO |
| `dureeScolaire` | entier | `menaDureeScolaire` | Nouveau |
| `tuteurDesigné` | booléen | aspirationnel | PREFILL_COUNT_ALWAYS_ZERO |
| `projetVieElabore` | booléen | aspirationnel | PREFILL_COUNT_ALWAYS_ZERO |
| `perspectiveAutonomie` | booléen | aspirationnel | PREFILL_COUNT_ALWAYS_ZERO |
| `menaceOrdrePublic` | booléen | aspirationnel | PREFILL_COUNT_ALWAYS_ZERO |

3 champs réels (age, date arrivée, durée scolaire). 5 aspirationnels.

---

## Critères d'acceptation

- [ ] POST ageActuel ≥ 18 → 400
- [ ] POST workspace FR → 400
- [ ] POST → verdictAESM=FAVORABLE si score ≥ 70
- [ ] POST ageActuel ≥ 17 → prioriteUrgence=true
- [ ] Volet tutelle : etapeTutelle renseigné si tuteurDesigné=false
- [ ] UT : ≥ 8 cas (FAVORABLE, SOUS_RESERVE, DEFAVORABLE, urgence 17 ans, tuteur absent)
- [ ] IT : ≥ 6 tests
- [ ] `F-IM-30-aesm-mena-be` dans `KNOWN_FRONTEND_TOOL_IDS`
- [ ] Migration : table `aesm_mena_be_analyses` + visibility CONTEXTUAL (`mineur_non_accompagne_be_detecte=true`)

---

## Hors périmètre
- Composant Angular (SF-215-12)
- Prorogation tutelle après 18 ans (F-221)
- Procédure asile MENA spécifique (F-221)

---

## Plan de test
- `AesmMenaBeCalculatorTest` : ≥ 8 cas
- `AesmMenaBeControllerIT`

## Notes et décisions
- Source : Loi 04/05/2007 relative à la tutelle MENA ; loi 15/12/1980 art. 9bis (adaptation MENA) ; circulaire OE 15/09/2005 sur l'AESM.
- Délai 30 jours signalement = art. 7 loi 04/05/2007 — à vérifier avocat BE (`// (à vérifier)`).
- Distinction MENA / mineur rejoignant parent : MENA = arrivé seul ; mineur rejoignant = regroupement art. 10 (hors périmètre de cet outil).
