# Mini-spec — F-243 / SF-243-02 — Frontend : section stade procédural

## Identifiant
`F-243 / SF-243-02`

## Feature parente
`F-243` — Stade procédural du dossier (juridiction + stade + position)

## Statut
`ready`

## Date de création
2026-05-15

## Branche Git
`feat/SF-243-02-frontend`

## Contrat API
Contrat importé de **SF-243-01-backend** (endpoints A/B/C figés dans `SF-243-01-backend-stade-procedural.md`). Le dev frontend utilise un **mock du service** pour les tests Jest — pas de dépendance au backend mergé.

---

## Objectif

Ajouter dans l'écran de détail du dossier un encart « Stade procédural » permettant à l'avocat de consulter et renseigner la juridiction, le stade et la position du dossier, avec des sélecteurs en cascade dépendant du domaine du dossier et du pays du workspace.

---

## Comportement attendu

### Cas nominal

1. À l'ouverture du dossier, le composant charge le stade procédural courant (`GET /api/v1/case-files/{id}/procedure-stage`) et le référentiel des options (`GET /api/v1/procedure-stage/options?domain=&country=`).
2. **Affichage** : si un stade est renseigné, l'encart montre les libellés humains (« Conseil de prud'hommes — Bureau de jugement (fond) — Demandeur (salarié) »). Sinon, état vide avec invitation à renseigner.
3. **Édition** : un bouton « Modifier » ouvre 3 sélecteurs en cascade :
   - Sélecteur **juridiction** : toutes les juridictions du référentiel.
   - Sélecteur **stade** : filtré sur les stades dont `jurisdictionCode` = juridiction choisie.
   - Sélecteur **position** : filtré sur les positions dont `stageCodes` contient le stade choisi.
4. Changer la juridiction réinitialise stade + position ; changer le stade réinitialise la position.
5. **Enregistrer** : `PATCH .../procedure-stage` avec la combinaison ; l'encart repasse en affichage avec les nouvelles valeurs. Les 3 champs peuvent être laissés vides (effacement).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Échec chargement options/valeur | `MatSnackBar` d'erreur + encart en état « indisponible », pas de crash |
| Échec `PATCH` (422/4xx) | `MatSnackBar` avec le message du backend, le formulaire reste ouvert et éditable |
| Domaine/pays du dossier non couverts | Encart masqué ou message info (ne devrait pas arriver — 6 combinaisons couvrent tout) |

---

## Analyse de cohérence transversale

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Autres outils décisionnels (F-DT/IM/FA) | Non | F-243 n'est pas un outil décisionnel — métadonnée de saisie |
| Pré-fill IA | Non | Le stade procédural est une décision de l'avocat, pas une donnée extraite des pièces |
| F-IA-03 / F-IA-02 (alertes, refresh dashboard) | Non | Aucun verdict, aucun impact sur les cards dashboard |
| Pattern UI sélecteurs en cascade | Oui | Nouveau pattern interne au composant — pas de composant partagé créé (cascade locale). Si réutilisé ailleurs plus tard → extraction en `shared/` |

### Décision
- [x] Étendu à toutes les cibles applicables dans cette subfeature (périmètre limité — métadonnée de dossier, pas d'outil décisionnel).

---

## Conformité F-IA-04

- [x] **Non applicable** — justification : SF frontend mais **pas décisionnelle**. Le composant ne consomme aucun endpoint POST décisionnel, ne produit pas de verdict/scoring, n'est pas intégré au panel F-IA-04 via `TOOL_REGISTRY`. C'est un encart de saisie de métadonnée du dossier (comme la description ou le titre).

---

## Critères d'acceptation

- [ ] L'encart « Stade procédural » est visible dans l'écran de détail du dossier.
- [ ] À l'ouverture, l'encart affiche le stade courant (libellés humains) ou un état vide.
- [ ] Le sélecteur de stade ne propose que les stades de la juridiction choisie.
- [ ] Le sélecteur de position ne propose que les positions valides pour le stade choisi.
- [ ] Changer la juridiction réinitialise stade + position ; changer le stade réinitialise la position.
- [ ] « Enregistrer » appelle `PATCH` et rafraîchit l'affichage.
- [ ] Les 3 champs peuvent être vidés (effacement du stade procédural).
- [ ] Une erreur backend (422) affiche le message dans un `MatSnackBar` sans fermer le formulaire.
- [ ] Tests Jest verts avec service mocké.

---

## Périmètre

### Hors scope (explicite)
- Endpoints backend (couverts par SF-243-01).
- Consommation du stade par F-98 (ultérieur).
- Affichage du stade procédural dans la liste des dossiers ou le dashboard (V2 si besoin).

---

## Technique

### Composants Angular
- `ProcedureStageSectionComponent` (standalone, OnPush, signals) — encart dans `case-file-detail`, affichage + édition inline (3 sélecteurs `<select>` natifs en cascade).
- `ProcedureStageService` (`core/services/`) — appels HTTP vers les endpoints A/B/C.
- Modèles TypeScript : `ProcedureStageOptions`, `ProcedureStage`, alignés sur le contrat SF-243-01.

### Intégration
- Insertion d'une `<mat-card>` dans `case-file-detail.component.html`, colonne gauche après la `stats-card`.
- Inputs : `caseFileId`, `legalDomain` (du dossier), `workspaceCountry` (du contexte workspace).

---

## Plan de test

### Tests unitaires (Jest, service mocké)
- [ ] Chargement initial : affiche le stade courant quand renseigné.
- [ ] Chargement initial : état vide quand non renseigné.
- [ ] Cascade : sélectionner une juridiction filtre les stades ; sélectionner un stade filtre les positions.
- [ ] Changer la juridiction réinitialise stade + position.
- [ ] « Enregistrer » appelle `procedureStageService.update()` avec la bonne combinaison.
- [ ] Erreur `PATCH` → `MatSnackBar`, formulaire reste ouvert.
- [ ] Effacement : enregistrer avec champs vides appelle `PATCH` avec `null`.

### Isolation workspace
- [x] Non applicable — l'isolation est garantie côté backend (SF-243-01) ; le frontend ne fait que consommer l'API authentifiée.

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Aucune préoccupation transversale** — ajout d'un encart isolé dans l'écran dossier, pas de modification de route/guard/auth/workspace.

### Smoke tests E2E concernés
- [x] Aucun smoke test concerné — additif, n'affecte pas auth/workspace/navigation.

---

## Dépendances

### Subfeatures bloquantes
- `SF-243-01` — contrat API figé (importé). Développement **parallèle** possible (tests sur mock) ; l'intégration end-to-end réelle est validée après merge des 2 PRs.

### Questions ouvertes impactées
- [ ] Aucune.

---

## Notes et décisions

- Sélecteurs `<select>` natifs (pas `mat-select` lourd) — convention design system pour les listes courtes.
- Pas de pré-fill IA : le stade procédural est une décision stratégique de l'avocat, pas une donnée extractible des pièces.
- Composant standalone OnPush + signals — si une mutation est faite dans un `subscribe()`, injecter `ChangeDetectorRef` + `markForCheck()` (cf. mémoire `feedback_onpush_subscribe_markforcheck`).
