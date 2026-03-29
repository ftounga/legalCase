import {
  animate,
  animation,
  query,
  stagger,
  style,
  transition,
  trigger,
  useAnimation,
} from '@angular/animations';

// ─── Easing professionnel ─────────────────────────────────────────────────────
// cubic-bezier(0.22, 1, 0.36, 1) = décélération rapide, fin fluide (type iOS)
const EASING = 'cubic-bezier(0.22, 1, 0.36, 1)';

// ─── Keyframes réutilisables ──────────────────────────────────────────────────

export const fadeInUpAnimation = animation([
  style({ opacity: 0, transform: 'translateY({{offset}}px)' }),
  animate('{{duration}} {{easing}}', style({ opacity: 1, transform: 'translateY(0)' })),
], { params: { duration: '380ms', easing: EASING, offset: 20 } });

export const slideInRightAnimation = animation([
  style({ opacity: 0, transform: 'translateX(24px)' }),
  animate(`280ms ${EASING}`, style({ opacity: 1, transform: 'translateX(0)' })),
]);

// ─── Trigger : fade-in page/section ──────────────────────────────────────────

export const fadeInUp = trigger('fadeInUp', [
  transition(':enter', [
    useAnimation(fadeInUpAnimation),
  ]),
]);

// ─── Trigger : stagger liste ─────────────────────────────────────────────────

export const listStagger = trigger('listStagger', [
  transition('* => *', [
    query(':enter', [
      style({ opacity: 0, transform: 'translateY(16px)' }),
      stagger('60ms', [
        animate(`320ms ${EASING}`, style({ opacity: 1, transform: 'translateY(0)' })),
      ]),
    ], { optional: true }),
  ]),
]);

// ─── Trigger : slide-in depuis la droite ─────────────────────────────────────

export const slideInRight = trigger('slideInRight', [
  transition(':enter', [
    useAnimation(slideInRightAnimation),
  ]),
]);
