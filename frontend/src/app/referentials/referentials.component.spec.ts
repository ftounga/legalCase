import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ReferentialsComponent } from './referentials.component';
import { ReferentialService } from '../core/services/referential.service';
import { WorkspaceService } from '../core/services/workspace.service';
import { WorkspaceMemberService } from '../core/services/workspace-member.service';
import { AuthService } from '../core/services/auth.service';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
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

function buildTestBed(referentialReturn: any, memberRole = 'OWNER') {
  const referentialServiceSpy = {
    getReferentials: jest.fn().mockReturnValue(referentialReturn),
    updateReferential: jest.fn(),
    getPendingAlertsCount: jest.fn().mockReturnValue(of({ count: 0 })),
    reportAnomaly: jest.fn().mockReturnValue(of(undefined)),
  };
  const workspaceServiceSpy = { getCurrentWorkspace: jest.fn().mockReturnValue(of(mockWorkspace)) };
  const workspaceMemberServiceSpy = {
    getMembers: jest.fn().mockReturnValue(of([{ userId: 'u1', memberRole }]))
  };
  const authServiceSpy = { currentUser: jest.fn().mockReturnValue({ id: 'u1' }) };
  const dialogSpy = { open: jest.fn() };
  const snackBarSpy = { open: jest.fn() };

  return TestBed.configureTestingModule({
    imports: [ReferentialsComponent],
    providers: [
      provideAnimationsAsync(),
      { provide: ReferentialService, useValue: referentialServiceSpy },
      { provide: WorkspaceService, useValue: workspaceServiceSpy },
      { provide: WorkspaceMemberService, useValue: workspaceMemberServiceSpy },
      { provide: AuthService, useValue: authServiceSpy },
      { provide: MatDialog, useValue: dialogSpy },
      { provide: MatSnackBar, useValue: snackBarSpy },
    ]
  });
}

describe('ReferentialsComponent — cas nominal (OWNER)', () => {
  let fixture: ComponentFixture<ReferentialsComponent>;
  let component: ReferentialsComponent;

  beforeEach(async () => {
    await buildTestBed(of(mockResponse), 'OWNER').compileComponents();
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

describe('ReferentialsComponent — MEMBER (pas de bouton Modifier)', () => {
  let fixture: ComponentFixture<ReferentialsComponent>;

  beforeEach(async () => {
    await buildTestBed(of(mockResponse), 'MEMBER').compileComponents();
    fixture = TestBed.createComponent(ReferentialsComponent);
    fixture.detectChanges();
  });

  // REF-UI-08 : bouton Modifier absent pour MEMBER
  it('REF-UI-08: bouton "Modifier" absent pour MEMBER', () => {
    const editBtns = fixture.nativeElement.querySelectorAll('.edit-btn');
    expect(editBtns.length).toBe(0);
  });
});

describe('ReferentialsComponent — OWNER (pas de bouton Signaler)', () => {
  let fixture: ComponentFixture<ReferentialsComponent>;

  beforeEach(async () => {
    await buildTestBed(of(mockResponse), 'OWNER').compileComponents();
    fixture = TestBed.createComponent(ReferentialsComponent);
    fixture.detectChanges();
  });

  // REF-UI-09 : bouton Signaler absent pour OWNER
  it('REF-UI-09: bouton "Signaler" absent pour OWNER', () => {
    const reportBtns = fixture.nativeElement.querySelectorAll('.report-btn');
    expect(reportBtns.length).toBe(0);
  });
});

describe('ReferentialsComponent — MEMBER (bouton Signaler visible)', () => {
  let fixture: ComponentFixture<ReferentialsComponent>;
  let component: ReferentialsComponent;

  beforeEach(async () => {
    await buildTestBed(of(mockResponse), 'MEMBER').compileComponents();
    fixture = TestBed.createComponent(ReferentialsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // REF-UI-10 : bouton Signaler présent pour MEMBER
  it('REF-UI-10: bouton "Signaler" présent pour MEMBER', () => {
    const reportBtns = fixture.nativeElement.querySelectorAll('.report-btn');
    expect(reportBtns.length).toBeGreaterThan(0);
  });

  // REF-UI-11 : canReport = true pour MEMBER
  it('REF-UI-11: canReport = true pour MEMBER', () => {
    expect(component.canReport()).toBe(true);
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
