
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

import javax.net.ssl.SSLContext;

/** everything you need to start is in the com.meterware.httpunit package **/
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

/**
 * This is a simple example of using HttpUnit to read and understand web pages.
 **/
public class YouTube {

    public static void main(String[] params) {
        System.out.println("start");
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            Enumeration _enum = sc.getProvider().elements();
            String key = null;
            while (_enum.hasMoreElements()) {
                key = _enum.nextElement().toString();
                System.out.println(key);
            }
        } catch (NoSuchAlgorithmException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            // create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Obtain the main page on the meterware web site
            String url = "https://ssyoutube.com/en489/";
            WebRequest request = new GetMethodWebRequest(url);

            WebResponse response = wc.getResponse(request);

            WebForm form = response.getFormWithID("main-form");
            String target = "https://www.youtube.com/watch?v=CnwGrI6T7X0";
            form.setParameter("url", target);
            response = form.submit();

            WebLink httpunitLink = response.getFirstMatchingLink(WebLink.MATCH_CONTAINED_TEXT,"download-mp4-720-audio");

            response = httpunitLink.click();

            // find the link which contains the string "HttpUnit" and click it
            // WebLink httpunitLink = response.getFirstMatchingLink(
            // WebLink.MATCH_CONTAINED_TEXT, "HttpUnit" );

            // print out the number of links on the HttpUnit main page
            System.out
                    .println("The HttpUnit main page '" + url + "' contains " + response.getLinks().length + " links");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Exception: " + e);
        }
    }
}