# Mini-spec — F-155 / SF-155-04-00-BE-immig-FR

## Identifiant

`F-155 / SF-155-04-00-BE-immig-FR`

## Feature parente

`F-155` — Audit cohérence composants décisionnels frontend + template canonique

## Statut

`draft`

## Date de création

2026-04-24

## Branche Git

`feat/SF-155-04-00-BE-immig-FR`

---

## Objectif

Étendre le record backend `ImmigrationExtractedData`, la section immigration du prompt système `IMMIGRATION_INSTRUCTION` et la méthode de parsing `extractImmigrationData()` avec 5 champs supplémentaires **France uniquement** nécessaires au pré-remplissage IA des 2 composants décisionnels immigration FR (`oqtf-avec-delai-section` F-IM-08-02, `oqtf-sans-delai-section` F-IM-08-04), sans consommer ces champs côté UI (les SFs frontend SF-155-04-B1/B2 s'en chargeront).

---

## Comportement attendu

### Cas nominal

1. L'IA reçoit un dossier immigration FR. Le prompt système `IMMIGRATION_INSTRUCTION` lui demande maintenant de remplir 5 champs supplémentaires :
   - `date_notification_oqtf` (date YYYY-MM-DD) pour dossiers **OQTF avec délai** (F-IM-08-02).
   - `motif_oqtf_code` (code enum texte) pour dossiers **OQTF avec délai**.
   - `recours_forme_detected` (objet `{reponse, justification}`) pour les deux variantes OQTF — indicateur d'existence d'un recours déjà introduit.
   - `date_heure_notification_oqtf_sans_delai` (datetime ISO `YYYY-MM-DDTHH:mm` ou `YYYY-MM-DDTHH:mm:ss`) pour dossiers **OQTF sans délai** (F-IM-08-04, urgence 48h).
   - `placement_cra_detected` (boolean) pour dossiers **OQTF sans délai**.
2. L'IA renvoie son JSON habituel. `extractImmigrationData()` parse les 5 nouveaux champs (null-safe).
3. Le record Java `ImmigrationExtractedData` est étendu (9 → 14 arguments) avec constructeur rétrocompat pour la signature 9-args actuelle (SF-IM-01-04).
4. `EnrichedAnalysisService` préserve les 5 nouveaux champs lors de la ré-analyse enrichie (aucune directive explicite immigration n'existe aujourd'hui dans le prompt enrichi — voir section « Notes et décisions » : la préservation repose sur la règle générale « tous les champs extractés précédemment doivent être conservés » portée par la section baseline). Aucune modification nécessaire de `EnrichedAnalysisService` dans cette SF.
5. Le DTO frontend `ImmigrationExtractedData` (`case-analysis.model.ts`) expose les 5 nouveaux champs en optional (`?: | null`), prêts à être consommés par B1/B2.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Dossier hors droit de l'immigration | `immigrationExtractedData` reste `null`, les 5 nouveaux champs n'apparaissent pas | 200 (analyse OK) |
| JSON IA malformé sur un des 5 champs (ex. type incorrect) | Le champ concerné est `null`, les autres champs de `ImmigrationExtractedData` sont préservés (fail-open) | 200 |
| Dossier immigration BE | Les 5 champs valent tous `null` (le prompt précise "FR uniquement") | 200 |
| Dossier immigration FR sans détection OQTF possible | Les 5 champs valent tous `null` (cas normal des dossiers non OQTF) | 200 |
| `motif_oqtf_code` renvoyé hors liste (ex. "AUTRE_CHOSE") | `motifOqtfCode` = `null` (fail-open, whitelist statique) | 200 |
| `date_heure_notification_oqtf_sans_delai` format invalide (ex. "15/03/2026") | Champ = `null` (validation regex permissive) | 200 |
| Justification `recours_forme_detected` > 500 caractères | Troncature à 500 caractères (même règle que `licenciement_validity_detection`) | 200 |
| Anciennes fixtures JSON (sans ces champs) | Rétrocompat totale — tous les champs historiques restent lisibles, les 5 nouveaux champs sont `null` | 200 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : scanné. Les outils antérieurs immigration (F-IM-05 Titre séjour, F-IM-06 Recours, F-IM-07 Droit au travail) ont étendu `ImmigrationExtractedData` au fil des SFs (2 constructeurs rétrocompat existent déjà). Cette SF applique rétroactivement la même pratique aux 2 outils OQTF FR du batch 2026-04-24 (F-IM-08-02/04). Les outils belges (F-IM-14 9bis/9ter/40bis/40ter, Annexe 13 F-IM-08-06) sont traités par la SF jumelle `SF-155-04-00-BE-immig-BE`.
- [x] **Autres pays** : l'instruction `IMMIGRATION_INSTRUCTION` est partagée FR/BE. Les 5 nouveaux champs concernent **FR seul** — le prompt doit le préciser explicitement ("null pour dossiers BE"). Les champs Annexe 13 BE sont dans la SF jumelle `SF-155-04-00-BE-immig-BE`.
- [x] **Autres domaines** : DROIT_DU_TRAVAIL (travail) traité par `SF-155-04-00-BE-travail` (PR #518). DROIT_FAMILLE non concerné par F-IM-08.
- [x] **Autres UI patterns** : pas de nouveau pattern UI (SF backend pure). Les patterns frontend (provenance, coherenceAlerts, CoherencePopoverTrigger) seront introduits dans B1/B2.
- [x] **Autres flows transversaux** : aucun. La SF ne touche ni auth, ni workspace, ni plans, ni routing, ni migration Liquibase.

### Niveaux de vérification

- [x] **Modèle TypeScript** — `frontend/src/app/core/models/case-analysis.model.ts` doit être étendu avec les 5 champs optional (contrat public pour B1/B2).
- [x] **Record / DTO backend** — `ImmigrationExtractedData` record étendu de 9 à 14 arguments.
- [x] **Service / logique métier** — `extractImmigrationData()` parse les 5 nouveaux champs.
- [x] **Entité JPA + schéma DB** — aucun impact. `immigration_extracted_data` est sérialisé en JSON dans le champ `result` de `case_analysis`, pas de colonne dédiée, pas de migration Liquibase.
- [x] **Tests existants** — `CaseAnalysisResponseTest` couvre `extractImmigrationData()` avec fixtures JSON. Tests légers à ajouter sur les 5 fixtures.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Outils backend antérieurs immigration (F-IM-01 à F-IM-07) | Non | Déjà IA-compliant, le constructeur 9-args rétrocompat garantit la non-régression. |
| Prompt belge (section `IMMIGRATION_INSTRUCTION` partagée) | Partiel | Les 5 champs sont mentionnés mais la règle "null pour dossiers BE" est explicite dans le prompt. |
| Record `TravailExtractedData` | Non | Traité par PR #518 (SF-155-04-00-BE-travail) déjà en cours de merge. |
| Annexe 13 BE (F-IM-08-06) | Non | Traité par SF parallèle `SF-155-04-00-BE-immig-BE`. |
| Entité `CaseAnalysis` / table `case_analysis` | Non applicable | La sérialisation JSON est agnostique au nombre de champs. |
| `ImmigrationTriggerEvent` extractor, `ImmigrationStrategyScenario` extractor | Non applicable | Ils ne partagent pas `ImmigrationExtractedData` ; pas d'impact. |
| Services consommateurs du record (`CaseAnalysisResponse.from` appelants dans controllers) | Oui | Intégré — tous accèdent via méthodes nommées du record, tolérant à l'extension. |
| `OqtfAvecDelaiService`, `OqtfSansDelaiService` (calculateurs métier F-IM-08) | Oui | Non touchés (hors scope — ils consomment une `Request` utilisateur, pas `ImmigrationExtractedData`). |
| Cas historiques de fixtures JSON legacy | Oui | Intégré — test explicite de rétrocompat via fixture signature 9-args. |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (record + prompt + parsing + DTO frontend + tests).
- [x] Subfeature(s) parallèle(s) créée(s) — `SF-155-04-00-BE-immig-BE` pour les 5 champs Annexe 13 BE, `SF-155-04-00-BE-travail` (PR #518) pour les 5 champs travail FR.
- [x] Backlog — SF-155-04-B1 (frontend OQTF avec délai) et SF-155-04-B2 (frontend OQTF sans délai) ouverts après merge backend.
- [x] Non applicable aux autres cibles — justifications ci-dessus.

### Cas spécifique : nouvelle feature d'outil décisionnel

N/A — cette SF ne crée pas d'outil décisionnel, elle étend uniquement le contrat IA qui alimentera les outils existants F-IM-08-02 et F-IM-08-04. La mise à niveau pré-fill IA + F-IA-03 des outils proprement dits est dans les SFs B1/B2 frontend.

### Cas spécifique : nouveau pattern UI ou service partagé

N/A — aucun nouveau pattern UI, service, endpoint ou DTO partagé. Extension d'un record existant (additive) et d'un prompt existant.

---

## Critères d'acceptation

- [ ] Le record Java `ImmigrationExtractedData` expose 5 nouveaux champs nullable : `dateNotificationOqtf` (String YYYY-MM-DD), `motifOqtfCode` (String enum), `recoursFormeDetected` (DetectedAnswer : reponse + justification), `dateHeureNotificationOqtfSansDelai` (String ISO datetime), `placementCraDetected` (Boolean).
- [ ] Le record conserve au moins un constructeur de rétrocompat pour la signature **9-args** actuelle (appel vers le constructeur canonique avec `null, null, null, null, null` sur les 5 nouveaux champs). Les 2 autres constructeurs historiques (4-args, 6-args) existaient déjà et doivent rester fonctionnels via chaînage.
- [ ] Le prompt `LegalDomainPromptBuilder.IMMIGRATION_INSTRUCTION` décrit explicitement les 5 nouveaux champs : valeurs enum autorisées pour `motif_oqtf_code`, format date/datetime, structure objet `{reponse, justification}` + règle "null pour dossiers belges".
- [ ] `extractImmigrationData()` parse les 5 nouveaux champs depuis le JSON IA, retourne `null` pour chaque champ absent ou malformé, tronque la justification de `recours_forme_detected` à 500 caractères.
- [ ] Le datetime `date_heure_notification_oqtf_sans_delai` est validé par une regex permissive qui accepte `YYYY-MM-DDTHH:mm` et `YYYY-MM-DDTHH:mm:ss` — formats invalides (ex. `15/03/2026 10:00`) → `null`.
- [ ] Le DTO frontend `ImmigrationExtractedData` dans `case-analysis.model.ts` expose les 5 nouveaux champs optional (`dateNotificationOqtf?: string | null`, etc.) ajoutés **à la fin** de l'interface.
- [ ] Tests unitaires couvrent : fixture OQTF avec délai (date + motif + recours remplis), fixture OQTF sans délai (datetime + placement CRA remplis), fixture motif enum invalide → null, fixture motif lowercase normalisé, fixture datetime invalide → null, fixture `placementCraDetected` en string-coerce ("true"/"false") → boolean, fixture `recoursFormeDetected` avec justification > 500 car → tronquée, fixture legacy (rétrocompat — tous les nouveaux champs valent `null`), fixture malformée (champ ignoré, autres champs OK).
- [ ] Tests sentinelles sur le prompt : `LegalDomainPromptBuilderTest` contient un test sentinelle vérifiant que `IMMIGRATION_INSTRUCTION` mentionne les 5 nouveaux noms de champs et les valeurs enum `motif_oqtf_code`.
- [ ] Les tests existants `CaseAnalysisResponseTest` (y compris U-41 à U-49 et SF-IM-01-06), `LegalDomainPromptBuilderTest`, `EnrichedAnalysisServiceTest`, et les tests des services consommateurs (`OqtfAvecDelaiServiceTest`, `OqtfSansDelaiServiceTest` si existants) restent verts.
- [ ] `./mvnw test` vert (backend complet), `tsc --noEmit` frontend vert.

---

## Périmètre

### Hors scope (explicite)

- Aucune consommation UI des 5 champs : les binding `aiData` côté `TOOL_REGISTRY.inputs(ctx)` pour F-IM-08-02 / F-IM-08-04 sont à faire dans SF-155-04-B1 et SF-155-04-B2 (frontend).
- Aucun pattern frontend (provenance, prefillFromAi, coherenceAlerts) — cf. SF-155-04-B1/B2.
- Aucune extension du record `TravailExtractedData` — cf. SF-155-04-00-BE-travail (PR #518).
- Aucune extension des champs **Annexe 13 BE** (F-IM-08-06) — cf. SF-155-04-00-BE-immig-BE (SF parallèle).
- Aucune modification de la logique de calcul des 2 calculateurs (`OqtfAvecDelaiService`, `OqtfSansDelaiService`) — endpoints et services métier inchangés.
- Aucune modification de `EnrichedAnalysisService` — la préservation des champs immigration se fait déjà via la règle baseline générique du mode enrichi ; aucune directive spécifique dédiée n'existe pour immigration aujourd'hui, pas de motif de l'ajouter dans cette SF (décision tracée ci-dessous).
- Aucune migration Liquibase (sérialisation JSON dans `case_analysis.result`).

---

## Valeurs initiales

| Champ | Valeur initiale (null-safe) | Règle |
|-------|----------------------------|-------|
| `dateNotificationOqtf` | `null` | Rempli par l'IA uniquement si pièce OQTF (arrêté préfectoral) présente et date lisible. |
| `motifOqtfCode` | `null` | Rempli par l'IA uniquement si le motif de l'OQTF est identifiable dans l'arrêté. Whitelist 5 valeurs (cf. Contraintes). |
| `recoursFormeDetected` | `null` | Rempli si pièces attestent qu'un recours a déjà été introduit ; `NON` si aucune trace ; `INCONNU` sinon. Justification obligatoire (phrase courte). |
| `dateHeureNotificationOqtfSansDelai` | `null` | Rempli depuis arrêté OQTF sans délai avec horodatage précis (important pour le décompte 48h). |
| `placementCraDetected` | `null` | Rempli si l'arrêté mentionne une décision de placement en centre de rétention administratif (CRA). |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `date_notification_oqtf` | Non | — | YYYY-MM-DD | Non | Texte brut (pas de validation regex dans cette SF — cohérent avec les champs date existants `date_expiration_titre`, `date_depot_procedure`). |
| `motif_oqtf_code` | Non | — | `REFUS_TITRE`, `EXPIRATION_TITRE`, `SEJOUR_IRREGULIER`, `RETRAIT_TITRE`, `AUTRE` | Non | Normalisation upper-case ; valeur hors liste → `null`. Liste alignée sur l'enum `MotifOqtf` du front (`frontend/src/app/core/models/oqtf-avec-delai.model.ts`). |
| `recours_forme_detected.reponse` | Non | — | `OUI`, `NON`, `INCONNU` | Non | Normalisation via `normalizeReponse()` existant. |
| `recours_forme_detected.justification` | Non | 500 caractères | texte libre | Non | Troncature 500 car (règle `MAX_JUSTIFICATION_LENGTH`). |
| `date_heure_notification_oqtf_sans_delai` | Non | — | ISO 8601 partiel : `YYYY-MM-DDTHH:mm` ou `YYYY-MM-DDTHH:mm:ss` | Non | Regex permissive : `^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$` — formats invalides → `null`. |
| `placement_cra_detected` | Non | — | boolean / "true" / "false" | Non | Via `booleanOrNull()` existant (fail-open). |

Notes :
- Les 5 valeurs de `motif_oqtf_code` sont celles acceptées par le formulaire OQTF avec délai (enum `MotifOqtf` frontend). Cela évite un mapping intermédiaire côté UI lors du pré-fill B1. Alignement vérifié dans `frontend/src/app/core/models/oqtf-avec-delai.model.ts` au 2026-04-24.
- **Pas de set séparé `MOTIFS_SANS_DELAI_FR_CODES`** : le champ `motif_oqtf_code` n'est rempli que pour F-IM-08-02 (avec délai). F-IM-08-04 (sans délai) utilise son propre enum `MotifSansDelai` (`RISQUE_FUITE`, `TROUBLE_ORDRE_PUBLIC`, `OQTF_PRECEDENTE_INEXECUTEE`, `AUTRE`), mais l'expérience montre que ce motif est peu fiablement détectable par l'IA (il implique une appréciation administrative complexe) — décision de ne pas l'extraire dans cette SF. Si un besoin remonte en B2, une SF complémentaire sera ouverte.
- Dossiers BE : les 5 champs restent `null` (les équivalents belges — Annexe 13 — seront portés par `SF-155-04-00-BE-immig-BE`).

---

## Technique

### Endpoint(s)

Aucun endpoint nouveau. SF backend pure qui étend un record + un prompt + une méthode de parsing. Les endpoints d'analyse existants (`POST /api/v1/case-files/{id}/analyze`, `GET /api/v1/case-files/{id}/analysis`) incluent automatiquement les nouveaux champs dans leur réponse JSON via le record étendu.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_analysis` | Aucune (colonne `result` = JSON) | Le JSON sérialisé contient déjà un champ libre contenant `immigration_extracted_data` — l'ajout de sous-champs est transparent pour la DB. |

### Migration Liquibase

- [ ] Oui — n/a
- [x] Non applicable — persistance JSON

### Composants Angular

Aucun. La modification du DTO `case-analysis.model.ts` est un ajout de champs optional ; aucun composant existant n'est tenu de les consommer.

### Référentiel métier

Pas de modification de `legal_referentials` — l'enum `MotifOqtf` utilisé côté calculateur (F-IM-08-02) reste Java/Typescript-only (pas de table de lookup). La liste de codes dans `IMMIGRATION_INSTRUCTION` reste en dur dans le prompt (cohérent avec `IMMIGRATION_TITLE_CODES`, `IMMIGRATION_RECOURS_CODES`).

### Coordination SF jumelle BE

Une SF parallèle `SF-155-04-00-BE-immig-BE` touche les mêmes 3 fichiers backend + le DTO frontend. Règles respectées dans cette SF pour minimiser les conflits de rebase :

1. **Record `ImmigrationExtractedData`** : les 5 champs FR sont ajoutés **à la fin** du record, après `inferredChecklistType`. Un nouveau constructeur de rétrocompat 9-args est ajouté à la fin de la liste existante. La SF BE viendra ajouter ses champs après les miens.
2. **Prompt `IMMIGRATION_INSTRUCTION`** : la section FR est ajoutée avec un marqueur de commentaire `SF-155-04-00-BE-immig-FR` pour l'identifier dans le diff.
3. **`extractImmigrationData()`** : l'appel au constructeur est étendu avec les 5 nouveaux champs FR à la fin. Les helpers privés `normalizeEnumCode`, `extractDetectedAnswer`, `validateIsoDateTime` sont ajoutés en zone "helpers privés".
4. **DTO frontend** : les 5 champs optional sont ajoutés **à la fin** de l'interface `ImmigrationExtractedData`.

Les 2 SFs sont indépendantes, peuvent merger dans n'importe quel ordre — celle qui arrive second aura un rebase mécanique (ajout à la fin = pas de conflit de texte attendu, sauf exactement sur la ligne de fermeture du record).

---

## Plan de test

### Tests unitaires

- [ ] `CaseAnalysisResponseTest.from_immigrationOqtfAvecDelai_parsesNewFields()` — fixture OQTF avec délai (date + motif + recours) → 3 champs remplis.
- [ ] `CaseAnalysisResponseTest.from_immigrationMotifOqtfCode_upperCase()` — fixture avec motif `"sejour_irregulier"` → normalisé à `"SEJOUR_IRREGULIER"`.
- [ ] `CaseAnalysisResponseTest.from_immigrationMotifOqtfCode_invalide_returnsNull()` — fixture avec motif `"AUTRE_CHOSE"` → `null` (autres champs présents pour que l'objet existe).
- [ ] `CaseAnalysisResponseTest.from_immigrationOqtfSansDelai_parsesNewFields()` — fixture OQTF sans délai (datetime + placement CRA) → 2 champs remplis.
- [ ] `CaseAnalysisResponseTest.from_immigrationOqtfSansDelai_datetimeInvalide_null()` — fixture avec `"15/03/2026 10:00"` → `dateHeureNotificationOqtfSansDelai` = `null`.
- [ ] `CaseAnalysisResponseTest.from_immigrationOqtfSansDelai_datetimeWithSeconds_accepted()` — fixture avec `"2026-03-15T10:30:45"` → accepté tel quel.
- [ ] `CaseAnalysisResponseTest.from_immigrationPlacementCraAsString_parsed()` — fixture `"placement_cra_detected": "true"` → `true` (via `booleanOrNull`).
- [ ] `CaseAnalysisResponseTest.from_immigrationRecoursFormeDetected_troncature500()` — fixture avec justification 600 car → tronquée à 500.
- [ ] `CaseAnalysisResponseTest.from_immigrationRecoursFormeDetected_reponseNormalized()` — fixture `{reponse: "oui", ...}` → normalisé `"OUI"`.
- [ ] `CaseAnalysisResponseTest.from_immigrationLegacyFixture_noNewFields_stillWorks()` — fixture SF-IM-01-04 sans les 5 nouveaux champs → record créé avec les 5 champs à `null`.
- [ ] `CaseAnalysisResponseTest.from_immigrationMalformedRecoursForme_gracefulNull()` — `"recours_forme_detected": 42` (non-object) → champ `null`, autres champs intacts.
- [ ] `LegalDomainPromptBuilderTest.immigrationPrompt_mentionsNouveauxChampsFR()` — le texte du prompt contient les 5 noms de champs + les 5 valeurs enum `motif_oqtf_code`.

### Tests d'intégration

- [ ] Pas de nouveau test d'intégration requis — la couverture d'intégration existante de `GET /api/v1/case-files/{id}/analysis` suffit. Les fixtures IA dans les ITs existants ne contiennent pas les 5 nouveaux champs → fallback `null` testé par rétrocompat unitaire.

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
| `ImmigrationPieceReferentiel.inferChecklistType` (appelé dans `CaseAnalysisResponse.from`) | Consomme `ImmigrationExtractedData.typeTitreSejourCode()` — inchangé, tolérant à l'extension | Test SF-IM-01-06 existant (`from_immigrationWithMariageTrigger_inferChecklistCSTVPFConjointFR`) doit rester vert |
| `CaseAnalysisResponse.from` (construction du record) | Création d'un nouvel objet `ImmigrationExtractedData` après inférence du `inferredChecklistType` — doit transférer les 5 nouveaux champs | Test explicite : fixture OQTF + trigger mariage → les 5 champs sont préservés après re-construction |
| DTO frontend `case-analysis.model.ts` | 5 nouveaux champs optional ajoutés | `tsc --noEmit` vert |
| `EnrichedAnalysisService` | Règle baseline générique "préserver les champs extraits" couvre déjà les 5 nouveaux champs sans directive dédiée | Aucun test dédié ajouté — non-régression des tests existants `EnrichedAnalysisServiceTest` |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — la SF ne touche ni auth, ni workspace switch, ni navigation.

---

## Dépendances

### Subfeatures bloquantes

- Aucune. Cette SF est indépendante et peut partir en parallèle avec :
  - `SF-155-04-00-BE-travail` (PR #518 en cours de merge).
  - `SF-155-04-00-BE-immig-BE` (SF parallèle pour les 5 champs Annexe 13 BE).

### Subfeatures débloquées par celle-ci

- SF-155-04-B1 (OQTF avec délai frontend) — dépend de `dateNotificationOqtf`, `motifOqtfCode`, `recoursFormeDetected`.
- SF-155-04-B2 (OQTF sans délai frontend) — dépend de `dateHeureNotificationOqtfSansDelai`, `placementCraDetected`, `recoursFormeDetected`.

### Questions ouvertes impactées

- [ ] Aucune question de `docs/OPEN_QUESTIONS.md` tranchée ou impactée.

---

## Notes et décisions

- **Choix 1 — Préservation via règle baseline générique (pas de directive dédiée `EnrichedAnalysisService`)** : le prompt enrichi contient déjà une section « RÈGLE CRITIQUE DE PRÉSERVATION BASELINE » qui stipule que « les champs de classification factuels extraits par la synthèse précédente sont la BASELINE à PRÉSERVER ». Les 5 nouveaux champs FR sont factuels (dates, motifs, présence/absence de recours) et tombent naturellement sous cette règle. Contrairement au choix fait dans `SF-155-04-00-BE-travail` (qui ajoutait une directive explicite `travail_extracted_data`), pas de précédent d'instruction explicite pour `immigration_extracted_data` dans `SYSTEM_PROMPT_TEMPLATE` — on reste cohérent avec l'existant et on ne mélange pas les conventions dans cette SF pour ne pas créer de conflit inutile avec la SF jumelle BE. Si une régression est observée plus tard, une SF de durcissement pourra ajouter la directive.
- **Choix 2 — Alignement enum `motif_oqtf_code` sur `MotifOqtf` frontend** : la liste {`REFUS_TITRE`, `EXPIRATION_TITRE`, `SEJOUR_IRREGULIER`, `RETRAIT_TITRE`, `AUTRE`} est celle de `frontend/src/app/core/models/oqtf-avec-delai.model.ts`. Cela permet un pré-fill direct sans mapping côté UI. La proposition initiale de la consigne (codes CESEDA `L.611-1 1°/3°/4°/5°/6°/7°`) est plus fine juridiquement mais nécessiterait un layer de traduction à chaque usage — anti-pattern pour un pré-fill simple.
- **Choix 3 — Pas d'enum pour OQTF sans délai** : `motif_sans_delai` (enum `MotifSansDelai` frontend : `RISQUE_FUITE`, `TROUBLE_ORDRE_PUBLIC`, `OQTF_PRECEDENTE_INEXECUTEE`, `AUTRE`) n'est pas extrait ici car le motif implique une qualification administrative qui mélange appréciation subjective et texte réglementaire — peu fiable en détection automatique pour la V1. Si un besoin remonte en SF-155-04-B2, une SF complémentaire sera ouverte.
- **Choix 4 — Nested record `DetectedAnswer` réutilisé** : `recoursFormeDetected` réutilise le nested record `DetectedAnswer` déjà défini dans `CaseAnalysisResponse` (cohérent avec `licenciement_validity_detection`, `rupture_conv_validity_detection`, `divorce_consentement_validity_detection`). Pas de nouveau record, pas de duplication.
- **Choix 5 — Validation regex datetime permissive** : `^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$` accepte les 2 formats ISO les plus fréquents (`YYYY-MM-DDTHH:mm` et `YYYY-MM-DDTHH:mm:ss`). Pas de `Z` ni de fuseau horaire — cohérent avec les datetime-local HTML5 utilisés dans `oqtf-sans-delai-section.component.html`. Formats invalides → `null` (fail-open).
- **Choix 6 — Rétrocompat du constructeur** : la signature actuelle 9-args (SF-IM-01-04) est conservée via un nouveau constructeur `public ImmigrationExtractedData(...9 args...)` qui délègue au canonique 14-args avec `null` sur les 5 derniers. Aucun breaking change pour les appelants internes (`CaseAnalysisResponse.from` ligne ~359 qui reconstruit le record après inférence du `inferredChecklistType` — voir impact ci-dessus : la reconstruction doit aussi passer les 5 nouveaux champs sinon ils seront perdus).
- **Choix 7 — Pas de helper `validateIsoDateTime` exporté** : la validation regex datetime reste interne à `extractImmigrationData()` (method-local `Pattern` statique) pour éviter de multiplier les helpers globaux. Cohérent avec la politique fail-open existante.
- **Choix 8 — Réutilisation des helpers `normalizeEnumCode` / `extractDetectedAnswer` de SF-155-04-00-BE-travail** : ces helpers privés statiques ont été introduits par `SF-155-04-00-BE-travail` et sont déjà présents sur master depuis le merge #518 (commit `424b348`). Cette SF les réutilise tels quels, ce qui évite toute duplication et garantit la cohérence de la normalisation enum / DetectedAnswer entre domaines travail et immigration.
- **Choix 9 — Pas de constructeur rétrocompat au-delà du 9-args** : seule la signature 9-args courante (SF-IM-01-04) est conservée. Les 2 constructeurs historiques plus anciens (4-args, 6-args) préexistants sont chaînés vers le 9-args via le compilateur Java et ne nécessitent pas de modification dans cette SF. En pratique les seuls appelants de `ImmigrationExtractedData` dans le code sont `extractImmigrationData()` et la reconstruction après inférence `inferredChecklistType` (ligne ~359) — les deux seront mis à jour dans cette SF.
