# Mini-spec — F-217 / SF-217-02 — Backend : checklist de procédure de liquidation-partage belge

## Identifiant
`F-217 / SF-217-02`

## Feature parente
`F-217` — P2 Famille BE — ~10 outils décisionnels de fréquence haute (Vague 1 — Patrimoine du couple)

## Statut
`ready`

## Date de création
2026-05-17

## Branche Git
`feat/SF-217-02-liquidation-partage-be`

---

## Objectif

Fournir un outil décisionnel backend qui suit l'avancement de la procédure de liquidation-partage post-divorce devant notaire commis (Code judiciaire belge art. 1207 et s. / 1218), positionne le dossier sur la séquence des étapes, calcule les délais procéduraux critiques (notamment le délai de contredits d'un mois) et rend un verdict d'état d'avancement.

---

## Comportement attendu

### Cas nominal

1. L'avocat soumet les éléments de la procédure de liquidation-partage : désignation du notaire-liquidateur par le Tribunal de la famille, ouverture des opérations, état liquidatif / projet de partage établi, date de notification du projet, contredits déposés, procès-verbal de dires et difficultés, et homologation par le TF.
2. Le `LiquidationPartageBeCalculator` positionne le dossier sur la séquence des étapes, marque chacune `FAITE` / `EN_COURS` / `A_VENIR`, calcule le délai de contredits restant (1 mois à compter de la notification du projet — CJ art. 1218) et détecte les anomalies de séquence.
3. Le calculateur produit un verdict d'avancement (`PROCEDURE_NON_ENGAGEE` / `EN_COURS` / `DELAI_CONTREDITS_CRITIQUE` / `EN_ATTENTE_HOMOLOGATION` / `CLOTUREE`), la checklist d'étapes, les délais et les bases juridiques.
4. Le résultat est persisté par dossier (un seul résultat courant par dossier, écrasé au recalcul) et renvoyé.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Champ obligatoire absent / date mal formée / date incohérente (ex. notification avant désignation) | Message d'erreur explicite | 400 |
| Non authentifié | — | 401 |
| Dossier d'un autre workspace | Accès refusé | 403 |
| Dossier inexistant | — | 404 |
| `GET` sans calcul préalable | — | 404 |
| Dossier dont le `legalDomain` n'est pas `DROIT_FAMILLE` | Message explicite | 422 |
| Workspace dont le `country` n'est pas `BELGIQUE` (outil BE-only) | Message explicite | 422 |

---

## Contrat API (FIGÉ — importé par SF-217-03)

### POST `/api/v1/case-files/{caseFileId}/liquidation-partage-be`

Body `LiquidationPartageBeRequest` :
```json
{
  "notaireDesigne": true,
  "dateDesignationNotaire": "2026-01-10",
  "operationsOuvertes": true,
  "dateOuvertureOperations": "2026-02-05",
  "inventaireEtabli": true,
  "projetLiquidationEtabli": true,
  "dateNotificationProjet": "2026-04-25",
  "contreditsDeposes": false,
  "procesVerbalDiresEtabli": false,
  "homologationDemandee": false,
  "dateHomologation": null,
  "commentaire": null
}
```
- `notaireDesigne`, `operationsOuvertes`, `inventaireEtabli`, `projetLiquidationEtabli`, `contreditsDeposes`, `procesVerbalDiresEtabli`, `homologationDemandee` : booleans obligatoires.
- `dateDesignationNotaire`, `dateOuvertureOperations`, `dateNotificationProjet`, `dateHomologation` : `yyyy-MM-dd`, nullable (obligatoires seulement si l'étape correspondante est `true` — contrôlé par le service).
- `commentaire` : nullable, max 1000.

Réponse `200` — `LiquidationPartageBeResponse` : **ré-expose l'intégralité des champs du body de requête** (snapshot pour pré-remplissage / ré-édition du formulaire) **+** les champs calculés ci-dessous.
```json
{
  "caseFileId": "uuid",
  "notaireDesigne": true,
  "dateDesignationNotaire": "2026-01-10",
  "operationsOuvertes": true,
  "dateOuvertureOperations": "2026-02-05",
  "inventaireEtabli": true,
  "projetLiquidationEtabli": true,
  "dateNotificationProjet": "2026-04-25",
  "contreditsDeposes": false,
  "procesVerbalDiresEtabli": false,
  "homologationDemandee": false,
  "dateHomologation": null,
  "commentaire": null,
  "verdict": "DELAI_CONTREDITS_CRITIQUE",
  "etapes": [
    {
      "code": "DESIGNATION_NOTAIRE",
      "libelle": "Désignation du notaire-liquidateur par le Tribunal de la famille",
      "statut": "FAITE",
      "ordre": 1,
      "fondement": "CJ art. 1207-1209",
      "explication": "Le Tribunal de la famille désigne un notaire-liquidateur (et le cas échéant un notaire représentant la partie défaillante) chargé de conduire les opérations."
    }
  ],
  "delais": [
    {
      "code": "DELAI_CONTREDITS",
      "libelle": "Délai de contredits sur le projet de liquidation-partage",
      "fondement": "CJ art. 1218",
      "dateDepart": "2026-04-25",
      "dateEcheance": "2026-05-25",
      "joursRestants": 8,
      "statut": "CRITIQUE"
    }
  ],
  "prochaineEtape": "Déposer les contredits au notaire avant le 25/05/2026 ou acter l'absence de contredits.",
  "basesJuridiques": [
    "CJ art. 1207-1209 (désignation du notaire-liquidateur)",
    "CJ art. 1214 (état liquidatif / projet de partage)",
    "CJ art. 1218 (notification du projet, contredits dans le mois, procès-verbal de dires)"
  ],
  "messages": [
    "Le projet de liquidation a été notifié le 25/04/2026 : le délai d'un mois pour déposer les contredits expire le 25/05/2026."
  ],
  "country": "BELGIQUE",
  "calculatedAt": "2026-05-17T10:00:00Z"
}
```

### GET `/api/v1/case-files/{caseFileId}/liquidation-partage-be`
- `200` : dernier résultat (même structure, inputs inclus → le formulaire est ré-éditable). `404` si jamais calculé. `403` / `401` / `422` idem POST.

### Enum `verdict` (`LiquidationPartageBeVerdict`)
- `PROCEDURE_NON_ENGAGEE` — le notaire n'est pas encore désigné.
- `EN_COURS` — procédure engagée, étapes en cours, pas de délai critique imminent.
- `DELAI_CONTREDITS_CRITIQUE` — le projet est notifié, le délai d'un mois pour les contredits court et reste ≤ 10 jours (ou est dépassé sans contredits actés).
- `EN_ATTENTE_HOMOLOGATION` — procès-verbal de dires établi, homologation par le TF non encore intervenue.
- `CLOTUREE` — homologation prononcée.

### Enum `statut` d'une étape (`EtapeStatutBe`)
`FAITE` · `EN_COURS` (étape immédiatement suivante de la dernière étape faite) · `A_VENIR`.

### Enum `statut` d'un délai (`DelaiStatutBe`)
`OK` (échéance > 10 jours) · `CRITIQUE` (échéance ≤ 10 jours et non dépassée) · `DEPASSE` (échéance dépassée) · `NON_DEMARRE` (point de départ non encore connu).

---

## Étapes de procédure et délais analysés

> ⚠️ **Validation juridique requise** : les articles du Code judiciaire belge (`art. 1207 à 1224`, `art. 1218` pour les contredits) et le délai d'un mois pour les contredits reflètent l'état du droit connu du modèle et la réforme de la procédure de liquidation-partage (loi du 13/08/2011, en vigueur 01/04/2012). Ils sont **à valider par un avocat belge avant mise en production**. Le contenu juridique (articles, séquence, délais) est centralisé dans le Calculator (source unique de vérité).

### Séquence des étapes (checklist de procédure)

| Ordre | Code | Étape | Fondement (à valider) | Marquée FAITE si |
|-------|------|-------|------------------------|-------------------|
| 1 | `DESIGNATION_NOTAIRE` | Désignation du notaire-liquidateur par le Tribunal de la famille | CJ art. 1207-1209 | `notaireDesigne = true` |
| 2 | `OUVERTURE_OPERATIONS` | Ouverture des opérations / inventaire des biens | CJ art. 1210-1213 | `operationsOuvertes = true` |
| 3 | `INVENTAIRE` | Établissement de l'inventaire du patrimoine | CJ art. 1213 | `inventaireEtabli = true` |
| 4 | `PROJET_LIQUIDATION` | Établissement de l'état liquidatif / projet de partage par le notaire | CJ art. 1214 | `projetLiquidationEtabli = true` |
| 5 | `NOTIFICATION_PROJET` | Notification du projet aux parties — point de départ du délai de contredits | CJ art. 1218 | `dateNotificationProjet` renseignée |
| 6 | `CONTREDITS` | Dépôt des contredits dans le mois de la notification | CJ art. 1218 | `contreditsDeposes = true` OU délai d'un mois écoulé |
| 7 | `PROCES_VERBAL_DIRES` | Établissement du procès-verbal de dires et difficultés par le notaire | CJ art. 1218 | `procesVerbalDiresEtabli = true` |
| 8 | `HOMOLOGATION_TF` | Homologation de l'état liquidatif par le Tribunal de la famille | CJ art. 1223-1224 | `homologationDemandee = true` ET `dateHomologation` renseignée |

Règle de statut : toutes les étapes antérieures à la dernière étape `true` sont `FAITE` ; l'étape immédiatement suivante est `EN_COURS` ; le reste est `A_VENIR`. Une étape `true` dont une étape antérieure est `false` produit un message d'anomalie de séquence (la procédure n'est pas linéaire dans le dossier).

### Délais critiques

| Code | Délai | Point de départ | Durée | Fondement (à valider) |
|------|-------|------------------|-------|------------------------|
| `DELAI_CONTREDITS` | Délai pour déposer les contredits sur le projet de liquidation-partage | `dateNotificationProjet` | 1 mois (date à date) | CJ art. 1218 |

`joursRestants` = jours calendaires entre la date du jour et l'échéance. `statut` : `NON_DEMARRE` si pas de notification ; `OK` si > 10 jours ; `CRITIQUE` si ≤ 10 jours et non dépassé ; `DEPASSE` si l'échéance est passée et `contreditsDeposes = false`.

**Calcul du verdict** : `notaireDesigne = false` → `PROCEDURE_NON_ENGAGEE`. Sinon, `dateHomologation` renseignée → `CLOTUREE`. Sinon, `DELAI_CONTREDITS` en statut `CRITIQUE` ou `DEPASSE` → `DELAI_CONTREDITS_CRITIQUE`. Sinon, `procesVerbalDiresEtabli = true` → `EN_ATTENTE_HOMOLOGATION`. Sinon → `EN_COURS`. Logique figée dans le Calculator, testée.

---

## Conformité F-IA-04
- [x] **Non applicable au sens strict** — SF backend pure. La conformité F-IA-04 (TOOL_REGISTRY, pré-fill, F-IA-03, gate `workspaceCountry`) est portée par SF-217-03 frontend. Le seed `decision_tool_visibility_rules` est lui aussi porté par SF-217-03 (migration 235), couplé à l'entrée TOOL_REGISTRY dans le même lot — un seed sans entrée frontend ferait échouer le garde-fou `DecisionToolVisibilityIntegrityIT` (précédent SF-211-05 / SF-DT-36-02).

---

## Critères d'acceptation

- [ ] `POST` calcule et persiste un résultat ; recalcul écrase le précédent (upsert 1:1 par dossier).
- [ ] Les 8 étapes sont positionnées `FAITE` / `EN_COURS` / `A_VENIR` selon la règle de séquence.
- [ ] Le délai de contredits (1 mois à compter de la notification) est calculé avec son échéance, ses `joursRestants` et son `statut`.
- [ ] Une anomalie de séquence (étape `true` avec étape antérieure `false`) produit un message explicite.
- [ ] Verdict piloté selon la règle : non engagée / clôturée / délai critique / en attente d'homologation / en cours.
- [ ] La réponse `POST` / `GET` ré-expose l'intégralité des inputs (formulaire ré-éditable — leçon F-DT-36).
- [ ] `GET` renvoie le dernier résultat ou 404.
- [ ] `400` (date mal formée, date incohérente, date manquante alors que l'étape est `true`) ; `403` workspace différent ; `404` dossier inexistant ; `422` domaine ≠ `DROIT_FAMILLE` ou pays ≠ `BELGIQUE` ; `401` non authentifié.
- [ ] Isolation workspace testée.

---

## Périmètre

### Hors scope
- Frontend (SF-217-03).
- Génération du document d'état liquidatif (l'audit F-191 § 3.5 classe le « générateur » comme extension — non couvert en V1, l'outil est une checklist + délais).
- Liquidation-partage successoral (`succession-be-partage-judiciaire` est un outil distinct — audit F-191 § 3.6, vague ultérieure).
- Pré-fill IA depuis l'analyse (aucun flag pivot dédié extrait par le pipeline V1 — saisie manuelle ; documenté `PREFILL_COUNT_ALWAYS_ZERO` côté SF-217-03).
- Réutilisation des Calculators FR (F-FA-17 liquidation-partage FR — procédure du notaire désigné distincte de la procédure belge du notaire commis).
- Seed `decision_tool_visibility_rules` (porté par SF-217-03, migration 235).

---

## Technique

### Endpoints
| Méthode | URL | Auth | Rôle |
|---------|-----|------|------|
| POST | `/api/v1/case-files/{caseFileId}/liquidation-partage-be` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/liquidation-partage-be` | Oui | MEMBER |

### Tables impactées
| Table | Opération | Notes |
|-------|-----------|-------|
| `liquidation_partage_be_analyses` | CREATE + INSERT/UPDATE/SELECT | id (UUID), case_file_id (FK UNIQUE), country (VARCHAR), snapshot_data (TEXT JSON — inputs + résultat calculé), created_at, updated_at |
| `decision_tool_visibility_rules` | INSERT (seed) | porté par SF-217-03 (migration 235), couplé à l'entrée TOOL_REGISTRY |

### Migration Liquibase
- [x] Oui — `234-create-liquidation-partage-be-analyses.xml` (table seule ; le seed `decision_tool_visibility_rules` est porté par SF-217-03 — migration 235)

> Note de renumérotation (2026-05-17) : la migration de cette SF était initialement numérotée `233`, numéro réservé à SF-217-01 (chantier parallèle). Renumérotée en `234` lors de la finalisation backend. Le seed `decision_tool_visibility_rules` de SF-217-03 glisse en conséquence de `234` à `235`.

### Classes backend (pattern `DivorceDcBe*` / `ProcedureNulliteLicenciement*`)
`LiquidationPartageBeCalculator` (static), `LiquidationPartageBeInput`, `LiquidationPartageBeResult`, `LiquidationPartageBeRequest`, `LiquidationPartageBeResponse`, `LiquidationPartageBeAnalysis` (@Entity), `LiquidationPartageBeRepository`, `LiquidationPartageBeService`, `LiquidationPartageBeController`.

---

## Plan de test

### Tests unitaires (Calculator)
- [ ] Notaire non désigné → `PROCEDURE_NON_ENGAGEE`, étape 1 `EN_COURS`.
- [ ] Procédure jusqu'au projet notifié, délai > 10 j → `EN_COURS`, délai `OK`.
- [ ] Projet notifié il y a 25 jours → `DELAI_CONTREDITS_CRITIQUE`, délai `CRITIQUE`, `joursRestants` correct.
- [ ] Projet notifié il y a 40 jours, contredits non déposés → `DELAI_CONTREDITS_CRITIQUE`, délai `DEPASSE`.
- [ ] Procès-verbal de dires établi, pas d'homologation → `EN_ATTENTE_HOMOLOGATION`.
- [ ] Homologation prononcée → `CLOTUREE`, toutes étapes `FAITE`.
- [ ] Étape avancée `true` avec étape antérieure `false` → message d'anomalie de séquence.
- [ ] Calcul du délai d'un mois date à date (gestion des mois de longueurs différentes).

### Tests d'intégration (Controller)
- [ ] `POST` → 200 + persistance ; recalcul écrase.
- [ ] `GET` → 200 / 404 si jamais calculé.
- [ ] `400` (date mal formée, date incohérente, date manquante pour une étape `true`) / `403` / `404` / `422` (domaine ≠ famille, pays ≠ Belgique) / `401`.

### Isolation workspace
- [x] Applicable — un utilisateur du workspace A ne peut pas calculer/lire le résultat d'un dossier du workspace B.

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Outil décisionnel métier** — `liquidation-partage-be` est un nouvel outil décisionnel. Scan des outils patrimoine / partage fait dans `SF-217-00-coherence.md` : aucun outil BE existant ne couvre la procédure du notaire commis (F-FA-17 est FR-only, masqué en BE ; F-FA-05 partage immobilier est un calcul de quote-part neutre, pas une procédure ; SF-217-01 `regime-mat-be-communaute-legale` qualifie la composition du patrimoine, situation distincte). `liquidation-partage-be` = **une situation métier distincte** (la procédure post-dissolution).
- [x] Aucune autre préoccupation transversale (auth/workspace/plans/navigation non modifiés).

### Smoke tests E2E
- [x] Aucun — feature additive (nouvel endpoint).

---

## Dépendances
- Aucune SF bloquante. SF-217-03 (frontend) importe le contrat API ci-dessus. Dev backend/frontend parallélisable (contrat figé). Indépendante de SF-217-01.

---

## Notes et décisions
- Persistance par snapshot JSON (`snapshot_data`) — pattern `DivorceDcBeAnalysis` / `ProcedureNulliteLicenciementAnalysis`, évite une colonne par étape.
- Aucune réutilisation du Calculator FR : la liquidation-partage belge repose sur le notaire-liquidateur **commis** par le Tribunal de la famille, avec projet de liquidation, contredits dans le mois et procès-verbal de dires (CJ art. 1207+) — procédure structurellement distincte de la liquidation FR. Outil bâti depuis les sources belges (`feedback_belgique_never_forget`).
- Le délai de contredits est calculé en date à date (1 mois) ; les jours restants en jours calendaires — décision documentée ici, à valider juridiquement (le caractère franc ou non du délai est un point à confirmer par un avocat belge).
- Le contenu juridique (articles CJ, séquence des étapes, délai de contredits) est centralisé dans le Calculator et signalé pour validation par un avocat belge avant prod.
</content>
