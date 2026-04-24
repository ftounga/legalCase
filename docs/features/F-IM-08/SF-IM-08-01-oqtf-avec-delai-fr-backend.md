# Mini-spec — F-IM-08 / SF-IM-08-01 OQTF avec délai de départ volontaire FR — BACKEND

## Identifiant
`F-IM-08 / SF-IM-08-01`

## Feature parente
`F-IM-08` — OQTF et contentieux éloignement (FR + BE) — 🔴 critique absolue

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-IM-08-01-oqtf-avec-delai-fr-backend`

---

## Objectif

**Premier SF d'un gros chantier 6-8 SFs.** Outil décisionnel dédié à l'OQTF assortie d'un délai de départ volontaire de 30 jours (art. L.614-5 CESEDA) — cas le plus fréquent en contentieux éloignement. Calcule les délais de recours devant le TA, identifie si le délai est bientôt expiré, rappelle les référés disponibles (L.521-1 suspension, L.521-2 liberté).

**Scope FR only** : l'OQTF française (L.614-5) et l'OQT belge (annexe 13) sont des procédures juridiquement distinctes. BE sera traité en SF-IM-08-05 séparée, conformément à l'invariant "un outil = une situation métier".

---

## Comportement

### Règles (CESEDA)

- **L.614-5** : OQTF avec délai de départ volontaire de 30 jours (jours calendaires).
- **L.614-5 al. 2** : recours suspensif devant TA dans les 30 jours (jours francs, R.776-18 CJA).
- **Audience TA** : dans les 3 mois du recours (art. R.776-13-4 CJA).
- **Décision TA** : dans les 6 mois (art. R.776-14 CJA).
- **Effet suspensif du recours** : l'éloignement ne peut être exécuté tant que le TA n'a pas statué.

### Inputs

- `dateNotificationOqtf` : LocalDate (jour de réception de l'OQTF par le ressortissant)
- `motifOqtf` : enum :
  - `REFUS_TITRE` (L.611-1 1° — refus de délivrance/renouvellement titre)
  - `EXPIRATION_TITRE` (L.611-1 3° — titre non renouvelé à échéance)
  - `SEJOUR_IRREGULIER` (L.611-1 2° — séjour irrégulier sans demande en cours)
  - `RETRAIT_TITRE` (L.611-1 4° — retrait du titre)
  - `AUTRE`
- `recoursFormé` : boolean (le recours a-t-il été introduit ?)
- `dateRecours` : LocalDate nullable (date de dépôt si recours formé)

### Outputs

- `dateExpirationDdv` : `dateNotificationOqtf + 30 jours` (jours calendaires)
- `dateExpirationDelaiRecours` : `dateNotificationOqtf + 30 jours` (jours francs — pour simplification aligné sur 30 jours calendaires, note dans message)
- `joursRestantsAvantExpirationDelai` : int (si recours pas formé, sinon 0)
- `statutDelaiRecours` : enum :
  - `DISPONIBLE` (> 7 jours restants)
  - `URGENT` (≤ 7 jours restants mais pas expiré)
  - `EXPIRE` (délai dépassé, pas de recours formé)
  - `RECOURS_FORME` (recours déjà introduit)
- `dateAudiencePrevisionnelle` : `dateRecours + 90 jours` (si recours formé)
- `dateDecisionTaPrevisionnelle` : `dateRecours + 180 jours` (si recours formé)
- `referedDisponibles` : array of codes :
  - `REFERE_SUSPENSION_L521_1` (toujours si recours OQTF en cours)
  - `REFERE_LIBERTE_L521_2` (si urgence caractérisée — mentionné dans messages)
- `formule` : texte
- `baseJuridique` : "Art. L.614-5, L.614-6, R.776-18 CJA"
- `messages` : rappels essentiels

### Messages standards

- "Recours devant le TA suspensif (art. L.614-5 al. 2 CESEDA) — l'éloignement ne peut être exécuté tant que le TA n'a pas statué."
- "Référé suspension (art. L.521-1 CJA) disponible en complément si urgence."
- "Référé liberté (art. L.521-2 CJA) disponible si atteinte grave à une liberté fondamentale — décision en 48h."
- "Délai de 30 jours = jours francs (R.776-18 CJA) : le jour de notification et le jour d'échéance ne comptent pas. Compter exactement en prenant une marge de sécurité."
- "Droit à interprète (R.614-5) et droit à l'assistance d'un conseil — mentionner sur le recours."
- Si motif `REFUS_TITRE` : "L'OQTF est motivée par le refus de titre — contester les deux décisions simultanément (L.614-6)."
- Si motif `SEJOUR_IRREGULIER` : "Pas de décision administrative préalable à contester. Focus sur l'absence de menace à l'ordre public et les liens personnels/familiaux."

### Cas d'erreur
| Situation | HTTP |
|---|---|
| dateNotificationOqtf dans le futur | 400 |
| motifOqtf null ou inconnu | 400 |
| dateRecours avant dateNotificationOqtf | 400 |
| recoursFormé=true sans dateRecours | 400 |
| Workspace BELGIQUE | 400 "OQTF procédure FR uniquement — en Belgique voir SF-IM-08-05 (Annexe 13)" |
| Dossier autre domaine (DROIT_DU_TRAVAIL / DROIT_FAMILLE) | 400 "Ce dossier n'est pas un dossier de droit de l'immigration" |
| Workspace étranger | 404 |

---

## Contrat API

### POST `/api/v1/case-files/{caseFileId}/oqtf-avec-delai`

**Request :**
```json
{
  "dateNotificationOqtf": "2026-04-01",
  "motifOqtf": "REFUS_TITRE",
  "recoursFormé": false,
  "dateRecours": null
}
```

**Response :**
```json
{
  "caseFileId": "uuid",
  "dateNotificationOqtf": "2026-04-01",
  "motifOqtf": "REFUS_TITRE",
  "recoursFormé": false,
  "dateRecours": null,
  "country": "FRANCE",
  "dateExpirationDdv": "2026-05-01",
  "dateExpirationDelaiRecours": "2026-05-01",
  "joursRestantsAvantExpirationDelai": 12,
  "statutDelaiRecours": "DISPONIBLE",
  "dateAudiencePrevisionnelle": null,
  "dateDecisionTaPrevisionnelle": null,
  "referedDisponibles": ["REFERE_SUSPENSION_L521_1", "REFERE_LIBERTE_L521_2"],
  "formule": "…",
  "baseJuridique": "Art. L.614-5, L.614-6, R.776-18 CJA",
  "messages": ["…"]
}
```

---

## Architecture

Pattern standard. Table `oqtf_avec_delai_analyses` (migration 116). Tool_id `F-IM-08-oqtf-avec-delai-fr`. **1 règle visibility CONTEXTUAL** FR sur trigger `type_procedure_detectee = OQTF_AVEC_DELAI` (cohérent avec F-IA-04 architecture) — UUID `f1a04001-0000-0000-0000-ee000000081a`, priority 57.

### Composants
- `OqtfAvecDelaiCalculator.java`
- `OqtfAvecDelaiAnalysis.java` (entity)
- `OqtfAvecDelaiRepository.java`
- `OqtfAvecDelaiRequest/Response/Result.java`
- `OqtfAvecDelaiService.java` (gate DROIT_IMMIGRATION + country==FRANCE)
- `OqtfAvecDelaiController.java`
- Migration `116-create-oqtf-avec-delai-analyses.xml`

### Entity colonnes
- id uuid PK
- case_file_id uuid UNIQUE FK
- date_notification_oqtf date
- motif_oqtf varchar(40)
- recours_forme boolean
- date_recours date nullable
- country varchar(20)
- result_data text
- timestamps

### Tests
- `OqtfAvecDelaiCalculatorTest` (UT)
- `OqtfAvecDelaiControllerIT` (IT)

---

## Plan de test

### UT

- 4 motifs testés (REFUS_TITRE, EXPIRATION_TITRE, SEJOUR_IRREGULIER, RETRAIT_TITRE, AUTRE) → messages différenciés
- Calcul dateExpirationDdv = notification + 30 jours calendaires
- Calcul joursRestants décroissants au fil du temps
- Statut DISPONIBLE / URGENT / EXPIRE / RECOURS_FORME selon conditions
- dateAudience et dateDecision null si pas de recours, sinon +90j et +180j
- referedDisponibles toujours présents
- dateNotification future → IllegalArgumentException
- dateRecours avant notification → IllegalArgumentException
- recoursFormé=true sans dateRecours → IllegalArgumentException

### IT

- POST FR nominal → 200
- POST BE workspace → 400
- POST dossier DROIT_DU_TRAVAIL → 400
- POST workspace étranger → 404
- Upsert (2 POST)
- GET après POST
- GET sans POST → 404

---

## Impact par domaine

DROIT_IMMIGRATION FR uniquement. BE couvert par SF-IM-08-05 (annexe 13, procédure juridiquement distincte — Conseil du contentieux des étrangers, délais et vocabulaire belges). Justification : OQTF française et OQT belge sont **des situations métier distinctes** — invariant "un outil = une situation" respecté.

## Parité niveau ≥5

Niveau 4 (arbre décisionnel basique + calc délais). Parité ≥5 N/A.

## Analyse de cohérence transversale

- [x] **F-IM-16** : enum `OQTF_AVEC_DELAI` déjà ajouté (SF-IM-16-01 mergée PR #487) + jalons créés dans migration 108. Règle visibility CONTEXTUAL peut déclencher ce nouvel outil quand l'IA détecte `type_procedure_detectee = OQTF_AVEC_DELAI`.
- [x] **F-IM-05/06/07** : outils existants immigration, pas impactés (cible différente).
- [x] **F-IA-04** : architecture déclarative respectée — règle visibility ajoutée, pas de code dans le moteur.

## Hors scope

- Référé suspension (L.521-1) comme outil distinct → SF-IM-08-07 ultérieure.
- Référé liberté (L.521-2) → SF-IM-08-08 ultérieure.
- Appel CAA → SF-IM-08-09 ultérieure.
- Annexe 13 BE → SF-IM-08-05.
- OQTF sans délai → SF-IM-08-03 / SF-IM-08-04 (procédure 48h JLD distincte).
- Frontend → SF-IM-08-02 (parallèle).
- Détection IA du motifOqtf → hors scope (l'avocat saisit).

## Critères d'acceptation

- [ ] Calculator 5 motifs + messages différenciés par motif
- [ ] Calcul expirationDdv et expirationRecours = notif + 30j
- [ ] joursRestants correct par rapport à `LocalDate.now()`
- [ ] statutDelaiRecours selon 4 cas
- [ ] dateAudience + dateDecision si recours formé
- [ ] Validation : dates incohérentes / futures rejetées
- [ ] Migration 116 : table + 1 règle visibility CONTEXTUAL
- [ ] Gate country FRANCE strict (workspace BE → 400)
- [ ] Gate DROIT_IMMIGRATION
- [ ] Isolation workspace
- [ ] ≥12 UT + ≥8 IT verts

---

## Notes
- **jours francs vs jours calendaires** : la simplification retenue traite le délai de recours comme 30 jours calendaires. Pour précision absolue, voir R.776-18 CJA. Message explicite rappelle la marge.
- **motifOqtf** : l'énumération est alignée avec les 5 cas de l'art. L.611-1 CESEDA. Pas d'intégration IA dans cette SF.
- **Table `oqtf_avec_delai_analyses`** plutôt que générique `oqtf_analyses` : respecte l'invariant "un outil = une situation". SF-IM-08-03 aura sa propre table.
