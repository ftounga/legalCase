import { LegalSection } from './legal-page.component';

export const MENTIONS_LEGALES: LegalSection[] = [
  {
    heading: '1. Éditeur du site',
    content: `
      <p><strong>Raison sociale :</strong> NG-CONSULTING</p>
      <p><strong>Forme juridique :</strong> Société par actions simplifiée (SAS) à associé unique</p>
      <p><strong>Capital social :</strong> 100,00 €</p>
      <p><strong>Siège social :</strong> 60 rue François 1er, 75008 Paris</p>
      <p><strong>RCS :</strong> 995 322 450 R.C.S. Paris</p>
      <p><strong>EUID :</strong> FR7501.995322450</p>
      <p><strong>Date d'immatriculation :</strong> 23 décembre 2025</p>
      <p><strong>Président :</strong> NG-ACQUISITIONS, SAS — 60 rue François Ier, 75008 Paris — RCS Paris 993 599 836</p>
      <p><strong>Email de contact :</strong> <a href="mailto:ai-legalcase@ng-itconsulting.com">ai-legalcase@ng-itconsulting.com</a></p>
    `
  },
  {
    heading: '2. Hébergement',
    content: `
      <p>Le site <strong>AI LegalCase</strong> est hébergé par :</p>
      <p><strong>Amazon Web Services EMEA SARL</strong><br>
      38 avenue John F. Kennedy, L-1855 Luxembourg<br>
      Infrastructure déployée en région <strong>eu-west-3 (Paris, France)</strong></p>
    `
  },
  {
    heading: '3. Propriété intellectuelle',
    content: `
      <p>L'ensemble des contenus présents sur le site AI LegalCase — textes, images, graphismes, logo, icônes, logiciels, code source — est la propriété exclusive de NG-CONSULTING ou de ses concédants, et est protégé par les lois françaises et internationales relatives à la propriété intellectuelle.</p>
      <p>Toute reproduction, représentation, modification, publication ou adaptation, totale ou partielle, est interdite sans autorisation écrite préalable de NG-CONSULTING.</p>
    `
  },
  {
    heading: '4. Données personnelles',
    content: `
      <p>Le traitement des données personnelles des utilisateurs est décrit dans la <a routerLink="/privacy">Politique de confidentialité</a> du site.</p>
      <p>Pour exercer vos droits (accès, rectification, effacement, portabilité, opposition), contactez-nous à : <a href="mailto:ai-legalcase@ng-itconsulting.com">ai-legalcase@ng-itconsulting.com</a></p>
    `
  },
  {
    heading: '5. Limitation de responsabilité',
    content: `
      <p>NG-CONSULTING s'efforce d'assurer l'exactitude et la mise à jour des informations diffusées sur le site, sans pouvoir garantir leur exhaustivité. La société ne saurait être tenue responsable des dommages directs ou indirects causés lors de l'accès au site.</p>
    `
  },
  {
    heading: '6. Droit applicable',
    content: `
      <p>Les présentes mentions légales sont soumises au droit français. En cas de litige, les tribunaux compétents sont ceux du ressort du Tribunal de commerce de Paris.</p>
    `
  }
];

export const POLITIQUE_CONFIDENTIALITE: LegalSection[] = [
  {
    content: `<p>La présente politique décrit comment <strong>NG-CONSULTING</strong> (60 rue François 1er, 75008 Paris — RCS Paris 995 322 450) collecte et traite vos données personnelles dans le cadre du service <strong>AI LegalCase</strong>.</p>
    <p>Contact : <a href="mailto:ai-legalcase@ng-itconsulting.com">ai-legalcase@ng-itconsulting.com</a></p>`
  },
  {
    heading: '1. Données collectées',
    content: `
      <p><strong>Données de compte :</strong> adresse email, prénom, nom, méthode d'authentification (Google, Microsoft ou locale).</p>
      <p><strong>Données de facturation :</strong> informations de paiement traitées par Stripe (nous ne stockons pas vos numéros de carte), historique des transactions.</p>
      <p><strong>Données d'utilisation :</strong> dossiers créés, documents téléversés (PDF), analyses et synthèses IA générées, notes internes, délais légaux, journaux d'activité.</p>
      <p><strong>Données techniques :</strong> adresse IP, type de navigateur, pages consultées et horodatage.</p>
    `
  },
  {
    heading: '2. Finalités et bases légales',
    content: `
      <table>
        <thead><tr><th>Finalité</th><th>Base légale (RGPD)</th></tr></thead>
        <tbody>
          <tr><td>Création et gestion de votre compte</td><td>Exécution du contrat (Art. 6.1.b)</td></tr>
          <tr><td>Fourniture du service d'analyse IA</td><td>Exécution du contrat (Art. 6.1.b)</td></tr>
          <tr><td>Gestion de la facturation et des abonnements</td><td>Exécution du contrat (Art. 6.1.b)</td></tr>
          <tr><td>Emails transactionnels (confirmation, alertes délais)</td><td>Exécution du contrat (Art. 6.1.b)</td></tr>
          <tr><td>Emails d'onboarding et de conversion</td><td>Intérêt légitime (Art. 6.1.f)</td></tr>
          <tr><td>Sécurité et prévention des fraudes</td><td>Intérêt légitime (Art. 6.1.f)</td></tr>
          <tr><td>Obligations légales et comptables</td><td>Obligation légale (Art. 6.1.c)</td></tr>
        </tbody>
      </table>
    `
  },
  {
    heading: '3. Sous-traitants',
    content: `
      <table>
        <thead><tr><th>Sous-traitant</th><th>Rôle</th><th>Localisation</th></tr></thead>
        <tbody>
          <tr><td><strong>Amazon Web Services</strong></td><td>Hébergement infrastructure</td><td>UE — Paris (eu-west-3)</td></tr>
          <tr><td><strong>Anthropic</strong></td><td>Traitement IA des documents</td><td>États-Unis — CCT UE</td></tr>
          <tr><td><strong>Brevo</strong></td><td>Envoi d'emails</td><td>France</td></tr>
          <tr><td><strong>Stripe</strong></td><td>Paiements</td><td>États-Unis — PCI-DSS / CCT UE</td></tr>
        </tbody>
      </table>
      <p>Vos documents sont transmis à Anthropic uniquement pour générer les analyses demandées. Anthropic ne les utilise pas pour entraîner ses modèles. Aucune donnée n'est vendue à des tiers.</p>
    `
  },
  {
    heading: '4. Durée de conservation',
    content: `
      <table>
        <thead><tr><th>Catégorie</th><th>Durée</th></tr></thead>
        <tbody>
          <tr><td>Données de compte</td><td>Durée de la relation + 3 ans après résiliation</td></tr>
          <tr><td>Documents et analyses</td><td>Durée de la relation — supprimés 30 jours après résiliation</td></tr>
          <tr><td>Données de facturation</td><td>10 ans (obligation légale)</td></tr>
          <tr><td>Journaux d'activité</td><td>12 mois glissants</td></tr>
          <tr><td>Données techniques (logs)</td><td>90 jours</td></tr>
        </tbody>
      </table>
    `
  },
  {
    heading: '5. Vos droits',
    content: `
      <p>Conformément au RGPD, vous disposez des droits suivants : <strong>accès</strong> (Art. 15), <strong>rectification</strong> (Art. 16), <strong>effacement</strong> (Art. 17), <strong>portabilité</strong> (Art. 20), <strong>opposition</strong> (Art. 21), <strong>limitation</strong> (Art. 18).</p>
      <p>Pour exercer ces droits : <a href="mailto:ai-legalcase@ng-itconsulting.com">ai-legalcase@ng-itconsulting.com</a> — réponse sous 30 jours.</p>
      <p>En cas de désaccord, vous pouvez saisir la <strong>CNIL</strong> (www.cnil.fr).</p>
    `
  },
  {
    heading: '6. Sécurité',
    content: `
      <ul>
        <li>Chiffrement en transit (TLS 1.2+) et au repos</li>
        <li>Authentification OAuth2/OIDC (Google, Microsoft) ou locale avec vérification email</li>
        <li>Isolation stricte des données par workspace</li>
        <li>Journalisation de toutes les actions sensibles (audit log)</li>
        <li>Accès restreint aux données de production (principe du moindre privilège)</li>
      </ul>
    `
  },
  {
    heading: '7. Modifications',
    content: `
      <p>Toute modification substantielle sera notifiée par email au moins 30 jours avant son entrée en vigueur. La poursuite de l'utilisation du service vaut acceptation.</p>
    `
  },
  {
    heading: '8. Data Processing Agreement (DPA) — Cabinets d\'avocats responsables de traitement',
    content: `
      <div class="dpa-download-section">
        <p>
          Si votre cabinet d'avocats utilise AI LegalCase pour traiter des données personnelles
          de vos clients (pièces de procédure, dossiers, documents contractuels), vous agissez
          en qualité de <strong>responsable de traitement</strong> et NG-CONSULTING en qualité de
          <strong>sous-traitant</strong> au sens de l'Article 28 du RGPD.
        </p>
        <p>
          Le Data Processing Agreement (DPA) ci-dessous régit ce traitement conformément au RGPD.
          Il couvre notamment les mesures de sécurité (TLS 1.3, chiffrement at-rest, isolation workspace),
          les sous-traitants ultérieurs (AWS eu-west-3, Anthropic, Stripe, Brevo) et vos droits
          en tant que responsable de traitement.
        </p>
        <p>
          <a href="/api/v1/legal/dpa" target="_blank" rel="noopener" class="dpa-download-link">
            ⬇ Télécharger le DPA (RGPD art. 28) — PDF
          </a>
        </p>
      </div>
    `
  }
];

export const CGU: LegalSection[] = [
  {
    heading: '1. Objet',
    content: `
      <p>Les présentes Conditions Générales d'Utilisation régissent l'accès et l'utilisation du service <strong>AI LegalCase</strong>, accessible à l'adresse <a href="https://legalcase.ng-itconsulting.com">legalcase.ng-itconsulting.com</a>, édité par <strong>NG-CONSULTING</strong> (SAS, capital 100 €, RCS Paris 995 322 450, 60 rue François 1er, 75008 Paris).</p>
      <p>En créant un compte ou en utilisant le service, l'Utilisateur accepte sans réserve les présentes CGU.</p>
    `
  },
  {
    heading: '2. Accès au Service',
    content: `
      <p>Le Service est réservé aux <strong>professionnels du droit</strong> (avocats, juristes, cabinets d'avocats) agissant dans le cadre de leur activité professionnelle.</p>
      <p>À l'ouverture de tout nouveau workspace, une <strong>période d'essai gratuite de 14 jours</strong> est accordée. À l'issue, l'accès est conditionné à la souscription d'un abonnement payant.</p>
    `
  },
  {
    heading: '3. Plans et facturation',
    content: `
      <p>Les abonnements sont facturés <strong>mensuellement par avance</strong> via Stripe. L'Utilisateur peut résilier à tout moment depuis l'interface ; la résiliation prend effet à la fin de la période en cours, sans remboursement proratisé.</p>
      <p>En cas de défaut de paiement, l'accès est suspendu après 7 jours puis résilié après 30 jours supplémentaires.</p>
      <p>Les crédits tokens top-up sont <strong>non remboursables et non transférables</strong>.</p>
    `
  },
  {
    heading: '4. Utilisation autorisée et interdite',
    content: `
      <p>L'Utilisateur s'engage à utiliser le Service dans le respect des lois, du secret professionnel et du RGPD concernant les données des tiers dans les documents téléversés.</p>
      <p>Sont <strong>strictement interdits</strong> : l'utilisation à des fins illégales, le contournement des mécanismes de sécurité, la revente du Service, l'utilisation de bots ou scrapers, la décompilation du code source.</p>
    `
  },
  {
    heading: '5. Avertissement IA — Limitation de responsabilité',
    content: `
      <p><strong>AI LegalCase est un outil d'assistance. Il ne constitue pas un conseil juridique et ne remplace pas le jugement professionnel d'un avocat.</strong></p>
      <p>Les synthèses et analyses générées par l'IA sont des productions automatisées susceptibles de contenir des erreurs. Elles doivent être vérifiées par un professionnel qualifié avant toute utilisation.</p>
      <p>La responsabilité de NG-CONSULTING est limitée aux sommes effectivement payées par l'Utilisateur au cours des 12 mois précédant le fait générateur du dommage.</p>
    `
  },
  {
    heading: '6. Propriété intellectuelle',
    content: `
      <p>Le Service et ses composants sont la propriété exclusive de NG-CONSULTING. L'Utilisateur conserve tous ses droits sur les documents téléversés et les synthèses générées depuis ses documents.</p>
    `
  },
  {
    heading: '7. Résiliation',
    content: `
      <p>L'Utilisateur peut supprimer son compte à tout moment depuis les paramètres ou en contactant <a href="mailto:ai-legalcase@ng-itconsulting.com">ai-legalcase@ng-itconsulting.com</a>. Les données sont conservées 30 jours puis supprimées définitivement.</p>
      <p>NG-CONSULTING peut suspendre ou résilier l'accès sans préavis en cas de violation des CGU, non-paiement ou comportement frauduleux.</p>
    `
  },
  {
    heading: '8. Modification des CGU',
    content: `
      <p>Toute modification substantielle est notifiée par email au moins <strong>30 jours avant son entrée en vigueur</strong>. La poursuite de l'utilisation vaut acceptation.</p>
    `
  },
  {
    heading: '9. Droit applicable et juridiction',
    content: `
      <p>Les présentes CGU sont soumises au <strong>droit français</strong>. Tout litige non résolu à l'amiable sous 30 jours sera porté devant les <strong>tribunaux du ressort du Tribunal de commerce de Paris</strong>.</p>
      <p>Contact : <a href="mailto:ai-legalcase@ng-itconsulting.com">ai-legalcase@ng-itconsulting.com</a></p>
    `
  }
];
