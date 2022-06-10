package org.fog.heuristics.algorithms.ga;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;

import org.fog.heuristics.Heuristic;
import org.fog.utils.Pair;

public abstract class GeneticAlgorithm<T, C extends Chromosome<T>> implements Heuristic<C> {
	protected static final double log_2 = Math.log(2);

	protected static <Tt, Cc extends Chromosome<Tt>> int compareForMinHeap(EvaluatedChromosome<Tt, Cc> c1,
			EvaluatedChromosome<Tt, Cc> c2) {
		return c2.compareTo(c1); // reversed for the MIN heap purpose
	}

	protected GeneticAlgorithm() {
		super();
		this.thresholdPercentageFitnessImprovement = 0.0125;
	}

	public GeneticAlgorithm(BiFunction<T, Random, T> mutationProvider) {
		this();
		this.mutationProvider = mutationProvider;
	}

	/**
	 * See {@link #getThresholdPercentageFitnessImprovement()}
	 */
	protected double thresholdPercentageFitnessImprovement;

	/**
	 * */
	protected BiFunction<T, Random, T> mutationProvider;

	/**
	 * If the maximum fitness does not improves by this relative (percentage from
	 * 0.0 to 1.0 excluded) then the evolution cycle stops.
	 */
	public double getThresholdPercentageFitnessImprovement() {
		return thresholdPercentageFitnessImprovement;
	}

	/**
	 * Function to determine the new value of a gene after a random mutation
	 */
	public BiFunction<T, Random, T> getMutationProvider() {
		return mutationProvider;
	}

	public abstract double getProbabilityMutation();

	//

	public void setThresholdPercentageFitnessImprovement(double thresholdPercentageFitnessImprovement) {
		this.thresholdPercentageFitnessImprovement = thresholdPercentageFitnessImprovement;
	}

	public abstract void setProbabilityMutation(double probabilityMutation);

	/**
	 * See {@link #getMutationProvider()}.
	 * 
	 * @param mutationProvider
	 */
	public void setMutationProvider(BiFunction<T, Random, T> mutationProvider) {
		this.mutationProvider = mutationProvider;
	}

	//

	/**
	 * Produce a population of {@link Chromosome} from a sample ones (which may be
	 * ignored)
	 * 
	 * @param initialGuess the initial solution to be optimized
	 * @param r            just a {@link Random} instance
	 * @return a randomly generated list of solutions (each encapsulated in a
	 *         {@link Chromosome})
	 */
	public abstract List<C> samplePopulation(C initialGuess, Random r);

	@Override
	public C optimize(C initialGuess, int maxIterations, Random r) {
		return this.optimize(this.samplePopulation(initialGuess, r), maxIterations, r);
	}

	@SuppressWarnings("unchecked")
	public C optimize(List<C> population, int maxIterations, Random r) {
		// final PriorityQueue<EvaluatedC> fitnessSortedChromosomes;
		int minIterations, iteration;
		double lastBestFitness;
		final SortedSet<EvaluatedChromosome<T, C>> fitnessSortedChromosomes;
		EvaluatedChromosome<T, C> fittest, secondFittest, last, secondToLast;
		C childChromosomeFirst, childChromosomeSecond;

		Objects.requireNonNull(r);

		fitnessSortedChromosomes = new TreeSet<>(GeneticAlgorithm::compareForMinHeap);

		population.forEach(c -> fitnessSortedChromosomes.add(new EvaluatedChromosome<>(evaluateSolution(c), c)));

		iteration = 0;
		minIterations = Math.min(16, (int) Math.ceil(Math.log(maxIterations) / log_2));

		// evolve
		do {
			// selection
			fittest = fitnessSortedChromosomes.first();
			secondFittest = fitnessSortedChromosomes.first();
			// remove the 2 worst ones
			fitnessSortedChromosomes.remove(last = fitnessSortedChromosomes.last());
			fitnessSortedChromosomes.remove(secondToLast = fitnessSortedChromosomes.last());

			// crossover
			// ELITIST approach: the crossover modifies the genes, so a clone is required to
			// preserve the original ones
			childChromosomeFirst = (C) fittest.chromosome.clone();
			childChromosomeSecond = (C) secondFittest.chromosome.clone();
			childChromosomeFirst.crossoverOnePoint(childChromosomeSecond, r);

			// mutation
			mutate(childChromosomeFirst, r);
			mutate(childChromosomeSecond, r);

			// addition & compute fitness
			// recycle the instances
			last.chromosome = childChromosomeFirst;
			last.fitness = evaluateSolution(childChromosomeFirst);
			if (last.fitness > 0.0) {
				fitnessSortedChromosomes.add(last);
			}
			secondToLast.chromosome = childChromosomeSecond;
			secondToLast.fitness = evaluateSolution(childChromosomeSecond);
			if (secondToLast.fitness > 0.0) {
				fitnessSortedChromosomes.add(secondToLast);
			}

			lastBestFitness = fittest.fitness;
			iteration++;
		} while ((fitnessSortedChromosomes.size() >= 2) && iteration < maxIterations && (iteration < minIterations || //
				(thresholdPercentageFitnessImprovement < ((fittest.fitness - lastBestFitness) / lastBestFitness))));

		if (fitnessSortedChromosomes.isEmpty()) {
			return null; // ERROR
		}
		return fitnessSortedChromosomes.first().chromosome;
	}

	protected void mutate(C chromosome, Random r) {
		int i;
		LinkedList<Pair<Integer, T>> mutations;
		Pair<Integer, T> p;
		List<T> genes;
		mutations = new LinkedList<>();
		i = 0;
		genes = chromosome.getGenes();
		for (T g : genes) {
			if (getProbabilityMutation() > r.nextDouble()) {
				mutations.add(new Pair<>(i, this.mutationProvider.apply(g, r)));
			}
			i++;
		}
		while (!mutations.isEmpty()) {
			p = mutations.removeFirst();
			genes.set(p.getFirst(), p.getSecond());
		}
	}

	protected static class EvaluatedChromosome<Tt, Cc extends Chromosome<Tt>>
			implements Comparable<EvaluatedChromosome<Tt, Cc>> {
		protected static int idProg = 0;

		public EvaluatedChromosome(double fitness, Cc chromosome) {
			super();
			this.fitness = fitness;
			this.chromosome = chromosome;
			this.ID = idProg++;
		}

		protected final int ID;
		protected double fitness;
		protected Cc chromosome;

		@Override
		public int compareTo(EvaluatedChromosome<Tt, Cc> otherChromosome) {
			if (this.ID == otherChromosome.ID) {
				return 0;
			}
			if (this.fitness == otherChromosome.fitness) {
				return this.ID > otherChromosome.ID ? 1 : -1;
			}
			if (this.fitness == 0.0) {
				return -1;
			}
			if (otherChromosome.fitness == 0.0) {
				return 1;
			}
			return this.fitness > otherChromosome.fitness ? 1 : -1;
		}
	}
}
