package ru.ifmo.ctddev.trofiv.walk;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Walk {
    private static final String READ_ERROR_MD5_VALUE = "00000000000000000000000000000000";

    public static void main(final String[] args) {
        if (args.length == 2) {
            processCommand(args[0], args[1]);
        } else {
            printHelp();
        }
    }

    private static void processCommand(final String inputFilePath, final String outputFilePath) {
        try (BufferedReader inputFileReader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(inputFilePath), UTF_8));
             BufferedWriter outputFileWriter = new BufferedWriter(
                     new OutputStreamWriter(
                             new FileOutputStream(outputFilePath), UTF_8))) {
            while (inputFileReader.ready()) {
                String filePath = null;
                try {
                    filePath = inputFileReader.readLine();
                    processInputPath(Paths.get(filePath), outputFileWriter);
                } catch (InvalidPathException e) {
                    System.err.println("Invalid path format: " + filePath);
                    e.printStackTrace();
                    printChecksum(outputFileWriter, READ_ERROR_MD5_VALUE, filePath);
                }
            }
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Your JVM doesn't support MD5 hashing!");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error with access input/output file");
            e.printStackTrace();
        }
    }

    private static void printChecksum(
            final BufferedWriter outputFileWriter,
            final String checksum,
            final String filePath)
            throws IOException {
        outputFileWriter.write(checksum + ' ' + filePath);
        outputFileWriter.newLine();
    }

    private static void processInputPath(final Path inputPath, final BufferedWriter outputFileWriter)
            throws NoSuchAlgorithmException, IOException {
        if (Files.isDirectory(inputPath)) {
            System.out.println("\nEntering directory " + inputPath + '\n');
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(inputPath)) {
                for (Path path : directoryStream) {
                    processInputPath(path, outputFileWriter);
                }
            } catch (IOException e) {
                System.err.println("Error occurred during access to directory " + inputPath);
                e.printStackTrace();
                printChecksum(outputFileWriter, READ_ERROR_MD5_VALUE, inputPath.toString());
            }
        } else {
            final String md5 = calculateMD5Checksum(inputPath);
            System.out.println("MD5 is " + md5 + " for file " + inputPath);
            printChecksum(outputFileWriter, md5, inputPath.toString());
        }
    }

    private static String calculateMD5Checksum(final Path filePath)
            throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("MD5");

        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            final byte[] buffer = new byte[1024];

            int nread;
            while ((nread = fis.read(buffer)) != -1) {
                md.update(buffer, 0, nread);
            }

            return DatatypeConverter.printHexBinary(md.digest());
        } catch (IOException e) {
            System.err.println("Error occurred during md5 calculation of file " + filePath);
            e.printStackTrace();
            return READ_ERROR_MD5_VALUE;
        }
    }

    private static void printHelp() {
        System.out.println("Usage: java Walk input_file_path output_file_path");
    }
}
