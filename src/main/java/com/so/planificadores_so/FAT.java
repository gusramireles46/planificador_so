package com.so.planificadores_so;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FAT {
    private int memoria = 1000;
    private int swap = 1000;
    private List<Proceso> listaProcesos;
    private List<BloqueMemoria> bloquesMemoria;
    private StringBuilder logBuilder;

    // Tamaño del bloque
    private static final int TAMANO_BLOQUE = 100;

    public FAT() {
        this.listaProcesos = new ArrayList<>();
        this.bloquesMemoria = new ArrayList<>();
        this.logBuilder = new StringBuilder();
        // Inicializar bloques de memoria
        int numeroBloques = memoria / TAMANO_BLOQUE;
        for (int i = 0; i < numeroBloques; i++) {
            this.bloquesMemoria.add(new BloqueMemoria(null, TAMANO_BLOQUE));
        }
        String initMessage = "Inicializada FAT con " + numeroBloques + " bloques de memoria de " + TAMANO_BLOQUE + " unidades cada uno.";
        System.out.println(initMessage);
        logBuilder.append(initMessage).append("\n");
    }

    public void agregarProceso(Proceso proceso) {
        this.listaProcesos.add(proceso);
        String agregarMessage = "Agregando proceso: " + proceso;
        System.out.println(agregarMessage);
        logBuilder.append(agregarMessage).append("\n");
        asignarMemoria(proceso);
    }

    private void asignarMemoria(Proceso proceso) {
        int bloquesNecesarios = (int) Math.ceil((double) proceso.getSize() / TAMANO_BLOQUE);
        int bloquesLibres = 0;
        int indexInicio = -1;

        String asignarMessage = "Asignando memoria para el proceso: " + proceso + ", bloques necesarios: " + bloquesNecesarios;
        System.out.println(asignarMessage);
        logBuilder.append(asignarMessage).append("\n");

        // Buscar bloques libres consecutivos
        for (int i = 0; i < bloquesMemoria.size(); i++) {
            if (bloquesMemoria.get(i).getProceso() == null) {
                if (indexInicio == -1) {
                    indexInicio = i;
                }
                bloquesLibres++;
                if (bloquesLibres == bloquesNecesarios) {
                    break;
                }
            } else {
                bloquesLibres = 0;
                indexInicio = -1;
            }
        }

        if (bloquesLibres >= bloquesNecesarios) {
            for (int i = indexInicio; i < indexInicio + bloquesNecesarios; i++) {
                bloquesMemoria.set(i, new BloqueMemoria(proceso, TAMANO_BLOQUE));
            }
            memoria -= bloquesNecesarios * TAMANO_BLOQUE;
            proceso.setUbicacion("Memoria");
            proceso.setEstado("Asignado");
            String asignadoMessage = "Proceso asignado en memoria: " + proceso;
            String memoriaDisponibleMessage = "Memoria disponible: " + memoria;
            System.out.println(asignadoMessage);
            System.out.println(memoriaDisponibleMessage);
            logBuilder.append(asignadoMessage).append("\n");
            logBuilder.append(memoriaDisponibleMessage).append("\n");
        } else {
            // Si no se encontró espacio en memoria, decidir qué hacer
            String noEspacioMessage = "No se encontró espacio contiguo en memoria para el proceso: " + proceso;
            System.out.println(noEspacioMessage);
            logBuilder.append(noEspacioMessage).append("\n");
            decidirAccion(proceso, bloquesNecesarios);
        }
    }

    private void decidirAccion(Proceso proceso, int bloquesNecesarios) {
        int espacioLibre = contarBloquesLibres();
        String decisionMessage = "Decidiendo acción para el proceso: " + proceso + ", bloques libres: " + espacioLibre + ", bloques necesarios: " + bloquesNecesarios;
        System.out.println(decisionMessage);
        logBuilder.append(decisionMessage).append("\n");

        if (espacioLibre >= bloquesNecesarios) {
            // Si hay espacio suficiente, pero no consecutivo, hacer defrag
            String defragMessage = "Realizando defragmentación...";
            System.out.println(defragMessage);
            logBuilder.append(defragMessage).append("\n");
            defrag();
            asignarMemoria(proceso);
        } else {
            // Si no hay espacio suficiente, hacer swap
            String swapMessage = "Realizando swap in...";
            System.out.println(swapMessage);
            logBuilder.append(swapMessage).append("\n");
            swapIn(proceso);
        }
    }

    private int contarBloquesLibres() {
        int espacioLibre = 0;
        for (BloqueMemoria bloque : bloquesMemoria) {
            if (bloque.getProceso() == null) {
                espacioLibre++;
            }
        }
        return espacioLibre;
    }

    public void defrag() {
        List<BloqueMemoria> nuevosBloques = new ArrayList<>();
        int espacioLibre = 0;

        for (BloqueMemoria bloque : bloquesMemoria) {
            if (bloque.getProceso() == null) {
                espacioLibre += bloque.getSize();
            } else {
                nuevosBloques.add(bloque);
            }
        }

        int bloquesLibres = espacioLibre / TAMANO_BLOQUE;
        for (int i = 0; i < bloquesLibres; i++) {
            nuevosBloques.add(new BloqueMemoria(null, TAMANO_BLOQUE));
        }

        this.bloquesMemoria = nuevosBloques;
        String defragCompleteMessage = "Defragmentación completada. Bloques libres: " + bloquesLibres;
        System.out.println(defragCompleteMessage);
        logBuilder.append(defragCompleteMessage).append("\n");
    }

    public void swapIn(Proceso proceso) {
        int bloquesNecesarios = (int) Math.ceil((double) proceso.getSize() / TAMANO_BLOQUE);
        if (swap >= bloquesNecesarios * TAMANO_BLOQUE) {
            swap -= bloquesNecesarios * TAMANO_BLOQUE;
            proceso.setUbicacion("Swap");
            proceso.setEstado("Swap In");
            String swapInMessage = "Proceso movido a swap: " + proceso;
            String swapDisponibleMessage = "Swap disponible: " + swap;
            System.out.println(swapInMessage);
            System.out.println(swapDisponibleMessage);
            logBuilder.append(swapInMessage).append("\n");
            logBuilder.append(swapDisponibleMessage).append("\n");
        } else {
            // Si no hay espacio suficiente en swap, hacer swap out de un proceso
            String noSwapEspacioMessage = "No hay espacio suficiente en swap, realizando swap out...";
            System.out.println(noSwapEspacioMessage);
            logBuilder.append(noSwapEspacioMessage).append("\n");
            swapOut();
            swapIn(proceso);
        }
    }

    public void swapOut() {
        for (BloqueMemoria bloque : bloquesMemoria) {
            if (bloque.getProceso() != null) {
                Proceso proceso = bloque.getProceso();
                int bloquesNecesarios = (int) Math.ceil((double) proceso.getSize() / TAMANO_BLOQUE);
                for (int i = 0; i < bloquesNecesarios; i++) {
                    bloquesMemoria.remove(bloque);
                }
                memoria += bloquesNecesarios * TAMANO_BLOQUE;
                proceso.setUbicacion("Swap");
                proceso.setEstado("Swap Out");
                String swapOutMessage = "Proceso movido fuera de memoria a swap: " + proceso;
                String memoriaDisponibleMessage = "Memoria disponible después de swap out: " + memoria;
                System.out.println(swapOutMessage);
                System.out.println(memoriaDisponibleMessage);
                logBuilder.append(swapOutMessage).append("\n");
                logBuilder.append(memoriaDisponibleMessage).append("\n");
                return;
            }
        }
    }

    public int getMemoria() {
        return memoria;
    }

    public int getSwap() {
        return swap;
    }

    public void setMemoria(int memoria) {
        this.memoria = memoria;
    }

    public void setSwap(int swap) {
        this.swap = swap;
    }

    @Override
    public String toString() {
        return "FAT{" + "memoria=" + memoria + ", swap=" + swap + '}' + "\n" + logBuilder.toString();
    }

    public void exportarSalida(List<Proceso> procesosSalidaList, String metodo) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        String fechaHora = dateFormat.format(new Date());
        String nombreArchivo = metodo + "_" + fechaHora + ".txt";
        String directorioEscritorio = System.getProperty("user.home") + "/Desktop/Planificador SO/";

        File carpeta = new File(directorioEscritorio);
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }

        File archivo = new File(directorioEscritorio + nombreArchivo);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
            for (Proceso proceso : procesosSalidaList) {
                writer.write(proceso.toString());
                writer.newLine();
            }
            writer.write("\nMemoria total: " + memoria);
            writer.write("\nSwap total: " + swap);
            writer.write("\n\nLog de procesos:\n" + logBuilder.toString());
            System.out.println("Archivo exportado correctamente: " + archivo.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error al exportar el archivo: " + e.getMessage());
        }
    }
}

class BloqueMemoria {
    private Proceso proceso;
    private int size;

    public BloqueMemoria(Proceso proceso, int size) {
        this.proceso = proceso;
        this.size = size;
    }

    public Proceso getProceso() {
        return proceso;
    }

    public int getSize() {
        return size;
    }

    public void setProceso(Proceso proceso) {
        this.proceso = proceso;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "BloqueMemoria{" +
                "proceso=" + (proceso != null ? proceso.getProceso() : "null") +
                ", size=" + size +
                '}';
    }
}
