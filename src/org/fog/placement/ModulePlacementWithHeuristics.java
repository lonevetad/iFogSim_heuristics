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
import org.fog.heuristics.fogImplementations.SolutionModulesDeployed;
import org.fog.heuristics.fogImplementations.SolutionMutatorFog;
import org.fog.heuristics.fogImplementations.ga.ChromosomeFog;
import org.fog.heuristics.fogImplementations.ga.GeneticAlgorithmFog;

/**
 * TODO: Should generate a {@link SolutionModulesDeployed}.
 */
public class ModulePlacementWithHeuristics extends ModulePlacement {
	public static enum HeuristicAccepted implements HeuristicFogFactory {
		GeneticAlgorith( //
				new HeuristicFogFactory() {
					@Override
					public <S extends SolutionModulesDeployed> HeuristicFog newInstance(
							Map<String, Application> applicationsSubmitted, List<AppModule> modules,
							List<FogDevice> devices, SolutionMutatorFog<S> mutator) {
						return new GeneticAlgorithmFog(applicationsSubmitted, modules, devices,
								new SolutionMutatorFog<ChromosomeFog>());
					}
				});

		HeuristicAccepted(HeuristicFogFactory factory) {
			this.factory = factory;
		}

		private final HeuristicFogFactory factory;

		@Override
		public <S extends SolutionModulesDeployed> HeuristicFog newInstance(
				Map<String, Application> applicationsSubmitted, List<AppModule> modules, List<FogDevice> devices,
				SolutionMutatorFog<S> mutator) {
			HeuristicFog h;
			h = this.factory.newInstance(applicationsSubmitted, modules, devices, mutator);
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

	public ModulePlacementWithHeuristics(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators,
			List<Application> applications, double thresholdProcessPower, double thresholdSolutionEvaluationImprovement,
			double thresholdDifferenceSolutions) {
		super();
		this.setFogDevices(fogDevices);
//		this.setApplication(application);
		this.setModuleToDeviceMap(new HashMap<>());
		this.setDeviceToModuleMap(new HashMap<>());
		this.setSensors(sensors);
		this.setActuators(actuators);
		this.setDeviceToModuleMap(new HashMap<>());
		this.setApplications(applications);
		this.thresholdProcessPower = thresholdProcessPower;
		this.thresholdSolutionEvaluationImprovement = thresholdSolutionEvaluationImprovement;
		this.thresholdDifferenceSolutions = thresholdDifferenceSolutions;

		this.mapModules();
	}

	protected double thresholdProcessPower, thresholdSolutionEvaluationImprovement, thresholdDifferenceSolutions;
	protected List<Sensor> sensors;
	protected List<Actuator> actuators;
	protected Map<Integer, List<AppModule>> deviceToModuleMap;
	protected List<Application> applications;

	//

	public List<Sensor> getSensors() {
		return sensors;
	}

	public List<Actuator> getActuators() {
		return actuators;
	}

	@Override
	public Map<Integer, List<AppModule>> getDeviceToModuleMap() {
		return deviceToModuleMap;
	}

	public List<Application> getApplications() {
		return applications;
	}

	/**
	 * Should be a value between 0 and 1
	 */
	public double getThresholdProcessPower() {
		return thresholdProcessPower;
	}

	/**
	 * Should be a value between 0 and 1 .
	 */
	public double getThresholdDifferenceSolutions() {
		return thresholdDifferenceSolutions;
	}

	//

	public void setApplications(List<Application> applications) {
		this.applications = applications;
	}

	public void setSensors(List<Sensor> sensors) {
		this.sensors = sensors;
	}

	public void setActuators(List<Actuator> actuators) {
		this.actuators = actuators;
	}

	@Override
	public void setDeviceToModuleMap(Map<Integer, List<AppModule>> deviceToModuleMap) {
		this.deviceToModuleMap = deviceToModuleMap;
	}

	public void setThresholdProcessPower(double thresholdProcessPower) {
		this.thresholdProcessPower = thresholdProcessPower;
	}

	/**
	 * */
	@Override
	protected void mapModules() {
		SolutionModulesDeployed solution;
		ApplicationsPlacements ap;
		Map<String, FogDevice> mapModuleDevice;
		final Consumer<AppModule> modAdder;

		// 1) create the module->device map
		ap = deadlineAwareEnergyEfficientApplicationPlacement();
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
			if (deviceToModuleMap.containsKey(devId)) {
				lam = deviceToModuleMap.get(devId);
			} else {
				lam = new ArrayList<>();
				deviceToModuleMap.put(devId, lam);
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

	protected ApplicationsPlacements deadlineAwareEnergyEfficientApplicationPlacement() {
		final double w;
		ApplicationsPlacements ap;
		PriorityQueue<Application> pq;
		SolutionModulesDeployed solution;
		Application app;

		solution = modulePlacement(MAX_ITERATIONS);
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

	protected SolutionModulesDeployed modulePlacement(int maxIterations) {
		boolean improvedEnough, diverseEnought;
		double lastEvaluation, evaluation;
		long difference, maxDifference;
		Random r;
		HeuristicFog h;
		SolutionMutatorFog<SolutionModulesDeployed> mutator;
		final Map<String, Application> mapAppl;
		final Map<String, AppModule> mapMod; // temporary variable
		List<AppModule> modules;
		SolutionModulesDeployed solution, prevSolution;

		r = new Random();
		mapAppl = new HashMap<>();
		mapMod = new HashMap<>();

		getApplications().forEach(app -> {
			mapAppl.put(app.getAppId(), app);

			app.getModules().forEach(m -> mapMod.put(m.getName(), m));
		});

		modules = new ArrayList<>(mapMod.size());
		mapMod.forEach((n, m) -> {
			modules.add(m);
		});
		mapMod.clear();

		mutator = new SolutionMutatorFog<>();
		solution = prevSolution = null;
		lastEvaluation = 0.0;
		h = ALL_HEURISTICS[r.nextInt(ALL_HEURISTICS.length)].newInstance(mapAppl, modules, getFogDevices(), mutator);
		do {
			mutator.resetContext(mapAppl, modules, getFogDevices());

			solution = h.optimize(solution, maxIterations, r);
			evaluation = SolutionsProducerEvaluator.evaluateSolution(solution, mapAppl);

			improvedEnough = (Math.abs(lastEvaluation - evaluation)
					/ lastEvaluation) > thresholdSolutionEvaluationImprovement;

			maxDifference = Math.max(solution.getPieces().size(),
					(prevSolution == null ? 0 : prevSolution.getPieces().size()));

			difference = SolutionsProducerEvaluator.evaluateDifference(solution, prevSolution);
			diverseEnought = (((double) difference) / ((double) maxDifference)) <= thresholdDifferenceSolutions;

			lastEvaluation = evaluation;
			if (govern(h, improvedEnough, diverseEnought)) {
				h = ALL_HEURISTICS[r.nextInt(ALL_HEURISTICS.length)].newInstance(mapAppl, modules, getFogDevices(),
						mutator);

				// fine tune??
				prevSolution = solution;
				solution = h.optimize(solution, maxIterations, r);
			} else {
				prevSolution = solution;
			}
		} while (improvedEnough && maxIterations-- > 0);
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
