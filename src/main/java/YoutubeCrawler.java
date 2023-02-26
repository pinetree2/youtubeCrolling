import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class YoutubeCrawler {

    public static void main(String[] args) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        String csvFilePath = "D:\\TitleList.csv";
        String downloadDirectory = "D:\\video";
        String encryptedNamesFilePath = "D:\\encryptedName.csv";
        //String secretKey = "mySecretKey";
        String secretKey = "aeskey12345678987654321asekey987";
        int keySize = 256;
        int iterationCount = 65536;
        int saltLength = keySize / 8;
        int ivLength = 16;

        // 만약 다운로드 폴더가 없을 경우 폴더 생성
        File downloadFolder = new File(downloadDirectory);
        if (!downloadFolder.exists()) {
            downloadFolder.mkdir();
        }

        BufferedReader reader = new BufferedReader(new FileReader(csvFilePath));
        String line;


        while ((line = reader.readLine()) != null) {
            String videoUrl =line;
            String videoId = YoutubeCrawler2.extractVideoId(line);
            String encryptedFileName = encryptFileName(videoId, secretKey, keySize, iterationCount, saltLength, ivLength);
            String downloadUrl = getDownloadUrl(videoId);
            String downloadPath = downloadDirectory + File.separator + encryptedFileName + ".mp4";
            downloadFile(videoUrl, downloadPath);
            writeToCsv(encryptedNamesFilePath, encryptedFileName);


        }


        reader.close();
    }

    private static String getVideoId(String videoUrl) {
        URL url = null;
        try {
            url = new URL(videoUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] pathSegments = url.getPath().split("/");
        return pathSegments[pathSegments.length - 1];
    }

    private static String getDownloadUrl(String videoId) {
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    private static String encryptFileName(String fileName, String secretKey, int keySize, int iterationCount, int saltLength, int ivLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        byte[] salt = new byte[saltLength];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(salt);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt, iterationCount, keySize);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

        byte[] iv = new byte[ivLength];
        secureRandom.nextBytes(iv);
        IvParameterSpec ivspec = new IvParameterSpec(iv);

        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        cipher.init(Cipher.ENCRYPT_MODE, secret, ivspec);
        byte[] encrypted = cipher.doFinal(fileName.getBytes(StandardCharsets.UTF_8));

        String base64EncodedSalt = Base64.getEncoder().encodeToString(salt);
        String base64EncodedIv = Base64.getEncoder().encodeToString(iv);
        String base64EncodedEncryptedData = Base64.getEncoder().encodeToString(encrypted);

        return base64EncodedSalt + ":" + base64EncodedIv + ":" + base64EncodedEncryptedData;
    }

    private static void downloadFile(String url, String filePath) throws IOException {
        URL downloadUrl = new URL(url);
        try (BufferedInputStream in = new BufferedInputStream(downloadUrl.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
             //FileOutputStream fileOutputStream = new FileOutputStream("D:\\video\\video.mp4")) {

            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }
    }

    private static void writeToCsv(String filePath, String encryptedFileName) throws IOException {
        File file = new File(filePath);
        boolean exists = file.exists();
        try (FileWriter csvWriter = new FileWriter(filePath, true);
             CSVPrinter csvPrinter = new CSVPrinter(csvWriter, CSVFormat.DEFAULT.withHeader("Encrypted File Name"))) {
            if (!exists) {
                csvPrinter.printRecord("Encrypted File Name");
            }
            csvPrinter.printRecord(encryptedFileName);
        }
    }
}
