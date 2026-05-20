# Mini-spec — [F-246 / SF-246-24] Lot Famille FR successions — booléens/énumérés `*Detected` aspirationnels

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Référence d'audit : `docs/features/F-246/SF-246-14-audit-prefill-exhaustif.md` §7.1 et §10 ligne SF-246-24.
> Référence découpage : `docs/features/F-246/cadrage-decoupage.md` vague D.
> **Modèle de référence** : SF-246-21 (lot groupé avec sous-objets thématiques).

---

## Identifiant

`F-246 / SF-246-24`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-246-24-lot-famille-successions-detected`

---

## Objectif

Résorber la **dette D2** du lot Famille FR successions/libéralités (SF-246-06) : brancher sur des sources backend réelles les **15 champs booléens/énumérés `*Detected`** déclarés dans le DTO frontend `FamilleExtractedData` mais absents du record backend `FamilleExtractedData` et du prompt `FAMILLE_INSTRUCTION`. Ces champs, lus par les helpers des 7 outils décisionnels (`acceptation-renonciation`, `reserve-heriditaire`, `rapport-succession`, `devolution-legale`, `donation`, `testament-validite`, `indivision-successorale`), retournent toujours `undefined` → `prefillFromAi()` est un no-op structurel pour eux.

---

## Comportement attendu

### Cas nominal

1. L'avocat analyse un dossier de droit de la famille FR comportant une succession, une donation, un testament ou une indivision successorale.
2. Le pipeline IA extrait, dans `famille_extracted_data`, le sous-objet `succession_detection_v2` contenant les 15 champs booléens/énumérés.
3. L'extracteur `extractFamilleData()` parse chaque champ via les fonctions appropriées (`booleanOrNull()`, `whitelistedOrNull()`).
4. Le record `FamilleExtractedData` expose les 15 nouveaux champs.
5. Les helpers des 7 outils lisent ces champs depuis le DTO frontend désormais alimenté.
6. `prefillFromAi()` de chaque composant renseigne les champs concernés avec badges `auto_awesome`.
7. `getPrefillCount()` reflète le nombre de champs effectivement renseignés.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Sous-objet `succession_detection_v2` absent | Tous les nouveaux champs `null` — no-op gracieux |
| Valeur booléenne hors `true`/`false` | `booleanOrNull()` retourne `null` |
| Valeur énumérée hors whitelist | `whitelistedOrNull()` retourne `null` |
| Dossier famille BELGIQUE | Prompt impose `null` — champs `null` |
| `aiData` arrive après le premier rendu | `prefillFromAi()` réinvoqué dans `ngOnChanges()` |

---

## Champs IA à extraire (pré-remplissage)

Stratégie : nouveau sous-objet `succession_detection_v2` dans le prompt, extension du sous-objet `succession_detection` existant côté extracteur (les deux coexistent — `succession_detection` de SF-246-06 reste inchangé).

### Tableau outil × champs ajoutés

| Outil | Champ formulaire | Champ record (`FamilleExtractedData`) | Clé JSON prompt (sous-objet `succession_detection_v2`) | Type backend | Whitelist / Contrainte | Note |
|---|---|---|---|---|---|---|
| `acceptation-renonciation` | `qualiteHeritier` | `qualiteHeritierDetectee` | `qualite_heritier` | `String` | `'PREMIER_RANG'`, `'SECOND_RANG'` | Héritier de premier ou second rang (art. 734-754 Cciv) |
| `acceptation-renonciation` | `actesEquivalentAcceptation` | `actesEquivalentAcceptationDejaPosesDetected` | `actes_equivalent_acceptation_dejas_poses` | `Boolean` | bool | Actes valant acceptation tacite (art. 783 Cciv) déjà posés |
| `acceptation-renonciation` | `dettesIncertaines` | `dettesIncertainesDetected` | `dettes_incertaines` | `Boolean` | bool | Présence de dettes incertaines dans la succession |
| `reserve-heriditaire` | `conjointSurvivant` | `conjointSurvivantDetected` | `conjoint_survivant` | `Boolean` | bool | Présence d'un conjoint survivant (art. 756 Cciv) |
| `reserve-heriditaire` | `qualiteDuDemandeur` | `qualiteDuDemandeurReserveDetecte` | `qualite_du_demandeur_reserve` | `String` | `'HERITIER_RESERVATAIRE_DESCENDANT'`, `'CONJOINT_SURVIVANT'` | Qualité du demandeur de la réserve (art. 913 Cciv) |
| `rapport-succession` | `qualiteHeritier` | `qualiteHeritierRapportDetectee` | `qualite_heritier_rapport` | `String` | `'DESCENDANT'`, `'CONJOINT_SURVIVANT'` | Qualité de l'héritier pour le rapport (art. 843 Cciv) |
| `rapport-succession` | `donationDispenseDeRapport` | `donationDispenseDeRapportDetected` | `donation_dispense_de_rapport` | `Boolean` | bool | Dispense de rapport stipulée (art. 843 al. 1 Cciv) |
| `rapport-succession` | `naturePresumeeNonRapportable` | `naturePresumeeNonRapportableDetected` | `nature_presumee_non_rapportable` | `Boolean` | bool | Nature présumée de la donation non rapportable (legs — art. 843 al. 2 Cciv) |
| `devolution-legale` | `conjointSurvivant` | `conjointSurvivantDetected` | *(mutualisé avec réserve)* | `Boolean` | bool | Même champ — mutualisé entre `reserve-heriditaire` et `devolution-legale` |
| `devolution-legale` | `tousDescendantsCommuns` | `tousDescendantsCommunsAvecConjointDetected` | `tous_descendants_communs_avec_conjoint` | `Boolean` | bool | Tous descendants communs au défunt et au conjoint (art. 757 Cciv — option ¼/usufruit) |
| `donation` | `formeDonation` | `formeDonationDetectee` | `forme_donation` | `String` | `'NOTARIEE'`, `'MANUELLE'`, `'INDIRECTE'`, `'DEGUISEE'` | Forme de la donation (art. 931-939 Cciv) |
| `donation` | `saineDEsprit` | `saineDEspritDonateurDetected` | `saine_esprit_donateur` | `Boolean` | bool | Capacité mentale du donateur (art. 901 Cciv) |
| `donation` | `respectQuotiteDisponible` | `respectQuotiteDisponibleDetected` | `respect_quotite_disponible` | `Boolean` | bool | Respect de la quotité disponible (art. 912-919-2 Cciv) |
| `testament-validite` | `formeTestament` | `formeTestamentDetectee` | `forme_testament` | `String` | `'OLOGRAPHE'`, `'AUTHENTIQUE'`, `'MYSTIQUE'` | Forme du testament (art. 967-1035 Cciv) |
| `testament-validite` | `saineDEsprit` | `saineDEspritTestateurDetected` | `saine_esprit_testateur` | `Boolean` | bool | Capacité mentale du testateur (art. 901 Cciv) |
| `testament-validite` | `legsExcedeQuotiteDisponible` | `legsExcedeQuotiteDisponibleDetected` | `legs_excede_quotite_disponible` | `Boolean` | bool | Le legs excède la quotité disponible (atteinte à la réserve) |

**Note de mutualisation** : `conjointSurvivantDetected` est un seul champ dans le record mais consommé par deux outils (`reserve-heriditaire` et `devolution-legale`). `qualiteHeritierDetectee` (pour `acceptation-renonciation`) est **distinct** de `qualiteHeritierRapportDetectee` (pour `rapport-succession`) — deux contextes juridiques différents.

---

## Critères d'acceptation

- [ ] Le record `FamilleExtractedData` contient les 15 nouveaux champs (types corrects, nullables).
- [ ] Le prompt `FAMILLE_INSTRUCTION` décrit le sous-objet `succession_detection_v2` avec les 15 clés, whitelists fermées et définitions juridiques sans ambiguïté.
- [ ] `extractFamilleData()` parse le sous-objet `succession_detection_v2` via `booleanOrNull()` et `whitelistedOrNull()`.
- [ ] Le DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`) documente la source backend réelle (SF-246-24) pour chaque champ.
- [ ] Les helpers des 7 outils lisent les champs depuis `FamilleExtractedData` sans type d'intersection aspirationnel.
- [ ] `prefillFromAi()` de chaque composant est effectif sur les champs concernés.
- [ ] `getPrefillCount()` reflète les champs réellement renseignés.
- [ ] Tests backend : cas nominal, valeur hors whitelist, sous-objet absent, bool non parseable.
- [ ] Tests frontend : cas 0 / nominal / parité count ↔ prefill.
- [ ] Smoke E2E : pas de nouveaux échecs.

---

## Périmètre

### Hors scope (explicite)

- Tout champ date/montant des 7 outils (déjà branchés par SF-246-06).
- Les 7 outils Famille FR lots SF-246-25 / SF-246-26 / SF-246-27 (régimes, vie commune, filiation, autorité parentale).
- Tout outil Famille BE (vague E).
- Toute migration Liquibase — aucune table impactée.

---

## Analyse d'impact

### Préoccupations transversales

- [x] **Outil décisionnel métier** — 7 outils modifiés. Composants impactés : `AcceptationRenonciation`, `ReserveHeriditaire`, `RapportSuccession`, `DevolutionLegale`, `Donation`, `TestamentValidite`, `IndivisionSuccessorale`.
- [ ] Auth / Principal — non
- [ ] Workspace context — non
- [ ] Plans / limites — non
- [ ] Navigation / routing — non

### Composants / endpoints potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|---|---|---|
| `extractFamilleData()` | Nouveau sous-objet parsé — additif, nullable | Tests d'extraction SF-246-24 |
| 7 helpers `*-prefill-rules.ts` | Lecture de champs réels (suppression du cast aspirationnel) | Tests Jest cas 0 / nominal |
| 7 composants `*-section` | `prefillFromAi()` effectif sur nouveaux champs | Tests Jest parité |
| Autres outils Famille FR consommant `familleExtractedData` | Champs additifs ignorés | Compilation TS |

---

## Dépendances

- **SF-246-11** — record `FamilleExtractedData` partagé. SF-246-24 démarrée après merge de SF-246-11 (confirmé : PR #1138 mergée).

---

## Notes et décisions

### Structure `succession_detection_v2`

Le sous-objet `succession_detection` de SF-246-06 contenait déjà 16 champs date/montant/dénombrement. Plutôt que d'alourdir ce sous-objet (qui instruit déjà le LLM sur des concepts distincts), SF-246-24 ajoute un **sous-objet complémentaire `succession_detection_v2`** spécifiquement dédié aux booléens et énumérés de qualification juridique. Le LLM ne renseigne ce sous-objet que si des éléments probants sont présents dans les pièces (no-op gracieux si absent).

### `booleanOrNull()`

Les champs booléens utilisent une nouvelle fonction `booleanOrNull(node, key)` (analogue à `isoDateOrNull()`) qui retourne `Boolean` nullable. Un champ manquant ou non-booléen retourne `null` (pas de valeur par défaut — invariant §5.1.2).
