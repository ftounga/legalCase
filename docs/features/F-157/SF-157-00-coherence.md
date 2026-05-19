# F-157 — Cadrage cohérence (étape 0)

> Skill : `ai-skills/feature-coherence-challenger.md`
> Date : 2026-05-19
> Feature : F-157 — « Reset password — audit visibilité + complétion du flow F-26 »
> Statut PRODUCT_SPEC avant cadrage : V8+ — « À planifier » (section « Features hors V1 (backlog) »)

## Verdict : STOP — feature sans objet (le flow F-26 est déjà complet et découvrable)

Le périmètre de F-157 est explicitement défini par sa propre ligne PRODUCT_SPEC comme un
**audit conditionnel** : « **Si manquant** : ajouter le lien login, fixer l'email, fixer la
page reset. ». L'audit de l'existant — étape obligatoire imposée par la spec elle-même — a
été mené de bout en bout (backend + frontend). **Aucun des 5 points d'audit ne révèle de
manque.** La clause « si manquant » n'est jamais déclenchée → il n'y a rien à coder. F-157
est un audit dont le résultat est « rien à faire ». STOP au sens gouvernance : on n'ouvre
pas de SF de dev, on ne crée pas de branche, on documente le constat.

Ce n'est pas un STOP « trou amont bloquant » : c'est un STOP « la complétion supposée
n'existe pas — le travail a déjà été livré par F-26 (SF-26-04 + SF-26-05) ».

## Intention métier (1 phrase)

Permettre à un avocat ayant créé son compte LegalCase avec un mot de passe local (auth F-26,
non-OAuth) et l'ayant oublié de retrouver l'accès à son espace de manière autonome, sans
contacter le support.

## Applicabilité du reset password vis-à-vis de l'architecture auth

Rappel `CLAUDE.md` : « L'auth V1 repose sur Spring Security + OAuth2/OIDC avec Google et
Microsoft — aucun mot de passe local ». Cette règle décrit l'auth **V1**. Or **F-26 « Auth
locale (email/mot de passe) »** est inscrite au PRODUCT_SPEC en statut **`Terminée`**
(mergée 2026-03-21, bloc 10) et introduit explicitement un mécanisme de mot de passe local
**coexistant** avec OAuth2 (migration 022 : colonne `password_hash` sur `auth_accounts`,
BCrypt, provider `LOCAL`).

→ Le reset password **est applicable** : il existe une population réelle de comptes `LOCAL`
avec mot de passe. Le cas « 100 % OAuth, zéro mot de passe → reset sans objet » envisagé
dans le brief **ne s'applique pas** : F-26 a introduit l'auth locale et est terminée. Le
STAOP de F-157 ne vient donc PAS d'une non-applicabilité du reset password, mais du fait que
le flow de reset **est déjà entièrement livré**.

## Workflow métier réel de l'utilisateur cible (avocat)

Source : signal terrain documenté dans la ligne F-157 du PRODUCT_SPEC (« on l'a oublié,
pourtant c'est un classique », 2026-04-25) + pratique standard d'un flow « mot de passe
oublié » SaaS. Étapes ⚠ hypothèse là où non sourcé directement.

1. L'avocat a créé son compte LegalCase avec email + mot de passe (auth locale F-26).
2. Quelque temps plus tard, il revient sur `https://legalcase.fr` et veut se connecter.
3. Il a oublié son mot de passe. ⚠ hypothèse : cas classique SaaS.
4. Il cherche sur l'écran de login un point d'entrée « Mot de passe oublié ? ».
5. Il clique, saisit son email, demande un lien de réinitialisation.
6. Le système envoie un email transactionnel contenant un lien `/reset-password?token=…`.
7. L'avocat ouvre sa boîte mail, clique le lien.
8. Une page publique s'ouvre, lui demande de choisir un nouveau mot de passe (+ confirmation).
9. Il valide ; le système met à jour le hash et invalide le token.
10. Il est invité à se reconnecter avec le nouveau mot de passe.
11. Il se reconnecte et reprend son travail sur ses dossiers.

Cas d'erreur métier attendus : email inexistant (réponse neutre anti-énumération), token
expiré (> 24 h), token déjà utilisé.

## Cartographie features actuelles ↔ workflow

| Étape workflow métier | Feature / brique LegalCase | Statut |
|---|---|---|
| 1. Compte avec mot de passe local | F-26 SF-26-01/02 (`password_hash`, `register`, BCrypt) | ✅ Livrée |
| 2-3. Retour sur le login, mot de passe oublié | F-105 LoginComponent V2 split-screen | ✅ Livrée |
| 4. Point d'entrée « Mot de passe oublié ? » visible | F-26 SF-26-05 — `LoginComponent` bloc `forgot-section` | ✅ Livrée |
| 5. Saisie email + demande de lien | F-26 SF-26-05 — `forgotForm` inline + `AuthService.forgotPassword` | ✅ Livrée |
| 6. Email transactionnel avec lien reset | F-26 SF-26-04 — `EmailService.sendPasswordReset` | ✅ Livrée |
| 7-8. Page publique `/reset-password?token=…` | F-26 SF-26-05 — route publique + `ResetPasswordComponent` | ✅ Livrée |
| 9. Mise à jour hash + invalidation token | F-26 SF-26-04 — `LocalAuthService.resetPassword` | ✅ Livrée |
| 10-11. Retour login + reconnexion | F-26 SF-26-05 — écran succès + lien `/login` | ✅ Livrée |

**Aucune étape ❌ manquante. Aucune étape 🟡 backlog.** L'intégralité du workflow métier de
F-157 est couverte par des briques **livrées**.

## Position de la nouvelle feature

F-157 ne s'insère à aucune étape nouvelle du workflow : elle est un **audit transverse** de
l'étape 4 à l'étape 11 déjà couvertes par F-26. Sa raison d'être (« le flow ne semble pas
découvrable / fonctionnel ») est une hypothèse à vérifier, pas un trou identifié.

## Résultat détaillé de l'audit (les 5 points imposés par la ligne F-157)

| # | Point d'audit imposé par PRODUCT_SPEC | Constat | Verdict |
|---|---|---|---|
| 1 | Lien « Mot de passe oublié ? » présent sur `LoginComponent` (V2 split-screen F-105) | `frontend/src/app/auth/login/login.component.html` l.84-110 : bouton `Mot de passe oublié ?` → `toggleForgot()` → formulaire email inline + état succès. Logique dans `login.component.ts` l.110-130 (`submitForgot`, `toggleForgot`). | ✅ Présent |
| 2 | Email transactionnel `forgot-password` envoyé | `LocalAuthService.forgotPassword` (l.122-138) génère un `PasswordResetToken` (24 h) et appelle `EmailService.sendPasswordReset` (`EmailService.java` l.77-103) : objet, lien `frontendUrl + /reset-password?token=…`, expiration 24 h, mention « ignorez cet email ». Envoi `mailEnabled`-gated, **fail-open** (l.134-136 du service : exception loggée, demande conservée). Email **transactionnel** (n'appelle pas `unsubscribeFooter`, pas de gate `marketingEmailsOptedOut`). | ✅ Présent — branché sur le `JavaMailSender` (SMTP configuré par profil). Test bout-en-bout staging/prod = ops, hors code. |
| 3 | Page `/reset-password?token=XXX` charge et soumet | Route publique `app.routes.ts` l.155-158 (aucun `canActivate`). `ResetPasswordComponent` lit le `token` en query param (`reset-password.component.ts` l.49), formulaire `newPassword` + `confirmPassword` avec validateur `passwordsMatch`, `minlength(8)`, `maxlength(72)` ; soumet via `AuthService.resetPassword` → `POST /api/v1/auth/reset-password`. | ✅ Présent |
| 4 | Expérience après reset (retour login + confirmation) | `reset-password.component.html` l.6-12 : état `success()` affiche une carte « Mot de passe réinitialisé ! » avec icône `lock_reset` et bouton `routerLink="/login"`. NB : implémentation = écran de succès in-place (pas snackbar + redirection auto), ce qui est **conforme et équivalent** au besoin métier (utilisateur informé + chemin clair vers la reconnexion). | ✅ Présent |
| 5 | Cas d'erreur (token expiré, déjà utilisé, email inexistant) | Backend : `resetPassword` (`LocalAuthService` l.142-158) rejette token introuvable, `usedAt != null` (déjà utilisé), `expiresAt` dépassé. `forgotPassword` est **fail-silent** sur email inexistant (réponse neutre anti-énumération, message « Si un compte existe… »). Frontend : `reset-password.component.ts` l.61-64 affiche `err.error?.message ?? 'Ce lien est invalide ou expiré.'`. Couvert par `ForgotResetPasswordControllerIT` (9 tests) + `ForgotResetPasswordServiceTest` (7 tests) + `reset-password.component.spec.ts` (6 tests) + `login.component.spec.ts` (12 tests). | ✅ Présent |

Vérifications transverses complémentaires :
- Endpoints backend publics : `SecurityConfig.java` l.59-60 — `/api/v1/auth/forgot-password`
  et `/api/v1/auth/reset-password` sont `permitAll()`. ✅
- Endpoints consommés par le frontend (`AuthService.forgotPassword` / `resetPassword`)
  correspondent exactement aux endpoints exposés par `LocalAuthController`. ✅

## Challenge amont

Toutes les briques amont (compte local F-26, écran login F-105, génération de token,
service email) sont **livrées**. Aucun trou amont. Le reset password a tout ce qu'il lui
faut pour fonctionner — et fonctionne déjà.

## Challenge aval

L'étape aval (reconnexion avec le nouveau mot de passe) est couverte par `LocalAuthController.login`
+ `LoginComponent`, livrés. Le token de reset est invalidé après usage (`usedAt`), évitant
le rejeu. Aucun trou aval.

## STOPs / pré-requis à ajouter au backlog

Aucun. F-157 n'a besoin d'aucune feature pré-requise : ses pré-requis sont tous livrés, et
sa propre cible (le flow reset) est livrée.

## Pourquoi le signal terrain « on l'a oublié » ne contredit pas ce constat

La ligne F-157 cite un signal terrain du 2026-04-25 (« on l'a oublié, pourtant c'est un
classique »). L'audit montre que ce signal était une **crainte non vérifiée**, pas un bug
constaté : le flow a bien été livré par F-26 SF-26-04 et SF-26-05 (2026-03-21), soit
**avant** le signal. F-157 a justement été conçue comme un audit *pour lever le doute* —
et l'audit conclut que le doute n'était pas fondé côté code.

Le seul résidu légitime de F-157 est **opérationnel, pas applicatif** : un test bout-en-bout
manuel en staging et en prod (l'email transactionnel `sendPasswordReset` part-il réellement
et arrive-t-il en boîte, sans être classé spam ?). Ce test :
- ne produit aucune ligne de code (le code d'envoi existe et est correct) ;
- relève de l'exploitation / QA manuelle, pas du cycle dev SF ;
- est tributaire de la configuration SMTP par environnement (`app.mail.enabled`,
  `spring.mail.*`), hors du périmètre d'un dev de feature.

Il est donc remonté comme **action ops** dans le verdict, sans ouverture de SF.

## Invariants anti-gadget pour la mini-spec

Sans objet — aucune mini-spec n'est produite (STOP). Pour mémoire, si un manque réel avait
été trouvé, les invariants auraient été : (a) le lien reset doit rester visible sans
interaction préalable sur le login ; (b) l'email reset doit rester transactionnel (jamais
soumis à l'opt-out marketing) ; (c) le token doit être à usage unique et expirer à 24 h.

## Décision finale

**STOP.** F-157 ne donne lieu à aucune subfeature de développement. Le flow de
réinitialisation de mot de passe est intégralement implémenté, testé, public et découvrable
depuis l'écran de login. La clause « Si manquant » de la ligne PRODUCT_SPEC n'est pas
déclenchée.

Actions recommandées (hors cycle dev) :
1. Mettre à jour la ligne F-157 de `docs/PRODUCT_SPEC.md` : statut « Backlog / À planifier »
   → **« Terminée (audit) — flow déjà couvert par F-26, aucune correction nécessaire »**, en
   citant le présent document. (À faire par le Product Owner / orchestrateur, pas par cet
   agent — l'agent ne modifie pas PRODUCT_SPEC sans validation, et aucune PR de code n'est
   produite.)
2. Programmer un test manuel bout-en-bout de l'email `forgot-password` en staging puis en
   prod (action ops/QA — délivrabilité Brevo/SMTP, classement spam). Si la délivrabilité
   échoue, cela relèvera d'une tâche ops de configuration SMTP, pas d'un dev applicatif.

Aucune branche n'est créée, aucune PR n'est ouverte.
