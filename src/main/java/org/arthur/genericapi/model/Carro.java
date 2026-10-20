package org.arthur.genericapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import org.arthur.genericapi.exception.RegraDeNegocioException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Veiculo da locadora. Uma {@link Carro} pode estar em varios alugueis ao
 * longo do tempo, mas em no maximo um por vez (garantido por {@link #alugar()}).
 *
 * <p><b>Placa como dado pessoal.</b> A placa nao identifica uma pessoa por si
 * so, mas identifica o condutor e o proprietario, e atravessa a fronteira do
 * veiculo sem difficulty. E tratada aqui como dado pessoal (LGPD, art. 5, I) e
 * recebe o mesmo tratamento do CPF: indice para unicidade e busca, cifra para
 * exibicao. O indice tambem substitui a placa no {@code group by} dos relatorios,
 * ja que ordenar ou agrupar por material cifrado nao produziria nada util.
 *
 * <p><b>Identificador.</b> UUID gerado na aplicacao, por causa da cardinalidade
 * e ordem cronologica que um inteiro sequencial revelaria.
 */
@Entity
@Table(
        name = "carros",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_carros_placa_indice", columnNames = "placa_indice")
)
@Getter
public class Carro {

    @Id
    private UUID id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "marca", nullable = false, length = 80)
    private String marca;

    @Column(name = "modelo", nullable = false, length = 80)
    private String modelo;

    @Column(name = "ano", nullable = false)
    private Integer ano;

    @Column(name = "placa_indice", nullable = false, length = 64)
    private String placaIndice;

    @Column(name = "placa_cifrada", nullable = false, length = 255)
    private String placaCifrada;

    @Column(name = "quilometragem", nullable = false)
    private Long quilometragem;

    @Column(name = "cor", length = 40)
    private String cor;

    @Column(name = "categoria", length = 40)
    private String categoria;

    @Column(name = "valor_diaria", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorDiaria;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusCarro status;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    protected Carro() {
    }

    public Carro(String nome, String marca, String modelo, Integer ano,
                 String placaIndice, String placaCifrada, Long quilometragem,
                 String cor, String categoria, BigDecimal valorDiaria) {
        this.id = UUID.randomUUID();
        this.nome = nome;
        this.marca = marca;
        this.modelo = modelo;
        this.ano = ano;
        this.placaIndice = placaIndice;
        this.placaCifrada = placaCifrada;
        this.quilometragem = quilometragem;
        this.cor = cor;
        this.categoria = categoria;
        this.valorDiaria = valorDiaria;
        this.status = StatusCarro.DISPONIVEL;
    }

    /** Atualiza os dados cadastrais. Placa e quilometragem sao papers de vehicle, nao editaveis aqui. */
    public void atualizarDados(String nome, String marca, String modelo, Integer ano,
                               String cor, String categoria, BigDecimal valorDiaria) {
        this.nome = nome;
        this.marca = marca;
        this.modelo = modelo;
        this.ano = ano;
        this.cor = cor;
        this.categoria = categoria;
        this.valorDiaria = valorDiaria;
    }

    public void alugar() {
        transicionarPara(StatusCarro.ALUGADO);
    }

    public void liberar() {
        transicionarPara(StatusCarro.DISPONIVEL);
    }

    public void enviarParaManutencao() {
        transicionarPara(StatusCarro.MANUTENCAO);
    }

    public void desativar() {
        transicionarPara(StatusCarro.DESATIVADO);
    }

public void alterarPlaca(String placaIndice, String placaCifrada) {
        this.placaIndice = placaIndice;
        this.placaCifrada = placaCifrada;
    }

    /** Quilometragem so cresce: um numero menor indica erro de digitacao. */
    public void registrarQuilometragem(Long quilometragem) {
        if (quilometragem == null || quilometragem < this.quilometragem) {
            throw new RegraDeNegocioException(
                    "Quilometragem do carro nao pode ser menor que a atual (" + this.quilometragem + ")");
        }
        this.quilometragem = quilometragem;
    }

    private void transicionarPara(StatusCarro destino) {
        if (!this.status.podeTransicionarPara(destino)) {
            throw new RegraDeNegocioException(
                    "Nao e possivel alterar o status do carro de " + this.status + " para " + destino);
        }
        this.status = destino;
    }

    @PrePersist
    void registrarCadastro() {
        this.dataCadastro = LocalDateTime.now();
    }

    @PreUpdate
    void registrarAtualizacao() {
        this.dataAtualizacao = LocalDateTime.now();
    }
}