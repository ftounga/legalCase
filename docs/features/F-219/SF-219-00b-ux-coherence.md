# F-219 — Cadrage cohérence écran (étape 0 bis)

## Verdict : GO avec ajustements

## Intention métier + comportement visible attendu

32 nouveaux outils décisionnels Travail BE-only **priorité P3 — spécificité belge forte** s'ajoutent au **panneau outils décisionnels** (onglet Décision du détail dossier) — visibles **uniquement pour les workspaces `country=BELGIQUE` / `legal_domain=DROIT_DU_TRAVAIL`**, masqués pour les workspaces FR. Chacun ouvre une `*-section.component` (formulaire + verdict), pattern canonique F-IA-04, **ALWAYS_ON priority croissante 119→150** (pattern F-213 — pas de CONTEXTUAL en V1, faute de flags DB existants pour les situations spécifiques P3).

## Rappel verdict feature-coherence-challenger (étape 0)

**GO** — toutes les briques d'infrastructure sont matures (F-207 + F-213 terminées). Panneau, gate `workspaceCountry`, pattern F-213 backend/frontend autonome, validation F-IA-03, `TOOL_REGISTRY`, pattern `*-analyses`, `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS` — tous opérationnels. Effort sur la substance juridique P3 BE-only et la maîtrise de la charge écran (32 outils ajoutés à un panel déjà chargé de 20+ outils BE Travail).

---

## Parcours écran réel de l'avocat BE — scénarios P3

Source : `docs/business/parcours-ecran-dossier.md` (enrichi par F-207 passage 6 + F-213 passage 7) + audit BE travail §3.

**Scénario licenciement RCC (le plus fréquent en P3)**

1. L'avocat BE ouvre un dossier de licenciement → écran **détail du dossier**, 4 onglets.
2. Onglet **Dossier** : contrat de travail uploadé → date de signature, âge, ancienneté, secteur d'activité détectés par l'IA.
3. Onglet **Analyse** : pipeline IA — détection `ageTravailleur` (calculé), `secteurActivite`, `ancienneteAnnees`, `dateRupture`, `motifRupture`, `commissionParitaire`.
4. Onglet **Décision** → panneau outils décisionnels BE : les 32 nouveaux outils F-219 apparaissent en **continuation de la séquence F-207 + F-213** (8 outils P1 en tête, 10 outils P2 ensuite, 32 outils P3 à la suite).
5. Si client 58+ ans métier lourd : l'avocat ouvre `rcc-be-metiers-lourds` OU `rcc-be-longue-carriere` (selon carrière).
6. Si entreprise en difficulté : `rcc-be-entreprise-difficulte`.
7. Vérification cumul allocations : `cumul-rcc-allocations`.
8. Si préavis ≥ 30 sem : `outplacement-be-general-30sem`.
9. Si licenciement collectif : `licenciement-be-collectif-renault`.
10. Si fermeture : `licenciement-be-fermeture-entreprise`.
11. Si transfert : `transfert-entreprise-cct-32bis`.
12. Refresh **dashboard décisionnel** (F-IA-02) → agrégation verdicts.
13. **Génération projet de conclusions** (F-98) — état terminal inchangé.

**Scénario statut particulier (flexi-job / étudiant / intérim / télétravail)**

1-4. Identique au scénario précédent.
5. L'avocat ouvre `flexi-job-be` OU `etudiant-jobiste-be` OU `interim-be-cct-322` selon le statut détecté.
6. Pour les intérimaires : `interim-be-indemnite-fin-mission`.
7. Pour les télétravailleurs : `teletravail-be-cct-85-149`.
8. Pour les clauses d'écolage : `clause-ecolage-be`.

**Scénario deal pour l'emploi 2022**

1-4. Identique.
5. Demande semaine 4 jours : `semaine-4-jours-be`.
6. Litige sur droit déconnexion : `droit-deconnexion-be`.

**Scénario rémunération complexe**

1-4. Identique.
5. Litige pécule vacances : `pecule-vacances-be`.
6. Litige éco-chèques / chèques-repas : `eco-cheques-cheques-repas-be`.
7. Litige égalité H/F : `egalite-femmes-hommes-be`.

**Scénario discrimination handicap / RPS**

1-4. Identique.
5. `discrimination-be-handicap-amenagement` (refus aménagements raisonnables).
6. `bien-etre-rps-conseiller-prevention` (saisine RPS).

**Scénario pénal du travail**

1-4. Identique.
5. Analyse `code-penal-social-be` niveaux 1-4.
6. Information `auditorat-travail-be` (saisine parquet spécialisé).

**Scénario travail dissimulé / faux indépendant**

1-4. Identique.
5. `travail-noir-be-dimona` sanctions DIMONA / ONSS.
6. `inastri-statut-travailleur-independant` requalification.

**Scénario AT/MP Fedris**

1-4. Identique (F-207 SF-207-04 déjà livré pour déclaration AT 8 jours).
5. Reconnaissance MP : `mp-fedris-reconnaissance` (liste fermée + système ouvert).
6. Calcul rente vs capital : `at-mp-rente-capital-be` (selon taux IPP).

**Scénario congés / représentation**

1-4. Identique.
5. Congé paternité : `conge-paternite-naissance-be` (20 jours réforme 2023).
6. Congé parental : `interruption-carriere-soins-parental` (4 mois CCT 64).
7. Délégué syndical statut : `delegue-syndical-cct-5`.
8. Élections sociales : `elections-sociales-be`.
9. Congé-éducation régionalisé : `conge-education-paye-region`.

---

## État terminal du processus

**Inchangé** — « projet de conclusions généré » (tranché par F-98). F-219 enrichit la chaîne décisionnelle **avant** la génération des conclusions, sans déplacer l'état terminal.

---

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours | Écran / zone | Statut |
|---|---|---|
| 1-3. Upload, analyse, synthèse | onglets Dossier + Analyse | ✅ inchangés |
| 4. Panneau outils décisionnels (filtre BE) | onglet Décision — `app-decisional-tools-panel` | ✅ existant — séquence F-207 (8 P1) + F-213 (10 P2) en tête |
| 5-11. Outils F-219 (32 sections P3) | onglet Décision — 32 nouvelles `*-section.component` | ❌ **manquant — apport F-219** |
| 12. Dashboard décisionnel | onglet Décision — `app-case-dashboard` | ✅ existant — agrège automatiquement |
| 13. Conclusions | onglet Décision — `app-conclusions-section` (F-98) | ✅ existant |

---

## Position candidate de la feature

Les 32 outils s'insèrent **à l'intérieur du panneau outils décisionnels** (onglet Décision), **après les 10 outils P2 de F-213** — aucun bloc primaire nouveau. Chacun est une entrée `TOOL_REGISTRY` standard (instanciation conditionnelle `workspaceCountry === 'BELGIQUE'`). Le panneau est conçu pour absorber N outils (pattern F-166 / F-IA-04). À l'issue de F-219, le panneau BE Travail comptera **~52 outils décisionnels** (12 existants pré-F-207 + 8 F-207 + 10 F-213 + 32 F-219 + 2 anciens F-DT-29/F-132 = 64 outils dont une majorité ALWAYS_ON visibles automatiquement).

---

## Challenge placement

L'écran cible (onglet **Décision** → `app-decisional-tools-panel`) est le placement standard de tous les outils décisionnels existants (Travail FR, Immigration FR/BE, Famille FR/BE, Travail BE P1+P2). Cohérent — l'avocat BE y trouve déjà ses outils F-207 / F-213. ✅ **Placement juste.**

---

## Challenge lisibilité de la séquence

⚠️ **Ajustement requis** : les 32 outils P3 doivent s'insérer **après les P1 + P2** dans un ordre métier lisible groupé par thème.

**Ordre proposé dans le TOOL_REGISTRY BE (suite F-207 + F-213)** :

_Outils P1 F-207 (déjà livrés — rappel séquence)_
1-8. `prescription-be-litige-travail` → `outplacement-be-obligatoire-45` (priorities 100→107)

_Outils P2 F-213 (déjà livrés — rappel séquence)_
9-18. `clause-non-concurrence-be` → `licenciement-be-cct109-deraisonnable` (priorities 110→118 — ALWAYS_ON)

_Outils P3 F-219 (nouveaux) — priorities 119→150, ALWAYS_ON_

**Bloc RCC variants (119→122)** — thématique cohérente "RCC élargi"
19. `rcc-be-metiers-lourds` — priority 119
20. `rcc-be-longue-carriere` — priority 120
21. `rcc-be-entreprise-difficulte` — priority 121
22. `cumul-rcc-allocations` — priority 122

**Bloc outplacement / collectif / transfert (123→126)** — thématique "rupture collective"
23. `outplacement-be-general-30sem` — priority 123
24. `licenciement-be-collectif-renault` — priority 124
25. `licenciement-be-fermeture-entreprise` — priority 125
26. `transfert-entreprise-cct-32bis` — priority 126

**Bloc représentation / formation (127→129)** — thématique "représentation du personnel et formation"
27. `elections-sociales-be` — priority 127
28. `delegue-syndical-cct-5` — priority 128
29. `conge-education-paye-region` — priority 129

**Bloc statuts particuliers (130→135)** — thématique "statuts hors CDI standard"
30. `flexi-job-be` — priority 130
31. `etudiant-jobiste-be` — priority 131
32. `interim-be-cct-322` — priority 132
33. `interim-be-indemnite-fin-mission` — priority 133
34. `teletravail-be-cct-85-149` — priority 134
35. `clause-ecolage-be` — priority 135

**Bloc deal pour l'emploi 2022 (136→137)**
36. `semaine-4-jours-be` — priority 136
37. `droit-deconnexion-be` — priority 137

**Bloc rémunération (138→140)**
38. `pecule-vacances-be` — priority 138
39. `eco-cheques-cheques-repas-be` — priority 139
40. `egalite-femmes-hommes-be` — priority 140

**Bloc discrimination / RPS (141→143)**
41. `discrimination-be-handicap-amenagement` — priority 141
42. `bien-etre-rps-conseiller-prevention` — priority 142
43. `conge-paternite-naissance-be` — priority 143 (placement transitoire — pourrait migrer dans bloc congés ; gardé proche RPS car protections santé/famille)

**Bloc pénal / dissimulation / Fedris MP (144→148)**
44. `code-penal-social-be` — priority 144
45. `auditorat-travail-be` — priority 145
46. `travail-noir-be-dimona` — priority 146
47. `inastri-statut-travailleur-independant` — priority 147
48. `mp-fedris-reconnaissance` — priority 148
49. `at-mp-rente-capital-be` — priority 149

**Bloc congés (150)**
50. `interruption-carriere-soins-parental` — priority 150

**Justification de l'ordre par blocs thématiques** : avec 32 outils ajoutés à un panel déjà très chargé (18 outils BE Travail P1+P2 + ~5 outils F-DT-XX BE existants), l'avocat doit pouvoir naviguer par thème. L'ordre proposé regroupe les outils par scénario métier (RCC, rupture collective, statuts particuliers, etc.) — cohérent avec l'usage avocat.

**Mise en œuvre** : `TOOL_REGISTRY` BE complété avec les 32 nouvelles entrées dans l'ordre ci-dessus. Chaque mini-spec frontend précisera la `priority` exacte attribuée à son outil.

---

## Challenge charge écran

⚠️ **Charge écran significative — ajustement recommandé**.

Onglet **Décision** porte 3 blocs primaires (`app-decisional-tools-panel`, `app-case-dashboard`, `app-conclusions-section`) — seuil ~3 respecté **au niveau bloc primaire**.

**Mais le panel décisionnel lui-même atteint ~50 outils BE Travail à l'issue de F-219**. Bien que tous ALWAYS_ON, l'avocat n'utilise concrètement que ~5-10 outils par dossier (selon scénario métier).

**Densité réelle par dossier typique** :
- Licenciement RCC standard : prescription + C4 + statut unique préavis (ou Claeys) + RCC métiers lourds + cumul + outplacement général + projet conclusions. **~7 outils ouverts**.
- Licenciement standard sans circonstance : prescription + C4 + statut unique + CCT 109 + projet. **~4 outils ouverts**.
- Cas flexi-job : prescription + flexi-job + rappel salaire + projet. **~4 outils ouverts**.
- Cas pénal du travail : prescription + code pénal social + auditorat + projet. **~4 outils ouverts**.

**Charge réelle utilisateur** : 4-10 outils ouverts par dossier — densité acceptable.

**Risque visuel** : la liste statique du panneau devient longue (~50 entrées BE). Recommandation pour F-219 :
- **Aucun changement structurel obligatoire en V1 F-219** — l'ordre par blocs thématiques (cf. §lisibilité) atténue la surcharge.
- **À considérer en V2 / feature ultérieure** : section repliable par thème (RCC, statuts particuliers, deal pour l'emploi, pénal, Fedris, congés) — ce serait une SF F-IA-04 transverse dédiée, pas un blocker F-219.

**Aucun dépassement de charge écran rédhibitoire — GO avec note d'attention V2.**

---

## Challenge état final / continuité

Après le verdict de chaque outil :
- Refresh dashboard décisionnel (F-IA-02) — `CaseDashboardRefreshService.triggerRefresh()` dans `next:` du POST (pattern SF-IA-02-03). ✅
- Verdicts enrichissent le projet de conclusions (F-98). ✅
- `KNOWN_NO_DASHBOARD_TILE_IDS` mis à jour pour chaque tool_id F-219 — pas de tile dashboard en V1 F-219 (pattern F-213). ✅

Continuité préservée — chaque outil mène vers la suite du parcours.

---

## Ajustements requis

1. **Ordre TOOL_REGISTRY BE** — séquence P1 (F-207) + P2 (F-213) + P3 (F-219) en blocs thématiques avec priority croissante 119→150 (cf. §lisibilité).
2. **`workspaceCountry === 'BELGIQUE'` strict** — test isolation France obligatoire par SF frontend.
3. **ALWAYS_ON par défaut** — pattern F-213 stabilisé. CONTEXTUAL uniquement si un flag DB existe pour une situation rare (à arbitrer mini-spec par mini-spec — pas attendu en F-219 V1).
4. **Pas d'agrégation visuelle interne aux outils** — chaque outil est une entrée TOOL_REGISTRY distincte (ex. RCC = 3 outils, pas 1 super-outil). Audit §4.4 + invariant CLAUDE.md "un outil = une situation métier".
5. **Pré-remplissage IA limité aux champs déjà extraits** par le modèle de base (pattern F-213 — pas d'enrichissement IA croisé cross-domain BE en V1 F-219). Champs P3 spécifiques (age, secteur, IPP, région, etc.) en saisie manuelle V1.
6. **`KNOWN_NO_DASHBOARD_TILE_IDS` mis à jour systématiquement** par chaque SF frontend `XXb` (sinon master-red — règle absolue depuis F-213).
7. **Note V2** : un éventuel découpage en sections repliables thématiques du panneau BE Travail sera traité dans une SF F-IA-04 dédiée ultérieurement — pas en F-219.

---

## Invariants anti-surcharge pour les mini-specs

- **Zéro bloc primaire nouveau** — enrichissement du contenu interne du panneau Décision uniquement.
- **`workspaceCountry === 'BELGIQUE'` strict** — pas de fuite FR. Test isolation `country=FRANCE` obligatoire.
- **Ordre du panneau respecte la séquence métier par blocs thématiques** (P1 → P2 → P3 RCC → P3 collectif → P3 statuts particuliers → P3 deal emploi → P3 rémunération → P3 discrimination/RPS → P3 pénal/Fedris → P3 congés).
- **ALWAYS_ON priority croissante 119→150** — pas de CONTEXTUAL en V1 F-219 (faute de flags DB existants).
- **`KNOWN_NO_DASHBOARD_TILE_IDS` mis à jour** dans chaque SF frontend `XXb` (`DashboardTileToolIdIntegrityIT`).
- **`KNOWN_FRONTEND_TOOL_IDS` mis à jour** dans chaque SF frontend `XXb` (`DecisionToolVisibilityIntegrityIT`) — sinon panel orphelin.
- **Critères F-IA-03 `BE_*` distincts** des codes FR équivalents — mais V1 F-219 = pattern F-213 = pas de modif `KNOWN_FRONTEND_CRITERE_CODES`.
- **`getPrefillCount(input)` obligatoire** — parité stricte avec `prefillFromAi()` runtime (sur les 1-4 champs pré-fillables).
- **Régionalisation interne pour outil 11** (`conge-education-paye-region`) — input `region` enum WBR/FLA/BXL, pas 3 outils distincts.

---

## MAJ apportée au parcours écran de référence

`docs/business/parcours-ecran-dossier.md` devra être enrichi lors du merge F-219 : 8ᵉ passage — ajout du flux outils décisionnels Travail BE P3 (32 outils par blocs thématiques RCC / collectif / représentation / statuts particuliers / deal emploi / rémunération / discrimination / pénal / Fedris / congés), invariant « ordre TOOL_REGISTRY respecte séquence P1 → P2 → P3 », visibility ALWAYS_ON priority 119→150, note V2 sur découpage repliable thématique éventuel.

---

## Décision finale

**GO avec ajustements.** Placement correct (panneau Décision standard BE-only, suite de F-207 + F-213). Charge écran significative (50+ entrées BE Travail) mais maîtrisée par l'ordre thématique et la densité réelle utilisateur (4-10 outils ouverts par dossier). Lisibilité séquence requise : `TOOL_REGISTRY` BE ordonné P1 → P2 → P3 par blocs thématiques. Les 7 ajustements ci-dessus sont à intégrer dans chaque mini-spec. Note V2 : un découpage repliable par thème pourra être considéré dans une SF F-IA-04 dédiée — hors périmètre F-219.
