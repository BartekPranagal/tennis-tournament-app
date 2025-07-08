package com.example.app.taskmanagement.ui.view;

import com.example.app.players.domain.Match;
import com.example.app.players.domain.TournamentRound;
import com.example.app.players.entity.Player;
import com.example.app.players.service.PlayerService;
import com.example.app.players.service.TournamentService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

@Route("turniej")
@PermitAll
@CssImport("./themes/default/background.css")
@CssImport("./themes/default/styles.css")
public class TournamentView extends HorizontalLayout {

    private final PlayerService playerService;
    private final TournamentService tournamentService;
    private final MultiSelectListBox<Player> playersListBox = new MultiSelectListBox<>();
    private final IntegerField courtsField = new IntegerField("Liczba kortów");
    private final Div roundsGrid = new Div();
    private final Grid<PlayerScore> scoreGrid = new Grid<>(PlayerScore.class, false);

    private List<TournamentRound> tournamentRounds;
    private Map<Player, PlayerScore> playerScores = new HashMap<>();

    @Autowired
    public TournamentView(PlayerService playerService, TournamentService tournamentService) {
        addClassName("app-background");
        this.playerService = playerService;
        this.tournamentService = tournamentService;

        setWidthFull();
        setSpacing(false);
        setPadding(false);

        // --- Lewy panel (konfiguracja) ---
        Button backButton = new Button("Powrót do zawodników", e ->
                getUI().ifPresent(ui -> ui.navigate(""))
        );
        backButton.getStyle().set("margin-bottom", "16px");

        playersListBox.setItems(playerService.getPlayers());
        playersListBox.setItemLabelGenerator(Player::getName);
        playersListBox.setWidth("250px");

        // Checkbox "Zaznacz wszystkich" + synchronizacja dwustronna
        Checkbox selectAll = new Checkbox("Zaznacz wszystkich");
        selectAll.addValueChangeListener(e -> {
            if (e.getValue()) {
                playersListBox.select(playersListBox.getListDataView().getItems().collect(Collectors.toSet()));
            } else {
                playersListBox.clear();
            }
        });

        courtsField.setMin(1);
        courtsField.setStep(1);
        courtsField.setValue(1);
        courtsField.setWidth("120px");

        Button generateButton = new Button("Losuj turniej", event -> generateTournament());

        // Layout do wyboru zawodników + checkboxa
        HorizontalLayout playersSelectRow = new HorizontalLayout(new H3("Wybierz zawodników"), selectAll);
        playersSelectRow.setAlignItems(Alignment.BASELINE);
        playersSelectRow.setSpacing(true);

        VerticalLayout configPanel = new VerticalLayout(
                backButton,
                playersSelectRow,
                playersListBox,
                courtsField,
                generateButton
        );
        configPanel.addClassName("left-panel-glass");
        configPanel.setPadding(false);
        configPanel.setWidth("300px");
        configPanel.setAlignItems(Alignment.START);
        configPanel.setSpacing(false);

        // --- Grid live tabeli wyników ---
        scoreGrid.addColumn(PlayerScore::getPlayerName).setHeader("Zawodnik");
        scoreGrid.addColumn(PlayerScore::getPoints).setHeader("Punkty");
        scoreGrid.addColumn(PlayerScore::getPauses).setHeader("Pauzy");
        scoreGrid.setWidthFull();
        scoreGrid.setAllRowsVisible(true);
        scoreGrid.addClassName("v-grid");

        // --- Panel centralny: tabela live + rundy w gridzie ---
        roundsGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(2, 1fr)")
                .set("gap", "24px")
                .set("margin-top", "24px")
                .set("width", "100%");

        // Wrapper do centrowania tabeli live
        VerticalLayout tabelaLiveWrapper = new VerticalLayout();
        tabelaLiveWrapper.setAlignItems(Alignment.START);
        tabelaLiveWrapper.setSpacing(false);
        tabelaLiveWrapper.setPadding(false);
        tabelaLiveWrapper.addClassName("live-table-glass");

        H3 tabelaNaglowek = new H3("Live tabela wyników");
        tabelaNaglowek.addClassName("fancy-header");
        tabelaNaglowek.getStyle().set("margin-bottom", "0.2em").set("margin-top", "0.2em");
        tabelaLiveWrapper.add(tabelaNaglowek, scoreGrid);

        // Wrapper do centrowania rund i nagłówka
        VerticalLayout rundyWrapper = new VerticalLayout();
        rundyWrapper.setAlignItems(Alignment.CENTER);
        rundyWrapper.setSpacing(false);
        rundyWrapper.setPadding(false);

        Div headerBg = new Div();
        headerBg.addClassName("fancy-header-bg");
        H3 rozpiskaNaglowek = new H3("Rozpiska rund");
        rozpiskaNaglowek.addClassName("fancy-header");
        headerBg.add(rozpiskaNaglowek);
        rozpiskaNaglowek.getStyle().set("margin-bottom", "0.5em").set("margin-top", "0.5em");
        rundyWrapper.add(headerBg, roundsGrid);

        // Panel centralny – całość na środku
        VerticalLayout centerPanel = new VerticalLayout(
                tabelaLiveWrapper,
                rundyWrapper
        );
        centerPanel.setWidthFull();
        centerPanel.setAlignItems(Alignment.CENTER);
        centerPanel.setSpacing(false);

        add(configPanel, centerPanel);
    }

    private void generateTournament() {
        List<Player> selected = new ArrayList<>(playersListBox.getSelectedItems());
        int courts = courtsField.getValue() != null ? courtsField.getValue() : 1;
        if (selected.size() < 2) {
            Notification.show("Wybierz min. 2 zawodników");
            return;
        }
        if (courts < 1) {
            Notification.show("Kortów musi być min. 1");
            return;
        }

        // generate rounds
        tournamentRounds = tournamentService.generateRoundRobinTournament(selected, courts);

        // init live scores
        playerScores = selected.stream().collect(Collectors.toMap(
                p -> p, p -> new PlayerScore(p.getName())
        ));
        refreshView();
    }

    private void refreshView() {
        roundsGrid.removeAll();
        int runda = 1;

        // Wyzeruj pauzy
        for (PlayerScore score : playerScores.values()) {
            score.setPauses(0);
        }

        for (TournamentRound round : tournamentRounds) {
            // Utwórz box rundy jako grid
            Div rundaGrid = new Div();
            rundaGrid.addClassName("runda-box-table");
            rundaGrid.add(new H3("Runda #" + runda) {{
                getStyle().set("grid-column", "1 / span 5")
                        .set("margin-bottom", "10px")
                        .set("margin-top", "2px");
            }});

            for (Match match : round.getMatches()) {
                if (match.getPlayer1() != null && match.getPlayer2() != null) {
                    // Gracz 1
                    Span name1 = new Span(match.getPlayer1().getName());
                    name1.addClassName("player-name");

                    // Wynik 1
                    IntegerField score1 = new IntegerField();
                    score1.setValue(match.getScore1());
                    score1.addClassName("score-field");

                    // Dwukropek
                    Span colon = new Span(":");
                    colon.addClassName("colon");

                    // Wynik 2
                    IntegerField score2 = new IntegerField();
                    score2.setValue(match.getScore2());
                    score2.addClassName("score-field");

                    // Gracz 2
                    Span name2 = new Span(match.getPlayer2().getName());
                    name2.addClassName("player-name");

                    // Eventy
                    score1.addValueChangeListener(e -> {
                        match.setScore1(e.getValue());
                        updateLiveScores();
                    });
                    score2.addValueChangeListener(e -> {
                        match.setScore2(e.getValue());
                        updateLiveScores();
                    });

                    rundaGrid.add(name1, score1, colon, score2, name2);

                } else if (match.getPlayer1() != null && match.getPlayer2() == null) {
                    // Pauza gracz1
                    Span pause = new Span(match.getPlayer1().getName() + "  PRZERWA");
                    pause.addClassName("pause");
                    rundaGrid.add(pause);

                    playerScores.get(match.getPlayer1()).addPause();

                } else if (match.getPlayer2() != null && match.getPlayer1() == null) {
                    // Pauza gracz2
                    Span pause = new Span(match.getPlayer2().getName() + "  PRZERWA");
                    pause.addClassName("pause");
                    rundaGrid.add(pause);

                    playerScores.get(match.getPlayer2()).addPause();
                }
            }
            roundsGrid.add(rundaGrid);
            runda++;
        }
        updateLiveScores();
    }

    private void updateLiveScores() {
        // Resetujemy punkty (pauzy już aktualizuje refreshView)
        for (PlayerScore score : playerScores.values()) {
            score.setPoints(0);
        }
        // Sumujemy wpisane wyniki
        for (TournamentRound round : tournamentRounds) {
            for (Match match : round.getMatches()) {
                if (match.getPlayer1() != null && match.getScore1() != null) {
                    playerScores.get(match.getPlayer1()).addPoints(match.getScore1());
                }
                if (match.getPlayer2() != null && match.getScore2() != null) {
                    playerScores.get(match.getPlayer2()).addPoints(match.getScore2());
                }
            }
        }
        // Sortujemy malejąco po punktach
        List<PlayerScore> sortedScores = playerScores.values().stream()
                .sorted(Comparator.comparingInt(PlayerScore::getPoints).reversed())
                .collect(Collectors.toList());
        scoreGrid.setItems(sortedScores);
    }

    // --- klasy pomocnicze ---

    public static class PlayerScore {
        private String playerName;
        private int points;
        private int pauses;
        public PlayerScore(String name) { this.playerName = name; this.points = 0; this.pauses = 0; }
        public String getPlayerName() { return playerName; }
        public int getPoints() { return points; }
        public void setPoints(int points) { this.points = points; }
        public void addPoints(int pts) { this.points += pts; }
        public int getPauses() { return pauses; }
        public void setPauses(int pauses) { this.pauses = pauses; }
        public void addPause() { this.pauses++; }
    }
}