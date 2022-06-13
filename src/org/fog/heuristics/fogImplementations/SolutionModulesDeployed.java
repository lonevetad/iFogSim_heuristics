package org.fog.heuristics.fogImplementations;

import java.io.Serializable;
import java.util.List;
import java.util.function.Supplier;

import org.fog.heuristics.Heuristic;
import org.fog.placement.ModulePlacement;

/**
 * It's a solution of a module-placement optimization problem (a "decision",
 * kind of), the one that {@link Heuristic}s will come up with and the
 * {@link ModulePlacement} has to provide and use.<br>
 * That's a
 */
public interface SolutionModulesDeployed extends Serializable, Supplier<List<PieceOfSolution>> {

//	public SolutionModulesDeployed() {super();}
//
//	public SolutionModulesDeployed(List<PieceOfSolution> pieces) {
//		this();
//		this.setPieces(pieces);
//	}
//
//	protected List<PieceOfSolution> pieces;

	@Override
	public default List<PieceOfSolution> get() {
		return this.getPieces();
	}

	public List<PieceOfSolution> getPieces();

	public void setPieces(List<PieceOfSolution> pieces);
}