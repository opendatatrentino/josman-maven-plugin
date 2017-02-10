package eu.trentorise.opendata.josman;

/**
 * @since 0.8.0
 *
 */
// see also discussion here: https://github.com/opendatatrentino/josman-maven-plugin/issues/29
public enum JosmanMode {

    /**
     * Default modality: Generates documentation only about current snapshot, 
     * doesn't fail on errors/warnings and doesn't copy javadocs.
     * 
     * @since 0.8.0
     */        
    dev,
    /**
     * Generates documentation only about current snapshot, doesn't fail on errors/warnings
     * and copies javadocs if present. 
     * 
     * @since 0.8.0
     */    
    ci,
    /**
     * Generates all it can about released versions, and fails on errors/warnings.
     * Also generates documentation about the  current snapshot.
     * 
     * @since 0.8.0
     */
    staging,
    
    /**
     * Generates all it can about released versions, and fails on errors/warnings.
     * 
     * @since 0.8.0
     */    
    release,    
}