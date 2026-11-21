import { Component } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api } from '../../core/api';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  form: FormGroup;
  loading = false;
  mensagem = '';
  erro = false;
  constructor(private fb: FormBuilder, private api: Api) {
    this.form = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      senha: ['', [Validators.required, Validators.minLength(6)]]
    });
  }
  cadastrar() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading = true;
    this.api.cadastrarUsuario(this.form.getRawValue()).subscribe({
      next: () => {
        this.erro = false;
        this.mensagem = 'Conta criada com sucesso.';
        this.form.reset();
        this.loading = false;
      },
      error: resposta => {
        this.erro = true;
        this.mensagem = resposta.error?.erro || 'Não foi possível criar a conta.';
        this.loading = false;
      }
    });
  }
}
