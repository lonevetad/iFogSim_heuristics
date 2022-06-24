package org.fog.placement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.function.Consumer;

import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.heuristics.Heuristic.HeuristicType;
import org.fog.heuristics.SolutionsProducerEvaluator;
import org.fog.heuristics.fogImplementations.HeuristicFog;
import org.fog.heuristics.fogImplementations.HeuristicFogFactory;
import org.fog.heuristics.fogImplementations.ModulePlacementAdditionalInformationFog;
import org.fog.heuristics.fogImplementations.ModulePlacementAdditionalInformationFog.DeviceNodeTypesLatencyMap;
import org.fog.heuristics.fogImplementations.SolutionModulesDeployed;
import org.fog.heuristics.fogImplementations.SolutionMutatorFog;
import org.fog.heuristics.fogImplementations.ga.ChromosomeFog;
import org.fog.heuristics.fogImplementations.ga.GeneticAlgorithmFog;
import org.fog.heuristics.fogImplementations.sa.SASolutionFog;
import org.fog.heuristics.fogImplementations.sa.SimulatedAnnealingFog;

/**
 * DISCLAIMER: <br>
 * The following documentation, as well as EACH code produced by the author
 * (me), comes from the analysis of the code, its interaction and a specific
 * paper (more of it below). It comes with no warranty and the usage of some
 * other parts of code (like {@code FogDevice#setUplinkLatency(double)}) may
 * contains some error. In that case, please contact me. I apologize in case of
 * troubles.<br>
 * Information of mentioned paper: <br>
 * <ul>
 * <li>Title: DEEDSP: Deadline-aware and energy-efficient dynamic service
 * placement in integrated Internet of Things and fog computing environments
 * </li>
 * <li>Authors: Meeniga Sri Raghavendra, Priyanka Chawla and Sukhpal Singh Gill
 * </li>
 * <li>DOI: 10.1002/ett.4368</li>
 * </ul>
 * 
 * <p>
 * 
 * Documentation:
 * <p>
 * Communication, expressed in milliseconds, time from this device to its parent
 * ({@link #getParentId()}).
 * <p>
 * 
 * 
 * @author marcoottina (marco.1995.ottina@gmail.com )
 *         <p>
 * 
 */
public class ModulePlacementWithHeuristics extends ModulePlacement {
	public static enum HeuristicAccepted implements HeuristicFogFactory {
		GeneticAlgorith( //
				new HeuristicFogFactory() {
					@Override
					public <S extends SolutionModulesDeployed> HeuristicFog newInstance(
							ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo,
							SolutionMutatorFog<S> mutator) {
						SolutionMutatorFog<ChromosomeFog> mut;
						mut = new SolutionMutatorFog<ChromosomeFog>();
						mut.setModPlacementAdditionalInfo(modPlacementAdditionalInfo);
						return new GeneticAlgorithmFog(modPlacementAdditionalInfo, mut);
					}
				}), //
		SimulatedAnnealing(new HeuristicFogFactory() {
			@Override
			public <S extends SolutionModulesDeployed> HeuristicFog newInstance(
					ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo, SolutionMutatorFog<S> mutator) {
				SolutionMutatorFog<SASolutionFog> mut;
				mut = new SolutionMutatorFog<SASolutionFog>();
				mut.setModPlacementAdditionalInfo(modPlacementAdditionalInfo);
				return new SimulatedAnnealingFog(modPlacementAdditionalInfo, mut);
			}
		});

		HeuristicAccepted(HeuristicFogFactory factory) {
			this.factory = factory;
		}

		private final HeuristicFogFactory factory;

		@Override
		public <S extends SolutionModulesDeployed> HeuristicFog newInstance(
				ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo, SolutionMutatorFog<S> mutator) {
			HeuristicFog h;
			h = this.factory.newInstance(modPlacementAdditionalInfo, mutator);
			return h;
		}
	}

	private static final HeuristicAccepted[] ALL_HEURISTICS = HeuristicAccepted.values();

	public static final Comparator<Application> APP_COMPARATOR = (a1, a2) -> {
		double priority1, priority2;
		if (a1 == a2) {
			return 0;
		}
		if (a1 == null) {
			return -1;
		}
		if (null == a2) {
			return 1;
		}
		priority1 = a1.getDeadlineMilliseconds() - a1.getDeploymentTimeMilliseconds();
		priority2 = a2.getDeadlineMilliseconds() - a2.getDeploymentTimeMilliseconds();

		if (priority1 == priority2) {
			return 0;
		}
		return (priority1 > priority2) ? 1 : -1;
	};

	public static final int MAX_ITERATIONS = 50;

	//

	public ModulePlacementWithHeuristics(//
			double thresholdProcessPower, double thresholdSolutionEvaluationImprovement,
			double thresholdDifferenceSolutions, ModuleMapping moduleMapping, List<FogDevice> fogDevices,
			List<Sensor> sensors, List<Actuator> actuators, List<Application> applications,
			DeviceNodeTypesLatencyMap latenciesBetweenDeviceTypes) {
		super();
		this.setModuleToDeviceMap(new HashMap<>());
		this.setDeviceToModuleMap(new HashMap<>());
		this.setModuleMapping(moduleMapping);
		this.setFogDevices(fogDevices);
//		this.setApplication(null);
		this.setSensors(sensors);
		this.setActuators(actuators);
		this.applicationsByID = null;
		this.modulesByName = null;
		this.modules = null;
		this.setApplications(applications);
		this.thresholdProcessPower = thresholdProcessPower;
		this.thresholdSolutionEvaluationImprovement = thresholdSolutionEvaluationImprovement;
		this.thresholdDifferenceSolutions = thresholdDifferenceSolutions;
		this.latenciesBetweenDeviceTypes = latenciesBetweenDeviceTypes;

		this.mapModules();
	}

	protected double thresholdProcessPower, thresholdSolutionEvaluationImprovement, thresholdDifferenceSolutions;
	protected ModuleMapping moduleMapping;
	protected List<Sensor> sensors;
	protected List<Actuator> actuators;
	protected List<AppModule> modules;
	protected List<Application> applications;
	protected DeviceNodeTypesLatencyMap latenciesBetweenDeviceTypes;

	protected Map<String, AppModule> modulesByName;
	protected Map<String, Application> applicationsByID;

	//

	/**
	 * @return the thresholdSolutionEvaluationImprovement
	 */
	public double getThresholdSolutionEvaluationImprovement() {
		return thresholdSolutionEvaluationImprovement;
	}

	/**
	 * Should be a value between 0 and 1
	 */
	public double getThresholdProcessPower() {
		return thresholdProcessPower;
	}

	public List<Application> getApplications() {
		return applications;
	}

	/**
	 * Should be a value between 0 and 1 .
	 */
	public double getThresholdDifferenceSolutions() {
		return thresholdDifferenceSolutions;
	}

	/**
	 * @return the modules
	 */
	public List<AppModule> getModules() {
		return modules;
	}

	public List<Sensor> getSensors() {
		return sensors;
	}

	public List<Actuator> getActuators() {
		return actuators;
	}

	/**
	 * @return the moduleMapping
	 */
	public ModuleMapping getModuleMapping() {
		return moduleMapping;
	}

	/**
	 * @return the modulesByName
	 */
	public Map<String, AppModule> getModulesByName() {
		return modulesByName;
	}

	/**
	 * @return the applicationsByID
	 */
	public Map<String, Application> getApplicationsByID() {
		return applicationsByID;
	}

	/**
	 * @return the latenciesBetweenDeviceTypes
	 */
	public DeviceNodeTypesLatencyMap getLatenciesBetweenDeviceTypes() {
		return latenciesBetweenDeviceTypes;
	}

	//

	public void setApplications(List<Application> applications) {
		final Map<String, Application> mapApp;
		final Map<String, AppModule> mapMod;
		final List<AppModule> mods;

		this.applications = applications;
		if (applications == null || applications.isEmpty()) {
			this.modules = null;
			this.modulesByName = null;
			this.applicationsByID = null;
			return;
		}
		this.modulesByName = mapMod = new HashMap<>();
		this.applicationsByID = mapApp = new HashMap<>();

		applications.forEach(app -> {
			mapApp.put(app.getAppId(), app);

			app.getModules().forEach(m -> mapMod.put(m.getName(), m));
		});

		mods = new ArrayList<>(mapMod.size());
		mapMod.forEach((n, m) -> {
			mods.add(m);
		});
		this.setModules(mods);
	}

	/**
	 * @param latenciesBetweenDeviceTypes the latenciesBetweenDeviceTypes to set
	 */
	public void setLatenciesBetweenDeviceTypes(DeviceNodeTypesLatencyMap latenciesBetweenDeviceTypes) {
		this.latenciesBetweenDeviceTypes = latenciesBetweenDeviceTypes;
	}

	/**
	 * @param modules the modules to set
	 */
	public void setModules(List<AppModule> modules) {
		this.modules = modules;
	}

	/**
	 * @param moduleMapping the moduleMapping to set
	 */
	public void setModuleMapping(ModuleMapping moduleMapping) {
		this.moduleMapping = moduleMapping;
	}

	public void setSensors(List<Sensor> sensors) {
		this.sensors = sensors;
	}

	public void setActuators(List<Actuator> actuators) {
		this.actuators = actuators;
	}

	public void setThresholdProcessPower(double thresholdProcessPower) {
		this.thresholdProcessPower = thresholdProcessPower;
	}

	//

	/**
	 * TODO: Should generate a {@link SolutionModulesDeployed}.
	 */
	@Override
	protected void mapModules() {
		SolutionModulesDeployed solution;
		ApplicationsPlacements ap;
		Map<String, FogDevice> mapModuleDevice;
		final Consumer<AppModule> modAdder;

		ModulePlacementAdditionalInformationFog additionalInformation;

		additionalInformation = new ModulePlacementAdditionalInformationFog(getModuleMapping(), getModulesByName(),
				getApplicationsByID(), getModules(), getFogDevices(), getLatenciesBetweenDeviceTypes());

		//

		// 1) create the module->device map
		ap = deadlineAwareEnergyEfficientApplicationPlacement(additionalInformation);
		solution = ap.solutionModulesDeployed;

		Objects.requireNonNull(solution);

		mapModuleDevice = new HashMap<>();
		solution.getPieces().forEach(p -> {
			Integer devId;
			String modName;
			List<AppModule> lam;
			List<Integer> ldi;
			Map<String, List<Integer>> mtdm;

			devId = p.getDevice().getId();
			if (getDeviceToModuleMap().containsKey(devId)) {
				lam = getDeviceToModuleMap().get(devId);
			} else {
				lam = new ArrayList<>();
				getDeviceToModuleMap().put(devId, lam);
			}
			if (!lam.contains(p.getModule())) {
				lam.add(p.getModule());
			}

			modName = p.getModule().getName();
			mtdm = getModuleToDeviceMap();
			if (mtdm.containsKey(modName)) {
				ldi = mtdm.get(modName);
			} else {
				ldi = new ArrayList<>();
				mtdm.put(modName, ldi);
			}
			if (!ldi.contains(devId)) {
				ldi.add(devId);
			}

			//

			mapModuleDevice.put(modName, p.getDevice());
		});

		// 2) run that map to invoke "createModuleInstanceOnDevice(module,device)"
		modAdder = m -> {
			createModuleInstanceOnDevice(m, mapModuleDevice.get(m.getName()));
		};
		ap.highCostQueue.forEach(modAdder);
		ap.lowCostQueue.forEach(modAdder);
		ap.sensorsActuatorsQueue.forEach(modAdder);

	}

	protected ApplicationsPlacements deadlineAwareEnergyEfficientApplicationPlacement(
			ModulePlacementAdditionalInformationFog additionalInformation) {
		final double w;
		ApplicationsPlacements ap;
		PriorityQueue<Application> pq;
		SolutionModulesDeployed solution;
		Application app;

		solution = modulePlacement(MAX_ITERATIONS, additionalInformation);
		Objects.requireNonNull(solution, "SOLUTION NULL IN DEADLINE AWARE ENERGU EFFICIENT APPLICATION PLACEMENT");

		ap = new ApplicationsPlacements();
		ap.setSolutionModulesDeployed(solution);
//		this.sensors.forEach(ap.sensorsActuatorsQueue::add);
//		this.actuators.forEach(ap.sensorsActuatorsQueue::add);

		pq = new PriorityQueue<>(APP_COMPARATOR);
		this.getApplications().forEach(pq::add);

		w = this.getThresholdProcessPower(); // not used

		while (!pq.isEmpty()) {
			app = pq.poll();
			app.getModules().forEach(m -> {
				if (modulePower(m) > w) {
					ap.highCostQueue.add(m);
				} else {
					ap.lowCostQueue.add(m);
				}
			});
		}

		// REFER TO THE PDF page 11 - Algorithm 1

		return ap;
	}

	protected double modulePower(AppModule m) {
		// TODO check this
		return m.getMips() + m.getBw() + m.getRam();
	}

	protected SolutionModulesDeployed modulePlacement(int maxIterations,
			ModulePlacementAdditionalInformationFog additionalInformation) {
		boolean improvedEnough, diverseEnought;
		double lastEvaluation, evaluation;
		long difference, maxDifference;
		Random r;
		HeuristicFog h;
		SolutionMutatorFog<SolutionModulesDeployed> mutator;
		SolutionModulesDeployed solution, prevSolution;

		r = new Random();
//		mapAppl = new HashMap<>();
//		mapMod = new HashMap<>();
//
//		getApplications().forEach(app -> {
//			mapAppl.put(app.getAppId(), app);
//
//			app.getModules().forEach(m -> mapMod.put(m.getName(), m));
//		});
//
//		modules = new ArrayList<>(mapMod.size());
//		mapMod.forEach((n, m) -> {
//			modules.add(m);
//		});
//		mapMod.clear();

		mutator = new SolutionMutatorFog<>();
		solution = prevSolution = null;
		lastEvaluation = 0.0;
		h = ALL_HEURISTICS[r.nextInt(ALL_HEURISTICS.length)].newInstance(additionalInformation, mutator);
		do {
//			mutator.resetContext(additionalInformation.getApplicationsByID(), modules, getFogDevices());

			solution = h.optimize(solution, maxIterations, r); // TODO : add ModuleToDeviceConstrain in order to respect
																// the module->device association constraints
			evaluation = SolutionsProducerEvaluator.evaluateSolution(solution, additionalInformation);

			improvedEnough = (Math.abs(lastEvaluation - evaluation)
					/ lastEvaluation) > thresholdSolutionEvaluationImprovement;

			maxDifference = Math.max(solution.getPieces().size(),
					(prevSolution == null ? 0 : prevSolution.getPieces().size()));

			difference = SolutionsProducerEvaluator.evaluateDifference(solution, prevSolution);
			diverseEnought = (((double) difference) / ((double) maxDifference)) <= thresholdDifferenceSolutions;

			lastEvaluation = evaluation;
			if (govern(h, improvedEnough, diverseEnought)) {
				h = ALL_HEURISTICS[r.nextInt(ALL_HEURISTICS.length)].newInstance(additionalInformation, mutator);

				// fine tune??
				prevSolution = solution;
				solution = h.optimize(solution, maxIterations, r);
			} else {
				prevSolution = solution;
			}
			System.out.println("module placement " + maxIterations + " iterations left");
		} while (improvedEnough && maxIterations-- > 0);

		System.out.println("solution: " + solution);
		return solution;
	}

	protected boolean govern(HeuristicFog h, boolean improvedEnough, boolean diverseEnought) {
		HeuristicType ht;
		ht = h.getHeuristicType();
		return !((ht == HeuristicType.SingleSolution && improvedEnough)
				|| (ht == HeuristicType.PopulationBased && improvedEnough && diverseEnought));
	}

	//

	protected static class ApplicationsPlacements {
		protected final List<AppModule> highCostQueue, lowCostQueue, sensorsActuatorsQueue;
		protected SolutionModulesDeployed solutionModulesDeployed;

		public ApplicationsPlacements() {
			super();
			highCostQueue = new LinkedList<>();
			lowCostQueue = new LinkedList<>();
			sensorsActuatorsQueue = new LinkedList<>();
		}

		public List<AppModule> getHighCostQueue() {
			return highCostQueue;
		}

		public List<AppModule> getLowCostQueue() {
			return lowCostQueue;
		}

		public List<AppModule> getSensorsActuatorsQueue() {
			return sensorsActuatorsQueue;
		}

		public SolutionModulesDeployed getSolutionModulesDeployed() {
			return solutionModulesDeployed;
		}

		//

		public void setSolutionModulesDeployed(SolutionModulesDeployed solutionModulesDeployed) {
			this.solutionModulesDeployed = solutionModulesDeployed;
		}

		@Override
		public String toString() {
			final StringBuilder sb;
			sb = new StringBuilder(1024);

			sb.append("ApplicationsPlacements [highCostQueue=").append(highCostQueue.size()).append(", lowCostQueue=")
					.append(lowCostQueue.size()).append(", sensorsActuatorsQueue=").append(sensorsActuatorsQueue.size())
					.append(", solutionModulesDeployed=\n\t");

			solutionModulesDeployed.getPieces().forEach(p -> sb.append("\n\t").append(p.toString()));
			sb.append("\n]");

			return sb.toString();
		}

	}
}
