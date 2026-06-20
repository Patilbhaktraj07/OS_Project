import java.util.Scanner;
import java.util.Set;
import java.io.File;

public class Main {
    private static final Set<String> BUILTINS = Set.of("echo", "exit", "type", "pwd", "cd");
    private static String currentDir = System.getProperty("user.dir");

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");

            if (!scanner.hasNextLine()) break;

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;

            String[] parts = input.split("\\s+", 2);
            String command = parts[0];
            String arguments = parts.length > 1 ? parts[1].trim() : "";

            switch (command) {
                case "exit":
                    int code = 0;
                    if (!arguments.isEmpty()) {
                        try { code = Integer.parseInt(arguments); }
                        catch (NumberFormatException e) { /* default 0 */ }
                    }
                    scanner.close();
                    System.exit(code);
                    break;

                case "echo":
                    System.out.println(arguments);
                    break;

                case "type":
                    if (BUILTINS.contains(arguments)) {
                        System.out.println(arguments + " is a shell builtin");
                    } else {
                        String path = findInPath(arguments);
                        if (path != null) {
                            System.out.println(arguments + " is " + path);
                        } else {
                            System.out.println(arguments + ": not found");
                        }
                    }
                    break;

                case "pwd":
                    System.out.println(currentDir);
                    break;

                case "cd":
                    changeDirectory(arguments);
                    break;

                default:
                    String execPath = findInPath(command);
                    if (execPath != null) {
                        runExternal(input.split("\\s+"));
                    } else {
                        System.out.println(command + ": command not found");
                    }
            }
        }

        scanner.close();
    }

    private static void changeDirectory(String path) {
        // Expand ~ to HOME
        if (path.equals("~") || path.startsWith("~/")) {
            String home = System.getenv("HOME");
            if (home == null) home = System.getProperty("user.home");
            path = path.equals("~") ? home : home + path.substring(1);
        }

        File target = path.startsWith("/")
            ? new File(path)
            : new File(currentDir, path);

        File resolved = target.getAbsoluteFile().toPath().normalize().toFile();

        if (!resolved.exists() || !resolved.isDirectory()) {
            System.out.println("cd: " + path + ": No such file or directory");
        } else {
            currentDir = resolved.getPath();
            System.setProperty("user.dir", currentDir);
        }
    }

    private static void runExternal(String[] cmdArgs) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmdArgs);
        pb.directory(new File(currentDir));
        pb.inheritIO();
        Process process = pb.start();
        process.waitFor();
    }

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