import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LandingComponent } from './landing.component';
import { provideRouter } from '@angular/router';
import { Title, Meta } from '@angular/platform-browser';

describe('LandingComponent', () => {
  let component: LandingComponent;
  let fixture: ComponentFixture<LandingComponent>;
  let titleService: Title;
  let metaService: Meta;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LandingComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(LandingComponent);
    component = fixture.componentInstance;
    titleService = TestBed.inject(Title);
    metaService = TestBed.inject(Meta);
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('définit le title SEO au chargement', () => {
    expect(titleService.getTitle()).toContain('280+ outils décisionnels');
    expect(titleService.getTitle()).not.toContain('92');
  });

  // SF-158-06 : SEO aligné V4 — title < 70 car, description < 160 car (bonnes pratiques)
  it('le title SEO fait moins de 70 caractères', () => {
    expect(titleService.getTitle().length).toBeLessThan(70);
  });

  it('la meta description SEO fait moins de 160 caractères et mentionne « 280+ » + conclusions', () => {
    const tag = metaService.getTag('name="description"');
    expect(tag?.content.length).toBeLessThan(160);
    expect(tag?.content).toContain('280+');
    expect(tag?.content).toContain('conclusions');
    expect(tag?.content).not.toContain('92');
  });

  it('définit la meta description SEO au chargement', () => {
    const tag = metaService.getTag('name="description"');
    // SF-158-06 : description V4 (< 160 car) — essentiel = 280+ outils + conclusions + FR/BE
    expect(tag?.content).toContain('280+');
    expect(tag?.content).toContain('FR/BE');
  });

  it('définit la meta og:title au chargement', () => {
    const tag = metaService.getTag('property="og:title"');
    expect(tag?.content).toContain('AI LegalCase');
  });

  it('définit la Twitter Card avec image large', () => {
    const card = metaService.getTag('name="twitter:card"');
    expect(card?.content).toBe('summary_large_image');
    const tw = metaService.getTag('name="twitter:title"');
    expect(tw?.content).toContain('280+ outils');
    expect(tw?.content).not.toContain('92');
  });

  it('insère un link rel="canonical"', () => {
    const link = document.querySelector('link[rel="canonical"]') as HTMLLinkElement | null;
    expect(link).not.toBeNull();
    expect(link?.href).toContain('legalcase.fr');
  });

  // SF-158-04 : hero « chaîne de valeur » — H1 nomme l'état terminal (conclusions),
  // ne contient plus le chiffre d'outils « 92 » (devenu faux, 254 au registre).
  it('le H1 nomme la chaîne de valeur jusqu\'aux conclusions', () => {
    const h1 = fixture.nativeElement.querySelector('h1');
    expect(h1?.textContent).toContain('conclusions');
  });

  it('le H1 ne contient plus le chiffre d\'outils « 92 »', () => {
    const h1 = fixture.nativeElement.querySelector('h1');
    expect(h1?.textContent).not.toContain('92');
  });

  it('le bouton outline annonce « 280+ outils »', () => {
    const btn = fixture.nativeElement.querySelector('.hero-actions .btn-outline');
    expect(btn?.textContent).toContain('280+');
    expect(btn?.textContent).not.toContain('92');
  });

  it('le titre de la section Outils ne contient plus « 92 »', () => {
    const sectionTitles: HTMLElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('.section-title')
    );
    const toolsTitle = sectionTitles.find(t => t.textContent?.includes('outils décisionnels'));
    expect(toolsTitle).toBeTruthy();
    expect(toolsTitle?.textContent).not.toContain('92');
    expect(toolsTitle?.textContent).toContain('280+');
  });

  it('repricing V7 — Solo 99 €, Team 219 €, Pro 429 €', () => {
    const html = fixture.nativeElement.innerHTML as string;
    expect(html).toContain('99 €');
    expect(html).toContain('219 €');
    expect(html).toContain('429 €');
    expect(html).not.toContain('59 €</span>');
    expect(html).not.toContain('119 €');
    expect(html).not.toContain('249 €');
  });

  it('affiche les 4 cartes de pricing', () => {
    const cards = fixture.nativeElement.querySelectorAll('.pricing-card');
    expect(cards.length).toBe(4);
  });

  it('le CTA principal pointe vers /login', () => {
    const cta = fixture.nativeElement.querySelector('.hero-actions .btn-primary');
    expect(cta?.getAttribute('href')).toBe('/login');
  });

  // SF-126-01 : galerie vidéo
  it('expose 6 vidéos dans la galerie démo', () => {
    expect(component.videos.length).toBe(6);
    expect(component.videos.map(v => v.videoId)).toEqual([
      'NGTRMWQKPEA', 'I5EemkFR8NE', 'HVGXeUnrbks', 'rKJXppVe2SA', 'Qh3hAO75xMk', '78hEuoD_L_4'
    ]);
  });

  it('sélectionne la première vidéo par défaut', () => {
    expect(component.selectedVideoId()).toBe('NGTRMWQKPEA');
  });

  it('selectVideo change la vidéo active', () => {
    component.selectVideo('I5EemkFR8NE');
    expect(component.selectedVideoId()).toBe('I5EemkFR8NE');
  });

  // SF-77-03 : RGPD — embed YouTube en mode no-cookie (pas de cookie tracking avant Play)
  it('videoEmbedUrl utilise youtube-nocookie.com (RGPD)', () => {
    const url = (component.videoEmbedUrl() as unknown as { changingThisBreaksApplicationSecurity: string });
    const raw = url.changingThisBreaksApplicationSecurity ?? String(component.videoEmbedUrl());
    expect(raw).toContain('youtube-nocookie.com/embed/');
    expect(raw).not.toContain('://www.youtube.com/embed/');
  });

  it('affiche 6 miniatures cliquables sous le player', () => {
    const thumbs = fixture.nativeElement.querySelectorAll('.video-thumb');
    expect(thumbs.length).toBe(6);
  });

  it('la première miniature est marquée active au chargement', () => {
    const firstThumb = fixture.nativeElement.querySelector('.video-thumb');
    expect(firstThumb?.classList.contains('video-thumb--active')).toBe(true);
    expect(firstThumb?.getAttribute('aria-pressed')).toBe('true');
  });

  it('expose les boutons de navigation prev/next du carrousel', () => {
    const prev = fixture.nativeElement.querySelector('.video-carousel-arrow--prev');
    const next = fixture.nativeElement.querySelector('.video-carousel-arrow--next');
    expect(prev).toBeTruthy();
    expect(next).toBeTruthy();
    expect(prev?.getAttribute('aria-label')).toBe('Vidéos précédentes');
    expect(next?.getAttribute('aria-label')).toBe('Vidéos suivantes');
  });

  it('atStart est vrai au démarrage (carrousel en début)', () => {
    expect(component.atStart()).toBe(true);
  });

  it('le bouton prev est disabled au démarrage', () => {
    const prev = fixture.nativeElement.querySelector('.video-carousel-arrow--prev');
    expect(prev?.hasAttribute('disabled')).toBe(true);
  });

  // SF-158-05 : section « Rédaction de conclusions » + jurisprudence vérifiable
  it('SF-158-05 — rend une section #conclusions', () => {
    const section = fixture.nativeElement.querySelector('section#conclusions');
    expect(section).toBeTruthy();
  });

  it('SF-158-05 — la section #conclusions vend le projet de conclusions dans le style, export Word/.docx', () => {
    const section = fixture.nativeElement.querySelector('section#conclusions') as HTMLElement;
    const txt = section?.textContent ?? '';
    expect(txt).toContain('projet de conclusions');
    expect(txt).toContain('style');
    expect(txt.includes('.docx') || txt.includes('Word')).toBe(true);
  });

  it('SF-158-05 — le pipeline rend exactement 6 étapes', () => {
    const steps = fixture.nativeElement.querySelectorAll('.pipeline-step');
    expect(steps.length).toBe(6);
  });

  it('SF-158-05 — la 6ᵉ étape du pipeline est « Conclusions »', () => {
    const labels: HTMLElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('.pipeline-step .step-label')
    );
    expect(labels[labels.length - 1]?.textContent).toContain('Conclusions');
  });

  it('SF-158-05 — la différenciation contient une carte « Jurisprudence vérifiable »', () => {
    // Recherche par texte sur toutes les cartes (robuste à l'ajout de sections
    // .why-us comme #pilotage, F-158 V5).
    const cards: HTMLElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('.why-card'),
    );
    const jurisCard = cards.find(c => c.textContent?.includes('Jurisprudence vérifiable'));
    expect(jurisCard).toBeTruthy();
  });

  it('SF-158-05 — ANTI-OVERCLAIM : aucune mention de jurisprudence/Cassation belge', () => {
    const html = (fixture.nativeElement.innerHTML as string).toLowerCase();
    expect(html).not.toContain('jurisprudence belge');
    expect(html).not.toContain('cassation belge');
    expect(html).not.toContain('cour de cassation belge');
  });

  // SF-240-04 : lien DPA dans le footer
  it('LDF-01 — footer contient un lien Télécharger le DPA pointant vers /api/v1/legal/dpa', () => {
    const footerLinks = fixture.nativeElement.querySelectorAll('footer .footer-links a');
    const dpaLink = Array.from(footerLinks).find(
      (a: any) => a.textContent?.includes('DPA') || a.textContent?.includes('Télécharger le DPA')
    ) as HTMLAnchorElement | undefined;
    expect(dpaLink).toBeTruthy();
    expect(dpaLink?.getAttribute('href')).toBe('/api/v1/legal/dpa');
  });

  // SF-158-06 : nettoyage claims non sourcés + allègement charge écran
  it('SF-158-06 — le hero ne contient plus le claim « 10× » ni « Plus rapide qu\'une lecture manuelle »', () => {
    const hero = fixture.nativeElement.querySelector('.hero-stats') as HTMLElement;
    const txt = hero?.textContent ?? '';
    expect(txt).not.toContain('10×');
    expect(txt).not.toContain('Plus rapide qu\'une lecture manuelle');
  });

  it('SF-158-06 — le hero expose une stat factuelle « France & Belgique »', () => {
    const hero = fixture.nativeElement.querySelector('.hero-stats') as HTMLElement;
    expect(hero?.textContent).toContain('France & Belgique');
  });

  it('SF-158-06 — la section OCR ne contient plus le claim « 200 dpi »', () => {
    const html = fixture.nativeElement.innerHTML as string;
    expect(html).not.toContain('200 dpi');
  });

  it('SF-158-06 — la section Fonctionnalités rend exactement 6 cartes', () => {
    const cards = fixture.nativeElement.querySelectorAll('#fonctionnalites .feature-card');
    expect(cards.length).toBe(6);
  });

  it('SF-158-06 — garde-fou : le template ne contient plus « 92 »', () => {
    const html = fixture.nativeElement.innerHTML as string;
    expect(html).not.toContain('92');
  });
});
