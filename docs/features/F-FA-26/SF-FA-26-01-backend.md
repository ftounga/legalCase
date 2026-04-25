# Mini-spec — F-FA-26 / SF-FA-26-01 Backend Changement d'état civil (nom / prénom / sexe)

## Identifiant

`F-FA-26 / SF-FA-26-01`

## Feature parente

`F-FA-26` — Changement d'état civil : nom (art. 61-3-1 Cciv loi 2022), prénom (art. 60 Cciv loi 2016 simplifiée mairie), sexe (art. 61-5 Cciv loi 2016)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-FA-26-01-changement-etat-civil-backend`

---

## Objectif

Évaluer la **compétence procédurale** (mairie / juge / tribunal judiciaire) et un **score d'acceptabilité 0-100** pour une demande de changement d'état civil (nom / prénom / sexe / nom + prénom), en fonction du motif invoqué, des preuves produites, de la majorité du demandeur, du consentement parental, de la concordance documentaire et de l'historique de changements antérieurs.

---

## Comportement attendu

### Cas nominal

L'avocat saisit pour un dossier `DROIT_FAMILLE` FRANCE :
- `typeChangement` enum (NOM, PRENOM, SEXE, NOM_ET_PRENOM)
- `motifInvoque` enum (INTERET_LEGITIME, MARIAGE, RECTIFICATION_ERREUR, IDENTIFICATION_GENRE, AUTRE)
- `preuvesProduites` enum array (JUSTIFICATIF_USAGE_30ANS, LIVRET_FAMILLE, CERTIFICAT_NAISSANCE, ACTES_CIVILS, TEMOIGNAGES, EXPERTISE_MEDICALE, AUTRE)
- `majeurDemandeur` (boolean)
- `consentementParental` (boolean — pertinent uniquement si mineur)
- `datesDocsConcordants` (boolean)
- `dejaChangeAuparavant` (boolean)
- `dateNaissanceDemandeur` (LocalDate, optionnelle)
- `departementDeclaration` (String, optionnel — code département FR sur 2 ou 3 caractères)

Le service calcule :
- `competenceProcedure` (MAIRIE / JUGE / TRIBUNAL_JUDICIAIRE)
- `delaiInstructionMoisPrevisionnel` (2 mois mairie, 3-6 mois juge selon procédure)
- `scoreAcceptabilite` 0-100
- `verdictAcceptabilite` (ELEVEE ≥ 70 / MOYENNE 40-69 / FAIBLE < 40)
- `documentsRequisManquants` liste des pièces canoniques manquantes selon `typeChangement`
- `baseJuridique`, `formule`, `messages` (liste contextuelle)
- `country` (FRANCE)

L'analyse est upsertée 1:1 par dossier dans `changement_etat_civil_analyses`. Re-POST = remplacement.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Body vide / null | Message explicite | 400 |
| `typeChangement` invalide ou absent | Message + valeurs autorisées | 400 |
| `motifInvoque` invalide ou absent | Message + valeurs autorisées | 400 |
| `preuvesProduites` contient une valeur non enum | Message + valeurs autorisées | 400 |
| Workspace ≠ FRANCE | "Outil disponible uniquement en FRANCE" | 400 |
| Dossier ≠ DROIT_FAMILLE | "Ce dossier n'est pas un dossier de droit de la famille" | 400 |
| Dossier d'un autre workspace | Case file not found | 404 |
| GET sans POST préalable | "Aucune analyse de changement d'état civil trouvée pour ce dossier" | 404 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — pattern réutilisé depuis F-FA-25 (MajeursProteges), F-FA-19 (DesaccordsParentaux), F-FA-14 (OrdonnanceProtection)
- [x] **Autres pays** — outil **single-country FR**. Équivalent BE (changement de nom : art. 370 Code judiciaire BE / changement prénom : loi 2017 BE) = procédure distincte couverte par SF jumelle backlog
- [x] **Autres domaines** — DROIT_FAMILLE seul (gate sur `cf.getLegalDomain()`)
- [x] **Autres UI patterns** — POST/GET upsert + `result_data` JSON identique aux autres outils décisionnels
- [x] **Autres flows transversaux** — auth OidcUser + workspace member resolver + isolation 404, identique aux autres calculators

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : champs candidats (`typeChangement`, `motifInvoque`, `majeurDemandeur`) — sera intégré dans la SF frontend (SF-FA-26-02)
- [x] **Refresh dashboard (F-IA-02)** : oui, à appeler dans le frontend
- [x] **Pré-remplissage IA** : possible (extraire `typeChangement`, `dateNaissanceDemandeur` depuis synthèse IA) — SF frontend
- [x] **Persistance des inputs** : tous les champs saisis sont stockés dans colonnes dédiées + `result_data` JSON
- [x] **Masquage conditionnel selon type** : FRANCE + DROIT_FAMILLE — règle visibility `ALWAYS_ON` priority 84 UUID `f1a04001-0000-0000-0000-ee00000fa261`
- [x] **Alertes actives après calcul** : N/A backend

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-FA-26 BE changement nom/prénom/sexe (art. 370 CJ BE + loi 2017) | Oui | Backlog — SF jumelle dédiée (procédures BE distinctes) |
| Frontend section + intégration TOOL_REGISTRY F-IA-04 | Oui | SF-FA-26-02 frontend (subfeature parallèle, contrat API figé ici) |

### Décision

- [x] Étendu à FR uniquement dans cette SF (couvre les 3 sous-procédures FR : nom + prénom + sexe)
- [x] Subfeatures parallèles : SF-FA-26-02 frontend
- [x] Backlog : équivalent BE (SF jumelle)

---

## Impact par domaine métier

Cette feature **est sensible au domaine** : elle relève exclusivement du DROIT_FAMILLE (procédures civiles devant la mairie / le juge).

- **DROIT_FAMILLE FR** : gate actif, calculator opérationnel
- **DROIT_FAMILLE BE** : équivalent = art. 370 CJ + loi 2017 — **non couverte** par cette SF (backlog SF jumelle)
- **DROIT_DU_TRAVAIL** : 400 "Ce dossier n'est pas un dossier de droit de la famille"
- **DROIT_IMMIGRATION** : 400 idem

---

## Parité des domaines métier

Outil de **niveau 5 (scoring)**. Parité des 3 domaines :

- **DROIT_FAMILLE** : F-FA-26 (cette feature) — concerne l'identité civile
- **DROIT_DU_TRAVAIL** : N/A — l'identité civile ne relève pas du droit du travail
- **DROIT_IMMIGRATION** : N/A — un changement d'état civil pour étranger se fait sur la base de l'acte civil étranger reconnu (procédure transcription) — pas via cet outil

→ Pas de feature jumelle à ouvrir au backlog.

---

## Critères d'acceptation

- [x] `POST /api/v1/case-files/{caseFileId}/changement-etat-civil` upserte une analyse, retourne 200 avec compétence, délai, score, verdict, documents manquants, base juridique, formule, messages, country
- [x] `GET /api/v1/case-files/{caseFileId}/changement-etat-civil` retourne l'analyse persistée (404 si absent)
- [x] Validation d'enum stricte (typeChangement, motifInvoque, preuvesProduites) → 400 explicite
- [x] Workspace BE → 400 "FR uniquement"
- [x] Dossier hors DROIT_FAMILLE → 400 "pas un dossier famille"
- [x] Dossier d'un autre workspace → 404 isolation
- [x] Score plafonné à 100, plancher 0
- [x] Compétence calculée selon arbre :
  - PRENOM → MAIRIE (art. 60 Cciv depuis loi 2016)
  - NOM + INTERET_LEGITIME + JUSTIFICATIF_USAGE_30ANS dans preuves → MAIRIE (art. 61-3-1 Cciv loi 2022)
  - NOM autres motifs → TRIBUNAL_JUDICIAIRE (procédure historique art. 61 Cciv classique)
  - SEXE → JUGE (TJ statuant en matière d'état des personnes art. 61-5 Cciv)
  - NOM_ET_PRENOM avec partie nom relevant TJ → TRIBUNAL_JUDICIAIRE
- [x] Délai indicatif : 2 mois mairie, 3 mois TJ pour sexe (art. 61-5), 6 mois TJ classique pour nom non simplifié
- [x] Documents requis selon type, manquants si non fournis
- [x] Migration Liquibase 155 crée la table + insère la règle visibility F-IA-04 (UUID `f1a04001-0000-0000-0000-ee00000fa261`, priority 84)
- [x] ≥ 14 tests unitaires + ≥ 8 tests d'intégration

---

## Périmètre

### Hors scope (explicite)

- Procédure BE (art. 370 CJ + loi 2017 BE) — backlog SF jumelle
- Frontend (SF-FA-26-02)
- Génération PDF requête / acte de notoriété
- Suivi de la publication au Journal Officiel pour changement de nom

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `country` | "FRANCE" | Issu du workspace |
| `result_data` | JSON sérialisé du `ChangementEtatCivilResult` | À chaque POST |
| `created_at` / `updated_at` | now | `@PrePersist` / `@PreUpdate` |

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs |
|-------|-------------|------------------|
| `typeChangement` | Oui | enum (4 valeurs) |
| `motifInvoque` | Oui | enum (5 valeurs) |
| `preuvesProduites` | Non (default `[]`) | enum array (7 valeurs) |
| `majeurDemandeur` | Non (default true) | boolean |
| `consentementParental` | Non (default false) | boolean |
| `datesDocsConcordants` | Non (default false) | boolean |
| `dejaChangeAuparavant` | Non (default false) | boolean |
| `dateNaissanceDemandeur` | Non | LocalDate |
| `departementDeclaration` | Non | String |

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/changement-etat-civil` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/changement-etat-civil` | Oui | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `changement_etat_civil_analyses` | CREATE | Nouvelle table 1:1 par case_file |
| `decision_tool_visibility_rules` | INSERT | UUID `f1a04001-0000-0000-0000-ee00000fa261`, priority 84, ALWAYS_ON FRANCE DROIT_FAMILLE |

### Migration Liquibase

`155-create-changement-etat-civil-analyses.xml`

### Composants Angular

N/A (SF-FA-26-02)

---

## Plan de test

### Tests unitaires (≥ 14)

1. PRENOM nominal → MAIRIE + délai 2 mois
2. NOM + INTERET_LEGITIME + JUSTIFICATIF_USAGE_30ANS → MAIRIE + délai 2 mois (art. 61-3-1 loi 2022)
3. NOM + RECTIFICATION_ERREUR → TRIBUNAL_JUDICIAIRE
4. SEXE + IDENTIFICATION_GENRE → JUGE + délai 3 mois
5. NOM_ET_PRENOM mixte → TRIBUNAL_JUDICIAIRE (la branche TJ prime)
6. score plafonné à 100
7. score plancher 0
8. verdict ELEVEE ≥ 70
9. verdict MOYENNE 40-69
10. verdict FAIBLE < 40
11. documentsRequisManquants pour PRENOM (acte naissance manquant)
12. documentsRequisManquants pour NOM (preuves usage manquantes si motif intérêt légitime)
13. mineur sans consentement parental → score baisse + message
14. dejaChangeAuparavant=true → score baisse
15. validation : typeChangement invalide → throws
16. validation : motifInvoque invalide → throws
17. validation : preuves invalides → throws
18. base juridique contient 60 + 61-3-1 + 61-5 Cciv
19. messages contiennent référence loi 2016/2022

### Tests d'intégration (≥ 8)

1. POST FR PRENOM nominal → 200, MAIRIE, délai 2 mois
2. POST FR NOM intérêt légitime + 30 ans → 200, MAIRIE
3. POST FR SEXE → 200, JUGE
4. POST workspace BE → 400
5. POST DROIT_DU_TRAVAIL → 400
6. POST autre workspace → 404
7. POST type invalide → 400
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

`POST + GET /api/v1/case-files/{caseFileId}/changement-etat-civil`

**Request body** (POST seulement) :

```json
{
  "typeChangement": "NOM",
  "motifInvoque": "INTERET_LEGITIME",
  "preuvesProduites": ["JUSTIFICATIF_USAGE_30ANS"],
  "majeurDemandeur": true,
  "consentementParental": true,
  "datesDocsConcordants": true,
  "dejaChangeAuparavant": false,
  "dateNaissanceDemandeur": "1985-04-15",
  "departementDeclaration": "75"
}
```

Enum :
- `typeChangement` : `NOM`, `PRENOM`, `SEXE`, `NOM_ET_PRENOM`
- `motifInvoque` : `INTERET_LEGITIME`, `MARIAGE`, `RECTIFICATION_ERREUR`, `IDENTIFICATION_GENRE`, `AUTRE`
- `preuvesProduites` : `JUSTIFICATIF_USAGE_30ANS`, `LIVRET_FAMILLE`, `CERTIFICAT_NAISSANCE`, `ACTES_CIVILS`, `TEMOIGNAGES`, `EXPERTISE_MEDICALE`, `AUTRE`

**Response** :

```json
{
  "caseFileId": "uuid",
  "typeChangement": "NOM",
  "motifInvoque": "INTERET_LEGITIME",
  "preuvesProduites": ["JUSTIFICATIF_USAGE_30ANS"],
  "majeurDemandeur": true,
  "consentementParental": true,
  "datesDocsConcordants": true,
  "dejaChangeAuparavant": false,
  "dateNaissanceDemandeur": "1985-04-15",
  "departementDeclaration": "75",
  "competenceProcedure": "MAIRIE",
  "delaiInstructionMoisPrevisionnel": 2,
  "scoreAcceptabilite": 85,
  "verdictAcceptabilite": "ELEVEE",
  "documentsRequisManquants": [],
  "baseJuridique": "Art. 60 (prénom) + 61-3-1 (nom) + 61-5 (sexe) Cciv",
  "formule": "Type NOM + intérêt légitime + 30 ans usage = mairie compétente, 2 mois.",
  "messages": ["..."],
  "country": "FRANCE"
}
```

`competenceProcedure` enum : `MAIRIE`, `JUGE`, `TRIBUNAL_JUDICIAIRE`.
`verdictAcceptabilite` enum : `ELEVEE` (≥ 70), `MOYENNE` (40-69), `FAIBLE` (< 40).

---

## Logique de scoring (référence)

Score 0-100 (clamp) :
- motif reconnu (INTERET_LEGITIME / MARIAGE / RECTIFICATION_ERREUR / IDENTIFICATION_GENRE) : +30
- ≥ 2 preuves variées : +25
- `majeurDemandeur` : +15 (si mineur, requiert `consentementParental` pour conserver +15)
- `!dejaChangeAuparavant` : +15
- `datesDocsConcordants` : +15

Compétence selon arbre :
1. `typeChangement == PRENOM` → `MAIRIE` (art. 60 Cciv depuis loi 2016)
2. `typeChangement == NOM` :
   - si `motifInvoque == INTERET_LEGITIME && preuvesProduites.contains(JUSTIFICATIF_USAGE_30ANS)` → `MAIRIE` (art. 61-3-1 Cciv loi 2022)
   - sinon → `TRIBUNAL_JUDICIAIRE` (procédure classique art. 61 Cciv)
3. `typeChangement == SEXE` → `JUGE` (TJ statuant en état des personnes, art. 61-5 Cciv)
4. `typeChangement == NOM_ET_PRENOM` :
   - si la partie NOM relèverait MAIRIE → `MAIRIE`
   - sinon → `TRIBUNAL_JUDICIAIRE`

Délai :
- `MAIRIE` → 2 mois
- `JUGE` (sexe) → 3 mois
- `TRIBUNAL_JUDICIAIRE` (nom classique) → 6 mois

Verdict :
- ≥ 70 → `ELEVEE`
- 40-69 → `MOYENNE`
- < 40 → `FAIBLE`

Base juridique : `Art. 60 (prénom) + 61-3-1 (nom) + 61-5 (sexe) Cciv`.

---

## Notes et décisions

- UUID visibility rule : `f1a04001-0000-0000-0000-ee00000fa261` (hex valide), priority 84 (au-dessus de 82 utilisé par F-FA-25 majeurs protégés).
- Migration ID : `155-create-changement-etat-civil-analyses` (suit la séquence — 154 réservé à une SF parallèle si besoin).
- Le scoring n'est volontairement pas asymétrique entre les 3 procédures (mairie / juge / TJ) : le score reflète l'**acceptabilité** indépendamment de la compétence — un dossier "mairie" mal monté peut très bien échouer à 30/100 ; un dossier "TJ" très bien monté peut atteindre 100.
