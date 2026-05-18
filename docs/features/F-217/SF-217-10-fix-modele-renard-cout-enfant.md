# Mini-spec — F-217 / SF-217-10 — Correction du modèle de calcul : vrai modèle « méthode Renard » (coût de l'enfant indexé sur les revenus)

## Identifiant
`F-217 / SF-217-10`

## Feature parente
`F-217` — P2 Famille BE — outils décisionnels de fréquence haute (Vague 2 — Enfants)

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
`fix/F-217-renard-model`

---

## Nature de la subfeature

**Correction d'un outil existant** (`ContributionAlimentaireEnfantsBeCalculator`, SF-217-06/07, déjà
mergé sur `master`). Il ne s'agit pas d'un nouvel outil décisionnel ni d'un nouvel écran : aucun
nouveau workflow, aucune nouvelle situation métier, aucun nouvel élément visible. → **Étapes 0
(cadrage cohérence) et 0 bis (cohérence écran) exemptées** (cf. CLAUDE.md — exemption explicite
bugfix / refactor sans élément visible nouveau). L'invariant « un outil décisionnel = une situation
métier » est respecté : on corrige le modèle de l'outil existant, on n'en crée pas un second.

---

## Objectif

> En une phrase : remplacer les forfaits de coût fixes par le **vrai modèle Renard** — coût de
> l'enfant = coefficient statistique (par âge) × revenus cumulés des parents — pour que l'outil
> applique réellement la méthode qu'il revendique.

---

## Le problème corrigé

Une contre-vérification IA (cf. historique `PRODUCT_SPEC.md` 2026-05-18) a établi que
`ContributionAlimentaireEnfantsBeCalculator` calcule le « coût de l'enfant » via des **forfaits de
coût fixes** par tranche d'âge (280 / 350 / 420 / 500 € — méthode `forfaitParEnfant`), indépendants
des revenus des parents. Or **la méthode Renard ne fonctionne pas ainsi** : le coût de l'enfant y
est **proportionnel aux revenus des parents** (un coefficient statistique par âge, multiplié par les
revenus). L'outil revendiquait « méthode Renard » sans l'appliquer. L'opérateur a tranché : option
(a) — **ré-implémenter le vrai modèle Renard** (et non requalifier l'outil).

---

## Le vrai modèle Renard — spécification

### Principe (source : doctrine Renard, étude Ligue des familles juin 2021)

La méthode Renard, première méthode belge de calcul des contributions alimentaires (Roland Renard,
années 80), procède en 2 étapes : **(1) évaluation du coût de l'enfant**, puis **(2) répartition de
ce coût entre les parents**. Le coût de l'enfant n'est pas un forfait : il est obtenu en multipliant
les **revenus des parents** par un **coefficient statistique propre à l'âge de l'enfant**.

> *« Le calcul se base sur des coefficients d'un coût théorique de l'enfant en fonction de son âge
> [...]. Les revenus des parents sont ensuite multipliés par ce coefficient pour calculer le coût
> réel d'un enfant. »* — Ligue des familles, *Des contributions alimentaires justes pour tous les
> parents séparés*, juin 2021, p. 6.

### Modèle de coût de l'enfant retenu

```
revenuBase            = revenuParent1 + revenuParent2 + allocationsFamiliales
coutParEnfant(i)      = coefficientAge(âge_i) × revenuBase
coutGlobalEnfants     = Σ coutParEnfant(i)   pour chaque enfant i
```

- Le coût d'**un** enfant est `coefficient(âge) × revenuBase`. Le coût global de la fratrie est la
  **somme** des coûts par enfant. L'outil ne collectant qu'**une tranche d'âge** pour l'ensemble des
  enfants (input existant `trancheAgeEnfants`), le coefficient de cette tranche est appliqué à
  chaque enfant : `coutGlobal = coefficient × revenuBase × nombreEnfants`.
- **Base de revenus** : revenus cumulés des deux parents **allocations familiales incluses**. La
  méthode Renard considère que les allocations familiales sont intégralement affectées aux dépenses
  liées à l'enfant — elles font donc partie du budget familial servant d'assiette au coefficient
  (Ligue des familles 2021, p. 6 : *« la méthode considère que les allocations familiales sont
  intégralement affectées à la prise en charge des dépenses liées à l'enfant »* ; voir aussi
  Wery.legal : coefficient appliqué à *« l'addition des salaires des parents et des allocations
  familiales »*).

### Coefficients par âge

Table des coefficients de coût théorique de l'enfant par année d'âge (anniversaire de l'enfant).

| Âge | Coef. | Âge | Coef. | Âge | Coef. |
|-----|-------|-----|-------|-----|-------|
| 0   | 0,1371 | 6  | 0,1812 | 12 | 0,2254 |
| 1   | 0,1444 | 7  | 0,1885 | 13 | 0,2327 |
| 2   | 0,1517 | 8  | 0,1959 | 14 | 0,2400 |
| 3   | 0,1591 | 9  | 0,2032 | 15 | 0,2474 |
| 4   | 0,1664 | 10 | 0,2106 | 16 | 0,2548 |
| 5   | 0,1738 | 11 | 0,2180 | 17 | 0,2621 |
|     |        |    |        | 18 | 0,2695 |

**Source de la table** : P.-A. Wustefeld et R. Renard, *Proposition de contribution alimentaire,
Méthode Renard pondérée et informatisée*, De Boeck & Larcier, Bruxelles, 1996, p. 23 — reproduite
dans Ligue des familles, *Des contributions alimentaires justes pour tous les parents séparés*, juin
2021, p. 6 (PDF public liguedesfamilles.be).

### Mapping tranche d'âge → coefficient

L'outil collecte une **tranche d'âge** (`TrancheAge`), pas l'âge exact. La table Renard est
annuelle ; on retient pour chaque tranche le coefficient de l'âge **médian** de la tranche, valeur
représentative :

| `TrancheAge`     | Âge médian retenu | Coefficient | Justification |
|------------------|-------------------|-------------|---------------|
| `ENFANT_0_5`     | 3 ans             | **0,1591**  | médian de 0–5 |
| `ENFANT_6_11`    | 9 ans             | **0,2032**  | médian de 6–11 |
| `ENFANT_12_17`   | 15 ans            | **0,2474**  | médian de 12–17 |
| `ENFANT_18_PLUS` | 18 ans            | **0,2695**  | coefficient terminal de la table Renard (la table s'arrête à 18 ans) |

### Honnêteté sur les coefficients — gate avocat BE

- **Les 19 coefficients annuels (table ci-dessus) sont des valeurs sourcées** : ils proviennent
  textuellement de la publication Wustefeld-Renard 1996 reproduite par la Ligue des familles (2021).
  Confiance élevée sur la **table elle-même**.
- **Le choix du coefficient médian par tranche** (3 / 9 / 15 / 18 ans) est une **décision de
  modélisation de cet outil**, pas une règle Renard : Renard travaille à l'âge exact. C'est un
  arbitrage de représentativité dû au fait que l'outil ne collecte qu'une tranche. **À valider par
  un avocat belge** — alternative possible : faire saisir l'âge exact (hors périmètre de cette
  correction, cf. § Hors périmètre).
- **Limite connue (multi-enfants)** : la méthode Renard prend en compte le nombre d'enfants de la
  fratrie, mais la doctrine publique ne fournit pas de table de coefficients de fratrie rigoureuse
  et univoque (les règles « 20 % pour 2 enfants / 25 % pour 3 » relevées dans la littérature sont
  des ordres de grandeur simplifiés, non la formule Renard). En l'absence de source fiable, l'outil
  applique le coefficient d'âge **par enfant** et **somme** — ce qui revient à un coût linéaire au
  nombre d'enfants. C'est structurellement défendable (chaque enfant a un coût) mais **n'intègre pas
  l'effet d'économie d'échelle de la fratrie**. **À valider / affiner par un avocat belge.**
- Les coefficients sont datés (1996) et critiqués comme « plus adaptés à la réalité 2021 » par la
  Ligue des familles — le résultat reste une **estimation indicative**, jamais un montant officiel.
- Aucun coefficient n'a été inventé. Tout coefficient ou choix de modélisation non strictement issu
  d'une source publique est explicitement marqué « à valider par un avocat belge » ci-dessus et dans
  le `messages` / la Javadoc du calculateur.

### Ce qui est préservé de l'existant (confirmé correct par la contre-vérification)

Les parties suivantes du calculateur sont **inchangées** — seul le calcul du coût de l'enfant
(forfait → coefficient × revenus) est réécrit :

1. **Répartition au prorata des revenus** (CC art. 203bis) — quote-part de chaque parent =
   `revenuParent / revenuTotal`. Inchangé.
2. **Imputation des allocations familiales** sur le coût de l'enfant — `coutNet = coutGlobal -
   allocations`, planché à 0. Inchangé.
   - ⚠️ Cohérence du modèle : les allocations entrent désormais **deux fois** dans le calcul, à deux
     titres distincts et non contradictoires — (i) dans l'assiette `revenuBase` du coefficient (elles
     font partie du budget familial qui détermine le *coût* de l'enfant), puis (ii) **déduites** du
     coût ainsi obtenu (elles *financent déjà* une part de ce coût). C'est conforme à la logique
     Renard : les allocations gonflent le budget de référence ET couvrent une part du coût. Ce point
     est explicité dans la Javadoc et fait partie du gate de validation avocat BE.
3. **Déduction de la part d'hébergement en nature** — prorata des nuits, `partHebergement =
   coutNet × fractionNuits`. Inchangé.
4. **Répartition des frais extraordinaires** au prorata des revenus (CC art. 203bis §3). Inchangé.
5. Verdict 3 niveaux, normalisation des nuits ≠ 365, seuil d'équilibre, validation des inputs,
   single-country BELGIQUE. Inchangés.

### Inputs nécessaires — vérification

L'outil collecte **déjà** tous les inputs nécessaires au vrai modèle :
- `revenuMensuelParent1`, `revenuMensuelParent2` — assiette du coefficient. ✅ déjà collectés (pour
  le prorata).
- `allocationsFamilialesMensuelles` — intègre l'assiette `revenuBase`. ✅ déjà collecté.
- `trancheAgeEnfants`, `nombreEnfants` — détermine le coefficient et le nombre de coûts à sommer.
  ✅ déjà collectés.

→ **Aucun nouvel input n'est requis.** Le formulaire frontend reste identique. Le champ optionnel
`coutMensuelGlobalEnfants` (coût saisi explicitement par l'avocat) est **conservé** : s'il est
renseigné il prime sur le modèle Renard (l'avocat dispose d'un coût réel justifié) ; s'il est vide,
le modèle Renard indexé sur les revenus s'applique (au lieu du forfait).

---

## Comportement attendu

### Cas nominal

1. L'avocat soumet la situation (inchangé) : nombre d'enfants, tranche d'âge, revenus des deux
   parents, allocations familiales, nuits d'hébergement, frais extraordinaires, coût global
   optionnel.
2. Le calculateur détermine le **coût mensuel global des enfants** :
   - si `coutMensuelGlobalEnfants` est renseigné → cette valeur est retenue telle quelle ;
   - sinon → `coefficient(tranche) × (revenuParent1 + revenuParent2 + allocations) × nombreEnfants`.
3. La suite est inchangée : imputation des allocations sur le coût → coût net ; quote-part au
   prorata des revenus ; déduction de la part hébergement en nature ; verdict ; frais
   extraordinaires au prorata.
4. Le `detailCalcul` et les `messages` n'évoquent plus de « forfait » : ils explicitent le modèle
   « coefficient Renard × revenus ».

### Cas d'erreur

Inchangés (toute la validation existante est préservée) :

| Situation | Comportement | Code HTTP |
|-----------|--------------|-----------|
| Corps absent / nombre d'enfants hors 1–12 / revenu négatif / nuits hors 0–365 / commentaire > 1000 | Erreur explicite | 400 |
| Revenus des deux parents nuls | Verdict `DONNEES_INSUFFISANTES` (le prorata ne peut être établi — et le coût Renard serait nul) | 200 |
| Non authentifié / dossier d'un autre workspace / workspace non BELGIQUE | 401 / 403 / 422 |

> Nota : revenus nuls → `revenuBase` nul → coût Renard nul. Le verdict `DONNEES_INSUFFISANTES`
> existant couvre déjà ce cas (court-circuit avant le calcul du prorata) — comportement cohérent
> renforcé : sans revenus, ni le coût ni la répartition Renard ne sont calculables.

---

## Critères d'acceptation

1. `coutMensuelGlobalEnfants` non renseigné → le coût retenu vaut
   `coefficient(tranche) × (revenu1 + revenu2 + allocations) × nombreEnfants`, et **non** un forfait
   fixe. Vérifiable numériquement.
2. À tranche d'âge égale, **doubler les revenus des parents double le coût retenu** (proportionnalité
   aux revenus — cœur de la correction).
3. Les coefficients par tranche valent exactement 0,1591 / 0,2032 / 0,2474 / 0,2695.
4. `coutMensuelGlobalEnfants` renseigné → cette valeur prime (comportement préservé).
5. Prorata des revenus, imputation des allocations, déduction hébergement, frais extraordinaires :
   résultats inchangés à coût net égal (non-régression des parties préservées).
6. Le `detailCalcul` et les `messages` ne contiennent plus le mot « forfait » ; ils mentionnent le
   modèle « coefficient Renard × revenus ».
7. La Javadoc du calculateur décrit le modèle indexé sur les revenus et marque les points à valider
   par un avocat belge.
8. Aucune migration de base : le schéma de `contribution_alimentaire_enfants_be_analyses` (snapshot
   JSON `TEXT`) est inchangé.

---

## Plan de test minimal

### Tests unitaires `ContributionAlimentaireEnfantsBeCalculatorTest` (réécriture des assertions de forfait)

- `compute_coutRenard_indexeSurRevenus` : 1 enfant `ENFANT_6_11`, revenus 2000 + 2000, allocations
  0 → coût retenu = 0,2032 × 4000 = 812,80 €.
- `compute_coutRenard_inclutAllocationsDansAssiette` : revenus 2000 + 2000, allocations 200 → coût
  retenu = 0,2032 × 4200 = 853,44 €.
- `compute_coutRenard_proportionnelAuxRevenus` : à tranche et nombre d'enfants égaux, revenus
  doublés → coût retenu doublé.
- `compute_coutRenard_sommeParNombreEnfants` : 3 enfants → coût = coefficient × revenuBase × 3.
- `compute_coefficientParTranche` : les 4 tranches → coefficients 0,1591 / 0,2032 / 0,2474 / 0,2695.
- `compute_coutExplicite_primeSurRenard` : `coutMensuelGlobalEnfants` renseigné → valeur retenue
  telle quelle (préservé).
- `compute_revenusNuls_donneesInsuffisantes` : préservé.
- `compute_allocationsImputees_coutNet` : coût net = coût Renard − allocations, planché à 0
  (préservé).
- `compute_prorataRevenus` / `compute_hebergement` / `compute_fraisExtraordinaires` : non-régression
  des parties préservées.
- `compute_detail_neMentionnePasForfait` : le `detailCalcul` ne contient pas « forfait ».
- Validation (nombre d'enfants, revenu négatif, nuits, pays France) : préservée.

### Tests d'intégration `ContributionAlimentaireEnfantsBeControllerIT`

- Non-régression : POST calcul nominal → 200 + structure de réponse inchangée ; GET après calcul →
  snapshot restitué ; isolation workspace (403 cross-workspace) ; 422 workspace non-BELGIQUE. Ajuster
  uniquement les valeurs numériques attendues si une assertion portait sur un montant forfaitaire.

### Isolation workspace

Couverte par les IT existants (résolution `caseFile` → `workspace` → membership). Inchangée par
cette correction (aucune modification du service ni du contrôleur).

---

## Tables / endpoints / composants impactés

| Élément | Impact |
|---------|--------|
| `ContributionAlimentaireEnfantsBeCalculator` | **Modifié** — `forfaitParEnfant` → `coefficientRenard` ; `coutMensuelRetenu` réécrit (coefficient × revenuBase × nb enfants) ; Javadoc, `detail`, `messages` mis à jour. |
| `ContributionAlimentaireEnfantsBeCalculatorTest` | **Modifié** — assertions de forfait réécrites pour le modèle indexé. |
| `ContributionAlimentaireEnfantsBeControllerIT` | **Potentiellement modifié** — uniquement si une assertion portait sur un montant forfaitaire. |
| `contribution-alimentaire-enfants-be-section` (frontend) | **Non modifié** — la structure du résultat (`ContributionAlimentaireEnfantsBeResponse`) est inchangée ; le coût reste un montant unique `coutMensuelRetenu`. L'aide UI « Laissez le coût global vide pour appliquer un forfait par tranche d'âge » est **corrigée** : le terme « forfait » est remplacé par « le modèle Renard indexé sur les revenus ». |
| Table `contribution_alimentaire_enfants_be_analyses` | **Inchangée** — snapshot JSON `TEXT`, aucune colonne ajoutée → **aucune migration Liquibase**. |
| `decision_tool_visibility_rules` / `TOOL_REGISTRY` | **Inchangés** — aucun nouvel outil, aucun nouvel `tool_id`. |
| Records `Input` / `Result` / `Response` / `Request` | **Inchangés** — aucun champ ajouté. |

---

## Préoccupations transversales

| Déclencheur | Concerné ? |
|-------------|-----------|
| Auth / Principal | Non — aucune modification du service, du contrôleur ni de la résolution d'identité. |
| Workspace context | Non — résolution workspace inchangée. |
| Plans / limites | Non. |
| Navigation / routing | Non — aucune route, aucun écran nouveau. |
| Outil décisionnel métier | **Oui** — modification du modèle de calcul d'un outil décisionnel existant (`ContributionAlimentaireEnfantsBeCalculator`). Invariant « un outil = une situation métier » respecté : on corrige le modèle de l'outil unique « contribution alimentaire enfants BE », on n'en crée pas un second et on ne touche aucun autre outil. Scan des autres outils décisionnels : aucun autre outil n'utilise les coefficients Renard ni le coût de l'enfant — la correction est strictement localisée à ce calculateur. |

Aucune préoccupation transversale ne nécessite de liste de composants additionnelle au-delà du
tableau d'impacts ci-dessus. Les smoke tests E2E (auth / workspace / navigation) ne sont pas
déclenchés (aucun de ces axes n'est touché).

---

## Hors périmètre

- **Saisie de l'âge exact** de chaque enfant (au lieu d'une tranche commune) — permettrait d'appliquer
  le coefficient annuel exact et de gérer des enfants d'âges différents. Évolution d'input à traiter
  séparément si le besoin émerge (nouvelle SF F-217 ou F-223).
- **Coefficients de fratrie / économie d'échelle** Renard rigoureux — nécessitent une source
  doctrinale fiable absente à ce jour ; l'outil somme les coûts par enfant en attendant.
- **Lissage des hauts revenus** (la PCA, dérivée de Renard, plafonne la part au-delà de ~3 000–4 000 €
  de revenus) — non implémenté ; le modèle reste linéaire. À arbitrer ultérieurement.
- **Charges de la vie courante** déduites des revenus des parents — non implémenté (la méthode Renard
  d'origine ne les soustrait pas non plus).
- Validation juridique fine par un avocat belge — gate produit hors dev, comme pour tout F-217 V2.
- Tout changement de schéma de base, de contrat API ou de structure du résultat.

---

## Points à valider par un avocat belge (gate produit)

1. Le **choix du coefficient médian par tranche** (3 / 9 / 15 / 18 ans → 0,1591 / 0,2032 / 0,2474 /
   0,2695) comme représentant de chaque tranche d'âge.
2. L'**assiette de revenus** incluant les allocations familiales, puis la **déduction** de ces mêmes
   allocations du coût obtenu (double prise en compte assumée et conforme à la logique Renard — à
   confirmer).
3. Le traitement **multi-enfants par sommation linéaire** des coûts par enfant (absence d'effet
   d'économie d'échelle de la fratrie).
4. L'usage de la table de coefficients **Wustefeld-Renard 1996** (datée — critiquée comme « plus
   adaptée à la réalité 2021 » par la Ligue des familles) ; opportunité d'une table plus récente.
5. La **non-codification** de la méthode Renard : le résultat doit rester qualifié d'« estimation
   indicative », jamais d'un montant officiel.
6. Le maintien de la priorité du coût saisi explicitement (`coutMensuelGlobalEnfants`) sur le modèle
   Renard.

---

## Sources

- Ligue des familles, *Des contributions alimentaires justes pour tous les parents séparés — Vers une
  méthode de calcul unique et flexible*, juin 2021 — section D.1 « La méthode Renard », p. 6
  (table des coefficients reproduite). PDF public liguedesfamilles.be.
- P.-A. Wustefeld et R. Renard, *Proposition de contribution alimentaire, Méthode Renard pondérée et
  informatisée*, De Boeck & Larcier, Bruxelles, 1996, p. 23 — source primaire de la table de
  coefficients.
- Loi du 19 mars 2010 visant à promouvoir une objectivation du calcul des contributions alimentaires
  (MB 21 avril 2010, en vigueur le 1er août 2010) — impose au juge de motiver la contribution
  (art. 1321 Code judiciaire) et crée la Commission des contributions alimentaires.
- Code civil, art. 203 / 203bis — obligation d'entretien proportionnelle aux facultés ; frais
  extraordinaires.
