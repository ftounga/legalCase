import { Component, AfterViewInit, ViewEncapsulation, inject, OnInit, OnDestroy, PLATFORM_ID, signal, computed, ElementRef, ViewChild } from '@angular/core';
import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl, Title, Meta } from '@angular/platform-browser';

import { LandingToolsShowcaseComponent } from './landing-tools-showcase/landing-tools-showcase.component';

/** SF-126-01 : vidéos de la galerie démo.
 *  Éditer ici pour changer l'ordre, les titres ou remplacer les vidéos.
 *  Les thumbnails proviennent de YouTube : img.youtube.com/vi/{ID}/maxresdefault.jpg */
export interface DemoVideo {
  videoId: string;
  title: string;
  subtitle: string;
}

export const DEMO_VIDEOS: DemoVideo[] = [
  {
    videoId: 'NGTRMWQKPEA',
    title: 'Votre dossier analysé en 3 min',
    subtitle: "De l'upload des pièces à la synthèse structurée",
  },
  {
    videoId: 'I5EemkFR8NE',
    title: 'Synthèse enrichie et diff sourcé',
    subtitle: 'Comparez les versions d\'analyse, chaque point relié à sa pièce',
  },
  {
    videoId: 'HVGXeUnrbks',
    title: 'Validité prud\'homale — pièces, délais, score',
    subtitle: 'Pièces manquantes identifiées, délais légaux vérifiés, score calculé',
  },
  {
    videoId: 'rKJXppVe2SA',
    title: 'Démo droit du travail',
    subtitle: 'Un dossier analysé de bout en bout : synthèse, procédure, indemnités',
  },
  {
    videoId: 'Qh3hAO75xMk',
    title: 'Démo droit de l\'immigration',
    subtitle: 'Un dossier analysé de bout en bout : titre de séjour, recours, pièces',
  },
  {
    videoId: '78hEuoD_L_4',
    title: 'Démo droit de la famille',
    subtitle: 'Un dossier analysé de bout en bout : divorce, étapes, pièces',
  },
];

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink, LandingToolsShowcaseComponent],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss',
  encapsulation: ViewEncapsulation.None
})
export class LandingComponent implements OnInit, AfterViewInit, OnDestroy {
  private title = inject(Title);
  private meta = inject(Meta);
  private platformId = inject(PLATFORM_ID);
  private document = inject(DOCUMENT);
  private sanitizer = inject(DomSanitizer);
  private jsonLdElement: HTMLScriptElement | null = null;

  readonly videos = DEMO_VIDEOS;
  readonly selectedVideoId = signal<string>(DEMO_VIDEOS[0].videoId);
  readonly videoEmbedUrl = computed<SafeResourceUrl>(() =>
    this.sanitizer.bypassSecurityTrustResourceUrl(
      `https://www.youtube-nocookie.com/embed/${this.selectedVideoId()}?rel=0`
    )
  );

  @ViewChild('videoScroller') videoScrollerRef?: ElementRef<HTMLDivElement>;
  readonly atStart = signal(true);
  readonly atEnd = signal(false);

  selectVideo(videoId: string): void {
    this.selectedVideoId.set(videoId);
  }

  videoThumbnailUrl(videoId: string): string {
    // hqdefault.jpg (480x360) existe pour 100% des vidéos YouTube.
    // maxresdefault peut renvoyer 404 selon la résolution d'upload → évité.
    return `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`;
  }

  scrollPrev(): void { this.scrollByThumb(-1); }
  scrollNext(): void { this.scrollByThumb(1); }

  onScrollerScroll(): void {
    const el = this.videoScrollerRef?.nativeElement;
    if (!el) return;
    this.atStart.set(el.scrollLeft <= 1);
    this.atEnd.set(el.scrollLeft + el.clientWidth >= el.scrollWidth - 1);
  }

  private scrollByThumb(direction: 1 | -1): void {
    const el = this.videoScrollerRef?.nativeElement;
    if (!el) return;
    const thumb = el.querySelector('.video-thumb') as HTMLElement | null;
    const step = thumb ? thumb.offsetWidth + 16 : el.clientWidth * 0.8;
    el.scrollBy({ left: direction * step, behavior: 'smooth' });
  }

  ngOnInit(): void {
    const title = 'AI LegalCase — 92 outils décisionnels pour avocats : Travail, Immigration, Famille (FR/BE)';
    const description = 'Plateforme de 92 outils décisionnels juridiques pré-remplis par IA. Calculateurs, scorings, comparateurs, générateurs sur droit du travail, immigration et famille — France et Belgique. OCR + Vision pour extraire scans dégradés et photos de SMS. Essai gratuit 14 jours.';
    const shortDescription = 'Plateforme de 92 outils décisionnels juridiques pré-remplis par IA depuis vos pièces. Travail, Immigration, Famille — France et Belgique. Essai gratuit 14 jours.';
    const url = 'https://legalcase.ng-itconsulting.com/';

    this.title.setTitle(title);
    this.meta.updateTag({ name: 'description', content: description });

    // Open Graph
    this.meta.updateTag({ property: 'og:title', content: title });
    this.meta.updateTag({ property: 'og:description', content: shortDescription });
    this.meta.updateTag({ property: 'og:url', content: url });
    this.meta.updateTag({ property: 'og:type', content: 'website' });
    this.meta.updateTag({ property: 'og:site_name', content: 'AI LegalCase' });
    this.meta.updateTag({ property: 'og:locale', content: 'fr_FR' });

    // Twitter Card
    this.meta.updateTag({ name: 'twitter:card', content: 'summary_large_image' });
    this.meta.updateTag({ name: 'twitter:title', content: title });
    this.meta.updateTag({ name: 'twitter:description', content: shortDescription });

    // Canonical link
    this.upsertCanonical(url);

    this.injectJsonLd();
  }

  private upsertCanonical(href: string): void {
    let link = this.document.querySelector('link[rel="canonical"]') as HTMLLinkElement | null;
    if (!link) {
      link = this.document.createElement('link');
      link.rel = 'canonical';
      this.document.head.appendChild(link);
    }
    link.href = href;
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
      'description': 'Plateforme de 92 outils décisionnels juridiques pré-remplis par IA depuis vos pièces. Calculateurs, scorings, comparateurs et générateurs sur droit du travail, immigration et famille — France et Belgique. OCR et Vision pour extraire scans dégradés et photos de SMS.',
      'featureList': [
        '92 outils décisionnels juridiques (calculateurs, scorings, comparateurs, générateurs, détecteurs)',
        'Legal OCR pour scans dégradés et fax',
        'Legal Vision pour photos de SMS, captures d\'écran, manuscrits',
        'Synthèse structurée : timeline, faits, risques, points de droit cités',
        'Cohérence multi-source : divergences IA détectées',
        'Couverture droit du travail, immigration, famille',
        'Couverture France et Belgique',
        'Multi-utilisateurs avec isolation par cabinet',
        'Hébergement Union Européenne, conforme RGPD',
      ],
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

    // Initial carousel edge state (atEnd peut être true si toutes les vidéos rentrent)
    queueMicrotask(() => this.onScrollerScroll());

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

    // SF-118-07 : compteurs animés dans le hero (ease-out cubic)
    const counterObserver = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (!entry.isIntersecting) return;
        const el = entry.target as HTMLElement;
        const target = parseInt(el.getAttribute('data-target') || '0', 10);
        if (!target) return;
        const duration = 1500;
        const start = performance.now();
        const step = (now: number) => {
          const progress = Math.min((now - start) / duration, 1);
          const eased = 1 - Math.pow(1 - progress, 3);
          el.textContent = Math.floor(eased * target).toString();
          if (progress < 1) requestAnimationFrame(step);
        };
        requestAnimationFrame(step);
        counterObserver.unobserve(el);
      });
    }, { threshold: 0.5 });
    document.querySelectorAll('.counter').forEach(el => counterObserver.observe(el));
  }
}
