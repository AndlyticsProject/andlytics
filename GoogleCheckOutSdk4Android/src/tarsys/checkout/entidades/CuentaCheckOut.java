/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tarsys.checkout.entidades;

import java.io.Serializable;

/**
 *
 * @author tarradelo
 */
public class CuentaCheckOut implements Serializable {
    private String loginUsuario = "";
    private String passwordUsuario = "";
    private long merchantId = 0;
    private boolean cuentaPorDefecto = false;
    
    /**
     * Constructor de la clase
     * @param login Login de acceso al sistema CheckOut
     * @param password Password de acceso al sistema CheckOut
     * @param id Id del usuario, es recomendable establecer el MerchantId del usuario
     */
    public CuentaCheckOut (String login, String password, long id, boolean porDefecto){
        this.loginUsuario = login;
        this.passwordUsuario = password;
        this.merchantId = id;
        this.cuentaPorDefecto = porDefecto;
    }
    
    public String LoginUsuario(){
        return this.loginUsuario;
    }
    public String PasswordUsuario(){
        return this.passwordUsuario;
    }
    
    public long MerchantId(){
        return this.merchantId;
    }
    
    public boolean CuentaPorDefecto(){
        return this.cuentaPorDefecto;
    }
    
    @Override
    public String toString(){
        return this.loginUsuario;
    }
}
