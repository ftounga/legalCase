# Mini-spec — F-218 / SF-218-09 — Action de groupe en discrimination — backend

## Identifiant

`F-218 / SF-218-09`

## Feature parente

`F-218a` — Procédure CPH avancée (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-09-action-groupe-discrimination-backend`

---

## Objectif

Analyser la recevabilité d'une action de groupe en discrimination au travail (qualité de l'organisation habilitée, mise en demeure préalable et délai de carence de 6 mois, identité de situation) et produire la checklist procédurale, car aucun outil ne couvre ce contentieux collectif L. 1134-7 et s.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/action-groupe-discrimination-analysis`
- Body :
  - `typeOrganisation` (enum `SYNDICAT_REPRESENTATIF` | `ASSOCIATION_AGREEE_5ANS` | `AUTRE`, requis)
  - `dateMiseEnDemeure` (LocalDate, optionnel) — date de la mise en demeure adressée à l'employeur
  - `motifDiscrimination` (enum parmi les critères L. 1132-1 : `ORIGINE`, `SEXE`, `AGE`, `HANDICAP`, `ETAT_SANTE`, `GROSSESSE`, `ACTIVITE_SYNDICALE`, `RELIGION`, `ORIENTATION_SEXUELLE`, `AUTRE`, requis)
  - `nombrePersonnesConcernees` (Integer ≥ 1, requis) — pluralité de candidats/salariés placés dans une situation similaire
  - `objetAction` (enum `CESSATION_MANQUEMENT` | `REPARATION_PREJUDICES` | `LES_DEUX`, défaut `LES_DEUX`)
- Analyzer `ActionGroupeDiscriminationAnalyzer` :
  - **Qualité à agir** : recevable si `typeOrganisation` ∈ {SYNDICAT_REPRESENTATIF, ASSOCIATION_AGREEE_5ANS} (L. 1134-7). Sinon `qualiteAAgir=false`.
  - **Mise en demeure préalable** : la saisine n'est possible qu'après mise en demeure de l'employeur de cesser le manquement + délai de 6 mois (L. 1134-9). Calcule `dateRecevabiliteSaisine` = `dateMiseEnDemeure` + 6 mois et `delaiCarenceRespecte` (true si aujourd'hui ≥ dateRecevabilité). Item bloquant si `dateMiseEnDemeure` absente.
  - **Pluralité** : `pluraliteEtablie = nombrePersonnesConcernees >= 2` (placées dans une situation similaire).
  - **Checklist procédurale** : mise en demeure écrite, délai 6 mois, identité de situation, saisine TJ (action de groupe judiciaire), phase de réparation individuelle ultérieure. Chaque item = `{ libelle, obligatoire, baseJuridique }`.
  - **Verdict recevabilité** : `RECEVABLE` (qualité + pluralité + carence respectée), `PREMATURE` (carence non écoulée), `IRRECEVABLE_QUALITE` (organisation non habilitée), `INFO_MANQUANTE` (mise en demeure absente).
  - `baseJuridique` : L. 1134-7 à L. 1134-10 Code travail ; L. 1132-1 (critères de discrimination).
- Output persisté dans `action_groupe_discrimination_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/action-groupe-discrimination-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| typeOrganisation inconnu | 400 |
| motifDiscrimination inconnu | 400 |
| nombrePersonnesConcernees < 1 | 400 |
| dateMiseEnDemeure future | 400 |
| caseFile inaccessible | 404 |

---

## Source juridique

- **L. 1134-7 Code travail** — organisations habilitées à exercer l'action de groupe (syndicats représentatifs, associations régulièrement déclarées depuis ≥ 5 ans).
- **L. 1134-8** — objet : cessation du manquement et/ou réparation des préjudices.
- **L. 1134-9** — mise en demeure préalable de l'employeur et délai de 6 mois avant saisine.
- **L. 1134-10** — articulation avec la réparation individuelle.
- **L. 1132-1** — liste des critères de discrimination prohibés.
- Loi J21 du 18/11/2016 — introduction de l'action de groupe en discrimination.

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `motifDiscrimination` | enum | dérivé de `motifNullitePressenti` / synthèse discrimination | [x] prompt (best-effort) — sinon saisie manuelle |
| `dateMiseEnDemeure` | date | `dateMiseEnDemeureDiscrimination` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Flag CONTEXTUAL pivot** : `action_groupe_discrimination_envisagee` (niveau 3, FR-only, default false) — nouveau flag. Bascule CONTEXTUAL quand l'IA détecte une discrimination collective + intention d'action de groupe (mention « action de groupe », « discrimination systémique », « plusieurs salariés », « organisation syndicale / association »).

---

## Critères d'acceptation

- [ ] POST `typeOrganisation=SYNDICAT_REPRESENTATIF`, mise en demeure J-200, `nombrePersonnesConcernees=5` → verdict `RECEVABLE`, `delaiCarenceRespecte=true`
- [ ] POST mise en demeure J-30 → verdict `PREMATURE`, `dateRecevabiliteSaisine` = mise en demeure + 6 mois
- [ ] POST `typeOrganisation=AUTRE` → verdict `IRRECEVABLE_QUALITE`
- [ ] POST sans `dateMiseEnDemeure` → verdict `INFO_MANQUANTE` + item bloquant
- [ ] POST `nombrePersonnesConcernees=0` → 400 ; `dateMiseEnDemeure` future → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; upsert sur double POST
- [ ] Isolation workspace
- [ ] Seed `decision_tool_visibility_rules` : CONTEXTUAL, trigger_field=`action_groupe_discrimination_envisagee`, trigger_value=`true`
- [ ] `F-DT-90-action-groupe-discrimination` dans `KNOWN_FRONTEND_TOOL_IDS`

## Plan de test minimal

- **UT** `ActionGroupeDiscriminationAnalyzerTest` : ≥ 6 cas (recevable, prématuré, irrecevable qualité, info manquante, pluralité, calcul carence 6 mois)
- **IT** `ActionGroupeDiscriminationControllerIT` : ≥ 5 cas (200 nominal, 400 country, 400 nombrePersonnes, 404 isolation, upsert)

## Tables / endpoints / composants impactés

- **Nouvelle table** `action_groupe_discrimination_analyses`
- **Migration Liquibase** + seed visibility rules
- **Endpoint** `ActionGroupeDiscriminationController`
- **Service** `ActionGroupeDiscriminationService` + **Analyzer** `ActionGroupeDiscriminationAnalyzer`
- **Extension** `TravailExtractedData` : `dateMiseEnDemeureDiscrimination`, flag `actionGroupeDiscriminationEnvisagee` + prompt
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-10)
- Génération de la mise en demeure (générateur futur)
- Gestion de la phase individuelle de réparation (L. 1134-10) — V2
- Vérification de l'agrément réel de l'association (saisie déclarative `typeOrganisation`)
