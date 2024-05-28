package com.so.planificadores_so;

public class Proceso {
    private String proceso;
    private int llegada;
    private int duracion;
    private String ubicacion;
    private String estado;
    private int size;

    public Proceso(String proceso, int llegada, int duracion) {
        this.proceso = proceso;
        this.llegada = llegada;
        this.duracion = duracion;
        this.ubicacion = "Memoria";
        this.estado = "W";
    }

    public Proceso(String proceso, int llegada, int duracion, int size) {
        this.proceso = proceso;
        this.llegada = llegada;
        this.duracion = duracion;
        this.ubicacion = "Memoria";
        this.estado = "W";
        this.size = size;
    }

    public String getProceso() {
        return proceso;
    }

    public int getLlegada() {
        return llegada;
    }

    public int getDuracion() {
        return duracion;
    }

    public void setDuracion(int duracion) {
        this.duracion = duracion;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void disminuirDuracion() {
        this.duracion--;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "Proceso: " + proceso +
                ", Llegada: " + llegada +
                ", Duración: " + duracion +
                ", Ubicación: " + ubicacion +
                ", Estado: " + estado +
                ", Tamaño: " + size;
    }
}