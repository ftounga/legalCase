# F-223 — Cadrage cohérence (étape 0)

> Skill appliquée : `ai-skills/feature-coherence-challenger.md`. Modèle de structure : `docs/features/F-222/SF-222-00-coherence.md`.
> Source d'audit : `docs/features/F-191/audit-famille-be-exhaustif.md` (Tableau B exhaustif, items classés P3 / P3 BE-only).

## Verdict : **GO avec ajustements** — périmètre brut ramené de **~37 items P3 de l'audit** à **9 outils neufs + 1 extension**

Le compte historique de 30 outils inscrit au PRODUCT_SPEC F-223 est un **majorant non trié** issu de l'audit. Après application de l'invariant « 1 outil = 1 situation », retrait des doublons avec les outils déjà livrés par **F-211 + F-217** (15 outils Famille BE en prod), fusion des variantes d'une même situation et différé P4 sur signal terrain, le périmètre net tombe à **9 🟢 + 1 extension**.

## Intention métier (1 phrase)
Compléter la longue traîne du droit de la famille belge avec les situations **BE-only sans aucun équivalent FR** restantes (cohabitation légale, kafala, GPA, régime algérien, DIP familial, état civil réformé, mandat extra-judiciaire), une fois les P1/P2 fréquence haute déjà livrés par F-211/F-217.

## Workflow métier réel de l'avocat famille BE (source : pratique standard cabinet belge — ⚠ hypothèse à valider avec un avocat famille BE)
1. Le client consulte sur une situation familiale (union, séparation, succession, proche vulnérable, situation internationale…).
2. L'avocat **qualifie la situation et identifie la matière** (TF — Tribunal de la famille, compétence quasi-unique CJ art. 572bis).
3. L'avocat **vérifie le rattachement / la reconnaissance** lorsqu'un élément d'extranéité existe (mariage étranger, talaq, kafala, défunt à l'étranger) ← spécificité très fréquente en BE.
4. L'avocat **évalue les conditions, droits, options, validité et montants** applicables ← **c'est ici que vivent les outils décisionnels**.
5. L'avocat conseille la mesure / la procédure / le régime adapté.
6. L'avocat rédige les actes (déclaration officier état civil, requête TF, convention, mandat) et constitue le dossier.
7. Saisine TF / officier état civil / notaire → décision ou enregistrement.

## Cartographie : ce que F-211 + F-217 ont DÉJÀ livré côté Famille BE (anti-doublon)
15 outils Famille BE sont **en production** dans `TOOL_REGISTRY` (`decisional-tools-panel.component.ts`), confirmés par migrations 224-238 :

| Outil live | Situation couverte | Bloquant pour F-223 |
|---|---|---|
| `divorce-dc-be` | Divorce par consentement mutuel (CJ 1287+) | couvre 3.2 DC |
| `divorce-ddi-3voies-be` | DDI 3 voies (consensuelle 6 mois / unilatérale 1 an / faits) | couvre 3.2 DDI + remplace F-FA-11 partiel |
| `F-FA-11-desunion-irremediable-be` | DDI consensuelle (legacy, absorbé par le 3-voies) | — |
| `tribunal-famille-be-mesures-prov` | Mesures provisoires référé familial (CJ 1253ter/2) | couvre 3.3 / 3.9 MP |
| `regime-mat-be-communaute-legale` | Communauté légale post-2018 (Livre 3) | couvre 3.5 base |
| `liquidation-partage-be` | Liquidation-partage notaire commis (CJ 1207+) | couvre 3.5 / méthode notaire commis |
| `autorite-parentale-be` | AP conjointe vs exclusive | couvre 3.3 AP |
| `contribution-alimentaire-enfants-be` | Contribution enfants (méthode Renard) | couvre 3.3 |
| `contribution-conjoint-be` | Pension entre époux (CC art. 301) | couvre 3.3 |
| `succession-be-devolution-reserve` | Dévolution + réserve 1/2 (réforme 2017) | couvre 3.6 dévolution/réserve |
| `succession-be-acceptation-renonciation` | Acceptation / renonciation (délai 4 mois) | couvre 3.6 |
| `pacte-successoral-be-2018` | Pacte successoral (loi 31/07/2017) | **couvre 3.6 pacte successoral** |
| `protection-majeur-be` | Administrateur personne/biens (loi 17/03/2013) | couvre 3.7 base |
| `mariage-etranger-be-reconnaissance` | Reconnaissance mariage étranger **dont talaq** | **couvre 3.1 + 3.10 talaq** |
| `contestation-filiation-be` | Contestation filiation/paternité (CC 318) | couvre 3.4 contestation |

**Conséquence directe sur le périmètre annoncé** : le PRODUCT_SPEC F-223 listait « méthode Renard, notaire commis, talaq, pacte successoral » → **tous déjà livrés par F-217**. Ils sont retirés du périmètre F-223 (doublons).

## Position de la nouvelle feature
Étape 4 du workflow (évaluation conditions/droits/validité) pour la longue traîne BE-only restante. Tous les outils sont **autoportants** : l'avocat saisit la situation qualifiée, l'outil évalue. Aucune brique produit amont à créer.

## Inventaire brut P3 de l'audit → tri
Les ~37 situations classées P3 (et variantes P2 P3 / P1 P3 / P3 BE-only) du Tableau B, triées :

### 3.1 — Mariage / cohabitation
| Item audit | Verdict | Justification |
|---|---|---|
| `mariage-be-reconnaissance-mariage-etranger` | ✅ couvert | = `mariage-etranger-be-reconnaissance` (F-217) |
| `mariage-be-empêchements` | 🔻 P4 différé | cas rares en consult pure (l'audit lui-même le dit) ; valeur faible isolée |
| `mariage-be-validite-formelle` | ❌ doublon | même situation que l'annulation (validité = condition de l'annulation) → fusionner |
| `mariage-be-annulation` (P2) | 🟢 à construire | situation distincte, demande réelle (mariages forcés, qualité fictive) ; **absorbe** validité formelle |
| `cohabitation-legale-be-formation` | 🟢 à construire | **BE-only**, aucun équivalent FR (≠ PACS) |
| `cohabitation-legale-be-effets` | ❌ fusion | même situation « régime de la cohabitation légale » que formation/dissolution → 1 seul outil multi-vues |
| `cohabitation-legale-be-dissolution` | ❌ fusion | idem → fusionné dans l'outil cohabitation légale unique |
| `cohabitation-fait-be-effets` | 🔻 P4 différé | « rien ne s'applique automatiquement » = arbre informatif faible valeur ; signal terrain |

### 3.2 — Divorce / séparation
| Item audit | Verdict | Justification |
|---|---|---|
| DC / DDI (toutes voies) | ✅ couvert | `divorce-dc-be` + `divorce-ddi-3voies-be` (F-217) |
| `separation-corps-be` (P3) | 🔻 P4 différé | dispositif « rare mais juridiquement vivant » (audit) ; signal terrain |
| `divorce-be-conversion-separation-corps` (P3) | 🔻 P4 différé | concept résiduel (audit) ; dépend de la séparation de corps |

### 3.4 — Filiation / adoption
| Item audit | Verdict | Justification |
|---|---|---|
| `contestation-paternite-be` | ✅ couvert | = `contestation-filiation-be` (F-217) |
| `adoption-be-pleniere` (P2) | 🟢 à construire | situation cardinale adoption BE (conditions âge/écart/agrément propres) |
| `adoption-be-co-parentale` (P2 P3) | ❌ fusion | **BE-only** mais même situation « recevabilité adoption » que plénière, variante d'adoptant → branche de l'outil adoption (cohabitant légal) |
| `adoption-be-simple` (P3) | ❌ fusion | variante d'effet de la même situation adoption → branche |
| `adoption-be-internationale` (P3) | 🔻 P4 différé | cas spécialisé (Convention La Haye + autorité centrale) ; faible fréquence ; signal terrain |
| `kafala-be-recueil-legal` (P3 BE-only) | 🟢 à construire | **BE-only pur, aucun équivalent FR** (FR interdit Cciv 370-3) ; explicitement reporté à F-223 par F-217 |
| `gpa-be-vide-juridique` (P3 BE-only) | 🟢 à construire | **BE-only pur** (vide juridique ≠ interdiction FR) ; situation contentieuse fréquente, valeur de cadrage forte |
| `filiation-be-presomption-paternite` (P3) | ❌ doublon | situation « litige de paternité » déjà adressée par `contestation-filiation-be` (la présomption est l'input de la contestation) |
| `possession-etat-be` (P3) | 🔻 P4 différé | concept existe mais articulation marginale ; signal terrain |
| `pma-be-fiv` (P2) | 🔻 P4 différé | hors longue traîne pure ; analyseur filiation lourd, à traiter avec un signal d'usage |

### 3.5 — Régimes matrimoniaux
| Item audit | Verdict | Justification |
|---|---|---|
| `regime-be-communaute-legale` / `liquidation` / `recompenses` | ✅ couvert | `regime-mat-be-communaute-legale` + `liquidation-partage-be` (F-217) |
| `regime-be-separation-biens` (P2) | 🟢 à construire | régime distinct (séparation pure / société d'acquêts / correctifs 2018) — situation autonome non couverte |
| `regime-be-communaute-universelle` (P3) | ❌ fusion | variante de régime → comparateur / branche du sélecteur de régime |
| `regime-be-participation-acquets` (P3) | ❌ fusion | variante de régime → idem |
| `clauses-mat-aménageables-be` (P3) | 🔻 P4 différé | clauses notariales fines ; faible fréquence avocat ; signal terrain |
| `regime-international-be` (P2 P3) | ❌ fusion | = `dip-be-loi-applicable-regime-mat` (même situation : loi applicable au régime, Règl. 2016/1103) → fusionné dans l'outil DIP régime |
| `regime-algerien-be` (P3 BE-only) | 🟢 à construire | **BE-only pur**, explicitement reporté à F-223 par le PRODUCT_SPEC ; reconnaissance mariage/talaq/dot algériens, ordre public |

### 3.6 — Successions
| Item audit | Verdict | Justification |
|---|---|---|
| dévolution / réserve / acceptation / pacte successoral | ✅ couvert | F-217 (`succession-be-devolution-reserve`, `-acceptation-renonciation`, `pacte-successoral-be-2018`) |
| `succession-be-internationale` (P2 P3) | ❌ fusion | = `dip-be-loi-applicable-succession` (Règl. 650/2012 + CSE) → fusionné dans l'outil DIP succession |
| `succession-be-droits-succession-regionaux` (P2 P3) | 🔻 P4 différé | calculateur fiscal régional (Bruxelles/Wallonie/Flandre) ; matière fiscale plus que familiale ; signal terrain |
| `succession-be-droits-donation-regionaux` (P2 P3) | 🔻 P4 différé | idem fiscal régional |
| `succession-be-testament-redaction` (P3) | 🔻 P4 différé | outil de rédaction ; faible valeur décisionnelle isolée |

### 3.7 — Protection des incapables
| Item audit | Verdict | Justification |
|---|---|---|
| `protection-majeur-be-administrateur` | ✅ couvert | = `protection-majeur-be` (F-217) |
| `protection-be-mandat-extra-judiciaire` (P2 P3 BE-only) | 🟢 à construire | **BE-only pur** (mandat hors administration, loi 17/03/2013) explicitement reporté à F-223 par le PRODUCT_SPEC ; distinct du régime d'administration |
| `protection-be-declaration-anticipee` (P2) | ❌ fusion | choix anticipé de l'administrateur = branche du mandat extra-judiciaire / de l'administration → pas un outil séparé |
| `protection-mineur-tutelle-be` (P3) | 🔻 P4 différé | cas rares (audit) ; signal terrain |

### 3.8 — Violences
| Item audit | Verdict | Justification |
|---|---|---|
| interdiction domicile / mesures TF référé | ✅ couvert (équiv.) | `tribunal-famille-be-mesures-prov` (référé familial CJ 1280) couvre la voie TF ; voie parquet = P1/P2 hors longue traîne (à traiter ailleurs si signal) |
| `violences-be-pol-mesures-administratives` (P3) | 🔻 P4 différé | urgence absolue police/bourgmestre ; checklist marginale ; signal terrain |

### 3.9 — Procédures TF
| Item audit | Verdict | Justification |
|---|---|---|
| compétence / saisine / MP / appel / cassation / prescription | hors P3 | ce sont des P1/P2 calculateurs de délais (référentiels déjà seedés migration 162) — hors périmètre longue traîne P3 ; à traiter par une feature procédure si besoin |
| `tf-be-conciliation` (P4) | 🔻 P4 différé | optionnelle (audit) |

### 3.10 — DIP familial
| Item audit | Verdict | Justification |
|---|---|---|
| `dip-be-reconnaissance-talaq` (P2 P3) | ✅ couvert | absorbé par `mariage-etranger-be-reconnaissance` (F-217, « dont talaq ») |
| `dip-be-loi-applicable-divorce` (P2 P3 BE-only) | 🟢 à construire | Rome III (Règl. 1259/2010) ; situation autonome, couples binationaux très fréquents en BE |
| `dip-be-reconnaissance-jugement-etranger` (P2 P3) | 🟢 à construire | exequatur CDIP art. 22+ (jugement non-UE) ; situation distincte de la loi applicable |
| `dip-be-loi-applicable-regime-mat` (P2 P3) | ❌ fusion | = `regime-international-be` → **1 outil DIP régime matrimonial** (Règl. 2016/1103) |
| `dip-be-loi-applicable-succession` (P2 P3) | ❌ fusion | = `succession-be-internationale` → **1 outil DIP succession** (Règl. 650/2012 + CSE) |
| `dip-be-mariage-religieux-non-civil` (P3) | ❌ fusion | situation « mariage religieux non reconnu civilement » explicitement nommée au PRODUCT_SPEC F-223 → **1 outil** absorbant l'analyse art. 21 Constitution + CC 161 (et non un sous-cas de la reconnaissance d'un mariage *étranger* déjà couvert) |

### 3.11 — État civil
| Item audit | Verdict | Justification |
|---|---|---|
| `etat-civil-be-changement-sexe` (P2 BE-only) | 🟢 à construire | **BE-only fort** (auto-déclaration loi 25/06/2017, ≠ FR judiciaire) ; valeur produit nette |
| `etat-civil-be-changement-nom` (P2) | ❌ fusion | procédure officier état civil réformée 2018 → branche de l'outil état civil |
| `etat-civil-be-changement-prenom` (P3) | ❌ fusion | procédure simplifiée 2018 → branche du même outil |
| `etat-civil-be-rectification` (P3) | 🔻 P4 différé | cas occasionnels (audit) |

## Challenge amont
Chaque outil retenu suppose uniquement que l'avocat ait **qualifié la situation** (étape 2 du workflow). Aucune dépendance à une analyse de dossier ni à un autre outil. Les outils sont autoportants : l'avocat saisit la situation, l'outil évalue. ✅ **Aucun trou amont bloquant.** Les flags IA Famille BE (audit §5.2) sont un *plus* pour le déclenchement CONTEXTUAL automatique mais ne sont pas un pré-requis bloquant — un outil peut être seedé CONTEXTUAL avec un flag dédié au moment de sa création (pattern F-217).

## Challenge aval
La sortie (validité / recevabilité / loi applicable / régime adapté / éligibilité) alimente le conseil et la rédaction d'actes — exploitable directement par l'avocat. ✅ **Pas de trou aval bloquant.**

⚠️ **Dépendance aval NON bloquante mais à signaler — citation jurisprudentielle BE** : la brique « citer avec autorité » côté BE (**F-JU-04**) est **🟡 En cours et PARKÉE** (web_search BE = 0/3 propre, mappings pilote archivés, cf. mémoire `reference_be_jurisprudence_sources` + `project_session_resume`). Conséquence : les outils F-223, comme tous les outils Famille BE déjà livrés, fonctionnent **sans citation jurisprudentielle automatique** tant que F-JU-04 n'est pas débloqué. Ce n'est **pas un STOP** (F-211/F-217 ont livré 15 outils BE dans cette même condition) — c'est une limite assumée, identique à l'existant. Invariant : **silence > erreur** (pas de citation hallucinée).

## STOPs / pré-requis à ajouter au backlog
Aucun STOP. Aucun pré-requis amont à créer. F-223 est **GO avec ajustements** (ajustement = trim du périmètre, pas ajout de brique).

## Invariants anti-gadget pour les mini-specs
- **1 outil = 1 situation** ([[feedback_decision_tools_one_per_situation]]) : la cohabitation légale (formation/effets/dissolution), l'adoption (plénière/simple/co-parentale), l'état civil (nom/prénom/sexe), le mandat de protection (mandat/déclaration anticipée) sont chacun **1 seul outil multi-vues**, jamais éclatés en N outils.
- **Anti-doublon F-211/F-217** : aucun outil ne doit recouvrir DC, DDI, mariage étranger/talaq, pacte successoral, méthode Renard, notaire commis, dévolution/réserve, protection-majeur (administration), contestation filiation, communauté légale, liquidation-partage — **tous déjà livrés**.
- **DIP régime ↔ régime international** et **DIP succession ↔ succession internationale** : 1 seul outil chacun (la loi applicable EST la situation), pas 2.
- **Mariage religieux non-civil ≠ reconnaissance mariage étranger** : situations distinctes (l'une sanctionne le défaut de mariage civil préalable art. 21 Constit. / CC 161 ; l'autre reconnaît un mariage *valablement* célébré à l'étranger, déjà couvert) — invariant respecté à condition que la mini-spec cadre nettement le périmètre.
- **CONTEXTUAL + flag IA bridé**, jamais ALWAYS_ON : chaque outil F-223 est de la longue traîne → trigger CONTEXTUAL avec le flag IA Famille BE dédié (audit §5.2 : `kafala_recueil_detecte`, `gpa_be_situation_contentieuse`, `cohabitation_legale_be_detectee`, `regime_algerien_be_detecte`, `changement_sexe_envisage_be`, `succession_internationale_detectee`…). Pas de saturation du panel.
- **Pré-fill IA (F-246)** : tout champ saisissable pré-rempli par l'IA, seule exception = info absente des documents uploadés.
- **Pas de citation BE non vérifiée** tant que F-JU-04 parké (silence > erreur).
- **Références juridiques à valider par un avocat belge** avant tout seed (renumérotation massive CC Livres 2/3/4 post-réformes 2017-2019 — l'audit annote « (à vérifier) »).

## Décision finale — périmètre net trimé
**GO avec ajustements.** Brut audit ~37 items P3 → **9 outils neufs + 1 extension**. Passage PRODUCT_SPEC `À planifier` → `À faire`. Mettre à jour la ligne F-223 (« ~30 outils / ~60 SF » → **9 outils + 1 extension**). Enchaîner étape 0 bis (cohérence écran) puis les mini-specs.

### Liste finale 🟢 (tool_id proposé · base juridique BE · logique)
1. **`cohabitation-legale-be`** · Loi 23/11/1998 + CC art. 1475-1479 · Régime de la cohabitation légale (formation par déclaration officier état civil / effets patrimoniaux / dissolution unilatérale ou commune) — 1 outil multi-vues, BE-only sans équivalent FR.
2. **`adoption-be`** · Loi 24/04/2003 + CC art. 343-1+ · Recevabilité adoption (plénière / simple / co-parentale par cohabitant légal) — conditions âge, écart, agrément, consentements ; branches d'adoptant et d'effet, 1 outil.
3. **`kafala-be-recueil-legal`** · CDIP + CC art. 343 al. 2 nouveau · Reconnaissance du recueil légal (kafala) — exclusion de l'adoption mais reconnaissance via DIP ; BE-only pur, reporté explicitement par F-217.
4. **`gpa-be-situation-contentieuse`** · Absence de loi spécifique (vide juridique) · Arbre décisionnel filiation post-GPA (convention non opposable, filiation par adoption après naissance) — BE-only pur (≠ interdiction FR).
5. **`regime-algerien-be`** · CDIP + Convention algéro-belge · Reconnaissance du mariage algérien, talaq, dot — conditions de consentement / ordre public belge ; BE-only pur, reporté explicitement par le PRODUCT_SPEC.
6. **`regime-be-separation-biens`** · Livre 3 CC (loi 22/07/2018) · Séparation de biens (pure / société d'acquêts / correctifs équitables et créance de participation 2018) — situation de régime distincte de la communauté légale déjà livrée.
7. **`dip-be-loi-applicable-famille`** · Rome III (Règl. 1259/2010) + Règl. 2016/1103 + Règl. 650/2012 + CDIP · Détermination de la loi applicable (divorce / régime matrimonial / succession) en présence d'un élément d'extranéité — **fusionne** `dip-be-loi-applicable-divorce` + `-regime-mat` + `-succession` + `regime-international-be` + `succession-be-internationale` (même situation : choix/détermination de loi applicable, multi-vues par matière).
8. **`dip-be-reconnaissance-decision-etrangere`** · CDIP art. 22-27 · Reconnaissance / exequatur d'une décision familiale étrangère (jugement non-UE, mariage religieux non précédé d'un civil art. 21 Constit. / CC 161) — analyse de conformité à l'ordre public belge ; distinct de la *loi applicable* et de la reconnaissance d'un mariage étranger valablement célébré (déjà couverte par `mariage-etranger-be-reconnaissance`).
9. **`etat-civil-be-modification`** · Loi 18/06/2018 (nom/prénom) + loi 25/06/2017 (sexe, auto-déclaration) · Modification de l'état civil (changement de nom / prénom / sexe) via officier de l'état civil — 1 outil multi-vues, spécificité BE forte (auto-déclaration sexe ≠ FR judiciaire).
10. **Extension `protection-majeur-be`** · Loi 17/03/2013 + CC art. 490 nouveau · Ajout d'une branche **mandat extra-judiciaire + déclaration anticipée** (protection conventionnelle hors administration judiciaire) à l'outil existant — **pas un nouvel outil**, même situation « protection du majeur vulnérable » que l'administration déjà livrée.

### Différés P4 / signal terrain (documentés, non abandonnés)
`mariage-be-empêchements`, `cohabitation-fait-be-effets`, `separation-corps-be` (+ conversion), `adoption-be-internationale`, `possession-etat-be`, `pma-be-fiv`, `clauses-mat-aménageables-be`, `succession-be-droits-succession/donation-regionaux` (fiscal régional), `succession-be-testament-redaction`, `protection-mineur-tutelle-be`, `violences-be-pol-mesures-administratives`, `tf-be-conciliation`, `etat-civil-be-rectification`. → rattachés à **F-224 (P4 cross-domain, à ré-évaluer au signal terrain)**.
