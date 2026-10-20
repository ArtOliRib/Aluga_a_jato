package org.arthur.genericapi.repository;

import org.arthur.genericapi.model.Aluguel;
import org.arthur.genericapi.model.StatusAluguel;
import org.arthur.genericapi.repository.projection.MovimentacaoPorCarroProjection;
import org.arthur.genericapi.repository.projection.ResumoMovimentacaoProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Consultas de aluguel. A entidade guarda os dados da pessoa (nome/cpf/...)
 * cifrados no proprio registro; as consultas de relatorio usam apenas colunas
 * em claro (datas, valores, quilometragem) ou o carro.
 */
public interface AluguelRepository extends JpaRepository<Aluguel, UUID> {

    boolean existsByCarroIdAndStatus(UUID carroId, StatusAluguel status);

    boolean existsByCarroId(UUID carroId);

    Page<Aluguel> findByCarroIdOrderByDataInicioDesc(UUID carroId, Pageable pageable);

    Page<Aluguel> findByStatusOrderByDataInicioDesc(StatusAluguel status, Pageable pageable);

    @Query("""
            select count(a) as totalAlugueis,
                   coalesce(sum(case when a.status = org.arthur.genericapi.model.StatusAluguel.CONCLUIDO
                                     then 1 else 0 end), 0) as totalConcluidos,
                   coalesce(sum(case when a.status = org.arthur.genericapi.model.StatusAluguel.ATIVO
                                     then 1 else 0 end), 0) as totalAtivos,
                   coalesce(sum(a.valorTotal), 0) as receitaTotal,
                   coalesce(sum(case when a.statusPagamento = org.arthur.genericapi.model.StatusPagamento.PAGO
                                     then a.valorTotal else 0 end), 0) as receitaPaga,
                   coalesce(sum(case when a.statusPagamento = org.arthur.genericapi.model.StatusPagamento.PENDENTE
                                     then a.valorTotal else 0 end), 0) as receitaPendente,
                   coalesce(sum(case when a.quilometragemFinal is not null
                                     then (a.quilometragemFinal - a.quilometragemInicial)
                                     else 0 end), 0) as quilometragemPercorrida
              from Aluguel a
             where a.status <> org.arthur.genericapi.model.StatusAluguel.CANCELADO
               and a.dataInicio <= :ate
               and (a.dataDevolucao is null or a.dataDevolucao >= :de)
               and (:carroId is null or a.carro.id = :carroId)
            """)
    ResumoMovimentacaoProjection resumirMovimentacao(@Param("de") LocalDateTime de,
                                                    @Param("ate") LocalDateTime ate,
                                                    @Param("carroId") UUID carroId);

    /**
     * Agrupa e ordena pela {@code placa_cifrada} em vez da placa: agrupar por
     * material cifrado nao muda o resultado (a cifra e funcionalmente dependente
     * do id) e evita que o claro entre na consulta.
     */
    @Query("""
            select c.id as carroId,
                   c.placaCifrada as placaCifrada,
                   c.marca as marca,
                   c.modelo as modelo,
                   count(a) as totalAlugueis,
                   coalesce(sum(a.valorTotal), 0) as receitaTotal,
                   coalesce(sum(case when a.statusPagamento = org.arthur.genericapi.model.StatusPagamento.PAGO
                                     then a.valorTotal else 0 end), 0) as receitaPaga,
                   coalesce(sum(case when a.quilometragemFinal is not null
                                     then (a.quilometragemFinal - a.quilometragemInicial)
                                     else 0 end), 0) as quilometragemPercorrida
              from Aluguel a
              join a.carro c
             where a.status <> org.arthur.genericapi.model.StatusAluguel.CANCELADO
               and a.dataInicio <= :ate
               and (a.dataDevolucao is null or a.dataDevolucao >= :de)
             group by c.id, c.placaCifrada, c.marca, c.modelo
             order by sum(a.valorTotal) desc
            """)
    List<MovimentacaoPorCarroProjection> listarMovimentacaoPorCarro(@Param("de") LocalDateTime de,
                                                                    @Param("ate") LocalDateTime ate);
}