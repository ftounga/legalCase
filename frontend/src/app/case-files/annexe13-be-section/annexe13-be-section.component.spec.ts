import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Annexe13BeSectionComponent } from './annexe13-be-section.component';
import { Annexe13BeResponse } from '../../core/models/annexe13-be.model';

describe('Annexe13BeSectionComponent', () => {
  let component: Annexe13BeSectionComponent;
  let fixture: ComponentFixture<Annexe13BeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/annexe13-be';

  function beResponse(overrides: Partial<Annexe13BeResponse> = {}): Annexe13BeResponse {
    return {
      caseFileId: 'case-1',
      dateNotificationAnnexe13: '2026-04-01',
      delaiDepartImposeJours: 30,
      motifOqt: 'SEJOUR_IRREGULIER_ART_7',
      transfertImminent: false,
      recoursForme: false,
      dateRecours: null,
      typeRecours: null,
      country: 'BELGIQUE',
      dateExpirationDelaiDepart: '2026-05-01',
      dateExpirationRecoursAnnulation: '2026-05-01',
      dateExpirationRecoursExtremeUrgence: '2026-04-08',
      joursRestantsAvantExpirationAnnulation: 12,
      statutRecoursAnnulation: 'DISPONIBLE',
      dateAudiencePrevisionnelle: null,
      dateDecisionPrevisionnelle: null,
      referedDisponibles: ['RECOURS_ANNULATION_30J', 'RECOURS_EXTREME_URGENCE_5JO'],
      formule: 'Notification + délai imposé',
      baseJuridique: 'Art. 39/2, 39/82 Loi 15.12.1980',
      messages: ['Recours suspensif possible'],
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        Annexe13BeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(Annexe13BeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('BELGIQUE → 4 motifs OQT disponibles', () => {
    expect(component.motifs.length).toBe(4);
    const codes = component.motifs.map(m => m.code);
    expect(codes).toEqual([
      'SEJOUR_IRREGULIER_ART_7',
      'REFUS_SEJOUR_APRES_DEMANDE',
      'FIN_SEJOUR_REGULIER',
      'AUTRE',
    ]);
  });

  it('BELGIQUE → 2 types de recours', () => {
    expect(component.typesRecours.length).toBe(2);
    const codes = component.typesRecours.map(t => t.code);
    expect(codes).toEqual(['ANNULATION_30J', 'EXTREME_URGENCE_5JO']);
  });

  it('BELGIQUE → isBelgium() true, GET appelé au ngOnInit', () => {
    component.workspaceCountry = 'BELGIQUE';
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
    httpMock.expectNone(r => r.url === BASE_URL);
  });

  it('charge l\'analyse existante si présente (GET 200)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(beResponse());
    expect(component.result()!.statutRecoursAnnulation).toBe('DISPONIBLE');
    expect(component.showForm()).toBe(false);
    expect(component.dateNotificationAnnexe13()).toBe('2026-04-01');
    expect(component.motifOqt()).toBe('SEJOUR_IRREGULIER_ART_7');
    expect(component.delaiDepartImposeJours()).toBe(30);
    expect(component.transfertImminent()).toBe(false);
    expect(component.recoursForme()).toBe(false);
    expect(component.dateRecours()).toBeNull();
    expect(component.typeRecours()).toBeNull();
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false si date ou motif manquants', () => {
    component.dateNotificationAnnexe13.set(null);
    component.motifOqt.set('SEJOUR_IRREGULIER_ART_7');
    expect(component.formValid()).toBe(false);

    component.dateNotificationAnnexe13.set('2026-04-01');
    component.motifOqt.set(null);
    expect(component.formValid()).toBe(false);

    component.motifOqt.set('SEJOUR_IRREGULIER_ART_7');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si dateNotificationAnnexe13 dans le futur', () => {
    const future = new Date();
    future.setDate(future.getDate() + 10);
    const futureIso = future.toISOString().slice(0, 10);
    component.dateNotificationAnnexe13.set(futureIso);
    component.motifOqt.set('SEJOUR_IRREGULIER_ART_7');
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si delaiDepartImposeJours hors bornes [0-30]', () => {
    component.dateNotificationAnnexe13.set('2026-04-01');
    component.motifOqt.set('SEJOUR_IRREGULIER_ART_7');
    component.delaiDepartImposeJours.set(-1);
    expect(component.formValid()).toBe(false);
    component.delaiDepartImposeJours.set(31);
    expect(component.formValid()).toBe(false);
    component.delaiDepartImposeJours.set(0);
    expect(component.formValid()).toBe(true);
    component.delaiDepartImposeJours.set(30);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : si recoursForme=true, dateRecours + typeRecours requis', () => {
    component.dateNotificationAnnexe13.set('2026-04-01');
    component.motifOqt.set('SEJOUR_IRREGULIER_ART_7');
    component.recoursForme.set(true);
    component.dateRecours.set(null);
    component.typeRecours.set(null);
    expect(component.formValid()).toBe(false);

    component.dateRecours.set('2026-04-05');
    component.typeRecours.set(null);
    expect(component.formValid()).toBe(false);

    component.typeRecours.set('ANNULATION_30J');
    expect(component.formValid()).toBe(true);

    component.dateRecours.set('2026-03-15'); // avant notification → invalide
    expect(component.formValid()).toBe(false);
  });

  it('analyze() POST recoursForme=false → body sans dateRecours/typeRecours', () => {
    component.dateNotificationAnnexe13.set('2026-04-01');
    component.motifOqt.set('SEJOUR_IRREGULIER_ART_7');
    component.delaiDepartImposeJours.set(30);
    component.transfertImminent.set(false);
    component.recoursForme.set(false);
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateNotificationAnnexe13: '2026-04-01',
      delaiDepartImposeJours: 30,
      motifOqt: 'SEJOUR_IRREGULIER_ART_7',
      transfertImminent: false,
      recoursForme: false,
    });
    req.flush(beResponse());

    expect(component.result()!.statutRecoursAnnulation).toBe('DISPONIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Annexe 13 analysée', 'OK', jasmine.any(Object));
  });

  it('analyze() POST recoursForme=true → body inclut dateRecours + typeRecours', () => {
    component.dateNotificationAnnexe13.set('2026-04-01');
    component.motifOqt.set('SEJOUR_IRREGULIER_ART_7');
    component.delaiDepartImposeJours.set(30);
    component.transfertImminent.set(true);
    component.recoursForme.set(true);
    component.typeRecours.set('EXTREME_URGENCE_5JO');
    component.dateRecours.set('2026-04-03');
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST');
    expect(req.request.body).toEqual({
      dateNotificationAnnexe13: '2026-04-01',
      delaiDepartImposeJours: 30,
      motifOqt: 'SEJOUR_IRREGULIER_ART_7',
      transfertImminent: true,
      recoursForme: true,
      dateRecours: '2026-04-03',
      typeRecours: 'EXTREME_URGENCE_5JO',
    });
    req.flush(beResponse({
      transfertImminent: true,
      recoursForme: true,
      dateRecours: '2026-04-03',
      typeRecours: 'EXTREME_URGENCE_5JO',
      statutRecoursAnnulation: 'RECOURS_FORME',
      dateAudiencePrevisionnelle: '2026-07-02',
      dateDecisionPrevisionnelle: '2026-09-30',
      joursRestantsAvantExpirationAnnulation: 0,
    }));

    expect(component.result()!.transfertImminent).toBe(true);
    expect(component.result()!.typeRecours).toBe('EXTREME_URGENCE_5JO');
    expect(component.bannerClass('RECOURS_FORME')).toContain('annexe13-banner--success');
  });

  it('transfertImminent=true persisté dans le result (drive le badge template)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(beResponse({ transfertImminent: true }));
    expect(component.result()!.transfertImminent).toBe(true);
    expect(component.transfertImminent()).toBe(true);
  });

  it('bannerClass : DISPONIBLE=info, URGENT=warning, EXPIRE=danger, RECOURS_FORME=success', () => {
    expect(component.bannerClass('DISPONIBLE')).toContain('annexe13-banner--info');
    expect(component.bannerClass('URGENT')).toContain('annexe13-banner--warning');
    expect(component.bannerClass('EXPIRE')).toContain('annexe13-banner--danger');
    expect(component.bannerClass('RECOURS_FORME')).toContain('annexe13-banner--success');
  });

  it('bannerIcon : DISPONIBLE=info_outline, URGENT=warning, EXPIRE=error, RECOURS_FORME=check_circle', () => {
    expect(component.bannerIcon('DISPONIBLE')).toBe('info_outline');
    expect(component.bannerIcon('URGENT')).toBe('warning');
    expect(component.bannerIcon('EXPIRE')).toBe('error');
    expect(component.bannerIcon('RECOURS_FORME')).toBe('check_circle');
  });

  it('statut EXPIRE → classe rouge (seule utilisation rouge)', () => {
    expect(component.bannerClass('EXPIRE')).toContain('annexe13-banner--danger');
  });

  it('showJoursRestants true seulement pour DISPONIBLE et URGENT', () => {
    expect(component.showJoursRestants(beResponse({ statutRecoursAnnulation: 'DISPONIBLE' }))).toBe(true);
    expect(component.showJoursRestants(beResponse({ statutRecoursAnnulation: 'URGENT' }))).toBe(true);
    expect(component.showJoursRestants(beResponse({ statutRecoursAnnulation: 'EXPIRE' }))).toBe(false);
    expect(component.showJoursRestants(beResponse({ statutRecoursAnnulation: 'RECOURS_FORME' }))).toBe(false);
    expect(component.showJoursRestants(null)).toBe(false);
  });

  it('analyze() erreur backend → snackbar rouge + analyzing reset', () => {
    component.dateNotificationAnnexe13.set('2026-04-01');
    component.motifOqt.set('SEJOUR_IRREGULIER_ART_7');
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
    component.dateNotificationAnnexe13.set(null);
    component.motifOqt.set(null);
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

  it('typeRecoursLabel renvoie un libellé humain', () => {
    expect(component.typeRecoursLabel('ANNULATION_30J')).toBe('Annulation 30 jours');
    expect(component.typeRecoursLabel('EXTREME_URGENCE_5JO')).toBe('Extrême urgence 5 jours ouvrables');
    expect(component.typeRecoursLabel(null)).toBe('');
  });
});
