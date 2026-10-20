package org.arthur.genericapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import org.arthur.genericapi.exception.RegraDeNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Aluguel: o vinculo entre uma pessoa e um {@link Carro} por um periodo.
 *
 * <p>Guarda historico de locomocao e os dados da pessoa — quem pegou qual
 * veiculo, quando e por quantos kilometros — que e dado pessoal (LGPD, art. 5,
 * I). Por isso os dados pessoais (nome, cpf, email, telefone, cnh) ficam no
 * proprio aluguel, sempre protegidos: {@code indice} (HMAC, para busca) e/ou
 * {@code cifrado} (AES-256-GCM, para exibicao). Nao ha mais cadastro de cliente
 * nem pseudonimo em tabela separada (ver ADR-0003).
 *
 * <p>O CPF pode se repetir: a mesma pessoa pode ter varios alugueis. Por isso
 * nao existe constraint de unicidade em {@code cpf_indice}: ele serve a busca e
 * ao agrupamento, nunca a unicidade.
 *
 * <p>Datas, valores e quilometragem ficam em claro de proposito: os relatorios
 * filtram por faixa e somam, e cifra nao suporta nenhuma das duas operacoes.
 *
 * <p>O valor total e mantido na tabela para permitir agregacao em SQL nos
 * relatorios. Como a entidade nao tem setters publicos, ele so e recalculado
 * dentro dos proprios metodos de negocio, nunca fica desatualizado.
 */
@Entity
@Table(name = "alugueis")
@Getter
public class Aluguel {

    private static final int DIARIA_MINIMA = 1;

    @Id
    private UUID id;

    /**
     * HMAC do nome normalizado, para busca exata. O nome nao e unico, entao o
     * indice nunca e tratado como chave de identidade.
     */
    @Column(name = "nome_indice", nullable = false, length = 64)
    private String nomeIndice;

    @Column(name = "nome_cifrado", nullable = false, length = 512)
    private String nomeCifrado;

    /**
     * HMAC do CPF normalizado. Sem unicidade: a mesma pessoa pode ter varios
     * alugueis (ADR-0003).
     */
    @Column(name = "cpf_indice", nullable = false, length = 64)
    private String cpfIndice;

    @Column(name = "cpf_cifrado", nullable = false, length = 255)
    private String cpfCifrado;

    @Column(name = "email_cifrado", nullable = false, length = 512)
    private String emailCifrado;

    @Column(name = "telefone_cifrado", nullable = false, length = 255)
    private String telefoneCifrado;

    @Column(name = "cnh_indice", nullable = false, length = 64)
    private String cnhIndice;

    @Column(name = "cnh_cifrado", nullable = false, length = 255)
    private String cnhCifrado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carro_id", nullable = false)
    private Carro carro;

    @Column(name = "data_inicio", nullable = false)
    private LocalDateTime dataInicio;

    @Column(name = "data_prevista_devolucao", nullable = false)
    private LocalDateTime dataPrevistaDevolucao;

    @Column(name = "data_devolucao")
    private LocalDateTime dataDevolucao;

    @Column(name = "quilometragem_inicial", nullable = false)
    private Long quilometragemInicial;

    @Column(name = "quilometragem_final")
    private Long quilometragemFinal;

    @Column(name = "valor_diaria", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorDiaria;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusAluguel status;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento", nullable = false, length = 30)
    private StatusPagamento statusPagamento;

    /**
     * Texto livre, portanto o vetor mais silencioso de dado sensivel (LGPD,
     * art. 5, II): nada impede que alguem escreva condicao de saude, religiao
     * ou endereco. Cifrado, ele deixa de vazar em dump de banco � mas a
     * alimentacao do campo continua sendo questao de processo, nao de codigo.
     */
    @Column(name = "observacoes_cifradas", length = 4096)
    private String observacoesCifradas;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    protected Aluguel() {
    }

    /**
     * Todos os campos pessoais chegam ja protegidos pelo service; a entidade
     * nao possui chave para cifrar nem para gerar indice.
     *
     * @param observacoesCifradas observacoes ja protegidas pelo service.
     */
    public Aluguel(String nomeIndice, String nomeCifrado, String cpfIndice, String cpfCifrado,
                   String emailCifrado, String telefoneCifrado, String cnhIndice, String cnhCifrado,
                   Carro carro, LocalDateTime dataInicio,
                   LocalDateTime dataPrevistaDevolucao, Long quilometragemInicial,
                   BigDecimal valorDiaria, String observacoesCifradas) {
        if (dataPrevistaDevolucao.isBefore(dataInicio)) {
            throw new RegraDeNegocioException(
                    "A data prevista de devolucao nao pode ser anterior a data de inicio");
        }
        if (quilometragemInicial == null || quilometragemInicial < carro.getQuilometragem()) {
            throw new RegraDeNegocioException(
                    "A quilometragem inicial do aluguel nao pode ser menor que a quilometragem atual do carro ("
                            + carro.getQuilometragem() + ")");
        }
        this.id = UUID.randomUUID();
        this.nomeIndice = nomeIndice;
        this.nomeCifrado = nomeCifrado;
        this.cpfIndice = cpfIndice;
        this.cpfCifrado = cpfCifrado;
        this.emailCifrado = emailCifrado;
        this.telefoneCifrado = telefoneCifrado;
        this.cnhIndice = cnhIndice;
        this.cnhCifrado = cnhCifrado;
        this.carro = carro;
        this.dataInicio = dataInicio;
        this.dataPrevistaDevolucao = dataPrevistaDevolucao;
        this.quilometragemInicial = quilometragemInicial;
        this.valorDiaria = valorDiaria;
        this.observacoesCifradas = observacoesCifradas;
        this.status = StatusAluguel.ATIVO;
        this.statusPagamento = StatusPagamento.PENDENTE;
        recalcularValorTotal();
    }

    /** Encerra o aluguel com devolucao em atraso ou no prazo. */
    public void registrarDevolucao(LocalDateTime dataDevolucao, Long quilometragemFinal) {
        exigirStatus(StatusAluguel.ATIVO, "registrar devolucao");
        if (dataDevolucao == null || dataDevolucao.isBefore(dataInicio)) {
            throw new RegraDeNegocioException("A data de devolucao nao pode ser anterior ao inicio do aluguel");
        }
        if (quilometragemFinal == null || quilometragemFinal < quilometragemInicial) {
            throw new RegraDeNegocioException(
                    "A quilometragem final nao pode ser menor que a inicial do aluguel");
        }
        this.dataDevolucao = dataDevolucao;
        this.quilometragemFinal = quilometragemFinal;
        this.status = StatusAluguel.CONCLUIDO;
        recalcularValorTotal();
    }

/** @param motivoCifrada motivo ja protegido pelo service. */
    public void cancelar(String motivoCifrada) {
        exigirStatus(StatusAluguel.ATIVO, "cancelar");
        this.observacoesCifradas = motivoCifrada == null ? this.observacoesCifradas : motivoCifrada;
        this.status = StatusAluguel.CANCELADO;
        transicionarPagamentoPara(StatusPagamento.CANCELADO);
        recalcularValorTotal();
    }

    public void registrarPagamento() {
        transicionarPagamentoPara(StatusPagamento.PAGO);
    }

    public boolean estaAtrasado() {
        return status == StatusAluguel.ATIVO
                && LocalDateTime.now().isAfter(dataPrevistaDevolucao);
    }

    /** Diarias cobradas: pelas previstas enquanto ativo, pelas reais quando concluido. */
    public int getTotalDiarias() {
        LocalDate fim = dataDevolucao != null
                ? dataDevolucao.toLocalDate()
                : dataPrevistaDevolucao.toLocalDate();
        long dias = ChronoUnit.DAYS.between(dataInicio.toLocalDate(), fim);
        return (int) Math.max(DIARIA_MINIMA, dias);
    }

    private void recalcularValorTotal() {
        if (status == StatusAluguel.CANCELADO) {
            this.valorTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return;
        }
        this.valorTotal = valorDiaria.multiply(BigDecimal.valueOf(getTotalDiarias()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Quilometragem percorrida no aluguel; 0 enquanto nao houve devolucao. */
    public long getQuilometragemPercorrida() {
        return quilometragemFinal == null ? 0L : quilometragemFinal - quilometragemInicial;
    }

    private void exigirStatus(StatusAluguel esperado, String acao) {
        if (status != esperado) {
            throw new RegraDeNegocioException(
                    "Nao e possivel " + acao + " em um aluguel com status " + status);
        }
    }

    private void transicionarPagamentoPara(StatusPagamento destino) {
        if (!statusPagamento.podeTransicionarPara(destino)) {
            throw new RegraDeNegocioException(
                    "Nao e possivel alterar o status de pagamento de " + statusPagamento + " para " + destino);
        }
        this.statusPagamento = destino;
    }

    @PrePersist
    void registrarCriacao() {
        this.dataCriacao = LocalDateTime.now();
    }

    @PreUpdate
    void registrarAtualizacao() {
        this.dataAtualizacao = LocalDateTime.now();
    }
}