# F-213 — Cadrage cohérence (étape 0)

## Verdict : GO

## Intention métier (1 phrase)

Couvrir les ~10 outils décisionnels Travail BE de **priorité P2 — fréquence haute** — pour que l'avocat belge dispose des calculateurs / analyseurs / checklists rencontrés plusieurs fois par mois dans toute pratique travailliste (clause non-concurrence, rappel de salaire, préavis statut unique, formule Claeys, protection grossesse, transaction fin contrat, harcèlement procédure formelle, protection délégué syndical, acte équipollent à rupture, CCT 109 licenciement déraisonnable), tous absents du produit à ce jour et distincts des 8 outils P1 livrés par F-207.

## Source juridique

`docs/features/F-191/audit-be-travail-exhaustif.md` — sections 3.1, 3.3, 3.5, 3.7, 4.3. Sources BE primaires : Loi 03/07/1978 relative aux contrats de travail (art. 15, 20, 35, 37, 40, 65), CCT 13 (clause non-concurrence), CCT 109 (licenciement manifestement déraisonnable), Loi 16/03/1971 art. 40 (protection grossesse), Loi 04/08/1996 art. 32bis (harcèlement), Loi 19/03/1991 (protection délégué syndical), Loi 12/04/1965 (salaire).

---

## Inclusion / exclusion vs audit BE — décision de cadrage

### Outils retenus dans F-213 (P2 — fréquence haute)

| # | tool_id | Audit §audit | Priorité audit | Justification inclusion |
|---|---|---|---|---|
| 1 | `clause-non-concurrence-be` | §3.7 | **P2 BE-only** | Très demandé ; régime indemnitaire BE distinct (½ rémun durée clause) ; jumeau annoncé F-DT-24 ; PRODUCT_SPEC.md cité en premier |
| 2 | `rappel-salaire-be` | §3.3 | **P2** | Très fréquent ; prescription 1 an post-rupture spécifique ; jumeau annoncé F-DT-20 ; PRODUCT_SPEC.md cité |
| 3 | `licenciement-be-statut-unique-preavis` | §3.1 | **P2** | Calcul préavis post-2014 (tranches semaines) vu à chaque dossier licenciement ; PRODUCT_SPEC.md cité |
| 4 | `licenciement-be-formule-claeys` | §3.1 | **P3 BE-only** (reclassé P2 factuel) | Contrats < 2014 encore très nombreux en litige 2026 ; « double préavis » très contesté ; PRODUCT_SPEC.md cité ; audit §4.4 recommande de l'éclater de F-DT-09 |
| 5 | `licenciement-be-protection-grossesse` | §3.1 | **P1 P2** | Indemnité 6 mois forfaitaire + dommages prouvés ; PRODUCT_SPEC.md cité ; très demandé en consultation |
| 6 | `transaction-be-travail` | §3.13 | **P2 BE-only** | Fin de contrat amiable ; jumeau annoncé F-DT-31 ; renonciation expresse différente du FR ; PRODUCT_SPEC.md cité |
| 7 | `harcelement-be-procedure-formelle` | §3.5 | **P1 P2 BE-only** | Procédure interne CPAP / CISP non couverte par F-DT-11 (F-DT-11 = licenciement nul représailles ; outil F-213 = procédure formelle plainte) ; PRODUCT_SPEC.md cité |
| 8 | `licenciement-be-protection-deleguee` | §3.1 | **P2 BE-only** | Indemnité 2-4 ans rémunération ; mécanisme très différent du statut protégé FR ; PRODUCT_SPEC.md cité |
| 9 | `licenciement-be-acte-equivalent` | §3.1 | **P2** | Acte équipollent à rupture — modification unilatérale conditions essentielles ; très spécifique BE ; fréquent |
| 10 | `licenciement-be-cct109-deraisonnable` | §3.1 | **P2** | Scoring 3/8/12/17 semaines CCT 109 ; vue dédiée qui complète F-DT-08/09 sans doublon (F-DT-08 = validité ; F-DT-09 = comparateur ; F-213-10 = score motivé autonome) |

**Total : 10 outils = 20 SF (1 backend + 1 frontend par outil).**

### Outils exclus de F-213 — justifications

| tool_id | Exclusion | Justification |
|---|---|---|
| Tous les P1 (prescription, C4, contestation ONEM, AT Fedris, référé, RCC, outplacement) | **Déjà livré F-207** | 8 outils P1 intégralement couverts |
| `F-DT-08-licenciement-validity`, `F-DT-09-comparateur-indemnites` | **Déjà livré F-204 / F-DT-XX** | Existants en DB + frontend câblés (Tableau A audit) |
| `pecule-vacances-be` | **Repoussé P2/P3** | Pas cité dans PRODUCT_SPEC.md F-213 ; complexité de calcul élevée (employés vs ouvriers, pécule départ) ; réservé vague suivante |
| `prime-fin-annee-be` | **Repoussé P2** | Pas cité PRODUCT_SPEC.md F-213 ; réservé vague suivante |
| `demission-be-validite` | **Repoussé P2** | Non cité PRODUCT_SPEC.md F-213 ; réservé vague suivante |
| `documents-fin-contrat-be` | **Repoussé P2** | Non cité PRODUCT_SPEC.md F-213 ; réservé vague suivante |
| `licenciement-be-rupture-irreguliere` | **Repoussé P2** | Non cité PRODUCT_SPEC.md F-213 ; proche de F-DT-09 existant |
| Tous les P3/P4 | **Réservés F-219** | Vague P3 prévue après F-213 (PRODUCT_SPEC.md F-219 : 32 outils BE-only) |

---

## Workflow métier réel de l'avocat Travail BE (P2 — fréquence haute)

Source : audit BE exhaustif §3 + pratique standard avocat travailliste belge.

**Scénario A — Licenciement contrat post-2014 (statut unique)**
1. Client arrivant avec notification de licenciement + contrat signé après 01/01/2014.
2. L'avocat calcule d'abord le **préavis statut unique** (durée en semaines selon ancienneté par tranches) — outil 3.
3. Si litige sur motivation du licenciement : analyse du **score CCT 109** (3 / 8 / 12 / 17 semaines selon gravité) — outil 10.
4. Si contexte de grossesse ou maternité : analyse de la **protection grossesse** (indemnité 6 mois + dommages prouvés) — outil 5.
5. Si délégué syndical ou candidat aux élections sociales : analyse de la **protection délégué** (indemnité 2-4 ans) — outil 8.
6. Si modification unilatérale des conditions essentielles (lieu, fonction, salaire) plutôt que notification formelle : analyse de l'**acte équipollent à rupture** — outil 9.
7. Suite : pipeline standard (synthèse, conclusions F-98).

**Scénario B — Licenciement contrat pré-2014 (formule Claeys)**
1. Client avec contrat signé avant 01/01/2014 et ancienneté significative.
2. L'avocat calcule le **préavis formule Claeys** (art. 82 ancien régime + clause de sauvegarde loi 26/12/2013 art. 67) — outil 4.
3. Comparaison avec la partie statut unique post-2014 pour les années d'ancienneté après 2014.
4. Si motif grave contesté : cf. F-DT-27 existant (hors périmètre F-213).

**Scénario C — Rappel de salaire / heures impayées**
1. Salarié dénonce des arriérés de salaire (heures sup non payées, avantages conventionnels non versés).
2. L'avocat calcule le **rappel de salaire BE** (prescription 1 an post-rupture / 5 ans pendant contrat + intérêts moratoires 10 %) — outil 2.
3. Si les impayés incluent des heures supplémentaires : croisement avec F-DT-19 existant (hors périmètre).

**Scénario D — Clause non-concurrence**
1. Employeur invoque une clause de non-concurrence après départ.
2. L'avocat vérifie la **validité BE** (durée ≤ 1 an, zone géographique, seuil salaire > 73 571 €/2024, indemnité = ½ rémun durée clause, absence de liste sectorielles d'exception CCT 13) — outil 1.
3. Si clause invalide : dommages-intérêts + main-levée.

**Scénario E — Harcèlement (procédure interne)**
1. Client victime de harcèlement moral ou sexuel souhaitant activer la procédure interne BE.
2. L'avocat guide la **procédure formelle** (saisine CPAP / personne de confiance / CISP, délais, protection contre représailles) — outil 7.
3. Si licenciement représailles survient ensuite : outil F-DT-11 existant (licenciement nul — hors périmètre F-213).

**Scénario F — Transaction fin contrat**
1. Employeur propose une transaction (fin de contrat amiable avec renonciation à tout recours).
2. L'avocat vérifie la **validité de la transaction BE** (renonciations expresses, concessions réciproques, objet du litige né ou à naître) — outil 6.

---

## Cartographie features actuelles ↔ workflow

| Étape métier | Outil LegalCase | Statut |
|---|---|---|
| Upload / extraction / analyse IA | F-43, F-121, F-122 | ✅ Livrée |
| Préavis statut unique post-2014 | `licenciement-be-statut-unique-preavis` | ❌ **MANQUE — SF-213-03** |
| Formule Claeys pré-2014 | `licenciement-be-formule-claeys` | ❌ **MANQUE — SF-213-04** |
| CCT 109 score déraisonnable | `licenciement-be-cct109-deraisonnable` | ❌ **MANQUE — SF-213-10** |
| Protection grossesse | `licenciement-be-protection-grossesse` | ❌ **MANQUE — SF-213-05** |
| Protection délégué syndical | `licenciement-be-protection-deleguee` | ❌ **MANQUE — SF-213-08** |
| Acte équipollent à rupture | `licenciement-be-acte-equivalent` | ❌ **MANQUE — SF-213-09** |
| Rappel salaire + intérêts | `rappel-salaire-be` | ❌ **MANQUE — SF-213-02** |
| Clause non-concurrence BE | `clause-non-concurrence-be` | ❌ **MANQUE — SF-213-01** |
| Harcèlement procédure formelle | `harcelement-be-procedure-formelle` | ❌ **MANQUE — SF-213-07** |
| Transaction fin contrat BE | `transaction-be-travail` | ❌ **MANQUE — SF-213-06** |
| Licenciement pour motif grave (délais 3+3 j) | F-DT-27 existant | ✅ Couvert |
| Indemnités comparateur / validité | F-DT-08 / F-DT-09 existants | ✅ Couvert |
| Pipeline IA + synthèse + conclusions | F-3/4/5, F-98 | ✅ Livrée |

---

## Briques d'infrastructure amont — toutes livrées (héritées de F-207)

- **Panneau outils décisionnels** (`app-decisional-tools-panel`) — accueille nouveaux outils via `TOOL_REGISTRY`. ✅
- **Gate `workspaceCountry`** — pattern BE-only éprouvé (F-198, F-204, F-207). ✅
- **Pré-remplissage IA** (`TravailExtractedData` BE + `LegalDomainPromptBuilder` branche `country=BELGIUM`). Enrichi par F-207. ✅
- **Validation F-IA-03** + émission `critereCode` (`BE_*`). Garde-fou `CritereCodeIntegrityIT` en place. ✅
- **Migrations Liquibase pattern** (`*-analyses` table + `decision_tool_visibility_rules`). Pattern canonique. ✅
- **`dateRuptureContrat` + `motifRupture`** dans `TravailExtractedData` — ajoutés par F-207 SF-207-01, réutilisables par F-213 sans nouveau champ dans la majorité des outils. ✅

---

## Challenge amont

- Upload / extraction / OCR : ✅ F-43, F-121, F-122 (SF-122-13 multi-pages inclus).
- Pipeline IA + détection contextuelle BE : ✅ Étendu à chaque outil via extension prompt BE.
- `dateRuptureContrat`, `ancienneteAnnees`, `motifRupture` : partiellement disponibles depuis F-207 SF-207-01. Les champs spécifiques P2 (ex. `clauseNonConcurrencePresente`, `salaireBrut`, `positionProtegee`, `dateDebut`, `dateContratsInitiaux`) seront ajoutés par les SF backend de F-213.

**Aucun trou bloquant amont.**

---

## Challenge aval

Sortie de chaque outil :
- Verdict décisionnel (validité / montant / durée) → **dashboard décisionnel** (F-IA-02) via `triggerRefresh()`.
- Résultat enrichit le **projet de conclusions** (F-98).

**Aucun trou aval.**

---

## STOPs / pré-requis

Aucun bloquant technique. F-207 est terminée et mergée — toute l'infrastructure BE Travail est en place. F-213 peut démarrer immédiatement après validation des mini-specs.

---

## Invariants anti-gadget pour les mini-specs

1. **Partir des sources BE — pas de calque FR** (`feedback_belgique_never_forget`). Chaque outil cite sa source primaire belge (Loi 03/07/1978, CCT 13, CCT 109, Loi 16/03/1971, Loi 04/08/1996, Loi 19/03/1991, Loi 12/04/1965).
2. **Workspace gate BE-only strict** — `workspaceCountry=BELGIQUE` côté controller + `country === 'BELGIQUE'` côté frontend. Test isolation `country=FRANCE` → outil masqué — obligatoire par SF frontend.
3. **Critères F-IA-03 BE distincts** — `BE_*` préfixés (exemples : `BE_ANCIENNETE_ANNEES`, `BE_SALAIRE_BRUT`, `BE_DATE_RUPTURE`). `CritereCodeIntegrityIT` reste vert.
4. **Un outil = une situation métier** (`feedback_decision_tools_one_per_situation`). `licenciement-be-statut-unique-preavis` (durée préavis post-2014) ≠ `licenciement-be-formule-claeys` (préavis pré-2014) ≠ `licenciement-be-cct109-deraisonnable` (score motivation) — trois outils distincts malgré la même notification de licenciement.
5. **Pré-remplissage IA obligatoire** sur tous les champs saisissables (`feedback_decision_tools_all_fields_prefilled` ; F-246 invariant).
6. **Pattern canonique F-IA-04** — chaque section frontend respecte le pattern `immigration-title-decision-section`.
7. **Annotation BELGIQUE UNIQUEMENT** sur chaque champ de `TravailExtractedData` ajouté pour F-213, avec garde `country === 'BE'` côté frontend.

---

## Découpage en 20 SF (parallélisation back/front par outil)

Pattern : **1 SF backend + 1 SF frontend** parallélisables (contrat API figé dans la mini-spec backend). 10 outils × 2 = 20 SF.

| # | Outil | SF backend | SF frontend | Source juridique BE principale |
|---|---|---|---|---|
| 1 | `clause-non-concurrence-be` | SF-213-01-backend | SF-213-01b-frontend | Loi 03/07/1978 art. 65 ; CCT 13 du 24/02/1971 modif. |
| 2 | `rappel-salaire-be` | SF-213-02-backend | SF-213-02b-frontend | Loi 12/04/1965 art. 10 ; Loi 03/07/1978 art. 15 |
| 3 | `licenciement-be-statut-unique-preavis` | SF-213-03-backend | SF-213-03b-frontend | Loi 26/12/2013 ; barème semaines statut unique |
| 4 | `licenciement-be-formule-claeys` | SF-213-04-backend | SF-213-04b-frontend | Loi 03/07/1978 art. 82 ancien ; loi 26/12/2013 art. 67 |
| 5 | `licenciement-be-protection-grossesse` | SF-213-05-backend | SF-213-05b-frontend | Loi 16/03/1971 art. 40 |
| 6 | `transaction-be-travail` | SF-213-06-backend | SF-213-06b-frontend | Loi 03/07/1978 ; art. 2044 ABC |
| 7 | `harcelement-be-procedure-formelle` | SF-213-07-backend | SF-213-07b-frontend | Loi 04/08/1996 art. 32bis-32sexies ; AR 10/04/2014 |
| 8 | `licenciement-be-protection-deleguee` | SF-213-08-backend | SF-213-08b-frontend | Loi 19/03/1991 ; CCT 5 |
| 9 | `licenciement-be-acte-equivalent` | SF-213-09-backend | SF-213-09b-frontend | Loi 03/07/1978 art. 20 ; Jurisprudence Cass. BE |
| 10 | `licenciement-be-cct109-deraisonnable` | SF-213-10-backend | SF-213-10b-frontend | CCT 109 art. 9 ; arrêt CE n°245.236/2019 |

**Ordre de livraison** : par l'agent dev en vague P2 — commencer par les outils les plus autonomes (non-concurrence, rappel salaire, protection grossesse) puis les outils liés au type de licenciement (statut unique, Claeys, délégué, acte équipollent, CCT 109 déraisonnable), enfin les outils transversaux (transaction, harcèlement).

---

## Décision finale

**GO.** Toutes les briques d'infrastructure amont/aval sont matures (F-207 terminée). Les 10 outils P2 sont indépendants entre eux et techniquement assimilables au pattern F-207. L'effort est sur la **substance juridique BE P2** et l'**isolation BE-only stricte**. Feature à impact écran → étape 0 bis requise (`SF-213-00b-ux-coherence.md`).

---

## Tableau récapitulatif — Outil → SF → flag F-204 → source juridique BE

| Outil | SF backend | SF frontend | Flag F-204 (jumeau ou existant) | Source juridique BE |
|---|---|---|---|---|
| `clause-non-concurrence-be` | SF-213-01 | SF-213-01b | Jumeau annoncé F-DT-24 (migration 199) — NOUVEAU | Loi 03/07/1978 art. 65 ; CCT 13 |
| `rappel-salaire-be` | SF-213-02 | SF-213-02b | Jumeau annoncé F-DT-20 (migration 199) — NOUVEAU | Loi 12/04/1965 ; Loi 03/07/1978 art. 15 |
| `licenciement-be-statut-unique-preavis` | SF-213-03 | SF-213-03b | Spin-off partiel F-DT-09 (non-doublon : vue préavis pure) — NOUVEAU | Loi 26/12/2013 |
| `licenciement-be-formule-claeys` | SF-213-04 | SF-213-04b | Spin-off partiel F-DT-05 supprimé (migration 191) — NOUVEAU | Loi 03/07/1978 art. 82 ancien ; Loi 26/12/2013 art. 67 |
| `licenciement-be-protection-grossesse` | SF-213-05 | SF-213-05b | Pas de jumeau FR direct — NOUVEAU | Loi 16/03/1971 art. 40 |
| `transaction-be-travail` | SF-213-06 | SF-213-06b | Jumeau annoncé F-DT-31 (migration 199) — NOUVEAU | Art. 2044 ABC ; Loi 03/07/1978 |
| `harcelement-be-procedure-formelle` | SF-213-07 | SF-213-07b | Complément F-DT-11 (F-DT-11 = licenciement nul ; ici = procédure interne) — NOUVEAU | Loi 04/08/1996 art. 32bis ; AR 10/04/2014 |
| `licenciement-be-protection-deleguee` | SF-213-08 | SF-213-08b | Pas de jumeau FR direct — NOUVEAU | Loi 19/03/1991 ; CCT 5 |
| `licenciement-be-acte-equivalent` | SF-213-09 | SF-213-09b | Pas d'équivalent FR direct — NOUVEAU | Loi 03/07/1978 art. 20 ; Cass. BE |
| `licenciement-be-cct109-deraisonnable` | SF-213-10 | SF-213-10b | Complément F-DT-08/09 (vue scoring dédiée) — NOUVEAU | CCT 109 art. 9 |
