package com.so.planificadores_so;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        new Planificador();
    }

    public static void main(String[] args) {
        launch();
    }
}