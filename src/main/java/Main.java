import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");

            if (!scanner.hasNextLine()) break; // EOF (Ctrl+D)

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;

            if (input.equals("exit") || input.startsWith("exit ")) {
                // Parse optional exit code: "exit 0", "exit 1", etc.
                String[] parts = input.split("\\s+", 2);
                int code = 0;
                if (parts.length == 2) {
                    try {
                        code = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        // ignore, default to 0
                    }
                }
                scanner.close();
                System.exit(code);
            }

            System.out.println(input + ": command not found");
        }

        scanner.close();
    }
}