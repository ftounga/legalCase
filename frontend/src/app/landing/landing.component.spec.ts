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
});
