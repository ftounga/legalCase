# Mini-spec — F-207 / SF-207-02-backend Outil C4 ONEM — checklist conformité

## Identifiant

`F-207 / SF-207-02-backend`

## Feature parente

`F-207` — P1 Travail BE — 8 outils urgences BE-only

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-207-02-backend-c4-onem-checklist`

## Cadrages amont

Étape 0 (cohérence) et étape 0 bis (cohérence écran) F-207 livrées en PR #1119 (`docs/features/F-207/SF-207-00-coherence.md` + `SF-207-00b-ux-coherence.md`). Verdict global : **GO avec ajustements** (BE-only strict, séquence métier dans `TOOL_REGISTRY`). Pas re-cadrés ici.

---

## Objectif

Calculateur de **conformité du document C4 ONEM** émis par l'employeur belge — vérifie la présence des mentions obligatoires (AR 25/11/1991 art. 92), détecte la mention « faute grave » qui entraîne l'exclusion ONEM 4-52 semaines, et génère le contenu d'une **lettre rectificative** quand des mentions sont manquantes.

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/c4-onem-checklist`

Inputs (`C4OnemChecklistRequest`) — champs du document C4 saisis par l'avocat (pré-remplissables IA) :
- `raisonSocialeEmployeur` (string)
- `numeroBce` (string, 10 chiffres ou null — Banque-Carrefour des Entreprises BE)
- `nomSalarie` (string)
- `numeroNationalRegistre` (string, 11 chiffres ou null — NRN peut être masqué pour des raisons RGPD)
- `dateEntreeService` (date)
- `dateSortieService` (date)
- `categorieOnem` (string ou null — code ONEM du C4, ex. « 9 » pour faute grave)
- `motifExplicite` (string — qualification écrite de la fin de contrat)
- `fauteGraveMentionnee` (boolean)
- `preavisPresteJours` (int ou null)
- `dernierSalaireMensuelBrut` (BigDecimal ou null)

Logique de vérification (`C4OnemChecklistCalculator`) — chaque vérification = un `Mention` enum + booléen présent/absent :

| Mention obligatoire (AR 25/11/1991 art. 92) | Règle |
|---|---|
| `RAISON_SOCIALE_EMPLOYEUR` | non null et non vide |
| `NUMERO_BCE` | non null + 10 chiffres |
| `NOM_SALARIE` | non null et non vide |
| `DATE_ENTREE_SERVICE` | non null |
| `DATE_SORTIE_SERVICE` | non null + ≥ dateEntreeService |
| `CATEGORIE_ONEM` | non null |
| `MOTIF_EXPLICITE` | non null + longueur ≥ 5 caractères |
| `PERIODE_OCCUPATION_COHERENTE` | dateSortie ≥ dateEntree |
| `PREAVIS_PRECISE_SI_NON_FAUTE_GRAVE` | si `fauteGraveMentionnee=false`, `preavisPresteJours` non null |
| `DERNIER_SALAIRE_INDIQUE` | `dernierSalaireMensuelBrut` non null |

Le `NUMERO_NATIONAL_REGISTRE` n'est **pas** obligatoire (peut être masqué).

Verdict :
- `RISQUE_EXCLUSION_FAUTE_GRAVE` si `fauteGraveMentionnee=true` — quel que soit le reste. Risque d'exclusion ONEM 4 à 52 semaines (art. 144 AR 25/11/1991). Recommandation : contestation C4 (renvoyer vers SF-207-03 `contestation-c4-onem`).
- `NON_CONFORME` si ≥ 1 mention obligatoire manque (et pas de faute grave).
- `CONFORME` si toutes mentions présentes et pas de faute grave.

Output (`C4OnemChecklistResponse`) :
```json
{
  "verdict": "CONFORME" | "NON_CONFORME" | "RISQUE_EXCLUSION_FAUTE_GRAVE",
  "mentionsManquantes": ["NUMERO_BCE", "PREAVIS_PRECISE_SI_NON_FAUTE_GRAVE"],
  "fauteGraveDetectee": true,
  "exclusionOnemRange": { "minSemaines": 4, "maxSemaines": 52 } | null,
  "lettreRectificativeProposee": "Madame, Monsieur,…" | null,
  "baseJuridique": "AR du 25 novembre 1991 art. 92 ; art. 144 (exclusion faute grave) ; loi du 3 juillet 1978",
  "etapeSuivante": "CONTESTATION_C4" | "RECTIFICATION_AUPRES_EMPLOYEUR" | "AUCUNE"
}
```

`lettreRectificativeProposee` : template texte (sans en-tête/signature, l'avocat les ajoute) listant les mentions à corriger et demandant un nouveau C4 — généré **seulement** si `NON_CONFORME` et **pas** de faute grave (cas faute grave → contestation, pas rectification).

Persistance : 1 ligne `c4_onem_checklist_analyses` par dossier (unique sur `case_file_id`, mise à jour à chaque POST). Inputs + résultat persistés en JSON (`result_data`).

`GET` du même path renvoie la dernière analyse ou 404 si aucune.

### Cas d'erreur

| Situation | Code | Comportement |
|---|---|---|
| `caseFileId` introuvable / autre workspace / `workspaceCountry !== 'BELGIQUE'` | 404 | Isolation BE-only stricte |
| `dateSortieService < dateEntreeService` | 400 | « date de sortie antérieure à la date d'entrée » |
| `numeroBce` non null mais format invalide (≠ 10 chiffres) | 400 | « format BCE invalide » |
| Champ obligatoire de la requête manquant (Bean Validation) | 400 | Message explicite |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : pattern canonique `DocumentsFinContrat*` (FR) — 9 fichiers Java. Pattern checklist + verdict — à mirrorer adapté BE.
- [x] **Autres pays / domaines / UI** : non applicable — BE-only, Travail uniquement, SF backend pure.
- [x] **Autres flows transversaux** : Workspace context — gate `BELGIQUE` strict (pattern réutilisé de SF-207-01).

### Décision

- [x] Étendu à la seule cible applicable. Aucun pattern partagé nouveau.

---

## Conformité F-IA-04

- [x] **Non applicable** — SF backend pure (frontend = SF-207-02b).

---

## Champs IA à extraire (pré-remplissage)

Pré-fill IA réutilisé / étendu (extension `LegalDomainPromptBuilder` BE Travail) :

| Champ formulaire | Type | Champ source `TravailExtractedData` BE | Extension requise |
|---|---|---|---|
| `dateEntreeService` | date (string ISO) | `dateEntree` (existe déjà côté FR — confirmer BE) | si manquant : ajouter |
| `dateSortieService` | date | `dateRuptureContrat` (livré SF-207-01) | ✅ déjà extrait |
| `fauteGraveMentionnee` | boolean | dérivé de `motifRupture` ∋ "faute grave" | ✅ déjà extrait (livré SF-207-01) |
| `categorieOnem`, `motifExplicite`, `raisonSocialeEmployeur`, `numeroBce`, `nomSalarie`, `preavisPresteJours`, `dernierSalaireMensuelBrut` | divers | nouveaux champs `TravailExtractedData` BE — extension ce SF | [x] record + [x] prompt |

Émission `critereCode` BE distincts : `BE_C4_RAISON_SOCIALE_EMPLOYEUR`, `BE_C4_NUMERO_BCE`, `BE_C4_DATE_ENTREE`, `BE_C4_DATE_SORTIE`, `BE_C4_CATEGORIE_ONEM`, `BE_C4_MOTIF`, `BE_C4_FAUTE_GRAVE`, `BE_C4_PREAVIS`, `BE_C4_DERNIER_SALAIRE`.

---

## Critères d'acceptation

- [ ] `POST` retourne `CONFORME` quand toutes mentions présentes et `fauteGraveMentionnee=false`.
- [ ] `POST` retourne `NON_CONFORME` avec liste précise des `mentionsManquantes` quand ≥ 1 mention obligatoire absente.
- [ ] `POST` retourne `RISQUE_EXCLUSION_FAUTE_GRAVE` avec range 4-52 semaines quand `fauteGraveMentionnee=true`.
- [ ] `NON_CONFORME` + non faute grave → `lettreRectificativeProposee` non null, contenant chaque mention manquante.
- [ ] Faute grave + autres mentions manquantes → verdict = `RISQUE_EXCLUSION_FAUTE_GRAVE` (priorité), `lettreRectificativeProposee` = null, `etapeSuivante = CONTESTATION_C4`.
- [ ] `numeroBce` au format invalide → 400.
- [ ] Workspace FR / autre workspace → 404 (isolation BE-only stricte).
- [ ] `GET` après `POST` → 200 + dernière analyse persistée.
- [ ] Critères F-IA-03 BE émis dans les prompts (`CritereCodeIntegrityIT` reste vert).
- [ ] `TravailExtractedData` BE étendu avec les nouveaux champs C4 — rétrocompat constructeurs.

---

## Périmètre

### Hors scope

- Frontend (SF-207-02b).
- Contestation C4 ONEM (SF-207-03 — outil distinct, déclenché en aval).
- Vérification des éléments du C4.2 (volet de contestation administrative — autre document).
- Génération réelle d'un PDF — le template est du texte brut, le rendu Word / PDF est hors scope (l'avocat le met dans son traitement de texte).

---

## Contraintes de validation

| Champ | Obligatoire | Format / Validation |
|---|---|---|
| `nomSalarie` | Oui (à la requête) | non vide |
| `dateEntreeService` | Oui | ISO 8601 date, ≤ today |
| `dateSortieService` | Oui | ISO 8601 date, ≥ `dateEntreeService` |
| `numeroBce` | Non (peut être absent du C4 — c'est alors une mention manquante) | si fourni : exactement 10 chiffres |
| `numeroNationalRegistre` | Non | si fourni : 11 chiffres |
| `fauteGraveMentionnee` | Oui | boolean |
| autres | Non | validation simple (length / range) |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/c4-onem-checklist` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/c4-onem-checklist` | OIDC | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `c4_onem_checklist_analyses` | INSERT / UPDATE / SELECT | 1 ligne par `case_file_id` (unique). Colonnes : `id` (UUID), `case_file_id` (FK CASCADE, unique), `result_data` (TEXT JSON), `created_at`, `updated_at`. |

### Migration Liquibase

- [x] Oui — `XXX-create-c4-onem-checklist-analyses.xml` (prochain numéro). Réversible.

### Composants à créer (pattern `DocumentsFinContrat*` à mirrorer)

Sous `backend/src/main/java/fr/ailegalcase/casefile/` :
- `C4OnemChecklistAnalysis.java` — entité JPA.
- `C4OnemChecklistRepository.java` — `JpaRepository<…, UUID>` + `findByCaseFileId`.
- `C4OnemChecklistMention.java` — enum des 10 mentions vérifiées.
- `C4OnemChecklistRequest.java` — DTO POST avec Bean Validation.
- `C4OnemChecklistResult.java` — record verdict (mentionsManquantes, fauteGraveDetectee, exclusionOnemRange, lettreRectificativeProposee, baseJuridique, etapeSuivante).
- `C4OnemChecklistResponse.java` — DTO GET.
- `C4OnemChecklistService.java` — orchestration : gate workspace `BELGIQUE`, gate appartenance caseFile, validation, calcul, persistance.
- `C4OnemChecklistCalculator.java` — fonction pure. Logique des 10 vérifications + verdict + génération du template de lettre rectificative.
- `C4OnemChecklistController.java` — POST + GET.

Extensions :
- `backend/src/main/java/fr/ailegalcase/analysis/LegalDomainPromptBuilder.java` — branche BE Travail : ajout des champs C4 + émission des 9 `critereCode` BE_C4_*.
- `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisResponse.java` — `TravailExtractedData` : ajout des champs `raisonSocialeEmployeur`, `numeroBce`, `categorieOnem`, `motifExplicite` (string), `preavisPresteJours` (Integer), `dernierSalaireMensuelBrut` (BigDecimal) — strings ISO partout pour les dates (déjà couvert par `dateRuptureContrat`). **Rétrocompat constructeurs obligatoire.**

---

## Plan de test

### Tests unitaires (`C4OnemChecklistCalculatorTest`)

- [ ] Toutes mentions présentes, pas faute grave → `CONFORME`.
- [ ] `numeroBce` null → `NON_CONFORME` + `NUMERO_BCE` dans `mentionsManquantes`.
- [ ] `dateSortie < dateEntree` → mention `PERIODE_OCCUPATION_COHERENTE` manquante (en plus de la validation 400 côté service).
- [ ] `fauteGraveMentionnee=true` + tout OK ailleurs → `RISQUE_EXCLUSION_FAUTE_GRAVE` + `exclusionOnemRange` = 4-52 + `etapeSuivante=CONTESTATION_C4`.
- [ ] `fauteGraveMentionnee=true` + autres mentions manquantes → verdict reste `RISQUE_EXCLUSION_FAUTE_GRAVE` (priorité), `lettreRectificativeProposee=null`.
- [ ] `fauteGraveMentionnee=false` + `preavisPresteJours=null` → `PREAVIS_PRECISE_SI_NON_FAUTE_GRAVE` manquant.
- [ ] `NON_CONFORME` → `lettreRectificativeProposee` mentionne chaque mention manquante explicitement.
- [ ] `motifExplicite` < 5 caractères → mention `MOTIF_EXPLICITE` manquante.
- [ ] `CONFORME` → `lettreRectificativeProposee = null`, `etapeSuivante = AUCUNE`.

### Tests d'intégration (`C4OnemChecklistControllerIT`)

- [ ] `POST` workspace BE → 200 + persistance.
- [ ] `POST` workspace FR → 404.
- [ ] `POST` caseFile autre workspace → 404.
- [ ] `GET` après POST → 200 ; sans POST → 404.
- [ ] Validation Bean : `dateEntreeService` ou `nomSalarie` manquant → 400 ; `numeroBce` format invalide → 400.

### Isolation workspace

- [x] Applicable — pattern réutilisé de SF-207-01.

---

## Analyse d'impact

### Préoccupations transversales

- [x] **Workspace context** — gate `workspaceCountry=BELGIQUE` (pattern SF-207-01).
- [x] Auth / Plans / Navigation : non touchés.

### Composants impactés (non-régression)

| Composant | Impact | Test |
|---|---|---|
| `LegalDomainPromptBuilder` branche BE Travail | Ajout des 9 `critereCode` BE_C4_* + extraction des champs | `LegalDomainPromptBuilderTest` |
| `CaseAnalysisResponse.TravailExtractedData` | Ajout 6 champs (rétrocompat constructeurs) | `CaseAnalysisResponseTest` |
| `CritereCodeIntegrityIT` (SF-250-11) | Doit rester vert | Les codes BE_C4_* émis dans le prompt sans front correspondant — `expected_value:null` patron |

### Smoke E2E

- [x] Aucun — pas de route Angular ni d'auth modifié.

---

## Dépendances

### Subfeatures bloquantes

- SF-207-01-backend (PR #1119, mergée) — pour les champs `TravailExtractedData` BE déjà étendus (`dateRuptureContrat`, `motifRupture`).
- SF-207-01b (PR #1121, mergée) — pour le modèle frontend & TOOL_REGISTRY (frontend SF-207-02b s'appuiera dessus).

---

## Notes et décisions

- **Pattern de référence : `DocumentsFinContrat*` (9 fichiers FR)** — pattern checklist + verdict + template texte. À mirrorer.
- **Sources juridiques BE strictes** : AR du 25 novembre 1991 portant réglementation du chômage (art. 92 mentions obligatoires ; art. 144 exclusion faute grave). Loi du 3 juillet 1978 (contrats de travail). Pas de transposition FR.
- **Priorité du verdict** : `RISQUE_EXCLUSION_FAUTE_GRAVE` > `NON_CONFORME` > `CONFORME`. La faute grave domine — exclusion ONEM = enjeu pécuniaire majeur (jusqu'à 1 an d'allocations).
- **Lettre rectificative en texte brut** — pas de PDF/Word (hors scope). L'avocat la copie dans son traitement de texte.
- **NRN non bloquant** — peut être masqué pour RGPD ; ne fait pas partie des mentions obligatoires vérifiées.
