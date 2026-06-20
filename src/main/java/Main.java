import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");

            if (!scanner.hasNextLine()) break; // EOF (Ctrl+D)

            String input = scanner.nextLine().trim();

            if (!input.isEmpty()) {
                System.out.println(input + ": command not found");
            }
        }

        scanner.close();
    }
}