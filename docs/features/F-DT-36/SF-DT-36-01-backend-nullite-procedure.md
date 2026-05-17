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

Réponse `200` — `ProcedureNulliteLicenciementResponse` :
```json
{
  "caseFileId": "uuid",
  "verdict": "NULLITE_PROBABLE",
  "scoreNullite": 55,
  "vicesDetectes": [
    {
      "code": "MOTIVATION_INSUFFISANTE",
      "libelle": "Motivation de la lettre insuffisante",
      "fondement": "Art. L.1232-6 et L.1235-2 C. trav.",
      "gravite": "AVERE",
      "explication": "Une lettre non motivée ou insuffisamment motivée rend le licenciement sans cause réelle et sérieuse."
    }
  ],
  "basesJuridiques": ["Art. L.1232-2 C. trav.", "Art. L.1232-6 C. trav."],
  "messages": ["Vérifier la disponibilité de la lettre de convocation au dossier (Pièce)."],
  "calculatedAt": "2026-05-17T10:00:00Z"
}
```

### GET `/api/v1/case-files/{caseFileId}/procedure-nullite-licenciement`
- `200` : dernier résultat (même structure). `404` si jamais calculé. `403` / `401` idem POST.

### Enum `verdict`
`NULLITE_AVEREE` (≥ 1 vice avéré grave) · `NULLITE_PROBABLE` (vices probables, pièces à confirmer) · `PROCEDURE_REGULIERE` (aucun vice).

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

**Calcul du verdict** : score = somme pondérée des vices (vice grave = 30, vice modéré = 20). `scoreNullite ≥ 60` → `NULLITE_AVEREE` ; `25–59` → `NULLITE_PROBABLE` ; `< 25` → `PROCEDURE_REGULIERE`. Barème exact figé dans le Calculator, testé.

> ⚠️ **Validation juridique requise** : les fondements légaux, les délais (5 jours ouvrables L.1232-2, 2 jours ouvrables L.1232-6) et le barème de score doivent être relus par un avocat avant mise en production. Les valeurs ci-dessus reflètent l'état du droit connu ; toute correction se fait dans le Calculator (source unique).

---

## Frontière avec F-DT-08 (ajustement du cadrage cohérence)

F-DT-08 (validité du licenciement) conserve la vérification **sommaire** du motif et de la forme générale. **F-DT-36 est l'outil dédié et approfondi des vices de procédure** : c'est lui la référence pour les 10 critères ci-dessus. F-DT-08 n'est pas modifié par cette SF. Les deux outils coexistent comme simulateurs indépendants (modèle produit `feedback_decision_tools_are_simulators`) — pas d'override, pas de fusion. La mini-spec ne touche pas F-DT-08.

---

## Conformité F-IA-04
- [x] **Non applicable au sens strict** — SF backend pure. La conformité F-IA-04 (TOOL_REGISTRY, pré-fill, F-IA-03) est portée par SF-DT-36-02 frontend. Côté backend : l'outil expose un endpoint POST décisionnel + seed `decision_tool_visibility_rules` (ci-dessous).

---

## Critères d'acceptation

- [ ] `POST` calcule et persiste un résultat ; recalcul écrase le précédent.
- [ ] Les 10 vices sont détectés selon les règles du tableau.
- [ ] Verdict `NULLITE_AVEREE` / `NULLITE_PROBABLE` / `PROCEDURE_REGULIERE` cohérent avec le score.
- [ ] `GET` renvoie le dernier résultat ou 404.
- [ ] `400` si date mal formée, `403` workspace différent, `404` dossier inexistant, `422` domaine ≠ `DROIT_DU_TRAVAIL`, `401` non authentifié.
- [ ] Isolation workspace testée.
- [ ] Seed `decision_tool_visibility_rules` : `F-DT-36-procedure-nullite-licenciement`, `DROIT_DU_TRAVAIL` / `FRANCE`, `CONTEXTUAL`, déclenché par licenciement détecté.

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
| `decision_tool_visibility_rules` | INSERT (seed) | entrée CONTEXTUAL F-DT-36 |

### Migration Liquibase
- [x] Oui — `230-create-procedure-nullite-licenciement-analyses.xml` (table + seed visibility rule)

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
