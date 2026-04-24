# Audit complémentaire F-155 — règle fondamentale pré-fill IA + validation IA F-IA-03

**Date** : 2026-04-24
**Mode** : CORRECTIF (READ-ONLY)
**Scope** : 6 composants décisionnels livrés en parallèle 2026-04-24
**Document parent** : `audit-2026-04-24.md` (PR #512, non mergée au moment de ce complément)
**Auteur du complément** : agent isolé, skill `frontend-coherence-audit` version mise à jour avec section "Pré-remplissage IA + validation IA au changement (RÈGLE FONDAMENTALE)"

---

## 1. Contexte

L'audit initial (PR #512, `audit-2026-04-24.md`) a conclu à **6/6 PASS** sur les 6 composants. Cependant, cet audit **n'avait pas vérifié** la règle fondamentale désormais documentée dans le skill `frontend-coherence-audit` section 5 — à savoir :

1. **Pré-remplissage IA** : `@Input() aiData`, méthode `prefillFromAi()`, signals `provenance<Field>`, badges UI "Pré-rempli depuis l'analyse", effacement au `onXxxChange()` manuel.
2. **Validation IA F-IA-03** : `coherenceAlerts` computed + directive `CoherencePopoverTriggerDirective` sur les fields clés, sources multiples (`aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`).

Ces 2 mécanismes sont **fondamentaux** : sans eux, l'outil décisionnel redevient "encore un formulaire" au lieu d'un assistant branché sur l'analyse IA du dossier. Le skill précise explicitement : **sans ces 2 mécanismes, l'outil est marqué FAIL (pas WARN)**.

**Pattern de référence** : `frontend/src/app/case-files/immigration-title-decision-section/immigration-title-decision-section.component.ts` — signals `provenanceMotif`, `provenanceSituationFamiliale`, `provenanceNationaliteUe`, méthodes `prefillFromAi()`, `buildMotifAlert()`, `buildNationaliteAlert()`, computed `coherenceAlerts` + `alertsSummary`, directive `CoherencePopoverTriggerDirective` dans le template.

Ce document est un **complément** à l'audit initial et ne le remplace pas. Les 10 divergences visuelles/comportementales identifiées initialement restent valides. Il ajoute un **axe orthogonal** : la conformité au pattern IA.

---

## 2. Tableau récapitulatif par composant

Légende :
- `aiData Input` : présence d'un `@Input() aiData?` (ou `synthesis`, `caseAnalysisResult`).
- `prefillFromAi()` : méthode privée invoquée dans `ngOnInit()` ET `ngOnChanges()`.
- `Signals provenance` : signal `provenance<Field> = signal<'IA' | null>(null)` par champ clé.
- `Badges UI` : template contient `auto_awesome` ou libellé "Pré-rempli depuis l'analyse".
- `onXxxChange clear` : handlers manuels remettent `provenance` à `null`.
- `coherenceAlerts` : computed signal d'alertes de cohérence.
- `CoherencePopoverTrigger` : directive présente dans le template sur les fields clés.

| Composant | aiData Input | prefillFromAi() | Signals provenance | Badges UI | onXxxChange clear | coherenceAlerts | CoherencePopoverTrigger | Verdict |
|---|---|---|---|---|---|---|---|---|
| `harcelement-licenciement-nul-section` (F-DT-11-02) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | **FAIL** |
| `inaptitude-section` (F-DT-15-02) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | **FAIL** |
| `heures-sup-section` (F-DT-19-02) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | **FAIL** |
| `oqtf-avec-delai-section` (F-IM-08-02) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | **FAIL** |
| `oqtf-sans-delai-section` (F-IM-08-04) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | **FAIL** |
| `annexe13-be-section` (F-IM-08-06) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | **FAIL** |

**Bilan : 6 FAIL / 6 composants audités.** Aucun des 6 composants livrés le 2026-04-24 n'implémente ni le pré-remplissage IA, ni la validation IA F-IA-03.

---

## 3. Détail par composant

### 3.1 `harcelement-licenciement-nul-section` (F-DT-11-02)

**Champs du form** :
- `salaireMensuelReference` (number)
- `motifNullite` (enum `MotifNullite`)

**Manque** :
- Aucun `@Input() aiData` ni `synthesis` déclaré.
- Aucune méthode `prefillFromAi()`.
- Aucun signal `provenance*`.
- Template : aucun badge "Pré-rempli depuis l'analyse", aucun `auto_awesome`.
- Aucun `coherenceAlerts` computed ni directive `CoherencePopoverTriggerDirective`.

**Sources IA disponibles à brancher** :
- `CaseAnalysisResult.salaireMensuelReference` (droit du travail).
- `motifsDiscrimination`, `indicesHarcelement` issus de l'analyse (permettent de proposer `MotifNullite.DISCRIMINATION` ou `HARCELEMENT_MORAL`).
- `piecesManquantes` (attestations médecin, témoignages) pour signaler un dossier non solide.

**Impact fonctionnel** : l'avocat ressaisit manuellement le salaire déjà extrait par l'analyse, et ne reçoit aucun signal si le motif choisi diverge des éléments détectés dans le dossier.

**Travail estimé** : **~0.5 jour** (2 champs, sources IA déjà disponibles, pattern `immigration-title-decision-section` à copier).

---

### 3.2 `inaptitude-section` (F-DT-15-02)

**Champs du form** :
- `salaireMensuelReference` (number)
- `ancienneteAnnees` (number)
- `origineInaptitude` (enum `OrigineInaptitude`)
- `reclassementRespecte` (boolean)
- `avisMedecinTravailDate` (date)

**Manque** : idem 3.1 — aucun mécanisme IA.

**Sources IA disponibles à brancher** :
- `CaseAnalysisResult.salaireMensuelReference`.
- `CaseAnalysisResult.ancienneteAnnees` (calcul automatique via dates contrat).
- `origineInaptitude` déductible des pièces (accident travail vs maladie ordinaire).
- `avisMedecinTravailDate` extractible d'une pièce type "Avis d'inaptitude".
- `procedureChecks` F-96 : recherche de reclassement documentée.

**Impact fonctionnel** : 5 champs potentiellement pré-remplis, aucun ne l'est. Perte de temps avocat maximale sur cet outil vs les 5 autres.

**Travail estimé** : **~1 jour** (5 champs, dont 1 date + 1 boolean avec logique `procedureChecks`).

---

### 3.3 `heures-sup-section` (F-DT-19-02)

**Champs du form** :
- `tauxHoraireBrut` (number)
- `heuresSupDeclarees25pct`, `heuresSupDeclarees50pct`, `heuresHorsContingent`
- `tauxMajoration25`, `tauxMajoration50` (défaut 25/50)
- `heuresSupSemaine`, `heuresDimancheJoursFeries` (BE)

**Manque** : idem 3.1.

**Sources IA disponibles à brancher** :
- `CaseAnalysisResult.tauxHoraireBrut` calculable depuis `salaireMensuelReference` / `heuresContractHebdo`.
- `heuresSupDeclarees*` extractibles des bulletins de paie (feature F-145 pièces).
- Alerte cohérence si total heures sup déclarées < heures revendiquées dans les `aiQuestions`.

**Impact fonctionnel** : outil de calcul quantitatif — sans pré-fill l'avocat recompose les chiffres à la main depuis l'analyse.

**Travail estimé** : **~1 jour** (5 à 7 champs numériques, calculs dérivés).

---

### 3.4 `oqtf-avec-delai-section` (F-IM-08-02)

**Champs du form** :
- `dateNotificationOqtf` (date)
- `motifOqtf` (enum `MotifOqtf`)
- `recoursForme` (boolean)
- `dateRecours` (date)

**Manque** : idem 3.1.

**Sources IA disponibles à brancher** :
- `ImmigrationExtractedData.dateNotificationOqtf` (extractible depuis pièce OQTF).
- `ImmigrationExtractedData.motifOqtfCode` → mapping vers `MotifOqtf`.
- `procedureChecks` F-96 : existence d'un recours déjà déposé.
- Alerte cohérence si `motifOqtf` saisi diverge du motif extrait.

**Impact fonctionnel** : l'analyse IA extrait déjà date et motif de l'OQTF dans F-IM-01/08 — l'outil de calcul du délai de recours devrait les récupérer automatiquement.

**Travail estimé** : **~0.75 jour** (4 champs, type correspond au pattern `immigration-title-decision-section` de référence).

---

### 3.5 `oqtf-sans-delai-section` (F-IM-08-04)

**Champs du form** :
- `dateHeureNotificationOqtf` (datetime-local)
- `motifSansDelai` (enum `MotifSansDelai`)
- `placementCra` (boolean)
- `recoursForme` (boolean)
- `dateHeureRecours` (datetime-local)

**Manque** : idem 3.1.

**Sources IA disponibles à brancher** :
- `dateHeureNotificationOqtf` et `placementCra` souvent co-extractibles d'un même arrêté (préfecture).
- `motifSansDelai` déductible du texte de l'arrêté.
- Alerte cohérence **critique** vu l'urgence 48h : si la date IA est > 48h et l'avocat coche "pas encore de recours", signaler le risque d'irrecevabilité.

**Impact fonctionnel** : sur cet outil à urgence absolue (délai 48h JLD), l'absence de pré-fill IA + alerte cohérence est le cas le plus dangereux des 6. Un recours oublié = conséquences directes client.

**Travail estimé** : **~1 jour** (5 champs, logique temporelle 48h + datetime précis).

---

### 3.6 `annexe13-be-section` (F-IM-08-06)

**Champs du form** :
- `dateNotificationAnnexe13` (date)
- `delaiDepartImposeJours` (number, défaut 30)
- `motifOqt` (enum `MotifOqt`)
- `transfertImminent` (boolean)
- `recoursForme` (boolean)
- `typeRecours` (enum `TypeRecours`)
- `dateRecours` (date)

**Manque** : idem 3.1.

**Sources IA disponibles à brancher** :
- `ImmigrationExtractedData` côté Belgique : date notification Annexe 13, délai départ imposé, motif OQT, indicateur transfert imminent extractibles.
- `procedureChecks` F-96 BE (recours CCE / extrême urgence).
- Alerte cohérence : si `transfertImminent=true` détecté par l'IA mais case non cochée par l'avocat.

**Impact fonctionnel** : outil Belgique avec le plus de champs (7). Pré-fill IA particulièrement rentable.

**Travail estimé** : **~1 jour** (7 champs, 2 enums + datepickers + logique transfert imminent).

---

## 4. Impact par domaine métier

| Domaine | Composants FAIL | Poids relatif | Commentaire |
|---|---|---|---|
| Droit du travail (FR) | 3 (harcèlement, inaptitude, heures sup) | ~50% | Les 3 sont alimentés par `CaseAnalysisResult` — déjà stabilisé depuis F-IA-01/02. |
| Immigration (FR) | 2 (OQTF avec délai, OQTF sans délai) | ~33% | Alimenté par `ImmigrationExtractedData` (F-IM-01) — pattern `immigration-title-decision-section` directement réutilisable. |
| Immigration (BE) | 1 (Annexe 13) | ~17% | Dépend de l'extraction BE (F-IM-07/08 BE) — à vérifier que `ImmigrationExtractedData` BE expose les bons champs. |

**Parité des domaines** : le pattern pré-fill IA + validation F-IA-03 est déjà prouvé côté Immigration FR (`immigration-title-decision-section`). Aucune asymétrie méthodologique — c'est purement un **déficit d'implémentation** sur les 6 composants du batch 2026-04-24.

---

## 5. Plan de migration (nouvelle SF)

### SF-155-04 (nouvelle) — Pré-fill IA + validation F-IA-03 sur les 6 composants décisionnels du batch 2026-04-24

**Objectif** : amener les 6 composants au niveau de conformité du pattern canonique `immigration-title-decision-section` sur les axes "pré-fill IA" et "validation IA F-IA-03".

**Effort total estimé** : **~5.25 jours** (somme des estimations composant par composant).

**Découpage en sous-SFs parallélisables par domaine** :

| Sub-SF | Composants | Domaine | Effort | Source IA |
|---|---|---|---|---|
| SF-155-04-A | `harcelement-licenciement-nul`, `inaptitude`, `heures-sup` | Droit du travail FR | ~2.5 j | `CaseAnalysisResult` |
| SF-155-04-B | `oqtf-avec-delai`, `oqtf-sans-delai` | Immigration FR | ~1.75 j | `ImmigrationExtractedData` |
| SF-155-04-C | `annexe13-be` | Immigration BE | ~1 j | `ImmigrationExtractedData` (champs BE) |

**Contrainte d'ordre** : chaque sub-SF est indépendante, elles peuvent être livrées en parallèle. SF-155-04-A et SF-155-04-B peuvent démarrer immédiatement. SF-155-04-C dépend de la disponibilité des champs BE dans `ImmigrationExtractedData` (à vérifier en readiness).

**Critères d'acceptation communs (à dupliquer dans chaque sub-SF)** :
1. `@Input() aiData?: <TypeSpecifiqueDomaine> | null` déclaré et connecté depuis le panel F-IA-04.
2. Méthode privée `prefillFromAi()` invoquée dans `ngOnInit()` et `ngOnChanges()` (quand `aiData` change avant première résolution).
3. Signals `provenance<Field>` pour chaque champ clé (tous les champs avec source IA crédible).
4. Badge UI "Pré-rempli depuis l'analyse" (`auto_awesome`) à côté de chaque champ avec provenance = 'IA'.
5. Handlers `onXxxChange()` remettent `provenance` à `null` dès modification manuelle.
6. `coherenceAlerts` computed signal exposant les alertes par field.
7. Directive `CoherencePopoverTriggerDirective` appliquée sur les fields clés du template.
8. Sources multiples considérées quand disponibles : `aiData`, `procedureChecks` (F-96), `aiQuestions` (F-IA-02), `piecesManquantes` (F-145).
9. Test unitaire : pré-fill avec `aiData` mocké → valeur affichée + badge présent.
10. Test unitaire : changement manuel → badge disparaît.
11. Test unitaire : divergence aiData vs valeur → alerte `coherenceAlerts` non vide.

**Non-goals** :
- Ne pas modifier la logique de calcul métier (formules, endpoints backend).
- Ne pas refaire les 10 divergences visuelles/comportementales de l'audit initial (déjà tracées dans `audit-2026-04-24.md`).
- Ne pas remplacer `immigration-title-decision-section` comme template canonique (toujours la référence).

**Prérequis readiness** :
- Confirmer que le panel F-IA-04 `decisional-tools-panel` transmet bien `aiData` aux composants (ou l'exposer via un service partagé type `CaseAiContextService`).
- Vérifier que `ImmigrationExtractedData` expose les champs BE nécessaires à SF-155-04-C (sinon ouvrir une SF backend BE préalable).

---

## 6. Analyse de cohérence transversale

Cet audit révèle une **dette de convergence critique** : 6 composants livrés en parallèle par 6 agents autonomes le **même jour** ont **tous** omis la règle fondamentale pré-fill IA / F-IA-03. Cela indique :

1. **La mini-spec des 6 SFs ne référençait pas explicitement le pattern canonique `immigration-title-decision-section`** — elles ont sans doute pris `harcelement-licenciement-nul-section` (F-DT-11-02, premier du batch) comme référence, composant lui-même non conforme.
2. **Le skill `frontend-coherence-audit` n'avait pas encore cette section "Pré-remplissage IA + validation IA au changement (RÈGLE FONDAMENTALE)"** — correction apportée postérieurement.
3. **L'audit préventif par skill a été court-circuité** sur ces 6 composants (livraison parallèle rapide en fin de sprint F-IA-04).

**Recommandations de gouvernance** :
- **Mettre à jour le template canonique** `harcelement-licenciement-nul-section` lors de SF-155-04-A pour qu'il devienne un vrai template complet (IA compris) — sinon risque de perpétuer le déficit sur les prochains composants livrés.
- **Bloquer en readiness** toute nouvelle SF frontend décisionnelle qui ne référence pas explicitement le pattern `immigration-title-decision-section` dans sa section "Pattern de référence" de la mini-spec.
- **Ajouter un check automatique CI** : si un fichier `*-section.component.ts` sous `frontend/src/app/case-files/` touche un outil décisionnel et n'expose ni `aiData` ni `coherenceAlerts`, échouer le build (règle linter custom ou test d'intégration de convention).

---

## 7. Conclusion

- **6/6 composants FAIL** sur la règle fondamentale pré-fill IA + validation F-IA-03.
- **Verdict global** : l'audit initial PR #512 reste valide sur ses axes (visuel, comportemental, palette, typographie, refresh dashboard) mais **doit être complété par ce document** avant de conclure "6/6 PASS".
- **Conclusion corrigée** : 6 PASS sur axes visuels/comportementaux + **6 FAIL sur axe IA** → sortie nette = **6 FAIL, plan de migration SF-155-04 nécessaire avant de considérer F-DT-11, F-DT-15, F-DT-19, F-IM-08 comme "Terminées"**.
- **Recommandation** : créer SF-155-04 (split en 3 sub-SFs A/B/C par domaine) et la prioriser avant tout nouveau composant décisionnel, pour éviter que la dette ne se propage aux 61 features restantes du bloc 2026-04-24 (F-DT-11→35, F-IM-08→20, F-FA-08→27, F-136/137/138).
- **Mise à jour du template canonique** obligatoire dans SF-155-04-A : `harcelement-licenciement-nul-section` doit exposer le pattern IA complet pour servir de vraie référence aux futures SFs.

---

## 8. Addendum 2026-04-24 (post-readiness) — cause racine : dette backend IA, pas pattern frontend

Lors de la readiness de SF-155-04, un fait structurant a été découvert qui modifie le plan de migration initialement proposé en section 5.

### 8.1 Constat

Les 6 composants ne peuvent pas être rendus IA-compliant par simple pattern frontend, parce que **les champs requis n'existent pas dans les DTOs backend `TravailExtractedData` et `ImmigrationExtractedData`** — donc le prompt système IA n'extrait pas ces champs, donc il n'y a rien à pré-remplir depuis l'analyse.

**Champs requis absents des records Java** (fichier `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisResponse.java`) :

| Composant | Champs absents | Record concerné |
|---|---|---|
| harcèlement | `motifNullitePressenti` | `TravailExtractedData` |
| inaptitude | `origineInaptitudePressentie`, `avisMedecinTravailDate`, `reclassementRespecteDetected` | `TravailExtractedData` |
| heures sup | `heuresSupMentionneesDansDossier`, `tauxHoraireBrutDeduit` | `TravailExtractedData` |
| oqtf-avec-délai | `dateNotificationOqtf`, `motifOqtfCode`, `placementCra` | `ImmigrationExtractedData` FR |
| oqtf-sans-délai | `dateHeureNotificationOqtf`, `motifSansDelaiCode` | `ImmigrationExtractedData` FR |
| annexe13-be | `dateNotificationAnnexe13`, `delaiDepartImposeJours`, `motifOqtCode`, `transfertImminent` | `ImmigrationExtractedData` BE |

### 8.2 Pourquoi les outils décisionnels antérieurs n'ont pas ce problème

Vérification dans `decisional-tools-panel.component.ts` ligne 96-278 : les outils livrés **avant** 2026-04-24 branchent bien `aiData` via `ctx.synthesis?.xxxExtractedData` (F-DT-07, F-DT-08, F-DT-09, F-DT-10, F-IM-05, F-IM-06, F-IM-07, F-FA-05, F-FA-06). Chacun, au moment de sa création, a étendu :
- le record Java `TravailExtractedData` ou `ImmigrationExtractedData`,
- le prompt système correspondant (`LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION` / `IMMIGRATION_INSTRUCTION`),
- la méthode de parsing JSON (`extractTravailData()` / `extractImmigrationData()`),
- le DTO frontend (`case-analysis.model.ts`),
- le binding `TOOL_REGISTRY.inputs(ctx)`.

Références d'extensions historiques : SF-IM-01-04 (`inferredChecklistType`), SF-130-01 (`salaireEstDeduit`), SF-DT-04-04 (identités salarié/employeur).

### 8.3 Cause racine — facteurs conjugués

1. **Parallélisation agressive** — 6 agents autonomes en 1 journée, chacun focalisé sur son calculateur (pattern implicite = "un formulaire qui calcule").
2. **Règle skill ajoutée APRÈS livraison** — commit `21e75ce docs(skill): add pre-fill IA + validation IA F-IA-03 rule` est postérieur aux 6 outils. Les agents n'avaient pas le rappel.
3. **Template canonique dégradé** — les agents ont implicitement pris `harcelement-licenciement-nul-section` (1er du batch, lui-même non-IA) comme référence au lieu du canonique `immigration-title-decision-section`.
4. **`subfeature-template.md` ne force pas la déclaration** — aucune section obligatoire "Champs IA à extraire — extensions record + prompt système requises".

### 8.4 Plan de migration corrigé (remplace la section 5)

**Abandon de la section 5** : le split A/B/C frontend-only ~5.25 j sous-estime la dette. Le vrai chantier est full-stack par domaine.

**Nouveau découpage Option B respectant règle 2j** (9 sub-SFs) :

| Palier | Sub-SF | Scope | Effort |
|---|---|---|---|
| 1 — Backend | SF-155-04-00-BE-travail | Extension `TravailExtractedData` + prompt travail + `extractTravailData()` → 5 champs harcèlement/inaptitude/heures-sup. Tests + fixtures. | ~1.5 j |
| 1 — Backend | SF-155-04-00-BE-immig-FR | Extension `ImmigrationExtractedData` + prompt immigration FR + `extractImmigrationData()` → 5 champs OQTF avec/sans délai. Tests + fixtures. | ~1.5 j |
| 1 — Backend | SF-155-04-00-BE-immig-BE | Extension même record + prompt immigration BE → 4 champs Annexe 13. Tests + fixtures. | ~1 j |
| 2 — Frontend | SF-155-04-A1 | `harcelement-licenciement-nul-section` (template canonique) — dépend BE-travail | ~0.5 j |
| 2 — Frontend | SF-155-04-A2 | `inaptitude-section` — dépend BE-travail | ~0.75 j |
| 2 — Frontend | SF-155-04-A3 | `heures-sup-section` — dépend BE-travail | ~0.75 j |
| 2 — Frontend | SF-155-04-B1 | `oqtf-avec-delai-section` — dépend BE-immig-FR | ~0.5 j |
| 2 — Frontend | SF-155-04-B2 | `oqtf-sans-delai-section` (urgence 48h, priorité) — dépend BE-immig-FR | ~1 j |
| 2 — Frontend | SF-155-04-C | `annexe13-be-section` — dépend BE-immig-BE | ~1 j |

**Total réel** : ~4 j backend + ~4.5 j frontend = ~8.5 j (vs 5.25 j annoncés initialement). Avec parallélisation maximale (3 agents backend + 3-5 agents frontend par palier), étalé sur ~3-4 jours calendaires.

### 8.5 Règle gouvernance à ajouter (préventive)

Pour empêcher la récurrence :
- **`subfeature-template.md`** — ajouter une section obligatoire "Champs IA à extraire" pour toute SF créant un outil décisionnel avec formulaire. Liste des champs requis + vérification de leur présence dans le record Java correspondant + extension prompt + parsing + DTO frontend.
- **`readiness-checklist.md`** — item bloquant : "Si la SF frontend consomme `aiData`, tous les champs utilisés par le composant existent déjà dans le record Java concerné (sinon ouvrir une SF backend préalable)".
- **`ai-skills/frontend-coherence-audit.md`** — la règle "Pré-fill IA + validation IA F-IA-03" est insuffisante seule ; elle doit renvoyer au prérequis backend (champs DTO).

---

**Document produit en READ-ONLY — aucune modification de code dans cet audit.**
