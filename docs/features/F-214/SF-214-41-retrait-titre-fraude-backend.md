# Mini-spec — F-214 / SF-214-41 — Retrait titre fraude L. 412-7 — backend

## Identifiant

`F-214 / SF-214-41`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Analyser la validité d'une décision de retrait de titre de séjour pour fraude (L. 412-7 CESEDA), notamment les conditions de mise en demeure préalable, la notion de mariage gris et les voies de recours.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/retrait-titre-fraude-analysis`
- Body : `dateRetrait` (LocalDate, requis), `motifRetrait` (enum `MARIAGE_GRIS` | `FAUSSES_DECLARATIONS` | `FRAUDE_DOCUMENTAIRE` | `PERTE_CONDITIONS`), `miseEnDemeurePrEalable` (boolean), `dateMiseEnDemeure` (LocalDate, optionnel)
- Analyzer `RetraitTitreFraudeAnalyzer` :
  - Vérifie procédure contradictoire préalable obligatoire (L. 412-7 + alinéa L. 411-5 à vérifier)
  - `vicesDeProcedure` : liste (absence mise en demeure, délai insuffisant, défaut contradictoire)
  - `motifsContestation` : selon motif (mariage gris : communauté de vie prouvée, enfants, photos ; fraude documentaire : authenticité, erreur)
  - `delaiRecoursTA` = dateRetrait + 2 mois
  - `statut` ∈ {`RECOURS_POSSIBLE`, `URGENT`, `PRESCRIT`}
- Output persisté dans `retrait_titre_fraude_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/retrait-titre-fraude-analysis` → 200 ou 404

---

## Source juridique

- **L. 412-7 CESEDA** (à vérifier numérotation — ancienne L. 313-5) — retrait ou non-renouvellement pour fraude.
- **L. 411-5 CESEDA** (à vérifier) — procédure contradictoire préalable.
- **CE 15 juillet 2004, n° 258040** — conditions retrait titre pour fraude (mariage gris).
- **CE 23 octobre 2009, n° 317866** — retrait documentation frauduleuse.

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `dateRetrait` | date | Absent | Extension record + prompt (`retraitTitreDateRetrait`) |
| `motifRetrait` | enum | Absent | Extension record + prompt (`retraitTitreMotif`) |

**Nouveau flag CONTEXTUAL** : `retraitTitreFraudeDetecte` (boolean) — extraction : mentions "retrait de titre", "fraude", "mariage blanc", "mariage gris", "fausses déclarations", "L.412-7". Ajouté dans `ImmigrationExtractedData` + prompt.

---

## Critères d'acceptation

- [x] POST MARIAGE_GRIS sans mise en demeure → vicesDeProcedure non vide
- [x] POST RECOURS_POSSIBLE → delaiRecoursTA calculé
- [x] POST PRESCRIT > 2 mois → statut PRESCRIT
- [x] POST workspace BE → 400
- [x] GET sans POST → 404
- [x] Isolation workspace
- [x] `F-IM-45-retrait-titre-fraude-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed : CONTEXTUAL, trigger_field=`retrait_titre_fraude_detecte`

## Plan de test minimal

- **UT** `RetraitTitreFraudeAnalyzerTest` : 6+ cas
- **IT** `RetraitTitreFraudeControllerIT` : 5+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `retrait_titre_fraude_analyses`
- **Migration Liquibase** + seed visibility rules
- **Extension** `ImmigrationExtractedData` : flag `retraitTitreFraudeDetecte` + champs `retraitTitreDateRetrait`, `retraitTitreMotif`
- **Endpoint** `RetraitTitreFraudeController`

## Hors périmètre

- Composant Angular (SF-214-42)
