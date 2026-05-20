import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LicenciementFauteGraveLourdSectionComponent } from './licenciement-faute-grave-lourd-section.component';
import {
  LicenciementFauteGraveLourdQualification,
  LicenciementFauteGraveLourdResponse,
} from '../../core/models/licenciement-faute-grave-lourd.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('LicenciementFauteGraveLourdSectionComponent', () => {
  let component: LicenciementFauteGraveLourdSectionComponent;
  let fixture: ComponentFixture<LicenciementFauteGraveLourdSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/licenciement-faute-grave-lourde';

  /** Fixture IA complète — les 6 champs `fauteGrave*`. */
  const FULL_AI_DATA: TravailExtractedData = {
    fauteGraveFaitsReproches: 'Insultes envers le supérieur hiérarchique',
    fauteGraveDatesFaits: ['2024-03-12', '2024-04-08'],
    fauteGraveQualificationEmployeur: 'FAUTE_GRAVE',
    fauteGraveIntentionNuireAlleeguee: false,
    fauteGraveAncienneteMois: 36,
    fauteGraveSalaireMensuelBrut: 2850,
  };

  function response(
    qualification: LicenciementFauteGraveLourdQualification,
  ): LicenciementFauteGraveLourdResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs (ré-édition du formulaire après recharge).
      faitsReproches: 'Insultes envers le supérieur hiérarchique',
      datesFaits: ['2024-03-12', '2024-04-08'],
      dateSaisineEmployeur: null,
      prescriptionFauteVerifiee: false,
      ancienneteMois: 36,
      salaireMensuelBrutEuros: 2850,
      qualificationEmployeur: qualification,
      intentionNuireAlleeguee: qualification === 'FAUTE_LOURDE',
      preuveIntentionNuire: null,
      attenteConvocation: true,
      motivationLettreAdequate: true,
      // Sorties calculées.
      qualificationRetenue: qualification,
      scoreQualification: qualification === 'FAUTE_LOURDE' ? 75
                       : qualification === 'FAUTE_GRAVE' ? 60
                       : 30,
      facteursQualification: [
        {
          code: 'DT36_QUALIFICATION_FAUTE',
          libelle: `Qualification : ${qualification}`,
          fondement: 'L. 1234-1 CT',
          poids: 50,
          explication: 'Test facteur.',
        },
      ],
      indemnitePreavisEuros: qualification === 'FAUTE_SIMPLE' ? 2850 : 0,
      indemniteLegaleEuros: qualification === 'FAUTE_SIMPLE' ? 2137.5 : 0,
      indemnitesCongesPayesEuros: 285,
      totalIndemnitesDuesEuros: qualification === 'FAUTE_SIMPLE' ? 5272.5 : 285,
      alertePrescriptionFaute: false,
      basesJuridiques: ['L. 1234-1 CT', 'L. 1234-9 CT'],
      messages: ['Faute disciplinaire qualifiée.'],
      country: 'FRANCE',
      calculatedAt: '2026-05-20T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        LicenciementFauteGraveLourdSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LicenciementFauteGraveLourdSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

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
    const banner = fixture.nativeElement.querySelector('[data-testid="fgl-country-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + getPrefillCount
  // ---------------------------------------------------------------------------

  it('pré-fill FRANCE : 6 champs renseignés depuis aiData + provenance IA', () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.faitsReproches()).toBe('Insultes envers le supérieur hiérarchique');
    expect(component.datesFaits()).toEqual(['2024-03-12', '2024-04-08']);
    expect(component.qualificationEmployeur()).toBe('FAUTE_GRAVE');
    expect(component.intentionNuireAlleeguee()).toBe(false);
    expect(component.ancienneteMois()).toBe(36);
    expect(component.salaireMensuelBrutEuros()).toBe(2850);
    expect(component.provenanceFaitsReproches()).toBe('IA');
    expect(component.provenanceQualification()).toBe('IA');
    expect(component.provenanceAncienneteMois()).toBe('IA');
    expect(component.provenanceSalaireMensuelBrut()).toBe('IA');
  });

  it('pré-fill BELGIQUE → 0 champ renseigné (gate FR-only)', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    expect(component.faitsReproches()).toBe('');
    expect(component.qualificationEmployeur()).toBe('FAUTE_SIMPLE'); // valeur par défaut
    expect(component.ancienneteMois()).toBeNull();
    expect(component.salaireMensuelBrutEuros()).toBeNull();
    expect(component.provenanceFaitsReproches()).toBeNull();
  });

  it('getPrefillCount FRANCE complet = 6', () => {
    const n = LicenciementFauteGraveLourdSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    });
    expect(n).toBe(6);
  });

  it('getPrefillCount BELGIQUE = 0 (gate FR-only)', () => {
    const n = LicenciementFauteGraveLourdSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    });
    expect(n).toBe(0);
  });

  it('getPrefillCount aiData partiel = nombre exact de champs présents', () => {
    const n = LicenciementFauteGraveLourdSectionComponent.getPrefillCount({
      aiData: {
        fauteGraveQualificationEmployeur: 'FAUTE_LOURDE',
        fauteGraveAncienneteMois: 60,
      } as TravailExtractedData,
      workspaceCountry: 'FRANCE',
    });
    expect(n).toBe(2);
  });

  it('getPrefillCount qualification hors whitelist ignorée', () => {
    const n = LicenciementFauteGraveLourdSectionComponent.getPrefillCount({
      aiData: {
        fauteGraveQualificationEmployeur: 'DISCIPLINAIRE',
      } as TravailExtractedData,
      workspaceCountry: 'FRANCE',
    });
    expect(n).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Validation du formulaire
  // ---------------------------------------------------------------------------

  it('formValid FRANCE avec données valides → true', () => {
    component.workspaceCountry = 'FRANCE';
    component.faitsReproches.set('Insultes');
    component.ancienneteMois.set(36);
    component.salaireMensuelBrutEuros.set(2850);
    expect(component.formValid()).toBe(true);
  });

  it('formValid FRANCE sans faits → false', () => {
    component.workspaceCountry = 'FRANCE';
    component.faitsReproches.set('   ');
    component.ancienneteMois.set(36);
    component.salaireMensuelBrutEuros.set(2850);
    expect(component.formValid()).toBe(false);
  });

  it('formValid FRANCE avec salaire 0 → false', () => {
    component.workspaceCountry = 'FRANCE';
    component.faitsReproches.set('Insultes');
    component.ancienneteMois.set(36);
    component.salaireMensuelBrutEuros.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid BELGIQUE → false', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.faitsReproches.set('Insultes');
    component.ancienneteMois.set(36);
    component.salaireMensuelBrutEuros.set(2850);
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Verdict + indemnités + alerte prescription
  // ---------------------------------------------------------------------------

  it('verdictBannerClass / label / icon — 3 niveaux', () => {
    expect(component.verdictBannerLabel('FAUTE_SIMPLE')).toBe('Faute simple');
    expect(component.verdictBannerLabel('FAUTE_GRAVE')).toBe('Faute grave');
    expect(component.verdictBannerLabel('FAUTE_LOURDE')).toBe('Faute lourde');
    expect(component.verdictBannerClass('FAUTE_LOURDE')).toContain('lourde');
    expect(component.verdictBannerClass('FAUTE_GRAVE')).toContain('grave');
    expect(component.verdictBannerClass('FAUTE_SIMPLE')).toContain('simple');
    expect(component.verdictBannerIcon('FAUTE_SIMPLE')).toBe('check_circle');
    expect(component.verdictBannerIcon('FAUTE_GRAVE')).toBe('warning');
    expect(component.verdictBannerIcon('FAUTE_LOURDE')).toBe('error');
  });

  it('result FAUTE_GRAVE → bannière + indemnités préavis/IL à 0', () => {
    component.forceExpanded = true;
    fixture.detectChanges(); // déclenche ngOnInit() + liaison vue
    const get = httpMock.expectOne(BASE_URL);
    get.flush(response('FAUTE_GRAVE'));
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="fgl-verdict-banner"]');
    expect(banner).toBeTruthy();
    expect(banner.textContent).toContain('Faute grave');
    expect(component.result()!.indemnitePreavisEuros).toBe(0);
    expect(component.result()!.indemniteLegaleEuros).toBe(0);
  });

  it('alerte prescription affichée si alertePrescriptionFaute=true', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const get = httpMock.expectOne(BASE_URL);
    const r = response('FAUTE_GRAVE');
    r.alertePrescriptionFaute = true;
    get.flush(r);
    fixture.detectChanges();
    const alert = fixture.nativeElement.querySelector('[data-testid="fgl-alert-prescription"]');
    expect(alert).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Soumission du formulaire (POST)
  // ---------------------------------------------------------------------------

  it('calculate → POST avec snapshot d\'inputs + refresh dashboard', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.faitsReproches.set('Insultes graves');
    component.datesFaits.set(['2024-03-12']);
    component.ancienneteMois.set(36);
    component.salaireMensuelBrutEuros.set(2850);
    component.qualificationEmployeur.set('FAUTE_GRAVE');
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.faitsReproches).toBe('Insultes graves');
    expect(post.request.body.qualificationEmployeur).toBe('FAUTE_GRAVE');
    expect(post.request.body.ancienneteMois).toBe(36);
    expect(post.request.body.salaireMensuelBrutEuros).toBe(2850);
    post.flush(response('FAUTE_GRAVE'));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
    expect(component.showForm()).toBe(false);
  });

  it('calculate erreur backend → snackBar erreur + saisie conservée', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.faitsReproches.set('Insultes');
    component.ancienneteMois.set(36);
    component.salaireMensuelBrutEuros.set(2850);
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'Boom' }, { status: 500, statusText: 'Server Error' });
    expect(snackSpy.open).toHaveBeenCalled();
    expect(component.faitsReproches()).toBe('Insultes'); // saisie conservée
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Handlers — modification manuelle efface le badge IA
  // ---------------------------------------------------------------------------

  it('onQualificationChange manuel → badge IA effacé', () => {
    component.provenanceQualification.set('IA');
    component.onQualificationChange('FAUTE_LOURDE');
    expect(component.qualificationEmployeur()).toBe('FAUTE_LOURDE');
    expect(component.provenanceQualification()).toBeNull();
  });

  it('onIntentionNuireChange(false) reset preuve', () => {
    component.preuveIntentionNuire.set('Email de menace');
    component.onIntentionNuireChange(false);
    expect(component.preuveIntentionNuire()).toBe('');
  });

  it('onDatesFaitsTextChange parse virgules + filtre vides', () => {
    component.onDatesFaitsTextChange('2024-03-12 , , 2024-04-08;  2024-05-01');
    expect(component.datesFaits()).toEqual(['2024-03-12', '2024-04-08', '2024-05-01']);
  });
});
