import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { Api, Carro } from '../../core/api';

@Component({
  selector: 'app-test-drive',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './test-drive.html',
  styleUrl: './test-drive.scss',
})
export class TestDrive implements OnInit {
  carro?: Carro;
  form: FormGroup;
  loading = false;
  mensagem = '';
  erro = false;
  constructor(private route: ActivatedRoute, private fb: FormBuilder, private api: Api) {
    this.form = this.fb.group({
      dataHora: ['', Validators.required],
      observacao: ['', Validators.maxLength(500)]
    });
  }
  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.buscarCarro(id).subscribe({
      next: carro => this.carro = carro,
      error: () => { this.erro = true; this.mensagem = 'Veículo não encontrado.'; }
    });
  }
  agendar() {
    if (!this.carro || this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading = true;
    this.api.criarAgendamento({
      carroId: this.carro.id,
      dataHora: this.form.value.dataHora,
      observacao: this.form.value.observacao
    }).subscribe({
      next: () => {
        this.erro = false;
        this.mensagem = 'Test drive agendado com sucesso.';
        this.form.reset();
        this.loading = false;
      },
      error: resposta => {
        this.erro = true;
        this.mensagem = resposta.error?.erro || 'Não foi possível agendar.';
        this.loading = false;
      }
    });
  }
}
