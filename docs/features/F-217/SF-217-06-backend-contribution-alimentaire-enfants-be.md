# Mini-spec — F-217 / SF-217-06 — Backend : calculateur de contribution alimentaire des enfants (méthode Renard) BE

## Identifiant
`F-217 / SF-217-06`

## Feature parente
`F-217` — P2 Famille BE — outils décisionnels de fréquence haute (Vague 2 — Enfants)

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
`feat/SF-217-06-contribution-alimentaire-enfants-be`

---

## Objectif

Fournir un outil décisionnel backend qui estime la contribution alimentaire due pour les
enfants d'un couple séparé belge, selon les principes de la **méthode Renard** (méthode de
référence en Belgique pour le calcul des parts contributives — CC art. 203 et 203bis), et
répartit cette contribution entre les deux parents au prorata de leurs facultés.

---

## Comportement attendu

### Cas nominal

1. L'avocat soumet les éléments de la situation alimentaire : nombre d'enfants et leur
   tranche d'âge, revenu mensuel net de chaque parent, coût mensuel global de l'enfant
   (ou estimation forfaitaire par âge si non renseigné), nombre de nuits d'hébergement
   par parent sur le cycle, allocations familiales perçues, et frais extraordinaires.
2. Le `ContributionAlimentaireEnfantsBeCalculator` applique les principes de la méthode
   Renard : (a) le coût de l'enfant est réparti entre les parents **au prorata de leurs
   revenus disponibles** (CC art. 203 §1 — chacun contribue à proportion de ses facultés) ;
   (b) la part hébergement de chaque parent (proportionnelle aux nuits) est déduite de sa
   part contributive ; (c) les allocations familiales sont imputées sur le coût global ;
   (d) les frais extraordinaires sont partagés au même prorata (CC art. 203bis §3).
3. Le calculateur produit un verdict (`CONTRIBUTION_DUE` / `CONTRIBUTION_EQUILIBREE` /
   `DONNEES_INSUFFISANTES`), le montant mensuel net de contribution dû par le parent
   débiteur à l'autre, le détail du calcul (coût retenu, quote-part de chaque parent,
   parts hébergement), les bases juridiques et des messages d'aide.
4. Le résultat est persisté par dossier (un seul résultat courant, écrasé au recalcul)
   et renvoyé.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Corps de requête absent | Message d'erreur explicite | 400 |
| Nombre d'enfants ≤ 0 ou > 12 | Message d'erreur explicite | 400 |
| Revenu négatif | Message d'erreur explicite | 400 |
| Nombre de nuits hors plage 0–365 | Message d'erreur explicite | 400 |
| Commentaire > 1000 caractères | Message d'erreur explicite | 400 |
| Non authentifié | — | 401 |
| Dossier d'un autre workspace | Accès refusé | 403 |
| Dossier inexistant | — | 404 |
| `GET` sans calcul préalable | — | 404 |
| Dossier dont le `legalDomain` n'est pas `DROIT_FAMILLE` | Message explicite | 422 |
| Workspace dont le `country` n'est pas `BELGIQUE` (outil BE-only) | Message explicite | 422 |

---

## Contrat API (FIGÉ — importé par SF-217-07)

### POST `/api/v1/case-files/{caseFileId}/contribution-alimentaire-enfants-be`

Body `ContributionAlimentaireEnfantsBeRequest` :
```json
{
  "nombreEnfants": 2,
  "trancheAgeEnfants": "ENFANT_6_11",
  "revenuMensuelParent1": 2800.00,
  "revenuMensuelParent2": 1900.00,
  "coutMensuelGlobalEnfants": null,
  "nuitsHebergementParent1": 110,
  "nuitsHebergementParent2": 255,
  "allocationsFamilialesMensuelles": 340.00,
  "fraisExtraordinairesMensuels": 80.00,
  "parentDebiteurEstParent1": true,
  "commentaire": null
}
```
- `nombreEnfants` : entier obligatoire, 1–12.
- `trancheAgeEnfants` : enum obligatoire.
- `revenuMensuelParent1` / `revenuMensuelParent2` : décimaux obligatoires, ≥ 0.
- `coutMensuelGlobalEnfants` : décimal nullable ≥ 0 — si null, le calculateur applique un
  forfait par tranche d'âge × nombre d'enfants.
- `nuitsHebergementParent1` / `nuitsHebergementParent2` : entiers obligatoires, 0–365 ;
  leur somme doit valoir 365 (sinon normalisée au prorata, message d'avertissement).
- `allocationsFamilialesMensuelles` / `fraisExtraordinairesMensuels` : décimaux nullables ≥ 0.
- `parentDebiteurEstParent1` : boolean nullable (calculé si absent : le parent à la part
  contributive résiduelle positive est débiteur).
- `commentaire` : nullable, max 1000 caractères.
- Le pays est dérivé du workspace côté service.

Réponse `200` — `ContributionAlimentaireEnfantsBeResponse` : **ré-expose l'intégralité du
body** (snapshot) **+** les champs calculés.
```json
{
  "caseFileId": "uuid",
  "nombreEnfants": 2,
  "trancheAgeEnfants": "ENFANT_6_11",
  "revenuMensuelParent1": 2800.00,
  "revenuMensuelParent2": 1900.00,
  "coutMensuelGlobalEnfants": null,
  "nuitsHebergementParent1": 110,
  "nuitsHebergementParent2": 255,
  "allocationsFamilialesMensuelles": 340.00,
  "fraisExtraordinairesMensuels": 80.00,
  "parentDebiteurEstParent1": true,
  "commentaire": null,
  "verdict": "CONTRIBUTION_DUE",
  "coutMensuelRetenu": 700.00,
  "coutNetApresAllocations": 360.00,
  "quotePartParent1Pct": 59.6,
  "quotePartParent2Pct": 40.4,
  "partContributiveParent1": 214.56,
  "partContributiveParent2": 145.44,
  "partHebergementParent1": 108.49,
  "partHebergementParent2": 251.51,
  "contributionMensuelleNette": 106.07,
  "parentDebiteur": "PARENT_1",
  "fraisExtraordinairesQuotePartParent1": 47.68,
  "fraisExtraordinairesQuotePartParent2": 32.32,
  "detailCalcul": [
    "Coût mensuel retenu (forfait tranche 6-11 ans × 2 enfants) : 700,00 €",
    "Coût net après imputation des allocations familiales (340,00 €) : 360,00 €",
    "Quote-part Renard : Parent 1 = 59,6 % / Parent 2 = 40,4 % (prorata des revenus)",
    "Part hébergement déduite (prorata des nuits) : Parent 1 = 110/365, Parent 2 = 255/365",
    "Contribution nette due par le Parent 1 au Parent 2 : 106,07 € / mois"
  ],
  "basesJuridiques": [
    "CC art. 203 §1 (obligation d'entretien — proportionnelle aux facultés des parents)",
    "CC art. 203bis (part contributive et frais extraordinaires)",
    "Méthode Renard (méthode de référence — répartition du coût de l'enfant)"
  ],
  "messages": [
    "Estimation indicative selon les principes de la méthode Renard — à affiner avec les justificatifs de revenus et le détail du coût réel de l'enfant."
  ],
  "country": "BELGIQUE",
  "calculatedAt": "2026-05-18T10:00:00Z"
}
```

### GET `/api/v1/case-files/{caseFileId}/contribution-alimentaire-enfants-be`
- `200` : dernier résultat (même structure, inputs inclus → formulaire ré-éditable).
  `404` si jamais calculé. `403` / `401` / `422` idem POST.

### Enum `verdict` (`ContributionAlimentaireEnfantsBeVerdict`)
- `CONTRIBUTION_DUE` — une contribution mensuelle nette non nulle est due par un parent
  à l'autre.
- `CONTRIBUTION_EQUILIBREE` — les parts contributives résiduelles s'équilibrent
  (contribution nette ≈ 0, sous le seuil de 5 €/mois).
- `DONNEES_INSUFFISANTES` — revenus des deux parents nuls : le prorata Renard ne peut pas
  être établi.

### Enum `trancheAgeEnfants` (`TrancheAgeEnfantBe`)
`ENFANT_0_5` · `ENFANT_6_11` · `ENFANT_12_17` · `ENFANT_18_PLUS` (enfant majeur poursuivant
des études — CC art. 203 §1 al. 2).

### Enum `parentDebiteur` (`ParentDebiteurBe`)
`PARENT_1` · `PARENT_2` · `AUCUN` (contribution équilibrée).

---

## Règles de calcul — méthode Renard

> ⚠️ **Validation juridique requise** : la méthode Renard n'est pas codifiée — c'est une
> méthode jurisprudentielle de référence. Les barèmes forfaitaires de coût par tranche
> d'âge ci-dessous sont des **valeurs de calage à valider impérativement par un avocat
> belge** avant mise en production. CC art. 203 / 203bis sont à confirmer. Le contenu
> juridique (forfaits, articles) est centralisé dans le Calculator.

### Barème forfaitaire du coût mensuel par enfant (utilisé si `coutMensuelGlobalEnfants` null)

| Tranche d'âge | Forfait mensuel / enfant (à valider) |
|---------------|--------------------------------------|
| `ENFANT_0_5` | 280 € |
| `ENFANT_6_11` | 350 € |
| `ENFANT_12_17` | 420 € |
| `ENFANT_18_PLUS` | 500 € |

Coût retenu = forfait × `nombreEnfants` (ou `coutMensuelGlobalEnfants` si fourni).

### Étapes de calcul (figées dans le Calculator)

1. **Coût net** = `coutMensuelRetenu` − `allocationsFamilialesMensuelles` (plancher 0).
2. **Quote-part de chaque parent** (prorata des revenus — CC art. 203 §1) :
   `quotePartParent1 = revenu1 / (revenu1 + revenu2)`. Si revenus totaux = 0 →
   `DONNEES_INSUFFISANTES`.
3. **Part contributive théorique** de chaque parent = `coutNet × quotePart`.
4. **Part hébergement** assumée en nature = `coutNet × (nuitsParent / 365)`.
5. **Contribution résiduelle** d'un parent = `partContributive − partHebergement`.
   Le parent dont la résiduelle est positive verse à l'autre ; montant net =
   `|résiduelleParent1|` (les deux résiduelles sont opposées par construction).
6. **Verdict** : contribution nette < 5 € → `CONTRIBUTION_EQUILIBREE` ; sinon
   `CONTRIBUTION_DUE`.
7. **Frais extraordinaires** : répartis au même prorata Renard (CC art. 203bis §3),
   exposés séparément (non inclus dans la contribution mensuelle nette).

Tous les montants monétaires sont arrondis à 2 décimales (`RoundingMode.HALF_UP`).

---

## Conformité F-IA-04
- [x] **Non applicable au sens strict** — SF backend pure. Conformité F-IA-04 et seed
  `decision_tool_visibility_rules` portés par SF-217-07 frontend (couplé TOOL_REGISTRY).

---

## Critères d'acceptation

- [ ] `POST` calcule et persiste un résultat ; recalcul écrase le précédent (upsert 1:1).
- [ ] Coût retenu = forfait par tranche d'âge si `coutMensuelGlobalEnfants` null, sinon valeur fournie.
- [ ] Quote-part calculée au prorata des revenus ; revenus totaux nuls → `DONNEES_INSUFFISANTES`.
- [ ] Part hébergement déduite au prorata des nuits.
- [ ] Contribution nette = résiduelle du parent débiteur ; < 5 € → `CONTRIBUTION_EQUILIBREE`.
- [ ] Frais extraordinaires répartis au prorata Renard, exposés séparément.
- [ ] Montants arrondis à 2 décimales.
- [ ] La réponse `POST` / `GET` ré-expose l'intégralité des inputs (formulaire ré-éditable).
- [ ] `GET` renvoie le dernier résultat ou 404.
- [ ] `400` (corps absent, nombre d'enfants invalide, revenu négatif, nuits hors plage) /
      `403` / `404` / `422` / `401`.
- [ ] Isolation workspace testée.

---

## Périmètre

### Hors scope
- Frontend (SF-217-07).
- Révision de la contribution post-divorce (CJ art. 1288bis) — outil distinct reporté F-223.
- Recouvrement / SECAL (Service des créances alimentaires) — hors scope.
- Réutilisation du Calculator FR `F-FA-02` (barème JAF français — méthode juridiquement
  distincte de la méthode Renard).
- Pré-fill IA depuis l'analyse (aucun flag pivot dédié extrait par le pipeline V1 —
  documenté `PREFILL_COUNT_ALWAYS_ZERO` côté SF-217-07).

---

## Technique

### Endpoints
| Méthode | URL | Auth | Rôle |
|---------|-----|------|------|
| POST | `/api/v1/case-files/{caseFileId}/contribution-alimentaire-enfants-be` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/contribution-alimentaire-enfants-be` | Oui | MEMBER |

### Tables impactées
| Table | Opération | Notes |
|-------|-----------|-------|
| `contribution_alimentaire_enfants_be_analyses` | CREATE + INSERT/UPDATE/SELECT | id (UUID), case_file_id (FK UNIQUE), country (VARCHAR), snapshot_data (TEXT JSON), created_at, updated_at |
| `decision_tool_visibility_rules` | INSERT (seed) | porté par SF-217-07 |

### Migration Liquibase
- [x] Oui — `239-create-contribution-alimentaire-enfants-be-analyses.xml` (table seule).

### Classes backend (pattern `ProcedureNulliteLicenciement*` — snapshot JSON)
`ContributionAlimentaireEnfantsBeCalculator` (static), `ContributionAlimentaireEnfantsBeInput`,
`ContributionAlimentaireEnfantsBeResult`, `ContributionAlimentaireEnfantsBeRequest`,
`ContributionAlimentaireEnfantsBeResponse`, `ContributionAlimentaireEnfantsBeAnalysis` (@Entity),
`ContributionAlimentaireEnfantsBeRepository`, `ContributionAlimentaireEnfantsBeService`,
`ContributionAlimentaireEnfantsBeController`.

---

## Plan de test

### Tests unitaires (Calculator)
- [ ] 2 enfants 6-11, revenus 2800/1900, hébergement déséquilibré → `CONTRIBUTION_DUE`,
      Parent 1 débiteur, montant > 0.
- [ ] Coût forfaitaire appliqué par tranche d'âge si `coutMensuelGlobalEnfants` null.
- [ ] Coût explicite fourni → utilisé tel quel.
- [ ] Revenus égaux + hébergement 182/183 → contribution nette < 5 € → `CONTRIBUTION_EQUILIBREE`.
- [ ] Revenus des deux parents = 0 → `DONNEES_INSUFFISANTES`.
- [ ] Allocations familiales > coût brut → coût net plafonné à 0.
- [ ] Frais extraordinaires répartis au prorata Renard.
- [ ] Somme des nuits ≠ 365 → normalisation + message d'avertissement.
- [ ] Arrondi à 2 décimales vérifié.

### Tests d'intégration (Controller)
- [ ] `POST` → 200 + persistance ; recalcul écrase.
- [ ] `GET` → 200 / 404 si jamais calculé.
- [ ] `400` (corps absent, nombreEnfants = 0, revenu négatif, nuits = 400) / `403` / `404` /
      `422` (domaine ≠ famille, pays ≠ Belgique) / `401`.

### Isolation workspace
- [x] Applicable — un utilisateur du workspace A ne peut pas calculer/lire le résultat
  d'un dossier du workspace B.

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Outil décisionnel métier** — `contribution-alimentaire-enfants-be` est un nouvel
  outil décisionnel. Scan fait dans `SF-217-00-coherence.md` : aucun outil BE existant ne
  couvre la contribution alimentaire des enfants belge (F-FA-02 pension alimentaire est
  FR-only, barème JAF, masqué en BE). La méthode Renard est spécifique au droit belge.
  `contribution-alimentaire-enfants-be` est une **situation métier distincte** —
  un outil = une situation.
- [x] Aucune autre préoccupation transversale.

### Smoke tests E2E
- [x] Aucun — feature additive.

---

## Dépendances
- Aucune SF bloquante. SF-217-07 (frontend) importe le contrat API. Parallélisable.

---

## Notes et décisions
- Persistance par snapshot JSON (`snapshot_data`) — aligné Vague 1 F-217.
- Aucune réutilisation du Calculator FR : la méthode Renard belge diffère du barème JAF
  français — modèle de répartition du coût de l'enfant distinct (`feedback_belgique_never_forget`).
- Les forfaits de coût par tranche d'âge sont des valeurs de calage : ils sont centralisés
  dans le Calculator et **explicitement signalés pour validation par un avocat belge**.
  La méthode Renard étant jurisprudentielle (non codifiée), le résultat est qualifié
  d'« estimation indicative » dans les messages — pas un montant opposable.
</content>
</invoke>
