package org.arthur.genericapi.repository;

import org.arthur.genericapi.model.Carro;
import org.arthur.genericapi.model.StatusCarro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarroRepository extends JpaRepository<Carro, UUID> {

    Optional<Carro> findByPlacaIndice(String placaIndice);

    boolean existsByPlacaIndice(String placaIndice);

    List<Carro> findByStatusOrderByIdAsc(StatusCarro status);

    Page<Carro> findByMarcaContainingIgnoreCase(String marca, Pageable pageable);

    Page<Carro> findByStatusOrderByIdAsc(StatusCarro status, Pageable pageable);

    /**
     * Trava pessimista a linha do carro. Usada na criacao do aluguel para que
     * duas requisicoes simultaneas nao aluguem o mesmo veiculo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Carro c where c.id = :id")
    Optional<Carro> findByIdParaAtualizacao(@Param("id") UUID id);
}