# Mini-spec — F-IM-16 / SF-IM-16-01 Extension enum `type_procedure_detectee`

## Identifiant
`F-IM-16 / SF-IM-16-01`

## Feature parente
`F-IM-16` — Extension enum IA `type_procedure_detectee`

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-IM-16-01-extend-type-procedure-detectee`

---

## Objectif

Étendre l'enum `type_procedure_detectee` (aujourd'hui 3 valeurs : `RENOUVELLEMENT_TITRE_SEJOUR`, `DEMANDE_ASILE_OFPRA`, `RECOURS_CNDA`) avec 6 nouveaux codes + rattrapage d'1 code déjà présent en DB (mais pas en Java fallback), pour débloquer la détection IA des procédures OQTF, regroupement familial, naturalisation, changement de statut et AES salarié — prérequis de F-IM-08, F-IM-09 et du calendrier procédural F-IM-15.

---

## Comportement attendu

### Cas nominal

**Nouveaux codes ajoutés (France) :**
| Code | Base légale | Jalons procéduraux |
|---|---|---|
| `REGROUPEMENT_FAMILIAL` | R.434-35 CESEDA | Instruction OFII 6 mois (180 j), silence vaut rejet 6 mois |
| `NATURALISATION_DECRET` | art. 21-15 CCiv / décret du 30/12/1993 | Instruction sous-préfecture 18 mois (540 j), silence vaut rejet 18 mois |
| `CHANGEMENT_STATUT` | L.412-1 CESEDA | Instruction préfecture 4 mois (120 j), silence vaut rejet 4 mois |
| `AES_SALARIE` | circulaire Valls 28/11/2012 | Instruction préfecture 6 mois (180 j), silence vaut rejet 6 mois |
| `OQTF_AVEC_DELAI` | L.614-5 CESEDA | Expiration délai de départ volontaire 30 j, audience TA 3 mois (90 j), décision TA 6 mois (180 j) |
| `OQTF_SANS_DELAI` | L.731-1 CESEDA | Audience TA (JLD) 96 h (4 j), décision TA (JLD) 7 j |

**Rattrapage Java ↔ DB :**
- `REGULARISATION_EXCEPTIONNELLE` (L.435-1 CESEDA) : déjà en DB (`f1100001-...-14`) mais absent du Java fallback `ImmigrationProcedureReferentiel`. Ajouté pour cohérence.

**Flow impacté :**
1. L'IA, guidée par le prompt enrichi, détecte la procédure et remplit `type_procedure_detectee` avec l'un des 7 (désormais 10) codes.
2. `StatutoryDeadlineService.createImmigrationProcedureDeadlines` lit `type_procedure_detectee` + `date_depot_procedure` → consulte `LegalReferentialService.getImmigrationJalons(code)` → crée automatiquement les `CaseDeadline` STATUTORY correspondants.
3. `DecisionToolVisibilityService` (F-IA-04) peut router ces nouveaux codes vers les futurs outils F-IM-08/F-IM-09 (hors périmètre de cette SF — pas de règle créée ici).

### Cas d'erreur
| Situation | Comportement |
|---|---|
| Code inconnu renvoyé par l'IA | `StatutoryDeadlineService` fail-open (log debug `unknown type_procedure_detectee`, skip) — comportement existant inchangé |
| Code valide mais `date_depot_procedure` absente | Pas de deadline créée (log debug) — existant |
| Migration Liquibase 108 rollback | `ImmigrationProcedureReferentiel` fallback Java assure toujours les 7 codes — pas de cassure d'instance |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier immigration** : F-IM-05 titre de séjour (pas impacté — lit `type_titre_sejour_code`), F-IM-06 recours (pas impacté — lit `type_recours_code`), F-IM-07 droit au travail (pas impacté — lit `type_titre_sejour_code`). Seul `StatutoryDeadlineService` consomme `type_procedure_detectee`.
- [x] **Autres pays** : BE — pas inclus dans cette SF. Les procédures belges (ordre de quitter le territoire, naturalisation belge, demande 9bis/9ter) seront traitées dans F-IM-14 (couverture belge étendue). Voir section "Impact par domaine".
- [x] **Autres domaines** : DROIT_DU_TRAVAIL / DROIT_FAMILLE — non applicable, concept immigration pur.
- [x] **Référentiels concurrents** : `ImmigrationProcedureReferentiel.java` (fallback) et table DB `legal_referentials` type `IMMIGRATION_JALONS`. Divergence existante (DB a `REGULARISATION_EXCEPTIONNELLE` non présent en Java) corrigée ici.
- [x] **Prompt IA** : `LegalDomainPromptBuilder` doit être étendu pour guider l'IA vers les nouveaux codes.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `ImmigrationProcedureReferentiel.java` | Oui | Intégré — ajout 7 entrées (6 nouvelles + rattrapage REGULARISATION_EXCEPTIONNELLE) |
| `legal_referentials` table DB | Oui | Intégré — migration 108 ajoute 6 entrées (REGULARISATION_EXCEPTIONNELLE déjà présente depuis migration 049) |
| `LegalDomainPromptBuilder.java` prompt | Oui | Intégré — liste enum `type_procedure_detectee` passe de 3 à 10 valeurs |
| Tests `ImmigrationProcedureReferentielTest` | Oui | Intégré — 7 nouveaux tests |
| Tests `LegalReferentialServiceTest` — getImmigrationJalons | Oui | Intégré — 3 tests (1 par nouveau type critique) |
| `StatutoryDeadlineService` — nouveau code mal formé | Oui | Déjà couvert — fail-open existant, validé par test déjà présent |
| F-IM-14 BE — équivalents procédures belges | Non bloquant | Backlog F-IM-14 (couverture belge étendue) |
| `decision_tool_visibility_rules` — rules sur nouveaux codes | Non bloquant | Seront créées par F-IM-08 (OQTF), F-IM-09 (AES), etc. Aucune rule dans cette SF — cohérent avec l'architecture F-IA-04 (règles déclaratives par domaine) |

### Décision

- [x] Étendu aux cibles applicables (Java fallback + DB + prompt + tests) dans cette SF
- [x] Backlog F-IM-14 — procédures belges équivalentes (transparent, cohérent avec le spec)
- [x] Non applicable aux autres domaines (justifié)

---

## Impact par domaine métier

Cette feature est **sensible au domaine** : spécifique à **DROIT_IMMIGRATION**.
- **Droit du travail** : non applicable — enum propre à l'immigration.
- **Droit de la famille** : non applicable.
- **Droit immigration** : France uniquement dans cette SF. Belgique couverte par F-IM-14 (backlog V7) qui ajoutera les procédures belges équivalentes (ordre de quitter le territoire art. 7 Loi 15/12/1980, naturalisation belge CNB, demande 9bis humanitaire, 9ter médical) avec leurs propres jalons. Séparer FR / BE est cohérent avec la structure existante de F-IM-16 (France uniquement) et permet de ne pas gonfler cette SF.

---

## Parité des domaines métier

Outil de **niveau 1** (extension de référentiel / catalogue enum) — la règle de parité niveau ≥5 ne s'applique pas.

---

## Critères d'acceptation

- [ ] **C1** : Migration Liquibase 108 insère 6 entrées `IMMIGRATION_JALONS` dans `legal_referentials` (France, `is_system=true`, `description` remplie en langage avocat per SF-140-03)
- [ ] **C2** : `ImmigrationProcedureReferentiel.resolve("REGROUPEMENT_FAMILIAL")` retourne 2 jalons (instruction + silence vaut rejet 6 mois)
- [ ] **C3** : `ImmigrationProcedureReferentiel.resolve("NATURALISATION_DECRET")` retourne 2 jalons (18 mois)
- [ ] **C4** : `ImmigrationProcedureReferentiel.resolve("CHANGEMENT_STATUT")` retourne 2 jalons (4 mois)
- [ ] **C5** : `ImmigrationProcedureReferentiel.resolve("AES_SALARIE")` retourne 2 jalons (6 mois)
- [ ] **C6** : `ImmigrationProcedureReferentiel.resolve("OQTF_AVEC_DELAI")` retourne 3 jalons (DDV 30 j, audience TA 3 mois, décision TA 6 mois)
- [ ] **C7** : `ImmigrationProcedureReferentiel.resolve("OQTF_SANS_DELAI")` retourne 2 jalons (audience + décision JLD 48-96 h)
- [ ] **C8** : `ImmigrationProcedureReferentiel.resolve("REGULARISATION_EXCEPTIONNELLE")` retourne 2 jalons (rattrapage Java)
- [ ] **C9** : `LegalReferentialService.getImmigrationJalons("REGROUPEMENT_FAMILIAL")` passe par la DB (pas le fallback) et retourne les mêmes jalons
- [ ] **C10** : le prompt généré par `LegalDomainPromptBuilder.build(DROIT_IMMIGRATION, FRANCE)` liste les 10 codes (3 existants + 7 nouveaux/rattrapés)
- [ ] **C11** : `StatutoryDeadlineService` crée bien les `CaseDeadline` STATUTORY pour un nouveau code (ex. `OQTF_AVEC_DELAI` + `date_depot_procedure` → 3 deadlines créés)
- [ ] **C12** : Pas de collision d'UUID (ids de la migration 108 hors des plages `f1100001-0000-0000-0000-00000000001X` déjà utilisées par migration 049)
- [ ] **C13** : `LegalReferentialDescriptionIntegrityIT` reste vert (description obligatoire sur chaque INSERT `is_system=true` — SF-140-03)

---

## Périmètre

### Hors scope (explicite)
- Création de règles `decision_tool_visibility_rules` pour ces nouveaux codes → sera fait par F-IM-08 / F-IM-09 qui fournissent les outils correspondants.
- Procédures belges équivalentes → F-IM-14.
- Création d'outils décisionnels OQTF / RF / naturalisation → F-IM-08, F-IM-11, F-IM-12, F-IM-13 respectivement.
- Modification du modèle de données (pas de nouvelle colonne).
- Frontend — aucun changement, le front consomme `typeProcedureDetectee` en texte libre sans enum côté client.

---

## Technique

### Endpoints
Inchangés.

### Tables impactées
| Table | Opération | Notes |
|-------|-----------|-------|
| `legal_referentials` | 6 INSERT | `referential_type='IMMIGRATION_JALONS'`, `country=NULL` (valable FR, BE distinct via F-IM-14), `is_system=true` |

### Migration Liquibase
- [x] Oui — `108-seed-immigration-jalons-extended.xml`
- Plage UUID : `f1100001-0000-0000-0000-00000000002X` (libre — `-14` est le dernier de la section IMMIGRATION_JALONS, les suivants passent au type IMMIGRATION_PIECES `-15/16/17`). Utiliser `f1108000-0000-0000-0000-00000000000X` pour rester isolable.
- Description obligatoire sur chaque INSERT (SF-140-03).

### Composants modifiés
- Backend : `ImmigrationProcedureReferentiel.java`, `LegalDomainPromptBuilder.java`
- Backend tests : `ImmigrationProcedureReferentielTest.java` (+7 tests)
- Backend IT : indirectement couvert par `LegalReferentialDescriptionIntegrityIT` (vérifie la règle description)

---

## Plan de test

### Tests unitaires

- [ ] `resolve_regroupementFamilial_returns2Jalons` — label + offsetDays corrects
- [ ] `resolve_naturalisationDecret_returns2Jalons`
- [ ] `resolve_changementStatut_returns2Jalons`
- [ ] `resolve_aesSalarie_returns2Jalons`
- [ ] `resolve_oqtfAvecDelai_returns3Jalons`
- [ ] `resolve_oqtfSansDelai_returns2Jalons`
- [ ] `resolve_regularisationExceptionnelle_returns2Jalons` — rattrapage Java/DB

### Tests d'intégration

- [ ] `LegalReferentialDescriptionIntegrityIT` reste vert après migration 108 (vérification description SF-140-03)

### Isolation workspace

- [x] Non applicable — référentiels `is_system=true` sont globaux (workspace_id NULL), accès en lecture seule via `LegalReferentialService`.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing
- [x] **Aucune** — extension pure de référentiel métier, pas d'impact sur les flows transversaux.

### Composants / endpoints impactés

Aucun impact de régression :
- `StatutoryDeadlineService` : aucun changement de comportement sur les 3 codes existants. Les 6 nouveaux codes produisent des deadlines supplémentaires. Fail-open inchangé sur codes inconnus.
- `CaseAnalysisResponse` : le champ `typeProcedureDetectee` est texte libre côté DTO, aucune enum contrainte n'est imposée par le record — pas de régression.
- Frontend : consomme le champ en string opaque, aucun impact.

### Smoke tests E2E concernés

- [x] Aucun — pas de changement d'auth / workspace / navigation / plans.

---

## Dépendances

### Subfeatures bloquantes
- SF-140-03 (règle description) — done (LegalReferentialDescriptionIntegrityIT) — respectée.
- Migrations 049 (seed IMMIGRATION_JALONS initial) — done.

### Questions ouvertes impactées
Aucune.

---

## Notes et décisions

- **OQTF_SANS_DELAI** : durée de la procédure JLD très courte (48h-96h). Deadlines créées en heures effectives même si notre modèle exprime en jours (offsetDays=4 au plus). Précision acceptable — alerte à 1 jour.
- **NATURALISATION_DECRET** : délai 18 mois correspond au délai légal (art. 21-25-1 CCiv : instruction max 18 mois + 3 mois décret). On prend 540 jours comme seuil d'alerte. Ne couvre pas la naturalisation par mariage (art. 21-2) — variante à ajouter plus tard si besoin.
- **AES_SALARIE** distinct d'`AES_PRIVE_FAMILIAL` : le spec parle d'AES "4 motifs" (F-IM-09). Ici on ajoute seulement le motif salarié (circulaire Valls). Les 3 autres motifs (privé familial L.435-1 déjà couvert par REGULARISATION_EXCEPTIONNELLE, protection L.435-2, discrétionnaire L.435-3) seront traités dans F-IM-09 avec leurs propres codes si besoin.
- **Rattrapage REGULARISATION_EXCEPTIONNELLE** : gouvernance — cf. CLAUDE.md règle "Modification d'un référentiel métier statique sans migration Liquibase accompagnant". Ici c'est l'inverse (DB a le seed, Java manque) — même principe, on aligne Java ↔ DB.
- **Plage UUID choisie** : `f1108000-0000-0000-0000-00000000000X` — isolable, repérable (108 = numéro de migration), ne collisionne avec aucune plage existante.
