package school.sptech.back_localizacao.presentation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.sptech.back_localizacao.application.dto.CursoDTO;
import school.sptech.back_localizacao.application.useCases.BuscaCursosProximos;
import school.sptech.back_localizacao.presentation.exception.JsonMalFormatadoException;

import java.util.List;

@RestController
@CrossOrigin
public class CursoController {

    @Autowired
    private BuscaCursosProximos buscaCursosProximos;

    @GetMapping("/cursos-proximos")
    public ResponseEntity<List<CursoDTO>> buscar(@RequestParam String endereco) throws JsonMalFormatadoException {
        try {
            return ResponseEntity.ok(buscaCursosProximos.buscar(endereco));
//        } catch (RuntimeException e) {
////            return ResponseEntity
////                    .status(HttpStatus.BAD_REQUEST)
////                    .body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            throw new JsonMalFormatadoException("Valores inválidos!", e);
        }
    }
}
