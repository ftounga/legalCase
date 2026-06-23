import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
  PLATFORM_ID,
} from '@angular/core';
import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl } from '@angular/forms';
import { Title, Meta } from '@angular/platform-browser';
import { ContactService } from '../contact/contact.service';

/**
 * F-296 SF-296-01 — Landing partenaire ACE + formulaire de contact.
 *
 * Page marketing publique `/ace` (no-auth, lazy, prérendue) ciblée par le bouton
 * CTA de l'encart inséré dans la plaquette « Offres de nos partenaires ACE 2026 ».
 * Présente l'offre LegalCase + le code adhérent ACE2026, et un formulaire de
 * contact qui réutilise l'endpoint EXISTANT `POST /api/v1/contact` (0 backend).
 * Encapsulation Emulated + classes préfixées `.ace-` (modèle EmployerLanding,
 * anti-collision avec la landing en ViewEncapsulation.None).
 */

/** Pilier produit affiché dans la grille de valeur. */
export interface AceValueItem {
  icon: string;
  title: string;
  text: string;
}

@Component({
  selector: 'app-ace-landing',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './ace-landing.component.html',
  styleUrl: './ace-landing.component.scss',
})
export class AceLandingComponent implements OnInit, OnDestroy {
  private title = inject(Title);
  private meta = inject(Meta);
  private document = inject(DOCUMENT);
  private platformId = inject(PLATFORM_ID);
  private fb = inject(FormBuilder);
  private contactService = inject(ContactService);
  private jsonLdElement: HTMLScriptElement | null = null;

  /** Sujet fixe pour repérer les leads ACE dans la boîte équipe. */
  private static readonly SUBJECT = 'Partenariat ACE 2026';

  readonly currentYear = new Date().getFullYear();

  /** Les 5 piliers produit (alignés sur l'encart de la plaquette ACE). */
  readonly valueItems: readonly AceValueItem[] = [
    {
      icon: '🗂',
      title: 'Vue 360 du dossier',
      text: "Frise de suivi, phases, échanges (rounds) et échéances : tout le dossier vivant d'un coup d'œil.",
    },
    {
      icon: '🔎',
      title: 'Analyse des pièces',
      text: 'Déposez le dossier : les points clés et la chronologie ressortent automatiquement.',
    },
    {
      icon: '€',
      title: 'Chiffrage auditable',
      text: 'Barème, ancienneté, indemnités avec les bornes légales : chaque chiffre est vérifiable.',
    },
    {
      icon: '⚖',
      title: '280+ outils décisionnels',
      text: '3 domaines — Travail, Étrangers, Famille (FR et BE), un outil par situation métier.',
    },
    {
      icon: '📑',
      title: 'Conclusions sourcées',
      text: "Trame argumentée, jurisprudence à l'appui ; vous gardez la main sur la rédaction.",
    },
  ];

  /** Piliers de confiance (section conformité). */
  readonly trustItems: readonly string[] = [
    'Hébergement souverain (AWS Paris)',
    'Conforme au RGPD',
    'Données isolées par cabinet',
  ];

  readonly sending = signal(false);
  readonly sent = signal(false);
  readonly errorMsg = signal<string | null>(null);

  readonly form = this.fb.group({
    nom: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    cabinet: ['', [Validators.maxLength(150)]],
    telephone: ['', [Validators.pattern(/^[\d\s\+\-\(\)]{7,20}$/)]],
    message: ['', [Validators.required, Validators.maxLength(3000)]],
  });

  get nom(): AbstractControl { return this.form.get('nom')!; }
  get email(): AbstractControl { return this.form.get('email')!; }
  get telephone(): AbstractControl { return this.form.get('telephone')!; }
  get message(): AbstractControl { return this.form.get('message')!; }

  ngOnInit(): void {
    const pageTitle =
      'LegalCase × ACE — de la pièce aux conclusions, l\'offre réservée aux adhérents';
    const description =
      "Adhérents ACE : LegalCase analyse vos pièces, pilote le dossier dans la durée et vous accompagne jusqu'aux conclusions sourcées. 30 jours d'essai supplémentaires avec le code ACE2026.";
    const url = 'https://legalcase.fr/ace';

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

  submit(): void {
    if (this.form.invalid || this.sending()) {
      this.form.markAllAsTouched();
      return;
    }
    this.sending.set(true);
    this.errorMsg.set(null);

    const v = this.form.value;
    const cabinet = (v.cabinet ?? '').trim();
    const body = (cabinet ? `Cabinet / Barreau : ${cabinet}\n\n` : '') + (v.message ?? '');

    this.contactService
      .send({
        nom: v.nom!,
        email: v.email!,
        telephone: v.telephone?.trim() || undefined,
        sujet: AceLandingComponent.SUBJECT,
        message: body,
      })
      .subscribe({
        next: () => {
          this.sending.set(false);
          this.sent.set(true);
        },
        error: () => {
          this.sending.set(false);
          this.errorMsg.set(
            "L'envoi a échoué. Réessayez, ou écrivez directement à tounga.franck@ng-itconsulting.com.",
          );
        },
      });
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
          { '@type': 'ListItem', position: 1, name: 'LegalCase', item: 'https://legalcase.fr/' },
          { '@type': 'ListItem', position: 2, name: 'ACE', item: url },
        ],
      },
    };
    this.jsonLdElement = this.document.createElement('script');
    this.jsonLdElement.type = 'application/ld+json';
    this.jsonLdElement.textContent = JSON.stringify(jsonLd);
    this.document.head.appendChild(this.jsonLdElement);
  }
}
