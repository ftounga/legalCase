# Mini-spec — F-167 / SF-167-04 Extension dashboard à tous les outils Immigration FR + BE restants

> Suite directe de SF-167-01. Pure addition de mappers Immigration FR + BE.
> Pattern strictement identique à SF-167-02/03.

---

## Identifiant `F-167 / SF-167-04`
## Branche Git `feat/SF-167-04-extension-immigration`
## Date `2026-05-02`
## Statut `draft`

---

## Objectif

Ajouter à `assembleTiles` les ~13 mappers Immigration restants (les pilotes SF-167-01 étaient F-IM-05/06/07/11).

## Liste exacte des outils à mapper

**Immigration FR** (~10 outils) : F-IM-01 (Checklist pièces immigration — pas d'analyse, tile "à utiliser"), F-IM-08 — 3 sous-outils FR (OQTF avec délai, OQTF sans délai, Référés admin), F-IM-09 — 4 sous-outils (AES étudiant, AES famille, AES humanitaire, AES métiers tension), F-IM-12 (Asile avancé), F-IM-13 (Naturalisation), F-IM-17 (Régime algérien), F-IM-19 (Mineurs), F-IM-20 (Mesures éloignement avancées).

**Immigration BE** (~5 outils) : F-IM-08-annexe13-be (Annexe 13 BE), F-IM-14 — 4 sous-outils (9bis humanitaire BE, 9ter médical BE, 40bis cohabitant UE BE, 40ter familial Belge BE).

Pour chaque mapper : lire l'`*Analysis`, mapper vers `DashboardTile`. Pour F-IM-01 (checklist sans analyse) → tile "Cliquer pour utiliser".

## Critères d'acceptation

- [ ] Chaque outil Immigration FR+BE produit une `DashboardTile` correctement formatée.
- [ ] Tests UT étendus.
- [ ] Test IT pour 2-3 outils représentatifs (F-IM-08 OQTF avec délai, F-IM-13 Naturalisation, F-IM-14 9ter médical BE).
- [ ] Aucune régression.

## Hors scope

- Frontend : aucune modif.
- Polish : SF-167-05.

## Cohérence transversale

Référence à SF-167-01/02/03 — pure addition.

## Préoccupations transversales

Aucune.

## Dépendances

- SF-167-01 mergée — `done`.
