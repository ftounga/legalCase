# M-79 — Kit d'activation : acquisition avocat 3 domaines (stack Apollo + Lemlist)

Statut : **activée 2026-06-09 par override PO** (cf. MARKETING_BACKLOG.md M-79).
**Stack retenue : Apollo + Lemlist + Claude API + Gmail (warm-up via Lemlist).**
Cible : **cabinets d'avocats multi-avocats**, France, 3 domaines produits.

> Décision 2026-06-09 : Apollo remplace **Sales Navigator** (sourcing) **ET** Hunter (emails) —
> il fait les deux. On réutilise le compte Apollo déjà servi pour les DRH. Lemlist reste
> indispensable pour le **warm-up + multi-touch LinkedIn + délivrabilité + tracking**.

---

## Rôle de chaque outil dans la chaîne
| Métier | Outil | Détail |
|---|---|---|
| 1. Sourcing / ciblage | **Apollo** | filtres titre + cabinet + taille + spécialité, 3 domaines |
| 2. Enrichissement email | **Apollo** (inclus) | emails pros révélés (crédits) — plus besoin de Hunter |
| 3. Personnalisation | **Claude API** (+ Claude Code) | 1 intro vérifiée par contact, par domaine, en batch |
| 4. Envoi multi-touch | **Lemlist** | warm-up Gmail, séquence connect+message+email+relances, stop-on-reply, tracking |

---

## A. Actions FRANCK (souscriptions — non automatisables côté Claude)
1. **Apollo** — plan payant (Basic ~50 $/mois) pour crédits emails + export volume (le free plafonne vite).
2. **Lemlist Multichannel Pro** ~99 €/mois (ou Smartlead Advanced ~64 €). C'est lui qui gère le **warm-up**.
3. **Connecter** Lemlist à : (a) ton compte LinkedIn perso (limite 30-40 connect/j), (b) Gmail (warm-up auto).
4. Fournir la CB / valider les abonnements.
> ❌ Plus de **Hunter** ni de **Sales Navigator** ni d'**Evaboot**.
> Coût mois 1 ≈ **200 €** (Apollo ~50 $ + Lemlist 99 € + Claude API ~50 €). Récurrent ≈ 150-200 €/mois.

## B. Ce que je prépare / lance (côté Claude — dès exports dispo)
- Les requêtes Apollo (ci-dessous).
- Le tri + dédoublonnage + tag domaine + contrôle adéquation des exports.
- La génération **batch** des intros personnalisées (1 fait vérifié/contact, par domaine).
- Les séquences (copy connect + message + relances) prêtes à coller dans Lemlist.
- Le tableau de suivi + analyse du taux de réponse **par domaine** (= livrable du test PMF).

---

## C. Requêtes Apollo (People Search — une par domaine)
Filtres communs :
- **Person location** : France
- **Company / Industry** : Legal Services / Law Practice (cabinets d'avocats — PAS les juristes d'entreprise)
- **Job titles** : `Avocat associé`, `Associé`, `Avocat` (décideurs — exclure « collaborateur »)
- **# Employees** : `11-20`, `21-50`, `51-200` (cabinets multi-avocats ; on saute les solos)
- **Reveal emails** : activer (consomme des crédits Apollo) → emails pros directs

Mots-clés (company/keyword), **une recherche par domaine**, exporter ~6 contacts chacune :
1. **Droit du travail** → `droit du travail`, `droit social`
2. **Immigration** → `droit des étrangers`, `droit de l'immigration`, `droit de la nationalité`
3. **Droit de la famille** → `droit de la famille`, `divorce`, `droit patrimonial`

Export → **CSV natif Apollo** (peu importe le nom, le pipeline détecte tout `.csv` récent dans `~/Downloads`).

## D. Angle de valeur par domaine (base des intros + séquences)
- **Travail** : « à partir des pièces, LegalCase chiffre l'exposition prud'homale et repère les vices de procédure avant l'audience ».
- **Immigration** : « LegalCase qualifie le dossier (titre, recours), vérifie les conditions de validité et sécurise les délais (CESEDA) ».
- **Famille** : « LegalCase structure la liquidation de communauté, chiffre prestation compensatoire et pension alimentaire (barèmes), cadre la procédure de divorce ».
> Règle copy : pas le mot « IA » (« LegalCase fait X »), framing « gain de productivité cabinet », jamais « remplace l'avocat ».

## E. Séquence multichannel type (Lemlist, par prospect, stop-on-reply)
1. J0 — Demande de connexion LinkedIn + note courte (accroche domaine).
2. J+2 — Message LinkedIn complet (si connexion acceptée).
3. J+3 — Email perso (intro fait vérifié + valeur domaine + Calendly).
4. J+6 — Relance email courte.
5. J+9 — Relance LinkedIn légère.
> Throttle : 30-40 connect/j, délais aléatoires. **Laisser Lemlist faire le warm-up Gmail quelques jours avant le 1er envoi de masse** (on a déjà envoyé 23 mails froids le 09/06 depuis le même domaine). Répondre aux replies < 24 h.

## F. Garde-fous
- Cabinets privés spécialisés uniquement (écarter juristes d'entreprise + généralistes à pôle vague).
- Emails catch-all/invalid → canal LinkedIn.
- **Mesure** : taux réponse / démo PAR DOMAINE → c'est le livrable du test PMF. Décider sur données, pas impressions.
- Escalade volume (5 000+ prospects) = M-57, seulement si ratio démo/touch prouvé.
