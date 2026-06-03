# F-220 — Cadrage cohérence (étape 0)

> Feature : **F-220 — P3 Immigration FR (longue traîne : accords bilatéraux + Outre-mer + niches de spécificité FR forte)**
> Source d'audit : `docs/features/F-191/audit-immigration-fr-exhaustif.md` (Tableau B, lignes P3/P4).
> Skill appliquée : `ai-skills/feature-coherence-challenger.md`. Modèle : `docs/features/F-222/SF-222-00-coherence.md`.

## Verdict : **GO avec ajustements (trim sévère)** — périmètre ramené de **~25 outils bruts → 6 outils 🟢 à construire + 16 différés/écartés**

Justification synthèse : la cible « ~25 outils » de la ligne PRODUCT_SPEC F-220 a été rédigée **avant** la livraison de F-214 (22 outils, 2026-05-29) et de F-235 (text-trigger CONTEXTUAL, 2026-05-10). Une fois recoupée avec l'existant, une partie significative de la liste F-220 est **déjà livrée** (MNA, assignation à résidence, naturalisation recours TJ/TA, UE/EEE/Suisse, retrait titre fraude) ou **déjà mécaniquement débloquée** (régime algérien CONTEXTUAL). Le reste se répartit entre vraies niches FR à valeur (régimes bilatéraux, Mayotte) et longue traîne marginale à différer signal terrain.

---

## Intention métier (1 phrase)

Compléter le catalogue décisionnel Immigration FR avec les **situations FR-only de longue traîne à spécificité juridique forte** que l'avocat en droit des étrangers rencontre sur des dossiers atypiques (ressortissant d'un pays à accord bilatéral dérogatoire, client en Outre-mer, voie de séjour résiduelle), sans réintroduire de doublon avec les 39 outils Immigration FR déjà livrés.

---

## Workflow métier réel de l'avocat en droit des étrangers FR (source : pratique standard cabinet — ⚠ hypothèse à valider avec un avocat en droit des étrangers)

1. Le client se présente avec une situation administrative (demande de titre, refus, mesure d'éloignement, projet de naturalisation, situation irrégulière…).
2. L'avocat **identifie la nationalité du client** — étape déterminante car certaines nationalités relèvent d'un **régime dérogatoire au CESEDA** (accord franco-algérien 1968 = régime fermé ; accords Tunisie/Maroc/Sénégal = particularités ponctuelles ; citoyens UE/EEE/Suisse = directive 2004/38).
3. L'avocat **identifie le territoire** où vit le client — un titre délivré à Mayotte/Guyane/Saint-Martin n'a pas la même portée qu'un titre hexagonal (régimes des art. L. 832+).
4. L'avocat **qualifie la situation juridique** et la voie applicable (titre, AES, asile, recours, naturalisation).
5. L'avocat **évalue les conditions / droits / délais** de la voie retenue ← **c'est ici que vivent les outils décisionnels**.
6. L'avocat conseille la démarche et identifie les pièces / délais.
7. L'avocat rédige l'acte (demande, recours) et constitue le dossier.
8. Saisine de l'administration / juridiction → décision → éventuel contentieux.

Les outils F-220 se situent **aux étapes 2-3-5** : ce sont des **aiguillages dérogatoires** (la nationalité ou le territoire change la règle applicable) ou des **voies résiduelles** que le catalogue généraliste FR ne couvre pas encore.

---

## Cartographie de l'existant Immigration FR (ce qui est DÉJÀ couvert — à NE PAS reproposer)

**39 outils Immigration FR livrés** (croisement `TOOL_REGISTRY` du `decisional-tools-panel.component.ts` + PRODUCT_SPEC) :

- **Socle F-IM-01→24** (transversaux + P1) : checklist pièces, arbre titre, recours, droit au travail, OQTF avec/sans délai, référés, AES (×4 voies), changement de statut, asile avancé (Dublin/accélérée/réexamen/apatridie/PS), naturalisation (6 voies), **régime algérien (F-IM-17, désormais CONTEXTUAL `nationalite=Algérienne` via F-235)**, mineurs (F-IM-19 : MNA / L.435-3 / DCEM / TIR), mesures d'éloignement (F-IM-20 : expulsion / IRTF / IAT), JLD rétention (F-IM-21), Dublin recours (F-IM-22), CRRV refus visa (F-IM-23), violences conjugales L.425-6 (F-IM-24).
- **F-214 P2 — F-IM-25→46 (22 outils, Terminée 2026-05-29)** : étranger malade L.425-9, regroupement familial, VPF liens personnels L.423-23, validation VLS-TS OFII, OQTF catégories L.611-1, AES présence prouvée, renouvellement délai dépôt, récépissé vs attestation, OFPRA introduction, AJ CNDA, victime traite L.425-1, carte résident L.426-1, ANEF procédure, **MNA évaluation âge (F-IM-38)**, **naturalisation recours TJ (F-IM-39)**, **naturalisation recours TA Nantes (F-IM-40)**, appel CAA / cassation CE, **assignation à résidence (F-IM-42)**, ITF judiciaire, **UE/EEE/Suisse séjour (F-IM-44)**, **retrait titre fraude (F-IM-45)**, autorisation travail employeur.

**Conséquence directe sur le périmètre F-220 brut** : la description PRODUCT_SPEC de F-220 cite encore comme « à faire » des outils **déjà livrés par F-214** : *« MNA mineurs non accompagnés ASE, naturalisation par décret, recours CGT [recours = TJ/TA livrés], JLD assignation à résidence »*. Ces mentions sont **caduques** et ne fondent aucun nouvel outil.

---

## Position de la nouvelle feature

Étapes 2-3-5 du workflow, sur des **branches dérogatoires** (régime selon nationalité / territoire) et des **voies résiduelles** non couvertes. Aucune dépendance amont produit nouvelle : chaque outil est autoportant (l'avocat saisit la situation, l'outil évalue), comme tous les outils décisionnels du catalogue.

---

## Inventaire P3/P4 brut de l'audit (~25 lignes) et classement

Légende : ✅ déjà couvert · ❌ doublon d'un existant (invariant 1 outil = 1 situation) · 🔻 P4/niche différé signal terrain · 🟢 à construire (valeur P3 réelle, spécificité FR forte).

### A. Régimes spéciaux selon nationalité (accords bilatéraux)

| # | Outil brut (audit) | Base | Classement | Motif |
|---|---|---|---|---|
| 1 | `F-IM-17-regime-algerien` (accord 1968) | Accord 27/12/1968 | ✅ déjà couvert | Livré + passé CONTEXTUAL par F-235. Rien à faire. |
| 2 | `regime-tunisien-accord-1988` | Accord franco-tunisien 17/03/1988 | 🟢 **à construire** | Régime dérogatoire réel (étudiant/commerçant/salarié, durées de séjour propres). Nationalité tunisienne fréquente. Mécanique trigger texte déjà prête (F-235). |
| 3 | `regime-marocain-convention-1983` | Convention 09/10/1983 | 🔻 P4 différé | Particularités plus minces que TN/DZ. Le socle généraliste FR couvre l'essentiel pour un Marocain. Différer signal terrain. |
| 4 | `regime-senegalais-accord-2006` | Accord 23/09/2006 (gestion concertée) | 🔻 P4 différé | Très spécifique, volume faible. Différer signal terrain. |
| 5 | `ue-eee-suisse-droit-sejour` | Directive 2004/38 | ✅ déjà couvert | Livré F-214 = `F-IM-44-ue-eee-suisse-sejour-fr`. |
| 6 | `ue-membre-famille-non-ue` | Directive 2004/38 art. 5+10 | ❌ doublon F-IM-44 | Même situation juridique (séjour au titre de la directive 2004/38) — déjà traitée dans F-IM-44 via son encadré « membre de famille non-UE ». Ne pas dédoubler. |
| 7 | `brexit-britanniques-titre-special` | Accord retrait UE 31/01/2020 | 🔻 P4 différé | Stock client en extinction (audit le note). Pas de fréquence. Différer. |

### B. Outre-mer et territoires spécifiques

| # | Outil brut (audit) | Base | Classement | Motif |
|---|---|---|---|---|
| 8 | `regime-mayotte-titre` | Ord. 2014-464 ; L.832-1+ | 🟢 **à construire** | Régime dérogatoire **majeur** (titre Mayotte non valable hexagone) — l'audit le classe « régime dérogatoire majeur, très spécifique ». Vraie valeur : un avocat se trompe sur la portée territoriale du titre. |
| 9 | `regime-saint-martin-titre` | L.832-1+ / L.833-1+ | 🔻 P4 différé | Marginal (audit P4). Volume très faible. Différer. |
| 10 | `regime-guyane-banga-bouedo` | L.832-1+ / L.836-1+ | 🔻 P4 différé | Marginal (audit P4). Différer. |
| 11 | `regime-mayotte-aide-medicale` | CASF ; L.832-1+ | ❌ hors invariant outil décisionnel | Droit social connexe (AME), pas une situation décisionnelle de séjour. Hors périmètre catalogue. Si besoin → rattacher à F-IM-08-Mayotte, pas d'outil dédié. |

### C. Voies de séjour résiduelles (CESEDA)

| # | Outil brut (audit) | Base | Classement | Motif |
|---|---|---|---|---|
| 12 | `vpf-jeune-majeur-l423-22` | L.423-22 (ancien 313-11 2°bis) | 🟢 **à construire** | Situation distincte (16-21 ans entré mineur, scolarisé) — non couverte par F-IM-27 (liens personnels L.423-23) ni F-IM-19 (mineurs, qui s'arrête à la majorité). Transition majorité = trou réel et fréquent (sortie ASE). |
| 13 | `pacs-vpf-eligibilite` | L.423-23 (jurisprudence PACS) | 🟢 **à construire** | Situation distincte du mariage (F-IM-21 couvre conjoint marié) et des liens personnels (F-IM-27). PACS = critères propres (1 an + intensité communauté). Fréquence non marginale. |
| 14 | `passeport-talent-l421-9` (dédié) | L.421-9+ | 🔻 différé | L'audit note que le référentiel des 10 sous-catégories est **déjà absorbé** dans `F-IM-05-arbre-decisionnel-titre`. Un outil dédié = re-découpage cosmétique, pas une nouvelle situation. Différer (V2 F-IM-10). |
| 15 | `salarie-detache-ict-eligibilite` | L.421-26+ ; dir. 2014/66 | 🔻 P3 niche différé | Grands groupes uniquement, volume cabinet faible. Couvert au cadrage par F-IM-05. Différer signal terrain. |
| 16 | `salarie-saisonnier-eligibilite` | L.421-32+ | 🔻 P4 différé | Niche agriculture/hôtellerie. Différer. |
| 17 | `aps-recherche-emploi-l422-10` (dédié) | L.422-10+ | ❌ doublon partiel | Déjà dans F-IM-01 (checklist) + F-IM-11 (changement statut étudiant→salarié). Un outil dédié chevaucherait F-IM-11. Ne pas créer. |
| 18 | `aes-l435-2-soins` | L.435-2 | ❌ doublon F-IM-25 | Voie santé résiduelle redondante avec `F-IM-25-etranger-malade-L425-9` (livré). Invariant 1 situation = 1 outil : la situation « étranger malade » a déjà son outil. Rattacher en mention, pas d'outil. |

### D. Mineurs / nationalité (compléments)

| # | Outil brut (audit) | Base | Classement | Motif |
|---|---|---|---|---|
| 19 | `mna-evaluation-age-isemi` | Cciv 388 | ✅ déjà couvert | Livré F-214 = `F-IM-38-mna-evaluation-age-fr`. |
| 20 | `tutelle-mna-age-juge-enfants` | Cciv 375 ; circ. Taubira | 🔻 P3 niche différé | Procédure tutelle dédiée mineurs isolés — proche de F-IM-38 (évaluation âge) + F-IM-19 (MNA). Risque de chevauchement. Différer signal terrain (préciser périmètre si demandé). |
| 21 | `decheance-nationalite-cciv-25` | Cciv 25 + 25-1 | 🟢 **à construire** | Situation distincte et sensible (terrorisme, fraude), juridiction propre. Rare mais à forte valeur : pas couverte par F-IM-13 (acquisition) ni par les recours naturalisation F-IM-39/40. Outil d'analyse de validité de la mesure + voies de recours. |
| 22 | `apatridie-cciv-21-23-bis` (nationalité) | Cciv 21-23 | ❌ doublon F-IM-12 | Apatridie déjà traitée dans `F-IM-12-asile-avance` (dispositif apatridie L.561-1). Angle Cciv = même situation. Ne pas dédoubler. |

### E. Séjour irrégulier / contentieux résiduel

| # | Outil brut (audit) | Base | Classement | Motif |
|---|---|---|---|---|
| 23 | `delit-sejour-irregulier-l823-1` | L.823-1+ ; Cass. crim. | 🔻 P3 niche différé | Volet pénal informatif, pas une voie de séjour. Faible fréquence côté avocat étrangers (relève du pénal). Différer. |
| 24 | `aide-au-sejour-irregulier-l622-1` | L.823-1+ ; délit solidarité | 🔻 P4 différé | Cas rare (audit P4). Différer. |
| 25 | `signalement-sis-radiation` | Règlement UE 1860/2018 | 🟢 **à construire** | Situation distincte et à valeur : un signalement SIS bloque l'entrée Schengen même titre valide. Procédure de radiation/recours propre, non couverte (connexe IRTF mais ≠ : F-IM-20 traite l'IRTF, pas le signalement SIS lui-même). Fréquence croissante (refoulements aéroport). |

**(Outils P3 « régularisation stratégie » et procédures asile détaillées — `regularisation-irreguliere-strategie`, `procedure-asile-acceleree`, `dublin-transfert-7j` — sont classés P1 dans l'audit, hors périmètre F-220 ; déjà couverts ou relèvent du chantier P1, pas de la longue traîne P3.)**

---

## Challenge amont

Chaque outil 🟢 retenu suppose uniquement que l'avocat ait **identifié la nationalité / le territoire / la situation** (étapes 2-3 du workflow). Aucune brique produit amont nouvelle n'est requise : pas de dépendance à une analyse de dossier préalable ni à un autre outil. Les 6 outils sont **autoportants**. ✅ Aucun trou amont bloquant.

Point d'attention amont **non bloquant** : pour rendre les régimes bilatéraux (TN) et Outre-mer (Mayotte) **CONTEXTUAL** (recommandé — fréquence < 30 %), le moteur de trigger texte sur `nationalite` (F-235) existe déjà ; un trigger territoire (`territoire_residence` / `lieu_demande`) **n'existe pas encore**. Mayotte devra donc soit (a) être ALWAYS_ON FR comme les autres outils non encore basculés, soit (b) attendre une extension trigger territoire (à arbitrer en mini-spec, hors périmètre cadrage). Tunisie peut être CONTEXTUAL `nationalite=Tunisienne` dès maintenant (F-235 prêt).

## Challenge aval

La sortie de chaque outil (régime applicable / portée territoriale du titre / éligibilité de la voie / validité de la mesure + recours) alimente directement le conseil et la rédaction d'actes (étapes 6-7). Citation jurisprudentielle (F-JU) et générateur de recours (F-IM-06) déjà disponibles en aval. ✅ Aucun trou aval bloquant.

---

## STOPs / pré-requis à ajouter au backlog

Aucun **STOP**. Une **dépendance non bloquante** à signaler en mini-spec :

- **Trigger territoire (Mayotte/Outre-mer)** : si l'on veut `regime-mayotte` en CONTEXTUAL, le moteur `extractDetectedSituations` doit exposer un champ territoire (analogue à ce que F-235 a fait pour `nationalite`). À défaut, Mayotte sort en ALWAYS_ON FR (acceptable V1, à noter dans la mini-spec). **Ne pas créer de feature pré-requise** : c'est une décision de visibilité interne à la mini-spec.

---

## Invariants anti-gadget pour les mini-specs

- **1 outil = 1 situation** : chaque outil 🟢 couvre une situation juridique distincte non déjà cartographiée. Tout chevauchement avec F-IM-12/13/19/20/25/27/38/44 est rédhibitoire (vérifier au `TOOL_REGISTRY` avant tout seed).
- **Pas de re-découpage cosmétique** : Passeport Talent, AES soins, APS recherche emploi, membre famille UE, apatridie Cciv = variantes/sous-cas d'outils existants → **écartés**, pas refaits.
- **layer CONTEXTUAL + flag IA bridé** (jamais ALWAYS_ON par défaut, sauf Mayotte si trigger territoire absent) : régime tunisien = `nationalite=Tunisienne` (F-235) ; déchéance = `decheance_nationalite_detectee` ; signalement SIS = `signalement_sis_detecte` ; PACS VPF = `pacs_detecte` ; jeune majeur = `jeune_majeur_ex_mna_detecte`.
- **Pré-fill IA obligatoire** sur tous les champs saisissables ([[feedback_decision_tools_all_fields_prefilled]]) + `static getPrefillCount` parité runtime/static (garde-fou F-237) + F-IA-03 sur le champ pivot.
- **Régimes bilatéraux** : NE PAS reproduire la mécanique fermée de l'accord algérien (régime CRA autonome) pour TN — la Tunisie reste **largement renvoyée au CESEDA** avec particularités ponctuelles. L'outil doit afficher « régime de droit commun CESEDA SAUF [particularités accord 1988] », pas un faux régime parallèle complet.
- **Mayotte** : sortie centrée sur la **portée territoriale** du titre (non valable hexagone) + obligations spécifiques, pas un re-clone de l'arbre titre F-IM-05.
- **Self-check grep pré-commit** obligatoire dans tout brief d'agent SF frontend ([[feedback_self_check_grep_pre_commit]]) + ajout `KNOWN_NO_DASHBOARD_TILE_IDS` / `KNOWN_FRONTEND_TOOL_IDS` ([[feedback_pre_merge_visibility_seed_check]]).

---

## Décision finale

**GO avec ajustements.** Périmètre F-220 net = **6 nouveaux outils décisionnels** (longue traîne FR à spécificité forte). Compte : **~25 bruts → 6 🟢 retenus + 4 ❌ doublons écartés + 3 ✅ déjà livrés (F-214/F-235) + ~12 🔻 différés signal terrain/P4**.

La ligne PRODUCT_SPEC F-220 (« ~25 outils, ~50 SF ») doit être **corrigée** au passage `À planifier → À faire` : mentions caduques à retirer (MNA, naturalisation recours, assignation résidence, UE/EEE/Suisse, régime algérien — tous livrés). Périmètre réel ≈ **6 outils × 2 SF = ~12 SF**.

### Périmètre net trimé — les 6 outils 🟢 à construire

| tool_id proposé | Base juridique | Logique (1 ligne) |
|---|---|---|
| `F-IM-XX-regime-tunisien-fr` | Accord franco-tunisien 17/03/1988 + avenants | Aiguillage : droit commun CESEDA **sauf** particularités de l'accord 1988 (étudiant / commerçant / salarié) ; CONTEXTUAL `nationalite=Tunisienne`. |
| `F-IM-XX-regime-mayotte-fr` | Ord. 2014-464 ; CESEDA L.832-1+ | Analyseur de **portée territoriale** : titre délivré à Mayotte non valable en métropole + obligations dérogatoires ; ALWAYS_ON FR (ou CONTEXTUAL si trigger territoire ajouté). |
| `F-IM-XX-vpf-jeune-majeur-l42322-fr` | CESEDA L.423-22 (ancien L.313-11 2°bis) | Analyseur d'éligibilité VPF jeune majeur 16-21 ans entré mineur + scolarisé (transition sortie ASE) ; CONTEXTUAL `jeune_majeur_ex_mna_detecte`. |
| `F-IM-XX-pacs-vpf-fr` | CESEDA L.423-23 (jurisprudence PACS) | Analyseur d'éligibilité VPF au titre d'un PACS conclu en France (1 an + intensité de la communauté de vie), distinct du mariage ; CONTEXTUAL `pacs_detecte`. |
| `F-IM-XX-decheance-nationalite-fr` | Cciv 25 + 25-1 | Analyseur de validité d'une mesure de déchéance de nationalité + voies de recours (cas terrorisme/fraude) ; CONTEXTUAL `decheance_nationalite_detectee`. |
| `F-IM-XX-signalement-sis-fr` | Règlement UE 1860/2018 ; CESEDA L.312-3 | Procédure de contestation/radiation d'un signalement SIS (blocage entrée Schengen) + recours ; CONTEXTUAL `signalement_sis_detecte`. |

### Différés (signal terrain / P4) — tracés, NON abandonnés

Régime marocain 1983, régime sénégalais 2006, Brexit britanniques, Saint-Martin, Guyane, AME Mayotte, Passeport Talent dédié, salarié détaché ICT, salarié saisonnier, tutelle MNA juge des enfants, délit séjour irrégulier, aide au séjour irrégulier. → à réévaluer au prochain audit de couverture (règle des 10 features) ou sur premier signal terrain d'un avocat (cf. [[feedback_ux_dev_threshold_3_signals]] pour l'UX ; ici seuil 1 signal métier suffit car ce sont des trous de couverture juridique, pas de l'UX).

### Écartés (doublons — invariant 1 outil = 1 situation)

Membre famille UE non-UE (→ F-IM-44), APS recherche emploi (→ F-IM-11), AES soins L.435-2 (→ F-IM-25), apatridie Cciv (→ F-IM-12), AME Mayotte (hors catalogue décisionnel).

**Prochaine étape** : étape 0 bis (cohérence écran) — F-220 est à impact écran (ajoute des cards au panel décisionnel) → produire `SF-220-00b-ux-coherence.md` avant les mini-specs.
