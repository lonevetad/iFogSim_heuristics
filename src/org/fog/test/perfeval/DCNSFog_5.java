package org.fog.test.perfeval;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDevice.DeviceNodeType;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementWithHeuristics;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

/**
 * Simulation setup for case study 2 - Intelligent Surveillance
 * 
 * @author Harshit Gupta
 *
 */
public class DCNSFog_5 {
	static final List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static final List<Sensor> sensors = new ArrayList<Sensor>();
	static final List<Actuator> actuators = new ArrayList<Actuator>();
	static int numOfAreas = 1;
	static int numOfCamerasPerArea = 4;
	private static FogDevice cloud;

	private static boolean CLOUD = false;

	private static final ApplicationData[] APP_DATA = new ApplicationData[] { //
			new ApplicationData("motion", 120, 60, false) //
			, new ApplicationData("video", 300, 0, true) //
			, new ApplicationData("sound", 300, 60, true) //
			, new ApplicationData("temperature", 360, 60, true) //
			, new ApplicationData("humidity", 240, 0, false) //
	};
	private static final double thresholdProcessPower = 2025, thresholdSolutionEvaluationImprovement = 0.1,
			thresholdDifferenceSolutions = 0.1;

	public static void main(String[] args) {

		Log.printLine("Starting DCNS fog 5...");

		try {
			Log.disable();
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events
			List<Application> allApplications;

			CloudSim.init(num_user, calendar, trace_flag);

			FogBroker broker = new FogBroker("broker");

			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping
			allApplications = new ArrayList<>(APP_DATA.length);
			for (ApplicationData ad : APP_DATA) {
				Application application;
				String appId = ad.appId;
				application = createApplication(appId, broker.getId());
				application.setUserId(broker.getId());
				application.setDeadlineMilliseconds(ad.getDeadline());
				application.setDeploymentTimeMilliseconds(ad.getDeployTime());
				application.setDelayTolerable(ad.delayTolerable);
				allApplications.add(application);
				createFogDevices(broker.getId(), appId);

				for (FogDevice device : fogDevices) {
					if (device.getName().startsWith("m")) { // names of all Smart Cameras start with 'm'
						moduleMapping.addModuleToDevice(appId + "__motion_detector", device.getName()); // fixing 1
																										// instance of
																										// the
						// Motion Detector module to
						// each Smart Camera
					}
				}
				moduleMapping.addModuleToDevice(appId + "__user_interface", "cloud"); // fixing instances of User
																						// Interface module in
				// the Cloud
				if (CLOUD) {
					// if the mode of deployment is cloud-based
					moduleMapping.addModuleToDevice(appId + "__object_detector", "cloud"); // placing all instances of
																							// Object Detector
					// module in the Cloud
					moduleMapping.addModuleToDevice(appId + "__object_tracker", "cloud"); // placing all instances of
																							// Object Tracker
					// module in the Cloud
				}
			}

			for (Application app : allApplications) {
				fogDevices.forEach(d -> {
					d.getApplicationMap().put(app.getAppId(), app);
				});
			}

			Controller controller;
			controller = new Controller("master-controller", fogDevices, sensors, actuators);
			ModulePlacementWithHeuristics modulePlacement;
			modulePlacement = new ModulePlacementWithHeuristics(fogDevices, sensors, actuators, allApplications,
					thresholdProcessPower, thresholdSolutionEvaluationImprovement, thresholdDifferenceSolutions);

			for (Application application : allApplications) {
				controller.submitApplication(application, modulePlacement
				/*
				 * (CLOUD) ? (new ModulePlacementMapping(fogDevices, application,
				 * moduleMapping)) : (new ModulePlacementEdgewards(fogDevices, sensors,
				 * actuators, application, moduleMapping))
				 */
				);
			}

			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

			CloudSim.startSimulation();

			CloudSim.stopSimulation();

			Log.printLine("VRGame finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}

	/**
	 * Creates the fog devices in the physical topology of the simulation.
	 * 
	 * @param userId
	 * @param appId
	 */
	private static void createFogDevices(int userId, String appId) {
		if (cloud == null) {
			cloud = createFogDevice("cloud", DeviceNodeType.CloudNode, 10000, 4000, 100, 10000, 0, 0.01, 16 * 103,
					16 * 83.25);
			cloud.setParentId(-1);
			cloud.setUplinkLatency(5000);
			fogDevices.add(cloud);
		}
		FogDevice proxy = createFogDevice("proxy-server__" + appId, DeviceNodeType.NeighboringFogControllerNode, 42800,
				4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		proxy.setParentId(cloud.getId());
		proxy.setUplinkLatency(500); // latency of connection between proxy server and cloud is 500 ms
		fogDevices.add(proxy);
		for (int i = 0; i < numOfAreas; i++) {
			addArea(appId + "__" + Integer.toString(i), userId, appId, proxy.getId());
		}
	}

	private static FogDevice addArea(String id, int userId, String appId, int parentId) {
		FogDevice router = createFogDevice("d-" + id, DeviceNodeType.FogControllerNode, 2800, 4000, 10000, 10000, 1,
				0.0, 107.339, 83.4333);
		fogDevices.add(router);
		router.setUplinkLatency(2); // latency of connection between router and proxy server is 2 ms
		for (int i = 0; i < numOfCamerasPerArea; i++) {
			String mobileId = id + "-" + i;
			FogDevice camera = addCamera(mobileId, userId, appId, router.getId()); // adding a smart camera to the
																					// physical topology. Smart cameras
																					// have been modeled as fog devices
																					// as well.
			camera.setUplinkLatency(2); // latency of connection between camera and router is 2 ms
			fogDevices.add(camera);
		}
		router.setParentId(parentId);
		return router;
	}

	private static FogDevice addCamera(String id, int userId, String appId, int parentId) {
		FogDevice camera = createFogDevice("m-" + id, DeviceNodeType.FogNode, 100, 500, 10000, 10000, 3, 0, 87.53,
				82.44);
		camera.setUplinkLatency(0.2);
		camera.setParentId(parentId);
		Sensor sensor = new Sensor("s-" + id, "CAMERA__" + appId, userId, appId, new DeterministicDistribution(5)); // inter-transmission
		// time of
		// camera
		// (sensor)
		// follows a
		// deterministic
		// distribution
		sensors.add(sensor);
		Actuator ptz = new Actuator("ptz-" + id, userId, appId, "PTZ_CONTROL__" + appId);
		actuators.add(ptz);
		sensor.setGatewayDeviceId(camera.getId());
		sensor.setLatency(1.0); // latency of connection between camera (sensor) and the parent Smart Camera is
								// 1 ms
		ptz.setGatewayDeviceId(camera.getId());
		ptz.setLatency(1.0); // latency of connection between PTZ Control and the parent Smart Camera is 1 ms
		return camera;
	}

	/**
	 * Creates a vanilla fog device
	 * 
	 * @param nodeName    name of the device to be used in simulation
	 * @param mips        MIPS
	 * @param ram         RAM
	 * @param upBw        uplink bandwidth
	 * @param downBw      downlink bandwidth
	 * @param level       hierarchy level of the device
	 * @param ratePerMips cost rate per MIPS used
	 * @param busyPower
	 * @param idlePower
	 * @return
	 */
	private static FogDevice createFogDevice(String nodeName, FogDevice.DeviceNodeType nodeType, long mips, int ram,
			long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {

		List<Pe> peList = new ArrayList<Pe>(2);

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(hostId, new RamProvisionerSimple(ram), new BwProvisionerOverbooking(bw), storage,
				peList, new StreamOperatorScheduler(peList), new FogLinearPowerModel(busyPower, idlePower));

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
		// devices by now

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(arch, os, vmm, host, time_zone, cost,
				costPerMem, costPerStorage, costPerBw);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, nodeType, characteristics, new AppModuleAllocationPolicy(hostList),
					storageList, 10, upBw, downBw, 0, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}

		fogdevice.setLevel(level);
		return fogdevice;
	}

	/**
	 * Function to create the Intelligent Surveillance application in the DDF model.
	 * 
	 * @param appId  unique identifier of the application
	 * @param userId identifier of the user of the application
	 * @return
	 */
	@SuppressWarnings({ "serial" })
	private static Application createApplication(String appId, int userId) {
		AppModule mod;

		Application application = Application.createApplication(appId, userId);
		/*
		 * Adding modules (vertices) to the application model (directed graph)
		 */
		application.addAppModule(appId + "__object_detector", 10);
		application.addAppModule(appId + "__motion_detector", 10);
		application.addAppModule(appId + "__object_tracker", 10);
		application.addAppModule(appId + "__user_interface", 10);

		mod = application.getModuleByName(appId + "__motion_detector");
		mod.setRam(30);
		mod.setSize(10);
//		addMips(mod //
//				.getHost() //
//				.getPeList(), new int[] { 50, 50 });

		mod = application.getModuleByName(appId + "__object_detector");
		mod.setRam(20);
		mod.setSize(30);
//		addMips(mod.getHost().getPeList(), new int[] { 200, 300 });

		mod = application.getModuleByName(appId + "__object_tracker");
		mod.setRam(20);
		mod.setSize(30);
//		addMips(mod.getHost().getPeList(), new int[] { 200, 300 });

		/*
		 * Connecting the application modules (vertices) in the application model
		 * (directed graph) with edges
		 */
		/*
		 * adding edge from CAMERA (sensor) to Motion Detector module carrying tuples of
		 * type CAMERA
		 */
		application.addAppEdge("CAMERA__" + appId, appId + "__motion_detector", 1000, 20000, "CAMERA__" + appId,
				Tuple.UP, AppEdge.SENSOR);

		application.addAppEdge(appId + "__motion_detector", appId + "__object_detector", 2000, 2000,
				"MOTION_VIDEO_STREAM__" + appId, Tuple.UP, AppEdge.MODULE); // adding edge from Motion Detector to
																			// Object Detector
		// module carrying tuples of
		// type MOTION_VIDEO_STREAM
		application.addAppEdge(appId + "__object_detector", appId + "__user_interface", 500, 2000,
				"DETECTED_OBJECT__" + appId, Tuple.UP, AppEdge.MODULE); // adding edge from Object Detector to User
																		// Interface module carrying tuples
																		// of
																		// type DETECTED_OBJECT
		application.addAppEdge(appId + "__object_detector", appId + "__object_tracker", 1000, 100,
				"OBJECT_LOCATION__" + appId, Tuple.UP, AppEdge.MODULE); // adding edge from Object Detector to Object
																		// Tracker module carrying tuples
																		// of
																		// type OBJECT_LOCATION
		application.addAppEdge(appId + "__object_tracker", "PTZ_CONTROL__" + appId, 100, 28, 100,
				"PTZ_PARAMS__" + appId, Tuple.DOWN, AppEdge.ACTUATOR); // adding edge from Object Tracker to PTZ CONTROL
																		// (actuator) carrying tuples of
																		// type PTZ_PARAMS

		/*
		 * Defining the input-output relationships (represented by selectivity) of the
		 * application modules.
		 */
		/*
		 * 1.0 tuples of type MOTION_VIDEO_STREAM are emitted by Motion Detector module
		 * per incoming tuple of type CAMERA
		 */
		application.addTupleMapping(appId + "__motion_detector", "CAMERA__" + appId, "MOTION_VIDEO_STREAM__" + appId,
				new FractionalSelectivity(1.0));
		application.addTupleMapping(appId + "__object_detector", "MOTION_VIDEO_STREAM__" + appId,
				"OBJECT_LOCATION__" + appId, new FractionalSelectivity(1.0)); // 1.0 tuples of type OBJECT_LOCATION are
																				// emitted by Object Detector
																				// module per incoming tuple of type
																				// MOTION_VIDEO_STREAM
		application.addTupleMapping(appId + "__object_detector", "MOTION_VIDEO_STREAM__" + appId,
				"DETECTED_OBJECT__" + appId, new FractionalSelectivity(0.05)); // 0.05 tuples of type
																				// MOTION_VIDEO_STREAM are emitted by
																				// Object
																				// Detector module per incoming tuple of
																				// type MOTION_VIDEO_STREAM

		/*
		 * Defining application loops (maybe incomplete loops) to monitor the latency
		 * of. Here, we add two loops for monitoring : Motion Detector -> Object
		 * Detector -> Object Tracker and Object Tracker -> PTZ Control
		 */
		final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
			{
				add(appId + "__motion_detector");
				add(appId + "__object_detector");
				add(appId + "__object_tracker");
			}
		});
		final AppLoop loop2 = new AppLoop(new ArrayList<String>() {
			{
				add(appId + "__object_tracker");
				add("PTZ_CONTROL__" + appId);
			}
		});
		List<AppLoop> loops = new ArrayList<AppLoop>() {
			{
				add(loop1);
				add(loop2);
			}
		};

		application.setLoops(loops);
		return application;
	}

	private static void addMips(List<Pe> processingElements, final int[] MIPS_SCENARIO) {
		int i;
		while (processingElements.size() < 2) {
			i = processingElements.size();
			processingElements.add(new Pe(i, new PeProvisionerOverbooking(MIPS_SCENARIO[i])));
		}
	}

	//

	public static class ApplicationData {
		public boolean delayTolerable;
		public String appId;
		public int deadline;
		public int deployTime;

		public ApplicationData(String appId, int deadline, int deployTime, boolean delayTolerable) {
			super();
			this.appId = appId;
			this.deadline = deadline;
			this.deployTime = deployTime;
			this.delayTolerable = delayTolerable;
		}

		public String getAppId() {
			return appId;
		}

		public int getDeadline() {
			return deadline;
		}

		public int getDeployTime() {
			return deployTime;
		}

		public void setAppId(String appId) {
			this.appId = appId;
		}

		public void setDeadline(int deadline) {
			this.deadline = deadline;
		}

		public void setDeployTime(int deployTime) {
			this.deployTime = deployTime;
		}

	}
}