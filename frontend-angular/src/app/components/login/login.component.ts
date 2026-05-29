import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { AlertService } from '../../services/alert.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  formularioLogin: FormGroup;
  mensajeError: string = '';
  estaCargando: boolean = false;
  mostrarPassword: boolean = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private alertService: AlertService
  ) {
    this.formularioLogin = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  alternarMostrarPassword() {
    this.mostrarPassword = !this.mostrarPassword;
  }

  enviarFormulario() {
    if (this.formularioLogin.invalid) return;

    this.estaCargando = true;
    this.mensajeError = '';

    this.authService.login(this.formularioLogin.value).subscribe({
      next: (response) => {
        const userInfo = JSON.parse(sessionStorage.getItem('user_info') || '{}');
        const rol = userInfo.rol;

        if (userInfo.debeCambiarPassword && rol !== 'ADMIN') {
          this.router.navigate(['/actualizar-password']);
          return;
        }
        
        if (rol === 'ADMIN') {
          this.router.navigate(['/admin']);
        } else if (rol === 'ESPECIALISTA') {
          this.router.navigate(['/especialista']);
        } else {
          this.router.navigate(['/usuario']);
        }
      },
      error: (err) => {
        this.estaCargando = false;
        
        const errorMsg = (typeof err.error === 'string') ? err.error : (err.error?.message || '');
        if (err.status === 403 || errorMsg.toLowerCase().includes('inactivo') || errorMsg.toLowerCase().includes('inhabilitado')) {
          this.alertService.error('Usuario Inactivo', 'Su cuenta se encuentra inhabilitada o inactiva en el sistema. Por favor, póngase en contacto con el administrador.');
          this.mensajeError = 'Usuario inactivo o inhabilitado';
        } else if (err.status === 401 || errorMsg.includes('incorrectas')) {
          this.mensajeError = 'Usuario o contraseña incorrectos';
        } else {
          this.mensajeError = errorMsg || 'Error al iniciar sesión';
        }
      }
    });
  }
}
