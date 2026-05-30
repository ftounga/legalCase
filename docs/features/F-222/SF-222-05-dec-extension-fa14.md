# Mini-spec — F-222 / SF-222-05 — Extension DEC dans F-FA-14 (ordonnance de protection)

## Identifiant
`F-222 / SF-222-05` — **extension** de `F-FA-14-ordonnance-protection` (PAS un nouvel outil — cf. étape 0, même situation que le BAR déjà intégré)

## Objectif (1 phrase)
Ajouter, dans l'outil existant ordonnance de protection, l'évaluation de l'opportunité d'un Dispositif Électronique Commun (DEC — suivi électronique anti-rapprochement du conjoint violent), au même titre que le BAR.

## Comportement
Ajout dans le formulaire/verdict existant de `F-FA-14` :
- Entrée additionnelle `decEnvisage` (bool) + réutilisation des critères de violences/danger déjà présents.
- Le DEC peut être prononcé dans le cadre d'une ordonnance de protection (mesure de surveillance électronique du respect de l'interdiction de rapprochement).
- Le verdict de F-FA-14 mentionne, quand les conditions d'interdiction de rapprochement sont réunies, l'**option DEC** (en plus / à la place du BAR selon le besoin de suivi du contact).

## Critères d'acceptation
- [ ] `F-FA-14` propose l'option DEC quand l'interdiction de rapprochement est retenue.
- [ ] Aucune nouvelle table / nouvel outil / nouvelle card (extension pure de l'existant).
- [ ] Anti-régression : le comportement existant de F-FA-14 (BAR inclus) reste intact (tests existants verts).

## Plan de test
UT service F-FA-14 (nouveau cas DEC), Jest composant ordonnance-protection (option DEC présente, anti-régression BAR).

## Tables / composants
- Backend : extension du service/DTO de `F-FA-14-ordonnance-protection` (champ `decEnvisage` + logique verdict). **Migration éventuelle** : ajout colonne `dec_envisage` à la table d'analyses existante de F-FA-14 (si stockage).
- Frontend : ajout du champ + mention DEC dans `ordonnance-protection-section.component` (existant).
- Champs IA : `decEnvisage` ajouté au record Famille si pertinent.

## Hors périmètre
Création d'un outil DEC séparé (explicitement écarté en étape 0). Le TGD (= F-FA-TGD, SF-222-02).
