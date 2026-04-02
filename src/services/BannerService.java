package services;

public class BannerService {
    public static void showWelcomeBanner() {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();

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

        System.out.println();
        for (String line : banner) {
            System.out.println(line);
            sleep(120);
        }
        System.out.println();

        sleep(1000);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
