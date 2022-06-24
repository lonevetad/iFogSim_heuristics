package org.fog.heuristics.fogImplementations.sa;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.util.Pair;
import org.fog.heuristics.SolutionsProducerEvaluator;
import org.fog.heuristics.SolutionsProducerEvaluator.SolutionDeployCosts;
import org.fog.heuristics.algorithms.sa.SimulatedAnnealing;
import org.fog.heuristics.fogImplementations.HeuristicFog;
import org.fog.heuristics.fogImplementations.ModulePlacementAdditionalInformationFog;
import org.fog.heuristics.fogImplementations.SolutionModulesDeployed;
import org.fog.heuristics.fogImplementations.SolutionMutatorFog;

/**
 * 
 * @author marcoottina (marco.1995.ottina@gmail.com )
 * 
 */
public class SimulatedAnnealingFog extends SimulatedAnnealing<SolutionModulesDeployed> implements HeuristicFog {

	protected ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo;
	protected SolutionMutatorFog<SASolutionFog> mutationProvider;

	public SimulatedAnnealingFog(ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo) {
		super();
		this.setModPlacementAdditionalInfo(modPlacementAdditionalInfo);
		// values taken from Table 6
		this.setStartingTemperature(10.0);
		this.setCoolingRate(0.05);
		// randomly decided amounts
		this.setMaxAmountNeighbour(100);
	}

	public SimulatedAnnealingFog(ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo,
			SolutionMutatorFog<SASolutionFog> mutationProvider) {
		this(modPlacementAdditionalInfo);
		if (mutationProvider != null) {
			this.setMutationProvider(mutationProvider);
		}
	}

	//

	/**
	 * @return the mutationProvider
	 */
	public SolutionMutatorFog<SASolutionFog> getMutationProvider() {
		return mutationProvider;
	}

	@Override
	public ModulePlacementAdditionalInformationFog getModPlacementAdditionalInfo() {
		return this.modPlacementAdditionalInfo;
	}

	//

	/**
	 * @param mutationProvider the mutationProvider to set
	 */
	public void setMutationProvider(SolutionMutatorFog<SASolutionFog> mutationProvider) {
		this.mutationProvider = mutationProvider;
	}

	@Override
	public void setCoolingRate(double coolingRate) {
		if (0.0 < coolingRate && coolingRate < 1.0) {
			super.setCoolingRate(coolingRate);
		}
	}

	/**
	 * @param modPlacementAdditionalInfo the modPlacementAdditionalInfo to set
	 */
	@Override
	public void setModPlacementAdditionalInfo(ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo) {
		this.modPlacementAdditionalInfo = modPlacementAdditionalInfo;
		this.mutationProvider.resetContext(modPlacementAdditionalInfo);
	}

	//

	@Override
	public double evaluateSolution(SolutionModulesDeployed solution) {
		// TODO probabilmente la stessa funzione di GeneticAlgorithmFOg
		return SolutionsProducerEvaluator.evaluateSolution(solution, this.getModPlacementAdditionalInfo());
	}

	@Override
	public List<SolutionModulesDeployed> newRandomNeighbours(SolutionModulesDeployed originalNode, int maxAmount,
			Random r) {
		List<SolutionModulesDeployed> neighs;
		SASolutionFog originalSolutionFog, neighbour;
		SolutionDeployCosts<SASolutionFog> costCurrentSolution;
//		final Map<String,FogDevice> devicesByNameInOriginalSolution;

		if (originalNode != null && (!(originalNode instanceof SASolutionFog))) {
			throw new RuntimeException(
					"Unexpected class for the original solution: " + originalNode.getClass().getName());
		}

		/**
		 * Algorithm :
		 * <ol>
		 * <li>empty the cost modifier's map of SolutionDeployCosts</li>
		 * <li>until "maxAmount" solutions are generated, clone the originalNode</li>
		 * <li>mutate that clone</li>
		 * <li>store the mutated clone and its cost (SolutionDeployCosts)</li>
		 * </ol>
		 */

		if (originalNode == null) {
			Pair<SASolutionFog, SolutionDeployCosts<SASolutionFog>> p = SolutionsProducerEvaluator.newRandomSolution(//
					this.getModPlacementAdditionalInfo().getApplicationsByID(),
					this.getModPlacementAdditionalInfo().getModules(),
					this.getModPlacementAdditionalInfo().getDevices(), r,
					this.getModPlacementAdditionalInfo().getDevicesPartitions(), SASolutionFog::new);
			originalNode = p.getFirst();
			originalSolutionFog = (SASolutionFog) originalNode;
			costCurrentSolution = p.getSecond();

			this.getMutationProvider().saveSolutionCostsInCache(originalSolutionFog, costCurrentSolution);
		} else {
			originalSolutionFog = (org.fog.heuristics.fogImplementations.sa.SASolutionFog) originalNode;
			if (this.getMutationProvider().hasSolutionCost(originalSolutionFog)) {
				costCurrentSolution = this.getMutationProvider().getCostSolution(originalSolutionFog);
			}
		}

		// step 1)
		// .. just ignore it

//		devicesByNameInOriginalSolution = new HashMap<>();
//		originalNode.get().forEach(pos->{
//			devicesByNameInOriginalSolution.put(pos.getDevice().getName(), pos.getDevice());
//		});
//		this.getMutationProvider().getCostsSolutions().clear();

		neighs = new ArrayList<>(maxAmount);

		while (maxAmount-- > 0) {

			// step 2)
			neighbour = (SASolutionFog) originalSolutionFog.clone();

			// step 3)
			neighbour = neighbour.randomWalk(r, this.getMutationProvider(), this.getModPlacementAdditionalInfo());

			// step 4)
			neighs.add(neighbour);
			this.getMutationProvider().saveSolutionCostsInCache(neighbour, neighbour.getCosts());

//			mutationProvider.mutateFragmentOfSolution(originalFragment, solutionContext, heuristicContext, r)
//			n = new SolutionModulesDeployed 
		}

		// TODO qui viene il bello ...

		return neighs;
	}

	@Override
	public double decreaseTemperature(double originalTemperature, int currentIteration, int maxIterations) {
		return originalTemperature * this.coolingRate; // geometric cooling
	}
}