# F-98 (SF-98-62 / SF-98-63) — Cadrage cohérence (étape 0)

> Extension de la matrice de génération de conclusions F-98 au stade **Bureau de Conciliation et d'Orientation (BCO)** du Conseil de prud'hommes FR.
> Couvre les deux nouvelles cellules : **SF-98-62** (CPH / BCO / Demandeur) et **SF-98-63** (CPH / BCO / Défendeur).

## Verdict : **GO**

Trou de couverture pur dans une matrice établie. Toutes les briques amont et aval existent et sont livrées. Aucun pré-requis fonctionnel manquant.

## Intention métier (1 phrase)

Permettre à l'avocat dont le dossier prud'homal est au stade Bureau de Conciliation et d'Orientation (porte d'entrée de la saisine du CPH) de générer l'acte écrit correspondant — une requête de saisine valant conclusions (demandes au fond + provisions en référé) côté demandeur, ou des conclusions/observations en défense côté défendeur — au lieu de buter sur un blocage muet.

## Workflow métier réel de l'utilisateur cible

**Source** : signal terrain documenté — dossier réel **STANOJEVIC c/ SARL SWIFT'TRANSPORT** de Me Marjolaine RENVERSEZ (Barreau de Montpellier), document joint « REQUETE AUX FINS DE SAISINE DEVANT LE CONSEIL DES PRUD'HOMMES VALANT CONCLUSIONS » (reçu 2026-06-11) + `TravailProcedureReferentiel.PRUDHOMMES_FR` (jalons R.1452-3, L.1454-1).

1. Le salarié consulte l'avocat (ici : impayés de salaire + rupture de période d'essai après accident du travail).
2. L'avocat réunit les pièces (contrat, arrêts de travail, bulletins, échanges) et qualifie les demandes.
3. L'avocat **rédige la requête de saisine du CPH** — l'acte qui saisit le conseil. En pratique, cette requête **vaut conclusions** : elle expose faits, moyens et demandes (au fond) et peut porter un **volet référé** (provisions sur salaires impayés). C'est l'acte produit **à l'entrée de la procédure, qui passe d'abord par le BCO**.
4. Le greffe convoque les parties au **Bureau de Conciliation et d'Orientation (BCO)** (convocation ≥ 15 j, R.1452-3).
5. **Audience BCO** : tentative de conciliation ; en cas de demande de provision, la formation de référé peut ordonner des sommes (L.1454-1).
6. En cas d'échec de la conciliation → **renvoi au bureau de jugement** (fond, L.1454-1-2).
7. Audience du bureau de jugement → jugement et notification.
8. (Côté défendeur/employeur) : à réception de la convocation BCO, l'avocat de l'employeur prépare ses **observations / conclusions en défense**.

→ L'acte écrit des étapes 3 et 8 est aujourd'hui **non générable** dans LegalCase : le stade BCO existe mais n'a pas de cellule de conclusions.

## Cartographie features actuelles ↔ workflow

| Étape workflow métier | Feature(s) LegalCase | Statut |
|---|---|---|
| 1-2. Réception dossier, upload & analyse des pièces | F-3/F-4/F-5 (analyse de dossier), pipeline IA | ✅ Livrée |
| 2. Qualification des demandes / outils décisionnels (rupture période d'essai, AT/MP, rappel de salaire…) | F-DT-37, F-DT-33, rappel_salaire, etc. | ✅ Livrées |
| Réglage juridiction + **stade (dont BCO)** + position | F-243 (stade procédural) | ✅ Livrée — **propose le BCO** |
| 3 / 8. **Génération de l'acte écrit au stade BCO** | **F-98 — cellules SF-98-62 / SF-98-63 (LA feature challengée)** | ❌ **Trou** (cellules Fond/Référé/Départage/Appel/Cassation existent, **pas BCO**) |
| Numérotation des pièces + bordereau | F-260 + SF-98-57 | ✅ Livrées |
| Réfutation jurisprudence adverse / vérif citations | SF-98-56 + F-179/JUDILIBRE | ✅ Livrées |
| Rendu « document juridique » + relecture/édition | F-259 + SF-98-49 | ✅ Livrées |
| Export Word / PDF | SF-98-50 / SF-98-51 | ✅ Livrées |
| Garde qualité rédactionnelle (anti-jargon, syllogisme, dispositif) | SF-98-55 (`REDACTION_QUALITY_GUARD`) | ✅ Livrée |
| Stratégie de conciliation (faut-il concilier ? BCA vs barème Macron) | **F-DT-84 (outil décisionnel BCO)** | ✅ Livrée — **distinct de l'acte écrit** |

## Position de la nouvelle feature

Étapes 3 (demandeur) et 8 (défendeur) du workflow : production de l'**acte écrit** au stade BCO. S'insère comme deux nouvelles cellules du registre `ConclusionPromptProvider` (combinaison `DROIT_DU_TRAVAIL / CPH / BCO / {DEMANDEUR, DEFENDEUR}`).

## Challenge amont

*« Chaque étape AVANT la génération est-elle couverte ? »* — **OUI, intégralement :**
- Analyse de dossier ✅, outils décisionnels ✅, **stade procédural BCO sélectionnable** ✅ (F-243), pièces numérotées ✅ (F-260).
- Aucun trou amont. La preuve : sur le dossier réel, l'analyse était `DONE` (16/16 pièces) et le stade renseigné `CPH/BCO/Demandeur` — seule la cellule de génération manquait.

## Challenge aval

*« La sortie est-elle exploitable par les étapes suivantes ? »* — **OUI :**
- Rendu document juridique (F-259), relecture/édition (SF-98-49), versions (SF-98-52), **bordereau de pièces** (SF-98-57), **export Word/PDF** (SF-98-50/51), bandeau de régénération (SF-98-53) : tout le pipeline aval est commun à la matrice et déjà livré. Une nouvelle cellule en hérite par construction.

## STOPs / pré-requis à ajouter au backlog

**Aucun.** Toutes les briques amont/aval sont livrées. C'est un comblement de couverture, pas une feature à fondations manquantes.

## Invariants anti-gadget pour la mini-spec

1. **L'acte BCO Demandeur (SF-98-62) doit produire une *requête de saisine valant conclusions*** (faits + moyens + demandes au fond + volet référé/provisions le cas échéant), pas un courrier générique ni de simples « conclusions au fond » de bureau de jugement.
2. **L'acte BCO Défendeur (SF-98-63) doit produire des observations/conclusions en défense** adaptées à la phase de conciliation (contestation des demandes, position sur la conciliation).
3. **Pas de doublon avec F-DT-84** : la cellule génère l'**acte écrit**, jamais l'analyse stratégique « faut-il concilier / comparaison BCA vs Macron » (qui reste l'outil décisionnel F-DT-84). Frontière explicite dans le prompt.
4. **Réutilisation des gardes communes** : `REDACTION_QUALITY_GUARD` (anti-jargon, syllogisme, dispositif complet : art. 700, dépens, exécution provisoire, intérêts + capitalisation) et `JURISPRUDENCE_GUARD` — non-régression.
5. **Ancrage pièces** : renvois « Pièce n° X » sur la numérotation persistante (F-260) + bordereau (SF-98-57) ; zéro pièce hallucinée.
6. **Couvrir les DEUX positions** (Demandeur + Défendeur) dans la même livraison pour ne pas recréer un demi-trou de couverture.
7. **Garde-fou d'intégrité anti-récidive** : un test qui échoue si une combinaison sélectionnable (domaine × juridiction × stade × position du `ProcedureStageCatalog`) n'a **ni** cellule `ConclusionPromptProvider` **ni** message explicite prévu. C'est ce garde-fou qui empêche qu'un futur stade ajouté au catalogue recrée un blocage muet.

## Décision finale

**GO** — enchaîner l'étape 0 bis (cohérence écran : impact minimal, mais la génération devient possible sur un stade où le bouton était muet → à cadrer) puis la mini-spec SF-98-62 / SF-98-63. Statut PRODUCT_SPEC : `Backlog` → `À faire`.
