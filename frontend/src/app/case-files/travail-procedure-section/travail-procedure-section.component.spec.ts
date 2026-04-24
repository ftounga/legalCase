import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { TravailProcedureSectionComponent } from './travail-procedure-section.component';
import { TravailProcedureJalon } from '../../core/models/travail-procedure.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('TravailProcedureSectionComponent', () => {
  let component: TravailProcedureSectionComponent;
  let fixture: ComponentFixture<TravailProcedureSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  function urlWith(typeProcedure: string, dateDeclencheur?: string): string {
    let url = `/api/v1/case-files/case-1/travail-procedure-jalons?typeProcedure=${typeProcedure}`;
    if (dateDeclencheur) url += `&dateDeclencheur=${dateDeclencheur}`;
    return url;
  }

  function frJalons(): TravailProcedureJalon[] {
    return [
      { code: 'PRUDHOMMES_FR', label: 'Convocation BCO', offsetDays: 45,
        articleRef: 'Code travail Art. R.1452-3', dateCible: null },
      { code: 'PRUDHOMMES_FR', label: 'Audience BCO', offsetDays: 90,
        articleRef: 'Code travail Art. L.1454-1', dateCible: null },
      { code: 'PRUDHOMMES_FR', label: 'Renvoi bureau de jugement', offsetDays: 180,
        articleRef: 'Code travail Art. L.1454-1-2', dateCible: null },
    ];
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [
        TravailProcedureSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TravailProcedureSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('mounts without error', () => {
    component.ngOnInit();
    expect(component).toBeTruthy();
    expect(component.jalons().length).toBe(0);
  });

  it('FRANCE → 3 options FR exposées', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    expect(component.options().length).toBe(3);
    const codes = component.options().map(o => o.code);
    expect(codes).toEqual(['PRUDHOMMES_FR', 'APPEL_CA_SOCIALE_FR', 'CASSATION_SOCIALE_FR']);
  });

  it('BELGIQUE → 3 options BE exposées', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    expect(component.options().length).toBe(3);
    const codes = component.options().map(o => o.code);
    expect(codes).toEqual(['TRIBUNAL_TRAVAIL_BE', 'COUR_TRAVAIL_BE', 'CASSATION_BE']);
  });

  it('bannière info → libellé "françaises" pour workspace FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    expect(component.countryBannerLabel()).toContain('françaises');
  });

  it('bannière info → libellé "belges" pour workspace BELGIQUE', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    expect(component.countryBannerLabel()).toContain('belges');
  });

  it('formValid → false sans typeProcedure, true avec typeProcedure', () => {
    component.ngOnInit();
    expect(component.formValid()).toBe(false);
    component.onTypeProcedureChange('PRUDHOMMES_FR');
    expect(component.formValid()).toBe(true);
  });

  it('calculate() → GET succès → jalons affichés + dashboardRefresh appelé', () => {
    component.ngOnInit();
    component.onTypeProcedureChange('PRUDHOMMES_FR');
    component.calculate();

    const req = httpMock.expectOne(urlWith('PRUDHOMMES_FR'));
    expect(req.request.method).toBe('GET');
    req.flush(frJalons());

    expect(component.jalons().length).toBe(3);
    expect(component.hasResult()).toBe(true);
    expect(component.loading()).toBe(false);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() avec dateDeclencheur → query param présent', () => {
    component.ngOnInit();
    component.onTypeProcedureChange('PRUDHOMMES_FR');
    component.onDateDeclencheurChange('2026-03-01');
    component.calculate();

    const req = httpMock.expectOne(urlWith('PRUDHOMMES_FR', '2026-03-01'));
    req.flush(frJalons());
    expect(component.jalons().length).toBe(3);
  });

  it('calculate() → liste vide → message "Aucun jalon"', () => {
    component.ngOnInit();
    component.onTypeProcedureChange('PRUDHOMMES_FR');
    component.calculate();

    const req = httpMock.expectOne(urlWith('PRUDHOMMES_FR'));
    req.flush([]);

    expect(component.jalons().length).toBe(0);
    expect(component.hasResult()).toBe(true);
  });

  it('calculate() → erreur HTTP → snackbar appelée', () => {
    component.ngOnInit();
    component.onTypeProcedureChange('PRUDHOMMES_FR');
    component.calculate();

    const req = httpMock.expectOne(urlWith('PRUDHOMMES_FR'));
    req.flush({ message: 'Erreur serveur' }, { status: 500, statusText: 'Internal Server Error' });

    expect(snackSpy.open).toHaveBeenCalled();
    expect(component.loading()).toBe(false);
    expect(component.hasResult()).toBe(false);
  });

  it('pré-fill IA depuis aiData.procedureTravailDetectee → typeProcedure sélectionné + provenance IA', () => {
    component.aiData = {
      // Champ non typé sur TravailExtractedData — accès via cast dans le composant.
      procedureTravailDetectee: 'APPEL_CA_SOCIALE_FR',
    } as any;
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();

    expect(component.typeProcedure()).toBe('APPEL_CA_SOCIALE_FR');
    expect(component.provenanceTypeProcedure()).toBe('IA');
  });

  it('pré-fill IA ignore code BE si workspace FRANCE', () => {
    component.aiData = {
      procedureTravailDetectee: 'TRIBUNAL_TRAVAIL_BE',
    } as any;
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();

    expect(component.typeProcedure()).toBeNull();
    expect(component.provenanceTypeProcedure()).toBeNull();
  });

  it('changement manuel typeProcedure → provenance IA effacée', () => {
    component.aiData = { procedureTravailDetectee: 'PRUDHOMMES_FR' } as any;
    component.ngOnInit();
    expect(component.provenanceTypeProcedure()).toBe('IA');

    component.onTypeProcedureChange('CASSATION_SOCIALE_FR');
    expect(component.typeProcedure()).toBe('CASSATION_SOCIALE_FR');
    expect(component.provenanceTypeProcedure()).toBeNull();
  });

  it('toggle collapse expand/collapse correct', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('coherenceAlerts → entrée TYPE_PROCEDURE si procedureChecks divergent', () => {
    component.workspaceCountry = 'FRANCE';
    component.procedureChecks = [{
      id: 'p1', ordre: 1, description: '...', statut: 'NON_COMPLIANT',
      critereCode: 'TRAVAIL_PROCEDURE_TYPE',
      expectedValue: 'CASSATION_SOCIALE_FR',
      raison: 'Type de procédure attendu différent',
    }];
    component.ngOnInit();
    component.onTypeProcedureChange('PRUDHOMMES_FR');

    const alerts = component.coherenceAlerts();
    expect(alerts.TYPE_PROCEDURE).toBeTruthy();
    expect(alerts.TYPE_PROCEDURE!.severity).toBe('WARNING');
    expect(alerts.TYPE_PROCEDURE!.expectedDisplay).toContain('Pourvoi en cassation');
  });

  it('ngOnChanges aiData → re-prefill si pas encore saisi', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    expect(component.typeProcedure()).toBeNull();

    component.aiData = { procedureTravailDetectee: 'PRUDHOMMES_FR' } as any;
    component.ngOnChanges({
      aiData: new SimpleChange(null, component.aiData, false),
    });

    expect(component.typeProcedure()).toBe('PRUDHOMMES_FR');
    expect(component.provenanceTypeProcedure()).toBe('IA');
  });
});
