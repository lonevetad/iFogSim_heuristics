package org.fog.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.power.PowerVm;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.scheduler.TupleScheduler;
import org.fog.utils.FogUtils;

/**
 * Class representing an application module, the processing elements of the
 * application model of iFogSim.
 * 
 * @author Harshit Gupta
 *
 */
public class AppModule extends PowerVm {

	/**
	 * Specifies the type of a module. Sensors and Actuators are
	 * {@link ModuleType#Physical} while Module is {@link ModuleType#Software}.
	 */
	public static enum ModuleType {
		Physical, Software
	}

	public static enum ModulePlacementPolicy {
		SensorsOnly, NonSensorsOnly, Anything
	}

	private String name;
	private String appId;
	private Map<Pair<String, String>, SelectivityModel> selectivityMap;

	/**
	 * A map from the AppModules sending tuples UP to this module to their instance
	 * IDs. If a new instance ID is detected, the number of instances is
	 * incremented.
	 */
	private Map<String, List<Integer>> downInstanceIdsMaps;

	/**
	 * Number of instances of this module
	 */
	private int numInstances;

	/**
	 * Mapping from tupleType emitted by this AppModule to Actuators subscribing to
	 * that tupleType
	 */
	private Map<String, List<Integer>> actuatorSubscriptions;

	protected ModuleType moduleType;
	protected int deadline;

	public AppModule(int id, String name, String appId, int userId, double mips, int ram, long bw, long size,
			String vmm, CloudletScheduler cloudletScheduler,
			Map<Pair<String, String>, SelectivityModel> selectivityMap) {
		this(id, name, appId, userId, mips, ram, bw, size, vmm, cloudletScheduler, selectivityMap, 0);
	}

	public AppModule(int id, String name, String appId, int userId, double mips, int ram, long bw, long size,
			String vmm, CloudletScheduler cloudletScheduler, Map<Pair<String, String>, SelectivityModel> selectivityMap,
			int deadline) {
		super(id, userId, mips, 1, ram, bw, size, 1, vmm, cloudletScheduler, 300);
		setName(name);
		setId(id);
		setAppId(appId);
		setUserId(userId);
		setUid(getUid(userId, id));
		setMips(mips);
		setNumberOfPes(1);
		setRam(ram);
		setBw(bw);
		setSize(size);
		setVmm(vmm);
		setCloudletScheduler(cloudletScheduler);
		setInMigration(false);
		setBeingInstantiated(true);
		setCurrentAllocatedBw(0);
		setCurrentAllocatedMips(null);
		setCurrentAllocatedRam(0);
		setCurrentAllocatedSize(0);
		setSelectivityMap(selectivityMap);
		setActuatorSubscriptions(new HashMap<String, List<Integer>>());
		setNumInstances(0);
		setDownInstanceIdsMaps(new HashMap<String, List<Integer>>());
		this.setDeadline(deadline);
	}

	public AppModule(AppModule operator) {
		super(FogUtils.generateEntityId(), operator.getUserId(), operator.getMips(), 1, operator.getRam(),
				operator.getBw(), operator.getSize(), 1, operator.getVmm(), new TupleScheduler(operator.getMips(), 1),
				operator.getSchedulingInterval());
		setName(operator.getName());
		setAppId(operator.getAppId());
		setInMigration(false);
		setBeingInstantiated(true);
		setCurrentAllocatedBw(0);
		setCurrentAllocatedMips(null);
		setCurrentAllocatedRam(0);
		setCurrentAllocatedSize(0);
		this.setDeadline(operator.getDeadline());
		setSelectivityMap(operator.getSelectivityMap());
		setDownInstanceIdsMaps(new HashMap<String, List<Integer>>());
	}

	public void subscribeActuator(int id, String tuplyType) {
		if (!getActuatorSubscriptions().containsKey(tuplyType))
			getActuatorSubscriptions().put(tuplyType, new ArrayList<Integer>());
		getActuatorSubscriptions().get(tuplyType).add(id);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		this.resetModuleType();
	}

	public Map<Pair<String, String>, SelectivityModel> getSelectivityMap() {
		return selectivityMap;
	}

	public void setSelectivityMap(Map<Pair<String, String>, SelectivityModel> selectivityMap) {
		this.selectivityMap = selectivityMap;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public Map<String, List<Integer>> getActuatorSubscriptions() {
		return actuatorSubscriptions;
	}

	public void setActuatorSubscriptions(Map<String, List<Integer>> actuatorSubscriptions) {
		this.actuatorSubscriptions = actuatorSubscriptions;
	}

	public Map<String, List<Integer>> getDownInstanceIdsMaps() {
		return downInstanceIdsMaps;
	}

	public void setDownInstanceIdsMaps(Map<String, List<Integer>> downInstanceIdsMaps) {
		this.downInstanceIdsMaps = downInstanceIdsMaps;
	}

	public int getNumInstances() {
		return numInstances;
	}

	public void setNumInstances(int numInstances) {
		this.numInstances = numInstances;
	}

	public ModuleType getModuleType() {
		return this.moduleType;
	}

	/**
	 * 
	 * @return deadline expected deadline in milliseconds. If {@code 0}, then it has
	 *         to be ignored
	 */
	public int getDeadline() {
		return deadline;
	}

	public void setDeadline(int deadline) {
		this.deadline = deadline;
	}

	/**
	 * Only modules whose name is completely uppercase are
	 * {@link ModuleType#Physical} (Sensors and Actuators)
	 */
	protected ModuleType resetModuleType() {
		char c;
		int i;
		i = this.name.length();
		while ((!Character.isAlphabetic(c = this.name.charAt(--i))) || Character.isUpperCase(c))
			;
		return this.moduleType = (i == 0) ? ModuleType.Physical : ModuleType.Software;
	}
}
