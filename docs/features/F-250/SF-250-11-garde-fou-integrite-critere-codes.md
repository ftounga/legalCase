# SF-250-11 — Garde-fou d'intégrité des critereCode F-IA-03 (F-250)

**Feature parente** : F-250 — Complétion de la validation de cohérence (F-IA-03) des outils décisionnels
**Date** : 2026-05-19
**Branche** : `feat/SF-250-11-garde-fou-integrite`

---

## Identifiant

`F-250 / SF-250-11`

## Feature parente

`F-250` — Complétion de la validation de cohérence (F-IA-03) des outils décisionnels

## Statut

`in-progress`

## Date de création

2026-05-19

## Branche Git

`feat/SF-250-11-garde-fou-integrite`

---

## Objectif

Créer un test d'intégrité backend `CritereCodeIntegrityIT` qui échoue automatiquement à chaque build si un `critereCode` utilisé par un composant décisionnel frontend n'est émis par aucun des deux prompts LLM backend (`CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` et `AiQuestionService.SYSTEM_PROMPT_TEMPLATE`), détectant ainsi toute future dette du type répararé par SF-250-02 à SF-250-10.

---

## Inventaire frontend (étape 1)

### Comment les composants frontend déclarent leurs critereCode

Après analyse des 166 fichiers `.ts` de `frontend/src/app/**/*-section/` contenant `critereCode`, les déclarations sont **éparpillées** dans les composants :

- Certains composants utilisent des maps littérales inline (`critereCode: 'XXXXX'` dans des objets de config)
- D'autres utilisent des constantes locales (ex : `FIELD_CRITERE_CODES` dans `belgian-9bis-section`)
- D'autres encore testent `chk.critereCode?.toUpperCase() === 'XXXXX'` directement dans la logique
- Aucun registre centralisé unique n'existe côté frontend

Il n'existe **pas** de fichier TypeScript unique listant l'ensemble des `critereCode` attendus pour le cross-check F-IA-03. La liste est donc **implicitement définie par la union** de tous les codes hard-codés dans les composants.

### Liste complète des critereCode frontend attendus (91 codes)

Extraits par `grep -oP "'[A-Z][A-Z0-9_]{2,}'" sur tous les `.ts` non-spec` :

**Domaine Travail (FR + BE)** (24 codes) :
`FR_CONVOCATION`, `FR_ENTRETIEN`, `FR_DELAI_NOTIFICATION`, `FR_MOTIVATION`, `FR_MOTIF_REEL`, `FR_PROCEDURE_DISCIPLINAIRE`, `FR_ORDRE_LICENCIEMENT`,
`BE_NOTIFICATION`, `BE_PREAVIS`, `BE_MOTIVATION`, `BE_AUDITION`, `BE_NON_DISCRIMINATION`, `BE_PROTECTION_SPECIALE`, `BE_INDEMNITE_MANIFESTE`,
`DT09_TYPE_RUPTURE`, `DT36_DATE_ENTRETIEN`, `DT36_MOTIVATION`, `DT36_ENTRETIEN_TENU`, `HLN_MOTIF_NULLITE`,
`DT13_MOTIF_ECONOMIQUE`, `DT13_DATE_NOTIFICATION`, `PSE_DATE_PROJET`, `PROTECTION_RP_MOTIF`,
`RC_CONSENTEMENT`, `RC_DELAI_RETRACTATION`, `RC_HOMOLOGATION`, `RC_ASSISTANCE`, `RC_INDEMNITE`, `RC_ENTRETIENS`,
`DT22_SALAIRE`, `DT23_SALAIRE`, `DT24_SALAIRE`,
`DT31_SALAIRE_MENSUEL`, `DT31_ANCIENNETE`, `RCI_SALAIRE`, `RCI_ANCIENNETE`,
`INAPT_ORIGINE`, `INAPT_RECLASSEMENT`, `AT_MP_DATE_ACCIDENT`,
`CREDIT_TEMPS_ANCIENNETE`, `CREDIT_TEMPS_AGE`, `TRAVAIL_PROCEDURE_TYPE`

**Domaine Immigration (FR + BE)** (30+ codes) :
`IM05_MOTIF`, `IM06_RECOURS_TYPE`, `IM07_TITRE_TYPE`,
`IM08_MOTIF_OQTF`, `IM08_RECOURS_FORME`, `IM08RA_DECISION_CONTESTEE`, `IM08_MOTIF_OQT_BE`,
`IM09_DATE_ENTREE_FRANCE`, `IM09_MOIS_ACTIVITE`, `IM09_ETU_DATE_ENTREE_FRANCE`, `IM09_ETU_DUREE_PRESENCE`, `IM09_ETU_DATE_DEPOT_DEMANDE`, `IM09H_DATE_ENTREE_FRANCE`, `IM09H_MOTIF_HUMANITAIRE`,
`IM11_TITRE_ACTUEL`, `IM12_DISPOSITIF_ASILE`, `IM13_VOIE_NATURALISATION`,
`IM17_VOIE_REGIME_ALGERIEN`,
`IM19_DATE_ENTREE`, `IM19_DATE_NAISSANCE`,
`IM20_DISPOSITIF_ELOIGNEMENT`, `IM20_MOTIF_MENACE`,
`IM21_DATE_NOTIFICATION_PLACEMENT`, `IM21_PLACEMENT_CRA`, `IM21_MOTIF_PLACEMENT`,
`IM22_DATE_NOTIFICATION_DUBLIN`, `IM23_DATE_NOTIFICATION_REFUS`,
`BE_9TER_MALADIE_GRAVE`, `BE_9TER_SOINS_BE`, `BE_9TER_SOINS_INACCESSIBLES`, `BE_9TER_MENACE_ORDRE_PUBLIC`,
`B9BIS_DATE_ENTREE_BELGIQUE`, `B9BIS_DUREE_PRESENCE`, `B9BIS_CIRCONSTANCES_EXCEPTIONNELLES`, `B9BIS_LIENS_FAMILIAUX_BE`, `B9BIS_LIENS_PROFESSIONNELS`, `B9BIS_SCOLARITE_ENFANTS_BE`, `B9BIS_MENACE_ORDRE_PUBLIC`, `B9BIS_DATE_DEPOT_DEMANDE`,
`IM14_LIEN_FAMILIAL`

**Domaine Famille (FR + BE)** (50+ codes) :
`FA06_MODE_GARDE`, `DA_DUREE_MARIAGE`, `FA09_DUREE_MARIAGE`, `FA09_FAUTES_INVOQUEES`, `FA09_DATE_DEPOT_ASSIGNATION`,
`FA12_DATE_AUDIENCE`, `FA12_VIOLENCES`, `FA13_NB_ENFANTS`,
`FA14_DATE_REQUETE`, `FA14_VIOLENCES_ALLEGUEES`, `FA14_LOGEMENT_COMMUN`,
`FA15_REGIME_MATRIMONIAL`,
`COMMUNAUTE_UNIVERSELLE_CONTRAT_NOTARIE`, `COMMUNAUTE_UNIVERSELLE_ENFANTS_NON_COMMUNS`,
`PARTAGE_JUDICIAIRE_PV`, `PARTAGE_JUDICIAIRE_TENTATIVE_AMIABLE`,
`FA19_REGIME_EXERCICE_ACTUEL`, `FA19_DANGER_CARACTERISE`, `FA19_CONSENTEMENT_AUTRE_PARENT`, `FA19_INTERFERENCE_VIE_ENFANT`, `FA19_AGE_ENFANTS`, `FA19_RAISON_CHANGEMENT`, `FA19_INFORME_PREALABLEMENT`, `FA19_MODE_RESIDENCE_ACTUEL`, `FA19_DOMAINE_DESACCORD`, `FA19_INTENSITE_DESACCORD`, `FA19_TENTATIVES_MEDIATION`, `FA19_AGE_ENFANTS_CONCERNES`, `FA19_URGENCE`,
`FA20_MODE_DISSOLUTION`, `FA20_REGIME_BIENS`, `FA20_CREANCES_ALLEGUEES`,
`FA21_DATE_JUGEMENT_SEPARATION`, `FA22_DATE_ORIGINE`, `FA22_OCCUPATION`,
`TESTAMENT_FORME`, `TESTAMENT_SAINE_ESPRIT`, `TESTAMENT_QUOTITE`,
`DONATION_FORME`, `DONATION_SAINE_ESPRIT`, `DONATION_QUOTITE`,
`PARTAGE_MODE`, `PARTAGE_CONSENTEMENTS`, `PARTAGE_PRESENCE_IMMEUBLES`,
`INDIVISION_DATE_OUVERTURE`, `INDIVISION_TYPE`,
`DEVOLUTION_LEGALE_CONJOINT`, `DEVOLUTION_LEGALE_DESCENDANTS_COMMUNS`,
`FA25_DATE_CERTIFICAT`, `FA25_ALT_MENTALES`, `FA25_CONSENTEMENT`, `FA25_DEMANDEUR_FAMILIAL`,
`FA26_TYPE_CHANGEMENT`, `FA26_MOTIF_INVOQUE`, `FA26_DATE_NAISSANCE`, `FA26_MAJEUR_DEMANDEUR`, `FA26_CONSENTEMENT_PARENTAL`,
`PMA_GPA_DISPOSITIF`, `FA05_VALEUR_VENALE`, `FA05_CAPITAL_RESTANT`,
`DESU_BE_DATE_SEPARATION`, `DESU_BE_CONSENTEE`, `DESU_BE_DATE_ASSIGNATION`,
`F217_DATE_MARIAGE`, `F217_DATE_NOTIFICATION_PROJET`,
`CONSENTEMENT_LIBRE`, `PATERNITE_VRAISEMBLABLE`,
`CONTESTATION_PATERNITE_MOTIFS_SERIEUX`, `CONTESTATION_PATERNITE_EXPERTISE_ADN`, `CONTESTATION_PATERNITE_POSSESSION_ETAT`,
`RECHERCHE_PATERNITE_POSSESSION_ETAT`, `RECHERCHE_PATERNITE_EXPERTISE_ADN`, `RECHERCHE_PATERNITE_REFUS_ADN`, `RECHERCHE_PATERNITE_MOTIFS_SERIEUX`,
`POSSESSION_ETAT_TRACTATUS`, `POSSESSION_ETAT_FAMA`, `POSSESSION_ETAT_CONTINUE`, `POSSESSION_ETAT_PAISIBLE`, `POSSESSION_ETAT_NON_EQUIVOQUE`,
`ADOPTION_FORME`, `ADOPTION_PUPILLE_ETAT`, `ADOPTION_ADOPTANT_MARIE`, `ADOPTION_AGE_ADOPTANT`, `ADOPTION_AGE_ADOPTE`,
`RESERVE_CONJOINT_SURVIVANT`,
`RAPPORT_QUALITE_HERITIER`, `RAPPORT_DONATION_NOMINALE`, `RAPPORT_VALEUR_PARTAGE`, `RAPPORT_DATE_DONATION`

**Checklist divorce FR + BE** (20 codes — pièces et étapes F-FA-07) :
`FR_ACTE_MARIAGE`, `FR_ACTE_NAISSANCE_EPOUX`, `FR_ACTE_NAISSANCE_ENFANTS`, `FR_LIVRET_FAMILLE`, `FR_JUSTIF_DOMICILE`, `FR_CONTRAT_MARIAGE`, `FR_ETAT_PATRIMOINE`, `FR_JUSTIF_REVENUS`, `FR_PIECE_IDENTITE`,
`FR_CHOIX_AVOCATS`, `FR_REDACTION_CONVENTION`, `FR_ENVOI_LRAR`, `FR_DELAI_REFLEXION`, `FR_SIGNATURE_CONVENTION`, `FR_DEPOT_NOTAIRE`, `FR_ENREGISTREMENT`,
`BE_ACTE_MARIAGE`, `BE_ACTE_NAISSANCE_EPOUX`, `BE_ACTE_NAISSANCE_ENFANTS`, `BE_COMPOSITION_MENAGE`, `BE_CONTRAT_MARIAGE`, `BE_CONVENTION_PREALABLE`, `BE_JUSTIF_REVENUS`, `BE_PIECE_IDENTITE`,
`BE_CHOIX_AVOCAT`, `BE_REDACTION_CONVENTION`, `BE_REQUETE_CONJOINTE`, `BE_COMPARUTION`, `BE_JUGEMENT`, `BE_TRANSCRIPTION`

**Dossier immigration validité (F-IM-21)** :
`IM21_REGULARITE_SEJOUR_FR`, `IM21_DELAI_DEPOT_FR`, `IM21_PIECE_IDENTITE_FR`, `IM21_JUSTIF_DOMICILE_FR`, `IM21_ETAT_CIVIL_FR`, `IM21_PHOTO_FR`, `IM21_TIMBRE_FISCAL_FR`, `IM21_PIECES_MARIAGE_FR`, `IM21_COMMUNAUTE_VIE_FR`, `IM21_RESSOURCES_FR`, `IM21_CONVENTION_ACCUEIL_FR`,
`IM21_REGULARITE_SEJOUR_BE`, `IM21_PIECE_IDENTITE_BE`, `IM21_PIECES_COHABITATION_BE`, `IM21_RESSOURCES_BE`, `IM21_LOGEMENT_BE`, `IM21_ASSURANCE_BE`, `IM21_EXTRAIT_CASIER_BE`

---

## Choix d'implémentation (étape 2)

### Deux options analysées

**(a) Parsing des sources frontend** : parcourir les 166 fichiers `.ts` et extraire les `critereCode` par regex.
- Avantages : auto-sync, pas de liste à maintenir
- Inconvénients : fragile (patterns hétérogènes en 166 fichiers), très lent, faux positifs dans les specs, faux positifs dans les commentaires, plusieurs patterns de déclaration différents, pas de distinction entre "code attendu du prompt" et "code de test unitaire"

**(b) Liste de référence maintenue dans le test** : une constante `KNOWN_FRONTEND_CRITERE_CODES` déclarée dans le test lui-même, que le test vérifie contre le contenu des prompts backend.
- Avantages : robuste, lisible, déterministe, rapide, documenté, facile à maintenir (1 ligne à ajouter par nouveau code), source de vérité explicite et auditable
- Inconvénients : nécessite une mise à jour manuelle quand un nouveau code est ajouté

### Choix retenu : option (b)

**Justification** : l'option (a) est techniquement réalisable mais produit un test fragile et difficile à maintenir. Le test `DecisionToolVisibilityIntegrityIT` (SF-164-01) a lui-même migré d'une liste hardcodée vers le parsing du source frontend — mais uniquement parce que le frontend avait un registre centralisé unique (`TOOL_REGISTRY` dans `decisional-tools-panel.component.ts`). Pour les `critereCode`, aucun registre centralisé n'existe : les codes sont dispersés dans 166 fichiers avec des patterns de déclaration hétérogènes. Extraire dynamiquement la liste via regex serait plus susceptible aux faux positifs (codes dans des specs, des commentaires, des variables locales de test) et aux faux négatifs (patterns non couverts par la regex).

L'option (b) est cohérente avec le pattern `BuilderPatternEnforcementIT` (`PROTECTED_RECORDS`) et garantit que la liste est **explicite, auditable et toujours à jour** : tout ajout d'un `critereCode` frontend sans mise à jour du test `CritereCodeIntegrityIT` sera détecté lors de la première exécution en CI, car le test échouerait si un code listé est absent des prompts backend. Inversement, si un code est ajouté en frontend sans être ajouté à la liste de référence du test, la CI ne détectera pas la divergence — mais cela sera visible lors de la review car la liste de référence est dans le test lui-même, co-commitée avec tout ajout frontend.

La liste `KNOWN_FRONTEND_CRITERE_CODES` est la **source de vérité de gouvernance** : un code doit y être inscrit pour être considéré comme "attendu du backend". Elle est maintenue dans `CritereCodeIntegrityIT.java` et est documentée dans cette mini-spec comme obligation de mise à jour.

---

## Comportement attendu

### Cas nominal

Le test `CritereCodeIntegrityIT` lit :
1. Le `SYSTEM_PROMPT_TEMPLATE` de `CaseAnalysisService` (via accès à la constante `package-private`)
2. Le `SYSTEM_PROMPT_TEMPLATE` de `AiQuestionService` (via accès à la constante `package-private`)

Pour chaque code dans `KNOWN_FRONTEND_CRITERE_CODES`, le test vérifie que le code apparaît (case-insensitive) dans au moins l'un des deux prompts. Le test **réussit** si tous les codes sont couverts.

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| Un code dans `KNOWN_FRONTEND_CRITERE_CODES` absent des deux prompts | Test FAIL avec message listant les codes orphelins |
| `KNOWN_FRONTEND_CRITERE_CODES` vide | Test FAIL avec assertion explicite (liste non vide requise) |

---

## Critères d'acceptation

- [ ] Le test `CritereCodeIntegrityIT` compile et s'exécute dans le package `fr.ailegalcase.analysis`
- [ ] Le test lit directement les constantes `package-private` `SYSTEM_PROMPT_TEMPLATE` de `CaseAnalysisService` et `AiQuestionService` (même package)
- [ ] `KNOWN_FRONTEND_CRITERE_CODES` contient tous les codes listés dans cette mini-spec (vérifiable par diff)
- [ ] Le test **passe** sur la branche `feat/SF-250-11-garde-fou-integrite` car SF-250-02→10 ont ajouté tous les codes aux prompts
- [ ] Le message d'erreur en cas d'échec liste explicitement les codes orphelins et indique le fichier à mettre à jour
- [ ] Le test ne dépend pas de la base de données, ne démarre pas le contexte Spring (test unitaire pur ou `@SpringBootTest` léger selon le besoin)

---

## Périmètre

### Dans le scope

- `backend/src/test/java/fr/ailegalcase/analysis/CritereCodeIntegrityIT.java` — le test
- `KNOWN_FRONTEND_CRITERE_CODES` — la liste de référence (constante dans le test)

### Hors scope

- Aucune modification des prompts (`CaseAnalysisService`, `AiQuestionService`) — ils sont complets après SF-250-02→10
- Aucune modification frontend
- Aucune migration Liquibase
- Aucun endpoint nouveau

---

## Technique

### Composants impactés

| Composant | Opération |
|---|---|
| `backend/src/test/.../CritereCodeIntegrityIT.java` | CREATE |

### Pas de migration Liquibase

Non applicable — test de gouvernance uniquement.

---

## Plan de test

### Test lui-même

Le test `CritereCodeIntegrityIT` est le plan de test complet. Il couvre :
- [ ] Tous les codes listés dans `KNOWN_FRONTEND_CRITERE_CODES` sont présents dans au moins un prompt backend
- [ ] La liste n'est pas vide (assertion de garde)

### Commande d'exécution

```bash
cd /home/francky/dev/legalCase/backend && ./mvnw test -Dtest=CritereCodeIntegrityIT -q
```

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité à son périmètre (test de gouvernance uniquement, aucun code de production modifié)

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — test backend pur, aucun comportement runtime modifié

---

## Conformité F-IA-04

- [x] **Non applicable** — SF backend pure, aucun composant frontend décisionnel livré ou modifié. Aucun pré-remplissage, aucun TOOL_REGISTRY, aucun endpoint.

## Champs IA à extraire

- [x] **Aucun pré-remplissage** — SF de gouvernance, aucun outil décisionnel créé.

---

## Notes et décisions

1. **Accès aux constantes** : `CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` et `AiQuestionService.SYSTEM_PROMPT_TEMPLATE` sont déclarés `static final String` avec visibilité `package-private` (pas de modificateur). Le test est dans le même package `fr.ailegalcase.analysis`, donc l'accès est direct sans réflexion.

2. **Sans contexte Spring** : le test n'a pas besoin du contexte Spring pour lire des constantes statiques. On utilise `@SpringBootTest` uniquement si nécessaire pour la compatibilité avec la configuration Maven Surefire/Failsafe. À éviter pour la rapidité.

3. **Maintenance** : quand un nouveau `critereCode` est ajouté côté frontend, le développeur DOIT l'ajouter à `KNOWN_FRONTEND_CRITERE_CODES` dans ce test ET l'ajouter aux prompts backend. L'ordre est important : sans mise à jour du prompt, le test échoue en CI.

4. **Séparation pièces manquantes / points procédure** : les codes apparaissent dans les deux prompts (pièces manquantes ET points procédure) sans distinction dans le test — ce qui est correct car les composants frontend consomment les deux sources.
