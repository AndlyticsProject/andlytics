/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tarsys.checkout.servicio.enumerados;

/**
 *
 * @author TaRRaDeLo
 */
public enum TipoConsultaCheckOut {
    BandejaEntradaVentas {public int value(){return 0;}},
    ArchivoVentas {public int value(){return 1;}},
    PagosVentas {public int value(){return 2;}},
    PerfilUsuarioVentas {public int value(){return 3;}},
    CorreosAsociadosVentas {public int value(){return 4;}};
    
    
    public abstract int value();
    
    public static TipoConsultaCheckOut valueOf(int dato){
        TipoConsultaCheckOut retorno = BandejaEntradaVentas;
        switch(dato){
            case 0:
                retorno = BandejaEntradaVentas;
                break;
            case 1:
                retorno = ArchivoVentas;
                break;
            case 2:
                retorno = PagosVentas;
                break;
            case 3:                
                retorno = PerfilUsuarioVentas;
                break;
            case 4:
                retorno = CorreosAsociadosVentas;
                break;
        }
        return retorno;
    }
}
