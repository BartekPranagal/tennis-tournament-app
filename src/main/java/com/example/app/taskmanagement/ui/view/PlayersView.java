package com.example.app.taskmanagement.ui.view;

import com.example.app.players.entity.Player;
import com.example.app.players.service.PlayerService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

@Route("")
@PermitAll
public class PlayersView extends VerticalLayout {

    private final PlayerService playerService;
    private final Grid<Player> playersGrid = new Grid<>(Player.class, false);

    @Autowired
    public PlayersView(PlayerService playerService) {
        addClassName("app-background");
        this.playerService = playerService;

        // Glassowe tło na całość
        // --- Glassowy wrapper ---
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.addClassName("players-table-glass");
       // wrapper.setWidth("480px");
        wrapper.setPadding(true);
        wrapper.setSpacing(true);
        wrapper.setAlignItems(Alignment.CENTER);

        // ---- PRZYCISK DO TURNIEJU ----
        Button goToTournament = new Button("Przejdź do turnieju", event ->
                getUI().ifPresent(ui -> ui.navigate("turniej"))
        );
        goToTournament.getStyle()
                .set("background", "linear-gradient(95deg, #2563eb 60%, #2563ebc7 100%)")
                .set("color", "white")
                .set("font-weight", "600")
                .set("border-radius", "10px")
                .set("padding", "10px 20px")
                .set("box-shadow", "0 2px 11px rgba(30, 64, 165, 0.13)");

        // ---- FORMULARZ DODAWANIA ----
        TextField playerNameField = new TextField();
        playerNameField.setPlaceholder("Imię zawodnika");
        playerNameField.setClearButtonVisible(true);

        Button addButton = new Button("Dodaj zawodnika");
        addButton.getStyle()
                .set("background", "linear-gradient(95deg, #2563eb 60%, #2563ebc7 100%)")
                .set("color", "white")
                .set("font-weight", "600")
                .set("border-radius", "10px")
                .set("padding", "8px 18px")
                .set("box-shadow", "0 2px 11px rgba(30, 64, 165, 0.12)");

        Button clearButton = new Button("Wyczyść wszystkich");
        clearButton.getStyle()
                .set("background", "white")
                .set("color", "#245")
                .set("border-radius", "10px")
                .set("font-weight", "600")
                .set("border", "1px solid #a3cafe")
                .set("padding", "8px 18px");

        // Akcja dodawania zawodnika (użyta dwa razy)
        Runnable addPlayerAction = () -> {
            String name = playerNameField.getValue();
            if (name == null || name.trim().isEmpty()) {
                Notification.show("Wpisz imię!");
                return;
            }
            boolean added = playerService.addPlayer(name);
            if (!added) {
                Notification.show("Zawodnik już istnieje lub imię jest niepoprawne!");
                return;
            }
            refreshGrid();
            playerNameField.clear();
        };

        addButton.addClickListener(event -> addPlayerAction.run());
        playerNameField.addKeyPressListener(Key.ENTER, event -> addPlayerAction.run());

        clearButton.addClickListener(e -> {
            playerService.clearPlayers();
            refreshGrid();
        });

        HorizontalLayout form = new HorizontalLayout(playerNameField, addButton, clearButton);
        form.setAlignItems(Alignment.END);
        form.setSpacing(true);
        form.setWidthFull();

        // ---- NAGŁÓWEK ----
        H3 header = new H3("Lista zawodników");
        header.getStyle()
                .set("color", "#18497b")
                .set("margin-bottom", "0.5em")
                .set("font-weight", "700")
                .set("font-size", "1.3em");

        // ---- GRID ----
        playersGrid.addColumn(Player::getName).setHeader("Zawodnik");
        playersGrid.addComponentColumn(player -> {
            Button removeBtn = new Button("Usuń", evt -> {
                playerService.removePlayer(player.getName());
                refreshGrid();
            });
            removeBtn.getStyle()
                    .set("color", "white")
                    .set("background", "#ef4444")
                    .set("font-weight", "600")
                    .set("border-radius", "8px")
                    .set("padding", "6px 14px")
                    .set("box-shadow", "0 1px 4px rgba(239,68,68,0.13)");
            return removeBtn;
        }).setHeader("Usuń");
        playersGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        playersGrid.setWidthFull();
        playersGrid.addClassName("v-grid");
        playersGrid.getStyle().set("margin-top", "12px");

        wrapper.add(goToTournament, header, form, playersGrid);

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        add(wrapper);

        refreshGrid();
    }

    private void refreshGrid() {
        playersGrid.setItems(playerService.getPlayers());
    }
}