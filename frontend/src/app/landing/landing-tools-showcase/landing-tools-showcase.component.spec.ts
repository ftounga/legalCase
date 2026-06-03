import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LandingToolsShowcaseComponent } from './landing-tools-showcase.component';
import { LANDING_TOOLS_CATALOG } from '../landing-tools-catalog';

describe('LandingToolsShowcaseComponent', () => {
  let component: LandingToolsShowcaseComponent;
  let fixture: ComponentFixture<LandingToolsShowcaseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LandingToolsShowcaseComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(LandingToolsShowcaseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // SF-158-04 : catalogue régénéré depuis TOOL_REGISTRY (254 entrées au 2026-06-03).
  // Garde-fou anti-dérive : le registre réel doit rester au-dessus de 250.
  it('expose le catalogue régénéré (>= 250 outils, ids uniques)', () => {
    expect(LANDING_TOOLS_CATALOG.length).toBeGreaterThanOrEqual(250);
    const ids = LANDING_TOOLS_CATALOG.map(t => t.id);
    expect(new Set(ids).size).toBe(ids.length);
  });

  it('aucun outil n\'a de label vide', () => {
    LANDING_TOOLS_CATALOG.forEach(t => {
      expect(t.label.trim().length).toBeGreaterThan(0);
    });
  });

  it('affiche tous les outils par défaut', () => {
    const cards = fixture.nativeElement.querySelectorAll('.tool-card:not(.tool-card--empty)');
    expect(cards.length).toBe(LANDING_TOOLS_CATALOG.length);
  });

  it('le titre mentionne le nombre total d\'outils', () => {
    const title = fixture.nativeElement.querySelector('.tools-showcase__title');
    expect(title?.textContent).toContain(`${LANDING_TOOLS_CATALOG.length} outils décisionnels`);
  });

  it('filtre par domaine TRAVAIL réduit la liste', () => {
    const travailCount = LANDING_TOOLS_CATALOG.filter(t => t.domain === 'TRAVAIL').length;
    expect(travailCount).toBeGreaterThan(0);

    const buttons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('.filter-row[aria-label="Domaine"] .chip')
    );
    const travailBtn = buttons.find(b => b.textContent?.includes('Droit du travail'));
    expect(travailBtn).toBeTruthy();
    travailBtn!.click();
    fixture.detectChanges();

    const cards = fixture.nativeElement.querySelectorAll('.tool-card:not(.tool-card--empty)');
    expect(cards.length).toBe(travailCount);
  });

  it('filtre par pays BE réduit la liste', () => {
    const beCount = LANDING_TOOLS_CATALOG.filter(t => t.country === 'BE').length;
    expect(beCount).toBeGreaterThan(0);

    const buttons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('.filter-row[aria-label="Pays"] .chip')
    );
    const beBtn = buttons.find(b => b.textContent?.trim() === 'Belgique');
    expect(beBtn).toBeTruthy();
    beBtn!.click();
    fixture.detectChanges();

    const cards = fixture.nativeElement.querySelectorAll('.tool-card:not(.tool-card--empty)');
    expect(cards.length).toBe(beCount);
  });

  it('cumul filtres domaine + pays', () => {
    const expected = LANDING_TOOLS_CATALOG.filter(t => t.domain === 'IMMIGRATION' && t.country === 'BE').length;

    const domainButtons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('.filter-row[aria-label="Domaine"] .chip')
    );
    domainButtons.find(b => b.textContent?.includes('immigration'))!.click();
    fixture.detectChanges();

    const countryButtons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('.filter-row[aria-label="Pays"] .chip')
    );
    countryButtons.find(b => b.textContent?.trim() === 'Belgique')!.click();
    fixture.detectChanges();

    const cards = fixture.nativeElement.querySelectorAll('.tool-card:not(.tool-card--empty)');
    expect(cards.length).toBe(expected);
  });

  it('chaque outil expose son domaine et son pays', () => {
    const cards = fixture.nativeElement.querySelectorAll('.tool-card:not(.tool-card--empty)');
    cards.forEach((card: HTMLElement) => {
      expect(card.dataset['domain']).toBeTruthy();
      expect(card.dataset['country']).toBeTruthy();
    });
  });
});
