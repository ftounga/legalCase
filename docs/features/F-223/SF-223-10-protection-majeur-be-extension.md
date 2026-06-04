# Mini-spec — F-223 / SF-223-10 — Extension mandat extra-judiciaire + déclaration anticipée dans `protection-majeur-be`

## Identifiant
`F-223 / SF-223-10` — **extension** de l'outil existant `protection-majeur-be` (F-217, SF-217-14/15) — **PAS un nouvel outil** (cf. étape 0, même situation « protection du majeur vulnérable » que l'administration judiciaire déjà livrée) — statut `done` — créé 2026-06-03 — branche `feat/SF-223-10-protection-majeur-be-extension`

> **Note dev SF-223-10** : aucune migration créée. Le stockage de `protection_majeur_be_analyses` repose sur un **snapshot JSON** (`snapshot_data` TEXT, migration 280) — les nouveaux champs (`mandatEtendue`, `mandatCapaciteMandantConfirmee`, `mandatEnregistreRegistreCentral`, `declarationAnticipeeAdministrateurDesigne`) sont sérialisés dans ce snapshot sans changement de schéma. Pas de colonne dédiée, donc pas de migration 596. Pré-fill IA conservé à 0 (`PREFILL_COUNT_ALWAYS_ZERO = true`) — champs saisis par l'avocat (mini-spec § Tables/IA). Aucun changement de visibilité / tool_id / TOOL_REGISTRY.

## Objectif (1 phrase)
Approfondir, dans l'outil existant `protection-majeur-be`, la **branche de la protection conventionnelle** (mandat extra-judiciaire — loi 17/03/2013 / CC art. 490 nouveau, à vérifier par avocat belge ; déclaration anticipée du majeur), au-delà du simple aiguillage actuel, pour qualifier la validité et la portée du mandat et son articulation avec l'administration judiciaire.

## Contexte (existant à étendre)
L'outil `protection-majeur-be` accepte déjà en entrée `mandatExtraJudiciaireSigne`, `mandatExtraJudiciaireDateSignature` et `declarationAnticipeeExiste`, et produit le verdict `MANDAT_EXTRA_JUDICIAIRE_VALABLE` quand un mandat a été signé après l'entrée en vigueur de la loi. Le traitement actuel est **superficiel** (validité = date postérieure à 2014-09-01). L'extension F-223 enrichit cette branche : étendue du mandat, capacité du mandant au moment de la signature, articulation avec l'administration si le mandat est partiel, et qualification de la déclaration anticipée comme choix anticipé de l'administrateur.

## Comportement (extension du formulaire / verdict existant)
- Entrées additionnelles dans `ProtectionMajeurBeRequest` (nullables, rétro-compatibles) :
  - `mandatEtendue` (enum nullable `BIENS` / `PERSONNE` / `BIENS_ET_PERSONNE`) — portée du mandat.
  - `mandatCapaciteMandantConfirmee` (bool nullable) — le mandant était capable lors de la signature.
  - `mandatEnregistreRegistreCentral` (bool nullable) — enregistrement au registre central des mandats (condition d'opposabilité — à vérifier).
  - `declarationAnticipeeAdministrateurDesigne` (bool nullable) — la déclaration anticipée désigne un administrateur préféré.
- Logique verdict enrichie (branche mandat de `ProtectionMajeurBeCalculator`) :
  - Mandat signé + capacité confirmée + enregistré + portée couvrant la situation → `MANDAT_EXTRA_JUDICIAIRE_VALABLE` (voie privée prime, pas de saisine judiciaire).
  - Mandat partiel (ne couvrant pas toute la situation) → `MANDAT_EXTRA_JUDICIAIRE_VALABLE` pour le périmètre couvert + recommandation d'administration judiciaire **complémentaire** pour le reste (message d'aide).
  - Mandat non enregistré / capacité non confirmée → `QUALIFICATION_INCOMPLETE` ou réserve (selon l'invariant existant).
  - `declarationAnticipeeAdministrateurDesigne = true` → la juridiction est, en principe, liée par le choix anticipé (message d'aide), sans changer le verdict d'administration.
- Le verdict de `protection-majeur-be` mentionne désormais l'articulation mandat/administration et la portée du mandat.

## Cas d'erreur
- Conserve les cas d'erreur existants (400 gate BE-only/DROIT_FAMILLE + enum invalide ; 404 isolation).
- `mandatExtraJudiciaireSigne = true` + nouvelles entrées incohérentes (ex. `mandatEnregistreRegistreCentral = false` sans `mandatEtendue`) → `QUALIFICATION_INCOMPLETE` (pas de 400 — champs nullables rétro-compatibles).

## Critères d'acceptation
- [ ] L'outil `protection-majeur-be` qualifie la portée du mandat (biens / personne / les deux) et l'articulation avec l'administration judiciaire complémentaire.
- [ ] Déclaration anticipée désignant un administrateur → message liant la juridiction au choix anticipé.
- [ ] **Aucun nouvel outil / nouveau tool_id / nouvelle card / nouvelle entrée de visibilité** (extension pure de l'existant — `decision_tool_visibility_rules` inchangé, pas de modif `KNOWN_NO_DASHBOARD_TILE_IDS`).
- [ ] **Anti-régression** : le comportement existant de `protection-majeur-be` (administration des biens/personne, urgence, verdicts F-217) reste intact (tests SF-217-14/15 verts).
- [ ] Aucune citation jurisprudentielle BE (F-JU-04 parké).
- [ ] Champs additionnels pré-remplis IA si factualisables (F-246), sinon vides.

## Plan de test
- UT `ProtectionMajeurBeCalculator` : nouveaux cas (mandat enregistré + capacité confirmée + portée → valable ; mandat partiel → valable + complément judiciaire ; déclaration anticipée → message) + **anti-régression** des cas F-217 existants.
- IT Controller : POST avec champs additionnels → 200 + persistance ; POST **sans** les champs additionnels (legacy) → comportement F-217 inchangé (rétro-compatibilité).
- Jest composant `protection-majeur-be-section` : champs additionnels affichés dans la branche mandat ; anti-régression de l'affichage existant.
- Isolation workspace : couverte par l'existant (gate BE-only/DROIT_FAMILLE inchangé).

## Tables / endpoints / composants
- Backend : extension du `ProtectionMajeurBeCalculator` + DTO `ProtectionMajeurBeRequest`/`Response` + Input/Result (champs additionnels). **Migration éventuelle** : `addColumn` (`mandat_etendue`, `mandat_capacite_mandant_confirmee`, `mandat_enregistre_registre_central`, `declaration_anticipee_administrateur_designe`) à la table existante `protection_majeur_be_analyses` **si stockage colonne** (le snapshot JSON existant peut suffire — à confirmer en dev ; numéro de migration **à pré-assigner** si addColumn retenu). **Pas de nouvelle table.**
- Frontend : ajout des champs + mention articulation mandat/administration dans `protection-majeur-be-section.component` (existant). Pas de nouvelle entrée `TOOL_REGISTRY` / `THEME_BY_TOOL`.
- IA : champs additionnels mandat ajoutés au record Famille **si pertinents** pour le pré-fill (sinon laissés non extraits V1 — `PREFILL_COUNT_ALWAYS_ZERO` toléré). Pas de nouveau flag pivot (la visibilité reste celle de `protection-majeur-be` F-217).

## Invariants
1 outil = 1 situation (la protection du majeur vulnérable est une seule situation — l'administration judiciaire ET la voie conventionnelle en sont deux branches) ; pas de nouveau tool_id/visibilité/card ; BE-only (gate hérité) ; pré-fill F-246 ; pas de citation jurisprudence BE ; anti-régression F-217.

## Hors périmètre
Création d'un outil mandat séparé (explicitement écarté en étape 0 — `protection-be-declaration-anticipee` fusionné comme branche) ; tutelle des mineurs (`protection-mineur-tutelle-be` → P4 différé F-224) ; génération du mandat ou de la déclaration anticipée (outil de génération dédié potentiel, reporté).
