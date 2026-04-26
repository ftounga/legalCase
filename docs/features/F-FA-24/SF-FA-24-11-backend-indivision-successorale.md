# Mini-spec — F-FA-24 / SF-FA-24-11 Backend indivision successorale

## Identifiant

`F-FA-24 / SF-FA-24-11`

## Feature parente

`F-FA-24` — Droit des successions (chantier successions FR — déjà livrées : SF-01/02 dévolution, SF-03/04 testament, SF-05/06 donation, SF-07/08 réserve héréditaire).

## Statut

`in-progress`

## Date de création

2026-04-26

## Branche Git

`feat/SF-FA-24-11-backend-indivision-successorale`

---

## Objectif

Calculator + endpoint d'analyse de la **gestion d'une indivision successorale** (FR — art. 815 à 832-2 Cciv pour l'indivision légale, art. 1873-1 et s. Cciv pour l'indivision conventionnelle, art. 815-1 et s. Cciv pour le maintien forcé), avec calcul d'indemnité d'occupation (art. 815-9), évaluation des frais de gestion et orientation vers le partage selon le degré de conflictualité.

À distinguer de `F-FA-22 IndivisionPostCommunautaire` (suite divorce) — ici il s'agit de l'indivision successorale héritée du défunt.

---

## Comportement attendu

### Cas nominal

L'avocat saisit le contexte de l'indivision successorale (date du décès = origine, type, consentement collectif, occupation exclusive, contestations, demande de partage) → l'outil retourne :
- `verdictGestion` : `HARMONIEUSE` / `CONFLICTUELLE` / `BLOCAGE`,
- `dispositifRecommande` : ex. `MAINTIEN_INDIVISION_LEGALE`, `CONVENTION_INDIVISION_5_ANS`, `MEDIATION_FAMILIALE`, `PARTAGE_AMIABLE`, `PARTAGE_JUDICIAIRE` (renvoi vers F-FA-24-09 partage judiciaire),
- `indemniteOccupationDueEur` : montant calculé (art. 815-9 al. 2 — taux conventionnel 4 %/an sur la valeur, à proportion de la quote-part des co-indivisaires non occupants),
- `fraisGestionEstimesEur` : estimation forfaitaire (entretien + comptabilité + actes administration),
- `dureeIndivisionMois` : calculé à partir de `dateOuvertureSuccession`,
- `baseJuridique` : `Art. 815 à 832-2 + 1873-1 et s. Cciv`,
- `formule` : trace explicative,
- `messages` : checklist actions (rendez-vous notarial, médiation, demande de partage, etc.).

### Trois régimes d'indivision (art. 815 et s.)

| Type | Référence | Caractéristiques |
|------|-----------|------------------|
| `INDIVISION_LEGALE` | art. 815 Cciv | Régime par défaut, pas de durée limitée, chacun peut demander partage à tout moment |
| `INDIVISION_CONVENTIONNELLE` | art. 1873-1 + Cciv | Convention écrite, durée ≤ 5 ans renouvelables, gérant désigné |
| `MAINTIEN_FORCE` | art. 815-1 + Cciv | Décision judiciaire, pour 2 à 5 ans, pour préserver une exploitation ou éviter une vente forcée |

### Verdict de gestion

| Conditions | Verdict |
|------------|---------|
| `consentementsTous` ET pas de conflit ni occupation exclusive contestée | `HARMONIEUSE` |
| `actesAdministrationContestes` OU `occupationExclusive` (sans accord) OU absence consentement | `CONFLICTUELLE` |
| `demandePartage` ET `actesAdministrationContestes` ET pas de consentement | `BLOCAGE` |

### Dispositif recommandé

| Verdict | Type courant | Demande partage | Dispositif |
|---------|-------------|-----------------|-----------|
| HARMONIEUSE | LEGALE | non | `CONVENTION_INDIVISION_5_ANS` (sécurise la gestion) |
| HARMONIEUSE | LEGALE/CONV | oui | `PARTAGE_AMIABLE` |
| CONFLICTUELLE | tout | non | `MEDIATION_FAMILIALE` (préalable à toute saisine, art. 1108 CPC) |
| CONFLICTUELLE | tout | oui | `PARTAGE_AMIABLE` (notarié) |
| BLOCAGE | tout | oui | `PARTAGE_JUDICIAIRE` (renvoi F-FA-22 / F-FA-24-09 art. 1364 CPC) |
| BLOCAGE | tout | non | `MEDIATION_FAMILIALE` |

### Indemnité d'occupation (art. 815-9 al. 2 Cciv)

```
indemnite = valeurBienOccupeEur × 0.04 / 12 × dureeIndivisionMois × quotePartLesee
```

où `quotePartLesee = 1 - (1/nbHeritiers)` (chaque héritier non occupant ayant une quote-part théorique égale dans une succession équitable — V1 simplifiée).

Si pas d'occupation exclusive → `indemnite = 0`.

### Frais de gestion estimés

Forfait indicatif :
- `entretien` = 1 % de la valeur du patrimoine indivis / an
- `actes administration` = 500 €/an
- `comptabilite` = 300 €/an

Total annuel = `valeurPatrimoineIndivis × 0.01 + 800` puis multiplié par `(dureeIndivisionMois / 12)`.

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| `dateOuvertureSuccession` future | rejet | 400 |
| `dateOuvertureSuccession` null | rejet | 400 |
| `typeIndivision` invalide | rejet | 400 |
| `nbHeritiers` < 2 ou > 50 | rejet | 400 |
| `valeurPatrimoineIndivisEur` < 0 | rejet | 400 |
| `valeurBienOccupeEur` négative ou > valeur patrimoine total | rejet | 400 |
| Pays workspace ≠ FRANCE | rejet (outil single-country) | 400 |
| Domaine ≠ DROIT_FAMILLE | rejet | 400 |
| Case file autre workspace | 404 | 404 |

---

## Critères d'acceptation

- [x] POST `/api/v1/case-files/{id}/indivision-successorale-analysis` calcule et persiste l'analyse (upsert 1:1).
- [x] GET du même endpoint renvoie l'analyse persistée (404 sinon).
- [x] `dureeIndivisionMois` calculé depuis `dateOuvertureSuccession`.
- [x] Verdict cohérent sur les 3 cas (HARMONIEUSE, CONFLICTUELLE, BLOCAGE).
- [x] `indemniteOccupationDueEur` correcte (art. 815-9), 0 si pas d'occupation.
- [x] `fraisGestionEstimesEur` cohérent (1 % patrimoine + 800 €/an, prorata).
- [x] Dispositif recommandé renvoie au bon outil suivant (médiation, partage amiable, judiciaire).
- [x] Outil exposé via `decision_tool_visibility_rules` ALWAYS_ON DROIT_FAMILLE FRANCE priority 98 tool_id `F-FA-24-indivision-successorale`.
- [x] Migration UUID `f1a04001-0000-0000-0000-ee0000000189` (pas de collision).

---

## Plan de test

### Unitaires (≥ 15)

1. Indivision LEGALE consentements unanimes → `HARMONIEUSE` + `CONVENTION_INDIVISION_5_ANS`.
2. Indivision LEGALE consentements unanimes + demande partage → `HARMONIEUSE` + `PARTAGE_AMIABLE`.
3. Occupation exclusive non consentie → `CONFLICTUELLE` + indemnité > 0.
4. Actes admin contestés → `CONFLICTUELLE` + `MEDIATION_FAMILIALE`.
5. Blocage total (partage + admin contestée + pas consent) → `BLOCAGE` + `PARTAGE_JUDICIAIRE`.
6. Pas d'occupation → indemnité = 0.
7. Calcul indemnité 200 000 × 0.04 / 12 × 24 × (1 − 1/3) → 10 666,67 €.
8. Indivision CONVENTIONNELLE harmonieuse → `HARMONIEUSE`, conserve la convention.
9. MAINTIEN_FORCE → `dispositifRecommande` mentionne le maintien.
10. `dureeIndivisionMois` 18 mois si décès il y a 18 mois.
11. Frais gestion 100 000 € patrimoine sur 24 mois → 1800 × 2 = 3 600 €.
12. Validation `dateOuvertureSuccession` future → IAE.
13. Validation `nbHeritiers` < 2 → IAE.
14. Validation `nbHeritiers` > 50 → IAE.
15. Validation `valeurPatrimoineIndivisEur` négative → IAE.
16. Validation `valeurBienOccupeEur > valeurPatrimoineIndivisEur` → IAE.
17. Validation `typeIndivision` null/invalide → IAE.
18. Messages contiennent "art. 815" et hiérarchie partage.

### Intégration (≥ 7)

1. POST nominal (LEGALE + harmonieuse) → 200 + persistance.
2. GET après POST → 200, même valeurs.
3. POST workspace BE → 400.
4. POST workspace DROIT_DU_TRAVAIL → 400.
5. POST sur case file d'un autre workspace → 404.
6. POST `dateOuvertureSuccession` future → 400.
7. POST upsert (2 fois POST) → remplace l'analyse.
8. GET sans POST préalable → 404.

---

## Tables / endpoints / composants impactés

- **Table** : `indivision_successorale_analyses` (1:1 `case_files`).
- **Endpoint** : `/api/v1/case-files/{caseFileId}/indivision-successorale-analysis` (POST, GET).
- **Visibility** : `decision_tool_visibility_rules` ALWAYS_ON DROIT_FAMILLE FRANCE priority 98 — UUID `f1a04001-0000-0000-0000-ee0000000189`, tool_id `F-FA-24-indivision-successorale`.

## Hors périmètre

- Frontend (SF jumelle ultérieure).
- Calcul par souche (descendants représentés) — V1 simplifiée (1/N par héritier).
- Régime BE (CC BE art. 577-2 et s. — feature jumelle backlog).
- Procédure judiciaire de partage (déjà couvert par F-FA-22 indivision post-communautaire / F-FA-24-09 partage judiciaire).
- Calcul détaillé des comptes d'indivision (compte de revenus, compte d'avances) — orientation seulement.
- Convention d'indivision rédigée — V1 ne fait que recommander la rédaction.

---

## Impact par domaine métier

Cette SF est **strictement Droit de la famille FR** — outil dédié indivision successorale FR, single-country.
- **Droit du travail** : non applicable (retour 400).
- **Immigration** : non applicable.
- **Famille FR** : ce qu'on livre.
- **Famille BE** : non couvert ici, à traiter dans une feature jumelle dédiée (CC BE art. 577-2 +).

## Parité des domaines métier

Outil de niveau 5 (verdict de gestion + scoring conflictualité). Pas d'équivalent dans Travail/Immigration (concept successoral patrimonial). Pas de feature jumelle requise hors du chantier successions.

## Analyse de cohérence transversale

| Cible | Statut |
|-------|--------|
| F-FA-22 (indivision post-communautaire) | Pattern source — divergent par origine (divorce vs décès), périmètre métier distinct, **intégré** |
| F-FA-24-07 (réserve héréditaire) | Pattern cohérent (BigDecimal, ALWAYS_ON, DROIT_FAMILLE FRANCE, single-country) — **intégré** |
| Outils BE famille | Hors scope SF — backlog dédié |
| Outils Travail/Immigration | Non applicable |
| Préoccupation transversale "Outil décisionnel métier" | Nouveau, isolé : un outil = une situation (indivision SUCCESSORALE — distincte de l'indivision post-communautaire F-FA-22 et du partage judiciaire F-FA-24-09) |
