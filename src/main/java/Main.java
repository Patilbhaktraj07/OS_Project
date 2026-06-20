private static void changeDirectory(String path) {
    // Absolute if starts with "/", otherwise relative to currentDir
    File target = path.startsWith("/")
        ? new File(path)
        : new File(currentDir, path);  // ← handles ./foo, ../foo, foo

    // normalize() resolves .., ., and cleans up the path
    File resolved = target.getAbsoluteFile().toPath().normalize().toFile();

    if (!resolved.exists() || !resolved.isDirectory()) {
        System.out.println("cd: " + path + ": No such file or directory");
    } else {
        currentDir = resolved.getPath();
        System.setProperty("user.dir", currentDir);
    }
}