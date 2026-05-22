# F-254 — Cadrage cohérence écran (étape 0 bis)

**Date** : 2026-05-23
**Skill appliquée** : `ai-skills/screen-coherence-challenger.md`
**Périmètre cadré** : SF-254-01 (route `/demos` + grille + CTA depuis landing + SEO). SF-254-02 (filtres domaine/tag) hors-périmètre pour ce cadrage — différée jusqu'à ~15 vidéos.

---

## Verdict global

**GO**

Le placement, la séquence et l'état terminal sont nets. La feature ne touche pas un écran utilisateur (dossier / synthèse / outils décisionnels) — elle vit dans le funnel **public d'acquisition**, entre la landing et la conversion (essai gratuit / Calendly). Aucune surcharge cumulée sur un écran existant, aucun ping-pong, l'état terminal est explicite.

---

## Intention métier + comportement visible attendu (1-2 phrases)

Donner au prospect avocat une **page galerie publique `/demos`** scalable (URL propre, indexable SEO, partageable), accessible depuis la landing via un CTA `Voir toutes les démos →`. Comportement visible : clic CTA landing → atterrissage `/demos` → grille de cards vidéo (thumbnail + titre + sous-titre) → clic card → ouverture du player (modal ou lecture inline) → 2 CTAs de sortie (Essai gratuit / Calendly).

---

## Rappel verdict feature-coherence-challenger

**GO** — `docs/features/F-254/SF-254-00-coherence.md` (2026-05-22). Pas de trou amont/aval, la galerie complète la landing sans la remplacer (carousel reste tant que < 15 vidéos).

---

## Parcours écran réel du prospect avocat

Source : pratique acquisition standard LegalCase, signaux M-71 / M-72, retours démos Mengue / Renversez. **⚠ Parcours marketing public — l'avocat-prospect n'est pas l'avocat-utilisateur.** Toujours partir du contexte où le prospect arrive d'une source externe (LinkedIn, recherche Google « analyse dossier IA avocat », recommandation, lien partagé) :

1. **Source externe** — LinkedIn / Google / mail prospection / recommandation → clic vers `legalcase.fr` ou `legalcase.fr/#demos` ou (post-F-254) directement `legalcase.fr/demos`
2. **Atterrissage landing** `legalcase.fr` → hero (H1 « 92 outils décisionnels juridiques pré-remplis ») + pitch + capacités
3. **Scroll preuve produit** → section problème / solution
4. **Scroll preuve vidéo** → section `.video-showcase` (`#demos`) avec carousel 9 vidéos + player principal — F-158 SF-158-03
5. **Décision « je veux voir plus de démos »** → friction si > 9 vidéos / carousel chargé → besoin d'une page dédiée
6. **Page galerie `/demos`** (SF-254-01) → grille de toutes les démos avec thumbnail + titre + sous-titre, ordonnée par pertinence éditoriale
7. **Sélection d'une démo** → ouverture du player (modal YouTube `iframe`) sur la card cliquée
8. **Visionnage** → le prospect se projette dans son cabinet
9. **CTA de sortie** → 2 chemins explicites :
   - « Essayer LegalCase 14 jours » → `/onboarding` (F-19 trial)
   - « Prendre rendez-vous démo » → Calendly externe
10. **Retour landing OU partage URL** → le prospect peut copier `legalcase.fr/demos` et l'envoyer à un confrère
11. **État terminal A** — Inscription essai (F-19) → l'avocat-prospect devient avocat-utilisateur (sort du périmètre marketing)
12. **État terminal B** — RDV pris via Calendly → l'opérateur reprend le lead (sort du périmètre LegalCase frontend)
13. **État terminal C** — Quitte la page sans CTA → cookie GA4 enregistre le passage, sortie propre (pas de pop-up agressive, pas de dark pattern)

---

## État terminal du processus (explicite)

Trois sorties admises et qui clôturent le parcours :
- **A — Inscription essai gratuit** (CTA primaire) → `/onboarding`
- **B — Prise de RDV Calendly** (CTA secondaire) → URL externe Calendly opérateur
- **C — Quitte sans CTA** (sortie naturelle) → tracking GA4, pas de gating

**Pas d'état « dossier traité »** car l'avocat n'a pas encore de dossier. Le parcours s'achève à la conversion (ou non-conversion) commerciale. C'est conforme à la nature **marketing/acquisition** de la feature.

---

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours | Écran / zone LegalCase | Statut |
|---|---|---|
| 1. Source externe | Hors périmètre LegalCase (LinkedIn, Google, mail outreach) | — |
| 2. Atterrissage landing | `LandingComponent` (`/`) — section `app-landing > header` + hero | ✅ existant (F-158 V3) |
| 3. Scroll preuve produit | `landing.component.html` sections problème + solution + tools-showcase | ✅ existant (F-158 SF-158-02) |
| 4. Scroll preuve vidéo (carousel) | `.video-showcase` section avec carousel 9 vidéos | ✅ existant (F-158 SF-158-03) + ancre `#demos` posée 2026-05-22 |
| 4 bis. CTA `Voir toutes les démos →` sous le carousel | **❌ manquant — à ajouter en SF-254-01** | Point d'entrée vers `/demos` |
| 5. Friction « je veux voir plus » | — | Pas d'écran : tension à résoudre par SF-254-01 |
| 6. Page galerie `/demos` | **❌ manquant — la feature SF-254-01** | Nouvelle route Angular standalone |
| 7. Sélection démo + player | Player inline (carousel) sur `LandingComponent` | ✅ existant pour landing, à dupliquer/réutiliser pour `/demos` (composant `app-landing-tools-showcase` n'est PAS le bon réfèrent — `.video-showcase` + `selectedVideoId()` l'est) |
| 8. Visionnage | Embedded YouTube `iframe` via `videoEmbedUrl()` | ✅ existant (`landing.component.ts`) |
| 9. CTA Essai gratuit | Bouton `/onboarding` ou Sign-In Google/Microsoft | ✅ existant (F-01 / F-02) |
| 9 bis. CTA Calendly | Lien externe Calendly | ✅ existant (URL configurée côté opérateur) |
| 10. Retour / partage URL | URL canonique de la page = identifiant partageable | ❌ aujourd'hui : seule `legalcase.fr/#demos` existe (ancre) → SF-254-01 introduit `/demos` |
| 11/12/13. États terminaux A/B/C | `/onboarding` (F-19) / Calendly externe / sortie navigateur | ✅ existants |
| Lien footer landing « Voir toutes les démos » | ❌ manquant — à ajouter en SF-254-01 | Point d'entrée alternatif |

**Trou identifié** : pas d'écran adapté entre l'étape 4 (carousel landing à ~9 vidéos) et l'étape 9 (CTA conversion) une fois le volume vidéo > 15. SF-254-01 vient exactement combler ce trou.

---

## Position candidate de la feature (écran, zone, points d'entrée)

**Écran** : nouvelle route Angular standalone `/demos`, **publique** (no-auth, no-guard), full-page.

**Zone du nouvel écran** :
- **Hero court** : titre `Démonstrations LegalCase`, sous-titre `Voyez l'IA en action sur 9 cas concrets`, badge nombre de démos (compteur dynamique sur le tableau de vidéos).
- **Grille de cards** : auto-fill responsive (3 par ligne desktop / 2 tablette / 1 mobile), thumbnail YouTube (`maxresdefault.jpg`), titre vidéo, sous-titre vidéo, icône play centrée au survol.
- **Player modal** : clic sur card ouvre un overlay plein écran (ou ~80% viewport) avec `<iframe>` YouTube + bouton fermer. Pas d'auto-play sur la grille (invariant anti-gadget).
- **Section CTA bas de page** : 2 boutons (`Essai gratuit 14 jours` primaire + `Prendre rendez-vous` secondaire), copy court (pattern landing F-158).
- **Footer** : lien retour vers landing + mentions légales (pattern existant).

**Points d'entrée vers `/demos`** :
1. **CTA `Voir toutes les démos →`** ajouté sous le carousel landing (`.video-carousel` après les arrows) — point d'entrée principal.
2. **Lien dans le footer landing** — point d'entrée secondaire pour les prospects qui sautent directement au footer.
3. **URL canonique partageable** — `https://legalcase.fr/demos` (lien sortant mail / LinkedIn / outreach).
4. **Sitemap + meta SEO** — indexation Google pour les recherches « démo logiciel avocat IA », « LegalCase démonstration », etc.

**Points de sortie depuis `/demos`** :
- CTA primaire « Essai gratuit » → `/onboarding`
- CTA secondaire « Prendre rendez-vous » → URL Calendly externe
- Lien header / logo → retour `/` landing
- Navigateur back → source d'arrivée

---

## Challenge placement

**Question** : *« L'écran `/demos` est-il bien à l'endroit où le prospect a besoin de plus de démos ? »*

**Réponse** : ✅ OUI. Le prospect est en mode « je veux voir le produit fonctionner sur un cas proche du mien » à l'étape 5. La landing actuelle l'arrête à 9 vidéos dans un carousel — pas de filtre, pas de navigation libre. `/demos` est l'écran exact qui sert ce besoin.

**Alternative envisagée et rejetée** : étendre la section `.video-showcase` de la landing en accordéon ou en grille deplyable. Rejeté parce que (a) ça allonge la landing déjà longue, (b) ça ne donne pas d'URL propre partageable, (c) ça ne s'indexe pas séparément en SEO.

---

## Challenge lisibilité de la séquence

**Question** : *« L'UI rend-elle visible que `/demos` se place entre la landing et la conversion ? »*

**Réponse** : ✅ OUI, mais avec un ajustement.
- Le CTA sous le carousel landing (`Voir toutes les démos →`) rend explicite « il y en a plus à voir, ça suit ».
- La page `/demos` doit afficher en bas les 2 CTAs de conversion (Essai gratuit / Calendly) — signale clairement « tu as vu les démos, voici la suite ».
- **Ajustement requis** : afficher dans le header de `/demos` un fil d'Ariane minimal (« LegalCase / Démonstrations ») ou un bouton retour landing, pour que le prospect comprenne où il se trouve et puisse revenir à l'expérience landing complète.

---

## Challenge charge écran

**Question** : *« La landing reste-t-elle lisible après ajout du CTA, et `/demos` tient-il la charge ? »*

**Réponse** :
- **Landing** : ajout d'un seul CTA `Voir toutes les démos →` sous le carousel — charge négligeable, +1 ligne, pas de bloc primaire ajouté. ✅
- **Page `/demos`** : nouvel écran, charge initiale **3 blocs primaires** (hero + grille + CTA bas). À 9 vidéos = 3 lignes desktop = page courte (~1.5 écran). À 20 vidéos = 7 lignes = page longue (~3 écrans) — toujours lisible, c'est un listing. ✅
- **Si > 30 vidéos un jour** : le seuil de lisibilité est franchi → ajouter pagination ou virtual scroll en SF future. Pas un sujet V1.

---

## Challenge état final / continuité

**Question** : *« Après l'output de la feature, que fait le prospect ? »*

**Réponse** : ✅ Trois sorties explicites (A inscription / B Calendly / C quitte). Pas de ping-pong subi entre landing et `/demos` — le prospect navigue à son rythme, et les 2 CTAs en bas de page sont des points de sortie *décidés*, pas des dead-ends.

**Vérification anti-ping-pong** : si le prospect clique « Retour » depuis `/demos`, il revient sur la landing à la position du CTA (scroll ancré `#demos` automatique si l'URL le porte) — pas en haut de page. Cet ajustement va dans la mini-spec.

---

## Ajustements IA requis (à intégrer en mini-spec SF-254-01)

1. **CTA `Voir toutes les démos →`** ajouté sous le carousel `.video-showcase` de la landing (après les flèches) — pas dans le header, pas en bas de page, exactement adjacent au carousel pour respecter la continuité du regard.
2. **Fil d'Ariane minimal** sur `/demos` (« LegalCase / Démonstrations ») ou bouton retour vers landing (`/#demos` pour revenir au carousel).
3. **2 CTAs de sortie en bas de `/demos`** : Essai gratuit (primaire) + Calendly (secondaire) — pas plus, pas moins ; pas de gating, pas de mailchimp newsletter.
4. **Pas d'auto-play sur grille** — clic explicite ouvre le player. Une seule vidéo en lecture à la fois.
5. **URL canonique** `https://legalcase.fr/demos` + `<link rel="canonical">` + meta OG + Twitter Card + sitemap.xml + JSON-LD `WebPage` (pattern SF-158-03 SEO meta V3).
6. **Single source of truth** : `DEMO_VIDEOS` actuellement dans `landing.component.ts` est promu en service partagé (`DemoVideosService` ou `demo-videos.data.ts` consommé par les 2 composants). Pas de duplication de la liste.
7. **Pas de marques tierces visibles** dans les titres/sous-titres ([[feedback_no_thirdparty_brands_landing]]).
8. **Pas de « l'IA fait X »** dans les titres ([[feedback_no_ai_word_in_emails]] appliqué au copy public).
9. **Page accessible** : sémantique HTML (`<main>`, `<h1>`, `<h2>`), labels ARIA sur les cards (`aria-label="Lire : <titre>"`), focus visible sur cards et CTAs, navigation clavier.
10. **Responsive** : grille `auto-fill, minmax(280px, 1fr)` ou breakpoints explicites (desktop 3 / tablette 2 / mobile 1). Player modal s'adapte (100% mobile, ~80vw desktop).

---

## Invariants anti-surcharge pour la mini-spec

1. **Une seule grille, pas de filtre** en SF-254-01. Les filtres SF-254-02 sont différés jusqu'à ~15 vidéos (sinon filtre vide).
2. **Pas d'auto-play sur la grille** (perf — N iframes YouTube auto-play tuent la page).
3. **Pas plus de 2 CTAs de sortie** sur la page (Essai gratuit + Calendly) — la simplicité est un attribut de conversion.
4. **Pas de scroll infini ni de pagination en V1** (9-15 vidéos = visibles en 2-3 lignes, pagination = sur-ingénierie).
5. **Pas de modale newsletter / gating popup** (dark pattern, casse la conversion).
6. **Pas d'auth requise** (page publique stricte) — pas de redirect login, pas de check workspace.
7. **Single source of truth des vidéos** — landing carousel et `/demos` lisent la même liste partagée (toute nouvelle vidéo s'ajoute à un endroit unique).
8. **Pas de PII dans les vidéos** — vidéos = cas factices ou anonymisés (cohérence RGPD F-240, déjà acquis sur les 9 vidéos existantes).
9. **Bouton retour landing** explicite (fil d'Ariane ou bouton « Retour » dans le header) — l'aller-retour landing ↔ galerie doit être un choix de design, pas une fuite.
10. **Build SSG-safe** — la page doit être pré-rendue par Angular SSG (Angular 19) sans regression hydration ([[project_angular_ssg_event_replay]] — handlers `(click)` peuvent être perdus, parade = toggle CSS ou wiring résistant à l'hydration).

---

## Décision finale

**GO** — la mini-spec SF-254-01 part avec :
- Le placement validé (nouvelle route `/demos`, CTA sous carousel landing, lien footer).
- La séquence visible (CTA → galerie → CTAs de sortie → conversion).
- L'état terminal explicite (A inscription / B Calendly / C sortie tracée GA4).
- Les 10 invariants anti-surcharge.

Pas de pré-requis bloquant. SF-254-02 (filtres) reste différée jusqu'à signal volume.

---

## MAJ apportée au parcours écran de référence

**Nouveau référentiel à créer** : `docs/business/parcours-ecran-prospect-acquisition.md` (n'existe pas — la skill construit incrémentalement le référentiel).

**Contenu à y consigner** :
- Parcours écran prospect avocat de la source externe → conversion (étapes 1 → 13 ci-dessus).
- État terminal du processus prospect : 3 sorties admises (A/B/C).
- Cartographie écrans LegalCase publics (landing F-158, `/demos` F-254 SF-254-01, `/onboarding` F-02, `/login`, pages légales F-74, blog F-120, `/unsubscribe` F-248).
- Invariants : pas d'auth requise sur les pages d'acquisition, pas de dark pattern, pas de marques tierces, single source of truth des vidéos.

Le référentiel sera créé dans la même PR que SF-254-01 (commit séparé ou même commit).

---

## Liens

- F-254 cohérence fonctionnelle — `docs/features/F-254/SF-254-00-coherence.md` (verdict GO 2026-05-22)
- F-158 SF-158-03 SEO meta V3 — pattern à reprendre pour `/demos`
- F-02 Onboarding — destination CTA primaire (Essai gratuit)
- DESIGN_SYSTEM.md — palette navy/or, typo Inter / JetBrains Mono
- [[project_angular_ssg_event_replay]] — bug Angular 19 SSG+@if, parade toggle CSS
- [[feedback_no_thirdparty_brands_landing]] — pas de marques tierces dans le copy public
- [[feedback_no_ai_word_in_emails]] — minimiser « IA » dans la com sortante
