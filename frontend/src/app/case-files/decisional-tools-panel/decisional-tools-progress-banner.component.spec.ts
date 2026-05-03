import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { DecisionalToolsProgressBannerComponent } from './decisional-tools-progress-banner.component';

describe('DecisionalToolsProgressBannerComponent', () => {
  let fixture: ComponentFixture<DecisionalToolsProgressBannerComponent>;
  let component: DecisionalToolsProgressBannerComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DecisionalToolsProgressBannerComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(DecisionalToolsProgressBannerComponent);
    component = fixture.componentInstance;
  });

  it('masque le bandeau quand activeJobTypes est vide', () => {
    component.activeJobTypes = [];
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.banner');
    expect(banner).toBeNull();
  });

  it('affiche le label spécifique pour CASE_ANALYSIS seul', () => {
    component.activeJobTypes = ['CASE_ANALYSIS'];
    fixture.detectChanges();
    const label = fixture.nativeElement.querySelector('.banner__label');
    expect(label?.textContent?.trim()).toBe('Analyse du dossier en cours…');
  });

  it('affiche le label spécifique pour ENRICHED_ANALYSIS seul', () => {
    component.activeJobTypes = ['ENRICHED_ANALYSIS'];
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.banner__label').textContent.trim())
      .toBe('Re-synthèse enrichie en cours…');
  });

  it('affiche le label spécifique pour DOCUMENT_ANALYSIS seul', () => {
    component.activeJobTypes = ['DOCUMENT_ANALYSIS'];
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.banner__label').textContent.trim())
      .toBe('Analyse des documents en cours…');
  });

  it('affiche le label agrégé quand plusieurs jobs simultanés', () => {
    component.activeJobTypes = ['CASE_ANALYSIS', 'DOCUMENT_ANALYSIS'];
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.banner__label').textContent.trim())
      .toBe('Analyses en cours… (2)');
  });

  it('expose role="status" aria-live="polite" pour l\'accessibilité', () => {
    component.activeJobTypes = ['CASE_ANALYSIS'];
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.banner');
    expect(banner.getAttribute('role')).toBe('status');
    expect(banner.getAttribute('aria-live')).toBe('polite');
  });
});
