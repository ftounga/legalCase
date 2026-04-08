# Plan de test manuel — 12 dossiers × features

## Prérequis

- App démarrée en local ou staging opérationnel
- 5 workspaces créés :
  - DROIT_DU_TRAVAIL / FRANCE
  - DROIT_DU_TRAVAIL / BELGIQUE
  - DROIT_IMMIGRATION / FRANCE
  - DROIT_FAMILLE / FRANCE
  - DROIT_FAMILLE / BELGIQUE

---

## Test 1 — Dupont (Syntec licenciement FR)

**Dossier :** `dossier-licenciement-syntec-dupont/` (4 fichiers)
**Workspace :** DROIT_DU_TRAVAIL / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 1.1 | Créer dossier "Dupont" | Dossier créé |
| 1.2 | Uploader les 4 fichiers | 4 documents listés |
| 1.3 | Lancer l'analyse IA | Synthèse générée : faits, risques, timeline |
| 1.4 | Vérifier le **pré-remplissage** (F-IA-01) de la section Ancienneté | Convention = SYNTEC, date entrée = 01/09/2018, salaire = 4200€ pré-remplis |
| 1.5 | Cliquer "Calculer" dans Ancienneté (F-DT-07) | 7 ans, congés 25+1=26j, prime 5%, écarts affichés |
| 1.6 | Remplir la grille Validité licenciement (F-DT-08) | Cocher OUI/NON par critère FR → score + verdict affiché |
| 1.7 | Ouvrir Comparateur indemnités (F-DT-09) | Salaire pré-rempli, fourchettes Macron affichées avec barres visuelles |
| 1.8 | Vérifier le **Dashboard** (F-IA-02) | Cards : risk score IA, validité licenciement, fourchette indemnités, ancienneté + écarts |

**Features testées :** F-DT-07, F-DT-08, F-DT-09, F-IA-01, F-IA-02

---

## Test 2 — Martin (BTP rupture conventionnelle FR)

**Dossier :** `dossier-rupture-btp-martin/` (2 fichiers)
**Workspace :** DROIT_DU_TRAVAIL / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 2.1 | Créer dossier, uploader 2 fichiers | OK |
| 2.2 | Lancer analyse IA | Synthèse avec type rupture conventionnelle détecté |
| 2.3 | Ancienneté (F-DT-07) | Convention = BTP, 15 ans, congés 25+4=29j, prime 12% |
| 2.4 | Comparateur indemnités (F-DT-09) | Fourchette Macron 15 ans, barres visuelles |
| 2.5 | Dashboard | Cards ancienneté + indemnités |

**Features testées :** F-DT-07, F-DT-09, F-IA-01, F-IA-02

---

## Test 3 — Janssen (CP200 licenciement BE)

**Dossier :** `dossier-licenciement-cp200-janssen/` (3 fichiers)
**Workspace :** DROIT_DU_TRAVAIL / BELGIQUE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 3.1 | Créer dossier, uploader 3 fichiers | OK |
| 3.2 | Analyse IA | Synthèse avec références belges |
| 3.3 | Ancienneté (F-DT-07) | Convention = CP200, 8 ans, congés 20+2=22j, prime 4% |
| 3.4 | Validité licenciement (F-DT-08) | Critères **belges** affichés (CCT 109, préavis, non-discrimination) |
| 3.5 | Comparateur indemnités (F-DT-09) | Fourchettes **CCT 109** (3-17 semaines) |
| 3.6 | Dashboard | Tout agrégé, country = BELGIQUE |

**Features testées :** F-DT-07, F-DT-08, F-DT-09, F-IA-02

---

## Test 4 — Peeters (CP124 faute grave BE)

**Dossier :** `dossier-faute-grave-cp124-peeters/` (3 fichiers)
**Workspace :** DROIT_DU_TRAVAIL / BELGIQUE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 4.1 | Créer dossier, uploader 3 fichiers | OK |
| 4.2 | Validité licenciement (F-DT-08) | BE_NOTIFICATION = OUI, BE_PREAVIS = NON (faute grave) → **INVALIDE** si bloquant |
| 4.3 | Ancienneté (F-DT-07) | CP124, 3 ans, pas de prime ni congés supp |
| 4.4 | Dashboard | Card licenciement avec alerte rouge |

**Features testées :** F-DT-07, F-DT-08, F-IA-02

---

## Test 5 — Diallo (titre salarié FR)

**Dossier :** `dossier-titre-salarie-diallo/` (4 fichiers)
**Workspace :** DROIT_IMMIGRATION / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 5.1 | Créer dossier, uploader 4 fichiers | OK |
| 5.2 | Analyse IA | `type_titre_sejour` et `date_expiration_titre` extraits |
| 5.3 | Titre de séjour recommandé (F-IM-05) | Pré-rempli avec données IA. Résultat : VLS-TS salarié / CST salarié |
| 5.4 | Droit au travail (F-IM-07) | VLS-TS salarié → **OUI**, obligations employeur (vérification préfecture, DPAE) |
| 5.5 | Checklist pièces (F-IM-01) | Checklist type TITRE_SALARIE/FRANCE |
| 5.6 | Dashboard | Cards titre recommandé + droit au travail OUI |

**Features testées :** F-IM-01, F-IM-05, F-IM-07, F-IA-01, F-IA-02

---

## Test 6 — Yilmaz (asile Belgique)

**Dossier :** `dossier-asile-belgique-yilmaz/` (3 fichiers)
**Workspace :** DROIT_IMMIGRATION / FRANCE (le pays workspace n'impacte pas l'immigration)

| Étape | Action | Vérification |
|-------|--------|-------------|
| 6.1 | Créer dossier, uploader 3 fichiers | OK |
| 6.2 | Analyse IA | type_procedure = DEMANDE_ASILE |
| 6.3 | Titre recommandé (F-IM-05) | Attestation d'immatriculation |
| 6.4 | Recours (F-IM-06) | Type RECOURS_CGRA, délai **15 jours**, date limite calculée, avertissement si dépassé |
| 6.5 | Droit au travail (F-IM-07) | Attestation → **CONDITIONNEL** (après 4 mois) |
| 6.6 | Dashboard | Card recours avec deadline + droit travail conditionnel |

**Features testées :** F-IM-05, F-IM-06, F-IM-07, F-IA-01, F-IA-02

---

## Test 7 — Chen (regroupement familial FR)

**Dossier :** `dossier-regroupement-familial-chen/` (3 fichiers)
**Workspace :** DROIT_IMMIGRATION / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 7.1 | Créer dossier, uploader 3 fichiers | OK |
| 7.2 | Titre recommandé (F-IM-05) | Motif FAMILLE → CST vie privée et familiale |
| 7.3 | Droit au travail (F-IM-07) | CST VPF → **OUI**, plein droit |
| 7.4 | Dashboard | Card titre + droit travail OUI (vert) |

**Features testées :** F-IM-05, F-IM-07, F-IA-02

---

## Test 8 — Kowalski (permis unique BE)

**Dossier :** `dossier-permis-unique-kowalski/` (3 fichiers)
**Workspace :** DROIT_IMMIGRATION / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 8.1 | Créer dossier, uploader 3 fichiers | OK |
| 8.2 | Titre recommandé (F-IM-05) | UE → libre circulation si court séjour, carte B si long séjour |
| 8.3 | Recours (F-IM-06) | RECOURS_CCE, délai 30j, juridiction CCE Bruxelles |
| 8.4 | Droit au travail (F-IM-07) | Carte B → **OUI** |
| 8.5 | Dashboard | Cards recours + droit travail |

**Features testées :** F-IM-05, F-IM-06, F-IM-07, F-IA-02

---

## Test 9 — Moreau (divorce amiable FR)

**Dossier :** `dossier-divorce-amiable-moreau/` (4 fichiers)
**Workspace :** DROIT_FAMILLE / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 9.1 | Créer dossier, uploader 4 fichiers | OK |
| 9.2 | Analyse IA | pension_alimentaire_data, prestation_compensatoire_data, liquidation extraits |
| 9.3 | Partage immobilier (F-FA-05) | 350K€, prêt 120K€, 50/50, divorce → droit 1.1% = 2530€, tableau complet |
| 9.4 | Calendrier garde (F-FA-06) | Résidence alternée FR → 182/183 jours |
| 9.5 | Checklist divorce (F-FA-07) | 7 étapes FR, 9 pièces FR, progression 0% → cocher → progression augmente |
| 9.6 | Dashboard | Cards soulte + garde + divorce progression + risk score |

**Features testées :** F-FA-05, F-FA-06, F-FA-07, F-IA-01, F-IA-02

---

## Test 10 — Dubois (divorce contentieux FR)

**Dossier :** `dossier-divorce-contentieux-dubois/` (3 fichiers)
**Workspace :** DROIT_FAMILLE / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 10.1 | Créer dossier, uploader 3 fichiers | OK |
| 10.2 | Partage immobilier (F-FA-05) | 280K€, prêt 90K€, 60/40 |
| 10.3 | Calendrier garde (F-FA-06) | DVH classique FR → 249/116 jours |
| 10.4 | Pension alimentaire + prestation compensatoire (existants) | Revenus 5200€ vs 1800€ |
| 10.5 | Dashboard | Cards partage + garde + pension |

**Features testées :** F-FA-05, F-FA-06, F-IA-02

---

## Test 11 — Vermeersch (divorce Belgique)

**Dossier :** `dossier-divorce-belgique-vermeersch/` (3 fichiers)
**Workspace :** DROIT_FAMILLE / BELGIQUE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 11.1 | Créer dossier, uploader 3 fichiers | OK |
| 11.2 | Partage immobilier (F-FA-05) | 420K€, prêt 150K€, 50/50, divorce BE → droit **1%** |
| 11.3 | Calendrier garde (F-FA-06) | Hébergement égalitaire BE → 182/183 |
| 11.4 | Checklist divorce (F-FA-07) | **6 étapes BE** (requête conjointe, comparution, jugement...) |
| 11.5 | Dashboard | Cards soulte BE + garde + divorce |

**Features testées :** F-FA-05, F-FA-06, F-FA-07, F-IA-02

---

## Test 12 — De Smet (garde seule BE)

**Dossier :** `dossier-garde-seule-desmet/` (2 fichiers)
**Workspace :** DROIT_FAMILLE / BELGIQUE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 12.1 | Créer dossier, uploader 2 fichiers | OK |
| 12.2 | Calendrier garde (F-FA-06) | Hébergement secondaire BE → 249/116 |
| 12.3 | Pension alimentaire | Revenus 2900€ vs 2100€ → montant calculé |
| 12.4 | Dashboard | Cards garde + pension |

**Features testées :** F-FA-06, F-IA-02

---

## Tests transversaux (sur chaque dossier)

| Test | Vérification |
|------|-------------|
| **Référentiels admin** (F-REF-01) | Aller dans /referentials → les 9 nouveaux types sont visibles et éditables |
| **Édition référentiel** | Modifier une valeur → validation IA se déclenche (warning ou saved) |
| **Pré-remplissage IA** (F-IA-01) | Après analyse, les formulaires outils sont pré-remplis (pas vides) |
| **Dashboard** (F-IA-02) | En haut du dossier, cards adaptatives selon les outils utilisés |
| **Isolation workspace** | Un dossier d'un workspace n'est pas visible depuis un autre |
| **Upsert** | Modifier les paramètres d'un outil et relancer → le résultat précédent est remplacé |
| **GET existant** | Recharger la page → l'outil affiche le dernier résultat sauvegardé |

---

## Matrice de couverture

| Feature | Test 1 | Test 2 | Test 3 | Test 4 | Test 5 | Test 6 | Test 7 | Test 8 | Test 9 | Test 10 | Test 11 | Test 12 |
|---------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
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
| F-IA-01 Pré-remplissage | ✓ | ✓ | | | ✓ | ✓ | | | ✓ | | | |
| F-IA-02 Dashboard | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| F-REF-01 Référentiels | transversal | transversal | transversal | transversal | transversal | transversal | transversal | transversal | transversal | transversal | transversal | transversal |
