# Mini-spec — F-DT-09 / SF-DT-09-05 Fiabiliser l'extraction du type de rupture

## Identifiant

`F-DT-09 / SF-DT-09-05`

## Feature parente

`F-DT-09` — Comparateur d'indemnités

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-DT-09-05-fiabiliser-extraction-type-rupture`

---

## Objectif

Garantir que `compensation_data.type_rupture` est toujours peuplé quand le dossier concerne une rupture du contrat de travail, afin que le pré-remplissage et les alertes de cohérence F-IA-03 de F-DT-09 fonctionnent de manière systématique. Observé en staging sur le dossier Dupont : l'IA a clairement détecté un licenciement (qualification, faits, F-96) mais `compensation_data` était absent → `compensationEstimate` null côté front → aucune alerte ne se déclenche, le formulaire tombe sur la valeur par défaut muette.

---

## Comportement attendu

### Cas nominal

1. L'IA reçoit un dossier droit du travail contenant une rupture du contrat.
2. Le prompt `TRAVAIL_INSTRUCTION` impose l'émission de l'objet `compensation_data` avec `type_rupture` obligatoire dès que le dossier parle d'une rupture (licenciement, rupture conventionnelle, démission, prise d'acte, rupture amiable BE, etc.).
3. Si l'IA n'a pas posé `type_rupture` mais que d'autres signaux fiables sont présents (`qualification_juridique`, `licenciement_validity_detection.critere_code`, pièces typées entretien préalable / lettre de licenciement), un fallback Java le dérive.
4. Le champ est normalisé upper-case et filtré sur l'enum connu avant sérialisation ; toute valeur hors enum est remplacée par `null`.
5. Le pré-remplissage F-DT-09 et les alertes de cohérence F-IA-03 (SF-IA-03-05) fonctionnent sur tout dossier où un licenciement est détecté.

### Règles d'extraction renforcées dans le prompt

Ajouter dans `TRAVAIL_INSTRUCTION` :

- `compensation_data.type_rupture` devient **obligatoire** dès que le dossier parle d'une rupture de contrat, même si l'ancienneté ou le salaire sont inconnus.
- Énumération explicite et exclusive des valeurs autorisées :
  - FR : `LICENCIEMENT` (cause réelle et sérieuse, sans faute grave), `LICENCIEMENT_ECONOMIQUE`, `RUPTURE_CONVENTIONNELLE`, `DEMISSION`, `PRISE_ACTE`, `RESILIATION_JUDICIAIRE`.
  - BE : `LICENCIEMENT_ORDINAIRE`, `LICENCIEMENT_MANIFESTEMENT_DERAISONNABLE`, `RUPTURE_AMIABLE`, `DEMISSION`.
- Règle de choix prioritaire quand plusieurs modes semblent possibles : toujours privilégier le type documenté par les pièces (lettre de licenciement > convention de rupture > décision judiciaire > allégation de l'avocat).
- Exemple few-shot positif ajouté : "Lettre de licenciement pour cause réelle et sérieuse citant motif X → `type_rupture = LICENCIEMENT`".
- Exemple few-shot négatif : "Dossier sans pièce de rupture, seule plainte sur harcèlement → `compensation_data = null` (pas d'objet partiel)".

### Fallback Java

Créer `TypeRuptureFallback.derive(CaseAnalysisResult base)` appelé dans `CaseAnalysisResponse.extractCompensationEstimate` et dans `EnrichedAnalysisService`, dans cet ordre :

1. Si `compensation_data.type_rupture` présent et dans l'enum → garder.
2. Sinon, si `licenciement_validity_detection` présent (un licenciement est détecté) → `LICENCIEMENT` (FR) ou `LICENCIEMENT_ORDINAIRE` (BE), selon pays workspace.
3. Sinon, si `qualification_juridique` contient un code rattaché à un type de rupture (ex. `DT07_LICENCIEMENT_SANS_CAUSE` → `LICENCIEMENT`), appliquer la table de mapping.
4. Sinon, laisser `null` — le comportement précédent est conservé, l'outil ne prétend rien.

### Compensation partielle — changement de comportement

Actuellement `CompensationCalculator.calculate()` renvoie `Optional.empty()` si `type_rupture` est `null`. Nouveau contrat :

- Si `type_rupture` est dans l'enum **supporté par le calculateur** (`LICENCIEMENT`, `LICENCIEMENT_ECONOMIQUE`, `LICENCIEMENT_ORDINAIRE`) → calcul complet comme aujourd'hui.
- Si `type_rupture` est dans l'enum **étendu** mais non calculable (`RUPTURE_CONVENTIONNELLE`, `DEMISSION`, `PRISE_ACTE`, `RESILIATION_JUDICIAIRE`, `RUPTURE_AMIABLE`) → renvoyer un `CompensationEstimate` avec `typeRupture` peuplé, `ancienneteAnnees`/`salaireReference` peuplés s'ils sont connus, mais `plafondMin`/`plafondMax` à 0 et `donneesPartielles = true`. Le frontend exploitera `typeRupture` pour le pré-remplissage et les alertes F-DT-09 ; le calcul n'est pas affiché sur la synthèse IA pour ces types.
- Si `type_rupture` null → `Optional.empty()` comme avant.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| L'IA renvoie `type_rupture` avec valeur inconnue | filtrage enum → traité comme null, fallback tenté |
| L'IA renvoie `compensation_data = null` alors que licenciement détecté ailleurs | fallback dérive `type_rupture`, reconstruit un estimate partiel |
| Aucun signal de rupture nulle part | `compensationEstimate = null`, comportement inchangé |
| Dossier multi-rupture (historique + nouvelle) | prendre la plus récente — règle explicitée dans le prompt |

---

## Critères d'acceptation

- [ ] `TRAVAIL_INSTRUCTION` mis à jour : enum exhaustif, obligation de `type_rupture` si rupture détectée, few-shot positif/négatif.
- [ ] `TypeRuptureFallback` Java avec mapping qualification → type_rupture + fallback `LICENCIEMENT`/`LICENCIEMENT_ORDINAIRE` basé sur `licenciement_validity_detection` et pays.
- [ ] `CompensationCalculator.calculate()` renvoie un `CompensationEstimate` partiel (sans chiffres) pour les types non calculables de l'enum étendu.
- [ ] `CaseAnalysisResponse.extractCompensationEstimate` consomme le fallback avant d'abandonner.
- [ ] Le pré-remplissage F-DT-09 et les alertes F-IA-03 (TYPE_RUPTURE) se déclenchent sur le dossier Dupont de staging.
- [ ] Tests backend : fallback licenciement FR/BE, enum étendu retourné avec données partielles, aucun fallback quand pas de signal, filtrage des valeurs hors enum.
- [ ] Test de non-régression : un dossier où l'IA peuple déjà `compensation_data` complet n'est pas modifié.
- [ ] Aucune régression sur les autres consommateurs de `compensationEstimate` (calcul Macron, export, synthèse).

---

## Périmètre

### Hors scope (explicite)

- Ajout d'un nouveau type de rupture non listé dans l'enum FR/BE (aucun besoin métier identifié).
- Calcul d'indemnités pour les types non-Macron (couvert par SF-DT-09-04 côté outil, pas côté estimate IA).
- Modification du frontend — le composant `IndemniteComparatifSectionComponent` est déjà prêt à consommer `compensationEstimate.typeRupture` non null.
- Refonte du prompt IA au-delà de la section compensation_data.
- Test E2E staging — validation manuelle suffit dans le cadre du test plan en cours.

---

## Valeurs initiales

Pas de nouvelles colonnes. Pas de migration.

---

## Contraintes de validation

| Champ | Obligatoire | Valeurs autorisées | Normalisation |
|-------|-------------|-------------------|--------------|
| `compensation_data.type_rupture` | Oui si rupture détectée | cf. enum FR/BE ci-dessus | upper-case, filtrage enum, null si hors enum |

---

## Technique

### Endpoints

Aucun endpoint modifié. Le payload `CaseAnalysisResponse` expose potentiellement plus de `compensationEstimate` non-null avec `donneesPartielles=true`.

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

### Composants backend

- `LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION` : enrichi.
- `TypeRuptureFallback` (nouveau) : logique de dérivation.
- `CompensationCalculator` : tolère l'enum étendu, renvoie partiel au lieu de vide.
- `CaseAnalysisResponse.extractCompensationEstimate` : applique le fallback avant de laisser null.

### Composants frontend

Aucune modification. `IndemniteComparatifSectionComponent` consomme déjà `compensationEstimate.typeRupture`.

---

## Plan de test

### Tests unitaires backend

- [ ] `TypeRuptureFallbackTest` : licenciement_validity_detection présent + pays FR → `LICENCIEMENT`.
- [ ] `TypeRuptureFallbackTest` : licenciement_validity_detection présent + pays BE → `LICENCIEMENT_ORDINAIRE`.
- [ ] `TypeRuptureFallbackTest` : qualification mapping `DT07_LICENCIEMENT_SANS_CAUSE` → `LICENCIEMENT`.
- [ ] `TypeRuptureFallbackTest` : aucun signal → null.
- [ ] `TypeRuptureFallbackTest` : type_rupture hors enum → null (fallback tenté).
- [ ] `CompensationCalculatorTest` : `RUPTURE_CONVENTIONNELLE` → estimate partiel, chiffres à 0, `donneesPartielles=true`.
- [ ] `CompensationCalculatorTest` : `LICENCIEMENT` → calcul complet (non-régression).
- [ ] `CompensationCalculatorTest` : type hors enum → empty (non-régression).
- [ ] `CaseAnalysisResponseTest` : JSON sans `compensation_data` + `licenciement_validity_detection` présent → estimate reconstruit avec `typeRupture=LICENCIEMENT`.
- [ ] `CaseAnalysisResponseTest` : JSON avec `compensation_data.type_rupture` valide → estimate complet inchangé.

### Tests d'intégration

- [ ] Pipeline E2E sur fixture Dupont (licenciement FR) → `compensationEstimate.typeRupture == "LICENCIEMENT"`.
- [ ] Pipeline E2E sur fixture Belgique (licenciement ordinaire) → `typeRupture == "LICENCIEMENT_ORDINAIRE"`.

### Tests frontend

Aucun nouveau test frontend requis. Les tests existants de F-DT-09 et F-IA-03 couvrent le comportement en aval.

### Isolation workspace

- [x] Non impactée — logique d'extraction, aucun accès base.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune** — logique d'extraction IA locale.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|-----------|--------|-----------------------|
| `CompensationCalculator` | Nouveau comportement partiel pour enum étendu | Test Macron inchangé |
| `CaseAnalysisResponse` | Plus d'estimates non-null grâce au fallback | Fixtures existantes relues |
| Synthèse IA affichée côté frontend | Peut afficher `typeRupture` là où c'était null avant | Vérif visuelle sur 2 dossiers — ne doit pas afficher de chiffre bidon |
| `EnrichedAnalysisService` | Injection fallback dans la chaîne d'enrichissement | Test enrichissement existant |

### Smoke tests E2E concernés

- [ ] Aucun smoke test critique — le pipeline IA n'est pas dans les chemins E2E.

---

## Dépendances

### Subfeatures bloquantes

- `SF-DT-09-04` (Done) — l'enum FR/BE et le champ `typeRupture` existent côté modèle.
- `SF-IA-03-05` (Done) — la cohérence consomme `compensationEstimate.typeRupture`.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi un fallback Java malgré un prompt renforcé** : le prompt ne garantit rien à 100 %. Un fallback déterministe depuis des signaux déjà fiables (F-96, qualification juridique) assure une couverture robuste même en cas de hallucination ou d'omission IA.
- **Pourquoi un estimate partiel au lieu de vide** : permet de faire remonter `typeRupture` sans chiffrer. Le calcul Macron ne doit pas s'afficher pour une rupture conventionnelle ; mais le champ pré-remplit F-DT-09 et nourrit les alertes de cohérence F-IA-03.
- **Pourquoi pas modifier le frontend** : le composant lit déjà `compensationEstimate.typeRupture`. Le problème était en amont (champ null). Corriger à la source évite d'accumuler des fallbacks sur plusieurs couches.
- **Pourquoi ne pas ajouter de nouveau type** : l'enum existant couvre tous les cas métiers V1 France + Belgique. Toute extension devrait passer par une vraie analyse métier séparée.
