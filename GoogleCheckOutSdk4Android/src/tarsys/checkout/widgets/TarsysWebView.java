/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tarsys.checkout.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Message;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import tarsys.checkout.widgets.listeners.EvFinCargaTarsysWebView;
import tarsys.checkout.widgets.listeners.FinCargaTarsysWebViewListener;

/**
 *
 * @author TaRRaDeLo
 */
public class TarsysWebView extends WebView
{
    /**
     * Clase que implementa un control WebViewClient personalizado que hace que el webView abra en sí mismo las webs,
     * y no en el navegador externo por defecto
     */
    private class TsWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return false;
        }
    }

    /**
     * Clase que utilizaremos como base para poder obtener el código html de la página
     */
    private class TsJavaScriptInterface {

        @SuppressWarnings("unused")
        public void showHTML(String url,String html)
        {
            EvFinCargaTarsysWebView evt = new EvFinCargaTarsysWebView();
            evt.sender = TarsysWebView.this;
            evt.Url = url;
            evt.CodigoHtml = html;
            // Ejecutamos el evento de fin de carga...
            synchronized(TarsysWebView.this) {
                for(FinCargaTarsysWebViewListener ml:TarsysWebView.this.listenersFinCarga) {
                    ml.FinCargaTarsysWebView(evt);
                }
            }
        }
    }
    private java.util.ArrayList<FinCargaTarsysWebViewListener> listenersFinCarga = new java.util.ArrayList<FinCargaTarsysWebViewListener>();
    private TsWebViewClient clienteWeb = null;
    private Context contextoActual = null;
    private TsJavaScriptInterface interfazJs = null;

    /**
     * Constructor de la clase
     * @param contexto Contexto en el que se crea
     */
    public TarsysWebView (Context contexto){
        super(contexto);
        this.contextoActual = contexto;

        this.clienteWeb = new TsWebViewClient(){

            @Override
            public void onFormResubmission(WebView view, Message dontResend, Message resend) {
                super.onFormResubmission(view, dontResend, resend);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                super.onReceivedHttpAuthRequest(view, handler, host, realm);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                super.onReceivedSslError(view, handler, error);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                String urlWeb = view.getUrl();

                TarsysWebView.this.loadUrl("javascript:window.SalidaHtml.showHTML('" + urlWeb + "',document.getElementsByTagName('html')[0].innerHTML);");
            }

        };
        this.getSettings().setJavaScriptEnabled(true);
        //this.getSettings().setBlockNetworkImage(true);
        this.addJavascriptInterface(new TsJavaScriptInterface(), "SalidaHtml");
        this.setWebViewClient(this.clienteWeb);

    }




    /**
     * Agrega un nuevo gestor de eventos cuando finaliza la carga de la página
     * @param hearer
     */
    public synchronized void addFinCargaTarsysWebListener(FinCargaTarsysWebViewListener hearer) {
        this.listenersFinCarga.add(hearer);
    }

    /**
     * Elimina el gestor de eventos de finalización de carga de página especificado
     * @param hearer
     */
    public synchronized void removeFinCargaTarsysWebListener(FinCargaTarsysWebViewListener hearer) {
        this.listenersFinCarga.remove(hearer);
    }

    /**
     * Elimina todos los listeners de notificación de Fin de Carga
     */
    public synchronized void removeAllFinCargaTarsysWebListener() {
        this.listenersFinCarga.clear();
    }
}
