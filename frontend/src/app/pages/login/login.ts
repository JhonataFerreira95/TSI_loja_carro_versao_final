import { Component } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Api } from '../../core/api';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  loading = false;
  mensagem = '';
  erro = false;
  form: FormGroup;
  constructor(private fb: FormBuilder, private api: Api, private router: Router) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      senha: ['', Validators.required]
    });
  }
  entrar() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading = true;
    this.api.login(this.form.value.email!, this.form.value.senha!).subscribe({
      next: () => this.router.navigate(['/']),
      error: resposta => { this.erro = true; this.mensagem = resposta.error?.erro || 'Falha no login.'; this.loading = false; },
      complete: () => this.loading = false
    });
  }
}
