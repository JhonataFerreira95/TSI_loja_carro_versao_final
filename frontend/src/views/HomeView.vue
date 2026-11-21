<script setup>
import { onMounted, ref } from 'vue'
import { api, lerErro } from '../api'

const usuario = ref(null)

onMounted(async () => {
  const resposta = await api('/usuario/logado')
  if (resposta.ok) usuario.value = await resposta.json()
})
</script>

<template>
  <section v-if="usuario" class="card">
    <h1>Loja de Carro</h1>
    <p>Olá, <b>{{ usuario.nome }}</b>. Selecione uma opção:</p>
    <div class="atalhos">
      <RouterLink class="atalho" to="/carros">{{ usuario.papel === 'ADMIN' ? 'Cadastro de carros' : 'Meus carros' }}</RouterLink>
      <RouterLink v-if="usuario.papel === 'ADMIN'" class="atalho" to="/usuarios">Gerenciar usuários</RouterLink>
      <RouterLink v-else class="atalho" to="/usuarios">Minha conta</RouterLink>
      <RouterLink class="atalho" to="/fotos">Minhas fotos</RouterLink>
      <RouterLink v-if="usuario.papel === 'ADMIN'" class="atalho" to="/logs">Logs do sistema</RouterLink>
    </div>
  </section>
</template>

<style scoped>
.atalhos {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 20px;
}
.atalho {
  border: 1px solid #dde5ed;
  border-radius: 10px;
  padding: 18px;
  color: #17633f;
  background: #f7faf9;
  text-decoration: none;
  font-weight: 800;
}
@media (max-width: 760px) {
  .atalhos { grid-template-columns: 1fr; }
}
</style>
