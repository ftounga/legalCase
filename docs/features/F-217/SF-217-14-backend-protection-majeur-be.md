# Mini-spec — F-217 / SF-217-14 — Backend : protection du majeur incapable (Belgique)

## Identifiant
`F-217 / SF-217-14`

## Feature parente
`F-217` — P2 Famille BE — outils décisionnels de fréquence haute (Vague 3 — Successions / protection / international)

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-217-14-protection-majeur-be`

---

## Objectif

Fournir un outil décisionnel backend qui qualifie la situation du majeur incapable et
recommande la voie de protection adéquate sous le régime belge unique de
l'administration (loi du 17/03/2013, en vigueur le 01/09/2014 — à vérifier) :
administration des biens, administration de la personne, mandat extra-judiciaire,
déclaration anticipée, à partir des éléments du dossier.

---

## Comportement attendu

### Cas nominal

1. L'avocat soumet les éléments de la situation du majeur : nature de l'altération
   (`MEDICALE_DURABLE` / `MEDICALE_TEMPORAIRE` / `PRODIGALITE` / `AUTRE`), gravité de
   l'incapacité (`INCAPACITE_GERER_BIENS` / `INCAPACITE_GERER_PERSONNE` / `LES_DEUX` /
   `INCAPACITE_PARTIELLE`), existence d'un mandat extra-judiciaire signé par le majeur
   alors qu'il était encore capable, existence d'une déclaration anticipée du majeur
   désignant son administrateur préféré, existence d'un environnement familial
   protecteur, urgence (`URGENCE_VITALE` / `URGENCE_PATRIMONIALE` / `AUCUNE_URGENCE`),
   et le mode de saisine envisagé (`REQUETE_JP` / `INDETERMINE`).
2. Le `ProtectionMajeurBeCalculator` applique l'arbre décisionnel de la **loi du
   17/03/2013** (statut unique d'administration remplaçant administration provisoire /
   minorité prolongée / interdiction) : (a) détermine la mesure adéquate
   (`MANDAT_EXTRA_JUDICIAIRE_VALABLE` si mandat signé avant l'incapacité et toujours
   adapté ; sinon `ADMINISTRATION_BIENS` / `ADMINISTRATION_PERSONNE` /
   `ADMINISTRATION_BIENS_ET_PERSONNE` selon la gravité) ; (b) calcule l'urgence
   procédurale ; (c) recommande la juridiction compétente (Justice de paix —
   à vérifier ; chiffrée par CJ art. 594-595 ou équivalent) ; (d) liste les actes
   protégés par la mesure (actes de disposition vs administration).
3. Le calculateur produit un verdict (`MANDAT_EXTRA_JUDICIAIRE_VALABLE` /
   `MESURE_PROTECTION_RECOMMANDEE` / `URGENCE_MESURE_PROVISOIRE` /
   `QUALIFICATION_INCOMPLETE`), la mesure recommandée, la juridiction de saisine, la
   liste des actes protégés, les bases juridiques mobilisées et des messages d'aide.
4. Le résultat est persisté par dossier (un seul résultat courant par dossier, écrasé
   au recalcul) et renvoyé.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Corps de requête absent | Message d'erreur explicite | 400 |
| Champ enum obligatoire absent ou invalide | Message d'erreur explicite | 400 |
| Commentaire > 1000 caractères | Message d'erreur explicite | 400 |
| Non authentifié | — | 401 |
| Dossier d'un autre workspace | Accès refusé | 403 |
| Dossier inexistant | — | 404 |
| `GET` sans calcul préalable | — | 404 |
| Dossier dont le `legalDomain` n'est pas `DROIT_FAMILLE` | Message explicite | 422 |
| Workspace dont le `country` n'est pas `BELGIQUE` (outil BE-only) | Message explicite | 422 |

---

## Contrat API (FIGÉ — importé par SF-217-15)

### POST `/api/v1/case-files/{caseFileId}/protection-majeur-be`

Body `ProtectionMajeurBeRequest` :
```json
{
  "natureAlteration": "MEDICALE_DURABLE",
  "graviteIncapacite": "LES_DEUX",
  "mandatExtraJudiciaireSigne": false,
  "mandatExtraJudiciaireDateSignature": null,
  "declarationAnticipeeExiste": false,
  "environnementFamilialProtecteur": true,
  "niveauUrgence": "URGENCE_PATRIMONIALE",
  "modeSaisineEnvisage": "REQUETE_JP",
  "commentaire": null
}
```
- `natureAlteration` : enum obligatoire.
- `graviteIncapacite` : enum obligatoire.
- `mandatExtraJudiciaireSigne` : boolean obligatoire.
- `mandatExtraJudiciaireDateSignature` : `yyyy-MM-dd`, nullable, obligatoire si
  `mandatExtraJudiciaireSigne = true`, postérieure à 2014-09-01 (entrée en vigueur de
  la loi — à vérifier).
- `declarationAnticipeeExiste` : boolean obligatoire.
- `environnementFamilialProtecteur` : boolean obligatoire (influence le choix de
  l'administrateur de la personne — un proche peut être désigné, à défaut un
  administrateur professionnel).
- `niveauUrgence` : enum obligatoire.
- `modeSaisineEnvisage` : enum obligatoire.
- `commentaire` : nullable, max 1000 caractères.
- Le pays est dérivé du workspace côté service.

Réponse `200` — `ProtectionMajeurBeResponse` : **ré-expose l'intégralité du body**
(snapshot) **+** les champs calculés.
```json
{
  "caseFileId": "uuid",
  "natureAlteration": "MEDICALE_DURABLE",
  "graviteIncapacite": "LES_DEUX",
  "mandatExtraJudiciaireSigne": false,
  "mandatExtraJudiciaireDateSignature": null,
  "declarationAnticipeeExiste": false,
  "environnementFamilialProtecteur": true,
  "niveauUrgence": "URGENCE_PATRIMONIALE",
  "modeSaisineEnvisage": "REQUETE_JP",
  "commentaire": null,
  "verdict": "MESURE_PROTECTION_RECOMMANDEE",
  "mesureRecommandee": "ADMINISTRATION_BIENS_ET_PERSONNE",
  "juridictionCompetente": "JUSTICE_PAIX",
  "fondementProcedural": "Loi 17/03/2013 (à vérifier) ; CJ art. 594-595 (à vérifier) — compétence Justice de paix de la résidence du majeur",
  "actesProteges": [
    {
      "code": "DISPOSITION_BIENS_IMMOBILIERS",
      "libelle": "Disposition d'un bien immobilier (vente, hypothèque)",
      "necessite": "AUTORISATION_JP_PREALABLE",
      "fondement": "Loi 17/03/2013 — actes de disposition soumis à autorisation préalable de la justice de paix (à vérifier)"
    },
    {
      "code": "ACTES_QUOTIDIENS",
      "libelle": "Actes de la vie quotidienne (subsistance, soins courants)",
      "necessite": "AUTONOMIE_PRESERVEE",
      "fondement": "Loi 17/03/2013 — principe de capacité résiduelle / autonomie maximale (à vérifier)"
    }
  ],
  "actionsConcretes": [
    "Saisir la Justice de paix de la résidence du majeur par requête (à vérifier — modèle à fournir par F-217 vague ultérieure ou F-223).",
    "Joindre un certificat médical circonstancié de moins de 15 jours (à vérifier).",
    "Le cas échéant, proposer un administrateur (proche identifié comme `environnementFamilialProtecteur = true`)."
  ],
  "basesJuridiques": [
    "Loi du 17/03/2013 (à vérifier) — statut unique de protection des majeurs (administration de la personne et/ou des biens)",
    "CJ art. 594-595 (à vérifier) — compétence Justice de paix",
    "CC art. 490 nouveau (à vérifier) — mandat extra-judiciaire"
  ],
  "messages": [
    "Incapacité durable + actes patrimoniaux à protéger : administration des biens recommandée. L'altération s'étend à la personne (soins, lieu de vie) → administration de la personne également recommandée.",
    "Pas de mandat extra-judiciaire ni de déclaration anticipée : la juridiction désignera l'administrateur. Environnement familial protecteur identifié → proposer un proche comme administrateur."
  ],
  "country": "BELGIQUE",
  "calculatedAt": "2026-05-20T10:00:00Z"
}
```

### GET `/api/v1/case-files/{caseFileId}/protection-majeur-be`
- `200` : dernier résultat (même structure, inputs inclus → formulaire ré-éditable).
  `404` si jamais calculé. `403` / `401` / `422` idem POST.

### Enum `verdict` (`ProtectionMajeurBeVerdict`)
- `MANDAT_EXTRA_JUDICIAIRE_VALABLE` — mandat signé alors que le majeur était capable +
  toujours adapté à la situation : la voie privée prime, pas de saisine judiciaire
  nécessaire (mais activation du mandat).
- `MESURE_PROTECTION_RECOMMANDEE` — mesure d'administration judiciaire nécessaire ;
  procédure normale par requête à la Justice de paix.
- `URGENCE_MESURE_PROVISOIRE` — urgence vitale ou patrimoniale critique : mesure
  provisoire envisageable (saisine urgente JP — à vérifier).
- `QUALIFICATION_INCOMPLETE` — combinaison d'inputs ne permettant pas de trancher
  (ex : `mandatExtraJudiciaireSigne = true` sans date de signature).

### Enum `natureAlteration` (`NatureAlterationBe`)
`MEDICALE_DURABLE` · `MEDICALE_TEMPORAIRE` · `PRODIGALITE` · `AUTRE`.

### Enum `graviteIncapacite` (`GraviteIncapaciteBe`)
`INCAPACITE_GERER_BIENS` · `INCAPACITE_GERER_PERSONNE` · `LES_DEUX` ·
`INCAPACITE_PARTIELLE`.

### Enum `niveauUrgence` (`NiveauUrgenceProtectionBe`)
`URGENCE_VITALE` · `URGENCE_PATRIMONIALE` · `AUCUNE_URGENCE`.

### Enum `modeSaisineEnvisage` (`ModeSaisineProtectionBe`)
`REQUETE_JP` · `INDETERMINE`.

### Enum `mesureRecommandee` (`MesureProtectionBe`)
`MANDAT_EXTRA_JUDICIAIRE` (le mandat signé suffit, pas de saisine) ·
`ADMINISTRATION_BIENS` · `ADMINISTRATION_PERSONNE` ·
`ADMINISTRATION_BIENS_ET_PERSONNE` · `MESURE_PROVISOIRE_URGENCE` · `AUCUNE_RECOMMANDATION`.

### Enum `juridictionCompetente` (`JuridictionProtectionBe`)
`JUSTICE_PAIX` (compétence de principe — à vérifier) ·
`AUTRE_JURIDICTION` (cas particuliers).

### Enum `code` d'un acte protégé (`ActeProtegeBeCode`)
`DISPOSITION_BIENS_IMMOBILIERS` · `DISPOSITION_BIENS_MOBILIERS_VALEURS` ·
`OPERATION_BANCAIRE` · `ACTES_PATRIMONIAUX_COURANTS` · `ACTES_PERSONNELS_LIEU_VIE` ·
`ACTES_PERSONNELS_SOINS` · `MARIAGE_PACS_TESTAMENT` · `ACTES_QUOTIDIENS`.

### Enum `necessite` d'un acte protégé (`NecessiteActeBe`)
`AUTORISATION_JP_PREALABLE` · `ADMINISTRATEUR_SEUL` · `AUTONOMIE_PRESERVEE` ·
`INTERDICTION_ABSOLUE` (cas très restrictifs — à vérifier).

---

## Règles de l'arbre décisionnel

> ⚠️ **Validation juridique requise** : la loi du **17/03/2013** (entrée en vigueur
> 01/09/2014) a refondu le droit belge de la protection des majeurs en créant un
> **statut unique d'administration** (qui remplace les anciens régimes de minorité
> prolongée, administration provisoire des biens, conseil judiciaire et interdiction).
> Les références aux articles du Code judiciaire (CJ art. 594-595 — compétence
> Justice de paix), à la **prééminence de la capacité résiduelle** (autonomie maximale —
> principe central de la loi 2013) et au **mandat extra-judiciaire** (CC art. 490
> nouveau — possibilité pour une personne capable d'organiser sa protection future)
> reflètent l'état du droit connu du modèle et sont **à valider par un avocat belge
> avant mise en production**. Le contenu juridique est centralisé dans le Calculator
> (source unique de vérité).

### Détermination du verdict (figée dans le Calculator)

1. `mandatExtraJudiciaireSigne = true` ET `mandatExtraJudiciaireDateSignature = null`
   → `QUALIFICATION_INCOMPLETE`.
2. `mandatExtraJudiciaireSigne = true` ET date avant 2014-09-01 (loi non encore en
   vigueur — à vérifier) → `QUALIFICATION_INCOMPLETE` + message d'aide « mandat
   antérieur à la loi du 17/03/2013, validité à confirmer ».
3. `mandatExtraJudiciaireSigne = true` (date valide) →
   `MANDAT_EXTRA_JUDICIAIRE_VALABLE`, `mesureRecommandee = MANDAT_EXTRA_JUDICIAIRE`,
   `juridictionCompetente = null` (voie privée).
4. `niveauUrgence = URGENCE_VITALE` → `URGENCE_MESURE_PROVISOIRE`,
   `mesureRecommandee = MESURE_PROVISOIRE_URGENCE`.
5. Sinon → `MESURE_PROTECTION_RECOMMANDEE`, mesure calculée selon
   `graviteIncapacite` :
   - `INCAPACITE_GERER_BIENS` → `ADMINISTRATION_BIENS`.
   - `INCAPACITE_GERER_PERSONNE` → `ADMINISTRATION_PERSONNE`.
   - `LES_DEUX` → `ADMINISTRATION_BIENS_ET_PERSONNE`.
   - `INCAPACITE_PARTIELLE` → `ADMINISTRATION_BIENS` (par défaut, à raffiner cas par
     cas — paramètre documenté).

### Actes protégés exposés

| Acte | Nécessité | Fondement (à valider) |
|------|-----------|------------------------|
| Vente / hypothèque d'un immeuble | `AUTORISATION_JP_PREALABLE` | Loi 17/03/2013 — actes de disposition |
| Vente de valeurs mobilières significatives | `AUTORISATION_JP_PREALABLE` | Idem |
| Opérations bancaires courantes | `ADMINISTRATEUR_SEUL` | Loi 17/03/2013 — actes d'administration |
| Choix du lieu de vie | `AUTORISATION_JP_PREALABLE` (si mesure étendue à la personne) | Loi 17/03/2013 |
| Soins médicaux courants | `AUTONOMIE_PRESERVEE` (sauf décision contraire de la JP) | Loi 17/03/2013 — principe de capacité résiduelle |
| Mariage, PACS, testament | Cas spécifiques — à vérifier (mariage parfois soumis à autorisation JP, testament principe d'autonomie sous conditions médicales) | Loi 17/03/2013 — à valider |
| Actes de la vie quotidienne | `AUTONOMIE_PRESERVEE` | Loi 17/03/2013 |

### Détermination de la juridiction

- Par défaut : `JUSTICE_PAIX` de la résidence du majeur (CJ art. 594-595 — à vérifier).
- Exception : `AUTRE_JURIDICTION` documenté pour les cas où la compétence est partagée
  (ex : majeur résidant en établissement spécialisé — à vérifier).

---

## Conformité F-IA-04
- [x] **Non applicable au sens strict** — SF backend pure. Conformité F-IA-04
  (TOOL_REGISTRY, pré-fill, F-IA-03, gate `workspaceCountry`) et seed
  `decision_tool_visibility_rules` portés par SF-217-15 frontend (couplé TOOL_REGISTRY).

---

## Champs IA à extraire (pré-remplissage IA — V1)

| Champ | Source backend potentielle | Statut V1 |
|-------|----------------------------|-----------|
| `natureAlteration` | Détection « Alzheimer », « démence », « altération facultés mentales » | Aspirationnel — non extrait V1 |
| `niveauUrgence` | Détection urgence vitale dans documents | Aspirationnel — non extrait V1 |

**V1 : `PREFILL_COUNT_ALWAYS_ZERO = true`** côté SF-217-15. Pas de nouveau champ
ajouté à `FamilleExtractedData` dans cette SF — l'extension IA sera tranchée à une SF
ultérieure (potentiel flag `protection_majeur_be_detectee` mentionné dans
l'audit F-191).

---

## Critères d'acceptation

- [ ] `POST` calcule et persiste un résultat ; recalcul écrase le précédent (upsert 1:1
      par dossier).
- [ ] Mandat extra-judiciaire signé après 2014-09-01 → `MANDAT_EXTRA_JUDICIAIRE_VALABLE`,
      pas de juridiction recommandée.
- [ ] Urgence vitale → `URGENCE_MESURE_PROVISOIRE`, mesure
      `MESURE_PROVISOIRE_URGENCE`.
- [ ] Incapacité « les deux » → `ADMINISTRATION_BIENS_ET_PERSONNE`.
- [ ] Incapacité gestion biens seule → `ADMINISTRATION_BIENS`.
- [ ] Incapacité gestion personne seule → `ADMINISTRATION_PERSONNE`.
- [ ] Mandat extra-judiciaire signé sans date → `QUALIFICATION_INCOMPLETE`.
- [ ] Mandat signé avant 2014-09-01 → `QUALIFICATION_INCOMPLETE` + message d'aide.
- [ ] Liste `actesProteges` non vide pour toute mesure d'administration.
- [ ] La réponse `POST` / `GET` ré-expose l'intégralité des inputs (formulaire
      ré-éditable).
- [ ] `GET` renvoie le dernier résultat ou 404.
- [ ] `400` / `403` / `404` / `422` / `401`.
- [ ] Isolation workspace testée.

---

## Périmètre

### Hors scope
- Frontend (SF-217-15).
- Génération de la requête à la Justice de paix — outil de génération dédié potentiel,
  reporté.
- Génération du mandat extra-judiciaire (`protection-be-mandat-extra-judiciaire` —
  audit F-191 § 3.7, reporté à F-223).
- Génération de la déclaration anticipée (`protection-be-declaration-anticipee` —
  audit F-191 § 3.7, reporté).
- Tutelle des mineurs (`protection-mineur-tutelle-be` — reporté F-223).
- Réutilisation du Calculator FR `F-FA-25` (régimes FR tutelle / curatelle / sauvegarde
  de justice — mécanisme distinct).
- Pré-fill IA depuis l'analyse (documenté `PREFILL_COUNT_ALWAYS_ZERO` côté SF-217-15).
- Seed `decision_tool_visibility_rules` (porté par SF-217-15).

---

## Technique

### Endpoints
| Méthode | URL | Auth | Rôle |
|---------|-----|------|------|
| POST | `/api/v1/case-files/{caseFileId}/protection-majeur-be` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/protection-majeur-be` | Oui | MEMBER |

### Tables impactées
| Table | Opération | Notes |
|-------|-----------|-------|
| `protection_majeur_be_analyses` | CREATE + INSERT/UPDATE/SELECT | id (UUID), case_file_id (FK UNIQUE), country (VARCHAR), snapshot_data (TEXT JSON), created_at, updated_at |
| `decision_tool_visibility_rules` | INSERT (seed) | porté par SF-217-15 |

### Migration Liquibase
- [x] Oui — `274-create-protection-majeur-be-analyses.xml` (table seule). Numéro `274`
  = prochain libre après `273` (SF-217-13). À renuméroter si conflit au merge.

### Classes backend (pattern Vague 1+2 F-217)
`ProtectionMajeurBeCalculator` (static), `ProtectionMajeurBeInput`,
`ProtectionMajeurBeResult`, `ProtectionMajeurBeRequest`, `ProtectionMajeurBeResponse`,
`ProtectionMajeurBeAnalysis` (@Entity), `ProtectionMajeurBeRepository`,
`ProtectionMajeurBeService`, `ProtectionMajeurBeController`.

---

## Plan de test

### Tests unitaires (Calculator)
- [ ] Médicale durable + les deux + pas de mandat + urgence patrimoniale →
      `MESURE_PROTECTION_RECOMMANDEE`, `ADMINISTRATION_BIENS_ET_PERSONNE`,
      `JUSTICE_PAIX`.
- [ ] Médicale durable + gestion biens seule → `ADMINISTRATION_BIENS`.
- [ ] Médicale durable + gestion personne seule → `ADMINISTRATION_PERSONNE`.
- [ ] Mandat extra-judiciaire signé après 2014-09-01 → `MANDAT_EXTRA_JUDICIAIRE_VALABLE`.
- [ ] Mandat signé sans date → `QUALIFICATION_INCOMPLETE`.
- [ ] Mandat signé avant 2014-09-01 → `QUALIFICATION_INCOMPLETE` + message.
- [ ] Urgence vitale → `URGENCE_MESURE_PROVISOIRE`.
- [ ] Liste `actesProteges` produite pour `ADMINISTRATION_BIENS_ET_PERSONNE`.

### Tests d'intégration (Controller)
- [ ] `POST` → 200 + persistance ; recalcul écrase.
- [ ] `GET` → 200 / 404 si jamais calculé.
- [ ] `400` (corps absent, enum invalide, commentaire trop long) / `403` / `404` /
      `422` (domaine ≠ famille, pays ≠ Belgique) / `401`.

### Isolation workspace
- [x] Applicable.

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Outil décisionnel métier** — `protection-majeur-be` est un nouvel outil
  décisionnel. Scan : aucun outil BE existant ne couvre la protection du majeur sous
  régime belge (F-FA-25 est FR-only ; les régimes FR — tutelle / curatelle /
  sauvegarde de justice — sont structurellement distincts du **régime unique
  d'administration** issu de la loi du 17/03/2013). **Un outil = une situation**.
- [x] Aucune autre préoccupation transversale.

### Smoke tests E2E
- [x] Aucun — feature additive.

---

## Dépendances
- Aucune SF bloquante. SF-217-15 (frontend) importe le contrat API. Parallélisable.

---

## Notes et décisions
- Persistance par snapshot JSON — pattern Vague 1+2 F-217.
- Aucune réutilisation du Calculator FR : la loi belge du 17/03/2013 a créé un
  **statut unique** d'administration (de la personne, des biens, ou les deux), avec
  un principe central de capacité résiduelle (autonomie maximale). La France conserve
  les trois régimes tutelle / curatelle / sauvegarde de justice (CC FR art. 415+).
  Mécanismes structurellement distincts — outil bâti depuis les sources belges
  (`feedback_belgique_never_forget`).
- La date de mise en vigueur de la loi (2014-09-01) est utilisée comme garde-fou
  pour les mandats extra-judiciaires antérieurs (à confirmer juridiquement —
  certains mandats antérieurs peuvent rester valables sous le droit transitoire).
- Le contenu juridique (articles de la loi 17/03/2013, CJ, CC art. 490 nouveau,
  fondements, actes protégés) est centralisé dans le Calculator et signalé pour
  validation par un avocat belge avant prod. Articles tagués `(à vérifier)` cohérents
  avec l'audit F-191.
