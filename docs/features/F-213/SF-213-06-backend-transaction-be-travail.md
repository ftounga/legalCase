# Mini-spec — F-213 / SF-213-06-backend Outil transaction fin contrat BE — validité + checklist renonciations

## Identifiant

`F-213 / SF-213-06-backend`

## Feature parente

`F-213` — P2 Travail BE — ~10 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-213-06-backend-transaction-be-travail`

---

## Objectif

Vérification de la validité d'une transaction de fin de contrat selon le droit belge (**art. 2044 Code civil belge ; Loi 03/07/1978**) et checklist des renonciations expresses obligatoires. **BELGIQUE UNIQUEMENT** — jumeau annoncé de F-DT-31 FR, mais le régime est différent : en BE, les renonciations doivent être **expresses et spécifiques** (pas de renonciation globale « à tous droits »), et certains droits sont **indisponibles** (ex. créances salariales futures, CCT impératives).

---

## Source juridique BE

- **Art. 2044 et suivants du Code civil belge** : définition de la transaction (contrat par lequel les parties terminent une contestation née ou préviennent une contestation à naître, par des concessions réciproques).
- **Loi du 3 juillet 1978** art. 6 : caractère d'ordre public de certaines dispositions — les droits qui découlent de dispositions impératives ne peuvent être abdiqués par avance (ex. droit au préavis légal minimum).
- **Jurisprudence** : les renonciations à des droits acquis sont valides ; les renonciations à des droits futurs ou à des dispositions d'ordre public sont nulles.
- **Exigence de concessions réciproques** : la transaction est nulle sans concessions des deux parties. Si l'employeur ne cède rien, ce n'est pas une transaction mais une quittance.
- **À vérifier** : jurisprudence récente Cour du travail BE sur la portée des clauses de non-recours.

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/transaction-be-travail`

Inputs (body) :
- `montantTransactionBrut` (BigDecimal, €) — montant total proposé, obligatoire.
- `indemniteLegaleEtimee` (BigDecimal, €) — indemnité légale à laquelle le salarié aurait eu droit (ICP + CCT 109 etc.), optionnel mais recommandé.
- `concessionsEmployeurDescrites` (boolean) — l'employeur a-t-il concédé quelque chose (ex. dispense de préavis, paiement immédiat, absence de mention « faute grave » sur C4) ?
- `renonciationsListees` (List<String>) — liste des droits auxquels le salarié renonce (ex. « Renonciation à l'action en indemnité de rupture », « Renonciation à l'action CCT 109 », etc.).
- `renonciationOrdrePunlicDetectee` (boolean, défaut false) — flag IA : renonciation à un droit d'ordre public détectée.
- `mentionContestation` (boolean) — la transaction fait-elle référence à une contestation née ou à naître ?

Logique (`TransactionBeTravailValidator`) :

| Condition | Verdict | Raison |
|---|---|---|
| `!concessionsEmployeurDescrites` | `INVALIDE` | Absence de concessions réciproques — ce n'est pas une transaction (art. 2044 ABC) |
| `renonciationOrdrePunlicDetectee` | `INVALIDE_PARTIELLE` | Renonciation à un droit d'ordre public — clause partiellement nulle |
| `!mentionContestation` | `A_COMPLETER` | La transaction ne vise pas de litige précis — risque de qualification juridique |
| Toutes conditions OK | `VALIDE` | Transaction valide selon les éléments saisis |

Calcul ratio :
- Si `indemniteLegaleEtimee` > 0 : `ratioPourcentage = (montantTransactionBrut / indemniteLegaleEtimee) × 100`.
- Si ratio < 50 % : `avertissement = "Montant de la transaction inférieur à 50 % de l'indemnité légale estimée — risque de lésion / vice du consentement à analyser"`.

Output :
```json
{
  "verdict": "VALIDE" | "INVALIDE" | "INVALIDE_PARTIELLE" | "A_COMPLETER",
  "raisonInvalidite": null | "ABSENCE_CONCESSIONS" | "RENONCIATION_ORDRE_PUBLIC" | "LITIGE_NON_VISE",
  "ratioPourcentage": 75.0,
  "avertissement": null,
  "checklistRenonciations": [
    { "item": "Renonciation à l'action en indemnité de rupture", "valide": true },
    { "item": "Renonciation à l'action CCT 109", "valide": true }
  ],
  "baseJuridique": "Art. 2044 Code civil belge ; Loi 03/07/1978 art. 6"
}
```

Persistance : `transaction_be_travail_analyses` — unique sur `case_file_id`.

`GET` → dernière analyse ou 404.

### Cas d'erreur

| Situation | Code | Comportement |
|---|---|---|
| `workspaceCountry !== 'BELGIQUE'` | 404 | Isolation |
| `montantTransactionBrut` ≤ 0 | 400 | Invalide |
| `renonciationsListees` vide | 200 + A_COMPLETER | Pas une erreur, mais verdict incomplet |

---

## Champs IA à extraire — BELGIQUE UNIQUEMENT

| Champ | Type | Champ `TravailExtractedData` BE | Notes |
|---|---|---|---|
| `montantTransactionBrut` | BigDecimal | `montantTransactionPropose` — **BELGIQUE UNIQUEMENT** | Extrait de l'accord ou document précontractuel |
| `concessionsEmployeurDescrites` | boolean | `transactionConcessionsPresentes` — **BELGIQUE UNIQUEMENT** | |
| `renonciationOrdrePunlicDetectee` | boolean | `transactionRenonciationOrdrePunlic` — **BELGIQUE UNIQUEMENT** | Détection IA de clauses générales |
| `mentionContestation` | boolean | `transactionMentionneControverse` — **BELGIQUE UNIQUEMENT** | |

`critereCode` : `BE_TRANSACTION_CONCESSIONS`, `BE_TRANSACTION_RENONCIATIONS`, `BE_TRANSACTION_MONTANT`.

---

## Critères d'acceptation

- [ ] Absence de concessions → `INVALIDE` / `ABSENCE_CONCESSIONS`.
- [ ] Renonciation ordre public → `INVALIDE_PARTIELLE`.
- [ ] Ratio < 50 % → `avertissement` présent.
- [ ] Workspace France → 404.
- [ ] `CritereCodeIntegrityIT` vert.

---

## Périmètre

### Hors scope

- Frontend — SF-213-06b.
- Analyse détaillée du contenu des clauses (lecture IA du document transaction) — pré-fill IA uniquement sur les métadonnées.
- Transaction pour **rupture conventionnelle** — pas de procédure formelle en BE (rupture de gré à gré couverte par F-132 existant).

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/transaction-be-travail` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/transaction-be-travail` | OIDC | MEMBER |

### Tables

`transaction_be_travail_analyses` — unique `case_file_id`.

### Composants backend

- `TransactionBeTravail{Analysis,Repository,Request,Result,Response,Service,Validator,Controller}.java`
- `TransactionBeTravailChecklistItem.java` — record item checklist
- Extension `TravailExtractedData` + `LegalDomainPromptBuilder` BE
- Migration `XXX-create-transaction-be-travail-analyses.xml`

---

## Plan de test

### Unitaires

- [ ] Absence concessions → `INVALIDE`.
- [ ] Renonciation ordre public → `INVALIDE_PARTIELLE`.
- [ ] Ratio 45 % → avertissement présent.
- [ ] Ratio 80 % → avertissement null.
- [ ] Toutes conditions OK → `VALIDE`.

### Intégration

- [ ] `POST` BE → 200, `POST` FR → 404.
- [ ] `GET` après POST → 200.

---

## Dépendances

- Aucune SF bloquante.
