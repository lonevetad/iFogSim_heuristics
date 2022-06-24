package org.fog.heuristics.fogImplementations;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.apache.commons.math3.util.Pair;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDevice.DeviceNodeType;
import org.fog.heuristics.ModulePlacementAdditionalInformation;
import org.fog.heuristics.SolutionsProducerEvaluator;
import org.fog.placement.ModuleMapping;

/**
 * 
 * @author marcoottina (marco.1995.ottina@gmail.com )
 *
 */
public class ModulePlacementAdditionalInformationFog implements ModulePlacementAdditionalInformation {

	public ModulePlacementAdditionalInformationFog(ModuleMapping allowedModulesOnDeviceAssociation,
			Map<String, AppModule> allModulesByName, Map<String, Application> appsByID, List<AppModule> modules,
			List<FogDevice> devices, DeviceNodeTypesLatencyMap latenciesBetweenDeviceTypes) {
		super();
		this.modulesByName = allModulesByName;
		this.latenciesBetweenDeviceTypes = latenciesBetweenDeviceTypes;
		this.devicesByName = null;
		this.devicesPartitions = null;
		this.setDevices(devices);
		this.devicesAllowingAssociation = null;
		this.modulesAllowedToAssociate = null;
		this.setOriginalModuleMapping(allowedModulesOnDeviceAssociation);
	}

	public ModulePlacementAdditionalInformationFog(ModuleMapping allowedModulesOnDeviceAssociation,
			Map<String, AppModule> allModulesByName, Map<String, Application> appsByID, List<AppModule> modules,
			List<FogDevice> devices) {
		this(allowedModulesOnDeviceAssociation, allModulesByName, appsByID, modules, devices,
				new DeviceNodeTypesLatencyMap());
	}

	protected List<AppModule> modules;
	protected List<FogDevice> devices;
	protected Map<String, AppModule> modulesByName;
	protected Map<String, Application> applicationsByID;
	protected Map<String, FogDevice> devicesByName;
	protected ListDevices[] devicesPartitions;
	protected ModuleMapping allowedModulesOnDeviceAssociation;
	protected DeviceNodeTypesLatencyMap latenciesBetweenDeviceTypes;

	/**
	 * The key is the AppModule's name
	 */
	protected Map<String, Map<String, FogDevice>> devicesAllowingAssociation;
	/**
	 * The key is the FogDevice's name. <br>
	 * Holds, for each device, the set of accepted AppModule
	 */
	protected Map<String, Map<String, AppModule>> modulesAllowedToAssociate;

	//

	/**
	 * @return the allowedModulesOnDeviceAssociation
	 */
	public ModuleMapping getOriginalModuleMapping() {
		return allowedModulesOnDeviceAssociation;
	}

	/**
	 * @return the devicesAllowingAssociation
	 */
	public Map<String, Map<String, FogDevice>> getDevicesAllowingAssociation() {
		return devicesAllowingAssociation;
	}

	/**
	 * @return the modulesAllowedToAssociate
	 */
	public Map<String, Map<String, AppModule>> getModulesAllowedToAssociate() {
		return modulesAllowedToAssociate;
	}

	@Override
	public Map<String, FogDevice> getDevicesAllowingAssociationWith(AppModule moduleToAssociatesTo) {
		return this.devicesAllowingAssociation.get(moduleToAssociatesTo.getName());
	}

	@Override
	public Map<String, AppModule> getModulesAllowedToAssociateWith(FogDevice device) {
		return this.modulesAllowedToAssociate.get(device.getName());
	}

	/**
	 * @return the modulesByName
	 */
	public Map<String, AppModule> getModulesByName() {
		return modulesByName;
	}

	/**
	 * Latencies are expressed in milliseconds
	 * 
	 * @return the latenciesBetweenDeviceTypes
	 */
	public DeviceNodeTypesLatencyMap getLatenciesBetweenDeviceTypes() {
		return latenciesBetweenDeviceTypes;
	}

	/**
	 * @return the applicationsByID
	 */
	public Map<String, Application> getApplicationsByID() {
		return applicationsByID;
	}

	/**
	 * @return the devices
	 */
	public List<FogDevice> getDevices() {
		return devices;
	}

	/**
	 * @return the devicesPartitions
	 */
	public ListDevices[] getDevicesPartitions() {
		return devicesPartitions;
	}

	/**
	 * @return the modules
	 */
	public List<AppModule> getModules() {
		return modules;
	}

	/**
	 * @return the devicesByName
	 */
	public Map<String, FogDevice> getDevicesByName() {
		return devicesByName;
	}

//

	/**
	 * @param modules the modules to set
	 */
	public void setModules(List<AppModule> modules) {
		this.modules = modules;
	}

	/**
	 * @param applicationsByID the applicationsByID to set
	 */
	public void setApplicationsByID(Map<String, Application> applicationsByID) {
		this.applicationsByID = applicationsByID;
	}

	/**
	 * @param devices the devices to set
	 */
	public void setDevices(List<FogDevice> devices) {
		this.devices = devices;
		if (this.devicesPartitions != null && devices == null) {
			for (ListDevices ld : this.devicesPartitions) {
				ld.clear();
			}
			this.devicesByName = null;
			this.devicesPartitions = null;
			return;
		}
		if (devices != null) {
			this.devicesPartitions = null;
			this.devicesByName = new HashMap<>();
			for (FogDevice d : devices) {
				this.devicesByName.put(d.getName(), d);
			}
			this.devicesPartitions = SolutionsProducerEvaluator.partitionateDevicesByType(devices);
		}
	}

	/**
	 * See {@link #getLatenciesBetweenDeviceTypes()}
	 * 
	 * @param latenciesBetweenDeviceTypes the latenciesBetweenDeviceTypes to set
	 */
	public void setLatenciesBetweenDeviceTypes(DeviceNodeTypesLatencyMap latenciesBetweenDeviceTypes) {
		this.latenciesBetweenDeviceTypes = latenciesBetweenDeviceTypes;
	}

	/**
	 * @param allowedModulesOnDeviceAssociation the
	 *                                          allowedModulesOnDeviceAssociation to
	 *                                          set
	 */
	public void setOriginalModuleMapping(ModuleMapping allowedModulesOnDeviceAssociation) {
		this.allowedModulesOnDeviceAssociation = allowedModulesOnDeviceAssociation;
		this.resetMapsModuleDevices();
	}

	/**
	 * @param modulesByName the modulesByName to set
	 */
	public void setModulesByName(Map<String, AppModule> modulesByName) {
		this.modulesByName = modulesByName;
	}

	/**
	 * @param devicesAllowingAssociation the devicesAllowingAssociation to set
	 */
	public void setDevicesAllowingAssociation(Map<String, Map<String, FogDevice>> devicesAllowingAssociation) {
		this.devicesAllowingAssociation = devicesAllowingAssociation;
		this.resetMapsModuleDevices();
	}

	/**
	 * @param modulesAllowedToAssociate the modulesAllowedToAssociate to set
	 */
	public void setModulesAllowedToAssociate(Map<String, Map<String, AppModule>> modulesAllowedToAssociate) {
		this.modulesAllowedToAssociate = modulesAllowedToAssociate;
	}

	//

	@Override
	public int getLatencyBetweenDevices(FogDevice.DeviceNodeType typeFrom, FogDevice.DeviceNodeType typeDestination) {
		return this.latenciesBetweenDeviceTypes.apply(typeFrom, typeDestination);
	}

	/**
	 * The latency is directional: from the first argument to the second.
	 * 
	 * @param sourceConnection      source of the link/connection
	 * @param destinationConnection destination of the link/connection
	 * @param latencyMilliseconds   latenxy expressed in milliseconds
	 * @see org.fog.heuristics.fogImplementations.ModulePlacementAdditionalInformationFog.DeviceNodeTypesLatencyMap#addPair(org.fog.entities.FogDevice.DeviceNodeType,
	 *      org.fog.entities.FogDevice.DeviceNodeType, int)
	 */
	public void addLatencyBetweenDevices(DeviceNodeType sourceConnection, DeviceNodeType destinationConnection,
			int latencyMilliseconds) {
		latenciesBetweenDeviceTypes.addPair(sourceConnection, destinationConnection, latencyMilliseconds);
	}

	//

	protected void resetMapsModuleDevices() {
		final Map<String, Map<String, FogDevice>> daa;
		final Map<String, Map<String, AppModule>> mata;
		final Map<String, AppModule> modByName;

		daa = new HashMap<>();
		mata = new HashMap<>();
		this.devicesAllowingAssociation = daa;
		this.modulesAllowedToAssociate = mata;
		modByName = this.modulesByName;
		this.allowedModulesOnDeviceAssociation.getModuleMapping().forEach((deviceName, modulesList) -> {
			Map<String, FogDevice> devicesAcceptingThisModule = null;
			Map<String, AppModule> modulesAccepted = null; // for the current device

			if (mata.containsKey(deviceName)) {
				modulesAccepted = mata.get(deviceName);
			} else {
				modulesAccepted = new HashMap<>();
				mata.put(deviceName, modulesAccepted);
			}

			for (String moduleName : modulesList) {
				if (daa.containsKey(moduleName)) {
					devicesAcceptingThisModule = daa.get(moduleName);
				} else {
					devicesAcceptingThisModule = new HashMap<>();
					daa.put(moduleName, devicesAcceptingThisModule);
				}
				modulesAccepted.put(moduleName, modByName.get(moduleName));
			}
		});
	}

	//

	/**
	 * Defines a map storing the latency information (in milliseconds): the latency
	 * between an ordered pair of {@link FogDevice}
	 * 
	 * @author marcoottina
	 *
	 */
	public static class DeviceNodeTypesLatencyMap
			implements BiFunction<FogDevice.DeviceNodeType, FogDevice.DeviceNodeType, Integer> {

		public DeviceNodeTypesLatencyMap(Comparator<DeviceNodeTypesLatencyPair> pairOrdering) {
			super();
			this.pairToLatency = new TreeMap<>(pairOrdering);
		}

		public DeviceNodeTypesLatencyMap() {
			this((p1, p2) -> {
				int c;
				final DeviceNodeType f1, s1, f2, s2;
				if (p1 == p2) {
					return 0;
				}
				f1 = p1.getFirst();
				s1 = p1.getSecond();
				f2 = p2.getFirst();
				s2 = p2.getSecond();
				if (f1 == f2) {
					if (s1 == s2) {
						return 0;
					}
					if (s1 == null) {
						return -1;
					}
					if (s2 == null) {
						return 1;
					}
					return Integer.compare(s1.ordinal(), s2.ordinal());
				} // else
				if (f1 == null) {
					return -1;
				}
				if (f2 == null) {
					return 1;
				}
				c = Integer.compare(f1.ordinal(), f2.ordinal());
				if (c == 0) {
					// this should be a dead code because of the "if" comparing f1 and f2 !
					// then, it follows a simple copy-paste of that "if"
					if (s1 == s2) {
						return 0;
					}
					if (s1 == null) {
						return -1;
					}
					if (s2 == null) {
						return 1;
					}
					return Integer.compare(s1.ordinal(), s2.ordinal());
				}
				return c;
			});
		}

		protected Map<DeviceNodeTypesLatencyPair, Integer> pairToLatency;

		/**
		 * @return the pairToLatency
		 */
		public Map<DeviceNodeTypesLatencyPair, Integer> getPairToLatency() {
			return pairToLatency;
		}

		@Override
		public Integer apply(DeviceNodeType arg0, DeviceNodeType arg1) {
			Integer latency = this.pairToLatency.get(new DeviceNodeTypesLatencyPair(arg0, arg1, 0));
			return latency == null ? 0 : latency;
		}

		public void addPair(DeviceNodeType k, DeviceNodeType v, int latencyMilliseconds) {
			DeviceNodeTypesLatencyPair p;
			p = new DeviceNodeTypesLatencyPair(k, v, latencyMilliseconds);
			this.pairToLatency.put(p, latencyMilliseconds);
		}
	}

	/**
	 * Just an ordered pair of devices and the latency between the first device and
	 * the second.
	 * 
	 * @author marcoottina
	 *
	 */
	public static class DeviceNodeTypesLatencyPair extends Pair<FogDevice.DeviceNodeType, FogDevice.DeviceNodeType> {
		protected int latencyMilliseconds;

		public DeviceNodeTypesLatencyPair(DeviceNodeType sourceCommunication, DeviceNodeType destinatinoCommunication,
				int latencyMilliseconds) {
			super(sourceCommunication, destinatinoCommunication);
			this.latencyMilliseconds = latencyMilliseconds;
		}

		public DeviceNodeTypesLatencyPair(Pair<? extends DeviceNodeType, ? extends DeviceNodeType> entry) {
			super(entry);
			if (entry instanceof DeviceNodeTypesLatencyPair) {
				this.latencyMilliseconds = ((DeviceNodeTypesLatencyPair) entry).latencyMilliseconds;
			} else {
				this.latencyMilliseconds = 0;
			}
		}

		/**
		 * @return the latencyMilliseconds
		 */
		public int getLatencyMilliseconds() {
			return latencyMilliseconds;
		}

		/**
		 * @param latencyMilliseconds the latencyMilliseconds to set
		 */
		public void setLatencyMilliseconds(int latencyMilliseconds) {
			this.latencyMilliseconds = latencyMilliseconds;
		}
	}
}
