# Mini-spec — F-FA-27 / SF-FA-27-01 Backend PMA / GPA / bioéthique

## Identifiant

`F-FA-27 / SF-FA-27-01`

## Feature parente

`F-FA-27` — PMA / GPA / bioéthique : reconnaissance anticipée PMA couples (loi bioéthique 2/8/2021,
art. 342-9 et s. Cciv), transcription état civil GPA (Cass. 4 arrêts 18/12/2022, Ménesson),
accès aux origines pour enfants nés de don de gamètes (art. 16-8-1 Cciv, depuis 1/9/2022).

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-FA-27-01-backend-pma-gpa-bioethique`

---

## Objectif

Évaluer la **recevabilité** d'une demande relevant de la bioéthique familiale (loi 2021), pour
3 dispositifs distincts : PMA (reconnaissance anticipée notariée), GPA (transcription / adoption
post-jurisprudence Cass. 18/12/2022), accès aux origines (art. 16-8-1 Cciv depuis 1/9/2022).

---

## Comportement attendu

### Cas nominal

L'avocat saisit pour un dossier `DROIT_FAMILLE` FRANCE :
- `dispositif` enum (PMA_RECONNAISSANCE_ANTICIPEE / GPA_TRANSCRIPTION_ETAT_CIVIL / DON_GAMETES_ACCES_ORIGINES)

Critères communs `PMA_RECONNAISSANCE_ANTICIPEE` :
- `consentementsConjointsNotaire` (Boolean) — consentement conjoint reçu par notaire avant la PMA
- `dateReconnaissanceAnterieurePMA` (LocalDate, optionnelle) — la reconnaissance doit
  intervenir AVANT la PMA
- `conditionsAccesPMA` (Boolean) — couple respecte conditions d'accès (couples de femmes,
  femmes seules, ≥ 18 ans, pas de séparation, capacité)

Critères communs `GPA_TRANSCRIPTION_ETAT_CIVIL` :
- `paysGPALegal` (Boolean) — la GPA a été réalisée dans un pays où elle est légale
- `parentBiologiqueAvere` (Boolean) — un des parents d'intention est génétiquement parent
- `decisionEtrangereProduite` (Boolean) — décision étrangère établissant la filiation produite
- `adoptionDemande` (Boolean) — demande d'adoption simple en parallèle (pour l'autre parent)

Critères communs `DON_GAMETES_ACCES_ORIGINES` :
- `dateDon` (LocalDate, optionnelle) — date du don
- `demandeAcces` (Boolean) — l'enfant majeur (≥ 18) a formulé une demande
- `ageDemandeur` (Integer) — âge du demandeur (doit être ≥ 18)

Le service calcule :
- `verdictRecevabilite` (ELEVEE / MOYENNE / FAIBLE)
- `proceduresAFollow` (List<String> — liste ordonnée des étapes à mener)
- `risqueRefus` (FAIBLE / MOYEN / ELEVE)
- `documentsRequis` (List<String> — pièces canoniques selon dispositif)
- `delaiInstructionMois` (≈ 1 PMA / ≈ 6-12 GPA / ≈ 6 accès donneur)
- `baseJuridique`
- `formule`
- `messages`
- `country` (FRANCE)

L'analyse est upsertée 1:1 par dossier dans `pma_gpa_bioethique_analyses`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Body vide / null | Message explicite | 400 |
| `dispositif` invalide ou absent | Message + valeurs autorisées | 400 |
| Workspace ≠ FRANCE | "Outil disponible uniquement en FRANCE" | 400 |
| Dossier ≠ DROIT_FAMILLE | "Ce dossier n'est pas un dossier de droit de la famille" | 400 |
| Dossier d'un autre workspace | Case file not found | 404 |
| GET sans POST préalable | "Aucune analyse de PMA/GPA/bioéthique trouvée pour ce dossier" | 404 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — pattern aligné sur `Naturalisation*` (SF-IM-13-01, PR #639), `ChangementEtatCivil*` (SF-FA-26-01, PR #654), `ReconnaissancePaternelle*` (SF-FA-18-01, PR #652) : POST/GET upsert + result_data JSON + gate FR + DROIT_FAMILLE
- [x] **Autres pays** — outil **single-country FR**. La législation belge sur PMA/GPA est très différente (loi 6/7/2007 PMA BE, GPA non encadrée explicitement) → backlog SF jumelle
- [x] **Autres domaines** — DROIT_FAMILLE seul (gate sur `cf.getLegalDomain()`)
- [x] **Autres UI patterns** — POST/GET upsert + result_data JSON, identique aux autres outils décisionnels
- [x] **Autres flows transversaux** — auth OidcUser + workspace member resolver + isolation 404 (pattern identique aux autres calculators)

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : champs candidats (`dispositif`, `dateDon`, `ageDemandeur`) — sera intégré dans la SF frontend (SF-FA-27-02)
- [x] **Refresh dashboard (F-IA-02)** : oui, à appeler dans le frontend
- [x] **Pré-remplissage IA** : possible (dispositif, dates) — SF frontend
- [x] **Persistance des inputs** : tous les champs saisis sont stockés (colonnes dédiées + `result_data` JSON)
- [x] **Masquage conditionnel selon type** : FRANCE + DROIT_FAMILLE — règle visibility `ALWAYS_ON` priority 89 UUID `f1a04001-0000-0000-0000-ee0000000180`
- [x] **Alertes actives après calcul** : N/A backend

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-FA-27 BE PMA / GPA (loi 6/7/2007 PMA BE, etc.) | Oui | Backlog — SF jumelle dédiée |
| Frontend section + intégration TOOL_REGISTRY F-IA-04 | Oui | SF-FA-27-02 frontend (subfeature parallèle, contrat API figé ici) |

### Décision

- [x] Étendu à FR uniquement dans cette SF (couvre les 3 dispositifs FR)
- [x] Subfeatures parallèles : SF-FA-27-02 frontend
- [x] Backlog : équivalent BE (SF jumelle)

---

## Impact par domaine métier

Cette feature **est sensible au domaine** : DROIT_FAMILLE exclusivement.

- **DROIT_FAMILLE FR** : gate actif, calculator opérationnel
- **DROIT_FAMILLE BE** : législation BE distincte — **non couverte** (backlog SF jumelle)
- **DROIT_DU_TRAVAIL** : 400 "pas un dossier famille"
- **DROIT_IMMIGRATION** : 400 idem

---

## Parité des domaines métier

Outil de **niveau 5 (scoring de recevabilité)**. Parité des 3 domaines :

- **DROIT_FAMILLE** : F-FA-27 (cette feature)
- **DROIT_DU_TRAVAIL** : N/A — la bioéthique ne relève pas du droit du travail
- **DROIT_IMMIGRATION** : N/A — la filiation issue de la bioéthique impacte parfois l'immigration
  (transmission nationalité), mais cela passe par les outils naturalisation/régularisation existants

→ Pas de feature jumelle à ouvrir au backlog.

---

## Critères d'acceptation

- [x] `POST /api/v1/case-files/{caseFileId}/pma-gpa-bioethique` upserte une analyse, retourne 200 avec verdict, procédures, risque, documents, délai, base juridique, formule, messages, country
- [x] `GET /api/v1/case-files/{caseFileId}/pma-gpa-bioethique` retourne l'analyse persistée (404 si absent)
- [x] Validation enum stricte (dispositif) → 400 explicite
- [x] Workspace BE → 400 "FR uniquement"
- [x] Dossier hors DROIT_FAMILLE → 400 "pas un dossier famille"
- [x] Dossier d'un autre workspace → 404 isolation
- [x] PMA : ELEVEE si reconnaissance notariée ANTÉRIEURE à la PMA + conditions d'accès remplies
- [x] PMA : FAIBLE si reconnaissance non notariée ou postérieure à PMA
- [x] GPA : ELEVEE si pays légal + parent biologique + décision étrangère + adoption simple parallèle
- [x] GPA : MOYENNE si certains éléments manquent (ex : pas d'adoption en parallèle)
- [x] Accès origines : ELEVEE si demandeur ≥ 18 ans + demande formelle + don postérieur à 1/9/2022
- [x] Accès origines : FAIBLE si âge < 18
- [x] Migration Liquibase 180 crée la table + insère la règle visibility F-IA-04 (UUID `f1a04001-0000-0000-0000-ee0000000180`, priority 89)
- [x] ≥ 15 tests unitaires + ≥ 7 tests d'intégration

---

## Périmètre

### Hors scope (explicite)

- Procédures BE (loi 6/7/2007 PMA BE, GPA BE) — backlog SF jumelle
- Frontend (SF-FA-27-02)
- Génération PDF requête / acte notarié
- Suivi de l'instruction CRPMA (Centre de l'AMP)
- Déclaration d'identité du donneur (CAPADD) — c'est le résultat, pas l'évaluation

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `country` | "FRANCE" | Issu du workspace |
| `result_data` | JSON sérialisé du `PmaGpaBioethiqueResult` | À chaque POST |
| `created_at` / `updated_at` | now | `@PrePersist` / `@PreUpdate` |

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs |
|-------|-------------|------------------|
| `dispositif` | Oui | enum (3 valeurs) |
| `consentementsConjointsNotaire` | Non | Boolean |
| `dateReconnaissanceAnterieurePMA` | Non | LocalDate |
| `conditionsAccesPMA` | Non | Boolean |
| `paysGPALegal` | Non | Boolean |
| `parentBiologiqueAvere` | Non | Boolean |
| `decisionEtrangereProduite` | Non | Boolean |
| `adoptionDemande` | Non | Boolean |
| `dateDon` | Non | LocalDate |
| `demandeAcces` | Non | Boolean |
| `ageDemandeur` | Non (default null) | Integer ≥ 0 |
| `datePMA` | Non | LocalDate (optionnelle, comparée à `dateReconnaissanceAnterieurePMA`) |

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/pma-gpa-bioethique` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/pma-gpa-bioethique` | Oui | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `pma_gpa_bioethique_analyses` | CREATE | Nouvelle table 1:1 par case_file |
| `decision_tool_visibility_rules` | INSERT | UUID `f1a04001-0000-0000-0000-ee0000000180`, priority 89, ALWAYS_ON FRANCE DROIT_FAMILLE, tool_id `'F-FA-27-pma-gpa'` |

### Migration Liquibase

`180-create-pma-gpa-bioethique-analyses.xml`

### Composants Angular

N/A (SF-FA-27-02)

---

## Plan de test

### Tests unitaires (≥ 15)

PMA :
1. PMA reconnaissance notariée + date antérieure + conditions OK → ELEVEE, risqueRefus FAIBLE
2. PMA reconnaissance non notariée → FAIBLE
3. PMA reconnaissance postérieure à la PMA → FAIBLE (ne respecte pas l'antériorité)
4. PMA conditions d'accès non remplies → FAIBLE
5. PMA délai 1 mois

GPA :
6. GPA pays légal + biologique + décision + adoption demandée → ELEVEE, risqueRefus FAIBLE
7. GPA pays non légal → FAIBLE
8. GPA biologique non avéré → FAIBLE (Cass. 18/12/2022)
9. GPA pas d'adoption en parallèle → MOYENNE (parent biologique transcrit, autre via adoption)
10. GPA délai 6-12 mois

Accès origines :
11. Don + demandeur ≥ 18 + demande formelle → ELEVEE
12. Don + demandeur < 18 → FAIBLE
13. Don sans demande → FAIBLE
14. Délai accès 6 mois

Validations :
15. dispositif inconnu → throws IllegalArgumentException
16. dispositif null → throws
17. dispositif blank → throws
18. ageDemandeur négatif → throws
19. base juridique contient art. 342-9 + 16-8-1 + Cass. 2022 selon dispositif
20. formule contient dispositif et verdict

### Tests d'intégration (≥ 7)

1. POST FR PMA nominal → 200, verdict ELEVEE
2. POST FR GPA nominal → 200, verdict ELEVEE
3. POST FR don gamètes nominal → 200, verdict ELEVEE
4. POST workspace BE → 400
5. POST DROIT_DU_TRAVAIL → 400
6. POST autre workspace → 404
7. POST dispositif invalide → 400
8. POST upsert remplace l'analyse
9. GET après POST → 200 avec données persistées
10. GET sans POST → 404

### Isolation workspace

- [x] Applicable — un user de workspace A ne peut pas accéder à un dossier du workspace B (404)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — pattern standard OidcUser + Principal
- [ ] Workspace context — résolution standard via `WorkspaceMemberRepository`
- [ ] Plans / limites — N/A
- [ ] Navigation / routing frontend — N/A backend
- [x] Aucune préoccupation transversale — subfeature isolée, nouvelle table + endpoint dédié

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné (justification : pas d'impact auth/workspace/nav, endpoint isolé)

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Contrat API (figé)

`POST + GET /api/v1/case-files/{caseFileId}/pma-gpa-bioethique`

**Request body** (POST seulement) :

```json
{
  "dispositif": "PMA_RECONNAISSANCE_ANTICIPEE",
  "consentementsConjointsNotaire": true,
  "dateReconnaissanceAnterieurePMA": "2024-01-15",
  "datePMA": "2024-04-01",
  "conditionsAccesPMA": true,
  "paysGPALegal": null,
  "parentBiologiqueAvere": null,
  "decisionEtrangereProduite": null,
  "adoptionDemande": null,
  "dateDon": null,
  "demandeAcces": null,
  "ageDemandeur": null
}
```

Enum :
- `dispositif` : `PMA_RECONNAISSANCE_ANTICIPEE`, `GPA_TRANSCRIPTION_ETAT_CIVIL`, `DON_GAMETES_ACCES_ORIGINES`

**Response** :

```json
{
  "caseFileId": "uuid",
  "dispositif": "PMA_RECONNAISSANCE_ANTICIPEE",
  "verdictRecevabilite": "ELEVEE",
  "proceduresAFollow": ["..."],
  "risqueRefus": "FAIBLE",
  "documentsRequis": ["..."],
  "delaiInstructionMois": 1,
  "baseJuridique": "Art. 342-9 et s. Cciv (loi bioéthique 2021)",
  "formule": "PMA reconnaissance anticipée — verdict ELEVEE.",
  "messages": ["..."],
  "country": "FRANCE"
}
```

`verdictRecevabilite` enum : `ELEVEE`, `MOYENNE`, `FAIBLE`.
`risqueRefus` enum : `FAIBLE`, `MOYEN`, `ELEVE`.

---

## Notes et décisions

- UUID visibility rule : `f1a04001-0000-0000-0000-ee0000000180` (hex valide), priority 89 (au-dessus de 88 utilisé par F-FA-26 changement état civil).
- Migration ID : `180-create-pma-gpa-bioethique-analyses` (suit la séquence — 179 = devolution-legale).
- Outil **single-country FR** : la SF jumelle BE (loi 6/7/2007 PMA BE) sera ouverte au backlog post-merge.
- Le calculator implémente une logique distincte par dispositif (3 branches indépendantes).
