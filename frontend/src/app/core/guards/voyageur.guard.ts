import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { VoyageurSessionService } from '../auth/voyageur-session.service';

export const voyageurGuard: CanActivateFn = () => {
  const router = inject(Router);
  const session = inject(VoyageurSessionService);

  if (session.isAuthenticated()) {
    return true;
  }

  return router.parseUrl('/voyageur');
};

