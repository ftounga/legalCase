# CLAUDE.md — Instructions projet AI LegalCase

## Documents à lire en priorité

Lire ces documents avant toute réponse impliquant du code, une spec ou une décision technique.

### Architecture
1. `docs/ARCHITECTURE_CANONIQUE.md` — source de vérité architecture (obligatoire)
2. `docs/PRODUCT_SPEC.md` — source de vérité fonctionnelle — liste officielle des features (obligatoire)
3. `docs/OPEN_QUESTIONS.md` — registre des sujets non tranchés (obligatoire)
4. `docs/DESIGN_SYSTEM.md` — charte graphique et règles UI (obligatoire pour tout travail frontend)

### Process
5. `project-governance/playbooks/feature-lifecycle.md` — cycle de vie des features
6. `project-governance/playbooks/definition-of-done.md` — critères de complétion
7. `project-governance/playbooks/coding-rules.md` — conventions de code
8. `project-governance/playbooks/review-rules.md` — critères de review
9. `project-governance/playbooks/testing-strategy.md` — stratégie de test

### Checklists
10. `project-governance/checklists/readiness-checklist.md` — avant de démarrer le dev
11. `project-governance/checklists/review-checklist.md` — avant toute PR
12. `project-governance/checklists/release-checklist.md` — avant tout merge

---

## Règles impératives

### Architecture
- Ne pas réinventer la stack, l'authentification, le modèle multi-tenant ou le modèle de données sans signaler explicitement une variante
- Le client est un workspace, pas un simple utilisateur
- L'utilisateur est une personne physique rattachée à un workspace
- La V1 cible le droit du travail uniquement
- L'auth V1 repose sur Spring Security + OAuth2/OIDC avec Google et Microsoft — aucun mot de passe local
- Backend : Spring Boot | Frontend : Angular | Base : PostgreSQL | Stockage : Object storage S3-compatible
- Les analyses de dossiers sont asynchrones
- Le pipeline IA fonctionne à 3 niveaux : chunk → document → dossier
- L'IA peut poser des questions interactives à l'avocat

---

## Suggestion de la prochaine feature

Quand l'utilisateur dit "on continue", "qu'est-ce qu'on fait ensuite" ou toute formulation équivalente sans préciser de feature :

1. Lire la section **"Features hors V1 (backlog)"** de `docs/PRODUCT_SPEC.md`
2. Proposer les 3 features les plus pertinentes du backlog, regroupées par thème (UX rapide / infrastructure / nouvelle capacité)
3. Ne jamais inventer une feature absente du backlog — si une idée est nouvelle, la soumettre d'abord à validation avant de l'ajouter au backlog et de la proposer
4. Toute feature choisie doit passer par la séquence obligatoire (mini-spec → readiness → dev → review → push)

**REFUS si** : une feature est implémentée sans être référencée dans `docs/PRODUCT_SPEC.md`.

---

## Séquence obligatoire par subfeature

Ce cycle est non négociable. Chaque étape produit un artefact visible dans la conversation.
**Sans l'artefact de l'étape N, l'étape N+1 est refusée.**

```
[1] Mini-spec → [2] Readiness → [3] Dev → [4] Review → [5] Push + Release checklist + PR (atomique) → [6] Docs post-merge
```

### Étape 1 — Mini-spec (ARTEFACT : document SF-XX rempli)

**Avant d'écrire la moindre ligne de code**, produire le fichier mini-spec en utilisant `project-governance/templates/subfeature-template.md` comme base.

Le fichier doit contenir :
- Objectif en une phrase
- Comportement nominal + cas d'erreur
- Critères d'acceptation vérifiables
- Plan de test minimal (unitaires + intégration + isolation workspace)
- Tables / endpoints / composants impactés
- Ce qui est hors périmètre

Le fichier est créé dans `docs/features/F-XX/SF-XX-YY-nom.md` et son contenu est affiché dans la conversation.

**REFUS si** : le dev démarre sans que ce fichier soit produit et visible dans la conversation.

---

### Étape 2 — Readiness checklist (ARTEFACT : checklist passée item par item)

Avant de créer la branche et d'écrire le code, passer `project-governance/checklists/readiness-checklist.md` et afficher le résultat dans la conversation avec un verdict PASS / FAIL explicite.

**REFUS si** : le premier commit est créé sans que la readiness checklist ait été passée dans cette conversation.

---

### Étape 3 — Dev

Travailler sur une branche `feat/SF-XX-YY-nom-court` créée depuis `master` à jour.
Respecter `project-governance/playbooks/coding-rules.md`.
Toute décision technique non prévue dans la mini-spec est documentée dans la PR.

#### Parallélisation backend / frontend (optionnelle)

Quand une feature comporte une subfeature backend et une subfeature frontend indépendantes (le frontend consomme une API exposée par le backend), les deux SF peuvent être développées **en parallèle** à condition que :

1. **Le contrat API est figé avant le dev dans la mini-spec** — chaque SF contient explicitement la section "Contrat API" avec :
   - Méthode HTTP + URL exacte
   - Schema du body de requête (champs, types, validation)
   - Schema de la réponse (tous les champs retournés)
   - Codes d'erreur et messages attendus
   - Codes enum éventuels (valeurs exactes et cas d'emploi)
2. **Chaque SF travaille sur sa propre branche** — `feat/SF-XX-YY-backend` et `feat/SF-XX-YY-frontend`, créées depuis `master` à jour.
3. **Chaque SF produit une PR indépendante** — elles peuvent être mergées dans n'importe quel ordre, mais la frontend ne sera utilisable en production qu'après le merge du backend.
4. **Les tests frontend utilisent un mock du service** — pas besoin d'attendre le backend mergé pour faire passer les tests unitaires Jest. L'intégration réelle (end-to-end) est validée après merge des deux PRs.
5. **Les deux mini-specs se référencent mutuellement** — la SF frontend indique "contrat importé de SF-XX-YY-backend" pour traçabilité.

**REFUS si** : deux SF sont lancées en parallèle sans que le contrat API soit présent explicitement dans la mini-spec backend — risque de divergence = dette de convergence immédiate.

**REFUS si** : deux SF parallèles partagent la même branche Git — elles doivent être strictement isolées.

Cette règle s'applique uniquement quand la parallélisation est **explicitement décidée**. Le mode sequentiel standard (SF backend mergée → SF frontend démarrée) reste le défaut.

---

### Étape 4 — Review checklist (ARTEFACT : checklist passée item par item)

Avant tout `git push`, lire `project-governance/checklists/review-checklist.md` et afficher le résultat dans la conversation avec un verdict PASS / FAIL explicite et les items bloquants identifiés.

Les items bloquants doivent être corrigés avant le push. Un item non bloquant peut être poussé avec une note explicite.

**REFUS si** : `git push` est exécuté sans que la review checklist ait été passée et affichée dans cette conversation.

---

### Étape 5 — Push, Release checklist et PR (étape atomique, non séparable)

Ces trois actions forment un bloc indivisible exécuté dans cet ordre exact :

1. `git push -u origin feat/SF-XX-YY-nom-court`
2. Passer `project-governance/checklists/release-checklist.md` item par item et afficher le résultat avec verdict PASS / FAIL — **ARTEFACT obligatoire**
3. Afficher le template PR rempli dans la conversation (titre, corps, checklist)

L'utilisateur ne voit le template PR qu'après avoir vu la release checklist. Il n'y a pas d'étape 6 séparée.

**REFUS si** : le push est effectué sans que la release checklist soit produite dans la même réponse.

**REFUS si** : une nouvelle subfeature démarre alors que la release checklist de la subfeature précédente n'a pas été passée dans cette conversation.

### Étape 6 — Mise à jour documentation post-merge (ARTEFACT : PRODUCT_SPEC.md à jour)

Dès que l'utilisateur confirme le merge ("mergé", "PR mergée", ou équivalent) :

1. Mettre à jour le statut de la feature parente dans `docs/PRODUCT_SPEC.md` si toutes ses subfeatures sont Done
2. Ajouter une ligne dans l'historique des évolutions de `docs/PRODUCT_SPEC.md`
3. Si une nouvelle table a été créée : vérifier et mettre à jour `docs/ARCHITECTURE_CANONIQUE.md`
4. Commiter ces mises à jour directement sur master

**REFUS si** : la feature parente est complète et PRODUCT_SPEC.md n'a pas été mis à jour avant de démarrer la feature suivante.

---

### Étape 7 — Sync backlog DB (automatique post-merge — F-178)

Toute modification de `docs/PRODUCT_SPEC.md` ou `docs/MARKETING_BACKLOG.md` doit aboutir à une synchronisation des tables `backlog_features`, `backlog_subfeatures`, `backlog_marketing_tasks` consommées par l'écran super-admin `/super-admin/backlog` (F-178).

**Mode normal — automatique** : tâche `@Scheduled` cron 5 min qui parse les 2 fichiers et upsert les tables. Audit dans `backlog_sync_runs` (timestamp, durée, count, success/error). **Aucun artefact obligatoire côté contributeur** — le merge sur master suffit.

**Mode resync manuelle — opérationnel** : si une modification doit être visible immédiatement (démo, présentation, debug), cliquer **"Resync now"** dans l'écran `/super-admin/backlog` (super-admin only). L'écran affiche un indicateur de fraîcheur ("Synchronisé il y a X minutes") visible en haut.

**Si la sync échoue répétitivement** (visible dans `backlog_sync_runs.success = false` sur plusieurs runs consécutifs) : ouvrir un ticket — ne pas éditer la DB à la main (les MD restent la source de vérité, la DB sera ré-écrasée au prochain cron réussi).

**REFUS si** : édition directe des tables `backlog_*` sans passer par l'édition du fichier MD source. Les MD sont la source de vérité (Option A retenue F-178), la DB est un cache de lecture.

---

## Blocages automatiques

Ces situations déclenchent un refus immédiat. Répondre avec le format de refus standard.

| Situation | Réponse |
|-----------|---------|
| Demande brute couvrant plusieurs features distinctes | REFUS — séparer les features avant tout découpage |
| Demande de code sans mini-spec produite dans la conversation | REFUS — produire la mini-spec d'abord (`subfeature-template.md`) |
| Demande de code sans critères d'acceptation dans la mini-spec | REFUS — compléter la mini-spec |
| Demande de code sans plan de test dans la mini-spec | REFUS — compléter la mini-spec |
| Mini-spec sans section "Analyse de cohérence transversale" remplie | REFUS — scanner les autres outils / pays / domaines / UI patterns d'abord, classer chaque cible applicable (intégrée / SF parallèle / backlog / non applicable avec justification) |
| SF introduit un composant partagé / service / endpoint transversal / directive / DTO réutilisable sans section "Nouveau pattern UI ou service partagé" remplie | REFUS — scanner toutes les zones où le pattern pourrait être réutilisé (badges, tooltips, popovers, panneaux existants), identifier les patterns concurrents à harmoniser, classer chaque cible (harmonisation immédiate / SF parallèle / backlog / non applicable). Évite la *dette de convergence* (2 mécanismes similaires qui divergent dans le temps). |
| Feature non découpée en subfeatures | REFUS — demander le découpage (`feature-splitter`) |
| Subfeature estimée > 2 jours | REFUS — demander un redécoupage |
| `git push` sans review checklist passée dans la conversation | REFUS — passer la review checklist d'abord |
| Push sans release checklist produite dans la même réponse | REFUS — release checklist fait partie du même bloc que le push |
| Démarrage d'une nouvelle subfeature sans release checklist passée pour la précédente | REFUS — produire la release checklist avant de continuer |
| Merge confirmé sans mise à jour PRODUCT_SPEC.md si feature parente complète | REFUS — mettre à jour PRODUCT_SPEC.md d'abord |
| Question ouverte non tranchée et bloquante | BLOCAGE — signaler, ne pas avancer |
| Incohérence avec `ARCHITECTURE_CANONIQUE.md` | BLOCAGE — signaler la divergence |
| Feature non référencée dans `PRODUCT_SPEC.md` | REFUS — ajouter la feature au PRODUCT_SPEC avant tout dev |
| Traitement IA demandé de façon synchrone | REFUS — rappeler la règle async |
| Accès données sans filtre `workspace_id` | REFUS — rappeler la règle d'isolation |
| Composant frontend utilisant couleurs/polices hors `DESIGN_SYSTEM.md` | BLOCAGE — signaler la divergence |
| Ecran produit sans header/layout conforme au design system | BLOCAGE — signaler la divergence |
| Feature avec écran utilisateur marquée `Terminée` sans composant Angular implémenté | REFUS — implémenter les écrans manquants avant de marquer Terminée |
| Subfeature backend mergée sans subfeature frontend planifiée (si la feature a une UI) | BLOCAGE — planifier et créer la subfeature frontend correspondante avant de continuer |
| Merge d'une SF frontend qui consomme un endpoint API alors que la SF backend correspondante n'est NI mergée NI dans une PR ouverte avec tests verts | REFUS — la SF frontend ne sera pas utilisable en production sans le backend (404 en runtime). **Vérifier avant tout `gh pr merge` frontend** : (1) lister les endpoints consommés par les services Angular modifiés (`HttpClient.post`/`get`/`put`/`delete` sur `/api/v1/...`), (2) pour chaque endpoint, confirmer qu'une route Spring `@RequestMapping` existe sur master OU sur une PR ouverte mergeable. Si absent : implémenter la SF backend manquante d'abord, ou ne pas merger la frontend. **Motivation** : cas réel F-DT-29 le 2026-04-25 — SF-DT-29-02 frontend mergée (PR #624) consommait `/api/v1/case-files/{id}/credit-temps-be-analysis` qui n'existait pas. Cause racine : worktree backend SF-DT-29-01 de la vague 19 crashé sans commit, branche `feat/SF-DT-29-01-...` créée vide à master. Anomalie détectée seulement après merge ; correctif PR #626 implémenté rétroactivement le jour même. Symétrique de la règle ci-dessus (backend sans frontend planifié). |
| Migration Liquibase qui INSERT/UPDATE dans `decision_tool_visibility_rules` un `tool_id` absent de `TOOL_REGISTRY` frontend (et de la liste `KNOWN_FRONTEND_TOOL_IDS` du test d'intégrité) | REFUS — l'outil sera silencieusement masqué en runtime (`resolveEntry()` retourne `null`, `console.warn`). **Vérifier avant tout merge backend** qui touche `decision_tool_visibility_rules` : (1) lister les `tool_id` ajoutés/modifiés ; (2) confirmer que chaque ID possède une entrée `TOOL_REGISTRY` dans `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` (mergée OU dans une PR ouverte mergeable) ; (3) mettre à jour `KNOWN_FRONTEND_TOOL_IDS` dans `DecisionToolVisibilityIntegrityIT` pour que la CI passe. Le test échoue automatiquement si un `tool_id` est orphelin. **Motivation** : cas réel 2026-04-26 — 15 `tool_id` orphelins dans `decision_tool_visibility_rules` après les vagues de fin avril 2026 → panneau F-IA-04 quasi vide en staging sur dossier E-36 (ntounga@gmail.com), outils silencieusement masqués. Symétrique de la règle ci-dessus (frontend mergé sans backend). Garde-fou F-164 SF-164-01. |
| Préoccupation transversale cochée sans liste de composants impactés dans la mini-spec | BLOCAGE — compléter l'analyse d'impact avant de continuer |
| Smoke tests E2E échouent après implémentation d'une préoccupation transversale | BLOCAGE — corriger avant push |
| Ajout backlog ou SF touchant un outil décisionnel métier sans scan systématique des autres outils décisionnels | REFUS — scanner tous les outils décisionnels existants, classer chacun (déjà séparé / multi-situations à scinder / paramétrage simple), inclure les cas jumeaux dans le périmètre ou ouvrir des features jumelles au backlog avant de continuer |
| Mini-spec sans section **"Impact par domaine métier"** remplie | REFUS — pour chaque SF, répondre explicitement : cette feature est-elle sensible au domaine (droit du travail / immigration / famille) ? Si oui, comment elle se comporte sur les 3 — et sur les 2 pays (FR + BE) quand pertinent. Si transversale ("infrastructure, aucune adaptation par domaine"), le dire explicitement. Cette section **existe dans toute mini-spec**, même courte. Évite les biais où une feature hardcode implicitement le droit du travail (cas réel F-145 SF-145-01 : enum `DocumentPieceType` initial adapté uniquement droit du travail — correction via SF-145-09). |
| SF livre un outil décisionnel de **niveau ≥ 5** (scoring / comparateur / détection d'événement) sans section **"Parité des domaines métier"** remplie | REFUS — à chaque scoring (niveau 5), comparateur (niveau 6) ou détection d'événement déclencheur (niveau 7) livré pour un domaine, lister explicitement si les 2 autres domaines ont l'équivalent. Si non : ouvrir une feature jumelle au backlog avec numéro dédié, ou justifier pourquoi le concept n'est pas pertinent sur cet autre domaine. **Motivation** : la règle "Impact par domaine métier" ci-dessus est préventive mais ne corrige pas l'asymétrie déjà créée. Historique : F-DT-08/09/10 livrées en avril 2026 pour le droit du travail seul, laissant Famille et Immigration 3 niveaux en retard — corrigé via F-150 à F-153 en avril 2026. Les 7 niveaux de profondeur : (1) Checklist, (2) Générateur de document, (3) Calculateur, (4) Arbre décisionnel, (5) Scoring / analyse validité, (6) Comparateur / fourchettes, (7) Détection événement déclencheur. |
| Migration Liquibase qui INSERT une entry `legal_referentials` avec `is_system=true` sans la colonne `description` renseignée | REFUS — ajouter `description` (texte en langage avocat — ce que c'est, par quel outil c'est utilisé, comment l'ajuster). Seuls les 7 types à description riche native dans `value_json` (`LICENCIEMENT_CRITERES`, `RUPTURE_CONV_CRITERES`, `IMMIGRATION_TITLES`, `IMMIGRATION_RECOURS`, `IMMIGRATION_WORK_RIGHTS`, `DIVORCE_ETAPES`, `DIVORCE_PIECES`) en sont exemptés. Le test `LegalReferentialDescriptionIntegrityIT` échoue automatiquement en CI si la règle est violée (garde-fou F-140 SF-140-03). |
| Modification d'un référentiel métier statique (classe `*Referentiel.java` sous `fr.ailegalcase.casefile` ou `fr.ailegalcase.referential`) **sans migration Liquibase accompagnant l'INSERT/UPDATE dans la table `legal_referentials`** | REFUS — la classe Java statique n'est qu'un **fallback**, la source de vérité est la table `legal_referentials` consommée par `LegalReferentialService`. Toute nouvelle entrée (type, code, mapping) ajoutée au Java DOIT être accompagnée d'une migration Liquibase correspondante (avec `description` SF-140-03 si non exempté). Divergence Java vs DB = bug silencieux en prod où la DB prime. Cas historique F-IM-01 SF-IM-01-04 2026-04-23 : 9 nouveaux types ajoutés en Java uniquement, seed DB oublié — rattrapé via migration 101. À chaque ajout de régime/code/type dans un référentiel : (1) Java fallback, (2) migration INSERT `legal_referentials`, (3) description obligatoire, (4) vérifier que l'UUID ne collisione pas avec les migrations existantes. |
| Migration Liquibase qui INSERT un nouveau `referential_type` dans `legal_referentials` (`is_system=true`) **sans intégration UX dédiée côté frontend** (entrée `SECTION_LABELS` + branche `formatValue` + branche `sectionIcon` dans `frontend/src/app/referentials/referentials.component.ts` + éventuellement `buildForm` du edit dialog) | REFUS — l'écran "Guide & Barème" affichera un titre brut (code DB), une valeur en `JSON.stringify` et une icône générique `info` au lieu d'une UX adaptée. **Vérifier avant tout merge** : (1) ajouter le label humain français dans `SECTION_LABELS` ; (2) brancher `formatValue()` pour un rendu lisible (pas JSON brut) ; (3) brancher `sectionIcon()` avec une icône MatIcon pertinente ; (4) si la structure d'édition est complexe, étendre `buildForm()` du `ReferentialEditDialogComponent` ; (5) ajouter le type dans `KNOWN_FRONTEND_REFERENTIAL_TYPES` du test `LegalReferentialDescriptionIntegrityIT` (garde-fou F-225 SF-225-03). Le test échoue automatiquement en CI si la règle est violée. **Motivation** : cas réel des 5 types orphelins identifiés audit 2026-05-06 (CONVENTION_PREAVIS / TRAVAIL_PROCEDURE_JALONS / FAMILLE_PROCEDURE_JALONS / MAJEURS_PROTEGES_REGIMES / IM21_VALIDITY_CRITERES) — F-225 SF-225-01 a livré le rattrapage UX. |
| Composant décisionnel frontend (`<app-XXX-section>` intégré au panel F-IA-04 via `TOOL_REGISTRY`) **sans référence explicite au template canonique** dans la mini-spec | REFUS — chaque nouveau composant décisionnel doit nommer le composant de référence (`immigration-title-decision-section` post-SF-177-12) et avoir passé la checklist du skill `ai-skills/frontend-coherence-audit.md` (§5 + §6, 19 items). Audit obligatoire tous les 5 nouveaux composants. **Motivation** : sans template canonique, chaque outil dérive en silo → dette de convergence (cas réel F-155 rétroactif sur 6 composants, fin 2026-04-24). |
| Composant décisionnel frontend **sans checklist cohérence visuelle passée** (palette / datepicker / typographie / gate pays / refresh / snackbar) | REFUS — appliquer strictement : (1) palette statut navy/or/vert, **rouge réservé aux alertes critiques** ; (2) `<input type="date">` ou `datetime-local` selon précision — **pas** `MatDatepicker` ; (3) `JetBrains Mono` pour `baseJuridique` et `formule`, `Inter` pour le reste ; (4) gate `workspaceCountry` = **bannière info** si mismatch — pas de masquage silencieux ; (5) `CaseDashboardRefreshService.triggerRefresh()` invoqué dans `next:` du POST de validation (pattern SF-IA-02-03) ; (6) `MatSnackBar` pour les erreurs — **pas** d'`alert()` / `confirm()`. Pattern de référence : `immigration-title-decision-section`. Skill : `ai-skills/frontend-coherence-audit.md` §6. **Motivation** : dette de convergence visuelle observée fin 2026-04-24 sur 6 composants (palette, pickers, handling country) → F-155 rétroactif. |
| Composant décisionnel frontend **sans pré-remplissage IA fonctionnel** (`@Input aiData?` + `prefillFromAi()` + signals provenance + badge `auto_awesome` + handlers reset) | REFUS — **FAIL, pas WARN** : c'est un **bug produit**, l'outil devient « encore un formulaire » au lieu d'un assistant. Vérifier : (1) `@Input() aiData?` typé strictement (`TravailExtractedData` / `ImmigrationExtractedData` / `FamilleExtractedData`) ; (2) `prefillFromAi()` invoqué dans `ngOnInit()` **ET** `ngOnChanges()` (cas où `aiData` arrive après la première résolution) ; (3) signal `provenance<Field> = signal<'IA'\|null>(null)` par champ pré-rempli ; (4) badge UI `auto_awesome` « Pré-rempli depuis l'analyse » à côté du champ ; (5) handler `onXxxChange()` qui remet la provenance à `null` au changement manuel. Pattern de référence : `immigration-title-decision-section.prefillFromAi()`. Skill : `ai-skills/frontend-coherence-audit.md` §6 items pré-fill. |
| Composant décisionnel frontend **sans validation IA au changement F-IA-03** (`coherenceAlerts` computed + `<app-coherence-popover-trigger>` + helper partagé `CoherenceAlertBuilder`) | REFUS — **FAIL, pas WARN** : sans cette validation, l'avocat peut saisir une valeur en contradiction directe avec l'analyse IA sans alerte visuelle — **bug produit**. Vérifier : (1) `coherenceAlerts = computed<Partial<Record<FieldName, CoherenceAlert>>>()` croisant 4 sources IA (`aiData`, `procedureChecks` F-96, `aiQuestions`, `piecesManquantes`) ; (2) hiérarchie F-96 > Question IA > IA détection > Pièce manquante respectée, source `MULTI` si convergence ; (3) directive `<app-coherence-popover-trigger>` câblée sur chaque field concerné (affichage non bloquant) ; (4) helper partagé obligatoire `frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts` — **pas** de définition locale ad hoc de l'interface `CoherenceAlert` (dette de convergence). Pattern de référence : `immigration-title-decision-section` (`buildMotifAlert`, `buildNationaliteAlert`, `coherenceAlerts`, `alertsSummary`). Skill : `ai-skills/frontend-coherence-audit.md` §6 items F-IA-03. |
| Composant décisionnel frontend **sans entrée `TOOL_REGISTRY` symétrique aux autres outils** (inputs IA complets, constantes `TOOL_LABEL` / `TOOL_ICON`) | REFUS — l'outil ne sera pas correctement présenté dans le panel F-IA-04 et la validation F-IA-03 ne recevra pas les sources IA. Vérifier dans `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` : (1) entrée `TOOL_REGISTRY` ajoutée avec `inputs: (ctx) => ({ caseFileId, workspaceCountry, aiData, procedureChecks, aiQuestions, piecesManquantes })` — **toutes** les sources IA nécessaires à F-IA-03 ; (2) constantes `TOOL_LABEL` et `TOOL_ICON` symétriques aux autres outils (pattern SF-177-03b/05/07) ; (3) `tool_id` présent dans `KNOWN_FRONTEND_TOOL_IDS` du test `DecisionToolVisibilityIntegrityIT` (garde-fou F-164 SF-164-01). |
| Composant décisionnel frontend **sans `static getPrefillCount(input): number`** parité stricte avec `prefillFromAi()` runtime + tests Jest (0/M/N champs) | REFUS — sans cette méthode, le panel F-IA-04 affiche un badge `auto_awesome` faux ou absent AVANT instanciation du composant. Vérifier : (1) signature `static getPrefillCount(input: { aiData?, procedureChecks?, aiQuestions?, piecesManquantes?, triggerEvents?, workspaceCountry? }): number` ; (2) **parité stricte** runtime↔static via le helper partagé `<ComponentName>PrefillRules` — mêmes guards `typeof === 'string'`, mêmes mappings, mêmes conditions de pays (toute divergence = bug = badge faux) ; (3) tests Jest obligatoires : 0 champs (return 0), M champs partiels, N champs cas nominal. Pattern miroir `TOOL_LABEL` / `TOOL_ICON` (SF-177-03b/05/07, étendu SF-177-12). Pattern de référence : `immigration-title-decision-section`. Garde-fou F-236 SF-236-05 (`prefill-count-integrity.spec.ts`) — la CI échoue automatiquement. **Motivation** : audit 2026-05-10 — 79 composants sur 103 (77 %) en infraction. |
| SF frontend décisionnelle mergée sans pré-remplissage IA fonctionnel **OU** sans validation F-IA-03 au changement | REFUS — ces 2 mécanismes sont la **différenciation produit** (vs "encore un formulaire") et l'articulation entre l'IA et les outils décisionnels. Sans pré-fill, l'avocat ressaisit ce que l'IA a déjà extrait. Sans validation F-IA-03, l'avocat peut saisir une valeur en contradiction directe avec l'analyse IA sans alerte visuelle. Ce n'est pas de la dette technique — c'est un bug produit. Toute SF qui livre un composant décisionnel frontend sans ces 2 mécanismes doit être corrigée en SF suivante immédiate avant toute nouvelle feature. Audit systématique obligatoire lors de tout audit périodique de cohérence frontend (règle des 5 composants). |
| Migration Liquibase ou seed qui INSERT/UPDATE dans `decision_tool_visibility_rules` (mode `visibility` ou flag) **sans audit "Impact F-166 cross-C×D" rempli dans la mini-spec** | REFUS — toute modification du registre de visibilité doit être analysée croisée Country × Domain (FR×Travail, FR×Immigration, FR×Famille, BE×Travail, BE×Immigration, BE×Famille) avant merge. **Vérifier dans la mini-spec** : (1) la cellule par défaut visée (ALWAYS_ON / CONTEXTUAL / OFF) pour chaque combinaison C×D ; (2) l'impact concret sur le panel F-IA-04 dans chaque cellule (outil apparaît / disparaît / change de mode) ; (3) la cohérence avec les autres outils déjà présents dans la même cellule (effet d'accumulation, conflit de mode). **Motivation** : sans cet audit, on accumule silencieusement des outils ALWAYS_ON candidats CONTEXTUAL ou inversement, dérive du périmètre F-IA-04 et bruit visuel pour l'avocat. Symétrique des garde-fous F-164 SF-164-01 (orphelins frontend) et F-140 SF-140-03 (description). Garde-fou F-199 SF-199-02. |
| Ajout d'une entrée seed `legal_referentials` ou `decision_tool_visibility_rules` avec `country='FR'` ou `country='BE'` **sans audit explicite "exhaustivité du droit national X-FR/BE" rempli dans la mini-spec** | REFUS — pour chaque entrée nationale ajoutée côté FR, l'équivalent BE doit avoir été vérifié (existe / pas pertinent — justifier) ; et inversement pour chaque entrée BE. **Vérifier dans la mini-spec** : (1) source juridique nationale (Code du travail FR / Code civil FR ; Code du travail BE / Code civil BE / régimes Bruxelles-Wallonie-Flandre) ; (2) équivalent dans l'autre pays — entrée jumelle simultanée OU justification explicite que le concept n'existe pas dans le droit national de l'autre pays ; (3) cohérence terminologique (libellés, codes, descriptions) avec les seeds existants déjà rapprochés FR↔BE. **Motivation** : éviter le biais où un outil ou un référentiel est implémenté pour 1 pays sans considérer l'équivalent dans l'autre, créant une asymétrie qu'il faudra rattraper rétroactivement (cas historique F-150 à F-153 pour les domaines, transposable au seed national). Couvre le seeding de l'invariant "Belgique never forget" (cf. mémoire `feedback_belgique_never_forget.md` : couverture exhaustive du droit belge attendue, pas miroir FR). Garde-fou F-199 SF-199-02. |
| Composant Angular décisionnel frontend (entrée `TOOL_REGISTRY`) sans `static getPrefillCount(input): number` exposé OU avec `getPrefillCount` retournant NaN/Infinity/négatif | REFUS — l'avocat ne verra pas le badge `auto_awesome (+N)` du panel F-IA-04, ce qui invalide la promesse UX "outils décisionnels assistés par l'IA". **Vérifier avant tout merge** : (1) la méthode statique est exposée ; (2) elle reproduit fidèlement la logique de `prefillFromAi()` runtime via le helper partagé `<ComponentName>PrefillRules` ; (3) elle est testée Jest dans 3 cas (0/M/N champs). Le test `prefill-count-integrity.spec.ts` échoue automatiquement en CI si la règle est violée. **Motivation** : audit 2026-05-10 — 79 composants sur 103 (77 %) en infraction, badge silencieusement absent partout sauf 4 outils. Garde-fou F-236 SF-236-05. |

**Format de refus standard :**
```
REFUS [contexte]
Motif : [raison précise]
Artefact manquant : [ce qui doit être produit]
Référence : [fichier de gouvernance concerné]
```

---

## Détection des demandes multi-features

Avant tout traitement, analyser si la demande brute couvre une seule feature ou plusieurs.

Une demande doit être considérée comme **potentiellement multi-features** si elle contient :
- plusieurs comportements visibles distincts et indépendants
- plusieurs responsabilités métier séparables
- plusieurs écrans ou endpoints indépendants qui ne partagent pas de flux unique
- plusieurs entités principales impactées de façon indépendante

**Règle :** Une demande multi-features ne doit jamais être traitée comme une feature unique sans arbitrage préalable.

**Action requise si multi-features détectée :**
```
REFUS [contexte]
Motif : La demande couvre plusieurs features distinctes.
Features identifiées : [liste des features détectées]
Action requise : Séparer en features indépendantes et traiter chacune séparément.
Référence : CLAUDE.md — Détection des demandes multi-features
```

---

## Préoccupations transversales — règle anti-régression

Certaines modifications impactent silencieusement des composants existants qui n'ont pas été touchés.
Ces **préoccupations transversales** doivent être traitées explicitement à chaque subfeature.

### Déclencheurs obligatoires

| Préoccupation | Exemples concrets | Action requise |
|--------------|------------------|----------------|
| **Auth / Principal** | Nouveau type d'auth, modification du Principal, changement de session | Lister tous les `@AuthenticationPrincipal` existants. Vérifier que chacun supporte le nouveau type. Ajouter test de non-régression. |
| **Workspace context** | Nouveau moyen de résoudre le workspace, changement de `workspace_id` | Lister tous les composants qui résolvent le workspace. Vérifier leur comportement. |
| **Plans / limites** | Nouveau plan, changement de quota, nouveau gate | Lister tous les appels à `PlanLimitService`. Vérifier les gates. |
| **Navigation / routing** | Nouvelle route, guard modifié, redirection ajoutée | Vérifier tous les chemins de navigation existants. Lancer les smoke tests. |
| **Outil décisionnel métier** | Création, modification ou observation concernant un outil décisionnel (calculator / analyzer / generator / decision engine côté backend ; section composant côté frontend). Inclut tout ajout backlog, toute SF qui touche un outil existant, toute observation de bug qui en mentionne un. | **Lister tous les outils décisionnels** (F-DT-07/08/09/10, F-IM-05/06/07, F-FA-05/06/07, etc.). **Scanner chacun** pour vérifier s'il contient un switch conditionnel sur un type métier, un pays ou un mode qui mélange plusieurs situations distinctes. **Classer** chaque outil : déjà séparé / multi-situations à scinder / paramétrage simple. **Appliquer l'invariant** : un outil décisionnel = une situation métier (pattern F-DT-08/F-DT-10). Si un autre outil présente le même pattern que celui à l'origine de la demande, l'inclure dans le périmètre ou ouvrir une feature jumelle au backlog. |

### Règle de blocage automatique

Si une subfeature coche une préoccupation transversale dans sa mini-spec **sans liste de composants impactés** → BLOCAGE.
Si les smoke tests E2E échouent après l'implémentation → BLOCAGE avant push.

### Suite de smoke tests E2E

Les tests de non-régression automatiques sont dans `e2e/smoke/`.
Lancer avant tout push touchant une préoccupation transversale :

```bash
cd e2e && npm test
```

Les smoke tests couvrent les chemins critiques d'intégration :
- `auth.spec.ts` — login local, login OAuth, logout, redirect non-authentifié
- `workspace.spec.ts` — switch workspace → rechargement des dossiers
- `navigation.spec.ts` — invitation → /login, guards, redirections

---

## Sujets non tranchés

- Toute décision touchant à `docs/OPEN_QUESTIONS.md` doit être explicitement posée avant implémentation
- Ne jamais implémenter silencieusement une solution à un sujet ouvert

## Features — règle d'existence

- Toute feature implémentée doit être référencée dans `docs/PRODUCT_SPEC.md`
- Toute nouvelle feature doit être ajoutée à `docs/PRODUCT_SPEC.md` et validée avant tout dev
- Le statut de chaque feature dans `docs/PRODUCT_SPEC.md` doit être maintenu à jour

---

## Quand tu proposes une modification

1. Rappeler la décision actuelle (architecture ou process)
2. Expliquer la variante proposée et son impact
3. Ne jamais remplacer silencieusement une décision existante
4. Si la modification touche un sujet ouvert, le signaler

---

## Agents et skills disponibles

### Agents
- `ai-agents/orchestrator/delivery-orchestrator.md` — point d'entrée de tout dev
- `ai-agents/backend/backend-agent.md` — implémentation Spring Boot
- `ai-agents/frontend/frontend-agent.md` — implémentation Angular
- `ai-agents/qa/qa-agent.md` — validation qualité
- `ai-agents/review/review-agent.md` — review de code
- `ai-agents/docs/docs-agent.md` — cohérence documentaire

### Skills
- `ai-skills/feature-splitter.md` — découper une feature en subfeatures
- `ai-skills/story-writer.md` — rédiger une mini-spec
- `ai-skills/test-case-generator.md` — générer un plan de test
- `ai-skills/review-checklist-runner.md` — évaluer une PR
- `ai-skills/definition-of-done-checker.md` — valider la complétude
- `ai-skills/feature-autonome.md` — livrer une feature complète **toutes SF d'affilée** sans pause entre SF (à invoquer quand l'utilisateur demande "implémente F-XXX en autonome" / "vas-y jusqu'au bout" / équivalent)

---

## Commandes de développement

### Démarrer le backend

**Profil `dev` (H2 en mémoire — pas besoin de Docker)**
```bash
source .env.local
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
- Port : 8080 | Base : H2 en mémoire (données perdues à chaque redémarrage)
- Console H2 : http://localhost:8080/h2-console

**Profil `local` (PostgreSQL + MinIO via docker compose)**
```bash
source .env.local
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
- Port : 8080 | Base : PostgreSQL (données persistantes)
- Requiert : `docker compose up -d`

### Démarrer le frontend
```bash
source ~/.nvm/nvm.sh && nvm use 22
cd frontend && npm start
```
- Port : 4200
- Node 22 requis (géré via nvm)

### Démarrer PostgreSQL (prod locale)
```bash
docker compose up -d
```
- Port : 5432
- DB : `legalcasedb` / User : `legalcase` / Password : `legalcase`

### Accès base de données H2 (dev uniquement)
- URL : http://localhost:8080/h2-console
- JDBC URL : `jdbc:h2:mem:legalcasedb`
- Utilisateur : `sa` / Mot de passe : (vide)

### Builder le backend sans tests
```bash
cd backend && ./mvnw clean package -DskipTests
```

### Builder le frontend
```bash
source ~/.nvm/nvm.sh && nvm use 22
cd frontend && npm run build
```

---

## Commandes de déploiement cloud

### Référence rapide — toutes les commandes

| Intention | Commande naturelle |
|-----------|-------------------|
| Redémarrer l'appli en local (dev) | "Redémarre l'application en local" |
| Redémarrer l'appli en local (local/postgres) | "Redémarre l'application en local avec PostgreSQL" |
| Déployer en staging via CI/CD | "Lance le workflow de déploiement staging" |
| Déployer en production via CI/CD | "Lance le workflow de déploiement production" |
| Déployer manuellement en staging | "Déploie l'application en staging" |
| Déployer manuellement en production | "Déploie l'application en production" |
| Voir les pods en staging | "Montre l'état des pods en staging" |
| Voir les pods en production | "Montre l'état des pods en production" |
| Voir les logs du backend staging | "Montre les logs du backend en staging" |
| Redémarrer le backend en staging | "Redémarre le backend en staging" |
| Redémarrer le backend en production | "Redémarre le backend en production" |

---

### Déployer en staging — via CI/CD (recommandé)

Déclenche le workflow GitHub Actions backend ou frontend sur master :

```bash
# Déployer le backend en staging
gh workflow run backend.yml --ref master

# Déployer le frontend en staging
gh workflow run frontend.yml --ref master

# Suivre le déploiement en cours
gh run list --workflow=backend.yml --limit=1
gh run watch $(gh run list --workflow=backend.yml --limit=1 --json databaseId -q '.[0].databaseId')
```

---

### Déployer en production — via CI/CD (workflow dédié)

Le déploiement production est `workflow_dispatch` uniquement — jamais automatique.
Il requiert les tags exacts des images à déployer et la confirmation manuelle `PRODUCTION`.

```bash
# 1. Récupérer les SHAs actuellement en staging
kubectl get deployment legalcase-backend -n staging \
  -o jsonpath='{.spec.template.spec.containers[0].image}' | cut -d: -f2
kubectl get deployment legalcase-frontend -n staging \
  -o jsonpath='{.spec.template.spec.containers[0].image}' | cut -d: -f2

# 2. Lancer le déploiement production avec les tags récupérés
gh workflow run deploy-production.yml \
  --ref master \
  --field backend_tag=<SHA_BACKEND> \
  --field frontend_tag=<SHA_FRONTEND> \
  --field confirm=PRODUCTION

# 3. Suivre le déploiement
gh run list --workflow=deploy-production.yml --limit=1
gh run watch $(gh run list --workflow=deploy-production.yml --limit=1 --json databaseId -q '.[0].databaseId')
```

> ⚠️ Ne jamais déployer en production sans avoir validé en staging d'abord.

---

### Déployer manuellement en staging (sans CI/CD)

Pour un correctif urgent sans passer par le pipeline :

```bash
# Mettre à jour kubeconfig
aws eks update-kubeconfig --region eu-west-3 --name legalcase-shared

# Appliquer les manifests kustomize
kubectl apply -k k8s/overlays/staging/

# Vérifier le rollout
kubectl rollout status deployment/legalcase-backend -n staging --timeout=120s
kubectl rollout status deployment/legalcase-frontend -n staging --timeout=60s
```

---

### Surveiller les pods

```bash
# État de tous les pods staging
kubectl get pods -n staging

# État de tous les pods production
kubectl get pods -n production

# Logs backend staging (30 dernières lignes)
kubectl logs -n staging deployment/legalcase-backend --tail=30

# Logs backend production
kubectl logs -n production deployment/legalcase-backend --tail=30

# Logs en temps réel
kubectl logs -n staging deployment/legalcase-backend -f
```

---

### Redémarrer un service sur le cluster

```bash
# Redémarrer le backend en staging
kubectl rollout restart deployment/legalcase-backend -n staging

# Redémarrer le frontend en staging
kubectl rollout restart deployment/legalcase-frontend -n staging

# Redémarrer le backend en production
kubectl rollout restart deployment/legalcase-backend -n production

# Redémarrer tous les services en staging
kubectl rollout restart deployment/legalcase-backend deployment/legalcase-frontend deployment/rabbitmq -n staging
```

---

### Vérifier la santé de l'application

```bash
# Health check staging
curl -s https://staging.legalcase.ng-itconsulting.com/api/actuator/health

# Health check production (quand déployé)
curl -s https://legalcase.ng-itconsulting.com/api/actuator/health

# Certificats TLS
kubectl get certificate -n staging
kubectl get certificate -n production
```

---

## Tâches marketing — règles de gouvernance

### Règle 1 — Complétion

Toute tâche du `docs/MARKETING_BACKLOG.md` suit cette règle :

**Une tâche marketing n'est marquée `Terminé` que si elle est entièrement opérationnelle en production.**

- Un email rédigé mais non branché dans le code → statut `Rédigé`, pas `Terminé`
- Une page web rédigée mais non déployée → statut `Rédigé`, pas `Terminé`
- Un document produit mais non publié/transmis → statut `Rédigé`, pas `Terminé`

Quand une tâche marketing implique du code (email automatique, tracking, intégration), elle doit passer par la séquence de dev standard (mini-spec → dev → review → push) avant d'être marquée `Terminé`.

**REFUS si** : une tâche marketing est marquée `Terminé` sans que le code correspondant soit implémenté et déployé.

### Règle 2 — Contrôle de cohérence avant tout ajout au backlog marketing

Avant d'ajouter une nouvelle tâche dans `docs/MARKETING_BACKLOG.md`, exécuter et **afficher dans la conversation** le contrôle suivant en 4 points :

1. **Cohérence budgétaire** — la tâche entre-t-elle dans l'enveloppe marketing en vigueur (cadrage actif `docs/marketing/m71-budget-cadrage-2026h2.md` ou successeur) ? Si l'enveloppe doit être dépassée, l'ajout est conditionné à une décision d'arbitrage budgétaire explicite, et la tâche prérequise correspondante (cadrage / révision d'enveloppe) doit être créée d'abord.
2. **Doublon avec une feature produit** — la capacité technique demandée n'est-elle pas déjà couverte par une feature livrée du `PRODUCT_SPEC.md` ? Exemples : ne pas demander Google Analytics si F-77 est `Terminée`, ne pas demander un tracking conversion Google Ads si F-119 est `Terminée`.
3. **Doublon backlog (overlap > 30 %)** — scanner les sections M-XX existantes par thème (Site, Vidéo, Email, LinkedIn, Vente, Belgique, Stratégie acquisition, Mesure, Cadrage stratégique) pour vérifier qu'aucune tâche existante ne couvre déjà l'intention. Si oui, étendre la tâche existante plutôt qu'en créer une nouvelle (cf. règle "feedback_backlog_overlap_analysis").
4. **Séquence stratégique** — la tâche met-elle la charrue avant les bœufs (par ex. engager un stand événementiel à 5 k € avant d'avoir tranché l'enveloppe globale et l'arbitrage entre canaux SEO/SEA/SDR/événements) ? Si oui, créer d'abord la tâche prérequise (cadrage budget, validation traction, etc.).

Le contrôle doit produire un tableau visible dans la réponse (tâche proposée → verdict 4 points → action). Les tâches qui sortent du contrôle peuvent être marquées `Bloqué` si elles dépendent d'un arbitrage non encore tranché.

**REFUS si** : une nouvelle tâche est ajoutée à `MARKETING_BACKLOG.md` sans que ce contrôle ait été affiché dans la conversation.

**REFUS si** : une tâche événementielle ou un canal d'acquisition payant > 1 000 € est ajoutée au backlog alors que le cadrage budget marketing en vigueur (M-71 ou successeur) n'a pas tranché l'enveloppe correspondante.

---

## Priorité

```
Cohérence architecture > nouveauté
Process > vitesse
Testabilité > complétude
Refuser explicitement > laisser passer silencieusement
```
