package org.lantern;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Whitelist {

    private final static Logger LOG = LoggerFactory.getLogger(Whitelist.class);
    
    private static final String WHITELIST_NAME = "whitelist.txt";
    private static final String REPORTED_WHITELIST_NAME = 
        "reportedWhitelist.txt";
    
    private static final File WHITELIST_FILE = 
        new File(LanternUtils.configDir(), WHITELIST_NAME);
    private static final File REPORTED_WHITELIST_FILE = 
        new File(LanternUtils.configDir(), REPORTED_WHITELIST_NAME);

    static {
        if (!WHITELIST_FILE.isFile()) {
            final File original = new File(WHITELIST_NAME);
            try {
                FileUtils.copyFile(original, WHITELIST_FILE);
            } catch (final IOException e) {
                LOG.error("Could not copy original whitelist?", e);
            }
        }
        if (!REPORTED_WHITELIST_FILE.isFile()) {
            try {
                REPORTED_WHITELIST_FILE.createNewFile();
            } catch (final IOException e) {
                LOG.error("Could not create reported whitelist file?", e);
            }
        }
    }    
    
    private static final Collection<String> whitelist = 
        buildWhitelist(WHITELIST_FILE);
    private static Collection<String> lastReportedWhitelist =
        buildWhitelist(REPORTED_WHITELIST_FILE);
    
    public static boolean isWhitelisted(final String uri,
        final Collection<String> wl) {
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
        return wl.contains(toMatch);
    }
    
    /**
     * Decides whether or not the specified full URI matches domains for our
     * whitelist.
     * 
     * @return <code>true</code> if the specified domain matches domains for
     * our whitelist, otherwise false.
     */
    public static boolean isWhitelisted(final String uri) {
        LOG.info("Parsing full URI: {}", uri);
        return isWhitelisted(uri, whitelist);
    }
    
    public static void addEntry(final String entry) {
        whitelist.add(entry);
        write(whitelist, WHITELIST_FILE);
    }
    
    private static void write(final Collection<String> list, final File file) {
        try {
            FileUtils.writeLines(file, "UTF-8", list, "UTF-8");
        } catch (final IOException e) {
            LOG.error("Could not write to file?", e);
        }
    }

    public static void removeEntry(final String entry) {
        whitelist.remove(entry);
        write(whitelist, WHITELIST_FILE);
    }
    
    public static Collection<String> getAdditions() {
        final Collection<String> additions = new HashSet<String>();
        synchronized (whitelist) {
            synchronized (lastReportedWhitelist) {
                for (final String entry : whitelist) {
                    if (!lastReportedWhitelist.contains(entry)) {
                        additions.add(entry);
                    }
                }
            }
        }
        return additions;
    }
    
    public static Collection<String> getRemovals() {
        final Collection<String> removals = new HashSet<String>();
        synchronized (whitelist) {
            synchronized (lastReportedWhitelist) {
                for (final String entry : lastReportedWhitelist) {
                    if (!whitelist.contains(entry)) {
                        removals.add(entry);
                    }
                }
            }
        }
        return removals;
    }
    
    private static Collection<String> buildWhitelist(final File file) {
        final Collection<String> wl = new HashSet<String>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String site = br.readLine();
            while (site != null) {
                site = site.trim();
                if (StringUtils.isNotBlank(site)) {
                    // Ignore commented-out sites.
                    if (!site.startsWith("#")) {
                        wl.add(site);
                    }
                }
                else {
                    break;
                }
                site = br.readLine();
            }
        } catch (final FileNotFoundException e) {
            LOG.error("Could not find whitelist file!!", e);
        } catch (final IOException e) {
            LOG.error("Could not read whitelist file", e);
        } finally {
            IOUtils.closeQuietly(br);
        }
        return wl;
    }

    public static void whitelistReported() {
        // We basically need to copy the current whitelist to be the last
        // reported whitelist.
        try {
            FileUtils.copyFile(WHITELIST_FILE, REPORTED_WHITELIST_FILE);
        } catch (final IOException e) {
            LOG.error("Could not copy whitelist file?");
        }
    }

}