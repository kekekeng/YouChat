package com.acesse.youchat;

/**
 * Constants.  Build system responsible for replacing the tokens. 
 */
public class Config {

    public final static boolean LOGGING = @CONFIG.LOGGING@;
    public final static String DOMAIN_NODEJS = "@CONFIG.DOMAIN_NODEJS@";
    public final static String DOMAIN_XMPP = "@CONFIG.DOMAIN_XMPP@";
    public final static boolean DEBUG = @CONFIG.DEBUG@;

}
