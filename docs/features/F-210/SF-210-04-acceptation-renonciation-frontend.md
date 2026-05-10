# SF-210-04 — Acceptation / renonciation succession FR (frontend)

## Objectif
Composant Angular minimal `<app-acceptation-renonciation-section>` qui consomme l'API SF-210-03 pour afficher les options successorales ouvertes et la recommandation prudente, avec délai restant.

## Comportement nominal
- Formulaire : date ouverture (input date), qualité héritier (radio premier/second rang), actif brut (€), passif (€), actes équivalent acceptation déjà posés (toggle), inventaire réalisé (toggle), dettes incertaines (toggle), intention exprimée (select).
- Bouton "Analyser les options" → POST → affiche options ouvertes (chips), option recommandée (banner), délai restant (en jours, en couleur navy si > 30, or si 0-30, rouge si négatif).
- Au mount, GET la dernière analyse.
- Pré-fill IA via `aiData?.dateOuvertureSuccessionDetectee`, `aiData?.actifBrutSuccessionEurDetecte`, `aiData?.passifSuccessionEurDetecte`, `aiData?.qualiteHeritierDetectee`. Pré-fill best-effort.
- Validation F-IA-03 sur `dateOuvertureSuccession` quand l'avocat saisit une date différente de la date détectée.

## Cas d'erreur
- Pays ≠ FRANCE → bannière info, pas de POST.
- Erreur HTTP → MatSnackBar.

## Critères d'acceptation
- AC1 : composant standalone créé sous `frontend/src/app/case-files/acceptation-renonciation-section/`.
- AC2 : `TOOL_REGISTRY['acceptation-renonciation-succession']` créé.
- AC3 : `KNOWN_FRONTEND_TOOL_IDS` mis à jour avec `acceptation-renonciation-succession`.
- AC4 : 1 spec Jest qui mocke service et valide affichage du verdict.
- AC5 : `static getPrefillCount()` exposé.

## Plan de test
- Spec Jest : 1 test rendering form + verdict après mock POST.

## Tables / endpoints / composants impactés
- Endpoint : `POST/GET /api/v1/case-files/{id}/acceptation-renonciation-succession`.
- Composant + service + modèles dans `frontend/src/app/`.
- TOOL_REGISTRY dans `decisional-tools-panel.component.ts`.

## Hors périmètre
- Backend (SF-210-03).
- Calcul fiscal des droits de succession.

## Analyse de cohérence transversale
- Pattern UI cohérent avec les 7 autres outils succession (`devolution-legale-section`, etc.).

## Nouveau pattern UI ou service partagé
- Aucun.

## Impact par domaine métier
- **Famille FR** : central.
- **Famille BE** : non applicable (outil BE équivalent à créer séparément).
- **Travail/Immigration** : non applicable.

## Contrat API
Importé de SF-210-03.
