/*
 * Copyright 2015  Trento Rise  (trentorise.eu)
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
package eu.trentorise.opendata.josman;

import eu.trentorise.opendata.josman.JosmanProject;

import java.net.URL;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * This mojo creates a site with documentation for all released versions of a
 * GitHub repository. The idea is that if MarkDown relative links work when in
 * Github, they should also work in the generated website.
 *
 */
@Mojo(name = "site", requiresDependencyResolution=ResolutionScope.TEST)
public class SiteMojo extends JosmanMojo {
    
    
    public SiteMojo() {
        super("JOSMAN SITE");
    }
    

    @Override
    public void execute() throws MojoExecutionException {

        JosmanProject josman = loadProjectInfo();      
        
        try {
            josman.generateSite();
        }
        catch (Exception ex) {
            throw new MojoExecutionException("\n\n  !!!!!!   JOSMAN: ERROR WHILE CREATING THE SITE !!!!!\n\n", ex);
        }
        info("");
        info("");
        info("Done.");
        info("");
        info("");
    }
}
