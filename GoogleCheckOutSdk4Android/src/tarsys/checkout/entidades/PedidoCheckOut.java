/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tarsys.checkout.entidades;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import tarsys.checkout.servicio.enumerados.EstadoCargaPedido;

/**
 *
 * @author TaRRaDeLo
 */
public class PedidoCheckOut  implements Serializable{
    public long NumeroPedido = 0;
    public Date FechaPedido = new Date();
    public double ImporteEurosBruto = 0;
    public double ImporteDivisaBruto = 0;
    public String Divisa = "";
    public EstadoCargaPedido EstadoCarga = EstadoCargaPedido.NoCargado;
    public boolean Enviado = false;
    public boolean Archivado = false;    
    public String Cliente = "";
    public String Poblacion = "";
    public String Pais = "";
    public String Telefono = "";   
    public String Email = ""; 
    public int latitud = 0;
    public int longitud = 0;
    public ArrayList<LineaPedidoCheckOut> LineasPedido = new ArrayList<LineaPedidoCheckOut>();
    public ArrayList<String>HistorialPedido = new ArrayList<String>();
    
    public boolean PermiteMarketing = false;
    
    public double ImporteEurosNeto(){
        return this.ImporteEurosBruto*0.7;
    }
    public double ImporteDivisaNeto(){
        return this.ImporteDivisaBruto*0.7;
    }
    public double ImporteTasaEuros(){
        return this.ImporteEurosBruto*0.3;
    }
    public double ImporteTasaDivisa(){
        return this.ImporteDivisaBruto*0.3;
    }
    
    
    
}
    