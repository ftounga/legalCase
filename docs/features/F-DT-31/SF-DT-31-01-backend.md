# Mini-spec — F-DT-31 / SF-DT-31-01 Backend transaction art. 2044-2052 Cciv (FR)

## Identifiant

`F-DT-31 / SF-DT-31-01`

## Feature parente

`F-DT-31` — Transaction / protocole transactionnel (droit du travail FR)

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-DT-31-01-transaction-backend`

---

## Objectif

Outil décisionnel backend qui analyse la validité d'un protocole transactionnel (rupture
amiable post-licenciement / rupture conv.) au regard des articles 2044 à 2052 du Code civil
et de la jurisprudence Cass. soc. 16/12/2010 sur les concessions réciproques, et produit
un score de validité 0-100 + verdict (VALIDE / CONTESTABLE / NULLE) + risque de nullité.

---

## Comportement attendu

### Cas nominal

Entrée : date de signature, listes de concessions employeur et salarié (enums), montants
(indemnité transactionnelle, salaire mensuel brut, ancienneté), garde-fous procéduraux
(délai de réflexion 15 jours, présence avocat, renonciation expresse à l'action prud'homale,
vice du consentement allégué) et nature de la rupture préalable.

Le calculateur :

1. Détermine si les **concessions réciproques** sont caractérisées : ≥ 1 concession côté
   employeur ET ≥ 1 côté salarié (Cass. soc. 16/12/2010 n° 09-67.679 — sans concessions
   réciproques, la transaction est nulle).
2. Calcule le **ratio des concessions employeur** = nombre concessions employeur /
   (concessions employeur + concessions salarié) en %.
3. Évalue si l'indemnité transactionnelle dépasse le **plancher Macron** (art. L.1235-3) :
   approximation barème = `½ × salaireMensuel × min(ancienneté, 30)` (borne basse pour
   un licenciement personnel hors nullité).
4. Score 0-100 :
   - Concessions réciproques caractérisées : +30
   - Délai de réflexion 15 jours respecté : +20
   - Présence avocat assistance : +15
   - Absence de vice du consentement allégué : +15
   - Indemnité transactionnelle > plancher Macron : +20
5. Verdict :
   - `VALIDE` ≥ 70
   - `CONTESTABLE` 40-69
   - `NULLE` < 40
6. `risqueNulliteRetenu` = vrai si vice du consentement allégué OU absence de concessions
   réciproques.

Persistance : snapshot JSON complet (inputs + outputs) dans une entité 1:1 par dossier
(table `transaction_analyses`).

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| `dateSignature` absente | Erreur explicite | 400 |
| `salaireMensuelBrutEur` ≤ 0 | Erreur explicite | 400 |
| `ancienneteAnnees` < 0 | Erreur explicite | 400 |
| `indemniteTransactionnelleEur` < 0 | Erreur explicite | 400 |
| Workspace pays = BELGIQUE | Refus — outil FR uniquement (BE = feature jumelle backlog) | 400 |
| Dossier ≠ DROIT_DU_TRAVAIL | Refus | 400 |
| `caseFileId` autre workspace | 404 | 404 |
| GET sans POST préalable | 404 | 404 |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] Outils similaires : F-DT-08 (validité licenciement) + F-DT-10 (validité rupture conv.)
  + F-DT-16 (licenciement nul détection) reposent sur le même pattern « score 0-100 + verdict
  3 niveaux + JSON snapshot ». Pattern réutilisé tel quel.
- [x] FR vs BE : la BE a sa propre transaction (Loi 03/07/1978 + CCT) avec mécanique
  différente (pas de concessions réciproques au sens Cass. fr) → SF jumelle F-DT-31 BE
  scopée séparément (backlog).
- [x] Domaines : Famille (F-FA) et Immigration (F-IM) ont leurs propres transactions
  (médiation civile / accord transactionnel asile) — non couvertes ici, hors scope.
- [x] UI patterns : pas concerné côté backend (frontend SF-DT-31-02 vague suivante).
- [x] Pré-remplissage IA : possible depuis synthèse (date rupture, indemnité versée,
  salaire) — frontend SF-DT-31-02 le câblera.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern score+verdict (F-DT-08/10/16/32) | Oui | Réutilisé tel quel |
| F-DT-31 BE | Oui | Backlog feature jumelle |
| Refresh dashboard F-IA-02 | Oui | Frontend SF-DT-31-02 |
| F-IA-03 cohérence | Oui | Date signature + salaire croisent avec autres outils — frontend |
| F-IA-04 visibility rule | Oui | ALWAYS_ON FRANCE DROIT_DU_TRAVAIL priority 61 |

### Décision

- [x] Étendu à toutes les cibles applicables backend dans cette SF
- [x] Frontend SF-DT-31-02 vague suivante (panel + intégration F-IA-04)
- [x] BE = feature jumelle backlog
- [x] Famille / Immigration = features dédiées (hors scope F-DT-31)

---

## Impact par domaine métier

Cette feature est **sensible au domaine** : strictement DROIT_DU_TRAVAIL (FR uniquement
pour cette SF). Famille et Immigration ont leurs propres mécaniques de transaction qui
font l'objet de features dédiées au backlog (médiation civile FA, accord transactionnel
préfecture IM).

## Parité des domaines métier

Outil de **niveau 5 (scoring/analyse de validité)** :

- DROIT_DU_TRAVAIL FR : couvert par cette SF
- DROIT_DU_TRAVAIL BE : feature jumelle backlog (F-DT-31 BE — Loi 03/07/1978 + CCT)
- FAMILLE FR/BE : équivalent fonctionnel = médiation civile / convention divorce — features
  dédiées (F-FA-XX backlog) — pas même cadre juridique
- IMMIGRATION FR/BE : équivalent fonctionnel = transaction préfecture / accord transactionnel
  asile — feature dédiée (F-IM-XX backlog) — pas même cadre juridique

Justification : les concessions réciproques au sens Cass. soc. 16/12/2010 sont un concept
spécifique au droit du travail français. Les autres domaines/pays ont besoin de leurs
propres outils — ce qui est tracé dans le backlog produit.

---

## Critères d'acceptation

- [x] `POST /api/v1/case-files/{caseFileId}/transaction` calcule + persiste le snapshot
- [x] `GET` retourne le dernier snapshot
- [x] Concessions réciproques caractérisées si ≥1 employeur ET ≥1 salarié
- [x] Score 0-100 selon barème (réciproques 30 / délai 20 / avocat 15 / absence vice 15 /
      indemnité 20)
- [x] Verdict VALIDE ≥ 70, CONTESTABLE 40-69, NULLE < 40
- [x] `risqueNulliteRetenu` = vice OU absence concessions réciproques
- [x] Ratio concessions employeur calculé en % (0-100)
- [x] BELGIQUE refusée (400)
- [x] Dossier non droit du travail refusé (400)
- [x] Migration Liquibase 150 + visibility rule UUID `f1a04001-0000-0000-0000-ee0000000311`,
      priority 61
- [x] ≥ 14 UT + ≥ 8 IT

---

## Périmètre

### Hors scope

- Génération PDF du protocole (autre SF F-DT-31-03 backlog)
- Frontend (SF-DT-31-02 vague suivante)
- Belgique (feature jumelle backlog)
- Famille / Immigration (features dédiées backlog)
- Détection automatique d'incohérences avec d'autres outils (F-IA-03 — frontend)

---

## Contraintes de validation

| Champ | Obligatoire | Format |
|-------|-------------|--------|
| `dateSignature` | Oui | ISO date |
| `salaireMensuelBrutEur` | Oui | > 0 |
| `ancienneteAnnees` | Oui | ≥ 0 |
| `indemniteTransactionnelleEur` | Oui | ≥ 0 |
| `concessionsEmployeur` | Non (default `[]`) | array enum |
| `concessionsSalarie` | Non (default `[]`) | array enum |
| `renonciationActionExpresse` | Non (default false) | bool |
| `delaiReflexion15jOk` | Non | bool |
| `rupturePrealable` | Non | enum |
| `presenceAvocatAssistance` | Non | bool |
| `viceConsentementAllégué` | Non | bool |

---

## Contrat API (figé pour SF-DT-31-02)

### Endpoints

| Méthode | URL | Auth |
|---------|-----|------|
| POST | `/api/v1/case-files/{caseFileId}/transaction` | Oui |
| GET  | `/api/v1/case-files/{caseFileId}/transaction` | Oui |

### Request body

```json
{
  "dateSignature": "2026-04-15",
  "concessionsEmployeur": ["INDEMNITE_TRANSACTIONNELLE"],
  "concessionsSalarie": ["RENONCIATION_ACTION_PRUDHOMALE"],
  "indemniteTransactionnelleEur": 15000.00,
  "salaireMensuelBrutEur": 2500.00,
  "ancienneteAnnees": 8,
  "renonciationActionExpresse": true,
  "delaiReflexion15jOk": true,
  "rupturePrealable": "LICENCIEMENT_PERSONNEL",
  "presenceAvocatAssistance": true,
  "viceConsentementAllegue": false
}
```

### Enums

- `concessionsEmployeur` : `INDEMNITE_TRANSACTIONNELLE`, `MAINTIEN_AVANTAGES_SOCIAUX`,
  `LETTRE_RECOMMANDATION`, `LEVEE_NON_CONCURRENCE`, `DELAI_PRESCRIPTION_ANTICIPE`, `AUTRE`
- `concessionsSalarie` : `RENONCIATION_ACTION_PRUDHOMALE`, `RENONCIATION_PARTAGE_BENEFICES`,
  `AUTRE`
- `rupturePrealable` : `LICENCIEMENT_PERSONNEL`, `LICENCIEMENT_ECONOMIQUE`,
  `RUPTURE_CONVENTIONNELLE`, `DEMISSION`, `FIN_CDD`, `AUTRE`
- `verdictValiditeContrat` : `VALIDE`, `CONTESTABLE`, `NULLE`

### Response body

```json
{
  "caseFileId": "uuid",
  "dateSignature": "2026-04-15",
  "concessionsEmployeur": ["INDEMNITE_TRANSACTIONNELLE"],
  "concessionsSalarie": ["RENONCIATION_ACTION_PRUDHOMALE"],
  "indemniteTransactionnelleEur": 15000.00,
  "salaireMensuelBrutEur": 2500.00,
  "ancienneteAnnees": 8,
  "renonciationActionExpresse": true,
  "delaiReflexion15jOk": true,
  "rupturePrealable": "LICENCIEMENT_PERSONNEL",
  "presenceAvocatAssistance": true,
  "viceConsentementAllegue": false,
  "concessionsReciproquesCaracterisees": true,
  "ratioConcessionsEmployeurPct": 50.0,
  "indemniteTransactionnelleSuperieureMacron": true,
  "scoreValidite": 100,
  "verdictValiditeContrat": "VALIDE",
  "risqueNulliteRetenu": false,
  "baseJuridique": "Art. 2044 + 2052 Cciv + Cass. soc. 16/12/2010 n° 09-67.679",
  "formule": "Score 100/100 — VALIDE | concessions réciproques | délai 15j OK | avocat | absence vice | indemnité > Macron",
  "messages": ["..."],
  "country": "FRANCE"
}
```

### Tables

| Table | Opération |
|-------|-----------|
| `transaction_analyses` | CREATE (migration 150) |
| `decision_tool_visibility_rules` | INSERT 1 ligne ALWAYS_ON FR DROIT_DU_TRAVAIL priority 61 |

### Migration Liquibase

- [x] `150-create-transaction-analyses.xml`
- UUID visibility : `f1a04001-0000-0000-0000-ee0000000311`

---

## Plan de test

### Tests unitaires (≥ 14)

- [x] Concessions réciproques caractérisées (1 emp + 1 sal) → score +30
- [x] Aucune concession employeur → réciproques NON caractérisées + risqueNullité
- [x] Aucune concession salarié → réciproques NON caractérisées + risqueNullité
- [x] Toutes garanties OK + indemnité > Macron → score 100 VALIDE
- [x] Vice du consentement allégué → risqueNulliteRetenu = true (même si score haut)
- [x] Délai 15j respecté → +20
- [x] Présence avocat → +15
- [x] Indemnité ≤ plancher Macron → pas de bonus
- [x] Indemnité > plancher Macron → +20
- [x] Score 70 → VALIDE
- [x] Score 60 → CONTESTABLE
- [x] Score 35 → NULLE
- [x] Ratio concessions employeur 0% si pas de concession employeur
- [x] Ratio concessions employeur 100% si que côté employeur
- [x] Ratio 50% si symétrique
- [x] BaseJuridique mentionne 2044, 2052, Cass. 16/12/2010
- [x] Formule lisible avec score
- [x] Salaire 0 → IllegalArgumentException
- [x] Salaire null → IllegalArgumentException
- [x] dateSignature null → IllegalArgumentException
- [x] Pays BELGIQUE → IllegalArgumentException
- [x] Pays null → IllegalArgumentException
- [x] Input null → IllegalArgumentException
- [x] Ancienneté négative → IllegalArgumentException
- [x] Indemnité négative → IllegalArgumentException

### Tests d'intégration (≥ 8)

- [x] POST nominal FR concessions réciproques + tout OK → 200 score 100 VALIDE
- [x] POST aucune concession salarié → 200 NULLE + risqueNullité
- [x] POST vice du consentement → 200 risqueNullité
- [x] POST workspace BE → 400
- [x] POST dossier immigration → 400
- [x] POST autre workspace → 404
- [x] POST salaire 0 → 400
- [x] POST sans dateSignature → 400
- [x] GET après POST → snapshot
- [x] GET sans POST → 404
- [x] POST upsert remplace l'analyse précédente

### Isolation workspace

- [x] Applicable — test 404 si workspace différent

---

## Analyse d'impact

### Préoccupations transversales

- [x] Aucune préoccupation transversale — endpoint isolé sur un dossier (pattern conforme
      F-DT-08/10/16/32)

### Smoke tests E2E

- [x] Aucun smoke test concerné — outil métier indépendant.

---

## Dépendances

### Subfeatures bloquantes

- Aucune

### Frontend planifié (vague suivante)

- `SF-DT-31-02` — frontend Angular (panel + section component) ; consommera le contrat
  ci-dessus.

---

## Notes

- Cass. soc. 16/12/2010 n° 09-67.679 : la transaction qui ne comporte pas de concessions
  réciproques est nulle. Pratique constante depuis (Cass. soc. 26/10/2011, 25/05/2017,
  etc.).
- Le délai de réflexion de 15 jours n'est pas légalement imposé en transaction (≠ rupture
  conv.) mais la jurisprudence valorise sa présence comme indice du consentement éclairé.
- La présence d'un avocat n'est pas non plus obligatoire mais elle écarte fortement les
  contestations ultérieures pour vice du consentement.
- L'approximation Macron (½ × salaire × min(anc, 30)) est une borne basse pour un
  licenciement personnel hors nullité — usage indicatif uniquement.
