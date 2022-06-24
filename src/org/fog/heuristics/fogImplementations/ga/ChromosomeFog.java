package org.fog.heuristics.fogImplementations.ga;

import java.util.List;

import org.fog.heuristics.algorithms.ga.Chromosome;
import org.fog.heuristics.fogImplementations.PieceOfSolution;
import org.fog.heuristics.fogImplementations.SolutionModulesDeployed;

public class ChromosomeFog extends Chromosome<PieceOfSolution> implements SolutionModulesDeployed {
	private static final long serialVersionUID = 1585210010103098L;

	public ChromosomeFog() {
		super();
	}

	public ChromosomeFog(List<PieceOfSolution> pieces) {
		super(pieces);
	}

	protected List<PieceOfSolution> pieces;

	//

	public SolutionModulesDeployed getSolution() {
		return this;
	}

	@Override
	public List<PieceOfSolution> getGenes() {
		return this.pieces;
	}

	@Override
	public List<PieceOfSolution> getPieces() {
		return pieces;
	}

	//

	public void setSolution(SolutionModulesDeployed solution) {
		if (solution != null) {
			this.pieces = solution.getPieces();
		}
	}

	@Override
	public void setGenes(List<PieceOfSolution> genes) {
		this.setPieces(genes);
	}

	@Override
	public void setPieces(List<PieceOfSolution> pieces) {
		if (pieces != null) {
			this.pieces = pieces;
		}
	}

	//

	@Override
	protected Chromosome<PieceOfSolution> newInstance(List<PieceOfSolution> genes) {
		ChromosomeFog c;
		c = new ChromosomeFog();
		c.setPieces(genes);
		return c;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(1024);
		sb.append("ChromosomeFog [\n\tpieces=");
		pieces.forEach(pos -> {
			sb.append("\n\t").append(pos);
		});
		return sb.append("\n]").toString();
	}

}