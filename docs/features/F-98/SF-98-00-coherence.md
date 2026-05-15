# F-98 — Génération de courrier / conclusions (Document de cadrage cohérence — étape 0)

**Date** : 2026-05-15
**Skill appliquée** : `ai-skills/feature-coherence-challenger.md`
**Recadrage** : ce travail avait été initié sous le numéro provisoire « F-243 ». La cartographie de cohérence a révélé que la feature existe déjà au PRODUCT_SPEC sous **F-98 « Génération de courrier / conclusions »** (statut `Hors scope` depuis 2026-04-01). Pas de doublon : on réactive F-98.
**Source signal terrain** : démo Marjolaine RENVERSEZ 13/05 (avocate FR — droit du travail). Demande répétée plusieurs fois en démo + « tous les utilisateurs de ce type d'outil le demandent ». F-98 attendait explicitement un « retour terrain positif » pour être reprise — ce signal le fournit.

---

## Verdict global

**GO avec ajustements**

Le produit est fonctionnellement **mûr** pour la génération de conclusions : presque toutes les briques amont du workflow métier existent déjà (analyse, synthèse, pièces numérotées, outils décisionnels, calcul d'indemnités, pistes stratégiques). **Un seul trou fonctionnel amont** doit être comblé avant le dev : la notion de **stade procédural du dossier**. Plus deux ajustements mineurs (export, F-DT-36).

---

## Intention métier (1 phrase)

Permettre à l'avocat de générer automatiquement, depuis un dossier analysé par LegalCase, le projet de conclusions juridiques au format adapté à la juridiction, au stade procédural et à la position défendue, en s'appuyant sur la synthèse, les pièces numérotées, les outils décisionnels et les pistes stratégiques déjà produits — puis, ultérieurement, dans le style rédactionnel propre de l'avocat.

---

## Workflow métier réel de l'utilisateur cible

**Source** : pratique standard d'un avocat en contentieux (droit du travail FR pris comme référence) + signal terrain Renversez 13/05. ⚠ Workflow validé pour le droit du travail ; à confirmer pour immigration et famille au moment de leurs SF respectives.

1. Premier rendez-vous client : recueil des faits + remise des documents (contrat, bulletins, lettres, échanges…)
2. Création du dossier + upload des pièces
3. Analyse juridique du dossier → synthèse (faits, points juridiques, risques, timeline)
4. Examen formel des pièces : numérotation, classement, force probante, pièces manquantes
5. Identification des moyens en droit (motif réel et sérieux, nullité de procédure, harcèlement, requalification…)
6. Calcul des indemnités et préjudices (chiffrage selon convention collective)
7. Définition des pistes stratégiques (négociation / saisine / abandon)
8. **Choix de la juridiction + du stade procédural + de la position** (CPH bureau de jugement / référé / appel / cassation × demandeur / défendeur)
9. **Rédaction des conclusions** adaptées au choix de l'étape 8 ⬅ **F-98**
10. Relecture et personnalisation des conclusions par l'avocat
11. Export du document + dépôt (RPVA / RPVE / greffe)
12. Suivi du calendrier procédural → audience → plaidoirie
13. Réception de la décision → analyse → appel ou exécution
14. Archivage du dossier

---

## Cartographie des features actuelles ↔ workflow

| # | Étape métier | Feature(s) LegalCase | Statut |
|---|---|---|---|
| 1 | Recueil faits + documents | F-43 import dossier | ✅ Livrée |
| 2 | Création dossier + upload pièces | F-43, F-30 multi-tenant | ✅ Livrée |
| 3 | Analyse + synthèse | F-3/F-4/F-5 pipeline IA, F-31 écran synthèse, F-14 re-synthèse | ✅ Livrée |
| 4 | Examen / numérotation pièces | F-145 identification pièces, F-146 source précise (doc+pièce+page), F-148 enrichissement visuel | ✅ Livrée |
| 4b | Détection pièces manquantes | F-92, F-194 pièces markables | ✅ Livrée |
| 5 | Moyens en droit (outils décisionnels) | F-DT-01 → F-DT-35 | ✅ Livrée — F-DT-36 (nullité procédure) au `Backlog` |
| 6 | Calcul indemnités / préjudices | Calculators F-DT (indemnités, ancienneté, rappels…) | ✅ Livrée |
| 7 | Pistes stratégiques | F-176 bloc pistes stratégiques, F-192 propagation | ✅ Livrée |
| 8 | **Choix juridiction + stade + position** | — | ❌ **Manquante (ni livrée, ni backlog)** |
| 9 | **Rédaction conclusions** | **F-98** (la feature challengée) | `Hors scope` → à réactiver |
| 10 | Relecture / personnalisation | à intégrer dans F-98 (éditeur) | 🟡 à prévoir dans F-98 |
| 11 | Export + dépôt | F-95 export Word, F-40 export PDF (synthèse uniquement) | 🟡 à étendre aux conclusions |
| 12 | Calendrier procédural | F-96 checklist procédurale, F-109 alertes, F-136/F-137 calendrier | ✅ Livrée |
| 13 | Décision → appel | — | ❌ hors périmètre actuel (acceptable) |
| 14 | Archivage dossier | dossier persistant LegalCase | ✅ Livrée |

---

## Position de la feature dans le workflow

F-98 se situe à l'**étape 9**. Elle consomme les sorties des étapes 3 à 8 et alimente les étapes 10-11.

---

## Challenge amont — la séquence métier tient-elle ?

**Question** : chaque étape avant l'étape 9 est-elle couverte par une feature du produit (livrée ou backlog) ?

| Étape amont | Couverte ? | Analyse |
|---|---|---|
| 1-2 Recueil / upload | ✅ | F-43 livrée |
| 3 Analyse + synthèse | ✅ | Pipeline IA livré |
| 4 Examen pièces | ✅ | F-145/146/148 livrées — la citation « Pièce n° X » dans les conclusions pourra s'appuyer dessus |
| 4b Pièces manquantes | ✅ | F-92/194 livrées |
| 5 Moyens en droit | ✅ partiel | F-DT-01→35 livrées. **F-DT-36 (nullité procédure) au backlog** — sans elle, les conclusions DT passent à côté du moyen le plus fort dans une partie des dossiers licenciement |
| 6 Calcul indemnités | ✅ | Calculators F-DT livrés — les demandes chiffrées des conclusions s'en serviront |
| 7 Pistes stratégiques | ✅ | F-176/192 livrées |
| 8 **Choix juridiction + stade + position** | ❌ | **TROU FONCTIONNEL**. Aucune feature ne porte le stade procédural du dossier. Sans cette information, F-98 ne sait pas quel type de conclusions générer (bureau de jugement ? référé ? appel ? quelle position ?). |

**Verdict challenge amont** : 1 trou bloquant (étape 8). Les autres étapes sont couvertes. F-DT-36 est un trou de qualité, pas un trou bloquant d'architecture.

---

## Challenge aval — la sortie est-elle exploitable ?

| Étape aval | Couverte ? | Analyse |
|---|---|---|
| 10 Relecture / personnalisation | 🟡 | À intégrer dans F-98 elle-même (éditeur de conclusions). Invariant : l'avocat doit pouvoir modifier avant export |
| 11 Export + dépôt | 🟡 | F-95 (Word) et F-40 (PDF) existent mais n'exportent que la **synthèse**. À étendre aux conclusions. Le dépôt RPVA lui-même est hors périmètre (OK — l'avocat dépose dans son outil barreau) |
| 12 Calendrier procédural | ✅ | F-96/109/136/137 livrées |
| 13 Décision → appel | ❌ | Hors périmètre actuel — acceptable, ce n'est pas une dépendance de F-98 |
| 14 Archivage | ✅ | Le document généré sera rattaché au dossier |

**Verdict challenge aval** : pas de trou bloquant. 2 ajustements : intégrer un éditeur dans F-98, étendre l'export aux conclusions.

---

## STOPs / pré-requis à ajouter au backlog

| # | Élément | Type | Action |
|---|---|---|---|
| 1 | **Feature « Stade procédural du dossier »** (juridiction + stade + position portés par le dossier) | 🔴 Pré-requis bloquant amont | À créer au backlog comme nouvelle feature (numéro F-243 désormais libre). Doit être livrée **avant** SF-98-01 |
| 2 | **F-DT-36 nullité procédure** | 🟡 Pré-requis qualité (volet DT) | Déjà au backlog (2026-05-14). À livrer avant SF-98-01 pour que les conclusions DT soient complètes — non bloquant pour l'architecture générale de F-98 |
| 3 | **Extension export Word/PDF aux conclusions** | 🟡 Ajustement aval | Soit SF dédiée de F-98, soit extension de F-95/F-40. À trancher en mini-spec |

---

## Invariants anti-gadget pour la mini-spec

À reprendre dans la mini-spec SF-98-01 :

1. **Sélection obligatoire du stade** avant génération (juridiction + stade procédural + position) — la feature « Stade procédural du dossier » fournit ces données ; F-98 ne génère rien sans elles
2. **Citation des pièces par numéro** (« Pièce n° X ») dans les conclusions, en s'appuyant sur F-145/F-146, avec lien interne vers la pièce
3. **Reprise des outils décisionnels activés** : chaque outil F-DT/IM/FA rempli doit apparaître dans le moyen juridique correspondant des conclusions
4. **Reprise des pistes stratégiques** (F-176) : les conclusions reflètent la stratégie retenue, pas une stratégie générique
5. **Demandes chiffrées** depuis les calculators d'indemnités, pas de montants réinventés
6. **Éditeur de relecture** : l'avocat modifie le texte avant export (étape 10 du workflow)
7. **Versions explicites** : brouillon → validé → déposé, avec historique
8. **Re-génération signalée** : si un input amont change (outil, pièce, piste) après génération → bandeau « conclusions à régénérer »
9. **Export réutilisable** : document Word ≥ 80 % utilisable sans retouche de mise en forme (structure barreau standard)
10. **Transparence** : bandeau « Projet généré automatiquement — relecture obligatoire avant dépôt » (couvre le risque « responsabilité juridique » noté à la mise en stand-by de F-98 le 01/04)

Ces invariants répondent directement aux 3 risques qui avaient mis F-98 en stand-by le 2026-04-01 : qualité rédactionnelle (invariants 2-6), responsabilité juridique (invariant 10), complexité templates (le périmètre matriciel ci-dessous découpe la complexité en SF unitaires).

---

## Périmètre F-98 — matrice exhaustive des types de conclusions

F-98 couvre la génération de courriers ET de conclusions. Le présent cadrage traite le **volet conclusions** (déclencheur = signal Renversez). Le volet courrier (mises en demeure, lettres) fera l'objet d'un cadrage distinct ultérieur.

1 SF par cellule. Périmètre volontairement large pour ne rien oublier ; la V1 dev n'en livrera qu'une fraction.

### Droit du travail

| Pays | Juridiction | Stade | Position | SF |
|---|---|---|---|---|
| FR | CPH | Bureau de jugement (fond) | Demandeur (salarié) | SF-98-01 |
| FR | CPH | Bureau de jugement (fond) | Défendeur (employeur) | SF-98-02 |
| FR | CPH | Référé | Demandeur | SF-98-03 |
| FR | CPH | Référé | Défendeur | SF-98-04 |
| FR | CPH | Départage | Demandeur | SF-98-05 |
| FR | CPH | Départage | Défendeur | SF-98-06 |
| FR | CA chambre sociale | Appel | Appelant | SF-98-07 |
| FR | CA chambre sociale | Appel | Intimé | SF-98-08 |
| FR | Cass chambre sociale | Pourvoi (mémoire ampliatif) | Demandeur au pourvoi | SF-98-09 |
| FR | Cass chambre sociale | Pourvoi (mémoire en défense) | Défendeur au pourvoi | SF-98-10 |
| BE | Tribunal du travail | Fond | Demandeur | SF-98-11 |
| BE | Tribunal du travail | Fond | Défendeur | SF-98-12 |
| BE | Président tribunal travail | Référé | Demandeur | SF-98-13 |
| BE | Président tribunal travail | Référé | Défendeur | SF-98-14 |
| BE | Cour du travail | Appel | Appelant | SF-98-15 |
| BE | Cour du travail | Appel | Intimé | SF-98-16 |
| BE | Cass | Pourvoi | DPV | SF-98-17 |

### Droit de l'immigration

| Pays | Juridiction | Stade / type | Position | SF |
|---|---|---|---|---|
| FR | Tribunal administratif | Recours OQTF | Requérant | SF-98-18 |
| FR | TA juge des référés | Référé liberté (L.521-2 CJA) | Requérant | SF-98-19 |
| FR | TA juge des référés | Référé suspension (L.521-1 CJA) | Requérant | SF-98-20 |
| FR | Cour administrative d'appel | Appel | Appelant | SF-98-21 |
| FR | Conseil d'État | Cassation | DPV | SF-98-22 |
| FR | CNDA | Recours asile | Requérant | SF-98-23 |
| FR | Préfecture / OFII | Mémoire admission au séjour (hors contentieux) | Demandeur titre | SF-98-24 |
| FR | TA | Recours refus titre / regroupement | Requérant | SF-98-25 |
| BE | Conseil du contentieux des étrangers | Recours plein contentieux | Requérant | SF-98-26 |
| BE | CCE | Référé suspension extrême urgence | Requérant | SF-98-27 |
| BE | Conseil d'État (BE) | Cassation administrative | DPV | SF-98-28 |
| BE | Office des étrangers | Mémoire admission au séjour (hors contentieux) | Demandeur titre | SF-98-29 |

### Droit de la famille

| Pays | Juridiction | Stade / type | Position | SF |
|---|---|---|---|---|
| FR | JAF (TJ) | Divorce au fond | Demandeur | SF-98-30 |
| FR | JAF | Divorce au fond | Défendeur | SF-98-31 |
| FR | JAF | Mesures provisoires (art. 255 Cciv) | Demandeur | SF-98-32 |
| FR | JAF | Mesures provisoires | Défendeur | SF-98-33 |
| FR | JAF | Référé / juge délégué | Demandeur | SF-98-34 |
| FR | JAF | Ordonnance de protection (violences) | Requérant | SF-98-35 |
| FR | CA chambre de la famille | Appel | Appelant | SF-98-36 |
| FR | CA chambre de la famille | Appel | Intimé | SF-98-37 |
| FR | Cass 1ère chambre civile | Pourvoi | DPV / DDV | SF-98-38 |
| FR | TJ | Filiation (établissement, contestation) | Demandeur | SF-98-39 |
| FR | TJ | Succession (partage judiciaire, indivision) | Demandeur | SF-98-40 |
| BE | Tribunal de la famille | Fond (divorce, autorité parentale) | Demandeur | SF-98-41 |
| BE | Tribunal de la famille | Fond | Défendeur | SF-98-42 |
| BE | Tribunal de la famille | Référé (mesures urgentes) | Demandeur | SF-98-43 |
| BE | Cour d'appel chambre famille | Appel | Appelant / Intimé | SF-98-44 |
| BE | Cass | Pourvoi | DPV / DDV | SF-98-45 |

### Style learning (transversal)

| SF | Périmètre |
|---|---|
| SF-98-46 | Ingestion corpus historique avocat (upload conclusions Word/PDF, parsing, anonymisation) |
| SF-98-47 | Style mimicking : génération adaptée au style rédactionnel appris |
| SF-98-48 | UI cabinet : gestion du corpus (ajout, suppression, désactivation par dossier) |

### Workflow / export (transversal)

| SF | Périmètre |
|---|---|
| SF-98-49 | Éditeur riche de relecture / modification avant validation |
| SF-98-50 | Export Word `.docx` des conclusions (format barreau standard) |
| SF-98-51 | Export PDF des conclusions avec en-tête cabinet |
| SF-98-52 | Versions multiples par dossier (brouillon / validé / déposé) + historique |
| SF-98-53 | Re-génération signalée si modification d'un input amont |

**Total : ~53 SF.** Périmètre de cadrage exhaustif — la V1 dev en livrera moins de 10 %.

---

## Décision finale

**GO avec ajustements.**

1. **Réactiver F-98** : statut `Hors scope` → `Backlog` au PRODUCT_SPEC, avec la matrice 53 SF ci-dessus comme périmètre et ce document comme cadrage de cohérence (étape 0).
2. **Créer la feature pré-requise « Stade procédural du dossier »** (numéro F-243 désormais libre) — à ajouter au backlog. Bloquant : SF-98-01 ne peut pas démarrer avant sa livraison.
3. **F-DT-36** (déjà au backlog) à livrer avant SF-98-01 pour la complétude du volet droit du travail.
4. **V1 dev proposée** : SF-98-01 uniquement (Conclusions CPH bureau de jugement FR, demandeur, droit du travail) — combinaison la plus fréquente, alignée sur le signal Renversez. Les 52 autres SF seront livrées par vagues selon signal terrain.
5. La mini-spec SF-98-01 ne peut démarrer qu'après : (a) validation de ce document par le user, (b) livraison de F-243 « Stade procédural du dossier », (c) livraison de F-DT-36.

---

## Liens

- `ai-skills/feature-coherence-challenger.md` — skill appliquée
- [[project_renversez_post_demo_13_05]] — source du signal terrain
- `docs/PRODUCT_SPEC.md` — F-98 (à réactiver), F-DT-36 (pré-requis), F-243 « Stade procédural du dossier » (pré-requis à créer)
- F-145 / F-146 — identification et source précise des pièces (support de l'invariant 2)
- F-176 / F-192 — pistes stratégiques (support de l'invariant 4)
- F-241 / F-242 — connecteur et citation jurispru (coordination pour enrichir les conclusions)
