package school.sptech.back_localizacao.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import school.sptech.back_localizacao.dto.CoordenadaDTO;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

@Service
public class GeocodingService {

    @Value("${mapbox.token}")
    private String token;

    private static final double RELEVANCIA_MINIMA = 0.5;
    private static final int PALAVRAS_EM_COMUM_MINIMAS = 2;

    public CoordenadaDTO getCoordenadas(String endereco) {

        CoordenadaDTO resultado = tentarGeocodificar(endereco);

        if (resultado == null) {
            String enderecoSemNumero = endereco.replaceAll("\\s*,?\\s*\\d+\\s*$", "").trim();

            if (!enderecoSemNumero.equals(endereco)) {
                System.out.println("TENTANDO SEM NÚMERO: " + enderecoSemNumero);
                resultado = tentarGeocodificar(enderecoSemNumero);
            }
        }

        if (resultado == null) {
            throw new RuntimeException(
                    "Endereço não encontrado. Tente formatos como: 'Rua Augusta, 1000' ou 'Praça da Sé'"
            );
        }

        return resultado;
    }

    private CoordenadaDTO tentarGeocodificar(String endereco) {
        try {
            // normaliza antes de enviar pra API (remove acentos)
            String enderecoNormalizado = Normalizer
                    .normalize(endereco, Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}", "");

            String url = "https://api.mapbox.com/geocoding/v5/mapbox.places/"
                    + URLEncoder.encode(enderecoNormalizado, StandardCharsets.UTF_8)
                    + ".json?access_token=" + token
                    + "&country=br"
                    + "&types=address,poi"
                    + "&bbox=-46.825,-24.008,-46.365,-23.356"
                    + "&proximity=-46.6333,-23.5505"
                    + "&autocomplete=false";

            RestTemplate restTemplate = new RestTemplate();
            String resposta = restTemplate.getForObject(url, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(resposta);
            JsonNode features = root.path("features");

            if (features.isEmpty()) {
                System.out.println(">> REPROVADO: nenhum resultado retornado pela API");
                return null;
            }

            JsonNode melhor = null;
            double maiorRelevancia = -1;

            for (JsonNode feature : features) {
                double relevancia = feature.path("relevance").asDouble();
                if (relevancia > maiorRelevancia) {
                    maiorRelevancia = relevancia;
                    melhor = feature;
                }
            }

            System.out.println("ENDEREÇO BUSCADO: " + endereco);
            System.out.println("ESCOLHIDO: " + melhor.path("place_name").asText());
            System.out.println("RELEVANCIA: " + maiorRelevancia);

            if (maiorRelevancia < RELEVANCIA_MINIMA) {
                System.out.println(">> REPROVADO: relevância baixa");
                return null;
            }

            // normaliza os dois lados antes de comparar
            String nomeDoLugar = Normalizer
                    .normalize(melhor.path("place_name").asText().toLowerCase(), Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}", "");

            String enderecoLower = Normalizer
                    .normalize(endereco.toLowerCase(), Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}", "");

            enderecoLower = enderecoLower.replaceAll("[,.]", "");

            String[] palavras = enderecoLower.split(" ");
            int palavrasIguais = 0;

//            for (String palavra : palavras) {
//                if (palavra.length() >= 2 && nomeDoLugar.contains(palavra)) {
//                    palavrasIguais++;
//                }
//            }

            List<String> ignorar = List.of("rua", "av", "avenida", "alameda", "praca", "travessa", "rodovia");

            int palavrasUteis = 0;
            for (String palavra : palavras) {
                if (palavra.length() >= 2
                        && !ignorar.contains(palavra)
                        && !palavra.matches("\\d+")) {
                    palavrasUteis++;
                }
            }

            int minimoNecessario = palavrasUteis >= 3 ? 2 : 1;

            for (String palavra : palavras) {
                if (palavra.length() >= 2
                        && !ignorar.contains(palavra)
                        && !palavra.matches("\\d+")
                        && nomeDoLugar.contains(palavra)) {
                    palavrasIguais++;
                }
            }

            System.out.println("PALAVRAS UTEIS: " + palavrasUteis);
            System.out.println("PALAVRAS EM COMUM: " + palavrasIguais);
            System.out.println("MINIMO NECESSARIO: " + minimoNecessario);
            System.out.println("PALAVRAS ANALISADAS: " + Arrays.toString(palavras));
            System.out.println("NOME DO LUGAR: " + nomeDoLugar);

            if (palavrasIguais < minimoNecessario) {
                System.out.println(">> REPROVADO: poucas palavras em comum");
                throw new RuntimeException(
                        "Endereço muito genérico. Tente incluir o número e a cidade. Ex: 'Rua das Flores, 99, São Paulo'"
                );
            }

            JsonNode center = melhor.path("center");
            double lng = center.get(0).asDouble();
            double lat = center.get(1).asDouble();

            System.out.println("LAT: " + lat + " | LNG: " + lng);

            return new CoordenadaDTO(lat, lng);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao chamar API de geocoding: " + e.getMessage(), e);
        }
    }
}