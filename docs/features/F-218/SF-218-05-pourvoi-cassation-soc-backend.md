# Mini-spec — F-218 / SF-218-05 — Pourvoi en cassation chambre sociale — backend

## Identifiant

`F-218 / SF-218-05`

## Feature parente

`F-218a` — Procédure CPH avancée (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-05-pourvoi-cassation-soc-backend`

---

## Objectif

Analyser les cas d'ouverture d'un pourvoi en cassation devant la chambre sociale (violation de la loi, défaut de base légale, dénaturation, etc.), calculer le délai de 2 mois et évaluer le risque de non-admission (filtre NPC), car aucun outil n'accompagne le contentieux de cassation sociale.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/pourvoi-cassation-soc-analysis`
- Body :
  - `dateNotificationArret` (LocalDate, requis) — notification de l'arrêt de la Cour d'appel
  - `casOuverture` (List<enum>, requis, ≥ 1) — parmi `VIOLATION_LOI`, `DEFAUT_BASE_LEGALE`, `DENATURATION`, `MANQUE_DE_BASE`, `CONTRADICTION_MOTIFS`, `VICE_FORME`, `PERTE_FONDEMENT_JURIDIQUE`
  - `representationAvocatCassation` (boolean, défaut false) — avocat aux Conseils constitué (obligatoire)
  - `moyenSerieuxIdentifie` (boolean, défaut false) — appréciation du caractère sérieux du moyen (anti-filtre NPC)
- Analyzer `PourvoiCassationSocAnalyzer` :
  - **Calcul délai** : 2 mois à compter de la notification de l'arrêt (art. 612 CPC). Calcule `dateLimitePourvoi`, `joursRestants`, verdict délai `DELAI_OUVERT` / `DELAI_URGENT` (≤ 14 j) / `DELAI_EXPIRE`.
  - **Analyse cas d'ouverture** : pour chaque `casOuverture`, produit `{ libelle, baseJuridique, forceProbatoire (FORTE/MOYENNE/FAIBLE) }`. Ex. DENATURATION = force moyenne (contrôle restreint), VIOLATION_LOI = force forte.
  - **Filtre NPC (non-admission)** : si aucun cas n'a force FORTE et `moyenSerieuxIdentifie=false` → `risqueNonAdmission = ELEVE` (art. 1014 CPC, procédure de non-admission). Sinon `MODERE` / `FAIBLE`.
  - **Item bloquant** : si `representationAvocatCassation=false` → représentation par avocat aux Conseils obligatoire (art. 973 CPC).
  - **Verdict global** : `POURVOI_RECOMMANDE` / `POURVOI_RISQUE` / `POURVOI_DECONSEILLE` / `DELAI_EXPIRE`.
  - `baseJuridique` : art. 901 et s. CPC ; art. 604 CPC (cas d'ouverture) ; art. 612 CPC ; art. 973 CPC ; art. 1014 CPC.
- Output persisté dans `pourvoi_cassation_soc_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/pourvoi-cassation-soc-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| dateNotificationArret absente / future | 400 |
| casOuverture vide | 400 |
| casOuverture contient une valeur inconnue | 400 |
| caseFile inaccessible | 404 |

---

## Source juridique

- **Art. 901 et s. CPC** — pourvoi en cassation, déclaration.
- **Art. 604 CPC** — le pourvoi tend à censurer la non-conformité de l'arrêt aux règles de droit.
- **Art. 612 CPC** — délai du pourvoi : 2 mois.
- **Art. 973 CPC** — représentation obligatoire par avocat aux Conseils.
- **Art. 1014 CPC** — procédure de non-admission (filtre NPC, en vigueur depuis 2017).
- Cas d'ouverture classiques : violation de la loi, défaut de base légale, dénaturation, manque de base légale, contradiction de motifs, perte de fondement juridique.

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `dateNotificationArret` | date | `dateNotificationArretAppel` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `casOuverture` | liste enum | dérivé synthèse moyens (non pré-rempli si non factualisable) | [x] prompt (best-effort) — sinon saisie manuelle |

**Flag CONTEXTUAL pivot** : `pourvoi_cassation_soc_envisage` (niveau 3, FR-only, default false) — nouveau flag. Bascule CONTEXTUAL quand l'IA détecte un arrêt de Cour d'appel défavorable + intention de pourvoi (mention « arrêt », « Cour d'appel », « cassation », « avocat aux Conseils »).

---

## Critères d'acceptation

- [ ] POST `casOuverture=[VIOLATION_LOI]`, `moyenSerieuxIdentifie=true` → `risqueNonAdmission=FAIBLE`, verdict `POURVOI_RECOMMANDE`
- [ ] POST `casOuverture=[DENATURATION]`, `moyenSerieuxIdentifie=false` → `risqueNonAdmission=ELEVE`, verdict `POURVOI_RISQUE`
- [ ] POST notification J-50 → `dateLimitePourvoi` = notification + 2 mois, verdict délai `DELAI_URGENT`
- [ ] POST notification J-70 → `DELAI_EXPIRE`
- [ ] POST `representationAvocatCassation=false` → item bloquant avocat aux Conseils
- [ ] POST `casOuverture=[]` → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; upsert sur double POST
- [ ] Isolation workspace
- [ ] Seed `decision_tool_visibility_rules` : CONTEXTUAL, trigger_field=`pourvoi_cassation_soc_envisage`, trigger_value=`true`
- [ ] `F-DT-87-pourvoi-cassation-soc` dans `KNOWN_FRONTEND_TOOL_IDS`

## Plan de test minimal

- **UT** `PourvoiCassationSocAnalyzerTest` : ≥ 6 cas (délai ouvert/urgent/expiré, NPC élevé, moyen fort, représentation absente)
- **IT** `PourvoiCassationSocControllerIT` : ≥ 5 cas (200 nominal, 400 country, 400 casOuverture vide, 404 isolation, upsert)

## Tables / endpoints / composants impactés

- **Nouvelle table** `pourvoi_cassation_soc_analyses`
- **Migration Liquibase** + seed visibility rules
- **Endpoint** `PourvoiCassationSocController`
- **Service** `PourvoiCassationSocService` + **Analyzer** `PourvoiCassationSocAnalyzer`
- **Extension** `TravailExtractedData` : `dateNotificationArretAppel`, flag `pourvoiCassationSocEnvisage` + prompt
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-06)
- Rédaction du mémoire ampliatif (générateur futur)
- Annuaire des avocats aux Conseils
