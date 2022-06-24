package org.fog.heuristics;

import java.util.Random;

import org.fog.heuristics.algorithms.ga.GeneticAlgorithm;

/**
 * 
 * @author marcoottina (marco.1995.ottina@gmail.com )
 * 
 */
public interface Heuristic<T> {
	public static enum HeuristicType {
		SingleSolution, PopulationBased;
	}

	public HeuristicType getHeuristicType();

	/**
	 * Acts as a metric, evaluating the provided solution. <br>
	 * The actual meaning and usage depends on th heuristic itself: it may be the
	 * fitness ({@link GeneticAlgorithm}), the energy (Simulated Annealing), etc
	 * <p>
	 * If this method is not required, then just return a random garbage-like value,
	 * like 0.
	 */
	public double evaluateSolution(T solution);

	/**
	 * Invokes {@link #optimize(Object, int, Random)} providing a brand new
	 * {@link Random} instance
	 */
	public default T optimize(T initialGuess, int maxIterations) {
		return optimize(initialGuess, maxIterations, new Random());
	}

	/**
	 * Perform the heuristic and select the best candidate (as a solution), starting
	 * from an initial guess.
	 * <p>
	 * Note: if {@code maxIteration} (second parameter) is negative or equal to
	 * zero, then it's ignored in the loop condition: only the fitness is considered
	 * 
	 * @param maxIterations maximum amount of training/finding iterations; ignored
	 *                      if non-positive
	 * @param r             random source of values, used in some contexts. Set it
	 *                      to null if unused.
	 */
	public T optimize(T initialGuess, int maxIterations, Random r);
}
