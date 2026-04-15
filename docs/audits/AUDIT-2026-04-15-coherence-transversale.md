# Audit de cohérence transversale — 2026-04-15

## Objet

Audit systématique des 10 outils décisionnels de l'application LegalCase contre les 7 patterns transversaux établis lors des subfeatures F-IA-02/03 et F-118. Déclenché par l'observation que F-DT-10 avait été livrée sans F-IA-03 (oubli rétroactif), et par la décision d'étendre l'audit à toute l'application plutôt qu'au seul cas découvert.

## Périmètre

**10 outils décisionnels** :
F-DT-07 Ancienneté · F-DT-08 Licenciement · F-DT-09 Comparateur indemnités · F-DT-10 Rupture conventionnelle · F-FA-05 Partage immobilier · F-FA-06 Calendrier garde · F-FA-07 Checklist divorce · F-IM-05 Titre séjour · F-IM-06 Recours · F-IM-07 Droit au travail

**7 patterns transversaux** :
1. F-IA-03 Cohérence IA (4 inputs, computed `coherenceAlerts`, hiérarchie F96 > QUESTION_IA > IA > PIECE_MANQUANTE, MULTI)
2. F-IA-02 Refresh dashboard (SF-IA-02-03 — injection `CaseDashboardRefreshService` + `triggerRefresh()`)
3. Pré-remplissage IA (`prefillFromAi()` / `applyAiPrefill()`)
4. Persistance inputs (SF-DT-07-04 — reload-safe via colonnes DB ou JSON)
5. Masquage conditionnel (SF-DT-10-04 — `@if` selon type/country/domain)
6. Gate `coherenceAlerts` correct (SF-IA-03-12 — uniquement `!showForm()`)
7. Design System (palette, Inter, espacements, MatSnackBar)

## Matrice résumée

| Outil | F-IA-03 | F-IA-02 | Prefill IA | Persist. | Masquage | Gate | Design |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| F-DT-07 Ancienneté | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| F-DT-08 Licenciement | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ |
| F-DT-09 Comparateur | ✅ | ✅ | ✅ | ⚠️ | ✅ | ✅ | ✅ |
| F-DT-10 Rupture conv. | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| F-FA-05 Partage | ⚠️ | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ |
| F-FA-06 Calendrier | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| F-FA-07 Divorce | ⚠️ | ✅ | N/A | ✅ | ✅ | ✅ | ✅ |
| F-IM-05 Titre séjour | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| F-IM-06 Recours | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| F-IM-07 Droit travail | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

Légende : ✅ conforme · ⚠️ partiel · ❌ manquant · N/A non applicable

## Observations positives (transversales)

- ✅ Les 10 outils injectent `CaseDashboardRefreshService` (@Optional) et appellent `triggerRefresh()` après action métier validée.
- ✅ Design System respecté : palette complète (#1A3A5C, #C9973A, #27AE60, #F59E0B, #E67E22, #C0392B, #6B7A8D, #1C2B3A, #E0E4EA, #F4F6F8, #F7FAFC), Inter, espacements 4px-multiples, `MatSnackBar` pour erreurs.
- ✅ Masquage conditionnel cohérent dans `case-file-detail.component.html`.
- ✅ Persistance inputs OK pour 9/10 outils (via colonnes DB ou JSON result).
- ✅ Gate `coherenceAlerts` post-SF-IA-03-12 : 9/10 outils conformes.

## Écarts identifiés

### 🔴 Écart bloquant — SF-IA-03-14

**F-DT-08 Licenciement × Pattern 6 (Gate coherenceAlerts)**
- Observation : le `computed coherenceAlerts` n'inclut PAS `if (!this.showForm()) return {};` au début. Il calcule à tout instant.
- Impact : calculs inutiles quand le bloc résultat est affiché. Potentiels badges fantôme si la vue change.
- Priorité : moyenne (peu visible en pratique car F-DT-08 a un formulaire persistant).
- Fix : 1 ligne + 1 test de non-régression.

### 🟠 Écarts d'enrichissement — SF-IA-03-14

**F-IM-05, F-IM-06, F-IM-07, F-FA-06 × Pattern 1 (F-IA-03 partiel)**
- Observation : ces 4 outils n'ont pas d'input `@Input() piecesManquantes`. Source PIECE_MANQUANTE absente de leur hiérarchie de cohérence.
- Impact : les pièces manquantes taggées par l'IA (`critere_code = IM05_MOTIF`, `FA06_MODE_GARDE`, etc.) ne déclenchent pas d'alerte côté frontend.
- Priorité : moyenne — faible fréquence d'utilisation (pièces rarement taggées), mais aligne la conception avec F-DT-08.
- Fix : 4 inputs ajoutés, `buildPiecesIndex` helper + intégration dans `coherenceAlerts` computed + tests.

**F-IM-05, F-IM-06, F-IM-07, F-FA-06 × Pattern 1 (MULTI non-détecté explicitement)**
- Observation : convergence F96 + Question IA + IA n'est pas catégorisée comme `MULTI` dans ces outils.
- Impact : tooltip moins précis, pas d'agrégation des justifications.
- Priorité : basse — design acceptable pour outils à 1-2 champs.
- Fix : même SF, aligner pattern F-DT-08 `multiOrSingle` + `collectSupportingSources`.

### 🟠 Écart majeur — SF-FA-05-05

**F-FA-05 Partage immobilier × Patterns 1 et 3 (F-IA-03 et prefill absents)**
- Observation : F-FA-05 n'a pas les inputs F-IA-03 standards (`aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`). Les alertes de cohérence sont basées uniquement sur un "best-match" avec les biens importés depuis `liquidationCommunaute`. Pas de `prefillFromAi()` — seulement un bouton Import manuel.
- Impact : aucune détection F96 / Question IA / Pièce manquante pour les deux champs numériques (`valeurVenale`, `capitalRestantDu`). Incohérence de richesse avec les 9 autres outils.
- Priorité : haute — design simplifié qui diverge du standard.
- Fix : extension backend prompts avec codes `FA05_VALEUR_VENALE` / `FA05_CAPITAL_RESTANT` (numériques avec `expected_value`), 4 inputs, computed `coherenceAlerts` complet, pré-remplissage automatique depuis la synthèse IA (sans clic Import), provenance notes.

### 🟡 Écart documentaire — Acceptation

**F-DT-09 × Pattern 4 (Persistance legacy)**
- Observation : résultats créés avant SF-DT-09-04 (avant 2026-04-14) n'ont pas `typeRupture`. Fallback `resp.country === 'BELGIQUE' ? 'LICENCIEMENT_ORDINAIRE' : 'LICENCIEMENT'` appliqué dans `prefillForm`.
- Impact : cas rare (quelques dossiers) — fallback raisonnable, ne masque pas de saisie utilisateur antérieure puisque la colonne n'existait pas.
- Priorité : basse.
- Décision : **aucun fix** — fallback documenté comme acceptable en `Notes et décisions` de SF-DT-09-04. Documenter ici.

**F-FA-07 Checklist divorce × Pattern 1 (design intentionnel)**
- Observation : pas d'input `aiData` (détection IA autonome). Alertes basées sur procedureChecks + aiQuestions + piecesManquantes.
- Impact : design volontaire — la checklist divorce est 100% procédurale, pas de détection IA structurée.
- Priorité : basse.
- Décision : **aucun fix** — documenté ici comme intentionnel.

## Synthèse

- **10 outils audités** · **70 points de vérification**
- **58 points conformes** (83%)
- **1 écart bloquant mineur** (F-DT-08 gate)
- **4 outils avec F-IA-03 enrichissable** (piecesManquantes + MULTI)
- **1 outil avec F-IA-03 divergent** (F-FA-05 enrichissement complet)
- **2 cas documentés comme acceptables** (F-DT-09 legacy, F-FA-07 design)

## Plan d'action

| Action | SF | Priorité | Effort |
|---|---|:-:|:-:|
| Fix gate F-DT-08 + enrichissement F-IM-05/06/07/F-FA-06 | SF-IA-03-14 | Haute | M |
| Enrichissement complet F-FA-05 | SF-FA-05-05 | Haute | L |
| Documentation écarts acceptés | cet artefact | Faite | XS |

Après exécution, l'application sera à **100% de cohérence transversale** sur les 10 outils décisionnels (modulo les 2 décisions explicites d'acceptation).

## Gouvernance associée

Cet audit illustre l'utilité des règles ajoutées aujourd'hui :
- **Scan transversal** dans la mini-spec (commit `8991159`)
- **5 niveaux de vérification** (commit `db40b4b`)
- **6 questions nouvelle feature outil décisionnel** (commit `c872f5a`)

Pour toute future feature d'outil décisionnel, ces trois mécanismes devraient empêcher les trous rétroactifs comme celui observé sur F-DT-10.

**Cadence recommandée** : audit similaire tous les ~20 subfeatures d'outils décisionnels ou tous les 3 mois, en utilisant cette matrice comme base.
