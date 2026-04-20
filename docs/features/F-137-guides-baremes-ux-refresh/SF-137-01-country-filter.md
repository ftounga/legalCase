# Mini-spec — F-137 / SF-137-01 Filtrage pays workspace sur Guides & barèmes

## Identifiant · `F-137 / SF-137-01`
## Date · `2026-04-20` · Branche · `feat/SF-137-01-guides-baremes-country-filter`

## Objectif
Masquer automatiquement les entries de `legal_referentials` dont le `country` ne correspond pas au `country` du workspace courant sur la page Admin "Guides & barèmes" (composant `ReferentialsComponent`). Les entries globales (`country == null`) restent visibles pour tous. Backend only — frontend n'a rien à changer, l'endpoint filtre en amont.

## Comportement
- Workspace **FR** : n'affiche que les entries `country=FRANCE` + globales
- Workspace **BE** : n'affiche que les entries `country=BELGIQUE` + globales
- `LegalReferentialService.getReferentials(domain, workspaceId)` : rétrocompat — ne filtre pas (super-admin / tests)
- Nouvelle surcharge `getReferentials(domain, workspaceId, workspaceCountry)` : filtre si `workspaceCountry` non null

## Critères d'acceptation
- [x] Méthode `isEntryVisibleForCountry(entry, workspaceCountry)` : `null` workspace → pas de filtrage ; entry globale → visible ; sinon match exact
- [x] `ReferentialController.getReferentials` récupère `workspace.country` via `WorkspaceMember.getWorkspace().getCountry()` et le passe au service
- [x] 3 tests unitaires ajoutés dans `LegalReferentialServiceTest` (FR, BE, null = pas de filtre)
- [x] Tests existants restent verts (signature 2-arg préservée)

## Analyse transversale
- Autres usages de `findActiveByDomain` : aucun consommateur hors `getReferentials` (grep vérifié)
- Cohérence outil décisionnel : les lookups précis (`findSystemEntry`, `findSystemEntryByCountry`) restent inchangés — ne sont pas concernés par le filtrage UI
- Préoccupations transversales : aucune (pas auth / workspace context / plans / nav nouveaux)

## Hors scope
- Recherche full-text (SF-137-02)
- Filtres multi-critères (SF-137-03)
- Refonte design (SF-137-04)
- Masquage sélectif côté frontend : non nécessaire grâce au filtrage backend

## Plan de test
- `LegalReferentialServiceTest.getReferentials_workspaceFR_masque_entries_BE` : OK
- `LegalReferentialServiceTest.getReferentials_workspaceBE_masque_entries_FR` : OK
- `LegalReferentialServiceTest.getReferentials_workspaceCountryNull_noFiltering` : OK
- Context load + toute la suite backend
