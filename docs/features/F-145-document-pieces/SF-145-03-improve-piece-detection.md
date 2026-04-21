# Mini-spec — F-145 / SF-145-03 Amélioration détection pièces

## Identifiant · `F-145 / SF-145-03`
## Date · `2026-04-22` · Branche · `feat/SF-145-03-improve-piece-detection`

## Objectif
Corriger le faux positif de segmentation observé sur staging (dossier E30) : un contrat de 2 pages est découpé en 2 pièces distinctes, la page 2 est même mal classifiée en SMS. Le prompt Haiku actuel ne force pas le regroupement des pages continues appartenant au même document.

## Constat staging 2026-04-22
- Document uploadé : PDF 7 pages sur dossier E30
- Attendu : 1 contrat (p. 1-2) + autres pièces
- Obtenu : 7 pièces distinctes p. 1 / p. 2 / p. 3 … avec mauvaises classifications (p. 2 = SMS au lieu de la suite du contrat)

## Corrections prévues
1. **Renforcer le SYSTEM_PROMPT** : règles explicites de regroupement des pages continues, indications sur les pièces typiquement mono vs multi-pages, instruction finale "en cas de doute, préfère un faible nombre de pièces"
2. **Ajouter 2 exemples few-shot** dans le prompt (INPUT → OUTPUT) démontrant le regroupement correct
3. **Basculer Haiku → Sonnet** : `analyzeFast` (modelFast) → `analyze` (model). La détection de structure documentaire demande plus de raisonnement que ce que Haiku offre. Coût augmente de ~0,002 € → ~0,01 €/doc (+400 % en relatif, acceptable en absolu).

## Comportement ciblé
- Après SF-145-03, le doc E30 doit retourner 1 pièce `CONTRAT` (p. 1-2) au lieu de 2 pièces fragmentées
- Les pièces courtes (SMS, CNI) peuvent toujours être détectées mono-page
- Les contrats, lettres, attestations, bulletins doivent être regroupés si continuité détectable

## Critères d'acceptation
- [ ] Prompt `SYSTEM_PROMPT` enrichi de règles de regroupement + 2 exemples few-shot
- [ ] Appel Sonnet via `anthropicService.analyze` (au lieu de `analyzeFast`)
- [ ] Test unitaire existant passe toujours (signature inchangée, mock adapté pour `analyze`)
- [ ] Backend compile, 1000+ tests backend verts
- [ ] Aucun changement DB ni DTO ni frontend

## Plan de test minimal
- Tests unitaires existants `DocumentPieceDetectionServiceTest` passent en adaptant les mocks de `analyzeFast` → `analyze`
- Ajout test U-08 : vérifie que le prompt utilisé contient bien les mots-clés "continuité" / "regroupe" (sanity check)
- Test manuel staging post-deploy : re-uploader le même document E30, vérifier que le contrat est bien 1 seule pièce p. 1-2

## Tables / endpoints / composants impactés
- Backend : `DocumentPieceDetectionService.SYSTEM_PROMPT` (string enrichi) + 1 changement de méthode (`analyzeFast` → `analyze`)
- Tests : adaptation des mocks (1 ligne par test × 4 tests mockés)

### Pas impacté
- DB : aucune migration, aucun champ modifié
- DTO : aucun changement de contrat API
- Frontend : aucun changement (la SF-145-02 consomme déjà le résultat quel que soit le modèle source)

## Analyse de cohérence transversale
| Cible | Évaluation | Classement |
|-------|-----------|------------|
| F-145 SF-145-01 / 02 | Correctif interne, compatible sans rupture | Intégré |
| Autres appels Haiku (F-56, F-IA-03, F-120) | Restent en Haiku — pas de migration transversale | Non applicable |

## Préoccupations transversales
- Aucune

## Hors scope
- Nouvelle table, nouveau DTO, nouveau composant frontend
- Bascule d'autres consommateurs Haiku vers Sonnet (pas justifié sur leurs cas d'usage)
- Rejeu automatique des documents déjà analysés pré-SF-145-03 (les cas erronés devront être re-déclenchés manuellement, ou resteront avec leur segmentation actuelle)
