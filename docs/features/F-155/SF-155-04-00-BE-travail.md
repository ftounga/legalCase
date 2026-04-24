# Mini-spec — F-155 / SF-155-04-00-BE-travail

## Identifiant

`F-155 / SF-155-04-00-BE-travail`

## Feature parente

`F-155` — Audit cohérence composants décisionnels frontend + template canonique

## Statut

`draft`

## Date de création

2026-04-24

## Branche Git

`feat/SF-155-04-00-BE-travail`

---

## Objectif

Étendre le record backend `TravailExtractedData`, le prompt système `TRAVAIL_INSTRUCTION` et la méthode de parsing `extractTravailData()` avec 5 champs supplémentaires nécessaires au pré-remplissage IA des 3 composants décisionnels droit du travail (harcèlement, inaptitude, heures sup), sans consommer ces champs côté UI (les SFs frontend SF-155-04-A1/A2/A3 s'en chargeront).

---

## Comportement attendu

### Cas nominal

1. L'IA reçoit un dossier droit du travail. Le prompt système `TRAVAIL_INSTRUCTION` lui demande maintenant de remplir 5 champs supplémentaires dans `travail_extracted_data` :
   - `motif_nullite_pressenti` (code enum texte) pour dossiers de harcèlement / discrimination.
   - `origine_inaptitude_pressentie` (code enum texte) pour dossiers d'inaptitude.
   - `avis_medecin_travail_date` (date YYYY-MM-DD) pour dossiers d'inaptitude.
   - `reclassement_respecte_detected` (objet `{reponse, justification}`) pour dossiers d'inaptitude.
   - `heures_sup_mentionnees` (objet agrégé) pour dossiers de contentieux heures sup.
2. L'IA renvoie son JSON habituel. `extractTravailData()` parse les 5 nouveaux champs (null-safe).
3. Le record Java `TravailExtractedData` est étendu (18 → 23 arguments) avec constructeur rétrocompat pour toutes les anciennes signatures.
4. `EnrichedAnalysisService` préserve les 5 nouveaux champs lors de la ré-analyse enrichie (ajout à la consigne "Le champ `travail_extracted_data` doit être préservé et actualisé").
5. Le DTO frontend `TravailExtractedData` (`case-analysis.model.ts`) expose les 5 nouveaux champs en optional (`?: | null`), prêts à être consommés par A1/A2/A3.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Dossier hors droit du travail | `travailExtractedData` reste `null`, les 5 nouveaux champs n'apparaissent pas | 200 (analyse OK) |
| JSON IA malformé sur un des 5 champs (ex. type incorrect) | Le champ concerné est `null`, les autres champs de `TravailExtractedData` sont préservés (fail-open) | 200 |
| Dossier droit du travail sans détection possible | Les 5 champs valent tous `null` dans la réponse (cas normal pour la plupart des dossiers) | 200 |
| Justification `reclassement_respecte_detected` > 500 caractères | Troncature à 500 caractères (même règle que `licenciement_validity_detection`) | 200 |
| Anciennes fixtures JSON (sans ces champs) | Rétrocompat totale — tous les champs historiques restent lisibles | 200 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : scanné — voir section 8.2 de `audit-prefill-ia-2026-04-24.md`. Les outils antérieurs (F-DT-07/08/09/10, F-IM-05/06/07, F-FA-05/06) ont tous étendu leur DTO backend en même temps que le composant frontend. Cette SF applique rétroactivement la même pratique aux 3 outils travail du batch 2026-04-24.
- [x] **Autres pays** : la convention `is_system` du prompt travail est partagée FR/BE (les instructions spécifiques pays sont inlinées). Les 5 nouveaux champs concernent **FR seul** (harcèlement/inaptitude/heures sup sont des concepts FR — en BE, `BE_INDEMNITE_MANIFESTE` couvre déjà le "manifestement déraisonnable"). Le prompt mentionne que ces champs peuvent rester `null` pour les dossiers BE.
- [x] **Autres domaines** : DROIT_FAMILLE / DROIT_IMMIGRATION non concernés (outils décisionnels famille et immigration consommés par d'autres records, étendus par SF-155-04-00-BE-immig-FR et SF-155-04-00-BE-immig-BE).
- [x] **Autres UI patterns** : pas de nouveau pattern UI (SF backend pure). Les patterns frontend (provenance, coherenceAlerts, CoherencePopoverTrigger) seront introduits dans A1 (template canonique).
- [x] **Autres flows transversaux** : aucun. La SF ne touche ni auth, ni workspace, ni plans, ni routing.

### Niveaux de vérification

- [x] **Modèle TypeScript** — `frontend/src/app/core/models/case-analysis.model.ts` doit être étendu avec les 5 champs optional (contrat public pour A1/A2/A3).
- [x] **Record / DTO backend** — `TravailExtractedData` record étendu de 18 à 23 arguments.
- [x] **Service / logique métier** — `extractTravailData()` parse les 5 nouveaux champs ; `EnrichedAnalysisService` les préserve lors de la ré-analyse.
- [x] **Entité JPA + schéma DB** — aucun impact. `travail_extracted_data` est sérialisé en JSON dans le champ `result` de `case_analysis`, pas de colonne dédiée, pas de migration Liquibase.
- [x] **Tests existants** — `CaseAnalysisResponseTest` couvre `extractTravailData()` avec fixtures JSON. Il faut ajouter 3 fixtures (harcèlement, inaptitude, heures sup) + un test de rétrocompat sur une fixture historique.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Outils backend antérieurs (F-DT-07 à F-DT-10) | Non | Déjà IA-compliant, pas de régression attendue — tests existants doivent rester verts. |
| Prompt système belge | Partiel | Les 5 champs sont mentionnés mais resteront `null` pour dossiers BE (noté dans le prompt). |
| Record `ImmigrationExtractedData` | Non | Traité par SF-155-04-00-BE-immig-FR et SF-155-04-00-BE-immig-BE. |
| Entité `CaseAnalysis` / table `case_analysis` | Non applicable | La sérialisation JSON de `travail_extracted_data` dans `result` est agnostique au nombre de champs. |
| `PrudhomeFicheService`, `TribunalTravailFicheService` (consommateurs actuels de `TravailExtractedData`) | Oui | Intégré dans la SF — vérifier que l'ajout de champs à un `record` Java ne casse pas ces services (accès via méthodes nommées, tolérant à l'extension). |
| Cas historiques de fixtures JSON legacy | Oui | Intégré dans la SF — test explicite de rétrocompat sur une fixture pré-SF-130-01. |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (record + prompt + parsing + DTO frontend + tests).
- [ ] Subfeature(s) parallèle(s) créée(s) — n/a.
- [x] Backlog pour les cibles non prioritaires — SF-155-04-00-BE-immig-FR et SF-155-04-00-BE-immig-BE à créer en parallèle (indépendantes, domaine immigration).
- [x] Non applicable aux autres cibles — justifications ci-dessus.

---

## Critères d'acceptation

- [ ] Le record Java `TravailExtractedData` expose 5 nouveaux champs nullable : `motifNullitePressenti` (String enum), `origineInaptitudePressentie` (String enum), `avisMedecinTravailDate` (String YYYY-MM-DD), `reclassementRespecteDetected` (DetectedAnswer : reponse + justification), `heuresSupMentionneesDansDossier` (objet `HeuresSupMentionnees`).
- [ ] Au moins 3 constructeurs de rétrocompat sont conservés (9, 17, 18 arguments → appel vers le constructeur canonique avec `null, null, null, null, null` sur les 5 nouveaux champs).
- [ ] Le prompt `LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION` décrit explicitement les 5 nouveaux champs : valeurs enum autorisées, format date, structure objet `{reponse, justification}`, structure objet `heures_sup_mentionnees` + règle "null pour dossiers BE si concept non applicable".
- [ ] `extractTravailData()` parse les 5 nouveaux champs depuis `travail_extracted_data` du JSON IA, retourne `null` pour chaque champ absent ou malformé, tronque la justification de `reclassement_respecte_detected` à 500 caractères.
- [ ] `EnrichedAnalysisService` mentionne les 5 nouveaux champs dans la consigne de préservation lors de la ré-analyse enrichie.
- [ ] Le DTO frontend `TravailExtractedData` dans `case-analysis.model.ts` expose les 5 nouveaux champs optional (`motifNullitePressenti?: string | null`, etc.) avec le type `HeuresSupMentionnees` déclaré si pertinent.
- [ ] Tests unitaires couvrent : fixture harcèlement (motif rempli), fixture inaptitude (3 champs remplis), fixture heures sup (objet rempli), fixture legacy (rétrocompat — tous les nouveaux champs valent `null`), fixture malformée (champ ignoré, autres champs OK), fixture justification > 500 car (troncature).
- [ ] Les tests existants `CaseAnalysisResponseTest`, `PrudhomeFicheServiceTest`, `TribunalTravailFicheServiceTest`, `EnrichedAnalysisServiceTest` restent verts.
- [ ] `./mvnw test` vert, `./mvnw clean package -DskipTests` vert, `npm run build` frontend vert (DTO étendu doit compiler).

---

## Périmètre

### Hors scope (explicite)

- Aucune consommation UI de ces 5 champs : les binding `aiData` côté `TOOL_REGISTRY.inputs(ctx)` pour F-DT-11 / F-DT-15 / F-DT-19 sont à faire dans SF-155-04-A1/A2/A3.
- Aucun pattern frontend (provenance, prefillFromAi, coherenceAlerts) — cf. SF-155-04-A1.
- Aucune extension du record `ImmigrationExtractedData` — cf. SF-155-04-00-BE-immig-FR et SF-155-04-00-BE-immig-BE.
- Aucune modification de la logique de calcul des 3 calculateurs (harcèlement/inaptitude/heures-sup) — endpoints et services métier inchangés.
- Aucune migration Liquibase (sérialisation JSON dans `case_analysis.result`).

---

## Valeurs initiales

| Champ | Valeur initiale (null-safe) | Règle |
|-------|----------------------------|-------|
| `motifNullitePressenti` | `null` | Rempli par IA uniquement si indices clairs (discrimination documentée, témoignages de harcèlement, etc.) dans les pièces. |
| `origineInaptitudePressentie` | `null` | Rempli par IA uniquement si avis médecin du travail présent avec qualification AT/MP/MO. |
| `avisMedecinTravailDate` | `null` | Rempli depuis date de l'avis d'inaptitude quand pièce présente. |
| `reclassementRespecteDetected` | `null` | Rempli si pièces de recherche de reclassement documentées ; `NON` si refus employeur explicite ; `INCONNU` sinon. |
| `heuresSupMentionneesDansDossier` | `null` | Rempli uniquement si bulletins de paie ou décomptes présents et évoquent des heures sup (25 %, 50 %, hors contingent). |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `motif_nullite_pressenti` | Non | — | `DISCRIMINATION`, `HARCELEMENT_MORAL`, `HARCELEMENT_SEXUEL`, `RETORSION`, `SYNDICAL`, `MATERNITE_PATERNITE`, `ACCIDENT_MP` | Non | Normalisation upper-case ; valeur hors liste → `null`. |
| `origine_inaptitude_pressentie` | Non | — | `ACCIDENT_TRAVAIL`, `MALADIE_PROFESSIONNELLE`, `MALADIE_ORDINAIRE` | Non | Normalisation upper-case ; valeur hors liste → `null`. |
| `avis_medecin_travail_date` | Non | — | YYYY-MM-DD | Non | Validation regex ISO ; sinon `null`. |
| `reclassement_respecte_detected.reponse` | Non | — | `OUI`, `NON`, `INCONNU` | Non | Normalisation via `normalizeReponse()` existant. |
| `reclassement_respecte_detected.justification` | Non | 500 caractères | texte libre | Non | Troncature 500 car (règle `MAX_JUSTIFICATION_LENGTH`). |
| `heures_sup_mentionnees.totalDeclarees25pct` | Non | — | entier ≥ 0 | Non | `null` si négatif ou non numérique. |
| `heures_sup_mentionnees.totalDeclarees50pct` | Non | — | entier ≥ 0 | Non | idem. |
| `heures_sup_mentionnees.horsContingent` | Non | — | entier ≥ 0 | Non | idem. |

Notes :
- Les 7 valeurs de `motif_nullite_pressenti` couvrent les principaux cas de nullité L.1132-1 / L.1152-3 / L.1153-3 / L.1225-71 / L.2411-1 CT FR. La liste sera validée par `MOTIFS_NULLITE_CODES` (nouveau set privé statique).
- Dossiers BE : les 5 champs restent `null` (les équivalents belges — CCT 109 nullité pour motif prohibé, etc. — sont couverts par `BE_INDEMNITE_MANIFESTE` déjà présent).

---

## Technique

### Endpoint(s)

Aucun endpoint nouveau. SF backend pure qui étend un record + un prompt + une méthode de parsing. Les endpoints d'analyse existants (`POST /api/v1/case-files/{id}/analyze`, `GET /api/v1/case-files/{id}/analysis`) incluent automatiquement les nouveaux champs dans leur réponse JSON via le record étendu.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_analysis` | Aucune (colonne `result` = JSON) | Le JSON sérialisé contient déjà un champ libre `travail_extracted_data` — l'ajout de sous-champs est transparent pour la DB. |

### Migration Liquibase

- [ ] Oui — n/a
- [x] Non applicable — persistance JSON

### Composants Angular

Aucun. La modification du DTO `case-analysis.model.ts` est un ajout de champs optional ; aucun composant existant n'est tenu de les consommer.

### Référentiel métier

Pas de modification de `legal_referentials` — les enums `MotifNullite`, `OrigineInaptitude` utilisés côté calculateurs (F-DT-11, F-DT-15) restent Java-only (pas de table de lookup). Les listes de codes dans `TRAVAIL_INSTRUCTION` restent en dur dans le prompt (cohérent avec `LICENCIEMENT_CRITERE_CODES`, `IMMIGRATION_TITLE_CODES`).

---

## Plan de test

### Tests unitaires

- [ ] `CaseAnalysisResponseTest.extractTravailData_motif_nullite_pressenti()` — fixture JSON harcèlement avec `motif_nullite_pressenti = "HARCELEMENT_MORAL"` → champ rempli.
- [ ] `CaseAnalysisResponseTest.extractTravailData_motif_nullite_invalide_retourne_null()` — fixture avec valeur hors liste → `null`.
- [ ] `CaseAnalysisResponseTest.extractTravailData_inaptitude_complete()` — fixture inaptitude avec `origine`, `avis_date`, `reclassement_respecte_detected` remplis → 3 champs remplis, parseur reconnaît la structure `{reponse, justification}`.
- [ ] `CaseAnalysisResponseTest.extractTravailData_reclassement_justification_troncature()` — fixture avec justification 600 car → tronquée à 500.
- [ ] `CaseAnalysisResponseTest.extractTravailData_heures_sup_object()` — fixture avec `heures_sup_mentionnees = {totalDeclarees25pct: 10, totalDeclarees50pct: 5, horsContingent: 2}` → objet correctement parsé.
- [ ] `CaseAnalysisResponseTest.extractTravailData_legacy_fixture_retrocompat()` — fixture historique sans les 5 champs → record Java créé avec tous les nouveaux champs `null`, champs historiques intacts.
- [ ] `CaseAnalysisResponseTest.extractTravailData_malformed_heures_sup_graceful()` — `heures_sup_mentionnees = 42` (not an object) → champ `null`, autres champs intacts.
- [ ] `LegalDomainPromptBuilderTest.travail_instruction_mentionne_nouveaux_champs()` — le texte du prompt contient les 5 noms de champs (test sentinelle anti-régression).
- [ ] `EnrichedAnalysisServiceTest.enriched_preserve_nouveaux_champs_travail()` — consigne d'enrichissement contient les noms des 5 nouveaux champs.

### Tests d'intégration

- [ ] `CaseAnalysisIntegrationTest` (s'il existe, sinon ajouter) — fake IA response avec un dossier harcèlement incluant `motif_nullite_pressenti` → réponse `GET /api/v1/case-files/{id}/analysis` contient `travailExtractedData.motifNullitePressenti`.
- [ ] Rétrocompat `PrudhomeFicheServiceTest` et `TribunalTravailFicheServiceTest` : les appels à `response.travailExtractedData().xxx()` sur les champs historiques restent verts.

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
| `PrudhomeFicheService` | Consomme `TravailExtractedData` — tolérant à l'extension record | `PrudhomeFicheServiceTest` doit rester vert |
| `TribunalTravailFicheService` | Idem | `TribunalTravailFicheServiceTest` doit rester vert |
| `EnrichedAnalysisService` | Consigne à étendre pour préserver les nouveaux champs | `EnrichedAnalysisServiceTest` — nouveau test sentinelle |
| DTO frontend `case-analysis.model.ts` | 5 nouveaux champs optional ajoutés | `npm run build` vert — pas de régression type-check |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — la SF ne touche ni auth, ni workspace switch, ni navigation.
- [x] Justification : SF backend pure sans impact UI.

---

## Dépendances

### Subfeatures bloquantes

- Aucune. Cette SF est la première du palier 1 et peut partir en parallèle avec SF-155-04-00-BE-immig-FR et SF-155-04-00-BE-immig-BE.

### Subfeatures débloquées par celle-ci

- SF-155-04-A1 (harcèlement frontend) — dépend de `motifNullitePressenti`.
- SF-155-04-A2 (inaptitude frontend) — dépend de `origineInaptitudePressentie`, `avisMedecinTravailDate`, `reclassementRespecteDetected`.
- SF-155-04-A3 (heures sup frontend) — dépend de `heuresSupMentionneesDansDossier`.

### Questions ouvertes impactées

- [ ] Aucune question de `docs/OPEN_QUESTIONS.md` tranchée ou impactée.

---

## Notes et décisions

- **Choix 1** — utiliser des enums String (pas Java enum) dans le record pour garder la tolérance fail-open existante (`normalizeReponse()` pattern). Cohérent avec `typeTitreSejourCode`, `motifLicenciement` qui sont déjà String dans le record.
- **Choix 2** — le sous-objet `HeuresSupMentionnees` est un nested record public de `CaseAnalysisResponse` (comme `DetectedAnswer`, `PieceManquanteEntry`). Pas de classe top-level.
- **Choix 3** — rétrocompat du constructeur traitée par empilement de constructeurs delégués (pattern déjà utilisé par `TravailExtractedData` pour SF-DT-04-04 et SF-130-01). Pas de breaking change pour les appelants Java.
- **Choix 4** — la liste des valeurs enum (`MOTIFS_NULLITE_CODES`, `ORIGINE_INAPTITUDE_CODES`) est validée dans `extractTravailData()` via un `Set<String>` statique privé — cohérent avec `LICENCIEMENT_CRITERE_CODES`. Valeur hors liste → `null` (fail-open).
- **Choix 5** — ne pas toucher aux tests des 3 calculateurs métier (F-DT-11, F-DT-15, F-DT-19) dans cette SF. Ils restent pilotés par leurs propres fixtures. La consommation `aiData` sera couverte par A1/A2/A3.
