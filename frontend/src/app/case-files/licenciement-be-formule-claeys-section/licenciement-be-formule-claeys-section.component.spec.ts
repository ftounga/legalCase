import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  LicenciementBeFormuleClaeysSectionComponent,
} from './licenciement-be-formule-claeys-section.component';
import {
  LicenciementBeFormuleClaeysResponse,
} from '../../core/models/licenciement-be-formule-claeys.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('LicenciementBeFormuleClaeysSectionComponent', () => {
  let component: LicenciementBeFormuleClaeysSectionComponent;
  let fixture: ComponentFixture<LicenciementBeFormuleClaeysSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/licenciement-be-formule-claeys';

  function response(
    overrides: Partial<LicenciementBeFormuleClaeysResponse> = {},
  ): LicenciementBeFormuleClaeysResponse {
    return {
      caseFileId: 'case-1',
      ancienneteAnneesPreStatutUnique: 12,
      ancienneteMoisPreStatutUnique: 0,
      remunerationAnnuelleBruteEnMilliers: 60,
      appliquerClauseSauvegarde: true,
      ancienneteAnneesPostStatutUnique: 12,
      salaireHebdomadaireBrut: 1200,
      preavisClaeysMois: 11.52,
      preavisClaeysSemaines: 50,
      preavisStatutUniquesSemaines: 36,
      preavisTotalSemaines: 86,
      indemniteClaeysBrute: 57600,
      indemniteTotaleBrute: 100800,
      formuleClaeys: '0.8 × (12 + 0.04 × 60) = 11.52 mois ≈ 50 sem',
      baseJuridique: 'Ancien art. 82 §3 Loi 03/07/1978 ; Cass. 28/02/2011 RG S.10.0073.F ; Loi 26/12/2013 art. 67',
      avertissement: 'La formule Claeys est jurisprudentielle (Cass. 14/12/2015 RG S.15.0024.F) — pas d\'ancrage légal stricto sensu.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        LicenciementBeFormuleClaeysSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(LicenciementBeFormuleClaeysSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ============================================================
  // Gate pays + init
  // ============================================================

  it('BELGIQUE → isAvailable() true, GET au ngOnInit', () => {
    expect(component.isAvailable()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('FRANCE → isAvailable() false, aucun GET au ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isAvailable()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
  });

  it('FRANCE → bannière « réservé droit BE » + masquage form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.empty-result');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('belge');
  });

  it('GET 200 → hydrate result + mode résultat', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.preavisClaeysMois).toBe(11.52);
    expect(component.showForm()).toBe(false);
    expect(component.provenanceAncienneteEPre()).toBeNull();
  });

  it('GET 404 → reste en mode formulaire', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ============================================================
  // Pré-fill IA
  // ============================================================

  it('pré-fill BE pré-2014 + clause sauvegarde activée par défaut', () => {
    component.aiData = {
      dateEntree: '2008-06-15',
      dateLicenciement: '2026-03-15',
      salaireBrutAnnuel: 60000,
    } as TravailExtractedData;
    component.ngOnInit();

    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

    // Ancienneté pré-2014 (de 2008-06-15 à 2014-01-01 = 5 ans 6 mois).
    expect(component.ancienneteAnneesPreStatutUnique()).toBe(5);
    expect(component.ancienneteMoisPreStatutUnique()).toBe(6);
    // Rémunération annuelle K€ : 60000 / 1000 = 60.
    expect(component.remunerationAnnuelleBruteEnMilliers()).toBe(60);
    // Clause de sauvegarde dérivée true (contrat pré-2014).
    expect(component.appliquerClauseSauvegarde()).toBe(true);
    // Ancienneté post-2014 (de 2014-01-01 à 2026-03-15 = 12 ans 2 mois).
    expect(component.ancienneteAnneesPostStatutUnique()).toBe(12);
    // Salaire hebdo : 60000 / 52 ≈ 1153.85.
    expect(component.salaireHebdomadaireBrut()).toBeCloseTo(1153.85, 2);

    expect(component.provenanceAncienneteEPre()).toBe('IA');
    expect(component.provenanceRemunerationAnnuelle()).toBe('IA');
    expect(component.provenanceClauseSauvegarde()).toBe('IA');
    expect(component.provenanceAncienneteEPost()).toBe('IA');
    expect(component.provenanceSalaireHebdo()).toBe('IA');
  });

  it('contrat post-2014 → aucun pré-fill (Claeys non applicable)', () => {
    component.aiData = {
      dateEntree: '2018-06-01',
      dateLicenciement: '2026-03-15',
      salaireBrutAnnuel: 60000,
    } as TravailExtractedData;
    component.ngOnInit();

    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

    expect(component.ancienneteAnneesPreStatutUnique()).toBeNull();
    expect(component.ancienneteAnneesPostStatutUnique()).toBeNull();
    // Mais rémunération K€ + salaire hebdo restent pré-fillables si le toggle reste à défaut true.
    expect(component.remunerationAnnuelleBruteEnMilliers()).toBe(60);
    // Toggle reste à défaut true (pas de pré-fill IA dérivé).
    expect(component.provenanceClauseSauvegarde()).toBeNull();
  });

  it('FRANCE → aucun pré-fill', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = {
      dateEntree: '2008-06-01',
      salaireBrutAnnuel: 60000,
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
    expect(component.ancienneteAnneesPreStatutUnique()).toBeNull();
    expect(component.remunerationAnnuelleBruteEnMilliers()).toBeNull();
  });

  // ============================================================
  // formValid()
  // ============================================================

  it('formValid : ancienneté pré obligatoire 0-80', () => {
    component.remunerationAnnuelleBruteEnMilliers.set(60);
    component.appliquerClauseSauvegarde.set(false); // pas de champs conditionnels.

    component.ancienneteAnneesPreStatutUnique.set(null);
    expect(component.formValid()).toBe(false);

    component.ancienneteAnneesPreStatutUnique.set(-1);
    expect(component.formValid()).toBe(false);

    component.ancienneteAnneesPreStatutUnique.set(81);
    expect(component.formValid()).toBe(false);

    component.ancienneteAnneesPreStatutUnique.set(10);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : si clause sauvegarde ON → exige ancienneté post + salaire hebdo', () => {
    component.ancienneteAnneesPreStatutUnique.set(10);
    component.remunerationAnnuelleBruteEnMilliers.set(60);
    component.appliquerClauseSauvegarde.set(true);

    // Sans les 2 champs conditionnels → invalid.
    expect(component.formValid()).toBe(false);

    component.ancienneteAnneesPostStatutUnique.set(12);
    expect(component.formValid()).toBe(false); // salaire hebdo manque.

    component.salaireHebdomadaireBrut.set(1200);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : si clause sauvegarde OFF → champs conditionnels non requis', () => {
    component.ancienneteAnneesPreStatutUnique.set(10);
    component.remunerationAnnuelleBruteEnMilliers.set(60);
    component.appliquerClauseSauvegarde.set(false);
    // ancienneté post + salaire hebdo null → toujours valide.
    expect(component.formValid()).toBe(true);
  });

  it('formValid : rémunération K€ ≤ 0 → invalid', () => {
    component.ancienneteAnneesPreStatutUnique.set(10);
    component.remunerationAnnuelleBruteEnMilliers.set(0);
    component.appliquerClauseSauvegarde.set(false);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // Toggle conditionnel (UX)
  // ============================================================

  it('toggle off → reset des 2 champs conditionnels + provenances', () => {
    component.appliquerClauseSauvegarde.set(true);
    component.ancienneteAnneesPostStatutUnique.set(12);
    component.salaireHebdomadaireBrut.set(1200);
    component.provenanceAncienneteEPost.set('IA');
    component.provenanceSalaireHebdo.set('IA');

    component.onAppliquerClauseSauvegardeChange(false);

    expect(component.appliquerClauseSauvegarde()).toBe(false);
    expect(component.ancienneteAnneesPostStatutUnique()).toBeNull();
    expect(component.salaireHebdomadaireBrut()).toBeNull();
    expect(component.provenanceAncienneteEPost()).toBeNull();
    expect(component.provenanceSalaireHebdo()).toBeNull();
  });

  it('toggle on → réactive le bloc conditionnel (DOM)', () => {
    // workspaceCountry FRANCE pour éviter le GET initial déclenché par ngOnInit
    // (test centré sur le DOM conditionnel — gate FR masque le form de toute façon).
    // On utilise BELGIQUE et on flush le 404 attendu.
    component.collapsed.set(false);
    component.ancienneteAnneesPreStatutUnique.set(10);
    component.remunerationAnnuelleBruteEnMilliers.set(60);
    component.appliquerClauseSauvegarde.set(false);
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    // Quand off → pas de champs conditionnels visibles.
    const inputsOff = fixture.nativeElement.querySelectorAll('input[name="ancienneteAnneesPostStatutUnique"]');
    expect(inputsOff.length).toBe(0);

    component.appliquerClauseSauvegarde.set(true);
    fixture.detectChanges();

    const inputsOn = fixture.nativeElement.querySelectorAll('input[name="ancienneteAnneesPostStatutUnique"]');
    expect(inputsOn.length).toBe(1);
  });

  // ============================================================
  // calculate()
  // ============================================================

  it('calculate() POST avec clause sauvegarde ON → envoie les 6 champs', () => {
    component.ancienneteAnneesPreStatutUnique.set(12);
    component.ancienneteMoisPreStatutUnique.set(0);
    component.remunerationAnnuelleBruteEnMilliers.set(60);
    component.appliquerClauseSauvegarde.set(true);
    component.ancienneteAnneesPostStatutUnique.set(12);
    component.salaireHebdomadaireBrut.set(1200);

    component.calculate();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      ancienneteAnneesPreStatutUnique: 12,
      ancienneteMoisPreStatutUnique: 0,
      remunerationAnnuelleBruteEnMilliers: 60,
      appliquerClauseSauvegarde: true,
      ancienneteAnneesPostStatutUnique: 12,
      salaireHebdomadaireBrut: 1200,
    });
    req.flush(response());

    expect(component.result()!.preavisClaeysMois).toBe(11.52);
    expect(component.showForm()).toBe(false);
  });

  it('calculate() POST avec clause sauvegarde OFF → 2 champs conditionnels à null', () => {
    component.ancienneteAnneesPreStatutUnique.set(12);
    component.ancienneteMoisPreStatutUnique.set(0);
    component.remunerationAnnuelleBruteEnMilliers.set(60);
    component.appliquerClauseSauvegarde.set(false);

    component.calculate();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body).toEqual({
      ancienneteAnneesPreStatutUnique: 12,
      ancienneteMoisPreStatutUnique: 0,
      remunerationAnnuelleBruteEnMilliers: 60,
      appliquerClauseSauvegarde: false,
      ancienneteAnneesPostStatutUnique: null,
      salaireHebdomadaireBrut: null,
    });
    req.flush(response({
      appliquerClauseSauvegarde: false,
      ancienneteAnneesPostStatutUnique: null,
      salaireHebdomadaireBrut: null,
      preavisStatutUniquesSemaines: 0,
      preavisTotalSemaines: 50,
      indemniteTotaleBrute: null,
    }));

    expect(component.result()!.preavisStatutUniquesSemaines).toBe(0);
    expect(component.result()!.indemniteTotaleBrute).toBeNull();
  });

  it('calculate() FRANCE → snackbar erreur + pas de requête', () => {
    component.workspaceCountry = 'FRANCE';
    component.ancienneteAnneesPreStatutUnique.set(10);
    component.remunerationAnnuelleBruteEnMilliers.set(60);
    component.appliquerClauseSauvegarde.set(false);
    component.calculate();
    expect(snackSpy.open).toHaveBeenCalled();
    httpMock.expectNone((r) => r.url === BASE_URL);
  });

  it('calculate() 400 → snackbar avec message backend', () => {
    component.ancienneteAnneesPreStatutUnique.set(10);
    component.remunerationAnnuelleBruteEnMilliers.set(60);
    component.appliquerClauseSauvegarde.set(false);
    component.calculate();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Données invalides' }, { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      expect.stringContaining('Données invalides'),
      'Fermer',
      expect.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  // ============================================================
  // Affichage résultat + bannière avertissement + cumul
  // ============================================================

  it('bannière avertissement jurisprudentiel TOUJOURS affichée', () => {
    component.forceExpanded = true;
    fixture.detectChanges(); // déclenche ngOnInit → 1 GET.
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('.result-alert');
    expect(alert).not.toBeNull();
    expect(alert.textContent).toContain('jurisprudentielle');
  });

  it('affichage cumul si preavisStatutUniquesSemaines > 0', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response()); // preavisStatutUniquesSemaines = 36.
    fixture.detectChanges();

    const sub = fixture.nativeElement.querySelector('.result-banner-sub');
    expect(sub).not.toBeNull();
    expect(sub.textContent).toContain('Cumul clause de sauvegarde');
    expect(sub.textContent).toContain('50 sem');
    expect(sub.textContent).toContain('36 sem');
    expect(sub.textContent).toContain('86 sem');
  });

  it('affichage Claeys seule si preavisStatutUniquesSemaines == 0', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      preavisStatutUniquesSemaines: 0,
      preavisTotalSemaines: 50,
      indemniteTotaleBrute: null,
    }));
    fixture.detectChanges();

    const sub = fixture.nativeElement.querySelector('.result-banner-sub');
    expect(sub).not.toBeNull();
    expect(sub.textContent).toContain('Claeys seule');
  });

  it('formule en JetBrains Mono dans le breakdown', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();

    const mono = fixture.nativeElement.querySelector('.bd-value--mono');
    expect(mono).not.toBeNull();
    expect(mono.textContent).toContain('0.8');
  });

  // ============================================================
  // Statics F-177 / F-236
  // ============================================================

  it('static TOOL_LABEL et TOOL_ICON présents', () => {
    expect(LicenciementBeFormuleClaeysSectionComponent.TOOL_LABEL)
      .toBe('PRÉAVIS FORMULE CLAEYS (BE)');
    expect(LicenciementBeFormuleClaeysSectionComponent.TOOL_ICON)
      .toBe('history_edu');
  });

  it('static getPrefillCount délègue au helper (parité)', () => {
    expect(LicenciementBeFormuleClaeysSectionComponent.getPrefillCount({
      aiData: {} as TravailExtractedData,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);

    expect(LicenciementBeFormuleClaeysSectionComponent.getPrefillCount({
      aiData: {
        dateEntree: '2008-06-15',
        dateLicenciement: '2026-03-15',
        salaireBrutAnnuel: 60000,
      } as TravailExtractedData,
      workspaceCountry: 'BELGIQUE',
    })).toBe(4);
  });

  it('static getPrefillCount FRANCE → 0', () => {
    expect(LicenciementBeFormuleClaeysSectionComponent.getPrefillCount({
      aiData: {
        dateEntree: '2008-06-01',
        salaireBrutAnnuel: 60000,
      } as TravailExtractedData,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });
});
