import java.util.Scanner;
import java.util.Set;
import java.io.File;

public class Main {
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
                    String target = arguments.trim();
                    if (BUILTINS.contains(target)) {
                        System.out.println(target + " is a shell builtin");
                    } else {
                        String path = findInPath(target);
                        if (path != null) {
                            System.out.println(target + " is " + path);
                        } else {
                            System.out.println(target + ": not found");
                        }
                    }
                    break;

                default:
                    System.out.println(command + ": command not found");
            }
        }

        scanner.close();
    }

    // Search PATH for an executable file with the given name.
    // Returns the full path if found, or null if not found.
    private static String findInPath(String command) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isEmpty()) return null;

        for (String dir : pathEnv.split(File.pathSeparator)) {
            File file = new File(dir, command);
            if (file.isFile() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }
}