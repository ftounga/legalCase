# Mini-spec — F-214 / SF-214-01 — Étranger malade L. 425-9 + recours OFII — backend

## Identifiant

`F-214 / SF-214-01`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-214-01-etranger-malade-l4259-backend`

---

## Objectif

Analyser l'éligibilité à la protection médicale L. 425-9 CESEDA (état de santé nécessitant soins indisponibles dans le pays d'origine, avis collège médical OFII) et générer les éléments de recours contre un avis OFII défavorable, en persistant l'analyse 1:1 par dossier.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/etranger-malade-analysis`
- Body : `dateDepotDossierOFII` (LocalDate, optionnel), `pathologiePrincipale` (string ≤ 500), `paysOrigine` (string), `traitementDisponiblePaysOrigine` (boolean), `avisOFIIRendu` (boolean), `avisOFII` (enum : `FAVORABLE` | `DEFAVORABLE` | `EN_ATTENTE`), `dateAvisOFII` (LocalDate, optionnel)
- Calculator / Analyzer `EtrangerMaladeAnalyzer` :
  - Vérifie les critères d'éligibilité L. 425-9 : (1) état de santé grave ou maladie grave, (2) traitement indisponible ou inaccessible dans le pays d'origine, (3) défaut de soins risquant d'entraîner des conséquences d'une exceptionnelle gravité.
  - Calcule le `verdict` ∈ {`ELIGIBLE_PROBABLE`, `ELIGIBLE_SOUS_RESERVE`, `NON_ELIGIBLE`, `EN_ATTENTE_AVIS_OFII`}
  - Calcule `delaiRecoursTA` = dateAvisOFII + 2 mois (si avisOFII = `DEFAVORABLE`)
  - Produit `motifRecours` (texte pré-rédigé reprenant les éléments médicaux) si `avisOFII = DEFAVORABLE`
  - `chipsCriteresNonRemplis` : liste des critères L. 425-9 non satisfaits
- Output persisté dans `etranger_malade_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/etranger-malade-analysis` → 200 ou 404

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| workspace.country ≠ FRANCE | Refus — outil FR-only | 400 |
| caseFile.legalDomain ≠ DROIT_IMMIGRATION | Refus | 400 |
| paysOrigine vide | 400 | 400 |
| pathologiePrincipale > 500 chars | 400 | 400 |
| dateAvisOFII future | 400 | 400 |
| avisOFII = DEFAVORABLE sans dateAvisOFII | 400 | 400 |
| caseFile inaccessible au workspace | 404 | 404 |

---

## Source juridique

- **L. 425-9 CESEDA** (ancien L. 313-11 11°) — titre étranger malade. Ordonnance 2020-1733 du 16/12/2020.
- **R. 425-9 à R. 425-16 CESEDA** — procédure collège médical OFII.
- **Circulaire du 10/11/2011** relative à la mise en œuvre du nouveau régime d'autorisation de travail L. 313-11 11°.
- **CE 7 avril 2010, n° 301640** — critères d'appréciation de l'accessibilité des soins.
- **Loi 7 mars 2016** (Collomb) : transfert compétence OFPRA → OFII pour l'avis médical.
- **R. 425-12** (à vérifier) : délai recours TA contre décision préfectorale s'appuyant sur avis OFII défavorable (2 mois de droit commun CJA).

---

## Champs IA à extraire (pré-remplissage)

| Champ du formulaire | Type | Champ source `ImmigrationExtractedData` | Extension requise |
|---|---|---|---|
| `pathologiePrincipale` | texte | Absent | Extension record + prompt IMMIGRATION_INSTRUCTION (`etrangerMaladePathologie`) |
| `paysOrigine` | texte | `nationalite` (proxy indirect) | Déjà présent — réutiliser |
| `traitementDisponiblePaysOrigine` | booléen | Absent | Extension record + prompt (`etrangerMaladeTraitementDisponible`) |
| `avisOFII` | enum | Absent | Extension record + prompt (`etrangerMaladeAvisOFII`) |
| `dateAvisOFII` | date | Absent | Extension record + prompt (`etrangerMalaDateAvisOFII`) |

**Nouveau flag CONTEXTUAL** : `etrangerMaladeDetecte` (boolean) — déclenche l'affichage de l'outil. Extraction : présence dans les pièces de mentions "maladie grave", "traitement indisponible", "OFII médical", "L.425-9", "titre étranger malade". Ajouté dans `ImmigrationExtractedData` + prompt.

---

## Critères d'acceptation

- [x] POST nominal (ELIGIBLE_PROBABLE) retourne 200 avec verdict, chipsCriteresNonRemplis, delaiRecoursTA null
- [x] POST avec avisOFII=DEFAVORABLE retourne verdict ELIGIBLE_PROBABLE + motifRecours + delaiRecoursTA calculé
- [x] POST workspace BE → 400
- [x] POST domaine travail → 400
- [x] POST dateAvisOFII future → 400
- [x] POST avisOFII DEFAVORABLE sans dateAvisOFII → 400
- [x] GET sans POST préalable → 404
- [x] POST upsert remplace l'analyse précédente (1:1 case_file)
- [x] Isolation workspace : l'avocat A ne voit pas l'analyse du dossier de l'avocat B (404)
- [x] `F-IM-25-etranger-malade-l4259-fr` présent dans `KNOWN_FRONTEND_TOOL_IDS`
- [x] Seed `decision_tool_visibility_rules` : CONTEXTUAL, DROIT_IMMIGRATION, FRANCE, trigger_field=`etranger_malade_detecte`

## Plan de test minimal

- **UT** `EtrangerMaladeAnalyzerTest` : 8+ cas (ELIGIBLE_PROBABLE, SOUS_RESERVE, NON_ELIGIBLE, EN_ATTENTE, avis défavorable avec recours, critères non remplis, délai TA calculé)
- **IT** `EtrangerMaladeControllerIT` : 6+ cas (POST nominal, POST BE→400, POST travail→400, POST autre workspace→404, GET sans POST→404, POST upsert)
- **Intégrité** : `F-IM-25-etranger-malade-l4259-fr` dans `KNOWN_FRONTEND_TOOL_IDS`

## Tables / endpoints / composants impactés

- **Nouvelle table** `etranger_malade_analyses` (id UUID, case_file_id UUID UNIQUE, date_depot_dossier_ofii DATE, pathologie_principale VARCHAR(500) NOT NULL, pays_origine VARCHAR(100) NOT NULL, traitement_disponible_pays_origine BOOLEAN NOT NULL, avis_ofii VARCHAR(20), date_avis_ofii DATE, country VARCHAR(20) NOT NULL, result_data TEXT NOT NULL, created_at TIMESTAMP, updated_at TIMESTAMP)
- **Migration Liquibase** `XXX-create-etranger-malade-analyses.xml` + INSERT `decision_tool_visibility_rules` (CONTEXTUAL, DROIT_IMMIGRATION, FRANCE, `F-IM-25-etranger-malade-l4259-fr`, trigger_field=`etranger_malade_detecte`)
- **Extension** `ImmigrationExtractedData` : 5 nouveaux champs (pathologie, paysOrigine proxy existant, traitement disponible, avis OFII, date avis OFII) + 1 flag boolean `etrangerMaladeDetecte`
- **Extension** prompt `IMMIGRATION_INSTRUCTION` : instructions d'extraction pour les 5 champs
- **Endpoint** `EtrangerMaladeController` (POST, GET) sous `/api/v1/case-files/{caseFileId}/etranger-malade-analysis`

## Hors périmètre

- Composant Angular (SF-214-02)
- Pré-fill IA runtime frontend (SF-214-02)
- Génération automatique de la requête TA (F-IM-06 existant)

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Outil décisionnel métier** — nouveau outil, scan obligatoire : aucun outil existant ne couvre L. 425-9 (F-IM-09-aes-humanitaire couvre L. 435-1 / circulaire Valls, distinct). Pas de doublon.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression |
|---|---|---|
| `ImmigrationExtractedData` (record + builder) | Ajout 5 champs + 1 flag — builder pattern F-234 absorbe le changement sans nouveau constructeur rétrocompat | Tests UT Builder existants |
| `IMMIGRATION_INSTRUCTION` prompt | Extension instructions — régression possible si token count dépasse le seuil | Test prompt extraction sur dossier test |
| `DecisionToolVisibilityIntegrityIT` | Ajout entrée `KNOWN_FRONTEND_TOOL_IDS` | Automatique dans le plan de test |

### Smoke tests E2E concernés

- [ ] Aucun smoke test E2E existant directement concerné (outil nouveau).
