package school.sptech.back_localizacao.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import school.sptech.back_localizacao.dto.CoordenadaDTO;
import school.sptech.back_localizacao.dto.CursoDTO;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class CursoService {

    @Autowired
    private GeocodingService geoService;

    @Value("${mapbox.token}")
    private String token;

    @Value("${plataforma.url:http://localhost:8080}")
    private String plataformaUrl;

    public List<CursoDTO> buscarCursosProximos(String endereco) {

        CoordenadaDTO user = geoService.getCoordenadas(endereco);

        // busca turmas reais da plataforma
        List<CursoDTO> cursos = buscarTurmasDaPlataforma();

        if (cursos.isEmpty()) {
            throw new RuntimeException("Nenhum curso disponível no momento.");
        }

        return cursos.stream()
                .filter(curso -> curso.getLat() != null && curso.getLng() != null)
                .map(curso -> {
                    Double distancia = calcularDistancia(
                            user.getLat(), user.getLng(),
                            curso.getLat(), curso.getLng()
                    );
                    curso.setDistancia(distancia);
                    return curso;
                })
                .sorted(Comparator.comparing(CursoDTO::getDistancia))
                .limit(5)
                .map(curso -> {
                    Double distanciaReal = calcularDistanciaRota(
                            user.getLat(), user.getLng(),
                            curso.getLat(), curso.getLng()
                    );
                    curso.setDistancia(distanciaReal);
                    return curso;
                })
                .sorted(Comparator.comparing(CursoDTO::getDistancia))
                .toList();
    }

    private List<CursoDTO> buscarTurmasDaPlataforma() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = plataformaUrl + "/turmas/detalhes";
            String resposta = restTemplate.getForObject(url, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode turmas = mapper.readTree(resposta);

            List<CursoDTO> cursos = new ArrayList<>();

            for (JsonNode turma : turmas) {
                String nomeCurso = turma.path("cursoNome").asText();
                String rua = turma.path("enderecoRua").asText();
                String numero = turma.path("enderecoNumero").asText();
                String cidade = turma.path("enderecoCidade").asText();

                if (rua.isBlank() || cidade.isBlank()) {
                    System.out.println("TURMA SEM ENDEREÇO: " + nomeCurso);
                    continue;
                }

                String enderecoCompleto = rua + ", " + numero + ", " + cidade;

                try {
                    CoordenadaDTO coordenada = geoService.getCoordenadas(enderecoCompleto);
                    CursoDTO curso = new CursoDTO(nomeCurso);
                    curso.setLat(coordenada.getLat());
                    curso.setLng(coordenada.getLng());
                    cursos.add(curso);
                } catch (Exception e) {
                    System.out.println("ENDEREÇO NÃO GEOCODIFICADO: " + enderecoCompleto);
                }
            }

            return cursos;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar turmas da plataforma: " + e.getMessage(), e);
        }
    }

    private Double calcularDistancia(Double lat1, Double lon1, Double lat2, Double lon2) {
        final Integer raioDaTerraEmQuilometros = 6371;

        Double diferencaLatitudeEmRadianos = Math.toRadians(lat2 - lat1);
        Double diferencaLongitudeEmRadianos = Math.toRadians(lon2 - lon1);

        Double anguloCentralEmRadianos = Math.sin(diferencaLatitudeEmRadianos / 2) * Math.sin(diferencaLatitudeEmRadianos / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(diferencaLongitudeEmRadianos / 2) * Math.sin(diferencaLongitudeEmRadianos / 2);

        Double parteDaFormulaHaversine = 2 * Math.atan2(Math.sqrt(anguloCentralEmRadianos), Math.sqrt(1 - anguloCentralEmRadianos));

        return raioDaTerraEmQuilometros * parteDaFormulaHaversine;
    }

    public Double calcularDistanciaRota(Double lat1, Double lon1, Double lat2, Double lon2) {
        try {
            String url = String.format(
                    Locale.US,
                    "https://api.mapbox.com/directions/v5/mapbox/driving/%f,%f;%f,%f?access_token=%s",
                    lon1, lat1, lon2, lat2, token
            );

            RestTemplate restTemplate = new RestTemplate();
            String resposta = restTemplate.getForObject(url, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(resposta);

            JsonNode routes = root.path("routes");

            if (routes.isEmpty()) {
                return Double.MAX_VALUE;
            }

            double distance = routes.get(0).path("distance").asDouble();

            return distance / 1000;

        } catch (Exception e) {
            e.printStackTrace();
            return Double.MAX_VALUE;
        }
    }
}