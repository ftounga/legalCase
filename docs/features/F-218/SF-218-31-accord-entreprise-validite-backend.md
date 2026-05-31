# Mini-spec — F-218 / SF-218-31 — Accord d'entreprise : validité (conditions de majorité) — backend

## Identifiant

`F-218 / SF-218-31`

## Feature parente

`F-218c` — IRP / négociation collective FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-31

## Branche Git

`feat/SF-218-31-accord-entreprise-validite-backend`

---

## Objectif

Analyser la **validité d'un accord d'entreprise** au regard des conditions de majorité issues de la loi Travail 2016 et de l'ordonnance 2017 (art. L.2232-12 CT) : signature par des syndicats représentatifs ayant recueilli **plus de 50 %** des suffrages exprimés au 1er tour des dernières élections, ou validation par **référendum** à la majorité des salariés si les signataires atteignent **au moins 30 %**. Vérifie aussi les conditions de **dénonciation** (préavis 3 mois, survie 12 mois) et de **révision** (art. L.2261-7 et suivants). **Analyseur de validité**. Aucun outil existant ne couvre la validité d'un accord d'entreprise (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/accord-entreprise-validite-analysis`
- Body :
  - `pourcentageSuffragesSignataires` (BigDecimal 0..100, requis) — % des suffrages exprimés au 1er tour recueillis par les syndicats signataires
  - `referendumOrganise` (boolean, défaut false) — un référendum de validation a été organisé
  - `referendumApprouve` (boolean, défaut false) — le référendum a approuvé l'accord (majorité des suffrages exprimés)
  - `typeOperation` (enum requis) — `CONCLUSION` | `REVISION` | `DENONCIATION`
  - `signePartiesHabilitees` (boolean, requis, révision) — signé par les parties habilitées à réviser (L.2261-7)
  - `preavisDenonciationRespecte` (boolean, défaut true, dénonciation) — préavis de 3 mois respecté
  - `dateDenonciation` (LocalDate, optionnel, dénonciation) — date de la dénonciation (point de départ de la survie 12 mois)
- Analyzer `AccordEntrepriseValiditeAnalyzer` :
  - **Conditions de majorité (CONCLUSION / REVISION)** :
    - `pourcentageSuffragesSignataires ≥ 50` → `conditionMajorite = MAJORITE_50` (accord valide sans référendum).
    - `30 ≤ pourcentageSuffragesSignataires < 50` ET `referendumOrganise=true` ET `referendumApprouve=true` → `conditionMajorite = REFERENDUM_30` (accord valide par référendum).
    - `30 ≤ pourcentageSuffragesSignataires < 50` SANS référendum approuvé → `conditionMajorite = INSUFFISANTE` (accord non valide en l'état).
    - `< 30` → `conditionMajorite = INSUFFISANTE` (référendum impossible, accord non valide).
  - **REVISION** : ajoute la vérification `signePartiesHabilitees` (item de validité).
  - **DENONCIATION** : `preavisDenonciationRespecte` ; si `dateDenonciation` présent → `dateFinSurvie` = `dateDenonciation + 3 mois (préavis) + 12 mois (survie)`. Item validité = préavis respecté.
  - **Verdict** :
    - conditions de majorité remplies (et, en révision, parties habilitées ; en dénonciation, préavis respecté) → `statut = VALIDE`.
    - conditions de majorité non remplies → `statut = NON_VALIDE`.
    - majorité atteinte par référendum 30 % → `statut = VALIDE_SOUS_RESERVE` + note « validité subordonnée à la régularité du référendum (L.2232-12) ».
  - Checklist `{ item, conforme, commentaire }` (majorité, référendum si applicable, parties habilitées si révision, préavis si dénonciation).
  - `baseJuridique` : art. L.2232-12 CT ; L.2261-7 et suivants — annoté `(à vérifier par avocat)`.
- Output persisté dans `accord_entreprise_validite_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/accord-entreprise-validite-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| `pourcentageSuffragesSignataires` absent / hors [0,100] | 400 |
| `typeOperation` absent / invalide | 400 |
| `typeOperation=REVISION` et `signePartiesHabilitees` null | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Art. L.2232-12 CT** — validité d'un accord d'entreprise : signature par des syndicats représentatifs ayant recueilli > 50 % des suffrages exprimés au 1er tour ; à défaut (≥ 30 %), validation par référendum des salariés à la majorité des suffrages exprimés.
- **Art. L.2261-7 et suivants CT** — révision (parties habilitées à engager la procédure) et dénonciation (préavis de 3 mois, maintien des effets pendant 12 mois — survie de l'accord).

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `pourcentageSuffragesSignataires` | montant (%) | `accordPourcentageSignataires` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `typeOperation` | énum | `accordTypeOperation` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Flag CONTEXTUAL pivot** : `accord_entreprise_detecte` (niveau 3, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte des signaux d'accord d'entreprise (mentions « accord d'entreprise », « accord collectif », « suffrages exprimés au 1er tour », « référendum de validation », « dénonciation d'accord », « avenant de révision », « syndicats signataires »).

---

## Critères d'acceptation

- [ ] POST `pourcentageSuffragesSignataires=55`, `typeOperation=CONCLUSION` → `conditionMajorite=MAJORITE_50`, `statut=VALIDE`
- [ ] POST `35`, `referendumOrganise=true`, `referendumApprouve=true` → `conditionMajorite=REFERENDUM_30`, `statut=VALIDE_SOUS_RESERVE`
- [ ] POST `35` sans référendum → `conditionMajorite=INSUFFISANTE`, `statut=NON_VALIDE`
- [ ] POST `25` → `INSUFFISANTE`, `statut=NON_VALIDE`
- [ ] POST `typeOperation=REVISION`, `signePartiesHabilitees=false` → item parties habilitées non conforme, `statut=NON_VALIDE`
- [ ] POST `typeOperation=DENONCIATION`, `dateDenonciation` fournie → `dateFinSurvie = +15 mois`
- [ ] POST `pourcentageSuffragesSignataires=120` → 400 ; `typeOperation` invalide → 400 ; REVISION sans `signePartiesHabilitees` → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`accord_entreprise_detecte`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL
- [ ] `F-DT-67-accord-entreprise-validite` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `AccordEntrepriseValiditeAnalyzerTest` : ≥ 6 cas (≥50 → MAJORITE_50 VALIDE, 30–50 + référendum → REFERENDUM_30 VALIDE_SOUS_RESERVE, 30–50 sans référendum → INSUFFISANTE NON_VALIDE, <30 → NON_VALIDE, révision parties habilitées, dénonciation calcul dateFinSurvie +15 mois)
- **IT** `AccordEntrepriseValiditeControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `accord_entreprise_validite_analyses`
- **Migrations** : `create-accord-entreprise-validite-analyses.xml` + `seed-accord-entreprise-validite-visibility.xml` (reconfirmer les numéros libres dans le worktree)
- **Endpoint** `AccordEntrepriseValiditeController` (POST + GET)
- **Service** `AccordEntrepriseValiditeService` + **Analyzer** `AccordEntrepriseValiditeAnalyzer`
- **Extension** `TravailExtractedData` : champs `accordPourcentageSignataires` + `accordTypeOperation` + flag `accordEntrepriseDetecte` + prompt `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-32)
- Contenu de fond / opposabilité d'une clause particulière de l'accord
- NAO (F-DT-66, situation distincte)
- Accord de branche / extension (situation distincte)
