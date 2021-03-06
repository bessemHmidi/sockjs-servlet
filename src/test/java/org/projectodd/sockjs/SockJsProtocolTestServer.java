/**
 * Copyright (C) 2014 Red Hat, Inc, and individual contributors.
 */

package org.projectodd.sockjs;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class SockJsProtocolTestServer extends AbstractSockJsTest {

    public static void main(String[] args) throws Exception {
        configureLogging(Level.FINEST);
        SockJsServer echoServer = new SockJsServer();
        echoServer.options.responseLimit = 4096;
        echoServer.onConnection(new SockJsServer.OnConnectionHandler() {
            @Override
            public void handle(final SockJsConnection connection) {
                System.out.println("    [+] echo open    " + connection);
                connection.onClose(new SockJsConnection.OnCloseHandler() {
                    @Override
                    public void handle() {
                        System.out.println("    [-] echo close    " + connection);
                    }
                });
                connection.onData(new SockJsConnection.OnDataHandler() {
                    @Override
                    public void handle(String message) {
                        System.out.println("    [ ] echo message " + connection + " " + message);
                        connection.write(message);
                    }
                });
            }
        });

        SockJsServer echoNoWsServer = new SockJsServer();
        echoNoWsServer.options.responseLimit = 4096;
        echoNoWsServer.options.websocket = false;
        echoNoWsServer.onConnection(new SockJsServer.OnConnectionHandler() {
            @Override
            public void handle(final SockJsConnection connection) {
                System.out.println("    [+] echo open    " + connection);
                connection.onClose(new SockJsConnection.OnCloseHandler() {
                    @Override
                    public void handle() {
                        System.out.println("    [-] echo close    " + connection);
                    }
                });
                connection.onData(new SockJsConnection.OnDataHandler() {
                    @Override
                    public void handle(String message) {
                        System.out.println("    [ ] echo message " + connection + " " + message);
                        connection.write(message);
                    }
                });
            }
        });

        SockJsServer echoCookie = new SockJsServer();
        echoCookie.options.responseLimit = 4096;
        echoCookie.options.jsessionid = true;
        echoCookie.onConnection(new SockJsServer.OnConnectionHandler() {
            @Override
            public void handle(final SockJsConnection connection) {
                System.out.println("    [+] echo open    " + connection);
                connection.onClose(new SockJsConnection.OnCloseHandler() {
                    @Override
                    public void handle() {
                        System.out.println("    [-] echo close    " + connection);
                    }
                });
                connection.onData(new SockJsConnection.OnDataHandler() {
                    @Override
                    public void handle(String message) {
                        System.out.println("    [ ] echo message " + connection + " " + message);
                        connection.write(message);
                    }
                });
            }
        });

        SockJsServer closeServer = new SockJsServer();
        closeServer.onConnection(new SockJsServer.OnConnectionHandler() {
            @Override
            public void handle(final SockJsConnection connection) {
                System.out.println("    [+] clos open    " + connection);
                connection.close(3000, "Go away!");
                connection.onClose(new SockJsConnection.OnCloseHandler() {
                    @Override
                    public void handle() {
                        System.out.println("    [-] clos close    " + connection);
                    }
                });
            }
        });

        SockJsProtocolTestServer servletTest = new SockJsProtocolTestServer();
        PathHandler pathHandler = new PathHandler();
        servletTest.installHandler(pathHandler, echoServer, "/echo");
        servletTest.installHandler(pathHandler, echoNoWsServer, "/disabled_websocket_echo");
        servletTest.installHandler(pathHandler, echoCookie, "/cookie_needed_echo");
        servletTest.installHandler(pathHandler, closeServer, "/close");
        servletTest.runServer(pathHandler, "localhost", 8081);
    }

    private void runServer(HttpHandler handler, String host, int port) throws Exception {
        Undertow undertow = createUndertow(handler, host, port);

        final CountDownLatch latch = new CountDownLatch(1);
        SignalHandler signalHandler = new SignalHandler() {
            @Override
            public void handle(Signal sig) {
                latch.countDown();
            }
        };
        undertow.start();
        System.out.println("SockJS Servlet running on http://" + host + ":" + port);
        System.out.println("CTRL+C to terminate");
        Signal.handle(new Signal("INT"), signalHandler);
        Signal.handle(new Signal("TERM"), signalHandler);
        latch.await();
    }

    private static void configureLogging(Level level) throws Exception {
        Logger rootLogger = Logger.getLogger("");
        String logFile = System.getProperty("logFile");
        if (logFile != null) {
            rootLogger.addHandler(new FileHandler(logFile));
        }
        for (Handler handler : rootLogger.getHandlers()) {
            handler.setFilter(new Filter() {
                private String[] noisyLoggers = new String[] { "com.sun.jmx", "javax.management", "org.xnio" };
                @Override
                public boolean isLoggable(LogRecord record) {
                    String loggerName = record.getLoggerName();
                    for (String noisyLogger : noisyLoggers) {
                        if (loggerName != null && loggerName.startsWith(noisyLogger)) {
                            return false;
                        }
                    }
                    return true;
                }
            });
            handler.setLevel(level);
            handler.setFormatter(new java.util.logging.Formatter() {
                // Totally ripped off from java.util.logging.SimpleFormatter
                private final String format = "%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %4$s [%2$s] %5$s%6$s%n";
                private final Date dat = new Date();
                @Override
                public synchronized String format(LogRecord record) {
                    dat.setTime(record.getMillis());
                    String source = record.getLoggerName();
                    String message = formatMessage(record);
                    String throwable = "";
                    if (record.getThrown() != null) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        pw.println();
                        record.getThrown().printStackTrace(pw);
                        pw.close();
                        throwable = sw.toString();
                    }
                    return String.format(format,
                            dat,
                            source,
                            record.getLoggerName(),
                            record.getLevel().getName(),
                            message,
                            throwable);
                }
            });
        }
        rootLogger.setLevel(level);
    }
}
