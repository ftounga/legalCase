# SF-212-07 — Backend : outil décisionnel « CSP/CRP — conformité de la proposition »

> Feature F-212. Outil : `F-DT-44-csp-crp-conformite`. Fondement : L. 1233-65 à L. 1233-70 CT ; accord national interprofessionnel CSP du 19/07/2011 révisé ; DARES.

## Objectif

Fournir le moteur backend qui vérifie la conformité de la proposition de CSP (Contrat de Sécurisation Professionnelle) lors d'un licenciement économique dans une entreprise de moins de 1 000 salariés, et calcule l'ASP (Allocation Spécifique de Reclassement) estimée.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/csp-crp-conformite` + `GET`.

L'analyseur vérifie :
- **Obligation de proposition** (L. 1233-66) : obligation de proposer le CSP lors d'un licenciement économique dans entreprise < 1 000 salariés. Délai de réponse du salarié : 21 jours calendaires.
- **Contenu de la proposition** : document d'information remis, mention du délai de réflexion, coordonnées de l'opérateur CSP.
- **Date de remise** : lors de l'entretien préalable ou, si PSE, à la date de notification individuelle.
- **ASP estimée** : 75 % du salaire journalier de référence pendant 12 mois (régime de droit commun) — à distinguer de l'ARE classique (57-40 % selon palier). Base SJR = rémunération brute des 12 derniers mois / 365 × coefficient.
- **Adhésion ou refus** : impact sur les droits — adhésion = rupture amiable hors préavis (L. 1233-67) ; refus = licenciement normal + préavis.

Verdict `ConformiteCsp` : `CONFORME` / `PARTIELLEMENT_CONFORME` / `NON_CONFORME` + `AspEstimeeEuros`.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- `country` ≠ `FRANCE` → 422 (CSP FR-only).
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-08)

```
POST /api/v1/case-files/{caseFileId}/csp-crp-conformite
Request {
  effectifEntreprise: int,                     // doit être < 1000 pour obligation CSP
  cspPropose: boolean,
  documentInformationRemis: boolean,
  delaiReflexionMentionne: boolean,
  dateRemise: LocalDate|null,
  dateEntretienPrealable: LocalDate|null,
  adhesionSalarie: boolean|null,               // null = inconnu
  salaireMensuelBrutEuros: double,
  remunerationBrute12MoisEuros: double
}
Response 200 {
  ...inputs (snapshot),
  obligationCspApplicable: boolean,
  conformiteCsp: CONFORME|PARTIELLEMENT_CONFORME|NON_CONFORME,
  scoreConformite: int,
  pointsNonConformite: [{code, libelle, fondement}],
  aspEstimeeJournaliereEuros: double,
  aspEstimeeAnnuelleEuros: double,
  dureeAspMois: int,
  basesJuridiques: [String],
  messages: [String],
  country: "FRANCE",
  calculatedAt: Instant
}
GET …/csp-crp-conformite → 200 | 204
```

`critereCode` F-IA-03 : `DT44_OBLIGATION_CSP`, `DT44_DOCUMENT_REMIS`, `DT44_DELAI_REFLEXION`, `DT44_DATE_REMISE`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `csp_detail` :
`cspEffectifEntreprise`, `cspPropose`, `cspDocumentRemis`, `cspDateRemise`, `cspAdhesion`, `cspSalaireMensuelBrut`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. Effectif > 1 000 salariés → `obligationCspApplicable = false`, message explicatif.
2. CSP non proposé alors que effectif < 1 000 → `NON_CONFORME`, facteur `DT44_OBLIGATION_CSP`.
3. Document information non remis → facteur `DT44_DOCUMENT_REMIS`.
4. Délai de réflexion non mentionné → facteur `DT44_DELAI_REFLEXION`.
5. ASP calculée correctement (75 % SJR × 12 mois).
6. 422 hors `FRANCE`.
7. Isolation workspace → 404.
8. `tool_id=F-DT-44-csp-crp-conformite` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `CspCrpConformiteCalculatorTest`** : effectif > 1 000 ; CSP non proposé ; chaque point de non-conformité ; calcul ASP.
- **IT `CspCrpConformiteControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `csp_crp_conformite_analyses`.
- **Seed** : `tool_id=F-DT-44-csp-crp-conformite`, `trigger_field=csp_propose`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard.

## Hors périmètre

- Frontend (→ SF-212-08).
- Congé de reclassement entreprises ≥ 1 000 salariés (couvert par F-DT-45 — P3, F-218).
