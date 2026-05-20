# Mini-spec — F-217 / SF-217-16 — Backend : reconnaissance d'un mariage ou divorce étranger en Belgique (incl. talaq)

## Identifiant
`F-217 / SF-217-16`

## Feature parente
`F-217` — P2 Famille BE — outils décisionnels de fréquence haute (Vague 3 — Successions / protection / international)

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-217-16-mariage-etranger-be-reconnaissance`

---

## Objectif

Fournir un outil décisionnel backend qui qualifie la reconnaissance en Belgique d'un
mariage ou d'un divorce prononcé à l'étranger (notamment **talaq**, mariages religieux
non précédés d'un mariage civil, mariages polygames) au regard du **Code de droit
international privé belge** (CDIP — loi du 16/07/2004, art. 21+ et art. 27+ —
à vérifier) et de la jurisprudence sur l'**ordre public belge**, à partir des éléments
du dossier.

---

## Comportement attendu

### Cas nominal

1. L'avocat soumet les éléments de la situation internationale : nature de l'acte
   étranger à reconnaître (`MARIAGE_CIVIL_ETRANGER` / `MARIAGE_RELIGIEUX_NON_CIVIL` /
   `MARIAGE_POLYGAME` / `DIVORCE_JUDICIAIRE_ETRANGER` / `TALAQ_REPUDIATION`), pays
   d'origine de l'acte, date de l'acte, présence de **lien de rattachement** des parties
   à la Belgique au moment de l'acte (résidence habituelle, nationalité belge ou non),
   conformité de l'acte aux conditions de fond du droit personnel des parties,
   conformité aux formes requises par le droit du lieu de célébration (`locus regit
   actum`), spécifiquement pour le talaq : `consentementEpouse`, `epousePresente`,
   `procedureContradictoire`, `decisionEcriteOfficielle`, `convention_bilaterale_applicable`.
2. Le `MariageEtrangerBeReconnaissanceCalculator` applique l'arbre du CDIP : (a) vérifie
   le **respect du droit du fond** (CDIP art. 46 — loi nationale au moment du mariage,
   à vérifier) ; (b) vérifie la **forme** (CDIP `locus regit actum` — à vérifier) ;
   (c) vérifie la **non-contrariété à l'ordre public belge** (CDIP art. 21 / art. 25 —
   à vérifier) : refus systématique pour polygamie civile (ordre public absolu),
   conditions strictes pour talaq (jurisprudence sur consentement effectif de
   l'épouse + procédure contradictoire) ; (d) tient compte des **conventions
   bilatérales** existantes (BE-Maroc, BE-Algérie, BE-Turquie — à vérifier).
3. Le calculateur produit un verdict (`RECONNAISSANCE_DE_PLEIN_DROIT` /
   `RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS` / `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC` /
   `EXEQUATUR_REQUIS` / `QUALIFICATION_INCOMPLETE`), les motifs de refus le cas échéant,
   les actes à produire (transcription registre état civil ; demande d'exequatur TF —
   à vérifier), les bases juridiques (CDIP, conventions bilatérales), et des messages
   d'aide (notamment les effets résiduels : succession, pension de réversion — qui
   restent en cas de polygamie non reconnue civilement).
4. Le résultat est persisté par dossier (un seul résultat courant par dossier, écrasé
   au recalcul) et renvoyé.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Corps de requête absent | Message d'erreur explicite | 400 |
| `natureActe` absent ou invalide | Message d'erreur explicite | 400 |
| `paysOrigine` absent (ISO 3166-1 alpha-2) | Message d'erreur explicite | 400 |
| `dateActe` absente / future / mal formée | Message d'erreur explicite | 400 |
| `natureActe = TALAQ_REPUDIATION` ET un champ talaq-spécifique (`consentementEpouse`, etc.) absent | Message d'erreur explicite | 400 |
| Commentaire > 1000 caractères | Message d'erreur explicite | 400 |
| Non authentifié | — | 401 |
| Dossier d'un autre workspace | Accès refusé | 403 |
| Dossier inexistant | — | 404 |
| `GET` sans calcul préalable | — | 404 |
| Dossier dont le `legalDomain` n'est pas `DROIT_FAMILLE` | Message explicite | 422 |
| Workspace dont le `country` n'est pas `BELGIQUE` (outil BE-only) | Message explicite | 422 |

---

## Contrat API (FIGÉ — importé par SF-217-17)

### POST `/api/v1/case-files/{caseFileId}/mariage-etranger-be-reconnaissance`

Body `MariageEtrangerBeReconnaissanceRequest` :
```json
{
  "natureActe": "TALAQ_REPUDIATION",
  "paysOrigine": "MA",
  "dateActe": "2024-08-15",
  "residenceHabituelleAuMoinsUnePartie": "BELGIQUE",
  "nationaliteAuMoinsUnePartie": "BELGIQUE",
  "conformiteDroitFondPersonnel": true,
  "conformiteFormeLocusRegitActum": true,
  "consentementEpouse": true,
  "epousePresente": true,
  "procedureContradictoire": false,
  "decisionEcriteOfficielle": true,
  "conventionBilateraleApplicable": false,
  "commentaire": null
}
```
- `natureActe` : enum obligatoire.
- `paysOrigine` : `String` ISO 3166-1 alpha-2 obligatoire (validation par regex `^[A-Z]{2}$`).
- `dateActe` : `yyyy-MM-dd`, obligatoire, non future.
- `residenceHabituelleAuMoinsUnePartie` : enum obligatoire (`BELGIQUE` /
  `ETRANGER` / `INCONNU`).
- `nationaliteAuMoinsUnePartie` : enum obligatoire (`BELGIQUE` / `UE` / `HORS_UE` /
  `INCONNU`).
- `conformiteDroitFondPersonnel`, `conformiteFormeLocusRegitActum` : booleans
  obligatoires.
- Talaq-spécifiques (`consentementEpouse`, `epousePresente`,
  `procedureContradictoire`, `decisionEcriteOfficielle`) : booleans, obligatoires
  uniquement si `natureActe = TALAQ_REPUDIATION` (nullables sinon).
- `conventionBilateraleApplicable` : boolean obligatoire.
- `commentaire` : nullable, max 1000 caractères.
- Le pays cible est dérivé du workspace côté service (validation `BELGIQUE` requise).

Réponse `200` — `MariageEtrangerBeReconnaissanceResponse` : **ré-expose l'intégralité
du body** (snapshot) **+** les champs calculés.
```json
{
  "caseFileId": "uuid",
  "natureActe": "TALAQ_REPUDIATION",
  "paysOrigine": "MA",
  "dateActe": "2024-08-15",
  "residenceHabituelleAuMoinsUnePartie": "BELGIQUE",
  "nationaliteAuMoinsUnePartie": "BELGIQUE",
  "conformiteDroitFondPersonnel": true,
  "conformiteFormeLocusRegitActum": true,
  "consentementEpouse": true,
  "epousePresente": true,
  "procedureContradictoire": false,
  "decisionEcriteOfficielle": true,
  "conventionBilateraleApplicable": false,
  "commentaire": null,
  "verdict": "RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS",
  "motifsRefus": [],
  "motifsReserve": [
    {
      "code": "TALAQ_PROCEDURE_NON_CONTRADICTOIRE",
      "libelle": "Talaq prononcé sans procédure contradictoire — point sensible jurisprudence belge sur ordre public",
      "fondement": "CDIP art. 25 (à vérifier) — exigences procédurales pour la reconnaissance des décisions étrangères",
      "severite": "HIGH"
    }
  ],
  "actesAProduire": [
    "Demande de reconnaissance auprès de l'officier de l'état civil compétent — à vérifier",
    "Production de l'acte étranger légalisé / apostillé + traduction jurée",
    "Préparation d'une argumentation sur l'effectivité du consentement de l'épouse (jurisprudence Cassation)"
  ],
  "basesJuridiques": [
    "CDIP (loi du 16/07/2004) art. 21+ (à vérifier) — reconnaissance des actes étrangers",
    "CDIP art. 25 (à vérifier) — conditions de reconnaissance des décisions étrangères",
    "CDIP art. 27 (à vérifier) — reconnaissance de plein droit / refus pour contrariété à l'ordre public",
    "CDIP art. 46 (à vérifier) — loi applicable aux conditions de fond du mariage"
  ],
  "messages": [
    "Talaq marocain post-2004 (réforme Moudawana) — la jurisprudence belge accepte la reconnaissance lorsque le consentement de l'épouse est effectif et la procédure officielle. L'absence de procédure contradictoire est un facteur de risque (réserve `HIGH`).",
    "Vérifier l'existence d'une convention bilatérale Belgique-Maroc applicable — peut faciliter la reconnaissance."
  ],
  "country": "BELGIQUE",
  "calculatedAt": "2026-05-20T10:00:00Z"
}
```

### GET `/api/v1/case-files/{caseFileId}/mariage-etranger-be-reconnaissance`
- `200` : dernier résultat (même structure, inputs inclus → formulaire ré-éditable).
  `404` si jamais calculé. `403` / `401` / `422` idem POST.

### Enum `verdict` (`MariageEtrangerBeReconnaissanceVerdict`)
- `RECONNAISSANCE_DE_PLEIN_DROIT` — acte civil étranger respectant fond + forme + ordre
  public, sans nécessité de procédure spécifique (CDIP art. 27 — à vérifier) :
  transcription à l'état civil.
- `RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS` — reconnaissance recevable mais réserves
  à lever (procédure contradictoire, preuve consentement, etc.).
- `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC` — atteinte manifeste à l'ordre public belge
  (polygamie civile, mariage forcé non consenti, mariage d'enfant) → refus civil.
  Note : effets résiduels possibles (succession, pension) — message d'aide.
- `EXEQUATUR_REQUIS` — l'acte est une décision étrangère nécessitant une procédure
  d'exequatur devant le TF (CDIP art. 22+ — à vérifier).
- `QUALIFICATION_INCOMPLETE` — combinaison d'inputs ne permettant pas de trancher.

### Enum `natureActe` (`NatureActeEtrangerBe`)
`MARIAGE_CIVIL_ETRANGER` · `MARIAGE_RELIGIEUX_NON_CIVIL` · `MARIAGE_POLYGAME` ·
`DIVORCE_JUDICIAIRE_ETRANGER` · `TALAQ_REPUDIATION`.

### Enum `residenceHabituelleAuMoinsUnePartie` (`ResidenceHabituelleBe`)
`BELGIQUE` · `ETRANGER` · `INCONNU`.

### Enum `nationaliteAuMoinsUnePartie` (`NationalitePartiesBe`)
`BELGIQUE` · `UE` · `HORS_UE` · `INCONNU`.

### Enum `code` d'un motif (`MotifReconnaissanceEtrangerBeCode`)
`POLYGAMIE_ORDRE_PUBLIC` · `MARIAGE_RELIGIEUX_NON_CIVIL` ·
`TALAQ_CONSENTEMENT_EPOUSE_ABSENT` · `TALAQ_PROCEDURE_NON_CONTRADICTOIRE` ·
`TALAQ_EPOUSE_NON_PRESENTE_NI_REPRESENTEE` · `FRAUDE_LOI_RECONNAISSABLE` ·
`MARIAGE_FORCE_DETECTE` · `MARIAGE_ENFANT` · `FOND_DROIT_PERSONNEL_NON_CONFORME` ·
`FORME_LOCUS_REGIT_ACTUM_NON_CONFORME` · `DECISION_NON_OFFICIELLE`.

### Enum `severite` d'un motif (`SeveriteMotifBe`)
`LOW` · `MEDIUM` · `HIGH`.

---

## Règles de l'arbre décisionnel

> ⚠️ **Validation juridique requise** : le **Code de droit international privé belge**
> (loi du 16/07/2004), les articles 21 (ordre public), 22+ (exequatur), 25 (conditions
> reconnaissance décisions étrangères), 27 (reconnaissance de plein droit) et 46
> (loi applicable au mariage) reflètent l'état du droit connu du modèle et sont
> **à valider par un avocat belge avant mise en production**. La jurisprudence belge
> sur le **talaq** (Cassation et juridictions du fond) a évolué — le critère central
> est l'effectivité du consentement de l'épouse et le caractère officiel / écrit /
> notifié de la décision (acceptation du talaq marocain post-Moudawana 2004 sous
> conditions). Les **conventions bilatérales** (Belgique-Maroc, Belgique-Algérie,
> Belgique-Turquie — à vérifier l'existence et la portée) peuvent modifier le régime
> de reconnaissance. Le contenu juridique est centralisé dans le Calculator
> (source unique de vérité).

### Détermination du verdict (figée dans le Calculator)

1. Champs talaq-spécifiques requis mais absents (`natureActe = TALAQ_REPUDIATION` sans
   booleans talaq) → `400` (validation Bean Validation), pas exécution du Calculator.
2. `natureActe = MARIAGE_POLYGAME` → `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC`, motif
   `POLYGAMIE_ORDRE_PUBLIC` `HIGH`. Message d'aide sur les effets résiduels
   (succession, pension — à vérifier).
3. `natureActe = MARIAGE_RELIGIEUX_NON_CIVIL` → `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC`,
   motif `MARIAGE_RELIGIEUX_NON_CIVIL` `HIGH`. Constitution art. 21 (à vérifier) — un
   mariage religieux non précédé du civil n'a pas d'effet civil en BE.
4. `natureActe = TALAQ_REPUDIATION` :
   - `consentementEpouse = false` → `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC`, motif
     `TALAQ_CONSENTEMENT_EPOUSE_ABSENT` `HIGH`.
   - `epousePresente = false` ET aucune représentation → motif réserve
     `TALAQ_EPOUSE_NON_PRESENTE_NI_REPRESENTEE` `HIGH`.
   - `procedureContradictoire = false` → motif réserve
     `TALAQ_PROCEDURE_NON_CONTRADICTOIRE` `HIGH` (peut être levé en cas de
     consentement écrit de l'épouse).
   - `decisionEcriteOfficielle = false` → motif réserve `DECISION_NON_OFFICIELLE` `HIGH`.
   - Si tous OK → `RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS` (verdict positif mais
     prudent — la jurisprudence belge reste cas par cas).
5. `natureActe = DIVORCE_JUDICIAIRE_ETRANGER` :
   - Reconnaissance hors UE → `EXEQUATUR_REQUIS` (CDIP art. 22+ — à vérifier).
   - Reconnaissance UE → `RECONNAISSANCE_DE_PLEIN_DROIT` (Règlement Bruxelles II bis —
     à vérifier).
6. `natureActe = MARIAGE_CIVIL_ETRANGER` :
   - `conformiteDroitFondPersonnel = false` → `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC`,
     motif `FOND_DROIT_PERSONNEL_NON_CONFORME` `HIGH`.
   - `conformiteFormeLocusRegitActum = false` → motif réserve
     `FORME_LOCUS_REGIT_ACTUM_NON_CONFORME` `MEDIUM`.
   - Si tous OK → `RECONNAISSANCE_DE_PLEIN_DROIT`.
7. Combinaison non couverte → `QUALIFICATION_INCOMPLETE`.

### Effets résiduels (refus civil ≠ aucun effet)

En cas de `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC` (notamment polygamie), un message
d'aide explicite que certains effets peuvent subsister en pratique (succession ab
intestat, pension de réversion sous réserve d'analyse cas par cas) — à valider
juridiquement. L'outil **ne tranche pas** ces effets résiduels (outil dédié potentiel,
reporté).

### Conventions bilatérales

Si `conventionBilateraleApplicable = true`, un message d'aide invite l'avocat à
consulter la convention (BE-Maroc 1995, BE-Algérie 1991, BE-Turquie 1958 — dates
indicatives à vérifier). L'outil V1 ne déduit pas automatiquement le régime de la
convention — l'avocat doit l'invoquer.

---

## Conformité F-IA-04
- [x] **Non applicable au sens strict** — SF backend pure. Conformité F-IA-04 et seed
  `decision_tool_visibility_rules` portés par SF-217-17 frontend (couplé TOOL_REGISTRY).

---

## Champs IA à extraire (pré-remplissage IA — V1)

| Champ | Source backend potentielle | Statut V1 |
|-------|----------------------------|-----------|
| `natureActe` | Détection « talaq », « mariage marocain », « polygamie » | Aspirationnel — non extrait V1 (flag `mariage_etranger_reconnaissance_detecte` mentionné audit F-191) |
| `paysOrigine` | Détection pays dans documents | Aspirationnel — non extrait V1 |
| `dateActe` | Détection date d'acte étranger | Aspirationnel — non extrait V1 |

**V1 : `PREFILL_COUNT_ALWAYS_ZERO = true`** côté SF-217-17. Pas de nouveau champ
ajouté à `FamilleExtractedData` dans cette SF — l'extension IA pourrait brancher
ultérieurement le flag `mariage_etranger_reconnaissance_detecte` du pipeline V2.

---

## Critères d'acceptation

- [ ] `POST` calcule et persiste un résultat ; recalcul écrase le précédent (upsert 1:1
      par dossier).
- [ ] `natureActe = MARIAGE_POLYGAME` → `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC` + motif
      `POLYGAMIE_ORDRE_PUBLIC` `HIGH`.
- [ ] `natureActe = MARIAGE_RELIGIEUX_NON_CIVIL` → `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC`
      + motif `MARIAGE_RELIGIEUX_NON_CIVIL` `HIGH`.
- [ ] `natureActe = TALAQ_REPUDIATION` + `consentementEpouse = false` →
      `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC` + motif `TALAQ_CONSENTEMENT_EPOUSE_ABSENT`.
- [ ] `natureActe = TALAQ_REPUDIATION` + tous booleans OK →
      `RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS`.
- [ ] `natureActe = TALAQ_REPUDIATION` sans booleans talaq → `400`.
- [ ] `natureActe = DIVORCE_JUDICIAIRE_ETRANGER` UE → `RECONNAISSANCE_DE_PLEIN_DROIT`,
      hors UE → `EXEQUATUR_REQUIS`.
- [ ] `natureActe = MARIAGE_CIVIL_ETRANGER` + fond OK + forme OK →
      `RECONNAISSANCE_DE_PLEIN_DROIT`.
- [ ] `natureActe = MARIAGE_CIVIL_ETRANGER` + fond KO →
      `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC`.
- [ ] La liste `motifsRefus` est vide si verdict positif, contient des codes avec
      sévérité `HIGH` si verdict négatif.
- [ ] La liste `actesAProduire` est non vide quel que soit le verdict (au minimum un
      message d'aide).
- [ ] La réponse `POST` / `GET` ré-expose l'intégralité des inputs (formulaire
      ré-éditable).
- [ ] `GET` renvoie le dernier résultat ou 404.
- [ ] `400` (corps absent, `paysOrigine` non ISO-2, date future, champ talaq manquant
      pour talaq, commentaire trop long) / `403` / `404` / `422` / `401`.
- [ ] Isolation workspace testée.

---

## Périmètre

### Hors scope
- Frontend (SF-217-17).
- Détermination automatique de la convention bilatérale applicable par pays — V1
  invite l'avocat à le faire ; outil dédié potentiel reporté.
- Effets résiduels du refus civil (succession, pension) — message d'aide uniquement,
  pas de calcul.
- Génération de la requête en transcription état civil / exequatur TF — outil dédié
  potentiel, reporté.
- Reconnaissance d'une kafala (`kafala-be-recueil-legal` — audit F-191 § 3.4,
  reporté à F-223).
- Réutilisation du Calculator FR `F-FA-18` (concepts FR de reconnaissance de mariage
  étranger structurellement proches mais articles et jurisprudence distincts).
- Pré-fill IA depuis l'analyse (documenté `PREFILL_COUNT_ALWAYS_ZERO` côté SF-217-17).
- Seed `decision_tool_visibility_rules` (porté par SF-217-17).

---

## Technique

### Endpoints
| Méthode | URL | Auth | Rôle |
|---------|-----|------|------|
| POST | `/api/v1/case-files/{caseFileId}/mariage-etranger-be-reconnaissance` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/mariage-etranger-be-reconnaissance` | Oui | MEMBER |

### Tables impactées
| Table | Opération | Notes |
|-------|-----------|-------|
| `mariage_etranger_be_reconnaissance_analyses` | CREATE + INSERT/UPDATE/SELECT | id (UUID), case_file_id (FK UNIQUE), country (VARCHAR), snapshot_data (TEXT JSON), created_at, updated_at |
| `decision_tool_visibility_rules` | INSERT (seed) | porté par SF-217-17 |

### Migration Liquibase
- [x] Oui — `276-create-mariage-etranger-be-reconnaissance-analyses.xml` (table seule).
  Numéro `276` = prochain libre après `275` (SF-217-15). À renuméroter si conflit
  au merge.

### Classes backend (pattern Vague 1+2 F-217)
`MariageEtrangerBeReconnaissanceCalculator` (static),
`MariageEtrangerBeReconnaissanceInput`, `MariageEtrangerBeReconnaissanceResult`,
`MariageEtrangerBeReconnaissanceRequest`, `MariageEtrangerBeReconnaissanceResponse`,
`MariageEtrangerBeReconnaissanceAnalysis` (@Entity),
`MariageEtrangerBeReconnaissanceRepository`,
`MariageEtrangerBeReconnaissanceService`,
`MariageEtrangerBeReconnaissanceController`.

---

## Plan de test

### Tests unitaires (Calculator)
- [ ] `MARIAGE_POLYGAME` → `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC` + motif
      `POLYGAMIE_ORDRE_PUBLIC` `HIGH`.
- [ ] `MARIAGE_RELIGIEUX_NON_CIVIL` → `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC` +
      motif `MARIAGE_RELIGIEUX_NON_CIVIL`.
- [ ] `TALAQ_REPUDIATION` + tous booleans favorables →
      `RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS`.
- [ ] `TALAQ_REPUDIATION` + `consentementEpouse = false` →
      `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC`.
- [ ] `TALAQ_REPUDIATION` + `procedureContradictoire = false` → motif réserve
      `TALAQ_PROCEDURE_NON_CONTRADICTOIRE`.
- [ ] `DIVORCE_JUDICIAIRE_ETRANGER` + pays UE → `RECONNAISSANCE_DE_PLEIN_DROIT`.
- [ ] `DIVORCE_JUDICIAIRE_ETRANGER` + pays hors UE → `EXEQUATUR_REQUIS`.
- [ ] `MARIAGE_CIVIL_ETRANGER` + fond + forme OK → `RECONNAISSANCE_DE_PLEIN_DROIT`.
- [ ] `MARIAGE_CIVIL_ETRANGER` + fond KO → `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC`,
      motif `FOND_DROIT_PERSONNEL_NON_CONFORME`.
- [ ] `conventionBilateraleApplicable = true` → message d'aide spécifique.

### Tests d'intégration (Controller)
- [ ] `POST` → 200 + persistance ; recalcul écrase.
- [ ] `GET` → 200 / 404 si jamais calculé.
- [ ] `400` (corps absent, `paysOrigine` non ISO-2 — ex `"FRA"` 3 lettres, champ
      talaq manquant pour `TALAQ_REPUDIATION`, date future) / `403` / `404` /
      `422` (domaine ≠ famille, pays ≠ Belgique) / `401`.

### Isolation workspace
- [x] Applicable.

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Outil décisionnel métier** — `mariage-etranger-be-reconnaissance` est un nouvel
  outil décisionnel. Scan : aucun outil BE existant ne couvre la reconnaissance des
  actes étrangers de mariage / divorce sous CDIP belge (F-FA-18-* sont FR-only ;
  l'immigration F-IM-* ne couvre pas le DIP famille). **Un outil = une situation** —
  outil unifié pour mariages étrangers + divorces étrangers + talaq (cohérence
  juridique : tous instruits sous le même CDIP, mêmes articles, même méthode
  d'analyse en 3 temps fond/forme/ordre public).
- [x] Aucune autre préoccupation transversale.

### Smoke tests E2E
- [x] Aucun — feature additive.

---

## Dépendances
- Aucune SF bloquante. SF-217-17 (frontend) importe le contrat API. Parallélisable.

---

## Notes et décisions
- **Outil unifié** mariages étrangers + divorces étrangers + talaq — décision motivée :
  la base juridique unique (CDIP belge) + la méthode d'analyse unique (fond / forme /
  ordre public) + les cas pratiques fortement chevauchants (un dossier marocain
  combine souvent mariage marocain + talaq) rendent la séparation artificielle.
  Le `natureActe` enum pilote la branche d'analyse. À distinguer de
  `kafala-be-recueil-legal` (audit F-191) qui relève d'une institution juridique
  distincte (recueil légal ≠ mariage / divorce) — reporté F-223.
- Persistance par snapshot JSON — pattern Vague 1+2 F-217.
- Aucune réutilisation du Calculator FR : le DIP belge (CDIP loi 16/07/2004) repose
  sur un code dédié, alors que le DIP FR (CC + Règlements UE) est fragmenté.
  Mécanismes structurellement distincts. Outil bâti depuis les sources belges
  (`feedback_belgique_never_forget`).
- Codes pays au format ISO 3166-1 alpha-2 (validation par regex `^[A-Z]{2}$`) pour
  cohérence avec les autres outils internationaux du produit.
- La distinction « pays UE » / « pays hors UE » est dérivée d'une liste statique
  documentée dans le Calculator (à mettre à jour si entrée / sortie UE — paramètre
  documenté).
- Le contenu juridique (CDIP, conventions bilatérales, jurisprudence talaq) est
  centralisé dans le Calculator et signalé pour validation par un avocat belge avant
  prod. Articles tagués `(à vérifier)` cohérents avec l'audit F-191. La validation
  est particulièrement importante pour la jurisprudence talaq (matière sensible,
  jurisprudence évolutive).
