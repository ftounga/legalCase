# Mini-spec — F-167 / SF-167-03 Extension dashboard à tous les outils Famille FR + BE restants

> Suite directe de SF-167-01. Pure addition de mappers Famille FR + BE.
> Pattern strictement identique à SF-167-02 (Travail).

---

## Identifiant `F-167 / SF-167-03`
## Branche Git `feat/SF-167-03-extension-famille`
## Date `2026-05-02`
## Statut `draft`

---

## Objectif

Ajouter à `assembleTiles` les ~24 mappers Famille restants (les pilotes SF-167-01 étaient F-FA-05/06/07).

## Liste exacte des outils à mapper

**Famille FR** (~22 outils) : F-FA-08 (Divorce altération), F-FA-09 (Divorce faute), F-FA-10 (Divorce accepté), F-FA-12 (Mesures provisoires), F-FA-13 (Révisions post-divorce), F-FA-14 (Ordonnance protection), F-FA-15 (Récompenses), F-FA-16 (Communauté universelle), F-FA-17 (Partage judiciaire), F-FA-18 — 5 sous-outils (Adoption, Contestation paternité, Possession état, Recherche paternité, Reconnaissance paternelle), F-FA-19 — 3 sous-outils (Autorité parentale, Changement résidence, Désaccords parentaux), F-FA-20 (PACS dissolution), F-FA-21 (Séparation corps), F-FA-22 (Indivision), F-FA-23 (Ordonnance requête), F-FA-24 — 6 sous-outils (Dévolution légale, Donation, Indivision successorale, Partage successoral, Rapport succession, Réserve héréditaire, Testament validité), F-FA-25 (Majeurs protégés), F-FA-26 (Changement état civil), F-FA-27 (PMA/GPA bioéthique).

**Famille BE** (~2 outils) : F-FA-11 (Désunion irrémédiable BE).

Pour chaque mapper : lire l'`*Analysis`, mapper vers `DashboardTile` avec verdict humain lisible. Outils sans verdict tranché → `primaryValue = "-"`.

## Critères d'acceptation

- [ ] Chaque outil Famille FR+BE produit une `DashboardTile` correctement formatée.
- [ ] Tests UT étendus.
- [ ] Test IT pour 2-3 outils représentatifs (F-FA-09 divorce faute, F-FA-24-rapport-succession, F-FA-11 désunion BE).
- [ ] Aucune régression.

## Hors scope

- Frontend : aucune modif (composant générique).
- Polish : SF-167-05.

## Cohérence transversale

Référence à SF-167-01 et SF-167-02 — pure addition.

## Préoccupations transversales

Aucune.

## Dépendances

- SF-167-01 mergée — `done`.
- SF-167-02 mergée si elle précède (recommandé — moins de conflits sur `CaseFileDashboardService`).
