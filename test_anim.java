public class test_anim {
    public static void main(String[] args) throws Exception {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        int boxWidth = 20;
        int height = 10;
        int startRow = 2;
        int startCol = 5;
        
        for (int i = 0; i < startRow - 1; i++) System.out.println();
        System.out.println("=".repeat(boxWidth));
        for (int i=0; i<height-2; i++) {
            System.out.println("|" + " ".repeat(boxWidth - 2) + "|");
        }
        System.out.println("=".repeat(boxWidth));
        
        System.out.print("Input: ");
        System.out.flush();
        
        Thread animThread = new Thread(() -> {
            try {
                int pos1 = 0;
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.print("\0337"); 
                    System.out.print("\033[" + startRow + ";" + (startCol + pos1) + "H=");
                    pos1 = (pos1 + 1) % boxWidth;
                    System.out.print("\033[" + startRow + ";" + (startCol + pos1) + "H*");
                    System.out.print("\0338"); 
                    System.out.flush();
                    
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {}
        });
        animThread.start();
        
        Thread.sleep(1000);
        animThread.interrupt();
        animThread.join();
        System.out.println("\nDone");
    }
}
