import java.util.Scanner;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

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

            List<String> tokens = parseTokens(input);
            if (tokens.isEmpty()) continue;

            String stdoutFile = null;
            String stderrFile = null;
            boolean stdoutAppend = false;
            List<String> cmdTokens = new ArrayList<>();

            for (int i = 0; i < tokens.size(); i++) {
                String t = tokens.get(i);
                if ((t.equals(">>") || t.equals("1>>")) && i + 1 < tokens.size()) {
                    stdoutFile = tokens.get(++i);
                    stdoutAppend = true;
                } else if ((t.equals(">") || t.equals("1>")) && i + 1 < tokens.size()) {
                    stdoutFile = tokens.get(++i);
                    stdoutAppend = false;
                } else if (t.equals("2>") && i + 1 < tokens.size()) {
                    stderrFile = tokens.get(++i);
                } else {
                    cmdTokens.add(t);
                }
            }

            if (cmdTokens.isEmpty()) continue;

            String command = cmdTokens.get(0);
            String arguments = cmdTokens.size() > 1
                ? String.join(" ", cmdTokens.subList(1, cmdTokens.size()))
                : "";

            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;

            if (stdoutFile != null) {
                File f = resolveFile(stdoutFile);
                f.getParentFile().mkdirs();
                System.setOut(new PrintStream(new FileOutputStream(f, stdoutAppend)));
            }
            if (stderrFile != null) {
                File f = resolveFile(stderrFile);
                f.getParentFile().mkdirs();
                System.setErr(new PrintStream(new FileOutputStream(f, false)));
            }

            try {
                switch (command) {
                    case "exit":
                        System.setOut(originalOut);
                        System.setErr(originalErr);
                        int code = 0;
                        if (!arguments.isEmpty()) {
                            try { code = Integer.parseInt(arguments); }
                            catch (NumberFormatException e) { /* default 0 */ }
                        }
                        scanner.close();
                        System.exit(code);
                        break;

                    case "echo":
                        System.out.println(String.join(" ", cmdTokens.subList(1, cmdTokens.size())));
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
                            runExternal(cmdTokens.toArray(new String[0]), stdoutFile, stdoutAppend, stderrFile);
                        } else {
                            System.err.println(command + ": command not found");
                        }
                }
            } finally {
                System.out.flush();
                System.err.flush();
                System.setOut(originalOut);
                System.setErr(originalErr);
            }
        }

        scanner.close();
    }

    private static File resolveFile(String path) {
        return path.startsWith("/")
            ? new File(path)
            : new File(currentDir + File.separator + path);
    }

    private static void runExternal(String[] cmdArgs, String stdoutFile, boolean stdoutAppend, String stderrFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmdArgs);
        pb.directory(new File(currentDir));

        if (stdoutFile != null) {
            File f = resolveFile(stdoutFile);
            f.getParentFile().mkdirs();
            pb.redirectOutput(stdoutAppend
                ? ProcessBuilder.Redirect.appendTo(f)
                : ProcessBuilder.Redirect.to(f));
        } else {
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        }

        if (stderrFile != null) {
            File f = resolveFile(stderrFile);
            f.getParentFile().mkdirs();
            pb.redirectError(ProcessBuilder.Redirect.to(f));
        } else {
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        }

        Process process = pb.start();
        process.waitFor();
    }

    private static List<String> parseTokens(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (inSingleQuote) {
                if (c == '\'') {
                    inSingleQuote = false;
                } else {
                    current.append(c);
                }
            } else if (inDoubleQuote) {
                if (c == '\\' && i + 1 < input.length()) {
                    char next = input.charAt(i + 1);
                    if (next == '"' || next == '\\') {
                        current.append(next);
                        i++;
                    } else {
                        current.append(c);
                    }
                } else if (c == '"') {
                    inDoubleQuote = false;
                } else {
                    current.append(c);
                }
            } else {
                if (c == '\\') {
                    if (i + 1 < input.length()) {
                        current.append(input.charAt(++i));
                    }
                } else if (c == '\'') {
                    inSingleQuote = true;
                } else if (c == '"') {
                    inDoubleQuote = true;
                } else if (c == ' ' || c == '\t') {
                    if (current.length() > 0) {
                        tokens.add(current.toString());
                        current.setLength(0);
                    }
                } else {
                    current.append(c);
                }
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    private static void changeDirectory(String path) {
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
            System.err.println("cd: " + path + ": No such file or directory");
        } else {
            currentDir = resolved.getPath();
            System.setProperty("user.dir", currentDir);
        }
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