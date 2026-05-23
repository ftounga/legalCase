import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';
import { Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';

import { SuperAdminPromoCodesComponent } from './super-admin-promo-codes.component';
import { PromoCodeAdminService } from './promo-code-admin.service';
import { PromoCodeDto } from './promo-code.model';
import { AuthService } from '../../core/services/auth.service';
import { User } from '../../core/models/user.model';

const mockSuperAdmin: User = {
  id: 'u-1',
  email: 'admin@test.com',
  firstName: 'Franck',
  lastName: 'Tounga',
  provider: 'GOOGLE',
  isSuperAdmin: true,
};

const mockCode1: PromoCodeDto = {
  id: '11111111-1111-1111-1111-111111111111',
  code: 'BARREAU2026',
  type: 'TRIAL_EXTENSION',
  valueDays: 30,
  stripeCouponId: null,
  stripePromotionCodeId: null,
  valueOffType: null,
  valueOffAmount: null,
  currency: null,
  duration: null,
  partnerLabel: 'Barreau de Bordeaux',
  maxUses: 100,
  usesCount: 5,
  expiresAt: '2027-01-01T23:59:59Z',
  active: true,
  createdAt: '2026-05-23T10:00:00Z',
  createdByUserId: 'u-1',
};

const mockCode2: PromoCodeDto = {
  ...mockCode1,
  id: '22222222-2222-2222-2222-222222222222',
  code: 'BARREAUPARIS',
  partnerLabel: 'Barreau de Paris',
  active: false,
  usesCount: 100,
};

describe('SuperAdminPromoCodesComponent', () => {
  let component: SuperAdminPromoCodesComponent;
  let fixture: ComponentFixture<SuperAdminPromoCodesComponent>;
  let promoService: jest.Mocked<PromoCodeAdminService>;
  let snackBar: jest.Mocked<MatSnackBar>;
  let dialog: jest.Mocked<MatDialog>;
  let router: Router;

  function setup(opts: {
    user?: User | null;
    listReturn?: any;
    createReturn?: any;
    deactivateReturn?: any;
    dialogResult?: boolean | undefined;
  } = {}) {
    promoService = jasmine.createSpyObj('PromoCodeAdminService', [
      'createCode', 'listCodes', 'deactivateCode',
    ]);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    dialog = jasmine.createSpyObj('MatDialog', ['open']);

    promoService.listCodes.mockReturnValue(
      opts.listReturn ?? of([mockCode1, mockCode2]),
    );
    if (opts.createReturn) {
      promoService.createCode.mockReturnValue(opts.createReturn);
    }
    if (opts.deactivateReturn) {
      promoService.deactivateCode.mockReturnValue(opts.deactivateReturn);
    }

    // Mock du MatDialog : retourne un faux MatDialogRef dont afterClosed renvoie la valeur attendue
    const dialogRefStub = {
      afterClosed: () => of(opts.dialogResult),
    } as unknown as MatDialogRef<unknown>;
    dialog.open.mockReturnValue(dialogRefStub);

    const userSignal = signal<User | null>(
      opts.user === undefined ? mockSuperAdmin : opts.user,
    );
    const authStub: Partial<AuthService> = { currentUser: userSignal };

    TestBed.configureTestingModule({
      imports: [SuperAdminPromoCodesComponent, NoopAnimationsModule],
      providers: [
        { provide: PromoCodeAdminService, useValue: promoService },
        { provide: AuthService, useValue: authStub },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
        provideRouter([]),
      ],
    });

    fixture = TestBed.createComponent(SuperAdminPromoCodesComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    jest.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
  }

  // ---------------------------------------------------------------------------
  // T-01 : listing au ngOnInit
  // ---------------------------------------------------------------------------
  it('T-01: charge la liste des codes au ngOnInit et l\'affiche dans la table', fakeAsync(() => {
    setup();
    expect(promoService.listCodes).toHaveBeenCalledTimes(1);
    expect(component.codes().length).toBe(2);
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('BARREAU2026');
    expect(text).toContain('BARREAUPARIS');
    expect(text).toContain('Barreau de Bordeaux');
  }));

  // ---------------------------------------------------------------------------
  // T-02 : non super-admin → redirection /case-files
  // ---------------------------------------------------------------------------
  it('T-02: redirige vers /case-files si non super-admin', fakeAsync(() => {
    setup({ user: { ...mockSuperAdmin, isSuperAdmin: false } });
    expect(router.navigate).toHaveBeenCalledWith(['/case-files']);
    expect(promoService.listCodes).not.toHaveBeenCalled();
  }));

  // ---------------------------------------------------------------------------
  // T-03 : création nominale → toast succès + refresh table + reset form
  // ---------------------------------------------------------------------------
  it('T-03: submit nominal POST createCode, toast succès, reset form et refresh liste', fakeAsync(() => {
    const newCode: PromoCodeDto = { ...mockCode1, id: '99999999-9999-9999-9999-999999999999', code: 'NEW2026' };
    setup({ createReturn: of(newCode) });
    promoService.listCodes.mockClear();
    promoService.listCodes.mockReturnValue(of([newCode, mockCode1, mockCode2]));

    component.form.patchValue({
      code: 'new2026',
      type: 'TRIAL_EXTENSION',
      valueDays: 30,
      partnerLabel: 'Barreau Lyon',
      maxUses: 50,
      expiresAt: '2027-01-01',
    });

    component.submit();
    tick();

    expect(promoService.createCode).toHaveBeenCalledTimes(1);
    const sent = promoService.createCode.mock.calls[0][0];
    expect(sent.code).toBe('NEW2026'); // uppercase appliqué côté client
    expect(sent.expiresAt).toBe('2027-01-01T23:59:59Z'); // fin de journée UTC
    expect(sent.type).toBe('TRIAL_EXTENSION');
    expect(sent.valueDays).toBe(30);

    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('NEW2026'),
      'Fermer',
      expect.objectContaining({ panelClass: ['snack-success'] }),
    );
    expect(promoService.listCodes).toHaveBeenCalledTimes(1);
    expect(component.form.get('code')?.value).toBe('');
  }));

  // ---------------------------------------------------------------------------
  // T-04 : erreur 409 PROMO_CODE_DUPLICATE → toast d'erreur avec message backend
  // ---------------------------------------------------------------------------
  it('T-04: erreur 409 → toast d\'erreur avec le message backend', fakeAsync(() => {
    setup({
      createReturn: throwError(() => ({
        status: 409,
        error: { message: 'Code BARREAU2026 already exists', code: 'PROMO_CODE_DUPLICATE' },
      })),
    });

    component.form.patchValue({
      code: 'BARREAU2026',
      type: 'TRIAL_EXTENSION',
      valueDays: 30,
      partnerLabel: 'Doublon',
      maxUses: 50,
      expiresAt: '2027-01-01',
    });

    component.submit();
    tick();

    expect(snackBar.open).toHaveBeenCalledWith(
      'Code BARREAU2026 already exists',
      'Fermer',
      expect.objectContaining({ panelClass: ['snack-error'] }),
    );
    expect(component.submitting()).toBe(false);
  }));

  // ---------------------------------------------------------------------------
  // T-05 : désactivation avec confirm=true → POST + update ligne + toast
  // ---------------------------------------------------------------------------
  it('T-05: désactivation avec dialog confirm=true appelle deactivateCode et met à jour la ligne', fakeAsync(() => {
    const deactivated: PromoCodeDto = { ...mockCode1, active: false };
    setup({
      deactivateReturn: of(deactivated),
      dialogResult: true,
    });

    component.confirmDeactivate(mockCode1);
    tick();

    expect(dialog.open).toHaveBeenCalledTimes(1);
    expect(promoService.deactivateCode).toHaveBeenCalledWith(mockCode1.id);
    const updatedRow = component.codes().find(c => c.id === mockCode1.id);
    expect(updatedRow?.active).toBe(false);
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('désactivé'),
      'Fermer',
      expect.objectContaining({ panelClass: ['snack-success'] }),
    );
  }));

  // ---------------------------------------------------------------------------
  // T-06 : désactivation avec confirm=false → aucun appel service
  // ---------------------------------------------------------------------------
  it('T-06: désactivation annulée via dialog (confirm=false) n\'appelle pas le service', fakeAsync(() => {
    setup({ dialogResult: false });

    component.confirmDeactivate(mockCode1);
    tick();

    expect(dialog.open).toHaveBeenCalledTimes(1);
    expect(promoService.deactivateCode).not.toHaveBeenCalled();
  }));

  // ---------------------------------------------------------------------------
  // T-07 : conditional rendering valueDays selon type
  // ---------------------------------------------------------------------------
  it('T-07: valueDays visible et required si type=TRIAL_EXTENSION, caché et disabled si STRIPE_DISCOUNT', fakeAsync(() => {
    setup();

    // Par défaut TRIAL_EXTENSION
    expect(component.isTrialExtension()).toBe(true);
    let valueDaysField = fixture.nativeElement.querySelector('[data-testid="value-days-field"]');
    expect(valueDaysField).not.toBeNull();
    expect(component.form.get('valueDays')?.enabled).toBe(true);

    // Switch vers STRIPE_DISCOUNT
    component.form.get('type')?.setValue('STRIPE_DISCOUNT');
    tick();
    fixture.detectChanges();

    expect(component.isTrialExtension()).toBe(false);
    valueDaysField = fixture.nativeElement.querySelector('[data-testid="value-days-field"]');
    expect(valueDaysField).toBeNull();
    expect(component.form.get('valueDays')?.disabled).toBe(true);
  }));

  // ---------------------------------------------------------------------------
  // T-08 : création avec type=STRIPE_DISCOUNT envoie valueDays=null
  // (SF-255-02b — les champs Stripe sont maintenant requis pour valider le form)
  // ---------------------------------------------------------------------------
  it('T-08: création STRIPE_DISCOUNT envoie valueDays=null au backend', fakeAsync(() => {
    const created: PromoCodeDto = {
      ...mockCode1,
      type: 'STRIPE_DISCOUNT',
      valueDays: null,
      valueOffType: 'PERCENT',
      valueOffAmount: 10,
      duration: 'ONCE',
      code: 'STRIPE10',
    };
    setup({ createReturn: of(created) });
    promoService.listCodes.mockReturnValue(of([created]));

    component.form.get('type')?.setValue('STRIPE_DISCOUNT');
    component.form.get('valueOffType')?.setValue('PERCENT');
    component.form.patchValue({
      code: 'STRIPE10',
      valueOffAmount: 10,
      duration: 'ONCE',
      partnerLabel: 'Partenaire X',
      maxUses: 10,
      expiresAt: '2027-12-31',
    });
    tick();

    component.submit();
    tick();

    expect(promoService.createCode).toHaveBeenCalledTimes(1);
    const sent = promoService.createCode.mock.calls[0][0];
    expect(sent.type).toBe('STRIPE_DISCOUNT');
    expect(sent.valueDays).toBeNull();
  }));

  // ---------------------------------------------------------------------------
  // T-09 (SF-255-02b) : sélection STRIPE_DISCOUNT → valueDays retiré des
  // validators, valueOffType + valueOffAmount + duration sont required
  // ---------------------------------------------------------------------------
  it('T-09: switch type=STRIPE_DISCOUNT → valueDays non required, valueOffType/valueOffAmount/duration required', fakeAsync(() => {
    setup();
    component.form.get('type')?.setValue('STRIPE_DISCOUNT');
    // valueOffType doit être choisi pour activer valueOffAmount → on choisit PERCENT
    component.form.get('valueOffType')?.setValue('PERCENT');
    tick();
    fixture.detectChanges();

    expect(component.isStripeDiscount()).toBe(true);
    // valueDays n'est plus required
    expect(component.form.get('valueDays')?.hasValidator(Validators.required)).toBe(false);
    // Les 3 champs Stripe sont required (valueOffAmount via la branche PERCENT)
    expect(component.form.get('valueOffType')?.hasValidator(Validators.required)).toBe(true);
    expect(component.form.get('valueOffAmount')?.hasValidator(Validators.required)).toBe(true);
    expect(component.form.get('duration')?.hasValidator(Validators.required)).toBe(true);
  }));

  // ---------------------------------------------------------------------------
  // T-10 (SF-255-02b) : STRIPE_DISCOUNT + valueOffType=AMOUNT → currency required
  // et value pré-remplie EUR
  // ---------------------------------------------------------------------------
  it('T-10: STRIPE_DISCOUNT + AMOUNT → currency required, EUR pré-rempli', fakeAsync(() => {
    setup();
    component.form.get('type')?.setValue('STRIPE_DISCOUNT');
    component.form.get('valueOffType')?.setValue('AMOUNT');
    tick();
    fixture.detectChanges();

    expect(component.isAmountOff()).toBe(true);
    expect(component.form.get('currency')?.hasValidator(Validators.required)).toBe(true);
    expect(component.form.get('currency')?.value).toBe('EUR');
    const currencyField = fixture.nativeElement.querySelector('[data-testid="currency-field"]');
    expect(currencyField).not.toBeNull();
  }));

  // ---------------------------------------------------------------------------
  // T-11 (SF-255-02b) : STRIPE_DISCOUNT + valueOffType=PERCENT → currency non
  // required et value null (le champ est masqué du DOM)
  // ---------------------------------------------------------------------------
  it('T-11: STRIPE_DISCOUNT + PERCENT → currency non required, value null, masqué', fakeAsync(() => {
    setup();
    component.form.get('type')?.setValue('STRIPE_DISCOUNT');
    component.form.get('valueOffType')?.setValue('AMOUNT'); // pré-remplit EUR
    tick();
    component.form.get('valueOffType')?.setValue('PERCENT'); // doit retirer EUR
    tick();
    fixture.detectChanges();

    expect(component.isPercentOff()).toBe(true);
    expect(component.form.get('currency')?.hasValidator(Validators.required)).toBe(false);
    expect(component.form.get('currency')?.value).toBeNull();
    const currencyField = fixture.nativeElement.querySelector('[data-testid="currency-field"]');
    expect(currencyField).toBeNull();
  }));

  // ---------------------------------------------------------------------------
  // T-12 (SF-255-02b) : submit STRIPE_DISCOUNT/PERCENT → payload contient
  // valueOffType, valueOffAmount, duration, et null pour valueDays + currency
  // ---------------------------------------------------------------------------
  it('T-12: submit STRIPE_DISCOUNT/PERCENT envoie valueOffType/valueOffAmount/duration et null pour valueDays + currency', fakeAsync(() => {
    const created: PromoCodeDto = {
      ...mockCode1,
      type: 'STRIPE_DISCOUNT',
      valueDays: null,
      valueOffType: 'PERCENT',
      valueOffAmount: 15,
      currency: null,
      duration: 'ONCE',
      code: 'STRIPE15',
    };
    setup({ createReturn: of(created) });
    promoService.listCodes.mockReturnValue(of([created]));

    component.form.get('type')?.setValue('STRIPE_DISCOUNT');
    component.form.get('valueOffType')?.setValue('PERCENT');
    component.form.patchValue({
      code: 'STRIPE15',
      valueOffAmount: 15,
      duration: 'ONCE',
      partnerLabel: 'Partenaire X',
      maxUses: 10,
      expiresAt: '2027-12-31',
    });
    tick();

    component.submit();
    tick();

    expect(promoService.createCode).toHaveBeenCalledTimes(1);
    const sent = promoService.createCode.mock.calls[0][0];
    expect(sent.type).toBe('STRIPE_DISCOUNT');
    expect(sent.valueOffType).toBe('PERCENT');
    expect(sent.valueOffAmount).toBe(15);
    expect(sent.duration).toBe('ONCE');
    expect(sent.valueDays).toBeNull();
    expect(sent.currency).toBeNull();
  }));

  // ---------------------------------------------------------------------------
  // T-13 (SF-255-02b) : submit STRIPE_DISCOUNT/AMOUNT → payload contient
  // currency='EUR'
  // ---------------------------------------------------------------------------
  it('T-13: submit STRIPE_DISCOUNT/AMOUNT envoie currency=EUR', fakeAsync(() => {
    const created: PromoCodeDto = {
      ...mockCode1,
      type: 'STRIPE_DISCOUNT',
      valueDays: null,
      valueOffType: 'AMOUNT',
      valueOffAmount: 5000,
      currency: 'EUR',
      duration: 'REPEATING_3',
      code: 'STRIPE50EUR',
    };
    setup({ createReturn: of(created) });
    promoService.listCodes.mockReturnValue(of([created]));

    component.form.get('type')?.setValue('STRIPE_DISCOUNT');
    component.form.get('valueOffType')?.setValue('AMOUNT');
    component.form.patchValue({
      code: 'STRIPE50EUR',
      valueOffAmount: 5000,
      duration: 'REPEATING_3',
      partnerLabel: 'Partenaire Y',
      maxUses: 25,
      expiresAt: '2027-12-31',
    });
    tick();

    component.submit();
    tick();

    expect(promoService.createCode).toHaveBeenCalledTimes(1);
    const sent = promoService.createCode.mock.calls[0][0];
    expect(sent.type).toBe('STRIPE_DISCOUNT');
    expect(sent.valueOffType).toBe('AMOUNT');
    expect(sent.valueOffAmount).toBe(5000);
    expect(sent.currency).toBe('EUR');
    expect(sent.duration).toBe('REPEATING_3');
    expect(sent.valueDays).toBeNull();
  }));

  // ---------------------------------------------------------------------------
  // T-14 (SF-255-02b) : switch STRIPE_DISCOUNT → TRIAL_EXTENSION → les 4
  // validators Stripe sont retirés et valueDays est required à nouveau
  // (anti-régression sur les validators résiduels qui bloqueraient le submit)
  // ---------------------------------------------------------------------------
  it('T-14: switch STRIPE_DISCOUNT → TRIAL_EXTENSION nettoie tous les validators Stripe et restaure valueDays required', fakeAsync(() => {
    setup();
    // 1. passage STRIPE_DISCOUNT + AMOUNT pour activer tous les validators
    component.form.get('type')?.setValue('STRIPE_DISCOUNT');
    component.form.get('valueOffType')?.setValue('AMOUNT');
    tick();
    fixture.detectChanges();
    expect(component.form.get('currency')?.hasValidator(Validators.required)).toBe(true);

    // 2. retour vers TRIAL_EXTENSION
    component.form.get('type')?.setValue('TRIAL_EXTENSION');
    tick();
    fixture.detectChanges();

    expect(component.isTrialExtension()).toBe(true);
    // valueDays redevient required
    expect(component.form.get('valueDays')?.hasValidator(Validators.required)).toBe(true);
    expect(component.form.get('valueDays')?.enabled).toBe(true);
    // Aucun validator Stripe résiduel
    expect(component.form.get('valueOffType')?.hasValidator(Validators.required)).toBe(false);
    expect(component.form.get('valueOffAmount')?.hasValidator(Validators.required)).toBe(false);
    expect(component.form.get('currency')?.hasValidator(Validators.required)).toBe(false);
    expect(component.form.get('duration')?.hasValidator(Validators.required)).toBe(false);
    // Valeurs Stripe purgées pour cohérence du payload
    expect(component.form.get('valueOffType')?.value).toBeNull();
    expect(component.form.get('valueOffAmount')?.value).toBeNull();
    expect(component.form.get('currency')?.value).toBeNull();
    expect(component.form.get('duration')?.value).toBeNull();
  }));

  // ---------------------------------------------------------------------------
  // T-15 : erreur 403 sur listing → redirection /case-files
  // ---------------------------------------------------------------------------
  it('T-15: erreur 403 sur listCodes → redirige vers /case-files', fakeAsync(() => {
    setup({
      listReturn: throwError(() => ({ status: 403 })),
    });
    expect(router.navigate).toHaveBeenCalledWith(['/case-files']);
  }));

  // ---------------------------------------------------------------------------
  // T-16 : submit ignoré si formulaire invalide
  // ---------------------------------------------------------------------------
  it('T-16: submit ne déclenche pas createCode si le formulaire est invalide', fakeAsync(() => {
    setup();
    component.form.patchValue({ code: '' }); // viole required
    component.submit();
    tick();
    expect(promoService.createCode).not.toHaveBeenCalled();
  }));

  // ---------------------------------------------------------------------------
  // T-17 : bouton « Désactiver » caché pour un code déjà inactif
  // ---------------------------------------------------------------------------
  it('T-17: la table ne rend pas le bouton désactiver pour un code déjà inactif', fakeAsync(() => {
    setup();
    const inactiveBtn = fixture.nativeElement.querySelector(
      `[data-testid="deactivate-${mockCode2.id}"]`,
    );
    const activeBtn = fixture.nativeElement.querySelector(
      `[data-testid="deactivate-${mockCode1.id}"]`,
    );
    expect(inactiveBtn).toBeNull();
    expect(activeBtn).not.toBeNull();
  }));
});
