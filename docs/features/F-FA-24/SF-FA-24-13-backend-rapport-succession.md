# Mini-spec — F-FA-24 / SF-FA-24-13 Backend rapport à succession (art. 843-863 + 919 Cciv)

## Identifiant

`F-FA-24 / SF-FA-24-13`

## Feature parente

`F-FA-24` — Droit des successions (chantier ~12-14 SF — déjà livrées : SF-01/02 dévolution, SF-03/04 testament, SF-05/06 donation, SF-07/08 réserve héréditaire).

## Statut

`in-progress`

## Date de création

2026-04-26

## Branche Git

`feat/SF-FA-24-13-backend-rapport-succession`

---

## Objectif

Calculator + endpoint d'analyse du **rapport à succession** (FR — art. 843-863 + 919 Cciv) : déterminer si une donation reçue d'un défunt par un cohéritier doit être réintégrée fictivement dans la masse à partager, qualifier le mode de rapport recommandé (en nature, en valeur, en moins prenant), et statuer sur les exemptions (dispense expresse art. 919, frais d'éducation art. 852, donations rémunératoires art. 851).

---

## Comportement attendu

### Cas nominal

L'avocat saisit la donation reçue (montant nominal au jour de la donation, valeur au jour du partage, qualité de l'héritier, dispense art. 919 éventuelle, nature présumée non rapportable art. 852) → l'outil détermine :
- Le **verdict d'obligation** (RAPPORTABLE / EXEMPT / DISPENSÉ),
- Le **montant rapportable** (BigDecimal — valeur au jour du partage si rapport en valeur, art. 860),
- Le **mode de rapport recommandé** (RAPPORT_EN_NATURE / RAPPORT_EN_VALEUR / RAPPORT_EN_MOINS_PRENANT),
- Le **délai de prescription** (5 ans, art. 924-1).

### Règles métier

| Règle | Article | Comportement |
|-------|---------|-------------|
| Tout héritier rapporte les donations reçues du défunt | 843 | Présomption de rapport pour héritiers DESCENDANT et CONJOINT_SURVIVANT (les seuls obligés) |
| Donation hors part successorale (dispense expresse) | 919 | DISPENSÉ — pas de rapport, s'impute sur la quotité disponible |
| Frais d'éducation, d'entretien, d'installation | 852 | EXEMPT — pas de rapport |
| Donations rémunératoires (services rendus) | 851 | EXEMPT — pas de rapport |
| Évaluation au jour du **partage** en l'état au jour de la **donation** | 860 | Mode par défaut = RAPPORT_EN_VALEUR — montant = valeurAuJourPartage |
| Bien donné encore détenu en nature (rare) | 858 | Mode possible = RAPPORT_EN_NATURE — montant = valeurAuJourPartage |
| Mode dégradé si héritier ne peut pas restituer | 858 | RAPPORT_EN_MOINS_PRENANT — montant prélevé sur la part future |
| Héritier non descendant ni conjoint survivant | 843 a contrario | NON_OBLIGÉ — pas de rapport (collatéraux, ascendants ordinaires hors réserve V1) |

### Critères d'entrée

- `donationsRecuesEur` (BigDecimal > 0) : montant nominal au jour de la donation
- `dateDonation` (LocalDate ≤ today) : pour traçabilité
- `valeurAuJourPartage` (BigDecimal > 0) : valeur retenue au jour du partage (art. 860)
- `donationDispenseDeRapport` (boolean) : dispense expresse art. 919
- `naturePresumeeNonRapportable` (boolean) : frais éducation/rémunératoire art. 851/852
- `qualiteHeritier` (enum DESCENDANT / CONJOINT_SURVIVANT) : seuls obligés au rapport

### Sorties

- `montantRapportable` (BigDecimal, scale 2) — 0 si EXEMPT/DISPENSÉ
- `modeRapportRecommande` (enum) — RAPPORT_EN_NATURE / RAPPORT_EN_VALEUR / RAPPORT_EN_MOINS_PRENANT / NON_APPLICABLE
- `verdictObligation` (enum) — RAPPORTABLE / EXEMPT / DISPENSÉ / NON_OBLIGÉ
- `delaiPrescriptionAns` (5 — art. 924-1)
- `baseJuridique` ("Art. 843-863 + 919 Cciv")
- `formule`, `messages`, `scoreEligibilite`

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| `donationsRecuesEur` ≤ 0 | rejet | 400 |
| `valeurAuJourPartage` ≤ 0 | rejet | 400 |
| `dateDonation` future | rejet | 400 |
| `qualiteHeritier` null | rejet | 400 |
| Pays workspace ≠ FRANCE | rejet (single-country) | 400 |
| Domaine ≠ DROIT_FAMILLE | rejet | 400 |
| Case file autre workspace | 404 | 404 |

---

## Critères d'acceptation

- [x] POST `/api/v1/case-files/{id}/rapport-succession-analysis` calcule et persiste l'analyse (upsert 1:1).
- [x] GET du même endpoint renvoie l'analyse persistée (404 sinon).
- [x] Donation dispensée art. 919 → verdict DISPENSÉ, montantRapportable = 0.
- [x] Donation présumée non rapportable (art. 852) → verdict EXEMPT, montantRapportable = 0.
- [x] Héritier descendant + donation non dispensée → verdict RAPPORTABLE, mode RAPPORT_EN_VALEUR (par défaut), montantRapportable = valeurAuJourPartage.
- [x] Héritier non obligé (autre que descendant/conjoint) — non testé en V1, qualités restreintes à l'enum.
- [x] `delaiPrescriptionAns` = 5 (art. 924-1).
- [x] Réponse contient `baseJuridique`, `formule`, `messages`.
- [x] Outil exposé via `decision_tool_visibility_rules` ALWAYS_ON DROIT_FAMILLE FRANCE priority 99 tool_id `F-FA-24-rapport-succession`.
- [x] Migration UUID `f1a04001-0000-0000-0000-ee0000000190`.

---

## Plan de test

### Unitaires (≥ 15)

1. Donation dispensée (art. 919) → DISPENSÉ, montantRapportable = 0.
2. Frais d'éducation présumés (art. 852) → EXEMPT, montantRapportable = 0.
3. Descendant + donation classique → RAPPORTABLE, mode RAPPORT_EN_VALEUR.
4. Conjoint survivant + donation classique → RAPPORTABLE, mode RAPPORT_EN_VALEUR.
5. Évaluation au jour du partage (art. 860) — montantRapportable = valeurAuJourPartage (≠ donationsRecuesEur).
6. Valeur au jour du partage > nominal → montant retenu = jour partage.
7. Valeur au jour du partage < nominal (perte de valeur) → montant retenu = jour partage.
8. Cumul dispense + nature non rapportable → DISPENSÉ prime sur EXEMPT (priorité art. 919 explicite).
9. `donationsRecuesEur` = 0 → IAE.
10. `donationsRecuesEur` < 0 → IAE.
11. `valeurAuJourPartage` ≤ 0 → IAE.
12. `dateDonation` future → IAE.
13. `qualiteHeritier` null → IAE.
14. Pays = BELGIQUE → IAE single-country.
15. `baseJuridique` contient "843" et "919".
16. `delaiPrescriptionAns` = 5.
17. Score d'éligibilité ≤ 100 et reflète RAPPORTABLE > NON_OBLIGÉ.
18. `formule` non blank, contient les valeurs clés.

### Intégration (≥ 7)

1. POST nominal descendant → 200, RAPPORTABLE, persistance.
2. GET après POST → 200, mêmes valeurs.
3. POST workspace BE → 400.
4. POST workspace DROIT_DU_TRAVAIL → 400.
5. POST sur case file d'un autre workspace → 404.
6. POST `donationsRecuesEur` ≤ 0 → 400.
7. POST upsert (2 fois POST) → remplace l'analyse.
8. GET sans POST préalable → 404.
9. POST dispense art. 919 → 200, verdict DISPENSÉ.

---

## Tables / endpoints / composants impactés

- **Table** : `rapport_succession_analyses` (1:1 `case_files`).
- **Endpoint** : `/api/v1/case-files/{caseFileId}/rapport-succession-analysis` (POST, GET).
- **Visibility** : `decision_tool_visibility_rules` ALWAYS_ON DROIT_FAMILLE FRANCE priority 99 — UUID `f1a04001-0000-0000-0000-ee0000000190`, tool_id `F-FA-24-rapport-succession`.

## Hors périmètre

- Frontend (SF-FA-24-14 jumelle suivant la livraison backend).
- Calcul de la masse à partager globale (interaction avec d'autres rapports) — V1 traite donation par donation.
- Régime BE (CC BE art. 843+ avec barème différent — feature jumelle backlog F-FA-24-BE).
- Imputation sur la quotité disponible (cf. SF-07 réserve héréditaire — interaction simulateur indépendant).
- Récompenses entre époux et avantages matrimoniaux.
- Évaluation détaillée du bien (V1 prend la valeur fournie par l'avocat — pas de logique d'évaluation).

---

## Impact par domaine métier

Cette SF est **strictement Droit de la famille FR** — outil dédié successions FR, single-country.
- **Droit du travail** : non applicable (retour 400).
- **Immigration** : non applicable.
- **Famille FR** : ce qu'on livre.
- **Famille BE** : non couvert ici, feature jumelle dédiée au backlog (F-FA-24-BE), barème CC BE différent.

## Parité des domaines métier

Outil de niveau 5 (scoring d'obligation et qualification du mode). Pas d'équivalent dans Travail/Immigration (concept successoral propre au droit civil patrimonial). Pas de feature jumelle requise hors du chantier successions.

## Analyse de cohérence transversale

| Cible | Statut |
|-------|--------|
| Outils décisionnels FR famille (dévolution F-FA-24-01, testament F-FA-24-03, donation F-FA-24-05, réserve F-FA-24-07) | Pattern cohérent (BigDecimal, ALWAYS_ON, DROIT_FAMILLE FRANCE, single-country) — **intégré** |
| Outils BE famille | Hors scope SF — backlog F-FA-24-BE dédié |
| Outils Travail/Immigration | Non applicable |
| Préoccupation transversale "Outil décisionnel métier" | Outil isolé : un outil = une situation (rapport à succession = qualification d'une donation comme rapportable / exempte / dispensée), distinct de la réserve héréditaire (action en réduction = excédent global libs > QD) |

## Nouveau pattern UI ou service partagé

Aucun nouveau pattern partagé — réutilise strictement le pattern Calculator/Service/Controller/Repository/Entity de `ReserveHereditaire*` (PR #672) avec persistance JSON via ObjectMapper.
