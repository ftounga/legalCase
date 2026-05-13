# Démo Renversez — 13/05/2026 — Réponses préparées aux objections / questions

> Aide-mémoire à garder sous les yeux pendant la démo (écran 2 ou imprimé).
> Format : Question / Réponse courte / Réponse étendue si elle creuse.

---

## 1. Open data jurisprudence — question posée au booking

**Q : « Votre IA intègre-t-elle l'open data de la jurisprudence ? »**

**Réponse courte :**

> Non. Ce n'est pas mon couloir. Doctrine, Lexis Plus, Lefebvre couvrent ça très bien. Mon couloir, c'est l'aval : ce qu'on fait avec le dossier client une fois sur le bureau.

**Si elle creuse — « vous comptez l'intégrer ? » :**

> Cette question m'a été posée par 3 prospects cette semaine. Honnêtement : pas tant que le marché jurispru reste tenu par les éditeurs établis. Construire un produit jurispru qui rivalise avec Doctrine prendrait 2 ans. Mon angle, c'est plutôt de me concentrer sur ce que Doctrine ne fait pas — le travail sur le dossier individuel.

**Si elle insiste — « mais comment je fais pour citer mes arrêts dans mes conclusions ? » :**

> Vous continuez à utiliser votre outil jurispru habituel. LegalCase ne remplace pas ça. LegalCase intervient AVANT et APRÈS la recherche jurispru : avant, il extrait les éléments du dossier qui orientent vos requêtes jurispru ; après, il intègre votre raisonnement juridique dans les conclusions générées.

---

## 2. Prix, abonnement, résiliation — question posée au booking

**Q : « Le prix de l'abonnement mensuel ou annuel, les conditions de résiliation ? »**

**Réponse courte :**

> 3 plans payants : SOLO 99 €/mois (1 user), TEAM 219 €/mois (3 users, +59 €/user supp, cap 6), PRO 429 €/mois (5 users, +79 €/user supp, sans cap). Mensuel, sans engagement, résiliable en 1 clic. Essai 14 jours gratuit sans CB.

**Détail si elle creuse :**

- **SOLO 99 € HT/mois** : 1 seul utilisateur. Pour le solo founder ou l'avocat individuel.
- **TEAM 219 € HT/mois** : 3 utilisateurs inclus. Jusqu'à 6 maximum (+59 € HT par utilisateur supplémentaire au-delà des 3 inclus).
- **PRO 429 € HT/mois** : 5 utilisateurs inclus. Pas de cap (+79 € HT par utilisateur supplémentaire au-delà des 5 inclus).

**Conditions de résiliation :**

- **Mensuel uniquement, pas d'engagement annuel forcé**
- **Résiliation en 1 clic dans l'espace billing du compte**
- Pas de pénalité, pas de frais cachés
- Si vous résiliez le 12 du mois, vous restez sur l'outil jusqu'au 11 du mois suivant — votre paiement du mois en cours est honoré, c'est tout
- Pas de prorata bizarre, pas de remise « si vous restez 6 mois »

**Essai gratuit 14 jours** :

- Sans carte bancaire requise
- Toutes les fonctionnalités sauf les exports massifs (anti-abus)
- À J+14 : si vous voulez continuer → vous mettez votre CB et choisissez votre plan. Si pas → le compte passe en lecture seule, accès aux dossiers déjà créés mais pas de nouvelle analyse. **Aucune facturation surprise.**

**Comparatif marché (à mentionner pour cadrer la valeur) :**

| Outil | Prix par user |
|-------|---------------|
| Harvey AI | 200-460 € |
| CoCounsel (Thomson Reuters) | ~300 € |
| Doctrine AI | 100-200 € |
| Predictice | 100-200 € |
| Jimini AI | 50-100 € |
| **LegalCase** | **99 € (SOLO) / 73-99 € (TEAM) / 86-118 € (PRO)** |

> « Je suis sciemment positionné en dessous du marché pour ma phase d'entrée. C'est le moment où je peux gagner votre confiance. Les clients qui rentrent maintenant gardent leur prix d'entrée à vie, même quand je revaloriserai dans 6-9 mois. »

---

## 3. « C'est juste une application de classement / OCR / extract de documents » (objection Mengue)

**Préemption proactive — à dire AVANT qu'elle ne le pense (en intro de la démo, Bloc 2 du script) :**

> Je préempte une objection que j'ai entendue récemment : « c'est juste une application qui analyse et extrait des documents, j'en ai déjà une ». C'est exact sur la partie haute. Mais le différenciateur de LegalCase, ce n'est pas l'extraction de pièces — ça, c'est devenu un commun de marché. Le différenciateur, c'est ce qu'on fait avec les pièces une fois extraites : des outils décisionnels métier qui se pré-remplissent automatiquement.

**Si elle ressort l'objection en fin de démo malgré la préemption :**

> Je comprends la lecture, mais je voudrais qu'on reprenne précisément : ce que j'ai montré, l'outil F-DT-08 qui calcule un score de validité du licenciement en tenant compte de la procédure, des motifs, des courriers d'alerte, des refus de formation contestables — ça ne se fait pas dans un outil d'extraction de documents. Ça suppose un raisonnement juridique. C'est ce raisonnement automatisé qui distingue LegalCase. L'extraction est juste l'amont nécessaire.

**Question retour à lui poser :**

> Concrètement, quel est l'outil que vous utilisez aujourd'hui pour extraire vos documents ? Et est-ce qu'il vous pré-remplit un calcul d'indemnité ou un score de validité ? Si oui, je suis preneur du nom — c'est utile à mon analyse marché. Si non, c'est exactement le gap que je couvre.

---

## 4. RGPD — question posée 2 fois par Mengue (réponse béton à préparer)

**Q : « Comment vous gérez le RGPD et le secret professionnel ? »**

**Réponse complète (à dire en 1 minute, pas plus) :**

> Hébergement exclusivement en **Région européenne AWS, datacenters Paris**. Aucune donnée client ne sort de l'Europe. Mes sous-processeurs sont les suivants :
>
> - **AWS Paris** : hébergement (compute + base de données + stockage S3)
> - **Anthropic via AWS Bedrock EU** : modèle IA pour les analyses, l'inférence se fait dans la région européenne, les données ne sortent pas
> - **Brevo (FR)** : emails transactionnels (notifications produit, pas de données dossier)
> - **Stripe (UE)** : paiement, traitement dans l'UE
>
> **Pas de Cloud Act applicable** : AWS Paris est une entité européenne avec contrat européen ; même si AWS US a une maison-mère américaine, le contrat de traitement est européen et le Cloud Act ne peut pas être invoqué pour réquisitionner les données stockées en UE.
>
> **DPA disponible sur demande** — c'est une simple signature à compléter dans l'espace billing.
>
> **Chiffrement** : TLS 1.3 en transit, AES-256 au repos. Les fichiers PDF sont stockés sur S3 chiffré, accès par signed URLs expirant.
>
> **Isolation par workspace** : aucune donnée d'un client n'est jamais accessible à un autre client (gate `workspace_id` sur tous les endpoints, audité).
>
> **Différence avec l'écosystème US** : Harvey, CoCounsel, Jimini sont hébergés aux États-Unis. Pour un avocat soumis au secret professionnel, c'est un sujet sérieux. Moi je suis dev solo français hébergé en France, c'est ma promesse d'entrée.

**Si elle re-pose la question** (signal de doute) : **ne pas répéter, creuser** :

> Vous me re-posez la question — c'est ma faute, j'ai dû manquer de précision. Sur quel point précisément voulez-vous que je sois plus net ? Sous-processeurs ? Lieu d'hébergement ? Chiffrement ? DPA ?

→ Elle va te donner le vrai sujet qui la préoccupe. Réponds spécifiquement.

---

## 5. « Pourquoi pas développer ça au Cameroun ? » (Mengue)

**Q : Question hors V1, en mode amical.**

**Réponse :**

> Merci, c'est gentil. Honnêtement, mon marché cible aujourd'hui c'est la France et la Belgique parce que j'y connais le droit, le tissu cabinets et les contraintes. Élargir vers un autre pays demande un investissement de plusieurs mois sur les référentiels juridiques (Code du travail OHADA, procédures locales, conventions collectives) — je ne peux pas tout faire en même temps. Si la France et la Belgique marchent, l'élargissement deviendra une vraie question. Pour l'instant, je reste focus.

---

## 6. « C'est fait par une seule personne ? Quel est le risque de pérennité ? »

**Q : Objection classique — solo founder = risque.**

**Réponse :**

> Vous avez raison de poser la question, c'est un sujet sérieux. Trois éléments concrets :
>
> 1. **Backups quotidiens chiffrés exportables** : à tout moment, vous pouvez exporter vos données en PDF + JSON depuis l'espace billing. Si demain je disparais, vous récupérez vos dossiers en 1 clic.
> 2. **Code en escrow** : le code source est déposé chez un tiers de confiance. Si l'entreprise cesse d'exister, le code passe en open-source. Vous pouvez le reprendre ou le faire reprendre.
> 3. **Pas de lock-in technique** : vos fichiers sources (PDF, DOCX) restent vos fichiers, je ne fais que les analyser. Pas de format propriétaire qui vous enferme.
>
> Le risque solo founder est réel. Je le compense par la transparence et la portabilité de vos données. Et soyons honnêtes — Harvey est aussi un risque (concentration US), Doctrine est un risque (récent rachat possible), CoCounsel est dans un grand groupe US. Personne n'est sans risque sur ce marché.

---

## 7. « Vos concurrents font la même chose, pourquoi vous choisir ? »

**Réponse :**

> Trois différenciateurs clairs :
>
> 1. **Outils décisionnels métier pré-remplis** — 92 outils par domaine et par pays (FR + BE), pas du généraliste. Harvey ou Doctrine font de l'IA généraliste sur du juridique. Moi je fais du spécialisé par situation métier (validité licenciement, comparateur indemnités, recours préfecture, prestation compensatoire…). Vous voyez la différence concrète sur l'écran.
>
> 2. **Souveraineté européenne** — hébergement Paris, pas de Cloud Act. Si vous prenez Harvey demain, vos pièces dossier passent par des serveurs US sans recours possible en cas de réquisition fédérale. Avec moi, vos données ne sortent jamais d'Europe.
>
> 3. **Prix d'entrée 2-3× inférieur** — c'est ma phase d'entrée, je gagne votre confiance maintenant pour revaloriser plus tard tout en grandfathering les premiers clients.

---

## 8. « Vous m'avez parlé du droit du travail, qu'est-ce qu'il y a sur l'immigration ? » (probable, vu sa double spécialité)

**Réponse :**

> Bonne question, mon V1 couvre les 3 domaines que vous pratiquez :
>
> - **Droit du travail** : ce que je viens de vous montrer
> - **Droit de l'immigration et asile** : analyse de validité d'un titre de séjour (F-IM-05), recours préfecture pour refus de titre (F-IM-06), droit au travail (F-IM-07), procédure asile.
> - **Droit de la famille** : divorce contentieux et amiable, prestation compensatoire, partage immobilier, garde des enfants
>
> Si vous voulez, je peux switcher rapidement sur un cas immigration. J'ai un cas de recours préfecture (Amadou — refus de renouvellement de titre salarié) sous la main. Vous voulez voir 3 minutes ?

→ Si oui, démo Amadou.
→ Si non, retour direct au closing.

---

## 9. « Comment je sais que l'IA ne se trompe pas ? » (frein adoption)

**Réponse :**

> Question essentielle. Trois mécanismes :
>
> 1. **Sources légales citées** : chaque calcul d'indemnité ou score de validité cite l'article du Code du travail, l'article conventionnel, et la jurisprudence quand pertinente. Vous vérifiez en 30 secondes.
>
> 2. **Cohérence multi-source (F-IA-03)** : si vous saisissez manuellement une valeur qui contredit ce que l'IA a extrait des pièces, vous avez une alerte visuelle en temps réel. Vous ne pouvez pas vous tromper silencieusement.
>
> 3. **L'avocat reste responsable** : LegalCase est un outil d'aide à la décision, pas une décision. C'est vous qui validez chaque calcul, chaque conclusion. Je ne remplace pas votre signature, je vous fais gagner du temps sur la mécanique.

**Si elle insiste — « mais si l'IA hallucine ? »** :

> L'IA hallucine moins quand elle a les pièces sous les yeux. Mon architecture lit chaque pièce extractivement (citation littérale, pas reformulation) puis assemble. Si quelque chose vous semble suspect dans une analyse, vous cliquez et l'outil vous renvoie à la pièce source avec la phrase exacte d'où c'est tiré. C'est traçable, pas du « croyez-moi sur parole ».

---

## 10. « Combien de clients avez-vous aujourd'hui ? » (signal de validation marché)

**Réponse honnête :**

> Je rentre tout juste en commercial actif. Aujourd'hui je n'ai pas encore de client payant signé — je suis en phase de test client avec quelques avocats du réseau et de la prospection (vous êtes une des premières à me prendre 30 minutes, je vous en remercie). Ma cible des 90 prochains jours : 10 clients payants stables.
>
> Je vous donne cette transparence parce que je préfère que vous décidiez en connaissance de cause. Le coût pour vous = 14 jours d'essai gratuit, pas de carte bancaire. Si vous trouvez de la valeur, vous me dites. Si pas, vous ne perdez rien. Et vous m'aidez à façonner le produit pour les vrais usages des avocats.

→ **Cette honnêteté est un atout vs Harvey/CoCounsel qui ont l'air corporate mais sont eux-mêmes en validation marché.**

---

## 11. Phrase de closing si elle hésite mais ne refuse pas

> Je vous propose deux choix simples : (a) on s'arrête là, on reste en contact, je vous recontacte dans 3 mois pour voir où en sont vos projets ; (b) je vous configure le compte test maintenant, vous regardez quand vous voulez, sans engagement. La deuxième option ne vous coûte que 30 secondes de connexion, et vous gardez une porte ouverte.

---

## Signaux à observer pendant la démo

| Signal | Interprétation | Action |
|--------|----------------|--------|
| Elle prend des notes | Intéressée, qualifie | Pousse plus loin sur le décisionnel |
| Elle pose des questions techniques (RGPD, hébergement, sources) | Sérieuse, dérisque | Réponds précisément, cite les chiffres |
| Elle reste silencieuse / regarde le téléphone | Désengagement | Reprends le pilotage, pose une question directe : « Maître Renversez, est-ce que ce que je montre vous parle ou je perds mon temps ? » |
| Elle dit « intéressant mais je vais y réfléchir » | Politesse, désengagement probable | Tu ne pars pas sur un « OK je vous laisse réfléchir ». Tu insistes pour qu'elle accepte au moins les identifiants. Cf. Bloc 5 du script. |
| Elle te coupe pour pitcher SON pratique | Bon signe (engagement) | Écoute, pose des questions, montre comment LegalCase répond précisément à ce qu'elle dit |
| Elle remet en cause le pitch (« c'est commodity », « c'est cher », « c'est dangereux ») | Engagement de niveau supérieur | Réponds calmement avec les réponses préparées — elle te teste, c'est bon signe |

---

## À NE PAS dire (apprentissages Mengue)

- ❌ « C'est révolutionnaire » → trop fort, met sur la défensive
- ❌ « C'est unique sur le marché » → faux, et tu perds en crédibilité
- ❌ « Tous les avocats devraient l'avoir » → arrogance
- ❌ « Vous allez gagner 50 % de votre temps » → chiffre creux, pas démontré
- ❌ « Je suis solo mais je vais embaucher bientôt » → ça sonne défensif
- ❌ « C'est moins cher que les concurrents » sans contexte → ressemble à du low-cost
- ❌ « Mengue m'a dit que… » → ne jamais citer un autre prospect
- ❌ « Je vous laisse la main, vous me dites quand vous êtes prête » → invitation au désengagement, on a vu le résultat
