# Radar stratégique — extension Cameroun / OHADA-17

> **Statut** : registre vivant des signaux faibles. Pas une roadmap, pas une feature au PRODUCT_SPEC.md.
> Hors V1. Ne devient un sujet d'engagement de roadmap que **si les critères d'activation sont réunis**.

> **Périmètre** : ce fichier trace une **extension géographique** du produit LegalCase vers le Cameroun et plus largement les 17 États membres OHADA. Distinct de `docs/radar-corporate-b2b.md` (changement de cible buyer, scope sectoriel élargi) et `docs/radar-pivots-totaux.md` (abandon de LegalCase au profit d'un autre produit). La décision business model (lifestyle vs levée seed) est tracée dans `docs/radar-business-model.md` et reste orthogonale à ce radar.

## ⚠️ Garde-fou discipline — à relire avant chaque réflexion

3 questions critiques à garder en tête à chaque consultation :

1. **Pourquoi explorer une extension géographique alors que LegalCase n'a pas été validé auprès d'un seul client payant en FR/BE ?** Tracer une trajectoire ne dispense pas de prouver le PMF du produit actuel. Si la couverture FR+BE n'a pas produit 30 K€ MRR, le problème est commercial, pas géographique — l'ajouter le Cameroun n'aide pas, ça dilue.
2. **Risque émotionnel "pays d'origine"** : signaler explicitement la possibilité qu'attachement personnel au Cameroun biaise la lecture des signaux. Le founder l'a anticipé : *"Je ne veux pas juste me lancer parce que je suis Cameroun. Je veux vraiment être sûr."* Cette discipline doit être conservée à chaque réévaluation.
3. **Le seuil non-négociable** : aucune extension OHADA ne devient un sujet d'engagement de roadmap avant **30 K€ MRR atteint en bootstrap sur la cible FR+BE actuelle**. En dessous, ce radar reste un observatoire passif — pas une feuille de route.

**Tracer ≠ engager.** Ce fichier accumule des signaux pour décision future, il ne décide pas et ne justifie aucune dépense produit/marketing maintenant.

## Pourquoi ce document existe

Au cours de mai 2026, **5 signaux convergents** sont remontés de sources distinctes :

1. **2-3 avocats** (à préciser dans le registre des signaux) — intérêt direct exprimé
2. **1 collègue** (à préciser) — intérêt direct exprimé
3. **1 magistrat hors hiérarchie au Cameroun** (le père du founder) — endossement métier + ouverture réseau panafricain

Cette convergence dépasse le seuil "1 source isolée = bruit". Le radar trace les paramètres de la décision pour le moment où elle deviendra légitimement décidable.

L'objectif est d'**éviter deux pièges symétriques** : (1) ignorer un signal stratégique qui s'accumule ; (2) céder à l'attrait du pays d'origine et engager 6-12 mois de dev sur un marché que rien ne valide encore commercialement.

## Orientation de scope retenue 2026-05-28 (sous verrou)

> **Décision conditionnelle** : si activation un jour, le scope candidat retenu est **Option β — Nouveau domaine "droit des affaires OHADA" sur les 17 pays**, en mode "et en plus" de FR/BE (pas en remplacement).
>
> **Positionnement différenciant retenu** (étude concurrentielle 2026-05-28) :
> - **Dossier-centric** (analyse du dossier client + outils décisionnels métier) — créneau vide
> - **Pricing FCFA + Mobile Money + mensualisé** — créneau ignoré par tous les concurrents
> - **Freemium SEO + couverture vérifiable** — funnel d'acquisition organique
>
> Cible buyer = **avocat d'affaires** (cabinets internationaux Douala/Yaoundé/Abidjan/Dakar **+** avocats solo des grandes villes francophones), pas avocat généraliste.
>
> Effort estimé : **6-10 mois** full-time, ou **12-18 mois** en parallèle de FR/BE (mode β à 30 % du temps founder).
>
> **Verrou intact** : décision GO/STOP ne se prend qu'après 30 K€ MRR FR/BE atteint en bootstrap (cf. `radar-business-model.md`). Cette orientation pré-trace le scope **probable** d'activation, elle ne déclenche pas le dev.
>
> **Vrai concurrent à étudier** : **Lexbase Afrique** (21 pays + partenariat Conférence des Barreaux OHADA) — leader installé du paradigme recherche-centric. Étude approfondie obligatoire avant activation. Jurisprudence.cc, à l'inverse, est un acteur fragile (8 mois, bootstrap, zéro traction publique mesurable).

## Trajectoire candidate — résumé

| Aujourd'hui (V1) | Trajectoire candidate |
|---|---|
| Marché = **France + Belgique** (~80 000 avocats cumulés) | Marché = **17 États OHADA** (~12 000-18 000 avocats estimés, dont Cameroun ~3 000) |
| Droit civil unifié (FR) + civil law BE | **Bijuridisme** : droit civil OHADA (10 Actes uniformes harmonisés) + droits nationaux (CM, CI, SN, etc.) + **common law NWSW** au Cameroun |
| Buyer = **avocat indépendant / petit cabinet** | Buyer = avocat OHADA (mix avocat affaires + TPE généraliste) + cabinets internationaux à Douala/Yaoundé/Abidjan/Dakar |
| Pricing F-123 = 99-429 €/mois | Pricing cible mix : **25 000 XAF (38 €) plan SOLO** / **60 000 XAF (91 €) TEAM** / **150 €+ AFFAIRES** / **199-399 € INTERNATIONAL** |
| Paiement = Stripe CB EUR | **Bi-rail obligatoire** : CinetPay (MTN MoMo + Orange Money + carte XAF) + Stripe (cabinets internationaux EUR) |
| Hébergement = OVH France | Hébergement local **obligatoire** (loi n°2024/017 Cameroun, échéance 23/06/2026) → AWS Cape Town ou OVH Africa |
| ARR cible 12 mois = 200-300 K€ | ARR cible 36 mois OHADA = **130-450 K€** (scénarios base/bull) |

## Le piège OHADA — recadrage honnête 2026-05-28

> ⚠️ **Recadrage post-étude** : la première version de ce radar présentait OHADA comme un effet de levier "1 dev = 17 pays". C'est **partiellement faux** pour LegalCase tel qu'il existe aujourd'hui.

### Diagnostic réel

| Périmètre LegalCase V1 actuel | Statut harmonisation OHADA |
|---|---|
| Droit du travail | ❌ National (chaque pays son code) |
| Droit immigration / étrangers | ❌ National |
| Droit famille | ❌ National |

**Aucun des 3 domaines actuels de LegalCase n'est harmonisé OHADA.** Le levier "1 dev = 17 pays" ne s'applique **que** au droit des affaires (10 Actes uniformes), qui est un **4ème domaine que LegalCase ne fait pas du tout aujourd'hui**.

### Conséquence sur le scope candidat

Quatre options réalistes, et toutes plus lourdes que ce qui était initialement écrit :

| Option | Périmètre | Effort dev | Cellules | Verdict |
|---|---|---|---|---|
| **α — Cameroun seul, domaines V1** | Travail CM + Immigration CM + Famille CM | ~10-14 mois | 3 nouvelles | TAM ~3 000 avocats, ARR plafond 50-100 K€. **Pas rentable face à l'effort.** |
| **β — OHADA business law seul, 17 pays** | Droit des affaires OHADA (10 Actes uniformes) | ~6-10 mois | 1 nouveau domaine multi-pays | C'est un **nouveau métier** pour LegalCase (buyer = avocat d'affaires, pas avocat généraliste). Cible étroite (gros cabinets internationaux), concurrent Jurisprudence.cc déjà lancé. |
| **γ — OHADA business + Cameroun complet** | β + α | ~16-22 mois | 4 cellules (1 multi + 3 mono) | **Plus lourd que la V1 FR+BE complète** (12-15 mois). Engagement majeur. |
| **δ — Pivot domaine pur OHADA business** | β + abandon partiel V1 FR/BE | ~6-10 mois | 1 nouveau, on jette de la V1 | À tracer dans `radar-pivots-totaux.md`, pas ici. **Hors scope de ce radar.** |

### Pourquoi l'erreur initiale

J'ai cumulé deux mauvaises intuitions :
1. *"OHADA harmonise donc tout l'effort est mutualisé"* — vrai uniquement pour le droit des affaires, pas pour le travail/famille/immigration qui restent nationaux.
2. *"Les outils décisionnels sont à peine plus que des prompts avec un placeholder pays"* — faux. Chaque outil décisionnel = une situation métier (règle invariante `feedback_decision_tools_one_per_situation`). Les blocs de synthèse, les prompts, les seeds de visibilité, les tests d'intégrité sont domaine-spécifiques ET pays-spécifiques.

### Conclusion lucide

- **Option α (Cameroun seul)** : effort réaliste = équivalent V1 BE actuelle, ROI faible (TAM trop petit). À garder en option défensive si le radar s'active uniquement par signal CM fort.
- **Option β (OHADA business law 17 pays)** : c'est une thèse défendable mais c'est un **nouveau produit** pour LegalCase, avec une cible buyer différente (avocat d'affaires). Pourrait se faire en parallèle d'une V1 FR+BE devenue mature, **pas en remplacement**.
- **Option γ (OHADA business + Cameroun complet)** : ambition maximale, mais ~16-22 mois full-time = ne se justifie qu'après levée seed (cf. `radar-business-model.md` chemin 2).
- **Option δ** : pas dans le périmètre de ce radar (pivot total).

**Le levier "OHADA = mini-effort × 17 pays" n'est pas la réalité de LegalCase.** Le vrai levier OHADA serait : **basculer le produit vers le droit des affaires**, ce qui est un changement de buyer et de positionnement à part entière.

## Registre des 5 signaux observés

| # | Date | Source | Contenu | Interprétation | Poids |
|---|------|--------|---------|----------------|-------|
| 1 | 2026-05-XX (à préciser) | Avocat #1 (Cameroun ou diaspora) | À préciser — verbatim attendu | Signal direct cible avocat CM | À évaluer |
| 2 | 2026-05-XX (à préciser) | Avocat #2 | À préciser | Signal direct | À évaluer |
| 3 | 2026-05-XX (à préciser) | Avocat #3 (optionnel) | À préciser | Signal direct | À évaluer |
| 4 | 2026-05-XX (à préciser) | Collègue (contexte à préciser : ex-collègue cabinet, contact LinkedIn, etc.) | À préciser | Signal indirect (non-avocat) — pondération moindre | Faible-Moyen |
| 5 | 2026-05-XX (à préciser) | Père du founder, magistrat hors hiérarchie Cameroun | Endossement métier + ouverture réseau panafricain | Signal qualitatif fort sur faisabilité métier + accès Bâtonniers OHADA. **À pondérer** : signal familial → risque de biais positif | Fort (avec garde-fou biais affectif) |

> **À faire avant prochaine réévaluation** : compléter chaque ligne avec date exacte, source identifiée, formulation verbatim de l'intérêt exprimé. Distinguer source externe (avocat / collègue) vs intra-familial (père). Un signal sans verbatim ≠ signal documenté.

## Analyse marché — synthèse étude 2026-05-28

> Étude complète dans `docs/radar-cameroun-ohada-etude-marche-2026-05-28.md` (à archiver). Synthèse opérationnelle ci-dessous.

### Marché juridique

- **Cameroun** : ~2 800-3 200 avocats inscrits + ~1 500 stagiaires (extrapolation 2026 depuis chiffre officiel 2016 de 2 086 + croissance ~5 %/an). Barreau national unique siège Yaoundé. Fracture **francophone (~60-70 %) vs anglophone NWSW (~30 %)**, crise 2016-17 toujours latente.
- **OHADA-17** : Bénin, Burkina Faso, Cameroun, Centrafrique, Comores, Congo, Côte d'Ivoire, Gabon, Guinée, Guinée-Bissau, Guinée Équatoriale, Mali, Niger, RDC, Sénégal, Tchad, Togo. Cumul avocats estimé **12 000-18 000** (donnée agrégée non publiée — chiffres partiels : Côte d'Ivoire > 500, Sénégal 439, Cameroun ~3 000, RDC plusieurs milliers).
- **Outils en place** : cabinets internationaux à Douala/Yaoundé utilisent LexisNexis / Dalloz (abonnements EUR). TPE locales = Word + Google + WhatsApp. **Pas de SaaS payant identifié sur la masse.**

### OHADA — le multiplicateur

- **10 Actes uniformes en vigueur** : droit commercial général, sociétés commerciales/GIE, sûretés, procédures simplifiées de recouvrement et voies d'exécution, procédures collectives, arbitrage, comptabilité, contrats de transport, sociétés coopératives, médiation.
- **CCJA** = juridiction supranationale unique, 1 325 arrêts indexés. Une analyse couvre 17 pays.
- **Avantage IA** : corpus juridique unifié, jurisprudence concentrée, fragmentation marketing seulement par pays (pas par droit). C'est le rêve d'un moteur IA juridique.

### Paiement & infra

- **Stripe** : **non supporté pour comptes marchands Cameroun**. Stripe Tax couvre depuis 2025 mais ce n'est pas un acquiring. Workaround Stripe Atlas (US LLC) risqué.
- **CinetPay** = candidat le plus pragmatique (1,5-3,5 %/transaction, supporte MTN MoMo + Orange Money + cartes, intégration API mature, panafricain).
- **Flutterwave** = alternative (~3 %, présent Cameroun).
- **Mobile money** = 10 M+ wallets actifs au Cameroun (96 % couverture cellulaire, 41,9 % pénétration internet). **Abonnements récurrents en mobile money restent rares en B2B** — risque churn involontaire (échec de prélèvement).
- **Bi-rail obligatoire** : CinetPay XAF (masse) + Stripe EUR (cabinets internationaux).
- **Peg EUR/XAF fixe** depuis 1999 (1 EUR = 655,957 XAF), garanti Trésor français — **risque change quasi nul**. Argument fort pour pricing affiché en EUR.

### Cadre juridique

- **Loi n°2024/017 du 23/12/2024** (Cameroun, 38ème pays africain data protection, inspirée RGPD) — échéance mise en conformité **23/06/2026**. Conséquence : **hébergement local OU justification transfert + DPO obligatoire** pour traitement de données sensibles (dossiers judiciaires = catégorie particulière).
- Coût additionnel : audit local + DPO + migration partielle vers AWS Cape Town ou OVH Africa.
- Position publique du Barreau Cameroun sur l'IA juridique : **non trouvée**. Tendance régionale (jurisprudence.cc, Modulaw, Lawis) montre que les Ordres ne bloquent pas.

### Concurrence legaltech Afrique francophone — recadrage 2026-05-28 (étude approfondie)

**Acteurs établis recherche-centric (paradigme Doctrine/Lexis)** :

- **Lexbase Afrique** — **LE leader installé OHADA**. 21 pays + 6 organisations régionales. **Partenariat Conférence des Barreaux OHADA**. Acteur sérieux à étudier en profondeur avant toute activation.
- **Legal Doctrine** (Algérie) — > 5 000 clients, primé Best African Legal Tech 2018-2019. Acteur établi Maghreb + Afrique.
- **Jurisprudence-OHADA.com** (IDEF, fonds institutionnel) — acteur historique OHADA.
- **Lexis 360 / Lextenso** — géants historiques EU avec présence Afrique partielle.

**Acteurs émergents** :

- **Jurisprudence.cc** (lancé 15/10/2025, Juris Intelligence, SIREN 938914959 Aix-en-Provence) — **acteur fragile**. Bootstrap 2 cofondateurs académiques (PhD candidates Aix-Marseille), 2-10 employés LinkedIn, 8 mois d'existence au 28/05/2026. **Annonce 17 pays, démontre 3** (témoignages BF + CI + Guinée). **Partenariats Barreaux confirmés sur 2 pays seulement** (BF + CI). Pricing **234 €/an EUR annuel only**, **pas de FCFA, pas de Mobile Money** = friction d'acquisition Afrique majeure. Zéro traction publique mesurable, pas de levée annoncée. Verdict : **fenêtre concurrentielle large ouverte sur cet acteur**.
- **Modulaw AI** (Nigeria, août 2025) — 28 cabinets, ~4 M NGN ARR (~2 400 €). Focus Nigeria common law, **pas OHADA francophone**.
- **Lawis.ai** — GCC + Maroc, **pas OHADA**.
- **Lexafrika / LegalSoba / Legafrik** — génération documents + mise en relation B2C, pas analyse de dossier IA. Legafrik facture 100 €/mois = benchmark pricing.

**Acteurs non concernés** : Predictice / Doctrine (fusion sept 2025, aucun plan Afrique annoncé).

### Conclusion concurrence — recadrée 2026-05-28

**Tous les concurrents établis OHADA sont du même paradigme : recherche-centric** (une base jurisprudentielle + IA de recherche augmentée). **Personne ne fait du dossier-centric** (analyse du dossier propre du client avec outils décisionnels métier). C'est exactement le pattern LegalCase.

**Fenêtre concurrentielle** :
- **Sur le paradigme recherche-centric** : Lexbase Afrique cadenasse partiellement (21 pays + Barreaux OHADA). Y entrer = guerre d'attrition.
- **Sur le paradigme dossier-centric** : **créneau vide**. Fenêtre 18-24 mois (au-delà, un acteur Lexbase ou Modulaw peut pivoter).

**Implication pour l'option β** : ne PAS concurrencer Jurisprudence.cc ou Lexbase frontalement (= encore une base). Positionner LegalCase OHADA comme **"l'analyseur de dossier OHADA qui parle FCFA + Mobile Money"** — un nouveau segment, pas une nouvelle base.

## Effort dev estimé

Réutilisation du noyau LegalCase actuel : **~60-70 %** (pipeline IA, OCR, multi-tenant, dashboard, outils décisionnels génériques, F-178 sync, etc.).

À développer (estimations en semaines-homme full-time, hors parallélisation agents) :

| Domaine | Effort | Commentaire |
|---|---|---|
| **OHADA business law** (10 Actes uniformes) | 30-42 sem | Sociétés commerciales + procédures collectives + voies d'exécution = ~60 % du volume |
| **Cameroun spécifique** (travail, étrangers, famille, fiscal, procédure) | 16-22 sem | Bloc équivalent à droit FR pour un seul pays |
| ~~**Bijuridisme common law NWSW**~~ | ~~9-13 sem~~ | **Reporté en V2** (décision 2026-05-28). Démarrage francophone OHADA-17 uniquement. -30 % Barreau CM accepté en V1. |
| **Jurisprudence CCJA + Cours suprêmes** | 7-9 sem | CCJA 1 325 arrêts = volume gérable |
| **Architecture multi-régions** (concept `workspace.region`, routage référentiels, branding conditionnel) | 2-3 sem | **Décision 2026-05-28** : pas de fork, pas d'autre app — même codebase, déploiement multi-régions |
| **Infra dédiée Africa** (cluster K8s AWS Cape Town ou OVH Africa, RDS, S3, CDN, monitoring) | 2-3 sem | Conformité loi 2024/017 CM (échéance 23/06/2026) — hébergement local obligatoire |
| **Paiement CinetPay** (MTN MoMo + Orange Money + carte XAF) | 1-2 sem | Routage `PaymentService` selon `workspace.region` |
| **DPO + conformité loi 2024/017** | 2 sem (doc + audit externe) | DPIA, registre CNDP, CGU locales |
| **Adaptation produit** (FR/EN bilingue, vocabulaire OHADA, templates) | 8-10 sem | Bilinguisme conditionne ouverture cabinets internationaux |
| **Go-to-market local** (landing OHADA, pricing XAF/EUR, CGU locales) | 4-6 sem | Hors dev pur |
| **Option α — Cameroun seul, 3 domaines V1** | **~40-56 sem (10-14 mois)** | Recopier V1 FR sur 1 pays. Aucun levier OHADA. TAM 3K avocats, ARR plafond 50-100 K€. |
| **Option β — OHADA business law seul, 17 pays** | **~24-40 sem (6-10 mois)** | Nouveau domaine pour LegalCase. Cible buyer = avocat d'affaires. Concurrent Jurisprudence.cc déjà lancé. |
| **Option γ — β + α combinés** | **~64-88 sem (16-22 mois)** | Plus lourd que V1 FR+BE actuelle (12-15 mois). Engagement majeur. |
| ~~Total minimal "OHADA business francophone seul"~~ | ~~estimé initial 4-6 mois~~ | **Estimation initiale corrigée 2026-05-28** : 6-10 mois (option β) |
| ~~Total "Cameroun complet"~~ | ~~estimé initial 6-8 mois~~ | **Estimation initiale corrigée** : 10-14 mois (option α) ou 16-22 mois (option γ) |

Avec parallélisation backend/frontend (cf. `feedback_parallel_frontback_default`) : calendrier divisable par ~1,5×.

**Comparaison V1 actuelle** : FR+BE a pris ~12-15 mois pour atteindre exhaustivité. Un OHADA-17 exhaustif demanderait un effort similaire — pas marginal.

## Modèle économique — 3 scénarios à 36 mois post-MVP

Hypothèses pricing (basées Legafrik 100 €/mois + Modulaw 85 €/mois + benchmarks SaaS B2B Afrique) :

| Plan | Prix mensuel | Cible |
|---|---|---|
| SOLO XAF | 25 000 XAF (~38 €) | TPE solo, masse OHADA |
| TEAM XAF | 60 000 XAF (~91 €) | Cabinets 2-5 personnes |
| AFFAIRES XAF/EUR | 100-150 000 XAF (~150-230 €) | Cabinets d'affaires OHADA |
| INTERNATIONAL EUR | 199-399 € | Cabinets internationaux Douala/Yaoundé/Abidjan/Dakar |

ARPU mixte projeté : **50-80 €/mois** (mix 60 % SOLO, 30 % TEAM, 10 % AFFAIRES/INTERNATIONAL).

| Scénario | Conversion sur TAM OHADA | Clients à 36 mois | ARPU | ARR à 36 mois |
|---|---|---|---|---|
| **Bear** | 0,5 % | 60-90 | 50 € | **36-54 K€** |
| **Base** | 1,5 % | 180-270 | 60 € | **130-195 K€** |
| **Bull** | 3 % | 360-540 | 70 € | **300-455 K€** |
| **Bull+** (avec corporate-b2b OHADA via cabinets affaires) | + 50 cabinets affaires à 200 € | 410-590 | mixte | **420-575 K€** |

**Lecture critique** :
- Bear scénario = **catastrophe ROI** (36-54 K€ pour 6-8 mois de dev full-time = coût opportunité énorme vs V1 FR/BE qui devrait être en croissance).
- Base scénario = correct mais **inférieur en €/client à FR+BE** (ARPU FR ~200-300 €/mois vs OHADA ~60 €).
- Bull/Bull+ = défendable comme thèse seed avec OHADA-17 → potentiel d'élargissement TAM cohérent avec radar-business-model.md chemin 2 (levée seed).

## Critères d'activation — très stricts (≥ 3/5)

Engager le dev OHADA suppose au minimum 3 des 5 critères suivants réunis :

| # | Critère | Seuil |
|---|---|---|
| 1 | **LegalCase FR+BE validé** | ≥ **30 K€ MRR atteint en bootstrap** (cf. radar-business-model.md) |
| 2 | **5 signaux convergents documentés** | Chaque signal du registre complété avec date + verbatim + identité source |
| 3 | **30 entretiens prospects OHADA approfondis menés** | Sur 3+ pays (CM + CI + SN minimum), 50 % positifs, 5+ engagements de paiement conditionnel |
| 4 | **Bijuridisme tranché** | ✅ **Tranché 2026-05-28 — Option A : francophone OHADA-17 uniquement au démarrage.** Common law NWSW reporté en V2 sous condition de traction Phase 1+2. Garde-fou : le messaging marketing ne doit jamais sous-entendre couvrir l'anglophone CM avant qu'il soit réellement implémenté. |
| 5 | **Co-fondateur OU partenaire local identifié** | ≥ 1 contact direct dans 3 pays OHADA (au-delà du père du founder, qui reste **advisor informel uniquement**) |

Si moins de 3 critères réunis : **observation passive, pas de décision**. Si 3-4 : **mode validation 30 entretiens** avant toute ligne de code. Si 5/5 : **engagement progressif scénario "Cameroun MVP → OHADA-17"**.

## Rôle du père magistrat — encadrement strict

**Règle non négociable** (cf. discussion 2026-05-28) :

> *"Mon père m'aide en famille, jamais en tant que magistrat. Tant qu'il est en fonction, il n'apparaît nulle part publiquement. Son réseau est un dernier recours pour ouvrir des portes après que le produit existe et tient debout."*

| Rôle | Acceptable ? | Modalité |
|---|---|---|
| Conseil métier en famille (droit OHADA/CM) | ✅ | Discussions privées |
| Intros informelles aux Bâtonniers via réseau magistrat panafricain (CCJA Abidjan, AHJUCAF) | ✅ avec précaution | Appels privés "père qui recommande son fils", pas lettres officielles |
| Participation au capital / equity / advisor formel rémunéré | ❌ | Interdit pendant fonction de magistrat (décret 95/048 statut magistrature CM) |
| Lettre de recommandation sur papier à en-tête | ❌ | Conflit d'intérêts manifeste |
| Apparition dans pitch deck / site / com publique | ❌ | Suicidaire institutionnellement |
| Post-retraite : advisor formalisé + equity | ✅ | À envisager si LegalCase OHADA est encore actif à ce moment |

Cette règle protège le père institutionnellement ET le produit (perception "outil côté juge" = repoussoir pour avocats).

## Décisions différées (à NE PAS faire maintenant)

- **Ne pas** créer de feature F-XXX au PRODUCT_SPEC.md liée à OHADA / Cameroun tant que les critères d'activation ne sont pas réunis
- **Ne pas** acheter de domaine type `legalcase.cm` / `legalcase.ci` / `legalcase-ohada.com`
- **Ne pas** créer de landing page Cameroun / OHADA, même brouillon
- **Ne pas** annoncer publiquement (LinkedIn / Village de la Justice / pitch) une thèse "leader OHADA" tant que la décision n'est pas prise
- **Ne pas** exposer le père publiquement, sous aucun prétexte, tant qu'il est en fonction
- **Ne pas** signer de partenariat / accord même informel avec un acteur OHADA (Barreau, cabinet, intégrateur paiement) sans verdict GO du radar
- **Ne pas** abandonner le développement FR/BE sous prétexte "OHADA semble plus prometteur" — l'invariant 30 K€ MRR FR/BE doit être atteint OU le marché FR/BE clairement invalidé

## Activation conditionnelle — séquence proposée si GO

Si à T+6 mois minimum (= ~2026-11-28) les 3+ critères sont réunis :

**Phase 0 (30 jours) — Validation terrain** : 30 entretiens prospects sur 3 pays OHADA (Cameroun, Côte d'Ivoire, Sénégal) via réseau père + LinkedIn + Bâtonniers. **Coût : 0 €, juste du temps founder.** Critère de passage Phase 1 : 50 % positifs + 5 engagements paiement conditionnel.

**Phase 1 (4-6 mois) — MVP "OHADA business law francophone"** : 10 Actes uniformes + jurisprudence CCJA + paiement CinetPay + hébergement AWS Cape Town. Cible 1ère vague : **20 cabinets affaires Douala/Yaoundé/Abidjan/Dakar**. Critère de passage Phase 2 : 10 cabinets payants à 90 €+/mois, NPS > 30.

**Phase 2 (3-4 mois) — Extension Cameroun spécifique** : droit du travail CM + immigration CM + famille CM. Cible : élargissement TPE Cameroun (200-500 cabinets adressables).

**Phase 3 (3-4 mois, optionnel) — Bijuridisme common law NWSW + expansion 16 autres pays OHADA** : décision à trancher selon traction Phase 1+2. **Bijuridisme common law reporté en V2** (décision 2026-05-28) sous condition de traction Phase 1+2 confirmée.

**Total calendrier estimé GO complet** : **10-14 mois** depuis décision d'engagement (sans common law NWSW V1).

## Réévaluation

- **Réévaluation programmée** : 2026-11-28 (T+6 mois) au plus tôt, avec les données du plan 30 K€ MRR FR/BE
- **Réévaluation anticipée si** :
  - 3+ nouveaux signaux convergents non sollicités (prospects OHADA, fonds VC vertical Africa, partenaire local entrant)
  - Concurrent OHADA lève une Series A significative (> 3 M€) ou annonce une expansion francophone agressive
  - Échec retentissant du PMF FR/BE (60 jours commercial sans signal d'achat) → la question devient *"continue, pivot ou stop ?"*
- **Réévaluation reportée si** : à T+6 mois la traction FR/BE est insuffisante (< 5 K€ MRR) — auquel cas pas d'extension géographique, recadrage produit prioritaire

## Lien avec les documents existants

- **`docs/radar-business-model.md`** — verrou commun : 30 K€ MRR FR/BE atteint **avant** toute décision business model OU extension. Si chemin 2 (levée seed) est activé un jour, la thèse OHADA-17 (TAM ×4) est un argument central.
- **`docs/radar-corporate-b2b.md`** — extension produit (cible buyer juriste d'entreprise) — **orthogonale** mais combinable : OHADA + corporate B2B = "Harvey francophone Afrique" thèse VC très défendable. À ne pas démarrer en parallèle (charge équipe).
- **`docs/radar-pivots-totaux.md`** — pivots où LegalCase est abandonné — **distinct** : ici on étend, on ne pivote pas.
- **`docs/PRODUCT_SPEC.md`** — aucun lien direct par construction. Si un jour activation : ouvrir une famille F-OHADA-XX (équivalente à F-DT/F-IM/F-FA pour FR+BE).
- **`docs/marketing/m71-budget-cadrage-2026h2.md`** — incompatible avec OHADA aujourd'hui (budget H2 2026 cible FR+BE). Une activation OHADA exigerait un nouveau cadrage budget M-XXX.
- **`docs/governance/transversal-concerns.md`** — toute extension OHADA déclenche les 4 préoccupations transversales (auth, workspace, paiement, navigation) → smoke tests E2E obligatoires.

## Annexes

- **Étude marché complète 2026-05-28** : transcrite dans la conversation Claude, à archiver dans `docs/radar-cameroun-ohada-etude-marche-2026-05-28.md` si décision GO Phase 0
- **Sources étude marché** : 23 URLs consultées (DataReportal, OHADA.com, loi 2024/017 PRC, African Law & Business, Investir au Cameroun, Cameroon Tribune, Dakar Actu, Africa Check, Stripe, Incorpuk, PayAtlas, CinetPay, Wikipedia CFA, Techpoint Modulaw, Lawis.ai, Latham & Watkins Doctrine-Predictice, Wikipedia crise anglophone, Onafriq, Le Monde du Droit, LinkedIn LegalSoba, We Are Tech Legafrik)
- **Zones aveugles documentées** : effectif Barreau CM 2025-2026, cumul avocats OHADA, revenu/propension à payer cabinet TPE CM, pricing exact jurisprudence.cc, position publique Barreau CM sur IA, taux smartphone vs feature phone chez avocats
