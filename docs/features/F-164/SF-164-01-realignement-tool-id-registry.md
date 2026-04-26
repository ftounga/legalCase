# Mini-spec — SF-164-01 Réalignement `tool_id` seeds DB ↔ `TOOL_REGISTRY` frontend + garde-fou CI

## Identifiant
`F-164 / SF-164-01`

## Feature parente
`F-164` — Réalignement `tool_id` seeds DB ↔ `TOOL_REGISTRY` frontend + garde-fou CI

## Statut
`ready`

## Date de création
2026-04-26

## Branche Git
`feat/SF-164-01-tool-id-alignment`

---

## Objectif

Réaligner les `tool_id` entre la table `decision_tool_visibility_rules` (backend) et `TOOL_REGISTRY` (frontend) pour que le panneau F-IA-04 affiche à nouveau ses outils, et installer un garde-fou CI + règle CLAUDE.md pour empêcher la régression.

---

## Contexte

Bug staging détecté 2026-04-26 sur dossier E-36 (ntounga@gmail.com) : panneau décisionnel F-IA-04 quasi vide. Cause : 14 `tool_id` seedés en DB sans entrée correspondante dans `TOOL_REGISTRY` frontend → `resolveEntry()` retourne `null` → outils silencieusement masqués via `.filter(x => x.entry !== null)`.

Diff exact (`comm`) entre `decision_tool_visibility_rules` et `TOOL_REGISTRY` :
- 90 IDs uniques en DB
- 78 IDs uniques en TOOL_REGISTRY
- **14 orphelins backend** (DB → pas de registry)
- **2 orphelins frontend** (registry → pas de DB)

Pattern symétrique de F-DT-29 (frontend mergé sans backend, voir mémoire `feedback_pre_merge_endpoint_check`).

---

## Comportement attendu

### Cas nominal après fix

1. Avocate ouvre dossier travail FR avec `type_rupture = LICENCIEMENT_ECONOMIQUE` détecté.
2. Backend renvoie via `GET /case-files/{id}/decision-tools-visibility` la liste des outils ALWAYS_ON et CONTEXTUAL applicables.
3. Frontend résout chaque `tool_id` → composant Angular → rend la card.
4. **Aucun `console.warn [decisional-tools-panel] Unknown toolId: …`** dans la console navigateur.
5. Panel affiche les sections "Outils principaux" et "Outils contextuels" peuplées.

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| Un `tool_id` est ajouté en DB mais pas en `TOOL_REGISTRY` | Test CI `DecisionToolVisibilityIntegrityIT` échoue avec message explicite |
| Un `tool_id` orphelin frontend (registry sans seed DB) | Pas un bug runtime (registry = whitelist), mais le test d'intégrité l'ignore — c'est ok, sera pris en compte si seedé plus tard |

---

## Plan détaillé du fix

### Cat A — Renommage backend (2 UPDATE)

| Avant (DB) | Après (DB = registry) |
|---|---|
| `F-DT-17-cdd-indemnite-precarite` | `F-DT-17-indemnite-precarite-cdd` |
| `F-DT-35-contestation-are` | `F-DT-35-contestation-are-fr` |

Migration Liquibase `187-realign-decision-tool-ids.xml` : 2 `UPDATE decision_tool_visibility_rules SET tool_id = ? WHERE tool_id = ?`.

### Cat B — Ajout `TOOL_REGISTRY` frontend (5 entrées)

Tous les composants Angular existent déjà sous `frontend/src/app/case-files/`. Il manque uniquement l'entrée registry dans `decisional-tools-panel.component.ts` :

| `tool_id` | Composant Angular | Inputs |
|---|---|---|
| `F-DT-03-prescription-litige` | `CaseDeadlinesSectionComponent` (`case-deadlines-section`) | `caseFileId`, `synthesis` (à confirmer en lisant le composant) |
| `F-DT-31-transaction` | `TransactionSectionComponent` (`transaction-section`) | `caseFileId`, `aiData` (TravailExtractedData), `procedureChecks`, `aiQuestions` |
| `F-IM-09-aes-etudiant` | `AesEtudiantSectionComponent` (`aes-etudiant-section`) | inputs F-IM standard |
| `F-IM-09-aes-humanitaire` | `AesHumanitaireSectionComponent` (`aes-humanitaire-section`) | inputs F-IM standard |
| `F-FA-11-desunion-irremediable-be` | `DivorceDesunionBeSectionComponent` (`divorce-desunion-be-section`) | inputs F-FA standard |

Pour chaque entrée, lire le composant existant pour aligner sur sa signature `@Input()` réelle. Pattern à suivre : entrées voisines déjà présentes dans `TOOL_REGISTRY` (ex. `F-DT-17-indemnite-precarite-cdd`).

### Cat C — DELETE seed DB (7 DELETE)

7 IDs n'ont **aucun** composant frontend (vérifié via `ls case-files/`). Migration Liquibase `187-realign-decision-tool-ids.xml` (même changeset) : 7 `DELETE FROM decision_tool_visibility_rules WHERE tool_id = ?`.

| `tool_id` supprimé | Justification |
|---|---|
| `F-DT-01-calcul-indemnite-simple` | Absorbé par F-DT-09 comparateur indemnités, jamais implémenté en outil dédié |
| `F-DT-05-preavis-be` | Pas de composant Angular, jamais implémenté |
| `F-FA-01-prestation-compensatoire` | Pas de composant Angular, jamais implémenté |
| `F-FA-02-pension-alimentaire` | Pas de composant Angular, jamais implémenté |
| `F-FA-04-liquidation-communaute` | Pas de composant Angular, jamais implémenté |
| `F-FA-18-possession-etat` | Pas de composant Angular (backend a la table mais pas l'UI) |
| `F-FA-24-reserve-heriditaire` | Pas de composant Angular + faute d'orthographe historique (`heriditaire` au lieu de `hereditaire`) |

**Important** : on supprime UNIQUEMENT du `decision_tool_visibility_rules`, **PAS** les tables d'analyses (`possession_etat_analyses`, `reserve_heriditaire_analyses`, etc.) ni les services/controllers backend. Quand le composant frontend sera créé plus tard, il suffira de re-INSERT dans `decision_tool_visibility_rules` (et l'UPSERT/INSERT-IF-NOT-EXISTS sera idempotent grâce au garde-fou CI).

### Garde-fou CI — `DecisionToolVisibilityIntegrityIT`

Nouveau test d'intégration backend `backend/src/test/java/fr/ailegalcase/casefile/DecisionToolVisibilityIntegrityIT.java` :

```java
@SpringBootTest
@ActiveProfiles("test")
class DecisionToolVisibilityIntegrityIT {

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * Source unique de vérité : tous les tool_id qui DOIVENT être présents
     * dans TOOL_REGISTRY frontend. À synchroniser à chaque ajout côté front.
     */
    private static final Set<String> KNOWN_FRONTEND_TOOL_IDS = Set.of(
        "F-DT-04-fiche-prudhomale",
        "F-DT-06-requete-tribunal-travail",
        // ... liste complète des 78 entrées TOOL_REGISTRY
    );

    @Test
    void noOrphanToolIdInVisibilityRules() {
        List<String> dbToolIds = jdbc.queryForList(
            "SELECT DISTINCT tool_id FROM decision_tool_visibility_rules", String.class);

        Set<String> orphans = dbToolIds.stream()
            .filter(id -> !KNOWN_FRONTEND_TOOL_IDS.contains(id))
            .collect(Collectors.toSet());

        assertThat(orphans)
            .as("tool_id seedés en DB mais absents de TOOL_REGISTRY frontend — "
              + "ajouter l'entrée frontend OU supprimer le seed. Voir CLAUDE.md règle "
              + "feedback_pre_merge_visibility_seed_check.")
            .isEmpty();
    }
}
```

La liste `KNOWN_FRONTEND_TOOL_IDS` est extraite **manuellement** de `TOOL_REGISTRY` frontend lors de la création du test, et **ré-extraite** à chaque PR qui touche `TOOL_REGISTRY` ou `decision_tool_visibility_rules`. Pas d'automatisation cross-langage tentaculaire — la règle de gouvernance + le test font le travail.

### Règle CLAUDE.md

Ajout d'une entrée dans le tableau "Blocages automatiques" :

> **Migration Liquibase qui INSERT/UPDATE dans `decision_tool_visibility_rules` un `tool_id` absent de `TOOL_REGISTRY` frontend** | REFUS — ajouter l'entrée `TOOL_REGISTRY` côté frontend (composant + inputs + entry dans la Map) **avant ou en même temps** que le merge backend. Le test `DecisionToolVisibilityIntegrityIT` (F-164 SF-164-01) échoue automatiquement en CI si la règle est violée. **Motivation** : cas réel 2026-04-26 — 14 `tool_id` orphelins dans `decision_tool_visibility_rules` après la vague de 59 features 2026-04-24 → panneau F-IA-04 quasi vide en staging sur dossier E-36, outils silencieusement masqués via `console.warn`. Symétrique de la règle `feedback_pre_merge_endpoint_check` (frontend mergé sans backend). |

### Mémoire

Nouvelle mémoire `feedback_pre_merge_visibility_seed_check.md` symétrique à `feedback_pre_merge_endpoint_check.md`, ligne ajoutée dans `MEMORY.md`.

---

## Critères d'acceptation

- [ ] Migration Liquibase `187-realign-decision-tool-ids.xml` créée, contenant : 2 UPDATE (Cat A) + 7 DELETE (Cat C), + rollback inverse.
- [ ] Migration `db.changelog-master.xml` mise à jour pour inclure 187.
- [ ] 5 nouvelles entrées dans `TOOL_REGISTRY` (`decisional-tools-panel.component.ts`).
- [ ] Composants Angular Cat B importés en haut du fichier (vérifier que les imports n'existent pas déjà).
- [ ] Test `DecisionToolVisibilityIntegrityIT` créé, passe en local.
- [ ] Tests existants `decisional-tools-panel.component.spec.ts` mis à jour si liste TOOL_REGISTRY est testée explicitement (sinon laissés tels quels).
- [ ] CLAUDE.md mis à jour avec la nouvelle règle.
- [ ] Mémoire `feedback_pre_merge_visibility_seed_check.md` créée + ligne dans `MEMORY.md`.
- [ ] **Test manuel staging** : ré-ouvrir le dossier E-36, vérifier que le panel s'affiche avec ses outils, console sans warning `Unknown toolId`.
- [ ] Tous tests verts : `./mvnw test` backend + `npm test` frontend.

---

## Plan de test minimal

### Backend
- `DecisionToolVisibilityIntegrityIT.noOrphanToolIdInVisibilityRules` — vérifie qu'aucun `tool_id` en DB n'est orphelin.
- Tests existants (`mvn test`) doivent rester verts (pas de régression sur l'API visibility).

### Frontend
- `DecisionToolsPanelComponent` spec existant doit rester vert.
- Pas de nouveau test unitaire requis (les 5 entrées ajoutées suivent un pattern déjà testé via les autres entrées).

### Manuel (staging)
- Dossier E-36 (compte ntounga@gmail.com) : panneau F-IA-04 affiche au moins 2 outils ALWAYS_ON travail FR (F-DT-04 fiche prudhomale + F-DT-07 ancienneté) + l'outil contextuel selon `type_rupture`. Console sans `Unknown toolId`.
- Créer un dossier famille FR pour vérifier que les outils famille s'affichent (F-FA-07 checklist divorce si type_procedure_detectee = DIVORCE_CONSENTEMENT_MUTUEL).

---

## Tables / endpoints / composants impactés

### Backend
- **Table** : `decision_tool_visibility_rules` (UPDATE 2 lignes, DELETE 7 lignes).
- **Migration nouvelle** : `187-realign-decision-tool-ids.xml`.
- **Test nouveau** : `DecisionToolVisibilityIntegrityIT.java`.
- **Aucun service** modifié.
- **Aucun endpoint** modifié.

### Frontend
- **Composant modifié** : `decisional-tools-panel.component.ts` (5 entrées ajoutées dans `TOOL_REGISTRY`, 5 imports en tête de fichier si pas déjà présents).
- **Aucun template** modifié.
- **Aucun routing** modifié.

### Documentation
- `CLAUDE.md` : nouvelle règle de blocage.
- `docs/PRODUCT_SPEC.md` : F-164 ajoutée (déjà fait avant cette mini-spec).
- `~/.claude/projects/-home-francky-dev-legalCase/memory/feedback_pre_merge_visibility_seed_check.md` : nouvelle mémoire.
- `~/.claude/projects/-home-francky-dev-legalCase/memory/MEMORY.md` : ligne d'index ajoutée.

---

## Analyse de cohérence transversale

### Périmètres scannés

- **Autres outils décisionnels** : applicable à tous (F-DT, F-IM, F-FA × FR + BE) — c'est précisément ce que ce fix corrige sur l'ensemble du parc en une fois.
- **Autres pays** : France ET Belgique, pas de variation par pays — règle d'intégrité technique transversale.
- **Autres domaines** : DROIT_DU_TRAVAIL, DROIT_FAMILLE, DROIT_IMMIGRATION — affectés en commun.
- **Autres mécanismes de seed-vs-registry** : aucun équivalent existant, mais le pattern (table SQL ↔ enum frontend) pourrait être réutilisé pour `legal_referentials` ↔ `referential.types.ts` côté frontend si bug similaire détecté un jour. Hors scope ici.

### Niveaux vérifiés
- ✅ Modèle TS frontend (`TOOL_REGISTRY`) : 78 IDs.
- ✅ Schéma DB backend (`decision_tool_visibility_rules`) : 90 IDs uniques.
- ✅ Migration Liquibase (105 + ajouts ultérieurs 109/132/150/158/etc.) : sources des INSERT.
- ✅ Tests existants `decisional-tools-panel.component.spec.ts` : ne testent PAS la liste explicite, donc pas d'impact.

### Cohérence IA (F-IA-03), refresh dashboard, pré-fill, persistance, masquage, alertes

Ces préoccupations s'appliquent aux **outils décisionnels** eux-mêmes, pas au mécanisme d'aiguillage. SF-164-01 ne crée ni ne modifie aucun outil décisionnel — elle corrige uniquement la **table d'aiguillage** et le **registry de résolution**. Donc :
- F-IA-03 : non applicable (pas de nouveau champ saisi).
- F-IA-02 refresh dashboard : non applicable.
- Pré-fill IA : non applicable.
- Persistance inputs : non applicable.
- Masquage conditionnel : déjà géré par `decision_tool_visibility_rules` (CONTEXTUAL avec `trigger_field`/`trigger_value`), inchangé.
- Alertes après calcul : non applicable.

---

## Impact par domaine métier

Cette feature est **transversale infrastructure**, sans adaptation par domaine. Le bug et le fix s'appliquent identiquement aux 3 domaines (Travail, Immigration, Famille) × 2 pays (FR, BE). La migration Liquibase touche les seeds de tous les domaines.

---

## Préoccupations transversales (CLAUDE.md)

- [x] **Auth / Principal** : non applicable.
- [x] **Workspace context** : non applicable.
- [x] **Plans / limites** : non applicable.
- [x] **Navigation / routing** : non applicable.
- [x] **Outil décisionnel métier** : applicable mais cette SF n'introduit aucun nouvel outil — elle aligne le mapping existant. Aucun outil décisionnel modifié individuellement.

---

## Hors périmètre

- Création des composants Angular pour les 7 IDs de Cat C supprimés (F-DT-01, F-DT-05, F-FA-01, F-FA-02, F-FA-04, F-FA-18, F-FA-24). Si signal terrain demandant ces outils, rouvrir au backlog avec entrées dédiées et SF jumelles (backend service + frontend component + ajout TOOL_REGISTRY + ré-INSERT seed DB).
- Refactoring de `decision_tool_visibility_rules` (ex: foreign key vers une table `decision_tools_catalog`). Ce serait propre mais hors scope du fix critique.
- Automatisation cross-langage de la liste `KNOWN_FRONTEND_TOOL_IDS` (ex: générer le set Java depuis le fichier TS). La règle de gouvernance + le test manuel suffisent pour le volume actuel (~80 IDs).

---

## Risques

| Risque | Probabilité | Mitigation |
|---|---|---|
| Une SF backend en cours non mergée a déjà ajouté un tool_id qui sera en conflit avec `KNOWN_FRONTEND_TOOL_IDS` | Faible (vague clôturée 2026-04-24) | Vérifier `git log master --since="2026-04-24"` sur `decision_tool_visibility_rules`. |
| Un composant Angular Cat B a une signature `@Input()` différente de ce qu'on attend | Moyen (pas testé en cours de mini-spec) | Lire chaque composant lors du dev avant d'écrire l'entrée TOOL_REGISTRY. |
| Le test d'intégrité liste 78 IDs en dur, drift possible | Faible (taille raisonnable, mise à jour manuelle à chaque changement) | Règle CLAUDE.md + mémoire. |

---

## Estimation
0,5 à 1 jour (1 SF backend + frontend + test + doc, mergeable en 1 PR).
