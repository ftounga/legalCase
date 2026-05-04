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
    expect(titleService.getTitle()).toContain('92 outils décisionnels');
  });

  it('définit la meta description SEO au chargement', () => {
    const tag = metaService.getTag('name="description"');
    expect(tag?.content).toContain('Essai gratuit 14 jours');
  });

  it('définit la meta og:title au chargement', () => {
    const tag = metaService.getTag('property="og:title"');
    expect(tag?.content).toContain('AI LegalCase');
  });

  it('affiche le titre principal — repositionnement plateforme outils décisionnels', () => {
    const h1 = fixture.nativeElement.querySelector('h1');
    expect(h1?.textContent).toContain('92 outils décisionnels');
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
  it('expose 5 vidéos dans la galerie démo', () => {
    expect(component.videos.length).toBe(5);
    expect(component.videos.map(v => v.videoId)).toEqual([
      'NGTRMWQKPEA', 'I5EemkFR8NE', 'HVGXeUnrbks', 'rKJXppVe2SA', 'Qh3hAO75xMk'
    ]);
  });

  it('sélectionne la première vidéo par défaut', () => {
    expect(component.selectedVideoId()).toBe('NGTRMWQKPEA');
  });

  it('selectVideo change la vidéo active', () => {
    component.selectVideo('I5EemkFR8NE');
    expect(component.selectedVideoId()).toBe('I5EemkFR8NE');
  });

  it('affiche 5 miniatures cliquables sous le player', () => {
    const thumbs = fixture.nativeElement.querySelectorAll('.video-thumb');
    expect(thumbs.length).toBe(5);
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
});
