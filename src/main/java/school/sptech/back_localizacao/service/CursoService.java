package school.sptech.back_localizacao.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import school.sptech.back_localizacao.dto.CoordenadaDTO;
import school.sptech.back_localizacao.dto.CursoDTO;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CursoService {

    @Autowired
    private GeocodingService geoService;

    @Value("${mapbox.token}")
    private String token;

    public List<CursoDTO> buscarCursosProximos(String endereco) {

        CoordenadaDTO user = geoService.getCoordenadas(endereco);

        // mockei só pra poc
        List<CursoDTO> cursos = List.of(
                new CursoDTO("Curso 1 - Praça da Sé", -23.5505, -46.6333),
                new CursoDTO("Curso 2 - Paulista", -23.5614, -46.6559),
                new CursoDTO("Curso 3 - Ibirapuera", -23.5874, -46.6576)
        );

        return cursos.stream()

                // calcula linha reta
                .map(curso -> {
                    Double distancia = calcularDistancia(
                            user.getLat(),
                            user.getLng(),
                            curso.getLat(),
                            curso.getLng()
                    );
                    curso.setDistancia(distancia);
                    return curso;
                })
                .sorted(Comparator.comparing(CursoDTO::getDistancia)) // ordena por proximidade
                .limit(5) // pega so os mais proximos
                .map(curso -> { // chama a api
                    Double distanciaReal = calcularDistanciaRota(
                            user.getLat(),
                            user.getLng(),
                            curso.getLat(),
                            curso.getLng()
                    );
                    curso.setDistancia(distanciaReal);
                    return curso;
                })
                .sorted(Comparator.comparing(CursoDTO::getDistancia)) // reordena com distância real
                .toList();
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

            return distance / 1000; // km

        } catch (Exception e) {
            e.printStackTrace();
            return Double.MAX_VALUE;
        }
    }
}
