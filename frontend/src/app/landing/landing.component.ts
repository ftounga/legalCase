import { Component, AfterViewInit, ViewEncapsulation, inject, OnInit, OnDestroy, PLATFORM_ID, signal, computed } from '@angular/core';
import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl, Title, Meta } from '@angular/platform-browser';

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
    title: 'Checklist prud\'homale automatique',
    subtitle: 'Pièces manquantes + vices de procédure détectés',
  },
  {
    videoId: 'HVGXeUnrbks',
    title: 'Comparateur d\'indemnités',
    subtitle: 'Barème Macron vs conventionnel en un clic',
  },
];

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
  private sanitizer = inject(DomSanitizer);
  private jsonLdElement: HTMLScriptElement | null = null;

  readonly videos = DEMO_VIDEOS;
  readonly selectedVideoId = signal<string>(DEMO_VIDEOS[0].videoId);
  readonly videoEmbedUrl = computed<SafeResourceUrl>(() =>
    this.sanitizer.bypassSecurityTrustResourceUrl(
      `https://www.youtube.com/embed/${this.selectedVideoId()}?rel=0`
    )
  );

  selectVideo(videoId: string): void {
    this.selectedVideoId.set(videoId);
  }

  videoThumbnailUrl(videoId: string): string {
    return `https://img.youtube.com/vi/${videoId}/maxresdefault.jpg`;
  }

  onThumbnailError(event: Event, videoId: string): void {
    // Fallback sur hqdefault.jpg si maxresdefault n'existe pas (vidéos anciennes)
    const img = event.target as HTMLImageElement;
    const hqUrl = `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`;
    if (img.src !== hqUrl) {
      img.src = hqUrl;
    }
  }

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
