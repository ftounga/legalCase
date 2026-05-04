# MARKETING_BACKLOG.md — AI LegalCase

Backlog des actions marketing, vente et communication.
Mis à jour au fil des conversations. Priorisé par impact estimé.

---

## Définition de Terminé — règle impérative

**Une tâche marketing n'est marquée `Terminé` que si elle est entièrement opérationnelle.**

| Type de tâche | Ce que "Terminé" signifie |
|---------------|--------------------------|
| Email / séquence automatique | Code implémenté, déployé en production, emails envoyés réellement |
| Page web / landing | Composant déployé en production, accessible à l'URL publique |
| Vidéo / visuel | Fichier livré, publié sur le canal cible |
| Document (pitch, CGU…) | Document finalisé ET publié / transmis |
| Tracking / analytics | Tag en place, données qui remontent en production |

**Statuts intermédiaires :**

| Statut | Signification |
|--------|---------------|
| `À faire` | Pas encore démarré |
| `Rédigé` | Contenu produit (texte, brief, maquette) — pas encore implémenté |
| `En cours` | Implémentation en cours |
| `Terminé` | Opérationnel en production — vérifié |
| `Bloqué` | En attente d'un prérequis externe |

---

## 🧭 Cadrage stratégique

| ID | Action | Priorité | Statut | Notes |
|----|--------|----------|--------|-------|
| M-71 | Cadrage budget marketing 2026 H2 — enveloppe globale et arbitrage canaux | Haute | `Terminé` | Document de référence : `docs/marketing/m71-budget-cadrage-2026h2.md` (v1 2026-04-25). **Plan B-dynamique** : Tranche 1 = **13 000 € déployés mois 0** (Transfodroit atelier 3 500 € + vidéo 1 200 € + Village 600 € + Google Ads 1 500 € + SDR 2 400 € + Belgique 500 € + barreaux 1 000 € + voix off 200 € + déplacements 500 € + réserve activable 1 800 €). Tranche 2 = **9 000 € dry powder** activable à T+3 sur le canal gagnant (audit ≈ 2026-07-25). Pricing F-123 : ARR 2 300 €/an, LTV 3 ans 6 000 €. ROI Y1 attendu **2,8×**. Gouvernance CLAUDE.md règle 2 : toute nouvelle tâche M-XX > 1 000 € doit être justifiée contre ce document. |

---

## 🌐 Site & Landing page

| ID | Action | Priorité | Statut | Notes |
|----|--------|----------|--------|-------|
| M-01 | Landing page — mise à jour multi-domaines, FAQ, sections V2 | Haute | `Terminé` | Fait le 2026-03-29 : domaines (travail/immigration/famille), 8 feature cards, FAQ 6 questions, CTA bas de page |
| M-02 | Mentions légales — rédaction | Haute | `Terminé` | Déployé en staging — /mentions-legales |
| M-03 | Politique de confidentialité — rédaction | Haute | `Terminé` | Déployé en staging — /privacy |
| M-04 | CGU — rédaction | Haute | `Terminé` | Déployé en staging — /cgu |
| M-05 | Page contact — formulaire email | Moyenne | `Terminé` | F-78 déployée en production 2026-03-30 — /contact opérationnel sur legalcase.ng-itconsulting.com. |
| M-06 | SEO — balises meta, Open Graph, sitemap | Moyenne | `Terminé` | PR #165 mergée 2026-03-30 — déployé en production via CI/CD |
| M-07 | Google Analytics / Plausible — intégration tracking | Moyenne | `Terminé` | SF-77-01 déployée en production 2026-03-30 — bannière consentement RGPD + GA4 G-2JPL8JTXE7 opérationnel sur legalcase.ng-itconsulting.com. Validé manuellement. |

---

## 🎥 Vidéo promotionnelle

| ID | Action | Priorité | Statut | Notes |
|----|--------|----------|--------|-------|
| M-10 | Brief designer — livré | Haute | `Terminé` | PDF + captures HD 8 écrans livrés le 2026-03-29 |
| M-11 | Réalisation vidéo — 4 scripts motion design | Haute | `En cours` | Motion designer contacté, devis 1000€ accepté. 4 vidéos en production. **Priorité de livraison : V1 → V4 → V2 → V3.** V1 (60s "La synthèse en 15 secondes") : landing page + Google Ads. V4 (45s "Ne rien rater") : outreach + événements. V2 (45s "Du document à la décision") : LinkedIn. V3 (45s "L'IA qui apprend de vous") : nurturing prospects chauds. **Prérequis avant livraison : confirmer l'URL finale (ailegalcase.fr ou legalcase.ng-itconsulting.com), fournir screen recordings avec données démo propres, confirmer voix off incluse ou budget ~200€ en plus.** |
| M-12 | Déclinaison carré LinkedIn/Instagram (1:1) | Haute | `À faire` | Après livraison M-11 |
| M-13 | Déclinaison Stories/Reels (9:16) | Moyenne | `À faire` | Après livraison M-11 |
| M-14 | Teaser 15s pour bannière web | Moyenne | `À faire` | Après livraison M-11 |

---

## 📧 Email marketing

| ID | Action | Priorité | Statut | Notes |
|----|--------|----------|--------|-------|
| M-20 | Séquence onboarding — email J+0 (bienvenue) | Haute | `Terminé` | Validé en staging le 2026-03-29 |
| M-21 | Séquence onboarding — email J+2 (tip analyse) | Haute | `Terminé` | Scheduler nightly 8h opérationnel en staging |
| M-22 | Séquence onboarding — email J+5 (tip partage client) | Haute | `Terminé` | Scheduler nightly 8h opérationnel en staging |
| M-23 | Séquence onboarding — email J+12 (avant expiration trial) | Haute | `Terminé` | Scheduler nightly 8h opérationnel en staging |
| M-24 | Email de conversion — FREE expiré | Haute | `Terminé` | Scheduler nightly 8h opérationnel en staging |
| M-25 | Choix d'un outil d'emailing + intégration | Haute | `Terminé` | Brevo déjà intégré via JavaMailSender SMTP — utilisé pour invitations, vérification email, reset MDP. Aucune action supplémentaire requise. |
| M-26 | Newsletter mensuelle — template | Basse | `Terminé` | PR #207 mergée 2026-04-01 — scheduler mensuel 1er du mois 8h, stats dynamiques par workspace, 24 features en rotation déterministe (2 ans), déduplication via email_sends |

---

## 💼 LinkedIn & réseaux sociaux

| ID | Action | Priorité | Statut | Notes |
|----|--------|----------|--------|-------|
| M-30 | Page entreprise LinkedIn — création | Haute | `Terminé` | Page créée le 2026-03-30 — URL : linkedin.com/company/ai-legalcase — bannière 1128×191px livrée |
| M-31 | Post de lancement LinkedIn (texte + vidéo) | Haute | `Terminé` | Publié le 2026-04-07 — post entreprise + post perso avec redirection. Vidéo démo jointe en natif. |
| M-32 | Post "Comment ça marche" — carousel 5 slides | Haute | `Terminé` | PDF 6 slides publié le 2026-03-30 — docs/marketing/m32-carousel-comment-ca-marche.pdf |
| M-33 | Post témoignage / démo utilisateur | Moyenne | `À faire` | Dès les premiers clients |
| M-34 | Stratégie de contenu LinkedIn — planning 3 mois | Moyenne | `Terminé` | 28 posts planifiés avril-juin 2026 — intégré dans le workflow n8n M-38 |
| M-35 | Compte Twitter/X — création et premiers posts | Basse | `À faire` | Audience tech/legal |
| M-38 | Workflow n8n — auto-génération et publication LinkedIn | Haute | `Bloqué` | **2026-05-04 — version d'essai n8n expirée + bug détecté** : Schedule Trigger configuré sur mardi/jeudi alors que les 28 dates planning M-34 tombent toutes sur mercredi/vendredi → 0 publication automatique depuis 2026-04-01 (seul le post du 2026-04-01 a été publié via Execute Workflow manuel). Décision : ne pas renouveler n8n, basculer en feature backend interne **F-188 LinkedIn auto-publication** ajoutée au PRODUCT_SPEC.md (À planifier, V8+). M-38 reste `Bloqué` jusqu'à livraison F-188 — sera remplacée par `Couvert par F-188 / n8n abandonné`. **Court terme** : publication manuelle des posts du planning (M-61 prêt, M-34 dates conservées) en attendant F-188. |

---

## 🤝 Vente directe & partenariats

| ID | Action | Priorité | Statut | Notes |
|----|--------|----------|--------|-------|
| M-40 | Pitch deck — présentation cabinets | Haute | `Rédigé` | PPTX 11 slides livré le 2026-03-29 — docs/marketing/pitch-deck-ailegalcase.pptx |
| M-41 | Liste de 50 cabinets cibles (droit travail, Paris/IDF) | Haute | `Terminé` | 50 cabinets, 47 URLs LinkedIn vérifiées. Suivi outreach dans m41-suivi-outreach.md. 5 premiers messages envoyés le 2026-03-30. |
| M-42 | Script d'outreach LinkedIn — message de prospection | Haute | `Rédigé` | 4 templates livrés le 2026-03-29 — docs/marketing/m42-script-outreach-linkedin.md |
| M-43 | Démo en ligne — Calendly ou équivalent | Haute | `Terminé` | https://calendly.com/tounga-franck-ng-itconsulting/30min — intégré dans Template E de M-42 |
| M-60 | Hébergement souverain — argument pitch deck et outreach | Haute | `Rédigé` | Template G+H ajoutés à M-42 (cabinets sensibles, Cloud Act). Brief slide pitch deck dans docs/marketing/m60-pitch-slide-souverainete.md — à intégrer dans le PPTX. |
| M-61 | Hébergement souverain — post LinkedIn comparatif | Haute | `Bloqué` | Post "Cloud Act vs hébergement européen" dans docs/marketing/m61-post-linkedin-cloud-act.md — texte rédigé, prêt à publier (version 2026-05-04 corrigée "LegalCase" sans "AI"). **2026-05-04 — Bloqué par F-188** (LinkedIn auto-publication, V8+ À planifier). Décision utilisateur : ne pas publier manuellement et attendre la mise en place de la feature F-188 — cohérent avec le choix de basculer toute la chaîne LinkedIn (M-38) en feature backend interne plutôt que via n8n. M-61 sera publié via la queue F-188 dès que la feature sera livrée. Statut → `Terminé` après publication effective. |
| M-44 | Partenariats barreaux / associations d'avocats | Moyenne | `En cours` | 8 envoyés (07-08/04) : FNUJA, AFDT, Incubateur Barreau Paris, SAF, ACE, Barreau de Paris, Barreau 93, Barreau 92. Barreau 94 bloqué (formulaire buggé). ADAO retirée (n'existe pas). Relance J+7 le 2026-04-14. **2026-05-04 — Réponse positive ACE** : Stéphanie Colin (s.colin@avocats-ace.fr, +33 6 95 63 11 00) a transmis la plaquette partenariats 2026 (1500+ adhérents barreau d'affaires, audience droit social employeur). Format ciblé pour démarrer = co-intervenant webinaire/formation Qualiopi 1 500 € HT (cf. M-77 dédiée — voir séquence stratégique M-71). Mail de cadrage envoyé le 2026-05-04 (lien Calendly fourni), en attente de RDV. Pas de Pack Visibilité 5 K€ ni Pack Majeur 20 K€ avant traction prouvée (≥ 5 clients payants signés, cohérent règle CLAUDE.md > 1 000 € hors M-71). Statut autres contacts (FNUJA, AFDT, SAF, etc.) : silence depuis relance — à clore en bilan. |
| M-45 | Programme de référence — un mois offert pour parrainage | Moyenne | `À faire` | Croissance organique |
| M-54 | Démos terrain — 2 avocats du réseau personnel | Haute | `À faire` | 1 avocat droit des affaires, 1 avocat droit de l'immigration (domaine activé en V1). Objectif : conversation 20 min, pas de vente — écouter leurs douleurs, montrer l'outil, demander des introductions vers des avocats droit du travail. Message : personnel, informel, demander un avis pas une vente. |
| M-53 | Partenariat éditorial Village de la Justice | Haute | `En cours` | Newsletter 60 000 professionnels du droit. **Accord obtenu**, article à rédiger. Contact : Christophe Albert (04 76 94 70 47). 600€ HT/an = vidéo + liens + relais newsletter + réseaux sociaux (budget Tranche 1 — cf. § Stratégie acquisition). Compte Membre créé le 2026-04-07. Brief tribune (angle "ce qui fonctionne vraiment en cabinet, ce qui ne fonctionne pas encore", 800-1 200 mots) prêt dans docs/marketing/m53-tribune-brief.md. **Note 2026-05-04** : on cherche **2 collaborations distinctes** avec le Village — (1) ce partenariat éditorial M-53 (tribune, newsletter, vidéo, réseaux), (2) **présence Transfodroit / Technodroit / table ronde** suivi dans M-72. Les 2 sont complémentaires (éditorial = visibilité continue ; Transfodroit = visibilité événementielle ponctuelle). |
| M-66 | CNB — identifier les bons contacts (commission Numérique, commission Textes, secrétariat général) | Haute | `À faire` | Cibler la **Commission Numérique CNB** (la plus pertinente pour outils IA) + **Commission Textes** (RGPD/secret pro). Pas de mass mailing, contacts nominatifs. Source : cnb.avocat.fr (organigramme + commissions). |
| M-67 | CNB — email de présentation officielle de l'outil | Haute | `À faire` | Dépend de M-66. Angle : outil IA souverain (hébergement européen, Cloud Act), conforme secret professionnel, dédié droit du travail. Demande : présentation devant la commission Numérique. Réutiliser le pitch deck M-40 + brief souveraineté M-60. |
| M-68 | CNB — référencement dans l'annuaire des solutions numériques pour avocats | Haute | `À faire` | Le CNB référence des solutions sur cnb.avocat.fr. Vérifier les conditions d'inscription, monter le dossier (présentation produit, conformité RGPD, hébergement, secret pro). |
| M-69 | Convention Nationale des Avocats — présence/sponsoring | Moyenne | `À faire` | Événement annuel CNB. Évaluer coût stand vs simple présence. À budgéter sur l'enveloppe M-58 ou en complément. |
| M-70 | CNB — partenariat formation continue (EFB / écoles d'avocats) | Moyenne | `À faire` | Toute heure de formation comptabilisée par les EFB est un canal d'acquisition massif. Proposer un module "IA et droit du travail" comme cas d'usage de l'outil. |
| M-72 | Village de la Justice — sondage Transfodroit 2026 (atelier sponsorisé + Technodroit + table ronde) | Haute | `En cours` | Cadré par M-71. **2ᵉ collaboration distincte** avec le Village (la 1ᵉ étant le partenariat éditorial M-53 — éditorial vs événementiel). Capitaliser sur M-53 en cours via Christophe Albert (relation chaude). Demander : (a) tarifs et conditions atelier sponsorisé 30 min Transfodroit (budget alloué 3 500 €), (b) conditions et deadline du concours **Technodroit 2026** (pitch jeunes legaltechs — si retenu, économie 3 500 € à réallouer en Tranche 1 réserve), (c) ouverture intervention table ronde. Output : tableau récap conditions + recommandation. Effort : 0,5 j. **Mail envoyé le 2026-05-04** à Christophe Albert (3 demandes en bullets : atelier / Technodroit / table ronde, capitalise sur M-53). En attente de retour. **Livrables — à produire uniquement quand l'utilisateur communique le retour de Christophe** : (1) tableau récap des 3 réponses (tarifs, deadlines, formats par axe) ; (2) recommandation budget — atelier sponsorisé seul (3 500 € engagés) **OU** candidature Technodroit (gratuit/peu cher, économie 3 500 € à réallouer en Tranche 1 réserve activable) ; (3) plan d'action prochaine étape (préparer dossier M-74 Technodroit ? signer le bon de commande atelier ? planifier la table ronde ?). **Ne rien produire avant le retour explicite** — éviter prépa prématurée. |
| M-73 | One-pager traction commerciale présentable | Haute | `Terminé` | **2026-05-04** : couvert par feature produit `F-187 Export traction commerciale (one-pager PDF auto)` Terminée le 2026-05-04 (PR #820 backend + PR #821 frontend mergées en parallèle). Page super-admin `/super-admin/traction-onepager` opérationnelle : KPIs F-76 lus automatiquement (inclusion/exclusion individuelle), formulaire pour verbatims (0-2), partenaires (0-10), accroche, contact pré-rempli depuis le user connecté → bouton "Générer PDF" → téléchargement direct `legalcase-traction-YYYY-MM-DD.pdf`. Friction Canva manuelle mensuelle (10-15 min) éliminée. Disponible en staging dès le déploiement (run CI/CD backend + frontend lancé 2026-05-04). |
| M-74 | Candidature Technodroit 2026 (concours jeunes legaltechs) | Moyenne | `Bloqué` | Bloqué par M-72 (modalités à confirmer) + M-73 (asset traction). Format pitch scène devant jury, généralement gratuit ou peu cher. Dossier : démo M-11, pitch deck M-40, traction M-73, brief souveraineté M-60. Effort : 2 j. |
| M-75 | Veille AMI France Legaltech 2027 (DGE / Mission French Tech) | Basse | `À faire` | Surveiller annonce DGE / Mission French Tech (probable T4 2026 pour vague 2027). Première promotion 2026 = 10 lauréats annoncés au AI Day 2026 (Dastra, Pappers Justice, Legapass…). Préparer dossier en amont (réutilise M-40 + M-73). Effort : 0,25 j veille + 2 j candidature le moment venu. |
| M-76 | Partenariats éditeurs logiciels juridiques (Secib, Kleos, Jarvis Legal, Clio, Juris Concept, LegalUp) | Moyenne | `Bloqué` | Canal de distribution alternatif via les éditeurs qui détiennent déjà la relation commerciale avec des dizaines de milliers de cabinets FR+BE. 3 modèles possibles : (a) **intégration native** — LegalCase devient un module dans Secib/Kleos/etc., l'avocat clique "Analyser avec LegalCase" depuis son outil de gestion ; partage de revenu ou facturation à l'éditeur. Le plus puissant mais plus dur à négocier ; (b) **distribution + commission** — l'éditeur recommande LegalCase via newsletters, commerciaux, événements ; rétrocession 15-25 % du 1er abonnement annuel. Plus simple à mettre en place ; (c) **white label** — l'éditeur revend LegalCase sous sa marque ("Analyse IA by Secib"). Bon revenu mais perte de visibilité marque. **Déclencheur de déblocage** : ≥ 5 clients payants signés + ≥ 2 témoignages publics utilisables (à atteindre via M-44, M-62, M-56, M-57). Sans traction prouvée, l'éditeur dira non — un non maintenant verrouille la conversation pour 6-12 mois. **Approche progressive** : commencer par le plus petit éditeur sans module IA + base clients intéressante, démo sur vrai dossier, pilote 10 clients gratuits 3 mois en échange de leur aide à intégrer et vendre ensuite. **Pas avant Q4 2026** selon traction. Effort : 5-10 j (cartographie marché + identification interlocuteurs + démos + négociation). Coût : 0 € direct, modèle auto-financé par la commission. |
| M-77 | Co-intervention webinaire / formation Qualiopi ACE | Haute | `À faire` | Format issu du retour positif M-44 (ACE — Stéphanie Colin). **1 500 € HT (1 800 € TTC)** ponctuel, sans engagement annuel. Audience = adhérents ACE droit social employeur (sous-ensemble des 1500+ adhérents barreau d'affaires). Format = co-intervenant webinaire ou formation hybride Qualiopi (organisme de formation Qualiopi ACE). **Imputation budgétaire** : ligne "réserve activable 1 800 €" Tranche 1 M-71 (cf. plan B-dynamique). **Justification > 1 000 € hors enveloppe barreaux M-71 (1 000 €)** : la ligne barreaux finance prospection sortante (mass mail, relances) ; M-77 finance une opportunité entrante créée par M-44 — cas d'usage exact de la réserve activable. **Prérequis** : RDV Calendly avec Stéphanie Colin pour caler thématique, format (30 min démo + 30 min Q/R proposé), date, et co-intervenant ACE (commission Droit social). **Critère de succès** : ≥ 1 lead qualifié converti en essai 14 j ; ≥ 3 contacts post-webinaire pertinents. **Signal Tranche 2** : si 2-3 clients payants émergent dans les 90 j, basculer vers Pack Visibilité ACE 5 K€ (Tranche 2 dry powder, audit T+3 ~2026-07-25). **Contrôle de cohérence 4 points** : (1) budget ✓ via réserve 1 800 € M-71 ; (2) doublon feature ✗ (hors champ produit) ; (3) doublon backlog ✗ — distinct de M-44 (prospection), M-58 (événements), M-70 (EFB) ; (4) séquence stratégique ✓ — convertit un retour M-44, format mesuré qui précède Pack Visibilité 5 K€ (qui exigerait traction prouvée). **Préparation RDV — à déclencher uniquement quand l'utilisateur dit "prépare-moi l'entretien ACE" ou équivalent (RDV calé / imminent)** — livrables attendus à ce moment-là : (i) 3 propositions de thématiques webinaire alignées sur l'actualité ACE (e-revue n°165 mai 2026 *« Bien-être au travail »* ; n°166 nov 2026 thème d'actualité) ; (ii) format type proposé (titre, durée, plan 30 min démo + 30 min Q/R, démo cas réel anonymisé) ; (iii) questions à poser à Stéphanie au RDV (audience attendue, mode de promo ACE, replay disponible, droits sur supports, deadline contenu) ; (iv) points de négo (date évitant Spring Break/Congrès, co-intervenant ACE commission Droit social à demander, mention sur newsletter ACE incluse ?) ; (v) draft d'offre adhérents ACE pour la page 7 vierge de la plaquette (typiquement -20 % 1ère année ou 1 mois offert sur SOLO/TEAM/PRO — cohérent F-123 LTV 6 K€, à arbitrer au moment de la prépa) ; (vi) GO/NO-GO sur le bon de commande 1 500 € HT à signer post-RDV avec critères de décision explicites. **Ne rien produire avant le déclencheur explicite** — éviter la prépa prématurée tant que la date du RDV n'est pas confirmée. |

### 🇧🇪 Belgique

| ID | Action | Priorité | Statut | Notes |
|----|--------|----------|--------|-------|
| M-62 | Partenariats barreaux / associations Belgique | Moyenne | `En cours` | 4 emails envoyés le 2026-04-07 : OBFG (info@avocats.be), Jeune Barreau de Bruxelles (info@jbb.be), Barreau de Liège-Huy (batonnat@barreaudeliege-huy.be), Barreau de Charleroi (barreau@barreaudecharleroi.be). Relance J+7 le 2026-04-14. ABDT retirée (n'existe pas, orga réelle = ABETRASS académique). |
| M-63 | Google Ads — campagne Belgique | Moyenne | `À faire` | Ciblage géo Belgique. Mots-clés : "logiciel avocat Belgique", "outil IA cabinet avocat Bruxelles", "analyse dossier juridique automatique". Budget à prendre sur l'enveloppe M-56 (split FR/BE, ex: 1 000€ FR / 500€ BE). |
| M-64 | Liste cabinets cibles Belgique + outreach | Moyenne | `À faire` | 20-30 cabinets droit du travail Bruxelles/Wallonie (francophone). Même approche que M-41. Budget : 0€. |
| M-65 | Partenariat éditorial Belgique | Moyenne | `À faire` | Cibles : Jubel.be, Droitbelge.be, Justice-en-ligne.be. Publication sponsorisée ou tribune. Budget estimé : 200-500€ (à évaluer au contact). |

---

## 🎯 Stratégie acquisition 13 000 € — Plan B-dynamique (cadré par M-71)

> 🧭 Cadrage de référence : `docs/marketing/m71-budget-cadrage-2026h2.md`. La présente section liste les tâches opérationnelles ; le doc M-71 fixe l'enveloppe (13 K€ Tranche 1 + 9 K€ Tranche 2 dry powder), le ROI attendu (2,8× Y1), et l'audit T+3.

Séquence à respecter : M-53 (crédibilité) + M-55 (vidéo) en parallèle → M-56 (Google Ads) + M-57 (SDR) → M-72 (sondage Transfodroit) → atelier sponsorisé OU Technodroit → M-58 (barreaux locaux).

| ID | Action | Budget | Priorité | Statut | Dépendances | Notes |
|----|--------|--------|----------|--------|-------------|-------|
| M-55 | Vidéo démo intégrée landing page | 0€ (couvert M-11) | Haute | `Terminé` | — | Vidéo YouTube embed intégrée entre hero et section problème/solution. Déployé en production 2026-04-07. |
| M-56 | Google Ads — campagne mots-clés intention | 1 500€ | Haute | `À faire` | M-55 intégré sur landing | Mots-clés : "logiciel avocat IA", "analyse dossier juridique automatique", "outil IA cabinet avocat". Budget ~2-3€/clic = 500-700 visiteurs. Durée : 4-6 semaines. **Prérequis tech OK** (F-119 mergée — tracking conversion branché). **Reste à faire** : (1) créer l'action de conversion dans Google Ads (Outils → Conversions), (2) renseigner le Conversion ID (`AW-XXXXXXXXX/YYYYYY`) dans `environment.prod.ts`, (3) redéployer le frontend, (4) créer la campagne Search (structure, annonces, mots-clés, budget). |
| M-57 | SDR freelance — 6 semaines | 2 400€ | Haute | `À faire` | M-55 intégré sur landing | Recruter sur Malt. Profil : SDR expérience SaaS B2B ou professions libérales. Mission : 10-20 RDV qualifiés avocats droit du travail Paris/IDF. Valider ses messages avant envoi. |
| M-58 | Événements barreaux / associations avocats | 1 000€ | Moyenne | `À faire` | Aucune | 2 événements du trimestre : barreau Paris, FNUJA, LegalTech Hub Paris. Présence physique = conversion 10x LinkedIn. |

**Tranche 1 (13 000 € — déployable mois 0)** : 600 € M-53 + 1 200 € M-11 + 1 500 € M-56 + 2 400 € M-57 + 1 000 € M-58 + 500 € M-65 + 3 500 € atelier Transfodroit (M-72 si non Technodroit) + 500 € déplacements + 200 € voix off + 1 800 € réserve activable.
**Tranche 2 (9 000 € — dry powder activable T+3)** : déploiement conditionné à l'audit signal (cf. M-71 §7). Cible : doublement canal gagnant, 2ᵉ événement, ou cycle SDR additionnel.

---

## 📊 Mesure & analytics

| ID | Action | Priorité | Statut | Notes |
|----|--------|----------|--------|-------|
| M-50 | Dashboard de conversion — inscription / activation / paiement | Haute | `Terminé` | Couvert par F-76 (super-admin) : totalWorkspaces, trialWorkspaces, paidWorkspaces, conversionRatePct, activeWorkspaces30d — déployé en production 2026-03-31 |
| M-51 | Tracking événements clés (analyse lancée, PDF exporté, lien partagé) | Moyenne | `Terminé` | analysis_launched (STANDARD+ENRICHED), pdf_exported, upgrade_clicked, docx_exported, chat_message_sent, share_link_created — déployé en prod via CI |
| M-52 | NPS — enquête satisfaction client | Basse | `À faire` | Dès 10 clients actifs |

---

## 📋 Priorités recommandées pour commencer

1. **M-02/03/04** — Mentions légales, CGV, RGPD (obligatoires légalement)
2. **M-25/20/21/22/23** — Séquence email onboarding (impact immédiat sur la conversion)
3. **M-40** — Pitch deck pour démos cabinets
4. **M-30/31** — Page LinkedIn + post de lancement
5. **M-11** — Vidéo promo (après le brief livré)
