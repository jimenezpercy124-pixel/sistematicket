import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    const userInfo = JSON.parse(sessionStorage.getItem('user_info') || '{}');
    const userRole = userInfo.rol;
    const debeCambiar = userInfo.debeCambiarPassword;

    // Si debe cambiar password y NO está en la ruta de cambio, obligarlo a ir
    if (debeCambiar && state.url !== '/actualizar-password') {
      router.navigate(['/actualizar-password']);
      return false;
    }

    // Si NO debe cambiar password pero intenta entrar a la ruta de "actualización inicial"
    if (!debeCambiar && state.url === '/actualizar-password') {
      if (userRole === 'ADMIN') router.navigate(['/admin']);
      else if (userRole === 'ESPECIALISTA') router.navigate(['/especialista']);
      else router.navigate(['/usuario']);
      return false;
    }

    const expectedRoles = route.data['roles'] as Array<string>;
    if (expectedRoles && expectedRoles.length > 0) {
      if (!expectedRoles.includes(userRole || '')) {
        if (userRole === 'ADMIN') router.navigate(['/admin']);
        else if (userRole === 'ESPECIALISTA') router.navigate(['/especialista']);
        else router.navigate(['/usuario']);
        return false;
      }
    }
    return true;
  }

  // Si no está logueado, expulsarlo al login
  router.navigate(['/login']);
  return false;
};
