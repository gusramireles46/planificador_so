package com.so.planificadores_so;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class Planificador extends Stage {
    // Interfaz de Usuario
    private BorderPane bdpPrincipal;
    private VBox vLeft, vCenter, vRight;
    private HBox hTextFields, hMain, hButtons;
    private Scene escena;
    private MenuBar menuBar;
    private Menu mnArchivo, mnOpciones;
    private MenuItem mitArchivo, mitSalir, mitCarpeta;
    private TableView<Proceso> tbvEntrada, tbvMemoria, tbvSalida, tbvProcesos;
    private ComboBox<String> cbxOrden;
    private Label lblCPU, lblQuantum, lblTiempo, lblTablaEntrada, lblTablaSalida, lblTabla;
    private TextField txtCPU, txtQuantum, txtTiempo;
    private Button btnIniciar, btnLimpiar;

    // Datos de Procesos
    private ObservableList<Proceso> procesosList, procesosTablaList, procesosSalidaList; // Listas de procesos
    private Proceso[] procesos; // Arreglo de procesos

    // Gestión de Tiempo
    private Timer timer; // Temporizador
    private int currentTime; // Tiempo actual
    private int quantum; // Quantum de tiempo
    private int quantumRemaining; // Tiempo restante del quantum

    // FAT
    private FAT fat;

    // Componentes de la Interfaz de Usuario Específicos
    private VBox vTabla, vMemoria, vCPU, vQuantum, vTiempo;
    private Label lblMemoria;

    public Planificador() {
        if (fat != null) {
            this.fat = fat;
        } else {
            this.fat = new FAT();
        }
        CrearGUI();
        escena = new Scene(bdpPrincipal, 1280, 720);
        escena.getStylesheets().addAll(BootstrapFX.bootstrapFXStylesheet(), getClass().getResource("css/style.css").toExternalForm());
        this.setScene(escena);
        this.setResizable(false);
        this.setMaximized(true);
        this.show();
        this.setOnCloseRequest(e -> {
            detenerTimer();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Salir");
            alert.setHeaderText(null);
            alert.setContentText("¿Está seguro que desea salir?");
            Optional<ButtonType> opc = alert.showAndWait();
            if (opc.get() == ButtonType.OK) {
                System.exit(0);
            } else {
                e.consume();
            }
        });
    }

    private void detenerTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void CrearGUI() {
        bdpPrincipal = new BorderPane();
        crearMenu();
        crearTablaEntrada();
        createCenter();
        createTableSalida();
        createButtons();

        cbxOrden = new ComboBox<>();
        cbxOrden.getItems().addAll("First In First Out", "Last In First Out", "Shortest Job First", "Longest Job First", "Round Robin + FIFO", "Round Robin + LIFO");
        cbxOrden.setValue("First In First Out");
        cbxOrden.getStyleClass().addAll("cbxOrden");

        lblTablaEntrada = new Label("Entrada");
        lblTablaEntrada.getStyleClass().addAll("h4");
        vLeft = new VBox(lblTablaEntrada, tbvEntrada, cbxOrden);
        vLeft.setSpacing(10);
        vLeft.setAlignment(Pos.TOP_CENTER);
        vLeft.setPadding(new Insets(15));
        VBox.setVgrow(tbvEntrada, Priority.ALWAYS);
        VBox.setVgrow(vLeft, Priority.ALWAYS);

        lblTablaSalida = new Label("Salida");
        lblTablaSalida.getStyleClass().addAll("h4");
        tbvSalida.setPrefWidth(200);
        vRight = new VBox(lblTablaSalida, tbvSalida);
        vRight.setSpacing(10);
        vRight.setAlignment(Pos.TOP_CENTER);
        vRight.setPadding(new Insets(15));
        VBox.setVgrow(tbvSalida, Priority.ALWAYS);
        VBox.setVgrow(vRight, Priority.ALWAYS);

        Label lblTitle = new Label("Planificador de Procesos");
        lblTitle.getStyleClass().addAll("h1", "strong");
        lblTitle.setAlignment(Pos.TOP_CENTER);
        HBox title = new HBox(lblTitle);
        title.setAlignment(Pos.TOP_CENTER);

        VBox vTop = new VBox(menuBar, title);

        bdpPrincipal.setLeft(vLeft);
        bdpPrincipal.setTop(vTop);
        bdpPrincipal.setCenter(vCenter);
        bdpPrincipal.setRight(vRight);
        bdpPrincipal.setBottom(hButtons);
    }

    private void crearMenu() {
        mitArchivo = new MenuItem("Importar desde archivo");
        mitArchivo.setOnAction(e -> chooseFile());

        mitSalir = new MenuItem("Salir");
        mitSalir.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Salir");
            alert.setHeaderText(null);
            alert.setContentText("¿Está seguro que desea salir?");
            Optional<ButtonType> opc = alert.showAndWait();
            if (opc.get() == ButtonType.OK) {
                System.exit(0);
            } else {
                e.consume();
            }
        });

        mitCarpeta = new MenuItem("Ir a la carpeta de salidas");
        mitCarpeta.setOnAction(e -> {
            try {
                Desktop.getDesktop().open(new File(System.getProperty("user.home") + "/Desktop/Planificador SO/"));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });

        mnArchivo = new Menu("Archivo");
        mnArchivo.getItems().addAll(mitArchivo, mitCarpeta);

        mnOpciones = new Menu("Opciones");
        mnOpciones.getItems().add(mitSalir);

        menuBar = new MenuBar();
        menuBar.getMenus().addAll(mnArchivo, mnOpciones);
        bdpPrincipal.setTop(menuBar);
    }

    private void chooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccione el archivo para importar");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Archivos para planificador", "*.pso"));
        File file = fileChooser.showOpenDialog(this);
        if (file != null) {
            System.out.println(file.getAbsolutePath());
            readFile(file);
        }
    }

    private void readFile(File file) {
        procesosList.clear();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                String proceso = parts[0];
                int llegada = Integer.parseInt(parts[1]);
                int duracion = Integer.parseInt(parts[2]);
                if (parts.length == 4) {
                    int size = Integer.parseInt(parts[3]);
                    procesosList.add(new Proceso(proceso, llegada, duracion, size));
                } else {
                    procesosList.add(new Proceso(proceso, llegada, duracion));
                }
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void crearTablaEntrada() {
        procesosList = FXCollections.observableArrayList();
        tbvEntrada = new TableView<>();
        TableColumn<Proceso, String> tbcProceso = new TableColumn<>("Proceso");
        tbcProceso.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProceso()));

        TableColumn<Proceso, Integer> tbcLlegada = new TableColumn<>("Llegada");
        tbcLlegada.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getLlegada()).asObject());

        TableColumn<Proceso, Integer> tbcDuracion = new TableColumn<>("Duracion");
        tbcDuracion.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getDuracion()).asObject());

        TableColumn<Proceso, Integer> tbcSize = new TableColumn<>("Tamaño");
        tbcSize.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getSize()).asObject());

        tbvEntrada.getColumns().addAll(tbcProceso, tbcLlegada, tbcDuracion, tbcSize);
        tbvEntrada.setItems(procesosList);
    }

    private void createCenter() {
        lblCPU = new Label("CPU");
        lblCPU.getStyleClass().addAll("h4");
        lblQuantum = new Label("Quantum");
        lblQuantum.getStyleClass().addAll("h4");
        lblTiempo = new Label("Tiempo");
        lblTiempo.getStyleClass().addAll("h4");

        txtCPU = new TextField();
        txtCPU.setPrefSize(50, 50);
        txtCPU.setEditable(false);
        vCPU = new VBox(lblCPU, txtCPU);
        vCPU.setSpacing(5);
        vCPU.setAlignment(Pos.CENTER);

        txtQuantum = new TextField();
        txtQuantum.setMaxSize(50, 50);
        txtQuantum.setPrefSize(50, 50);
        vQuantum = new VBox(lblQuantum, txtQuantum);
        vQuantum.setSpacing(5);
        vQuantum.setAlignment(Pos.CENTER);

        txtTiempo = new TextField();
        txtTiempo.setPrefSize(50, 50);
        txtTiempo.setEditable(false);
        vTiempo = new VBox(lblTiempo, txtTiempo);
        vTiempo.setSpacing(5);
        vTiempo.setAlignment(Pos.CENTER);

        hTextFields = new HBox(vCPU, vQuantum, vTiempo);
        hTextFields.setSpacing(100);
        hTextFields.setAlignment(Pos.CENTER);

        createTableMemory();
        createTableProcess();

        lblTabla = new Label("Tabla de procesos");
        lblTabla.getStyleClass().addAll("h4");
        vTabla = new VBox(lblTabla, tbvProcesos);
        vTabla.setSpacing(10);

        lblMemoria = new Label("Memoria");
        lblMemoria.getStyleClass().addAll("h4");
        vMemoria = new VBox(lblMemoria, tbvMemoria);
        vMemoria.setSpacing(10);

        hMain = new HBox(vTabla, vMemoria);
        hMain.setSpacing(10);
        hMain.setAlignment(Pos.TOP_CENTER);
        hMain.setPadding(new Insets(15));

        vCenter = new VBox(hTextFields, hMain);
        vCenter.setSpacing(10);
        vCenter.setAlignment(Pos.TOP_CENTER);
        vCenter.setPadding(new Insets(15));
    }

    private void createTableMemory() {
        procesosTablaList = FXCollections.observableArrayList();
        tbvMemoria = new TableView<>(procesosTablaList);

        TableColumn<Proceso, String> tbcProceso = new TableColumn<>("Proceso");
        tbcProceso.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProceso()));

        TableColumn<Proceso, String> tbcEstado = new TableColumn<>("Estado");
        tbcEstado.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEstado()));

        tbvMemoria.getColumns().addAll(tbcProceso, tbcEstado);
    }

    private void createTableProcess() {
        tbvProcesos = new TableView<>(procesosList);

        TableColumn<Proceso, String> tbcProceso = new TableColumn<>("Proceso");
        tbcProceso.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProceso()));

        TableColumn<Proceso, String> tbcUbicacion = new TableColumn<>("Ubicacion");
        tbcUbicacion.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUbicacion()));

        TableColumn<Proceso, String> tbcEstado = new TableColumn<>("Estado");
        tbcEstado.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEstado()));

        TableColumn<Proceso, Integer> tbcDuracion = new TableColumn<>("Duracion");
        tbcDuracion.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getDuracion()).asObject());

        tbvProcesos.getColumns().addAll(tbcProceso, tbcUbicacion, tbcEstado, tbcDuracion);
    }

    private void createTableSalida() {
        procesosSalidaList = FXCollections.observableArrayList();
        tbvSalida = new TableView<>(procesosSalidaList);

        TableColumn<Proceso, String> tbcProceso = new TableColumn<>("Proceso");
        tbcProceso.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProceso()));

        tbvSalida.getColumns().addAll(tbcProceso);
        tbvSalida.resizeColumn(tbcProceso, 115);
        tbcProceso.setStyle("-fx-alignment: center;");
    }

    private void createButtons() {
        btnIniciar = new Button("Iniciar");
        btnIniciar.getStyleClass().addAll("btn", "btn-primary");
        btnIniciar.setOnAction(e -> {
            String opcion = cbxOrden.getValue();
            switch (opcion) {
                case "First In First Out":
                default:
                    startFifo();
                    break;
                case "Last In First Out":
                    startLifo();
                    break;
                case "Shortest Job First":
                    startSJF();
                    break;
                case "Longest Job First":
                    startLJF();
                    break;
                case "Round Robin + FIFO":
                    startRoundRobinFIFO();
                    break;
                case "Round Robin + LIFO":
                    startRoundRobinLIFO();
                    break;
            }
        });

        btnLimpiar = new Button("Limpiar");
        btnLimpiar.setOnAction(e -> clearAll());
        btnLimpiar.getStyleClass().addAll("btn", "btn-danger");

        hButtons = new HBox(btnIniciar, btnLimpiar);
        hButtons.setSpacing(15);
        hButtons.setAlignment(Pos.CENTER);
        hButtons.setPadding(new Insets(15));
    }

    private void startFifo() {
        if (tbvEntrada.getItems().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error");
            alert.setContentText("Debe ingresar al menos un proceso");
            alert.showAndWait();
        } else {
            btnIniciar.setDisable(true);
            btnLimpiar.setDisable(true);
            clearTables();
            currentTime = 0;
            processesToTablesFifo();

            timer = new Timer(true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> updateTablesFifo());
                }
            }, 0, 1000);
        }
    }

    private void clearAll() {
        detenerTimer();
        clearTables();
        procesosList.clear();
        cbxOrden.setValue("First In First Out");
        txtCPU.clear();
        txtQuantum.clear();
        txtTiempo.clear();
        btnIniciar.setDisable(false);
        btnLimpiar.setDisable(false);
        fat = null;
        this.fat = new FAT();
    }

    private void clearTables() {
        procesosTablaList.clear();
        procesosSalidaList.clear();
    }

    private void processesToTablesFifo() {
        procesosList.sort(Comparator.comparingInt(Proceso::getLlegada));

        for (Proceso proceso : procesosList) {
            if (proceso.getLlegada() <= currentTime) {
                proceso.setUbicacion("CPU");
                proceso.setEstado("X");
                procesosTablaList.add(proceso);
                fat.agregarProceso(proceso);
            } else {
                proceso.setUbicacion("Memoria");
                proceso.setEstado("W");
            }
        }

        txtCPU.setText(procesosTablaList.isEmpty() ? "" : procesosTablaList.get(0).getProceso());
    }

    private void updateTablesFifo() {
        currentTime++;
        txtTiempo.setText(String.valueOf(currentTime));

        Proceso currentProcess = null;
        for (Proceso proceso : procesosTablaList) {
            if (proceso.getUbicacion().equals("CPU")) {
                proceso.disminuirDuracion();
                currentProcess = proceso;
                break;
            }
        }

        if (currentProcess != null && currentProcess.getDuracion() <= 0) {
            currentProcess.setUbicacion("Salida");
            currentProcess.setEstado("F");
            procesosSalidaList.add(currentProcess);
            procesosTablaList.remove(currentProcess);
        }

        for (Proceso proceso : procesosList) {
            if (proceso.getLlegada() == currentTime && !proceso.getUbicacion().equals("CPU") && !proceso.getUbicacion().equals("Salida")) {
                proceso.setUbicacion("Memoria");
                proceso.setEstado("W");
                procesosTablaList.add(proceso);
                fat.agregarProceso(proceso); // Asegúrate de que el proceso se agregue a FAT al llegar
            }
        }

        if (!procesosTablaList.isEmpty() && (currentProcess == null || currentProcess.getDuracion() <= 0)) {
            procesosTablaList.sort(Comparator.comparingInt(Proceso::getLlegada));
            Proceso nextProcess = procesosTablaList.get(0);
            nextProcess.setUbicacion("CPU");
            nextProcess.setEstado("X");
            txtCPU.setText(nextProcess.getProceso());
        }

        tbvMemoria.refresh();
        tbvProcesos.refresh();
        tbvSalida.refresh();

        if (procesosList.size() == procesosSalidaList.size()) {
            detenerTimer();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Proceso terminado");
            alert.setHeaderText(null);
            alert.setContentText("Todos los procesos han terminado");
            alert.showAndWait();
            btnLimpiar.setDisable(false);
            exportFile(procesosSalidaList, "FIFO");
            fat.exportarSalida(procesosSalidaList, "FIFO");
        }
    }

    private void startLifo() {
        if (tbvEntrada.getItems().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error");
            alert.setContentText("Debe ingresar al menos un proceso");
            alert.showAndWait();
        } else {
            btnIniciar.setDisable(true);
            btnLimpiar.setDisable(true);
            clearTables();
            currentTime = 0;
            processesToTablesLifo();

            timer = new Timer(true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> updateTablesLifo());
                }
            }, 0, 1000);
        }
    }

    private void processesToTablesLifo() {
        procesosList.sort(Comparator.comparingInt(Proceso::getLlegada).reversed()); // Ordenar por llegada en orden inverso

        for (Proceso proceso : procesosList) {
            if (proceso.getLlegada() <= currentTime) {
                proceso.setUbicacion("CPU");
                proceso.setEstado("X");
                procesosTablaList.add(proceso);
                fat.agregarProceso(proceso); // Asegúrate de que el proceso se agregue a FAT al llegar
            } else {
                proceso.setUbicacion("Memoria");
                proceso.setEstado("W");
            }
        }

        txtCPU.setText(procesosTablaList.isEmpty() ? "" : procesosTablaList.get(0).getProceso());
    }

    private void updateTablesLifo() {
        currentTime++;
        txtTiempo.setText(String.valueOf(currentTime));

        Proceso currentProcess = null;
        for (Proceso proceso : procesosTablaList) {
            if (proceso.getUbicacion().equals("CPU")) {
                proceso.disminuirDuracion();
                currentProcess = proceso;
                break;
            }
        }

        if (currentProcess != null && currentProcess.getDuracion() <= 0) {
            currentProcess.setUbicacion("Salida");
            currentProcess.setEstado("F");
            procesosSalidaList.add(currentProcess);
            procesosTablaList.remove(currentProcess);
        }

        for (Proceso proceso : procesosList) {
            if (proceso.getLlegada() == currentTime && !proceso.getUbicacion().equals("CPU") && !proceso.getUbicacion().equals("Salida")) {
                proceso.setUbicacion("Memoria");
                proceso.setEstado("W");
                procesosTablaList.add(proceso);
                fat.agregarProceso(proceso); // Asegúrate de que el proceso se agregue a FAT al llegar
            }
        }

        if (!procesosTablaList.isEmpty() && (currentProcess == null || currentProcess.getDuracion() <= 0)) {
            procesosTablaList.sort(Comparator.comparingInt(Proceso::getLlegada).reversed());
            Proceso nextProcess = procesosTablaList.get(0);
            nextProcess.setUbicacion("CPU");
            nextProcess.setEstado("X");
            txtCPU.setText(nextProcess.getProceso());
        }

        tbvMemoria.refresh();
        tbvProcesos.refresh();
        tbvSalida.refresh();

        if (procesosList.size() == procesosSalidaList.size()) {
            detenerTimer();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Proceso terminado");
            alert.setHeaderText(null);
            alert.setContentText("Todos los procesos han terminado");
            alert.showAndWait();
            btnLimpiar.setDisable(false);
            exportFile(procesosSalidaList, "LIFO");
        }
    }

    private void startSJF() {
        if (tbvEntrada.getItems().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error");
            alert.setContentText("Debe ingresar al menos un proceso");
            alert.showAndWait();
        } else {
            btnIniciar.setDisable(true);
            btnLimpiar.setDisable(true);
            clearTables();
            currentTime = 0;
            processesToTablesSjf();

            timer = new Timer(true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> updateTablesSjf());
                }
            }, 0, 1000);
        }
    }

    private void updateTablesSjf() {
        currentTime++;
        txtTiempo.setText(String.valueOf(currentTime));

        Proceso currentProcess = null;
        for (Proceso proceso : procesosTablaList) {
            if (proceso.getUbicacion().equals("CPU")) {
                proceso.disminuirDuracion();
                currentProcess = proceso;
                break;
            }
        }

        if (currentProcess != null && currentProcess.getDuracion() <= 0) {
            currentProcess.setUbicacion("Salida");
            currentProcess.setEstado("F");
            procesosSalidaList.add(currentProcess);
            procesosTablaList.remove(currentProcess);
        }

        for (Proceso proceso : procesosList) {
            if (proceso.getLlegada() == currentTime && !proceso.getUbicacion().equals("CPU") && !proceso.getUbicacion().equals("Salida")) {
                proceso.setUbicacion("Memoria");
                proceso.setEstado("W");
                procesosTablaList.add(proceso);
                fat.agregarProceso(proceso); // Asegúrate de que el proceso se agregue a FAT al llegar
            }
        }

        if (!procesosTablaList.isEmpty() && (currentProcess == null || currentProcess.getDuracion() <= 0)) {
            procesosTablaList.sort((p1, p2) -> {
                if (p1.getSize() != p2.getSize()) {
                    return Integer.compare(p1.getSize(), p2.getSize());
                } else {
                    return Integer.compare(p1.getLlegada(), p2.getLlegada());
                }
            });

            Proceso nextProcess = procesosTablaList.get(0);
            nextProcess.setUbicacion("CPU");
            nextProcess.setEstado("X");
            txtCPU.setText(nextProcess.getProceso());
        }

        tbvMemoria.refresh();
        tbvProcesos.refresh();
        tbvSalida.refresh();

        if (procesosList.size() == procesosSalidaList.size()) {
            detenerTimer();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Proceso terminado");
            alert.setHeaderText(null);
            alert.setContentText("Todos los procesos han terminado");
            alert.showAndWait();
            btnLimpiar.setDisable(false);
            exportFile(procesosSalidaList, "SJF");
        }
    }

    private void processesToTablesSjf() {
        procesosList.sort(Comparator.comparingInt(Proceso::getLlegada).reversed());

        for (Proceso proceso : procesosList) {
            if (proceso.getLlegada() <= currentTime) {
                proceso.setUbicacion("CPU");
                proceso.setEstado("X");
                procesosTablaList.add(proceso);
                fat.agregarProceso(proceso); // Asegúrate de que el proceso se agregue a FAT al llegar
            } else {
                proceso.setUbicacion("Memoria");
                proceso.setEstado("W");
            }
        }

        txtCPU.setText(procesosTablaList.isEmpty() ? "" : procesosTablaList.get(0).getProceso());
    }

    private void startLJF() {
        if (tbvEntrada.getItems().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error");
            alert.setContentText("Debe ingresar al menos un proceso");
            alert.showAndWait();
        } else {
            btnIniciar.setDisable(true);
            btnLimpiar.setDisable(true);
            clearTables();
            currentTime = 0;
            processesToTablesLjf();

            timer = new Timer(true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> updateTablesLjf());
                }
            }, 0, 1000);
        }
    }

    private void processesToTablesLjf() {
        // Ordenar por duración de proceso de mayor a menor
        procesosList.sort(Comparator.comparingInt(Proceso::getDuracion).reversed());

        for (Proceso proceso : procesosList) {
            if (proceso.getLlegada() <= currentTime) {
                proceso.setUbicacion("CPU");
                proceso.setEstado("X");
                procesosTablaList.add(proceso);
                fat.agregarProceso(proceso); // Asegúrate de que el proceso se agregue a FAT al llegar
            } else {
                proceso.setUbicacion("Memoria");
                proceso.setEstado("W");
            }
        }

        txtCPU.setText(procesosTablaList.isEmpty() ? "" : procesosTablaList.get(0).getProceso());
    }

    private void updateTablesLjf() {
        currentTime++;
        txtTiempo.setText(String.valueOf(currentTime));

        Proceso currentProcess = null;
        for (Proceso proceso : procesosTablaList) {
            if (proceso.getUbicacion().equals("CPU")) {
                proceso.disminuirDuracion();
                currentProcess = proceso;
                break;
            }
        }

        if (currentProcess != null && currentProcess.getDuracion() <= 0) {
            currentProcess.setUbicacion("Salida");
            currentProcess.setEstado("F");
            procesosSalidaList.add(currentProcess);
            procesosTablaList.remove(currentProcess);
        }

        for (Proceso proceso : procesosList) {
            if (proceso.getLlegada() == currentTime && !proceso.getUbicacion().equals("CPU") && !proceso.getUbicacion().equals("Salida")) {
                proceso.setUbicacion("Memoria");
                proceso.setEstado("W");
                procesosTablaList.add(proceso);
                fat.agregarProceso(proceso); // Asegúrate de que el proceso se agregue a FAT al llegar
            }
        }

        // Aquí se realiza la comparación y ordenación
        if (!procesosTablaList.isEmpty() && (currentProcess == null || currentProcess.getDuracion() <= 0)) {
            procesosTablaList.sort((p1, p2) -> {
                if (p1.getSize() != p2.getSize()) {
                    return Integer.compare(p2.getSize(), p1.getSize());
                } else {
                    return Integer.compare(p1.getLlegada(), p2.getLlegada());
                }
            });

            Proceso nextProcess = procesosTablaList.get(0);
            nextProcess.setUbicacion("CPU");
            nextProcess.setEstado("X");
            txtCPU.setText(nextProcess.getProceso());
        }

        tbvMemoria.refresh();
        tbvProcesos.refresh();
        tbvSalida.refresh();

        if (procesosList.size() == procesosSalidaList.size()) {
            detenerTimer();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Proceso terminado");
            alert.setHeaderText(null);
            alert.setContentText("Todos los procesos han terminado");
            alert.showAndWait();
            btnLimpiar.setDisable(false);
            exportFile(procesosSalidaList, "LJF");
        }
    }

    private void startRoundRobinFIFO() {
        if (tbvEntrada.getItems().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error");
            alert.setContentText("Debe ingresar al menos un proceso");
            alert.showAndWait();
        } else {
            btnIniciar.setDisable(true);
            btnLimpiar.setDisable(true);
            clearTables();
            currentTime = 0;

            String quantumText = txtQuantum.getText();
            if (quantumText.isEmpty()) {
                quantum = 3;
            } else {
                try {
                    quantum = Integer.parseInt(quantumText);
                } catch (NumberFormatException e) {
                    quantum = 3;
                }
            }

            quantumRemaining = quantum;
            txtQuantum.setText(String.valueOf(quantumRemaining));
            processesToTablesRoundRobinFIFO();

            timer = new Timer(true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> updateTablesRoundRobinFIFO());
                }
            }, 0, 1000);
        }
    }

    private void updateTablesRoundRobinFIFO() {
        currentTime++;
        txtTiempo.setText(String.valueOf(currentTime));

        for (Proceso proceso : procesosList) {
            if (proceso.getLlegada() == currentTime && !procesosTablaList.contains(proceso) && !proceso.getUbicacion().equals("Salida")) {
                proceso.setUbicacion("Memoria");
                proceso.setEstado("W");
                procesosTablaList.add(proceso);
                fat.agregarProceso(proceso); // Asegúrate de que el proceso se agregue a FAT al llegar
            }
        }

        if (!procesosTablaList.isEmpty()) {
            Proceso currentProcess = procesosTablaList.get(0);

            if (!currentProcess.getUbicacion().equals("CPU")) {
                currentProcess.setUbicacion("CPU");
                currentProcess.setEstado("X");
            }

            currentProcess.disminuirDuracion();
            quantumRemaining--;
            txtQuantum.setText(String.valueOf(quantumRemaining)); // Actualizar el quantum restante en el txtQuantum

            if (currentProcess.getDuracion() <= 0) {
                currentProcess.setUbicacion("Salida");
                currentProcess.setEstado("F");
                procesosSalidaList.add(currentProcess);
                procesosTablaList.remove(currentProcess);
                quantumRemaining = quantum; // Reiniciar el quantum para el siguiente proceso
            } else if (quantumRemaining <= 0) {
                currentProcess.setUbicacion("Memoria");
                currentProcess.setEstado("B"); // Marcar el proceso como bloqueado
                procesosTablaList.remove(currentProcess);
                procesosTablaList.add(currentProcess); // Mover el proceso al final de la lista de memoria
                quantumRemaining = quantum; // Reiniciar el quantum para el siguiente proceso
            }

            if (!procesosTablaList.isEmpty()) {
                Proceso nextProcess = procesosTablaList.get(0);
                nextProcess.setUbicacion("CPU");
                nextProcess.setEstado("X");
                txtCPU.setText(nextProcess.getProceso());
            } else {
                txtCPU.setText("");
            }

            tbvMemoria.refresh();
            tbvProcesos.refresh();
            tbvSalida.refresh();
        }

        if (procesosList.size() == procesosSalidaList.size()) {
            detenerTimer();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Proceso terminado");
            alert.setHeaderText(null);
            alert.setContentText("Todos los procesos han terminado");
            alert.showAndWait();
            btnLimpiar.setDisable(false);
            exportFile(procesosSalidaList, "RR_FIFO");
        }
    }

    private void processesToTablesRoundRobinFIFO() {
        procesosList.sort(Comparator.comparingInt(Proceso::getLlegada));

        for (Proceso proceso : procesosList) {
            if (proceso.getLlegada() <= currentTime) {
                proceso.setUbicacion("CPU");
                proceso.setEstado("X");
                procesosTablaList.add(proceso);
                fat.agregarProceso(proceso); // Asegúrate de que el proceso se agregue a FAT al llegar
            } else {
                proceso.setUbicacion("Memoria");
                proceso.setEstado("W");
            }
        }
        txtCPU.setText(procesosTablaList.isEmpty() ? "" : procesosTablaList.get(0).getProceso());
    }

    private void startRoundRobinLIFO() {
        if (tbvEntrada.getItems().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error");
            alert.setContentText("Debe ingresar al menos un proceso");
            alert.showAndWait();
        } else {
            btnIniciar.setDisable(true);
            btnLimpiar.setDisable(true);
            clearTables();
            currentTime = 0;

            String quantumText = txtQuantum.getText();
            if (quantumText.isEmpty()) {
                quantum = 3;
            } else {
                try {
                    quantum = Integer.parseInt(quantumText);
                } catch (NumberFormatException e) {
                    quantum = 3;
                }
            }

            quantumRemaining = quantum;
            txtQuantum.setText(String.valueOf(quantumRemaining));
            processesToTablesRoundRobinLIFO();

            timer = new Timer(true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> updateTablesRoundRobinLIFO());
                }
            }, 0, 1000);
        }
    }

    private void updateTablesRoundRobinLIFO() {
        currentTime++;
        txtTiempo.setText(String.valueOf(currentTime));

        Proceso currentProcess = null;
        for (Proceso proceso : procesosTablaList) {
            if (proceso.getUbicacion().equals("CPU")) {
                proceso.disminuirDuracion();
                quantumRemaining--;
                txtQuantum.setText(String.valueOf(quantumRemaining)); // Actualizar el quantum restante en el txtQuantum
                currentProcess = proceso;
                break;
            }
        }

        for (Proceso proceso : procesosList) {
            if (proceso.getLlegada() == currentTime && !procesosTablaList.contains(proceso) && !proceso.getUbicacion().equals("Salida")) {
                proceso.setUbicacion("Memoria");
                proceso.setEstado("W");
                procesosTablaList.add(proceso);
                fat.agregarProceso(proceso); // Asegúrate de que el proceso se agregue a FAT al llegar
            }
        }

        if (currentProcess != null && currentProcess.getDuracion() <= 0) {
            currentProcess.setUbicacion("Salida");
            currentProcess.setEstado("F");
            procesosSalidaList.add(currentProcess);
            procesosTablaList.remove(currentProcess);
            quantumRemaining = quantum; // Reiniciar el quantum para el siguiente proceso
        } else if (quantumRemaining <= 0) {
            currentProcess.setUbicacion("Memoria");
            currentProcess.setEstado("B"); // Marcar el proceso como bloqueado
            procesosTablaList.remove(currentProcess);
            procesosTablaList.add(currentProcess); // Mover el proceso al final de la lista de memoria
            quantumRemaining = quantum; // Reiniciar el quantum para el siguiente proceso
        }

        for (Proceso proceso : procesosList) {
            if (proceso.getLlegada() == currentTime && !proceso.getUbicacion().equals("CPU") && proceso.getUbicacion().equals("Salida")) {
                proceso.setUbicacion("Memoria");
                proceso.setEstado("W");
                procesosTablaList.add(proceso);
            }
        }

        if (!procesosTablaList.isEmpty() && (currentProcess == null || currentProcess.getDuracion() <= 0)) {
            procesosTablaList.sort(Comparator.comparingInt(Proceso::getLlegada).reversed());
            Proceso nextProcess = procesosTablaList.get(0);
            nextProcess.setUbicacion("CPU");
            nextProcess.setEstado("X");
            txtCPU.setText(nextProcess.getProceso());
        }

        tbvMemoria.refresh();
        tbvProcesos.refresh();
        tbvSalida.refresh();

        if (procesosList.size() == procesosSalidaList.size()) {
            detenerTimer();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Proceso terminado");
            alert.setHeaderText(null);
            alert.setContentText("Todos los procesos han terminado");
            alert.showAndWait();
            btnLimpiar.setDisable(false);
            exportFile(procesosSalidaList, "RR_LIFO");
        }
    }

    private void processesToTablesRoundRobinLIFO() {
        procesosList.sort(Comparator.comparingInt(Proceso::getLlegada).reversed());

        for (Proceso proceso : procesosList) {
            if (proceso.getLlegada() <= currentTime) {
                proceso.setUbicacion("CPU");
                proceso.setEstado("X");
                procesosTablaList.add(proceso);
                fat.agregarProceso(proceso); // Asegúrate de que el proceso se agregue a FAT al llegar
            } else {
                proceso.setUbicacion("Memoria");
                proceso.setEstado("W");
            }
        }
        txtCPU.setText(procesosTablaList.isEmpty() ? "" : procesosTablaList.get(0).getProceso());
    }

    private void exportFile(List<Proceso> procesos, String metodo) {
        // Obtener la fecha y hora actual
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        String fechaHora = dateFormat.format(new Date());

        // Crear el nombre del archivo
        String nombreArchivo = metodo + "_" + fechaHora + ".txt";

        // Obtener el directorio del escritorio del usuario
        String directorioEscritorio = System.getProperty("user.home") + "/Desktop/Planificador SO/";

        // Crear el directorio si no existe
        File carpeta = new File(directorioEscritorio);
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }

        File archivo = new File(directorioEscritorio + nombreArchivo);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
            // Escribir el contenido del archivo
            for (Proceso proceso : procesos) {
                writer.write(proceso.toString());
                writer.newLine();
            }
            writer.write("\nTiempo total: " + currentTime);
            writer.newLine();
            writer.write(fat.toString());
            System.out.println("Archivo exportado correctamente: " + archivo.getAbsolutePath());
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Archivo exportado");
            alert.setHeaderText("El archivo se ha exportado correctamente.");
            ButtonType aceptar = new ButtonType("Aceptar");
            ButtonType verArchivo = new ButtonType("Ver archivo");
            alert.getButtonTypes().setAll(aceptar, verArchivo);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == verArchivo) {
                openDirectoryAndFile(archivo);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error al exportar el archivo: " + e.getMessage());
        }
    }

    private void openDirectoryAndFile(File file) {
        String folderPath = file.getParent();
        String filePath = file.getAbsolutePath();
        String[] commands = {"explorer.exe", "/select,", filePath};
        try {
            Runtime.getRuntime().exec(commands, null, new File(folderPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
