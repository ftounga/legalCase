import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterModule } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { WorkspaceUpgradeRequiredPageComponent } from './workspace-upgrade-required-page.component';

/**
 * F-156 SF-156-02 — CA11 / invariant SF-156-00b §5 :
 * CTA pricing ouvre en nouvelle page (target="_blank") pour préserver le contexte de l'app.
 */
describe('WorkspaceUpgradeRequiredPageComponent (F-156 SF-156-02)', () => {
  let fixture: ComponentFixture<WorkspaceUpgradeRequiredPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        WorkspaceUpgradeRequiredPageComponent,
        RouterModule.forRoot([]),
        NoopAnimationsModule,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkspaceUpgradeRequiredPageComponent);
    fixture.detectChanges();
  });

  it('UPR-01 — affiche le titre + sous-titre TEAM/PRO', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Création d\'un workspace supplémentaire');
    expect(el.textContent).toContain('TEAM');
    expect(el.textContent).toContain('PRO');
  });

  // Invariant SF-156-00b §5
  it('UPR-02 — CTA "Découvrir les plans" ouvre en nouvelle page (target="_blank")', () => {
    const el: HTMLElement = fixture.nativeElement;
    const cta = el.querySelector('[data-testid="wupr-cta-pricing"]') as HTMLAnchorElement;
    expect(cta).toBeTruthy();
    expect(cta.getAttribute('target')).toBe('_blank');
    expect(cta.getAttribute('rel')).toContain('noopener');
    expect(cta.getAttribute('href')).toBeTruthy();
  });

  it('UPR-03 — bouton retour dashboard pointe sur /dashboard', () => {
    const el: HTMLElement = fixture.nativeElement;
    // Le bouton retour utilise routerLink — Angular l'expose via ng-reflect-router-link.
    const back = Array.from(el.querySelectorAll('a')).find(a => (a.textContent || '').includes('Retour'));
    expect(back).toBeTruthy();
    expect(back?.getAttribute('ng-reflect-router-link')).toBe('/dashboard');
  });
});
