# Mini-spec — F-96 / SF-96-06 Durcissement prompt `points_procedure` (bannir options stratégiques quand `critere_code = null`)

## Identifiant

`F-96 / SF-96-06`

## Feature parente

`F-96` — Checklist procédurale interactive

## Statut

`ready`

## Date de création

2026-04-30

## Branche Git

`feat/SF-96-06-durcissement-prompt-points-procedure`

---

## Objectif

Durcir le prompt qui produit le champ JSON `points_procedure` afin de bannir explicitement, quand `critere_code = null`, les options stratégiques, les opportunités futures (> 6 mois) et les recommandations d'action — et de rediriger ces éléments vers `risques` ou `questions_ouvertes` (en attendant F-176 qui introduira un bloc dédié `pistes_strategiques`). Effet attendu : la checklist procédurale en immigration ne contient plus que des points binaires VERIFIED / NON_COMPLIANT / TO_CHECK qui ont du sens pour les statuts ✅/❌/⚠️.

---

## Comportement attendu

### Cas nominal

Au moment où Claude construit la réponse JSON (`CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` pour la synthèse standard, `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE` pour la synthèse enrichie), les règles supplémentaires injectées dans la zone `points_procedure` du prompt déclarent explicitement :

1. Quand un point a un `critere_code` énuméré (FR_*, BE_*, IM05_MOTIF, IM06_RECOURS_TYPE, IM07_TITRE_TYPE, FA06_MODE_GARDE, DT09_TYPE_RUPTURE, etc.) → comportement inchangé, le point reste dans `points_procedure`.
2. Quand un point a `critere_code = null` (texte libre), Claude ne doit produire le point dans `points_procedure` **que** s'il s'agit d'une **vérification binaire et présente** d'une étape légalement requise sur le dossier en cours (ex. « Le récépissé de renouvellement doit avoir été délivré avant l'expiration du titre actuel »).
3. Sont **interdits** dans `points_procedure` quand `critere_code = null` :
   - **Options stratégiques** — tout libellé qui commence par « En cas de… », « Si l'avocat envisage… », « Possibilité de demander… », « Alternative : … »
   - **Opportunités futures > 6 mois** — tout libellé qui mentionne un événement à plus de 6 mois de la date courante du dossier (ex. « Après 3 ans de mariage, la carte de résident… »)
   - **Recommandations d'action** — tout libellé qui demande à l'avocat de **faire** quelque chose plutôt que de **vérifier** quelque chose (ex. « Demande de titre de séjour à déposer auprès de la Préfecture », « Joindre la convention d'accueil de l'INRIA »)
4. Pour ces 3 catégories interdites, Claude est instruit de **rediriger** le contenu :
   - vers `risques` si l'élément exprime un risque procédural (délai à venir, condition à remplir)
   - vers `questions_ouvertes` si l'élément suppose une décision stratégique de l'avocat
   - (futur F-176) vers le champ `pistes_strategiques` quand celui-ci existera — pour cette SF, on ne le mentionne pas dans le prompt (sera ajouté par SF-176-01)

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Claude ne respecte pas la règle (régression LLM) | Aucun blocage technique — le point passe quand même, fail-open. La couverture régressive est par tests d'IT sur dossier de référence. | 200 |
| Format JSON invalide | Comportement inchangé (extraction fail-open existante de `ProcedureCheckService.parsePointsProcedure`) | 200 |
| Prompt trop long après ajout des règles | Limites `AnalysisLimitsProperties.LevelLimits` inchangées (les nouvelles règles font ~400 caractères supplémentaires, soit ~100 tokens, négligeable) | — |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : F-DT-07 / F-DT-08 / F-DT-09 / F-DT-10 / F-FA-05 / F-FA-06 / F-FA-07 / F-IM-05 / F-IM-06 / F-IM-07 — non concernés (ces outils consomment `procedure_checks` mais ne produisent pas le contenu, le contenu vient du prompt). La règle est appliquée à la source du flux (le prompt). Aucun de ces outils n'a besoin d'évolution.
- [x] **Autres pays** : France + Belgique — la règle est rédigée de façon générique et s'applique aux 2 pays automatiquement (le prompt n'est pas paramétré par pays sur ce champ).
- [x] **Autres domaines** : DROIT_DU_TRAVAIL / DROIT_FAMILLE / DROIT_IMMIGRATION — la règle est transversale. Effet **majeur** en immigration (où >90 % des points ont aujourd'hui `critere_code = null`). Effet **marginal** en travail et famille (qui ont 14 et 13 critères énumérés couvrant la quasi-totalité du périmètre). Pas d'effet de bord négatif attendu.
- [x] **Autres UI patterns** : aucun — pas de frontend touché.
- [x] **Autres flows transversaux** : pas d'impact auth / workspace / plans / navigation.

### Niveaux de vérification

- [x] **Modèle TypeScript / API exposée** — `ProcedureCheck` interface inchangée, format de réponse identique.
- [x] **Record / DTO backend** — `CaseAnalysisResponse.parsePointsProcedure` inchangé (rétrocompat string legacy maintenue).
- [x] **Service / logique métier** — `ProcedureCheckService.parsePointsProcedure` inchangé.
- [x] **Entité JPA + schéma DB** — `procedure_checks` table inchangée, `critere_code` reste `null` pour le texte libre.
- [x] **Tests existants** — `ProcedureCheckServiceTest` (5 tests prompt-related), `CaseAnalysisServiceTest` (prompt assertions) — à étendre.

### Cas spécifique : pas de nouvelle feature outil décisionnel

(N/A — modification de prompt sans nouvel outil)

### Cas spécifique : pas de nouveau pattern UI

(N/A)

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-DT-07 / F-DT-08 / F-DT-09 / F-DT-10 (Travail FR) | Non (déjà couvert par codes énumérés) | Non applicable — les points avec `critere_code` non null ne sont pas affectés par la nouvelle règle |
| F-FA-05 / F-FA-06 / F-FA-07 (Famille FR + BE) | Non (déjà couvert par codes énumérés FR_/BE_) | Non applicable |
| F-IM-05 / F-IM-06 / F-IM-07 (Immigration FR + BE) | **Oui — bénéficiaire principal** | Intégré dans cette SF (la règle s'applique automatiquement) |
| Autres domaines / pays futurs | Oui en propagation | Intégré (règle générique) |
| F-IA-03 (cohérence IA) | Non | Non applicable — F-IA-03 ne se déclenche que sur les points avec `critere_code` énuméré |
| F-176 (bloc Pistes stratégiques, à venir) | Couplé (prérequis) | Cette SF est prérequis explicite de F-176 ; SF-176-01 ajoutera la mention `pistes_strategiques` au prompt à son tour |
| F-IM-21 (critères binaires immigration, à venir) | Couplé (prérequis indirect) | Cette SF débloque la cohérence d'ensemble (sans elle, F-IM-21 livre des critères binaires mais le prompt continue à mettre des pistes dans `points_procedure`) |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (transversal par construction)
- [x] Subfeature(s) parallèle(s) créée(s) pour les cibles restantes : F-176 SF-176-01 (ajout `pistes_strategiques`), F-IM-21 SF-IM-21-02 (ajout codes IM21_*)
- [x] Backlog : F-176 + F-IM-21 mergés au backlog `PRODUCT_SPEC.md` ce jour (commit `3b2e8c6b`)

---

## Impact par domaine métier

| Domaine | Effet de la SF |
|---------|---------------|
| **Droit du travail (FR)** | Marginal — 14 critères F-DT-08 énumérés (FR_CONVOCATION/FR_ENTRETIEN/etc.) couvrent la majorité du périmètre. Les points texte libre rares ne contiennent généralement pas d'options stratégiques. |
| **Droit du travail (BE)** | Marginal — 7 critères F-DT-08 BE énumérés. Idem FR. |
| **Droit de la famille (FR + BE)** | Marginal — 13 étapes F-FA-07 énumérées + 1 critère F-FA-06. Idem. |
| **Droit de l'immigration (FR)** | **Effet majeur** — seulement 3 critères énumérés (IM05_MOTIF, IM06_RECOURS_TYPE, IM07_TITRE_TYPE) qui sont des sélecteurs de valeur. Tout le reste passe par texte libre, et c'est exactement là que l'IA dérive vers les pistes stratégiques. La SF règle ce comportement. |
| **Droit de l'immigration (BE)** | **Effet majeur** — symétrique FR. |

(Section explicite obligatoire CLAUDE.md — "Impact par domaine métier")

---

## Parité des domaines métier

(N/A — cette SF ne livre pas un outil décisionnel de niveau ≥ 5. C'est une modification de prompt transversale qui s'applique aux 3 domaines × 2 pays automatiquement. Il n'y a pas d'asymétrie créée.)

---

## Critères d'acceptation

- [ ] Le prompt `CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` contient une nouvelle section explicite (3-5 lignes) qui interdit dans `points_procedure` les options stratégiques, opportunités > 6 mois et recommandations d'action quand `critere_code = null`, et qui demande la redirection vers `risques` ou `questions_ouvertes`.
- [ ] Idem pour `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE`.
- [ ] Les règles d'usage du `critere_code` énuméré (codes FR_*, BE_*, IM05_*, IM06_*, IM07_*, FA06_*, DT09_*) restent **strictement inchangées** dans le prompt — pas de régression sur F-DT-08 / F-FA-06 / F-IM-05 / etc.
- [ ] Test unitaire `CaseAnalysisServiceTest` (ou équivalent) qui valide la présence de la nouvelle section dans le prompt généré.
- [ ] Test unitaire `EnrichedAnalysisServiceTest` (ou équivalent) qui valide la présence de la nouvelle section dans le prompt enrichi.
- [ ] Test d'intégration (un suffit) qui exécute le pipeline IA sur un dossier immigration de référence type Chen 2 (mocké AnthropicService) et vérifie : (a) les points avec `critere_code = null` retournés sont des vérifications binaires ; (b) une éventuelle option stratégique du fixture mocké atterrit en `risques` ou `questions_ouvertes`.
- [ ] Aucune migration Liquibase, aucun changement de schéma, aucun changement frontend.
- [ ] Documentation `docs/features/F-96/SF-96-06-durcissement-prompt-points-procedure.md` créée et incluse dans le commit.

---

## Périmètre

### Hors scope (explicite)

- Création du bloc UI `pistes_strategiques` (couvert par F-176 SF-176-02)
- Création du champ JSON `pistes_strategiques` côté backend (couvert par F-176 SF-176-01)
- Création des codes binaires IM21_* (couvert par F-IM-21 SF-IM-21-01/02)
- Modification frontend de `SynthesisComponent` ou de la checklist
- Modification du format JSON de sortie de la synthèse
- Modification du `ProcedureCheckService` ou de l'extraction `parsePointsProcedure`
- Re-traitement automatique des dossiers existants (ré-analyse manuelle si besoin par l'avocat)

---

## Valeurs initiales

(N/A — pas de nouvelle entité)

---

## Contraintes de validation

(N/A — pas de nouveau champ saisi)

---

## Technique

### Endpoint(s)

Aucun nouvel endpoint. Modification interne aux constantes `SYSTEM_PROMPT_TEMPLATE` de 2 services.

### Tables impactées

(N/A)

### Migration Liquibase

- [x] Non applicable

### Composants Angular

(N/A)

### Fichiers backend modifiés

| Fichier | Modification |
|---------|--------------|
| `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisService.java` | Extension `SYSTEM_PROMPT_TEMPLATE` — ajout des règles d'exclusion dans le bloc `points_procedure` (~5-7 lignes après la ligne 60 actuelle). |
| `backend/src/main/java/fr/ailegalcase/analysis/EnrichedAnalysisService.java` | Idem (les 2 prompts ont la même structure pour ce bloc). |
| `backend/src/test/java/fr/ailegalcase/analysis/CaseAnalysisServiceTest.java` | Test prompt — assertion de présence de la nouvelle section. |
| `backend/src/test/java/fr/ailegalcase/analysis/EnrichedAnalysisServiceTest.java` | Idem. |
| `backend/src/test/java/fr/ailegalcase/analysis/ProcedureCheckPromptHardeningIT.java` | **Nouveau** — IT pipeline mocké AnthropicService sur dossier immigration de référence. |

---

## Plan de test

### Tests unitaires

- [ ] `CaseAnalysisServiceTest.systemPromptIncludesStrategicOptionsExclusionRule()` — assert que `buildSystemPrompt(...)` contient les mots-clés interdits explicites
- [ ] `CaseAnalysisServiceTest.systemPromptKeepsExistingEnumeratedCodesIntact()` — non-régression sur F-DT-08 / F-IM-05 / etc.
- [ ] `EnrichedAnalysisServiceTest.systemPromptIncludesStrategicOptionsExclusionRule()` — idem
- [ ] `EnrichedAnalysisServiceTest.systemPromptKeepsExistingEnumeratedCodesIntact()` — idem

### Tests d'intégration

- [ ] `ProcedureCheckPromptHardeningIT.immigration_RegularCase_KeepsBinaryChecks()` — fixture immigration avec mock AnthropicService renvoyant un JSON contenant **uniquement des vérifications binaires** dans `points_procedure` → ces points sont créés en DB
- [ ] `ProcedureCheckPromptHardeningIT.immigration_StrategicOptionInProcedurePoints_DoesNotCrash()` — fixture où le mock renvoie une option stratégique dans `points_procedure` (Claude qui ne respecte pas la règle) → fail-open : le point est quand même créé (pas de blocage technique), test documente le comportement
- [ ] (optionnel) `ProcedureCheckPromptHardeningIT.travail_NoRegression()` — fixture travail FR avec critères F-DT-08 énumérés → comportement inchangé

### Isolation workspace

- [x] Non applicable — la SF ne touche pas la persistance, seulement le prompt construit côté backend pour Anthropic.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — modification interne au prompt LLM, pas d'impact sur les flux d'auth / workspace / plans / navigation

### Composants / endpoints existants potentiellement impactés

(Aucune préoccupation transversale → tableau N/A)

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — la SF modifie le prompt LLM en amont de la persistance, sans changer le contrat API ni les écrans. Les tests `auth.spec.ts` / `workspace.spec.ts` / `navigation.spec.ts` ne sont pas pertinents.

---

## Dépendances

### Subfeatures bloquantes

- Aucune. Cette SF est démarrable immédiatement.

### Subfeatures bloquées par celle-ci

- F-176 SF-176-01 (bloc Pistes stratégiques backend) — dépend de cette SF pour éviter le doublon de pistes dans `points_procedure` + `pistes_strategiques`.
- F-IM-21 SF-IM-21-02 (extension prompt codes IM21_*) — dépend indirectement (le prompt sera étendu en cohérence après que SF-96-06 a posé les règles génériques).

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` n'est tranchée par cette SF.

---

## Notes et décisions

- **Pourquoi pas une nouvelle feature F-175 mais une SF de F-96 ?** F-96 possède le prompt `points_procedure` ; SF-96-04 et SF-96-05 ont déjà étendu ce prompt (injection NON_COMPLIANT/TO_CHECK, requalification). La cohérence du prompt appartient à F-96. F-175 standalone aurait fragmenté la spécification du prompt sur 2 features.
- **Pourquoi pas une règle au niveau extraction (ProcedureCheckService) ?** L'extraction fail-open ne peut pas distinguer une option stratégique d'une vraie vérification — le sens est sémantique et appartient au LLM. La meilleure place pour la règle est le prompt lui-même.
- **Pourquoi le seuil 6 mois ?** Choix produit pragmatique. À 6 mois, on couvre les renouvellements imminents tout en bannissant les opportunités à 3 ans (carte de résident après mariage). Si retour terrain, ajuster.
- **Compatibilité avec F-IA-03** : la SF ne modifie pas les codes énumérés. F-IA-03 reste opérationnel sur les 14 codes existants.
- **Risque LLM (variance)** : Claude peut occasionnellement ignorer la règle (variance ~5-10 % observée sur des règles de prompt similaires F-172). Fail-open accepté en V1. Si problème terrain persistant, ajouter une étape post-extraction de filtrage heuristique en V2.
