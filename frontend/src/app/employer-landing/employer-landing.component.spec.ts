import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { EmployerLandingComponent } from './employer-landing.component';

const CALENDLY_URL = 'https://calendly.com/tounga-franck-ng-itconsulting/30min';

describe('EmployerLandingComponent', () => {
  let component: EmployerLandingComponent;
  let fixture: ComponentFixture<EmployerLandingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmployerLandingComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(EmployerLandingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('rend le hero avec le titre exact', () => {
    const title = fixture.nativeElement.querySelector('.emp-hero-title');
    expect(title).toBeTruthy();
    expect(title.textContent.trim()).toBe(
      "Chiffrez l'exposition prud'homale d'un licenciement — avant de décider.",
    );
  });

  it('expose un CTA Calendly au hero ET en fin de page (href + target + rel)', () => {
    const calendlyLinks: HTMLAnchorElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('a.emp-btn--primary'),
    );
    // hero + cta final
    expect(calendlyLinks.length).toBeGreaterThanOrEqual(2);
    calendlyLinks.forEach(link => {
      expect(link.getAttribute('href')).toBe(CALENDLY_URL);
      expect(link.getAttribute('target')).toBe('_blank');
      expect(link.getAttribute('rel')).toContain('noopener');
    });
  });

  it('le CTA pointe vers le créneau /30min (audience employeur)', () => {
    expect(component.calendlyUrl).toBe(CALENDLY_URL);
    expect(component.calendlyUrl).toContain('/30min');
  });

  it("la section valeur liste exactement 4 capacités réelles", () => {
    expect(component.valueItems.length).toBe(4);
    const cards = fixture.nativeElement.querySelectorAll('.emp-value-card');
    expect(cards.length).toBe(4);
  });

  it('présente le messaging conformité et JAMAIS « gagner contre vos salariés »', () => {
    const text = (fixture.nativeElement.textContent || '').toLowerCase();
    expect(text).toContain('conformité');
    expect(text).toContain('maîtrise du risque');
    expect(text).not.toContain('gagner contre');
    expect(text).not.toContain('contre vos salariés');
  });

  it('mentionne hébergement EU, RGPD et isolation des données', () => {
    const text = (fixture.nativeElement.textContent || '').toLowerCase();
    expect(text).toContain('union européenne');
    expect(text).toContain('rgpd');
    expect(text).toContain('isolées');
  });

  it("n'affiche aucun prix (pas de montant €/mois ni de plan tarifé)", () => {
    const text: string = fixture.nativeElement.textContent || '';
    // Aucun montant monétaire chiffré (ex: « 99 € », « 800€/mois »).
    expect(text).not.toMatch(/\d+\s?€/);
    expect(text.toLowerCase()).not.toContain('/mois');
    expect(text.toLowerCase()).not.toContain('tarif');
  });

  it("n'utilise pas /login comme CTA d'action (pas de self-serve)", () => {
    const ctaButtons: HTMLAnchorElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('a.emp-btn'),
    );
    ctaButtons.forEach(btn => {
      expect(btn.getAttribute('href')).toBe(CALENDLY_URL);
      expect(btn.getAttribute('routerlink')).toBeNull();
    });
  });

  it('met à jour le title de la page (contient « Employeur »)', () => {
    expect(document.title).toContain('Employeur');
  });

  it('définit le canonical vers https://legalcase.fr/employeur', () => {
    const canonical = document.head.querySelector(
      'link[rel="canonical"]',
    ) as HTMLLinkElement;
    expect(canonical).toBeTruthy();
    expect(canonical.href).toBe('https://legalcase.fr/employeur');
  });

  it('injecte puis nettoie le JSON-LD WebPage /employeur', () => {
    const present = Array.from(
      document.head.querySelectorAll('script[type="application/ld+json"]'),
    ).find(s => {
      const json = JSON.parse(s.textContent || '{}');
      return json['@type'] === 'WebPage' && json.url === 'https://legalcase.fr/employeur';
    });
    expect(present).toBeDefined();

    fixture.destroy();
    const remaining = Array.from(
      document.head.querySelectorAll('script[type="application/ld+json"]'),
    ).filter(s => {
      const json = JSON.parse(s.textContent || '{}');
      return json['@type'] === 'WebPage' && json.url === 'https://legalcase.fr/employeur';
    });
    expect(remaining.length).toBe(0);
  });
});
