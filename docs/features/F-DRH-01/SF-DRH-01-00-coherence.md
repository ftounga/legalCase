# F-DRH-01 — Page publique « LegalCase Employeur » — Cadrage cohérence (étape 0)

> Skill `feature-coherence-challenger`. Date 2026-06-08.
> Contexte : pivot testé vers le segment DRH/employeur (cf. `memory/project_traction_baseline_2026_06_08` — 0 client payant sur le segment avocat). Décision PO : activer une **couche légère** (page de présentation + démos), garder le lourd corporate pour après 2 POC.

## Verdict : **GO avec ajustements**

La page s'appuie sur une capacité **réelle et livrée** (le moteur de calcul de risque : validité licenciement, indemnités Macron, vices de procédure F-DT-36, délais — neutres et valables côté employeur). Pas de gadget **à condition** de ne présenter que ce que le moteur fait vraiment et d'orienter vers une démo/RDV, pas vers un produit DRH-natif qui n'existe pas encore.

## Intention métier (1 phrase)
Donner un point d'entrée crédible (page + lien partageable) pour amorcer les conversations DRH et matérialiser l'offre « chiffrer l'exposition prud'homale d'un licenciement, avant de décider » — **support du discovery, pas produit fini**.

## Workflow métier réel de la cible (DRH 200+)
Source : `docs/drh/CADRAGE-STRATEGIQUE-DRH.md` (fiche 90/100).
1. Le DRH envisage une décision RH risquée (licenciement, sanction, rupture).
2. Il cherche à évaluer le **risque** et à **sécuriser la procédure** avant d'agir.
3. Aujourd'hui : avocat externe (coûteux, lent) ou « au jugé ».
4. Il découvre une solution (bouche-à-oreille, réseau, recherche).
5. Il veut comprendre vite la valeur, puis **être rassuré** (conformité, neutralité) avant d'engager une démo.
6. Il sollicite un **contact / une démo** (achat B2B, pas self-serve).

## Cartographie capacités ↔ besoin

| Besoin DRH | Capacité produit | Statut |
|---|---|---|
| Chiffrer l'exposition (indemnités) | Comparateur indemnités / barème Macron | ✅ livré (neutre) |
| Détecter les vices de procédure | F-DT-36 nullité procédure | ✅ livré |
| Vérifier validité / délais | F-DT-08, délais | ✅ livré |
| Vue portefeuille, comparaison d'options, note de décision | écrans DRH-natifs | ❌ **à concevoir après discovery** |
| Conformité corporate (SSO/ISO/Règlement IA) | F-22/F-134 | ❌ reporté post-POC |

## Challenge amont
La page suppose seulement que le **moteur de calcul existe** (✅) et qu'on peut **prendre un RDV** (✅ Calendly/contact). Aucun trou amont bloquant.

## Challenge aval
Sortie de la page = **un RDV de démo** (pas une inscription self-serve, car le produit DRH-natif n'est pas prêt). Aval = le discovery (guide d'entretien). Cohérent.
**Risque inverse à éviter** : que la page laisse croire à un produit DRH complet (portefeuille, etc.) → déception en démo. D'où les invariants ci-dessous.

## STOPs / pré-requis
Aucun STOP. La page est une couche de présentation, pas un engagement produit.

## Invariants anti-gadget pour la mini-spec
1. **Ne présenter que le réel** : chiffrage d'exposition, vices de procédure, délais, validité — ce que le moteur fait *aujourd'hui*. Pas de capture d'écran d'écrans DRH inexistants.
2. **CTA = « réserver une démo » / « être recontacté »**, jamais inscription self-serve (le produit DRH-natif n'existe pas).
3. **Pas de prix affiché** (négocié en POC).
4. **Messaging « maîtrise du risque & conformité », jamais « gagner contre vos salariés »** (invariant D8 du cadrage — risque perceptuel vis-à-vis du segment avocat).
5. **Marque « LegalCase Employeur »** comme sous-gamme, pas produit séparé.

## Décision finale
**GO avec ajustements.** Page de présentation + CTA démo. Passage à l'étape 0 bis (cohérence écran) requis (nouvelle page publique). Feature à inscrire au `PRODUCT_SPEC.md` (F-DRH-01) en notant que le **gate DRH est partiellement levé par décision PO** pour la seule couche d'amorce (page + discovery), le produit DRH-natif restant gaté à 2 POC payants.
