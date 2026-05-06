import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProcedureChecksOutputComponent } from './procedure-checks-output.component';
import { ProcedureCheckAlignment } from '../../../core/models/procedure-check-alignment.model';

const TOOL_ID = 'F-DT-08-licenciement-validity';

function check(matchStatus: ProcedureCheckAlignment['matchStatus'], i = 1): ProcedureCheckAlignment {
  return {
    checkId: `c-${matchStatus}-${i}`,
    libelle: matchStatus === 'NON_COMPLIANT_FLAG'
      ? 'Notification du licenciement par LRAR'
      : matchStatus === 'TO_VERIFY_FLAG'
        ? 'Convocation à entretien préalable'
        : 'Délai de prescription respecté',
    critereCode: 'C-' + matchStatus,
    statut: matchStatus === 'ALIGNED' ? 'VERIFIED'
      : matchStatus === 'NON_COMPLIANT_FLAG' ? 'NON_COMPLIANT'
      : 'TO_CHECK',
    expectedValue: matchStatus === 'ALIGNED' ? 'LRAR' : null,
    raison: matchStatus === 'NON_COMPLIANT_FLAG' ? 'LRAR absente du dossier' : null,
    toolIdCible: TOOL_ID,
    matchStatus,
  };
}

describe('ProcedureChecksOutputComponent — F-193 SF-193-02', () => {
  let fixture: ComponentFixture<ProcedureChecksOutputComponent>;
  let component: ProcedureChecksOutputComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProcedureChecksOutputComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(ProcedureChecksOutputComponent);
    component = fixture.componentInstance;
  });

  it('CA-06 alignement vide → composant ne rend rien', () => {
    component.alignment = [];
    fixture.detectChanges();
    const native: HTMLElement = fixture.nativeElement;
    expect(native.querySelector('[data-testid="procedure-checks-output"]')).toBeNull();
  });

  it('CA-02 ALIGNED → bloc Vérifications confirmées rendu', () => {
    component.alignment = [check('ALIGNED')];
    fixture.detectChanges();
    const native: HTMLElement = fixture.nativeElement;
    const block = native.querySelector('[data-testid="pc-verified-block"]');
    expect(block).not.toBeNull();
    expect(block!.textContent).toContain('Vérifications confirmées par votre avocat');
    expect(block!.textContent).toContain('Délai de prescription respecté');
    expect(block!.textContent).toContain('LRAR');
  });

  it('CA-03 NON_COMPLIANT_FLAG → bloc Points non conformes rendu avec raison', () => {
    component.alignment = [check('NON_COMPLIANT_FLAG')];
    fixture.detectChanges();
    const native: HTMLElement = fixture.nativeElement;
    const block = native.querySelector('[data-testid="pc-non-compliant-block"]');
    expect(block).not.toBeNull();
    expect(block!.textContent).toContain('Points non conformes signalés');
    expect(block!.textContent).toContain('Notification du licenciement par LRAR');
    expect(block!.textContent).toContain('LRAR absente du dossier');
  });

  it('CA-04 TO_VERIFY_FLAG → bloc Points à vérifier rendu', () => {
    component.alignment = [check('TO_VERIFY_FLAG')];
    fixture.detectChanges();
    const native: HTMLElement = fixture.nativeElement;
    const block = native.querySelector('[data-testid="pc-to-verify-block"]');
    expect(block).not.toBeNull();
    expect(block!.textContent).toContain('Points à vérifier');
    expect(block!.textContent).toContain('Convocation à entretien préalable');
  });

  it('CA-05 mix : 3 sous-sections affichées simultanément', () => {
    component.alignment = [
      check('ALIGNED'),
      check('NON_COMPLIANT_FLAG'),
      check('TO_VERIFY_FLAG'),
    ];
    fixture.detectChanges();
    const native: HTMLElement = fixture.nativeElement;
    expect(native.querySelector('[data-testid="pc-verified-block"]')).not.toBeNull();
    expect(native.querySelector('[data-testid="pc-non-compliant-block"]')).not.toBeNull();
    expect(native.querySelector('[data-testid="pc-to-verify-block"]')).not.toBeNull();
  });

  it('DIVERGENT et NOT_ANALYZED ignorés (pas de rendu)', () => {
    component.alignment = [check('DIVERGENT'), check('NOT_ANALYZED')];
    fixture.detectChanges();
    const native: HTMLElement = fixture.nativeElement;
    expect(native.querySelector('[data-testid="procedure-checks-output"]')).toBeNull();
  });
});
