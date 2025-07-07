package com.example.app;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

@Route("")
@PermitAll
public class MainView extends VerticalLayout {

    private List<String> players = new ArrayList<>();
    private Grid<String> grid = new Grid<>();

    public MainView() {
        TextField nameField = new TextField("Imię zawodnika");
        Button addButton = new Button("Dodaj zawodnika", e -> {
            if (!nameField.getValue().isEmpty()) {
                players.add(nameField.getValue());
                grid.setItems(players);
                nameField.clear();
            }
        });

        NumberField courtCount = new NumberField("Liczba kortów");

        Button startButton = new Button("Start losowania", e -> {
            // Tu logika losowania, na razie pusta
        });

        grid.addColumn(name -> name).setHeader("Zawodnicy");

        add(nameField, addButton, courtCount, startButton, grid);
    }
}