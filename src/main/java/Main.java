private static void changeDirectory(String path) {
    // Expand ~ to the HOME environment variable
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