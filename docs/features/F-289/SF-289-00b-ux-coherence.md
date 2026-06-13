# F-289 — Cadrage cohérence écran (étape 0 bis)

> Feature : **Vue d'ensemble du dossier**. Étape 0 : `SF-289-00-coherence.md` (GO).
> Skill : `ai-skills/screen-coherence-challenger.md`. Date : 2026-06-13.
> Enjeu central : on ajoute un **nouvel onglet à fort contenu** en tête du détail dossier. Le risque n°1 n'est pas la donnée (elle existe), c'est la **surcharge visuelle**. Ce document tranche le placement, la séquence de lecture et la charge.
> **Statut git : BROUILLON hors repo.**

## Verdict : **GO avec ajustements** — onglet en tête validé, charge maîtrisée par hiérarchie stricte en 3 registres + repli du fil.

---

## Parcours écran réel de l'avocat (de l'ouverture au départ)

1. Clique sur un dossier dans la liste → **arrive sur le détail dossier**.
2. Aujourd'hui : atterrit sur l'onglet **Dossier** (intake + identité + pièces + documents) → doit ensuite aller chercher l'état procédural ailleurs.
3. Cible F-289 : atterrit sur **Vue d'ensemble** (nouvel onglet 0, actif par défaut) → voit immédiatement état + to-do + fil.
4. Agit depuis la vue (ouvre une pièce, génère une réplique, marque une échéance) **ou** plonge dans un onglet de travail via un raccourci.
5. Repart.

---

## Cartographie des écrans / zones existants sur ce parcours

| Onglet | Charge actuelle | Impact de F-289 |
|---|---|---|
| **(nouveau) Vue d'ensemble** | — | onglet 0, actif par défaut |
| Dossier | intake, identité, stade, stats, vague de pièces, documents | inchangé (n'est plus la 1ʳᵉ chose vue) |
| Analyse | pipeline, synthèse, questions | inchangé |
| Décision | outils, stratégie, dashboard | inchangé |
| Suivi | échéancier, phases, rounds, deadlines, notes | inchangé (les frises restent) |
| Stepper (sticky) | parcours outil | inchangé en V1 (rôles clarifiés) |

**Décision de placement : nouvel onglet en tête, actif par défaut.** Justification : la 1ʳᵉ question de l'avocat (« où en étais-je ? ») doit recevoir sa réponse sans clic. On passe de 4 à 5 onglets — acceptable (libellés courts, pas de scroll horizontal d'onglets sur desktop cible).

---

## Challenge anti-surcharge

### Risque : le « tout réunir » recrée une mosaïque illisible
**Parade : hiérarchie stricte en 3 registres, du plus actionnable au plus profond, jamais en vrac.**

1. **PILOTER** (haut, dense, court) : bandeau d'état (1-2 lignes) + bloc « ce qui requiert ton attention » (≤ 5 lignes priorisées, repli si plus).
2. **PARCOURIR** (centre, cœur visuel) : le fil vertical, repère Aujourd'hui, **passé replié par défaut au-delà de N=8 événements** (« voir l'historique complet »), futur toujours visible.
3. **APPROFONDIR / AGIR** (bas, discret) : barre de raccourcis + export.

### Garde-fous de charge (invariants pour la mini-spec)
- **Bloc attention plafonné** à 5 items visibles, triés par urgence ; au-delà → « +N autres ».
- **Fil : regroupement** (une vague = 1 ligne « +3 pièces » dépliable, pas 3 ; versions de conclusions condensées) + **repli du passé lointain**.
- **Filtres par voie** (chips Tout/Procédure/Échanges/Pièces/Production) pour réduire le bruit à la demande — **défaut = Tout**, jamais un filtre masquant par surprise.
- **Pièces en accordéon fermé par défaut** : l'événement montre son titre ; on déplie pour voir les pièces (pas d'avalanche de fichiers ouverte d'emblée).
- **Actions inline limitées** aux gestes naturels sur le fil (ajouter échange/phase/échéance, marquer échéance faite, générer réplique, ouvrir pièce). Les actions « lourdes » (répondre à une question IA, arbitrer un risque) **routent** vers l'écran dédié → pas de formulaires lourds dans la vue.
- **Densité typographique** conforme `DESIGN_SYSTEM.md` (Merriweather titres, Inter corps, JetBrains Mono dates, espacement 4px) ; réutiliser le vocabulaire visuel des frises existantes (contradictoire/phases) pour que le fil paraisse leur « frise mère ».

### Lisibilité de la séquence
La lecture descend naturellement : *état → ce qui presse → l'histoire → approfondir*. Le repère **Aujourd'hui** (ligne dorée) ancre l'œil au centre et sépare réalisé (traits pleins) / à-venir (pointillés).

### État final / continuité
Chaque sortie de la vue est un **routage assumé** (onglet + ancre, mécanisme `selectedTabIndex` + scroll existant) ou l'**ouverture d'une pièce** (preview). Aucun cul-de-sac, aucun rechargement brutal.

---

## Invariants anti-surcharge (que la mini-spec DEVRA respecter)

1. **3 registres hiérarchisés**, jamais une grille plate de cartes équivalentes.
2. **Bloc attention ≤ 5 items**, le reste replié.
3. **Fil : regroupement + repli du passé > 8 événements** ; pièces en accordéon fermé.
4. **Filtres optionnels, défaut = Tout** (aucun masquage implicite).
5. **Actions inline = gestes naturels seulement** ; le lourd route.
6. **Design system strict** + cohérence visuelle avec les frises existantes.
7. **Repère Aujourd'hui** obligatoire (sépare passé/futur).

---

## Enrichissement du référentiel

À répercuter dans `docs/business/parcours-ecran-dossier.md` lors du passage en repo : ajout de l'onglet **Vue d'ensemble** en tête de parcours, avec son rôle (poste de pilotage / journal) distinct de la Synthèse (fond) et du Stepper (parcours outil).

## Décision finale

**GO avec ajustements.** Onglet en tête validé. La surcharge est le seul vrai risque et elle est maîtrisée par les 7 invariants ci-dessus. → mini-spec SF-289-01.
