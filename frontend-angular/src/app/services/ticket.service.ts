import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TicketService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // --- TICKETS ---
  getAllTickets(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/todos-los-tickets`);
  }

  getTicketsByIRE(codIre: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/tickets/ire/${codIre}`);
  }

  getTicketsByEspecialista(num: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/tickets/especialista/${num}`);
  }

  getMisTickets(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/tickets/mis-tickets`);
  }

  createTicket(formData: FormData): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/tickets/crear`, formData);
  }

  updateTicketStatus(id: string | number, status: string, motivo?: string): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/tickets/${id}/status`, { estado: status, motivo });
  }

  delegateTicket(id: string | number, especialista_num: number): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/tickets/${id}/delegate`, { especialista_num });
  }

  getEspecialistasDisponibles(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/tickets/especialistas-disponibles`);
  }

  getEspecialistasByIRE(codIre: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/especialistas/ire/${codIre}`);
  }

  deleteTicket(id: string): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/admin/tickets/${id}`);
  }

  confirmTicketResolution(token: string, action: 'confirmar' | 'rechazar'): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/tickets/confirmar?token=${token}&accion=${action}`);
  }

  confirmTicketResolutionDirect(id: string): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/tickets/${id}/confirmar-directo`, {});
  }

  rejectTicketResolutionDirect(id: string, formData: FormData): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/tickets/${id}/rechazar-directo`, formData);
  }

  getTicketHistorial(id: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/tickets/${id}/historial`);
  }

  // --- CATALOGS ---
  getIncidencias(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/incidencias`);
  }

  getIntendencias(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/intendencias`);
  }

  createIncidencia(data: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/admin/incidencias`, data);
  }

  deleteIncidencia(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/admin/incidencias/${id}`);
  }

  // --- USERS ---
  getUsuarios(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/usuarios`);
  }

  createUsuario(userData: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/admin/crear-usuario`, userData);
  }

  deleteUsuario(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/admin/usuarios/${id}`);
  }

  resetPassword(id: number, password: any): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/admin/usuarios/${id}/reset-password`, { password });
  }

  toggleUserStatus(id: number, active: boolean): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/admin/usuarios/${id}/toggle-status`, { activo: active });
  }

  getEstadisticas(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/tickets/stats`);
  }

  getReportes(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/admin/reportes`);
  }

  getReporteIres(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/reportes/ires`);
  }

  getReporteTematicas(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/reportes/tematicas`);
  }

  reassignIncidenciaSpecialist(idIncidencia: number, idEspecialista: number): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/admin/incidencias/${idIncidencia}/reassign-specialist`, { id_especialista: idEspecialista });
  }

  activarContingencia(idAusente: number, idReemplazo: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/admin/contingencia/activar`, { id_ausente: idAusente, id_reemplazo: idReemplazo });
  }
}
