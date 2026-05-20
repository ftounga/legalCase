# F-212 — Cadrage cohérence (étape 0)

> Produit par la skill `ai-skills/feature-coherence-challenger.md`. Étape 0 du cycle de gouvernance, avant la mini-spec.
> Feature : **F-212 — P2 Travail FR — ~22 outils fréquence haute**.
> Date : 2026-05-20.

## Verdict : 🟢 GO avec ajustements

Les ~22 outils P2 s'insèrent proprement dans l'architecture existante des outils décisionnels (pipeline IA → détection F-IA-04 → section décisionnelle → dashboard F-167 → conclusions F-98). Toutes les briques amont sont livrées par F-206/F-205. Les flags de détection IA nécessaires sont déjà livrés par F-205 (23 flags, PR #914). **Ajustements** : (1) deux outils P2 partiellement couverts par des outils existants (F-DT-33 pour faute inexcusable, F-DT-22 pour CDD requalification) méritent un outil dédié distinct ; (2) l'outil `RTT / monétisation` est reclassé P3 (dispositif potentiellement expiré — à vérifier) et exclu du périmètre P2 ; (3) le découpage 1 SF combinée vs 2 SF (backend + frontend) suit le critère ≥ 1 nouveau champ IA.

## Intention métier (1 phrase)

Donner à l'avocat en droit du travail FR (côté salarié ou employeur) **22 outils décisionnels pour les situations rencontrées plusieurs fois par mois** dans tout cabinet travailliste — faute grave/lourde, forfait jours, transfert d'entreprise, CSP/CRP, faute inexcusable employeur, modification/mutation du contrat, télétravail, CDD requalification, etc. — situations non couvertes par les 28 outils existants.

## Workflow métier réel de l'utilisateur cible

**Source** : audit exhaustif `docs/features/F-191/audit-travail-fr-exhaustif.md` §3 (Tableau B, outils MANQUE P2) + pratique standard de l'avocat travailliste FR + signal terrain Renversez 13/05/2026.

Workflow d'un dossier du travail côté salarié ou employeur — situations P2 :

1. Le salarié ou l'employeur consulte l'avocat sur une situation récurrente mais non-urgente procéduralement : licenciement pour faute grave, clause forfait jours contestée, refus de mutation, requalification CDD en CDI, transfert d'entreprise (L. 1224-1), conformité CSP, faute inexcusable AT/MP, litige télétravail, etc.
2. L'avocat collecte les pièces : contrat de travail, bulletins de paie, courriers, accord d'entreprise, mise en demeure.
3. L'avocat **qualifie la situation** et identifie le régime juridique applicable.
4. L'avocat **évalue les chances de succès, les risques et chiffre les demandes** — c'est le cœur décisionnel. Par exemple : faute grave = pas d'indemnité de préavis ni d'indemnité légale → impact financier direct ; forfait jours nul → rappel d'heures supplémentaires sur 3 ans ; L. 1224-1 violé → nullité des licenciements opérés.
5. L'avocat **calcule les délais** : prescription 3 ans pour rappel salarial, 12 mois pour action licenciement.
6. L'avocat **arbitre la stratégie** : contester, négocier, aller au CPH.
7. L'avocat **rédige les actes** : courrier, conclusions, requête CPH.
8. L'avocat **chiffre les demandes** : rappels salariaux, indemnités, dommages-intérêts.

## Cartographie features actuelles ↔ workflow

| Étape workflow métier | Feature(s) LegalCase | Statut |
|---|---|---|
| 1-2. Réception dossier + collecte des pièces | Bloc Documents F-1→F-12 | ✅ Livrée |
| 3. Qualification de la situation | F-10 Analyse IA + F-IA-04 affichage conditionnel | ✅ Livrée |
| 4. **Évaluation décisionnelle P2** | **F-212 (les ~22 outils challengés)** — trou actuel | ❌ C'est le trou que F-212 comble |
| 4 bis. Pré-remplissage IA des champs | F-IA-01 + F-246 invariant tous-champs | ✅ Livrée |
| 4 ter. Contrôle cohérence saisies | F-IA-03 validation de cohérence | ✅ Livrée |
| 5. Délais procéduraux | F-69 Suivi des délais légaux + F-DT-03 prescription | ✅ Livrée |
| 6. Arbitrage stratégique | F-176 + F-IA-02 + F-167 | ✅ Livrée |
| 7. Rédaction des actes | F-98 Génération conclusions/courrier (53/53 SF) | ✅ Livrée |
| 8. Chiffrage des demandes | F-DT-09/25/26 indemnités | ✅ Livrée |

## Position de la nouvelle feature

F-212 s'insère **exactement à l'étape 4** du workflow. Les 22 outils sont 22 **sections décisionnelles** de l'espace décisionnel du dossier, affichées conditionnellement par F-IA-04 sur détection IA de la situation. Tous les flags de détection nécessaires ont été livrés par F-205 (PR #914).

## Challenge amont

| Brique amont nécessaire | Couverture | Verdict |
|---|---|---|
| Import dossier + upload documents | Bloc Documents F-1→F-12 — ✅ livré | OK |
| Analyse IA du dossier | F-10 — ✅ livré | OK |
| Pré-remplissage IA des champs | F-IA-01 — ✅ livré ; invariant F-246 — ✅ livré | OK |
| Détection IA pour affichage conditionnel | F-IA-04 — ✅ livré. Flags requis : `motif_faute_grave_pressenti`, `forfait_jours_detecte`, `transfert_entreprise_detecte`, `csp_propose`, `faute_inexcusable_envisagee`, `modification_contrat_refusee`, `mutation_refusee`, `teletravail_litige_detecte`, `requalification_cdd_cdi_envisagee`, `rupture_anticipee_cdd_detectee`, `mise_a_pied_disciplinaire_detectee`, `egalite_salariale_pressentie`, `lanceur_alerte_detecte`, `conge_maternite_paternite_detecte`, `protection_rp_detecte`, `temps_partiel_requalification_envisagee`, `burnout_detecte`, `election_cse_detectee`, `pdv_rcc_envisage` — livrés par F-205 PR #914 | ✅ OK |
| Référentiel des délais procéduraux | F-69 — ✅ livré | OK |
| P1 Travail FR | F-206 — ✅ Terminée (8/8 SF, 2026-05-20) | OK |

**Aucun trou amont bloquant.**

## Challenge aval

| Étape aval | Exploite la sortie de F-212 ? | Couverture |
|---|---|---|
| 5. Délais procéduraux | Oui — délais prescription, échéances datées | F-69 ✅ |
| 6. Arbitrage stratégique | Oui — verdicts alimentent pistes stratégiques et dashboard | F-176 ✅ · F-IA-02 ✅ · F-167 ✅ |
| 7. Rédaction des actes | Oui — analyseurs produisent la matière des conclusions | F-98 ✅ |
| 8. Chiffrage | Oui — calculateurs chiffrent les rappels et indemnités | F-DT-09/25/26 ✅ |

**Aucun trou aval bloquant.**

## Périmètre retenu — sélection des ~22 outils P2

### Outils inclus (P2 validés)

| # | tool_id (audit F-191) | Situation juridique | Priorité audit | Justification inclusion |
|---|---|---|---|---|
| 1 | `F-DT-36-licenciement-faute-grave-lourde` | Faute grave / faute lourde — privation préavis + IL | P2 | Top 10 F-191 §5 ; distinct de F-DT-08 générique |
| 2 | `F-DT-50-forfait-jours-validite` | Forfait jours — validité, accord collectif, rappel HS si nul | P2 FR-only | Top 10 F-191 §5 ; très contesté Syntec |
| 3 | `F-DT-72-transfert-entreprise-l1224-1` | Transfert d'entreprise — maintien contrats L. 1224-1 | P2 | Top 10 F-191 §5 ; fréquent en M&A |
| 4 | `F-DT-44-csp-crp-conformite` | CSP/CRP conformité entreprises < 1 000 salariés | P2 FR-only | Top 10 F-191 §5 ; central licenciement éco PME |
| 5 | `F-DT-91-faute-inexcusable-employeur` | Faute inexcusable employeur — majoration rente AT/MP | P2 | Top 10 F-191 §5 ; procédure distincte de F-DT-33 |
| 6 | `F-DT-70-modification-contrat-refus` | Modification du contrat — refus salarié + conséquences | P2 FR-only | Top 10 F-191 §5 ; très fréquent |
| 7 | `F-DT-71-mutation-clause-mobilite` | Mutation — validité clause mobilité + refus | P2 | Top 10 F-191 §5 |
| 8 | `F-DT-82-teletravail-accord` | Télétravail — droit, accord, indemnité occupation | P2 FR-only | Post-COVID ; ANI 26/11/2020 |
| 9 | `F-DT-22-requalification-cdd-cdi` (nouveau outil CDD rupture anticipée) → `F-DT-43-rupture-anticipee-cdd` | Rupture anticipée CDD — sanctions L. 1243-4 | P2 | Régime sanctions distinct ; F-DT-22 couvre la requalification, pas la rupture anticipée |
| 10 | `F-DT-48-mise-a-pied-disciplinaire` | Mise à pied disciplinaire — durée, procédure, salaire | P2 | Souvent confondue avec la mise à pied conservatoire |
| 11 | `F-DT-41-demission-validite-equivoque` | Démission équivoque — requalification possible | P2 | Cas fréquent (démission sous pression, par mail) |
| 12 | `F-DT-56-egalite-salariale-femmes-hommes` | Égalité salariale F/H — index, action individuelle | P2 FR-only | Action individuelle en discrimination salariale (5 ans) |
| 13 | `F-DT-61-lanceur-alerte-protection` | Protection lanceur d'alerte (loi Waserman 2022) | P2 FR-only | Régime renforcé 2022 |
| 14 | `F-DT-64-burnout-reconnaissance` | Burn-out — reconnaissance maladie professionnelle hors tableau | P2 FR-only | CRRMP ; distinct de F-DT-33-at-mp |
| 15 | `F-DT-77-conge-paternite-maternite` | Congé maternité / paternité — durée, indemnisation, protection | P2 FR-only | Paternité 25 j depuis 2021 ; protection licenciement |
| 16 | `F-DT-65-elections-cse-conformite` | Élections CSE — calendrier, PAP, vote, contestation | P2 FR-only | CSE ord. 2017 ; délai contestation 15 j |
| 17 | `F-DT-49-temps-partiel-requalification` | Temps partiel — requalification en temps plein | P2 | Heures complémentaires > 1/3 ; mentions absentes |
| 18 | `F-DT-46-pdv-rcc` | Plan de départs volontaires / RCC | P2 FR-only | Procédure formaliste ; distinct du PSE |
| 19 | `F-DT-84-conciliation-cph-bca` | Phase conciliation obligatoire CPH (BCO/BCA) | P2 FR-only | Obligatoire avant bureau jugement |
| 20 | `F-DT-88-execution-jugement-cph` | Exécution forcée jugement CPH (AGS) | P2 | AGS en cas d'employeur en RJ ; très utile |
| 21 | `F-DT-104-vrp-statut` | VRP — statut, indemnité clientèle L. 7313-13 | P2 FR-only | Régime spécifique ; indemnité clientèle en plus de l'IL |
| 22 | `F-DT-108-particuliers-employeurs-cesu` | Particulier employeur (CESU, garde enfants) | P2 FR-only | Régime spécifique ; beaucoup de demandes |

**Total : 22 outils retenus.**

### Outils exclus et justification

| tool_id (audit) | Raison d'exclusion | Renvoi |
|---|---|---|
| `F-DT-43-rupture-anticipee-cdd` (nom original) | Intégré sous l'outil #9 sous ce même ID | Périmètre F-212 |
| `F-DT-51-rtt-monetisation` | Dispositif loi 16/08/2022 potentiellement expiré (à vérifier) ; P3 selon audit F-191 §3.2 | P3 → F-218 |
| `F-DT-59-harcelement-moral-procedure-interne` | Outil compliance employeur, moins fréquent côté salarié ; P2 mais P3 FR-only en pratique | P3 → F-218 |
| `F-DT-75-conges-payes-acquisition-arrets-maladie` | **Livré par F-206** (SF-206-03/04) | ✅ Couvert |
| `F-DT-42-abandon-poste-presomption-demission` | **Livré par F-206** (SF-206-01/02) | ✅ Couvert |
| `F-DT-39-prise-acte-rupture` | **Livré par F-206** (SF-206-05/06) | ✅ Couvert |
| `F-DT-40-resiliation-judiciaire-cph` | **Livré par F-206** (SF-206-07/08) | ✅ Couvert |

## Découpage backend + frontend (critère : ≥ 1 nouveau champ IA → 2 SF)

**Règle** : si l'outil introduit ≥ 1 nouveau champ dans `TravailExtractedData` qui étend le record IA → **2 SF** (backend *-backend + frontend *b-frontend). Si l'outil n'introduit aucun nouveau champ IA (outil purement calculatoire sans extraction IA) → **1 SF combinée**. Tous les outils P2 introduisent au moins 1 nouveau champ IA — découpage **2 SF systématique** → **44 SF au total**.

## Tableau récapitulatif des 22 outils

| # | Outil | SF Backend # | SF Frontend # | Flag F-205 | Source juridique |
|---|---|---|---|---|---|
| 1 | Faute grave / lourde | SF-212-01 | SF-212-02 | `motif_faute_grave_pressenti` | L. 1234-1 ; L. 1234-9 ; Cass. soc. |
| 2 | Forfait jours validité | SF-212-03 | SF-212-04 | `forfait_jours_detecte` | L. 3121-58 à L. 3121-66 ; Cass. soc. 29/06/2011 |
| 3 | Transfert entreprise L. 1224-1 | SF-212-05 | SF-212-06 | `transfert_entreprise_detecte` | L. 1224-1 |
| 4 | CSP/CRP conformité | SF-212-07 | SF-212-08 | `csp_propose` | L. 1233-65 à L. 1233-70 |
| 5 | Faute inexcusable employeur | SF-212-09 | SF-212-10 | `faute_inexcusable_envisagee` | L. 452-1 à L. 452-5 CSS ; Cass. ass. plén. 24/06/2005 |
| 6 | Modification contrat refus | SF-212-11 | SF-212-12 | `modification_contrat_refusee` | L. 1222-6 ; jurisprudence |
| 7 | Mutation clause mobilité | SF-212-13 | SF-212-14 | `mutation_refusee` | L. 1221-1 ; jurisprudence |
| 8 | Télétravail accord | SF-212-15 | SF-212-16 | `teletravail_litige_detecte` | L. 1222-9 à L. 1222-11 ; ANI 26/11/2020 |
| 9 | Rupture anticipée CDD | SF-212-17 | SF-212-18 | `rupture_anticipee_cdd_detectee` | L. 1243-1 à L. 1243-4 |
| 10 | Mise à pied disciplinaire | SF-212-19 | SF-212-20 | `mise_a_pied_disciplinaire_detectee` | L. 1331-1 ; jurisprudence |
| 11 | Démission équivoque | SF-212-21 | SF-212-22 | `demission_equivoque_pressentie` | L. 1237-1 ; jurisprudence |
| 12 | Égalité salariale F/H | SF-212-23 | SF-212-24 | `egalite_salariale_pressentie` | L. 1142-7 à L. 1142-10 ; L. 1144-1 |
| 13 | Lanceur d'alerte protection | SF-212-25 | SF-212-26 | `lanceur_alerte_detecte` | L. 1132-3-3 ; loi Waserman 21/03/2022 |
| 14 | Burn-out reconnaissance MP | SF-212-27 | SF-212-28 | `burnout_detecte` | L. 461-1 CSS ; tableau 57 ; CRRMP |
| 15 | Congé maternité / paternité | SF-212-29 | SF-212-30 | `conge_maternite_paternite_detecte` | L. 1225-1+ ; L. 1225-35+ ; CSS |
| 16 | Élections CSE conformité | SF-212-31 | SF-212-32 | `election_cse_detectee` | L. 2314-1 à L. 2314-37 |
| 17 | Temps partiel requalification | SF-212-33 | SF-212-34 | `temps_partiel_requalification_envisagee` | L. 3123-6 ; L. 3123-9 |
| 18 | PDV / RCC conformité | SF-212-35 | SF-212-36 | `pdv_rcc_envisage` | L. 1237-17 à L. 1237-19-14 |
| 19 | Conciliation CPH (BCO/BCA) | SF-212-37 | SF-212-38 | *(nouveau flag `conciliation_cph_envisagee`)* | R. 1454-7 à R. 1454-12 |
| 20 | Exécution jugement CPH (AGS) | SF-212-39 | SF-212-40 | *(nouveau flag `execution_jugement_cph_detectee`)* | art. 514 CPC ; L. 3253-6+ |
| 21 | VRP statut + indemnité clientèle | SF-212-41 | SF-212-42 | `statut_vrp_detecte` | L. 7311-1 à L. 7313-18 |
| 22 | Particulier employeur (CESU) | SF-212-43 | SF-212-44 | `particulier_employeur_detecte` | CCN salariés du particulier employeur |

**Note outils 19 et 20** : les flags `conciliation_cph_envisagee` et `execution_jugement_cph_detectee` ne figurent pas explicitement dans la liste F-205. Leur ajout est intégré au périmètre des SF backend correspondantes (SF-212-37 et SF-212-39) — extension `TravailExtractedData` + prompt, dans le pattern F-205.

## Préoccupation transversale « Outil décisionnel métier »

Cochée pour tous les 22 outils. Composants impactés au niveau feature :
- `CaseAnalysisResponse.java` — record `TravailExtractedData` : extensions champs pour chaque outil (≥ 1 champ / outil).
- `LegalDomainPromptBuilder.java` — bloc `DROIT_DU_TRAVAIL` : extension prompts extraction + `critereCode`.
- `DecisionToolVisibilityService.java` — `extractDetectedSituations()` : consomme les flags.
- Migrations Liquibase : 22 nouvelles tables d'analyses + 22 seeds `decision_tool_visibility_rules`.
- `TOOL_REGISTRY` (`decisional-tools-panel.component.ts`) : 22 nouvelles entrées.
- `CaseFileDashboardService.java` : 22 mappers `DashboardTile`.
- `DecisionToolVisibilityIntegrityIT` : 22 nouveaux `tool_id` dans `KNOWN_FRONTEND_TOOL_IDS`.

## STOPs / pré-requis

**Aucun STOP.** F-206 Terminée (8/8 SF) et F-205 Terminée (23 flags) — tous les pré-requis sont satisfaits.

## Invariants anti-gadget pour la mini-spec

1. **Un outil = une situation métier** — les 22 outils restent distincts, pas de fusion même si les situations sont proches (ex. faute grave et faute lourde traitées dans le **même** outil `F-DT-36` car situation juridique identique avec gradation, mais distinct de F-DT-08 générique).
2. **Pré-remplissage IA total (invariant F-246)** — tout champ saisissable de chaque outil est pré-rempli par l'IA. Aucune exception hors information absente des pièces.
3. **`critereCode` F-IA-03 émis par le prompt (invariant F-250)** — tout critère modifiable doit voir son `critereCode` émis par le prompt LLM.
4. **Affichage conditionnel réel (F-IA-04)** — chaque outil est `CONTEXTUAL`, jamais `ALWAYS_ON`.
5. **Tile du dashboard agrégé (F-167)** — chaque outil livre son mapper `DashboardTile`.
6. **Verdict tranché et sourcé** — chaque analyseur rend un verdict actionnable assorti de sources (F-93).
7. **Isolation workspace** — `caseFileId` appartient au workspace de l'utilisateur authentifié (404 si non).
8. **Country gate** — outils FR-only : gate `country=FRANCE` côté backend (422) et gate `isFrance` côté frontend (bannière, pas masquage silencieux).
9. **`KNOWN_FRONTEND_TOOL_IDS`** — chaque nouveau `tool_id` est ajouté au test d'intégrité `DecisionToolVisibilityIntegrityIT` dans la même SF backend (garde-fou F-164).

## Décision finale

🟢 **GO avec ajustements.** F-212 comble 22 trous fonctionnels à l'étape 4 du workflow (évaluation décisionnelle P2), sur les situations les plus fréquentes de tout cabinet travailliste FR. Toutes les briques amont et aval sont livrées. Les 22 invariants anti-gadget ci-dessus sont à intégrer à chaque mini-spec.

**Conséquence PRODUCT_SPEC** : F-212 passe de « À planifier » à « À faire ». Étape suivante : 0 bis — cadrage cohérence écran (les 22 outils ajoutent des sections à l'espace décisionnel du dossier).
