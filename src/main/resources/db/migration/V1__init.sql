CREATE TABLE carros (
    id UUID PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    marca VARCHAR(80) NOT NULL,
    modelo VARCHAR(80) NOT NULL,
    ano INTEGER NOT NULL,
    placa_indice VARCHAR(64) NOT NULL,
    placa_cifrada VARCHAR(255) NOT NULL,
    quilometragem BIGINT NOT NULL,
    cor VARCHAR(40),
    categoria VARCHAR(40),
    valor_diaria NUMERIC(10,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    data_cadastro TIMESTAMP NOT NULL,
    data_atualizacao TIMESTAMP
);

ALTER TABLE carros ADD CONSTRAINT uk_carros_placa_indice UNIQUE (placa_indice);

CREATE INDEX idx_carros_status ON carros(status);
CREATE INDEX idx_carros_placa_indice ON carros(placa_indice);

CREATE TABLE alugueis (
    id UUID PRIMARY KEY,
    nome_indice VARCHAR(64) NOT NULL,
    nome_cifrado VARCHAR(512) NOT NULL,
    cpf_indice VARCHAR(64) NOT NULL,
    cpf_cifrado VARCHAR(255) NOT NULL,
    email_cifrado VARCHAR(512) NOT NULL,
    telefone_cifrado VARCHAR(255) NOT NULL,
    cnh_indice VARCHAR(64) NOT NULL,
    cnh_cifrado VARCHAR(255) NOT NULL,
    carro_id UUID NOT NULL,
    data_inicio TIMESTAMP NOT NULL,
    data_prevista_devolucao TIMESTAMP NOT NULL,
    data_devolucao TIMESTAMP,
    quilometragem_inicial BIGINT NOT NULL,
    quilometragem_final BIGINT,
    valor_diaria NUMERIC(10,2) NOT NULL,
    valor_total NUMERIC(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    status_pagamento VARCHAR(30) NOT NULL,
    observacoes_cifradas VARCHAR(4096),
    data_criacao TIMESTAMP NOT NULL,
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_alugueis_carro FOREIGN KEY (carro_id) REFERENCES carros(id)
);

CREATE INDEX idx_alugueis_carro_id ON alugueis(carro_id);
CREATE INDEX idx_alugueis_cpf_indice ON alugueis(cpf_indice);
CREATE INDEX idx_alugueis_nome_indice ON alugueis(nome_indice);
CREATE INDEX idx_alugueis_cnh_indice ON alugueis(cnh_indice);
CREATE INDEX idx_alugueis_status ON alugueis(status);
CREATE INDEX idx_alugueis_status_pagamento ON alugueis(status_pagamento);
CREATE INDEX idx_alugueis_data_inicio ON alugueis(data_inicio);
CREATE INDEX idx_alugueis_data_prevista_devolucao ON alugueis(data_prevista_devolucao);
CREATE INDEX idx_alugueis_data_devolucao ON alugueis(data_devolucao);
