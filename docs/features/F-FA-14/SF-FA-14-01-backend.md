# Mini-spec — F-FA-14 / SF-FA-14-01 Ordonnance de protection FR — BACKEND

## Identifiant
`F-FA-14 / SF-FA-14-01`

## Feature parente
`F-FA-14` — Ordonnance de protection (violences conjugales, art. 515-9 Cciv) — 🔴 critique

## Statut `draft` · Date `2026-04-25` · Branche `feat/SF-FA-14-01-ordonnance-protection-backend`

---

## Objectif

Outil décisionnel FR pour **ordonnance de protection** (art. 515-9 à 515-13 Cciv + Loi 30/07/2020 BAR). ~30 000/an FR, en forte hausse. Le JAF statue dans un délai indicatif de **6 jours** (art. 515-11 Cciv). Mesures : éviction du conjoint violent, interdiction d'approcher, TGD (téléphone grave danger), BAR (bracelet anti-rapprochement), interdiction de paraître en certains lieux, attribution provisoire du logement, fixation provisoire de la résidence des enfants.

L'outil produit un **score de vraisemblance des faits** (0-100) sur la base des violences alléguées et des preuves disponibles, un **verdict de probabilité d'octroi** (ELEVEE/MOYENNE/FAIBLE), une liste de **mesures recommandées** (intersection entre demandes et contexte) et le **délai indicatif** de traitement.

**Scope FR only** : équivalent BE = art. 1253ter CJ — procédure juridiquement distincte, hors scope cette SF (couverte par SF-FA-14-04 ultérieure conforme à l'invariant "un outil = une situation métier").

---

## Comportement

### Règles (art. 515-9 à 515-13 Cciv + Loi 2020-936 du 30/07/2020)

- Demandeur : conjoint, partenaire PACS, concubin (actuel ou ancien) victime de violences vraisemblables
- Charge de la preuve : **vraisemblance** (pas certitude pénale)
- Mesures (art. 515-11) :
  - éviction du conjoint violent du domicile
  - interdiction d'approcher la victime
  - TGD (téléphone grave danger) — attribué dans les 24h (art. 41-3-1 CPP)
  - BAR (bracelet anti-rapprochement, Loi 30/07/2020) — sur danger immédiat caractérisé
  - interdiction de paraître certains lieux
  - fixation résidence enfants
  - obligation de soin
- Durée : 6 mois renouvelables (art. 515-12)
- Délai indicatif de traitement par le JAF : **6 jours** (art. 515-11 al. 1)

### Inputs

- `dateRequete` : LocalDate (jour de dépôt de la requête au JAF)
- `violencesAlleguees` : liste enum non vide :
  - `PHYSIQUES`
  - `PSYCHOLOGIQUES`
  - `SEXUELLES`
  - `ECONOMIQUES`
  - `MENACES_MORT`
- `preuvesViolences` : liste enum :
  - `CONSTAT_HUISSIER`
  - `MAIN_COURANTE`
  - `CERTIFICAT_MEDICAL`
  - `TEMOIGNAGES`
  - `PHOTOS`
  - `PLAINTE_DEPOSEE`
  - `JUGEMENT_CORRECTIONNEL`
  - `AUTRE`
- `dangerImmediat` : boolean
- `presenceEnfants` : boolean
- `ageEnfants` : list<int> nullable (ages des enfants concernés)
- `logementCommun` : boolean
- `victimeFinanciairementDependante` : boolean
- `demandeurDejaProtege` : boolean (1ère demande ou répétée)
- `demandeMesures` : liste enum :
  - `EVICTION_CONJOINT`
  - `INTERDICTION_APPROCHER`
  - `TGD`
  - `BAR`
  - `INTERDICTION_PARAITRE`
  - `OBLIGATION_SOIN`
  - `RESIDENCE_ENFANTS`

### Outputs

- `scoreVraisemblance` : 0-100 :
  - +30 si **≥ 3 catégories** de violences alléguées
  - +30 si **≥ 3 types** de preuves
  - +20 si `dangerImmediat=true`
  - +10 si `presenceEnfants=true`
  - +5 si `victimeFinanciairementDependante=true`
  - +5 si `demandeurDejaProtege=false` (1ère demande, signal positif)
- `verdictProbabiliteOctroi` :
  - `ELEVEE` si score ≥ 75
  - `MOYENNE` si 50 ≤ score < 75
  - `FAIBLE` si score < 50
- `mesuresRecommandees` : intersection entre `demandeMesures` et mesures **appropriées au contexte** :
  - `EVICTION_CONJOINT` retenu si `logementCommun=true`
  - `INTERDICTION_APPROCHER` retenu (toujours pertinent)
  - `TGD` retenu si `dangerImmediat=true`
  - `BAR` retenu si `dangerImmediat=true` (Loi 30/07/2020)
  - `INTERDICTION_PARAITRE` retenu (toujours pertinent)
  - `OBLIGATION_SOIN` retenu (toujours pertinent)
  - `RESIDENCE_ENFANTS` retenu si `presenceEnfants=true`
- `delaiTraitementJoursPrevisionnel` : `6` (constante art. 515-11)
- `baseJuridique` : "Art. 515-9 à 515-13 Cciv + Loi 30/07/2020 (BAR)"
- `formule` : récap court ("Score 88 = vraisemblance des faits élevée (≥ 75). Mesures EVICTION + INTERDICTION + TGD + BAR justifiées par danger immédiat + enfants présents.")
- `messages` : list<string> :
  - "Audience à demander en urgence — délai indicatif 6 jours (art. 515-11 Cciv)"
  - "TGD (Téléphone Grave Danger) attribué au plus tard 24h après ordonnance"
  - "BAR autorisé depuis Loi 30/07/2020 si danger immédiat caractérisé" (si BAR retenu)
  - alertes contextuelles selon inputs
- `country` : "FRANCE"

### Cas d'erreur
| Situation | HTTP |
|---|---|
| `violencesAlleguees` vide ou null | 400 |
| Code de violence inconnu | 400 |
| Code de preuve inconnu | 400 |
| Code de mesure demandée inconnu | 400 |
| `dateRequete` future de plus d'un an | 400 |
| Workspace BELGIQUE | 400 "Ordonnance de protection FR uniquement (art. 515-9 Cciv) — équivalent BE = art. 1253ter CJ" |
| Dossier `DROIT_DU_TRAVAIL` ou `DROIT_IMMIGRATION` | 400 |
| Dossier d'un autre workspace | 404 |

---

## Contrat API

### POST `/api/v1/case-files/{caseFileId}/ordonnance-protection`

**Request :**
```json
{
  "dateRequete": "2026-04-20",
  "violencesAlleguees": ["PHYSIQUES", "PSYCHOLOGIQUES", "MENACES_MORT"],
  "preuvesViolences": ["CONSTAT_HUISSIER", "MAIN_COURANTE", "CERTIFICAT_MEDICAL", "TEMOIGNAGES"],
  "dangerImmediat": true,
  "presenceEnfants": true,
  "ageEnfants": [5, 8],
  "logementCommun": true,
  "victimeFinanciairementDependante": false,
  "demandeurDejaProtege": false,
  "demandeMesures": ["EVICTION_CONJOINT", "INTERDICTION_APPROCHER", "TGD", "BAR"]
}
```

**Response :**
```json
{
  "caseFileId": "uuid",
  "dateRequete": "2026-04-20",
  "violencesAlleguees": ["PHYSIQUES", "PSYCHOLOGIQUES", "MENACES_MORT"],
  "preuvesViolences": ["CONSTAT_HUISSIER", "MAIN_COURANTE", "CERTIFICAT_MEDICAL", "TEMOIGNAGES"],
  "dangerImmediat": true,
  "presenceEnfants": true,
  "ageEnfants": [5, 8],
  "logementCommun": true,
  "victimeFinanciairementDependante": false,
  "demandeurDejaProtege": false,
  "demandeMesures": ["EVICTION_CONJOINT", "INTERDICTION_APPROCHER", "TGD", "BAR"],
  "scoreVraisemblance": 95,
  "verdictProbabiliteOctroi": "ELEVEE",
  "mesuresRecommandees": ["EVICTION_CONJOINT", "INTERDICTION_APPROCHER", "TGD", "BAR"],
  "delaiTraitementJoursPrevisionnel": 6,
  "baseJuridique": "Art. 515-9 à 515-13 Cciv + Loi 30/07/2020 (BAR)",
  "formule": "Score 95 = vraisemblance des faits élevée (≥ 75). Mesures EVICTION + INTERDICTION + TGD + BAR justifiées par danger immédiat + enfants présents.",
  "messages": [
    "Audience à demander en urgence — délai indicatif 6 jours (art. 515-11 Cciv)",
    "TGD (Téléphone Grave Danger) attribué au plus tard 24h après ordonnance",
    "BAR autorisé depuis Loi 30/07/2020 si danger immédiat caractérisé"
  ],
  "country": "FRANCE"
}
```

### GET `/api/v1/case-files/{caseFileId}/ordonnance-protection`

Retourne la dernière analyse (404 si aucune).

---

## Architecture

Pattern F-FA-09 (single-country FR DROIT_FAMILLE). Migration **137**. Table `ordonnance_protection_analyses`. Tool_id `F-FA-14-ordonnance-protection`. UUID visibility `f1a04001-0000-0000-0000-ee00000fa141`, ALWAYS_ON FR DROIT_FAMILLE, priority **75**.

### Composants
- `OrdonnanceProtectionCalculator.java`
- `OrdonnanceProtectionAnalysis.java` (entity)
- `OrdonnanceProtectionRepository.java`
- `OrdonnanceProtectionRequest/Response/Result.java`
- `OrdonnanceProtectionService.java` (gate DROIT_FAMILLE + country==FRANCE)
- `OrdonnanceProtectionController.java`
- Migration `137-create-ordonnance-protection-analyses.xml`

### Entity colonnes
- `id` uuid PK
- `case_file_id` uuid UNIQUE FK
- `date_requete` date nullable
- `violences_alleguees` text (JSON array)
- `preuves_violences` text (JSON array)
- `danger_immediat` boolean
- `presence_enfants` boolean
- `age_enfants` text nullable (JSON array)
- `logement_commun` boolean
- `victime_financierement_dependante` boolean
- `demandeur_deja_protege` boolean
- `demande_mesures` text (JSON array)
- `country` varchar(20)
- `result_data` text
- `created_at` / `updated_at` timestamps

### Tests
- `OrdonnanceProtectionCalculatorTest` (≥ 18 UT)
- `OrdonnanceProtectionControllerIT` (≥ 10 IT)

---

## Plan de test

### UT (≥ 18)
- 5 enums violences alléguées reconnus
- 8 enums preuves reconnus
- 7 enums demande mesures reconnus
- Scoring : 3 catégories violences + 3 preuves + danger + enfants + dépendance + 1ère demande → 100 ELEVEE
- Scoring : 1 violence + 1 preuve seulement → bas, FAIBLE
- Scoring borne 0-100
- Verdict ELEVEE ≥ 75, MOYENNE 50-74, FAIBLE < 50
- `mesuresRecommandees` intersection : TGD seulement si dangerImmediat
- BAR seulement si dangerImmediat
- EVICTION seulement si logementCommun
- RESIDENCE_ENFANTS seulement si presenceEnfants
- INTERDICTION_APPROCHER toujours retenu si demandé
- Délai 6 jours systématique
- Base juridique contient "515-9", "515-13", "30/07/2020"
- Formule contient verdict et score
- Messages : audience urgence, TGD 24h
- Validation : violences vides → IllegalArgumentException
- Validation : violence inconnue → IllegalArgumentException
- Validation : preuve inconnue → IllegalArgumentException
- Validation : mesure inconnue → IllegalArgumentException
- Déduplication des codes répétés

### IT (≥ 10)
- POST FR nominal (score élevé) → 200
- POST FR sans dangerImmediat (TGD + BAR exclus de mesuresRecommandees) → 200
- POST FR violences vides → 400
- POST FR enum inconnu → 400
- POST workspace BELGIQUE → 400
- POST dossier DROIT_DU_TRAVAIL → 400
- POST workspace étranger → 404
- POST upsert (2 POST consécutifs)
- GET après POST → données persistées
- GET sans POST → 404

---

## Impact par domaine métier

DROIT_FAMILLE FR uniquement. **Sensible au domaine** (FR≠BE) : équivalent BE = art. 1253ter CJ procédure juridiquement distincte (Tribunal de la famille, vocabulaire et délais belges) — couvert par SF-FA-14-04 séparée conformément à l'invariant "un outil = une situation métier". Non pertinent en DROIT_DU_TRAVAIL ni DROIT_IMMIGRATION.

## Parité des domaines métier (niveau ≥ 5)

Outil de **niveau 5 (scoring / analyse de validité)** : un score de vraisemblance de 0-100 + verdict ELEVEE/MOYENNE/FAIBLE est calculé. Parité à analyser :

- **DROIT_FAMILLE FR** : couvert par cette SF.
- **DROIT_FAMILLE BE** : équivalent existant (art. 1253ter CJ) — au backlog comme **SF-FA-14-04** (ouverte ici en référence). Procédure distincte = outil distinct.
- **DROIT_DU_TRAVAIL** : non pertinent (l'OP est un dispositif civil de protection contre violences conjugales, pas applicable au monde du travail).
- **DROIT_IMMIGRATION** : non pertinent (concept absent du droit des étrangers).

L'asymétrie FR/BE est volontaire et tracée — pas de dette métier.

## Analyse de cohérence transversale

- [x] **F-FA-08/09** : autres outils décisionnels famille FR — cible et règles juridiques distinctes, pas d'impact croisé. Le présent outil cible un cas d'urgence (515-9 Cciv) tandis que F-FA-08/09 traitent du divorce.
- [x] **F-IA-04** : architecture déclarative respectée — règle visibility ALWAYS_ON ajoutée, pas de code dans le moteur.
- [x] **F-IA-03** : la collecte d'inputs (dates, présence enfants, etc.) sera prise en charge par F-IA-03 lors du frontend SF-FA-14-02.
- [x] **F-IM-08** : pas d'overlap, gate par `legalDomain` strict (DROIT_FAMILLE).
- [x] **TOOL_REGISTRY (frontend)** : non concerné par cette SF backend ; l'entrée registry sera ajoutée par SF-FA-14-02.

## Nouveau pattern UI ou service partagé

Aucun. Pattern réutilisé strictement de F-FA-09 (DivorceFauteCalculator + entity + service + controller + migration + visibility rule).

## Hors scope

- Frontend (SF-FA-14-02 vague suivante)
- Détection IA des violences (F-IA-03 prendra en charge dans la phase frontend)
- Modélisation Tribunal de la famille BE (SF-FA-14-04 ultérieure)
- Suivi du renouvellement de l'OP (au-delà de 6 mois) — autre SF
- Articulation avec procédure pénale (plainte, ITT, etc.)

## Critères d'acceptation

- [ ] Calculator produit un score 0-100 selon les 6 facteurs pondérés
- [ ] Verdict ELEVEE/MOYENNE/FAIBLE selon seuils 75 / 50
- [ ] `mesuresRecommandees` calculées comme intersection contextuelle des demandes
- [ ] Délai 6 jours fixe (art. 515-11)
- [ ] Base juridique = "Art. 515-9 à 515-13 Cciv + Loi 30/07/2020 (BAR)"
- [ ] Migration 137 : table + 1 règle visibility ALWAYS_ON
- [ ] Gate country FRANCE strict (workspace BE → 400)
- [ ] Gate DROIT_FAMILLE strict
- [ ] Isolation workspace
- [ ] ≥ 18 UT + ≥ 10 IT verts

---

## Notes

- L'outil **n'estime pas l'octroi par le JAF** mais la **vraisemblance des faits** + cohérence des mesures demandées avec le contexte. Seul le juge décide de l'octroi.
- Le BAR (bracelet anti-rapprochement) requiert un **danger immédiat caractérisé** (Loi 30/07/2020) : volontairement implémenté comme condition stricte.
- L'enum `MENACES_MORT` est isolé de `PHYSIQUES` car les menaces de mort relèvent d'une qualification pénale distincte (art. 222-17 CP) et leur poids est volontairement traité comme une catégorie de violence à part entière.
