# SF-FA-18-01 — Backend reconnaissance paternelle (art. 316 Cciv)

> **SF-01 du chantier F-FA-18 (Filiation)** — 5-7 SF prévues au total. Futures :
> contestation paternité/maternité (332-335), action en recherche de paternité,
> possession d'état (317), adoption simple (360) et plénière (343).
> Cette première SF démarre par la reconnaissance paternelle volontaire (art. 316).

## Objectif

Exposer un endpoint POST/GET d'analyse de recevabilité d'une **reconnaissance paternelle** volontaire et unilatérale (art. 316 Cciv) côté France, en distinguant les 3 sous-types (prénatale, post-natale lors de l'établissement de l'acte de naissance, post-natale ultérieure).

## Concept métier

La reconnaissance paternelle établit le lien de filiation entre un père et son enfant. Mécanisme **volontaire et unilatéral**. 3 sous-types :

1. **RECONNAISSANCE_PRENATALE** (art. 316 al. 1) — avant la naissance, devant tout officier d'état civil. Effet immédiat à la naissance.
2. **RECONNAISSANCE_POST_NATALE_NAISSANCE** (art. 316 al. 2) — pendant l'établissement de l'acte de naissance. Le père est nommé sur l'acte.
3. **RECONNAISSANCE_POST_NATALE_ULTERIEURE** (art. 316 al. 3) — à tout moment après la naissance, devant officier d'état civil ou notaire.

## Critères de validité

- `consentementLibreDuPere = true` (vice = nullité ; art. 316 + 1130 Cciv)
- `paterniteVraisemblable = true` (présomption — pas d'examen ADN à ce stade)
- `enfantNonReconnuParAutrePere = true` (la mère doit être seule reconnue, ou autre père doit avoir été contesté)
- `procedureRespectee = true` (présence du père OU procuration spéciale notariée)

## Verdict

- **ELEVEE** si tous les critères + sous-type clairement identifié
- **MOYENNE** si paternité vraisemblable mais procédure partiellement contestable (procuration manquante, etc.)
- **FAIBLE** si vice de consentement OU enfant déjà reconnu par autre père OU procédure non respectée

## Effets attendus

- Filiation établie **rétroactivement à la naissance** (date à fournir dans `effetFiliation`)
- Droits-devoirs : autorité parentale (sous conditions art. 372), nom (art. 311-21), succession
- Recours en contestation possible (art. 332-335) par les tiers intéressés dans **10 ans** (`delaiContestationAns`)

## Sortie

`verdictRecevabilite`, `effetFiliation` (date à partir de laquelle la filiation est établie — naissance ou jour de la reconnaissance pour ultérieure), `risquesContestation` (List<String> — vice consentement, autre père reconnu, possession d'état contraire), `documentsRequis` (List<String> : acte naissance, pièce identité père, procuration si applicable, etc.), `delaiContestationAns` (10), `baseJuridique` (`"Art. 316 Cciv + 332-335 + 372 Cciv"`), `formule`, `messages`, `country`.

## Comportement nominal

- POST `/api/v1/case-files/{caseFileId}/reconnaissance-paternelle-analysis` : calcule + persiste (upsert 1:1).
- GET `/api/v1/case-files/{caseFileId}/reconnaissance-paternelle-analysis` : renvoie la dernière analyse (404 si aucune).

## Cas d'erreur

- 400 si critères obligatoires manquants (sousType null, dateNaissanceEnfant null pour types post-natals).
- 400 si workspace BELGIQUE (single-country FR — équivalent BE distinct, art. 327 et s. CC belge → backlog jumeau).
- 400 si dossier non DROIT_FAMILLE.
- 404 si dossier hors workspace de l'utilisateur.
- 404 si GET sans POST préalable.

## Critères d'acceptation vérifiables

1. POST FR + tous critères + sous-type prénatal → verdict `ELEVEE`, effet à la naissance, `delaiContestationAns = 10`.
2. POST FR + tous critères + sous-type post-natal ultérieur → verdict `ELEVEE`, effet rétroactif à la naissance.
3. POST FR + `consentementLibreDuPere = false` → verdict `FAIBLE`, message "vice de consentement".
4. POST FR + `enfantNonReconnuParAutrePere = false` → verdict `FAIBLE`, message "enfant déjà reconnu".
5. POST FR + `procedureRespectee = false` → verdict `FAIBLE` ou `MOYENNE` selon sous-type.
6. `documentsRequis` contient toujours « acte naissance » (sauf prénatale) et « pièce identité père ».
7. `risquesContestation` non vide quand au moins un critère est partiellement défaillant.
8. `baseJuridique` contient « 316 », « 332 » et « 372 ».
9. POST workspace BE → 400 avec message d'orientation backlog jumeau.
10. POST workspace immigration FR → 400.
11. Cross-workspace → 404.
12. Upsert : second POST remplace l'analyse précédente.
13. GET sans POST → 404.

## Plan de test

- **Unit** (`ReconnaissancePaterneleCalculatorTest`, ≥ 15) : verdicts (ELEVEE/MOYENNE/FAIBLE) × 3 sous-types, cumul de défauts, validations (nulls, country BE), `effetFiliation` cohérent, `documentsRequis` adapté au sous-type, `delaiContestationAns = 10`, formule contient score+verdict, baseJuridique 316/332/372.
- **IT** (`ReconnaissancePaterneleControllerIT`, ≥ 7) : POST nominal ELEVEE, POST vice consentement → FAIBLE, POST workspace BE → 400, POST immigration → 400, POST cross-workspace → 404, upsert, GET 404 sans POST.

## Tables / endpoints / composants impactés

- **Migration Liquibase** : `178-create-reconnaissance-paternelle-analyses.xml` — table `reconnaissance_paternelle_analyses` (UNIQUE `case_file_id`) + INSERT `decision_tool_visibility_rules` (ALWAYS_ON, DROIT_FAMILLE, FRANCE, priority **87**, UUID `f1a04001-0000-0000-0000-ee0000000178`, tool_id `'F-FA-18-reconnaissance-paternelle'`).
- **Backend** : `ReconnaissancePaterneleRequest/Response/Result/Analysis/Repository/Calculator/Service/Controller`.

## Hors périmètre

- Frontend (SF-FA-18-02 séparée).
- Belgique — reconnaissance paternelle BE diffère (CC art. 327 et s.) → backlog jumeau.
- Contestation paternité/maternité (art. 332-335) → SF-FA-18-03.
- Action en recherche de paternité (art. 327 et s.) → SF-FA-18-04.
- Possession d'état (art. 317) → SF-FA-18-05.
- Adoption simple/plénière → SF-FA-18-06/07.
- Filiation par PMA (art. 342-9 et s.) couverte par F-FA-27.

## Impact par domaine métier

Feature **sensible au domaine** :
- **Droit du travail / Immigration** : non applicable (404 / 400 par gate).
- **Droit famille** :
  - **France** : couvert par cette SF (art. 316 + 332-335 + 372 Cciv).
  - **Belgique** : régime différent (CC art. 327 et s., reconnaissance subordonnée à consentement maternel) — **feature jumelle au backlog**.

## Parité des domaines métier (outil de niveau 5 — scoring de validité)

- **Droit du travail FR/BE** : non applicable (sphère filiation).
- **Droit immigration FR/BE** : non applicable directement (la filiation peut être un fait soutenant une demande, mais l'établir = droit famille).
- **Droit famille FR** : couvert par cette SF.
- **Droit famille BE** : équivalent existant en CC art. 327 et s. — **à ouvrir au backlog comme feature jumelle F-FA-18-BE**. Justification de l'asymétrie temporaire : régime juridique distinct (consentement maternel obligatoire en Belgique pour la reconnaissance d'un enfant déjà reconnu par la mère, possibilité de refus de la mère, contentieux différent).

## Analyse de cohérence transversale

- **Outils décisionnels existants** : F-FA-18 vient ouvrir le bloc filiation, distinct du bloc divorce (F-FA-08/09/10), du bloc patrimonial (F-FA-04/05/15/16/17), des mesures provisoires (F-FA-12), de la séparation de corps (F-FA-21), du PACS (F-FA-20), de l'ordonnance de protection (F-FA-14), de la PMA (F-FA-27). Pas de doublon. Outil **single-country FRANCE** uniformément avec F-FA-21 (séparation de corps), F-FA-17 (partage judiciaire).
- **Patterns transversaux** : aucun nouveau composant partagé / DTO / directive / service. Réutilisation stricte du pattern `PartageJudiciaire*` (PR #636 mergé) — record Request/Response/Result + Calculator pur statique + Service Spring + Controller REST + Analysis JPA + Repository.

## Préoccupations transversales

- **Auth / Principal** : aucun changement (réutilise pattern existant `CurrentUserResolver` + `OAuthProviderResolver`).
- **Workspace context** : aucun changement (gate `cf.getWorkspace().getCountry()` + `cf.getLegalDomain()`).
- **Plans / limites** : non concerné.
- **Navigation / routing** : aucun ajout (endpoints REST seulement, pas de route Angular).
- **Outil décisionnel métier** : nouveau outil, scan effectué — un outil = une situation métier (reconnaissance volontaire ≠ contestation 332-335 ≠ recherche paternité 327 ≠ adoption 343/360). Pas de mélange dans ce calculator.

## Contrat API (figé pour parallélisation éventuelle)

### POST `/api/v1/case-files/{caseFileId}/reconnaissance-paternelle-analysis`

Body :
```json
{
  "sousType": "RECONNAISSANCE_PRENATALE" | "RECONNAISSANCE_POST_NATALE_NAISSANCE" | "RECONNAISSANCE_POST_NATALE_ULTERIEURE",
  "dateNaissanceEnfant": "2024-03-15",
  "dateReconnaissance": "2024-02-10",
  "consentementLibreDuPere": true,
  "paterniteVraisemblable": true,
  "enfantNonReconnuParAutrePere": true,
  "procedureRespectee": true,
  "presenceParProcuration": false
}
```

Response 200 :
```json
{
  "caseFileId": "uuid",
  "sousType": "RECONNAISSANCE_PRENATALE",
  "verdictRecevabilite": "ELEVEE" | "MOYENNE" | "FAIBLE",
  "scoreEligibilite": 90,
  "effetFiliation": "2024-03-15",
  "risquesContestation": ["..."],
  "documentsRequis": ["..."],
  "delaiContestationAns": 10,
  "baseJuridique": "Art. 316 Cciv + 332-335 + 372 Cciv",
  "formule": "...",
  "messages": ["..."],
  "country": "FRANCE"
}
```

Codes d'erreur : 400 (validation, country BE, domain mismatch), 404 (case file inconnu / autre workspace, GET sans POST).
