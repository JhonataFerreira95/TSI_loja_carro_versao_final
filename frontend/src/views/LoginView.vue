<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { lerErro } from '../api'

const router = useRouter()
const login = ref({ email: '', senha: '' })
const cadastro = ref({ nome: '', email: '', senha: '' })
const mensagem = ref('')
const erro = ref(false)

async function enviar(url, dados) {
  return fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dados),
  })
}

async function entrar() {
  erro.value = false
  const resposta = await enviar('/usuario/login', login.value)
  if (resposta.ok) {
    router.replace('/')
  } else {
    mensagem.value = await lerErro(resposta)
    erro.value = true
  }
}

async function criarConta() {
  erro.value = false
  const resposta = await enviar('/usuario/salvar', cadastro.value)
  if (resposta.ok) {
    login.value.email = cadastro.value.email
    cadastro.value = { nome: '', email: '', senha: '' }
    mensagem.value = 'Conta criada. Agora faça o login.'
  } else {
    mensagem.value = await lerErro(resposta)
    erro.value = true
  }
}
</script>

<template>
  <section class="login-grid">
    <div class="card">
      <h1>Loja de Carro</h1>
      <p v-if="mensagem" :class="['mensagem', erro ? 'erro' : 'sucesso']">{{ mensagem }}</p>
      <h2>Entrar</h2>
      <form @submit.prevent="entrar">
        <label>E-mail<input v-model="login.email" type="email" required /></label>
        <label>Senha<input v-model="login.senha" type="password" required /></label>
        <button type="submit">Entrar</button>
      </form>
    </div>

    <div class="card">
      <h2>Criar conta</h2>
      <form @submit.prevent="criarConta">
        <label>Nome<input v-model="cadastro.nome" required /></label>
        <label>E-mail<input v-model="cadastro.email" type="email" required /></label>
        <label>Senha (mínimo 6 caracteres)<input v-model="cadastro.senha" type="password" minlength="6" required /></label>
        <button type="submit">Criar conta</button>
      </form>
    </div>
  </section>
</template>

<style scoped>
.login-grid {
  max-width: 980px;
  margin: 48px auto;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

@media (max-width: 760px) {
  .login-grid {
    grid-template-columns: 1fr;
  }
}
</style>
