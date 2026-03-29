# Politique de confidentialité

*Dernière mise à jour : mars 2026*

---

## 1. Responsable du traitement

Les données personnelles collectées via le service **AI LegalCase** sont traitées par :

**NG-CONSULTING**
60 rue François 1er, 75008 Paris
RCS Paris : 995 322 450
Email : ai-legalcase@ng-itconsulting.com

---

## 2. Données collectées

### 2.1 Données de compte
Lors de la création d'un compte, nous collectons :
- Adresse email
- Prénom et nom de famille
- Méthode d'authentification utilisée (Google, Microsoft ou inscription locale)

### 2.2 Données de facturation
En cas de souscription à un plan payant :
- Informations de paiement (traitées directement par Stripe — nous ne stockons pas vos numéros de carte)
- Historique des transactions

### 2.3 Données d'utilisation du service
- Dossiers juridiques créés (titres, domaines de droit)
- Documents téléversés (fichiers PDF)
- Analyses IA générées, synthèses, questions et réponses
- Notes internes et délais légaux saisis
- Journaux d'activité (actions sur les dossiers, connexions)

### 2.4 Données techniques
- Adresse IP
- Type de navigateur et système d'exploitation
- Pages consultées et horodatage

---

## 3. Finalités et bases légales du traitement

| Finalité | Base légale (RGPD) |
|----------|-------------------|
| Création et gestion de votre compte | Exécution du contrat (Art. 6.1.b) |
| Fourniture du service d'analyse IA | Exécution du contrat (Art. 6.1.b) |
| Gestion de la facturation et des abonnements | Exécution du contrat (Art. 6.1.b) |
| Envoi d'emails transactionnels (confirmation, alerte délais, analyse terminée) | Exécution du contrat (Art. 6.1.b) |
| Envoi d'emails d'onboarding et de conversion | Intérêt légitime (Art. 6.1.f) |
| Sécurité du service et prévention des fraudes | Intérêt légitime (Art. 6.1.f) |
| Obligations légales et comptables | Obligation légale (Art. 6.1.c) |
| Amélioration du service (données agrégées anonymisées) | Intérêt légitime (Art. 6.1.f) |

---

## 4. Destinataires des données — sous-traitants

Nous faisons appel aux sous-traitants suivants pour opérer le service :

| Sous-traitant | Rôle | Localisation | Garanties |
|---------------|------|-------------|-----------|
| **Amazon Web Services** | Hébergement de l'infrastructure (serveurs, base de données, stockage fichiers) | Union Européenne — Paris (eu-west-3) | Clauses contractuelles types UE |
| **Anthropic** | Traitement IA des documents (analyse, synthèse) | États-Unis | Clauses contractuelles types UE — les données transmises sont limitées au contenu des documents analysés |
| **Brevo (Sendinblue)** | Envoi des emails transactionnels et d'onboarding | France | Conformité RGPD |
| **Stripe** | Traitement des paiements | États-Unis | Certifié PCI-DSS — clauses contractuelles types UE |

Vos documents juridiques sont transmis à Anthropic uniquement pour générer les analyses demandées. Anthropic s'engage contractuellement à ne pas utiliser ces données pour entraîner ses modèles d'IA sans consentement explicite.

Aucune donnée personnelle n'est vendue à des tiers.

---

## 5. Transferts de données hors Union Européenne

Certains sous-traitants (Anthropic, Stripe) sont établis aux États-Unis. Ces transferts sont encadrés par des **Clauses Contractuelles Types (CCT)** approuvées par la Commission européenne, conformément à l'article 46 du RGPD.

---

## 6. Durée de conservation

| Catégorie de données | Durée de conservation |
|---------------------|----------------------|
| Données de compte | Durée de la relation contractuelle + 3 ans après résiliation |
| Documents et analyses | Durée de la relation contractuelle — supprimés à la demande ou 30 jours après résiliation |
| Données de facturation | 10 ans (obligation comptable légale) |
| Journaux d'activité (audit logs) | 12 mois glissants |
| Emails envoyés (table email_sends) | 3 ans |
| Données techniques (logs) | 90 jours |

---

## 7. Vos droits

Conformément au RGPD, vous disposez des droits suivants concernant vos données personnelles :

- **Droit d'accès** (Art. 15) : obtenir une copie de vos données
- **Droit de rectification** (Art. 16) : corriger vos données inexactes
- **Droit à l'effacement** (Art. 17) : demander la suppression de vos données
- **Droit à la portabilité** (Art. 20) : recevoir vos données dans un format structuré
- **Droit d'opposition** (Art. 21) : vous opposer au traitement pour des raisons légitimes
- **Droit de limitation** (Art. 18) : restreindre le traitement dans certains cas
- **Droit de retrait du consentement** : à tout moment pour les traitements basés sur le consentement

**Pour exercer vos droits :** ai-legalcase@ng-itconsulting.com

Nous répondrons à votre demande dans un délai de **30 jours**. En cas de désaccord sur la réponse apportée, vous pouvez saisir la **CNIL** (Commission Nationale de l'Informatique et des Libertés) — www.cnil.fr.

---

## 8. Sécurité

NG-CONSULTING met en œuvre les mesures techniques et organisationnelles suivantes pour protéger vos données :

- Chiffrement des données en transit (TLS 1.2+)
- Chiffrement des données au repos
- Authentification sécurisée (OAuth2/OIDC via Google et Microsoft, ou authentification locale avec vérification email)
- Isolation stricte des données par workspace (aucune fuite inter-client)
- Journalisation de toutes les actions sensibles (audit log)
- Accès restreint aux données de production (principe du moindre privilège)

---

## 9. Cookies

### Cookies strictement nécessaires (pas de consentement requis)
- **Session** : maintien de votre authentification
- **Sécurité** : protection CSRF

### Cookies de performance (avec consentement)
Si vous acceptez le suivi analytique, nous pouvons utiliser des cookies pour mesurer l'utilisation du service (pages visitées, temps passé) à des fins d'amélioration produit.

Vous pouvez gérer vos préférences cookies à tout moment depuis les paramètres de votre navigateur.

---

## 10. Modifications de la politique

Nous nous réservons le droit de modifier la présente politique à tout moment. En cas de modification substantielle, vous serez informé(e) par email au moins 30 jours avant l'entrée en vigueur des nouvelles dispositions.

La poursuite de l'utilisation du service après ce délai vaut acceptation de la politique mise à jour.

---

*Pour toute question : ai-legalcase@ng-itconsulting.com*
