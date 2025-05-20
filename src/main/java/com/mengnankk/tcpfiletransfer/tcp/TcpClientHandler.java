package com.mengnankk.tcpfiletransfer.tcp;

import com.mengnankk.tcpfiletransfer.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

public class TcpClientHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TcpClientHandler.class);
    private final Socket clientSocket;
    private final StorageService storageService;

    // Define command prefixes
    private static final String UPLOAD_COMMAND = "UPLOAD ";
    private static final String DOWNLOAD_COMMAND = "DOWNLOAD ";
    private static final String MSG_OK = "OK";
    private static final String MSG_ERROR = "ERROR";
    private static final int BUFFER_SIZE = 8192; // 8KB buffer

    public TcpClientHandler(Socket socket, StorageService storageService) {
        this.clientSocket = socket;
        this.storageService = storageService;
    }

    @Override
    public void run() {
        try (InputStream inputStream = clientSocket.getInputStream();
             OutputStream outputStream = clientSocket.getOutputStream();
             DataInputStream dataInputStream = new DataInputStream(inputStream);
             DataOutputStream dataOutputStream = new DataOutputStream(outputStream)) {

            // Read the command line (e.g., "UPLOAD filename.txt" or "DOWNLOAD filename.txt")
            String commandLine = dataInputStream.readUTF();
            logger.info("Received command from {}: {}", clientSocket.getRemoteSocketAddress(), commandLine);

            if (commandLine.startsWith(UPLOAD_COMMAND)) {
                String filename = commandLine.substring(UPLOAD_COMMAND.length()).trim();
                if (filename.isEmpty()){
                    sendResponse(dataOutputStream, MSG_ERROR + " Filename cannot be empty.");
                    return;
                }
                handleUpload(filename, dataInputStream, dataOutputStream);
            } else if (commandLine.startsWith(DOWNLOAD_COMMAND)) {
                String filename = commandLine.substring(DOWNLOAD_COMMAND.length()).trim();
                 if (filename.isEmpty()){
                    sendResponse(dataOutputStream, MSG_ERROR + " Filename cannot be empty.");
                    return;
                }
                handleDownload(filename, dataOutputStream);
            } else {
                sendResponse(dataOutputStream, MSG_ERROR + " Unknown command: " + commandLine);
                logger.warn("Unknown command from {}: {}", clientSocket.getRemoteSocketAddress(), commandLine);
            }

        } catch (IOException e) {
            logger.error("Error handling client {}: {}", clientSocket.getRemoteSocketAddress(), e.getMessage(), e);
        } finally {
            try {
                clientSocket.close();
                logger.info("Connection with {} closed.", clientSocket.getRemoteSocketAddress());
            } catch (IOException e) {
                logger.error("Error closing client socket {}: {}", clientSocket.getRemoteSocketAddress(), e.getMessage(), e);
            }
        }
    }

    private void handleUpload(String clientOriginalFilename, DataInputStream dataInputStream, DataOutputStream dataOutputStream) throws IOException {
        try {
            // Read file size first
            long fileSize = dataInputStream.readLong();
            if (fileSize <= 0) {
                sendResponse(dataOutputStream, MSG_ERROR + " Invalid file size: " + fileSize);
                return;
            }
            logger.info("Receiving file '{}' of size {} bytes from {}", clientOriginalFilename, fileSize, clientSocket.getRemoteSocketAddress());

            // Create a PipedInputStream to pass to StorageService
            // This avoids writing the entire file to a temporary local file first if the storage service can stream
            // However, for simplicity in this handler, we'll read into a ByteArrayOutputStream first
            // then pass its InputStream. For very large files, a more direct streaming approach would be better.

            // Acknowledge client to start sending file data
            sendResponse(dataOutputStream, MSG_OK + " Ready to receive file.");

            // Store the file using StorageService
            // The StorageService will generate a new unique filename
            String storedFilename = storageService.store(new LimitedInputStream(dataInputStream, fileSize), clientOriginalFilename);

            sendResponse(dataOutputStream, MSG_OK + " File uploaded successfully as " + storedFilename);
            logger.info("File '{}' (originally '{}') stored as '{}' from {}", clientOriginalFilename, clientOriginalFilename, storedFilename, clientSocket.getRemoteSocketAddress());

        } catch (Exception e) {
            logger.error("Error during upload of file '{}' from {}: {}", clientOriginalFilename, clientSocket.getRemoteSocketAddress(), e.getMessage(), e);
            sendResponse(dataOutputStream, MSG_ERROR + " Failed to upload file: " + e.getMessage());
        }
    }

    private void handleDownload(String filename, DataOutputStream dataOutputStream) throws IOException {
        try {
            org.springframework.core.io.Resource resource = storageService.loadAsResource(filename);
            if (resource.exists() && resource.isReadable()) {
                long fileSize = resource.contentLength();
                sendResponse(dataOutputStream, MSG_OK + " File found. Size: " + fileSize);
                // Send file size
                dataOutputStream.writeLong(fileSize);

                // Send file data
                try (InputStream fileInputStream = resource.getInputStream()) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    long totalBytesSent = 0;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        dataOutputStream.write(buffer, 0, bytesRead);
                        totalBytesSent += bytesRead;
                    }
                    dataOutputStream.flush(); // Ensure all data is sent
                    logger.info("Sent file '{}' ({} bytes) to {}", filename, totalBytesSent, clientSocket.getRemoteSocketAddress());
                }
            } else {
                sendResponse(dataOutputStream, MSG_ERROR + " File not found or not readable: " + filename);
                logger.warn("File not found or not readable for download request from {}: {}", clientSocket.getRemoteSocketAddress(), filename);
            }
        } catch (Exception e) {
            logger.error("Error during download of file '{}' for {}: {}", filename, clientSocket.getRemoteSocketAddress(), e.getMessage(), e);
            // Try to send error to client, but socket might be broken
            try {
                 sendResponse(dataOutputStream, MSG_ERROR + " Failed to download file: " + e.getMessage());
            } catch (IOException ex) {
                logger.warn("Could not send error response to client {}: {}", clientSocket.getRemoteSocketAddress(), ex.getMessage());
            }
        }
    }

    private void sendResponse(DataOutputStream dataOutputStream, String message) throws IOException {
        dataOutputStream.writeUTF(message);
        dataOutputStream.flush();
        logger.debug("Sent to {}: {}", clientSocket.getRemoteSocketAddress(), message);
    }

    /**
     * An InputStream wrapper that limits the number of bytes that can be read from the underlying stream.
     * This is useful to prevent a malicious client from sending more data than declared.
     */
    private static class LimitedInputStream extends InputStream {
        private final InputStream original;
        private long remaining;

        public LimitedInputStream(InputStream original, long limit) {
            this.original = original;
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1; // End of stream
            }
            int result = original.read();
            if (result != -1) {
                remaining--;
            }
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1; // End of stream
            }
            int bytesToRead = (int) Math.min(len, remaining);
            int bytesRead = original.read(b, off, bytesToRead);
            if (bytesRead != -1) {
                remaining -= bytesRead;
            }
            return bytesRead;
        }

        @Override
        public long skip(long n) throws IOException {
            long bytesToSkip = Math.min(n, remaining);
            long bytesSkipped = original.skip(bytesToSkip);
            remaining -= bytesSkipped;
            return bytesSkipped;
        }

        @Override
        public int available() throws IOException {
            return (int) Math.min(original.available(), remaining);
        }

        @Override
        public void close() throws IOException {
            // Do not close the original underlying stream (clientSocket's DataInputStream)
            // as it's managed by the try-with-resources block in the run() method.
        }
    }
}