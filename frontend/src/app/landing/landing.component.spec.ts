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
    expect(titleService.getTitle()).toBe('AI LegalCase — Analyse IA pour avocats en droit du travail');
  });

  it('définit la meta description SEO au chargement', () => {
    const tag = metaService.getTag('name="description"');
    expect(tag?.content).toContain('Essai gratuit 14 jours');
  });

  it('définit la meta og:title au chargement', () => {
    const tag = metaService.getTag('property="og:title"');
    expect(tag?.content).toContain('AI LegalCase');
  });

  it('affiche le titre principal', () => {
    const h1 = fixture.nativeElement.querySelector('h1');
    expect(h1?.textContent).toContain('collaborateur');
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
  it('expose 4 vidéos dans la galerie démo', () => {
    expect(component.videos.length).toBe(4);
    expect(component.videos.map(v => v.videoId)).toEqual([
      'NGTRMWQKPEA', 'I5EemkFR8NE', 'HVGXeUnrbks', 'rKJXppVe2SA'
    ]);
  });

  it('sélectionne la première vidéo par défaut', () => {
    expect(component.selectedVideoId()).toBe('NGTRMWQKPEA');
  });

  it('selectVideo change la vidéo active', () => {
    component.selectVideo('I5EemkFR8NE');
    expect(component.selectedVideoId()).toBe('I5EemkFR8NE');
  });

  it('affiche 4 miniatures cliquables sous le player', () => {
    const thumbs = fixture.nativeElement.querySelectorAll('.video-thumb');
    expect(thumbs.length).toBe(4);
  });

  it('la première miniature est marquée active au chargement', () => {
    const firstThumb = fixture.nativeElement.querySelector('.video-thumb');
    expect(firstThumb?.classList.contains('video-thumb--active')).toBe(true);
    expect(firstThumb?.getAttribute('aria-pressed')).toBe('true');
  });
});
