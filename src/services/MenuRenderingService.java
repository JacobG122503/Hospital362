package services;

import java.util.ArrayList;
import java.util.List;

public class MenuRenderingService {

    public static void drawMainMenu(boolean animate) {
        int termWidth = 80;
        int termHeight = 24;
        int boxWidth = 75;

        String[] banner = {
            " _   _                 _ _        _       _____  ____  _____ ",
            "| | | |               (_) |      | |     |____ |/ ___|/ __  \\",
            "| |_| | ___  ___ _ __  _| |_ __ _| |______   / / /___ `' / /'",
            "|  _  |/ _ \\/ __| '_ \\| | __/ _` | |______|  \\ \\ ___ \\  / /  ",
            "| | | | (_) \\__ \\ |_) | | || (_| | |     .___/ / \\_/ |./ /___",
            "\\_| |_/\\___/|___/ .__/|_|\\__\\__,_|_|     \\____/\\_____/\\_____/",
            "                | |                                          ",
            "                |_|                                          "
        };

        String[] options = {
            "[1]  Log in as Employee",
            "[2]  Patients",
            "[3]  Create New Person",
            "[4]  Hire New Employee",
            "[5]  Immediate Assistance",
            "[q]  Quit"
        };

        int totalHeight = 19;
        int startRow = Math.max(1, (termHeight - totalHeight) / 2 + 1);
        int startCol = Math.max(1, (termWidth - boxWidth) / 2 + 1);

        System.out.print("\033[H\033[2J\033[3J");
        System.out.print("\033[?25l"); // Hide cursor
        System.out.flush();

        int bannerStartRow = startRow + 1;
        int optionsStartRow = bannerStartRow + banner.length + 1;
        int promptRow = optionsStartRow + options.length + 1;

        if (animate) {
            // 1. Line by line load the logo in the center
            for (int i = 0; i < banner.length; i++) {
                int padLen = (boxWidth - 2 - banner[i].length()) / 2;
                System.out.print("\033[" + (bannerStartRow + i) + ";" + (startCol + 1 + padLen) + "H" + banner[i]);
                System.out.flush();
                sleep(120);
            }

            // 2. Circle around the border
            List<int[]> perimeter = new ArrayList<>();
            for (int c = 0; c < boxWidth - 1; c++) perimeter.add(new int[]{startRow, startCol + c});
            for (int r = 0; r < totalHeight - 1; r++) perimeter.add(new int[]{startRow + r, startCol + boxWidth - 1});
            for (int c = boxWidth - 1; c > 0; c--) perimeter.add(new int[]{startRow + totalHeight - 1, startCol + c});
            for (int r = totalHeight - 1; r > 0; r--) perimeter.add(new int[]{startRow + r, startCol});

            long sleepTime = 1000 / Math.max(1, perimeter.size());

            for (int[] pos : perimeter) {
                String c = getBorderChar(pos[0], pos[1], startRow, startCol, boxWidth, totalHeight);
                System.out.print("\033[" + pos[0] + ";" + pos[1] + "H" + c);
                System.out.flush();
                sleep(sleepTime);
            }

            // 3. Load in all the selection items
            for (int i = 0; i < options.length; i++) {
                int padLen = (boxWidth - 2 - options[i].length()) / 2;
                System.out.print("\033[" + (optionsStartRow + i) + ";" + (startCol + 1 + padLen) + "H" + options[i]);
                System.out.flush();
                sleep(50);
            }
        } else {
            // Draw immediately
            for (int i = 0; i < banner.length; i++) {
                int padLen = (boxWidth - 2 - banner[i].length()) / 2;
                System.out.print("\033[" + (bannerStartRow + i) + ";" + (startCol + 1 + padLen) + "H" + banner[i]);
            }
            
            for (int r = startRow; r < startRow + totalHeight; r++) {
                for (int c = startCol; c < startCol + boxWidth; c++) {
                    if (r == startRow || r == startRow + totalHeight - 1 || c == startCol || c == startCol + boxWidth - 1) {
                        System.out.print("\033[" + r + ";" + c + "H" + getBorderChar(r, c, startRow, startCol, boxWidth, totalHeight));
                    }
                }
            }

            for (int i = 0; i < options.length; i++) {
                int padLen = (boxWidth - 2 - options[i].length()) / 2;
                System.out.print("\033[" + (optionsStartRow + i) + ";" + (startCol + 1 + padLen) + "H" + options[i]);
            }
        }

        // 4. Print prompt
        String prompt = "Select an option: ";
        System.out.print("\033[" + promptRow + ";" + (startCol + 2) + "H" + prompt);
        
        System.out.print("\033[?25h"); // Show cursor
        // Position cursor for input
        System.out.print("\033[" + promptRow + ";" + (startCol + 2 + prompt.length()) + "H");
        System.out.flush();
    }

    private static String getBorderChar(int r, int c, int startRow, int startCol, int width, int height) {
        if ((r == startRow && c == startCol) || (r == startRow && c == startCol + width - 1) ||
            (r == startRow + height - 1 && c == startCol) || (r == startRow + height - 1 && c == startCol + width - 1)) {
            return "+";
        }
        if (r == startRow || r == startRow + height - 1) {
            return "─"; // Straight horizontal line
        }
        return "│"; // Vertical line
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}