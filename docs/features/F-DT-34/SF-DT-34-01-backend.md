# Mini-spec — F-DT-34 / SF-DT-34-01 Backend Référé prud'homal (FR)

## Identifiant

`F-DT-34 / SF-DT-34-01`

## Feature parente

`F-DT-34` — Référé prud'homal

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-DT-34-01-refere-prudhomal-backend`

---

## Objectif

Outil décisionnel backend qui évalue la probabilité de succès d'un référé prud'homal
(art. R.1454-1 et s. Code du travail) — provisions, expertises, mesures conservatoires,
réintégration en urgence — et calcule un score 0-100 + verdict de recommandation +
délais prévisionnels d'audience et d'ordonnance.

---

## Comportement attendu

### Cas nominal

Entrée : type de référé, nature de la créance, montant de provision demandée, faits
caractérisant l'urgence (absence de contestation sérieuse, preuves d'urgence,
dommage immédiat, trésorerie employeur douteuse), date de mise en demeure, ancienneté.

Le calculateur :
1. Score 0-100 :
   - `absenceContestationSerieuse` : +35 (condition R.1454-1)
   - ≥ 2 preuves d'urgence produites : +25
   - `dommageImmediatCarac` : +20
   - `tresorerieEmployeurDouteuse` : +10
   - Urgence caractérisée (calculée si dateMiseEnDemeure ≥ 7 jours, ou si
     `dommageImmediatCarac` true) : +10
2. Verdict :
   - Si type = `PROVISION_SALAIRES` et score ≥ 70 → `PROVISION_PROBABLE`
   - Si type ∈ `EXPERTISE_*` → `EXPERTISE_RECOMMANDEE` (R.1454-1 al. 2)
   - Si score < 40 → `INSUFFISAMMENT_FONDE`
   - Sinon → `AUTRE_VOIE_RECOMMANDEE`
3. Délais prévisionnels :
   - Audience : 15 jours en moyenne (R.1455-1 — bref délai)
   - Ordonnance : 8 jours après audience (pratique constante)
4. Montant provision recommandé : si `montantProvisionDemandeeEur` saisi et créance
   non contestée → recommandé tel quel. Sinon 0.
5. Persistance : snapshot JSON complet (inputs + outputs) entité 1:1 par dossier.

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| `typeRefere` absent ou inconnu | Erreur explicite | 400 |
| `natureCreance` absente ou inconnue | Erreur explicite | 400 |
| `montantProvisionDemandeeEur` < 0 | Erreur explicite | 400 |
| `preuvesUrgenceProduites` contient une valeur inconnue | Erreur explicite | 400 |
| `dateMiseEnDemeure` dans le futur | Erreur explicite | 400 |
| `ancienneteContratMois` < 0 | Erreur explicite | 400 |
| Workspace pays = BELGIQUE | Refus — outil FR uniquement | 400 |
| Dossier ≠ DROIT_DU_TRAVAIL | Refus | 400 |
| `caseFileId` autre workspace | 404 | 404 |
| GET sans POST préalable | 404 | 404 |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] Outils similaires : `ReferesAdminCalculator` (F-IM-08-07) — référé administratif
  CJA, même pattern « score 0-100 + verdict + délais ». Pattern réutilisé tel quel
  (single-country FR, table dédiée 1:1, gate domaine + pays).
- [x] Outils similaires DT : `LicenciementNulDetectionCalculator` (F-DT-16) — pattern
  score+verdict réutilisé. `TransactionCalculator` (F-DT-31) — gate similaire.
- [x] FR vs BE : la BE a son propre référé (procédure devant le tribunal du travail BE,
  art. 19 al. 3 Code judiciaire — référé absolue urgence). Hors scope de cette SF —
  feature jumelle au backlog (`F-DT-34 BE` à scoper).
- [x] Domaines : transversal n'est pas pertinent — strictement DROIT_DU_TRAVAIL FR.
- [x] UI patterns : pas concerné côté backend (frontend SF-DT-34-02 vague suivante).
- [x] Pré-remplissage IA : possible depuis synthèse (mise en demeure + créance) —
  frontend SF-DT-34-02 le câblera.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern score+verdict (F-DT-08/10/16/31, F-IM-08-07) | Oui | Réutilisé tel quel |
| F-DT-34 BE | Oui | Backlog feature jumelle (référé tribunal travail BE) |
| Refresh dashboard F-IA-02 | Oui | À câbler côté frontend SF-DT-34-02 |
| F-IA-03 cohérence | Oui | Date mise en demeure croise avec autres outils — frontend |
| F-IA-04 visibility rule | Oui | ALWAYS_ON FRANCE DROIT_DU_TRAVAIL priority 63 |

### Décision

- [x] Étendu à toutes les cibles applicables backend dans cette SF
- [x] Frontend SF-DT-34-02 vague suivante (panel + intégration F-IA-04)
- [x] BE = feature jumelle backlog

---

## Nouveau pattern UI ou service partagé

Aucun. Le composant Calculator est strictement dédié à F-DT-34. Pas de DTO réutilisable,
pas d'endpoint transversal, pas de directive partagée. Pattern existant respecté.

---

## Impact par domaine métier

- DROIT_DU_TRAVAIL : oui — la procédure de référé prud'homal est strictement spécifique
  à ce domaine (formation de référé du conseil de prud'hommes — art. L.1423-1 et
  R.1454-1).
- DROIT_IMMIGRATION : non applicable (référés CJA = F-IM-08-07 différents).
- DROIT_FAMILLE : non applicable (référé devant JAF = procédure différente, F-FA-08
  partiellement couvert).
- FR vs BE : FR uniquement (procédure CT FR, BE = feature jumelle backlog).

---

## Parité des domaines métier (outil niveau 5 — scoring)

Cet outil est de **niveau 5** (scoring + verdict). La règle CLAUDE.md exige de vérifier
la parité.

- DROIT_IMMIGRATION : F-IM-08-07 (Référés admin CJA) couvre déjà les référés en
  immigration FR — pas de gap. BE non applicable (CJA = FR uniquement).
- DROIT_FAMILLE : pas de référé spécifique au JAF couvert. À investiguer dans une
  feature jumelle backlog si pertinent (ex. ordonnance de protection F-FA-14 a déjà
  son scoring — proche d'un référé).

Conclusion : pas d'asymétrie créée par cette SF. Le concept « scoring de référé »
existe déjà côté immigration. Couverture famille à arbitrer hors scope.

---

## Critères d'acceptation

- [x] `POST /api/v1/case-files/{caseFileId}/refere-prudhomal` calcule + persiste
- [x] `GET` retourne le dernier snapshot
- [x] Score 0-100 selon la grille (35/25/20/10/10)
- [x] Verdict cohérent : PROVISION_PROBABLE ≥ 70 si type=PROVISION_SALAIRES,
  EXPERTISE_RECOMMANDEE si type=EXPERTISE_*, INSUFFISAMMENT_FONDE < 40, AUTRE sinon
- [x] Délais prévisionnels : audience 15j, ordonnance 8j (R.1455-1)
- [x] BELGIQUE refusée
- [x] Dossier non droit du travail refusé
- [x] Migration Liquibase 157 + visibility rule UUID
  `f1a04001-0000-0000-0000-ee0000000341`, priority 63, ALWAYS_ON FR DROIT_DU_TRAVAIL
- [x] ≥ 14 UT + ≥ 8 IT
- [x] Isolation workspace (404 si dossier d'un autre WS)

---

## Périmètre

### Hors scope

- Génération PDF de l'assignation en référé (autre SF future)
- Frontend (SF-DT-34-02 vague suivante)
- Belgique (feature jumelle backlog)
- Recouvrement / mise à exécution de l'ordonnance

---

## Contraintes de validation

| Champ | Obligatoire | Format |
|-------|-------------|--------|
| `typeRefere` | Oui | enum |
| `natureCreance` | Oui | enum |
| `montantProvisionDemandeeEur` | Non | ≥ 0 |
| `absenceContestationSerieuse` | Non (default false) | bool |
| `preuvesUrgenceProduites` | Non | List<enum> |
| `dommageImmediatCarac` | Non (default false) | bool |
| `tresorerieEmployeurDouteuse` | Non (default false) | bool |
| `dateMiseEnDemeure` | Non | ISO date ≤ today |
| `ancienneteContratMois` | Non | ≥ 0 |

Enums :

- `typeRefere` : `PROVISION_SALAIRES`, `EXPERTISE_MEDICALE`, `EXPERTISE_TECHNIQUE`,
  `MESURES_CONSERVATOIRES`, `REINTEGRATION_URGENCE`, `AUTRE`
- `natureCreance` : `SALAIRES_NON_VERSES`, `INDEMNITE_RUPTURE`,
  `HEURES_SUPPLEMENTAIRES`, `PRIMES`, `CONGES_PAYES`, `AUTRE`
- `preuvesUrgenceProduites` : `BULLETIN_PAIE`, `RELANCE_EMPLOYEUR`, `MISE_EN_DEMEURE`,
  `CONSTAT_HUISSIER`, `CERTIFICAT_MEDICAL`, `CONTRAT`, `AUTRE`
- `verdictRecommandation` : `PROVISION_PROBABLE`, `EXPERTISE_RECOMMANDEE`,
  `INSUFFISAMMENT_FONDE`, `AUTRE_VOIE_RECOMMANDEE`

---

## Contrat API (figé pour SF-DT-34-02 frontend)

### POST `/api/v1/case-files/{caseFileId}/refere-prudhomal`

Request :
```json
{
  "typeRefere": "PROVISION_SALAIRES",
  "natureCreance": "SALAIRES_NON_VERSES",
  "montantProvisionDemandeeEur": 5000.00,
  "absenceContestationSerieuse": true,
  "preuvesUrgenceProduites": ["BULLETIN_PAIE", "RELANCE_EMPLOYEUR"],
  "dommageImmediatCarac": true,
  "tresorerieEmployeurDouteuse": false,
  "dateMiseEnDemeure": "2026-04-01",
  "ancienneteContratMois": 18
}
```

Response 200 :
```json
{
  "caseFileId": "uuid",
  "typeRefere": "PROVISION_SALAIRES",
  "natureCreance": "SALAIRES_NON_VERSES",
  "montantProvisionDemandeeEur": 5000.00,
  "absenceContestationSerieuse": true,
  "preuvesUrgenceProduites": ["BULLETIN_PAIE", "RELANCE_EMPLOYEUR"],
  "dommageImmediatCarac": true,
  "tresorerieEmployeurDouteuse": false,
  "dateMiseEnDemeure": "2026-04-01",
  "ancienneteContratMois": 18,
  "scoreSuccess": 80,
  "verdictRecommandation": "PROVISION_PROBABLE",
  "delaiAudienceJoursPrevisionnel": 15,
  "delaiOrdonnanceJoursPrevisionnel": 8,
  "montantProvisionRecommandeEur": 5000.00,
  "baseJuridique": "Art. R.1454-1 + R.1454-3 + R.1455-1 Code travail",
  "formule": "Type PROVISION_SALAIRES + créance non contestée + 2 preuves + dommage immédiat → score 80",
  "messages": ["Référé statué dans 15 jours en moyenne (R.1455-1)", "..."],
  "country": "FRANCE"
}
```

### GET `/api/v1/case-files/{caseFileId}/refere-prudhomal`

Réponse identique au POST. 404 si pas de snapshot.

---

## Technique

### Endpoints

| Méthode | URL | Auth |
|---------|-----|------|
| POST | `/api/v1/case-files/{caseFileId}/refere-prudhomal` | Oui |
| GET  | `/api/v1/case-files/{caseFileId}/refere-prudhomal` | Oui |

### Tables

| Table | Opération |
|-------|-----------|
| `refere_prudhomal_analyses` | CREATE (migration 157) |
| `decision_tool_visibility_rules` | INSERT 1 ligne ALWAYS_ON FR DROIT_DU_TRAVAIL priority 63 |

### Migration Liquibase

- [x] `157-create-refere-prudhomal-analyses.xml`
- UUID visibility : `f1a04001-0000-0000-0000-ee0000000341`
- tool_id : `F-DT-34-refere-prudhomal`

---

## Plan de test

### Tests unitaires (≥ 14)

- [x] Type PROVISION + créance non contestée + 2 preuves + dommage immédiat → score 80 PROVISION_PROBABLE
- [x] Type PROVISION + score < 70 → AUTRE_VOIE_RECOMMANDEE
- [x] Type EXPERTISE_MEDICALE → EXPERTISE_RECOMMANDEE quel que soit le score
- [x] Type EXPERTISE_TECHNIQUE → EXPERTISE_RECOMMANDEE
- [x] Aucune condition cochée → score 0 INSUFFISAMMENT_FONDE
- [x] Score plafonné à 100 (toutes conditions + many preuves)
- [x] Bonus preuves : 1 preuve → 0 bonus, 2 preuves → +25, 5 preuves → +25 (cap)
- [x] Trésorerie douteuse seul → +10
- [x] BaseJuridique mentionne R.1454-1 et R.1455-1
- [x] Formule lisible
- [x] Délais 15j / 8j retournés
- [x] Montant provision recommandé = montant demandé si créance non contestée
- [x] Montant provision recommandé = 0 si créance contestée
- [x] typeRefere null → IllegalArgumentException
- [x] typeRefere inconnu → IllegalArgumentException
- [x] natureCreance inconnue → IllegalArgumentException
- [x] preuve inconnue → IllegalArgumentException
- [x] dateMiseEnDemeure future → IllegalArgumentException
- [x] montant négatif → IllegalArgumentException
- [x] Messages contiennent rappel R.1455-1 (15 jours)

### Tests d'intégration (≥ 8)

- [x] POST nominal FR PROVISION → 200 score ≥ 70
- [x] POST type EXPERTISE_MEDICALE → 200 verdict EXPERTISE_RECOMMANDEE
- [x] POST workspace BE → 400
- [x] POST dossier immigration → 400
- [x] POST autre workspace → 404
- [x] POST typeRefere invalide → 400
- [x] GET après POST → snapshot
- [x] GET sans POST → 404
- [x] POST upsert remplace l'existant
- [x] POST natureCreance inconnue → 400

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

- `SF-DT-34-02` — frontend Angular (panel + section component) ; consommera le contrat ci-dessus.

---

## Notes

- Le référé prud'homal est régi par les art. R.1454-1 à R.1455-12 du Code du travail.
  Conditions cumulatives : urgence + absence de contestation sérieuse OU mesures
  d'instruction utiles (R.1454-1 al. 2 pour les expertises).
- L'audience est tenue à bref délai (R.1455-1) — la pratique constante observe 15 jours
  entre l'assignation et l'audience devant la formation de référé du Conseil de prud'hommes.
- L'ordonnance est rendue dans un délai bref après l'audience (R.1455-2) — pratique
  constante 8 jours.
- Score et verdict sont indicatifs — l'avocat reste seul juge de l'opportunité d'engager
  le référé. F-IA-03 vérifiera la cohérence avec les autres outils côté frontend.
