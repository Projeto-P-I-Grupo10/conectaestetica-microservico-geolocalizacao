package school.sptech.back_localizacao.dto;

public class CursoDTO {

    private String nome;
    private Double lat;
    private Double lng;
    private Double distancia;

    public CursoDTO(String nome) {
        this.nome = nome;
    }

    public String getNome() { return nome; }
    public Double getLat() { return lat; }
    public Double getLng() { return lng; }
    public Double getDistancia() { return distancia; }

    public void setLat(Double lat) { this.lat = lat; }
    public void setLng(Double lng) { this.lng = lng; }
    public void setDistancia(Double distancia) { this.distancia = distancia; }
}