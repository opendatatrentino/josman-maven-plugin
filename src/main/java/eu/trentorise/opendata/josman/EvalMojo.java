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


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Evaluates all $eval{expr} expressions in markdown files under {@code /docs},
 * and stores the evaluation results in
 * {@link JosmanProject#TARGET_EVAL_FILEPATH}
 *
 * @since 0.8.0
 */
@Mojo(name = "eval", requiresDependencyResolution = ResolutionScope.TEST, defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class EvalMojo extends JosmanMojo {

    /**
     * @since 0.8.0
     */
    public EvalMojo() {
        super("JOSMAN EVAL");
    }

    @Override
    public void execute() throws MojoExecutionException {

        JosmanProject josman = loadProjectInfo();

        try {
            josman.evalDocs();
        } catch (Exception ex) {
            throw new MojoExecutionException("JOSMAN: Error while creating evaluations file!", ex);
        }

        info("");
        info("");
        info("Done.");
        info("");
        info("");
    }
}
