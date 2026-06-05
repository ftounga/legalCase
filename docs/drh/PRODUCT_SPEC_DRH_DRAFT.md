# Fiche produit — LegalCase DRH (offre employeur) — DRAFT VIVANT

> **HYPOTHÈSE DE CADRAGE — hors backlog F-178.** Document vivant. MAJ 2026-06-06. Cible = marché
> DRH/employeur (D12). Invariant anti-conflit D8 (conformité/productivité, jamais 'contre les salariés').
> NE PAS confondre avec un ordre de build. Verrou : voir CADRAGE-STRATEGIQUE-DRH.md.

> **Statut : HYPOTHÈSE — hors backlog.** Exclu du `docs/PRODUCT_SPEC.md` live et du sync F-178 (D4).
> Cible de contenu = **marché DRH/employeur** (D12). LegalCase avocat = ancre de cohérence/format, pas plafond.
> Document **vivant** : appends justifiés (provenance + changelog), modif sur info marché/directive PO seulement,
> jamais de réécriture from-scratch. Invariants : D3 (dossier-centric), D7 (rôle = attribut workspace),
> D8 (anti-conflit : conformité/anticipation/productivité, jamais « gagner contre vos salariés »),
> « 1 outil décisionnel = 1 situation métier », corporate-readiness = features produit (D10), pas infra (D11).

---

## Maturité

| Axe | Score /100 |
|-----|-----------|
| marketCoverage | 90 |
| competitiveDifferentiation | 89 |
| legalGrounding | 92 |
| coherenceWithExisting | 91 |
| completeness | 87 |
| **overall** | **90** |

**Δ vs run précédent : 5** (85 → 90).

**Verdict : `continue`** — continuer la maturation. Reste à traiter (manques identifiés ce run) :

1. **Périmètre NET toujours non matérialisé** : appliedDeletions reste vide partout (≈30 proposedDeletions documentés D4 mais non appliqués). Tant que les doublons ne sont pas supprimés et que les 5-6 capacités transverses uniques (PLATFORM-04 pivot, CCN-aware F-DT-07, jurisprudence F-JU-01, famille audit logs F-38, cadre AI-ACT-01, moteur de scoring CHIFFRAGE-07, moteur d'arbitrage commun) ne sont pas matérialisées une seule fois, le compte affiché surévalue le périmètre réel et la complétude reste plafonnée.
2. **Doublons inter-domaines portés mais non consolidés** (portage = décision PO en attente) : PLATFORM-10⊂ONBOARD-02, ONBOARD-08/CHIFFRAGE-17/REQUAL-16⊂SCOPE-01, API-SIRH-17⊂SSO-06 (SCIM), API-SIRH-18⊂CORP-READY-17 (status page), SCOPE-04⊂CORP-READY (kit procurement), PLATFORM-05⊂API-SIRH-03. Fixer la feature de référence et replier les déclinaisons fait gagner cohérence + lisibilité.
3. **Transfert effectif des 2 tâches marketing** (AI-ACT-10 badge « AI Act-ready », API-SIRH-10 partenariat SIRH) vers MARKETING_BACKLOG via le contrôle de cohérence 4 points (CLAUDE.md règle 2) — toujours seulement signalées comme suppressions proposées du périmètre produit, jamais tracées côté marketing.
4. **Sujet UX « scoring/conclusions/métrage ⊂ outils CALCULÉS/persistés »** désormais explicite et propagé (CHIFFRAGE-07, INAPT-07, REQUAL-04, CSE-CONFORM-05, DASHBOARD-02, PRICING-04, ONBOARD-07, API-SIRH-14) mais NON tranché : décision PO requise sur les 3 options (alerte / pré-calcul auto / laisser tel quel) avant tout dev des scorings et du métrage facturable — bloque l'exécution.
5. **Validation terrain D8 manquante** : la coexistence des 2 grilles et le messaging « LegalCase Employeur » reposent sur l'invariant anti-conflit (réaction de 2-3 clients avocats à tester AVANT engagement, D8). Aucune feature ne porte ce test de perception ni le déclencheur de bascule produit séparé (≥3 signaux de défiance procurement).
6. **Seuils de déclenchement encore implicites pour le bundle corporate** (SSO/SCIM/CORP-READY/AUDIT-LOG/API-SIRH) : la plupart sont V9+ (≥50 clients, churn <5%, V1-V8 stable 3 mois) — la fiche gagnerait à matérialiser un ordonnancement GTM (long-lead-time certif ISO/SOC 2 vs gates d'achat AI Act provider) pour distinguer ce qui doit être lancé AVANT le 1er cycle grand compte de ce qui suit la traction.

---

## Décisions PO — arbitrage 2026-06-06

> Arbitrage du PO suite au run de maturation 3 (overall 90/100). Convertit les `proposedDeletions` et
> manques résiduels en **DÉCISIONS**. Tant que le verrou d'activation (radar corporate : 30 K€ MRR FR/BE
> **OU** 2 POC DRH payants **OU** intro DAF/DJ entreprise 200p) n'est pas franchi, ces décisions sont
> **tracées mais non exécutées** : suppressions et consolidations seront appliquées mécaniquement
> (`appliedDeletions` + matérialisation) au passage backlog. Aucune ne déclenche de dev (D4).

### A — Périmètre net (suppressions DÉCIDÉES, à appliquer au passage backlog)
- **A.1 — Doublons purs → SUPPRIMER** (référence survivante confirmée) : journal de contrôle humain par domaine → **AI-ACT-01** ; génération d'actes par domaine → domaine **ACTES** ; checklist CSE inaptitude → **CSE-CONFORM-02** ; calendaire disciplinaire SANCTION-03 → **SECU-PROC-03** ; export pré-avocat par domaine → **PREAVOCAT-01** ; portefeuille par domaine → **DASHBOARD-01**.
- **A.2 — Consolidations (feature de référence fixée, replier les déclinaisons)** : PLATFORM-10 → **ONBOARD-02** ; ONBOARD-08 / CHIFFRAGE-17 / REQUAL-16 → **SCOPE-01** ; API-SIRH-17 → **SSO-06** (SCIM) ; API-SIRH-18 → **CORP-READY-17** (status page) ; SCOPE-04 → **CORP-READY** (kit procurement) ; PLATFORM-05 → **API-SIRH-03**.

### B — Capacités transverses uniques (principe structurant ADOPTÉ)
Principe figé : **1 capacité = 1 implémentation de référence + N déclinaisons** (jamais N moteurs). Références : **PLATFORM-04** (lecture employeur de tous les F-DT) · **F-DT-07** (CCN-aware) · **F-JU-01** (jurisprudence via PLATFORM-04) · **F-38 / AUDIT-LOG-01** (audit logs) · **CHIFFRAGE-07** (scoring d'exposition) · **CHIFFRAGE-11** (provision IAS 37) · **moteur d'arbitrage commun** contester/transiger (paramètres procédure 2026 partagés). Toute mini-spec future réutilise ces références, n'en recrée aucune.

### C — Typologie marketing (contrôle de cohérence 4 points PASSÉ le 2026-06-06)
**AI-ACT-10** (badge « AI Act-ready ») et **API-SIRH-10** (co-marketing SIRH) = tâches MARKETING, **retirées du périmètre PRODUIT**. **Décision : NE PAS ajouter à `MARKETING_BACKLOG` maintenant** — verdict 4 points : séquence stratégique KO (DRH non activé). **Gelées jusqu'à l'activation DRH**, à re-soumettre au contrôle 4 points à ce moment-là.

### D — Sujet UX « scoring / métrage ⊂ outils CALCULÉS » (OUVERT — recommandation PO)
Impacte le métrage **facturable** (PRICING-04, API-SIRH-14) et l'invariant transverse côté produit avocat existant. **Recommandation : option « alerte avant génération »** (signaler quand un score / une conclusion ne s'appuie pas sur des outils calculés, sans forcer le pré-calcul — préserve le modèle « outils = simulateurs indépendants »). **Reste un sujet OUVERT** dépassant le périmètre DRH : à ratifier formellement (→ `docs/OPEN_QUESTIONS.md`) avant tout dev de scoring ou de facturation à l'usage.

### E — Cohérences éditoriales (RÈGLE appliquée)
- Échéances AI Act : **AI-ACT-13 = source unique** de la date de référence ; toute feature y renvoie (aucune date en dur).
- Scope juridiction : **SCOPE-01/02 = note canonique** ; les notes par domaine = simples renvois.

### F — Risque déontologique D8 (ACTION terrain — verrou avant engagement)
- **GO/NO-GO conditionné** : tester la réaction de 2-3 clients avocats à la coexistence avocat/employeur **AVANT** tout engagement (D8).
- PREAVOCAT-07/10/12/13 = **export borné/révocable uniquement** (jamais accès croisé inter-workspaces) + **smoke test d'isolation workspace** obligatoire avant tout merge.

### G — Pricing & GTM corporate (DIFFÉRÉ)
Affiner la grille **PRICING-01** + ordonnancer le bundle corporate (long-lead ISO/SOC 2 vs gates AI Act provider) — **à l'approche du verrou d'activation seulement**.

---

## 1) Plateforme & moteur réutilisés

### Domaine — Plateforme & moteur réutilisés (pipeline droit du travail) (`platform`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-01 | Authentification OAuth2 | Login Google + Microsoft, aucun mot de passe local. Réutilisé tel quel côté EMPLOYEUR ; SSO/MFA entreprise = domaine corporate-readiness (F-22/CORP-READY-09). | plateforme-reutilisee | Terminée |
| F-02 | Onboarding & workspace | Premier login → création user + nom workspace. Côté EMPLOYEUR accueille le type d'acteur (F-DRH-PLATFORM-01). | plateforme-reutilisee | Terminée |
| F-03 | Création de dossier | Titre, domaine (DROIT_DU_TRAVAIL), description. Côté EMPLOYEUR dossier centré-salarié (F-DRH-PLATFORM-03). | plateforme-reutilisee | Terminée |
| F-04 | Liste & consultation des dossiers | Dashboard liste paginée. Côté EMPLOYEUR base de la vue portefeuille (F-DRH-DASHBOARD-01), distincte. | plateforme-reutilisee | Terminée |
| F-05 | Upload de documents | Ajout fichiers, validation, S3. Porte d'entrée dossier-centric employeur. | plateforme-reutilisee | Terminée |
| F-52 | Upload multi-documents — sélection batch et soumission différée | Mode panier multi-sélection. Réutilisé tel quel (dépôt groupé dossier RH). | plateforme-reutilisee | Terminée |
| F-06 | Extraction de texte | Fichier → texte, document_extractions. Étape 2 pipeline dossier-centric. | plateforme-reutilisee | Terminée |
| F-07 | Chunking | Segmentation, document_chunks. Réutilisé tel quel. | plateforme-reutilisee | Terminée |
| F-08 | Analyse IA — chunk | Analyse chunk, chunk_analyses, async. Réutilisé tel quel. | plateforme-reutilisee | Terminée |
| F-09 | Analyse IA — document | Synthèse chunks, document_analyses, async. Réutilisé tel quel. | plateforme-reutilisee | Terminée |
| F-10 | Analyse IA — dossier | Synthèse globale, case_analyses. Côté EMPLOYEUR lue « mon risque ». Base de F-DRH-PREAVOCAT-01. | plateforme-reutilisee | Terminée |
| F-11 | Suivi des jobs asynchrones | analysis_jobs, RabbitMQ. Réutilisé tel quel. | plateforme-reutilisee | Terminée |
| F-28 | Scalabilité pipeline IA — résumés compacts | Truncation déterministe, input borné. Réutilisé tel quel. | plateforme-reutilisee | Terminée |
| F-30 | Parallélisme pipeline IA — concurrence RabbitMQ | Consumers concurrents. Réutilisé tel quel. | plateforme-reutilisee | Terminée |
| F-32 | Optimisation coût LLM — modèle adaptatif par étape | Modèle éco/qualité par étape. Réutilisé tel quel. | plateforme-reutilisee | Terminée |
| F-51 | Pipeline IA adaptatif — chunking conditionnel | Bypass chunking si tient en contexte. Réutilisé tel quel. | plateforme-reutilisee | Terminée |
| F-12 | Restitution de l'analyse | Affichage structuré synthèse. Côté EMPLOYEUR support « mon risque ». | plateforme-reutilisee | Terminée |
| F-31 | Écran dédié synthèse | /case-files/:id/synthesis + re-analyser. Réutilisé tel quel. | plateforme-reutilisee | Terminée |
| F-13 | Questions IA interactives | Questions complémentaires, ai_questions. Côté EMPLOYEUR répondant = RH. | plateforme-reutilisee | Terminée |
| F-14 | Réponses utilisateur & re-synthèse | ai_question_answers → synthèse enrichie. Répondant RH côté EMPLOYEUR. | plateforme-reutilisee | Terminée |
| F-33 | Limite de re-analyses par dossier | Gate billing ENRICHED_ANALYSIS. Quotas → pricing compte employeur (D9). | plateforme-reutilisee | Terminée |
| F-34 | Budget tokens mensuel par workspace | Plafond tokens/mois, alerte + blocage. Réutilisé tel quel. | plateforme-reutilisee | Terminée |
| F-35 | Chat libre sur dossier (RAG) | Q&A RAG. Côté EMPLOYEUR DRH interroge « mon risque ». | plateforme-reutilisee | Terminée |
| F-36 | Déclenchement manuel de l'analyse dossier | Bouton + gate billing. Réutilisé tel quel. | plateforme-reutilisee | Terminée |
| F-37 | Versioning des synthèses & audit trail | Versions v1/v2, badge Enrichie. Double usage preuve contrôle humain AI Act/SOC 2 (base de F-DRH-AI-ACT-01 + AUDIT-LOG-02). | plateforme-reutilisee | Terminée |
| F-38 | Suppression de documents & audit logs | Suppression + audit_logs (/workspace/audit-logs avec recherche+filtre). SOCLE de toute la famille audit logs (AUDIT-LOG-01..10, CORP-READY-10, AI-ACT-07). | plateforme-reutilisee | Terminée |
| F-DRH-PLATFORM-01 | Type d'acteur du workspace (AVOCAT vs EMPLOYEUR) — feature fondatrice | Attribut workspace fixé à l'onboarding (D7), jamais sélecteur bloquant. Détermine framing/packaging/visibilité outils/pricing, MÊME moteur lu côté employeur. Fonde la coexistence + isolation (D8). Feature fondatrice légitime, absente du périmètre avocat. | plateforme-reutilisee | Hypothèse |
| F-DRH-PLATFORM-02 | Comptes multi-utilisateurs RH (logique « compte » employeur) — recoupe le multi-utilisateur existant | Workspace employeur multi-RH avec rôles, logique compte (pas per-seat). ⚠️ Le multi-utilisateur workspace existe déjà côté plateforme (cadrage D7 : 'le multi-utilisateur existant couvre l'équipe RH'). VALEUR = rôles granulaires RH + pricing compte, pas un nouveau multi-tenant. Recadré. | plateforme-reutilisee | Hypothèse |
| F-DRH-PLATFORM-03 | Dossier centré-salarié (modèle de dossier RH) | Dossier employeur = salarié sujet, employeur partie (D7). Réutilise F-03 en lecture employeur. Ancrage des outils lus « mon risque ». Conservé. | droit-travail | Hypothèse |
| F-DRH-PLATFORM-04 | Lecture employeur du moteur d'outils décisionnels (« mon risque ») — capacité transverse PIVOT | Couche de présentation/visibilité re-cadrant les sorties des outils décisionnels existants (F-DT-*) côté employeur, SANS dupliquer (« 1 outil = 1 situation »). ⚠️ FEATURE PIVOT : c'est elle qui porte la 'lecture employeur' de TOUS les F-DT — toutes les features 'lecture employeur de F-DT-XX' (CHIFFRAGE-01..06, INAPT-05/06, REQUAL-CDD-01/02, DISCRIM-HARC-01/03, SECU-PROC-10, SANCTION-07...) sont des CONFIGURATIONS de cette capacité, pas des features distinctes. D7. | plateforme-reutilisee | Hypothèse |
| F-DRH-PLATFORM-05 | Pré-remplissage de dossier via connecteur SIRH (API) — recoupe API-SIRH-03 | ⚠️ Recoupe F-DRH-API-SIRH-03 (pré-remplissage contexte employé). À consolider : une seule feature pré-remplissage SIRH (le domaine API-SIRH la porte en détail). Réutilise F-05/F-06. | concurrent-gap | Hypothèse |
| F-DRH-PLATFORM-06 | Isolation stricte AVOCAT ↔ EMPLOYEUR dans la coexistence multi-buyer (invariant D8 plateforme) | Étanchéité de bout en bout entre workspaces AVOCAT et EMPLOYEUR partageant le même moteur : cloisonnement dossiers/pièces/sorties par workspace_id + type d'acteur, aucun croisement de données ni de visibilité. FONDATION de la coexistence multi-buyer ('attribut workspace = fondation de la coexistence'). Distinct de PLATFORM-01 (qui FIXE l'attribut) : ici on GARANTIT l'étanchéité. D8 : anti-conflit déontologique structurel. Réutilise le modèle multi-tenant existant. À valider par smoke test d'isolation workspace (préoccupation transversale CLAUDE.md). | plateforme-reutilisee | Hypothèse |
| F-DRH-PLATFORM-07 | Moteur de framing & packaging employeur (copy / visibilité outils / pricing) piloté par le type d'acteur | Mécanisme réutilisable qui applique, à partir du type d'acteur EMPLOYEUR (PLATFORM-01), le re-cadrage transverse : copy « mon risque »/conformité (jamais « gagner contre vos salariés », D8), visibilité/packaging des outils par offre, grille de pricing « compte » (D9). Distinct de PLATFORM-04 (qui re-cadre les SORTIES des outils) : ici le framing applicatif global (libellés, navigation, packaging, tarif). Réutilise la couche de visibilité/feature-flags existante. | concurrent-gap | Hypothèse |
| F-DRH-PLATFORM-08 | Traçabilité « factualisé depuis les pièces » sur toute sortie employeur (provenance dossier-centric) | Capacité plateforme qui tague chaque valeur affichée/calculée côté employeur (chiffrage, scoring, champ d'outil, acte) : extraite des pièces vs hypothèse vs « non factualisable ». Matérialise l'invariant produit F-246 (tout champ d'outil pré-rempli par l'IA, seule exception = info absente des pièces) côté employeur, visible/auditable. LE différenciant dossier-centric vs paramétriques (Jobexit, Predictice). Socle de confiance + preuve de contrôle humain AI Act. Réutilise F-37 (versioning/audit trail) + le pipeline d'extraction (F-06). | concurrent-gap | Hypothèse |
| F-DRH-PLATFORM-09 | Réutilisation du pipeline IA 3-niveaux (chunk → document → dossier) côté employeur — accélérateur time-to-market | Feature de cadrage : le pipeline IA asynchrone 3-niveaux et le moteur d'outils droit du travail existants (~90 % du build, D1) sont réutilisés tels quels côté employeur (D3). Aucune ré-implémentation du moteur (CLAUDE.md : ne pas réinventer stack/pipeline). VALEUR = time-to-market ; s'appuie sur F-28/F-30/F-32/F-51 (pipeline, concurrence RabbitMQ, modèle adaptatif, chunking) déjà en prod. Ancre la cohérence « même moteur » et borne le périmètre build DRH au framing + lectures employeur + corporate-readiness. | plateforme-reutilisee | Hypothèse |
| F-DRH-PLATFORM-10 | Parcours d'activation EMPLOYEUR (premier dossier centré-salarié, réduction de friction) | Parcours guidé post-onboarding spécifique à l'acteur EMPLOYEUR (workspace → premier dossier centré-salarié → pièces → pipeline → première lecture « mon risque »). Distinct de PLATFORM-01 (attribut) et de F-02 (onboarding générique). ⚠️ DOUBLON inter-domaines : recoupe massivement le domaine ONBOARD (ONBOARD-02 assistant premier dossier + ONBOARD-06 friction d'activation). À consolider : le PARCOURS d'activation employeur est porté par le domaine ONBOARD (feature de référence ONBOARD-02) ; PLATFORM ne porte que la réutilisation des briques F-02/F-03/F-05/F-10. Sujet UX non tranché : pas de pré-calcul forcé des outils (mémoire scoring ⊂ outils calculés). Statut Hypothèse, seuil dev UX ≥3 signaux. | vision-po | Hypothèse |
| F-DRH-PLATFORM-11 | Métrage d'usage employeur (usage_events) — substrat du packaging/pricing compte (D9) | Capacité plateforme agrégeant par workspace EMPLOYEUR les événements d'usage (dossiers traités, re-analyses, tokens, actes, appels API-SIRH) pour alimenter le packaging/pricing compte. NE FIXE PAS la grille (domaine PRICING). Réutilise usage_events (F-33/F-34/F-257) + quota API-SIRH-14. ⚠️ Recoupe API-SIRH-14 (métrage API) et PRICING-04/05 (composante variable) : c'est le SUBSTRAT plateforme transverse, à articuler sans dupliquer les compteurs API-SIRH-14. | vision-po | Hypothèse |
| F-DRH-PLATFORM-12 | Traitement RGPD-conforme des pièces sensibles salariés dans le pipeline (Art. 28/32) — capacité plateforme | Garde-fous plateforme (chiffrement repos/transit Art. 32, encadrement sous-traitants Art. 28, minimisation, durées CNIL RH, finalité « gestion des contentieux et précontentieux ») appliqués au pipeline IA traitant des pièces salariés. Capacité PRODUIT plateforme (D10), distincte du livrable client CORP-READY (DPA self-serve / sous-processeurs géolocalisés / kit DPO). Réutilise F-38 + multi-tenant + gate F-257. ⚠️ Recoupe CORP-READY-12/04 et AUDIT-LOG-17 (certificat de purge) : ici la BRIQUE pipeline-conforme, pas le livrable opposable client. | marche | Hypothèse |

**Ajoutées ce run (APPEND 2026-06-06 — run de maturation) :** F-DRH-PLATFORM-10, -11, -12.
- `F-DRH-PLATFORM-10` — Marqué doublon inter-domaines avec ONBOARD-02/06 (le domaine ONBOARD porte le parcours d'activation). Conservée comme cadrage plateforme mais à consolider sous ONBOARD ; non supprimée (situation de cadrage légitime, décision de portage = PO).
- `F-DRH-PLATFORM-11` — Précision anti-doublon : substrat de métrage transverse distinct du métrage API (API-SIRH-14, qu'il consomme) et de la grille tarifaire (PRICING-04). Conservée.
- `F-DRH-PLATFORM-12` — Anti-doublon explicité avec CORP-READY (livrable client) et AUDIT-LOG-17 (preuve de purge). Conservée — la conformité par construction du pipeline est une capacité distincte légitime.

**Modifiées / justifiées (curation 2026-06-06) :**
- `F-DRH-PLATFORM-10` — Marqué doublon inter-domaines avec ONBOARD-02/06 : le parcours d'activation employeur est porté par le domaine ONBOARD. Conservé comme cadrage plateforme, portage à consolider (décision PO).
- `F-02` — Cross-référence conservée vers PLATFORM-10/ONBOARD-02 ; F-02 reste l'onboarding générique du workspace (statut Terminée, scope inchangé).
- `F-37` — Cross-référence conservée vers PLATFORM-08 + AUDIT-LOG-02 + AI-ACT-01 (F-37 = base technique versioning/audit trail). Statut Terminée inchangé.

**Suppressions proposées (2026-06-06, non appliquées — appliedDeletions vide, D4) :**
- `F-DRH-PLATFORM-05` — Recoupe F-DRH-API-SIRH-03 (pré-remplissage contexte employé via SIRH). Une seule feature de pré-remplissage SIRH, portée par le domaine API-SIRH (API-SIRH-03). Non appliquée (D4 — décision de portage PO) ; appliedDeletions vide.

**Ajoutées ce run (APPEND 2026-06-05) :** F-DRH-PLATFORM-06, -07, -08, -09.
- `F-DRH-PLATFORM-06` — Conservée — l'isolation multi-buyer est la fondation nommée par le driver marché et l'invariant D8 ; PLATFORM-01 fixe l'attribut sans garantir l'étanchéité. Cohérent avec le modèle multi-tenant existant (PRODUCT_SPEC).
- `F-DRH-PLATFORM-07` — Conservée — différenciant 'même moteur reframé côté employeur' ; PLATFORM-01 décide l'attribut, PLATFORM-04 re-cadre les sorties, aucune feature ne portait le mécanisme transverse de framing/packaging/pricing.
- `F-DRH-PLATFORM-08` — Conservée — F-246 (mémoire/décision PO) existe côté produit mais n'était pas portée comme capacité employeur traçable/auditable. Cohérent avec l'invariant 'tout champ pré-rempli' du projet.
- `F-DRH-PLATFORM-09` — Conservée — driver marché n°1 (réutilisation ~90 % = accélérateur time-to-market) non porté par une feature de cadrage ; cohérent avec D1/D3 et la règle CLAUDE.md anti-réinvention.

**Modifiées / justifiées (curation) :**

- `F-01` — Statut réel rétabli (Terminée).
- `F-02` — Statut réel rétabli.
- `F-03` — Statut réel rétabli.
- `F-04` — Statut réel rétabli ; lien DASHBOARD-01.
- `F-05` — Statut réel rétabli.
- `F-52` — Statut réel rétabli.
- `F-06` — Statut réel rétabli.
- `F-07` — Statut réel rétabli.
- `F-08` — Statut réel rétabli.
- `F-09` — Statut réel rétabli.
- `F-10` — Statut réel rétabli ; lien PREAVOCAT-01.
- `F-11` — Statut réel rétabli.
- `F-28` — Statut réel rétabli.
- `F-30` — Statut réel rétabli.
- `F-32` — Statut réel rétabli.
- `F-51` — Statut réel rétabli.
- `F-12` — Statut réel rétabli.
- `F-31` — Statut réel rétabli.
- `F-13` — Statut réel rétabli.
- `F-14` — Statut réel rétabli.
- `F-33` — Statut réel rétabli.
- `F-34` — Statut réel rétabli.
- `F-35` — Statut réel rétabli.
- `F-36` — Statut réel rétabli.
- `F-37` — Statut réel rétabli ; base AI-ACT-01/AUDIT-LOG-02.
- `F-38` — Statut réel rétabli ; SOCLE famille audit logs (a déjà recherche/filtre).
- `F-DRH-PLATFORM-02` — Recadré : multi-utilisateur déjà existant (D7) ; provenance plateforme-reutilisee.
- `F-DRH-PLATFORM-04` — Désignée FEATURE PIVOT D7 absorbant les 'lecture employeur de F-DT-XX'.
- `F-DRH-PLATFORM-05` — Recoupement API-SIRH-03 signalé.

---

### Domaine — Déclaration de périmètre juridiction (V1=FR seul, BE=backlog différé) (`SCOPE`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-DRH-SCOPE-01 | Note de scope juridiction figée — V1 = droit du travail FR seul | Énoncé normatif unique, opposable et versionné : la V1 DRH couvre EXCLUSIVEMENT le droit du travail français (CPH, barème Macron, CNIL/référentiel RH, France Travail, CSE, conventions collectives FR). Référence canonique citée par toutes les autres features et reflétée dans le copy (D8). Cohérent avec « La V1 cible le droit du travail uniquement » (CLAUDE.md). Réutilise le moteur FR existant (D1). | vision-po | Hypothèse |
| F-DRH-SCOPE-02 | Statut backlog différé Belgique — couverture exhaustive du droit social belge attendue (PAS un miroir FR) | Statue la Belgique en BACKLOG DIFFÉRÉ explicite : couverture EXHAUSTIVE du droit social belge depuis les sources BE, jamais un miroir FR (invariant mémoire belgique_never_forget). Bloque tout engagement commercial BE V1. Verrou de déclenchement aligné sur le radar OHADA/BE (observation passive jusqu'à seuil MRR). | vision-po | Hypothèse |
| F-DRH-SCOPE-03 | Signalement procédural des limitations de périmètre à chaque feature (garde-fou anti-scope-creep) | Mécanisme transverse apposant sur toute sortie potentiellement hors V1 (juridiction ≠ FR, domaine ≠ droit du travail) un signalement explicite des limites, plutôt qu'un résultat trompeur. Réutilise la couche de visibilité/feature-flags + le tagging de provenance PLATFORM-08 — à BRANCHER dessus, PAS réimplémenter. D8 (transparence). | marche | Hypothèse |
| F-DRH-SCOPE-04 | Clause contractuelle de couverture géographique initiale (FR uniquement) exposée au client | Clause CGU/onboarding du compte employeur = couverture géographique initiale France uniquement (aligne SCOPE-01). Point de procurement (D10) : borne de périmètre opposable. S'appuie sur le framing/packaging PLATFORM-07 ; ⚠️ recoupe CORP-READY (kit procurement/Trust Center) sur l'angle « engagement contractuel exposé » — à articuler comme volet juridiction du kit, pas un livrable procurement distinct. | marche | Hypothèse |
| F-DRH-SCOPE-05 | Garde-fou anti-fausse-exhaustivité Belgique sur les sorties jurisprudence/citations | Borne les sorties du moteur jurisprudence (F-JU-01, lu employeur via PLATFORM-04) à la juridiction FR (Cassation FR) tant que le backlog BE n'est pas livré, conformément au verdict figé mémoire reference_be_jurisprudence_sources (web_search BE parké : source non fiable, ne JAMAIS citer BE avec autorité). Réutilise F-JU-01 borné ; pas de second moteur. D8. | concurrent-gap | Hypothèse |

**Domaine créé ce run (APPEND 2026-06-06 — run de maturation) :** F-DRH-SCOPE-01→05. Comble la directive PO « scopeBE » : statuer explicitement le périmètre juridiction (V1 = FR seul ; BE = backlog différé, couverture exhaustive depuis sources BE, PAS un miroir FR). SCOPE-01/02 = note de scope CANONIQUE ; les notes par domaine (ONBOARD-08, CHIFFRAGE-17, REQUAL-CDD-16, SECU-PROC-15 marqueur, SANCTION-12 marqueur) sont des déclinaisons/renvois à consolider sous SCOPE. Toutes decisionTool=false. Aucune suppression (domaine nouveau).

---

### Domaine — Onboarding & activation de l'acteur EMPLOYEUR (`ONBOARD`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-DRH-ONBOARD-01 | Parcours de création de workspace EMPLOYEUR (sélection du type d'acteur à l'onboarding, non bloquante) | Étend F-02 avec l'étape de choix du type d'acteur (EMPLOYEUR vs AVOCAT) à la création du workspace : FIXE l'attribut PLATFORM-01 (D7), ni rejouable ni bloquant (pré-sélection contextuelle, défaut sûr). Porte le PARCOURS UX de sélection, distinct de PLATFORM-01 (stockage/exploitation de l'attribut). Réutilise F-01 ; SSO/MFA = CORP-READY-09/F-22. ⚠️ Recoupe PLATFORM-10 (parcours d'activation) — ONBOARD-01 = étape de SÉLECTION D'ACTEUR, distincte de l'activation premier-dossier (ONBOARD-02). | vision-po | Hypothèse |
| F-DRH-ONBOARD-02 | Assistant de premier dossier centré-salarié (guided first-case wizard employeur) — feature de référence activation | Parcours guidé EMPLOYEUR de l'arrivée au premier dossier exploitable (situation employeur → dossier centré-salarié F-03/PLATFORM-03 → upload F-05/F-06 → pipeline PLATFORM-09 → lecture « mon risque » PLATFORM-04). Feature de référence du parcours d'activation employeur : PLATFORM-10 en est le cadrage plateforme. NE duplique aucun outil : orchestre le pattern dossier-centric existant (D3). Réduit la friction terrain (Renversez/Mengue). D8. | vision-po | Hypothèse |
| F-DRH-ONBOARD-03 | Kit de démarrage employeur : dossier-modèle + pièces d'exemple (sample case) | Dossier-modèle anonymisé + pièces d'exemple au premier login EMPLOYEUR pour montrer le pattern complet upload → pipeline → outil « mon risque » → acte, sans vrai dossier. Réutilise PLATFORM-09 + PLATFORM-04 en démo isolée. Marqué « exemple », n'altère ni le métrage réel (API-SIRH-14 / usage_events F-33/F-257) ni le dashboard (DASHBOARD-01). Réduit le temps-jusqu'à-valeur. | concurrent-gap | Hypothèse |
| F-DRH-ONBOARD-04 | Configuration guidée des rôles RH à l'activation (DRH, manager, assistant RH, lecteur direction) | Étape d'onboarding d'invitation + attribution des rôles RH granulaires, optionnelle/non bloquante (DRH solo possible). S'APPUIE sur PLATFORM-02 (rôles RH) + le multi-utilisateur workspace existant (D7) — NE recrée PAS le modèle de rôles : porte seulement le PARCOURS d'invitation/attribution. D7 : pas de mur de configuration. | marche | Hypothèse |
| F-DRH-ONBOARD-05 | Activation des capacités d'usage à l'onboarding (visibilité quota d'analyses, pricing, connecteur SIRH) | Rend visibles/activables au démarrage : quota du palier, grille de pricing « compte » (D9), amorce optionnelle du pré-remplissage SIRH. S'APPUIE sur usage_events (F-33/F-257), quota API-SIRH-14 et framing PLATFORM-07 — NE recrée NI le métrage NI la grille (PRICING + API-SIRH). Porte la SURFACE d'onboarding. Le pré-remplissage SIRH renvoie à PLATFORM-05/API-SIRH-03. | corporate-readiness | Hypothèse |
| F-DRH-ONBOARD-06 | Réduction de la friction d'activation employeur : empty-states guidés et prochaine action recommandée | Empty-states guidés + prochaine action recommandée sur chaque écran clé d'un workspace EMPLOYEUR peu avancé (dashboard DASHBOARD-01, dossier sans pièces, analyse non lancée). Couche de guidage UX réutilisant les écrans existants ; aucun nouvel outil décisionnel. D8. ⚠️ Dev UX généraliste = seuil ≥3 signaux convergents (règle mémoire) ; 2 signaux nommés (Renversez/Mengue) + driver churn → statut Hypothèse, à confirmer terrain avant build. | marche | Hypothèse |
| F-DRH-ONBOARD-07 | Métrage visible de l'activation (jalons time-to-value : 1er dossier, 1ère analyse, 1er acte) | Jauge d'activation des jalons (workspace → 1er dossier → 1ères pièces → 1ère analyse → 1er outil « mon risque » → 1er acte). S'APPUIE sur usage_events (F-33/F-257) — NE recrée PAS le métrage. Distinct de DASHBOARD-01 (risque métier) : ici jalons d'ADOPTION produit. ⚠️ SUJET UX NON TRANCHÉ (à arbitrer avant dev) : « 1er outil consulté » compte-t-il l'outil pré-rempli non cliqué ou seulement l'outil CALCULÉ/persisté ? Même question que CHIFFRAGE-07/scorings — ne pas résoudre en silence (mémoire conclusions ⊂ outils calculés). | marche | Hypothèse |
| F-DRH-ONBOARD-08 | Note de scope juridiction de l'onboarding employeur : V1 = FR seul, BE différé (backlog) | Verrou de périmètre : le parcours d'onboarding/activation EMPLOYEUR cible la France seule en V1 (CPH, barème Macron, CNIL/référentiel RH, France Travail). BE explicitement DIFFÉRÉ au backlog (couverture exhaustive depuis sources BE, PAS un miroir FR). ⚠️ DOUBLON avec SCOPE-01/SCOPE-04 : la note de scope juridiction est portée par le domaine SCOPE (feature de référence SCOPE-01) ; ONBOARD n'en applique que la déclinaison parcours. À consolider. | vision-po | Hypothèse |

**Domaine créé ce run (APPEND 2026-06-06 — run de maturation) :** F-DRH-ONBOARD-01→08. Comble la directive PO « onboarding » (sélection du type d'acteur D7 ; parcours premier dossier centré-salarié ; réduction de friction d'activation Renversez/Mengue).
- `F-DRH-ONBOARD-02` — Désignée feature de référence du parcours d'activation employeur ; PLATFORM-10 en devient le cadrage plateforme (anti-doublon).
- `F-DRH-ONBOARD-08` — Marqué doublon de SCOPE-01 (note de scope juridiction canonique) ; conservé comme déclinaison parcours, portage de la note = domaine SCOPE.
- ⚠️ Sujets UX : ONBOARD-06 (seuil dev UX ≥3 signaux) et ONBOARD-07 (« 1er outil consulté » ⊂ outils calculés/persistés — non tranché) à arbitrer avant dev. Aucune suppression (domaine nouveau).

---

## 2) Situations employeur

### Domaine — Chiffrage de l'exposition prud'homale & indemnités (situation-employeur) (`CHIFFRAGE`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-DRH-CHIFFRAGE-01 | Lecture employeur du barème Macron / exposition licenciement sans cause réelle (réutilise F-DT-01 + F-DT-09) | NE CRÉE PAS un nouveau simulateur : LIT côté employeur (« mon exposition ») le moteur existant F-DT-01 (CompensationCalculator barème Macron) + F-DT-09 (comparateur jurisprudentiel d'indemnités), via la couche F-DRH-PLATFORM-04. Le pipeline IA dossier-centric extrait déjà ancienneté/effectif/salaire de référence. Différenciant vs Case Law Analytics/Predictice (paramétriques) : on part des pièces — capacité déjà acquise côté avocat. D8 : conformité/anticipation. | plateforme-reutilisee | Hypothèse |
| F-DRH-CHIFFRAGE-02 | Lecture employeur de la détection de nullité écartant le barème (réutilise F-DT-16 + F-DT-36) | LIT côté employeur le moteur existant F-DT-16 (licenciement nul — détection des 7 protections : discrimination, harcèlement, AT/MP, maternité, lanceur d'alerte, mandat syndical, action en justice) + F-DT-36 (vices de procédure). Alerte « exposition non plafonnée + réintégration possible » côté employeur. Aucun nouveau détecteur à construire — F-DT-16 le fait déjà. D8 : conformité/anticipation, pas armer contre le salarié. | plateforme-reutilisee | Hypothèse |
| F-DRH-CHIFFRAGE-03 | Lecture employeur du calculateur d'indemnités de rupture (réutilise F-DT-01/07/15/25/26) | LIT côté employeur les calculateurs existants : F-DT-01 (indemnité licenciement), F-DT-25 (préavis FR), F-DT-26 (congés payés), F-DT-07 (barème conventionnel/CCN), F-DT-15 (indemnité spéciale inaptitude). Le CCN-aware existe déjà dans F-DT-07. Aucun nouveau calculateur. D8 : « le dû certain de la rupture » lu côté employeur. | plateforme-reutilisee | Hypothèse |
| F-DRH-CHIFFRAGE-04 | Lecture employeur du chiffrage de transaction / départ négocié (réutilise F-DT-31) | LIT côté employeur F-DT-31 (transaction/protocole transactionnel, 5 critères Cass. 16/12/2010). La borne basse (sécurité juridique) ↔ borne haute (exposition CPH via F-DT-01) est une RESTITUTION orientée employeur des outils existants, pas un nouveau moteur. D8 : sécuriser un accord équitable. | plateforme-reutilisee | Hypothèse |
| F-DRH-CHIFFRAGE-05 | Contrôle du régime social et fiscal des indemnités de rupture (seuil 2 PASS) — GAP RÉEL | Contrôle le régime social/fiscal de toute indemnité chiffrée : exonération de cotisations jusqu'à 2 PASS (≈ 94 200 €), assujettissement au-delà, CSG/CRDS, IR. Vérifié contre le catalogue F-DT : AUCUN outil existant ne traite le net-à-payer / régime social-fiscal → situation métier nouvelle légitime (cohérence avocat ET employeur). À cadrer comme outil transverse aux deux acteurs, pas comme outil employeur-only. | droit-travail | Hypothèse |
| F-DRH-CHIFFRAGE-06 | Lecture employeur du simulateur de réintégration / D-I cas de nullité (réutilise F-DT-16 + F-DT-30) | LIT côté employeur le chiffrage réintégration déjà porté par F-DT-16 (licenciement nul → salaire d'éviction, D&I) et F-DT-30 (représentants du personnel : indemnité ≥ 6 mois + salaire éviction). Aucun nouveau calculateur de réintégration. D8 : mesure du risque maximal côté employeur. | plateforme-reutilisee | Hypothèse |
| F-DRH-CHIFFRAGE-07 | Scoring d'exposition consolidé d'un dossier (orchestration des outils calculés) — situation nouvelle | Agrège les SORTIES des outils existants (F-DT-01/09/16/31…) en un score d'exposition unique : motif + procédure + chiffrage + probabilité CPH. N'est PAS un nouveau calcul de fond mais un orchestrateur. ⚠️ Invariant interne (memory project_coherence_conclusions_outils_non_calcules) : le score doit s'appuyer UNIQUEMENT sur les outils CALCULÉS/persistés, pas sur les champs pré-remplis non cliqués. Gap réel : aucun outil ne consolide aujourd'hui. | concurrent-gap | Hypothèse |
| F-DRH-CHIFFRAGE-08 | Tableau de bord du risque social consolidé (portefeuille d'exposition agrégée) | Vue portefeuille agrégeant l'exposition de tous les dossiers du workspace employeur. ⚠️ DOUBLON inter-domaines : strictement identique à F-DRH-DASHBOARD-01/03. À traiter dans le domaine DASHBOARD (situation-employeur), pas dupliqué ici. Inexistant chez les acteurs unitaires. | concurrent-gap | Hypothèse |
| F-DRH-CHIFFRAGE-09 | Calculateur de ROI / provision opposable au procurement | ROI chiffré opposable aux Achats à partir du chiffrage réel + repères marché. ⚠️ DOUBLON avec F-DRH-DASHBOARD-06 (ROI procurement) et F-DRH-PREAVOCAT-04 (chiffrage interne provision). À fusionner sous une seule feature ROI. | marche | Hypothèse |
| F-DRH-CHIFFRAGE-10 | Journal de contrôle humain du chiffrage (AI Act déployeur, haut risque) | Journalise le contrôle humain sur les chiffrages liés au licenciement. ⚠️ DOUBLON avec le domaine AI-ACT transverse (F-DRH-AI-ACT-01/06). À porter par AI-ACT (cadre transverse) plutôt que par chaque domaine. Échéance haut-risque glissée à déc. 2027 (Digital Omnibus). | corporate-readiness | Hypothèse |
| F-DRH-CHIFFRAGE-11 | Fiche de provision IAS 37 du contentieux social (input DAF, datée/signée) | Produit la fiche de provision comptable du litige social conforme IAS 37 : probabilité de condamnation × montant estimé (scoring CHIFFRAGE-07 + chiffrages F-DT-01/09/16/31) + frais, datée et signable, réévaluable à chaque clôture. LIVRABLE attendu par le DAF/DAS, distinct du ROI procurement (CHIFFRAGE-09/DASHBOARD-06). Réutilise F-DT-04 (export PDF) + F-37 (versioning/audit trail). ⚠️ FICHE DE PROVISION TRANSVERSE : DASHBOARD-09 (consolidé + unitaire), INAPT-13, SANCTION-12, DISCRIM-HARC-15 en sont des DÉCLINAISONS — cette feature est le générateur de référence de la fiche unitaire ; à articuler pour ne PAS multiplier les moteurs (1 générateur, N configurations de scénario). D8 : conformité comptable. | marche | Hypothèse |
| F-DRH-CHIFFRAGE-12 | Aide à la décision contester / transiger (intègre contribution de saisine CPH ~50 € et risque d'appel) | Outil décisionnel d'arbitrage contester ↔ transiger pour l'employeur défendeur : scénario 'CPH' (exposition F-DT-01/09/16, contribution de saisine CPH ~50 €, durée ~13,7 mois, ~67 % de risque d'appel, honoraires ≥ 4 500 € HT) vs 'transiger' (borne F-DT-31 + régime social-fiscal 2 PASS via CHIFFRAGE-05). Distinct de F-DT-03 (prescriptions) et du scoring CHIFFRAGE-07. ⚠️ Même pattern d'arbitrage que SANCTION-11 (sanction contestée), REQUAL-CDD-11 (requalification) et PREAVOCAT-08 (volet transaction) : situations métier DISTINCTES (objet ≠), à garder séparées mais aligner sur un moteur d'arbitrage commun. Réutilise les chiffrages en orchestration. D8. | concurrent-gap | Hypothèse |
| F-DRH-CHIFFRAGE-13 | Restitution multi-assiette du chiffrage (brut / coût employeur / net / net-après-impôt) | Restitue tout montant chiffré (F-DT-01/07/15/25/26, transaction F-DT-31, réintégration F-DT-16/30) en 4 assiettes : brut, coût employeur (≈ +30 % charges au-delà des seuils), net salarié, net-après-impôt — en branchant CHIFFRAGE-05 (régime social-fiscal 2 PASS, CSG/CRDS, IR) + différé ARE. Parité avec le différenciant Jobexit, ancré dossier-centric (lecture via PLATFORM-04 sur les chiffrages calculés). Couche de restitution, pas un nouveau moteur de calcul. | concurrent-gap | Hypothèse |
| F-DRH-CHIFFRAGE-14 | Traçabilité et fiabilité opposable du chiffrage (paramètres sourcés, valeur de référence) | Rend chaque chiffrage opposable/auditable : paramètres retenus (ancienneté, salaire de réf., effectif, CCN via F-DT-07, plafonds Macron), leur source dans les pièces extraites + base de droit, versioning daté (F-37) et journal. Atteint la barre de confiance Jobexit (montant brut garanti). Pré-requis de CHIFFRAGE-11 (provision) et CHIFFRAGE-12 (arbitrage). ⚠️ Recoupe la capacité plateforme PLATFORM-08 (provenance « factualisé depuis les pièces ») : à traiter comme déclinaison chiffrage de PLATFORM-08, PAS un second moteur de traçabilité ; renvoie le journal de contrôle humain au cadre transverse AI-ACT. Réutilise F-37 + F-38. D8. | concurrent-gap | Hypothèse |

| F-DRH-CHIFFRAGE-15 | Réévaluation de la provision IAS 37 à chaque clôture comptable (workflow daté, alerte de re-mesure) | Workflow de re-mesure de la provision à chaque clôture (IAS 37) : rappel à date configurable, recalcul depuis scoring/chiffrages courants, écart vs provision précédente (reprise/dotation), historique versionné (F-37). Distincte de CHIFFRAGE-11 (générateur de fiche ponctuelle) : ici le CYCLE DE VIE de la provision. Trou réel (le DAS le fait à la main). D8 : conformité comptable. | marche | Hypothèse |
| F-DRH-CHIFFRAGE-16 | Chiffrage du différé d'indemnisation ARE / impact France Travail de la rupture (parité Jobexit) | Estime le différé d'indemnisation ARE applicable au salarié (différé lié aux indemnités supra-légales / RC, plafonné) et le replace dans le calendrier de la rupture. Différenciant Jobexit explicite ; vérifié non couvert par F-DT (F-DT-32 = documents de fin de contrat, pas le calcul du différé). Alimente l'arbitrage CHIFFRAGE-12 (le différé pèse sur l'acceptabilité d'une transaction), distinct de la restitution CHIFFRAGE-13 et du régime social-fiscal CHIFFRAGE-05. Périmètre FR seul (France Travail), cf. CHIFFRAGE-17. D8. | concurrent-gap | Hypothèse |
| F-DRH-CHIFFRAGE-17 | Note de scope juridiction du chiffrage : V1 = FR seul (CPH, barème Macron, CNIL, France Travail) ; BE différé au backlog | Verrou de périmètre : TOUS les chiffrages d'exposition de ce domaine ciblent la France V1 (CPH, barème Macron L.1235-3, contribution saisine CPH, régime social-fiscal 2 PASS, différé ARE/France Travail). BE = backlog différé (couverture exhaustive depuis sources BE, PAS un miroir FR). ⚠️ Déclinaison de la note canonique SCOPE-01 appliquée au chiffrage (borne CHIFFRAGE-01→16) — à consolider sous le domaine SCOPE. | vision-po | Hypothèse |

**Ajoutées ce run (APPEND 2026-06-06 — run de maturation) :** F-DRH-CHIFFRAGE-15, -16, -17.
- `F-DRH-CHIFFRAGE-15` — Conservée — workflow de re-mesure à chaque clôture (IAS 37), cycle de vie distinct du générateur de fiche ponctuelle CHIFFRAGE-11. decisionTool=false.
- `F-DRH-CHIFFRAGE-16` — Conservée — différé ARE/France Travail, différenciant Jobexit, non couvert par F-DT. decisionTool=true.
- `F-DRH-CHIFFRAGE-17` — Conservée — note de scope juridiction (déclinaison de SCOPE-01), portage à consolider sous le domaine SCOPE. decisionTool=false.

**Modifiées / justifiées (curation 2026-06-06) :**
- `F-DRH-CHIFFRAGE-07` — Confirmé moteur de scoring de référence (toutes les déclinaisons par domaine s'y rattachent). Sujet UX non tranché maintenu explicite avec 3 options ; décision PO requise.
- `F-DRH-CHIFFRAGE-13` — Différé ARE extrait vers CHIFFRAGE-16 (situation métier distincte = trésorerie/calendrier France Travail). 1 situation = 1 feature.
- `F-DRH-CHIFFRAGE-11` — Recadrage : workflow de réévaluation à chaque clôture sorti vers CHIFFRAGE-15. CHIFFRAGE-11 reste le générateur de fiche ponctuelle de référence.
- `F-DRH-CHIFFRAGE-17` — Rattaché à SCOPE-01 (note de scope juridiction canonique) ; déclinaison chiffrage, portage = domaine SCOPE.
- `F-DRH-CHIFFRAGE-10` — Correction conformité AI Act : employeur privé = DÉPLOYEUR (Annexe III.4, contrôle humain + info salariés + CSE) ; FRIA Art. 27 = secteur public uniquement. Renvoi au cadre transverse AI-ACT maintenu (proposedDeletion).

**Suppressions proposées (2026-06-06, non appliquées — appliedDeletions vide, D4) :**
- `F-DRH-CHIFFRAGE-08` — Redondant avec F-DRH-DASHBOARD-01/03 (portefeuille d'exposition agrégée). Référence unique = DASHBOARD-01. Non appliqué (D4 — touche le périmètre DASHBOARD, décision PO) ; appliedDeletions vide.
- `F-DRH-CHIFFRAGE-09` — Doublon de DASHBOARD-06 (ROI procurement, feature de référence) + PREAVOCAT-04. Une seule feature ROI de référence (DASHBOARD-06). Non appliqué (D4 — touche le périmètre ROI/DASHBOARD) ; appliedDeletions vide.
- `F-DRH-CHIFFRAGE-10` — Doublon du cadre transverse AI-ACT (AI-ACT-01/06 journal de contrôle humain). À porter une fois par AI-ACT. Non appliqué ici (D4 — relève d'une décision de matérialisation transverse PO) ; appliedDeletions vide.

**Ajoutées ce run (APPEND 2026-06-05) :** F-DRH-CHIFFRAGE-11, -12, -13, -14.
- `F-DRH-CHIFFRAGE-11` — Conservée + recadrée référence : trou marché/norme IAS 37 daté. Désignée générateur de référence de la fiche de provision pour absorber les déclinaisons par domaine (INAPT-13, SANCTION-12, DISCRIM-HARC-15, DASHBOARD-09) — invariant 1 outil = 1 situation appliqué au pont DAF. decisionTool=false.
- `F-DRH-CHIFFRAGE-12` — Conservée — besoin marché daté (contribution saisine CPH ~50 € mars 2026). decisionTool=true. Aligné explicitement avec les autres outils d'arbitrage contester/transiger (situations distinctes) pour cohérence du pattern.
- `F-DRH-CHIFFRAGE-13` — Conservée — combler le différenciant Jobexit. Couche de restitution sur les F-DT existants, pas de doublon de calcul. decisionTool=false.
- `F-DRH-CHIFFRAGE-14` — Conservée — norme d'achat (fiabilité Jobexit). ⚠️ Recoupement signalé avec PLATFORM-08 (provenance traçable) : recadrée comme déclinaison chiffrage pour éviter un doublon de capacité transverse. decisionTool=false.

**Modifiées / justifiées (curation) :**

- `F-DRH-CHIFFRAGE-01` — Reclassé plateforme-reutilisee (lit F-DT-01/09), decisionTool retiré.
- `F-DRH-CHIFFRAGE-02` — Reclassé plateforme-reutilisee (lit F-DT-16/36), decisionTool retiré.
- `F-DRH-CHIFFRAGE-03` — Reclassé plateforme-reutilisee (lit F-DT-01/07/15/25/26), CCN-aware déjà existant (F-DT-07).
- `F-DRH-CHIFFRAGE-04` — Reclassé plateforme-reutilisee (lit F-DT-31), decisionTool retiré.
- `F-DRH-CHIFFRAGE-06` — Reclassé plateforme-reutilisee (lit F-DT-16/30), decisionTool retiré.
- `F-DRH-CHIFFRAGE-08` — Doublon DASHBOARD signalé.
- `F-DRH-CHIFFRAGE-09` — Doublon ROI (DASHBOARD-06/PREAVOCAT-04) signalé.
- `F-DRH-CHIFFRAGE-10` — Doublon AI-ACT transverse signalé.

**Suppressions proposées (non appliquées — appliedDeletions vide, D4) :**

- `F-DRH-CHIFFRAGE-08` — Redondant avec F-DRH-DASHBOARD-01/03 (même 'portefeuille d'exposition agrégée'). Garder une seule feature dans le domaine DASHBOARD.
- `F-DRH-CHIFFRAGE-09` — Doublon de F-DRH-DASHBOARD-06 (ROI procurement) + F-DRH-PREAVOCAT-04 (chiffrage interne provision). Une seule feature ROI de référence (DASHBOARD-06).

---

### Domaine — Sécurisation procédurale des ruptures & actes (situation-employeur) (`SECU-PROC`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-DRH-SECU-PROC-01 | Checklist procédurale bloquante — licenciement motif personnel (s'appuie sur F-DT-08 + F-DT-36) | Checklist procédurale anti-vice séquencée, ALIMENTÉE par les détecteurs existants F-DT-08 (validité licenciement, conditions de forme/délais/motivation) et F-DT-36 (10 vices de procédure côté employeur, déjà construit suite signal Renversez). La VALEUR NOUVELLE = transformer ces détecteurs en checklist bloquante séquencée + garde-fous, pas re-détecter les vices. D8 : « mon risque procédural ». | concurrent-gap | Hypothèse |
| F-DRH-SECU-PROC-02 | Checklist procédurale bloquante — licenciement économique (s'appuie sur F-DT-13 + F-DT-14) | Checklist anti-vice du licenciement économique ALIMENTÉE par F-DT-13 (licenciement économique détaillé : motif, critères d'ordre, reclassement) et F-DT-14 (PSE : 4 axes L.1233, CSE, DREETS, seuils). La détection du fond existe déjà ; la checklist bloquante séquencée = valeur nouvelle. D8 : conformité. | droit-travail | Hypothèse |
| F-DRH-SECU-PROC-03 | Garde-fou calendaire — délais durs procédure disciplinaire (situation nouvelle) | Détecte dates et surveille les délais durs disciplinaires (prescription 2 mois L1332-4, 5 jours ouvrables, notification 2j-1mois L1332-2). Vérifié : aucun outil F-DT n'est un moteur calendaire de surveillance disciplinaire (F-DT-03 = prescriptions par type de litige, pas le compteur procédural). Gap réel. D8 : « suis-je dans les délais ? ». | concurrent-gap | Hypothèse |
| F-DRH-SECU-PROC-04 | Checklist procédurale bloquante — inaptitude (s'appuie sur F-DT-15) — DOUBLON domaine INAPT | ⚠️ DOUBLON inter-domaines : strictement identique à F-DRH-INAPT-02. Le domaine inaptitude-reclassement porte déjà cette checklist. S'appuie sur F-DT-15 (licenciement inaptitude existant). À traiter dans INAPT, pas ici. | concurrent-gap | Hypothèse |
| F-DRH-SECU-PROC-05 | Génération de la lettre de licenciement motivée et opposable — DOUBLON ACTES | ⚠️ DOUBLON inter-domaines avec F-DRH-ACTES-01 (lettre licenciement motif personnel) et F-DRH-ACTES-02 (économique). Réutilise F-DT-04 (génération fiche/PDF) + F-98 (génération courrier/conclusions). À porter par le domaine ACTES. | concurrent-gap | Hypothèse |
| F-DRH-SECU-PROC-06 | Génération acte disciplinaire proportionné — DOUBLON ACTES/SANCTION | ⚠️ DOUBLON avec F-DRH-ACTES-03 (notification sanction) et F-DRH-SANCTION-06 (acte de sanction). Le contrôle de proportionnalité = F-DRH-SANCTION-02. À consolider. | droit-travail | Hypothèse |
| F-DRH-SECU-PROC-07 | Génération convocation entretien préalable — DOUBLON ACTES | ⚠️ DOUBLON avec F-DRH-ACTES-04 (convocation entretien préalable). À porter par ACTES. | concurrent-gap | Hypothèse |
| F-DRH-SECU-PROC-08 | Génération proposition de reclassement — DOUBLON ACTES/INAPT | ⚠️ DOUBLON avec F-DRH-ACTES-05 (proposition reclassement) et F-DRH-INAPT-03 (reclassement documenté). À consolider. | concurrent-gap | Hypothèse |
| F-DRH-SECU-PROC-09 | Génération protocole transactionnel / RC — DOUBLON ACTES (s'appuie sur F-DT-31 + F-DT-10) | ⚠️ DOUBLON avec F-DRH-ACTES-07 (protocole transactionnel + RC). Réutilise F-DT-31 (validité transaction) + F-DT-10 (validité RC) comme garde-fous. À porter par ACTES. | concurrent-gap | Hypothèse |
| F-DRH-SECU-PROC-10 | Lecture employeur du contrôle de validité clause de non-concurrence (réutilise F-DT-24) | LIT côté employeur F-DT-24 (clause de non-concurrence : limitation temps/espace, spécificité, contrepartie financière, calcul). Aucun nouveau contrôle à construire. D8 : « ma clause tient-elle ? ». | plateforme-reutilisee | Hypothèse |
| F-DRH-SECU-PROC-11 | Journal de contrôle humain AI Act actes générés — DOUBLON AI-ACT | ⚠️ DOUBLON avec le domaine transverse AI-ACT (F-DRH-AI-ACT-01/06). À porter par AI-ACT, pas par domaine. | corporate-readiness | Hypothèse |
| F-DRH-SECU-PROC-12 | Audit social procédural préventif dossier-centric — cartographie des zones de fragilité AVANT tout déclenchement | Scanne un dossier centré-salarié (contrat, avenants, sanctions passées, courriers, paie) pour cartographier en amont les zones de fragilité procédurale AVANT toute décision de rupture : vices de forme dormants, prescriptions disciplinaires en cours, clauses fragiles, antécédents exposant à un grief de discrimination. Orchestre les détecteurs existants (F-DT-08, F-DT-36, F-DT-24) en POSTURE DE PRÉVENTION récurrente, distincte de PREAVOCAT-02 (agrégation aval pour handoff avocat). Déplace l'audit social humain non scalable vers un self-service temps réel. D8 strict. | concurrent-gap | Hypothèse |
| F-DRH-SECU-PROC-13 | Checklist procédurale bloquante — rupture conventionnelle (entretiens, rétractation 15j, homologation DREETS) | Checklist anti-vice séquencée de la PROCÉDURE de RC : entretien(s), faculté d'assistance, signature/remise d'un exemplaire (nullité), rétractation 15 j calendaires, homologation DREETS (15 j ouvrables). S'appuie sur F-DT-10 (validité RC : consentement, montant) qui contrôle le FOND ; valeur = la chaîne PROCÉDURALE bloquante et son calendrier. Étend le pattern checklists 01/02 (licenciement perso/éco) à la voie négociée. D8. | concurrent-gap | Hypothèse |
| F-DRH-SECU-PROC-14 | Garde-fou calendaire — chaîne de rupture (rétractation & homologation RC, délais de notification licenciement) | Étend le moteur calendaire de SECU-PROC-03 (limité au disciplinaire L1332) à la chaîne de rupture : rétractation RC (15 j cal.), homologation DREETS (15 j ouvrables, implicite), délais durs de notification licenciement (2 j ouvrables à 1 mois selon motif), préavis. Compteur critico-urgent + alertes anti-forclusion. Ne re-détecte aucun vice de fond ; surveille des bornes que ni F-DT-03 ni SECU-PROC-03 ne couvrent. D8. | concurrent-gap | Hypothèse |
| F-DRH-SECU-PROC-15 | Mise à jour jurisprudentielle des checklists anti-vice (veille appliquée au dossier) | Maintient les checklists 01/02/13 et le garde-fou 03/14 à jour de la jurisprudence/normes récentes, appliquées au dossier : reclassement « caractérisé » écrit/motivé (Cass. nov. 2025), nouveaux modèles d'avis médecin (01/07/2025), consolidation post-Macron 2026, contribution saisine CPH (~50 €). Réutilise le moteur jurisprudence F-JU-01 lu côté employeur (via PLATFORM-04) ; ne crée pas un second moteur de veille. ⚠️ Recoupe AI-ACT-13 (veille échéances réglementaires) sur l'angle 'fraîcheur normative' : à articuler. ⚠️ SCOPE JURIDICTION V1 = FR seul (CPH, barème Macron, France Travail/DREETS) ; BE = backlog différé (couverture exhaustive du droit social belge attendue, PAS un miroir FR). D8. | marche | Hypothèse |
| F-DRH-SECU-PROC-16 | Sécurisation procédurale de la mise à pied conservatoire (concomitance, articulation au calendrier disciplinaire) | Sécurise la procédure de la mise à pied conservatoire précédant un licenciement faute grave/lourde : concomitance avec l'engagement de la procédure disciplinaire (sinon requalification en mise à pied disciplinaire), absence de double sanction, articulation aux délais durs (2 mois prescription, 5 j ouvrables, notification). S'appuie sur SECU-PROC-03 et oriente vers le chiffrage de nullité SANCTION-05 (rappel de salaire si annulation). Situation distincte de la convocation (ACTES-04) et de la notification (ACTES-03). D8. | droit-travail | Hypothèse |
| F-DRH-SECU-PROC-17 | Garde-fou de la priorité de réembauche post-licenciement économique (L1233-45, 12 mois) | Surveille une obligation DURABLE POST-rupture : pendant 12 mois après un licenciement économique, l'employeur doit proposer au salarié qui en fait la demande tout poste disponible compatible (L1233-45). Suit la demande du salarié, les postes ouverts, la traçabilité des propositions/réponses ; alerte anti-violation (sa méconnaissance = dommages-intérêts ≥ 1 mois de salaire). Distinct de SECU-PROC-02 (checklist PRÉ-décision) et de SECU-PROC-14 (calendrier PRÉ-notification) : c'est une borne AVAL. À implémenter comme extension du moteur calendaire SECU-PROC-03/14 (un seul moteur, N bornes). D8 : « ai-je respecté mon obligation ? », jamais armer contre le salarié. | concurrent-gap | Hypothèse |
| F-DRH-SECU-PROC-18 | Contrôle de précision & d'individualisation de l'offre de reclassement (licenciement économique) | Vérifie depuis les pièces que l'offre de reclassement économique est écrite, précise, personnalisée et sérieuse — l'imprécision ou le caractère générique de l'offre prive le licenciement de cause réelle et sérieuse (exposition 6-12 mois de salaire). Pendant économique de F-DRH-INAPT-12 (refus de reclassement « caractérisé » côté inaptitude, Cass. nov. 2025) : situation procédurale distincte. Lit les détecteurs de fond F-DT-13/14 via PLATFORM-04 ; sortie = fragilité bloquante si offre imprécise ; aval éditorial = ACTES-02/05. Zone anti-vice vide chez les concurrents (éditeurs documentaires, jurimétrie paramétrique). D8 : « mon offre tient-elle ? ». | concurrent-gap | Hypothèse |

**Ajoutées ce run (APPEND 2026-06-06) :** F-DRH-SECU-PROC-17, -18.
- `F-DRH-SECU-PROC-17` — Conservée — borne AVAL (post-rupture, L1233-45, 12 mois) non couverte par 02 (pré-décision) ni 14 (pré-notification). Extension du moteur calendaire (un seul moteur, N bornes). decisionTool=true. D8 strict (« ai-je respecté », pas « comment éviter de réembaucher »).
- `F-DRH-SECU-PROC-18` — Conservée — pendant économique de INAPT-12 (caractérisation refus inaptitude) ; situation distincte. Lit F-DT-13/14 via PLATFORM-04, n'ajoute pas de détecteur de fond. decisionTool=true.

**Note de scope (verrou D-juridiction, 2026-06-06) :** V1 du domaine = FR seul (procédures CPH, délais L1332/L1233, barème Macron, DREETS/France Travail, contribution saisine CPH ~50 € 2026). BE = backlog différé — couverture exhaustive du droit social belge attendue, PAS un miroir FR (cf. feedback_belgique_never_forget). Marqueur ajouté sur SECU-PROC-15.

**Ajoutées run précédent (APPEND 2026-06-05) :** F-DRH-SECU-PROC-12, -13, -14, -15, -16.
- `F-DRH-SECU-PROC-12` — Conservée — trou marché (audit social préventif = prestation humaine ponctuelle non scalable). Distincte de PREAVOCAT-02 (aval). 1 outil = 1 situation respecté. decisionTool=true.
- `F-DRH-SECU-PROC-13` — Conservée — RC = voie de rupture la plus fréquente côté employeur ; F-DT-10 ne contrôle que le fond. Gap distinct des checklists licenciement. decisionTool=true.
- `F-DRH-SECU-PROC-14` — Conservée — extension du moteur 03 à de NOUVELLES bornes (situation distincte), pas un doublon. ⚠️ À implémenter comme extension de SECU-PROC-03 (un seul moteur calendaire, N bornes). decisionTool=true.
- `F-DRH-SECU-PROC-15` — Conservée — normes datées/mouvantes rendent une checklist figée obsolète. Réutilise F-JU-01 via PLATFORM-04 (pas de nouveau moteur). Recoupement AI-ACT-13 signalé. decisionTool=false.
- `F-DRH-SECU-PROC-16` — Conservée — vice fréquent (défaut de concomitance) non couvert par 01/02, SECU-PROC-03 générique, ni ACTES. Situation procédurale propre. decisionTool=true.

**Modifiées / justifiées (curation) :**

- `F-DRH-SECU-PROC-01` — Recadré : orchestre F-DT-08/36 (détection vices déjà existante), ne duplique pas.
- `F-DRH-SECU-PROC-02` — Recadré : orchestre F-DT-13/14 existants.
- `F-DRH-SECU-PROC-04` — Doublon F-DRH-INAPT-02 signalé.
- `F-DRH-SECU-PROC-05` — Doublon ACTES-01/02 signalé.
- `F-DRH-SECU-PROC-06` — Doublon ACTES-03/SANCTION-06 signalé.
- `F-DRH-SECU-PROC-07` — Doublon ACTES-04 signalé.
- `F-DRH-SECU-PROC-08` — Doublon ACTES-05/INAPT-03 signalé.
- `F-DRH-SECU-PROC-09` — Doublon ACTES-07 signalé ; F-DT-31/10 réutilisés.
- `F-DRH-SECU-PROC-10` — Reclassé plateforme-reutilisee (lit F-DT-24), decisionTool retiré.
- `F-DRH-SECU-PROC-11` — Doublon AI-ACT signalé.

**Suppressions proposées :**

- `F-DRH-SECU-PROC-04` — Doublon strict de F-DRH-INAPT-02 (checklist anti-vice inaptitude). Conserver l'unique feature dans le domaine inaptitude-reclassement.
- `F-DRH-SECU-PROC-05` — Doublon de F-DRH-ACTES-01/02 (génération lettre licenciement). Conserver les features de génération dans le domaine ACTES.
- `F-DRH-SECU-PROC-06` — Doublon de F-DRH-ACTES-03 + F-DRH-SANCTION-06 (acte disciplinaire). Conserver dans ACTES/SANCTION.
- `F-DRH-SECU-PROC-07` — Doublon de F-DRH-ACTES-04 (convocation entretien préalable).
- `F-DRH-SECU-PROC-08` — Doublon de F-DRH-ACTES-05 + F-DRH-INAPT-03 (proposition reclassement).
- `F-DRH-SECU-PROC-09` — Doublon de F-DRH-ACTES-07 (protocole transactionnel/RC).
- `F-DRH-SECU-PROC-11` — Doublon du cadre transverse AI-ACT (F-DRH-AI-ACT-01/06).

---

### Domaine — Inaptitude médicale & obligation de reclassement (situation-employeur) (`inaptitude-reclassement`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-DRH-INAPT-01 | Qualification dossier-centric inaptitude partielle vs totale (étend F-DT-15) | Qualifie partielle/totale + dispense de reclassement à partir de l'avis du médecin du travail. F-DT-15 (licenciement inaptitude) existe déjà mais chiffre l'indemnité ; la qualification partielle/totale + conséquences procédurales en amont est une situation distincte légitime, à brancher sur F-DT-15. D7 : « mon risque ». | droit-travail | Hypothèse |
| F-DRH-INAPT-02 | Checklist procédurale anti-vice inaptitude (bloquante) — feature de référence (cf. SECU-PROC-04) | Checklist bloquante de la chaîne inaptitude (visite médicale, reclassement documenté, consultation CSE, délai 1 mois, notification), alimentée par F-DT-15 + F-DT-36. C'est ICI la feature de référence ; F-DRH-SECU-PROC-04 est son doublon (à supprimer). | concurrent-gap | Hypothèse |
| F-DRH-INAPT-03 | Assistant de reclassement documenté (preuve de recherche) — feature de référence (cf. ACTES-05/SECU-PROC-08) | Structure et documente l'obligation de reclassement (postes compatibles, critères, périmètre, traçabilité refus) → dossier de preuve opposable. Feature de référence ; F-DRH-ACTES-05 (génération proposition) en est l'aval acte et F-DRH-SECU-PROC-08 un doublon. D8 : non-discrimination. | droit-travail | Hypothèse |
| F-DRH-INAPT-04 | Trame de consultation CSE inaptitude + garde-fou bloquant — DOUBLON CSE-CONFORM | ⚠️ DOUBLON inter-domaines avec F-DRH-CSE-CONFORM-02/03 (checklist + génération convocation CSE). La situation 'consultation CSE inaptitude' est portée par le domaine CSE-CONFORM. À consolider. | concurrent-gap | Hypothèse |
| F-DRH-INAPT-05 | Lecture employeur du chiffrage indemnité spéciale inaptitude (réutilise F-DT-15) | LIT côté employeur F-DT-15 (InaptitudeCalculator : indemnité spéciale doublée origine pro, compensatrice préavis, congés, damages 12 mois si reclassement non respecté, CCN-aware). Rappels de salaire post-1 mois à brancher si non couverts par F-DT-15. Aucun nouveau simulateur. | plateforme-reutilisee | Hypothèse |
| F-DRH-INAPT-06 | Lecture employeur détection nullité salarié protégé/santé (réutilise F-DT-16 + F-DT-30) | LIT côté employeur F-DT-16 (licenciement nul, dont AT/MP + état de santé) + F-DT-30 (salarié protégé, autorisation inspection du travail). Aucun nouveau détecteur. D8 : exposition non plafonnée. | plateforme-reutilisee | Hypothèse |
| F-DRH-INAPT-07 | Scoring d'exposition global du dossier inaptitude (orchestration) — décliné de CHIFFRAGE-07 | Score consolidé inaptitude. ⚠️ Recoupe le pattern F-DRH-CHIFFRAGE-07 (scoring d'exposition générique orchestrateur). À traiter comme une CONFIGURATION du scoring générique par situation, pas un moteur de scoring distinct, pour respecter 1 outil=1 situation. | marche | Hypothèse |
| F-DRH-INAPT-08 | Génération lettre licenciement inaptitude — DOUBLON ACTES (réutilise F-98 + F-DT-04) | ⚠️ Génération d'acte : doublon du pattern ACTES (F-DRH-ACTES-01/06). Réutilise F-98 (génération courrier) + F-DT-04 (PDF). À porter par le domaine ACTES en tant que variante inaptitude. | concurrent-gap | Hypothèse |
| F-DRH-INAPT-09 | Lecture employeur du moteur jurisprudence sur suffisance reclassement (réutilise F-JU-01) | LIT côté employeur F-JU-01 (citations jurisprudentielles Cassation, qualité durcie F-JU-06) appliqué à la suffisance/régularité du reclassement. Aucun nouveau moteur jurisprudence. ⚠️ Recoupe le pattern 'réutilisation moteur jurisprudence' répété dans CHAQUE domaine (REQUAL-CDD-10, DISCRIM-HARC-11, SANCTION-07, CSE-CONFORM-09, TEMPS-TRAVAIL-11). | plateforme-reutilisee | Hypothèse |
| F-DRH-INAPT-10 | Conformité AI Act décision licenciement inaptitude — DOUBLON AI-ACT | ⚠️ DOUBLON du cadre transverse AI-ACT (F-DRH-AI-ACT-01/02/03/04/06). À porter par AI-ACT. | corporate-readiness | Hypothèse |
| F-DRH-INAPT-11 | Normalisation des nouveaux modèles d'avis du médecin du travail (01/07/2025) | Parse et normalise les avis d'inaptitude sur les nouveaux modèles (mentions de dispense de reclassement, case origine professionnelle, capacités résiduelles). Alimente INAPT-01 (qualification) et INAPT-02 (checklist). Trou : les éditeurs documentaires commentent mais n'extraient pas la mention depuis la pièce uploadée. Pattern D3 : upload avis → extraction → branche dispense/obligation. D7. | marche | Hypothèse |
| F-DRH-INAPT-12 | Contrôle de caractérisation du refus de reclassement (standard Cass. nov. 2025) | Vérifie depuis les pièces que le refus de reclassement est « caractérisé » par un écrit motivé conforme au nouveau standard (Cass. nov. 2025). Sortie = fragilité bloquante si non caractérisé (privation de cause réelle et sérieuse → 6-12 mois). Aval éditorial : ACTES-05. Zone procédurale anti-vice à jour, vide chez les concurrents. D8. | concurrent-gap | Hypothèse |
| F-DRH-INAPT-13 | Fiche de provision IAS 37 du dossier inaptitude (pont DRH→DAF) — décliné de CHIFFRAGE-11 | Produit une fiche de provision datée/signable (probabilité × montant F-DT-15 + frais + différé ARE) pour le risque du dossier inaptitude. ⚠️ DÉCLINAISON du générateur de fiche de provision de référence CHIFFRAGE-11 (1 outil = 1 situation) : configuration 'inaptitude', PAS un moteur distinct. Réutilise le chiffrage lu en INAPT-05 et le scoring lu en INAPT-07. Spécificité = chiffrage inaptitude (indemnité spéciale doublée + 12 mois reclassement). | marche | Hypothèse |
| F-DRH-INAPT-14 | Export dossier inaptitude structuré « mode pré-avocat » — décliné de PREAVOCAT-01 | Exporte un dossier inaptitude structuré (faits, chronologie médicale, pièces, qualification, traçabilité reclassement, vices, chiffrage). ⚠️ DÉCLINAISON du pattern d'export 'pré-avocat' porté par PREAVOCAT-01 (domaine dédié) : CONFIGURATION inaptitude, PAS un nouveau moteur. Réutilise F-DT-04 (PDF). D8 : ROI/internalisation, non aliénant pour le Barreau. | concurrent-gap | Hypothèse |

| F-DRH-INAPT-15 | Aide à la décision sur le recours contre l'avis d'inaptitude (référé CPH, délai 15 jours) | Outil décisionnel contester l'avis du médecin du travail (saisine CPH en référé, médecin-inspecteur) vs reclasser/licencier sur l'avis : capacités résiduelles vs poste, cohérence dispense/origine pro (INAPT-11), délai dur 15 j, exposition comparée (INAPT-05/06). Situation DISTINCTE de la qualification (INAPT-01) et de la checklist (INAPT-02) : objet = régularité/bien-fondé de l'avis. Trou marché total. D8 : sécurisation/conformité. | concurrent-gap | Hypothèse |
| F-DRH-INAPT-16 | Compteur du délai d'1 mois — reprise obligatoire du paiement du salaire (échéancier anti-forclusion) | Compteur calendaire du délai d'1 mois post-avis d'inaptitude : à expiration sans reclassement ni licenciement, reprise obligatoire du versement du salaire (rappels exposés dans F-DT-15). Alerte critico-urgente J-X + état (en cours / échu / salaire dû). Distinct de INAPT-02 (qui liste l'item) et INAPT-05 (qui calcule les rappels). ⚠️ NE PAS dupliquer SECU-PROC-14 (rupture) : déclinaison « inaptitude » du même garde-fou calendaire (paramètre = délai 1 mois post-avis). Trou réel. | droit-travail | Hypothèse |
| F-DRH-INAPT-17 | Qualification de l'origine professionnelle de l'inaptitude (AT/MP) quand l'avis est silencieux ou contesté | Qualifie depuis les pièces (déclaration AT, certificats, lien accident/MP) le régime applicable quand l'avis est silencieux ou l'origine contestée. L'origine pro déclenche doublement de l'indemnité spéciale + indemnité compensatrice de préavis + protection renforcée (exposition nullité). Distinct de INAPT-11 (qui extrait la MENTION cochée) : ici qualification de fond quand la mention manque/est litigieuse. Alimente INAPT-05 (chiffrage) + INAPT-06 (nullité). D8. | droit-travail | Hypothèse |
| F-DRH-INAPT-18 | Aide à la décision contester/transiger — déclinaison inaptitude (moteur d'arbitrage commun) | Décline le pattern contester ↔ transiger sur le contentieux inaptitude : CPH (exposition INAPT-05/06 = indemnité doublée + risque nullité, contribution saisine CPH ~50 €, ~13,7 mois, ~67 % appel, honoraires ≥ 4 500 € HT) vs transiger (borne F-DT-31 + régime social-fiscal CHIFFRAGE-05). ⚠️ Situation DISTINCTE (objet = licenciement inaptitude) partageant le MOTEUR D'ARBITRAGE COMMUN avec CHIFFRAGE-12 / SANCTION-11 / REQUAL-CDD-11 / PREAVOCAT-08 (paramètres procédure 2026 alignés) : configuration, PAS un nouveau moteur. D8. | marche | Hypothèse |

**Ajoutées ce run (APPEND 2026-06-06 — run de maturation) :** F-DRH-INAPT-15, -16, -17, -18.
- `F-DRH-INAPT-15` — Conservée — recours contre l'avis d'inaptitude (référé CPH 15 j), trou marché total. decisionTool=true.
- `F-DRH-INAPT-16` — Conservée — compteur délai 1 mois reprise salaire ; déclinaison « inaptitude » du garde-fou calendaire (ne pas dupliquer SECU-PROC-14). decisionTool=true.
- `F-DRH-INAPT-17` — Conservée — qualification de l'origine pro AT/MP quand l'avis est silencieux ; distinct de INAPT-11 (mention cochée). decisionTool=true.
- `F-DRH-INAPT-18` — Conservée — arbitrage contester/transiger inaptitude, situation distincte partageant le moteur d'arbitrage commun. decisionTool=true.

**Modifiées / justifiées (curation 2026-06-06) :**
- `F-DRH-INAPT-07` — Marquage explicite du sujet UX non tranché (scoring ⊂ outils CALCULÉS/persistés, pas champs pré-remplis non cliqués) à arbitrer avant dev. Déclinaison du moteur de scoring de référence CHIFFRAGE-07.
- `F-DRH-INAPT-13` — Articulé sur les couches transverses chiffrage (CHIFFRAGE-13 multi-assiette, CHIFFRAGE-14 traçabilité) + compteur reprise salaire INAPT-16 ; déclinaison du générateur de fiche CHIFFRAGE-11, pas un second moteur.
- `F-DRH-INAPT-14` — Rappel de l'invariant D8 (export borné/révocable, jamais accès croisé inter-workspaces) aligné sur PREAVOCAT-07/10 ; déclinaison de PREAVOCAT-01.

**Suppressions proposées (2026-06-06, non appliquées — appliedDeletions vide, D4) :**
- `F-DRH-INAPT-04` — Doublon strict de F-DRH-CSE-CONFORM-02/03 (checklist + génération convocation/note CSE). La consultation CSE inaptitude est portée par CSE-CONFORM (référence CSE-CONFORM-02 présente). 1 outil = 1 situation.
- `F-DRH-INAPT-08` — Doublon du pattern de génération d'actes (ACTES-01/06, F-98 + F-DT-04). La lettre de licenciement inaptitude est une variante du domaine ACTES, pas une feature inaptitude.
- `F-DRH-INAPT-10` — Doublon du cadre transverse AI-ACT (AI-ACT-01/02/03/04/06). Conformité AI Act = capacité transverse, pas répliquée par domaine.

**Ajoutées ce run (APPEND 2026-06-05) :** F-DRH-INAPT-11, -12, -13, -14.
- `F-DRH-INAPT-11` — Conservée — driver marché daté (modèles 01/07/2025) ; aucune feature n'extrait la dispense depuis la pièce réelle (distinct de INAPT-01 générique).
- `F-DRH-INAPT-12` — Conservée — arrêt Cass. nov. 2025 = nouveau standard, situation de contrôle distincte de INAPT-02 (checklist générique) et INAPT-03 (assistant). decisionTool=true.
- `F-DRH-INAPT-13` — Conservée comme DÉCLINAISON de CHIFFRAGE-11 (générateur de provision de référence) pour respecter l'invariant 1 outil = 1 situation. Spécificité inaptitude justifiée.
- `F-DRH-INAPT-14` — Conservée comme DÉCLINAISON inaptitude de PREAVOCAT-01 (l'export pré-avocat est un domaine dédié) — recoupement transverse résolu par configuration, pas nouveau moteur.

**Modifiées / justifiées (curation) :**

- `F-DRH-INAPT-01` — Recadré : complète F-DT-15 (qualification amont), ne le duplique pas.
- `F-DRH-INAPT-02` — Désignée feature de référence vs doublon SECU-PROC-04 ; decisionTool ajouté.
- `F-DRH-INAPT-03` — Désignée référence reclassement documenté ; decisionTool ajouté.
- `F-DRH-INAPT-04` — Doublon CSE-CONFORM-02/03 signalé.
- `F-DRH-INAPT-05` — Reclassé plateforme-reutilisee (lit F-DT-15), decisionTool retiré.
- `F-DRH-INAPT-06` — Reclassé plateforme-reutilisee (lit F-DT-16/30), decisionTool retiré.
- `F-DRH-INAPT-07` — Recoupement scoring CHIFFRAGE-07 signalé.
- `F-DRH-INAPT-08` — Doublon génération ACTES signalé (F-98/F-DT-04).
- `F-DRH-INAPT-09` — Reclassé plateforme-reutilisee (lit F-JU-01) ; pattern répété 6x signalé.
- `F-DRH-INAPT-10` — Doublon AI-ACT signalé.

**Suppressions proposées :**

- `F-DRH-INAPT-10` — Doublon du cadre transverse AI-ACT. La conformité AI Act doit être UNE feature transverse, pas répliquée par domaine.

---

### Domaine — Sanctions disciplinaires & proportionnalité (situation employeur) (`SANCTION`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-DRH-SANCTION-01 | Qualification de la faute à partir des pièces (situation nouvelle, complète F-DT-08/36) | Qualifie la faute (simple/grave/lourde) depuis les pièces. Vérifié : aucun F-DT ne qualifie la faute disciplinaire en amont (F-DT-08 = validité motif licenciement, F-DT-36 = vices de procédure). Gap réel côté qualification disciplinaire. Réutilise le moteur de qualification pipeline IA existant. | droit-travail | Hypothèse |
| F-DRH-SANCTION-02 | Contrôle de proportionnalité de la sanction (situation nouvelle — GAP RÉEL) | Test de proportionnalité jurisprudentiel (gravité, antécédents, échelle CCN/RI) situant la sanction sur l'échelle. Vérifié : AUCUN outil F-DT n'évalue la proportionnalité d'une sanction (le catalogue couvre licenciement/nullité/indemnités, pas la gradation disciplinaire). Gap concurrentiel ET produit réel. À cadrer comme outil plateforme (utile aussi à l'avocat). D8 : « quelle sanction tient en CPH ». | concurrent-gap | Hypothèse |
| F-DRH-SANCTION-03 | Checklist anti-vice procédure disciplinaire (garde-fou calendaire) — DOUBLON SECU-PROC-03 | ⚠️ DOUBLON avec F-DRH-SECU-PROC-03 (garde-fou calendaire délais durs disciplinaires, mêmes articles L1332-2/L1332-4). À consolider en une seule feature. | droit-travail | Hypothèse |
| F-DRH-SANCTION-04 | Détection des droits procéduraux du salarié avant sanction (complète SANCTION-03) | Liste les droits procéduraux substantiels (assistance, délais de prévenance, consultation salarié protégé, mentions). Distinct du calendrier (SANCTION-03). Vérifié non couvert tel quel par F-DT. D8 : conformité. | droit-travail | Hypothèse |
| F-DRH-SANCTION-05 | Chiffrage du risque de requalification/nullité de la sanction (situation nouvelle) | Chiffre l'exposition en cas de contestation (annulation pour disproportion/vice, rappel salaire mise à pied annulée, requalification en licenciement déguisé). Vérifié : aucun F-DT ne chiffre la nullité d'une SANCTION (≠ licenciement). Gap réel. D8 : « mon risque ». | concurrent-gap | Hypothèse |
| F-DRH-SANCTION-06 | Génération acte de sanction conforme — DOUBLON ACTES (réutilise F-98 + F-DT-04) | ⚠️ DOUBLON avec F-DRH-ACTES-03 (notification sanction) + F-DRH-ACTES-04 (convocation). Réutilise F-98 + F-DT-04. À porter par ACTES. | plateforme-reutilisee | Hypothèse |
| F-DRH-SANCTION-07 | Lecture employeur jurisprudence proportionnalité sanction (réutilise F-JU-01) | LIT côté employeur F-JU-01 appliqué à la proportionnalité des sanctions comparables. ⚠️ Pattern 'réutilisation jurisprudence' répété 6x (cf. INAPT-09). À unifier en capacité plateforme. ⚠️ UX NON TRANCHÉ : s'appuie sur SANCTION-02 CALCULÉ/persisté, pas sur les champs pré-remplis non cliqués — à arbitrer avant dev. | plateforme-reutilisee | Hypothèse |
| F-DRH-SANCTION-08 | Intégration CCN à l'échelle des sanctions (CCN-aware) — recoupe F-DT-07 + ACTES-08 | Intègre l'échelle conventionnelle des sanctions. ⚠️ Le CCN-aware existe déjà (F-DT-07) et est répété dans CHAQUE domaine (REQUAL-CDD-05, DISCRIM-HARC-05, ACTES-08, API-SIRH-04, TEMPS-TRAVAIL-06, CSE-CONFORM-06). À unifier en capacité transverse CCN-aware, pas 7 features. | concurrent-gap | Hypothèse |
| F-DRH-SANCTION-09 | Journal de contrôle humain AI Act décision de sanction — DOUBLON AI-ACT | ⚠️ DOUBLON du cadre transverse AI-ACT. À porter par AI-ACT. | corporate-readiness | Hypothèse |
| F-DRH-SANCTION-10 | Comparateur de niveaux de sanction (avertissement → licenciement) avec exposition par niveau | Compare côte à côte les niveaux de sanction pour les mêmes faits (avertissement/blâme/mise à pied disciplinaire/rétrogradation/licenciement) avec, par niveau : tenue jurisprudentielle attendue (SANCTION-02), exposition chiffrée (SANCTION-05), conformité échelle CCN/RI. DISTINCT de SANCTION-02 (qui SITUE une sanction donnée) : ici on COMPARE des alternatives. Gap : Jobexit compare 9 ruptures mais aucun acteur ne compare les NIVEAUX de sanction disciplinaire. D8. | concurrent-gap | Hypothèse |
| F-DRH-SANCTION-11 | Aide à la décision contester/maintenir une sanction contestée (saisine CPH 2026 + risque d'appel) | Quand une sanction notifiée est contestée, aide à décider maintenir/retirer/transiger avec les paramètres 2026 (contribution saisine CPH ~50 €, ~13,7 mois, ~67 % d'appel) + exposition (SANCTION-05) + tenue jurisprudentielle (SANCTION-02/07). DISTINCT de SANCTION-05 (qui CHIFFRE). ⚠️ Même pattern d'arbitrage que CHIFFRAGE-12 / REQUAL-CDD-11 (situations distinctes) — aligner sur le moteur d'arbitrage commun. D8. | marche | Hypothèse |
| F-DRH-SANCTION-12 | Fiche de provision IAS 37 du risque d'annulation de sanction — décliné de CHIFFRAGE-11 | Produit la fiche de provision propre au risque d'annulation/requalification de LA sanction (rappel salaire mise à pied annulée + DI disproportion/vice + frais) depuis SANCTION-05, datée/signée, réévaluable. ⚠️ DÉCLINAISON du générateur de fiche de provision de référence CHIFFRAGE-11 (1 outil = 1 situation) : configuration 'sanction', PAS un moteur distinct. Spécificité = rappel salaire mise à pied annulée. ⚠️ UX NON TRANCHÉ : la fiche s'alimente du scoring SANCTION-05/CHIFFRAGE-07 CALCULÉ/persisté, pas des champs pré-remplis non cliqués — arbitrage avant dev (alerte avant génération / pré-calcul auto / laisser tel quel). D8 : conformité comptable. | marche | Hypothèse |
| F-DRH-SANCTION-13 | Détection du cumul illégal de sanctions (non bis in idem + sanction pécuniaire interdite) | Détecte le double-emploi d'une sanction sur les mêmes faits (principe non bis in idem disciplinaire) et les sanctions pécuniaires prohibées (retenue/amende déguisée, art. L1331-2). Situation métier NOUVELLE et distincte du garde-fou calendaire (SECU-PROC-03/ex-SANCTION-03 = délais de la procédure) et de la prescription des faits (SANCTION-14) : ici on contrôle la LICÉITÉ du cumul, pas le calendrier. Aucun F-DT ne couvre le non bis in idem disciplinaire ni la prohibition pécuniaire. Réutilise le pipeline IA d'extraction des antécédents disciplinaires. Alimente SANCTION-05 (chiffrage nullité). D8 : conformité, « cette sanction tient-elle ». | droit-travail | Hypothèse |
| F-DRH-SANCTION-14 | Contrôle de prescription des faits fautifs (délai 2 mois L1332-4, réitération, point de départ) | Outil de datation/contrôle de la prescription disciplinaire des faits (engagement des poursuites > 2 mois après connaissance des faits par l'employeur ; effet de la réitération qui ravive ; point de départ depuis les pièces). DISTINCT du garde-fou calendaire de la PROCÉDURE (SECU-PROC-03, qui borne les délais ENTRE convocation/entretien/notification) : SANCTION-14 contrôle la recevabilité des FAITS en amont, vice substantiel propre. Aucun F-DT ne porte la prescription des faits fautifs. Alimente SANCTION-01 (qualification) et SANCTION-05 (chiffrage). D8 : « ma sanction est-elle prescrite ». | droit-travail | Hypothèse |
| F-DRH-SANCTION-15 | Alerte AI Act « décision de sanction = système haut risque » (orchestration du gate, pas un nouveau moteur) | Lors d'une décision de sanction assistée (qualification/proportionnalité/comparateur), DÉCLENCHE et oriente vers le cadre transverse AI Act : rappel contrôle humain obligatoire (Annexe III.4, échéance 02/08/2026), info préalable des salariés + consultation CSE, journalisation. NE DUPLIQUE PAS AI-ACT-01 (journal) ni AI-ACT-02 (FRIA conditionnelle) : pure orchestration contextuelle côté situation sanction qui invoque le cadre transverse. Remplace l'angle de l'ex-SANCTION-09 (qui dupliquait le journal) par un déclencheur, pas un moteur. D10 : gate d'achat. | corporate-readiness | Hypothèse |

**Ajoutées ce run (APPEND 2026-06-06) :** F-DRH-SANCTION-13, -14, -15.
- `F-DRH-SANCTION-13` — Gap réel : non bis in idem disciplinaire + prohibition des sanctions pécuniaires (L1331-2) non couverts par F-DT ni par les SANCTION existantes. Distinct du calendrier (SECU-PROC-03) et de la prescription (SANCTION-14). decisionTool=true.
- `F-DRH-SANCTION-14` — Gap réel : prescription des FAITS fautifs (2 mois L1332-4) ≠ délais de la PROCÉDURE (SECU-PROC-03). Vice substantiel distinct, alimente qualification + chiffrage. decisionTool=true.
- `F-DRH-SANCTION-15` — Orchestration du gate AI Act côté situation sanction (déclencheur contextuel), PAS un nouveau journal : réutilise AI-ACT-01/02. Lève l'ambiguïté de l'ex-SANCTION-09 (doublon journal) en la remplaçant par un déclencheur. decisionTool=false.

**Modifiées ce run (APPEND 2026-06-06) :**
- `F-DRH-SANCTION-07` — UX NON TRANCHÉ marqué : la lecture employeur de la jurisprudence de proportionnalité s'appuie sur SANCTION-02 CALCULÉ/persisté, pas sur des champs pré-remplis non cliqués (sujet à arbitrer avant dev).
- `F-DRH-SANCTION-12` — UX NON TRANCHÉ marqué (scoring/fiche ⊂ outils calculés/persistés) ; scope juridiction V1 = FR seul (IAS 37 + CPH FR), BE différé backlog.

**Ajoutées ce run (APPEND 2026-06-05) :** F-DRH-SANCTION-10, -11, -12.
- `F-DRH-SANCTION-10` — Conservée — orthogonal à SANCTION-02 (comparaison ≠ situation unique). 1 outil = 1 situation respecté. decisionTool=true.
- `F-DRH-SANCTION-11` — Conservée — besoin marché daté (saisine CPH ~50 €). Distinct du chiffrage (SANCTION-05). Aligné avec le pattern d'arbitrage contester/transiger (CHIFFRAGE-12). decisionTool=true.
- `F-DRH-SANCTION-12` — Conservée comme DÉCLINAISON de CHIFFRAGE-11 (anti-doublon du pont DAF) ; spécificité sanction justifiée. Le borderline-doublon assumé est résolu : rattaché au générateur de référence. decisionTool=false.

**Modifiées / justifiées (curation) :**

- `F-DRH-SANCTION-01` — decisionTool ajouté ; confirmé gap réel (qualification faute).
- `F-DRH-SANCTION-02` — Confirmé gap majeur ; signalé comme outil plateforme avocat+employeur.
- `F-DRH-SANCTION-03` — Doublon SECU-PROC-03 signalé.
- `F-DRH-SANCTION-06` — Doublon ACTES-03/04 signalé ; F-98/F-DT-04 réutilisés.
- `F-DRH-SANCTION-07` — Réutilise F-JU-01 ; pattern répété signalé.
- `F-DRH-SANCTION-08` — CCN-aware déjà existant (F-DT-07), dupliqué 7x — unification signalée.
- `F-DRH-SANCTION-09` — Doublon AI-ACT signalé.

**Suppressions proposées :**

- `F-DRH-SANCTION-03` — Doublon de F-DRH-SECU-PROC-03 (garde-fou calendaire disciplinaire, mêmes articles). Garder une seule feature.
- `F-DRH-SANCTION-06` — Doublon de F-DRH-ACTES-03 (notification) + F-DRH-ACTES-04 (convocation). À porter par ACTES.
- `F-DRH-SANCTION-08` — CCN-aware déjà existant (F-DT-07) et dupliqué 7x. À unifier en capacité transverse CCN-aware.
- `F-DRH-SANCTION-09` — Doublon du cadre transverse AI-ACT (journal de contrôle humain = AI-ACT-01). Une feature transverse, pas répliquée par domaine.

---

### Domaine — Tableau de bord du risque social consolidé (`situation-employeur`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-DRH-DASHBOARD-01 | Vue portefeuille du risque social (dossiers consolidés) — feature de référence portefeuille | Écran unique listant les dossiers du workspace employeur avec exposition estimée par dossier. Feature de référence pour le 'portefeuille' (F-DRH-CHIFFRAGE-08 en est le doublon). Réutilise F-04 (liste dossiers) + le scoring par dossier. Gap réel (acteurs unitaires un dossier à la fois). D8. | concurrent-gap | Hypothèse |
| F-DRH-DASHBOARD-02 | Scoring de priorité des dossiers (triage du risque) | Score de priorité par dossier (urgence calendaire + exposition + risque procédural), tri du portefeuille. Agrège les sorties des outils existants. Situation portefeuille distincte du scoring unitaire (CHIFFRAGE-07). Gap réel. | droit-travail | Hypothèse |
| F-DRH-DASHBOARD-03 | Consolidation du budget contentieux social (provisions justifiées) | Agrège l'exposition du portefeuille en provisions objectivées + honoraires estimés. ⚠️ Recoupe F-DRH-CHIFFRAGE-08. Réutilise les chiffrages dossier (F-DT-*). Besoin DAF/DJ réel. | marche | Hypothèse |
| F-DRH-DASHBOARD-04 | Reporting direction & CSE/DP sur l'état du risque social | Synthèse exportable (PDF) pour direction et CSE. ⚠️ Recoupe partiellement F-DRH-CSE-CONFORM-08 (reporting CSE) et DISCRIM-HARC-09. À distinguer : ici reporting global multi-situations. Réutilise F-DT-04 (export PDF). | concurrent-gap | Hypothèse |
| F-DRH-DASHBOARD-05 | Benchmarking des honoraires avocat par dossier | Vue comparative honoraires vs repères marché. Besoin DAF/DJ réel, non couvert. Lecture seule. | marche | Hypothèse |
| F-DRH-DASHBOARD-06 | Calculateur de ROI / valeur évitée (argumentaire procurement) — feature de référence ROI | Estime la valeur de réduction de risque/coût pour justifier l'ARPU corporate. Feature de référence ROI ; F-DRH-CHIFFRAGE-09 et F-DRH-PREAVOCAT-04 en sont des doublons partiels. | marche | Hypothèse |
| F-DRH-DASHBOARD-07 | Alertes portefeuille — échéances dures & cas de nullité agrégés | Bandeau d'alertes consolidées (prescription, recours, audiences, dossiers à nullité détectée). Agrège F-DT-03 (prescriptions) + F-DT-16 (nullité) au niveau portefeuille. Gap réel portefeuille. D8. | droit-travail | Hypothèse |
| F-DRH-DASHBOARD-08 | Journal d'audit & traçabilité des accès — DOUBLON AUDIT-LOG/AI-ACT (s'appuie sur F-38) | ⚠️ DOUBLON avec le domaine AUDIT-LOG (F-DRH-AUDIT-LOG-01/09) et F-DRH-AI-ACT-07. Réutilise F-38 (audit_logs existant). À porter par AUDIT-LOG. | corporate-readiness | Hypothèse |
| F-DRH-DASHBOARD-09 | Fiche de provision IAS 37 auto-générée consolidée + unitaire — décliné de CHIFFRAGE-11 | Produit la fiche de provision IAS 37 (probabilité × montant + frais, base tracée vers les pièces, datée/signable, réévaluée à chaque clôture) au niveau CONSOLIDÉ (portefeuille). ⚠️ La fiche UNITAIRE par dossier est portée par le générateur de référence CHIFFRAGE-11 ; ici la VALEUR propre = l'AGRÉGATION consolidée pour le DAF/commissaire aux comptes (distincte de DASHBOARD-03 qui agrège le budget). Réutilise CHIFFRAGE-11 (générateur) + scoring DASHBOARD-02 (probabilité). D8. | marche | Hypothèse |
| F-DRH-DASHBOARD-10 | Évolution historique de l'exposition consolidée (courbe inter-clôtures, reporting board) | Suit la variation de l'exposition agrégée et des provisions dans le temps (entre clôtures), courbe de tendance + décomposition (dossiers entrés/sortis, réévaluations, issues). Pilotage récurrent DAS/DAF, ancre la récurrence d'abonnement (NRR D6). Non couvert par les vues instantanées DASHBOARD-01/03. Réutilise les snapshots DASHBOARD-03/09. Lecture seule, exportable. D8. | marche | Hypothèse |
| F-DRH-DASHBOARD-11 | Segmentation du risque par typologie (type de rupture, établissement, population) | Ventile l'exposition consolidée par axes (type de procédure, établissement/site, population/CSP, ancienneté) pour mettre en évidence les zones de fragilité récurrentes et orienter la prévention RH. Distinct de DASHBOARD-02 (scoring par dossier) : agrégation par cohorte. Réutilise métadonnées dossier + chiffrages F-DT-*. Lecture seule. D8 (prévention, jamais ciblage de salariés). | concurrent-gap | Hypothèse |
| F-DRH-DASHBOARD-12 | Réconciliation prédictif vs réalisé (valeur réellement évitée a posteriori) | Rapproche, par dossier clôturé, l'exposition estimée ex-ante (chiffrage/scoring) et l'issue réelle (transaction, condamnation, désistement) pour mesurer la valeur évitée et fiabiliser les chiffrages futurs. Crédibilise le ROI DASHBOARD-06 (ex-ante) avec des données ex-post et atteint la barre de confiance (fiabilité chiffrage, cf. Jobexit). Distinct de DASHBOARD-06. Lecture seule. D8. | concurrent-gap | Hypothèse |

| F-DRH-DASHBOARD-13 | Calendrier critique consolidé du contentieux social (délais durs + clôtures comptables) | Vue calendrier agrégée de tous les dossiers : délais procéduraux durs (prescription disciplinaire 2 mois, entretien préalable, recours/saisine CPH, audiences, consultation CSE) ET échéances de clôture comptable (provisions IAS 37 à réévaluer). Distinct de DASHBOARD-07 (bandeau d'alertes instantané) : ici une timeline planifiable croisant agenda juridique et agenda financier. Agrège F-DT-03 + jalons CSE-CONFORM + dates de clôture DASHBOARD-09/10. Lecture seule, exportable (iCal/PDF). D8. | concurrent-gap | Hypothèse |
| F-DRH-DASHBOARD-14 | Export comptable de la liasse de provisions sociales pour clôture (input DAF / commissaire aux comptes) | Export structuré (CSV/XLSX + PDF signable) à date de clôture de la liasse des provisions pour risque prud'homal : par dossier, probabilité × montant + frais, base tracée vers les pièces, date, variation depuis la clôture précédente. Distinct de DASHBOARD-09 (fiche narrative agrégée) : ici le FORMAT comptable structuré ingérable par l'ERP, opposable au CAC. Distinct de DASHBOARD-03 (budget prévisionnel). Pont DRH→DAF différenciant. Réutilise CHIFFRAGE-11 + DASHBOARD-02 + traçabilité F-38. Lecture seule. D8. | marche | Hypothèse |

**Ajoutées ce run (APPEND 2026-06-06 — run de maturation) :** F-DRH-DASHBOARD-13, -14.
- `F-DRH-DASHBOARD-13` — Conservée — calendrier consolidé croisant agenda juridique et financier, distinct du bandeau d'alertes DASHBOARD-07. decisionTool=false.
- `F-DRH-DASHBOARD-14` — Conservée — export comptable de la liasse de provisions (pont DRH→DAF/CAC), format structuré distinct de la fiche narrative DASHBOARD-09. decisionTool=false.

**Modifiées / justifiées (curation 2026-06-06) :**
- `F-DRH-DASHBOARD-02` — Marquage explicite du sujet UX non tranché (scoring ⊂ outils calculés/persistés, pas champs pré-remplis non cliqués) + filtre portefeuille à arbitrer avant dev. decisionTool=true confirmé.
- `F-DRH-DASHBOARD-01` — Précision du filtrage par situation/zone de risque/montant ; feature de référence portefeuille confirmée (CHIFFRAGE-08, REQUAL-CDD-08, TEMPS-TRAVAIL-10, CSE-CONFORM-08, DISCRIM-HARC-09 = doublons à consolider ici).
- `F-DRH-DASHBOARD-06` — Articulation explicitée avec PRICING-06 (ROI dossier-centric post-usage vs simulateur pricing paramétrique amont) ; feature de référence ROI confirmée (CHIFFRAGE-09, PREAVOCAT-04 = doublons).

**Suppressions proposées (2026-06-06, non appliquées — appliedDeletions vide, D4) :**
- `F-DRH-DASHBOARD-08` — Doublon de la famille audit logs (AUDIT-LOG-01/09 + AI-ACT-07 + CORP-READY-10, base F-38). Le journal d'audit est une capacité transverse corporate-readiness portée par AUDIT-LOG-01 (EXTEND F-38), pas répliquée par domaine.

**Ajoutées ce run (APPEND 2026-06-05) :** F-DRH-DASHBOARD-09, -10, -11, -12.
- `F-DRH-DASHBOARD-09` — Recadrée : la fiche unitaire revient à CHIFFRAGE-11 (générateur de référence) ; DASHBOARD-09 porte uniquement l'AGRÉGATION consolidée (sa valeur distincte). Anti-doublon avec CHIFFRAGE-11. decisionTool=true.
- `F-DRH-DASHBOARD-10` — Conservée — pilotage récurrent dans le temps non couvert par les vues instantanées ; ancre NRR (D6).
- `F-DRH-DASHBOARD-11` — Conservée — segmentation par cohorte non couverte par DASHBOARD-01/02. D8 explicité (anticipation, jamais ciblage).
- `F-DRH-DASHBOARD-12` — Conservée — boucle ex-post de la valeur évitée non couverte ; transforme le ROI d'argument en preuve mesurée.

**Modifiées / justifiées (curation) :**

- `F-DRH-DASHBOARD-09` — Recadrée pour ne porter que l'AGRÉGATION consolidée de la fiche de provision ; la fiche unitaire est déléguée à CHIFFRAGE-11 (générateur de référence) — anti-doublon.
- `F-DRH-DASHBOARD-01` — Désignée référence portefeuille vs CHIFFRAGE-08 ; F-04 réutilisé.
- `F-DRH-DASHBOARD-03` — Recoupement CHIFFRAGE-08 signalé.
- `F-DRH-DASHBOARD-04` — Recoupement reporting CSE (CSE-CONFORM-08/DISCRIM-HARC-09) signalé.
- `F-DRH-DASHBOARD-06` — Désignée référence ROI vs doublons.
- `F-DRH-DASHBOARD-08` — Doublon AUDIT-LOG/AI-ACT ; F-38 réutilisé.

**Suppressions proposées :**

- `F-DRH-DASHBOARD-08` — Doublon de F-DRH-AUDIT-LOG-01/09 + F-DRH-AI-ACT-07 (audit logs). Conserver dans le domaine AUDIT-LOG, basé sur F-38 existant.

---

### Domaine — Mode pré-avocat : structuration & export de dossier (situation-employeur) (`PREAVOCAT`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-DRH-PREAVOCAT-01 | Export structuré du dossier pour l'avocat (dossier de transmission) — situation nouvelle | Document de transmission unique (synthèse faits, chronologie, pièces indexées, chiffrage, points faibles, questions) exportable PDF/DOCX. Réutilise F-10 (synthèse dossier) + F-DT-04/F-98 (génération/PDF) + les chiffrages F-DT-*. Gap réel (mode pré-avocat). D8 : prépare la défense, n'arme pas contre le salarié. | concurrent-gap | Hypothèse |
| F-DRH-PREAVOCAT-02 | Identification des points faibles du dossier (procéduraux & substantiels) — orchestre F-DT-08/16/36 | Cartographie les points faibles avant transmission. ORCHESTRE les détecteurs existants F-DT-08 (validité), F-DT-16 (nullité), F-DT-36 (vices procédure) + signaux substantiels. La valeur = agrégation/synthèse côté employeur, pas re-détection. D8. | concurrent-gap | Hypothèse |
| F-DRH-PREAVOCAT-03 | Brief avocat ciblé : réduction du périmètre de consultation | Génère un brief de questions bornées à partir des points faibles + chiffrage. Réutilise F-98 (génération). Gap réel (pilotage budgétaire). D8. | marche | Hypothèse |
| F-DRH-PREAVOCAT-04 | Chiffrage interne pour objectiver devis et provision — DOUBLON ROI/scoring | ⚠️ Recoupe F-DRH-CHIFFRAGE-07 (scoring) + F-DRH-DASHBOARD-06 (ROI/provision). Agrège les simulateurs F-DT-* existants (n'invente pas de calcul). À consolider. | marche | Hypothèse |
| F-DRH-PREAVOCAT-05 | Constitution du dossier de preuve (sélection & indexation des pièces) — situation nouvelle | Bordereau de communication de pièces, indexation, signalement pièces manquantes. Vérifié non couvert par F-DT. Gap réel. D8 : pure organisation des documents détenus par l'employeur (partie au litige). | concurrent-gap | Hypothèse |
| F-DRH-PREAVOCAT-06 | Journal de contrôle humain joint à l'export — DOUBLON AI-ACT | ⚠️ DOUBLON du cadre transverse AI-ACT (F-DRH-AI-ACT-01). À porter par AI-ACT (le journal s'attache à l'export). | corporate-readiness | Hypothèse |
| F-DRH-PREAVOCAT-07 | Espace de transmission sécurisé DRH ↔ avocat — situation nouvelle | Canal de partage sécurisé (lien borné, traçabilité, révocation) du dossier vers l'avocat externe. ⚠️ ATTENTION D8 : un partage workspace EMPLOYEUR → avocat externe traverse la frontière d'isolation multi-tenant qui FONDE l'invariant anti-conflit. Mécanisme à concevoir comme export borné, jamais comme accès croisé inter-workspaces. Réutilise l'isolation + audit logs (F-38). | corporate-readiness | Hypothèse |
| F-DRH-PREAVOCAT-08 | Volet préparation transactionnelle joint à l'export (fourchette borne basse / borne haute défendable) | Ajoute au dossier de transmission un volet contester/transiger : fourchette de transaction défendable (sécurité juridique ↔ exposition CPH), barème Macron comme référence de négociation, paramètres 2026 (contribution saisine CPH ~50 €, ~67 % d'appel, post-Macron) + licéité RC/transaction. RÉUTILISE les simulateurs F-DT-* et les points faibles de PREAVOCAT-02 — n'invente pas de calcul. ⚠️ Même pattern d'arbitrage que CHIFFRAGE-12 / SANCTION-11 / REQUAL-CDD-11 (situations distinctes) — aligner sur le moteur commun ; la borne/licéité transaction est portée par RUPTURE/transaction. D8. | marche | Hypothèse |
| F-DRH-PREAVOCAT-09 | Export pré-avocat versionné, daté et opposable (artefact de transmission traçable) | Fige chaque dossier de transmission en version horodatée, immuable, traçable (qui a généré/exporté/transmis quoi, quand) + empreinte du périmètre exporté. RÉUTILISE F-37 (versioning/audit trail) + F-38 (audit logs). Répond à la traçabilité opposable (dossier de preuve) + exigence procurement (audit logs accès données salariés). Distinct de PREAVOCAT-01 (contenu) : ici intégrité/horodatage de l'artefact. ⚠️ Recoupe AUDIT-LOG-13 (horodatage de confiance) — à articuler. D8. | corporate-readiness | Hypothèse |
| F-DRH-PREAVOCAT-10 | Boucle de retour avocat sur l'espace de transmission (annotations rapatriées au dossier) — garde-fou D8 | Sur le canal sécurisé PREAVOCAT-07, capture le retour structuré de l'avocat (annotations, demandes de pièces, ajustement chiffrage/stratégie) et le rapatrie de façon bornée au dossier employeur. RÉUTILISE l'isolation multi-tenant + audit logs (F-38) ; le retour est un IMPORT BORNÉ, pas un accès partagé. Comble le trou legal-ops 'pilotage du contentieux'. ⚠️ D8/D7 : import borné STRICT, jamais accès croisé inter-workspaces — l'invariant d'isolation (PLATFORM-06) doit tenir. | marche | Hypothèse |

| F-DRH-PREAVOCAT-11 | Liasse procédurale consolidée jointe à l'export (actes + réponses aux objections regroupés) | Compose en un bloc joint au dossier de transmission la liasse procédurale (actes générés : convocation, lettre de licenciement, notification de sanction, offre de reclassement, protocole transactionnel) + trame de réponses aux objections probables du salarié, dans l'ordre procédural et reliés aux pièces. ORCHESTRE la génération centralisée ACTES (F-98 + F-DT-04) — pas de nouvel acte ni moteur ; vue d'assemblage pour le handoff. Distinct de PREAVOCAT-05 (bordereau de PIÈCES) : ici assemblage des ACTES produits. D8. | concurrent-gap | Hypothèse |
| F-DRH-PREAVOCAT-12 | Export composable multi-vues / multi-destinataires (recomposition avocat ↔ DAF ↔ interne) | Recompose le même dossier de transmission en vues bornées par destinataire : AVOCAT (PREAVOCAT-01 complet), DAF (fiche de provision IAS 37 CHIFFRAGE-11 + fourchette CHIFFRAGE-07), INTERNE/CSE (synthèse non sensible). Chaque vue = sous-ensemble révocable du même artefact versionné (PREAVOCAT-09), sans dupliquer le contenu. Réutilise F-DT-04/F-98 + moteur de chiffrage transverse. Pont DRH↔DAF différenciant. D8 : la vue interne ne raisonne jamais « gagner contre le salarié ». | concurrent-gap | Hypothèse |
| F-DRH-PREAVOCAT-13 | Minimisation RGPD à l'export hors-workspace (pseudonymisation/caviardage sélectif avant transmission au tiers) | Avant tout export franchissant la frontière du workspace EMPLOYEUR vers un tiers (avocat externe PREAVOCAT-07, ou DAF/CSE PREAVOCAT-12), minimisation sélective : pseudonymisation des tiers non parties (témoins, autres salariés cités), caviardage des données sensibles non nécessaires à la finalité « gestion des contentieux et précontentieux » (référentiel RH CNIL). RGPD Art. 5.1.c, gate procurement DPO. RÉUTILISE le pipeline IA (repérage entités), validation humaine obligatoire (journal AI-ACT-01). Distinct de PREAVOCAT-07 (canal) : ici le CONTENU minimisé. D8/D11. | marche | Hypothèse |
| F-DRH-PREAVOCAT-14 | Préparation du bureau de conciliation (BCO) jointe à l'export — note de posture conciliation | Note de posture conciliation en amont de la phase de conciliation prud'homale : positions à proposer/refuser, fourchette (PREAVOCAT-08 + moteur d'arbitrage contester/transiger), pièces à présenter au BCO, points de procédure, paramètres 2026 (contribution saisine CPH ~50 €, ~13,7 mois, ~67 % appel). RÉUTILISE PREAVOCAT-02 + PREAVOCAT-08 + moteur de chiffrage transverse — pas de nouveau calcul. Trou marché (paramétriques/Q&A ne préparent pas le BCO). D8 : productivité défensive. | concurrent-gap | Hypothèse |

**Ajoutées ce run (APPEND 2026-06-06 — run de maturation) :** F-DRH-PREAVOCAT-11, -12, -13, -14.
- `F-DRH-PREAVOCAT-11` — Conservée — liasse procédurale (actes + réponses aux objections) ; assemblage des ACTES, distinct du bordereau de pièces PREAVOCAT-05. decisionTool=false.
- `F-DRH-PREAVOCAT-12` — Conservée — export multi-vues/multi-destinataires (avocat/DAF/interne), sous-ensembles révocables du même artefact versionné. ⚠️ D8 : franchit la frontière d'isolation, export borné/révocable obligatoire. decisionTool=false.
- `F-DRH-PREAVOCAT-13` — Conservée — minimisation RGPD à l'export hors-workspace (Art. 5.1.c), gate DPO. ⚠️ D8/isolation : validation humaine obligatoire. decisionTool=false.
- `F-DRH-PREAVOCAT-14` — Conservée — préparation du bureau de conciliation (BCO), trou marché vs paramétriques. decisionTool=false.

**Suppressions proposées (2026-06-06, non appliquées — appliedDeletions vide, D4) :**
- `F-DRH-PREAVOCAT-04` — Doublon ROI/scoring : recoupe CHIFFRAGE-07 (scoring) + DASHBOARD-06 (ROI de référence). Chiffrage interne porté par le moteur transverse, lu en mode pré-avocat via PREAVOCAT-01. Non appliqué (D4 — touche le périmètre ROI/scoring, décision PO) ; appliedDeletions vide.
- `F-DRH-PREAVOCAT-06` — Doublon du cadre transverse AI-ACT-01 (journal de contrôle humain), attaché transversalement aux exports. Non appliqué (D4 — matérialisation transverse PO) ; appliedDeletions vide.

**Ajoutées ce run (APPEND 2026-06-05) :** F-DRH-PREAVOCAT-08, -09, -10.
- `F-DRH-PREAVOCAT-08` — Conservée — besoin marché 'borne haute/basse défendable' + paramètres 2026. Aligné explicitement avec le pattern d'arbitrage contester/transiger (situation distincte = volet joint à l'export). decisionTool=true.
- `F-DRH-PREAVOCAT-09` — Conservée — traçabilité opposable de l'artefact exporté (RGPD + procurement). Recoupement AUDIT-LOG-13 signalé.
- `F-DRH-PREAVOCAT-10` — Conservée — complète PREAVOCAT-07 (sortie) par le retour entrant borné. Risque D8 explicité (jamais accès croisé).

**Modifiées / justifiées (curation) :**

- `F-DRH-PREAVOCAT-01` — Désignée feature de référence du pattern export pré-avocat (INAPT-14, TEMPS-TRAVAIL-09, REQUAL-CDD-07 = déclinaisons/doublons) pour cohérence inter-domaines.
- `F-DRH-PREAVOCAT-08` — Aligné avec le pattern d'arbitrage contester/transiger (CHIFFRAGE-12, SANCTION-11, REQUAL-CDD-11) ; situation distincte (volet joint à l'export).
- `F-DRH-PREAVOCAT-02` — Recadré : orchestre F-DT-08/16/36 (détection déjà existante).
- `F-DRH-PREAVOCAT-04` — Doublon CHIFFRAGE-07/DASHBOARD-06 signalé.
- `F-DRH-PREAVOCAT-06` — Doublon AI-ACT signalé.
- `F-DRH-PREAVOCAT-07` — Risque D8 (frontière isolation multi-tenant) souligné.

**Suppressions proposées :**

- `F-DRH-PREAVOCAT-06` — Doublon du cadre transverse AI-ACT (journal de contrôle humain). Le journal s'attache transversalement aux exports.
- `F-DRH-PREAVOCAT-04` — Doublon ROI/scoring : recoupe CHIFFRAGE-07 (scoring d'exposition) + DASHBOARD-06 (feature de référence ROI). Le chiffrage interne est porté par le moteur de chiffrage transverse, lu en mode pré-avocat via PREAVOCAT-01.

---

### Domaine — Génération d'actes & courriers RH conformes (`situation-employeur`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-DRH-ACTES-01 | Générateur lettre licenciement motif personnel — dossier-centric (réutilise F-DT-04 + F-98, chaîne F-DT-08/36) | Génère la lettre motivée prête à envoyer. Réutilise F-DT-04 (génération PDF) + F-98 (génération courrier/conclusions) et CHAÎNE F-DT-08 (validité) + F-DT-36 (nullités procédure) comme garde-fous amont — comme déjà annoncé dans la description d'origine. Feature de référence vs doublon SECU-PROC-05. D8. | droit-travail | Hypothèse |
| F-DRH-ACTES-02 | Générateur lettre licenciement économique (réutilise F-98 + F-DT-04, chaîne F-DT-13/14) | Génère la lettre éco opposable (cause éco, critères d'ordre, reclassement, priorité réembauche). CHAÎNE F-DT-13 (licenciement éco détaillé) + F-DT-14 (PSE) comme contrôle de fond. Réutilise F-98/F-DT-04. D8. | droit-travail | Hypothèse |
| F-DRH-ACTES-03 | Générateur notification de sanction disciplinaire — feature de référence (cf. SANCTION-06/SECU-PROC-06) | Notification de sanction avec contrôle de proportionnalité (issu de F-DRH-SANCTION-02) et garde-fou calendaire (F-DRH-SANCTION-03/SECU-PROC-03). Feature de référence ; SANCTION-06 et SECU-PROC-06 sont des doublons. Réutilise F-98. | concurrent-gap | Hypothèse |
| F-DRH-ACTES-04 | Générateur convocation entretien préalable — feature de référence (cf. SECU-PROC-07) | Convocation avec calcul des délais durs + mention assistance. Feature de référence ; SECU-PROC-07 est un doublon. Réutilise F-98. | droit-travail | Hypothèse |
| F-DRH-ACTES-05 | Générateur proposition(s) de reclassement — feature de référence (cf. SECU-PROC-08/INAPT-03) | Génère les propositions de reclassement (inaptitude/éco) avec recherche documentée. Feature de référence acte ; aval de F-DRH-INAPT-03 (assistant reclassement). SECU-PROC-08 = doublon. Réutilise F-98. | concurrent-gap | Hypothèse |
| F-DRH-ACTES-06 | Générateur convocation entretien reclassement / inaptitude (réutilise F-98) | Convocation liée à la procédure inaptitude, articulée avec la consultation CSE (domaine CSE-CONFORM). Réutilise F-98. ⚠️ Articuler avec CSE-CONFORM-03 (génération convocation CSE) pour ne pas dupliquer la trame CSE. | droit-travail | Hypothèse |
| F-DRH-ACTES-07 | Générateur protocole transactionnel & RC — feature de référence (cf. SECU-PROC-09 ; réutilise F-DT-31/F-DT-10) | Génère protocole transactionnel + RC, garde-fous F-DT-31 (validité transaction) + F-DT-10 (validité RC) + régime 2 PASS (F-DRH-CHIFFRAGE-05). Feature de référence ; SECU-PROC-09 = doublon. Réutilise F-98. | marche | Hypothèse |
| F-DRH-ACTES-08 | CCN-aware dans les actes générés — recoupe F-DT-07 + capacité transverse CCN | Injecte la CCN dans motivation/calculs des actes. ⚠️ CCN-aware déjà existant (F-DT-07) et répété dans 7 domaines. À rattacher à UNE capacité transverse CCN-aware (cf. SANCTION-08), pas une feature par domaine. | concurrent-gap | Hypothèse |
| F-DRH-ACTES-09 | Conformité AI Act intégrée à la génération d'actes — DOUBLON AI-ACT | ⚠️ DOUBLON du cadre transverse AI-ACT (F-DRH-AI-ACT-01/02). À porter par AI-ACT. | corporate-readiness | Hypothèse |
| F-DRH-ACTES-10 | Vérification de licéité 'prêt à envoyer' avant émission (gate anti-vice transverse) — situation nouvelle | Contrôle bloquant transverse avant export d'acte (mentions, délais, cohérence motif↔procédure↔chiffrage, nullité). ORCHESTRE F-DT-08/16/36 + les checklists. Gate transverse aux générateurs ACTES. Gap réel. Besoin marché central (acte opposable sans relecture avocat). | marche | Hypothèse |
| F-DRH-ACTES-11 | Générateur des documents de fin de contrat (certificat de travail, reçu pour solde de tout compte, attestation employeur France Travail) — situation nouvelle | Génère le « kit de sortie » légalement obligatoire à remettre au salarié à la rupture (quel que soit le motif), pré-rempli depuis le dossier (ancienneté, postes, qualif, chiffrage de la rupture déjà calculé). Vérifié non couvert (aucun F-DT, aucun domaine DRH ne traite les documents de fin de contrat ; ACTES-01→10 couvrent la décision/notification, pas la remise post-rupture). Aucun concurrent ne les produit dossier-centric (Jobexit chiffre, Tissot/Lefebvre = modèles paramétriques). Réutilise F-98 (génération courrier) + F-DT-04 (PDF) + le chiffrage rupture (F-DRH-CHIFFRAGE). D8 : obligation légale employeur, pas arme anti-salarié. | concurrent-gap | Hypothèse |
| F-DRH-ACTES-12 | Génération en lot des actes de licenciement économique collectif (critères d'ordre appliqués par salarié) — situation nouvelle | Génère la série de lettres éco pour un licenciement collectif, en appliquant les critères d'ordre du licenciement individuellement à chaque salarié sélectionné et en répliquant les garde-fous de F-DRH-ACTES-02. Répond au besoin marché « tâches répétitives à fort volume » (CNB ~65 % de gain sur la rédaction d'actes) et au contexte PSE (F-DT-14). Vérifié non couvert (ACTES-02 = un acte ; aucune génération en lot dans le draft). Réutilise F-98 + F-DT-04 ; CHAÎNE F-DT-13/14. Gate ACTES-10 appliqué à chaque acte de la série. D8 : conformité du processus collectif, pas ciblage. | marche | Hypothèse |
| F-DRH-ACTES-13 | Traçabilité horodatée et versionnage des actes générés — recoupe AUDIT-LOG + AI-ACT + PLATFORM-08 | Conserve chaque acte généré avec version, horodatage, motif, paramètres, opérateur → fiche d'acte datée/signée opposable, alimentant la fiche de provision (CHIFFRAGE-11) et la piste d'audit RGPD. ⚠️ Recoupe l'audit logs corporate-readiness (AUDIT-LOG/F-38), le journal AI-ACT et la provenance PLATFORM-08 — à BRANCHER sur ces capacités transverses, PAS réimplémenter. La valeur propre = versionnage des actes (absent du draft). | corporate-readiness | Hypothèse |
| F-DRH-ACTES-14 | Cohérence de la liasse procédurale (séquence d'actes d'une même procédure, anti-contradiction) — situation nouvelle | Au-delà du gate par acte (ACTES-10), contrôle la cohérence transverse de la SÉQUENCE d'actes d'une même procédure (convocation ACTES-04 → notification ACTES-01/02/03 → documents de fin ACTES-11) : dates ordonnées, motif identique d'un acte à l'autre, chiffrage cohérent, pas de mention contradictoire. Vérifié non couvert (ACTES-10 contrôle un acte isolé). Différenciant fort vs wizards paramétriques mono-acte des éditeurs. D8 : sécurisation procédurale, conformité. | concurrent-gap | Hypothèse |
| F-DRH-ACTES-15 | Générateur des « autres courriers RH » à effet juridique (mise en demeure, rappel à l'ordre, lettre de mise à pied conservatoire) | Génère la famille de courriers RH à portée juridique non couverte par le draft : mise en demeure (exécution d'une obligation), rappel à l'ordre / observation écrite (mesure NON disciplinaire, à distinguer de la sanction ACTES-03 pour éviter la requalification), lettre de mise à pied conservatoire concomitante aux poursuites. Pré-rempli depuis le dossier. Vérifié non couvert : ACTES-03 = notification de sanction ; SECU-PROC-16 SÉCURISE la mise à pied (concomitance) mais ne PRODUIT pas la lettre. Aval de SECU-PROC-16 + SANCTION-01 ; gate ACTES-10 ; liasse ACTES-14. Réutilise F-98 + F-DT-04 + CCN-aware (ACTES-08). D8 : éviter la requalification abusive, jamais arme anti-salarié. | droit-travail | Hypothèse |
| F-DRH-ACTES-16 | Module de notification & preuve de remise opposable des actes (LRAR / remise contre décharge / horodatage de présentation) | Aval de la génération : organise et trace l'ÉMISSION effective de l'acte (LRAR, remise en main propre contre décharge, horodatage de première présentation) et capture la preuve de notification — étape qui rend l'acte opposable et fait courir les délais (préavis, rétractation RC, délais disciplinaires). Vérifié non couvert : ACTES-10 = gate AVANT envoi, ACTES-13 = versionnage, mais AUCUNE feature ne gère la NOTIFICATION ni la preuve de remise. Branche sur ACTES-13 + AUDIT-LOG/F-38 + calendrier SECU-PROC-03 + fiche provision CHIFFRAGE-11. NE réimplémente PAS l'audit log : valeur propre = preuve de remise + calage du point de départ des délais. D8. | concurrent-gap | Hypothèse |

**Ajoutées ce run (APPEND 2026-06-06 — run de maturation) :** F-DRH-ACTES-15, -16.
- `F-DRH-ACTES-15` — Conservée — famille « autres courriers RH » à effet juridique (mise en demeure, rappel à l'ordre, mise à pied conservatoire), non couverte par ACTES-03 ni SECU-PROC-16. decisionTool=false.
- `F-DRH-ACTES-16` — Conservée — notification & preuve de remise opposable (LRAR/décharge/horodatage), cale le point de départ des délais ; non couvert par ACTES-10/13. decisionTool=false.

**Suppressions proposées (2026-06-06, non appliquées — appliedDeletions vide, D4) :**
- `F-DRH-ACTES-09` — Doublon du cadre transverse AI-ACT (AI-ACT-01/02) : conformité AI Act de la génération d'actes portée une seule fois par AI-ACT. Maintenue (proposée aux runs précédents).

**Modifiées / justifiées (curation) :**

- `F-DRH-ACTES-01` — Référence vs SECU-PROC-05 ; F-DT-04/F-98 + chaîne F-DT-08/36 ; decisionTool retiré.
- `F-DRH-ACTES-02` — Chaîne F-DT-13/14 explicitée ; decisionTool retiré.
- `F-DRH-ACTES-03` — Référence vs SANCTION-06/SECU-PROC-06 ; decisionTool retiré.
- `F-DRH-ACTES-04` — Référence vs SECU-PROC-07 ; decisionTool retiré.
- `F-DRH-ACTES-05` — Référence acte reclassement vs SECU-PROC-08 ; decisionTool retiré.
- `F-DRH-ACTES-06` — Articulation CSE-CONFORM-03 signalée.
- `F-DRH-ACTES-07` — Référence vs SECU-PROC-09 ; F-DT-31/10 réutilisés ; decisionTool retiré.
- `F-DRH-ACTES-08` — CCN-aware existant (F-DT-07) dupliqué 7x — unification signalée.
- `F-DRH-ACTES-09` — Doublon AI-ACT signalé.
- `F-DRH-ACTES-10` — decisionTool retiré (gate de contrôle, pas outil décisionnel).
- `F-DRH-ACTES-13` — (run 2026-06-05) Recadrée comme branchement sur les capacités transverses (AUDIT-LOG/F-38, AI-ACT, PLATFORM-08) au lieu de réimplémenter ; valeur propre = versionnage des actes.

**Suppressions proposées :**

- `F-DRH-ACTES-09` — Doublon du cadre transverse AI-ACT (conformité génération d'actes).

---

### Domaine — Requalification CDD → CDI & chiffrage du risque (`REQUAL-CDD`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-DRH-REQUAL-CDD-01 | Détection des motifs de requalification (lecture employeur de F-DT-22 + F-DT-23) | Détecte les motifs de requalification depuis les pièces. F-DT-22 (requalification CDD→CDI) + F-DT-23 (intérim→CDI) existent déjà côté avocat. LIT ces moteurs côté employeur ; la VALEUR = lecture « mon risque » + rattachement pièces, pas un nouveau détecteur. D8. | plateforme-reutilisee | Hypothèse |
| F-DRH-REQUAL-CDD-02 | Lecture employeur du chiffrage de requalification CDD→CDI (réutilise F-DT-22 + F-DT-17) | Chiffre l'exposition requalification (indemnité ≥ 1 mois, rappels interstitiels, ancienneté rétroactive, rupture→barème via F-DT-01). F-DT-22 chiffre déjà la requalification ; F-DT-17 (précarité CDD) complète. LIT côté employeur. D8. | plateforme-reutilisee | Hypothèse |
| F-DRH-REQUAL-CDD-03 | Détection du risque pénal employeur CDD (L1243-4/L1248) — situation nouvelle | Détecte l'exposition pénale du CDD irrégulier. Vérifié : aucun F-DT ne traite la dimension pénale (cf. F-PE-01 backlog pénal hors V1). Gap réel côté employeur. ⚠️ Cohérence : le pénal est cadré hors V1 (F-PE-01) ; cette détection doit rester une ALERTE d'exposition, pas un outil de droit pénal complet. D8. | droit-travail | Hypothèse |
| F-DRH-REQUAL-CDD-04 | Scoring d'exposition requalification — décliné de CHIFFRAGE-07 | Score d'exposition requalification. ⚠️ Recoupe le moteur de scoring générique CHIFFRAGE-07. À décliner (config par situation), pas dupliquer. Garde-fou 'conclusions ⊂ outils calculés' bien repris. ⚠️ UX NON TRANCHÉ (à arbitrer avant dev, ne PAS résoudre en silence) : le score ne s'appuie QUE sur les outils CALCULÉS/persistés (clic « Calculer » → résultat), PAS sur les champs pré-remplis non cliqués (REQUAL-02 pré-rempli mais non calculé → exposition sous-estimée sans alerte). 3 options ouvertes : (a) alerte « N outils requalification pré-remplis non calculés » avant le scoring ; (b) pré-calcul auto des outils requalification pré-remplis ; (c) laisser tel quel. Mêmes options que CHIFFRAGE-07 et tous les scorings d'exposition — décision PO transverse. | marche | Hypothèse |
| F-DRH-REQUAL-CDD-05 | CCN-aware dans le chiffrage requalification — recoupe F-DT-07 + capacité transverse | ⚠️ CCN-aware déjà existant (F-DT-07), répété 7x. À unifier en capacité transverse. | concurrent-gap | Hypothèse |
| F-DRH-REQUAL-CDD-06 | Plan d'action de régularisation préventive (anti-requalification) — situation nouvelle | Plan d'action de mise en conformité (passage CDI, correction motifs/durées/carences) + génération actes via F-98. Vérifié non couvert. Gap réel. D8 strict : sécuriser, jamais priver le salarié de droits acquis. | droit-travail | Hypothèse |
| F-DRH-REQUAL-CDD-07 | Note d'exposition exportable mode pré-avocat — DOUBLON PREAVOCAT-01 | ⚠️ DOUBLON avec F-DRH-PREAVOCAT-01 (export structuré dossier). Le mode pré-avocat est un domaine dédié (PREAVOCAT). À consolider : ce besoin est porté par PREAVOCAT, décliné par situation. | marche | Hypothèse |
| F-DRH-REQUAL-CDD-08 | Vue portefeuille du risque de requalification — DOUBLON DASHBOARD | ⚠️ DOUBLON avec F-DRH-DASHBOARD-01 (vue portefeuille). Le portefeuille est un domaine dédié. À consolider (filtre/vue requalification du dashboard). | concurrent-gap | Hypothèse |
| F-DRH-REQUAL-CDD-09 | Journal contrôle humain & AIPD requalification — DOUBLON AI-ACT | ⚠️ DOUBLON du cadre transverse AI-ACT. À porter par AI-ACT. | corporate-readiness | Hypothèse |
| F-DRH-REQUAL-CDD-10 | Lecture employeur jurisprudence requalification (réutilise F-JU-01) | LIT côté employeur F-JU-01 appliqué à la requalification. ⚠️ Pattern jurisprudence répété 6x. À unifier en capacité plateforme. | plateforme-reutilisee | Hypothèse |
| F-DRH-REQUAL-CDD-11 | Aide à la décision « contester vs régulariser/transiger » sur une demande de requalification (intègre contribution saisine CPH ~50 € 2026 + risque d'appel ~67 %) | Outil décisionnel propre à la situation « le salarié saisit (ou peut saisir) le CPH en requalification ». Compare le coût attendu de la contestation (aléa × exposition requalification = rappels interstitiels + ancienneté rétroactive + rupture barème, frais avocat ≥ 4 500 € HT/CPH, durée ~13,7 mois, risque d'appel ~67 %, nouvelle contribution de saisine ~50 € mars 2026) au coût d'une régularisation/transaction bornée (barème Macron comme référence de négociation). Aucun calculateur paramétrique (Jobexit, simulateurs Macron) ne raisonne sur l'aléa du dossier réel ni n'intègre ces paramètres 2026. Situation métier distincte de REQUAL-04 (scoring d'exposition) et de CHIFFRAGE (provision) : ici on arbitre une action. Réutilise les chiffrages F-DT-22/17/01 et le scoring CHIFFRAGE-07 en entrée. D8 : aide la décision conformité/risque, ne « gagne pas contre le salarié ». ⚠️ Cohérence : la borne de transaction et le contrôle de licéité RC/transaction restent portés par les domaines RUPTURE/transaction — ici, vue requalification uniquement. | marche | Hypothèse |
| F-DRH-REQUAL-CDD-12 | Chronologie automatique des contrats successifs & carences inter-contrats (parité Jobexit upload, alimente la détection) | Reconstitue depuis les pièces (contrats CDD, avenants, bulletins) la chronologie des contrats successifs : dates de début/fin, motifs, délais de carence, périodes interstitielles, dépassement durée max. Sert d'INTRANT factuel à REQUAL-01 (motifs « conflit de succession », défaut de carence, terme imprécis) et à REQUAL-02 (assiette des rappels interstitiels / ancienneté rétroactive). Comble la parité avec Jobexit (drag-and-drop bulletins → auto-remplissage) tout en restant dossier-centric (extraction + qualification, pas simple pré-remplissage de variables). Situation d'extraction distincte des détecteurs. ⚠️ Réutilise le pipeline IA chunk→document→dossier (D3) ; ne pas réimplémenter l'extraction générique — déclinaison de la capacité d'extraction sur les contrats successifs. | concurrent-gap | Hypothèse |
| F-DRH-REQUAL-CDD-13 | Indicateur de fiabilité & traçabilité du chiffrage de requalification — décliné de CHIFFRAGE-14/PLATFORM-08 | Affiche sur REQUAL-02 un indicateur de fiabilité : sources de chaque montant (pièce / CCN F-DT-07 / barème), hypothèses, champs « non factualisables », horodatage. Atteint la barre de confiance (montant brut garanti Jobexit, D10). Distinct du journal AI-ACT (traçabilité IA réglementaire). ⚠️ DÉCLINAISON de la capacité transverse « traçabilité du chiffrage » (CHIFFRAGE-14 / PLATFORM-08) — à BRANCHER dessus, PAS réimplémenter. | marche | Hypothèse |
| F-DRH-REQUAL-CDD-14 | Fiche de provision IAS 37 du risque de requalification (input DAF, datée/signée) — décliné de CHIFFRAGE-11 | Produit la fiche de provision comptable spécifique au risque de requalification : probabilité de condamnation (scoring REQUAL-04) × exposition estimée (REQUAL-02 = rappels interstitiels + ancienneté rétroactive + rupture barème via F-DT-22/17/01) + frais, datée et signable, réévaluable à chaque clôture. Répond à l'obligation IAS 37 de provisionner dès que le litige est probable, dossier par dossier — le DAS produit aujourd'hui cet input à la main, sans moteur d'aléa rattaché au dossier (trou marché confirmé). ⚠️ DÉCLINAISON du générateur de référence CHIFFRAGE-11 (1 générateur, N configurations de scénario) — NE PAS recréer de moteur ; configuration « scénario requalification ». Distinct de REQUAL-11 (arbitrage contester/transiger) et de REQUAL-04 (scoring brut). Réutilise F-DT-04 (export PDF) + F-37 (versioning). D8 : conformité comptable, jamais « armer contre le salarié ». | marche | Hypothèse |
| F-DRH-REQUAL-CDD-15 | Explicabilité des motifs de requalification détectés (pièce + base de droit, opposable) — décliné de PLATFORM-08 | Pour chaque motif remonté par REQUAL-01 (absence d'écrit, terme/objet imprécis ou fictif, défaut de carence, succession/enchaînement, dépassement durée max, raison véritable du recours absente), restitue la justification factuelle : la/les pièce(s) source(s) (via REQUAL-12 chronologie), l'article du Code du travail mobilisé, et le degré de certitude (avéré / probable / à vérifier). Transforme un détecteur « boîte noire » en alerte opposable au DPO/Achats et exploitable par le DRH pour décider. Comble la limite des concurrents (Jobexit pré-remplit des variables sans qualifier juridiquement ; les assistants Q&A ne partent pas des pièces). ⚠️ DÉCLINAISON de la capacité transverse de provenance « factualisé depuis les pièces » (PLATFORM-08) appliquée aux MOTIFS (vs REQUAL-13 = traçabilité des MONTANTS) — à brancher dessus, pas un second moteur. Renvoie le journal de contrôle humain au cadre AI-ACT. D8. | concurrent-gap | Hypothèse |
| F-DRH-REQUAL-CDD-16 | Note de scope juridiction du domaine requalification : V1 = FR seul ; BE = backlog différé | Note de cadrage (non un outil) verrouillant le périmètre juridiction du domaine : V1 couvre la requalification de droit FRANÇAIS uniquement (CDD art. L1242-* / L1243-*, intérim, CPH, barème Macron, sanction pénale L1243-4/L1248). Le droit social BELGE de la requalification (régime des contrats à durée déterminée successifs, sanctions propres) est explicitement DIFFÉRÉ au backlog — couverture exhaustive attendue à partir des sources BE, PAS un miroir FR (cf. invariant projet « Belgique never forget »). Évite la fausse exhaustivité FR-centric : tant que BE n'est pas développé, REQUAL-01→15 sont à lire « périmètre FR ». ⚠️ Aligne ce domaine sur la note de scope juridiction transverse (même verrou que les autres domaines DRH). Aucun dev tant que le verrou d'activation marché n'est pas atteint. | vision-po | Hypothèse |

**Modifiées / justifiées (curation) :**

- `F-DRH-REQUAL-CDD-01` — Reclassé plateforme-reutilisee (lit F-DT-22/23) ; INCOHÉRENCE 'créneau vide' corrigée.
- `F-DRH-REQUAL-CDD-02` — Reclassé plateforme-reutilisee (lit F-DT-22/17), decisionTool retiré.
- `F-DRH-REQUAL-CDD-03` — Cohérence F-PE-01 (pénal hors V1) — rester au niveau alerte.
- `F-DRH-REQUAL-CDD-04` — Recoupement scoring CHIFFRAGE-07 signalé.
- `F-DRH-REQUAL-CDD-05` — CCN-aware existant dupliqué — unification signalée.
- `F-DRH-REQUAL-CDD-07` — Doublon PREAVOCAT-01 signalé.
- `F-DRH-REQUAL-CDD-08` — Doublon DASHBOARD-01 signalé.
- `F-DRH-REQUAL-CDD-09` — Doublon AI-ACT signalé.
- `F-DRH-REQUAL-CDD-10` — Réutilise F-JU-01 ; pattern répété signalé.
- `F-DRH-REQUAL-CDD-11` — APPEND (run 2026-06-05). Gap marché : aide à la décision contester/régulariser/transiger intégrant la contribution de saisine CPH ~50 € (mars 2026) et le risque d'appel ~67 % — qu'aucun calculateur paramétrique ne propose finement sur le dossier réel. Situation d'arbitrage distincte du scoring (REQUAL-04) et de la provision (CHIFFRAGE).
- `F-DRH-REQUAL-CDD-12` — APPEND (run 2026-06-05). Gap concurrent : parité Jobexit (upload bulletins/contrats → auto-remplissage) tournée en extraction dossier-centric de la chronologie des contrats successifs + carences ; intrant factuel de REQUAL-01/02. Décline le pipeline IA (D3), ne réimplémente pas l'extraction.
- `F-DRH-REQUAL-CDD-13` — APPEND (run 2026-06-05). Gap marché : indicateur de fiabilité/traçabilité du chiffrage = barre d'achetabilité DRH posée par la garantie contractuelle Jobexit (D10). Distinct du journal AI-ACT (réglementaire) ; à brancher sur la traçabilité transverse, décliné requalification.
- `F-DRH-REQUAL-CDD-13` — (run 2026-06-05, recadrage) Recadrée comme DÉCLINAISON requalification de la traçabilité du chiffrage transverse (CHIFFRAGE-14/PLATFORM-08) — anti-doublon, distinct du journal AI-ACT.
- `F-DRH-REQUAL-CDD-04` — MODIFIÉE (run 2026-06-06). Ajout du flag UX NON TRANCHÉ « scoring ⊂ outils CALCULÉS/persistés, pas les champs pré-remplis non cliqués » avec les 3 options ouvertes (alerte / pré-calcul auto / laisser tel quel). changeReason = directive PO run 3 (uxNonTranche) : marquer explicitement le sujet, ne pas le résoudre en silence ; aligné sur CHIFFRAGE-07 et tous les scorings d'exposition.
- `F-DRH-REQUAL-CDD-14` — APPEND (run 2026-06-06). Gap marché : fiche de provision IAS 37 spécifique au risque de requalification, listée comme situation explicite (« Fiche provision IAS 37 requalification »). Déclinaison du générateur de référence CHIFFRAGE-11 (1 générateur, N scénarios) — anti-doublon. Distincte du scoring (REQUAL-04) et de l'arbitrage (REQUAL-11). Comble le trou « le DAS produit l'input à la main, sans moteur d'aléa rattaché au dossier ».
- `F-DRH-REQUAL-CDD-15` — APPEND (run 2026-06-06). Gap concurrent : explicabilité juridique des motifs détectés (pièce source + article + degré de certitude). Différencie de Jobexit (pré-remplit sans qualifier) et des assistants Q&A (ne partent pas des pièces). Déclinaison de PLATFORM-08 appliquée aux MOTIFS (vs REQUAL-13 = traçabilité des MONTANTS) — anti-doublon. Rend le détecteur opposable DPO/Achats.
- `F-DRH-REQUAL-CDD-16` — APPEND (run 2026-06-06). Directive PO run 3 (scopeBE) : note de scope juridiction verrouillant V1 = FR seul, BE = backlog différé (sources BE, pas miroir FR). Évite la fausse exhaustivité FR-centric. Note de cadrage, pas un outil (decisionTool=false).

**Suppressions proposées :**

- `F-DRH-REQUAL-CDD-09` — Doublon du cadre transverse AI-ACT.

---

### Domaine — Égalité F/H & prévention discrimination / harcèlement (situation employeur) (`DISCRIM-HARC`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-DRH-DISCRIM-HARC-01 | Lecture employeur de la détection d'indicateurs de discrimination (réutilise F-DT-12 + F-DT-16) | Détecte les indices de discrimination dans les pièces. F-DT-12 (discrimination FR+BE) + F-DT-16 (licenciement nul, dont discrimination) existent déjà. LIT côté employeur « mon exposition ». Aucun nouveau détecteur. D8. | plateforme-reutilisee | Hypothèse |
| F-DRH-DISCRIM-HARC-02 | Évaluation de l'écart de traitement vs panel (comparateurs) — situation nouvelle (étend F-DT-12) | Reconstitue l'écart vs pairs (faisceau d'indices). Vérifié : F-DT-12 traite la discrimination/indemnité mais le comparateur de panel (salaire/carrière/promotions) est une situation distincte légitime. À brancher sur F-DT-12. D8. | droit-travail | Hypothèse |
| F-DRH-DISCRIM-HARC-03 | Lecture employeur du chiffrage harcèlement/discrimination nullité (réutilise F-DT-11 + F-DT-12 + F-DT-16) | Chiffre l'exposition cas de nullité (plancher 6 mois, barème écarté, indemnités aggravées, réintégration). F-DT-11 (harcèlement → indemnités + licenciement nul) + F-DT-12 + F-DT-16 couvrent déjà. LIT côté employeur. D8. | plateforme-reutilisee | Hypothèse |
| F-DRH-DISCRIM-HARC-04 | Scoring d'exposition discrim/harcèlement — décliné de CHIFFRAGE-07 | ⚠️ Recoupe le scoring générique CHIFFRAGE-07. À décliner, pas dupliquer. | marche | Hypothèse |
| F-DRH-DISCRIM-HARC-05 | CCN-aware chiffrage indemnités aggravées — recoupe F-DT-07 | ⚠️ CCN-aware existant (F-DT-07), répété 7x. À unifier. | concurrent-gap | Hypothèse |
| F-DRH-DISCRIM-HARC-06 | Audit d'égalité professionnelle F/H & index Pénicaud (loi Rixain) — situation nouvelle | Calcule/contrôle l'index égalité F/H + quotas Rixain → rapport de conformité. Vérifié non couvert (conformité organisationnelle ≠ contentieux unitaire). Gap réel. D8. | marche | Hypothèse |
| F-DRH-DISCRIM-HARC-07 | Checklist anti-vice enquête harcèlement interne — situation nouvelle | Checklist bloquante de l'enquête harcèlement (impartialité, contradictoire, CSE, délais, mesures conservatoires) + trame. Vérifié non couvert par F-DT. Gap réel. D8. | concurrent-gap | Hypothèse |
| F-DRH-DISCRIM-HARC-08 | Génération d'actes liés au harcèlement/discrimination — DOUBLON ACTES (réutilise F-98) | ⚠️ Génération d'actes : à porter par le domaine ACTES (rapport enquête, notification, lettre licenciement auteur, protocole). Réutilise F-98 + F-DT-04. Le plan de remédiation égalité est l'aval de DISCRIM-HARC-06. | concurrent-gap | Hypothèse |
| F-DRH-DISCRIM-HARC-09 | Vue portefeuille discrim/harcèlement & reporting — DOUBLON DASHBOARD | ⚠️ DOUBLON avec F-DRH-DASHBOARD-01/04. À consolider (filtre/vue du dashboard). | concurrent-gap | Hypothèse |
| F-DRH-DISCRIM-HARC-10 | Journal contrôle humain & AIPD/FRIA anti-biais — DOUBLON AI-ACT (durcissement anti-biais) | ⚠️ DOUBLON du cadre AI-ACT (F-DRH-AI-ACT-01/02/05). La spécificité anti-biais discrimination = enrichissement à porter par F-DRH-AI-ACT-05 (registre gouvernance biais), pas une feature domaine distincte. | corporate-readiness | Hypothèse |
| F-DRH-DISCRIM-HARC-11 | Lecture employeur jurisprudence discrimination/harcèlement (réutilise F-JU-01) | LIT côté employeur F-JU-01 (faisceau d'indices, charge de la preuve aménagée). ⚠️ Pattern jurisprudence répété 6x — unifier. | plateforme-reutilisee | Hypothèse |
| F-DRH-DISCRIM-HARC-12 | Note d'exposition exportable mode pré-avocat — DOUBLON PREAVOCAT-01 | ⚠️ DOUBLON avec F-DRH-PREAVOCAT-01. À consolider dans PREAVOCAT. | marche | Hypothèse |
| F-DRH-DISCRIM-HARC-13 | Checklist obligation de prévention documentée (DUERP RPS + formation + dispositif de signalement) | Checklist bloquante de l'obligation de prévention employeur : DUERP incluant le risque harcèlement/discrimination documenté/daté, plan d'actions, formation managers, affichage (L1153-5), référent harcèlement sexuel CSE, dispositif de signalement. La jurisprudence repose sur le manquement à l'obligation de sécurité de résultat : sans prévention documentée, l'employeur est présumé fautif. Non couvert par F-DT (contentieux unitaire, pas la documentation amont). Distinct de HARC-07 (enquête, aval post-signalement). Sortie : checklist + trame DUERP volet RPS + pièces manquantes. D8. | marche | Hypothèse |
| F-DRH-DISCRIM-HARC-14 | Reporting égalité F/H grand compte (CSRD / ESRS S1) — distinct de l'index Rixain | Produit les indicateurs ESRS S1 (écarts de rémunération, diversité instances dirigeantes, incidents de discrimination/harcèlement et mesures correctives) à partir des données/dossiers du workspace. Distinct de HARC-06 (index Pénicaud + quotas Rixain = obligation légale FR unitaire) : ici alimentation du rapport de durabilité, critère d'achat grand compte. Réutilise les agrégats de HARC-06 et du dashboard (pas de doublon de calcul). Sortie : tableau ESRS S1 exportable + note méthodologique. D8. | marche | Hypothèse |
| F-DRH-DISCRIM-HARC-15 | Fiche de provision IAS 37 spécifique nullité discrim/harcèlement (réintégration + indemnités aggravées) — décliné de CHIFFRAGE-11 | Décline la fiche de provision (CHIFFRAGE-11 / IAS 37) sur la spécificité du risque de nullité : plancher 6 mois écartant Macron, indemnités aggravées, coût de réintégration (salaires rétroactifs) qui distingue l'exposition discrim/harcèlement d'un licenciement abusif ordinaire. Lit HARC-03 (chiffrage nullité) + F-DT-16. ⚠️ DÉCLINAISON du générateur de provision de référence CHIFFRAGE-11 (1 outil = 1 situation), PAS un nouveau moteur. Sortie : fiche datée/signée intégrant la probabilité de réintégration. D8. | marche | Hypothèse |

| F-DRH-DISCRIM-HARC-16 | Conformité directive Transparence des rémunérations (UE 2023/970) — reporting d'écart + évaluation conjointe au-delà de 5 % | Conformité à la directive UE 2023/970 (transposition FR juin 2026) : reporting d'écart F/H par catégorie de travail de valeur égale, déclenchement automatique de l'évaluation conjointe quand l'écart inexpliqué > 5 %, préparation des réponses au « droit à l'information ». Distinct de HARC-06 (index Pénicaud/Rixain = barème de points national) et HARC-14 (ESRS S1 = rapport de durabilité) : ici déclencheur légal autonome avec inversion de la charge de la preuve. Échéance 2026 = catalyseur d'achat. D8 : conformité, jamais arme contre les salariés. | marche | Hypothèse |
| F-DRH-DISCRIM-HARC-17 | Contrôle anti-discrimination de l'acte généré (test de non-reproduction du biais) — décliné de AI-ACT-05 | Avant émission d'un acte décisionnel (projet de sanction, lettre de licenciement, critères d'ordre PSE), passe l'acte au crible des critères prohibés (genre, âge, état de santé, grossesse/maternité, mandat syndical, convictions, origine) et alerte si la motivation/sélection présente un indice de discrimination directe/indirecte, avec traçabilité. Répond à l'exigence AI Act « l'outil décisionnel ne doit pas reproduire de discrimination » (test + logging). ⚠️ DÉCLINAISON du registre de gouvernance des biais AI-ACT-05 + contrôle humain AI-ACT-01 : ici le point d'application = test pré-émission sur l'acte du dossier, pas le registre transverse. Lit F-DT-12/F-DT-16. D8 : garde-fou protégeant aussi le salarié. | concurrent-gap | Hypothèse |

**Ajoutées ce run (APPEND 2026-06-06 — run de maturation) :** F-DRH-DISCRIM-HARC-16, -17.
- `F-DRH-DISCRIM-HARC-16` — Conservée — conformité directive UE 2023/970 (transposition FR juin 2026), déclencheur légal autonome distinct de HARC-06/14. decisionTool=true.
- `F-DRH-DISCRIM-HARC-17` — Conservée — test anti-discrimination pré-émission de l'acte (déclinaison de AI-ACT-05/01), point d'application = acte du dossier. decisionTool=true.

**Modifiées / justifiées (curation 2026-06-06) :**
- `F-DRH-DISCRIM-HARC-05` — Reclassé plateforme-reutilisee : chiffrage des indemnités conventionnelles aggravées via le moteur CCN-aware central F-DT-07, pas un calcul propre. Anti-doublon confirmé.
- `F-DRH-DISCRIM-HARC-08` — Reclassé plateforme-reutilisee : génération d'actes centralisée sous ACTES (F-98 + F-DT-04) ; le plan de remédiation égalité = aval de HARC-06.
- `F-DRH-DISCRIM-HARC-10` — Précision D11 : obligation côté déployeur (contrôle humain + info salariés + CSE), pas de FRIA employeur privé ; anti-biais porté par AI-ACT-05. Test pré-émission isolé dans HARC-17 (situation distincte).
- `F-DRH-DISCRIM-HARC-15` — Marquage explicite du sujet UX non tranché (fiche de provision ⊂ outils calculés/persistés uniquement).

**Suppressions proposées (2026-06-06, non appliquées — appliedDeletions vide, D4) :**
- `F-DRH-DISCRIM-HARC-04` — Recoupe le scoring d'exposition générique CHIFFRAGE-07 (déclinaison, pas dupliqué). Le scoring discrim/harcèlement = vue/déclinaison du moteur central. Non appliqué (D4 — relève de la matérialisation du moteur de scoring transverse, décision PO) ; appliedDeletions vide.
- `F-DRH-DISCRIM-HARC-07` — DÉPLACEMENT, pas suppression : la checklist anti-vice enquête harcèlement reste un trou réel mais relève du moteur transverse de checklists procédurales anti-vice (distinct de la prévention amont HARC-13). Conserver la capacité, déplacer le portage. Non appliqué (D4 — relocalisation, pas suppression) ; appliedDeletions vide.
- `F-DRH-DISCRIM-HARC-09` — Doublon de F-DRH-DASHBOARD-01/04 — vue portefeuille discrim/harcèlement = filtre/vue du dashboard consolidé. Non appliqué (D4 — touche le périmètre DASHBOARD) ; appliedDeletions vide.
- `F-DRH-DISCRIM-HARC-12` — Doublon de F-DRH-PREAVOCAT-01 — note d'exposition exportable mode pré-avocat portée par PREAVOCAT (export borné/révocable, D8). Non appliqué (D4 — touche le périmètre PREAVOCAT) ; appliedDeletions vide.

**Ajoutées ce run (APPEND 2026-06-05) :** F-DRH-DISCRIM-HARC-13, -14, -15.
- `F-DRH-DISCRIM-HARC-13` — Conservée — prévention documentée amont distincte de la détection (HARC-01), enquête (HARC-07), chiffrage (HARC-03). Trou réel. decisionTool=true.
- `F-DRH-DISCRIM-HARC-14` — Conservée — reporting CSRD/ESRS S1 = situation d'achat grand compte distincte de l'index Rixain (HARC-06). Branché sur HARC-06 + dashboard. decisionTool=false.
- `F-DRH-DISCRIM-HARC-15` — Conservée comme DÉCLINAISON de CHIFFRAGE-11 ; spécificité réintégration (salaires rétroactifs) justifie une config dédiée, anti-doublon explicite. decisionTool=false.

**Modifiées / justifiées (curation) :**

- `F-DRH-DISCRIM-HARC-15` — Recadrée comme déclinaison du générateur de fiche de provision de référence CHIFFRAGE-11 — anti-doublon du pont DAF.
- `F-DRH-DISCRIM-HARC-01` — Reclassé plateforme-reutilisee (lit F-DT-12/16).
- `F-DRH-DISCRIM-HARC-02` — Comparateur panel = situation distincte ; decisionTool ajouté.
- `F-DRH-DISCRIM-HARC-03` — Reclassé plateforme-reutilisee (lit F-DT-11/12/16) ; 'point aveugle' corrigé.
- `F-DRH-DISCRIM-HARC-04` — Recoupement scoring CHIFFRAGE-07 signalé.
- `F-DRH-DISCRIM-HARC-05` — CCN-aware existant dupliqué — unification signalée.
- `F-DRH-DISCRIM-HARC-07` — Enquête harcèlement non couverte ; decisionTool ajouté.
- `F-DRH-DISCRIM-HARC-08` — Génération d'actes à consolider sous ACTES.
- `F-DRH-DISCRIM-HARC-09` — Doublon DASHBOARD signalé.
- `F-DRH-DISCRIM-HARC-10` — Doublon AI-ACT ; anti-biais → AI-ACT-05.
- `F-DRH-DISCRIM-HARC-11` — Réutilise F-JU-01 ; pattern répété signalé.
- `F-DRH-DISCRIM-HARC-12` — Doublon PREAVOCAT-01 signalé.

**Suppressions proposées :**

- `F-DRH-DISCRIM-HARC-12` — Doublon de F-DRH-PREAVOCAT-01 (mode pré-avocat = domaine dédié).

---

### Domaine — Représentation du personnel & conformité CSE (`CSE-CONFORM`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-DRH-CSE-CONFORM-01 | Détection des cas de consultation CSE obligatoire — situation nouvelle (s'appuie sur F-DT-13/14/36) | Drapeau « consultation CSE obligatoire » pour les cas durs (éco collectif, inaptitude, salarié protégé). ⚠️ La consultation CSE est DÉJÀ partiellement couverte : F-DT-14 (PSE → CSE), F-DT-36 (vice 9 = procédure CSE collectif), F-DT-30 (salarié protégé). La VALEUR = drapeau transverse de détection multi-situations. À brancher sur ces outils, pas re-détecter. D8. | droit-travail | Hypothèse |
| F-DRH-CSE-CONFORM-02 | Checklist anti-vice consultation CSE (bloquante par situation) — feature de référence CSE (cf. INAPT-04) | Checklist calendaire bloquante par situation (inaptitude, éco collectif, salarié protégé). Feature de référence ; F-DRH-INAPT-04 (trame CSE inaptitude) en est un sous-cas. S'appuie sur F-DT-14/36/30. D8. | concurrent-gap | Hypothèse |
| F-DRH-CSE-CONFORM-03 | Génération convocation CSE & avis/note d'information-consultation — feature de référence (cf. INAPT-04, ACTES-06) | Génère convocation CSE + note info-consultation + trame PV. Feature de référence génération CSE ; réutilise F-98. Articuler avec F-DRH-ACTES-06 (convocation inaptitude) pour ne pas dupliquer. D8. | droit-travail | Hypothèse |
| F-DRH-CSE-CONFORM-04 | Suivi calendaire des délais de consultation CSE + plancher bloquant préfix (48 j éco, 15 j inaptitude) — situation nouvelle | Compteur des délais CSE (avis attendu, délais préfix éco collectif) articulé aux autres délais durs. ⚠️ Enrichie ce run pour absorber le garde-fou calendaire CRITIQUE : pose un PLANCHER BLOQUANT préfix (48 j CSE éco, 15 j inaptitude) empêchant la notification prématurée. Dimension purement calendaire/préfix ici (pas de doublon avec CONFORM-02 checklist). Vérifié non couvert (F-DT-03 = prescription action). Gap réel. D8. | droit-travail | Hypothèse |
| F-DRH-CSE-CONFORM-05 | Scoring du risque de vice consultation CSE — décliné de CHIFFRAGE-07 | ⚠️ Recoupe le scoring générique CHIFFRAGE-07. À décliner (axe procédural CSE), pas dupliquer le moteur. | marche | Hypothèse |
| F-DRH-CSE-CONFORM-06 | CCN-aware obligations de consultation — recoupe F-DT-07 + capacité transverse | ⚠️ CCN-aware existant (F-DT-07), répété 7x. À unifier. | concurrent-gap | Hypothèse |
| F-DRH-CSE-CONFORM-07 | Note conformité CSE exportable mode pré-avocat — DOUBLON PREAVOCAT-01 | ⚠️ DOUBLON avec F-DRH-PREAVOCAT-01. À consolider dans PREAVOCAT. | marche | Hypothèse |
| F-DRH-CSE-CONFORM-08 | Vue portefeuille & reporting direction/CSE consultations — DOUBLON DASHBOARD | ⚠️ DOUBLON avec F-DRH-DASHBOARD-01/04. À consolider. | concurrent-gap | Hypothèse |
| F-DRH-CSE-CONFORM-09 | Lecture employeur jurisprudence vices consultation CSE (réutilise F-JU-01) | LIT F-JU-01 appliqué aux vices CSE. ⚠️ Pattern jurisprudence répété 6x — unifier. | plateforme-reutilisee | Hypothèse |
| F-DRH-CSE-CONFORM-10 | Journal contrôle humain qualifications CSE — DOUBLON AI-ACT | ⚠️ DOUBLON du cadre transverse AI-ACT (recoupement déjà admis dans la description d'origine). À porter par AI-ACT. | corporate-readiness | Hypothèse |
| F-DRH-CSE-CONFORM-11 | Détection du risque de délit d'entrave au CSE (exposition pénale distincte) | Drapeau d'alerte qualifiant le risque de DÉLIT D'ENTRAVE au fonctionnement du CSE (absence/insuffisance de consultation, défaut de communication, passage outre l'avis) — exposition PÉNALE (1 an / 7 500 €, peines aggravées) s'ajoutant à la nullité civile. ⚠️ Ne pas confondre avec CSE-CONFORM-01 (obligation de consulter) ni CSE-CONFORM-05 (vice civil). ⚠️ Cohérence D5 : le pénal est hors scope DRH — DOIT rester une ALERTE d'exposition (comme REQUAL-CDD-03), pas un outil de droit pénal. Trou concurrentiel total. D8. | concurrent-gap | Hypothèse |
| F-DRH-CSE-CONFORM-12 | Gestion du PV / avis CSE & communication horodatée aux membres (preuve opposable) | Workflow post-réunion : enregistre l'avis rendu (favorable/défavorable/réputé rendu), archive PV + note d'information-consultation horodatés/opposables, trace la communication aux membres (envoi, accusés). Preuve de consultation effective (input de CONFORM-02 et de la note pré-avocat). ⚠️ Distinct de CONFORM-03 (qui GÉNÈRE convocation/trame PV) : ici CYCLE DE VIE du PV/avis + traçabilité. Réutilise F-37 + F-38 + F-DT-04. D8. | concurrent-gap | Hypothèse |

| F-DRH-CSE-CONFORM-13 | Consultation CSE spécifique au déploiement d'une IA RH haut-risque (objet = système IA, distinct des consultations métier) | Détecte l'obligation de consulter le CSE PRÉALABLEMENT au déploiement d'une IA impactant conditions de travail / décisions d'emploi (Code du travail + AI Act Annexe III.4, 02/08/2026) — objet = le SYSTÈME IA (finalité, données, transparence, contrôle humain, droit à l'explication). Pose le gate « CSE informé/consulté + salariés informés avant mise en service » + alerte délit d'entrave à défaut. ⚠️ DISTINCT de CONFORM-01 (consultation métier) et CONFORM-11 (entrave générique). Articule avec le kit AI-ACT (déployeur) sans le dupliquer (AI-ACT-04 porte la trame transverse ; ici la SITUATION CSE-IA). Trou concurrentiel daté. D8. | concurrent-gap | Hypothèse |
| F-DRH-CSE-CONFORM-14 | Détection salarié CSE protégé → gate autorisation inspection du travail avant licenciement (situation propre) | Détecte qu'un salarié visé est PROTÉGÉ (mandat CSE/représentation) et pose le gate procédural DUR : consultation CSE PUIS autorisation de l'inspection du travail AVANT toute notification (un licenciement notifié sans autorisation est nul + délit d'entrave). VALEUR = la SITUATION procédurale complète et bloquante (séquence consultation → autorisation → délais → preuve), pas le simple drapeau « protégé » (déjà via F-DT-30 dans CONFORM-01). Branche sur F-DT-30 + CONFORM-02/04/12. Volet pénal (entrave) reste une ALERTE (D5) portée par CONFORM-11. D8. | droit-travail | Hypothèse |
| F-DRH-CSE-CONFORM-15 | Alertes & trames pré-remplies de consultation CSE (champs renseignés depuis le dossier) | Couche d'ACTIVATION : émet les ALERTES en temps utile (« consultation CSE requise avant le JJ/MM », « délai préfix entamé », « avis non rendu ») et pré-remplit TOUTES les trames (convocation, note info-consultation, ordre du jour, trame PV) depuis les pièces (objet, périmètre, effectif, dates, situation, CCN). Invariant F-246 (tout champ saisissable pré-rempli, exception = non factualisable). Distinct de CONFORM-03 (qui GÉNÈRE le document) : ici PRÉ-REMPLISSAGE + déclenchement des alertes (input de CONFORM-03/04/12). Réutilise PLATFORM-04 + CONFORM-04. Réduit la friction (Renversez/Mengue). D8. | concurrent-gap | Hypothèse |

**Ajoutées ce run (APPEND 2026-06-06 — run de maturation) :** F-DRH-CSE-CONFORM-13, -14, -15.
- `F-DRH-CSE-CONFORM-13` — Conservée — consultation CSE du déploiement d'une IA RH haut-risque (objet = système IA), distincte des consultations métier. decisionTool=true.
- `F-DRH-CSE-CONFORM-14` — Conservée — gate autorisation inspection du travail avant licenciement d'un salarié protégé ; situation procédurale bloquante complète. decisionTool=true.
- `F-DRH-CSE-CONFORM-15` — Conservée — alertes + pré-remplissage des trames CSE (F-246), couche d'activation distincte de CONFORM-03 (génération). decisionTool=false.

**Modifiées / justifiées (curation 2026-06-06) :**
- `F-DRH-CSE-CONFORM-05` — Marqueur UX non tranché ajouté (scoring/conclusions ⊂ outils CALCULÉS/persistés, pas champs CSE pré-remplis non cliqués) à arbitrer avant dev. Déclinaison du moteur de scoring de référence CHIFFRAGE-07, sans changement de périmètre.

**Suppressions proposées (2026-06-06, non appliquées — appliedDeletions vide, D4) :**
- `F-DRH-CSE-CONFORM-07` — Doublon de F-DRH-PREAVOCAT-01 (mode pré-avocat = domaine dédié). Maintenue.
- `F-DRH-CSE-CONFORM-10` — Doublon du cadre transverse AI-ACT (journal contrôle humain). À porter par AI-ACT. Maintenue.

**Ajoutées ce run (APPEND 2026-06-05) :** F-DRH-CSE-CONFORM-11, -12.
- `F-DRH-CSE-CONFORM-11` — Conservée — exposition pénale du dirigeant non couverte (CONFORM-01 = obligation, CONFORM-05 = vice civil). ⚠️ Recadrée en ALERTE (cohérence D5 : pénal hors scope DRH), comme REQUAL-CDD-03.
- `F-DRH-CSE-CONFORM-12` — Conservée — cycle de vie du PV/avis distinct de la génération (CONFORM-03), non couvert.

**Modifiées / justifiées (curation) :**

- `F-DRH-CSE-CONFORM-04` — (run 2026-06-05) Enrichie pour absorber le garde-fou calendaire CRITIQUE (plancher 48 j CSE éco, 15 j inaptitude) en posant un PLANCHER BLOQUANT préfix empêchant la notification prématurée. Dimension purement calendaire/préfix ici (pas de doublon avec CONFORM-02 checklist).
- `F-DRH-CSE-CONFORM-01` — Recadré : F-DT-14/36/30 couvrent déjà des pans CSE ; détection transverse = valeur nouvelle.
- `F-DRH-CSE-CONFORM-02` — Désignée référence checklist CSE vs INAPT-04.
- `F-DRH-CSE-CONFORM-03` — Référence génération CSE ; F-98 réutilisé ; articulation ACTES-06.
- `F-DRH-CSE-CONFORM-05` — Recoupement scoring CHIFFRAGE-07 signalé.
- `F-DRH-CSE-CONFORM-06` — CCN-aware existant dupliqué — unification signalée.
- `F-DRH-CSE-CONFORM-07` — Doublon PREAVOCAT-01 signalé.
- `F-DRH-CSE-CONFORM-08` — Doublon DASHBOARD-01/04 signalé.
- `F-DRH-CSE-CONFORM-09` — Réutilise F-JU-01 ; pattern répété signalé.
- `F-DRH-CSE-CONFORM-10` — Doublon AI-ACT signalé.

**Suppressions proposées :**

- `F-DRH-CSE-CONFORM-07` — Doublon de F-DRH-PREAVOCAT-01 (mode pré-avocat = domaine dédié).
- `F-DRH-CSE-CONFORM-10` — Doublon du cadre transverse AI-ACT (recoupement déjà admis).

---

### Domaine — Temps de travail & litiges durée/repos (`TEMPS-TRAVAIL`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-DRH-TEMPS-TRAVAIL-01 | Audit de validité de la convention de forfait-jours — situation nouvelle | Contrôle les conditions de validité du forfait-jours (accord collectif + clauses suivi charge/repos, convention écrite, catégorie éligible). Vérifié : aucun F-DT ne traite le forfait-jours (F-DT-19 = heures sup, pas validité forfait). Gap réel. D8. | concurrent-gap | Hypothèse |
| F-DRH-TEMPS-TRAVAIL-02 | Contrôle du droit au repos & jours non-pris (forfait-jours) — situation nouvelle | Contrôle repos quotidien/hebdo, plafond 218j, suivi de charge, jours non-pris valorisés. Vérifié non couvert. Gap réel. D8. | droit-travail | Hypothèse |
| F-DRH-TEMPS-TRAVAIL-03 | Lecture employeur du rappel d'heures supplémentaires (réutilise F-DT-19 + F-DT-20) | Chiffre l'arriéré d'heures sup (décompte hebdo, majorations 25/50, contingent, repos compensateur) sur 3 ans. F-DT-19 (calculateur heures sup) + F-DT-20 (rappel de salaire) existent déjà. LIT côté employeur. Aucun nouveau simulateur. | plateforme-reutilisee | Hypothèse |
| F-DRH-TEMPS-TRAVAIL-04 | Qualification des litiges de durée (heures sup non-décomptées vs forfait/tâche) — situation nouvelle | Qualifie le régime de temps de travail réel (heures sup non-décomptées, requalification forfait invalide, astreinte/travail effectif/déplacement). Oriente le calcul (réutilise F-DT-19/20). Vérifié non couvert. Gap réel. D8. | droit-travail | Hypothèse |
| F-DRH-TEMPS-TRAVAIL-05 | Scoring d'exposition durée du travail — décliné de CHIFFRAGE-07 | ⚠️ Recoupe le scoring générique CHIFFRAGE-07. À décliner, pas dupliquer. | marche | Hypothèse |
| F-DRH-TEMPS-TRAVAIL-06 | CCN-aware chiffrage durée — recoupe F-DT-07 + capacité transverse | ⚠️ CCN-aware existant (F-DT-07), répété 7x. À unifier. | concurrent-gap | Hypothèse |
| F-DRH-TEMPS-TRAVAIL-07 | Plan d'action de régularisation préventive temps de travail — situation nouvelle (cf. REQUAL-CDD-06) | Plan de conformité (suivi de charge, refonte forfait, régularisation heures, repos compensateur). Même pattern que F-DRH-REQUAL-CDD-06. Gap réel. D8. | droit-travail | Hypothèse |
| F-DRH-TEMPS-TRAVAIL-08 | Génération d'actes temps de travail (avenant forfait, régularisation, suivi charge) — DOUBLON ACTES (réutilise F-98) | ⚠️ Génération d'actes : à porter par le domaine ACTES (avenant forfait, régularisation, suivi charge). Réutilise F-98. Contrôle de licéité = ACTES-10. | droit-travail | Hypothèse |
| F-DRH-TEMPS-TRAVAIL-09 | Note d'exposition durée exportable — DOUBLON PREAVOCAT-01 | ⚠️ DOUBLON avec F-DRH-PREAVOCAT-01. À consolider dans PREAVOCAT. | marche | Hypothèse |
| F-DRH-TEMPS-TRAVAIL-10 | Vue portefeuille du risque durée du travail — DOUBLON DASHBOARD | ⚠️ DOUBLON avec F-DRH-DASHBOARD-01. À consolider. | concurrent-gap | Hypothèse |
| F-DRH-TEMPS-TRAVAIL-11 | Lecture employeur jurisprudence durée du travail (réutilise F-JU-01) | LIT F-JU-01 appliqué à la durée (nullité forfait faute de suivi, charge de la preuve heures sup, astreinte/travail effectif). ⚠️ Pattern jurisprudence répété 6x — unifier. | plateforme-reutilisee | Hypothèse |
| F-DRH-TEMPS-TRAVAIL-12 | Chiffrage de l'exposition astreintes & RTT non-pris (trésorerie) — situation nouvelle | Outil décisionnel dédié au cash-out astreintes/RTT, distinct du rappel d'heures sup (TEMPS-TRAVAIL-03) : valorise les rappels d'astreinte requalifiée en travail effectif (contreparties non versées, dépassements de durée induits) + RTT non pris/non payés, majorations CCN. Besoin marché 'RTT/astreintes = exposition de trésorerie' non chiffré par 03 (heures sup) ni 04 (qualification). Alimente la fiche de provision (CHIFFRAGE-11). D8. | marche | Hypothèse |
| F-DRH-TEMPS-TRAVAIL-13 | Audit amont du forfait-jours : heures supplémentaires structurelles & déséquilibre de charge — situation nouvelle | Contrôle de conformité AMONT détectant les forfaits masquant un sur-temps structurel : amplitude récurrente au-delà des repos, charge déséquilibrée, alertes de suivi non traitées (invalident la convention, exposent au rappel d'heures sup). Distinct de 01 (validité formelle) et 02 (repos/jours non-pris) : 13 attaque la pratique réelle d'exécution. Trou : Jobexit/éditeurs paramétriques/documentaires n'auditent pas l'exécution du forfait depuis les pièces. D8. | concurrent-gap | Hypothèse |
| F-DRH-TEMPS-TRAVAIL-14 | Checklist de preuve du décompte horaire (charge de la preuve heures sup) — situation nouvelle (anti-vice) | Checklist bloquante vérifiant que l'employeur dispose d'un système fiable de décompte du temps opposable : depuis la répartition de la charge de la preuve, l'absence d'éléments côté employeur fait basculer le litige heures sup. Identifie depuis les pièces l'existence/complétude des relevés, les trous probants, l'antériorité. Besoin 'charge de la preuve heures sup' + anti-vice ; aucun concurrent ne produit de checklist de preuve dossier-centric. Distinct du chiffrage (03/12) et de la qualification (04). D8. | concurrent-gap | Hypothèse |
| F-DRH-TEMPS-TRAVAIL-15 | Fiche de provision IAS 37 — litige temps de travail (configuration de CHIFFRAGE-11) | Configuration durée du moteur de fiche de provision CHIFFRAGE-11 : produit la fiche datée/signée (probabilité de condamnation × montant estimé heures sup/astreintes/RTT + frais) consommée par le DAF à la clôture, depuis le scoring durée (05/CHIFFRAGE-07) et les chiffrages (03/12). Besoin marché IAS 37 'provisionner tout litige probable, fiche d'estimation datée'. NE recrée PAS le moteur (CHIFFRAGE-11 = référence) — config + agrégation des inputs durée. Pont DRH↔DAF. decisionTool=false (lit/configure). D8. ⚠️ UX NON TRANCHÉ : la fiche ne doit s'appuyer que sur les outils CALCULÉS/persistés, pas les champs pré-remplis non cliqués (alerte / pré-calcul auto / laisser tel quel — à arbitrer avant dev). | marche | Hypothèse |
| F-DRH-TEMPS-TRAVAIL-16 | Aide à la décision contester / transiger — litige heures sup (configuration du moteur d'arbitrage commun) | Décline le moteur d'arbitrage contester/transiger commun (paramètres procédure 2026 : contribution de saisine CPH ~50 €, risque d'appel ~67 %, durée CPH 13,7 mois) au litige de durée : compare l'exposition rappel d'heures sup/astreintes/RTT chiffrée (03/12) + aléa (05) à une borne de transaction défendable. NE recrée PAS le moteur d'arbitrage (commun toutes situations) — config durée. Besoin marché 'aide à la décision contester/transiger intégrant la contribution CPH 2026'. D8 : conformité/anticipation, jamais 'gagner contre le salarié'. decisionTool=true (situation métier distincte : arbitrage durée). | marche | Hypothèse |
| F-DRH-TEMPS-TRAVAIL-17 | Alerte DUERP — risque durée/charge non documenté (situation nouvelle, conformité transverse) | Détecte depuis les pièces du dossier durée les signaux de risque non couverts par le DUERP de l'employeur (amplitude horaire récurrente, charge déséquilibrée du forfait-jours, dépassements répétés exposant à un risque santé/sécurité). Produit une ALERTE d'exposition (obligation DUERP = document unique d'évaluation des risques, obligation employeur transverse) reliée à la pratique de durée réelle — pas un outil de gestion DUERP complet. Trou : aucun concurrent ne relie l'exécution du temps de travail depuis les pièces à l'obligation DUERP. D8 (prévention/conformité). decisionTool=false (alerte d'exposition, pas simulateur). | concurrent-gap | Hypothèse |

**Ajoutées ce run (APPEND 2026-06-06) :** F-DRH-TEMPS-TRAVAIL-15, -16, -17.
- `F-DRH-TEMPS-TRAVAIL-15` — Trou marché IAS 37 non porté par le domaine durée (CHIFFRAGE-11 = moteur de référence ; ici = config + agrégation inputs durée). Pont DRH↔DAF. decisionTool=false. Sujet UX non tranché signalé explicitement (vision PO RUN 3).
- `F-DRH-TEMPS-TRAVAIL-16` — Trou marché 'contester/transiger + contribution CPH 2026' non couvert ; décline le moteur d'arbitrage commun (pas de doublon de moteur). Situation métier distincte = arbitrage durée. decisionTool=true.
- `F-DRH-TEMPS-TRAVAIL-17` — Trou concurrent-gap : DUERP × exécution du temps de travail depuis les pièces, zone vide. ALERTE d'exposition (pas outil DUERP complet, pas pénal). decisionTool=false. D8.

**Ajoutées run précédent (APPEND 2026-06-05) :** F-DRH-TEMPS-TRAVAIL-12, -13, -14.
- `F-DRH-TEMPS-TRAVAIL-12` — Conservée — trou marché 'RTT/astreintes = exposition de trésorerie' non chiffré par 03/04. 1 outil = 1 situation. decisionTool=true.
- `F-DRH-TEMPS-TRAVAIL-13` — Conservée — situation amont distincte de la validité formelle (01) et du contrôle repos (02). Trou dossier-centric. decisionTool=true.
- `F-DRH-TEMPS-TRAVAIL-14` — Conservée — 'charge de la preuve heures sup' + checklist anti-vice, zone vide concurrents. Situation distincte du chiffrage et de la qualification. decisionTool=true.

**Modifiées / justifiées (curation) :**

- `F-DRH-TEMPS-TRAVAIL-01` — decisionTool ajouté ; gap réel confirmé (forfait-jours).
- `F-DRH-TEMPS-TRAVAIL-02` — decisionTool ajouté ; gap réel (repos forfait).
- `F-DRH-TEMPS-TRAVAIL-03` — Reclassé plateforme-reutilisee (lit F-DT-19/20), decisionTool retiré ; 'aucun outil employeur' corrigé.
- `F-DRH-TEMPS-TRAVAIL-04` — decisionTool ajouté ; qualification distincte alimentant F-DT-19/20.
- `F-DRH-TEMPS-TRAVAIL-05` — Recoupement scoring CHIFFRAGE-07 signalé.
- `F-DRH-TEMPS-TRAVAIL-06` — CCN-aware existant dupliqué — unification signalée.
- `F-DRH-TEMPS-TRAVAIL-08` — Génération d'actes à consolider sous ACTES.
- `F-DRH-TEMPS-TRAVAIL-09` — Doublon PREAVOCAT-01 signalé.
- `F-DRH-TEMPS-TRAVAIL-10` — Doublon DASHBOARD-01 signalé.
- `F-DRH-TEMPS-TRAVAIL-11` — Réutilise F-JU-01 ; pattern répété signalé.

**Suppressions proposées :**

- `F-DRH-TEMPS-TRAVAIL-09` — Doublon de F-DRH-PREAVOCAT-01 (mode pré-avocat = domaine dédié).

---

## 3) Corporate-readiness (features produit)

### Domaine — Conformité Règlement IA (AI Act Annexe III) (`AI-ACT`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-DRH-AI-ACT-01 | Journal de contrôle humain (traçabilité ≥ 6 mois, exportable) — CADRE TRANSVERSE de référence | Journal du contrôle humain sur toute décision assistée par IA (validation/rejet, motif, user, dossier, horodatage), ≥ 6 mois, exportable DPO. C'est LA feature transverse de référence ; tous les 'journal de contrôle humain par domaine' (CHIFFRAGE-10, SECU-PROC-11, INAPT-10, SANCTION-09, PREAVOCAT-06, ACTES-09, REQUAL-CDD-09, DISCRIM-HARC-10, CSE-CONFORM-10) sont ses doublons. S'appuie sur F-37 (versioning/audit trail) + F-38 (audit_logs). Gate D10. | corporate-readiness | Hypothèse |
| F-DRH-AI-ACT-02 | AIPD pré-remplie (capacités, limites, risques, atténuations) — transverse ; FRIA conditionnel secteur public uniquement | Génère l'AIPD pré-remplie, opposable DPO/Achats, versionnée. Feature transverse de référence vs AIPD répétées par domaine (INAPT-10, REQUAL-CDD-09, DISCRIM-HARC-10). ⚠️ CORRECTION marché : la FRIA (Art. 27 AI Act) ne s'impose PAS aux employeurs privés — uniquement secteur public / service public. L'employeur privé reste DÉPLOYEUR haut-risque (Annexe III.4) : contrôle humain + info salariés + CSE, PAS la FRIA. FRIA générée uniquement si workspace 'secteur public' (variante conditionnelle). Gate D10. | corporate-readiness | Hypothèse |
| F-DRH-AI-ACT-03 | Information des salariés & explicabilité de la décision assistée — transverse | Information préalable des salariés + explication intelligible des facteurs. Transverse. D8 : la transparence protège le salarié ET sécurise l'employeur. | droit-travail | Hypothèse |
| F-DRH-AI-ACT-04 | Alerte & trame consultation/consentement CSE IA haut-risque — transverse (cf. CSE-CONFORM) | Trame de consultation CSE à l'activation d'un outil IA haut-risque. ⚠️ Articuler avec le domaine CSE-CONFORM (qui porte la consultation CSE métier) : ici c'est la consultation CSE SPÉCIFIQUE au déploiement d'IA (objet distinct). À ne pas confondre avec CSE-CONFORM-02/03 (consultation CSE sur ruptures). | droit-travail | Hypothèse |
| F-DRH-AI-ACT-05 | Registre de gouvernance des biais (décisions IA refusées/requalifications/nullités) — transverse + anti-biais discrim | Registre auditable des cas IA refusés/requalifiés/écartés. Absorbe la spécificité anti-biais de F-DRH-DISCRIM-HARC-10. Réutilise les outputs F-DT-16 (nullité)/F-DT-22 (requalif). Gate D10. | corporate-readiness | Hypothèse |
| F-DRH-AI-ACT-06 | Seuil de confiance & override manuel obligatoire — transverse à tous les outils | Impose que l'output IA n'est jamais la décision (confiance affichée, override/interruption, motif capturé alimentant -01/-05). Transverse. ⚠️ Cohérence produit : F-IA-03 (validation) et le modèle 'outils = simulateurs indépendants' (memory) existent déjà — articuler sans contredire (pas d'override forcé entre simulateurs). | corporate-readiness | Hypothèse |
| F-DRH-AI-ACT-07 | Audit logs double usage AI Act + SOC 2/ISO — DOUBLON CORP-READY-10/AUDIT-LOG (s'appuie sur F-38) | ⚠️ DOUBLON avec F-DRH-CORP-READY-10 (logs double usage) et le domaine AUDIT-LOG entier. Réutilise F-38 (audit_logs existant). À unifier : UNE seule feature audit logs, référencée par AI-ACT et CORP-READY. | corporate-readiness | Hypothèse |
| F-DRH-AI-ACT-08 | Kit procurement conformité IA (CAIQ/SIG, Trust Center, DPA) — DOUBLON CORP-READY | ⚠️ DOUBLON avec F-DRH-CORP-READY-05/06/07 (Trust Center, CAIQ, SIG) + F-DRH-CORP-READY-14 (kit procurement). À consolider dans CORP-READY ; AI-ACT ne porte que le VOLET AI Act du kit. | marche | Hypothèse |
| F-DRH-AI-ACT-09 | Documentation technique déployeur & instructions fournisseur — transverse | Doc technique + instructions fournisseur (capacités/limites) alimentant l'AIPD (-02). Transverse, non couvert ailleurs. Gate D10. | corporate-readiness | Hypothèse |
| F-DRH-AI-ACT-10 | Badge & argumentaire commercial AI Act-ready déployeur — tâche marketing, pas feature produit | ⚠️ INCOHÉRENCE typologie : badge + page comparateur + trame de pitch = TÂCHE MARKETING (MARKETING_BACKLOG.md), pas une feature produit PRODUCT_SPEC. Soumettre au contrôle de cohérence marketing 4 points (CLAUDE.md règle 2). Échéance 2 déc. 2027 (Digital Omnibus). | concurrent-gap | Hypothèse |
| F-DRH-AI-ACT-11 | Système de gestion des risques & gouvernance des données FOURNISSEUR (Art. 9/10) — volet provider distinct du déployeur | ⚠️ DISTINCTION provider vs deployer non couverte par -01/-10 (orientés DÉPLOYEUR/client). LegalCase = FOURNISSEUR du système haut-risque (Annexe III.4) : Art. 9 (système de gestion des risques documenté, itératif) + Art. 10 (qualité/gouvernance des données : pertinence, représentativité, examen des biais). Sans cette doc fournisseur, le déployeur ne peut pas se conformer → rend l'outil achetable. Alimente l'AIPD (-02) et la doc technique (-09). Distinct de -09 (instructions au déployeur) : ici dispositif INTERNE éditeur. Gate D10. | corporate-readiness | Hypothèse |
| F-DRH-AI-ACT-12 | Surveillance post-déploiement & détection de dérive (post-market monitoring, Art. 72) — volet provider | Suivi de gouvernance IA en continu : détection de dérive (drift) des outputs, journal permanent automatique des incidents, mesures correctives tracées. Obligation provider de surveillance post-commercialisation (Art. 72). Distinct de -05 (registre des biais sur décisions individuelles) et -07 (audit logs) : ici monitoring agrégé de la qualité du système + déclenchement de revues. Alimente -05 et -11. | corporate-readiness | Hypothèse |
| F-DRH-AI-ACT-13 | Veille & checklist de conformité par échéance réglementaire glissante (Digital Omnibus) — feature transverse de pilotage | Tracker daté des échéances AI Act applicables au workspace (échéance haut-risque glissée à déc. 2027 via Digital Omnibus) + checklist de readiness par jalon (obligations -01..-12 dues à quelle date). Centralise la date de référence et conditionne l'activation des gates ; évite la dérive entre features citant des dates incohérentes (CHIFFRAGE-10 'déc 2027', AI-ACT-10 '2 déc 2027', needs '02/08/2026'). ⚠️ Recoupe SECU-PROC-15 (veille jurisprudentielle) sur l'angle 'fraîcheur normative' — à articuler. Transforme un gate mouvant en tableau de bord daté. | marche | Hypothèse |
| F-DRH-AI-ACT-14 | Kit DPO acheteur RGPD (registre type, base légale 'contentieux RH', durées CNIL) — recoupe CORP-READY-15 | Livrable pré-rempli pour le DPO du client : registre de traitement type (Art. 30), base légale 'gestion des contentieux et précontentieux' (référentiel RH CNIL), durées de conservation (référentiel CNIL durées RH 02/04/2026), articulation avec l'AIPD (-02). Distinct de l'AIPD (-02, analyse de risque IA). ⚠️ DOUBLON avec F-DRH-CORP-READY-15 (kit DPO pré-rempli, même contenu : registre type + base légale contentieux RH + durées CNIL). À CONSOLIDER : une seule feature kit DPO portée par CORP-READY-15 (domaine corporate-readiness) ; AI-ACT n'en pointe que l'articulation avec l'AIPD. | concurrent-gap | Hypothèse |

| F-DRH-AI-ACT-15 | Marquage CE, déclaration UE de conformité & enregistrement base de données UE (Art. 16/47/48/49) — volet provider, gate d'achat dur | Volet FOURNISSEUR non couvert par -09 (doc technique) ni -11 (risk/data interne) : déclaration UE de conformité (Art. 47), marquage CE (Art. 48), enregistrement du système haut-risque emploi (Annexe III.4) dans la base UE (Art. 49/71). Attestation OPPOSABLE qui clôt la chaîne provider : sans elle, le déployeur grand compte ne peut pas inscrire l'outil au procurement → inachetable (D10). Consultable au Trust Center (-08/CORP-READY-05), référencé au kit (CORP-READY-14). Distinct de -09 (instructions déployeur) et -11 (dispositif interne). Date alignée sur -13 (échéance haut-risque glissée déc. 2027, Digital Omnibus). | corporate-readiness | Hypothèse |
| F-DRH-AI-ACT-16 | Signalement des incidents graves aux autorités & chaîne de notification déployeur→fournisseur (Art. 73) | Procédure outillée de signalement des INCIDENTS GRAVES (Art. 73) à l'autorité de surveillance dans les délais légaux : gabarit de déclaration, horodatage, suivi du statut, chaîne déployeur→fournisseur (le client signale à LegalCase qui consolide et déclare). Distinct de -12 (post-market monitoring Art. 72 = surveillance CONTINUE agrégée) : ici l'obligation PONCTUELLE et datée de déclaration réglementaire, destinataire externe (autorité), échéance légale. Alimenté par -12 (un drift peut déclencher un incident à qualifier) et le registre des biais (-05). | corporate-readiness | Hypothèse |
| F-DRH-AI-ACT-17 | Journalisation automatique par conception au niveau système (Art. 12, logging-by-design) — volet provider distinct du journal de contrôle humain | Enregistrement AUTOMATIQUE par le système, sur tout son cycle de vie, des événements de traçabilité du fonctionnement (entrées/référence des données, version modèle/prompt, identifiant d'opération, résultat, déclenchement des garde-fous), conformément à l'Art. 12 — logging by design FOURNISSEUR. ⚠️ Triple distinction : -01 (contrôle HUMAIN de la décision validée/rejetée) ; AUDIT-LOG-02 (enregistrement brut métier, réutilise F-37) ; ici = capacité TECHNIQUE de journalisation native exigée du provider. Alimente -09, -12, -15. Réutilise/étend F-37 + F-38 sans dupliquer. | corporate-readiness | Hypothèse |

**Ajoutées ce run (APPEND 2026-06-06 — run de maturation) :** F-DRH-AI-ACT-15, -16, -17.
- `F-DRH-AI-ACT-15` — Conservée — marquage CE / déclaration UE / enregistrement base UE (Art. 16/47/48/49), volet provider, gate d'achat dur. decisionTool=false.
- `F-DRH-AI-ACT-16` — Conservée — signalement des incidents graves (Art. 73), obligation ponctuelle datée distincte du post-market monitoring -12. decisionTool=false.
- `F-DRH-AI-ACT-17` — Conservée — logging-by-design Art. 12 (volet provider), distinct du journal de contrôle humain -01 et de l'audit log métier AUDIT-LOG-02. decisionTool=false.

**Modifiées / justifiées (curation 2026-06-06) :**
- `F-DRH-AI-ACT-01` — Confirmé CADRE TRANSVERSE de référence du journal de contrôle humain : tous les « journal de contrôle humain par domaine » (CHIFFRAGE-10, SECU-PROC-11, INAPT-10, SANCTION-09, PREAVOCAT-06, ACTES-09, REQUAL-CDD-09, DISCRIM-HARC-10, CSE-CONFORM-10) y sont rattachés. S'appuie sur F-37 + F-38.
- `F-DRH-AI-ACT-02` — Correction maintenue : FRIA (Art. 27) = secteur public/service public uniquement ; employeur privé = DÉPLOYEUR haut-risque (contrôle humain + info salariés + CSE). FRIA générée seulement si workspace « secteur public » (variante conditionnelle).

**Suppressions proposées (2026-06-06, non appliquées — appliedDeletions vide, D4) :**
- `F-DRH-AI-ACT-07` — Doublon de la famille audit logs (CORP-READY-10 + AUDIT-LOG, base F-38). Une seule feature audit logs, référencée par AI-ACT et CORP-READY. Non appliqué (D4 — matérialisation de la capacité transverse audit logs, décision PO) ; appliedDeletions vide.
- `F-DRH-AI-ACT-08` — Doublon de CORP-READY-05/06/07 (Trust Center, CAIQ, SIG) + CORP-READY-14 (kit procurement). À consolider dans CORP-READY ; AI-ACT ne porte que le VOLET AI Act du kit. Non appliqué (D4 — touche le périmètre CORP-READY) ; appliedDeletions vide.
- `F-DRH-AI-ACT-10` — Badge / page comparateur / trame de pitch « AI Act-ready » = TÂCHE MARKETING (MARKETING_BACKLOG.md), hors typologie feature produit PRODUCT_SPEC. À transférer via le contrôle de cohérence marketing 4 points (CLAUDE.md règle 2). Non appliqué (D4 — transfert vers MARKETING_BACKLOG = décision PO, pas une suppression sèche) ; appliedDeletions vide.
- `F-DRH-AI-ACT-14` — Doublon de F-DRH-CORP-READY-15 (kit DPO : registre type Art. 30 + base légale contentieux RH + durées CNIL 02/04/2026). À consolider sous CORP-READY-15 ; AI-ACT ne conserve que l'articulation avec l'AIPD (-02). Non appliqué (D4 — touche le périmètre CORP-READY) ; appliedDeletions vide.

**Ajoutées ce run (APPEND 2026-06-05) :** F-DRH-AI-ACT-11, -12, -13, -14.
- `F-DRH-AI-ACT-11` — Conservée — trou : volet provider Art.9/10 non porté par -01..-10. Cohérent avec D10 (gate dur AI Act). decisionTool=false.
- `F-DRH-AI-ACT-12` — Conservée — obligation provider Art.72 non couverte par -01..-11 (contrôle humain par décision, registre, doc statique). decisionTool=false.
- `F-DRH-AI-ACT-13` — Conservée — source unique de vérité datée + checklist par jalon (incohérence de dates dans le domaine). Recoupement SECU-PROC-15 signalé. decisionTool=false.
- `F-DRH-AI-ACT-14` — ⚠️ CONTRADICTION INTER-DOMAINES détectée : AI-ACT-14 et CORP-READY-15 décrivent le MÊME kit DPO. Recadrée comme doublon à consolider sous CORP-READY-15 (domaine corporate-readiness). Voir incohérences.

**Modifiées / justifiées (curation) :**

- `F-DRH-AI-ACT-02` — (run 2026-06-05) Correction marché (FRIA Art.27 ≠ employeur privé) cohérente avec D10. AIPD pour tous, FRIA conditionnelle secteur public.
- `F-DRH-AI-ACT-14` — (run 2026-06-05) ⚠️ Doublon inter-domaines avec CORP-READY-15 (même kit DPO : registre type + base légale contentieux RH + durées CNIL). Recadrée pour consolidation sous CORP-READY-15 ; AI-ACT ne garde que l'articulation AIPD. Contradiction résolue.
- `F-DRH-AI-ACT-01` — Désignée cadre transverse de référence vs 9 doublons ; F-37/F-38 réutilisés.
- `F-DRH-AI-ACT-02` — Désignée transverse de référence AIPD.
- `F-DRH-AI-ACT-04` — Distinction vs CSE-CONFORM (CSE déploiement IA ≠ CSE rupture).
- `F-DRH-AI-ACT-05` — Absorbe anti-biais DISCRIM-HARC-10.
- `F-DRH-AI-ACT-06` — Cohérence F-IA-03 + simulateurs indépendants signalée.
- `F-DRH-AI-ACT-07` — Doublon CORP-READY-10/AUDIT-LOG ; F-38 réutilisé.
- `F-DRH-AI-ACT-08` — Doublon CORP-READY-05/06/07/14 signalé.
- `F-DRH-AI-ACT-10` — Reclassé tâche marketing (hors PRODUCT_SPEC).

**Suppressions proposées :**

- `F-DRH-AI-ACT-10` — Badge/argumentaire/pitch = tâche MARKETING_BACKLOG, pas feature produit. À transférer via le contrôle de cohérence marketing 4 points.

---

### Domaine — SSO/OIDC entreprise & authentication (corporate-readiness) (`SSO`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-22 | SSO entreprise (Azure AD, Google Workspace, SAML) | Feature EXISTANTE (PRODUCT_SPEC l.432, statut V9+ Grands comptes). Pour le scope DRH/employeur 200+, SSO/OIDC contre l'IdP entreprise = prérequis d'achat. ⚠️ INCOHÉRENCE corrigée : le titre existant est 'Azure AD, Google Workspace, SAML' — ne pas réécrire en 'Entra ID/Okta' sans variante explicite. Préserver le scope F-22. Réutilise le socle auth Spring Security + OAuth2/OIDC (F-01). | corporate-readiness | V9+ — Grands comptes |
| F-DRH-SSO-01 | Connexion OIDC Microsoft Entra ID (tenant entreprise) — découpage indicatif de F-22 | Décline F-22 sur Entra ID. ⚠️ Sous-feature d'une feature V9+ post-stabilisation : statut backlog cohérent. Gate procurement. Réutilise F-01. | corporate-readiness | Hypothèse |
| F-DRH-SSO-02 | Connexion OIDC Okta (et IdP cloud génériques) — découpage F-22 | Décline F-22 sur Okta/Ping/ForgeRock/Keycloak. Découpage indicatif. Réutilise F-01. | corporate-readiness | Hypothèse |
| F-DRH-SSO-03 | MFA obligatoire & politique d'authentification forte | MFA délégué à l'IdP + contrôle assertion MFA. ⚠️ DOUBLON avec F-DRH-CORP-READY-09 (SSO+MFA). À consolider. | corporate-readiness | Hypothèse |
| F-DRH-SSO-04 | Gestion des sessions & timeout d'inactivité (paramétrable par workspace) | Politique de session paramétrable par workspace. Situation distincte non couverte. ⚠️ Préoccupation transversale 'session' (CLAUDE.md) à cocher. | corporate-readiness | Hypothèse |
| F-DRH-SSO-05 | Single Logout propagé côté IdP (SLO OIDC/SAML) | Déconnexion propagée IdP. Situation distincte non couverte. | corporate-readiness | Hypothèse |
| F-DRH-SSO-06 | Provisioning / dé-provisioning automatique (SCIM 2.0) | SCIM 2.0 depuis l'annuaire. ⚠️ Recoupe le découpage indicatif F-22 (SCIM). Situation distincte légitime mais à rattacher à F-22. | corporate-readiness | Hypothèse |
| F-DRH-SSO-07 | Visibilité centralisée des accès pour le RSSI (rapport rôles par dossier) — distinct de l'audit log | Vue 'qui peut accéder à quoi' (état des droits) ≠ historique d'actions (AUDIT-LOG/F-38). Distinction bien posée. Gap réel. | concurrent-gap | Hypothèse |
| F-DRH-SSO-08 | Réponse procurement SSO/auth (section Trust Center) — DOUBLON CORP-READY/AI-ACT kit | ⚠️ DOUBLON avec le kit procurement (F-DRH-CORP-READY-05/06/07/14, F-DRH-AI-ACT-08). À consolider : volet identity du kit unique. | marche | Hypothèse |
| F-DRH-SSO-09 | Journal des événements d'authentification (login succès/échec, challenge MFA, refresh/expiration de token) — distinct de l'audit métier | Trace dédiée des ÉVÉNEMENTS D'IDENTITÉ (connexion réussie/échouée avec IdP/IP/UA, challenge/validation MFA, émission/refresh/révocation de token, expiration). Distinct de l'audit logs métier (AUDIT-LOG-01/F-38, qui trace les actions sur dossier salarié) : ici l'objet est l'accès lui-même. Exigence procurement + RGPD. Alimente la vue RSSI (SSO-07) et le Trust Center (SSO-08). ⚠️ À brancher sur la couche audit existante (F-38) sans la dupliquer (angle 'événements de sécurité auth'). | concurrent-gap | Hypothèse |
| F-DRH-SSO-10 | Mapping des rôles depuis les claims IdP & provisioning Just-In-Time (JIT) à la connexion — distinct de SCIM | À la connexion SSO, mappe les groupes/claims IdP vers le rôle applicatif du workspace EMPLOYEUR, avec création/MAJ Just-In-Time si absent. Distinct de SCIM 2.0 (SSO-06, push annuaire→app hors login) : le JIT opère DANS le flux d'authentification (mode léger pour comptes 200-500 sans agent SCIM). ⚠️ D7 : le rôle WORKSPACE 'EMPLOYEUR' reste fixé à la création ; ce mapping concerne les rôles INTERNES (admin RH, gestionnaire, lecture seule), jamais le basculement employeur/avocat. Réutilise F-01. | concurrent-gap | Hypothèse |
| F-DRH-SSO-11 | Restriction d'accès par IP / plage réseau (allowlist conditionnelle, paramétrable par workspace) — situation nouvelle | Permet à l'admin du workspace EMPLOYEUR de restreindre l'accès à une allowlist d'IP/CIDR (VPN, siège), en option et désactivable. Distinct de SSO-04 (durée/inactivité) : ici condition sur l'ORIGINE réseau. Exigence RSSI fréquente (conditional access). ⚠️ Préoccupation transversale 'session/auth' (CLAUDE.md) à cocher (lister composants impactés). Réutilise F-01. Optionnel par défaut (ne jamais bloquer un workspace par config accidentelle). | marche | Hypothèse |
| F-DRH-SSO-12 | Documentation & assistant de configuration SSO self-serve (kit intégration partenaire IdP) — accélérateur de cycle | Documentation pas-à-pas + assistant self-serve pour brancher l'IdP entreprise (Entra ID, Okta, SAML) : métadonnées/endpoints, mapping de claims, test de connexion, statut d'intégration. Différent de SSO-08 (réponse procurement statique) et du Trust Center (preuve) : ici OUTIL OPÉRATIONNEL de branchement. Levier de vélocité de vente (compliance-as-a-feature, réduire 4-12 sem. de review à des jours). Réutilise les connecteurs OIDC (SSO-01/02) et l'API documentée (API-SIRH-01). Capacité produit (D10), pas d'infra (D11). | marche | Hypothèse |
| F-DRH-SSO-13 | Révocation centralisée immédiate des accès (suppression groupe/compte IdP → sessions tuées en temps réel) — situation nouvelle | Quand l'admin supprime un compte/groupe dans l'IdP entreprise (Entra ID/Okta) ou désactive un collaborateur, l'accès LegalCase est révoqué IMMÉDIATEMENT : invalidation des sessions actives et des refresh tokens en cours, pas seulement blocage au prochain login. Couvre le besoin marché « révocation centralisée des accès » et « suppression groupe Entra = révocation immédiate » (exigence RSSI/procurement sur données salariés sensibles). Distinct de SSO-06 (SCIM deprovisioning, cycle annuaire batch/push hors temps réel) : ici propagation TEMPS RÉEL côté session + jeton, déclenchée par signal IdP (back-channel logout OIDC / event SCIM / révocation token). Distinct de SSO-05 (SLO = déconnexion initiée par l'utilisateur). ⚠️ Préoccupation transversale 'session/auth' (CLAUDE.md) à cocher (lister composants impactés : Principal, store de sessions, refresh tokens). Alimente SSO-09 (journal événements auth) et SSO-07 (vue RSSI). Réutilise F-01. | marche | Hypothèse |
| F-DRH-SSO-14 | Authentification renforcée (step-up MFA) sur actions sensibles touchant les données salariés — situation nouvelle | Exige une ré-authentification / un challenge MFA additionnel au MOMENT d'une action sensible sur des données salariés (consultation d'un dossier salarié verrouillé, export pré-avocat, génération d'acte de rupture, modification des droits d'accès), au-delà du MFA initial de connexion. Répond à l'exigence marché « MFA obligatoire — données salariés sensibles » + traçabilité RGPD « qui a consulté/modifié quel dossier salarié » en élevant le niveau de preuve sur les opérations à risque. Distinct de SSO-03 (MFA au login, une fois par session) : ici step-up CONTEXTUEL par action critique, paramétrable par workspace. Distinct de SSO-04 (durée de session). Le challenge est délégué à l'IdP (assertion MFA fraîche) ou à un second facteur, le résultat horodaté alimente SSO-09 et le journal AI-ACT-01 quand l'action est une décision assistée. ⚠️ Préoccupation transversale 'session/auth'. Réutilise F-01. | concurrent-gap | Hypothèse |
| F-DRH-SSO-15 | Détection des comptes dormants / orphelins & revue périodique des accès (access review RSSI) — situation nouvelle | Identifie les comptes inactifs au-delà d'un seuil et les comptes orphelins (plus rattachés à un groupe/claim IdP valide) et propose une revue périodique des accès (recertification) à l'admin/RSSI : liste à statuer, désactivation en un clic, rapport daté exportable. Répond au besoin marché « visibilité centralisée + révocation centralisée des accès » côté gouvernance (hygiène d'accès aux données salariés sensibles, attendu en audit ISO 27001 / questionnaire sécurité). Distinct de SSO-07 (état instantané « qui peut accéder à quoi ») : ici détection ACTIVE d'anomalies dans le temps + workflow de recertification. Distinct de SSO-13 (révocation événementielle temps réel) : ici revue proactive périodique. S'appuie sur SSO-09 (événements d'auth, dernière connexion) et la couche audit F-38. Capacité produit (D10), pas d'infra. ⚠️ UX NON TRANCHÉE : l'action de désactivation reste manuelle/validée par l'admin (pas de révocation auto silencieuse). decisionTool=false. | marche | Hypothèse |

**Ajoutées ce run (APPEND 2026-06-06) :** F-DRH-SSO-13, -14, -15.
- `F-DRH-SSO-13` — Nouvelle — révocation centralisée TEMPS RÉEL (sessions + tokens tués) distincte du SCIM deprovisioning batch (SSO-06) et du SLO utilisateur (SSO-05). Comble le trou marché « suppression groupe Entra = révocation immédiate ». Préoccupation transversale session/auth signalée. decisionTool=false.
- `F-DRH-SSO-14` — Nouvelle — step-up MFA CONTEXTUEL par action sensible distinct du MFA au login (SSO-03). Comble « MFA données salariés sensibles » + traçabilité RGPD. decisionTool=false.
- `F-DRH-SSO-15` — Nouvelle — détection comptes dormants/orphelins + recertification distincte de l'état instantané (SSO-07) et de la révocation événementielle (SSO-13). Exigence audit ISO 27001 / hygiène d'accès. decisionTool=false.

**Ajoutées au run précédent (APPEND 2026-06-05) :** F-DRH-SSO-09, -10, -11, -12.
- `F-DRH-SSO-09` — Conservée — événements de la couche d'identité (login/MFA/token) distincts des actions métier (AUDIT-LOG/F-38). Invariant 1 outil = 1 situation. decisionTool=false.
- `F-DRH-SSO-10` — Conservée — JIT/claim-mapping distinct de SCIM (push hors-login). Garde-fou D7 explicité (rôle workspace intouché). decisionTool=false.
- `F-DRH-SSO-11` — Conservée — conditional access par IP distinct de la durée de session (SSO-04). Préoccupation transversale signalée. decisionTool=false.
- `F-DRH-SSO-12` — Conservée — outil opérationnel de branchement self-serve distinct de la réponse procurement statique (08). Provenance marché (vélocité de vente, D6). decisionTool=false.

**Modifiées / justifiées (curation) :**

- `F-22` — Statut réel rétabli (V9+), titre/scope préservés ('Azure AD/Google Workspace/SAML'), F-01 réutilisé.
- `F-DRH-SSO-03` — Doublon CORP-READY-09 signalé.
- `F-DRH-SSO-04` — Préoccupation transversale session signalée.
- `F-DRH-SSO-06` — Rattachement F-22 (SCIM) explicité.
- `F-DRH-SSO-08` — Doublon kit procurement signalé.

---

### Domaine — ISO 27001 / SOC 2 & DPA RGPD self-serve (`CORP-READY`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-134 | Bundle corporate-readiness procurement (ombrelle : certifs + DPA + Trust Center) | Feature EXISTANTE (PRODUCT_SPEC l.478, 'Certification sécurité formelle — DPA signé, rapport de conformité, ISO 27001/SOC 2', statut V9+ Grands comptes). Parent du domaine ; les F-DRH-CORP-READY-NN détaillent. ⚠️ Statut réel = V9+, pas Hypothèse. | corporate-readiness | V9+ — Grands comptes |
| F-DRH-CORP-READY-01 | Rapport SOC 2 Type II téléchargeable (sous NDA) + roadmap datée | Découpage de F-134. ⚠️ INCOHÉRENCE statut : marqué Done dans certains contextes ? Non — Hypothèse cohérent (certif non acquise). Gate dur DPO. | marche | Hypothèse |
| F-DRH-CORP-READY-02 | Attestation / certificat ISO 27001 publié au Trust Center | Découpage F-134 (équivalent international SOC 2). Conservé. | marche | Hypothèse |
| F-DRH-CORP-READY-03 | DPA Article 28 RGPD self-serve (click-through) | Découpage F-134. Conservé. Gap réel (accélérateur cycle). | marche | Hypothèse |
| F-DRH-CORP-READY-04 | Liste des sous-traitants ultérieurs à jour (localisation + finalité) | Découpage F-134, complète le DPA. Conservé. | corporate-readiness | Hypothèse |
| F-DRH-CORP-READY-05 | Trust Center public — feature de référence Trust Center | Portail Trust Center. Feature de référence ; F-DRH-AI-ACT-08, F-DRH-SSO-08 en sont des volets. Conservé. | concurrent-gap | Hypothèse |
| F-DRH-CORP-READY-06 | CAIQ pré-rempli (~300 questions) exportable | Découpage F-134. Conservé. | marche | Hypothèse |
| F-DRH-CORP-READY-07 | Réponses SIG / VSA pré-remplies | Découpage F-134. Conservé. | marche | Hypothèse |
| F-DRH-CORP-READY-08 | Preuves de contrôles téléchargeables (pen-test, extraits logs) | Alimente le Trust Center (-05). Conservé. | marche | Hypothèse |
| F-DRH-CORP-READY-09 | SSO/OIDC entreprise (Entra ID/Okta) + MFA — feature de référence SSO+MFA (recoupe F-22) | ⚠️ Recoupe F-22 (existant) + F-DRH-SSO-01/02/03. Feature de référence SSO+MFA côté procurement ; le domaine SSO en détaille les briques. Réutilise F-01. À articuler avec F-22 (pas réécrire). | corporate-readiness | Hypothèse |
| F-DRH-CORP-READY-10 | Logs d'audit avancés compliance — feature de référence audit logs (recoupe AUDIT-LOG/AI-ACT-07, base F-38) | ⚠️ Feature de référence des audit logs côté procurement ; recoupe le domaine AUDIT-LOG entier + F-DRH-AI-ACT-07. Base = F-38 (audit_logs existant). À UNIFIER : une seule famille audit logs. | corporate-readiness | Hypothèse |
| F-DRH-CORP-READY-11 | Isolation multi-tenant documentée — recoupe AUDIT-LOG-08 + PLATFORM-06 | ⚠️ Recoupe AUDIT-LOG-08 (isolation des journaux) et PLATFORM-06 (isolation multi-buyer = invariant D8). Ici = isolation GLOBALE documentée (angle procurement/preuve). Réutilise le modèle multi-tenant existant. À articuler avec PLATFORM-06 (fondation) — CORP-READY-11 en est la documentation opposable. D8. | corporate-readiness | Hypothèse |
| F-DRH-CORP-READY-12 | Attestation hébergement UE + RGPD | Hébergement UE (eu-west-3 déjà acquis) + RGPD. Standard non différenciant. Conservé. | corporate-readiness | Hypothèse |
| F-DRH-CORP-READY-13 | API documentée (intégration SIRH) — recoupe domaine API-SIRH | ⚠️ Recoupe F-DRH-API-SIRH-01 (API REST OpenAPI). À unifier : l'API documentée est portée par le domaine API-SIRH ; CORP-READY n'en pointe que l'angle procurement. | corporate-readiness | Hypothèse |
| F-DRH-CORP-READY-14 | Kit procurement clé-en-main mesuré (-3 à -6 semaines) — feature de référence kit | Packaging du kit procurement. Feature de référence ; F-DRH-AI-ACT-08 et F-DRH-SSO-08 = volets. Conservé. | marche | Hypothèse |
| F-DRH-CORP-READY-15 | Kit DPO pré-rempli (registre de traitement type, base légale « contentieux RH », durées CNIL) — feature de référence kit DPO (cf. AI-ACT-14) | Livrable clé-en-main au DPO du client : registre de traitement type (Art. 30), base légale 'gestion des contentieux et précontentieux' (référentiel RH CNIL), durées de conservation (référentiel CNIL durées RH 02/04/2026). Neutralise la friction n°1 du DPO acheteur. Distinct de CORP-READY-03 (DPA = contrat Art. 28) et de AI-ACT-02 (AIPD/FRIA). ⚠️ FEATURE DE RÉFÉRENCE du kit DPO : F-DRH-AI-ACT-14 décrit le MÊME kit — à consolider ici (AI-ACT-14 ne garde que l'articulation AIPD). Alimenté via Trust Center (-05) et kit procurement (-14). | marche | Hypothèse |
| F-DRH-CORP-READY-16 | SLA de réponse au questionnaire sécurité (engagement 2-5 jours ouvrés) | Engagement de délai mesuré/affiché : réponse à un questionnaire sécurité standard (CAIQ/SIG/VSA) en 2-5 jours ouvrés grâce au pré-remplissage, vs des semaines pour un éditeur non préparé. Angle PROCESSUS/VÉLOCITÉ, distinct du CONTENU pré-rempli (-06 CAIQ, -07 SIG/VSA) : adresse le goulot de vente (review 4-12 semaines) sur le cycle DRH 1-3 mois. Métrique exposable au kit procurement (-14). | concurrent-gap | Hypothèse |
| F-DRH-CORP-READY-17 | Page de statut SLA / uptime publique au Trust Center | Composant public du Trust Center exposant le statut de disponibilité (SLA, uptime, incidents). Distinct de -05 (portail conteneur) et -08 (preuves statiques) : ici statut SLA/uptime vivant. Volet du Trust Center, pas infra (D11) : la feature = la PAGE et l'engagement SLA exposé, pas le monitoring sous-jacent. | marche | Hypothèse |
| F-DRH-CORP-READY-18 | Réponse anticipée « data residency EU + sous-processeurs géolocalisés » (données salariés sensibles) | Réponse pré-formatée et VISIBLE au Trust Center à la question achats récurrente sur la localisation des données salariés sensibles : hébergement EU (eu-west-3) + cartographie géolocalisée des sous-processeurs. Articule -04 (liste sous-traitants) + -12 (attestation UE) : ici l'angle = ACCÉLÉRATION procurement par mise en visibilité anticipée, pas une nouvelle attestation. Volet du kit (-14) / Trust Center (-05). | concurrent-gap | Hypothèse |
| F-DRH-CORP-READY-19 | Feuille de route de certification datée & séquencée (ISO 27001 d'abord, SOC 2 Type II en parallèle) — publiée au Trust Center | Expose une roadmap de certification DATÉE et séquencée : ISO 27001 lancée en premier (cible FR/EU, reconnaissance internationale), SOC 2 Type II poursuivie en parallèle (recouvrement ~70-80 % des contrôles), jalons Type I (2-3 mois) puis Type II (observation 3-12 mois). Signal GTM long-lead-time critique (marché : la certif doit être LANCÉE avant le premier cycle grand compte, sinon le deal cale 6-12 mois). Distinct de -01 (artefact rapport SOC 2) et -02 (certificat ISO obtenu) : ici l'ENGAGEMENT de calendrier opposable tant que la certif n'est pas acquise (réponse au DPO/achats « où en êtes-vous ? »). Volet Trust Center (-05) + kit procurement (-14). decisionTool=false. | marche | Hypothèse |
| F-DRH-CORP-READY-20 | Matrice de contrôles partagée multi-référentiels (ISO 27001 ↔ SOC 2 ↔ CCM/CAIQ) — livrable procurement | Matrice de correspondance des contrôles de sécurité entre référentiels (ISO 27001 Annexe A, critères SOC 2 TSC, CCM/CAIQ de la CSA) avec preuve unique réutilisée par contrôle — exploite le recouvrement ~70-80 % pour répondre une seule fois à plusieurs cadres. Livrable de procurement exposé/téléchargeable, qui alimente le pré-remplissage du CAIQ (-06) et des réponses SIG/VSA (-07). ⚠️ FEATURE PRODUIT (D10) = la matrice/le livrable de correspondance exposé à l'acheteur ; PAS l'outillage interne d'obtention des certifs (Vanta/Drata/Secureframe restent des fournisseurs hors périmètre produit, D11). Distinct de -08 (preuves brutes) : ici la cartographie inter-référentiels. Volet Trust Center (-05). decisionTool=false. | concurrent-gap | Hypothèse |
| F-DRH-CORP-READY-21 | Engagement de réponse à incident & notification de violation (RGPD Art. 33/34) | Engagement documenté et exposé de procédure de réponse à incident + notification de violation de données : notification au responsable de traitement (employeur client) sans délai indu, contenu type de la notification, délai de l'employeur vers la CNIL (72 h). Mesure de sécurité explicitement attendue au questionnaire achats et exigée par le DPA (Art. 28). Distinct du DPA contrat (-03, la CLAUSE) et des audit logs (-10, la traçabilité) : ici la CAPACITÉ opérationnelle + l'engagement documenté côté sous-traitant. Volet Trust Center (-05) / kit DPO (-15). decisionTool=false. | marche | Hypothèse |
| F-DRH-CORP-READY-22 | Notification & droit d'opposition aux changements de sous-traitants ultérieurs (RGPD Art. 28.2) | Mécanisme de notification préalable des ajouts/changements de sous-traitants ultérieurs et fenêtre d'opposition motivée du client, conformément à l'Art. 28(2) RGPD. Distinct de -04 (la LISTE à jour, état statique) et de -18 (visibilité data residency) : ici le WORKFLOW de notification/objection dans le temps, obligation contractuelle DPA réelle. Neutralise une friction DPO récurrente sur les sous-processeurs (Anthropic/OpenAI/Textract/AWS). Réutilise l'audit/notification de la plateforme. Articule -03 (DPA) + -04 (liste). decisionTool=false. | droit-travail | Hypothèse |
| F-DRH-CORP-READY-23 | Support des demandes d'audit client & droit d'audit (RGPD Art. 28.3.h) | Capacité opérationnelle de répondre aux demandes d'audit/évaluation du client (mise à disposition des informations nécessaires, accueil des audits/inspections mandatés) prévue à l'Art. 28(3)(h) — souvent via rapports SOC 2/ISO en lieu et place d'un audit sur site. Distinct de -03 (la CLAUSE DPA), de -01/-02 (les rapports eux-mêmes) et de -08 (preuves de contrôles) : ici le PROCESSUS de prise en charge d'une demande d'audit (mode substitution par rapports, périmètre, NDA). Exigence DPA dure côté grand compte. Volet kit procurement (-14) / Trust Center (-05). decisionTool=false. | marche | Hypothèse |

**Ajoutées run 2026-06-06 (RUN 3, APPEND) :** F-DRH-CORP-READY-19, -20, -21, -22, -23.
- `F-DRH-CORP-READY-19` — Nouvelle — roadmap de certification datée/séquencée (ISO d'abord + SOC 2 parallèle, jalons Type I/II). Comble le trou GTM long-lead-time (certif à LANCER avant le 1er cycle grand compte). Distinct de -01 (rapport SOC 2) et -02 (certificat ISO). decisionTool=false.
- `F-DRH-CORP-READY-20` — Nouvelle — matrice de contrôles multi-référentiels (ISO↔SOC 2↔CCM/CAIQ) exposée comme livrable procurement, exploite le recouvrement ~70-80 %. Cadrée PRODUIT (D10) ; les compliance-automation (Vanta/Drata/Secureframe) restent FOURNISSEURS hors produit (D11). decisionTool=false.
- `F-DRH-CORP-READY-21` — Nouvelle — engagement réponse à incident + notification de violation (RGPD Art. 33/34). Mesure de sécurité/DPA attendue, distincte du DPA contrat (-03) et des audit logs (-10). decisionTool=false.
- `F-DRH-CORP-READY-22` — Nouvelle — workflow de notification/opposition aux changements de sous-traitants (Art. 28.2), distinct de la liste statique -04. Friction DPO réelle sur les sous-processeurs IA. decisionTool=false.
- `F-DRH-CORP-READY-23` — Nouvelle — support des demandes d'audit client / droit d'audit (Art. 28.3.h), souvent par substitution rapports SOC 2/ISO. Distinct de la clause -03 et des rapports -01/-02. decisionTool=false.

**Ajoutées run 2026-06-05 (APPEND) :** F-DRH-CORP-READY-15, -16, -17, -18.
- `F-DRH-CORP-READY-15` — Conservée + désignée feature de référence du kit DPO ; F-DRH-AI-ACT-14 (même contenu) consolidée ici. Contradiction inter-domaines résolue. decisionTool=false.
- `F-DRH-CORP-READY-16` — Conservée — engagement de turnaround (vélocité) distinct du contenu pré-rempli (-06/-07). Avantage monétisable D6. decisionTool=false.
- `F-DRH-CORP-READY-17` — Conservée — SLA/uptime listé comme contenu Trust Center attendu, non décliné en feature (-05 = portail, -08 = preuves statiques). Cadré produit (D10), pas infra (D11). decisionTool=false.
- `F-DRH-CORP-READY-18` — Conservée — angle visibilité anticipée / réponse pré-formatée non porté par -04/-12. Accélérateur procurement. decisionTool=false.

**Modifiées / justifiées (curation) :**

- `F-DRH-CORP-READY-15` — (run 2026-06-05) Désignée feature de référence du kit DPO ; F-DRH-AI-ACT-14 (même contenu : registre type + base légale contentieux RH + durées CNIL) consolidée ici. Résout la contradiction inter-domaines AI-ACT-14 ↔ CORP-READY-15.
- `F-DRH-CORP-READY-11` — (run 2026-06-05) Articulée avec PLATFORM-06 (isolation multi-buyer = fondation D8) : CORP-READY-11 en est la documentation opposable côté procurement, pas un second mécanisme d'isolation.
- `F-134` — Statut réel rétabli (V9+) ; titre PRODUCT_SPEC rappelé.
- `F-DRH-CORP-READY-05` — Désignée référence Trust Center.
- `F-DRH-CORP-READY-09` — Recoupement F-22 + domaine SSO signalé.
- `F-DRH-CORP-READY-10` — Référence audit logs ; recoupement AUDIT-LOG/AI-ACT-07 ; F-38 réutilisé.
- `F-DRH-CORP-READY-11` — Recoupement AUDIT-LOG-08 signalé.
- `F-DRH-CORP-READY-13` — Recoupement API-SIRH-01 signalé.
- `F-DRH-CORP-READY-14` — Désignée référence kit procurement.

---

### Domaine — Audit logs avancés & traçabilité compliance (`AUDIT-LOG`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-DRH-AUDIT-LOG-01 | Journal d'actions utilisateur granulaire — feature de référence (extend F-38) | Trace granulaire des actions métier sur dossier salarié. EXTEND F-38 (audit_logs existant, /workspace/audit-logs). Feature de référence audit logs ; recoupe F-DRH-CORP-READY-10, F-DRH-AI-ACT-07, F-DRH-DASHBOARD-08. ⚠️ Tout ce domaine AUDIT-LOG + CORP-READY-10 + AI-ACT-07 + DASHBOARD-08 = UNE famille à consolider. D8. | corporate-readiness | Hypothèse |
| F-DRH-AUDIT-LOG-02 | Journal d'actions IA (analyse, résultat, confiance, override) — recoupe AI-ACT-01 | ⚠️ Recoupe F-DRH-AI-ACT-01 (journal contrôle humain) — ici = couche d'enregistrement brute des événements IA. Réutilise F-37 (versioning/audit trail). Articulation correcte mais à expliciter (brut vs conformité). | corporate-readiness | Hypothèse |
| F-DRH-AUDIT-LOG-03 | Exportabilité des logs (JSON/CSV) pour SIEM & archivage DPO | Export SIEM/archivage. Situation distincte légitime. Conservé. | marche | Hypothèse |
| F-DRH-AUDIT-LOG-04 | Filtres & recherche des logs — recoupe F-38 (/workspace/audit-logs a déjà recherche+filtre) | ⚠️ F-38 existant inclut DÉJÀ 'recherche et filtre' sur /workspace/audit-logs. La valeur ajoutée = filtre par sensibilité RGPD. À cadrer comme EXTENSION de F-38, pas nouvelle feature complète. | corporate-readiness | Hypothèse |
| F-DRH-AUDIT-LOG-05 | Classification de sensibilité RGPD des actions journalisées | Étiquetage RGPD alimentant les filtres (-04). Pré-requis de la valeur de -04. Conservé. | droit-travail | Hypothèse |
| F-DRH-AUDIT-LOG-06 | Immutabilité des logs (tamper-proof, hash chaîné) | Append-only + chaînage hash. Situation distincte non couverte. Conservé. | corporate-readiness | Hypothèse |
| F-DRH-AUDIT-LOG-07 | Rétention configurable des logs ≥ 6 mois (AI Act) | Politique de rétention paramétrable ≥ 6 mois. Conservé. | corporate-readiness | Hypothèse |
| F-DRH-AUDIT-LOG-08 | Isolation multi-tenant prouvée des journaux — recoupe CORP-READY-11/PLATFORM-06 | Recoupe CORP-READY-11 (isolation globale) et PLATFORM-06 (isolation multi-buyer). Ici = cloisonnement des JOURNAUX. À articuler. D8. | corporate-readiness | Hypothèse |
| F-DRH-AUDIT-LOG-09 | Trail d'accès en consultation (lecture/téléchargement) — complète F-38 | Logs d'accès en lecture (consultation, téléchargement, export). Complète les logs d'écriture (-01/F-38). Gap réel (F-38 trace les actions sensibles d'écriture). Conservé. | corporate-readiness | Hypothèse |
| F-DRH-AUDIT-LOG-10 | Publication des logs au Trust Center — DOUBLON kit procurement | ⚠️ DOUBLON avec F-DRH-CORP-READY-05/14 + F-DRH-AI-ACT-08 (kit/Trust Center). Volet 'logs' du kit. À consolider. | concurrent-gap | Hypothèse |
| F-DRH-AUDIT-LOG-11 | Détection d'anomalies & alertes sur les accès aux dossiers salariés | Surveillance proactive des journaux : téléchargement massif, accès hors horaires, pics de consultation, export inhabituel. SOC 2 Type II exige des logs 'pour détection d'anomalies'. Distinct de 01-10 (qui enregistrent/exportent/filtrent mais ne DÉTECTENT pas) : couche d'analyse au-dessus de F-38. D8 : protection des données salariés, jamais surveillance des salariés. Réutilise les events F-38 + classification RGPD (-05). | marche | Hypothèse |
| F-DRH-AUDIT-LOG-12 | Rapport d'accès par salarié pour le DPO (droit d'accès RGPD / référentiel CNIL RH) | Vue consolidée 'qui a consulté/modifié/exporté le dossier de tel salarié, quand, d'où' à la demande du DPO. Le référentiel RH CNIL + droit d'accès RGPD imposent de restituer les accès. Distinct de -09 (capture du trail brut) : ici livrable orienté personne concernée, exploitable sans SIEM. Distinct de -03 (export SIEM technique). Réutilise -09 + classification RGPD (-05). D8 : conformité côté employeur, pas profilage du salarié. | droit-travail | Hypothèse |
| F-DRH-AUDIT-LOG-13 | Horodatage de confiance des entrées de journal (preuve datée opposable) | Horodatage fiable/synchronisé (source de temps de confiance) sur chaque entrée, garantissant antériorité et ordre opposables. Complète -06 (intégrité/non-altération) en ajoutant la preuve de DATE — nécessaire à la fiche de provision IAS 37 datée (CHIFFRAGE-11), au journal AI Act horodaté ≥ 6 mois (AI-ACT-01), à la défense d'un audit trail. ⚠️ Recoupe PREAVOCAT-09 (export versionné/horodaté) — à articuler. Distinct de -06 (intégrité) et -07 (rétention). Réutilise la chaîne de hash de -06. | corporate-readiness | Hypothèse |
| F-DRH-AUDIT-LOG-14 | Pack de preuves d'audit pour évaluation SOC 2 / ISO 27001 (auditor evidence) | Export structuré orienté AUDITEUR : extraits de journaux corrélés aux contrôles SOC 2 (CC) / ISO 27001 (A.) pertinents (accès, journalisation, détection d'anomalies), période d'observation + attestation d'intégrité. Distinct de -03 (export SIEM pour l'exploitation client) et de -10/Trust Center (publication procurement) : ici livrable consommé pendant l'audit de certif de LegalCase, réutilisable par le client pour SA certif. Accélère la legal/security review (4-12 sem → jours). Réutilise -03 + classification (-05) + horodatage (-13). | corporate-readiness | Hypothèse |

| F-DRH-AUDIT-LOG-15 | Journal des changements de configuration & d'administration (rôles, settings, intégrations SIRH) — change management SOC 2 | Trace dédiée des événements d'ADMINISTRATION du workspace, distincts des actions sur dossier salarié (-01/F-38) et des événements d'identité (SSO-09) : changement de rôle/permission, modification des settings, activation/désactivation d'un connecteur SIRH, changement de politique de rétention (-07), modification du type d'acteur (PLATFORM-01, attribut figé — toute tentative journalisée), ajout/révocation d'intégration. Avant/après valeur, opérateur, horodatage de confiance (-13). Répond au contrôle SOC 2 Type II « change management » + « access controls » + traçabilité RGPD des habilitations. Distinct de SSO-07 (état instantané) et SSO-15 (revue périodique) : ici l'HISTORIQUE horodaté des changements. Réutilise F-38 + classification RGPD (-05) + horodatage (-13). D8/D10. | droit-travail | Hypothèse |
| F-DRH-AUDIT-LOG-16 | Workflow de notification de violation de données (RGPD Art. 33/34) déclenché depuis la détection d'anomalies | Boucle de conformité au-dessus de la détection d'anomalies (-11) : quand un accès suspect aux dossiers salariés est confirmé (téléchargement massif, export inhabituel, accès hors périmètre), produit le dossier de violation présumée (journaux corrélés -01/-09, nature/volume des données -05, horodatage opposable -13) et outille la notification CNIL 72 h (Art. 33) + information des personnes si risque élevé (Art. 34) : trame pré-remplie, registre des violations, suivi du délai. Distinct de -11 (DÉTECTE) et -12 (rapport routine DPO) : ici l'OBLIGATION DE NOTIFICATION. ⚠️ Recoupe CORP-READY-21 (engagement de notification côté éditeur) : AUDIT-LOG-16 = workflow côté employeur responsable de traitement — à articuler. D8/D10. | marche | Hypothèse |
| F-DRH-AUDIT-LOG-17 | Certificat de purge / preuve de fin de conservation (RGPD durées CNIL RH) — clôture du cycle de vie de la donnée | Preuve OPPOSABLE qu'une suppression/purge a eu lieu à l'échéance des durées de conservation : à chaque purge automatique (politique -07, référentiel CNIL durées RH 02/04/2026) ou suppression manuelle (F-38), génère une entrée immuable (-06) horodatée (-13) attestant QUOI supprimé, QUAND, par QUELLE règle, sans réintroduire la donnée (métadonnées seulement). Certificat de purge daté/signé exploitable par le DPO. Distinct de -07 (DÉFINIT la politique) et F-38 (exécute la suppression) : ici la PREUVE de fin de conservation (accountability Art. 5.2). Réutilise -06/-07/-13/-05. ⚠️ Recoupe PLATFORM-12 (durées de conservation dans le pipeline) : ici la PREUVE de purge côté livrable client. D8/D10. | droit-travail | Hypothèse |

**Ajoutées ce run (APPEND 2026-06-06 — run de maturation) :** F-DRH-AUDIT-LOG-15, -16, -17.
- `F-DRH-AUDIT-LOG-15` — Conservée — journal change management (rôles/settings/intégrations), contrôle SOC 2 Type II ; historique horodaté distinct de SSO-07/15. decisionTool=false.
- `F-DRH-AUDIT-LOG-16` — Conservée — workflow notification de violation (Art. 33/34) côté employeur ; à articuler avec CORP-READY-21 (côté éditeur). decisionTool=false.
- `F-DRH-AUDIT-LOG-17` — Conservée — certificat de purge / preuve de fin de conservation (accountability Art. 5.2) ; à articuler avec PLATFORM-12 (durées pipeline). decisionTool=false.

**Modifiées / justifiées (curation 2026-06-06) :**
- `F-DRH-AUDIT-LOG-16` — Anti-doublon explicité avec CORP-READY-21 (engagement de notification côté éditeur vs workflow côté employeur responsable de traitement).
- `F-DRH-AUDIT-LOG-17` — Anti-doublon explicité avec PLATFORM-12 (durées de conservation dans le pipeline) ; AUDIT-LOG-17 = preuve de purge opposable côté livrable client.

**Suppressions proposées (2026-06-06, non appliquées — appliedDeletions vide, D4) :**
- `F-DRH-AUDIT-LOG-10` — Doublon du volet « logs » du kit procurement / Trust Center (CORP-READY-05/14 + AI-ACT-08). La publication des logs au Trust Center est un angle du kit, pas une situation audit distincte. Non appliqué (D4 — touche le périmètre CORP-READY) ; appliedDeletions vide.

**Ajoutées ce run (APPEND 2026-06-05) :** F-DRH-AUDIT-LOG-11, -12, -13, -14.
- `F-DRH-AUDIT-LOG-11` — Conservée — alerte/détection proactive non couverte par 01-10 (enregistrement passif). Gate procurement (SOC 2). D8 explicité. decisionTool=false.
- `F-DRH-AUDIT-LOG-12` — Conservée — rapport d'accès PAR SALARIÉ (droit d'accès RGPD) non produit par les features existantes. D8 explicité. decisionTool=false.
- `F-DRH-AUDIT-LOG-13` — Conservée — preuve de DATE de confiance distincte de l'immutabilité (-06). Recoupement PREAVOCAT-09 signalé. decisionTool=false.
- `F-DRH-AUDIT-LOG-14` — Conservée — export orienté AUDITEUR corrélé aux contrôles, distinct de l'export SIEM (-03) et du Trust Center (-10). Raccourcit le cycle de vente DRH. decisionTool=false.

**Modifiées / justifiées (curation) :**

- `F-DRH-AUDIT-LOG-08` — (run 2026-06-05) Articulée avec PLATFORM-06 (isolation multi-buyer = fondation D8) et CORP-READY-11 (isolation globale documentée) : AUDIT-LOG-08 = cloisonnement des journaux, sous-cas de la fondation.
- `F-DRH-AUDIT-LOG-01` — Référence granulaire, extend F-38 ; famille audit logs à consolider.
- `F-DRH-AUDIT-LOG-02` — Recoupement AI-ACT-01 ; F-37 réutilisé.
- `F-DRH-AUDIT-LOG-04` — F-38 a déjà recherche/filtre — recadré en extension RGPD.
- `F-DRH-AUDIT-LOG-08` — Recoupement CORP-READY-11 signalé.
- `F-DRH-AUDIT-LOG-10` — Doublon kit procurement signalé.

**Suppressions proposées :**

- `F-DRH-AUDIT-LOG-10` — Doublon du volet 'logs' du kit procurement (F-DRH-CORP-READY-05/14 + F-DRH-AI-ACT-08).

---

### Domaine — API documentée & intégrations SIRH (`API-SIRH`)

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-22 | SSO entreprise (Azure AD, Google Workspace, SAML) — socle auth des connecteurs SIRH | Feature EXISTANTE (PRODUCT_SPEC l.432, V9+). Rattachée au domaine comme socle auth/provisioning des connecteurs SIRH, SANS réécriture de scope. ⚠️ INCOHÉRENCE corrigée : le titre/scope F-22 réel est 'Azure AD, Google Workspace, SAML' — ne pas réécrire en SCIM/Okta dans le titre. Réutilise F-01. | corporate-readiness | V9+ — Grands comptes |
| F-DRH-API-SIRH-01 | API REST documentée (OpenAPI 3.0) — recoupe CORP-READY-13, feature de référence API | API REST versionnée OpenAPI 3.0. ⚠️ Recoupe F-DRH-CORP-READY-13 (API documentée). Feature de référence API ; CORP-READY-13 = angle procurement. Gate D10. Pas d'infra (D11) : contrat produit. | corporate-readiness | Hypothèse |
| F-DRH-API-SIRH-02 | Connexion SIRH par OAuth 2.0 (PayFit, Lucca, Cegid, Silae) | OAuth 2.0 vers SIRH (consentement, scopes minimaux, révocation). Situation distincte. Réutilise F-01 (socle OAuth2). Gap réel (canal distribution). Conservé. | marche | Hypothèse |
| F-DRH-API-SIRH-03 | Pré-remplissage automatique du contexte employé depuis le SIRH — recoupe PLATFORM-05 | ⚠️ Recoupe F-DRH-PLATFORM-05 (pré-remplissage dossier via SIRH). À consolider. Invariant D3 bien préservé (upload pièces reste manuel). Réutilise F-05/F-06 (upload/extraction). | marche | Hypothèse |
| F-DRH-API-SIRH-04 | Récupération CCN depuis le SIRH → moteur CCN-aware (F-DT-07) — alimente la capacité CCN unifiée | Importe l'IDCC du SIRH et l'injecte dans F-DT-07 (CCN-aware existant) + capacité CCN transverse. Situation distincte (amont) légitime. ⚠️ La CCN-aware elle-même existe (F-DT-07) — ici c'est l'ALIMENTATION amont. Conservé. | concurrent-gap | Hypothèse |
| F-DRH-API-SIRH-05 | Webhooks de mise à jour des données amont (rôle, fin d'emploi, mobilité) — ENTRANTS | Webhooks SIRH ENTRANTS pour fraîcheur du contexte employé (le SIRH notifie LegalCase). Situation distincte du webhook SORTANT produit (API-SIRH-13). Conservé. | droit-travail | Hypothèse |
| F-DRH-API-SIRH-06 | Mapping & réconciliation employé SIRH ↔ dossier-salarié (revue avant import) | Rapprochement + revue humaine avant import. Situation distincte. Réutilise F-DRH-PLATFORM-03 (dossier centré-salarié). Conservé. | concurrent-gap | Hypothèse |
| F-DRH-API-SIRH-07 | Connecteurs SIRH packagés (catalogue self-service) | Catalogue de connecteurs activables sans dev. Situation distincte. Conservé. | marche | Hypothèse |
| F-DRH-API-SIRH-08 | Journal d'audit des accès API & imports SIRH — DOUBLON AUDIT-LOG/AI-ACT-07 | ⚠️ DOUBLON avec la famille audit logs (AUDIT-LOG-01, CORP-READY-10, AI-ACT-07). Volet 'API/imports' des audit logs. À consolider. | corporate-readiness | Hypothèse |
| F-DRH-API-SIRH-09 | Garde-fou D8 sur les données importées (finalité conformité/chiffrage, jamais profilage) | Limite l'usage des données SIRH au périmètre conformité/chiffrage, interdit le profilage de masse. Matérialise D8 + RGPD (minimisation). ⚠️ D8 est un INVARIANT projet (CLAUDE.md/cadrage) qui devrait être un garde-fou transverse, pas une feature locale — mais sa matérialisation produit ici est légitime. Conservé avec note. | droit-travail | Hypothèse |
| F-DRH-API-SIRH-10 | Argumentaire partenariat SIRH (positionnement canal) — tâche marketing, pas feature produit | ⚠️ INCOHÉRENCE typologie : asset commercial/co-marketing/référencement marketplace = TÂCHE MARKETING (MARKETING_BACKLOG.md), pas feature PRODUCT_SPEC. Soumettre au contrôle de cohérence marketing 4 points. | concurrent-gap | Hypothèse |
| F-DRH-API-SIRH-11 | Authentification API par service account (OAuth2 client_credentials / bearer token) | Flux d'authentification machine-to-machine pour les intégrations serveur du SIRH/SI client : OAuth2 client_credentials, jetons bearer à durée limitée, rotation et révocation. Distinct de F-22 (SSO utilisateur interactif) et de API-SIRH-02 (OAuth utilisateur délégué vers le SIRH) : ici AUTH DE L'APPELANT vers l'API LegalCase. Réutilise le socle Spring Security + OAuth2 (F-01) en resource server. Pas d'infra (D11) : contrat produit. Garde-fou D7 (scopes liés au workspace EMPLOYEUR) + D8 (scopes finalité conformité/chiffrage). | corporate-readiness | Hypothèse |
| F-DRH-API-SIRH-12 | Endpoints d'export programmatique (chiffrage, actes, analyses, fiche de provision) | Endpoints API de RÉCUPÉRATION des livrables : chiffrage d'exposition (CHIFFRAGE-07), actes/courriers (ACTES), analyses (PLATFORM), fiche de provision IAS 37 (CHIFFRAGE-11, pont DRH↔DAF). La valeur LegalCase doit ressortir vers SIRH/GED/outil de provisioning. Distinct de API-SIRH-03 (import ENTRANT). Réutilise les outils existants (lecture seule, JSON/PDF/Excel). Garde-fou D7 (export borné au workspace) + D8 (export = mon risque, jamais profilage salarié exporté en masse). | marche | Hypothèse |
| F-DRH-API-SIRH-13 | Webhooks d'événements produit (analyse terminée, conclusion disponible, provision recalculée) — SORTANTS | Webhooks SORTANTS sur événements LegalCase pour intégration asynchrone : 'analyse terminée', 'conclusion/acte disponible', 'provision recalculée', 'alerte d'exposition franchie'. Le pipeline IA étant asynchrone (CLAUDE.md), un SI intégrateur a besoin d'être notifié plutôt que de poller. Distinct de API-SIRH-05 (webhooks ENTRANTS SIRH→LegalCase) : ici LegalCase NOTIFIE le SI client. Réutilise le statut des jobs async existants. Pas d'infra (D11). Garde-fou D7 (events scoping workspace) + D8 (payload = métadonnées, pas de profilage). | corporate-readiness | Hypothèse |
| F-DRH-API-SIRH-14 | Rate limiting, quotas & métrage d'usage de l'API (par workspace) | Limitation de débit, quotas et compteurs d'usage par workspace EMPLOYEUR, exposés au client (headers de quota, tableau de consommation). Prérequis d'une API publique corporate (protection + prévisibilité) ET socle d'un pricing variable (base plateforme + composante par dossier/outcome). Réutilise les usage_events existants (F-257/AnthropicService, migration 447). Distinct du pricing lui-même (décision PO/DAF, D9). Garde-fou D11 : ici le CONTRAT produit de quota/métrage, pas l'infra d'application. | marche | Hypothèse |
| F-DRH-API-SIRH-15 | Portail développeur & sandbox (clés API self-service, données fictives) | Portail self-service : génération/rotation de clés API, console OpenAPI interactive, sandbox à données salariées FICTIVES pour tester l'intégration sans exposer de données réelles. Le sandbox accélère le cycle d'intégration (technical evaluation 4-8 sem, D6) et est un argument procurement (pas de PII en test). Réutilise OpenAPI (API-SIRH-01) + auth service account (API-SIRH-11). Garde-fou D8 : sandbox cloisonné, jamais de données salariées réelles. | corporate-readiness | Hypothèse |
| F-DRH-API-SIRH-16 | DPA & cartographie des sous-processeurs incluant les flux SIRH — recoupe CORP-READY (angle intégration) | Volet DPA spécifique aux INTÉGRATIONS : annexe sous-processeurs couvrant les flux SIRH↔LegalCase (Anthropic/OpenAI/Textract/AWS + le SIRH connecté comme source), géolocalisation (EU/eu-west-3), sécurité du transit, droits d'audit. Le DPA Art. 28 est bloquant ; dès qu'un connecteur fait circuler des données salariés, la liste géolocalisée devient une exigence dure du DPO. ⚠️ Recoupe CORP-READY-03/04/15 (DPA self-serve + sous-traitants + kit DPO) : ici angle 'flux d'intégration SIRH', à articuler avec CORP-READY (pas réécrire). Garde-fou D8. | corporate-readiness | Hypothèse |

| F-DRH-API-SIRH-17 | Provisioning automatique des accès (SCIM 2.0) — création / désactivation centralisée des utilisateurs | Provisioning/déprovisioning automatiques des comptes via SCIM 2.0 depuis l'IdP (Entra ID/Okta) : création à l'arrivée, révocation centralisée au départ, synchronisation des rôles. Le procurement exige « visibilité ET révocation centralisées » (D10) que F-22 (SSO = AUTHENTIFICATION au login) NE couvre PAS. Situation distincte (lifecycle vs login). ⚠️ Recoupe SSO-06 (SCIM) : à consolider — une seule feature SCIM (référence à fixer entre API-SIRH-17 et SSO-06). Réutilise F-01/F-22. D7 (provisioning borné au workspace EMPLOYEUR). V9+ Grands comptes. | corporate-readiness | Hypothèse |
| F-DRH-API-SIRH-18 | Status page publique, uptime & SLA d'API (disponibilité, incidents, communication) | Page de statut publique de l'API et des connecteurs : disponibilité/uptime, historique des incidents, maintenance, communication, engagement SLA. Besoin « status page / incident communication » + attendu du Trust Center. Accélère l'évaluation technique (D6). Distinct du métrage de quota client (API-SIRH-14). ⚠️ Recoupe CORP-READY-17 (page de statut SLA/uptime au Trust Center) : à consolider — une seule page de statut, volet API référencé. Pas d'infra de monitoring (D11) : SURFACE produit. | corporate-readiness | Hypothèse |
| F-DRH-API-SIRH-19 | Politique de versioning & de dépréciation de l'API (stabilité de contrat) | Politique publique de cycle de vie de l'API : versionnement (v1/v2), garanties de non-rupture intra-version, calendrier de dépréciation, fenêtres de support, en-têtes/sunset, changelog. Le procurement évalue la STABILITÉ du contrat d'intégration (pas seulement l'existence d'une doc OpenAPI). Distinct de API-SIRH-01 (qui DÉCRIT l'API à un instant T) : ici la garantie de PÉRENNITÉ dans le temps. Réutilise OpenAPI (API-SIRH-01). Pas d'infra (D11) : engagement produit/contractuel. | corporate-readiness | Hypothèse |
| F-DRH-API-SIRH-20 | SDKs officiels d'intégration (Python, Node.js, Java) pour intégrateurs SIRH | Bibliothèques clientes officielles (Python, Node.js, Java) encapsulant l'auth service account (API-SIRH-11), les endpoints d'export (API-SIRH-12), le rate limiting (API-SIRH-14) et la vérification des webhooks (API-SIRH-13). Besoin explicite des situations cibles. Réduit le coût d'intégration côté éditeur SIRH / DSI client, raccourcit le technical evaluation (D6). Distinct du portail/sandbox (API-SIRH-15 = environnement de test) : ici le CODE client packagé. Réutilise OpenAPI (API-SIRH-01). D8 (SDKs n'exposent que les scopes finalité conformité/chiffrage). | marche | Hypothèse |

**Ajoutées ce run (APPEND 2026-06-06 — run de maturation) :** F-DRH-API-SIRH-17, -18, -19, -20.
- `F-DRH-API-SIRH-17` — Conservée — SCIM 2.0 (provisioning/déprovisioning), situation lifecycle distincte de F-22 (login). ⚠️ Doublon SCIM avec SSO-06 signalé, une seule référence à fixer, portage à consolider. decisionTool=false.
- `F-DRH-API-SIRH-18` — Conservée — status page/uptime/SLA, volet API. ⚠️ Doublon avec CORP-READY-17 (page de statut de référence), portage à consolider. decisionTool=false.
- `F-DRH-API-SIRH-19` — Conservée — politique de versioning/dépréciation (stabilité de contrat), distincte de la doc OpenAPI API-SIRH-01. decisionTool=false.
- `F-DRH-API-SIRH-20` — Conservée — SDKs officiels (Python/Node/Java), distinct du portail/sandbox API-SIRH-15. decisionTool=false.

**Modifiées / justifiées (curation 2026-06-06) :**
- `F-DRH-API-SIRH-14` — Renvoi explicite au sujet UX non tranché (métrage ⊂ analyses CALCULÉES/persistées uniquement, pas champs pré-remplis non cliqués) à arbitrer avant dev. Aucune réécriture de scope.
- `F-22` — Précision (sans réécriture de scope/titre) que SSO/OIDC+MFA = exigence d'accès STANDARD du procurement (D10), pour ancrer la distinction avec SCIM (API-SIRH-17 = lifecycle des comptes, non couvert par F-22). Statut V9+ inchangé.
- `F-DRH-API-SIRH-17` — Doublon SCIM avec SSO-06 signalé — une seule feature SCIM de référence à fixer ; conservée comme situation lifecycle distincte de F-22, portage à consolider.
- `F-DRH-API-SIRH-18` — Doublon signalé avec CORP-READY-17 (page de statut SLA/uptime) — une seule page de statut de référence, volet API référencé.

**Suppressions proposées (2026-06-06, non appliquées — appliedDeletions vide, D4) :**
- `F-DRH-API-SIRH-08` — Doublon de la famille audit logs (AUDIT-LOG-01, CORP-READY-10, AI-ACT-07, base F-38). Volet « API/imports » des audit logs. À consolider. Non appliqué (D4 — matérialisation de la capacité transverse audit logs, décision PO) ; appliedDeletions vide.
- `F-DRH-API-SIRH-10` — Argumentaire partenariat SIRH / co-marketing / référencement marketplace = TÂCHE MARKETING (MARKETING_BACKLOG.md), hors typologie feature produit PRODUCT_SPEC. À transférer via le contrôle de cohérence marketing 4 points (CLAUDE.md règle 2). Non appliqué (D4 — transfert vers MARKETING_BACKLOG = décision PO) ; appliedDeletions vide.

**Ajoutées ce run (APPEND 2026-06-05) :** F-DRH-API-SIRH-11, -12, -13, -14, -15, -16.
- `F-DRH-API-SIRH-11` — Conservée — auth machine-to-machine non couverte par F-22 (SSO interactif) ni API-SIRH-02 (OAuth délégué). Gap critère d'achat corporate.
- `F-DRH-API-SIRH-12` — Conservée — EXPORT programmatique des livrables (+ pont DAF) non porté (API-SIRH-03 = import entrant). Gap marché.
- `F-DRH-API-SIRH-13` — Conservée — sens SORTANT LegalCase→SI client distinct de API-SIRH-05 (entrant). Cohérent avec la nature async du pipeline (CLAUDE.md).
- `F-DRH-API-SIRH-14` — Conservée — métrage/quota d'API non porté ; réutilise usage_events (F-257). Socle pricing variable (D9). Cadré produit, pas infra (D11).
- `F-DRH-API-SIRH-15` — Conservée — portail/sandbox/clés self-service non couvert par la doc OpenAPI seule (API-SIRH-01). Accélérateur D6.
- `F-DRH-API-SIRH-16` — Conservée — angle 'flux d'intégration SIRH' du DPA non couvert. ⚠️ Recoupe CORP-READY-03/04/15 : à articuler comme volet intégration, pas réécrire le DPA self-serve.

**Modifiées / justifiées (curation) :**

- `F-DRH-API-SIRH-05` — (run 2026-06-05) Précision de scope (pas de réécriture) : explicité ENTRANT (SIRH→LegalCase) pour le distinguer de API-SIRH-13 (webhooks SORTANTS produit). Évite le doublon apparent.
- `F-DRH-API-SIRH-16` — (run 2026-06-05) Recoupement explicité avec CORP-READY-03/04/15 (DPA self-serve + sous-traitants + kit DPO) : cadrée comme volet 'flux d'intégration SIRH' du DPA, à articuler sans réécrire.
- `F-22` — Statut réel rétabli (V9+) ; titre/scope préservés ; recoupement CORP-READY-09/SSO assumé.
- `F-DRH-API-SIRH-01` — Référence API ; recoupement CORP-READY-13 signalé.
- `F-DRH-API-SIRH-02` — F-01 réutilisé.
- `F-DRH-API-SIRH-03` — Recoupement PLATFORM-05 signalé.
- `F-DRH-API-SIRH-04` — Alimentation amont CCN (F-DT-07 réutilisé).
- `F-DRH-API-SIRH-06` — PLATFORM-03 réutilisé.
- `F-DRH-API-SIRH-08` — Doublon famille audit logs signalé.
- `F-DRH-API-SIRH-09` — D8 invariant transverse noté.
- `F-DRH-API-SIRH-10` — Reclassé tâche marketing (hors PRODUCT_SPEC).

**Suppressions proposées :**

- `F-DRH-API-SIRH-10` — Argumentaire/partenariat/co-marketing = tâche MARKETING_BACKLOG, pas feature produit. À transférer via le contrôle de cohérence marketing 4 points.

---

### Domaine — Pricing & packaging compte employeur (corporate 800-3000 €/mois) (corporate-readiness) (`PRICING`)

> Comble le **manque #1 du run 2** : aucune feature ne portait la grille tarifaire corporate, le gating/packaging
> par type d'acteur, ni le métrage d'usage comme base de facturation. D9 : palier corporate **800-3000 €/mois,
> engagement annuel**, distinct des paliers avocat (**F-123 : 99/219/429**), **coexistence des deux grilles** dans
> la même app. NRR ~130 %, acheteur récurrent (D6). Socle de métrage = **F-33** (gate billing ENRICHED_ANALYSIS) +
> **F-257/usage_events** (migration 447) + **F-DRH-API-SIRH-14** (quotas/métrage API). Le **moteur de framing/
> packaging/pricing piloté par le type d'acteur existe déjà** (F-DRH-PLATFORM-07) : ce domaine porte la GRILLE et le
> MÉTRAGE, PLATFORM-07 porte l'APPLICATION transverse. Garde-fou D11 : ce sont des CONTRATS produit (grille, gating,
> compteurs), pas de l'infra de facturation (Stripe/billing engine = infra, hors périmètre fiche).

| ID | Feature | Description | Provenance | Statut |
|----|---------|-------------|------------|--------|
| F-33 | Limite de re-analyses par dossier (gate billing ENRICHED_ANALYSIS) — socle de métrage | Feature EXISTANTE (plateforme, Terminée). Gate de quota au niveau dossier sur les re-analyses enrichies. ⚠️ Rattachée au domaine PRICING comme PREMIER socle de métrage/quota déjà en prod : la composante variable du pricing employeur s'appuie dessus (re-analyses = unité facturable existante). NE PAS réécrire ; F-DRH-PRICING-* la consomment. | plateforme-reutilisee | Terminée |
| F-DRH-API-SIRH-14 | Rate limiting, quotas & métrage d'usage de l'API (par workspace) — socle de métrage API | Feature EXISTANTE du domaine API-SIRH (Hypothèse). ⚠️ Rattachée au domaine PRICING comme socle de métrage côté API : compteurs/quotas par workspace = unité variable de la grille corporate. Portée détaillée reste dans API-SIRH ; ici on consomme ses compteurs. NE PAS dupliquer. | marche | Hypothèse |
| F-DRH-PRICING-01 | Grille tarifaire corporate 3-4 paliers (base / mid / enterprise) à engagement annuel — feature de référence pricing | Définit la grille employeur D9 : 3-4 paliers (ex. base / mid / enterprise) entre 800 et 3000 €/mois, engagement annuel obligatoire, distincte des paliers avocat F-123 (99/219/429). C'est le CONTRAT produit (paliers, prix de référence, périodicité d'engagement), pas l'infra de facturation (D11). Feature de référence du domaine ; PRICING-02..09 la déclinent. ⚠️ Coexistence des deux grilles = piloté par le type d'acteur (F-DRH-PLATFORM-01) et appliqué par F-DRH-PLATFORM-07 (framing/packaging/pricing) : ce domaine fournit la GRILLE, PLATFORM-07 l'APPLIQUE. Garde-fou D9 : « à affiner par la fiche produit + le marché » (cadrage). | marche | Hypothèse |
| F-DRH-PRICING-02 | Coexistence des deux grilles (avocat F-123 ↔ employeur) sélectionnée par le type d'acteur | Garantit qu'un workspace expose UNE SEULE grille selon son type d'acteur (AVOCAT → F-123 99/219/429 ; EMPLOYEUR → grille corporate PRICING-01), sans fuite ni croisement. Déterminant marché majeur (driver D12) : les deux grilles vivent dans la même app. RÉUTILISE F-DRH-PLATFORM-01 (attribut workspace) + F-DRH-PLATFORM-07 (application du packaging/pricing) + F-DRH-PLATFORM-06 (isolation). Ce n'est pas un nouveau moteur : c'est le BRANCHEMENT grille↔type d'acteur. ⚠️ Préoccupation transversale « Plans / limites » (CLAUDE.md) : lister les gates impactés. D7 : attribut figé, jamais sélecteur bloquant. | marche | Hypothèse |
| F-DRH-PRICING-03 | Packaging des features par palier (gating EMPLOYEUR : T1 chiffrage+actes / T2 +scoring+CSE / T3 +dashboard+API SIRH) | Mappe les capacités employeur sur les paliers : T1 = chiffrage (PLATFORM-04/CHIFFRAGE) + actes (ACTES) ; T2 = + scoring d'exposition (CHIFFRAGE-07) + conformité CSE (CSE-CONFORM) ; T3 = + dashboard portefeuille (DASHBOARD-01) + intégration API/SIRH (API-SIRH). RÉUTILISE la couche de visibilité/feature-flags existante via F-DRH-PLATFORM-07. Le gating PAR PALIER est distinct du gating PAR TYPE D'ACTEUR (PRICING-02) : ici on restreint la visibilité des outils selon l'offre souscrite. ⚠️ Préoccupation transversale « Plans / limites » : nouveau gate par palier → lister les outils visibles par tier. D8 : aucun outil « contre le salarié » quel que soit le palier. | marche | Hypothèse |
| F-DRH-PRICING-04 | Composante variable du pricing : métrage d'usage facturable (dossiers traités / analyses enrichies / appels API) | Définit les UNITÉS de la composante variable (base plateforme + variable) : nombre de dossiers traités, analyses enrichies (gate F-33), appels API (compteurs F-DRH-API-SIRH-14), alignée sur la valeur « risque évité » plutôt que le pur per-seat (tendance 2026, §opportunités). C'est le CONTRAT de métrage (unités, agrégation, période), pas l'infra. RÉUTILISE F-33 + F-257/usage_events + API-SIRH-14. ⚠️ Sujet UX NON TRANCHÉ (memory project_coherence_conclusions_outils_non_calcules) appliqué au métrage : ne compter comme « dossier traité » que les analyses CALCULÉES/persistées (clic → résultat), pas les dossiers ouverts non analysés — à arbitrer avant dev (alerte / auto / laisser). D11 : contrat produit, pas billing engine. | marche | Hypothèse |
| F-DRH-PRICING-05 | Tableau de consommation employeur (suivi d'usage vs quota, projection de palier) | Expose au DRH/acheteur sa consommation (dossiers, analyses enrichies, appels API) vs les quotas du palier souscrit + projection de dépassement / suggestion de palier supérieur. Transparence = prérequis d'un pricing variable corporate (prévisibilité budgétaire DAF). RÉUTILISE les compteurs F-DRH-API-SIRH-14 (headers/tableau de quota) + usage_events (F-257) + le quota F-33. Distinct de PRICING-04 (qui DÉFINIT les unités) : ici la RESTITUTION au client. ⚠️ Préoccupation transversale « Plans / limites ». D11 : page produit, pas infra de mesure. | corporate-readiness | Hypothèse |
| F-DRH-PRICING-06 | Self-serve pricing calculator opposable au procurement (coût LegalCase vs coût évité par dossier) | Calculateur public/semi-public exposant au procurement le coût d'abonnement corporate face au coût évité (honoraires avocat ≥ 4 500 € HT/CPH, erreur de calcul d'indemnité ≈ 50 K€/an évités — argument ROI D6). Distinct de F-DRH-DASHBOARD-06 (ROI = valeur de réduction de risque calculée SUR les dossiers réels du workspace) : ici un SIMULATEUR de pricing en AMONT de l'achat, paramétrique (nb salariés, nb ruptures/an, palier visé). ⚠️ Recoupe DASHBOARD-06 (ROI) et CHIFFRAGE-09 (ROI procurement déjà signalé doublon) : à articuler — PRICING-06 = pré-vente paramétrique, DASHBOARD-06 = ROI dossier-centric post-usage. Alimente le kit procurement (CORP-READY-14). D8 : ROI « productivité/risque évité », jamais « gagner contre vos salariés ». | marche | Hypothèse |
| F-DRH-PRICING-07 | Gestion de l'engagement annuel & cycle de renouvellement (NRR ~130 %) | Porte le contrat produit du cycle annuel : terme d'engagement, date de renouvellement, fenêtre d'upsell/downgrade au renouvellement, alignée sur l'objectif NRR ~130 % (acheteur récurrent, D6). C'est le CONTRAT de cycle (terme, renouvellement, upgrade au palier supérieur), pas l'infra de subscription billing (D11). RÉUTILISE le modèle workspace + la grille PRICING-01. Distinct de PRICING-04 (métrage d'usage) : ici la PÉRIODICITÉ/engagement. D9 : engagement annuel = décision de cadrage. | marche | Hypothèse |
| F-DRH-PRICING-08 | Dépassement de quota : alerte douce, soft-cap et upgrade assisté (jamais coupure dure du service) | Comportement produit au franchissement d'un quota de palier : alerte au DRH, soft-cap gradué, proposition d'upgrade assisté — PAS de coupure dure qui bloquerait la conformité d'un dossier en cours. Cohérent avec le pattern existant côté plateforme (F-34 budget tokens : alerte + blocage ; F-33 : gate). RÉUTILISE F-34/F-33 + les compteurs API-SIRH-14. Distinct de PRICING-05 (suivi passif) : ici la RÉACTION au seuil. ⚠️ Préoccupation transversale « Plans / limites » : nouveau gate de dépassement. D8 : ne jamais bloquer une mise en conformité d'un dossier salarié en cours (sécurité juridique avant upsell). | corporate-readiness | Hypothèse |
| F-DRH-PRICING-09 | Devis/proforma corporate exportable (palier + estimation variable) pour le cycle d'achat | Génère un devis/proforma corporate (palier choisi PRICING-01 + estimation de la composante variable PRICING-04 + engagement annuel PRICING-07) exportable PDF, opposable au procurement et au DAF dans le cycle d'achat 1-3 mois (D6). RÉUTILISE F-DT-04 (export PDF) + le self-serve calculator PRICING-06. Distinct de PRICING-06 (simulateur interactif) : ici l'ARTEFACT de devis figé/daté pour le dossier d'achat. Alimente le kit procurement (CORP-READY-14). D11 : génération de document produit, pas facturation. | corporate-readiness | Hypothèse |

**Ajoutées ce run (APPEND 2026-06-06) :** F-DRH-PRICING-01→09 (domaine PRICING créé — manque #1 du run 2 comblé).
- `F-DRH-PRICING-01` — Conservée + désignée feature de référence pricing. Driver marché majeur D9 (grille corporate 800-3000 €/mois, engagement annuel). Distincte de PLATFORM-07 (qui APPLIQUE le packaging) : ici la GRILLE. decisionTool=false.
- `F-DRH-PRICING-02` — Conservée — coexistence des deux grilles = déterminant marché explicite (D12). Branchement grille↔type d'acteur (PLATFORM-01/06/07), pas nouveau moteur. Préoccupation transversale « Plans / limites » signalée. decisionTool=false.
- `F-DRH-PRICING-03` — Conservée — packaging par palier (gating EMPLOYEUR) listé comme déterminant marché. Réutilise la couche feature-flags via PLATFORM-07. Distinct du gating par type d'acteur (PRICING-02). decisionTool=false.
- `F-DRH-PRICING-04` — Conservée — composante variable (métrage) = base pricing 2026 alignée « risque évité ». Réutilise F-33 + usage_events (F-257) + API-SIRH-14. ⚠️ Sujet UX non tranché (compter le CALCULÉ/persisté, pas l'ouvert) signalé pour arbitrage avant dev. decisionTool=false.
- `F-DRH-PRICING-05` — Conservée — transparence d'usage = prérequis d'un pricing variable corporate (prévisibilité DAF). Réutilise compteurs API-SIRH-14 + usage_events. Restitution, pas nouveau métrage. decisionTool=false.
- `F-DRH-PRICING-06` — Conservée — self-serve pricing calculator opposable au procurement = déterminant marché (ROI D6). ⚠️ Recoupe DASHBOARD-06/CHIFFRAGE-09 (ROI) : articulée comme pré-vente paramétrique (DASHBOARD-06 = post-usage dossier-centric). decisionTool=true (simulateur de décision d'achat).
- `F-DRH-PRICING-07` — Conservée — engagement annuel + renouvellement (NRR ~130 %) = décision de cadrage D9/D6 non portée. Contrat de cycle, pas billing infra (D11). decisionTool=false.
- `F-DRH-PRICING-08` — Conservée — comportement au dépassement de quota non porté ; cohérent avec F-33/F-34 (alerte + blocage). Garde-fou D8 (jamais couper une mise en conformité en cours). decisionTool=false.
- `F-DRH-PRICING-09` — Conservée — devis/proforma corporate (artefact d'achat) non porté ; distinct du simulateur PRICING-06. Réutilise F-DT-04 (PDF). Alimente CORP-READY-14. decisionTool=false.

**Modifiées / justifiées (curation) :**

- `F-33` — Rattachée au domaine PRICING comme PREMIER socle de métrage (re-analyses = unité facturable existante). Aucune réécriture de scope/statut (reste plateforme/Terminée) ; cross-référence pour matérialiser le périmètre pricing réel.
- `F-DRH-API-SIRH-14` — Rattachée au domaine PRICING comme socle de métrage API (consommée par PRICING-04/05/08). Portée détaillée reste dans le domaine API-SIRH (pas de duplication, IDs préservés).

**Suppressions proposées :** aucune (domaine nouveau).

**Garde-fous appliqués au domaine :**
- D9 : grille corporate 800-3000 €/mois engagement annuel, coexistence F-123 — « à affiner par fiche produit + marché ».
- D7 : pricing/packaging pilotés par l'attribut workspace (PLATFORM-01), jamais sélecteur bloquant.
- D8 : aucun outil « contre le salarié » par palier ; jamais couper une mise en conformité en cours pour forcer l'upsell.
- D11 : CONTRATS produit (grille, gating, compteurs, devis) — l'infra de facturation (billing engine/Stripe) est hors périmètre.
- Anti-doublon : PLATFORM-07 APPLIQUE le packaging (ce domaine fournit la GRILLE) ; ROI post-usage = DASHBOARD-06 (PRICING-06 = pré-vente) ; métrage API = API-SIRH-14 (consommé, pas dupliqué).
- ⚠️ Sujet UX NON TRANCHÉ (PRICING-04) : métrer le CALCULÉ/persisté, pas l'ouvert — à arbitrer avant dev.

---

## Table de synthèse

| Domaine | Nb features | Priorité marché | Provenance dominante |
|---------|-------------|-----------------|----------------------|
| Chiffrage de l'exposition prud'homale & indemnités (situation-employeur) (`CHIFFRAGE`) | 17 | Haute (pont DAF) | plateforme-reutilisee / marche |
| Sécurisation procédurale des ruptures & actes (situation-employeur) (`SECU-PROC`) | 18 | Haute (anti-vice) | concurrent-gap |
| Inaptitude médicale & obligation de reclassement (situation-employeur) (`inaptitude-reclassement`) | 18 | Haute | concurrent-gap / droit-travail |
| Sanctions disciplinaires & proportionnalité (situation employeur) (`SANCTION`) | 15 | Haute (gap réel) | concurrent-gap / droit-travail |
| Tableau de bord du risque social consolidé (`situation-employeur`) | 14 | Haute (DAF/DJ) | marche / concurrent-gap |
| Mode pré-avocat : structuration & export de dossier (situation-employeur) (`PREAVOCAT`) | 14 | Haute (ROI) | concurrent-gap / marche |
| Génération d'actes & courriers RH conformes (`situation-employeur`) | 16 | Haute (volume) | droit-travail / concurrent-gap |
| Requalification CDD → CDI & chiffrage du risque (`REQUAL-CDD`) | 16 | Moyenne-haute | plateforme-reutilisee / marche |
| Égalité F/H & prévention discrimination / harcèlement (situation employeur) (`DISCRIM-HARC`) | 17 | Moyenne-haute | concurrent-gap / marche |
| Conformité Règlement IA (AI Act Annexe III) (`AI-ACT`) | 17 | Gate dur (D10) | corporate-readiness |
| SSO/OIDC entreprise & authentication (corporate-readiness) (`SSO`) | 16 (dont F-22 existant) | Gate procurement | corporate-readiness |
| ISO 27001 / SOC 2 & DPA RGPD self-serve (`CORP-READY`) | 24 (dont F-134 existant) | Gate procurement (D6) | corporate-readiness / marche |
| Audit logs avancés & traçabilité compliance (`AUDIT-LOG`) | 17 | Gate procurement | corporate-readiness |
| API documentée & intégrations SIRH (`API-SIRH`) | 21 (dont F-22 rattaché) | Haute (canal D6) | corporate-readiness / marche |
| Pricing & packaging compte employeur (corporate 800-3000 €/mois) (`PRICING`) | 11 (dont F-33 + F-DRH-API-SIRH-14 socles) | Haute (driver marché D9 — manque #1 comblé) | marche / corporate-readiness |
| Plateforme & moteur réutilisés (pipeline droit du travail) (`platform`) | 38 | Fondation (bloquant) | plateforme-reutilisee |
| Déclaration de périmètre juridiction (V1=FR seul, BE différé) (`SCOPE`) | 5 | Verrou (anti-scope-creep) | vision-po / marche |
| Onboarding & activation de l'acteur EMPLOYEUR (`ONBOARD`) | 8 | Haute (activation/churn) | vision-po / marche |
| Représentation du personnel & conformité CSE (`CSE-CONFORM`) | 15 | Moyenne | droit-travail / concurrent-gap |
| Temps de travail & litiges durée/repos (`TEMPS-TRAVAIL`) | 17 | Haute (trésorerie) | droit-travail / concurrent-gap |

## Doutes résiduels

### Incohérences relevées — run 2026-06-06

1. **PÉRIMÈTRE NET NON MATÉRIALISÉ (récurrent run 2→3)** : ~22 doublons sont documentés en proposedDeletions mais appliedDeletions reste vide PARTOUT (D4 — toute suppression sèche est laissée à la décision PO). Tant que les suppressions ne sont pas appliquées, le compte de features affiché surestime fortement le périmètre réel. Action PO requise : (a) appliquer les doublons PURS dont la référence survivante est confirmée présente (journal de contrôle humain par domaine → AI-ACT-01 ; génération d'actes par domaine → ACTES ; checklist CSE inaptitude → CSE-CONFORM-02 ; calendaire disciplinaire SANCTION-03 → SECU-PROC-03 ; export pré-avocat par domaine → PREAVOCAT-01 ; portefeuille par domaine → DASHBOARD-01) ; (b) trancher les transferts vers MARKETING_BACKLOG (AI-ACT-10, API-SIRH-10) via le contrôle de cohérence marketing 4 points ; (c) trancher les consolidations qui touchent un autre domaine (CHIFFRAGE-08/09/10, PREAVOCAT-04/06, DISCRIM-HARC-04/07/09/12, SSO-03/08, AI-ACT-07/08/14, AUDIT-LOG-10, API-SIRH-08, PLATFORM-05).
2. **5 CAPACITÉS TRANSVERSES UNIQUES non matérialisées comme features de référence explicites** : (1) PLATFORM-04 = pivot lecture employeur de TOUS les F-DT ; (2) capacité CCN-aware = F-DT-07 (répétée 7x : SANCTION-08, REQUAL-CDD-05, DISCRIM-HARC-05, ACTES-08, TEMPS-TRAVAIL-06, CSE-CONFORM-06, API-SIRH-04) ; (3) capacité jurisprudence = F-JU-01 lu via PLATFORM-04 (répétée 6x : INAPT-09, REQUAL-CDD-10, DISCRIM-HARC-11, SANCTION-07, CSE-CONFORM-09, TEMPS-TRAVAIL-11) ; (4) famille audit logs = F-38/AUDIT-LOG-01 (répétée : DASHBOARD-08, AI-ACT-07, CORP-READY-10, API-SIRH-08, SSO-08/09) ; (5) cadre AI-ACT-01 (journal de contrôle humain). Tant que ces 5 capacités ne sont pas posées comme features de référence avec déclinaisons explicites, le pattern de duplication par domaine se reproduira à chaque nouveau domaine.
3. **MOTEUR DE SCORING D'EXPOSITION (CHIFFRAGE-07) — sujet UX NON TRANCHÉ propagé** sur 6 déclinaisons (INAPT-07, REQUAL-CDD-04, DASHBOARD-02, CSE-CONFORM-05, TEMPS-TRAVAIL-05, DISCRIM-HARC-04) + ONBOARD-07 (jalon « 1er outil consulté ») + PRICING-04/API-SIRH-14 (métrage « dossier traité ») : le score/métrage doit-il s'appuyer SEULEMENT sur les outils CALCULÉS/persistés ou aussi sur les champs pré-remplis non cliqués ? 3 options ouvertes (alerte avant génération / pré-calcul auto / laisser tel quel). DÉCISION PO TRANSVERSE REQUISE AVANT TOUT DEV — risque de score/métrage appauvri sans alerte (mémoire project_coherence_conclusions_outils_non_calcules).
4. **MOTEUR D'ARBITRAGE COMMUN contester/transiger instancié sur 6 situations distinctes** (CHIFFRAGE-12, SANCTION-11, REQUAL-CDD-11, INAPT-18, TEMPS-TRAVAIL-16, PREAVOCAT-08/14) : situations métier légitimement distinctes (objet ≠) MAIS doivent partager UN SEUL moteur d'arbitrage (paramètres procédure 2026 : contribution saisine CPH ~50 €, ~13,7 mois, ~67 % appel) sous peine de divergence de calcul entre outils. À matérialiser comme moteur commun + N configurations, pas N moteurs.
5. **GÉNÉRATEUR DE FICHE DE PROVISION IAS 37 (CHIFFRAGE-11) instancié sur 6 déclinaisons** (INAPT-13, SANCTION-12, DISCRIM-HARC-15, REQUAL-CDD-14, TEMPS-TRAVAIL-15, DASHBOARD-09/14) : 1 générateur de référence + N scénarios, à ne PAS multiplier en N moteurs. Cohérent, mais le portage du générateur unique doit être explicitement posé avant le premier dev.
6. **NOTE DE SCOPE JURIDICTION dupliquée** : un domaine SCOPE dédié (SCOPE-01..05) coexiste avec des notes de scope par domaine (ONBOARD-08, CHIFFRAGE-17, REQUAL-CDD-16, SANCTION-12 marqueur, SECU-PROC-15 marqueur). La note canonique doit être SCOPE-01/02 ; les notes par domaine = déclinaisons/renvois, pas des features de scope autonomes. Risque de dates/périmètres divergents si non rattachées à la note canonique (même risque que les dates AI Act incohérentes, cf. AI-ACT-13).
7. **DATES AI ACT INCOHÉRENTES entre features** : 02/08/2026 (CSE-CONFORM-13, SANCTION-15, needs) vs déc. 2027 (CHIFFRAGE-10, AI-ACT-13, glissement Digital Omnibus) vs 2 déc. 2027 (AI-ACT-10). AI-ACT-13 (tracker daté glissant) doit être la SOURCE UNIQUE de la date de référence ; toutes les features citant une échéance AI Act doivent y renvoyer plutôt que coder une date en dur.
8. **TYPOLOGIE FEATURE vs TÂCHE MARKETING** : AI-ACT-10 (badge/comparateur/pitch AI Act-ready) et API-SIRH-10 (argumentaire partenariat SIRH/co-marketing) sont des TÂCHES MARKETING (MARKETING_BACKLOG.md), pas des features produit PRODUCT_SPEC. Maintenues en proposedDeletions du périmètre produit ; à transférer via le contrôle de cohérence marketing 4 points (CLAUDE.md règle 2) — pas une suppression sèche.
9. **STATUTS HÉTÉROGÈNES** : F-22, F-134 sont des features EXISTANTES au statut réel « V9+ — Grands comptes » (PRODUCT_SPEC) tandis que toutes les F-DRH-* sont « Hypothèse » (hors backlog, draft DRH). Cohérent avec le cadre document-vivant, mais à ne PAS confondre au moment d'un éventuel passage au backlog : les F-DRH-* doivent passer par le cycle CLAUDE.md (PRODUCT_SPEC Backlog → étape 0 cohérence → mini-spec…) avant tout dev, et F-22/F-134 conservent leur statut V9+ (post-stabilisation 50 clients, churn <5 %, mémoire enterprise_readiness_v9).
10. **INVARIANT D8 (anti-conflit, « ne pas armer contre le salarié »)** : globalement bien tenu (copy « mon risque »/conformité partout). 2 points de vigilance structurels à valider par smoke test d'isolation workspace AVANT merge : PREAVOCAT-07 (espace de transmission DRH→avocat externe) et PREAVOCAT-10/12/13 (boucle retour avocat + export multi-destinataires + minimisation) franchissent la frontière d'isolation multi-tenant qui FONDE l'invariant (PLATFORM-06) — doivent être des exports bornés/révocables, JAMAIS un accès croisé inter-workspaces.

### Incohérences relevées — run 2026-06-05

1. **CONTRADICTION INTER-DOMAINES (résolue par recadrage, à acter)** : F-DRH-AI-ACT-14 et F-DRH-CORP-READY-15 décrivent le MÊME kit DPO (registre de traitement type Art. 30 + base légale « gestion des contentieux et précontentieux » du référentiel RH CNIL + durées CNIL 02/04/2026). Deux features pour un seul livrable. Recadrage appliqué : CORP-READY-15 = feature de référence du kit DPO ; AI-ACT-14 ne conserve que l'articulation avec l'AIPD. À fusionner avant tout passage au backlog.
2. **FICHE DE PROVISION IAS 37 ÉCLATÉE EN 5 FEATURES** : CHIFFRAGE-11 (unitaire de référence), DASHBOARD-09 (consolidé), INAPT-13, SANCTION-12, DISCRIM-HARC-15. Risque de 5 moteurs pour 1 capacité. Recadrage appliqué : CHIFFRAGE-11 = générateur de référence ; les 4 autres sont des CONFIGURATIONS de scénario (inaptitude, sanction, discrim/harcèlement) ou un niveau d'agrégation (DASHBOARD-09 = consolidé). Invariant '1 outil = 1 situation' = 1 générateur + N déclinaisons, pas 5 moteurs.
3. **PATTERN D'ARBITRAGE CONTESTER/TRANSIGER RÉPLIQUÉ 4x** : CHIFFRAGE-12, SANCTION-11, REQUAL-CDD-11, PREAVOCAT-08. Ce sont des situations métier DISTINCTES (objet du litige ≠ : licenciement, sanction, requalification, volet export) donc légitimes au sens '1 outil = 1 situation', MAIS doivent partager un moteur d'arbitrage commun (aléa × exposition vs coût transaction + paramètres 2026 saisine CPH ~50 €/appel ~67 %) sous peine de divergence de calcul entre outils (cf. mémoire 'outils = simulateurs indépendants, divergence ≠ bug' : ici on veut au contraire une cohérence des paramètres de procédure). À aligner en mini-spec.
4. **PATTERN 'LECTURE EMPLOYEUR DE F-JU-01' RÉPLIQUÉ 6x** (INAPT-09, REQUAL-CDD-10, SANCTION-07, DISCRIM-HARC-11, CSE-CONFORM-09, TEMPS-TRAVAIL-11) + 'CCN-aware' répliqué 7x (F-DT-07 : SANCTION-08, REQUAL-CDD-05, DISCRIM-HARC-05, ACTES-08, API-SIRH-04, TEMPS-TRAVAIL-06, CSE-CONFORM-06) + 'journal de contrôle humain' répliqué 9x (AI-ACT-01) : tous sont des CONFIGURATIONS de capacités transverses (PLATFORM-04 pour la lecture employeur, F-DT-07 pour CCN, AI-ACT-01 pour le journal), PAS des features par domaine. À unifier en capacités plateforme avant build (sinon explosion de features redondantes).
5. **FAMILLE AUDIT LOGS ÉCLATÉE SUR 4 DOMAINES** : domaine AUDIT-LOG entier + CORP-READY-10 + AI-ACT-07 + DASHBOARD-08 + API-SIRH-08 reposent tous sur F-38 (audit_logs existant, /workspace/audit-logs avec recherche+filtre déjà livré). Risque de réimplémentation. AUDIT-LOG-01 = feature de référence ; les autres sont des volets/angles (procurement, AI Act, dashboard, API). DASHBOARD-08 et AUDIT-LOG-10 marqués en suppression. À traiter comme UNE famille extend-F-38.
6. **ISOLATION MULTI-TENANT TRIPLÉE** : PLATFORM-06 (fondation invariant D8), CORP-READY-11 (documentation globale opposable), AUDIT-LOG-08 (cloisonnement des journaux). Articulation posée (PLATFORM-06 = mécanisme fondateur, CORP-READY-11 = preuve documentaire, AUDIT-LOG-08 = sous-cas journaux) mais à NE PAS implémenter en 3 mécanismes distincts. Réutilise le modèle multi-tenant existant (CLAUDE.md : ne pas réinventer le multi-tenant).
7. **RISQUE D8 STRUCTUREL — TRAVERSÉE DE LA FRONTIÈRE D'ISOLATION** : PREAVOCAT-07 (espace transmission DRH↔avocat) et PREAVOCAT-10 (boucle retour avocat) font sortir/rentrer des données du workspace EMPLOYEUR vers un avocat externe. C'est exactement la frontière que PLATFORM-06 / l'invariant anti-conflit D8 protègent. Doivent être conçus STRICTEMENT comme export/import borné (lien/jeton, audit), JAMAIS comme accès croisé inter-workspaces. Smoke test d'isolation workspace obligatoire (préoccupation transversale CLAUDE.md) avant tout merge.
8. **TYPOLOGIE — 2 FEATURES SONT DES TÂCHES MARKETING, PAS DES FEATURES PRODUIT** : AI-ACT-10 (badge/argumentaire AI Act-ready) et API-SIRH-10 (argumentaire partenariat SIRH). Marquées en proposedDeletions. À transférer vers MARKETING_BACKLOG.md via le contrôle de cohérence marketing 4 points (CLAUDE.md règle 2), pas vers PRODUCT_SPEC.
9. **COHÉRENCE D5 (scope = droit social, pénal hors V1)** : REQUAL-CDD-03 (risque pénal CDD L1243-4/L1248) et CSE-CONFORM-11 (délit d'entrave, exposition pénale) touchent le PÉNAL, cadré hors scope DRH (D5, cf. F-PE-01 backlog pénal hors V1). Recadrées en ALERTES D'EXPOSITION (drapeau), PAS en outils de droit pénal complets. À maintenir comme tel en mini-spec pour ne pas déborder du périmètre.
10. **STATUTS** : F-22 et F-134 sont des features EXISTANTES de PRODUCT_SPEC avec statut réel 'V9+ — Grands comptes' (lignes 432 et 478), pas 'Hypothèse'. Préservés ainsi dans les domaines SSO/API-SIRH/CORP-READY. Toutes les F-DRH-* restent 'Hypothèse' (D4 : hors backlog, hors PRODUCT_SPEC live, exclues du sync F-178). RAPPEL : aucune F-DRH-* ne doit être insérée dans PRODUCT_SPEC.md ni dans les tables backlog_* tant que le verrou d'activation du radar corporate (30 K€ MRR OU 2 POC DRH payants OU intro DAF/DJ 200p) n'est pas atteint.
11. **VEILLE/FRAÎCHEUR NORMATIVE DOUBLÉE** : SECU-PROC-15 (mise à jour jurisprudentielle des checklists via F-JU-01) et AI-ACT-13 (veille échéances AI Act/Digital Omnibus) couvrent deux objets différents (jurisprudence sociale appliquée au dossier vs échéances réglementaires AI Act) mais partagent l'angle 'fraîcheur normative datée'. Distinctes mais à articuler pour éviter deux trackers de veille redondants.
12. **RAPPEL GOUVERNANCE** : aucune suppression appliquée (appliedDeletions vide). Toutes les suppressions restent au statut proposedDeletions car le livrable DRH est une HYPOTHÈSE hors backlog (D4) ; la décision d'appliquer revient au PO au moment d'un éventuel passage au backlog. Les proposedDeletions reconduisent les verdicts du draft existant (doublons inter-domaines déjà tracés) sans en introduire de nouveaux non justifiés.

### Incohérences relevées (curation — runs antérieurs, conservées)

- DÉDOUBLONNAGE MASSIF vs catalogue F-DT existant (invariant 1 outil=1 situation + D7 'même moteur lu côté employeur') : ~25 features prétendaient CRÉER un outil décisionnel déjà construit côté avocat. Reclassées plateforme-reutilisee / decisionTool retiré : barème Macron (CHIFFRAGE-01→F-DT-01/09), nullité (CHIFFRAGE-02/INAPT-06/DISCRIM-HARC-01→F-DT-16/11/12/30), indemnités rupture (CHIFFRAGE-03→F-DT-01/07/15/25/26), transaction (CHIFFRAGE-04/SECU-PROC-09→F-DT-31), réintégration (CHIFFRAGE-06→F-DT-16/30), clause non-concurrence (SECU-PROC-10→F-DT-24), chiffrage inaptitude (INAPT-05→F-DT-15), requalification CDD/intérim (REQUAL-CDD-01/02→F-DT-22/23/17), discrimination/harcèlement (DISCRIM-HARC-03→F-DT-11/12/16), heures sup (TEMPS-TRAVAIL-03→F-DT-19/20).
- INCOHÉRENCES FACTUELLES de cadrage : plusieurs descriptions affirmaient un 'créneau VIDE / point aveugle / aucun outil' alors que l'outil existe déjà côté avocat — REQUAL-CDD-01 ('créneau dossier-centric VIDE' alors que F-DT-22 existe), CHIFFRAGE-03 ('CCN = point de défaillance n°1 non outillé' alors que F-DT-07 CCN-aware existe), DISCRIM-HARC-03 ('point aveugle des calculateurs' alors que F-DT-11/16 le font), TEMPS-TRAVAIL-03 ('aucun outil employeur' alors que F-DT-19/20 existent). Corrigées : le différenciant reste valable (lecture dossier-centric côté employeur), mais l'outil de calcul est réutilisé, pas recréé.
- DUPLICATION TRANSVERSE répétée dans 6-9 domaines, à factoriser en 1 capacité transverse chacune : (a) Journal contrôle humain / AI Act = 9 occurrences (CHIFFRAGE-10, SECU-PROC-11, INAPT-10, SANCTION-09, PREAVOCAT-06, ACTES-09, REQUAL-CDD-09, DISCRIM-HARC-10, CSE-CONFORM-10) → tenir par AI-ACT-01/02. (b) CCN-aware = 7 occurrences (SANCTION-08, ACTES-08, REQUAL-CDD-05, DISCRIM-HARC-05, CSE-CONFORM-06, TEMPS-TRAVAIL-06, + API-SIRH-04 amont) → F-DT-07 existant + 1 capacité transverse. (c) Réutilisation moteur jurisprudence = 6 occurrences (INAPT-09, SANCTION-07, REQUAL-CDD-10, DISCRIM-HARC-11, CSE-CONFORM-09, TEMPS-TRAVAIL-11) → F-JU-01 lu via PLATFORM-04. (d) Note pré-avocat = 4 occurrences (REQUAL-CDD-07, DISCRIM-HARC-12, CSE-CONFORM-07, TEMPS-TRAVAIL-09) → PREAVOCAT-01. (e) Vue portefeuille = 5 occurrences (CHIFFRAGE-08, REQUAL-CDD-08, DISCRIM-HARC-09, CSE-CONFORM-08, TEMPS-TRAVAIL-10) → DASHBOARD-01. (f) Scoring d'exposition = 5 occurrences (INAPT-07, REQUAL-CDD-04, DISCRIM-HARC-04, CSE-CONFORM-05, TEMPS-TRAVAIL-05) → CHIFFRAGE-07 décliné par situation.
- DUPLICATION INTER-DOMAINES de génération d'actes : SECU-PROC-05/06/07/08/09 dupliquent ACTES-01/03/04/05/07 ; INAPT-08, SANCTION-06, DISCRIM-HARC-08, TEMPS-TRAVAIL-08 dupliquent le pattern ACTES. Toute la génération doit vivre dans le domaine ACTES (réutilisant F-98 + F-DT-04), déclinée par situation. 8 suppressions proposées.
- DUPLICATION corporate-readiness/audit/SSO : famille audit logs éclatée sur 4 domaines (AUDIT-LOG-01..10, CORP-READY-10, AI-ACT-07, API-SIRH-08, DASHBOARD-08) — toutes au-dessus de F-38 (audit_logs existant, qui a DÉJÀ recherche+filtre, donc AUDIT-LOG-04 = simple extension RGPD). Kit procurement dupliqué (AI-ACT-08, SSO-08, AUDIT-LOG-10 = volets de CORP-READY-05/14). SSO+MFA dupliqué (SSO-03, CORP-READY-09, F-22). API documentée dupliquée (API-SIRH-01, CORP-READY-13).
- STATUTS FAUX : toutes les features EXISTANTES (F-01 à F-52, F-22, F-134) étaient marquées 'Hypothèse' alors que les features plateforme sont 'Terminée' (PRODUCT_SPEC) et F-22/F-134 sont 'V9+ — Grands comptes'. Statuts rétablis. Risque : marquer une feature livrée comme Hypothèse fausserait le backlog DB (sync F-178).
- RÉÉCRITURE de scope F-22 non signalée comme variante : les domaines SSO et API-SIRH réécrivent le titre F-22 ('Azure AD/Google Workspace/SAML' → 'Entra ID/Okta/SCIM'). CLAUDE.md interdit de remplacer silencieusement une décision existante. Titre/scope F-22 préservés ; les IdP Entra/Okta restent des découpages indicatifs (sous-features), pas une réécriture du parent.
- TYPOLOGIE produit vs marketing : F-DRH-AI-ACT-10 (badge/argumentaire/pitch 'AI Act-ready') et F-DRH-API-SIRH-10 (argumentaire partenariat SIRH/co-marketing) sont des TÂCHES MARKETING, pas des features PRODUCT_SPEC. À transférer vers MARKETING_BACKLOG via le contrôle de cohérence marketing 4 points (CLAUDE.md règle 2). 2 suppressions proposées du périmètre produit.
- GAPS RÉELS confirmés (à conserver, situations métier nouvelles non couvertes par F-DT) : régime social/fiscal des indemnités 2 PASS (CHIFFRAGE-05) ; contrôle de proportionnalité de la sanction (SANCTION-02) ; qualification de la faute disciplinaire (SANCTION-01) ; chiffrage nullité d'une sanction (SANCTION-05) ; garde-fou calendaire disciplinaire (SECU-PROC-03) et CSE (CSE-CONFORM-04) ; validité forfait-jours + repos (TEMPS-TRAVAIL-01/02) + qualification durée (04) ; comparateur de panel discrimination (DISCRIM-HARC-02) ; enquête harcèlement interne (DISCRIM-HARC-07) ; index égalité F/H Rixain (DISCRIM-HARC-06) ; scoring/orchestration d'exposition (CHIFFRAGE-07) ; mode pré-avocat (PREAVOCAT-01/02/03/05) ; portefeuille/reporting/ROI (DASHBOARD-01/02/05/06/07) ; type d'acteur + lecture employeur (PLATFORM-01/04) ; intégrations SIRH (API-SIRH-02/04/05/06/07). RECOMMANDATION : SANCTION-01/02/05 et CHIFFRAGE-05 sont des outils utiles AUSSI à l'avocat → les cadrer comme outils plateforme (F-DT-39+), pas comme forks employeur, pour préserver 1 outil=1 situation à l'échelle produit.
- RISQUE D8 spécifique : F-DRH-PREAVOCAT-07 (espace de transmission DRH↔avocat) traverse la frontière d'isolation multi-tenant qui FONDE la neutralisation du risque anti-conflit (cadrage D8 : 'isolation logique stricte des workspaces'). À concevoir comme export borné/révocable, jamais comme accès croisé inter-workspaces, sinon l'argument déontologique D8 s'effondre.
- COHÉRENCE invariant interne 'conclusions ⊂ outils calculés' (memory project_coherence_conclusions_outils_non_calcules, sujet UX NON TRANCHÉ) : tous les scorings d'exposition (CHIFFRAGE-07 et déclinaisons) doivent s'appuyer UNIQUEMENT sur les outils CALCULÉS/persistés, pas sur les champs pré-remplis non cliqués — sinon score appauvri silencieux. À trancher avant dev (alerte avant génération / pré-calcul auto / laisser tel quel).
- COHÉRENCE F-IA-03 + modèle 'outils = simulateurs indépendants' (memory feedback_decision_tools_are_simulators) : F-DRH-AI-ACT-06 (override obligatoire) ne doit pas introduire d'override forcé ENTRE simulateurs (divergence inter-outils ≠ bug, pas d'override). L'override AI Act porte sur la décision humaine vs output IA, pas sur la réconciliation des simulateurs.
- COHÉRENCE pénal hors V1 : F-DRH-REQUAL-CDD-03 (risque pénal CDD L1243-4) touche le pénal, cadré hors V1 (F-PE-01, backlog, ≥5 signaux requis). À garder au niveau ALERTE d'exposition, ne pas en faire un outil de droit pénal (sinon empiète sur F-PE-01 non démarré).

**Risque déontologique D8 à valider terrain** : le pont employeur → avocat (F-DRH-PREAVOCAT-07) et plus généralement le double usage des données ne doivent jamais affaiblir l'isolation multi-tenant qui FONDE la neutralisation du risque anti-conflit. À valider en conditions réelles.

### Sources marché

- https://www.carrieres-juridiques.com/actualites-et-conseils-emploi-juridique/la-fabrique-juridique-la-legaltech-dediee-au-droit-social/1554
- https://www.lexisnexis.com/fr-fr/produits/case-law-analytics/indemnite-licenciement
- https://www.data.gouv.fr/reuses/predictice-organise-toute-linformation-juridique-pour-les-professionnels-du-droit
- https://blog.predictice.com/actualites-juridiques/calcul-indemnite-rupture-conventionnelle
- https://www.lefebvre-dalloz.fr/genia-l/
- https://www.editions-legislatives.fr/genial
- https://www.lefebvre-dalloz.fr/ressources/genia-l-un-allie-incontournable-des-professionnels-du-droit-social/
- https://www.editions-tissot.fr/
- https://www.editions-tissot.fr/actualite/droit-du-travail/lia-act-les-nouvelles-obligations-pour-les-entreprises-en-matiere-dintelligence-artificielle
- https://payfit.com/fr/fiches-pratiques/logiciel-sirh/
- https://www.cegid.com/fr/blog/outils-sirh/
- https://tensoria.fr/blog/prompts-ia-droit-du-travail-avocat-rh
- https://neo-campus.org/droit-social-obligations-rh/contentieux-prud-homal-rh/
- https://www.pios-avocats.com/details-contentieux+prud+homal+comment+s+y+preparer+et+limiter+les+risques+pour+l+employeur-28
- https://startlaw.fr/direction-juridique-externalisee/
- https://www.troispointquatorze.fr/nos-domaines/direction-juridique-externalisee/
- https://www.adevweb.com/ressources/ai-act-entreprise-2026
- https://dpo101.fr/ai-act-le-calendrier-2026-pour-les-pme-et-eti-ce-qui-change-vraiment/
- https://audaria.fr/ai-act/conformite-rh
- https://www.entreprises.cci-paris-idf.fr/actualites/2026-2027-vers-un-recrutement-plus-sur-mais-plus-exigeant-base-sur-lai-act
- https://www.entreprises.gouv.fr/secteurs-dactivite/le-secteur-des-services-marchands-en-france/france-legaltech-accompagner
- https://www.service-public.gouv.fr/simulateur/calcul/bareme-indemnites-prudhomales
- https://www.swim.legal/blog/honoraires-avocat-droit-du-travail-budget-dossier-social-entreprise
- https://www.swim.legal/blog/indemnite-licenciement-abusif-bareme-macron-calcul-risques-employeur
- https://www.simonnetavocat.fr/jugement-cph-ou-transaction-quel-traitement-fiscal-et-social-des-indemnites-de-rupture/
- https://boss.gouv.fr/portail/accueil/exonerations/indemnites-de-rupture.html
- https://www.eurecia.com/blog/indemnites-prudhomales-bareme-licenciements/
- https://www.lja.fr/fiches-pratiques/direction-juridique/construire-et-piloter-le-budget-de-la-direction-juridique-directeurs-juridiques-arretez-de-subir-lexercice-budgetaire-527588.php
- https://www.village-justice.com/articles/maturite-budgetaire-des-directions-juridiques-enjeu-structurant-pour-les-legal,55490.html
- https://www.epiqglobal.com/fr-fr/resource-center/articles/how-legal-operations-can-get-ahead-of-the-budget-p
- https://www.blog-rh.com/2026/03/le-piege-du-licenciement-pour-inaptitude/
- https://www.pointblog.com/piege-licenciement-inaptitude/
- https://www.editions-tissot.fr/actualite/sante-securite/licenciement-pour-inaptitude-dun-salarie-protege-les-motifs-controles-par-ladministration-simposent-au-juge-judiciaire
- https://www.unsa.org/Inaptitude-et-licenciement-ce-qu-il-faut-retenir-de-la-jurisprudence-de-l.html
- https://dynexio.com/blog/ai-act-guide-conformite-entreprise-2026/
- https://naaia.ai/ia-act-liste-ia-a-haut-risque/
- https://www.managementqualite.com/news/ai-act-les-obligations-ia-a-haut-risque-repoussees-a-decembre-2027/
- https://payfit.com/fr/fiches-pratiques/deroulement-entretien-prealable-sanction-disciplinaire/
- https://www.fr.adp.com/rhinfo/articles/2021/01/les-delais-a-respecter-dans-le-cadre-dune-procedure-disciplinaire.aspx
- https://www.swim.legal/blog/modele-convocation-entretien-prealable-sanction
- https://www.plateya.fr/blog/detail/cout-dun-drh-externalise-missions-conseils-efficacite
- https://payfit.com/fr/fiches-pratiques/sirh/
- https://www.lucca.fr/magazine/administration/paie/sirh-gestion-paie
- https://www.svp.com/actualite/top-9-logiciels-sirh-2025
- https://zylo.com/blog/saas-compliance-checklist
- https://secureprivacy.ai/blog/data-processing-agreements-dpas-for-saas
- https://www.konfirmity.com/blog/soc-2-for-saas
- https://ssojet.com/blog/sso-compliance-requirements-compared-soc-2-iso-27001-hipaa-pci-dss-and-gdpr
- https://securityboulevard.com/2026/04/11-sso-compliance-requirements-compared-soc-2-iso-27001-hipaa-pci-dss-and-gdpr/
- https://www.leto.legal/guides/comparatif-logiciel-rgpd
- https://www.leto.legal/guides/rgpd-et-saas
- https://donnees.net/avis-dastra-logiciel
- https://www.arcade.software/post/enterprise-sales-cycle
- https://ziellab.com/post/b2b-sales-cycle-length-shorten-2026-guide
- https://www.cyberbase.ai/blog/enterprise-saas-deal-acceleration
- https://www.growthspreeofficial.com/blogs/b2b-saas-sales-cycle-length-benchmarks-2026-by-acv-vertical
- https://www.knowlee.ai/blog/ai-act-annex-iii-hr-employment
- https://hr-on.com/eu-ai-act-for-hr-2026/
- https://bm.consulting/en/insights/ai-act-high-risk-system-obligations/
- https://artificialintelligenceact.eu/what-the-act-means-for-staffing-businesses/
- https://www.upguard.com/blog/top-vendor-assessment-questionnaires
- https://www.bitsight.com/blog/caiq-vs-sig-top-questionnaires-vendor-risk-assessment
- https://safebase.io/resources/security-questionnaires
- https://blog.legaltechmg.com/why-saas-doesnt-always-work-in-legal
- https://getpulsesignal.com/for/law-firms
- https://www.legal-suite.septeo.com/en

**Sources ajoutées run 2026-06-05 :**

- https://enclair.media/articles/provision-risques-prudhomaux-calcul
- https://neo-campus.org/droit-social-obligations-rh/contentieux-prud-homal-rh/
- https://www.juriguide.com/2026/05/16/actualite-jurisprudence-sociale-prudhommes/
- https://www.jobexit.fr/simuler-rupture-contrat-de-travail
- https://www.jobexit.fr/indemnites-en-cas-de-contentieux/indemnite-licenciement-sans-cause-reelle-et-serieuse
- https://blog.caselawanalytics.com/integrer-la-jurimetrie-a-sa-pratique-professionnelle-en-droit-social/
- https://www.editions-tissot.fr/produit/modeles-commentes-pour-la-gestion-du-personnel/
- https://www.editions-tissot.fr/actualite/droit-du-travail/lia-act-les-nouvelles-obligations-pour-les-entreprises-en-matiere-dintelligence-artificielle
- https://boutique.lefebvre-dalloz.fr/solution-rh.html
- https://aoriarh.fr/
- https://askrh.fr/
- https://blog.laboris.fr/posts/assistant-ia-droit-social-gagner-10-heures-par-semaine/
- https://www.legisocial.fr/contrat-de-travail/recrutement/mettre-conformite-ia-act.html
- https://www.extencia.fr/anticiper-risque-prudhomal-audit-social-rh
- https://www.apogea.fr/audit-rh/
- https://www.svp.com/offres/accompagnement-reglementaire-et-juridique/audit-social
- https://www.plateya.fr/blog/detail/cout-dun-drh-externalise-missions-conseils-efficacite
- https://www.cabinet-avocats-langlet.fr/externaliser-vos-rh-les-5-benefices-pour-les-pme-de-10-a-250-salaries/
- https://www.assistant-juridique.fr/cout_saisine_prudhommes.jsp
- https://www.wolterskluwer.com/fr-fr/expert-insights/outsourcing-strategies-for-legal-teams-in-2025
- https://www.swim.legal/blog/indemnite-licenciement-abusif-bareme-macron-calcul-risques-employeur
- https://www.eurecia.com/blog/indemnites-prudhomales-bareme-licenciements/
- https://www.simplidroit.fr/affaires-et-fiscalite/legal-ops-performance-juridique/
- https://www.fox-group.org/post/legaltech-2025-les-15-outils-indispensables-pour-une-direction-juridique-moderne
- https://www.unsa.org/Inaptitude-et-licenciement-ce-qu-il-faut-retenir-de-la-jurisprudence-de-l.html
- https://www.rozenblit.fr/2026/03/27/piege-licenciement-inaptitude/
- https://www.fidal.com/en/node/18341
- https://www.capstan.fr/articles/2324-recours-contre-lavis-dinaptitude-reclasser-ou-licencier-malgre-tout/
- https://solutionscse.edenred.fr/actus/ai-act-ce-que-les-cse-doivent-verifier-en-2026
- https://audaria.fr/ai-act/conformite-rh
- https://www.leto.legal/guides/ai-act-conformite
- https://www.victorisavocat.com/en/blog/provision-pour-litiges-definition-comptabilisation-et-conseils-pratiques-pour-les-dirigeants-de-pme
- https://www.swapn.fr/simulateurs/simulateur-indemnite-rupture-conventionnelle
- https://www.agiris.fr/logiciel/previsionnel-ifc
- https://www.concerto-rh.fr/simulateur-licenciement-et-rupture-conventionnelle/
- https://zylo.com/blog/saas-compliance-checklist
- https://secureprivacy.ai/blog/data-processing-agreements-dpas-for-saas
- https://nflo.tech/knowledge-base/saas-security-audit-enterprise-client-requirements-preparation/
- https://www.atlassystems.com/blog/soc-2-vendor-management
- https://aexus.com/how-long-is-the-average-b2b-software-sales-cycle/
- https://blog.legaltechmg.com/18-month-legal-tech-sales-cycle
- https://www.default.com/post/enterprise-saas-sales
- https://www.cnil.fr/sites/cnil/files/atoms/files/cnil-gdpr_practical_guide_data-protection-officers.pdf
- https://www.dlapiperdataprotection.com/index.html?t=data-protection-officers&c=FR
- https://artificialintelligenceact.eu/annex/3/
- https://artificialintelligenceact.eu/article/27/
- https://secureprivacy.ai/blog/fria-fundamental-rights-impact-assessment-ai
- https://www.aoshearman.com/en/insights/ao-shearman-on-tech/zooming-in-on-ai-10-eu-ai-act-what-are-the-obligations-for-high-risk-ai-systems
- https://euaicompass.com/eu-ai-act-high-risk-deployer-guide.html
- https://bm.consulting/en/insights/ai-act-high-risk-system-obligations/
- https://www.leto.legal/guides/comment-realiser-son-aipd-en-5-etapes
- https://www.cnil.fr/fr/le-referentiel-relatif-la-gestion-des-ressources-humaines-en-questions
- https://www.cnil.fr/fr/referentiel-durees-conservation-donnees-rh
- https://www.andrh.fr/article/L-analyse-d-impact-relative-a-la-protection-des-donnees-pour-les-RH-Memo-ANDRH
- https://peoplemanagingpeople.com/employee-retention/hr-compliance-software-price/
- https://pricingnow.com/question/workplace-compliance-system-pricing/
- https://improvado.io/blog/saas-pricing-models-outcome-based
- https://www.thoropass.com/blog/soc-2-vs-iso-27001
- https://secureframe.com/blog/soc-2-vs-iso-27001
- https://sprinto.com/blog/why-soc-2-for-saas-companies/

---

## Changelog (append-only)

- **2026-06-06 — run de maturation** — overall 85 → **90** (Δ 5), verdict `continue`. APPEND de **75 features** ce run (cumul). Nouveaux domaines : `SCOPE` (01→05, note de scope juridiction canonique V1=FR / BE backlog différé) et `ONBOARD` (01→08, sélection du type d'acteur D7, assistant premier dossier centré-salarié = feature de référence activation, kit de démarrage, rôles RH, friction d'activation, métrage time-to-value, note de scope parcours). Features ajoutées : PLATFORM-10→12 (parcours d'activation ⊂ ONBOARD-02, substrat métrage usage, conformité pipeline RGPD Art. 28/32), CHIFFRAGE-15→17 (réévaluation provision IAS 37 à chaque clôture, différé ARE/France Travail, note de scope chiffrage), SECU-PROC-17→18, INAPT-15→18 (recours avis inaptitude, compteur reprise salaire 1 mois, qualification origine pro AT/MP, arbitrage inaptitude), SANCTION-13→15, DASHBOARD-13→14 (calendrier critique consolidé, export comptable liasse provisions), PREAVOCAT-11→14 (liasse procédurale, export multi-vues, minimisation RGPD, préparation BCO), ACTES-15→16 (autres courriers RH à effet juridique, notification & preuve de remise), REQUAL-CDD-14→16, DISCRIM-HARC-16→17 (directive UE 2023/970 transparence des rémunérations, test anti-discrimination de l'acte), CSE-CONFORM-13→15 (consultation CSE-IA, gate salarié protégé, alertes & trames pré-remplies), TEMPS-TRAVAIL-15→17, AI-ACT-15→17 (marquage CE/déclaration UE/base UE, incidents graves Art. 73, logging-by-design Art. 12), SSO-13→15, CORP-READY-19→23, AUDIT-LOG-15→17 (change management, notification de violation, certificat de purge), API-SIRH-17→20 (SCIM 2.0, status page/SLA, versioning API, SDKs). Modifications/recadrages anti-doublon : PLATFORM-10⊂ONBOARD-02, ONBOARD-08/CHIFFRAGE-17/REQUAL-16⊂SCOPE-01, API-SIRH-17⊂SSO-06 (SCIM), API-SIRH-18⊂CORP-READY-17 (status page), AUDIT-LOG-16↔CORP-READY-21, AUDIT-LOG-17↔PLATFORM-12 ; CHIFFRAGE-07 confirmé moteur de scoring de référence + sujet UX non tranché propagé (INAPT-07, REQUAL-04, DASHBOARD-02, CSE-CONFORM-05, ONBOARD-07, PRICING-04, API-SIRH-14) ; AI-ACT-01 confirmé cadre transverse du journal de contrôle humain ; moteur d'arbitrage commun étendu à INAPT-18/TEMPS-TRAVAIL-16/PREAVOCAT-14 ; générateur de fiche IAS 37 (CHIFFRAGE-11) + cycle de vie CHIFFRAGE-15. Suppressions proposées reconduites/ajoutées (≈30 doublons, appliedDeletions vide — D4, décision PO). 2 tâches marketing (AI-ACT-10, API-SIRH-10) toujours à transférer vers MARKETING_BACKLOG. Statuts F-22/F-134 = V9+ préservés ; toutes F-DRH-* = Hypothèse hors backlog (D4). Directive PO ce run : RUN 3 — viser le seuil d'excellence 90, combler les 3 trous de contenu run 2 (pricing/onboarding/scope BE) ET tenir le dédoublonnage ; marquer explicitement le sujet UX non tranché « scoring/conclusions ⊂ outils CALCULÉS/persistés » ; statuts réels (plateforme=Terminée, F-22/F-134=V9+) ; prioriser les 8 must-have §7 + créneau dossier-centric employeur VIDE.
- **2026-06-05 — run de maturation** — overall 82 → **85** (Δ 3), verdict `continue`. APPEND de **64 features** : PLATFORM-06→09 (isolation multi-buyer, framing/packaging, traçabilité « factualisé depuis les pièces », réutilisation pipeline 3-niveaux), CHIFFRAGE-11→14 (fiche provision IAS 37 générateur de référence, arbitrage contester/transiger, restitution multi-assiette, traçabilité chiffrage), SECU-PROC-12→16, INAPT-11→14, SANCTION-10→12, DASHBOARD-09→12, PREAVOCAT-08→10, ACTES-13 (recadrage), REQUAL-CDD-13 (recadrage), DISCRIM-HARC-13→15, CSE-CONFORM-11→12 (+ CONFORM-04 enrichie plancher préfix), TEMPS-TRAVAIL-12→14, AI-ACT-11→14 (+ AI-ACT-02 FRIA conditionnelle), SSO-09→12, CORP-READY-15→18 (+ CORP-READY-11 articulée PLATFORM-06), AUDIT-LOG-11→14 (+ AUDIT-LOG-08 articulée), API-SIRH-11→16 (+ API-SIRH-05 ENTRANT). Modifications/recadrages sur info marché ou anti-doublon inter-domaines (AI-ACT-14 ↔ CORP-READY-15 = même kit DPO, consolidé sous CORP-READY-15 ; fiche provision IAS 37 = 1 générateur CHIFFRAGE-11 + N déclinaisons ; arbitrage contester/transiger = 1 moteur commun, 4 situations). Suppressions proposées reconduites (≈20 doublons, appliedDeletions vide — D4, décision PO). 2 tâches marketing identifiées (AI-ACT-10, API-SIRH-10) à transférer vers MARKETING_BACKLOG. Statuts F-22/F-134 = V9+ préservés ; toutes F-DRH-* = Hypothèse hors backlog (D4). Directive PO ce run : aucune.
- **2026-06-05** — Création du domaine `REQUAL-CDD` (10 features, status Hypothèse). Driver : veille concurrentielle
  2026-06-04 (trou « dossier-centric employeur » VIDE) + cadrage §7 capacité #7. Provenance détaillée par feature.
- **2026-06-05** — Création du domaine `AI-ACT` (Conformité Règlement IA Annexe III, 10 features, status Hypothèse).
  Driver : veille concurrentielle 2026-06 + D10 (gate dur procurement grand compte). MAJ calendrier : application
  haut-risque Annexe III glissée au **2 déc. 2027** (Digital Omnibus 07/05/2026) → conformité = différenciant de
  confiance plutôt qu'urgence août 2026. Ancrage = 6 obligations déployeur (journal contrôle humain, AIPD/FRIA,
  info salariés, alerte CSE, gouvernance biais, override) + 4 features marché/procurement/différenciation. Aucune
  feature existante de ce domaine (IDs `[]`) → tout en APPEND. Aucune suppression. Provenance détaillée par feature.
- **2026-06-05** — Création du domaine `CORP-READY` (ISO 27001 / SOC 2 & DPA RGPD self-serve, 15 features dont F-134
  préservée comme ombrelle, status Hypothèse). Driver : veille concurrentielle 2026-06 — ticket d'entreprise 2026
  (63 % des acheteurs exigent un questionnaire de sécurité ; SOC 2 Type II **ou** ISO 27001 = gate dur DPO/Achats sans
  lequel l'achat est bloqué) + D10 (corporate-readiness = features produit). F-134 conservée comme feature ombrelle du
  bundle (provenance corporate-readiness, IDs préservés `[F-134]`). APPEND de 14 features fermant les trous procurement :
  SOC 2 Type II, ISO 27001, DPA self-serve Art. 28, liste sous-traitants, Trust Center, CAIQ/SIG/VSA pré-remplis, preuves
  de contrôles (pen-test/logs), SSO/OIDC+MFA, audit logs, isolation multi-tenant documentée, hébergement UE/RGPD, API,
  accélérateur de cycle de vente. Levier différenciant : surclasser les éditeurs droit social (Tissot/Lamy/Lefebvre)
  faiblement outillés procurement, égaler les legal-ops (Legisway/Septeo). Aucune suppression. Provenance par feature.
- **2026-06-05** — Création du domaine `API-SIRH` (API documentée & intégrations SIRH, 11 features dont **F-22 préservée**
  comme ancre plateforme, status Hypothèse). Driver : veille concurrentielle 2026-06 — les **SIRH (PayFit/Lucca/Cegid/
  Silae) détiennent déjà le DRH et la donnée amont** (même acheteur D6) mais sont **aveugles au contentieux** → bascule
  D7 « nice-to-have » vers **canal de distribution** : s'intégrer plutôt qu'affronter, LegalCase = brique contentieux/
  risque manquante. F-22 (SSO/OIDC entreprise, `PRODUCT_SPEC.md` l.432) **rattachée sans réécriture** comme socle auth &
  provisioning des connecteurs SIRH (changeReason = recontextualisation domaine, pas de modif de scope). APPEND de 10
  features : API REST OpenAPI 3.0 auditée, connexion OAuth 2.0 SIRH, **pré-remplissage du contexte employé** (nom/
  ancienneté/salaire/rôle/affectation), récupération **CCN amont → moteur CCN-aware**, webhooks données amont, mapping/
  réconciliation employé↔dossier, connecteurs packagés sans dev, journal d'audit accès API (double usage AI Act+SOC 2),
  garde-fou D8 sur données importées (conformité/chiffrage, jamais profilage anti-salarié), argumentaire partenariat
  SIRH. **Invariant D3 préservé** : seul le contexte employé est auto-peuplé, l'**upload des pièces du dossier litigieux
  reste manuel**. Recouvrement assumé/explicité avec `CORP-READY` (F-DRH-CORP-READY-09 SSO+MFA, -13 API) : angle ici =
  intégration/pré-remplissage, angle là-bas = questionnaire sécurité/Trust Center. Aucune suppression. Provenance par
  feature.
