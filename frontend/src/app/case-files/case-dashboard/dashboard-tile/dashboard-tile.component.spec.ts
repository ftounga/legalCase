import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { DashboardTileComponent } from './dashboard-tile.component';
import { DashboardTile } from '../../../core/models/case-dashboard.model';

/**
 * F-167 SF-167-01 — Tests Jest du composant `<app-dashboard-tile>`.
 */
describe('DashboardTileComponent', () => {
  let fixture: ComponentFixture<DashboardTileComponent>;
  let component: DashboardTileComponent;

  const baseTile: DashboardTile = {
    toolId: 'F-IM-11-changement-statut',
    theme: 'VALIDITE',
    label: 'Changement de statut',
    primaryValue: 'ETUDIANT → VPF (ELEVEE)',
    secondaryValue: '8 mois restants',
    alertLevel: 'OK',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardTileComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardTileComponent);
    component = fixture.componentInstance;
  });

  it('renders the tile via decision-tool-card', () => {
    component.tile = baseTile;
    fixture.detectChanges();

    const cardEl: HTMLElement = fixture.nativeElement.querySelector('app-decision-tool-card');
    expect(cardEl).toBeTruthy();
    // Le titre passé à la card est le label de la tile.
    expect(fixture.nativeElement.textContent).toContain('Changement de statut');
  });

  it('emits open(toolId) when the card emits open', () => {
    component.tile = baseTile;
    fixture.detectChanges();

    let emitted: string | undefined;
    component.open.subscribe((toolId: string) => (emitted = toolId));

    // Simulation : on déclenche l'@Output() open de la card enfant.
    const cardDebug = fixture.debugElement.children[0];
    cardDebug.componentInstance.open.emit();

    expect(emitted).toBe('F-IM-11-changement-statut');
  });

  it('maps theme to correct Material icon', () => {
    expect(component.iconForTheme('INDEMNITES')).toBe('euro');
    expect(component.iconForTheme('VALIDITE')).toBe('verified');
    expect(component.iconForTheme('DELAIS')).toBe('schedule');
    expect(component.iconForTheme('DOCUMENTS')).toBe('description');
    expect(component.iconForTheme('DIAGNOSTIC')).toBe('health_and_safety');
    expect(component.iconForTheme('UNKNOWN_THEME')).toBe('extension');
  });

  it('summaryFromTile builds DecisionToolSummary from DashboardTile fields', () => {
    component.tile = baseTile;
    const s = component.summaryFromTile();
    expect(s.label).toBe('Changement de statut');
    expect(s.primaryValue).toBe('ETUDIANT → VPF (ELEVEE)');
    expect(s.secondaryValue).toBe('8 mois restants');
    expect(s.alertLevel).toBe('OK');
  });

  it('metierAlertFromTile returns null when alertLevel is null', () => {
    component.tile = { ...baseTile, alertLevel: null };
    expect(component.metierAlertFromTile()).toBeNull();
  });

  it('metierAlertFromTile returns null when alertLevel is undefined', () => {
    const { alertLevel: _omitted, ...rest } = baseTile;
    component.tile = rest as DashboardTile;
    expect(component.metierAlertFromTile()).toBeNull();
  });
});
