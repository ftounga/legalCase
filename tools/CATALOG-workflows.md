# Catalogue des workflows — LegalCase

Les workflows « nommés » vivent dans `.claude/workflows/*.js` et s'**auto-enregistrent comme skills**
(invocables via `/<nom>` après redémarrage de la session). Leur doc = le bloc `meta` (description,
whenToUse, phases) en tête de chaque fichier.

## Voir / lister
- `ls .claude/workflows/` → les fichiers de workflows.
- `/` (taper slash) → les skills, dont les workflows nommés, avec leur description.
- `/workflows` → **moniteur d'exécutions** (progression en direct d'un run en cours/récent ; vide si rien ne tourne).
- Doc complète d'un workflow → ouvrir son `.js` et lire le bloc `meta` + commentaires.

## Lancer
- Par nom : `Workflow({ name: "<nom>", args: {...} })`
- Par chemin : `Workflow({ scriptPath: ".claude/workflows/<nom>.js", args: {...} })`
- Via skill : `/<nom>` (si exposé en skill).
- ⚠️ **Gotcha connu** (cf. mémoire `feedback_workflow_named_args_binding`) : les `args` d'un workflow
  lancé **par `name`** ne se lient pas toujours (→ valeurs par défaut). Pour des args non-défaut fiables,
  préférer `scriptPath`, ou éditer la constante par défaut dans le script.

## Suivre le résultat
- Pendant : `/workflows` (arbre de progression live).
- À la fin : le workflow **retourne un objet** (résumé) + produit ses **artefacts** (CSV, fichiers, PRs selon le workflow)
  + je te fais une **synthèse**. Une `<task-notification>` arrive quand le run se termine.

---

## 1. `avocat-wave`  (acquisition)
- **Rôle** : vague de prospection avocat de bout en bout — sourcing Apollo (travail/famille/immigration) + filtre domaine-fit → accroches personnalisées (fan-out) → **push direct dans la campagne Lemlist**.
- **Fichier** : `.claude/workflows/avocat-wave.js` · **Support** : `tools/prospection-apollo/{avocat_pipeline,split_batches,merge_intros,lemlist_push}.py`
- **Args** : `perDomain` (nb de cabinets/domaine, défaut **100**) · `country` (`FR` défaut ou `BE` = Belgique francophone → localisation Apollo Belgium + messaging droit belge + campagne Lemlist BE).
- **Pré-requis** : `.apollo_key` + `.lemlist_key` dans `tools/prospection-apollo/`. **BE** : créer d'abord la campagne Lemlist avocat BE et remplacer `cam_REMPLACER_AVOCAT_BE` dans `avocat-wave.js`. Run BE **gaté** post-signal 24/06.
- **Lancer** :
  ```
  Workflow({ name: "avocat-wave", args: { perDomain: 100 } })
  // Belgique francophone :
  Workflow({ name: "avocat-wave", args: { perDomain: 50, country: "BE" } })
  // essai à petit volume :
  Workflow({ scriptPath: ".claude/workflows/avocat-wave.js", args: { perDomain: 5 } })
  ```
- **Coût** : crédits Apollo + tokens → réservé à une vraie vague (post-signal).

## 2. `drh-wave`  (acquisition)
- **Rôle** : vague de prospection DRH (employeur) de bout en bout — sourcing Apollo (5 secteurs : sécurité, propreté, transport, restauration, médico-social privé), exclut hôpitaux publics + déjà-contactés, enrich → accroches secteur-aware (fan-out) → **push direct dans la campagne Lemlist DRH** (`cam_sikMYuuPxpjoYysSa`).
- **Fichier** : `.claude/workflows/drh-wave.js` · **Support** : `tools/prospection-apollo/{apollo_drh_pipeline,drh_split_batches,drh_merge_intros,lemlist_push}.py`
- **Args** : `perSector` (nb d'entreprises/secteur, défaut **20**) · `country` (`FR` défaut ou `BE` → localisation Apollo Belgium + accroches droit social belge (tribunal du travail / CCT 109 / CCT 32bis) + campagne Lemlist DRH BE). Séquence BE dédiée : `sequence-drh-BE-lemlist.md`.
- **Pré-requis BE** : créer la campagne Lemlist DRH BE et remplacer `cam_REMPLACER_DRH_BE` dans `drh-wave.js`. Run BE **gaté** post-signal 24/06.
- **Lancer** :
  ```
  Workflow({ name: "drh-wave", args: { perSector: 20 } })
  // Belgique francophone :
  Workflow({ name: "drh-wave", args: { perSector: 15, country: "BE" } })
  ```
- **Coût** : crédits Apollo + tokens → réservé à une vraie vague (post-signal).

## 3. `autonomous-delivery-wave`  (dev produit)
- **Rôle** : livre une **vague de N features** (cible 10) du backlog `PRODUCT_SPEC.md`, équipe d'agents, gouvernance CLAUDE.md, auto-merge. **Ne crée pas** de feature.
- **Fichier** : `.claude/workflows/autonomous-delivery-wave.js` (+ skill `ai-skills/autonomous-delivery-wave.md`).
- **Phases** : Bootstrap → Audit+File → Classer → Livrer → Docs+Staging → Récap.
- **Lancer** : `Workflow({ name: "autonomous-delivery-wave" })` ou la skill. Cadrer le volume via la directive budget (« +500k »).

## 4. `drh-product-spec`  (cadrage)
- **Rôle** : fait **mûrir la fiche produit DRH** (offre employeur) comme document vivant, par appends justifiés.
- **Fichier** : `.claude/workflows/drh-product-spec.js`.
- **Lancer** : skill `/drh-product-spec` ou `Workflow({ name: "drh-product-spec" })`. Ré-invocable (converge).

## 5. `afrique-product-spec`  (cadrage)
- **Rôle** : fait **mûrir la fiche produit Afrique OHADA**, piloté par le marché africain.
- **Fichier** : `.claude/workflows/afrique-product-spec.js`.
- **Lancer** : skill `/afrique-product-spec` ou `Workflow({ name: "afrique-product-spec" })`. Suppose `docs/afrique/CADRAGE-STRATEGIQUE-OHADA.md` figé.
