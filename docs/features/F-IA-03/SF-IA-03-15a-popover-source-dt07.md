# Mini-spec — F-IA-03 / SF-IA-03-15a Popover source enrichie (infrastructure + pilote F-DT-07)

## Identifiant

`F-IA-03 / SF-IA-03-15a`

## Feature parente

`F-IA-03` — Contrôle de cohérence sur les outils décisionnels

## Statut

`draft`

## Date de création

2026-04-16

## Branche Git

`feat/SF-IA-03-15a-popover-source-dt07`

---

## Objectif

Remplacer le tooltip d'incohérence actuel (une ligne floue `"L'IA a détecté : X"`) par un popover actionnable qui affiche **la source typée**, **une raison reformulée** et **une explication textuelle générée lors de l'analyse**, avec un lien de navigation vers la source (document, question, checklist F-96, chat, pièce manquante). Pilote sur F-DT-07 ; infrastructure partagée pour les autres outils (SF-IA-03-15b/c).

---

## Comportement attendu

### Cas nominal

1. Le pipeline d'analyse dossier reste inchangé jusqu'à la réponse Sonnet. **En post-traitement synchrone du même job d'analyse**, un nouveau service `SourceExplanationGenerator` appelle **Claude Haiku** (tâche simple : reformulation pédagogique) avec comme input les données extraites par Sonnet (convention, date entrée, salaire, type rupture, prime contractuelle, congés contractuels…) + extraits bruts des documents cités + ancres (doc id, question id…). Haiku retourne un JSON `source_explanations: [{sourceKey, sourceType, label, sentence (≤ 220 car), anchor}]` persisté dans la nouvelle table `case_analysis_source_explanations`. Le job d'analyse ne passe DONE que quand cette étape est terminée (ou échoue fail-open).
2. Chaque phrase est associée à une **source typée** (`source_key` + `anchor` : doc id / question id / F-96 code / chat message id / null) et à un **libellé lisible** (`label`, ex. `contrat_dupont.pdf`, `Question : "Quelle est l'ancienneté ?"`).
3. L'avocat ouvre F-DT-07 sur un dossier analysé. Le backend `AncienneteService.toResponse` enrichit chaque `EcartData` avec la source+phrase associée au champ concerné (mapping statique champ→sourceKey).
4. L'avocat saisit une valeur qui diverge. Un badge apparaît (inchangé).
5. Au survol du badge (délai 200 ms) : **popover CDK Overlay** ~340 px s'affiche, ancré sur le badge, design navy/gold :
   - Icône + libellé du type de source (ex. 📄 *Document du dossier*)
   - Titre bold : la **raison reformulée** (ex. *"La convention BTP prévoit une prime de 12 %"*)
   - Italique : **explication générée** (ex. *"La CCN des ouvriers du BTP (IDCC 1596) fixe la prime d'ancienneté à 12 % à partir de 15 ans d'ancienneté continue."*)
   - Séparateur + lien bouton : *"Voir la source →"* (actif uniquement si anchor résolu)
6. Le popover reste ouvert tant que la souris est dans le badge OU dans le popover (buffer de 100 ms). Clic extérieur ou `Escape` le ferme.
7. **Clic sur "Voir la source →"** : dispatch via `CoherenceSourceNavigatorService` selon `actionType` :
   - `OPEN_DOCUMENT` → router vers `/case-files/{id}/documents/{docId}` + scroll vers document (viewer existant).
   - `SCROLL_QA` → router vers `/case-files/{id}/synthesis?qa={questionId}` + scroll + highlight 2 s (pattern SF-IA-03-12).
   - `SCROLL_F96` → router vers `/case-files/{id}/synthesis?check={code}` + scroll + highlight.
   - `OPEN_CHAT` → router vers `/case-files/{id}/synthesis?chat={messageId}` + scroll panneau chat + highlight.
   - `MISSING_PIECE` → router vers `/case-files/{id}/synthesis?piece={index}` + scroll section pièces manquantes.
   - `NONE` → lien masqué.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Phrase d'explication absente pour un champ (dossier ancien analysé avant cette SF, ou IA silencieuse pour ce champ) | Popover affiche uniquement la raison reformulée (template statique Java) sans explication générée ni lien actionnable | 200 |
| `anchor` défini mais document/question/F-96/chat supprimé entre-temps | Lien "Voir la source" absent, popover reste informatif | 200 |
| Haiku retourne un JSON mal formé pour `source_explanations` | Fallback fail-open : les explications manquantes → fallback template Java, analyse dossier ne doit **pas** échouer | 200 |
| Haiku indisponible / timeout / 5xx Anthropic | Job d'analyse **reste DONE**, les explications sont absentes, popover tombe en fallback | 200 |
| Haiku dépasse le budget tokens configuré | Troncature déterministe, phrases restantes ignorées, fallback | 200 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : 10 outils porteurs d'alertes F-IA-03 (F-DT-07/08/09/10, F-FA-05/06/07, F-IM-05/06/07). Applicable à tous → propagés dans SF-IA-03-15b (Droit du travail + Famille) et SF-IA-03-15c (Immigration). Cette SF livre l'**infrastructure partagée** et l'**intégration pilote F-DT-07**.
- [x] **Autres pays** : FR + BE applicables. F-DT-07 couvre les deux (conventions BTP/SYNTEC/CP200…). Pas de logique pays-spécifique dans le popover : la phrase générée par l'IA est déjà adaptée au contexte.
- [x] **Autres domaines** : DROIT_DU_TRAVAIL / DROIT_FAMILLE / DROIT_IMMIGRATION — applicable partout, délégué à 15b/15c.
- [x] **Autres UI patterns** : nouveau pattern transversal *"popover d'incohérence actionnable"* introduit par cette SF. Les composants sont dans `shared/` pour réutilisation.
- [x] **Autres flows transversaux** : aucun (pas d'auth, pas de workspace context modifié, pas de plan/limite, pas de guard/redirect ajouté).

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : `AncienneteCoherenceAlert` étendu (nouveaux champs `source`, `sourceLabel`, `reason`, `explanation`, `action`, `actionTarget`). Nouveau modèle `SourceExplanation`.
- [x] **Record / DTO backend** : `AncienneteResponse.EcartData` étendu ; nouveau record `SourceExplanationData` dans la response.
- [x] **Service / logique métier** : **Sonnet inchangé** (pas de dérive risque qualité). Nouveau `SourceExplanationGenerator` appelle Haiku en post-traitement synchrone dans `CaseAnalysisService.analyse()`. Nouveau `SourceExplanationService` (persistance + lookup). `AncienneteService.toResponse` augmenté.
- [x] **Entité JPA + schéma DB** : nouvelle table `case_analysis_source_explanations` (1:N depuis `case_analyses`). Migration Liquibase 075.
- [x] **Tests existants** : les IT d'ancienneté + specs frontend doivent rester verts (non-régression). Les tests F-IA-03-04 (cohérence ancienneté) adaptés au nouveau DTO.

### Cas spécifique : nouvelle feature d'outil décisionnel

Non applicable — cette SF n'introduit pas un nouvel outil, elle enrichit un pattern transversal existant (F-IA-03). Checklist outil décisionnel ignorée.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-DT-07 Ancienneté | Oui | **Intégré dans cette SF (pilote)** |
| F-DT-08 Validité licenciement | Oui | SF-IA-03-15b |
| F-DT-09 Comparateur indemnités | Oui | SF-IA-03-15b |
| F-DT-10 Validité rupture conv | Oui | SF-IA-03-15b |
| F-FA-05 Partage immobilier | Oui | SF-IA-03-15b |
| F-FA-06 Calendrier garde | Oui | SF-IA-03-15b |
| F-FA-07 Checklist divorce | Oui | SF-IA-03-15b |
| F-IM-05 Titre séjour | Oui | SF-IA-03-15c |
| F-IM-06 Recours | Oui | SF-IA-03-15c |
| F-IM-07 Droit au travail | Oui | SF-IA-03-15c |
| FR et BE | Oui | Couvert naturellement (l'IA génère dans la langue/juridiction du dossier) |

### Décision

- [x] Étendu à F-DT-07 dans cette SF + infrastructure partagée
- [x] Subfeatures parallèles créées : SF-IA-03-15b (DT-08/09/10 + FA-05/06/07), SF-IA-03-15c (IM-05/06/07)
- [x] Backlog VN pour les zones hors outils décisionnels où le pattern popover s'applique (complément rétrospectif, voir ci-dessous)

### Scan élargi — autres zones où le pattern popover est applicable (complément rétrospectif 2026-04-16)

Identification manquante dans la mini-spec initiale. Le composant `CoherencePopoverComponent` + `CoherenceSourceNavigator` + endpoint `/source-explanations` étant des primitives transversales, leur réutilisation hors F-IA-03 est envisageable :

| Zone | Pattern actuel | Applicabilité | Classement |
|---|---|---|---|
| **F-69 Délais légaux** — badges "proposé par IA" sur `CaseDeadlinesSectionComponent` | Tooltip simple | Le popover enrichi permettrait d'afficher "détecté dans *assignation.pdf*, §2 : délai de 2 mois art. R1452-1" | **Backlog** → nouvelle SF-IA-03-16 (après 15c) |
| **F-92 Pièces manquantes** — liste dans `SynthesisComponent` | Juste liste, pas de popover | Popover pourrait expliquer "pourquoi cette pièce est recommandée" basé sur la synthèse IA | **Backlog** → SF-92-03 potentiel |
| **F-96 Checklist procédurale** — points `TO_CHECK`/`NON_COMPLIANT` avec `raison` IA | Tooltip raison existant, pattern concurrent | Convergence possible : remplacer le tooltip maison par `CoherencePopoverComponent` | **Backlog** → SF-96-06 (harmonisation) |
| **F-93 Traçabilité sources IA** — badges `source` + `extrait` inline dans `SynthesisComponent` | Badge inline avec nom doc + extrait italique | Pattern *proche* mais moins actionnable ; candidat à migration vers popover unifié | **Backlog** → SF-93-03 (harmonisation) |
| **F-94 Score de risque** — badge couleur `SynthesisComponent` + liste dossiers | Aucun popover | Popover pourrait expliquer "score 75 car 3 risques élevés détectés" | **Backlog** → SF-94-03 (moins prioritaire) |

Aucune de ces cibles n'est bloquante pour la livraison du pilote F-DT-07. Elles seront évaluées lors de la roadmap post-15c. Documentées ici pour traçabilité.

---

## Note de rétrospective

Ce scan élargi a été ajouté **après merge** (2026-04-16) suite à une vérification de gouvernance. La mini-spec initiale avait scanné les 10 outils décisionnels F-IA-03 mais omis les 5 zones connexes ci-dessus. Correctif gouvernance appliqué dans `subfeature-template.md` et `readiness-checklist.md` : section "Nouveau pattern UI ou service partagé" ajoutée pour forcer ce scan à l'avenir.

---

## Critères d'acceptation

- [ ] Nouvelle table `case_analysis_source_explanations` créée via migration Liquibase 075 (id, case_analysis_id FK, source_key, source_type, label, sentence, anchor_doc_id, anchor_question_id, anchor_f96_code, anchor_chat_message_id).
- [ ] Nouveau `SourceExplanationGenerator` : appelle Haiku avec un prompt dédié ("reformule de façon pédagogique et factuelle, ≤ 220 car par phrase, JSON strict `source_explanations`"), prend en input la synthèse Sonnet + documents cités. Appel **synchrone** à la fin de `CaseAnalysisService.analyse()`, avant le commit final du job.
- [ ] Parsing fail-open dédié : JSON invalide → liste vide, analyse dossier reste DONE.
- [ ] Les explications sont persistées par `SourceExplanationService.persist(caseAnalysisId, list)` appelé juste après le `SourceExplanationGenerator`.
- [ ] `AncienneteService.toResponse` enrichit chaque `EcartData` avec 6 nouveaux champs : `sourceType` (enum 7 valeurs), `sourceLabel`, `reason`, `explanation`, `actionType` (enum 6 valeurs), `actionTarget`. Mapping statique champ→sourceKey pour les 5 champs F-DT-07 (CONVENTION, DATE_ENTREE, SALAIRE, CONGES, PRIME).
- [ ] Si aucune explication trouvée pour un sourceKey → `reason` rempli par template Java statique, `explanation` = null, `actionType` = NONE.
- [ ] Frontend : nouveau composant partagé `CoherencePopoverComponent` (CDK Overlay, 340 px, design système, bord gauche orange/rouge selon statut) dans `shared/coherence-popover/`.
- [ ] Frontend : directive `[appCoherencePopover]` qui attache le popover à un badge, gère hover + clic + Escape, affiche icône/type/raison/explication/lien.
- [ ] Frontend : service `CoherenceSourceNavigator` qui dispatche selon `actionType` (router + scroll + highlight). Fallback `NONE` masque le lien.
- [ ] `AncienneteSectionComponent` : le computed `coherenceAlerts` consomme les nouveaux champs. Les 5 badges F-DT-07 déclenchent un popover enrichi.
- [ ] Isolation workspace : lecture des `source_explanations` sous jointure `case_analysis → case_file.workspace_id`. Tests IT.
- [ ] Tests unitaires backend : parsing fail-open (JSON valide, JSON invalide, champ absent, phrase trop longue). `SourceExplanationService.persist`/`findByCaseAnalysisId`.
- [ ] Tests IT backend : GET ancienneté avec explications → retourne les 5 champs enrichis ; GET sur dossier legacy (sans explications) → fallback propre, pas de 500.
- [ ] Tests frontend : popover apparaît au hover, disparaît à `Escape`, disparaît au clic extérieur, lien actionnable appelle le service navigator.
- [ ] Tests frontend : directive navigator simulé — `OPEN_DOCUMENT`, `SCROLL_QA`, `SCROLL_F96`, `OPEN_CHAT`, `MISSING_PIECE`, `NONE` dispatchent correctement.
- [ ] Build frontend vert, build backend vert, smoke E2E pour F-DT-07 verts.

---

## Périmètre

### Hors scope (explicite)

- Propagation du popover aux 9 autres outils (SF-IA-03-15b/15c).
- Ré-enrichissement des dossiers déjà analysés avant cette SF (endpoint `/regenerate-explanations` → backlog éventuel).
- Surlignage précis de l'extrait dans le document viewer (navigation uniquement vers le document, sans ancre texte).
- Animation d'entrée/sortie du popover au-delà d'un fade simple (l'effet est déjà dans le design system via CDK).
- Tracking analytics des clics sur "Voir la source" (pourrait faire l'objet d'un follow-up).
- Traduction multi-langue (FR seulement, cohérent avec l'app actuelle).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `case_analysis_source_explanations.created_at` | `now()` | auto |
| `case_analysis_source_explanations.source_type` | enum STRING `{DOCUMENT, QUESTION_AI, CHECKLIST_F96, CHAT, MISSING_PIECE, ANALYSIS_DETECTION, MULTI}` | imposé par l'IA / parsing |
| `anchor_*` | NULL si pas résolu | nullable |

Comportements à la création :
- Persistées en batch à la fin de `CaseAnalysisService.analyse()` après commit du `CaseAnalysis` parent.
- Supprimées en cascade avec `CaseAnalysis` (FK ON DELETE CASCADE).

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format | Unicité | Normalisation |
|-------|-------------|-------------|--------|---------|---------------|
| `source_key` | Oui | 64 | snake_case | (case_analysis_id, source_key) | trim + lower |
| `source_type` | Oui | 32 | enum 7 valeurs | — | — |
| `label` | Oui | 255 | texte libre | — | trim |
| `sentence` | Non | 300 | texte libre | — | trim |
| `anchor_doc_id` | Non | UUID | FK molle (pas de FK DB stricte pour rester tolérant aux suppressions) | — | — |
| `anchor_question_id` | Non | UUID | idem | — | — |
| `anchor_f96_code` | Non | 64 | code référentiel F-96 | — | — |
| `anchor_chat_message_id` | Non | UUID | idem | — | — |

Notes :
- `sentence` nullable car l'IA peut être silencieuse sur certaines données extraites ; fallback template.
- Un seul champ `anchor_*` renseigné à la fois (cohérence avec `source_type`).
- Unicité `(case_analysis_id, source_key)` : une donnée extraite = une explication par dossier.

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{id}/anciennete` | Oui (existant) | MEMBER |

Aucun nouvel endpoint. La response `AncienneteResponse` est enrichie.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_analysis_source_explanations` | CREATE + INSERT + SELECT | Nouvelle table |
| `case_analyses` | SELECT | Lecture FK |

### Migration Liquibase

- [x] Oui — `075-create-case-analysis-source-explanations.xml`

### Composants Angular

- `CoherencePopoverComponent` (nouveau, `shared/coherence-popover/`) — card CDK Overlay, 340 px, design navy/gold.
- `CoherencePopoverTriggerDirective` (nouveau) — `[appCoherencePopover]` attaché à un badge.
- `CoherenceSourceNavigatorService` (nouveau, `core/services/`) — dispatcher.
- `AncienneteSectionComponent` — intégration des nouveaux champs + directive sur les 5 badges.

### Composants backend

- `SourceExplanation` (nouvelle entité JPA).
- `SourceExplanationRepository`.
- `SourceExplanationGenerator` (nouveau service) — construit le prompt Haiku à partir de la synthèse Sonnet + documents, appelle Anthropic (modèle Haiku), parse le JSON fail-open.
- `SourceExplanationService` (persist + findByCaseAnalysisId).
- `CaseAnalysisService` — **Sonnet inchangé** ; ajout d'un hook post-réponse Sonnet qui appelle `SourceExplanationGenerator` puis `SourceExplanationService.persist()` avant de committer le job DONE.
- `AncienneteService.toResponse` — jointure + mapping champ→sourceKey.
- `AncienneteResponse.EcartData` étendu.

---

## Plan de test

### Tests unitaires backend

- [ ] `SourceExplanationGenerator` — prompt Haiku bien construit (données extraites + docs cités en input).
- [ ] `SourceExplanationGenerator` — parsing JSON Haiku valide.
- [ ] `SourceExplanationGenerator` — parsing JSON invalide fail-open → liste vide, pas d'exception.
- [ ] `SourceExplanationGenerator` — timeout / 5xx Haiku → liste vide, pas d'exception (analyse continue).
- [ ] `SourceExplanationService` — persist batch nominal.
- [ ] `SourceExplanationService` — phrase > 300 car → tronquée, pas d'exception.
- [ ] `SourceExplanationService` — source_type inconnu → skip silencieux.
- [ ] `AncienneteService` — mapping CONVENTION → sourceKey `convention` trouvée → ecart enrichi.
- [ ] `AncienneteService` — mapping CONVENTION → sourceKey absent → fallback template.

### Tests d'intégration backend

- [ ] `GET /api/v1/case-files/{id}/anciennete` → 200 avec `ecarts[].source*`, `explanation`, `actionType`, `actionTarget` peuplés si analyse récente.
- [ ] `GET /api/v1/case-files/{id}/anciennete` → 200 sur dossier analysé avant cette SF (pas d'explications) → `ecarts[].source*` avec fallback, `explanation=null`, `actionType=NONE`, pas de 500.
- [ ] Isolation workspace : user workspace A ne peut pas lire les `source_explanations` du workspace B (via contrôle d'accès CaseFile).

### Tests frontend

- [ ] `CoherencePopoverComponent` — rendu avec tous les champs non nuls.
- [ ] `CoherencePopoverComponent` — rendu en fallback (explanation=null, actionType=NONE) — lien masqué.
- [ ] `CoherencePopoverTriggerDirective` — hover déclenche ouverture (délai 200 ms).
- [ ] `CoherencePopoverTriggerDirective` — Escape ferme.
- [ ] `CoherencePopoverTriggerDirective` — clic extérieur ferme.
- [ ] `CoherenceSourceNavigatorService` — dispatch correct pour les 6 actionTypes.
- [ ] `AncienneteSectionComponent` — les 5 badges déclenchent le popover avec les bonnes valeurs.
- [ ] Non-régression : les tests SF-DT-07-04, SF-DT-07-05, SF-IA-03-04 restent verts.

### Isolation workspace

- [x] Applicable — la lecture passe par `CaseFile → workspace_id`. Test IT avec 2 users workspaces distincts.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale structurelle** — pas d'auth modifié, pas de workspace context, pas de plans/limites, pas de navigation guardée.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `CaseAnalysisService` | **Prompt Sonnet inchangé** — ajout d'un hook post-réponse uniquement (appel Haiku fail-open). Risque de régression minime. | Tests existants d'analyse + nouveau test fail-open Haiku |
| `AncienneteResponse` | 6 nouveaux champs dans `EcartData` (rétrocompat ajout) | Tests IT Ancienneté existants |
| `AncienneteSectionComponent` | UI badge enrichi | Specs existants + nouveaux |

### Smoke tests E2E concernés

- [x] `e2e/smoke/auth.spec.ts` — Non applicable (pas d'auth modifié) — mais rester vert par précaution.
- [x] Aucun smoke test navigation/workspace touché.

---

## Dépendances

### Subfeatures bloquantes

- `SF-DT-07-05 Done` — fondation F-DT-07 récente (alerts vs bareme).
- `SF-IA-03-14 Done` — détection MULTI source déjà introduite.
- `SF-IA-03-12 Done` — pattern scroll + highlight sur Q&A.

### Questions ouvertes impactées

- [x] Aucune question ouverte impactée (aucune décision structurelle sur pipeline IA / isolation SQL modifiée).

---

## Notes et décisions

- **Pourquoi persister en DB dédiée et pas en JSON dans `case_analyses`** : les explications sont requêtées par outil/champ (lookup par sourceKey). Une table normalisée permet des index et évite de parser un JSON à chaque GET. Pattern cohérent avec `ai_questions`, `deadlines_detected`, etc.
- **Pourquoi sourceKey statique côté backend (pas côté IA)** : le mapping champ métier → sourceKey est une convention technique. L'IA n'a pas à le connaître — elle extrait les données brutes et produit une phrase. Le mapping est codé dans `AncienneteService` (5 entrées), reproductible outil par outil dans 15b/15c.
- **Pourquoi CDK Overlay et pas MatTooltip étendu** : MatTooltip n'accepte pas de contenu riche (icône + bouton cliquable). CDK Overlay est le standard Angular Material pour popovers actionnables.
- **Pourquoi 340 px et pas responsive** : compromis lisibilité/compacité, aligné sur d'autres popovers de l'app (panneau chatbot 360 px, tooltips data). Sur mobile : la directive dégrade en fullscreen dialog (à valider à 15b/c, hors scope pilote).
- **Coût token estimé** : appel Haiku dédié (~2 000 tokens input + 500 output) soit **~0,2 ¢ par dossier analysé**, 10-15× moins cher que d'enrichir le prompt Sonnet. Pattern aligné sur F-32 (modèle adaptatif par étape).
- **Pourquoi synchrone et pas async** : simplicité (pas de nouvelle queue RabbitMQ, pas de statut intermédiaire), cohérence UX (l'analyse est DONE avec tout prêt), latence +3-5 s acceptable sur un job déjà long de ~30 s. Fail-open isolé : si Haiku indisponible, l'analyse passe DONE sans explications et le popover tombe en fallback template.
- **Pourquoi Sonnet reste inchangé** : zéro risque de régression sur la qualité d'analyse principale. La reformulation pédagogique est une tâche simple parfaitement adaptée à Haiku.
- **Fallback template Java** : pour les 5 sourceKeys F-DT-07 (convention, dateEntree, salaireBase, congesContrat, primeContrat), un template de phrase par défaut est codé dans `AncienneteService` ou un helper `SourceExplanationFallback` — utilisé si l'IA n'a pas généré de phrase.
