import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HelloAppEngine extends HttpServlet {

  public void doGet(HttpServletRequest request, HttpServletResponse response) 
      throws IOException {
      
    response.setContentType("text/html");
//    response.getWriter().print("Hello App Engine!\r\n");
    
     
    URL url = new URL("http://www.researcherid.com/rid/" + request.getParameter("id"));

    URLConnection urlConnection = url.openConnection();
    urlConnection.setConnectTimeout(60000); 
    urlConnection.setReadTimeout(60000);
    
    BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
    StringBuffer json = new StringBuffer();
    String line;

    while ((line = reader.readLine()) != null) {
      json.append(line);
    }
    reader.close();    

    response.getWriter().print(json.toString());
  }
}