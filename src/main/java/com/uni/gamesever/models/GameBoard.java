package com.uni.gamesever.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameBoard {
    private BoardSize size;
    private Tile[][] tiles;

    private GameBoard(BoardSize size) {
        this.size = size;
        this.tiles = new Tile[size.getRows()][size.getCols()];
    }
    public BoardSize getSize() {
        return size;
    }
    public Tile[][] getTiles() {
        return tiles;
    }
    public void setTile(int row, int col, Tile tile) {
        this.tiles[row][col] = tile;
    }

    // Tile generation and assignment
    public static GameBoard generateBoard(BoardSize size) {
        GameBoard board = new GameBoard(size);
        int rows = size.getRows();
        int cols = size.getCols();

        //ecken generieren
        board.setTile(0, 0, new Tile(List.of("RIGHT", "DOWN"), "CORNER"));
        board.setTile(0, cols - 1, new Tile(List.of("LEFT", "DOWN"), "CORNER"));
        board.setTile(rows - 1, 0, new Tile(List.of("UP", "RIGHT"), "CORNER"));
        board.setTile(rows - 1, cols - 1, new Tile(List.of("UP", "LEFT"), "CORNER"));


        //Randkreuzungen generieren
        for(int i = 0; i < rows; i++){
            for (int j = 0; j < cols; j++){
                if(i%2 == 0 && j % 2 == 0){
                    boolean isEdge = (i == 0 || j == 0 || i == rows -1 || j == cols -1);
                    if(isEdge && board.getTiles()[i][j] == null){
                        List<String> entrances = generateEdgeCrossEntrances(i, j, rows, cols);
                        Tile t = new Tile(entrances, "CROSS");
                        board.setTile(i, j, t);
                    }
                }
            }
        }

        // Innenkreuzungen generieren
        for (int i = 2; i < rows - 1; i += 2) {
            for (int j = 2; j < cols - 1; j += 2) {
                if (board.getTiles()[i][j] == null) {
                    Tile t = new Tile(generateEntrancesForTypeWithRandomRotation("CROSS"), "CROSS");
                    board.setTile(i, j, t);
                }
            }
        }

        // Restliche Tiles zufällig füllen
        fillRandomTiles(board);
        return board;
    }

    private static void fillRandomTiles(GameBoard board) {
        int rows = board.getSize().getRows();
        int cols = board.getSize().getCols();
        int totalTiles = rows * cols;
        int totalCards = totalTiles + 1; // +1 for the spare tile

        //basiswerte aus 7x7 Board
        int baseRows = 7;
        int baseCols = 7;
        int baseCorners = 20;
        int baseCrosses = 18;
        int baseStraights = 12;

        // Skalierungsfaktoren
        int corners = totalCards * baseCorners / (baseRows * baseCols + 1);
        int crosses = totalCards * baseCrosses / (baseRows * baseCols + 1);
        int straights = totalCards * baseStraights / (baseRows * baseCols + 1);

        // rundungsfehler ausgleichen
        int sum = corners + crosses + straights;
        int diff = totalCards - sum;
        if (diff > 0) straights += diff;

        // bereits gesetzte tiles zählen und die anzahl reduzieren
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Tile t = board.getTiles()[i][j];
                if (t != null && t.getType() != null) {
                    switch (t.getType()) {
                        case "CORNER":
                            corners--;
                            break;
                        case "CROSS":
                            crosses--;
                            break;
                        case "STRAIGHT":
                            straights--;
                            break;
                    }
                }
            }
        }

        //Restkarten in eine Liste packen
        List<String> remainingTiles = new ArrayList<>();
        for (int i = 0; i < corners; i++) remainingTiles.add("CORNER");
        for (int i = 0; i < crosses; i++) remainingTiles.add("CROSS");
        for (int i = 0; i < straights; i++) remainingTiles.add("STRAIGHT");

        Collections.shuffle(remainingTiles);

        //Restkarten zuweisen
        int index = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board.getTiles()[i][j] == null && index < remainingTiles.size()) {
                    String type = remainingTiles.get(index++);
                    Tile t = new Tile(generateEntrancesForTypeWithRandomRotation(type), type);
                    board.setTile(i, j, t);
                }
            }
        }
    }

    private static List<String> generateEdgeCrossEntrances(int row, int col, int rows, int cols) {
        List<String> entrances = new ArrayList<>();

        boolean topEdge = (row == 0);
        boolean bottomEdge = (row == rows - 1);
        boolean leftEdge = (col == 0);
        boolean rightEdge = (col == cols - 1);

        List<String> allDirs = List.of("UP", "RIGHT", "DOWN", "LEFT");
        entrances.addAll(allDirs);

        if (topEdge) entrances.remove("UP");
        if (bottomEdge) entrances.remove("DOWN");
        if (leftEdge) entrances.remove("LEFT");
        if (rightEdge) entrances.remove("RIGHT");

   
        if (entrances.size() < 3) {
            if (topEdge) entrances.add("DOWN");
            if (bottomEdge) entrances.add("UP");
            if (leftEdge) entrances.add("RIGHT");
            if (rightEdge) entrances.add("LEFT");
        }

        return entrances;
    }


    private static List<String> generateEntrancesForTypeWithRandomRotation(String type) {
    Random rnd = new Random();
        switch (type) {
            case "CORNER":
            List<List<String>> corners = List.of(
                List.of("UP", "RIGHT"),
                List.of("RIGHT", "DOWN"),
                List.of("DOWN", "LEFT"),
                List.of("LEFT", "UP")
            );
            return new ArrayList<>(corners.get(rnd.nextInt(corners.size())));

        case "STRAIGHT":
            List<List<String>> straights = List.of(
                List.of("UP", "DOWN"),
                List.of("LEFT", "RIGHT")
            );
            return new ArrayList<>(straights.get(rnd.nextInt(straights.size())));

        case "CROSS":
            List<List<String>> crosses = List.of(
                List.of("UP", "LEFT", "RIGHT"),
                List.of("UP", "RIGHT", "DOWN"),
                List.of("RIGHT", "DOWN", "LEFT"),
                List.of("DOWN", "LEFT", "UP")
            );
            return new ArrayList<>(crosses.get(rnd.nextInt(crosses.size())));

        default:
            return new ArrayList<>();
    }


}



    //DEMO 
    public static void printBoard(GameBoard board) {
    Tile[][] tiles = board.getTiles();
    int rows = board.getSize().getRows();
    int cols = board.getSize().getCols();

    System.out.println("=== GAME BOARD VISUALIZATION ===");
    for (int i = 0; i < rows; i++) {
        for (int j = 0; j < cols; j++) {
            Tile t = tiles[i][j];
            if (t == null) {
                System.out.print(" - ");
            } else {
                System.out.print(String.format("%-3s", getTileSymbol(t)));
            }
        }
        System.out.println();
    }
}


private static String getTileSymbol(Tile t) {
    List<String> e = t.getEntrances();
    if (e == null || e.isEmpty()) return " ? ";

    // Ecken
    if (e.size() == 2 && e.contains("UP") && e.contains("RIGHT")) return "└──";
    if (e.size() == 2 && e.contains("RIGHT") && e.contains("DOWN")) return "┌──";
    if (e.size() == 2 && e.contains("DOWN") && e.contains("LEFT")) return "──┐";
    if (e.size() == 2 && e.contains("LEFT") && e.contains("UP")) return "──┘";

    // Gerade
    if (e.size() == 2 && e.contains("LEFT") && e.contains("RIGHT")) return "───";
    if (e.size() == 2 && e.contains("UP") && e.contains("DOWN")) return " │ ";

    // T-Stücke
    if (e.size() == 3) {
        if (!e.contains("UP")) return "╦──";      
        if (!e.contains("RIGHT")) return "╣ │";
        if (!e.contains("DOWN")) return "╩──";
        if (!e.contains("LEFT")) return "╠ │";
    }


    // Fallback
    return " ? ";
}


}
