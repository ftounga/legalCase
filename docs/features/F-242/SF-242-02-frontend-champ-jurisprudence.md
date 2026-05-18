# Mini-spec — F-242 / SF-242-02 — Frontend : champ « Jurisprudence à l'appui » par point juridique

> Cadrages amont : `SF-242-00-coherence.md` (étape 0 — GO) + `SF-242-00b-ux-coherence.md` (étape 0 bis — GO avec ajustements). Contrat API figé par `SF-242-01-backend-citations-jurisprudence.md` (SF parallélisable).

## Identifiant
`F-242 / SF-242-02`

## Feature parente
`F-242` — Citation jurispru structurée + enrichissement conclusions

## Statut
`done` — livrée 2026-05-18.

## Date de création
2026-05-18

## Branche Git
`feat/SF-242-02-frontend-champ-jurisprudence`

---

## Objectif
Permettre à l'avocat, sous chaque point juridique de l'écran synthèse, de saisir / éditer / supprimer des citations de jurisprudence d'appui, en consommant les endpoints de SF-242-01.

---

## Comportement attendu

### Cas nominal
1. Dans l'écran synthèse (`SynthesisComponent`), panneau **« Points juridiques »**, chaque point juridique affiche une zone compacte **« Jurisprudence à l'appui »** sous le texte du point, à proximité du bouton deeplink F-241.
2. La zone liste les citations existantes du point (référence + portée) avec une action éditer et une action supprimer par citation, et un formulaire d'ajout (champ référence + champ portée + bouton « Ajouter »).
3. À l'ajout : `POST .../jurisprudence-citations` avec `pointJuridiqueIndex` = index du point dans la liste, `pointJuridiqueTexte` = texte du point, `reference`, `portee`. La citation apparaît dans la liste sans rechargement.
4. Édition : `PUT`, suppression : `DELETE` — la liste se met à jour.
5. Au chargement de la synthèse, `GET .../jurisprudence-citations` charge les citations existantes et les répartit par `pointJuridiqueIndex`.
6. La zone est **discrète quand le point n'a aucune citation** (lien / bouton « + Jurisprudence à l'appui » qui déplie le formulaire) — pas de formulaire déployé en permanence.

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| `reference` vide à l'ajout / l'édition | Bouton désactivé ou message de validation ; pas d'appel API |
| Erreur API (`400` / `404` / réseau) | Message d'erreur non bloquant (snackbar) ; la saisie n'est pas perdue |
| Aucune citation sur un point | Zone réduite à l'appel à l'action « + Jurisprudence à l'appui » |

---

## Analyse de cohérence transversale

- [x] **Outil décisionnel ?** Non — saisie d'enrichissement, pas de verdict. Pas de `TOOL_REGISTRY`, pas de F-IA-03 / F-IA-04.
- [x] **UI patterns** : formulaire réactif compact + dialogue de confirmation de suppression (pattern existant). `OnPush` : si `SynthesisComponent` est en `OnPush`, toute mutation dans un `subscribe()` doit appeler `ChangeDetectorRef.markForCheck()` (cf. retour `feedback_onpush_subscribe_markforcheck`).
- [x] **Navigation / routing** : aucune route nouvelle — la zone vit dans l'écran synthèse existant.
- [x] **Charge écran** : aucun bloc primaire nouveau — enrichissement per-item du panneau « Points juridiques » existant (invariant anti-surcharge de l'étape 0 bis).

---

## Conformité F-IA-04
- [x] **Non applicable** — pas un outil décisionnel.

---

## Critères d'acceptation
- [ ] **CA1** — sous chaque point juridique du panneau « Points juridiques », une zone « Jurisprudence à l'appui » permet de lister / ajouter / éditer / supprimer des citations via les endpoints SF-242-01.
- [ ] **CA2** — la zone est **discrète quand le point n'a aucune citation** (appel à l'action repliée) ; elle ne déploie un formulaire que sur action de l'avocat.
- [ ] **CA3** — la zone est placée **adjacente au bouton deeplink F-241** du même point juridique, dans l'ordre aller (F-241) → retour (F-242).
- [ ] **CA4** — le libellé « Jurisprudence à l'appui » est **distinct** de la section F-179 « Jurisprudences citées » ; aucune fusion des deux.
- [ ] **CA5** — `reference` vide → ajout/édition impossible ; erreur API → snackbar non bloquante, saisie conservée.
- [ ] **CA6** — `pointJuridiqueIndex` et `pointJuridiqueTexte` envoyés correspondent au point juridique courant (index dans la liste + texte affiché).

---

## Périmètre
### Hors scope
- Tout le backend (→ SF-242-01).
- La péremption (`stale`) des conclusions : calculée côté backend (SF-242-01 + SF-98-53) ; le bandeau « à régénérer » existant de SF-98-53 la reflète déjà — aucun travail frontend supplémentaire.
- L'affichage des citations dans la section conclusions ou les outils décisionnels.

---

## Technique
### Contrat API
Consommé tel que figé dans `SF-242-01` — aucun écart.
### Composants
- Frontend (neufs) : `JurisprudenceCitationService` (4 appels HTTP), modèle `JurisprudenceCitation`, un composant compact « Jurisprudence à l'appui » (ou un bloc intégré au template du panneau « Points juridiques »).
- Frontend (modifié) : `SynthesisComponent` — template du panneau `section-points-juridiques` (zone per-item), chargement des citations à l'init.
- Backend : aucun.

---

## Plan de test
### Frontend (Jest)
- [ ] Tests du service `JurisprudenceCitationService` : les 4 appels ciblent les bonnes URLs.
- [ ] Tests du composant : liste, ajout (payload `pointJuridiqueIndex` / `pointJuridiqueTexte` corrects), édition, suppression ; zone repliée quand aucune citation ; `reference` vide bloque l'ajout ; erreur API → snackbar, saisie conservée.
- [ ] Self-check grep pré-commit : aucune régression de la suite Jest existante de `SynthesisComponent`.

---

## Analyse d'impact
- [x] Aucune préoccupation transversale bloquante (pas d'auth/workspace/navigation modifiés). Charge écran couverte par l'étape 0 bis.
- [x] Smoke tests E2E : aucun concerné (la zone vit dans l'écran synthèse, hors chemins critiques auth/workspace ; le smoke `case-analysis-flow` reste en quarantaine indépendamment).

## Dépendances
- SF-242-01 — contrat API (parallélisable : backend et frontend sur branches isolées, contrat figé).
- F-241 (Terminée) — boutons deeplink existants, adjacents.

## Notes et décisions
- Option δ : saisie légère (référence + portée), pas le texte intégral — friction faible = adoption.
- Distinction visuelle des 3 briques jurisprudence de la synthèse (F-179 vérification documents / F-241 aller / F-242 retour) : libellés explicites, invariant de l'étape 0 bis.
