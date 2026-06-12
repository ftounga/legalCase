# SF-284-00b — Cadrage cohérence écran (étape 0 bis)

> Feature : **F-284 — Échéancier procédural proactif & alertes**
> Skill : `ai-skills/screen-coherence-challenger.md`
> Date : 2026-06-12 · Verdict : **GO avec ajustements**

## 1. Parcours écran réel de l'avocat

Ouverture du dossier → barre d'onglets `Dossier · Analyse · Conclusions · Suivi`. L'onglet
**Suivi** est l'onglet de **pilotage du cycle de vie**. Après F-282/F-283, son ordre vertical est :

1. `app-case-phases-timeline` (frise des phases — F-283) — *où en est le dossier dans la procédure*
2. `app-contradictoire-timeline` (frise des échanges — F-282) — *qui doit jouer le prochain coup*
3. `app-case-deadlines-section` (liste des délais — F-69) — *liste passive, CRUD, repliée par défaut*
4. `app-case-notes-section` (notes)

La question « **quel est le prochain couperet, dans combien de jours ?** » n'a aujourd'hui **aucune
réponse en tête d'écran** : elle est noyée dans la liste F-69 (3ᵉ position, repliée). L'avocat doit
déplier et scanner. C'est précisément le geste de pilotage le plus fréquent.

## 2. Cartographie des écrans / zones existants

| Zone | Rôle | Charge |
|---|---|---|
| Tête onglet Suivi | aujourd'hui : frise phases (F-283) | moyenne |
| `case-deadlines-section` | liste éditable repliée | faible (replié) |
| Dashboard cabinet | délais urgents tous dossiers | hors dossier |

## 3. Challenge placement / lisibilité / charge / état final

**Placement** : l'échéancier proactif doit être **la toute première carte de l'onglet Suivi**,
au-dessus de la frise des phases. Justification : l'urgence temporelle prime sur la position
procédurale dans la hantise de l'avocat ; c'est le « tableau de bord » du dossier. La frise des
phases répond à « où en suis-je », l'échéancier à « qu'est-ce qui me menace » — ce dernier est
consulté plus souvent et plus vite. Mettre l'échéancier en tête respecte l'ordre d'attention.

**Lisibilité de la séquence** : échéancier (menace) → phases (position) → contradictoire (tour de
jeu) → délais détaillés (édition) → notes. Du plus actionnable/urgent au plus détaillé. Cohérent.

**Charge de l'écran cible** : risque de surcharge si l'échéancier réaffiche toute la liste F-69.
**Ajustement imposé** : l'échéancier ne montre que le **hero (prochain couperet)** + les **3
prochaines échéances** ; le détail complet reste dans `case-deadlines-section` (lien « Voir tous
les délais » qui déplie / scrolle). Pas de doublon visuel intégral.

**État final / continuité** : état vide soigné (aucun délai → invitation à en ajouter, renvoi à la
section F-69). État de chargement (skeleton/texte). État « tout est sous contrôle » (prochain délai
> 30 j → ton apaisé navy, pas d'alarme rouge gratuite).

## 4. Invariants anti-surcharge (pour la mini-spec)

- **UX-1** : carte en **colonne document** `max-width: 760px` (gabarit F-282), pas d'étirement plein
  écran. Charte navy/or, Merriweather/Inter/JetBrains Mono, espacements ×4px.
- **UX-2** : **hero unique** = le prochain couperet (gros compte à rebours J-X + libellé + date
  mono). Couleur sémantique : rouge (dépassé/≤7j), or (≤15j), navy (sous contrôle).
- **UX-3** : sous le hero, **3 échéances max** en liste compacte ; au-delà → lien vers la section
  détaillée F-69. Ne PAS recopier toute la liste.
- **UX-4** : placée **en 1ʳᵉ position** de l'onglet Suivi (avant la frise des phases).
- **UX-5** : états vide / chargement / sous-contrôle explicitement stylés (pas de carte nue).
- **UX-6** : champs date natifs `type="date" lang="fr-FR"` partout (convention projet) — ici lecture
  seule, donc pas de saisie dans ce composant.

## 5. Verdict

**GO avec ajustements.** Placement en tête de Suivi validé ; surcharge évitée par le triptyque
hero + 3 + lien-vers-détail (UX-3). Enrichit `docs/business/parcours-ecran-dossier.md`.
