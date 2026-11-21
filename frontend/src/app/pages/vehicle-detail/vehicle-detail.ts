import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { Api, Carro } from '../../core/api';

@Component({
  selector: 'app-vehicle-detail',
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './vehicle-detail.html',
  styleUrl: './vehicle-detail.scss',
})
export class VehicleDetail implements OnInit {
  carro?: Carro;
  loading = true;
  erro = '';
  constructor(private route: ActivatedRoute, private api: Api) {}
  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.buscarCarro(id).subscribe({
      next: carro => { this.carro = carro; this.loading = false; },
      error: () => { this.erro = 'Veículo não encontrado.'; this.loading = false; }
    });
  }
}
