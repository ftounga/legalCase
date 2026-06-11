# Mini-spec — F-98 / SF-98-62 + SF-98-63 — Conclusions CPH stade BCO (FR, demandeur + défendeur)

> Feature parente : **F-98** — Génération de courrier / conclusions.
> Étape 0 : `SF-98-62-00-coherence.md` (GO). Étape 0 bis : `SF-98-62-00b-ux-coherence.md` (GO avec ajustements).
> Statut : `ready` · Date : 2026-06-11 · Branche : `feat/SF-98-62-conclusions-cph-bco`

## Identifiant

`F-98 / SF-98-62` (CPH / BCO / **Demandeur**) **et** `F-98 / SF-98-63` (CPH / BCO / **Défendeur**) — livrées ensemble (invariant anti-demi-trou #6 de l'étape 0).

## Objectif

Couvrir le stade **Bureau de Conciliation et d'Orientation (BCO)** du Conseil de prud'hommes FR dans la matrice de conclusions : générer l'acte écrit attendu à l'entrée de la saisine, là où la génération était bloquée en silence — et empêcher toute récidive de ce trou de couverture par un test d'intégrité.

## Comportement attendu

### Cas nominal

1. Un dossier `DROIT_DU_TRAVAIL` est réglé `procedure_jurisdiction=CPH`, `procedure_stage=BCO`, `procedure_position=DEMANDEUR` (ou `DEFENDEUR`), analyse `DONE`.
2. L'avocat clique « Générer le projet de conclusions » (`ConclusionsSectionComponent`, existant).
3. `CaseConclusionCommandService.triggerGeneration` passe les 4 gardes (la combinaison **est désormais couverte**) → crée la ligne `case_conclusions` PENDING → publie après commit.
4. Le worker `CaseConclusionService` résout la cellule via `ConclusionPromptRegistry` :
   - **DEMANDEUR** → `CphBcoDemandeurPromptProvider` : génère une **requête de saisine du CPH valant conclusions** (faits + procédure ; demandes au fond ; volet **provisions/référé** le cas échéant ; demande de remise des documents de fin de contrat sous astreinte).
   - **DEFENDEUR** → `CphBcoDefendeurPromptProvider` : génère des **observations / conclusions en défense** au stade conciliation (contestation des demandes, position sur la conciliation, à titre subsidiaire renvoi au fond).
5. Statut `DONE` + contenu rendu (F-259), bordereau (SF-98-57), export Word/PDF (SF-98-50/51).

### Cas d'erreur

| Situation | Comportement attendu | Code |
|---|---|---|
| Stade/juridiction/position non renseignés | Garde `STAGE_NOT_SET` (inchangé) | 409 |
| Analyse non terminée | Garde `ANALYSIS_NOT_READY` (inchangé) | 409 |
| Génération déjà en cours | Garde `ALREADY_GENERATING` (inchangé) | 409 |
| **Combinaison toujours non couverte** (autre stade futur) | `COMBINATION_NOT_SUPPORTED` → **message explicite et persistant** dans la section conclusions (ajustement étape 0 bis), plus de snackbar fugace | 409 |
| Réponse IA vide | `FAILED` + message (inchangé) | — |
| Accès dossier d'un autre workspace | Refus | 403/404 |

## Analyse de cohérence transversale

### Périmètres scannés

- **Autres stades CPH** : Fond / Référé / Départage / Appel / Cassation déjà couverts. BCO était le seul manquant pour le CPH FR.
- **Autres juridictions** : le BCO est spécifique au CPH FR. Le tribunal du travail BE a sa propre phase (conciliation préalable) — **hors périmètre** (signal terrain FR uniquement ; à tracer backlog si signal BE).
- **Autres domaines** : sans objet (le BCO est propre au droit du travail).
- **Garde-fou registre ↔ catalogue** : applicable et **intégré** (test d'intégrité ci-dessous).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|---|---|---|
| CPH FR stade BCO (Demandeur + Défendeur) | Oui | **Intégré dans cette SF** (2 cellules) |
| Conciliation préalable TT belge | Oui (analogue) | **Backlog** — signal terrain BE requis (cohérent avec périmètre FR de l'étape 0) |
| Test d'intégrité combinaisons sélectionnables ↔ cellules | Oui | **Intégré dans cette SF** (`ConclusionCombinationCoverageIT`) |

### Décision

- [x] Étendu à toutes les cibles FR applicables dans cette subfeature (BCO Demandeur + Défendeur + garde-fou).
- [x] Backlog pour la conciliation TT belge (non prioritaire, hors signal terrain).

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : aucune section décisionnelle `<app-XXX-section>` ni entrée `TOOL_REGISTRY`. La génération de conclusions n'est pas un outil décisionnel ; le seul éventuel changement frontend est l'**encart de message d'indisponibilité** (texte, pas de formulaire, pas de pré-fill).

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : pas de formulaire à champs saisissables. La cellule consomme la synthèse + verdicts d'outils déjà extraits ; aucun nouveau champ `*ExtractedData`.

## Critères d'acceptation

- [ ] Une cellule `CphBcoDemandeurPromptProvider` existe avec `combination() = (DROIT_DU_TRAVAIL, FRANCE, "CPH", "BCO", "DEMANDEUR")` et un prompt produisant une **requête de saisine valant conclusions** (faits + procédure, demandes au fond, volet provisions/référé, remise documents sous astreinte, « Par ces motifs »).
- [ ] Une cellule `CphBcoDefendeurPromptProvider` existe avec `combination() = (…, "CPH", "BCO", "DEFENDEUR")` et un prompt produisant des **observations/conclusions en défense** adaptées à la conciliation.
- [ ] Génération nominale : sur un dossier CPH/BCO/Demandeur analysé, la génération aboutit `DONE` avec contenu non vide, renvois « Pièce n° X » et dispositif (réutilise `REDACTION_QUALITY_GUARD` + `JURISPRUDENCE_GUARD` — non-régression SF-98-55/56).
- [ ] Les prompts **ne dupliquent pas** l'analyse stratégique de F-DT-84 (pas de « faut-il concilier / comparaison BCA vs barème Macron ») — ils produisent l'acte écrit.
- [ ] **Test d'intégrité** `ConclusionCombinationCoverageIT` : pour chaque combinaison `(domaine, pays, juridiction, stade, position)` énumérable depuis `ProcedureStageCatalog`, il existe soit une cellule `ConclusionPromptProvider`, soit une entrée explicite dans une liste blanche documentée `KNOWN_UNCOVERED_COMBINATIONS` (avec justification). Le test échoue sinon. **Le BCO FR n'est plus dans la liste blanche après cette SF.**
- [ ] **Message d'indisponibilité explicite** : si `COMBINATION_NOT_SUPPORTED`, la section conclusions affiche un encart persistant nommant la combinaison (plus de snackbar fugace).
- [ ] Isolation workspace : la génération reste cloisonnée au workspace du dossier (non-régression, mécanisme F-98 existant).

## Périmètre

### Hors scope (explicite)

- La conciliation du **tribunal du travail belge** (backlog, signal terrain BE requis).
- Toute refonte de l'UI de la section conclusions au-delà de l'encart de message.
- L'outil décisionnel de stratégie de conciliation **F-DT-84** (déjà livré, distinct).
- Persistance/optimisation des prompts au-delà du pattern `ConclusionPromptProvider` existant.

## Technique

### Composants backend

- `backend/.../casefile/conclusion/CphBcoDemandeurPromptProvider.java` — `@Component implements ConclusionPromptProvider`, combinaison `CPH/BCO/DEMANDEUR`, prompt « requête de saisine valant conclusions ».
- `backend/.../casefile/conclusion/CphBcoDefendeurPromptProvider.java` — idem, `CPH/BCO/DEFENDEUR`, prompt « observations en défense ».
- Enregistrement **automatique** par `ConclusionPromptRegistry` (scan Spring des `@Component`) — aucune constante figée à modifier.
- `ConclusionCombinationCoverageIT` (nouveau test d'intégrité) croisant `ProcedureStageCatalog.allCombinationKeys()` ↔ `ConclusionPromptRegistry`.

### Composant frontend (minimal)

- `ConclusionsSectionComponent` : transformer le retour `409 COMBINATION_NOT_SUPPORTED` en **encart d'état persistant** (au lieu du snackbar). Aucun autre changement.

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `case_conclusions` | INSERT/SELECT/UPDATE | Inchangé — la ligne se crée désormais aussi pour le stade BCO |

### Migration Liquibase

- [x] **Non applicable** (aucun changement de schéma).

## Plan de test

### Tests unitaires

- [ ] `CphBcoDemandeurPromptProvider` — `combination()` exacte ; le prompt contient les marqueurs clés (saisine valant conclusions, provisions/référé, « Par ces motifs », « Pièce n° »).
- [ ] `CphBcoDefendeurPromptProvider` — `combination()` exacte ; prompt « observations en défense / conciliation ».
- [ ] `ConclusionPromptRegistry` — résout bien une cellule pour `CPH/BCO/DEMANDEUR` et `CPH/BCO/DEFENDEUR` (avant : `Optional.empty`).

### Tests d'intégration

- [ ] `ConclusionCombinationCoverageIT` — échoue si une combinaison sélectionnable n'a ni cellule ni entrée liste blanche ; **passe** une fois les 2 cellules BCO ajoutées.
- [ ] `triggerGeneration` sur dossier CPH/BCO/Demandeur analysé → 202 PENDING (plus de 409 COMBINATION_NOT_SUPPORTED) ; ligne `case_conclusions` créée.
- [ ] `triggerGeneration` sur une combinaison volontairement non couverte → 409 `COMBINATION_NOT_SUPPORTED` (garde inchangée).
- [ ] Isolation workspace : un utilisateur du workspace A ne peut pas déclencher la génération sur un dossier du workspace B (403/404).

### Isolation workspace

- [x] Applicable — couverte par le test ci-dessus (mécanisme F-98 existant, non-régression).

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — pas d'auth, pas de workspace context nouveau, pas de plan/limite, pas de route nouvelle. Extension interne de la matrice F-98.

### Smoke tests E2E concernés

- [x] Aucun smoke test E2E bloquant (pas d'impact auth/workspace/navigation). La génération est couverte par les tests d'intégration backend.

## Dépendances

### Subfeatures bloquantes

- F-243 (stade procédural) — ✅ done. F-260 (numérotation pièces) + SF-98-57 (bordereau) — ✅ done. F-259 (rendu) — ✅ done.

### Questions ouvertes impactées

- [ ] Aucune entrée `docs/OPEN_QUESTIONS.md` concernée.

## Notes et décisions

- Le BCO est procéduralement la **porte d'entrée** de la saisine du CPH : l'acte du demandeur y est une **requête introductive valant conclusions** (fond + référé), conforme au document terrain STANOJEVIC. Ce n'est pas une « conclusion au fond » de bureau de jugement → prompt dédié.
- Frontière nette avec **F-DT-84** (stratégie de conciliation) : la cellule génère l'écrit, pas l'analyse d'opportunité.
- Le test d'intégrité est le **correctif durable** de la classe de bug (combinaison sélectionnable sans cellule) — réponse à « pourquoi ça n'avait pas été détecté ».
