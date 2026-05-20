# SF-216-03 — Pension alimentaire enfant FR — backend

## Objectif

Restaurer et compléter le calculateur backend de la pension alimentaire pour enfant (art. 371-2 Cciv) : calculer le montant mensuel selon le barème indicatif de la Cour de cassation (2010, révisé), intégrer l'indexation INSEE et déterminer la charge par enfant selon le mode de résidence.

> Outil `F-FA-02-pension-alimentaire` — DELETE migration 191, à restaurer. Pivot avocat : outil le plus utilisé en pratique (rang 1 Top-10 audit F-191).

## Comportement nominal

- Endpoint `POST/GET /api/v1/case-files/{caseFileId}/pension-alimentaire-enfant`.
- Body :
  - `revenusNetsParent1Eur` (int, requis) — parent débiteur ou le plus aisé
  - `revenusNetsParent2Eur` (int, requis)
  - `nombreEnfants` (int, requis, 1-12)
  - `agesEnfants` (List<Integer>, requis, longueur = nombreEnfants)
  - `modeResidence` (ALTERNEE | PRINCIPALE_PARENT1 | PRINCIPALE_PARENT2)
  - `dateCalcul` (LocalDate, optionnel — pour indexation INSEE à la date souhaitée)
  - `dateOrdonnanceOrDecision` (LocalDate, optionnel — si révision)
- Calculator `PensionAlimentaireEnfantCalculator` détermine :
  - **Montant mensuel brut par enfant** selon grille barème indicatif Cass. (revenus nets débiteur × taux par enfant × coefficient mode résidence).
  - **Coefficient mode résidence** : résidence principale = 1,0 ; résidence alternée = 0,5 ; résidence principale de l'autre parent = 0,0 (à nuancer par la jurisprudence).
  - **Indexation INSEE** : si `dateOrdonnanceOrDecision` fourni, calcule la revalorisation (indice des prix à la consommation INSEE).
  - **Contribution globale** : total mensuel pour l'ensemble des enfants + avertissement sur les charges extraordinaires (frais scolaires, médicaux) à adapter.
- Retourne : `montantParEnfantMensuelEur[]`, `totalMensuelEur`, `baseLegale`, `grilleSourcée`, `indexationAppliquee`, `messages`, `alertes`.
- Persiste 1:1 par dossier (table `pension_alimentaire_enfant_analyses`).

## Cas d'erreur

- `country ≠ FRANCE` → 400 (barème FR — SECAL BE = outil BE-only séparé).
- `legalDomain ≠ DROIT_FAMILLE` → 400.
- `nombreEnfants < 1` ou `agesEnfants.size() ≠ nombreEnfants` → 400.
- Revenus négatifs → 400.
- Workspace mismatch → 404.

## Source juridique

- **art. 371-2 Cciv** — contribution à l'éducation et à l'entretien de l'enfant.
- **Barème indicatif Cour de cassation** (2010, actualisé périodiquement) — taux de l'ordre de 14-18% des revenus pour 1 enfant, 24-28% pour 2, 29-34% pour 3+.
- **art. L. 581-1 + art. L. 582-1 CSS** — recouvrement et indexation.
- **art. 373-2-2 Cciv** — révision en cas de changement de situation.

## Champs IA à extraire (FamilleExtractedData)

**Réutilisés (F-246)** :
- `vie_commune_detection.revenusAnnuelsEpoux1` → converti en mensuel
- `vie_commune_detection.revenusAnnuelsEpoux2` → converti en mensuel
- `filiation_detection_v2.agesEnfantsDetectes`
- `filiation_detection_v2.nombreEnfantsDetecte`

**Nouveaux champs à ajouter à `FamilleExtractedData` + prompt `FAMILLE_INSTRUCTION`** :
- `pensionAlimentaireEnvisagee` (boolean | null) — détecté si mention « pension alimentaire », « contribution entretien », « 371-2 » dans le dossier.
- `modeResidenceEnfantsDetecte` (ALTERNEE | PRINCIPALE_PARENT1 | PRINCIPALE_PARENT2 | null) — extrait d'une ordonnance ou convention.

## Plan de test

- UT calculator : (a) 1 enfant, revenus 3000 € nets → montant dans fourchette barème ; (b) résidence alternée → montant × 0,5 ; (c) 3 enfants → montant cumulé ; (d) indexation avec date d'ordonnance 2 ans passés → revalorisation > 0 ; (e) revenus débiteur = 0 → montant 0 avec message.
- UT service : gates pays + domaine.
- IT controller : POST + GET round-trip.

## Composants impactés

- Migration Liquibase 273 : table `pension_alimentaire_enfant_analyses`.
- Migration Liquibase 274 : INSERT `decision_tool_visibility_rules` CONTEXTUAL `pensionAlimentaireEnvisagee`, `DROIT_FAMILLE`, `FRANCE`, priority 99.
- Java : `PensionAlimentaireEnfantCalculator`, `PensionAlimentaireEnfantResult`, `PensionAlimentaireEnfantAnalysis`, repository, service, controller, `ModeResidenceEnum`.
- `CaseAnalysisResponse.java` — ajout `pensionAlimentaireEnvisagee`, `modeResidenceEnfantsDetecte`.
- `LegalDomainPromptBuilder` — section `FAMILLE_INSTRUCTION`.

## Critères d'acceptation

- AC1 : 1 enfant, débiteur 3000 €/mois nets → montant indicatif entre 420 et 540 €/mois.
- AC2 : résidence alternée → montant réduit de moitié.
- AC3 : `country=BELGIQUE` → 400.
- AC4 : POST puis GET → idempotent.
- AC5 : champ `modeResidenceEnfantsDetecte` extrait correctement d'une pièce contenant « résidence alternée ».

## Hors périmètre

- Frontend (SF-216-04).
- Calcul SECAL belge (outil BE-only séparé).
- Recouvrement ARIPA (SF-216-07/08).
