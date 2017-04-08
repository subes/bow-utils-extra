/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-4-6. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.util;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.concurrent.TimeUnit;

public class ExecuteAndLogProcess {

    public static Process exec(String applicationName, Logger logger, String... command) throws IOException {
        return exec(new ProcessBuilder(command), applicationName, logger);
    }

    public static Process exec(ProcessBuilder processBuilder, String applicationName, Logger logger) throws IOException {
        return exec(processBuilder, applicationName, logger, 0);
    }

    public static Process exec(ProcessBuilder processBuilder, String applicationName, Logger logger, long maxTimeToWait) throws IOException {
        Process p = processBuilder.start();
        return exec(applicationName, logger, maxTimeToWait, p);
    }

    public static Process exec(String applicationName, Logger logger, long maxTimeToWait, Process p) throws IOException {
        LogErrorThread errorThread = new LogErrorThread(applicationName, p, logger);
        LogInfoThread stdThread = new LogInfoThread(applicationName, p, logger);
        errorThread.start();
        stdThread.start();
        try {
            boolean terminated;
            if (maxTimeToWait > 0) {
                terminated = p.waitFor(maxTimeToWait, TimeUnit.MILLISECONDS);
            } else {
                p.waitFor();
                terminated = true;
            }
            if (errorThread.exception != null) {
                throw new IOException("Could not read error output stream", errorThread.exception);
            }
            if (stdThread.exception != null) {
                throw new IOException("Could not read std output stream", stdThread.exception);
            }
            if (terminated) {
                errorThread.join();
                stdThread.join();
                if (p.exitValue() != 0) {
                    throw new RuntimeException("Executing process failed. See the logs for the error.");
                }
            }
            return p;
        } catch (InterruptedException exp) {
            p.destroy();
            throw new RuntimeException("Received interrupted exception while running " + applicationName + ", terminating process");
        }
    }

    private abstract static class CollectOutputThread extends Thread {

        private final String applicationName;
        private final InputStream is;
        final Logger logger;
        public Exception exception;

        public CollectOutputThread(String applicationName, InputStream is, Logger logger) {
            super("collect_output_thread");
            this.applicationName = applicationName;
            this.is = is;
            this.logger = logger;
        }

        public void run() {
            try {
                IOUtils.copy(is, new Writer() {
                    StringBuffer stringBuffer = new StringBuffer();

                    @Override
                    public void write(char[] cbuf, int off, int len) throws IOException {
                        for (int i = off; i < off + len; i++) {
                            char c = cbuf[i];
                            if (c == '\n') {
                                printLine("~~" + applicationName + "~~ " + stringBuffer.toString());
                                stringBuffer = new StringBuffer();
                            } else {
                                stringBuffer.append(c);
                            }
                        }
                    }

                    @Override
                    public void flush() throws IOException {
                        //OK
                    }

                    @Override
                    public void close() throws IOException {
                        String remaining = stringBuffer.toString();
                        if (remaining.length() > 0) {
                            printLine(remaining);
                        }
                    }
                }, "UTF-8");
            } catch (IOException exp) {
                exception = exp;
            }
        }

        protected abstract void printLine(String line);
    }

    private static class LogInfoThread extends CollectOutputThread {
        public LogInfoThread(String applicationName, Process process, Logger logger) {
            super(applicationName, process.getInputStream(), logger);
        }

        @Override
        protected void printLine(String line) {
            logger.info(line);
        }
    }

    private static class LogErrorThread extends CollectOutputThread {
        public LogErrorThread(String applicationName, Process process, Logger logger) {
            super(applicationName, process.getErrorStream(), logger);
        }

        @Override
        protected void printLine(String line) {
            logger.error(line);
        }
    }

}
