import { Component, AfterViewInit, ViewEncapsulation, inject, OnInit, OnDestroy, PLATFORM_ID } from '@angular/core';
import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Title, Meta } from '@angular/platform-browser';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss',
  encapsulation: ViewEncapsulation.None
})
export class LandingComponent implements OnInit, AfterViewInit, OnDestroy {
  private title = inject(Title);
  private meta = inject(Meta);
  private platformId = inject(PLATFORM_ID);
  private document = inject(DOCUMENT);
  private jsonLdElement: HTMLScriptElement | null = null;

  ngOnInit(): void {
    this.title.setTitle('AI LegalCase — Analyse IA pour avocats en droit du travail');
    this.meta.updateTag({ name: 'description', content: 'Outil IA pour avocats : analysez vos dossiers contentieux en droit du travail en quelques minutes. Synthèse structurée, risques, chronologie, points de droit. Conçu pour les cabinets de 1 à 10 avocats. Essai gratuit 14 jours.' });
    this.meta.updateTag({ property: 'og:title', content: 'AI LegalCase — Analyse IA pour avocats en droit du travail' });
    this.meta.updateTag({ property: 'og:description', content: 'Outil IA pour avocats : analysez vos dossiers contentieux en droit du travail en quelques minutes. Synthèse structurée, risques, chronologie, points de droit. Essai gratuit 14 jours.' });
    this.meta.updateTag({ property: 'og:url', content: 'https://legalcase.ng-itconsulting.com/' });
    this.injectJsonLd();
  }

  ngOnDestroy(): void {
    if (this.jsonLdElement) {
      this.jsonLdElement.remove();
      this.jsonLdElement = null;
    }
  }

  private injectJsonLd(): void {
    const jsonLd = {
      '@context': 'https://schema.org',
      '@type': 'SoftwareApplication',
      'name': 'AI LegalCase',
      'description': 'Outil d\'analyse IA pour avocats. Analysez vos dossiers contentieux en droit du travail : synthèse structurée, chronologie, risques juridiques, points de droit.',
      'applicationCategory': 'BusinessApplication',
      'operatingSystem': 'Web',
      'url': 'https://legalcase.ng-itconsulting.com',
      'offers': {
        '@type': 'Offer',
        'price': '0',
        'priceCurrency': 'EUR',
        'description': 'Essai gratuit 14 jours',
      },
      'publisher': {
        '@type': 'Organization',
        'name': 'NG-Consulting',
        'address': {
          '@type': 'PostalAddress',
          'addressLocality': 'Paris',
          'addressCountry': 'FR',
        },
      },
    };
    this.jsonLdElement = this.document.createElement('script');
    this.jsonLdElement.type = 'application/ld+json';
    this.jsonLdElement.textContent = JSON.stringify(jsonLd);
    this.document.head.appendChild(this.jsonLdElement);
  }

  ngAfterViewInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    // Scroll animations
    const observer = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
        }
      });
    }, { threshold: 0.12, rootMargin: '0px 0px -40px 0px' });

    document.querySelectorAll('.fade-in').forEach(el => observer.observe(el));

    // Header scroll effect
    const header = document.querySelector('header');
    if (header) {
      window.addEventListener('scroll', () => {
        header.style.boxShadow = window.scrollY > 20
          ? '0 4px 24px rgba(26,58,92,0.1)'
          : 'none';
      });
    }
  }
}
