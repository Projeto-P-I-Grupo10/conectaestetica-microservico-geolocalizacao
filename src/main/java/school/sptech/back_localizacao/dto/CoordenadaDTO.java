package school.sptech.back_localizacao.dto;

public class CoordenadaDTO {
    private Double lat;
    private Double lng;

    public CoordenadaDTO(Double lat, Double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public Double getLat() { return lat; }
    public Double getLng() { return lng; }
}
