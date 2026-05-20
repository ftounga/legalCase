# Mini-spec — [F-246 / SF-246-23] Lot Travail BE — motif-grave-be, avantages-conventionnels-be, credit-temps-be

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Référence d'audit : `docs/features/F-246/SF-246-14-audit-prefill-exhaustif.md` §4 Domaine Travail BE + §10.
> Référence découpage : `docs/features/F-246/cadrage-decoupage.md` §3 vertical, §5 invariants.
> **Modèle de référence** : SF-246-21 (PR #1134) — sous-objets thématiques + whitelists.

---

## Identifiant

`F-246 / SF-246-23`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-246-23-lot-travail-be`

---

## Objectif

Brancher sur des sources backend réelles les **7 champs extractibles restants**
des 3 outils Travail BE (`motif-grave-be`, `avantages-conventionnels-be`,
`credit-temps-be`) dont la source manque côté record `TravailExtractedData` et
prompt `LegalDomainPromptBuilder`.

---

## Contexte

L'audit exhaustif SF-246-14 §4 révèle 3 outils Travail BE partiellement couverts :

| Outil | Champs pré-remplis | Champs « à brancher » (dette SF-246-23) |
|---|---|---|
| `motif-grave-be` | `dateNotificationRupture` ← `dateLicenciement`, `salaireMensuelReference` ← `salaireBrutMensuel` | `dateConnaissanceFait`, `dateNotificationMotifs` |
| `avantages-conventionnels-be` | `salaireMensuelBrutEur` ← `salaireBrutMensuel` | `commissionParitaireBe`, `joursTravaillesAnneePrecedenteBe`, `joursPrestesBe` |
| `credit-temps-be` | `ageDemandeurAnnees` (SF-246-05), `ancienneteEntrepriseMois` ← `dateEntree` | `dateDemandeCreditTemps` |

Les helpers `*-prefill-rules.ts` existants ont les interfaces correctes mais les
champs sources ne sont pas dans `TravailExtractedData` (backend) ni dans le prompt.
Cette SF branche la chaîne complète : record → prompt → extracteur → DTO → helper → composant.

---

## Regroupement en sous-objet JSON (stratégie groupée)

Les 7 nouveaux champs sont regroupés dans **un unique sous-objet thématique**
`travail_be_detection` dans le prompt et l'extracteur.

| Sous-objet JSON | Outils couverts | Justification |
|---|---|---|
| `travail_be_detection` | `motif-grave-be`, `avantages-conventionnels-be`, `credit-temps-be` | 3 outils BE partageant les mêmes pièces (contrat de travail, lettre de motif grave, fiches de paie, demande de crédit-temps). Concepts BE sans équivalent FR. |

Le sous-objet est émis **BELGIQUE UNIQUEMENT** — reste `null` pour tout dossier
Travail FRANCE. Chaque champ interne est nullable (no-op gracieux si absent).

---

## Champs IA à extraire (pré-remplissage)

### Sous-objet `travail_be_detection`

| Outil | Champ formulaire (composant) | Champ record (nouveau) | Clé JSON prompt | Type Java | Type TS | Nullable | Note |
|---|---|---|---|---|---|---|---|
| `motif-grave-be` | `dateConnaissanceFait` | `dateConnaissanceFait` | `date_connaissance_fait` | `String` | `string` | oui | Date à laquelle l'employeur a eu connaissance du fait constituant le motif grave (ISO YYYY-MM-DD). Point de départ du délai de 3 j ouvrables art. 35 Loi 03/07/1978. Extractible de la lettre de motif ou du dossier de notification. NE PAS confondre avec `dateLicenciement` (date notification rupture). |
| `motif-grave-be` | `dateNotificationMotifs` | `dateNotificationMotifs` | `date_notification_motifs` | `String` | `string` | oui | Date à laquelle l'employeur a notifié les motifs de la rupture au travailleur par lettre recommandée (ISO YYYY-MM-DD). Point d'arrivée du 2e délai de 3 j ouvrables. Extractible de l'avis de dépôt ou du courrier de notification. Strictement postérieure à `dateConnaissanceFait`. NE PAS confondre avec `dateNotificationRupture` (déjà branché ← `dateLicenciement`). |
| `avantages-conventionnels-be` | `commissionParitaire` | `commissionParitaireBe` | `commission_paritaire_be` | `String` | `string` | oui | Numéro ou libellé de la commission paritaire belge applicable (ex. "CP 200", "SCP 200.01", "Employés SCP 218.01"). Extractible du contrat de travail, du C4, ou des fiches de paie. Borne : code normalisé ≤ 20 car. ou null. Concept distinct de `conventionCollective` (domaine FR — convention collective IDCC). |
| `avantages-conventionnels-be` | `joursTravaillesAnneePrecedente` | `joursTravaillesAnneePrecedenteBe` | `jours_travailles_annee_precedente_be` | `Integer` | `number` | oui | Nombre de jours de travail effectif (ou assimilés) au cours de l'année précédente, utilisé pour le calcul du pécule de vacances simple. Borné [0, 365]. Extractible des fiches de paie ou du contrat (base annuelle). |
| `avantages-conventionnels-be` | `joursPrestesEffectifs` | `joursPrestesBe` | `jours_prestes_be` | `Integer` | `number` | oui | Nombre de jours effectivement prestés dans l'entreprise depuis le 1er avril de l'exercice de vacances courant. Borné [0, 365]. Extractible des fiches de paie. Distinct de `joursTravaillesAnneePrecedenteBe`. |
| `credit-temps-be` | `dateDemande` | `dateDemandeCreditTemps` | `date_demande_credit_temps` | `String` | `string` | oui | Date à laquelle le travailleur a formellement introduit sa demande de crédit-temps (ISO YYYY-MM-DD). Extractible de la lettre ou du formulaire de demande déposé auprès de l'employeur. NE PAS confondre avec la date d'entrée en vigueur du crédit-temps. |

> **Nota** — `ancienneteEntrepriseMois` (credit-temps-be) est déjà branché côté
> frontend via `computeAncienneteMois(dateEntree)` dans le helper. Aucun champ
> supplémentaire backend nécessaire pour ce champ — l'extraction de `dateEntree`
> est déjà couverte depuis l'origine.

---

## Comportement attendu

### Cas nominal

Pour un dossier Travail BELGIQUE avec pièces suffisantes :

1. Le pipeline IA extrait le sous-objet `travail_be_detection` depuis la clé JSON
   correspondante dans la réponse LLM.
2. L'extracteur `extractTravailData()` parse le nœud et appelle
   `.dateConnaissanceFait()`, `.dateNotificationMotifs()`,
   `.commissionParitaireBe()`, `.joursTravaillesAnneePrecedenteBe()`,
   `.joursPrestesBe()`, `.dateDemandeCreditTemps()` sur le builder.
3. Le DTO frontend `TravailExtractedData` (dans `case-analysis.model.ts`) expose
   ces 6 nouveaux champs.
4. Les helpers `*-prefill-rules.ts` des 3 composants lisent ces champs et
   calculent `computePrefillCount()` / `computeXxx()` en retournant des valeurs
   réelles (plus de no-op).
5. Les 3 composants `*-section` appellent `prefillFromAi()` au chargement et
   remplissent les signaux + posent `provenanceXxx = 'IA'`.
6. Les badges `auto_awesome` apparaissent en regard des champs pré-remplis.
7. L'alerte F-IA-03 se déclenche si l'avocat saisit une valeur divergeant > seuil
   (délai, dates, jours).
8. Le reset manuel (handler `onXxxChange`) efface la provenance IA.

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| Sous-objet `travail_be_detection` absent (dossier FR ou pièces insuffisantes) | Tous les nouveaux champs restent `null` — no-op gracieux (comportement identique à avant la SF) |
| Date non ISO dans le nœud JSON | `isoDateOrNull()` retourne `null` — champ ignoré |
| Nombre hors borne dans le nœud JSON | `boundedIntOrNull()` retourne `null` — champ ignoré |
| Dossier FRANCE — workspace FR | Les helpers rejettent via `workspaceCountry !== 'BELGIQUE'` — `computeXxx() = null` |
| `joursTravaillesAnneePrecedenteBe` > 365 ou < 0 | Borné → `null` |
| `joursPrestesBe` > 365 ou < 0 | Borné → `null` |

---

## Critères d'acceptation

| # | Critère | Vérifiable par |
|---|---|---|
| CA-1 | Un fixture BE avec `travail_be_detection` rempli : `extractTravailData()` retourne les 6 champs non-null | Test unitaire backend |
| CA-2 | Un fixture FR (sans sous-objet) : `extractTravailData()` retourne null pour tous les 6 champs | Test unitaire backend |
| CA-3 | Date non-ISO dans le fixture → champ null | Test unitaire backend |
| CA-4 | `commissionParitaireBe` > 20 car. → tronqué à 20 | Test unitaire backend |
| CA-5 | `computePrefillCount()` motif-grave-be retourne 4 (2 existants + 2 nouveaux) quand tous les champs sont disponibles | Test Jest helper |
| CA-6 | `computePrefillCount()` avantages-conventionnels-be retourne 4 (1 existant + 3 nouveaux) | Test Jest helper |
| CA-7 | `computePrefillCount()` credit-temps-be retourne 3 (2 existants + 1 nouveau) | Test Jest helper |
| CA-8 | `prefillFromAi()` motif-grave-be : `dateConnaissanceFait` et `dateNotificationMotifs` pré-remplis + provenance nulle avant SF / 'IA' après SF | Test Jest composant |
| CA-9 | `prefillFromAi()` avantages-conventionnels-be : CP + jours travaillés + jours prestés pré-remplis | Test Jest composant |
| CA-10 | `prefillFromAi()` credit-temps-be : dateDemande pré-remplie + provenanceDate 'IA' | Test Jest composant |
| CA-11 | Reset manuel (`onDateConnaissanceChange`) efface la provenance | Test Jest composant |
| CA-12 | Dossier FR (workspaceCountry = 'FRANCE') → aucun champ pré-rempli | Test Jest helper (garde pays) |
| CA-13 | Smoke E2E : pas de régression sur les 3 outils BE existants | `cd e2e && npm test` — ~27 échecs préexistants tolérés |

---

## Plan de test minimal

### Backend (JUnit / Spring)

1. `MotifGraveBeExtractionTest` (nouveau) :
   - fixture `motif_grave_be_dates_be.json` : sous-objet `travail_be_detection` avec les 2 dates → fields non-null
   - fixture identique mais dossier FR (sans sous-objet) → null
   - date non-ISO → null
   - fixture avec 2 dates concurrentes (ex. `dateLicenciement` vs `dateConnaissanceFait`) → bonne discrimination

2. `AvantagesConventionnelsBeExtractionTest` (nouveau) :
   - fixture `avantages_conventionnels_be_extraction.json` : CP, jours travaillés, jours prestés → non-null
   - jours > 365 → null (borne)
   - CP > 20 car. → tronqué

3. `CreditTempsBeExtractionTest` (nouveau) :
   - fixture `credit_temps_be_date_demande.json` → `dateDemandeCreditTemps` non-null
   - date non-ISO → null

### Frontend (Jest)

4. `motif-grave-be-section-prefill-rules.spec.ts` :
   - `computeDateConnaissanceFait` retourne la valeur si BE + champ présent
   - `computeDateNotificationMotifs` idem
   - `computePrefillCount` = 4 quand tous disponibles, = 2 avec données existantes uniquement
   - garde pays : FR → null

5. `avantages-conventionnels-be-section-prefill-rules.spec.ts` :
   - `computeCommissionParitaire` retourne la valeur si BE
   - `computeJoursTravailles` retourne la valeur si BE
   - `computeJoursPrestes` retourne la valeur si BE
   - `computePrefillCount` = 4
   - garde pays : FR → null

6. `credit-temps-be-section-prefill-rules.spec.ts` :
   - `computeDateDemande` retourne la valeur si BE
   - `computePrefillCount` = 3 quand tous disponibles

7. Specs composants (`*.component.spec.ts`) : `prefillFromAi()` + reset provenance.

---

## Périmètre hors-scope

- Recalcul des montants avantages conventionnels (logique métier existante inchangée).
- Logique métier de validation des délais motif grave (service `MotifGraveBeService` inchangé).
- Autres outils BE (`tribunal-travail-fiche`, `prescription-be`, `c4-onem`).
- Tout champ classé « info structurellement absente » dans l'audit (pécule vacances double/prime fin année/éco-chèques prévus — paramètres conventionnels par défaut, non extractibles des pièces).
- Refonte du pipeline IA, des formules de calcul, des endpoints métier.

---

## Tables / endpoints / composants impactés

### Backend

| Fichier | Modification |
|---|---|
| `CaseAnalysisResponse.java` | Ajout de 6 champs dans `TravailExtractedData` record + Builder (tous BELGIQUE uniquement) : `dateConnaissanceFait`, `dateNotificationMotifs`, `commissionParitaireBe`, `joursTravaillesAnneePrecedenteBe`, `joursPrestesBe`, `dateDemandeCreditTemps` |
| `LegalDomainPromptBuilder.java` | Ajout sous-objet `travail_be_detection` (BELGIQUE UNIQUEMENT) avec 6 clés JSON instruitées |
| `CaseAnalysisResponse.java` `extractTravailData()` | Parsing du nœud `travail_be_detection` + appel des 6 builders |
| Tests unitaires backend | 3 nouvelles classes de test + fixtures JSON |

### Frontend

| Fichier | Modification |
|---|---|
| `case-analysis.model.ts` | Ajout de 6 champs dans `TravailExtractedData` interface (commentaire `// BELGIQUE`) |
| `motif-grave-be-section-prefill-rules.ts` | Ajout `computeDateConnaissanceFait`, `computeDateNotificationMotifs` ; mise à jour `computePrefillCount` |
| `avantages-conventionnels-be-section-prefill-rules.ts` | Ajout `computeCommissionParitaire`, `computeJoursTravailles`, `computeJoursPrestes` ; mise à jour `computePrefillCount` |
| `credit-temps-be-section-prefill-rules.ts` | Ajout `computeDateDemande` ; mise à jour `computePrefillCount` |
| `motif-grave-be-section.component.ts` | Extension `prefillFromAi()` + 2 nouveaux signaux provenance + alertes F-IA-03 |
| `avantages-conventionnels-be-section.component.ts` | Extension `prefillFromAi()` + 3 nouveaux signaux provenance + alertes F-IA-03 |
| `credit-temps-be-section.component.ts` | Extension `prefillFromAi()` + 1 signal provenance `dateDemande` |
| Specs (`.spec.ts` des helpers + composants) | Mise à jour fixtures + nouveaux cas de test |

---

## Analyse de cohérence transversale

### Préoccupation transversale : Outil décisionnel métier

- **Déclencheur coché** : modification de 3 outils décisionnels existants.
- **Composants impactés** (listés explicitement) :
  - `motif-grave-be-section` (F-DT-27)
  - `avantages-conventionnels-be-section` (F-DT-28)
  - `credit-temps-be-section` (F-DT-29)
- **Règle invariante** : un outil = une situation métier. Les 3 outils restent distincts — pas de fusion.
- **TOOL_REGISTRY** : aucune entrée nouvelle — les 3 outils sont déjà enregistrés. Vérification pré-commit obligatoire.
- **Self-check grep pré-commit** : vérifier `getPrefillCount` dans les 3 composants statiques et les 3 helpers.
- **Smoke tests E2E** : `cd e2e && npm test` avant push.

### Préoccupation transversale : Workspace context

- Champ country (`workspaceCountry`) utilisé comme garde dans tous les helpers.
- Pas de nouvelle résolution du workspace — guard existant réutilisé.

### Autres préoccupations

- **Auth / Principal** : non applicable (pas de nouveau endpoint).
- **Plans / limites** : non applicable.
- **Navigation / routing** : non applicable (pas de nouvelle route).

---

## Analyse d'impact — smoke tests E2E

- `cd e2e && npm test` avant push.
- Régression attendue : ~27 échecs préexistants tolérés (connus depuis F-245).
- Cible : aucun échec nouveau au-delà des 27 préexistants.

---

## Architecture & dépendances

- Pas de nouvelle table de base de données.
- Pas de nouveau endpoint REST.
- Dépendances bloquantes : aucune (SF-246-05 livré — `ageDemandeurAnnees` et `ancienneteEntrepriseMois` déjà branchés, SF-246-23 complète uniquement les champs restants).
- Pas de question ouverte impactée.
- Pas de migration Liquibase nécessaire (champs purement en mémoire/DTO — pas de persistance en DB).
