# Skill — prospect-account-bootstrap

**Quand l'invoquer** : à la fin d'une démo prospect (Calendly, salon, événement), pour créer le compte prod du prospect **côté admin** au lieu de lui demander de le faire en autonomie pendant ou après la démo. Élimine la friction d'activation matérielle observée sur les prospects non-tech (typiquement 50+, faible aisance informatique).

Triggers utilisateur typiques :
- *"Crée le compte de Maître Untel"*
- *"Bootstrap un compte pour ce prospect"*
- *"Inscris-la, elle galère"*

## Pourquoi cette skill existe

**Signal terrain** : 2/2 démos consécutives (Mengue Nzengue 11/05, Marjolaine Renversez 13/05) ont échoué l'activation autonome. Mengue : compte jamais créé. Renversez : galère lien activation + partage écran + chat Meet → opérateur a créé le compte à la main en prod (UPDATE SQL). Demander au prospect de s'inscrire pendant ou juste après la démo introduit une friction qui tue le taux d'activation.

**Solution** : capturer les infos en fin de démo (nom, prénom, email, choix pays/domaine), créer le compte côté opérateur en 2-3 minutes après le RDV, envoyer un mail avec identifiants + reset password en 1er login. Le prospect n'a qu'à cliquer un lien et changer son mdp.

## Pré-requis non négociables

| # | Item | Comment vérifier |
|---|------|------------------|
| 1 | Accès `kubectl` au cluster prod (`arn:aws:eks:eu-west-3:504895205419:cluster/legalcase-shared`) | `kubectl config current-context` |
| 2 | Endpoints d'auth locale déployés en prod | `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/forgot-password` (déployés depuis migration 022 `infra-auth-locale.xml`) |
| 3 | Accès Gmail MCP pour envoi mail | tool `mcp__gmail__send_email` disponible |
| 4 | Consentement explicite du prospect (verbal en démo) à la création du compte par l'opérateur | Toujours demander oralement avant — éviter surprise |

## Procédure

### Étape 1 — Collecter les infos (en démo)

À la fin de la démo, demander :
- Prénom, NOM (format affiché en signature mail : NOM Prénom pour les avocats francophones)
- Email pro
- Pays (FRANCE / BELGIQUE)
- Domaine principal (DROIT_DU_TRAVAIL / DROIT_IMMIGRATION / DROIT_FAMILLE)
- Nom souhaité pour le cabinet/workspace (souvent `NOM-PRENOM` ou nom de la structure)

### Étape 2 — Vérifier l'existant en DB prod

```bash
PGPASSWORD=$(kubectl exec -n production <backend-pod> -- printenv SPRING_DATASOURCE_PASSWORD)
kubectl run psql-tmp -n production --image=postgres:16 --rm -i --restart=Never \
  --env="PGPASSWORD=$PGPASSWORD" --quiet -- \
  psql -h <RDS_HOST> -U legalcase_admin -d legalcase \
  -c "SELECT id, email, first_name, last_name FROM users WHERE email='<email>';"
```

Si le compte existe :
- **Cas A — compte vierge sans workspace** (cas Renversez 13/05) : **NE PAS supprimer**. Faire reset password (UPDATE `password_hash`) + créer workspace + membership.
- **Cas B — compte avec activité métier** : ne rien toucher, recontacter le prospect, demander ce qui bloque côté UX.

### Étape 3 — Générer un mot de passe initial simple ET BCrypt

Mot de passe initial : **un mot français de 8-12 caractères**, facile à prononcer/épeler au téléphone (ex: `printemps`, `automne2025`, `bonjour123`). Le prospect le changera au 1er login.

Hash BCrypt rounds=10 via pod éphémère Python :

```bash
kubectl run bcrypt-gen -n production --image=python:3.12-slim --rm -i --restart=Never --quiet -- \
  sh -c "pip install -q bcrypt && python -c \"import bcrypt; print(bcrypt.hashpw(b'<MDP>', bcrypt.gensalt(rounds=10)).decode())\""
```

### Étape 4 — Transaction SQL : compte + workspace + membership

```sql
BEGIN;

-- Si compte n'existe pas : INSERT INTO users + auth_accounts (provider='LOCAL', email_verified=true)
-- Si compte existe : UPDATE auth_accounts SET password_hash='<HASH>' WHERE user_id='<UID>' AND provider='LOCAL';

INSERT INTO workspaces (id, name, slug, owner_user_id, billing_email, plan_code, status, created_at, legal_domain, country)
VALUES (gen_random_uuid(), '<WORKSPACE_NAME>', gen_random_uuid()::text, '<UID>', '<email>',
        'FREE', 'ACTIVE', NOW(), '<LEGAL_DOMAIN>', '<COUNTRY>')
RETURNING id;

INSERT INTO workspace_members (workspace_id, user_id, member_role, created_at, is_primary)
SELECT id, '<UID>', 'OWNER', NOW(), true
FROM workspaces WHERE owner_user_id='<UID>' AND name='<WORKSPACE_NAME>';

COMMIT;
```

### Étape 5 — Vérifier le login bout-en-bout

```bash
curl -sS -X POST https://legalcase.fr/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -c /tmp/cookies.txt \
  -d '{"email":"<email>","password":"<MDP>"}'

curl -sS -b /tmp/cookies.txt https://legalcase.fr/api/v1/workspaces
rm /tmp/cookies.txt
```

Le 1er retour doit être 200 + objet user. Le 2ᵉ doit lister le workspace créé.

### Étape 6 — Envoyer le mail de bienvenue

**Toujours montrer le mail au user opérateur AVANT envoi** (pas en autonomie). Template :

```
Sujet : Votre accès LegalCase est prêt — <Prénom>

Bonjour <Prénom>,

Votre compte LegalCase est prêt, vous pouvez vous connecter dès maintenant.

Vos identifiants
• URL : https://legalcase.fr
• Email : <email>
• Mot de passe : <MDP>

Je vous invite à changer ce mot de passe dès la première connexion
(menu profil → mot de passe).

Votre cabinet <WORKSPACE_NAME> est déjà configuré (<COUNTRY>, <DOMAINE>).

[Optionnel : 7 PDFs dossier fictif Dupont en PJ pour tester sans dossier réel,
ou texte encourageant à uploader un dossier réel à elle]

Rendez-vous <DATE> pour valider ensemble que tout fonctionne
Je vous propose 3 créneaux : ...

[...]

TOUNGA Franck
Fondateur — LegalCase
```

Envoyer via `mcp__gmail__send_email` depuis `tounga.franck@ng-itconsulting.com` (cf. [[user_pro_email]]).

### Étape 7 — Tracer dans la mémoire

Créer ou mettre à jour `memory/project_<nom>_post_demo_<date>.md` (type project) avec :
- Date démo + profil prospect
- Signaux terrain (positifs / freins / convergence avec autres démos)
- Workspace prod ID + user ID
- Gmail message ID du mail envoyé
- RDV suivi prévu

Ajouter ligne dans `MEMORY.md`.

## Variantes

### V1 — Compte existant à reset (cas Renversez)
Sauter l'INSERT users / auth_accounts. Garder UPDATE password_hash + INSERT workspace + membership.

### V2 — Prospect avec dossier réel apporté
Au lieu de joindre les 7 PDFs Dupont, suggérer dans le mail d'uploader son propre dossier directement. Donne un meilleur signal d'engagement (cf. memory `feedback_marketing_prospection`).

### V3 — Multi-utilisateurs (cabinet)
Si le prospect demande à inviter ses collaborateurs : ne PAS créer leurs comptes en bootstrap (volume × friction). Lui montrer l'écran d'invitation interne au cabinet pour qu'il le fasse lui-même quand il sera dans l'app.

## Hors périmètre

- Ne PAS bootstrap des comptes pour des prospects qui n'ont **pas demandé verbalement** un accès (risque de spam non-sollicité).
- Ne PAS bootstrap si le pays n'est pas couvert V1 (FR / BE uniquement à ce jour) — leur dire qu'on les rappelle quand le domaine est couvert.
- Ne PAS bootstrap avec un workspace déjà nommé "TEST" ou "DEMO" — utiliser le vrai nom de structure pour que le compte ait l'air pro dès l'arrivée.

## Métriques à surveiller (cron J+5 / J+10)

- A-t-il fait sa 1ère connexion ? (DB : `users.last_login_at` ou logs)
- A-t-il uploadé un dossier ? (DB : `case_files` count workspace)
- A-t-il lancé une analyse ? (DB : `documents` count + statut pipeline)
- Si 0 sur 3 à J+10 → relance manuelle. Si 0 sur 3 à J+15 → signal très fort que le bootstrap manuel ne suffit pas (UX onboarding à refondre).

## Cas d'usage validés

- 2026-05-14 — **Marjolaine Renversez** (avocate FR droit du travail) : bootstrap réussi après échec inscription autonome 13/05. Workspace `RENVERSEZ-MARJOLAINE` créé en DB prod, mail envoyé avec PDFs Dupont, RDV lundi 18/05. Voir [[project_renversez_post_demo_13_05]].

## Liens

- [[user_pro_email]] — adresse expéditeur tounga.franck@ng-itconsulting.com
- [[feedback_prod_url_canonical]] — URL prod = legalcase.fr (jamais ng-itconsulting.com en com client)
- [[feedback_no_ai_word_in_emails]] — minimiser le mot "IA" dans le mail de bienvenue
