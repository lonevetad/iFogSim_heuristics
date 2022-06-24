package org.fog.heuristics.algorithms.sa;

import java.util.List;
import java.util.Random;

import org.fog.heuristics.Heuristic;

/**
 * 
 * @author marcoottina (marco.1995.ottina@gmail.com )
 * 
 */
public abstract class SimulatedAnnealing<T> implements Heuristic<T> {

	public SimulatedAnnealing() {
		super();
		this.startingTemperature = 100.0; // randomly decided
		this.coolingRate = 0.05; // choosen from the paper
		this.maxAmountNeighbour = 20; // randomly decided
		this.lowerBoundTemperature = 0;
		this.lowerBoundEnergy = 1;
		this.upperBoundEnergy = 100000;
	}

	@Override
	public HeuristicType getHeuristicType() {
		return HeuristicType.SingleSolution;
	}

	protected int maxAmountNeighbour;
	protected double startingTemperature, coolingRate, lowerBoundTemperature, lowerBoundEnergy, upperBoundEnergy;

	/**
	 * @return the startingTemperature
	 */
	public double getStartingTemperature() {
		return startingTemperature;
	}

	/**
	 * See {@link #setCoolingRate(double)}.
	 * 
	 * @return the coolingRate
	 */
	public double getCoolingRate() {
		return coolingRate;
	}

	/**
	 * @return the lowerBoundEnergy
	 */
	public double getLowerBoundEnergy() {
		return lowerBoundEnergy;
	}

	/**
	 * @return the upperBoundEnergy
	 */
	public double getUpperBoundEnergy() {
		return upperBoundEnergy;
	}

	/**
	 * @return the lowerBoundTemperature
	 */
	public double getLowerBoundTemperature() {
		return lowerBoundTemperature;
	}

	/**
	 * @return the maxAmountNeighbour
	 */
	public int getMaxAmountNeighbour() {
		return maxAmountNeighbour;
	}

	//

	/**
	 * A value that guide the cooling of the temperature inside the
	 * {@link #decreaseTemperature(double, int, int)} implementation. <br>
	 * A geometric reduction rule requires this value to be in the range (0; 1)
	 * extremes excluded
	 * 
	 * @param coolingRate the coolingRate to set
	 */
	public void setCoolingRate(double coolingRate) {
		this.coolingRate = coolingRate;
	}

	/**
	 * @param lowerBoundEnergy the lowerBoundEnergy to set
	 */
	public void setLowerBoundEnergy(double lowerBoundEnergy) {
		this.lowerBoundEnergy = lowerBoundEnergy;
	}

	/**
	 * @param upperBoundEnergy the upperBoundEnergy to set
	 */
	public void setUpperBoundEnergy(double upperBoundEnergy) {
		this.upperBoundEnergy = upperBoundEnergy;
	}

	/**
	 * @param startingTemperature the startingTemperature to set
	 */
	public void setStartingTemperature(double startingTemperature) {
		this.startingTemperature = startingTemperature;
	}

	/**
	 * @param lowerBoundTemperature the lowerBoundTemperature to set
	 */
	public void setLowerBoundTemperature(double lowerBoundTemperature) {
		this.lowerBoundTemperature = lowerBoundTemperature;
	}

	/**
	 * @param maxAmountNeighbour the maxAmountNeighbour to set
	 */
	public void setMaxAmountNeighbour(int maxAmountNeighbour) {
		this.maxAmountNeighbour = maxAmountNeighbour;
	}

	/**
	 * 
	 * @param originalNode beware of null. If null, then simply create a random one
	 * @param maxAmount    maximum amount of neighbour (the returned list size)
	 * @param r
	 * @return
	 */
	public abstract List<T> newRandomNeighbours(T originalNode, int maxAmount, Random r);

	/**
	 * 
	 * @param energySolution   energy of the current solution
	 * @param currentSolution
	 * @param energy           minimum energy currently tracked
	 * @param temperature      current temperature
	 * @param currentIteration iteration counter
	 * @param liveNeighbours
	 * @param r
	 * @return
	 */
	public boolean canAnneal(double energySolution, T currentSolution, double energy, double temperature,
			int currentIteration, List<T> liveNeighbours, Random r) {
		return (!liveNeighbours.isEmpty()) && temperature > this.lowerBoundTemperature && this.lowerBoundEnergy < energy
				&& energy > this.upperBoundEnergy;
	}

	/**
	 * See {@link #canAnneal(double, Object, double, double, int, List, Random)}.
	 */
	public boolean canAnneal(T currentSolution, double energy, double temperature, int currentIteration,
			List<T> liveNeighbours, Random r) {
		return this.canAnneal(this.evaluateSolution(currentSolution), currentSolution, energy, temperature,
				currentIteration, liveNeighbours, r);
	}

	/**
	 * 
	 * @param originalTemperature original temperature, to be cooled
	 * @param currentIteration    current iteration when this function is called
	 * @param maxIterations       maximum amount of iterations
	 * @return
	 */
	public abstract double decreaseTemperature(double originalTemperature, int currentIteration, int maxIterations);

	@Override
	public T optimize(T initialGuess, int maxIterations, Random r) {
		int currentIteration;
		double energy, temperature, prob, energySolution;
		List<T> live;
		T expanded;

		energy = Double.MAX_VALUE;
		temperature = this.startingTemperature;
		currentIteration = 0;
		energySolution = 0.0;

		live = this.newRandomNeighbours(initialGuess, maxAmountNeighbour, r);
		expanded = initialGuess != null ? initialGuess : live.get(0);

		while ((currentIteration++ < maxIterations)
				&& this.canAnneal(energySolution, initialGuess, energy, temperature, currentIteration, live, r)) {
			while (!live.isEmpty()) {
				initialGuess = live.remove(live.size() - 1);
				energySolution = this.evaluateSolution(initialGuess);
				if (energy >= energySolution) {
					expanded = initialGuess;
				} else {
					prob = 0.0;
					if (temperature != 0.0) {
						prob = Math.exp(-(energySolution - energy) / temperature);
					}
					if (r.nextDouble() < prob) {
						expanded = initialGuess;
					} // else: keep the expanded
				}
				energy = energySolution;
			}
			live = this.newRandomNeighbours(expanded, maxAmountNeighbour, r);
			temperature = this.decreaseTemperature(temperature, currentIteration, maxIterations);
		}

		return expanded;
	}
}