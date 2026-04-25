# Mini-spec — F-IM-19 / SF-IM-19-01 Backend MNA / enfant né en France / documents mineurs

## Identifiant

`F-IM-19 / SF-IM-19-01`

## Feature parente

`F-IM-19` — MNA / enfant né en France / documents mineurs étrangers (FR)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-IM-19-01-backend-mineurs-immigration`

---

## Objectif

Évaluer en un point unique l'éligibilité d'un mineur étranger à l'un des 4 dispositifs de protection / régularisation : MNA (ordonnance JE art. 375 Cciv + L.221-2-2 CASF), enfant né en France (L.435-3 CESEDA), DCEM (R.321-3 CESEDA), TIR (R.321-7 CESEDA), avec verdict ELEVEE/MOYENNE/FAIBLE et liste des documents/risques.

---

## Comportement attendu

### Cas nominal

L'avocat saisit la situation du mineur (date naissance, date entrée en France, parent en situation régulière, isolement avéré, motif d'ordre public éventuel) et le dispositif visé. Le calculateur applique le switch sur `dispositifVise` :

1. **MNA_ORDONNANCE_JE** (art. 375 Cciv + L.221-2-2 CASF) : exige minorité prouvée (< 18 ans à date analyse) + isolement avéré (pas d'adulte référent). Verdict ELEVEE si critères réunis. Verdict MOYENNE si minorité contestable (proche 18 ans, examens osseux). Verdict FAIBLE si majorité ou pas d'isolement.
2. **TITRE_SEJOUR_L435_3** (CESEDA L.435-3) : exige né en France + résidence ≥ 3 ans + au moins un parent en situation régulière. Verdict ELEVEE si tous critères. MOYENNE si entrée non documentée. FAIBLE si parent non régulier ou résidence < 3 ans.
3. **DCEM** (CESEDA R.321-3) : exige présence régulière en France + pas de motif d'ordre public. Verdict ELEVEE si tous critères. FAIBLE si motif d'ordre public ou présence non régulière.
4. **TIR** (CESEDA R.321-7) : pour mineur apatride / bénéficiaire asile. Exige preuve du statut. Verdict ELEVEE si statut établi.

Pré-bloque transversal : si `dateNaissance` indique majorité (≥ 18 ans à date analyse) → verdict FAIBLE forcé pour les 4 dispositifs (les 4 sont réservés aux mineurs).

Sortie : `verdictEligibilite`, `dispositifRecommande` (si différent du visé), `criteresNonRemplis`, `documentsRequis`, `delaiInstructionMois` (2-6 selon dispositif), `baseJuridique`, `formule`, `messages`.

Persistance 1:1 par `case_file_id` (upsert).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Body absent | Message "Corps de requête requis" | 400 |
| `dispositifVise` null/vide | Message "dispositifVise est requis" | 400 |
| `dispositifVise` non supporté | Message "Dispositif non supporté" | 400 |
| `dateNaissance` null | Message "dateNaissance est requise" | 400 |
| `dateNaissance` future | Message "dateNaissance ne peut pas être dans le futur" | 400 |
| `dateEntreeFrance` antérieure à dateNaissance | Message "dateEntreeFrance doit être ≥ dateNaissance" | 400 |
| Workspace BELGIQUE | Message "Régime mineurs étrangers propre à la France" | 400 |
| Domaine ≠ DROIT_IMMIGRATION | Message "Ce dossier n'est pas un dossier de droit de l'immigration" | 400 |
| Case file d'un autre workspace | Message "Case file not found" | 404 |
| GET sans POST préalable | Message "Aucune analyse trouvée" | 404 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier immigration** : F-IM-05 Titre séjour (référentiel pièces — réutilisable), F-IM-08 OQTF (différent — éloignement), F-IM-09 AES (transition spéciale L.435-4), F-IM-10 Passeport talent, F-IM-11 Changement statut (transition titres adultes — pattern de référence), F-IM-14 Belgian-9bis/9ter (pas de mapping mineurs), F-IM-16 procedures detection.
- [x] **Pays** : Couvre uniquement la **France** (CESEDA + Cciv + CASF français). Belgique : équivalent MENA / Tutelle DGDE — pas de mapping 1:1, à verser au backlog (F-IM-19-BE).
- [x] **Domaine** : DROIT_IMMIGRATION uniquement.
- [x] **UI patterns** : N/A — backend seul. Frontend SF-IM-19-02 dédiée.
- [x] **Flows transversaux** : Pas de touche auth / workspace / plans / nav. Endpoint suit le pattern ChangementStatut (POST + GET sur `/api/v1/case-files/{id}/mineurs-immigration-analysis`).

### Niveaux de vérification

- [x] **Record / DTO backend** : Request, Response, Result.
- [x] **Service / logique métier** : MineursImmigrationService gates + délégation.
- [x] **Entité JPA + schéma DB** : `mineurs_immigration_analyses` avec UNIQUE case_file_id + result_data JSON.
- [x] **Tests existants** : pattern ChangementStatut (≥ 15 unit + ≥ 6 IT).

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : sera intégrée dans la SF frontend (les champs `dateNaissance`, `nationalite`, `dateEntreeFrance` sont sources IA).
- [x] **Refresh dashboard (F-IA-02)** : SF frontend.
- [x] **Pré-remplissage IA** : SF frontend (ImmigrationExtractedData).
- [x] **Persistance des inputs** : colonnes dédiées + result_data JSON.
- [x] **Masquage conditionnel** : visibility rule ALWAYS_ON pour DROIT_IMMIGRATION + FRANCE (priority 74).
- [x] **Alertes actives** : SF frontend.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-IM-11 Changement statut | Oui (pattern) | Réutilisé comme template (single-FR, switch sur enum, gates DROIT_IMMIGRATION + FRANCE) |
| F-IM-09 AES (L.435-4) | Voisin | Conserver distinct — L.435-4 vise majeurs entrés mineurs ; F-IM-19 vise les mineurs eux-mêmes |
| F-IM-05 référentiel pièces | Possible | Documents listés en dur dans calculator (pas d'extension référentiel pour cette SF) |
| Belgique équivalent MENA | Non immédiat | Backlog F-IM-19-BE (procédure tutelle DGDE distincte) |
| Famille (autorité parentale F-FA-19) | Non applicable | Procédure JE pour MNA distincte de l'autorité parentale ordinaire |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (FR uniquement par scope CESEDA/Cciv/CASF)
- [x] Subfeature(s) parallèle(s) créée(s) pour les cibles restantes : SF-IM-19-02 (frontend) à planifier
- [x] Backlog : F-IM-19-BE (mineurs étrangers Belgique — procédure tutelle DGDE)
- [ ] Non applicable aux autres cibles (justification explicite)

---

## Impact par domaine métier

- **Droit du travail** : Non applicable. Les 4 dispositifs sont strictement immigration mineurs.
- **Droit de l'immigration FR** : Cœur de la feature. Enum dispositif fermé (4 valeurs).
- **Droit de l'immigration BE** : Non couvert ici — backlog F-IM-19-BE.
- **Droit de la famille FR** : Lien indirect (le JE intervient dans MNA, mais le sujet d'analyse est l'éligibilité au dispositif immigration, pas la mesure d'AP). Pas de chevauchement avec F-FA-19.

Sensibilité au domaine : **OUI** — outil DROIT_IMMIGRATION exclusif (gate workspace.legalDomain).

---

## Parité des domaines métier

Niveau : **5 (scoring / analyse validité)** — verdict ELEVEE/MOYENNE/FAIBLE.

| Domaine | Outil équivalent ? | Note |
|---------|--------------------|------|
| Droit du travail | Non pertinent | Mineurs salariés couverts par d'autres règles (≠ statut administratif) |
| Famille FR | Non pertinent direct | F-FA-19 traite l'autorité parentale, pas le statut administratif d'un mineur étranger |
| Immigration BE | À ouvrir au backlog | Dispositif tutelle MENA DGDE distinct → F-IM-19-BE backlog |

---

## Critères d'acceptation

- [ ] POST `/api/v1/case-files/{id}/mineurs-immigration-analysis` calcule et persiste l'analyse avec verdict + documentsRequis + criteresNonRemplis.
- [ ] Switch sur `dispositifVise` couvre les 4 dispositifs.
- [ ] Verdict ELEVEE quand tous critères du dispositif réunis.
- [ ] Verdict FAIBLE quand `dateNaissance` indique majorité (≥ 18 ans à date analyse) — bloque transversal.
- [ ] MNA : Verdict FAIBLE si `isolementAvere = false`.
- [ ] L.435-3 : Verdict FAIBLE si `parentRegulier = false`.
- [ ] DCEM : Verdict FAIBLE si `motifOrdrePublic = true`.
- [ ] Workspace BELGIQUE → 400.
- [ ] Workspace DROIT_DU_TRAVAIL → 400.
- [ ] Case file d'un autre workspace (cross-isolation) → 404.
- [ ] GET retourne l'analyse persistée.
- [ ] Migration 172 crée la table + UNIQUE + visibility rule.
- [ ] Tests : ≥ 15 unitaires + ≥ 6 IT.

---

## Périmètre

### Hors scope

- Frontend (SF-IM-19-02 dédiée à planifier).
- Belgique (F-IM-19-BE backlog).
- Génération automatique du dossier (article 375 Cciv requête JE, formulaire CERFA L.435-3, etc.).
- Suivi de l'instruction préfectorale (CaseDeadline traité en F-IM-16).
- Calcul de la durée résidence ≥ 3 ans automatisé (l'avocat fournit `dateEntreeFrance`, on compare).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| country | "FRANCE" | Imposé par gate workspace |
| result_data | "{}" puis JSON sérialisé | rempli au compute |
| created_at / updated_at | now() | @PrePersist |

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs | Notes |
|-------|-------------|------------------|-------|
| `dispositifVise` | Oui | enum : MNA_ORDONNANCE_JE, TITRE_SEJOUR_L435_3, DCEM, TIR | |
| `dateNaissance` | Oui | LocalDate | passée |
| `dateEntreeFrance` | Non | LocalDate | requise pour L.435-3 ; ≥ dateNaissance |
| `parentRegulier` | Non | Boolean (default false) | requis pour L.435-3 |
| `isolementAvere` | Non | Boolean (default false) | requis pour MNA |
| `motifOrdrePublic` | Non | Boolean (default false) | bloquant pour DCEM |
| `nationalite` | Non | String (max 60) | informationnel |

---

## Contrat API

### POST `/api/v1/case-files/{caseFileId}/mineurs-immigration-analysis`

**Request body** :
```json
{
  "dispositifVise": "MNA_ORDONNANCE_JE",
  "dateNaissance": "2010-03-15",
  "dateEntreeFrance": "2024-09-01",
  "parentRegulier": false,
  "isolementAvere": true,
  "motifOrdrePublic": false,
  "nationalite": "Côte d'Ivoire"
}
```

**Response 200** :
```json
{
  "caseFileId": "uuid",
  "country": "FRANCE",
  "dispositifVise": "MNA_ORDONNANCE_JE",
  "dispositifRecommande": "MNA_ORDONNANCE_JE",
  "dateNaissance": "2010-03-15",
  "dateEntreeFrance": "2024-09-01",
  "parentRegulier": false,
  "isolementAvere": true,
  "motifOrdrePublic": false,
  "nationalite": "Côte d'Ivoire",
  "verdictEligibilite": "ELEVEE",
  "criteresNonRemplis": [],
  "documentsRequis": ["Acte de naissance original ou copie", "..."],
  "delaiInstructionMois": 4,
  "baseJuridique": "Cciv art. 375 + CASF L.221-2-2",
  "formule": "MNA — minorité confirmée (15 ans) + isolement avéré → saisine JE éligible.",
  "messages": ["Saisine JE par requête conjointe procureur + ASE."]
}
```

**Codes d'erreur** : 400 (body invalide / dispositif inconnu / workspace BE / domaine non immigration), 404 (case file inconnu / cross-workspace).

### GET `/api/v1/case-files/{caseFileId}/mineurs-immigration-analysis`

Retourne le dernier état persisté. 404 si jamais POSTé.

### Codes enum

- `dispositifVise` : `MNA_ORDONNANCE_JE`, `TITRE_SEJOUR_L435_3`, `DCEM`, `TIR`
- `verdictEligibilite` : `ELEVEE`, `MOYENNE`, `FAIBLE`

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/mineurs-immigration-analysis` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/mineurs-immigration-analysis` | Oui | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `mineurs_immigration_analyses` | createTable + UNIQUE case_file_id | nouvelle |
| `decision_tool_visibility_rules` | INSERT | F-IM-19-mineurs, ALWAYS_ON, DROIT_IMMIGRATION, FRANCE, priority 74, UUID f1a04001-0000-0000-0000-ee0000000172 |

### Migration Liquibase

- [x] Oui — `172-create-mineurs-immigration-analyses.xml`

---

## Plan de test

### Tests unitaires (≥ 15 — `MineursImmigrationCalculatorTest`)

1. MNA — minorité 15 ans + isolement avéré → ELEVEE
2. MNA — majeur (18 ans à date analyse) → FAIBLE
3. MNA — mineur sans isolement → FAIBLE
4. MNA — mineur 17 ans 11 mois (limite) → MOYENNE (minorité contestable)
5. L.435-3 — né en France + 3 ans + parent régulier → ELEVEE
6. L.435-3 — né en France + < 3 ans → FAIBLE
7. L.435-3 — né en France + 3 ans + parent non régulier → FAIBLE
8. DCEM — présence régulière + pas d'OP → ELEVEE
9. DCEM — motif ordre public → FAIBLE
10. TIR — apatride/asile → ELEVEE (informationnel, requiert preuve du statut)
11. Bloque transversal — majeur (≥ 18 ans) → FAIBLE même avec critères réunis
12. Dispositif non supporté → IllegalArgumentException
13. dateNaissance null → IllegalArgumentException
14. dateNaissance future → IllegalArgumentException
15. dateEntreeFrance < dateNaissance → IllegalArgumentException
16. baseJuridique contient les références correctes par dispositif
17. delaiInstructionMois cohérent (MNA=4, L.435-3=6, DCEM=2, TIR=3)

### Tests d'intégration (≥ 6 — `MineursImmigrationControllerIT`)

1. POST FR MNA mineur isolé → 200 verdict ELEVEE
2. POST FR L.435-3 conditions remplies → 200 verdict ELEVEE
3. POST FR DCEM ordre public → 200 verdict FAIBLE
4. POST workspace BELGIQUE → 400
5. POST domaine DROIT_DU_TRAVAIL → 400
6. POST cross-workspace → 404
7. POST upsert (2 fois) → remplace
8. GET après POST → 200
9. GET sans POST → 404

### Isolation workspace

- [x] Applicable — un user FR ne peut accéder à un dossier d'un autre workspace → 404.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Outil décisionnel métier** — création nouvel outil

### Liste des outils décisionnels scannés

| Outil | Pattern | Décision |
|-------|---------|----------|
| F-IM-09 AES (L.435-4) | single-FR + verdict booléen | déjà séparé, distinct (vise majeurs entrés mineurs) |
| F-IM-08 OQTF | sous-types FR + BE | déjà découpé |
| F-IM-10 Passeport talent | référentiel pièces | non utilisé |
| F-IM-11 Changement statut | single-FR + switch enum | template de référence |

Verdict : F-IM-19 = **une situation = un outil**. 4 dispositifs distincts mais tous "mineur étranger immigration" — switch interne au calculator. Conforme à l'invariant F-DT-08/F-DT-10.

### Smoke tests E2E

- [x] Aucun (pas de modif auth/workspace/nav)

---

## Dépendances

### Subfeatures bloquantes

- F-IA-04 SF-01/02 (visibility rules) : Done — pattern réutilisé
- Aucune autre.

### Questions ouvertes

- Aucune impactée.

---

## Notes et décisions

- **Date analyse** : on calcule l'âge à `LocalDate.now()` pour le bloque "majorité ≥ 18". Pour des cas archivés, la valeur sera figée à la date de POST (le service capture la date au moment du compute).
- **Délai instruction par dispositif** : MNA = 4 mois (ordonnance JE), L.435-3 = 6 mois (préfecture), DCEM = 2 mois, TIR = 3 mois. Constantes dans le calculator.
- **Champ `nationalite`** : optionnel, informationnel pour le cas TIR (apatride). Pas de validation référentiel à ce stade.
- **Visibility rule UUID** : `f1a04001-0000-0000-0000-ee0000000172` (numérotation alignée sur la migration).
- **Tool ID** : `F-IM-19-mineurs`.
- **Numérotation migration** : 172 (171 réservé à un éventuel ajustement intermédiaire — disponible aujourd'hui).

---

## Readiness checklist

| Item | Verdict | Note |
|------|---------|------|
| Mini-spec rédigée | PASS | Ce fichier |
| Critères d'acceptation listés | PASS | 13 items |
| Plan de test ≥ 15 unit / ≥ 6 IT | PASS | 17 unit / 9 IT prévus |
| Pattern de référence identifié | PASS | F-IM-11 ChangementStatut |
| Migration numérotée libre | PASS | 172 disponible |
| UUID visibility rule libre | PASS | f1a04001-...-ee0000000172 |
| Référence PRODUCT_SPEC | PASS | F-IM-19 ligne backlog V9+ |
| Gates workspace + domaine | PASS | DROIT_IMMIGRATION + FRANCE |
| Isolation workspace | PASS | Test cross-workspace prévu |

**Verdict global** : PASS.

