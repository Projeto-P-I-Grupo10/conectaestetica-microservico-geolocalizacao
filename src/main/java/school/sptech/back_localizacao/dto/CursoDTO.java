package school.sptech.back_localizacao.dto;

public class CursoDTO {

    private String nome;
    private Double lat;
    private Double lng;
    private Double distancia;

    public CursoDTO(String nome, Double lat, Double lng) {
        this.nome = nome;
        this.lat = lat;
        this.lng = lng;
    }

    public String getNome() { return nome; }
    public Double getLat() { return lat; }
    public Double getLng() { return lng; }
    public Double getDistancia() { return distancia; }

    public void setDistancia(Double distancia) {
        this.distancia = distancia;
    }
}
