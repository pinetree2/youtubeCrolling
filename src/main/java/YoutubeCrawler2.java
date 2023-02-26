import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.json.JSONArray;
import org.json.JSONObject;

public class YoutubeCrawler2 {

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY_ALGORITHM = "AES";
    private static final int IV_LENGTH = 16;
    private static final int KEY_LENGTH = 128;
    private static final String OUTPUT_CSV_FILE = "D:\\encrypted_titles.csv";
    private static final String API_KEY = "API-KEY";

    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader("D:\\TitleList.csv"));
        String line;
        while ((line = br.readLine()) != null) {
            String videoUrl = line;
            System.out.println("videoUrl: "+line);
            String videoId = extractVideoId(videoUrl);
            if (videoId == null) {
                System.out.println("Invalid video URL: " + videoUrl);
                continue;
            }
            String apiUrl = "https://www.googleapis.com/youtube/v3/videos?id=" + videoId + "&key=" + API_KEY + "&part=snippet,contentDetails,statistics,status";
            String response = sendGetRequest(apiUrl);
            if (response == null) {
                System.out.println("Failed to retrieve video information: " + videoUrl);
                continue;
            }
            String title = getVideoTitle(response);
            System.out.println("response:"+response);
            String downloadUrl = getDownloadUrl(response);

            byte[] iv = generateIv();
            SecretKey secretKey = generateSecretKey();
            System.out.println("secretKey: "+secretKey);
            Cipher cipher = createCipher(secretKey, iv, Cipher.ENCRYPT_MODE);

            byte[] encryptedTitle = cipher.doFinal(title.getBytes());
            String base64EncodedTitle = Base64.getEncoder().encodeToString(encryptedTitle);

            saveToCsvFile(videoId, base64EncodedTitle);
            //downloadVideo(videoUrl, base64EncodedTitle);

            downloadVideo(videoUrl, title);
        }
        br.close();
    }

    public static String extractVideoId(String videoUrl) {
        String[] parts = videoUrl.split("v=");
        if (parts.length != 2) {
            return null;
        }
        return parts[1];
    }

    private static String sendGetRequest(String url) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new java.io.InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    private static String getVideoTitle(String response) {
        return new JSONObject(response).getJSONArray("items").getJSONObject(0).getJSONObject("snippet").getString("title");
    }

    private static String getDownloadUrl(String response) {
        return new JSONObject(response).getJSONArray("items").getJSONObject(0).getString("id");
    }

    private static byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }

    private static SecretKey generateSecretKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(SECRET_KEY_ALGORITHM);
        keyGen.init(KEY_LENGTH);
        return keyGen.generateKey();
    }

    private static Cipher createCipher(SecretKey secretKey, byte[] iv, int mode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(mode, secretKey, new IvParameterSpec(iv));
        return cipher;
    }

    private static void saveToCsvFile(String videoId, String encryptedTitle) throws IOException {
        String row = String.format("%s,%s%n", videoId, encryptedTitle);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(OUTPUT_CSV_FILE, true));
        out.write(row.getBytes());
        out.close();
    }

    private static void downloadVideo(String downloadUrl, String title) throws IOException {
        URL url = new URL(downloadUrl);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(title + ".mp4"));

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedInputStream in = new BufferedInputStream(con.getInputStream());

        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        out.close();
        in.close();
    }
}
