# Mini-spec — F-218 / SF-218-15 — Journaliste professionnel : statut et clauses spécifiques — backend

## Identifiant

`F-218 / SF-218-15`

## Feature parente

`F-218b` — Régimes catégoriels FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-15-journaliste-statut-backend`

---

## Objectif

Analyser le régime du **journaliste professionnel** (art. L.7111-1 et s. CT) lors d'une rupture : qualifier la rupture éligible à la **clause de cession** (cession du titre) ou à la **clause de conscience** (changement notable de l'orientation du journal), calculer l'**indemnité de congédiement** spécifique et signaler le passage par la **commission arbitrale paritaire** lorsque l'ancienneté dépasse 15 ans. Aucun outil existant ne couvre le statut journaliste (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/journaliste-statut-analysis`
- Body :
  - `dateEntree` (LocalDate, requis) — début du contrat
  - `dateRupture` (LocalDate, requis) — date de notification de la rupture
  - `typeRupture` (enum `LICENCIEMENT` | `CLAUSE_CESSION` | `CLAUSE_CONSCIENCE` | `DEMISSION` | `FAUTE_GRAVE`, requis)
  - `salaireMensuelMoyen` (BigDecimal, requis) — base de l'indemnité de congédiement
  - `carteIdentiteProfessionnelle` (boolean, défaut true) — détention de la carte de presse (présomption de qualité de journaliste)
  - `cessionTitreConstatee` (boolean, défaut false) — fait générateur de la clause de cession
  - `changementOrientationConstate` (boolean, défaut false) — fait générateur de la clause de conscience
- Analyzer `JournalisteStatutAnalyzer` :
  - **Qualification du statut** : journaliste professionnel présumé si `carteIdentiteProfessionnelle=true`. Champ `statutJournaliste` ∈ { `CONFIRME`, `A_QUALIFIER` }.
  - **Validité de la clause invoquée** :
    - `CLAUSE_CESSION` : valable si `cessionTitreConstatee=true` (cession ou cessation de publication, art. L.7112-5 1°). La rupture est alors assimilée à un licenciement ouvrant droit à indemnité.
    - `CLAUSE_CONSCIENCE` : valable si `changementOrientationConstate=true` (changement notable du caractère/orientation portant atteinte à l'honneur ou aux intérêts moraux, art. L.7112-5 2°/3°). Idem assimilation licenciement.
    - Champ `clauseValide` ∈ { `VALIDE`, `NON_VALIDE`, `SANS_OBJET` } + `motif`.
  - **Indemnité de congédiement** (art. L.7112-3) : 1 mois de salaire par année ou fraction d'année d'ancienneté (plafond légal 15 mois sans passage commission). Champ `indemniteCongediement`. Annoter `// indemnité de congédiement L.7112-3 — base 1 mois/année`.
  - **Commission arbitrale** : si ancienneté > 15 ans OU faute grave/fautes répétées invoquées → indemnité fixée souverainement par la **commission arbitrale paritaire** (art. L.7112-4) ; champ `commissionArbitraleRequise=true` + note « le montant ci-dessus est plafonné à 15 mois ; au-delà, compétence exclusive de la commission arbitrale ».
  - **Exclusion** : `FAUTE_GRAVE` → pas d'indemnité de congédiement de droit (renvoi commission) ; `DEMISSION` → pas d'indemnité.
  - **Verdict global** : `RUPTURE_ASSIMILEE_LICENCIEMENT` / `INDEMNITE_DUE` / `COMMISSION_ARBITRALE` / `INDEMNITE_NON_DUE`.
  - `baseJuridique` : art. L.7111-1 à L.7113-12 CT ; L.7112-3 (indemnité de congédiement) ; L.7112-4 (commission arbitrale) ; L.7112-5 (clauses de cession et de conscience) — annoté `(à vérifier par avocat)`.
- Output persisté dans `journaliste_statut_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/journaliste-statut-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| dateEntree ou dateRupture absente | 400 |
| dateRupture < dateEntree | 400 |
| salaireMensuelMoyen négatif/absent | 400 |
| typeRupture inconnue | 400 |
| `typeRupture=CLAUSE_CESSION` sans `cessionTitreConstatee` ni indication (validation métier) | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Art. L.7111-1 à L.7113-12 CT** — statut du journaliste professionnel.
- **Art. L.7112-3 CT** — indemnité de congédiement (1 mois par année d'ancienneté).
- **Art. L.7112-4 CT** — commission arbitrale paritaire au-delà de 15 ans d'ancienneté ou en cas de faute grave/fautes répétées.
- **Art. L.7112-5 CT** — clause de cession (cession du titre) et clause de conscience (changement d'orientation).
- Carte d'identité de journaliste professionnel (CCIJP) — présomption de qualité.

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `dateEntree` | date | `dateEntree` (existant) | Réutiliser |
| `dateRupture` | date | `dateRupture` / `dateLicenciement` (existant) | Réutiliser si présent |
| `carteIdentiteProfessionnelle` | booléen | `journalisteCartePresse` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Flag CONTEXTUAL pivot** : `statut_journaliste_detecte` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte un statut journaliste (mentions « journaliste », « carte de presse », « clause de cession », « clause de conscience », « rédaction », « organe de presse », « pigiste »).

---

## Critères d'acceptation

- [ ] POST `typeRupture=CLAUSE_CESSION`, `cessionTitreConstatee=true`, ancienneté 5 ans → `clauseValide=VALIDE`, `verdictGlobal=RUPTURE_ASSIMILEE_LICENCIEMENT`, `indemniteCongediement=5×salaire`
- [ ] POST `typeRupture=CLAUSE_CONSCIENCE`, `changementOrientationConstate=true` → `clauseValide=VALIDE`
- [ ] POST `typeRupture=CLAUSE_CONSCIENCE`, `changementOrientationConstate=false` → `clauseValide=NON_VALIDE` + motif
- [ ] POST ancienneté 18 ans → `commissionArbitraleRequise=true` + note plafond 15 mois
- [ ] POST `typeRupture=FAUTE_GRAVE` → renvoi commission, `INDEMNITE_NON_DUE` de droit
- [ ] POST `carteIdentiteProfessionnelle=false` → `statutJournaliste=A_QUALIFIER`
- [ ] POST dateRupture < dateEntree → 400 ; salaire négatif → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`statut_journaliste_detecte`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL
- [ ] `F-DT-105-journaliste-statut` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `JournalisteStatutAnalyzerTest` : ≥ 6 cas (clause cession valide, clause conscience valide / non valide, indemnité congédiement 1 mois/année, commission arbitrale > 15 ans, faute grave exclusion, statut à qualifier sans carte)
- **IT** `JournalisteStatutControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `journaliste_statut_analyses`
- **Migrations** : `create-journaliste-statut-analyses.xml` + `seed-journaliste-statut-visibility.xml` (reconfirmer les numéros libres dans le worktree)
- **Endpoint** `JournalisteStatutController` (POST + GET)
- **Service** `JournalisteStatutService` + **Analyzer** `JournalisteStatutAnalyzer`
- **Extension** `TravailExtractedData` : champ `journalisteCartePresse` + flag `statutJournalisteDetecte` + prompt `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-16)
- Évaluation chiffrée du montant fixé par la commission arbitrale (compétence souveraine — non simulable)
- Régime des pigistes : présomption de salariat / requalification (autre situation)
- Droits d'auteur du journaliste (propriété intellectuelle, hors droit du travail)
