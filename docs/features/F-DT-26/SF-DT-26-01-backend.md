# Mini-spec — F-DT-26 / SF-DT-26-01 Indemnité compensatrice congés payés FR — BACKEND

## Identifiant
`F-DT-26 / SF-DT-26-01`

## Feature parente
`F-DT-26` — Indemnité compensatrice de congés payés FR

## Statut `draft` · Date `2026-04-25` · Branche `feat/SF-DT-26-01-conges-payes-backend`

---

## Objectif

Outil décisionnel calculant l'indemnité compensatrice de congés payés due au salarié à la rupture du contrat de travail (art. L.3141-26 et s. Code du travail) selon les **deux méthodes légales** — règle du dixième (10 %) vs règle du maintien de salaire — et retenant **la méthode la plus favorable au salarié** (jurisprudence constante, art. L.3141-28 Code du travail).

---

## Comportement

### Règles métier

**Article L.3141-26** — À la rupture du contrat, le salarié bénéficie d'une indemnité compensatrice pour les congés payés acquis et non pris.

**Article L.3141-24** — Méthode du dixième : indemnité de congés = 1/10 de la rémunération totale brute perçue pendant la période de référence (1er juin → 31 mai pour les CP légaux, ou période conventionnelle).

**Article L.3141-24 II** — Méthode du maintien de salaire : indemnité = rémunération qui aurait été perçue si le salarié avait continué à travailler.

**Article L.3141-28** (jurisprudence constante : Cass. soc. nombreuses) — La méthode retenue est **celle qui donne le résultat le plus favorable au salarié**. Si l'avocat veut forcer une méthode (cas spécifique), `methodeForcee` peut être renseigné.

### Inputs

- `totalRemunerationPeriodeEur` : BigDecimal > 0 (somme rémunération brute période de référence)
- `joursAcquisAnnee` : int ≥ 0 (jours CP acquis sur l'année)
- `joursPris` : int ≥ 0 (jours CP déjà pris)
- `salaireMensuelBrutEur` : BigDecimal > 0 (référence pour méthode maintien)
- `dateRupture` : LocalDate (date de rupture, informative)
- `methodeForcee` : enum nullable {`DIX_POURCENT`, `MAINTIEN`} — si null, calcul automatique le plus favorable

### Calculs

- `joursDus = max(0, joursAcquisAnnee - joursPris)`
- `montantMethodeDixPourcentEur = totalRemunerationPeriodeEur × 0.10`
- `montantMethodeMaintienEur = (salaireMensuelBrutEur / 30) × joursDus` (méthode trentième jours calendaires)
- Si `methodeForcee == null` :
  - `methodeRetenue` = celle qui donne le **montant le plus élevé** (favorable salarié)
  - En cas d'égalité : `DIX_POURCENT` (favorable historiquement par défaut)
- Sinon : `methodeRetenue = methodeForcee`
- `montantIndemniteEur = montant de la méthode retenue`

### Outputs

- `joursDus` : int
- `montantMethodeDixPourcentEur` / `montantMethodeMaintienEur` : BigDecimal
- `methodeRetenue` : enum
- `montantIndemniteEur` : BigDecimal
- `baseJuridique` : "Art. L.3141-26 et L.3141-28 Code du travail"
- `formule` : texte explicatif comparant les deux méthodes
- `messages` : list<String> (rappels juridiques)
- `country` : "FRANCE"

### Cas d'erreur

- `totalRemunerationPeriodeEur ≤ 0` → 400
- `salaireMensuelBrutEur ≤ 0` → 400
- `joursAcquisAnnee < 0` ou `joursPris < 0` → 400
- `joursPris > joursAcquisAnnee` → 400 (incohérence : plus pris qu'acquis)
- `methodeForcee` invalide → 400 (validation enum)
- Workspace BELGIQUE → 400 ("Outil FR uniquement — voir F-DT-28 pour pécule de vacances BE")
- Dossier autre domaine que DROIT_DU_TRAVAIL → 400
- Workspace étranger / non membre → 404

---

## Contrat API (verbatim — figé pour SF-DT-26-02 frontend)

### POST `/api/v1/case-files/{caseFileId}/conges-payes-indemnite`

**Request :**
```json
{
  "totalRemunerationPeriodeEur": 30000.00,
  "joursAcquisAnnee": 25,
  "joursPris": 5,
  "salaireMensuelBrutEur": 2500.00,
  "dateRupture": "2026-04-30",
  "methodeForcee": null
}
```

`methodeForcee` enum nullable : `DIX_POURCENT`, `MAINTIEN`. Si null → calcul automatique le plus favorable.

**Response 200 :**
```json
{
  "caseFileId": "uuid",
  "totalRemunerationPeriodeEur": 30000.00,
  "joursAcquisAnnee": 25,
  "joursPris": 5,
  "salaireMensuelBrutEur": 2500.00,
  "dateRupture": "2026-04-30",
  "methodeForcee": null,
  "joursDus": 20,
  "montantMethodeDixPourcentEur": 3000.00,
  "montantMethodeMaintienEur": 1666.67,
  "methodeRetenue": "DIX_POURCENT",
  "montantIndemniteEur": 3000.00,
  "baseJuridique": "Art. L.3141-26 et L.3141-28 Code du travail",
  "formule": "10 % × 30 000,00 € = 3 000,00 € (vs maintien 20 j × 2 500,00 / 30 = 1 666,67 €)",
  "messages": ["Méthode la plus favorable au salarié retenue (art. L.3141-28)."],
  "country": "FRANCE"
}
```

### GET `/api/v1/case-files/{caseFileId}/conges-payes-indemnite`

Même response. **404** si pas de POST préalable.

### Codes erreur

- 400 : validation input (montants, jours, methodeForcee invalide, country mismatch, domaine mismatch)
- 404 : dossier inconnu / non membre du workspace / GET sans POST préalable

---

## Architecture

Pattern standard (cf. `IndemnitePrecariteCddCalculator`). Table `indemnite_conges_payes_analyses` (migration 133). Tool_id `F-DT-26-conges-payes-indemnite`. **1 règle visibility ALWAYS_ON FRANCE DROIT_DU_TRAVAIL priority 52**, UUID `f1a04001-0000-0000-0000-ee0000000261`.

> **Note** : le brief initial proposait l'UUID `f1a04001-0000-0000-0000-ee00000dt261` mais la lettre `t` n'est pas un caractère hexadécimal valide (H2 rejette le cast UUID). Substitution choisie : `ee0000000261` (convention `ee0000000XYZ` déjà en usage — ex. `ee0000000191` F-DT-19, `ee0000000121` F-DT-12). Pas de collision.

### Composants à créer

- `IndemniteCongesPayesCalculator.java` (logique pure : 2 méthodes + comparaison)
- `IndemniteCongesPayesAnalysis.java` (entity 1:1 case_file)
- `IndemniteCongesPayesRepository.java`
- `IndemniteCongesPayesRequest.java` / `Response.java` / `Result.java` (records)
- `IndemniteCongesPayesMethode.java` (enum DIX_POURCENT / MAINTIEN)
- `IndemniteCongesPayesService.java` (gate FRANCE + DROIT_DU_TRAVAIL + workspace isolation)
- `IndemniteCongesPayesController.java`
- Migration `133-create-indemnite-conges-payes-analyses.xml`

### Tables impactées

- **Création** : `indemnite_conges_payes_analyses` (1:1 case_files via case_file_id unique)
- **INSERT** dans `decision_tool_visibility_rules` (1 ligne)

### Endpoints exposés

- `POST /api/v1/case-files/{caseFileId}/conges-payes-indemnite`
- `GET /api/v1/case-files/{caseFileId}/conges-payes-indemnite`

### Composants frontend impactés

Aucun — frontend = SF-DT-26-02 (parallèle, branche distincte).

---

## Plan de test

### UT (`IndemniteCongesPayesCalculatorTest`) ≥ 14

1. Méthode 10 % nominale : 30000 → 3000
2. Méthode maintien : 25 jours acquis, 5 pris, 2500 €/mois → (2500/30)×20 = 1666,67
3. `joursDus` = acquis - pris (20)
4. Comparaison automatique : 10 % > maintien → méthode retenue DIX_POURCENT
5. Comparaison automatique : maintien > 10 % → méthode retenue MAINTIEN (cas haut salaire mensuel + faible total)
6. Égalité parfaite → méthode retenue DIX_POURCENT (tie-breaker)
7. `methodeForcee = DIX_POURCENT` → méthode retenue DIX_POURCENT même si maintien plus favorable
8. `methodeForcee = MAINTIEN` → méthode retenue MAINTIEN même si 10 % plus favorable
9. Tous les jours déjà pris (acquis = pris) → joursDus = 0, maintien = 0, 10 % retenu si > 0
10. Validation `totalRemunerationPeriodeEur` ≤ 0 → IllegalArgumentException
11. Validation `salaireMensuelBrutEur` ≤ 0 → IllegalArgumentException
12. Validation `joursAcquisAnnee` < 0 → IllegalArgumentException
13. Validation `joursPris` < 0 → IllegalArgumentException
14. Validation `joursPris > joursAcquisAnnee` → IllegalArgumentException
15. Format `formule` contient les deux montants comparés
16. `baseJuridique` = "Art. L.3141-26 et L.3141-28 Code du travail"
17. Arrondi HALF_UP à 2 décimales

### IT (`IndemniteCongesPayesControllerIT`) ≥ 8

1. POST nominal FR DROIT_DU_TRAVAIL → 200 avec montants persistés
2. POST `methodeForcee=MAINTIEN` → 200 avec methode retenue MAINTIEN
3. POST upsert (deuxième POST sur même dossier remplace les valeurs)
4. POST validation `totalRemunerationPeriodeEur=0` → 400
5. POST `joursPris > joursAcquisAnnee` → 400
6. POST workspace autre user → 404 (isolation)
7. POST dossier DROIT_IMMIGRATION → 400
8. GET après POST → 200 avec données persistées
9. GET sans POST préalable → 404

### Isolation workspace

Test dédié n°6 — un utilisateur d'un autre workspace ne peut accéder ni en POST ni en GET au dossier d'un autre workspace.

---

## Préoccupations transversales

- **Auth / Principal** : N/A (pattern existant `OidcUser + Principal` réutilisé).
- **Workspace context** : N/A (résolution standard via `WorkspaceMemberRepository.findByUserAndPrimaryTrue`).
- **Plans / limites** : N/A (outil décisionnel, pas de quota dédié).
- **Navigation / routing** : N/A (backend pur).
- **Outil décisionnel métier** : oui — un outil = une situation (CP rupture FR uniquement). Pattern symétrique aux 16 calculateurs `*Analysis` existants. Pas de switch métier interne, pas de mélange de situations. Aucun outil existant n'a vocation à être scindé du fait de cette SF.

---

## Analyse de cohérence transversale

- **Autres pays** : Belgique → outil distinct (pécule de vacances + indemnité de sortie BE) prévu en F-DT-28 (backlog V8). Hors scope ici.
- **Autres domaines** : N/A (concept droit du travail uniquement).
- **Autres outils décisionnels FR/DT** : aucun ne calcule l'indemnité CP. F-DT-09 (comparateur indemnités) compare les indemnités de licenciement, ne touche pas aux CP. Pas de conflit.
- **Référentiels** : aucune entrée `legal_referentials` créée (pas besoin de référentiel — calcul algorithmique pur).

## Nouveau pattern UI ou service partagé

Aucun pattern partagé nouveau — entièrement local au calculateur.

## Impact par domaine métier

DROIT_DU_TRAVAIL FR uniquement. Sensible au domaine : oui (concept salarié/employeur). Sensible au pays : oui (CP français spécifiques, concept différent en BE = pécule de vacances). Justification : la SF traite un concept de droit du travail français spécifique (art. L.3141 Code du travail).

## Parité des domaines métier

Niveau 3 (calculateur). Non concerné par la règle parité ≥5.

---

## Critères d'acceptation

- [ ] `IndemniteCongesPayesCalculator` calcule les deux méthodes
- [ ] Comparaison automatique retient la méthode la plus favorable
- [ ] `methodeForcee` respecte le choix avocat
- [ ] POST/GET endpoints fonctionnent + persistance upsert 1:1
- [ ] Validation 400 pour entrées incohérentes
- [ ] Gate FRANCE + DROIT_DU_TRAVAIL + isolation workspace
- [ ] Migration 133 + 1 règle visibility F-IA-04
- [ ] ≥ 14 UT verts + ≥ 8 IT verts
- [ ] `baseJuridique` = "Art. L.3141-26 et L.3141-28 Code du travail"

## Hors scope

- Frontend (SF-DT-26-02 parallèle)
- Pécule de vacances belge (F-DT-28 V8 backlog)
- Calcul des CP eux-mêmes (acquisition, fractionnement, jours mobiles) — l'outil prend les jours acquis/pris en input
- Cas particuliers : maladie longue durée, congé maternité, période de référence personnalisée
- CP sur indemnité de licenciement (case d'usage spécifique mentionné en backlog F-DT-26 — extension future)

## Notes

- Pas de référentiel à seeder (calcul algorithmique).
- Le maintien de salaire utilise la méthode du **trentième** (30 jours calendaires) — la méthode des "jours ouvrés" (≈ 25 j/mois) reste applicable mais la doctrine majoritaire et la plupart des CCN retiennent le trentième pour la méthode comparative simple.
- Précision arithmétique : `BigDecimal` `RoundingMode.HALF_UP` à 2 décimales.
