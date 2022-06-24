package org.fog.heuristics.fogImplementations.ga;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math3.util.Pair;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.heuristics.SolutionMutator;
import org.fog.heuristics.SolutionsProducerEvaluator;
import org.fog.heuristics.SolutionsProducerEvaluator.SolutionDeployCosts;
import org.fog.heuristics.algorithms.ga.GeneticAlgorithm;
import org.fog.heuristics.fogImplementations.HeuristicFog;
import org.fog.heuristics.fogImplementations.ModulePlacementAdditionalInformationFog;
import org.fog.heuristics.fogImplementations.PieceOfSolution;
import org.fog.heuristics.fogImplementations.SolutionModulesDeployed;
import org.fog.heuristics.fogImplementations.SolutionMutatorFog;

/**
 * 
 * @author marcoottina (marco.1995.ottina@gmail.com )
 *
 */
public class GeneticAlgorithmFog implements HeuristicFog {

	public final double crossoverRate, elitism;
	protected final GeneticAlgorithmDelegated ga;

	public GeneticAlgorithmFog(ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo) {
		this.ga = new GeneticAlgorithmDelegated(50, 10, 0.02, modPlacementAdditionalInfo);
		this.crossoverRate = 0.5;
		this.elitism = 0.2;
	}

	public GeneticAlgorithmFog(ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo,
			SolutionMutatorFog<ChromosomeFog> mutationProvider) {
		this(modPlacementAdditionalInfo);
		if (mutationProvider != null) {
			this.setMutationProvider(mutationProvider);
		}
	}

	//

	@Override
	public ModulePlacementAdditionalInformationFog getModPlacementAdditionalInfo() {
		return this.ga.getModPlacementAdditionalInfo();
	}

	public List<AppModule> getModules() {
		return ga.getModules();
	}

	public List<FogDevice> getDevices() {
		return ga.getDevices();
	}

	public double getProbabilityMutation() {
		return this.ga.mutationRate;
	}

	public SolutionMutatorFog<ChromosomeFog> getSolutionMutatorFog() {
		return this.ga.getSolutionMutatorFog();
	}

	public Map<String, Application> getApplicationsSubmitted() {
		return this.ga.getApplicationsSubmitted();
	}

	@Override
	public HeuristicType getHeuristicType() {
		return this.ga.getHeuristicType();
	}

	//

	public void setApplicationsSubmitted(Map<String, Application> applicationsSubmitted) {
		this.ga.setApplicationsSubmitted(applicationsSubmitted);
	}

	public void setModules(List<AppModule> modules) {
		this.ga.setModules(modules);
	}

	public void setDevices(List<FogDevice> devices) {
		this.ga.setDevices(devices);
	}

	public void setProbabilityMutation(double probabilityMutation) {
		this.ga.setProbabilityMutation(probabilityMutation);
	}

	public void setMutationProvider(SolutionMutator<PieceOfSolution, ChromosomeFog> mutator) {
		this.ga.setMutationProvider(mutator);
	}

	/**
	 * @param modPlacementAdditionalInfo the modPlacementAdditionalInfo to set
	 */
	@Override
	public void setModPlacementAdditionalInfo(ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo) {
		this.ga.setModPlacementAdditionalInfo(modPlacementAdditionalInfo);
	}

	//

	@Override
	public double evaluateSolution(SolutionModulesDeployed solution) {
		ChromosomeFog c;
		c = new ChromosomeFog();
		c.setSolution(solution);
		return this.ga.evaluateSolution(c);
	}

	public List<SolutionModulesDeployed> samplePopulation(SolutionModulesDeployed initialGuess, Random r) {
		ArrayList<SolutionModulesDeployed> pop;
		List<ChromosomeFog> population;
		ChromosomeFog c;
		c = new ChromosomeFog();
		c.setSolution(initialGuess);

		population = this.ga.samplePopulation(c, r);
		pop = new ArrayList<SolutionModulesDeployed>(population.size());
		population.forEach(person -> {
			pop.add(person.getSolution());
		});
		return pop;
	}

	@Override
	public SolutionModulesDeployed optimize(SolutionModulesDeployed initialGuess, int maxIterations, Random r) {
		ChromosomeFog best;
		best = new ChromosomeFog();
		best.setSolution(initialGuess);
		best = this.ga.optimize(best, maxIterations, r);
		return best.getSolution();
	}

	//

	public static class GeneticAlgorithmDelegated extends GeneticAlgorithm<PieceOfSolution, ChromosomeFog> {
		public final int populationSize, maxGenerations;
		protected double mutationRate;
		protected ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo;

		public GeneticAlgorithmDelegated(int populationSize, int maxGenerations, double mutationRate,
				ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo) {
			super(new SolutionMutatorFog<>());
			this.populationSize = populationSize;
			this.maxGenerations = maxGenerations;
			this.mutationRate = mutationRate;
			this.modPlacementAdditionalInfo = modPlacementAdditionalInfo;
		}

		public SolutionMutatorFog<ChromosomeFog> getSolutionMutatorFog() {
			return (SolutionMutatorFog<ChromosomeFog>) this.mutator;
		}

		//

		public List<AppModule> getModules() {
			return this.modPlacementAdditionalInfo.getModules();
		}

		public List<FogDevice> getDevices() {
			return this.modPlacementAdditionalInfo.getDevices();
		}

		@Override
		public double getProbabilityMutation() {
			return this.mutationRate;
		}

		public Map<String, Application> getApplicationsSubmitted() {
			return this.modPlacementAdditionalInfo.getApplicationsByID();
		}

		//

		public void setModules(List<AppModule> modules) {
			this.modPlacementAdditionalInfo.setModules(modules);
		}

		public void setDevices(List<FogDevice> devices) {
			this.modPlacementAdditionalInfo.setDevices(devices);
		}

		public void setApplicationsSubmitted(Map<String, Application> applicationsSubmitted) {
			this.modPlacementAdditionalInfo.setApplicationsByID(applicationsSubmitted);
		}

		@Override
		public void setProbabilityMutation(double probabilityMutation) {
			this.mutationRate = probabilityMutation;
		}

		//

		/**
		 * @return the modPlacementAdditionalInfo
		 */
		public ModulePlacementAdditionalInformationFog getModPlacementAdditionalInfo() {
			return modPlacementAdditionalInfo;
		}

		/**
		 * @param modPlacementAdditionalInfo the modPlacementAdditionalInfo to set
		 */
		public void setModPlacementAdditionalInfo(ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo) {
			this.modPlacementAdditionalInfo = modPlacementAdditionalInfo;
		}

		@Override
		public double evaluateSolution(ChromosomeFog solution) {
			return SolutionsProducerEvaluator.evaluateSolution(solution, this.getModPlacementAdditionalInfo());
		}

		@Override
		public ChromosomeFog optimize(ChromosomeFog initialGuess, int maxIterations, Random r) {
//			this.getSolutionMutatorFog().resetContext(getApplicationsSubmitted(), getModules(), getDevices(),
//					this.modPlacementAdditionalInfo.getDevicesPartitions());
			return super.optimize(initialGuess, maxIterations, r);
		}

		@Override
		public List<ChromosomeFog> samplePopulation(ChromosomeFog initialGuess, Random r) {
			int chrToGenerate, attemptsLeft;
			List<ChromosomeFog> population;

			population = new ArrayList<ChromosomeFog>(this.populationSize);

			if (initialGuess == null) {
				chrToGenerate = this.populationSize;
			} else {
				chrToGenerate = (this.populationSize - 1);
				population.add(initialGuess);
			}

			while (--chrToGenerate >= 0) {
				Pair<ChromosomeFog, SolutionDeployCosts<ChromosomeFog>> p;
				attemptsLeft = 16;
				do {
					p = SolutionsProducerEvaluator.newRandomSolution(getApplicationsSubmitted(), this.getModules(),
							this.getDevices(), r, modPlacementAdditionalInfo.getDevicesPartitions(),
							ChromosomeFog::new);
				} while (p == null && (attemptsLeft-- > 0));
				if (p != null) {
					population.add(p.getFirst());
					this.getSolutionMutatorFog().saveSolutionCostsInCache(p.getFirst(), p.getSecond());
				}

			}
			return population;
		}

		@Override
		public HeuristicType getHeuristicType() {
			return HeuristicType.PopulationBased;
		}
	}
}