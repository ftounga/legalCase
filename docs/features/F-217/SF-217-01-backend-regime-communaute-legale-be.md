# Mini-spec — F-217 / SF-217-01 — Backend : analyseur du régime de communauté légale belge

## Identifiant
`F-217 / SF-217-01`

## Feature parente
`F-217` — P2 Famille BE — ~10 outils décisionnels de fréquence haute (Vague 1 — Patrimoine du couple)

## Statut
`ready`

## Date de création
2026-05-17

## Branche Git
`feat/SF-217-01-regime-communaute-legale-be`

---

## Objectif

Fournir un outil décisionnel backend qui qualifie la composition du patrimoine d'un couple marié sous le régime de communauté légale belge (biens communs / biens propres), identifie le régime de gestion et la nature des dettes, et rend un verdict de composition à partir des éléments du dossier.

---

## Comportement attendu

### Cas nominal

1. L'avocat soumet les éléments du patrimoine d'un couple marié BE sans contrat de mariage (donc communauté légale par défaut depuis la loi du 22/07/2018) : date de mariage, présence d'un contrat de mariage, et la nature de chaque bien / dette caractérisé par ses critères de qualification (origine, date d'acquisition, financement, affectation professionnelle).
2. Le `RegimeCommunauteLegaleBeCalculator` applique les règles de qualification du Livre 3 du Code civil belge à chaque item, le classe `COMMUN` / `PROPRE` / `MIXTE_RECOMPENSE`, détermine le mode de gestion applicable et la nature de la dette.
3. Le calculateur produit un verdict global de composition (`COMMUNAUTE_LEGALE_APPLICABLE` / `REGIME_CONVENTIONNEL_DETECTE` / `QUALIFICATION_INCOMPLETE`), la liste qualifiée des biens et dettes, et les bases juridiques mobilisées.
4. Le résultat est persisté par dossier (un seul résultat courant par dossier, écrasé au recalcul) et renvoyé.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Champ obligatoire absent / date mal formée / liste de biens vide | Message d'erreur explicite | 400 |
| Non authentifié | — | 401 |
| Dossier d'un autre workspace | Accès refusé | 403 |
| Dossier inexistant | — | 404 |
| `GET` sans calcul préalable | — | 404 |
| Dossier dont le `legalDomain` n'est pas `DROIT_FAMILLE` | Message explicite | 422 |
| Workspace dont le `country` n'est pas `BELGIQUE` (outil BE-only) | Message explicite | 422 |

---

## Contrat API (FIGÉ — importé par SF-217-03)

### POST `/api/v1/case-files/{caseFileId}/regime-mat-be-communaute-legale`

Body `RegimeCommunauteLegaleBeRequest` :
```json
{
  "dateMariage": "2015-06-20",
  "contratMariageSigne": false,
  "biens": [
    {
      "libelle": "Appartement Schaerbeek",
      "categorie": "IMMOBILIER",
      "acquisAvantMariage": false,
      "acquisParDonationOuSuccession": false,
      "financeParFondsPropres": false,
      "affectationProfessionnelle": false,
      "outilDeTravailPersonnel": false,
      "commentaire": null
    }
  ],
  "dettes": [
    {
      "libelle": "Emprunt hypothécaire appartement",
      "anterieureAuMariage": false,
      "contracteeDansLInteretDuMenage": true,
      "contracteeParUnSeulEpoux": false,
      "commentaire": null
    }
  ]
}
```
- `dateMariage` : `yyyy-MM-dd`, obligatoire, non future.
- `contratMariageSigne` : boolean obligatoire.
- `biens` : liste obligatoire, non vide ; chaque item a un `libelle` (max 200), une `categorie` enum, des booleans de qualification obligatoires, un `commentaire` nullable (max 1000).
- `dettes` : liste obligatoire (peut être vide) ; mêmes contraintes de longueur.

Réponse `200` — `RegimeCommunauteLegaleBeResponse` : **ré-expose l'intégralité des champs du body de requête** (snapshot pour pré-remplissage / ré-édition du formulaire) **+** les champs calculés ci-dessous.
```json
{
  "caseFileId": "uuid",
  "dateMariage": "2015-06-20",
  "contratMariageSigne": false,
  "biens": [
    {
      "libelle": "Appartement Schaerbeek",
      "categorie": "IMMOBILIER",
      "acquisAvantMariage": false,
      "acquisParDonationOuSuccession": false,
      "financeParFondsPropres": false,
      "affectationProfessionnelle": false,
      "outilDeTravailPersonnel": false,
      "commentaire": null
    }
  ],
  "dettes": [
    {
      "libelle": "Emprunt hypothécaire appartement",
      "anterieureAuMariage": false,
      "contracteeDansLInteretDuMenage": true,
      "contracteeParUnSeulEpoux": false,
      "commentaire": null
    }
  ],
  "verdict": "COMMUNAUTE_LEGALE_APPLICABLE",
  "biensQualifies": [
    {
      "libelle": "Appartement Schaerbeek",
      "qualification": "COMMUN",
      "modeGestion": "GESTION_CONCURRENTE",
      "fondement": "CC Livre 3, art. 3.45 (biens communs — acquêts) ; art. 3.65 (gestion concurrente)",
      "explication": "Bien acquis à titre onéreux pendant le mariage : il entre dans le patrimoine commun. Gestion concurrente par chacun des époux, sauf actes graves soumis à cogestion."
    }
  ],
  "dettesQualifiees": [
    {
      "libelle": "Emprunt hypothécaire appartement",
      "qualification": "DETTE_COMMUNE",
      "fondement": "CC Livre 3, art. 3.55 (dettes communes — intérêt du ménage)",
      "explication": "Dette contractée dans l'intérêt du ménage : elle est commune et engage le patrimoine commun."
    }
  ],
  "syntheseComposition": {
    "nbBiensCommuns": 1,
    "nbBiensPropres": 0,
    "nbBiensMixtesAvecRecompense": 0,
    "nbDettesCommunes": 1,
    "nbDettesPropres": 0
  },
  "basesJuridiques": [
    "CC Livre 3, art. 3.45 (biens communs — acquêts)",
    "CC Livre 3, art. 3.55 (dettes communes — intérêt du ménage)",
    "CC Livre 3, art. 3.65 (gestion concurrente)"
  ],
  "messages": [
    "Aucun contrat de mariage : le régime légal de communauté (loi du 22/07/2018) s'applique de plein droit."
  ],
  "country": "BELGIQUE",
  "calculatedAt": "2026-05-17T10:00:00Z"
}
```

### GET `/api/v1/case-files/{caseFileId}/regime-mat-be-communaute-legale`
- `200` : dernier résultat (même structure, inputs inclus → le formulaire est ré-éditable). `404` si jamais calculé. `403` / `401` / `422` idem POST.

### Enum `verdict` (`RegimeCommunauteLegaleBeVerdict`)
- `COMMUNAUTE_LEGALE_APPLICABLE` — aucun contrat de mariage signé : le régime légal s'applique de plein droit, la qualification produite fait foi.
- `REGIME_CONVENTIONNEL_DETECTE` — un contrat de mariage est signé : la qualification légale est fournie à titre indicatif mais le contrat peut y déroger ; à confronter au contrat.
- `QUALIFICATION_INCOMPLETE` — un ou plusieurs biens présentent une combinaison de critères contradictoire ou insuffisante pour trancher (signalé item par item).

### Enum `categorie` d'un bien (`BienCategorieBe`)
`IMMOBILIER` · `MOBILIER` · `FINANCIER` · `PROFESSIONNEL` · `AUTRE`.

### Enum `qualification` d'un bien (`QualificationBienBe`)
`COMMUN` · `PROPRE` · `MIXTE_RECOMPENSE` (bien propre financé en partie par des fonds communs, ou inversement → ouvre droit à récompense) · `INDETERMINE` (critères contradictoires).

### Enum `modeGestion` (`ModeGestionBe`)
`GESTION_CONCURRENTE` (chaque époux gère seul — CC art. 3.65) · `GESTION_EXCLUSIVE` (l'époux propriétaire gère seul son bien propre / son outil de travail) · `COGESTION` (actes graves sur biens communs — vente immeuble, donation : accord des deux époux requis).

### Enum `qualification` d'une dette (`QualificationDetteBe`)
`DETTE_COMMUNE` · `DETTE_PROPRE` · `DETTE_PROPRE_AVEC_RECOURS` (dette propre mais le créancier peut poursuivre les biens communs — recours interne par récompense).

---

## Règles de qualification analysées

> ⚠️ **Validation juridique requise** : la numérotation du Livre 3 du Code civil belge issue de la loi du 22/07/2018 (entrée en vigueur 01/09/2018) a été massivement renumérotée. Les articles ci-dessous (`art. 3.45`, `3.55`, `3.65`, etc.) reflètent l'état du droit connu du modèle et sont **à valider par un avocat belge avant mise en production**. Le contenu juridique (articles, règles, libellés) est centralisé dans le Calculator (source unique de vérité).

### Qualification d'un bien

| Règle | Critères | Qualification | Fondement (à valider) |
|-------|----------|---------------|------------------------|
| Bien antérieur au mariage | `acquisAvantMariage = true` | `PROPRE` | CC Livre 3, art. 3.46 (biens propres par nature / origine) |
| Bien reçu par donation ou succession | `acquisParDonationOuSuccession = true` | `PROPRE` | CC Livre 3, art. 3.46 (libéralités) |
| Outil de travail strictement personnel | `outilDeTravailPersonnel = true` | `PROPRE` | CC Livre 3, art. 3.46 (biens propres par affectation) |
| Bien acquis pendant le mariage à titre onéreux | aucun critère de propre coché | `COMMUN` (acquêt) | CC Livre 3, art. 3.45 (biens communs — acquêts) |
| Bien propre financé par des fonds communs (ou inverse) | critère de propre coché ET `financeParFondsPropres` incohérent avec l'origine | `MIXTE_RECOMPENSE` | CC Livre 3, art. 3.46 + régime des récompenses |
| Critères contradictoires | combinaison non résoluble | `INDETERMINE` → contribue au verdict `QUALIFICATION_INCOMPLETE` | — |

### Détermination du mode de gestion

| Règle | Condition | Mode |
|-------|-----------|------|
| Bien commun courant | `qualification = COMMUN` ET non immobilier ET non `affectationProfessionnelle` | `GESTION_CONCURRENTE` |
| Bien commun à acte grave | `qualification = COMMUN` ET (`categorie = IMMOBILIER`) | `COGESTION` |
| Bien commun affecté à la profession d'un époux | `qualification = COMMUN` ET `affectationProfessionnelle = true` | `GESTION_EXCLUSIVE` (l'époux qui exerce la profession) |
| Bien propre | `qualification = PROPRE` ou `MIXTE_RECOMPENSE` | `GESTION_EXCLUSIVE` |

### Qualification d'une dette

| Règle | Critères | Qualification | Fondement (à valider) |
|-------|----------|---------------|------------------------|
| Dette antérieure au mariage | `anterieureAuMariage = true` | `DETTE_PROPRE` | CC Livre 3, art. 3.56 (dettes propres) |
| Dette dans l'intérêt du ménage | `contracteeDansLInteretDuMenage = true` | `DETTE_COMMUNE` | CC Livre 3, art. 3.55 (dettes communes) |
| Dette contractée par un seul époux hors intérêt du ménage | `contracteeParUnSeulEpoux = true` ET `contracteeDansLInteretDuMenage = false` | `DETTE_PROPRE_AVEC_RECOURS` | CC Livre 3, art. 3.56 + art. 3.58 (recours du créancier / récompense) |
| Autre | aucun critère discriminant | `DETTE_COMMUNE` (présomption de communauté pendant le mariage) | CC Livre 3, art. 3.55 |

**Calcul du verdict** : `contratMariageSigne = true` → `REGIME_CONVENTIONNEL_DETECTE` (la qualification reste fournie, indicative). Sinon, si au moins un bien est `INDETERMINE` → `QUALIFICATION_INCOMPLETE`. Sinon → `COMMUNAUTE_LEGALE_APPLICABLE`. Logique figée dans le Calculator, testée.

---

## Conformité F-IA-04
- [x] **Non applicable au sens strict** — SF backend pure. La conformité F-IA-04 (TOOL_REGISTRY, pré-fill, F-IA-03, gate `workspaceCountry`) est portée par SF-217-03 frontend. Le seed `decision_tool_visibility_rules` est lui aussi porté par SF-217-03 (migration 232), couplé à l'entrée TOOL_REGISTRY dans le même lot — un seed sans entrée frontend ferait échouer le garde-fou `DecisionToolVisibilityIntegrityIT` (précédent SF-211-05 / SF-DT-36-02).

---

## Critères d'acceptation

- [ ] `POST` calcule et persiste un résultat ; recalcul écrase le précédent (upsert 1:1 par dossier).
- [ ] Chaque bien est qualifié `COMMUN` / `PROPRE` / `MIXTE_RECOMPENSE` / `INDETERMINE` selon les règles du tableau, avec son mode de gestion et son fondement.
- [ ] Chaque dette est qualifiée `DETTE_COMMUNE` / `DETTE_PROPRE` / `DETTE_PROPRE_AVEC_RECOURS`.
- [ ] Verdict piloté : contrat signé → `REGIME_CONVENTIONNEL_DETECTE` ; bien indéterminé → `QUALIFICATION_INCOMPLETE` ; sinon `COMMUNAUTE_LEGALE_APPLICABLE`.
- [ ] La `syntheseComposition` reflète les comptes exacts.
- [ ] La réponse `POST` / `GET` ré-expose l'intégralité des inputs (formulaire ré-éditable — leçon F-DT-36).
- [ ] `GET` renvoie le dernier résultat ou 404.
- [ ] `400` liste de biens vide / date mal formée ; `403` workspace différent ; `404` dossier inexistant ; `422` domaine ≠ `DROIT_FAMILLE` ou pays ≠ `BELGIQUE` ; `401` non authentifié.
- [ ] Isolation workspace testée.

---

## Périmètre

### Hors scope
- Frontend (SF-217-03).
- Calcul chiffré des récompenses entre patrimoine commun et propre (`recompenses-be` est un outil distinct — audit F-191 § 3.5, reporté).
- Régimes de séparation de biens / communauté universelle / participation aux acquêts (outils distincts — F-217 vague ultérieure ou F-223).
- Pré-fill IA depuis l'analyse (aucun flag pivot dédié extrait par le pipeline V1 — saisie manuelle ; documenté `PREFILL_COUNT_ALWAYS_ZERO` côté SF-217-03).
- Réutilisation des Calculators FR (régime FR de communauté réduite aux acquêts — mécanisme juridiquement distinct, pas réutilisable).
- Seed `decision_tool_visibility_rules` (porté par SF-217-03, migration 232).

---

## Technique

### Endpoints
| Méthode | URL | Auth | Rôle |
|---------|-----|------|------|
| POST | `/api/v1/case-files/{caseFileId}/regime-mat-be-communaute-legale` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/regime-mat-be-communaute-legale` | Oui | MEMBER |

### Tables impactées
| Table | Opération | Notes |
|-------|-----------|-------|
| `regime_communaute_legale_be_analyses` | CREATE + INSERT/UPDATE/SELECT | id (UUID), case_file_id (FK UNIQUE), country (VARCHAR), snapshot_data (TEXT JSON — inputs + résultat calculé), created_at, updated_at |
| `decision_tool_visibility_rules` | INSERT (seed) | porté par SF-217-03 (migration 232), couplé à l'entrée TOOL_REGISTRY |

### Migration Liquibase
- [x] Oui — `232-create-regime-communaute-legale-be-analyses.xml` (table seule ; le seed `decision_tool_visibility_rules` est porté par SF-217-03 — migration 234)

### Classes backend (pattern `DivorceDcBe*` / `ProcedureNulliteLicenciement*`)
`RegimeCommunauteLegaleBeCalculator` (static), `RegimeCommunauteLegaleBeInput`, `RegimeCommunauteLegaleBeResult`, `RegimeCommunauteLegaleBeRequest`, `RegimeCommunauteLegaleBeResponse`, `RegimeCommunauteLegaleBeAnalysis` (@Entity), `RegimeCommunauteLegaleBeRepository`, `RegimeCommunauteLegaleBeService`, `RegimeCommunauteLegaleBeController`.

---

## Plan de test

### Tests unitaires (Calculator)
- [ ] Couple sans contrat, biens tous acquêts → `COMMUNAUTE_LEGALE_APPLICABLE`, tous `COMMUN`.
- [ ] Bien antérieur au mariage → `PROPRE`, `GESTION_EXCLUSIVE`.
- [ ] Bien reçu par donation / succession → `PROPRE`.
- [ ] Outil de travail personnel → `PROPRE`.
- [ ] Bien immobilier commun → `COGESTION`.
- [ ] Bien commun affecté à la profession → `GESTION_EXCLUSIVE`.
- [ ] Bien propre financé par fonds communs → `MIXTE_RECOMPENSE`.
- [ ] Critères contradictoires → `INDETERMINE` → verdict `QUALIFICATION_INCOMPLETE`.
- [ ] Contrat de mariage signé → `REGIME_CONVENTIONNEL_DETECTE`.
- [ ] Dette intérêt du ménage → `DETTE_COMMUNE` ; dette antérieure → `DETTE_PROPRE` ; dette d'un seul époux hors ménage → `DETTE_PROPRE_AVEC_RECOURS`.
- [ ] `syntheseComposition` : comptes exacts.

### Tests d'intégration (Controller)
- [ ] `POST` → 200 + persistance ; recalcul écrase.
- [ ] `GET` → 200 / 404 si jamais calculé.
- [ ] `400` (liste de biens vide, date mal formée) / `403` / `404` / `422` (domaine ≠ famille, pays ≠ Belgique) / `401`.

### Isolation workspace
- [x] Applicable — un utilisateur du workspace A ne peut pas calculer/lire le résultat d'un dossier du workspace B.

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Outil décisionnel métier** — `regime-mat-be-communaute-legale` est un nouvel outil décisionnel. Scan des outils régime matrimonial / patrimoine fait dans `SF-217-00-coherence.md` : aucun outil BE existant ne couvre la composition du patrimoine sous communauté légale belge (F-FA-15/16 sont FR-only, masqués en BE ; F-FA-05 partage immobilier est un calcul neutre distinct). `regime-mat-be-communaute-legale` est une **situation métier distincte** — un outil = une situation.
- [x] Aucune autre préoccupation transversale (auth/workspace/plans/navigation non modifiés).

### Smoke tests E2E
- [x] Aucun — feature additive (nouvel endpoint).

---

## Dépendances
- Aucune SF bloquante. SF-217-03 (frontend) importe le contrat API ci-dessus. Dev backend/frontend parallélisable (contrat figé).

---

## Notes et décisions
- Persistance par snapshot JSON (`snapshot_data`) — pattern `DivorceDcBeAnalysis.result_data` / `ProcedureNulliteLicenciementAnalysis.snapshot_data`, évite une colonne par bien (listes de longueur variable).
- Aucune réutilisation du Calculator FR : la communauté légale belge (Livre 3 CC, loi 22/07/2018) diffère de la communauté réduite aux acquêts française — composition, régime des dettes et des récompenses distincts. Outil bâti depuis les sources belges (`feedback_belgique_never_forget`).
- Le contenu juridique (articles du Livre 3, règles de qualification, libellés de fondement) est centralisé dans le Calculator et signalé pour validation par un avocat belge avant prod.
</content>
</invoke>
