# Mini-spec — F-282 / SF-282-04 — Sélecteur de *version de conclusions* source dans le dialogue de round

> Feature parente : **F-282** (cycle contradictoire). Branche : `feat/SF-282-04-conclusion-source`. Date : 2026-06-13. Statut : `ready`.
>
> **Origine** : demande PO — *« faire en sorte que chacune de nos conclusions soit aussi sourcée dans son round »*. Le sélecteur de **pièce/document** source (côté **adverse**, `sourceDocumentId`) a été livré par **SF-282-03** (PR #1675 + fix #1677) ; cette même SF a **explicitement renvoyé en hors-scope** le sélecteur de **version de conclusions** (`sourceConclusionId`, côté **nous**) :
> > *« Hors scope (V1.1+) : sélecteur de version de conclusions (`sourceConclusionId`) — reste réglable via l'API, pas exposé ici. »*
> SF-282-04 comble **uniquement** ce demi-trou symétrique. Construire **sur** SF-282-03, ne rien dupliquer.
>
> **Étape 0 / 0 bis** : couvertes par le cadrage « rounds sourcés » produit le 2026-06-13 (verdict étape 0 **GO avec ajustements**, étape 0 bis **GO avec ajustements**) qui analysait **les deux** sources (conclusions + documents) — le demi « document » a été livré par SF-282-03, le demi « conclusions » est l'objet d'ici. Décisions PO d'alors réutilisées : **symétrie des deux sources**, **ancrage manuel d'abord (auto ensuite)**, **lien unidirectionnel** (round→source). Aucun nouvel écran ni workflow.

## Objectif
Permettre à l'avocat de **rattacher une version de conclusions (la nôtre) à un round « nous »** depuis le dialogue de round, et afficher ce lien sur la frise — en branchant le champ `sourceConclusionId` **déjà persisté** par le backend, en miroir exact du sélecteur de pièce de SF-282-03.

## Constat de départ (code `origin/master` lu le 2026-06-13)
- `contradictoire_rounds.source_conclusion_id` existe (migration 600) et `ContradictoireService.apply()` le **persiste déjà** (create + update).
- `ContradictoireRoundRequest`/`Response` portent déjà `sourceConclusionId`.
- **SF-282-03 livré** : `contradictoire-timeline.component` expose un sélecteur **« Pièce source »** (documents via `DocumentService.list`) + affiche le lien sur la frise ; il **n'expose pas** les conclusions.
- `GET /api/v1/case-files/{id}/conclusions/versions` renvoie `List<ConclusionVersionSummary> { id, versionNumber, lifecycleStatus, status, generatedAt, createdAt }` (SF-98-52) — **source du sélecteur**.
- Page conclusions (F-267) : route `case-files/:id/conclusions` — **cible de navigation** du lien.
- ⚠️ Le backend **ne valide pas** l'isolation des ids liés (ni la cohérence partie↔source) — voir Part B (optionnel).

## Comportement attendu

### Cas nominal — Part A (cœur : sélecteur conclusions + affichage)
1. Dans le dialogue d'ajout/édition de round, **quand `party = OURS`**, un champ **« Version de conclusions liée (optionnel) »** (`mat-select`, `appearance="outline"`) liste les versions du dossier (`GET .../conclusions/versions`), libellé option = `v{versionNumber} · {lifecycleStatus}`, valeur = id de version → alimente `sourceConclusionId` dans la requête (création **et** édition).
2. **Conditionnel à la partie** : `party = OURS` → sélecteur **conclusions** (cette SF) ; `party = ADVERSE` → sélecteur **document** (SF-282-03, inchangé). Au changement de partie, l'id de l'autre type est réinitialisé (un round ne porte qu'une source, du bon type). Aucun choix → `null` (comportement préservé).
3. Sur la frise, un round « nous » lié à une version affiche le **libellé « Conclusions v{n} »**, **cliquable** → navigation Router vers `/case-files/{caseFileId}/conclusions?version={sourceConclusionId}` (la page F-267 pré-sélectionne la version).
4. En édition d'un round déjà lié, le sélecteur est **pré-renseigné**.
5. Round sans source → **aucun lien** affiché (pas de ligne/placeholder vide), exactement comme SF-282-03.

### Cas nominal — Part B (optionnel, recommandé : durcissement backend)
6. `POST`/`PUT` round valide l'isolation : `sourceConclusionId` doit référencer une conclusion **de ce dossier** (`CaseConclusionRepository.findByIdAndCaseFileId`) sinon **400** ; idem `sourceDocumentId` (document du dossier) sinon **400**. Cohérence partie↔source : `OURS` interdit `sourceDocumentId`, `ADVERSE` interdit `sourceConclusionId` (**400** sinon). *Défense en profondeur* (l'UI ne propose que les ressources du dossier) — peut être livré dans une SF backend distincte si on veut garder SF-282-04 frontend-only.

### Cas d'erreur
| Situation | Comportement | Code |
|---|---|---|
| Dossier sans version de conclusions | sélecteur vide/désactivé « aucune version » ; round créable sans source | — |
| Version supprimée après liaison | la frise n'affiche pas de lien cassé (libellé absent → pas de lien), pas d'erreur | 200 |
| Échec `GET .../conclusions/versions` | dialogue utilisable sans le sélecteur (dégradation gracieuse), `markForCheck()` | — |
| (Part B) `sourceConclusionId` hors dossier / `OURS`+document / `ADVERSE`+conclusion | rejet | 400 |
| Dossier/round d'un autre workspace | inchangé (isolation via `case_file`) | 404 |

## Périmètre
- **Inclus (Part A)** : sélecteur **version de conclusions** (`sourceConclusionId`) conditionnel `party=OURS` + affichage + navigation `?version=` vers F-267.
- **Inclus (Part B, optionnel)** : validation backend isolation + cohérence partie↔source.
- **Hors scope** : **auto-rattachement** à la génération (« Générer ma réplique » qui pose `sourceConclusionId` tout seul) → V1.1 suivante, ne pas toucher la chaîne de génération ; auto-dérivation de rounds entiers ; lien bidirectionnel (`round_id` sur `case_conclusions`) ; aperçu drawer (le lien conclusions **route** vers la page F-267, il n'ouvre pas un drawer).

## Technique
- **Frontend** : `contradictoire-timeline.component.{ts,html}` — ajouter un `FormControl sourceConclusionId`, chargement des versions (`ConclusionService`/service existant exposant `GET .../conclusions/versions`), affichage conditionnel selon `party`, réinitialisation croisée des deux ids au changement de partie, binding création/édition, lien sur la frise + `routerLink`/`router.navigate` avec `queryParams: { version }`. OnPush + `markForCheck()` dans les `subscribe()`. Réutiliser le style/markup du sélecteur de pièce SF-282-03 (cohérence).
  - `core/models/contradictoire.model.ts` : `sourceConclusionId` existe déjà ; rien à ajouter (le libellé est résolu côté front depuis la liste des versions — **pas** besoin d'un `sourceLabel` backend).
  - Page F-267 : lire le queryParam `version` au chargement et pré-sélectionner cette version (sans casser le défaut sans queryParam).
- **Backend (Part B uniquement)** : `ContradictoireService.validate(request, caseFile)` + injection `CaseConclusionRepository` (et repo documents) ; aucune migration.
- **Migration** : **aucune** (colonnes déjà présentes, migration 600).

## Critères d'acceptation
- [ ] Le dialogue expose, **si `party=OURS`**, un sélecteur « Version de conclusions liée (optionnel) » peuplé par `GET .../conclusions/versions`.
- [ ] À l'enregistrement (création ET édition), `sourceConclusionId` est transmis et persisté ; absence de choix → `null`.
- [ ] Changer la partie réinitialise l'id de l'autre type ; un round ne part jamais avec `sourceDocumentId` **et** `sourceConclusionId`.
- [ ] En édition d'un round lié, le sélecteur est pré-renseigné.
- [ ] La frise affiche « Conclusions v{n} » cliquable → `/case-files/:id/conclusions?version=<id>` ; version absente/supprimée → pas de lien cassé.
- [ ] La page F-267 pré-sélectionne la version passée en `?version=`, sans régression sans queryParam.
- [ ] Dégradation gracieuse si la liste des versions échoue.
- [ ] **Aucune modification** de la chaîne de génération des conclusions (self-check grep : prompt/worker/`prepare`).
- [ ] (Part B) `POST`/`PUT` rejette (400) un `sourceConclusionId` hors dossier et les incohérences partie↔source ; **testé** (dossier ≠ dossier, workspace A ≠ B).
- [ ] Conforme `DESIGN_SYSTEM.md` (mat-form-field outline, navy/or, JetBrains Mono pour `v{n}`).
- [ ] Revue visuelle PO.

## Plan de test
- **Jest** : sélecteur conclusions visible si OURS, document si ADVERSE ; changement de partie réinitialise l'autre id ; enregistrement transmet `sourceConclusionId` ; pré-remplissage en édition ; lien frise → `router.navigate({ queryParams: { version } })` ; dégradation si liste KO ; self-check grep (génération intacte).
- **(Part B) Intégration backend** : `PUT` round avec `sourceConclusionId` d'un autre dossier → 400 ; `OURS`+document → 400 ; `ADVERSE`+conclusion → 400 ; CRUD nominal conservé vert ; isolation workspace A→B → 404.
- **Isolation workspace** : applicable — réutilise les endpoints isolés via `case_file` ; testée si Part B.

## Analyse transversale
- **Navigation/routing** : nouveau lien `?version=` vers la page F-267 existante (pas de guard nouveau).
- **Outils décisionnels / pré-fill IA** : non applicable (pas un outil décisionnel ; la source est un lien vers une ressource du dossier).
- **Auth / workspace / plans** : aucun nouveau contexte (isolation via `case_file`, Part B la durcit).
- **Cohérence inter-écrans (invariant)** : la frise (Suivi) et le fil unifié F-289 (Vue d'ensemble) doivent désigner la **même** ressource pour un round donné — *une seule vérité*.

## Dépendances
- **SF-282-03** (sélecteur de pièce + affichage frise) — **done** (master) : SF-282-04 réutilise son markup/pattern et son binding d'édition.
- `GET .../conclusions/versions` (SF-98-52) — **done**.
- Page conclusions F-267 — **done** (cible `?version=`).

## Notes et décisions
- **Construire sur SF-282-03**, pas le réécrire : le champ source devient **conditionnel à la partie** (OURS→conclusions, ADVERSE→document) — léger ajustement du composant déjà livré, même fichier, même style.
- **Pas de `sourceLabel` backend nécessaire** : le libellé « Conclusions v{n} » se résout côté front depuis la liste des versions (déjà chargée pour le sélecteur), comme SF-282-03 résout le nom de document via `DocumentService`.
- **Part B (isolation) séparable** : peut être une SF backend distincte si on veut livrer SF-282-04 en frontend-only, à l'image de SF-282-03.
- ⚠️ **Numérotation** : `SF-282-03` est **pris** (sélecteur document, mergé). Cette SF est **SF-282-04**. Ne pas réutiliser les fichiers `SF-282-03-00*` produits en session parallèle (mal numérotés, à ignorer/supprimer).
