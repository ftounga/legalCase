import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { PensionAlimentaireSectionComponent } from './pension-alimentaire-section.component';
import { CaseAnalysisResult, PensionAlimentaireEstimate } from '../../core/models/case-analysis.model';

describe('PensionAlimentaireSectionComponent', () => {
  let fixture: ComponentFixture<PensionAlimentaireSectionComponent>;
  let component: PensionAlimentaireSectionComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PensionAlimentaireSectionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(PensionAlimentaireSectionComponent);
    component = fixture.componentInstance;
  });

  it('expose les statics F-177 attendus par le panel F-IA-04', () => {
    expect(PensionAlimentaireSectionComponent.TOOL_LABEL).toBe('PENSION ALIMENTAIRE');
    expect(PensionAlimentaireSectionComponent.TOOL_ICON).toBe('family_restroom');
    expect(PensionAlimentaireSectionComponent.getPrefillCount()).toBe(0);
  });

  it('rend l\'état vide quand synthesis est null', () => {
    component.synthesis = null;
    fixture.detectChanges();
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(html).toContain('Aucune donnée de pension alimentaire');
  });

  it('rend la fourchette quand pensionAlimentaireEstimate est fourni (FRANCE, garde EXCLUSIVE)', () => {
    const estimate: PensionAlimentaireEstimate = {
      montantMin: 350,
      montantMax: 550,
      revenus: 2800,
      nbEnfants: 2,
      modeGarde: 'EXCLUSIVE',
      pays: 'FRANCE',
      donneesPartielles: false,
    };
    component.synthesis = { pensionAlimentaireEstimate: estimate } as unknown as CaseAnalysisResult;
    fixture.detectChanges();
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(html).toContain('Fourchette mensuelle');
    expect(html).toContain('UNAF France');
    expect(html).toContain('Garde exclusive');
  });

  it('rend le barème CGKR Belgique avec garde alternée', () => {
    const estimate: PensionAlimentaireEstimate = {
      montantMin: 280,
      montantMax: 420,
      revenus: 2500,
      nbEnfants: 1,
      modeGarde: 'ALTERNEE',
      pays: 'BELGIQUE',
      donneesPartielles: false,
    };
    component.synthesis = { pensionAlimentaireEstimate: estimate } as unknown as CaseAnalysisResult;
    fixture.detectChanges();
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(html).toContain('CGKR Belgique');
    expect(html).toContain('Garde alternée');
  });

  it('affiche le bandeau warning quand donneesPartielles=true', () => {
    const estimate: PensionAlimentaireEstimate = {
      montantMin: 350,
      montantMax: 550,
      revenus: 2800,
      nbEnfants: 2,
      modeGarde: 'EXCLUSIVE',
      pays: 'FRANCE',
      donneesPartielles: true,
    };
    component.synthesis = { pensionAlimentaireEstimate: estimate } as unknown as CaseAnalysisResult;
    fixture.detectChanges();
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(html).toContain('Données partielles');
  });
});
