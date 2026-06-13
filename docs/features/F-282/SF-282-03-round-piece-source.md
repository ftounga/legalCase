# Mini-spec — F-282 / SF-282-03 — Sélecteur de pièce source dans le dialogue de round

> Feature parente : **F-282** (cycle contradictoire, V1 livrée PR #1646). Branche : `feat/SF-282-03-round-source-piece`. Date : 2026-06-13.
> Origine : **trou détecté au test 2026-06-13** — la mini-spec SF-282-01 prévoyait « pièce/conclusion source optionnelle » dans le dialogue de round, mais le **sélecteur n'a jamais été construit côté frontend** (les champs `sourceDocumentId`/`sourceConclusionId` existent et sont persistés côté backend, mais partent toujours à `null` depuis l'UI).
> Étape 0 / 0 bis : **couvertes par le cadrage initial F-282** (SF-282-00 / SF-282-00b) — cette SF **complète** le dialogue déjà cadré, n'ajoute aucun écran ni workflow nouveau.

## Objectif
Permettre à l'avocat de **rattacher une pièce (document) source à un round** depuis le dialogue de round, et afficher ce lien sur la frise — en branchant le champ `sourceDocumentId` déjà supporté par le backend.

## Comportement attendu
### Nominal
1. Dans le dialogue d'ajout/édition d'un round, un champ **« Pièce source (optionnel) »** (`mat-select`, `appearance="outline"`) liste les **documents du dossier** (via `DocumentService.list(caseFileId)`, libellé = nom de fichier original).
2. À l'enregistrement, la valeur choisie alimente `sourceDocumentId` dans la requête (création **et** édition). Aucun choix → `null` (comportement actuel préservé).
3. Sur la frise, un round ayant une pièce source affiche le **nom du document lié** (avec, si simple, un accès aperçu/téléchargement réutilisant `DocumentService`).
4. En édition d'un round déjà lié, le sélecteur est **pré-renseigné**.

### Cas d'erreur
| Situation | Comportement |
|---|---|
| Dossier sans document | sélecteur vide / désactivé avec libellé « aucune pièce » ; round créable sans source |
| Document supprimé après liaison | la frise n'affiche pas de lien cassé (nom absent → pas de lien), pas d'erreur |
| Échec `DocumentService.list` | le dialogue reste utilisable sans le sélecteur (dégradation), `markForCheck()` |

## Périmètre
- **Inclus** : sélecteur **document** (`sourceDocumentId`) + affichage du lien sur la frise.
- **Hors scope (V1.1+)** : sélecteur de **version de conclusions** (`sourceConclusionId` — reste réglable via l'API, pas exposé ici) ; auto-dérivation des rounds depuis documents tagués adverses.

## Technique
- **Backend** : **aucun changement** — `ContradictoireRoundRequest.sourceDocumentId` et `ContradictoireService` (create + update) persistent déjà le champ.
- **Frontend** : `contradictoire-timeline.component.{ts,html}` — ajout d'un `FormControl sourceDocumentId` au formulaire de round, chargement des documents (`DocumentService.list`), binding création/édition, affichage du lien sur la frise. OnPush + `markForCheck()` dans les `subscribe()`.
- **Migration** : aucune.

## Critères d'acceptation
- [ ] Le dialogue de round expose un sélecteur « Pièce source (optionnel) » peuplé par les documents du dossier.
- [ ] À l'enregistrement (création ET édition), `sourceDocumentId` est transmis et persisté ; absence de choix → `null`.
- [ ] En édition d'un round lié, le sélecteur est pré-renseigné.
- [ ] La frise affiche le nom de la pièce liée ; pièce absente/supprimée → pas de lien cassé.
- [ ] Dégradation gracieuse si la liste des documents échoue.
- [ ] **Conséquence Vue d'ensemble** : un round avec `sourceDocumentId` voit sa pièce apparaître dans l'accordéon du fil (l'`OverviewService` la rattache déjà) — vérifiable après déploiement.
- [ ] Conforme `DESIGN_SYSTEM.md` (mat-form-field outline, navy/or).

## Plan de test
- **Jest** : sélecteur peuplé via `DocumentService.list` (mické) ; enregistrement transmet `sourceDocumentId` ; pré-remplissage en édition ; dégradation si liste KO ; affichage du lien sur la frise.
- **Isolation workspace** : inchangée (réutilise les endpoints existants, isolés via `case_file`).

## Analyse transversale
- **Navigation/routing** : aucun (dialogue interne au composant). **Outils décisionnels** : aucun touché. **Auth/workspace/plans** : aucun. **Pré-fill IA** : non applicable (pas un outil décisionnel à champs IA).
