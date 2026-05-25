import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AcceptationRenonciationSectionComponent } from './acceptation-renonciation-section.component';
import { AcceptationRenonciationSuccessionResponse } from '../../core/models/acceptation-renonciation-succession.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('AcceptationRenonciationSectionComponent', () => {
  let component: AcceptationRenonciationSectionComponent;
  let fixture: ComponentFixture<AcceptationRenonciationSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/acceptation-renonciation-succession';

  function pureSimpleResponse(): AcceptationRenonciationSuccessionResponse {
    return {
      caseFileId: 'case-1',
      dateOuvertureSuccession: '2026-04-01',
      qualiteHeritier: 'PREMIER_RANG',
      actifBrutEur: 250000,
      passifEur: 0,
      actesEquivalentAcceptationDejaPosesDetected: false,
      inventaireRealise: false,
      dettesIncertainesDetected: false,
      intentionExprimee: 'INCERTAIN',
      optionsOuvertes: ['ACCEPTATION_PURE_SIMPLE', 'ACCEPTATION_CONCURRENCE_ACTIF', 'RENONCIATION'],
      optionRecommandee: 'ACCEPTATION_PURE_SIMPLE',
      delaiRestantJours: 90,
      delaiTotalJours: 120,
      baseJuridique: 'Cciv art. 768+ + 771-772',
      formule: 'Actif 250000 € — passif 0 €',
      messages: ['Succession ouverte'],
      country: 'FRANCE',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        AcceptationRenonciationSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AcceptationRenonciationSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('mount FRANCE → GET initial 404 → showForm reste true', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
  });

  it('GET 200 → restore les champs form + showForm=false', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(pureSimpleResponse());
    expect(component.result()!.optionRecommandee).toBe('ACCEPTATION_PURE_SIMPLE');
    expect(component.showForm()).toBe(false);
    expect(component.dateOuvertureSuccession()).toBe('2026-04-01');
    expect(component.actifBrutEur()).toBe(250000);
  });

  it('POST nominal → result + showForm=false + refresh + snack', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onDateOuvertureChange('2026-04-01');
    component.onActifChange(250000);
    component.onPassifChange(0);
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dateOuvertureSuccession).toBe('2026-04-01');
    req.flush(pureSimpleResponse());

    expect(component.result()!.optionRecommandee).toBe('ACCEPTATION_PURE_SIMPLE');
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('prefill IA — date + actif + passif + qualité appliqués après 404', () => {
    component.aiData = {
      dateOuvertureSuccessionDetectee: '2026-04-01',
      actifBrutSuccessionEurDetecte: 200000,
      passifSuccessionEurDetecte: 30000,
      qualiteHeritierDetectee: 'PREMIER_RANG',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dateOuvertureSuccession()).toBe('2026-04-01');
    expect(component.provenanceDateOuverture()).toBe('IA');
    expect(component.actifBrutEur()).toBe(200000);
    expect(component.provenanceActifBrut()).toBe('IA');
    expect(component.passifEur()).toBe(30000);
  });

  it('formValid retourne true si date + actif + passif renseignés', () => {
    expect(component.formValid()).toBe(false);
    component.onDateOuvertureChange('2026-04-01');
    component.onActifChange(100000);
    component.onPassifChange(0);
    expect(component.formValid()).toBe(true);
  });

  it('delaiBadgeClass — danger si négatif, warn si <=30, ok sinon', () => {
    expect(component.delaiBadgeClass(-5)).toContain('danger');
    expect(component.delaiBadgeClass(15)).toContain('warn');
    expect(component.delaiBadgeClass(60)).toContain('ok');
  });

  it('isFrance() — false en BELGIQUE', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.isFrance()).toBe(false);
  });

  it('getPrefillCount — 0 si aiData absent', () => {
    expect(AcceptationRenonciationSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount — 4 avec date + actif + passif + qualité', () => {
    expect(AcceptationRenonciationSectionComponent.getPrefillCount({
      aiData: {
        dateOuvertureSuccessionDetectee: '2026-04-01',
        actifBrutSuccessionEurDetecte: 200000,
        passifSuccessionEurDetecte: 30000,
        qualiteHeritierDetectee: 'PREMIER_RANG',
      } as FamilleExtractedData,
    })).toBe(4);
  });
});
