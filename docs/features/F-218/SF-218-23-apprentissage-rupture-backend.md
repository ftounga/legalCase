# Mini-spec — F-218 / SF-218-23 — Apprentissage : validité de la rupture du contrat — backend

## Identifiant

`F-218 / SF-218-23`

## Feature parente

`F-218b` — Régimes catégoriels FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-23-apprentissage-rupture-backend`

---

## Objectif

Analyser la **validité de la rupture d'un contrat d'apprentissage** dont le régime est **hybride** (art. L.6222-18 et s. CT) : distinguer la rupture pendant les **45 premiers jours** (libre, sans motif) de la rupture après ce délai (limitée à : accord écrit des parties, faute grave, force majeure, inaptitude médicale, exclusion définitive du CFA), afin de qualifier le motif invoqué et signaler les conséquences (saisine du conseil de prud'hommes, indemnités). Aucun outil existant ne couvre la rupture d'apprentissage (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/apprentissage-rupture-analysis`
- Body :
  - `dateDebutContrat` (LocalDate, requis) — début de l'exécution du contrat d'apprentissage
  - `dateRupture` (LocalDate, requis) — date de la rupture
  - `auteurRupture` (enum `EMPLOYEUR` | `APPRENTI`, requis)
  - `motifRupture` (enum `ACCORD_PARTIES` | `FAUTE_GRAVE` | `FORCE_MAJEURE` | `INAPTITUDE` | `EXCLUSION_DEFINITIVE_CFA` | `SANS_MOTIF`, requis)
  - `apprentiMajeur` (boolean, défaut true) — pertinent pour certaines formalités
- Analyzer `ApprentissageRuptureAnalyzer` :
  - **Calcul de la période** : `joursDepuisDebut = dateDebut → dateRupture`. Période `DANS_45_PREMIERS_JOURS` (≤ 45 jours de formation pratique en entreprise) vs `APRES_45_JOURS`. Constante `SEUIL_RUPTURE_LIBRE_JOURS = 45`.
  - **Validité dans les 45 jours** : rupture libre par l'une ou l'autre partie sans motif ni indemnité (forme écrite requise). Si `motifRupture=SANS_MOTIF` et période `DANS_45_PREMIERS_JOURS` → `validite = VALIDE` (rupture libre).
  - **Validité après 45 jours** : `motifRupture` doit appartenir à { `ACCORD_PARTIES`, `FAUTE_GRAVE`, `FORCE_MAJEURE`, `INAPTITUDE`, `EXCLUSION_DEFINITIVE_CFA` }. Si `SANS_MOTIF` après 45 jours → `validite = NON_VALIDE` + note « rupture irrégulière : dommages-intérêts possibles, saisine CPH ».
  - **Cas particuliers** :
    - `FAUTE_GRAVE` → la résiliation passe désormais par un licenciement (procédure) ; `INAPTITUDE` constatée par le médecin du travail (employeur dispensé de reclassement, L.6222-18-1).
    - `EXCLUSION_DEFINITIVE_CFA` → motif valable de rupture par l'employeur.
  - Verdict `validite` ∈ { `VALIDE`, `NON_VALIDE`, `A_SECURISER` } (`A_SECURISER` si formalité manquante, p. ex. faute grave sans procédure de licenciement) + `motif`.
  - **Conséquences** : champ `consequences[]` (ex. « indemnités de rupture irrégulière », « saisine CPH », « procédure de licenciement requise »).
  - **Verdict global** : `RUPTURE_REGULIERE` / `RUPTURE_IRREGULIERE` / `RUPTURE_A_SECURISER`.
  - `baseJuridique` : art. L.6222-18 (rupture 45 jours / accord) ; L.6222-18-1 (inaptitude) ; L.6222-21 (poursuite formation) ; L.6222-23 — annoté `(à vérifier par avocat)`.
- Output persisté dans `apprentissage_rupture_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/apprentissage-rupture-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| dateDebutContrat ou dateRupture absente | 400 |
| dateRupture < dateDebutContrat | 400 |
| auteurRupture / motifRupture inconnu | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Art. L.6222-18 CT** — rupture du contrat d'apprentissage : libre durant les 45 premiers jours (en entreprise), puis sur accord écrit des parties ou, à défaut, par le conseil de prud'hommes pour faute grave / manquements répétés / inaptitude.
- **Art. L.6222-18-1 CT** — rupture pour inaptitude médicale constatée par le médecin du travail.
- **Art. L.6222-21 CT** — exclusion définitive du CFA : motif de rupture.
- **Art. L.6222-23 CT** — poursuite de la formation en cas de rupture.
- Réforme 2018 (loi Avenir professionnel) : suppression de la résiliation judiciaire systématique, alignement partiel sur le licenciement.

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `dateDebutContrat` | date | `dateEntree` (existant) | Réutiliser si présent |
| `dateRupture` | date | `dateRupture` / `dateLicenciement` (existant) | Réutiliser si présent |
| `motifRupture` | enum | `apprentissageMotifRupture` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Flag CONTEXTUAL pivot** : `apprentissage_rupture_detectee` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte une rupture d'apprentissage (mentions « contrat d'apprentissage », « apprenti », « CFA », « maître d'apprentissage », « rupture apprentissage », « 45 jours »).

---

## Critères d'acceptation

- [ ] POST rupture J+20, `motifRupture=SANS_MOTIF` → période `DANS_45_PREMIERS_JOURS`, `validite=VALIDE`
- [ ] POST rupture J+90, `motifRupture=SANS_MOTIF` → `validite=NON_VALIDE`, `verdictGlobal=RUPTURE_IRREGULIERE`, consequences saisine CPH
- [ ] POST J+90, `motifRupture=ACCORD_PARTIES` → `validite=VALIDE`
- [ ] POST J+90, `motifRupture=FAUTE_GRAVE` → `validite=A_SECURISER` (procédure de licenciement requise)
- [ ] POST J+120, `motifRupture=INAPTITUDE` → `validite=VALIDE` (constat médecin du travail)
- [ ] POST `motifRupture=EXCLUSION_DEFINITIVE_CFA` → `validite=VALIDE`
- [ ] POST dateRupture < dateDebutContrat → 400 ; motif inconnu → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`apprentissage_rupture_detectee`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL
- [ ] `F-DT-110-apprentissage-rupture` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `ApprentissageRuptureAnalyzerTest` : ≥ 6 cas (rupture libre 45 j, irrégulière sans motif après 45 j, accord parties valide, faute grave A_SECURISER, inaptitude valide, exclusion CFA valide)
- **IT** `ApprentissageRuptureControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `apprentissage_rupture_analyses`
- **Migrations** : `create-apprentissage-rupture-analyses.xml` + `seed-apprentissage-rupture-visibility.xml` (reconfirmer les numéros libres dans le worktree)
- **Endpoint** `ApprentissageRuptureController` (POST + GET)
- **Service** `ApprentissageRuptureService` + **Analyzer** `ApprentissageRuptureAnalyzer`
- **Extension** `TravailExtractedData` : champ `apprentissageMotifRupture` + flag `apprentissageRuptureDetectee` + prompt `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-24)
- Chiffrage des dommages-intérêts en cas de rupture irrégulière (renvoi vers calculateurs existants)
- Régime du contrat de professionnalisation (situation distincte)
- Régime du stagiaire (F-DT-109, SF-218-21)
