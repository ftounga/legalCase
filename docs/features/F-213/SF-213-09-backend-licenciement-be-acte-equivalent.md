# Mini-spec — F-213 / SF-213-09-backend Outil acte équipollent à rupture BE — analyseur validité

## Identifiant

`F-213 / SF-213-09-backend`

## Feature parente

`F-213` — P2 Travail BE — ~10 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-213-09-backend-licenciement-be-acte-equivalent`

---

## Objectif

Analyseur de l'**acte équipollent à rupture** — concept belge spécifique par lequel une modification unilatérale substantielle d'un élément essentiel du contrat par l'employeur équivaut à un licenciement imputable à l'employeur, ouvrant droit à l'indemnité compensatoire de préavis. **Loi 03/07/1978 art. 20** et jurisprudence Cass. BE. **BELGIQUE UNIQUEMENT** — le concept belge est distinct du régime FR (prise d'acte FR + résiliation judiciaire).

---

## Source juridique BE

- **Loi du 3 juillet 1978** art. 20 : l'employeur ne peut modifier unilatéralement un élément essentiel du contrat.
- **Jurisprudence** Cass. BE (arrêt 23/12/1957 et jurisprudence postérieure consolidée) : si l'employeur modifie unilatéralement un élément essentiel, le salarié peut soit accepter (sous conditions), soit considérer le contrat rompu aux torts de l'employeur → **acte équipollent à rupture** = rupture imputable à l'employeur = droit à ICP.
- **Éléments essentiels** : salaire, lieu de travail substantiel (≠ mobilité contractuelle), fonction substantielle, horaire (si condition essentielle du contrat).
- **Doctrine Ius Variandi** : l'employeur conserve un pouvoir de direction limité (modifications mineures) — l'acte équipollent concerne uniquement les modifications **substantielles**.
- **Délai d'action** : le salarié doit réagir rapidement (le silence prolongé peut valoir acceptation tacite) — pas de délai légal strict mais jurisprudence recommande de réagir dans les 30 jours.

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-acte-equivalent`

Inputs (body) :
- `typeModification` (enum) — `SALAIRE` | `LIEU_TRAVAIL` | `FONCTION` | `HORAIRE` | `AUTRE`.
- `ampleurModification` (enum) — `MINEURE` | `SUBSTANTIELLE` | `INDETERMINEES`.
- `elementEssentielDuContrat` (boolean) — la clause modifiée est-elle un élément essentiel du contrat ?
- `dateModification` (ISO date) — obligatoire.
- `salarieAProt este` (boolean, défaut false) — le salarié a-t-il protesté formellement.
- `delaiDepuisModificationJours` (int, optionnel) — jours depuis la modification (calculé si `dateModification` fournie).
- `remunerationHebdomadaireBrute` (BigDecimal, €) — pour calcul ICP indicatif.
- `dureePreavisCalculeeSemaines` (int, optionnel) — si calculé par ailleurs (outil statut unique ou Claeys), pour le renvoi.

Logique (`LicenciementBeActeEquipollentAnalyzer`) :

| Condition | Verdict |
|---|---|
| `ampleurModification = MINEURE` | `PAS_ACTE_EQUIPOLLENT` — modification dans le Ius Variandi |
| `ampleurModification = SUBSTANTIELLE` && `elementEssentielDuContrat = true` | `ACTE_EQUIPOLLENT_PROBABLE` |
| `ampleurModification = SUBSTANTIELLE` && `elementEssentielDuContrat = false` | `A_ANALYSER` — dépend de la jurisprudence |
| `ampleurModification = INDETERMINEES` | `A_ANALYSER` |
| `!salariéAProteste` && `delaiDepuisModificationJours > 30` | `RISQUE_ACCEPTATION_TACITE` (avertissement ajouté) |

**Calcul ICP indicatif :**
- Si `ACTE_EQUIPOLLENT_PROBABLE` && `remunerationHebdomadaireBrute > 0` && `dureePreavisCalculeeSemaines > 0` :
  - `icpIndicatif = remunerationHebdomadaireBrute × dureePreavisCalculeeSemaines`
  - Marqué comme « indicatif — à combiner avec l'outil préavis statut unique ou formule Claeys ».

Output :
```json
{
  "verdict": "ACTE_EQUIPOLLENT_PROBABLE" | "PAS_ACTE_EQUIPOLLENT" | "RISQUE_ACCEPTATION_TACITE" | "A_ANALYSER",
  "fondamentJuridique": "Loi 03/07/1978 art. 20 — modification unilatérale élément essentiel",
  "icpIndicatif": 27000.00,
  "risqueAcceptationTacite": false,
  "delaiRecommandeProtestationJours": 30,
  "baseJuridique": "Loi 03/07/1978 art. 20 ; Cass. BE 23/12/1957",
  "avertissement": null
}
```

Persistance : `licenciement_be_acte_equivalent_analyses` — unique sur `case_file_id`.

### Cas d'erreur

| Situation | Code | Comportement |
|---|---|---|
| `workspaceCountry !== 'BELGIQUE'` | 404 | Isolation |
| `typeModification` manquant | 400 | Obligatoire |
| `ampleurModification` manquant | 400 | Obligatoire |

---

## Champs IA à extraire — BELGIQUE UNIQUEMENT

| Champ | Type | Champ `TravailExtractedData` BE | Notes |
|---|---|---|---|
| `typeModification` | enum | `typeModificationUnilaterale` — **BELGIQUE UNIQUEMENT** | Extrait des documents (courrier employeur, avenant) |
| `dateModification` | date | `dateModificationUnilaterale` — **BELGIQUE UNIQUEMENT** | |
| `ampleurModification` | enum | `ampleurModificationDetectee` — **BELGIQUE UNIQUEMENT** | Dérivé IA (analyse du changement) |

`critereCode` : `BE_ACTE_EQUIPOLLENT_TYPE`, `BE_ACTE_EQUIPOLLENT_AMPLEUR`, `BE_ACTE_EQUIPOLLENT_DATE`.

---

## Critères d'acceptation

- [ ] Modification substantielle élément essentiel → `ACTE_EQUIPOLLENT_PROBABLE`.
- [ ] Modification mineure → `PAS_ACTE_EQUIPOLLENT`.
- [ ] Délai > 30 j sans protestation → `RISQUE_ACCEPTATION_TACITE` en avertissement.
- [ ] ICP indicatif calculé si données disponibles.
- [ ] Workspace France → 404.
- [ ] `CritereCodeIntegrityIT` vert.

---

## Périmètre

### Hors scope

- Frontend — SF-213-09b.
- **Prise d'acte FR** (art. L. 1237-19 CT FR) — différent, hors scope.
- **Clause de mobilité** — outil distinct (`clause-mobilite-be`, P3).

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-acte-equivalent` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-acte-equivalent` | OIDC | MEMBER |

### Tables

`licenciement_be_acte_equivalent_analyses` — unique `case_file_id`.

### Composants backend

- `LicenciementBeActeEquipollent{Analysis,Repository,Request,Result,Response,Service,Analyzer,Controller}.java`
- Enums `TypeModification`, `AmpleurModification`
- Extension `TravailExtractedData` + `LegalDomainPromptBuilder` BE
- Migration `XXX-create-licenciement-be-acte-equivalent-analyses.xml`

---

## Plan de test

### Unitaires

- [ ] Modification salaire substantielle, élément essentiel → `ACTE_EQUIPOLLENT_PROBABLE`.
- [ ] Modification horaire mineure → `PAS_ACTE_EQUIPOLLENT`.
- [ ] 35 jours sans protestation + substantielle → avertissement `RISQUE_ACCEPTATION_TACITE`.
- [ ] ICP indicatif : 500 € × 27 sem = 13 500 €.

### Intégration

- [ ] `POST` BE → 200, `POST` FR → 404.

---

## Dépendances

- Aucune SF bloquante. ICP indicatif optionnel (nécessite préavis calculé par SF-213-03 ou SF-213-04).
