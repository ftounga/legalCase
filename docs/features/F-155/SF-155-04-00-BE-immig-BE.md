# Mini-spec — F-155 / SF-155-04-00-BE-immig-BE

## Identifiant

`F-155 / SF-155-04-00-BE-immig-BE`

## Feature parente

`F-155` — Audit cohérence composants décisionnels frontend + template canonique

## Statut

`draft`

## Date de création

2026-04-24

## Branche Git

`feat/SF-155-04-00-BE-immig-BE`

---

## Objectif

Étendre le record backend `ImmigrationExtractedData`, la section `IMMIGRATION_INSTRUCTION` du prompt système et la méthode de parsing `extractImmigrationData()` avec 4 champs supplémentaires **Belgique uniquement** (Annexe 13 — OQT belge) nécessaires au pré-remplissage IA du composant `annexe13-be-section` (F-IM-08-06), sans consommer ces champs côté UI (la SF frontend SF-155-04-C s'en chargera).

---

## Comportement attendu

### Cas nominal

1. L'IA reçoit un dossier immigration Belgique. La section `IMMIGRATION_INSTRUCTION` lui demande maintenant de remplir 4 champs supplémentaires dans l'objet JSON d'extraction immigration :
   - `date_notification_annexe13` (YYYY-MM-DD) — date de notification de l'Annexe 13 / OQT belge.
   - `delai_depart_impose_jours` (entier ≥ 0) — délai de départ volontaire fixé par l'Office des étrangers (typiquement 0 / 7 / 30 selon art. 7 et 74/14 Loi 15/12/1980).
   - `motif_oqt_code_be` (enum 4 valeurs alignées sur le calculateur F-IM-08) — motif factuel de l'OQT : `SEJOUR_IRREGULIER_ART_7`, `REFUS_SEJOUR_APRES_DEMANDE`, `FIN_SEJOUR_REGULIER`, `AUTRE`.
   - `transfert_imminent_detected` (boolean) — signal critique : indices d'un transfert imminent vers CRA ou frontière (placement en centre fermé, escorte annoncée, vol de retour programmé).
2. L'IA renvoie son JSON habituel. `extractImmigrationData()` parse les 4 nouveaux champs (null-safe, fail-open).
3. Le record Java `ImmigrationExtractedData` est étendu (9 → 13 arguments) avec **un** constructeur rétrocompat 9-args (pour préserver l'appel existant dans `from()` lignes ~407-417 qui reconstruit la donnée avec `inferredChecklistType`).
4. Les dossiers **FRANCE** doivent avoir ces 4 champs à `null` (le prompt le stipule explicitement) — l'OQTF française et ses champs dédiés sont traités par la SF jumelle `SF-155-04-00-BE-immig-FR` qui s'exécute en parallèle.
5. Une directive de préservation courte et dédiée est ajoutée dans `EnrichedAnalysisService` pour le bloc `immigration_extracted_data` afin d'éviter que ces champs soient perdus lors de la ré-analyse enrichie (pattern symétrique à la directive travail SF-155-04-00-BE-travail).
6. Le DTO frontend `ImmigrationExtractedData` (`case-analysis.model.ts`) expose les 4 nouveaux champs en optional (`?: | null`), prêts à être consommés par SF-155-04-C.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Dossier hors immigration | `immigrationExtractedData` reste `null`, les 4 nouveaux champs n'apparaissent pas | 200 (analyse OK) |
| Dossier immigration FRANCE | L'IA doit remplir les 4 nouveaux champs à `null` (prompt l'indique explicitement) | 200 |
| JSON IA malformé sur un des 4 champs (type incorrect) | Le champ concerné est `null`, les autres champs de `ImmigrationExtractedData` sont préservés (fail-open) | 200 |
| `motif_oqt_code_be` hors whitelist | Champ `null` (fail-open via `normalizeEnumCode`) | 200 |
| `delai_depart_impose_jours` négatif ou non numérique | Champ `null` (fail-open via helper `nonNegativeIntOrNull` introduit en SF-155-04-00-BE-travail) | 200 |
| `transfert_imminent_detected` en string "true"/"false" | Booléen normalisé via `booleanOrNull` existant | 200 |
| `date_notification_annexe13` hors format ISO | Conservé tel quel (pas de validation stricte de format — cohérent avec `dateNotificationDecisionContestee` existant) | 200 |
| Anciennes fixtures JSON (sans ces champs) | Rétrocompat totale — tous les champs historiques restent lisibles, 4 nouveaux champs `null` | 200 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : scanné — voir section 8 de `audit-prefill-ia-2026-04-24.md`. Les 3 outils immigration livrés 2026-04-24 (`oqtf-avec-delai-section`, `oqtf-sans-delai-section`, `annexe13-be-section`) manquent tous de pré-fill IA faute de champs dans le record. Cette SF couvre **BE uniquement** (Annexe 13). Les champs FR (OQTF avec/sans délai) sont traités par SF jumelle SF-155-04-00-BE-immig-FR qui tourne en parallèle.
- [x] **Autres pays** : les 4 champs concernent **BE uniquement**. Le prompt mentionne explicitement "null pour dossiers FRANCE". L'OQTF française relève de SF-155-04-00-BE-immig-FR (procédure et droit distincts — L.614-5 CESEDA vs Loi 15/12/1980 art. 7).
- [x] **Autres domaines** : DROIT_DU_TRAVAIL / DROIT_FAMILLE non concernés (pas d'équivalent Annexe 13 en droit du travail ou famille).
- [x] **Autres UI patterns** : pas de nouveau pattern UI (SF backend pure). Les patterns frontend (provenance, coherenceAlerts, CoherencePopoverTrigger) seront introduits dans SF-155-04-C.
- [x] **Autres flows transversaux** : aucun. La SF ne touche ni auth, ni workspace, ni plans, ni routing.

### Niveaux de vérification

- [x] **Modèle TypeScript** — `frontend/src/app/core/models/case-analysis.model.ts` étendu avec 4 champs optional (contrat public pour SF-155-04-C).
- [x] **Record / DTO backend** — `ImmigrationExtractedData` record étendu de 9 à 13 arguments.
- [x] **Service / logique métier** — `extractImmigrationData()` parse les 4 nouveaux champs ; `EnrichedAnalysisService` directive dédiée pour les préserver lors de la ré-analyse enrichie.
- [x] **Entité JPA + schéma DB** — aucun impact. `immigration_extracted_data` est sérialisé en JSON dans le champ `result` de `case_analysis`, pas de colonne dédiée, pas de migration Liquibase.
- [x] **Tests existants** — `CaseAnalysisResponseTest` couvre `extractImmigrationData()` avec fixtures JSON. Ajout d'environ 10 tests (nominal, enum, delai, transfert, legacy, malformé).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Outils immigration antérieurs (F-IM-05/06/07) | Non | Déjà IA-compliant, pas de régression attendue — tests existants doivent rester verts. |
| Prompt système FR (OQTF avec/sans délai) | Non | Traité par SF jumelle SF-155-04-00-BE-immig-FR (parallèle). |
| Record `TravailExtractedData` | Non | Traité et mergé par SF-155-04-00-BE-travail (#518). |
| Entité `CaseAnalysis` / table `case_analysis` | Non applicable | Sérialisation JSON agnostique au nombre de champs. |
| `Annexe13BeService`, `Annexe13BeCalculator`, `Annexe13BeController` | Oui | Intégré dans la SF — vérifier que l'ajout de champs au record Java ne casse pas ces services (ils ne consomment pas le record, ils consomment leur propre `Annexe13BeRequest` envoyé par le frontend). |
| Code qui reconstruit `ImmigrationExtractedData` dans `CaseAnalysisResponse.from()` (ligne ~407 — post-inferChecklistType) | Oui | Rétrocompat 9-args garantie → le `new ImmigrationExtractedData(...)` actuel à 9 args continue de fonctionner. |
| Cas historiques de fixtures JSON legacy | Oui | Intégré dans la SF — test explicite de rétrocompat sur une fixture pré-SF-155-04. |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (record + prompt + parsing + DTO frontend + tests).
- [x] Subfeature(s) parallèle(s) créée(s) — `SF-155-04-00-BE-immig-FR` (OQTF France) tourne en parallèle sur la même zone de code (coordination par position des champs : BE à la fin après `inferredChecklistType`, FR entre ou autour — chaque SF écrit à son endroit défini, le rebase mécanique du second qui merge).
- [x] Backlog pour les cibles non prioritaires — n/a (les 3 SF backend du palier 1 couvrent tout le nécessaire pour débloquer SF-155-04-A/B/C frontend).
- [x] Non applicable aux autres cibles — justifications ci-dessus.

---

## Critères d'acceptation

- [ ] Le record Java `ImmigrationExtractedData` expose 4 nouveaux champs nullable : `dateNotificationAnnexe13` (String YYYY-MM-DD), `delaiDepartImposeJours` (Integer ≥ 0), `motifOqtCodeBe` (String enum via whitelist), `transfertImminentDetected` (Boolean).
- [ ] Les 4 champs sont ajoutés **à la fin** du record, après `inferredChecklistType`, pour minimiser les conflits avec la SF jumelle FR qui insère ses champs entre ou à proximité.
- [ ] Un constructeur de rétrocompat 9-args est conservé (signature actuelle) pour préserver le code du `from()` qui reconstruit le record après `inferChecklistType`. Les 4 nouveaux champs sont alors initialisés à `null`.
- [ ] Un set statique privé `MOTIFS_OQT_BE_CODES` contenant les 4 codes alignés sur le calculateur F-IM-08 (`SEJOUR_IRREGULIER_ART_7`, `REFUS_SEJOUR_APRES_DEMANDE`, `FIN_SEJOUR_REGULIER`, `AUTRE`) sert de whitelist pour `motifOqtCodeBe`.
- [ ] Le prompt `LegalDomainPromptBuilder.IMMIGRATION_INSTRUCTION` décrit explicitement les 4 nouveaux champs (inséré **juste avant** le bloc `trigger_events` pour minimiser les conflits avec SF jumelle FR) : valeurs enum autorisées, format date, contrainte entier ≥ 0, règle "null pour dossiers FRANCE".
- [ ] `extractImmigrationData()` parse les 4 nouveaux champs depuis le JSON IA, retourne `null` pour chaque champ absent, malformé, hors-liste ou négatif. Les 4 nouveaux champs sont passés au constructeur à la fin, après les 9 existants.
- [ ] `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE` inclut une directive courte de préservation pour `immigration_extracted_data` listant les champs clés (symétrique à celle de `travail_extracted_data`), citant les 4 nouveaux champs BE.
- [ ] Le DTO frontend `ImmigrationExtractedData` dans `case-analysis.model.ts` expose les 4 nouveaux champs optional (`dateNotificationAnnexe13?: string | null`, `delaiDepartImposeJours?: number | null`, `motifOqtCodeBe?: string | null`, `transfertImminentDetected?: boolean | null`), ajoutés à la fin de l'interface.
- [ ] Tests unitaires dans `CaseAnalysisResponseTest` couvrent : fixture nominal BE complet (4 champs remplis), fixture enum valide upper-case, fixture enum invalide → null, fixture enum lowercase → normalisé, fixture delai négatif → null, fixture delai 0 (cas OQT sans délai) → conservé, fixture transfert boolean + fixture transfert en string-coerce, fixture date ISO simple, fixture legacy (rétrocompat — 4 nouveaux champs `null`), fixture malformée (un des champs type incorrect, autres OK).
- [ ] Tests unitaires dans `LegalDomainPromptBuilderTest` : sentinelle prompt contient les 4 noms de champs + les 4 valeurs enum BE + la mention "FRANCE" (règle null).
- [ ] Tests unitaires dans `EnrichedAnalysisServiceTest` : sentinelle directive de préservation mentionne `immigration_extracted_data` + les 4 nouveaux noms de champs.
- [ ] Les tests existants `CaseAnalysisResponseTest` (immigration + travail), `Annexe13BeCalculatorTest`, `Annexe13BeControllerIT`, `EnrichedAnalysisServiceTest`, `LegalDomainPromptBuilderTest` restent verts.
- [ ] `./mvnw test` vert (1578+ tests), `tsc --noEmit` frontend vert.

---

## Périmètre

### Hors scope (explicite)

- Aucune consommation UI de ces 4 champs : le binding `aiData` côté `TOOL_REGISTRY.inputs(ctx)` pour F-IM-08-06 est à faire dans SF-155-04-C.
- Aucun pattern frontend (provenance, prefillFromAi, coherenceAlerts) — cf. SF-155-04-C.
- Aucune extension pour les champs OQTF FRANCE (`dateNotificationOqtf`, `motifOqtfCode`, `placementCra`, `dateHeureNotificationOqtf`, `motifSansDelaiCode`) — cf. SF-155-04-00-BE-immig-FR.
- Aucune extension pour le record `TravailExtractedData` — déjà traité par SF-155-04-00-BE-travail (mergée #518).
- Aucune modification de la logique de calcul du calculateur `Annexe13BeCalculator` (codes, formules, délais CCE) — inchangée.
- Aucune modification du contrat API du POST Annexe 13 BE — la `Annexe13BeRequest` reste telle quelle.
- Aucune migration Liquibase (sérialisation JSON dans `case_analysis.result`).

---

## Valeurs initiales

| Champ | Valeur initiale (null-safe) | Règle |
|-------|----------------------------|-------|
| `dateNotificationAnnexe13` | `null` | Rempli par IA uniquement si l'annexe 13 / OQT est notifiée et présente au dossier (date de notification lisible). |
| `delaiDepartImposeJours` | `null` | Rempli uniquement si délai explicitement fixé par l'OE (texte "Vous devez quitter le territoire dans X jours"). Valeurs typiques : 0 (sans délai / urgence), 7 (délai réduit), 30 (délai standard). |
| `motifOqtCodeBe` | `null` | Rempli uniquement si le motif invoqué par l'OE est clairement identifiable dans l'annexe 13 (art. invoqué, formulation standardisée). |
| `transfertImminentDetected` | `null` | `true` uniquement si indices factuels clairs (placement en centre fermé, notification d'un vol retour, escorte annoncée). `false` si explicitement pas de placement. `null` par défaut (la plupart des dossiers). |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `date_notification_annexe13` | Non | — | YYYY-MM-DD (texte conservé tel quel, cohérent avec les autres dates du record) | Non | aucune |
| `delai_depart_impose_jours` | Non | — | entier ≥ 0 | Non | `null` si négatif ou non numérique (helper `nonNegativeIntOrNull` partagé avec SF-155-04-00-BE-travail). |
| `motif_oqt_code_be` | Non | — | `SEJOUR_IRREGULIER_ART_7`, `REFUS_SEJOUR_APRES_DEMANDE`, `FIN_SEJOUR_REGULIER`, `AUTRE` | Non | Normalisation upper-case via `normalizeEnumCode(..., MOTIFS_OQT_BE_CODES)` ; valeur hors liste → `null` (fail-open). |
| `transfert_imminent_detected` | Non | — | boolean (ou string "true"/"false" normalisé) | Non | Via `booleanOrNull` existant (tolère string-coerce). |

Notes :
- Les 4 valeurs de `motif_oqt_code_be` sont **exactement alignées** sur `Annexe13BeCalculator.MOTIFS_VALIDES` (art. 7 Loi 15/12/1980) et le type `MotifOqt` frontend. Toute divergence briserait le pré-fill IA — l'IA doit produire ce que le consommateur sait lire.
- Dossiers FRANCE : les 4 champs restent `null` (les champs OQTF FR — `motifOqtfCode`, `dateNotificationOqtf`, etc. — seront ajoutés par SF-155-04-00-BE-immig-FR, distincts en code et en droit).
- Le helper `nonNegativeIntOrNull` et `normalizeEnumCode` sont déjà présents dans `CaseAnalysisResponse.java` depuis SF-155-04-00-BE-travail — on les réutilise tels quels (pas de duplication).

---

## Technique

### Endpoint(s)

Aucun endpoint nouveau. SF backend pure qui étend un record + un prompt + une méthode de parsing. Les endpoints d'analyse existants (`POST /api/v1/case-files/{id}/analyze`, `GET /api/v1/case-files/{id}/analysis`) incluent automatiquement les nouveaux champs dans leur réponse JSON via le record étendu.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_analysis` | Aucune (colonne `result` = JSON) | Le JSON sérialisé contient déjà un objet libre côté immigration — l'ajout de 4 sous-champs est transparent pour la DB. |

### Migration Liquibase

- [ ] Oui — n/a
- [x] Non applicable — persistance JSON

### Composants Angular

Aucun. La modification du DTO `case-analysis.model.ts` est un ajout de champs optional ; aucun composant existant n'est tenu de les consommer (SF-155-04-C s'en chargera).

### Référentiel métier

Pas de modification de `legal_referentials` — les codes `MOTIFS_OQT_BE_CODES` restent Java-only (pas de table de lookup — cohérent avec `IMMIGRATION_TITLE_CODES`, `IMMIGRATION_RECOURS_CODES`, `MOTIFS_NULLITE_CODES`). Les 4 codes sont déjà exposés par `Annexe13BeCalculator.MOTIFS_VALIDES` (source unique métier).

---

## Plan de test

### Tests unitaires

- [ ] `CaseAnalysisResponseTest.from_immigrationExtractedData_annexe13Be_parsed()` — fixture nominal BE avec les 4 champs (date + delai=30 + motif SEJOUR_IRREGULIER_ART_7 + transfert false) → record complet.
- [ ] `CaseAnalysisResponseTest.from_immigrationExtractedData_motifOqtBe_upperCase()` — fixture `motif_oqt_code_be: "sejour_irregulier_art_7"` → normalisé en upper-case accepté.
- [ ] `CaseAnalysisResponseTest.from_immigrationExtractedData_motifOqtBe_invalide_returnsNull()` — fixture `motif_oqt_code_be: "VALEUR_INCONNUE"` → champ `null`, autres champs préservés.
- [ ] `CaseAnalysisResponseTest.from_immigrationExtractedData_delaiDepart_zero_kept()` — delai=0 (cas OQT sans délai) doit être conservé (≥ 0).
- [ ] `CaseAnalysisResponseTest.from_immigrationExtractedData_delaiDepart_negative_null()` — delai=-5 → `null`.
- [ ] `CaseAnalysisResponseTest.from_immigrationExtractedData_transfertImminent_stringTrue_parsed()` — `transfert_imminent_detected: "true"` → `Boolean.TRUE`.
- [ ] `CaseAnalysisResponseTest.from_immigrationExtractedData_annexe13Be_dateOnly_parsed()` — fixture avec seulement `date_notification_annexe13` → champ rempli, autres BE `null`.
- [ ] `CaseAnalysisResponseTest.from_immigrationExtractedData_annexe13Be_malformedDelai_gracefulNull()` — `delai_depart_impose_jours: "trente"` → `null`, autres champs intacts.
- [ ] `CaseAnalysisResponseTest.from_immigrationExtractedData_annexe13Be_legacyFixture_retrocompat()` — fixture pré-SF-155 sans les 4 champs → record Java créé avec les 4 champs `null`, champs historiques (type_titre_sejour_code, type_recours_code, etc.) intacts.
- [ ] `CaseAnalysisResponseTest.from_immigrationExtractedData_annexe13Be_allFieldsBeAbsent_otherFieldsStillWorking()` — fixture immigration FR typique (pas de champs BE) → les 4 champs BE sont `null`, l'existant fonctionne.
- [ ] `LegalDomainPromptBuilderTest.domainSpecificInstruction_immigration_mentionsAnnexe13BeFields()` — le texte du prompt contient les 4 noms de champs + les 4 valeurs enum BE + mention "FRANCE" (règle null) (sentinelle anti-régression).
- [ ] `EnrichedAnalysisServiceTest.systemPrompt_preservesAnnexe13BeFields()` — la directive d'enrichi mentionne `immigration_extracted_data` et les 4 nouveaux champs BE.

### Tests d'intégration

- [ ] Non requis pour cette SF (pas de nouvel endpoint, pas de flow end-to-end nouveau). Les intégrations consommatrices (`Annexe13BeControllerIT`) ne dépendent pas du record IA — elles reçoivent un `Annexe13BeRequest` du frontend.

### Isolation workspace

- [x] Non applicable — la SF modifie un record DTO et un prompt système, aucune query ni accès base. L'isolation workspace est garantie par les endpoints d'analyse existants (inchangés).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — SF backend pure, extension de record + prompt + parsing.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| Reconstruction `new ImmigrationExtractedData(...)` dans `CaseAnalysisResponse.from()` (ligne ~407) | Constructeur 9-args → garanti par constructeur rétrocompat | Tests existants `from_immigrationWithMariageTrigger_inferChecklistCSTVPFConjointFR` et `from_immigrationTypeTitreCode_upperCase` doivent rester verts |
| `Annexe13BeCalculator` / `Annexe13BeService` / `Annexe13BeController` | Aucun — ne consomment pas `ImmigrationExtractedData`, reçoivent leur propre `Annexe13BeRequest` | `Annexe13BeCalculatorTest` + `Annexe13BeControllerIT` doivent rester verts |
| `EnrichedAnalysisService` prompt template | Directive immigration ajoutée | Tests existants `buildEnrichedPrompt_*` doivent rester verts ; nouveau test sentinelle ajouté |
| DTO frontend `case-analysis.model.ts` | 4 nouveaux champs optional ajoutés | `tsc --noEmit` vert — pas de régression type-check |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — la SF ne touche ni auth, ni workspace switch, ni navigation.

---

## Dépendances

### Subfeatures bloquantes

- Aucune. Cette SF est au palier 1 et peut partir en parallèle avec `SF-155-04-00-BE-immig-FR` (isolation par position des champs + sections du prompt + marqueurs).

### Subfeature parallèle (coordination)

- `SF-155-04-00-BE-immig-FR` touche les **mêmes 3 fichiers backend** (`CaseAnalysisResponse.java`, `LegalDomainPromptBuilder.java`, `EnrichedAnalysisService.java`) et le **même DTO frontend**.
- **Règles de coordination** (pas de coordination live, assumées par les deux SF) :
  1. Record `ImmigrationExtractedData` — BE ajoute ses 4 champs **à la fin**, après `inferredChecklistType`. FR ajoutera ses champs avant (ou à un autre endroit convenu). Le rebase du second mergeur sera mécanique (ordre d'arguments à réaligner dans le constructeur `extractImmigrationData()`).
  2. Prompt `IMMIGRATION_INSTRUCTION` — BE ajoute sa section **juste avant** `trigger_events`. FR ajoute la sienne ailleurs. Les deux sections sont marquées par des commentaires SF explicites.
  3. `extractImmigrationData()` — BE passe ses 4 champs **à la fin** de l'appel au constructeur. FR passe les siens avant. Rebase mécanique.
  4. DTO frontend — BE ajoute ses 4 champs **à la fin** de l'interface. FR ajoute les siens avant.

### Subfeatures débloquées par celle-ci

- `SF-155-04-C` (annexe13-be-section frontend) — dépend de `dateNotificationAnnexe13`, `delaiDepartImposeJours`, `motifOqtCodeBe`, `transfertImminentDetected`.

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` tranchée ou impactée.

---

## Notes et décisions

- **Choix 1** — **Alignement enum sur le calculateur, pas sur la proposition initiale du brief**. Le brief proposait des codes (`SEJOUR_IRREGULIER`, `FIN_SEJOUR_LEGAL`, `CONDAMNATION_GRAVE`, `MENACE_ORDRE_PUBLIC`, `FRAUDE`, `REFUS_PROTECTION`). Le calculateur `Annexe13BeCalculator` existant (F-IM-08, mergé) et le frontend `annexe13-be.model.ts` acceptent 4 codes : `SEJOUR_IRREGULIER_ART_7`, `REFUS_SEJOUR_APRES_DEMANDE`, `FIN_SEJOUR_REGULIER`, `AUTRE`. **Décision : aligner l'IA sur ces 4 codes** — toute valeur produite par l'IA doit être consommable par le composant downstream. Produire des codes non consommés aurait cassé le pré-fill en silence. Le brief invite explicitement à confirmer en lisant le composant ("confirme-la si tu trouves mieux dans le composant").
- **Choix 2** — pas de Java enum (String), cohérent avec les autres champs du record (`typeTitreSejourCode`, `typeRecoursCode`, `motifNullitePressenti`). Fail-open garanti par `normalizeEnumCode`.
- **Choix 3** — un seul constructeur de rétrocompat 9-args (pas d'empilement de 2+ signatures comme pour `TravailExtractedData` qui empile 9/17/18). La raison : le record immigration n'a qu'une évolution principale dans son histoire (9-args est la forme "post-SF-IM-01-04"). Les anciennes signatures 4/6/8-args déjà présentes restent inchangées — elles chaîneront vers le nouveau constructeur 9-args ou directement le canonique 13-args.
- **Choix 4** — directive de préservation **dédiée immigration** ajoutée dans `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE` (pattern symétrique à celle de `travail_extracted_data` ajoutée par SF-155-04-00-BE-travail). La baseline générique (`type_titre_sejour_code`, `type_procedure_detectee`, etc. lignes 70-74 du template) couvre les champs de classification mais ne mentionne pas les 4 nouveaux champs factuels BE. Sans directive dédiée, ils risquent d'être omis lors de la ré-analyse enrichie.
- **Choix 5** — le marqueur `SF-155-04-00-BE-immig-BE` est placé en commentaire Java/prompt pour rendre le rebase de la SF jumelle FR mécanique. Les 4 champs BE sont **à la fin** de chaque ajout (record, appel constructeur, DTO frontend) — la SF FR inserte ses champs avant, rebase direct sans conflit sémantique.
- **Choix 6** — ne pas toucher au constructeur de reconstruction post-inference dans `from()` (ligne ~407). Grâce au constructeur rétrocompat 9-args, l'appel `new ImmigrationExtractedData(expiration, titre, procedure, depot, code, ue, recours, notif, inferred)` continue de fonctionner — les 4 nouveaux champs BE sont dans ce cas initialisés à `null` (les champs BE ne sont pas recalculés après `inferChecklistType`, ce qui est le comportement voulu : si l'IA n'a pas détecté d'Annexe 13, `null` reste la valeur finale).
