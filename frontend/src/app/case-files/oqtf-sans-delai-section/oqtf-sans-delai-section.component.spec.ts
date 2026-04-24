import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { OqtfSansDelaiSectionComponent } from './oqtf-sans-delai-section.component';
import { OqtfSansDelaiResponse } from '../../core/models/oqtf-sans-delai.model';

describe('OqtfSansDelaiSectionComponent', () => {
  let component: OqtfSansDelaiSectionComponent;
  let fixture: ComponentFixture<OqtfSansDelaiSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/oqtf-sans-delai';

  function frResponse(overrides: Partial<OqtfSansDelaiResponse> = {}): OqtfSansDelaiResponse {
    return {
      caseFileId: 'case-1',
      dateHeureNotificationOqtf: '2026-04-23T10:00',
      motifSansDelai: 'RISQUE_FUITE',
      placementCra: false,
      recoursForme: false,
      dateHeureRecours: null,
      country: 'FRANCE',
      dateHeureExpirationDelaiRecours: '2026-04-25T10:00',
      heuresRestantes: 32,
      statutDelaiRecours: 'DISPONIBLE',
      dateHeureAudiencePrevisionnelle: null,
      dateDecisionPrevisionnelle: null,
      refereDisponibles: ['REFERE_LIBERTE_L521_2'],
      formule: 'Notification + 48 heures',
      baseJuridique: 'Art. L.614-6, L.614-7, L.521-2 CJA',
      messages: ['Urgence absolue : 48h pour former le recours'],
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        OqtfSansDelaiSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(OqtfSansDelaiSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('FRANCE → 4 motifs sans délai disponibles', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.motifs.length).toBe(4);
    const codes = component.motifs.map(m => m.code);
    expect(codes).toEqual([
      'RISQUE_FUITE',
      'TROUBLE_ORDRE_PUBLIC',
      'OQTF_PRECEDENTE_INEXECUTEE',
      'AUTRE',
    ]);
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
    req.flush(frResponse({ placementCra: true }));
    expect(component.result()!.statutDelaiRecours).toBe('DISPONIBLE');
    expect(component.showForm()).toBe(false);
    expect(component.motifSansDelai()).toBe('RISQUE_FUITE');
    expect(component.placementCra()).toBe(true);
    expect(component.recoursForme()).toBe(false);
    expect(component.dateHeureRecours()).toBeNull();
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false si date ou motif manquants', () => {
    component.dateHeureNotificationOqtf.set(null);
    component.motifSansDelai.set('RISQUE_FUITE');
    expect(component.formValid()).toBe(false);

    component.dateHeureNotificationOqtf.set('2026-04-23T10:00');
    component.motifSansDelai.set(null);
    expect(component.formValid()).toBe(false);

    component.motifSansDelai.set('RISQUE_FUITE');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si dateHeureNotificationOqtf dans le futur', () => {
    const future = new Date();
    future.setDate(future.getDate() + 10);
    const pad = (n: number) => n.toString().padStart(2, '0');
    const futureIso = `${future.getFullYear()}-${pad(future.getMonth() + 1)}-${pad(future.getDate())}T${pad(future.getHours())}:${pad(future.getMinutes())}`;
    component.dateHeureNotificationOqtf.set(futureIso);
    component.motifSansDelai.set('RISQUE_FUITE');
    expect(component.formValid()).toBe(false);
  });

  it('formValid : si recoursForme=true, dateHeureRecours requise et >= notification', () => {
    component.dateHeureNotificationOqtf.set('2026-04-23T10:00');
    component.motifSansDelai.set('RISQUE_FUITE');
    component.recoursForme.set(true);
    component.dateHeureRecours.set(null);
    expect(component.formValid()).toBe(false);

    component.dateHeureRecours.set('2026-04-22T10:00'); // avant notification
    expect(component.formValid()).toBe(false);

    component.dateHeureRecours.set('2026-04-23T12:00');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST recoursForme=false → body sans dateHeureRecours + snackbar succès', () => {
    component.dateHeureNotificationOqtf.set('2026-04-23T10:00');
    component.motifSansDelai.set('RISQUE_FUITE');
    component.placementCra.set(true);
    component.recoursForme.set(false);
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateHeureNotificationOqtf: '2026-04-23T10:00',
      motifSansDelai: 'RISQUE_FUITE',
      placementCra: true,
      recoursForme: false,
    });
    req.flush(frResponse({ placementCra: true }));

    expect(component.result()!.statutDelaiRecours).toBe('DISPONIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('OQTF sans délai analysée', 'OK', jasmine.any(Object));
  });

  it('analyze() POST recoursForme=true inclut dateHeureRecours + bannière succès verte', () => {
    component.dateHeureNotificationOqtf.set('2026-04-23T10:00');
    component.motifSansDelai.set('RISQUE_FUITE');
    component.placementCra.set(false);
    component.recoursForme.set(true);
    component.dateHeureRecours.set('2026-04-23T20:00');
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST');
    expect(req.request.body).toEqual({
      dateHeureNotificationOqtf: '2026-04-23T10:00',
      motifSansDelai: 'RISQUE_FUITE',
      placementCra: false,
      recoursForme: true,
      dateHeureRecours: '2026-04-23T20:00',
    });
    req.flush(frResponse({
      recoursForme: true,
      dateHeureRecours: '2026-04-23T20:00',
      statutDelaiRecours: 'RECOURS_FORME',
      dateHeureAudiencePrevisionnelle: '2026-04-25T09:00',
      dateDecisionPrevisionnelle: '2026-04-25',
      heuresRestantes: 0,
    }));

    expect(component.result()!.dateHeureAudiencePrevisionnelle).toBe('2026-04-25T09:00');
    expect(component.bannerClass('RECOURS_FORME')).toContain('oqtf-sd-banner--success');
    expect(component.bannerIcon('RECOURS_FORME')).toBe('check_circle');
    expect(component.showHeuresRestantes(component.result())).toBe(false);
  });

  it('bannerClass : DISPONIBLE=danger-medium, URGENT=danger-strong, EXPIRE=danger-dark, RECOURS_FORME=success', () => {
    expect(component.bannerClass('DISPONIBLE')).toContain('oqtf-sd-banner--danger-medium');
    expect(component.bannerClass('URGENT')).toContain('oqtf-sd-banner--danger-strong');
    expect(component.bannerClass('EXPIRE')).toContain('oqtf-sd-banner--danger-dark');
    expect(component.bannerClass('RECOURS_FORME')).toContain('oqtf-sd-banner--success');
  });

  it('bannerIcon : DISPONIBLE=warning, URGENT=error, EXPIRE=error, RECOURS_FORME=check_circle', () => {
    expect(component.bannerIcon('DISPONIBLE')).toBe('warning');
    expect(component.bannerIcon('URGENT')).toBe('error');
    expect(component.bannerIcon('EXPIRE')).toBe('error');
    expect(component.bannerIcon('RECOURS_FORME')).toBe('check_circle');
  });

  it('placementCra=true dans la réponse → résultat expose le flag (badge CRA visible côté template)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse({ placementCra: true, statutDelaiRecours: 'URGENT' }));
    expect(component.result()!.placementCra).toBe(true);
    expect(component.result()!.statutDelaiRecours).toBe('URGENT');
  });

  it('showHeuresRestantes true seulement pour DISPONIBLE et URGENT', () => {
    expect(component.showHeuresRestantes(frResponse({ statutDelaiRecours: 'DISPONIBLE' }))).toBe(true);
    expect(component.showHeuresRestantes(frResponse({ statutDelaiRecours: 'URGENT' }))).toBe(true);
    expect(component.showHeuresRestantes(frResponse({ statutDelaiRecours: 'EXPIRE' }))).toBe(false);
    expect(component.showHeuresRestantes(frResponse({ statutDelaiRecours: 'RECOURS_FORME' }))).toBe(false);
    expect(component.showHeuresRestantes(null)).toBe(false);
  });

  it('analyze() erreur backend → snackbar rouge + analyzing reset', () => {
    component.dateHeureNotificationOqtf.set('2026-04-23T10:00');
    component.motifSansDelai.set('RISQUE_FUITE');
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
    component.dateHeureNotificationOqtf.set(null);
    component.motifSansDelai.set(null);
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
    expect(component.statutLabel('DISPONIBLE')).toBe('Délai 48h disponible');
    expect(component.statutLabel('URGENT')).toBe('Délai 48h urgent');
    expect(component.statutLabel('EXPIRE')).toBe('Délai 48h expiré');
    expect(component.statutLabel('RECOURS_FORME')).toBe('Recours formé');
  });
});
