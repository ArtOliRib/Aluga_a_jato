# ADR-0004: Campo protegido como ponto unico de normalizacao + indice + cifra

## Status
Aceito

## Contexto

A protecao de dado pessoal (LGPD) exige que nome, CPF, e-mail, telefone, CNH e
placa sejam guardados como indice HMAC (para busca) mais cifra AES-GCM (para
exibicao). Antes desta decisao, esses tres passos eram tres chamadas
independentes que cada service precisava acertar em sequencia:

```java
protecao.gerarIndice(Normalizador.nome(request.nome())),
protecao.cifrar(request.nome()),
```

Isso rendia 21 chamadas `protecao.*` e 6 `Normalizador.*` espalhadas por
`AluguelService`, `CarroService` e `RelatorioService`.

O risco nao era o volume, era a **falha silenciosa**. `ProtecaoDeDadoPessoal.gerarIndice`
recebe o valor **ja normalizado** — quem normaliza e o chamador. Entao
`gerarIndice(request.nome())` e `cifrar(request.nome())` compilam, passam em
qualquer teste de comportamento, gravam um par aparentemente valido... e a busca
por nome deixa de encontrar o registro, porque o indice foi calculado sobre um
valor que nunca mais sera reproduzido. Um erro de pareamento entre normalizador e
campo nao tem sintoma na gravacao; so aparece na consulta, ja em producao.

O `Normalizador` ja prometia no proprio Javadoc "um unico ponto de verdade evita
que placa/cpf/cnh/nome sejam comparados de formas diferentes". Essa promessa nao
era cumprida: o ponto de verdade existia como funcao, mas nao como **obrigacao**.

## Decisao

1. **Introduzir `CampoProtegido`**: o par indice + cifra de um valor de origem,
   com a normalizacao do papel ja aplicada. Metodos de fabrica estaticos
   (`.deNome`, `.deDocumento`, `.deTexto`) recebem o valor, o **cofre** e — para
   leitura — o valor cifrado.
2. **O papel do dado entra no vocabulario do modulo**, nao como escolha do
   chamador. `CampoProtegido` e o unico ponto que chama `Normalizador` no caminho
   de gravacao e de busca.
3. **Fabrica de indice puro** para busca que nao grava: normalizar e indexar
   numa chamada so (`CarroService.buscarPorPlaca` fazia
   `gerarIndice(Normalizador.documento(placa))` a mao).
4. **Nulo e tolerado na leitura e recusado na gravacao.** Nome, CPF, e-mail,
   telefone e CNH sao `nullable = false`; deixar `CampoProtegido` engolir nulo
   transformaria violacao de constraint em `null` gravado e erro tardio com
   mensagem ruim.
5. **O cofre permanece injetado nos services.** `CampoProtegido` e estatico e
   puro; recebe o cofre por parametro. Os services nao perdem acesso a nada.
6. **O escopo e gravacao e indice de busca, nao leitura.** `RelatorioService` e
   os `responder(...)` continuam chamando `protecao.decifrar(...)` direto: ali
   nao ha normalizacao nem indice, entao o unico erro possivel e escolher o campo
   errado, e um wrapper nao impede isso. Cobrir a leitura seria ganho de fachada.

## Consequencias

- Impossivel parear normalizador errado no caminho de gravacao/busca: o papel
  escolhe a normalizacao dentro do modulo.
- `AluguelService.registrar` e `CarroService.cadastrar/alterarPlaca` encolhem e
  deixam de intercalar 8 construtores `String` de dados pessoais.
- `Normalizador` continua existindo, mas deixa de ser chamado pelos services no
  caminho de gravacao; vira dependencia interna de `CampoProtegido`.
- Custo: uma classe a mais em `support/` e uma indirecao na leitura do service.
  Aceito porque o ganho e a eliminacao de uma classe de bug silencioso, nao
  apenas menos linhas.

## Relacao com ADR-0001

ADR-0001 prioriza simplicidade didatica e exige justificar cada abstracao. Esta
decisao nao a contradiz, mas **encosta** nela, por isso fica registrada:

- `CampoProtegido` nao e um mapper generico nem heranca; e uma classe `final` com
  construtor privado e fabricas estaticas, o mesmo formato de `Normalizador` e
  `Ordenacao` que o projeto ja usa.
- Nenhum service perde acesso a nada — `protecao` continua injetado. A mudanca
  adiciona uma forma mais curta e a prova de erro, nao uma camada obrigatoria.
- O criterio de aceite foi explicito: o service tem que ficar **menor** e parar
  de importar `ProtecaoDeDadoPessoal` onde so criava campos. Se algum service
  tivesse ficado maior, o ADR-0001 teria vencido.

## Alternativas consideradas

- **Value object que guarda so o par armazenado (indice + cifra), sem o valor em
  claro.** Rejeitado: alguem precisaria fabricar o par, e esse alguem seria o
  modulo de verdade, deixando o value object como casca.
- **Enum `TipoDeDadoPessoal` com `CampoProtegido.de(valor, tipo)`.** Rejeitado:
  abstracao a mais para tres casos, contra ADR-0001. Os papeis viraram metodos.
- **Um `CampoProtegido` instanciado e injetado, cofre escondido do service.**
  Rejeitado pelo usuario: ele quer o cofre visivel no service, e o desenho
  estatico entrega o mesmo ganho de seguranca sem esconder a dependencia.
- **Entidade `Aluguel`/`Carro` recebendo `CampoProtegido` em vez de `String`.**
  Rejeitado: faria `model` depender de `support` e nao impediria confundir indice
  com cifra, ja que ambos continuariam `String` por dentro.
- **Estender `CampoProtegido` a leitura em `RelatorioService`.** Rejeitado por
  ser ganho de fachada: nao ha normalizacao nem indice ali.
