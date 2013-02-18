/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tarsys.checkout.servicio;

import android.content.Context;
import android.text.Html;
import android.util.Xml;
import android.util.Xml.Encoding;
import android.widget.Toast;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import tarsys.checkout.entidades.LineaPagoCheckOut;
import tarsys.checkout.entidades.LineaPedidoCheckOut;
import tarsys.checkout.entidades.PedidoCheckOut;
import tarsys.checkout.servicio.enumerados.EstadoCargaPedido;
import tarsys.checkout.servicio.listeners.*;
import tarsys.checkout.util.Utilidades;
import tarsys.checkout.widgets.TarsysWebView;
import tarsys.checkout.widgets.listeners.EvFinCargaTarsysWebView;
import tarsys.checkout.widgets.listeners.FinCargaTarsysWebViewListener;

/**
 *
 * @author TaRRaDeLo
 */
public class ServicioCheckOutVenta {

    // <editor-fold defaultstate="collapsed" desc="Variables privadas de la clase">

    private Context contextoActual = null;
    private String usuario = "";
    private String password = "";
    private boolean cancelandoProcesos = false;
    //
    // Urls de Gestión de Ventas
    //

    // Url Inicial de carga
    private String urlCheckOutInicial = "https://www.google.com/accounts/ServiceLogin?service=sierra&continue=";

    private String urlCheckOutMain = "https://checkout.google.com/main";
    // Url que se utilizará para hacer post e iniciar sesión
    private String urlServiceLoginAuth = "https://www.google.com/accounts/ServiceLoginAuth";
    // Url que se utilizará para hacer login (sin acceso a datos, sólo para hacer el login inicial)
    private String urlLoginCheckout = "https://www.google.com/accounts/ClientLogin";
    // Url Inicial de Entrada de Pedidos de Venta
    private String UrlVentasCheckOut = "https://checkout.google.com/sell/orders?page_size=100&ordersTable=1";
    // Url de Datos de pedido, para obtener un pedido, habrá que concatener el número de pedido al final (se aplicará a order
    private String UrlVentasCheckOutDatoPedido = "https://checkout.google.com/sell/multiOrder?ordersTable=1&order=";
    // Url de Datos de pedidos archivados
    private String UrlVentasCheckOutArchivadas = "https://checkout.google.com/sell/archive?page_size=100&ordersTable=1";
    private String urlCvs = "https://checkout.google.com/cws/v2/AndroidMarket-1011/@MERCHANTID@/reportsForm";
    // Url de Datos de Pagos
    private String UrlVentasCheckOutPagos = "https://checkout.google.com/sell/payouts?MerchantBalanceSheet=0%3A1%3A100";
    // Url de parámetros del perfil
    private String UrlVentasCheckOutCfgPerfil = "https://checkout.google.com/sell/settings?section=Profile";
    // Url de datos de configuración de usuarios asociados
    private String UrlVentasCheckOutCfgUsuario = "https://checkout.google.com/sell/settings?section=Users";

    private long merchantId = 0;
    private boolean siConectado;
    //private CookieSyncManager gestorCookies = null;

    // Variables de gestión de listeners para obtención de datos de consulta...
    private ArrayList<ObtencionListaPedidosCheckOutListener> listenersObtencionListaPedidos = new ArrayList<ObtencionListaPedidosCheckOutListener>();
    private ArrayList<ObtencionDatosPedidoListener> listenersObtencionDatosPedido = new ArrayList<ObtencionDatosPedidoListener>();
    private ArrayList<ObtencionDatosPagosListener> listenersObtencionDatosPago = new ArrayList<ObtencionDatosPagosListener>();
    private ArrayList<CambioEstadoArchivadoListener> listenersCambioEstadoArchivado = new ArrayList<CambioEstadoArchivadoListener>();
    private ArrayList<EnvioPedidoListener> listenersEnvioPedido = new ArrayList<EnvioPedidoListener>();
    private ArrayList<ProcesoLoginListener> listenersProcesoLogin = new ArrayList<ProcesoLoginListener>();

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Métodos privados de parsing de la clase">

    /**
     * Parsea el código fuente de la ventana de pagos, con el fin de obtener los datos necesarios, Obtiene siempre a 100 días vista...
     * @param evt
     */
    private void ParsePagos(EvFinCargaTarsysWebView evt){
        String codigoHtml = evt.CodigoHtml.replace("\n", "");
        boolean siParse = false;
        ArrayList<LineaPagoCheckOut> lineasPago = new ArrayList<LineaPagoCheckOut>();
        if (!codigoHtml.equals("")){
            String regExPagos = "<tr\\s+class=\"lineitem\"\\s*>(.*?)</tr\\s*>";

            try{
                Pattern patPagos = Pattern.compile(regExPagos,Pattern.CASE_INSENSITIVE);
                Matcher matchPagos = patPagos.matcher(codigoHtml);
                if (matchPagos != null){
                    while (matchPagos.find() && !this.cancelandoProcesos){

                        String codigoDetalle = matchPagos.group(1);
                        if (!codigoDetalle.startsWith("<th")){
                            // Si no estamos con la cabecera... estamos con el detalle...
                            String []columnas = codigoDetalle.split("</td>");
                            if (columnas != null){
                                if (columnas.length == 6){
                                    LineaPagoCheckOut lineaPago = new LineaPagoCheckOut();
                                    lineaPago.Fecha = new SimpleDateFormat("dd/MM/yyyy").parse(columnas[0].substring(columnas[0].lastIndexOf(">")+1));
                                    lineaPago.SaldoInicial = columnas[1].substring(columnas[1].lastIndexOf(">")+1);
                                    lineaPago.SaldoCompras = columnas[2].replace("</a>", "").substring(columnas[2].replace("</a>", "").lastIndexOf(">")+1);
                                    lineaPago.SaldoOtrasActividades = columnas[3].substring(columnas[3].lastIndexOf(">")+1);
                                    lineaPago.SaldoPagoCheckOut = columnas[4].replace("</a>", "").substring(columnas[4].replace("</a>", "").lastIndexOf(">")+1);
                                    lineaPago.SaldoFinal = columnas[5].substring(columnas[5].lastIndexOf(">")+1);
                                    lineasPago.add(lineaPago);
                                    siParse= true;
                                }
                            }
                        }
                    }
                }
            }catch (Exception ex){

            }

        }

        synchronized(this){
            EvObtencionDatosPago evtDatosPago = new EvObtencionDatosPago();
            evtDatosPago.ProcesoCancelado = this.cancelandoProcesos;
            evtDatosPago.DatosObtenidos = this.cancelandoProcesos?false:siParse;
            evtDatosPago.Pagos = this.cancelandoProcesos?new ArrayList<LineaPagoCheckOut>():lineasPago;
            for(ObtencionDatosPagosListener lst: this.listenersObtencionDatosPago){
                try{
                    lst.ObtencionDatosPago(evtDatosPago);
                }catch (Exception ex){}
            }
        }

    }

    private synchronized void ConmutaEstadoArchivadoPedido (final PedidoCheckOut pedido, EvFinCargaTarsysWebView evt){
        if (pedido != null && !evt.CodigoHtml.equals("")){
            String codigoHtmlNoCR = evt.CodigoHtml.replace("\n", "");
            // Preparamos la expresión regular con la que vamos a obtener los parámetros que pasaremos al sistema...
            String regExFormArchivo = "<form action=\"([^\"]+)\" method=\"POST\" name=\"archiving\" style=\"margin:0px;\">(.*?)</form\\s*>";
            Pattern patRegExArchivo = Pattern.compile(regExFormArchivo,Pattern.CASE_INSENSITIVE);
            Matcher matchArchivado = patRegExArchivo.matcher(codigoHtmlNoCR);
            if (matchArchivado != null){
                if (matchArchivado.find()){
                    final String urlPost = matchArchivado.group(1);
                    String codigoFormulario = matchArchivado.group(2);
                    if (!urlPost.equals("") && !codigoFormulario.equals("")){
                        // Primero, sacamos la información del userToken
                        String regexUserToken = "<input\\s+name=\"userToken\"\\s+type=\"hidden\"\\s+value=\"(.*?)\"\\s*>";
                        String regexToggleArchive = "<input\\s+type=\"submit\"\\s+name=\"toggleArchive\"\\s+value=\"(.*?)\"\\s+id=\"toggleArchive\"\\s+style=\".*?\"\\s*>";
                        String userToken = "";
                        String toggleArchive = "";
                        Pattern patUserToken = Pattern.compile(regexUserToken);
                        Pattern patToggleArchive = Pattern.compile(regexToggleArchive);
                        Matcher matchUserToken = patUserToken.matcher(codigoFormulario);
                        if (matchUserToken != null){
                            if (matchUserToken.find()){
                                try {
                                    userToken = java.net.URLEncoder.encode(matchUserToken.group(1), "UTF-8");
                                } catch (UnsupportedEncodingException ex) {
                                    userToken = matchUserToken.group(1);
                                }

                            }
                        }
                        Matcher matchToggleArchive = patToggleArchive.matcher(codigoFormulario);
                        if (matchToggleArchive != null){
                            if (matchToggleArchive.find()){
                                try {
                                    toggleArchive = java.net.URLEncoder.encode(matchToggleArchive.group(1), "UTF-8");
                                } catch (UnsupportedEncodingException ex) {
                                    toggleArchive = matchToggleArchive.group(1);
                                }
                            }
                        }

                        if (!userToken.equals("") && !toggleArchive.equals("")){
                            // Si el usertoken es distinto de cadena vacía, tenemos todo lo necesario para archivar/desarchivar el pedido...
                            String cadenaPost = String.format("order=%d&userToken=%s&toggleArchive=%s", pedido.NumeroPedido,userToken,toggleArchive);
                            if (!cadenaPost.equals("")){
                                try{
                                    ((TarsysWebView)evt.sender).postUrl(urlPost, cadenaPost.getBytes());
                                }catch (Exception ex){
                                    // En caso de excepción, provocamos el evento de cambio, pero sin cambio...
                                    synchronized(ServicioCheckOutVenta.this){
                                        EvCambioEstadoArchivado evtCambioArchivo = new EvCambioEstadoArchivado();
                                        evtCambioArchivo.Pedido = pedido;
                                        evtCambioArchivo.ProcesoCancelado = ServicioCheckOutVenta.this.cancelandoProcesos;
                                        for(CambioEstadoArchivadoListener ml:ServicioCheckOutVenta.this.listenersCambioEstadoArchivado){
                                            ml.CambioEstadoArchivado(evtCambioArchivo);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Parsea el pedido proporcionado con el código html proporcionado
     * @param pedido
     * @param codigoHtml
     */
    private synchronized boolean ParsePedido(PedidoCheckOut pedido, String codigoHtml){
        boolean datosRecibidos = false;
        if (pedido != null && !codigoHtml.equals("")){
            // Si tengo pedido, y tengo código fuente que parsear...
            String codigoHtmlNoCR = codigoHtml.replace("\n", "").replace("<br/>", "<br>");
            // Primero, obtenemos los datos de logística...
            // <editor-fold defaultstate="collapsed" desc="Obtención de los datos logísticos">
            String regExDireccion = "<tr\\s+id=\"address1\">(.*?)</tr\\s*>";
            Pattern patRegExDireccion = Pattern.compile(regExDireccion, Pattern.CASE_INSENSITIVE);
            Matcher matchDireccion = patRegExDireccion.matcher(codigoHtmlNoCR);
            if (matchDireccion != null){
                while (matchDireccion.find() && !this.cancelandoProcesos){
                    // Mientras que encuentre algo...
                    String codigoDireccion = matchDireccion.group(1);
                    if (!codigoDireccion.equals("")){
                        datosRecibidos = true;
                        try{
                            codigoDireccion = codigoDireccion.substring(codigoDireccion.indexOf("</td>")+5)
                                                                                     .replace("<td>", "").replace("</td>", "")
                                                                                     .replace("<span style=\"white-space: nowrap;\">", "")
                                                                                     .replace("</span>", "").replace("<b>", "").replace("</b>", "");
                            String []datosDireccion = codigoDireccion.split("<br>");
                            if (datosDireccion != null){
                                Pattern patEmail = Pattern.compile("mailto:(\\w*\\@\\w*)");
                                Matcher matchEmail = patEmail.matcher(codigoDireccion);
                                if (matchEmail.find())
                                    pedido.Email = matchEmail.group(1);
                                pedido.PermiteMarketing = !pedido.Email.contains("checkout");
                                pedido.Email = pedido.PermiteMarketing?pedido.Email:"";
                                if (datosDireccion.length == 6 || datosDireccion.length == 7){
                                    // Hay marketing
                                    pedido.Cliente = datosDireccion[0].trim();
                                    pedido.Poblacion = datosDireccion.length == 6?datosDireccion[2].trim():datosDireccion[3].trim();
                                    pedido.Pais = datosDireccion.length == 6?datosDireccion[3].trim():datosDireccion[4].trim();
                                    pedido.Telefono = datosDireccion.length == 6?datosDireccion[4].trim():datosDireccion[5].trim();
                                    //pedido.Email = datosDireccion.length == 6?datosDireccion[5].replace("</a>", "").substring(datosDireccion[5].indexOf(">")+1).trim():datosDireccion[6].replace("</a>", "").substring(datosDireccion[6].indexOf(">")+1).trim();
                                    //pedido.PermiteMarketing = true;

                                }else if (datosDireccion.length == 4 || datosDireccion.length == 5){
                                    // No hay marketing
                                    pedido.Cliente = datosDireccion[0].trim();
                                    pedido.Poblacion = datosDireccion[1].trim();
                                    pedido.Pais = datosDireccion.length == 5?datosDireccion[3].trim():datosDireccion[2].trim();
                                    pedido.Telefono = "";
                                    //pedido.Email = "";
                                    //pedido.PermiteMarketing = false;
                                }
                            }
                        }catch (Exception ex){}
                    }
                }
            }
            // Obtenemos las coordenadas de geoposicionamiento tipo E6
            if (!pedido.Poblacion.equals("") && !pedido.Pais.equals("")){
                int [] coordGPS = Utilidades.CoordenadasGPS(pedido.Poblacion, pedido.Pais);
                if (coordGPS != null){
                    pedido.latitud = coordGPS[0];
                    pedido.longitud = coordGPS[1];
                }
            }
            // </editor-fold>

            // <editor-fold defaultstate="collapsed" desc="Obtención de los datos de detalle del pedido">
            try{
            String regexLineasPedido = "<tr\\s+class=\"lineitemcleaninvoice\"\\s+id=\"[^\"]+\">(.+?)</tr\\s*>";
                Pattern patRegExLineasPedido = Pattern.compile(regexLineasPedido);
                if (patRegExLineasPedido != null){
                    Matcher matchLineasPedido = patRegExLineasPedido.matcher(codigoHtmlNoCR);
                    if (matchLineasPedido != null){
                        while (matchLineasPedido.find() && !this.cancelandoProcesos){
                            String codigoLineaPedido = matchLineasPedido.group(1);
                            if (!codigoLineaPedido.equals("")){
                                String []columnasDetalle = codigoLineaPedido.split("</td>");
                                if (columnasDetalle != null){
                                    double cantidad = Double.parseDouble(columnasDetalle[1].substring(columnasDetalle[1].indexOf(">")+1).replace(",", "."));
                                    String Producto = columnasDetalle[2].contains("&nbsp;-&nbsp;")?columnasDetalle[2].substring(columnasDetalle[2].indexOf(">")+1,columnasDetalle[2].indexOf("&nbsp;-&nbsp;")):columnasDetalle[2];
                                    double importeLinea = 0;
                                    Pattern patImporte = Pattern.compile("\\w*(\\d+\\.?\\d+)");
                                    Matcher matchImporte = patImporte.matcher(columnasDetalle[3].replace(",", "."));
                                    if (matchImporte.find()){
                                        importeLinea = Double.parseDouble(matchImporte.group(1));
                                    }
                                    if (cantidad != 0 && !Producto.equals("")){
                                        // Si la cantidad y el nombre del producto, son distintos de 0 y cadena vacía respectivamente...
                                        LineaPedidoCheckOut lineaPedido = new LineaPedidoCheckOut();
                                        lineaPedido.Cantidad = cantidad;
                                        lineaPedido.Articulo = Producto.trim();
                                        lineaPedido.ImporteNeto = importeLinea;
                                        if (pedido.LineasPedido == null)
                                            pedido.LineasPedido = new ArrayList<LineaPedidoCheckOut>();
                                        pedido.LineasPedido.add(lineaPedido);
                                    }
                                }
                            }
                        }
                    }
                }
            }catch (Exception ex){
                if (ex != null){

                }
            }

            // </editor-fold>

            // <editor-fold defaultstate="collapsed" desc="Obtención de los datos del histórico de acciones del pedido">
            String regexHistorico = "<tr\\s+\\w+=\\\"[^\\\"]+\\\"\\s+class=\"lineitem\".+?\\s*>(.+?)</tr\\s*>";
            Pattern patHistorico = Pattern.compile(regexHistorico,Pattern.CASE_INSENSITIVE);
            Matcher matchHistorico = patHistorico.matcher(codigoHtmlNoCR);
            if (pedido.HistorialPedido == null)
                pedido.HistorialPedido = new ArrayList<String>();
            if (matchHistorico != null){
                while (matchHistorico.find() && !this.cancelandoProcesos){
                    String historico = "";
                    String []codigoHistorico = matchHistorico.group(1).split("</td>");
                    if (codigoHistorico != null){
                        for(String tag:codigoHistorico){

                            String texto = Html.fromHtml(tag.replace("&nbsp;", " ").replace("<br>", "\n")).toString();
                            historico += texto.substring((texto.indexOf(">") >= 0?texto.indexOf(">")+1:0)) + ". ";
                        }
                        pedido.HistorialPedido.add(historico.trim());
                    }
                }
            }


            // </editor-fold>

        }
        // Para terminar, provocamos el evento que indique que el pedido ha sido modificado

        synchronized (this){
            EvObtencionDatosPedido evt = new EvObtencionDatosPedido();
            evt.ProcesoCancelado = this.cancelandoProcesos;
            evt.PedidoObtenido = this.cancelandoProcesos?false:datosRecibidos;
            evt.Pedido = this.cancelandoProcesos?null:pedido;
            for(ObtencionDatosPedidoListener ml: this.listenersObtencionDatosPedido){
                try{
                    ml.ObtencionDatosPedido(evt);
                }catch (Exception ex){}
            }
        }
        return datosRecibidos;
    }

    /**
     * Este método es el encargado de parsear el código fuente, cuando acabe, lanzará un evento de lectura de bandeja de
     * entrada de pedidos de venta
     * @param evt
     */
    private synchronized void ParsingBandejaEntradaVenta(boolean siArchivados, EvFinCargaTarsysWebView evt) {
        String codigoHtml = evt.CodigoHtml;
        codigoHtml = codigoHtml.replace("\n", "").replace("</table>", "</table>\n").replace("</tr>", "</tr>\n");
        String exprReg = "<tr\\s+class=\"inboxline\"\\s+id=\"[^\"]+\"\\s+onmouseover=\"changeCursor\\([^\"]+\\);\"\\s+onmouseout=\"[^\"]+\\);\">(.*?)</tr\\s*>";
        Pattern regExprFila = Pattern.compile(exprReg,Pattern.CASE_INSENSITIVE);
        ArrayList<PedidoCheckOut>PedidosCheckOut = new ArrayList<PedidoCheckOut>();

        if (regExprFila != null){
            Matcher filasEntrada = regExprFila.matcher(codigoHtml);
            while (filasEntrada.find() && !this.cancelandoProcesos){
                String codigoFila = filasEntrada.group(1);
                codigoFila = codigoFila.replace("</td>", "</td>\n");
                if (!codigoFila.equals("")){
                    // En este punto, tenemos el cuerpo de la fila que contiene a los elementos a extraer...

                    PedidoCheckOut pedidoCheckOut = null;
                    String []columnas = codigoFila.split("\n");
                    int idColumna = 0;
                    for (String columna:columnas){
                        if (this.cancelandoProcesos)
                            break;
                        try{
                            String valorCelda = columna.replace("</td>", "");
                            valorCelda = valorCelda.substring(valorCelda.indexOf(">")+1);
                            if (!valorCelda.equals("")){
                                if (pedidoCheckOut == null)
                                    pedidoCheckOut = new PedidoCheckOut();
                                if (!siArchivados){
                                    switch (idColumna){
                                        case 0: // Número de Pedido
                                            pedidoCheckOut.NumeroPedido = Long.parseLong(valorCelda.substring(valorCelda.indexOf("orderNumLink\">") + "orderNumLink\">".length()).replace("</a>", ""));
                                            break;
                                        case 1: // Importe Bruto en Euros
                                            Pattern regExEuro = Pattern.compile("\\d+\\.\\d+");
                                            try{
                                                Matcher m = regExEuro.matcher(valorCelda.replace(",", "."));
                                                if(m.find())
                                                    pedidoCheckOut.ImporteEurosBruto = Double.parseDouble(m.group());
                                            }catch (Exception ex){
                                                pedidoCheckOut.ImporteEurosBruto = 0;
                                            }
                                            break;
                                        case 2: // Importe Bruto en Divisa (si Procede)
                                            if (columnas.length == 8){

                                            }else{
                                                Pattern regExDivisa = Pattern.compile("alt=\"([a-zA-Z]+)(\\d+\\.\\d+)\"");
                                                Pattern regExDivisaD = Pattern.compile("");
                                                try{
                                                    Matcher mD = regExDivisa.matcher(valorCelda.replace(",", "."));
                                                    if (mD.find()){
                                                        pedidoCheckOut.Divisa = mD.group(1).trim();
                                                        pedidoCheckOut.ImporteDivisaBruto = Double.parseDouble(mD.group(2));
                                                    }
                                                }catch (Exception ex){
                                                    pedidoCheckOut.ImporteDivisaBruto = 0;
                                                    pedidoCheckOut.Divisa = "";
                                                }
                                            }
                                            break;
                                        case 3: // TODO: Reservado
                                            if (columnas.length == 8){
                                                pedidoCheckOut.EstadoCarga = valorCelda.contains("icon_charge_none")?EstadoCargaPedido.NoCargado:valorCelda.contains("icon_charge_full")?EstadoCargaPedido.Cargado:valorCelda.contains("icon_charge_half")?EstadoCargaPedido.SemiCargado:valorCelda.contains("icon_cancelled")?EstadoCargaPedido.Cancelado:EstadoCargaPedido.Rechazado;
                                            }
                                            break;
                                        case 4: // Estado de Carga en tarjeta del pedido
                                            if (columnas.length == 8){
                                                pedidoCheckOut.Enviado = valorCelda.contains("icon_ship_full");
                                            }else{
                                                pedidoCheckOut.EstadoCarga = valorCelda.contains("icon_charge_none")?EstadoCargaPedido.NoCargado:valorCelda.contains("icon_charge_full")?EstadoCargaPedido.Cargado:valorCelda.contains("icon_charge_half")?EstadoCargaPedido.SemiCargado:valorCelda.contains("icon_cancelled")?EstadoCargaPedido.Cancelado:EstadoCargaPedido.Rechazado;
                                            }
                                            break;
                                        case 5: // Estado de Envío de pedido
                                            if (columnas.length == 8){
                                                pedidoCheckOut.Archivado = false;
                                            }else{
                                                pedidoCheckOut.Enviado = valorCelda.contains("icon_ship_full");
                                            }
                                            break;
                                        case 6: // Formulario de Archivado.
                                            if (columnas.length == 8){
                                                valorCelda = valorCelda.toLowerCase().contains("cancelado") || valorCelda.toLowerCase().contains("rechazado")?valorCelda.substring(valorCelda.indexOf("&nbsp;")+7):valorCelda;
                                                pedidoCheckOut.Cliente = valorCelda.substring(0,valorCelda.indexOf("&nbsp;")).trim();
                                            }else{
                                                pedidoCheckOut.Archivado = false;
                                            }
                                            break;
                                        case 7: // Nombre de Cliente - Nombre Aplicacion
                                            if (columnas.length == 8){
                                                try {
                                                    // Fecha de Pedido
                                                    pedidoCheckOut.FechaPedido = new SimpleDateFormat("dd/MM/yyyy hh:mm").parse(valorCelda.substring(0,valorCelda.indexOf("GMT")));
                                                } catch (ParseException ex) {

                                                }
                                            }else{
                                                valorCelda = valorCelda.toLowerCase().contains("cancelado") || valorCelda.toLowerCase().contains("rechazado")?valorCelda.substring(valorCelda.indexOf("&nbsp;")+7):valorCelda;
                                                pedidoCheckOut.Cliente = valorCelda.substring(0,valorCelda.indexOf("&nbsp;")).trim();
                                            }
                                            break;
                                        case 8:
                                            if (columnas.length == 9){
                                                try {
                                                    // Fecha de Pedido
                                                    pedidoCheckOut.FechaPedido = new SimpleDateFormat("dd/MM/yyyy hh:mm").parse(valorCelda.substring(0,valorCelda.indexOf("GMT")));
                                                } catch (ParseException ex) {

                                                }
                                            }
                                            break;
                                        }
                                }else{
                                    pedidoCheckOut.Archivado = siArchivados;
                                    switch (idColumna){
                                        case 0: // Número de Pedido
                                            pedidoCheckOut.NumeroPedido = Long.parseLong(valorCelda.substring(valorCelda.indexOf("orderNumLink\">") + "orderNumLink\">".length()).replace("</a>", ""));
                                            break;
                                        case 1: // Importe Bruto en Euros
                                            Pattern regExEuro = Pattern.compile("\\d+\\.\\d+");
                                            try{
                                                Matcher m = regExEuro.matcher(valorCelda.replace(",", "."));
                                                if(m.find())
                                                    pedidoCheckOut.ImporteEurosBruto = Double.parseDouble(m.group());
                                            }catch (Exception ex){
                                                pedidoCheckOut.ImporteEurosBruto = 0;
                                            }
                                            break;
                                        case 2: // Importe Bruto en Divisa (si Procede)
                                            Pattern regExDivisa = Pattern.compile("alt=\"([a-zA-Z]+)(\\d+\\.\\d+)\"");
                                            Pattern regExDivisaD = Pattern.compile("");
                                            try{
                                                Matcher mD = regExDivisa.matcher(valorCelda.replace(",", "."));
                                                if (mD.find()){
                                                    pedidoCheckOut.Divisa = mD.group(1).trim();
                                                    pedidoCheckOut.ImporteDivisaBruto = Double.parseDouble(mD.group(2));
                                                }
                                            }catch (Exception ex){
                                                pedidoCheckOut.ImporteDivisaBruto = 0;
                                                pedidoCheckOut.Divisa = "";
                                            }
                                            break;
                                        case 3: // Estado de Carga en tarjeta del pedido
                                            pedidoCheckOut.EstadoCarga = valorCelda.contains("icon_charge_none")?EstadoCargaPedido.NoCargado:valorCelda.contains("icon_charge_full")?EstadoCargaPedido.Cargado:valorCelda.contains("icon_charge_half")?EstadoCargaPedido.SemiCargado:valorCelda.contains("icon_cancelled")?EstadoCargaPedido.Cancelado:EstadoCargaPedido.Rechazado;
                                            break;
                                        case 4: // Estado de Envío de pedido
                                            pedidoCheckOut.Enviado = valorCelda.contains("icon_ship_full");
                                            break;
                                        case 5: // Nombre de Cliente - Nombre Aplicacion
                                            valorCelda = valorCelda.toLowerCase().contains("cancelado") || valorCelda.toLowerCase().contains("rechazado")?valorCelda.substring(valorCelda.indexOf("&nbsp;")+7):valorCelda;
                                            pedidoCheckOut.Cliente = valorCelda.substring(0,valorCelda.indexOf("&nbsp;")).trim();
                                            break;
                                        case 6:
                                            try {
                                                // Fecha de Pedido
                                                pedidoCheckOut.FechaPedido = new SimpleDateFormat("dd/MM/yyyy hh:mm").parse(valorCelda.substring(0,valorCelda.indexOf("GMT")));
                                            } catch (ParseException ex) {

                                            }
                                            break;
                                    }
                                }
                            }
                        }catch (Exception ex){}
                        idColumna++;
                    }
                    PedidosCheckOut.add(pedidoCheckOut);
                }
            }
        }
        // Lanzamos el evento que notifica que han sido obtenidos los pedidos...
        synchronized(ServicioCheckOutVenta.this) {
            EvObtencionListaPedidosCheckOut evtObtencion = new EvObtencionListaPedidosCheckOut();
            evtObtencion.ProcesoCancelado = this.cancelandoProcesos;
            evtObtencion.Pedidos = this.cancelandoProcesos?new ArrayList<PedidoCheckOut>():PedidosCheckOut;
            for(ObtencionListaPedidosCheckOutListener ml:ServicioCheckOutVenta.this.listenersObtencionListaPedidos) {
                try{
                    ml.ObtencionListaPedidosCheckOut(evtObtencion);
                }catch (Exception ex){}
            }
        }
    }

    private synchronized void ParsingBandejaEntradaVentaSincrono(boolean siArchivados, DefaultHttpClient cliente, String codigoHtml) {
        codigoHtml = codigoHtml.replace("\n", "").replace("</table>", "</table>\n").replace("</tr>", "</tr>\n").replace("<br/>", "<br>");
        String exprReg = "<tr\\s+class=\"inboxline\"\\s+id=\"[^\"]+\"\\s+onmouseover=\"changeCursor\\([^\"]+\\);\"\\s+onmouseout=\"[^\"]+\\);\">(.*?)</tr\\s*>";
        Pattern regExprFila = Pattern.compile(exprReg,Pattern.CASE_INSENSITIVE);
        ArrayList<PedidoCheckOut>PedidosCheckOut = new ArrayList<PedidoCheckOut>();

        if (regExprFila != null){
            Matcher filasEntrada = regExprFila.matcher(codigoHtml);
            while (filasEntrada.find() && !this.cancelandoProcesos){
                String codigoFila = filasEntrada.group(1);
                codigoFila = codigoFila.replace("</td>", "</td>\n");
                if (!codigoFila.equals("")){
                    // En este punto, tenemos el cuerpo de la fila que contiene a los elementos a extraer...

                    PedidoCheckOut pedidoCheckOut = null;
                    String []columnas = codigoFila.split("\n");
                    int idColumna = 0;
                    for (String columna:columnas){
                        if (this.cancelandoProcesos)
                            break;
                        try{
                            String valorCelda = columna.replace("</td>", "");
                            valorCelda = valorCelda.substring(valorCelda.indexOf(">")+1);
                            if (!valorCelda.equals("")){
                                if (pedidoCheckOut == null)
                                    pedidoCheckOut = new PedidoCheckOut();
                                if (!siArchivados){
                                    switch (idColumna){
                                        case 0: // Número de Pedido
                                            pedidoCheckOut.NumeroPedido = Long.parseLong(valorCelda.substring(valorCelda.indexOf("orderNumLink\">") + "orderNumLink\">".length()).replace("</a>", ""));
                                            break;
                                        case 1: // Importe Bruto en Euros
                                            Pattern regExEuro = Pattern.compile("\\d+\\.\\d+");
                                            try{
                                                Matcher m = regExEuro.matcher(valorCelda.replace(",", "."));
                                                if(m.find())
                                                    pedidoCheckOut.ImporteEurosBruto = Double.parseDouble(m.group());
                                            }catch (Exception ex){
                                                pedidoCheckOut.ImporteEurosBruto = 0;
                                            }
                                            break;
                                        case 2: // Importe Bruto en Divisa (si Procede)
                                            if (columnas.length == 8){

                                            }else{
                                                Pattern regExDivisa = Pattern.compile("alt=\"([a-zA-Z]+)(\\d+\\.\\d+)\"");
                                                Pattern regExDivisaD = Pattern.compile("");
                                                try{
                                                    Matcher mD = regExDivisa.matcher(valorCelda.replace(",", "."));
                                                    if (mD.find()){
                                                        pedidoCheckOut.Divisa = mD.group(1).trim();
                                                        pedidoCheckOut.ImporteDivisaBruto = Double.parseDouble(mD.group(2));
                                                    }
                                                }catch (Exception ex){
                                                    pedidoCheckOut.ImporteDivisaBruto = 0;
                                                    pedidoCheckOut.Divisa = "";
                                                }
                                            }
                                            break;
                                        case 3: // TODO: Reservado
                                            if (columnas.length == 8){
                                                pedidoCheckOut.EstadoCarga = valorCelda.contains("icon_charge_none")?EstadoCargaPedido.NoCargado:valorCelda.contains("icon_charge_full")?EstadoCargaPedido.Cargado:valorCelda.contains("icon_charge_half")?EstadoCargaPedido.SemiCargado:valorCelda.contains("icon_cancelled")?EstadoCargaPedido.Cancelado:EstadoCargaPedido.Rechazado;
                                            }
                                            break;
                                        case 4: // Estado de Carga en tarjeta del pedido
                                            if (columnas.length == 8){
                                                pedidoCheckOut.Enviado = valorCelda.contains("icon_ship_full");
                                            }else{
                                                pedidoCheckOut.EstadoCarga = valorCelda.contains("icon_charge_none")?EstadoCargaPedido.NoCargado:valorCelda.contains("icon_charge_full")?EstadoCargaPedido.Cargado:valorCelda.contains("icon_charge_half")?EstadoCargaPedido.SemiCargado:valorCelda.contains("icon_cancelled")?EstadoCargaPedido.Cancelado:EstadoCargaPedido.Rechazado;
                                            }
                                            break;
                                        case 5: // Estado de Envío de pedido
                                            if (columnas.length == 8){
                                                pedidoCheckOut.Archivado = false;
                                            }else{
                                                pedidoCheckOut.Enviado = valorCelda.contains("icon_ship_full");
                                            }
                                            break;
                                        case 6: // Formulario de Archivado.
                                            if (columnas.length == 8){
                                                valorCelda = valorCelda.toLowerCase().contains("cancelado") || valorCelda.toLowerCase().contains("rechazado")?valorCelda.substring(valorCelda.indexOf("&nbsp;")+7):valorCelda;
                                                pedidoCheckOut.Cliente = valorCelda.substring(0,valorCelda.indexOf("&nbsp;")).trim();
                                            }else{
                                                pedidoCheckOut.Archivado = false;
                                            }
                                            break;
                                        case 7: // Nombre de Cliente - Nombre Aplicacion
                                            if (columnas.length == 8){
                                                try {
                                                    // Fecha de Pedido
                                                    pedidoCheckOut.FechaPedido = new SimpleDateFormat("dd/MM/yyyy hh:mm").parse(valorCelda.substring(0,valorCelda.indexOf("GMT")));
                                                } catch (ParseException ex) {

                                                }
                                            }else{
                                                valorCelda = valorCelda.toLowerCase().contains("cancelado") || valorCelda.toLowerCase().contains("rechazado")?valorCelda.substring(valorCelda.indexOf("&nbsp;")+7):valorCelda;
                                                pedidoCheckOut.Cliente = valorCelda.substring(0,valorCelda.indexOf("&nbsp;")).trim();
                                            }
                                            break;
                                        case 8:
                                            if (columnas.length == 9){
                                                try {
                                                    // Fecha de Pedido
                                                    pedidoCheckOut.FechaPedido = new SimpleDateFormat("dd/MM/yyyy hh:mm").parse(valorCelda.substring(0,valorCelda.indexOf("GMT")));
                                                } catch (ParseException ex) {

                                                }
                                            }
                                            break;
                                        }
                                }else{
                                    pedidoCheckOut.Archivado = siArchivados;
                                    switch (idColumna){
                                        case 0: // Número de Pedido
                                            pedidoCheckOut.NumeroPedido = Long.parseLong(valorCelda.substring(valorCelda.indexOf("orderNumLink\">") + "orderNumLink\">".length()).replace("</a>", ""));
                                            break;
                                        case 1: // Importe Bruto en Euros
                                            Pattern regExEuro = Pattern.compile("\\d+\\.\\d+");
                                            try{
                                                Matcher m = regExEuro.matcher(valorCelda.replace(",", "."));
                                                if(m.find())
                                                    pedidoCheckOut.ImporteEurosBruto = Double.parseDouble(m.group());
                                            }catch (Exception ex){
                                                pedidoCheckOut.ImporteEurosBruto = 0;
                                            }
                                            break;
                                        case 2: // Importe Bruto en Divisa (si Procede)
                                            Pattern regExDivisa = Pattern.compile("alt=\"([a-zA-Z]+)(\\d+\\.\\d+)\"");
                                            Pattern regExDivisaD = Pattern.compile("");
                                            try{
                                                Matcher mD = regExDivisa.matcher(valorCelda.replace(",", "."));
                                                if (mD.find()){
                                                    pedidoCheckOut.Divisa = mD.group(1).trim();
                                                    pedidoCheckOut.ImporteDivisaBruto = Double.parseDouble(mD.group(2));
                                                }
                                            }catch (Exception ex){
                                                pedidoCheckOut.ImporteDivisaBruto = 0;
                                                pedidoCheckOut.Divisa = "";
                                            }
                                            break;
                                        case 3: // Estado de Carga en tarjeta del pedido
                                            pedidoCheckOut.EstadoCarga = valorCelda.contains("icon_charge_none")?EstadoCargaPedido.NoCargado:valorCelda.contains("icon_charge_full")?EstadoCargaPedido.Cargado:valorCelda.contains("icon_charge_half")?EstadoCargaPedido.SemiCargado:valorCelda.contains("icon_cancelled")?EstadoCargaPedido.Cancelado:EstadoCargaPedido.Rechazado;
                                            break;
                                        case 4: // Estado de Envío de pedido
                                            pedidoCheckOut.Enviado = valorCelda.contains("icon_ship_full");
                                            break;
                                        case 5: // Nombre de Cliente - Nombre Aplicacion
                                            valorCelda = valorCelda.toLowerCase().contains("cancelado") || valorCelda.toLowerCase().contains("rechazado")?valorCelda.substring(valorCelda.indexOf("&nbsp;")+7):valorCelda;
                                            pedidoCheckOut.Cliente = valorCelda.substring(0,valorCelda.indexOf("&nbsp;")).trim();
                                            break;
                                        case 6:
                                            try {
                                                // Fecha de Pedido
                                                pedidoCheckOut.FechaPedido = new SimpleDateFormat("dd/MM/yyyy hh:mm").parse(valorCelda.substring(0,valorCelda.indexOf("GMT")));
                                            } catch (ParseException ex) {

                                            }
                                            break;
                                    }
                                }
                            }
                        }catch (Exception ex){}
                        idColumna++;
                    }
                    PedidosCheckOut.add(pedidoCheckOut);
                }
            }
        }
        // Lanzamos el evento que notifica que han sido obtenidos los pedidos...
        synchronized(ServicioCheckOutVenta.this) {
            EvObtencionListaPedidosCheckOut evtObtencion = new EvObtencionListaPedidosCheckOut();
            evtObtencion.ProcesoCancelado = this.cancelandoProcesos;
            evtObtencion.Pedidos = this.cancelandoProcesos?new ArrayList<PedidoCheckOut>():PedidosCheckOut;
            for(ObtencionListaPedidosCheckOutListener ml:ServicioCheckOutVenta.this.listenersObtencionListaPedidos) {
                try{
                    ml.ObtencionListaPedidosCheckOut(evtObtencion);
                }catch (Exception ex){}
            }
        }
    }

    private boolean LoginWebViewCheckOut (TarsysWebView webView, EvFinCargaTarsysWebView evt){
        boolean retorno = true;
        if (evt.CodigoHtml.contains("id=\"gaia_loginform\"")){
            // Si entramos aquí, significa que aún no hemos hecho login...
            // por tanto, prepararemos todo y haremos login...
            // si el código html contiene el id="gaia_loginform" significa que estamos intentando hacer login, por lo que
            // sacaremos los datos necesarios, y luego, haremos un post para recargar...
            String urlAction = "https://accounts.google.com/ServiceLoginAuth";
            String Continue = "";
            String GALX = "";
            String DSH = "";

            Pattern exprContinue = Pattern.compile("input\\s+type=\"hidden\"\\s+name=\"continue\"\\s+id=\"continue\"\\s+value=\"([^\"]+)\"");
            Pattern exprGALX = Pattern.compile("input\\s+type=\"hidden\"\\s+name=\"GALX\"\\s+value=\"([^\"]+)\"");
            Pattern exprDSH = Pattern.compile("input\\s+type=\"hidden\"\\s+name=\"dsh\"\\s+id=\"dsh\"\\s+value=\"(-?\\d+)\"");

            Matcher mtContinue = exprContinue.matcher(evt.CodigoHtml),
                    mtGALX = exprGALX.matcher(evt.CodigoHtml),
                    mtDSH = exprDSH.matcher(evt.CodigoHtml);
            if (mtContinue.find()){
                Continue = mtContinue.group(1);
            }
            if (mtGALX.find()){
                GALX = mtGALX.group(1);
            }
            if (mtDSH.find()){
                DSH = mtDSH.group(1);
            }
            String cadPost = "service=sierra&GALX=" + GALX + "&dsh=" + DSH + "&&timeStmp=&ltml=default&nui=1&pstMsg=0&dnConn=&secTok=&Email=" + ServicioCheckOutVenta.this.usuario + "&Passwd=" + ServicioCheckOutVenta.this.password + "&continue=" + Continue;
            webView.postUrl(urlAction, cadPost.getBytes());
//            try {
//                Thread.sleep(600);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(ServicioCheckOutVenta.class.getName()).log(Level.SEVERE, null, ex);
//            }
            retorno = true;
        }else{
            retorno = false;
        }
        return retorno;
    }

    private String LoginCheckOutSincrono (DefaultHttpClient cliente, String urlContinue, String codigo){
        String retorno = "";

        String urlAction = "https://accounts.google.com/ServiceLoginAuth";
                String Continue = "";
                String GALX = "";
                String DSH = "";

                Pattern exprContinue = Pattern.compile("input\\s+type=\"hidden\"\\s+name=\"continue\"\\s+id=\"continue\"\\s+value=\"([^\"]+)\"");
                Pattern exprGALX = Pattern.compile("input\\s+type=\"hidden\"\\s+name=\"GALX\"\\s+value=\"([^\"]+)\"");
                Pattern exprDSH = Pattern.compile("input\\s+type=\"hidden\"\\s+name=\"dsh\"\\s+id=\"dsh\"\\s+value=\"(-?\\d+)\"");

                Matcher mtContinue = exprContinue.matcher(codigo),
                        mtGALX = exprGALX.matcher(codigo),
                        mtDSH = exprDSH.matcher(codigo);
                if (mtContinue.find()){
                    Continue = mtContinue.group(1);
                }
                if (mtGALX.find()){
                    GALX = mtGALX.group(1);
                }
                if (mtDSH.find()){
                    DSH = mtDSH.group(1);
                }
                String cadPost = "service=sierra&GALX=" + GALX +
                                            "&dsh=" + DSH +
                                            "&timeStmp=&ltml=default&nui=1&pstMsg=0&dnConn=&secTok=&Email=" + ServicioCheckOutVenta.this.usuario +
                                            "&Passwd=" + ServicioCheckOutVenta.this.password +
                                            "&continue=" + Continue;
                HttpPost post = new HttpPost(urlAction);
                ArrayList<BasicNameValuePair> paramsHttp = new ArrayList<BasicNameValuePair>();
                paramsHttp.add(new BasicNameValuePair("service","sierra"));
                paramsHttp.add(new BasicNameValuePair("GALX",GALX));
                paramsHttp.add(new BasicNameValuePair("dsh",DSH));
                paramsHttp.add(new BasicNameValuePair("timeStmp",""));
                paramsHttp.add(new BasicNameValuePair("ltml","default"));
                paramsHttp.add(new BasicNameValuePair("nui","1"));
                paramsHttp.add(new BasicNameValuePair("pstMsg","0"));
                paramsHttp.add(new BasicNameValuePair("dnConn",""));
                paramsHttp.add(new BasicNameValuePair("secTok",""));
                paramsHttp.add(new BasicNameValuePair("Email",this.usuario));
                paramsHttp.add(new BasicNameValuePair("Passwd",this.password));
                paramsHttp.add(new BasicNameValuePair("continue",urlContinue.equals("")?Continue:urlContinue));
                try{
                post.setEntity(new UrlEncodedFormEntity(paramsHttp,HTTP.UTF_8));
                HttpResponse resp = cliente.execute(post);
                HttpEntity ent = resp.getEntity();
                InputStream str = ent.getContent();
                retorno = Utilidades.Stream2String(str);
                }catch (Exception ex){
                    retorno = "";
                }


        return retorno;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Constructores y creadores de instancia de la clase">

    public ServicioCheckOutVenta(){
        this.usuario = "";
        this.password = "";
        this.siConectado = false;
    }

    /**
     * Constructor de la clase
     * @param user Usuario de acceso al servicio checkout
     * @param pass Password de acceso
     */
    public ServicioCheckOutVenta(Context contexto, String user, String pass){
        this.usuario = user;
        this.password = pass;
        this.contextoActual = contexto;
        this.siConectado = false;
    }

    public ServicioCheckOutVenta(Context contexto, long merchant_id, String user, String pass){
        this.usuario = user;
        this.password = pass;
        this.contextoActual = contexto;
        this.siConectado = false;
        this.merchantId = merchant_id;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Propiedades públicas de Lectura / Escritura de la clase">

    /**
     * Obtiene el contexto de trabajo para el que se ha definido el entorno de checkout
     * @return
     */
    public Context ContextoActual(){
        return this.contextoActual;
    }
    /**
     * Establece el contexto de trabajo para el que se va a definir el entorno de checkout
     * @return
     */
    public Context ContextoActual(Context contexto){
        this.contextoActual = contexto;
        return this.contextoActual;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Métodos públicos de ejecución de consultas a CheckOut de la clase">

    /**
     * Determina si el usuario/password establecido para el servicio conecta a los servicios de google
     * @return True si es posible la conexión, false en otro caso.
     */
    public  synchronized void Login(){
        this.siConectado = false;

        try{
            DefaultHttpClient cliente = new DefaultHttpClient();
            HttpPost post = new HttpPost(this.urlLoginCheckout);
            ArrayList<BasicNameValuePair> paramsHttp = new ArrayList<BasicNameValuePair>();
            paramsHttp.add(new BasicNameValuePair("accountType","HOSTED_OR_GOOGLE"));
            paramsHttp.add(new BasicNameValuePair("service","sierra"));
            paramsHttp.add(new BasicNameValuePair("Email",this.usuario));
            paramsHttp.add(new BasicNameValuePair("Passwd",this.password));
            post.setEntity(new UrlEncodedFormEntity(paramsHttp,HTTP.UTF_8));
            HttpResponse resp = cliente.execute(post);
            HttpEntity ent = resp.getEntity();
            InputStream str = ent.getContent();
            String codigo = Utilidades.Stream2String(str);
            this.siConectado = codigo.contains("\nAuth=");
        }catch(Exception ex){
            this.siConectado = false;
        }

        synchronized(ServicioCheckOutVenta.this){
            EvProcesoLogin evtLogin = new EvProcesoLogin();
            evtLogin.LoginRealizado = ServicioCheckOutVenta.this.siConectado;
            evtLogin.Usuario = ServicioCheckOutVenta.this.usuario;
            evtLogin.Password = ServicioCheckOutVenta.this.password;
            for(ProcesoLoginListener ml:ServicioCheckOutVenta.this.listenersProcesoLogin){
                try{
                    ml.ProcesoLogin(evtLogin);
                }catch (Exception ex){}
            }
        }
    }

    /**
     * Cancela una petición de login
     * @param webViewLogin
     */
    public void CancelarLogin(){
        this.siConectado = false;
        // Una vez parado todo, lanzamos el evento de login
        synchronized(ServicioCheckOutVenta.this){
            try{
                ServicioCheckOutVenta.this.RemoveAllProcesoLoginListener();
            }catch (Exception ex){}
        }
    }

    /**
     * * Ejecuta una petición de obtención de Bandeja de Entrada de Ventas de forma asíncrona.
     * El resultado final se devolverá a través del evento ConsultaCheckOut del servicio.
     * @param siArchivados True si debe consultar los pedidos marcados como archivados, false si se deben consultar los que haya en la bandeja de entrada
     */
    public synchronized void BandejaDeEntradaVentasSincro(final boolean siArchivados, int nroPagina){
        if (this.contextoActual != null){
            this.cancelandoProcesos = false;
            try{
                TarsysWebView webViewIB = new TarsysWebView(this.contextoActual);
                webViewIB.addFinCargaTarsysWebListener(new FinCargaTarsysWebViewListener() {
                    public synchronized void FinCargaTarsysWebView(EvFinCargaTarsysWebView evt) {
                        if (!evt.Url.equals("") && !evt.CodigoHtml.equals("")){
                            if (!ServicioCheckOutVenta.this.LoginWebViewCheckOut((TarsysWebView)evt.sender, evt)){
                                if (evt.Url.contains("/sell/orders") || evt.Url.contains("/sell/archive")){
                                        try {
                                            ServicioCheckOutVenta.this.ParsingBandejaEntradaVenta(siArchivados,evt);
                                        } catch (Exception ex) {
                                            if (ex != null){

                                            }
                                        }
                                }
                            }
                        }
                    }
                });
                // Una vez definido el objeto, lanzamos la petición inicial...
                if (!siArchivados)
                    webViewIB.loadUrl(this.urlCheckOutInicial + this.UrlVentasCheckOut + ":" + String.valueOf(nroPagina));
                else
                    webViewIB.loadUrl(this.urlCheckOutInicial + this.UrlVentasCheckOutArchivadas + ":" + String.valueOf(nroPagina));
            }catch(Exception ex){
                if (ex != null){

                }
            }
        }
    }



    public void BandejaEntradaSincrona(boolean siArchivados, int nroPagina){
        DefaultHttpClient cliente = new DefaultHttpClient();
        HttpGet getter = new HttpGet(this.urlCheckOutInicial + (siArchivados?this.UrlVentasCheckOutArchivadas:this.UrlVentasCheckOut) + ":1");
        try {
            HttpResponse respuesta = cliente.execute(getter);
            HttpEntity entidad = respuesta.getEntity();
            String codigo = Utilidades.Stream2String(entidad.getContent());
            if (!codigo.equals("")){
                String codigoChk = this.LoginCheckOutSincrono(cliente, (siArchivados?this.UrlVentasCheckOutArchivadas:this.UrlVentasCheckOut) + ":1", codigo);
                if (!codigoChk.equals("")){
                    this.ParsingBandejaEntradaVentaSincrono(siArchivados,cliente, codigoChk);
                }
            }
        } catch (Exception ex) {

        }

    }

    public boolean ObtenerDatosPedidoSincrono(PedidoCheckOut pedido){
        boolean retorno = false;
        DefaultHttpClient cliente = new DefaultHttpClient();
        HttpGet getter = new HttpGet(this.urlCheckOutInicial + this.UrlVentasCheckOutDatoPedido + String.valueOf(pedido.NumeroPedido));
        try {
            HttpResponse respuesta = cliente.execute(getter);
            HttpEntity entidad = respuesta.getEntity();
            String codigo = Utilidades.Stream2String(entidad.getContent());
            if (!codigo.equals("")){
                String codigoChk = this.LoginCheckOutSincrono(cliente, this.UrlVentasCheckOutDatoPedido + String.valueOf(pedido.NumeroPedido),codigo);
                if (!codigoChk.equals("")){
                    retorno = this.ParsePedido(pedido, codigoChk);
                }
            }
        } catch (Exception ex) {

        }
        return retorno;
    }

    /**
     * Ejecuta una obtención de los datos adicionales de un pedido (datos del cliente, pais, población, teléfono, email, lineas de pedido...
     * @param pedido
     */
    public synchronized  void ObtenerDatosPedido(final PedidoCheckOut pedido) {
        if (this.contextoActual != null){
            this.cancelandoProcesos = false;
            try{
                TarsysWebView wvGestionWeb = new TarsysWebView(this.contextoActual);
                wvGestionWeb = new TarsysWebView(this.contextoActual);
                wvGestionWeb.addFinCargaTarsysWebListener(new FinCargaTarsysWebViewListener() {
                        public synchronized void FinCargaTarsysWebView(EvFinCargaTarsysWebView evt) {
                            if (!evt.Url.equals("") && !evt.CodigoHtml.equals("")){
                                if (evt.Url.toLowerCase().contains("/sell/multiorder")){
                                    ServicioCheckOutVenta.this.ParsePedido(pedido, evt.CodigoHtml);
                                }
                            }
                        }
                    });
                wvGestionWeb.loadUrl(ServicioCheckOutVenta.this.UrlVentasCheckOutDatoPedido + String.valueOf(pedido.NumeroPedido));
            }catch (Exception ex){
                if (ex != null){
                    Toast.makeText(contextoActual, "Error: " + ex.getMessage(), 4).show();
                }
            }
        }
    }

    /**
     * Envia el pedido proporcionado, cuando finaliza se provoca un evento de envío de pedido
     * @param pedido
     */
    public  void EnviarPedido(final PedidoCheckOut pedido){
        if (this.contextoActual != null){
            this.cancelandoProcesos = false;
            String urlEnvio = "https://checkout.google.com/sell/inboxShippingDlg?order=" + String.valueOf(pedido.NumeroPedido);
            try{

                TarsysWebView wvGestionWeb = new TarsysWebView(this.contextoActual);
                wvGestionWeb.addFinCargaTarsysWebListener(new FinCargaTarsysWebViewListener() {
                        public synchronized void FinCargaTarsysWebView(EvFinCargaTarsysWebView evt) {
                            if (!evt.Url.equals("") && !evt.CodigoHtml.equals("")){
                                if (!ServicioCheckOutVenta.this.LoginWebViewCheckOut((TarsysWebView)evt.sender, evt)){
                                if (evt.Url.toLowerCase().contains("sell/inboxshippingdlg")){
                                        if (evt.CodigoHtml.contains("SHIPPING_DIALOG_FORM")){
                                            String userToken = "", shipButton = "", ship = "ship", order = String.valueOf(pedido.NumeroPedido);

                                            String regexUserToken = "<input\\s+name=\"userToken\"\\s+type=\"hidden\"\\s+value=\"(.*?)\"\\s*>";
                                            Pattern patUserToken = Pattern.compile(regexUserToken);
                                            Matcher matchUserToken = patUserToken.matcher(evt.CodigoHtml);
                                            if (matchUserToken != null){
                                                if (matchUserToken.find()){
                                                    try {
                                                        userToken = java.net.URLEncoder.encode(matchUserToken.group(1), "UTF-8");
                                                    } catch (UnsupportedEncodingException ex) {
                                                        userToken = matchUserToken.group(1);
                                                    }

                                                }
                                            }
                                            String regexShipButton = "<input\\s+type=\"submit\"\\s+name=\"shipButton\"\\s+value=\"(.*?)\"\\s+class=\"mb\"\\s*>";
                                            Pattern patShipButton = Pattern.compile(regexShipButton);
                                            Matcher matchShipButton = patShipButton.matcher(evt.CodigoHtml);
                                            if (matchShipButton != null){
                                                if (matchShipButton.find()){
                                                    try {
                                                        shipButton = java.net.URLEncoder.encode(matchShipButton.group(1), "UTF-8");
                                                    } catch (UnsupportedEncodingException ex) {
                                                        shipButton = matchShipButton.group(1);
                                                    }
                                                }
                                            }

                                            if (userToken.trim().length() > 0 && shipButton.trim().length() > 0){

                                                /*
                                             * userToken
                                             * carrier_select1 (<select> vacio)
                                             * tracking_number1 (vacio)
                                             * invoiceUrl (https://checkout.google.com/sell/multiOrder?order=<numeroPedido>)
                                             * ordersTable (vacio)
                                             * searchField (vacio)
                                             * backToInbox (vacio)
                                             * shipButton
                                             * ship
                                             * order
                                             */
                                                String datosPost = "userToken=" + userToken + "&carrier_select1=&tracking_number1=&ordersTable=&searchField=&backToInbox=";
                                                datosPost += "&invoiceUrl=https://checkout.google.com/sell/multiOrder?order=" + String.valueOf(pedido.NumeroPedido);
                                                datosPost += "&ship=ship&shipButton=" + shipButton + "&order=" + String.valueOf(pedido.NumeroPedido);

                                                ((TarsysWebView)evt.sender).postUrl("https://checkout.google.com/sell/inboxShippingDlg", datosPost.getBytes());
                                            }
                                        }
                                    }else if (evt.Url.contains("sell/multiOrder") || evt.Url.contains("sell/orders")) {
                                            // Si estamos aquí hemos terminado y debemos provocar el evento...
                                            synchronized(ServicioCheckOutVenta.this){
                                                EvEnvioPedido evtEnvio = new EvEnvioPedido();
                                                evtEnvio.pedido = pedido;
                                                evtEnvio.ProcesoCancelado = ServicioCheckOutVenta.this.cancelandoProcesos;
                                                for(EnvioPedidoListener ml:ServicioCheckOutVenta.this.listenersEnvioPedido){
                                                    ml.EnvioPedido(evtEnvio);
                                                }
                                            }
                                    }
                                }
                            }
                        }
                    });
                wvGestionWeb.loadUrl(this.urlCheckOutInicial + urlEnvio);
            }catch (Exception ex){
                if (ex != null){
                    Toast.makeText(contextoActual, "Error: " + ex.getMessage(), 4).show();
                }
            }
        }
    }

    /**
     * Conmuta el estado de Archivado de un pedido en el sistema CheckOut.
     * @param pedido Pedido al que conmutar su estado de archivado
     */
    public synchronized void ConmutarEstadoArchivadoEnPedido(final PedidoCheckOut pedido){
        if (this.contextoActual != null){
            this.cancelandoProcesos = false;

            try{

                TarsysWebView wvGestionWeb = new TarsysWebView(this.contextoActual);
                wvGestionWeb.addFinCargaTarsysWebListener(new FinCargaTarsysWebViewListener() {
                        public synchronized void FinCargaTarsysWebView(EvFinCargaTarsysWebView evt) {
                            if (!evt.Url.equals("") && !evt.CodigoHtml.equals("")){
                                if (evt.Url.toLowerCase().contains("/sell/multiorder")){
                                    if (evt.CodigoHtml.contains("feedbackMessage") && !evt.CodigoHtml.contains("<div id=\"feedbackMessage\" style=\"visibility:hidden;\">")){
                                        pedido.Archivado = !pedido.Archivado;

                                        // Para terminar, provocamos el evento
                                        synchronized(ServicioCheckOutVenta.this){
                                            EvCambioEstadoArchivado evtCambioArchivo = new EvCambioEstadoArchivado();
                                            evtCambioArchivo.Pedido = pedido;
                                            evtCambioArchivo.ProcesoCancelado = ServicioCheckOutVenta.this.cancelandoProcesos;
                                            for(CambioEstadoArchivadoListener ml:ServicioCheckOutVenta.this.listenersCambioEstadoArchivado){
                                                ml.CambioEstadoArchivado(evtCambioArchivo);
                                            }
                                        }
                                    }else{
                                        ServicioCheckOutVenta.this.ConmutaEstadoArchivadoPedido(pedido, evt);
                                    }
                                }
                            }
                        }
                    });
                wvGestionWeb.loadUrl(ServicioCheckOutVenta.this.UrlVentasCheckOutDatoPedido + String.valueOf(pedido.NumeroPedido));
            }catch (Exception ex){
                if (ex != null){
                    Toast.makeText(contextoActual, "Error: " + ex.getMessage(), 4).show();
                }
            }
        }
    }

    /**
     * Obtiene los datos de pago
     */
    public synchronized void ObtenerDatosPago(){
        if (this.contextoActual != null){
            this.cancelandoProcesos = false;

            TarsysWebView wvGestionWeb = new TarsysWebView(this.contextoActual);
            if (wvGestionWeb != null){
                wvGestionWeb.addFinCargaTarsysWebListener(new FinCargaTarsysWebViewListener() {

                    public void FinCargaTarsysWebView(EvFinCargaTarsysWebView evt) {
                        if (!evt.Url.equals("") && !evt.CodigoHtml.equals("")){
                        if (!ServicioCheckOutVenta.this.LoginWebViewCheckOut((TarsysWebView)evt.sender, evt)){
                            if (evt.Url.contains("sell/payouts")){
                                    try {
                                        ServicioCheckOutVenta.this.ParsePagos(evt);
                                    } catch (Exception ex) {
                                        if (ex != null){
                                            synchronized (ServicioCheckOutVenta.this){
                                                for (ObtencionDatosPagosListener e:ServicioCheckOutVenta.this.listenersObtencionDatosPago){
                                                    EvObtencionDatosPago ev = new EvObtencionDatosPago();
                                                    ev.DatosObtenidos = false;
                                                    ev.Pagos = new ArrayList<LineaPagoCheckOut>();
                                                    e.ObtencionDatosPago(ev);
                                                }
                                            }
                                        }
                                    }
                            }
                        }
                    }
                    }
                });
                wvGestionWeb.loadUrl(this.urlCheckOutInicial + this.UrlVentasCheckOutPagos);
            }
        }
    }

    /**
     * Cancela los procesos de parsing que pudiese ver activos
     */
    public synchronized void CancelarProcesoActual(){
        this.cancelandoProcesos = true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Métodos públicos de ejecución de consultas a CheckOut usando CSV's">

    public ArrayList<PedidoCheckOut> ObtencionDatosBandejaEntrada (final boolean siArchivados){
        ArrayList<PedidoCheckOut> retorno = new ArrayList<PedidoCheckOut>();

        if (this.merchantId > 0){

            if (this.contextoActual != null){
                this.cancelandoProcesos = false;
                try{
                    final TarsysWebView webViewIB = new TarsysWebView(this.contextoActual);
                    webViewIB.addFinCargaTarsysWebListener(new FinCargaTarsysWebViewListener() {
                        public synchronized void FinCargaTarsysWebView(EvFinCargaTarsysWebView evt) {
                            if (!evt.Url.equals("") && !evt.CodigoHtml.equals("")){
                                if (!ServicioCheckOutVenta.this.LoginWebViewCheckOut((TarsysWebView)evt.sender, evt)){
                                    if (evt.Url.contains("/sell/orders") || evt.Url.contains("/sell/archive")){
                                            try {
                                                // Si estamos aquí, hemos conectado, y estamos autenticados... por lo que podremos trabajar...
                                                if (ServicioCheckOutVenta.this.merchantId > 0){
                                                    // Si el merchant id es válido...
                                                    String url = ServicioCheckOutVenta.this.urlCvs.replace("@MERCHANTID@", String.valueOf(ServicioCheckOutVenta.this.merchantId));
                                                    String cadenaPost = "";
                                                    cadenaPost += "start-date=" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "&";
                                                    cadenaPost += "end-date=" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "&";
                                                    cadenaPost += "date-time-zone=Europe/Paris&";
                                                    cadenaPost += "_type=order-list-request&";
                                                    cadenaPost += "query-type=&";
                                                    cadenaPost += "column-style=EXPANDED";

                                                    ((TarsysWebView)evt.sender).postUrl(url, cadenaPost.getBytes());

                                                }

                                            } catch (Exception ex) {
                                                if (ex != null){

                                                }
                                            }
                                    }
                                }
                            }
                        }
                    });
                    // Una vez definido el objeto, lanzamos la petición inicial...
                    if (!siArchivados)
                        webViewIB.loadUrl(this.urlCheckOutInicial + this.UrlVentasCheckOut + ":1");
                    else
                        webViewIB.loadUrl(this.urlCheckOutInicial + this.UrlVentasCheckOutArchivadas + ":1" );
                }catch(Exception ex){
                    if (ex != null){

                    }
                }
            }

        }



        return retorno;
    }

private void writeToFile(String fileName, InputStream iStream,
    boolean createDir)
    throws IOException
  {
    String me = "FileUtils.WriteToFile";
    if (fileName == null)
    {
      throw new IOException(me + ": filename is null");
    }
    if (iStream == null)
    {
      throw new IOException(me + ": InputStream is null");
    }

    File theFile = new File(fileName);

    // Check if a file exists.
    if (theFile.exists())
    {
       String msg =
         theFile.isDirectory() ? "directory" :
         (! theFile.canWrite() ? "not writable" : null);
       if (msg != null)
       {
         throw new IOException(me + ": file '" + fileName + "' is " + msg);
       }
    }

    // Create directory for the file, if requested.
    if (createDir && theFile.getParentFile() != null)
    {
      theFile.getParentFile().mkdirs();
    }

    // Save InputStream to the file.
    BufferedOutputStream fOut = null;
    try
    {
      fOut = new BufferedOutputStream(new FileOutputStream(theFile));
      byte[] buffer = new byte[32 * 1024];
      int bytesRead = 0;
      while ((bytesRead = iStream.read()) != -1)
      {
        fOut.write(buffer, 0, bytesRead);
      }
    }
    catch (Exception e)
    {
      throw new IOException(me + " failed, got: " + e.toString());
    }
    finally
    {
      close(iStream, fOut);
    }
  }

private void close(InputStream iStream, OutputStream oStream)
    throws IOException
  {
    try
    {
      if (iStream != null)
      {
        iStream.close();
      }
    }
    finally
    {
      if (oStream != null)
      {
        oStream.close();
      }
    }
  }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Métodos públicos de gestión de eventos">

    /**
     * Agrega un nuevo gestor de eventos de obtencion de la lista de pedidos checkout
     * @param hearer
     */
    public synchronized void addObtencionListaPedidosCheckOutListener(ObtencionListaPedidosCheckOutListener hearer) {
        this.listenersObtencionListaPedidos.add(hearer);
    }

    /**
     * Elimina el gestor de eventos de obtencion de la lista de pedidos checkout
     * @param hearer
     */
    public synchronized void removeObtencionListaPedidosCheckOutListener(ObtencionListaPedidosCheckOutListener hearer) {
        this.listenersObtencionListaPedidos.remove(hearer);
    }

    public synchronized void removeAllObtencionListaPedidosCheckOutListener() {
        this.listenersObtencionListaPedidos.clear();
    }

    /**
     * Agrega un nuevo gestor de eventos de obtencion de la lista de pedidos checkout
     * @param hearer
     */
    public synchronized void addProcesoLoginListener(ProcesoLoginListener hearer) {
        this.listenersProcesoLogin.add(hearer);
    }

    /**
     * Elimina el gestor de eventos de obtencion de la lista de pedidos checkout
     * @param hearer
     */
    public synchronized void removeProcesoLoginListener(ProcesoLoginListener hearer) {
        this.listenersProcesoLogin.remove(hearer);
    }

    public synchronized void RemoveAllProcesoLoginListener(){
        this.listenersProcesoLogin.clear();
    }

    /**
     * Agrega un nuevo gestor de eventos de obtencion de datos adicionales para un pedido checkout
     * @param hearer
     */
    public synchronized void addObtencionDatosPedidoListener(ObtencionDatosPedidoListener hearer) {
        this.listenersObtencionDatosPedido.add(hearer);
    }

    /**
     * Elimina el gestor de eventos de obtencion de datos adicionales para un pedido checkout
     * @param hearer
     */
    public synchronized void removeObtencionDatosPedidoListener(ObtencionDatosPedidoListener hearer) {
        this.listenersObtencionDatosPedido.remove(hearer);
    }

    /**
     * Elimina todos los  gestores de eventos de obtencion de datos adicionales para un pedido checkout
     */
    public synchronized void removeObtencionDatosPedidoListener(){
        this.listenersObtencionDatosPedido.clear();
    }

    /**
     * Agrega un nuevo gestor de eventos de obtencion de datos de Pagos de CheckOut
     * @param hearer
     */
    public synchronized void addObtencionDatosPagosListener(ObtencionDatosPagosListener hearer) {
        this.listenersObtencionDatosPago.add(hearer);
    }

    /**
     * Elimina el gestor de eventos de obtencion de datos de Pagos de CheckOut
     * @param hearer
     */
    public synchronized void removeObtencionDatosPagosListener(ObtencionDatosPagosListener hearer) {
        this.listenersObtencionDatosPago.remove(hearer);
    }

    /**
     * Elimina todos los  gestores de eventos de obtencion de datos de Pagos de CheckOut
     */
    public synchronized void removeObtencionDatosPagosListener(){
        this.listenersObtencionDatosPago.clear();
    }

    /**
     * Agrega un nuevo gestor de eventos de obtencion de datos de Pagos de CheckOut
     * @param hearer
     */
    public synchronized void addCambioEstadoArchivadoListener(CambioEstadoArchivadoListener hearer) {
        this.listenersCambioEstadoArchivado.add(hearer);
    }

    /**
     * Elimina el gestor de eventos de obtencion de datos de Pagos de CheckOut
     * @param hearer
     */
    public synchronized void removeCambioEstadoArchivadoListener(CambioEstadoArchivadoListener hearer) {
        this.listenersCambioEstadoArchivado.remove(hearer);
    }

    /**
     * Elimina todos los  gestores de eventos de obtencion de datos de Pagos de CheckOut
     */
    public synchronized void removeCambioEstadoArchivadoListener(){
        this.listenersCambioEstadoArchivado.clear();
    }

    /**
     * Agrega un nuevo gestor de eventos de Envio de pedidos
     * @param hearer
     */
    public synchronized void addEnvioPedidoListener(EnvioPedidoListener hearer) {
        this.listenersEnvioPedido.add(hearer);
    }

    /**
     * Elimina el gestor de eventos de Envio de pedido
     * @param hearer
     */
    public synchronized void removeEnvioPedidoListener(EnvioPedidoListener hearer) {
        this.listenersEnvioPedido.remove(hearer);
    }

    /**
     * Elimina todos los  gestores de eventos de Envio de pedidos
     */
    public synchronized void removeEnvioPedidoListener(){
        this.listenersEnvioPedido.clear();
    }

    // </editor-fold>

}
