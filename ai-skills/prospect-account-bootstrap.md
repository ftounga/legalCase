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
| 1 | Compte super-admin opérationnel (Google OAuth ou LOCAL) pour l'opérateur | `users.is_super_admin = true` |
| 2 | Endpoints super-admin + auth locale déployés en prod | `/api/v1/super-admin/prospect-bootstrap` (F-251 SF-251-03), `/api/v1/auth/login` |
| 3 | Accès Gmail MCP pour envoi mail | tool `mcp__gmail__send_email` disponible |
| 4 | Consentement explicite du prospect (verbal en démo) à la création du compte par l'opérateur | Toujours demander oralement avant — éviter surprise |

**Mode préféré depuis SF-251-04** : utiliser la page UI `/super-admin/prospect-bootstrap` qui formulaire-ise les champs de l'étape 1 et appelle l'endpoint pour vous. Le bloc curl de l'étape 4 reste en backup pour cas exceptionnel (UI down, scripting batch).

## Procédure

### Étape 1 — Collecter les infos (en démo)

À la fin de la démo, demander :
- Prénom, NOM (format affiché en signature mail : NOM Prénom pour les avocats francophones)
- Email pro
- Pays (FRANCE / BELGIQUE)
- Domaine principal (DROIT_DU_TRAVAIL / DROIT_IMMIGRATION / DROIT_FAMILLE)
- Nom souhaité pour le cabinet/workspace (souvent `NOM-PRENOM` ou nom de la structure)

### Étape 2 — Choisir le mot de passe initial

Mot de passe initial : **un mot français de 8-12 caractères**, facile à prononcer/épeler au téléphone (ex: `printemps2026`, `automne2025`, `bonjour123`). Le prospect le changera au 1er login. **Pas besoin de BCrypt à la main** : l'endpoint backend hash via BCrypt en interne (SF-251-03).

### Étape 3 — (réservée — étape de vérification DB facultative)

Si vous avez besoin de pré-vérifier l'existant (ex. avant un événement qui a généré du double-onboarding), interrogez l'API super-admin existante (`/api/v1/super-admin/users`) plutôt que d'ouvrir un psql. L'endpoint bootstrap (étape 4) refuse de toute façon les comptes déjà actifs (HTTP 409) — vous serez alerté si conflit.

### Étape 4 — Appel endpoint super-admin `prospect-bootstrap` (F-251 SF-251-03)

> **Pourquoi cet endpoint et plus de SQL direct ?** SF-251-03 expose `POST /api/v1/super-admin/prospect-bootstrap` qui orchestre user + AuthAccount LOCAL + workspace + membership + subscription via JPA. Bénéficie automatiquement du `@PrePersist` SF-251-02 sur `Subscription.expiresAt` → **ferme définitivement le risque NULL** qui avait piégé le compte Renversez (workspace `5d07e421-3e3c-4076-91a1-9ff8e8aaf7b8`, corrigé en migration 058 puis garde-fou backend).

```bash
# 1) Login super-admin (LOCAL ou OAuth) → cookie SESSION dans /tmp/admin-cookies.txt
curl -sS -X POST https://legalcase.fr/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -c /tmp/admin-cookies.txt \
  -d '{"email":"<SUPER_ADMIN_EMAIL>","password":"<SUPER_ADMIN_MDP>"}'

# 2) POST bootstrap (le backend hash le mdp + crée tout dans une transaction JPA)
curl -sS -X POST https://legalcase.fr/api/v1/super-admin/prospect-bootstrap \
  -H 'Content-Type: application/json' \
  -b /tmp/admin-cookies.txt \
  -d '{"firstName":"<PRENOM>","lastName":"<NOM>","email":"<EMAIL>","password":"<MDP>","country":"FRANCE","legalDomain":"DROIT_DU_TRAVAIL","workspaceName":"<NOM_CABINET>"}'

# Réponse 201 :
# {"userId":"...","workspaceId":"...","workspaceName":"<NOM_CABINET>","expiresAt":"2026-06-08T..."}

rm /tmp/admin-cookies.txt
```

**Codes de retour** :
- `201` → succès, parser `userId` / `workspaceId` / `expiresAt` pour la mémoire (étape 7)
- `409` → compte productif déjà existant (cas B) → recontacter le prospect, ne rien forcer
- `400` → payload invalide (email mal formé, password < 8, country/domaine inconnu)
- `403` → opérateur pas super-admin
- `401` → cookie expiré, refaire le login

### Étape 5 — Vérifier le login bout-en-bout

```bash
curl -sS -X POST https://legalcase.fr/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -c /tmp/cookies.txt \
  -d '{"email":"<email>","password":"<MDP>"}'

curl -sS -b /tmp/cookies.txt https://legalcase.fr/api/v1/workspaces
rm /tmp/cookies.txt
```

Le 1er retour doit être 200 + objet user. Le 2ᵉ doit lister le workspace créé avec `expiresAt` ≈ now + 14 jours (preuve que SF-251-03 a bien posé la trial).

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
Depuis SF-251-03, l'endpoint détecte automatiquement le cas A (user existant sans WorkspaceMember) et fait le reset password + création workspace dans la même transaction. Aucune procédure manuelle distincte — utiliser le même curl que ci-dessus.

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
