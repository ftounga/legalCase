# Mini-spec — F-246 / SF-246-26 — Lot Famille FR filiation & autorité parentale — champs `*Detected`

## Identifiant

`F-246 / SF-246-26`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-246-26-lot-famille-fr-filiation-autorite-detected`

---

## Objectif

Brancher les champs `*Detected` booléens et énumérés aspirationnels sur 8 outils Famille FR (`contestation-paternite`, `recherche-paternite`, `reconnaissance-paternelle`, `adoption`, `possession-etat`, `autorite-parentale`, `changement-residence`, `desaccords-parentaux`) en ajoutant un sous-objet `filiation_detection_v2` au prompt et au record backend, en étendant l'extracteur, et en supprimant les casts `as any` résiduels dans les composants.

**Prérequis** : SF-246-25 mergée (record `FamilleExtractedData` partagé).

---

## Contexte dette D2

Le sous-objet `filiation_detection` (SF-246-09) couvre uniquement les dates et âges. Les champs de qualification juridique booléens/énumérés (qualité à agir, motifs sérieux, expertise ADN, possession d'état, etc.) sont déclarés dans le DTO frontend mais absents du record backend et du prompt LLM :

**`contestation-paternite`** — 4 champs aspirationnels :
- `qualiteAagirContestationDetected` (enum : `PERE_DECLARE` | `PERE_BIOLOGIQUE_PRESUME` | `MERE` | `ENFANT_MAJEUR`)
- `possessionEtatConforme5AnsDetected` (boolean)
- `expertiseAdnDemandeeDetected` (boolean)
- `motifsSerieuxDetected` (boolean)

**`recherche-paternite`** — 5 champs aspirationnels :
- `qualiteDuDemandeurRechercheDetected` (enum : `ENFANT_MAJEUR` | `REPRESENTANT_LEGAL_MINEUR` | `MERE`)
- `presomptionPossessionEtatRechercheDetected` (boolean)
- `expertiseAdnDemandeeRechercheDetected` (boolean)
- `pereDesigneRefuseADNDetected` (boolean)
- `motifsSerieuxRechercheDetected` (boolean)

**`reconnaissance-paternelle`** — 4 champs déjà réels (SF-246-09) sauf : tous OK — pas de dette D2 résiduelle. Seule correction : `as any` éventuel à vérifier.

**`adoption`** — 3 champs déjà réels (SF-246-09 : ageAdoptant, ageAdopte + 3 booléens existants) sauf : `formeAdoptionDemandeeDetected`, `pupilleEtatDetected`, `adoptantMarieDetected` (présents en frontend, absents du backend).

**`autorite-parentale`**, **`changement-residence`**, **`desaccords-parentaux`** — champs déjà réels (SF-246-10 : agesEnfantsDetectes). Pas de dette D2 supplémentaire → vérification des `as any` seulement.

**`possession-etat`** — vérifier si le helper lit des champs aspirationnels.

---

## Comportement attendu

### Cas nominal

Le pipeline IA extrait un sous-objet `filiation_detection_v2` contenant les qualifications juridiques booléennes/énumérées pour les outils de filiation. Le backend parse ce sous-objet et renseigne les nouveaux champs nullable du record `FamilleExtractedData`. Côté frontend, `prefillFromAi()` de chaque composant lit ces champs réels (plus d'`as any`) et pré-remplit les énumérations/booléens avec badge `auto_awesome` + signal provenance.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Sous-objet `filiation_detection_v2` absent | Tous les nouveaux champs = null, no-op gracieux | N/A |
| Qualité à agir hors whitelist | `whitelistedOrNull()` → null | N/A |
| Booléen non parseable | `booleanOrNull()` → null | N/A |
| Dossier belge | Sous-objet null (prompt impose null hors FR) | N/A |

---

## Analyse de cohérence transversale

- [x] **Autres outils Famille FR** : les 8 outils du lot + lot régimes (SF-246-25 déjà mergée)
- [x] **Belgique** : nouveau sous-objet null pour les dossiers BE (prompt le précise)
- [x] **Outil décisionnel métier** : pas de nouveau composant — modification du pré-fill d'outils existants
- [x] **Préoccupations transversales** : pas de nouveau endpoint, pas de changement auth/workspace/routing

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `contestation-paternite` champs qualif | Oui | Intégré dans cette SF |
| `recherche-paternite` champs qualif | Oui | Intégré dans cette SF |
| `reconnaissance-paternelle` as any | Oui (vérif) | Intégré si trouvé |
| `adoption` 3 booléens | Oui | Intégré dans cette SF |
| `possession-etat` champs aspirationnels | Oui (vérif) | Intégré si trouvé |
| `autorite-parentale` / `changement-residence` / `desaccords-parentaux` | Vérif as any | Intégré si trouvé |
| Outils BE filiation | Non | Non applicable — scope BE = SF-246-28 |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature

---

## Conformité F-IA-04

Non applicable — SF de pré-fill sur outils existants déjà enregistrés dans `TOOL_REGISTRY`. Pas de nouveau composant décisionnel.

---

## Champs IA à extraire (pré-remplissage)

Nouveau sous-objet prompt : `filiation_detection_v2`

| Outil | Champ du formulaire | Type | Champ source `FamilleExtractedData` | Source JSON |
|-------|---------------------|------|--------------------------------------|-------------|
| `contestation-paternite` | qualiteAagir (enum) | string enum | `qualiteAagirContestationDetected` | `filiation_detection_v2.qualite_aagir_contestation` |
| `contestation-paternite` | possessionEtatConforme5Ans (bool) | boolean | `possessionEtatConforme5AnsDetected` | `filiation_detection_v2.possession_etat_conforme_5ans` |
| `contestation-paternite` | expertiseAdnDemandee (bool) | boolean | `expertiseAdnDemandeeDetected` | `filiation_detection_v2.expertise_adn_demandee_contestation` |
| `contestation-paternite` | motifsSerieux (bool) | boolean | `motifsSerieuxDetected` | `filiation_detection_v2.motifs_serieux_contestation` |
| `recherche-paternite` | qualiteDuDemandeur (enum) | string enum | `qualiteDuDemandeurRechercheDetected` | `filiation_detection_v2.qualite_demandeur_recherche` |
| `recherche-paternite` | presomptionPossessionEtat (bool) | boolean | `presomptionPossessionEtatRechercheDetected` | `filiation_detection_v2.presomption_possession_etat_recherche` |
| `recherche-paternite` | expertiseAdnDemandee (bool) | boolean | `expertiseAdnDemandeeRechercheDetected` | `filiation_detection_v2.expertise_adn_demandee_recherche` |
| `recherche-paternite` | pereDesigneRefuseADN (bool) | boolean | `pereDesigneRefuseADNDetected` | `filiation_detection_v2.pere_designe_refuse_adn` |
| `recherche-paternite` | motifsSerieux (bool) | boolean | `motifsSerieuxRechercheDetected` | `filiation_detection_v2.motifs_serieux_recherche` |
| `adoption` | formeAdoption (enum) | string enum | `formeAdoptionDemandeeDetected` | `filiation_detection_v2.forme_adoption_demandee` |
| `adoption` | pupilleEtat (bool) | boolean | `pupilleEtatDetected` | `filiation_detection_v2.pupille_etat` |
| `adoption` | adoptantMarie (bool) | boolean | `adoptantMarieDetected` | `filiation_detection_v2.adoptant_marie` |

**Whitelists fermées :**
- `qualite_aagir_contestation` : `PERE_DECLARE`, `PERE_BIOLOGIQUE_PRESUME`, `MERE`, `ENFANT_MAJEUR`
- `qualite_demandeur_recherche` : `ENFANT_MAJEUR`, `REPRESENTANT_LEGAL_MINEUR`, `MERE`
- `forme_adoption_demandee` : `PLENIERE`, `SIMPLE`

**Champs déjà réels (SF-246-09, dans `filiation_detection`) — non à créer :**
- `dateEtablissementFiliationDetectee`, `dateConnaissanceVeriteDetectee`, `dateMajoriteEnfantDetectee`, `dateNaissanceEnfantRechercheDetectee`, `dateNaissanceEnfantDetectee`, `ageAdoptantDetecte`, `ageAdopteDetecte`

**Champs déjà réels (SF-246-10, dans `autorite_parentale_detection`) — non à créer :**
- `agesEnfantsDetectes`, `dateDebutCalendrierDetectee`, `dateFinCalendrierDetectee`

**Champs déjà dans FamilleExtractedData et frontend (non aspirationnels) :**
- `consentementLibreDuPereDetected`, `paterniteVraisemblableDetected`, `enfantNonReconnuParAutrePereDetected`, `procedureRespecteeReconnaissanceDetected` — à vérifier si alimentés backend ou aspirationnels

---

## Critères d'acceptation

- [ ] `FamilleExtractedData` (Java record) contient les 12 nouveaux champs nullable via Builder F-234
- [ ] `FAMILLE_INSTRUCTION` contient un sous-objet `filiation_detection_v2` documenté avec whitelists, définitions juridiques et garde FR-only
- [ ] `extractFamilleData()` parse `filiation_detection_v2` : `booleanOrNull()`, `whitelistedOrNull()` pour les énumérations
- [ ] `FamilleExtractedData` (TypeScript) a les JSDoc mis à jour
- [ ] Aucun `as any` résiduel dans les composants du lot (contestation-paternite, recherche-paternite en particulier)
- [ ] `prefillFromAi()` de chaque composant lit les champs réels
- [ ] `computePrefillCount()` de chaque helper retourne > 0 pour un input avec champs renseignés
- [ ] Tests backend : nominal `filiation_detection_v2`, sous-objet absent, hors whitelist, booléen non parseable
- [ ] Tests Jest frontend : prefillCount = 0 (vide), N champs partiels, N champs nominaux — par composant concerné
- [ ] Smoke E2E : ~27 échecs préexistants tolérés, aucun nouveau

---

## Périmètre

### Dans scope

- 12 nouveaux champs backend (record + prompt + extracteur) dans `filiation_detection_v2`
- Vérification et correction des `as any` dans les 8 composants du lot
- Vérification des champs booléens de `reconnaissance-paternelle` et `adoption` (déjà en frontend, à checker si alimentés backend ou aspirationnels)
- Mise à jour JSDoc DTO frontend
- Tests backend + Jest frontend

### Hors scope

- Champs dates/âges déjà branchés en SF-246-09 et SF-246-10
- Outils `possession-etat` et `autorite-parentale` si aucun champ aspirationnel trouvé après vérification
- Outils Famille BE (SF-246-28)
- Nouveau formulaire / endpoint backend décisionnel

---

## Technique

### Tables impactées

Aucune (modification du parsing JSON LLM uniquement).

### Migration Liquibase

Non applicable.

### Composants Angular impactés

- `contestation-paternite-section.component.ts` — suppression `as any`, prefillFromAi vérifié
- `recherche-paternite-section.component.ts` — suppression `as any`, prefillFromAi vérifié
- `reconnaissance-paternelle-section.component.ts` — vérification as any
- `adoption-section.component.ts` — prefillFromAi étendu
- `possession-etat-section.component.ts` — vérification
- `autorite-parentale-section.component.ts` — vérification
- `changement-residence-section.component.ts` — vérification
- `desaccords-parentaux-section.component.ts` — vérification

---

## Plan de test

### Tests unitaires backend

- `CaseAnalysisResponseTest` — SF-246-26 : nominal `filiation_detection_v2` complet
- `CaseAnalysisResponseTest` — SF-246-26 : sous-objet absent → tous null
- `CaseAnalysisResponseTest` — SF-246-26 : qualiteAagir hors whitelist → null
- `CaseAnalysisResponseTest` — SF-246-26 : booléen non parseable → null
- `CaseAnalysisResponseTest` — SF-246-26 : seul `filiation_detection_v2` renseigné → record non null

### Tests Jest frontend

- `contestation-paternite` : computePrefillCount 0 / partiel / nominal
- `recherche-paternite` : computePrefillCount 0 / partiel / nominal
- `adoption` : computePrefillCount avec nouveaux champs booléens

### Isolation workspace

Non applicable.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — modification isolée du parsing LLM + pré-fill frontend.

### Smoke tests E2E concernés

- [x] `e2e/` — smoke complet — ~27 échecs préexistants tolérés, aucune nouvelle régression attendue.

---

## Dépendances

### Subfeatures bloquantes

- `SF-246-09` (statut : done) — record filiation dates/âges
- `SF-246-10` (statut : done) — record autorité parentale
- `SF-246-25` (statut : **doit être mergée avant le dev de SF-246-26**) — record `FamilleExtractedData` partagé

---

## Notes et décisions

**Sous-objet `filiation_detection_v2`** complémentaire à `filiation_detection` (SF-246-09) : même modèle que `succession_detection_v2` complémentaire à `succession_detection`. Les dates et âges restent dans le premier sous-objet ; les qualifications juridiques vont dans le v2.

**Vérification reconnaissance-paternelle** : `consentementLibreDuPereDetected`, `paterniteVraisemblableDetected`, `enfantNonReconnuParAutrePereDetected`, `procedureRespecteeReconnaissanceDetected` — ces 4 champs sont dans le DTO frontend et dans le FamilleExtractedData Java. À vérifier s'ils sont déjà alimentés par un sous-objet backend existant (dans un sprint précédent) ou s'ils sont aspirationnels → si aspirationnels, les intégrer dans `filiation_detection_v2`.

**Suppression des `as any`** : les composants `contestation-paternite` et `recherche-paternite` utilisent `this.qualiteAagir.set(iaQ as any)` car `computeQualiteAagir()` retourne `string | null` alors que le signal attend `QualiteAagir | null`. La solution : retourner le type littéral strict depuis le helper (après `whitelistedOrNull()`) ou caster explicitement avec type guard. Préférer le type guard dans le helper.
