package com.innerfunction.semo.core;

/**
 * Interface of objects able to construct components from component definitions.
 * @author juliangoacher
 *
 */
public interface ComponentFactory {
    
    /**
     * Make a component from a definition.
     * @param definition    The component definition.
     * @param id            An ID for identifying the new component instance.
     * @return The new component instance, or null if the component couldn't be created.
     */
    public Component makeComponent(Configuration definition, String id);
    
}
