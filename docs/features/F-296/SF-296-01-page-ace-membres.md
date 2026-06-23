# SF-296-01 — Landing partenaire ACE + formulaire de contact

## Objectif (1 phrase)
Offrir aux adhérents ACE une page publique soignée `legalcase.fr/ace` qui présente l'offre LegalCase et permet de nous contacter via un formulaire dont le message arrive dans la boîte de l'équipe (Franck).

## Rattachement
Cette SF couvre la **partie (b)** de **F-296** : « créer une page `/ace` membres ». La **partie (a)** (mettre en avant la « vue 360 » comme différenciateur sur la landing principale) est **hors périmètre** et fera l'objet de **SF-296-02**.

## Contexte
La V3 de l'encart inséré dans la plaquette ACE 2026 doit pointer son bouton CTA vers une page d'atterrissage dédiée (comme les autres partenaires de la plaquette), et non vers la home générique. Cette page sert de point de conversion unique pour le code adhérent `ACE2026`. Invariants F-296 respectés : CSS scopé (`Emulated` + `.ace-`), catalogue landing non touché, pas de marque tierce, vérif visuelle staging avant prod.

## Comportement nominal
1. Un adhérent ACE clique sur le bouton de l'encart → arrive sur `legalcase.fr/ace`.
2. La page présente : hero ACE × LegalCase, les 5 piliers produit, le bandeau offre (30 jours d'essai + code `ACE2026`), un formulaire de contact, un pied de page.
3. Il remplit le formulaire (Nom, Email, Téléphone optionnel, Message) et envoie.
4. `POST /api/v1/contact` (endpoint EXISTANT, réutilisé) avec `sujet = "Partenariat ACE 2026"`.
5. Backend : `sendContactToTeam()` (mail vers `CONTACT_TEAM_EMAIL`, `reply-to` = email du visiteur) + `sendContactConfirmation()` (accusé au visiteur).
6. La page affiche un message de succès et masque le formulaire.

## Cas d'erreur
- **Champs invalides** (nom vide, email mal formé) → bouton désactivé + messages de validation Angular, pas d'appel réseau.
- **Échec réseau / 5xx** → snackbar d'erreur « Envoi impossible, réessayez », le formulaire reste rempli (pas de perte de saisie).
- **Téléphone** : champ optionnel ; s'il est vide, il est OMIS du payload (envoyé `undefined`) pour ne pas heurter le `@Pattern` backend (qui rejette une chaîne vide mais accepte `null`).

## Critères d'acceptation (vérifiables)
- [ ] `GET /ace` renvoie la page (route publique, sans auth) et est prérendue (SSG) — présente dans `prerender-routes.txt`.
- [ ] La page utilise `ViewEncapsulation.Emulated` et toutes ses classes sont préfixées `.ace-` (aucune classe générique globale type `.hero`).
- [ ] Métadonnées SEO : `<title>`, meta description, canonical `https://legalcase.fr/ace`.
- [ ] Le formulaire valide nom (requis), email (requis + format) ; bouton désactivé tant qu'invalide ou envoi en cours.
- [ ] Un envoi réussi appelle `POST /api/v1/contact` avec `sujet = "Partenariat ACE 2026"` et affiche l'état de succès.
- [ ] Aucune régression visuelle sur les autres écrans (pas de fuite CSS) — vérifié par grep des classes + smoke E2E.
- [ ] Le code `ACE2026` est affiché de façon visible dans le bandeau offre.

## Plan de test minimal
- **Unitaires (frontend, `ace-landing.component.spec.ts`)** :
  - le composant se crée ;
  - formulaire invalide → `submit()` n'appelle pas le service ;
  - formulaire valide → appelle `ContactService.send()` avec `sujet = "Partenariat ACE 2026"` et téléphone omis si vide ;
  - succès → `sent === true` ; erreur → snackbar et `sending === false`.
- **Intégration** : réutilise le `ContactController` déjà couvert (aucun nouvel endpoint) — pas de nouveau test backend.
- **Isolation workspace** : N/A (page publique non authentifiée, aucune donnée tenant).
- **Non-régression CSS** : `grep` confirmant l'absence de classes génériques non préfixées ; smoke E2E `cd e2e && npm test` (préoccupation transversale routing).

## Tables / endpoints / composants impactés
- **Endpoints** : `POST /api/v1/contact` (EXISTANT, réutilisé tel quel — 0 modification backend).
- **Tables** : aucune (0 migration).
- **Composants créés** :
  - `frontend/src/app/ace-landing/ace-landing.component.{ts,html,scss}` (standalone, Emulated)
  - `frontend/src/app/ace-landing/ace-landing.component.spec.ts`
- **Composants/fichiers modifiés** :
  - `frontend/src/app/app.routes.ts` (+ route `/ace`, lazy, sans guard)
  - `frontend/prerender-routes.txt` (+ `/ace`)
  - réutilise `frontend/src/app/contact/contact.service.ts` (`ContactService`, inchangé)

## Préoccupation transversale
- **Navigation / routing** : nouvelle route publique `/ace`. Composants impactés listés ci-dessus. Vérification : route déclarée sans `authGuard`, ajout au prerender, smoke E2E avant push.

## Hors périmètre
- Pas de page `/employeur`-like multi-section complexe ni A/B test.
- Pas de nouvel endpoint backend ni de champ « cabinet/barreau » structuré (replié dans le message si besoin).
- Pas de rate-limit / captcha (dette connue commune à `/contact` ; à traiter globalement si spam constaté, pas dans cette SF).
- Pas de tracking analytics custom au-delà des UTM déjà portés par le lien de l'encart.
- Le branchement final du bouton de l'encart V3 sur `https://legalcase.fr/ace` se fait après mise en prod de la page (étape de suivi, pas dans le code de cette SF).
