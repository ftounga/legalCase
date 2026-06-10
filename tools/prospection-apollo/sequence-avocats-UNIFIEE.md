# Séquence UNIQUE avocats (variables) — à coller dans Lemlist

Une seule campagne, une seule séquence, couvre les 3 domaines via variables.
Import : `lemlist-import-master.csv` (367 leads).
Variables (créées auto depuis les colonnes du CSV) : `{{firstName}}`, `{{companyName}}`,
`{{introPerso}}`, `{{subject}}`, `{{valueProp}}`, `{{domainNoun}}`.
Calendly : https://calendly.com/tounga-franck-ng-itconsulting/30min
Expéditeur : tounga.franck@ng-itconsulting.com (= adresse en warm-up).
Activer **Stop on reply**. Throttle 30-40/j, horaires ouvrés Europe/Paris.

---

## Étape 1 — J0 · LinkedIn : invitation + note
> Bonjour {{firstName}}, je développe LegalCase, un outil d'analyse de dossiers en {{domainNoun}}. J'aimerais beaucoup le regard d'un avocat spécialisé. Au plaisir d'échanger.

## Étape 2 — J+2 · LinkedIn : message (si connexion acceptée)
> Merci d'avoir accepté, {{firstName}}. {{introPerso}} En deux mots, LegalCase part des pièces d'un dossier et en sort l'essentiel — chiffrage, points de procédure, écritures argumentées — un vrai gain de temps sur la préparation. Seriez-vous ouvert à une démo de 20 min, sans engagement ? Mon agenda : https://calendly.com/tounga-franck-ng-itconsulting/30min

## Étape 3 — J+3 · Email
**Objet :** `{{subject}}`
> Bonjour {{firstName}},
>
> {{introPerso}}
>
> {{valueProp}}
>
> Je serais ravi de vous le montrer en 20 minutes, sans engagement ; votre regard d'avocat en {{domainNoun}} m'intéresse particulièrement. Vous pouvez réserver le créneau qui vous arrange : https://calendly.com/tounga-franck-ng-itconsulting/30min
>
> Bien à vous,
>
> Franck Tounga
> Fondateur — LegalCase
> tounga.franck@ng-itconsulting.com
> https://legalcase.fr

## Étape 4 — J+6 · Email : relance
**Objet :** `Re: {{subject}}` (réponse au fil)
> Bonjour {{firstName}}, je me permets un bref rappel — je serais ravi de vous montrer en 20 min comment LegalCase prépare un dossier en {{domainNoun}} à partir des pièces. Si le sujet vous parle : https://calendly.com/tounga-franck-ng-itconsulting/30min . Sinon, dites-le-moi sans souci.
>
> Bien à vous,
> Franck

## Étape 5 — J+9 · LinkedIn : relance
> {{firstName}}, je relance une dernière fois — un avis d'avocat en {{domainNoun}} me serait précieux. 20 min en visio quand vous voulez : https://calendly.com/tounga-franck-ng-itconsulting/30min

---

## Réglages campagne
- **Sender** : tounga.franck@ng-itconsulting.com (l'adresse chauffée — PAS ai-legalcase@).
- **Stop on reply** : ON.
- **Limite** : 30-40 actions/jour, lun-ven 9h-18h (Europe/Paris).
- **Ne lancer qu'après ~1-2 semaines de warm-up** (score Deliverability sain).
- Comparaison par domaine : filtrer les stats sur la variable `{{domaine}}`.
- ⚠️ ~100 leads sans email = LinkedIn-only → les étapes 1/2/5 (LinkedIn) les couvrent ; les étapes email les sautent automatiquement (pas d'adresse).
