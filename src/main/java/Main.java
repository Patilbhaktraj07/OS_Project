private static void runUniversalPipeline(List<String> leftCmd, List<String> rightCmd,
                                             String stdoutFile, boolean stdoutAppend,
                                             String stderrFile, boolean stderrAppend) throws Exception {
        String leftCommand = leftCmd.get(0);
        String leftArgs = leftCmd.size() > 1 ? String.join(" ", leftCmd.subList(1, leftCmd.size())) : "";

        String rightCommand = rightCmd.get(0);
        String rightArgs = rightCmd.size() > 1 ? String.join(" ", rightCmd.subList(1, rightCmd.size())) : "";

        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        // --- OPTIMIZED PATH: Built-in LHS (e.g., echo apple | wc) ---
        if (BUILTINS.contains(leftCommand)) {
            ByteArrayOutputStream lhsOut = new ByteArrayOutputStream();
            PrintStream lhsPrintStream = new PrintStream(lhsOut);

            System.setOut(lhsPrintStream);
            try {
                executeSingleCommand(leftCommand, leftArgs, leftCmd, originalOut, originalErr, null);
            } finally {
                System.out.flush();
                System.setOut(originalOut);
            }
            byte[] pipeBuffer = lhsOut.toByteArray();

            // Deliver to RHS
            PrintStream destinationOut = originalOut;
            if (stdoutFile != null) {
                File f = resolveFile(stdoutFile);
                f.getParentFile().mkdirs();
                destinationOut = new PrintStream(new FileOutputStream(f, stdoutAppend));
            }

            if (BUILTINS.contains(rightCommand)) {
                System.setOut(destinationOut);
                if (rightCommand.equals("type") && rightArgs.isEmpty()) {
                    String inputData = new String(pipeBuffer).trim();
                    if (!inputData.isEmpty()) {
                        rightArgs = inputData.split("\\s+")[0];
                        rightCmd = new ArrayList<>(rightCmd);
                        rightCmd.add(rightArgs);
                    }
                }
                try {
                    executeSingleCommand(rightCommand, rightArgs, rightCmd, originalOut, originalErr, null);
                } finally {
                    System.out.flush();
                    if (stdoutFile != null) destinationOut.close();
                    System.setOut(originalOut);
                }
            } else {
                ProcessBuilder pbRight = new ProcessBuilder(rightCmd);
                pbRight.directory(new File(currentDir));
                pbRight.redirectError(ProcessBuilder.Redirect.INHERIT);

                if (stdoutFile != null) {
                    File f = resolveFile(stdoutFile);
                    pbRight.redirectOutput(stdoutAppend ? ProcessBuilder.Redirect.appendTo(f) : ProcessBuilder.Redirect.to(f));
                } else {
                    pbRight.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                }

                Process procRight = pbRight.start();
                try (OutputStream os = procRight.getOutputStream()) {
                    os.write(pipeBuffer);
                    os.flush();
                }
                procRight.waitFor();
            }
            return;
        }

        // --- STREAMING PATH: External LHS (e.g., tail -f file | head) ---
        ProcessBuilder pbLeft = new ProcessBuilder(leftCmd);
        pbLeft.directory(new File(currentDir));
        pbLeft.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process procLeft = pbLeft.start();

        if (BUILTINS.contains(rightCommand)) {
            // LHS External -> RHS Built-in (e.g., ls | type exit)
            ByteArrayOutputStream lhsOut = new ByteArrayOutputStream();
            try (InputStream is = procLeft.getInputStream()) {
                is.transferTo(lhsOut);
            }
            procLeft.waitFor();
            byte[] pipeBuffer = lhsOut.toByteArray();

            PrintStream destinationOut = originalOut;
            if (stdoutFile != null) {
                File f = resolveFile(stdoutFile);
                f.getParentFile().mkdirs();
                destinationOut = new PrintStream(new FileOutputStream(f, stdoutAppend));
            }

            System.setOut(destinationOut);
            if (rightCommand.equals("type") && rightArgs.isEmpty()) {
                String inputData = new String(pipeBuffer).trim();
                if (!inputData.isEmpty()) {
                    rightArgs = inputData.split("\\s+")[0];
                    rightCmd = new ArrayList<>(rightCmd);
                    rightCmd.add(rightArgs);
                }
            }
            try {
                executeSingleCommand(rightCommand, rightArgs, rightCmd, originalOut, originalErr, null);
            } finally {
                System.out.flush();
                if (stdoutFile != null) destinationOut.close();
                System.setOut(originalOut);
            }
        } else {
            // LHS External -> RHS External (e.g., tail -f | head)
            ProcessBuilder pbRight = new ProcessBuilder(rightCmd);
            pbRight.directory(new File(currentDir));
            pbRight.redirectError(ProcessBuilder.Redirect.INHERIT);

            if (stdoutFile != null) {
                File f = resolveFile(stdoutFile);
                pbRight.redirectOutput(stdoutAppend ? ProcessBuilder.Redirect.appendTo(f) : ProcessBuilder.Redirect.to(f));
            } else {
                pbRight.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            }

            Process procRight = pbRight.start();

            // Run active asynchronous engine loop to pass stream data smoothly
            Thread transferThread = new Thread(() -> {
                try (InputStream is = procLeft.getInputStream();
                     OutputStream os = procRight.getOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                        os.flush();
                    }
                } catch (Exception e) {
                    // Stream closed or broken pipe handled gracefully
                }
            });
            transferThread.start();

            // Wait for RHS to finish consuming the necessary lines (e.g., head stops after 5 lines)
            procRight.waitFor();
            
            // Cleanup lingering processes
            procLeft.destroy();
        }
    }