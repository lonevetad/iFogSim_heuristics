package org.fog.heuristics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.ParameterException;
import org.cloudbus.cloudsim.power.PowerHost;
import org.fog.application.AppModule;
import org.fog.application.AppModule.ModuleType;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDevice.DeviceNodeType;
import org.fog.heuristics.Utils.EditCosts;
import org.fog.heuristics.fogImplementations.ListDevices;
import org.fog.heuristics.fogImplementations.PieceOfSolution;
import org.fog.heuristics.fogImplementations.SolutionModulesDeployed;

public class SolutionsProducerEvaluator {
	protected static final BinaryOperator<Integer> ADDER_Int = (a, b) -> a + b;
	protected static final BinaryOperator<Double> ADDER_Double = (a, b) -> a + b, MAX_D = (a, b) -> a > b ? a : b;
	protected static final EditCosts<AppModule> EDIT_COST_APP_MODULE = new EditCosts<AppModule>() {

		@Override
		public long insertion(AppModule element) {
			return 1;
		}

		@Override
		public long deletion(AppModule element) {
			return 1;
		}

		@Override
		public long substitution(AppModule previous, AppModule newOne) {
			return previous == newOne ? 0 : 1;
		}
	};
	protected static final EditCosts<ListAppMod> EDIT_COST_LIST_APP_MODULE = new EditCosts<ListAppMod>() {

		@Override
		public long insertion(ListAppMod element) {
			return element.size();
		}

		@Override
		public long deletion(ListAppMod element) {
			return element.size();
		}

		@Override
		public long substitution(ListAppMod previous, ListAppMod newOne) {
			return Utils.editDistance(previous, newOne, EDIT_COST_APP_MODULE);
		}
	};

	public static long evaluateDifference(SolutionModulesDeployed solution1, SolutionModulesDeployed solution2) {
		int i;
		ListAppMod l, a1[], a2[];
		String id;
		Map<String, ListAppMod> modulesEachDevice1, modulesEachDevice2;
		if (solution1 == solution2) {
			return 0;
		}
		if (solution1 == null) {
			return solution2.getPieces().size(); // the amount of AppModules
		}
		if (solution2 == null) {
			return solution1.getPieces().size(); // the amount of AppModules
		}

		modulesEachDevice1 = new TreeMap<>();
		for (PieceOfSolution p : solution1.getPieces()) {
			id = p.getDevice().getName();
			if (modulesEachDevice1.containsKey(id)) {
				l = modulesEachDevice1.get(id);
			} else {
				l = new ListAppMod();
				modulesEachDevice1.put(id, l);
			}
			l.add(p.getModule());
		}

		modulesEachDevice2 = new TreeMap<>();
		for (PieceOfSolution p : solution2.getPieces()) {
			id = p.getDevice().getName();
			if (modulesEachDevice2.containsKey(id)) {
				l = modulesEachDevice2.get(id);
			} else {
				l = new ListAppMod();
				modulesEachDevice2.put(id, l);
			}
			l.add(p.getModule());
		}

		a1 = new ListAppMod[modulesEachDevice1.size()];
		i = 0;
		for (Entry<String, ListAppMod> e : modulesEachDevice1.entrySet()) {
			a1[i++] = e.getValue();
		}
		a2 = new ListAppMod[modulesEachDevice2.size()];
		i = 0;
		for (Entry<String, ListAppMod> e : modulesEachDevice2.entrySet()) {
			a2[i++] = e.getValue();
		}

		return Utils.editDistance(a1, a2, EDIT_COST_LIST_APP_MODULE);

//		pos1 = solution1.getPieces();
//		pos2 = solution2.getPieces();
//		size1 = pos1.size();
//		size2 = pos2.size();
//		if (size1 < size2) {
//			int t;
//			List<PieceOfSolution> pt;
//			t = size1;
//			size1 = size2;
//			size2 = t;
//			pt = pos1;
//			pos1 = pos2;
//			pos2 = pt;
//		}
//		diff=0;
//		size1++; // as a shifter
//		i1=pos1.iterator();
//		i2=pos2.iterator();
//		while(size2-->0) {
//			p1=i1.next();
//			p2=i2.next();
//			if(p1.getDevice()==p2.getDevice()) {
//				diff++;
//			}
//		}
//		return diff;
	}

	/**
	 * TODO calculation missing:<br>
	 * <ul>
	 * <li>deployment time (d.t.) for the whole application (+ checking if a
	 * neighboring controller has at least one of its module , which would increase
	 * the d.t. by the "propagation delay" and "expected deploy time"). Also, the
	 * deployment time is both only bounded to the Application and provided as an
	 * instance field, which is NOT dynamic.</li>
	 * <li>node and module capacities (there's only a draft)</li>
	 * <li>ways to check if a module is / would be deployed in {fog ; neighboring ;
	 * control ; cloud } node and their respective distances</li>
	 * <li>the energy consumption for each modules (in the article, the addends are
	 * the energies of {fog nodes ("efn"); foc controller node ("efcn"); neighboring
	 * controller node ("enfcn"); cloud node ("ecn") }</li>
	 * </ul>
	 * 
	 * @param appsByID given to provide context to the {@link AppModule} and the
	 *                 internal calculations
	 */
	public static double evaluateSolution(SolutionModulesDeployed solution, Map<String, Application> appsByID) {
		double evaluation, respTime;
		AppModule m;
		FogDevice d;
		Application app;

		if (solution == null || solution.getPieces() == null) {
			return 0.0;
		}
		evaluation = 0.0;
		for (PieceOfSolution pos : solution.getPieces()) {
			m = pos.getModule();
			d = pos.getDevice();
			app = appsByID.get(m.getAppId());
			if (app != null) {
				respTime = responseTime(m, d, app);

				evaluation += respTime;
				// idle power et similia will be calculated in the global calculation below
//						+ energyConsumption(m, d, app);
			}
		}

		evaluation += energyConsumptionWholeSolution(solution, appsByID);
		return evaluation;
	}

	@Deprecated
	public static double[][] resourcesMetricsPairs(AppModule m, FogDevice d) {
		PowerHost powerHost;
		powerHost = d.getHost();
		return new double[][] { //
//				new double[] { // start power
//						m.getMips() * d.getRatePerMips(), //
//						d.getPower() //
//				}, // end power
				new double[] { // start mips (million of instructions per seconds)
						m.getMips(), //
						powerHost.getAvailableMips() //
				}, // end mips
				new double[] { // start bandwidth
						m.getBw(), // TODO: getCurrentAllocatedBw() may be more suitable
						powerHost.getBw() //
				}, // end bw
				new double[] { // start ram
						m.getRam(), // TODO: getCurrentAllocatedRam() may be more suitable
						powerHost.getRam() //
				}, // end ram
				new double[] { // start "size" (TODO: very generic, undocumented: is it a local storage size?)
						m.getSize(), // TODO: probably, it will be changed to something more suitable
						powerHost.getStorage() //
				} // end size/storage
		};
	}

	public static double getExpectedModuleMillionOfIstructionsToExec(AppModule module, Application app) {
		return app.getEdges().stream() //
				.filter(e -> e.getSource() == module.getName()) //
				.map(e -> e.getTupleCpuLength()) //
				.reduce(0.0, ADDER_Double);
	}

	/**
	 * Returns the result in seconds.
	 */
	public static double executionTime(AppModule module, FogDevice device, Application app) {
		return getExpectedModuleMillionOfIstructionsToExec(module, app) / device.getHost().getAvailableMips();
	}

	public static double responseTime(AppModule module, FogDevice device, Application app) {
		boolean isInFog, isInNbr, isInController, isInCloud;
		double mkspanTime, executionTime, distFog, distNbr, distController, distCloud;
		DeviceNodeType dt;
		// note: all variables name are taken from the article

		mkspanTime = 0.0;
		executionTime = 0.0;
		isInFog = isInNbr = isInController = isInCloud = false;
		distFog = distNbr = distController = distCloud = 0.0;

		// TODO: how to get those values?

		// draft 1 : just adding up everything available
//		for (double[] moduleNodeCapacity : resourcesMetricsPairs(module, device)) {
//			if (moduleNodeCapacity[1] > 0.0) {
//				executionTime += moduleNodeCapacity[0] / moduleNodeCapacity[1];
//			}
//		}

		// draft 2
//		executionTime = executionTime(module, device, app);

		// draft 3
		executionTime = module.getMips() / device.getHost().getAvailableMips();

		mkspanTime = executionTime;

		dt = device.getDeviceNodeType();
		switch (dt) {
		case CloudNode:
			isInCloud = true;
			distCloud = 0; // device.get
			break;
		case FogControllerNode:
			isInController = true;
			distController = 0; // I'm already there!
			break;
		case FogNode:
			isInFog = true;
			distFog = device.getUplinkLatency();
			break;
		case NeighboringFogControllerNode:
			isInNbr = true;
			distNbr = device.getUplinkLatency();
			break;
		default:
			break;
		}
		/*
		 * TODO : (FogDevice)CloudSim.getEntity(fogDeviceId) may be usefull, especially
		 * to crawl from the current device up to the cloud
		 */

		if (isInFog) {
			mkspanTime += distFog;
		}
		if (isInNbr) {
			mkspanTime += distNbr * 2.0;
		}
		if (isInController) {
			mkspanTime += distController;
		}
		if (isInCloud) {
			mkspanTime += distCloud * 2.0;
		}

		//

		return mkspanTime;
	}

	public static double energyConsumptionWholeSolution(SolutionModulesDeployed solution,
			Map<String, Application> appsByID) {
		double accumulator, totalMips;
		String idd;
		FogDevice d;
		AppModule mod;
		PowerHost ph;
		final Map<String, Pair<FogDevice, Double>> totalMipsLoadedOnDevice;

		totalMipsLoadedOnDevice = new HashMap<>();
		for (PieceOfSolution pos : solution.getPieces()) {
			d = pos.getDevice();
			mod = pos.getModule();

			// recycle the accumulator variable
			totalMips = 0.0;
			idd = d.getName();
			if (totalMipsLoadedOnDevice.containsKey(idd)) {
				totalMips = totalMipsLoadedOnDevice.get(idd).getSecond();
				totalMipsLoadedOnDevice.remove(idd);
			}
			totalMips += getExpectedModuleMillionOfIstructionsToExec(mod, appsByID.get(mod.getAppId()));
			totalMipsLoadedOnDevice.put(idd, new Pair<FogDevice, Double>(d, totalMips));
		}

		accumulator = 0.0;
		for (Entry<String, Pair<FogDevice, Double>> e : totalMipsLoadedOnDevice.entrySet()) {
			d = e.getValue().getFirst();
			totalMips = e.getValue().getSecond();
			ph = d.getHost();
			accumulator +=
					// get the total power
					ph.getPowerModel().getPower(Math.min(ph.getAvailableMips(), totalMips) / ph.getAvailableMips()) //
							// * time, but in fraction of seconds
							* (totalMips / ph.getAvailableMips());
		}

		return accumulator;
	}

	@Deprecated
	public static double energyConsumption(AppModule m, FogDevice d, Application app) {
		double energyNode, energyModule, efn, efcn, enfcn, ecn;
		PowerHost powerHost;

		energyModule = energyNode = 0.0;
		efn = efcn = enfcn = ecn = 0.0;

		powerHost = d.getHost();

		/*
		 * TODO: the article is a bit unclear about the exact formula; analyzing the
		 * PowerModel code the following code seems to fit the purpose:
		 */
		energyNode = powerHost.getPowerModel().getPower(
				// recycle "energyNode" variable as a temporary variable
				((energyNode = powerHost.getAvailableMips()) == 0.0 ? 0.0 : ( //
				m.getMips() / energyNode)));

		energyModule = efn + efcn + enfcn + ecn;

		return energyNode + energyModule;
	}

	/**
	 * Just calls {@link #newRandomSolution(Map, List, List, Random, ListDevices[])}
	 * by providing the result of {@link #partitionateDevicesByType(List)} as last
	 * parameter.
	 */
	public static <S extends SolutionModulesDeployed> Pair<S, SolutionDeployCosts<S>> newRandomSolution(
			Map<String, Application> applicationsSubmitted, List<AppModule> modules, List<FogDevice> devices, Random r,
			Function<List<PieceOfSolution>, S> solutionFactory) {
		return newRandomSolution(applicationsSubmitted, modules, devices, r, partitionateDevicesByType(devices),
				solutionFactory);
	}

	/**
	 * Generate a new random {@link PieceOfSolution} for a given {@link AppModule}
	 * that satisfy some design constraints, using one {@link FogDevice} from the
	 * provided ones
	 * 
	 * @param applicationsSubmitted applications from which the modules comes from
	 * @param modules               the list of {@link AppModule} from where get the
	 *                              modules to base the solution
	 * @param devices               the list of {@link FogDevice} from which one has
	 *                              to be paired to the provided {@link AppModule}
	 * @param r                     just an instance of {@link Random}
	 * 
	 * @return a new randomly created solution
	 */

	public static <S extends SolutionModulesDeployed> Pair<S, SolutionDeployCosts<S>> newRandomSolution(
			Map<String, Application> applicationsSubmitted, List<AppModule> modules, List<FogDevice> devices, Random r,
			final ListDevices[] devicesPartitions, Function<List<PieceOfSolution>, S> solutionFactory) {
		boolean isOk;
		int iterationsLeft;
		Application app;
		PieceOfSolution pieceOfSolution;
		List<PieceOfSolution> pieces;
		final List<FogDevice> nonFogNodes;
		final SolutionDeployCosts<S> cumulatedCostsEachDevice; // on each device

		Objects.requireNonNull(solutionFactory);

		pieces = new ArrayList<>(modules.size());
		cumulatedCostsEachDevice = new SolutionDeployCosts<S>();

//		modulesByName = new HashMap<>();
//		modules.forEach(m -> modulesByName.put(m.getName(), m));

		nonFogNodes = new ArrayList<FogDevice>(
				devices.size() - devicesPartitions[DeviceNodeType.FogNode.ordinal()].size());
		for (DeviceNodeType dnt : DeviceNodeType.values()) {
			if (dnt != DeviceNodeType.FogNode) {
				devicesPartitions[dnt.ordinal()].forEach(nonFogNodes::add);
			}
		}

		iterationsLeft = 10; // Arbitrarily chosen
		try {
			do {
				// reset
				cumulatedCostsEachDevice.clear();
				pieces.clear();

				for (AppModule module : modules) {
					app = applicationsSubmitted.get(module.getAppId());
					if (app == null) {
						return null; // brutal interruption: constraint 1 can't be met
					}

					pieceOfSolution = newRandomPieceOfSolution(applicationsSubmitted, module, devicesPartitions,
							devices, r, cumulatedCostsEachDevice, nonFogNodes);

					if (pieceOfSolution == null) {
						// no suitable devices
						pieces.clear();
						break;
					}
					pieces.add(pieceOfSolution);
				}
				isOk = (!pieces.isEmpty()) && isAcceptableSolution(pieces, applicationsSubmitted);
			} while ((!isOk) && (iterationsLeft-- > 0));
		} catch (ParameterException e) {
			e.printStackTrace();
			return null;
		}

		return new Pair<>(solutionFactory.apply(pieces), cumulatedCostsEachDevice);
	}

	public static ListDevices[] partitionateDevicesByType(List<FogDevice> devices) {
		ListDevices[] devicesPartitions;
		DeviceNodeType[] types;
		types = DeviceNodeType.values();
		devicesPartitions = new ListDevices[types.length];
		for (DeviceNodeType dt : types) {
			devicesPartitions[dt.ordinal()] = new ListDevices();
		}
		devices.forEach(dd -> {
			devicesPartitions[dd.getDeviceNodeType().ordinal()].add(dd);
		});
		return devicesPartitions;
	}

	public static <S extends SolutionModulesDeployed> PieceOfSolution newRandomPieceOfSolution(
			Map<String, Application> applicationsSubmitted, AppModule module, final ListDevices[] devicesPartitions,
			List<FogDevice> devices, Random r) {
		return newRandomPieceOfSolution(applicationsSubmitted, module, devicesPartitions, devices, r, null, null);
	}

	public static <S extends SolutionModulesDeployed> PieceOfSolution newRandomPieceOfSolution(
			Map<String, Application> applicationsSubmitted, AppModule module, final ListDevices[] devicesPartitions,
			List<FogDevice> devices, Random r,
			// optional
			final SolutionDeployCosts<S> cumulatedCostsEachDevice, List<FogDevice> nonFogNodes) {
		Application app;
		FogDevice d;
		List<FogDevice> availableDevices;

		app = applicationsSubmitted.get(module.getAppId());
		if (app == null) {
			return null; // brutal interruption: constraint 1 can't be met
		}
		if (nonFogNodes == null) {
			nonFogNodes = new ArrayList<FogDevice>(
					devices.size() - devicesPartitions[DeviceNodeType.FogNode.ordinal()].size());
			for (DeviceNodeType dnt : DeviceNodeType.values()) {
				if (dnt != DeviceNodeType.FogNode) {
					devicesPartitions[dnt.ordinal()].forEach(nonFogNodes::add);
				}
			}
		}
		/**
		 * 1) select a list of devices suitable for the module type <br>
		 * 2) filter that list to the devices that have enough MIPS, storage and ram
		 * available AND capable to execute the module within the module's application's
		 * deadline <br>
		 * 3) random extraction (a "traveler backpack problem" is there: what is one of
		 * the combination of modules and devices that fits in the backpack made by the
		 * union of the device ones [== mips+storage+ram] and application ones [==
		 * deadline]? a future work may optimize this extraction moving from a random
		 * one to a "optimizing" one)
		 * 
		 */

		// 1)
		if (module.getModuleType() == ModuleType.Physical) {
			availableDevices = devicesPartitions[DeviceNodeType.CloudNode.ordinal()];
		} else if (!app.isDelayTolerable()) {
			availableDevices = devicesPartitions[DeviceNodeType.FogNode.ordinal()];
		} else {
			availableDevices = nonFogNodes;
		}

		// 2)
		availableDevices = availableDevices.stream() //
				.filter(dd -> {
					CumulatedCostsOnDevice cc;
					if (cumulatedCostsEachDevice != null) {
						if (cumulatedCostsEachDevice.contains(dd)) {
							cc = cumulatedCostsEachDevice.getCumulatedCosts(dd);
						} else {
							cc = new CumulatedCostsOnDevice(dd);
							cumulatedCostsEachDevice.addCumulatedCosts(cc);
						}
					} else {
						cc = new CumulatedCostsOnDevice(dd);
					}

					return cc.areNewCostsWithinLimits(module, applicationsSubmitted);
				})
				// preferred over "collect(Collectors.toList())" because I can decide what class
				// to instantiate (ArrayList)
				.reduce(new ArrayList<FogDevice>(), //
						// it's a "mapReduce" -> collect all devices in a list
						(ArrayList<FogDevice> l, FogDevice dd) -> {
							l.add(dd);
							return l;
						}, //
						(ArrayList<FogDevice> l1, ArrayList<FogDevice> l2) -> l2);

		if (availableDevices.isEmpty()) {
			// no suitable devices
			return null;
		}

		// 3(
		d = availableDevices.get(r.nextInt(availableDevices.size()));

		if (cumulatedCostsEachDevice != null) {
			CumulatedCostsOnDevice cc;
			if (cumulatedCostsEachDevice.contains(d)) {
				cc = cumulatedCostsEachDevice.getCumulatedCosts(d);
				cc.removeCostsOf(module, app);
			} else {
				cc = new CumulatedCostsOnDevice(d);
				cumulatedCostsEachDevice.addCumulatedCosts(cc);
			}
			cc.accumulateCostsOf(module, app);
		}

		/*
		 * the constraint over the module-device association are already implemented in
		 * the step 1 and 2
		 */
		return new PieceOfSolution(module, d);
	}

	/**
	 * Returns {@code true} if the given {@link AppModule} can be assigned to the
	 * given {@link FogDevice} by checking the constraints. The {@link Application}
	 * instance is provided by means of context.
	 * 
	 * @deprecated can't check for global constraints. Use
	 *             {@link #isAcceptableSolution(List, Map)} instead
	 */
	@Deprecated
	public static boolean isAcceptableAssociation(AppModule module, FogDevice device, Application app,
			final ListDevices[] devicesPartitions) {
		double[][] resourcesMetricsPairs;
		ModuleType mt;

		mt = module.getModuleType();

		// constraint 4
		if (mt == ModuleType.Physical && device.getDeviceNodeType() == DeviceNodeType.CloudNode) {
			return false;
		}

		// constraint 3, somehow
		resourcesMetricsPairs = resourcesMetricsPairs(module, device);
		for (double[] pair : resourcesMetricsPairs) {
			if (pair[0] > pair[1]) {
				return false;
			}
		}

		return true;
	}

	public static <S extends SolutionModulesDeployed> boolean isAcceptableSolution(S solution,
			Map<String, Application> apps) throws ParameterException {
		return isAcceptableSolution(solution.getPieces(), apps);
	}

	public static <S extends SolutionModulesDeployed> boolean isAcceptableSolution(final List<PieceOfSolution> pieces,
			Map<String, Application> apps) throws ParameterException {
		String id;
		AppModule mod;
		FogDevice d;
		Application app;
		SolutionDeployCosts<S> cumulatedCosts; // for each devices
		/**
		 * Sum each time spent by each AppModule of a given Application (the key) in
		 * order to check for deadlines
		 */
		final ApplicationModulesTimeTracker applicationModulesTimeTracker;
		CumulatedCostsOnDevice cc;

		cumulatedCosts = new SolutionDeployCosts<>();
		applicationModulesTimeTracker = new ApplicationModulesTimeTracker();

		// calculate the costs

		for (PieceOfSolution pos : pieces) {
			mod = pos.getModule();
			if (!apps.containsKey(mod.getAppId())) {
				throw new ParameterException("The current solution is unacceptable because the app with ID \""
						+ mod.getAppId() + "\" from the module \"" + mod.getName() + "\" is missing");
			}
			app = apps.get(mod.getAppId());

			d = pos.getDevice();

			id = d.getName();
			if (cumulatedCosts.contains(d)) {
				cc = cumulatedCosts.getCumulatedCosts(d);
			} else {
				cumulatedCosts.addCumulatedCosts(cc = new CumulatedCostsOnDevice(d));
			}

			cc.accumulateCostsOf(mod, app);
		}

		// check for constraints

		/*
		 * constraint 1 and 4 are assumed to be satisfied by the PieceOfSolution list
		 * generation procedure
		 */

		for (Entry<String, CumulatedCostsOnDevice> ecc : cumulatedCosts.getCumulatedCostsEachDevice().entrySet()) {
			cc = ecc.getValue();
			if (!cc.areCostsWithinLimits()) {
				return false; // constraint 3 not met
			}

			// register the total time spent on devices
			for (Entry<String, Double> eAppnameTime : cc.applicationModulesTimeTracker.cumulatedTimeEachApp_Modules
					.entrySet()) {
				applicationModulesTimeTracker.registerTimeSpent(eAppnameTime.getKey(), eAppnameTime.getValue());
			}
		}

		// check for deadlines
		for (Entry<String, Application> e : apps.entrySet()) {
			id = e.getKey();
			app = apps.get(id);
			if (applicationModulesTimeTracker.getCumulativeTimeOf(id) > app.getDeadlineMilliseconds()) {
				return false; // constraint 2 not met
			}
		}

		return true;
	}

	//

	public static class CumulatedCostsOnDevice {
		protected final Costs costs;
		/** Time spent on this device by each module */
		protected ApplicationModulesTimeTracker applicationModulesTimeTracker;
		protected Map<String, Costs> costsEachAppModule;
		public final FogDevice device;

		public CumulatedCostsOnDevice(FogDevice device) {
			super();
			this.device = device;
			this.costs = new Costs();
			this.applicationModulesTimeTracker = null;
		}

		public ApplicationModulesTimeTracker getApplicationModulesTimeTracker() {
			if (this.applicationModulesTimeTracker == null) {
				this.applicationModulesTimeTracker = new ApplicationModulesTimeTracker();
			}
			return applicationModulesTimeTracker;
		}

		public FogDevice getDevice() {
			return device;
		}

		/**
		 * @return the costsEachAppModule
		 */
		public Map<String, Costs> getCostsEachAppModule() {
			if (this.costsEachAppModule == null) {
				this.costsEachAppModule = new HashMap<>();
			}
			return this.costsEachAppModule;
		}

		/**
		 * The core method to track and accumulate the costs associated with the given
		 * {@link AppModule} (and its relative {@link Application} and the current
		 * {@link FogDevice} (retrieved by {@link #getDevice()}).
		 * 
		 * @param mod
		 * @param app
		 */
		public void accumulateCostsOf(AppModule mod, Application app) {
			double millionInstrLoaded;
			String modName;
			Costs costs;

			modName = mod.getName();
			if (this.getCostsEachAppModule().containsKey(modName)) {
				costs = this.costsEachAppModule.get(modName);
				this.costsEachAppModule.remove(modName);
				this.costs.remove(costs);
			} else {
				costs = new Costs();
			}

			millionInstrLoaded = getExpectedModuleMillionOfIstructionsToExec(mod, app);

			costs.ram = mod.getRam();
			costs.miLoaded = millionInstrLoaded;
			costs.storage = mod.getSize();
			costs.executionTime = millionInstrLoaded / device.getHost().getAvailableMips();
			this.costs.add(costs);
			this.addExecutionTime(app.getAppId(), costs.executionTime);
		}

		protected void removeCostsOf(String modName, Application app) {
			Costs costs;
			if (this.getCostsEachAppModule().containsKey(modName)) {
				costs = this.costsEachAppModule.get(modName);
				this.costsEachAppModule.remove(modName);
			} else {
				return;
			}
			this.applicationModulesTimeTracker.removeTimeSpent(app.getAppId(), costs.executionTime);
			this.costs.remove(costs);
		}

		public void removeCostsOf(AppModule mod, Application app) {
			this.removeCostsOf(mod.getName(), app);
		}

		/**
		 * See {@link #areNewCostsWithinLimits(int, double, long, Application, Map)}
		 * 
		 * @param mod        the module that is going to be tracked over the current
		 *                   device ({@link #getDevice()})
		 * @param appsByName a mapping to retrieve the application associated with the
		 *                   given module
		 * @return
		 */
		public boolean areNewCostsWithinLimits(AppModule mod, Map<String, Application> appsByName) {
			Application app;
			app = appsByName.get(mod.getAppId());
			return areNewCostsWithinLimits(mod.getRam(), getExpectedModuleMillionOfIstructionsToExec(mod, app),
					mod.getSize(), app);
		}

		/**
		 * Check the device capabilities and the application deadline
		 */
		public boolean areNewCostsWithinLimits(int moduleRam, double expectedModuleMillionOfIstructionsToExec,
				long moduleStorage, Application app) {
			double dev_mips = device.getHost().getAvailableMips();
			Objects.requireNonNull(app);

			return ((this.costs.ram + moduleRam) <= device.getHost().getRam()) && //
					((this.costs.miLoaded + expectedModuleMillionOfIstructionsToExec) <= dev_mips) && //
					((this.costs.storage + moduleStorage) <= device.getHost().getStorage()) && //
					(//
					((1000 * expectedModuleMillionOfIstructionsToExec / dev_mips)
							+ getApplicationModulesTimeTracker().getCumulativeTimeOf(app.getAppId())) //
							<= app.getDeadlineMilliseconds());
		}

		public boolean areCostsWithinLimits() {
			return (this.costs.ram <= device.getHost().getRam()) && //
					(this.costs.miLoaded <= device.getHost().getAvailableMips()) && //
					(this.costs.storage <= device.getHost().getStorage());
		}

		public void addExecutionTime(Application app, AppModule mod) {
			this.getApplicationModulesTimeTracker().registerTimeSpent(mod, device, app);
		}

		protected void addExecutionTime(String appID, double timeSpent) {
			this.getApplicationModulesTimeTracker().registerTimeSpent(appID, timeSpent);
		}

		protected static class Costs {
			public int ram;
			/** Million of instructions assigned to be executed on this device */
			public double miLoaded, executionTime;
			public long storage;

			public Costs() {
				super();
				this.miLoaded = 0.0;
				this.ram = 0;
				this.storage = 0;
				this.executionTime = 0.0;
			}

			public void add(Costs c) {
				this.miLoaded += c.miLoaded;
				this.ram += c.ram;
				this.storage += c.storage;
				this.executionTime += c.executionTime;
			}

			public void remove(Costs c) {
				this.miLoaded -= c.miLoaded;
				this.ram -= c.ram;
				this.storage -= c.storage;
				this.executionTime -= c.executionTime;
			}
		}
	}

	//

	/***
	 * Tracks the amount of time required to execute each {@link Application} by
	 * summing (and storing), for each application, the individual contributions of
	 * each {@link AppModule} composing that application.
	 * 
	 * @author marcoottina
	 *
	 */
	public static class ApplicationModulesTimeTracker {
		/**
		 * Mapping between application's name/ID to time.<br>
		 * The amount of time tracked (the map's value) is the total time spent by all
		 * modules inside each application (the latter is the map's key).
		 */
		public final Map<String, Double> cumulatedTimeEachApp_Modules;

		public ApplicationModulesTimeTracker() {
			this.cumulatedTimeEachApp_Modules = this.newMap();
		}

		protected Map<String, Double> newMap() {
			return new HashMap<>();
		}

		public void resetTracking() {
			this.cumulatedTimeEachApp_Modules.clear();
		}

		/**
		 * See {@link #registerTimeSpent(AppModule, FogDevice, Application)}. <br>
		 * The given map is used to determine the {@link Application} through
		 * {@link AppModule#getAppId()}
		 * 
		 * @param module
		 * @param deviceExecutingModule
		 * @param appsByID
		 */
		public void registerTimeSpent(AppModule module, FogDevice deviceExecutingModule,
				Map<String, Application> appsByID) {
			registerTimeSpent(module, deviceExecutingModule, appsByID.get(module.getAppId()));
		}

		/**
		 * See the parameters specifications.
		 * 
		 * @param module                {@link AppModule} whose execution time has to be
		 *                              tracked
		 * @param deviceExecutingModule {@link FogDevice} executing the given
		 *                              {@link AppModule}. Its computing capability
		 *                              determines the amount of time actually spent.
		 *                              See
		 *                              {@link SolutionsProducerEvaluator#executionTime(AppModule, FogDevice, Application)}.
		 * @param appOwningModule       the {@link Application} this {@link AppModule}
		 *                              belongs to.
		 */
		public void registerTimeSpent(AppModule module, FogDevice deviceExecutingModule, Application appOwningModule) {
			String appID;
			appID = appOwningModule.getAppId();
			if (!appID.equals(module.getAppId())) {
				throw new IllegalArgumentException("app's ID \"" + appID
						+ "\" is not the same of the module's app ID \"" + module.getAppId() + "\"!");
			}
			registerTimeSpent(appID, executionTime(module, deviceExecutingModule, appOwningModule));
		}

		/**
		 * Raw internal implementation used by
		 * {@link #registerTimeSpent(AppModule, FogDevice, Application)}
		 */
		public void registerTimeSpent(String appID, double timeSpent) {
			if (cumulatedTimeEachApp_Modules.containsKey(appID)) {
				timeSpent += cumulatedTimeEachApp_Modules.get(appID);
				cumulatedTimeEachApp_Modules.remove(appID);
			}
			cumulatedTimeEachApp_Modules.put(appID, timeSpent);
		}

		/**
		 * Reverse of {@link #registerTimeSpent(String, double)}
		 */
		public void removeTimeSpent(String appID, double timeSpent) {
			if (!cumulatedTimeEachApp_Modules.containsKey(appID)) {
				return;
			}
			timeSpent = cumulatedTimeEachApp_Modules.get(appID) - timeSpent;
			cumulatedTimeEachApp_Modules.remove(appID);
			cumulatedTimeEachApp_Modules.put(appID, timeSpent);
		}

		/**
		 * Reverse of {@link #registerTimeSpent(AppModule, FogDevice, Application) }
		 */
		public void removeTimeSpent(AppModule module, FogDevice deviceExecutingModule, Application appOwningModule) {
			String appID;
			appID = appOwningModule.getAppId();
			if (!appID.equals(module.getAppId())) {
				throw new IllegalArgumentException("app's ID \"" + appID
						+ "\" is not the same of the module's app ID \"" + module.getAppId() + "\"!");
			}
			removeTimeSpent(appID, executionTime(module, deviceExecutingModule, appOwningModule));
		}

		/**
		 * See return section
		 * 
		 * @param appID application ID (got from {@link Application#getAppId()} or
		 *              {@link AppModule#getAppId()})
		 * @return the currently tracked sum of time spent by some (none/some/all)
		 *         {@link AppModule} associated with the {@link Application} which the
		 *         given ID belongs to.
		 */
		public double getCumulativeTimeOf(String appID) {
			if (this.cumulatedTimeEachApp_Modules.containsKey(appID)) {
				return this.cumulatedTimeEachApp_Modules.get(appID);
			} else {
				return 0.0;
			}
		}
	}

	public static class SolutionDeployCosts<S extends SolutionModulesDeployed> {
		protected final Map<String, CumulatedCostsOnDevice> cumulatedCostsEachDevice;
		protected S solution;

		public SolutionDeployCosts() {
			this.cumulatedCostsEachDevice = new HashMap<>();
			this.solution = null;
		}

		public SolutionDeployCosts(S solution) {
			this();
			this.solution = solution;
		}

		public Map<String, CumulatedCostsOnDevice> getCumulatedCostsEachDevice() {
			return cumulatedCostsEachDevice;
		}

		public S getSolution() {
			return solution;
		}

		public CumulatedCostsOnDevice getCumulatedCosts(FogDevice device) {
			String deviceName;
			CumulatedCostsOnDevice c;
			deviceName = device.getName();
			if (this.cumulatedCostsEachDevice.containsKey(deviceName)) {
				c = this.cumulatedCostsEachDevice.get(deviceName);
			} else {
				this.cumulatedCostsEachDevice.put(deviceName, c = new CumulatedCostsOnDevice(device));
			}
			return c;
		}

		public void clear() {
			this.cumulatedCostsEachDevice.clear();
		}

		public boolean contains(FogDevice d) {
			return this.cumulatedCostsEachDevice.containsKey(d.getName());
		}

		public void remove(FogDevice d) {
			this.cumulatedCostsEachDevice.remove(d.getName());
		}

		public void addCumulatedCosts(CumulatedCostsOnDevice cc) {
			FogDevice d;
			d = cc.getDevice();
			if (this.contains(d)) {
				this.remove(d);
			}
			this.cumulatedCostsEachDevice.put(d.getName(), cc);
		}
	}

	protected static class ListAppMod extends ArrayList<AppModule> {
		private static final long serialVersionUID = 1L;

		public ListAppMod() {
			super();
		}

		public ListAppMod(Collection<? extends AppModule> c) {
			super(c);
		}

		public ListAppMod(int initialCapacity) {
			super(initialCapacity);
		}
	}
}