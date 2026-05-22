package school.sptech.back_localizacao.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.sptech.back_localizacao.dto.CursoDTO;
import school.sptech.back_localizacao.service.CursoService;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
public class CursoController {

    @Autowired
    private CursoService service;

    @GetMapping("/cursos-proximos")
    public ResponseEntity<?> buscar(@RequestParam String endereco) {
        try {
            return ResponseEntity.ok(service.buscarCursosProximos(endereco));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", e.getMessage()));
        }
    }
}
