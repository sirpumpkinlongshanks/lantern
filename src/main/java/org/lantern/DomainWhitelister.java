package org.lantern;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that decides whether or not to proxy specific requests.
 */
public class DomainWhitelister {

    private final static Logger LOG = 
        LoggerFactory.getLogger(DomainWhitelister.class);
    
    
    /**
     * Decides whether or not the specified full URI matches domains for our
     * whitelist.
     * 
     * @return <code>true</code> if the specified domain matches domains for
     * our whitelist, otherwise false.
     */
    public static boolean isWhitelisted(final String uri, 
        final Collection<String> whitelist) {
        LOG.info("Parsing full URI: {}", uri);
        final String afterHttp;
        if (!uri.startsWith("http")) {
            afterHttp = uri;
        } else {
            afterHttp = StringUtils.substringAfter(uri, "://");
        }
        final String base;
        if (afterHttp.contains("/")) {
            base = StringUtils.substringBefore(afterHttp, "/");
        } else {
            base = afterHttp;
        }
        String domainExtension = StringUtils.substringAfterLast(base, ".");
        
        // Make sure we strip alternative ports, like 443.
        if (domainExtension.contains(":")) {
            domainExtension = StringUtils.substringBefore(domainExtension, ":");
        }
        final String domain = StringUtils.substringBeforeLast(base, ".");
        final String toMatchBase;
        if (domain.contains(".")) {
            toMatchBase = StringUtils.substringAfterLast(domain, ".");
        } else {
            toMatchBase = domain;
        }
        final String toMatch = toMatchBase + "." + domainExtension;
        LOG.info("Matching against: {}", toMatch);
        return whitelist.contains(toMatch);
    }
}