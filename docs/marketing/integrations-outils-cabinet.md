# Intégrations avec les logiciels de gestion de cabinet (LPM) — antisèche

**Pour qui** : Franck, fondateur — usage avant tout RDV prospect.
**Format** : 5 min de lecture, structuré pour répondre vite.
**Pourquoi ce doc** : la question *« est-ce que ça s'intègre avec [Kleos / Secib / Jarvis] ? »* revient quasi systématiquement dès qu'un avocat évalue un nouvel outil. Une réponse hésitante = signal d'amateurisme. Une réponse claire = signal de maturité produit.

---

## Le marché des LPM en 1 paragraphe

Un **LPM** (Legal Practice Management) ou **PMS** (Practice Management System) est l'**ERP/CRM/GED tout-en-un d'un cabinet d'avocat**. Sans LPM, un cabinet de plus de 50 dossiers actifs devient ingérable (perdus dans Excel + Outlook + Drive). C'est l'équivalent fonctionnel d'un Salesforce + Microsoft 365 + ClickUp + facturier dans un seul outil métier.

Fonctions standard d'un LPM :

| Fonction | Équivalent dev/SaaS |
|----------|---------------------|
| Gestion des dossiers (un dossier = un client + son contentieux) | Project tracker |
| Gestion clients + adversaires + magistrats | CRM |
| Time tracking facturable (15 min, 30 min, par tâche) | Toggl / Harvest |
| Facturation + acomptes + provisions + CARPA | Stripe + spécialités métier |
| Comptabilité cabinet (comptes tiers, ventilation) | Sage / spécialités métier |
| Agenda audiences + délais procéduraux | Calendar partagé |
| GED (Gestion Électronique de Documents) | Dropbox / Drive |
| Échange RPVA / e-Barreau (réseau privé virtuel des avocats) | EDI métier |
| Archivage légal (5-30 ans selon pièce) | Storage long-terme |
| Gestion conflits d'intérêts (ne pas représenter 2 parties opposées) | Spécialité métier |

---

## Les acteurs principaux à connaître

### En **Belgique**

| Acteur | Editeur / Groupe | Position |
|--------|------------------|----------|
| **Kleos** | Wolters Kluwer (NL, 4 Md€ CA) | **Leader BE — standard de fait** |
| **Toga** | Sylogist | Niche, cabinets BE moyens |
| **Cicero LawPack** | Indépendant BE | Petits cabinets BE |
| **Knowliah** | Indépendant BE | KM juridique (pas LPM complet) |

### En **France**

| Acteur | Editeur / Groupe | Position |
|--------|------------------|----------|
| **Secib** | Septeo | **Leader FR — 12 000 cabinets** |
| **Kleos** | Wolters Kluwer | Présent FR mais derrière Septeo |
| **Jarvis Legal** | Septeo | Cabinets indépendants FR, UX moderne |
| **Polyacte** | Septeo | Niche |
| **Easystar** | Septeo | Petits/moyens cabinets FR |
| **AvoCom** | Indépendant | Petits cabinets FR |

### À l'**international**

| Acteur | Pays | Position |
|--------|------|----------|
| **Clio** | Canada | Leader mondial (~150 000 cabinets US/Canada/UK) — peu présent FR/BE |
| **MyCase** | US | Petit/moyen cabinet US |
| **PracticePanther** | US | US |
| **Lexis Visualfiles** | US/UK | Grand cabinet international |

### Ce que ça t'apprend sur le prospect

| Si le prospect dit utiliser... | Probabilité de profil |
|-------------------------------|----------------------|
| **Kleos** | Cabinet BE structuré, ≥ 2 avocats, ≥ 50 dossiers actifs, sensible à la stabilité — bonne cible LegalCase |
| **Secib** | Cabinet FR moyen-grand (Paris/IDF souvent), workflow industriel — cible LegalCase OK mais cycle de décision plus long |
| **Jarvis Legal** | Cabinet FR indépendant moderne, sensible UX — **excellente cible** LegalCase |
| **AvoCom / Easystar** | Petit cabinet FR, budget serré — cible LegalCase OK mais sensible au prix |
| **Rien (Outlook + Drive)** | Avocat solo, < 30 dossiers actifs, anti-paperasse — cible LegalCase si test simple à activer |
| **Clio** | Atypique en FR/BE — probablement avocat ayant exercé à l'international |

---

## Les 4 niveaux d'intégration

| Niveau | Ce que ça fait | Effort dev | Effort relationnel | Exemple concret |
|--------|----------------|-----------|--------------------|-----------------|
| **1 — SSO** (Single Sign-On) | Se connecter à LegalCase avec son compte Kleos | 1-2 semaines | Faible (OAuth standard) | Bouton *"Login avec Kleos"* au lieu de Google |
| **2 — Import / Export manuel** | Sortir un fichier d'un côté, l'entrer de l'autre | 0 (déjà couvert) | Aucun | Tu télécharges les PDFs du dossier depuis Kleos, drag-and-drop dans LegalCase |
| **3 — Connecteur API bidirectionnel** | Les 2 systèmes se parlent automatiquement | 2-4 semaines + maintenance permanente | Élevé (négociation 6-12 mois avec l'éditeur) | LegalCase voit la liste des dossiers Kleos. Synthèse LegalCase → automatiquement attachée au dossier Kleos |
| **4 — Intégration native (module)** | LegalCase apparaît à l'intérieur de Kleos | 4-8 semaines + maintenance | Très élevé (partenariat profond, partage de revenu) | Bouton *"Analyser avec LegalCase"* dans l'écran dossier Kleos. L'avocat ne quitte jamais Kleos |

---

## Position LegalCase aujourd'hui

✅ **Niveau 2 (import/export manuel) est opérationnel en V1.**

Workflow concret avec Kleos / Secib :

1. L'avocat ouvre son dossier dans Kleos
2. Sélectionne les pièces utiles (contrat, courriers, expertises, etc.)
3. **Clique "Télécharger" → ZIP local** (ou "Glisser vers explorateur")
4. Ouvre LegalCase dans un autre onglet
5. **Drag-and-drop le ZIP ou les PDFs** dans le wizard d'upload
6. Lance l'analyse (clic 1 fois)
7. Récupère la synthèse en **PDF / Word / Markdown**
8. **Importe la synthèse dans le dossier Kleos** comme nouvelle pièce (drag-and-drop inverse)

⏱ **Temps total** : ~30 secondes par dossier pour le copier-coller. ~2-5 minutes pour l'analyse IA. Comparé à 1-3 heures de lecture/synthèse manuelle = ratio gain 20-100x.

---

## Roadmap intégrations (M-76 du backlog marketing)

**Statut actuel** : `Bloqué` jusqu'à atteinte de **≥ 5 clients payants signés + ≥ 2 témoignages publics utilisables**.

**Pourquoi bloqué** :
1. **Coût technique** : chaque connecteur API (Kleos, Secib, Jarvis) = 2-4 semaines de dev + maintenance permanente (l'éditeur change son API régulièrement, on suit ou on casse)
2. **Coût relationnel** : l'éditeur (Wolters Kluwer pour Kleos, Septeo pour Secib/Jarvis) **dit non si on n'a pas déjà 50-100 cabinets clients** qui réclament le connecteur. Sans traction, la conversation est verrouillée pour 6-12 mois
3. **Coût d'opportunité** : tant que la valeur core (l'analyse IA elle-même) n'est pas validée, on n'investit pas dans la plomberie

**Quand l'attaquer** : quand on aura 50+ clients équipés Kleos qui réclament le connecteur. À ce moment-là, on pourra :
- Négocier dans de bonnes conditions avec Wolters Kluwer / Septeo
- Justifier le coût dev par le volume utilisateur
- Choisir le bon niveau (3 ou 4) selon le ROI

**Estimation timing** : V2-V3 (12-24 mois) selon traction.

---

## Réponses prêtes pour les 5 questions probables

### 1. *« Est-ce que ça s'intègre avec Kleos ? »*

> *« Pas en V1 — c'est sur la roadmap V2-V3, on l'attaquera quand on aura suffisamment de cabinets équipés Kleos qui le réclament pour qu'on puisse négocier le connecteur avec WoltersKluwer dans des bonnes conditions. Aujourd'hui, le mode d'usage c'est : vous prenez les PDFs depuis Kleos, vous les déposez dans LegalCase, vous récupérez la synthèse en PDF/Word et vous l'importez dans le dossier Kleos. C'est manuel mais c'est 30 secondes par dossier. »*

### 2. *« Et si vous ne vous intégrez jamais avec Kleos ? »*

> *« Possible. Mais le gain de temps de l'analyse IA (1-2 heures par dossier) est tellement supérieur au coût du copier-coller des PDFs (30 secondes) que ça reste rentable même sans intégration. Pour info, beaucoup de cabinets utilisent Kleos en parallèle d'autres outils non intégrés (transcription, signature électronique, e-Barreau) — c'est devenu la norme. »*

### 3. *« Quelles intégrations vous avez aujourd'hui ? »*

> *« Aucune intégration API en V1. Mais on est compatible avec **tous** les LPM qui exportent des PDFs — c'est-à-dire 100 % du marché. Le format PDF est universel, c'est notre porte d'entrée. »*

### 4. *« Pourquoi vous n'avez pas commencé par l'intégration ? »*

> *« Parce que la valeur core, c'est l'analyse IA, pas la plomberie. Tant que les avocats ne valident pas que l'analyse leur fait gagner 1-2 heures par dossier, l'intégration ne sert à rien. C'est l'erreur typique des SaaS qui sortent un produit moyen mais bien intégré : ils sont dépendants du bon vouloir des éditeurs et n'ont pas de différenciation. Notre stratégie c'est : valeur core d'abord, intégration ensuite quand le marché le réclame. »*

### 5. *« Mes pièces vont quitter Kleos quand je les charge dans LegalCase ? »*

> *« Oui — vous les exportez de Kleos puis vous les uploadez dans LegalCase. Côté LegalCase, elles sont stockées sur AWS Paris (eu-west-3), aucun stockage hors UE. Les sous-processeurs sont déclarés dans la politique de confidentialité (Anthropic, AWS, Brevo pour les emails transactionnels). Vous pouvez à tout moment supprimer vos données — bouton "supprimer" sur chaque dossier. La déontologie de l'avocat est respectée car LegalCase agit comme sous-traitant déclaré, avec contrat de sous-traitance signé à la souscription. »*

---

## Tableau réactions express

| Si elle dit... | Tu réponds... | Pivot vers... |
|----------------|---------------|---------------|
| *« J'utilise Kleos »* | *« Très bien, c'est notre cas d'usage type. »* | Workflow drag-and-drop + roadmap M-76 |
| *« J'utilise Secib »* | *« Idem Kleos côté workflow — export PDF, drag, analyse. »* | Workflow + roadmap |
| *« J'utilise Jarvis Legal »* | *« Excellent, vous êtes typiquement notre cible — UX moderne, cabinet indépendant. »* | Avantage couplage outil moderne + IA |
| *« Je n'ai pas de LPM, je gère sur Outlook »* | *« Pas de souci, LegalCase ne remplace pas un LPM. À terme vous voudrez peut-être un LPM, mais ça peut attendre. »* | LegalCase comme premier outil structurant |
| *« Vous remplacez mon LPM ? »* | *« Non, on est complémentaire. LegalCase = analyse IA des dossiers. LPM = gestion administrative du cabinet. On ne se marche pas dessus. »* | Différenciation produit |
| *« Quand l'intégration arrivera-t-elle ? »* | *« V2-V3, estimation 12-24 mois selon traction. Si vous voulez être informée quand le sujet bouge, je vous mets en boucle. »* | Capitaliser sur le test 14 j |

---

## Erreurs à éviter

| Erreur | Pourquoi |
|--------|----------|
| Promettre une intégration sans engagement de date | Le prospect te citera dans 6 mois — promesse cassée = perte définitive |
| Mentionner Clio comme alternative | Très peu présent FR/BE, semble dépaysant — tu donnes l'impression de mal connaître le marché local |
| Dénigrer Kleos/Secib | Le prospect en est probablement client — tu insultes son choix passé |
| Sous-estimer le coût d'une intégration | Si tu dis "2 jours de dev", tu passes pour amateur. La vérité c'est 2-4 semaines + 6-12 mois de négo |
| Ignorer la question intégration | Réponse silence/évasive = l'avocat sent l'amateurisme. **Réponds toujours frontalement** |

---

## Mise à jour de ce document

**Dernière révision** : 2026-05-10
**Sources** :
- Sites web éditeurs (kleos.com, secib.fr, jarvis-legal.fr) — vérifié 2026-05-10
- M-76 dans `docs/MARKETING_BACKLOG.md`
- Retours d'expérience marché legaltech FR/BE

**À mettre à jour si** :
- Un éditeur sort une **API publique** sans négociation préalable (rare mais déjà arrivé chez Clio)
- Un nouveau LPM émerge en FR/BE (ex: clone open source)
- LegalCase atteint le seuil de 5 clients payants → débloque M-76 → mise à jour majeure
- Un éditeur **rachète** un concurrent legaltech IA (signal de menace direct)
