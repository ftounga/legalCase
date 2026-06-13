# Mini-spec — F-290 / SF-290-04 — Échéance de réponse de round satisfaite par le round suivant

> Feature parente : **F-290** (garde-fous de cohérence du dossier vivant). Branche : `feat/SF-290-04-echeance-round-satisfaite`. Date : 2026-06-13.
> Origine : **trou détecté au test 2026-06-13** — après création du Round 2 (adverse), l'échéancier affichait toujours l'échéance de réponse du Round 1 (« J+54 dépassé »), alors que la réponse était arrivée. L'`EcheancierService` ajoutait une échéance pour **chaque** round, sans considérer qu'un round postérieur **satisfait** l'échéance du précédent.
> Étape 0 : SF-290-04 est l'axe **non ambigu et sans arbitrage** de F-290 (à l'inverse de SF-290-01 divergence stade/phase qui reste en attente d'arbitrage PO). Pas d'impact écran (logique backend) → étape 0 bis non applicable.

## Objectif
Ne plus afficher comme due/dépassée l'échéance de réponse d'un round dont **un round postérieur existe** : la réponse attendue est arrivée → l'échéance est **satisfaite**.

## Comportement attendu
### Nominal
- L'`EcheancierService` n'ajoute l'échéance de réponse (`responseDueAt`) **que du dernier round** (le round de plus grand `roundNumber`, sans round postérieur). Les rounds antérieurs ayant un `responseDueAt` sont considérés **satisfaits** et exclus.
- Propagation **automatique** : `OverviewService` consomme `EcheancierService` → le bandeau **Piloter** (prochain couperet), le bloc **attention** et les échéances futures du **fil** (Vue d'ensemble) bénéficient du même filtre, sans modification supplémentaire.

### Cas limites
| Situation | Comportement |
|---|---|
| 1 seul round avec `responseDueAt` | inchangé (c'est le dernier → échéance vivante) |
| Dernier round sans `responseDueAt` | aucune échéance de round (les antérieurs sont satisfaits) |
| Aucun round | aucune échéance de round (inchangé) |

## Critères d'acceptation
- [ ] Avec Round 1 (`responseDueAt` passé) + Round 2 (postérieur) → l'échéancier **n'affiche plus** l'échéance du Round 1 (`counts.overdue` ne la compte plus) ; seule l'échéance du Round 2 (si présente) subsiste.
- [ ] Dernier round sans `responseDueAt` + rounds antérieurs → **0** échéance de round.
- [ ] 1 seul round avec `responseDueAt` → inchangé (échéance présente).
- [ ] Pilotage / attention / fil de la Vue d'ensemble cohérents (via `EcheancierService`).
- [ ] Isolation workspace inchangée.

## Technique
- **Backend** : `EcheancierService.forCaseFile` — remplacer la boucle « pour chaque round » par « **uniquement le dernier round** de `findByCaseFileIdOrderByRoundNumberAsc` ». **Aucune migration.** Lecture seule. Aucun changement de contrat (DTO `EcheancierItem`/`EcheancierResponse` inchangés).
- **Frontend** : aucun (consomme le contrat inchangé).

## Plan de test
- **IT `EcheancierControllerIT`** : (a) Round 1 passé + Round 2 postérieur → 1 item, `overdue=0` ; (b) dernier round sans échéance + antérieur → 0 item ; (régression) 1 round seul avec échéance → 1 item.
- **Isolation workspace** : couverte par les IT existants (404 cross-workspace).

## Hors périmètre
- SF-290-01 (divergence stade/phase), SF-290-02 (round après clôture), SF-290-03 (état « instruction close ») — restent au backlog (arbitrage PO requis pour SF-290-01).
- Notion de « réponse de la partie *opposée* » stricte : on retient l'heuristique simple et robuste « un round postérieur existe » (ordre `roundNumber`), suffisante pour le cas réel (alternance des échanges).

## Analyse transversale
- **Outil décisionnel** : aucun. **Auth/workspace/plans/navigation** : aucun. **Pré-fill IA** : non applicable. Pur ajustement de logique d'agrégation lecture seule.
