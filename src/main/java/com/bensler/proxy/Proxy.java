package com.bensler.proxy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.eclipse.jetty.rewrite.handler.ProxyRule;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ajax.JSON;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class Proxy extends Object {

  private static final Logger LOG = Log.getLogger(Proxy.class);

  public static final String RULES_FILE  = "ProxyRules.json";
  
  public static final String PORT        = "port";
  public static final String RULES       = "rules";
  public static final String MATCHES     = "matches";
  public static final String TARGET_HOST = "target-host";
  public static final String TARGET_PATH = "target-path";
  
  public static class Main {
    
    public static void main(String[] args) throws Exception {
      new Proxy();
    }
    
  }
    
  Proxy() throws Exception {
    configureServer(readJson()).start();
  }

  private Server configureServer(Map<?, ?> jsonCfg) {
    final Number port = (Number) jsonCfg.get(PORT);
    final Object[] rules = (Object[]) jsonCfg.get(RULES);
    final Server server;
    final RewriteHandler rewriteHandler;
    
    if (port == null) {
      throw new IllegalArgumentException(PORT + " missing");
    }
    if (rules == null) {
      throw new IllegalArgumentException(RULES + " array missing");
    }

    LOG.info("listening on localhost:{}", port.intValue());
    server = new Server(port.intValue());
    rewriteHandler = new RewriteHandler();
    for (Object rule : rules) {
      createRule(rewriteHandler, (Map<?, ?>) rule);
    }
    server.setHandler(rewriteHandler);
    return server;
  }

  private void createRule(RewriteHandler rewriteHandler, Map<?, ?> rule) {
    final String matches = (String)rule.get(MATCHES);
    final String targetHost = (String) rule.get(TARGET_HOST);
    final String targetPath = (String) rule.get(TARGET_PATH);
    
    if ((matches == null) || matches.isEmpty()) {
      throw new IllegalArgumentException(MATCHES + " missing");
    }
    if ((targetHost == null) || targetHost.isEmpty()) {
      throw new IllegalArgumentException(TARGET_HOST + " missing");
    }
    rewriteHandler.addRule(createRule(
      matches, targetHost,
      ((targetPath == null) ? "" : targetPath)
    ));
  }
  
  private ProxyRule createRule(String ruleStr, String host, String path) {
    final String targetUrl = "http://" + host + (path.isEmpty() ? "" : "/") + path;
    final ProxyRule rule = new ProxyRule();
    
    LOG.info("proxying {} -> {}", ruleStr, targetUrl);
    rule.setPattern(ruleStr);
    rule.setTerminating(false);
    rule.setHostHeader(host);
    rule.setProxyTo(targetUrl);
    return rule;
  }

  private Map<?, ?> readJson() throws IOException {
    final InputStream is = getClass().getResourceAsStream("/" + RULES_FILE);
    
    if (is == null) {
      throw new FileNotFoundException(RULES_FILE);
    }
    return (Map<?, ?>)JSON.parse(new InputStreamReader(is));
  }

}
