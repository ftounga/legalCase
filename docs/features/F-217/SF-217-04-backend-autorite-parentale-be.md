# Mini-spec — F-217 / SF-217-04 — Backend : arbre décisionnel de l'autorité parentale belge

## Identifiant
`F-217 / SF-217-04`

## Feature parente
`F-217` — P2 Famille BE — outils décisionnels de fréquence haute (Vague 2 — Enfants)

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
`feat/SF-217-04-autorite-parentale-be`

---

## Objectif

Fournir un outil décisionnel backend qui qualifie le régime d'autorité parentale applicable
à un enfant d'un couple belge (conjointe par défaut, exclusive sur décision du Tribunal de
la famille) et oriente l'avocat sur la voie procédurale, à partir des éléments du dossier.

---

## Comportement attendu

### Cas nominal

1. L'avocat soumet les éléments de la situation parentale : filiation établie à l'égard
   des deux parents, existence d'un accord parental, demande d'autorité exclusive,
   éléments de gravité invoqués (désintérêt durable, mise en danger de l'enfant,
   incapacité d'un parent), existence d'une décision judiciaire antérieure, et le mode
   d'hébergement principal envisagé.
2. Le `AutoriteParentaleBeCalculator` applique l'arbre décisionnel du Code civil belge
   (art. 374-375) : (a) l'autorité parentale est **conjointe par principe** dès lors que
   la filiation est établie à l'égard des deux parents — peu importe que les parents
   vivent ensemble ; (b) l'autorité **exclusive** ne peut être confiée à un parent que
   par décision du Tribunal de la famille, dans l'intérêt de l'enfant, et suppose un motif
   grave caractérisé ; (c) à défaut de motif grave, une demande d'exclusive est vouée à
   l'échec — le TF maintient la conjointe.
3. Le calculateur produit un verdict (`AUTORITE_CONJOINTE` / `AUTORITE_EXCLUSIVE_FONDEE` /
   `AUTORITE_EXCLUSIVE_NON_FONDEE` / `QUALIFICATION_INCOMPLETE`), la voie procédurale
   recommandée, la liste des facteurs retenus avec leur fondement, les bases juridiques
   et des messages d'aide.
4. Le résultat est persisté par dossier (un seul résultat courant, écrasé au recalcul)
   et renvoyé.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Corps de requête absent | Message d'erreur explicite | 400 |
| Champ booléen obligatoire absent | Message d'erreur explicite | 400 |
| Commentaire > 1000 caractères | Message d'erreur explicite | 400 |
| Non authentifié | — | 401 |
| Dossier d'un autre workspace | Accès refusé | 403 |
| Dossier inexistant | — | 404 |
| `GET` sans calcul préalable | — | 404 |
| Dossier dont le `legalDomain` n'est pas `DROIT_FAMILLE` | Message explicite | 422 |
| Workspace dont le `country` n'est pas `BELGIQUE` (outil BE-only) | Message explicite | 422 |

---

## Contrat API (FIGÉ — importé par SF-217-05)

### POST `/api/v1/case-files/{caseFileId}/autorite-parentale-be`

Body `AutoriteParentaleBeRequest` :
```json
{
  "filiationEtablieDeuxParents": true,
  "accordParentalExiste": false,
  "demandeAutoriteExclusive": true,
  "desinteretDurableParent": false,
  "miseEnDangerEnfant": true,
  "incapaciteParent": false,
  "decisionJudiciaireAnterieure": false,
  "modeHebergementPrincipal": "HEBERGEMENT_PRINCIPAL_UN_PARENT",
  "commentaire": null
}
```
- `filiationEtablieDeuxParents` : boolean obligatoire.
- `accordParentalExiste` : boolean obligatoire.
- `demandeAutoriteExclusive` : boolean obligatoire.
- `desinteretDurableParent`, `miseEnDangerEnfant`, `incapaciteParent` : booleans
  obligatoires — facteurs de gravité justifiant une exclusive.
- `decisionJudiciaireAnterieure` : boolean obligatoire.
- `modeHebergementPrincipal` : enum obligatoire.
- `commentaire` : nullable, max 1000 caractères.
- Le pays est dérivé du workspace côté service.

Réponse `200` — `AutoriteParentaleBeResponse` : **ré-expose l'intégralité du body**
(snapshot) **+** les champs calculés.
```json
{
  "caseFileId": "uuid",
  "filiationEtablieDeuxParents": true,
  "accordParentalExiste": false,
  "demandeAutoriteExclusive": true,
  "desinteretDurableParent": false,
  "miseEnDangerEnfant": true,
  "incapaciteParent": false,
  "decisionJudiciaireAnterieure": false,
  "modeHebergementPrincipal": "HEBERGEMENT_PRINCIPAL_UN_PARENT",
  "commentaire": null,
  "verdict": "AUTORITE_EXCLUSIVE_FONDEE",
  "voieProcedurale": "REQUETE_TRIBUNAL_FAMILLE",
  "facteurs": [
    {
      "code": "MISE_EN_DANGER",
      "libelle": "Mise en danger de l'enfant invoquée",
      "fondement": "CC art. 374 §1 al. 2 (dérogation à l'autorité conjointe dans l'intérêt de l'enfant)",
      "favorableExclusive": true,
      "explication": "La mise en danger de l'enfant est un motif grave susceptible de fonder le retrait de l'exercice conjoint et l'attribution de l'autorité exclusive à l'autre parent."
    }
  ],
  "basesJuridiques": [
    "CC art. 374 §1 (autorité parentale exercée conjointement par les père et mère)",
    "CC art. 374 §1 al. 2 (le Tribunal de la famille peut confier l'exercice exclusif à un parent)",
    "CC art. 375 (déchéance de l'autorité parentale — cas extrêmes)"
  ],
  "messages": [
    "L'autorité parentale conjointe est le principe (CC art. 374 §1) : elle se maintient même après séparation, indépendamment du mode d'hébergement.",
    "Un motif grave est caractérisé : la demande d'autorité exclusive devant le Tribunal de la famille est plaidable — réunir les pièces établissant la mise en danger."
  ],
  "country": "BELGIQUE",
  "calculatedAt": "2026-05-18T10:00:00Z"
}
```

### GET `/api/v1/case-files/{caseFileId}/autorite-parentale-be`
- `200` : dernier résultat (même structure, inputs inclus → formulaire ré-éditable).
  `404` si jamais calculé. `403` / `401` / `422` idem POST.

### Enum `verdict` (`AutoriteParentaleBeVerdict`)
- `AUTORITE_CONJOINTE` — aucune demande d'exclusive : l'autorité parentale conjointe
  s'applique de plein droit (CC art. 374 §1).
- `AUTORITE_EXCLUSIVE_FONDEE` — une demande d'exclusive est formulée ET au moins un motif
  grave est caractérisé : la demande devant le TF est plaidable.
- `AUTORITE_EXCLUSIVE_NON_FONDEE` — une demande d'exclusive est formulée mais aucun motif
  grave n'est caractérisé : le TF maintiendra l'autorité conjointe (principe légal).
- `QUALIFICATION_INCOMPLETE` — la filiation n'est pas établie à l'égard des deux parents :
  l'arbre décisionnel de l'autorité parentale partagée ne peut pas s'appliquer en l'état.

### Enum `voieProcedurale` (`VoieProceduraleApBe`)
- `AUCUNE_AUTORITE_CONJOINTE_DROIT` — autorité conjointe de plein droit, aucune saisine
  nécessaire.
- `ACCORD_HOMOLOGUE_TF` — un accord parental existe : le faire homologuer par le TF
  (sécurise l'accord, lui donne force exécutoire).
- `REQUETE_TRIBUNAL_FAMILLE` — saisine du TF par requête pour trancher (exclusive demandée
  et fondée, ou désaccord à arbitrer).
- `ETABLISSEMENT_FILIATION_PREALABLE` — la filiation doit d'abord être établie
  (verdict `QUALIFICATION_INCOMPLETE`).

### Enum `modeHebergementPrincipal` (`ModeHebergementBe`)
`HEBERGEMENT_EGALITAIRE` (alterné — loi 18/07/2006) ·
`HEBERGEMENT_PRINCIPAL_UN_PARENT` · `HEBERGEMENT_NON_FIXE`.

### Enum `code` d'un facteur (`FacteurApBeCode`)
`DESINTERET_DURABLE` · `MISE_EN_DANGER` · `INCAPACITE_PARENT` ·
`ACCORD_PARENTAL` · `DECISION_ANTERIEURE`.

---

## Règles de l'arbre décisionnel

> ⚠️ **Validation juridique requise** : les articles CC art. 374-375 (autorité parentale,
> dérogation à l'exercice conjoint, déchéance) reflètent l'état du droit connu du modèle et
> sont **à valider par un avocat belge avant mise en production**. Le contenu juridique
> (articles, règles, libellés) est centralisé dans le Calculator (source unique de vérité).

### Détermination du verdict (figée dans le Calculator)

1. `filiationEtablieDeuxParents = false` → `QUALIFICATION_INCOMPLETE`,
   voie `ETABLISSEMENT_FILIATION_PREALABLE`.
2. `demandeAutoriteExclusive = false` → `AUTORITE_CONJOINTE` ;
   voie = `ACCORD_HOMOLOGUE_TF` si `accordParentalExiste`, sinon
   `AUCUNE_AUTORITE_CONJOINTE_DROIT`.
3. `demandeAutoriteExclusive = true` ET au moins un motif grave coché
   (`desinteretDurableParent` OU `miseEnDangerEnfant` OU `incapaciteParent`) →
   `AUTORITE_EXCLUSIVE_FONDEE`, voie `REQUETE_TRIBUNAL_FAMILLE`.
4. `demandeAutoriteExclusive = true` ET aucun motif grave →
   `AUTORITE_EXCLUSIVE_NON_FONDEE`, voie `AUCUNE_AUTORITE_CONJOINTE_DROIT`.

### Facteurs retenus

| Critère coché | Code facteur | favorableExclusive | Fondement (à valider) |
|---------------|--------------|--------------------|------------------------|
| `desinteretDurableParent` | `DESINTERET_DURABLE` | true | CC art. 374 §1 al. 2 |
| `miseEnDangerEnfant` | `MISE_EN_DANGER` | true | CC art. 374 §1 al. 2 (intérêt de l'enfant) |
| `incapaciteParent` | `INCAPACITE_PARENT` | true | CC art. 374 §1 al. 2 / art. 375 |
| `accordParentalExiste` | `ACCORD_PARENTAL` | false | CC art. 374 §1 (accord homologué par le TF) |
| `decisionJudiciaireAnterieure` | `DECISION_ANTERIEURE` | false | CC art. 387bis (révision de toute décision dans l'intérêt de l'enfant) |

Les facteurs ne pilotent le verdict que via la règle 3 (présence d'au moins un motif
grave) ; les facteurs non graves (`ACCORD_PARENTAL`, `DECISION_ANTERIEURE`) sont exposés
pour information et n'influent pas le verdict.

---

## Conformité F-IA-04
- [x] **Non applicable au sens strict** — SF backend pure. Conformité F-IA-04 et seed
  `decision_tool_visibility_rules` portés par SF-217-05 frontend (couplé TOOL_REGISTRY).

---

## Critères d'acceptation

- [ ] `POST` calcule et persiste un résultat ; recalcul écrase le précédent (upsert 1:1).
- [ ] Filiation non établie aux deux parents → `QUALIFICATION_INCOMPLETE`.
- [ ] Pas de demande d'exclusive → `AUTORITE_CONJOINTE` ; voie `ACCORD_HOMOLOGUE_TF` si
      accord parental, sinon `AUCUNE_AUTORITE_CONJOINTE_DROIT`.
- [ ] Demande d'exclusive + motif grave → `AUTORITE_EXCLUSIVE_FONDEE`, voie `REQUETE_TRIBUNAL_FAMILLE`.
- [ ] Demande d'exclusive sans motif grave → `AUTORITE_EXCLUSIVE_NON_FONDEE`.
- [ ] Chaque critère coché produit un facteur avec son code, son fondement et son drapeau.
- [ ] La réponse `POST` / `GET` ré-expose l'intégralité des inputs (formulaire ré-éditable).
- [ ] `GET` renvoie le dernier résultat ou 404.
- [ ] `400` (corps absent, booléen absent, commentaire trop long) / `403` / `404` /
      `422` (domaine ≠ famille, pays ≠ Belgique) / `401`.
- [ ] Isolation workspace testée.

---

## Périmètre

### Hors scope
- Frontend (SF-217-05).
- Hébergement égalitaire — recevabilité du mode (`hebergement-egalitaire-be` — reporté F-223 ;
  le mode d'hébergement n'est qu'un input contextuel ici).
- Désaccords parentaux ponctuels (scolarité, santé) — outil distinct reporté F-223.
- Déchéance de l'autorité parentale au sens de l'art. 375 (procédure pénale/protection —
  hors scope, mentionnée dans les bases juridiques uniquement).
- Réutilisation du Calculator FR `F-FA-19` (autorité parentale FR — mécanisme distinct).
- Pré-fill IA depuis l'analyse (aucun flag pivot dédié extrait par le pipeline V1 —
  documenté `PREFILL_COUNT_ALWAYS_ZERO` côté SF-217-05).

---

## Technique

### Endpoints
| Méthode | URL | Auth | Rôle |
|---------|-----|------|------|
| POST | `/api/v1/case-files/{caseFileId}/autorite-parentale-be` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/autorite-parentale-be` | Oui | MEMBER |

### Tables impactées
| Table | Opération | Notes |
|-------|-----------|-------|
| `autorite_parentale_be_analyses` | CREATE + INSERT/UPDATE/SELECT | id (UUID), case_file_id (FK UNIQUE), country (VARCHAR), snapshot_data (TEXT JSON), created_at, updated_at |
| `decision_tool_visibility_rules` | INSERT (seed) | porté par SF-217-05 |

### Migration Liquibase
- [x] Oui — `237-create-autorite-parentale-be-analyses.xml` (table seule).

### Classes backend (pattern `ProcedureNulliteLicenciement*` — snapshot JSON)
`AutoriteParentaleBeCalculator` (static), `AutoriteParentaleBeInput`,
`AutoriteParentaleBeResult`, `AutoriteParentaleBeRequest`, `AutoriteParentaleBeResponse`,
`AutoriteParentaleBeAnalysis` (@Entity), `AutoriteParentaleBeRepository`,
`AutoriteParentaleBeService`, `AutoriteParentaleBeController`.

---

## Plan de test

### Tests unitaires (Calculator)
- [ ] Filiation deux parents + pas de demande exclusive → `AUTORITE_CONJOINTE`,
      voie `AUCUNE_AUTORITE_CONJOINTE_DROIT`.
- [ ] Pas de demande exclusive + accord parental → voie `ACCORD_HOMOLOGUE_TF`.
- [ ] Demande exclusive + mise en danger → `AUTORITE_EXCLUSIVE_FONDEE`, voie `REQUETE_TRIBUNAL_FAMILLE`.
- [ ] Demande exclusive + désintérêt durable → `AUTORITE_EXCLUSIVE_FONDEE`.
- [ ] Demande exclusive sans aucun motif grave → `AUTORITE_EXCLUSIVE_NON_FONDEE`.
- [ ] Filiation non établie aux deux parents → `QUALIFICATION_INCOMPLETE`,
      voie `ETABLISSEMENT_FILIATION_PREALABLE`.
- [ ] Facteurs produits : code + fondement + drapeau favorableExclusive corrects.

### Tests d'intégration (Controller)
- [ ] `POST` → 200 + persistance ; recalcul écrase.
- [ ] `GET` → 200 / 404 si jamais calculé.
- [ ] `400` (corps absent, booléen manquant) / `403` / `404` / `422` (domaine ≠ famille,
      pays ≠ Belgique) / `401`.

### Isolation workspace
- [x] Applicable — un utilisateur du workspace A ne peut pas calculer/lire le résultat
  d'un dossier du workspace B.

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Outil décisionnel métier** — `autorite-parentale-be` est un nouvel outil
  décisionnel. Scan fait dans `SF-217-00-coherence.md` : aucun outil BE existant ne couvre
  l'autorité parentale belge (F-FA-19 est FR-only, masqué en BE). L'AP belge est
  structurellement distincte (conjointe de plein droit même séparés, exclusive uniquement
  sur décision du TF). `autorite-parentale-be` est une **situation métier distincte** —
  un outil = une situation.
- [x] Aucune autre préoccupation transversale.

### Smoke tests E2E
- [x] Aucun — feature additive.

---

## Dépendances
- Aucune SF bloquante. SF-217-05 (frontend) importe le contrat API. Parallélisable.

---

## Notes et décisions
- Persistance par snapshot JSON (`snapshot_data`) — aligné Vague 1 F-217.
- Aucune réutilisation du Calculator FR : l'autorité parentale belge (CC art. 374-375)
  diffère de l'AP française (`feedback_belgique_never_forget`).
- Le contenu juridique (articles CC, règles, libellés de fondement) est centralisé dans le
  Calculator et signalé pour validation par un avocat belge avant prod.
</content>
