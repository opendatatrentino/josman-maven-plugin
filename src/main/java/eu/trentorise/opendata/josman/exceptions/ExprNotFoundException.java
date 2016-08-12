/* 
 * Copyright 2015 Trento Rise  (trentorise.eu) 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.trentorise.opendata.josman.exceptions;

import java.util.logging.Logger;

import eu.trentorise.opendata.josman.JosmanProject;

/**
 * A runtime exception to raise when something is not found.
 * 
 * @since 0.8.0
 */
public class ExprNotFoundException extends JosmanException {
    
    private static final Logger LOG = Logger.getLogger(ExprNotFoundException.class.getName());
    
    private static final long serialVersionUID = 1L;

    private String expr;

    private String relPath;

    /**
     * @since 0.8.0
     */    
    private ExprNotFoundException(){
        super();
    }
    
    /**
     * @since 0.8.0
     */
    public String getExpr(){
        return expr;
    }
    
    /**
     * @since 0.8.0
     */
    public String getRelPath(){
        return relPath;
    }
    
    /**
     * Creates the exception using the provided throwable
     * 
     * @param relPath the path to the file where the proble originates
     * 
     * @since 0.8.0
     */
    public ExprNotFoundException(String msg, String expr, String relPath, Throwable tr) {
        super(msg, tr);
        setExpr(expr);
        setRelPath(relPath);
    }
    
    /**
     * @since 0.8.0
     */    
    private void setRelPath(String relPath){
        if (relPath == null){
            LOG.severe("Found null path, setting it to blank!");
            this.relPath = "";
        } else {
            this.relPath = relPath;
        }
    }
    
    
    /**
     * @since 0.8.0
     */    
    private void setExpr(String expr){
        if (expr == null){
            LOG.severe("Found null expr, setting it to blank!");
            this.expr = "";
        } else {
            this.expr = expr;
        }
    }


    /**
     * Creates the exception using the provided message
     * 
     * @param relPath the path to the file where the proble originates
     * @since 0.8.0
     */
    public ExprNotFoundException(String msg, String expr, String relPath) {
        super(msg);
        setExpr(expr);
        setRelPath(relPath);
    }
}
