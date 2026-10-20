# GLOSSARY

Vocabulário do projeto. Use estes termos exatamente como definidos aqui; não
troque por sinônimos. Cada entrada diz também o que o termo **não** é, porque a
confusão mora aí.

---

## Domínio

### Aluguel
O vínculo entre uma pessoa e um **Carro** por um período. É a entidade central
do sistema e o único lugar onde os dados pessoais da pessoa que pegou o veículo
existem (ver ADR-0003).

_Não é_: um contrato assinado, uma reserva futura, um orçamento. O aluguel é
criado quando o carro é retirado e termina em devolução ou cancelamento.

### Carro
Veículo da frota. Pode participar de vários aluguéis ao longo do tempo, mas de
no máximo um por vez — garantido por `Carro.alugar()`, não por constraint de
banco.

### Movimentação
Um aluguel que **toca** um período consultado. O critério é sobreposição, não
contenção: um aluguel que começou antes e terminou depois ainda aparece. Aluguéis
cancelados nunca são movimentação.

_Não é_: uma transação financeira, nem um evento de auditoria. É uma leitura
agregada sobre aluguéis existentes.

### Frota
O conjunto dos carros com seus status. A frota precisa ficar coerente com os
aluguéis: um carro `ALUGADO` tem exatamente um aluguel `ATIVO`.

---

## Proteção de dados pessoais

### Dado pessoal
Informação que identifica uma pessoa (nome, CPF, e-mail, telefone, CNH, placa).
No sistema, dado pessoal é sempre **guardado protegido**, nunca em claro —
requisito de lei, não preferência de design.

### Índice (blind index)
HMAC-SHA-256, com chave secreta, do valor **normalizado**. Determinístico: o
mesmo valor produz sempre o mesmo índice. Existe para permitir igualdade e busca
sem guardar o valor em claro.

_Não é_: hash comum (SHA-256 puro). O espaço de um CPF tem ~10⁹ combinações e cai
por força bruta em segundos; o índice é *keyed* justamente para que um vazamento
do banco não permita nem calcular o índice para comparar.

_Não é_: chave de unicidade. O CPF pode se repetir (a mesma pessoa pode ter
vários aluguéis). O índice serve à busca, nunca à unicidade — só a **placa**
tem constraint única sobre o próprio índice.

### Cifra
AES-256-GCM do valor, com IV aleatório prefixado ao resultado. Não
determinística: o mesmo texto claro cifra diferente a cada vez. Existe para
devolver o valor quando a API precisa mostrar.

_Não é_: ordenável, nem agregável. Cifra não suporta operação de faixa — por isso
datas, valores e quilometragem ficam **em claro**, e por isso colunas cifradas
nunca entram em allow-list de ordenação.

### Campo protegido
O par **índice + cifra** de um mesmo valor de origem, com a normalização correta
já aplicada. É o que o sistema guarda no lugar do dado pessoal.

O ganho do conceito: normalizar, indexar e cifrar deixam de ser três decisões
independentes que o chamador precisa acertar em sequência. Como um mesmo valor
tem normalizações diferentes conforme o **papel** (nome colapsa acento e caixa,
documento remove pontuação), parear o normalizador errado com o campo certo
compila e passa em teste — só quebra na busca, em produção.

### Papel do dado
Qual normalização um valor recebe, e que é o que decide seu **índice**:

| Papel | Normalização | Usado por |
|---|---|---|
| **Nome** | sem acento, minúsculo, espaços colapsados | nome da pessoa |
| **Documento** | só letras e dígitos, maiúsculo | CPF, CNH, placa |
| **Texto** | nenhuma | e-mail, telefone, observações, motivo |

_Não é_: o tipo Java do valor. CPF e e-mail são ambos `String`; o papel é o que
distingue o tratamento.

---

## Arquitetura

### Cofre
O módulo que detém as chaves e executa a criptografia (`ProtecaoDeDadoPessoal`).
Sabe cifrar, decifrar e gerar índice, e **só isso** — não conhece campos, papéis
nem o domínio.

_Não é_: um campo protegido. O cofre é a máquina; o campo protegido é o que o
domínio pede dela. Quem não precisa de chave (porque só repassa strings já
cifradas) não deve ver o cofre.
