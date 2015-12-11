package com.innerfunction.semo;

import java.util.List;

import com.innerfunction.util.Regex;
import com.innerfunction.util.StringTemplate;

public class StringRewriteRules {

    public static class Rule {
        
        private Regex patternRegexp;
        private StringTemplate resultTemplate;
        
        public Rule() {}
        
        public void setPattern(String pattern) {
            patternRegexp = new Regex( pattern );
        }
        
        public void setResult(String result) {
            resultTemplate = new StringTemplate( result );
        }
        
        public String rewriteString(String s) {
            String result = null;
            String[] matches = patternRegexp.matches( s );
            if( matches != null ) {
                result = resultTemplate.render( matches );
            }
            return result;
        }
    }
    
    private List<Rule> rules;
    
    public StringRewriteRules() {}
    
    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }
    
    public String rewriteString(String s) {
        String result = null;
        if( rules != null ) {
            for( Rule rule : rules ) {
                result = rule.rewriteString( s );
                if( result != null ) {
                    break;
                }
            }
        }
        return result;
    }
}
