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
import org.fog.heuristics.fogImplementations.ListDevices;
import org.fog.heuristics.fogImplementations.PieceOfSolution;
import org.fog.heuristics.fogImplementations.SolutionModulesDeployed;
import org.fog.heuristics.fogImplementations.SolutionMutatorFog;

public class GeneticAlgorithmFog implements HeuristicFog
//extends GeneticAlgorithm<PieceOfSolution, ChromosomeFog>
{
	public GeneticAlgorithmFog(Map<String, Application> applicationsSubmitted, List<AppModule> modules,
			List<FogDevice> devices) {
		this.ga = new GeneticAlgorithmDelegated(50, 10, 0.02, modules, devices, applicationsSubmitted);
		this.setApplicationsSubmitted(applicationsSubmitted);
		this.setModules(modules);
		this.setDevices(devices);
		this.crossoverRate = 0.5;
		this.elitism = 0.2;
	}

	public GeneticAlgorithmFog(Map<String, Application> applicationsSubmitted, List<AppModule> modules,
			List<FogDevice> devices, SolutionMutatorFog<ChromosomeFog> mutationProvider) {
//		SolutionMutatorFog<S> mutationProvider
		this(applicationsSubmitted, modules, devices);
		if (mutationProvider != null) {
			this.setMutationProvider(mutationProvider);
		}
//		super(mutationProvider);
	}

	public final double crossoverRate, elitism;
	protected final GeneticAlgorithmDelegated ga;

	//

	public List<AppModule> getModules() {
		return ga.modules;
	}

	public List<FogDevice> getDevices() {
		return ga.devices;
	}

	public double getProbabilityMutation() {
		return this.ga.mutationRate;
	}

	public SolutionMutatorFog<ChromosomeFog> getSolutionMutatorFog() {
		return this.ga.getSolutionMutatorFog();
	}

	public Map<String, Application> getApplicationsSubmitted() {
		return this.ga.applicationsSubmitted;
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
		protected List<AppModule> modules;
		protected List<FogDevice> devices;
		protected Map<String, Application> applicationsSubmitted;
		protected ListDevices[] devicesPartitions;

		public GeneticAlgorithmDelegated(int populationSize, int maxGenerations, double mutationRate,
				List<AppModule> modules, List<FogDevice> devices, Map<String, Application> applicationsSubmitted) {
			super(new SolutionMutatorFog<>());
			this.populationSize = populationSize;
			this.maxGenerations = maxGenerations;
			this.mutationRate = mutationRate;
			this.modules = modules;
			this.setDevices(devices);
			this.applicationsSubmitted = applicationsSubmitted;
		}

		public SolutionMutatorFog<ChromosomeFog> getSolutionMutatorFog() {
			return (SolutionMutatorFog<ChromosomeFog>) this.mutator;
		}

		//

		public List<AppModule> getModules() {
			return modules;
		}

		public List<FogDevice> getDevices() {
			return devices;
		}

		@Override
		public double getProbabilityMutation() {
			return this.mutationRate;
		}

		public Map<String, Application> getApplicationsSubmitted() {
			return applicationsSubmitted;
		}

		//

		public void setModules(List<AppModule> modules) {
			this.modules = modules;
		}

		public void setDevices(List<FogDevice> devices) {
			this.devices = devices;
			this.devicesPartitions = SolutionsProducerEvaluator.partitionateDevicesByType(devices);
		}

		public void setApplicationsSubmitted(Map<String, Application> applicationsSubmitted) {
			this.applicationsSubmitted = applicationsSubmitted;
		}

		@Override
		public void setProbabilityMutation(double probabilityMutation) {
			this.mutationRate = probabilityMutation;
		}

		//

		@Override
		public double evaluateSolution(ChromosomeFog solution) {
			return SolutionsProducerEvaluator.evaluateSolution(solution, applicationsSubmitted);
		}

		@Override
		public ChromosomeFog optimize(ChromosomeFog initialGuess, int maxIterations, Random r) {
			this.getSolutionMutatorFog().resetContext(getApplicationsSubmitted(), getModules(), getDevices(),
					this.devicesPartitions);
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
				attemptsLeft = 10;
				do {
					p = SolutionsProducerEvaluator.newRandomSolution(applicationsSubmitted, this.modules, this.devices,
							r, ChromosomeFog::new);
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