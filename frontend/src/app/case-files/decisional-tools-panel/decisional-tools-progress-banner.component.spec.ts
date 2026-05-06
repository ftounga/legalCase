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

  // F-190 SF-190-03 — sous-ligne "X/Y sections reçues" pendant le streaming d'une analyse standard / enrichie.
  describe('sous-ligne "X/Y sections reçues" (SF-190-03)', () => {
    it('affiche "3/7 sections reçues" pendant CASE_ANALYSIS', () => {
      component.activeJobTypes = ['CASE_ANALYSIS'];
      component.sectionsReceived = 3;
      component.sectionsExpected = 7;
      fixture.detectChanges();
      const sub = fixture.nativeElement.querySelector('.banner__sections');
      expect(sub?.textContent?.trim()).toBe('3/7 sections reçues');
    });

    it('affiche la sous-ligne aussi pendant ENRICHED_ANALYSIS', () => {
      component.activeJobTypes = ['ENRICHED_ANALYSIS'];
      component.sectionsReceived = 5;
      component.sectionsExpected = 7;
      fixture.detectChanges();
      const sub = fixture.nativeElement.querySelector('.banner__sections');
      expect(sub?.textContent?.trim()).toBe('5/7 sections reçues');
    });

    it('masque la sous-ligne quand sectionsReceived est null', () => {
      component.activeJobTypes = ['CASE_ANALYSIS'];
      component.sectionsReceived = null;
      component.sectionsExpected = 7;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.banner__sections')).toBeNull();
    });

    it('masque la sous-ligne quand sectionsExpected vaut 0', () => {
      component.activeJobTypes = ['CASE_ANALYSIS'];
      component.sectionsReceived = 2;
      component.sectionsExpected = 0;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.banner__sections')).toBeNull();
    });

    it('masque la sous-ligne pour DOCUMENT_ANALYSIS seul (pas de sections)', () => {
      component.activeJobTypes = ['DOCUMENT_ANALYSIS'];
      component.sectionsReceived = 4;
      component.sectionsExpected = 7;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.banner__sections')).toBeNull();
    });
  });
});
