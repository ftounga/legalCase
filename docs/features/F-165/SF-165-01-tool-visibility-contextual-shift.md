# Mini-spec — SF-165-01 Conversion ALWAYS_ON → CONTEXTUAL pour 14 outils décisionnels Travail FR (vague 1+2)

## Identifiant
`F-165 / SF-165-01`

## Feature parente
`F-165` — Réduction des outils ALWAYS_ON Travail FR via détection IA contextuelle

## Statut
`ready`

## Date de création
2026-04-27

## Branche Git
`feat/F-165-tool-visibility-contextual-shift`

---

## Objectif

Réduire le nombre d'outils décisionnels affichés ALWAYS_ON sur un dossier Travail FR (de 27 → 11) en convertissant 14 outils en CONTEXTUAL déclenchés par les flags IA déjà extraits du JSON `analysis_result`.

---

## Contexte

Bug UX staging 2026-04-27 sur dossier E-37 (vide) : panel F-IA-04 affiche 27 cards "blanches" car tous les nouveaux outils des 61 features récentes ont été seedés en ALWAYS_ON, alors que la philosophie F-IA-04 (cf. `project_f_ia_04_prerequisite.md`) recommande **2-4 ALWAYS_ON par domaine + le reste CONTEXTUAL**.

Cette SF résout les outils dont le déclencheur logique correspond à un champ déjà produit par l'IA (sans modification des prompts Sonnet). Les 8 outils restants nécessitent des nouveaux flags IA spécifiques → backlog **F-166** (Niveau 3).

---

## Mapping ALWAYS_ON → CONTEXTUAL

### Trigger `type_rupture` (déjà supporté backend)

| Outil | Trigger value(s) |
|---|---|
| F-DT-13 licenciement-economique | `LICENCIEMENT_ECONOMIQUE` |
| F-DT-14 pse-validite | `LICENCIEMENT_ECONOMIQUE` |
| F-DT-16 licenciement-nul-detection | `LICENCIEMENT`, `LICENCIEMENT_ECONOMIQUE` |
| F-DT-25 indemnite-preavis | `LICENCIEMENT`, `LICENCIEMENT_ECONOMIQUE`, `RUPTURE_CONVENTIONNELLE` |
| F-DT-26 conges-payes-indemnite | `LICENCIEMENT`, `LICENCIEMENT_ECONOMIQUE`, `RUPTURE_CONVENTIONNELLE` |
| F-DT-32 documents-fin-contrat | `LICENCIEMENT`, `LICENCIEMENT_ECONOMIQUE`, `RUPTURE_CONVENTIONNELLE` |

### Trigger `type_contrat` (nouveau, lu depuis `travail_extracted_data.type_contrat`)

| Outil | Trigger value |
|---|---|
| F-DT-17 indemnite-precarite-cdd | `CDD` |
| F-DT-22 requalification-cdd-cdi | `CDD` |
| F-DT-18 fin-mission-interim | `INTERIM` |
| F-DT-23 requalification-interim-cdi | `INTERIM` |

### Trigger `origine_inaptitude_pressentie` (nouveau, lu depuis `travail_extracted_data.origine_inaptitude_pressentie`)

| Outil | Trigger value(s) |
|---|---|
| F-DT-15 inaptitude | `ACCIDENT_TRAVAIL`, `MALADIE_PROFESSIONNELLE`, `MALADIE_ORDINAIRE` |

### Trigger `motif_nullite_pressenti` (nouveau, lu depuis `travail_extracted_data.motif_nullite_pressenti`)

| Outil | Trigger value(s) |
|---|---|
| F-DT-11 harcelement-licenciement-nul | `HARCELEMENT_MORAL`, `HARCELEMENT_SEXUEL` |
| F-DT-12 discrimination-dommages-interets | `DISCRIMINATION` |

### Trigger `heures_sup_mentionnees` (nouveau, lecture custom : `PRESENT` si node non-null)

| Outil | Trigger value |
|---|---|
| F-DT-19 heures-sup | `PRESENT` |

### Outils restant ALWAYS_ON (3 essentiels + 8 backlog F-166)

**Essentiels universels** : F-DT-03 prescription-litige, F-DT-04 fiche-prudhomale, F-DT-07 anciennete-conges-prime.

**Backlog F-166 (Niveau 3 — nouveaux flags IA à ajouter aux prompts)** : F-DT-20 rappel-salaire, F-DT-21 travail-dissimule, F-DT-24 non-concurrence, F-DT-30 protection-rp, F-DT-31 transaction, F-DT-33 at-mp, F-DT-34 refere-prudhomal, F-DT-35 contestation-are-fr.

---

## Comportement attendu

### Cas nominal — dossier vide (sans analyse)

1. Avocat ouvre un dossier travail FR neuf (jamais analysé).
2. Backend `DecisionToolVisibilityService.resolveVisibleTools` retourne :
   - `alwaysOn` : 11 outils (3 essentiels + 8 du backlog F-166)
   - `contextual` : `[]`
   - `catalog` : tous les autres CONTEXTUAL
3. Panel affiche 11 cards (vs 27 actuellement).

### Cas nominal — dossier avec analyse `LICENCIEMENT_ECONOMIQUE`

1. IA détecte `compensation_data.type_rupture = LICENCIEMENT_ECONOMIQUE`.
2. `extractDetectedSituations` produit `{type_rupture: {LICENCIEMENT_ECONOMIQUE}}`.
3. Le résolveur active F-DT-13, F-DT-14, F-DT-16, F-DT-25, F-DT-26, F-DT-32, F-DT-08, F-DT-09 → 8 outils contextuels affichés en plus.

### Cas nominal — dossier CDD avec inaptitude

1. IA détecte `travail_extracted_data.type_contrat = CDD` + `origine_inaptitude_pressentie = MALADIE_PROFESSIONNELLE`.
2. Resolver active F-DT-17, F-DT-22 (CDD) + F-DT-15 (inaptitude).

### Cas d'erreur

| Situation | Comportement |
|---|---|
| `analysis_result` JSON invalide | `extractDetectedSituations` retourne map vide → seulement les ALWAYS_ON |
| Champ `travail_extracted_data` absent | OK, lecture défensive `path()` → null |
| Valeur enum non reconnue (ex: `type_contrat = STAGE`) | Filtré par les rules CONTEXTUAL (pas de match) → outil pas affiché. Pas d'erreur. |

---

## Tables / endpoints / composants impactés

### Backend
- **Service modifié** : `DecisionToolVisibilityService.extractDetectedSituations()` — ajout lecture de 5 nouveaux trigger_field.
- **Table modifiée (data only)** : `decision_tool_visibility_rules` — DELETE 14 ALWAYS_ON + INSERT 23 CONTEXTUAL (multiple lignes par outil pour les triggers multi-valeurs).
- **Migration nouvelle** : `193-shift-tools-to-contextual-travail-fr.xml`.
- **Test étendu** : `DecisionToolVisibilityServiceTest` — vérifier extraction des nouveaux triggers.
- **Test nouveau** : `DecisionToolVisibilityIntegrityIT` (existant) reste vert.

### Frontend
- **Aucune modification frontend** — TOOL_REGISTRY inchangé. Le panel résout simplement les bonnes entrées.

### Documentation
- F-165 ajoutée à `PRODUCT_SPEC.md` (section *Pipeline IA & qualité* — c'est une amélioration de F-IA-04).
- F-166 ajoutée au backlog (Niveau 3 — vague 2 enrichissement IA).

---

## Critères d'acceptation

- [ ] Migration `193-shift-tools-to-contextual-travail-fr.xml` créée + rollback réversible.
- [ ] `DecisionToolVisibilityService.extractDetectedSituations` lit `type_contrat`, `motif_licenciement` (réservé futur), `origine_inaptitude_pressentie`, `motif_nullite_pressenti`, `heures_sup_mentionnees` (logique custom).
- [ ] Test unitaire pour chaque nouveau trigger : extraction OK, mapping OK, fallback OK si absent.
- [ ] Test d'intégration : dossier vide → 11 ALWAYS_ON. Dossier avec licenciement éco → +8 contextuels.
- [ ] Sur dossier réel staging E-37 (après recharge crédits Anthropic) : panel n'affiche plus 27 cards mais 11 ALWAYS_ON, et après analyse, les outils contextuels apparaissent selon détection.

---

## Plan de test minimal

### Tests unitaires `DecisionToolVisibilityServiceTest`
- `extract_lit_type_contrat_depuis_travail_extracted_data` (CDD, INTERIM, CDI)
- `extract_lit_origine_inaptitude_pressentie`
- `extract_lit_motif_nullite_pressenti`
- `extract_heures_sup_mentionnees_PRESENT_si_objet_non_null`
- `extract_heures_sup_mentionnees_absent_si_node_null` (= pas de trigger)
- Tests existants restent verts.

### Test d'intégration
- `dossier_vide_affiche_11_alwayson_travail_fr`
- `dossier_licenciement_economique_active_outils_contextuels` (F-DT-13, 14, 16, 25, 26, 32 + F-DT-08, 09 existants)
- `dossier_cdd_active_F_DT_17_22`
- `dossier_inaptitude_active_F_DT_15`

---

## Analyse de cohérence transversale

### Périmètres scannés
- **Autres outils décisionnels** : applicable uniquement Travail FR. Travail BE / Immigration / Famille restent inchangés (pas de sur-affichage signalé sur ces domaines).
- **Autres pays** : non applicable (BE n'a que 5 ALWAYS_ON travail).
- **Autres domaines** : transversal au mécanisme mais data-only Travail FR. Les domaines Immigration / Famille fonctionnent déjà avec leurs triggers spécifiques (`type_titre_sejour_code`, `regime_matrimonial`, etc.).
- **Autres mécanismes** : `extractDetectedSituations` est étendu sans casser les triggers existants (lecture défensive).

### Cohérence IA / refresh / pré-fill / validation F-IA-03
- **Pré-fill IA** : non applicable (pas de nouveau composant frontend).
- **Validation F-IA-03** : non applicable.
- **F-IA-02 refresh** : non applicable (la liste visibility ne se rafraîchit que via `loadVisibility` au mount/refresh case file, comportement inchangé).

---

## Impact par domaine métier

**Cette SF cible uniquement le DROIT_DU_TRAVAIL FRANCE.** Aucune adaptation pour les autres domaines :
- Travail BE : 5 ALWAYS_ON, déjà raisonnable.
- Immigration : utilise déjà des triggers spécifiques (`type_titre_sejour_code`, `type_recours_code`).
- Famille : utilise déjà des triggers spécifiques (`type_procedure_detectee`, `regime_matrimonial`, `mode_garde_detaille`).

Le mécanisme `extractDetectedSituations` étendu peut être réutilisé pour les autres domaines plus tard, mais pas dans cette SF.

---

## Préoccupations transversales (CLAUDE.md)

- [x] **Auth / Principal** : non applicable.
- [x] **Workspace context** : non applicable.
- [x] **Plans / limites** : non applicable.
- [x] **Navigation / routing** : non applicable.
- [x] **Outil décisionnel métier** : applicable mais SF aligne le mapping existant — aucun nouvel outil créé. Les composants Angular restent inchangés.

---

## Hors périmètre

- Niveau 3 (F-DT-20/21/24/30/31/33/34/35) — backlog F-166. Nécessite enrichir prompts IA Sonnet avec nouveaux flags `rappel_salaire_detecte`, `travail_dissimule_detecte`, `clause_non_concurrence_detectee`, `statut_protege_detecte`, `transaction_envisagee`, `at_mp_detecte`, `urgence_procedurale`, `contestation_are_envisagee`.
- Refactoring `extractDetectedSituations` en lookup table déclaratif (peut faire l'objet d'une feature qualité plus tard).
- Modification du frontend (aucune nécessaire).

---

## Estimation

0.5 - 1 jour : 1 PR backend (extension service + migration + tests).
