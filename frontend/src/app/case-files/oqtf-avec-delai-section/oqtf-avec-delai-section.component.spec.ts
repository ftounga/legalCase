import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { OqtfAvecDelaiSectionComponent } from './oqtf-avec-delai-section.component';
import { OqtfAvecDelaiResponse } from '../../core/models/oqtf-avec-delai.model';

describe('OqtfAvecDelaiSectionComponent', () => {
  let component: OqtfAvecDelaiSectionComponent;
  let fixture: ComponentFixture<OqtfAvecDelaiSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/oqtf-avec-delai';

  function frResponse(overrides: Partial<OqtfAvecDelaiResponse> = {}): OqtfAvecDelaiResponse {
    return {
      caseFileId: 'case-1',
      dateNotificationOqtf: '2026-04-01',
      motifOqtf: 'REFUS_TITRE',
      recoursForme: false,
      dateRecours: null,
      country: 'FRANCE',
      dateExpirationDdv: '2026-05-01',
      dateExpirationDelaiRecours: '2026-05-01',
      joursRestantsAvantExpirationDelai: 12,
      statutDelaiRecours: 'DISPONIBLE',
      dateAudiencePrevisionnelle: null,
      dateDecisionTaPrevisionnelle: null,
      referedDisponibles: ['REFERE_SUSPENSION_L521_1', 'REFERE_LIBERTE_L521_2'],
      formule: 'Notification + 30 jours',
      baseJuridique: 'Art. L.614-5, L.614-6, R.776-18 CJA',
      messages: ['Recours suspensif'],
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        OqtfAvecDelaiSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(OqtfAvecDelaiSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('FRANCE → 5 motifs OQTF disponibles', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.motifs.length).toBe(5);
    const codes = component.motifs.map(m => m.code);
    expect(codes).toEqual(['REFUS_TITRE', 'EXPIRATION_TITRE', 'SEJOUR_IRREGULIER', 'RETRAIT_TITRE', 'AUTRE']);
  });

  it('FRANCE → isFrance() true, GET appelé au ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isFrance()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('BELGIQUE → isFrance() false, pas d\'appel HTTP au ngOnInit', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.isFrance()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone(r => r.url === BASE_URL);
  });

  it('charge l\'analyse existante si présente (GET 200)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse());
    expect(component.result()!.statutDelaiRecours).toBe('DISPONIBLE');
    expect(component.showForm()).toBe(false);
    expect(component.dateNotificationOqtf()).toBe('2026-04-01');
    expect(component.motifOqtf()).toBe('REFUS_TITRE');
    expect(component.recoursForme()).toBe(false);
    expect(component.dateRecours()).toBeNull();
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false si date ou motif manquants', () => {
    component.dateNotificationOqtf.set(null);
    component.motifOqtf.set('REFUS_TITRE');
    expect(component.formValid()).toBe(false);

    component.dateNotificationOqtf.set('2026-04-01');
    component.motifOqtf.set(null);
    expect(component.formValid()).toBe(false);

    component.motifOqtf.set('REFUS_TITRE');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si dateNotificationOqtf dans le futur', () => {
    // today + 10 jours
    const future = new Date();
    future.setDate(future.getDate() + 10);
    const futureIso = future.toISOString().slice(0, 10);
    component.dateNotificationOqtf.set(futureIso);
    component.motifOqtf.set('REFUS_TITRE');
    expect(component.formValid()).toBe(false);
  });

  it('formValid : si recoursForme=true, dateRecours requise et >= notification', () => {
    component.dateNotificationOqtf.set('2026-04-01');
    component.motifOqtf.set('REFUS_TITRE');
    component.recoursForme.set(true);
    component.dateRecours.set(null);
    expect(component.formValid()).toBe(false);

    component.dateRecours.set('2026-03-15'); // avant notification → invalide
    expect(component.formValid()).toBe(false);

    component.dateRecours.set('2026-04-05');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST recoursForme=false → résultat + snackbar succès + pas de dateRecours dans le body', () => {
    component.dateNotificationOqtf.set('2026-04-01');
    component.motifOqtf.set('REFUS_TITRE');
    component.recoursForme.set(false);
    component.dateRecours.set(null);
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateNotificationOqtf: '2026-04-01',
      motifOqtf: 'REFUS_TITRE',
      recoursForme: false,
    });
    req.flush(frResponse());

    expect(component.result()!.statutDelaiRecours).toBe('DISPONIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('OQTF analysée', 'OK', jasmine.any(Object));
  });

  it('analyze() POST recoursForme=true inclut dateRecours + bannière succès verte', () => {
    component.dateNotificationOqtf.set('2026-04-01');
    component.motifOqtf.set('REFUS_TITRE');
    component.recoursForme.set(true);
    component.dateRecours.set('2026-04-10');
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST');
    expect(req.request.body).toEqual({
      dateNotificationOqtf: '2026-04-01',
      motifOqtf: 'REFUS_TITRE',
      recoursForme: true,
      dateRecours: '2026-04-10',
    });
    req.flush(frResponse({
      recoursForme: true,
      dateRecours: '2026-04-10',
      statutDelaiRecours: 'RECOURS_FORME',
      dateAudiencePrevisionnelle: '2026-07-09',
      dateDecisionTaPrevisionnelle: '2026-10-07',
      joursRestantsAvantExpirationDelai: 0,
    }));

    expect(component.result()!.dateAudiencePrevisionnelle).toBe('2026-07-09');
    expect(component.result()!.dateDecisionTaPrevisionnelle).toBe('2026-10-07');
    expect(component.bannerClass('RECOURS_FORME')).toContain('oqtf-banner--success');
    expect(component.bannerIcon('RECOURS_FORME')).toBe('check_circle');
    expect(component.showJoursRestants(component.result())).toBe(false);
  });

  it('bannerClass : DISPONIBLE=info, URGENT=warning, EXPIRE=danger, RECOURS_FORME=success', () => {
    expect(component.bannerClass('DISPONIBLE')).toContain('oqtf-banner--info');
    expect(component.bannerClass('URGENT')).toContain('oqtf-banner--warning');
    expect(component.bannerClass('EXPIRE')).toContain('oqtf-banner--danger');
    expect(component.bannerClass('RECOURS_FORME')).toContain('oqtf-banner--success');
  });

  it('bannerIcon : DISPONIBLE=info_outline, URGENT=warning, EXPIRE=error, RECOURS_FORME=check_circle', () => {
    expect(component.bannerIcon('DISPONIBLE')).toBe('info_outline');
    expect(component.bannerIcon('URGENT')).toBe('warning');
    expect(component.bannerIcon('EXPIRE')).toBe('error');
    expect(component.bannerIcon('RECOURS_FORME')).toBe('check_circle');
  });

  it('statut EXPIRE → classe rouge (seule utilisation rouge)', () => {
    const cls = component.bannerClass('EXPIRE');
    expect(cls).toContain('oqtf-banner--danger');
  });

  it('showJoursRestants true seulement pour DISPONIBLE et URGENT', () => {
    expect(component.showJoursRestants(frResponse({ statutDelaiRecours: 'DISPONIBLE' }))).toBe(true);
    expect(component.showJoursRestants(frResponse({ statutDelaiRecours: 'URGENT' }))).toBe(true);
    expect(component.showJoursRestants(frResponse({ statutDelaiRecours: 'EXPIRE' }))).toBe(false);
    expect(component.showJoursRestants(frResponse({ statutDelaiRecours: 'RECOURS_FORME' }))).toBe(false);
    expect(component.showJoursRestants(null)).toBe(false);
  });

  it('analyze() erreur backend → snackbar rouge + analyzing reset', () => {
    component.dateNotificationOqtf.set('2026-04-01');
    component.motifOqtf.set('REFUS_TITRE');
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.analyzing()).toBe(false);
  });

  it('analyze() ignoré si form invalide (pas d\'appel HTTP POST)', () => {
    component.dateNotificationOqtf.set(null);
    component.motifOqtf.set(null);
    component.analyze();
    httpMock.expectNone(r => r.method === 'POST');
  });

  it('editMode() → showForm true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('toggleCollapse() inverse l\'état collapsed', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('statutLabel renvoie un libellé humain pour chaque statut', () => {
    expect(component.statutLabel('DISPONIBLE')).toBe('Délai disponible');
    expect(component.statutLabel('URGENT')).toBe('Délai urgent');
    expect(component.statutLabel('EXPIRE')).toBe('Délai expiré');
    expect(component.statutLabel('RECOURS_FORME')).toBe('Recours formé');
  });
});
