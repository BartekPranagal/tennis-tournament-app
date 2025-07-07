package com.example.app.taskmanagement.ui.view;

import com.example.app.players.domain.Match;
import com.example.app.players.domain.TournamentRound;
import com.example.app.players.entity.Player;
import com.example.app.players.service.PlayerService;
import com.example.app.players.service.TournamentService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
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
        this.playerService = playerService;
        this.tournamentService = tournamentService;

        setWidthFull();
        setSpacing(false);
        setPadding(false);

        // --- Lewy panel (konfiguracja) ---
        Button backButton = new Button("Powrót do zawodników", e ->
                getUI().ifPresent(ui -> ui.navigate("zawodnicy"))
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
        playersListBox.addValueChangeListener(e -> {
            Set<Player> allPlayers = playersListBox.getListDataView().getItems().collect(Collectors.toSet());
            Set<Player> selected = new HashSet<>(playersListBox.getSelectedItems());
            if (selected.containsAll(allPlayers) && selected.size() == allPlayers.size()) {
                if (!selectAll.getValue()) selectAll.setValue(true);
            } else {
                if (selectAll.getValue()) selectAll.setValue(false);
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

        VerticalLayout configPanel = new VerticalLayout(
                backButton,
                playersSelectRow,
                playersListBox,
                courtsField,
                generateButton
        );
        configPanel.setPadding(true);
        configPanel.setWidth("280px");
        configPanel.setAlignItems(Alignment.START);

        // --- Grid live tabeli wyników ---
        scoreGrid.addColumn(PlayerScore::getPlayerName).setHeader("Zawodnik");
        scoreGrid.addColumn(PlayerScore::getPoints).setHeader("Punkty");
        scoreGrid.addColumn(PlayerScore::getPauses).setHeader("Pauzy");
        scoreGrid.setWidth("350px");
        scoreGrid.setAllRowsVisible(true);
        scoreGrid.getStyle().set("margin-left", "auto").set("margin-right", "auto");

        // --- Panel centralny: tabela live + rundy w gridzie ---
        roundsGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(4, 1fr)")
                .set("gap", "24px")
                .set("margin-top", "24px")
                .set("width", "100%");

        // Wrapper do centrowania tabeli live
        VerticalLayout tabelaLiveWrapper = new VerticalLayout();
        tabelaLiveWrapper.setAlignItems(Alignment.CENTER);
        tabelaLiveWrapper.setSpacing(false);
        tabelaLiveWrapper.setPadding(false);

        H3 tabelaNaglowek = new H3("Live tabela wyników");
        tabelaNaglowek.getStyle().set("margin-bottom", "0.2em").set("margin-top", "0.2em");
        tabelaLiveWrapper.add(tabelaNaglowek, scoreGrid);

        // Wrapper do centrowania rund i nagłówka
        VerticalLayout rundyWrapper = new VerticalLayout();
        rundyWrapper.setAlignItems(Alignment.CENTER);
        rundyWrapper.setSpacing(false);
        rundyWrapper.setPadding(false);

        H3 rozpiskaNaglowek = new H3("Rozpiska rund");
        rozpiskaNaglowek.getStyle().set("margin-bottom", "0.5em").set("margin-top", "0.5em");
        rundyWrapper.add(rozpiskaNaglowek, roundsGrid);

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
            VerticalLayout rundaLayout = new VerticalLayout();
            rundaLayout.setWidth("420px"); // szeroko, możesz dać np. 480px jeśli masz bardzo długie nazwiska
            rundaLayout.getStyle()
                    .set("border", "1px solid #f2f2f2")
                    .set("border-radius", "14px")
                    .set("padding", "16px")
                    .set("background", "#fafbfc");
            rundaLayout.setSpacing(false);
            rundaLayout.add(new H3("Runda #" + runda));
            for (Match match : round.getMatches()) {
                if (match.getPlayer1() != null && match.getPlayer2() != null) {
                    // [imię1] [wynik1] : [wynik2] [imię2]
                    HorizontalLayout matchLayout = new HorizontalLayout();
                    matchLayout.setWidthFull();
                    matchLayout.setAlignItems(Alignment.CENTER);

                    Span name1 = new Span(match.getPlayer1().getName());
                    name1.getStyle().set("font-weight", "500").set("margin-right", "8px").set("min-width", "70px");

                    IntegerField score1 = new IntegerField();
                    score1.setWidth("48px");
                    score1.setValue(match.getScore1());
                    score1.getStyle().set("margin-right", "8px");

                    Span colon = new Span(":");
                    colon.getStyle().set("font-weight", "bold").set("margin", "0 8px");

                    IntegerField score2 = new IntegerField();
                    score2.setWidth("48px");
                    score2.setValue(match.getScore2());
                    score2.getStyle().set("margin-left", "8px");

                    Span name2 = new Span(match.getPlayer2().getName());
                    name2.getStyle().set("font-weight", "500").set("margin-left", "8px").set("min-width", "70px");

                    score1.addValueChangeListener(e -> {
                        match.setScore1(e.getValue());
                        updateLiveScores();
                    });
                    score2.addValueChangeListener(e -> {
                        match.setScore2(e.getValue());
                        updateLiveScores();
                    });

                    matchLayout.add(name1, score1, colon, score2, name2);
                    rundaLayout.add(matchLayout);

                } else if (match.getPlayer1() != null && match.getPlayer2() == null) {
                    // Pauzuje player1 — [nazwa]     PRZERWA
                    HorizontalLayout pauseLayout = new HorizontalLayout();
                    pauseLayout.setWidthFull();
                    pauseLayout.setAlignItems(Alignment.CENTER);

                    Span name = new Span(match.getPlayer1().getName());
                    name.getStyle()
                            .set("font-weight", "500")
                            .set("margin-right", "24px")
                            .set("min-width", "110px");

                    Span przerwa = new Span("PRZERWA");
                    przerwa.getStyle()
                            .set("background", "#f7f7f7")
                            .set("color", "#455")
                            .set("font-weight", "bold")
                            .set("padding", "6px 38px")
                            .set("border-radius", "8px")
                            .set("display", "inline-block")
                            .set("text-align", "center")
                            .set("font-size", "15px");

                    pauseLayout.add(name, przerwa);
                    rundaLayout.add(pauseLayout);
                    playerScores.get(match.getPlayer1()).addPause();

                } else if (match.getPlayer2() != null && match.getPlayer1() == null) {
                    // Pauzuje player2 — [nazwa]     PRZERWA
                    HorizontalLayout pauseLayout = new HorizontalLayout();
                    pauseLayout.setWidthFull();
                    pauseLayout.setAlignItems(Alignment.CENTER);

                    Span name = new Span(match.getPlayer2().getName());
                    name.getStyle()
                            .set("font-weight", "500")
                            //.set("margin-right", "10px")
                            .set("min-width", "110px");

                    Span przerwa = new Span("PRZERWA");
                    przerwa.getStyle()
                            .set("background", "#f7f7f7")
                            .set("color", "#455")
                            .set("font-weight", "bold")
                            .set("padding", "6px 38px")
                            .set("border-radius", "8px")
                            .set("display", "inline-block")
                            .set("text-align", "center")
                            .set("font-size", "15px");

                    pauseLayout.add(name, przerwa);
                    rundaLayout.add(pauseLayout);
                    playerScores.get(match.getPlayer2()).addPause();
                }
            }
            roundsGrid.add(rundaLayout);
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