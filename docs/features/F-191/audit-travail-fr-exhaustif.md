# Audit juridique exhaustif — Outils décisionnels Droit du travail France

**Auteur** : LegalCase — automatique (audit F-191)
**Date** : 2026-05-06
**Périmètre** : droit du travail français uniquement (hors Immigration et Famille, hors Belgique).
**Méthode** : départ des **sources juridiques françaises** (Code du travail, CSS, CPC, lois récentes 2017-2025), pas du miroir BE. Les outils FR-only (barème Macron, CPH, CSE, France Travail, abandon poste 2022) sont valorisés à part.
**Sortie** : Tableau A (existant), Tableau B (audit exhaustif), Tableau C (audit F-166 contextualisation), synthèse chiffrée, Top 10.

---

## 1. Contexte et avertissement méthodologique

L'audit BE-Travail (`F-191/audit-be-travail-exhaustif.md`, 2026-05-06) avait été produit en miroir FR puis recadré : *un audit doit partir des sources légales du pays, pas d'une grille qu'on duplique*. Le présent document applique la même règle côté France.

L'écosystème Travail FR a sa propre topologie qu'il faut prendre comme telle :

- **Barème Macron L. 1235-3** (ordonnances 22/09/2017) : forfait d'indemnité licenciement sans cause réelle et sérieuse, pas d'équivalent BE.
- **Conseil de prud'hommes** : juridiction paritaire avec phase conciliation obligatoire (art. L. 1411-1, R. 1454-1) — la BE a un Tribunal du travail (CJ 704) mais pas de phase conciliation paritaire systématique.
- **CSE** (ordonnances 2017) : instance unique fusionnant DP + CE + CHSCT, complètement différente du Conseil d'entreprise + CPPT belge.
- **France Travail** (ex-Pôle Emploi depuis 01/01/2024) : ARE, contestation refus, radiation — équivalent ONEM côté BE mais avec règles très différentes.
- **CSP / CRP** (Contrat de sécurisation professionnelle) : dispositif licenciement éco entreprises < 1 000 salariés, FR-only.
- **Lois récentes 2022-2024** : présomption démission (loi marché travail 21/12/2022 art. L. 1237-1-1), partage de la valeur (loi 29/11/2023), prime PPV (loi pouvoir d'achat 16/08/2022), monétisation 5e semaine (à vérifier).
- **PSE / PDV** : plan de sauvegarde de l'emploi, validation/homologation DREETS (L. 1233-57-1 à L. 1233-57-22).

Toutes les références (articles, dates) sont issues des connaissances générales du modèle. Les références dont le modèle n'est pas certain à 100 % sont annotées **"(à vérifier)"** — un avocat travailliste FR doit confirmer avant de seeder.

Les priorités utilisent l'échelle :

- **P1 — urgence procédurale** : un délai court irréversible expose le client à perdre son droit (saisine CPH 12 mois post-licenciement, contestation rupture conv 12 mois, recours France Travail 2 mois, déclaration AT 24 h employeur, opposition PSE…).
- **P2 — fréquence haute** : situation rencontrée plusieurs fois par mois par tout avocat travailliste FR.
- **P3 — spécificité FR** : pas d'équivalent BE, valeur produit pure (barème Macron, CSE, CSP, abandon poste, France Travail, DREETS).
- **P4 — confort** : utile mais on peut différer sans perte de couverture.

---

## 2. Tableau A — Outils FR Travail existants

Source : migrations Liquibase 105 (seed initial F-IA-04), 106 (ajustement rétrocompat), 109/110/111/112/113/114/132/133/134/140/141/142/143/147/148/149/150/157/158/164/166/175 (seed par feature), 191 (réalignement IDs F-164), 193 (F-165 SF-165-01 : 14 outils ALWAYS_ON → CONTEXTUAL), 194 (suppression doublons F-DT-08/09), 195+196 (rollback trigger F-DT-09 RC), 199 (F-166 SF-166-02 : 8 outils ALWAYS_ON → CONTEXTUAL avec flags niveau 3).

Croisement avec `TOOL_REGISTRY` dans `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts`.

### 2.1 — État final FR Travail au 2026-05-06 (post-F-166)

| tool_id | layer | trigger_field / trigger_value | Frontend câblé (TOOL_REGISTRY) | Situation juridique |
|---|---|---|---|---|
| `F-DT-03-prescription-litige` | ALWAYS_ON | — | OUI | Calculateur de prescription (action salaires 3 ans L. 3245-1, action licenciement 12 mois L. 1471-1, action discrimination 5 ans L. 1134-5, action harcèlement 5 ans). Toujours utile en évaluation de dossier. |
| `F-DT-04-fiche-prudhomale` | ALWAYS_ON | — | OUI | Fiche prud'homale — synthèse pour conseil de prud'hommes (parties, faits, demandes, fondements juridiques). FR-only (pas de fiche CPH en BE). |
| `F-DT-07-anciennete-conges-prime` | ALWAYS_ON | — | OUI | Calcul ancienneté + congés payés L. 3141-3 (2,5 j ouvrables / mois) + prime ancienneté selon CCN (Syntec, BTP, métallurgie, HCR, etc. — barèmes seedés `legal_referentials.CONVENTION_BAREMES`). Toujours utile. |
| `F-DT-08-licenciement-validity` | CONTEXTUAL | `type_rupture` ∈ {LICENCIEMENT, LICENCIEMENT_ECONOMIQUE} | OUI | Analyseur de validité du licenciement — cause réelle et sérieuse (L. 1232-1), procédure (entretien préalable L. 1232-2, lettre L. 1232-6), motif (L. 1232-1, L. 1233-3), sanction barème Macron L. 1235-3. |
| `F-DT-09-comparateur-indemnites` | CONTEXTUAL | `type_rupture` ∈ {LICENCIEMENT, LICENCIEMENT_ECONOMIQUE} | OUI | Comparateur indemnités licenciement : indemnité légale L. 1234-9 / R. 1234-2, indemnité conventionnelle (CCN), barème Macron L. 1235-3 (seedés `INDEMNITE_BAREMES.MACRON`). Note : ne s'affiche **pas** sur RC (rollback migration 196). |
| `F-DT-10-rupture-conv-validity` | CONTEXTUAL | `type_rupture = RUPTURE_CONVENTIONNELLE` | OUI | Validité rupture conventionnelle (L. 1237-11 à L. 1237-16) — entretien, délai 15 j rétractation L. 1237-13, homologation DDETS L. 1237-14. |
| `F-DT-11-harcelement-licenciement-nul` | CONTEXTUAL | `motif_nullite_pressenti` ∈ {HARCELEMENT_MORAL, HARCELEMENT_SEXUEL} | OUI | Harcèlement moral L. 1152-1 / sexuel L. 1153-1 — licenciement nul L. 1152-3, indemnité ≥ 6 mois rémun L. 1235-3-1. |
| `F-DT-12-discrimination-dommages-interets` | CONTEXTUAL | `motif_nullite_pressenti = DISCRIMINATION` | OUI | Discrimination L. 1132-1 — licenciement nul, indemnité ≥ 6 mois L. 1235-3-1, dommages-intérêts non plafonnés. |
| `F-DT-13-licenciement-economique` | CONTEXTUAL | `type_rupture = LICENCIEMENT_ECONOMIQUE` | OUI | Analyseur cause éco (L. 1233-3) — difficultés économiques, mutations technologiques, cessation activité, sauvegarde compétitivité ; ordre des licenciements (L. 1233-5) ; obligation de reclassement (L. 1233-4). |
| `F-DT-14-pse-validite` | CONTEXTUAL | `type_rupture = LICENCIEMENT_ECONOMIQUE` | OUI | Validité PSE (Plan Sauvegarde Emploi) — entreprises ≥ 50 salariés et ≥ 10 licenciements, contenu obligatoire L. 1233-61 à L. 1233-63, validation/homologation DREETS L. 1233-57-1+. |
| `F-DT-15-inaptitude` | CONTEXTUAL | `origine_inaptitude_pressentie` ∈ {ACCIDENT_TRAVAIL, MALADIE_PROFESSIONNELLE, MALADIE_ORDINAIRE} | OUI | Inaptitude médicale art. L. 4624-4 — visite médecin du travail, étude poste, recherche reclassement L. 1226-2 / L. 1226-10 (origine pro), licenciement pour inaptitude. |
| `F-DT-16-licenciement-nul-detection` | CONTEXTUAL | `type_rupture` ∈ {LICENCIEMENT, LICENCIEMENT_ECONOMIQUE} | OUI | Détecteur cas de nullité (statut protégé sans autorisation L. 2411-1+, salariée enceinte L. 1225-4, lanceur d'alerte L. 1132-3-3, accident travail L. 1226-9, etc.). |
| `F-DT-17-indemnite-precarite-cdd` | CONTEXTUAL | `type_contrat = CDD` | OUI | Indemnité de précarité CDD L. 1243-8 — 10 % de la rémunération brute totale (sauf cas exclusions L. 1243-10). |
| `F-DT-18-fin-mission-interim` | CONTEXTUAL | `type_contrat = INTERIM` | OUI | Indemnité fin de mission intérim L. 1251-32 — 10 % de la rémunération brute totale. |
| `F-DT-19-heures-sup` | CONTEXTUAL | `heures_sup_mentionnees = PRESENT` | OUI | Heures supplémentaires L. 3121-28 à L. 3121-37 — majoration 25 % (8 premières) puis 50 %, repos compensateur L. 3121-30. |
| `F-DT-20-rappel-salaire` | CONTEXTUAL | `rappel_salaire_detecte = true` | OUI | **F-166** — Rappel de salaire (heures non payées, primes contractuelles, complément salaire pendant maladie, 13e mois CCN, congés payés non pris). Prescription 3 ans L. 3245-1. |
| `F-DT-21-travail-dissimule` | CONTEXTUAL | `travail_dissimule_detecte = true` | OUI | **F-166** — Travail dissimulé L. 8221-3 à L. 8221-5 — indemnité forfaitaire 6 mois rémun L. 8223-1, sanctions pénales L. 8224-1+. |
| `F-DT-22-requalification-cdd-cdi` | CONTEXTUAL | `type_contrat = CDD` | OUI | Requalification CDD en CDI (L. 1245-1 à L. 1245-2) — motif non écrit, succession irrégulière, durée max dépassée, indemnité ≥ 1 mois L. 1245-2. |
| `F-DT-23-requalification-interim-cdi` | CONTEXTUAL | `type_contrat = INTERIM` | OUI | Requalification mission intérim en CDI auprès utilisateur (L. 1251-39 à L. 1251-40), indemnité ≥ 1 mois. |
| `F-DT-24-non-concurrence` | CONTEXTUAL | `clause_non_concurrence_detectee = true` | OUI | **F-166** — Clause non-concurrence — validité (4 conditions Cass. 10/07/2002 : intérêt légitime, limitation temps/espace/activité, contrepartie financière). Suppression possible par l'employeur dans le délai contractuel. |
| `F-DT-25-indemnite-preavis` | CONTEXTUAL | `type_rupture` ∈ {LICENCIEMENT, LICENCIEMENT_ECONOMIQUE, RUPTURE_CONVENTIONNELLE} | OUI | Indemnité compensatrice de préavis L. 1234-5 — durée selon ancienneté (1 mois 6m-2a, 2 mois ≥ 2a) ou CCN si plus favorable. |
| `F-DT-26-conges-payes-indemnite` | CONTEXTUAL | `type_rupture` ∈ {LICENCIEMENT, LICENCIEMENT_ECONOMIQUE, RUPTURE_CONVENTIONNELLE} | OUI | Indemnité compensatrice de congés payés L. 3141-28 — congés acquis non pris au moment de la rupture. |
| `F-DT-30-protection-rp` | CONTEXTUAL | `statut_protege_detecte = true` | OUI | **F-166** — Statut protégé représentants du personnel (CSE, DS) — autorisation inspecteur du travail L. 2411-1 à L. 2411-22, indemnité forfaitaire si licenciement irrégulier. |
| `F-DT-31-transaction` | CONTEXTUAL | `transaction_envisagee = true` | OUI | **F-166** — Transaction art. 2044 Code civil + L. 1237-11 — concessions réciproques, validité, étendue de la renonciation. |
| `F-DT-32-documents-fin-contrat` | CONTEXTUAL | `type_rupture` ∈ {LICENCIEMENT, LICENCIEMENT_ECONOMIQUE, RUPTURE_CONVENTIONNELLE} | OUI | Documents fin de contrat — solde de tout compte L. 1234-20, certificat travail L. 1234-19, attestation France Travail (ex-Pôle Emploi). |
| `F-DT-33-at-mp` | CONTEXTUAL | `at_mp_detecte = true` | OUI | **F-166** — Accident travail / Maladie professionnelle — déclaration L. 441-2 CSS, IJ majorées, faute inexcusable employeur L. 452-1 CSS. |
| `F-DT-34-refere-prudhomal` | CONTEXTUAL | `urgence_procedurale = true` | OUI | **F-166** — Référé prud'homal R. 1455-5 à R. 1455-12 — urgence, mesures conservatoires, paiement provisionnel salaires. |
| `F-DT-35-contestation-are-fr` | CONTEXTUAL | `contestation_are_envisagee = true` | OUI | **F-166** — Contestation décision France Travail (ex-Pôle Emploi) — refus ARE, montant ARE, radiation. Recours gracieux puis Médiateur puis TA. |
| `F-132-rupture-conv-indemnite` | CONTEXTUAL | `type_rupture = RUPTURE_CONVENTIONNELLE` | OUI (note 1) | Calcul indemnité spécifique de rupture conventionnelle L. 1237-13 — minimum = indemnité légale licenciement, libre au-delà. |

**Note 1** : `F-132-rupture-conv-indemnite` est référencée dans `TOOL_REGISTRY` sous l'ID exact `F-132-rupture-conv-indemnite` (à confirmer croisement registry — le seed migration 105 mentionne ce tool_id). Si le label registry diffère, désambiguïsation à faire en SF.

### 2.2 — Synthèse Tableau A

- **ALWAYS_ON FR Travail (post-F-166)** : 3 outils (F-DT-03, F-DT-04, F-DT-07) — exactement comme prévu par la mini-spec F-166 SF-166-02.
- **CONTEXTUAL FR Travail (post-F-166)** : 25 outils sur 28 distincts.
- **Total effectif FR Travail au 2026-05-06** : **28 outils décisionnels seedés et câblés frontend**.
- **Évolution F-IA-04 → F-165 → F-166** : 25 ALWAYS_ON sur dossier vide à T0 → 11 après F-165 → 3 après F-166 (cohérent avec la philosophie "panel au repos quand l'IA n'a rien détecté").

### 2.3 — Outils backend qui n'ont pas (ou plus) d'aiguillage F-IA-04

Pour mémoire, certains tool_id ont été DELETE par la migration 191 car le composant frontend n'existait pas :

- `F-DT-01-calcul-indemnite-simple` : DELETE migration 191 (composant frontend supprimé/non livré). Service backend conservé mais plus aiguillé.
- `F-DT-05-preavis-be` : DELETE 191 (BE-only, déjà couvert par F-DT-09).

Aucune autre suppression FR Travail. Le panel F-IA-04 affiche bien la liste des 28 outils ci-dessus quand les triggers IA sont satisfaits.

---

## 3. Tableau B — Audit juridique exhaustif FR Travail

Une ligne = une situation juridique distincte qui mérite un outil décisionnel autonome (règle CLAUDE.md *un outil = une situation*). Les outils déjà existants au Tableau A sont signalés **EXISTE**. Les autres sont **MANQUE** avec priorité.

Numérotation suggérée pour les nouveaux outils : `F-DT-36-...` et suivants (les 35 premiers sont consommés).

### 3.1 — Rupture du contrat (LICENCIEMENT, DEMISSION, RC, autres)

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-DT-08-licenciement-validity` | Validité licenciement personnel — cause réelle et sérieuse, procédure, motif | L. 1232-1 à L. 1232-6 ; L. 1235-1 | Analyseur | EXISTE | — | Couvert. |
| `F-DT-09-comparateur-indemnites` | Indemnités licenciement (légale / CCN / Macron) | L. 1234-9 ; R. 1234-2 ; L. 1235-3 | Comparateur | EXISTE | — | Couvert. |
| `F-DT-10-rupture-conv-validity` | Validité rupture conventionnelle individuelle | L. 1237-11 à L. 1237-16 | Analyseur | EXISTE | — | Couvert. |
| `F-132-rupture-conv-indemnite` | Indemnité spécifique RC (≥ indemnité légale) | L. 1237-13 | Calculateur | EXISTE | — | Couvert. |
| `F-DT-13-licenciement-economique` | Cause éco + ordre + reclassement | L. 1233-3 ; L. 1233-4 ; L. 1233-5 | Analyseur | EXISTE | — | Couvert. |
| `F-DT-14-pse-validite` | PSE — entreprises ≥ 50 et ≥ 10 lic | L. 1233-57-1+ ; L. 1233-61+ | Analyseur conformité | EXISTE | — | Couvert. |
| `F-DT-16-licenciement-nul-detection` | Détecteur cas de nullité | L. 1132-1 ; L. 1152-3 ; L. 1225-4 ; L. 2411-1+ | Détecteur | EXISTE | — | Couvert. |
| `F-DT-25-indemnite-preavis` | Indemnité compensatrice préavis | L. 1234-5 | Calculateur | EXISTE | — | Couvert. |
| `F-DT-26-conges-payes-indemnite` | Indemnité compensatrice CP | L. 3141-28 | Calculateur | EXISTE | — | Couvert. |
| `F-DT-32-documents-fin-contrat` | Solde tout compte + certif + attestation France Travail | L. 1234-19 ; L. 1234-20 | Checklist + générateur | EXISTE | — | Couvert. |
| `F-DT-31-transaction` | Transaction post-rupture | art. 2044 C. civ. ; L. 1237-11 | Analyseur validité | EXISTE | — | Couvert. |
| `F-DT-36-licenciement-faute-grave-lourde` | Licenciement faute grave / faute lourde — privation indemnité préavis + IL | L. 1234-1 ; L. 1234-9 ; jurisprudence Cass. soc. | Analyseur | MANQUE | **P2** | Concept FR critique : faute grave = pas de préavis + pas d'IL ; faute lourde = en plus dommages au-delà. Mérite analyseur dédié vs F-DT-08 générique. |
| `F-DT-37-licenciement-cdi-chantier-operation` | Licenciement CDI de chantier / opération — conditions, motif, indemnité | L. 1223-8 à L. 1223-9 | Analyseur + calculateur | MANQUE | P3 FR-only | CCN BTP, ingénierie. Régime spécifique distinct du CDI classique. |
| `F-DT-38-licenciement-fin-cdi-mission` | Fin de CDI de mission (ex-CDD à objet défini) | L. 1242-1+ | Analyseur | MANQUE | P3 | Cas particulier CDI mission. |
| `F-DT-39-prise-acte-rupture` | Prise d'acte de la rupture par le salarié — analyse griefs + chances de succès aux effets licenciement abusif | Cass. soc. 25/06/2003 ; jurisprudence | Scoring + analyseur | MANQUE | **P2 P3 FR-only** | Très fréquent. Si griefs jugés graves → effets licenciement sans cause / indemnité Macron + IL. Sinon → effets démission. Outil scoring proche de `F-DT-10-rupture-conv-validity`. |
| `F-DT-40-resiliation-judiciaire-cph` | Résiliation judiciaire du contrat aux torts de l'employeur | Cass. soc. 16/03/1989 ; L. 1411-1 | Analyseur + générateur conclusions | MANQUE | **P2 P3 FR-only** | Demande au CPH — alternative à la prise d'acte. Pendant la procédure, le salarié reste en poste. Outil distinct prise d'acte / résiliation judiciaire. |
| `F-DT-41-demission-validite-equivoque` | Démission — validité, caractère équivoque, requalification | L. 1237-1 ; jurisprudence "volonté claire et non équivoque" | Analyseur | MANQUE | **P2** | Démission donnée sous pression, par mail, après altercation → équivoque → peut être requalifiée. Cas fréquent. |
| `F-DT-42-abandon-poste-presomption-demission` | Abandon de poste — présomption démission après mise en demeure | L. 1237-1-1 (loi 21/12/2022) ; D. 1237-1+ | Analyseur procédure + détecteur d'irrégularité | MANQUE | **P1 P2 P3 FR-only** | **Loi marché du travail 2022**. Mise en demeure puis présomption démission après 15 j. L'avocat salarié doit pouvoir contester (motifs légitimes). FR-only et très récent. |
| `F-DT-43-rupture-anticipee-cdd` | Rupture anticipée CDD (faute grave, force majeure, accord, inaptitude) | L. 1243-1 à L. 1243-4 | Analyseur + calculateur indemnités L. 1243-4 | MANQUE | P2 | Régime sanctions très spécifique : si rupture irrégulière par employeur = dommages au moins égal aux salaires restants. |
| `F-DT-44-csp-crp-conformite` | CSP (Contrat sécurisation pro) entreprises < 1 000 salariés | L. 1233-65 à L. 1233-70 | Checklist conformité + calculateur ASP | MANQUE | **P2 P3 FR-only** | Proposition obligatoire dans la procédure licenciement éco. ASP (Allocation Spécifique Reclassement) majorée. Pas d'équivalent BE. |
| `F-DT-45-conge-reclassement` | Congé de reclassement entreprises ≥ 1 000 salariés | L. 1233-71 à L. 1233-76 | Checklist + calculateur durée | MANQUE | P3 FR-only | 4 à 12 mois selon entreprise. Réservé aux gros employeurs. |
| `F-DT-46-pdv-rcc` | Plan de départs volontaires / Rupture conventionnelle collective | L. 1237-17 à L. 1237-19-14 | Analyseur conformité | MANQUE | **P2 P3 FR-only** | RCC instituée par ord. 22/09/2017. Procédure très formaliste, validation DREETS. À distinguer du PSE. |
| `F-DT-47-rupture-amiable-art-1193-cciv` | Rupture d'un commun accord hors RC | art. 1193 C. civ. ; jurisprudence | Information + générateur | MANQUE | P3 | Rare en pratique (RC est l'outil dédié) mais existe pour cas particuliers (CDD impossible RC). |
| `F-DT-48-mise-a-pied-disciplinaire` | Mise à pied disciplinaire — durée, procédure, salaire | L. 1331-1 ; jurisprudence | Analyseur | MANQUE | P2 | Souvent confondue avec mise à pied conservatoire. Mérite outil distinct. |

### 3.2 — Indemnités et calculs salariaux (hors rupture)

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-DT-07-anciennete-conges-prime` | Ancienneté + CP + prime ancienneté CCN | L. 3141-3 + CCN | Calculateur | EXISTE | — | Couvert. |
| `F-DT-19-heures-sup` | Heures supplémentaires + repos compensateur | L. 3121-28 à L. 3121-37 | Calculateur | EXISTE | — | Couvert. |
| `F-DT-20-rappel-salaire` | Rappel de salaire global (3 ans) | L. 3245-1 | Calculateur | EXISTE | — | Couvert. |
| `F-DT-49-temps-partiel-requalification` | Temps partiel — requalification en temps plein si non-respect mentions / dépassement durée | L. 3123-6 ; L. 3123-9 | Détecteur + calculateur | MANQUE | **P2** | Cas fréquent : avenant temps plein, heures complémentaires > 1/3, mentions absentes. Indemnité = différence salaires. |
| `F-DT-50-forfait-jours-validite` | Forfait jours — validité, accord collectif, suivi charge travail | L. 3121-58 à L. 3121-66 | Analyseur conformité + calculateur indemnité si nul | MANQUE | **P2 P3 FR-only** | Très contesté — Cass. soc. 29/06/2011 (Syntec). Si forfait nul : rappel HS sur 3 ans. FR-only (pas d'équivalent BE). |
| `F-DT-51-rtt-monetisation` | RTT — monétisation, valorisation | loi 16/08/2022 ; CCN | Calculateur | MANQUE | P3 FR-only (à vérifier) | Loi pouvoir d'achat 2022 a permis la monétisation de RTT. À confirmer pérennité du dispositif. |
| `F-DT-52-prime-partage-valeur-ppv` | Prime de partage de la valeur (ex-prime Macron) | loi 16/08/2022 ; loi 29/11/2023 | Information + checklist exonération | MANQUE | P3 FR-only | Plafond 3 000 € (6 000 € si accord intéressement). Exonération sociale et fiscale conditionnelle. |
| `F-DT-53-interessement-participation` | Intéressement / Participation aux bénéfices | L. 3311-1+ ; L. 3321-1+ | Calculateur + checklist accord | MANQUE | P3 | Loi partage de la valeur 29/11/2023 a étendu les obligations aux entreprises 11-49 salariés. |
| `F-DT-54-prime-anciennete-ccn` | Prime ancienneté pure selon CCN (Syntec, BTP, etc.) | CCN | Calculateur | MANQUE | P3 | Aujourd'hui inclus dans F-DT-07 mais pourrait être éclaté pour clarté avocat. |
| `F-DT-55-13e-mois-prime-vacances` | 13e mois / prime de vacances | CCN | Calculateur | MANQUE | P3 | Nombreuses CCN. À considérer comme paramétrage de F-DT-20 plutôt que outil dédié. |
| `F-DT-56-egalite-salariale-femmes-hommes` | Égalité salariale F/H — index, comparaison | L. 1142-7 à L. 1142-10 ; loi 05/09/2018 | Analyseur écart | MANQUE | **P2 P3 FR-only** | Index égalité pro obligatoire ≥ 50 salariés. Possibilité d'action individuelle en discrimination salariale (5 ans, charge preuve aménagée L. 1144-1). |
| `F-DT-57-frais-professionnels` | Frais professionnels — remboursements obligatoires (L. 1221-1 implicite) | L. 1221-1 ; URSSAF | Calculateur | MANQUE | P3 | Frais télétravail, transport (2/3 IDF), repas (à vérifier). |
| `F-DT-58-tickets-restaurant` | Tickets restaurant — exonération, contribution employeur | L. 3262-1 ; URSSAF | Calculateur | MANQUE | P4 | À éventuellement intégrer F-DT-07. |

### 3.3 — Discrimination, harcèlement, droit pénal du travail

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-DT-11-harcelement-licenciement-nul` | Harcèlement moral/sexuel + nullité | L. 1152-1 ; L. 1153-1 ; L. 1235-3-1 | Analyseur + calculateur | EXISTE | — | Couvert. |
| `F-DT-12-discrimination-dommages-interets` | Discrimination + DI | L. 1132-1 ; L. 1235-3-1 | Analyseur + calculateur | EXISTE | — | Couvert. |
| `F-DT-21-travail-dissimule` | Travail dissimulé (heures non déclarées) | L. 8221-3 ; L. 8223-1 | Analyseur + calculateur 6 mois | EXISTE | — | Couvert. |
| `F-DT-59-harcelement-moral-procedure-interne` | Procédure interne harcèlement — référent CSE, alerte employeur | L. 1153-5-1 ; L. 2314-1 | Checklist conformité employeur | MANQUE | P2 | Distinct de F-DT-11 qui se concentre sur la nullité du licenciement consécutif. Outil **conformité** pour avocat employeur. |
| `F-DT-60-discrimination-sexuelle-orientation` | Discrimination orientation sexuelle / identité genre | L. 1132-1 ; L. 1142-2-1 | Analyseur + calculateur | MANQUE | P3 | Aujourd'hui couvert globalement par F-DT-12. À éclater si volume signal terrain. |
| `F-DT-61-lanceur-alerte-protection` | Protection lanceur d'alerte (loi Sapin II 2016 + loi Waserman 2022) | L. 1132-3-3 ; loi 21/03/2022 | Analyseur + checklist procédure | MANQUE | **P2 P3 FR-only** | Régime renforcé 2022 : externe possible, dommages 10 000 € en cas de représailles. FR-only. |
| `F-DT-62-droit-retrait-danger-grave` | Droit de retrait — danger grave et imminent | L. 4131-1 à L. 4132-1 | Analyseur validité | MANQUE | P3 | Important suite COVID, à confirmer en ressentir un signal. |
| `F-DT-63-obligation-securite-resultat` | Obligation de sécurité résultat de moyens renforcée | L. 4121-1 ; Cass. soc. 25/11/2015 | Analyseur faute employeur | MANQUE | P3 | Cas pratiques harcèlement, AT/MP, burn-out. |
| `F-DT-64-burnout-reconnaissance` | Burn-out — reconnaissance maladie professionnelle hors tableau | L. 461-1 CSS ; tableau 57 ; comité régional reconnaissance MP | Analyseur dossier MP | MANQUE | **P2 P3 FR-only** | Non-inscrit aux tableaux MP — passage par CRRMP. Distinct de F-DT-33-at-mp. |

### 3.4 — Statuts protégés, IRP, négociation collective

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-DT-30-protection-rp` | Statut protégé RP — autorisation IT | L. 2411-1 à L. 2411-22 | Analyseur procédure | EXISTE | — | Couvert. |
| `F-DT-65-elections-cse-conformite` | Élections CSE — calendrier, PAP, vote, contestation | L. 2314-1 à L. 2314-37 | Checklist + analyseur | MANQUE | **P2 P3 FR-only** | CSE = ord. 2017. Très fréquent. Délai contestation 15 j. FR-only (BE a CE distinct). |
| `F-DT-66-negociation-collective-naoa` | NAO annuelle obligatoire — délai, sujets, PV désaccord | L. 2242-1 à L. 2242-8 | Checklist conformité | MANQUE | P3 FR-only | Obligation employeur ≥ 50 salariés. |
| `F-DT-67-accord-entreprise-validite` | Accord d'entreprise — validité, conditions de majorité, dénonciation | L. 2261-7 ; L. 2232-12 | Analyseur validité | MANQUE | P3 | Loi Travail 2016 + ord. 2017 ont changé règles majorité (50 % au lieu de 30 %). |
| `F-DT-68-droit-greve-validite` | Droit de grève — légalité, sanctions abusives | art. préambule Constitution 1946 ; jurisprudence | Analyseur | MANQUE | P3 | Refus de servir, salaires, abus. |
| `F-DT-69-delegation-syndicale-protection` | DS / RSS — désignation, protection, monopole | L. 2143-1+ ; L. 2411-3 | Analyseur | MANQUE | P3 | Inclus partiellement F-DT-30 mais mérite parfois éclatement. |

### 3.5 — Modification, mutation, transfert

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-DT-70-modification-contrat-refus` | Modification du contrat (rémunération, durée travail, qualif) — refus salarié | L. 1222-6 (modification éco) ; jurisprudence "modif unilatérale" | Analyseur + générateur courrier | MANQUE | **P2 P3 FR-only** | Refus → licenciement sur le motif éco invoqué. Très fréquent. FR-only (BE a "acte équipollent à rupture" différent). |
| `F-DT-71-mutation-clause-mobilite` | Mutation — validité clause de mobilité, refus | L. 1221-1 ; jurisprudence | Analyseur clause + scoring refus | MANQUE | **P2** | Refus mutation = faute si clause valide ; sinon motif éco. Tracas régulier. |
| `F-DT-72-transfert-entreprise-l1224-1` | Transfert d'entreprise — maintien contrats | L. 1224-1 | Analyseur conformité + checklist | MANQUE | **P2 P3** | Très fréquent en M&A, externalisation. Maintien automatique des contrats au repreneur. |
| `F-DT-73-mise-a-disposition-salarie` | Mise à disposition (groupe, prêt main d'œuvre) | L. 8241-1 (interdiction prêt non-but lucratif) ; L. 8241-2 (groupe) | Analyseur licéité | MANQUE | P3 | Distinguer prêt légal (groupe, partenariat) vs marchandage L. 8231-1. |
| `F-DT-74-marchandage-pret-illicite` | Marchandage / prêt main d'œuvre illicite | L. 8231-1 ; L. 8241-1 | Détecteur + sanctions | MANQUE | P3 FR | Indemnité salariés + sanctions pénales. |

### 3.6 — Temps de travail, repos, congés spécifiques

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-DT-75-conges-payes-acquisition-arrets-maladie` | CP acquis pendant arrêts maladie (revirement Cass. 13/09/2023) | L. 3141-5 ; CJUE C-518/20 ; loi 22/04/2024 | Calculateur rétroactif | MANQUE | **P1 P2 FR-only** | Loi 22/04/2024 : rattrapage rétroactif jusqu'à 24 mois. Très demandé en 2024-2026. |
| `F-DT-76-conges-evenements-familiaux` | Congés évènements familiaux | L. 3142-1 à L. 3142-5 | Calculateur durée + maintien salaire | MANQUE | P3 | Mariage, naissance, décès, déménagement. CCN parfois plus favorable. |
| `F-DT-77-conge-paternite-maternite` | Congé maternité / paternité — durée, indemnisation, protection | L. 1225-1+ ; L. 1225-35+ ; CSS | Calculateur + analyseur protection | MANQUE | **P2 P3 FR-only** | Maternité 16-46 sem selon enfants ; paternité 25 j depuis 2021. Protection licenciement L. 1225-4. |
| `F-DT-78-conge-parental-education` | Congé parental d'éducation | L. 1225-47 à L. 1225-60 | Calculateur durée + reprise | MANQUE | P3 FR | Jusqu'à 3 ans. CAF + protection. |
| `F-DT-79-conge-proche-aidant` | Congé proche aidant + CAF | L. 3142-16 à L. 3142-27 ; loi 06/03/2020 | Calculateur droits | MANQUE | P3 FR | Récent. À considérer si signal terrain. |
| `F-DT-80-jours-rtt-acquisition` | Jours RTT — acquisition selon accord | accord d'entreprise / CCN | Calculateur | MANQUE | P3 (à vérifier) | Souvent intégré F-DT-19 mais distinct. |
| `F-DT-81-temps-trajet-deplacements` | Temps de trajet domicile-travail vs déplacement professionnel | L. 3121-4 | Analyseur + calculateur compensation | MANQUE | P3 | Cas fréquent télé/itinérants. |
| `F-DT-82-teletravail-accord` | Télétravail — droit au télétravail, accord, indemnité occupation | L. 1222-9 à L. 1222-11 ; ANI 26/11/2020 | Checklist conformité | MANQUE | P2 FR | Très fréquent post-COVID. |
| `F-DT-83-droit-deconnexion` | Droit à la déconnexion | L. 2242-17 7° | Analyseur conformité | MANQUE | P3 FR | Obligation négociation ≥ 50 salariés. |

### 3.7 — Procédure CPH, référé, contentieux

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-DT-04-fiche-prudhomale` | Fiche pour CPH | — | Générateur | EXISTE | — | Couvert. |
| `F-DT-34-refere-prudhomal` | Référé prud'homal | R. 1455-5+ | Analyseur urgence + générateur | EXISTE | — | Couvert. |
| `F-DT-03-prescription-litige` | Prescriptions diverses | L. 1471-1 ; L. 3245-1 ; L. 1134-5 | Calculateur | EXISTE | — | Couvert. |
| `F-DT-84-conciliation-cph-bca` | Phase conciliation obligatoire CPH (BCO/BCA) | R. 1454-7 à R. 1454-12 | Checklist + générateur PV accord | MANQUE | **P2 P3 FR-only** | BCO obligatoire avant le bureau jugement. Pas d'équivalent BE. |
| `F-DT-85-departage-cph` | Départage juge professionnel CPH | R. 1454-29 | Information délais | MANQUE | P3 FR | Cas de partage de voix conseillers. |
| `F-DT-86-appel-cph-cour-appel` | Appel arrêt CPH — délai 1 mois, formalités CA | R. 1461-1+ ; CPC | Calculateur délai + checklist | MANQUE | P2 FR | Spécificités appel social (procédure orale, représentation obligatoire). |
| `F-DT-87-pourvoi-cassation-soc` | Pourvoi en cassation chambre sociale | CPC art. 901+ | Analyseur cas d'ouverture | MANQUE | P3 | Délai 2 mois. Filtres NPC depuis 2017. |
| `F-DT-88-execution-jugement-cph` | Exécution forcée jugement CPH (provisoire, AGS) | art. 514 CPC ; L. 3253-6+ | Checklist + détecteur AGS | MANQUE | **P2** | Très utile car beaucoup d'employeurs en redressement. AGS prend le relais jusqu'à 6 mois salaire. |
| `F-DT-89-saisie-arret-salaire` | Saisie sur rémunération | R. 3252-1+ | Calculateur quotité saisissable | MANQUE | P3 FR | Côté défendeur ou créancier. |
| `F-DT-90-action-de-groupe-discrimination` | Action de groupe discrimination | L. 1134-7 à L. 1134-10 | Analyseur + checklist organisations habilitées | MANQUE | P3 FR | Loi J21 2016. Rare mais structurant. |

### 3.8 — Sécurité sociale, AT/MP, prévoyance

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-DT-33-at-mp` | AT / MP base | L. 411-1+ CSS ; L. 461-1+ CSS | Analyseur | EXISTE | — | Couvert. |
| `F-DT-91-faute-inexcusable-employeur` | Faute inexcusable employeur — majoration rente + préjudices | L. 452-1 à L. 452-5 CSS ; Cass. ass. plén. 24/06/2005 | Analyseur + scoring | MANQUE | **P2** | Aujourd'hui inclus dans F-DT-33 mais mérite éclatement (procédure différente : amiable puis pôle social TJ). |
| `F-DT-92-ij-maladie-cpam` | Indemnités journalières CPAM — calcul, plafond, délai carence | L. 323-1+ CSS ; R. 323-4 | Calculateur | MANQUE | P3 | Côté salarié indépendant ou complément CCN. |
| `F-DT-93-prevoyance-maintien-salaire` | Maintien salaire CCN pendant maladie | L. 1226-1 ; CCN | Calculateur durée + montant | MANQUE | P3 | CCN très variables (Syntec : 100 % 3 mois, etc.). |
| `F-DT-94-mutuelle-obligatoire-anie` | Mutuelle santé entreprise obligatoire (ANI 2013) | L. 911-7 CSS | Checklist conformité | MANQUE | P3 FR | Obligation employeur depuis 2016. |
| `F-DT-95-retraite-progressive-cumul` | Retraite progressive / cumul emploi-retraite | L. 161-22+ CSS | Information + calculateur | MANQUE | P4 | Cas niche. |
| `F-DT-96-protection-sociale-frontaliers` | Protection sociale salariés frontaliers (Suisse, Luxembourg) | règlements CE 883/2004 | Analyseur affiliation | MANQUE | P3 FR | Très demandé Alsace/Lorraine. |

### 3.9 — Inspecteur du travail, contrôles, sanctions employeur

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-DT-97-controle-inspection-travail` | Contrôle inspection du travail — droits et obligations | L. 8112-1+ ; L. 8113-1+ | Checklist conformité + générateur réponses | MANQUE | P3 FR | Suite à PV inspection. |
| `F-DT-98-sanction-administrative-dreets` | Sanctions administratives DREETS (ex-DIRECCTE) | L. 8115-1+ | Analyseur + recours | MANQUE | P3 FR | Amendes administratives. |
| `F-DT-99-document-unique-prevention` | Document unique d'évaluation des risques (DUER) | L. 4121-3 ; R. 4121-1+ | Checklist conformité | MANQUE | P3 FR | Obligation employeur. |
| `F-DT-100-reglement-interieur-validite` | Règlement intérieur — contenu obligatoire, dépôt | L. 1311-1 à L. 1322-4 | Analyseur conformité | MANQUE | P3 FR | Obligation ≥ 50 salariés. |
| `F-DT-101-egalite-pro-index` | Index égalité pro F/H — calcul, publication, sanction | L. 1142-7 à L. 1142-10 | Calculateur + checklist | MANQUE | P3 FR | Aujourd'hui rangé en F-DT-56 mais distinct car obligation conformité employeur (vs action salarié). |
| `F-DT-102-bdese-contenu` | BDESE — base données économiques sociales et environnementales | L. 2312-36 ; L. 2312-21 | Checklist conformité | MANQUE | P3 FR | Obligation entreprises ≥ 50 salariés. |

### 3.10 — Régime spécifique catégoriels

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-DT-103-cadres-forfait-jours-syntec` | Cadres au forfait Syntec | CCN Syntec art. 4 ; L. 3121-58+ | Analyseur conformité | MANQUE | P3 FR | Aujourd'hui couvert par F-DT-50 générique. À éclater pour Syntec si volume. |
| `F-DT-104-vrp-statut` | VRP — statut, indemnité clientèle L. 7313-13 | L. 7311-1 à L. 7313-18 | Analyseur statut + calculateur indemnité | MANQUE | P2 FR-only | Régime spécifique VRP. Indemnité clientèle = en plus de l'indemnité licenciement. FR-only. |
| `F-DT-105-journalistes-statut` | Journalistes pro — clause de cession, conscience | L. 7111-1 à L. 7113-12 | Analyseur clauses | MANQUE | P3 FR-only | Régime spécifique. |
| `F-DT-106-artistes-spectacle-intermittents` | Intermittents du spectacle — annexes 8 et 10 régime ARE | règlement Unedic | Analyseur ouverture droits ARE | MANQUE | P3 FR-only | Inclus partiellement F-DT-35 mais régime distinct intermittents. |
| `F-DT-107-cadres-dirigeants-non-soumis-duree` | Cadres dirigeants — exclusion durée travail | L. 3111-2 | Analyseur statut | MANQUE | P3 FR | Cass. soc. très restrictive. |
| `F-DT-108-particuliers-employeurs-cesu` | Particulier employeur (CESU, garde enfants) | CCN salariés du particulier employeur ; CCN assistants maternels | Calculateur préavis + IL | MANQUE | P2 FR-only | Régime spécifique. Beaucoup de demandes. |
| `F-DT-109-stagiaires-gratification-requalification` | Stagiaire — gratification min, requalification CDI | L. 124-1+ Code éducation ; D. 124-1+ | Analyseur + calculateur | MANQUE | P3 FR | Requalification fréquente si missions hors stage. |
| `F-DT-110-apprentissage-rupture` | Apprentissage — rupture (45 j, accord, faute, force majeure) | L. 6222-18 ; L. 6222-23 | Analyseur validité + calculateur | MANQUE | P3 FR | Régime hybride. |

### 3.11 — Synthèse Tableau B

| Section | EXISTE | MANQUE | dont P1 | P2 | P3 FR-only/spécifique | P4 |
|---|---|---|---|---|---|---|
| 3.1 Rupture | 11 | 13 | 1 (F-DT-42) | 7 | 5 | 0 |
| 3.2 Indemnités hors rupture | 3 | 10 | 0 | 4 | 5 | 1 |
| 3.3 Discrimination/harcèlement/pénal | 3 | 6 | 0 | 3 | 3 | 0 |
| 3.4 IRP/négociation | 1 | 5 | 0 | 1 | 4 | 0 |
| 3.5 Modification/transfert | 0 | 5 | 0 | 3 | 2 | 0 |
| 3.6 Temps/congés | 0 | 9 | 1 (F-DT-75) | 2 | 6 | 0 |
| 3.7 Procédure CPH | 3 | 7 | 0 | 3 | 4 | 0 |
| 3.8 Sécu sociale/AT-MP | 1 | 6 | 0 | 1 | 4 | 1 |
| 3.9 Contrôles employeur | 0 | 6 | 0 | 0 | 6 | 0 |
| 3.10 Régimes catégoriels | 0 | 8 | 0 | 2 | 6 | 0 |
| **TOTAL** | **22** | **75** | **2** | **26** | **45** | **2** |

Ratio **EXISTE / MANQUE = 22/97 ≈ 23 % de couverture juridique réelle** sur le périmètre Travail FR identifié. Les 28 outils seedés en DB couvrent 22 situations distinctes (les autres seeds sont des conventions de naming, pas des situations juridiques distinctes — ex. F-DT-04 et F-DT-32 qui sont des outils transverses).

La couverture est honorable côté **rupture** (~46 %) mais faible côté **temps/congés** (0/9), **modification/transfert** (0/5), **régimes catégoriels** (0/8), **contrôles employeur** (0/6).

---

## 4. Tableau C — Audit F-166 (contextualisation des ALWAYS_ON)

L'utilisateur explicite la question : *F-166 a-t-elle laissé d'autres outils mal classés ALWAYS_ON qui devraient être CONTEXTUAL ?*

Réponse : **après F-166, il ne reste que 3 outils ALWAYS_ON FR Travail** : `F-DT-03-prescription-litige`, `F-DT-04-fiche-prudhomale`, `F-DT-07-anciennete-conges-prime`.

### 4.1 — Audit des 3 ALWAYS_ON résiduels

| Outil ALWAYS_ON | Doit-il rester ALWAYS_ON ? | Si non, flag IA proposé | Preuves textuelles à chercher | Priorité |
|---|---|---|---|---|
| `F-DT-03-prescription-litige` | **OUI ALWAYS_ON** | — | — | — |
| **Justification** | Toute action prud'homale a une prescription. L'avocat évalue toujours les délais en première lecture. Pas de cas où l'outil n'est pas pertinent sur un dossier Travail FR. | | | |
| `F-DT-04-fiche-prudhomale` | **OUI ALWAYS_ON** | — | — | — |
| **Justification** | La fiche prud'homale est l'output type d'un dossier Travail FR — qu'on aille au CPH ou pas, l'avocat veut souvent une synthèse structurée. Couvre tous les dossiers, donc ALWAYS_ON cohérent. **Note honnêteté** : si le dossier est purement informatif (consultation pré-action, conseil employeur sur compliance) la fiche peut être bruit, mais le coût d'affichage est faible (markup léger, pas de calcul). À garder ALWAYS_ON sauf signal terrain contraire. | | | |
| `F-DT-07-anciennete-conges-prime` | **OUI ALWAYS_ON** | — | — | — |
| **Justification** | Calcul ancienneté + congés payés est utile sur tout dossier Travail FR (rupture comme contentieux salarial), valeur ajoutée immédiate (extraction date embauche + calcul automatique). À garder. |  |  |  |

**Verdict 4.1** : les 3 ALWAYS_ON résiduels sont juridiquement justifiés. F-166 a bien fait son travail. Aucune contextualisation supplémentaire à proposer **pour les outils existants**.

### 4.2 — Mais : l'audit (Tableau B) révèle des MANQUES qui mèneraient à de nouveaux ALWAYS_ON candidats à contextualiser dès le seeding

Si les outils manquants sont seedés, il faut décider d'emblée leur layer pour ne pas répliquer le bug staging E-37 (panel rempli de cards blanches). Voici la suggestion par flag IA pour les **20 nouveaux outils prioritaires** (P1+P2+P3 FR-only) :

| tool_id proposé | Layer suggéré au seed | Trigger field | Trigger value | Flag IA niveau (1-3) | Notes |
|---|---|---|---|---|---|
| `F-DT-36-licenciement-faute-grave-lourde` | CONTEXTUAL | `type_rupture` | `LICENCIEMENT` (avec sub-flag `motif_faute_grave_pressenti`) | Niveau 2-3 | Ajouter flag `motif_rupture_pressenti` ∈ {FAUTE_SIMPLE, FAUTE_GRAVE, FAUTE_LOURDE, INSUFFISANCE} dans `TravailExtractedData`. Niveau 2 sur signaux explicites (lettre licenciement, attestation), niveau 3 (LLM) sur contexte (insultes, vol, abandon poste contesté). |
| `F-DT-39-prise-acte-rupture` | CONTEXTUAL | `prise_acte_envisagee` | `true` | Niveau 3 | Nouveau flag boolean. Détection LLM : "le salarié envisage de prendre acte", "saisir le CPH pour griefs employeur", "rupture aux torts de l'employeur". |
| `F-DT-40-resiliation-judiciaire-cph` | CONTEXTUAL | `resiliation_judiciaire_envisagee` | `true` | Niveau 3 | Nouveau flag. Symétrique de F-DT-39. Distinguer : prise d'acte = rupture immédiate. Résiliation judiciaire = saisine en restant en poste. |
| `F-DT-41-demission-validite-equivoque` | CONTEXTUAL | `type_rupture` | `DEMISSION` (avec sub-flag `demission_equivoque_pressentie`) | Niveau 2-3 | Ajouter `DEMISSION` dans enum `type_rupture` (à vérifier qu'il n'y est pas déjà), + flag `demission_equivoque_pressentie` boolean. |
| `F-DT-42-abandon-poste-presomption-demission` | CONTEXTUAL | `abandon_poste_detecte` | `true` | Niveau 2 | Nouveau flag. Niveau 2 sur mention "absence injustifiée" + "mise en demeure" + "présomption démission" + "loi 2022". P1 priorité haute. |
| `F-DT-43-rupture-anticipee-cdd` | CONTEXTUAL | `type_contrat = CDD` AND `rupture_anticipee_cdd_detectee` | `true` | Niveau 3 | Combiner avec type_contrat existant. |
| `F-DT-44-csp-crp-conformite` | CONTEXTUAL | `type_rupture = LICENCIEMENT_ECONOMIQUE` AND `csp_propose_detecte` | `true` | Niveau 2 | Niveau 2 sur mention "CSP" / "Contrat sécurisation pro" / "ASP" dans pièces. |
| `F-DT-45-conge-reclassement` | CONTEXTUAL | `type_rupture = LICENCIEMENT_ECONOMIQUE` AND `entreprise_grand_groupe_detecte` | `true` | Niveau 3 | Niveau 3 sur effectif > 1 000 salariés. |
| `F-DT-46-pdv-rcc` | CONTEXTUAL | `pdv_rcc_envisage` | `true` | Niveau 2-3 | Niveau 2 sur "rupture conventionnelle collective", "PDV", "plan départs volontaires". |
| `F-DT-49-temps-partiel-requalification` | CONTEXTUAL | `temps_partiel_detecte` AND `requalification_temps_plein_envisagee` | `true` | Niveau 3 | Niveau 3 (LLM) : détection mentions "heures complémentaires > 1/3", "avenant temps plein non signé", "mentions absentes". |
| `F-DT-50-forfait-jours-validite` | CONTEXTUAL | `forfait_jours_detecte` | `true` | Niveau 2 | Niveau 2 sur mention "forfait jours" / "convention forfait" dans contrat. |
| `F-DT-56-egalite-salariale-femmes-hommes` | CONTEXTUAL | `discrimination_salariale_pressentie` | `true` | Niveau 3 | Plus large que `motif_nullite_pressenti=DISCRIMINATION` (qui vise nullité licenciement). Ici cible action salariale en cours d'emploi. |
| `F-DT-61-lanceur-alerte-protection` | CONTEXTUAL | `lanceur_alerte_detecte` | `true` | Niveau 3 | Détection LLM : "alerte interne", "signalement", "loi Sapin", "loi Waserman", "représailles". |
| `F-DT-64-burnout-reconnaissance` | CONTEXTUAL | `burnout_detecte` | `true` | Niveau 3 | Mentions "syndrome épuisement", "burn-out", "demande reconnaissance MP", "CRRMP". |
| `F-DT-65-elections-cse-conformite` | CONTEXTUAL | `elections_cse_detectees` | `true` | Niveau 2 | Sur mention "élections CSE", "PAP", "contestation élections". |
| `F-DT-70-modification-contrat-refus` | CONTEXTUAL | `modification_contrat_envisagee` | `true` | Niveau 3 | Mentions "modif rémunération", "changement horaires", "nouvelle qualif", "refus salarié". |
| `F-DT-71-mutation-clause-mobilite` | CONTEXTUAL | `clause_mobilite_detectee` OR `mutation_envisagee` | `true` | Niveau 2-3 | Niveau 2 sur "clause mobilité" dans contrat ; niveau 3 sur "mutation refusée". |
| `F-DT-72-transfert-entreprise-l1224-1` | CONTEXTUAL | `transfert_entreprise_detecte` | `true` | Niveau 3 | Niveau 3 (LLM) : détection "rachat", "fusion", "cession fonds", "L. 1224-1", "reprise activité". |
| `F-DT-75-conges-payes-acquisition-arrets-maladie` | CONTEXTUAL | `arret_maladie_long_detecte` | `true` | Niveau 2 | Niveau 2 sur "arrêt maladie longue durée" + "rappel CP" / "loi 22 avril 2024". P1 priorité (rétroactif 24 mois). |
| `F-DT-77-conge-paternite-maternite` | CONTEXTUAL | `conge_maternite_paternite_detecte` | `true` | Niveau 2 | Niveau 2 sur mention dates congé maternité / paternité dans pièces (bulletins, courriers). |
| `F-DT-82-teletravail-accord` | CONTEXTUAL | `teletravail_litige_detecte` | `true` | Niveau 3 | Détection litige autour télétravail (refus, indemnité occupation, accident). |
| `F-DT-91-faute-inexcusable-employeur` | CONTEXTUAL | `at_mp_detecte = true` AND `faute_inexcusable_envisagee` | `true` | Niveau 3 | Sub-flag de `at_mp_detecte`. Niveau 3 sur signaux gravité : "manquement obligation sécurité", "danger connu", "tribunal pôle social". |
| `F-DT-104-vrp-statut` | CONTEXTUAL | `statut_vrp_detecte` | `true` | Niveau 2 | Niveau 2 sur mention "VRP", "représentant", "indemnité clientèle". |
| `F-DT-108-particuliers-employeurs-cesu` | CONTEXTUAL | `particulier_employeur_detecte` | `true` | Niveau 2 | Niveau 2 sur mentions CESU, garde enfants, employeur particulier. |

### 4.3 — Synthèse Tableau C

- **Outils ALWAYS_ON existants à contextualiser** : 0. F-166 a couvert le périmètre des outils déjà seedés.
- **Nouveaux flags IA à ajouter (extension F-166)** pour les 20 outils prioritaires manquants :
  - 4 nouveaux flags niveau 2 (extraction directe pièces) : `motif_rupture_pressenti` (enum), `csp_propose_detecte`, `forfait_jours_detecte`, `arret_maladie_long_detecte`, `conge_maternite_paternite_detecte`, `statut_vrp_detecte`, `particulier_employeur_detecte`, `elections_cse_detectees`, `clause_mobilite_detectee`, `abandon_poste_detecte`.
  - 14 nouveaux flags niveau 3 (LLM contextuel) : `prise_acte_envisagee`, `resiliation_judiciaire_envisagee`, `demission_equivoque_pressentie`, `rupture_anticipee_cdd_detectee`, `entreprise_grand_groupe_detecte`, `pdv_rcc_envisage`, `requalification_temps_plein_envisagee`, `discrimination_salariale_pressentie`, `lanceur_alerte_detecte`, `burnout_detecte`, `modification_contrat_envisagee`, `mutation_envisagee`, `transfert_entreprise_detecte`, `teletravail_litige_detecte`, `faute_inexcusable_envisagee`.
- **Convention** : ces flags suivent le pattern F-166 (boolean ou enum dans `TravailExtractedData`, alimenté par prompt LLM avec preuves textuelles, consommé par `DecisionToolVisibilityService.extractDetectedSituations`).
- **Volume** : ~24 nouveaux flags pour ~20 nouveaux outils. Découpage SF naturel : 4-5 SFs de 5 outils chacune (1 SF = 1 wave), pour absorber le contrat IA + migration + composants frontend en parallèle backend/frontend.

---

## 5. Top 10 outils manquants prioritaires

Ordre de priorité combinée (priorité juridique × valeur produit × signal terrain) :

1. **`F-DT-42-abandon-poste-presomption-demission`** — P1 récent (loi 21/12/2022), FR-only, contestation employeur fréquente, **gros risque d'erreur sans outil dédié**.
2. **`F-DT-75-conges-payes-acquisition-arrets-maladie`** — P1 (loi 22/04/2024 rétroactive 24 mois), forte demande post-revirement Cass. 13/09/2023.
3. **`F-DT-39-prise-acte-rupture`** — P2, FR-only, scoring chances de succès, fréquent avocat salarié.
4. **`F-DT-40-resiliation-judiciaire-cph`** — P2, FR-only, alternative à prise d'acte.
5. **`F-DT-50-forfait-jours-validite`** — P2, FR-only, contestation très fréquente (Syntec, Cass. 29/06/2011).
6. **`F-DT-72-transfert-entreprise-l1224-1`** — P2, fréquent en M&A.
7. **`F-DT-44-csp-crp-conformite`** — P2, FR-only, central dans licenciement éco PME.
8. **`F-DT-91-faute-inexcusable-employeur`** — P2, à éclater de F-DT-33-at-mp.
9. **`F-DT-70-modification-contrat-refus`** — P2, FR-only, très fréquent.
10. **`F-DT-36-licenciement-faute-grave-lourde`** — P2, à éclater de F-DT-08, distinction faute grave / lourde / simple.

Trois P1 et sept P2 — couvre les situations *urgences procédurales* et *fréquentes* de tout cabinet travailliste.

---

## 6. Découpages à éclater (un outil = une situation)

Application de la règle CLAUDE.md *un outil décisionnel = une situation métier* + `feedback_decision_tools_one_per_situation.md`. Identification des outils existants qui mêlent plusieurs situations distinctes :

| Outil existant | Mélange-t-il plusieurs situations ? | Éclatement proposé |
|---|---|---|
| `F-DT-08-licenciement-validity` | Oui — couvre licenciement personnel ET licenciement éco (trigger 2 valeurs). Or les analyses sont très différentes (cause éco vs personnel, ordre lic, reclassement éco). | Garder F-DT-08 sur lic personnel, créer `F-DT-13-licenciement-economique` (déjà existant ✅), considérer `F-DT-36-licenciement-faute-grave-lourde` séparé. |
| `F-DT-09-comparateur-indemnites` | Acceptable — comparateur est par essence transverse aux types de licenciement. Garder en l'état. | — |
| `F-DT-15-inaptitude` | Trigger 3 valeurs (AT, MP, maladie ordinaire). Les obligations diffèrent (reclassement renforcé pour origine pro L. 1226-10 vs ordinaire L. 1226-2). | Acceptable de garder un seul outil avec branchement interne, mais à surveiller. Si le code de l'outil a un `if origine == AT_MP` partout = signal éclatement. |
| `F-DT-16-licenciement-nul-detection` | Détecteur multi-cas (statut protégé, grossesse, harcèlement, etc.). C'est sa raison d'être. | Acceptable — outil de tri qui aiguille vers les outils spécialisés. |
| `F-DT-30-protection-rp` | Flag boolean unique mais couvre plusieurs catégories protégées (CSE, DS, conseiller pru, etc.). | Acceptable mais à éclater si volume terrain (séparer DS / CSE / conseiller pru). |
| `F-DT-31-transaction` | Outil unique pour transaction post-licenciement. La transaction post-RC ou post-démission a des contours différents. | Considérer éclatement `F-DT-31-transaction-post-licenciement` vs `F-DT-31-transaction-post-rc` selon signal terrain. |
| `F-DT-32-documents-fin-contrat` | Couvre 3 documents distincts. Acceptable car checklist groupée. | — |
| `F-DT-33-at-mp` | Couvre AT, MP, IJ, faute inexcusable. **Mélange situations distinctes**. | À éclater : garder `F-DT-33-at-mp` sur déclaration + IJ ; créer `F-DT-91-faute-inexcusable-employeur` séparé (proposé). |
| `F-DT-35-contestation-are-fr` | Couvre refus ARE, montant, radiation. Acceptable car procédure recours commune. | — |

**Verdict découpages** : 2 éclatements clairs à proposer (F-DT-33 → +F-DT-91 ; F-DT-08 → +F-DT-36 faute grave/lourde). Les autres restent acceptables.

---

## 7. Alignement avec les 7 niveaux de profondeur (CLAUDE.md)

CLAUDE.md définit 7 niveaux de profondeur d'outils décisionnels :
1. Checklist
2. Générateur de document
3. Calculateur
4. Arbre décisionnel
5. Scoring / analyse de validité
6. Comparateur / fourchettes
7. Détection événement déclencheur

Distribution des outils proposés par niveau (sur 75 manquants) :

| Niveau | Outils proposés |
|---|---|
| 1 Checklist | `F-DT-44-csp-crp`, `F-DT-45-conge-reclassement`, `F-DT-65-elections-cse`, `F-DT-66-naoa`, `F-DT-82-teletravail-accord`, `F-DT-99-duer`, `F-DT-100-ri-validite`, `F-DT-102-bdese`, `F-DT-77-mater-pater` partial, `F-DT-94-mutuelle-anie` (~12) |
| 2 Générateur | `F-DT-70-modification-contrat-refus` (générateur courrier), `F-DT-84-conciliation-cph` (PV accord), inclus dans plusieurs (~6) |
| 3 Calculateur | `F-DT-49-temps-partiel-requalification`, `F-DT-51-rtt-monetisation`, `F-DT-54-prime-anciennete`, `F-DT-55-13e-mois`, `F-DT-57-frais-pro`, `F-DT-58-tickets-resto`, `F-DT-75-cp-arrets-mal`, `F-DT-76-conges-evt-fam`, `F-DT-78-conge-parental`, `F-DT-79-proche-aidant`, `F-DT-80-rtt`, `F-DT-86-appel-cph-delai`, `F-DT-89-saisie-arret`, `F-DT-92-ij-cpam`, `F-DT-93-prevoyance` (~17) |
| 4 Arbre décisionnel | `F-DT-72-transfert-l1224-1`, `F-DT-73-mise-disposition`, `F-DT-74-marchandage`, `F-DT-92-frontaliers` (~5) |
| 5 Scoring/validité | `F-DT-36-faute-grave-lourde`, `F-DT-41-demission-equivoque`, `F-DT-42-abandon-poste`, `F-DT-43-rupture-cdd`, `F-DT-46-pdv-rcc`, `F-DT-50-forfait-jours`, `F-DT-67-accord-entreprise`, `F-DT-68-droit-greve`, `F-DT-71-clause-mobilite`, `F-DT-91-faute-inexcusable`, `F-DT-104-vrp`, `F-DT-105-journalistes`, `F-DT-107-cadres-dirigeants` (~15) |
| 6 Comparateur/fourchettes | (peu de candidats nouveaux — F-DT-09 couvre l'essentiel) |
| 7 Détection événement | `F-DT-39-prise-acte` (détection griefs), `F-DT-40-resiliation-judiciaire`, `F-DT-61-lanceur-alerte`, `F-DT-64-burnout`, `F-DT-69-deleg-syndicale` (~7) |

**Garde-fou CLAUDE.md "Parité des domaines métier"** : tout outil de niveau ≥ 5 livré pour Travail FR doit être audité par rapport à Travail BE, Immigration, Famille. Les niveaux 5/7 ci-dessus (15 + 7 = 22 outils) déclencheraient autant de mini-features jumelles à ouvrir au backlog côté Travail BE / Immigration / Famille (à réétudier si concept transposable). C'est un **gros volume** de travail jumeau — à signaler explicitement à la gouvernance produit avant lancement.

---

## 8. Hors périmètre (transparence)

Cet audit ne traite **pas** :

- Les outils Travail BE — voir `audit-be-travail-exhaustif.md` (F-191).
- Le droit du travail public (statuts fonction publique, statuts spéciaux SNCF/RATP/EDF) — non V1.
- Le droit international du travail (détachement, mobilité UE) sauf F-DT-96 mentionné.
- La concurrence déloyale et le débauchage par les anciens employeurs (régime civil pur) — déjà couvert F-DT-24-non-concurrence côté contractuel.
- Le contentieux pénal du travail au-delà du travail dissimulé (homicide involontaire, mise en danger) — relève du droit pénal.
- Les outils de paie pure (bulletin, charges sociales, CSG/CRDS) — relève d'un produit comptable.

---

## 9. Honnêteté méthodologique — références à vérifier

Liste des références dont le modèle n'est pas certain à 100 %. Un avocat travailliste FR doit valider avant tout seed :

- **`F-DT-51-rtt-monetisation`** — pérennité du dispositif loi 16/08/2022 au-delà de 2025 (à vérifier).
- **`F-DT-58-tickets-restaurant`** — montant exonération 2026 (à vérifier).
- **`F-DT-77-conge-paternite`** — durée exacte 25 j post-2021 (à vérifier 25 vs 28).
- **`F-DT-79-conge-proche-aidant`** — durée et indemnisation CAF post-loi 06/03/2020 (à vérifier).
- **`F-DT-101-egalite-pro-index`** — calcul exact 5 indicateurs (à vérifier seuils).
- **Articles `R.` divers** — la version 2026 du Code du travail peut avoir renuméroté certains articles R. (notamment R. 1234-2, R. 1454-7).
- **Nouveau Code du travail** — la recodification de 2008 a modifié toutes les références. Toutes les références « L. xxxx » dans cet audit sont **post-2008**.
- **Loi 22/04/2024 sur CP pendant arrêt maladie** — vérification du périmètre exact rattrapage (24 mois ? prescription d'action ?).

Toute migration `legal_referentials` qui consomme ces références doit ajouter `description` (règle CLAUDE.md F-140 SF-140-03) avec le numéro d'article exact validé par avocat.

---

## 10. Recommandations de découpage en SF / Features

Pour exécuter cet audit, proposer **4 features groupées** (pas 75 outils en vrac — ingérable) :

### F-200 — Travail FR ruptures avancées (P1 + P2)

- F-DT-36 faute grave/lourde, F-DT-39 prise d'acte, F-DT-40 résiliation judiciaire, F-DT-41 démission équivoque, F-DT-42 abandon poste, F-DT-43 rupture CDD, F-DT-44 CSP, F-DT-46 PDV-RCC.
- 8 outils. ~12 SFs (1 backend + 1 frontend par outil + flags IA niveau 2-3 + tests).
- Priorité **P1** : F-DT-42 abandon poste (loi 2022).

### F-201 — Travail FR salaires avancés et CP exceptionnels (P1 + P2)

- F-DT-49 temps partiel, F-DT-50 forfait jours, F-DT-56 égalité salariale, F-DT-75 CP pendant maladie, F-DT-77 maternité/paternité, F-DT-91 faute inexcusable employeur.
- 6 outils. ~9 SFs.
- Priorité **P1** : F-DT-75 (loi 22/04/2024 rétroactive).

### F-202 — Travail FR conflits modification/transfert (P2)

- F-DT-70 modification contrat, F-DT-71 clause mobilité, F-DT-72 transfert L. 1224-1, F-DT-73 mise à dispo, F-DT-74 marchandage.
- 5 outils. ~8 SFs.

### F-203 — Travail FR procédure CPH avancée (P2)

- F-DT-84 conciliation BCO, F-DT-86 appel CA, F-DT-87 cassation, F-DT-88 exécution AGS, F-DT-89 saisie arrêt.
- 5 outils. ~8 SFs.

### Backlog résiduel (P3-P4)

Les ~50 outils P3-P4 non couverts par F-200 à F-203 vont au backlog avec priorité ré-évaluable au signal terrain (volume cabinet, demandes utilisateurs Joëlle SAF, etc.). À ne pas livrer en bloc.

---

## 11. Alignement avec les règles de gouvernance F-IA-04 / F-165 / F-166

Tous les outils nouveaux livrés via F-200 à F-203 doivent suivre la philosophie **F-IA-04** (panel au repos quand l'IA n'a rien détecté) confirmée par F-165 et F-166 :

- Layer **CONTEXTUAL par défaut** au seed (jamais ALWAYS_ON sauf justification écrite).
- Trigger field/value précis branché sur un flag IA existant ou nouveau.
- Si flag IA niveau 3 (LLM), ajouter au prompt système `TravailExtractedData` les preuves textuelles à chercher.
- Test d'intégrité `DecisionToolVisibilityIntegrityIT` (F-164 SF-164-01) doit lister le `tool_id` dans `KNOWN_FRONTEND_TOOL_IDS` ; symétrique côté frontend `TOOL_REGISTRY` (règle CLAUDE.md `feedback_pre_merge_visibility_seed_check`).
- Composant Angular doit suivre le **template canonique** (skill `ai-skills/frontend-coherence-audit.md`), pré-remplissage IA + validation F-IA-03 + `getPrefillCount()` static obligatoires (règle CLAUDE.md « SF frontend décisionnelle mergée sans pré-remplissage IA fonctionnel OU sans validation F-IA-03 = REFUS »).
- Pour tout outil de **niveau ≥ 5** : ouvrir features jumelles backlog côté Travail BE / Immigration / Famille (règle « Parité des domaines métier »).

---

**Fin de l'audit F-191 Travail FR.**

Synthèse d'une phrase : **75 outils manquants identifiés sur 97 situations juridiques distinctes (couverture actuelle ~23 %), dont 2 P1 + 26 P2 prioritaires, 24 nouveaux flags IA à ajouter en extension F-166 ; F-166 a correctement traité les 3 ALWAYS_ON résiduels existants — le travail à faire porte sur les outils manquants (F-200 à F-203 proposés, ~5 sprints).**
