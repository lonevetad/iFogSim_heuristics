package org.fog.heuristics.fogImplementations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.heuristics.algorithms.ga.GeneticAlgorithm;
import org.fog.heuristics.fogImplementations.Utils.ListDevices;

public class GeneticAlgorithmFog implements HeuristicFog
//extends GeneticAlgorithm<PieceOfSolution, ChromosomeFog>
{
	public GeneticAlgorithmFog(Map<String, Application> applicationsSubmitted, List<AppModule> modules,
			List<FogDevice> devices, SolutionMutatorFog mutationProvider) {
//		super(mutationProvider);
		this.ga = new GeneticAlgorithmDelegated(50, 10, 0.02, modules, devices, applicationsSubmitted);
		this.setApplicationsSubmitted(applicationsSubmitted);
		this.setModules(modules);
		this.setDevices(devices);
		this.crossoverRate = 0.5;
		this.elitism = 0.2;
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

	public SolutionMutatorFog getSolutionMutatorFog() {
		return this.ga.getSolutionMutatorFog();
	}

	public Map<String, Application> getApplicationsSubmitted() {
		return this.ga.applicationsSubmitted;
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

	//

	@Override
	public double evaluateSolution(SolutionModulesDeployed solution) {
		return this.ga.evaluateSolution(new ChromosomeFog(solution));
	}

	public List<SolutionModulesDeployed> samplePopulation(SolutionModulesDeployed initialGuess, Random r) {
		ArrayList<SolutionModulesDeployed> pop;
		List<ChromosomeFog> p;

		p = this.ga.samplePopulation(new ChromosomeFog(initialGuess), r);
		pop = new ArrayList<SolutionModulesDeployed>(p.size());
		p.forEach(c -> {
			pop.add(c.getSolution());
		});
		return pop;
	}

	@Override
	public SolutionModulesDeployed optimize(SolutionModulesDeployed initialGuess, int maxIterations, Random r) {
		ChromosomeFog best;
		best = this.ga.optimize(new ChromosomeFog(initialGuess), maxIterations, r);
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
			super();
			this.populationSize = populationSize;
			this.maxGenerations = maxGenerations;
			this.mutationRate = mutationRate;
			this.modules = modules;
			this.setDevices(devices);
			this.applicationsSubmitted = applicationsSubmitted;
		}

		public SolutionMutatorFog getSolutionMutatorFog() {
			return (SolutionMutatorFog) this.mutationProvider;
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
			this.devicesPartitions = Utils.partitionateDevicesByType(devices);
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
			return Utils.evaluateSolution(solution, applicationsSubmitted);
		}

		@Override
		public ChromosomeFog optimize(ChromosomeFog initialGuess, int maxIterations, Random r) {
			this.getSolutionMutatorFog().resetEvolutionEnvironment(getApplicationsSubmitted(), getModules(),
					getDevices(), this.devicesPartitions);
			return super.optimize(initialGuess, maxIterations, r);
		}

		@Override
		public List<ChromosomeFog> samplePopulation(ChromosomeFog initialGuess, Random r) {
			int chrToGenerate;
			List<ChromosomeFog> population;

			population = new ArrayList<ChromosomeFog>(this.populationSize);

			if (initialGuess == null) {
				chrToGenerate = this.populationSize;
			} else {
				chrToGenerate = (this.populationSize - 1);
				population.add(initialGuess);
			}

			while (--chrToGenerate >= 0) {
				population.add(new ChromosomeFog(
						Utils.newRandomSolution(applicationsSubmitted, this.modules, this.devices, r)));
			}
			return population;
		}
	}
}