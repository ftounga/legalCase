# Mini-spec — F-FA-25 / SF-FA-25-01 Backend Majeurs protégés (sauvegarde de justice + habilitation familiale)

## Identifiant

`F-FA-25 / SF-FA-25-01`

## Feature parente

`F-FA-25` — Majeurs protégés (sauvegarde de justice, habilitation familiale, curatelle, tutelle, mandat protection future)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-FA-25-01-majeurs-proteges-backend`

---

## Objectif

Évaluer un score d'éligibilité 0-100 et recommander le régime de protection optimal entre **sauvegarde de justice** (art. 433-441 Cciv) et **habilitation familiale** (art. 494-1 et s. depuis 2016) pour un majeur dont les facultés sont altérées, en fonction du certificat médical, des facultés altérées, du consentement, du demandeur familial et du contexte patrimonial/social.

---

## Comportement attendu

### Cas nominal

L'avocat saisit pour un dossier `DROIT_FAMILLE` FRANCE :
- `regimeProtectionDemande` : régime envisagé (SAUVEGARDE_JUSTICE, HABILITATION_FAMILIALE, CURATELLE_SIMPLE, CURATELLE_RENFORCEE, TUTELLE, MANDAT_PROTECTION_FUTURE)
- `altertationFacultesMentales` (boolean)
- `altertationFacultesPhysiques` (boolean)
- `certificatMedicalCirconstancie` (boolean) + `dateCertificatMedical` (LocalDate, optionnelle)
- `consentementPersonneAProteger` (boolean)
- `demandeurFamilial` enum (CONJOINT, ENFANT_MAJEUR, PARENT, FRERE_SOEUR, TIERS_PROCHE, MINISTERE_PUBLIC)
- `actesEnvisages` enum array (GESTION_PATRIMOINE, DECISIONS_LOGEMENT, DECISIONS_SANTE, DECISIONS_FAMILIALES, ACTES_ETAT_CIVIL, AUTRE)
- `urgencePatrimoniale` (boolean)
- `patrimoineSignificatif` (boolean)
- `isolementSocial` (boolean)

Le service calcule :
- `scoreEligibilite` 0-100
- `regimeOptimalRecommande` (un des 6 régimes enum)
- `verdictAcceptabiliteJaf` (ELEVEE / MOYENNE / FAIBLE)
- `delaiProcedureMoisPrevisionnel` (4 mois sauvegarde/habilitation, 8 mois curatelle/tutelle)
- `auditionPersonneObligatoire` (true sauf certificat d'impossibilité d'audition art. 432 Cciv)
- `expertisePsyComplementaireRecommandee` (true si altération non claire OU acte irréversible)
- `baseJuridique`, `formule`, `messages` (liste contextuelle)

L'analyse est upsertée 1:1 par dossier dans `majeurs_proteges_analyses`. Re-POST = remplacement.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Body vide / null | Message explicite | 400 |
| `regimeProtectionDemande` invalide ou absent | Message + valeurs autorisées | 400 |
| `demandeurFamilial` invalide ou absent | Message + valeurs autorisées | 400 |
| `actesEnvisages` contient une valeur non enum | Message + valeurs autorisées | 400 |
| Workspace ≠ FRANCE | "Outil disponible uniquement en FRANCE — équivalent BE en cours de scoping" | 400 |
| Dossier ≠ DROIT_FAMILLE | "Ce dossier n'est pas un dossier de droit de la famille" | 400 |
| Dossier d'un autre workspace | Case file not found | 404 |
| GET sans POST préalable | "Aucune analyse de majeur protégé trouvée pour ce dossier" | 404 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — patterns réutilisés depuis F-FA-19 (DesaccordsParentaux), F-DT-16 (LicenciementNulDetection), F-FA-14 (OrdonnanceProtection)
- [x] **Autres pays** — outil **single-country FR**. Équivalent BE = administration provisoire (art. 488 et s. Code civil BE) — **procédure distincte**, fera l'objet d'une SF jumelle backlog
- [x] **Autres domaines** — DROIT_FAMILLE seul (gate sur `cf.getLegalDomain()`)
- [x] **Autres UI patterns** — POST/GET upsert + `result_data` JSON identique aux autres outils décisionnels (pattern SF-DT-07-04)
- [x] **Autres flows transversaux** — auth OidcUser + workspace member resolver + isolation 404, identique aux autres calculators

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : champs candidats à validation IA (`certificatMedicalCirconstancie`, `consentementPersonneAProteger`, `regimeOptimalRecommande`) — sera intégré dans la SF frontend (SF-FA-25-02)
- [x] **Refresh dashboard (F-IA-02)** : oui, à appeler dans le frontend
- [x] **Pré-remplissage IA** : possible (extraire depuis synthèse IA `altertationFacultesMentales`, `consentementPersonneAProteger`) — SF frontend
- [x] **Persistance des inputs** : tous les champs saisis sont stockés dans colonnes dédiées + `result_data` JSON (entity)
- [x] **Masquage conditionnel selon type** : FRANCE + DROIT_FAMILLE — règle visibility `ALWAYS_ON` priority 82 UUID `f1a04001-0000-0000-0000-ee00000fa251` (à respecter dans le frontend via TOOL_REGISTRY F-IA-04)
- [x] **Alertes actives après calcul** : N/A backend (logique frontend)

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-FA-25 BE administration provisoire (art. 488 Code civil BE) | Oui | Backlog — SF jumelle dédiée (procédure 100% distincte juge de paix BE) |
| F-FA-25 curatelle simple/renforcée + tutelle (FR) | Oui | SF futures (SF-FA-25-03 suite) — la présente SF couvre uniquement sauvegarde + habilitation |
| F-FA-25 mandat protection future (FR) | Oui | SF future (SF-FA-25-04 ou ultérieure) |
| Frontend section + intégration TOOL_REGISTRY F-IA-04 | Oui | SF-FA-25-02 frontend (subfeature parallèle, contrat API figé ici) |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (sauvegarde + habilitation = 2 régimes les plus simples côté JAF, partagent une procédure courte 4 mois)
- [x] Subfeatures parallèles : SF-FA-25-02 frontend
- [x] Backlog (cibles non prioritaires) : curatelle/tutelle FR (SF future), mandat protection future FR (SF future), administration provisoire BE (SF jumelle)

---

## Impact par domaine métier

Cette feature **est sensible au domaine** : elle relève exclusivement du DROIT_FAMILLE (procédures civiles devant le JAF/juge des contentieux de la protection). Comportement par domaine :

- **DROIT_FAMILLE FR** : gate actif, calculator opérationnel
- **DROIT_FAMILLE BE** : équivalent = administration provisoire (art. 488 Code civil BE) **non couverte** par cette SF — backlog SF jumelle
- **DROIT_DU_TRAVAIL** : 400 "Ce dossier n'est pas un dossier de droit de la famille"
- **DROIT_IMMIGRATION** : 400 idem

---

## Parité des domaines métier

Outil de **niveau 5 (scoring)**. Parité des 3 domaines :

- **DROIT_FAMILLE** : F-FA-25 (cette feature) — concerne les majeurs vulnérables
- **DROIT_DU_TRAVAIL** : équivalent N/A — la protection des majeurs ne relève pas du droit du travail (un salarié sous tutelle reste salarié, sa protection est traitée par le JAF)
- **DROIT_IMMIGRATION** : équivalent N/A — la protection d'un étranger sous tutelle est traitée par le même JAF (saisine commune), pas par la procédure migratoire

→ Pas de feature jumelle à ouvrir au backlog (concept non pertinent sur les 2 autres domaines).

---

## Critères d'acceptation

- [x] `POST /api/v1/case-files/{caseFileId}/majeurs-proteges` upserte une analyse, retourne 200 avec score, verdict, régime recommandé, délai, audition obligatoire, expertise psy, base juridique, formule, messages, country
- [x] `GET /api/v1/case-files/{caseFileId}/majeurs-proteges` retourne l'analyse persistée (404 si absent)
- [x] Validation d'enum stricte (régime, demandeur, actes) → 400 explicite
- [x] Workspace BE → 400 "FR uniquement"
- [x] Dossier hors DROIT_FAMILLE → 400 "pas un dossier famille"
- [x] Dossier d'un autre workspace → 404 isolation
- [x] Score plafonné à 100, plancher 0
- [x] Régime optimal recommandé selon arbre : urgence + altération temporaire → SAUVEGARDE ; famille proche + consentement + altération mentale → HABILITATION ; gestion permanente → CURATELLE/TUTELLE
- [x] `auditionPersonneObligatoire = true` sauf certificat médical d'impossibilité (cf. art. 432 Cciv — non géré dans cette SF, donc toujours true ici)
- [x] `expertisePsyComplementaireRecommandee = true` si altération non claire OU acte irréversible
- [x] Délai 4 mois (sauvegarde/habilitation) ou 8 mois (curatelle/tutelle/mandat)
- [x] Migration Liquibase 153 crée la table + insère la règle visibility F-IA-04 (UUID `f1a04001-0000-0000-0000-ee00000fa251`, priority 82)
- [x] ≥ 16 tests unitaires + ≥ 8 tests d'intégration

---

## Périmètre

### Hors scope (explicite)

- Curatelle simple, curatelle renforcée, tutelle, mandat de protection future (régimes prévus dans l'enum mais le calcul du régime optimal s'arrête à sauvegarde/habilitation pour cette SF — les autres régimes restent listables/recommandables comme cibles potentielles mais sont des SF futures pour scoring fin)
- Administration provisoire BE (SF jumelle backlog)
- Frontend (SF-FA-25-02)
- Génération PDF requête JAF
- Suivi du mandataire/tuteur

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `country` | "FRANCE" | Issu du workspace |
| `result_data` | JSON sérialisé du `MajeursProtegesResult` | À chaque POST |
| `created_at` / `updated_at` | now | `@PrePersist` / `@PreUpdate` |

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs |
|-------|-------------|------------------|
| `regimeProtectionDemande` | Oui | enum (6 valeurs) |
| `demandeurFamilial` | Oui | enum (6 valeurs) |
| `actesEnvisages` | Non (default `[]`) | enum array (6 valeurs) |
| `dateCertificatMedical` | Non | LocalDate ISO |
| booleans | Non (default false) | true/false |

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/majeurs-proteges` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/majeurs-proteges` | Oui | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `majeurs_proteges_analyses` | CREATE | Nouvelle table 1:1 par case_file |
| `decision_tool_visibility_rules` | INSERT | UUID `f1a04001-0000-0000-0000-ee00000fa251`, priority 82, ALWAYS_ON FRANCE DROIT_FAMILLE |

### Migration Liquibase

`153-create-majeurs-proteges-analyses.xml`

### Composants Angular

N/A (SF-FA-25-02)

---

## Plan de test

### Tests unitaires (≥ 16)

1. nominal sauvegarde recommandée (urgence + altération temporaire)
2. nominal habilitation recommandée (famille proche + consentement + mentale)
3. nominal curatelle/tutelle recommandée (gestion permanente sans consentement)
4. score plafonné à 100
5. score plancher 0
6. verdict ELEVEE ≥ 75
7. verdict MOYENNE 50-74
8. verdict FAIBLE < 50
9. délai 4 mois (sauvegarde)
10. délai 4 mois (habilitation)
11. délai 8 mois (tutelle)
12. expertise psy recommandée si altération non claire
13. expertise psy recommandée si acte irréversible
14. expertise psy non recommandée si altération claire ET pas d'acte irréversible
15. audition obligatoire toujours true (art. 432)
16. validation : régime invalide → throws
17. validation : demandeur invalide → throws
18. validation : actes invalides → throws
19. base juridique contient 433-441 + 494-1
20. messages contiennent références juridiques

### Tests d'intégration (≥ 8)

1. POST FR nominal habilitation → 200, verdict, score, régime
2. POST FR sauvegarde urgence → 200
3. POST workspace BE → 400
4. POST DROIT_DU_TRAVAIL → 400
5. POST autre workspace → 404
6. POST régime invalide → 400
7. POST upsert remplace l'analyse
8. GET après POST → 200 avec données persistées
9. GET sans POST → 404

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

`POST + GET /api/v1/case-files/{caseFileId}/majeurs-proteges`

**Request body** (POST seulement) :

```json
{
  "regimeProtectionDemande": "HABILITATION_FAMILIALE",
  "altertationFacultesMentales": true,
  "altertationFacultesPhysiques": false,
  "certificatMedicalCirconstancie": true,
  "dateCertificatMedical": "2026-04-15",
  "consentementPersonneAProteger": true,
  "demandeurFamilial": "ENFANT_MAJEUR",
  "actesEnvisages": ["GESTION_PATRIMOINE"],
  "urgencePatrimoniale": false,
  "patrimoineSignificatif": true,
  "isolementSocial": false
}
```

Enum :
- `regimeProtectionDemande` : `SAUVEGARDE_JUSTICE`, `HABILITATION_FAMILIALE`, `CURATELLE_SIMPLE`, `CURATELLE_RENFORCEE`, `TUTELLE`, `MANDAT_PROTECTION_FUTURE`
- `demandeurFamilial` : `CONJOINT`, `ENFANT_MAJEUR`, `PARENT`, `FRERE_SOEUR`, `TIERS_PROCHE`, `MINISTERE_PUBLIC`
- `actesEnvisages` : `GESTION_PATRIMOINE`, `DECISIONS_LOGEMENT`, `DECISIONS_SANTE`, `DECISIONS_FAMILIALES`, `ACTES_ETAT_CIVIL`, `AUTRE`

**Response** :

```json
{
  "caseFileId": "uuid",
  "regimeProtectionDemande": "HABILITATION_FAMILIALE",
  "altertationFacultesMentales": true,
  "altertationFacultesPhysiques": false,
  "certificatMedicalCirconstancie": true,
  "dateCertificatMedical": "2026-04-15",
  "consentementPersonneAProteger": true,
  "demandeurFamilial": "ENFANT_MAJEUR",
  "actesEnvisages": ["GESTION_PATRIMOINE"],
  "urgencePatrimoniale": false,
  "patrimoineSignificatif": true,
  "isolementSocial": false,
  "scoreEligibilite": 85,
  "regimeOptimalRecommande": "HABILITATION_FAMILIALE",
  "verdictAcceptabiliteJaf": "ELEVEE",
  "delaiProcedureMoisPrevisionnel": 4,
  "auditionPersonneObligatoire": true,
  "expertisePsyComplementaireRecommandee": false,
  "baseJuridique": "Art. 433-441 + 494-1 et s. Cciv",
  "formule": "Score 85 = acceptabilité ELEVEE...",
  "messages": ["..."],
  "country": "FRANCE"
}
```

`verdictAcceptabiliteJaf` enum : `ELEVEE` (≥ 75), `MOYENNE` (50-74), `FAIBLE` (< 50).

---

## Logique de scoring (référence)

Score 0-100 (clamp) :
- `certificatMedicalCirconstancie` : +30 (obligatoire art. 431)
- `altertationFacultesMentales` : +25
- `consentementPersonneAProteger` ET `regimeProtectionDemande == HABILITATION_FAMILIALE` : +20 (consentement requis pour habilitation)
- `demandeurFamilial ∈ {CONJOINT, ENFANT_MAJEUR, PARENT}` : +15 (famille proche)
- `patrimoineSignificatif` : +10

Régime optimal :
- `urgencePatrimoniale && altertationFacultesMentales (provisoire suffit)` → `SAUVEGARDE_JUSTICE`
- `consentementPersonneAProteger && demandeurFamilial famille proche && altertationFacultesMentales && !urgencePatrimoniale` → `HABILITATION_FAMILIALE`
- `actesEnvisages.contains(GESTION_PATRIMOINE) && patrimoineSignificatif && !consentementPersonneAProteger` → `CURATELLE_RENFORCEE`
- `altertationFacultesMentales && isolementSocial && !consentementPersonneAProteger` → `TUTELLE`
- sinon → fallback sur `regimeProtectionDemande`

Verdict :
- ≥ 75 → `ELEVEE`
- 50-74 → `MOYENNE`
- < 50 → `FAIBLE`

Audition art. 432 : `true` (sauf certificat d'impossibilité — non géré dans cette SF).

Expertise psy : `expertisePsyComplementaireRecommandee = true` si :
- `(altertationFacultesMentales && altertationFacultesPhysiques)` (altération non claire mixte) OU
- `actesEnvisages.contains(DECISIONS_SANTE) || actesEnvisages.contains(ACTES_ETAT_CIVIL)` (acte irréversible) OU
- `!certificatMedicalCirconstancie` (besoin objectif)

Délai :
- `SAUVEGARDE_JUSTICE`, `HABILITATION_FAMILIALE` → 4 mois
- `CURATELLE_*`, `TUTELLE`, `MANDAT_PROTECTION_FUTURE` → 8 mois

Base juridique : `Art. 433-441 + 494-1 et s. Cciv`.

---

## Notes et décisions

- L'enum `regimeProtectionDemande` accueille **6 valeurs** dès maintenant pour figer le contrat (le frontend n'aura pas à migrer après ajout de SF futures), même si le scoring détaillé pour curatelle/tutelle/mandat sera affiné en SF ultérieures.
- UUID visibility rule : `f1a04001-0000-0000-0000-ee00000fa251` (hex valide), priority 82 (au-dessus de 79 utilisé par F-FA-19-05 désaccords parentaux ; cohérent avec la séquence des features F-FA récentes).
- Migration ID : `153-create-majeurs-proteges-analyses` (le numéro 153 saute les 149-152 qui pourront être attribués à d'autres SF en cours sur d'autres branches).
