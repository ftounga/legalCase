# Mini-spec — F-218 / SF-218-29 — NAO : négociation annuelle obligatoire — backend

## Identifiant

`F-218 / SF-218-29`

## Feature parente

`F-218c` — IRP / négociation collective FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-31

## Branche Git

`feat/SF-218-29-nao-negociation-annuelle-backend`

---

## Objectif

Vérifier la **conformité de la négociation annuelle obligatoire (NAO)** dans les entreprises pourvues d'un délégué syndical (art. L.2242-1 à L.2242-8 CT) : déclenchement de la négociation sur les blocs obligatoires (rémunération/temps de travail/partage de la valeur ; égalité professionnelle et QVT), respect de la périodicité (annuelle, ou jusqu'à 4 ans par accord de méthode), établissement d'un PV de désaccord, et exposition de l'employeur aux sanctions (pénalité égalité F/H, délit d'entrave). Outil **conformité employeur + calculateur de délai**. Aucun outil existant ne couvre la NAO (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/nao-negociation-annuelle-analysis`
- Body :
  - `effectif` (int, requis) — effectif (NAO obligatoire dès lors qu'un DS est désigné, soit ≥ 50 salariés en pratique)
  - `delegueSyndicalPresent` (boolean, requis) — présence d'au moins un délégué syndical (déclencheur de l'obligation)
  - `blocRemunerationNegocie` (boolean, requis) — bloc 1 : rémunération, temps de travail, partage de la valeur ajoutée (L.2242-15)
  - `blocEgaliteQvtNegocie` (boolean, requis) — bloc 2 : égalité professionnelle F/H et qualité de vie au travail (L.2242-17)
  - `accordMethodePeriodicite` (boolean, défaut false) — accord de méthode portant la périodicité au-delà de l'année (max 4 ans, L.2242-11)
  - `dateDerniereNegociation` (LocalDate, optionnel) — date de la dernière négociation engagée
  - `periodiciteMois` (int, défaut 12) — périodicité retenue en mois (12 par défaut, 13–48 si accord de méthode)
  - `pvDesaccordEtabli` (boolean, défaut false) — PV de désaccord établi en cas d'échec
  - `negociationAboutie` (boolean, défaut false) — la négociation a abouti à un accord
- Analyzer `NaoNegociationAnnuelleAnalyzer` :
  - **Applicabilité** : si `delegueSyndicalPresent=false` → `statut = NON_APPLICABLE` (pas de DS → pas de NAO) + note « obligation conditionnée à la présence d'un délégué syndical ».
  - **Checklist de conformité** (si applicable) : `{ item, conforme, obligatoire, commentaire }` :
    1. Bloc rémunération négocié (obligatoire).
    2. Bloc égalité pro / QVT négocié (obligatoire).
    3. Périodicité respectée (obligatoire ; ≤ 12 mois sans accord, ≤ 48 mois avec accord de méthode).
    4. PV de désaccord établi (obligatoire si `negociationAboutie=false`).
  - **Calculateur de délai** : si `dateDerniereNegociation` présent → `dateProchaineEcheance` = `dateDerniereNegociation + periodiciteMois`. `joursAvantEcheance` (peut être négatif). `statutEcheance` ∈ { `A_JOUR`, `ECHEANCE_PROCHE`, `DEPASSEE` } (ECHEANCE_PROCHE si 0 ≤ jours ≤ 60, DEPASSEE si < 0).
  - **Validation périodicité** : si `periodiciteMois > 12` sans `accordMethodePeriodicite=true` → item périodicité non conforme ; si `periodiciteMois > 48` → 400.
  - **Verdict** :
    - applicable + tous items obligatoires conformes + échéance non dépassée → `statut = CONFORME`.
    - applicable + au moins un item obligatoire non conforme ou `statutEcheance=DEPASSEE` → `statut = NON_CONFORME`.
    - blocs non engagés alors que DS présent → `statut = NON_CONFORME` + `risqueEntrave = ELEVE` (délit d'entrave L.2243-2, pénalité égalité F/H jusqu'à 1 % de la masse salariale).
  - `risqueEntrave` ∈ { `ELEVE`, `MODERE`, `FAIBLE` } (ELEVE si blocs non négociés, MODERE si NON_CONFORME, FAIBLE si CONFORME).
  - `baseJuridique` : art. L.2242-1 à L.2242-8 CT (+ L.2242-15, L.2242-17) — annoté `(à vérifier par avocat)`.
- Output persisté dans `nao_negociation_annuelle_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/nao-negociation-annuelle-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| un des booléens requis absent (null) | 400 |
| effectif ≤ 0 | 400 |
| periodiciteMois < 1 ou > 48 | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Art. L.2242-1 CT** — dans les entreprises où sont constituées une ou plusieurs sections syndicales d'organisations représentatives, l'employeur engage chaque année une négociation.
- **Art. L.2242-15 CT** — bloc « rémunération, temps de travail et partage de la valeur ajoutée ».
- **Art. L.2242-17 CT** — bloc « égalité professionnelle entre les femmes et les hommes et qualité de vie au travail » (inclut le droit à la déconnexion).
- **Art. L.2242-11 CT** — accord de méthode pouvant porter la périodicité à 4 ans maximum.
- **Art. L.2242-8 / L.2243-2 CT** — défaut de négociation : délit d'entrave et pénalité financière en matière d'égalité professionnelle.

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `effectif` | entier | `effectifEntreprise` (existant) | Réutiliser si présent |
| `delegueSyndicalPresent` | booléen | `delegueSyndicalPresent` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Flag CONTEXTUAL pivot** : `nao_detectee` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte des signaux de NAO (mentions « négociation annuelle obligatoire », « NAO », « PV de désaccord », « réunion de négociation salariale », « accord de méthode », « délégué syndical » dans un contexte de négociation collective).

---

## Critères d'acceptation

- [ ] POST `delegueSyndicalPresent=false` → `statut=NON_APPLICABLE`
- [ ] POST DS présent + 2 blocs négociés + périodicité 12 + échéance non dépassée → `statut=CONFORME`, `risqueEntrave=FAIBLE`
- [ ] POST DS présent + blocs non négociés → `statut=NON_CONFORME`, `risqueEntrave=ELEVE`
- [ ] POST `dateDerniereNegociation` à -2 mois, périodicité 12 → `statutEcheance=A_JOUR` ; à -13 mois → `DEPASSEE`, `statut=NON_CONFORME`
- [ ] POST `periodiciteMois=24` sans accord de méthode → item périodicité non conforme ; avec accord → conforme
- [ ] `negociationAboutie=false` + `pvDesaccordEtabli=false` → item PV non conforme
- [ ] POST booléen requis null → 400 ; effectif=0 → 400 ; periodiciteMois=60 → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`nao_detectee`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL
- [ ] `F-DT-66-nao-negociation-annuelle` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `NaoNegociationAnnuelleAnalyzerTest` : ≥ 6 cas (pas de DS → NON_APPLICABLE, conforme complet → CONFORME, blocs non négociés → NON_CONFORME + entrave ELEVE, calcul échéance A_JOUR/PROCHE/DEPASSEE, validation périodicité 24 mois avec/sans accord, PV de désaccord obligatoire)
- **IT** `NaoNegociationAnnuelleControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `nao_negociation_annuelle_analyses`
- **Migrations** : `create-nao-negociation-annuelle-analyses.xml` + `seed-nao-negociation-annuelle-visibility.xml` (reconfirmer les numéros libres dans le worktree)
- **Endpoint** `NaoNegociationAnnuelleController` (POST + GET)
- **Service** `NaoNegociationAnnuelleService` + **Analyzer** `NaoNegociationAnnuelleAnalyzer`
- **Extension** `TravailExtractedData` : champ `delegueSyndicalPresent` + flag `naoDetectee` + prompt `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-30)
- Validité d'un accord d'entreprise issu de la NAO (F-DT-67, situation distincte)
- Index égalité professionnelle F/H (F-DT-101, situation distincte)
- Élections CSE (F-DT-65, situation distincte)
