# Différenciateurs LegalCase vs concurrents — antisèche

**Pour qui** : Franck, fondateur — usage avant tout RDV prospect, démo, pitch.
**Format** : 5 min de lecture, structuré pour répondre vite aux objections.
**Mise à jour** : voir bas de page.

---

## Le marché en 1 paragraphe

Le segment "IA pour avocats" est animé par **3 archétypes de concurrents** :
- **Recherche jurisprudence + assistant texte** (Doctrine, Strada, Lexbase) — cible cabinets FR de toutes tailles
- **Analyse prédictive** (Predictice) — cible avocats + DAJ entreprises FR
- **Assistants IA généralistes** (Harvey, CoCounsel, Jimini, Ordalie, Luminance) — cible mixte, du petit cabinet (Jimini) aux multinationales (Harvey)

LegalCase n'est **aucun de ces 3 archétypes**. C'est un **outil d'analyse de dossier + outils décisionnels métier par domaine**. Cela change tout dans la conversation.

---

## Les 3 concurrents principaux à nommer

| Concurrent | Pays / Hébergement | Produit principal | Cible | Prix |
|------------|---------------------|-------------------|-------|------|
| **Doctrine** | FR, AWS Paris (mais investisseurs US) | Recherche jurisprudence + assistant IA "Doctrine GPT" | Tous juristes FR (~25 000 cabinets) | Opaque, ~100-300 €/mois/utilisateur |
| **Predictice** | FR, Azure Paris | Analyse prédictive jurisprudence + assistant IA | Avocats + DAJ entreprises FR (~5 000 cabinets) | ~150 €/mois/utilisateur |
| **Harvey** | US, Azure US (Cloud Act applicable) | Assistant IA généraliste (rédaction, recherche, due diligence) | Top 100 grands cabinets internationaux | Enterprise 50-200k$/an/firme |

**À retenir pour vite répondre :**
- Doctrine FR + AWS Paris — **EU OK**
- Predictice FR + Azure Paris — **EU OK**
- Harvey US + Azure US — **EU PAS OK (Cloud Act)**

---

## Les 3 différenciateurs principaux

### 🥇 1. Outils décisionnels métier (vs assistant chatbot générique)

**Le fait** : Doctrine GPT, Predictice GPT, Harvey = tous des **assistants généraux** (tu poses une question, ça répond ; tu écris un brouillon, ça améliore). LegalCase = **outils décisionnels calibrés par situation métier**.

Exemples d'outils LegalCase qui n'existent nulle part ailleurs :
- Calculateur de prestation compensatoire (méthode Cresson-Rascle)
- Calculateur de liquidation de régime matrimonial
- Outil décision asile / séjour / regroupement familial
- Outil recours OQTF avec calcul délais procédure
- Calculateur indemnité licenciement (FR + BE CCT 109)
- Détecteur prescription action prud'homale

**Plus** : ces outils sont **pré-remplis automatiquement par l'IA** depuis les pièces du dossier (badge `auto_awesome` à côté de chaque champ). **Plus** : alerte visuelle si l'avocat saisit une valeur contradictoire avec ce que l'IA a détecté.

**Phrase type pour la démo** :
> *« La différence concrète avec un Doctrine GPT ou un Harvey, c'est qu'on ne fait pas un assistant général qui répond à tout vaguement. On fabrique des outils décisionnels métier — un par situation type — qui se pré-remplissent depuis votre dossier. Vous obtenez un chiffrage défendable en 30 secondes, pas une réponse texte à interpréter. »*

🎯 **C'est LE différenciateur. À pousser systématiquement.**

---

### 🥈 2. Souveraineté EU stricte (vs Harvey surtout)

**Le fait** : LegalCase = AWS Paris, aucun stockage hors UE, sous-processeur Anthropic déclaré dans la politique de confidentialité. Harvey = Azure US, **Cloud Act** applicable (loi US 2018 qui permet aux autorités américaines de réquisitionner des données chez tout opérateur US, même quand elles concernent des Européens et sont physiquement en Europe, sans passer par la justice européenne).

**Pourquoi c'est lourd pour un avocat** :
- Secret professionnel = obligation déontologique absolue (article 458 Code pénal BE / article 226-13 Code pénal FR)
- L'OBFG (Belgique) et le CNB (France) ont publié des avis défavorables sur l'utilisation de SaaS US pour données client sensibles
- Risque déontologique direct si dossier asile, divorce conflictuel, défense pénale sensible

**Phrase type** :
> *« Sur la dimension européenne stricte, on est positionné différemment des outils US comme Harvey. AWS Paris, pas de Cloud Act, sous-processeurs déclarés. C'est important si vous traitez des dossiers étrangers/asile sensibles ou de la défense pénale. »*

⚠️ **Argument faible vs Doctrine et Predictice** (eux aussi sont en Europe). À sortir uniquement si la conversation porte sur Harvey ou si la question RGPD est posée frontalement.

---

### 🥉 3. Solo founder + dialogue direct + cycle court

**Le fait** : LegalCase = 1 personne (Franck), pas d'équipe commerciale, pas de manager produit qui filtre les retours. Doctrine = ~150 employés. Harvey = ~200 employés, focus grands comptes uniquement. Predictice = ~50 employés, process commercial standardisé.

**Pourquoi c'est un atout pour les cabinets indépendants** :
- Leurs besoins sont **invisibles pour Doctrine/Harvey** (non prioritaires sur des roadmaps massives)
- Avec LegalCase : retour utilisateur → évaluation en 1 semaine → développement en 2-4 semaines si pertinent
- Pas de boîte commerciale entre l'avocat et les décisions produit

**Phrase type** :
> *« Je suis seul à décider de la roadmap. Si vous me dites pendant le test "il me manque X", je peux l'évaluer dans la semaine et le sortir dans 2-4 semaines si c'est pertinent. Vous n'aurez pas ça avec Doctrine ou Harvey, ce n'est pas leur modèle. »*

⚠️ **Anticiper le revers** : *"un solo, c'est fragile, et si tu pars en vacances ?"*. Réponse :
> *« Hébergement et infrastructure tournent en automatique 24/7, surveillance automatisée, pas d'arrêt de service. Le développement est piloté par moi seul — c'est un atout vitesse, pas une faiblesse opérationnelle. »*

---

## Les 4 différenciateurs secondaires (en réserve)

| # | Argument | Quand le dégainer |
|---|----------|-------------------|
| 4 | **Cycle Q&A IA → avocat répond → re-synthèse enrichie** | Si question *"que se passe-t-il si l'IA rate quelque chose ?"* — réponse : elle pose des questions ciblées, vous répondez, elle re-synthétise en intégrant vos réponses |
| 5 | **Versioning analyses + diff sémantique** | Si elle ajoute des pièces à un dossier en cours — elle voit ce qui a changé d'une analyse à l'autre, en surbrillance |
| 6 | **Multi-domaines V1 (travail + famille + étrangers)** sur le même outil | Si elle a 3-4 spécialités — pas besoin de 3 outils, 1 seul couvre |
| 7 | **Pricing accessible : 99-429 €/mois** | Si question prix vs Harvey 50-200k$/an et Doctrine opaque |

---

## Ce qu'on NE DIT PAS (humilité produit)

L'honnêteté sur les angles morts est **un argument en soi** — ça rend la promesse crédible.

| Ce que les autres ont mieux | Réponse honnête |
|----------------------------|----------------|
| Doctrine = base jurisprudence + textes de loi indexés (millions de décisions) | *« On n'est pas un moteur de recherche jurisprudence. Pour ça, restez sur Doctrine ou Strada. On est complémentaire, pas substitut. »* |
| Predictice = analyse statistique prédictive (taux de succès, fourchettes indemnités) | *« On ne fait pas de prédictif statistique. Notre approche, c'est l'aide à la décision sur SON dossier, pas la moyenne du marché. »* |
| Harvey = qualité rédaction sur contrats anglo-saxons complexes | *« Si vous travaillez en M&A international, Harvey est mieux. Notre cible, c'est le contentieux courant FR/BE. »* |

---

## Tableau réactions express selon question

| Question type | Phrase de cadrage | Pivot vers... |
|---------------|-------------------|---------------|
| *« J'ai déjà Doctrine, vous faites quoi de plus ? »* | *« Doctrine et nous, on est complémentaires, pas concurrents. »* | **Différenciateur 1** (outils décisionnels métier) |
| *« Pourquoi pas Predictice ? »* | *« Predictice fait du prédictif statistique sur le marché. Nous, on assiste sur SON dossier. »* | **Différenciateur 1** (pré-remplissage F-IA-04 sur le dossier client) |
| *« Vous êtes plus cher que Harvey ? »* | *« Non, l'inverse. Harvey c'est 50-200k$/an. Nous c'est 99-429 €/mois. Les modèles ne se comparent pas. »* | **Différenciateur 6** (cible cabinet indépendant) |
| *« Comment vous vous démarquez en 3 mots ? »* | *« Souveraineté EU stricte, outils décisionnels métier par domaine, dialogue direct sans intermédiaire. »* | Selon ce qu'elle relance |
| *« C'est hébergé où ? »* | *« AWS Paris (eu-west-3), aucun stockage hors UE, sous-processeur Anthropic déclaré. »* | **Différenciateur 2** (souveraineté) |
| *« Vous êtes seul ? »* | *« Oui — fondateur unique, ingénieur SI, 14 ans d'expérience IT. C'est aussi un argument : pas de boîte de vente, dialogue direct. »* | **Différenciateur 3** (cycle court) |
| *« Quelle est votre roadmap pénal / IP / fiscal ? »* | *« Pas en V1, on priorise selon demande marché. Si vous êtes intéressée, je vous mets en boucle quand le sujet se concrétise. »* | Pas de pivot — accepter et capitaliser sur le test 14 j |

---

## Erreurs à éviter

| Erreur | Pourquoi |
|--------|----------|
| Dénigrer Doctrine ou Harvey | Le prospect peut être client de Doctrine — tu insultes son choix passé |
| Promettre des features qu'on n'a pas | Le test 14 jours révèle tout. Promesse cassée = perte définitive |
| Tomber dans la guerre des fonctionnalités | Le différenciateur 1 (outils décisionnels) gagne la conversation. Les autres sont des renforts |
| Insister sur la souveraineté avec un avocat qui s'en fiche | Pas tous les avocats sont sensibles RGPD. Si elle ne mord pas, passe à autre chose |
| Cacher qu'on est solo | C'est visible sur le site, sur LinkedIn, dans le Calendly. Mieux vaut l'assumer comme atout |

---

## Mise à jour de ce document

**Dernière révision** : 2026-05-10
**Sources** :
- Mémoire `project_competitive_advantages.md` (39 jours)
- Site web concurrents (Doctrine, Predictice, Harvey) — vérifié 2026-05-10
- Levées de fonds Crunchbase / TechCrunch 2024-2026

**À mettre à jour si** :
- Nouveau concurrent émerge en FR/BE (Mistral Legal, Anthropic Legal Suite, etc.)
- Doctrine ou Predictice sortent de nouveaux modules (analyse de dossier sur pièces, outils décisionnels) — la vraie menace
- Harvey baisse drastiquement son prix ou cible explicitement les indépendants
- Modification du périmètre V1 LegalCase
