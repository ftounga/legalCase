# Mini-spec — F-217 / SF-217-12 — Backend : acceptation / renonciation à succession belge

## Identifiant
`F-217 / SF-217-12`

## Feature parente
`F-217` — P2 Famille BE — outils décisionnels de fréquence haute (Vague 3 — Successions / protection / international)

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-217-12-succession-be-acceptation-renonciation`

---

## Objectif

Fournir un outil décisionnel backend qui guide l'héritier belge entre les trois options
successorales — acceptation pure et simple, acceptation sous bénéfice d'inventaire,
renonciation — calcule les délais impératifs (4 mois pour la déclaration officielle,
17 ans pour l'inventaire — à vérifier), et signale les risques (dévolution forcée,
acceptation tacite par actes d'héritier), à partir des éléments du dossier.

---

## Comportement attendu

### Cas nominal

1. L'avocat soumet les éléments de la situation successorale du client : date du décès,
   qualité d'héritier appelé, état du patrimoine successoral (solvable / douteux /
   insolvable / inconnu), actes éventuellement déjà accomplis (vente d'un bien, paiement
   d'une dette du défunt, encaissement, prise de possession), volonté du client
   (`ACCEPTER` / `ACCEPTER_BENEFICE_INVENTAIRE` / `RENONCER` / `INDECIS`), et l'existence
   éventuelle d'une mise en demeure d'un créancier d'opter (qui réduit le délai).
2. Le `SuccessionBeAcceptationRenonciationCalculator` applique l'arbre décisionnel du
   **Livre 4 du Code civil belge réformé** (CC art. 774+ nouveau — à vérifier) :
   (a) calcule le délai d'option (par défaut 4 mois à compter de l'ouverture de la
   succession — à vérifier ; réductible sur mise en demeure d'un créancier) ;
   (b) détecte les actes d'héritier qui valent acceptation tacite (CC art. 779 ancien /
   équivalent réformé — à vérifier) ; (c) recommande l'option la plus protectrice
   compte tenu de l'état du patrimoine ; (d) flag les risques de dévolution forcée
   passé le délai sans option.
3. Le calculateur produit un verdict (`OPTION_LIBRE_DELAI_OK` /
   `OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE` / `OPTION_RECOMMANDEE_RENONCIATION` /
   `ACCEPTATION_TACITE_PROBABLE` / `DELAI_CRITIQUE` / `DELAI_DEPASSE` /
   `QUALIFICATION_INCOMPLETE`), la date limite calculée, le nombre de jours restants,
   l'option recommandée avec son fondement, la liste des risques identifiés, les bases
   juridiques et les actions concrètes à poser (déclaration au greffe TF du domicile
   du défunt — à vérifier).
4. Le résultat est persisté par dossier (un seul résultat courant par dossier, écrasé
   au recalcul) et renvoyé.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Corps de requête absent | Message d'erreur explicite | 400 |
| Date de décès absente / future / mal formée | Message d'erreur explicite | 400 |
| Date de mise en demeure antérieure à la date de décès | Message d'erreur explicite | 400 |
| Commentaire > 1000 caractères | Message d'erreur explicite | 400 |
| Non authentifié | — | 401 |
| Dossier d'un autre workspace | Accès refusé | 403 |
| Dossier inexistant | — | 404 |
| `GET` sans calcul préalable | — | 404 |
| Dossier dont le `legalDomain` n'est pas `DROIT_FAMILLE` | Message explicite | 422 |
| Workspace dont le `country` n'est pas `BELGIQUE` (outil BE-only) | Message explicite | 422 |

---

## Contrat API (FIGÉ — importé par SF-217-13)

### POST `/api/v1/case-files/{caseFileId}/succession-be-acceptation-renonciation`

Body `SuccessionBeAcceptationRenonciationRequest` :
```json
{
  "dateDeces": "2026-02-10",
  "qualiteHeritier": "ENFANT",
  "etatPatrimoineSuccessoral": "DOUTEUX",
  "actesAccomplis": ["ENCAISSEMENT_CREANCE_DEFUNT"],
  "volonteClient": "INDECIS",
  "miseEnDemeureCreancier": false,
  "dateMiseEnDemeureCreancier": null,
  "commentaire": null
}
```
- `dateDeces` : `yyyy-MM-dd`, obligatoire, non future.
- `qualiteHeritier` : enum obligatoire.
- `etatPatrimoineSuccessoral` : enum obligatoire.
- `actesAccomplis` : liste obligatoire (peut être vide) d'enums (codes d'actes).
- `volonteClient` : enum obligatoire.
- `miseEnDemeureCreancier` : boolean obligatoire.
- `dateMiseEnDemeureCreancier` : `yyyy-MM-dd`, nullable, obligatoire si
  `miseEnDemeureCreancier = true`, postérieure ou égale à `dateDeces`.
- `commentaire` : nullable, max 1000 caractères.
- Le pays est dérivé du workspace côté service.

Réponse `200` — `SuccessionBeAcceptationRenonciationResponse` : **ré-expose
l'intégralité du body** (snapshot) **+** les champs calculés ci-dessous.
```json
{
  "caseFileId": "uuid",
  "dateDeces": "2026-02-10",
  "qualiteHeritier": "ENFANT",
  "etatPatrimoineSuccessoral": "DOUTEUX",
  "actesAccomplis": ["ENCAISSEMENT_CREANCE_DEFUNT"],
  "volonteClient": "INDECIS",
  "miseEnDemeureCreancier": false,
  "dateMiseEnDemeureCreancier": null,
  "commentaire": null,
  "verdict": "ACCEPTATION_TACITE_PROBABLE",
  "optionRecommandee": "ACCEPTER_BENEFICE_INVENTAIRE",
  "dateLimiteOption": "2026-06-10",
  "joursRestants": 21,
  "delaiStatut": "OK",
  "actionsConcretes": [
    "Déposer la déclaration d'option au greffe du Tribunal de la famille du dernier domicile du défunt (à vérifier — possibilité notaire selon réforme).",
    "En cas d'acceptation sous bénéfice d'inventaire : faire dresser l'inventaire dans les délais (17 ans maximum — à vérifier).",
    "Cesser tout acte d'héritier supplémentaire jusqu'à l'option formelle."
  ],
  "risques": [
    {
      "code": "ACTE_HERITIER_VALANT_ACCEPTATION_TACITE",
      "libelle": "L'encaissement d'une créance du défunt peut être qualifié d'acte d'héritier valant acceptation tacite.",
      "fondement": "CC art. 779 ancien / équivalent réformé (à vérifier) — acceptation tacite par actes d'administration ou de disposition à titre d'héritier",
      "severite": "HIGH"
    }
  ],
  "basesJuridiques": [
    "CC art. 774+ nouveau (à vérifier) — options de l'héritier (acceptation pure, acceptation sous bénéfice d'inventaire, renonciation)",
    "CC art. 779 ancien / équivalent réformé (à vérifier) — acceptation tacite par actes d'héritier",
    "CC art. 795 ancien / équivalent réformé (à vérifier) — délai et formalités de la renonciation"
  ],
  "messages": [
    "Patrimoine douteux + acte susceptible d'acceptation tacite : l'acceptation sous bénéfice d'inventaire est l'option la plus protectrice — elle limite l'engagement aux forces de la succession.",
    "Délai d'option par défaut : 4 mois à compter de l'ouverture de la succession (à vérifier). Date limite : 2026-06-10. 21 jours restants — agir dans les délais."
  ],
  "country": "BELGIQUE",
  "calculatedAt": "2026-05-20T10:00:00Z"
}
```

### GET `/api/v1/case-files/{caseFileId}/succession-be-acceptation-renonciation`
- `200` : dernier résultat (même structure, inputs inclus → formulaire ré-éditable).
  `404` si jamais calculé. `403` / `401` / `422` idem POST.

### Enum `verdict` (`SuccessionBeAcceptationRenonciationVerdict`)
- `OPTION_LIBRE_DELAI_OK` — aucun acte susceptible d'acceptation tacite, délai > 30 j,
  patrimoine clair : l'héritier peut librement opter.
- `OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE` — patrimoine douteux ou inconnu OU dettes
  signalées : l'acceptation sous bénéfice d'inventaire est recommandée.
- `OPTION_RECOMMANDEE_RENONCIATION` — patrimoine insolvable confirmé : la renonciation
  est recommandée.
- `ACCEPTATION_TACITE_PROBABLE` — au moins un acte d'héritier accompli (encaissement,
  vente, paiement) → risque qualifié, l'option doit être prise immédiatement.
- `DELAI_CRITIQUE` — `joursRestants` ≤ 30 j et option pas encore prise.
- `DELAI_DEPASSE` — `joursRestants` < 0 (dévolution forcée probable — risque
  d'acceptation tacite à défaut d'option, ou alimentation du créancier sur la succession).
- `QUALIFICATION_INCOMPLETE` — combinaison d'inputs ne permettant pas de trancher
  (ex : `miseEnDemeureCreancier = true` sans date).

### Enum `qualiteHeritier` (`QualiteHeritierBe`)
`CONJOINT_SURVIVANT` · `COHABITANT_LEGAL_SURVIVANT` · `ENFANT` ·
`DESCENDANT_REPRESENTATION` · `PARENT` · `FRERE_SOEUR` · `LEGATAIRE_UNIVERSEL` ·
`LEGATAIRE_PARTICULIER`.

### Enum `etatPatrimoineSuccessoral` (`EtatPatrimoineSuccessoralBe`)
`SOLVABLE` · `DOUTEUX` · `INSOLVABLE` · `INCONNU`.

### Enum `actesAccomplis` (`ActeHeritierBe`)
`AUCUN` · `ENCAISSEMENT_CREANCE_DEFUNT` · `PAIEMENT_DETTE_DEFUNT` ·
`VENTE_BIEN_SUCCESSION` · `PRISE_POSSESSION_BIENS` · `ACTE_CONSERVATOIRE_NEUTRE`
(acte de conservation ne valant pas acceptation — à vérifier) ·
`DEMANDE_INVENTAIRE` (ne vaut pas acceptation tacite — à vérifier).

### Enum `volonteClient` (`VolonteClientSuccessionBe`)
`ACCEPTER` · `ACCEPTER_BENEFICE_INVENTAIRE` · `RENONCER` · `INDECIS`.

### Enum `optionRecommandee` (`OptionRecommandeeSuccessionBe`)
`ACCEPTER` · `ACCEPTER_BENEFICE_INVENTAIRE` · `RENONCER`.

### Enum `delaiStatut` (`DelaiStatutBe`)
`OK` (> 30 j restants) · `CRITIQUE` (≤ 30 j et > 0) · `DEPASSE` (< 0).

### Enum `severite` d'un risque (`SeveriteRisqueBe`)
`LOW` · `MEDIUM` · `HIGH`.

### Enum `code` d'un risque (`RisqueSuccessionBeCode`)
`ACTE_HERITIER_VALANT_ACCEPTATION_TACITE` · `DELAI_TRES_COURT` ·
`PATRIMOINE_INSOLVABLE_ACCEPTATION_DANGEREUSE` · `MISE_EN_DEMEURE_DELAI_REDUIT` ·
`DEVOLUTION_FORCEE_RISQUE` (passé le délai sans option).

---

## Règles de l'arbre décisionnel

> ⚠️ **Validation juridique requise** : les articles CC art. 774+ (options de
> l'héritier), art. 779 ancien / équivalent réformé (acceptation tacite), art. 795
> ancien (renonciation), et le délai de 4 mois pour la déclaration d'option (par défaut,
> à compter de l'ouverture de la succession ; réductible sur mise en demeure d'un
> créancier) reflètent l'état du droit connu du modèle et sont **à valider par un
> avocat belge avant mise en production**. Le délai de 17 ans pour l'inventaire en cas
> d'acceptation sous bénéfice d'inventaire est également à confirmer. La compétence
> du greffe TF (vs notaire) pour recevoir la déclaration d'option est un point à
> trancher avec l'avocat belge — la réforme 2018-2019 a pu redistribuer ces compétences.
> Le contenu juridique est centralisé dans le Calculator (source unique de vérité).

### Calcul du délai

1. `dateLimiteOption = dateDeces + 4 mois` (date à date — à vérifier).
2. Si `miseEnDemeureCreancier = true` ET `dateMiseEnDemeureCreancier` fournie : le
   délai peut être raccourci (mise en demeure d'opter — à vérifier sur le délai
   précis ; V1 conserve la date la plus proche entre `dateDeces + 4 mois` et
   `dateMiseEnDemeureCreancier + 2 mois` — paramètre documenté, à confirmer).
3. `joursRestants = ChronoUnit.DAYS.between(today, dateLimiteOption)`.
4. `delaiStatut` : `OK` (> 30 j) / `CRITIQUE` (≤ 30 j et > 0) / `DEPASSE` (< 0).

### Détermination du verdict (figée dans le Calculator)

1. `miseEnDemeureCreancier = true` ET `dateMiseEnDemeureCreancier = null` →
   `QUALIFICATION_INCOMPLETE`.
2. `joursRestants < 0` → `DELAI_DEPASSE`, risque `DEVOLUTION_FORCEE_RISQUE` ajouté.
3. Au moins un `actesAccomplis ∈ {ENCAISSEMENT_CREANCE_DEFUNT, PAIEMENT_DETTE_DEFUNT,
   VENTE_BIEN_SUCCESSION, PRISE_POSSESSION_BIENS}` → `ACCEPTATION_TACITE_PROBABLE`,
   risque `ACTE_HERITIER_VALANT_ACCEPTATION_TACITE` ajouté.
4. `etatPatrimoineSuccessoral = INSOLVABLE` → `OPTION_RECOMMANDEE_RENONCIATION`.
5. `etatPatrimoineSuccessoral ∈ {DOUTEUX, INCONNU}` → `OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE`.
6. `joursRestants ≤ 30` → `DELAI_CRITIQUE`.
7. Sinon → `OPTION_LIBRE_DELAI_OK`.

### Option recommandée

| Patrimoine | Volonté client | Option recommandée |
|------------|----------------|---------------------|
| `SOLVABLE` | `ACCEPTER` ou `INDECIS` | `ACCEPTER` |
| `DOUTEUX` ou `INCONNU` | quelconque | `ACCEPTER_BENEFICE_INVENTAIRE` |
| `INSOLVABLE` | quelconque | `RENONCER` |
| toute | `RENONCER` explicite | `RENONCER` (sauf actes d'héritier déjà accomplis — bloque) |

### Risques détectés

| Critère | Code risque | Sévérité | Fondement (à valider) |
|---------|-------------|----------|------------------------|
| Au moins un acte d'héritier accompli | `ACTE_HERITIER_VALANT_ACCEPTATION_TACITE` | `HIGH` | CC art. 779 ancien / équivalent réformé |
| `delaiStatut = CRITIQUE` | `DELAI_TRES_COURT` | `HIGH` | CC art. 774+ |
| `etatPatrimoineSuccessoral = INSOLVABLE` ET `volonteClient = ACCEPTER` | `PATRIMOINE_INSOLVABLE_ACCEPTATION_DANGEREUSE` | `HIGH` | — |
| `miseEnDemeureCreancier = true` | `MISE_EN_DEMEURE_DELAI_REDUIT` | `MEDIUM` | CC art. 774+ (mise en demeure d'opter) |
| `joursRestants < 0` | `DEVOLUTION_FORCEE_RISQUE` | `HIGH` | CC art. 774+ |

---

## Conformité F-IA-04
- [x] **Non applicable au sens strict** — SF backend pure. Conformité F-IA-04
  et seed `decision_tool_visibility_rules` portés par SF-217-13 frontend (bundle Vague 3
  successions, couplé TOOL_REGISTRY).

---

## Champs IA à extraire (pré-remplissage IA — V1)

| Champ | Source backend potentielle | Statut V1 |
|-------|----------------------------|-----------|
| `dateDeces` | Détection date de décès | Aspirationnel — non extrait V1 |
| `etatPatrimoineSuccessoral` | Détection dettes / actif successoral | Aspirationnel — non extrait V1 |

**V1 : `PREFILL_COUNT_ALWAYS_ZERO = true`** côté SF-217-13 — aucun flag pivot dédié
n'est extrait par le pipeline IA pour les successions BE en V1. Pas de nouveau champ
ajouté à `FamilleExtractedData` dans cette SF.

---

## Critères d'acceptation

- [ ] `POST` calcule et persiste un résultat ; recalcul écrase le précédent (upsert 1:1
      par dossier).
- [ ] Délai calculé : `dateDeces + 4 mois` (date à date) ; `joursRestants` en jours
      calendaires.
- [ ] `miseEnDemeureCreancier = true` ET date fournie → délai potentiellement raccourci
      (test du chemin).
- [ ] `actesAccomplis` contient un acte d'héritier → verdict `ACCEPTATION_TACITE_PROBABLE`
      + risque `ACTE_HERITIER_VALANT_ACCEPTATION_TACITE`.
- [ ] `etatPatrimoineSuccessoral = INSOLVABLE` → `optionRecommandee = RENONCER`,
      verdict `OPTION_RECOMMANDEE_RENONCIATION`.
- [ ] `etatPatrimoineSuccessoral = DOUTEUX` → `optionRecommandee = ACCEPTER_BENEFICE_INVENTAIRE`.
- [ ] `joursRestants ≤ 30` → verdict `DELAI_CRITIQUE`, `delaiStatut = CRITIQUE`.
- [ ] `joursRestants < 0` → verdict `DELAI_DEPASSE`, risque `DEVOLUTION_FORCEE_RISQUE`.
- [ ] `miseEnDemeureCreancier = true` sans date → `QUALIFICATION_INCOMPLETE` /
      `400` selon que la validation Bean Validation passe ou non (validation `400`
      privilégiée, cohérent avec les autres SF F-217).
- [ ] La réponse `POST` / `GET` ré-expose l'intégralité des inputs (formulaire
      ré-éditable).
- [ ] `GET` renvoie le dernier résultat ou 404.
- [ ] `400` (corps absent, date future, date mise en demeure antérieure, commentaire
      trop long) / `403` / `404` / `422` / `401`.
- [ ] Isolation workspace testée.

---

## Périmètre

### Hors scope
- Frontend (SF-217-13).
- Génération de la déclaration d'option (acte) — outil de génération dédié potentiel,
  reporté.
- Production de l'inventaire bénéficiaire — reporté.
- Calcul détaillé des conséquences fiscales de chaque option (droits de succession
  régionaux) — reporté (`succession-be-droits-succession-regionaux`).
- Réutilisation du Calculator FR (options FR proches mais délais et formes différents —
  pas réutilisable).
- Pré-fill IA depuis l'analyse (documenté `PREFILL_COUNT_ALWAYS_ZERO` côté SF-217-13).
- Seed `decision_tool_visibility_rules` (porté par SF-217-13).

---

## Technique

### Endpoints
| Méthode | URL | Auth | Rôle |
|---------|-----|------|------|
| POST | `/api/v1/case-files/{caseFileId}/succession-be-acceptation-renonciation` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/succession-be-acceptation-renonciation` | Oui | MEMBER |

### Tables impactées
| Table | Opération | Notes |
|-------|-----------|-------|
| `succession_be_acceptation_renonciation_analyses` | CREATE + INSERT/UPDATE/SELECT | id (UUID), case_file_id (FK UNIQUE), country (VARCHAR), snapshot_data (TEXT JSON), created_at, updated_at |
| `decision_tool_visibility_rules` | INSERT (seed) | porté par SF-217-13 |

### Migration Liquibase
- [x] Oui — `272-create-succession-be-acceptation-renonciation-analyses.xml` (table
  seule). Numéro `272` = prochain libre après `271` (SF-217-11). À renuméroter si
  conflit au merge.

### Classes backend (pattern Vague 1+2 F-217)
`SuccessionBeAcceptationRenonciationCalculator` (static),
`SuccessionBeAcceptationRenonciationInput`, `SuccessionBeAcceptationRenonciationResult`,
`SuccessionBeAcceptationRenonciationRequest`,
`SuccessionBeAcceptationRenonciationResponse`,
`SuccessionBeAcceptationRenonciationAnalysis` (@Entity),
`SuccessionBeAcceptationRenonciationRepository`,
`SuccessionBeAcceptationRenonciationService`,
`SuccessionBeAcceptationRenonciationController`.

---

## Plan de test

### Tests unitaires (Calculator)
- [ ] Décès jour J, aucun acte, patrimoine solvable, indécis, > 30 j restants →
      `OPTION_LIBRE_DELAI_OK`.
- [ ] Patrimoine douteux → `OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE`.
- [ ] Patrimoine insolvable → `OPTION_RECOMMANDEE_RENONCIATION`.
- [ ] Encaissement d'une créance du défunt → `ACCEPTATION_TACITE_PROBABLE`, risque
      `ACTE_HERITIER_VALANT_ACCEPTATION_TACITE` `HIGH`.
- [ ] Date décès = today - 100 j → `joursRestants ≈ 20`, `delaiStatut = CRITIQUE`,
      verdict `DELAI_CRITIQUE`.
- [ ] Date décès = today - 150 j → `joursRestants < 0`, verdict `DELAI_DEPASSE`,
      risque `DEVOLUTION_FORCEE_RISQUE`.
- [ ] Mise en demeure créancier 1 mois après décès → calcul du délai raccourci
      (chemin alternatif).
- [ ] Acte conservatoire neutre seul → **pas** d'acceptation tacite, verdict normal.
- [ ] Calcul du délai date à date sur mois de longueurs différentes (cas février /
      31 → 28).

### Tests d'intégration (Controller)
- [ ] `POST` → 200 + persistance ; recalcul écrase.
- [ ] `GET` → 200 / 404 si jamais calculé.
- [ ] `400` (corps absent, date future, date mise en demeure antérieure au décès) /
      `403` / `404` / `422` (domaine ≠ famille, pays ≠ Belgique) / `401`.

### Isolation workspace
- [x] Applicable — un utilisateur du workspace A ne peut pas calculer/lire le résultat
  d'un dossier du workspace B.

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Outil décisionnel métier** — `succession-be-acceptation-renonciation` est un
  nouvel outil décisionnel. Scan : aucun outil BE existant ne couvre les options
  successorales BE (`F-FA-24` FR-only). Distinct de `succession-be-devolution-reserve`
  (SF-217-11 — quantification de la dévolution) et de `pacte-successoral-be-2018`
  (F-211 — pacte anticipé pré-décès). **Un outil = une situation** (arbre décisionnel
  + délais d'option post-décès).
- [x] Aucune autre préoccupation transversale.

### Smoke tests E2E
- [x] Aucun — feature additive.

---

## Dépendances
- Aucune SF bloquante. SF-217-13 (frontend bundle Vague 3 successions) importe le
  contrat API. Parallélisable.
- Indépendante de SF-217-11 — situations métier distinctes (quantification vs
  arbre + délais).

---

## Notes et décisions
- Persistance par snapshot JSON — pattern Vague 1+2 F-217.
- Délai par défaut de 4 mois : valeur figée dans le Calculator comme constante
  documentée (`DELAI_OPTION_MOIS = 4`) ; modification = nouvelle version de l'outil.
- Le délai raccourci sur mise en demeure d'un créancier est un cas réel mais le délai
  exact (V1 = 2 mois après mise en demeure) doit être confirmé par un avocat belge
  — paramètre constant documenté.
- Aucune réutilisation du Calculator FR : les options successorales FR existent (CC
  art. 768+) mais les délais (FR : 4 mois mais articulation différente avec
  mise en demeure CC art. 771 — à vérifier) et formes diffèrent. Outil bâti depuis
  les sources belges (`feedback_belgique_never_forget`).
- Le contenu juridique (articles CC, délais, fondements, codes d'actes d'héritier)
  est centralisé dans le Calculator et signalé pour validation par un avocat belge
  avant prod. Articles tagués `(à vérifier)` cohérents avec l'audit F-191.
