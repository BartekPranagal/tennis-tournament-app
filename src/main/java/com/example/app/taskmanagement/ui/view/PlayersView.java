package com.example.app.taskmanagement.ui.view;

import com.example.app.players.entity.Player;
import com.example.app.players.service.PlayerService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

@Route("zawodnicy")
@PermitAll
public class PlayersView extends VerticalLayout {

    private final PlayerService playerService;
    private final Grid<Player> playersGrid = new Grid<>(Player.class, false);

    @Autowired
    public PlayersView(PlayerService playerService) {
        this.playerService = playerService;

        // ---- PRZYCISK DO TURNIEJU ----
        Button goToTournament = new Button("Przejdź do turnieju", event ->
                getUI().ifPresent(ui -> ui.navigate("turniej"))
        );
        goToTournament.getStyle().set("background", "#2196f3").set("color", "white");
        add(goToTournament);

        TextField playerNameField = new TextField("Imię zawodnika");
        Button addButton = new Button("Dodaj zawodnika");

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

// Dodaj nasłuchiwanie na ENTER:
        playerNameField.addKeyPressListener(Key.ENTER, event -> addPlayerAction.run());
        Button clearButton = new Button("Wyczyść wszystkich");
        clearButton.addClickListener(e -> {
            playerService.clearPlayers();
            refreshGrid();
        });

        playersGrid.addColumn(Player::getName).setHeader("Zawodnik");
        playersGrid.addComponentColumn(player -> {
            Button removeBtn = new Button("Usuń", evt -> {
                playerService.removePlayer(player.getName());
                refreshGrid();
            });
            removeBtn.getStyle().set("color", "red");
            return removeBtn;
        }).setHeader("Usuń");
        playersGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        playersGrid.setWidthFull();

        HorizontalLayout form = new HorizontalLayout(playerNameField, addButton, clearButton);
        form.setAlignItems(Alignment.END);

        add(form, playersGrid);

        refreshGrid();
    }

    private void refreshGrid() {
        playersGrid.setItems(playerService.getPlayers());
    }
}
