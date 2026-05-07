# Audit juridique exhaustif — Outils décisionnels Droit de la famille Belgique

**Auteur** : LegalCase — automatique (audit F-191)
**Date** : 2026-05-06
**Périmètre** : droit belge de la famille uniquement (Famille BE, hors Travail BE et Immigration).
**Méthode** : départ des **sources juridiques belges** (Code civil belge — Livres 2/3/4 ; Code judiciaire ; Code DIP belge ; lois 2017-2019 de réforme), pas du miroir FR. Les outils BE-only (TF, DDI deux voies, pacte successoral 2018, kafala, régime algérien BE, cohabitation légale) sont valorisés comme dispositifs autonomes.
**Sortie** : Tableau A (existant), Tableau B (audit exhaustif), Tableau C (extension F-166 BE), synthèse chiffrée + Top 10.

---

## 1. Contexte et avertissement méthodologique

### 1.1 Pourquoi un audit Famille BE séparé

LegalCase couvre 3 domaines × 2 pays. Côté Famille BE, le seed initial F-IA-04 (migration `105`) a posé 4 outils ALWAYS_ON et 10 CONTEXTUAL en `country=NULL` (transversal). À partir de la vague de fin avril 2026 (F-150 à F-153 + F-DT-29/30/F-FA-08+), **toutes les nouvelles entrées Famille ont été seedées en `country='FRANCE'` exclusivement**, à l'exception de :

- `F-FA-11-desunion-irremediable-be` (BE only — divorce DDI partiel),
- `F-FA-23-ordonnance-requete` (FR + BE).

L'audit F-166 (niveau 3 prompts Sonnet + bascule ALWAYS_ON → CONTEXTUAL) **n'a touché ni Famille FR ni Famille BE**. Aucun flag IA Famille BE n'existe à ce jour dans `FamilleExtractedData` (frontend) ou dans le pipeline backend, à part deux champs F-FA-11 (`dateSeparation` + `separationConsentue`).

Conséquence opérationnelle : un avocat famille belge qui ouvre un dossier `country=BELGIQUE` voit aujourd'hui dans le panel F-IA-04 :

- 4 outils transversaux fonctionnels (F-FA-05 partage immobilier, F-FA-06 calendrier garde, F-FA-07 checklist divorce, et d'office aussi `F-153-fourchettes-jaf` qui est pourtant FR-only — DELETE migration 191 → invisible aujourd'hui),
- 1 outil BE dédié (F-FA-11),
- 1 outil bipays (F-FA-23),
- les **30 autres outils Famille FR** sont seedés `country='FRANCE'`, **mais cachés** quand l'utilisateur est BE.

Autrement dit : le panel décisionnel Famille BE compte aujourd'hui **3 à 6 outils utilisables** contre **30+ côté FR**, alors que le droit belge de la famille a ses propres concepts structurants (TF, DDI, pacte successoral, réforme régimes 2018, cohabitation légale, kafala…).

### 1.2 Topologie BE différente du miroir FR

Quelques différences structurantes empêchent toute approche par miroir mécanique :

- **Tribunal de la famille (TF)** créé par loi 30/07/2013 — chambre unique du tribunal de 1ʳᵉ instance regroupant divorce, AP, contribution alimentaire, succession en partage, régimes matrimoniaux (CJ art. 572bis). En FR, le JAF reste un cadre + plusieurs juridictions distinctes (TJ, TC successions, TI tutelles).
- **Divorce** :
  - **DC** (consentement mutuel) — toujours juge en BE (CJ art. 1287+ — refonte loi 27/04/2007), ≠ FR depuis loi 23/03/2019 où le DC a basculé "notarié sans juge" sauf demande enfant.
  - **DDI** (désunion irrémédiable) — art. 229 § 1 CC, **deux voies distinctes** : preuve séparation 6 mois consensuelle ou 1 an unilatérale, ou faits constitutifs sans délai. Les 2 voies justifient 2 outils décisionnels.
- **Successions** : réforme loi 31/07/2017 (en vigueur 01/09/2018) — réserve désormais 1/2 quel que soit le nombre d'enfants (avant : 1/2, 2/3, 3/4). Pacte successoral admis depuis 2018 (loi 31/07/2017) — interdit en FR (sauf RAAR très restreint).
- **Régimes matrimoniaux** : refonte loi 22/07/2018 — communauté légale belge ≠ communauté légale FR (composition régie par CC nouveau Livre 3).
- **Cohabitation légale** (loi 23/11/1998) — régime intermédiaire entre PACS FR et concubinage. **Cohabitation de fait** : sans aucun cadre formel (≠ concubinage FR qui a lui-même un cadre Cciv 515-8).
- **Filiation / paternité** : refonte loi 01/07/2006 + art. 318 CC contestation, art. 332ter recherche, présomption de paternité légèrement différente (CC nouveau).
- **Adoption** : loi 24/04/2003 + adoption co-parentale couples mariés et cohabitants légaux.
- **Protection des incapables** : loi 17/03/2013 — réforme structurante remplaçant administration provisoire / minorité prolongée → administrateur de la personne / des biens unique.
- **DIP** : Code DIP belge 16/07/2004 + Règlements UE 650/2012 (succession), 2016/1103 (régime mat), 2016/1104 (effets patrimoniaux partenariats).
- **GPA** : vide juridique persistant en BE (≠ FR où la GPA est interdite avec interdiction explicite). En BE les conventions sont licites entre les parties mais pas opposables au regard de la filiation ni au regard de l'état civil — situation contentieuse.
- **Kafala** : reconnaissance partielle en BE (CC art. 343 al. 2 nouveau exclut l'adoption-kafala mais le recueil légal peut être reconnu via le DIP).
- **Régime algérien BE** : reconnaissance des mariages algériens et des divorces religieux (talaq) avec exigences spécifiques (consentement, ordre public).

### 1.3 Échelle de priorité

- **P1 — urgence procédurale** : délai court irréversible (DDI séparation 6 mois, déclarations succession 4 mois, pourvoi cassation 3 mois, appel TF 1 mois).
- **P2 — fréquence haute** : situation rencontrée par tout avocat famille belge plusieurs fois par mois.
- **P3 — spécificité BE** : pas d'équivalent FR direct, c'est de la valeur produit pure (TF, DDI, pacte successoral, cohabitation légale, kafala, régime algérien BE).
- **P4 — confort** : utile mais peut différer sans perte de couverture.

### 1.4 Avertissement de fiabilité

Toutes les références CC / CJ / lois sont issues des connaissances générales du modèle. La **réforme 2018-2019 du Code civil belge a renuméroté massivement** les articles (Livre 3 régimes mat, Livre 4 successions). Les références exactes Livre/article sont annotées **(à vérifier)** quand le modèle n'est pas certain. Un avocat belge **doit confirmer chaque article avant tout seed en production**.

---

## 2. Tableau A — Outils Famille BE existants

Source : migrations Liquibase `105`, `106`, `127-190` qui INSERT dans `decision_tool_visibility_rules`, plus migration `191` (DELETE Cat C) et `192` (restauration partielle). Croisement avec `TOOL_REGISTRY` dans `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts`.

Les outils marqués `country=NULL` sont transversaux FR+BE. Les outils marqués `country='FRANCE'` sont FR-only — invisibles côté BE par le service de visibilité.

| tool_id | layer | country | trigger | Frontend câblé | Gate composant | Situation juridique pour BE |
|---|---|---|---|---|---|---|
| `F-FA-05-partage-immobilier` | ALWAYS_ON | NULL | — | OUI | aucune (transversal) | Partage d'un bien immobilier après dissolution communauté ou indivision. Utilisable en BE — la logique de quote-part est neutre. |
| `F-FA-06-calendrier-garde` | ALWAYS_ON | NULL | — | OUI | distingue ALTERNEE_BE / SECONDAIRE_BE / SECONDAIRE_ELARGI_BE | Calendrier hébergement enfants — supporte les modes BE explicitement. **Outil partagé fonctionnel BE**. |
| `F-FA-07-checklist-divorce` | ALWAYS_ON | NULL | — | OUI | aucune | Checklist documents et étapes divorce — partagée FR/BE mais le contenu juridique est principalement FR (à vérifier — le composant peut nécessiter un seed BE distinct dans `legal_referentials` si pas déjà fait). |
| `F-FA-08-divorce-alteration` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **N'a pas d'équivalent BE** (BE = DDI séparation 6 mois ≠ altération 1 an FR). |
| `F-FA-09-divorce-faute` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **N'a pas d'équivalent BE direct** (BE = DDI faits, voie unilatérale art. 229 § 3 CC nouveau). |
| `F-FA-10-divorce-accepte` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **N'a pas d'équivalent BE** (BE = DC seul). |
| `F-FA-11-desunion-irremediable-be` | ALWAYS_ON | BELGIQUE | — | OUI | `=== 'BELGIQUE'` | **BE-only, partiel** : couvre "DDI séparation prouvée 6 mois consensuelle". Manque la 2ᵉ voie DDI (faits/preuves art. 229 § 3) et la 3ᵉ branche (1 an séparation unilatérale). À éclater. |
| `F-FA-12-mesures-provisoires` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : les mesures provisoires devant le TF en référé familial (CJ art. 1253ter/2 + art. 1280) sont l'équivalent fonctionnel et sont massivement utilisées. |
| `F-FA-13-revisions-post-divorce` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : révision contribution alimentaire / pension après divorce devant TF (CJ art. 1278+). |
| `F-FA-14-ordonnance-protection` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : protection violences intrafamiliales — interdiction de domicile loi 15/05/2012, mesures TF référé violences. |
| `F-FA-15-recompenses` | ALWAYS_ON | FRANCE | — | OUI | (à vérifier — workspaceCountry passé) | Récompenses entre patrimoine commun et patrimoine propre. Concept existant en droit BE communauté légale (Livre 3 nouveau, art. 1432+ ancien CC). À vérifier si l'outil tolère BE. |
| `F-FA-16-communaute-universelle` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Pas applicable directement BE** (régime distinct — voir Tableau B `regime-be-communaute-universelle`). |
| `F-FA-17-partage-judiciaire` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : liquidation-partage notaire commis (CJ art. 1207+), équivalent fonctionnel mais procédure différente (notaire commis, projet de liquidation, contredits, compétence TF). |
| `F-FA-18-reconnaissance-paternelle` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : reconnaissance art. 327bis CC + consentement mère + consentement enfant majeur (BE). |
| `F-FA-18-contestation-paternite` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : contestation art. 318 CC nouveau, qualité à agir + délais 1 an / 30 ans selon qualité. |
| `F-FA-18-recherche-paternite` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : recherche art. 332ter CC nouveau, conditions et expertise ADN. |
| `F-FA-18-possession-etat` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Concept existe en BE** (art. 331octies ancien CC, refondu) mais articulation différente — feature jumelle BE à ouvrir. |
| `F-FA-18-adoption` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : adoption interne + internationale BE (loi 24/04/2003), adoption co-parentale, conditions âge/écart différents (art. 343-1+ CC nouveau à vérifier). |
| `F-FA-19-autorite-parentale` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : autorité parentale conjointe par défaut (art. 374 CC), exclusive sur décision TF. |
| `F-FA-19-changement-residence` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : changement résidence enfant (CJ art. 374/1) — autorisation TF si désaccord. |
| `F-FA-19-desaccords-parentaux` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : décision TF en cas de désaccord (CJ art. 387ter ancien / refondu) — domaines ressemblants à FR mais cadre différent. |
| `F-FA-20-pacs-dissolution` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Pas d'équivalent BE** — la dissolution de la cohabitation légale BE est très différente (loi 23/11/1998 + art. 1476 CC) et nécessite outil dédié. |
| `F-FA-21-separation-corps` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : séparation de corps existe en BE (CC art. 308+, loi historique) — devenue rare mais juridiquement vivante. Conversion en divorce après 5 ans. |
| `F-FA-22-indivision` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : indivision art. 815 CC nouveau, sortie d'indivision via partage TF. Concept identique FR mais procédure différente (notaire commis CJ 1207+). |
| `F-FA-23-ordonnance-requete` | ALWAYS_ON | FRANCE + BELGIQUE | — | OUI | (à vérifier — composant supporte 2 pays) | **Bipays fonctionnel** — ordonnance sur requête FR (CPC art. 493) ou requête unilatérale BE (CJ art. 1025+). |
| `F-FA-24-devolution-legale` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : dévolution légale BE (Livre 4 CC nouveau — droits du conjoint survivant fortement réformés 2017). |
| `F-FA-24-testament-validite` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : validité testament BE (CC nouveau Livre 4 — formes olographe, authentique, international). |
| `F-FA-24-donation` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : donation entre vifs BE (CC nouveau Livre 4 — réduction, rapport, droits régionaux DT donation Bruxelles/Wallonie/Flandre). |
| `F-FA-24-reserve-heriditaire` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : réserve héréditaire BE post-réforme 2017 — désormais 1/2 quel que soit le nombre d'enfants (≠ FR 1/2, 2/3, 3/4). **Différence majeure**. |
| `F-FA-24-partage-successoral` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : partage successoral BE — notaire commis, procédure distincte. |
| `F-FA-24-indivision-successorale` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : indivision successorale BE — sortie via partage TF. |
| `F-FA-24-rapport-succession` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : rapport à succession BE — règles Livre 4 réformé. |
| `F-FA-25-majeurs-proteges` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : administrateur de la personne / des biens (loi 17/03/2013, en vigueur 01/09/2014 — refonte complète). Articulation très différente. |
| `F-FA-26-changement-etat-civil` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : changement nom (loi du 15/05/1987 + 18/06/2018), changement prénom (procédure officier état civil), changement sexe (loi 25/06/2017). |
| `F-FA-27-pma-gpa` | ALWAYS_ON | FRANCE | — | OUI | `isFrance` masque BE | FR-only. **Manque BE** : PMA / FIV BE (loi 06/07/2007), GPA (vide juridique BE — situation contentieuse), bioéthique très différente. |
| `F-152-divorce-consentement-scoring` | (DELETE) | — | — | composant pres. seul | — | Outil supprimé migration 191 — non disponible. |
| `F-153-fourchettes-jaf` | (DELETE) | — | — | composant pres. seul | — | Outil supprimé migration 191 — JAF FR uniquement de toute manière. |

**Total effectif Famille BE au 2026-05-06** :

- 3 outils transversaux fonctionnels (F-FA-05, F-FA-06, F-FA-07).
- 1 outil BE-only partiel (F-FA-11 — couvre 1 voie sur 2 du DDI).
- 1 outil bipays (F-FA-23).
- **Total : 5 outils décisionnels Famille utilisables côté BE**, contre **30+ côté FR**.

C'est un déséquilibre 6/1 — exactement le pattern dénoncé dans `feedback_belgique_never_forget.md` (mémoire utilisateur).

### 2.1 Référentiel procédural Famille BE déjà seedé

La migration `162` a seedé `legal_referentials.FAMILLE_PROCEDURE_JALONS` avec 3 entrées BE complètes : `TRIBUNAL_FAMILLE_BE` (5 jalons CJ art. 572bis/1253ter/2/747/770), `COUR_APPEL_FAMILLE_BE` (4 jalons CJ art. 1051/1056/770), `CASSATION_FAMILLE_BE` (4 jalons CJ art. 1073/1080/1095/1109). **Description SF-140-03 conforme**. Les délais procéduraux sont donc disponibles côté backend pour câbler des outils Famille BE — mais aucun outil ne les consomme aujourd'hui.

---

## 3. Tableau B — Audit juridique exhaustif Famille BE

Une ligne = une situation juridique distincte qui mérite un outil décisionnel autonome (un outil = une situation, règle CLAUDE.md `feedback_decision_tools_one_per_situation`). Les outils déjà existants en Tableau A sont signalés **EXISTE**. Les autres sont **MANQUE** avec priorité.

### 3.1 — Mariage et formation du couple

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `mariage-be-empêchements` | Empêchements au mariage (âge, parenté, lien antérieur, consentement) | CC Livre 2 (refonte loi 13/04/2019 — articles à vérifier) | Checklist + analyseur | MANQUE | P3 | Cas pratiques rares en consultation pure, mais utiles pour mariages internationaux. |
| `mariage-be-validite-formelle` | Validité formelle (publication bans, célébration officier état civil, conditions de fond) | CC art. 63+ (à vérifier) | Checklist | MANQUE | P3 | Souvent utile en cas d'annulation. |
| `mariage-be-annulation` | Annulation mariage — absolue / relative, qualité à agir, délais | CC art. 180+ (à vérifier) | Analyseur validité + délais | MANQUE | P2 | Cas plus fréquent qu'attendu (mariages forcés, contrainte, qualité civile fictive). |
| `mariage-be-reconnaissance-mariage-etranger` | Reconnaissance d'un mariage étranger (talaq, polygamie, mariage religieux) | CDIP art. 21+ ; CC art. 27+ | Analyseur | MANQUE | **P1 P3 BE-only** | **Critique** — mariage marocain / algérien / turc fréquent en BE. Reconnaissance partielle, ordre public belge, polygamie réfutée mais effets succession/pension restent. |
| `cohabitation-legale-be-formation` | Formation cohabitation légale (déclaration officier état civil, conditions, contrat) | Loi 23/11/1998 + CC art. 1475+ | Checklist + générateur déclaration | MANQUE | **P2 P3 BE-only** | **Outil clé** — équivalent fonctionnel PACS FR mais conditions distinctes (déclaration commune, pas de contrat obligatoire, simple cohabitation). |
| `cohabitation-legale-be-effets` | Effets cohabitation légale (devoirs, dettes, régime des biens, succession partielle) | CC art. 1477+ | Analyseur | MANQUE | **P2 P3 BE-only** | Différents du PACS FR — pas d'équivalent en droit français. |
| `cohabitation-legale-be-dissolution` | Dissolution cohabitation légale — déclaration commune, déclaration unilatérale, mariage | CC art. 1476 § 2 | Générateur déclaration + checklist | MANQUE | **P2 P3 BE-only** | Distinct de F-FA-20 PACS FR (décharge en mairie ≠ déclaration officier état civil BE). Non couvert. |
| `cohabitation-fait-be-effets` | Concubinage / cohabitation de fait — absence de cadre légal, créances entre concubins | Jurisprudence + RGPD informationnel | Information / arbre décisionnel | MANQUE | P3 BE-only | Rarement objet d'outil mais utile pour répondre aux clients ("rien ne s'applique automatiquement"). |
| `regime-mat-be-choix-contrat` | Choix régime matrimonial (communauté légale, séparation biens, communauté universelle, participation acquêts) | Livre 3 CC (refonte loi 22/07/2018) | Comparateur + générateur acte notarié | MANQUE | **P1 P2 BE-only** | **Outil très demandé** par les notaires mais aussi les avocats consult. Pas du tout couvert. |
| `regime-mat-be-changement` | Changement régime matrimonial pendant le mariage (acte notarié, intervention juge si enfants) | Loi 22/07/2018 + CC art. 1394+ (à vérifier) | Checklist + analyseur | MANQUE | **P2 BE-only** | Pratique courante. Procédure 2018 simplifiée. |

### 3.2 — Divorce et séparation

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `dc-be-conditions-recevabilite` | DC (consentement mutuel) — conditions, délai 6 mois (sauf 6 mois mariage), accord total sur tous points | CJ art. 1287+ ; loi 27/04/2007 | Analyseur recevabilité + checklist | MANQUE | **P1 P2 BE-only** | **Critique** — DC reste la voie majoritaire BE. Pas du tout couvert. |
| `dc-be-convention-prealable` | Convention préalable — pension alimentaire, partage, autorité parentale, hébergement enfants | CJ art. 1287 + 1288 | Générateur de convention + checklist | MANQUE | **P1 P2 BE-only** | Document central de la procédure. Préalable obligatoire. |
| `dc-be-procedure-comparutions` | Procédure DC — 1ʳᵉ comparution, 3 mois entre comparutions, transcription | CJ art. 1289+ | Calculateur de délais | MANQUE | **P1 BE-only** | Délais stricts. |
| `ddi-be-separation-6mois-consensuelle` | DDI séparation prouvée 6 mois consensuelle (les 2 époux demandent) | CC art. 229 § 1 + CJ art. 1255 § 1 | Analyseur recevabilité | EXISTE (F-FA-11 partiel) | — | **F-FA-11 couvre cette branche**. À renommer / éclater. |
| `ddi-be-separation-1an-unilaterale` | DDI séparation prouvée 1 an unilatérale (1 époux demande) | CC art. 229 § 1 + CJ art. 1255 § 2 | Analyseur recevabilité | MANQUE | **P1 P2 BE-only** | **Manque** — voie distincte de F-FA-11. À éclater. |
| `ddi-be-faits-preuves` | DDI faits constitutifs (sans délai — preuves de mésentente persistante) | CC art. 229 § 3 | Analyseur de preuves + scoring | MANQUE | **P1 P2 BE-only** | **Manque** — voie distincte (faute / dispute prouvée). Très utilisé. |
| `divorce-be-effets-temporaires` | Effets temporaires du divorce — résidences séparées, dispense devoir cohabitation | CJ art. 1280 référé familial | Information / checklist | MANQUE | P2 BE-only | Souvent demandé en consult initial. |
| `divorce-be-conversion-separation-corps` | Conversion séparation de corps en divorce — délai 5 ans | CC art. 311 + CJ art. 1286 (à vérifier) | Calculateur de délais | MANQUE | P3 BE-only | Concept résiduel. |
| `separation-corps-be` | Séparation de corps BE — recevabilité, effets, conversion | CC art. 308+ ; CJ art. 1310+ | Analyseur + générateur | MANQUE | P3 BE-only | F-FA-21 FR-only — manque l'équivalent BE. |
| `divorce-be-cassation-tf` | Pourvoi cassation contre arrêt cour d'appel famille | CJ art. 1073 (3 mois) | Calculateur de délais | MANQUE | **P1 BE-only** | Référentiel `CASSATION_FAMILLE_BE` déjà seedé migration 162 — outil simple à câbler. |

### 3.3 — Autorité parentale, hébergement, contributions

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `autorite-parentale-be-conjointe-vs-exclusive` | AP conjointe (par défaut) vs exclusive (sur décision TF) | CC art. 374 + 375 (à vérifier) | Arbre décisionnel | MANQUE | **P1 P2 BE-only** | **Manque** — F-FA-19 FR-only. AP BE différente structurellement. |
| `hebergement-egalitaire-be` | Hébergement égalitaire (1 sem/1 sem) — conditions, présomption favorable | CJ art. 374 § 2 + jurisprudence | Analyseur recevabilité | MANQUE | **P1 P2 BE-only** | Loi 18/07/2006 — présomption hébergement égalitaire en cas d'accord. F-FA-06 couvre le calendrier mais pas la décision sur le mode. |
| `hebergement-secondaire-elargi-be` | Hébergement secondaire / élargi — modes intermédiaires | Jurisprudence + accords parentaux | Calendrier (déjà couvert F-FA-06) | EXISTE (F-FA-06 partiel) | — | F-FA-06 supporte les 4 modes BE — utile mais pas d'analyseur décisionnel. |
| `contribution-alimentaire-be` | Contribution alimentaire enfants — méthode Renard, contributions, part contributive | CC art. 203 + art. 203bis | Calculateur + comparateur | MANQUE | **P1 P2 BE-only** | **Manque critique** — méthode Renard très spécifique BE. F-FA-02 FR-only ne s'applique pas. |
| `contribution-conjoint-survivant-be` | Pension alimentaire entre époux post-divorce | CC art. 301 § 4 nouveau (à vérifier) | Calculateur + analyseur | MANQUE | **P2 BE-only** | Différent de la prestation compensatoire FR — pension alimentaire avec révision. |
| `desaccords-parentaux-be` | Désaccords parentaux (scolarité, santé, religion, voyages) — saisine TF | CC art. 374 § 1 al. 2 (à vérifier) | Analyseur + générateur requête | MANQUE | **P2 BE-only** | F-FA-19-desaccords-parentaux FR-only — manque équivalent BE. |
| `changement-residence-enfant-be` | Changement résidence avec déménagement — autorisation TF | CJ art. 374 § 2 | Analyseur + générateur | MANQUE | **P2 BE-only** | F-FA-19-changement-residence FR-only. Très demandé (mobilité internationale). |
| `mediation-familiale-be` | Médiation familiale obligatoire ou volontaire — médiateur agréé | Loi 21/02/2005 + CJ art. 1730+ | Information + checklist | MANQUE | P3 BE-only | Médiation prévue avant audience fond TF. Outil simple. |
| `revisions-pa-be` | Révision contribution alimentaire post-divorce | CC art. 301 + 1288bis CJ | Analyseur + générateur | MANQUE | **P2 BE-only** | F-FA-13 FR-only. |
| `mesures-provisoires-be-tf` | Mesures provisoires devant TF en référé familial | CJ art. 1253ter/2 + 1280 | Analyseur urgence + générateur | MANQUE | **P1 P2 BE-only** | F-FA-12 FR-only. **Outil très demandé** — résidence enfants, contribution provisoire. Référentiel `TRIBUNAL_FAMILLE_BE` déjà seedé. |
| `protection-jeunesse-tribunal-be` | Saisine tribunal de la jeunesse — protection enfant en danger | Décret communautaire + CJ | Information / checklist | MANQUE | P3 BE-only | Compétence régionale. Cas plus rares mais juridiquement clair. |

### 3.4 — Filiation et adoption

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `filiation-be-presomption-paternite` | Présomption de paternité du mari | CC art. 315 (à vérifier) | Analyseur | MANQUE | P3 BE-only | Différent FR — délai contestation distinct. |
| `reconnaissance-paternelle-be` | Reconnaissance paternelle — consentement mère + enfant | CC art. 327bis (à vérifier) | Analyseur + générateur | MANQUE | **P2 BE-only** | F-FA-18-reconnaissance-paternelle FR-only. |
| `contestation-paternite-be` | Contestation paternité — qualité à agir, délais 1/30 ans | CC art. 318 nouveau | Analyseur + délais | MANQUE | **P1 P2 BE-only** | F-FA-18-contestation-paternite FR-only. **Délai contestation 1 an BE pour enfant mineur, 30 ans après majorité enfant** (à vérifier). |
| `recherche-paternite-be` | Action en recherche paternité — qualité à agir, ADN | CC art. 332ter (à vérifier) | Analyseur + générateur | MANQUE | **P2 BE-only** | F-FA-18-recherche-paternite FR-only. |
| `possession-etat-be` | Possession d'état — tractatus / fama / nomen | CC nouveau Livre 2 (à vérifier) | Analyseur faisceaux | MANQUE | P3 BE-only | F-FA-18-possession-etat FR-only — concept BE existe mais référence article différente. |
| `adoption-be-pleniere` | Adoption plénière BE — conditions, procédure, agrément | Loi 24/04/2003 + CC art. 343-1+ (à vérifier) | Analyseur recevabilité + scoring | MANQUE | **P2 BE-only** | F-FA-18-adoption FR-only. Conditions BE différentes (âge adoptant, écart âge, délai parents biologiques). |
| `adoption-be-simple` | Adoption simple BE — conditions, effets | Loi 24/04/2003 | Analyseur | MANQUE | P3 BE-only | Distincte de l'adoption plénière. |
| `adoption-be-co-parentale` | Adoption co-parentale — couple marié + cohabitants légaux | Loi 24/04/2003 + extension partenaires de même sexe | Analyseur | MANQUE | **P2 P3 BE-only** | **Spécificité BE** — adoption co-parentale par cohabitant légal possible. Très demandé couples LGBTQ+. |
| `adoption-be-internationale` | Adoption internationale — Convention La Haye + autorité centrale communautaire | Loi 24/04/2003 + Convention 29/05/1993 | Checklist + analyseur | MANQUE | P3 BE-only | Cas spécialisé. |
| `kafala-be-recueil-legal` | Kafala — recueil légal (droit musulman) — reconnaissance partielle BE | CDIP + CC art. 343 al. 2 nouveau | Analyseur reconnaissance | MANQUE | **P3 BE-only** | **Spécificité BE** — kafala reconnue partiellement (pas adoption mais recueil légal possible). Aucun équivalent FR. |
| `pma-be-fiv` | PMA / FIV en BE — conditions, accès, filiation | Loi 06/07/2007 | Analyseur recevabilité + filiation | MANQUE | **P2 BE-only** | F-FA-27 FR-only. PMA BE ouverte aux femmes seules / couples lesbiens depuis 2007 (avant FR). Filiation différente. |
| `gpa-be-vide-juridique` | GPA en BE — vide juridique, situation contentieuse | Pas de loi spécifique | Information / arbre décisionnel | MANQUE | **P3 BE-only** | **Spécificité BE** — pas interdit explicitement (≠ FR), mais convention non opposable. Filiation par adoption après naissance possible mais incertain. |

### 3.5 — Régimes matrimoniaux et liquidation

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `regime-be-communaute-legale` | Communauté légale BE post-2018 — composition, gestion, dettes | Livre 3 CC (loi 22/07/2018) | Analyseur composition + comparateur | MANQUE | **P1 P2 BE-only** | **Régime par défaut, non couvert**. Différences importantes vs communauté légale FR (composition acquêts, dettes propres, biens propres). |
| `regime-be-separation-biens` | Séparation de biens BE — pure, avec société d'acquêts, avec correctifs équitables | Livre 3 CC nouveau + jurisprudence | Analyseur | MANQUE | **P2 BE-only** | Règles correctives 2018 — créances de participation. |
| `regime-be-communaute-universelle` | Communauté universelle BE | Livre 3 CC (loi 22/07/2018) | Analyseur | MANQUE | **P3 BE-only** | F-FA-16-communaute-universelle FR-only — concept BE proche mais articles différents. |
| `regime-be-participation-acquets` | Participation aux acquêts BE | Livre 3 CC (loi 22/07/2018) | Calculateur créance participation | MANQUE | P3 BE-only | Régime moins courant mais en croissance. |
| `liquidation-partage-be-notaire-commis` | Liquidation-partage post-divorce — notaire commis, projet de liquidation, contredits | CJ art. 1207+ + 1218 | Checklist procédure + générateur | MANQUE | **P1 P2 BE-only** | **Procédure très différente FR** — notaire commis par le TF désigne un notaire qui établit un projet, contredits en 1 mois, homologation TF. F-FA-17 FR-only. |
| `recompenses-be` | Récompenses entre patrimoine commun et propre | CC art. 1432+ ancien / Livre 3 nouveau (à vérifier) | Calculateur | MANQUE | **P2 BE-only** | F-FA-15 FR-only. Le concept existe mais articulation BE post-2018 différente. |
| `partage-immobilier-be` | Partage immobilier — quote-part, soulte, attribution préférentielle | Livre 3 CC nouveau | Calculateur (déjà couvert F-FA-05) | EXISTE (F-FA-05 transversal) | — | F-FA-05 fonctionne en BE — calcul mathématique neutre. |
| `clauses-mat-aménageables-be` | Clauses régime matrimonial — préciput, partage inégal, attribution intégrale | Livre 3 CC + AR | Comparateur | MANQUE | P3 BE-only | Très utilisé en pratique notariale. |
| `regime-international-be` | Régime applicable mariage international — Règlement UE 2016/1103 | CDIP + R 2016/1103 | Analyseur DIP | MANQUE | **P2 P3 BE-only** | **Critique** — couples binationaux ; loi applicable au régime souvent contestée. |
| `regime-algerien-be` | Régime algérien BE — reconnaissance mariage, talaq, dot | CDIP + Convention algéro-belge | Analyseur reconnaissance | MANQUE | **P3 BE-only** | **Spécificité BE** — régime algérien fréquent (population algérienne BE). F-IM-17-regime-algerien existe côté immigration mais pas côté famille. |

### 3.6 — Successions

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `succession-be-devolution-legale` | Dévolution légale BE — ordre des héritiers, droits du conjoint survivant | Livre 4 CC (loi 31/07/2017) | Analyseur ordre successoral | MANQUE | **P1 P2 BE-only** | **Manque critique**. F-FA-24-devolution-legale FR-only. Réforme 2017 fortement modifiée droits conjoint. |
| `succession-be-reserve-1-2` | Réserve héréditaire BE post-réforme 2017 — 1/2 quel que soit nombre d'enfants | CC art. 913+ nouveau | Analyseur | MANQUE | **P1 P2 BE-only** | **Très différent FR** (FR : 1/2, 2/3, 3/4 selon nombre enfants ; BE : toujours 1/2 depuis 2018). |
| `succession-be-quotite-disponible` | Quotité disponible et donations / legs excédant | CC art. 920+ nouveau | Calculateur + analyseur réduction | MANQUE | **P2 BE-only** | Réformé en 2017 — règles fluctuation valeur des biens. |
| `succession-be-acceptation-renonciation` | Acceptation pure, sous bénéfice d'inventaire, renonciation | CC art. 774+ nouveau | Arbre décisionnel + délais | MANQUE | **P1 BE-only** | Délai 4 mois pour acceptation/renonciation déclarée + 17 ans inventaire. Risque de dévolution forcée. |
| `succession-be-pacte-successoral` | Pacte successoral admis depuis 2018 (loi 31/07/2017) | CC art. 1100/1+ | Analyseur validité + générateur | MANQUE | **P2 P3 BE-only** | **Spécificité BE post-2018** — pacte global ou ponctuel. Aucun équivalent FR (interdiction Cciv 1130 al 2). |
| `succession-be-rapport` | Rapport à succession — donations rapportables | CC art. 843+ nouveau | Calculateur | MANQUE | **P2 BE-only** | F-FA-24-rapport-succession FR-only. |
| `succession-be-partage-judiciaire` | Partage successoral judiciaire — notaire commis | CJ art. 1207+ | Checklist + générateur | MANQUE | **P1 P2 BE-only** | F-FA-24-partage-successoral FR-only. Procédure BE distincte. |
| `succession-be-indivision` | Indivision successorale — sortie via partage TF | CC art. 815+ + CJ 1207+ | Analyseur | MANQUE | **P2 BE-only** | F-FA-24-indivision-successorale FR-only. |
| `succession-be-testament-validite` | Validité testament BE — formes (olographe, authentique, international) | CC art. 967+ nouveau | Analyseur validité | MANQUE | **P2 BE-only** | F-FA-24-testament-validite FR-only. |
| `succession-be-testament-redaction` | Testament — clauses à risque (substitution, privation réserve) | CC nouveau Livre 4 | Générateur + analyseur | MANQUE | P3 BE-only | Outil rédaction. |
| `succession-be-donation-validite` | Donation entre vifs BE | CC art. 893+ nouveau | Analyseur validité + réduction | MANQUE | **P2 BE-only** | F-FA-24-donation FR-only. Régionalisation droits de donation Bruxelles/Wallonie/Flandre. |
| `succession-be-droits-succession-regionaux` | Droits de succession — barèmes régionaux | Décrets régionaux Bruxelles/Wallonie/Flandre | Calculateur (3 régions) | MANQUE | **P2 P3 BE-only** | **Spécificité BE** — taux différents selon région du défunt. Très demandé. |
| `succession-be-droits-donation-regionaux` | Droits de donation — barèmes régionaux | Décrets régionaux | Calculateur (3 régions) | MANQUE | **P2 P3 BE-only** | Idem. |
| `succession-be-internationale` | Succession internationale — Règlement UE 650/2012 + CSE | R 650/2012 + CDIP | Analyseur loi applicable | MANQUE | **P2 P3 BE-only** | **Critique** — clients binationaux. CSE (certificat successoral européen) demandé fréquemment. |
| `succession-be-déclaration-fiscale` | Déclaration de succession (4 mois résidence BE / 5 mois Europe / 6 mois hors Europe) | CDPS / décrets régionaux | Calculateur de délais + checklist | MANQUE | **P1 BE-only** | Délai impératif. Pénalités lourdes en cas de retard. |

### 3.7 — Protection des incapables

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `protection-majeur-be-administrateur` | Administration de la personne / des biens — réforme 2014 | Loi 17/03/2013 (en vigueur 01/09/2014) | Arbre décisionnel + analyseur | MANQUE | **P1 P2 BE-only** | **Manque critique**. F-FA-25 FR-only. Régime BE complètement différent (administrateur unique, déclaration anticipée, mandat extra-judiciaire). |
| `protection-mineur-tutelle-be` | Tutelle mineur — désignation, conseil de famille | CC art. 389+ | Checklist + générateur | MANQUE | P3 BE-only | Cas plus rares mais juridiquement clair. |
| `protection-be-mandat-extra-judiciaire` | Mandat extra-judiciaire (équivalent mandat protection future FR) | Loi 17/03/2013 + CC art. 490 nouveau | Générateur + analyseur | MANQUE | **P2 P3 BE-only** | **Spécificité BE** — mandat conclu hors administration provisoire. Très utilisé en gestion patrimoniale. |
| `protection-be-declaration-anticipee` | Déclaration anticipée — choix de l'administrateur en cas d'incapacité | Loi 17/03/2013 | Générateur déclaration | MANQUE | **P2 BE-only** | Outil simple. |

### 3.8 — Violences intrafamiliales

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `violences-be-interdiction-domicile` | Interdiction temporaire de résidence (parquet) | Loi 15/05/2012 | Analyseur urgence + générateur réquisition | MANQUE | **P1 P2 BE-only** | **Manque critique**. Equivalent fonctionnel ordonnance protection FR (F-FA-14) mais procédure différente (parquet + juge). |
| `violences-be-mesures-tf-refere` | Mesures TF référé violences (CJ 1280) | CJ art. 1280 | Générateur requête + checklist preuves | MANQUE | **P1 P2 BE-only** | Distinct de l'interdiction domicile parquet. |
| `violences-be-pol-mesures-administratives` | Mesures police / bourgmestre — éloignement urgence | Loi communale + circulaires | Checklist | MANQUE | P3 BE-only | Cas urgence absolue. |

### 3.9 — Procédures

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `tf-be-competence` | Compétence TF — matières (CJ 572bis), territorial | CJ art. 572bis + 624 | Arbre décisionnel | MANQUE | **P2 BE-only** | Évite déclinatoires. Compétence très large. |
| `tf-be-saisine-requete` | Saisine TF — requête contradictoire / unilatérale | CJ art. 1253ter + 1025 | Générateur requête + checklist | MANQUE | **P1 P2 BE-only** | Outil de production. |
| `tf-be-mesures-provisoires-mp` | Audience MP — référé familial CJ 1253ter/2 | CJ art. 1253ter/2 | Générateur requête + checklist conditions | MANQUE | **P1 P2 BE-only** | F-FA-12 FR-only. Voir 3.3. |
| `tf-be-conciliation` | Conciliation préalable / pôle famille | CJ art. 731 + AR | Information | MANQUE | P4 BE-only | Optionnelle mais pratiquée. |
| `tf-be-appel-cour` | Appel cour d'appel chambre famille — délai 1 mois | CJ art. 1051 | Calculateur de délais | MANQUE | **P1 BE-only** | Référentiel `COUR_APPEL_FAMILLE_BE` déjà seedé migration 162. |
| `tf-be-cassation` | Pourvoi cassation chambre civile — 3 mois | CJ art. 1073+ | Calculateur de délais | MANQUE | **P1 BE-only** | Référentiel `CASSATION_FAMILLE_BE` déjà seedé. |
| `tf-be-prescription-actions-famille` | Prescription des actions — alimentaires (5 ans), partage (5 ans), réserve (10 ans) | CC variés | Calculateur | MANQUE | **P1 P2 BE-only** | Critique pour ne pas perdre une action. |

### 3.10 — DIP, mariages internationaux, divorces étrangers

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `dip-be-loi-applicable-divorce` | Loi applicable au divorce — Règlement Rome III | R 1259/2010 + CDIP art. 55 | Analyseur DIP | MANQUE | **P2 P3 BE-only** | Très utilisé couples binationaux. |
| `dip-be-reconnaissance-jugement-etranger` | Reconnaissance jugement étranger — exequatur | CDIP art. 22+ | Checklist + analyseur | MANQUE | **P2 P3 BE-only** | Talaq, divorce religieux, jugement non-européen. |
| `dip-be-reconnaissance-talaq` | Reconnaissance d'un talaq prononcé à l'étranger — ordre public | CDIP art. 27 + jurisprudence | Analyseur conformité OP | MANQUE | **P2 P3 BE-only** | **Critique** — fréquent (Maroc, Algérie). Conditions strictes (consentement épouse, comparution). |
| `dip-be-loi-applicable-regime-mat` | Loi applicable régime matrimonial — Règlement UE 2016/1103 | R 2016/1103 + CDIP | Analyseur DIP | MANQUE | **P2 P3 BE-only** | Très lié à `regime-international-be`. |
| `dip-be-loi-applicable-succession` | Loi applicable succession — Règlement UE 650/2012 | R 650/2012 | Analyseur DIP + générateur option de loi | MANQUE | **P2 P3 BE-only** | Lié à `succession-be-internationale`. |
| `dip-be-mariage-religieux-non-civil` | Mariage religieux non précédé d'un mariage civil — sanction art. 21 Constitution | Constitution art. 21 + CC art. 161 | Analyseur | MANQUE | P3 BE-only | Cas pratique fréquent (mariages religieux en BE sans civil). |

### 3.11 — État civil et identité

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `etat-civil-be-changement-nom` | Changement de nom — loi 18/06/2018 (réforme) | Loi 18/06/2018 | Checklist + analyseur recevabilité | MANQUE | **P2 BE-only** | F-FA-26 FR-only. Procédure BE différente (officier état civil). |
| `etat-civil-be-changement-prenom` | Changement de prénom — officier état civil | Loi 18/06/2018 | Checklist | MANQUE | P3 BE-only | Procédure simplifiée 2018. |
| `etat-civil-be-changement-sexe` | Changement de sexe — loi 25/06/2017 (auto-déclaration) | Loi 25/06/2017 | Checklist | MANQUE | **P2 BE-only** | **Spécificité BE** — auto-déclaration sans certificat médical. Très différent FR. |
| `etat-civil-be-rectification` | Rectification acte état civil | CC art. 99+ + CJ | Générateur requête | MANQUE | P3 BE-only | Cas occasionnels. |

---

## 4. Synthèse chiffrée

### 4.1 Résumé global

| Catégorie | Existant | Manquant | Total |
|---|---|---|---|
| **3.1 Mariage / formation couple** | 0 | 10 | 10 |
| **3.2 Divorce / séparation** | 1 (partiel F-FA-11) | 9 | 10 |
| **3.3 AP / hébergement / contributions** | 1 (transversal F-FA-06) | 10 | 11 |
| **3.4 Filiation / adoption** | 0 | 13 | 13 |
| **3.5 Régimes matrimoniaux** | 1 (transversal F-FA-05) | 9 | 10 |
| **3.6 Successions** | 0 | 15 | 15 |
| **3.7 Protection incapables** | 0 | 4 | 4 |
| **3.8 Violences** | 0 | 3 | 3 |
| **3.9 Procédures TF** | 0 | 7 | 7 |
| **3.10 DIP** | 0 | 6 | 6 |
| **3.11 État civil** | 0 | 4 | 4 |
| **TOTAL** | **3** | **90** | **93** |

(Les 5 outils existants Tableau A sont : F-FA-05, F-FA-06, F-FA-07 transversaux + F-FA-11 partiel + F-FA-23 bipays. Sur les 5, F-FA-07 est listé comme "transversal mais contenu juridique principalement FR" — il ne compte pas pleinement comme couvert BE. F-FA-23 est déjà bipays donc compté à part. Net : 3 outils Famille **réellement BE-fonctionnels** sur 93 situations distinctes identifiées.)

### 4.2 Top 10 outils manquants par priorité (P1 + P2 + BE-only)

Critères de tri : (1) urgence procédurale, (2) fréquence haute, (3) spécificité BE-only, (4) absence totale d'équivalent.

1. **`dc-be-conditions-recevabilite`** — DC = voie majoritaire BE. Aucune couverture. **P1 P2 BE-only**.
2. **`mesures-provisoires-be-tf`** — référé familial CJ 1253ter/2. Référentiel déjà seedé. **P1 P2 BE-only**.
3. **`liquidation-partage-be-notaire-commis`** — procédure CJ 1207+ très différente FR. **P1 P2 BE-only**.
4. **`succession-be-reserve-1-2`** — réforme 2017 fortement différente FR (1/2 quel que soit nombre enfants). **P1 P2 BE-only**.
5. **`succession-be-acceptation-renonciation`** — délai 4 mois impératif. Risque dévolution forcée. **P1 BE-only**.
6. **`autorite-parentale-be-conjointe-vs-exclusive`** — AP par défaut conjointe BE, exclusive rare. **P1 P2 BE-only**.
7. **`contribution-alimentaire-be`** — méthode Renard très spécifique. Aucun équivalent F-FA-02 FR. **P1 P2 BE-only**.
8. **`ddi-be-faits-preuves`** + **`ddi-be-separation-1an-unilaterale`** — F-FA-11 ne couvre qu'une voie sur trois du DDI. **P1 P2 BE-only**.
9. **`protection-majeur-be-administrateur`** — loi 17/03/2013 fortement différente FR. **P1 P2 BE-only**.
10. **`mariage-be-reconnaissance-mariage-etranger`** + **`dip-be-reconnaissance-talaq`** — fréquent en BE (Maroc, Algérie, Turquie). Aucun équivalent FR. **P1 P3 BE-only**.

### 4.3 Outils BE-only majeurs sans équivalent FR (preuve d'indépendance)

Ces situations n'ont **aucun miroir FR** : elles existent uniquement en BE et requièrent un modèle juridique propre. Leur existence prouve que l'audit Famille BE ne peut pas se faire par symétrie avec FR.

| Concept BE-only | Source | Pourquoi pas en FR |
|---|---|---|
| **Tribunal de la famille (TF)** unique | CJ art. 572bis | FR : JAF + TJ + TC successions + TI tutelles dispersés. |
| **DDI 3 voies** (consensuelle 6 mois / unilatérale 1 an / faits preuves) | CC art. 229 §§ 1 et 3 | FR depuis 2019 : DC notarié + altération 1 an + faute. Régime entier différent. |
| **DC reste judiciaire** | CJ art. 1287+ | FR depuis loi 23/03/2019 : DC notarié sans juge sauf demande enfant. |
| **Pacte successoral** depuis 2018 | CC art. 1100/1+ | Interdit en FR (Cciv 1130 al 2 + RAAR très restreint). |
| **Réserve héréditaire 1/2 fixe** | CC art. 913+ nouveau | FR : 1/2, 2/3, 3/4 selon nombre d'enfants. |
| **Cohabitation légale** distincte du PACS | Loi 23/11/1998 + CC 1475+ | PACS FR a un régime juridique propre, pas transposable. |
| **Cohabitation de fait** sans cadre | Jurisprudence | FR : concubinage avec cadre Cciv 515-8. |
| **Méthode Renard** contribution alimentaire | CC art. 203bis + jurisprudence | FR : barème JAF ou évaluation libre. |
| **Notaire commis** liquidation-partage | CJ art. 1207+ | FR : pas de procédure équivalente (notaire choisi par les parties ou désigné JAF mais sans cadre 1207+). |
| **Adoption co-parentale par cohabitant légal** | Loi 24/04/2003 | FR : adoption simple par concubin/PACSé moins ouverte avant loi 21/02/2022, encore différente. |
| **Kafala — recueil légal** | CDIP + CC art. 343 al. 2 nouveau | FR : interdiction d'adoption de kafala (Cciv 370-3). Reconnaissance plus large en BE. |
| **GPA — vide juridique** | Pas de loi | FR : interdiction explicite Cciv 16-7. |
| **PMA femmes seules / lesbiennes** depuis 2007 | Loi 06/07/2007 | FR : ouverte par loi 02/08/2021 — encadrement différent. |
| **Changement sexe par auto-déclaration** | Loi 25/06/2017 | FR : intervention judiciaire encore requise. |
| **Mandat extra-judiciaire** (protection majeur) | Loi 17/03/2013 + CC 490 nouveau | FR : mandat de protection future différent. |
| **Régionalisation droits succession / donation** Bruxelles / Wallonie / Flandre | Décrets régionaux | FR : barème national. |
| **Régime algérien BE** | CDIP + Convention algéro-belge | FR : absent (autres conventions FR-Maghreb). |
| **DDI conversion séparation corps** délai 5 ans | CC art. 311 | FR : délai 2 ans loi 23/03/2019. |

---

## 5. Tableau C — Audit F-166 Famille BE

### 5.1 Constat F-166 Famille BE

F-166 (SF-166-01 + SF-166-02) a couvert **8 outils Travail FR niveau 3 ALWAYS_ON → CONTEXTUAL** avec extension du pipeline IA pour produire 8 booleans (`rappel_salaire_detecte`, `urgence_procedurale`, etc.). **Famille FR et Famille BE sont hors-périmètre F-166** à ce jour.

Pour Famille BE, l'audit révèle :

- **5 outils existants** dans le panel F-IA-04 (3 transversaux + F-FA-11 + F-FA-23).
- **Tous en ALWAYS_ON** — aucun outil Famille BE n'utilise de trigger CONTEXTUAL.
- **Aucun flag IA Famille BE** dans `FamilleExtractedData` à part `dateSeparation` + `separationConsentue` (F-FA-11).

Mais les **30 outils Famille FR** seedés `country='FRANCE'` ne s'appliquent pas — ils sont déjà filtrés hors BE par `country`. Il n'y a donc rien à basculer ALWAYS_ON → CONTEXTUAL côté Famille BE existant.

L'enjeu F-166 Famille BE devient inversé : **avant de basculer en CONTEXTUAL, il faut d'abord créer les outils manquants** (Tableau B). Une fois créés, certains seront naturellement ALWAYS_ON (outils universels comme la dévolution légale, prescription actions famille) et d'autres CONTEXTUAL (déclenchés par flag IA).

### 5.2 Flags IA Famille BE proposés

Pour anticiper la bascule CONTEXTUAL (et la faire dès le seed initial des nouveaux outils Famille BE plutôt qu'un re-shift après coup), on identifie les flags IA Famille BE qui seront utiles :

| Flag IA proposé | Outil cible | Source de détection (analyse documentaire) | Priorité |
|---|---|---|---|
| `divorce_dc_envisage` | `dc-be-conditions-recevabilite`, `dc-be-convention-prealable`, `dc-be-procedure-comparutions` | Mots-clés "consentement mutuel", "DC", "accord total" + indices conventions préalables | **P1** |
| `divorce_ddi_envisage` (avec sous-flag `voie`) | `ddi-be-separation-6mois-consensuelle`, `ddi-be-separation-1an-unilaterale`, `ddi-be-faits-preuves` | "désunion irrémédiable", "séparation N mois/an", "faits constitutifs" | **P1** |
| `cohabitation_legale_be_detectee` | `cohabitation-legale-be-formation`, `cohabitation-legale-be-effets`, `cohabitation-legale-be-dissolution` | "cohabitation légale", "déclaration commune officier état civil" | **P2** |
| `cohabitation_fait_be_detectee` | `cohabitation-fait-be-effets` | "concubinage", "vie commune sans déclaration" | P3 |
| `pacte_successoral_envisage` | `succession-be-pacte-successoral` | "pacte successoral", "pacte global", "renonciation héréditaire" | **P2 P3** |
| `reforme_regimes_2018_applicable` | `regime-be-communaute-legale`, `regime-be-separation-biens` (post 01/09/2018) | Date contrat de mariage | **P2** |
| `kafala_recueil_detecte` | `kafala-be-recueil-legal` | "kafala", "recueil légal", origine droit musulman | **P3 BE-only** |
| `mariage_etranger_reconnaissance_detecte` | `mariage-be-reconnaissance-mariage-etranger`, `dip-be-reconnaissance-talaq` | Mariage célébré hors BE, talaq, mariages multiples | **P1 P3** |
| `regime_algerien_be_detecte` | `regime-algerien-be` | Mariage algérien, talaq, dot | **P3 BE-only** |
| `liquidation_partage_judiciaire_detecte` | `liquidation-partage-be-notaire-commis` | "PV difficultés", "notaire commis", "projet liquidation" | **P1 P2** |
| `mediation_familiale_obligatoire_detectee_be` | `mediation-familiale-be` | "médiation", "médiateur agréé", saisine TF | P3 |
| `protection_majeur_be_detectee` | `protection-majeur-be-administrateur`, `protection-be-mandat-extra-judiciaire`, `protection-be-declaration-anticipee` | "incapacité", "administrateur", "Alzheimer", "perte facultés" | **P1 P2** |
| `cas_violences_intrafamiliales_detecte` | `violences-be-interdiction-domicile`, `violences-be-mesures-tf-refere` | "violences", "menaces", "constat médical lésions", "main courante" | **P1 P2** |
| `succession_internationale_detectee` | `dip-be-loi-applicable-succession`, `succession-be-internationale` | Défunt étranger, biens immo étrangers, dernière résidence à l'étranger | **P2 P3** |
| `succession_acceptation_delai_4mois_detecte` | `succession-be-acceptation-renonciation` | Date décès récente + question acceptation/renonciation | **P1** |
| `region_succession_detectee` (`BRUXELLES` / `WALLONIE` / `FLANDRE`) | `succession-be-droits-succession-regionaux`, `succession-be-droits-donation-regionaux` | Domicile défunt | **P2 BE-only** |
| `changement_sexe_envisage_be` | `etat-civil-be-changement-sexe` | "changement sexe", "transition", "auto-déclaration" | P3 |
| `pma_be_envisagee` | `pma-be-fiv` | "PMA", "FIV", "couple lesbien", "femme seule" | P2 |
| `gpa_be_situation_contentieuse` | `gpa-be-vide-juridique` | "GPA", "mère porteuse", "convention de gestation" | P3 BE-only |
| `presomption_paternite_litige_be` | `filiation-be-presomption-paternite`, `contestation-paternite-be` | Doute paternité, ADN, naissance < 300 jours après divorce | **P2** |
| `mariage_religieux_non_civil_detecte` | `dip-be-mariage-religieux-non-civil` | Mariage religieux uniquement, sans acte civil | P3 |

### 5.3 Tableau C — Outils Famille BE existants candidats à CONTEXTUAL

Aujourd'hui, sur les 5 outils Famille **réellement BE-fonctionnels**, **5 sont en ALWAYS_ON**. Aucun n'est CONTEXTUAL. Le tableau ci-dessous identifie ceux qui pourraient bénéficier d'une bascule CONTEXTUAL avec un flag IA approprié, **après** que les nouveaux outils Famille BE auront été livrés.

| Outil ALWAYS_ON | Layer actuel | Flag IA proposé | Preuves textuelles attendues | Priorité bascule |
|---|---|---|---|---|
| `F-FA-05-partage-immobilier` (transversal) | ALWAYS_ON | `bien_immobilier_a_partager_detecte` | Mention bien immo dans inventaire + indivision/communauté | P3 (utile mais ALWAYS_ON acceptable car situation très fréquente) |
| `F-FA-06-calendrier-garde` (transversal) | ALWAYS_ON | `enfant_mineur_detecte` | Mention enfant < 18 ans | P3 (acceptable ALWAYS_ON — outil neutre toujours utile) |
| `F-FA-07-checklist-divorce` (transversal) | ALWAYS_ON | `divorce_envisage` (FR ou BE) | Mention divorce, séparation procédurale | **P2** (pertinent si dossier non-divorce) |
| `F-FA-11-desunion-irremediable-be` | ALWAYS_ON BE | `divorce_ddi_envisage` (sous-flag `consensuelle`) | Voie consensuelle 6 mois détectée | **P2** (à éclater 3 voies + CONTEXTUAL chacune) |
| `F-FA-23-ordonnance-requete` (FR + BE) | ALWAYS_ON | `urgence_familiale_unilaterale_detectee` | Saisine non-contradictoire urgente | **P2** (pertinent quand pas d'urgence) |

**Conclusion** : la priorité Famille BE n'est pas la bascule CONTEXTUAL — c'est la **création des outils manquants** (90 selon Tableau B). La bascule CONTEXTUAL viendra naturellement à la création des nouveaux outils, en utilisant les flags IA listés en 5.2 dès le seed initial.

### 5.4 Implémentation flags IA Famille BE — recommandations

Pour câbler les flags IA Famille BE, deux travaux préalables :

1. **Étendre `FamilleExtractedData`** (frontend) avec les nouveaux booleans BE — pattern existant SF-166-01 pour Travail FR (ajout de booleans `*_detecte`).
2. **Étendre le prompt Sonnet Famille** (backend, à localiser dans `backend/src/main/java/.../ai/promp/famille*` ou équivalent — non audité ici car focus juridique). Le prompt doit produire les 21 booleans listés en 5.2 dans la section `famille_extracted_data` du JSON `analysis_result`.
3. **Étendre `DecisionToolVisibilityService.extractDetectedSituations`** pour lire ces booleans et émettre les valeurs trigger (pattern existant SF-166-02).

Ces 3 étapes sont mécaniquement transposables du pattern F-166 Travail FR.

---

## 6. Découpages à éclater

### 6.1 Outils existants à scinder

| Outil actuel | Situation | Découpage proposé |
|---|---|---|
| `F-FA-11-desunion-irremediable-be` | Couvre uniquement la voie consensuelle 6 mois | Éclater en **3 outils** : `ddi-be-separation-6mois-consensuelle`, `ddi-be-separation-1an-unilaterale`, `ddi-be-faits-preuves`. Chaque voie a ses propres conditions de recevabilité, ses propres preuves, son propre cheminement procédural. |
| `regime-mat-be-choix-contrat` | 4 régimes différents | Éclater en **4 outils** parallèles : `regime-be-communaute-legale`, `regime-be-separation-biens`, `regime-be-communaute-universelle`, `regime-be-participation-acquets` + 1 outil méta `regime-mat-be-comparateur` qui les compare côte à côte. Pattern F-DT-08/F-DT-09/F-DT-10. |
| `dc-be-conditions-recevabilite` (proposé) | Le DC a 2 phases distinctes (recevabilité + procédure) | Garder en 1 outil mais avec section recevabilité + section calendrier (2 vues dans le composant). Alternative : éclater en `dc-be-conditions-recevabilite` + `dc-be-procedure-comparutions`. **Préférer 1 outil multi-vues**. |
| `etat-civil-be-changement-nom` (proposé) | Nom + prénom + sexe = 3 procédures distinctes | Éclater en **3 outils** : `etat-civil-be-changement-nom`, `etat-civil-be-changement-prenom`, `etat-civil-be-changement-sexe`. Pattern F-DT-08/F-DT-10. |
| `succession-be-droits-succession-regionaux` (proposé) | 3 régions = 3 barèmes différents | Soit 1 outil avec input "région", soit 3 outils. **Préférer 1 outil avec input** (calculateur paramétré, pas situation distincte). |

### 6.2 Outils transversaux à valider pour BE

| Outil | Vérification à faire |
|---|---|
| `F-FA-05-partage-immobilier` | Confirmer que les enums et la logique mathématique sont neutres — pas de référence Cciv FR hardcodée. |
| `F-FA-06-calendrier-garde` | Confirmer que les modes BE (ALTERNEE_BE, SECONDAIRE_BE, SECONDAIRE_ELARGI_BE) sont câblés dans le composant et non juste seedés. |
| `F-FA-07-checklist-divorce` | **Risque de divergence** : la checklist documents est-elle BE-aware (acte de mariage BE, extrait registre national, attestation domicile commune) ou seulement FR ? À auditer le seed `legal_referentials.DIVORCE_PIECES` pour voir s'il a un variant BE. Sinon, refondre. |
| `F-FA-15-recompenses` | Composant FR-only mais le concept existe BE post-2018. Vérifier les enums et la logique. **Probable feature jumelle BE à ouvrir**. |
| `F-FA-23-ordonnance-requete` | Confirmer que la procédure BE (CJ art. 1025+) est implémentée et pas juste documentée. |

---

## 7. Hors périmètre / Honnêteté

### 7.1 Références à valider impérativement

- **Tous les articles CC nouveau Livre 2/3/4** post-réformes 2017/2018/2019 ont été renumérotés massivement. Les références CC art. 318 / 332ter / 343-1 / 374 / 815 / 913 / 1100/1 / 1432 / 1475 / 1476 / 1477 / 1526 du Tableau B sont annotées (à vérifier) et **doivent être confirmées par un avocat belge avant tout seed**.
- Les **articles CJ** (572bis, 1025, 1051, 1073, 1207, 1253ter/2, 1280, 1287, 1288, 1289) sont plus stables mais demandent confirmation en cas de doute.
- Les **délais procéduraux** (1 mois appel TF, 3 mois cassation, 6 mois DDI, 4 mois acceptation succession) sont à confirmer cas par cas.

### 7.2 Limites de l'audit

- Cet audit ne couvre pas le **droit du travail BE** (déjà couvert par `audit-be-travail-exhaustif.md`) ni l'**immigration BE** (à auditer séparément).
- Les **droits régionaux** (succession Wallonie / Flandre / Bruxelles) sont mentionnés mais non détaillés. Un audit dédié peut être justifié pour les 3 régions.
- Les **conventions internationales bilatérales** (Belgique-Maroc, Belgique-Algérie, Belgique-Turquie pour mariages et talaq) sont mentionnées mais non détaillées. Cas par cas.
- L'**audit du pipeline IA Famille** (prompts Sonnet, schéma JSON `famille_extracted_data`) n'est pas inclus — il est listé comme prérequis en 5.4 mais demande une exploration séparée du backend.
- L'**audit du référentiel `DIVORCE_PIECES`** et `DIVORCE_ETAPES` (utilisés par F-FA-07) pour vérifier la couverture BE n'est pas inclus.

### 7.3 Recommandations de séquencement

1. **Vague 1 — Top 3 P1 fondamentaux (≈ 6 SF)** :
   - `dc-be-conditions-recevabilite` + `dc-be-convention-prealable` + `dc-be-procedure-comparutions`
   - `mesures-provisoires-be-tf`
   - `liquidation-partage-be-notaire-commis`
2. **Vague 2 — DDI complétion + AP + contributions (≈ 5 SF)** :
   - `ddi-be-separation-1an-unilaterale` + `ddi-be-faits-preuves` (éclatement F-FA-11)
   - `autorite-parentale-be-conjointe-vs-exclusive`
   - `contribution-alimentaire-be` (méthode Renard)
   - `mesures-provisoires-be-tf` (si pas déjà fait vague 1)
3. **Vague 3 — Successions BE (≈ 8 SF)** :
   - `succession-be-devolution-legale`
   - `succession-be-reserve-1-2`
   - `succession-be-acceptation-renonciation`
   - `succession-be-pacte-successoral`
   - `succession-be-rapport`
   - `succession-be-partage-judiciaire`
   - `succession-be-testament-validite`
   - `succession-be-droits-succession-regionaux`
4. **Vague 4 — Régimes matrimoniaux BE (≈ 5 SF)** :
   - `regime-be-communaute-legale`
   - `regime-be-separation-biens`
   - `regime-be-communaute-universelle`
   - `regime-be-participation-acquets`
   - `recompenses-be`
5. **Vague 5 — Filiation / adoption BE (≈ 6 SF)** :
   - `reconnaissance-paternelle-be`
   - `contestation-paternite-be`
   - `recherche-paternite-be`
   - `adoption-be-pleniere`
   - `adoption-be-co-parentale`
   - `kafala-be-recueil-legal`
6. **Vague 6 — DIP + protection majeurs + violences (≈ 6 SF)** :
   - `dip-be-loi-applicable-divorce`
   - `dip-be-reconnaissance-talaq`
   - `mariage-be-reconnaissance-mariage-etranger`
   - `protection-majeur-be-administrateur`
   - `violences-be-interdiction-domicile`
   - `violences-be-mesures-tf-refere`
7. **Vague 7 — Cohabitation légale + état civil BE (≈ 4 SF)** :
   - `cohabitation-legale-be-formation` + `cohabitation-legale-be-effets` + `cohabitation-legale-be-dissolution`
   - `etat-civil-be-changement-nom` + `etat-civil-be-changement-sexe`
8. **Vague 8 — résiduels P3 / P4** : tutelles, séparation corps BE, conciliation, conversion, annulation.

**Total estimé : ~45 SF** sur ~90 outils manquants, avec mutualisation de certains composants frontend pour réduire le coût (notamment les vues de calculateurs paramétrés et les générateurs de requêtes).

### 7.4 Inputs flags IA — pré-requis transversal

Avant les vagues 1-8 ci-dessus, planifier une **feature dédiée enrichissement IA Famille BE** (équivalent F-166 mais Famille BE) :

- Étendre `FamilleExtractedData` avec les 21 flags IA proposés en 5.2.
- Étendre le prompt Sonnet Famille pour produire ces flags.
- Étendre `DecisionToolVisibilityService.extractDetectedSituations` pour lire les booleans Famille BE.
- Ainsi, à chaque création d'outil Famille BE, le seed initial peut directement utiliser CONTEXTUAL avec le bon flag — sans repasser par une vague de bascule rétroactive.

Cette feature peut être préparée et mergée **avant** la vague 1, ou en parallèle de la vague 1 si le contrat IA est figé.

---

## 8. Conclusion

L'audit Famille BE révèle un **déséquilibre structurel** entre la couverture FR (30+ outils) et la couverture BE (3 outils réellement BE-fonctionnels), aggravé par le fait que le droit belge de la famille a sa propre topologie — TF, DDI 3 voies, pacte successoral, réforme régimes 2018, cohabitation légale, kafala, régime algérien, GPA en vide juridique. Aucune feature ne peut résoudre cela par symétrie mécanique avec FR.

**90 outils manquants** sont identifiés (Tableau B), dont **10 critiques P1+P2 BE-only** (Top 10 §4.2), **18 BE-only sans aucun équivalent FR** (§4.3), et **22 flags IA à câbler** dans le pipeline IA Famille (§5.2).

La séquence recommandée (§7.3) en **8 vagues / ~45 SF** permet de rattraper la couverture en respectant les invariants CLAUDE.md (un outil = une situation, pas de symétrie forcée, parité domaines métier, audit cohérence transversale à chaque SF).

Le pré-requis F-166-Famille-BE (étendre flags IA Famille) est à planifier en amont des vagues 1-8 — sans lui, les outils créés devront tous être en ALWAYS_ON et nécessiteront une bascule CONTEXTUAL rétroactive à terme (dette de convergence).
