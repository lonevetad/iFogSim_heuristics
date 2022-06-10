package org.fog.heuristics.fogImplementations;

import java.util.List;

import org.fog.heuristics.algorithms.ga.Chromosome;

public class ChromosomeFog extends Chromosome<PieceOfSolution> {
	public ChromosomeFog(SolutionModulesDeployed solution) {
		super();
		this.solution = solution;
	}

	protected SolutionModulesDeployed solution;

	public SolutionModulesDeployed getSolution() {
		return solution;
	}

	public void setSolution(SolutionModulesDeployed solution) {
		this.solution = solution;
	}

	@Override
	public List<PieceOfSolution> getGenes() {
		return this.solution.getPieces();
	}

	@Override
	public void setGenes(List<PieceOfSolution> genes) {
		if (this.solution != null) {
			this.solution.setPieces(genes);
		}
	}

	@Override
	protected Chromosome<PieceOfSolution> newInstance(List<PieceOfSolution> genes) {
		return new ChromosomeFog(new SolutionModulesDeployed(genes));
	}
}