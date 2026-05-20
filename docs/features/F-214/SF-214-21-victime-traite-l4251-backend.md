# Mini-spec — F-214 / SF-214-21 — Victime traite L. 425-1 — backend

## Identifiant

`F-214 / SF-214-21`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Analyser l'éligibilité au titre « victime de la traite des êtres humains » (L. 425-1 CESEDA), qui requiert une plainte et une collaboration avec les services d'enquête (OCRTEH), et calculer les délais de protection.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/victime-traite-analysis`
- Body : `plainteDeposee` (boolean), `collaborationOCRTEH` (boolean), `datePlainte` (LocalDate, optionnel), `titreActuel` (string, optionnel), `presenceAutoriteRefugieDetectee` (boolean, optionnel)
- Analyzer `VictimeTraiteAnalyzer` :
  - Critères L. 425-1 : plainte déposée (ou signalement ONG agrée) + collaboration avec OCRTEH + identification comme victime TEH
  - `verdict` ∈ {`ELIGIBLE_PROBABLE`, `ELIGIBLE_SOUS_RESERVE_PLAINTE`, `NON_ELIGIBLE`, `EN_COURS_IDENTIFICATION`}
  - `chipsCriteresManquants` : liste critères
  - `mesuresProtection` : liste (hébergement d'urgence, APS 6 mois L. 425-1, droit au travail attaché)
  - `risqueVictimeEnDanger` : boolean — si pas de plainte et présence autorité refuge détectée → alerte sécurité
  - `baseJuridique` : L. 425-1 + Protocole de Palerme
- Output persisté dans `victime_traite_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/victime-traite-analysis` → 200 ou 404

---

## Source juridique

- **L. 425-1 CESEDA** (ancien L. 316-1) — protection victime TEH, APS 6 mois avec droit au travail.
- **L. 225-4-1 à L. 225-4-9 Code pénal** — traite des êtres humains.
- **Protocole de Palerme (ONU, 2000)** — définition TEH.
- **OCRTEH** (office central pour la répression de la traite des êtres humains) — rôle dans identification.
- **Directive UE 2011/36** — protection victimes TEH.
- **Circ. du 19/05/2015** (Taubira) — identification victimes TEH.

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `plainteDeposee` | boolean | Absent | Extension record + prompt (`teh_plainte_deposee`) |
| `datePlainte` | date | Absent | Extension record + prompt (`teh_date_plainte`) |

**Nouveau flag CONTEXTUAL** : `victimeTraiteDetectee` (boolean) — extraction : mentions "traite des êtres humains", "TEH", "prostitution forcée", "exploitation", "OCRTEH", "victime de traite", "servitude". Ajouté dans `ImmigrationExtractedData` + prompt.

**Distinction L. 425-6 vs L. 425-1** : L. 425-6 (livré F-208) = violences conjugales (ordonnance JAF). L. 425-1 (cet outil) = traite des êtres humains. Deux situations juridiques distinctes, deux outils distincts — conforme à l'invariant CLAUDE.md.

---

## Critères d'acceptation

- [x] POST ELIGIBLE_PROBABLE (plainte + collaboration) retourne mesuresProtection, baseJuridique
- [x] POST ELIGIBLE_SOUS_RESERVE_PLAINTE retourne chipsCriteresManquants
- [x] POST risqueVictimeEnDanger = true si presenceAutoriteRefuge + pas de plainte
- [x] POST workspace BE → 400
- [x] GET sans POST → 404
- [x] Isolation workspace
- [x] `F-IM-35-victime-traite-l4251-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed : CONTEXTUAL, trigger_field=`victime_traite_detectee`

## Plan de test minimal

- **UT** `VictimeTraiteAnalyzerTest` : 6+ cas
- **IT** `VictimeTraiteControllerIT` : 5+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `victime_traite_analyses`
- **Migration Liquibase** + seed visibility rules
- **Extension** `ImmigrationExtractedData` : flag `victimeTraiteDetectee` + champs `tehPlainteDeposee`, `tehDatePlainte`
- **Endpoint** `VictimeTraiteController`

## Hors périmètre

- Composant Angular (SF-214-22)
- L. 425-2 proxénétisme/mariage forcé (P3 → F-220)
