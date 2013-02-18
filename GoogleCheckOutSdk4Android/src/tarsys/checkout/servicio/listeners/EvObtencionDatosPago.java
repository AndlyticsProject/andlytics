/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tarsys.checkout.servicio.listeners;

import java.util.ArrayList;
import tarsys.checkout.entidades.LineaPagoCheckOut;
/**
 *
 * @author tarradelo
 */
public class EvObtencionDatosPago {
    public boolean ProcesoCancelado = false;
    public boolean DatosObtenidos;
    public ArrayList<LineaPagoCheckOut> Pagos;
}
