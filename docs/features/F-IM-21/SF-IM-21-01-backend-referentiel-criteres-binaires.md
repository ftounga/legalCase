# Mini-spec — F-IM-21 / SF-IM-21-01 Backend référentiel critères binaires immigration

## Identifiant

`F-IM-21 / SF-IM-21-01`

## Feature parente

`F-IM-21` — Critères binaires de validité dossier immigration (équivalent F-DT-08, FR + BE)

## Statut

`ready`

## Date de création

2026-04-30

## Branche Git

`feat/SF-IM-21-01-backend-referentiel`

---

## Objectif

Créer le référentiel `ImmigrationValidationCriteriaReferentiel` (Java fallback) + seedage `legal_referentials` (source de vérité) pour les critères binaires de validité d'un dossier immigration FR + BE. Ces codes seront ensuite consommés par le prompt IA (SF-IM-21-02) afin que la checklist procédurale F-96 produise du binaire en immigration (et non plus des pistes stratégiques mal placées).

---

## Comportement attendu

### Cas nominal

1. **Référentiel Java statique** : `ImmigrationValidationCriteriaReferentiel` (sous `fr.ailegalcase.casefile`) expose la liste figée des codes IM21_* avec : `code`, `country` (FR/BE), `label`, `description`, `baseJuridique`. Pattern miroir `RuptureConvCritereReferentiel`.
2. **Seedage DB** : migration Liquibase `198-seed-immigration-validation-criteria.xml` insère 1 ligne par critère dans `legal_referentials` avec `referential_type = 'IM21_VALIDITY_CRITERES'`, `legal_domain = 'DROIT_IMMIGRATION'`, `country = 'FRANCE'` ou `'BELGIQUE'`, `is_system = true`, `description` rempli (SF-140-03 obligatoire).
3. **Lecture par les services** : `LegalReferentialService.findActive(domain, type, country)` retourne la liste des critères pour un pays donné. Pas d'API REST exposée — le référentiel est un détail technique consommé par le prompt builder (SF-IM-21-02).
4. **Cohérence Java fallback ↔ DB** : si la DB n'a pas seedé le référentiel (cas test ou rollback), le service retombe sur le fallback Java.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Migration partielle (qq codes manquants) | Le service retourne ce qui est en DB ; les codes manquants sont absents du prompt — l'IA ne les utilisera pas (échec silencieux acceptable V1) |
| Code IM21_ inconnu fourni à l'avocat | Géré par `LegalReferentialService` (404 ou null silencieux selon API) |
| Workspace différent | N/A — `is_system = true`, lecture publique cross-workspace (pas de filtre) |
| Description vide (`description IS NULL`) | Test d'intégrité `LegalReferentialDescriptionIntegrityIT` échoue en CI (garde-fou F-140 SF-140-03) |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : F-DT-08 (Validité licenciement FR/BE) — pattern de référence STRICT, miroir codes binaires. F-FA-07 (Checklist divorce) — pattern proche mais codes énumérés (FR_CHOIX_AVOCATS, etc.). F-IM-05/06/07 (énumérés, pas binaires) — différents par essence. F-RUPTURE_CONV_CRITERES — pattern miroir (Java + seed DB + description).
- [x] **Autres pays** : France + Belgique — mini-spec couvre les deux explicitement.
- [x] **Autres domaines** : N/A — F-IM-21 est strictement immigration. Le travail a déjà F-DT-08, la famille a F-FA-07.
- [x] **Autres UI patterns** : N/A — pas d'UI dédiée. Les codes IM21_* alimenteront le bloc F-96 existant.
- [x] **Autres flows** : pas d'auth nouvelle, pas de routing, pas de quota. Pas de migration de données (les anciennes analyses immigration n'ont pas IM21_*, elles continuent de fonctionner sans — fail-open prompt).

### Niveaux de vérification

- [x] **Java référentiel** : `ImmigrationValidationCriteriaReferentiel` (record + Map figée).
- [x] **Migration Liquibase** : `198-seed-immigration-validation-criteria.xml` — 18 INSERT, idempotent, réversible.
- [x] **Description SF-140-03** : chaque entrée a une `description` obligatoire (le test d'intégrité `LegalReferentialDescriptionIntegrityIT` couvrira automatiquement).
- [x] **Tests unitaires** : 1 test `resolve()` par critère (×18) + 1 test global "tous les codes ont description non vide".

### Nouveau pattern UI ou service partagé

- [x] **Nouveau type `IM21_VALIDITY_CRITERES`** dans `legal_referentials` — pattern miroir RUPTURE_CONV_CRITERES, LICENCIEMENT_CRITERES. Pas de divergence.
- [x] **Pas de nouveau service** — `LegalReferentialService` existant suffit.
- [x] **Pas de nouveau composant frontend** — la checklist F-96 affiche les critères binaires comme F-DT-08 (transparent côté UI).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-DT-08 (Validité licenciement) | Oui — pattern de référence | Pattern miroir strict suivi |
| F-RUPTURE_CONV_CRITERES | Oui — pattern de référence | Pattern miroir strict (Java + seed DB + description) |
| F-FA-07 (Checklist divorce) | Non | Codes énumérés, pas binaires — différent par essence |
| F-IM-05/06/07 (énumérés) | Non | Différents (codes valeurs, pas binaire) |
| Frontend (checklist F-96) | Non | Affichage transparent des codes binaires (pattern existant) |
| F-IA-03 | À traiter en SF-IM-21-03 | Hors scope SF-IM-21-01 |

### Décision

- [x] Pattern miroir F-DT-08 + RUPTURE_CONV strict appliqué.
- [x] SF-IM-21-02 (prompt) et SF-IM-21-03 (F-IA-03) suivent dans l'ordre.

---

## Impact par domaine métier

| Domaine | Effet |
|---------|-------|
| **Droit du travail (FR + BE)** | Aucun — F-IM-21 ne touche pas le droit du travail. F-DT-08 reste source de vérité travail. |
| **Droit de la famille (FR + BE)** | Aucun — F-IM-21 ne touche pas le droit de la famille. F-FA-07 reste source de vérité famille. |
| **Droit de l'immigration (FR + BE)** | **Cible directe** — 11 critères FR + 7 critères BE seedés. Couvre les types de titre les plus fréquents (VPF / Passeport talent / Renouvellement / Conjoint / Étudiant / AES en France ; 40ter Conjoint / Permis unique / 9bis Humanitaire / Étudiant en Belgique). |

---

## Parité des domaines métier

(N/A — F-IM-21 est explicitement réservé à l'immigration. Le travail a F-DT-08, la famille a F-FA-07. Le concept "critères binaires de validité" est déjà couvert dans les 3 domaines.)

---

## Critères d'acceptation

- [ ] Classe `ImmigrationValidationCriteriaReferentiel` créée sous `backend/src/main/java/fr/ailegalcase/casefile/`.
- [ ] Record `ValidityCriterion(String code, String country, String label, String baseJuridique, String description)`.
- [ ] Map figée `REFERENTIEL` avec les 18 critères listés ci-dessous.
- [ ] Méthodes : `resolve(String code) → ValidityCriterion?`, `forCountry(String country) → List<ValidityCriterion>`.
- [ ] Migration Liquibase `198-seed-immigration-validation-criteria.xml` insère 18 lignes dans `legal_referentials` (1 INSERT par critère) avec `referential_type='IM21_VALIDITY_CRITERES'`, `description` non null, `is_system=true`, `is_active=true`.
- [ ] Rollback dans la migration : DELETE par les 18 UUIDs.
- [ ] Tests unitaires `ImmigrationValidationCriteriaReferentielTest` :
  - 1 test par code : `resolve("IM21_REGULARITE_SEJOUR_FR")` retourne le bon record
  - 1 test : `forCountry("FRANCE").size() == 11`
  - 1 test : `forCountry("BELGIQUE").size() == 7`
  - 1 test : tous les codes ont `description` non vide
  - 1 test : tous les codes ont `baseJuridique` non vide
- [ ] Pas de modification du prompt (laissé pour SF-IM-21-02).
- [ ] Pas d'endpoint REST dédié.

---

## Périmètre

### Hors scope

- Modification du prompt `CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` (SF-IM-21-02)
- Alignement F-IA-03 (SF-IM-21-03)
- Frontend dédié — la checklist F-96 affiche les critères binaires comme F-DT-08 (transparent)
- Workflow de génération de dossier (F-IM-06 recours suffit)
- Critères pour titres de séjour rares (long-séjour visiteur, retraité, etc.) — V2 si retour terrain
- Validation runtime des codes IM21_ (l'IA peut produire un code inconnu — fail-open)

---

## Critères figés (18 codes)

### France (11 critères)

| Code | Label | Base juridique | Description (SF-140-03) |
|------|-------|---------------|--------|
| IM21_REGULARITE_SEJOUR_FR | Régularité du séjour au moment du dépôt | Art. R.431-2 CESEDA | Le demandeur est-il en situation régulière au moment du dépôt (titre en cours de validité, récépissé, ou pré-droit ouvert) ? |
| IM21_DELAI_DEPOT_FR | Délai de dépôt avant expiration | Art. R.431-5 CESEDA | La demande de renouvellement a-t-elle été déposée dans les 2 mois précédant l'expiration du titre en cours ? |
| IM21_PIECE_IDENTITE_FR | Passeport ou pièce d'identité valide | Art. R.431-10 CESEDA | Le passeport ou la pièce d'identité du demandeur est-il valide pour la durée du séjour demandé ? |
| IM21_JUSTIF_DOMICILE_FR | Justificatif de domicile en France | Art. R.431-10 CESEDA | Le demandeur produit-il un justificatif de domicile en France de moins de 6 mois (facture, attestation hébergement, bail) ? |
| IM21_ETAT_CIVIL_FR | Pièces d'état civil traduites/légalisées | Art. R.431-10 CESEDA + Conv. La Haye 1961 | Les actes d'état civil étrangers sont-ils traduits par traducteur assermenté et légalisés/apostillés selon le pays d'origine ? |
| IM21_PHOTO_FR | Photo d'identité conforme | Art. R.431-10 CESEDA | Les photos d'identité respectent-elles la norme préfecture (récentes, fond uni, format 35×45 mm) ? |
| IM21_TIMBRE_FISCAL_FR | Timbre fiscal acquitté | Art. L.311-13 CESEDA | Le timbre fiscal correspondant au type de titre demandé est-il acquitté et présenté ? |
| IM21_PIECES_MARIAGE_FR | Acte de mariage transcrit | Art. L.423-1 CESEDA + Art. 47 Cciv | Pour les titres VPF conjoint français : l'acte de mariage est-il transcrit en France (ou pris en compte sans transcription si mariage en France) ? |
| IM21_COMMUNAUTE_VIE_FR | Communauté de vie justifiée | Art. L.423-1 CESEDA | Pour les titres VPF conjoint : la communauté de vie effective et continue est-elle justifiée (factures communes, témoignages, photos) ? |
| IM21_RESSOURCES_FR | Ressources stables et suffisantes | Art. L.423-2+ CESEDA | Pour les titres exigeant des ressources (étudiant, salarié, regroupement) : les ressources sont-elles stables et au moins égales au seuil légal du titre concerné ? |
| IM21_CONVENTION_ACCUEIL_FR | Convention d'accueil organisme recherche | Art. L.421-14 CESEDA + Art. R.421-21 | Pour le Passeport talent — Chercheur : la convention d'accueil signée par un organisme de recherche habilité (CNRS, INRIA, université, etc.) est-elle présente ? |

### Belgique (7 critères)

| Code | Label | Base juridique | Description (SF-140-03) |
|------|-------|---------------|--------|
| IM21_REGULARITE_SEJOUR_BE | Régularité du séjour au moment du dépôt | Art. 9 + 9bis Loi 15/12/1980 | Le demandeur est-il en situation régulière au moment du dépôt (annexe 19, annexe 15, carte en cours) ou justifie-t-il d'une demande pour circonstances exceptionnelles ? |
| IM21_PIECE_IDENTITE_BE | Passeport valide | Art. 41 Loi 15/12/1980 | Le passeport du demandeur est-il valide et couvre-t-il au minimum 1 an au-delà de la date de demande ? |
| IM21_PIECES_COHABITATION_BE | Pièces cohabitation/mariage | Art. 40bis + 40ter + 10 + 11 Loi 15/12/1980 | Pour les titres familiaux (40ter, cohabitant légal, regroupement) : l'acte de mariage ou contrat de cohabitation légale est-il enregistré et la composition de ménage commune produite ? |
| IM21_RESSOURCES_BE | Ressources suffisantes | Art. 10 §5 + 40ter §2 Loi 15/12/1980 | Pour les titres familiaux et étudiants : les revenus mensuels nets sont-ils ≥ 120 % du RIS (revenu d'intégration sociale) ? |
| IM21_LOGEMENT_BE | Logement suffisant | Art. 10 §2 1° Loi 15/12/1980 | Le demandeur (ou regroupant) dispose-t-il d'un logement suffisant pour héberger sa famille (dimensions, salubrité, attestation commune) ? |
| IM21_ASSURANCE_BE | Assurance maladie | Art. 9 Loi 15/12/1980 + AR 8/10/1981 | Le demandeur dispose-t-il d'une assurance maladie couvrant la durée du séjour demandé (mutuelle, attestation employeur, ou européenne) ? |
| IM21_EXTRAIT_CASIER_BE | Extrait de casier judiciaire | Art. 9 + 13 Loi 15/12/1980 | L'extrait de casier judiciaire du pays d'origine de moins de 6 mois est-il fourni et traduit ? |

---

## Technique

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `legal_referentials` | INSERT (18 lignes) | Migration `198-seed-immigration-validation-criteria.xml` |

### UUIDs

Plage `f1100001-0000-0000-0000-0000000001a0` à `f1100001-0000-0000-0000-0000000001b1` (18 UUIDs, à vérifier qu'ils sont libres avant migration).

### Migration Liquibase

- [x] Oui — `db/changelog/migrations/198-seed-immigration-validation-criteria.xml` (à confirmer si 198 disponible).

### Composants Angular

(N/A — couvert plus tard par SF-IM-21-02 prompt + intégration F-96 frontend transparente)

---

## Plan de test

### Tests unitaires

- [ ] `ImmigrationValidationCriteriaReferentielTest.resolve_returnsCorrectCriterion()` — 18 assertions
- [ ] `ImmigrationValidationCriteriaReferentielTest.forCountry_FRANCE_returns11Criteria()`
- [ ] `ImmigrationValidationCriteriaReferentielTest.forCountry_BELGIQUE_returns7Criteria()`
- [ ] `ImmigrationValidationCriteriaReferentielTest.allCriteria_haveNonEmptyDescription()`
- [ ] `ImmigrationValidationCriteriaReferentielTest.allCriteria_haveNonEmptyBaseJuridique()`
- [ ] `ImmigrationValidationCriteriaReferentielTest.resolve_unknownCode_returnsNull()`

### Tests d'intégration

- [ ] `LegalReferentialDescriptionIntegrityIT` (existant SF-140-03) — couvre automatiquement la non-régression "description non vide" sur les 18 nouvelles entrées.

### Isolation workspace

- [x] N/A — `is_system = true`, le référentiel est public cross-workspace par construction (pattern miroir F-DT-08).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — la SF crée une donnée référentielle isolée (pattern miroir F-DT-08 et RUPTURE_CONV_CRITERES, déjà éprouvés).

### Smoke tests E2E concernés

- [x] Aucun.

---

## Dépendances

### Subfeatures bloquantes

- **SF-176-01 + SF-176-02** : mergées (PR #709, #713). Sans elles, l'IA continue à mettre des pistes stratégiques dans `points_procedure` malgré le référentiel.
- **SF-96-06** : mergée (PR #708).

### Subfeatures parallèles

- (N/A — la séquence F-IM-21 est strictement sérielle car SF-IM-21-02 dépend des codes figés ici.)

### Subfeatures débloquées

- **SF-IM-21-02** — extension prompt avec liste explicite des 18 codes IM21_*.

### Questions ouvertes impactées

- [x] Aucune.

---

## Notes et décisions

- **Pourquoi 18 codes et pas plus ?** Couverture "core" des titres les plus fréquents en V1 (≥ 80 % des dossiers cliniques typiques). Liste extensible en V2 (long-séjour visiteur, naturalisation, OQTF, retour volontaire).
- **Pourquoi `is_system = true` ?** Pattern miroir F-DT-08 / RUPTURE_CONV_CRITERES. Les critères sont juridiques universels FR + BE, pas customisables par workspace.
- **Pourquoi pas une table dédiée `immigration_validity_criteria` ?** La table `legal_referentials` (SF-140) est l'abstraction commune pour tous les référentiels juridiques. Cohérence architecture.
- **Pourquoi pas d'endpoint REST ?** Les codes IM21_* sont consommés en interne par le prompt (SF-IM-21-02). L'avocat les voit via la checklist F-96 (existante). Pas besoin d'API frontend dédiée.
- **Pourquoi la liste FR a 11 codes et BE seulement 7 ?** Les exigences administratives FR sont plus granulaires (timbre fiscal, photo, état civil légalisé séparés). En BE, plusieurs sont fusionnés sous "annexe 19/15 régularité séjour". Asymétrie naturelle des deux régimes.
- **Pourquoi `IM21_RESSOURCES_FR` est générique (pas par titre) ?** Le seuil dépend du titre (étudiant ≠ salarié ≠ Passeport talent), mais l'exigence "ressources suffisantes" est universelle. Le prompt SF-IM-21-02 instruira l'IA d'indiquer le seuil applicable dans `texte`.
