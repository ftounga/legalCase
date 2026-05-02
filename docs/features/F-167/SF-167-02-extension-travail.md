# Mini-spec — F-167 / SF-167-02 Extension dashboard à tous les outils Travail FR + BE restants

> Suite directe de SF-167-01. Pas d'innovation infra — pure addition de mappers
> dans `CaseFileDashboardService.assembleTiles` pour couvrir les ~25 outils
> Travail restants. Pattern strictement identique à SF-167-01 (analyse de
> cohérence transversale, sécurité, design system : référence à SF-167-01).

---

## Identifiant `F-167 / SF-167-02`
## Branche Git `feat/SF-167-02-extension-travail`
## Date `2026-05-02`
## Statut `draft`

---

## Objectif

Ajouter à `assembleTiles` les ~25 mappers d'outils Travail FR + BE non couverts par SF-167-01 (les pilotes étaient F-DT-07/08/09).

## Liste exacte des outils à mapper

**Travail FR** (~22 outils) : F-DT-03 (Prescription litige), F-DT-04 (Fiche prudhomale — pas d'analyse mais peut être tile "à utiliser"), F-DT-10 (Rupture conv validity), F-DT-11 (Harcèlement licenciement nul), F-DT-12 (Discrimination), F-DT-13 (Licenciement économique), F-DT-14 (PSE), F-DT-15 (Inaptitude), F-DT-16 (Licenciement nul detection), F-DT-17 (Indemnité précarité CDD), F-DT-18 (Fin mission intérim), F-DT-19 (Heures sup), F-DT-20 (Rappel salaire), F-DT-21 (Travail dissimulé), F-DT-22 (Requalif CDD/CDI), F-DT-23 (Requalif intérim/CDI), F-DT-24 (Non concurrence), F-DT-25 (Indemnité préavis), F-DT-26 (Congés payés), F-DT-30 (Protection RP), F-DT-31 (Transaction), F-DT-32 (Documents fin contrat), F-DT-33 (AT/MP), F-DT-34 (Référé prudhomal), F-DT-35 (Contestation ARE), F-132 (Rupture conv indemnité), F-136 (Travail procedure).

**Travail BE** (~3 outils) : F-DT-06 (Tribunal travail BE — pas d'analyse, tile "à utiliser"), F-DT-27 (Motif grave BE), F-DT-28 (Avantages conv BE), F-DT-29 (Crédit-temps BE).

Pour chaque mapper :
- Lire l'`*Analysis` correspondante (repository).
- Construire une `DashboardTile` avec un `primaryValue` lisible.
- Si l'outil n'a pas d'analyse persistée (ex. F-DT-04 fiche prudhomale qui n'a pas de verdict mais une fiche), retourner une tile "Cliquer pour utiliser" avec `alertLevel = null`.
- Pour les outils sans verdict tranché (générateurs de document), tile avec `primaryValue = "-"` et `alertLevel = null`.

## Critères d'acceptation

- [ ] Chaque outil Travail FR+BE de la liste ci-dessus produit une `DashboardTile` correctement formatée quand son analyse existe.
- [ ] Tests UT `CaseFileDashboardServiceTest` étendus avec 1 test par outil (peut être paramétré).
- [ ] Test IT `CaseFileDashboardControllerIT` étendu avec 1 cas pour 2-3 outils représentatifs (F-DT-15 inaptitude, F-DT-19 heures sup, F-DT-27 motif grave BE).
- [ ] Aucune régression sur les 10 tiles déjà mappées par SF-167-01.

## Hors scope

- Frontend : le composant `<app-dashboard-tile>` est générique, aucune modif requise. Les nouvelles tiles apparaîtront automatiquement.
- Polish (groupement thème, tri alertLevel) : SF-167-05.

## Cohérence transversale

Référence à SF-167-01 — pas de nouveau pattern. Pure addition de mappers selon le contrat `DashboardTile` figé.

## Plan de test

- 25 UT (1 par outil) ou 3 UT paramétrés
- 3 IT représentatifs

## Tests d'intégration

`POST` création de l'analyse via endpoint existant + `GET /api/v1/case-files/{id}/dashboard` → assert tile présente.

## Isolation workspace

Couverte par les IT existants.

## Préoccupations transversales

Aucune.

## Dépendances

- SF-167-01 mergée — `done`.
