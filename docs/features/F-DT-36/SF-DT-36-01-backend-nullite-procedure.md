# Mini-spec — F-DT-36 / SF-DT-36-01 — Backend : analyse des nullités de procédure de licenciement

## Identifiant
`F-DT-36 / SF-DT-36-01`

## Feature parente
`F-DT-36` — Analyse des nullités de procédure de licenciement (vices côté employeur)

## Statut
`ready`

## Date de création
2026-05-17

## Branche Git
`feat/SF-DT-36-01-backend`

---

## Objectif

Fournir un outil décisionnel backend qui analyse les vices de forme de la procédure de licenciement (FR) et rend un verdict de nullité, à partir des éléments du dossier.

---

## Comportement attendu

### Cas nominal

1. L'avocat soumet les éléments de la procédure (dates de convocation / entretien / notification, présence et motivation de la lettre, motif grave, licenciement collectif, convention collective).
2. Le `ProcedureNulliteLicenciementCalculator` détecte les vices parmi les 10 critères, calcule un score et un verdict.
3. Le résultat est persisté par dossier (un seul résultat courant par dossier, écrasé au recalcul) et renvoyé.

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|--------------|-----------|
| Champ obligatoire absent / date mal formée | Message explicite | 400 |
| Dossier inexistant | — | 404 |
| Dossier d'un autre workspace | Accès refusé | 403 |
| Dossier dont le `legalDomain` n'est pas `DROIT_DU_TRAVAIL` | Message explicite | 422 |
| Non authentifié | — | 401 |
| `GET` sans calcul préalable | — | 404 |

---

## Contrat API (FIGÉ — importé par SF-DT-36-02)

### POST `/api/v1/case-files/{caseFileId}/procedure-nullite-licenciement`

Body `ProcedureNulliteLicenciementRequest` :
```json
{
  "convocationEnvoyee": true,
  "dateConvocationPresentee": "2025-01-06",
  "dateEntretienPrealable": "2025-01-13",
  "entretienTenu": true,
  "dateNotificationLicenciement": "2025-01-17",
  "lettreLicenciementEcrite": true,
  "lettreMotivee": true,
  "motivationSuffisante": false,
  "motivationCommentaire": "Motif vague : « insuffisance » sans faits précis",
  "licenciementPourMotifGrave": false,
  "licenciementCollectif": false,
  "procedureCseRespectee": null,
  "conventionCollectiveApplicable": true,
  "conventionCollectiveRespectee": true,
  "conventionCollectiveCommentaire": null
}
```
- Booleans obligatoires ; dates `yyyy-MM-dd` nullable ; commentaires nullable (max 1000).

Réponse `200` — `ProcedureNulliteLicenciementResponse` : **ré-expose l'intégralité des champs du body de requête** (snapshot pour pré-remplissage / ré-édition du formulaire) **+** les champs calculés ci-dessous.
```json
{
  "caseFileId": "uuid",
  "convocationEnvoyee": true,
  "dateConvocationPresentee": "2025-01-06",
  "dateEntretienPrealable": "2025-01-13",
  "entretienTenu": true,
  "dateNotificationLicenciement": "2025-01-17",
  "lettreLicenciementEcrite": true,
  "lettreMotivee": true,
  "motivationSuffisante": false,
  "motivationCommentaire": "Motif vague : « insuffisance » sans faits précis",
  "licenciementPourMotifGrave": false,
  "licenciementCollectif": false,
  "procedureCseRespectee": null,
  "conventionCollectiveApplicable": true,
  "conventionCollectiveRespectee": true,
  "conventionCollectiveCommentaire": null,
  "verdict": "NULLITE_PROBABLE",
  "scoreNullite": 20,
  "vicesDetectes": [
    {
      "code": "MOTIVATION_INSUFFISANTE",
      "libelle": "Motivation de la lettre insuffisante",
      "fondement": "Art. L.1232-6 et L.1235-2 C. trav.",
      "gravite": "PROBABLE",
      "explication": "Une lettre dont les motifs sont imprécis ou non matériellement vérifiables peut rendre le licenciement sans cause réelle et sérieuse — appréciation soumise au juge du fond."
    }
  ],
  "basesJuridiques": ["Art. L.1232-6 et L.1235-2 C. trav."],
  "messages": ["Nullité probable : un ou plusieurs vices sont identifiés — confirmer les pièces du dossier avant d'arbitrer la stratégie contentieuse."],
  "country": "FRANCE",
  "calculatedAt": "2026-05-17T10:00:00Z"
}
```

### GET `/api/v1/case-files/{caseFileId}/procedure-nullite-licenciement`
- `200` : dernier résultat (même structure, inputs inclus → le formulaire est ré-éditable). `404` si jamais calculé. `403` / `401` idem POST.

### Enum `verdict`
`NULLITE_AVEREE` (≥ 1 vice de gravité AVERE) · `NULLITE_PROBABLE` (aucun vice AVERE mais ≥ 1 vice PROBABLE, à confirmer par pièce) · `PROCEDURE_REGULIERE` (aucun vice).

### Enum `gravite` d'un vice
`AVERE` (vice caractérisé) · `PROBABLE` (présomption, à confirmer par pièce).

---

## Les 10 critères de vice

| Code | Vice | Fondement | Règle de détection |
|------|------|-----------|--------------------|
| `ABSENCE_CONVOCATION` | Pas de convocation à l'entretien préalable | L.1232-2 | `convocationEnvoyee = false` |
| `DELAI_CONVOCATION_INSUFFISANT` | Entretien tenu moins de 5 jours ouvrables après présentation de la convocation | L.1232-2 | délai (`dateConvocationPresentee` → `dateEntretienPrealable`) < 5 jours ouvrables |
| `ABSENCE_ENTRETIEN` | Entretien préalable non tenu | L.1232-2 | `entretienTenu = false` |
| `NOTIFICATION_PREMATUREE` | Lettre de licenciement envoyée moins de 2 jours ouvrables après l'entretien | L.1232-6 | délai (`dateEntretienPrealable` → `dateNotificationLicenciement`) < 2 jours ouvrables |
| `ABSENCE_LETTRE` | Pas de lettre de licenciement écrite | L.1232-6 | `lettreLicenciementEcrite = false` |
| `LETTRE_NON_MOTIVEE` | Lettre sans énoncé des motifs | L.1232-6 | `lettreMotivee = false` |
| `MOTIVATION_INSUFFISANTE` | Motivation imprécise / non matériellement vérifiable | L.1232-6, L.1235-2 | `lettreMotivee = true` ET `motivationSuffisante = false` |
| `ABSENCE_CONVOCATION_MOTIF_GRAVE` | Faute grave/lourde sans procédure de convocation respectée | L.1234-9, L.1332-2 | `licenciementPourMotifGrave = true` ET (`ABSENCE_CONVOCATION` OU `ABSENCE_ENTRETIEN`) |
| `PROCEDURE_CSE_NON_RESPECTEE` | Procédure d'information/consultation du CSE non respectée (licenciement collectif) | L.1233-8, L.1233-28 et s. | `licenciementCollectif = true` ET `procedureCseRespectee = false` |
| `CONVENTION_COLLECTIVE_NON_RESPECTEE` | Procédure conventionnelle (préalable disciplinaire, commission paritaire…) non respectée | clause de la CCN applicable | `conventionCollectiveApplicable = true` ET `conventionCollectiveRespectee = false` |

**Gravité des vices** : 8 vices reposent sur des faits objectifs (dates, absence d'acte) → gravité `AVERE`. 2 vices reposent sur une appréciation à confirmer par pièce → gravité `PROBABLE` : `MOTIVATION_INSUFFISANTE` (la suffisance de la motivation relève de l'appréciation du juge du fond) et `CONVENTION_COLLECTIVE_NON_RESPECTEE` (l'existence et la portée de la procédure conventionnelle dépendent de la clause exacte de la CCN).

**Calcul du verdict** : le verdict est **piloté par la gravité des vices**, non par un seuil de score additif — juridiquement, un seul vice substantiel avéré suffit à caractériser l'irrégularité de la procédure. Règle : `≥ 1 vice AVERE` → `NULLITE_AVEREE` ; sinon `≥ 1 vice PROBABLE` → `NULLITE_PROBABLE` ; sinon `PROCEDURE_REGULIERE`. Le `scoreNullite` (somme pondérée : vice AVERE = 30, vice PROBABLE = 20, plafonné à 100) n'est qu'un **indicateur secondaire d'ampleur du cumul**, affiché à titre informatif — il ne décide pas du verdict. Logique figée dans le Calculator, testée.

> ⚠️ **Validation juridique requise** : les fondements légaux, les délais (5 jours ouvrables L.1232-2, 2 jours ouvrables L.1232-6), la classification de gravité des vices et le barème de score doivent être relus par un avocat avant mise en production. Les valeurs ci-dessus reflètent l'état du droit connu ; toute correction se fait dans le Calculator (source unique).

---

## Frontière avec F-DT-08 (ajustement du cadrage cohérence)

F-DT-08 (validité du licenciement) conserve la vérification **sommaire** du motif et de la forme générale. **F-DT-36 est l'outil dédié et approfondi des vices de procédure** : c'est lui la référence pour les 10 critères ci-dessus. F-DT-08 n'est pas modifié par cette SF. Les deux outils coexistent comme simulateurs indépendants (modèle produit `feedback_decision_tools_are_simulators`) — pas d'override, pas de fusion. La mini-spec ne touche pas F-DT-08.

---

## Conformité F-IA-04
- [x] **Non applicable au sens strict** — SF backend pure. La conformité F-IA-04 (TOOL_REGISTRY, pré-fill, F-IA-03) est portée par SF-DT-36-02 frontend. Le seed `decision_tool_visibility_rules` est lui aussi porté par SF-DT-36-02 (migration 231), couplé à l'entrée TOOL_REGISTRY dans le même lot — un seed sans entrée frontend ferait échouer le garde-fou `DecisionToolVisibilityIntegrityIT` (précédent SF-211-05).

---

## Critères d'acceptation

- [ ] `POST` calcule et persiste un résultat ; recalcul écrase le précédent.
- [ ] Les 10 vices sont détectés selon les règles du tableau, avec la bonne gravité (8 `AVERE`, 2 `PROBABLE`).
- [ ] Verdict piloté par la gravité : `≥ 1 vice AVERE` → `NULLITE_AVEREE` ; sinon `≥ 1 vice PROBABLE` → `NULLITE_PROBABLE` ; sinon `PROCEDURE_REGULIERE`.
- [ ] La réponse `POST` / `GET` ré-expose l'intégralité des inputs (formulaire ré-éditable).
- [ ] `GET` renvoie le dernier résultat ou 404.
- [ ] `400` si date mal formée, `403` workspace différent, `404` dossier inexistant, `422` domaine ≠ `DROIT_DU_TRAVAIL`, `401` non authentifié.
- [ ] Isolation workspace testée.

---

## Périmètre

### Hors scope
- Frontend (SF-DT-36-02).
- Pré-fill IA depuis l'analyse (les flags procéduraux dédiés ne sont pas extraits par le pipeline en V1 — saisie manuelle ; extension IA = SF ultérieure si besoin).
- Outil jumeau Belgique (backlog post-livraison FR).
- Modification de F-DT-08.

---

## Technique

### Endpoints
| Méthode | URL | Auth | Rôle |
|---------|-----|------|------|
| POST | `/api/v1/case-files/{caseFileId}/procedure-nullite-licenciement` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/procedure-nullite-licenciement` | Oui | MEMBER |

### Tables impactées
| Table | Opération | Notes |
|-------|-----------|-------|
| `procedure_nullite_licenciement_analyses` | CREATE + INSERT/UPDATE/SELECT | id, case_file_id (FK unique), snapshot_data (TEXT JSON), country, created_at, updated_at |
| `decision_tool_visibility_rules` | INSERT (seed) | porté par SF-DT-36-02 (migration 231), couplé à l'entrée TOOL_REGISTRY |

### Migration Liquibase
- [x] Oui — `230-create-procedure-nullite-licenciement-analyses.xml` (table seule ; le seed `decision_tool_visibility_rules` est porté par SF-DT-36-02 — migration 231)

### Classes backend (pattern F-DT-16)
`ProcedureNulliteLicenciementCalculator` (static), `ProcedureNulliteLicenciementInput`, `ProcedureNulliteResult`, `ProcedureNulliteLicenciementRequest`, `ProcedureNulliteLicenciementResponse`, `ProcedureNulliteLicenciementAnalysis` (@Entity), `ProcedureNulliteLicenciementRepository`, `ProcedureNulliteLicenciementService`, `ProcedureNulliteLicenciementController`.

---

## Plan de test

### Tests unitaires (Calculator)
- [ ] Procédure régulière → `PROCEDURE_REGULIERE`, 0 vice.
- [ ] Chaque vice isolé détecté (10 cas).
- [ ] Délai convocation < 5 jours ouvrables → `DELAI_CONVOCATION_INSUFFISANT` (avec jours fériés/week-ends).
- [ ] Cumul de vices graves → `NULLITE_AVEREE`.
- [ ] Faute grave + absence convocation → `ABSENCE_CONVOCATION_MOTIF_GRAVE`.

### Tests d'intégration (Controller)
- [ ] `POST` → 200 + persistance ; recalcul écrase.
- [ ] `GET` → 200 / 404 si jamais calculé.
- [ ] 400 / 403 / 404 / 422 / 401.

### Isolation workspace
- [x] Applicable — un utilisateur du workspace A ne peut pas calculer/lire le résultat d'un dossier du workspace B.

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Outil décisionnel métier** — F-DT-36 est un nouvel outil décisionnel. Scan des outils licenciement existants fait dans `SF-DT-36-00-coherence.md` : F-DT-08 (validité motif), F-DT-16 (protections salarié) — F-DT-36 est une **situation métier distincte** (vices de procédure côté employeur), pas de mélange. Frontière F-DT-08 traitée ci-dessus.
- [x] Aucune autre préoccupation transversale (auth/workspace/plans/navigation non modifiés).

### Smoke tests E2E
- [x] Aucun — feature additive.

---

## Dépendances
- Aucune SF bloquante. SF-DT-36-02 (frontend) importe le contrat API ci-dessus.

---

## Notes et décisions
- Persistance par snapshot JSON (`snapshot_data`) — pattern F-DT-16, évite une colonne par critère.
- Verdict à 3 niveaux aligné sur F-DT-16 (cohérence inter-outils licenciement).
- Le contenu juridique (délais, fondements, barème) est centralisé dans le Calculator et signalé pour validation juridique avant prod.
