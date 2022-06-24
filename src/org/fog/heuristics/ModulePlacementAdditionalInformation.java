package org.fog.heuristics;

import java.util.Map;

import org.fog.application.AppModule;
import org.fog.entities.FogDevice;
import org.fog.heuristics.fogImplementations.SolutionModulesDeployed;
import org.fog.placement.ModuleMapping;

/**
 * Defines a set of information used while executing {@link Heuristic}s and
 * evaluating the solutions ({@link SolutionModulesDeployed}). <br>
 * 
 * @author marcoottina (marco.1995.ottina@gmail.com )
 *
 */
public interface ModulePlacementAdditionalInformation {

	public int getLatencyBetweenDevices(FogDevice.DeviceNodeType typeFrom, FogDevice.DeviceNodeType typeDestination);

	/**
	 * Each {@link AppModule} may be restricted to be associate with a subset of
	 * {@link FogDevice}s only. This methods returns that subset, if it exists, as a
	 * {@link Map} from {@link FogDevice} identifiers ({@link FogDevice#getName()})
	 * to these devices. If that subset does not exists, then an empty map or simply
	 * {@code null} may be returned.
	 * <p>
	 * An implementation of this interface may use an underlying
	 * {@link ModuleMapping} in order to fetch the required information.
	 * 
	 * @param moduleToAssociatesTo
	 * @return
	 */
	// getDevicesAllowingAssociationWith
	public Map<String, FogDevice> getDevicesAllowingAssociationWith(AppModule moduleToAssociatesTo);

	/**
	 * Closely similar to {@link #getModulesAllowedToAssociateWith(FogDevice)}
	 * 
	 * @param device
	 * @return
	 */
	public Map<String, AppModule> getModulesAllowedToAssociateWith(FogDevice device);
}
