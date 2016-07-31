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
 * A runtime exception to raise when something is not found.
 * 
 * @since 0.8.0
 */
public class JosmanNotFoundException extends JosmanException {
    
    private static final long serialVersionUID = 1L;

    private JosmanNotFoundException(){
        super();
    }
    
    /**
     * Creates the exception using the provided throwable
     * 
     * @since 0.8.0
     */
    public JosmanNotFoundException(Throwable tr) {
        super(tr);
    }

    /**
     * Creates the exception using the provided message and throwable
     * 
     * @since 0.8.0
     */
    public JosmanNotFoundException(String msg, Throwable tr) {
        super(msg, tr);
    }

    /**
     * Creates the exception using the provided message
     * 
     * @since 0.8.0
     */
    public JosmanNotFoundException(String msg) {
        super(msg);
    }
}
