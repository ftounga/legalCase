import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FourchettesJafSectionComponent } from './fourchettes-jaf-section.component';
import {
  CaseAnalysisResult,
  PensionAlimentaireEstimate,
  PrestationCompensatoireEstimate,
} from '../../core/models/case-analysis.model';

describe('FourchettesJafSectionComponent', () => {
  let fixture: ComponentFixture<FourchettesJafSectionComponent>;
  let component: FourchettesJafSectionComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FourchettesJafSectionComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(FourchettesJafSectionComponent);
    component = fixture.componentInstance;
  });

  it('expose les statics F-177 attendus', () => {
    expect(FourchettesJafSectionComponent.TOOL_LABEL).toBe('FOURCHETTES JURISPRUDENTIELLES JAF');
    expect(FourchettesJafSectionComponent.TOOL_ICON).toBe('analytics');
    expect(FourchettesJafSectionComponent.getPrefillCount()).toBe(0);
  });

  it('rend l\'état vide quand synthesis est null', () => {
    component.synthesis = null;
    fixture.detectChanges();
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(html).toContain('Aucune fourchette jurisprudentielle JAF');
    expect(component.hasData).toBe(false);
  });

  it('rend l\'état vide quand aucune jurisprudenceRange n\'est présente', () => {
    component.synthesis = {
      pensionAlimentaireEstimate: { jurisprudenceRange: null } as unknown as PensionAlimentaireEstimate,
      prestationCompensatoireEstimate: undefined,
    } as unknown as CaseAnalysisResult;
    fixture.detectChanges();
    expect(component.hasData).toBe(false);
  });

  it('rend uniquement la pension alimentaire si seule sa jurisprudenceRange est fournie', () => {
    const pa: PensionAlimentaireEstimate = {
      montantMin: 350,
      montantMax: 550,
      revenus: 2800,
      nbEnfants: 2,
      modeGarde: 'EXCLUSIVE',
      pays: 'FRANCE',
      donneesPartielles: false,
      jurisprudenceRange: {
        p25: 320,
        p50: 460,
        p75: 600,
        label: 'JAF Paris (2024)',
        sourceRef: 'TJ Paris, JAF, échantillon 2024',
      },
    };
    component.synthesis = { pensionAlimentaireEstimate: pa } as unknown as CaseAnalysisResult;
    fixture.detectChanges();
    expect(component.entries.length).toBe(1);
    expect(component.entries[0].category).toBe('Pension alimentaire');
  });

  it('rend les 2 fourchettes (pension + prestation) quand toutes deux sont fournies', () => {
    const pa: PensionAlimentaireEstimate = {
      montantMin: 350,
      montantMax: 550,
      revenus: 2800,
      nbEnfants: 2,
      modeGarde: 'EXCLUSIVE',
      pays: 'FRANCE',
      donneesPartielles: false,
      jurisprudenceRange: {
        p25: 320, p50: 460, p75: 600,
        label: 'JAF Paris (2024)',
        sourceRef: 'TJ Paris, JAF, échantillon 2024',
      },
    };
    const pc: PrestationCompensatoireEstimate = {
      montantMin: 15000,
      montantMax: 50000,
      ecartRevenus: 2000,
      dureeMarriage: 10,
      pays: 'FRANCE',
      donneesPartielles: false,
      jurisprudenceRange: {
        p25: 18000, p50: 30000, p75: 45000,
        label: 'CA Paris (2024)',
        sourceRef: 'CA Paris, ch. 24, échantillon 2024',
      },
    };
    component.synthesis = {
      pensionAlimentaireEstimate: pa,
      prestationCompensatoireEstimate: pc,
    } as unknown as CaseAnalysisResult;
    fixture.detectChanges();
    expect(component.entries.length).toBe(2);
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(html).toContain('Pension alimentaire');
    expect(html).toContain('Prestation compensatoire');
    expect(html).toContain('p25');
    expect(html).toContain('médiane');
    expect(html).toContain('p75');
  });
});
