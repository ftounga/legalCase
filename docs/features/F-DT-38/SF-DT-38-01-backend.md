# Mini-spec — F-DT-38 / SF-DT-38-01 — Backend : qualification rupture de période d'essai

## Identifiant
`F-DT-38 / SF-DT-38-01`

## Feature parente
`F-DT-38` — Rupture de période d'essai (qualification régulière / abusive / nulle / illégale)

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-DT-38-01-backend`

---

## Objectif

Fournir un outil décisionnel backend qui qualifie une rupture de contrat pendant la période d'essai (FR) en 4 niveaux (`REGULIERE` / `RISQUE_ABUSIVE` / `NULLE` / `ILLEGALE_REQUALIF_LICENCIEMENT`), à partir des éléments factuels du dossier et de la jurisprudence Cass. soc.

---

## Comportement attendu

### Cas nominal

1. L'avocat soumet les éléments factuels (catégorie professionnelle, type de contrat, dates, durée essai contractuelle, délai de prévenance appliqué, motif, état de santé / grossesse / AT-MP, renouvellement, lettre motivée).
2. Le `RupturePeriodeEssaiCalculator` analyse les 12 critères, calcule un score d'irrégularité et rend un verdict.
3. Le résultat est persisté par dossier (un seul résultat courant par dossier, écrasé au recalcul) et renvoyé.

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|--------------|-----------|
| Champ obligatoire absent / date mal formée | Message explicite | 400 |
| Pays workspace ≠ FRANCE | Message explicite (BE = outil distinct backlog F-DT-39) | 400 |
| Dossier inexistant | — | 404 |
| Dossier d'un autre workspace | Accès refusé | 403 |
| Dossier dont le `legalDomain` n'est pas `DROIT_DU_TRAVAIL` | Message explicite | 422 |
| Non authentifié | — | 401 |
| `GET` sans calcul préalable | — | 404 |

---

## Contrat API (FIGÉ — importé par SF-DT-38-02)

### POST `/api/v1/case-files/{caseFileId}/rupture-periode-essai`

Body `RupturePeriodeEssaiRequest` :
```json
{
  "categorieSocioProfessionnelle": "CADRE",
  "typeContrat": "CDI",
  "dureeCddMois": null,
  "dateDebutContrat": "2025-01-06",
  "dateRupture": "2025-04-15",
  "dureePeriodeEssaiContractuelleMois": 4,
  "renouvellementInvoque": false,
  "accordBrancheRenouvellement": null,
  "accordEcritSalarieRenouvellement": null,
  "auteurRupture": "EMPLOYEUR",
  "delaiPrevenanceJoursAppliques": 30,
  "motifInvoque": "Insuffisance des résultats commerciaux pendant l'essai",
  "motifLieAuxCompetencesProfessionnelles": true,
  "motifEconomiqueOuOrganisationnel": false,
  "discriminationInvoquee": null,
  "grossesseAuMomentRupture": false,
  "arretAccidentTravailEnCours": false,
  "atteinteLiberteFondamentale": null,
  "lettreRuptureMotivee": true,
  "motifsAveresParPieces": true,
  "conventionCollectiveApplicable": true,
  "conventionCollectivePlusFavorableRespectee": true,
  "salaireMensuelBrut": 4500.00
}
```

**Champs (tous nullables sauf indication contraire)** :
- `categorieSocioProfessionnelle` (enum, **obligatoire**) : `OUVRIER_EMPLOYE`, `AGENT_MAITRISE_TECHNICIEN`, `CADRE` — détermine la durée légale L.1221-19.
- `typeContrat` (enum, **obligatoire**) : `CDI`, `CDD`, `INTERIM`.
- `dureeCddMois` (int) : durée totale du CDD si applicable, sinon null.
- `dateDebutContrat` (`yyyy-MM-dd`, **obligatoire**) : date d'entrée du salarié.
- `dateRupture` (`yyyy-MM-dd`, **obligatoire**) : date de rupture de la période d'essai.
- `dureePeriodeEssaiContractuelleMois` (int, **obligatoire**) : durée de la période d'essai stipulée au contrat (en mois).
- `renouvellementInvoque` (Boolean) : un renouvellement a-t-il été invoqué ?
- `accordBrancheRenouvellement` (Boolean nullable) : l'accord de branche prévoit-il le renouvellement ?
- `accordEcritSalarieRenouvellement` (Boolean nullable) : un accord exprès écrit du salarié a-t-il été obtenu ?
- `auteurRupture` (enum **obligatoire**) : `EMPLOYEUR`, `SALARIE`.
- `delaiPrevenanceJoursAppliques` (int nullable) : nombre de jours de prévenance effectivement appliqués.
- `motifInvoque` (String nullable, max 1000) : motif libre figurant dans la notification.
- `motifLieAuxCompetencesProfessionnelles` (Boolean nullable) : appréciation OUI/NON.
- `motifEconomiqueOuOrganisationnel` (Boolean nullable) : motif étranger à l'essai ?
- `discriminationInvoquee` (enum nullable) : `RACE_ORIGINE`, `SEXE`, `GROSSESSE`, `SANTE`, `SYNDICAL`, `AUTRE`, `null` (aucune discrimination invoquée).
- `grossesseAuMomentRupture` (Boolean nullable).
- `arretAccidentTravailEnCours` (Boolean nullable) : suspension AT/MP active au moment de la rupture ?
- `atteinteLiberteFondamentale` (String nullable, max 500) : description libre si invoquée.
- `lettreRuptureMotivee` (Boolean nullable) : présence d'une lettre de rupture motivée ?
- `motifsAveresParPieces` (Boolean nullable) : les motifs sont-ils corroborés par les pièces ?
- `conventionCollectiveApplicable` (Boolean nullable) : CCN applicable au contrat ?
- `conventionCollectivePlusFavorableRespectee` (Boolean nullable) : dispositions CCN plus favorables respectées ?
- `salaireMensuelBrut` (double nullable) : salaire mensuel brut — utilisé pour le calcul indicatif d'indemnité abus.

Réponse `200` — `RupturePeriodeEssaiResponse` : **ré-expose l'intégralité des champs du body de requête** (snapshot pour pré-remplissage / ré-édition du formulaire) **+** les champs calculés ci-dessous.
```json
{
  "caseFileId": "uuid",
  "categorieSocioProfessionnelle": "CADRE",
  ... (tous les inputs ré-exposés)
  "verdict": "REGULIERE",
  "scoreIrregularite": 0,
  "ancienneteJoursAuMomentRupture": 99,
  "dureeLegaleMaximaleMois": 4,
  "delaiPrevenanceLegalJours": 30,
  "delaiPrevenanceRespecte": true,
  "anomaliesDetectees": [
    {
      "code": "DUREE_ESSAI_DEPASSEE",
      "libelle": "Durée de la période d'essai supérieure à la durée légale",
      "fondement": "Art. L.1221-19 C. trav.",
      "gravite": "AVERE",
      "explication": "..."
    }
  ],
  "indemniteEstimee": {
    "montantMinEuros": 4500.00,
    "montantMaxEuros": 27000.00,
    "baseCalcul": "Salaire brut mensuel × 1 à 6 mois (fourchette jurisprudence locale)",
    "fondement": "Cass. soc., dommages et intérêts pour abus"
  },
  "remedeReintegration": false,
  "basesJuridiques": ["Art. L.1221-19 C. trav.", "Art. L.1221-25 C. trav."],
  "messages": ["Rupture conforme aux articles L.1221-19 à L.1221-25 — aucun vice détecté."],
  "country": "FRANCE",
  "calculatedAt": "2026-05-20T10:00:00Z"
}
```

### GET `/api/v1/case-files/{caseFileId}/rupture-periode-essai`
- `200` : dernier résultat (même structure, inputs inclus → le formulaire est ré-éditable). `404` si jamais calculé. `403` / `401` idem POST.

### Enum `verdict`
- `REGULIERE` — tous les critères respectés
- `RISQUE_ABUSIVE` — motif détourné, non-respect prévenance, légèreté blâmable — indemnité 1-6 mois
- `NULLE` — discrimination L.1132-1 / grossesse L.1225-1 / AT-MP L.1226-9 / liberté fondamentale — option **réintégration** + rappel salaires
- `ILLEGALE_REQUALIF_LICENCIEMENT` — durée essai > durée légale OU renouvellement irrégulier — barème Macron L.1235-3 applicable (sauf lettre motivée + motifs avérés)

### Enum `gravite` d'une anomalie
`AVERE` (anomalie caractérisée par les faits) · `PROBABLE` (présomption à confirmer par pièce)

---

## Les 12 critères d'analyse

| Code | Anomalie | Fondement | Règle de détection |
|------|----------|-----------|--------------------|
| `PERIODE_ESSAI_ABSENTE` | Pas de période d'essai contractuelle (hors scope) | L.1221-19 | `dureePeriodeEssaiContractuelleMois <= 0` |
| `DUREE_ESSAI_DEPASSEE` | Durée essai > durée légale par catégorie | L.1221-19 | `dureePeriodeEssaiContractuelleMois > dureeLegaleMaximaleMois` selon catégorie/contrat |
| `RENOUVELLEMENT_IRREGULIER` | Renouvellement invoqué sans accord de branche OU sans accord écrit du salarié | L.1221-23 | `renouvellementInvoque = true` ET (`accordBrancheRenouvellement = false` OU `accordEcritSalarieRenouvellement = false`) |
| `RUPTURE_HORS_PERIODE_ESSAI` | Date de rupture postérieure à l'expiration de la période d'essai (effective) | L.1221-25 | `dateRupture > (dateDebutContrat + dureeEffectiveEssaiMois)` |
| `DELAI_PREVENANCE_INSUFFISANT` | Délai de prévenance non respecté selon l'échelle L.1221-25 | L.1221-25 | `delaiPrevenanceJoursAppliques < delaiPrevenanceLegalJours` calculé selon `auteurRupture` + ancienneté |
| `MOTIF_NON_PROFESSIONNEL` | Motif sans rapport avec les qualités professionnelles | Cass. soc. 20/11/2007 | `motifLieAuxCompetencesProfessionnelles = false` |
| `MOTIF_ETRANGER_A_ESSAI` | Motif économique / organisationnel déguisé | Cass. soc. jurisprudence constante | `motifEconomiqueOuOrganisationnel = true` |
| `DISCRIMINATION_AVEREE` | Discrimination invoquée et caractérisée | L.1132-1 | `discriminationInvoquee != null` |
| `GROSSESSE_PROTECTION_VIOLEE` | Rupture pendant grossesse | L.1225-1 et s. | `grossesseAuMomentRupture = true` |
| `AT_MP_PROTECTION_VIOLEE` | Rupture pendant suspension AT/MP | L.1226-9 | `arretAccidentTravailEnCours = true` |
| `ATTEINTE_LIBERTE_FONDAMENTALE` | Atteinte à une liberté fondamentale | Cass. jurisprudence | `atteinteLiberteFondamentale != null` |
| `CONVENTION_COLLECTIVE_NON_RESPECTEE` | Dispositions CCN plus favorables non respectées | clause CCN applicable | `conventionCollectiveApplicable = true` ET `conventionCollectivePlusFavorableRespectee = false` |

**Calcul du verdict** :

Le verdict est piloté par la **présence des anomalies prioritaires**, dans l'ordre de gravité décroissant :

1. Si `DISCRIMINATION_AVEREE` OU `GROSSESSE_PROTECTION_VIOLEE` OU `AT_MP_PROTECTION_VIOLEE` OU `ATTEINTE_LIBERTE_FONDAMENTALE` → **`NULLE`** (avec `remedeReintegration = true`).
2. Sinon si `DUREE_ESSAI_DEPASSEE` OU `RENOUVELLEMENT_IRREGULIER` :
   - Si `lettreRuptureMotivee = true` ET `motifsAveresParPieces = true` → **`RISQUE_ABUSIVE`** (atténuation Marjolaine 19/05).
   - Sinon → **`ILLEGALE_REQUALIF_LICENCIEMENT`**.
3. Sinon si `MOTIF_NON_PROFESSIONNEL` OU `MOTIF_ETRANGER_A_ESSAI` OU `DELAI_PREVENANCE_INSUFFISANT` OU `CONVENTION_COLLECTIVE_NON_RESPECTEE` OU `RUPTURE_HORS_PERIODE_ESSAI` → **`RISQUE_ABUSIVE`**.
4. Sinon → **`REGULIERE`**.

`PERIODE_ESSAI_ABSENTE` → traitement spécial : verdict `REGULIERE` mais avec message `"Aucune période d'essai contractuelle — outil non applicable. Utiliser F-DT-08 (validité du licenciement) à la place."` (situation hors scope).

**Score d'irrégularité (0-100)** : indicateur secondaire, somme pondérée des anomalies (AVERE = 30, PROBABLE = 20, plafonné à 100).

**Indemnité estimée** :
- Verdict `RISQUE_ABUSIVE` : `min = 1 × salaire`, `max = 6 × salaire` (fourchette CPH).
- Verdict `NULLE` : pas de plancher fixe, `remedeReintegration = true` + indication dommages et intérêts subsidiaires.
- Verdict `ILLEGALE_REQUALIF_LICENCIEMENT` : note « barème Macron L.1235-3 applicable » sans calcul ici (F-DT-08 ou F-DT-09 si l'avocat enchaîne).
- Verdict `REGULIERE` : pas d'indemnité.

**Délai de prévenance légal calculé** :
- Si `auteurRupture = EMPLOYEUR` :
  - Ancienneté < 8 jours → 24 h (1 jour calendaire)
  - 8 jours ≤ ancienneté < 1 mois → 48 h (2 jours)
  - 1 mois ≤ ancienneté < 3 mois → 2 semaines (14 jours)
  - ≥ 3 mois → 1 mois (30 jours)
- Si `auteurRupture = SALARIE` :
  - Ancienneté < 8 jours → 24 h
  - Sinon → 48 h

**Durée légale maximale** (L.1221-19 / L.1242-10) :
- `CDI` + `OUVRIER_EMPLOYE` → 2 mois (renouvelable 1 fois)
- `CDI` + `AGENT_MAITRISE_TECHNICIEN` → 3 mois (renouvelable 1 fois)
- `CDI` + `CADRE` → 4 mois (renouvelable 1 fois)
- `CDD` ≤ 6 mois → 1 jour par semaine, max 2 semaines
- `CDD` > 6 mois → 1 jour par semaine, max 1 mois
- `INTERIM` : aligné sur CDD

**Durée effective de l'essai** : si `renouvellementInvoque = true` ET renouvellement régulier → `dureePeriodeEssaiContractuelleMois × 2`. Sinon `dureePeriodeEssaiContractuelleMois`.

> ⚠️ **Validation juridique requise** : les durées légales L.1221-19, l'échelle de prévenance L.1221-25, le bagage de protections L.1132-1/L.1225-1/L.1226-9 et la fourchette indemnitaire 1-6 mois (Marjolaine 19/05) doivent être relus par un avocat avant mise en production. Source unique = Calculator.

---

## Frontière avec F-DT-08 / F-DT-16 / F-DT-36

- **F-DT-08** (validité du licenciement, post-essai) reste l'outil de référence pour les ruptures **après** la période d'essai. F-DT-38 traite **uniquement** la rupture en cours de période d'essai. Si F-DT-38 rend `ILLEGALE_REQUALIF_LICENCIEMENT`, l'avocat peut enchaîner sur F-DT-08 pour le calcul barème.
- **F-DT-16** (licenciement nul, 7 protections post-essai) — F-DT-38 invoque ces protections (grossesse, AT/MP, discrimination) dans le contexte spécifique de l'essai SANS recalcul d'indemnité licenciement nul plancher 6 mois (Marjolaine 19/05 : option réintégration).
- **F-DT-36** (vices de procédure) — non applicable à l'essai (pas d'entretien préalable, pas de lettre motivée obligatoire). F-DT-38 ne touche pas F-DT-36.

Les outils coexistent comme simulateurs indépendants (modèle produit `feedback_decision_tools_are_simulators`) — pas d'override, pas de fusion.

---

## Conformité F-IA-04
- [x] **Non applicable au sens strict** — SF backend pure. La conformité F-IA-04 (TOOL_REGISTRY, pré-fill, F-IA-03) est portée par SF-DT-38-02 frontend. Le seed `decision_tool_visibility_rules` est lui aussi porté par SF-DT-38-02, couplé à l'entrée TOOL_REGISTRY dans le même lot (précédent SF-211-05).

---

## Critères d'acceptation

- [ ] `POST` calcule et persiste un résultat ; recalcul écrase le précédent.
- [ ] Les 12 critères sont détectés selon les règles du tableau.
- [ ] Verdict piloté par la priorité 4 niveaux : `NULLE` > `ILLEGALE_REQUALIF_LICENCIEMENT` > `RISQUE_ABUSIVE` > `REGULIERE`.
- [ ] Atténuation `ILLEGALE_REQUALIF_LICENCIEMENT` → `RISQUE_ABUSIVE` si lettre motivée + motifs avérés.
- [ ] Verdict `NULLE` ⇒ `remedeReintegration = true`.
- [ ] Durée légale calculée correctement selon catégorie + type contrat.
- [ ] Échelle de prévenance L.1221-25 codée correctement (employeur 24h/48h/2sem/1mois ; salarié 24h/48h).
- [ ] Indemnité fourchette 1-6 mois × salaire pour `RISQUE_ABUSIVE`.
- [ ] La réponse `POST` / `GET` ré-expose l'intégralité des inputs.
- [ ] `GET` renvoie le dernier résultat ou 404.
- [ ] `400` si date mal formée ou pays ≠ FRANCE, `403` workspace différent, `404` dossier inexistant, `422` domaine ≠ `DROIT_DU_TRAVAIL`, `401` non authentifié.
- [ ] Isolation workspace testée.

---

## Périmètre

### Hors scope
- Frontend (SF-DT-38-02).
- Outil jumeau Belgique (backlog F-DT-39 post-livraison FR uniquement si signal terrain BE).
- Modification de F-DT-08 / F-DT-16 / F-DT-36.
- Calcul barème Macron L.1235-3 (renvoi vers F-DT-09 / F-DT-08).
- Extraction IA dédiée des champs F-DT-38 — portée par SF-DT-38-02 frontend via extension du record `TravailExtractedData` + prompt `LegalDomainPromptBuilder` (champs déjà utilisés `dateLicenciement`, `typeContrat`, `motifLicenciement`, `salaireBrutMensuel` + nouveaux champs essai).

---

## Technique

### Endpoints
| Méthode | URL | Auth | Rôle |
|---------|-----|------|------|
| POST | `/api/v1/case-files/{caseFileId}/rupture-periode-essai` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/rupture-periode-essai` | Oui | MEMBER |

### Tables impactées
| Table | Opération | Notes |
|-------|-----------|-------|
| `rupture_periode_essai_analyses` | CREATE + INSERT/UPDATE/SELECT | id, case_file_id (FK unique), snapshot_data (TEXT JSON), country, created_at, updated_at |
| `decision_tool_visibility_rules` | INSERT (seed) | porté par SF-DT-38-02, couplé à l'entrée TOOL_REGISTRY |

### Migration Liquibase
- [x] Oui — `255-create-rupture-periode-essai-analyses.xml` (table seule ; le seed `decision_tool_visibility_rules` est porté par SF-DT-38-02 — migration ~256)

### Classes backend (pattern F-DT-36)
`RupturePeriodeEssaiCalculator` (static), `RupturePeriodeEssaiInput`, `RupturePeriodeEssaiResult`, `RupturePeriodeEssaiRequest`, `RupturePeriodeEssaiResponse`, `RupturePeriodeEssaiAnalysis` (@Entity), `RupturePeriodeEssaiRepository`, `RupturePeriodeEssaiService`, `RupturePeriodeEssaiController`.

---

## Plan de test

### Tests unitaires (Calculator)
- [ ] Rupture régulière employeur ≥ 3 mois prévenance 1 mois → `REGULIERE`, 0 anomalie.
- [ ] Rupture régulière salarié < 8 jours prévenance 24h → `REGULIERE`.
- [ ] Durée essai 5 mois pour cadre (limite 4) sans lettre → `ILLEGALE_REQUALIF_LICENCIEMENT`.
- [ ] Durée essai 5 mois pour cadre AVEC lettre motivée + motifs avérés → `RISQUE_ABUSIVE` (atténuation).
- [ ] Renouvellement invoqué sans accord branche → `ILLEGALE_REQUALIF_LICENCIEMENT`.
- [ ] Renouvellement invoqué avec accord branche + accord salarié → essai effectif × 2, verdict possible `REGULIERE`.
- [ ] Discrimination invoquée → `NULLE` + `remedeReintegration = true`.
- [ ] Grossesse au moment de la rupture → `NULLE` + reintegration.
- [ ] AT/MP en cours → `NULLE` + reintegration.
- [ ] Atteinte liberté fondamentale → `NULLE` + reintegration.
- [ ] Motif économique déguisé → `RISQUE_ABUSIVE`.
- [ ] Motif non professionnel → `RISQUE_ABUSIVE`.
- [ ] Délai prévenance insuffisant → `RISQUE_ABUSIVE`.
- [ ] CCN plus favorable non respectée → `RISQUE_ABUSIVE`.
- [ ] Rupture hors période d'essai → `RISQUE_ABUSIVE` + message « utiliser F-DT-08 ».
- [ ] Période d'essai absente (`dureePeriodeEssaiContractuelleMois = 0`) → `REGULIERE` avec message hors scope.
- [ ] Cumul : nullité prime sur illégalité (grossesse + durée dépassée → `NULLE`).
- [ ] Indemnité fourchette 1-6 mois calculée correctement pour `RISQUE_ABUSIVE` (salaire 4500 → min 4500, max 27000).
- [ ] Pays ≠ FRANCE → `IllegalArgumentException`.

### Tests d'intégration (Controller)
- [ ] `POST` → 200 + persistance ; recalcul écrase.
- [ ] `GET` → 200 / 404 si jamais calculé.
- [ ] 400 / 403 / 404 / 422 / 401.
- [ ] Isolation workspace (utilisateur du workspace A ne lit pas dossier workspace B).

### Isolation workspace
- [x] Applicable — un utilisateur du workspace A ne peut pas calculer/lire le résultat d'un dossier du workspace B.

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Outil décisionnel métier** — F-DT-38 est un nouvel outil décisionnel. Scan des outils licenciement existants fait dans `SF-DT-38-00-coherence.md` : F-DT-08, F-DT-10, F-DT-16, F-DT-22, F-DT-36 — F-DT-38 est une **situation métier distincte** (rupture pendant l'essai, régime L.1221-19 à L.1221-25 autonome). Frontières traitées ci-dessus.
- [x] Aucune autre préoccupation transversale (auth/workspace/plans/navigation non modifiés).

### Smoke tests E2E
- [x] Aucun — feature additive backend.

---

## Dépendances
- Aucune SF bloquante. SF-DT-38-02 (frontend) importe le contrat API ci-dessus.

---

## Notes et décisions
- Persistance par snapshot JSON (`snapshot_data`) — pattern F-DT-36, évite une colonne par critère.
- Verdict 4 niveaux aligné sur la précision Marjolaine 19/05 (régulière / abusive / nulle / illégale-requalif).
- Atténuation `ILLEGALE → RISQUE_ABUSIVE` si lettre motivée + motifs avérés : explicitement demandée par Marjolaine.
- Option réintégration pour `NULLE` : remède principal selon Marjolaine, dommages et intérêts en subsidiaire (pas de plancher 6 mois L.1235-3-1).
- Le contenu juridique (délais, fondements, fourchettes indemnités) est centralisé dans le Calculator et signalé pour validation juridique avant prod.
