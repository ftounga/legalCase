# Mini-spec — F-217 / SF-217-11 — Backend : dévolution légale + réserve héréditaire belge

## Identifiant
`F-217 / SF-217-11`

## Feature parente
`F-217` — P2 Famille BE — outils décisionnels de fréquence haute (Vague 3 — Successions / protection / international)

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-217-11-succession-be-devolution-reserve`

---

## Objectif

Fournir un outil décisionnel backend qui établit la dévolution légale d'une succession
belge (ordre des héritiers, parts du conjoint survivant) et quantifie la réserve
héréditaire post-réforme du 31/07/2017 (réserve globale 1/2, quelle que soit le nombre
d'enfants — différence majeure avec la France), à partir des éléments du dossier.

---

## Comportement attendu

### Cas nominal

1. L'avocat soumet les éléments de la succession : date du décès, état civil du défunt
   (marié / cohabitant légal / célibataire / divorcé / veuf), régime matrimonial du
   défunt (s'il était marié), présence et nombre d'enfants vivants (ou prédécédés laissant
   des descendants), existence de parents / collatéraux privilégiés (frères et sœurs et
   leurs descendants) en l'absence de descendants, donations ou legs déjà consentis (pour
   le calcul de la quotité disponible).
2. Le `SuccessionBeDevolutionReserveCalculator` applique l'arbre de dévolution du
   **Livre 4 du Code civil belge réformé** (loi du 31/07/2017, en vigueur 01/09/2018) :
   (a) détermine l'ordre des héritiers réservataires et non-réservataires ; (b) calcule
   les parts en pleine propriété / usufruit du conjoint survivant ; (c) calcule la
   **réserve globale = 1/2 de la masse**, indépendamment du nombre d'enfants (CC art. 913
   nouveau — à vérifier) ; (d) déduit la **quotité disponible = 1/2** et compare avec
   les libéralités déjà consenties pour signaler un éventuel dépassement.
3. Le calculateur produit un verdict (`DEVOLUTION_ETABLIE` / `RESERVE_RESPECTEE` /
   `QUOTITE_DEPASSEE` / `QUALIFICATION_INCOMPLETE`), la liste des héritiers avec leur
   quote-part (réserve + part libre), le montant de la réserve globale, la quotité
   disponible, les bases juridiques mobilisées et des messages d'aide.
4. Le résultat est persisté par dossier (un seul résultat courant par dossier, écrasé
   au recalcul) et renvoyé.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Corps de requête absent | Message d'erreur explicite | 400 |
| Date de décès absente / future / mal formée | Message d'erreur explicite | 400 |
| Nombre d'enfants négatif ou > 20 | Message d'erreur explicite | 400 |
| Montant de la masse successorale négatif | Message d'erreur explicite | 400 |
| Commentaire > 1000 caractères | Message d'erreur explicite | 400 |
| Non authentifié | — | 401 |
| Dossier d'un autre workspace | Accès refusé | 403 |
| Dossier inexistant | — | 404 |
| `GET` sans calcul préalable | — | 404 |
| Dossier dont le `legalDomain` n'est pas `DROIT_FAMILLE` | Message explicite | 422 |
| Workspace dont le `country` n'est pas `BELGIQUE` (outil BE-only) | Message explicite | 422 |

---

## Contrat API (FIGÉ — importé par SF-217-13)

### POST `/api/v1/case-files/{caseFileId}/succession-be-devolution-reserve`

Body `SuccessionBeDevolutionReserveRequest` :
```json
{
  "dateDeces": "2025-03-12",
  "etatCivilDefunt": "MARIE",
  "regimeMatrimonialDefunt": "COMMUNAUTE_LEGALE",
  "nombreEnfantsVivants": 2,
  "nombreEnfantsPredecedesAvecDescendants": 0,
  "presenceParentsVivants": false,
  "presenceFreresSoeursOuDescendants": false,
  "masseSuccessoraleEur": 400000,
  "libertesConsentiesEur": 100000,
  "commentaire": null
}
```
- `dateDeces` : `yyyy-MM-dd`, obligatoire, non future, postérieure au 01/09/2018 pour
  application directe de la réforme (date antérieure → message d'aide « droit antérieur
  applicable » mais calcul exécuté selon nouvelles règles avec message explicite —
  arbitrage produit, V1).
- `etatCivilDefunt` : enum obligatoire.
- `regimeMatrimonialDefunt` : enum obligatoire si `etatCivilDefunt = MARIE`, nullable
  sinon.
- `nombreEnfantsVivants` : entier obligatoire, ≥ 0, ≤ 20.
- `nombreEnfantsPredecedesAvecDescendants` : entier obligatoire, ≥ 0, ≤ 20 — descendants
  venant par représentation (CC art. 740 — à vérifier).
- `presenceParentsVivants`, `presenceFreresSoeursOuDescendants` : booleans obligatoires
  (utilisés uniquement en l'absence de descendants directs).
- `masseSuccessoraleEur` : `BigDecimal` ≥ 0, obligatoire (montant de la masse à partager,
  hors récompenses régime matrimonial — celles-ci sont l'objet de SF-217-01 / `recompenses-be`).
- `libertesConsentiesEur` : `BigDecimal` ≥ 0, obligatoire (total des donations entre vifs
  et legs déjà consentis, pour comparer à la quotité disponible).
- `commentaire` : nullable, max 1000 caractères.
- Le pays est dérivé du workspace côté service.

Réponse `200` — `SuccessionBeDevolutionReserveResponse` : **ré-expose l'intégralité du
body** (snapshot pour pré-remplissage / ré-édition du formulaire) **+** les champs
calculés ci-dessous.
```json
{
  "caseFileId": "uuid",
  "dateDeces": "2025-03-12",
  "etatCivilDefunt": "MARIE",
  "regimeMatrimonialDefunt": "COMMUNAUTE_LEGALE",
  "nombreEnfantsVivants": 2,
  "nombreEnfantsPredecedesAvecDescendants": 0,
  "presenceParentsVivants": false,
  "presenceFreresSoeursOuDescendants": false,
  "masseSuccessoraleEur": 400000,
  "libertesConsentiesEur": 100000,
  "commentaire": null,
  "verdict": "RESERVE_RESPECTEE",
  "heritiers": [
    {
      "code": "CONJOINT_SURVIVANT",
      "libelle": "Conjoint survivant",
      "quotePartReserveFraction": "0",
      "quotePartReserveEur": 0,
      "quotePartGlobaleFraction": "USUFRUIT_TOTAL",
      "quotePartGlobaleEur": null,
      "fondement": "CC art. 745bis (à vérifier) — usufruit universel du conjoint survivant en présence de descendants"
    },
    {
      "code": "ENFANT",
      "libelle": "Enfant n°1 (nue-propriété)",
      "quotePartReserveFraction": "1/4",
      "quotePartReserveEur": 100000,
      "quotePartGlobaleFraction": "1/2",
      "quotePartGlobaleEur": 200000,
      "fondement": "CC art. 913 (à vérifier) — réserve globale 1/2, partagée par tête entre descendants"
    },
    {
      "code": "ENFANT",
      "libelle": "Enfant n°2 (nue-propriété)",
      "quotePartReserveFraction": "1/4",
      "quotePartReserveEur": 100000,
      "quotePartGlobaleFraction": "1/2",
      "quotePartGlobaleEur": 200000,
      "fondement": "CC art. 913 (à vérifier)"
    }
  ],
  "reserveGlobaleEur": 200000,
  "reserveGlobaleFraction": "1/2",
  "quotiteDisponibleEur": 200000,
  "quotiteDisponibleFraction": "1/2",
  "libertesConsentiesEur": 100000,
  "depassementQuotiteEur": 0,
  "basesJuridiques": [
    "CC art. 913 nouveau (à vérifier) — réserve globale 1/2 quelle que soit le nombre d'enfants (réforme loi 31/07/2017)",
    "CC art. 745bis (à vérifier) — usufruit du conjoint survivant en présence de descendants",
    "CC art. 740 (à vérifier) — représentation"
  ],
  "messages": [
    "Réserve globale = 1/2 (200 000 €), quotité disponible = 1/2 (200 000 €). Libéralités consenties (100 000 €) ≤ quotité disponible : pas de réduction.",
    "Conjoint survivant : usufruit universel sur la totalité de la succession (CC art. 745bis — à vérifier). Les enfants reçoivent la nue-propriété de leur quote-part."
  ],
  "country": "BELGIQUE",
  "calculatedAt": "2026-05-20T10:00:00Z"
}
```

### GET `/api/v1/case-files/{caseFileId}/succession-be-devolution-reserve`
- `200` : dernier résultat (même structure, inputs inclus → formulaire ré-éditable).
  `404` si jamais calculé. `403` / `401` / `422` idem POST.

### Enum `verdict` (`SuccessionBeDevolutionReserveVerdict`)
- `DEVOLUTION_ETABLIE` — situation sans descendants ni conjoint : dévolution aux
  ascendants / collatéraux ; pas de réserve à proprement parler (les ascendants n'ont
  plus de réserve depuis la réforme 2017 — à vérifier), tout est quotité disponible.
- `RESERVE_RESPECTEE` — descendants ou conjoint réservataire : la réserve est établie
  ET les libéralités déjà consenties (`libertesConsentiesEur`) sont ≤ quotité disponible.
- `QUOTITE_DEPASSEE` — descendants ou conjoint réservataire ET libéralités >
  quotité disponible : risque de réduction (CC art. 920+ — à vérifier).
- `QUALIFICATION_INCOMPLETE` — combinaison d'inputs ne permettant pas de trancher
  (ex : état civil `MARIE` sans régime matrimonial).

### Enum `etatCivilDefunt` (`EtatCivilDefuntBe`)
`MARIE` · `COHABITANT_LEGAL` · `CELIBATAIRE` · `DIVORCE` · `VEUF`.

### Enum `regimeMatrimonialDefunt` (`RegimeMatrimonialDefuntBe`)
`COMMUNAUTE_LEGALE` · `SEPARATION_BIENS` · `COMMUNAUTE_UNIVERSELLE` ·
`PARTICIPATION_ACQUETS` · `AUTRE`.

### Enum `code` d'un héritier (`HeritierCodeBe`)
`CONJOINT_SURVIVANT` · `COHABITANT_LEGAL_SURVIVANT` · `ENFANT` · `DESCENDANT_REPRESENTATION`
· `PARENT` · `FRERE_SOEUR` · `DESCENDANT_FRERE_SOEUR` · `ETAT` (déshérence).

---

## Règles de dévolution et de réserve analysées

> ⚠️ **Validation juridique requise** : la réforme du droit successoral belge
> (loi du 31/07/2017, en vigueur 01/09/2018) a modifié en profondeur la réserve
> (désormais 1/2 quelle que soit le nombre d'enfants — différence majeure FR), les droits
> des ascendants (suppression de la réserve des ascendants — à vérifier), le calcul de la
> quotité disponible et la fluctuation de valeur des biens donnés. Les références
> (`CC art. 913`, `745bis`, `740`, `843+`, `920+`) reflètent l'état du droit connu
> du modèle et sont **à valider par un avocat belge avant mise en production**.
> Les droits du cohabitant légal survivant (CC art. 1477 — à vérifier) sont plus limités
> que ceux du conjoint et doivent être confirmés (usufruit limité au logement principal
> + meubles meublants). Le contenu juridique est centralisé dans le Calculator (source
> unique de vérité).

### Détermination du verdict (figée dans le Calculator)

1. `etatCivilDefunt = MARIE` ET `regimeMatrimonialDefunt = null` →
   `QUALIFICATION_INCOMPLETE`.
2. Présence de descendants (vivants + représentés) OU conjoint survivant /
   cohabitant légal survivant → calcul de la réserve. Si `libertesConsentiesEur` >
   `quotiteDisponibleEur` → `QUOTITE_DEPASSEE`, sinon `RESERVE_RESPECTEE`.
3. Aucun descendant ni conjoint : dévolution aux ascendants / collatéraux →
   `DEVOLUTION_ETABLIE` (pas de quote-part de réserve calculée, la quotité disponible
   couvre toute la masse).

### Calcul de la réserve (post-réforme 2017)

| Configuration | Réserve globale | Fondement (à valider) |
|---------------|-----------------|------------------------|
| Au moins 1 descendant (vivant ou représenté) | **1/2** de la masse, partagée par tête entre descendants (représentation par souche) | CC art. 913 nouveau |
| Aucun descendant, conjoint survivant en concours avec collatéraux | Conjoint = usufruit universel sur le patrimoine commun ET nue-propriété du logement familial (à vérifier) | CC art. 745bis nouveau |
| Aucun descendant, aucun conjoint | Pas de réserve — toute la masse est disponible (suppression de la réserve des ascendants par la réforme 2017 — à vérifier) | CC Livre 4 réformé |

### Droits du conjoint survivant en présence de descendants

| Situation | Droits |
|-----------|--------|
| Descendants communs et/ou non communs | Usufruit universel sur toute la succession ; les descendants reçoivent la nue-propriété de leur quote-part (CC art. 745bis — à vérifier) |
| Aucun descendant | Conjoint = pleine propriété sur la part successorale (en concours avec ascendants / collatéraux selon les cas) |

### Cohabitant légal survivant

- Droits **plus limités** que ceux du conjoint marié : usufruit limité au logement
  familial occupé en commun + meubles meublants (CC art. 1477 — à vérifier).
- Le cohabitant légal n'est **pas réservataire** : il ne dispose pas d'une réserve
  héréditaire opposable aux libéralités du de cujus.

### Vérification du dépassement de quotité

- `quotiteDisponibleEur = masseSuccessoraleEur - reserveGlobaleEur`.
- Si `libertesConsentiesEur > quotiteDisponibleEur` → verdict `QUOTITE_DEPASSEE`,
  `depassementQuotiteEur = libertesConsentiesEur - quotiteDisponibleEur` (montant
  potentiellement sujet à réduction — CC art. 920+ — à vérifier).
- Sinon `depassementQuotiteEur = 0`.

---

## Conformité F-IA-04
- [x] **Non applicable au sens strict** — SF backend pure. Conformité F-IA-04
  (TOOL_REGISTRY, pré-fill, F-IA-03, gate `workspaceCountry`) et seed
  `decision_tool_visibility_rules` portés par SF-217-13 frontend (bundle Vague 3 —
  successions).

---

## Champs IA à extraire (pré-remplissage IA — V1)

| Champ | Source backend potentielle | Statut V1 |
|-------|----------------------------|-----------|
| `dateDeces` | Détection date de décès dans les documents | Aspirationnel — non extrait V1 |
| `nombreEnfantsVivants` | Détection enfants du défunt | Aspirationnel — non extrait V1 |
| `etatCivilDefunt` | Détection état civil du défunt | Aspirationnel — non extrait V1 |
| `masseSuccessoraleEur` | Détection patrimoine successoral | Aspirationnel — non extrait V1 |

**V1 : `PREFILL_COUNT_ALWAYS_ZERO = true`** côté SF-217-13 — aucun flag pivot dédié
n'est extrait par le pipeline IA pour les successions BE (cohérent avec les vagues 1+2
F-217). Extension IA = SF ultérieure si signal terrain (à brancher éventuellement à un
`succession_be_detection` du pipeline V2). Pas de nouveau champ ajouté à
`FamilleExtractedData` dans cette SF.

---

## Critères d'acceptation

- [ ] `POST` calcule et persiste un résultat ; recalcul écrase le précédent (upsert 1:1
      par dossier).
- [ ] Configuration descendants seuls : réserve = 1/2, par tête entre descendants ;
      verdict `RESERVE_RESPECTEE` si libéralités ≤ quotité disponible.
- [ ] Configuration descendants + conjoint : conjoint = usufruit universel,
      descendants = nue-propriété de leur quote-part.
- [ ] Configuration sans descendants ni conjoint : verdict `DEVOLUTION_ETABLIE`,
      pas de réserve, toute la masse en quotité disponible.
- [ ] Libéralités > quotité disponible → verdict `QUOTITE_DEPASSEE`, montant
      `depassementQuotiteEur` correct.
- [ ] État civil `MARIE` sans régime matrimonial → `QUALIFICATION_INCOMPLETE`.
- [ ] Calcul indépendant du nombre d'enfants : la réserve reste 1/2 que l'on ait 1 ou
      6 enfants (différence FR vérifiée par test).
- [ ] La réponse `POST` / `GET` ré-expose l'intégralité des inputs (formulaire
      ré-éditable — leçon F-DT-36).
- [ ] `GET` renvoie le dernier résultat ou 404.
- [ ] `400` (corps absent, date future, nombre négatif, masse négative, commentaire
      trop long) / `403` workspace différent / `404` dossier inexistant /
      `422` domaine ≠ `DROIT_FAMILLE` ou pays ≠ `BELGIQUE` / `401` non authentifié.
- [ ] Isolation workspace testée.

---

## Périmètre

### Hors scope
- Frontend (SF-217-13 — bundle Vague 3 successions).
- Calcul détaillé du rapport à succession (`succession-be-rapport` — audit F-191 § 3.6,
  reporté).
- Calcul des droits de succession régionaux Bruxelles / Wallonie / Flandre
  (`succession-be-droits-succession-regionaux` — reporté).
- Validité du testament (`succession-be-testament-validite` — reporté).
- Pacte successoral (couvert par F-211 `pacte-successoral-be-2018` — outil distinct).
- Succession internationale (Règlement UE 650/2012 — `succession-be-internationale`
  reporté F-223).
- Réduction effective des libéralités excessives (l'outil signale le dépassement mais
  ne génère pas l'action en réduction — outil dédié potentiel `succession-be-reduction`,
  reporté).
- Pré-fill IA depuis l'analyse (documenté `PREFILL_COUNT_ALWAYS_ZERO` côté SF-217-13).
- Seed `decision_tool_visibility_rules` (porté par SF-217-13).
- Réutilisation des Calculators FR (réserve FR 1/2-2/3-3/4 — mécanisme distinct, pas
  réutilisable).

---

## Technique

### Endpoints
| Méthode | URL | Auth | Rôle |
|---------|-----|------|------|
| POST | `/api/v1/case-files/{caseFileId}/succession-be-devolution-reserve` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/succession-be-devolution-reserve` | Oui | MEMBER |

### Tables impactées
| Table | Opération | Notes |
|-------|-----------|-------|
| `succession_be_devolution_reserve_analyses` | CREATE + INSERT/UPDATE/SELECT | id (UUID), case_file_id (FK UNIQUE), country (VARCHAR), snapshot_data (TEXT JSON — inputs + résultat calculé), created_at, updated_at |
| `decision_tool_visibility_rules` | INSERT (seed) | porté par SF-217-13 (bundle Vague 3 successions) |

### Migration Liquibase
- [x] Oui — `271-create-succession-be-devolution-reserve-analyses.xml` (table seule ;
  le seed `decision_tool_visibility_rules` est porté par SF-217-13). Numéro `271` =
  prochain libre à la rédaction (master HEAD `270`). À renuméroter si conflit au merge.

### Classes backend (pattern `RegimeCommunauteLegaleBe*` / `AutoriteParentaleBe*`)
`SuccessionBeDevolutionReserveCalculator` (static), `SuccessionBeDevolutionReserveInput`,
`SuccessionBeDevolutionReserveResult`, `SuccessionBeDevolutionReserveRequest`,
`SuccessionBeDevolutionReserveResponse`, `SuccessionBeDevolutionReserveAnalysis`
(@Entity), `SuccessionBeDevolutionReserveRepository`,
`SuccessionBeDevolutionReserveService`, `SuccessionBeDevolutionReserveController`.

---

## Plan de test

### Tests unitaires (Calculator)
- [ ] Défunt marié, 2 enfants, masse 400 000 €, libéralités 0 €, conjoint vivant →
      réserve 200 000 €, conjoint usufruit, enfants nue-propriété 200 000 € chacun.
- [ ] Défunt marié, 6 enfants, masse 600 000 € → réserve **1/2 = 300 000 €** (test de
      la différence avec FR : la réserve ne passe pas à 3/4 à 3 enfants — point central
      de la réforme 2017).
- [ ] Défunt veuf, 1 enfant, masse 100 000 €, libéralités 60 000 € → quotité disponible
      50 000 €, dépassement 10 000 €, verdict `QUOTITE_DEPASSEE`.
- [ ] Défunt célibataire sans descendants, parents vivants, masse 100 000 € →
      `DEVOLUTION_ETABLIE`, pas de réserve.
- [ ] Défunt cohabitant légal, 1 enfant → réserve descendants 1/2, cohabitant droits
      limités (logement + meubles).
- [ ] Représentation : 1 enfant prédécédé laissant 2 descendants → 2 souches, chaque
      souche = 1/2 de la part de l'enfant prédécédé.
- [ ] Défunt marié sans régime renseigné → `QUALIFICATION_INCOMPLETE`.
- [ ] Aucun héritier de rang utile → `DEVOLUTION_ETABLIE` avec un héritier `ETAT`
      (déshérence).

### Tests d'intégration (Controller)
- [ ] `POST` → 200 + persistance ; recalcul écrase.
- [ ] `GET` → 200 / 404 si jamais calculé.
- [ ] `400` (corps absent, date future, masse négative, commentaire trop long) /
      `403` / `404` / `422` (domaine ≠ famille, pays ≠ Belgique) / `401`.

### Isolation workspace
- [x] Applicable — un utilisateur du workspace A ne peut pas calculer/lire le résultat
  d'un dossier du workspace B.

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Outil décisionnel métier** — `succession-be-devolution-reserve` est un nouvel
  outil décisionnel. Scan des outils successions / dévolution / réserve fait dans
  `SF-217-00-coherence.md` : aucun outil BE existant ne couvre la dévolution / réserve
  belge post-réforme 2017 (F-FA-24-devolution-legale et F-FA-24-reserve-heriditaire sont
  FR-only, masqués en BE — réserves FR 1/2-2/3-3/4 fondamentalement distinctes de la
  réserve BE 1/2 fixe). `succession-be-devolution-reserve` = **une situation métier
  distincte** — un outil = une situation.
- [x] Aucune autre préoccupation transversale (auth/workspace/plans/navigation non
  modifiés).

### Smoke tests E2E
- [x] Aucun — feature additive (nouvel endpoint).

---

## Dépendances
- Aucune SF bloquante. SF-217-13 (frontend bundle Vague 3 successions) importe le
  contrat API ci-dessus. Dev backend/frontend parallélisable (contrat figé).
- Indépendante de SF-217-12 (acceptation/renonciation) — situations métier distinctes.

---

## Notes et décisions
- Persistance par snapshot JSON (`snapshot_data`) — pattern Vague 1+2 F-217
  (`RegimeCommunauteLegaleBeAnalysis`, `AutoriteParentaleBeAnalysis`), évite une
  colonne par champ et supporte la liste de longueur variable des héritiers.
- Aucune réutilisation du Calculator FR : la réserve héréditaire belge post-réforme
  2017 est désormais **fixe à 1/2 quelle que soit le nombre d'enfants**, alors que
  la France conserve 1/2-2/3-3/4 selon le nombre d'enfants. Ce sont des mécanismes
  juridiquement distincts — outil bâti depuis les sources belges
  (`feedback_belgique_never_forget`).
- Les fractions sont exprimées en chaîne (`"1/2"`, `"1/4"`) pour la lisibilité dans la
  réponse JSON ; les montants en euros sont calculés en `BigDecimal` côté serveur,
  exposés en `long` (centimes ? non, euros entiers — montant arrondi côté serveur,
  documenté).
- Pour les successions ouvertes avant le 01/09/2018, le droit antérieur s'applique
  (réserves 1/2-2/3-3/4 et droits ascendants) — l'outil V1 émet un message d'avertissement
  mais calcule selon la réforme. Arbitrage produit V1 (un outil dédié au droit antérieur
  serait reporté à F-223 si signal terrain). À mentionner explicitement dans les
  `messages` du résultat.
- Le contenu juridique (articles CC du Livre 4 réformé, fractions, fondements) est
  centralisé dans le Calculator et signalé pour validation par un avocat belge avant
  prod. Articles tagués `(à vérifier)` cohérents avec l'audit F-191.
