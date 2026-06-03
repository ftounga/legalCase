# F-221 — Cadrage cohérence (étape 0)

## Verdict : **GO avec ajustements** — périmètre ramené de **~30 outils P3 bruts** à **6 nouveaux outils BE-only**

Le reste de la longue traîne est soit déjà couvert (P2 F-215), soit doublon d'un outil livré (invariant « 1 outil = 1 situation »), soit différé P4 / signal terrain.

---

## Intention métier (1 phrase)

Compléter la longue traîne du catalogue décisionnel Immigration BE avec les situations BE-only à valeur réelle qui restent non couvertes après la P2 (F-215), sans reproduire un outil déjà livré ni fabriquer des variantes d'une même situation.

---

## Workflow métier réel de l'avocat en droit des étrangers BE (source : audit F-191 `audit-immigration-be-exhaustif.md` + topologie loi 15/12/1980 — ⚠ hypothèse à valider avec un avocat BE spécialisé étrangers)

1. Le client (ressortissant tiers / citoyen UE / Britannique / MENA) consulte sur un problème de séjour, travail, asile, regroupement, éloignement ou nationalité.
2. L'avocat **qualifie la situation** et le titre / la procédure pertinents (carte A/B/C/F/H, single permit, 9bis/9ter, asile, regroupement 10/40, OQT, détention, nationalité).
3. L'avocat **identifie la juridiction compétente** (OE / CGRA en 1re instance ; CCE en recours ; chambre du conseil pour la détention ; tribunal de la famille pour l'apatridie ; Conseil d'État BE en cassation administrative).
4. L'avocat **évalue les conditions / délais / éligibilité / chances** ← **c'est ici que vivent les outils décisionnels**.
5. L'avocat conseille la démarche (demande, prorogation, recours, requête de mise en liberté).
6. L'avocat constitue le dossier (pièces, annexes) et rédige les actes / requêtes.
7. Dépôt OE / commune / CGRA, ou saisine CCE / chambre du conseil → décision.
8. Le cas échéant, voies de recours (CCE annulation / suspension / extrême urgence, Conseil d'État BE).

---

## Inventaire de départ : outils Immigration BE DÉJÀ LIVRÉS (à ne pas reproposer)

Vérifié dans `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` (TOOL_REGISTRY, 2026-06-03).

**Tableau A (original, 9 dont 5 BE-only)** : `F-IM-08-annexe13-be`, `F-IM-14-9bis-humanitaire-be`, `F-IM-14-9ter-medical-be`, `F-IM-14-40bis-cohabitant-ue-be`, `F-IM-14-40ter-familial-belge-be` + transversaux FR+BE `F-IM-01`, `F-IM-05`, `F-IM-06`, `F-IM-07`.

**P2 F-215 (Terminée — 10 outils BE-only F-IM-25→34)** :
| tool_id | Situation BE couverte |
|---|---|
| `F-IM-25-single-permit-be` | Single permit (autorisation unique travail+séjour, régionalisée) |
| `F-IM-26-regroupement-10ter-be` | Regroupement ressortissant tiers en séjour illimité (art. 10ter) |
| `F-IM-27-regroupement-10bis-be` | Regroupement ressortissant tiers en séjour limité (art. 10bis) |
| `F-IM-28-naturalisation-12bis-be` | Déclaration de nationalité art. 12bis (voie principale) |
| `F-IM-29-naturalisation-conjoint-belge-be` | Acquisition nationalité par mariage avec un Belge (art. 16) |
| `F-IM-30-aesm-mena-be` | AESM / MENA (mineur étranger non accompagné) |
| `F-IM-31-cce-annulation-30j-be` | Recours CCE en annulation — 30 j calendaires |
| `F-IM-32-cce-extreme-urgence-5j-be` | Recours CCE en extrême urgence — 5 j ouvrables |
| `F-IM-33-annexe13quinquies-ie-be` | Annexe 13quinquies = OQT + interdiction d'entrée |
| `F-IM-34-protection-temporaire-ukraine-be` | Protection temporaire Ukraine (directive 2001/55) |

**Conclusion d'inventaire** : la P2 a déjà absorbé **9 des 10 entrées du Top 10** de l'audit. Le périmètre P3 résiduel est donc beaucoup plus mince que les « ~30 outils bruts » annoncés — la plupart sont des doublons ou des variantes des outils ci-dessus.

---

## Cartographie : ~30 outils P3 bruts de l'audit → verdict de trim

Légende : ✅ déjà couvert · ❌ doublon (invariant 1 outil = 1 situation) · 🔻 P4 / niche différé signal terrain · 🟢 à construire (vraie valeur P3 BE-only).

### Titres de séjour ressortissants tiers (§ 3.1)
| Outil brut audit | Verdict | Justification |
|---|---|---|
| `carte-a-prorogation-be` | 🟢 | Prorogation carte A (séjour temporaire) — démarche 30-45 j avant expiration, instruite par la commune. Situation fréquente, **non couverte**, autoportante. |
| `carte-b-illimite-conditions` | 🟢 | Passage carte A → carte B (séjour illimité) après 5 ans, art. 14. Situation distincte (éligibilité au séjour illimité), **non couverte**. |
| `carte-c-installation-conditions` | ❌ | Établissement, largement absorbé par carte B / résident longue durée UE. Conditions floues (« remplacée pour partie » — audit). Pas une situation nette distincte → ne pas fabriquer. |
| `carte-k-l-ue-residence-longue-duree` | 🟢 | Statut résident longue durée UE (directive 2003/109/CE, art. 15bis). Conditions propres (5 ans + ressources + assurance + intégration), **distinct** de la carte B BE. Valeur BE-only. |
| `single-permit-renouvellement` | ❌ | Variante de `F-IM-25-single-permit-be` (même situation = single permit). Le renouvellement = branche conditionnelle de l'outil existant, pas un nouvel outil. |
| `carte-h-brexit` | 🔻 P4 | Régime fermé (inscription close 31/12/2021), stock résiduel décroissant. Différer signal terrain. |
| `carte-fplus-rlue-membre-famille` | 🔻 P4 | Carte F+ = permanence de la carte F après 5 ans ; même situation famille-UE que `F-IM-14-40bis`, simple jalon de durée. Différer. |
| `attestation-immatriculation-annexe-35` / `annexe-15-recepisse` | ❌ | Titres provisoires « informationnels » — relèvent de l'arbre décisionnel titre (`F-IM-05`) et de la checklist (`F-IM-01`) déjà ALWAYS_ON. Pas un outil décisionnel autonome. |

### Regroupement familial (§ 3.2)
| Outil brut audit | Verdict | Justification |
|---|---|---|
| `regroupement-40ter-belge` / `regroupement-40bis-membre-ue` | ✅ | `F-IM-14-40ter` / `F-IM-14-40bis`. |
| `regroupement-10bis` / `regroupement-10ter` | ✅ | `F-IM-27-regroupement-10bis-be` / `F-IM-26-regroupement-10ter-be` (P2). |
| `regroupement-conditions-ressources-1500` | ❌ | Sous-condition (seuil ressources) des regroupements 10bis/10ter/40ter déjà couverts — pas une situation autonome. À traiter comme champ calculé dans les outils existants. |
| `regroupement-condition-logement-conforme` | ❌ | Idem — sous-condition (attestation logement) des regroupements existants. |
| `regroupement-cohabitation-legale-be` | ❌ | Cohabitation légale (art. 1475 CC) = **modalité du lien** dans 40ter/10bis/10ter (déjà couverts), pas une situation distincte. Branche, pas outil. |
| `regroupement-conjoint-violences` (= `victime-violences-conjugales-be` § 3.13) | 🟢 | Maintien du droit au séjour du conjoint regroupé en cas de violences (art. 11 §2 al. 4). **Situation défensive distincte** (rupture du regroupement), non couverte, P1 dans l'audit. |
| `regroupement-mineur-non-accompagne-rejoignant` (= `mineur-belge-naissance` proche) | 🔻 P4 | Mineur rejoignant un parent = variante du regroupement 10 §1 4° ; faible volume distinct. Différer. |

### Procédures humanitaires (§ 3.3)
| Outil brut audit | Verdict | Justification |
|---|---|---|
| `9bis-humanitaire-be` / `9ter-medical-be` | ✅ | `F-IM-14-9bis` / `F-IM-14-9ter`. |
| `9bis-circonstances-exceptionnelles-renforce` / `9bis-circonstances-medicales` / `9bis-enfants-scolarises` / `9bis-presence-prolongee` | ❌ | Spin-offs du **même 9bis** déjà couvert. L'audit lui-même (§ 6.1) tranche : laisser le scoring agrégé, n'éclater que sur retour utilisateur. Violerait l'invariant. |
| `9ter-rapport-medical-type-modele` | 🔻 P4 | Générateur de document type — couplé à `F-IM-14-9ter` (même situation) ; relève d'un enrichissement générateur, pas d'un outil décisionnel autonome. Différer. |
| `aesm-mineur-non-accompagne` / `mena-tutelle-dgde` | ✅ | `F-IM-30-aesm-mena-be` (P2). La tutelle DGDE est le volet protection lié à la même situation MENA. |
| `apatride-procedure-be` | 🟢 | Reconnaissance d'apatridie (loi 27/02/2019) devant le **tribunal de la famille** — situation BE-only, juridiction distincte (judiciaire, pas CCE), **non couverte**. Croissant. |
| `regularisation-2009-historique` | 🔻 P4 | Régime abrogé, stock historique. Différer (audit lui-même = P4). |

### Asile et protection internationale (§ 3.4)
| Outil brut audit | Verdict | Justification |
|---|---|---|
| `protection-temporaire-ukraine-be` | ✅ | `F-IM-34-protection-temporaire-ukraine-be` (P2). |
| `asile-protection-subsidiaire-be` | 🟢 | Statut protection subsidiaire (art. 48/4, loi 18/12/2016) — alternative quand réfugié refusé. Situation **distincte** du statut réfugié, BE-only sur la mise en œuvre, **non couverte**. P2 dans l'audit. |
| `demande-asile-cgra-procedure` / `cgra-procedure-asile` | 🔻 P4 | Checklist procédurale 1re instance CGRA — relève de `F-IM-01` (pièces) + `F-IM-05`. Faible valeur décisionnelle (pas de calcul / éligibilité tranchée). Différer. |
| `dublin-iii-be-determination` / `transfert-dublin-be` | 🔻 P4 | Dublin = règlement UE 604/2013 identique aux 27 ; recours déjà gérable via `F-IM-31`/`F-IM-32` (CCE). Spécificité BE faible. Différer signal terrain. |
| `asile-procedure-acceleree-be` | 🔻 P4 | Calculateur de délais accélérés — recoupe largement les recours CCE existants. Différer. |
| `asile-renvoi-cgra-reexamen` / `asile-pays-origine-sur-be` / `asile-femme-traitement-genre` / `asile-mineur-non-accompagne` | 🔻 P4 | Niches asile (réexamen, liste pays sûrs, genre, MENA asile). Volume / spécificité décisionnelle faibles ou couverts par MENA (`F-IM-30`). Différer signal terrain. |

### Éloignement, détention, interdiction d'entrée (§ 3.5)
| Outil brut audit | Verdict | Justification |
|---|---|---|
| `annexe-13-oqt-be` | ✅ | `F-IM-08-annexe13-be`. |
| `annexe-13quinquies-oqt-interdiction-entree` | ✅ | `F-IM-33-annexe13quinquies-ie-be` (P2). |
| `cce-recours-annulation-30j` / `cce-recours-extreme-urgence-5j` | ✅ | `F-IM-31-cce-annulation-30j-be` / `F-IM-32-cce-extreme-urgence-5j-be` (P2). |
| `cce-recours-suspension-procedure` | 🟢 | Recours CCE **en suspension** (référé administratif, art. 39/82) — procédure et conditions (urgence + préjudice grave) **distinctes** de l'annulation (30j) et de l'extrême urgence (5j ouvrables). L'audit (§ 6.2) tranche explicitement : 3 recours distincts, ne pas fusionner. Le 3e n'est **pas** couvert. |
| `detention-centre-ferme-be` + `detention-recours-chambre-conseil-be` | 🟢 | Détention administrative en centre fermé + requête de mise en liberté devant la **chambre du conseil** (art. 71+, délai 5j) — **catégorie entière non couverte**, BE-only (≠ JLD/CRA FR), critique en urgence. **1 situation** = détention + son recours (fusionner les deux entrées audit). |
| `interdiction-entree-be-recours-art-74-12` | ❌ | Recours en **levée** de l'IE — même situation que l'interdiction d'entrée déjà couverte par `F-IM-33-annexe13quinquies-ie-be`. Branche, pas outil. |
| `expulsion-art-20-22-be` | 🔻 P4 | Expulsion (>10 ans de séjour, ordre public grave + Commission consultative) — cas rare, distinct de l'OQT. Différer signal terrain. |
| `non-refoulement-art-3-cedh-be` | ❌ | Argumentaire art. 3 CEDH transversal — s'intègre aux recours OQT/expulsion/CCE existants, pas une situation autonome. |
| `cce-pourvoi-conseil-etat-be` | ❌ | Cassation administrative Conseil d'État BE — déjà couverte par `F-IM-06-recours` (type `RECOURS_CE_BELGIQUE` seedé). |

### Citoyens UE / Brexit (§ 3.7)
| Outil brut audit | Verdict | Justification |
|---|---|---|
| `citoyen-ue-droit-sejour-3-mois` / `attestation-enregistrement` | 🔻 P4 | Cas simples / informationnels (séjour court UE, attestation E). Faible valeur décisionnelle. Différer. |
| `citoyen-ue-fin-droit-sejour-charge-publique` | 🔻 P4 | Contentieux fin de séjour UE (art. 42bis) — niche. Différer signal terrain. |
| `carte-h-brexit-conditions` | 🔻 P4 | Doublon du `carte-h-brexit` § 3.1 déjà différé (régime fermé). |

### Étudiants (§ 3.8)
| Outil brut audit | Verdict | Justification |
|---|---|---|
| `etudiant-autorisation-sejour-art-58` (Annexe 47) | 🟢 | Autorisation de séjour étudiant (art. 58, Annexe 47) — situation fréquente, conditions propres (inscription + ressources ~671 €/mois + prise en charge), BE-only, **non couverte**. |
| `etudiant-prorogation-r58` | ❌ | Prorogation = branche conditionnelle de la **même situation** « séjour étudiant » → intégrer à l'outil étudiant ci-dessus (condition résultats académiques), pas un outil séparé. |
| `etudiant-changement-statut-fin-etudes` | 🔻 P4 | Transition étudiant → single permit ; recoupe `F-IM-25-single-permit-be`. Différer signal terrain. |
| `etudiant-aps-recherche-emploi` / `etudiant-ressources-tiers-garant` (Annexe 32) / `etudiant-droit-travail-be` (20h) | 🔻 P4 / ❌ | APS post-études = niche (différer) ; garant Annexe 32 + droit travail 20h = sous-conditions de la situation étudiant (intégrer / `F-IM-07`), pas des outils. |

### Travail des étrangers (§ 3.9)
| Outil brut audit | Verdict | Justification |
|---|---|---|
| `single-permit-procedure-complete` / `conditions-emploi-tension` / `cadre-haut-niveau` | ❌ | Même situation que `F-IM-25-single-permit-be` (métiers en pénurie + carte bleue UE = branches/variantes). Ne pas re-fabriquer. |
| `carte-professionnelle-independant-be` | 🟢 | Carte professionnelle indépendant non-UE (loi 19/02/1965, régionalisée) — situation **distincte** du single permit (salarié) : activité indépendante. BE-only, non couverte. |
| `detachement-limosa-be` / `permis-saisonnier-be` | 🔻 P4 | Côté employeur / cas rares en cabinet généraliste (audit = P4). Différer. |

### Nationalité (§ 3.10)
| Outil brut audit | Verdict | Justification |
|---|---|---|
| `nationalite-declaration-art-12bis` | ✅ | `F-IM-28-naturalisation-12bis-be` (P2). |
| `nationalite-conjoint-belge-art-16` | ✅ | `F-IM-29-naturalisation-conjoint-belge-be` (P2). |
| `nationalite-naturalisation-art-19-21` | 🔻 P4 | Voie discrétionnaire (décret Chambre) « très rare » (audit). Différer. |
| `nationalite-mineur-art-11-13` / `recouvrement-art-24` / `perte-art-22-23` / `deux-nationalites` | 🔻 P4 | Cas rares / informationnels / compétence État d'origine (audit = P3/P4 faible). Différer. |
| `naturalisation-langue-niveau-a2-be` | ❌ | Sous-condition (preuve langue A2) de la déclaration 12bis déjà couverte (`F-IM-28`). Branche, pas outil. |

### Procédures / juridictions (§ 3.11) — voir aussi détention/CCE ci-dessus
| Outil brut audit | Verdict | Justification |
|---|---|---|
| `cce-recours-annulation-procedure` / `extreme-urgence-procedure` | ✅ | `F-IM-31` / `F-IM-32`. |
| `cce-recours-suspension-procedure` | 🟢 | Voir § 3.5 (3e recours CCE distinct, retenu). |
| `oe-recours-gracieux-be` / `competence-juridiction-be-immigration` / `delais-be-jours-ouvrables-vs-calendaires` | ❌ / 🔻 | Recours gracieux inexistant en BE (informationnel) ; arbre de compétence relève de `F-IM-05` ; calculateur délais = service technique partagé déjà utilisé (`BelgianBusinessDaysCalculator`), pas un outil utilisateur. |

### Court séjour / visas (§ 3.12)
| Outil brut audit | Verdict | Justification |
|---|---|---|
| `visa-c-court-sejour-90j` / `visa-d-long-sejour-procedure` | 🔻 P4 / ❌ | Compétence consulaire, informationnel ; visa D = préalable couvert par l'arbre titre `F-IM-05`. |
| `visa-c-refus-recours` | 🔻 P4 | Recours CCE contre refus visa C (60j) — niche ; le mécanisme de recours CCE est déjà outillé (`F-IM-31`). Différer signal terrain. |
| `regularisation-overstay-be` | ✅ | Couvert via `F-IM-14-9bis-humanitaire-be`. |

### Autres situations spécifiques (§ 3.13)
| Outil brut audit | Verdict | Justification |
|---|---|---|
| `victime-traite-etres-humains-be` | 🟢 | Titre de séjour spécifique victime de traite (art. 61/2+, circulaire 26/09/2008, partenariat PAG-ASA/Sürya/Payoke) — situation **distincte**, BE-only, protectrice, non couverte. NB : le pendant FR `F-IM-35-victime-traite-l4251-fr` existe → confirme la pertinence métier, mais le régime BE est propre. |
| `victime-violences-conjugales-be` | 🟢 | = `regroupement-conjoint-violences` (§ 3.2), retenu une seule fois. |
| `regroupement-cite-mariage-blanc` | 🔻 P4 | Outil défensif (soupçon mariage blanc) — niche contentieuse. Différer signal terrain. |
| `attestation-immatriculation-effets-be` | ❌ | Informationnel (effets Annexe 35) — relève de `F-IM-05`/`F-IM-01`. |
| `integration-cours-civique-region` | 🔻 P4 | Parcours d'intégration régionalisé (Wallonie/Flandre/Bruxelles) — préalable parfois à 12bis ; informationnel régionalisé. Différer. |

---

## Position de la nouvelle feature

Étape 4 du workflow (évaluation conditions / délais / éligibilité) pour les **6 situations BE-only retenues**, toutes en aval de la qualification (étape 2) déjà outillée par `F-IM-05` (arbre titre) et `F-IM-01` (pièces).

---

## Challenge amont

Chaque outil retenu suppose uniquement que l'avocat ait **qualifié la situation** (étape 2) — couverte par `F-IM-05-arbre-decisionnel-titre` (ALWAYS_ON) et `F-IM-01-checklist-pieces` (ALWAYS_ON). Aucun outil retenu ne dépend d'une autre brique produit absente : tous sont **autoportants** (l'avocat saisit la situation, l'outil évalue). ✅ Aucun trou amont bloquant.

## Challenge aval

La sortie (éligibilité / conditions remplies / délai de recours / chances) alimente le conseil et la rédaction d'actes. Les recours (CCE suspension, requête de mise en liberté) s'appuient sur le générateur de recours `F-IM-06` déjà livré. Citation jurisprudentielle BE : pas de dépendance bloquante (cf. mémoire `reference_be_jurisprudence_sources` — sourcing BE parké, mais les outils décisionnels n'en dépendent pas pour fonctionner). ✅ Pas de trou aval bloquant.

---

## STOPs / pré-requis à ajouter au backlog

Aucun STOP. Aucun pré-requis amont manquant : F-221 s'appuie intégralement sur les briques transversales (`F-IM-05`, `F-IM-01`, `F-IM-06`) et la P2 (F-215) déjà livrées.

---

## Invariants anti-gadget pour les mini-specs

- **1 outil = 1 situation** : aucun des 6 outils retenus n'est une variante / sous-condition / spin-off d'un outil livré. Toutes les variantes détectées (renouvellement single permit, prorogation étudiant, levée IE, sous-conditions ressources/logement/langue, spin-offs 9bis, métiers tension, carte bleue) sont écartées comme branches conditionnelles, pas comme outils.
- **Détention = 1 seul outil** couvrant la détention centre fermé **et** sa requête de mise en liberté chambre du conseil (les deux entrées audit fusionnées) — sortie = conditions de détention + délai + base du recours.
- **CCE suspension = outil distinct** des deux recours CCE déjà livrés (annulation 30j / extrême urgence 5j) ; cadrer nettement les conditions propres (urgence + risque préjudice grave) pour ne pas chevaucher l'extrême urgence (`F-IM-32`).
- **Apatridie = juridiction judiciaire** (tribunal de la famille), pas CCE — l'outil doit l'expliciter pour éviter le déclinatoire de compétence.
- **CONTEXTUAL + flag IA bridé** : chaque nouvel outil arrive en CONTEXTUAL, déclenché par un boolean IA `false` par défaut (jamais `null`), pattern F-166 ; pas d'ALWAYS_ON (éviter la surcharge du panel BE déjà dénoncée Tableau C de l'audit).
- **Pré-fill IA (F-246)** : tout champ saisissable pré-rempli par l'IA, seule exception = info absente des documents.
- **Pas de flag orphelin** : chaque boolean IA est livré avec son outil (jamais de flag sans outil ni d'outil sans flag).
- **Anti-doublon explicite** : carte B (séjour illimité ressortissant tiers) vs carte K/L (résident longue durée UE) sont deux situations distinctes à cadrer sans chevauchement dans leurs mini-specs respectives.

---

## Décision finale — périmètre net trimé

**GO avec ajustements.** Brut audit P3 « ~30 outils annexes + régimes BE » → **6 nouveaux outils BE-only** retenus. Le reste : ~14 déjà couverts (Tableau A + P2 F-215), ~13 doublons/branches écartés (invariant 1 outil = 1 situation), ~20+ différés P4 / signal terrain (documentés ci-dessus).

| tool_id proposé | Base juridique BE | Logique (1 ligne) |
|---|---|---|
| `F-IM-47-carte-a-prorogation-be` | Loi 15/12/1980 art. 13 ; AR 08/10/1981 art. 33 | Calcule le délai (30-45 j avant expiration) et vérifie les conditions de prorogation de la carte A (motif persistant), instruite par la commune. |
| `F-IM-48-carte-b-sejour-illimite-be` | Loi 15/12/1980 art. 14 ; AR 08/10/1981 | Évalue l'éligibilité au passage carte A → carte B (séjour illimité) après 5 ans de séjour régulier + attache. |
| `F-IM-49-residence-longue-duree-ue-be` | Loi 15/12/1980 art. 15bis ; directive 2003/109/CE | Évalue le statut de résident longue durée UE (5 ans séjour légal + ressources stables + assurance maladie + intégration) — distinct de la carte B. |
| `F-IM-50-detention-centre-ferme-be` | Loi 15/12/1980 art. 7 al. 3, 27, 29, 71+, 74/5 ; AR 02/08/2002 | Calcule la durée / prolongation de détention administrative et cadre la requête de mise en liberté devant la chambre du conseil (délai 5 j). |
| `F-IM-51-cce-suspension-be` | Loi 15/12/1980 art. 39/82 ; loi 15/09/2006 | Évalue les conditions du recours CCE en suspension (référé administratif : urgence + risque de préjudice grave) — distinct de l'annulation 30j et de l'extrême urgence 5j. |
| `F-IM-52-victime-traite-be` | Loi 15/12/1980 art. 61/2+ ; circulaire 26/09/2008 | Évalue l'éligibilité au titre de séjour spécifique « victime de traite des êtres humains » (coopération + rupture avec le réseau + accompagnement centre spécialisé). |

**Différés réévaluables sur signal terrain (P4 / niche)** — non perdus, tracés ici : apatridie (tribunal famille), étudiant Annexe 47 (à reconsidérer si volume), carte professionnelle indépendant, protection subsidiaire asile, conjoint regroupé victime de violences (art. 11 §2 al. 4), carte H Brexit, carte F+, expulsion art. 20-22, Dublin/accélérée/réexamen asile, parcours d'intégration régionalisé, mariage blanc défensif, visa C refus, naturalisation voies rares (19-21 / 11-13 / 24 / 22-23).

**Note de trim** : deux situations à réelle valeur métier (apatridie loi 27/02/2019 et étudiant Annexe 47) sont laissées en différé plutôt qu'au périmètre F-221 par réalisme (250+ outils déjà livrés, pas de signal terrain BE étudiant/apatride). Elles sont **candidates n°1 à un éventuel F-221 bis** si un signal terrain émerge — à ne pas re-auditer de zéro.

Passage suggéré du statut F-221 : `Backlog` → `À faire` sur les 6 outils ci-dessus. Enchaîner étape 0 bis (cohérence écran) puis les mini-specs.
