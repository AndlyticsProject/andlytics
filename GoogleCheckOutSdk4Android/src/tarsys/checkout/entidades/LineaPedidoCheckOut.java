/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tarsys.checkout.entidades;

import java.io.Serializable;

/**
 *
 * @author TaRRaDeLo
 */
public class LineaPedidoCheckOut  implements Serializable{
    public String Articulo = "";
    public double Cantidad = 0;
    public double ImporteNeto = 0;
}
