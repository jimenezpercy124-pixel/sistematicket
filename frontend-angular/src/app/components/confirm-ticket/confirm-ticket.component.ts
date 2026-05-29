import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-confirm-ticket',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './confirm-ticket.component.html',
  styleUrls: ['./confirm-ticket.component.css']
})
export class ConfirmTicketComponent implements OnInit {
  status: 'loading' | 'success' | 'error' | 'info' | 'reject_form' = 'loading';
  message: string = '';
  idTicket: string = '';
  token: string | null = null;
  accion: string | null = null;
  
  motivoRechazo: string = '';
  evidenciaFile: File | null = null;

  private apiUrl = environment.apiUrl + '/tickets';

  constructor(private route: ActivatedRoute, private http: HttpClient) {}

  ngOnInit() {
    this.route.queryParamMap.subscribe(params => {
      this.token = params.get('token');
      this.accion = params.get('accion');
      this.procesar();
    });
  }

  procesar() {
    if (!this.token || !this.accion) {
      this.status = 'error';
      this.message = 'Enlace inválido o incompleto.';
      return;
    }

    if (this.accion === 'rechazar') {
      this.status = 'reject_form';
      return;
    }

    this.confirmar();
  }

  confirmar() {
    this.http.get<any>(`${this.apiUrl}/confirmar?token=${this.token}&accion=confirmar`).subscribe({
      next: (res) => {
        if (res.success) {
          this.status = 'success';
          this.idTicket = res.id_ticket;
          this.message = `El ticket ${res.id_ticket} ha sido cerrado exitosamente.`;
        } else {
          this.status = 'info';
          this.message = res.message || 'La solicitud ya fue procesada.';
        }
      },
      error: () => {
        this.status = 'error';
        this.message = 'Error de conexión con el servidor.';
      }
    });
  }

  enviarRechazo() {
    if (!this.motivoRechazo) return;

    this.status = 'loading';
    const formData = new FormData();
    formData.append('token', this.token!);
    formData.append('motivo', this.motivoRechazo);
    if (this.evidenciaFile) {
      formData.append('evidencia', this.evidenciaFile);
    }

    this.http.post<any>(`${this.apiUrl}/rechazar`, formData).subscribe({
      next: (res) => {
        if (res.success) {
          this.status = 'info';
          this.message = `El ticket ${res.id_ticket} ha sido devuelto al especialista.`;
        } else {
          this.status = 'error';
          this.message = res.message || 'Error al procesar el rechazo.';
        }
      },
      error: () => {
        this.status = 'error';
        this.message = 'Error de conexión al enviar el rechazo.';
      }
    });
  }

  onFileChange(event: any) {
    if (event.target.files.length > 0) {
      this.evidenciaFile = event.target.files[0];
    }
  }
}
