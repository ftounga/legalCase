# F-207 — Cadrage cohérence (étape 0)

## Verdict : GO

## Intention métier (1 phrase)

Couvrir les 8 outils décisionnels Travail BE de priorité **P1** — outils urgences BE-only sans équivalent FR — pour que l'avocat belge dispose des calculateurs / checklists / générateurs critiques de la séquence post-rupture (délais, C4 ONEM, AT Fedris, référé tribunal travail, RCC, outplacement 45+).

## Source juridique

`docs/features/F-191/audit-be-travail-exhaustif.md` — sections 3.1, 3.2, 3.4, 3.5, 4.3. Tous les articles cités proviennent de cet audit BE (Loi 03/07/1978, CCT 17, CCT 82, AR 25/11/1991, CJ art. 584/580, Loi 10/04/1971).

## Workflow métier réel de l'avocat Travail BE

Source : audit BE exhaustif + pratique standard avocat BE.

1. Le client (salarié BE) arrive avec un dossier de rupture (licenciement, démission, faute grave, RCC, AT).
2. L'avocat évalue d'abord les **délais de prescription** : **1 an** post-rupture pour créances ex-contrat (Loi 03/07/1978 **art. 15**), 5 ans pendant le contrat. **Forclusion irréversible** — outil P1 transversal car il impacte tous les autres.
3. Si **licenciement** : analyse du document **C4 ONEM** émis par l'employeur. Mention « faute grave » → exclusion allocations chômage 4-52 semaines. Contestation du C4 = priorité absolue.
4. Si C4 contesté / exclusion ONEM : **recours administratif** auprès de l'ONEM dans **1 mois**, puis tribunal du travail dans **3 mois** (AR 25/11/1991 art. 144 ; CJ art. 580).
5. Si **accident du travail** : déclaration **Fedris** (ex-Fonds AT, organisme fédéral BE fusionné 2017) sous **8 jours**, sinon préjudice salarié (Loi 10/04/1971).
6. Cas d'**urgence procédurale** (harcèlement persistant, salaire impayé, modification unilatérale) : **référé président tribunal du travail** (CJ art. 584). Mesures provisoires.
7. Cas **RCC** (régime de chômage avec complément d'entreprise, ex-prépension) : analyse de l'éligibilité — RCC général **60+/40**, RCC métiers lourds **58+/35**, RCC long carrière **59+/40**, RCC entreprise en difficulté (CCT 17 ; AR 03/05/2007).
8. Si éligible RCC : calcul de l'**indemnité complémentaire** = différentiel ONEM / dernière rémunération nette (CCT 17, CCT sectorielles). Calcul complexe.
9. Si salarié **45+ ans** : vérifier l'**outplacement obligatoire** — 60 h sur 12 mois à offrir par l'employeur (CCT 82). Sanction 1 800 € employeur + perte d'allocations salarié si non respecté.
10. Suite du dossier : pipeline standard LegalCase (analyse IA, synthèse, conclusions).

## Cartographie features actuelles ↔ workflow

| Étape métier | Outil LegalCase | Statut |
|---|---|---|
| 1. Constitution / upload dossier | F-43 | ✅ Livrée |
| 2. **Prescription délais** | `prescription-be-litige-travail` | ❌ **MANQUE — SF-207-01** |
| 3. **C4 ONEM checklist** | `c4-onem-checklist` | ❌ **MANQUE — SF-207-02** |
| 4. **Contestation C4 / exclusion ONEM** | `contestation-c4-onem` (jumeau BE F-DT-35) | ❌ **MANQUE — SF-207-03** |
| 5. **AT Fedris déclaration 8 j** | `at-fedris-declaration` (jumeau BE F-DT-33) | ❌ **MANQUE — SF-207-04** |
| 6. **Référé tribunal du travail** | `refere-tribunal-travail-be` (jumeau BE F-DT-34) | ❌ **MANQUE — SF-207-05** |
| 7. **RCC conditions éligibilité** | `rcc-be-conditions` | ❌ **MANQUE — SF-207-06** |
| 8. **RCC indemnité complémentaire** | `rcc-be-indemnite-complementaire` | ❌ **MANQUE — SF-207-07** |
| 9. **Outplacement obligatoire 45+** | `outplacement-be-obligatoire-45` | ❌ **MANQUE — SF-207-08** |
| 10. Analyse / synthèse / conclusions | F-3/4/5, F-98 | ✅ Livrée |

## Briques d'infrastructure amont — toutes livrées

- **Panneau outils décisionnels** (F-IA-04 / `decisional-tools-panel`) — accueille de nouveaux outils via `TOOL_REGISTRY`.
- **Gate `workspaceCountry`** — pattern BE-only éprouvé (F-198/F-204, BE Famille, BE Immigration).
- **Pré-remplissage IA** via record `TravailExtractedData` + prompt `LegalDomainPromptBuilder` (extension `country=BELGIUM`). Pattern F-246.
- **Validation cohérence F-IA-03** + émission `critereCode` côté BE (F-250 SF-250-04 livré pour Travail BE existant). Garde-fou `CritereCodeIntegrityIT` (SF-250-11) en place.
- **Migration Liquibase `*-analyses`** : pattern existant (`refere-prudhomal-analyses` 157, `contestation-are-analyses` 158, etc.). À répliquer BE.

## Challenge amont

Chaque étape amont est couverte ?
- Upload / extraction / OCR : ✅ (F-43, F-121, F-122 incl. SF-122-13 multi-pages).
- Pipeline IA + analyse documentaire : ✅ (F-3/4/5).
- Détection contextuelle BE des situations travail (`statut_protege_detecte`, etc.) : pattern F-166 — à étendre par outil BE (extension prompt) au sein de chaque SF.

**Aucun trou amont.** Les briques d'infrastructure sont matures.

## Challenge aval

Sortie de chaque outil : verdict décisionnel (éligible / non, délai dépassé / non, sanction encourue / non) consommé par :
- Le **dashboard décisionnel** du dossier (F-IA-02) — refresh `triggerRefresh()`.
- La **synthèse + conclusions** (F-98) — le calcul de l'outil enrichit le projet de conclusions.

**Aucun trou aval.**

## STOPs / pré-requis à ajouter au backlog

Aucun. Les 8 outils sont indépendants l'un de l'autre du point de vue technique (chacun est un calculateur/checklist autonome). Dépendance fonctionnelle uniquement (l'outil prescription est transversal — il s'applique à tous les autres dossiers BE, pas un bloqueur de dev).

## Invariants anti-gadget pour les mini-specs

1. **Partir des sources BE — pas de calque FR** (mémoire `feedback_belgique_never_forget`). Droit BE distinct : prescription 1 an post-rupture (vs FR 12 mois rupture + 2 ans contrat + 3 ans salaires), RCC sans équivalent FR, Fedris ≠ Sécu FR, CJ art. 584/580 ≠ Code du travail FR.
2. **Workspace gate BE-only strict.** Chaque outil n'apparaît qu'aux workspaces `country=BELGIUM`. Test d'isolation `country=FRANCE` → outil masqué — **obligatoire par SF**.
3. **Critères F-IA-03 distincts BE** — chaque outil émet ses `critereCode` (préfixe `BE_*` à arbitrer dans les mini-specs) dans les prompts BE de `CaseAnalysisService` et `AiQuestionService`. Garde-fou `CritereCodeIntegrityIT` (SF-250-11) doit rester vert à chaque merge.
4. **Un outil = une situation métier** (mémoire `feedback_decision_tools_one_per_situation`). RCC conditions et RCC indemnité sont **distincts** (CCT 17 conditions vs CCT 17 indemnité, audit explicite : « le calcul est complexe »).
5. **Pré-remplissage IA obligatoire** sur tous les champs saisissables (mémoire `feedback_decision_tools_all_fields_prefilled` ; F-246 invariant). Chaque mini-spec frontend a une section « Champs IA à extraire » couverte par l'extension backend `TravailExtractedData` + prompt.
6. **Pattern canonique F-IA-04** — chaque section frontend respecte `immigration-title-decision-section` (provenance pré-fill, validation F-IA-03, `getPrefillCount(input)`, `TOOL_REGISTRY` symétrique).

## Découpage en 16 SF (parallélisation back/front par outil)

Pattern par outil : **1 SF backend + 1 SF frontend** parallélisables (contrat API figé dans la mini-spec backend). 8 outils × 2 = 16 SF.

| # | Outil | SF backend | SF frontend | Source juridique |
|---|---|---|---|---|
| 1 | `prescription-be-litige-travail` | SF-207-01-backend | SF-207-01b-frontend | Loi 03/07/1978 art. 15 ; CCT 109 art. 11 |
| 2 | `c4-onem-checklist` | SF-207-02-backend | SF-207-02b-frontend | AR 25/11/1991 art. 92 |
| 3 | `contestation-c4-onem` | SF-207-03-backend | SF-207-03b-frontend | AR 25/11/1991 art. 144 ; CJ art. 580 |
| 4 | `at-fedris-declaration` | SF-207-04-backend | SF-207-04b-frontend | Loi 10/04/1971 |
| 5 | `refere-tribunal-travail-be` | SF-207-05-backend | SF-207-05b-frontend | CJ art. 584 |
| 6 | `rcc-be-conditions` | SF-207-06-backend | SF-207-06b-frontend | CCT 17 ; AR 03/05/2007 |
| 7 | `rcc-be-indemnite-complementaire` | SF-207-07-backend | SF-207-07b-frontend | CCT 17 ; CCT sectorielles |
| 8 | `outplacement-be-obligatoire-45` | SF-207-08-backend | SF-207-08b-frontend | CCT 82 ; CCT 82 bis ; Loi 05/09/2001 |

**Ordre de livraison** : par outil dans l'ordre du tableau ci-dessus — l'outil 1 (prescription) est transversal et fondamental, l'ordre 2-5 suit la séquence métier (rupture → C4 → contestation → AT → urgences), l'ordre 6-8 couvre RCC + outplacement (régimes spécifiques).

**Découpage validé par l'audit** (`docs/features/F-191/audit-be-travail-exhaustif.md` §4.3 et §4.4) : « RCC conditions ≠ RCC indemnité — le calcul est complexe, à éclater ».

## Décision finale

**GO.** Toutes les briques d'infrastructure amont et aval sont matures (panneau, gate, prefill, F-IA-03, F-IA-04). Les 8 outils sont indépendants entre eux et techniquement assimilables au pattern existant — l'effort est sur la **substance juridique BE** et l'**isolation BE-only**. Feature à impact écran (chaque outil ajoute une `*-section.component` au panneau Décision) → étape 0 bis requise (produite dans `SF-207-00b-ux-coherence.md`).
