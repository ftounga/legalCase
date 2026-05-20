# Mini-spec — F-217 / SF-217-18 — Backend : contestation de filiation (paternité) belge

## Identifiant
`F-217 / SF-217-18`

## Feature parente
`F-217` — P2 Famille BE — outils décisionnels de fréquence haute (Vague 3 — Successions / protection / international)

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-217-18-contestation-filiation-be`

---

## Objectif

Fournir un outil décisionnel backend qui qualifie la recevabilité d'une action en
contestation de filiation paternelle en Belgique (paternité présumée du mari ou
paternité reconnue volontairement), au regard du **CC art. 318** (à vérifier —
contestation de paternité, qualité à agir, délais), à partir des éléments du dossier.

---

## Comportement attendu

### Cas nominal

1. L'avocat soumet les éléments : nature de la filiation contestée
   (`PATERNITE_PRESUMEE_MARI` / `PATERNITE_RECONNUE_VOLONTAIRE` /
   `MATERNITE` — V1 hors scope mais l'enum existe pour évolutivité), qualité du
   demandeur (`ENFANT_MAJEUR` / `ENFANT_MINEUR_REPRESENTE` / `MERE` /
   `PERE_PRESUME_OU_RECONNAISSANT` / `TIERS_PRETENDANT_PERE_BIOLOGIQUE` / `MINISTERE_PUBLIC`),
   date de naissance de l'enfant, date de connaissance de l'élément remettant en cause
   la filiation (élément ADN, aveu, etc.), existence d'une **possession d'état**
   conforme à la filiation contestée (qui peut bloquer la contestation), existence
   d'un test ADN ou demande d'expertise.
2. Le `ContestationFiliationBeCalculator` applique l'arbre du **CC art. 318**
   (à vérifier — réforme filiation loi 01/07/2006) : (a) vérifie la **qualité à agir**
   (limitée par le code) ; (b) vérifie le **délai d'action** (1 an à compter de la
   connaissance du fait remettant en cause la filiation pour l'enfant ; règles
   distinctes pour la mère, le père présumé, le tiers — à vérifier ;
   note audit F-191 : « délai 1 an BE pour enfant mineur, 30 ans après majorité
   enfant » à vérifier) ; (c) vérifie si la **possession d'état** conforme bloque
   l'action (CC art. 318 § 2 — à vérifier : la contestation est irrecevable si la
   possession d'état a duré 5 ans depuis la naissance ou la reconnaissance, sauf
   exceptions) ; (d) recommande la production d'une expertise ADN si pertinent.
3. Le calculateur produit un verdict (`ACTION_RECEVABLE` /
   `ACTION_RECEVABLE_DELAI_CRITIQUE` / `IRRECEVABLE_DELAI_DEPASSE` /
   `IRRECEVABLE_QUALITE_A_AGIR` / `IRRECEVABLE_POSSESSION_ETAT` /
   `QUALIFICATION_INCOMPLETE`), le délai limite calculé, le nombre de jours restants,
   les motifs d'irrecevabilité le cas échéant, la voie procédurale (saisine TF —
   à vérifier), les bases juridiques mobilisées et des messages d'aide.
4. Le résultat est persisté par dossier (un seul résultat courant par dossier, écrasé
   au recalcul) et renvoyé.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Corps de requête absent | Message d'erreur explicite | 400 |
| `natureActionFiliation` absent ou `MATERNITE` (hors scope V1) | Message d'erreur explicite (`MATERNITE` → 400 « contestation de maternité hors scope V1 ») | 400 |
| `qualiteDemandeur` absent | Message d'erreur explicite | 400 |
| `dateNaissanceEnfant` absente / future / mal formée | Message d'erreur explicite | 400 |
| `dateConnaissanceFaitContestation` antérieure à `dateNaissanceEnfant` | Message d'erreur explicite | 400 |
| Commentaire > 1000 caractères | Message d'erreur explicite | 400 |
| Non authentifié | — | 401 |
| Dossier d'un autre workspace | Accès refusé | 403 |
| Dossier inexistant | — | 404 |
| `GET` sans calcul préalable | — | 404 |
| Dossier dont le `legalDomain` n'est pas `DROIT_FAMILLE` | Message explicite | 422 |
| Workspace dont le `country` n'est pas `BELGIQUE` (outil BE-only) | Message explicite | 422 |

---

## Contrat API (FIGÉ — importé par SF-217-19)

### POST `/api/v1/case-files/{caseFileId}/contestation-filiation-be`

Body `ContestationFiliationBeRequest` :
```json
{
  "natureActionFiliation": "PATERNITE_PRESUMEE_MARI",
  "qualiteDemandeur": "PERE_PRESUME_OU_RECONNAISSANT",
  "dateNaissanceEnfant": "2018-03-12",
  "dateConnaissanceFaitContestation": "2026-01-10",
  "possessionEtatConforme": false,
  "dureePossessionEtatAnnees": 0,
  "expertiseAdnDisponible": true,
  "demandeExpertiseAdnEnvisagee": false,
  "commentaire": null
}
```
- `natureActionFiliation` : enum obligatoire.
- `qualiteDemandeur` : enum obligatoire.
- `dateNaissanceEnfant` : `yyyy-MM-dd`, obligatoire, non future.
- `dateConnaissanceFaitContestation` : `yyyy-MM-dd`, obligatoire, postérieure ou
  égale à `dateNaissanceEnfant`, non future.
- `possessionEtatConforme` : boolean obligatoire.
- `dureePossessionEtatAnnees` : entier obligatoire, ≥ 0, ≤ 100 (durée en années
  entières — affichée comme telle à l'avocat).
- `expertiseAdnDisponible` : boolean obligatoire.
- `demandeExpertiseAdnEnvisagee` : boolean obligatoire.
- `commentaire` : nullable, max 1000 caractères.
- Le pays est dérivé du workspace côté service.

Réponse `200` — `ContestationFiliationBeResponse` : **ré-expose l'intégralité du body**
(snapshot) **+** les champs calculés.
```json
{
  "caseFileId": "uuid",
  "natureActionFiliation": "PATERNITE_PRESUMEE_MARI",
  "qualiteDemandeur": "PERE_PRESUME_OU_RECONNAISSANT",
  "dateNaissanceEnfant": "2018-03-12",
  "dateConnaissanceFaitContestation": "2026-01-10",
  "possessionEtatConforme": false,
  "dureePossessionEtatAnnees": 0,
  "expertiseAdnDisponible": true,
  "demandeExpertiseAdnEnvisagee": false,
  "commentaire": null,
  "verdict": "ACTION_RECEVABLE",
  "dateLimiteAction": "2027-01-10",
  "joursRestants": 235,
  "delaiStatut": "OK",
  "voieProcedurale": "REQUETE_TRIBUNAL_FAMILLE",
  "motifsIrrecevabilite": [],
  "actionsConcretes": [
    "Saisir le Tribunal de la famille du lieu de résidence de l'enfant par requête contradictoire (CJ art. 572bis — à vérifier).",
    "Produire le test ADN disponible ou demander une expertise judiciaire.",
    "Aviser le Ministère public (présence obligatoire en filiation — à vérifier)."
  ],
  "basesJuridiques": [
    "CC art. 318 nouveau (à vérifier) — contestation de la paternité du mari : qualité à agir et délais",
    "CC art. 318 § 2 (à vérifier) — irrecevabilité de la contestation en cas de possession d'état conforme de 5 ans depuis la naissance",
    "Loi du 01/07/2006 — refonte du droit de la filiation",
    "CJ art. 572bis — compétence Tribunal de la famille en matière de filiation"
  ],
  "messages": [
    "Père présumé contestant la paternité : action recevable dans le délai d'un an à compter de la connaissance du fait — date limite 2027-01-10, 235 jours restants.",
    "Pas de possession d'état conforme : la contestation n'est pas bloquée par CC art. 318 § 2.",
    "Test ADN disponible : pièce centrale de la contestation, à produire."
  ],
  "country": "BELGIQUE",
  "calculatedAt": "2026-05-20T10:00:00Z"
}
```

### GET `/api/v1/case-files/{caseFileId}/contestation-filiation-be`
- `200` : dernier résultat (même structure, inputs inclus → formulaire ré-éditable).
  `404` si jamais calculé. `403` / `401` / `422` idem POST.

### Enum `verdict` (`ContestationFiliationBeVerdict`)
- `ACTION_RECEVABLE` — qualité à agir reconnue + délai non dépassé + pas de
  possession d'état bloquante : l'action est recevable.
- `ACTION_RECEVABLE_DELAI_CRITIQUE` — recevable mais `joursRestants` ≤ 30 j → action
  à introduire d'urgence.
- `IRRECEVABLE_DELAI_DEPASSE` — `joursRestants < 0` : forclusion.
- `IRRECEVABLE_QUALITE_A_AGIR` — la qualité du demandeur n'est pas reconnue par CC
  art. 318 pour cette nature de contestation.
- `IRRECEVABLE_POSSESSION_ETAT` — possession d'état conforme ≥ 5 ans depuis la
  naissance ou la reconnaissance → action irrecevable (CC art. 318 § 2 — à vérifier).
- `QUALIFICATION_INCOMPLETE` — combinaison d'inputs ne permettant pas de trancher.

### Enum `natureActionFiliation` (`NatureActionFiliationBe`)
`PATERNITE_PRESUMEE_MARI` · `PATERNITE_RECONNUE_VOLONTAIRE` ·
`MATERNITE` (hors scope V1 — retour 400 si soumis).

### Enum `qualiteDemandeur` (`QualiteDemandeurFiliationBe`)
`ENFANT_MAJEUR` · `ENFANT_MINEUR_REPRESENTE` · `MERE` ·
`PERE_PRESUME_OU_RECONNAISSANT` · `TIERS_PRETENDANT_PERE_BIOLOGIQUE` ·
`MINISTERE_PUBLIC`.

### Enum `voieProcedurale` (`VoieProceduraleFiliationBe`)
`REQUETE_TRIBUNAL_FAMILLE` · `AUCUNE_ACTION_RECEVABLE`.

### Enum `delaiStatut` (`DelaiStatutBe`)
`OK` (> 30 j) · `CRITIQUE` (≤ 30 j et > 0) · `DEPASSE` (< 0).

### Enum `code` d'un motif d'irrecevabilité (`MotifIrrecevabiliteFiliationBeCode`)
`DELAI_FORCLUSION` · `QUALITE_NON_RECONNUE` · `POSSESSION_ETAT_CONFORME_5_ANS` ·
`MATERNITE_HORS_SCOPE_V1`.

---

## Règles de l'arbre décisionnel

> ⚠️ **Validation juridique requise** : le **CC art. 318** (refonte loi du 01/07/2006),
> la **qualité à agir** restrictive (limitée à l'enfant, ses père et mère, le père
> présumé ou reconnaissant, le tiers prétendant être le père biologique, et le
> Ministère public) et les **délais** (1 an à compter de la connaissance pour
> l'enfant, le père présumé, le tiers ; règles distinctes pour la mère — à vérifier)
> reflètent l'état du droit connu du modèle et sont **à valider par un avocat belge
> avant mise en production**. La règle de la **possession d'état conforme de 5 ans
> bloquant la contestation** (CC art. 318 § 2 — à vérifier) est centrale et son
> articulation exacte (point de départ, exceptions) doit être confirmée. L'audit
> F-191 mentionne « délai contestation 1 an BE pour enfant mineur, 30 ans après
> majorité enfant » qui est cohérent mais à vérifier. La présence obligatoire du
> Ministère public en matière de filiation est également à confirmer. Le contenu
> juridique est centralisé dans le Calculator (source unique de vérité).

### Détermination du verdict (figée dans le Calculator)

1. `natureActionFiliation = MATERNITE` → `400` (validation Bean Validation),
   motif `MATERNITE_HORS_SCOPE_V1` — pas exécution du Calculator.
2. Possession d'état conforme **ET** `dureePossessionEtatAnnees >= 5` →
   `IRRECEVABLE_POSSESSION_ETAT`, motif `POSSESSION_ETAT_CONFORME_5_ANS` (CC art. 318
   § 2 — à vérifier).
3. Qualité à agir non reconnue pour la nature de l'action (table ci-dessous) →
   `IRRECEVABLE_QUALITE_A_AGIR`, motif `QUALITE_NON_RECONNUE`.
4. Calcul de `dateLimiteAction = dateConnaissanceFaitContestation + 1 an` (par défaut,
   pour enfant mineur, père présumé, tiers — à vérifier).
5. `joursRestants < 0` → `IRRECEVABLE_DELAI_DEPASSE`, motif `DELAI_FORCLUSION`.
6. `joursRestants ≤ 30` → `ACTION_RECEVABLE_DELAI_CRITIQUE`.
7. Sinon → `ACTION_RECEVABLE`.

### Qualité à agir (par nature d'action — à valider par un avocat belge)

| Nature | Qualités reconnues | Fondement |
|--------|---------------------|-----------|
| `PATERNITE_PRESUMEE_MARI` | `ENFANT_MAJEUR`, `ENFANT_MINEUR_REPRESENTE`, `MERE`, `PERE_PRESUME_OU_RECONNAISSANT`, `TIERS_PRETENDANT_PERE_BIOLOGIQUE`, `MINISTERE_PUBLIC` | CC art. 318 (à vérifier) — qualités larges |
| `PATERNITE_RECONNUE_VOLONTAIRE` | Idem (l'enfant peut toujours, le reconnaissant lui-même peut sous conditions de vice du consentement — à vérifier) | CC art. 330 (à vérifier) |

### Calcul du délai

- `dateLimiteAction = dateConnaissanceFaitContestation + 1 an` (date à date — à
  vérifier ; règle commune V1, à raffiner par qualité du demandeur dans une SF
  ultérieure si la mère a un délai distinct par exemple).
- `joursRestants = ChronoUnit.DAYS.between(today, dateLimiteAction)`.
- `delaiStatut` : `OK` (> 30 j) / `CRITIQUE` (≤ 30 j et > 0) / `DEPASSE` (< 0).

### Possession d'état

- La possession d'état est conforme si les faits (tractatus / fama / nomen) sont
  cohérents avec la filiation contestée. V1 : qualifiée à l'audience par l'avocat
  via le boolean `possessionEtatConforme` + `dureePossessionEtatAnnees`. Outil dédié
  `possession-etat-be` (audit F-191 § 3.4) reporté à F-223.
- Règle V1 figée : `possessionEtatConforme = true` ET
  `dureePossessionEtatAnnees >= 5` → contestation irrecevable (CC art. 318 § 2 —
  à vérifier).

---

## Conformité F-IA-04
- [x] **Non applicable au sens strict** — SF backend pure. Conformité F-IA-04 et seed
  `decision_tool_visibility_rules` portés par SF-217-19 frontend (couplé TOOL_REGISTRY).

---

## Champs IA à extraire (pré-remplissage IA — V1)

| Champ | Source backend potentielle | Statut V1 |
|-------|----------------------------|-----------|
| `dateNaissanceEnfant` | Détection date naissance d'un enfant | Aspirationnel — non extrait V1 |
| `qualiteDemandeur` | Détection rôle du client dans le dossier | Aspirationnel — non extrait V1 |

**V1 : `PREFILL_COUNT_ALWAYS_ZERO = true`** côté SF-217-19. Pas de nouveau champ
ajouté à `FamilleExtractedData` dans cette SF — l'extension IA est aspirationnelle
(flag `presomption_paternite_litige_be` mentionné audit F-191 pourrait à terme
brancher).

---

## Critères d'acceptation

- [ ] `POST` calcule et persiste un résultat ; recalcul écrase le précédent (upsert 1:1
      par dossier).
- [ ] `natureActionFiliation = MATERNITE` → `400` avec message hors scope V1.
- [ ] Possession d'état conforme ≥ 5 ans → `IRRECEVABLE_POSSESSION_ETAT`, motif
      `POSSESSION_ETAT_CONFORME_5_ANS`.
- [ ] Qualité non reconnue → `IRRECEVABLE_QUALITE_A_AGIR` (test cas par cas par nature).
- [ ] `dateConnaissanceFaitContestation + 1 an` < today → `IRRECEVABLE_DELAI_DEPASSE`.
- [ ] `joursRestants ≤ 30` → `ACTION_RECEVABLE_DELAI_CRITIQUE`.
- [ ] Tous critères favorables → `ACTION_RECEVABLE`.
- [ ] `dateConnaissanceFaitContestation` antérieure à `dateNaissanceEnfant` → `400`.
- [ ] La réponse `POST` / `GET` ré-expose l'intégralité des inputs (formulaire
      ré-éditable).
- [ ] `GET` renvoie le dernier résultat ou 404.
- [ ] `400` / `403` / `404` / `422` / `401`.
- [ ] Isolation workspace testée.

---

## Périmètre

### Hors scope
- Frontend (SF-217-19).
- Contestation de **maternité** — V1 : `400` avec message hors scope (article CC
  distinct, cas pratique plus rare — outil dédié potentiel reporté).
- Action en **recherche** de paternité (`recherche-paternite-be` — audit F-191 § 3.4,
  reporté F-223 — action distincte, fondement CC art. 332ter).
- Reconnaissance de paternité (`reconnaissance-paternelle-be` — reporté F-223 — acte
  positif, pas une contestation).
- Possession d'état (`possession-etat-be` — outil distinct, reporté F-223 ; V1 utilise
  un boolean simple en input).
- Génération de la requête au TF — outil dédié potentiel, reporté.
- Réutilisation du Calculator FR `F-FA-18-contestation-paternite` (concepts proches
  mais articles, délais et qualité à agir distincts — pas réutilisable).
- Pré-fill IA depuis l'analyse (documenté `PREFILL_COUNT_ALWAYS_ZERO` côté SF-217-19).
- Seed `decision_tool_visibility_rules` (porté par SF-217-19).

---

## Technique

### Endpoints
| Méthode | URL | Auth | Rôle |
|---------|-----|------|------|
| POST | `/api/v1/case-files/{caseFileId}/contestation-filiation-be` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/contestation-filiation-be` | Oui | MEMBER |

### Tables impactées
| Table | Opération | Notes |
|-------|-----------|-------|
| `contestation_filiation_be_analyses` | CREATE + INSERT/UPDATE/SELECT | id (UUID), case_file_id (FK UNIQUE), country (VARCHAR), snapshot_data (TEXT JSON), created_at, updated_at |
| `decision_tool_visibility_rules` | INSERT (seed) | porté par SF-217-19 |

### Migration Liquibase
- [x] Oui — `278-create-contestation-filiation-be-analyses.xml` (table seule). Numéro
  `278` = prochain libre après `277` (SF-217-17). À renuméroter si conflit au merge.

### Classes backend (pattern Vague 1+2 F-217)
`ContestationFiliationBeCalculator` (static), `ContestationFiliationBeInput`,
`ContestationFiliationBeResult`, `ContestationFiliationBeRequest`,
`ContestationFiliationBeResponse`, `ContestationFiliationBeAnalysis` (@Entity),
`ContestationFiliationBeRepository`, `ContestationFiliationBeService`,
`ContestationFiliationBeController`.

---

## Plan de test

### Tests unitaires (Calculator)
- [ ] Père présumé, connaissance récente, pas de possession d'état → `ACTION_RECEVABLE`.
- [ ] Père présumé, connaissance > 1 an → `IRRECEVABLE_DELAI_DEPASSE`.
- [ ] Père présumé, connaissance il y a ~340 j → `ACTION_RECEVABLE_DELAI_CRITIQUE`.
- [ ] Possession d'état conforme 6 ans → `IRRECEVABLE_POSSESSION_ETAT`.
- [ ] Possession d'état conforme 3 ans → pas de blocage par possession d'état.
- [ ] `natureActionFiliation = MATERNITE` → `400` (validation).
- [ ] `dateConnaissanceFaitContestation` antérieure à `dateNaissanceEnfant` → `400`.
- [ ] Calcul du délai 1 an date à date (gestion années bissextiles, mois différents).

### Tests d'intégration (Controller)
- [ ] `POST` → 200 + persistance ; recalcul écrase.
- [ ] `GET` → 200 / 404 si jamais calculé.
- [ ] `400` (corps absent, `MATERNITE` soumis, date connaissance antérieure à
      naissance, date future, commentaire trop long) / `403` / `404` / `422`
      (domaine ≠ famille, pays ≠ Belgique) / `401`.

### Isolation workspace
- [x] Applicable.

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Outil décisionnel métier** — `contestation-filiation-be` est un nouvel outil
  décisionnel. Scan : aucun outil BE existant ne couvre la contestation de filiation
  belge (F-FA-18-contestation-paternite FR-only, articles et délais distincts).
  **Un outil = une situation** — outil dédié à la contestation (≠ reconnaissance ≠
  recherche, qui sont des actions juridiquement distinctes).
- [x] Aucune autre préoccupation transversale.

### Smoke tests E2E
- [x] Aucun — feature additive.

---

## Dépendances
- Aucune SF bloquante. SF-217-19 (frontend) importe le contrat API. Parallélisable.

---

## Notes et décisions
- Persistance par snapshot JSON — pattern Vague 1+2 F-217.
- **Maternité hors scope V1** : la contestation de maternité (CC art. 330 ou distinct
  — à vérifier) est juridiquement distincte et statistiquement rare. V1 retourne `400`
  avec un message explicite. Évolution : SF dédiée si signal terrain.
- Aucune réutilisation du Calculator FR : les délais BE (1 an / 5 ans / 30 ans selon
  qualité — à vérifier) et la règle de possession d'état BE (5 ans bloque la
  contestation, CC art. 318 § 2 — à vérifier) sont distincts du droit FR
  (`feedback_belgique_never_forget`).
- Délai V1 figé à 1 an (commun aux qualités principales — enfant, père présumé,
  tiers). Si l'audit F-191 confirme un délai distinct pour la mère, une SF ultérieure
  raffinera la table « Qualité à agir → délai ».
- Le contenu juridique (CC art. 318 nouveau, loi 01/07/2006, CJ art. 572bis,
  délais, possession d'état) est centralisé dans le Calculator et signalé pour
  validation par un avocat belge avant prod. Articles tagués `(à vérifier)` cohérents
  avec l'audit F-191.
