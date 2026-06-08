import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  PLATFORM_ID,
} from '@angular/core';
import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Title, Meta } from '@angular/platform-browser';

/**
 * F-DRH-01 SF-DRH-01-01 — Page publique « LegalCase Employeur ».
 *
 * Page marketing d'amorce du pivot DRH/employeur : présente l'offre côté
 * employeur (chiffrer l'exposition prud'homale, sécuriser la procédure) et
 * oriente le prospect vers une démo Calendly. Pas de prix, pas d'inscription
 * self-serve. Encapsulation par défaut (Emulated) + classes préfixées `emp-`
 * (double sécurité anti-collision avec la landing en ViewEncapsulation.None).
 */

/** Capacité réelle du moteur, exposée dans la section valeur. */
export interface EmployerValueItem {
  icon: string;
  title: string;
  text: string;
}

@Component({
  selector: 'app-employer-landing',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './employer-landing.component.html',
  styleUrl: './employer-landing.component.scss',
})
export class EmployerLandingComponent implements OnInit, OnDestroy {
  private title = inject(Title);
  private meta = inject(Meta);
  private document = inject(DOCUMENT);
  private platformId = inject(PLATFORM_ID);
  private jsonLdElement: HTMLScriptElement | null = null;

  readonly currentYear = new Date().getFullYear();

  /** Créneau démo 30 min — audience employeur/DRH (distinct de /demos avocat). */
  readonly calendlyUrl =
    'https://calendly.com/tounga-franck-ng-itconsulting/30min';

  /** Les 4 capacités RÉELLES du moteur (cf. cadrage — pas d'écran DRH-natif). */
  readonly valueItems: readonly EmployerValueItem[] = [
    {
      icon: '⚖',
      title: 'Qualification du litige',
      text: "À partir des pièces du dossier, LegalCase qualifie la situation et identifie le motif de rupture en jeu.",
    },
    {
      icon: '🛡',
      title: 'Détection des vices de procédure',
      text: "Entretien préalable, motivation, délais : les points de fragilité procédurale sont signalés avant la décision.",
    },
    {
      icon: '€',
      title: "Chiffrage de l'exposition",
      text: "L'exposition prud'homale est estimée selon le barème Macron, pour décider avec une fourchette chiffrée.",
    },
    {
      icon: '⏱',
      title: 'Signalement des délais',
      text: 'Les délais légaux pertinents sont mis en évidence pour ne pas engager une procédure hors des temps.',
    },
  ];

  /** Piliers de confiance affichés dans la section conformité. */
  readonly trustItems: readonly string[] = [
    'Hébergement en Union Européenne',
    'Conforme au RGPD',
    'Données isolées par organisation',
  ];

  ngOnInit(): void {
    const pageTitle =
      'LegalCase Employeur — chiffrez l\'exposition prud\'homale avant de décider';
    const description =
      "LegalCase Employeur : à partir des pièces du dossier, qualifiez le litige, détectez les vices de procédure, chiffrez l'exposition prud'homale (barème Macron) et sécurisez vos décisions RH. Maîtrise du risque et conformité. Réservez une démo.";
    const url = 'https://legalcase.fr/employeur';

    this.title.setTitle(pageTitle);
    this.meta.updateTag({ name: 'description', content: description });

    this.meta.updateTag({ property: 'og:title', content: pageTitle });
    this.meta.updateTag({ property: 'og:description', content: description });
    this.meta.updateTag({ property: 'og:url', content: url });
    this.meta.updateTag({ property: 'og:type', content: 'website' });
    this.meta.updateTag({ property: 'og:site_name', content: 'AI LegalCase' });
    this.meta.updateTag({ property: 'og:locale', content: 'fr_FR' });

    this.meta.updateTag({ name: 'twitter:card', content: 'summary_large_image' });
    this.meta.updateTag({ name: 'twitter:title', content: pageTitle });
    this.meta.updateTag({ name: 'twitter:description', content: description });

    this.upsertCanonical(url);
    this.injectJsonLd(pageTitle, description, url);
  }

  ngOnDestroy(): void {
    if (this.jsonLdElement) {
      this.jsonLdElement.remove();
      this.jsonLdElement = null;
    }
  }

  private upsertCanonical(href: string): void {
    if (!isPlatformBrowser(this.platformId) && !this.document) return;
    let link = this.document.querySelector(
      'link[rel="canonical"]',
    ) as HTMLLinkElement | null;
    if (!link) {
      link = this.document.createElement('link');
      link.rel = 'canonical';
      this.document.head.appendChild(link);
    }
    link.href = href;
  }

  private injectJsonLd(name: string, description: string, url: string): void {
    const jsonLd = {
      '@context': 'https://schema.org',
      '@type': 'WebPage',
      name,
      description,
      url,
      breadcrumb: {
        '@type': 'BreadcrumbList',
        itemListElement: [
          {
            '@type': 'ListItem',
            position: 1,
            name: 'LegalCase',
            item: 'https://legalcase.fr/',
          },
          { '@type': 'ListItem', position: 2, name: 'Employeur', item: url },
        ],
      },
    };
    this.jsonLdElement = this.document.createElement('script');
    this.jsonLdElement.type = 'application/ld+json';
    this.jsonLdElement.textContent = JSON.stringify(jsonLd);
    this.document.head.appendChild(this.jsonLdElement);
  }
}
