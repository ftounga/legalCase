# Mini-spec — F-207 / SF-207-01-backend Outil prescription Travail BE — calculateur de délais

## Identifiant

`F-207 / SF-207-01-backend`

## Feature parente

`F-207` — P1 Travail BE — 8 outils urgences BE-only

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-207-01-backend-prescription-be`

---

## Objectif

Calculateur des délais de prescription des actions Travail BE — **1 an post-rupture** pour les créances ex-contrat (Loi 03/07/1978 art. 15), **5 ans** pour les actions pendant le contrat et les arriérés de salaire (art. 15 et CCT 109 art. 11).

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/prescription-be-litige-travail`

Inputs (body) :
- `dateRupture` (ISO date) — date de rupture du contrat (obligatoire pour `EX_CONTRAT`).
- `typeCreance` (enum) — `EX_CONTRAT_GENERAL` | `EX_CONTRAT_CCT_109` | `PENDANT_CONTRAT` | `ARRIERES_SALAIRE`.
- `dateActionEnvisagee` (ISO date) — date à laquelle l'avocat envisage d'agir (par défaut : aujourd'hui).

Logique de calcul (`PrescriptionBeLitigeTravailCalculator`) :

| `typeCreance` | Délai | Point de départ | Base juridique |
|---|---|---|---|
| `EX_CONTRAT_GENERAL` | **1 an** | date de rupture | Loi 03/07/1978 art. 15 al. 1 |
| `EX_CONTRAT_CCT_109` | **1 an** | date de rupture | CCT 109 art. 11 (action en motivation du licenciement) |
| `PENDANT_CONTRAT` | **5 ans** | fait générateur | Loi 03/07/1978 art. 15 al. 2 |
| `ARRIERES_SALAIRE` | **5 ans** | échéance de paiement | Loi 03/07/1978 art. 15 al. 2 ; CC art. 2262bis |

- `dateLimitePrescription = dateRupture + délai` (pour EX_CONTRAT_*) ou `dateActionEnvisagee + délai` (pour PENDANT_CONTRAT / ARRIERES_SALAIRE — l'avocat saisit alors la date d'échéance comme `dateRupture`).
- `joursRestants = dateLimitePrescription - dateActionEnvisagee` (en jours pleins, fuseau Europe/Brussels).
- `verdict` :
  - **PRESCRIT** si `joursRestants ≤ 0`.
  - **IMMINENT** si `0 < joursRestants ≤ 30`.
  - **NON_PRESCRIT** sinon.

Output (`PrescriptionBeLitigeTravailResponse`) :
```json
{
  "verdict": "PRESCRIT" | "IMMINENT" | "NON_PRESCRIT",
  "dateLimitePrescription": "2027-05-19",
  "joursRestants": 365,
  "regleAppliquee": "1_AN_POST_RUPTURE_LOI_1978_ART_15" | "1_AN_CCT_109_ART_11" | "5_ANS_PENDANT_CONTRAT" | "5_ANS_ARRIERES_SALAIRE",
  "baseJuridique": "Loi du 3 juillet 1978 art. 15 ; CCT 109 art. 11",
  "formuleCalcul": "dateRupture (2026-05-19) + 1 an = dateLimite (2027-05-19) ; joursRestants = 365"
}
```

Persistance : 1 ligne `prescription_be_litige_travail_analyses` par dossier — la dernière (unique sur `case_file_id`, mise à jour à chaque POST). Inputs persistés en JSON (`result_data`) pour survivre au reload (mémoire `feedback_decision_tools_all_fields_prefilled`).

`GET` du même path renvoie la dernière analyse ou 404 si aucune.

### Cas d'erreur

| Situation | Code | Comportement |
|---|---|---|
| `caseFileId` introuvable ou autre workspace | 404 | « Case file not found » |
| `workspaceCountry !== 'BELGIUM'` | 404 | Outil masqué côté FR — l'endpoint répond 404 pour préserver l'isolation BE-only |
| `typeCreance` manquant ou invalide | 400 | « typeCreance invalide » |
| `dateRupture` manquant pour `EX_CONTRAT_*` ou `ARRIERES_SALAIRE` | 400 | Message explicite |
| `dateActionEnvisagee` au format invalide | 400 | « date invalide » |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : pattern canonique `ContestationAre*` (F-DT-35) — 10 fichiers Java + migration `158-create-contestation-are-analyses.xml`. À mirrorer pour BE.
- [x] **Autres pays** : non applicable — outil **BE-only** par construction. Aucune logique FR.
- [x] **Autres domaines** : non applicable — Droit du travail uniquement.
- [x] **Autres UI patterns** : non applicable — SF backend pure (frontend = SF-207-01b).
- [x] **Autres flows transversaux** : workspace context (`workspaceCountry=BELGIUM` gate) — pattern existant F-198 / F-204.

### Décision

- [x] Étendu à la cible applicable. Pas de pattern partagé nouveau créé — réutilise le pattern `ContestationAre*` et le gate workspace BE existant.

---

## Conformité F-IA-04

- [x] **Non applicable** — SF backend pure (le composant frontend décisionnel est livré par SF-207-01b).

---

## Champs IA à extraire (pré-remplissage)

Cette SF backend **est pré-requise** pour le pré-remplissage frontend de la SF-207-01b. Champs à extraire par l'IA (extension `LegalDomainPromptBuilder` pour `country=BELGIUM`) :

| Champ formulaire | Type | Champ source `TravailExtractedData` BE | Extension requise |
|---|---|---|---|
| `dateRupture` | date | `dateRuptureContrat` | [x] record + [x] prompt + [x] parser BE |
| `typeCreance` | enum | dérivé : si `motif_rupture_detecte` ∈ {licenciement, demission, faute_grave, RCC} → `EX_CONTRAT_GENERAL` ; sinon prompt direct | [x] record + [x] prompt |

- [x] Extension `TravailExtractedData` BE + prompt `LegalDomainPromptBuilder` (branche `country=BELGIUM`) dans le périmètre de cette SF.

---

## Critères d'acceptation

- [ ] `POST` retourne `PRESCRIT` pour une `dateRupture` > 1 an dans le passé avec `typeCreance=EX_CONTRAT_GENERAL` et `dateActionEnvisagee=today`.
- [ ] `POST` retourne `IMMINENT` pour une `dateRupture` 11 mois dans le passé (joursRestants ∈ ]0; 30]) avec `EX_CONTRAT_GENERAL`.
- [ ] `POST` retourne `NON_PRESCRIT` pour une `dateRupture` 3 mois dans le passé avec `EX_CONTRAT_GENERAL`.
- [ ] `POST` applique 5 ans pour `PENDANT_CONTRAT` et `ARRIERES_SALAIRE`.
- [ ] `POST` retourne 404 si le workspace est `country=FRANCE` (isolation BE-only).
- [ ] `POST` retourne 404 si le case_file appartient à un autre workspace (isolation workspace standard).
- [ ] `GET` renvoie la dernière analyse persistée ou 404 si aucune.
- [ ] Émission `critereCode` BE dans le prompt `CaseAnalysisService` BE : `BE_PRESCRIPTION_DATE_RUPTURE`, `BE_PRESCRIPTION_TYPE_CREANCE`. Garde-fou `CritereCodeIntegrityIT` reste vert.
- [ ] `TravailExtractedData` BE étendu pour exposer `dateRuptureContrat` + `motifRupture` extraits par l'IA.

---

## Périmètre

### Hors scope

- Frontend (`prescription-be-litige-travail-section.component`) — SF-207-01b.
- Prescription **pénale** du travail (art. 1ᵉʳ Code pénal social BE) — autre régime, hors scope.
- Prescription des actions des organismes (ONSS, ONEM) — autre régime, hors scope.
- Délai de forclusion CJ art. 1051 (appel) — hors scope (autre outil potentiel : `appel-cour-du-travail`).

---

## Contraintes de validation

| Champ | Obligatoire | Format | Validation |
|---|---|---|---|
| `dateRupture` | Oui (pour `EX_CONTRAT_*` et `ARRIERES_SALAIRE`) | ISO 8601 date | passée (≤ today + 1 j) |
| `typeCreance` | Oui | enum | `EX_CONTRAT_GENERAL` \| `EX_CONTRAT_CCT_109` \| `PENDANT_CONTRAT` \| `ARRIERES_SALAIRE` |
| `dateActionEnvisagee` | Non (défaut today) | ISO 8601 date | postérieure à `dateRupture` |

Calcul des jours en fuseau **Europe/Brussels** (cohérent avec les autres outils BE).

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/prescription-be-litige-travail` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/prescription-be-litige-travail` | OIDC | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `prescription_be_litige_travail_analyses` | INSERT / UPDATE / SELECT | 1 ligne par `case_file_id` (unique). Colonnes : `id` (UUID), `case_file_id` (FK CASCADE), `result_data` (TEXT JSON : inputs + verdict + formuleCalcul), `created_at`, `updated_at`. |

### Migration Liquibase

- [x] Oui — `XXX-create-prescription-be-litige-travail-analyses.xml` (prochain numéro disponible — l'agent vérifie). Reversible (`<rollback><dropTable .../></rollback>`).

### Composants impactés (pattern `ContestationAre*` à mirrorer)

À créer dans `backend/src/main/java/fr/ailegalcase/casefile/` (sauf indication contraire) :
- `PrescriptionBeLitigeTravailAnalysis.java` — entité JPA.
- `PrescriptionBeLitigeTravailRepository.java` — `JpaRepository<…, UUID>` + `findByCaseFileId`.
- `PrescriptionBeLitigeTravailTypeCreance.java` — enum (4 valeurs).
- `PrescriptionBeLitigeTravailRequest.java` — DTO POST.
- `PrescriptionBeLitigeTravailResult.java` — record verdict (verdict, dateLimite, joursRestants, regleAppliquee, baseJuridique, formuleCalcul).
- `PrescriptionBeLitigeTravailResponse.java` — DTO GET.
- `PrescriptionBeLitigeTravailService.java` — orchestration (validation, calcul, persistance, isolation).
- `PrescriptionBeLitigeTravailCalculator.java` — fonction pure `compute(Request) → Result`.
- `PrescriptionBeLitigeTravailController.java` — `@RestController` POST + GET.

Dans `backend/src/main/java/fr/ailegalcase/analysis/LegalDomainPromptBuilder.java` :
- Extension de la branche `country=BELGIUM` pour Travail : ajout des `critereCode` `BE_PRESCRIPTION_DATE_RUPTURE`, `BE_PRESCRIPTION_TYPE_CREANCE` + section explicative du nouveau champ `dateRuptureContrat` extrait.

Dans `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisResponse.java` (record `TravailExtractedData`) :
- Ajout champs `dateRuptureContrat` (LocalDate) + `motifRupture` (String) si non présents pour BE. Vérifier rétrocompatibilité des constructeurs.

### Composants Angular

Aucun (SF-207-01b).

---

## Plan de test

### Tests unitaires (`PrescriptionBeLitigeTravailCalculatorTest`)

- [ ] `EX_CONTRAT_GENERAL` — 1 an exact écoulé → `PRESCRIT` (joursRestants = 0).
- [ ] `EX_CONTRAT_GENERAL` — 11 mois écoulés → `IMMINENT`.
- [ ] `EX_CONTRAT_GENERAL` — 3 mois → `NON_PRESCRIT`.
- [ ] `EX_CONTRAT_CCT_109` — délai 1 an, regleAppliquee = `1_AN_CCT_109_ART_11`.
- [ ] `PENDANT_CONTRAT` — 5 ans appliqués.
- [ ] `ARRIERES_SALAIRE` — 5 ans appliqués.
- [ ] `dateActionEnvisagee` ≤ `dateRupture` → erreur de validation.
- [ ] Frontière exacte (joursRestants = 30) → `IMMINENT` (inclus).
- [ ] Frontière exacte (joursRestants = 31) → `NON_PRESCRIT`.

### Tests d'intégration (`PrescriptionBeLitigeTravailControllerIT`)

- [ ] `POST` workspace BE → 200 + persistance.
- [ ] `POST` workspace FR → 404.
- [ ] `POST` case_file d'un autre workspace → 404.
- [ ] `GET` après POST → 200 avec dernière analyse.
- [ ] `GET` sans POST préalable → 404.
- [ ] Validation Bean : `typeCreance` manquant → 400.

### Isolation workspace

- [x] Applicable — un utilisateur du workspace A ne peut pas POST/GET sur un `caseFileId` du workspace B (404).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Workspace context** — nouveau gate `workspaceCountry=BELGIUM` strict côté controller. Composants impactés :
  - `PrescriptionBeLitigeTravailController` — gate dans le service via `WorkspaceMemberRepository` + check `country`.
  - Pattern réplique celui de `ContestationAreController` (F-DT-35).
- [x] Auth / Principal, Plans / limites, Navigation / routing — non touchés.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|---|---|---|
| `LegalDomainPromptBuilder` (branche BE Travail) | Ajout `critereCode` BE_PRESCRIPTION_* | `LegalDomainPromptBuilderTest` BE (vérifier émission des nouveaux codes sans casser les codes existants) |
| `CaseAnalysisResponse.TravailExtractedData` | Ajout `dateRuptureContrat` + `motifRupture` | `CaseAnalysisResponseTest` (rétrocompatibilité des constructeurs / désérialisation) |
| `CritereCodeIntegrityIT` (SF-250-11) | Nouveaux codes émis → doivent être dans `KNOWN_FRONTEND_CRITERE_CODES` | À mettre à jour côté frontend dans SF-207-01b |

### Smoke tests E2E concernés

- [x] Aucun — pas de route Angular, pas d'auth modifié, pas de workspace context modifié au sens routing.

---

## Dépendances

### Subfeatures bloquantes

- Aucune. F-207 démarre indépendamment des autres SF F-207 (les 8 outils sont autonomes).

### Questions ouvertes impactées

- [x] Aucune.

---

## Notes et décisions

- **Pattern de référence : `ContestationAre*` (F-DT-35)** — 10 fichiers + migration `158-create-contestation-are-analyses.xml`. L'agent **mirroir** ce pattern, adapté à la substance BE.
- **Sources juridiques BE strictes** — la mémoire `feedback_belgique_never_forget` interdit le calque FR. Les délais et règles ci-dessus viennent de la **Loi belge du 3 juillet 1978 relative aux contrats de travail** (art. 15) et de la **CCT 109** (motivation du licenciement). Pas de transposition d'art. L.1471-1 FR.
- **Outil P1 transversal** — la prescription est vérifiée en premier pour tout dossier post-rupture BE. Visibilité `ALWAYS_ON` BE (cf. étape 0 bis, à acter dans SF-207-01b côté `TOOL_REGISTRY`).
- **`dateRuptureContrat` est un champ de pré-fill réutilisable** par les autres SF F-207 (C4, contestation, AT, RCC, outplacement) — gain d'un facteur sur toute la vague.
