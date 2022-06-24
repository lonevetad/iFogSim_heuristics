package org.fog.placement;

import java.util.Map;
import java.util.Set;

import org.fog.application.AppModule;
import org.fog.entities.FogDevice;

/**
 * Takes inspiration from {@link ModuleMapping}
 * <p>
 * .... it helps in respecting the module -> device association constraints....
 * 
 * @author marcoottina (marco.1995.ottina@gmail.com )
 */
@Deprecated
public class ModuleToDeviceConstraints {

	public ModuleToDeviceConstraints(ModuleMapping moduleMapping) {
		super();
		// TODO do the map
	}

	// TODO : FINISH IT
	/**
	 * Defines the set of {@link AppModule}s that each {@link FogDevice} (whose name
	 * is the key of this map) accepts, i.e. allows to be associated with.
	 */
	protected Map<String, Set<String>> allowedAppModuleEachDevice;

}