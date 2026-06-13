import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConclusionGenerationProgressComponent } from './conclusion-generation-progress.component';

describe('ConclusionGenerationProgressComponent', () => {
  let fixture: ComponentFixture<ConclusionGenerationProgressComponent>;
  let component: ConclusionGenerationProgressComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConclusionGenerationProgressComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(ConclusionGenerationProgressComponent);
    component = fixture.componentInstance;
  });

  function el(testid: string): HTMLElement {
    return fixture.nativeElement.querySelector(`[data-testid="${testid}"]`);
  }

  it('rend le pourcentage et la section en cours', () => {
    component.percent = 42;
    component.currentSection = 'II. DISCUSSION';
    component.sectionIndex = 3;
    fixture.detectChanges();

    expect(el('cgp-percent').textContent).toContain('42 %');
    expect(el('cgp-section').textContent).toContain('II. DISCUSSION');
    expect(el('cgp-section').textContent).toContain('section 3');
  });

  it('borne le pourcentage à [0,100] et reflète aria-valuenow', () => {
    component.percent = 250;
    fixture.detectChanges();
    expect(component.clampedPercent).toBe(100);
    const bar = el('cgp-bar');
    expect(bar.getAttribute('aria-valuenow')).toBe('100');

    component.percent = -10;
    fixture.detectChanges();
    expect(component.clampedPercent).toBe(0);
  });

  it('affiche un libellé d’attente honnête avant le 1er titre détecté', () => {
    component.percent = 5;
    component.currentSection = null;
    fixture.detectChanges();
    expect(el('cgp-section').textContent).toContain('Préparation');
  });

  it('utilise la barre en mode déterminé', () => {
    component.percent = 30;
    fixture.detectChanges();
    const bar = el('cgp-bar');
    // mat-progress-bar déterminé → aria-valuenow présent (mode indeterminate l'omet)
    expect(bar.getAttribute('aria-valuenow')).toBe('30');
  });
});
