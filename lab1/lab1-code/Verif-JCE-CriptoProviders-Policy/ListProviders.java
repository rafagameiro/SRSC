/** 
 * Materiais/Labs para SRSC 17/18
 * Henrique Domingos, 12/3/17
 **/

import java.security.Provider;
import java.security.Security;

/**
 * Mostrar os provedores de criptografia (crypto providers) 
 * que se encontram instalados no JRE (i.e., crypto libs from
 * all crypto providers installed
 */

public class ListProviders
{
    public static void main(String[] args)
    {
        Provider[]	providers = Security.getProviders();

        System.out.println("------------------------------------------");     
        System.out.println("Cripto Providers instalados");     
        System.out.println("------------------------------------------");     
        for (int i = 0; i != providers.length; i++)
        {
            System.out.println("Name: " + providers[i].getName() + Utils.makeBlankString(12 - providers[i].getName().length())+ " Version: " + providers[i].getVersion());
        System.out.println("------------------------------------------");     
            System.out.println("Provider Description: " + providers[i].getInfo());
        }
        System.out.println("------------------------------------------");     
    }
}


