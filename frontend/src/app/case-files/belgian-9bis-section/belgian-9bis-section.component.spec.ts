import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Belgian9bisSectionComponent } from './belgian-9bis-section.component';
import { Belgian9bisResponse } from '../../core/models/belgian-9bis.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('Belgian9bisSectionComponent', () => {
  let component: Belgian9bisSectionComponent;
  let fixture: ComponentFixture<Belgian9bisSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/belgian-9bis';

  function r9bis(overrides: Partial<Belgian9bisResponse> = {}): Belgian9bisResponse {
    return {
      caseFileId: 'case-1',
      dateEntreeBelgique: '2023-01-15',
      dureePresenceMois: 38,
      circonstancesExceptionnelles: true,
      liensFamiliauxBe: true,
      liensProfessionnels: false,
      scolariteEnfantsBe: true,
      menaceOrdrePublic: false,
      dateDepotDemande: '2026-04-01',
      country: 'BELGIQUE',
      presence3AnsOk: true,
      liensConstitutifsOk: true,
      pasMenace: true,
      scoreGlobal: 100,
      verdictProbabilite: 'ELEVEE',
      criteresNonRemplis: [],
      dateExpirationInstructionPrevisionnelle: '2027-10-01',
      formule: 'Présence + liens + pas menace',
      baseJuridique: 'Loi 15/12/1980 art. 9bis + AR 17/05/2007',
      messages: ['Demande recevable au regard de l\'art. 9bis Loi 15/12/1980'],
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        Belgian9bisSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Belgian9bisSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('mounts and is collapsed by default', () => {
    expect(component).toBeTruthy();
    expect(component.collapsed()).toBe(true);
  });

  it('BELGIQUE → isBelgium() true, GET appelé au ngOnInit', () => {
    expect(component.isBelgium()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('FRANCE → isBelgium() false, pas d\'appel HTTP, bannière info', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isBelgium()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone(r => r.url === BASE_URL);
  });

  it('charge l\'analyse existante si présente (GET 200)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(r9bis());
    expect(component.result()!.verdictProbabilite).toBe('ELEVEE');
    expect(component.result()!.scoreGlobal).toBe(100);
    expect(component.showForm()).toBe(false);
    expect(component.dateEntreeBelgique()).toBe('2023-01-15');
    expect(component.dureePresenceMois()).toBe(38);
    expect(component.circonstancesExceptionnelles()).toBe(true);
    expect(component.menaceOrdrePublic()).toBe(false);
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false si dateEntreeBelgique manquante', () => {
    component.dateEntreeBelgique.set(null);
    component.dureePresenceMois.set(36);
    expect(component.formValid()).toBe(false);

    component.dateEntreeBelgique.set('2023-01-01');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si dateEntreeBelgique dans le futur', () => {
    const future = new Date();
    future.setDate(future.getDate() + 10);
    const futureIso = future.toISOString().slice(0, 10);
    component.dateEntreeBelgique.set(futureIso);
    component.dureePresenceMois.set(12);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si dureePresenceMois négatif', () => {
    component.dateEntreeBelgique.set('2023-01-01');
    component.dureePresenceMois.set(-1);
    expect(component.formValid()).toBe(false);
    component.dureePresenceMois.set(0);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si dateDepotDemande avant dateEntreeBelgique', () => {
    component.dateEntreeBelgique.set('2023-06-01');
    component.dureePresenceMois.set(36);
    component.dateDepotDemande.set('2023-01-01');
    expect(component.formValid()).toBe(false);
    component.dateDepotDemande.set('2024-01-01');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST sans dateDepotDemande → body sans dateDepotDemande', () => {
    component.dateEntreeBelgique.set('2023-01-15');
    component.dureePresenceMois.set(38);
    component.circonstancesExceptionnelles.set(true);
    component.liensFamiliauxBe.set(true);
    component.liensProfessionnels.set(false);
    component.scolariteEnfantsBe.set(true);
    component.menaceOrdrePublic.set(false);
    component.dateDepotDemande.set(null);
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateEntreeBelgique: '2023-01-15',
      dureePresenceMois: 38,
      circonstancesExceptionnelles: true,
      liensFamiliauxBe: true,
      liensProfessionnels: false,
      scolariteEnfantsBe: true,
      menaceOrdrePublic: false,
    });
    req.flush(r9bis());
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse 9bis humanitaire enregistrée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
    expect(component.showForm()).toBe(false);
    expect(component.result()!.verdictProbabilite).toBe('ELEVEE');
  });

  it('analyze() POST avec dateDepotDemande → body inclut dateDepotDemande', () => {
    component.dateEntreeBelgique.set('2023-01-15');
    component.dureePresenceMois.set(38);
    component.dateDepotDemande.set('2026-04-01');
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dateDepotDemande).toBe('2026-04-01');
    req.flush(r9bis());
  });

  it('analyze() error → MatSnackBar erreur', () => {
    component.dateEntreeBelgique.set('2023-01-15');
    component.dureePresenceMois.set(38);
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    req.flush({ message: 'Workspace mismatch' }, { status: 403, statusText: 'Forbidden' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Workspace mismatch', 'Fermer', jasmine.objectContaining({ panelClass: 'snack-error' }));
    expect(component.analyzing()).toBe(false);
    expect(component.showForm()).toBe(true);
  });

  it('pré-fill IA : dateDepotProcedure → dateDepotDemande + provenance IA', () => {
    const ai: Partial<ImmigrationExtractedData> = {
      dateDepotProcedure: '2026-03-15',
    };
    component.aiData = ai;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

    expect(component.dateDepotDemande()).toBe('2026-03-15');
    expect(component.provenanceDateDepot()).toBe('IA');
  });

  it('badge IA effacé au changement manuel', () => {
    component.dateDepotDemande.set('2026-03-15');
    component.provenanceDateDepot.set('IA');
    component.onDateDepotChange('2026-04-10');
    expect(component.dateDepotDemande()).toBe('2026-04-10');
    expect(component.provenanceDateDepot()).toBeNull();
  });

  it('toggleCollapse alterne collapsed', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('verdictLabel mappe ELEVEE/MOYENNE/FAIBLE', () => {
    expect(component.verdictLabel('ELEVEE')).toBe('Probabilité élevée');
    expect(component.verdictLabel('MOYENNE')).toBe('Probabilité moyenne');
    expect(component.verdictLabel('FAIBLE')).toBe('Probabilité faible');
    expect(component.verdictLabel(null)).toBe('');
  });

  it('bannerClass : ELEVEE → success, FAIBLE → warning (pas de rouge dominant)', () => {
    expect(component.bannerClass('ELEVEE')).toContain('success');
    expect(component.bannerClass('MOYENNE')).toContain('info');
    expect(component.bannerClass('FAIBLE')).toContain('warning');
    // Aucun verdict ne renvoie '--danger' (palette navy/or)
    expect(component.bannerClass('ELEVEE')).not.toContain('danger');
    expect(component.bannerClass('MOYENNE')).not.toContain('danger');
    expect(component.bannerClass('FAIBLE')).not.toContain('danger');
  });

  it('editMode → reaffiche le formulaire', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });
});
