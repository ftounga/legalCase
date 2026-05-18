# Mini-spec — F-217 / SF-217-08 — Backend : analyseur de la pension alimentaire entre ex-époux belge

## Identifiant
`F-217 / SF-217-08`

## Feature parente
`F-217` — P2 Famille BE — outils décisionnels de fréquence haute (Vague 2 — Enfants)

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
`feat/SF-217-08-contribution-conjoint-be`

---

## Objectif

Fournir un outil décisionnel backend qui analyse le droit à une pension alimentaire après
divorce entre ex-époux belges (CC art. 301), apprécie sa recevabilité, plafonne sa durée
légale et estime un montant indicatif à partir des besoins du créancier et des facultés du
débiteur.

---

## Comportement attendu

### Cas nominal

1. L'avocat soumet les éléments de la situation : type de divorce prononcé (DC / DDI),
   existence d'une renonciation à pension dans la convention, état de besoin du créancier,
   faute grave du créancier invoquée, durée du mariage, revenu mensuel net de chaque
   ex-époux, et l'existence d'une dégradation économique significative liée aux choix du
   mariage.
2. Le `ContributionConjointBeCalculator` applique les règles du CC art. 301 :
   (a) en divorce par consentement mutuel (DC), la pension relève de la convention des
   parties — l'outil renvoie la convention sans imposer de barème ; (b) en divorce pour
   désunion irrémédiable (DDI), une pension peut être accordée par le TF à l'époux dans le
   besoin, sauf faute grave de celui-ci ; (c) la durée de la pension ne peut excéder la
   durée du mariage (CC art. 301 §4) ; (d) le montant ne peut couvrir que l'état de besoin
   et reste plafonné au tiers des revenus du débiteur (CC art. 301 §3).
3. Le calculateur produit un verdict (`PENSION_DUE` / `PENSION_NON_DUE` /
   `PENSION_CONVENTIONNELLE` / `DONNEES_INSUFFISANTES`), la durée maximale légale en mois,
   un montant mensuel indicatif, les motifs d'exclusion éventuels, les bases juridiques et
   des messages d'aide.
4. Le résultat est persisté par dossier (un seul résultat courant, écrasé au recalcul)
   et renvoyé.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Corps de requête absent | Message d'erreur explicite | 400 |
| Champ booléen / enum obligatoire absent | Message d'erreur explicite | 400 |
| Durée du mariage négative ou > 80 ans | Message d'erreur explicite | 400 |
| Revenu négatif | Message d'erreur explicite | 400 |
| Commentaire > 1000 caractères | Message d'erreur explicite | 400 |
| Non authentifié | — | 401 |
| Dossier d'un autre workspace | Accès refusé | 403 |
| Dossier inexistant | — | 404 |
| `GET` sans calcul préalable | — | 404 |
| Dossier dont le `legalDomain` n'est pas `DROIT_FAMILLE` | Message explicite | 422 |
| Workspace dont le `country` n'est pas `BELGIQUE` (outil BE-only) | Message explicite | 422 |

---

## Contrat API (FIGÉ — importé par SF-217-09)

### POST `/api/v1/case-files/{caseFileId}/contribution-conjoint-be`

Body `ContributionConjointBeRequest` :
```json
{
  "typeDivorce": "DDI",
  "renonciationPensionConvention": false,
  "creancierEnEtatDeBesoin": true,
  "fauteGraveCreancier": false,
  "dureeMariageAnnees": 18,
  "revenuMensuelCreancier": 900.00,
  "revenuMensuelDebiteur": 3600.00,
  "degradationEconomiqueLieeAuMariage": true,
  "commentaire": null
}
```
- `typeDivorce` : enum obligatoire (`DC` / `DDI`).
- `renonciationPensionConvention` : boolean obligatoire.
- `creancierEnEtatDeBesoin` : boolean obligatoire.
- `fauteGraveCreancier` : boolean obligatoire.
- `dureeMariageAnnees` : entier obligatoire, 0–80.
- `revenuMensuelCreancier` / `revenuMensuelDebiteur` : décimaux obligatoires, ≥ 0.
- `degradationEconomiqueLieeAuMariage` : boolean obligatoire.
- `commentaire` : nullable, max 1000 caractères.
- Le pays est dérivé du workspace côté service.

Réponse `200` — `ContributionConjointBeResponse` : **ré-expose l'intégralité du body**
(snapshot) **+** les champs calculés.
```json
{
  "caseFileId": "uuid",
  "typeDivorce": "DDI",
  "renonciationPensionConvention": false,
  "creancierEnEtatDeBesoin": true,
  "fauteGraveCreancier": false,
  "dureeMariageAnnees": 18,
  "revenuMensuelCreancier": 900.00,
  "revenuMensuelDebiteur": 3600.00,
  "degradationEconomiqueLieeAuMariage": true,
  "commentaire": null,
  "verdict": "PENSION_DUE",
  "dureeMaximaleMois": 216,
  "montantMensuelIndicatif": 900.00,
  "plafondTiersRevenusDebiteur": 1200.00,
  "motifsExclusion": [],
  "detailCalcul": [
    "Divorce pour désunion irrémédiable : une pension peut être accordée par le Tribunal de la famille (CC art. 301 §1).",
    "Durée maximale légale de la pension = durée du mariage : 18 ans = 216 mois (CC art. 301 §4).",
    "Besoin du créancier estimé : écart de revenus 3600,00 € − 900,00 € = 2700,00 €.",
    "Montant indicatif retenu = min(besoin, plafond du tiers des revenus du débiteur 1200,00 €) : 900,00 €."
  ],
  "basesJuridiques": [
    "CC art. 301 §1 (pension alimentaire après divorce pour désunion irrémédiable)",
    "CC art. 301 §2 (refus de pension en cas de faute grave du créancier)",
    "CC art. 301 §3 (montant — couverture de l'état de besoin)",
    "CC art. 301 §4 (durée — ne peut excéder la durée du mariage)"
  ],
  "messages": [
    "Estimation indicative — le Tribunal de la famille apprécie souverainement le montant et la durée selon les besoins et les facultés des parties."
  ],
  "country": "BELGIQUE",
  "calculatedAt": "2026-05-18T10:00:00Z"
}
```

### GET `/api/v1/case-files/{caseFileId}/contribution-conjoint-be`
- `200` : dernier résultat (même structure, inputs inclus → formulaire ré-éditable).
  `404` si jamais calculé. `403` / `401` / `422` idem POST.

### Enum `verdict` (`ContributionConjointBeVerdict`)
- `PENSION_DUE` — divorce DDI, créancier dans le besoin, pas de faute grave : une pension
  peut être accordée par le TF.
- `PENSION_NON_DUE` — un motif d'exclusion s'applique (faute grave du créancier, créancier
  hors état de besoin, renonciation conventionnelle).
- `PENSION_CONVENTIONNELLE` — divorce par consentement mutuel : la pension relève de la
  convention préalable des parties, l'outil ne fixe pas de barème.
- `DONNEES_INSUFFISANTES` — revenus des deux ex-époux nuls : le besoin et le plafond ne
  peuvent pas être estimés.

### Enum `typeDivorce` (`TypeDivorceBe`)
`DC` (divorce par consentement mutuel — CJ art. 1287+) ·
`DDI` (divorce pour désunion irrémédiable — CC art. 229).

### Enum `code` d'un motif d'exclusion (`MotifExclusionPensionBe`)
`FAUTE_GRAVE_CREANCIER` · `CREANCIER_HORS_BESOIN` · `RENONCIATION_CONVENTIONNELLE`.

---

## Règles d'analyse — CC art. 301

> ⚠️ **Validation juridique requise** : CC art. 301 (§§1 à 4 — pension alimentaire après
> divorce, faute grave, montant, durée) reflète l'état du droit connu du modèle et est
> **à valider par un avocat belge avant mise en production**. Le plafond du tiers des
> revenus du débiteur est une règle jurisprudentielle de référence, à confirmer. Le
> contenu juridique est centralisé dans le Calculator (source unique de vérité).

### Détermination du verdict (figée dans le Calculator)

1. `typeDivorce = DC` → `PENSION_CONVENTIONNELLE` (la pension relève de la convention
   préalable — CJ art. 1287 ; l'outil n'impose pas de barème).
2. `typeDivorce = DDI` :
   - `renonciationPensionConvention = true` → `PENSION_NON_DUE`, motif
     `RENONCIATION_CONVENTIONNELLE`.
   - `fauteGraveCreancier = true` → `PENSION_NON_DUE`, motif `FAUTE_GRAVE_CREANCIER`
     (CC art. 301 §2).
   - `creancierEnEtatDeBesoin = false` → `PENSION_NON_DUE`, motif `CREANCIER_HORS_BESOIN`.
   - Sinon, si `revenuMensuelCreancier = 0` ET `revenuMensuelDebiteur = 0` →
     `DONNEES_INSUFFISANTES`.
   - Sinon → `PENSION_DUE`.

### Calcul de la durée et du montant (verdict `PENSION_DUE`)

- **Durée maximale** = `dureeMariageAnnees × 12` mois (CC art. 301 §4 — la pension ne peut
  excéder la durée du mariage).
- **Besoin estimé** = `max(0, revenuMensuelDebiteur − revenuMensuelCreancier)`.
- **Plafond du tiers** = `revenuMensuelDebiteur / 3` (règle jurisprudentielle de référence).
- **Montant mensuel indicatif** = `min(besoin estimé, plafond du tiers)`, arrondi à 2
  décimales (`RoundingMode.HALF_UP`).
- Un message signale que `degradationEconomiqueLieeAuMariage` renforce l'argumentaire
  mais n'est pas un critère de calcul autonome.

Pour les verdicts autres que `PENSION_DUE`, `dureeMaximaleMois` et `montantMensuelIndicatif`
valent 0 (et `plafondTiersRevenusDebiteur` est tout de même calculé pour information).

---

## Conformité F-IA-04
- [x] **Non applicable au sens strict** — SF backend pure. Conformité F-IA-04 et seed
  `decision_tool_visibility_rules` portés par SF-217-09 frontend (couplé TOOL_REGISTRY).

---

## Critères d'acceptation

- [ ] `POST` calcule et persiste un résultat ; recalcul écrase le précédent (upsert 1:1).
- [ ] Divorce DC → `PENSION_CONVENTIONNELLE` (pas de barème imposé).
- [ ] Divorce DDI + besoin + pas de faute → `PENSION_DUE`.
- [ ] Renonciation conventionnelle / faute grave / créancier hors besoin → `PENSION_NON_DUE`
      avec le motif d'exclusion correspondant.
- [ ] Durée max = durée du mariage × 12 mois.
- [ ] Montant = min(écart de revenus, tiers des revenus du débiteur), arrondi 2 décimales.
- [ ] Revenus des deux ex-époux nuls (DDI, besoin, pas de faute) → `DONNEES_INSUFFISANTES`.
- [ ] La réponse `POST` / `GET` ré-expose l'intégralité des inputs (formulaire ré-éditable).
- [ ] `GET` renvoie le dernier résultat ou 404.
- [ ] `400` (corps absent, enum/booléen absent, durée invalide, revenu négatif) / `403` /
      `404` / `422` (domaine ≠ famille, pays ≠ Belgique) / `401`.
- [ ] Isolation workspace testée.

---

## Périmètre

### Hors scope
- Frontend (SF-217-09).
- Révision de la pension post-divorce (CJ art. 1288bis) — outil distinct reporté F-223.
- Pension alimentaire entre époux *pendant* l'instance (mesures provisoires — couvert F-211
  `tribunal-famille-be-mesures-prov`).
- Pension du conjoint survivant en succession — situation distincte, couverte par la
  Vague 3 (`succession-be-devolution-reserve`).
- Réutilisation du Calculator FR `F-FA-01` (prestation compensatoire FR — mécanisme
  juridiquement distinct : capital forfaitaire FR vs pension révisable BE).
- Pré-fill IA depuis l'analyse (aucun flag pivot dédié extrait par le pipeline V1 —
  documenté `PREFILL_COUNT_ALWAYS_ZERO` côté SF-217-09).

---

## Technique

### Endpoints
| Méthode | URL | Auth | Rôle |
|---------|-----|------|------|
| POST | `/api/v1/case-files/{caseFileId}/contribution-conjoint-be` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/contribution-conjoint-be` | Oui | MEMBER |

### Tables impactées
| Table | Opération | Notes |
|-------|-----------|-------|
| `contribution_conjoint_be_analyses` | CREATE + INSERT/UPDATE/SELECT | id (UUID), case_file_id (FK UNIQUE), country (VARCHAR), snapshot_data (TEXT JSON), created_at, updated_at |
| `decision_tool_visibility_rules` | INSERT (seed) | porté par SF-217-09 |

### Migration Liquibase
- [x] Oui — `241-create-contribution-conjoint-be-analyses.xml` (table seule).

### Classes backend (pattern `ProcedureNulliteLicenciement*` — snapshot JSON)
`ContributionConjointBeCalculator` (static), `ContributionConjointBeInput`,
`ContributionConjointBeResult`, `ContributionConjointBeRequest`,
`ContributionConjointBeResponse`, `ContributionConjointBeAnalysis` (@Entity),
`ContributionConjointBeRepository`, `ContributionConjointBeService`,
`ContributionConjointBeController`.

---

## Plan de test

### Tests unitaires (Calculator)
- [ ] Divorce DC → `PENSION_CONVENTIONNELLE`, durée et montant à 0.
- [ ] Divorce DDI + besoin + pas de faute, revenus 900/3600 → `PENSION_DUE`,
      durée = mariage × 12, montant > 0 et ≤ plafond du tiers.
- [ ] Divorce DDI + faute grave du créancier → `PENSION_NON_DUE`, motif `FAUTE_GRAVE_CREANCIER`.
- [ ] Divorce DDI + renonciation conventionnelle → `PENSION_NON_DUE`, motif `RENONCIATION_CONVENTIONNELLE`.
- [ ] Divorce DDI + créancier hors état de besoin → `PENSION_NON_DUE`, motif `CREANCIER_HORS_BESOIN`.
- [ ] Divorce DDI + besoin + revenus des deux ex-époux nuls → `DONNEES_INSUFFISANTES`.
- [ ] Montant plafonné au tiers des revenus du débiteur quand l'écart de revenus est supérieur.
- [ ] Durée max = durée du mariage × 12.
- [ ] Arrondi à 2 décimales vérifié.

### Tests d'intégration (Controller)
- [ ] `POST` → 200 + persistance ; recalcul écrase.
- [ ] `GET` → 200 / 404 si jamais calculé.
- [ ] `400` (corps absent, durée = -1, revenu négatif) / `403` / `404` / `422`
      (domaine ≠ famille, pays ≠ Belgique) / `401`.

### Isolation workspace
- [x] Applicable — un utilisateur du workspace A ne peut pas calculer/lire le résultat
  d'un dossier du workspace B.

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Outil décisionnel métier** — `contribution-conjoint-be` est un nouvel outil
  décisionnel. Scan fait dans `SF-217-00-coherence.md` : aucun outil BE existant ne couvre
  la pension alimentaire entre ex-époux belge (F-FA-01 prestation compensatoire est
  FR-only, capital forfaitaire, masqué en BE). La pension belge (CC art. 301, révisable,
  plafonnée en durée) est juridiquement distincte de la prestation compensatoire FR.
  `contribution-conjoint-be` est une **situation métier distincte** — un outil = une
  situation.
- [x] Aucune autre préoccupation transversale.

### Smoke tests E2E
- [x] Aucun — feature additive.

---

## Dépendances
- Aucune SF bloquante. SF-217-09 (frontend) importe le contrat API. Parallélisable.

---

## Notes et décisions
- Persistance par snapshot JSON (`snapshot_data`) — aligné Vague 1 F-217.
- Aucune réutilisation du Calculator FR : la pension belge (CC art. 301) diffère de la
  prestation compensatoire française (`feedback_belgique_never_forget`).
- Le plafond du tiers des revenus du débiteur est une règle jurisprudentielle de référence :
  il est centralisé dans le Calculator et **signalé pour validation par un avocat belge**.
  Le résultat est qualifié d'« estimation indicative » — le TF apprécie souverainement.
</content>
