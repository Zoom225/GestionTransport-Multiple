import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AdminSessionService } from '../auth/admin-session.service';
import { VoyageurSessionService } from '../auth/voyageur-session.service';

export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {
  const adminSession = inject(AdminSessionService);
  const voyageurSession = inject(VoyageurSessionService);

  const token = adminSession.token() || voyageurSession.token();

  const isApiCall = req.url.startsWith('/api');
  const isLoginCall = req.url.includes('/api/auth/login') || req.url.includes('/api/voyageurs/login'); // Modified

  if (!token || !isApiCall || isLoginCall) {
    return next(req);
  }

  return next(
    req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    })
  );
};
