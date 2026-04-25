# SF-DT-14-01 — Backend PSE critères de validité (FR)

> **Feature parente** : F-DT-14 Plan de Sauvegarde de l'Emploi (V8).
> **Pays** : FRANCE uniquement (l'équivalent BE — congé-réorganisation Loi 1976 / Loi Renault — fait l'objet d'une feature jumelle au backlog).
> **Statut** : `In progress` — backend-first, frontend SF-DT-14-02 en vague suivante.
> **Branche** : `feat/SF-DT-14-01-backend-pse-validite`
> **Dépendance** : F-DT-13 backend (licenciement économique critères) déjà mergé. Le PSE est l'outil obligatoire pour les licenciements économiques ≥10 salariés en entreprise ≥50.

## Objectif (1 phrase)

Fournir un calculateur backend qui évalue la validité d'un Plan de Sauvegarde de l'Emploi (PSE) selon les 4 axes du Code du travail (art. L.1233-24-1 et s.) : déclenchement obligatoire, mode d'adoption (accord majoritaire ou document unilatéral), consultation CSE et statut DREETS.

## Comportement nominal

L'utilisateur saisit dans un dossier de droit du travail FR :
- Taille de l'entreprise en salariés (`tailleEntrepriseSalaries`)
- Nombre de licenciements économiques envisagés (`nombreLicenciementsEnvisages`)
- Période de référence en jours (`periodeJours`, typiquement 30)
- Mode d'adoption du PSE (`ACCORD_COLLECTIF_MAJORITAIRE` ou `DOCUMENT_UNILATERAL`)
- Avis du CSE (`FAVORABLE`, `DEFAVORABLE`, `NON_CONSULTE`)
- Statut DREETS (`VALIDE`, `HOMOLOGUE`, `REFUS`, `EN_COURS`)
- Date de notification DREETS (optionnelle)
- Liste des mesures du PSE (`contenuMesures` enum multi-select)
- Date du projet (`dateProjet`)

Le service calcule :
- `pseRequis` (boolean) selon art. L.1233-24-1
- `scoreConformite` (0-100) score global de conformité
- `verdictValidite` (`VALIDE`, `CONTESTABLE`, `NUL`)
- `criteresRemplis` / `criteresManquants` (listes)
- `delaiContestationJours` (60 jours, art. L.1235-7-1)
- Messages contextuels

L'analyse est persistée 1:1 par dossier (upsert) et exposée via GET.

## Règles de calcul

### 1. Déclenchement (`pseRequis`)
- `pseRequis = true` si `nombreLicenciementsEnvisages ≥ 10` ET `periodeJours ≤ 30` ET `tailleEntrepriseSalaries ≥ 50`
- Sinon `pseRequis = false`

### 2. Si `pseRequis = false`
- Le PSE n'est pas obligatoire → `verdictValidite = VALIDE`, `scoreConformite = 100`
- Message : "PSE non requis — procédure normale de licenciement économique applicable (L.1233-24-1)"

### 3. Si `pseRequis = true` — règles de validité

**Critères structurants** :
1. **Consultation CSE** (L.1233-30) : `csaeConsulteAvis ≠ NON_CONSULTE` (sinon NUL — vice de procédure absolu)
2. **Statut DREETS** (L.1233-57-2) :
   - `REFUS` → NUL
   - `EN_COURS` → CONTESTABLE (procédure non finalisée)
   - `VALIDE` ou `HOMOLOGUE` → critère rempli
3. **Contenu minimal** (L.1233-61) :
   - Doit contenir `RECLASSEMENT_INTERNE` (sinon CONTESTABLE — contenu insuffisant)
   - Doit contenir au moins 2 mesures distinctes
4. **Cohérence mode d'adoption / DREETS** :
   - Si `modeAdoption = ACCORD_COLLECTIF_MAJORITAIRE` → la DREETS doit `VALIDE`
   - Si `modeAdoption = DOCUMENT_UNILATERAL` → la DREETS doit `HOMOLOGUE`
   - Mismatch → CONTESTABLE (procédure non conforme)

### 4. Score de conformité (0-100, si `pseRequis`)
- Base 100, déduit :
  - CSE non consulté : -50 (bloquant)
  - DREETS REFUS : -50 (bloquant)
  - DREETS EN_COURS : -25
  - Mismatch mode adoption / DREETS : -20
  - `RECLASSEMENT_INTERNE` absent : -20
  - Moins de 2 mesures distinctes : -10
- Plancher 0, plafond 100

### 5. Verdict
- Si critère bloquant (CSE non consulté OU DREETS REFUS) → `NUL`
- Sinon si score < 80 OU critères manquants → `CONTESTABLE`
- Sinon → `VALIDE`

### 6. Délai contestation
- Toujours 60 jours (art. L.1235-7-1) — le décret D.1235-21 fixe 2 mois pour la contestation devant le tribunal administratif.

### Listes dérivées
- `criteresRemplis` : liste des critères validés (`CSE_CONSULTE`, `DREETS_VALIDE_OU_HOMOLOGUE`, `CONTENU_RECLASSEMENT`, `MODE_DREETS_COHERENT`, `MESURES_MULTIPLES`)
- `criteresManquants` : liste des critères non validés

### Messages
- Confirmation chaque critère rempli
- Alerte chaque critère manquant avec article de loi

## Cas d'erreur

- Body manquant → 400
- `tailleEntrepriseSalaries` null → 400
- `nombreLicenciementsEnvisages` null → 400
- `periodeJours` null → 400
- `modeAdoption` null → 400
- `csaeConsulteAvis` null → 400
- `dreetsStatut` null → 400
- `contenuMesures` null → 400
- `dateProjet` null → 400
- Dossier non droit du travail → 400
- Dossier non FR (workspace BELGIQUE) → 400 (pays non couvert par la SF)
- Dossier inexistant ou hors workspace → 404

## Critères d'acceptation

1. POST avec ≥10 licenciements / 30 jours / entreprise ≥50 → `pseRequis = true`
2. POST avec 5 licenciements → `pseRequis = false`, `verdictValidite = VALIDE`, `scoreConformite = 100`
3. POST avec PSE requis + CSE NON_CONSULTE → `verdictValidite = NUL`
4. POST avec PSE requis + DREETS REFUS → `verdictValidite = NUL`
5. POST avec PSE requis + DREETS EN_COURS → `verdictValidite = CONTESTABLE`
6. POST avec PSE requis + tout OK + RECLASSEMENT_INTERNE + 2 mesures → `verdictValidite = VALIDE`, `scoreConformite ≥ 80`
7. POST avec PSE requis + sans RECLASSEMENT_INTERNE → `verdictValidite = CONTESTABLE`
8. POST sur dossier BE → 400 (single-country FR)
9. POST sur dossier immigration → 400
10. POST sur dossier d'un autre workspace → 404
11. GET après POST retourne les données persistées
12. GET sans POST préalable → 404
13. POST 2 fois sur le même dossier upsert l'analyse (1:1)
14. La règle de visibilité ALWAYS_ON DROIT_DU_TRAVAIL FRANCE priority 56 est ajoutée (F-IA-04)
15. `delaiContestationJours = 60` retourné systématiquement

## Plan de test

### Unit tests (≥ 22 — `PseCalculatorTest`)
1. PSE requis — 10 licenciements / 30 jours / entreprise 50 → true
2. PSE requis — 9 licenciements → false
3. PSE requis — 10 licenciements / 31 jours → false
4. PSE requis — 10 licenciements / entreprise 49 → false
5. PSE non requis → verdict VALIDE + score 100
6. PSE requis + CSE NON_CONSULTE → verdict NUL
7. PSE requis + DREETS REFUS → verdict NUL
8. PSE requis + DREETS EN_COURS → verdict CONTESTABLE
9. PSE requis + ACCORD + DREETS HOMOLOGUE (mismatch) → CONTESTABLE
10. PSE requis + DOCUMENT_UNILATERAL + DREETS VALIDE (mismatch) → CONTESTABLE
11. PSE requis + ACCORD + DREETS VALIDE → critère mode cohérent OK
12. PSE requis + DOCUMENT_UNILATERAL + DREETS HOMOLOGUE → critère mode cohérent OK
13. PSE requis sans RECLASSEMENT_INTERNE → CONTESTABLE
14. PSE requis avec 1 seule mesure → CONTESTABLE
15. PSE requis tout OK → VALIDE + score 100
16. PSE requis tout OK + 5 mesures → VALIDE
17. Score conformité — CSE non consulté → -50
18. Score conformité — DREETS EN_COURS → -25
19. Score conformité — mismatch mode → -20
20. Score conformité — sans RECLASSEMENT_INTERNE → -20
21. Score conformité — 1 seule mesure → -10
22. Délai contestation toujours 60 jours
23. criteresRemplis non vide quand tout OK
24. criteresManquants liste les critères absents
25. Validation — country BELGIQUE → IllegalArgumentException
26. Validation — modeAdoption null → IllegalArgumentException
27. Validation — dreetsStatut null → IllegalArgumentException

### Integration tests (≥ 7 — `PseAnalysisControllerIT`)
1. POST PSE requis + tout OK FR → 200 + verdict VALIDE
2. POST PSE non requis → verdict VALIDE + score 100
3. POST PSE requis + CSE non consulté → verdict NUL
4. POST PSE requis + DREETS EN_COURS → verdict CONTESTABLE
5. POST workspace BE → 400
6. POST workspace immigration → 400
7. POST autre workspace → 404
8. POST upsert → mise à jour
9. GET après POST → 200 données persistées
10. GET sans POST → 404
11. POST body invalide (modeAdoption null) → 400

### Isolation workspace
- Test cross-workspace : un user d'un workspace différent reçoit 404 sur l'endpoint d'un dossier qui ne lui appartient pas (vérifié par les patterns `WorkspaceMemberRepository.findByUserAndPrimaryTrue`).

## Tables / endpoints / composants impactés

### Tables
- **NEW** `pse_analyses` — entité 1:1 par dossier (UNIQUE constraint `case_file_id`)
  - Champs : id, case_file_id, taille_entreprise_salaries, nombre_licenciements_envisages, periode_jours, mode_adoption, csae_consulte_avis, dreets_statut, date_notification_dreets, contenu_mesures_json, date_projet, country, result_data, created_at, updated_at

### Endpoints
- **POST** `/api/v1/case-files/{caseFileId}/pse-analysis` — calcul + upsert
- **GET** `/api/v1/case-files/{caseFileId}/pse-analysis` — retour de l'analyse persistée

### Migration
- **164**-create-pse-analyses.xml
- + 1 INSERT `decision_tool_visibility_rules` ALWAYS_ON DROIT_DU_TRAVAIL FRANCE priority 56 UUID `f1a04001-0000-0000-0000-ee0000000164`

### Composants Java
- `PseCalculator` (logique pure, statique)
- `PseRequest`, `PseResponse`, `PseResult` (records)
- `PseAnalysis` (Entity)
- `PseRepository` (JpaRepository)
- `PseService` (orchestration + serialization)
- `PseController` (REST)

## Ce qui est hors périmètre

- **Frontend** : SF-DT-14-02 (vague suivante, contrat API figé ci-dessous)
- **Belgique** : congé-réorganisation Loi 1976 / Loi Renault — feature jumelle dédiée à ouvrir au backlog (régime BE différent en mode collectif et en seuils)
- **Calcul indemnités PSE** : ce calculator est un évaluateur de **validité procédurale**, pas un calculator d'indemnité (cf. F-DT-08 indemnité légale)
- **Workflow DREETS dynamique** : pas de mise à jour en temps réel du statut DREETS — saisie manuelle par l'avocat
- **Génération automatique du document PSE** : SF future (générateur niveau 2)

## Contrat API (importé par SF-DT-14-02 frontend)

### POST `/api/v1/case-files/{caseFileId}/pse-analysis`

Request body :
```json
{
  "tailleEntrepriseSalaries": 250,
  "nombreLicenciementsEnvisages": 30,
  "periodeJours": 30,
  "modeAdoption": "ACCORD_COLLECTIF_MAJORITAIRE",
  "csaeConsulteAvis": "FAVORABLE",
  "dreetsStatut": "VALIDE",
  "dateNotificationDreets": "2026-04-01",
  "contenuMesures": ["RECLASSEMENT_INTERNE", "RECLASSEMENT_EXTERNE", "FORMATION", "AIDE_CREATION", "INDEMNITES_SUPRA"],
  "dateProjet": "2026-03-15"
}
```

Enums :
- `modeAdoption` : `ACCORD_COLLECTIF_MAJORITAIRE`, `DOCUMENT_UNILATERAL`
- `csaeConsulteAvis` : `FAVORABLE`, `DEFAVORABLE`, `NON_CONSULTE`
- `dreetsStatut` : `VALIDE`, `HOMOLOGUE`, `REFUS`, `EN_COURS`
- `contenuMesures[]` : `RECLASSEMENT_INTERNE`, `RECLASSEMENT_EXTERNE`, `FORMATION`, `AIDE_CREATION`, `INDEMNITES_SUPRA`, `CONGE_RECLASSEMENT`, `CELLULE_RECLASSEMENT`, `AUTRE`

Response 200 :
```json
{
  "caseFileId": "uuid",
  "pseRequis": true,
  "scoreConformite": 100,
  "verdictValidite": "VALIDE",
  "criteresRemplis": ["CSE_CONSULTE", "DREETS_VALIDE_OU_HOMOLOGUE", "CONTENU_RECLASSEMENT", "MODE_DREETS_COHERENT", "MESURES_MULTIPLES"],
  "criteresManquants": [],
  "delaiContestationJours": 60,
  "baseJuridique": "Art. L.1233-24-1 + L.1233-30 + L.1233-57-2 + L.1233-61 + L.1235-7-1 Code du travail",
  "formule": "PSE requis (30 licenciements / 30 jours / 250 salariés) + 5 critères remplis = score 100 → verdict VALIDE",
  "messages": ["PSE requis (L.1233-24-1) ✓", "CSE consulté avec avis FAVORABLE (L.1233-30) ✓", "..."]
}
```

Codes d'erreur :
- 400 : body manquant, champ obligatoire null, dossier hors droit du travail, workspace BE
- 404 : dossier inexistant, dossier d'un autre workspace, GET sans POST préalable

## Analyse de cohérence transversale

| Cible | Statut | Justification |
|-------|--------|---------------|
| Autres outils décisionnels DT FR | Intégrée — pattern miroir LicenciementEconomique (F-DT-13) | Calculator pur statique + service upsert + controller standard |
| BE | Backlog — feature jumelle | Régime BE = congé-réorganisation Loi 1976 / Loi Renault — différent en seuils, mode d'adoption et autorités. À ajouter au backlog comme feature jumelle après merge. |
| Immigration FR/BE | Non applicable | Domaine différent |
| Famille FR/BE | Non applicable | Domaine différent |
| Frontend | SF parallèle (SF-DT-14-02) | Contrat API figé ci-dessus, frontend démarre quand backend mergé |
| F-IA-04 visibility | Intégrée | 1 règle ALWAYS_ON DROIT_DU_TRAVAIL FRANCE priority 56 |
| Référentiels métier | Non applicable | Pas de nouveau type ajouté à `legal_referentials` (les enums sont gérées en code calculator) |

## Nouveau pattern UI ou service partagé

Aucun nouveau pattern UI (frontend hors SF). Aucun service partagé nouveau côté backend — réutilise `CurrentUserResolver`, `WorkspaceMemberRepository`, `CaseFileRepository`, `ObjectMapper` existants. Le calculator pur statique suit le pattern établi par F-DT-13 / F-DT-15 / F-DT-16.

## Impact par domaine métier

- **Droit du travail** : feature **principale** — outil dédié au PSE FR
- **Droit immigration** : non applicable
- **Droit famille** : non applicable
- **Pays** : FRANCE uniquement. La Belgique a un régime différent (congé-réorganisation Loi 1976 + Loi Renault) avec des seuils, autorités et mesures distincts. Une feature jumelle BE doit être ajoutée au backlog (idéalement F-DT-14-BE ou similaire).

## Parité des domaines métier (niveau 5 — scoring)

L'outil est un **scoring de validité procédurale** (niveau 5 sur l'échelle des 7 niveaux de profondeur).

| Domaine | Équivalent existe ? | Statut |
|---------|---------------------|--------|
| Droit du travail FR | OUI (cette SF) | En cours |
| Droit du travail BE | NON (régime collectif Loi 1976 / Loi Renault — différent) | **À ouvrir au backlog comme feature jumelle** |
| Immigration | NON pertinent | Concept inapplicable |
| Famille | NON pertinent | Concept inapplicable |

Action requise : ouvrir au backlog une feature jumelle BE (PSE-équivalent / Loi Renault) après merge de cette SF.

## Préoccupations transversales

- **Auth / Principal** : non — réutilise le pattern `@AuthenticationPrincipal OidcUser` + `Principal` standard, identique au F-DT-13
- **Workspace context** : non — réutilise `WorkspaceMemberRepository.findByUserAndPrimaryTrue` standard
- **Plans / limites** : non — pas de quota appliqué à cet outil
- **Navigation / routing** : non (backend uniquement)
- **Outil décisionnel métier** : OUI — un nouveau scoring niveau 5 ajouté. Scan effectué :
  - F-DT-13 (Licenciement économique) — déjà séparé, distinct du PSE qui couvre le **collectif** ≥10
  - F-DT-08 (Indemnité légale) — distinct (calculateur d'indemnité, pas validité procédurale)
  - F-DT-15 (Inaptitude) — distinct
  - F-DT-16 (Licenciement nul) — distinct
  - PSE = situation collective ≥10 sur 30 jours en entreprise ≥50 → **invariant respecté : un outil = une situation métier**
- **Smoke tests E2E** : non concernés (backend uniquement, pas de changement d'auth/routing)
