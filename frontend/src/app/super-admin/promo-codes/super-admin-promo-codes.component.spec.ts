import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';
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
  // ---------------------------------------------------------------------------
  it('T-08: création STRIPE_DISCOUNT envoie valueDays=null au backend', fakeAsync(() => {
    const created: PromoCodeDto = {
      ...mockCode1,
      type: 'STRIPE_DISCOUNT',
      valueDays: null,
      code: 'STRIPE10',
    };
    setup({ createReturn: of(created) });
    promoService.listCodes.mockReturnValue(of([created]));

    component.form.get('type')?.setValue('STRIPE_DISCOUNT');
    component.form.patchValue({
      code: 'STRIPE10',
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
  // T-09 : erreur 403 sur listing → redirection /case-files
  // ---------------------------------------------------------------------------
  it('T-09: erreur 403 sur listCodes → redirige vers /case-files', fakeAsync(() => {
    setup({
      listReturn: throwError(() => ({ status: 403 })),
    });
    expect(router.navigate).toHaveBeenCalledWith(['/case-files']);
  }));

  // ---------------------------------------------------------------------------
  // T-10 : submit ignoré si formulaire invalide
  // ---------------------------------------------------------------------------
  it('T-10: submit ne déclenche pas createCode si le formulaire est invalide', fakeAsync(() => {
    setup();
    component.form.patchValue({ code: '' }); // viole required
    component.submit();
    tick();
    expect(promoService.createCode).not.toHaveBeenCalled();
  }));

  // ---------------------------------------------------------------------------
  // T-11 : bouton « Désactiver » caché pour un code déjà inactif
  // ---------------------------------------------------------------------------
  it('T-11: la table ne rend pas le bouton désactiver pour un code déjà inactif', fakeAsync(() => {
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
