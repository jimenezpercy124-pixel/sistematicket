import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = environment.apiUrl + '/auth';

  // SocketService se inyecta con inject() para evitar dependencia circular
  private socketServiceRef?: { stop: () => void; restart: () => void };

  constructor(private http: HttpClient) {}

  /** Registra referencia al SocketService (llamar desde el propio SocketService) */
  registerSocketService(ref: { stop: () => void; restart: () => void }) {
    this.socketServiceRef = ref;
  }

  login(credentials: { username: string; password: string }): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        if (response && response.token) {
          sessionStorage.setItem('token', response.token);
          sessionStorage.setItem('user_info', JSON.stringify({
            id: response.id,
            username: response.username,
            rol: response.rol,
            codIre: response.cod_ire || response.codIre,
            especialistaNum: response.especialista_num || response.especialistaNum,
            debeCambiarPassword: response.debe_cambiar_password !== undefined ? response.debe_cambiar_password : response.debeCambiarPassword
          }));
        }
      })
    );
  }

  actualizarPasswordInicial(nuevaPassword: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/actualizar-password`, { 
      nuevaPassword: nuevaPassword,
      nueva_password: nuevaPassword 
    });
  }

  actualizarPasswordGeneral(passwordActual: string, nuevaPassword: string): Observable<any> {
    const id = JSON.parse(sessionStorage.getItem('user_info') || '{}').id;
    return this.http.patch<any>(`${this.apiUrl}/actualizar-password`, { id, current_password: passwordActual, new_password: nuevaPassword });
  }

  logout() {
    // Detener SSE antes de limpiar sesión
    if (this.socketServiceRef) {
      this.socketServiceRef.stop();
    }
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user_info');
  }

  getToken(): string | null {
    return sessionStorage.getItem('token');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  getUserRole(): string | null {
    const userInfoStr = sessionStorage.getItem('user_info');
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        return userInfo.rol;
      } catch (e) {
        return null;
      }
    }
    return null;
  }
}
