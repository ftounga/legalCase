# M-79 — Kit d'activation : acquisition avocat 3 domaines (stack SaaS)

Statut : **activée 2026-06-09 par override PO** (cf. MARKETING_BACKLOG.md M-79).
Stack : Lemlist/Smartlead + Sales Navigator + Hunter + Claude API + Gmail (warm-up).
Cible : **cabinets d'avocats multi-avocats**, France, 3 domaines produits.

---

## A. Actions FRANCK (souscriptions — non automatisables côté Claude)
1. **Lemlist Multichannel Pro** ~99 €/mois (ou Smartlead Advanced ~64 €). Warm-up Gmail auto.
2. **Hunter.io Starter** ~49 € (enrichissement emails pro).
3. **Sales Navigator** — essai gratuit 30 j (alarme J27 pour résilier si non conservé).
4. **Connecter** Lemlist à : (a) compte LinkedIn perso (limite 30-40 connect/j), (b) Gmail (warm-up).
5. Fournir la **CB** / valider les abonnements.
> Coût mois 1 ≈ 250 € (cf. M-79). Récurrent ≈ 200 €/mois.

## B. Ce que je prépare / génère (côté Claude — prêt ou à lancer dès listes dispo)
- Les requêtes de ciblage (ci-dessous).
- La qualification + dédoublonnage des exports.
- La génération **batch** des intros personnalisées (Claude API) — 1 fait vérifié/contact, par domaine.
- Les séquences (copy connect + message + relances) par domaine.
- Le tableau de suivi + analyse par domaine.

---

## C. Requêtes Sales Navigator (une par domaine — décliner par région si volume trop gros)
Filtres communs : **Geography** = France · **Industry** = Legal Services / Law Practice ·
**Company headcount** = 11-50 et 51-200 · **Current job title** = `Avocat` OR `Avocat associé` OR `Associé`
(décideurs — exclure « collaborateur »).

1. **Droit du travail** — Keywords profil : `"droit du travail" OR "droit social"`
2. **Immigration** — Keywords profil : `"droit des étrangers" OR "droit de l'immigration" OR "droit de la nationalité"`
3. **Droit de la famille** — Keywords profil : `"droit de la famille" OR "divorce" OR "droit patrimonial"`

Export via Evaboot (~50 $) → CSV → je qualifie/dédoublonne/enrichis (Hunter).

## D. Angle de valeur par domaine (base des intros + séquences)
- **Travail** : « à partir des pièces, LegalCase chiffre l'exposition prud'homale et repère les vices de procédure avant l'audience ».
- **Immigration** : « LegalCase qualifie le dossier (titre, recours), vérifie les conditions de validité et sécurise les délais (CESEDA) ».
- **Famille** : « LegalCase structure la liquidation de communauté, chiffre prestation compensatoire et pension alimentaire (barèmes), et cadre la procédure de divorce ».
> Règle copy : pas le mot « IA » (« LegalCase fait X »), ton « gain de productivité cabinet », pas « remplace l'avocat ».

## E. Séquence multichannel type (Lemlist, par prospect, stop-on-reply)
1. J0 — Demande de connexion LinkedIn + note courte (accroche domaine).
2. J+2 — Message LinkedIn complet (si connexion acceptée).
3. J+3 — Email perso (intro fait vérifié + valeur domaine + Calendly).
4. J+6 — Relance email courte.
5. J+9 — Relance LinkedIn légère.
> Throttle : 30-40 connect/j, délais aléatoires. Répondre aux replies < 24 h (priorité absolue).

## F. Garde-fous
- Cabinets privés spécialisés uniquement (écarter juristes d'entreprise + généralistes à pôle vague).
- Emails catch-all/invalid → canal LinkedIn.
- **Mesure** : taux réponse / démo PAR DOMAINE → c'est le livrable du test PMF. Décider sur données, pas impressions.
- Escalade volume (5 000+) = M-57, seulement si ratio démo/touch prouvé.
