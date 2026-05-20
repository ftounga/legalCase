# Mini-spec — F-214 / SF-214-05 — VPF liens personnels et familiaux L. 423-23 — backend

## Identifiant

`F-214 / SF-214-05`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-214-05-vpf-liens-personnels-l42323-backend`

---

## Objectif

Analyser l'éligibilité à la VPF sur le fondement des liens personnels et familiaux L. 423-23 CESEDA (ancienne « vie privée et familiale 7° »), en vérifiant les critères jurisprudentiels denses (durée de résidence, intensité des liens, intégration, famille en France vs à l'étranger) et en persisant l'analyse 1:1 par dossier.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/vpf-liens-personnels-analysis`
- Body : `dureeResidenceFranceMois` (int), `entreeEnFranceMineur` (boolean), `enfantsEnFrance` (boolean), `conjointEnFrance` (boolean), `parentsEnFrance` (boolean), `situationFamilialeAlEtranger` (string, optionnel, ≤ 300), `niveauIntegration` (enum `FORT` | `MOYEN` | `FAIBLE`), `ancienneConvictionPenale` (boolean)
- Analyzer `VpfLiensPersonnelsAnalyzer` :
  - Score d'intensité des liens : +2 pts enfant scolarisé en France, +2 pts conjoint régulier en France, +1 pt parents en France, +2 pts entrée mineure, +1 pt niveauIntegration FORT, -2 pts condamnation pénale grave
  - Vérifie durée résidence ≥ 5 ans (critère principal jurisprudentiel) ou ≥ 3 ans si entrée mineure
  - `verdict` ∈ {`ELIGIBLE_PROBABLE`, `ELIGIBLE_SOUS_RESERVE`, `NON_ELIGIBLE`, `DOSSIER_A_CONSOLIDER`}
  - `chipsCriteresNonRemplis` : critères jurisprudentiels non satisfaits
  - `recommandations` : pièces à rassembler pour consolider le dossier
- Output persisté dans `vpf_liens_personnels_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/vpf-liens-personnels-analysis` → 200 ou 404

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_IMMIGRATION | 400 |
| dureeResidenceFranceMois < 0 | 400 |
| situationFamilialeAlEtranger > 300 chars | 400 |
| caseFile inaccessible | 404 |

---

## Source juridique

- **L. 423-23 CESEDA** (ancien L. 313-11 7° — recodification 2021). Critères : « étranger ne vivant pas en état de polygamie, dont les liens personnels et familiaux en France sont tels que le refus de délivrance d'un titre de séjour porterait une atteinte disproportionnée à son droit au respect de sa vie privée et familiale ».
- **Article 8 CEDH** — droit au respect de la vie privée et familiale. Jurisprudence CEDH Boultif c. Suisse, 2001.
- **CE 19 janvier 2011, n° 334018** — appréciation proportionnalité refus VPF vie privée.
- **CE 24 mars 2006, Mme Amidzadeh** (à vérifier numéro) — critères d'intensité des liens.
- **Circ. 28/11/2012 (Valls)** — non applicable directement mais donne des repères sur les 5 ans.

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `dureeResidenceFranceMois` | int | `aesDureePresenceMois` (proxy) | Réutiliser |
| `entreeEnFranceMineur` | boolean | `clientMineurDetecte` (proxy) | Réutiliser |
| `enfantsEnFrance` | boolean | `aesDureeScolaritePlusAncienEnfantAnnees` > 0 (proxy) | Dériver |
| `niveauIntegration` | enum | Absent | Extension record + prompt (`vpfNiveauIntegration`) |

**Nouveau flag CONTEXTUAL** : `viePriveeFamilialeDetectee` (boolean) — extraction : mentions "vie privée et familiale", "L.423-23", "liens familiaux en France", "atteinte disproportionnée", "7°", "ancienneté de résidence". Ajouté dans `ImmigrationExtractedData` + prompt.

---

## Critères d'acceptation

- [x] POST ELIGIBLE_PROBABLE retourne 200 avec score, verdict, recommandations
- [x] POST DOSSIER_A_CONSOLIDER retourne chipsCriteresNonRemplis + recommandations
- [x] POST workspace BE → 400
- [x] POST dureeResidenceFranceMois négatif → 400
- [x] GET sans POST préalable → 404
- [x] POST upsert → remplacement
- [x] Isolation workspace
- [x] `F-IM-27-vpf-liens-personnels-l42323-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed `decision_tool_visibility_rules` : CONTEXTUAL, trigger_field=`vie_privee_familiale_detectee`

## Plan de test minimal

- **UT** `VpfLiensPersonnelsAnalyzerTest` : 8+ cas (score, durée, entrée mineure, condamnation pénale, recommandations)
- **IT** `VpfLiensPersonnelsControllerIT` : 6+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `vpf_liens_personnels_analyses`
- **Migration Liquibase** + seed visibility rules
- **Extension** `ImmigrationExtractedData` + prompt : flag `viePriveeFamilialeDetectee` + champ `vpfNiveauIntegration`
- **Endpoint** `VpfLiensPersonnelsController`

## Hors périmètre

- Composant Angular (SF-214-06)
- Référentiel jurisprudentiel CE/CNDA (différé F-220)
