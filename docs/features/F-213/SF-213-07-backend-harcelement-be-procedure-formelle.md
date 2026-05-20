# Mini-spec — F-213 / SF-213-07-backend Outil harcèlement BE — procédure formelle + protection représailles

## Identifiant

`F-213 / SF-213-07-backend`

## Feature parente

`F-213` — P2 Travail BE — ~10 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-213-07-backend-harcelement-be-procedure-formelle`

---

## Objectif

Checklist de la **procédure interne formelle** de plainte pour harcèlement moral ou sexuel selon la **Loi du 4 août 1996** (art. 32bis–32sexies) et l'**AR du 10 avril 2014** — distinct de `F-DT-11` qui couvre la **nullité du licenciement représailles** (la suite). Outil complémentaire : `F-DT-11` intervient si un licenciement représailles survient ; `SF-213-07` intervient pour guider le salarié avant toute rupture (activation du CPAP, de la personne de confiance, de la demande formelle). **BELGIQUE UNIQUEMENT — BE-only** : la procédure interne belge (CPAP, CISP, personne de confiance) est distincte du dispositif FR (référent harcèlement, médecin du travail).

---

## Source juridique BE

- **Loi du 4 août 1996** relative au bien-être des travailleurs lors de l'exécution de leur travail, **art. 32bis–32sexies** : définition du harcèlement moral/sexuel, procédure interne, protection contre les représailles.
- **AR du 10 avril 2014** relatif à la prévention des risques psychosociaux au travail : modalités de la procédure (demande informelle, demande formelle, délais, rôle de la CISP et du CPAP).
- **Protection contre les représailles** : toute mesure défavorable dans les 12 mois suivant le dépôt de la plainte est présumée constituer des représailles (art. 32sexies).
- **Délais clés** (à vérifier par avocat BE) :
  - Demande informelle : pas de délai légal strict.
  - Demande formelle : le CPAP a **90 jours** pour conclure l'enquête (AR 10/04/2014).
  - Recours tribunal du travail : après épuisement de la procédure interne ou si l'employeur n'a pas de CPAP.
- **CISP** (Comité pour la Prévention et la Protection au Travail) — organe obligatoire dans les entreprises ≥ 50 travailleurs.

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/harcelement-be-procedure-formelle`

Inputs (body) :
- `typeHarcelement` (enum) — `MORAL` | `SEXUEL` | `LES_DEUX`.
- `etapeProcedure` (enum) — `AVANT_DEPOT` | `DEMANDE_INFORMELLE_EN_COURS` | `DEMANDE_FORMELLE_EN_COURS` | `ENQUETE_TERMINEE` | `MESURE_DEFAVORABLE_APRES_PLAINTE`.
- `dateDepotPlainte` (ISO date, optionnel) — date de dépôt de la demande formelle.
- `entreprisePossedeCPAP` (boolean) — l'entreprise a-t-elle un Conseiller en Prévention Aspects Psychosociaux ?
- `entrepriseTaille` (enum) — `MOINS_DE_50` | `CINQUANTE_ET_PLUS`.
- `mesureDefavorableApres` (boolean, défaut false) — mesure défavorable prise après le dépôt de la plainte ?
- `delaiDepuisDepotJours` (int, optionnel) — nombre de jours depuis le dépôt.

Logique (`HarcelementBeProcedureFormelleProcedureChecker`) :

**Checklist par étape :**

| `etapeProcedure` | Items checklist | Délais |
|---|---|---|
| `AVANT_DEPOT` | 1. Identifier le CPAP (interne ou SEPP) ; 2. Voie informelle ou formelle ; 3. Consigner les faits avec dates | — |
| `DEMANDE_INFORMELLE_EN_COURS` | 1. Délai de réponse CPAP (pas de délai strict) ; 2. Si échec → passer à formelle | — |
| `DEMANDE_FORMELLE_EN_COURS` | 1. Enquête CPAP — max **90 jours** ; 2. Copie plainte à conserver ; 3. Protection représailles active dès dépôt | 90 j enquête |
| `ENQUETE_TERMINEE` | 1. Mesures prises par employeur ? ; 2. Si insuffisant → tribunal du travail (CJ art. 578) | — |
| `MESURE_DEFAVORABLE_APRES_PLAINTE` | 1. Présomption représailles (art. 32sexies) ; 2. Recours via F-DT-11 (licenciement nul) ; 3. Dépôt SPIP/Inspection sociale | — |

**Protection représailles :**
- Si `mesureDefavorableApres = true` ET `dateDepotPlainte` < `today - 0 jours` (dès dépôt) : `representaillesPossibles = true`.
- `delaiProtectionMois = 12` (12 mois après le dépôt = période de protection).

Output (`HarcelementBeProcedureFormelleResponse`) :
```json
{
  "etapeProcedure": "DEMANDE_FORMELLE_EN_COURS",
  "checklistItems": [
    { "item": "Enquête CPAP — délai 90 jours", "statut": "EN_COURS", "dateEcheance": "2026-08-19" },
    { "item": "Protection contre représailles active", "statut": "ACTIF", "dateEcheance": null }
  ],
  "representaillesPossibles": false,
  "dateDebutProtectionRepresailles": "2026-05-20",
  "dateFinProtectionRepresailles": "2027-05-20",
  "prochainDelaiFatal": "2026-08-19",
  "baseJuridique": "Loi 04/08/1996 art. 32bis-32sexies ; AR 10/04/2014",
  "avertissement": null
}
```

Persistance : `harcelement_be_procedure_formelle_analyses` — unique sur `case_file_id`.

`GET` → dernière analyse ou 404.

### Cas d'erreur

| Situation | Code | Comportement |
|---|---|---|
| `workspaceCountry !== 'BELGIQUE'` | 404 | Isolation |
| `typeHarcelement` manquant | 400 | Obligatoire |
| `etapeProcedure` manquant | 400 | Obligatoire |
| `dateDepotPlainte` requis si `etapeProcedure != AVANT_DEPOT` | 400 | Message explicite |

---

## Champs IA à extraire — BELGIQUE UNIQUEMENT

| Champ | Type | Champ `TravailExtractedData` BE | Notes |
|---|---|---|---|
| `typeHarcelement` | enum | `typeHarcelementDetecte` — **BELGIQUE UNIQUEMENT** | |
| `dateDepotPlainte` | date | `dateDepotPlainteHarcelement` — **BELGIQUE UNIQUEMENT** | |
| `mesureDefavorableApres` | boolean | `mesureDefavorableDetectee` — **BELGIQUE UNIQUEMENT** | |

`critereCode` : `BE_HARCELEMENT_TYPE`, `BE_HARCELEMENT_DATE_DEPOT`, `BE_HARCELEMENT_MESURE_DEFAVORABLE`.

---

## Critères d'acceptation

- [ ] Checklist contextuelle selon étape procédurale.
- [ ] Délai 90 jours enquête calculé depuis `dateDepotPlainte`.
- [ ] `representaillesPossibles=true` si mesure défavorable détectée.
- [ ] Période de protection 12 mois calculée.
- [ ] Workspace France → 404.
- [ ] `CritereCodeIntegrityIT` vert.

---

## Périmètre

### Hors scope

- Frontend — SF-213-07b.
- **Licenciement nul représailles** — couvert par `F-DT-11` existant.
- Harcèlement par un tiers (client, fournisseur) — cas marginal, hors scope V1.
- Recours CPAM / indemnisation AT stress — autre outil (P3).

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/harcelement-be-procedure-formelle` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/harcelement-be-procedure-formelle` | OIDC | MEMBER |

### Tables

`harcelement_be_procedure_formelle_analyses` — unique `case_file_id`.

### Composants backend

- `HarcelementBeProcedureFormelle{Analysis,Repository,Request,Result,Response,Service,ProcedureChecker,Controller}.java`
- `HarcelementBeTypeEnum.java`, `HarcelementBeEtapeEnum.java`
- Extension `TravailExtractedData` + `LegalDomainPromptBuilder` BE
- Migration `XXX-create-harcelement-be-procedure-formelle-analyses.xml`

---

## Plan de test

### Unitaires

- [ ] Étape `AVANT_DEPOT` → 3 items checklist.
- [ ] Étape `DEMANDE_FORMELLE_EN_COURS` avec date → délai 90 j calculé.
- [ ] `mesureDefavorableApres=true` → `representaillesPossibles=true`.
- [ ] `dateFinProtectionRepresailles = dateDepot + 12 mois`.

### Intégration

- [ ] `POST` BE → 200, `POST` FR → 404.

---

## Dépendances

- Aucune SF bloquante.
