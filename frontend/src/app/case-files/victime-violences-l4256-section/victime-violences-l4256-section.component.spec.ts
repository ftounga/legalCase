import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';

import { VictimeViolencesL4256SectionComponent } from './victime-violences-l4256-section.component';
import { VictimeViolencesL4256Response } from '../../core/models/victime-violences-l4256.model';

describe('VictimeViolencesL4256SectionComponent', () => {
  let component: VictimeViolencesL4256SectionComponent;
  let fixture: ComponentFixture<VictimeViolencesL4256SectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/victime-violences-l4256-analysis';

  function frResponse(
    verdict: 'ELIGIBLE_PLEIN_DROIT' | 'ELIGIBLE_SOUS_RESERVE' | 'NON_ELIGIBLE' = 'ELIGIBLE_PLEIN_DROIT',
    overrides: Partial<VictimeViolencesL4256Response> = {},
  ): VictimeViolencesL4256Response {
    return {
      caseFileId: 'case-1',
      dateOrdonnanceProtection: '2026-03-01',
      juridiction: 'JAF Paris',
      dureeProtectionMois: 6,
      dateExpirationProtectionEffective: '2026-09-01',
      enfantsAcharge: 2,
      nationalite: 'Marocaine',
      country: 'FRANCE',
      eligibiliteScore: verdict,
      criteresValides: ['Ordonnance JAF rendue le 2026-03-01'],
      criteresManquants: [],
      dureeTitreSejourMois: 12,
      formule: 'Demande L.425-6 - verdict ELIGIBLE',
      baseJuridique: 'CESEDA L.425-6+, Cciv 515-9+',
      messages: ['Carte de sejour VPF de plein droit'],
      ...overrides,
    };
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [VictimeViolencesL4256SectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(VictimeViolencesL4256SectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('TOOL_LABEL and TOOL_ICON statics', () => {
    expect(VictimeViolencesL4256SectionComponent.TOOL_LABEL).toContain('VICTIME');
    expect(VictimeViolencesL4256SectionComponent.TOOL_ICON).toBe('shield');
  });

  it('static getPrefillCount returns 0 (no IA signal V1)', () => {
    expect(VictimeViolencesL4256SectionComponent.getPrefillCount({})).toBe(0);
    expect(VictimeViolencesL4256SectionComponent.getPrefillCount({
      aiData: { dateNotificationOqtf: '2026-04-01' },
    })).toBe(0);
    expect(VictimeViolencesL4256SectionComponent.getPrefillCount({
      aiData: { dateNotificationOqtf: '2026-04-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('FRANCE -> GET', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'NF' });
  });

  it('BELGIQUE -> no HTTP', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('GET 200 -> loads result', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse('ELIGIBLE_PLEIN_DROIT'));
    expect(component.result()!.eligibiliteScore).toBe('ELIGIBLE_PLEIN_DROIT');
    expect(component.juridiction()).toBe('JAF Paris');
    expect(component.dureeProtectionMois()).toBe(6);
    expect(component.enfantsAcharge()).toBe(2);
    expect(component.showForm()).toBe(false);
  });

  it('GET 404 -> form mode', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
  });

  it('formValid requires date + juridiction + duree > 0 + enfants >= 0', () => {
    component.dateOrdonnanceProtection.set('2026-03-01');
    component.juridiction.set('JAF Paris');
    component.dureeProtectionMois.set(6);
    component.enfantsAcharge.set(0);
    expect(component.formValid()).toBe(true);
    component.juridiction.set(null);
    expect(component.formValid()).toBe(false);
    component.juridiction.set('   ');
    expect(component.formValid()).toBe(false);
    component.juridiction.set('JAF Paris');
    component.dureeProtectionMois.set(0);
    expect(component.formValid()).toBe(false);
    component.dureeProtectionMois.set(6);
    component.enfantsAcharge.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid rejects future date', () => {
    const future = new Date();
    future.setDate(future.getDate() + 5);
    component.dateOrdonnanceProtection.set(future.toISOString().slice(0, 10));
    component.juridiction.set('JAF Paris');
    expect(component.formValid()).toBe(false);
  });

  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    component.dateOrdonnanceProtection.set('2026-03-01');
    component.juridiction.set('JAF Paris');
    component.dureeProtectionMois.set(6);
    component.enfantsAcharge.set(2);
    component.nationalite.set('Marocaine');
    component.analyze();
    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateOrdonnanceProtection: '2026-03-01',
      juridiction: 'JAF Paris',
      dureeProtectionMois: 6,
      dateExpirationProtection: null,
      enfantsAcharge: 2,
      nationalite: 'Marocaine',
    });
    req.flush(frResponse('ELIGIBLE_PLEIN_DROIT'));
    expect(component.result()!.eligibiliteScore).toBe('ELIGIBLE_PLEIN_DROIT');
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() error -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.dateOrdonnanceProtection.set('2026-03-01');
    component.juridiction.set('JAF Paris');
    component.analyze();
    httpMock.expectOne(r => r.method === 'POST').flush({ message: 'Bad' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String), 'Fermer', jasmine.objectContaining({ panelClass: 'snack-error' })
    );
  });

  it('verdict ELIGIBLE_PLEIN_DROIT -> success', () => {
    expect(component.verdictClass('ELIGIBLE_PLEIN_DROIT')).toContain('vvl-banner--success');
    expect(component.verdictIcon('ELIGIBLE_PLEIN_DROIT')).toBe('verified');
    expect(component.verdictLabel('ELIGIBLE_PLEIN_DROIT')).toContain('plein droit');
  });

  it('verdict ELIGIBLE_SOUS_RESERVE -> warning', () => {
    expect(component.verdictClass('ELIGIBLE_SOUS_RESERVE')).toContain('vvl-banner--warning');
    expect(component.verdictIcon('ELIGIBLE_SOUS_RESERVE')).toBe('warning');
    expect(component.verdictLabel('ELIGIBLE_SOUS_RESERVE')).toContain('sous reserve');
  });

  it('verdict NON_ELIGIBLE -> danger', () => {
    expect(component.verdictClass('NON_ELIGIBLE')).toContain('vvl-banner--danger');
    expect(component.verdictIcon('NON_ELIGIBLE')).toBe('error');
    expect(component.verdictLabel('NON_ELIGIBLE')).toContain('Non eligible');
  });

  it('coherenceAlerts : divergence F-96 -> WARNING', () => {
    component.procedureChecks = [
      { id: 'p1', ordre: 1, description: 'Ordonnance', statut: 'NON_COMPLIANT',
        critereCode: 'IM24_DATE_ORDONNANCE_PROTECTION', expectedValue: '2026-03-01' },
    ];
    component.ngOnInit();
    flush404();
    component.dateOrdonnanceProtection.set('2026-03-05');
    component.juridiction.set('JAF Paris');
    const alert = component.coherenceAlerts().DATE_ORDONNANCE;
    expect(alert).toBeDefined();
    expect(alert!.severity).toBe('WARNING');
    expect(alert!.expectedDisplay).toBe('2026-03-01');
  });

  it('no alerts when showForm=false', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());
    expect(component.coherenceAlerts()).toEqual({});
  });

  it('onDateOrdonnanceChange clears provenance', () => {
    component.provenanceDateOrdonnance.set('IA');
    component.onDateOrdonnanceChange('2026-03-05');
    expect(component.provenanceDateOrdonnance()).toBeNull();
  });

  it('toggleCollapse + editMode', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });
});
