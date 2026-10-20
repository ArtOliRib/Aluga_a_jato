package org.arthur.genericapi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.arthur.genericapi.dto.PageResponse;
import org.arthur.genericapi.dto.carro.CarroRequest;
import org.arthur.genericapi.dto.carro.CarroResponse;
import org.arthur.genericapi.dto.carro.CarroStatusRequest;
import org.arthur.genericapi.dto.carro.CarroUpdateRequest;
import org.arthur.genericapi.dto.carro.PlacaRequest;
import org.arthur.genericapi.dto.carro.QuilometragemRequest;
import org.arthur.genericapi.model.StatusCarro;
import org.arthur.genericapi.service.CarroService;
import org.arthur.genericapi.support.Ordenacao;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/carros")
@RequiredArgsConstructor
public class CarroController {

    private final CarroService carroService;

    @PostMapping
    public ResponseEntity<CarroResponse> cadastrar(@Valid @RequestBody CarroRequest request) {
        CarroResponse criado = carroService.cadastrar(request);
        URI local = URI.create("/api/v1/carros/" + criado.id());
        return ResponseEntity.created(local).body(criado);
    }

    @GetMapping("/{id}")
    public CarroResponse buscarPorId(@PathVariable UUID id) {
        return carroService.buscarPorId(id);
    }

    @GetMapping("/por-placa/{placa}")
    public CarroResponse buscarPorPlaca(@PathVariable String placa) {
        return carroService.buscarPorPlaca(placa);
    }

    @GetMapping
    public PageResponse<CarroResponse> listar(
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) StatusCarro status,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho,
            @RequestParam(defaultValue = "id") String ordenarPor,
            @RequestParam(defaultValue = "asc") String direcao) {
        // placa fica fora da lista por ser cifrada.
        return carroService.listar(marca, status, Ordenacao.page(
                pagina, tamanho, ordenarPor, direcao,
                "id", "ano", "quilometragem", "valorDiaria", "dataCadastro"));
    }

    @GetMapping("/disponiveis")
    public PageResponse<CarroResponse> listarDisponiveis(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return carroService.listarDisponiveis(Ordenacao.page(pagina, tamanho));
    }

    @PutMapping("/{id}")
    public CarroResponse atualizar(@PathVariable UUID id, @Valid @RequestBody CarroUpdateRequest request) {
        return carroService.atualizar(id, request);
    }

    /** Troca parcial de status. Recurso proprio em vez de PUT, porque a regra depende do status atual. */
    @PatchMapping("/{id}/status")
    public CarroResponse alterarStatus(@PathVariable UUID id, @Valid @RequestBody CarroStatusRequest request) {
        return carroService.alterarStatus(id, request.status());
    }

    @PutMapping("/{id}/placa")
    public CarroResponse alterarPlaca(@PathVariable UUID id, @Valid @RequestBody PlacaRequest request) {
        return carroService.alterarPlaca(id, request.placa());
    }

    @PutMapping("/{id}/quilometragem")
    public CarroResponse registrarQuilometragem(@PathVariable UUID id,
                                                @Valid @RequestBody QuilometragemRequest request) {
        return carroService.registrarQuilometragem(id, request.quilometragem());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable UUID id) {
        carroService.remover(id);
    }
}