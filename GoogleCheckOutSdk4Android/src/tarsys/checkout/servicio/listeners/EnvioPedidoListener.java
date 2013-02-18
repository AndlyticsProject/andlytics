/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tarsys.checkout.servicio.listeners;

import java.util.EventListener;
/**
 *
 * @author tarradelo
 */
public interface EnvioPedidoListener extends EventListener{
    void EnvioPedido(EvEnvioPedido evt);
}
