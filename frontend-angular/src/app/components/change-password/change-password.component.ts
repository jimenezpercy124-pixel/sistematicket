import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { AlertService } from '../../services/alert.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.css']
})
export class ChangePasswordComponent {
  formularioPassword: FormGroup;
  mensajeError: string = '';
  estaCargando: boolean = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private alertService: AlertService
  ) {
    this.formularioPassword = this.fb.group({
      password_actual: ['', Validators.required],
      nueva_password: ['', [Validators.required, Validators.minLength(8)]],
      confirmar_password: ['', Validators.required]
    }, { validator: this.validadorCoincidencia });
  }

  validadorCoincidencia(g: FormGroup) {
    return g.get('nueva_password')?.value === g.get('confirmar_password')?.value
      ? null : { 'desigual': true };
  }

  enviarFormulario() {
    if (this.formularioPassword.invalid) return;
    
    this.estaCargando = true;
    const { password_actual, nueva_password } = this.formularioPassword.value;

    this.authService.actualizarPasswordGeneral(password_actual, nueva_password).subscribe({
      next: () => {
        this.alertService.success('¡Hecho!', 'Contraseña actualizada correctamente. Por seguridad, inicie sesión nuevamente.').then(() => {
          this.authService.logout();
          this.router.navigate(['/login']);
        });
      },
      error: (err: any) => {
        this.estaCargando = false;
        this.mensajeError = err.error?.message || 'Error al actualizar contraseña';
      }
    });
  }
}
