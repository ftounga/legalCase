# Feedback terrain — Me Marjolaine RENVERSEZ (Barreau de Montpellier) — 2026-06-11

> Source : réponse mail au « une dernière question, sans relance » (fil `19e97bc8d5c1342a`, mail `19eb5efce71d8e4e`).
> Contexte : seule avocate ayant utilisé LegalCase sur un **vrai dossier en prod** (dossier **STANOJEVIC**, cf. [[project_renversez_post_demo_13_05]]). Retour candide + conclusions au fond jointes (`renversez-conclusions-fond.docx`).

## Le dossier (vérifié dans le .docx)
- **M. Amine STANOJEVIC** c/ **SARL SWIFT'TRANSPORT** — CPH Montpellier (référé + fond), CDI chauffeur livreur du 11/12/2025.
- Accident du travail 08/01/2026 → arrêt de travail 14/01/2026 (validité contestée par l'employeur) → arrêt 20–22/01.
- Cœur juridique : **nullité de la rupture de la période d'essai = discrimination liée à l'état de santé** ; rappels de salaire, heures supp., travail dissimulé, obligation de sécurité, demandeur à l'aide juridictionnelle.

## Griefs (verbatim → analyse → statut)

| # | Grief | Nature | Statut produit |
|---|-------|--------|----------------|
| 1 | « Votre IA ne m'a pas permis de générer des conclusions » malgré conclusions-type fournies (nullité période d'essai / discrimination) ; « les IA généralistes adaptent les écritures à un autre dossier » | 🐛 **Bug bloquant génération conclusions** | À REPRODUIRE (dossier STANOJEVIC dispo) — F-98/F-243 |
| 2 | « Chance de succès 80 % » irréaliste : à Montpellier ≈ 50 %, CPH défavorable, aucun dossier intégralement gagné depuis 2017 | ⚠️ **Sur-promesse pronostic** (crédibilité) | Recalibrer/nuancer l'outil pronostic — pas de % sans intervalle/réserve juridiction |
| 3 | « Un arrêt de travail ne concernait pas mon client » → **inexact** (le dossier EST bâti sur ses arrêts) | 🐛 **Erreur d'attribution/extraction** pièce↔partie | À investiguer (pipeline extraction) |
| 4 | « Vos offres ne permettent pas de visibilité sur le coût mensuel prévisible » | 💶 Friction pricing | Réel — clarifier le pricing |
| 5 | « Il faut doubler l'abonnement avec un logiciel de données juridiques » → intégrer **Légifrance + Judilibre** (accès libre) | 🎯 Manque perçu | Judilibre Cassation déjà intégré (vérif citations) ; pas exposé à l'utilisateur comme base consultable. Légifrance non intégré |
| 6 | Doctrine : offre IA < 300 € + 18 M décisions + **numérotation auto des pièces** + remplissage **aide juridictionnelle** | 🥊 Concurrence citée | Numérotation/bordereau pièces = **déjà livré** (F-260/SF-98-57, prod 10/06). Aide juridictionnelle = piste |
| 7 | « Je ne cherche pas un logiciel pour lister les demandes… même s'il évite un oubli » | Positionnement | LegalCase doit produire de la valeur rédactionnelle, pas un checklist |

## Déjà corrigé sur la prod déployée le 10/06 (à lui montrer factuellement)
- **Numérotation + bordereau des pièces** (F-260 / SF-98-57) → adresse directement le point 6.
- **Réfutation de la jurisprudence adverse** + rendu Word/PDF propre des conclusions (F-259) → qualité du livrable.
- **Vérification des citations via JUDILIBRE** (SF-179-05) → fiabilité jurisprudence.

## Pistes backlog à VALIDER (PO) — ne pas implémenter sans passer la séquence
1. **Recalibrage du pronostic** (point 2) : bande de probabilité + réserve explicite « variabilité par juridiction / formation », jamais un % sec sur-vendeur. Invariant « silence/prudence > sur-promesse ».
2. **Robustesse génération conclusions à partir d'un modèle fourni** (point 1) : reproduire le bug, comprendre l'échec quand l'avocat fournit ses conclusions-type.
3. **Fix attribution pièce↔partie** (point 3) : un document médical/arrêt de travail du demandeur ne doit pas être déclaré « ne concerne pas le client ».
4. **Visibilité coût** (point 4) : estimation du coût mensuel prévisible / plafond.
5. (Observation) Aide juridictionnelle : remplissage assisté — à évaluer vs périmètre.

## Action relationnelle
Réponse fondateur soignée (humble, point par point, sans sur-vendre), remerciement pour le .docx, proposer de reproduire le blocage conclusions sur son dossier et de lui revenir. Pas de relance commerciale.
