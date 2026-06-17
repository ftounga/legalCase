# M-83 — Sécurité & confidentialité (one-pager cabinets) + DPA

> Tue l'objection n°1 des avocats (confidentialité / secret pro). Débloque la vente **jusqu'à ~50 avocats sans ISO**. **Factuel — jamais « certifié ».**
> ⚠️ Items **[À VÉRIFIER]** : à confirmer AVANT toute diffusion — un claim faux avec des avocats est pire que pas de claim.

## One-pager (à remettre / joindre)
- **Hébergement** : souverain, AWS région **Paris (UE)** — données hébergées en France/UE.
- **Authentification** : OAuth2/OIDC (Google, Microsoft) **ou** compte local email/mot de passe (**haché BCrypt**, jamais en clair). MFA héritée du fournisseur d'identité pour les comptes SSO.
- **Jetons (validation email / réinitialisation mot de passe)** : à **expiration** et **usage unique**.
- **Chiffrement** : en transit (**TLS**) ; **au repos activé** (base RDS + object storage S3).
- **Cloisonnement** : architecture multi-tenant, **isolation par `workspace_id`** — données cloisonnées entre cabinets.
- **Contrôle d'accès** : moindre privilège, accès restreint. [À VÉRIFIER]
- **Sauvegardes** : RDS, **rétention 7 jours** (production) ; `deletion_protection` activée en prod.
- **Sous-processeurs** : hébergeur AWS (Paris) ; fournisseur(s) de modèles pour l'analyse — listés dans la FAQ confidentialité. [À VÉRIFIER : liste à jour]
- **RGPD** : conforme RGPD, registre des traitements, **DPA fourni sur demande**.
- **Conservation / suppression** : documents conservés (transition stockage à 90 j) ; **suppression sur demande** ; anciennes versions purgées à 365 j.
- **Secret professionnel** : les données du cabinet restent sous son contrôle ; l'outil sert le jugement de l'avocat.

## Le DPA (accord de traitement — art. 28 RGPD)
Document contractuel encadrant le traitement par LegalCase en tant que sous-traitant. **À fournir signable sur demande.** [À FAIRE : finaliser un DPA-type via le prestataire RGPD M-83.]

## Réponses-type au questionnaire de sécurité
- Hébergement ? → AWS Paris (UE).
- Chiffrement ? → TLS en transit ; **chiffrement au repos (RDS + S3)**.
- Accès ? → restreint, moindre privilège.
- Sous-traitants ? → AWS + fournisseur(s) modèles (FAQ).
- Suppression ? → sur demande.
- Authentification ? → SSO Google/Microsoft (OAuth2/OIDC) **ou** compte local (mot de passe **haché BCrypt**).
- Certifications ? → **conforme RGPD ; pas d'ISO à ce stade** (factuel).
- DPA ? → fourni sur demande.

## Règle de communication
- ✅ « RGPD-conforme, hébergement souverain AWS Paris, DPA fourni » (factuel).
- ❌ « Certifié ISO / SecNumCloud » **INTERDIT** tant que non obtenu.
