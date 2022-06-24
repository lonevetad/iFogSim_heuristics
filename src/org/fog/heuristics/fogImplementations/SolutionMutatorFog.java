package org.fog.heuristics.fogImplementations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;

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
 * <p>
 * 
 * @author marcoottina (marco.1995.ottina@gmail.com )
 */
public class SolutionMutatorFog<S extends SolutionModulesDeployed> implements SolutionMutator<PieceOfSolution, S> {

	@Override
	public PieceOfSolution mutateFragmentOfSolution(PieceOfSolution originalFragment, S solutionContext,
			Heuristic<S> heuristicContext, Random r) {
		return SolutionsProducerEvaluator.newRandomPieceOfSolution(getApplicationsSubmitted(),
				originalFragment.getModule(), getDevicesPartitions(), getDevices(), r,
				this.costsSolutions.get(solutionContext), null);
	}

	protected ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo;
	protected Map<S, SolutionDeployCosts<S>> costsSolutions;

	public SolutionMutatorFog() {
		super();
		this.costsSolutions = new HashMap<>();
	}

	public void resetContext(ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo) {
		this.modPlacementAdditionalInfo = modPlacementAdditionalInfo;
	}

	/**
	 * @return the costsSolutions
	 */
	public Map<S, SolutionDeployCosts<S>> getCostsSolutions() {
		return costsSolutions;
	}

	public Map<String, Application> getApplicationsSubmitted() {
		return this.modPlacementAdditionalInfo.getApplicationsByID();
	}

	public List<AppModule> getModules() {
		return this.modPlacementAdditionalInfo.getModules();
	}

	public List<FogDevice> getDevices() {
		return this.modPlacementAdditionalInfo.getDevices();
	}

	public ListDevices[] getDevicesPartitions() {
		return this.modPlacementAdditionalInfo.getDevicesPartitions();
	}

	//

	/**
	 * DO NOT USE IT! <br>
	 * FOR HEURISTICS ONLY!
	 */
	public void saveSolutionCostsInCache(S solution, SolutionDeployCosts<S> costs) {
		this.costsSolutions.put(solution, costs);
	}

	public boolean hasSolutionCost(S solution) {
		return this.costsSolutions.containsKey(solution);
	}

	public SolutionDeployCosts<S> getCostSolution(S solution) {
		return this.costsSolutions.get(solution);
	}

	public void removeCostSolution(S solution) {
		this.costsSolutions.remove(solution);
	}

	public void forEachCostSolution(BiConsumer<S, SolutionDeployCosts<S>> action) {
		this.costsSolutions.forEach(action);
	}
}
