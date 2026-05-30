# Mini-spec — F-218 / SF-218-21 — Stagiaire : gratification minimale et requalification en CDI — backend

## Identifiant

`F-218 / SF-218-21`

## Feature parente

`F-218b` — Régimes catégoriels FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-21-stagiaire-gratification-backend`

---

## Objectif

Outiller le contentieux du **stage en milieu professionnel** (art. L.124-1 et s. Code de l'éducation) : vérifier le seuil de déclenchement et calculer la **gratification minimale obligatoire** (au-delà de 2 mois / 44 jours de présence), et apprécier le **risque de requalification du stage en contrat de travail (CDI)** lorsque le stagiaire occupe un poste de travail permanent, exécute des missions hors projet pédagogique ou dépasse la durée maximale légale. Aucun outil existant ne couvre le régime du stagiaire (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/stagiaire-gratification-analysis`
- Body :
  - `dateDebutStage` (LocalDate, requis)
  - `dateFinStage` (LocalDate, requis)
  - `nombreJoursPresence` (int, requis) — jours de présence effective (1 jour = 7 h ; règle des 22 jours/mois)
  - `gratificationMensuelleVersee` (BigDecimal, défaut 0) — gratification réellement perçue
  - `tauxHoraireConventionnel` (BigDecimal, optionnel) — si CCN plus favorable que le minimum légal
  - `missionsHorsProjetPedagogique` (boolean, défaut false) — exécution de tâches sans lien avec le projet pédagogique
  - `posteTravailPermanent` (boolean, défaut false) — occupation d'un poste correspondant à une tâche régulière de l'entreprise
- Analyzer `StagiaireGratificationAnalyzer` :
  - **Seuil de gratification** : obligatoire si présence > 2 mois consécutifs ou non (au-delà de 44 jours / 308 h sur l'année d'enseignement). Champ `gratificationObligatoire` (boolean) + `seuilAtteint`.
  - **Gratification minimale** : `gratificationMinHoraire = TAUX_HORAIRE_GRATIFICATION × heures` où `TAUX_HORAIRE_GRATIFICATION` = pourcentage du plafond horaire de la Sécurité sociale (constante `// gratification minimale = 15 % du plafond horaire SS — taux à actualiser annuellement (plafond SS)`). Retenir le plus favorable entre minimum légal et `tauxHoraireConventionnel`. Champs `gratificationMinimaleDue`, `rappelGratification` (= max(0, due − versée)).
  - **Risque de requalification en CDI** : score à partir des indices `missionsHorsProjetPedagogique`, `posteTravailPermanent`, dépassement de la **durée maximale (6 mois / 924 h par année d'enseignement)**. Verdict `risqueRequalification` ∈ { `ELEVE`, `MODERE`, `FAIBLE` } + `motifs[]`. ELEVE si ≥ 2 indices présents ou durée > 6 mois.
  - **Conséquence requalification** : si `risqueRequalification=ELEVE` → note « requalification en CDI possible : rappel de salaire sur la base du SMIC/minimum conventionnel + indemnités de rupture ».
  - **Verdict global** : `STAGE_CONFORME` / `RAPPEL_GRATIFICATION` / `REQUALIFICATION_PROBABLE`.
  - `baseJuridique` : art. L.124-1 et s. Code de l'éducation ; art. L.124-6 (gratification) ; D.124-1 et s. ; art. L.124-5 (durée max 6 mois) ; L.124-8 (requalification, poste permanent) — annoté `(à vérifier par avocat)`.
- Output persisté dans `stagiaire_gratification_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/stagiaire-gratification-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| dateDebutStage ou dateFinStage absente | 400 |
| dateFinStage < dateDebutStage | 400 |
| nombreJoursPresence négatif/absent | 400 |
| gratificationMensuelleVersee / tauxHoraireConventionnel négatif | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Art. L.124-1 et s. Code de l'éducation** — encadrement des périodes de formation en milieu professionnel et des stages.
- **Art. L.124-6 + D.124-6 Code de l'éducation** — gratification minimale obligatoire au-delà de 2 mois (44 jours / 308 h), égale à 15 % du plafond horaire de la Sécurité sociale.
- **Art. L.124-5 Code de l'éducation** — durée maximale du stage : 6 mois par année d'enseignement (924 h).
- **Art. L.124-8 Code de l'éducation** — interdiction d'occuper un poste de travail permanent ; requalification en contrat de travail.
- Taux de gratification et plafond SS — constantes à actualiser annuellement.

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `dateDebutStage` | date | `dateEntree` / `dateDebutStage` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `dateFinStage` | date | `dateFinStage` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Flag CONTEXTUAL pivot** : `stage_detecte` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte un stage (mentions « convention de stage », « stagiaire », « gratification », « établissement d'enseignement », « tuteur de stage », « PFMP », « école »).

---

## Critères d'acceptation

- [ ] POST stage 4 mois, `nombreJoursPresence=80`, gratification 0 → `gratificationObligatoire=true`, `rappelGratification` > 0
- [ ] POST stage 1 mois, `nombreJoursPresence=20` → `gratificationObligatoire=false` (seuil non atteint)
- [ ] POST `tauxHoraireConventionnel` > minimum légal → gratification calculée sur le taux conventionnel
- [ ] POST `posteTravailPermanent=true` + `missionsHorsProjetPedagogique=true` → `risqueRequalification=ELEVE`, `verdictGlobal=REQUALIFICATION_PROBABLE`
- [ ] POST durée > 6 mois → `risqueRequalification=ELEVE` (dépassement durée max)
- [ ] POST stage conforme + gratification versée ≥ due → `STAGE_CONFORME`, `rappelGratification=0`
- [ ] POST dateFinStage < dateDebutStage → 400 ; montants négatifs → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`stage_detecte`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL
- [ ] `F-DT-109-stagiaire-gratification-requalification` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `StagiaireGratificationAnalyzerTest` : ≥ 6 cas (seuil atteint / non atteint, rappel gratification, taux conventionnel plus favorable, requalification ELEVE 2 indices, requalification durée > 6 mois, stage conforme)
- **IT** `StagiaireGratificationControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `stagiaire_gratification_analyses`
- **Migrations** : `create-stagiaire-gratification-analyses.xml` + `seed-stagiaire-gratification-visibility.xml` (reconfirmer les numéros libres dans le worktree)
- **Endpoint** `StagiaireGratificationController` (POST + GET)
- **Service** `StagiaireGratificationService` + **Analyzer** `StagiaireGratificationAnalyzer`
- **Extension** `TravailExtractedData` : champs `dateDebutStage`, `dateFinStage` + flag `stageDetecte` + prompt `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-22)
- Chiffrage complet des indemnités de rupture en cas de requalification (renvoi vers calculateurs IL existants)
- Stages relevant de la formation professionnelle continue (régime distinct)
- Régime de l'apprentissage (F-DT-110, situation distincte SF-218-23)
