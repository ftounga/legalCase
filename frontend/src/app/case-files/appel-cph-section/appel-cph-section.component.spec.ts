import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AppelCphSectionComponent } from './appel-cph-section.component';
import { AppelCphResponse, AppelCphStatut } from '../../core/models/appel-cph.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

/**
 * SF-218-02 — Tests du composant Appel CPH devant la Cour d'appel (F-DT-86).
 */
describe('AppelCphSectionComponent', () => {
  let component: AppelCphSectionComponent;
  let fixture: ComponentFixture<AppelCphSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/appel-cph-analysis';

  /** Fixture IA complète — le champ `dateNotificationJugement`. */
  const FULL_AI_DATA: TravailExtractedData = {
    dateNotificationJugement: '2026-05-10',
  };

  function response(
    statut: AppelCphStatut = 'DELAI_OUVERT',
    joursRestants = 20,
    overrides: Partial<AppelCphResponse> = {},
  ): AppelCphResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs.
      dateNotificationJugement: '2026-05-10',
      partieAppelante: 'SALARIE',
      modeNotification: 'SIGNIFICATION',
      representationConstituee: 'AVOCAT',
      jugementEnDernierRessort: false,
      // Sorties calculées.
      statut,
      dateEcheanceAppel: '2026-06-10',
      joursRestants,
      checklistFormalites: [
        { libelle: "Déclaration d'appel via RPVA", obligatoire: true, baseJuridique: 'art. 901 CPC' },
        { libelle: 'Mention des chefs de jugement critiqués', obligatoire: true, baseJuridique: 'art. 901 CPC' },
        { libelle: 'Représentation obligatoire (avocat ou défenseur syndical)', obligatoire: true, baseJuridique: 'R. 1461-2 CPC' },
      ],
      renvoiPourvoiCassation: statut === 'VOIE_FERMEE',
      baseJuridique: 'R. 1461-1 et s. CPC ; art. 538 CPC',
      basesJuridiques: ['R. 1461-1 CPC', 'art. 538 CPC', 'art. 901 CPC'],
      messages: ['Analyse calculée.'],
      country: 'FRANCE',
      calculatedAt: '2026-05-25T10:00:00Z',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        AppelCphSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppelCphSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ---------------------------------------------------------------------------
  // Chargement (GET) et gate pays
  // ---------------------------------------------------------------------------

  it('mount FRANCE → GET initial déclenché', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
  });

  it('mount BELGIQUE → pas d\'appel HTTP', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('BELGIQUE → bannière info pays affichée', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="appel-country-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + getPrefillCount
  // ---------------------------------------------------------------------------

  it('pré-remplit dateNotificationJugement depuis aiData (FRANCE) avec badge provenance', () => {
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    expect(component.dateNotificationJugement()).toBe('2026-05-10');
    expect(component.provenanceDateNotification()).toBe('IA');
  });

  it('ne pré-remplit pas si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);

    expect(component.dateNotificationJugement()).toBe('');
    expect(component.provenanceDateNotification()).toBeNull();
  });

  it('getPrefillCount = 1 sur fixture IA FR complète', () => {
    expect(AppelCphSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('getPrefillCount = 0 hors France', () => {
    expect(AppelCphSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 sur input vide', () => {
    expect(AppelCphSectionComponent.getPrefillCount({})).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST) — verdicts + refresh dashboard
  // ---------------------------------------------------------------------------

  it('POST → verdict DELAI_OUVERT + échéance + refresh dashboard', () => {
    component.dateNotificationJugement.set('2026-05-10');
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dateNotificationJugement).toBe('2026-05-10');
    req.flush(response('DELAI_OUVERT', 20));

    expect(component.result()?.statut).toBe('DELAI_OUVERT');
    expect(component.result()?.dateEcheanceAppel).toBe('2026-06-10');
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('chip verdict DELAI_URGENT → classe or', () => {
    expect(component.statutChipClass('DELAI_URGENT')).toBe('appel-chip-urgent');
    expect(component.statutChipClass('DELAI_OUVERT')).toBe('appel-chip-ouvert');
    expect(component.statutChipClass('DELAI_EXPIRE')).toBe('appel-chip-expire');
    expect(component.statutChipClass('VOIE_FERMEE')).toBe('appel-chip-fermee');
  });

  it('verdict DELAI_URGENT affiché après calcul (J-29 → urgent)', () => {
    component.collapsed.set(false);
    component.dateNotificationJugement.set('2026-05-10');
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('DELAI_URGENT', 5));
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('[data-testid="appel-verdict-banner"]');
    expect(banner).toBeTruthy();
    expect(banner.className).toContain('appel-chip-urgent');
    const label = fixture.nativeElement.querySelector('[data-testid="appel-verdict-label"]');
    expect(label.textContent).toContain('urgent');
  });

  it('VOIE_FERMEE → lien croisé pourvoi F-DT-87 affiché', () => {
    component.collapsed.set(false);
    component.dateNotificationJugement.set('2026-05-10');
    component.jugementEnDernierRessort.set(true);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('VOIE_FERMEE', 0));
    fixture.detectChanges();

    expect(component.showPourvoiLink()).toBe(true);
    const link = fixture.nativeElement.querySelector('[data-testid="appel-pourvoi-link"]');
    expect(link).toBeTruthy();
    expect(link.textContent).toContain('F-DT-87');
  });

  it('checklist formalités non vide après calcul', () => {
    component.collapsed.set(false);
    component.dateNotificationJugement.set('2026-05-10');
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('DELAI_OUVERT', 20));
    fixture.detectChanges();

    const items = fixture.nativeElement.querySelectorAll('[data-testid="appel-checklist-item"]');
    expect(items.length).toBeGreaterThan(0);
  });

  it('item checklist bloquant (représentation absente) mis en avant', () => {
    component.collapsed.set(false);
    component.dateNotificationJugement.set('2026-05-10');
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('DELAI_OUVERT', 20, {
      representationConstituee: 'AUCUNE',
      checklistFormalites: [
        { libelle: "Constituer une représentation (obligatoire en appel social)", obligatoire: true, bloquant: true, baseJuridique: 'R. 1461-2 CPC' },
        { libelle: "Déclaration d'appel via RPVA", obligatoire: true, baseJuridique: 'art. 901 CPC' },
      ],
    }));
    fixture.detectChanges();

    const bloquant = fixture.nativeElement.querySelector('.appel-checklist-bloquant');
    expect(bloquant).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  it('formValid = false si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    component.dateNotificationJugement.set('2026-05-10');
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false sans date de notification', () => {
    component.dateNotificationJugement.set('');
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false si date future', () => {
    component.dateNotificationJugement.set('2099-01-01');
    expect(component.formValid()).toBe(false);
  });

  it('formValid = true sur baseline FR (date passée)', () => {
    component.dateNotificationJugement.set('2026-05-10');
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Handlers — effacement du badge IA sur modif manuelle
  // ---------------------------------------------------------------------------

  it('onDateNotificationChange efface le badge IA', () => {
    component.provenanceDateNotification.set('IA');
    component.onDateNotificationChange('2026-05-12');
    expect(component.provenanceDateNotification()).toBeNull();
    expect(component.dateNotificationJugement()).toBe('2026-05-12');
  });

  // ---------------------------------------------------------------------------
  // Helpers UI
  // ---------------------------------------------------------------------------

  it('statutLabel couvre les 4 statuts', () => {
    expect(component.statutLabel('DELAI_OUVERT')).toContain('ouvert');
    expect(component.statutLabel('DELAI_URGENT')).toContain('urgent');
    expect(component.statutLabel('DELAI_EXPIRE')).toContain('expiré');
    expect(component.statutLabel('VOIE_FERMEE')).toContain('fermée');
  });

  it('formatDate formate une date ISO en jj/mm/aaaa', () => {
    const out = component.formatDate('2026-06-10');
    expect(out).toContain('06');
    expect(out).toContain('2026');
  });

  it('formatDate — null → tiret', () => {
    expect(component.formatDate(null)).toBe('—');
  });

  it('showPourvoiLink = false sans résultat', () => {
    expect(component.showPourvoiLink()).toBe(false);
  });
});
