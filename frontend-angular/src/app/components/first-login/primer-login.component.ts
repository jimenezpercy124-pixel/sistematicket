import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { AlertService } from '../../services/alert.service';

@Component({
  selector: 'app-primer-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './primer-login.component.html',
  styleUrls: ['./primer-login.component.css']
})
export class PrimerLoginComponent {
  formularioPassword: FormGroup;
  estaCargando: boolean = false;
  mostrarPassword1: boolean = false;
  mostrarPassword2: boolean = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private alertService: AlertService
  ) {
    this.formularioPassword = this.fb.group({
      nuevo_password: ['', [Validators.required, Validators.minLength(8)]],
      confirmar_password: ['', Validators.required]
    }, { validator: this.validadorCoincidencia });
  }

  validadorCoincidencia(g: FormGroup) {
    return g.get('nuevo_password')?.value === g.get('confirmar_password')?.value
      ? null : { 'desigual': true };
  }

  alternarVisibilidad(id: number) {
    if (id === 1) this.mostrarPassword1 = !this.mostrarPassword1;
    if (id === 2) this.mostrarPassword2 = !this.mostrarPassword2;
  }

  enviarFormulario() {
    if (this.formularioPassword.invalid) return;
    
    this.estaCargando = true;
    const { nuevo_password } = this.formularioPassword.value;

    this.authService.actualizarPasswordInicial(nuevo_password).subscribe({
      next: () => {
        this.alertService.success('¡Seguridad Actualizada!', 'Tu contraseña ha sido cambiada. Ahora puedes acceder a tu panel.').then(() => {
          const userInfo = JSON.parse(sessionStorage.getItem('user_info') || '{}');
          userInfo.debeCambiarPassword = false;
          sessionStorage.setItem('user_info', JSON.stringify(userInfo));

          const rol = this.authService.getUserRole();
          if (rol === 'ADMIN') this.router.navigate(['/admin']);
          else if (rol === 'ESPECIALISTA') this.router.navigate(['/especialista']);
          else this.router.navigate(['/usuario']);
        });
      },
      error: (err) => {
        this.estaCargando = false;
        const mensaje = typeof err.error === 'string' ? err.error : (err.error?.message || 'No se pudo actualizar la contraseña');
        this.alertService.error('Error', mensaje);
      }
    });
  }
}
