# Mini-spec — F-FA-24 / SF-FA-24-03 Backend validité testament

## Identifiant

`F-FA-24 / SF-FA-24-03`

## Feature parente

`F-FA-24` — Droit des successions (chantier ~9-11 SF — déjà livrées : SF-01 dévolution légale, SF-02 frontend dévolution).

## Statut

`in-progress`

## Date de création

2026-04-26

## Branche Git

`feat/SF-FA-24-03-backend-testament`

---

## Objectif

3ème SF backend de F-FA-24 — calculator + endpoint d'analyse de la **validité d'un testament** (FR — art. 967-1035 + 901-911 Cciv) — détermine la forme du testament (olographe / authentique / mystique / international), identifie les vices (capacité, forme, consentement, révocation) et rend un verdict de validité (VALIDE / CONTESTABLE / NUL).

---

## Comportement attendu

### Cas nominal

L'avocat saisit la forme du testament + critères de forme, capacité et consentement → l'outil applique les exigences légales propres à chaque forme + capacité art. 901-902 + vices art. 901+ + révocation art. 1035-1038 → renvoie un verdict, la liste des vices identifiés, les éventuelles actions complémentaires (réduction art. 920+) et la base juridique.

#### 4 formes de testament FR

1. **TESTAMENT_OLOGRAPHE** (art. 970) : entièrement écrit, daté et signé de la main du testateur.
   - Critères : `ecritureManuscritIntegrale`, `dateComplete`, `signatureTestateur`.
2. **TESTAMENT_AUTHENTIQUE** (art. 971-975) : reçu par 2 notaires ou 1 notaire + 2 témoins.
   - Critères : `presenceNotaireEtTemoinsConforme`, `dicteEnPresence`, `lectureFinaleAuTestateur`, `signaturesCompletes`.
3. **TESTAMENT_MYSTIQUE** (art. 976-980) : remis cacheté à un notaire devant 2 témoins.
   - Critères : `remiseSousPliCache`, `declarationDevant2Temoins`, `acteSuscriptionNotaire`.
4. **TESTAMENT_INTERNATIONAL** (Convention Washington 1973 — peu utilisé) : forme dérogatoire pour situations internationales.
   - Critères : `respecteFormeWashington`, `signaturesCompletes`.

#### Capacité (art. 901-911 Cciv)

- `ageAuMoins16Ans` (art. 904 — un mineur < 16 ans ne peut tester).
- `saineDEsprit` (art. 901 — sain d'esprit nécessaire).
- `majeurProtegeAvecAssistance` (si majeur sous tutelle/curatelle : art. 476, 470).

#### Vices de consentement (art. 901+)

- `dolViolence` ou `erreurSubstantielle` → cause de nullité absolue.

#### Révocation (art. 1035-1038)

- `testamentPosterieurContradictoire` (art. 1036) → révocation tacite.
- `dechirureVolontaireOriginal` (art. 1038) → révocation matérielle.

#### Quotité disponible / réserve (art. 913-920)

- `legsExcedeQuotiteDisponible` (boolean) → ne nullifie pas le testament mais ouvre **action en réduction** (art. 920+) — délai 5 ans à compter de l'ouverture de la succession (art. 921).

#### Verdict

- **VALIDE** : forme respectée + capacité + pas de vices + pas de révocation.
- **CONTESTABLE** : un ou plusieurs critères douteux mais pas tranché — `vicesIdentifies` non vide, le juge tranchera.
- **NUL** : vice rédhibitoire (incapacité, vice consentement, forme manifestement non respectée, révocation).

#### Délai d'action

`delaiContestationAns` = 5 ans à compter de la connaissance du vice (art. 1304 Cciv — droit commun de la nullité).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Body absent | "Corps de requête requis" | 400 |
| `formeTestament` null | "Forme du testament requise" | 400 |
| `dateRedaction` null | "Date de rédaction du testament requise" | 400 |
| `ageTestateurAnsRedaction` < 0 ou > 130 | "Âge du testateur invalide" | 400 |
| Workspace pays ≠ FRANCE | "Outil non disponible pour le pays X — backlog jumeau F-FA-24-BE-testament" | 400 |
| Dossier ≠ DROIT_FAMILLE | "Ce dossier n'est pas un dossier de droit de la famille" | 400 |
| Dossier d'un autre workspace | "Case file not found" | 404 |
| GET sans POST préalable | "Aucune analyse de validité de testament trouvée pour ce dossier" | 404 |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils décisionnels famille FR** : F-FA-24 SF-01 dévolution légale (PR #651), F-FA-16 communauté universelle (PR #648), F-FA-17 partage judiciaire — **classement** : déjà séparés un par situation. Le testament concerne une situation distincte (acte unilatéral du défunt). **Non applicable** : aucun outil existant ne couvre la validité d'un testament.
- [x] **Autres pays** : Belgique → règles différentes (art. 895+ CC BE — formes équivalentes mais quelques exigences différentes notamment pour le testament international + différences dans les délais de prescription). **Backlog jumeau F-FA-24-BE-testament** prévu (mention dans la mini-spec et message d'erreur explicite si workspace BE).
- [x] **Autres domaines** : DROIT_DU_TRAVAIL / DROIT_IMMIGRATION → **non applicable** (testament = strictement DROIT_FAMILLE).
- [x] **UI patterns** : section décisionnelle Angular F-IA-04 → SF-FA-24-04 frontend future (séquentiel).
- [x] **Auth / workspace** : pattern `CurrentUserResolver` + `WorkspaceMemberRepository` + gate `legalDomain == "DROIT_FAMILLE"` + gate `country == "FRANCE"` (cf. SF-FA-24-01 dévolution) → **réutilisé tel quel**.

### Verdict

Pattern aligné F-FA-24 SF-01 (DevolutionLegale) et F-FA-16 (CommunauteUniverselle). Aucune duplication créée — outil isolé, single-country, single-domain.

---

## Impact par domaine métier

- **Sensibilité au domaine** : forte — feature 100% droit famille FR (testament, succession). Aucun impact DROIT_DU_TRAVAIL ou DROIT_IMMIGRATION.
- **Sensibilité au pays** : forte — règles propres au Code civil français. Belgique = backlog jumeau **F-FA-24-BE-testament** (CC BE art. 895+ avec différences de forme et de délais).

---

## Parité des domaines métier (outil de niveau 5 — scoring/analyse de validité)

L'outil est une **analyse de validité** (niveau 5 — verdict VALIDE/CONTESTABLE/NUL). Application des règles de parité :

| Domaine | Équivalent existant | Décision |
|---------|---------------------|----------|
| DROIT_DU_TRAVAIL | N/A — concept inapplicable | Non applicable, justifié |
| DROIT_IMMIGRATION | N/A — concept inapplicable | Non applicable, justifié |
| DROIT_FAMILLE FRANCE | **Cette SF** | En cours |
| DROIT_FAMILLE BELGIQUE | Règles propres CC BE art. 895+ (formes similaires mais différences) | **Backlog jumeau F-FA-24-BE-testament** à ouvrir |

---

## Critères d'acceptation

1. POST `/api/v1/case-files/{id}/testament-validite-analysis` avec body valide (FR, DROIT_FAMILLE) → 200 + `verdictValidite` + `formeTestament` + `vicesIdentifies` + `actionEnReductionPossible` + `delaiContestationAns` + `baseJuridique` + `formule` + `messages`.
2. **Olographe** valide (manuscrit + daté + signé + capacité OK) → `verdictValidite=VALIDE`, `vicesIdentifies` vide.
3. **Olographe** non manuscrit (tapuscrit) → `verdictValidite=NUL`, vice "FORME_OLOGRAPHE_NON_MANUSCRITE".
4. **Olographe** non daté → `verdictValidite=NUL`, vice "FORME_OLOGRAPHE_NON_DATE".
5. **Authentique** valide (notaires + témoins + dictée + lecture + signatures) → `verdictValidite=VALIDE`.
6. **Authentique** sans dictée → `verdictValidite=NUL`, vice "FORME_AUTHENTIQUE_DICTEE_MANQUANTE".
7. **Mystique** valide → `verdictValidite=VALIDE`.
8. **Mystique** sans pli cacheté → `verdictValidite=NUL`.
9. **International** valide → `verdictValidite=VALIDE`.
10. **Mineur < 16 ans** → `verdictValidite=NUL`, vice "INCAPACITE_MINEUR_MOINS_16_ANS".
11. **Insanité d'esprit** → `verdictValidite=NUL`, vice "INSANITE_ESPRIT".
12. **Majeur sous tutelle sans assistance** → `verdictValidite=CONTESTABLE`, vice "MAJEUR_PROTEGE_SANS_ASSISTANCE".
13. **Vice de consentement (dol)** → `verdictValidite=NUL`, vice "VICE_CONSENTEMENT_DOL".
14. **Testament postérieur contradictoire** → `verdictValidite=NUL`, vice "REVOCATION_TESTAMENT_POSTERIEUR".
15. **Déchirure volontaire** → `verdictValidite=NUL`, vice "REVOCATION_DECHIRURE".
16. **Legs excédant quotité disponible** sur testament olographe valide → `verdictValidite=VALIDE` + `actionEnReductionPossible=true`.
17. POST workspace BE → 400 mentionnant `BELGIQUE` et backlog jumeau.
18. POST dossier DROIT_DU_TRAVAIL FR → 400.
19. POST dossier d'un autre workspace → 404.
20. POST upsert (2ème POST sur même dossier) → remplace l'analyse précédente (1 ligne via UNIQUE).
21. GET après POST → renvoie l'analyse persistée.
22. GET sans POST → 404.
23. `baseJuridique` contient `967`, `970`, `901`, `1035`, `920`.
24. Migration Liquibase 182 crée la table + UNIQUE + insert visibility rule ALWAYS_ON DROIT_FAMILLE FRANCE priority 91 UUID `f1a04001-0000-0000-0000-ee0000000182` tool_id `F-FA-24-testament-validite`.

---

## Plan de test

### Tests unitaires (`TestamentValiditeCalculatorTest`) — ≥ 18

1. Olographe valide → VALIDE.
2. Olographe non manuscrit → NUL + vice forme.
3. Olographe non daté → NUL.
4. Olographe non signé → NUL.
5. Authentique valide → VALIDE.
6. Authentique dictée manquante → NUL.
7. Authentique lecture manquante → CONTESTABLE.
8. Authentique signatures incomplètes → NUL.
9. Mystique valide → VALIDE.
10. Mystique sans pli cacheté → NUL.
11. International valide → VALIDE.
12. International forme Washington non respectée → NUL.
13. Mineur < 16 ans → NUL + vice incapacité.
14. Insanité d'esprit → NUL.
15. Majeur protégé sans assistance → CONTESTABLE.
16. Vice consentement (dol/violence) → NUL.
17. Testament postérieur contradictoire → NUL + vice révocation.
18. Déchirure volontaire originale → NUL + vice révocation.
19. Legs excédant quotité disponible sur olographe valide → VALIDE + actionEnReductionPossible=true.
20. Validation : country null → IllegalArgumentException.
21. Validation : country BELGIQUE → IllegalArgumentException mentionnant feature jumelle.
22. `baseJuridique` contient 967, 970, 971, 901, 1035, 920.
23. `formule` contient forme + verdict + score.

### Tests intégration (`TestamentValiditeControllerIT`) — ≥ 7

1. POST FR DROIT_FAMILLE olographe valide → 200 + verdictValidite=VALIDE + formeTestament=TESTAMENT_OLOGRAPHE.
2. POST FR DROIT_FAMILLE olographe non manuscrit → 200 + verdictValidite=NUL.
3. POST FR DROIT_FAMILLE legs excédant quotité → 200 + actionEnReductionPossible=true.
4. POST workspace BE → 400.
5. POST DROIT_DU_TRAVAIL FR → 400.
6. POST autre workspace → 404.
7. POST formeTestament manquant → 400.
8. POST upsert remplace → 200 + nouveau verdict.
9. GET après POST → 200 + données persistées.
10. GET sans POST → 404.

### Isolation workspace

Test cross-workspace explicite (workspace A POST sur dossier de workspace B → 404).

---

## Tables / endpoints / composants impactés

### Tables
- **Nouvelle** : `testament_validite_analyses` (1:1 case_files via UNIQUE) — créée par migration **182-create-testament-validite-analyses.xml**.
- **Modifiée** : `decision_tool_visibility_rules` — INSERT règle ALWAYS_ON DROIT_FAMILLE FRANCE tool_id `F-FA-24-testament-validite` priority 91 UUID `f1a04001-0000-0000-0000-ee0000000182`.

### Endpoints
- `POST /api/v1/case-files/{caseFileId}/testament-validite-analysis` — upsert
- `GET /api/v1/case-files/{caseFileId}/testament-validite-analysis` — lecture

### Composants Java
- `TestamentValiditeRequest` (record)
- `TestamentValiditeResponse` (record)
- `TestamentValiditeResult` (record)
- `TestamentValiditeAnalysis` (entity JPA)
- `TestamentValiditeRepository` (JpaRepository)
- `TestamentValiditeCalculator` (final class — règles métier pures)
- `TestamentValiditeService` (Spring service — orchestration + auth)
- `TestamentValiditeController` (REST endpoint)

---

## Hors périmètre

- **Frontend** : SF-FA-24-04 future.
- **Belgique** : F-FA-24-BE-testament (backlog jumeau).
- **Autres SF de F-FA-24** : donation (893+), réserve héréditaire (913+), action en réduction, partage successoral, indivision successorale, rapport à succession.
- Cas exotiques non couverts en SF-03 :
  - Testament conjonctif (interdit art. 968) — pas un cas usuel.
  - Codicille (modification testamentaire) — traité comme un nouveau testament.
  - Testament international détaillé Convention Washington (vérification champ par champ).
  - Recel successoral (art. 778) — non lié à la validité du testament.
  - Indignité successorale (art. 726-729) — sortie d'un héritier indépendamment du testament.

---

## Contrat API

### POST /api/v1/case-files/{caseFileId}/testament-validite-analysis

**Body**
```json
{
  "formeTestament": "TESTAMENT_OLOGRAPHE",
  "dateRedaction": "2024-03-15",
  "ageTestateurAnsRedaction": 72,
  "saineDEsprit": true,
  "majeurProtegeAvecAssistance": null,
  "ecritureManuscritIntegrale": true,
  "dateComplete": true,
  "signatureTestateur": true,
  "presenceNotaireEtTemoinsConforme": null,
  "dicteEnPresence": null,
  "lectureFinaleAuTestateur": null,
  "signaturesCompletes": null,
  "remiseSousPliCache": null,
  "declarationDevant2Temoins": null,
  "acteSuscriptionNotaire": null,
  "respecteFormeWashington": null,
  "vicesConsentementDol": false,
  "erreurSubstantielle": false,
  "testamentPosterieurContradictoire": false,
  "dechirureVolontaireOriginal": false,
  "legsExcedeQuotiteDisponible": false
}
```

**Réponse 200**
```json
{
  "caseFileId": "...",
  "formeTestament": "TESTAMENT_OLOGRAPHE",
  "verdictValidite": "VALIDE",
  "vicesIdentifies": [],
  "actionEnReductionPossible": false,
  "delaiContestationAns": 5,
  "scoreEligibilite": 100,
  "baseJuridique": "Art. 967-1035, 901-911, 920 et s. Cciv",
  "formule": "Forme TESTAMENT_OLOGRAPHE + verdict VALIDE + 0 vice → score 100",
  "messages": ["..."],
  "country": "FRANCE"
}
```

**Codes enum**
- `formeTestament` : `TESTAMENT_OLOGRAPHE` | `TESTAMENT_AUTHENTIQUE` | `TESTAMENT_MYSTIQUE` | `TESTAMENT_INTERNATIONAL`
- `verdictValidite` : `VALIDE` | `CONTESTABLE` | `NUL`
- `vicesIdentifies[*]` : codes parmi `FORME_OLOGRAPHE_NON_MANUSCRITE`, `FORME_OLOGRAPHE_NON_DATE`, `FORME_OLOGRAPHE_NON_SIGNE`, `FORME_AUTHENTIQUE_NOTAIRES_TEMOINS`, `FORME_AUTHENTIQUE_DICTEE_MANQUANTE`, `FORME_AUTHENTIQUE_LECTURE_MANQUANTE`, `FORME_AUTHENTIQUE_SIGNATURES_INCOMPLETES`, `FORME_MYSTIQUE_PLI_NON_CACHE`, `FORME_MYSTIQUE_TEMOINS`, `FORME_MYSTIQUE_SUSCRIPTION`, `FORME_INTERNATIONAL_WASHINGTON`, `INCAPACITE_MINEUR_MOINS_16_ANS`, `INSANITE_ESPRIT`, `MAJEUR_PROTEGE_SANS_ASSISTANCE`, `VICE_CONSENTEMENT_DOL`, `VICE_CONSENTEMENT_ERREUR`, `REVOCATION_TESTAMENT_POSTERIEUR`, `REVOCATION_DECHIRURE`

---

## Préoccupations transversales

- [x] **Outil décisionnel métier** : F-FA-24 SF-03 = nouvel outil dédié à la validité du testament FR. Scan effectué : aucun outil existant ne le couvre. Le périmètre F-FA-24 (chantier successions) sera découpé en 9-11 SF, chacune = un outil pour une situation distincte (testament, donation, réduction, partage, indivision, rapport...).
- [x] **Auth / Principal** : pattern `OidcUser + Principal` réutilisé tel quel — aucun changement.
- [x] **Workspace context** : pattern `WorkspaceMemberRepository.findByUserAndPrimaryTrue` + gate FRANCE/DROIT_FAMILLE strictement copié de SF-FA-24-01 — aucun changement.

Aucune préoccupation critique modifiée — pas besoin de smoke tests E2E.
