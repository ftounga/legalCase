# Mini-spec — F-214 / SF-214-37 — ITF judiciaire (peine prononcée par juge pénal) — backend

## Identifiant

`F-214 / SF-214-37`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Analyser l'interdiction du territoire français (ITF) prononcée par un juge pénal (peine complémentaire), ses conditions de contestation devant la cour d'appel pénale et sa distinction avec l'IRTF administrative (couverte par F-IM-20).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/itf-judiciaire-analysis`
- Body : `dateCondamnation` (LocalDate, requis), `dureeITFAnnees` (int), `infracionPrincipale` (string ≤ 200), `condamnationDefinitive` (boolean), `dateEcheanceRecoursPenal` (LocalDate, optionnel)
- Analyzer `ItfJudiciaireAnalyzer` :
  - `dateEcheanceReleve` = after 5 ans minimum (relevé grâce judiciaire) — C. pén. 131-30-1
  - `voiesRecours` : liste (appel pénal 10 j, cassation chambre criminelle 5 j, requête en relèvement C. pén. 702-1 après délai)
  - `requisReleve` : conditions de la requête en relèvement (bonne conduite, intégration, situation familiale)
  - `distinctionItfVsIrtf` : texte explicatif (ITF = juge pénal ; IRTF = préfet/admin, couverte F-IM-20)
  - `statut` ∈ {`APPEL_POSSIBLE`, `RECOURS_PRESCRIT`, `RELEVE_POSSIBLE`, `EN_COURS_PURGE`}
- Output persisté dans `itf_judiciaire_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/itf-judiciaire-analysis` → 200 ou 404

---

## Source juridique

- **C. pén. 131-30 à 131-30-2** — interdiction du territoire français (peine complémentaire).
- **C. pén. 702-1** — requête en relèvement ITF.
- **C. pr. pén. 380-1** — appel correctionnel, délai 10 j.
- **C. pr. pén. 568** — délai pourvoi cassation criminelle 5 j.
- **L. 631-3 CESEDA** — lien entre ITF et droit des étrangers.
- **Cass. crim. 14 sept. 2016, n° 16-80.161** — conditions relèvement ITF.

**Distinction ITF vs IRTF** : ITF (cet outil) = peine complémentaire prononcée par un juge pénal (C. pén.). IRTF (F-IM-20) = mesure administrative prononcée par le préfet (L. 612-6 CESEDA). Deux régimes juridiques entièrement distincts, deux outils distincts — conforme à l'invariant CLAUDE.md.

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `dateCondamnation` | date | Absent | Extension record + prompt (`itfJudiciaireDateCondamnation`) |
| `dureeITFAnnees` | int | Absent | Extension record + prompt (`itfJudiciaireDureeAnnees`) |

**Trigger CONTEXTUAL** : `mesureEloignementDetectee` (existant F-201) — l'ITF est une mesure d'éloignement même si d'origine pénale. Partage le trigger avec F-IM-20 et F-IM-43.

---

## Critères d'acceptation

- [x] POST APPEL_POSSIBLE retourne voiesRecours avec délais 10j/5j
- [x] POST RELEVE_POSSIBLE retourne requisReleve
- [x] POST workspace BE → 400
- [x] GET sans POST → 404
- [x] Isolation workspace
- [x] `F-IM-43-itf-judiciaire-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed : CONTEXTUAL, trigger_field=`mesure_eloignement_detectee` (partage avec F-IM-20)

## Plan de test minimal

- **UT** `ItfJudiciaireAnalyzerTest` : 6+ cas
- **IT** `ItfJudiciaireControllerIT` : 5+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `itf_judiciaire_analyses`
- **Migration Liquibase** + seed visibility rules
- **Extension** `ImmigrationExtractedData` : champs `itfJudiciaireDateCondamnation` + `itfJudiciaireDureeAnnees`
- **Endpoint** `ItfJudiciaireController`

## Hors périmètre

- Composant Angular (SF-214-38)
