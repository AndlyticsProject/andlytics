/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tarsys.checkout.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author TaRRaDeLo
 */
public class Utilidades {

    /**
     * Genera un documento xml a partir del código xml alojado en un string
     * @param codigo
     * @return
     */
    public static Document ObtenerDocumentoXmlDesdeString(String codigo){
        Document retorno = null;
        try {
            // Limpiamos el código de datos indeseados...
            String codigoXml = !codigo.contains("<!-- www.000webhost.com Analytics Code -->")?codigo:codigo.substring(0,codigo.indexOf("<!-- www.000webhost.com Analytics Code -->"));
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setExpandEntityReferences(true);
            factory.setCoalescing(true);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);

            DocumentBuilder builder = factory.newDocumentBuilder();

            retorno = builder.parse(new ByteArrayInputStream(codigoXml.getBytes()));
        } catch (Exception ex) {
            Logger.getLogger(Utilidades.class.getName()).log(Level.SEVERE, null, ex);
        }
        return retorno;
    }

    /**
     * Genera el hash md5 de una cadena
     * @param s
     * @return
     */
    public static String Md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();
            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
        }
        return "";
    }

    /**
     * Extrae el código xml de un Nodo de un documento xml
     * @param root
     * @return
     * @throws IOException
     */
    public static String DocumentAString(Node root) throws IOException {
        StringBuilder result = new StringBuilder();
        if (root.getNodeType() == 3)
            result.append(root.getNodeValue());
        else {
            if (root.getNodeType() != 9) {
                StringBuffer attrs = new StringBuffer();
                for (int k = 0; k < root.getAttributes().getLength(); ++k) {
                    attrs.append(" ").append(
                                root.getAttributes().item(k).getNodeName()).append(
                                    "=\"").append(
                                        root.getAttributes().item(k).getNodeValue()).append("\" ");
                }
                result.append("<").append(root.getNodeName()).append(" ").append(attrs).append(">");
            } else {
                result.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            }
            NodeList nodes = root.getChildNodes();
            for (int i = 0, j = nodes.getLength(); i < j; i++) {
                Node node = nodes.item(i);
                result.append(DocumentAString(node));
            }
            if (root.getNodeType() != 9) {
                result.append("</").append(root.getNodeName()).append(">");
            }
        }
        return result.toString();
    }

    /**
     * Extrae el código xml de un Documento xml
     * @param doc
     * @return
     */
    public static String DocumentAString(Document doc){
        String retorno = "";
        try{
            retorno = Utilidades.DocumentAString((Node)doc.getDocumentElement());
        }catch (Exception ex){
            retorno = "";
        }
        return retorno;
    }

    public static String Stream2String(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        is.close();
        return sb.toString();
    }

    public static String Stream2StringSinCR(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        is.close();
        return sb.toString();
    }

    public static long ConvertirDateStringALong(String fechaStr){
        long retorno = 0;
        try{
        Date fecha = new Date(fechaStr);
        // una vez tenemos el objeto fecha...
        retorno = fecha.getTime();
        }catch(Exception ex){
            retorno = 0;
        }
        return retorno;
    }

    public static boolean ConectadoARedWifi(Context contexto){
        boolean retorno = false;
        ConnectivityManager cm = (ConnectivityManager) contexto.getSystemService(Context.CONNECTIVITY_SERVICE);
        TelephonyManager mTelephony = (TelephonyManager) contexto.getSystemService(Context.TELEPHONY_SERVICE);
        NetworkInfo infoNetWork = null;
        if (cm != null && mTelephony != null){
            if (cm.getBackgroundDataSetting()){
                try{
                infoNetWork = cm.getActiveNetworkInfo();
                }catch (Exception ex){
                    infoNetWork = null;
                }
                if (infoNetWork != null){
                    int netType = infoNetWork.getType();
                    int netSubtype = infoNetWork.getSubtype();

                    if (netType == ConnectivityManager.TYPE_WIFI) {
                        retorno = infoNetWork.isConnected();
                    }
                }
            }
        }

        return retorno;
    }

    public static int[]CoordenadasGPS(String poblacion, String pais){
        int []retorno = new int[]{0,0};
        String urlJSON = String.format("http://maps.googleapis.com/maps/api/geocode/json?address=%s&sensor=true", URLEncoder.encode(poblacion + "," +pais));
        DefaultHttpClient clienteHttp = new DefaultHttpClient();

        try {
            HttpGet getter = new HttpGet(urlJSON);
            HttpResponse respuesta = clienteHttp.execute(getter);
            HttpEntity entidad = respuesta.getEntity();
            InputStream streamJSon = entidad.getContent();
            if (streamJSon != null){
                String stringJSon = Utilidades.Stream2String(streamJSon);
                JSONObject objetoJSon = new JSONObject(stringJSon);
                if (objetoJSon != null){
                    JSONObject resultados = objetoJSon.getJSONArray("results").getJSONObject(0);
                    if (resultados != null){
                        retorno[0] = (int)(resultados.getJSONObject("geometry").getJSONObject("location").getDouble("lat")*1e6);
                        retorno[1] = (int)(resultados.getJSONObject("geometry").getJSONObject("location").getDouble("lng")*1e6);
                    }
                }
            }
        } catch (Exception ex) {
            if (ex != null){

            }
        }

        return retorno;
    }
}
