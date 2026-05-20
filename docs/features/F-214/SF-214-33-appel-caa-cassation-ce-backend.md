# Mini-spec — F-214 / SF-214-33 — Appel CAA + cassation CE délais — backend

## Identifiant

`F-214 / SF-214-33`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Calculer les délais d'appel devant la Cour administrative d'appel (CAA) et de cassation devant le Conseil d'État (CE) dans les contentieux des étrangers, après un jugement de tribunal administratif (TA).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/appel-caa-cassation-analysis`
- Body : `dateJugementTA` (LocalDate, requis), `typeDecisionTA` (enum `REJET` | `ANNULATION`), `typeContentieux` (enum `OQTF` | `REFUS_TITRE` | `EXPULSION` | `AUTRE`), `delaiSpecialOQTF` (boolean — si OQTF sans délai → appel 15 j au lieu de 1 mois)
- Calculator `AppelCaaCassationCalculator` :
  - `dateEcheanceAppelCaa` = dateJugementTA + 1 mois (droit commun CJA) ou + 15 j (si OQTF sans délai spécial)
  - `dateEcheanceCassationCe` = dateArretCAA + 2 mois (à saisir manuellement si connu)
  - `courAppelCompetente` : CAA compétente selon ressort du TA (ex. TA Paris → CAA Paris)
  - `motifsAppelPossibles` : liste (erreur droit, dénaturation, vice procédure, moyen d'ordre public)
  - `filtrePorvoisCassation` : boolean — en matière OQTF, cassation CE filtrée par art. L. 821-2 CJA (admission du pourvoi)
  - `statut` ∈ {`APPEL_POSSIBLE`, `URGENT` (< 15 j), `PRESCRIT`, `EN_CASSATION`}
- Output persisté dans `appel_caa_cassation_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/appel-caa-cassation-analysis` → 200 ou 404

---

## Source juridique

- **CJA L. 811-1 + R. 811-1** — appel CAA, délai 1 mois.
- **CJA L. 821-1 + R. 821-1** — cassation CE, délai 2 mois.
- **L. 614-6 CESEDA** (à vérifier) — délai spécial 15 j appel OQTF sans délai.
- **CE 5 juin 2013, n° 357775** — filtre pourvoi en matière OQTF.
- **Loi 26/01/2024** : modifications délais OQTF.

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `typeContentieux` | enum | `typeProcedureDetectee` (proxy) | Dériver |
| `dateJugementTA` | date | Absent | Extension record + prompt (`recoursDateJugementTA`) |

**Nouveau flag CONTEXTUAL** : `recoursEnvisageDetecte` (boolean) — extraction : mentions "jugement", "décision TA", "appel", "CAA", "pourvoi en cassation", "CE". Ajouté dans `ImmigrationExtractedData` + prompt.

---

## Critères d'acceptation

- [x] POST OQTF delaiSpecialOQTF=true → dateEcheanceAppelCaa = jugement + 15 j
- [x] POST droit commun → dateEcheanceAppelCaa = jugement + 1 mois
- [x] POST PRESCRIT → 200 + statut PRESCRIT
- [x] POST workspace BE → 400
- [x] GET sans POST → 404
- [x] Isolation workspace
- [x] `F-IM-41-appel-caa-cassation-ce-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed : CONTEXTUAL, trigger_field=`recours_envisage_detecte`

## Plan de test minimal

- **UT** `AppelCaaCassationCalculatorTest` : 6+ cas (Clock fixé, délais 15j/1mois/2mois)
- **IT** `AppelCaaCassationControllerIT` : 5+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `appel_caa_cassation_analyses`
- **Migration Liquibase** + seed visibility rules
- **Extension** `ImmigrationExtractedData` : flag `recoursEnvisageDetecte` + champ `recoursDateJugementTA`
- **Endpoint** `AppelCaaCassationController`

## Hors périmètre

- Composant Angular (SF-214-34)
- Appel CNDA (F-IM-12 existant pour procédures asile)
