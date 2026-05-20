# SF-246-29 — Pré-remplissage IA exhaustif de l'outil F-DT-38 (rupture de période d'essai)

## Statut

`À faire` — SF de dette planifiée pendant la livraison F-DT-38 (PR #1135, 2026-05-20).

## Contexte

L'outil décisionnel F-DT-38 (rupture de période d'essai — qualification régulière / abusive / nulle / illégale) a été livré le 2026-05-20 avec un **pré-fill IA partiel** : **9 champs sur 22** sont effectivement pré-remplis par l'IA à partir des champs IA existants (date début contrat, ancienneté, type de contrat, convention collective applicable, etc., déjà extraits par F-DT-08 / F-DT-22 / F-DT-25).

Les **13 champs restants** sont **spécifiques à la situation "période d'essai"** et n'ont pas d'équivalent dans les `*ExtractedData` actuels — l'IA ne les extrait donc jamais, et le composant frontend les laisse vides à l'ouverture du formulaire.

Cette SF brache complètement l'outil F-DT-38 sur l'invariant **« tous les champs »** établi par F-246 le 2026-05-19 (« tout champ saisissable d'un outil décisionnel doit être pré-rempli par l'IA ; seule exception admise = info absente des documents uploadés »).

Décision product owner 2026-05-20 (après livraison F-DT-38) : F-246 est **rouverte** pour absorber cette SF de complément — cohérent avec son scope « tous les outils décisionnels ».

## Objectif

En une phrase : étendre le contrat de données backend (`*ExtractedData` + prompt LLM + extracteur) pour que les 13 champs spécifiques de la section `rupture-periode-essai-section` soient pré-remplis par l'IA dès l'ouverture du formulaire, sans intervention manuelle préalable de l'avocat.

## Périmètre

### Champs à brancher (estimatif — à finaliser après inspection du composant frontend)

Les **13 champs spécifiques période d'essai** (à confirmer en mini-spec contre `rupture-periode-essai-section.component.ts`) :

1. `dateDebutPeriodeEssai` — date de début effective de la période d'essai (souvent = date début contrat ; à pré-remplir distinctement quand le contrat le précise explicitement)
2. `dureePeriodeEssaiContractuelle` — durée prévue au contrat (jours ou mois)
3. `categorieSocioProfessionnelle` — ouvrier / employé / agent de maîtrise / cadre (détermine durée légale max L.1221-19)
4. `dureeLegaleMaximale` — durée légale applicable (calculée à partir de la catégorie + type de contrat, mais pré-fill explicite utile pour traçabilité)
5. `presenceRenouvellement` — oui/non
6. `dateRenouvellement` — date du renouvellement si applicable
7. `accordEcritRenouvellement` — confirmation accord exprès écrit du salarié (L.1221-23, jamais tacite)
8. `motifInvoque` — texte du motif tel qu'il apparaît dans la notification de rupture
9. `presenceLettreRupture` — oui/non (lettre formelle motivée distincte d'une notification orale)
10. `motifsAveresDansLettre` — qualité des motifs dans la lettre (atténuation possible `ILLEGALE → RISQUE_ABUSIVE`)
11. `etatSanteSalarie` — arrêt maladie / AT-MP / grossesse / aucun (déclenche régimes protecteurs L.1226-9 / L.1225-1)
12. `derogationConventionnelle` — convention collective prévoit-elle une durée ou un préavis plus favorable au salarié ?
13. `derogationApplicableDescription` — texte de la dérogation conventionnelle effectivement opposable

### Architecture backend (pattern miroir SF-246-13 / SF-246-21)

1. **Extension record `TravailExtractedData`** dans `backend/src/main/java/.../analysis/CaseAnalysisResponse.java` — ajout d'un sous-objet `rupturePeriodeEssaiDetail` (record nested) avec les 13 champs.
2. **Extension prompt LLM** `LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION` — ajout d'une section dédiée demandant explicitement à l'IA d'extraire chaque champ avec ses critères (catégorie socio-pro = lecture du contrat de travail ; convention collective = lecture du bulletin OU du contrat ; etc.).
3. **Extension extracteur** dans `CaseAnalysisService` — parsing du nouveau sous-objet, fallback heuristique sur les champs déjà extraits (ex. `categorieSocioProfessionnelle` peut être déduite de `fonction` + `convention_collective_code` via le référentiel).
4. **Frontend** : aucune nouvelle modification — le helper `rupture-periode-essai-section-prefill-rules.ts` consomme déjà `aiData.travailExtracted.*` ; l'ajout du sous-objet le rendra automatiquement disponible. Vérifier toutefois que les 13 champs sont bien lus dans le helper (sinon étendre le helper).

## Critères d'acceptation

- [ ] Sur un dossier qui contient un contrat de travail explicite + une lettre de rupture motivée pendant la période d'essai, l'IA pré-remplit **les 13 champs** (les 22 au total) — vérifié sur un dossier de test dédié.
- [ ] Les champs non présents dans les documents uploadés restent vides sans erreur (pas de valeurs hallucinées).
- [ ] La validation F-IA-03 (cohérence) reste opérationnelle sur les champs pré-remplis (cross-check date début vs date rupture vs durée légale).
- [ ] Garde-fou `CritereCodeIntegrityIT` reste vert (les `critereCode` F-DT-38 sont déjà émis depuis SF-DT-38 ; cette SF n'en ajoute pas de nouveau).
- [ ] Aucun champ saisissable du formulaire `rupture-periode-essai-section` ne reste non pré-remplissable par conception (= aucune exception ajoutée à la liste « champs structurellement non extractibles » sans justification documentée).

## Plan de test

- **UT backend** : nouveau test `LegalDomainPromptBuilderTest` vérifiant que le prompt contient bien les 13 nouveaux champs dans la section attendue.
- **UT backend** : test du parser dans `CaseAnalysisServiceTest` sur une réponse LLM mock contenant le sous-objet `rupturePeriodeEssaiDetail` complet → record JPA correctement peuplé.
- **IT backend** : pipeline complet sur un fichier de test (contrat de travail + lettre de rupture) → vérifier que les 13 champs apparaissent dans la réponse de l'endpoint `GET /api/v1/case-files/{id}/analysis`.
- **Frontend** : si le helper `rupture-periode-essai-section-prefill-rules.ts` doit être étendu, ajouter les specs Jest correspondantes (pré-remplissage des 13 champs depuis `aiData`).
- **Test manuel** : sur staging, créer un dossier avec le contrat de Thomas Dupont du jeu de test (qui contient une période d'essai au sens contractuel) + une lettre de rupture pendant période d'essai → ouvrir l'outil F-DT-38 → vérifier visuellement que les 22 champs sont pré-remplis.

## Tables / endpoints / composants impactés

- **Backend** :
  - `backend/src/main/java/.../analysis/CaseAnalysisResponse.java` — record `TravailExtractedData` (ajout sous-objet)
  - `backend/src/main/java/.../analysis/LegalDomainPromptBuilder.java` — prompt `TRAVAIL_INSTRUCTION` (extension)
  - `backend/src/main/java/.../analysis/CaseAnalysisService.java` — parser/extractor du nouveau sous-objet
- **Frontend** (potentiel) :
  - `frontend/src/app/case-files/rupture-periode-essai-section/rupture-periode-essai-section-prefill-rules.ts` — extension helper si nécessaire
  - `frontend/src/app/core/models/case-analysis.model.ts` — extension du type `TravailExtracted` (interface TS)
- **Pas de migration DB** — extension contractuelle backend, pas de nouvelle table.
- **Pas de modification visibility seed** — l'outil F-DT-38 est déjà visible (SF-DT-38, migration 256).

## Hors périmètre

- Modification du calcul / verdict F-DT-38 (le Calculator reste tel que livré par SF-DT-38).
- Ajout de nouveaux critères de qualification (les 4 verdicts REGULIERE / RISQUE_ABUSIVE / NULLE / ILLEGALE_REQUALIF_LICENCIEMENT sont gelés).
- Création d'une nouvelle section frontend.
- Jumeau BE F-DT-39 (à backloger uniquement si signal terrain Belgique).

## Préoccupations transversales cochées

- **Outil décisionnel métier** : extension du pré-fill IA d'un outil existant (F-DT-38) — pas de mélange de situations, pas de modification du Calculator. Conforme à l'invariant `un outil décisionnel = une situation métier`.

## Pré-requis

- F-DT-38 livrée (PR #1135 mergée 2026-05-20) ✅
- F-246 réouverte pour absorber cette SF (décision product owner 2026-05-20) ✅
- Pattern SF-246-XX prouvé sur 28 SF antérieures ✅

## Source

- Livraison F-DT-38 PR #1135 (2026-05-20), agent autonome `a16d35399ec288293` — section « Tâches restantes après confirmation merge » du rapport final : « SF F-246 dédiée à créer pour le pré-fill IA exhaustif (10+ nouveaux champs IA `rupture_periode_essai_detail`) ».
- Décision product owner 2026-05-20 : option 1 — rouvrir F-246 avec SF-246-29 plutôt que créer une nouvelle feature F-25X (cohérence avec scope F-246 « tous les outils décisionnels »).
