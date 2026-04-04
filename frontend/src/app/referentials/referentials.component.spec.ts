import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ReferentialsComponent } from './referentials.component';
import { ReferentialService } from '../core/services/referential.service';
import { WorkspaceService } from '../core/services/workspace.service';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of, throwError } from 'rxjs';
import { ReferentialResponse } from '../core/models/referential.model';

const mockWorkspace = {
  id: 'ws1', name: 'Cabinet Test', slug: 'cabinet-test',
  planCode: 'STARTER', status: 'ACTIVE', legalDomain: 'DROIT_DU_TRAVAIL'
};

const mockResponse: ReferentialResponse = {
  domain: 'DROIT_DU_TRAVAIL',
  sections: {
    LITIGATION_TYPE: [
      { key: 'DISCRIMINATION', label: 'Discrimination', valueJson: '{"years":5,"article":"Art. L1132-1"}', isSystem: true, sourceRef: 'Art. L1132-1' }
    ],
    BAREME_MACRON: [
      { key: 'LICENCIEMENT', label: 'Licenciement', valueJson: '{"supported":true}', isSystem: true }
    ]
  }
};

function buildTestBed(referentialReturn: any) {
  const referentialServiceSpy = { getReferentials: jest.fn().mockReturnValue(referentialReturn) };
  const workspaceServiceSpy = { getCurrentWorkspace: jest.fn().mockReturnValue(of(mockWorkspace)) };
  return TestBed.configureTestingModule({
    imports: [ReferentialsComponent],
    providers: [
      provideAnimationsAsync(),
      { provide: ReferentialService, useValue: referentialServiceSpy },
      { provide: WorkspaceService, useValue: workspaceServiceSpy },
    ]
  });
}

describe('ReferentialsComponent — cas nominal', () => {
  let fixture: ComponentFixture<ReferentialsComponent>;
  let component: ReferentialsComponent;

  beforeEach(async () => {
    await buildTestBed(of(mockResponse)).compileComponents();
    fixture = TestBed.createComponent(ReferentialsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // REF-UI-01 : panels affichés pour chaque section
  it('REF-UI-01: affiche un panel pour chaque section retournée', () => {
    const panels = fixture.nativeElement.querySelectorAll('mat-expansion-panel');
    expect(panels.length).toBe(2);
  });

  // REF-UI-02 : titre lisible pour LITIGATION_TYPE
  it('REF-UI-02: affiche "Types de litiges" pour la section LITIGATION_TYPE', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Types de litiges');
  });

  // REF-UI-03 : titre lisible pour BAREME_MACRON
  it('REF-UI-03: affiche "Barème Macron" pour la section BAREME_MACRON', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Barème Macron');
  });

  // REF-UI-06 : formatValue LITIGATION_TYPE
  it('REF-UI-06: formatValue pour LITIGATION_TYPE retourne "X an(s) — Art. XXX"', () => {
    const entry = { key: 'DISCRIMINATION', label: 'Discrimination', valueJson: '{"years":5,"article":"Art. L1132-1"}', isSystem: true };
    expect(component.formatValue(entry, 'LITIGATION_TYPE')).toBe('5 ans — Art. L1132-1');
  });

  // REF-UI-07 : formatValue PRESTATION_COEFF
  it('REF-UI-07: formatValue pour PRESTATION_COEFF retourne coefficient formaté', () => {
    const entry = { key: 'FRANCE', label: 'Coeff PC', valueJson: '{"coeff":0.30,"dureeReferenceAns":8}', isSystem: true };
    const result = component.formatValue(entry, 'PRESTATION_COEFF');
    expect(result).toContain('30 %');
    expect(result).toContain('8 ans');
  });
});

describe('ReferentialsComponent — erreur API', () => {
  let fixture: ComponentFixture<ReferentialsComponent>;

  beforeEach(async () => {
    await buildTestBed(throwError(() => new Error('500'))).compileComponents();
    fixture = TestBed.createComponent(ReferentialsComponent);
    fixture.detectChanges();
  });

  // REF-UI-04 : message d'erreur si API échoue
  it('REF-UI-04: affiche un message d\'erreur si l\'API échoue', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Impossible de charger');
  });
});

describe('ReferentialsComponent — sections vides', () => {
  let fixture: ComponentFixture<ReferentialsComponent>;

  beforeEach(async () => {
    await buildTestBed(of({ domain: 'DROIT_DU_TRAVAIL', sections: {} })).compileComponents();
    fixture = TestBed.createComponent(ReferentialsComponent);
    fixture.detectChanges();
  });

  // REF-UI-05 : "Aucun référentiel disponible" si sections vides
  it('REF-UI-05: affiche "Aucun référentiel disponible" si sections vides', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Aucun référentiel disponible');
  });
});
