# F-254 — Galerie de démos vidéo dédiée (Document de cadrage cohérence — étape 0)

**Date** : 2026-05-22
**Skill appliquée** : `ai-skills/feature-coherence-challenger.md`
**Origine** : signal opérateur 2026-05-22 — la section vidéo de la landing (`landing.component.html:65`, 9 vidéos déjà en place) va recevoir ~10 vidéos supplémentaires. Au-delà de ~15, le carousel horizontal devient inconfortable (pas de filtre par thème, mauvais SEO, thumbnails écrasés). Avant cette inflation : poser une ancre `#demos` (livré dans le même commit, **non SF-254** — pré-marche) et cadrer la galerie dédiée comme nouvelle feature.

---

## Verdict global

**GO**

F-254 est une feature **purement marketing/acquisition**, sans dépendance fonctionnelle amont autre que le matériel vidéo lui-même. Le carousel existant sur la landing (F-158 SF-158-03) reste en place tant que le volume est < 15 vidéos ; F-254 prépare la bascule vers une galerie dédiée scalable et SEO-friendly. Pas de trou amont, pas de trou aval.

---

## Intention métier (1 phrase)

Offrir au prospect avocat une **galerie publique navigable des démos vidéo LegalCase** (`/demos`), filtrable par domaine juridique (Travail / Immigration / Famille) et par cas d'usage (analyse de dossier / outils décisionnels / rédaction), afin de scaler au-delà du carousel landing dès que le volume dépasse ~15 vidéos et d'ouvrir une page indexable SEO par démo.

---

## Workflow métier réel — position de F-254

Source : workflow d'acquisition standard d'un prospect avocat LegalCase (`docs/marketing/`, signal terrain démos M-71). L'avocat-prospect n'est pas un avocat-utilisateur — son workflow est marketing :

1. Le prospect entend parler de LegalCase (LinkedIn, recommandation, recherche Google « analyse dossier IA avocat »)
2. Il atterrit sur `legalcase.fr` (landing F-158)
3. Il scanne le H1 / le pitch / les capacités (« 92 outils décisionnels pré-remplis »)
4. **Il cherche une preuve concrète** — il veut VOIR le produit fonctionner sur un cas proche de sa pratique
5. Il regarde 1 à 3 vidéos qui correspondent à son domaine (Travail / Immigration / Famille / autre)   ⬅ **F-254** (page galerie) **OU** carousel landing F-158 SF-158-03
6. Il se projette mentalement dans son cabinet (« est-ce que ça résout mon cas Mengue / Renversez / etc. »)
7. Il prend rendez-vous démo Calendly OU s'inscrit en essai gratuit OU partage le lien à un confrère
8. Le confrère reçoit l'URL et doit pouvoir atterrir directement sur la démo pertinente   ⬅ **F-254** (URL propre par démo, V2)

Le verrou de l'étape 4 → 5 → 7 est purement marketing : si la preuve vidéo est introuvable / pas filtrable / pas partageable, le prospect quitte la landing sans CTA.

---

## Cartographie features ↔ workflow (challenge amont/aval)

| Élément du workflow | Couvert ? | Analyse |
|---|---|---|
| Landing publique avec pitch | ✅ | F-158 livrée — V3 refondue 2026-05-04 |
| Carousel vidéos sur landing | ✅ | F-158 SF-158-03 livré — 9 vidéos YouTube, thumbnails `img.youtube.com` |
| Ancre `#demos` pour partager un lien direct vers la section vidéos | ✅ | **Livré dans le même commit** que ce cadrage (pré-marche F-254) — `landing.component.html:65` + `scroll-margin-top` |
| Matériel vidéo (10+ vidéos supplémentaires) | 🟡 | À produire en parallèle par l'opérateur — indépendant du dev F-254 |
| **Galerie dédiée `/demos`** | — | **F-254 (la feature challengée)** |
| Calendly + essai gratuit en sortie de galerie | ✅ | F-19 (Stripe + trial), prise de rdv Calendly opérationnelle |
| Footer landing avec liens d'amorçage | ✅ | Existe — ajout d'un lien « Voir toutes les démos » dans le périmètre F-254 |
| URL propre par démo (SEO indexable) | — | **SF-254-03 V2** — conditionné à signal terrain ou volume |

**Challenge amont** : aucun trou. La landing existe, le carousel existe, l'ancre existe, le CTA essai gratuit existe, Calendly est en place. Le seul prérequis non technique est la production des vidéos — orthogonal au dev F-254 (la page peut être livrée avec les 9 vidéos actuelles et croître ensuite).

**Challenge aval** : la sortie de la galerie est exploitable :
- Le prospect clique « Essai gratuit » → onboarding F-02 livré
- Le prospect clique « Prendre rendez-vous » → lien Calendly externe (pas de dev)
- Le prospect partage l'URL → V2 (SF-254-03) requise pour permettre un partage par démo individuelle

---

## Position de la nouvelle feature dans le PRODUCT_SPEC

À inscrire dans `### UX & exploitation` (section où F-158 / F-162 / F-110 sont logés). Marketing/landing est traité comme UX exploitation produit.

---

## STOPs / pré-requis à ajouter au backlog

**Aucun pré-requis bloquant.** Cohérent avec le verdict GO.

Conditions de cohérence à respecter en mini-spec :
1. Ne pas supprimer la section vidéo de la landing tant que < 15 vidéos (preuve sociale immédiate).
2. Le catalogue de vidéos est **dérivé de la même source** que le carousel landing (`landing.component.ts` `DEMO_VIDEOS`) — pas de double saisie / pas de divergence possible. À refactoriser en service partagé en SF-254-01.
3. Les thumbnails restent YouTube (`img.youtube.com/vi/{ID}/maxresdefault.jpg`) — pas de stockage S3, pas de download asset (déjà acté SF-158-03).

---

## Invariants anti-gadget pour les mini-specs

1. **Pas de player auto-play** sur le listing (perf — N iframes YouTube auto-play tue la page) ; un seul player s'ouvre au clic (lightbox ou route `/demos/:slug` V2).
2. **Pas de marques tierces visibles** ([[feedback_no_thirdparty_brands_landing]]) — pas de mention « Claude / Anthropic / OpenAI / Textract » dans les titres/descriptions ; conserver la nomenclature « Legal Vision / Legal OCR ». Exception RGPD : sous-processeurs cités uniquement dans la FAQ confidentialité.
3. **Pas de « l'IA fait X »** dans les titres/descriptions ([[feedback_no_ai_word_in_emails]] appliqué au copy public) — préférer « LegalCase pré-remplit / analyse / extrait ».
4. **Pré-requis design** : DESIGN_SYSTEM.md (navy / or / Inter / JetBrains Mono pour références d'articles si présents) — pas de palette nouvelle.
5. **SEO indexable** : `<title>`, `<meta description>`, balise `og:image`, lien canonique, ajout au sitemap (cf. SF-158-03 SEO meta V3 — appliquer le même pattern).
6. **Single source of truth des vidéos** : un seul fichier de données partagé entre landing carousel et page `/demos` — toute nouvelle vidéo s'ajoute à un endroit unique.
7. **Pas de PII / pas de dossier client réel** dans les vidéos — vidéos = cas factices ou anonymisés (cohérence RGPD F-240).
8. **Filtres par domaine** : alignés sur les 3 domaines V1 (Travail / Immigration / Famille) — pas de catégorisation hors-domaine.
9. **État vide soigné** si filtre ne renvoie aucune vidéo (« Aucune démo pour cette combinaison — toutes les démos restent visibles via le filtre 'Tous' »).
10. **Pas d'auth requise** — page publique strict, pas de gate workspace / pas de redirection login.

---

## Découpage indicatif en SF (à figer en mini-spec)

| SF | Périmètre | Parallélisable |
|---|---|---|
| **SF-254-01 frontend** | Route `/demos` (composant standalone), hero, listing pleine largeur avec grille auto-fill, lien depuis le footer landing + CTA `Voir toutes les démos →` sous le carousel, SEO meta + sitemap. Refactor `DEMO_VIDEOS` en service partagé (consommé par landing + galerie). | — (purement frontend) |
| **SF-254-02 frontend** | Filtres `domaine` (Travail / Immigration / Famille) + `tag` (analyse-dossier / outil-décisionnel / rédaction), enrichissement du modèle `DemoVideo` avec champs `domain`, `tags`. Compteur résultats + état vide. | Parallélisable avec SF-254-01 si contrat de modèle figé en mini-spec. |
| **SF-254-03 frontend** *(V2, conditionnée)* | Page détail par démo `/demos/:slug`, indexable SEO, description longue, CTAs « Essai gratuit » + « Calendly », vidéos liées (3 autres du même domaine). | Démarrée seulement si signal terrain (partage par démo demandé) ou volume > 20 vidéos. |

Cycle obligatoire par SF (mini-spec → readiness → dev → review → push + release).

---

## Décision finale

**GO** — F-254 passe `Backlog` → `À faire`, démarrage possible dès que l'opérateur dispose du matériel vidéo ou décide de démarrer SF-254-01 avec les 9 vidéos actuelles (la page est non vide).

**Démarrage immédiat non recommandé** tant que la dynamique de production vidéo (10+ à venir) n'est pas confirmée — démarrer SF-254-01 quand on est à ~12-15 vidéos pour que la page ait du sens dès le J1. Entre-temps, l'ancre `#demos` (livrée maintenant) couvre 100 % du besoin « lien partageable vers les démos ».

---

## Pré-marche livrée dans le même commit (hors SF-254)

- Ancre HTML : `<section id="demos" class="video-showcase">` sur `landing.component.html`
- `scroll-margin-top: 84px` sur `.video-showcase` (header fixed 68px) — pas de masquage du titre lors du scroll
- URLs partageables : `https://legalcase.fr/#demos` et `https://legalcase.fr/?utm_source=...#demos`

Cette pré-marche ne consomme aucun budget de feature ; elle accélère immédiatement la prospection ([[feedback_marketing_prospection]]) sans préempter le design final de F-254.

---

## Liens

- F-158 SF-158-03 — section vidéo landing V3 (`docs/features/F-158/SF-158-03-seo-meta-v3.md`)
- DESIGN_SYSTEM.md — palette navy/or, typo Inter / JetBrains Mono
- [[feedback_no_thirdparty_brands_landing]] — pas de marques tierces dans le copy public
- [[feedback_no_ai_word_in_emails]] — minimiser « IA » dans la com sortante
- [[feedback_marketing_prospection]] — ne pas bloquer la prospection sur un produit parfait
