import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';
import { AlertService } from './alert.service';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const alertService = inject(AlertService);
  const token = authService.getToken();

  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      const isLoginUrl = req.url.includes('/auth/login');
      if (!isLoginUrl && (error.status === 401 || error.status === 403)) {
        authService.logout();
        router.navigate(['/login']);
        alertService.error(
          'Acceso Inhabilitado', 
          'Su sesión ha expirado o su cuenta ha sido inhabilitada. Por favor, inicie sesión nuevamente o póngase en contacto con el administrador.'
        );
      }
      return throwError(() => error);
    })
  );
};
