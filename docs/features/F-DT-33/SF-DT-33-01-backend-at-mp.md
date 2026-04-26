# Mini-spec — F-DT-33 / SF-DT-33-01 Backend AT/MP (FR)

## Identifiant

`F-DT-33 / SF-DT-33-01`

## Feature parente

`F-DT-33` — Accident du travail / Maladie professionnelle (FR)

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-DT-33-01-backend-at-mp`

---

## Objectif

Outil décisionnel backend qui évalue la recevabilité d'une procédure AT/MP (Code de la
sécurité sociale FR) sur les 3 dispositifs principaux du contentieux : reconnaissance
d'un accident du travail (L.411-1), reconnaissance d'une maladie professionnelle
(L.461-1), contestation du taux d'incapacité permanente partielle (L.434-2).

---

## Comportement attendu

### Cas nominal

L'avocat saisit le **dispositif** visé et les critères propres à chaque procédure :

1. **RECONNAISSANCE_AT** (art. L.411-1 CSS) — présomption d'imputabilité au travail si
   l'accident survient « au temps et lieu du travail ». Critères :
   - `dateAccident` (date)
   - `lieuTravail` (boolean — accident sur le lieu de travail ?)
   - `declarationEmployeurDansLes48h` (boolean — déclaration employeur dans le délai
     L.441-1)
   - `certificatMedicalInitial` (boolean — CMI L.441-6 produit)
   - Verdict :
     - `lieuTravail = true` + `certificatMedicalInitial = true` → ELEVEE
     - `lieuTravail = true` ou `certificatMedicalInitial = true` (pas les deux) → MOYENNE
     - sinon → FAIBLE
   - Délai instruction : 30 jours (enquête CPAM L.441-1) + 60 jours
     (instruction médicale et décision motivée) = 90 jours.
   - Compétence : CPAM (puis CMRA si contestation, puis Pôle Social TJ).
   - `expertiseRequise` = false par défaut, true si causalité contestée.

2. **RECONNAISSANCE_MP** (art. L.461-1 CSS) :
   - `numeroTableau` (string — numéro du tableau MP, ou `HORS_TABLEAU`)
   - `delaiPriseEnChargeRespecte` (boolean — délai du tableau respecté ?)
   - `dateExposition` (date)
   - `certificatMedicalInitial` (boolean)
   - Verdict :
     - `numeroTableau` ≠ HORS_TABLEAU + `delaiPriseEnChargeRespecte = true` +
       `certificatMedicalInitial = true` → ELEVEE
     - `numeroTableau = HORS_TABLEAU` (système complémentaire L.461-1 al.4) → MOYENNE
       avec saisine CRRMP requise
     - `delaiPriseEnChargeRespecte = false` → FAIBLE (saisine CRRMP recommandée)
     - sinon → MOYENNE
   - Délai instruction : 120 jours (enquête + instruction CPAM + saisine CRRMP au besoin).
   - Compétence : CPAM (tableau) ou CRRMP (hors tableau / délai non respecté).
   - `expertiseRequise` = true si saisine CRRMP requise.

3. **CONTESTATION_TAUX_IPP** (art. L.434-2 CSS) — contestation du taux d'IPP fixé par
   la CPAM :
   - `tauxFixeParCpam` (int 0-100)
   - `tauxRevendique` (int 0-100, > tauxFixeParCpam)
   - `expertiseMedicaleProduite` (boolean — rapport médical à l'appui)
   - `datePremierAvisCpam` (date — point de départ du délai)
   - Verdict :
     - écart ≥ 10 points + `expertiseMedicaleProduite = true` → ELEVEE
     - écart ≥ 5 points + `expertiseMedicaleProduite = true` → MOYENNE
     - écart < 5 points OU `expertiseMedicaleProduite = false` → FAIBLE
   - Délai recours :
     - Phase amiable : 60 jours (CMRA — Commission Médicale de Recours Amiable) à
       compter de `datePremierAvisCpam`.
     - Phase judiciaire : 60 jours supplémentaires (Pôle Social du TJ) après décision CMRA.
   - Compétence :
     - Initialement CMRA (recours amiable obligatoire).
     - Puis TJ Pôle Social (compétence exclusive depuis 2019, art. L.142-2 CSS).
   - `expertiseRequise` = true (expertise médicale L.141-1 systématique en TJ).

Le calculateur produit un snapshot JSON persisté en table `at_mp_analyses` (entité 1:1
par dossier).

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| `dispositif` absent ou inconnu | Erreur explicite | 400 |
| `tauxFixeParCpam` ou `tauxRevendique` hors [0,100] | Erreur explicite | 400 |
| `tauxRevendique` ≤ `tauxFixeParCpam` (CONTESTATION_TAUX_IPP) | Erreur explicite | 400 |
| `dateAccident` / `dateExposition` / `datePremierAvisCpam` future | Erreur explicite | 400 |
| Workspace pays = BELGIQUE | Refus — outil FR uniquement (équivalent BE = FEDRIS) | 400 |
| Dossier ≠ DROIT_DU_TRAVAIL | Refus | 400 |
| `caseFileId` autre workspace | 404 | 404 |
| GET sans POST préalable | 404 | 404 |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] Outils similaires DT FR : `MesuresEloignementCalculator` (F-IM-20), `NaturalisationCalculator`
  (F-IM-13), `RefereProdHomalCalculator` (F-DT-34) — pattern multi-dispositifs +
  verdict + délais réutilisé tel quel.
- [x] FR vs BE : équivalent BE = procédure FEDRIS (Agence fédérale des risques
  professionnels) — mécanisme distinct (loi 03/07/1967 sur les accidents du travail
  + loi 03/06/1970 sur les maladies professionnelles). Hors scope de cette SF —
  feature jumelle au backlog (`F-DT-33-BE` à scoper).
- [x] Domaines : strictement DROIT_DU_TRAVAIL FR. Ni Immigration ni Famille concernés.
- [x] UI patterns : pas concerné côté backend (frontend SF-DT-33-02 vague suivante).
- [x] Pré-remplissage IA : possible depuis synthèse (date d'accident, certificat médical,
  taux CPAM) — frontend SF-DT-33-02 le câblera.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern multi-dispositifs+verdict+délais (F-IM-13/20, F-DT-34) | Oui | Réutilisé tel quel |
| F-DT-33-BE | Oui | Backlog feature jumelle (procédure FEDRIS BE) |
| Refresh dashboard F-IA-02 | Oui | À câbler côté frontend SF-DT-33-02 |
| F-IA-03 cohérence | Oui | Date accident croise avec autres outils — frontend |
| F-IA-04 visibility rule | Oui | ALWAYS_ON FRANCE DROIT_DU_TRAVAIL priority 59 |

### Décision

- [x] Étendu à toutes les cibles applicables backend dans cette SF
- [x] Frontend SF-DT-33-02 vague suivante (panel + intégration F-IA-04)
- [x] BE = feature jumelle backlog

---

## Nouveau pattern UI ou service partagé

Aucun. Le composant Calculator est strictement dédié à F-DT-33. Pas de DTO réutilisable,
pas d'endpoint transversal, pas de directive partagée. Pattern existant respecté.

---

## Impact par domaine métier

- DROIT_DU_TRAVAIL : oui — la procédure AT/MP est strictement spécifique à ce domaine
  (livre IV CSS — art. L.411-1 et s. accidents du travail, L.461-1 et s. maladies
  professionnelles, L.434-2 incapacité permanente).
- DROIT_IMMIGRATION : non applicable.
- DROIT_FAMILLE : non applicable.
- FR vs BE : FR uniquement (CSS = code FR, BE = procédure FEDRIS = feature jumelle backlog).

---

## Parité des domaines métier (outil niveau 5 — scoring)

Cet outil est de **niveau 5** (scoring + verdict). La règle CLAUDE.md exige de vérifier
la parité.

- DROIT_IMMIGRATION : aucun équivalent — pas de risque professionnel en immigration.
- DROIT_FAMILLE : aucun équivalent — pas de risque professionnel en famille.

Conclusion : pas d'asymétrie créée par cette SF. Le concept AT/MP est strictement
propre au droit du travail.

---

## Critères d'acceptation

- [x] `POST /api/v1/case-files/{caseFileId}/at-mp-analysis` calcule + persiste
- [x] `GET` retourne le dernier snapshot
- [x] 3 dispositifs supportés : `RECONNAISSANCE_AT`, `RECONNAISSANCE_MP`,
  `CONTESTATION_TAUX_IPP`
- [x] Verdict cohérent : ELEVEE / MOYENNE / FAIBLE par grille décrite
- [x] Délai instruction et compétence retournés selon le dispositif
- [x] `expertiseRequise` calculé selon dispositif et critères
- [x] BELGIQUE refusée (400)
- [x] Dossier non droit du travail refusé (400)
- [x] Migration Liquibase 175 + visibility rule UUID
  `f1a04001-0000-0000-0000-ee0000000175`, priority 59, ALWAYS_ON FR DROIT_DU_TRAVAIL
- [x] tool_id `F-DT-33-at-mp`
- [x] ≥ 18 UT + ≥ 7 IT
- [x] Isolation workspace (404 si dossier d'un autre WS)

---

## Périmètre

### Hors scope

- Génération PDF de la déclaration AT, du recours CMRA, ou de l'assignation TJ Pôle Social
  (autre SF future)
- Frontend (SF-DT-33-02 vague suivante)
- Belgique / FEDRIS (feature jumelle backlog)
- Calcul des prestations en espèces (indemnités journalières, rente, capital — autre SF)

---

## Contraintes de validation

| Champ | Obligatoire | Format |
|-------|-------------|--------|
| `dispositif` | Oui | enum (3 valeurs) |
| `dateAccident` | Non (AT) | ISO date ≤ today |
| `lieuTravail` | Non (AT, default false) | bool |
| `declarationEmployeurDansLes48h` | Non (AT, default false) | bool |
| `certificatMedicalInitial` | Non (AT, MP, default false) | bool |
| `numeroTableau` | Non (MP) | string ou `HORS_TABLEAU` |
| `delaiPriseEnChargeRespecte` | Non (MP, default true) | bool |
| `dateExposition` | Non (MP) | ISO date ≤ today |
| `tauxFixeParCpam` | Non (CONTESTATION_TAUX_IPP) | int 0-100 |
| `tauxRevendique` | Non (CONTESTATION_TAUX_IPP) | int 0-100, > tauxFixe |
| `expertiseMedicaleProduite` | Non (CONTESTATION_TAUX_IPP, default false) | bool |
| `datePremierAvisCpam` | Non (CONTESTATION_TAUX_IPP) | ISO date ≤ today |

Enums :

- `dispositif` : `RECONNAISSANCE_AT`, `RECONNAISSANCE_MP`, `CONTESTATION_TAUX_IPP`
- `competence` : `CPAM`, `CRRMP`, `CMRA`, `TJ_POLE_SOCIAL`
- `verdictRecevabilite` : `ELEVEE`, `MOYENNE`, `FAIBLE`

---

## Contrat API (figé pour SF-DT-33-02 frontend)

### POST `/api/v1/case-files/{caseFileId}/at-mp-analysis`

Request (exemple RECONNAISSANCE_AT) :
```json
{
  "dispositif": "RECONNAISSANCE_AT",
  "dateAccident": "2026-03-15",
  "lieuTravail": true,
  "declarationEmployeurDansLes48h": true,
  "certificatMedicalInitial": true
}
```

Request (exemple CONTESTATION_TAUX_IPP) :
```json
{
  "dispositif": "CONTESTATION_TAUX_IPP",
  "tauxFixeParCpam": 8,
  "tauxRevendique": 25,
  "expertiseMedicaleProduite": true,
  "datePremierAvisCpam": "2026-03-01"
}
```

Response 200 :
```json
{
  "caseFileId": "uuid",
  "country": "FRANCE",
  "dispositif": "RECONNAISSANCE_AT",
  "dispositifLibelle": "Reconnaissance accident du travail (CSS L.411-1)",
  "dateAccident": "2026-03-15",
  "lieuTravail": true,
  "declarationEmployeurDansLes48h": true,
  "certificatMedicalInitial": true,
  "numeroTableau": null,
  "delaiPriseEnChargeRespecte": null,
  "dateExposition": null,
  "tauxFixeParCpam": null,
  "tauxRevendique": null,
  "expertiseMedicaleProduite": null,
  "datePremierAvisCpam": null,
  "verdictRecevabilite": "ELEVEE",
  "delaiInstructionJours": 90,
  "competence": "CPAM",
  "expertiseRequise": false,
  "documentsRequis": ["..."],
  "risqueRefus": ["..."],
  "baseJuridique": "CSS art. L.411-1 + L.441-1 + L.441-6 (présomption AT)",
  "formule": "Reconnaissance AT — verdict ELEVEE — instruction CPAM 90 jours.",
  "messages": ["..."]
}
```

### GET `/api/v1/case-files/{caseFileId}/at-mp-analysis`

Réponse identique au POST. 404 si pas de snapshot.

---

## Technique

### Endpoints

| Méthode | URL | Auth |
|---------|-----|------|
| POST | `/api/v1/case-files/{caseFileId}/at-mp-analysis` | Oui |
| GET  | `/api/v1/case-files/{caseFileId}/at-mp-analysis` | Oui |

### Tables

| Table | Opération |
|-------|-----------|
| `at_mp_analyses` | CREATE (migration 175) |
| `decision_tool_visibility_rules` | INSERT 1 ligne ALWAYS_ON FR DROIT_DU_TRAVAIL priority 59 |

### Migration Liquibase

- [x] `175-create-at-mp-analyses.xml`
- UUID visibility : `f1a04001-0000-0000-0000-ee0000000175`
- tool_id : `F-DT-33-at-mp`

---

## Plan de test

### Tests unitaires (≥ 18)

- [x] AT lieu travail + CMI → ELEVEE 90j CPAM
- [x] AT lieu travail seul → MOYENNE
- [x] AT CMI seul → MOYENNE
- [x] AT ni lieu ni CMI → FAIBLE
- [x] AT déclaration tardive → message d'avertissement
- [x] MP tableau + délai respecté + CMI → ELEVEE 120j CPAM
- [x] MP HORS_TABLEAU → MOYENNE compétence CRRMP
- [x] MP délai non respecté → FAIBLE saisine CRRMP recommandée
- [x] MP sans CMI → MOYENNE/FAIBLE selon critères
- [x] CONTESTATION_TAUX_IPP écart 17pts + expertise → ELEVEE
- [x] CONTESTATION_TAUX_IPP écart 7pts + expertise → MOYENNE
- [x] CONTESTATION_TAUX_IPP écart 3pts + expertise → FAIBLE
- [x] CONTESTATION_TAUX_IPP sans expertise → FAIBLE quel que soit l'écart
- [x] CONTESTATION_TAUX_IPP compétence = CMRA initiale puis TJ_POLE_SOCIAL
- [x] CONTESTATION_TAUX_IPP délai = 60+60 jours
- [x] dispositif null → IllegalArgumentException
- [x] dispositif inconnu → IllegalArgumentException
- [x] CONTESTATION_TAUX_IPP tauxFixe > 100 → IllegalArgumentException
- [x] CONTESTATION_TAUX_IPP tauxRevendique ≤ tauxFixe → IllegalArgumentException
- [x] AT date future → IllegalArgumentException
- [x] BaseJuridique mentionne L.411 / L.461 / L.434 selon dispositif
- [x] expertiseRequise = true pour CONTESTATION_TAUX_IPP

### Tests d'intégration (≥ 7)

- [x] POST nominal AT FR DT → 200 ELEVEE
- [x] POST nominal CONTESTATION_TAUX_IPP → 200 délai 60j compétence CMRA
- [x] POST workspace BE → 400
- [x] POST dossier immigration → 400
- [x] POST autre workspace → 404
- [x] POST dispositif invalide → 400
- [x] GET après POST → snapshot
- [x] GET sans POST → 404
- [x] POST upsert remplace l'existant

### Isolation workspace

- [x] Applicable — test 404 si workspace différent

---

## Analyse d'impact

### Préoccupations transversales

- [x] Aucune préoccupation transversale — endpoint isolé sur un dossier

### Smoke tests E2E

- [x] Aucun smoke test concerné — outil métier indépendant.

---

## Dépendances

### Subfeatures bloquantes

- Aucune

### Frontend planifié (vague suivante)

- `SF-DT-33-02` — frontend Angular (panel + section component) ; consommera le contrat ci-dessus.

---

## Notes

- La procédure AT (L.411-1) repose sur la présomption d'imputabilité — l'accident
  survenu au temps et lieu du travail est présumé d'origine professionnelle, à charge
  pour l'employeur ou la CPAM de renverser la présomption.
- Délai déclaration employeur : 48h (L.441-1) — dépassement = sanction mais pas perte
  de droit.
- Délai déclaration salarié : 24h sauf force majeure.
- Délai d'instruction CPAM : 30 jours pour AT (L.441-1) ou 120 jours pour MP — extensible
  de 60 jours sur enquête complémentaire (R.441-13 / R.461-9).
- Système complémentaire MP (L.461-1 al.4) : permet la reconnaissance hors tableau ou
  quand un critère du tableau n'est pas rempli, via saisine CRRMP — exige lien direct
  avec le travail (et IPP ≥ 25 % pour la voie complémentaire stricto sensu).
- Contestation IPP : depuis 2019 (loi 18/11/2016 + décret 2018-928), le contentieux
  technique de la sécurité sociale est unifié au sein du Pôle Social du TJ —
  compétence exclusive (L.142-2 CSS).
- CMRA = recours amiable préalable obligatoire avant TJ (R.142-1 et s.).
- Verdict ELEVEE/MOYENNE/FAIBLE est indicatif — l'avocat reste seul juge.
