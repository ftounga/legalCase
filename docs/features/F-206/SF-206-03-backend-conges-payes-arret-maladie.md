# SF-206-03 — Backend : outil décisionnel « congés payés acquis pendant arrêt maladie »

> Feature F-206 — P1 Travail FR — 4 outils d'urgences procédurales.
> Outil : `F-DT-75-conges-payes-arret-maladie`. Fondement : loi 22/04/2024 (art. 37), L. 3141-5 / L. 3141-5-1 CT, transposition Cass. soc. 13/09/2023.

## Objectif

Fournir le moteur backend qui chiffre le **rappel de congés payés** acquis par le salarié pendant ses arrêts maladie non décomptés et détermine le **délai d'action** encore ouvert.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/conges-payes-arret-maladie` reçoit la saisie de l'avocat, calcule via un `Calculator` stateless, persiste un snapshot, renvoie le chiffrage. `GET` renvoie le dernier snapshot.

Règles de calcul :
- **Maladie non professionnelle** : 2 jours ouvrables de congés payés par mois d'arrêt, **plafonné à 24 jours ouvrables par an** (L. 3141-5-1).
- **AT / maladie professionnelle** : 2,5 jours ouvrables par mois, **sans limite de durée** (la limite d'un an est supprimée).
- Jours de rappel = jours acquis − jours déjà accordés/décomptés par l'employeur.
- Valorisation indicative = jours de rappel × salaire journalier (méthode du dixième : `salaireBrutMensuel × 12 / 10 / 24`, indicatif).
- **Délai d'action** : salarié encore en poste → forclusion de 2 ans à compter du 24/04/2024 (date butoir **24/04/2026**) pour la période antérieure à la loi ; salarié sorti → prescription triennale L. 3245-1, soit `dateRuptureContrat + 3 ans`.

Verdict (`Verdict`) : `RAPPEL_SIGNIFICATIF` / `RAPPEL_LIMITE` / `PAS_DE_RAPPEL` / `ACTION_FORCLOSE` (délai d'action dépassé à la date du calcul).

## Cas d'erreur

- `caseFileId` inexistant / hors workspace → 404 ; domaine ≠ `DROIT_DU_TRAVAIL` → 422 ; `country` ≠ `FRANCE` → 422.
- `nombreMoisArret` ≤ 0, `salaireBrutMensuel` < 0, `salarieEncoreEnPoste=false` sans `dateRuptureContrat` → 400.

## Contrat API (figé — référence pour SF-206-04)

```
POST /api/v1/case-files/{caseFileId}/conges-payes-arret-maladie
Request {
  typeArret: enum MALADIE_NON_PROFESSIONNELLE|ACCIDENT_TRAVAIL_MALADIE_PRO,
  nombreMoisArret: int,                       // nombre de mois d'arrêt cumulés
  salarieEncoreEnPoste: boolean,
  dateRuptureContrat: LocalDate|null,         // requis si salarieEncoreEnPoste=false
  joursCpDejaAccordes: BigDecimal,            // jours déjà décomptés par l'employeur (défaut 0)
  salaireBrutMensuel: BigDecimal
}
Response 200 {
  ...6 champs input (snapshot),
  verdict: RAPPEL_SIGNIFICATIF|RAPPEL_LIMITE|PAS_DE_RAPPEL|ACTION_FORCLOSE,
  joursCpAcquis: BigDecimal,
  joursCpRappel: BigDecimal,
  valorisationIndicativeEur: BigDecimal,
  dateLimiteAction: LocalDate,                // échéance dérivée
  actionEncoreOuverte: boolean,
  basesJuridiques: [String],
  messages: [String],
  country: "FRANCE",
  calculatedAt: Instant
}
GET …/conges-payes-arret-maladie → 200 | 204
```

`critereCode` F-IA-03 : `DT75_TYPE_ARRET`, `DT75_DUREE_ARRET`, `DT75_SALARIE_EN_POSTE`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `conges_payes_arret_maladie_detail` :
`cpArretMaladieType`, `cpArretMaladieNombreMois`, `cpArretMaladieSalarieEnPoste`, `cpArretMaladieDateRupture`, `cpArretMaladieJoursDejaAccordes`.
(`salaireBrutMensuel` est déjà extrait par le pré-remplissage existant.)
Extension `LegalDomainPromptBuilder` pour l'extraction.

## Critères d'acceptation

1. Maladie non pro, 18 mois → 36 j théoriques, plafonné à 24 j (sur la part annuelle) ; AT/MP, 18 mois → 45 j sans plafond.
2. `joursCpRappel` = `joursCpAcquis − joursCpDejaAccordes`, jamais négatif.
3. Salarié en poste → `dateLimiteAction = 2026-04-24` ; salarié sorti → `dateRuptureContrat + 3 ans`.
4. `dateLimiteAction` < date du calcul → `verdict=ACTION_FORCLOSE`, `actionEncoreOuverte=false`.
5. `joursCpRappel = 0` → `PAS_DE_RAPPEL`.
6. 422 hors domaine / hors `FRANCE` ; 404 cross-workspace ; 400 sur entrées invalides.
7. Pré-remplissage : les 5 champs `conges_payes_arret_maladie_detail` extraits par le prompt.

## Plan de test

- **UT `CongesPayesArretMaladieCalculatorTest`** : plafonnement maladie non pro ; absence de plafond AT/MP ; rappel net après déduction ; forclusion 24/04/2026 ; prescription 3 ans salarié sorti ; `ACTION_FORCLOSE`.
- **IT `CongesPayesArretMaladieControllerIT`** : POST + GET, droits, domaine, pays, isolation workspace, validation 400.
- **IT visibilité** : `DecisionToolVisibilityIntegrityIT` vert.

## Tables / endpoints / composants impactés

- **Nouvelle table** `conges_payes_arret_maladie_analyses` — migration Liquibase.
- **Seed** `decision_tool_visibility_rules` : `tool_id=F-DT-75-conges-payes-arret-maladie`, `DROIT_DU_TRAVAIL`, `FRANCE`, `CONTEXTUAL`, `trigger_field=arret_maladie_long_detecte`, `trigger_value=true`.
- **Nouveaux fichiers** `fr.ailegalcase.casefile` : `CongesPayesArretMaladieCalculator`, `…Request`, `…Response`, `…Input`, `…Result`, `…Analysis`, `…Repository`, `…Service`, `…Controller`.
- **Modifiés** : `CaseAnalysisResponse.java`, `LegalDomainPromptBuilder.java`, `CaseFileDashboardService.java` (mapper `DashboardTile`, thème `INDEMNITES`).

## Préoccupations transversales

**Outil décisionnel métier** — calculateur de rappel. Invariant « un outil = une situation » : situation = rappel de congés payés acquis pendant arrêt maladie (rappel de droits, **pas** une rupture — d'où le thème dashboard `INDEMNITES` et le groupe F-169 « Rappels et indemnités salariales »). Pas d'impact auth/workspace/plan/navigation.

## Hors périmètre

- Frontend (→ SF-206-04).
- Liste détaillée période par période des arrêts (saisie agrégée en `nombreMoisArret`).
- Branchement de `dateLimiteAction` dans `case_deadlines` (exposée, intégration F-69 hors périmètre).
