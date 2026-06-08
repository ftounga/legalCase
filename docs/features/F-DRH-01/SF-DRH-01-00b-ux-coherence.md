# F-DRH-01 — Cadrage cohérence écran (étape 0 bis)

> Skill `screen-coherence-challenger`. Date 2026-06-08.
> Pré-requis : étape 0 (`SF-DRH-01-00-coherence.md`) rendue **GO avec ajustements**. Feature inscrite au `PRODUCT_SPEC.md` (F-DRH-01).
> Particularité : il ne s'agit **pas** d'un écran applicatif inséré dans le parcours avocat connecté, mais d'une **page publique marketing** (`/employeur`) à côté de `/`, `/demos`, `/blog`. Le « parcours » à challenger est donc le **parcours prospect DRH non connecté** (atterrissage → compréhension de la valeur → prise de RDV), pas le parcours dossier de l'avocat.

## Verdict : **GO avec ajustements**

Le placement est trivialement sain (nouvelle route publique autonome, aucune surcharge d'un écran existant). Les ajustements ne portent pas sur l'emplacement mais sur la **séquence interne de la page** et sur **l'état terminal** (le prospect doit toujours pouvoir atteindre le CTA démo sans cul-de-sac).

## Intention métier + comportement visible attendu (1-2 phrases)

Un DRH/Directeur des affaires sociales arrive sur `/employeur` (lien partagé, signature mail, outreach), comprend en quelques sections que LegalCase chiffre l'exposition prud'homale d'un licenciement et sécurise la procédure **à partir des pièces du dossier**, est rassuré sur la conformité/hébergement, puis **réserve une démo** (Calendly). Comportement visible : une page publique défilante, hero + valeur + confiance + CTA, sans login ni prix.

## Rappel verdict feature-coherence-challenger (étape 0)

**GO avec ajustements** : la page s'appuie sur une capacité réelle et livrée (moteur de chiffrage / détection de vices / délais), à condition de ne présenter que le réel, d'orienter vers une démo (pas de self-serve), sans prix, et avec un messaging conformité (jamais « gagner contre vos salariés »).

## Parcours écran réel du prospect DRH (atterrissage → état terminal)

Source : pratique B2B SaaS standard + pages publiques existantes du produit (`/`, `/demos`) — ⚠ hypothèse à valider au premier signal terrain DRH.

1. Le DRH reçoit/clique un lien `/employeur` (signature mail, outreach M-79, bouche-à-oreille).
2. **Hero** : il lit en < 5 s la promesse (« Chiffrez l'exposition prud'homale d'un licenciement — avant de décider ») et voit immédiatement le CTA primaire « Réserver une démo ».
3. S'il n'est pas convaincu au hero, il **scrolle** vers la section problème/valeur.
4. **Section problème/valeur** : il reconnaît sa douleur (décider à l'aveugle, avocat lent/cher) et comprend ce que l'outil fait *réellement* (qualifier, détecter les vices, chiffrer Macron, signaler les délais).
5. **Section confiance** : il vérifie hébergement EU / RGPD / isolation des données (réflexe DPO/procurement) et lit le cadre « maîtrise du risque & conformité ».
6. **CTA final** : il réserve une démo (Calendly) — OU remonte au CTA hero.
7. **État terminal** : ouverture de la page Calendly dans un nouvel onglet (RDV pris). C'est la sortie assumée de la page.

## État terminal du processus (explicite)

**État terminal = clic sur « Réserver une démo » → ouverture Calendly (`https://calendly.com/tounga-franck-ng-itconsulting/30min`) dans un nouvel onglet.** Il n'existe **aucun** état terminal « inscription » / « espace DRH » : le produit DRH-natif n'est pas livré (invariant étape 0). Tout autre lien (footer, retour accueil) est secondaire et ne doit pas concurrencer visuellement le CTA démo.

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours | Écran / zone LegalCase | Statut |
|---|---|---|
| Atterrissage lien | route publique `/employeur` (nouvelle) | ❌ à créer (objet de cette feature) |
| Hero + promesse | hero de la page `/employeur` | ❌ à créer |
| Problème/valeur | section de la page `/employeur` | ❌ à créer |
| Confiance/conformité | section de la page `/employeur` | ✅ contenu réel (hébergement EU/RGPD déjà revendiqués sur `/` et FAQ confidentialité) |
| CTA démo | lien Calendly (déjà utilisé sur `/demos`) | ✅ existant (réutilisé) |
| Retour produit avocat | footer → `/`, `/demos`, légal | ✅ existant |

## Position candidate de la feature (écran, zone, points d'entrée)

- **Écran** : nouvelle page publique standalone `/employeur` (lazy `loadComponent`), à côté de `/demos` et `/blog`. Aucune insertion dans le shell authentifié.
- **Points d'entrée** : liens externes (signatures mail, outreach), partage direct de l'URL. **Pas** de lien depuis la navigation produit avocat en V1 (éviter de mélanger les audiences avocat/employeur dans la même navigation — invariant neutralité D8).

## Challenge placement

Page autonome → **aucun risque de surcharge** d'un écran existant. Le placement est correct : le DRH a besoin de toute l'information au même endroit, au moment où il découvre l'offre. Aucune zone applicative n'est touchée.

## Challenge lisibilité de la séquence

La séquence verticale doit être **monotone descendante** : hero (promesse + CTA) → valeur (le réel) → confiance → CTA final. Ajustement : le **CTA « Réserver une démo » doit apparaître au moins deux fois** (hero + fin) pour qu'un prospect convaincu tôt n'ait pas à scroller jusqu'en bas, et qu'un prospect convaincu tard ait un CTA sous les yeux. La section valeur doit **précéder** la section confiance (on convainc de l'utilité avant de rassurer sur la conformité).

## Challenge charge écran

Page neuve, densité maîtrisée : **4 blocs primaires** (hero, valeur, confiance, CTA final). En dessous du seuil de surcharge. Invariant : ne **pas** ajouter de blocs gadget (témoignages fictifs, logos clients inexistants, captures d'écran d'écrans DRH-natifs non livrés). La section valeur liste **4 capacités réelles maximum** (qualifier, détecter les vices, chiffrer Macron, signaler les délais).

## Challenge état final / continuité

L'état terminal (Calendly) est explicite et atteignable depuis deux points (hero + fin). **Pas de dead-end** : si le prospect ne convertit pas, le footer offre `/`, `/demos`, mentions légales — retours assumés, non concurrents du CTA. Aucun ping-pong subi (la page ne renvoie pas vers un écran qui re-renvoie vers elle).

## Ajustements IA requis (à intégrer dans la mini-spec)

1. CTA « Réserver une démo » présent **au hero ET en fin de page** (même URL Calendly `/30min`).
2. Ordre imposé : **hero → valeur → confiance → CTA final** (valeur avant confiance).
3. Section valeur limitée à **4 capacités réelles** (pas de promesse d'écran DRH-natif : pas de « tableau de bord portefeuille »).
4. CTA Calendly = ancre `href` externe `target="_blank" rel="noopener noreferrer"` (pattern `/demos`), **jamais** un `routerLink` vers `/login` ou une inscription.

## Invariants anti-surcharge pour la mini-spec

- **Max 4 blocs primaires** sur la page (hero, valeur, confiance, CTA final) ; tout bloc supplémentaire requiert un nouveau cadrage.
- **Tout output a une sortie explicite** : la page mène toujours au CTA démo (état terminal unique et assumé).
- **Aucune section ne présente une capacité non livrée** (pas de capture/mention d'écran DRH-natif).
- **Aucun prix, aucune inscription self-serve** sur la page (cohérence étape 0).
- Classes CSS **préfixées** (`emp-*`) — la page vit dans le même bundle global que la landing (`ViewEncapsulation.None`), risque de collision de noms génériques (`.hero`, `.section`) — cf. retour SF-249-04.

## Décision finale

**GO avec ajustements.** Nouvelle page publique `/employeur`, 4 blocs primaires, double CTA Calendly, séquence valeur→confiance, aucune capacité fictive, aucun prix, classes préfixées. Passage à l'étape 1 (mini-spec) autorisé.

## MAJ apportée au parcours écran de référence

Nouveau parcours **prospect non connecté** (audience employeur/DRH) : distinct des 3 référentiels existants (`parcours-ecran-dossier`, `-cabinet`, `-super-admin`) qui couvrent l'avocat connecté. À ce stade le parcours est interne à la page (atterrissage → CTA Calendly) ; un référentiel `docs/business/parcours-ecran-acquisition.md` pourra être créé si d'autres pages publiques d'acquisition apparaissent (éviter de créer un référentiel pour une page unique).
