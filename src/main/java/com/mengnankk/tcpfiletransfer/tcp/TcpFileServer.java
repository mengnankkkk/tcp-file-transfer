package com.mengnankk.tcpfiletransfer.tcp;

import com.mengnankk.tcpfiletransfer.config.TcpServerProperties;
import com.mengnankk.tcpfiletransfer.factory.StorageServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class TcpFileServer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TcpFileServer.class);

    private final TcpServerProperties tcpServerProperties;
    private final StorageServiceFactory storageServiceFactory;
    private final ExecutorService executorService;

    @Autowired
    public TcpFileServer(TcpServerProperties tcpServerProperties, StorageServiceFactory storageServiceFactory) {
        this.tcpServerProperties = tcpServerProperties;
        this.storageServiceFactory = storageServiceFactory;
        // Consider making thread pool size configurable
        this.executorService = Executors.newFixedThreadPool(10);
    }

    @Override
    public void run(String... args) throws Exception {
        startServer();
    }

    public void startServer() {
        int port = tcpServerProperties.getPort();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("TCP File Server started on port: {}", port);
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("Accepted connection from: {}", clientSocket.getRemoteSocketAddress());
                    // Pass the storageService instance from the factory
                    TcpClientHandler clientHandler = new TcpClientHandler(clientSocket, storageServiceFactory.getStorageService());
                    executorService.submit(clientHandler);
                } catch (IOException e) {
                    logger.error("Error accepting client connection", e);
                }
            }
        } catch (IOException e) {
            logger.error("Could not start TCP server on port: " + port, e);
            // Consider a more robust error handling or shutdown mechanism
            executorService.shutdown();
        }
    }
}