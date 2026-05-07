# Radar stratégique — pivots totaux (LegalCase mis en pause / abandonné)

> **Statut** : registre vivant des opportunités SaaS B2B où LegalCase serait **abandonné ou mis en pause** au profit d'un autre produit. Pas une roadmap, pas une feature au PRODUCT_SPEC.md.
> **Hors V1.** Engagement réversible interdit avant que les critères d'activation soient réunis (voir plus bas).

> **Périmètre** : ce fichier trace les **pivots de produit complets** — situations où le founder change d'aventure entrepreneuriale en gardant uniquement ses compétences. Distinct de `docs/radar-corporate-b2b.md` qui trace les **extensions du produit LegalCase** (le produit reste, la cible/scope évolue). Distinct de `docs/radar-business-model.md` qui trace la décision lifestyle vs levée seed.

## ⚠️ Garde-fou discipline — à relire avant chaque réflexion

Plus encore que pour les extensions de produit, la discipline est critique ici parce que le coût d'opportunité est radical : **un pivot total = 6-12 mois de retard** par rapport au produit en cours, plus le risque de jeter 6 mois de travail déjà capitalisés.

Les 3 questions critiques :

1. **Pourquoi explorer un nouveau produit alors que LegalCase n'a pas été validé auprès d'un seul client payant ?** Tracer une opportunité ne dispense pas de prouver le PMF du produit actuel. Si la réponse honnête est *"je doute du potentiel de LegalCase"*, l'action n'est PAS d'enrichir ce radar mais de mener 30 entretiens prospects en 30 jours pour vérifier le doute.
2. **Coder est-il devenu plus confortable que vendre ?** Aucun nouveau produit ne sauve l'évitement de la vente. Le problème n'est pas l'idée de produit mais le rapport au commercial. Forcer 90 jours de vente LegalCase avant tout pivot.
3. **Le seuil non-négociable** : aucun pivot total ne devient un sujet d'engagement réel avant que **LegalCase ait été validé OU clairement invalidé** sur 30 entretiens approfondis + 30-60 jours de tentative commerciale réelle. Sans cette validation, l'arbitrage est un caprice intellectuel — pas une décision business.

**Tracer ≠ pivoter.** Ce fichier accumule des hypothèses externes pour décision future, il ne décide pas et ne justifie aucune action de pivot maintenant.

## Pourquoi ce document existe

Au cours d'analyses stratégiques (mai 2026), des opportunités SaaS B2B ont été évoquées comme alternatives à LegalCase. Aucune ne réutilise le produit actuel à plus de 30 % — ce sont des **nouvelles entreprises** qui partagent uniquement le founder.

L'objectif de ce registre est de :
- Tracer ces opportunités pour ne pas les oublier si une réorientation devient nécessaire un jour
- **Empêcher** une décision impulsive de pivot avant que LegalCase ait été validé / invalidé proprement
- Documenter pour chaque pivot ses pré-requis, sa concurrence et son réalisme à partir de la situation actuelle (solo, tech, sans réseau enterprise, FR/EU, bootstrap)

## Critères d'activation — très stricts

Un pivot total ne devient un sujet de décision réelle que si **AU MOINS 3 des 4 critères suivants** sont réunis :

| # | Critère | Seuil |
|---|---|---|
| 1 | **LegalCase validé OU clairement invalidé** | ≥ 100 K€ ARR atteint OU 30 entretiens approfondis + 60 jours commercial sans aucun signal d'achat |
| 2 | **Signal externe entrant non sollicité** | ≥ 2 sources indépendantes (prospect, marché, partenaire potentiel) signalent l'opportunité **sans qu'on l'ait suggéré** |
| 3 | **Expertise ou réseau dans le domaine cible** | Connaissance métier > 6 mois OU ≥ 1 contact direct dans l'écosystème (DPO/DAF, RH paie, fonds VC vertical, éditeur juridique) |
| 4 | **Runway personnel ≥ 18 mois** | Capacité à survivre sans revenus pendant la phase de validation du nouveau produit (épargne ou levée préalable) |

Si moins de 3 critères réunis : **observation passive, pas de décision**.

Si 3-4 réunis : **étape de validation par 30 entretiens prospects** sur la nouvelle cible **avant** toute ligne de code.

## Registre des opportunités

Pour chaque pivot : périmètre, buyer, use case, % réutilisation stack, ARPU, concurrents, tendance VC, raisons d'écarter aujourd'hui.

### #1 — Compliance automation pour PME (RGPD + AI Act + DSA + DAC7)

| Critère | Détail |
|---|---|
| **Buyer** | DPO ou Directeur Juridique ou DAF (PME 10-250 salariés) |
| **Use case** | Registre RGPD, gestion sous-traitants, DPIA, registre IA (AI Act applicable août 2026), reporting DAC7, conformité DSA |
| **TAM théorique** | ~150 000 PME 10-250 salariés FR, ~3 M en Europe → 2-3 Md€ Europe |
| **Concurrents** | Dastra, Witik, Data Legal Drive (200-800 €/mois) |
| **Réutilisation stack LegalCase** | ~30 % (OCR, F-IA-03 cohérence, F-95 export PDF, F-69 délais) |
| **Pricing cible** | 100-500 €/mois TPE, 800-2000 € PME |
| **Cycle de vente** | 2-4 semaines (peur de la sanction = closing rapide) |
| **Décideur** | Unique (DPO) — vente courte |
| **Tendance VC** | Forte (TAM Md€, churn faible car obligation légale, AI Act drives urgence août 2026) |
| **Estimation revenus solo** | Y1 50-100 K€ ARR, Y2 250-400 K€, Y3 500-900 K€ |
| **Source** | Analyse Claude 2026-05-05 (Niveau 1 #1 + Option B.1) |
| **Pourquoi écarté aujourd'hui** | (a) Repart de zéro sur la connaissance métier (RGPD/AI Act/DSA/DAC7) ; (b) compétition installée (Dastra, Witik) ; (c) violente le critère #1 d'activation (LegalCase non validé) ; (d) 0 réseau dans l'écosystème compliance |

### #2 — Saisies sur salaire / oppositions tiers détenteur (ATD)

| Critère | Détail |
|---|---|
| **Buyer** | Responsable Paie / DRH PME (~30 000 entreprises 50-2000 salariés FR) + cabinets EC paie (~20 000) |
| **Use case** | Calcul automatique quotité saisissable, génération courriers Trésor public/huissier, calendrier versements, gestion ATD reçus en PDF |
| **TAM théorique** | ~50 000 cibles directes FR. Niche profonde mal couverte. |
| **Concurrents** | Sage, Cegid (mauvaise couverture), aucun acteur dominant |
| **Réutilisation stack LegalCase** | ~20 % (OCR + calcul barème — proche F-DT-29 crédit-temps) |
| **Pricing cible** | 200-800 €/mois selon taille |
| **Cycle de vente** | Court (acheteur unique Resp. Paie, ROI démontrable en 1 démo) |
| **Décideur** | Unique (Resp. Paie / DRH) — vente très courte |
| **Tendance VC** | Faible (niche pure, TAM trop étroit pour seed VC) — adapté lifestyle |
| **Estimation revenus solo** | Y1 80-150 K€ ARR, Y2 300-500 K€, Y3 600 K€-1 M€ |
| **Source** | Analyse Claude 2026-05-05 (Niveau 1 #2) |
| **Pourquoi écarté aujourd'hui** | (a) Connaissance métier paie nulle, courbe d'apprentissage 6-12 mois ; (b) erreurs de calcul = pénalités client = risque réputationnel violent en TPE ; (c) niche oui mais nécessite expertise paie certifiée ; (d) violente critère #1 |

### #3 — Vertical AI agent pour secteur réglementé (santé, finance, juridique)

| Critère | Détail |
|---|---|
| **Buyer** | Direction métier + DSI + Compliance + Direction Générale (multi-stakeholder lourd) |
| **Use case** | Agent autonome bout en bout — ex : instruction sinistre auto pour assureurs (analyse photo + devis + détection fraude + paiement) ; KYC/AML pour banques ; admission patient pour cliniques |
| **TAM théorique** | 10-50 Md€ par vertical |
| **Concurrents** | Acteurs financés (Harvey AI 500 M$, Hyperexponential 73 M$, etc.) |
| **Réutilisation stack LegalCase** | ~20 % (pipeline IA, certaines abstractions) — le reste est nouvelle architecture agent |
| **Pricing cible** | 5 000-50 000 €/mois par compte enterprise |
| **Cycle de vente** | 6-12 mois |
| **Décideur** | Multi-stakeholder (DSI + métier + Compliance + DG) |
| **Tendance VC** | Très forte (thèse VC #1 en 2026 : Sequoia, a16z, Index parient massivement sur les vertical AI agents) |
| **Estimation sortie** | 100 M€-1 Md€ |
| **Source** | Analyse Claude 2026-05-05 (Option B.2 + Niveau 2 #4) |
| **Pourquoi écarté aujourd'hui** | (a) Nécessite co-fondateur du métier ciblé (santé/finance/assurance) — solo non viable ; (b) intégrations lourdes (API métier, ERP, SIRH) hors compétence actuelle ; (c) cycle de vente long incompatible bootstrap ; (d) compétition très financée — entrer sans seed minimum 5 M€ = mort rapide |

### #4 — Infrastructure / API LLM pour acteurs établis (B2B2B "Plaid for legal tech")

| Critère | Détail |
|---|---|
| **Buyer** | Grands éditeurs juridiques (Lefebvre Sarrut, Lamy, LexisNexis, Wolters Kluwer, Doctrine) |
| **Use case** | API white-label / modules embedded dans leurs produits existants → leurs 50 000+ clients deviennent clients indirects |
| **TAM théorique** | 50-100 M€ EU mais 10 contrats à 200 K€-1 M€ = 10 M€ ARR concentré |
| **Concurrents** | Aucun pure-play infra sur ce vertical |
| **Réutilisation stack LegalCase** | ~60 % (le moteur d'analyse devient un service B2B2B) |
| **Pricing cible** | 200 K€-1 M€ par contrat / éditeur |
| **Cycle de vente** | 12-18 mois (négociation âpre, perte de 70 % de la valeur dans la marge éditeur) |
| **Décideur** | Comité éditeur + DSI éditeur + COMEX |
| **Tendance VC** | Faible à ce stade (revenu trop concentré, peu de logos, pas pitchable seed) |
| **Source** | Analyse Claude 2026-05-05 (Option B.3 + Niveau 2 #5) |
| **Pourquoi écarté aujourd'hui** | (a) Aucun réseau dans les éditeurs — les éditeurs ne signent pas avec un inconnu sans introduction ; (b) cycle 12-18 mois incompatible bootstrap solo ; (c) modèle de marge tué par l'intermédiation ; (d) nécessite déjà un produit éprouvé qu'ils peuvent labelliser |

### #5 — Outils due diligence / M&A pour fonds d'investissement et Big 4

| Critère | Détail |
|---|---|
| **Buyer** | Cabinets d'audit (Big 4), fonds d'investissement (PE/VC/Family Offices), 200 cabinets M&A |
| **Use case** | Analyse 200-500 documents juridiques pré-deal (clauses change of control, exclusivité, MAC clauses), comparaison base référence, génération rapport DD |
| **TAM théorique** | Top 100 fonds + Big 4 + 200 cabinets M&A = ~350 cibles ARPU 1500-5000 €/mois → 50-100 M€ EU |
| **Concurrents** | Acteurs établis (Kira Systems, Luminance), nouveaux entrants (Spellbook) |
| **Réutilisation stack LegalCase** | ~50 % (extraction clause + cohérence — proche du moteur LegalCase) |
| **Pricing cible** | 1500-5000 €/mois |
| **Cycle de vente** | 6 mois minimum |
| **Décideur** | Multi-stakeholder M&A team + Compliance |
| **Source** | Analyse Claude 2026-05-05 (Niveau 2 #6) |
| **Pourquoi écarté aujourd'hui** | (a) Très haute exigence qualité — un fonds ne tolère pas une erreur de DD à 100 M€ d'enjeu ; (b) vente enterprise nécessite équipe (cycle 6 mois min) ; (c) coût de switching faible — concurrents financés ; (d) marché très consolidé chez les leaders |

## Décisions différées (à NE PAS faire maintenant)

- **Ne pas** abandonner LegalCase sans avoir mené 30 entretiens prospects approfondis + 60 jours de vente réelle
- **Ne pas** créer un dépôt Git ou un site landing pour un de ces 5 pivots
- **Ne pas** acheter de domaine, ouvrir d'entité, faire de design pour un de ces 5 pivots
- **Ne pas** annoncer publiquement (LinkedIn / Village / etc.) une réflexion de pivot
- **Ne pas** ajouter de feature F-XXX au PRODUCT_SPEC.md alignée sur l'un de ces pivots

## Réévaluation

Ce document est revu **uniquement** quand :
- Un signal externe entrant non sollicité arrive sur l'un des 5 pivots (ajout d'une ligne dans la fiche concernée)
- LegalCase atteint un seuil franc : 30 K€ MRR (validation = on continue), OU 60 jours commercial sans aucun signal (invalidation = on rouvre la décision)

Pas de réévaluation prévue avant T+90 jours par défaut. Tracer ≠ rouvrir le débat.

## Lien avec les documents existants

- **`docs/PRODUCT_SPEC.md`** — aucun lien, par construction (PRODUCT_SPEC ne contient que des features LegalCase)
- **`docs/radar-corporate-b2b.md`** — extensions du produit (cible adjacente, scope élargi). Distinct de ce fichier (nouveaux produits)
- **`docs/radar-business-model.md`** — décision lifestyle vs levée seed (orthogonale au choix produit)
- **`docs/MARKETING_BACKLOG.md`** — aucune tâche M-XXX ne devrait référencer ces 5 pivots tant que les critères d'activation ne sont pas réunis
