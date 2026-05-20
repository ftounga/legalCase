# Mini-spec — F-246 / SF-246-25 — Lot Famille FR régimes & vie commune — champs `*Detected`

## Identifiant

`F-246 / SF-246-25`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-246-25-lot-famille-fr-regimes-vie-commune-detected`

---

## Objectif

Brancher les champs `*Detected` booléens et énumérés aspirationnels sur 8 outils Famille FR (`communaute-universelle`, `partage-judiciaire`, `ordonnance-protection`, `mesures-provisoires`, `revisions-post-divorce`, `pacs-dissolution`, `separation-corps`, `indivision`) en ajoutant les champs manquants au record backend `FamilleExtractedData`, au prompt `FAMILLE_INSTRUCTION` (nouveau sous-objet `communaute_partage_protection_detection_v2`), à l'extracteur, et en corrigeant les casts résiduels dans les composants frontend.

---

## Contexte dette D2/D3

Les helpers `*-prefill-rules.ts` de ces 8 outils lisent des champs `*Detected` présents dans le DTO frontend (`divorce-accepte.model.ts`) mais **absents du record Java `FamilleExtractedData`** et du prompt LLM — `computePrefillCount()` retourne 0 et `prefillFromAi()` est un no-op structurel pour :

- `communaute-universelle` : `contratNotarieDetected`, `enfantsNonCommunsDetected`, `clauseAttributionIntegraleDetected`
- `partage-judiciaire` : `pvDifficultesEtablisDetected`, `tentativeAmiableEpuiseueeDetected`
- `ordonnance-protection` : `violencesAllegueesDetectees[]`, `preuvesViolencesDetectees[]`, `dangerImmediatDetected`, `presenceEnfantsDetected`, `logementCommunDetected`, `victimeFinanciairementDependanteDetected`
- `pacs-dissolution` : `modeDissolutionPacsDetecte`, `regimeBiensPacsDetecte`, `creancesAllegueesDetectees[]`, `patrimoineCommunSignificatifDetecte`
- `separation-corps` : `patrimoineCommun` (boolean) — champ existant en DB mais absent du sous-objet `vie_commune_detection` → ré-exposé via le nouveau sous-objet
- `indivision` : `logementCommunDetected` (partagé avec OP — mutualisé dans le sous-objet)
- `revisions-post-divorce` : cast résiduel `as { nbEnfantsACharge }` à supprimer (`nbEnfantsACharge` est déjà réel SF-246-08)
- `mesures-provisoires` : `violencesAlleguees` boolean — déjà dans FamilleExtractedData via MesuresProvisoiresAiData ; vérification si alimentation backend réelle

---

## Comportement attendu

### Cas nominal

Le pipeline IA extrait le JSON `famille_extracted_data` contenant un sous-objet `communaute_partage_protection_detection_v2`. Le backend parse ce sous-objet et renseigne les nouveaux champs nullable du record `FamilleExtractedData` (via le Builder F-234). L'API renvoie ces champs dans `CaseAnalysisResult`. Côté frontend, `prefillFromAi()` de chaque composant lit ces champs réels (plus d'intersection aspirationnelle) et pré-remplit les booléens/listes/énumérations avec badge `auto_awesome` + signal provenance. Aucun champ ne remplace une valeur saisie manuellement (guard provenance = `'IA'`).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Sous-objet `communaute_partage_protection_detection_v2` absent du JSON | Tous les nouveaux champs = null, no-op gracieux du prefill | N/A |
| Valeur hors whitelist (ex: mode dissolution PACS inconnu) | `whitelistedOrNull()` → null, pas d'erreur | N/A |
| Code violence inconnu dans la liste | Filtré par whitelist VALID_VIOLENCE_CODES, ignoré | N/A |
| Champ booléen non parseable (ex: string "oui") | `booleanOrNull()` → null | N/A |
| Liste vide après filtrage whitelist | null (jamais `[]` — invariant cadrage §5.1.2) | N/A |

---

## Analyse de cohérence transversale

- [x] **Autres outils Famille FR** : les 8 outils cibles de cette SF sont les seuls concernés par ce lot. Les outils successions (SF-246-24) et filiation (SF-246-26) ont leurs propres sous-objets.
- [x] **Belgique** : le nouveau sous-objet ne se remplit que pour les dossiers FR (prompt impose `null` pour BE).
- [x] **Outil décisionnel métier** : aucune création de nouvel outil — modification du pré-fill d'outils existants uniquement.
- [x] **Préoccupations transversales** : pas de nouveau endpoint, pas de changement d'auth/workspace/routing. Pas de migration Liquibase (record non persisté — JSON LLM only).

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `communaute-universelle` prefill booléens | Oui | Intégré dans cette SF |
| `partage-judiciaire` prefill booléens | Oui | Intégré dans cette SF |
| `ordonnance-protection` prefill listes/booléens | Oui | Intégré dans cette SF |
| `pacs-dissolution` prefill énumérations | Oui | Intégré dans cette SF |
| `separation-corps` patrimoineCommun bool | Oui | Intégré dans cette SF |
| `indivision` logementCommunDetected | Oui | Mutualisé avec OP dans cette SF |
| `revisions-post-divorce` cast résiduel | Oui | Nettoyage intégré dans cette SF |
| Outils BE Famille | Non | Non applicable — scope BE = SF-246-28 |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature

---

## Conformité F-IA-04

Non applicable — SF de pré-fill sur outils existants déjà enregistrés dans `TOOL_REGISTRY`. Pas de nouveau composant décisionnel. Les outils sont déjà dans `KNOWN_FRONTEND_TOOL_IDS`.

---

## Champs IA à extraire (pré-remplissage)

Nouveau sous-objet prompt : `communaute_partage_protection_detection_v2`

| Outil | Champ du formulaire | Type | Champ source `FamilleExtractedData` | Source JSON |
|-------|---------------------|------|--------------------------------------|-------------|
| `communaute-universelle` | contratNotarie (bool) | boolean | `contratNotarieDetected` | `communaute_partage_protection_detection_v2.contrat_notarie` |
| `communaute-universelle` | enfantsNonCommuns (bool) | boolean | `enfantsNonCommunsDetected` | `communaute_partage_protection_detection_v2.enfants_non_communs` |
| `communaute-universelle` | clauseAttributionIntegrale (bool) | boolean | `clauseAttributionIntegraleDetected` | `communaute_partage_protection_detection_v2.clause_attribution_integrale` |
| `partage-judiciaire` | pvDifficultes (bool) | boolean | `pvDifficultesEtablisDetected` | `communaute_partage_protection_detection_v2.pv_difficultes_etablis` |
| `partage-judiciaire` | tentativeAmiable (bool) | boolean | `tentativeAmiableEpuiseueeDetected` | `communaute_partage_protection_detection_v2.tentative_amiable_epuisee` |
| `ordonnance-protection` | violencesAllegueesDetectees[] | string[] whitelist | `violencesAllegueesDetectees` | `communaute_partage_protection_detection_v2.violences_alleguees` |
| `ordonnance-protection` | preuvesViolencesDetectees[] | string[] whitelist | `preuvesViolencesDetectees` | `communaute_partage_protection_detection_v2.preuves_violences` |
| `ordonnance-protection` / `indivision` | dangerImmediat (bool) | boolean | `dangerImmediatDetected` | `communaute_partage_protection_detection_v2.danger_immediat` |
| `ordonnance-protection` | presenceEnfants (bool) | boolean | `presenceEnfantsDetected` | `communaute_partage_protection_detection_v2.presence_enfants` |
| `ordonnance-protection` / `indivision` | logementCommun (bool) | boolean | `logementCommunDetected` | `communaute_partage_protection_detection_v2.logement_commun` |
| `ordonnance-protection` | victimeFinanciairementDependante (bool) | boolean | `victimeFinanciairementDependanteDetected` | `communaute_partage_protection_detection_v2.victime_financierement_dependante` |
| `pacs-dissolution` | modeDissolution | string enum | `modeDissolutionPacsDetecte` | `communaute_partage_protection_detection_v2.mode_dissolution_pacs` |
| `pacs-dissolution` | regimeBiens | string enum | `regimeBiensPacsDetecte` | `communaute_partage_protection_detection_v2.regime_biens_pacs` |
| `pacs-dissolution` | creancesAlleguees[] | string[] whitelist | `creancesAllegueesDetectees` | `communaute_partage_protection_detection_v2.creances_alleguees` |
| `pacs-dissolution` | patrimoineCommun (bool) | boolean | `patrimoineCommunSignificatifDetecte` | `communaute_partage_protection_detection_v2.patrimoine_commun_significatif` |
| `separation-corps` | patrimoineCommun (bool) | boolean | `patrimoineCommun` | `communaute_partage_protection_detection_v2.patrimoine_commun_bool` |
| `mesures-provisoires` | violencesAlleguees (bool) | boolean | `violencesAlleguees` (MesuresProvisoiresAiData) | `communaute_partage_protection_detection_v2.violences_alleguees_bool` |

**Whitelists fermées :**
- `violences_alleguees[]` : `PHYSIQUES`, `PSYCHOLOGIQUES`, `SEXUELLES`, `ECONOMIQUES`, `MENACES_MORT`
- `preuves_violences[]` : `CONSTAT_HUISSIER`, `MAIN_COURANTE`, `CERTIFICAT_MEDICAL`, `TEMOIGNAGES`, `PHOTOS`, `PLAINTE_DEPOSEE`, `JUGEMENT_CORRECTIONNEL`, `AUTRE`
- `mode_dissolution_pacs` : `DECLARATION_UNILATERALE`, `DECLARATION_CONJOINTE`, `MARIAGE_PARTENAIRES`, `MARIAGE_TIERS`, `DECES`
- `regime_biens_pacs` : `SEPARATION_BIENS`, `INDIVISION_AMENAGEE`, `INDIVISION_PAR_DEFAUT`
- `creances_alleguees[]` : `CONTRIBUTION_DESEQUILIBRE`, `INVESTISSEMENT_BIEN_PROPRE`, `ENRICHISSEMENT_INJUSTE`, `PRESTATION_TRAVAIL_NON_REMUNEREE`, `AUCUNE`

**Nota :** `violencesAlleguees` (boolean mesures provisoires) et `violencesAllegueesDetectees[]` (liste OP) sont deux champs distincts. Le premier est un flag boolean "y a-t-il des violences allégées ?" ; le second est la liste des types de violences détaillés.

---

## Critères d'acceptation

- [ ] `FamilleExtractedData` (Java record) contient les 17 nouveaux champs nullable via Builder F-234
- [ ] `FAMILLE_INSTRUCTION` contient un sous-objet `communaute_partage_protection_detection_v2` documenté (whitelists, définitions juridiques, garde FR-only)
- [ ] `extractFamilleData()` parse correctement le sous-objet : `booleanOrNull()`, `listOrNull()` avec whitelist, `whitelistedOrNull()` — liste vide → null
- [ ] `FamilleExtractedData` (TypeScript) a les JSDoc mis à jour (source backend `communaute_partage_protection_detection_v2.xxx`)
- [ ] Aucun `as any` résiduel dans les 8 composants du lot
- [ ] Cast aspirationnel `as { nbEnfantsACharge }` supprimé dans `revisions-post-divorce-section.component.ts` (ligne 484)
- [ ] `prefillFromAi()` de chaque composant lit les champs réels (plus d'intersection aspirationnelle)
- [ ] `computePrefillCount()` de chaque helper retourne > 0 pour un input avec champs renseignés
- [ ] Tests backend : nominal, sous-objet absent, hors whitelist, booléen non parseable, liste vide après filtrage
- [ ] Tests Jest frontend : prefillCount = 0 (vide), N champs partiels, N champs nominaux — par composant concerné
- [ ] Smoke E2E : ~27 échecs préexistants tolérés, aucun nouveau

---

## Périmètre

### Dans scope

- 17 nouveaux champs backend (record + prompt + extracteur)
- Mise à jour JSDoc DTO frontend (divorce-accepte.model.ts)
- Suppression du cast aspirationnel `as { nbEnfantsACharge }` dans `revisions-post-divorce`
- Vérification et correction des `prefillFromAi()` des 8 composants
- Tests backend + Jest frontend

### Hors scope

- Champs date/montant des mêmes outils (déjà branchés SF-246-07/08)
- Outils Famille BE (SF-246-28)
- Outils filiation (SF-246-26)
- Nouveau formulaire / endpoint backend décisionnel

---

## Technique

### Tables impactées

Aucune (modification du parsing JSON LLM uniquement — pas de persistance DB).

### Migration Liquibase

Non applicable.

### Composants Angular impactés

- `communaute-universelle-section.component.ts` — prefillFromAi vérifié
- `partage-judiciaire-section.component.ts` — prefillFromAi vérifié
- `ordonnance-protection-section.component.ts` — prefillFromAi vérifié
- `mesures-provisoires-section.component.ts` — prefillFromAi vérifié
- `revisions-post-divorce-section.component.ts` — suppression cast aspirationnel
- `pacs-dissolution-section.component.ts` — prefillFromAi vérifié
- `separation-corps-section.component.ts` — prefillFromAi vérifié
- `indivision-section.component.ts` — prefillFromAi vérifié

---

## Plan de test

### Tests unitaires backend

- `CaseAnalysisResponseTest` — SF-246-25 : nominal `communaute_partage_protection_detection_v2` complet
- `CaseAnalysisResponseTest` — SF-246-25 : sous-objet absent → tous null
- `CaseAnalysisResponseTest` — SF-246-25 : mode dissolution hors whitelist → null
- `CaseAnalysisResponseTest` — SF-246-25 : violence code hors whitelist → null / liste filtrée
- `CaseAnalysisResponseTest` — SF-246-25 : liste vide après filtrage → null (jamais [])
- `CaseAnalysisResponseTest` — SF-246-25 : booléen non parseable → null

### Tests Jest frontend

Par composant (`communaute-universelle`, `partage-judiciaire`, `ordonnance-protection`, `pacs-dissolution`, `separation-corps`, `indivision`) :
- computePrefillCount = 0 (aiData null)
- computePrefillCount = N (champs nominaux)
- computePrefillCount partiel (certains champs null)

### Isolation workspace

Non applicable — modification du parsing uniquement.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — modification isolée du parsing LLM + pré-fill frontend.

### Smoke tests E2E concernés

- [x] `e2e/` — smoke complet — ~27 échecs préexistants tolérés, aucun nouveau régression attendue.

---

## Dépendances

### Subfeatures bloquantes

- `SF-246-07` (statut : done) — record régimes matrimoniaux
- `SF-246-08` (statut : done) — record vie commune

---

## Notes et décisions

**Choix de mutuali sation** : `logementCommunDetected` et `dangerImmediatDetected` sont partagés entre `ordonnance-protection` et `indivision` (lecture du même champ du record). Un seul champ backend suffit.

**Sous-objet `communaute_partage_protection_detection_v2`** plutôt qu'extension du sous-objet `vie_commune_detection` (SF-246-08) : les champs visés sont des *qualifications juridiques booléennes/énumérées* (nature du contrat, type de violence, mode de dissolution PACS…) distinctes des champs *date/montant* de vie commune. Cette séparation suit le modèle SF-246-24 (`succession_detection_v2` complémentaire à `succession_detection`).

**`violencesAlleguees` (bool mesures provisoires)** : déjà dans `MesuresProvisoiresAiData` (interface locale) mais pas alimenté côté backend. On alimente via le nouveau sous-objet et on ajoute le champ au record `FamilleExtractedData` pour uniformiser.

**Cast aspirationnel `revisions-post-divorce`** ligne 484 : `nbEnfantsACharge` est réel depuis SF-246-08. Le cast `as { nbEnfantsACharge?: number | null }` est un vestige — à supprimer ; lire directement `this.aiDataSignal()?.nbEnfantsACharge`.
