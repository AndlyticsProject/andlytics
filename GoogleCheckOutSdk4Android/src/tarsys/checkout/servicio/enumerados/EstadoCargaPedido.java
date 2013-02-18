/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tarsys.checkout.servicio.enumerados;

import java.io.Serializable;
import tarsys.checkout.sdk.R;

/**
 *
 * @author TaRRaDeLo
 */
public enum EstadoCargaPedido  implements Serializable{
    NoCargado {public int value(){return 0;} public int resId(){return R.string.EstadoNoCargado;}},
    Rechazado {public int value(){return 1;}public int resId(){return R.string.EstadoRechazado;}},
    SemiCargado {public int value(){return 2;}public int resId(){return R.string.EstadoSemiCargado;}},
    Cargado {public int value(){return 3;}public int resId(){return R.string.EstadoCargado;}},
    Cancelado {public int value(){return 4;}public int resId(){return R.string.EstadoCancelado;}},
    CargadoNoEnviado {public int value(){return 5;}public int resId(){return R.string.EstadoCargadoNoEnviado;}};

    public abstract int value();
    public abstract int resId();

    public static EstadoCargaPedido valueOf(int dato){
        EstadoCargaPedido retorno = NoCargado;

        switch(dato){
            case 0:
                retorno = NoCargado;
                break;
            case 1:
                retorno = Rechazado;
                break;
            case 2:
                retorno = SemiCargado;
                break;
            case 3:
                retorno = Cargado;
                break;
            case 4:
                retorno = Cancelado;
                break;
            case 5:
                retorno = CargadoNoEnviado;
                break;
        }

        return retorno;
    }
}
