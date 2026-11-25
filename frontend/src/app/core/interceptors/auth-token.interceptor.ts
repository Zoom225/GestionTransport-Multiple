import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { inject } from '@angular/core';
import { AdminSessionService } from '../auth/admin-session.service';
import { VoyageurSessionService } from '../auth/voyageur-session.service';

export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {
  const adminSession = inject(AdminSessionService);
  const voyageurSession = inject(VoyageurSessionService);

  const token = adminSession.token() || voyageurSession.token();

  const apiBaseUrl = environment.apiBaseUrl;
  const path = req.url.split('?')[0];
  const isApiCall = path === apiBaseUrl || path.startsWith(`${apiBaseUrl}/`);
  const isLoginCall = path === `${apiBaseUrl}/auth/login` || path === `${apiBaseUrl}/voyageurs/login`;

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
