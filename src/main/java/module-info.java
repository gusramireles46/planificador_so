module com.so.planificadores_so {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;

    opens com.so.planificadores_so to javafx.fxml;
    exports com.so.planificadores_so;
}