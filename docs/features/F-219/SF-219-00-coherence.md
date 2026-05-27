# F-219 — Cadrage cohérence (étape 0)

## Verdict : GO

## Intention métier (1 phrase)

Couvrir les **~32 outils décisionnels Travail BE-only de priorité P3 — spécificité belge forte** — pour que l'avocat belge dispose des calculateurs / analyseurs / checklists qui n'ont aucun équivalent fonctionnel français (RCC variants, congé-éducation régionalisé, flexi-job, étudiant jobiste 600h, code pénal social, deal pour l'emploi 2022 — semaine 4 jours / droit déconnexion, élections sociales, transfert CCT 32bis, fonds fermeture entreprise, Fedris MP/rente, intérim CCT 322, télétravail CCT 85/149, pécule de vacances, éco-chèques, Auditorat du travail), tous absents du produit à ce jour et distincts des 8 outils P1 livrés par F-207 et des 10 outils P2 livrés par F-213.

## Source juridique

`docs/features/F-191/audit-be-travail-exhaustif.md` — sections 3.1 à 3.13, §4.2 (« 35+ outils BE-only sans équivalent FR »), §4.4 (découpages à éclater). Sources BE primaires : CCT 17 / CCT 17/13 (RCC), Loi 22/01/1985 + CCT 103 (crédit-temps et interruptions de carrière), AR 25/11/1991 (ONEM), Loi 22/01/1985 + régionalisation 2014 (congé-éducation Wallonie/Flandre/Bruxelles), Loi 16/11/2015 + Loi 25/04/2014 (flexi-job), Loi 03/07/1978 art. 120bis + Loi 30/04/1999 (étudiant jobiste), CCT 322 + Loi 24/07/1987 (intérim), CCT 85 + CCT 149 (télétravail), CCT 32bis (transfert d'entreprise), Loi 13/02/1998 (loi Renault — licenciement collectif), Loi 26/06/2002 + AR 23/03/2007 (fermeture d'entreprise FFE), Code pénal social 06/06/2010, Loi 03/10/2022 (deal pour l'emploi), Loi 04/12/2007 (élections sociales), Loi 28/06/1971 + AR 30/03/1967 (pécule de vacances), AR 14/01/2013 (éco-chèques), Loi 22/04/2012 (égalité salariale H/F), Loi 27/12/2006 (faux indépendants), CJ art. 138bis (Auditorat du travail), AR 28/03/1969 + Loi 10/04/1971 (Fedris MP/rente), CCT 64 / 64bis (congés parental / soins palliatifs), Loi 07/04/2023 (congé paternité 20 jours).

---

## Inclusion / exclusion vs audit BE — décision de cadrage

### Outils retenus dans F-219 (P3 — spécificité BE-only)

| # | tool_id | Audit §audit | Priorité audit | Justification inclusion |
|---|---|---|---|---|
| 1 | `rcc-be-metiers-lourds` | §3.1 | **P3 BE-only** | RCC métiers lourds 58+ / 35 ans carrière — barème âge/carrière distinct du RCC général (F-207 couvre conditions et indemnité du régime général). Audit §4.4 recommande explicitement de splitter RCC en 3 outils. |
| 2 | `rcc-be-longue-carriere` | §3.1 | **P3 BE-only** | RCC longue carrière 59+ / 40 ans — barème distinct. Idem éclatement §4.4. |
| 3 | `rcc-be-entreprise-difficulte` | §3.1 | **P3 BE-only** | RCC entreprise reconnaissance difficulté/restructuration — procédure AR + CCT sectorielle ad hoc. Distinct du général. |
| 4 | `cumul-rcc-allocations` | §3.4 | **P3 BE-only** | Cumul allocations chômage + indemnité complémentaire RCC (CCT 17) — analyseur cumul réservé aux dossiers RCC. |
| 5 | `outplacement-be-general-30sem` | §3.1 | **P3 BE-only** | Outplacement général régime préavis ≥ 30 sem (≠ outplacement 45+ couvert par F-207). |
| 6 | `licenciement-be-collectif-renault` | §3.1 | **P3 BE-only** | Loi 13/02/1998 (loi Renault) — procédure 3 phases info-consult-décision, sanction renvoi délai d'attente 30 j. Aucun équivalent FR (PSE FR très différent). |
| 7 | `licenciement-be-fermeture-entreprise` | §3.1 | **P3 BE-only** | Loi 26/06/2002 + AR 23/03/2007 — indemnité fermeture spécifique + créances FFE. Aucun équivalent FR. |
| 8 | `transfert-entreprise-cct-32bis` | §3.8 | **P3 BE-only** | CCT 32bis — maintien droits, info-consult préalable, reprise contrats. Régime BE-spécifique (FR L. 1224-1 plus simple). |
| 9 | `elections-sociales-be` | §3.8 | **P3 BE-only** | Loi 04/12/2007 — cycles 4 ans (2024, 2028…), CE + CPPT, candidatures protégées. Aucun équivalent FR direct. |
| 10 | `delegue-syndical-cct-5` | §3.8 | **P3 BE-only** | CCT 5 — statut délégué syndical (≠ F-213 qui couvre la protection contre licenciement). Information / checklist droits-missions. |
| 11 | `conge-education-paye-region` | §3.6 | **P3 BE-only** | Régionalisé depuis 2014 — Wallonie (congé-éducation payé + chèques-formation) / Flandre (Vlaams Opleidingsverlof) / Bruxelles (régime spécifique). Arbre décisionnel régional. |
| 12 | `flexi-job-be` | §3.7 | **P3 BE-only** | Loi 16/11/2015 + Loi 25/04/2014 — secteurs autorisés (HoReCa, distribution étendue 2024), plafonds, déclaration Dimona. Aucun équivalent FR. |
| 13 | `etudiant-jobiste-be` | §3.7 | **P3 BE-only** | Loi 03/07/1978 art. 120bis + Loi 30/04/1999 — quota 600 h/an, ONSS réduit, Dimona spécifique. Aucun équivalent FR. |
| 14 | `interim-be-cct-322` | §3.7 | **P3 BE-only** | CCT 322 + Loi 24/07/1987 — validité mission, motifs limités, 3 mois renouvelables. ≠ intérim FR. |
| 15 | `interim-be-indemnite-fin-mission` | §3.7 | **P3 BE-only** | CCT 322 — indemnité fin mission spécifique (≠ IFM FR de F-DT-18). |
| 16 | `teletravail-be-cct-85-149` | §3.7 | **P3 BE-only** | CCT 85 (télétravail structurel) + CCT 149 (occasionnel) — accord écrit obligatoire, indemnité forfaitaire frais. Régime BE distinct. |
| 17 | `clause-ecolage-be` | §3.7 | **P3 BE-only** | CCT 13/2/2013 + Loi 03/07/1978 art. 22bis — clause d'écolage remboursement formation employeur. Très spécifique BE. |
| 18 | `semaine-4-jours-be` | §3.6 | **P3 BE-only** | Loi 03/10/2022 (deal pour l'emploi) — checklist conformité semaine compressée. Récent (2022-2023), très demandé. |
| 19 | `droit-deconnexion-be` | §3.6 | **P3 BE-only** | Loi 03/10/2022 + accord d'entreprise/sectoriel depuis 01/04/2023 — entreprises 20+ travailleurs. Aucun équivalent FR direct (FR a un droit déconnexion mais sans cadre obligatoire). |
| 20 | `pecule-vacances-be` | §3.3 | **P3 BE-only** (reclassé P2 factuel) | Loi 28/06/1971 + AR 30/03/1967 — pécule simple + double, pécule départ employeur. Très technique. Aucun équivalent FR (≠ ICCP FR). |
| 21 | `eco-cheques-cheques-repas-be` | §3.3 | **P3 BE-only** | AR 14/01/2013 (éco-chèques) + AR 12/10/2010 (chèques-repas) — montants exonérés ONSS, conditions. Avantages extra-légaux fréquents. |
| 22 | `egalite-femmes-hommes-be` | §3.9 | **P3 BE-only** | Loi 22/04/2012 — obligation rapport égalité salariale + analyse fonctions, comparateur H/F sectoriel. Loi BE 2012 spécifique. |
| 23 | `discrimination-be-handicap-amenagement` | §3.9 | **P3 BE-only** | Loi 10/05/2007 art. 14 + CCT 95 — refus d'aménagements raisonnables = discrimination indirecte. Concept BE spécifique. |
| 24 | `code-penal-social-be` | §3.12 | **P3 BE-only** | Code pénal social du 06/06/2010 — infractions employeur niveaux 1-4, sanctions. Code autonome BE — aucun équivalent FR. |
| 25 | `auditorat-travail-be` | §3.12 | **P3 BE-only** | CJ art. 138bis + CIC art. 24 — saisine parquet spécialisé. Concept BE-spécifique (≠ procureur civil FR). |
| 26 | `travail-noir-be-dimona` | §3.11 | **P3 BE-only** | Loi 06/07/1989 (DIMONA) + Loi 27/06/1969 (ONSS) — travail dissimulé, sanctions cotisations rétroactives + amende ONSS. Mécanisme BE spécifique. |
| 27 | `inastri-statut-travailleur-independant` | §3.11 | **P3 BE-only** | Loi 27/12/2006 (nature relation travail) — 4 critères généraux + critères sectoriels (ONSS, INASTI). Faux indépendant BE distinct du salariat dissimulé FR. |
| 28 | `mp-fedris-reconnaissance` | §3.5 | **P3 BE-only** | AR 28/03/1969 (liste fermée maladies) + AR 16/12/1985 (système ouvert preuve causalité). Fedris = organisme BE spécialisé. |
| 29 | `at-mp-rente-capital-be` | §3.5 | **P3 BE-only** | Loi 10/04/1971 art. 24 — rente vs capital selon taux IPP (< 19 % capitalisation, ≥ 19 % rente). Mécanisme BE distinct. |
| 30 | `bien-etre-rps-conseiller-prevention` | §3.5 | **P3 BE-only** | Loi 04/08/1996 + AR 10/04/2014 + CCT 72 — procédure RPS interne (CISP, demande informelle, formelle, enquête). F-DT-30 couvre la protection ; ici la saisine. |
| 31 | `conge-paternite-naissance-be` | §3.6 | **P3 BE-only** | Loi 03/07/1978 art. 30 + Loi 07/04/2023 — congé paternité 20 jours (réforme 2023, vs 11 j pré-réforme). Régime BE distinct. |
| 32 | `interruption-carriere-soins-parental` | §3.6 | **P3 BE-only** | CCT 64 + Loi 22/01/1985 — congé parental 4 mois, cumul allocations ONEM. Régime ONEM distinct du congé parental FR. |

**Total : 32 outils = 64 SF (1 backend + 1 frontend par outil) + 2 SF cadrages (SF-219-00 + SF-219-00b) = 66 SF.**

### Outils exclus de F-219 — justifications

| tool_id | Exclusion | Justification |
|---|---|---|
| `prescription-be-litige-travail`, `c4-onem-checklist`, `contestation-c4-onem`, `at-fedris-declaration`, `refere-tribunal-travail-be`, `rcc-be-conditions`, `rcc-be-indemnite-complementaire`, `outplacement-be-obligatoire-45` | **Déjà livré F-207** (8 outils P1) | Couverture P1 urgence procédurale terminée |
| `clause-non-concurrence-be`, `rappel-salaire-be`, `licenciement-be-statut-unique-preavis`, `licenciement-be-formule-claeys`, `licenciement-be-protection-grossesse`, `transaction-be-travail`, `harcelement-be-procedure-formelle`, `licenciement-be-protection-deleguee`, `licenciement-be-acte-equivalent`, `licenciement-be-cct109-deraisonnable` | **Déjà livré F-213** (10 outils P2) | Couverture P2 fréquence haute terminée |
| `F-DT-29-credit-temps-be` | **Déjà livré F-DT-29** | Crédit-temps fondamental CCT 103 ancien — déjà en DB + frontend câblé (Tableau A audit) |
| `F-132-rupture-amiable-info` | **Déjà livré F-132** | Rupture de gré à gré couverte par F-132 |
| `licenciement-be-protection-conseiller-prevention` | **Repoussé F-224 P4** | Cas peu fréquent — peut différer ; lien indirect avec `bien-etre-rps-conseiller-prevention` inclus dans F-219 |
| `licenciement-be-protection-credit-temps`, `licenciement-be-protection-soins-palliatifs-ecart` | **Repoussé F-224 P4** | Protection rupture pendant ces interruptions — cas plus rares, peuvent attendre. Le calcul d'éligibilité et de durée des interruptions reste dans F-219 (#32) |
| `licenciement-be-faute-arbitraire-ouvrier` (art. 63 abrogé 2014) | **Repoussé F-224 P4 voire DROPPED** | Dossiers historiques résiduels — fréquence quasi-nulle 2026+ |
| `interruption-carriere-soins-palliatifs` | **Repoussé F-224 P4** | Cas plus rare que congé parental — différable |
| `conge-adoption-be` | **Repoussé F-224 P4** | Cas peu fréquent — différable |
| `chomage-economique-be`, `chomage-temporaire-force-majeure` | **Repoussé F-224 P4** | Procédures employeur (côté défense salarié moins fréquent) |
| `allocations-onem-conditions` | **Repoussé F-224 P4** | Analyseur éligibilité général — peu d'usage défensif avocat (l'ONEM tranche unilatéralement) |
| `delai-attente-onem-licenciement-volontaire` | **Repoussé F-224 P4** | Cas spécifique — l'avocat utilise plutôt `contestation-c4-onem` (F-207) |
| `appel-cour-du-travail`, `pourvoi-cassation-social`, `competence-tribunal-travail-matiere` | **Repoussé F-224 P4** | Calculateurs de délais procéduraux génériques — utiles mais sans urgence spécifique BE-only |
| `cdd-be-validite`, `cdd-be-indemnite-rupture` | **Repoussé F-224 P4** | Mécanisme BE distinct mais fréquence moyenne — différable |
| `demission-be-validite`, `licenciement-be-rupture-irreguliere` | **Couvert partiellement F-213 / F-DT-09** | Préavis démission inclus dans la logique des outils statut unique ; rupture irrégulière proche F-DT-09 |
| `documents-fin-contrat-be`, `attestation-vacances-be` | **Repoussé F-224 P4** | Checklists documents — utiles mais V1 acceptable manuellement |
| `intereets-moratoires-paiement-tardif` | **Couvert F-213 SF-213-02** | Intérêts moratoires 10 % calculés dans `rappel-salaire-be` |
| `prime-fin-annee-be` | **Repoussé F-224 P4** | Sous-cas de F-DT-28-avantages-conventionnels-be existant |
| `secret-loon-clause-non-divulgation`, `retenues-be-cessions-saisies` | **Repoussé F-224 P4** | Cas ponctuels |
| `discrimination-be-unia-recours`, `discrimination-be-test-situation` | **Repoussé F-224 P4** | Procédure UNIA / test situation — utile mais l'outil F-DT-12 couvre le calcul dommages |
| `cct-39-introduction-technologies` | **Repoussé F-224 P4** | Rare |
| `domestiques-be-cct-323`, `petite-flexibilite-be`, `interventions-domicile-cct-149` | **Repoussé F-224 P4** | Cas rares |
| `clause-arbitrage-travail-be`, `clause-mobilite-be` | **Repoussé F-224 P4** | Sous-cas — `clause-mobilite-be` proche `licenciement-be-acte-equivalent` F-213 |
| `surveillance-sante-travailleurs` | **Repoussé F-224 P4** | Côté employeur |
| `conciliation-prealable-be` | **Repoussé F-224 P4** | Conciliation optionnelle en BE |
| `cotisations-onss-recuperation` | **Repoussé F-224 P4** | Sanction employeur — moins fréquent côté défense salarié |

**Bilan exclusion** : sur ~60 outils manquants identifiés par l'audit, 32 sont retenus pour F-219 (P3 BE-only), ~25 sont repoussés F-224 P4, 2-3 sont déjà partiellement couverts par les outils existants.

---

## Workflow métier réel de l'avocat Travail BE (P3 — spécificités BE-only)

Source : audit BE exhaustif §3 + pratique standard avocat travailliste belge + actualités droit social belge 2022-2025 (deal pour l'emploi 2022, réformes congé paternité 2023, élections sociales 2024).

**Scénario A — RCC (Régime Chômage Complément d'Entreprise) — situation très fréquente en BE**

1. Client 58+ ans, métier lourd (construction, soins santé, transport), souhaite anticiper départ.
2. L'avocat vérifie l'éligibilité **RCC métiers lourds** (58+ / 35 ans carrière + listes professionnelles) — outil 1.
3. Si carrière > 40 ans : vérifie aussi **RCC longue carrière** — outil 2.
4. Si entreprise en difficulté/restructuration (AR + CCT sectorielle reconnaissance) : **RCC entreprise difficulté** — outil 3.
5. Calcul indemnité complémentaire RCC = F-207 SF-207-07 (déjà livré).
6. Vérifie le **cumul RCC + allocations ONEM** — outil 4 (analyseur cumul des deux flux).
7. Si licenciement > 30 sem préavis : **outplacement général 30 sem** — outil 5.

**Scénario B — Licenciement collectif / fermeture d'entreprise — situations BE-only**

1. Employeur annonce une restructuration ou fermeture.
2. L'avocat analyse la **procédure loi Renault** (3 phases info-consult-décision, sanction délai d'attente 30 j) — outil 6.
3. Si fermeture définitive : analyse **fermeture d'entreprise** + créances FFE (Fonds Fermeture Entreprises) — outil 7.
4. Si transfert d'activité plutôt que fermeture : analyse **transfert CCT 32bis** (maintien droits, info-consult) — outil 8.

**Scénario C — Élections sociales / représentation du personnel**

1. Cycle 4 ans (2024 dernier, 2028 prochain) — entreprises 50+ (CE) ou 50-99 (CPPT).
2. L'avocat suit la **chronologie élections sociales** (calculateur de phases + candidatures protégées) — outil 9.
3. Si délégué syndical : information **statut CCT 5** (droits, missions hors protection licenciement) — outil 10.

**Scénario D — Congé-éducation régionalisé**

1. Salarié en formation souhaite faire valoir son droit au congé-éducation payé.
2. L'avocat oriente vers le **régime régional applicable** — Wallonie (congé-éducation payé + chèques-formation), Flandre (Vlaams Opleidingsverlof), Bruxelles (régime spécifique) — outil 11.

**Scénario E — Statuts particuliers (flexi-job, étudiant, intérim, télétravail)**

1. Salarié exerce un **flexi-job** (HoReCa, distribution) — l'avocat vérifie éligibilité, secteurs, plafonds, déclaration Dimona — outil 12.
2. Étudiant : quota **600 h/an** étudiant jobiste, ONSS réduit — outil 13.
3. Intérimaire : **validité mission CCT 322** (motifs limités, 3 mois renouvelables) — outil 14, et **indemnité fin mission** spécifique BE — outil 15.
4. Télétravail : conformité **CCT 85 / CCT 149** (accord écrit, indemnité forfaitaire) — outil 16.
5. Clause d'écolage employeur (formation remboursable) — **CCT 13/2/2013** — outil 17.

**Scénario F — Deal pour l'emploi 2022 (semaine 4 jours, droit déconnexion)**

1. Salarié souhaite passer en **semaine de 4 jours** (Loi 03/10/2022) — l'avocat vérifie conditions et cadre — outil 18.
2. Salarié subit débordements horaires : vérifie l'existence d'un **accord droit déconnexion** (obligatoire 20+ travailleurs depuis 01/04/2023) — outil 19.

**Scénario G — Rémunération BE complexe**

1. Litige sur le **pécule de vacances** (employé vs ouvrier, pécule simple + double, pécule départ) — outil 20.
2. Litige sur **éco-chèques / chèques-repas** (exonérations ONSS, conditions) — outil 21.
3. Doute sur égalité salariale H/F : analyse selon **Loi 22/04/2012** (rapport obligatoire + analyse fonctions) — outil 22.

**Scénario H — Discrimination handicap**

1. Travailleur handicapé refusé/licencié faute d'aménagements raisonnables.
2. L'avocat analyse **refus aménagements** = discrimination indirecte (Loi 10/05/2007 art. 14 + CCT 95) — outil 23.

**Scénario I — Contentieux pénal du travail**

1. Infractions employeur graves : l'avocat analyse les **niveaux 1-4 Code pénal social** (Loi 06/06/2010) — outil 24.
2. Si poursuites : information **saisine Auditorat du travail** (parquet spécialisé) — outil 25.

**Scénario J — Travail dissimulé / faux indépendant**

1. Travail au noir constaté : analyse **sanctions Dimona / ONSS** — outil 26.
2. Indépendant requalifié salarié : analyse **4 critères Loi 2006** + critères sectoriels INASTI — outil 27.

**Scénario K — Accidents du travail / maladies professionnelles Fedris**

1. Reconnaissance MP : liste fermée AR 28/03/1969 ou système ouvert AR 16/12/1985 — outil 28.
2. Calcul rente vs capital AT/MP selon taux IPP (< 19 % capital, ≥ 19 % rente) — outil 29.

**Scénario L — RPS et conseiller en prévention**

1. Salarié subit risques psychosociaux : saisit conseiller prévention CISP, plainte informelle → formelle → enquête — outil 30 (F-DT-30 protection ; ici la saisine).

**Scénario M — Congés naissance / parental BE**

1. Congé paternité 20 jours (Loi 07/04/2023) — outil 31.
2. Congé parental 4 mois CCT 64 + cumul allocations ONEM — outil 32.

---

## Cartographie features actuelles ↔ workflow

| Étape métier | Outil LegalCase | Statut |
|---|---|---|
| Upload / extraction / analyse IA | F-43, F-121, F-122 | ✅ Livrée |
| RCC général conditions + indemnité | F-207 SF-207-06/07 | ✅ Livrée |
| Outplacement 45+ | F-207 SF-207-08 | ✅ Livrée |
| Crédit-temps CCT 103 général | F-DT-29-credit-temps-be | ✅ Livrée |
| RCC métiers lourds 58+ | `rcc-be-metiers-lourds` | ❌ **MANQUE — F-219 outil 1** |
| RCC longue carrière 59+/40 | `rcc-be-longue-carriere` | ❌ **MANQUE — F-219 outil 2** |
| RCC entreprise difficulté | `rcc-be-entreprise-difficulte` | ❌ **MANQUE — F-219 outil 3** |
| Cumul RCC + ONEM | `cumul-rcc-allocations` | ❌ **MANQUE — F-219 outil 4** |
| Outplacement général 30 sem | `outplacement-be-general-30sem` | ❌ **MANQUE — F-219 outil 5** |
| Licenciement collectif loi Renault | `licenciement-be-collectif-renault` | ❌ **MANQUE — F-219 outil 6** |
| Fermeture entreprise + FFE | `licenciement-be-fermeture-entreprise` | ❌ **MANQUE — F-219 outil 7** |
| Transfert entreprise CCT 32bis | `transfert-entreprise-cct-32bis` | ❌ **MANQUE — F-219 outil 8** |
| Élections sociales chronologie | `elections-sociales-be` | ❌ **MANQUE — F-219 outil 9** |
| Délégué syndical CCT 5 statut | `delegue-syndical-cct-5` | ❌ **MANQUE — F-219 outil 10** |
| Congé-éducation régionalisé | `conge-education-paye-region` | ❌ **MANQUE — F-219 outil 11** |
| Flexi-job | `flexi-job-be` | ❌ **MANQUE — F-219 outil 12** |
| Étudiant jobiste 600 h | `etudiant-jobiste-be` | ❌ **MANQUE — F-219 outil 13** |
| Intérim CCT 322 mission | `interim-be-cct-322` | ❌ **MANQUE — F-219 outil 14** |
| Intérim indemnité fin mission | `interim-be-indemnite-fin-mission` | ❌ **MANQUE — F-219 outil 15** |
| Télétravail CCT 85 / 149 | `teletravail-be-cct-85-149` | ❌ **MANQUE — F-219 outil 16** |
| Clause écolage employeur | `clause-ecolage-be` | ❌ **MANQUE — F-219 outil 17** |
| Semaine 4 jours (deal pour l'emploi) | `semaine-4-jours-be` | ❌ **MANQUE — F-219 outil 18** |
| Droit déconnexion accord | `droit-deconnexion-be` | ❌ **MANQUE — F-219 outil 19** |
| Pécule vacances BE | `pecule-vacances-be` | ❌ **MANQUE — F-219 outil 20** |
| Éco-chèques / chèques-repas | `eco-cheques-cheques-repas-be` | ❌ **MANQUE — F-219 outil 21** |
| Égalité salariale H/F BE | `egalite-femmes-hommes-be` | ❌ **MANQUE — F-219 outil 22** |
| Discrimination handicap aménagement | `discrimination-be-handicap-amenagement` | ❌ **MANQUE — F-219 outil 23** |
| Code pénal social niveaux 1-4 | `code-penal-social-be` | ❌ **MANQUE — F-219 outil 24** |
| Auditorat du travail saisine | `auditorat-travail-be` | ❌ **MANQUE — F-219 outil 25** |
| Travail noir Dimona / ONSS | `travail-noir-be-dimona` | ❌ **MANQUE — F-219 outil 26** |
| Faux indépendant Loi 2006 | `inastri-statut-travailleur-independant` | ❌ **MANQUE — F-219 outil 27** |
| MP Fedris reconnaissance | `mp-fedris-reconnaissance` | ❌ **MANQUE — F-219 outil 28** |
| AT/MP rente vs capital | `at-mp-rente-capital-be` | ❌ **MANQUE — F-219 outil 29** |
| Bien-être RPS conseiller prévention | `bien-etre-rps-conseiller-prevention` | ❌ **MANQUE — F-219 outil 30** |
| Congé paternité 20 j (Loi 2023) | `conge-paternite-naissance-be` | ❌ **MANQUE — F-219 outil 31** |
| Congé parental 4 mois CCT 64 | `interruption-carriere-soins-parental` | ❌ **MANQUE — F-219 outil 32** |
| Pipeline IA + synthèse + conclusions | F-3/4/5, F-98 | ✅ Livrée |

---

## Briques d'infrastructure amont — toutes livrées (héritées F-207 + F-213)

- **Panneau outils décisionnels** (`app-decisional-tools-panel`) — accueille N outils via `TOOL_REGISTRY`. ✅
- **Gate `workspaceCountry === 'BELGIQUE'`** — pattern BE-only stabilisé sur 18 outils livrés (F-DT-29, F-132, F-207 ×8, F-213 ×10). ✅
- **Pattern F-213 backend autonome** (`feedback_f213_backend_pattern`) — pas de modif `TravailExtractedData` / `CritereCode` / `LegalDomainPromptBuilder` / `SYSTEM_PROMPT_TEMPLATEs`. ✅
- **Pattern F-213 frontend autonome** — ALWAYS_ON priority croissante, ajout `KNOWN_NO_DASHBOARD_TILE_IDS` obligatoire dans `DashboardTileToolIdIntegrityIT`. ✅
- **Validation F-IA-03** + émission `critereCode` BE — `CritereCodeIntegrityIT` reste vert. ✅
- **Migrations Liquibase pattern** (`*-analyses` table + `decision_tool_visibility_rules`). Pattern canonique. ✅
- **Test d'intégrité visibility** (`DecisionToolVisibilityIntegrityIT`) + `KNOWN_FRONTEND_TOOL_IDS` — empêche les orphelins (mémoire `feedback_pre_merge_visibility_seed_check`). ✅

---

## Challenge amont

- Upload / extraction / OCR : ✅ F-43, F-121, F-122 (SF-122-13 multi-pages inclus).
- Pipeline IA + détection contextuelle BE : ✅ Étendu à chaque outil via extension prompt BE.
- `dateRuptureContrat`, `ancienneteAnnees`, `motifRupture`, `salaireBrutAnnuel` : disponibles depuis F-207 SF-207-01 + F-213. Les champs spécifiques P3 (ex. `ageTravailleur`, `secteurFlexiJob`, `regionConge`, `dateDebutInterim`, `tauxIPP`, `categorieMaladieProf`) seront saisis manuellement par l'avocat en V1 (pattern F-213 autonome — pas d'enrichissement IA croisé en V1).

**Aucun trou bloquant amont.**

---

## Challenge aval

Sortie de chaque outil :
- Verdict décisionnel (éligibilité / montant / durée / conformité / score) → **dashboard décisionnel** (F-IA-02) via `triggerRefresh()`.
- Résultat enrichit le **projet de conclusions** (F-98).
- Le `KNOWN_NO_DASHBOARD_TILE_IDS` reçoit chaque tool_id F-219 par défaut (pattern F-213 — pas de dashboard tile en V1, ajout possible plus tard via SF dédiée).

**Aucun trou aval.**

---

## STOPs / pré-requis

Aucun bloquant technique. F-207 et F-213 sont terminées et mergées — toute l'infrastructure BE Travail est en place. F-219 peut démarrer immédiatement après validation des mini-specs.

---

## Invariants anti-gadget pour les mini-specs

1. **Partir des sources BE — pas de calque FR** (`feedback_belgique_never_forget`). Chaque outil cite sa source primaire belge (CCT, Loi, AR, Code pénal social, jurisprudence BE). Audit §4.2 prouve l'indépendance fonctionnelle des 32 outils vs FR.
2. **Workspace gate BE-only strict** — `workspaceCountry=BELGIQUE` côté controller + `country === 'BELGIQUE'` côté frontend. Test isolation `country=FRANCE` → outil masqué — obligatoire par SF frontend.
3. **Critères F-IA-03 BE distincts** — `BE_*` préfixés (ex. `BE_AGE_TRAVAILLEUR`, `BE_RCC_TYPE`, `BE_REGION_CONGE`, `BE_TAUX_IPP`). `CritereCodeIntegrityIT` reste vert (mais V1 : pas de modif `KNOWN_FRONTEND_CRITERE_CODES` — pattern F-213).
4. **Un outil = une situation métier** (`feedback_decision_tools_one_per_situation`). RCC = 3 outils distincts (métiers lourds / longue carrière / entreprise difficulté) — pas un super-outil. Conformément à audit §4.4.
5. **Pattern F-213 backend autonome** (`feedback_f213_backend_pattern`) — pas de modif `TravailExtractedData` / `CritereCode` / `LegalDomainPromptBuilder` / `SYSTEM_PROMPT_TEMPLATEs`. Chaque outil = ~10-12 classes autonomes + migration `XXX-create-<tool>-analyses.xml`.
6. **Pattern F-213 frontend autonome** — composant standalone + helper prefill-rules + model + service + entrée TOOL_REGISTRY + migration visibility ALWAYS_ON priority croissante (119, 120, 121… à partir de 119). **Ajout obligatoire** dans `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS` (sinon master-red — récidive 2 fois en 24 h sur F-213).
7. **Pré-remplissage IA limité aux champs déjà extraits par le modèle de base** (`feedback_decision_tools_all_fields_prefilled` à appliquer au niveau outil V2 / consolidation IA cross-domain BE future, pas chaque SF F-219 individuelle). V1 F-219 : pré-fill sur `salaireBrutAnnuel`, `dateRuptureContrat`, `ancienneteAnnees`, `motifRupture` quand pertinent — autres champs en saisie manuelle.
8. **CONTEXTUAL vs ALWAYS_ON arbitré au cas par cas** (cf. étape 0 bis) — par défaut ALWAYS_ON (pattern F-213), CONTEXTUAL uniquement si un flag DB existe déjà OU si la situation est suffisamment rare pour mériter d'être masquée par défaut.
9. **Régionalisation Wallonie/Flandre/Bruxelles** — outil 11 (`conge-education-paye-region`) sera un outil UNIQUE avec branchement régional interne (input `region` enum 3 valeurs), pas 3 outils distincts. Audit §4.4 recommandation explicite.
10. **Annotation `BELGIQUE UNIQUEMENT`** sur chaque champ ajouté pour F-219 (modèle frontend + payload backend), avec garde `country === 'BE'` côté frontend.

---

## Découpage en 66 SF (parallélisation back/front par outil)

Pattern : **1 SF backend + 1 SF frontend** parallélisables (contrat API figé dans la mini-spec backend) + 2 SF cadrages (SF-219-00 + SF-219-00b). 32 outils × 2 = 64 SF + 2 cadrages = **66 SF**.

| # | Outil | SF backend | SF frontend | Source juridique BE principale |
|---|---|---|---|---|
| 1 | `rcc-be-metiers-lourds` | SF-219-01-backend | SF-219-01b-frontend | CCT 17 ; AR 03/05/2007 art. 3 (métiers lourds 58+) |
| 2 | `rcc-be-longue-carriere` | SF-219-02-backend | SF-219-02b-frontend | CCT 17 ; AR 03/05/2007 art. 3 (longue carrière 59+/40) |
| 3 | `rcc-be-entreprise-difficulte` | SF-219-03-backend | SF-219-03b-frontend | CCT 17 ; AR 03/05/2007 (reconnaissance entreprise) |
| 4 | `cumul-rcc-allocations` | SF-219-04-backend | SF-219-04b-frontend | CCT 17 ; AR 03/05/2007 (cumul ONEM + indemnité complémentaire) |
| 5 | `outplacement-be-general-30sem` | SF-219-05-backend | SF-219-05b-frontend | Loi 05/09/2001 ; AR 21/10/2007 |
| 6 | `licenciement-be-collectif-renault` | SF-219-06-backend | SF-219-06b-frontend | Loi 13/02/1998 (loi Renault) ; CCT 24 ; CCT 39 |
| 7 | `licenciement-be-fermeture-entreprise` | SF-219-07-backend | SF-219-07b-frontend | Loi 26/06/2002 ; AR 23/03/2007 ; FFE |
| 8 | `transfert-entreprise-cct-32bis` | SF-219-08-backend | SF-219-08b-frontend | CCT 32bis ; CCT 32ter ; directive 2001/23/CE |
| 9 | `elections-sociales-be` | SF-219-09-backend | SF-219-09b-frontend | Loi 04/12/2007 (élections sociales) |
| 10 | `delegue-syndical-cct-5` | SF-219-10-backend | SF-219-10b-frontend | CCT 5 ; Loi 19/03/1991 |
| 11 | `conge-education-paye-region` | SF-219-11-backend | SF-219-11b-frontend | Loi 22/01/1985 (régionalisé 2014) — décrets WBR/FLA/BXL |
| 12 | `flexi-job-be` | SF-219-12-backend | SF-219-12b-frontend | Loi 16/11/2015 ; Loi 25/04/2014 |
| 13 | `etudiant-jobiste-be` | SF-219-13-backend | SF-219-13b-frontend | Loi 03/07/1978 art. 120bis ; Loi 30/04/1999 |
| 14 | `interim-be-cct-322` | SF-219-14-backend | SF-219-14b-frontend | CCT 322 ; Loi 24/07/1987 |
| 15 | `interim-be-indemnite-fin-mission` | SF-219-15-backend | SF-219-15b-frontend | Loi 24/07/1987 ; CCT 322 |
| 16 | `teletravail-be-cct-85-149` | SF-219-16-backend | SF-219-16b-frontend | CCT 85 (structurel) ; CCT 149 (occasionnel) |
| 17 | `clause-ecolage-be` | SF-219-17-backend | SF-219-17b-frontend | CCT 13/2/2013 ; Loi 03/07/1978 art. 22bis |
| 18 | `semaine-4-jours-be` | SF-219-18-backend | SF-219-18b-frontend | Loi 03/10/2022 (deal pour l'emploi) |
| 19 | `droit-deconnexion-be` | SF-219-19-backend | SF-219-19b-frontend | Loi 03/10/2022 ; accord d'entreprise/sectoriel 2023 |
| 20 | `pecule-vacances-be` | SF-219-20-backend | SF-219-20b-frontend | Loi 28/06/1971 ; AR 30/03/1967 |
| 21 | `eco-cheques-cheques-repas-be` | SF-219-21-backend | SF-219-21b-frontend | AR 14/01/2013 (éco-chèques) ; AR 12/10/2010 (chèques-repas) |
| 22 | `egalite-femmes-hommes-be` | SF-219-22-backend | SF-219-22b-frontend | Loi 22/04/2012 ; Loi 12/01/2007 |
| 23 | `discrimination-be-handicap-amenagement` | SF-219-23-backend | SF-219-23b-frontend | Loi 10/05/2007 art. 14 ; CCT 95 |
| 24 | `code-penal-social-be` | SF-219-24-backend | SF-219-24b-frontend | Code pénal social du 06/06/2010 |
| 25 | `auditorat-travail-be` | SF-219-25-backend | SF-219-25b-frontend | CJ art. 138bis ; CIC art. 24 |
| 26 | `travail-noir-be-dimona` | SF-219-26-backend | SF-219-26b-frontend | Loi 06/07/1989 (DIMONA) ; Loi 27/06/1969 (ONSS) |
| 27 | `inastri-statut-travailleur-independant` | SF-219-27-backend | SF-219-27b-frontend | Loi 27/12/2006 (nature relation travail) |
| 28 | `mp-fedris-reconnaissance` | SF-219-28-backend | SF-219-28b-frontend | AR 28/03/1969 (liste) ; AR 16/12/1985 (système ouvert) |
| 29 | `at-mp-rente-capital-be` | SF-219-29-backend | SF-219-29b-frontend | Loi 10/04/1971 art. 24 ; Fedris |
| 30 | `bien-etre-rps-conseiller-prevention` | SF-219-30-backend | SF-219-30b-frontend | Loi 04/08/1996 ; AR 10/04/2014 ; CCT 72 |
| 31 | `conge-paternite-naissance-be` | SF-219-31-backend | SF-219-31b-frontend | Loi 03/07/1978 art. 30 ; Loi 07/04/2023 (20 jours) |
| 32 | `interruption-carriere-soins-parental` | SF-219-32-backend | SF-219-32b-frontend | CCT 64 ; Loi 22/01/1985 |

**Ordre de livraison** : par l'agent dev en vagues parallèles ~3-4 outils simultanés (mémoire `feedback_max_2_parallel_agents` à respecter — bundles par 2 outils). Vague 1 : RCC variants (1-4, thématique cohérente). Vague 2 : licenciement collectif/fermeture/transfert (5-8). Vague 3 : élections / délégué / congé-éducation (9-11). Vague 4 : statuts particuliers (12-17). Vague 5 : deal pour l'emploi (18-19). Vague 6 : rémunération (20-22). Vague 7 : discrimination + Auditorat (23-25). Vague 8 : travail dissimulé / faux indépendant (26-27). Vague 9 : Fedris MP/rente (28-29). Vague 10 : RPS + congés naissance/parental (30-32).

---

## Décision finale

**GO.** Toutes les briques d'infrastructure amont/aval sont matures (F-207 + F-213 terminées). Les 32 outils P3 sont indépendants entre eux et techniquement assimilables au pattern F-213 autonome (backend + frontend autonomes, ALWAYS_ON priority croissante, pas de modif transverse). L'effort est sur la **substance juridique BE P3 BE-only** (RCC variants, lois récentes 2022-2023, Code pénal social, Fedris MP/rente). Feature à impact écran → étape 0 bis requise (`SF-219-00b-ux-coherence.md`).

---

## Tableau récapitulatif — Outil → SF → flag couverture → source juridique BE

| Outil | SF backend | SF frontend | Couverture (déjà livré ?) | Source juridique BE |
|---|---|---|---|---|
| `rcc-be-metiers-lourds` | SF-219-01 | SF-219-01b | Spin-off F-207 SF-207-06 (RCC général) — NOUVEAU | CCT 17 ; AR 03/05/2007 art. 3 |
| `rcc-be-longue-carriere` | SF-219-02 | SF-219-02b | Spin-off F-207 SF-207-06 — NOUVEAU | CCT 17 ; AR 03/05/2007 art. 3 |
| `rcc-be-entreprise-difficulte` | SF-219-03 | SF-219-03b | Spin-off F-207 SF-207-06 — NOUVEAU | CCT 17 ; AR 03/05/2007 |
| `cumul-rcc-allocations` | SF-219-04 | SF-219-04b | Complément F-207 RCC + ONEM — NOUVEAU | CCT 17 ; AR 03/05/2007 |
| `outplacement-be-general-30sem` | SF-219-05 | SF-219-05b | Complément F-207 SF-207-08 (45+) — NOUVEAU | Loi 05/09/2001 ; AR 21/10/2007 |
| `licenciement-be-collectif-renault` | SF-219-06 | SF-219-06b | Aucun équivalent FR — NOUVEAU | Loi 13/02/1998 ; CCT 24 ; CCT 39 |
| `licenciement-be-fermeture-entreprise` | SF-219-07 | SF-219-07b | Aucun équivalent FR (FFE) — NOUVEAU | Loi 26/06/2002 ; AR 23/03/2007 |
| `transfert-entreprise-cct-32bis` | SF-219-08 | SF-219-08b | Régime BE distinct de L. 1224-1 FR — NOUVEAU | CCT 32bis ; CCT 32ter |
| `elections-sociales-be` | SF-219-09 | SF-219-09b | Aucun équivalent FR — NOUVEAU | Loi 04/12/2007 |
| `delegue-syndical-cct-5` | SF-219-10 | SF-219-10b | Complément F-213 SF-213-08 (protection) — NOUVEAU | CCT 5 ; Loi 19/03/1991 |
| `conge-education-paye-region` | SF-219-11 | SF-219-11b | Régionalisé BE — NOUVEAU | Loi 22/01/1985 (régionalisé 2014) |
| `flexi-job-be` | SF-219-12 | SF-219-12b | Aucun équivalent FR — NOUVEAU | Loi 16/11/2015 |
| `etudiant-jobiste-be` | SF-219-13 | SF-219-13b | Aucun équivalent FR (quota 600 h) — NOUVEAU | Loi 03/07/1978 art. 120bis |
| `interim-be-cct-322` | SF-219-14 | SF-219-14b | Régime BE distinct de l'intérim FR — NOUVEAU | CCT 322 ; Loi 24/07/1987 |
| `interim-be-indemnite-fin-mission` | SF-219-15 | SF-219-15b | Régime BE distinct IFM FR — NOUVEAU | CCT 322 ; Loi 24/07/1987 |
| `teletravail-be-cct-85-149` | SF-219-16 | SF-219-16b | Régime BE distinct accord télétravail FR — NOUVEAU | CCT 85 ; CCT 149 |
| `clause-ecolage-be` | SF-219-17 | SF-219-17b | Très spécifique BE — NOUVEAU | CCT 13/2/2013 ; Loi 03/07/1978 art. 22bis |
| `semaine-4-jours-be` | SF-219-18 | SF-219-18b | Régime BE 2022 (deal pour l'emploi) — NOUVEAU | Loi 03/10/2022 |
| `droit-deconnexion-be` | SF-219-19 | SF-219-19b | Régime BE 2022/2023 obligatoire — NOUVEAU | Loi 03/10/2022 |
| `pecule-vacances-be` | SF-219-20 | SF-219-20b | Régime BE distinct ICCP FR — NOUVEAU | Loi 28/06/1971 ; AR 30/03/1967 |
| `eco-cheques-cheques-repas-be` | SF-219-21 | SF-219-21b | Avantages extra-légaux BE — NOUVEAU | AR 14/01/2013 ; AR 12/10/2010 |
| `egalite-femmes-hommes-be` | SF-219-22 | SF-219-22b | Loi BE 2012 spécifique — NOUVEAU | Loi 22/04/2012 ; Loi 12/01/2007 |
| `discrimination-be-handicap-amenagement` | SF-219-23 | SF-219-23b | Concept BE — NOUVEAU | Loi 10/05/2007 art. 14 ; CCT 95 |
| `code-penal-social-be` | SF-219-24 | SF-219-24b | Code BE autonome 2010 — NOUVEAU | Code pénal social 06/06/2010 |
| `auditorat-travail-be` | SF-219-25 | SF-219-25b | Concept BE — NOUVEAU | CJ art. 138bis ; CIC art. 24 |
| `travail-noir-be-dimona` | SF-219-26 | SF-219-26b | Mécanisme BE DIMONA — NOUVEAU | Loi 06/07/1989 ; Loi 27/06/1969 |
| `inastri-statut-travailleur-independant` | SF-219-27 | SF-219-27b | Loi BE 2006 — NOUVEAU | Loi 27/12/2006 |
| `mp-fedris-reconnaissance` | SF-219-28 | SF-219-28b | Fedris organisme BE — NOUVEAU | AR 28/03/1969 ; AR 16/12/1985 |
| `at-mp-rente-capital-be` | SF-219-29 | SF-219-29b | Mécanisme BE rente/capital — NOUVEAU | Loi 10/04/1971 art. 24 |
| `bien-etre-rps-conseiller-prevention` | SF-219-30 | SF-219-30b | Procédure BE (CISP) — NOUVEAU | Loi 04/08/1996 ; AR 10/04/2014 ; CCT 72 |
| `conge-paternite-naissance-be` | SF-219-31 | SF-219-31b | Régime BE 2023 (20 jours) — NOUVEAU | Loi 03/07/1978 art. 30 ; Loi 07/04/2023 |
| `interruption-carriere-soins-parental` | SF-219-32 | SF-219-32b | Régime ONEM BE distinct — NOUVEAU | CCT 64 ; Loi 22/01/1985 |
