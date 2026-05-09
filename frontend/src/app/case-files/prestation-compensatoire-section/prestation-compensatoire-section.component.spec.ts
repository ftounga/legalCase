import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PrestationCompensatoireSectionComponent } from './prestation-compensatoire-section.component';
import { CaseAnalysisResult, PrestationCompensatoireEstimate } from '../../core/models/case-analysis.model';

describe('PrestationCompensatoireSectionComponent', () => {
  let fixture: ComponentFixture<PrestationCompensatoireSectionComponent>;
  let component: PrestationCompensatoireSectionComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrestationCompensatoireSectionComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PrestationCompensatoireSectionComponent);
    component = fixture.componentInstance;
  });

  it('expose les statics F-177 SF-177-03b/12 attendus par le panel F-IA-04', () => {
    expect(PrestationCompensatoireSectionComponent.TOOL_LABEL).toBe('PRESTATION COMPENSATOIRE');
    expect(PrestationCompensatoireSectionComponent.TOOL_ICON).toBe('balance');
    expect(PrestationCompensatoireSectionComponent.getPrefillCount()).toBe(0);
  });

  it('rend l\'état vide quand synthesis est null', () => {
    component.synthesis = null;
    fixture.detectChanges();
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(html).toContain('Aucune donnée de prestation compensatoire');
  });

  it('rend l\'état vide quand prestationCompensatoireEstimate est absent', () => {
    component.synthesis = { prestationCompensatoireEstimate: null } as unknown as CaseAnalysisResult;
    fixture.detectChanges();
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(html).toContain('Aucune donnée de prestation compensatoire');
  });

  it('rend la fourchette quand prestationCompensatoireEstimate est fourni (FRANCE)', () => {
    const estimate: PrestationCompensatoireEstimate = {
      montantMin: 15000,
      montantMax: 50000,
      ecartRevenus: 2000,
      dureeMarriage: 10,
      pays: 'FRANCE',
      donneesPartielles: false,
    };
    component.synthesis = { prestationCompensatoireEstimate: estimate } as unknown as CaseAnalysisResult;
    fixture.detectChanges();
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(html).toContain('Fourchette de capital');
    expect(html).toContain('art. 271 Cciv');
    // Disclaimer art. 271 présent
    expect(html).toContain('art. 271 Cciv');
  });

  it('affiche le bandeau warning quand donneesPartielles=true', () => {
    const estimate: PrestationCompensatoireEstimate = {
      montantMin: 15000,
      montantMax: 50000,
      ecartRevenus: 2000,
      dureeMarriage: 10,
      pays: 'FRANCE',
      donneesPartielles: true,
    };
    component.synthesis = { prestationCompensatoireEstimate: estimate } as unknown as CaseAnalysisResult;
    fixture.detectChanges();
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(html).toContain('Données partielles');
  });

  it('affiche le barème Belgique (coeff. 0.25) quand pays=BELGIQUE', () => {
    const estimate: PrestationCompensatoireEstimate = {
      montantMin: 12000,
      montantMax: 30000,
      ecartRevenus: 1500,
      dureeMarriage: 8,
      pays: 'BELGIQUE',
      donneesPartielles: false,
    };
    component.synthesis = { prestationCompensatoireEstimate: estimate } as unknown as CaseAnalysisResult;
    fixture.detectChanges();
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(html).toContain('Belgique (coeff. 0.25)');
  });

  it('affiche la fourchette jurisprudentielle p25/p50/p75 quand jurisprudenceRange est fourni', () => {
    const estimate: PrestationCompensatoireEstimate = {
      montantMin: 15000,
      montantMax: 50000,
      ecartRevenus: 2000,
      dureeMarriage: 10,
      pays: 'FRANCE',
      donneesPartielles: false,
      jurisprudenceRange: {
        p25: 18000,
        p50: 30000,
        p75: 45000,
        label: 'Cour d\'appel de Paris (2024)',
        sourceRef: 'CA Paris, ch. 24, 12 mars 2024',
      },
    };
    component.synthesis = { prestationCompensatoireEstimate: estimate } as unknown as CaseAnalysisResult;
    fixture.detectChanges();
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(html).toContain('p25');
    expect(html).toContain('médiane');
    expect(html).toContain('p75');
  });
});
