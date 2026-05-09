import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LiquidationCommunauteSectionComponent } from './liquidation-communaute-section.component';
import { CaseAnalysisResult, LiquidationCommunaute } from '../../core/models/case-analysis.model';

describe('LiquidationCommunauteSectionComponent', () => {
  let fixture: ComponentFixture<LiquidationCommunauteSectionComponent>;
  let component: LiquidationCommunauteSectionComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LiquidationCommunauteSectionComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LiquidationCommunauteSectionComponent);
    component = fixture.componentInstance;
  });

  it('expose les statics F-177 attendus', () => {
    expect(LiquidationCommunauteSectionComponent.TOOL_LABEL).toBe('LIQUIDATION DE COMMUNAUTÉ');
    expect(LiquidationCommunauteSectionComponent.TOOL_ICON).toBe('account_balance');
    expect(LiquidationCommunauteSectionComponent.getPrefillCount()).toBe(0);
  });

  it('rend l\'état vide quand synthesis est null', () => {
    component.synthesis = null;
    fixture.detectChanges();
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(html).toContain('Aucun inventaire de communauté');
  });

  it('rend les 4 sections quand liquidationCommunaute est fourni', () => {
    const liquidation: LiquidationCommunaute = {
      regimeMatrimonial: 'COMMUNAUTE_LEGALE',
      actifCommun: [
        { libelle: 'Maison principale', valeur: 300000 },
        { libelle: 'Compte joint', valeur: 12000 },
      ],
      biensPropresEpouxA: [{ libelle: 'Héritage parents A', valeur: 50000 }],
      biensPropresEpouxB: [],
      passifCommun: [{ libelle: 'Crédit immobilier', valeur: 120000 }],
    };
    component.synthesis = { liquidationCommunaute: liquidation } as unknown as CaseAnalysisResult;
    fixture.detectChanges();
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(html).toContain('Communauté légale');
    expect(html).toContain('Actif commun');
    expect(html).toContain('Maison principale');
    expect(html).toContain('Biens propres époux A');
    expect(html).toContain('Biens propres époux B');
    expect(html).toContain('Aucun bien détecté'); // section vide
    expect(html).toContain('Passif commun');
    expect(html).toContain('Crédit immobilier');
  });

  it('formate "Non détecté" quand regimeMatrimonial est null', () => {
    const liquidation: LiquidationCommunaute = {
      regimeMatrimonial: null,
      actifCommun: [],
      biensPropresEpouxA: [],
      biensPropresEpouxB: [],
      passifCommun: [],
    };
    component.synthesis = { liquidationCommunaute: liquidation } as unknown as CaseAnalysisResult;
    fixture.detectChanges();
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(html).toContain('Non détecté');
  });

  it('affiche "—" quand un bien n\'a pas de valeur', () => {
    const liquidation: LiquidationCommunaute = {
      regimeMatrimonial: 'COMMUNAUTE_LEGALE',
      actifCommun: [{ libelle: 'Bien sans valeur', valeur: null }],
      biensPropresEpouxA: [],
      biensPropresEpouxB: [],
      passifCommun: [],
    };
    component.synthesis = { liquidationCommunaute: liquidation } as unknown as CaseAnalysisResult;
    fixture.detectChanges();
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(html).toContain('—');
  });
});
