package org.arthur.genericapi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.arthur.genericapi.dto.PageResponse;
import org.arthur.genericapi.dto.aluguel.AluguelRequest;
import org.arthur.genericapi.dto.aluguel.AluguelResponse;
import org.arthur.genericapi.dto.aluguel.CancelamentoRequest;
import org.arthur.genericapi.dto.aluguel.DevolucaoRequest;
import org.arthur.genericapi.model.StatusAluguel;
import org.arthur.genericapi.service.AluguelService;
import org.arthur.genericapi.support.Ordenacao;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alugueis")
@RequiredArgsConstructor
public class AluguelController {

    private final AluguelService aluguelService;

    @PostMapping
    public ResponseEntity<AluguelResponse> registrar(@Valid @RequestBody AluguelRequest request) {
        AluguelResponse criado = aluguelService.registrar(request);
        URI local = URI.create("/api/v1/alugueis/" + criado.id());
        return ResponseEntity.created(local).body(criado);
    }

    @GetMapping("/{id}")
    public AluguelResponse buscarPorId(@PathVariable UUID id) {
        return aluguelService.buscarPorId(id);
    }

    @GetMapping
    public PageResponse<AluguelResponse> listar(
            @RequestParam(required = false) UUID carroId,
            @RequestParam(required = false) StatusAluguel status,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho,
            @RequestParam(defaultValue = "dataInicio") String ordenarPor,
            @RequestParam(defaultValue = "desc") String direcao) {
        // observacoes fica fora da lista por ser texto livre cifrado.
        return aluguelService.listar(carroId, status, Ordenacao.page(
                pagina, tamanho, ordenarPor, direcao,
                "id", "dataInicio", "dataPrevistaDevolucao", "dataDevolucao", "valorTotal"));
    }

    /** Devolucao do veiculo: encerra o aluguel, baixa a quilometragem e libera o carro. */
    @PatchMapping("/{id}/devolucao")
    public AluguelResponse registrarDevolucao(@PathVariable UUID id,
                                              @Valid @RequestBody DevolucaoRequest request) {
        return aluguelService.registrarDevolucao(id, request);
    }

    @PatchMapping("/{id}/pagamento")
    public AluguelResponse registrarPagamento(@PathVariable UUID id) {
        return aluguelService.registrarPagamento(id);
    }

    @PatchMapping("/{id}/cancelamento")
    public AluguelResponse cancelar(@PathVariable UUID id,
                                    @Valid @RequestBody(required = false) CancelamentoRequest request) {
        if (request == null) {
            request = new CancelamentoRequest(null);
        }
        return aluguelService.cancelar(id, request);
    }
}