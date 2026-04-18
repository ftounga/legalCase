# Mini-spec — F-DT-06 / SF-DT-06-05 Enrichir pré-remplissage requête tribunal du travail belge

## Identifiant
`F-DT-06 / SF-DT-06-05`

## Feature parente
`F-DT-06` — Requête contradictoire tribunal du travail belge

## Statut
`draft`

## Date de création
`2026-04-18`

## Branche Git
`feat/SF-DT-06-05-enrichir-prefill-tribunal-be`

---

## Objectif

Pendant de SF-DT-04-04 (FR). Faire passer le pré-remplissage de la requête tribunal du travail belge de ~10 % (uniquement indemnité compensatoire de préavis) à ~60-70 % en consommant les 8 nouveaux champs du `TravailExtractedData` (identité salarié + identité employeur + BCE) ajoutés en SF-DT-04-04. Ajouter une demande CCT 109 "licenciement manifestement déraisonnable" (valeur indicative à mi-fourchette) quand le type de rupture est LICENCIEMENT.

---

## Comportement attendu

### Cas nominal

À l'ouverture du composant `TribunalTravailFicheSectionComponent` sur un dossier BE en droit du travail avec analyse IA DONE :

- **Requérant** : `nom`, `prenom`, `domicile` pré-remplis depuis `travail_extracted_data` (`nom_salarie`, `prenom_salarie`, `adresse_salarie`). `registreNational` reste vide (rarement présent dans les pièces).
- **Défendeur** : `nom`, `siegeSocial`, `numeroBce`, `representant` pré-remplis depuis `travail_extracted_data` (`nom_employeur`, `adresse_employeur`, `bce_employeur`, `representant_employeur`). Champ `bceEmployeur` (BE) utilisé, pas `siretEmployeur` (FR).
- **ContratInfo** : `dateDebut` (= `dateEntree`), `dateFin` (= `dateLicenciement`) pré-remplis si présents. `typeContrat` et `motifRupture` restent vides (pas assez fiables pour un pré-remplissage automatique — l'avocat choisit).
- **ProcedureInfo** : inchangé (`langue` défaut "FR", `tribunal` / `division` / `commissionParitaire` vides — choix avocat).
- **Demandes** : outre l'indemnité compensatoire de préavis existante, ajouter :
  - **Indemnité pour licenciement manifestement déraisonnable (CCT 109)** — valeur indicative à **10 semaines de salaire hebdomadaire** (milieu de la fourchette 3-17), uniquement si `type_rupture ∈ { LICENCIEMENT, LICENCIEMENT_ECONOMIQUE }` et salaire connu. Label inclut la mention "(valeur indicative, fourchette 3-17 semaines)".
- **ExposeDesMoyens** : inchangé (reste null — trop contextuel pour pré-remplissage auto).

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| Aucune analyse IA DONE sur le dossier | Fiche ouverte avec tous les champs vides (comportement actuel) |
| `travail_extracted_data` absent du JSON IA | Pré-remplissage limité à l'indemnité de préavis existante (rétrocompat) |
| Champ individuel manquant (ex. `adresse_employeur`) | Le champ cible reste vide, les autres sont pré-remplis normalement |
| `type_rupture` autre que LICENCIEMENT/LICENCIEMENT_ECONOMIQUE | Pas de demande CCT 109 ajoutée |
| Salaire inconnu → `donneesPartielles = true` | Pas de demande CCT 109 (valeur non calculable) |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — F-DT-04 (prud'hommes FR) symétrique, déjà fait en SF-DT-04-04. Cette SF consomme directement les fondations posées.
- [x] **Autres pays** — Aucun. Le droit du travail V1 cible FR + BE. La symétrie est complète après cette SF.
- [x] **Autres domaines** — Non applicable. `travail_extracted_data` est spécifique au droit du travail.
- [x] **Autres UI patterns** — Pas de nouveau pattern UI. Le formulaire `TribunalTravailFicheRequest` existe déjà (SF-DT-06-02), ses champs cibles sont inchangés.
- [x] **Autres flows transversaux** — Aucun (pas d'auth, pas de workspace context nouveau, pas de plan/quota).

### Classification

| Cible | Applicable ? | Traitement |
|---|---|---|
| F-DT-04 Fiche prud'homale FR | Déjà traité en SF-DT-04-04 | Rien à faire |
| F-DT-06 Requête tribunal BE | Oui | **Intégré dans cette SF** |
| F-DT-07 / F-DT-09 (usages secondaires des identités) | Oui, faible valeur | Backlog |
| F-95 Export Word (en-tête avec identités) | Oui, faible valeur | Backlog |

### Nouveau pattern UI ou service partagé

- [x] **Aucun nouveau pattern partagé.** Cette SF réplique le pattern accumulator introduit dans `PrudhomeFicheService.prefillFromAnalysis` (SF-DT-04-04). Pas de service transversal à extraire : le code d'accumulation est spécifique à chaque record de requête (PrudhomeFicheRequest vs TribunalTravailFicheRequest), et les champs cibles diffèrent (profession vs registreNational, siret vs bce, etc.). Factoriser prématurément créerait une abstraction fragile.

---

## Critères d'acceptation

- [ ] `TribunalTravailFicheService.prefillFromAnalysis` peuple `Requerant.nom/prenom/domicile` depuis `travail_extracted_data`
- [ ] `Defendeur.nom/siegeSocial/numeroBce/representant` peuplés depuis `travail_extracted_data`
- [ ] `ContratInfo.dateDebut/dateFin` peuplés depuis `dateEntree/dateLicenciement`
- [ ] Demande "Indemnité compensatoire de préavis (X semaines)" conservée (rétrocompat)
- [ ] Demande "Indemnité pour licenciement manifestement déraisonnable (CCT 109)" ajoutée uniquement si LICENCIEMENT/LICENCIEMENT_ECONOMIQUE + salaire > 0 + `donneesPartielles = false`
- [ ] Fiche déjà persistée → pas de re-prefill (comportement existant)
- [ ] Isolation workspace inchangée (validations existantes conservées)
- [ ] Tests unitaires nouveaux (`TribunalTravailFicheServiceTest`) couvrent les cas principaux
- [ ] `TribunalTravailFicheControllerIT` existant reste vert
- [ ] Build backend `mvnw clean verify` vert

---

## Périmètre

### Hors scope (explicite)

- Changement du schéma `TribunalTravailFicheRequest` (les champs cible existent déjà)
- Modification du prompt IA `travail_extracted_data` (déjà fait en SF-DT-04-04)
- Pré-remplissage de `exposeDesMoyens` (trop contextuel, hors scope)
- Demandes "arriérés de salaire / pécule de vacances / chèques repas" — non extraites par l'IA actuellement
- Détection automatique EMPLOYE/OUVRIER — rarement fiable sans ambiguïté, l'avocat choisit
- Extraction tribunal/division/commission paritaire — l'IA n'extrait pas ces infos aujourd'hui
- Migration DB — aucune (persistance JSON existante)

---

## Technique

### Composants impactés

| Fichier | Opération |
|---|---|
| `backend/src/main/java/fr/ailegalcase/casefile/TribunalTravailFicheService.java` | Modifier `prefillFromAnalysis` + introduire accumulator |
| `backend/src/test/java/fr/ailegalcase/casefile/TribunalTravailFicheServiceTest.java` | **NOUVEAU** — tests unitaires |

### Endpoints / Tables

Aucun changement (réutilise endpoints existants SF-DT-06-01).

### Migration Liquibase

- [ ] Non

---

## Plan de test

### Tests unitaires (TribunalTravailFicheServiceTest)

- [ ] U-DT06-05-01 — identité complète (salarié + employeur) → requerant/defendeur pré-remplis
- [ ] U-DT06-05-02 — identité partielle → seuls les champs extraits sont pré-remplis
- [ ] U-DT06-05-03 — `dateEntree`/`dateLicenciement` → `contratInfo.dateDebut/dateFin`
- [ ] U-DT06-05-04 — LICENCIEMENT + salaire > 0 → préavis + CCT 109 ajoutés
- [ ] U-DT06-05-05 — DEMISSION → pas de CCT 109
- [ ] U-DT06-05-06 — salaire = 0 / null → pas de CCT 109
- [ ] U-DT06-05-07 — fiche déjà persistée → pas de re-prefill
- [ ] U-DT06-05-08 — rétrocompat : analyse sans `travail_extracted_data` → préavis uniquement

### Tests d'intégration existants

- [ ] `TribunalTravailFicheControllerIT` reste vert (4 tests)

### Isolation workspace

- [x] Applicable (inchangé, déjà testé par SF-DT-06-01)

---

## Analyse d'impact

- [x] **Aucune préoccupation transversale.** Modification interne à un service existant. Pas de nouveau type d'auth, pas de workspace context modifié, pas de plan/quota, pas de routing.
