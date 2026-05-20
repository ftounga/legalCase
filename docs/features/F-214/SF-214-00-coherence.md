# F-214 — Cadrage cohérence (étape 0)

**Skill** : `ai-skills/feature-coherence-challenger.md`
**Date** : 2026-05-20
**Auteur** : AI-Agent (mode autonome)

---

## Verdict : GO avec ajustements

---

## Intention métier (1 phrase)

Outiller les avocats en droit des étrangers avec ~22 outils décisionnels de fréquence haute (P2) couvrant les situations les plus courantes de leur pratique quotidienne : étranger malade L. 425-9, regroupement familial, VPF liens personnels L. 423-23, validation VLS-TS OFII, OQTF catégories L. 611-1, naturalisation recours, AES calcul présence prouvée, UE/EEE, ANEF pannes, récépissé/attestation, renouvellement 2 mois avant, assignation résidence, ITF judiciaire, appel CAA/CE, MNA évaluation âge, carte résident L. 426-1, demande OFPRA introduction, AJ CNDA, victime traite L. 425-1, retrait titre fraude, régularisation stratégie, autorisation travail employeur.

---

## Rappel de la chaîne de features amont

- **F-201** ✅ Terminée (2/2 SF, PR #908, 2026-05-10) — F-166 généralisée Immigration FR : 9 flags booléens IA dans `ImmigrationExtractedData`, 10 outils ALWAYS_ON → CONTEXTUAL. Base essentielle pour les nouveaux outils P2 qui seront eux aussi CONTEXTUAL.
- **F-208** ✅ Terminée (8/8 SF, PR #915 + #941, 2026-05-10/11) — P1 Immigration FR : 4 outils délais courts livrés (JLD rétention, Dublin recours, CRRV refus visa, victime violences L. 425-6). Ces 4 outils P1 constituent le modèle canonique technique pour F-214.
- **F-235** ✅ Terminée (2/2 SF, PR #923, 2026-05-10) — Extension matching CONTEXTUAL champs texte (régime algérien). Débloque le pattern trigger texte (nationalite='Algérienne') — utilisable dans F-214.
- **F-234** ✅ Terminée (2/2 SF, PR #924, 2026-05-10) — Builder pattern ImmigrationExtractedData. Essentiel pour les SF F-214 qui ajouteront des champs.
- **F-246** ✅ (salve en cours 2026-05-19) — Pré-remplissage IA lot vague 2 : ajoute des champs dans `ImmigrationExtractedData` (SF-246-04/17/18/19/20). Plusieurs champs utiles à F-214 sont déjà livrés ou planifiés dans cette vague.

---

## Workflow métier réel de l'utilisateur cible (avocat en droit des étrangers)

Source : signal terrain (démo Marjolaine RENVERSEZ 13/05 — `docs/memory/project_renversez_post_demo_13_05.md` + pratique standard avocat droit des étrangers, France uniquement).

1. **Primo-consultation** : l'avocat reçoit le client étranger, identifie sa situation (nationalité, titre actuel, situation familiale, état de santé, situation irrégulière ou non). ⚠ Hypothèse standard.
2. **Collecte des pièces** : passeport, titre ou récépissé, preuves de présence, contrats, actes d'état civil, documents médicaux selon le cas. [Couvert par F-IM-01 checklist pièces + F-IM-21 critères binaires ✅]
3. **Orientation vers le bon titre** : quelle voie de régularisation, quel titre demander, quel renouvellement. [Couvert F-IM-05 arbre décisionnel ✅]
4. **Analyse des délais critiques** : date d'expiration du titre, délai de dépôt renouvellement (2 mois avant), délai validation VLS-TS (3 mois post-arrivée). [F-IM-01/F-IM-21 partiellement ; **outil dédié renouvellement manque — P2** ; VLS-TS manque — P1 à traiter dans F-214]
5. **Vérification de la régularité administrative** : le titre est-il valide ? Récépissé ou attestation de prolongation ? Droit au travail attaché ? [F-IM-07 ✅ ; récépissé/attestation distinction : **manque — P2**]
6. **Démarches ANEF en ligne** : dépôt de la demande sur l'ANEF. Que faire en cas de panne ou de refus de la plateforme ? [ANEF procédure pannes : **manque — P2**]
7. **Traitement du cas de figure spécifique** : selon le motif (étranger malade, regroupement familial, VPF liens personnels, AES, naturalisation, victime traite, situation irrégulière, mineur), l'avocat prépare un dossier spécialisé. [**Outils P2 ciblés de F-214**]
8. **Préparation de la demande** : rédaction, calcul du critère ressources (regroupement familial, AES), calcul des années de présence (AES multi-motifs), checklist pièces par voie.
9. **Dépôt en préfecture / OFII / OFPRA** : dépôt dématérialisé ANEF ou physique.
10. **Traitement des décisions administratives** : refus → recours selon la voie (TA, CRRV, TJ, CAA, CE). [F-IM-06 recours généraliste ✅ ; recours naturalisation TJ + TA : **manque — P2** ; appel CAA + cassation CE : **manque — P2** ; assignation résidence : **manque — P2** ; ITF judiciaire : **manque — P2**]
11. **Droit des étrangers en situation familiale** : regroupement familial, conjoint de Français, parent d'enfant français, carte de résident. [F-IM-21 partiellement ✅ ; regroupement familial outil dédié : **manque — P2** ; carte résident : **manque — P2**]
12. **Situation des mineurs** : MNA évaluation d'âge, tutelle JE. [F-IM-19 ✅ ; MNA évaluation âge : **manque — P2**]
13. **Naturalisation** : vérification éligibilité 6 voies, recours si refus. [F-IM-13 ✅ ; recours refus naturalisation TJ + TA : **manque — P2**]
14. **Clôture du dossier / suivi** : l'avocat documente la stratégie retenue, génère les conclusions si procédure contentieuse.

---

## Cartographie features actuelles ↔ workflow

| Étape workflow métier | Feature(s) LegalCase | Statut |
|---|---|---|
| 1. Primo-consultation identification | F-IM-05 arbre décisionnel titre | ✅ Livrée |
| 2. Collecte pièces | F-IM-01 checklist pièces + F-IM-21 critères binaires | ✅ Livrée |
| 3. Orientation vers le bon titre | F-IM-05 arbre décisionnel titre | ✅ Livrée |
| 4. Délai renouvellement 2 mois avant | F-IM-01/F-IM-21 partiel | 🟡 P2 — F-214 SF-214-05 (calculateur délai) |
| 4. Validation VLS-TS OFII 3 mois | — | 🟡 P2 — F-214 SF-214-04 (outil dédié) |
| 5. Récépissé vs attestation prolongation | — | 🟡 P2 — F-214 SF-214-06 |
| 5. Droit au travail | F-IM-07 | ✅ Livrée |
| 6. ANEF procédure pannes | — | 🟡 P2 — F-214 SF-214-07 |
| 7a. Étranger malade L. 425-9 + recours OFII | — | 🟡 P2 — F-214 SF-214-01 (backend) + SF-214-02 (frontend) |
| 7b. Regroupement familial R. 434-1+ | — | 🟡 P2 — F-214 SF-214-08 (backend) + SF-214-09 (frontend) |
| 7c. VPF liens personnels L. 423-23 | — | 🟡 P2 — F-214 SF-214-10 (backend) + SF-214-11 (frontend) |
| 7d. AES calcul présence prouvée | — | 🟡 P2 — F-214 SF-214-12 (backend) + SF-214-13 (frontend) |
| 7e. Victime traite L. 425-1 | — | 🟡 P2 — F-214 SF-214-14 (backend) + SF-214-15 (frontend) |
| 7f. UE/EEE/Suisse droit séjour | — | 🟡 P2 — F-214 SF-214-16 (backend) + SF-214-17 (frontend) |
| 7g. Régularisation séjour irrégulier | — | 🟡 P2 — F-214 SF-214-18 (backend) + SF-214-19 (frontend) |
| 7h. OQTF catégories L. 611-1 | F-IM-08 (OQTF avec/sans délai) partiel | 🟡 P2 — F-214 SF-214-20 (backend) + SF-214-21 (frontend) |
| 8. Calcul ressources regroupement familial | — | 🟡 P2 — absorbé dans SF-214-08/09 |
| 9. Dépôt préfecture ANEF | ANEF outil dédié manque | 🟡 absorbé SF-214-07 |
| 10a. Recours naturalisation TJ | — | 🟡 P2 — F-214 SF-214-22 (backend) + SF-214-23 (frontend) |
| 10b. Appel CAA + cassation CE | — | 🟡 P2 — F-214 SF-214-24 (backend) + SF-214-25 (frontend) |
| 10c. Assignation résidence L. 731-1 | — | 🟡 P2 — F-214 SF-214-26 (backend) + SF-214-27 (frontend) |
| 10d. ITF judiciaire | — | 🟡 P2 — F-214 SF-214-28 (backend) + SF-214-29 (frontend) |
| 11. Carte résident L. 426-1 | F-IM-21 partiel | 🟡 P2 — F-214 SF-214-30 (backend) + SF-214-31 (frontend) |
| 12. MNA évaluation âge | F-IM-19 partiel | 🟡 P2 — F-214 SF-214-32 (backend) + SF-214-33 (frontend) |
| 13. Recours refus naturalisation | F-IM-13 partiel | 🟡 absorbé SF-214-22/23 (TJ) + SF-214-24/25 (TA Nantes) |
| 14. Demande OFPRA introduction | — | 🟡 P2 — F-214 SF-214-34 (backend) + SF-214-35 (frontend) |
| Transversal. Retrait titre fraude | — | 🟡 P2 — F-214 SF-214-36 (backend) + SF-214-37 (frontend) |
| Transversal. AJ CNDA | — | 🟡 P2 — F-214 SF-214-38 (backend) + SF-214-39 (frontend) |
| Transversal. Autorisation travail employeur | F-IM-07 partiel | 🟡 P2 — F-214 SF-214-40 (backend) + SF-214-41 (frontend) |

---

## Position de la nouvelle feature

F-214 intervient aux étapes 4 à 14 du workflow — elle couvre toutes les situations P2 (fréquence haute) qui ne sont pas des urgences procédurales P1 (déjà couverts par F-208). Ce sont les situations que l'avocat rencontre plusieurs fois par mois et pour lesquelles le produit ne propose pas encore d'outil dédié.

---

## Challenge amont

Les pré-requis fonctionnels sont tous livrés ou au backlog :

| Pré-requis | Feature | Statut |
|---|---|---|
| Analyse IA du dossier (extraction données immigration) | F-3/F-4/F-5 + pipeline IA | ✅ Livrée |
| `ImmigrationExtractedData` — champs existants | F-201/F-234/F-235/F-246 | ✅ Livrée (builder pattern, flags F-201, champs F-246) |
| Panel F-IA-04 (affichage outils décisionnels CONTEXTUAL) | F-IA-04 / F-199 | ✅ Livrée |
| Modèle canonique backend outil Immigration FR | F-208 (4 outils P1) | ✅ Livrée |
| Modèle canonique frontend outil Immigration FR | F-208 SF-208-05..08 | ✅ Livrée |
| Flags CONTEXTUAL Immigration FR | F-201 (9 flags) | ✅ Livrée |
| Extension champ texte nationalite pour trigger | F-235 | ✅ Livrée |
| Nouveaux champs pré-fill IA (AES, naturalisation, etc.) | F-246 (vague 2026-05-19) | ✅ En cours / livrée |

**Conclusion amont** : pas de trou bloquant. Tous les mécanismes techniques (canal CONTEXTUAL, `ImmigrationExtractedData`, pattern outil backend+frontend, préfill IA) sont disponibles depuis F-201/F-208/F-234/F-235/F-246.

---

## Challenge aval

La sortie de chaque outil F-214 est utilisée par :

1. **Tableau de bord décisionnel** (`app-case-dashboard`) — les verdicts produits par les outils P2 alimentent le tableau de bord. ✅ Existant (F-IA-02).
2. **Génération de conclusions** (`app-conclusions-section`, F-98) — les outils P2 enrichissent les éléments de stratégie transmis au générateur de conclusions. ✅ F-98 est livrable en V1 sans les outils P2, mais leur présence enrichit la qualité.
3. **F-IA-03 validation cohérence** — les alertes de cohérence entre inputs et données IA. ✅ Existant (`CoherenceAlertBuilder`).
4. **F-IM-01 checklist pièces** — les outils P2 complètent la checklist par sous-type (ex. regroupement familial → pièces OFII spécifiques). ✅ Enrichissement possible sans blocage.

**Pas de trou aval bloquant.** Les outils P2 se branchent sur une chaîne existante.

---

## STOPs / pré-requis

**Aucun STOP**. Tous les pré-requis techniques et fonctionnels sont livrés.

**Ajustements requis** (GO avec ajustements) :

1. **F-246 doit être validée avant les SF F-214 qui dépendent de nouveaux champs IA**. Certaines SF F-214 exploitent des champs `ImmigrationExtractedData` introduits par F-246 (SF-246-04/17/18/19). Le séquencement doit garantir que les champs sont présents avant les SF correspondantes. Si F-246 n'a pas livré un champ spécifique, la SF F-214 concernée le documente comme « gap V1 / pré-fill partiel » (pattern documenté dans F-208 SF-208-05..08).
2. **Séquencement interne F-214** : les outils qui introduisent de nouveaux champs IA (`ImmigrationExtractedData` + prompt `IMMIGRATION_INSTRUCTION`) doivent partir d'une SF backend (backend + migration + nouveau flag IA) avant la SF frontend correspondante — cohérence avec le pattern F-208.
3. **Découpage 22 outils × 2 SF (backend + frontend)** = jusqu'à 44 SF. Compte tenu du volume, les outils sont regroupés par thème pour la livraison en vagues parallèles, suivant le pattern des salves précédentes.

---

## Invariants anti-gadget pour la mini-spec

1. **Un outil = une situation juridique distincte** (règle CLAUDE.md feedback memory). Aucun outil ne doit absorber silencieusement une situation couverte par un outil existant (F-IM-08/09/11/12/13/17/19/20).
2. **Tout outil P2 doit être CONTEXTUAL** (sauf si fréquence > 30 % des dossiers Immigration FR — voir audit Tableau C section 4.1). Le nouveau flag IA correspondant doit être ajouté dans `ImmigrationExtractedData` + prompt `IMMIGRATION_INSTRUCTION` dans la SF backend.
3. **Pré-fill IA obligatoire** (feedback `feedback_decision_tools_all_fields_prefilled.md`) : tout champ saisissable doit être pré-rempli depuis `ImmigrationExtractedData`. Exception : champ non factualisable et absent des pièces (documenté explicitement dans la mini-spec).
4. **Isolation workspace** : chaque outil persiste ses analyses dans une table `*_analyses` avec contrainte `case_file_id UNIQUE` + gate `workspace.country = FRANCE`.
5. **F-IA-03 obligatoire** : `coherenceAlerts` computed + `CoherenceAlertBuilder` partagé sur tout composant frontend décisionnel.
6. **TOOL_REGISTRY symétrique** : entrée dans `TOOL_REGISTRY` + `KNOWN_FRONTEND_TOOL_IDS` du test `DecisionToolVisibilityIntegrityIT` à poser dès la SF backend, même si le frontend n'est pas encore livré (pattern F-208 SF-208-01..04).
7. **Pas de doublon** avec les outils existants : vérifier avant chaque outil que F-IM-01/05/06/07/08/09/11/12/13/17/19/20/21 ne couvre pas déjà le cas.

---

## Tableau récapitulatif des 22 outils retenus

| N° | Outil | tool_id | SF backend | SF frontend | Flag IA CONTEXTUAL | Source juridique |
|---|---|---|---|---|---|---|
| 1 | Étranger malade L. 425-9 + recours OFII | `F-IM-25-etranger-malade-l4259-fr` | SF-214-01 | SF-214-02 | `etrangerMaladeDetecte` (nouveau) | L. 425-9 CESEDA ; R. 425-12 |
| 2 | Regroupement familial éligibilité + ressources | `F-IM-26-regroupement-familial-fr` | SF-214-03 | SF-214-04 | `regroupementFamilialEnvisage` (nouveau) | L. 434-1+ ; R. 434-1+ CESEDA |
| 3 | VPF liens personnels et familiaux L. 423-23 | `F-IM-27-vpf-liens-personnels-l42323-fr` | SF-214-05 | SF-214-06 | `viePriveeFamilialDetectee` (nouveau) | L. 423-23 CESEDA |
| 4 | Validation VLS-TS OFII 3 mois | `F-IM-28-vls-ts-validation-ofii-fr` | SF-214-07 | SF-214-08 | ALWAYS_ON (délai 3 mois critique) | R. 311-3+ CESEDA |
| 5 | OQTF catégories L. 611-1 (1° à 7°) | `F-IM-29-oqtf-categories-l6111-fr` | SF-214-09 | SF-214-10 | `typeProcedureDetectee = OQTF_*` (existant F-IM-08) | L. 611-1 1° à 7° CESEDA |
| 6 | AES calcul présence prouvée (transverse 4 AES) | `F-IM-30-aes-presence-prouvee-fr` | SF-214-11 | SF-214-12 | `aesCalculPresenceDeclenche` (nouveau flag dérivé de détection AES existante) | Circulaire Valls 28/11/2012 ; L. 435-1 CESEDA |
| 7 | Renouvellement délai dépôt 2 mois avant | `F-IM-31-renouvellement-delai-depot-fr` | SF-214-13 | SF-214-14 | ALWAYS_ON (délai irréversible — pattern F-DT-03) | R. 433-1+ CESEDA |
| 8 | Récépissé vs attestation prolongation | `F-IM-32-recepisse-attestation-fr` | SF-214-15 | SF-214-16 | `recouvrementTitreEnCours` (nouveau) | R. 311-4+ CESEDA |
| 9 | Demande OFPRA introduction (GUDA/ADA) | `F-IM-33-ofpra-introduction-fr` | SF-214-17 | SF-214-18 | `procedureAsileDetectee` (existant F-201) | L. 521-1+ ; L. 521-7 CESEDA |
| 10 | AJ CNDA procédure | `F-IM-34-aj-cnda-fr` | SF-214-19 | SF-214-20 | `procedureAsileDetectee` (existant F-201) | L. 532-29 CESEDA ; Loi 91-647 |
| 11 | Victime traite L. 425-1 | `F-IM-35-victime-traite-l4251-fr` | SF-214-21 | SF-214-22 | `victimeTraiteDetectee` (nouveau) | L. 425-1 CESEDA |
| 12 | Carte résident L. 426-1 (5 ans + intégration) | `F-IM-36-carte-resident-l4261-fr` | SF-214-23 | SF-214-24 | `carteResidentEnvisagee` (nouveau) | L. 426-1+ CESEDA |
| 13 | ANEF procédure / pannes / recours | `F-IM-37-anef-procedure-fr` | SF-214-25 | SF-214-26 | `anefPanneDetectee` (nouveau) | R. 311-2-2 CESEDA ; arrêté 27/04/2021 |
| 14 | MNA évaluation âge + recours | `F-IM-38-mna-evaluation-age-fr` | SF-214-27 | SF-214-28 | `clientMineurDetecte` (existant F-201) | Cciv 388 ; arrêté 17/11/2016 |
| 15 | Recours refus naturalisation TJ (déclaration) | `F-IM-39-naturalisation-recours-tj-fr` | SF-214-29 | SF-214-30 | `naturalisationEnvisageeDetectee` (existant F-201) | Cciv 26-3 ; CPC |
| 16 | Recours refus naturalisation TA Nantes (décret) | `F-IM-40-naturalisation-recours-ta-fr` | SF-214-31 | SF-214-32 | `naturalisationEnvisageeDetectee` (existant F-201) | L. 213-2+ ; CJA |
| 17 | Appel CAA + cassation CE délais | `F-IM-41-appel-caa-cassation-ce-fr` | SF-214-33 | SF-214-34 | `recoursEnvisageDetecte` (nouveau) | L. 614-? CESEDA ; CJA |
| 18 | Assignation résidence L. 731-1 | `F-IM-42-assignation-residence-fr` | SF-214-35 | SF-214-36 | `assignationResidenceDetectee` (nouveau) | L. 731-1+ ; L. 732-1+ CESEDA |
| 19 | ITF judiciaire (peine prononcée par juge pénal) | `F-IM-43-itf-judiciaire-fr` | SF-214-37 | SF-214-38 | `mesureEloignementDetectee` (existant F-201) | C. pén. 131-30+ ; L. 631-3 CESEDA |
| 20 | UE/EEE/Suisse droit au séjour | `F-IM-44-ue-eee-suisse-sejour-fr` | SF-214-39 | SF-214-40 | `nationaliteUe = true` (existant dans `ImmigrationExtractedData`) | Directive 2004/38 CE ; L. 233+ CESEDA |
| 21 | Retrait titre fraude L. 412-7 | `F-IM-45-retrait-titre-fraude-fr` | SF-214-41 | SF-214-42 | `retraitTitreFraudeDetecte` (nouveau) | L. 412-7+ CESEDA |
| 22 | Autorisation travail employeur L. 421-1 | `F-IM-46-autorisation-travail-employeur-fr` | SF-214-43 | SF-214-44 | ALWAYS_ON (côté employeur, complémentaire F-IM-07) | L. 5221-1+ Code travail ; L. 421-1 CESEDA |

**Total** : 22 outils × 2 SF (backend + frontend) = **44 SF** (conforme à l'estimation PRODUCT_SPEC.md).

---

## Justification des exclusions (outils P2 de l'audit non retenus dans F-214)

| Outil exclu | Raison |
|---|---|
| `recours-refus-visa-crrv` | **Livré F-208** (SF-208-03 + SF-208-07 — `F-IM-23-crrv-refus-visa-fr`). ✅ |
| `vls-ts-validation-ofii-FR` | **Reclassé P2** et intégré F-214 (SF-214-07/08). |
| `victime-violences-conjugales-l425-6` | **Livré F-208** (SF-208-04 + SF-208-08 — `F-IM-24-victime-violences-l4256-fr`). ✅ |
| `dublin-iii-recours-transfert-7j` | **Livré F-208** (SF-208-02 + SF-208-06 — `F-IM-22-dublin-recours-fr`). ✅ |
| `recours-jld-retention-24h` | **Livré F-208** (SF-208-01 + SF-208-05 — `F-IM-21-jld-retention-fr`). ✅ |
| `regularisation-irreguliere-strategie` | **Reclassé** : absorbé partiellement par les 4 AES existants (F-IM-09) + VPF L. 423-23 (outil 3) + regroupement familial (outil 2). Un outil "comparateur stratégique" global est à créer en F-214 SF-214-18/19 si périmètre non couvert. **Inclus en outil 9 AES présence prouvée + outil 7 régularisation stratégie** : scindé en outil 7 (renouvellement) et outil 6 (AES). Un comparateur de stratégie maître reste au backlog F-220. |
| `salarie-eligibilite-renouvellement` | Couvert par F-IM-01/F-IM-21 (critères binaires). Outil dédié P2 → reporté F-220 (non prioritaire vs les 22 outils retenus). |
| `regroupement-familial-conditions-ressources` | Absorbé dans SF-214-03/04 (regroupement familial complet). |
| `cnda-aide-juridictionnelle` | Inclus en outil 10 (AJ CNDA). ✅ |
| `etranger-malade-recours-ofii` | Absorbé dans outil 1 (SF-214-01 inclut le recours contre avis OFII défavorable). |
| `procedure-asile-acceleree-l531-24` | Reclassé P1 → traitement dans F-208 étendu ou F-220. La procédure accélérée (délai CNDA 15 j) est critique mais partiellement couverte par F-IM-12. Reporté F-220 (P3 spécificité FR). |
| `procedure-detention-cra-conditions` | P2 borderline → reporté F-220 (couvert partiellement par F-IM-21-jld-retention-fr de F-208). |
| `signalement-sis-radiation` | P3 → F-220. |

---

## Décision finale

**Verdict : GO avec ajustements.**

Les ajustements (séquencement F-246 avant certaines SF, découpage backend+frontend, nouveaux flags IA) sont intégrés dans les mini-specs. F-214 peut démarrer dès que ce document est validé et que SF-214-00b-ux-coherence est produit.

**Statut PRODUCT_SPEC.md** : passer de `À planifier` à `À faire` après validation de ce document.
