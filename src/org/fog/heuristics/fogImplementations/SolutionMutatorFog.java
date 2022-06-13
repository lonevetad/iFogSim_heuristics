package org.fog.heuristics.fogImplementations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.heuristics.Heuristic;
import org.fog.heuristics.SolutionMutator;
import org.fog.heuristics.SolutionsProducerEvaluator;
import org.fog.heuristics.SolutionsProducerEvaluator.SolutionDeployCosts;
import org.fog.heuristics.algorithms.ga.GeneticAlgorithm;

/**
 * Utility class that both provides the so-called "mutations" (used in
 * {@link GeneticAlgorithm}) and a way to generate a random
 * {@link PieceOfSolution} that satisfy some design constraints (through
 * {@link #newRandomSolution(AppModule, List, Random)}).
 * <p>
 * Beware: its' NOT synchronized! Different instances must be created in order
 * to not mess up the results!
 */
public class SolutionMutatorFog<S extends SolutionModulesDeployed> implements SolutionMutator<PieceOfSolution, S> {

	@Override
	public PieceOfSolution mutateFragmentOfSolution(PieceOfSolution originalFragment, S solutionContext,
			Heuristic<S> heuristicContext, Random r) {
		return SolutionsProducerEvaluator.newRandomPieceOfSolution(getApplicationsSubmitted(),
				originalFragment.getModule(), getDevicesPartitions(), getDevices(), r,
				this.costsSolutions.get(solutionContext), null);
	}

	protected Map<String, Application> applicationsSubmitted;
	protected List<AppModule> modules;
	protected List<FogDevice> devices;
	protected ListDevices[] devicesPartitions;
	protected Map<S, SolutionDeployCosts<S>> costsSolutions;

	public SolutionMutatorFog() {
		super();
		this.costsSolutions = new HashMap<>();
	}

	public void resetContext(Map<String, Application> applicationsSubmitted, List<AppModule> modules,
			List<FogDevice> devices) {
		resetContext(applicationsSubmitted, modules, devices,
				SolutionsProducerEvaluator.partitionateDevicesByType(devices));
	}

	public void resetContext(Map<String, Application> applicationsSubmitted, List<AppModule> modules,
			List<FogDevice> devices, ListDevices[] devicesPartitions) {
		this.applicationsSubmitted = applicationsSubmitted;
		this.modules = modules;
		this.devices = devices;
		this.devicesPartitions = devicesPartitions;
	}

	public Map<String, Application> getApplicationsSubmitted() {
		return applicationsSubmitted;
	}

	public List<AppModule> getModules() {
		return modules;
	}

	public List<FogDevice> getDevices() {
		return devices;
	}

	public ListDevices[] getDevicesPartitions() {
		return devicesPartitions;
	}

	//

	/**
	 * DO NOT USE IT! <br>
	 * FOR HEURISTICS ONLY!
	 */
	public void saveSolutionCostsInCache(S solution, SolutionDeployCosts<S> costs) {
		this.costsSolutions.put(solution, costs);
	}
}
