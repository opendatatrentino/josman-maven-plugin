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

/**
 * A generic runtime exception. 
 * 
 * @since 0.8.0
 */
public class JosmanException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    
    protected JosmanException(){
        super();
    }
    
    /**
     * @since 0.8.0
     */
    public JosmanException(Throwable tr) {
        super(tr);
    }

    /**
     * @since 0.8.0
     */    
    public JosmanException(String msg, Throwable tr) {
        super(msg, tr);
    }

    /**
     * @since 0.8.0
     */    
    public JosmanException(String msg) {
        super(msg);
    }
}
