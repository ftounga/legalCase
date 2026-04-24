import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { SimpleChange } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Belgian9terSectionComponent } from './belgian-9ter-section.component';
import { Belgian9terResponse } from '../../core/models/belgian-9ter.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('Belgian9terSectionComponent', () => {
  let component: Belgian9terSectionComponent;
  let fixture: ComponentFixture<Belgian9terSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/belgian-9ter';

  function beResponse(overrides: Partial<Belgian9terResponse> = {}): Belgian9terResponse {
    return {
      caseFileId: 'case-1',
      dateDebutSymptomes: '2026-01-10',
      maladieGraveCertifiee: true,
      soinsNecessairesDisponiblesBe: true,
      soinsInaccessiblesPaysOrigine: true,
      menaceOrdrePublic: false,
      dateDepotDemande: '2026-02-15',
      country: 'BELGIQUE',
      certificatMedicalType1Ok: true,
      soinsRequisOk: true,
      inaccessibiliteOk: true,
      pasMenace: true,
      scoreGlobal: 100,
      verdictProbabiliteAcceptation: 'ELEVEE',
      criteresNonRemplis: [],
      dateExpirationInstruction: '2027-02-15',
      formule: 'Régularisation 9ter médical : score 100/100, probabilité ELEVEE.',
      baseJuridique: 'Loi 15/12/1980 art. 9ter + AR 17/05/2007 art. 7-8',
      messages: ['Procédure 9ter — instruction OE 6-24 mois.'],
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [
        Belgian9terSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Belgian9terSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('mount : composant créé, signaux à leur défaut', () => {
    expect(component).toBeTruthy();
    expect(component.collapsed()).toBe(true);
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
    expect(component.maladieGraveCertifiee()).toBe(false);
    expect(component.soinsNecessairesDisponiblesBe()).toBe(false);
    expect(component.soinsInaccessiblesPaysOrigine()).toBe(false);
    expect(component.menaceOrdrePublic()).toBe(false);
    expect(component.dateDebutSymptomes()).toBeNull();
    expect(component.dateDepotDemande()).toBeNull();
  });

  it('BELGIQUE → isBelgium() true, GET appelé au ngOnInit', () => {
    expect(component.isBelgium()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('FRANCE → isBelgium() false, pas d\'appel HTTP au ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isBelgium()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('charge l\'analyse existante si présente (GET 200) → result + showForm=false', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(beResponse());
    expect(component.result()!.scoreGlobal).toBe(100);
    expect(component.result()!.verdictProbabiliteAcceptation).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(component.dateDebutSymptomes()).toBe('2026-01-10');
    expect(component.maladieGraveCertifiee()).toBe(true);
    expect(component.soinsNecessairesDisponiblesBe()).toBe(true);
    expect(component.soinsInaccessiblesPaysOrigine()).toBe(true);
    expect(component.menaceOrdrePublic()).toBe(false);
    expect(component.dateDepotDemande()).toBe('2026-02-15');
  });

  it('GET 404 → reste en mode formulaire', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid : tous booléens à false → valide (le backend retournera FAIBLE)', () => {
    expect(component.formValid()).toBe(true);
  });

  it('formValid : refuse dateDebutSymptomes dans le futur', () => {
    const future = new Date();
    future.setDate(future.getDate() + 10);
    const futureIso = future.toISOString().slice(0, 10);
    component.dateDebutSymptomes.set(futureIso);
    expect(component.formValid()).toBe(false);
  });

  it('formValid : refuse dateDepotDemande < dateDebutSymptomes', () => {
    component.dateDebutSymptomes.set('2026-02-01');
    component.dateDepotDemande.set('2026-01-15');
    expect(component.formValid()).toBe(false);

    component.dateDepotDemande.set('2026-02-15');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST → body complet, snack OK, refresh triggered, showForm=false', () => {
    component.dateDebutSymptomes.set('2026-01-10');
    component.maladieGraveCertifiee.set(true);
    component.soinsNecessairesDisponiblesBe.set(true);
    component.soinsInaccessiblesPaysOrigine.set(true);
    component.menaceOrdrePublic.set(false);
    component.dateDepotDemande.set('2026-02-15');
    component.analyze();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateDebutSymptomes: '2026-01-10',
      maladieGraveCertifiee: true,
      soinsNecessairesDisponiblesBe: true,
      soinsInaccessiblesPaysOrigine: true,
      menaceOrdrePublic: false,
      dateDepotDemande: '2026-02-15',
    });
    req.flush(beResponse());

    expect(component.result()!.scoreGlobal).toBe(100);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Régularisation 9ter analysée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('analyze() ignoré si formValid=false (date dans le futur)', () => {
    const future = new Date();
    future.setDate(future.getDate() + 5);
    component.dateDebutSymptomes.set(future.toISOString().slice(0, 10));
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('analyze() erreur backend → snack-error, analyzing reset', () => {
    component.maladieGraveCertifiee.set(true);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.analyzing()).toBe(false);
  });

  it('bannerClass : ELEVEE=success, MOYENNE=info, FAIBLE=warning', () => {
    expect(component.bannerClass('ELEVEE')).toContain('belgian-9ter-banner--success');
    expect(component.bannerClass('MOYENNE')).toContain('belgian-9ter-banner--info');
    expect(component.bannerClass('FAIBLE')).toContain('belgian-9ter-banner--warning');
    expect(component.bannerClass(null)).toBe('belgian-9ter-banner');
  });

  it('bannerIcon : ELEVEE=check_circle, MOYENNE=info_outline, FAIBLE=warning', () => {
    expect(component.bannerIcon('ELEVEE')).toBe('check_circle');
    expect(component.bannerIcon('MOYENNE')).toBe('info_outline');
    expect(component.bannerIcon('FAIBLE')).toBe('warning');
  });

  it('verdictLabel renvoie un libellé humain', () => {
    expect(component.verdictLabel('ELEVEE')).toBe('Probabilité élevée');
    expect(component.verdictLabel('MOYENNE')).toBe('Probabilité moyenne');
    expect(component.verdictLabel('FAIBLE')).toBe('Probabilité faible');
    expect(component.verdictLabel(null)).toBe('');
  });

  it('hasCriticalMenace : true si pasMenace=false (menace présente)', () => {
    expect(component.hasCriticalMenace(beResponse({ pasMenace: false }))).toBe(true);
    expect(component.hasCriticalMenace(beResponse({ pasMenace: true }))).toBe(false);
    expect(component.hasCriticalMenace(null)).toBe(false);
  });

  it('toggleCollapse() inverse l\'état collapsed', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('editMode() → showForm=true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('pré-fill IA gracieux : aiData absent → no-op, pas d\'exception', () => {
    component.aiData = undefined;
    expect(() => {
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    }).not.toThrow();
    expect(component.maladieGraveCertifiee()).toBe(false);
    expect(component.provenanceMaladieGrave()).toBeNull();
  });

  it('pré-fill IA gracieux : aiData fourni mais sans champ médical → no-op', () => {
    component.aiData = { dateNotificationOqtf: '2026-01-01' } as any;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.maladieGraveCertifiee()).toBe(false);
    expect(component.dateDebutSymptomes()).toBeNull();
    expect(component.provenanceMaladieGrave()).toBeNull();
    expect(component.provenanceDateDebutSymptomes()).toBeNull();
  });

  it('pré-fill IA si champs médicaux présents (clés tolérées) → signaux + provenance IA', () => {
    component.aiData = {
      // ImmigrationExtractedData ne déclare pas ces clés ; le composant
      // les consomme défensivement (cast any).
      dateDebutSymptomes: '2026-01-15',
      maladieGrave: true,
      soinsNecessairesDisponiblesBe: true,
    } as any;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dateDebutSymptomes()).toBe('2026-01-15');
    expect(component.provenanceDateDebutSymptomes()).toBe('IA');
    expect(component.maladieGraveCertifiee()).toBe(true);
    expect(component.provenanceMaladieGrave()).toBe('IA');
    expect(component.soinsNecessairesDisponiblesBe()).toBe(true);
    expect(component.provenanceSoinsBe()).toBe('IA');
  });

  it('onMaladieGraveChange efface la provenance IA', () => {
    component.provenanceMaladieGrave.set('IA');
    component.onMaladieGraveChange(true);
    expect(component.maladieGraveCertifiee()).toBe(true);
    expect(component.provenanceMaladieGrave()).toBeNull();
  });

  it('onDateDebutSymptomesChange efface la provenance IA', () => {
    component.provenanceDateDebutSymptomes.set('IA');
    component.onDateDebutSymptomesChange('2026-03-01');
    expect(component.dateDebutSymptomes()).toBe('2026-03-01');
    expect(component.provenanceDateDebutSymptomes()).toBeNull();
  });

  it('ngOnChanges(aiData) en mode formulaire sans result → re-prefill appliqué', () => {
    component.aiData = undefined;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceMaladieGrave()).toBeNull();

    component.aiData = { maladieGrave: true } as any;
    component.ngOnChanges({
      aiData: new SimpleChange(undefined, component.aiData, false),
    });

    expect(component.maladieGraveCertifiee()).toBe(true);
    expect(component.provenanceMaladieGrave()).toBe('IA');
  });

  it('ngOnChanges(aiData) firstChange → pas de re-prefill (ngOnInit s\'en charge)', () => {
    component.aiData = { maladieGrave: true } as any;
    component.ngOnChanges({
      aiData: new SimpleChange(undefined, component.aiData, true),
    });
    // En firstChange ngOnChanges ne lance pas prefillFromAi (évite double appel).
    expect(component.maladieGraveCertifiee()).toBe(false);
    expect(component.provenanceMaladieGrave()).toBeNull();
  });

  it('FRANCE : prefillFromAi non appelé (gate isBelgium)', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = { maladieGrave: true } as any;
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.maladieGraveCertifiee()).toBe(false);
  });

  it('résultat : score 50 (MOYENNE) → bannière info', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({
      scoreGlobal: 50,
      verdictProbabiliteAcceptation: 'MOYENNE',
      certificatMedicalType1Ok: true,
      soinsRequisOk: true,
      inaccessibiliteOk: false,
      pasMenace: true,
      criteresNonRemplis: ['Inaccessibilité des soins au pays d\'origine non prouvée'],
    }));
    expect(component.result()!.scoreGlobal).toBe(50);
    expect(component.bannerClass('MOYENNE')).toContain('--info');
  });

  it('résultat : score 0 (FAIBLE) avec menace ordre public → hasCriticalMenace=true', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({
      scoreGlobal: 0,
      verdictProbabiliteAcceptation: 'FAIBLE',
      maladieGraveCertifiee: false,
      certificatMedicalType1Ok: false,
      soinsNecessairesDisponiblesBe: false,
      soinsRequisOk: false,
      soinsInaccessiblesPaysOrigine: false,
      inaccessibiliteOk: false,
      menaceOrdrePublic: true,
      pasMenace: false,
      criteresNonRemplis: ['Certificat médical non produit', 'Menace ordre public présumée'],
    }));
    expect(component.result()!.scoreGlobal).toBe(0);
    expect(component.hasCriticalMenace(component.result())).toBe(true);
    expect(component.bannerClass('FAIBLE')).toContain('--warning');
  });
});
