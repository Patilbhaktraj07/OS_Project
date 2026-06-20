import java.util.Scanner;
import java.util.Set;

public class Main {
    // Single source of truth for all known builtins
    private static final Set<String> BUILTINS = Set.of("echo", "exit", "type");

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");

            if (!scanner.hasNextLine()) break;

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;

            String[] parts = input.split("\\s+", 2);
            String command = parts[0];
            String arguments = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case "exit":
                    int code = 0;
                    if (!arguments.isEmpty()) {
                        try { code = Integer.parseInt(arguments.trim()); }
                        catch (NumberFormatException e) { /* default 0 */ }
                    }
                    scanner.close();
                    System.exit(code);
                    break;

                case "echo":
                    System.out.println(arguments);
                    break;

                case "type":
                    if (BUILTINS.contains(arguments.trim())) {
                        System.out.println(arguments.trim() + " is a shell builtin");
                    } else {
                        System.out.println(arguments.trim() + ": not found");
                    }
                    break;

                default:
                    System.out.println(command + ": command not found");
            }
        }

        scanner.close();
    }
}