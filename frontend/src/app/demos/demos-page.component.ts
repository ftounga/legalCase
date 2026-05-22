import {
  Component,
  OnInit,
  OnDestroy,
  ViewEncapsulation,
  inject,
  signal,
  computed,
  PLATFORM_ID,
} from '@angular/core';
import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl, Title, Meta } from '@angular/platform-browser';

import {
  DEMO_VIDEOS,
  DemoVideo,
  getDemoEmbedUrl,
  getDemoThumbnailUrl,
} from '../shared/demos/demo-videos.data';

@Component({
  selector: 'app-demos-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './demos-page.component.html',
  styleUrl: './demos-page.component.scss',
  encapsulation: ViewEncapsulation.None,
})
export class DemosPageComponent implements OnInit, OnDestroy {
  private title = inject(Title);
  private meta = inject(Meta);
  private sanitizer = inject(DomSanitizer);
  private document = inject(DOCUMENT);
  private platformId = inject(PLATFORM_ID);
  private jsonLdElement: HTMLScriptElement | null = null;

  readonly videos: readonly DemoVideo[] = DEMO_VIDEOS;
  readonly selectedVideoId = signal<string | null>(null);
  readonly count = computed(() => this.videos.length);
  readonly currentYear = new Date().getFullYear();

  readonly calendlyUrl =
    'https://calendly.com/tounga-franck-ng-itconsulting/demo-legalcase';

  thumbnailUrl(videoId: string): string {
    return getDemoThumbnailUrl(videoId);
  }

  embedUrl(videoId: string): SafeResourceUrl {
    return getDemoEmbedUrl(videoId, this.sanitizer);
  }

  isSelected(videoId: string): boolean {
    return this.selectedVideoId() === videoId;
  }

  selectVideo(videoId: string): void {
    this.selectedVideoId.set(videoId);
  }

  closeVideo(event?: Event): void {
    event?.stopPropagation();
    this.selectedVideoId.set(null);
  }

  onCardKeydown(event: KeyboardEvent, videoId: string): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.selectVideo(videoId);
    } else if (event.key === 'Escape') {
      this.closeVideo();
    }
  }

  ngOnInit(): void {
    const pageTitle =
      'Démonstrations vidéo — AI LegalCase : analyse de dossier, outils décisionnels';
    const description =
      'Démonstrations vidéo de LegalCase : analyse de dossier de bout en bout, outils décisionnels pré-remplis, droit du travail, immigration et famille — France et Belgique.';
    const url = 'https://legalcase.fr/demos';

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
    let link = this.document.querySelector('link[rel="canonical"]') as HTMLLinkElement | null;
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
          { '@type': 'ListItem', position: 1, name: 'LegalCase', item: 'https://legalcase.fr/' },
          { '@type': 'ListItem', position: 2, name: 'Démonstrations', item: url },
        ],
      },
      mainContentOfPage: this.videos.map(v => ({
        '@type': 'VideoObject',
        name: v.title,
        description: v.subtitle,
        thumbnailUrl: getDemoThumbnailUrl(v.videoId),
        embedUrl: `https://www.youtube-nocookie.com/embed/${v.videoId}`,
      })),
    };
    this.jsonLdElement = this.document.createElement('script');
    this.jsonLdElement.type = 'application/ld+json';
    this.jsonLdElement.textContent = JSON.stringify(jsonLd);
    this.document.head.appendChild(this.jsonLdElement);
  }
}
