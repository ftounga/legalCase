# Plan de test manuel — 12 dossiers × features (incluant F-IA-03 cohérence)

## Prérequis

- App démarrée en local ou staging opérationnel
- 5 workspaces créés :
  - DROIT_DU_TRAVAIL / FRANCE
  - DROIT_DU_TRAVAIL / BELGIQUE
  - DROIT_IMMIGRATION / FRANCE
  - DROIT_FAMILLE / FRANCE
  - DROIT_FAMILLE / BELGIQUE

## Nouveautés F-IA-03 à vérifier partout

À chaque test, contrôler systématiquement :
- **Pré-remplissage IA** : les champs des outils métier sont pré-remplis depuis l'analyse IA
- **Provenance notes** : "Pré-rempli depuis l'analyse IA" visible sous les champs concernés (F-DT-09 type rupture, F-FA-05 valeurs, F-FA-06 mode garde, F-IM-06 recours, F-IM-07 titre)
- **Cohérence IA** : si l'avocat modifie une valeur de manière à diverger de l'IA (ou des points F-96 / réponses aux questions IA), un badge warning/blocker apparaît à côté du champ avec tooltip explicatif
- **Bandeau récap** : compteur "X incohérences (Y bloquantes)" en haut de la section concernée quand il y a ≥ 1 alerte

---

## Test 1 — Dupont (Syntec licenciement FR)

**Dossier :** `dossier-licenciement-syntec-dupont/` (4 fichiers)
**Workspace :** DROIT_DU_TRAVAIL / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 1.1 | Créer dossier "Dupont", uploader les 4 fichiers | Dossier créé, 4 documents listés |
| 1.2 | Lancer l'analyse IA | Synthèse : faits, risques, timeline |
| 1.3 | Vérifier **pré-remplissage F-IA-01** section Ancienneté | Convention=SYNTEC, date entrée=01/09/2018, salaire=4200€ |
| 1.4 | **SF-IA-01-03** : vérifier pré-cochage de la grille Validité licenciement | Critères FR_CONVOCATION, FR_MOTIVATION, etc. pré-remplis (OUI/NON) depuis l'IA |
| 1.5 | Calculer Ancienneté (F-DT-07) → modifier salaire de 10 % (ex: 4620) | **SF-IA-03-04** : badge orange warning "Incohérence IA (4200€)" visible |
| 1.6 | Revenir à 4200 | Badge disparaît |
| 1.7 | Grille Validité licenciement (F-DT-08) → modifier FR_MOTIVATION vers NON contrairement à l'IA | **SF-IA-03-01** : badge rouge `blocker` "Incohérence IA (OUI)", tooltip justification IA |
| 1.8 | Comparateur indemnités (F-DT-09) : vérifier **SF-DT-09-04** sélecteur type de rupture | Pré-sélectionné `LICENCIEMENT` depuis IA, 3 options FR |
| 1.9 | F-DT-09 : modifier type vers RUPTURE_CONVENTIONNELLE | **SF-IA-03-05** : badge blocker + affichage mode INDEMNITE_SPECIFIQUE avec indemnité légale calculée |
| 1.10 | Revenir LICENCIEMENT + calculer | Fourchette Macron affichée normalement |
| 1.11 | Dashboard (F-IA-02) | Cards agrégées |

**Features testées :** F-DT-07, F-DT-08, F-DT-09, F-IA-01, F-IA-02, **SF-IA-01-03, SF-DT-09-04, SF-IA-03-01/04/05**

---

## Test 2 — Martin (BTP rupture conventionnelle FR)

**Dossier :** `dossier-rupture-btp-martin/` (2 fichiers)
**Workspace :** DROIT_DU_TRAVAIL / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 2.1 | Créer dossier, uploader 2 fichiers, analyser | Synthèse avec rupture conventionnelle détectée |
| 2.2 | Ancienneté (F-DT-07) | Convention=BTP, 15 ans, congés 25+4=29j, prime 12% |
| 2.3 | **SF-DT-09-04** : ouvrir Comparateur indemnités | Type `RUPTURE_CONVENTIONNELLE` pré-sélectionné depuis IA |
| 2.4 | Calculer → vérifier mode INDEMNITE_SPECIFIQUE | Indemnité légale calculée (1/4 × min(10, anc) + 1/3 × max(0, anc-10)) × salaire, message "doit être ≥ indemnité légale" |
| 2.5 | Modifier type vers LICENCIEMENT → recalculer | Bascule vers fourchette Macron, messages contextuels mis à jour |
| 2.6 | Revenir RUPTURE_CONVENTIONNELLE | **SF-IA-03-05** : pas d'alerte car match IA |
| 2.7 | Modifier ancienneté vers 8 ans (vs 15 IA) | **SF-IA-03-05** : warning numérique (écart > 0,5 an) |
| 2.8 | Dashboard | Cards ancienneté + indemnités |

**Features testées :** F-DT-07, F-DT-09, F-IA-01, F-IA-02, **SF-DT-09-04, SF-IA-03-05**

---

## Test 3 — Janssen (CP200 licenciement BE)

**Dossier :** `dossier-licenciement-cp200-janssen/` (3 fichiers)
**Workspace :** DROIT_DU_TRAVAIL / BELGIQUE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 3.1 | Créer dossier, uploader 3 fichiers, analyser | Synthèse avec références belges |
| 3.2 | **SF-IA-01-03** : grille Validité licenciement | Critères BE_NOTIFICATION, BE_PREAVIS, BE_MOTIVATION pré-cochés depuis l'IA |
| 3.3 | Ancienneté (F-DT-07) | Convention=CP200, 8 ans, congés 20+2=22j, prime 4% |
| 3.4 | **SF-IA-03-04** : modifier congés vers 30j | Warning "Incohérence IA (22j)" |
| 3.5 | Comparateur indemnités (F-DT-09) | **SF-DT-09-04** : type `LICENCIEMENT_ORDINAIRE` pré-sélectionné (BE) |
| 3.6 | Modifier vers RUPTURE_AMIABLE | Mode NEGOCIATION_LIBRE, pas de fourchette, message négociation libre |
| 3.7 | **SF-IA-03-01/02** : modifier BE_NOTIFICATION vers NON | Badge blocker (critère bloquant) |
| 3.8 | Dashboard | Tout agrégé, country=BELGIQUE |

**Features testées :** F-DT-07, F-DT-08, F-DT-09, F-IA-02, **SF-IA-01-03, SF-DT-09-04, SF-IA-03-01/02/04/05**

---

## Test 4 — Peeters (CP124 faute grave BE)

**Dossier :** `dossier-faute-grave-cp124-peeters/` (3 fichiers)
**Workspace :** DROIT_DU_TRAVAIL / BELGIQUE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 4.1 | Créer dossier, uploader 3 fichiers, analyser | OK |
| 4.2 | **SF-IA-01-03** : Validité licenciement pré-cochée | BE_PREAVIS=NON automatique (faute grave) |
| 4.3 | Modifier BE_PREAVIS vers OUI contrairement à l'IA | **SF-IA-03-01** : blocker (critère bloquant) + tooltip justification IA |
| 4.4 | Ancienneté (F-DT-07) | CP124, 3 ans, pas de prime |
| 4.5 | Dashboard | Card licenciement avec alerte rouge |

**Features testées :** F-DT-07, F-DT-08, F-IA-02, **SF-IA-01-03, SF-IA-03-01**

---

## Test 5 — Diallo (titre salarié FR)

**Dossier :** `dossier-titre-salarie-diallo/` (4 fichiers)
**Workspace :** DROIT_IMMIGRATION / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 5.1 | Créer dossier, uploader 4 fichiers, analyser | `type_titre_sejour` extrait |
| 5.2 | **SF-IM-05-04** : ouvrir Titre de séjour recommandé (F-IM-05) | `motif` pré-rempli (TRAVAIL) via mapping CODE_TO_MOTIF, `nationaliteUe` booléen pré-sélectionné |
| 5.3 | **SF-IA-03-09** : modifier motif vers ETUDES | Badge warning "Incohérence IA (TRAVAIL)" |
| 5.4 | Revenir TRAVAIL → résoudre | Résultat : VLS-TS salarié / CST salarié |
| 5.5 | **SF-IA-03-11** : ouvrir Droit au travail (F-IM-07) | Titre `VLS_TS_SALARIE` pré-rempli + provenance note |
| 5.6 | Modifier titre vers CARTE_RESIDENT | Warning "Incohérence IA (VLS_TS_SALARIE)" |
| 5.7 | Revenir → résoudre → OUI, obligations employeur | Autorisé, DPAE, vérification préfecture |
| 5.8 | Checklist pièces (F-IM-01) | Checklist type TITRE_SALARIE/FRANCE |
| 5.9 | Dashboard | Cards titre + droit travail OUI |

**Features testées :** F-IM-01, F-IM-05, F-IM-07, F-IA-02, **SF-IM-05-04, SF-IA-03-09/11**

---

## Test 6 — Yilmaz (asile Belgique)

**Dossier :** `dossier-asile-belgique-yilmaz/` (3 fichiers)
**Workspace :** DROIT_IMMIGRATION / FRANCE (workspace FR pour immigration)

| Étape | Action | Vérification |
|-------|--------|-------------|
| 6.1 | Créer dossier, uploader 3 fichiers, analyser | type_procedure = DEMANDE_ASILE |
| 6.2 | Titre recommandé (F-IM-05) | Attestation d'immatriculation / RECEPISSE_ASILE |
| 6.3 | **SF-IM-06-04** : ouvrir Recours (F-IM-06) | Type `RECOURS_CNDA` pré-sélectionné + date notification pré-remplie, 2 provenance notes visibles |
| 6.4 | **SF-IA-03-10** : modifier type vers RECOURS_GRACIEUX_PREFET | Badge warning "Incohérence IA (RECOURS_CNDA)" |
| 6.5 | Modifier date notification de +10 jours vs IA | Badge warning "écart de 10 jours" (seuil 7j strict) |
| 6.6 | Revenir → générer | Délai 30 jours, date limite calculée |
| 6.7 | **SF-IA-03-11** : Droit au travail (F-IM-07) | Pré-remplissage titre, CONDITIONNEL après 4 mois |
| 6.8 | Dashboard | Card recours + droit travail conditionnel |

**Features testées :** F-IM-05, F-IM-06, F-IM-07, F-IA-02, **SF-IM-05-04, SF-IM-06-04, SF-IA-03-09/10/11**

---

## Test 7 — Chen (regroupement familial FR)

**Dossier :** `dossier-regroupement-familial-chen/` (3 fichiers)
**Workspace :** DROIT_IMMIGRATION / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 7.1 | Créer dossier, uploader 3 fichiers, analyser | Motif FAMILLE |
| 7.2 | **SF-IM-05-04** : Titre recommandé | `motif=FAMILLE` pré-sélectionné, CST_VPF recommandé |
| 7.3 | **SF-IA-03-09** : modifier motif vers TRAVAIL | Warning "Incohérence IA (FAMILLE)" |
| 7.4 | **SF-IA-03-11** : Droit au travail → modifier titre vers VLS_TS_SALARIE | Warning "Incohérence IA (CST_VPF)" |
| 7.5 | Revenir → résoudre | OUI plein droit |
| 7.6 | Dashboard | Cards titre + droit travail OUI (vert) |

**Features testées :** F-IM-05, F-IM-07, F-IA-02, **SF-IM-05-04, SF-IA-03-09/11**

---

## Test 8 — Kowalski (permis unique BE)

**Dossier :** `dossier-permis-unique-kowalski/` (3 fichiers)
**Workspace :** DROIT_IMMIGRATION / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 8.1 | Créer dossier, uploader 3 fichiers, analyser | OK |
| 8.2 | Titre recommandé (F-IM-05) | UE → libre circulation / carte B |
| 8.3 | **SF-IM-06-04** : Recours → vérifier pré-remplissage | `RECOURS_CCE` ou `RECOURS_CGRA` pré-sélectionné selon détection IA |
| 8.4 | **SF-IA-03-10** : modifier vers RECOURS_GRACIEUX_PREFET (code FR sur contexte BE) | Warning "Incohérence IA" |
| 8.5 | **SF-IA-03-11** : Droit au travail → modifier titre BE | Warning si mismatch |
| 8.6 | Dashboard | Cards recours + droit travail |

**Features testées :** F-IM-05, F-IM-06, F-IM-07, F-IA-02, **SF-IM-05-04, SF-IM-06-04, SF-IA-03-09/10/11**

---

## Test 9 — Moreau (divorce amiable FR)

**Dossier :** `dossier-divorce-amiable-moreau/` (4 fichiers)
**Workspace :** DROIT_FAMILLE / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 9.1 | Créer dossier, uploader 4 fichiers, analyser | pension_alimentaire, prestation_compensatoire, liquidation extraits |
| 9.2 | **SF-FA-05-04** : ouvrir Partage immobilier | Bouton "Importer depuis l'analyse IA" visible. Cliquer → panneau avec bien + prêt détectés. Sélectionner + Appliquer → champs remplis + 2 provenance notes |
| 9.3 | Modifier valeur vénale de +15 % | **SF-IA-03-08** : warning "Incohérence IA (valeur détectée)" |
| 9.4 | Revenir → Calculer | 350K€, prêt 120K€, 50/50, divorce → droit 1.1% = 2530€ |
| 9.5 | **SF-FA-06-04** : Calendrier garde | `ALTERNEE_FR` pré-sélectionné (mode détaillé IA) + note si pays opposé |
| 9.6 | **SF-IA-03-07** : modifier vers DVH_CLASSIQUE_FR | Warning IA_COARSE (catégorie alternée → non alternée) ou blocker IA si mode détaillé détecté |
| 9.7 | Résidence alternée → 182/183 jours |
| 9.8 | **SF-IA-03-06** : Checklist divorce (F-FA-07) | 7 étapes FR + 9 pièces. Cocher FR_CHOIX_AVOCATS en "FAIT" alors que l'IA détecte NON_COMPLIANT via F-96 → badge blocker |
| 9.9 | Pièce PRESENTE sur FR_ACTE_MARIAGE alors que pieces_manquantes IA l'indique manquante | Badge warning "Incohérence IA" |
| 9.10 | Progression 0% → cocher → progression augmente | Progress bar mise à jour |
| 9.11 | Dashboard | Cards soulte + garde + divorce progression + risk score |

**Features testées :** F-FA-05, F-FA-06, F-FA-07, F-IA-01, F-IA-02, **SF-FA-05-04, SF-FA-06-04, SF-IA-03-06/07/08**

---

## Test 10 — Dubois (divorce contentieux FR)

**Dossier :** `dossier-divorce-contentieux-dubois/` (3 fichiers)
**Workspace :** DROIT_FAMILLE / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 10.1 | Créer dossier, uploader 3 fichiers, analyser | OK |
| 10.2 | **SF-FA-05-04** : Partage immobilier avec import IA | 280K€, prêt 90K€, 60/40 |
| 10.3 | **SF-IA-03-08** : saisir valeur manuelle très différente (+20 %) vs best-match IA | Warning visible |
| 10.4 | **SF-FA-06-04** : Calendrier garde DVH classique | `DVH_CLASSIQUE_FR` pré-sélectionné ou fallback selon IA |
| 10.5 | 249/116 jours | OK |
| 10.6 | Pension alimentaire + prestation compensatoire (existants) | Revenus 5200€ vs 1800€ |
| 10.7 | Dashboard | Cards partage + garde + pension |

**Features testées :** F-FA-05, F-FA-06, F-IA-02, **SF-FA-05-04, SF-FA-06-04, SF-IA-03-07/08**

---

## Test 11 — Vermeersch (divorce Belgique)

**Dossier :** `dossier-divorce-belgique-vermeersch/` (3 fichiers)
**Workspace :** DROIT_FAMILLE / BELGIQUE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 11.1 | Créer dossier, uploader 3 fichiers, analyser | OK |
| 11.2 | Partage immobilier (F-FA-05) | 420K€, prêt 150K€, 50/50, droit BE 1% |
| 11.3 | **SF-FA-06-04** : Calendrier garde → hébergement égalitaire BE | `ALTERNEE_BE` pré-sélectionné |
| 11.4 | **SF-IA-03-07** : modifier vers SECONDAIRE_ELARGI_BE | Warning (différents modes BE) |
| 11.5 | 182/183 jours | OK |
| 11.6 | **SF-IA-03-06** : Checklist divorce BE (6 étapes) | Cocher BE_REQUETE_CONJOINTE + F-96 NON_COMPLIANT → blocker |
| 11.7 | Dashboard | Cards soulte BE + garde + divorce |

**Features testées :** F-FA-05, F-FA-06, F-FA-07, F-IA-02, **SF-FA-05-04, SF-FA-06-04, SF-IA-03-06/07/08**

---

## Test 12 — De Smet (garde seule BE)

**Dossier :** `dossier-garde-seule-desmet/` (2 fichiers)
**Workspace :** DROIT_FAMILLE / BELGIQUE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 12.1 | Créer dossier, uploader 2 fichiers, analyser | OK |
| 12.2 | **SF-FA-06-04** : Calendrier garde | `SECONDAIRE_BE` pré-sélectionné |
| 12.3 | **SF-IA-03-07** : modifier vers ALTERNEE_BE | Warning IA_COARSE (catégorie change) |
| 12.4 | 249/116 jours | OK |
| 12.5 | Pension alimentaire | Revenus 2900€ vs 2100€ |
| 12.6 | Dashboard | Cards garde + pension |

**Features testées :** F-FA-06, F-IA-02, **SF-FA-06-04, SF-IA-03-07**

---

## Tests transversaux (sur chaque dossier)

| Test | Vérification |
|------|-------------|
| **Référentiels admin** (F-REF-01) | Aller dans /referentials → les 9 nouveaux types visibles et éditables |
| **Édition référentiel** | Modifier une valeur → validation IA se déclenche |
| **Pré-remplissage IA** (F-IA-01) | Après analyse, les formulaires outils sont pré-remplis |
| **Dashboard** (F-IA-02) | En haut du dossier, cards adaptatives |
| **Isolation workspace** | Un dossier d'un workspace n'est pas visible depuis un autre |
| **Upsert** | Modifier les paramètres et relancer → résultat précédent remplacé |
| **GET existant** | Recharger la page → l'outil affiche le dernier résultat sauvegardé |
| **Cohérence F-IA-03 — alertes gelées** | Une fois le résultat d'un outil calculé/sauvegardé, modifier la valeur via "Modifier" ne doit pas déclencher de nouvelle alerte tant que le résultat est chargé |
| **Cohérence F-IA-03 — bandeau compteur** | Dans une section, si ≥ 1 alerte, bandeau "X incohérences (Y bloquantes)" en haut. Compteur blockers = 0 sur F-DT-07, F-DT-09 numérique, F-FA-05, F-FA-06 fallback, F-IM-05, F-IM-06, F-IM-07, F-FA-07 pièces. Compteur blockers > 0 sur F-DT-08 et F-FA-07 étapes |
| **Cohérence F-IA-03 — tooltip source** | Survol d'un badge : tooltip indique la source (F-96, Question IA, IA, PIECE_MANQUANTE, MULTI) + justification ou valeur attendue |
| **Provenance notes** | Sous les champs pré-remplis par IA : "Pré-rempli depuis l'analyse IA" visible. Disparaît dès modification manuelle |

---

## Matrice de couverture

### Features métier

| Feature | T1 | T2 | T3 | T4 | T5 | T6 | T7 | T8 | T9 | T10 | T11 | T12 |
|---------|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| F-DT-07 Ancienneté | ✓ | ✓ | ✓ | ✓ | | | | | | | | |
| F-DT-08 Licenciement | ✓ | | ✓ | ✓ | | | | | | | | |
| F-DT-09 Indemnités | ✓ | ✓ | ✓ | | | | | | | | | |
| F-IM-01 Checklist pièces | | | | | ✓ | | | | | | | |
| F-IM-05 Titre séjour | | | | | ✓ | ✓ | ✓ | ✓ | | | | |
| F-IM-06 Recours | | | | | | ✓ | | ✓ | | | | |
| F-IM-07 Droit travail | | | | | ✓ | ✓ | ✓ | ✓ | | | | |
| F-FA-05 Partage immo | | | | | | | | | ✓ | ✓ | ✓ | |
| F-FA-06 Calendrier garde | | | | | | | | | ✓ | ✓ | ✓ | ✓ |
| F-FA-07 Divorce checklist | | | | | | | | | ✓ | | ✓ | |
| F-IA-01 Pré-remplissage | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| F-IA-02 Dashboard | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |

### Subfeatures F-IA-03 + préalables (nouveauté)

| SF | Outil | T1 | T2 | T3 | T4 | T5 | T6 | T7 | T8 | T9 | T10 | T11 | T12 |
|----|-------|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| SF-IA-01-03 Prefill grille licenciement | F-DT-08 | ✓ | | ✓ | ✓ | | | | | | | | |
| SF-IA-03-01/02/03 Cohérence F-DT-08 | F-DT-08 | ✓ | | ✓ | ✓ | | | | | | | | |
| SF-IA-03-04 Cohérence F-DT-07 | F-DT-07 | ✓ | ✓ | ✓ | | | | | | | | | |
| SF-DT-09-04 Type rupture | F-DT-09 | ✓ | ✓ | ✓ | | | | | | | | | |
| SF-IA-03-05 Cohérence F-DT-09 | F-DT-09 | ✓ | ✓ | ✓ | | | | | | | | | |
| SF-IA-03-06 Cohérence F-FA-07 | F-FA-07 | | | | | | | | | ✓ | | ✓ | |
| SF-FA-06-04 Mode garde détaillé | F-FA-06 | | | | | | | | | ✓ | ✓ | ✓ | ✓ |
| SF-IA-03-07 Cohérence F-FA-06 | F-FA-06 | | | | | | | | | ✓ | ✓ | ✓ | ✓ |
| SF-FA-05-04 Sélecteur IA bien | F-FA-05 | | | | | | | | | ✓ | ✓ | | |
| SF-IA-03-08 Cohérence F-FA-05 | F-FA-05 | | | | | | | | | ✓ | ✓ | | |
| SF-IM-05-04 Normalisation titre | F-IM-05 | | | | | ✓ | ✓ | ✓ | ✓ | | | | |
| SF-IA-03-09 Cohérence F-IM-05 | F-IM-05 | | | | | ✓ | | ✓ | ✓ | | | | |
| SF-IM-06-04 Prefill recours | F-IM-06 | | | | | | ✓ | | ✓ | | | | |
| SF-IA-03-10 Cohérence F-IM-06 | F-IM-06 | | | | | | ✓ | | ✓ | | | | |
| SF-IA-03-11 Cohérence F-IM-07 | F-IM-07 | | | | | ✓ | ✓ | ✓ | ✓ | | | | |
