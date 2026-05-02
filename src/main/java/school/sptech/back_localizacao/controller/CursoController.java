package school.sptech.back_localizacao.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.sptech.back_localizacao.dto.CursoDTO;
import school.sptech.back_localizacao.service.CursoService;

import java.util.List;

@RestController
@CrossOrigin
public class CursoController {

    @Autowired
    private CursoService service;

    @GetMapping("/cursos-proximos")
    public List<CursoDTO> buscar(@RequestParam String endereco) {
        return service.buscarCursosProximos(endereco);
    }
}
