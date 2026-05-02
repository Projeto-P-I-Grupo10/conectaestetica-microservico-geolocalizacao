package school.sptech.back_localizacao.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import school.sptech.back_localizacao.dto.CoordenadaDTO;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class GeocodingService {

    @Value("${mapbox.token}") // aqui eu to puxando o meu token da properties
    private String token;

    public CoordenadaDTO getCoordenadas(String endereco) {
        try {

            // validacao com regex pra garantir que tenha algum numero
//            if (!endereco.matches(".*\\d+.*")) {
//                throw new RuntimeException("Informe o número do endereço");
//            }

            String url = "https://api.mapbox.com/geocoding/v5/mapbox.places/"
                    + URLEncoder.encode(endereco, StandardCharsets.UTF_8)
                    + ".json?access_token=" + token
                    + "&country=br" // tem que estar no brasil
                    + "&types=address,poi" // aqui to definindo que é um endereço
                    + "&bbox=-46.825,-24.008,-46.365,-23.356" // to limitando a sp
                    + "&proximity=-46.6333,-23.5505" // centro de SP
                    + "&autocomplete=false"; // aqui eu tiro sugestao pra pegar o endereco exato

            RestTemplate restTemplate = new RestTemplate();
            String resposta = restTemplate.getForObject(url, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(resposta);

            JsonNode features = root.path("features"); // pega a lista de resultados da api

            if (features.isEmpty()) {
                throw new RuntimeException("Endereço não encontrado");
            }

            // pega o melhor resultado baseado em relevância
            JsonNode melhor = null;
            double maiorRelevancia = -1;

            for (JsonNode feature : features) {
                double relevancia = feature.path("relevance").asDouble();

                if (relevancia > maiorRelevancia) {
                    maiorRelevancia = relevancia;
                    melhor = feature;
                }
            }

            if (maiorRelevancia < 0.8) {
                throw new RuntimeException("Endereço não confiável (baixa relevância: " + maiorRelevancia + ")");
            }


            String nomeDoLugar = melhor.path("place_name").asText().toLowerCase();
            String enderecoLower = endereco.toLowerCase();

            String[] palavras = enderecoLower.split(" "); // quebro o endereco em palavras
            int palavrasIguais = 0;

            for (String palavra : palavras) {
                if (nomeDoLugar.contains(palavra)) {
                    palavrasIguais++;
                }
            }

            if (palavrasIguais < 2) { // se nao tiverem pelo menos 2 palavras iguais da erro
                throw new RuntimeException("Endereço divergente do buscado");
            }

            JsonNode center = melhor.path("center");

            double lng = center.get(0).asDouble();
            double lat = center.get(1).asDouble();

            System.out.println("ENDEREÇO BUSCADO: " + endereco);
            System.out.println("ESCOLHIDO: " + melhor.path("place_name").asText());
            System.out.println("RELEVANCIA: " + maiorRelevancia);
            System.out.println("LAT: " + lat);
            System.out.println("LNG: " + lng);

            return new CoordenadaDTO(lat, lng);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter endereço", e);
        }
    }
}