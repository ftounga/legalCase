# Bugfix — Échéancier : exclure les non-délais (pièces / checks / pistes)

> Bugfix issu d'une **vérification en base** du dossier de test (2026-06-16). Exempt étape 0/0bis (correction de comportement).

## Constat (vérifié en base staging)

Sur un dossier réel, **27 « délais »** affichés dont **seulement 3 sont de vrais délais** :

| Source | Nb | Nature |
|---|---|---|
| `PIECE_A_DEMANDER` | 17 | pièces à demander au client (F-194) — **pas un délai** |
| `PROCEDURE_CHECK_TO_CHECK` | 5 | points de checklist procédurale (F-193) — **pas un délai** |
| `PISTE_RETENUE` | 2 | pistes stratégiques (F-192) — **pas un délai** |
| `AI` PENDING | 2 | vraies prescriptions (non encore confirmées) |
| `STATUTORY` | 1 | vraie prescription légale |

F-192/193/194 injectaient ces items dans `case_deadlines` (avec des dates synthétiques) pour leur donner une présence datée → ils **noyaient les vrais délais** (24/27 de bruit) dans l'échéancier, le fil et la Vue d'ensemble. Ils ont déjà leurs **propres panneaux** (pièces manquantes, checklist procédurale, pistes).

## Décision (signalée — réversible)

**Réversion partielle de l'intention F-192/193/194** : ces 3 sources synthétiques sont **exclues de l'échéancier** (point d'agrégation unique `EcheancierService.isConfirmed`). Conséquence : échéancier (F-284) + fil + Vue d'ensemble (F-289) ne montrent plus que les **vrais délais**. **Aucune donnée supprimée en base** ; les items restent visibles dans leurs panneaux dédiés. Le front F-69 ne les affichait déjà pas (sources inconnues de son filtre).

## Comportement

- `EcheancierService` exclut `PIECE_A_DEMANDER`, `PROCEDURE_CHECK_TO_CHECK`, `PISTE_RETENUE`.
- Restent : `MANUAL`, `STATUTORY`, `AI` (ACCEPTED), `CONTRADICTOIRE` (dernier round).

## Critères d'acceptation

- [ ] Un dossier avec pièces/checks/pistes + 1 délai légal → l'échéancier ne renvoie **que** le délai légal.
- [ ] Délais MANUAL/STATUTORY/AI-accepté/contradictoire toujours présents.
- [ ] Données conservées en base (aucune suppression).

## Tests

- `EcheancierControllerIT.excludesNonDeadlineSources_pieceCheckPiste` + 9 existants = **10 verts**.

## Composants impactés

- `EcheancierService.java` (set `NON_DEADLINE_SOURCES` + `isConfirmed`).

**Aucun** : migration, endpoint, frontend (consommateurs inchangés — ils reçoivent juste moins de bruit).

## Hors périmètre / suivi

- Arrêter la **propagation à la source** (F-192/193/194 n'écrivent plus dans `case_deadlines`) — refactor plus large, à cadrer si le PO confirme que ces lignes n'ont plus d'utilité ailleurs.
- Nettoyage des lignes synthétiques déjà en base (purge optionnelle).

## Analyse transversale

- **Lecture seule** : filtre d'agrégation, aucune mutation. Isolation workspace inchangée (résolution par `resolveCaseFileForUser`).
- **Smoke E2E** : N/A (pas d'impact auth/workspace/navigation).
