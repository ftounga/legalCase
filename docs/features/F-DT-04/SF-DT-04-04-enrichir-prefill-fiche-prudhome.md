# Mini-spec — F-DT-04 / SF-DT-04-04 Enrichir pré-remplissage fiche prud'homale + fondations partagées BE

## Identifiant
`F-DT-04 / SF-DT-04-04`

## Feature parente
`F-DT-04` — Génération fiche prud'homale (FR)

## Statut
`draft`

## Date de création
`2026-04-18`

## Branche Git
`feat/SF-DT-04-04-enrichir-prefill-prudhome`

---

## Objectif

Atteindre un pré-remplissage effectif de ~60-70 % sur `PrudhomeFicheResponse` à l'ouverture d'un dossier FR en droit du travail, au lieu des ~10 % actuels (seule l'indemnité de licenciement est pré-remplie). Enrichir au passage les **fondations partagées FR+BE** : ajout de 8 champs dans le prompt IA `travail_extracted_data` et dans le record `TravailExtractedData` (identité salarié + identité employeur avec SIRET FR ET BCE BE séparés). Ces fondations seront consommées par **SF-DT-06-05** (requête tribunal du travail BE).

---

## Comportement attendu

### Cas nominal

À l'ouverture du composant `PrudhomeFicheSectionComponent` sur un dossier FR avec analyse IA DONE :

- **Demandeur** : `nom`, `prenom`, `adresse`, `profession` pré-remplis depuis `travail_extracted_data` (champs `nom_salarie`, `prenom_salarie`, `adresse_salarie`, `poste`). `telephone` et `email` restent vides (rarement présents dans les pièces).
- **Défendeur** : `nom`, `adresse`, `siret`, `representant` pré-remplis depuis `travail_extracted_data` (champs `nom_employeur`, `adresse_employeur`, `siret_employeur`, `representant_employeur`).
- **Demandes** : outre l'indemnité de licenciement existante, ajouter selon contexte :
  - Indemnité de préavis (si `type_rupture ∈ { LICENCIEMENT, LICENCIEMENT_ECONOMIQUE }` et `salaireBrutMensuel` connu) — calculée pour le FR selon la formule légale (min 1 mois < 6 mois anc., min 2 mois ≥ 2 ans) ou convention
  - Indemnité compensatoire de congés payés (si `congesContractuels` connu, proratisé sur ancienneté)
  - Dommages et intérêts pour licenciement sans cause réelle et sérieuse (1 mois de salaire minimum, valeur indicative)
- **Champs inchangés** : `faitsTexte`, `moyensDroitTexte` restent null (trop contextuels pour un pré-remplissage automatique).

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| Aucune analyse IA DONE sur le dossier | Fiche ouverte avec tous les champs vides (comportement actuel conservé) |
| `travail_extracted_data` absent du JSON IA | Pré-remplissage se limite aux valeurs disponibles sur `compensation_data` (comportement actuel dégradé) |
| Champ individuel manquant (ex. `nom_employeur`) | Le champ cible reste vide, les autres sont pré-remplis normalement |
| Adresse partielle (rue sans ville, etc.) | Concaténer ce qui existe, ne pas bloquer |
| Salaire inconnu (demandes calculées échouent) | Les demandes additionnelles ne sont pas ajoutées, l'existante (indemnité de licenciement) peut aussi manquer |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — F-DT-06 (requête tribunal BE) exactement symétrique. **Non intégré dans cette SF** mais **SF-DT-06-05** consommera directement les 7 nouveaux champs du `TravailExtractedData` enrichi.
- [x] **Autres pays** — Le prompt `travail_extracted_data` est partagé FR+BE. Les 7 nouveaux champs seront peuplés côté BE aussi (identité salarié / employeur / BCE à la place de SIRET via champ dédié).
- [x] **Autres domaines** — Non applicable. `travail_extracted_data` est spécifique au droit du travail.
- [x] **Autres UI patterns** — Pas de nouveau pattern UI côté frontend. Les formulaires `PrudhomeFicheRequest` existent déjà.
- [x] **Autres flows transversaux** — Aucun (pas d'auth, pas de workspace context nouveau).

### Niveaux de vérification couverts

- [x] **Modèle TypeScript / API exposée** — Pas de changement frontend. Le DTO `PrudhomeFicheResponse` est stable (champs déjà exposés, on ne fait que les remplir).
- [x] **Record / DTO backend** — `TravailExtractedData` étendu de 7 champs. Ajout non-breaking : nouveaux champs en fin de constructeur, tests de non-régression sur l'existant.
- [x] **Service / logique métier** — `PrudhomeFicheService.prefillFromAnalysis` enrichie. Pas de changement de contrat d'appel.
- [x] **Entité JPA + schéma DB** — Pas de changement DB. La fiche prud'homale persiste déjà les champs du formulaire (JSON sérialisé via `ObjectMapper`).
- [x] **Tests existants** — `PrudhomeFicheServiceTest` et `PrudhomeFicheControllerIT` à étendre. Tests actuels du pré-remplissage partiel (indemnité de licenciement) restent verts.

### Cas spécifique : nouveau pattern UI ou service partagé

Cette SF introduit **7 nouveaux champs partagés** dans le prompt IA `travail_extracted_data` qui seront consommés par d'autres outils métier. Scan effectué :

- [x] **Où les nouveaux champs pourront-ils être réutilisés ?**
  - **SF-DT-06-05** (requête tribunal BE) : consommation directe
  - F-DT-07 Ancienneté : `nom_employeur` pourrait alimenter un bandeau info (hors scope)
  - F-DT-09 Comparateur indemnités : pas de besoin immédiat
  - F-95 Export Word : `nom_salarie` / `nom_employeur` pourraient enrichir l'en-tête (hors scope)
- [x] **Patterns concurrents ?** Aucun — les identités salarié/employeur n'étaient extraites nulle part dans le projet.
- [x] **Le service enrichi (`prefillFromAnalysis`) peut-il servir à d'autres features ?** Non — il est spécifique à la fiche prud'homale FR. Le service jumeau BE aura sa propre implémentation.
- [x] **Équivalent design existant ?** Non — premier pré-remplissage identité côté fiches administratives.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|---|---|---|
| F-DT-04 Fiche prud'homale FR | Oui | Intégré dans cette SF |
| F-DT-06 Requête tribunal BE | Oui | Fondations posées ici, consommation dans SF-DT-06-05 (SF parallèle prévue) |
| F-DT-07 / F-DT-09 (bandeau info identités) | Oui, faible valeur | Backlog — à reprendre si retour terrain demande |
| F-95 Export Word (en-tête avec identités) | Oui, faible valeur | Backlog |

### Décision

- [x] Étendu aux cibles directement applicables (F-DT-04 FR + fondations pour F-DT-06 BE)
- [x] SF parallèle prévue : **SF-DT-06-05** pour BE
- [x] Backlog pour les usages secondaires (bandeau info, en-tête export) — non prioritaires

---

## Critères d'acceptation

- [ ] `TravailExtractedData` contient 8 nouveaux champs : `nomSalarie`, `prenomSalarie`, `adresseSalarie`, `nomEmployeur`, `adresseEmployeur`, `siretEmployeur`, `bceEmployeur`, `representantEmployeur`
- [ ] Le prompt `LegalDomainPromptBuilder` (TRAVAIL) et `EnrichedAnalysisService` demandent explicitement ces 7 champs avec leurs règles d'extraction
- [ ] `extractTravailData()` dans `CaseAnalysisResponse` extrait les 7 champs (texte ou null)
- [ ] Sur un dossier FR avec analyse DONE : ouvrir `PrudhomeFicheResponse` → `demandeur` a ≥ 3 champs pré-remplis si les documents contiennent l'identité du salarié
- [ ] Sur ce même dossier : `defendeur` a ≥ 2 champs pré-remplis (au minimum `nom` + `adresse` / `siret` si détectable)
- [ ] Les demandes sont étendues : si `LICENCIEMENT` / `LICENCIEMENT_ECONOMIQUE` + salaire connu, ajouter au moins "Indemnité compensatoire de préavis" (montant calculé) et "Dommages et intérêts pour licenciement sans cause réelle et sérieuse" (1 mois de salaire, valeur indicative)
- [ ] Aucune régression sur les tests existants de `PrudhomeFicheService` (pré-remplissage indemnité de licenciement toujours OK)
- [ ] Tests ajoutés : pré-remplissage complet avec tous les champs IA disponibles, pré-remplissage dégradé avec identités manquantes, non-ajout de demandes si salaire inconnu
- [ ] Tests existants sur `CaseAnalysisResponse.extractTravailData` restent verts (rétrocompat : nouveaux champs tolérés absents)
- [ ] Aucun changement frontend — les formulaires remplis par ces champs existent déjà

---

## Périmètre

### Hors scope (explicite)

- Consommation côté BE (`TribunalTravailFicheService`) — **SF-DT-06-05** à suivre
- Changement du schéma du formulaire `PrudhomeFicheRequest` — les champs cible existent déjà
- Pré-remplissage de `faitsTexte` et `moyensDroitTexte` — trop contextuels, besoin d'un module IA dédié (hors scope, éventuellement F-98 si relancée)
- Migration DB — aucune (les champs persistés sont en JSON dans `prudhome_fiches.demandeur_json` etc.)
- Ajout de numéros de téléphone / emails dans l'extraction IA — rarement présents dans les pièces, ROI faible
- Extraction d'infos tribunal (division, localité) — côté FR, le tribunal est toujours le Conseil de prud'hommes, pas besoin d'extraction. Côté BE (SF-DT-06-05) sera à étudier

---

## Valeurs initiales

Pas de nouvelle entité. Les 7 champs enrichissent un record DTO existant.

### Comportements sur `TravailExtractedData` enrichi

| Champ | Valeur si non extrait par IA | Règle |
|---|---|---|
| `nomSalarie` | null | trim si extrait |
| `prenomSalarie` | null | trim si extrait |
| `adresseSalarie` | null | concaténation rue + CP + ville si composé, null si aucune partie |
| `nomEmployeur` | null | raison sociale prioritaire (ex. "FinConsult SPRL"), nom commercial en fallback |
| `adresseEmployeur` | null | siège social uniquement (pas les succursales) |
| `siretEmployeur` | null | SIREN 9 chiffres ou SIRET 14 chiffres (FR). Null sur un dossier BE. |
| `bceEmployeur` | null | Numéro BCE 10 chiffres (BE). Null sur un dossier FR. |
| `representantEmployeur` | null | le signataire d'une lettre RH ou l'administrateur délégué mentionné |

> **Décision** : **2 champs séparés** `siretEmployeur` (FR) + `bceEmployeur` (BE) — formats différents, pas d'ambiguïté métier, permet au prompt IA de distinguer proprement. Le frontend `PrudhomeFiche` (FR) lit `siretEmployeur`, `TribunalTravailFiche` (BE, SF-DT-06-05) lira `bceEmployeur`.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format | Unicité | Normalisation |
|---|---|---|---|---|---|
| `nomSalarie` | Non | 200 | texte | Non | trim |
| `prenomSalarie` | Non | 100 | texte | Non | trim |
| `adresseSalarie` | Non | 500 | texte libre | Non | trim + collapse espaces |
| `nomEmployeur` | Non | 300 | texte (raison sociale) | Non | trim |
| `adresseEmployeur` | Non | 500 | texte libre | Non | trim + collapse |
| `siretEmployeur` | Non | 14 | chiffres uniquement, 9 ou 14 car (SIREN ou SIRET) | Non | retirer espaces/points |
| `bceEmployeur` | Non | 10 | chiffres uniquement, 10 car (format `BE 0XXX.XXX.XXX` normalisé en 10 chiffres) | Non | retirer espaces/points/préfixe `BE` |
| `representantEmployeur` | Non | 200 | texte | Non | trim |

**Règles de sérialisation JSON IA attendues** :

- L'IA peut renvoyer `null` sur n'importe lequel si indéterminable dans les pièces
- L'IA ne doit **jamais inventer** un SIRET ou une adresse — si incertaine, null
- Pour les lettres contenant une adresse d'envoi (lettre de licenciement, attestation), prioriser l'adresse en en-tête pour `adresseEmployeur` et l'adresse destinataire pour `adresseSalarie`

---

## Technique

### Endpoint(s)

Aucun nouvel endpoint. `GET /api/v1/case-files/{id}/prudhome-fiche` (existant) renvoie désormais un payload plus riche.

### Tables impactées

Aucune.

### Migration Liquibase

Aucune.

### Composants / classes backend impactés

| Classe | Modification |
|---|---|
| `CaseAnalysisResponse.java` | Étendre `TravailExtractedData` avec 7 nouveaux champs. Modifier `extractTravailData()` pour les parser. |
| `LegalDomainPromptBuilder.java` | Étendre la section `travail_extracted_data` du prompt avec les 7 champs et leurs règles. |
| `EnrichedAnalysisService.java` | Idem dans la signature JSON attendue. |
| `PrudhomeFicheService.java` | Enrichir `prefillFromAnalysis` : peupler `demandeur` / `defendeur` / ajouter demandes conditionnelles. |

### Composants frontend

Aucun. Les champs cible sont déjà dans `PrudhomeFicheRequest` et affichés par le template.

---

## Plan de test

### Tests unitaires

- [ ] `CaseAnalysisResponseTest` : `extractTravailData_withNewIdentityFields_parsesAllSeven`
- [ ] `CaseAnalysisResponseTest` : `extractTravailData_missingNewFields_returnsNullTolerantly` (rétrocompat analyses anciennes)
- [ ] `PrudhomeFicheServiceTest` : `prefill_withFullIdentity_populatesDemandeurDefendeur` (7 champs IA → demandeur/défendeur complets)
- [ ] `PrudhomeFicheServiceTest` : `prefill_withPartialIdentity_populatesOnlyAvailable` (ex. nomSalarie présent, reste null)
- [ ] `PrudhomeFicheServiceTest` : `prefill_withLicenciementAndSalary_addsPrefixeAndDI` (demandes additionnelles)
- [ ] `PrudhomeFicheServiceTest` : `prefill_withoutSalary_noAdditionalDemandes` (demandes ne s'ajoutent pas si salaire null)
- [ ] `PrudhomeFicheServiceTest` : `prefill_existingFiche_keepsUserEdits` (si une fiche est déjà persistée, `get` retourne cette version — pas de re-prefill, comportement actuel)
- [ ] `PrudhomeFicheServiceTest` : rétrocompat — pré-remplissage existant indemnité de licenciement toujours OK

### Tests d'intégration

- [ ] `PrudhomeFicheControllerIT` : `GET /prudhome-fiche` sur dossier analysé FR avec documents → demandeur/défendeur/demandes cohérents avec l'analyse persistée

### Isolation workspace

- [x] **Non applicable** — isolation gérée par `PrudhomeFicheService.resolveCaseFile` (inchangé). Le pré-remplissage lit l'analyse du dossier, pas de fuite cross-workspace possible.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non touché
- [ ] Workspace context — non touché
- [ ] Plans / limites — non touché
- [ ] Navigation / routing frontend — non touché
- [x] **Aucune préoccupation transversale** — subfeature isolée au module casefile/analysis

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|---|---|---|
| `CaseAnalysisResponse.extractTravailData()` | Ajout de champs au record. Analyses anciennes n'ont pas ces champs dans le JSON → doivent tomber à null sans erreur | Test rétrocompat explicite |
| Prompt IA `travail_extracted_data` | 7 nouveaux champs demandés. Le prompt devient plus long (coût léger) mais la structure reste compatible | Tests existants sur le parsing JSON IA |
| `PrudhomeFicheService.prefillFromAnalysis` | Extension — les tests actuels qui vérifient l'indemnité de licenciement doivent passer | Tests existants |
| `EnrichedAnalysisService` prompt | Signature JSON étendue | Tests existants |

### Smoke tests E2E concernés

Aucun smoke test n'exerce F-DT-04 actuellement (`e2e/smoke/` couvre auth / workspace / navigation uniquement). Pas de risque de régression côté E2E.

---

## Dépendances

### Subfeatures bloquantes

- `SF-DT-04-01` — **done** (backend CRUD fiche prud'homale)
- `SF-DT-04-02` — **done** (frontend formulaire)
- `SF-DT-04-03` — **done** (export PDF)

### Subfeatures prévues consommatrices

- `SF-DT-06-05` — à démarrer après merge de SF-DT-04-04 : consommation des 7 nouveaux champs dans `TribunalTravailFicheService.prefillFromAnalysis` + demandes multiples CCT 109

### Questions ouvertes impactées

- [x] **Tranchée 2026-04-18** : 2 champs séparés `siretEmployeur` + `bceEmployeur`.

---

## Notes et décisions

### Pourquoi pas de migration

Les fiches prud'homales persistent leurs champs en **JSON string** dans les colonnes `demandeur_json`, `defendeur_json`, `demandes_json`, etc. (voir `PrudhomeFiche` entity). L'ajout de champs dans les records est un ajout JSON non-breaking — les anciens JSON sans ces clés seront lus avec `null` par Jackson (comportement `unknown property = null` grâce au record). Aucune migration nécessaire.

### Pourquoi pas de pré-remplissage pour `faitsTexte` / `moyensDroitTexte`

Ces champs sont :
- **Contextuels** : l'avocat rédige un récit structuré propre au dossier, pas une liste de faits bruts
- **Sensibles** : une formulation incorrecte en pré-remplissage pourrait tromper l'avocat et se retrouver dans le document final
- **Risque IA** : génération de texte libre à partir de documents = risque d'hallucination

Un pré-remplissage pertinent nécessiterait un module dédié (type F-98 Génération courrier, actuellement en stand-by). Hors scope.

### Ordre d'implémentation

1. Étendre `TravailExtractedData` (record Java)
2. Étendre `extractTravailData()` (parsing JSON)
3. Étendre les 2 prompts IA (`LegalDomainPromptBuilder`, `EnrichedAnalysisService`)
4. Étendre `PrudhomeFicheService.prefillFromAnalysis`
5. Écrire les 8 tests unitaires
6. Vérifier suite complète backend (891+ tests verts)
7. Review + PR

### Validation IA Haiku

Non applicable pour ces champs — la validation IA Haiku (SF-110-10) concerne uniquement les modifications des référentiels (`legal_referentials`), pas les extractions de l'analyse principale.
