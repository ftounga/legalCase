/**
 * F-240 / SF-240-03 — Tests unitaires de PaymentTermsAcceptanceDialogComponent.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import {
  PaymentTermsAcceptanceDialogComponent,
  PaymentTermsDialogData
} from './payment-terms-acceptance-dialog.component';

const subscriptionData: PaymentTermsDialogData = {
  planLabel: 'Solo',
  price: '99 €',
  type: 'SUBSCRIPTION'
};

const topupData: PaymentTermsDialogData = {
  planLabel: '1M tokens',
  price: '9,90 €',
  type: 'TOPUP'
};

async function buildDialog(data: PaymentTermsDialogData): Promise<{
  fixture: ComponentFixture<PaymentTermsAcceptanceDialogComponent>;
  component: PaymentTermsAcceptanceDialogComponent;
  dialogRefSpy: jest.Mocked<MatDialogRef<PaymentTermsAcceptanceDialogComponent, boolean>>;
}> {
  const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

  await TestBed.configureTestingModule({
    imports: [PaymentTermsAcceptanceDialogComponent, NoopAnimationsModule],
    providers: [
      { provide: MatDialogRef, useValue: dialogRefSpy },
      { provide: MAT_DIALOG_DATA, useValue: data }
    ]
  }).compileComponents();

  const fixture = TestBed.createComponent(PaymentTermsAcceptanceDialogComponent);
  const component = fixture.componentInstance;
  fixture.detectChanges();
  return { fixture, component, dialogRefSpy };
}

describe('PaymentTermsAcceptanceDialogComponent', () => {
  describe('PD-01 — checkbox initialement non cochée → bouton confirm désactivé', () => {
    it('acceptedTerms est false à l\'initialisation', async () => {
      const { component } = await buildDialog(subscriptionData);
      expect(component.acceptedTerms).toBe(false);
    });

    it('le bouton "Confirmer et payer" est disabled quand acceptedTerms = false', async () => {
      const { fixture } = await buildDialog(subscriptionData);
      const btn = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
        'button[color="primary"]'
      );
      expect(btn?.disabled).toBe(true);
    });
  });

  describe('PD-02 — checkbox cochée → bouton confirm actif', () => {
    it('le bouton "Confirmer et payer" est enabled quand acceptedTerms = true', async () => {
      const { component, fixture } = await buildDialog(subscriptionData);
      component.acceptedTerms = true;
      fixture.detectChanges();
      const btn = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
        'button[color="primary"]'
      );
      expect(btn?.disabled).toBeFalsy();
    });
  });

  describe('PD-03 — clic confirm → dialogRef.close(true)', () => {
    it('appelle dialogRef.close(true) quand acceptedTerms = true', async () => {
      const { component, dialogRefSpy } = await buildDialog(subscriptionData);
      component.acceptedTerms = true;
      component.confirm();
      expect(dialogRefSpy.close).toHaveBeenCalledWith(true);
    });

    it('n\'appelle pas dialogRef.close si acceptedTerms = false', async () => {
      const { component, dialogRefSpy } = await buildDialog(subscriptionData);
      component.acceptedTerms = false;
      component.confirm();
      expect(dialogRefSpy.close).not.toHaveBeenCalled();
    });
  });

  describe('PD-04 — clic annuler → dialogRef.close(false)', () => {
    it('appelle dialogRef.close(false) sur cancel()', async () => {
      const { component, dialogRefSpy } = await buildDialog(subscriptionData);
      component.cancel();
      expect(dialogRefSpy.close).toHaveBeenCalledWith(false);
    });
  });

  describe('PD-05 — affichage de planLabel et price', () => {
    it('affiche le libellé du plan SUBSCRIPTION dans le titre', async () => {
      const { fixture } = await buildDialog(subscriptionData);
      const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(html).toContain('Solo');
    });

    it('affiche le prix pour SUBSCRIPTION', async () => {
      const { fixture } = await buildDialog(subscriptionData);
      const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(html).toContain('99 €');
    });

    it('affiche le libellé du pack TOPUP', async () => {
      const { fixture } = await buildDialog(topupData);
      const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(html).toContain('1M tokens');
    });

    it('affiche le prix pour TOPUP', async () => {
      const { fixture } = await buildDialog(topupData);
      const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(html).toContain('9,90 €');
    });

    it('affiche les liens vers /cgu et /privacy', async () => {
      const { fixture } = await buildDialog(subscriptionData);
      const links = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLAnchorElement>('a');
      const hrefs = Array.from(links).map(a => a.getAttribute('href'));
      expect(hrefs).toContain('/cgu');
      expect(hrefs).toContain('/privacy');
    });

    it('les liens CGU et Privacy s\'ouvrent en nouvel onglet', async () => {
      const { fixture } = await buildDialog(subscriptionData);
      const links = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLAnchorElement>('a');
      links.forEach(link => {
        expect(link.getAttribute('target')).toBe('_blank');
      });
    });
  });
});
